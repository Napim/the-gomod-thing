package me.ballmc.AntiShuffle.features;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.event.EventBus;
import net.weavemc.loader.api.event.SubscribeEvent;
import net.weavemc.loader.api.event.PacketEvent.Receive;
import net.minecraft.network.play.server.S02PacketChat;
import me.ballmc.AntiShuffle.command.TeamSpamCommand;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles responses to team invite commands, providing customized formatting.
 */
public class TeamInviteResponseHandler {
    
    // Pattern to match player not found message
    private static final String PLAYER_NOT_FOUND = "Couldn't find a player by that name!";
    
    // Pattern to match player already in game message
    private static final String PLAYER_IN_GAME = "is already on your team!";
    
    // Pattern to match player on full team message
    private static final String PLAYER_ON_FULL_TEAM = "That player is already on a full team!";
    
    // Pattern to match team invite sent message
    private static final Pattern TEAM_INVITE_SENT_PATTERN = Pattern.compile("Team request sent to ([\\w]+)!");
    
    public TeamInviteResponseHandler() {
        // Initialization is now handled by Main.java
        System.out.println("[TeamInviteResponseHandler] Initialized");
    }
    
    @SubscribeEvent
    public void onChat(Receive event) {
        try {
            if (!(event.getPacket() instanceof S02PacketChat)) {
                return;
            }
            
            S02PacketChat packet = (S02PacketChat) event.getPacket();
            String message = packet.getChatComponent().getUnformattedText();
            
            // Track outgoing invite commands
            if (message.startsWith("/team invite ")) {
                // Extract player name from the command
                String[] parts = message.split("/team invite ");
                if (parts.length > 1) {
                    TeamSpamCommand.lastInvitedPlayer = parts[1].trim();
                }
                return; // Don't interfere with outgoing commands
            }
            
            // Always check for the most specific patterns first to avoid false matches
            
            // Check if it's "player not found" message
            if (message.contains(PLAYER_NOT_FOUND)) {
                // Cancel the original message to prevent it from showing up
                event.setCancelled(true);
                
                // Use the player name from TeamSpamCommand
                String playerName = "Unknown";
                if (!TeamSpamCommand.lastInvitedPlayer.isEmpty()) {
                    playerName = TeamSpamCommand.lastInvitedPlayer;
                } else {
                    // Try a fallback extraction method if TeamSpamCommand doesn't have it
                    String[] parts = message.split("Couldn't find a player by that name");
                    if (parts.length > 0 && parts[0].contains("/team invite ")) {
                        String[] cmdParts = parts[0].split("/team invite ");
                        if (cmdParts.length > 1) {
                            playerName = cmdParts[1].trim();
                        }
                    }
                }
                
                // Send our custom format message instead
                sendCustomNotFoundMessage(playerName);
                return; // Ensure we return early to avoid other checks
            } 
            // Check if player is on a full team
            else if (message.contains(PLAYER_ON_FULL_TEAM)) {
                // Cancel the original message
                event.setCancelled(true);
                
                // Use the last invited player name since this message doesn't contain the player name
                String playerName = "Unknown";
                if (!TeamSpamCommand.lastInvitedPlayer.isEmpty()) {
                    playerName = TeamSpamCommand.lastInvitedPlayer;
                }
                
                // Mark this player as found for future prioritization - player IS in the game
                TeamSpamCommand.lastFoundPlayer = playerName;
                
                // Send our custom format message
                sendCustomFullTeamMessage(playerName);
                return;
            }
            // Check if player is already in your team
            else if (message.contains(PLAYER_IN_GAME)) {
                // Cancel the original message
                event.setCancelled(true);
                
                // Extract the player name from the message
                // Format is typically "<player> is already on your team!"
                String playerName = message.replace(PLAYER_IN_GAME, "").trim();
                
                // If we can't extract a name but have lastInvitedPlayer, use that
                if (playerName.isEmpty() && !TeamSpamCommand.lastInvitedPlayer.isEmpty()) {
                    playerName = TeamSpamCommand.lastInvitedPlayer;
                }
                
                // Mark this player as found for future prioritization - player IS in the game
                TeamSpamCommand.lastFoundPlayer = playerName;
                
                // Send our custom format message
                sendCustomInGameMessage(playerName);
                return;
            }
            // Check for team invite sent to a nick
            else {
                Matcher matcher = TEAM_INVITE_SENT_PATTERN.matcher(message);
                if (matcher.find()) {
                    String nickName = matcher.group(1);
                    String realName = TeamSpamCommand.lastInvitedPlayer;
                    
                    // If the names don't match, it's likely a nick
                    if (!realName.isEmpty() && !realName.equals(nickName)) {
                        // Cancel the original message
                        event.setCancelled(true);
                        
                        // Mark this player as found for future prioritization - player IS in the game
                        TeamSpamCommand.lastFoundPlayer = realName;
                        
                        // Send our custom nicked player message
                        sendCustomNickedPlayerMessage(realName, nickName);
                        return;
                    } else if (!realName.isEmpty()) {
                        // The player was found with their real name
                        // This is the case where the invite was sent successfully, but we can't be sure they exist yet
                        // We'll only know if they exist when we see a response that confirms they are in the game
                        // So do NOT set lastFoundPlayer here
                        
                        // Let the original message go through without marking as found
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[TeamInviteResponseHandler] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Sends a custom formatted message when a player is not found.
     * @param playerName The name of the player not found
     */
    private void sendCustomNotFoundMessage(String playerName) {
        ChatComponentText component = new ChatComponentText(
            EnumChatFormatting.DARK_RED + "- " + playerName
        );
        Minecraft.getMinecraft().thePlayer.addChatMessage(component);
    }
    
    /**
     * Sends a custom formatted message when a player is already in the game.
     * @param playerName The name of the player in the game
     */
    private void sendCustomInGameMessage(String playerName) {
        ChatComponentText component = new ChatComponentText(
            EnumChatFormatting.GOLD + "- " + playerName + 
            EnumChatFormatting.YELLOW + " is in this game"
        );
        Minecraft.getMinecraft().thePlayer.addChatMessage(component);
    }
    
    /**
     * Sends a custom formatted message when a player is on a full team.
     * @param playerName The name of the player on a full team
     */
    private void sendCustomFullTeamMessage(String playerName) {
        ChatComponentText component = new ChatComponentText(
            EnumChatFormatting.GOLD + "- " + playerName + 
            EnumChatFormatting.YELLOW + " is in this game"
        );
        Minecraft.getMinecraft().thePlayer.addChatMessage(component);
    }
    
    /**
     * Sends a custom formatted message when a player is nicked.
     * @param realName The real name of the player
     * @param nickName The nick of the player
     */
    private void sendCustomNickedPlayerMessage(String realName, String nickName) {
        ChatComponentText component = new ChatComponentText(
            EnumChatFormatting.GOLD + "- " + realName + 
            EnumChatFormatting.YELLOW + " is nicked as " + nickName
        );
        Minecraft.getMinecraft().thePlayer.addChatMessage(component);
    }
} 