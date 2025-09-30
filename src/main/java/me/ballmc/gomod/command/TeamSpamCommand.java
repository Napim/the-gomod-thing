package me.ballmc.gomod.command;

import net.weavemc.loader.api.command.Command;
import me.ballmc.gomod.Main;
import me.ballmc.gomod.features.TeamSpamManager;
import me.ballmc.gomod.features.TeamSpamManager.Player;
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
    private static final AtomicInteger delay = new AtomicInteger(900); // ms between invites
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

    // Public accessor for GUI toggle state
    public static boolean isRunning() {
        return running.get();
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
                    final String usernameToAdd = args[1];
                    sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Adding player " + usernameToAdd + "...");
                    new Thread(() -> {
                        String result = TeamSpamManager.addPlayer(usernameToAdd);
                        sendMessage(result);
                    }).start();
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
        
        sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Team spam stopped.");
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
            sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "No MVP++ players in team spam list. Add MVP++ players with /tspam add <username>");
            return;
        }
        
        // Sort players by priority: most recently found first, then most recently added, then MVP++ first, then alphabetically
        Collections.sort(mvpPlusPlusPlayers, (p1, p2) -> {
            // Primary: most recently found first
            long found1 = p1.getLastFoundTimestamp();
            long found2 = p2.getLastFoundTimestamp();
            boolean p1HasFound = found1 > 0;
            boolean p2HasFound = found2 > 0;

            // Players with a lastFoundTimestamp appear before those without
            if (p1HasFound && !p2HasFound) return -1;
            if (!p1HasFound && p2HasFound) return 1;

            // If both have found timestamps, newest first
            if (p1HasFound && p2HasFound && found1 != found2) {
                return Long.compare(found2, found1);
            }

            // Secondary: most recently added first among players with same found status
            long added1 = p1.getAddedTimestamp();
            long added2 = p2.getAddedTimestamp();
            int addedCompare = Long.compare(added2, added1);
            if (addedCompare != 0) return addedCompare;

            // Tertiary: MVP++ first (though all should be MVP++ already)
            if (p1.hasMVPPlusPlus() && !p2.hasMVPPlusPlus()) return -1;
            if (!p1.hasMVPPlusPlus() && p2.hasMVPPlusPlus()) return 1;

            // Quaternary: alphabetical by username
            return p1.getUsername().compareToIgnoreCase(p2.getUsername());
        });
        
        // Debug: Log the initial sorted order
        System.out.println("[TeamSpamCommand] Initial sorted player order:");
        for (int i = 0; i < Math.min(5, mvpPlusPlusPlayers.size()); i++) {
            Player p = mvpPlusPlusPlayers.get(i);
            System.out.println("  " + (i+1) + ". " + p.getUsername() + " (found: " + p.getLastFoundTimestamp() + ")");
        }
        
        // Removed verbose notice about prioritizing recently found player
        
        running.set(true);
        count.set(0);
        
        sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN + "Starting team spam with " + mvpPlusPlusPlayers.size() + " MVP++ players...");
        
        // Schedule the spam task to invite all players once, then stop
        scheduleNextInvite();
    }
    
    private void scheduleNextInvite() {
        if (!running.get()) {
            return;  // Stop if running was set to false
        }
        
        // Check if we're already processing an invite
        if (isProcessing.get()) {
            return;  // Skip if we're still processing the previous invite
        }
        
        isProcessing.set(true);  // Set lock
        
        try {
            // Get a fresh list of MVP++ players in case it's been updated
            List<Player> currentPlayers = new ArrayList<>();
            for (Player player : TeamSpamManager.getPlayers()) {
                if (player.hasMVPPlusPlus()) {
                    currentPlayers.add(player);
                }
            }
            
            // Sort players by priority: most recently found first, then most recently added, then MVP++ first, then alphabetically
            Collections.sort(currentPlayers, (p1, p2) -> {
                // Primary: most recently found first
                long found1 = p1.getLastFoundTimestamp();
                long found2 = p2.getLastFoundTimestamp();
                boolean p1HasFound = found1 > 0;
                boolean p2HasFound = found2 > 0;

                // Players with a lastFoundTimestamp appear before those without
                if (p1HasFound && !p2HasFound) return -1;
                if (!p1HasFound && p2HasFound) return 1;

                // If both have found timestamps, newest first
                if (p1HasFound && p2HasFound && found1 != found2) {
                    return Long.compare(found2, found1);
                }

                // Secondary: most recently added first among players with same found status
                long added1 = p1.getAddedTimestamp();
                long added2 = p2.getAddedTimestamp();
                int addedCompare = Long.compare(added2, added1);
                if (addedCompare != 0) return addedCompare;

                // Tertiary: MVP++ first (though all should be MVP++ already)
                if (p1.hasMVPPlusPlus() && !p2.hasMVPPlusPlus()) return -1;
                if (!p1.hasMVPPlusPlus() && p2.hasMVPPlusPlus()) return 1;

                // Quaternary: alphabetical by username
                return p1.getUsername().compareToIgnoreCase(p2.getUsername());
            });
            
            // Debug: Log the sorted order
            System.out.println("[TeamSpamCommand] Sorted player order:");
            for (int i = 0; i < Math.min(5, currentPlayers.size()); i++) {
                Player p = currentPlayers.get(i);
                System.out.println("  " + (i+1) + ". " + p.getUsername() + " (found: " + p.getLastFoundTimestamp() + ")");
            }
            
            if (currentPlayers.isEmpty()) {
                stopTeamSpam();
                sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "No MVP++ players left in team spam list. Stopping.");
                return;
            }
            
            int currentIndex = count.get();
            if (currentIndex >= currentPlayers.size()) {
                // We've invited all players once, stop the spam
                stopTeamSpam();
                sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN + "Finished inviting all " + currentPlayers.size() + " MVP++ players. Stopping.");
                return;
            }
            
            Player player = currentPlayers.get(currentIndex);
            count.incrementAndGet();  // Increment after getting the player
            
            // Store the player's name before sending the invite
            lastInvitedPlayer = player.getUsername();
            
            // Send team invite (using /team instead of /party)
            Minecraft.getMinecraft().thePlayer.sendChatMessage("/team invite " + lastInvitedPlayer);
            
            // DO NOT update timestamp here - wait for confirmation from TeamInviteResponseHandler
            // that the player is actually online before marking them as "found"
            
            // Schedule the next invite after the delay
            currentTask = scheduler.schedule(() -> {
                isProcessing.set(false);  // Release lock
                scheduleNextInvite();  // Schedule next invite
            }, delay.get(), TimeUnit.MILLISECONDS);
            
        } catch (Exception e) {
            System.err.println("Error in team spam task: " + e.getMessage());
            e.printStackTrace();
            isProcessing.set(false);  // Release lock
        }
    }
    
    private void showPlayers() {
        List<Player> players = TeamSpamManager.getPlayers();
        
        if (players.isEmpty()) {
            sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "No players in team spam list. Add players with /tspam add <username>");
            return;
        }
        
        if (TeamSpamManager.isUsingResourceFile()) {
            sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Using default player list. Type /tspam confirm to save this list as your own.");
        }
        
        sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN + "Players in team spam list (" + players.size() + "):");
        
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
        // Enforce a 10-minute cooldown between refreshes
        if (!RefreshCooldown.allow()) {
            long waitMs = RefreshCooldown.timeRemainingMs();
            long mins = waitMs / 60000;
            long secs = (waitMs % 60000) / 1000;
            sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Refresh is on cooldown. Try again in " + mins + "m " + secs + "s.");
            return;
        }

        sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Refreshing player list... This might take a while.");
        
        // Run the refresh in a separate thread to avoid blocking the game
        new Thread(() -> {
            try {
                String result = TeamSpamManager.checkAllPlayersMVPPlusPlus();
                sendMessage(Main.CHAT_PREFIX + result);
            } catch (Exception e) {
                sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Error refreshing players: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    // Simple in-process cooldown helper
    public static class RefreshCooldownPublic {
        public static boolean allow() { return RefreshCooldown.allow(); }
        public static long timeRemainingMs() { return RefreshCooldown.timeRemainingMs(); }
    }

    private static class RefreshCooldown {
        private static final long COOLDOWN_MS = 10 * 60 * 1000L; // 10 minutes
        private static long lastRefreshAt = 0L;

        static synchronized boolean allow() {
            long now = System.currentTimeMillis();
            if (now - lastRefreshAt >= COOLDOWN_MS) {
                lastRefreshAt = now;
                return true;
            }
            return false;
        }

        static synchronized long timeRemainingMs() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefreshAt;
            return elapsed >= COOLDOWN_MS ? 0L : (COOLDOWN_MS - elapsed);
        }
    }
    
    private void sendMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
}
