package me.ballmc.gomod.features;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages kit levels data for players using Hypixel API.
 * Provides functionality to search for players with specific kits and their levels.
 */
public class KitLevelsManager {
    
    // Kits that use experience-based leveling instead of direct level values
    private static final Set<String> EXP_BASED_KITS = Set.of(
        "ranger", "donkeytamer", "warrior", "phoenix", "milkman"
    );
    
    // Kit name mapping (display name -> API name)
    private static final Map<String, String> KIT_NAME_MAPPING = new HashMap<String, String>() {{
        put("Knight", "knight");
        put("Archer", "archer");
        put("Blaze", "blaze");
        put("Golem", "golem");
        put("Guardian", "guardian");
        put("Phoenix", "phoenix");
        put("Pigman", "pigman");
        put("Ranger", "ranger");
        put("Red Dragon", "reddragon");
        put("Rogue", "rogue");
        put("Scout", "scout");
        put("Shark", "shark");
        put("Slimey Slime", "slimeyslime");
        put("Snowman", "snowman");
        put("Speleologist", "speleologist");
        put("Tim", "tim");
        put("Toxicologist", "toxicologist");
        put("Troll", "troll");
        put("Viking", "viking");
        put("Warlock", "warlock");
        put("Wolf Tamer", "wolftamer");
        put("Baker", "baker");
        put("Armorer", "armorer");
        put("Creeper Tamer", "creepertamer");
        put("Fisherman", "fisherman");
        put("Florist", "florist");
        put("Horse Tamer", "horsetamer");
        put("Hunter", "hunter");
        put("Meat Master", "meatmaster");
        put("Necromancer", "necromancer");
        put("Reaper", "reaper");
        put("Donkey Tamer", "donkeytamer");
        put("Arachnologist", "arachnologist");
        put("Diver", "diver");
        put("Paladin", "paladin");
        put("Shadow Knight", "shadow knight");
        put("Astronaut", "astronaut");
        put("Warrior", "warrior");
        put("Milkman", "milkman");
        put("Hype Train", "hype train");
        put("Jockey", "jockey");
        put("Rambo", "rambo");
        put("Farmer", "farmer");
        put("Random", "random");
    }};
    
    private static final Map<String, List<KitPlayer>> kitCache = new ConcurrentHashMap<>();
    private static final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes
    // Rank cache: uuid -> { rank, rankPlusColor, monthlyRankColor }
    private static final Map<String, String[]> rankCache = new ConcurrentHashMap<>();
    
    /**
     * Represents a player with kit level information.
     */
    public static class KitPlayer {
        private String username;
        private String uuid;
        private int level;
        private int prestige;
        private boolean isPrestiged;
        private String levelDisplay;
        private String kitName; // Display name of the kit
        private String apiKitName; // API name of the kit
        private String rank; // Player rank (MVP+, MVP++, etc.)
        private String rankPlusColor; // Color for + in MVP+
        private String monthlyRankColor; // Color for MVP++
        
        public KitPlayer(String username, String uuid, int level, int prestige, boolean isPrestiged) {
            this.username = username;
            this.uuid = uuid;
            this.level = level;
            this.prestige = prestige;
            this.isPrestiged = isPrestiged;
            this.levelDisplay = isPrestiged ? 
                (prestige == 1 ? "★" : "★★") : 
                intToRoman(level);
        }
        
        public KitPlayer(String username, String uuid, int level, int prestige, boolean isPrestiged, String kitName, String apiKitName) {
            this.username = username;
            this.uuid = uuid;
            this.level = level;
            this.prestige = prestige;
            this.isPrestiged = isPrestiged;
            this.kitName = kitName;
            this.apiKitName = apiKitName;
            this.levelDisplay = isPrestiged ? 
                (prestige == 1 ? "★" : "★★") : 
                intToRoman(level);
        }
        
        public KitPlayer(String username, String uuid, int level, int prestige, boolean isPrestiged, String kitName, String apiKitName, String rank, String rankPlusColor, String monthlyRankColor) {
            this.username = username;
            this.uuid = uuid;
            this.level = level;
            this.prestige = prestige;
            this.isPrestiged = isPrestiged;
            this.kitName = kitName;
            this.apiKitName = apiKitName;
            this.rank = rank;
            this.rankPlusColor = rankPlusColor;
            this.monthlyRankColor = monthlyRankColor;
            this.levelDisplay = isPrestiged ? 
                (prestige == 1 ? "★" : "★★") : 
                intToRoman(level);
        }
        
        // Getters
        public String getUsername() { return username; }
        public String getUuid() { return uuid; }
        public int getLevel() { return level; }
        public int getPrestige() { return prestige; }
        public boolean isPrestiged() { return isPrestiged; }
        public String getLevelDisplay() { return levelDisplay; }
        public String getKitName() { return kitName; }
        public String getApiKitName() { return apiKitName; }
        public String getRank() { return rank; }
        public String getRankPlusColor() { return rankPlusColor; }
        public String getMonthlyRankColor() { return monthlyRankColor; }
        
        @Override
        public String toString() {
            if (kitName != null) {
                return String.format("%s (%s): %s", username, kitName, levelDisplay);
            }
            return String.format("%s: %s", username, levelDisplay);
        }
    }
    
    /**
     * Gets all players from the team spam list that have the specified kit.
     * 
     * @param kitName The kit name to search for
     * @return A list of KitPlayer objects sorted by level (highest first)
     */
    public static CompletableFuture<List<KitPlayer>> getPlayersWithKit(String kitName) {
        // Normalize kit name
        String normalizedKitName = normalizeKitName(kitName);
        if (normalizedKitName == null) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        // Check cache first
        if (isCacheValid(normalizedKitName)) {
            return CompletableFuture.completedFuture(new ArrayList<>(kitCache.get(normalizedKitName)));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<KitPlayer> playersWithKit = new ArrayList<>();
                List<me.ballmc.gomod.features.KitPlayersManager.Player> allPlayers = me.ballmc.gomod.features.KitPlayersManager.getPlayers();
                
                String apiKey = ApiKeyManager.getApiKey("hypixel");
                if (apiKey == null || apiKey.isEmpty()) {
                    System.err.println("[KitLevelsManager] No Hypixel API key set");
                    return playersWithKit;
                }
                
                for (me.ballmc.gomod.features.KitPlayersManager.Player player : allPlayers) {
                    try {
                        KitPlayer kitPlayer = getPlayerKitLevel(player.getUuid(), player.getUsername(), normalizedKitName, apiKey);
                        if (kitPlayer != null) {
                            playersWithKit.add(kitPlayer);
                        }
                        
                        // Add delay to avoid rate limits
                        Thread.sleep(100);
                    } catch (Exception e) {
                        System.err.println("[KitLevelsManager] Error checking kit for " + player.getUsername() + ": " + e.getMessage());
                    }
                }
                
                // Sort by level (highest first)
                playersWithKit.sort((a, b) -> {
                    if (a.isPrestiged() && !b.isPrestiged()) return -1;
                    if (!a.isPrestiged() && b.isPrestiged()) return 1;
                    if (a.isPrestiged() && b.isPrestiged()) {
                        return Integer.compare(b.getPrestige(), a.getPrestige());
                    }
                    return Integer.compare(b.getLevel(), a.getLevel());
                });
                
                // Cache the results
                kitCache.put(normalizedKitName, new ArrayList<>(playersWithKit));
                cacheTimestamps.put(normalizedKitName, System.currentTimeMillis());
                
                return playersWithKit;
                
            } catch (Exception e) {
                System.err.println("[KitLevelsManager] Error getting players with kit: " + e.getMessage());
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }
    
    /**
     * Gets the kit level for a specific player and kit.
     */
    private static KitPlayer getPlayerKitLevel(String uuid, String username, String apiKitName, String apiKey) throws Exception {
        JsonObject playerData = getHypixelPlayerData(uuid, apiKey);
        if (playerData == null) {
            return null;
        }
        
        // Extract Blitz stats
        JsonObject blitzStats = playerData.getAsJsonObject("stats")
            .getAsJsonObject("HungerGames");
        
        if (blitzStats == null) {
            return null;
        }
        // Extract rank info (without extra API call) and cache it
        String rank = "Default";
        String rankPlusColor = null;
        String monthlyRankColor = null;
        try {
            if (playerData.has("rank") && !playerData.get("rank").isJsonNull()) {
                String r = playerData.get("rank").getAsString();
                if (r != null && !r.isEmpty() && !"NORMAL".equals(r)) {
                    rank = r;
                }
            }
            if (playerData.has("monthlyPackageRank") && !playerData.get("monthlyPackageRank").isJsonNull()) {
                String monthly = playerData.get("monthlyPackageRank").getAsString();
                if (!"NONE".equals(monthly)) {
                    rank = "MVP++";
                    if (playerData.has("monthlyRankColor") && !playerData.get("monthlyRankColor").isJsonNull()) {
                        monthlyRankColor = playerData.get("monthlyRankColor").getAsString();
                    } else {
                        monthlyRankColor = "DEFAULT";
                    }
                }
            }
            if ("Default".equals(rank) && playerData.has("newPackageRank") && !playerData.get("newPackageRank").isJsonNull()) {
                String pkg = playerData.get("newPackageRank").getAsString();
                if ("MVP_PLUS".equals(pkg)) {
                    rank = "MVP+";
                    if (playerData.has("rankPlusColor") && !playerData.get("rankPlusColor").isJsonNull()) {
                        rankPlusColor = playerData.get("rankPlusColor").getAsString();
                    }
                } else if ("MVP".equals(pkg)) {
                    rank = "MVP";
                } else if ("VIP_PLUS".equals(pkg)) {
                    rank = "VIP+";
                } else if ("VIP".equals(pkg)) {
                    rank = "VIP";
                }
            }
        } catch (Exception ignored) {}
        cacheRankInfo(uuid, rank, rankPlusColor, monthlyRankColor);

        return processKitLevelWithRank(username, uuid, apiKitName, blitzStats, null, rank, rankPlusColor, monthlyRankColor);
    }
    
    /**
     * Processes kit level data from Hypixel API response.
     */
    private static KitPlayer processKitLevel(String username, String uuid, String apiKitName, JsonObject blitzStats) {
        return processKitLevel(username, uuid, apiKitName, blitzStats, null);
    }
    
    /**
     * Processes kit level data from Hypixel API response with kit name.
     */
    private static KitPlayer processKitLevel(String username, String uuid, String apiKitName, JsonObject blitzStats, String displayKitName) {
        // Determine ownership: prefer explicit has_{kit}; otherwise infer from any kit stat present
        boolean ownsKit = false;
        String hasKitKey = "has_" + apiKitName;
        if (blitzStats.has(hasKitKey)) {
            ownsKit = blitzStats.get(hasKitKey).getAsBoolean();
        } else {
            // Infer: if any of these keys have data, assume ownership
            String expKey = "exp_" + apiKitName;
            if ((blitzStats.has(apiKitName) && !blitzStats.get(apiKitName).isJsonNull()) ||
                (blitzStats.has(expKey) && !blitzStats.get(expKey).isJsonNull()) ||
                (blitzStats.has("p" + apiKitName) && !blitzStats.get("p" + apiKitName).isJsonNull())) {
                ownsKit = true;
            }
        }
        if (!ownsKit) {
            return null;
        }
        
        // Get prestige level
        String prestigeKey = "p" + apiKitName;
        int prestigeLevel = blitzStats.has(prestigeKey) ? blitzStats.get(prestigeKey).getAsInt() : 0;
        
        int kitLevel;
        boolean isPrestiged = prestigeLevel > 0;
        
        if (isPrestiged) {
            // Prestiged kits are always level 1
            kitLevel = 1;
        } else {
            // Calculate level based on kit type
            if (EXP_BASED_KITS.contains(apiKitName)) {
                // Experience-based leveling
                String expKey = "exp_" + apiKitName;
                int exp = blitzStats.has(expKey) ? blitzStats.get(expKey).getAsInt() : 0;
                kitLevel = getKitLevelFromExp(exp);
            } else {
                // Direct level value (stored as level-1 in API)
                kitLevel = blitzStats.has(apiKitName) ? blitzStats.get(apiKitName).getAsInt() + 1 : 1;
            }
        }
        
        return new KitPlayer(username, uuid, kitLevel, prestigeLevel, isPrestiged, displayKitName, apiKitName);
    }
    
    /**
     * Processes kit level data from Hypixel API response with rank information.
     */
    private static KitPlayer processKitLevelWithRank(String username, String uuid, String apiKitName, JsonObject blitzStats, String displayKitName, String rank, String rankPlusColor, String monthlyRankColor) {
        // Determine ownership: prefer explicit has_{kit}; otherwise infer from any kit stat present
        boolean ownsKit = false;
        String hasKitKey2 = "has_" + apiKitName;
        if (blitzStats.has(hasKitKey2)) {
            ownsKit = blitzStats.get(hasKitKey2).getAsBoolean();
        } else {
            String expKey = "exp_" + apiKitName;
            if ((blitzStats.has(apiKitName) && !blitzStats.get(apiKitName).isJsonNull()) ||
                (blitzStats.has(expKey) && !blitzStats.get(expKey).isJsonNull()) ||
                (blitzStats.has("p" + apiKitName) && !blitzStats.get("p" + apiKitName).isJsonNull())) {
                ownsKit = true;
            }
        }
        if (!ownsKit) {
            return null;
        }
        
        // Get prestige level
        String prestigeKey = "p" + apiKitName;
        int prestigeLevel = blitzStats.has(prestigeKey) ? blitzStats.get(prestigeKey).getAsInt() : 0;
        
        int kitLevel;
        boolean isPrestiged = prestigeLevel > 0;
        
        if (isPrestiged) {
            // Prestiged kits are always level 1
            kitLevel = 1;
        } else {
            // Calculate level based on kit type
            if (EXP_BASED_KITS.contains(apiKitName)) {
                // Experience-based leveling
                String expKey = "exp_" + apiKitName;
                int exp = blitzStats.has(expKey) ? blitzStats.get(expKey).getAsInt() : 0;
                kitLevel = getKitLevelFromExp(exp);
            } else {
                // Direct level value (stored as level-1 in API)
                kitLevel = blitzStats.has(apiKitName) ? blitzStats.get(apiKitName).getAsInt() + 1 : 1;
            }
        }
        
        return new KitPlayer(username, uuid, kitLevel, prestigeLevel, isPrestiged, displayKitName, apiKitName, rank, rankPlusColor, monthlyRankColor);
    }
    
    /**
     * Converts experience to kit level for experience-based kits.
     */
    private static int getKitLevelFromExp(int exp) {
        if (exp < 100) return 1;
        if (exp < 250) return 2;
        if (exp < 500) return 3;
        if (exp < 1000) return 4;
        if (exp < 1500) return 5;
        if (exp < 2000) return 6;
        if (exp < 2500) return 7;
        if (exp < 5000) return 8;
        if (exp < 10000) return 9;
        return 10;
    }
    
    /**
     * Converts integer to Roman numeral.
     */
    private static String intToRoman(int num) {
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (num >= values[i]) {
                result.append(symbols[i]);
                num -= values[i];
            }
        }
        return result.toString();
    }
    
    /**
     * Gets Hypixel player data from API.
     */
    private static JsonObject getHypixelPlayerData(String uuid, String apiKey) throws Exception {
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
            return null; // Player not found
        }
        
        return jsonResponse.getAsJsonObject("player");
    }
    
    /**
     * Normalizes kit name for API lookup.
     */
    private static String normalizeKitName(String kitName) {
        // Direct mapping check
        if (KIT_NAME_MAPPING.containsKey(kitName)) {
            return KIT_NAME_MAPPING.get(kitName);
        }
        
        // Case-insensitive search
        for (Map.Entry<String, String> entry : KIT_NAME_MAPPING.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(kitName)) {
                return entry.getValue();
            }
        }
        
        // Partial match search
        for (Map.Entry<String, String> entry : KIT_NAME_MAPPING.entrySet()) {
            if (entry.getKey().toLowerCase().contains(kitName.toLowerCase()) || 
                kitName.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * Checks if cache is valid for a kit.
     */
    private static boolean isCacheValid(String kitName) {
        if (!kitCache.containsKey(kitName) || !cacheTimestamps.containsKey(kitName)) {
            return false;
        }
        
        long cacheTime = cacheTimestamps.get(kitName);
        return (System.currentTimeMillis() - cacheTime) < CACHE_DURATION;
    }
    
    /**
     * Clears the cache for a specific kit or all kits.
     */
    public static void clearCache(String kitName) {
        if (kitName != null) {
            kitCache.remove(kitName);
            cacheTimestamps.remove(kitName);
        } else {
            kitCache.clear();
            cacheTimestamps.clear();
        }
    }

    // Persist caches to disk (simple JSON)
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.weave/gomod123";
    private static final String KIT_CACHE_FILE = CONFIG_DIR + "/kitlevel_cache.json";

    public static synchronized void saveCaches() {
        try {
            java.io.File dir = new java.io.File(CONFIG_DIR);
            if (!dir.exists()) dir.mkdirs();
            com.google.gson.JsonObject root = new com.google.gson.JsonObject();
            com.google.gson.JsonObject kits = new com.google.gson.JsonObject();
            for (Map.Entry<String, List<KitPlayer>> e : kitCache.entrySet()) {
                com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
                for (KitPlayer kp : e.getValue()) {
                    com.google.gson.JsonObject o = new com.google.gson.JsonObject();
                    o.addProperty("username", kp.getUsername());
                    o.addProperty("uuid", kp.getUuid());
                    o.addProperty("level", kp.getLevel());
                    o.addProperty("prestige", kp.getPrestige());
                    o.addProperty("isPrestiged", kp.isPrestiged());
                    o.addProperty("kitName", kp.getKitName());
                    o.addProperty("apiKitName", kp.getApiKitName());
                    o.addProperty("rank", kp.getRank());
                    o.addProperty("rankPlusColor", kp.getRankPlusColor());
                    o.addProperty("monthlyRankColor", kp.getMonthlyRankColor());
                    arr.add(o);
                }
                kits.add(e.getKey(), arr);
            }
            root.add("kits", kits);
            com.google.gson.JsonObject ranks = new com.google.gson.JsonObject();
            for (Map.Entry<String, String[]> r : rankCache.entrySet()) {
                com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
                arr.add(new com.google.gson.JsonPrimitive(r.getValue()[0] == null ? "" : r.getValue()[0]));
                arr.add(new com.google.gson.JsonPrimitive(r.getValue()[1] == null ? "" : r.getValue()[1]));
                arr.add(new com.google.gson.JsonPrimitive(r.getValue()[2] == null ? "" : r.getValue()[2]));
                ranks.add(r.getKey(), arr);
            }
            root.add("ranks", ranks);
            java.io.FileWriter w = new java.io.FileWriter(KIT_CACHE_FILE);
            w.write(root.toString());
            w.close();
        } catch (Exception ignored) {}
    }

    public static synchronized void loadCaches() {
        try {
            java.io.File f = new java.io.File(KIT_CACHE_FILE);
            if (!f.exists()) return;
            java.io.FileReader r = new java.io.FileReader(f);
            com.google.gson.JsonObject root = new com.google.gson.JsonParser().parse(r).getAsJsonObject();
            r.close();
            if (root.has("kits")) {
                com.google.gson.JsonObject kits = root.getAsJsonObject("kits");
                for (Map.Entry<String, com.google.gson.JsonElement> e : kits.entrySet()) {
                    com.google.gson.JsonArray arr = e.getValue().getAsJsonArray();
                    List<KitPlayer> list = new ArrayList<>();
                    for (com.google.gson.JsonElement el : arr) {
                        com.google.gson.JsonObject o = el.getAsJsonObject();
                        KitPlayer kp = new KitPlayer(
                            o.get("username").getAsString(),
                            o.get("uuid").getAsString(),
                            o.get("level").getAsInt(),
                            o.get("prestige").getAsInt(),
                            o.get("isPrestiged").getAsBoolean(),
                            o.get("kitName").isJsonNull() ? null : o.get("kitName").getAsString(),
                            o.get("apiKitName").isJsonNull() ? null : o.get("apiKitName").getAsString(),
                            o.get("rank").isJsonNull() ? null : o.get("rank").getAsString(),
                            o.get("rankPlusColor").isJsonNull() ? null : o.get("rankPlusColor").getAsString(),
                            o.get("monthlyRankColor").isJsonNull() ? null : o.get("monthlyRankColor").getAsString()
                        );
                        list.add(kp);
                    }
                    kitCache.put(e.getKey(), list);
                    cacheTimestamps.put(e.getKey(), System.currentTimeMillis());
                }
            }
            if (root.has("ranks")) {
                com.google.gson.JsonObject ranks = root.getAsJsonObject("ranks");
                for (Map.Entry<String, com.google.gson.JsonElement> e : ranks.entrySet()) {
                    com.google.gson.JsonArray arr = e.getValue().getAsJsonArray();
                    rankCache.put(e.getKey(), new String[] {
                        arr.get(0).getAsString(),
                        arr.get(1).getAsString(),
                        arr.get(2).getAsString()
                    });
                }
            }
        } catch (Exception ignored) {}
    }
    
    /**
     * Gets all available kit names.
     */
    public static Set<String> getAvailableKits() {
        return new HashSet<>(KIT_NAME_MAPPING.keySet());
    }
    
    /**
     * Fetches only rank information for a player.
     * @return { rank, rankPlusColor, monthlyRankColor } (values may be null)
     */
    public static String[] getPlayerRankInfo(String uuid, String apiKey) throws Exception {
        JsonObject playerData = getHypixelPlayerData(uuid, apiKey);
        if (playerData == null) {
            return new String[] { "Default", null, null };
        }
        String rank = "Default";
        String rankPlusColor = null;
        String monthlyRankColor = null;
        if (playerData.has("rank") && !playerData.get("rank").isJsonNull()) {
            String r = playerData.get("rank").getAsString();
            if (r != null && !r.isEmpty() && !"NORMAL".equals(r)) {
                rank = r;
            }
        }
        if (playerData.has("monthlyPackageRank") && !playerData.get("monthlyPackageRank").isJsonNull()) {
            String monthly = playerData.get("monthlyPackageRank").getAsString();
            if (!"NONE".equals(monthly)) {
                rank = "MVP++";
                if (playerData.has("monthlyRankColor") && !playerData.get("monthlyRankColor").isJsonNull()) {
                    monthlyRankColor = playerData.get("monthlyRankColor").getAsString();
                } else {
                    monthlyRankColor = "DEFAULT";
                }
            }
        }
        if ("Default".equals(rank) && playerData.has("newPackageRank") && !playerData.get("newPackageRank").isJsonNull()) {
            String pkg = playerData.get("newPackageRank").getAsString();
            if ("MVP_PLUS".equals(pkg)) {
                rank = "MVP+";
                if (playerData.has("rankPlusColor") && !playerData.get("rankPlusColor").isJsonNull()) {
                    rankPlusColor = playerData.get("rankPlusColor").getAsString();
                }
            } else if ("MVP".equals(pkg)) {
                rank = "MVP";
            } else if ("VIP_PLUS".equals(pkg)) {
                rank = "VIP+";
            } else if ("VIP".equals(pkg)) {
                rank = "VIP";
            }
        }
        String[] info = new String[] { rank, rankPlusColor, monthlyRankColor };
        // store in cache
        rankCache.put(uuid, info);
        return info;
    }

    /**
     * Gets cached rank info if available.
     */
    public static String[] getCachedRankInfo(String uuid) {
        return rankCache.get(uuid);
    }

    /**
     * Gets rank info from cache, or fetches it from Hypixel and caches it.
     */
    public static String[] getOrFetchRankInfo(String uuid, String apiKey) throws Exception {
        String[] cached = getCachedRankInfo(uuid);
        if (cached != null) return cached;
        // Fallback: fetch fresh via player data
        JsonObject playerData = getHypixelPlayerData(uuid, apiKey);
        if (playerData == null) return new String[] { "Default", "", "" };
        String rank = "Default";
        String rankPlusColor = "";
        String monthlyRankColor = "";
        if (playerData.has("rank")) {
            rank = playerData.get("rank").getAsString();
        } else if (playerData.has("monthlyPackageRank")) {
            String monthlyRank = playerData.get("monthlyPackageRank").getAsString();
            if (!"NONE".equals(monthlyRank)) {
                rank = "MVP++";
                monthlyRankColor = playerData.has("monthlyRankColor") ? playerData.get("monthlyRankColor").getAsString() : "DEFAULT";
            } else if (playerData.has("newPackageRank")) {
                String packageRank = playerData.get("newPackageRank").getAsString();
                if ("MVP_PLUS".equals(packageRank)) {
                    rank = "MVP+";
                    rankPlusColor = playerData.has("rankPlusColor") ? playerData.get("rankPlusColor").getAsString() : "DEFAULT";
                } else if ("MVP".equals(packageRank)) {
                    rank = "MVP";
                } else if ("VIP_PLUS".equals(packageRank)) {
                    rank = "VIP+";
                } else if ("VIP".equals(packageRank)) {
                    rank = "VIP";
                }
            }
        }
        cacheRankInfo(uuid, rank, rankPlusColor, monthlyRankColor);
        return new String[] { rank, rankPlusColor, monthlyRankColor };
    }

    /**
     * Explicitly cache rank info.
     */
    public static void cacheRankInfo(String uuid, String rank, String rankPlusColor, String monthlyRankColor) {
        rankCache.put(uuid, new String[] { rank, rankPlusColor, monthlyRankColor });
    }

    /**
     * Gets all kits for a specific player.
     */
    public static List<KitPlayer> getPlayerAllKits(String uuid, String username, String apiKey) throws Exception {
        JsonObject playerData = getHypixelPlayerData(uuid, apiKey);
        if (playerData == null) {
            return new ArrayList<>();
        }
        
        // Extract player rank information
        String rank = "Default";
        String rankPlusColor = null;
        String monthlyRankColor = null;
        
        if (playerData.has("rank")) {
            rank = playerData.get("rank").getAsString();
        } else if (playerData.has("monthlyPackageRank")) {
            String monthlyRank = playerData.get("monthlyPackageRank").getAsString();
            if (!monthlyRank.equals("NONE")) {
                rank = "MVP++";
                monthlyRankColor = playerData.has("monthlyRankColor") ? playerData.get("monthlyRankColor").getAsString() : "DEFAULT";
            } else if (playerData.has("newPackageRank")) {
                String packageRank = playerData.get("newPackageRank").getAsString();
                if (packageRank.equals("MVP_PLUS")) {
                    rank = "MVP+";
                    rankPlusColor = playerData.has("rankPlusColor") ? playerData.get("rankPlusColor").getAsString() : "DEFAULT";
                } else if (packageRank.equals("MVP")) {
                    rank = "MVP";
                } else if (packageRank.equals("VIP_PLUS")) {
                    rank = "VIP+";
                } else if (packageRank.equals("VIP")) {
                    rank = "VIP";
                }
            }
        }
        
        // Cache the rank
        cacheRankInfo(uuid, rank, rankPlusColor, monthlyRankColor);

        // Extract Blitz stats
        JsonObject blitzStats = playerData.getAsJsonObject("stats")
            .getAsJsonObject("HungerGames");
        
        if (blitzStats == null) {
            return new ArrayList<>();
        }
        
        List<KitPlayer> allKits = new ArrayList<>();
        
        // Check all available kits
        for (Map.Entry<String, String> entry : KIT_NAME_MAPPING.entrySet()) {
            String displayName = entry.getKey();
            String apiName = entry.getValue();
            
            // Skip Random kit
            if ("random".equals(apiName)) {
                continue;
            }
            
            KitPlayer kitPlayer = processKitLevelWithRank(username, uuid, apiName, blitzStats, displayName, rank, rankPlusColor, monthlyRankColor);
            if (kitPlayer != null) {
                allKits.add(kitPlayer);
            }
        }
        
        return allKits;
    }
    
    /**
     * Caches all kit data for a player to make searches faster.
     */
    public static void cachePlayerKitData(String uuid, String username, String apiKey) throws Exception {
        List<KitPlayer> allKits = getPlayerAllKits(uuid, username, apiKey);
        
        // Cache each kit separately for easy searching
        for (KitPlayer kitPlayer : allKits) {
            String apiKitName = kitPlayer.getApiKitName();
            if (apiKitName != null) {
                if (!kitCache.containsKey(apiKitName)) {
                    kitCache.put(apiKitName, new ArrayList<>());
                }
                
                // Check if this player is already in the cache for this kit
                List<KitPlayer> cachedKits = kitCache.get(apiKitName);
                boolean found = false;
                for (int i = 0; i < cachedKits.size(); i++) {
                    if (cachedKits.get(i).getUuid().equals(uuid)) {
                        cachedKits.set(i, kitPlayer); // Update existing entry
                        found = true;
                        break;
                    }
                }
                
                if (!found) {
                    cachedKits.add(kitPlayer);
                }
                
                // Sort by level (highest first)
                cachedKits.sort((a, b) -> {
                    if (a.isPrestiged() && !b.isPrestiged()) return -1;
                    if (!a.isPrestiged() && b.isPrestiged()) return 1;
                    if (a.isPrestiged() && b.isPrestiged()) {
                        return Integer.compare(b.getPrestige(), a.getPrestige());
                    }
                    return Integer.compare(b.getLevel(), a.getLevel());
                });
                
                // Update cache timestamp
                cacheTimestamps.put(apiKitName, System.currentTimeMillis());
            }
        }
    }
}
