package me.ballmc.AntiShuffle.command;

import net.weavemc.loader.api.command.Command;
import me.ballmc.AntiShuffle.features.TeamSpamManager;
import me.ballmc.AntiShuffle.features.TeamSpamManager.Player;
import me.ballmc.AntiShuffle.features.TeamInviteResponseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Command to manage team spam functionality.
 * This allows players to automatically spam team invites to a list of players.
 */
public class TeamSpamCommand extends Command {
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static final AtomicInteger count = new AtomicInteger(0);
    private static final AtomicInteger delay = new AtomicInteger(750); // ms between invites
    private static final AtomicBoolean isProcessing = new AtomicBoolean(false); // Lock for preventing concurrent execution
    private static ScheduledFuture<?> currentTask = null; // Reference to the current scheduled task
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy");
    
    // Keep track of the last player that was invited
    public static String lastInvitedPlayer = "";
    
    // Keep track of the last player successfully found
    public static String lastFoundPlayer = "";
    
    public TeamSpamCommand() {
        super("tspam");
    }
    
    @Override
    public void handle(String[] args) {
        if (args.length == 0) {
            toggleTeamSpam();
            return;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "add":
                if (args.length > 1) {
                    String result = TeamSpamManager.addPlayer(args[1]);
                    sendMessage(result);
                } else {
                    sendMessage(EnumChatFormatting.RED + "Usage: /tspam add <username>");
                }
                break;
                
            case "remove":
                if (args.length > 1) {
                    String result = TeamSpamManager.removePlayer(args[1]);
                    sendMessage(result);
                } else {
                    sendMessage(EnumChatFormatting.RED + "Usage: /tspam remove <username>");
                }
                break;
                
            case "players":
                showPlayers();
                break;
                
            case "confirm":
                if (TeamSpamManager.isUsingResourceFile()) {
                    sendMessage(EnumChatFormatting.GREEN + "Confirmed using the default player list.");
                    TeamSpamManager.savePlayers();
                } else {
                    sendMessage(EnumChatFormatting.RED + "You're already using your own player list. No confirmation needed.");
                }
                break;
                
            case "clear":
                TeamSpamManager.clearPlayers();
                sendMessage(EnumChatFormatting.GREEN + "Cleared all players from team spam list.");
                break;
                
            case "refresh":
                refreshPlayers();
                break;
                
            default:
                sendMessage(EnumChatFormatting.RED + "Unknown subcommand: " + subCommand);
                sendMessage(EnumChatFormatting.YELLOW + "Available commands: add, remove, players, confirm, clear, refresh");
        }
    }
    
    private void toggleTeamSpam() {
        if (running.get()) {
            // Stop the spam
            stopTeamSpam();
        } else {
            // Start the spam
            startTeamSpam();
        }
    }
    
    private void stopTeamSpam() {
        running.set(false);
        
        // Cancel the current task if it exists
        if (currentTask != null && !currentTask.isDone() && !currentTask.isCancelled()) {
            currentTask.cancel(false);
            currentTask = null;
        }
        
        // Reset the scheduler to ensure a clean state
        scheduler.shutdown();
        scheduler = Executors.newScheduledThreadPool(1);
        
        // Reset other state
        isProcessing.set(false);
        count.set(0);
        lastInvitedPlayer = "";
        lastFoundPlayer = "";
        
        sendMessage(EnumChatFormatting.RED + "Team spam stopped.");
    }
    
    private void startTeamSpam() {
        // First make sure any previous spam is properly stopped
        if (running.get()) {
            stopTeamSpam();
        }
        
        List<Player> allPlayers = TeamSpamManager.getPlayers();
        
        // Filter only MVP++ players
        List<Player> mvpPlusPlusPlayers = new ArrayList<>();
        for (Player player : allPlayers) {
            if (player.hasMVPPlusPlus()) {
                mvpPlusPlusPlayers.add(player);
            }
        }
        
        if (mvpPlusPlusPlayers.isEmpty()) {
            sendMessage(EnumChatFormatting.RED + "No MVP++ players in team spam list. Add MVP++ players with /tspam add <username>");
            return;
        }
        
        // If we have a previously found player, prioritize them by putting them at the start
        if (lastFoundPlayer != null && !lastFoundPlayer.isEmpty()) {
            // Reorder the list to start with the last found player if they exist
            List<Player> reorderedPlayers = new ArrayList<>();
            Player lastFoundPlayerObj = null;
            
            // Find and remove the last found player
            for (int i = 0; i < mvpPlusPlusPlayers.size(); i++) {
                Player player = mvpPlusPlusPlayers.get(i);
                if (player.getUsername().equalsIgnoreCase(lastFoundPlayer)) {
                    lastFoundPlayerObj = player;
                    mvpPlusPlusPlayers.remove(i);
                    break;
                }
            }
            
            // Add them at the beginning if found
            if (lastFoundPlayerObj != null) {
                reorderedPlayers.add(lastFoundPlayerObj);
                reorderedPlayers.addAll(mvpPlusPlusPlayers);
                mvpPlusPlusPlayers = reorderedPlayers;
                sendMessage(EnumChatFormatting.YELLOW + "Prioritizing previously found player: " + 
                    EnumChatFormatting.GOLD + lastFoundPlayer);
            }
        }
        
        running.set(true);
        count.set(0);
        
        sendMessage(EnumChatFormatting.GREEN + "Starting team spam with " + mvpPlusPlusPlayers.size() + " MVP++ players...");
        
        // Schedule the spam task
        currentTask = scheduler.scheduleWithFixedDelay(() -> {
            try {
                if (!running.get()) {
                    return;  // Stop if running was set to false
                }
                
                // Check if we're already processing an invite
                if (isProcessing.get()) {
                    return;  // Skip if we're still processing the previous invite
                }
                
                isProcessing.set(true);  // Set lock
                
                // Get a fresh list of MVP++ players in case it's been updated
                List<Player> currentPlayers = new ArrayList<>();
                for (Player player : TeamSpamManager.getPlayers()) {
                    if (player.hasMVPPlusPlus()) {
                        currentPlayers.add(player);
                    }
                }
                
                if (currentPlayers.isEmpty()) {
                    stopTeamSpam();
                    sendMessage(EnumChatFormatting.RED + "No MVP++ players left in team spam list. Stopping.");
                    return;
                }
                
                int index = count.getAndIncrement() % currentPlayers.size();
                Player player = currentPlayers.get(index);
                
                // Store the player's name before sending the invite
                lastInvitedPlayer = player.getUsername();
                
                // Send team invite (using /team instead of /party)
                Minecraft.getMinecraft().thePlayer.sendChatMessage("/team invite " + lastInvitedPlayer);
                
                // Update the player's timestamp to move them to the end of the list
                TeamSpamManager.updatePlayerTimestamp(lastInvitedPlayer);
                
            } catch (Exception e) {
                System.err.println("Error in team spam task: " + e.getMessage());
                e.printStackTrace();
            } finally {
                isProcessing.set(false);  // Release lock
            }
        }, 0, delay.get(), TimeUnit.MILLISECONDS);
    }
    
    private void showPlayers() {
        List<Player> players = TeamSpamManager.getPlayers();
        
        if (players.isEmpty()) {
            sendMessage(EnumChatFormatting.RED + "No players in team spam list. Add players with /tspam add <username>");
            return;
        }
        
        if (TeamSpamManager.isUsingResourceFile()) {
            sendMessage(EnumChatFormatting.YELLOW + "Using default player list. Type /tspam confirm to save this list as your own.");
        }
        
        sendMessage(EnumChatFormatting.GREEN + "Players in team spam list (" + players.size() + "):");
        
        // Sort players: MVP++ first, then alphabetically
        List<Player> sortedPlayers = new ArrayList<>(players);
        Collections.sort(sortedPlayers, (p1, p2) -> {
            // First sort by MVP++ status
            if (p1.hasMVPPlusPlus() && !p2.hasMVPPlusPlus()) {
                return -1;
            } else if (!p1.hasMVPPlusPlus() && p2.hasMVPPlusPlus()) {
                return 1;
            }
            // Then sort alphabetically
            return p1.getUsername().compareToIgnoreCase(p2.getUsername());
        });
        
        // Display players one per line with their added date
        for (int i = 0; i < sortedPlayers.size(); i++) {
            Player player = sortedPlayers.get(i);
            StringBuilder entry = new StringBuilder();
            entry.append((i + 1)).append(". ");
            
            if (player.hasMVPPlusPlus()) {
                entry.append(EnumChatFormatting.GOLD).append(player.getUsername())
                     .append(EnumChatFormatting.YELLOW).append(" [MVP++]");
            } else {
                entry.append(EnumChatFormatting.WHITE).append(player.getUsername());
            }
            
            // Add the date when the player was added
            String dateAdded = DATE_FORMAT.format(new Date(player.getAddedTimestamp()));
            entry.append(EnumChatFormatting.GRAY).append(" (Added: ").append(dateAdded).append(")");
            
            sendMessage(entry.toString());
        }
    }
    
    private void refreshPlayers() {
        sendMessage(EnumChatFormatting.YELLOW + "Refreshing player list... This might take a while.");
        
        // Run the refresh in a separate thread to avoid blocking the game
        new Thread(() -> {
            try {
                String result = TeamSpamManager.checkAllPlayersMVPPlusPlus();
                sendMessage(result);
            } catch (Exception e) {
                sendMessage(EnumChatFormatting.RED + "Error refreshing players: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
    
    private void sendMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
} 