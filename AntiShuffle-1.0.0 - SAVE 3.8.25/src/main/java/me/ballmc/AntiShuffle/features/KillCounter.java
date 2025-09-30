package me.ballmc.AntiShuffle.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.network.play.server.S02PacketChat;
import net.weavemc.loader.api.event.Event;
import net.weavemc.loader.api.event.SubscribeEvent;
import net.weavemc.loader.api.event.PacketEvent.Receive;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ChatComponentText;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class KillCounter {
    private final Map<String, Integer> playerKills = new HashMap<>();
    private final Pattern KILL_PATTERN = Pattern.compile("(.*?) was killed by (.*?)(?:!|$)");
    private boolean isInGame = false;
    private long gameStartTime = 0;
    private boolean debugMode = true;

    public void startGame() {
        isInGame = true;
        gameStartTime = System.currentTimeMillis();
        playerKills.clear();
        sendMessage(EnumChatFormatting.GREEN + "Game tracking started!");
        if (debugMode) {
            System.out.println(String.format("[%tT] [Debug] ========= GAME STARTED =========", new Date()));
        }
    }

    public void endGame() {
        if (debugMode) {
            System.out.println(String.format("[%tT] [Debug] ========= GAME ENDED =========", new Date()));
        }
        isInGame = false;
        sendMessage(EnumChatFormatting.RED + "Game tracking ended!");
        // Display final stats
        displayStats();
        gameStartTime = 0;
    }

    private void displayStats() {
        if (playerKills.isEmpty()) {
            sendMessage(EnumChatFormatting.YELLOW + "No kills were recorded.");
            return;
        }

        sendMessage(EnumChatFormatting.YELLOW + "Final Kill Stats:");
        for (Map.Entry<String, Integer> entry : getSortedKills()) {
            sendMessage(EnumChatFormatting.WHITE + entry.getKey() + 
                       EnumChatFormatting.GRAY + ": " + 
                       EnumChatFormatting.GREEN + entry.getValue() + " kills");
        }
    }

    @SubscribeEvent
    public void onChat(Receive event) {
        if (!(event.getPacket() instanceof S02PacketChat)) return;
        
        S02PacketChat packet = (S02PacketChat) event.getPacket();
        String unformattedMessage = packet.getChatComponent().getUnformattedText();
        String formattedMessage = packet.getChatComponent().getFormattedText();

        // Only log game state changes or when we're in game
        if (debugMode && isInGame) {
            System.out.println(String.format("[%tT] [Debug] Currently in game - Time elapsed: %ds", 
                new Date(), (System.currentTimeMillis() - gameStartTime) / 1000));
        }

        // Check for game start - only on specific message
        if (!isInGame) {
            if (debugMode) {
                System.out.println("[Debug] Checking message for game start:");
                System.out.println("[Debug] Formatted message: " + formattedMessage);
                System.out.println("[Debug] Contains 'Survive while eliminating': " + formattedMessage.contains("Survive while eliminating"));
            }
            
            if (formattedMessage.contains("Survive while eliminating")) {
                if (debugMode) {
                    System.out.println(String.format("[%tT] [Debug] Game start detected from message: " + formattedMessage, new Date()));
                }
                startGame();
                return;
            }
        }

        // Track kills - now checking both in-game and potential kills
        Matcher matcher = KILL_PATTERN.matcher(unformattedMessage);
        if (matcher.find()) {
            String victim = matcher.group(1).trim();
            String killer = matcher.group(2).trim();
            
            if (debugMode) {
                System.out.println("[Debug] Kill detected!");
                System.out.println("[Debug] Game State when kill detected: " + (isInGame ? "IN_GAME" : "NOT_IN_GAME"));
                System.out.println("[Debug] Victim: " + victim);
                System.out.println("[Debug] Killer: " + killer);
            }

            // If we're in game and it's a valid kill
            if (isInGame && killer != null && !killer.isEmpty() && !killer.contains("with")) {
                playerKills.merge(killer, 1, Integer::sum);
                // Notify of kill
                sendMessage(EnumChatFormatting.GRAY + "Kill recorded for " + 
                          EnumChatFormatting.WHITE + killer + 
                          EnumChatFormatting.GRAY + " (Total: " + 
                          EnumChatFormatting.GREEN + playerKills.get(killer) + 
                          EnumChatFormatting.GRAY + ")");
            } else if (debugMode) {
                System.out.println("[Debug] Kill not counted because: " + 
                    (!isInGame ? "game not started" : 
                     killer == null ? "null killer" :
                     killer.isEmpty() ? "empty killer" :
                     killer.contains("with") ? "contains 'with'" : "unknown reason"));
            }
        }

        // Check for game end conditions
        if (isInGame && (
            unformattedMessage.contains("Winner - ") || 
            unformattedMessage.contains("Game over!") ||
            unformattedMessage.contains("Sending you to"))) {
            if (debugMode) {
                System.out.println("[Debug] Game end detected! Trigger message: " + unformattedMessage);
            }
            endGame();
        }
    }

    @SubscribeEvent
    public void onRender(Event event) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (!isInGame || 
                mc == null || 
                mc.thePlayer == null || 
                mc.gameSettings == null || 
                mc.gameSettings.showDebugInfo ||
                (mc.currentScreen != null && !(mc.currentScreen instanceof net.minecraft.client.gui.GuiChat))) {
                return;
            }

            // Only try to render if we have a valid OpenGL context
            if (!org.lwjgl.opengl.GLContext.getCapabilities().OpenGL11) {
                return;
            }

            ScaledResolution sr = new ScaledResolution(mc);
            FontRenderer fr = mc.fontRendererObj;
            int x = sr.getScaledWidth() - 150;
            int y = sr.getScaledHeight() / 4;

            // Draw background
            Gui.drawRect(x - 5, y - 5, x + 105, y + 15 + (playerKills.size() * 12), 0x80000000);
            
            // Draw title
            fr.drawStringWithShadow(
                EnumChatFormatting.YELLOW + "Kill Counter", 
                x, 
                y, 
                0xFFFFFF
            );

            // Draw time elapsed
            long elapsedSeconds = (System.currentTimeMillis() - gameStartTime) / 1000;
            String timeStr = String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60);
            fr.drawStringWithShadow(
                EnumChatFormatting.GRAY + "Time: " + timeStr,
                x + 60,
                y,
                0xFFFFFF
            );

            // Draw player kills
            int yOffset = 15;
            for (Map.Entry<String, Integer> entry : getSortedKills()) {
                String text = EnumChatFormatting.WHITE + entry.getKey() + 
                             EnumChatFormatting.GRAY + ": " + 
                             EnumChatFormatting.GREEN + entry.getValue();
                fr.drawStringWithShadow(text, x, y + yOffset, 0xFFFFFF);
                yOffset += 12;
            }
        } catch (Exception e) {
            // Log error instead of silently failing
            e.printStackTrace();
            isInGame = false;
        }
    }

    private void sendMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }

    // Methods for command access
    public boolean isInGame() {
        return isInGame;
    }

    public List<Map.Entry<String, Integer>> getSortedKills() {
        List<Map.Entry<String, Integer>> sortedKills = new ArrayList<>(playerKills.entrySet());
        sortedKills.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        return sortedKills;
    }
} 