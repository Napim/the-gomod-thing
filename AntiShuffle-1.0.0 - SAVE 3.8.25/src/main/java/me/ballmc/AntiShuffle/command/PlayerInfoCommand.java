package me.ballmc.AntiShuffle.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.command.Command;
import me.ballmc.AntiShuffle.features.ApiKeyManager;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PlayerInfoCommand extends Command {
    private static final SimpleDateFormat startDateFormat = new SimpleDateFormat("MM/dd/yy HH:mm");
    private static final SimpleDateFormat endTimeFormat = new SimpleDateFormat("HH:mm");

    public PlayerInfoCommand() {
        super("infop");
    }

    @Override
    public void handle(String[] args) {
        if (args.length != 1) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(
                new ChatComponentText(EnumChatFormatting.RED + "Usage: /infop <player>")
            );
            return;
        }

        String playerName = args[0];
        new Thread(() -> {
            try {
                // First, get the player's UUID using Mojang API
                URL mojangUrl = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
                HttpURLConnection mojangConn = (HttpURLConnection) mojangUrl.openConnection();
                mojangConn.setRequestMethod("GET");
                
                JsonObject mojangResponse = new JsonParser().parse(
                    new InputStreamReader(mojangConn.getInputStream())
                ).getAsJsonObject();
                
                String uuid = mojangResponse.get("id").getAsString();
                
                // Get player status
                String apiKey = ApiKeyManager.getApiKey("hypixel");
                if (apiKey == null || apiKey.isEmpty()) {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText(EnumChatFormatting.RED + "Hypixel API key not set! Use /gmapi hypixel <your-api-key>")
                    );
                    return;
                }
                
                URL statusUrl = new URL("https://api.hypixel.net/v2/status?key=" + apiKey + "&uuid=" + uuid);
                HttpURLConnection statusConn = (HttpURLConnection) statusUrl.openConnection();
                statusConn.setRequestMethod("GET");
                
                JsonObject statusResponse = new JsonParser().parse(
                    new InputStreamReader(statusConn.getInputStream())
                ).getAsJsonObject();
                
                // Get player data
                URL playerUrl = new URL("https://api.hypixel.net/player?key=" + apiKey + "&uuid=" + uuid);
                HttpURLConnection playerConn = (HttpURLConnection) playerUrl.openConnection();
                playerConn.setRequestMethod("GET");
                
                JsonObject playerResponse = new JsonParser().parse(
                    new InputStreamReader(playerConn.getInputStream())
                ).getAsJsonObject();
                
                // Get recent games
                URL recentGamesUrl = new URL("https://api.hypixel.net/v2/recentgames?key=" + apiKey + "&uuid=" + uuid);
                HttpURLConnection recentGamesConn = (HttpURLConnection) recentGamesUrl.openConnection();
                recentGamesConn.setRequestMethod("GET");
                
                JsonObject recentGamesResponse = new JsonParser().parse(
                    new InputStreamReader(recentGamesConn.getInputStream())
                ).getAsJsonObject();
                
                if (statusResponse.get("success").getAsBoolean() && 
                    playerResponse.get("success").getAsBoolean() && 
                    recentGamesResponse.get("success").getAsBoolean()) {
                    
                    JsonObject session = statusResponse.get("session").getAsJsonObject();
                    boolean online = session.get("online").getAsBoolean();
                    JsonObject player = playerResponse.get("player").getAsJsonObject();
                    
                    StringBuilder message = new StringBuilder();
                    message.append(EnumChatFormatting.GOLD).append("Player Info for ").append(playerName).append(":\n");
                    
                    // Handle status display based on lastLogin with specific colors
                    String status;
                    EnumChatFormatting statusColor;
                    if (player.has("lastLogin")) {
                        if (online) {
                            status = "Online";
                            statusColor = EnumChatFormatting.GREEN;
                        } else {
                            status = "Offline";
                            statusColor = EnumChatFormatting.RED;
                        }
                    } else {
                        status = "Hidden";
                        statusColor = EnumChatFormatting.DARK_RED;
                    }
                    message.append(EnumChatFormatting.AQUA).append("Status: ").append(statusColor).append(status).append("\n");
                    
                    if (online) {
                        String gameType = session.has("gameType") ? session.get("gameType").getAsString() : "N/A";
                        String mode = session.has("mode") ? session.get("mode").getAsString() : "N/A";
                        String map = session.has("map") ? session.get("map").getAsString() : "N/A";
                        
                        message.append(EnumChatFormatting.AQUA).append("Game Type: ").append(EnumChatFormatting.DARK_GREEN).append(gameType).append("\n");
                        message.append(EnumChatFormatting.AQUA).append("Mode: ").append(EnumChatFormatting.DARK_GREEN).append(mode).append("\n");
                        message.append(EnumChatFormatting.AQUA).append("Map: ").append(EnumChatFormatting.DARK_GREEN).append(map).append("\n");
                    }

                    // Check for HungerGames stats instead of Blitz
                    if (player.has("stats") && player.getAsJsonObject("stats").has("HungerGames")) {
                        JsonObject blitz = player.getAsJsonObject("stats").getAsJsonObject("HungerGames");
                        
                        String taunt = blitz.has("chosen_taunt") ? blitz.get("chosen_taunt").getAsString() : "None";
                        String victoryDance = blitz.has("chosen_victorydance") ? blitz.get("chosen_victorydance").getAsString() : "None";
                        String finisher = blitz.has("chosen_finisher") ? blitz.get("chosen_finisher").getAsString() : "None";
                        String afterkill = blitz.has("afterkill") ? blitz.get("afterkill").getAsString() : "None";
                        if (afterkill.equals("None")) afterkill = "SPEED";
                        String aura = blitz.has("aura") ? blitz.get("aura").getAsString() : "None";

                        message.append(EnumChatFormatting.YELLOW).append("Cosmetics:\n");
                        message.append(EnumChatFormatting.AQUA).append("Taunt: ").append(EnumChatFormatting.WHITE).append(taunt).append("\n");
                        message.append(EnumChatFormatting.AQUA).append("Victory Dance: ").append(EnumChatFormatting.WHITE).append(victoryDance).append("\n");
                        message.append(EnumChatFormatting.AQUA).append("Finisher: ").append(EnumChatFormatting.WHITE).append(finisher).append("\n");
                        message.append(EnumChatFormatting.AQUA).append("Kill Effect: ").append(getKillEffectColor(afterkill)).append(formatKillEffectName(afterkill)).append("\n");
                        message.append(EnumChatFormatting.AQUA).append("Aura: ").append(EnumChatFormatting.WHITE).append(aura).append("\n");
                    } else {
                        message.append(EnumChatFormatting.RED).append("No Blitz cosmetics data found for this player\n");
                    }

                    // Add recent games
                    if (recentGamesResponse.has("games") && !recentGamesResponse.get("games").isJsonNull()) {
                        JsonArray games = recentGamesResponse.get("games").getAsJsonArray();
                        if (games.size() > 0) {
                            message.append(EnumChatFormatting.YELLOW).append("\nRecent Games:\n");
                            int gamesShown = 0;
                            for (int i = 0; i < games.size() && gamesShown < 5; i++) {
                                JsonObject game = games.get(i).getAsJsonObject();
                                if (game.has("gameType")) {
                                    String gameType = game.get("gameType").getAsString();
                                    String mode = game.has("mode") ? game.get("mode").getAsString() : "N/A";
                                    String map = game.has("map") ? game.get("map").getAsString() : "N/A";
                                    long startDate = game.get("date").getAsLong();
                                    long endDate = game.has("ended") ? game.get("ended").getAsLong() : startDate;
                                    
                                    message.append(EnumChatFormatting.AQUA)
                                           .append(startDateFormat.format(new Date(startDate)))
                                           .append(EnumChatFormatting.WHITE)
                                           .append(" > ")
                                           .append(EnumChatFormatting.AQUA)
                                           .append(endTimeFormat.format(new Date(endDate)))
                                           .append(EnumChatFormatting.WHITE)
                                           .append(" - ")
                                           .append(EnumChatFormatting.DARK_GREEN)
                                           .append(gameType)
                                           .append(EnumChatFormatting.WHITE)
                                           .append(" (")
                                           .append(EnumChatFormatting.DARK_GREEN)
                                           .append(mode)
                                           .append(EnumChatFormatting.WHITE)
                                           .append(" on ")
                                           .append(EnumChatFormatting.DARK_GREEN)
                                           .append(map)
                                           .append(EnumChatFormatting.WHITE)
                                           .append(")\n");
                                    gamesShown++;
                                }
                            }
                        } else {
                            message.append(EnumChatFormatting.YELLOW).append("\nRecent Games: ").append(EnumChatFormatting.RED).append("No recent games found");
                        }
                    }

                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message.toString()));
                } else {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText(EnumChatFormatting.RED + "Failed to fetch player information!")
                    );
                }
            } catch (Exception e) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.RED + "Error fetching player information: " + e.getMessage())
                );
            }
        }).start();
    }

    private EnumChatFormatting getKillEffectColor(String effect) {
        switch (effect.toUpperCase()) {
            case "REGENERATION": return EnumChatFormatting.LIGHT_PURPLE;
            case "RESISTANCE": return EnumChatFormatting.GOLD;
            case "GRAVEDIGGER": return EnumChatFormatting.GREEN;
            case "RANDOM": return EnumChatFormatting.BLUE;
            case "LEVELUP": return EnumChatFormatting.YELLOW;
            case "FLAME": return EnumChatFormatting.DARK_RED;
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
            case "LEVELUP": return "Level up";
            case "FLAME": return "Flame";
            case "SPEED": return "Speed";
            default: return "Speed";
        }
    }
} 