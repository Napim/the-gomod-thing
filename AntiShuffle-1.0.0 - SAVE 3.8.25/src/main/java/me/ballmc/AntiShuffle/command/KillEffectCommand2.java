package me.ballmc.AntiShuffle.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.command.Command;
import net.minecraft.client.network.NetworkPlayerInfo;
import me.ballmc.AntiShuffle.features.ApiKeyManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class KillEffectCommand2 extends Command {
    private static final int RATE_LIMIT_DELAY = 750; // 1.2 seconds between requests
    private static final int MAX_RETRIES = 3;

    public KillEffectCommand2() {
        super("ke");
    }

    @Override
    public void handle(String[] args) {
        if (args.length != 1 || !args[0].equals("all2")) {
            return;
        }

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
                            response = makeRequest("https://api.hypixel.net/player?key=" + apiKey + "&uuid=" + uuid);
                            if (response == null) {
                                retries++;
                                Thread.sleep(RATE_LIMIT_DELAY);
                            }
                        }

                        if (response != null && response.get("success").getAsBoolean() && response.has("player") && !response.get("player").isJsonNull()) {
                            JsonObject playerData = response.get("player").getAsJsonObject();
                            if (playerData.has("stats") && !playerData.get("stats").isJsonNull()) {
                                JsonObject stats = playerData.get("stats").getAsJsonObject();
                                if (stats.has("Blitz") && !stats.get("Blitz").isJsonNull()) {
                                    JsonObject blitzStats = stats.get("Blitz").getAsJsonObject();
                                    String killEffect = getKillEffect(blitzStats);
                                    
                                    String playerName = player.getGameProfile().getName();
                                    sendMessage(EnumChatFormatting.GREEN + playerName + ": " + 
                                        getKillEffectColor(killEffect) + formatKillEffectName(killEffect));
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Just skip this player and continue with the next
                    }
                }
                
                sendMessage(EnumChatFormatting.GREEN + "Finished checking all players!");
            } catch (Exception e) {
                sendMessage(EnumChatFormatting.RED + "An error occurred: " + e.getMessage());
            }
        }).start();
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
            return null;
        }
    }

    private EnumChatFormatting getKillEffectColor(String effect) {
        switch (effect.toUpperCase()) {
            case "REGENERATION": return EnumChatFormatting.LIGHT_PURPLE;
            case "RESISTANCE": return EnumChatFormatting.GOLD;
            case "GRAVEDIGGER": return EnumChatFormatting.GREEN;
            case "RANDOM": return EnumChatFormatting.BLUE;
            case "LEVEL_UP": return EnumChatFormatting.YELLOW;
            case "RAPID_FIRE": return EnumChatFormatting.RED;
            case "SPEED": return EnumChatFormatting.AQUA;
            default: return EnumChatFormatting.AQUA;
        }
    }

    private String formatKillEffectName(String effect) {
        switch (effect.toUpperCase()) {
            case "REGENERATION": return "Regeneration";
            case "RESISTANCE": return "Resistance";
            case "GRAVEDIGGER": return "Grave digger";
            case "RANDOM": return "Random";
            case "LEVEL_UP": return "Level up";
            case "RAPID_FIRE": return "Rapid fire";
            case "SPEED": return "Speed";
            default: return "Speed";
        }
    }

    private void sendColorLegend() {
        sendMessage(EnumChatFormatting.GOLD + "Kill Effect Colors:");
        sendMessage(EnumChatFormatting.LIGHT_PURPLE + "> " + EnumChatFormatting.LIGHT_PURPLE + "Regeneration");
        sendMessage(EnumChatFormatting.GOLD + "> " + EnumChatFormatting.GOLD + "Resistance");
        sendMessage(EnumChatFormatting.GREEN + "> " + EnumChatFormatting.GREEN + "Grave digger");
        sendMessage(EnumChatFormatting.BLUE + "> " + EnumChatFormatting.BLUE + "Random");
        sendMessage(EnumChatFormatting.YELLOW + "> " + EnumChatFormatting.YELLOW + "Level up");
        sendMessage(EnumChatFormatting.RED + "> " + EnumChatFormatting.RED + "Rapid fire");
        sendMessage(EnumChatFormatting.AQUA + "> " + EnumChatFormatting.AQUA + "Speed");
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

    private List<NetworkPlayerInfo> getPlayersInTabOrder() {
        if (Minecraft.getMinecraft().getNetHandler() == null) return new ArrayList<>();
        return new ArrayList<>(Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap());
    }
} 