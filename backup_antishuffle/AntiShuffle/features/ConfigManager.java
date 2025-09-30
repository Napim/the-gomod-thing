package me.ballmc.AntiShuffle.features;

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
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.weave/gomod123";
    private static final String CONFIG_FILE = CONFIG_DIR + "/gomod123Config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static JsonObject config = new JsonObject();
    private static boolean loaded = false;
    
    // Default settings
    private static boolean antiShuffleEnabled = true;
    private static boolean goStatsEnabled = true;
    private static boolean autoTranslateEnabled = false;
    private static Map<String, String> apiKeys = new HashMap<>();
    private static boolean itemCallEnabled = false;
    
    private static final String AUTO_QUEUE_MODE_KEY = "autoQueueMode";
    
    /**
     * Loads the configuration from the config file.
     * If the file doesn't exist, default values are used.
     */
    public static void loadConfig() {
        // Create the directories if they don't exist
        File configDirFile = new File(CONFIG_DIR);
        if (!configDirFile.exists()) {
            boolean dirCreated = configDirFile.mkdirs();
            if (dirCreated) {
                System.out.println("[ConfigManager] Created config directory: " + CONFIG_DIR);
            } else {
                System.out.println("[ConfigManager] Failed to create config directory: " + CONFIG_DIR);
            }
        }
        
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            saveConfig();
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
            
            // Load auto-translate setting
            if (config.has("autoTranslateEnabled")) {
                autoTranslateEnabled = config.get("autoTranslateEnabled").getAsBoolean();
            }
            
            // Load API keys
            if (config.has("apiKeys")) {
                JsonObject apiKeysObject = config.getAsJsonObject("apiKeys");
                Set<Map.Entry<String, JsonElement>> entries = apiKeysObject.entrySet();
                
                for (Map.Entry<String, JsonElement> entry : entries) {
                    apiKeys.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            
            // Load itemCallEnabled setting
            if (config.has("itemCallEnabled")) {
                itemCallEnabled = config.get("itemCallEnabled").getAsBoolean();
            }
            
            // Load auto-queue mode setting
            // No need to store it in a variable as we access it directly via getAutoQueueMode()
            
            System.out.println("[ConfigManager] Loaded configuration from " + CONFIG_FILE);
            loaded = true;
        } catch (Exception e) {
            System.out.println("[ConfigManager] Error loading config: " + e.getMessage());
            e.printStackTrace();
            saveConfig(); // Create a new config file with default values
        }
    }

    /**
     * Saves the current configuration to the config file.
     */
    public static void saveConfig() {
        try {
            // Ensure directory exists
            File configDirFile = new File(CONFIG_DIR);
            if (!configDirFile.exists()) {
                boolean dirCreated = configDirFile.mkdirs();
                if (!dirCreated) {
                    System.out.println("[ConfigManager] Failed to create config directory: " + CONFIG_DIR);
                }
            }
            
            File configFile = new File(CONFIG_FILE);
            JsonObject config = new JsonObject();
            
            // Save antishuffle and goStats settings
            config.addProperty("antiShuffleEnabled", antiShuffleEnabled);
            config.addProperty("goStatsEnabled", goStatsEnabled);
            
            // Save auto-translate setting
            config.addProperty("autoTranslateEnabled", autoTranslateEnabled);
            
            // Save API keys
            JsonObject apiKeysObject = new JsonObject();
            for (Map.Entry<String, String> entry : apiKeys.entrySet()) {
                apiKeysObject.addProperty(entry.getKey(), entry.getValue());
            }
            config.add("apiKeys", apiKeysObject);
            
            // Save itemCallEnabled setting
            config.addProperty("itemCallEnabled", itemCallEnabled);
            
            // Save auto-queue mode setting
            if (config.has(AUTO_QUEUE_MODE_KEY)) {
                String mode = config.get(AUTO_QUEUE_MODE_KEY).getAsString();
                config.addProperty(AUTO_QUEUE_MODE_KEY, mode);
            }
            
            // Write to file with pretty printing
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(config, writer);
            }
            
            System.out.println("[ConfigManager] Saved configuration to " + CONFIG_FILE);
            loaded = true;
        } catch (IOException e) {
            System.out.println("[ConfigManager] Error saving config: " + e.getMessage());
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
     * Checks if goStats is enabled.
     * @return true if goStats is enabled, false otherwise
     */
    public static boolean isGoStatsEnabled() {
        if (!loaded) {
            loadConfig();
        }
        return goStatsEnabled;
    }

    /**
     * Sets whether goStats is enabled.
     * @param enabled true to enable goStats, false to disable
     */
    public static void setGoStatsEnabled(boolean enabled) {
        goStatsEnabled = enabled;
        saveConfig();
    }
    
    /**
     * Checks if auto-translate is enabled.
     * @return true if auto-translate is enabled, false otherwise
     */
    public static boolean isAutoTranslateEnabled() {
        if (!loaded) {
            loadConfig();
        }
        return autoTranslateEnabled;
    }
    
    /**
     * Sets whether auto-translate is enabled.
     * @param enabled true to enable auto-translate, false to disable
     */
    public static void setAutoTranslateEnabled(boolean enabled) {
        autoTranslateEnabled = enabled;
        saveConfig();
    }
    
    /**
     * Gets the API key for the specified service.
     * @param service The service name (hypixel, openai, translation)
     * @return The API key, or empty string if not found
     */
    public static String getApiKey(String service) {
        if (!loaded) {
            loadConfig();
        }
        return apiKeys.getOrDefault(service, "");
    }
    
    /**
     * Sets the API key for the specified service.
     * @param service The service name (hypixel, openai, translation)
     * @param apiKey The API key
     */
    public static void setApiKey(String service, String apiKey) {
        apiKeys.put(service, apiKey);
        saveConfig();
    }
    
    /**
     * Removes the API key for the specified service.
     * @param service The service name (hypixel, openai, translation)
     */
    public static void removeApiKey(String service) {
        apiKeys.remove(service);
        saveConfig();
    }

    /**
     * Checks if itemCall is enabled.
     * @return true if itemCall is enabled, false otherwise
     */
    public static boolean isItemCallEnabled() {
        if (!loaded) {
            loadConfig();
        }
        return itemCallEnabled;
    }

    /**
     * Sets whether itemCall is enabled.
     * @param enabled true to enable itemCall, false to disable
     */
    public static void setItemCallEnabled(boolean enabled) {
        itemCallEnabled = enabled;
        saveConfig();
    }
    
    /**
     * Gets the auto queue mode setting.
     * @return The auto queue mode (DISABLED, TEAMS, or SOLO)
     */
    public static String getAutoQueueMode() {
        if (!loaded) {
            loadConfig();
        }
        
        if (config.has(AUTO_QUEUE_MODE_KEY)) {
            return config.get(AUTO_QUEUE_MODE_KEY).getAsString();
        }
        
        return "DISABLED"; // Default value
    }
    
    /**
     * Sets the auto queue mode setting.
     * @param mode The auto queue mode (DISABLED, TEAMS, or SOLO)
     */
    public static void setAutoQueueMode(String mode) {
        if (!loaded) {
            loadConfig();
        }
        
        config.addProperty(AUTO_QUEUE_MODE_KEY, mode);
        saveConfig();
    }
} 