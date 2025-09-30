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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command to check which friends are playing Blitz Survival Games.
 */
public class FriendSpamCommand extends Command {
    private boolean isScanning = false;
    private int currentFriendListPage = 1;
    private long lastCommandTime = 0;
    private static final long DELAY_MS = 2000; // 2 seconds between commands
    private static final long PAGE_TIMEOUT_MS = 5000; // 5 seconds timeout per page
    private boolean isWaitingForResponse = false;
    private long pageStartTime = 0; // Track when we started scanning a page
    // Update pattern to be more flexible with "Game" vs "Games" and potential variations
    private static final Pattern BSG_GAME_PATTERN = Pattern.compile("(.*) is in a Blitz Survival Game.*\\((.+)\\)");
    // Add alternative pattern to catch more formats
    private static final Pattern BSG_ALT_PATTERN = Pattern.compile("(.*) is (?:playing|in) (?:a )?(?:Blitz|BSG).*");
    private List<BSGPlayer> bsgPlayers = new ArrayList<>();
    private Map<String, Boolean> playerMVPStatus = new HashMap<>();
    
    // Add a variable to track total pages (default to a large number)
    private int totalPages = 10; 
    // Track if we've detected page info at least once
    private boolean hasDetectedPageInfo = false;
    // Add a flag to indicate we found an offline player and should stop after this page
    private boolean foundOfflinePlayer = false;

    private static class BSGPlayer {
        String name;
        String gameInfo;
        String mode;
        String map;
        boolean isMVPPlus;
        
        BSGPlayer(String name, String gameInfo, boolean isMVPPlus) {
            this.name = name;
            this.gameInfo = gameInfo;
            this.isMVPPlus = isMVPPlus;
            
            // Parse the gameInfo to extract mode and map
            if (gameInfo.contains("-")) {
                String[] parts = gameInfo.split(" - ", 2);
                this.mode = parts[0].trim();
                this.map = parts.length > 1 ? parts[1].trim() : "Unknown";
            } else {
                this.mode = "Unknown";
                this.map = gameInfo;
            }
        }
    }
    
    public FriendSpamCommand() {
        super("fspam");
        EventBus.subscribe(this);
    }
    
    @Override
    public void handle(String[] args) {
        if (isScanning) {
            stopScanning();
            sendMessage(EnumChatFormatting.RED + "Friend list scanning stopped.");
        } else {
            startScanning();
            sendMessage(EnumChatFormatting.GREEN + "Starting friend list scan...");
        }
    }
    
    private void startScanning() {
        isScanning = true;
        isWaitingForResponse = false;
        currentFriendListPage = 1;
        hasDetectedPageInfo = false;
        foundOfflinePlayer = false; // Reset the offline player flag
        bsgPlayers.clear();
        playerMVPStatus.clear();
        System.out.println("[FSpam] Starting scan");
        sendMessage(EnumChatFormatting.GREEN + "Starting friend list scan...");
        pageStartTime = System.currentTimeMillis();
        sendFriendListCommand();
    }
    
    private void stopScanning() {
        isScanning = false;
        displayResults();
    }
    
    private void sendFriendListCommand() {
        if (!isScanning) return;
        
        String command = currentFriendListPage == 1 ? "/fl" : "/fl " + currentFriendListPage;
        Minecraft.getMinecraft().thePlayer.sendChatMessage(command);
        lastCommandTime = System.currentTimeMillis();
        pageStartTime = System.currentTimeMillis();
        isWaitingForResponse = true;
        
        // Debug message
        sendMessage(EnumChatFormatting.GRAY + "Checking page " + currentFriendListPage + "...");
        System.out.println("[FSpam Debug] Sent command: " + command);
    }
    
    private void displayResults() {
        System.out.println("[FSpam Debug] Displaying results, BSG players found: " + bsgPlayers.size());
        
        // Print the list of found players for debugging
        for (BSGPlayer player : bsgPlayers) {
            System.out.println("[FSpam Debug] Found player: " + player.name + " - " + player.gameInfo);
        }
        
        if (bsgPlayers.isEmpty()) {
            sendMessage(EnumChatFormatting.YELLOW + "No friends currently playing Blitz Survival Games.");
            return;
        }
        
        // Sort players: non-MVP++ first, then MVP++ at the bottom
        bsgPlayers.sort(Comparator.comparing(p -> p.isMVPPlus));
        
        // Display header
        sendMessage(EnumChatFormatting.GREEN + "Friends currently playing Blitz Survival Games:");
        
        // Display each player
        for (BSGPlayer player : bsgPlayers) {
            String mvpPrefix = player.isMVPPlus ? EnumChatFormatting.GOLD + "[MVP++] " : "";
            String modeDisplay = player.mode != null && !player.mode.isEmpty() 
                            ? EnumChatFormatting.AQUA + " - Mode: " + player.mode : "";
            String mapDisplay = player.map != null && !player.map.isEmpty() 
                            ? EnumChatFormatting.GREEN + " - Map: " + player.map : "";
            
            sendMessage(mvpPrefix + EnumChatFormatting.YELLOW + player.name + modeDisplay + mapDisplay);
        }
        
        // Play a sound to notify the user that the scan is complete
        playNoteSound();
    }
    
    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (!isScanning) return;
        
        long currentTime = System.currentTimeMillis();
        
        // Check for page timeout - move to next page if we've waited too long
        if (isWaitingForResponse && (currentTime - pageStartTime) >= PAGE_TIMEOUT_MS) {
            System.out.println("[FSpam Debug] Page timeout, moving to next page");
            
            // If we've already reached the maximum number of pages, stop scanning
            if (currentFriendListPage >= totalPages) {
                System.out.println("[FSpam Debug] Reached max pages, stopping scan");
                isScanning = false;
                displayResults();
                return;
            }
            
            // Move to next page after timeout
            isWaitingForResponse = false;
            currentFriendListPage++;
        }
        
        // Send next command if we're not waiting and enough time has passed
        if (!isWaitingForResponse && (currentTime - lastCommandTime) >= DELAY_MS) {
            sendFriendListCommand();
        }
    }
    
    @SubscribeEvent
    public void onChat(Receive event) {
        if (!isScanning || !(event.getPacket() instanceof S02PacketChat)) return;
        
        String unformattedMessage = ((S02PacketChat) event.getPacket()).getChatComponent().getUnformattedText();
        String formattedMessage = ((S02PacketChat) event.getPacket()).getChatComponent().getFormattedText();
        
        // Debug ALL messages to see what's happening
        System.out.println("[FSpam RAW Unformatted] " + unformattedMessage);
        System.out.println("[FSpam RAW Formatted] " + formattedMessage);
        
        // Check for known BSG player format from the example
        if (unformattedMessage.contains("taunTted is in a Blitz Survival Games Game")) {
            System.out.println("[FSpam Debug] DIRECT HIT - Found taunTted in BSG!");
            addBSGPlayer("taunTted", "Blitz Survival Games Game", formattedMessage);
        }
        
        // Manual check for friend list entries
        if (unformattedMessage.contains(" is in ")) {
            // This looks like a friend list entry
            String[] parts = unformattedMessage.split(" is in ", 2);
            if (parts.length == 2) {
                String playerName = parts[0].trim();
                String activity = parts[1].trim();
                
                if (activity.contains("Blitz") || activity.contains("BSG") || activity.contains("Survival Games")) {
                    System.out.println("[FSpam Debug] Found BSG player via manual check: " + playerName);
                    addBSGPlayer(playerName, "in " + activity, formattedMessage);
                }
            }
        }
        
        // Skip separator lines
        if (unformattedMessage.matches(".*-{5,}.*")) {
            System.out.println("[FSpam Debug] Skipping separator line");
            isWaitingForResponse = false;
            return;
        }
        
        // FIXED: Proper offline detection - must contain words and not just separator lines
        if (unformattedMessage.contains("is") && unformattedMessage.contains("offline")) {
            System.out.println("[FSpam Debug] ********** FOUND OFFLINE PLAYER **********");
            System.out.println("[FSpam Debug] Message: " + unformattedMessage);
            
            // Set the flag to stop after this page but continue processing the current page
            foundOfflinePlayer = true;
            sendMessage(EnumChatFormatting.YELLOW + "Found offline player on page " + currentFriendListPage + ". Will stop after this page.");
        }
        
        // Check for BSG players with the original patterns
        checkForBSGPlayer(formattedMessage, true);
        checkForBSGPlayer(unformattedMessage, false);
        
        // More robust page detection - check for various forms of page indication
        if (unformattedMessage.contains("You are on page") || unformattedMessage.contains("Page ") || unformattedMessage.contains(" of ")) {
            System.out.println("[FSpam Debug] Page info detected: " + unformattedMessage);
            isWaitingForResponse = false; // We got a response
            
            // Consider a page completely processed after we see page info or separator
            if (foundOfflinePlayer) {
                System.out.println("[FSpam Debug] Finished scanning page " + currentFriendListPage + " where offline player was found. Stopping scan.");
                isScanning = false;
                // Use a small delay before displaying results to ensure all messages are processed
                new Thread(() -> {
                    try {
                        // Wait 1 second for all messages to be processed
                        Thread.sleep(1000);
                        displayResults();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                return;
            }
            
            try {
                // Try to extract current page and total pages information from message
                String[] parts = unformattedMessage.split(" ");
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].equalsIgnoreCase("page") && i + 1 < parts.length) {
                        try {
                            // Try to find current page number
                            int detectedPage = Integer.parseInt(parts[i+1].replace(",", "").replace(".", ""));
                            System.out.println("[FSpam Debug] Detected page number: " + detectedPage);
                            
                            // Try to find total pages
                            if (unformattedMessage.contains(" of ") && i + 3 < parts.length && parts[i+2].equalsIgnoreCase("of")) {
                                try {
                                    totalPages = Integer.parseInt(parts[i+3].replace(".", "").replace(",", ""));
                                    System.out.println("[FSpam Debug] Total pages: " + totalPages);
                                } catch (NumberFormatException e) {
                                    System.out.println("[FSpam Debug] Couldn't parse total pages: " + e.getMessage());
                                }
                            }
                            
                            // If we're already at or past the last page, stop
                            if (detectedPage >= totalPages) {
                                System.out.println("[FSpam Debug] Reached last page, stopping scan");
                                isScanning = false;
                                displayResults();
                                return;
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("[FSpam Debug] Couldn't parse page number: " + e.getMessage());
                        }
                    }
                }
                
                // Continue to next page only if no offline player was found
                currentFriendListPage++;
                System.out.println("[FSpam Debug] Moving to page: " + currentFriendListPage);
                
            } catch (Exception e) {
                System.out.println("[FSpam Debug] Error parsing page info: " + e.getMessage());
                currentFriendListPage++;
            }
        }
        
        // If we get any friend list header or indication we've received the page
        if (unformattedMessage.contains("Friend List") || 
            unformattedMessage.contains("FRIENDS") || 
            unformattedMessage.contains("FRIEND LIST") || 
            (unformattedMessage.contains("(") && unformattedMessage.contains(")"))) {
            System.out.println("[FSpam Debug] Detected friend list message, marking page as received");
            isWaitingForResponse = false; // We got a response for this page
        }
    }
    
    private void checkForBSGPlayer(String message, boolean isFormatted) {
        // Log the raw message for inspection
        System.out.println("[FSpam DEBUG CHECK] Checking message for BSG: " + message);
        
        // Try multiple patterns to catch BSG players - UPDATED TO MATCH "Blitz Survival Games Game"
        String[] patterns = {
            " is in a Blitz Survival Games Game",
            " is in a Blitz Survival Game",
            " is playing Blitz",
            " is in Blitz",
            " is in BSG",
            " is playing BSG",
            "Blitz Survival Games"
        };
        
        // Direct check for taunTted specifically based on the example
        if (message.contains("taunTted") && message.contains("Blitz Survival Games")) {
            System.out.println("[FSpam Debug] DIRECT MATCH FOR taunTted!");
            addBSGPlayer("taunTted", "Blitz Survival Games Game", isFormatted ? message : null);
            return;
        }
        
        // Super simple check - if message has both a player name and mentions Blitz
        if (message.contains(" is in") && message.contains("Blitz")) {
            String[] parts = message.split(" is in", 2);
            if (parts.length > 0) {
                String playerName = parts[0].trim();
                String gameInfo = parts.length > 1 ? "in" + parts[1].trim() : "Blitz Game";
                
                System.out.println("[FSpam Debug] SIMPLE CHECK - FOUND BSG PLAYER: " + playerName);
                addBSGPlayer(playerName, gameInfo, isFormatted ? message : null);
                return;
            }
        }
        
        // Original pattern matching
        for (String pattern : patterns) {
            if (message.contains(pattern)) {
                String[] parts = message.split(" is ", 2);
                if (parts.length > 0) {
                    String playerName = parts[0].trim();
                    String gameInfo = parts.length > 1 ? parts[1].trim() : "Unknown";
                    
                    System.out.println("[FSpam Debug] !!!!!!!! POTENTIAL BSG PLAYER FOUND !!!!!!!!");
                    System.out.println("[FSpam Debug] Pattern matched: " + pattern);
                    System.out.println("[FSpam Debug] Player: " + playerName);
                    System.out.println("[FSpam Debug] Game info: " + gameInfo);
                    
                    addBSGPlayer(playerName, gameInfo, isFormatted ? message : null);
                    break; // Found a match, no need to check other patterns
                }
            }
        }
    }
    
    // Helper method to add a BSG player to the list
    private void addBSGPlayer(String playerName, String gameInfo, String formattedMessage) {
        // Ignore empty names
        if (playerName == null || playerName.trim().isEmpty()) {
            return;
        }
        
        System.out.println("[FSpam Debug] Attempting to add player: " + playerName);
        
        // Check if we already added this player
        boolean alreadyAdded = false;
        for (BSGPlayer player : bsgPlayers) {
            if (player.name.equalsIgnoreCase(playerName)) {
                alreadyAdded = true;
                break;
            }
        }
        
        if (!alreadyAdded) {
            boolean isMVPPlus = (formattedMessage != null && (formattedMessage.contains("§6") || formattedMessage.contains("§g"))) || 
                               (playerMVPStatus.containsKey(playerName) && playerMVPStatus.get(playerName));
            
            playerMVPStatus.put(playerName, isMVPPlus);
            bsgPlayers.add(new BSGPlayer(playerName, gameInfo, isMVPPlus));
            System.out.println("[FSpam Debug] SUCCESSFULLY ADDED BSG PLAYER: " + playerName);
        } else {
            System.out.println("[FSpam Debug] Player already in list: " + playerName);
        }
    }
    
    private void playNoteSound() {
        Minecraft.getMinecraft().getSoundHandler().playSound(
            PositionedSoundRecord.create(
                new ResourceLocation("note.pling"), 1.0F
            )
        );
    }
    
    private void sendMessage(String message) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(
            EnumChatFormatting.DARK_PURPLE + "[FSpam] " + EnumChatFormatting.RESET + message
        ));
    }
} 