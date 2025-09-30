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
import java.util.List;
import java.util.ArrayList;

public class LanguageCommand extends Command {
    private static final int RATE_LIMIT_DELAY = 750; // 1.2 seconds between requests
    private static final int MAX_RETRIES = 3;

    public LanguageCommand() {
        super("lg");
    }

    @Override
    public void handle(String[] args) {
        if (args.length != 1) {
            return;
        }

        String target = args[0];

        if (target.equals("all")) {
            System.out.println("/lg all command triggered");
            new Thread(() -> {
                try {
                    System.out.println("Starting to process players");
                    int requestCount = 0;
                    for (NetworkPlayerInfo player : getPlayersInTabOrder()) {
                        try {
                            System.out.println("Processing player: " + player.getGameProfile().getName());
                            if (requestCount > 0) {
                                Thread.sleep(RATE_LIMIT_DELAY);
                            }
                            requestCount++;

                            String uuid = player.getGameProfile().getId().toString().replace("-", "");
                            if (uuid.isEmpty()) continue;

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
                                String playerName = player.getGameProfile().getName();
                                String language = "Unknown";
                                
                                if (playerData.has("userLanguage")) {
                                    language = playerData.get("userLanguage").getAsString();
                                }
                                
                                sendMessage(EnumChatFormatting.GREEN + playerName + ": " + 
                                    EnumChatFormatting.AQUA + language);
                            }
                        } catch (Exception e) {
                            // Skip this player and continue
                        }
                    }
                    
                    sendMessage(EnumChatFormatting.GREEN + "Finished checking all players!");
                } catch (Exception e) {
                    sendMessage(EnumChatFormatting.RED + "An error occurred: " + e.getMessage());
                }
            }).start();
        } else {
            // Handle single player lookup
            fetchPlayerLanguage(target);
        }
    }
    
    private void fetchPlayerLanguage(String target) {
        new Thread(() -> {
            try {
                // Get UUID from Mojang API
                String uuidUrl = "https://api.mojang.com/users/profiles/minecraft/" + target;
                JsonObject uuidResponse = makeRequest(uuidUrl);
                
                if (uuidResponse == null || !uuidResponse.has("id")) {
                    sendMessage(EnumChatFormatting.RED + "Player not found!");
                    return;
                }
                
                String uuid = uuidResponse.get("id").getAsString();
                
                // Get player data from Hypixel API
                String apiKey = ApiKeyManager.getApiKey("hypixel");
                JsonObject response = makeRequest("https://api.hypixel.net/player?key=" + apiKey + "&uuid=" + uuid);
                
                if (response == null || !response.get("success").getAsBoolean()) {
                    sendMessage(EnumChatFormatting.RED + "Failed to retrieve player data from Hypixel API.");
                    return;
                }
                
                if (!response.has("player") || response.get("player").isJsonNull()) {
                    sendMessage(EnumChatFormatting.RED + "Player has no Hypixel data!");
                    return;
                }
                
                JsonObject playerData = response.get("player").getAsJsonObject();
                String language = "Unknown";
                
                if (playerData.has("userLanguage")) {
                    language = playerData.get("userLanguage").getAsString();
                }
                
                sendMessage(EnumChatFormatting.GREEN + target + "'s language: " + 
                    EnumChatFormatting.AQUA + language);
                
            } catch (Exception e) {
                sendMessage(EnumChatFormatting.RED + "Error: " + e.getMessage());
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

    private List<NetworkPlayerInfo> getPlayersInTabOrder() {
        if (Minecraft.getMinecraft().getNetHandler() == null) return new ArrayList<>();
        return new ArrayList<>(Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap());
    }
} 