package me.ballmc.AntiShuffle.command;

import net.weavemc.loader.api.command.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.event.SubscribeEvent;
import net.weavemc.loader.api.event.TickEvent;
import net.weavemc.loader.api.event.PacketEvent.Receive;
import net.minecraft.network.play.server.S02PacketChat;
import net.weavemc.loader.api.event.EventBus;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.network.NetworkPlayerInfo;
import me.ballmc.AntiShuffle.features.TeamSpamManager;
import me.ballmc.AntiShuffle.features.TeamSpamManager.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.io.IOException;
import com.google.gson.GsonBuilder;
import java.util.Map;
import java.util.HashMap;

/**
 * Command to manage team spam players.
 */
public class TeamSpamCommands extends Command {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yy HH:mm");
    private boolean isSpamming = false;
    private List<PlayerInfo> players = new ArrayList<>();
    private int currentPlayerIndex = 0;
    private long lastInviteTime = 0;
    private static final long DELAY_MS = 850; // 
    private PlayerInfo lastInvitedPlayer = null;
    private boolean confirmedResourcePlayers = false;
    private static final String FOUND_PLAYERS_FILE = "antishuffle_found_players.json";
    private List<String> foundPlayers = new ArrayList<>();
    private boolean suppressNextMessage = false;
    private String pendingNickedName = null;
    
    private static class PlayerInfo {
        String name;
        String uuid;
        
        PlayerInfo(String name, String uuid) {
            this.name = name;
            this.uuid = uuid;
        }
    }
    
    public TeamSpamCommands() {
        super("tspam");
        EventBus.subscribe(this);
        loadFoundPlayers(); // Load previously found players
    }
    
    @Override
    public void handle(String[] args) {
        if (args.length == 0) {
            // Toggle team spam
            if (isSpamming) {
                stopSpamming();
            } else {
                // Check if using resource file and not confirmed
                if (TeamSpamManager.isUsingResourceFile() && !confirmedResourcePlayers) {
                    sendMessage(EnumChatFormatting.YELLOW + "Warning: You are using the default player list.");
                    sendMessage(EnumChatFormatting.YELLOW + "Type " + EnumChatFormatting.WHITE + "/tspam confirm" + 
                              EnumChatFormatting.YELLOW + " to use these players, or add your own with " + 
                              EnumChatFormatting.WHITE + "/tspam add <username>");
                    return;
                }
                startSpamming();
            }
        } else {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "add":
                    if (args.length >= 2) {
                        handleAddPlayer(args[1]);
                    } else {
                        sendMessage(EnumChatFormatting.RED + "Usage: /tspam add <username>");
                    }
                    break;
                    
                case "remove":
                    if (args.length >= 2) {
                        handleRemovePlayer(args[1]);
                    } else {
                        sendMessage(EnumChatFormatting.RED + "Usage: /tspam remove <username>");
                    }
                    break;
                    
                case "players":
                case "list":
                    handleListPlayers();
                    break;
                    
                case "confirm":
                    confirmedResourcePlayers = true;
                    sendMessage(EnumChatFormatting.GREEN + "Default player list confirmed. You can now use /tspam to start inviting.");
                    break;
                    
                case "clear":
                    TeamSpamManager.clearPlayers();
                    confirmedResourcePlayers = false;
                    sendMessage(EnumChatFormatting.GREEN + "All players have been cleared from the team spam list.");
                    break;
                    
                case "stop":
                    stopSpamming();
                    break;
                
                case "refresh":
                    // Get player count and calculate time estimate (using 550ms per player for accuracy)
                    List<Player> playersToCheck = TeamSpamManager.getPlayers();
                    int playerCount = playersToCheck.size();
                    // Calculate time in seconds (550ms per player for a more accurate estimate)
                    double estimatedTimeSeconds = playerCount * 0.55; 
                    String timeDisplay;
                    
                    if (estimatedTimeSeconds < 60) {
                        // Less than a minute, show seconds
                        timeDisplay = String.format("%.1f seconds", estimatedTimeSeconds);
                    } else {
                        // More than a minute, show minutes and seconds
                        int minutes = (int)(estimatedTimeSeconds / 60);
                        int seconds = (int)(estimatedTimeSeconds % 60);
                        timeDisplay = minutes + " minute" + (minutes != 1 ? "s" : "") + 
                                     (seconds > 0 ? " and " + seconds + " second" + (seconds != 1 ? "s" : "") : "");
                    }
                    
                    sendMessage(EnumChatFormatting.YELLOW + "Refreshing player data for " + playerCount + 
                              " players (updating names and MVP++ status). This will take approximately " + timeDisplay + ".");
                    
                    // Run the check in a separate thread to avoid blocking the main thread
                    new Thread(() -> {
                        String result = TeamSpamManager.checkAllPlayersMVPPlusPlus();
                        sendMessage(EnumChatFormatting.GREEN + result);
                        sendMessage(EnumChatFormatting.YELLOW + "Use " + EnumChatFormatting.WHITE + "/tspam players" + 
                                  EnumChatFormatting.YELLOW + " to see updated player data.");
                    }).start();
                    break;
                    
                default:
                    sendUsage();
                    break;
            }
        }
    }
    
    private void handleAddPlayer(String username) {
        sendMessage(EnumChatFormatting.GRAY + "Adding player " + username + "...");
        
        // Run in a separate thread to prevent game freezing
        new Thread(() -> {
            String result = TeamSpamManager.addPlayer(username);
            
            if (result != null) {
                sendMessage(EnumChatFormatting.GREEN + "Added player " + EnumChatFormatting.AQUA + result + 
                          EnumChatFormatting.GREEN + " to team spam list.");
                
                // Auto-confirm when a player is successfully added
                confirmedResourcePlayers = true;
            } else {
                sendMessage(EnumChatFormatting.RED + "Failed to add player " + username + ". Check the username and try again.");
            }
        }).start();
    }
    
    private void handleRemovePlayer(String username) {
        String result = TeamSpamManager.removePlayer(username);
        sendMessage(result.startsWith("Removed") ? 
            EnumChatFormatting.GREEN + result : 
            EnumChatFormatting.RED + result);
    }
    
    private void handleListPlayers() {
        List<Player> managedPlayers = TeamSpamManager.getPlayers();
        
        if (managedPlayers.isEmpty()) {
            sendMessage(EnumChatFormatting.YELLOW + "No players in the team spam list. Add players with /tspam add <username>");
            return;
        }
        
        // Display header with source information
        if (TeamSpamManager.isUsingResourceFile()) {
            sendMessage(EnumChatFormatting.GOLD + "=== Team Spam Players (Default List) ===");
        } else {
            sendMessage(EnumChatFormatting.GOLD + "=== Team Spam Players (Your Custom List) ===");
        }
        
        // Sort players by when they were last found (newest first)
        // And put MVP++ players at the top
        List<Player> sortedPlayers = new ArrayList<>(managedPlayers);
        Collections.sort(sortedPlayers, (p1, p2) -> {
            // First group by MVP++ status
            if (p1.hasMVPPlusPlus() && !p2.hasMVPPlusPlus()) {
                return -1; // p1 comes first
            } else if (!p1.hasMVPPlusPlus() && p2.hasMVPPlusPlus()) {
                return 1;  // p2 comes first
            }
            
            // If same MVP++ status, sort by timestamp
            return Long.compare(p2.getAddedTimestamp(), p1.getAddedTimestamp());
        });
        
        // Load found players to know which ones have been found
        loadFoundPlayers();
        
        // Count MVP++ players
        int mvpPlusPlusCount = 0;
        for (Player player : sortedPlayers) {
            if (player.hasMVPPlusPlus()) {
                mvpPlusPlusCount++;
            }
        }
        
        // Display players with their timestamp and MVP++ status
        for (Player player : sortedPlayers) {
            String username = player.getUsername();
            String dateLabel = foundPlayers.contains(username.toLowerCase()) ? "Found" : "Added";
            String timestamp = DATE_FORMAT.format(new Date(player.getAddedTimestamp()));
            
            // Display in different colors based on status
            EnumChatFormatting nameColor;
            if (!player.hasMVPPlusPlus()) {
                nameColor = EnumChatFormatting.WHITE; // White for non-MVP++
            } else {
                nameColor = EnumChatFormatting.GOLD; // Orange for all MVP++ players
            }
            
            // Removed the [Not MVP++] text as requested
            sendMessage(nameColor + username + 
                      EnumChatFormatting.GRAY + " (" + dateLabel + ": " + timestamp + ")");
        }
        
        // Show totals
        sendMessage(EnumChatFormatting.YELLOW + "Total: " + sortedPlayers.size() + " players (" + 
                   mvpPlusPlusCount + " MVP++, " + (sortedPlayers.size() - mvpPlusPlusCount) + " without MVP++)");
        
        // Show confirmation status if using resource file
        if (TeamSpamManager.isUsingResourceFile()) {
            if (confirmedResourcePlayers) {
                sendMessage(EnumChatFormatting.GREEN + "Default player list is confirmed for use.");
            } else {
                sendMessage(EnumChatFormatting.RED + "Type " + EnumChatFormatting.WHITE + "/tspam confirm" + 
                          EnumChatFormatting.RED + " to use this list, or add your own players with " + 
                          EnumChatFormatting.WHITE + "/tspam add <username>");
            }
        }
    }
    
    private void loadPlayers() {
        // Load players from TeamSpamManager
        players.clear();
        List<Player> managedPlayers = TeamSpamManager.getPlayers();
        
        System.out.println("[TeamSpamCommands] Loading players from TeamSpamManager");
        System.out.println("[TeamSpamCommands] IsUsingResourceFile: " + TeamSpamManager.isUsingResourceFile());
        System.out.println("[TeamSpamCommands] Player count: " + managedPlayers.size());
        
        if (managedPlayers.isEmpty()) {
            sendMessage(EnumChatFormatting.YELLOW + "No players in the team spam list. Add players with /tspam add <username>");
        } else {
            // Debug: Print the first few players
            int debugCount = Math.min(5, managedPlayers.size());
            System.out.println("[TeamSpamCommands] First " + debugCount + " players:");
            for (int i = 0; i < debugCount; i++) {
                Player player = managedPlayers.get(i);
                System.out.println("  - " + player.getUsername() + " (" + player.getUuid() + ") MVP++: " + player.hasMVPPlusPlus());
            }
            
            // Only add MVP++ players to the active spam list
            for (Player player : managedPlayers) {
                if (player.hasMVPPlusPlus()) {
                    players.add(new PlayerInfo(
                        player.getUsername(),
                        player.getUuid()
                    ));
                }
            }
            
            if (TeamSpamManager.isUsingResourceFile()) {
                sendMessage(EnumChatFormatting.YELLOW + "Loaded " + players.size() + " players from default list.");
                if (!confirmedResourcePlayers) {
                    sendMessage(EnumChatFormatting.YELLOW + "Type " + EnumChatFormatting.WHITE + "/tspam confirm" + 
                              EnumChatFormatting.YELLOW + " to use these players, or add your own with " + 
                              EnumChatFormatting.WHITE + "/tspam add <username>");
                }
            } else {
                sendMessage(EnumChatFormatting.GREEN + "Loaded " + players.size() + " players from your custom list.");
            }
        }
    }

    // Start team spam with players from TeamSpamManager
    private void startSpamming() {
        loadPlayers();  // Reload players from TeamSpamManager
        
        if (players.isEmpty()) {
            return; // Message already sent in loadPlayers
        }
        
        // Check if using resource file and not confirmed
        if (TeamSpamManager.isUsingResourceFile() && !confirmedResourcePlayers) {
            sendMessage(EnumChatFormatting.YELLOW + "Please confirm using the default player list with " + 
                      EnumChatFormatting.WHITE + "/tspam confirm");
            return;
        }
        
        // Load the latest found players list first
        loadFoundPlayers();
        
        // Debug log the found players
        System.out.println("[TeamSpamCommands] Found players list contains " + foundPlayers.size() + " players");
        int foundDebugCount = Math.min(5, foundPlayers.size());
        for (int i = 0; i < foundDebugCount; i++) {
            if (i < foundPlayers.size()) {
                System.out.println("  - Found player: " + foundPlayers.get(i));
            }
        }
        
        // Get the timestamps from the player list to use for sorting
        Map<String, Long> playerTimestamps = new HashMap<>();
        for (PlayerInfo player : players) {
            // Find the player in the TeamSpamManager list to get their timestamp
            Player managedPlayer = TeamSpamManager.findPlayerByName(player.name);
            if (managedPlayer != null) {
                playerTimestamps.put(player.name.toLowerCase(), managedPlayer.getAddedTimestamp());
            }
        }
        
        // Sort the players list based on timestamps (most recent first)
        Collections.sort(players, (p1, p2) -> {
            String p1Lower = p1.name.toLowerCase();
            String p2Lower = p2.name.toLowerCase();
            
            // Get timestamps (default to 0 if not found)
            long p1Time = playerTimestamps.getOrDefault(p1Lower, 0L);
            long p2Time = playerTimestamps.getOrDefault(p2Lower, 0L);
            
            // Sort by timestamp (most recent first)
            return Long.compare(p2Time, p1Time);
        });
        
        isSpamming = true;
        currentPlayerIndex = 0;
        lastInviteTime = 0;
        
        // Debug: Print the players we're about to spam
        System.out.println("[TeamSpamCommands] Starting team spam with " + players.size() + " players");
        int debugCount = Math.min(5, players.size());
        for (int i = 0; i < debugCount; i++) {
            PlayerInfo player = players.get(i);
            long timestamp = playerTimestamps.getOrDefault(player.name.toLowerCase(), 0L);
            String timeInfo = timestamp > 0 ? 
                " (last found: " + DATE_FORMAT.format(new Date(timestamp)) + ")" : 
                " (never found)";
            System.out.println("  - " + player.name + timeInfo);
        }
        
        // Calculate estimated time to invite all players
        double estimatedTimeSeconds = (players.size() * DELAY_MS) / 1000.0;
        // Format to one decimal place
        String formattedTime = String.format("%.1f", estimatedTimeSeconds);
        
        // Count total players vs. MVP++ players
        int totalPlayers = TeamSpamManager.getPlayers().size();
        int skippedPlayers = totalPlayers - players.size();
        
        if (skippedPlayers > 0) {
            sendMessage(EnumChatFormatting.GREEN + "Started team invite spam with " + players.size() + 
                    " MVP++ players! (" + formattedTime + "s) " + 
                    EnumChatFormatting.YELLOW + "Skipped " + skippedPlayers + " non-MVP++ players.");
        } else {
            sendMessage(EnumChatFormatting.GREEN + "Started team invite spam with " + players.size() + 
                    " players! (" + formattedTime + "s)");
        }
    }
    
    // Stop team spam
    private void stopSpamming() {
        if (isSpamming) {
            isSpamming = false;
            sendMessage(EnumChatFormatting.DARK_RED + "Stopped team invite spam!");
        }
    }
    
    private void loadFoundPlayers() {
        File file = new File(FOUND_PLAYERS_FILE);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<List<String>>() {}.getType();
                List<String> loadedPlayers = new Gson().fromJson(reader, type);
                
                if (loadedPlayers != null) {
                    // Clear existing found players and add loaded ones
                    foundPlayers.clear();
                    
                    // Use a set temporarily to deduplicate player names
                    Set<String> uniquePlayers = new HashSet<>();
                    
                    // Ensure all player names are lowercase for consistent matching
                    for (String playerName : loadedPlayers) {
                        if (playerName != null) {
                            uniquePlayers.add(playerName.toLowerCase());
                        }
                    }
                    
                    // Add back to list
                    foundPlayers.addAll(uniquePlayers);
                    
                    System.out.println("[TeamSpamCommands] Loaded " + foundPlayers.size() + " found players");
                }
            } catch (Exception e) {
                System.err.println("Error loading found players: " + e.getMessage());
                foundPlayers = new ArrayList<>();
            }
        }
    }
    
    private void saveFoundPlayers() {
        try {
            File file = new File(FOUND_PLAYERS_FILE);
            if (!file.exists()) {
                file.createNewFile();
            }
            
            try (FileWriter writer = new FileWriter(file)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(foundPlayers, writer);
                System.out.println("[TeamSpamCommands] Saved " + foundPlayers.size() + " found players");
            }
        } catch (IOException e) {
            System.err.println("[TeamSpamCommands] Error saving found players: " + e.getMessage());
        }
    }
    
    private void addFoundPlayer(String playerName) {
        // Normalize the name for consistency
        String normalizedName = playerName.toLowerCase();
        
        // Remove if already in the list (to add it to the end, making it most recent)
        foundPlayers.remove(normalizedName);
        
        // Add to the end of the list (most recent)
        foundPlayers.add(normalizedName);
        
        // Update the player's timestamp in the main player list
        TeamSpamManager.updatePlayerTimestamp(playerName);
        
        // Save changes to disk
        saveFoundPlayers();
        System.out.println("[TeamSpamCommands] Added/Updated " + playerName + " to found players list (position " + (foundPlayers.size()-1) + ")");
    }
    
    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (!isSpamming || Minecraft.getMinecraft().thePlayer == null || Minecraft.getMinecraft().theWorld == null) return;
        
        if (players == null || players.isEmpty()) {
            isSpamming = false;
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInviteTime >= DELAY_MS) {
            if (currentPlayerIndex >= players.size()) {
                sendMessage(EnumChatFormatting.GREEN + "Finished inviting all players!");
                currentPlayerIndex = 0;
                isSpamming = false;
                return;
            }
            
            PlayerInfo player = players.get(currentPlayerIndex);
            if (player == null || player.name == null) {
                currentPlayerIndex++;
                lastInviteTime = currentTime;
                return;
            }
            
            // Check if player is in our team via scoreboard - improved check
            boolean skipPlayer = isPlayerInScoreboard(player.name);
            if (skipPlayer) {
                sendMessage(EnumChatFormatting.GRAY + "Skipping " + player.name + " (already in team)");
                currentPlayerIndex++;
                lastInviteTime = currentTime;
                return;
            }
            
            // Check if player is in the tab list - CRITICAL FIX
            boolean isInTabList = false;
            if (Minecraft.getMinecraft().getNetHandler() != null) {
                for (NetworkPlayerInfo tabPlayer : Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap()) {
                    if (tabPlayer == null || tabPlayer.getGameProfile() == null) continue;
                    
                    String tabPlayerName = tabPlayer.getGameProfile().getName();
                    if (tabPlayerName != null && tabPlayerName.equalsIgnoreCase(player.name)) {
                        isInTabList = true;
                        break;  // Found the player in tab list, exit loop
                    }
                }
            }
            
            // Handle players found in tab list
            if (isInTabList) {
                // Add to found players list
                addFoundPlayer(player.name);
                
                // Update message to be consistent with "team full" messaging
                sendMessage(EnumChatFormatting.YELLOW + "- " + player.name + " is in game (team full)");
                
                // Play sounds for found players
                try {
                    // Use a separate thread for scheduling sounds without freezing
                    new Thread(() -> {
                        try {
                            playNoteSound(0.8F);
                            playNoteSound(1.0F);
                        } catch (Exception e) {
                            System.err.println("Error scheduling sounds: " + e.getMessage());
                        }
                    }).start();
                } catch (Exception e) {
                    System.err.println("Error starting sound thread: " + e.getMessage());
                }
                
                // Move to next player directly - DO NOT DISPLAY RED NAME OR SEND INVITE
                currentPlayerIndex++;
                lastInviteTime = currentTime;
                return;
            }
            
            // Only reaches here if player is NOT in tab list
            lastInvitedPlayer = player;
            
            // Don't send the message now, we'll wait to see if there's a nickname
            suppressNextMessage = true;
            pendingNickedName = player.name;
            
            // Send the invite command
            String command = "/team invite " + player.name;
            Minecraft.getMinecraft().thePlayer.sendChatMessage(command);
            
            lastInviteTime = currentTime;
            currentPlayerIndex++;
        }
    }
    
    @SubscribeEvent
    public void onChat(Receive event) {
        if (!(event.getPacket() instanceof S02PacketChat)) return;
        
        String message = ((S02PacketChat) event.getPacket()).getChatComponent().getUnformattedText();
        
        // If it's any of the error messages and we're spamming, cancel them
        if (isSpamming && (
            message.contains("Couldn't find a player by that name!") ||
            message.contains("That player is already on a full team!")
        )) {
            event.setCancelled(true);
            if (message.contains("That player is already on a full team!") && lastInvitedPlayer != null) {
                sendMessage(EnumChatFormatting.YELLOW + "- " + lastInvitedPlayer.name + " is in game (team full)");
                
                // Add to found players list
                addFoundPlayer(lastInvitedPlayer.name);
                
                // Fix freezing by removing Thread.sleep - playSound from different threads
                // We'll schedule sounds to play on the main thread
                try {
                    // Use a separate thread for scheduling sounds without freezing
                    new Thread(() -> {
                        try {
                            playNoteSound(1.2F);
                            playNoteSound(1.5F);
                        } catch (Exception e) {
                            System.err.println("Error scheduling sounds: " + e.getMessage());
                        }
                    }).start();
                } catch (Exception e) {
                    System.err.println("Error starting sound thread: " + e.getMessage());
                }
            } else if (message.contains("Couldn't find a player by that name!") && pendingNickedName != null) {
                // Show the message for players that can't be found
                sendMessage("- " + EnumChatFormatting.DARK_RED + pendingNickedName);
                pendingNickedName = null;
                suppressNextMessage = false;
            }
            lastInvitedPlayer = null;
            return;
        }
        
        // If we don't have a last invited player, return
        if (lastInvitedPlayer == null) return;
        
        if (message.startsWith("Team request sent to ")) {
            String actualPlayer = message.substring("Team request sent to ".length(), message.length() - 1);
            
            // Check if the player is nicked (has a different name)
            if (!lastInvitedPlayer.name.equals(actualPlayer)) {
                // Show combined message for nicked player with ORANGE color for original name
                // and show the team invite message
                sendMessage("- " + EnumChatFormatting.GOLD + lastInvitedPlayer.name + 
                          EnumChatFormatting.GRAY + " is nicked as " + 
                          EnumChatFormatting.YELLOW + actualPlayer);
                
                // Show the team invite message but with our own formatting
                sendMessage(EnumChatFormatting.GRAY + "Team request sent to " + 
                          EnumChatFormatting.YELLOW + actualPlayer + 
                          EnumChatFormatting.GRAY + "!");
                
                // We handled the pending nicked name
                pendingNickedName = null;
                suppressNextMessage = false;
            } else if (suppressNextMessage && pendingNickedName != null) {
                // Player is not nicked, show regular message
                sendMessage("- " + EnumChatFormatting.DARK_RED + pendingNickedName);
                
                // Show the team invite message but with our own formatting
                sendMessage(EnumChatFormatting.GRAY + "Team request sent to " + 
                          EnumChatFormatting.YELLOW + actualPlayer + 
                          EnumChatFormatting.GRAY + "!");
                
                pendingNickedName = null;
                suppressNextMessage = false;
            }
            
            // Hide the original message since we're showing our custom one
            event.setCancelled(true);
        }
        
        lastInvitedPlayer = null;
    }
    
    // Add this helper method for playing sounds without freezing
    private void playNoteSound(float pitch) {
        try {
            if (Minecraft.getMinecraft() != null && 
                Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.playSound("note.pling", 1.0F, pitch);
            }
        } catch (Exception e) {
            System.err.println("Error playing sound: " + e.getMessage());
        }
    }

    // Add this helper method to better check if a player is in the scoreboard
    private boolean isPlayerInScoreboard(String playerName) {
        if (Minecraft.getMinecraft().theWorld == null) return false;
        
        // Check the main scoreboard
        net.minecraft.scoreboard.Scoreboard scoreboard = Minecraft.getMinecraft().theWorld.getScoreboard();
        if (scoreboard == null) return false;
        
        // Check if player is in any team
        net.minecraft.scoreboard.ScorePlayerTeam playerTeam = scoreboard.getPlayersTeam(playerName);
        if (playerTeam != null) return true;
        
        // Check for specific scoreboard entries that might indicate team membership
        for (net.minecraft.scoreboard.ScoreObjective objective : scoreboard.getScoreObjectives()) {
            if (objective == null) continue;
            
            for (net.minecraft.scoreboard.Score score : scoreboard.getSortedScores(objective)) {
                if (score == null) continue;
                
                // Get the score name and check if it has Team and player name
                String scoreName = score.getPlayerName();
                if (scoreName != null && scoreName.contains("Team")) {
                    net.minecraft.scoreboard.ScorePlayerTeam team = scoreboard.getPlayersTeam(scoreName);
                    String displayName = team != null ? 
                        team.getColorPrefix() + scoreName + team.getColorSuffix() : 
                        scoreName;
                    
                    if (displayName.contains(playerName)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    private void sendUsage() {
        sendMessage(EnumChatFormatting.YELLOW + "=== Team Spam Command Usage ===");
        sendMessage(EnumChatFormatting.GREEN + "/tspam" + 
                  EnumChatFormatting.GRAY + " - Toggle team invite spam");
        sendMessage(EnumChatFormatting.GREEN + "/tspam add <username>" + 
                  EnumChatFormatting.GRAY + " - Add a player to the team spam list");
        sendMessage(EnumChatFormatting.GREEN + "/tspam remove <username>" + 
                  EnumChatFormatting.GRAY + " - Remove a player from the team spam list");
        sendMessage(EnumChatFormatting.GREEN + "/tspam players" + 
                  EnumChatFormatting.GRAY + " - List all players in the team spam list");
        sendMessage(EnumChatFormatting.GREEN + "/tspam confirm" + 
                  EnumChatFormatting.GRAY + " - Confirm using the default player list");
        sendMessage(EnumChatFormatting.GREEN + "/tspam stop" + 
                  EnumChatFormatting.GRAY + " - Stop team invite spam");
        sendMessage(EnumChatFormatting.GREEN + "/tspam clear" + 
                  EnumChatFormatting.GRAY + " - Clear all players from the team spam list");
        sendMessage(EnumChatFormatting.GREEN + "/tspam refresh" + 
                  EnumChatFormatting.GRAY + " - Update player names and check MVP++ status");
    }
    
    private void sendMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
} 