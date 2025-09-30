package me.ballmc.gomod.features;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Separate player list used by Kit Levels feature.
 * Does NOT depend on the TeamSpam list.
 */
public class KitPlayersManager {
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.weave/gomod123";
    private static final String PLAYERS_FILE = CONFIG_DIR + "/kitlevel_players.json";
    private static final String RESOURCE_PLAYERS_FILE = "/players.json"; // fallback bundled resource
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class Player {
        private String username;
        private String uuid;

        public Player(String username, String uuid) {
            this.username = username;
            this.uuid = uuid;
        }

        public String getUsername() { return username; }
        public String getUuid() { return uuid; }
    }

    private static List<Player> players = new ArrayList<>();
    private static boolean loaded = false;

    public static synchronized void loadPlayers() {
        if (loaded) return;
        players.clear();

        try {
            File dir = new File(CONFIG_DIR);
            if (!dir.exists()) dir.mkdirs();

            File file = new File(PLAYERS_FILE);
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    Player[] arr = GSON.fromJson(reader, Player[].class);
                    if (arr != null) {
                        Collections.addAll(players, arr);
                    }
                }
            } else {
                // fallback to resource players
                InputStream is = KitPlayersManager.class.getResourceAsStream(RESOURCE_PLAYERS_FILE);
                if (is != null) {
                    try (InputStreamReader reader = new InputStreamReader(is)) {
                        JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                        JsonArray playersArray = json.getAsJsonArray("players");
                        for (int i = 0; i < playersArray.size(); i++) {
                            JsonObject p = playersArray.get(i).getAsJsonObject();
                            String name = p.get("name").getAsString();
                            String uuid = p.get("uuid").getAsString();
                            players.add(new Player(name, uuid));
                        }
                    }
                }
                // Persist the initial list so the user can edit later
                savePlayers();
            }
        } catch (Exception e) {
            System.err.println("[KitPlayersManager] Error loading players: " + e.getMessage());
        }

        loaded = true;
    }

    public static synchronized void savePlayers() {
        try {
            File dir = new File(CONFIG_DIR);
            if (!dir.exists()) dir.mkdirs();
            try (FileWriter writer = new FileWriter(PLAYERS_FILE)) {
                GSON.toJson(players, writer);
            }
        } catch (Exception e) {
            System.err.println("[KitPlayersManager] Error saving players: " + e.getMessage());
        }
    }

    public static synchronized List<Player> getPlayers() {
        if (!loaded) loadPlayers();
        return new ArrayList<>(players);
    }

    public static synchronized void setPlayers(List<Player> newPlayers) {
        players = new ArrayList<>(newPlayers);
        savePlayers();
    }

    public static synchronized void addPlayer(String username, String uuid) {
        if (!loaded) loadPlayers();
        players.add(new Player(username, uuid));
        savePlayers();
    }

    // When no file exists, seed with requested default names and resolve UUIDs
    static {
        try {
            File file = new File(PLAYERS_FILE);
            if (!file.exists()) {
                List<Player> seed = new ArrayList<>();
                String[] defaultNames = new String[] {"naku", "_checkered", "Jurce", "Banjoboom17", "UrTryhard"};
                for (String name : defaultNames) {
                    String[] data = resolvePlayer(name);
                    if (data != null) {
                        seed.add(new Player(data[0], data[1]));
                    } else {
                        // store name with empty uuid; can be resolved later
                        seed.add(new Player(name, ""));
                    }
                }
                players = seed;
                savePlayers();
            }
        } catch (Exception e) {
            System.err.println("[KitPlayersManager] Error seeding defaults: " + e.getMessage());
        }
    }

    public static String[] resolvePlayer(String username) {
        BufferedReader reader = null;
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() != 200) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            JsonObject obj = new JsonParser().parse(sb.toString()).getAsJsonObject();
            if (obj.has("id") && obj.has("name")) {
                String uuid = obj.get("id").getAsString();
                String correct = obj.get("name").getAsString();
                return new String[] { correct, uuid };
            }
        } catch (Exception ignored) {
        } finally {
            try { if (reader != null) reader.close(); } catch (Exception ignored) {}
        }
        return null;
    }
}


