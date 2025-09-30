package me.ballmc.gomod.features;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.event.SubscribeEvent;
import net.weavemc.loader.api.event.PacketEvent.Receive;
import net.minecraft.network.play.server.S02PacketChat;
import me.ballmc.gomod.command.TeamSpamCommand;
import me.ballmc.gomod.Main;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles responses to team invite commands, providing customized formatting.
 */
public class TeamInviteResponseHandler {
    // Map real username -> nickname (persist during session)
    public static final java.util.concurrent.ConcurrentHashMap<String, String> REAL_TO_NICK = new java.util.concurrent.ConcurrentHashMap<>();
    public static final java.util.concurrent.ConcurrentHashMap<String, String> REAL_TO_NICK_COLORED = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Pattern to match player not found message
    private static final String PLAYER_NOT_FOUND = "Couldn't find a player by that name!";
    
    // Pattern to match player already in game message
    private static final String PLAYER_IN_GAME = "is in this game";
    
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
            String formattedMessage = packet.getChatComponent().getFormattedText();
            
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
                // Only interfere with automated invites (when lastInvitedPlayer is set)
                if (!TeamSpamCommand.lastInvitedPlayer.isEmpty()) {
                    // Cancel the original message to prevent it from showing up
                    event.setCancelled(true);
                    
                    // Send our custom format message for automated invites
                    sendCustomNotFoundMessage(TeamSpamCommand.lastInvitedPlayer);
                }
                // For manual invites, let the original message show (don't interfere)
                return; // Ensure we return early to avoid other checks
            } 
            // Check if player is on a full team
            else if (message.contains(PLAYER_ON_FULL_TEAM)) {
                // Only interfere with automated invites (when lastInvitedPlayer is set)
                if (!TeamSpamCommand.lastInvitedPlayer.isEmpty()) {
                    // Cancel the original message
                    event.setCancelled(true);
                    
                    // Mark this player as found for future prioritization - player IS in the game
                    TeamSpamCommand.lastFoundPlayer = TeamSpamCommand.lastInvitedPlayer;
                    
                    // Update the player's timestamp to move them up in the list
                    TeamSpamManager.updatePlayerTimestamp(TeamSpamCommand.lastInvitedPlayer);
                    
                    // Send our custom format message for automated invites
                    sendCustomFullTeamMessage(TeamSpamCommand.lastInvitedPlayer);
                }
                // For manual invites, let the original message show (don't interfere)
                return;
            }
            // Check if player is already in your team
            else if (message.contains(PLAYER_IN_GAME)) {
                // Only interfere with automated invites (when lastInvitedPlayer is set)
                if (!TeamSpamCommand.lastInvitedPlayer.isEmpty()) {
                    // Cancel the original message
                    event.setCancelled(true);
                    
                    // Extract the player name from the message
                    // Format is typically "<player> is in this game"
                    String playerName = message.replace(PLAYER_IN_GAME, "").trim();
                    
                    // If we can't extract a name, use lastInvitedPlayer
                    if (playerName.isEmpty()) {
                        playerName = TeamSpamCommand.lastInvitedPlayer;
                    }
                    
                    // Mark this player as found for future prioritization - player IS in the game
                    TeamSpamCommand.lastFoundPlayer = playerName;
                    
                    // Update the player's timestamp to move them up in the list
                    TeamSpamManager.updatePlayerTimestamp(playerName);
                    
                    // Send our custom format message for automated invites
                    sendCustomInGameMessage(playerName);
                }
                // For manual invites, let the original message show (don't interfere)
                return;
            }
            // Check for team invite sent to a nick
            else {
                Matcher matcher = TEAM_INVITE_SENT_PATTERN.matcher(message);
                if (matcher.find()) {
                    String nickName = matcher.group(1);
                    String realName = TeamSpamCommand.lastInvitedPlayer;
                    
                    // If the names don't match, it's likely a nick - this confirms the player is online
                    if (!realName.isEmpty() && !realName.equals(nickName)) {
                        // Cancel the original message
                        event.setCancelled(true);
                        
                        // Mark this player as found for future prioritization - player IS in the game
                        TeamSpamCommand.lastFoundPlayer = realName;
                        
                        // Update the player's timestamp to move them up in the list
                        TeamSpamManager.updatePlayerTimestamp(realName);
                        
                        // Try to capture colored nick segment from the formatted chat line
                        String coloredNick = extractColoredSegment(formattedMessage, nickName);
                        if (coloredNick != null && !coloredNick.isEmpty()) {
                            REAL_TO_NICK_COLORED.put(realName.toLowerCase(), coloredNick);
                        }
                        // Send our custom nicked player message
                        sendCustomNickedPlayerMessage(realName, nickName);
                        return;
                    } else if (!realName.isEmpty()) {
                        // The player was found with their real name
                        // This is the case where the invite was sent successfully, but we can't be sure they exist yet
                        // We'll only know if they exist when we see a response that confirms they are in the game
                        // So do NOT set lastFoundPlayer here
                        
                        // Let the original message go through without marking as found
                        System.out.println("[TeamInviteResponseHandler] Team request sent to " + realName + " - NOT marking as found (waiting for confirmation)");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[TeamInviteResponseHandler] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Attempts to extract the colored substring for the given unformatted target
    private String extractColoredSegment(String formatted, String target) {
        if (formatted == null || target == null || target.isEmpty()) return null;
        // Build unformatted from formatted while tracking indices
        StringBuilder plain = new StringBuilder();
        java.util.List<int[]> spans = new java.util.ArrayList<>(); // each int[]{startFmt, endFmt} for each plain char
        for (int i = 0; i < formatted.length(); ) {
            char c = formatted.charAt(i);
            if (c == '\u00A7' && i + 1 < formatted.length()) { // color code
                i += 2; // skip color code
                continue;
            }
            int start = i;
            // advance one visible char (could be surrogate, but MC range is BMP)
            i++;
            plain.append(c);
            spans.add(new int[]{start, i});
        }
        String plainStr = plain.toString();
        int idx = plainStr.indexOf(target);
        if (idx < 0) return null;
        int startFmt = spans.get(idx)[0];
        int endFmt = spans.get(idx + target.length() - 1)[1];
        // Expand end over any trailing color codes directly following the last character
        while (endFmt + 1 < formatted.length() && formatted.charAt(endFmt) == '\u00A7') {
            endFmt += 2;
        }
        return formatted.substring(startFmt, Math.min(endFmt, formatted.length()));
    }
    
    /**
     * Sends a custom formatted message when a player is not found.
     * @param playerName The name of the player not found
     */
    private void sendCustomNotFoundMessage(String playerName) {
        ChatComponentText component = new ChatComponentText(
            Main.CHAT_PREFIX + EnumChatFormatting.DARK_RED + "- " + playerName
        );
        Minecraft.getMinecraft().thePlayer.addChatMessage(component);
    }
    
    /**
     * Sends a custom formatted message when a player is already in the game.
     * @param playerName The name of the player in the game
     */
    private void sendCustomInGameMessage(String playerName) {
        ChatComponentText component = new ChatComponentText(
            Main.CHAT_PREFIX + EnumChatFormatting.GOLD + "- " + playerName + 
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
            Main.CHAT_PREFIX + EnumChatFormatting.GOLD + "- " + playerName + 
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
        // Remember mapping (case-insensitive by storing lower-case key)
        if (realName != null && !realName.isEmpty() && nickName != null && !nickName.isEmpty()) {
            REAL_TO_NICK.put(realName.toLowerCase(), nickName);
        }
        ChatComponentText component = new ChatComponentText(
            Main.CHAT_PREFIX + EnumChatFormatting.GOLD + "- " + realName + 
            EnumChatFormatting.YELLOW + " is nicked as " + nickName
        );
        Minecraft.getMinecraft().thePlayer.addChatMessage(component);
    }
}
