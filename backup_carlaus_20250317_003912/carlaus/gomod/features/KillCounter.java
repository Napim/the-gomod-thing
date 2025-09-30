package carlaus.gomod.features;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks kill counts in games.
 */
public class KillCounter {
    
    // For detecting kill messages in chat
    private static final Pattern KILL_MESSAGE_PATTERN = Pattern.compile("\\b(?:killed|murdered|slain|finished|shot|annihilated)\\b");
    
    private int killCount = 0;
    private int lastAnnouncedKillCount = 0;
    private boolean trackingEnabled = false;
    
    /**
     * Reset the kill counter to zero.
     */
    public void resetKillCount() {
        killCount = 0;
        lastAnnouncedKillCount = 0;
        if (trackingEnabled) {
            sendMessage(EnumChatFormatting.YELLOW + "Kill counter reset to 0");
        }
    }
    
    /**
     * Toggle whether kill tracking is enabled.
     * @return true if tracking is enabled, false if disabled
     */
    public boolean toggleTracking() {
        trackingEnabled = !trackingEnabled;
        return trackingEnabled;
    }
    
    /**
     * Get the current kill count.
     * @return the kill count
     */
    public int getKillCount() {
        return killCount;
    }
    
    /**
     * Check if a chat message contains a kill announcement.
     * @param message the chat message to check
     */
    public void checkForKillMessage(String message) {
        if (!trackingEnabled) return;
        
        // Don't check messages that are clearly from the mod itself
        if (message.startsWith("[gomod]")) return;
        
        // Check if the message contains kill-related keywords
        Matcher matcher = KILL_MESSAGE_PATTERN.matcher(message.toLowerCase());
        if (matcher.find()) {
            // This is a rough heuristic - a real implementation would be more sophisticated
            // and check for player names, etc.
            killCount++;
            
            // Announce multiples of 5 kills
            if (killCount % 5 == 0 && killCount > lastAnnouncedKillCount) {
                sendMessage(EnumChatFormatting.GREEN + "Kill Count: " + killCount);
                lastAnnouncedKillCount = killCount;
            }
        }
    }
    
    /**
     * Send a message to the player's chat.
     * @param message the message to send
     */
    private void sendMessage(String message) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.thePlayer != null) {
            minecraft.thePlayer.addChatMessage(
                new ChatComponentText(EnumChatFormatting.GOLD + "[gomod] " + EnumChatFormatting.RESET + message)
            );
        }
    }
} 