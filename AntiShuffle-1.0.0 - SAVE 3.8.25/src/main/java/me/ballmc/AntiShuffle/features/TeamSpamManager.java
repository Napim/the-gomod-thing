package me.ballmc.AntiShuffle.features;

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
import java.util.UUID;

/**
 * Manages players for team spam functionality.
 */
public class TeamSpamManager {
    // Use a single player file in the main directory
    private static final String PLAYERS_FILE = "antishuffle_players.json";
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
        private boolean hasMVPPlusPlus = true; // Default to true until proven otherwise
        
        public Player(String username, String uuid) {
            this.username = username;
            this.uuid = uuid;
            this.addedTimestamp = System.currentTimeMillis();
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
        
        public boolean hasMVPPlusPlus() {
            return hasMVPPlusPlus;
        }
        
        public void setHasMVPPlusPlus(boolean hasMVPPlusPlus) {
            this.hasMVPPlusPlus = hasMVPPlusPlus;
        }
    }
    
    /**
     * Loads players from the JSON file.
     */
    public static void loadPlayers() {
        boolean loadedAnyFile = false;
        usingResourceFile = false;
        
        System.out.println("[TeamSpamManager] Starting to load players...");
        
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
                return "Player " + player.getUsername() + " is already in the team spam list.";
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
            
            // Add player to list
            players.add(new Player(correctUsername, uuid));
            savePlayers();
            
            if (!correctUsername.equals(username)) {
                return "Added player " + correctUsername + " (corrected from " + username + ") to team spam list.";
            } else {
                return "Added player " + correctUsername + " to team spam list.";
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
        
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getUsername().equalsIgnoreCase(username)) {
                String correctUsername = players.get(i).getUsername();
                players.remove(i);
                savePlayers();
                return "Removed player " + correctUsername + " from team spam list.";
            }
        }
        
        return "Player " + username + " is not in the team spam list.";
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
     * Updates the timestamp for a player to mark them as recently found.
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
                // Update the timestamp to current time
                player.setAddedTimestamp(System.currentTimeMillis());
                
                // Save the changes
                savePlayers();
                
                System.out.println("[TeamSpamManager] Updated timestamp for player " + player.getUsername() + " to " + new java.text.SimpleDateFormat("MM/dd/yy HH:mm").format(new java.util.Date()));
                return true;
            }
        }
        
        // Player not found
        return false;
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
                
                // Then, check MVP++ status
                boolean hasMVPPlusPlus = checkPlayerMVPPlusPlus(uuid, apiKey);
                player.setHasMVPPlusPlus(hasMVPPlusPlus);
                checkedCount++;
                
                if (hasMVPPlusPlus) {
                    mvpPlusPlusCount++;
                }
                
                // Add a small delay to avoid rate limits
                Thread.sleep(100);
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
     * Checks if a player has MVP++ rank using the Hypixel API.
     * 
     * @param uuid The player's UUID
     * @param apiKey The Hypixel API key
     * @return true if the player has MVP++, false otherwise
     */
    private static boolean checkPlayerMVPPlusPlus(String uuid, String apiKey) throws Exception {
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
            return false; // Player not found, assume no MVP++
        }
        
        JsonObject playerData = jsonResponse.getAsJsonObject("player");
        
        // Check for rank or rankPlusColor (MVP++ specific field)
        if (playerData.has("rank")) {
            String rank = playerData.get("rank").getAsString();
            return rank.equals("MVP_PLUS_PLUS");
        } else if (playerData.has("monthlyPackageRank")) {
            String monthlyRank = playerData.get("monthlyPackageRank").getAsString();
            return monthlyRank.equals("SUPERSTAR");
        } else if (playerData.has("newPackageRank") && playerData.has("rankPlusColor")) {
            String packageRank = playerData.get("newPackageRank").getAsString();
            return packageRank.equals("MVP_PLUS");
        }
        
        return false;
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
                
                // Add a small delay to avoid rate limits
                Thread.sleep(100);
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