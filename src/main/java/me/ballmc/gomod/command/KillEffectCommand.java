package me.ballmc.gomod.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.command.Command;
import net.minecraft.client.network.NetworkPlayerInfo;
import me.ballmc.gomod.Main;
import me.ballmc.gomod.features.ApiKeyManager;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.weavemc.loader.api.event.PacketEvent.Receive;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.network.play.server.S3EPacketTeams;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.weavemc.loader.api.event.SubscribeEvent;
import net.weavemc.loader.api.event.Event;

public class KillEffectCommand extends Command {
    private static final Map<String, String> playerKillEffects = new HashMap<>();
    private static final int RATE_LIMIT_DELAY = 750; // 750ms between requests
    private static final int MAX_RETRIES = 3;
    private static final Map<String, String> playerStats = new HashMap<>();
    private static boolean enabled = true; // Add toggle state
    // Per-player cooldown tracking for 429 handling (ms timestamp)
    private static final java.util.concurrent.ConcurrentHashMap<String, Long> cooldownUntilMs = new java.util.concurrent.ConcurrentHashMap<>();
    private static long lastRefreshMs = 0L;

    public KillEffectCommand() {
        super("ke");
    }

    @Override
    public void handle(String[] args) {
        // Debug: Test guild tag for specific player
        if (args[0].equalsIgnoreCase("testguildplayer") && args.length > 1) {
            sendMessage(EnumChatFormatting.YELLOW + "Testing guild tag for player: " + args[1]);
            me.ballmc.gomod.features.KillCounter.testGuildTagFetch(args[1]);
            return;
        }
        
        if (args.length != 1) {
            sendMessage(EnumChatFormatting.RED + "Usage: /ke <player/ALL/toggle/testguild/testguildplayer <player>>");
            return;
        }
        
        // Debug: Test guild tag functionality
        if (args[0].equalsIgnoreCase("testguild")) {
            sendMessage(EnumChatFormatting.YELLOW + "Testing guild tag functionality...");
            me.ballmc.gomod.features.KillCounter.testGuildTagFetch(Minecraft.getMinecraft().thePlayer.getName());
            return;
        }

        if (args[0].equalsIgnoreCase("toggle")) {
            enabled = !enabled;
            sendMessage(EnumChatFormatting.YELLOW + "Kill effects display " + 
                      (enabled ? EnumChatFormatting.GREEN + "enabled" : EnumChatFormatting.RED + "disabled"));
            
            // Reset all player names in tab if disabled
            if (!enabled) {
                resetAllPlayerNames();
            } else {
                // Reapply effects if enabled
                updateAllPlayerNames();
            }
            return;
        }

        if (args[0].equalsIgnoreCase("ALL")) {
            new Thread(() -> {
                try {
                    sendColorLegend();
                    java.util.List<NetworkPlayerInfo> retryAfterCooldown = new java.util.ArrayList<>();
                    for (NetworkPlayerInfo player : getPlayersInTabOrder()) {
                        try {
                            String uuid = player.getGameProfile().getId().toString().replace("-", "");
                            if (uuid.isEmpty()) continue;
                            String pnLower = player.getGameProfile().getName().toLowerCase();
                            long nowTs = System.currentTimeMillis();
                            long untilTs = cooldownUntilMs.getOrDefault(pnLower, 0L);
                            if (untilTs > nowTs) {
                                long rem = untilTs - nowTs;
                                long secs = Math.max(1L, rem / 1000L);
                                sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + player.getGameProfile().getName() +
                                            EnumChatFormatting.RED + ": Hit rate limit. Try again in " + secs + "s..");
                                retryAfterCooldown.add(player);
                                continue;
                            }

                            // Fetch guild tag for this player if enabled
                            me.ballmc.gomod.features.KillCounter.fetchGuildTag(player.getGameProfile().getName());
                            
                            // Add retry logic
                            JsonObject response = null;
                            int retries = 0;
                            while (response == null && retries < MAX_RETRIES) {
                                String apiKey = ApiKeyManager.getApiKey("hypixel");
                                if (apiKey == null || apiKey.isEmpty()) {
                                    sendMessage(EnumChatFormatting.RED + "Hypixel API key not set! Use /gomod api to set it.");
                                    return;
                                }
                                
                                try {
                                    response = makeRequest("https://api.hypixel.net/player?key=" + apiKey + "&uuid=" + uuid, true);
                                    if (response == null) retries++;
                                } catch (Exception ex) {
                                    if (ex.getMessage() != null && ex.getMessage().contains("429")) {
                                        String pn = player.getGameProfile().getName();
                                        String key = pn.toLowerCase();
                                        long now = System.currentTimeMillis();
                                        long existing = cooldownUntilMs.getOrDefault(key, 0L);
                                        long until = (existing > now) ? existing : (now + 60_000L);
                                        cooldownUntilMs.put(key, until);
                                        long remaining = Math.max(0L, until - now);
                                        long secs = Math.max(1L, remaining / 1000L);
                                        sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + pn + EnumChatFormatting.RED + ": Hit rate limit. Try again in " + secs + "s..");
                                        // queue for non-blocking retry after remaining time
                                        retryAfterCooldown.add(player);
                                        break;
                                    } else {
                                        // Re-throw for generic handling
                                        throw ex;
                                    }
                                }
                            }

                            if (response != null && response.get("success").getAsBoolean() && response.has("player") && !response.get("player").isJsonNull()) {
                                JsonObject playerData = response.get("player").getAsJsonObject();
                                if (playerData.has("stats") && !playerData.get("stats").isJsonNull()) {
                                    JsonObject stats = playerData.get("stats").getAsJsonObject();
                                    if (stats.has("HungerGames") && !stats.get("HungerGames").isJsonNull()) {
                                        JsonObject blitzStats = stats.get("HungerGames").getAsJsonObject();
                                        String killEffect = getKillEffect(blitzStats);
                                        String playerName = player.getGameProfile().getName();
                                        
                                        playerKillEffects.put(playerName, killEffect);
                                        playerStats.put(playerName, calculateStats(blitzStats));
                                        updatePlayerTabName(player, playerName, killEffect);
                                        if (me.ballmc.gomod.features.KillCounter.isShowKeAllChat()) {
                                            sendMessage(EnumChatFormatting.GREEN + playerName + "'s kill effect: " + 
                                                getKillEffectColor(killEffect) + formatKillEffectName(killEffect));
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (!retryAfterCooldown.isEmpty()) {
                        new Thread(() -> {
                            for (NetworkPlayerInfo p : retryAfterCooldown) {
                                try {
                                    String pn = p.getGameProfile().getName();
                                    long now = System.currentTimeMillis();
                                    long until = cooldownUntilMs.getOrDefault(pn.toLowerCase(), now);
                                    long wait = Math.max(0L, until - now);
                                    if (wait > 0) { try { Thread.sleep(wait); } catch (InterruptedException ignored) {} }
                                    String uuid = p.getGameProfile().getId().toString().replace("-", "");
                                    if (uuid.isEmpty()) continue;
                                    String apiKey = ApiKeyManager.getApiKey("hypixel");
                                    if (apiKey == null || apiKey.isEmpty()) continue;
                                    JsonObject response = makeRequest("https://api.hypixel.net/player?key=" + apiKey + "&uuid=" + uuid, true);
                                    if (response != null && response.get("success").getAsBoolean() && response.has("player") && !response.get("player").isJsonNull()) {
                                        JsonObject playerData = response.get("player").getAsJsonObject();
                                        if (playerData.has("stats") && !playerData.get("stats").isJsonNull()) {
                                            JsonObject stats = playerData.get("stats").getAsJsonObject();
                                            if (stats.has("HungerGames") && !stats.get("HungerGames").isJsonNull()) {
                                                JsonObject blitzStats = stats.get("HungerGames").getAsJsonObject();
                                                String killEffect = getKillEffect(blitzStats);
                                                String name = p.getGameProfile().getName();
                                                playerKillEffects.put(name, killEffect);
                                                playerStats.put(name, calculateStats(blitzStats));
                                                me.ballmc.gomod.features.KillCounter.forceTabRefresh();
                                            }
                                        }
                                    }
                                } catch (Exception ignored) {}
                            }
                        }, "KE-RetryAfterCooldown").start();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            fetchPlayerKillEffect(args[0]);
        }
    }

    private void sendMessage(String message) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
    }

    private JsonObject makeRequest(String urlString) throws Exception {
        return makeRequest(urlString, false);
    }

    private JsonObject makeRequest(String urlString, boolean suppressErrorMessage) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return new JsonParser().parse(response.toString()).getAsJsonObject();
        } else {
            if (!suppressErrorMessage) {
                sendMessage(EnumChatFormatting.RED + "API Error: " + conn.getResponseCode() + " - " + conn.getResponseMessage());
            }
            throw new Exception("API Error: " + responseCode + " - " + conn.getResponseMessage());
        }
    }

    private EnumChatFormatting getKillEffectColor(String effect) {
        switch (effect.toUpperCase()) {
            case "REGENERATION": return EnumChatFormatting.LIGHT_PURPLE;
            case "RESISTANCE": return EnumChatFormatting.GOLD;
            case "GRAVEDIGGER": return EnumChatFormatting.GREEN;
            case "RANDOM": return EnumChatFormatting.DARK_BLUE;
            case "LEVEL_UP": return EnumChatFormatting.YELLOW;
            case "LEVELUP": return EnumChatFormatting.YELLOW;
            case "RAPID_FIRE": return EnumChatFormatting.RED;
            case "FLAME": return EnumChatFormatting.DARK_RED;
            case "SPEED": return EnumChatFormatting.AQUA;
            default: return EnumChatFormatting.AQUA;
        }
    }

    private void updatePlayerTabName(NetworkPlayerInfo player, String playerName, String effect) {
        try {
            // Don't update if disabled
            if (!enabled) {
                return;
            }
            // Do not directly set displayName to avoid conflicts with KillCounter.
            // Just ensure our stats cache is up-to-date and let KillCounter compose Tab.
        } catch (Exception e) {
            // Silently handle any errors
        }
    }

    private void resetAllPlayerNames() {
        if (Minecraft.getMinecraft().getNetHandler() == null) return;
        
        for (NetworkPlayerInfo player : getPlayersInTabOrder()) {
            try {
                // Clear override to restore server-provided formatting
                player.setDisplayName(null);
            } catch (Exception e) {
                // Silently handle errors
            }
        }
    }

    private void updateAllPlayerNames() {
        if (Minecraft.getMinecraft().getNetHandler() == null) return;
        
        for (NetworkPlayerInfo player : getPlayersInTabOrder()) {
            String playerName = player.getGameProfile().getName();
            if (playerKillEffects.containsKey(playerName)) {
                updatePlayerTabName(player, playerName, playerKillEffects.get(playerName));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerListUpdate(Event event) {
        if (Minecraft.getMinecraft().getNetHandler() == null) return;
        
        try {
            new ArrayList<>(playerKillEffects.values()).forEach(effect -> {
                try {
                    String name = playerKillEffects.entrySet().stream()
                        .filter(entry -> entry.getValue().equals(effect))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);
                    if (name == null || name.isEmpty()) return;
                    
                    NetworkPlayerInfo playerInfo = Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap()
                        .stream()
                        .filter(p -> p.getGameProfile().getName().equals(name))
                        .findFirst()
                        .orElse(null);
                    if (playerInfo == null) return;
                    
                    updatePlayerTabName(playerInfo, name, effect);
                } catch (Exception e) {
                    // Silently handle individual player errors
                }
            });
        } catch (Exception e) {
            System.err.println("Error updating player list: " + e.getMessage());
        }
    }

    // Re-apply on server tab/teams updates to prevent disappearance
    @SubscribeEvent
    public void onPackets(Receive event) {
        if (!enabled) return;
        if (Minecraft.getMinecraft().getNetHandler() == null) return;
        if (event.getPacket() instanceof S38PacketPlayerListItem || event.getPacket() instanceof S3EPacketTeams) {
            updateAllPlayerNames();
        }
    }

    private String formatKillEffectName(String effect) {
        switch (effect.toUpperCase()) {
            case "REGENERATION": return "Regeneration";
            case "RESISTANCE": return "Resistance";
            case "GRAVEDIGGER": return "Grave digger";
            case "RANDOM": return "Random";
            case "LEVEL_UP": return "Level Up";
            case "LEVELUP": return "Level Up";
            case "RAPID_FIRE": return "Flaming Arrows";
            case "FLAME": return "Flaming Arrows";
            case "SPEED": return "Speed";
            default: return "Speed";
        }
    }

    private void sendColorLegend() {
        if (me.ballmc.gomod.features.KillCounter.isShowKeAllChat()) {
            sendMessage(EnumChatFormatting.WHITE + "Kill Effect Colors:");
            sendMessage(EnumChatFormatting.AQUA + "> " + EnumChatFormatting.AQUA + "Speed");
            sendMessage(EnumChatFormatting.GOLD + "> " + EnumChatFormatting.GOLD + "Resistance");
            sendMessage(EnumChatFormatting.LIGHT_PURPLE + "> " + EnumChatFormatting.LIGHT_PURPLE + "Regeneration");
            sendMessage(EnumChatFormatting.DARK_RED + "> " + EnumChatFormatting.DARK_RED + "Flaming Arrows");
            sendMessage(EnumChatFormatting.YELLOW + "> " + EnumChatFormatting.YELLOW + "Level Up");
            sendMessage(EnumChatFormatting.GREEN + "> " + EnumChatFormatting.GREEN + "Grave digger");
            sendMessage(EnumChatFormatting.DARK_BLUE + "> " + EnumChatFormatting.DARK_BLUE + "Random");
        }
    }

    private String getKillEffect(JsonObject blitzStats) {
        if (blitzStats.has("afterkill") && !blitzStats.get("afterkill").isJsonNull()) {
            String effect = blitzStats.get("afterkill").getAsString().toUpperCase();
            
            // Handle special cases
            if (effect.equals("NONE")) {
                return "SPEED";
            } else if (effect.equals("LEVEL_UP") || effect.equals("LEVELUP")) {
                return "LEVEL_UP";
            } else if (effect.equals("RAPID_FIRE")) {
                return "FLAME";
            }
            
            return effect;
        }
        return "SPEED";
    }

    private String calculateStats(JsonObject blitzStats) {
        try {
            int soloWins = blitzStats.has("wins") ? blitzStats.get("wins").getAsInt() : 0;
            int teamWins = blitzStats.has("wins_teams_normal") ? blitzStats.get("wins_teams_normal").getAsInt() : 0;
            int totalWins = soloWins + teamWins;
            int deaths = blitzStats.has("deaths") ? blitzStats.get("deaths").getAsInt() : 1;
            double wl = deaths == 0 ? totalWins : (double) totalWins / deaths;
            
            String color;
            if (wl >= 0.70) {
                color = EnumChatFormatting.DARK_RED.toString() + EnumChatFormatting.BOLD + EnumChatFormatting.UNDERLINE;
            } else if (wl >= 0.50) {
                color = EnumChatFormatting.DARK_RED.toString() + EnumChatFormatting.BOLD;
            } else if (wl >= 0.35) {
                color = EnumChatFormatting.DARK_RED.toString();
            } else if (wl >= 0.20) {
                color = EnumChatFormatting.DARK_PURPLE.toString();
            } else if (wl >= 0.10) {
                color = EnumChatFormatting.WHITE.toString();
            } else {
                color = EnumChatFormatting.GRAY.toString();
            }
            
            String wlStr;
            if (wl == 0) {
                wlStr = "0";
            } else if (wl == Math.floor(wl * 10) / 10) {
                wlStr = String.format("%.1f", wl);
            } else {
                wlStr = String.format("%.2f", wl);
            }
            
            return String.format("%s[%swl/%dw]%s", 
                color,
                wlStr,
                totalWins,
                EnumChatFormatting.RESET);
        } catch (Exception e) {
            return "";
        }
    }

    public static String getPlayerEffect(String playerName) {
        return playerKillEffects.get(playerName);
    }
    
    public static boolean hasPlayerEffect(String playerName) {
        return playerKillEffects.containsKey(playerName);
    }

    // Expose stats suffix for KillCounter composition
    public static String getStatsSuffixFor(String playerName) {
        if (playerName == null) return "";
        String val = playerStats.get(playerName);
        if (val != null && !val.isEmpty()) return val;
        val = playerStats.get(playerName.toLowerCase());
        if (val != null && !val.isEmpty()) return val;
        // Fallback case-insensitive search
        for (Map.Entry<String, String> e : playerStats.entrySet()) {
            if (e.getKey().equalsIgnoreCase(playerName)) {
                return e.getValue();
            }
        }
        return "";
    }

    private List<NetworkPlayerInfo> getPlayersInTabOrder() {
        if (Minecraft.getMinecraft().getNetHandler() == null) return new ArrayList<>();
        return new ArrayList<>(Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap());
    }

    @SubscribeEvent
    public void onTick(Event event) {
        if (Minecraft.getMinecraft().theWorld == null) return;
        long now = System.currentTimeMillis();
        if (now - lastRefreshMs < 1500) return;
        lastRefreshMs = now;
        // Delegate composition to KillCounter to avoid flicker/conflicts
        me.ballmc.gomod.features.KillCounter.forceTabRefresh();
    }

    private void fetchPlayerKillEffect(String playerName) {
        new Thread(() -> {
            try {
                if (me.ballmc.gomod.features.KillCounter.isShowKeAllChat()) {
                    sendMessage(EnumChatFormatting.GRAY + "Fetching data for " + playerName + "...");
                }
                
                // First get UUID from Mojang API
                String uuidUrl = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
                JsonObject mojangResponse = makeRequest(uuidUrl);
                
                if (mojangResponse == null || !mojangResponse.has("id")) {
                    sendMessage(EnumChatFormatting.RED + "Player not found: " + playerName);
                    return;
                }
                
                String uuid = mojangResponse.get("id").getAsString();

                // Then get player data from Hypixel API
                String apiKey = ApiKeyManager.getApiKey("hypixel");
                if (apiKey == null || apiKey.isEmpty()) {
                    sendMessage(EnumChatFormatting.RED + "Hypixel API key not set! Use /gomod api to set it.");
                    return;
                }
                
                String hypixelUrl = "https://api.hypixel.net/player?key=" + apiKey + "&uuid=" + uuid;
                JsonObject hypixelResponse = makeRequest(hypixelUrl);

                if (hypixelResponse != null && hypixelResponse.has("success") && hypixelResponse.get("success").getAsBoolean()) {
                    JsonObject player = hypixelResponse.get("player").getAsJsonObject();
                    
                    if (player != null && player.has("stats") && player.getAsJsonObject("stats").has("HungerGames")) {
                        JsonObject blitzStats = player.getAsJsonObject("stats").getAsJsonObject("HungerGames");
                        String effect = getKillEffect(blitzStats);
                        String stats = calculateStats(blitzStats);
                        
                        playerKillEffects.put(playerName.toLowerCase(), effect);
                        playerStats.put(playerName.toLowerCase(), stats);
                        
                        // Update player name in tab list
                        updatePlayerInTab(playerName, effect);
                        
                        if (me.ballmc.gomod.features.KillCounter.isShowKeAllChat()) {
                            sendMessage(EnumChatFormatting.GREEN + "Kill effect for " + playerName + ": " + 
                                      getKillEffectColor(effect) + formatKillEffectName(effect));
                            sendMessage(EnumChatFormatting.YELLOW + "Stats: " + EnumChatFormatting.AQUA + stats);
                        }
                    } else {
                        // Default to "none" instead of showing error
                        playerKillEffects.put(playerName.toLowerCase(), "none");
                        playerStats.put(playerName.toLowerCase(), "No Blitz stats available");
                        
                        // Update player name in tab list
                        updatePlayerInTab(playerName, "none");
                        
                        if (me.ballmc.gomod.features.KillCounter.isShowKeAllChat()) {
                            sendMessage(EnumChatFormatting.YELLOW + "Player " + playerName + " has no Blitz stats. Using default effect.");
                        }
                    }
                } else {
                    String errorMsg = (hypixelResponse != null && hypixelResponse.has("cause")) ? 
                                      hypixelResponse.get("cause").getAsString() : "Failed to get player data!";
                    sendMessage(EnumChatFormatting.RED + errorMsg);
                }
            } catch (Exception e) {
                sendMessage(EnumChatFormatting.RED + "Error: " + e.getMessage());
            }
        }).start();
    }

    private void updatePlayerInTab(String playerName, String effect) {
        if (Minecraft.getMinecraft().getNetHandler() == null) return;
        
        for (NetworkPlayerInfo playerInfo : getPlayersInTabOrder()) {
            if (playerInfo.getGameProfile().getName().equals(playerName)) {
                updatePlayerTabName(playerInfo, playerName, effect);
                break;
            }
        }
    }
}
