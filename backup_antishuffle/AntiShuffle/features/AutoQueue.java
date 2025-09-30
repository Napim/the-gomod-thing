package me.ballmc.AntiShuffle.features;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.event.EventBus;
import net.weavemc.loader.api.event.SubscribeEvent;
import net.weavemc.loader.api.event.PacketEvent.Receive;
import net.minecraft.network.play.server.S02PacketChat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Handles automatic requeuing after a game ends.
 */
public class AutoQueue {
    
    // Pattern to match the specific win bonus message with §6 color code
    private static final Pattern GAME_END_PATTERN = Pattern.compile("^\\+(\\d+) coins! §6\\(Win Bonus\\)$");
    
    // Queuing modes
    public enum QueueMode {
        DISABLED,
        TEAMS,
        SOLO
    }
    
    private static QueueMode currentMode = QueueMode.DISABLED;
    private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final int REQUEUE_DELAY_SECONDS = 3; // Delay before requeuing
    
    public AutoQueue() {
        System.out.println("[AutoQueue] Initialized");
    }
    
    /**
     * Sets the auto-queue mode.
     * @param mode The queue mode to set
     * @return A message indicating the new mode
     */
    public static String setMode(QueueMode mode) {
        currentMode = mode;
        ConfigManager.setAutoQueueMode(mode.toString());
        
        String status;
        switch (mode) {
            case TEAMS:
                status = EnumChatFormatting.GREEN + "enabled" + 
                         EnumChatFormatting.YELLOW + " (Teams mode)";
                break;
            case SOLO:
                status = EnumChatFormatting.GREEN + "enabled" + 
                         EnumChatFormatting.YELLOW + " (Solo mode)";
                break;
            default:
                status = EnumChatFormatting.RED + "disabled";
                break;
        }
        
        return EnumChatFormatting.GOLD + "[gomod] " + EnumChatFormatting.RESET + 
               EnumChatFormatting.YELLOW + "Auto-queue " + status;
    }
    
    /**
     * Gets the current auto-queue mode.
     * @return The current QueueMode
     */
    public static QueueMode getMode() {
        return currentMode;
    }
    
    /**
     * Loads the auto-queue mode from config.
     */
    public static void loadMode() {
        String savedMode = ConfigManager.getAutoQueueMode();
        if (savedMode != null && !savedMode.isEmpty()) {
            try {
                currentMode = QueueMode.valueOf(savedMode);
            } catch (IllegalArgumentException e) {
                currentMode = QueueMode.DISABLED;
            }
        }
    }
    
    @SubscribeEvent
    public void onChat(Receive event) {
        try {
            if (!(event.getPacket() instanceof S02PacketChat)) {
                return;
            }
            
            // Skip if auto-queue is disabled
            if (currentMode == QueueMode.DISABLED) {
                return;
            }
            
            S02PacketChat packet = (S02PacketChat) event.getPacket();
            String message = packet.getChatComponent().getFormattedText();
            
            // Check if message matches game end pattern with §6 color code
            Matcher matcher = GAME_END_PATTERN.matcher(message);
            if (matcher.find()) {
                // Game ended, schedule requeue
                scheduleRequeue();
            }
        } catch (Exception e) {
            System.err.println("[AutoQueue] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Schedules a requeue after a short delay.
     */
    private void scheduleRequeue() {
        // Cancel any existing scheduled requeue
        scheduler.shutdownNow();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Schedule new requeue task
        scheduler.schedule(() -> {
            try {
                if (Minecraft.getMinecraft().thePlayer != null) {
                    String command;
                    String modeText;
                    
                    switch (currentMode) {
                        case TEAMS:
                            command = "/play blitz_teams_normal";
                            modeText = "teams";
                            break;
                        case SOLO:
                            command = "/play blitz_solo_normal";
                            modeText = "solo";
                            break;
                        default:
                            return; // Should not happen, but just in case
                    }
                    
                    // Send the requeue command
                    Minecraft.getMinecraft().thePlayer.sendChatMessage(command);
                    
                    // Notify the player
                    sendMessage(EnumChatFormatting.GOLD + "[gomod] " + EnumChatFormatting.RESET + 
                                EnumChatFormatting.YELLOW + "Auto-queueing for " + modeText + " game...");
                }
            } catch (Exception e) {
                System.err.println("[AutoQueue] Error requeueing: " + e.getMessage());
                e.printStackTrace();
            }
        }, REQUEUE_DELAY_SECONDS, TimeUnit.SECONDS);
    }
    
    /**
     * Helper method to send a chat message to the player.
     */
    private void sendMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
} 