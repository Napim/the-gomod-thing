package me.ballmc.gomod.features;

import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.event.SubscribeEvent;
import net.weavemc.loader.api.event.RenderGameOverlayEvent;
import net.weavemc.loader.api.event.PacketEvent.Receive;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ChatComponentText;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Score;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.network.play.server.S3EPacketTeams;

import java.util.*;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TAB Stats feature that tracks and displays player kills during a game.
 * Shows a GUI in the top-left corner with kill counts for each player.
 * 
 * Features:
 * - Kill tracking and display in tab list
 * - Teammate kit detection from chat messages
 * - Enemy kit detection from gear items (5 seconds after players receive kits)
 * - Guild tag display
 * - Kill effect colors
 * - Stats display (WL/Wins)
 * 
 * Enemy Kit Detection:
 * - Activates 5 seconds after "The Blitz Star will be released in 2 minutes!" message
 * - Scans player gear for 1 second only to avoid false positives from looted items
 * - Scans player gear for kit items matching pattern: "KitName's Item (Level)"
 * - Detects all Blitz Survival Games kits with levels I-X or ✫/✫✫
 * - Displays detected enemy kits in yellow parentheses in tab list
 */
public class KillCounter {
    private static boolean enabled = true; // Default to enabled
    private static boolean statsDisplay = true; // Default to showing stats display
    private static boolean showColors = true; // Default to showing kill effect colors
    private static int wlMode = 1; // 0 = wl only, 1 = wl + w, 2 = w only, 3 = username only (default to wl + w)
    private static final Map<String, Integer> killCounts = new ConcurrentHashMap<>();
    private static final Map<String, String> playerColors = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> playerVictims = new ConcurrentHashMap<>();
    private static final Map<String, String> playerKits = new ConcurrentHashMap<>(); // Store player kits
    private static final Map<String, String> enemyPlayerKits = new ConcurrentHashMap<>(); // Store enemy player kits detected from gear
    private static long lastRefresh = 0L;
    private static final Set<String> deadPlayers = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static String lastWorldName = null; // Track world changes
    private static final Map<String, String> playerGuildTags = new ConcurrentHashMap<>(); // Cache guild tags
    private static final Map<String, String> playerGuildTagColors = new ConcurrentHashMap<>(); // Cache guild tag colors
    private static long cagesOpenTime = 0L; // Track when cages opened for kit detection timing
    private static boolean kitDetectionActive = false; // Track if we should be detecting kits from gear
    private static long lastGameEndCheck = 0L; // Track when we last checked for game end
    private static final long GAME_END_CHECK_INTERVAL = 2000L; // Check every 2 seconds
    private static boolean gameHasEnded = false; // Flag to track if game has actually ended
    private static boolean blitzHourActive = false; // Track if BLITZ HOUR is active from scoreboard
    private static long blitzHourDetectionTime = 0L; // Track when BLITZ HOUR was detected
    private static final long BLITZ_HOUR_TIMEOUT = 30000L; // 30 seconds timeout for BLITZ HOUR detection
    
    // Rate limiting for API calls
    private static long lastApiCall = 0;
    private static final long API_CALL_DELAY = 1000; // 1 second between calls
    
    /**
     * Initializes KillCounter settings from ConfigManager
     */
    public static void initializeSettings() {
        enabled = ConfigManager.isKillCounterEnabled();
        System.out.println("[KillCounter] Initialized with enabled state: " + enabled);
    }
    
    // Patterns to detect kill messages
    private static final String[] KILL_PATTERNS = {
        "was killed by",
        "was slain by",
        "was shot by",
        "was blown up by",
        "was burned to death by",
        "was frozen to death by",
        "was electrocuted by",
        "was killed by",
        "was eliminated by",
        "was defeated by"
    };
    
    // Pattern to detect kit selection messages
    private static final String KIT_SELECTION_PATTERN = "has picked their";
    
    // Kit names for Blitz Survival Games
    private static final String[] KIT_NAMES = {
        "Archer", "Meatmaster", "Speleologist", "Baker", "Knight", "Guardian",
        "Scout", "Hunter", "Hype Train", "Fisherman", "Armorer", "Horsetamer",
        "Astronaut", "Troll", "Reaper", "Shark", "RedDragon", "Toxicologist",
        "Rogue", "Warlock", "SlimeySlime", "Jockey", "Golem", "Viking",
        "Shadow Knight", "Pigman", "Paladin", "Necromancer", "Florist", "Diver",
        "Arachnologist", "Blaze", "Wolftamer", "Tim", "Farmer", "Creepertamer",
        "Snowman", "Rambo", "Ranger", "Donkeytamer", "Phoenix", "Warrior", "Milkman"
    };
    
    // Regex pattern to detect kit items from gear (e.g., "Guardian's Stone Axe (VI)" or "Armorer's Leather Boots (?)")
    private static final java.util.regex.Pattern KIT_ITEM_PATTERN = 
        java.util.regex.Pattern.compile("^([A-Za-z]+)'s\\s+.*?\\(([IVX\\?✫]+)\\)$", java.util.regex.Pattern.CASE_INSENSITIVE);
    
    /**
     * Starts TAB Stats.
     */
    public static void start() {
        enabled = true;
        // Sync with ConfigManager
        ConfigManager.setKillCounterEnabled(true);
        killCounts.clear();
        playerColors.clear();
        playerKits.clear();
        enemyPlayerKits.clear();
        deadPlayers.clear();
        cagesOpenTime = 0L;
        kitDetectionActive = false;
        gameHasEnded = false; // Reset game end flag for new game
        blitzHourActive = false; // Reset BLITZ HOUR flag for new game
        blitzHourDetectionTime = 0L; // Reset BLITZ HOUR detection time
        // Initialize world name tracking
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld != null) {
                lastWorldName = mc.theWorld.getWorldInfo().getWorldName();
            }
        } catch (Exception e) {
            lastWorldName = null;
        }
        System.out.println("[KillCounter] TAB Stats started!");
        updateAllTabNames();
    }
    
    /**
     * Stops TAB Stats.
     */
    public static void stop() {
        enabled = false;
        // Sync with ConfigManager
        ConfigManager.setKillCounterEnabled(false);
        deadPlayers.clear();
        System.out.println("[KillCounter] TAB Stats stopped!");
        resetAllTabNames();
    }
    
    /**
     * Stops TAB Stats temporarily (for world changes) without changing user settings.
     */
    public static void stopTemporarily() {
        enabled = false;
        // DON'T change ConfigManager settings - keep user's preference
        deadPlayers.clear();
        gameHasEnded = false; // Reset game end flag for new world
        blitzHourActive = false; // Reset BLITZ HOUR flag for new world
        blitzHourDetectionTime = 0L; // Reset BLITZ HOUR detection time
        System.out.println("[KillCounter] TAB Stats stopped temporarily for world change!");
        resetAllTabNames();
    }
    
    /**
     * Resets TAB Stats.
     */
    public static void reset() {
        killCounts.clear();
        playerColors.clear();
        playerVictims.clear();
        playerKits.clear();
        enemyPlayerKits.clear();
        deadPlayers.clear();
        cagesOpenTime = 0L;
        kitDetectionActive = false;
        gameHasEnded = false; // Reset game end flag
        blitzHourActive = false; // Reset BLITZ HOUR flag
        blitzHourDetectionTime = 0L; // Reset BLITZ HOUR detection time
        System.out.println("[KillCounter] TAB Stats reset!");
        updateAllTabNames();
    }
    
    /**
     * Checks if the kill counter is enabled.
     */
    public static boolean isEnabled() {
        return ConfigManager.isKillCounterEnabled();
    }
    
    public static void setEnabled(boolean enabled) {
        ConfigManager.setKillCounterEnabled(enabled);
        KillCounter.enabled = enabled; // Keep internal state for immediate use
    }
    
    public static boolean isStatsDisplay() {
        return statsDisplay;
    }
    
    public static void setStatsDisplay(boolean statsDisplay) {
        KillCounter.statsDisplay = statsDisplay;
    }
    
    public static boolean isShowColors() {
        return showColors;
    }
    
    public static void setShowColors(boolean showColors) {
        KillCounter.showColors = showColors;
    }
    
    public static int getWlMode() {
        return wlMode;
    }
    
    public static void setWlMode(int wlMode) {
        KillCounter.wlMode = wlMode;
    }
    
    public static boolean isShowTeammateKits() {
        return me.ballmc.gomod.features.ConfigManager.isKillCounterShowTeammateKits();
    }
    
    public static void setShowTeammateKits(boolean showTeammateKits) {
        me.ballmc.gomod.features.ConfigManager.setKillCounterShowTeammateKits(showTeammateKits);
    }
    
    public static boolean isShowEnemyKits() {
        return me.ballmc.gomod.features.ConfigManager.isKillCounterShowEnemyKits();
    }
    
    public static void setShowEnemyKits(boolean showEnemyKits) {
        me.ballmc.gomod.features.ConfigManager.setKillCounterShowEnemyKits(showEnemyKits);
    }
    
    public static boolean isShowKeAllChat() {
        return me.ballmc.gomod.features.ConfigManager.isKillCounterShowKeAllChat();
    }
    
    public static void setShowKeAllChat(boolean showKeAllChat) {
        me.ballmc.gomod.features.ConfigManager.setKillCounterShowKeAllChat(showKeAllChat);
    }
    
    public static boolean isAutoKeAll() {
        return me.ballmc.gomod.features.ConfigManager.isKillCounterAutoKeAll();
    }
    
    public static void setAutoKeAll(boolean autoKeAll) {
        me.ballmc.gomod.features.ConfigManager.setKillCounterAutoKeAll(autoKeAll);
    }
    
    public static boolean isShowGuildTag() {
        return me.ballmc.gomod.features.ConfigManager.isKillCounterShowGuildTag();
    }
    
    public static void setShowGuildTag(boolean showGuildTag) {
        me.ballmc.gomod.features.ConfigManager.setKillCounterShowGuildTag(showGuildTag);
        System.out.println("[KillCounter] Guild tag display set to: " + showGuildTag);
    }
    
    /**
     * Fetches guild tags for all players currently in the tab list
     */
    private static void fetchGuildTagsForAllPlayers() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getNetHandler() == null) {
                return;
            }
            
            System.out.println("[KillCounter] Fetching guild tags for all players in tab list...");
            
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                String playerName = info.getGameProfile().getName();
                if (playerName != null && !playerName.isEmpty() && !playerGuildTags.containsKey(playerName)) {
                    fetchGuildTag(playerName);
                }
            }
        } catch (Exception e) {
            System.err.println("[KillCounter] Error fetching guild tags for all players: " + e.getMessage());
        }
    }
    
    /**
     * Test method to manually fetch guild tag for debugging
     */
    public static void testGuildTagFetch(String playerName) {
        System.out.println("[KillCounter] Testing guild tag fetch for " + playerName);
        System.out.println("[KillCounter] Show guild tag setting: " + isShowGuildTag());
        fetchGuildTag(playerName);
    }
    
    /**
     * Gets the kill count for a specific player.
     */
    public static int getKillCount(String playerName) {
        if (playerName == null) {
            return 0;
        }
        Integer exact = killCounts.get(playerName);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, Integer> entry : killCounts.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(playerName)) {
                return entry.getValue();
            }
        }
        return 0;
    }
    
    /**
     * Gets all kill counts.
     */
    public static Map<String, Integer> getAllKillCounts() {
        return new HashMap<>(killCounts);
    }

    /**
     * Gets the maximum kill count among all players.
     */
    private static int getMaxKills() {
        int max = 0;
        for (int value : killCounts.values()) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    /**
     * Gets the maximum kill count among alive players only.
     */
    private static int getMaxKillsAlive() {
        int max = 0;
        for (Map.Entry<String, Integer> entry : killCounts.entrySet()) {
            String name = entry.getKey();
            int value = entry.getValue();
            if (!deadPlayers.contains(name.toLowerCase(Locale.ROOT)) && value > max) {
                max = value;
            }
        }
        return max;
    }
    
    /**
     * Gets the victims for a specific player.
     */
    public static List<String> getPlayerVictims(String playerName) {
        return new ArrayList<>(playerVictims.getOrDefault(playerName, new ArrayList<>()));
    }
    
    /**
     * Gets the top players sorted by kill count.
     */
    public static List<Map.Entry<String, Integer>> getTopPlayers() {
        List<Map.Entry<String, Integer>> sortedKills = new ArrayList<>(killCounts.entrySet());
        sortedKills.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return sortedKills;
    }
    
    /**
     * Gets the rank of a player (1-based, 0 if not found).
     */
    public static int getPlayerRank(String playerName) {
        List<Map.Entry<String, Integer>> topPlayers = getTopPlayers();
        for (int i = 0; i < topPlayers.size(); i++) {
            if (topPlayers.get(i).getKey().equalsIgnoreCase(playerName)) {
                return i + 1;
            }
        }
        return 0;
    }
    
    /**
     * Gets the color for a player based on their rank.
     */
    public static String getPlayerColorByRank(String playerName) {
        int rank = getPlayerRank(playerName);
        if (rank == 1) {
            return EnumChatFormatting.GOLD.toString(); // Gold for 1st only
        }
        // All others (2nd, 3rd, etc.) use default color
        return playerColors.getOrDefault(playerName, EnumChatFormatting.WHITE.toString());
    }
    
    /**
     * Gets the color for a player in the tab list (only top killer gets orange).
     */
    public static String getPlayerTabColor(String playerName) {
        if (!enabled) {
            return null;
        }
        
        int rank = getPlayerRank(playerName);
        if (rank == 1) {
            System.out.println("[KillCounter] Top killer detected for tab coloring: " + playerName);
            return EnumChatFormatting.GOLD.toString(); // Orange/Gold for top killer only
        }
        return null; // No color change for others
    }
    
    /**
     * Checks if the game has ended by looking for "Taunt:" in the scoreboard.
     * When "Taunt:" is missing AND we have kill counts, it means the game has ended.
     */
    private static boolean isGameEnded() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null || mc.theWorld.getScoreboard() == null) {
                return false;
            }
            
            // Only check for game end if we actually have kill counts (meaning we were in a game)
            if (killCounts.isEmpty()) {
                return false;
            }
            
            net.minecraft.scoreboard.Scoreboard scoreboard = mc.theWorld.getScoreboard();
            net.minecraft.scoreboard.ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1); // Sidebar
            
            if (objective == null) {
                return false;
            }
            
            // Look for "Taunt:" in the scoreboard - if it's missing, game has ended
            for (net.minecraft.scoreboard.Score score : scoreboard.getSortedScores(objective)) {
                ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
                String line = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
                
                if (line.contains("Taunt:")) {
                    System.out.println("[KillCounter] Game still active - found 'Taunt:' in scoreboard");
                    return false; // Game is still active
                }
            }
            
            System.out.println("[KillCounter] Game ended detected - no 'Taunt:' found in scoreboard and we have kill counts");
            return true; // Game has ended
        } catch (Exception e) {
            System.out.println("[KillCounter] Exception in isGameEnded: " + e.getMessage());
            return false;
        }
    }

    /**
     * Renders the kill counter GUI.
     */
    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent event) {
        // HUD rendering disabled - keeping tab list functionality only
        return;
    }
    
           /**
           * Detects teleportation/respawn packets to auto-stop TAB Stats.
            */
           @SubscribeEvent
           public void onTeleport(Receive event) {
               if (!enabled) {
                   return;
               }
               
               // Stop on server-wide changes (join game, respawn, dimension changes)
               // Only stop on actual server joins, not respawns (which happen during normal gameplay)
               if (event.getPacket() instanceof S01PacketJoinGame) {
                   stopTemporarily(); // Don't change user settings, just clear current game data
                    System.out.println("[KillCounter] Auto-stopped TAB Stats temporarily due to server join!");
               }
               // Re-apply tab names on tab list or team updates (server can rewrite formatting)
               if (event.getPacket() instanceof S38PacketPlayerListItem ||
                   event.getPacket() instanceof S3EPacketTeams) {
                   updateAllTabNames();
               }
               
               // Update world name tracking for debugging (but don't auto-stop)
               try {
                   Minecraft mc = Minecraft.getMinecraft();
                   if (mc.theWorld != null) {
                       String currentWorldName = mc.theWorld.getWorldInfo().getWorldName();
                       if (lastWorldName != null && !lastWorldName.equals(currentWorldName)) {
                           System.out.println("[KillCounter] World changed: " + lastWorldName + " -> " + currentWorldName + " (not auto-stopping)");
                       }
                       lastWorldName = currentWorldName;
                   }
               } catch (Exception e) {
                   // Ignore world change detection errors
               }
           }

           /**
            * Processes chat messages to detect kills and auto-start.
            */
           @SubscribeEvent
           public void onChat(Receive event) {
        if (!(event.getPacket() instanceof S02PacketChat)) {
            return;
        }
        
        S02PacketChat chatPacket = (S02PacketChat) event.getPacket();
        String message = chatPacket.getChatComponent().getUnformattedText();
        if (message == null || message.isEmpty()) {
            return;
        }
        
        // Check for auto-start message (strict: only whitespace allowed around exact phrase)
        if (!enabled && message.replaceAll("\\s+", " ").trim().equals("Survive while eliminating")) {
            start();
            System.out.println("[KillCounter] Auto-started TAB Stats from game message!");
            return;
        }
        
        // Check for cages open message and auto-run /ke all if enabled
        if (message.contains("The Blitz Star will be released in 3 minutes!")) {
            if (isAutoKeAll()) {
                System.out.println("[KillCounter] Auto-running /ke all in 0.5 seconds...");
                // Run /ke all command after 0.5 second delay
                new Thread(() -> {
                    try {
                        Thread.sleep(500); // 0.5 second delay
                        Minecraft.getMinecraft().thePlayer.sendChatMessage("/ke all");
                    } catch (InterruptedException e) {
                        System.err.println("[KillCounter] Failed to auto-run /ke all: " + e.getMessage());
                    }
                }).start();
            }
            return;
        }
        
        // Check for the 2-minute message when players receive their kits
        if (message.contains("The Blitz Star will be released in 2 minutes!")) {
            cagesOpenTime = System.currentTimeMillis();
            System.out.println("[KillCounter] Players received their kits! Starting kit detection timer...");
            
            // Schedule kit detection to start 5 seconds after players get their kits
            // Note: This will only detect enemy kits for players who don't already have teammate kits displayed
            new Thread(() -> {
                try {
                    Thread.sleep(5000); // 5 second delay
                    kitDetectionActive = true;
                    System.out.println("[KillCounter] Kit detection is now active! Starting brief scanning window...");
                    
                    // Start brief scanning for kit items (only 1 second window)
                    startBriefKitScanning();
                } catch (InterruptedException e) {
                    System.err.println("[KillCounter] Kit detection timer interrupted: " + e.getMessage());
                }
            }).start();
            return;
        }
        
        // Check for "The game starts in 1 second!" message to detect BLITZ HOUR
        if (message.contains("The game starts in 1 second!")) {
            System.out.println("[KillCounter] Game starting soon - checking for BLITZ HOUR in scoreboard...");
            // Check scoreboard for BLITZ HOUR text
            blitzHourActive = checkScoreboardForBlitzHour();
            if (blitzHourActive) {
                blitzHourDetectionTime = System.currentTimeMillis();
                System.out.println("[KillCounter] BLITZ HOUR detected in scoreboard!");
            } else {
                blitzHourDetectionTime = 0L;
                System.out.println("[KillCounter] No BLITZ HOUR detected in scoreboard - normal mode");
            }
            return;
        }
        
        if (!enabled) {
            return;
        }
        
        // Check for kit selection messages
        if (message.contains(KIT_SELECTION_PATTERN)) {
            // Only process if the message starts with a player name (server message)
            // This prevents false positives when players type messages containing kit selection text
            if (isKitSelectionServerMessage(message)) {
                processKitSelectionMessage(message);
            }
            return;
        }
        
        // Check for kill patterns
        for (String pattern : KILL_PATTERNS) {
            if (message.contains(pattern)) {
                processKillMessage(message, pattern);
                break;
            }
        }
    }
    
    /**
     * Checks if BLITZ HOUR is currently active
     * @return true if BLITZ HOUR was detected in scoreboard and within timeout period
     */
    private static boolean isBlitzHourActive() {
        if (!blitzHourActive || blitzHourDetectionTime == 0L) {
            return false;
        }
        
        // Check if BLITZ HOUR detection has timed out (30 seconds)
        long currentTime = System.currentTimeMillis();
        if (currentTime - blitzHourDetectionTime > BLITZ_HOUR_TIMEOUT) {
            System.out.println("[KillCounter] BLITZ HOUR detection timed out - reverting to normal mode");
            blitzHourActive = false;
            blitzHourDetectionTime = 0L;
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks the scoreboard for BLITZ HOUR text
     * @return true if BLITZ HOUR is found in scoreboard
     */
    private static boolean checkScoreboardForBlitzHour() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null || mc.theWorld.getScoreboard() == null) {
                System.out.println("[KillCounter] No scoreboard available for BLITZ HOUR check");
                return false;
            }
            
            Scoreboard scoreboard = mc.theWorld.getScoreboard();
            ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1); // Sidebar
            
            if (objective == null) {
                System.out.println("[KillCounter] No sidebar objective found for BLITZ HOUR check");
                return false;
            }
            
            Collection<Score> scores = scoreboard.getSortedScores(objective);
            System.out.println("[KillCounter] Checking " + scores.size() + " scoreboard entries for BLITZ HOUR...");
            
            // Check all scoreboard entries for BLITZ HOUR
            for (Score score : scores) {
                // Use the same method as other scoreboard reading to properly handle formatting
                ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
                String scoreText = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
                if (scoreText != null) {
                    // Strip formatting codes before checking for BLITZ HOUR
                    String cleanText = stripFormatting(scoreText);
                    System.out.println("[KillCounter] Scoreboard entry: '" + cleanText + "'");
                    
                    // Check for BLITZ HOUR (case insensitive)
                    if (cleanText.toLowerCase().contains("blitz hour")) {
                        System.out.println("[KillCounter] Found BLITZ HOUR in scoreboard: '" + cleanText + "'");
                        return true;
                    }
                }
            }
            
            // Also check the objective name itself
            String objectiveName = objective.getDisplayName();
            if (objectiveName != null) {
                String cleanObjectiveName = stripFormatting(objectiveName);
                System.out.println("[KillCounter] Objective name: '" + cleanObjectiveName + "'");
                
                if (cleanObjectiveName.toLowerCase().contains("blitz hour")) {
                    System.out.println("[KillCounter] Found BLITZ HOUR in objective name: '" + cleanObjectiveName + "'");
                    return true;
                }
            }
            
            System.out.println("[KillCounter] No BLITZ HOUR found in scoreboard");
            return false;
            
        } catch (Exception e) {
            System.err.println("[KillCounter] Error checking scoreboard for BLITZ HOUR: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Validates that a kit selection message is a server message (starts with player name)
     * and not a player chat message containing kit selection text.
     */
    private boolean isKitSelectionServerMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        
        // Remove formatting codes to get clean text
        String cleanMessage = message.replaceAll("§[0-9a-fk-or]", "").trim();
        
        // Split by the kit selection pattern
        String[] parts = cleanMessage.split(KIT_SELECTION_PATTERN);
        if (parts.length < 2) {
            return false;
        }
        
        // The first part should contain only the player name (and possibly some whitespace)
        String playerNamePart = parts[0].trim();
        
        // Check if there's any text before the player name that would indicate it's a chat message
        // Server messages typically start directly with the player name
        // Chat messages might have prefixes like "<Player>", "[Guild]", etc.
        String[] words = playerNamePart.split("\\s+");
        if (words.length == 0) {
            return false;
        }
        
        // If there's only one word and it looks like a player name, it's likely a server message
        if (words.length == 1) {
            String potentialPlayerName = words[0];
            // Player names typically don't contain special characters like <, >, [, ], etc.
            if (potentialPlayerName.matches("^[a-zA-Z0-9_]+$") && potentialPlayerName.length() >= 3 && potentialPlayerName.length() <= 16) {
                System.out.println("[KillCounter] Detected server kit selection message for player: " + potentialPlayerName);
                return true;
            }
        }
        
        // If there are multiple words or the first word contains special characters,
        // it's likely a chat message, not a server message
        System.out.println("[KillCounter] Skipping kit selection message - appears to be chat: " + cleanMessage);
        return false;
    }
    
    /**
     * Processes a kit selection message to extract player and kit.
     */
    private void processKitSelectionMessage(String message) {
        try {
            // Extract player name and kit from message like "LegitTooMike has picked their Warrior X kit"
            String[] parts = message.split(KIT_SELECTION_PATTERN);
            if (parts.length >= 2) {
                String playerName = extractPlayerName(parts[0]);
                String kitPart = parts[1].trim();
                
                if (playerName != null && kitPart.contains("kit")) {
                    // Extract kit name from "Warrior X kit" -> "Warrior X"
                    String kitName = kitPart.replace("kit", "").trim();
                    if (!kitName.isEmpty()) {
                        // Special handling for Random kit
                        if (kitName.equalsIgnoreCase("Random")) {
                            // Check if we detected BLITZ HOUR from scoreboard
                            if (isBlitzHourActive()) {
                                kitName = "Random X";
                                System.out.println("[KillCounter] BLITZ HOUR detected - using Random X for Random kit");
                            } else {
                                kitName = "Random";
                                System.out.println("[KillCounter] Normal mode - using Random for Random kit");
                            }
                        } else {
                            // If kit name doesn't contain a level (no Roman numerals, stars, or question marks), add default level "I"
                            if (!kitName.matches(".*\\b([IVX]+|✫+|\\?+)\\b.*")) {
                                kitName = kitName + " I";
                                System.out.println("[KillCounter] Added default level I to kit: " + kitName);
                            }
                        }
                        playerKits.put(playerName, kitName);
                        System.out.println("[KillCounter] " + playerName + " selected kit: " + kitName);
                        // Update tab list to show the kit
                        updateAllTabNames();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[KillCounter] Error processing kit selection message: " + e.getMessage());
        }
    }
    
    /**
     * Processes a kill message to extract killer and victim.
     */
    private void processKillMessage(String message, String pattern) {
        try {
            // Extract player names from the message
            String[] parts = message.split(pattern);
            if (parts.length >= 2) {
                String victim = extractPlayerName(parts[0]);
                String killer = extractPlayerName(parts[1]);
                
                if (victim != null && killer != null && !victim.equals(killer)) {
                    // Increment killer's kill count
                    killCounts.put(killer, killCounts.getOrDefault(killer, 0) + 1);
                    
                    // Add victim to killer's victim list
                    playerVictims.computeIfAbsent(killer, k -> new ArrayList<>()).add(victim);
                    
                    // Track death status: mark victim as dead, ensure killer is marked alive
                    deadPlayers.add(victim.toLowerCase(Locale.ROOT));
                    deadPlayers.remove(killer.toLowerCase(Locale.ROOT));
                    
                    // Store player color if we can extract it
                    String killerColor = extractPlayerColor(message, killer);
                    if (killerColor != null) {
                        playerColors.put(killer, killerColor);
                    }
                    
                    System.out.println("[KillCounter] " + killer + " killed " + victim + " (Total: " + 
                                     killCounts.get(killer) + " kills)");
                    // Update tab list display after each kill
                    updateAllTabNames();
                }
            }
        } catch (Exception e) {
            System.err.println("[KillCounter] Error processing kill message: " + e.getMessage());
        }
    }

    /**
     * Updates all player names in the tab list to include kill counts as a suffix.
     * Preserves original team colors/prefixes/suffixes.
     */
    private static void updateAllTabNames() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getNetHandler() == null) {
                return;
            }
            
            // Check if the game has ended - if so, remove all kill counters (rate limited)
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastGameEndCheck > GAME_END_CHECK_INTERVAL) {
                lastGameEndCheck = currentTime;
                if (isGameEnded()) {
                    System.out.println("[KillCounter] Game has ended - removing all kill counters from tab list");
                    gameHasEnded = true; // Set flag to prevent re-adding kill counters
                    resetAllTabNames();
                    return;
                }
            }
            
            // If game has ended, don't add kill counters to tab list but keep other functionality
            if (gameHasEnded) {
                System.out.println("[KillCounter] Game has ended - not displaying kill counters in tab list");
                // Still update tab names but without kill counters
                updateTabNamesWithoutKillCounters();
                return;
            }
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                String baseName = info.getGameProfile().getName();
                if (baseName == null || baseName.isEmpty()) {
                    continue;
                }

                int kills = getKillCount(baseName);
                if (kills >= 1) {
                    // Always rebuild from team formatting to avoid duplicating previous suffixes
                    String serverName = ScorePlayerTeam.formatPlayerName(info.getPlayerTeam(), baseName);
                    boolean isDead = deadPlayers.contains(baseName.toLowerCase(Locale.ROOT));
                    // Optionally color the base player name using kill effect color from /ke all (only if alive)
                    if (!isDead && showColors) {
                        String effect = me.ballmc.gomod.command.KillEffectCommand.getPlayerEffect(baseName);
                        if (effect != null && !effect.isEmpty()) {
                            String effectColor = mapKillEffectToColor(effect);
                            if (effectColor != null) {
                                String coloredBase = effectColor + baseName + EnumChatFormatting.RESET;
                                serverName = serverName.replace(baseName, coloredBase);
                            }
                        }
                    }
                    int maxKillsAlive = getMaxKillsAlive();
                    boolean isTop = !isDead && (kills == maxKillsAlive && maxKillsAlive > 0);
                    // Dead players: blue (BLUE), no bold. Alive: dark red, bold if top (ties included).
                    String style = isDead
                        ? EnumChatFormatting.BLUE.toString()
                        : (EnumChatFormatting.DARK_RED + (isTop ? EnumChatFormatting.BOLD.toString() : ""));
                    String stats = me.ballmc.gomod.command.KillEffectCommand.getStatsSuffixFor(baseName);
                    String formattedStats = formatStatsForMode(stats);
                    String guildTag = getGuildTag(baseName);
                    String playerKit = getPlayerKit(baseName);
                    String enemyPlayerKit = getEnemyPlayerKit(baseName);
                    
                    // Only fetch guild tags when /ke all is run or cages open (not automatically)
                    // Guild tags will be fetched when the user runs /ke all or when cages open
                    
                    System.out.println("[KillCounter] Building tab name for " + baseName + ":");
                    System.out.println("  - Stats: " + formattedStats);
                    System.out.println("  - Guild tag: " + guildTag);
                    System.out.println("  - Teammate Kit: " + playerKit);
                    System.out.println("  - Enemy Kit: " + enemyPlayerKit);
                    System.out.println("  - Enemy Kit Empty: " + enemyPlayerKit.isEmpty());
                    
                    // Build the display name with kill counter, stats, kit, then guild tag
                    StringBuilder displayName = new StringBuilder();
                    displayName.append(serverName);
                    displayName.append(" ").append(style).append("[").append(kills).append("]");
                    
                    if (!formattedStats.isEmpty()) {
                        displayName.append(" ").append(EnumChatFormatting.RESET).append(formattedStats);
                    }
                    
                    if (!playerKit.isEmpty()) {
                        displayName.append(" ").append(EnumChatFormatting.RESET).append(EnumChatFormatting.YELLOW).append("(").append(playerKit).append(")");
                    }
                    
                    if (!enemyPlayerKit.isEmpty()) {
                        System.out.println("[KillCounter] Adding enemy kit to display: " + enemyPlayerKit);
                        displayName.append(" ").append(EnumChatFormatting.RESET).append(EnumChatFormatting.YELLOW).append("(").append(enemyPlayerKit).append(")");
                    } else {
                        System.out.println("[KillCounter] Enemy kit is empty, not adding to display");
                    }
                    
                    if (!guildTag.isEmpty()) {
                        // Apply the same color to brackets as the guild tag
                        // Apply the same color to brackets as the guild tag
                        String guildTagColor = playerGuildTagColors.get(baseName);
                        if (guildTagColor == null) {
                            guildTagColor = "";
                        }
                        System.out.println("[KillCounter] Using stored guild tag color: " + guildTagColor + " for guild tag: " + guildTag);
                        displayName.append(" ").append(EnumChatFormatting.RESET).append(guildTagColor).append("[").append(guildTag).append("]");
                    }
                    
                    System.out.println("[KillCounter] Final display name: " + displayName.toString());
                    
                    info.setDisplayName(new ChatComponentText(displayName.toString()));
                } else {
                    // No kills: use server default formatting
                    String stats = me.ballmc.gomod.command.KillEffectCommand.getStatsSuffixFor(baseName);
                    String formattedStats = formatStatsForMode(stats);
                    String guildTag = getGuildTag(baseName);
                    String playerKit = getPlayerKit(baseName);
                    String enemyPlayerKit = getEnemyPlayerKit(baseName);
                    
                    // Only fetch guild tags when /ke all is run or cages open (not automatically)
                    // Guild tags will be fetched when the user runs /ke all or when cages open
                    
                    if (!formattedStats.isEmpty() || !guildTag.isEmpty() || !playerKit.isEmpty() || !enemyPlayerKit.isEmpty()) {
                        String serverName = ScorePlayerTeam.formatPlayerName(info.getPlayerTeam(), baseName);
                        boolean isDead = deadPlayers.contains(baseName.toLowerCase(Locale.ROOT));
                        // Optionally color the base player name using kill effect color from /ke all (only if alive)
                        if (!isDead && showColors) {
                            String effect = me.ballmc.gomod.command.KillEffectCommand.getPlayerEffect(baseName);
                            if (effect != null && !effect.isEmpty()) {
                                String effectColor = mapKillEffectToColor(effect);
                                if (effectColor != null) {
                                    String coloredBase = effectColor + baseName + EnumChatFormatting.RESET;
                                    serverName = serverName.replace(baseName, coloredBase);
                                }
                            }
                        }
                        
                        // Build display name with stats, kit, then guild tag
                        StringBuilder displayName = new StringBuilder();
                        displayName.append(serverName);
                        
                        if (!formattedStats.isEmpty()) {
                            displayName.append(" ").append(EnumChatFormatting.RESET).append(formattedStats);
                        }
                        
                        if (!playerKit.isEmpty()) {
                            displayName.append(" ").append(EnumChatFormatting.RESET).append(EnumChatFormatting.YELLOW).append("(").append(playerKit).append(")");
                        }
                        
                        if (!enemyPlayerKit.isEmpty()) {
                            displayName.append(" ").append(EnumChatFormatting.RESET).append(EnumChatFormatting.YELLOW).append("(").append(enemyPlayerKit).append(")");
                        }
                        
                    if (!guildTag.isEmpty()) {
                        // Apply the same color to brackets as the guild tag
                        // Apply the same color to brackets as the guild tag
                        String guildTagColor = playerGuildTagColors.get(baseName);
                        if (guildTagColor == null) {
                            guildTagColor = "";
                        }
                        System.out.println("[KillCounter] Using stored guild tag color: " + guildTagColor + " for guild tag: " + guildTag);
                        displayName.append(" ").append(EnumChatFormatting.RESET).append(guildTagColor).append("[").append(guildTag).append("]");
                    }
                        
                        info.setDisplayName(new ChatComponentText(displayName.toString()));
                    } else {
                        info.setDisplayName(null);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    // Map kill effect name to color, matching KillEffectCommand
    private static String mapKillEffectToColor(String effect) {
        if (effect == null) return null;
        String e = effect.toUpperCase(Locale.ROOT);
        switch (e) {
            case "REGENERATION": return EnumChatFormatting.LIGHT_PURPLE.toString();
            case "RESISTANCE": return EnumChatFormatting.GOLD.toString();
            case "GRAVEDIGGER": return EnumChatFormatting.GREEN.toString();
            case "RANDOM": return EnumChatFormatting.DARK_BLUE.toString();
            case "LEVEL_UP":
            case "LEVELUP": return EnumChatFormatting.YELLOW.toString();
            case "RAPID_FIRE":
            case "FLAME": return EnumChatFormatting.DARK_RED.toString();
            case "SPEED": return EnumChatFormatting.AQUA.toString();
            default: return null;
        }
    }

    /**
     * Updates tab names without kill counters (for when game has ended)
     */
    private static void updateTabNamesWithoutKillCounters() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getNetHandler() == null) {
                return;
            }
            
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                String baseName = info.getGameProfile().getName();
                if (baseName == null || baseName.isEmpty()) {
                    continue;
                }

                // Update tab names with stats, guild tags but NO kill counters and NO kits when game has ended
                String stats = me.ballmc.gomod.command.KillEffectCommand.getStatsSuffixFor(baseName);
                String formattedStats = formatStatsForMode(stats);
                String guildTag = getGuildTag(baseName);
                // Don't show kit information when game has ended
                String playerKit = "";
                String enemyPlayerKit = "";
                
                if (!formattedStats.isEmpty() || !guildTag.isEmpty()) {
                    String serverName = ScorePlayerTeam.formatPlayerName(info.getPlayerTeam(), baseName);
                    boolean isDead = deadPlayers.contains(baseName.toLowerCase(Locale.ROOT));
                    
                    // Apply kill effect colors if enabled
                    if (!isDead && showColors) {
                        String effect = me.ballmc.gomod.command.KillEffectCommand.getPlayerEffect(baseName);
                        if (effect != null && !effect.isEmpty()) {
                            String effectColor = mapKillEffectToColor(effect);
                            if (effectColor != null) {
                                String coloredBase = effectColor + baseName + EnumChatFormatting.RESET;
                                serverName = serverName.replace(baseName, coloredBase);
                            }
                        }
                    }
                    
                    // Build display name with stats, guild tag but NO kill counters and NO kits
                    StringBuilder displayName = new StringBuilder();
                    displayName.append(serverName);
                    
                    if (!formattedStats.isEmpty()) {
                        displayName.append(" ").append(EnumChatFormatting.RESET).append(formattedStats);
                    }
                    
                    // Kit information hidden when game has ended
                    
                    if (!guildTag.isEmpty()) {
                        String guildTagColor = playerGuildTagColors.get(baseName);
                        if (guildTagColor == null) {
                            guildTagColor = "";
                        }
                        displayName.append(" ").append(EnumChatFormatting.RESET).append(guildTagColor).append("[").append(guildTag).append("]");
                    }
                    
                    info.setDisplayName(new ChatComponentText(displayName.toString()));
                } else {
                    // No additional info to show, use server default
                    info.setDisplayName(null);
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Resets all player names in the tab list back to server defaults.
     */
    private static void resetAllTabNames() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getNetHandler() == null) {
                return;
            }
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                // Null clears the override so Minecraft uses the default formatting
                info.setDisplayName(null);
            }
        } catch (Exception ignored) {
        }
    }
    
    /**
     * Extracts player name from a message part.
     */
    private String extractPlayerName(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        // Remove color codes and extract the last word (usually the player name)
        String cleanText = text.replaceAll("§[0-9a-fk-or]", "").trim();
        String[] words = cleanText.split("\\s+");
        
        if (words.length > 0) {
            String name = words[words.length - 1];
            // Remove any trailing punctuation
            name = name.replaceAll("[.,!?]", "");
            
            // Filter out kill counters and other non-player text
            if (isKillCounterOrNonPlayer(name)) {
                return null;
            }
            
            return name.trim();
        }
        
        return null;
    }
    
    /**
     * Checks if a string is a kill counter or other non-player text.
     */
    private boolean isKillCounterOrNonPlayer(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        
        // Check for kill counter patterns
        if (text.matches("\\(Kill #\\d+[,\\d]*\\)")) {
            return true;
        }
        
        // Check for other common non-player patterns
        if (text.matches("\\(\\d+\\)") || // (123)
            text.matches("\\[\\d+\\]") || // [123]
            text.matches("#\\d+") || // #123
            text.matches("\\d+") || // Pure numbers
            text.matches(".*\\(.*\\).*") || // Contains parentheses
            text.matches(".*\\[.*\\].*") || // Contains brackets
            text.matches(".*#.*")) { // Contains hash
            return true;
        }
        
        return false;
    }
    
    /**
     * Extracts player color from the message.
     */
    private String extractPlayerColor(String message, String playerName) {
        try {
            // Look for color codes before the player name
            int nameIndex = message.indexOf(playerName);
            if (nameIndex > 0) {
                String beforeName = message.substring(0, nameIndex);
                // Find the last color code
                int lastColorIndex = beforeName.lastIndexOf("§");
                if (lastColorIndex >= 0 && lastColorIndex + 1 < beforeName.length()) {
                    String colorCode = beforeName.substring(lastColorIndex, lastColorIndex + 2);
                    return "§" + colorCode.charAt(1);
                }
            }
        } catch (Exception e) {
            // Ignore color extraction errors
        }
        return null;
    }

    // Exposed for other features (e.g., KillEffectCommand) to force a Tab refresh without duplications
    public static void forceTabRefresh() {
        updateAllTabNames();
    }
    
    /**
     * Formats stats suffix based on current WL mode setting
     */
    private static String formatStatsForMode(String originalStats) {
        if (originalStats == null || originalStats.isEmpty()) {
            return "";
        }
        
        if (!statsDisplay) {
            return ""; // Don't show stats if stats display is disabled
        }
        
        // Extract wl and wins from original format like [0.50wl/1000w]
        // Pattern: [0.50wl/1000w] or [1000w]
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[([\\d.]+)wl/(\\d+)w\\]|\\[(\\d+)w\\]");
        java.util.regex.Matcher matcher = pattern.matcher(originalStats);
        
        if (matcher.find()) {
            String wl = matcher.group(1); // First group: wl ratio
            String wins = matcher.group(2); // Second group: total wins (from wl/w format)
            String winsOnly = matcher.group(3); // Third group: wins only (from w format)
            
            // Extract color codes from original
            String colorPrefix = originalStats.substring(0, originalStats.indexOf('['));
            
            switch (wlMode) {
                case 0: // WL Only
                    if (wl != null) {
                        return colorPrefix + "[" + wl + "wl]";
                    }
                    break;
                case 1: // WL + W
                    if (wl != null && wins != null) {
                        return colorPrefix + "[" + wl + "wl/" + wins + "w]";
                    }
                    break;
                case 2: // W Only
                    String totalWins = wins != null ? wins : winsOnly;
                    if (totalWins != null) {
                        return colorPrefix + "[" + totalWins + "w]";
                    }
                    break;
                case 3: // Username Only
                    return ""; // Don't show any stats
            }
        }
        
        // Fallback: return original if we can't parse it
        return originalStats;
    }
    
    /**
     * Gets guild tag for a player with proper color formatting
     */
    private static String getGuildTag(String playerName) {
        if (!isShowGuildTag()) {
            System.out.println("[KillCounter] Guild tag display is disabled for " + playerName);
            return "";
        }
        
        // Check cache first
        if (playerGuildTags.containsKey(playerName)) {
            String cachedTag = playerGuildTags.get(playerName);
            System.out.println("[KillCounter] Using cached guild tag for " + playerName + ": " + cachedTag);
            return cachedTag;
        }
        
        System.out.println("[KillCounter] No cached guild tag for " + playerName);
        // Return empty if not cached - guild tags are fetched when /ke all is run
        return "";
    }
    
    /**
     * Gets kit for a player if teammate kit display is enabled
     */
    private static String getPlayerKit(String playerName) {
        if (!isShowTeammateKits()) {
            return "";
        }
        
        // Check if player has a kit
        if (playerKits.containsKey(playerName)) {
            String kit = playerKits.get(playerName);
            System.out.println("[KillCounter] Using cached kit for " + playerName + ": " + kit);
            return kit;
        }
        
        return "";
    }
    
    /**
     * Gets enemy kit for a player if enemy kit display is enabled
     */
    private static String getEnemyPlayerKit(String playerName) {
        if (!isShowEnemyKits()) {
            return "";
        }
        
        // Check if player has a detected enemy kit
        if (enemyPlayerKits.containsKey(playerName)) {
            String kit = enemyPlayerKits.get(playerName);
            System.out.println("[KillCounter] Using cached enemy kit for " + playerName + ": " + kit);
            return kit;
        }
        
        return "";
    }
    
    /**
     * Detects kit from item name using regex patterns
     */
    private static String detectKitFromItemName(String itemName) {
        if (itemName == null || itemName.isEmpty()) {
            System.out.println("[KillCounter] No kit detected from item: null/empty");
            return null;
        }
        
        System.out.println("[KillCounter] Testing item: '" + itemName + "'");
        java.util.regex.Matcher matcher = KIT_ITEM_PATTERN.matcher(itemName);
        if (matcher.find()) {
            String kitName = matcher.group(1).trim();
            String level = matcher.group(2).trim();
            System.out.println("[KillCounter] Regex matched - Kit: '" + kitName + "', Level: '" + level + "'");
            
            // Check if the kit name is in our known kit list
            for (String knownKit : KIT_NAMES) {
                if (kitName.equalsIgnoreCase(knownKit)) {
                    System.out.println("[KillCounter] Found matching kit: '" + knownKit + "'");
                    // Convert ?? characters to proper prestige symbols
                    String formattedLevel = formatKitLevel(level);
                    System.out.println("[KillCounter] Formatted level: '" + formattedLevel + "'");
                    String result = kitName + " " + formattedLevel;
                    System.out.println("[KillCounter] Final result: '" + result + "'");
                    return result;
                }
            }
            System.out.println("[KillCounter] Kit name '" + kitName + "' not found in known kits list");
        } else {
            System.out.println("[KillCounter] Regex did not match item: '" + itemName + "'");
        }
        
        return null;
    }
    
    /**
     * Formats kit level to standard format, handling ?? characters and ✫ symbols
     */
    private static String formatKitLevel(String level) {
        if (level == null || level.isEmpty()) {
            return null;
        }
        
        // Handle ?? characters (which represent ✫ in logs due to Unicode issues)
        if (level.equals("?")) {
            return "P1"; // Prestige I
        } else if (level.equals("??")) {
            return "P2"; // Prestige II
        }
        
        // Handle actual ✫ symbols (direct Unicode characters)
        if (level.equals("✫")) {
            return "P1"; // Prestige I
        } else if (level.equals("✫✫")) {
            return "P2"; // Prestige II
        }
        
        // Handle prestige levels with ✫ symbols (if they come through correctly)
        if (level.contains("✫")) {
            int starCount = level.length() - level.replace("✫", "").length();
            if (starCount == 1) {
                return "P1"; // Prestige I
            } else if (starCount == 2) {
                return "P2"; // Prestige II
            }
        }
        
        // Handle Roman numerals (I-X)
        if (level.matches("^[IVX]+$")) {
            return level; // Already in correct format
        }
        
        // Handle numeric levels (1-10)
        try {
            int numericLevel = Integer.parseInt(level);
            if (numericLevel >= 1 && numericLevel <= 10) {
                return romanNumeral(numericLevel);
            }
        } catch (NumberFormatException e) {
            // Not a number, continue checking
        }
        
        // If we can't parse it, return null (invalid level)
        return null;
    }
    
    /**
     * Converts numeric level to Roman numeral
     */
    private static String romanNumeral(int number) {
        if (number >= 1 && number <= 10) {
            String[] romanNumerals = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
            return romanNumerals[number - 1];
        }
        return String.valueOf(number);
    }
    
    /**
     * Processes player gear to detect kit items
     */
    public static void processPlayerGear(String playerName, String[] gearItems) {
        if (!isShowEnemyKits() || !kitDetectionActive) {
            return;
        }
        
        // Skip if we already detected a kit for this player
        if (enemyPlayerKits.containsKey(playerName)) {
            return;
        }
        
        // Skip if this player already has a teammate kit displayed
        // This prevents duplicate kit display when 2-minute message appears
        if (playerKits.containsKey(playerName)) {
            System.out.println("[KillCounter] Skipping enemy kit detection for " + playerName + " - already has teammate kit: " + playerKits.get(playerName));
            return;
        }
        
        // Check each gear item for kit patterns
        for (String item : gearItems) {
            if (item != null && !item.isEmpty()) {
                String detectedKit = detectKitFromItemName(item);
                if (detectedKit != null) {
                    enemyPlayerKits.put(playerName, detectedKit);
                    System.out.println("[KillCounter] Detected enemy kit for " + playerName + ": " + detectedKit + " from item: " + item);
                    // Update tab list to show the detected kit
                    updateAllTabNames();
                    break; // Stop after first kit detection
                }
            }
        }
    }
    
    /**
     * Starts brief scanning for kit items (only 1 second window)
     */
    private static void startBriefKitScanning() {
        new Thread(() -> {
            try {
                System.out.println("[KillCounter] Starting 1-second kit detection window...");
                
                // Scan immediately when activated
                scanPlayersForKits();
                
                // Wait 1 second
                Thread.sleep(1000);
                
                // Disable kit detection to prevent false positives from looted items
                kitDetectionActive = false;
                System.out.println("[KillCounter] Kit detection window closed! No more scanning to avoid false positives from looted items.");
                
            } catch (InterruptedException e) {
                System.err.println("[KillCounter] Brief kit scanning interrupted: " + e.getMessage());
                kitDetectionActive = false;
            }
        }).start();
    }
    
    /**
     * Scans all nearby players for kit items when detection is active
     */
    private static void scanPlayersForKits() {
        System.out.println("[KillCounter] scanPlayersForKits called - showEnemyKits: " + isShowEnemyKits() + ", kitDetectionActive: " + kitDetectionActive);
        
        if (!isShowEnemyKits() || !kitDetectionActive) {
            System.out.println("[KillCounter] Skipping scan - showEnemyKits: " + isShowEnemyKits() + ", kitDetectionActive: " + kitDetectionActive);
            return;
        }
        
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null || mc.thePlayer == null) {
                return;
            }
            
            // Scan players within maximum visibility range (200 blocks)
            for (net.minecraft.entity.player.EntityPlayer player : mc.theWorld.playerEntities) {
                if (player == mc.thePlayer) {
                    continue; // Skip the current player
                }
                
                // Calculate distance to player
                double distanceSq = mc.thePlayer.getDistanceSqToEntity(player);
                double distance = Math.sqrt(distanceSq);
                
                // Only scan players within 200 blocks (maximum reasonable visibility for equipment)
                if (distance > 200.0) {
                    continue; // Skip players too far away
                }
                
                String playerName = player.getName();
                System.out.println("[KillCounter] Scanning player: " + playerName + " (" + Math.round(distance) + " blocks away)");
                
                // Skip if we already detected a kit for this player
                if (enemyPlayerKits.containsKey(playerName)) {
                    continue;
                }
                
                // Skip if this player already has a teammate kit displayed
                // This prevents duplicate kit display when scanning for enemy kits
                if (playerKits.containsKey(playerName)) {
                    System.out.println("[KillCounter] Skipping enemy kit detection for " + playerName + " - already has teammate kit: " + playerKits.get(playerName));
                    continue;
                }
                
                System.out.println("[KillCounter] Scanning player: " + playerName);
                
                // Check held item
                if (player.getCurrentEquippedItem() != null) {
                    String itemName = player.getCurrentEquippedItem().getDisplayName();
                    System.out.println("[KillCounter] Checking held item for " + playerName + ": " + itemName);
                    String detectedKit = detectKitFromItemName(itemName);
                    if (detectedKit != null) {
                        enemyPlayerKits.put(playerName, detectedKit);
                        System.out.println("[KillCounter] Detected enemy kit for " + playerName + ": " + detectedKit + " from held item: " + itemName);
                        updateAllTabNames();
                        continue; // Move to next player
                    }
                } else {
                    System.out.println("[KillCounter] No held item for " + playerName);
                }
                
                // Check armor slots
                System.out.println("[KillCounter] Starting armor check for " + playerName);
                String[] armorNames = {"Boots", "Leggings", "Chestplate", "Helmet"};
                boolean kitFound = false;
                for (int i = 0; i < 4; i++) {
                    System.out.println("[KillCounter] Checking armor slot " + i + " (" + armorNames[i] + ") for " + playerName);
                    try {
                        if (player.inventory.armorInventory != null && player.inventory.armorInventory[i] != null) {
                            String rawItemName = player.inventory.armorInventory[i].getDisplayName();
                            String itemName = stripFormatting(rawItemName);
                            System.out.println("[KillCounter] Found " + armorNames[i] + " for " + playerName + ": " + itemName + " (raw: " + rawItemName + ")");
                            String detectedKit = detectKitFromItemName(itemName);
                            if (detectedKit != null && !kitFound) {
                                enemyPlayerKits.put(playerName, detectedKit);
                                System.out.println("[KillCounter] Detected enemy kit for " + playerName + ": " + detectedKit + " from " + armorNames[i] + ": " + itemName);
                                updateAllTabNames();
                                kitFound = true;
                            } else {
                                System.out.println("[KillCounter] No kit detected from " + armorNames[i] + ": " + itemName);
                            }
                        } else {
                            System.out.println("[KillCounter] No " + armorNames[i] + " for " + playerName + " (slot " + i + " is null)");
                        }
                    } catch (Exception e) {
                        System.out.println("[KillCounter] Error checking armor slot " + i + " for " + playerName + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[KillCounter] Error scanning players for kits: " + e.getMessage());
        }
    }
    
    /**
     * Strips Minecraft formatting codes (§ and &) from text
     * This prevents issues with formatting codes in item names
     */
    private static String stripFormatting(String text) {
        if (text == null) return "";
        
        // Remove both § and & formatting codes
        // The pattern is § or & followed by one of the Minecraft format codes (0-9, a-f, k-o, r)
        return text.replaceAll("(?i)[§&][0-9a-fk-or]", "");
    }
    
    /**
     * Test method to manually check if game has ended
     */
    public static void testGameEndDetection() {
        System.out.println("[KillCounter] Testing game end detection...");
        boolean gameEnded = isGameEnded();
        System.out.println("[KillCounter] Game end detection result: " + gameEnded);
        
        if (gameEnded) {
            System.out.println("[KillCounter] Game appears to have ended - removing kill counters from tab list");
            resetAllTabNames();
        } else {
            System.out.println("[KillCounter] Game appears to still be active - keeping kill counters");
        }
    }

    /**
     * Test method to manually test kit detection functionality
     */
    public static void testKitDetection() {
        System.out.println("[KillCounter] Testing kit detection functionality...");
        
        // Test kit detection with sample items
        String[] testItems = {
            "Guardian's Stone Axe (VI)",
            "Meatmaster's Iron Sword (?)", // Prestige I (shows as ? in logs) -> P1
            "Archer's Bow (??)", // Prestige II (shows as ?? in logs) -> P2
            "Donkeytamer's Leather Helmet (X)",
            "Archer's Bow (✫)", // Prestige I -> P1
            "Knight's Iron Sword (✫✫)", // Prestige II -> P2
            "Knight's Iron Sword (VII)",
            "Regular Iron Sword", // Should not match
            "Baker's Bread (III)"
        };
        
        for (String item : testItems) {
            String detectedKit = detectKitFromItemName(item);
            if (detectedKit != null) {
                System.out.println("[KillCounter] Test: '" + item + "' -> Detected kit: " + detectedKit);
            } else {
                System.out.println("[KillCounter] Test: '" + item + "' -> No kit detected");
            }
        }
        
        // Test with sample player gear
        System.out.println("[KillCounter] Testing player gear processing...");
        String testPlayer = "TestPlayer";
        processPlayerGear(testPlayer, new String[]{"Guardian's Stone Axe (VI)", "Regular Leather Boots"});
        
        if (enemyPlayerKits.containsKey(testPlayer)) {
            System.out.println("[KillCounter] Test: Successfully detected kit for " + testPlayer + ": " + enemyPlayerKits.get(testPlayer));
        } else {
            System.out.println("[KillCounter] Test: Failed to detect kit for " + testPlayer);
        }
        
        // Clean up test data
        enemyPlayerKits.remove(testPlayer);
        
        // Test manual scanning of real players
        System.out.println("[KillCounter] Testing manual scan of real players...");
        scanPlayersForKits();
        System.out.println("[KillCounter] Manual scan completed.");
        
        // Test the regex pattern with more examples
        System.out.println("[KillCounter] Testing regex pattern with additional examples...");
        String[] additionalTests = {
            "Guardian's Leather Helmet (VI)",
            "Meatmaster's Iron Sword (?)", // Prestige I (shows as ? in logs) -> P1
            "Archer's Bow (??)", // Prestige II (shows as ?? in logs) -> P2
            "Donkeytamer's Diamond Sword (X)",
            "Archer's Bow (✫)", // Prestige I -> P1
            "Phoenix's Gold Helmet (✫✫)", // Prestige II -> P2
            "Knight's Iron Chestplate (VII)",
            "Knight's Leather Boots (I)",
            "Knight's Iron Helmet (I)",
            "Baker's Bread (III)",
            "Regular Iron Sword",
            "Diamond Helmet",
            "Guardian's Stone Axe (V)"
        };
        
        for (String testItem : additionalTests) {
            String detectedKit = detectKitFromItemName(testItem);
            System.out.println("[KillCounter] Test: '" + testItem + "' -> " + (detectedKit != null ? "Detected: " + detectedKit : "No kit detected"));
        }
        
        // Test the actual items from the logs
        System.out.println("[KillCounter] Testing actual items from logs...");
        String[] realItems = {
            "Armorer's Leather Boots (?)",
            "Troll's Leather Boots (??)",
            "Wolftamer's Diamond Boots (?)"
        };
        
        for (String realItem : realItems) {
            String detectedKit = detectKitFromItemName(realItem);
            System.out.println("[KillCounter] Real item test: '" + realItem + "' -> " + (detectedKit != null ? "Detected: " + detectedKit : "No kit detected"));
        }
    }
    
    /**
     * Manually triggers a scan for kit detection (for testing or manual use)
     */
    public static void manualKitScan() {
        System.out.println("[KillCounter] Manual kit scan triggered...");
        System.out.println("[KillCounter] Current settings - showEnemyKits: " + isShowEnemyKits() + ", kitDetectionActive: " + kitDetectionActive);
        
        // Force enable kit detection for manual scanning
        boolean wasActive = kitDetectionActive;
        kitDetectionActive = true;
        
        scanPlayersForKits();
        
        // Restore original state
        kitDetectionActive = wasActive;
        
        System.out.println("[KillCounter] Manual kit scan completed.");
    }
    
    /**
     * Fetches and caches guild tag for a player from Hypixel API
     */
    public static void fetchGuildTag(String playerName) {
        if (!isShowGuildTag()) {
            System.out.println("[KillCounter] Guild tag display is disabled, skipping fetch for " + playerName);
            return;
        }
        
        // Don't fetch if already cached
        if (playerGuildTags.containsKey(playerName)) {
            System.out.println("[KillCounter] Guild tag already cached for " + playerName + ": " + playerGuildTags.get(playerName));
            return;
        }
        
        System.out.println("[KillCounter] Fetching guild tag for " + playerName);
        
        new Thread(() -> {
            try {
                // Rate limiting - wait if we made a call recently
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastApiCall < API_CALL_DELAY) {
                    long waitTime = API_CALL_DELAY - (currentTime - lastApiCall);
                    System.out.println("[KillCounter] Rate limiting: waiting " + waitTime + "ms before API call");
                    Thread.sleep(waitTime);
                }
                lastApiCall = System.currentTimeMillis();
                
                String apiKey = me.ballmc.gomod.features.ApiKeyManager.getApiKey("hypixel");
                if (apiKey == null || apiKey.isEmpty()) {
                    System.out.println("[KillCounter] No Hypixel API key found for guild tag fetch");
                    return;
                }
                
                // Get player UUID first
                String uuid = getPlayerUUID(playerName);
                if (uuid == null || uuid.isEmpty()) {
                    System.out.println("[KillCounter] Could not get UUID for " + playerName);
                    return;
                }
                
                System.out.println("[KillCounter] Got UUID for " + playerName + ": " + uuid);
                
                // Fetch guild data
                String guildUrl = "https://api.hypixel.net/guild?key=" + apiKey + "&player=" + uuid;
                String response = makeHttpRequest(guildUrl);
                
                if (response != null && response.contains("\"success\":true")) {
                    // Parse guild tag and color from JSON response
                    String guildTag = extractGuildTag(response);
                    String tagColor = extractGuildTagColor(response);
                    
                    System.out.println("[KillCounter] Parsed guild tag: " + guildTag + ", color: " + tagColor);
                    
                    if (guildTag != null && !guildTag.isEmpty()) {
                        // Fix Unicode characters for Minecraft compatibility
                        String fixedGuildTag = fixGuildTagUnicode(guildTag);
                        String coloredTag = getGuildTagColor(tagColor) + fixedGuildTag;
                        playerGuildTags.put(playerName, coloredTag);
                        playerGuildTagColors.put(playerName, getGuildTagColor(tagColor));
                        System.out.println("[KillCounter] Cached guild tag for " + playerName + ": " + coloredTag + " (original: " + guildTag + ")");
                        
                        // Force tab refresh to show the guild tag
                        updateAllTabNames();
                    } else {
                        System.out.println("[KillCounter] No guild tag found for " + playerName);
                    }
                } else {
                    System.out.println("[KillCounter] Guild API request failed for " + playerName);
                }
            } catch (Exception e) {
                System.err.println("[KillCounter] Error fetching guild tag for " + playerName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
     * Gets player UUID from Mojang API
     */
    private static String getPlayerUUID(String playerName) {
        try {
            String url = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
            String response = makeHttpRequest(url);
            
            if (response != null && response.contains("\"id\"")) {
                System.out.println("[KillCounter] Mojang API response: " + response);
                
                // Extract UUID from JSON response - handle both "id":" and "id" : " formats
                int start = response.indexOf("\"id\":\"");
                if (start == -1) {
                    // Try with spaces: "id" : "
                    start = response.indexOf("\"id\" : \"");
                    if (start == -1) {
                        System.out.println("[KillCounter] No 'id' field found in Mojang response");
                        return null;
                    }
                    start += 8; // Skip past "id" : "
                } else {
                    start += 6; // Skip past "id":"
                }
                
                int end = response.indexOf("\"", start);
                if (end > start) {
                    String uuid = response.substring(start, end);
                    System.out.println("[KillCounter] Extracted UUID: " + uuid);
                    return uuid;
                }
                
                System.out.println("[KillCounter] Could not parse UUID from Mojang response");
                return null;
            }
        } catch (Exception e) {
            System.err.println("[KillCounter] Error getting UUID for " + playerName + ": " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Makes HTTP request and returns response
     */
    private static String makeHttpRequest(String url) {
        try {
            System.out.println("[KillCounter] Making HTTP request to: " + url);
            java.net.URL urlObj = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "gomod123/1.0");
            
            int responseCode = conn.getResponseCode();
            System.out.println("[KillCounter] HTTP response code: " + responseCode);
            
            if (responseCode == 200) {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                String responseStr = response.toString();
                System.out.println("[KillCounter] HTTP response: " + responseStr.substring(0, Math.min(200, responseStr.length())) + "...");
                return responseStr;
            } else {
                // Read error stream
                java.io.BufferedReader errorReader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getErrorStream()));
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorResponse.append(line);
                }
                errorReader.close();
                System.out.println("[KillCounter] HTTP error response: " + errorResponse.toString());
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("[KillCounter] HTTP request failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Extracts guild tag from API response
     */
    private static String extractGuildTag(String response) {
        try {
            System.out.println("[KillCounter] Parsing guild tag from response...");
            System.out.println("[KillCounter] Full response: " + response);
            
            // Look for guild tag at the guild level (not in ranks section)
            // The guild tag is in guild.tag, which is the main guild tag like "G<PARK"
            int guildStart = response.indexOf("\"guild\":{");
            if (guildStart == -1) {
                System.out.println("[KillCounter] No 'guild' object found in response");
                return null;
            }
            
            // Find the ranks section and look for the tag AFTER it
            int ranksStart = response.indexOf("\"ranks\"", guildStart);
            int searchStart = guildStart;
            
            if (ranksStart != -1) {
                // Find the end of the ranks section
                int ranksEnd = response.indexOf("]", ranksStart);
                if (ranksEnd != -1) {
                    // Look for the tag field after the ranks section
                    searchStart = ranksEnd;
                    System.out.println("[KillCounter] Found ranks section, searching for tag after position: " + searchStart);
                }
            }
            
            // Look for the main guild tag after the ranks section
            int tagStart = response.indexOf("\"tag\":\"", searchStart);
            if (tagStart == -1) {
                // Try with spaces
                tagStart = response.indexOf("\"tag\" : \"", searchStart);
                if (tagStart == -1) {
                    System.out.println("[KillCounter] No 'tag' field found after ranks section");
                    return null;
                }
                tagStart += 8; // Skip past "tag" : "
            } else {
                tagStart += 7; // Skip past "tag":"
            }
            
            int tagEnd = response.indexOf("\"", tagStart);
            if (tagEnd > tagStart) {
                String tag = response.substring(tagStart, tagEnd);
                System.out.println("[KillCounter] Extracted guild tag: " + tag);
                return tag;
            }
            
            System.out.println("[KillCounter] Could not parse guild tag");
            return null;
        } catch (Exception e) {
            System.err.println("[KillCounter] Error extracting guild tag: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extracts guild tag color from API response
     */
    private static String extractGuildTagColor(String response) {
        try {
            System.out.println("[KillCounter] Parsing guild tag color from response...");
            
            // Look for tagColor in the guild object
            int guildStart = response.indexOf("\"guild\":{");
            if (guildStart == -1) {
                System.out.println("[KillCounter] No 'guild' object found for color parsing");
                return "WHITE";
            }
            
            // Find the tagColor field within the guild object
            int colorStart = response.indexOf("\"tagColor\":\"", guildStart);
            if (colorStart == -1) {
                // Try with spaces
                colorStart = response.indexOf("\"tagColor\" : \"", guildStart);
                if (colorStart == -1) {
                    System.out.println("[KillCounter] No 'tagColor' field found in guild object");
                    return "WHITE";
                }
                colorStart += 13; // Skip past "tagColor" : "
            } else {
                colorStart += 12; // Skip past "tagColor":"
            }
            
            int colorEnd = response.indexOf("\"", colorStart);
            if (colorEnd > colorStart) {
                String color = response.substring(colorStart, colorEnd);
                System.out.println("[KillCounter] Extracted guild tag color: " + color);
                return color;
            }
            
            System.out.println("[KillCounter] Could not parse guild tag color, using WHITE");
            return "WHITE";
        } catch (Exception e) {
            System.err.println("[KillCounter] Error extracting guild tag color: " + e.getMessage());
            return "WHITE";
        }
    }
    
    /**
     * Fixes problematic Unicode characters in guild tags for Minecraft compatibility
     */
    private static String fixGuildTagUnicode(String guildTag) {
        if (guildTag == null || guildTag.isEmpty()) {
            return guildTag;
        }
        
        // Replace problematic Unicode characters with Minecraft-compatible alternatives
        return guildTag
            .replace("✧", "\u2727")      // White four pointed star
            .replace("✪", "\u272A")      // Circled white star
            .replace("✖", "\u2716")      // Heavy multiplication x
            .replace("✓", "\u2713")      // Heavy check mark
            .replace("✿", "\u273F")      // Heavy eight petalled rosette
            .replace("✌", "\u270C")      // Victory hand
            .replace("➊", "\u278A")      // Dingbat circled sans-serif digit one
            .replace("\u2764", "\u2764");    // Heavy black heart
    }
    
    /**
     * Converts Hypixel guild tag color to Minecraft formatting
     */
    private static String getGuildTagColor(String tagColor) {
        if (tagColor == null) return "";
        
        switch (tagColor.toUpperCase()) {
            case "GRAY":
                return EnumChatFormatting.GRAY.toString();
            case "DARK_AQUA":
                return EnumChatFormatting.DARK_AQUA.toString();
            case "DARK_GREEN":
                return EnumChatFormatting.DARK_GREEN.toString();
            case "YELLOW":
                return EnumChatFormatting.YELLOW.toString();
            case "GOLD":
                return EnumChatFormatting.GOLD.toString();
            default:
                return EnumChatFormatting.WHITE.toString();
        }
    }
}
