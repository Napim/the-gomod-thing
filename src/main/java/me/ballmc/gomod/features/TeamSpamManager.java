package me.ballmc.gomod.features;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.util.EnumChatFormatting;

/**
 * Manages players for team spam functionality.
 */
public class TeamSpamManager {
    // Use a directory consistent with ConfigManager
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.weave/gomod123";
    private static final String PLAYERS_FILE = CONFIG_DIR + "/tspam_list.json";
    private static final String RESOURCE_PLAYERS_FILE = "/players.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type PLAYER_LIST_TYPE = new TypeToken<List<Player>>() {}.getType();
    
    private static List<Player> players = new ArrayList<>();
    private static boolean loaded = false;
    private static boolean usingResourceFile = false;
    
    /**
     * Represents a player for team spam.
     */
    public static class Player {
        private String username;
        private String uuid;
        private long addedTimestamp;
        private long lastFoundTimestamp; // Separate timestamp for when player was last found
        private boolean hasMVPPlusPlus = true; // Default to true until proven otherwise
        private String rank = ""; // Store the actual rank
        private String rankPlusColor = ""; // Store rank plus color for MVP+
        private String monthlyRankColor = ""; // Store monthly rank color for MVP++
        
        public Player(String username, String uuid) {
            this.username = username;
            this.uuid = uuid;
            this.addedTimestamp = System.currentTimeMillis();
            this.lastFoundTimestamp = 0; // Not found yet
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getUuid() {
            return uuid;
        }
        
        public long getAddedTimestamp() {
            return addedTimestamp;
        }
        
        public void setAddedTimestamp(long timestamp) {
            this.addedTimestamp = timestamp;
        }
        
        public long getLastFoundTimestamp() {
            return lastFoundTimestamp;
        }
        
        public void setLastFoundTimestamp(long timestamp) {
            this.lastFoundTimestamp = timestamp;
        }
        
        public boolean hasMVPPlusPlus() {
            return hasMVPPlusPlus;
        }
        
        public void setHasMVPPlusPlus(boolean hasMVPPlusPlus) {
            this.hasMVPPlusPlus = hasMVPPlusPlus;
        }
        
        public String getRank() {
            return rank;
        }
        
        public void setRank(String rank) {
            this.rank = rank;
        }
        
        public String getRankPlusColor() {
            return rankPlusColor;
        }
        
        public void setRankPlusColor(String rankPlusColor) {
            this.rankPlusColor = rankPlusColor;
        }
        
        public String getMonthlyRankColor() {
            return monthlyRankColor;
        }
        
        public void setMonthlyRankColor(String monthlyRankColor) {
            this.monthlyRankColor = monthlyRankColor;
        }
    }
    
    /**
     * Loads players from the JSON file.
     */
    public static void loadPlayers() {
        boolean loadedAnyFile = false;
        usingResourceFile = false;
        
        System.out.println("[TeamSpamManager] Starting to load players...");
        
        // Ensure directory exists
        File configDirFile = new File(CONFIG_DIR);
        if (!configDirFile.exists()) {
            boolean dirCreated = configDirFile.mkdirs();
            if (dirCreated) {
                System.out.println("[TeamSpamManager] Created config directory: " + CONFIG_DIR);
            } else {
                System.out.println("[TeamSpamManager] Failed to create config directory: " + CONFIG_DIR);
            }
        }
        
        // Try to load from the player file
        File file = new File(PLAYERS_FILE);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                players = GSON.fromJson(reader, PLAYER_LIST_TYPE);
                if (players == null) {
                    players = new ArrayList<>();
                } else {
                    loadedAnyFile = true;
                    System.out.println("[TeamSpamManager] Loaded " + players.size() + " players from " + PLAYERS_FILE);
                    
                    // If we successfully loaded from the main file, don't try to load from other sources
                    // This prevents duplicate player entries
                    loaded = true;
                    return;
                }
            } catch (IOException e) {
                System.err.println("[TeamSpamManager] Error loading players from " + PLAYERS_FILE + ": " + e.getMessage());
                players = new ArrayList<>();
            }
        } else {
            System.out.println("[TeamSpamManager] File " + PLAYERS_FILE + " does not exist");
        }
        
        // If we couldn't load from the file, load the default players from resource
        if (!loadedAnyFile) {
            players.clear(); // Start with a clean list
            
            try {
                System.out.println("[TeamSpamManager] Attempting to load default players from resource");
                InputStream is = TeamSpamManager.class.getResourceAsStream(RESOURCE_PLAYERS_FILE);
                
                if (is != null) {
                    try (InputStreamReader reader = new InputStreamReader(is)) {
                        // Resource file has a different format
                        JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                        JsonArray playersArray = json.getAsJsonArray("players");
                        
                        for (int i = 0; i < playersArray.size(); i++) {
                            JsonObject playerObj = playersArray.get(i).getAsJsonObject();
                            String name = playerObj.get("name").getAsString();
                            String uuid = playerObj.get("uuid").getAsString();
                            players.add(new Player(name, uuid));
                        }
                        
                        loadedAnyFile = true;
                        usingResourceFile = true;
                        System.out.println("[TeamSpamManager] Successfully loaded " + players.size() + " default players from resource");
                    }
                } else {
                    System.out.println("[TeamSpamManager] No resource file found at " + RESOURCE_PLAYERS_FILE);
                    
                    // Add a default list of players as fallback
                    System.out.println("[TeamSpamManager] Using hardcoded default player list");
                    players.add(new Player("_checkered", "12a6476f-059c-4924-a56f-c4807433df62"));
                    players.add(new Player("cjk500", "17f23144-5440-4d37-9598-ea466c2c150f"));
                    players.add(new Player("Jurce", "be5bc621-1107-478f-8c93-e4d2975e4afa"));
                    // Just a few defaults as example
                    
                    loadedAnyFile = true;
                    usingResourceFile = true;
                }
                
                // Print players for verification
                int countToShow = Math.min(5, players.size());
                System.out.println("[TeamSpamManager] First " + countToShow + " default players:");
                for (int i = 0; i < countToShow; i++) {
                    System.out.println("  - " + players.get(i).getUsername() + " (" + players.get(i).getUuid() + ")");
                }
                
            } catch (Exception e) {
                System.err.println("[TeamSpamManager] Error loading default players: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // If all methods failed, create a new empty file
        if (!loadedAnyFile) {
            System.out.println("[TeamSpamManager] No player files found. Creating new empty file.");
            players = new ArrayList<>();
            savePlayers();
        }
        
        System.out.println("[TeamSpamManager] Final player count: " + players.size());
        System.out.println("[TeamSpamManager] Using resource file: " + usingResourceFile);
        
        loaded = true;
    }
    
    /**
     * Saves players to the JSON file.
     */
    public static void savePlayers() {
        try {
            // Ensure the directory exists
            File configDirFile = new File(CONFIG_DIR);
            if (!configDirFile.exists()) {
                boolean dirCreated = configDirFile.mkdirs();
                if (!dirCreated) {
                    System.out.println("[TeamSpamManager] Failed to create config directory: " + CONFIG_DIR);
                }
            }
            
            File file = new File(PLAYERS_FILE);
            if (!file.exists()) {
                file.createNewFile();
            }
            
            FileWriter writer = new FileWriter(file);
            GSON.toJson(players, writer);
            writer.close();
            
            // When we save players, we're no longer using the resource file
            usingResourceFile = false;
            
            System.out.println("Saved " + players.size() + " players to " + PLAYERS_FILE);
        } catch (IOException e) {
            System.err.println("Error saving players: " + e.getMessage());
        }
    }
    
    /**
     * Adds a player to the team spam list.
     * 
     * @param username The player's username
     * @return A message indicating success or failure
     */
    public static String addPlayer(String username) {
        if (!loaded) {
            loadPlayers();
        }
        
        // Check if player already exists
        for (Player player : players) {
            if (player.getUsername().equalsIgnoreCase(username)) {
                return me.ballmc.gomod.Main.CHAT_PREFIX + "Player " + player.getUsername() + " is already in the team spam list.";
            }
        }
        
        try {
            // Get correct username and UUID from Mojang API
            String[] playerData = getPlayerData(username);
            if (playerData == null) {
                return "Failed to get data for player " + username + ". Player might not exist.";
            }
            
            String correctUsername = playerData[0];
            String uuid = playerData[1];
            
            // Build player and enrich with rank info if possible
            Player newPlayer = new Player(correctUsername, uuid);

            // Try to fetch rank details via Hypixel API if a key is set
            String apiKey = ApiKeyManager.getApiKey("hypixel");
            String rankTagForMessage = "";
            if (apiKey != null && !apiKey.isEmpty()) {
                try {
                    String[] rankData = checkPlayerRank(uuid, apiKey);
                    boolean hasMVPPlusPlus = rankData[0].equals("MVP++");
                    newPlayer.setHasMVPPlusPlus(hasMVPPlusPlus);
                    newPlayer.setRank(rankData[0]);
                    if (rankData.length > 1) newPlayer.setRankPlusColor(rankData[1]);
                    if (rankData.length > 2) newPlayer.setMonthlyRankColor(rankData[2]);
                    rankTagForMessage = buildRankTag(newPlayer);
                } catch (Exception rankEx) {
                    // Keep defaults if rank lookup fails; include a brief note in the message
                    rankTagForMessage = "";
                }
            }
            
            // Add player to list
            players.add(newPlayer);
            savePlayers();
            
            String usernameColor = getUsernameColor(newPlayer);
            if (!correctUsername.equals(username)) {
                return me.ballmc.gomod.Main.CHAT_PREFIX + "Added player " + rankTagForMessage + " " + usernameColor + correctUsername + EnumChatFormatting.RESET + " to team spam list.";
            } else {
                return me.ballmc.gomod.Main.CHAT_PREFIX + "Added player " + rankTagForMessage + " " + usernameColor + correctUsername + EnumChatFormatting.RESET + " to team spam list.";
            }
        } catch (Exception e) {
            if (e.getMessage().contains("403")) {
                return "API Error: No Hypixel API key set. Please set one with /gmapi hypixel <your-api-key>";
            } else if (e.getMessage().contains("401")) {
                return "API Error: Invalid Hypixel API key. Please renew it with /gmapi hypixel <your-api-key>";
            } else {
                return "Error adding player: " + e.getMessage();
            }
        }
    }

    private static String buildRankTag(Player player) {
        String rank = player.getRank();
        if (rank == null || rank.isEmpty() || "Default".equals(rank)) {
            return "";
        }
        // MVP++ special coloring: monthlyRankColor for base, rankPlusColor for ++
        if ("MVP++".equals(rank)) {
            String base = getMVPPlusPlusColor(player.getMonthlyRankColor());
            String plus = getMVPPlusPlusSignsColor(player.getRankPlusColor());
            return base + " [MVP" + plus + "++" + base + "]" + EnumChatFormatting.RESET;
        }
        // MVP+ special coloring: aqua base, rankPlusColor for +
        if ("MVP+".equals(rank)) {
            String base = EnumChatFormatting.AQUA + "";
            String plus = getMVPPlusColor(player.getRankPlusColor());
            return base + " [MVP" + plus + "+" + base + "]" + EnumChatFormatting.RESET;
        }
        // Common ranks
        String color = getRankColor(rank);
        return color + " [" + rank + "]" + EnumChatFormatting.RESET;
    }
    
    private static String getUsernameColor(Player player) {
        String rank = player.getRank();
        if (rank == null || rank.isEmpty()) {
            return EnumChatFormatting.WHITE + "";
        }
        // MVP++: use monthlyRankColor for username
        if ("MVP++".equals(rank)) {
            return getMVPPlusPlusColor(player.getMonthlyRankColor());
        }
        // MVP+: use aqua for username
        if ("MVP+".equals(rank)) {
            return EnumChatFormatting.AQUA + "";
        }
        // Other ranks: use rank color for username
        return getRankColor(rank);
    }

    private static String getRankColor(String rank) {
        if (rank == null) return EnumChatFormatting.YELLOW + "";
        switch (rank) {
            case "OWNER": return EnumChatFormatting.DARK_RED + "";
            case "ADMIN": return EnumChatFormatting.RED + "";
            case "GM": return EnumChatFormatting.DARK_GREEN + "";
            case "YOUTUBER": return EnumChatFormatting.GOLD + "";
            case "MOJANG": return EnumChatFormatting.GOLD + "";
            case "EVENTS": return EnumChatFormatting.LIGHT_PURPLE + "";
            case "INNIT": return EnumChatFormatting.LIGHT_PURPLE + "";
            case "MVP": return EnumChatFormatting.AQUA + "";
            case "VIP+": return EnumChatFormatting.GOLD + "";
            case "VIP": return EnumChatFormatting.GREEN + "";
            case "Default": return EnumChatFormatting.GRAY + "";
            default: return EnumChatFormatting.YELLOW + "";
        }
    }

    private static String getMVPPlusColor(String rankPlusColor) {
        if (rankPlusColor == null || rankPlusColor.isEmpty()) {
            return EnumChatFormatting.GOLD + "";
        }
        switch (rankPlusColor) {
            case "RED": return EnumChatFormatting.RED + "";
            case "BLUE": return EnumChatFormatting.BLUE + "";
            case "GREEN": return EnumChatFormatting.GREEN + "";
            case "YELLOW": return EnumChatFormatting.YELLOW + "";
            case "GOLD": return EnumChatFormatting.GOLD + "";
            case "AQUA": return EnumChatFormatting.AQUA + "";
            case "LIGHT_PURPLE": return EnumChatFormatting.LIGHT_PURPLE + "";
            case "DARK_PURPLE": return EnumChatFormatting.DARK_PURPLE + "";
            case "WHITE": return EnumChatFormatting.WHITE + "";
            case "GRAY": return EnumChatFormatting.GRAY + "";
            case "DARK_GRAY": return EnumChatFormatting.DARK_GRAY + "";
            case "BLACK": return EnumChatFormatting.BLACK + "";
            default: return EnumChatFormatting.GOLD + "";
        }
    }

    private static String getMVPPlusPlusColor(String monthlyRankColor) {
        if (monthlyRankColor == null || monthlyRankColor.isEmpty()) {
            return EnumChatFormatting.GOLD + "";
        }
        switch (monthlyRankColor) {
            case "DEFAULT": return EnumChatFormatting.GOLD + "";
            case "DARK_PURPLE": return EnumChatFormatting.DARK_PURPLE + "";
            case "LIGHT_PURPLE": return EnumChatFormatting.LIGHT_PURPLE + "";
            case "DARK_AQUA": return EnumChatFormatting.DARK_AQUA + "";
            case "AQUA": return EnumChatFormatting.AQUA + "";
            case "RED": return EnumChatFormatting.RED + "";
            case "GOLD": return EnumChatFormatting.GOLD + "";
            case "GREEN": return EnumChatFormatting.GREEN + "";
            case "YELLOW": return EnumChatFormatting.YELLOW + "";
            case "WHITE": return EnumChatFormatting.WHITE + "";
            case "BLUE": return EnumChatFormatting.BLUE + "";
            case "DARK_GREEN": return EnumChatFormatting.DARK_GREEN + "";
            case "DARK_RED": return EnumChatFormatting.DARK_RED + "";
            case "DARK_GRAY": return EnumChatFormatting.DARK_GRAY + "";
            case "BLACK": return EnumChatFormatting.BLACK + "";
            case "DARK_BLUE": return EnumChatFormatting.DARK_BLUE + "";
            default: return EnumChatFormatting.GOLD + "";
        }
    }

    private static String getMVPPlusPlusSignsColor(String rankPlusColor) {
        if (rankPlusColor == null || rankPlusColor.isEmpty()) {
            return EnumChatFormatting.GOLD + "";
        }
        switch (rankPlusColor) {
            case "DEFAULT": return EnumChatFormatting.GOLD + "";
            case "DARK_PURPLE": return EnumChatFormatting.DARK_PURPLE + "";
            case "LIGHT_PURPLE": return EnumChatFormatting.LIGHT_PURPLE + "";
            case "DARK_AQUA": return EnumChatFormatting.DARK_AQUA + "";
            case "AQUA": return EnumChatFormatting.AQUA + "";
            case "RED": return EnumChatFormatting.RED + "";
            case "GOLD": return EnumChatFormatting.GOLD + "";
            case "GREEN": return EnumChatFormatting.GREEN + "";
            case "YELLOW": return EnumChatFormatting.YELLOW + "";
            case "WHITE": return EnumChatFormatting.WHITE + "";
            case "BLUE": return EnumChatFormatting.BLUE + "";
            case "DARK_GREEN": return EnumChatFormatting.DARK_GREEN + "";
            case "DARK_RED": return EnumChatFormatting.DARK_RED + "";
            case "DARK_GRAY": return EnumChatFormatting.DARK_GRAY + "";
            case "BLACK": return EnumChatFormatting.BLACK + "";
            case "DARK_BLUE": return EnumChatFormatting.DARK_BLUE + "";
            default: return EnumChatFormatting.GOLD + "";
        }
    }

    /**
     * Updates a single player's information (name and rank) using Mojang and Hypixel APIs.
     * @param username The player's current username (case-insensitive)
     * @return A result message for chat
     */
    public static String updatePlayerInfo(String username) {
        if (!loaded) {
            loadPlayers();
        }

        Player player = findPlayerByName(username);
        if (player == null) {
            return me.ballmc.gomod.Main.CHAT_PREFIX + "Player " + username + " is not in the team spam list.";
        }

        String oldUsername = player.getUsername();
        String uuid = player.getUuid();
        int updates = 0;
        int errors = 0;

        // Update current username via Mojang API
        try {
            String currentUsername = getCurrentUsername(uuid);
            if (currentUsername != null && !currentUsername.equals(oldUsername)) {
                player.username = currentUsername;
                updates++;
            }
        } catch (Exception e) {
            errors++;
        }

        // Update rank info via Hypixel API
        String apiKey = ApiKeyManager.getApiKey("hypixel");
        boolean rankUpdated = false;
        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                String[] rankData = checkPlayerRank(uuid, apiKey);
                boolean hasMVPPlusPlus = rankData[0].equals("MVP++");
                player.setHasMVPPlusPlus(hasMVPPlusPlus);
                player.setRank(rankData[0]);
                player.setRankPlusColor(rankData.length > 1 ? rankData[1] : "");
                player.setMonthlyRankColor(rankData.length > 2 ? rankData[2] : "");
                rankUpdated = true;
                updates++;
            } catch (Exception e) {
                errors++;
            }
        }

        savePlayers();

        StringBuilder msg = new StringBuilder();
        msg.append(me.ballmc.gomod.Main.CHAT_PREFIX)
           .append("Updated ");
        String tag = buildRankTag(player);
        if (!tag.isEmpty()) {
            msg.append(tag).append(" ");
        }
        String usernameColor = getUsernameColor(player);
        msg.append(usernameColor).append(player.getUsername()).append(EnumChatFormatting.RESET);
        if (updates == 0) {
            msg.append(". No changes.");
        }
        if (rankUpdated) {
            msg.append(" ").append(EnumChatFormatting.GRAY).append("(Rank refreshed)");
        }
        if (errors > 0) {
            msg.append(" ").append(EnumChatFormatting.GRAY).append("(" + errors + " issue" + (errors == 1 ? "" : "s") + " during update)");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            msg.append(" ").append(EnumChatFormatting.YELLOW).append("(Set Hypixel API key to update rank)");
        }
        return msg.toString();
    }
    
    /**
     * Removes a player from the team spam list.
     * 
     * @param username The player's username
     * @return A message indicating success or failure
     */
    public static String removePlayer(String username) {
        if (!loaded) {
            loadPlayers();
        }
        
        // Sanitize incoming name (strip color codes and rank tags)
        String sanitized = username
            .replaceAll("\u00A7[0-9A-FK-ORa-fk-or]", "")
            .replaceAll("^\\[[^]]+\\]\\s+", "")
            .trim();
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getUsername().equalsIgnoreCase(sanitized)) {
                Player removed = players.get(i);
                String correctUsername = removed.getUsername();
                String tag = buildRankTag(removed);
                String usernameColor = getUsernameColor(removed);
                players.remove(i);
                savePlayers();
                return me.ballmc.gomod.Main.CHAT_PREFIX + EnumChatFormatting.RESET + "Removed player " +
                    (tag.isEmpty() ? "" : tag + " ") + usernameColor + correctUsername + EnumChatFormatting.RESET + " from team spam list.";
            }
        }
        
        return me.ballmc.gomod.Main.CHAT_PREFIX + EnumChatFormatting.RESET + "Player " + EnumChatFormatting.WHITE + sanitized + EnumChatFormatting.RESET + " is not in the team spam list.";
    }
    
    /**
     * Gets a list of all players in the team spam list, sorted by most recently added.
     * 
     * @return A list of player usernames
     */
    public static List<Player> getPlayers() {
        if (!loaded) {
            loadPlayers();
        }
        
        // Sort by most recently added
        List<Player> sortedPlayers = new ArrayList<>(players);
        Collections.sort(sortedPlayers, new Comparator<Player>() {
            @Override
            public int compare(Player p1, Player p2) {
                return Long.compare(p2.getAddedTimestamp(), p1.getAddedTimestamp());
            }
        });
        
        return sortedPlayers;
    }
    
    /**
     * Checks if the current player list is from the resource file.
     * 
     * @return true if using the resource file, false otherwise
     */
    public static boolean isUsingResourceFile() {
        if (!loaded) {
            loadPlayers();
        }
        return usingResourceFile;
    }
    
    /**
     * Clears all players from the team spam list.
     */
    public static void clearPlayers() {
        players.clear();
        savePlayers();
        System.out.println("Cleared all players from team spam list.");
    }
    
    /**
     * Gets player data (username and UUID) from the Mojang API.
     * 
     * @param username The player's username
     * @return An array containing [correctUsername, uuid], or null if not found
     */
    private static String[] getPlayerData(String username) throws Exception {
        URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        if (conn.getResponseCode() == 403) {
            throw new Exception("403 - Forbidden. API key may be missing or invalid.");
        } else if (conn.getResponseCode() == 401) {
            throw new Exception("401 - Unauthorized. API key is invalid.");
        } else if (conn.getResponseCode() != 200) {
            return null;
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        JsonObject jsonResponse = new JsonParser().parse(response.toString()).getAsJsonObject();
        if (jsonResponse.has("id") && jsonResponse.has("name")) {
            String uuid = jsonResponse.get("id").getAsString();
            String correctUsername = jsonResponse.get("name").getAsString();
            return new String[] { correctUsername, uuid };
        }
        
        return null;
    }
    
    /**
     * Updates the last found timestamp for a player to mark them as recently found.
     * 
     * @param username The player's username
     * @return True if the player was found and updated, false otherwise
     */
    public static boolean updatePlayerTimestamp(String username) {
        if (!loaded) {
            loadPlayers();
        }
        
        // Find the player in the list (case insensitive)
        for (Player player : players) {
            if (player.getUsername().equalsIgnoreCase(username)) {
                // Update the last found timestamp to current time (not the added timestamp)
                player.setLastFoundTimestamp(System.currentTimeMillis());
                
                // Save the changes
                savePlayers();
                
                System.out.println("[TeamSpamManager] Updated last found timestamp for player " + player.getUsername() + " to " + new java.text.SimpleDateFormat("MM/dd/yy HH:mm").format(new java.util.Date()));
                
                // Notify GUI to refresh if it's open
                notifyGUIRefresh();
                
                return true;
            }
        }
        
        // Player not found
        return false;
    }
    
    /**
     * Notifies the GUI to refresh the player list when a player is found.
     */
    private static void notifyGUIRefresh() {
        try {
            // Get the current GUI screen
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc != null) {
                // Schedule a GUI refresh on the main thread
                mc.addScheduledTask(() -> {
                    try {
                        // Use the static method to refresh the GUI
                        me.ballmc.gomod.gui.TeamSpamGUI.refreshCurrentGUI();
                    } catch (Exception e) {
                        System.out.println("[TeamSpamManager] Could not refresh GUI: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            // Silently handle any errors to avoid disrupting the main functionality
            System.out.println("[TeamSpamManager] Error notifying GUI refresh: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Finds a player in the team spam list by their name.
     * 
     * @param username The player's username
     * @return The Player object if found, or null if not found
     */
    public static Player findPlayerByName(String username) {
        if (!loaded) {
            loadPlayers();
        }
        
        for (Player player : players) {
            if (player.getUsername().equalsIgnoreCase(username)) {
                return player;
            }
        }
        
        return null;
    }
    
    /**
     * Checks all players' MVP++ status using the Hypixel API and updates player names based on their UUIDs.
     * Players without MVP++ will be marked accordingly, and names will be updated if they've changed.
     * 
     * @return A message with the results of the check
     */
    public static String checkAllPlayersMVPPlusPlus() {
        if (!loaded) {
            loadPlayers();
        }
        
        String apiKey = ApiKeyManager.getApiKey("hypixel");
        if (apiKey == null || apiKey.isEmpty()) {
            return "Error: No Hypixel API key set. Please set one with /gmapi hypixel <your-api-key>";
        }
        
        int checkedCount = 0;
        int mvpPlusPlusCount = 0;
        int nameUpdatedCount = 0;
        int errorCount = 0;
        
        int processed = 0;
        long windowStart = System.currentTimeMillis();
        for (Player player : players) {
            try {
                // First, check if the player's name has changed
                String uuid = player.getUuid();
                String oldUsername = player.getUsername();
                String currentUsername = getCurrentUsername(uuid);
                
                if (currentUsername != null && !currentUsername.equals(oldUsername)) {
                    // Player has changed their name
                    System.out.println("[TeamSpamManager] Player name changed: " + oldUsername + " -> " + currentUsername);
                    player.username = currentUsername; // Update the name
                    nameUpdatedCount++;
                }
                
                // Then, check MVP++ status and rank
                String[] rankData = checkPlayerRank(uuid, apiKey);
                boolean hasMVPPlusPlus = rankData[0].equals("MVP++");
                String rank = rankData[0];
                String rankPlusColor = rankData.length > 1 ? rankData[1] : "";
                String monthlyRankColor = rankData.length > 2 ? rankData[2] : "";
                
                player.setHasMVPPlusPlus(hasMVPPlusPlus);
                player.setRank(rank);
                player.setRankPlusColor(rankPlusColor);
                player.setMonthlyRankColor(monthlyRankColor);
                checkedCount++;
                
                if (hasMVPPlusPlus) {
                    mvpPlusPlusCount++;
                }
                
                // Rate limiting: Hypixel allows 300 requests / 5 minutes
                processed++;
                if (processed % 300 == 0) {
                    long elapsed = System.currentTimeMillis() - windowStart;
                    long minWindow = 5 * 60 * 1000L; // 5 minutes
                    if (elapsed < minWindow) {
                        long sleepMs = minWindow - elapsed;
                        try { Thread.sleep(sleepMs); } catch (InterruptedException ignored) {}
                    }
                    windowStart = System.currentTimeMillis();
                }
                // Enforce 0.1s per player pacing regardless of other delays
                long paceStart = System.nanoTime();
                long targetNs = 100_000_000L; // 100ms
                long nowNs = System.nanoTime();
                long remaining = targetNs - (nowNs - paceStart);
                if (remaining > 0) {
                    try { Thread.sleep(remaining / 1_000_000L, (int)(remaining % 1_000_000L)); } catch (InterruptedException ignored) {}
                }
            } catch (Exception e) {
                System.err.println("[TeamSpamManager] Error checking player " + player.getUsername() + ": " + e.getMessage());
                errorCount++;
            }
        }
        
        // Save the updated player data
        savePlayers();
        
        StringBuilder result = new StringBuilder();
        result.append("Checked ").append(checkedCount).append(" players: ");
        result.append(mvpPlusPlusCount).append(" have MVP++, ");
        result.append(checkedCount - mvpPlusPlusCount).append(" do not. ");
        
        // Always report name check results, even if no names were updated
        result.append("Checked ").append(checkedCount).append(" player names");
        if (nameUpdatedCount > 0) {
            result.append(": ").append(nameUpdatedCount).append(" were updated");
        } else {
            result.append(": all names are current");
        }
        result.append(". ");
        
        if (errorCount > 0) {
            result.append(errorCount).append(" errors occurred.");
        }
        
        return result.toString();
    }
    
    /**
     * Checks a player's rank using the Hypixel API.
     * 
     * @param uuid The player's UUID
     * @param apiKey The Hypixel API key
     * @return Array with [rank, rankPlusColor, monthlyRankColor] where rank is the actual rank string
     */
    private static String[] checkPlayerRank(String uuid, String apiKey) throws Exception {
        URL url = new URL("https://api.hypixel.net/player?key=" + apiKey + "&uuid=" + uuid);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        if (conn.getResponseCode() != 200) {
            throw new Exception("Failed API request: " + conn.getResponseCode() + " " + conn.getResponseMessage());
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        JsonObject jsonResponse = new JsonParser().parse(response.toString()).getAsJsonObject();
        
        // Check if the request was successful
        if (!jsonResponse.get("success").getAsBoolean()) {
            throw new Exception("API request was not successful: " + 
                               (jsonResponse.has("cause") ? jsonResponse.get("cause").getAsString() : "Unknown reason"));
        }
        
        // Check if player exists in response
        if (!jsonResponse.has("player") || jsonResponse.get("player").isJsonNull()) {
            return new String[]{"Default", "", ""}; // Player not found, assume default rank
        }
        
        JsonObject playerData = jsonResponse.getAsJsonObject("player");
        
        // Extract colors first
        String rankPlusColor = playerData.has("rankPlusColor") ? playerData.get("rankPlusColor").getAsString() : "";
        String monthlyRankColor = playerData.has("monthlyRankColor") ? playerData.get("monthlyRankColor").getAsString() : "";
        
        // Check for special prefixes first
        if (playerData.has("prefix")) {
            String prefix = playerData.get("prefix").getAsString();
            if (prefix.contains("MOJANG")) {
                return new String[]{"MOJANG", "", ""};
            } else if (prefix.contains("EVENTS")) {
                return new String[]{"EVENTS", "", ""};
            } else if (prefix.contains("INNIT")) {
                return new String[]{"INNIT", "", ""};
            } else if (prefix.contains("OWNER")) {
                return new String[]{"OWNER", "", ""};
            }
        }
        
        // Check for special ranks
        if (playerData.has("rank")) {
            String rank = playerData.get("rank").getAsString();
            if (rank.equals("YOUTUBER")) {
                return new String[]{"YOUTUBER", "", ""};
            } else if (rank.equals("ADMIN")) {
                // Check if it's actually OWNER based on prefix
                if (playerData.has("prefix") && playerData.get("prefix").getAsString().contains("OWNER")) {
                    return new String[]{"OWNER", "", ""};
                }
                return new String[]{"ADMIN", "", ""};
            } else if (rank.equals("GAME_MASTER")) {
                return new String[]{"GM", "", ""};
            } else if (rank.equals("MVP_PLUS_PLUS")) {
                return new String[]{"MVP++", rankPlusColor, monthlyRankColor};
            }
        }
        
        // Check for MVP++ (monthly rank)
        if (playerData.has("monthlyPackageRank")) {
            String monthlyRank = playerData.get("monthlyPackageRank").getAsString();
            if (monthlyRank.equals("SUPERSTAR")) {
                return new String[]{"MVP++", rankPlusColor, monthlyRankColor};
            }
        }
        
        // Check new package rank system
        if (playerData.has("newPackageRank")) {
            String packageRank = playerData.get("newPackageRank").getAsString();
            if (packageRank.equals("MVP_PLUS")) {
                return new String[]{"MVP+", rankPlusColor, ""};
            } else if (packageRank.equals("MVP")) {
                return new String[]{"MVP", "", ""};
            } else if (packageRank.equals("VIP_PLUS")) {
                return new String[]{"VIP+", "", ""};
            } else if (packageRank.equals("VIP")) {
                return new String[]{"VIP", "", ""};
            }
        }
        
        // Check old package rank system (fallback)
        if (playerData.has("packageRank")) {
            String packageRank = playerData.get("packageRank").getAsString();
            if (packageRank.equals("MVP_PLUS")) {
                return new String[]{"MVP+", rankPlusColor, ""};
            } else if (packageRank.equals("MVP")) {
                return new String[]{"MVP", "", ""};
            } else if (packageRank.equals("VIP_PLUS")) {
                return new String[]{"VIP+", "", ""};
            } else if (packageRank.equals("VIP")) {
                return new String[]{"VIP", "", ""};
            }
        }
        
        return new String[]{"Default", "", ""};
    }
    
    
    /**
     * Gets a player's current username from their UUID using the Mojang API.
     * 
     * @param uuid The player's UUID (without dashes)
     * @return The player's current username, or null if not found
     */
    private static String getCurrentUsername(String uuid) throws Exception {
        URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        if (conn.getResponseCode() != 200) {
            throw new Exception("Failed to get player data: " + conn.getResponseCode() + " " + conn.getResponseMessage());
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        JsonObject jsonResponse = new JsonParser().parse(response.toString()).getAsJsonObject();
        if (jsonResponse.has("name")) {
            return jsonResponse.get("name").getAsString();
        }
        
        return null;
    }
    
    /**
     * Updates all player names in the team spam list to their current names,
     * based on their UUIDs. This ensures the system works when players change their names.
     * 
     * @return A result message indicating how many names were updated
     */
    public static String updateAllPlayerNames() {
        if (!loaded) {
            loadPlayers();
        }
        
        int checkedCount = 0;
        int updatedCount = 0;
        int errorCount = 0;
        
        int processed = 0;
        long windowStart = System.currentTimeMillis();
        for (Player player : players) {
            try {
                String uuid = player.getUuid();
                String oldUsername = player.getUsername();
                String currentUsername = getCurrentUsername(uuid);
                
                if (currentUsername != null && !currentUsername.equals(oldUsername)) {
                    // Player has changed their name
                    System.out.println("[TeamSpamManager] Player name changed: " + oldUsername + " -> " + currentUsername);
                    player.username = currentUsername; // Update the name
                    updatedCount++;
                }
                
                checkedCount++;
                processed++;
                // Batch limit 300 requests per 5 minutes
                if (processed % 300 == 0) {
                    long elapsed = System.currentTimeMillis() - windowStart;
                    long minWindow = 5 * 60 * 1000L; // 5 minutes
                    if (elapsed < minWindow) {
                        long sleepMs = minWindow - elapsed;
                        try { Thread.sleep(sleepMs); } catch (InterruptedException ignored) {}
                    }
                    windowStart = System.currentTimeMillis();
                }
                // Enforce 0.1s per player pacing regardless
                long paceStart = System.nanoTime();
                long targetNs = 100_000_000L; // 100ms
                long nowNs = System.nanoTime();
                long remaining = targetNs - (nowNs - paceStart);
                if (remaining > 0) {
                    try { Thread.sleep(remaining / 1_000_000L, (int)(remaining % 1_000_000L)); } catch (InterruptedException ignored) {}
                }
            } catch (Exception e) {
                System.err.println("[TeamSpamManager] Error updating name for " + player.getUsername() + ": " + e.getMessage());
                errorCount++;
            }
        }
        
        // Save the updated player data if any names changed
        if (updatedCount > 0) {
            savePlayers();
        }
        
        return "Checked " + checkedCount + " players: " + updatedCount + " had name changes and were updated." +
               (errorCount > 0 ? " " + errorCount + " errors occurred." : "");
    }
}
