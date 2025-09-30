package me.ballmc.AntiShuffle.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.command.Command;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.network.NetHandlerPlayClient;
import me.ballmc.AntiShuffle.features.ApiKeyManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;
import net.weavemc.loader.api.event.SubscribeEvent;
import net.weavemc.loader.api.event.Event;

public class KillEffectCommand extends Command {
    private static final Map<String, String> playerKillEffects = new HashMap<>();
    private static final int RATE_LIMIT_DELAY = 750; // 1.2 seconds between requests
    private static final int MAX_RETRIES = 3;
    private static final Map<String, String> playerStats = new HashMap<>();
    private static boolean enabled = true; // Add toggle state

    public KillEffectCommand() {
        super("ke");
    }

    @Override
    public void handle(String[] args) {
        if (args.length != 1) {
            sendMessage(EnumChatFormatting.RED + "Usage: /ke <player/ALL/toggle>");
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
                    int requestCount = 0;
                    
                    for (NetworkPlayerInfo player : getPlayersInTabOrder()) {
                        try {
                            // Add rate limiting
                            if (requestCount > 0) {
                                Thread.sleep(RATE_LIMIT_DELAY);
                            }
                            requestCount++;

                            String uuid = player.getGameProfile().getId().toString().replace("-", "");
                            if (uuid.isEmpty()) continue;

                            // Add retry logic
                            JsonObject response = null;
                            int retries = 0;
                            while (response == null && retries < MAX_RETRIES) {
                                String apiKey = ApiKeyManager.getApiKey("hypixel");
                                if (apiKey == null || apiKey.isEmpty()) {
                                    sendMessage(EnumChatFormatting.RED + "Hypixel API key not set! Use /gmapi hypixel <your-api-key>");
                                    return;
                                }
                                
                                response = makeRequest("https://api.hypixel.net/player?key=" + apiKey + "&uuid=" + uuid);
                                if (response == null) {
                                    retries++;
                                    Thread.sleep(RATE_LIMIT_DELAY * 2); // Wait longer between retries
                                }
                            }

                            if (response != null && response.get("success").getAsBoolean() && response.has("player") && !response.get("player").isJsonNull()) {
                                JsonObject playerData = response.get("player").getAsJsonObject();
                                if (playerData.has("stats") && !playerData.get("stats").isJsonNull()) {
                                    JsonObject stats = playerData.get("stats").getAsJsonObject();
                                    if (stats.has("HungerGames") && !stats.get("HungerGames").isJsonNull()) {
                                        JsonObject blitzStats = stats.get("HungerGames").getAsJsonObject();
                                        System.out.println("Blitz stats for " + player.getGameProfile().getName() + ": " + blitzStats.toString());
                                        String killEffect = getKillEffect(blitzStats);
                                        String playerName = player.getGameProfile().getName();
                                        
                                        playerKillEffects.put(playerName, killEffect);
                                        playerStats.put(playerName, calculateStats(blitzStats));
                                        updatePlayerTabName(player, playerName, killEffect);
                                        sendMessage(EnumChatFormatting.GREEN + playerName + "'s kill effect: " + 
                                            getKillEffectColor(killEffect) + formatKillEffectName(killEffect));
                                    }
                                }
                            }
                            Thread.sleep(300); // Rate limit compliance
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
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
            sendMessage(EnumChatFormatting.RED + "API Error: " + conn.getResponseCode() + " - " + conn.getResponseMessage());
            throw new Exception("API Error: " + responseCode);
        }
    }

    private EnumChatFormatting getKillEffectColor(String effect) {
        switch (effect.toUpperCase()) {
            case "REGENERATION": return EnumChatFormatting.LIGHT_PURPLE;
            case "RESISTANCE": return EnumChatFormatting.GOLD;
            case "GRAVEDIGGER": return EnumChatFormatting.GREEN;
            case "RANDOM": return EnumChatFormatting.BLUE;
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

            String displayName = player.getDisplayName() != null ? player.getDisplayName().getFormattedText() : "";
            if (displayName.startsWith("§7")) {
                return;
            }
            
            EnumChatFormatting color = getKillEffectColor(effect);
            String stats = playerStats.getOrDefault(playerName, "");
            String newName = color + playerName + EnumChatFormatting.GRAY + stats;
            player.setDisplayName(new ChatComponentText(newName));
        } catch (Exception e) {
            // Silently handle any errors
        }
    }

    private void resetAllPlayerNames() {
        if (Minecraft.getMinecraft().getNetHandler() == null) return;
        
        for (NetworkPlayerInfo player : getPlayersInTabOrder()) {
            try {
                String playerName = player.getGameProfile().getName();
                String currentDisplayName = player.getDisplayName() != null ? player.getDisplayName().getFormattedText() : "";
                
                // Check if there's any formatting before the name
                int nameIndex = currentDisplayName.indexOf(playerName);
                if (nameIndex > 0) {
                    String prefix = currentDisplayName.substring(0, nameIndex);
                    String suffix = currentDisplayName.substring(nameIndex + playerName.length());
                    player.setDisplayName(new ChatComponentText(prefix + playerName + suffix));
                } else {
                    player.setDisplayName(new ChatComponentText(playerName));
                }
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

    private String formatKillEffectName(String effect) {
        switch (effect.toUpperCase()) {
            case "REGENERATION": return "Regeneration";
            case "RESISTANCE": return "Resistance";
            case "GRAVEDIGGER": return "Grave digger";
            case "RANDOM": return "Random";
            case "LEVEL_UP": return "Level up";
            case "LEVELUP": return "Level up";
            case "RAPID_FIRE": return "Rapid fire";
            case "FLAME": return "Flame";
            case "SPEED": return "Speed";
            default: return "Speed";
        }
    }

    private void sendColorLegend() {
        sendMessage(EnumChatFormatting.WHITE + "Kill Effect Colors:");
        sendMessage(EnumChatFormatting.LIGHT_PURPLE + "> " + EnumChatFormatting.LIGHT_PURPLE + "Regeneration");
        sendMessage(EnumChatFormatting.AQUA + "> " + EnumChatFormatting.AQUA + "Speed");
        sendMessage(EnumChatFormatting.GOLD + "> " + EnumChatFormatting.GOLD + "Resistance");
        sendMessage(EnumChatFormatting.GREEN + "> " + EnumChatFormatting.GREEN + "Grave digger");
        sendMessage(EnumChatFormatting.BLUE + "> " + EnumChatFormatting.BLUE + "Random");
        sendMessage(EnumChatFormatting.YELLOW + "> " + EnumChatFormatting.YELLOW + "Level up");
        sendMessage(EnumChatFormatting.DARK_RED + "> " + EnumChatFormatting.DARK_RED + "Flame");
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
                return "RAPID_FIRE";
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
            
            return String.format("%s[%s wl/%d w]%s", 
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

    private List<NetworkPlayerInfo> getPlayersInTabOrder() {
        NetHandlerPlayClient netHandler = Minecraft.getMinecraft().getNetHandler();
        if (netHandler == null) return new ArrayList<>();
        
        return new ArrayList<>(netHandler.getPlayerInfoMap());  // Preserves tab list order
    }

    @SubscribeEvent
    public void onTick(Event event) {
        if (Minecraft.getMinecraft().theWorld == null) return;
        
        for (NetworkPlayerInfo player : getPlayersInTabOrder()) {
            String playerName = player.getGameProfile().getName();
            if (playerKillEffects.containsKey(playerName)) {
                updatePlayerTabName(player, playerName, playerKillEffects.get(playerName));
            }
        }
    }

    private void fetchPlayerKillEffect(String playerName) {
        new Thread(() -> {
            try {
                sendMessage(EnumChatFormatting.GRAY + "Fetching data for " + playerName + "...");
                
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
                    sendMessage(EnumChatFormatting.RED + "Hypixel API key not set! Use /gmapi hypixel <your-api-key>");
                    return;
                }
                
                String hypixelUrl = "https://api.hypixel.net/player?key=" + apiKey + "&uuid=" + uuid;
                JsonObject hypixelResponse = makeRequest(hypixelUrl);

                if (hypixelResponse != null && hypixelResponse.has("success") && hypixelResponse.get("success").getAsBoolean()) {
                    JsonObject player = hypixelResponse.get("player").getAsJsonObject();
                    
                    if (player != null && player.has("stats") && player.getAsJsonObject("stats").has("Blitz")) {
                        JsonObject blitzStats = player.getAsJsonObject("stats").getAsJsonObject("Blitz");
                        String effect = getKillEffect(blitzStats);
                        String stats = calculateStats(blitzStats);
                        
                        playerKillEffects.put(playerName.toLowerCase(), effect);
                        playerStats.put(playerName.toLowerCase(), stats);
                        
                        // Update player name in tab list
                        updatePlayerInTab(playerName, effect);
                        
                        sendMessage(EnumChatFormatting.GREEN + "Kill effect for " + playerName + ": " + 
                                  getKillEffectColor(effect) + formatKillEffectName(effect));
                        sendMessage(EnumChatFormatting.YELLOW + "Stats: " + EnumChatFormatting.AQUA + stats);
                    } else {
                        // Default to "none" instead of showing error
                        playerKillEffects.put(playerName.toLowerCase(), "none");
                        playerStats.put(playerName.toLowerCase(), "No Blitz stats available");
                        
                        // Update player name in tab list
                        updatePlayerInTab(playerName, "none");
                        
                        sendMessage(EnumChatFormatting.YELLOW + "Player " + playerName + " has no Blitz stats. Using default effect.");
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