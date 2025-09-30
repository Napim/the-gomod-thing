package carlaus.gomod.features;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages persistent configuration settings for the mod.
 * Saves and loads user preferences between game sessions.
 */
public class ConfigManager {
    private static final String CONFIG_FILE = "gomod_config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static JsonObject config = new JsonObject();
    private static boolean loaded = false;
    
    // Settings
    private static boolean antiShuffleEnabled = true;
    private static boolean goStatsEnabled = true;
    private static boolean autoTranslateEnabled = false;
    private static Map<String, String> apiKeys = new HashMap<>();
    
    /**
     * Loads configuration settings from file
     */
    public static void loadConfig() {
        if (loaded) return;
        
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            System.out.println("Config file not found. Creating with default settings.");
            saveConfig();
            loaded = true;
            return;
        }
        
        try (FileReader reader = new FileReader(configFile)) {
            JsonParser parser = new JsonParser();
            config = parser.parse(reader).getAsJsonObject();
            
            // Load antishuffle and goStats settings
            if (config.has("antiShuffleEnabled")) {
                antiShuffleEnabled = config.get("antiShuffleEnabled").getAsBoolean();
            }
            
            if (config.has("goStatsEnabled")) {
                goStatsEnabled = config.get("goStatsEnabled").getAsBoolean();
            }
            
            if (config.has("autoTranslateEnabled")) {
                autoTranslateEnabled = config.get("autoTranslateEnabled").getAsBoolean();
            }
            
            // Load API keys
            if (config.has("apiKeys")) {
                JsonObject apiKeysJson = config.getAsJsonObject("apiKeys");
                Set<Map.Entry<String, JsonElement>> entries = apiKeysJson.entrySet();
                
                for (Map.Entry<String, JsonElement> entry : entries) {
                    apiKeys.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            
            loaded = true;
            System.out.println("Config loaded successfully");
            
        } catch (Exception e) {
            System.out.println("Error loading config: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Saves current configuration settings to file
     */
    public static void saveConfig() {
        try {
            // Create the config object
            config = new JsonObject();
            
            // Save antishuffle and goStats settings
            config.addProperty("antiShuffleEnabled", antiShuffleEnabled);
            config.addProperty("goStatsEnabled", goStatsEnabled);
            config.addProperty("autoTranslateEnabled", autoTranslateEnabled);
            
            // Save API keys
            JsonObject apiKeysJson = new JsonObject();
            for (Map.Entry<String, String> entry : apiKeys.entrySet()) {
                apiKeysJson.addProperty(entry.getKey(), entry.getValue());
            }
            config.add("apiKeys", apiKeysJson);
            
            // Write to file
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(config, writer);
                System.out.println("Config saved successfully");
            }
            
        } catch (Exception e) {
            System.out.println("Error saving config: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Checks if antishuffle is enabled.
     * @return true if antishuffle is enabled, false otherwise
     */
    public static boolean isAntishuffleEnabled() {
        if (!loaded) {
            loadConfig();
        }
        return antiShuffleEnabled;
    }
    
    /**
     * Sets whether antishuffle is enabled.
     * @param enabled true to enable antishuffle, false to disable
     */
    public static void setAntishuffleEnabled(boolean enabled) {
        antiShuffleEnabled = enabled;
        saveConfig();
    }
    
    /**
     * Checks if GoStats is enabled.
     * @return true if GoStats is enabled, false otherwise
     */
    public static boolean isGoStatsEnabled() {
        if (!loaded) {
            loadConfig();
        }
        return goStatsEnabled;
    }
    
    /**
     * Sets whether GoStats is enabled.
     * @param enabled true to enable GoStats, false to disable
     */
    public static void setGoStatsEnabled(boolean enabled) {
        goStatsEnabled = enabled;
        saveConfig();
    }
    
    /**
     * Checks if auto-translation is enabled.
     * @return true if auto-translation is enabled, false otherwise
     */
    public static boolean isAutoTranslateEnabled() {
        if (!loaded) {
            loadConfig();
        }
        return autoTranslateEnabled;
    }
    
    /**
     * Sets whether auto-translation is enabled.
     * @param enabled true to enable auto-translation, false to disable
     */
    public static void setAutoTranslateEnabled(boolean enabled) {
        autoTranslateEnabled = enabled;
        saveConfig();
    }
    
    /**
     * Gets the API key for a given service.
     * @param service the service name
     * @return the API key, or null if not set
     */
    public static String getApiKey(String service) {
        if (!loaded) {
            loadConfig();
        }
        return apiKeys.get(service);
    }
    
    /**
     * Sets an API key for a service.
     * @param service the service name
     * @param apiKey the API key to set
     */
    public static void setApiKey(String service, String apiKey) {
        apiKeys.put(service, apiKey);
        saveConfig();
    }
    
    /**
     * Removes an API key for a service.
     * @param service the service name
     */
    public static void removeApiKey(String service) {
        apiKeys.remove(service);
        saveConfig();
    }
} 