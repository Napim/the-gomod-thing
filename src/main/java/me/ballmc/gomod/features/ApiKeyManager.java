package me.ballmc.gomod.features;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Manages API keys for various services used by the mod.
 * Currently supports OpenAI API keys for the @gostats123 AI functionality.
 */
public class ApiKeyManager {
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.weave/gomod123";
    private static final String API_KEYS_FILE = CONFIG_DIR + "/api_keys.json";
    private static JsonObject apiKeys = new JsonObject();
    private static boolean loaded = false;
    
    /**
     * Loads API keys from the configuration file.
     * Creates the file with default values if it doesn't exist.
     */
    public static void loadApiKeys() {
        if (loaded) return;
        
        try {
            // Ensure config directory exists
            File configDir = new File(CONFIG_DIR);
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            
            // Load existing API keys or create new file
            File apiKeysFile = new File(API_KEYS_FILE);
            if (apiKeysFile.exists()) {
                try (FileReader reader = new FileReader(apiKeysFile)) {
                    JsonParser parser = new JsonParser();
                    apiKeys = parser.parse(reader).getAsJsonObject();
                    System.out.println("[ApiKeyManager] Loaded API keys from " + API_KEYS_FILE);
                }
            } else {
                // Create default API keys file
                apiKeys = new JsonObject();
                apiKeys.addProperty("openai", "");
                apiKeys.addProperty("hypixel", "");
                saveApiKeys();
                System.out.println("[ApiKeyManager] Created new API keys file at " + API_KEYS_FILE);
            }
            
            loaded = true;
        } catch (Exception e) {
            System.err.println("[ApiKeyManager] Error loading API keys: " + e.getMessage());
            e.printStackTrace();
            // Initialize with empty keys on error
            apiKeys = new JsonObject();
            loaded = true;
        }
    }
    
    /**
     * Saves API keys to the configuration file.
     */
    public static void saveApiKeys() {
        try {
            // Ensure config directory exists
            File configDir = new File(CONFIG_DIR);
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            
            // Write API keys to file
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(API_KEYS_FILE)) {
                gson.toJson(apiKeys, writer);
                System.out.println("[ApiKeyManager] Saved API keys to " + API_KEYS_FILE);
            }
        } catch (IOException e) {
            System.err.println("[ApiKeyManager] Error saving API keys: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Gets an API key for a specific service.
     * 
     * @param service The service name (e.g., "openai")
     * @return The API key, or null if not found
     */
    public static String getApiKey(String service) {
        if (!loaded) {
            loadApiKeys();
        }
        
        if (apiKeys.has(service)) {
            String key = apiKeys.get(service).getAsString();
            return key.isEmpty() ? null : key;
        }
        
        return null;
    }
    
    /**
     * Sets an API key for a specific service.
     * 
     * @param service The service name (e.g., "openai")
     * @param apiKey The API key to set
     */
    public static void setApiKey(String service, String apiKey) {
        if (!loaded) {
            loadApiKeys();
        }
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKeys.addProperty(service, "");
        } else {
            apiKeys.addProperty(service, apiKey.trim());
        }
        
        saveApiKeys();
        System.out.println("[ApiKeyManager] Set API key for " + service + ": " + 
                          (apiKey == null || apiKey.trim().isEmpty() ? "cleared" : "updated"));
    }
    
    /**
     * Checks if an API key is set for a specific service.
     * 
     * @param service The service name (e.g., "openai")
     * @return True if a non-empty API key is set, false otherwise
     */
    public static boolean hasApiKey(String service) {
        String key = getApiKey(service);
        return key != null && !key.trim().isEmpty();
    }
    
    /**
     * Gets a masked version of an API key for display purposes.
     * Shows first 4 and last 4 characters with asterisks in between.
     * 
     * @param service The service name
     * @return Masked API key, or "Not set" if no key is configured
     */
    public static String getMaskedApiKey(String service) {
        String key = getApiKey(service);
        if (key == null || key.trim().isEmpty()) {
            return "Not set";
        }
        
        if (key.length() <= 8) {
            return "****";
        }
        
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
    
    /**
     * Lists all configured API keys (masked) for display purposes.
     * 
     * @return A formatted string showing all API key statuses
     */
    public static String listApiKeys() {
        if (!loaded) {
            loadApiKeys();
        }
        
        StringBuilder result = new StringBuilder();
        result.append("API Keys Status:\n");
        
        // Check for OpenAI key
        result.append("OpenAI: ").append(getMaskedApiKey("openai")).append("\n");
        
        // Check for Hypixel key
        result.append("Hypixel: ").append(getMaskedApiKey("hypixel"));
        
        return result.toString();
    }
}
