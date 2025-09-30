package me.ballmc.AntiShuffle.features;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages API keys for various services used in the mod.
 */
public class ApiKeyManager {
    private static final Map<String, String> apiKeys = new HashMap<>();
    private static boolean initialized = false;
    
    /**
     * Initializes the API keys from ConfigManager
     */
    private static void initialize() {
        if (initialized) {
            return;
        }
        
        System.out.println("[ApiKeyManager] Initializing API keys from config");
        
        // Load keys from ConfigManager for each supported service
        String[] services = new String[]{"hypixel", "openai", "translation"};
        
        for (String service : services) {
            String savedKey = ConfigManager.getApiKey(service);
            
            if (savedKey != null && !savedKey.isEmpty() && !savedKey.equals("default-key")) {
                apiKeys.put(service, savedKey);
                // Mask the key for privacy in logs
                String maskedKey = savedKey.length() > 8 
                    ? savedKey.substring(0, 4) + "****" + savedKey.substring(savedKey.length() - 4)
                    : "****";
                System.out.println("[ApiKeyManager] Loaded " + service + " API key from config: " + maskedKey);
            } else {
                // Don't set a default key, leave it as not configured
                System.out.println("[ApiKeyManager] No " + service + " API key found in config");
                
                // Log warning that API keys are needed
                if (service.equals("openai") || service.equals("translation")) {
                    System.out.println("[ApiKeyManager] NOTE: " + service + " requires a valid API key to function!");
                }
            }
        }
        
        initialized = true;
        
        // Print a summary of API key status
        logApiKeyStatus();
    }
    
    /**
     * Logs the status of all API keys for debugging
     */
    private static void logApiKeyStatus() {
        System.out.println("[ApiKeyManager] API Key Status:");
        String[] services = new String[]{"hypixel", "openai", "translation"};
        
        for (String service : services) {
            boolean hasKey = apiKeys.containsKey(service) && !apiKeys.get(service).isEmpty();
            
            if (hasKey) {
                System.out.println("  - " + service + ": CONFIGURED");
            } else {
                System.out.println("  - " + service + ": NOT CONFIGURED");
            }
        }
    }
    
    /**
     * Gets the API key for the specified service
     * @param service The service name (hypixel, openai, translation)
     * @return The API key, or empty string if not set
     */
    public static String getApiKey(String service) {
        if (!initialized) {
            initialize();
        }
        
        return apiKeys.getOrDefault(service, "");
    }
    
    /**
     * Sets the API key for the specified service
     * @param service The service name (hypixel, openai, translation)
     * @param apiKey The API key
     */
    public static void setApiKey(String service, String apiKey) {
        if (!initialized) {
            initialize();
        }
        
        // Trim any whitespace
        apiKey = apiKey.trim();
        
        if (apiKey.isEmpty()) {
            // If empty, remove the key entirely
            apiKeys.remove(service);
            ConfigManager.removeApiKey(service);
            System.out.println("[ApiKeyManager] Removed " + service + " API key");
        } else {
            // Otherwise, set the key
            apiKeys.put(service, apiKey);
            
            // Save to config for persistence
            ConfigManager.setApiKey(service, apiKey);
            
            // Mask the key for privacy in logs
            String maskedKey = apiKey.length() > 8 
                ? apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4)
                : "****";
            
            System.out.println("[ApiKeyManager] Updated " + service + " API key and saved to config: " + maskedKey);
        }
    }
    
    /**
     * Resets all API keys by removing them entirely
     */
    public static void resetApiKeys() {
        if (!initialized) {
            initialize();
        }
        
        apiKeys.clear();
        
        // Remove all keys from config
        String[] services = new String[]{"hypixel", "openai", "translation"};
        for (String service : services) {
            ConfigManager.removeApiKey(service);
        }
        
        System.out.println("[ApiKeyManager] Reset all API keys - all services now require configuration");
    }
    
    /**
     * Checks if the specified service has a valid API key
     * @param service The service name (hypixel, openai, translation)
     * @return True if the service has a valid API key, false otherwise
     */
    public static boolean hasValidApiKey(String service) {
        if (!initialized) {
            initialize();
        }
        
        return apiKeys.containsKey(service) && !apiKeys.get(service).isEmpty();
    }
} 