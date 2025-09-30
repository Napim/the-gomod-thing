package carlaus.gomod.features;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages player data for team commands.
 */
public class TeamSpamManager {
    
    private static final String PLAYERS_FILE = "gomod_players.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static List<Player> players = new ArrayList<>();
    private static boolean loaded = false;
    
    /**
     * Represents a player with associated data
     */
    public static class Player {
        private String name;
        private String nickname;
        private boolean favorite;
        
        public Player(String name, String nickname, boolean favorite) {
            this.name = name;
            this.nickname = nickname;
            this.favorite = favorite;
        }
        
        public String getName() {
            return name;
        }
        
        public String getNickname() {
            return nickname;
        }
        
        public boolean isFavorite() {
            return favorite;
        }
        
        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
        
        public void setFavorite(boolean favorite) {
            this.favorite = favorite;
        }
    }
    
    /**
     * Loads player data from file
     */
    public static void loadPlayers() {
        if (loaded) return;
        
        File playersFile = new File(PLAYERS_FILE);
        if (!playersFile.exists()) {
            System.out.println("Players file not found. Creating empty file.");
            savePlayers();
            loaded = true;
            return;
        }
        
        try (FileReader reader = new FileReader(playersFile)) {
            JsonParser parser = new JsonParser();
            JsonArray playersArray = parser.parse(reader).getAsJsonArray();
            
            players.clear();
            for (JsonElement element : playersArray) {
                JsonObject playerObj = element.getAsJsonObject();
                String name = playerObj.get("name").getAsString();
                String nickname = playerObj.has("nickname") ? playerObj.get("nickname").getAsString() : name;
                boolean favorite = playerObj.has("favorite") && playerObj.get("favorite").getAsBoolean();
                
                players.add(new Player(name, nickname, favorite));
            }
            
            loaded = true;
            System.out.println("Loaded " + players.size() + " players");
            
        } catch (Exception e) {
            System.out.println("Error loading players: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Saves player data to file
     */
    public static void savePlayers() {
        try {
            JsonArray playersArray = new JsonArray();
            
            for (Player player : players) {
                JsonObject playerObj = new JsonObject();
                playerObj.addProperty("name", player.getName());
                playerObj.addProperty("nickname", player.getNickname());
                playerObj.addProperty("favorite", player.isFavorite());
                playersArray.add(playerObj);
            }
            
            try (FileWriter writer = new FileWriter(PLAYERS_FILE)) {
                GSON.toJson(playersArray, writer);
                System.out.println("Saved " + players.size() + " players");
            }
            
        } catch (Exception e) {
            System.out.println("Error saving players: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Adds a player to the list
     * @param name the player's name
     * @param nickname the player's nickname
     * @param favorite whether the player is a favorite
     * @return true if the player was added, false if they already exist
     */
    public static boolean addPlayer(String name, String nickname, boolean favorite) {
        if (!loaded) {
            loadPlayers();
        }
        
        for (Player player : players) {
            if (player.getName().equalsIgnoreCase(name)) {
                return false; // Player already exists
            }
        }
        
        players.add(new Player(name, nickname, favorite));
        savePlayers();
        return true;
    }
    
    /**
     * Removes a player from the list
     * @param name the player's name
     * @return true if the player was removed, false if they don't exist
     */
    public static boolean removePlayer(String name) {
        if (!loaded) {
            loadPlayers();
        }
        
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getName().equalsIgnoreCase(name)) {
                players.remove(i);
                savePlayers();
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Gets all players
     * @return the list of players
     */
    public static List<Player> getPlayers() {
        if (!loaded) {
            loadPlayers();
        }
        return players;
    }
    
    /**
     * Gets favorite players
     * @return the list of favorite players
     */
    public static List<Player> getFavoritePlayers() {
        List<Player> favorites = new ArrayList<>();
        
        for (Player player : getPlayers()) {
            if (player.isFavorite()) {
                favorites.add(player);
            }
        }
        
        return favorites;
    }
} 