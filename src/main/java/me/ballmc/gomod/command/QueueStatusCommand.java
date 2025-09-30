package me.ballmc.gomod.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.command.Command;
import me.ballmc.gomod.features.ApiKeyManager;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;

public class QueueStatusCommand extends Command {

    public QueueStatusCommand() {
        super("qstatus");
    }

    @Override
    public void handle(String[] args) {
        new Thread(() -> {
            try {
                String apiKey = ApiKeyManager.getApiKey("hypixel");
                if (apiKey == null || apiKey.isEmpty()) {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText(EnumChatFormatting.RED + "Hypixel API key not set! Use /gomod api hypixel <your-api-key>")
                    );
                    return;
                }
                
                URL url = new URL("https://api.hypixel.net/gameCounts?key=" + apiKey);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                JsonObject response = new JsonParser().parse(
                    new InputStreamReader(conn.getInputStream())
                ).getAsJsonObject();

                if (response.get("success").getAsBoolean()) {
                    JsonObject games = response.get("games").getAsJsonObject();
                    JsonObject survival = games.get("SURVIVAL_GAMES").getAsJsonObject();
                    JsonObject modes = survival.get("modes").getAsJsonObject();

                    int totalPlayers = survival.get("players").getAsInt();
                    int soloPlayers = modes.get("solo_normal").getAsInt();
                    int teamsPlayers = modes.get("teams_normal").getAsInt();
                    int lobbyPlayers = totalPlayers - (soloPlayers + teamsPlayers);

                    String message = String.format(
                        "%s[%s%s%s] %sBlitz Queue Status:%s\n" +
                        "Total Players: %s%d%s\n" +
                        "Solo Queue: %s%d%s\n" +
                        "Teams Queue: %s%d%s\n" +
                        "In Lobby: %s%d%s",
                        EnumChatFormatting.DARK_GRAY, EnumChatFormatting.DARK_GREEN, "gomod", EnumChatFormatting.DARK_GRAY,
                        EnumChatFormatting.GREEN, EnumChatFormatting.RESET,
                        EnumChatFormatting.GREEN, totalPlayers, EnumChatFormatting.RESET,
                        EnumChatFormatting.AQUA, soloPlayers, EnumChatFormatting.RESET,
                        EnumChatFormatting.BLUE, teamsPlayers, EnumChatFormatting.RESET,
                        EnumChatFormatting.YELLOW, lobbyPlayers, EnumChatFormatting.RESET
                    );

                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
                } else {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText(EnumChatFormatting.RED + "Failed to fetch queue status!")
                    );
                }
            } catch (Exception e) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.RED + "Error fetching queue status: " + e.getMessage())
                );
            }
        }).start();
    }
}