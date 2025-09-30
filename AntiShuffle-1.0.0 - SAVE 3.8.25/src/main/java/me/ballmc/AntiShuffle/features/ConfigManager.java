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
    private static final String CONFIG_FILE = "antishuffle_config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static JsonObject config = new JsonObject();
    private static boolean loaded = false;
    
    // Default settings
    private static boolean antiShuffleEnabled = true;
    private static boolean goStatsEnabled = true;
    private static boolean autoTranslateEnabled = false;
    private static Map<String, String> apiKeys = new HashMap<>();
    
    /**
     * Loads the configuration from the config file.
     * If the file doesn't exist, default values are used.
     */
    public static void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            saveConfig(); // Create default config file
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            JsonParser parser = new JsonParser();
            JsonObject config = parser.parse(reader).getAsJsonObject();
            
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
            if (config.has("apiKeys") && config.get("apiKeys").isJsonObject()) {
                JsonObject apiKeysObject = config.getAsJsonObject("apiKeys");
                apiKeys.clear();
                
                Set<Map.Entry<String, JsonElement>> entries = apiKeysObject.entrySet();
                for (Map.Entry<String, JsonElement> entry : entries) {
                    apiKeys.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            
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
} 