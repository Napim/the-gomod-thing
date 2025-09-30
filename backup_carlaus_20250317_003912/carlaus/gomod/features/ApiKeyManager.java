package carlaus.gomod.features;

/**
 * Manages API keys for various services used by the mod.
 */
public class ApiKeyManager {
    
    private static final String OPENAI_API_KEY = "openai";
    private static final String HYPIXEL_API_KEY = "hypixel";
    
    /**
     * Loads API keys from configuration
     */
    public static void loadApiKey() {
        // API keys are loaded through ConfigManager
        System.out.println("API key configuration loaded");
    }
    
    /**
     * Sets an API key for a specific service
     * @param service the service name
     * @param apiKey the API key to set
     */
    public static void setApiKey(String service, String apiKey) {
        ConfigManager.setApiKey(service, apiKey);
    }
    
    /**
     * Gets the API key for a specific service
     * @param service the service name
     * @return the API key, or null if not set
     */
    public static String getApiKey(String service) {
        return ConfigManager.getApiKey(service);
    }
    
    /**
     * Gets the OpenAI API key
     * @return the OpenAI API key, or null if not set
     */
    public static String getOpenAIApiKey() {
        return getApiKey(OPENAI_API_KEY);
    }
    
    /**
     * Sets the OpenAI API key
     * @param apiKey the API key to set
     */
    public static void setOpenAIApiKey(String apiKey) {
        setApiKey(OPENAI_API_KEY, apiKey);
    }
    
    /**
     * Gets the Hypixel API key
     * @return the Hypixel API key, or null if not set
     */
    public static String getHypixelApiKey() {
        return getApiKey(HYPIXEL_API_KEY);
    }
    
    /**
     * Sets the Hypixel API key
     * @param apiKey the API key to set
     */
    public static void setHypixelApiKey(String apiKey) {
        setApiKey(HYPIXEL_API_KEY, apiKey);
    }
} 