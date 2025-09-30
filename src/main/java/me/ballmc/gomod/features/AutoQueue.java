package me.ballmc.gomod.features;

import net.weavemc.loader.api.event.SubscribeEvent;
import net.weavemc.loader.api.event.PacketEvent.Receive;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScorePlayerTeam;
import java.util.Collection;

/**
 * Auto-queue feature that automatically joins blitz teams when a win bonus is detected
 */
public class AutoQueue {
    private static boolean enabled = false;
    
    /**
     * Sets whether auto-queue is enabled
     * @param value true to enable, false to disable
     */
    public static void setEnabled(boolean value) {
        enabled = value;
        ConfigManager.setAutoQueueEnabled(value);
    }
    
    /**
     * Checks if auto-queue is enabled
     * @return true if enabled, false otherwise
     */
    public static boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Loads the auto-queue setting from config
     */
    public static void loadSetting() {
        enabled = ConfigManager.isAutoQueueEnabled();
    }
    
    /**
     * Handles chat events to detect server-sent win bonus messages
     */
    @SubscribeEvent
    public void onChat(Receive event) {
        try {
            if (!(event.getPacket() instanceof S02PacketChat)) {
                return;
            }
            
            // Skip if auto-queue is disabled
            if (!enabled) {
                return;
            }
            
            S02PacketChat packet = (S02PacketChat) event.getPacket();
            String message = packet.getChatComponent().getFormattedText();
            String unformattedMessage = packet.getChatComponent().getUnformattedText();
            
            // Debug: Log any message containing "Win Bonus" to see the format
            if (message.contains("Win Bonus")) {
                System.out.println("[AutoQueue] Debug - Win Bonus message received: '" + message + "'");
                System.out.println("[AutoQueue] Debug - Unformatted: '" + unformattedMessage + "'");
            }
            
            // Check if message contains "(Win Bonus)" and is from server (not from a player)
            // Server messages typically don't start with player names or chat prefixes
            if (message.contains("(Win Bonus)") && !isPlayerMessage(unformattedMessage)) {
                System.out.println("[AutoQueue] Server win bonus detected! Triggering auto-queue...");
                System.out.println("[AutoQueue] Message: '" + message + "'");
                System.out.println("[AutoQueue] Unformatted: '" + unformattedMessage + "'");
                
                // Determine game mode based on scoreboard
                boolean isTeamMode = isTeamGameMode();
                String gameMode = isTeamMode ? "teams" : "solo";
                String command = isTeamMode ? "/play blitz_teams_normal" : "/play blitz_solo_normal";
                
                System.out.println("[AutoQueue] Detected game mode: " + gameMode + " (isTeamMode: " + isTeamMode + ")");
                System.out.println("[AutoQueue] Will execute command: " + command);
                
                // Get the appropriate delay from config
                float delaySeconds = isTeamMode ? ConfigManager.getAutoQueueTeamsDelay() : ConfigManager.getAutoQueueSoloDelay();
                int delay = (int)(delaySeconds * 1000); // Convert to milliseconds for Thread.sleep
                
                // Notify the player immediately with proper [gomod] formatting
                sendMessage(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.DARK_GREEN + EnumChatFormatting.BOLD + "gomod" + 
                           EnumChatFormatting.GRAY + "] " + EnumChatFormatting.RESET + 
                           EnumChatFormatting.YELLOW + "Auto-queuing for blitz " + gameMode + " in " + delaySeconds + " seconds...");
                
                // Execute the command after the configured delay
                new Thread(() -> {
                    try {
                        Thread.sleep(delay);
                        
                        // Send the auto-queue command
                        if (Minecraft.getMinecraft().thePlayer != null) {
                            System.out.println("[AutoQueue] Executing command: " + command);
                            Minecraft.getMinecraft().thePlayer.sendChatMessage(command);
                        }
                    } catch (InterruptedException e) {
                        // Thread was interrupted, don't send the command
                        System.out.println("[AutoQueue] Command execution was interrupted");
                    }
                }).start();
            }
        } catch (Exception e) {
            System.err.println("[AutoQueue] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Helper method to send a chat message to the player
     */
    private static void sendMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
    
    /**
     * Checks if a message is from a player (not from the server)
     * @param message The unformatted message to check
     * @return true if the message appears to be from a player, false if from server
     */
    private static boolean isPlayerMessage(String message) {
        // Server messages typically start with "+" for coin rewards (e.g., "+49 coins! ... (Win Bonus)")
        // Player messages don't start with "+"
        return !message.startsWith("+");
    }
    
    /**
     * Checks if the current game mode is a team mode by looking for "Team" in the scoreboard
     * @return true if "Team" is found in the scoreboard, false otherwise
     */
    private static boolean isTeamGameMode() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null || mc.theWorld.getScoreboard() == null) {
                System.out.println("[AutoQueue] No scoreboard available, defaulting to solo mode");
                return false;
            }
            
            Scoreboard scoreboard = mc.theWorld.getScoreboard();
            ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1); // Sidebar
            
            if (objective == null) {
                System.out.println("[AutoQueue] No sidebar objective found, defaulting to solo mode");
                return false;
            }
            
            Collection<Score> scores = scoreboard.getSortedScores(objective);
            System.out.println("[AutoQueue] Checking " + scores.size() + " scoreboard entries for team mode...");
            
            // Check all scoreboard entries for "Team" mention
            for (Score score : scores) {
                // Use the same method as MapQueuer to properly read scoreboard entries
                ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
                String scoreText = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
                if (scoreText != null) {
                    System.out.println("[AutoQueue] Scoreboard entry: '" + scoreText + "'");
                    
                    // Strip formatting codes before checking for team keywords
                    String cleanText = stripFormatting(scoreText);
                    System.out.println("[AutoQueue] Cleaned scoreboard entry: '" + cleanText + "'");
                    
                    // Check for various team-related keywords (case insensitive)
                    String lowerText = cleanText.toLowerCase();
                    if (lowerText.contains("team") || lowerText.contains("teams") || 
                        lowerText.contains("team mode") || lowerText.contains("team game")) {
                        System.out.println("[AutoQueue] Found team keyword in scoreboard: '" + cleanText + "', using teams mode");
                        return true;
                    }
                }
            }
            
            // Also check the objective name itself
            String objectiveName = objective.getDisplayName();
            if (objectiveName != null) {
                System.out.println("[AutoQueue] Objective name: '" + objectiveName + "'");
                
                // Strip formatting codes before checking for team keywords
                String cleanObjectiveName = stripFormatting(objectiveName);
                System.out.println("[AutoQueue] Cleaned objective name: '" + cleanObjectiveName + "'");
                
                String lowerObjective = cleanObjectiveName.toLowerCase();
                if (lowerObjective.contains("team") || lowerObjective.contains("teams")) {
                    System.out.println("[AutoQueue] Found team keyword in objective name: '" + cleanObjectiveName + "', using teams mode");
                    return true;
                }
            }
            
            System.out.println("[AutoQueue] No team keywords found in scoreboard, using solo mode");
            return false;
            
        } catch (Exception e) {
            System.err.println("[AutoQueue] Error checking scoreboard: " + e.getMessage());
            e.printStackTrace();
            // Default to solo mode on error
            return false;
        }
    }
    
    /**
     * Strips Minecraft formatting codes from text
     * @param text The text to clean
     * @return Clean text without formatting codes
     */
    private static String stripFormatting(String text) {
        if (text == null) {
            return "";
        }
        // Remove § formatting codes and & color codes
        return text.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
    }
}
