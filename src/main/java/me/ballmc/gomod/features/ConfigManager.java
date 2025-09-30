package me.ballmc.gomod.features;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Manages persistent configuration settings for the gomod mod.
 * Saves and loads user preferences between game sessions.
 */
public class ConfigManager {
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.weave/gomod";
    private static final String CONFIG_FILE = CONFIG_DIR + "/gomodConfig.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static JsonObject config = new JsonObject();
    private static boolean loaded = false;
    
    // Default settings
    private static boolean autoQueueEnabled = false;
    private static float autoQueueSoloDelay = 0.50f; // Solo game delay in seconds (0.0-5.0)
    private static float autoQueueTeamsDelay = 0.50f; // Teams game delay in seconds (0.0-5.0)
    private static boolean inventoryHUDEnabled = false;
    private static boolean inventoryHUDShowEmpty = true; // Show HUD even when inventory is empty
    private static int inventoryHUDCustomX = 5; // Custom X position
    private static int inventoryHUDCustomY = 5; // Custom Y position
    private static float inventoryHUDScale = 2.0f; // Scale factor (1.0-4.0, supports 1.5)
    private static int inventoryHUDBackgroundOpacity = 110; // Background opacity (0-255)
    private static int inventoryHUDBackgroundColor = 0x000000; // Background color (RGB)
    private static boolean inventoryHUDForce = false; // Force show HUD even when in GUI
    private static float perspectiveDistance = 4.0f; // Third-person camera distance in blocks
    private static boolean perspectiveEnabled = true; // Enable perspective adjustment feature
    private static float perspectiveScrollStep = 0.1f; // Mouse wheel step size in blocks
    private static boolean perspectiveResetOnExit = true; // Reset to default when leaving third-person
    private static int perspectiveResetKeyCode = 34; // Default G key (LWJGL Keyboard.KEY_G)
    private static boolean perspectiveRequireHold = true; // Require holding perspective key to scroll
    private static boolean perspectiveSmoothZoom = true; // Enable smooth zoom transitions
    private static float perspectiveSmoothSpeed = 1.00f; // Smooth transition speed (0.01-1.0)
    private static int perspectiveZoomKeyCode = 42; // Default Left Shift key (LWJGL Keyboard.KEY_LSHIFT)
    private static float perspectiveFOV = 70.0f; // Perspective FOV setting (30-110)
    private static boolean perspectiveLockScroll = false; // Lock hotbar scroll while in perspective (default disabled)
    private static boolean goStatsEnabled = false; // GoStats AI functionality
    private static boolean killCounterEnabled = true; // KillCounter main enable/disable
    private static boolean killCounterAutoKeAll = false; // Auto-run /ke all when cages open
    private static boolean killCounterShowGuildTag = false; // Show guild tags in TAB stats
    private static boolean killCounterShowTeammateKits = true; // Show teammate kits in TAB stats
    private static boolean killCounterShowEnemyKits = false; // Show enemy players' kits in TAB stats
    private static boolean killCounterShowKeAllChat = true; // Show /ke all info in chat
    // Global chat defaults
    private static boolean globalChatEnabled = false;
    private static String globalChatEndpoint = "http://localhost:8080/relay";
    private static String globalChatChannel = "global";
    private static int globalChatPollSeconds = 3;
    
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
            
            // Load auto-queue settings
            if (config.has("autoQueueEnabled")) {
                autoQueueEnabled = config.get("autoQueueEnabled").getAsBoolean();
            }
            if (config.has("autoQueueSoloDelay")) {
                autoQueueSoloDelay = config.get("autoQueueSoloDelay").getAsFloat();
            }
            if (config.has("autoQueueTeamsDelay")) {
                autoQueueTeamsDelay = config.get("autoQueueTeamsDelay").getAsFloat();
            }
            
            // Load inventory HUD settings
            if (config.has("inventoryHUDEnabled")) {
                inventoryHUDEnabled = config.get("inventoryHUDEnabled").getAsBoolean();
            }
            if (config.has("inventoryHUDShowEmpty")) {
                inventoryHUDShowEmpty = config.get("inventoryHUDShowEmpty").getAsBoolean();
            }
            if (config.has("inventoryHUDCustomX")) {
                inventoryHUDCustomX = config.get("inventoryHUDCustomX").getAsInt();
            }
            if (config.has("inventoryHUDCustomY")) {
                inventoryHUDCustomY = config.get("inventoryHUDCustomY").getAsInt();
            }
            if (config.has("inventoryHUDScale")) {
                inventoryHUDScale = config.get("inventoryHUDScale").getAsFloat();
            }
            if (config.has("inventoryHUDBackgroundOpacity")) {
                inventoryHUDBackgroundOpacity = config.get("inventoryHUDBackgroundOpacity").getAsInt();
            }
            if (config.has("inventoryHUDForce")) {
                inventoryHUDForce = config.get("inventoryHUDForce").getAsBoolean();
            }


            // Load perspective distance
            if (config.has("perspectiveDistance")) {
                perspectiveDistance = config.get("perspectiveDistance").getAsFloat();
            }

            // Load perspective feature toggles
            if (config.has("perspectiveEnabled")) {
                perspectiveEnabled = config.get("perspectiveEnabled").getAsBoolean();
            }
            if (config.has("perspectiveScrollStep")) {
                perspectiveScrollStep = config.get("perspectiveScrollStep").getAsFloat();
            }
            if (config.has("perspectiveResetOnExit")) {
                perspectiveResetOnExit = config.get("perspectiveResetOnExit").getAsBoolean();
            }
            if (config.has("perspectiveResetKeyCode")) {
                perspectiveResetKeyCode = config.get("perspectiveResetKeyCode").getAsInt();
            }
            if (config.has("perspectiveRequireHold")) {
                perspectiveRequireHold = config.get("perspectiveRequireHold").getAsBoolean();
            }
            if (config.has("perspectiveSmoothZoom")) {
                perspectiveSmoothZoom = config.get("perspectiveSmoothZoom").getAsBoolean();
            }
            if (config.has("perspectiveSmoothSpeed")) {
                perspectiveSmoothSpeed = config.get("perspectiveSmoothSpeed").getAsFloat();
            }
            if (config.has("perspectiveZoomKeyCode")) {
                perspectiveZoomKeyCode = config.get("perspectiveZoomKeyCode").getAsInt();
            }
            if (config.has("perspectiveFOV")) {
                perspectiveFOV = config.get("perspectiveFOV").getAsFloat();
            }
            if (config.has("perspectiveLockScroll")) {
                perspectiveLockScroll = config.get("perspectiveLockScroll").getAsBoolean();
            }
            
            // Load GoStats setting
            if (config.has("goStatsEnabled")) {
                goStatsEnabled = config.get("goStatsEnabled").getAsBoolean();
            }
            
            // Load KillCounter enabled setting
            if (config.has("killCounterEnabled")) {
                killCounterEnabled = config.get("killCounterEnabled").getAsBoolean();
            }
            
            // Load KillCounter auto /ke all setting
            if (config.has("killCounterAutoKeAll")) {
                killCounterAutoKeAll = config.get("killCounterAutoKeAll").getAsBoolean();
            }
            
            // Load KillCounter guild tag setting
            if (config.has("killCounterShowGuildTag")) {
                killCounterShowGuildTag = config.get("killCounterShowGuildTag").getAsBoolean();
            }
            
            // Load KillCounter teammate kit setting
            if (config.has("killCounterShowTeammateKits")) {
                killCounterShowTeammateKits = config.get("killCounterShowTeammateKits").getAsBoolean();
            }
            
            // Load KillCounter enemy kit setting
            if (config.has("killCounterShowEnemyKits")) {
                killCounterShowEnemyKits = config.get("killCounterShowEnemyKits").getAsBoolean();
            }
            
            // Load KillCounter /ke all chat setting
            if (config.has("killCounterShowKeAllChat")) {
                killCounterShowKeAllChat = config.get("killCounterShowKeAllChat").getAsBoolean();
            }

            // Load global chat settings
            if (config.has("globalChatEnabled")) {
                globalChatEnabled = config.get("globalChatEnabled").getAsBoolean();
            }
            if (config.has("globalChatEndpoint")) {
                globalChatEndpoint = config.get("globalChatEndpoint").getAsString();
            }
            if (config.has("globalChatChannel")) {
                globalChatChannel = config.get("globalChatChannel").getAsString();
            }
            if (config.has("globalChatPollSeconds")) {
                globalChatPollSeconds = Math.max(1, config.get("globalChatPollSeconds").getAsInt());
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
            
            // Save auto-queue settings
            config.addProperty("autoQueueEnabled", autoQueueEnabled);
            config.addProperty("autoQueueSoloDelay", autoQueueSoloDelay);
            config.addProperty("autoQueueTeamsDelay", autoQueueTeamsDelay);
            
            // Save inventory HUD settings
            config.addProperty("inventoryHUDEnabled", inventoryHUDEnabled);
            config.addProperty("inventoryHUDShowEmpty", inventoryHUDShowEmpty);
            config.addProperty("inventoryHUDCustomX", inventoryHUDCustomX);
            config.addProperty("inventoryHUDCustomY", inventoryHUDCustomY);
            config.addProperty("inventoryHUDScale", inventoryHUDScale);
            config.addProperty("inventoryHUDBackgroundOpacity", inventoryHUDBackgroundOpacity);
            config.addProperty("inventoryHUDBackgroundColor", inventoryHUDBackgroundColor);
            config.addProperty("inventoryHUDForce", inventoryHUDForce);
            config.addProperty("perspectiveDistance", perspectiveDistance);
            config.addProperty("perspectiveEnabled", perspectiveEnabled);
            config.addProperty("perspectiveScrollStep", perspectiveScrollStep);
            config.addProperty("perspectiveResetOnExit", perspectiveResetOnExit);
            config.addProperty("perspectiveResetKeyCode", perspectiveResetKeyCode);
            config.addProperty("perspectiveRequireHold", perspectiveRequireHold);
            config.addProperty("perspectiveSmoothZoom", perspectiveSmoothZoom);
            config.addProperty("perspectiveSmoothSpeed", perspectiveSmoothSpeed);
            config.addProperty("perspectiveZoomKeyCode", perspectiveZoomKeyCode);
            config.addProperty("perspectiveFOV", perspectiveFOV);
            config.addProperty("perspectiveLockScroll", perspectiveLockScroll);
            config.addProperty("goStatsEnabled", goStatsEnabled);
            config.addProperty("killCounterEnabled", killCounterEnabled);
            config.addProperty("killCounterAutoKeAll", killCounterAutoKeAll);
            config.addProperty("killCounterShowGuildTag", killCounterShowGuildTag);
        config.addProperty("killCounterShowTeammateKits", killCounterShowTeammateKits);
        config.addProperty("killCounterShowEnemyKits", killCounterShowEnemyKits);
        config.addProperty("killCounterShowKeAllChat", killCounterShowKeAllChat);
            // Save global chat settings
            config.addProperty("globalChatEnabled", globalChatEnabled);
            config.addProperty("globalChatEndpoint", globalChatEndpoint);
            config.addProperty("globalChatChannel", globalChatChannel);
            config.addProperty("globalChatPollSeconds", globalChatPollSeconds);
            
            // Write to file with pretty printing
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(config, writer);
            }
            
            System.out.println("[ConfigManager] Saved configuration to " + CONFIG_FILE);
            loaded = true;
        } catch (IOException e) {
            System.out.println("[ConfigManager] Error saving config: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Gets the desired third-person perspective distance.
     */
    public static float getPerspectiveDistance() {
        if (!loaded) {
            loadConfig();
        }
        return perspectiveDistance;
    }

    /**
     * Sets and persists the desired third-person perspective distance.
     */
    public static void setPerspectiveDistance(float distance) {
        perspectiveDistance = distance;
        saveConfig();
    }

    public static boolean isPerspectiveEnabled() {
        if (!loaded) {
            loadConfig();
        }
        return perspectiveEnabled;
    }

    public static void setPerspectiveEnabled(boolean enabled) {
        perspectiveEnabled = enabled;
        saveConfig();
    }

    public static float getPerspectiveScrollStep() {
        if (!loaded) {
            loadConfig();
        }
        return perspectiveScrollStep;
    }

    public static void setPerspectiveScrollStep(float step) {
        perspectiveScrollStep = step;
        saveConfig();
    }

    public static boolean isPerspectiveResetOnExit() {
        if (!loaded) {
            loadConfig();
        }
        return perspectiveResetOnExit;
    }

    public static void setPerspectiveResetOnExit(boolean value) {
        perspectiveResetOnExit = value;
        saveConfig();
    }

    public static int getPerspectiveResetKeyCode() {
        if (!loaded) {
            loadConfig();
        }
        return perspectiveResetKeyCode;
    }

    public static void setPerspectiveResetKeyCode(int keyCode) {
        perspectiveResetKeyCode = keyCode;
        saveConfig();
    }

    public static boolean isPerspectiveRequireHold() {
        if (!loaded) {
            loadConfig();
        }
        return perspectiveRequireHold;
    }

    public static void setPerspectiveRequireHold(boolean value) {
        perspectiveRequireHold = value;
        saveConfig();
    }

    public static boolean isPerspectiveSmoothZoom() {
        if (!loaded) {
            loadConfig();
        }
        return perspectiveSmoothZoom;
    }

    public static void setPerspectiveSmoothZoom(boolean value) {
        perspectiveSmoothZoom = value;
        saveConfig();
    }

    public static float getPerspectiveSmoothSpeed() {
        if (!loaded) {
            loadConfig();
        }
        return perspectiveSmoothSpeed;
    }

    public static void setPerspectiveSmoothSpeed(float speed) {
        perspectiveSmoothSpeed = Math.max(0.01f, Math.min(1.0f, speed));
        saveConfig();
    }

    public static int getPerspectiveZoomKeyCode() {
        if (!loaded) {
            loadConfig();
        }
        return perspectiveZoomKeyCode;
    }

    public static void setPerspectiveZoomKeyCode(int keyCode) {
        perspectiveZoomKeyCode = keyCode;
        saveConfig();
    }

    public static float getPerspectiveFOV() {
        if (!loaded) {
            loadConfig();
        }
        return perspectiveFOV;
    }

    public static void setPerspectiveFOV(float fov) {
        perspectiveFOV = Math.max(30.0f, Math.min(110.0f, fov));
        saveConfig();
    }

    // Lock scroll while in perspective
    public static boolean isPerspectiveLockScroll() {
        if (!loaded) {
            loadConfig();
        }
        return perspectiveLockScroll;
    }

    public static void setPerspectiveLockScroll(boolean value) {
        perspectiveLockScroll = value;
        saveConfig();
    }
    
    /**
     * Checks if auto-queue is enabled.
     * @return true if auto-queue is enabled, false otherwise
     */
    public static boolean isAutoQueueEnabled() {
        if (!loaded) {
            loadConfig();
        }
        return autoQueueEnabled;
    }

    /**
     * Sets whether auto-queue is enabled.
     * @param enabled true to enable auto-queue, false to disable
     */
    public static void setAutoQueueEnabled(boolean enabled) {
        autoQueueEnabled = enabled;
        saveConfig();
    }
    
    /**
     * Gets the auto-queue delay for solo games.
     * @return delay in seconds
     */
    public static float getAutoQueueSoloDelay() {
        if (!loaded) {
            loadConfig();
        }
        return autoQueueSoloDelay;
    }
    
    /**
     * Sets the auto-queue delay for solo games.
     * @param delay delay in seconds
     */
    public static void setAutoQueueSoloDelay(float delay) {
        autoQueueSoloDelay = delay;
        saveConfig();
    }
    
    /**
     * Gets the auto-queue delay for teams games.
     * @return delay in seconds
     */
    public static float getAutoQueueTeamsDelay() {
        if (!loaded) {
            loadConfig();
        }
        return autoQueueTeamsDelay;
    }
    
    /**
     * Sets the auto-queue delay for teams games.
     * @param delay delay in seconds
     */
    public static void setAutoQueueTeamsDelay(float delay) {
        autoQueueTeamsDelay = delay;
        saveConfig();
    }
    
    /**
     * Checks if inventory HUD is enabled.
     * @return true if inventory HUD is enabled, false otherwise
     */
    public static boolean isInventoryHUDEnabled() {
        if (!loaded) {
            loadConfig();
        }
        return inventoryHUDEnabled;
    }

    /**
     * Sets whether inventory HUD is enabled.
     * @param enabled true to enable inventory HUD, false to disable
     */
    public static void setInventoryHUDEnabled(boolean enabled) {
        inventoryHUDEnabled = enabled;
        saveConfig();
    }
    
    /**
     * Checks if inventory HUD should show when empty.
     * @return true if inventory HUD should show when empty, false otherwise
     */
    public static boolean isInventoryHUDShowEmpty() {
        if (!loaded) {
            loadConfig();
        }
        return inventoryHUDShowEmpty;
    }

    /**
     * Sets whether inventory HUD should show when empty.
     * @param showEmpty true to show HUD when empty, false to hide when empty
     */
    public static void setInventoryHUDShowEmpty(boolean showEmpty) {
        inventoryHUDShowEmpty = showEmpty;
        saveConfig();
    }
    

    
    /**
     * Gets the inventory HUD custom X position.
     * @return custom X position
     */
    public static int getInventoryHUDCustomX() {
        if (!loaded) {
            loadConfig();
        }
        return inventoryHUDCustomX;
    }

    /**
     * Sets the inventory HUD custom X position.
     * @param x custom X position
     */
    public static void setInventoryHUDCustomX(int x) {
        inventoryHUDCustomX = x;
        saveConfig();
    }
    
    /**
     * Gets the inventory HUD custom Y position.
     * @return custom Y position
     */
    public static int getInventoryHUDCustomY() {
        if (!loaded) {
            loadConfig();
        }
        return inventoryHUDCustomY;
    }

    /**
     * Sets the inventory HUD custom Y position.
     * @param y custom Y position
     */
    public static void setInventoryHUDCustomY(int y) {
        inventoryHUDCustomY = y;
        saveConfig();
    }
    
    /**
     * Gets the inventory HUD scale.
     * @return scale factor (1.0-4.0, supports 1.5)
     */
    public static float getInventoryHUDScale() {
        if (!loaded) {
            loadConfig();
        }
        return inventoryHUDScale;
    }

    /**
     * Sets the inventory HUD scale.
     * @param scale scale factor (1.0-4.0, supports 1.5)
     */
    public static void setInventoryHUDScale(float scale) {
        inventoryHUDScale = scale;
        saveConfig();
    }
    
    /**
     * Gets the inventory HUD background opacity.
     * @return opacity value (0-255)
     */
    public static int getInventoryHUDBackgroundOpacity() {
        if (!loaded) {
            loadConfig();
        }
        return inventoryHUDBackgroundOpacity;
    }

    /**
     * Sets the inventory HUD background opacity.
     * @param opacity opacity value (0-255)
     */
    public static void setInventoryHUDBackgroundOpacity(int opacity) {
        inventoryHUDBackgroundOpacity = opacity;
        saveConfig();
    }
    
    /**
     * Gets the inventory HUD background color.
     * @return background color (RGB)
     */
    public static int getInventoryHUDBackgroundColor() {
        if (!loaded) {
            loadConfig();
        }
        return inventoryHUDBackgroundColor;
    }

    /**
     * Sets the inventory HUD background color.
     * @param color background color (RGB)
     */
    public static void setInventoryHUDBackgroundColor(int color) {
        inventoryHUDBackgroundColor = color;
        saveConfig();
    }
    
    /**
     * Checks if inventory HUD force mode is enabled.
     * @return true if force mode is enabled, false otherwise
     */
    public static boolean isInventoryHUDForce() {
        if (!loaded) {
            loadConfig();
        }
        return inventoryHUDForce;
    }

    /**
     * Sets whether inventory HUD force mode is enabled.
     * @param force true to enable force mode, false to disable
     */
    public static void setInventoryHUDForce(boolean force) {
        inventoryHUDForce = force;
        saveConfig();
    }
    
    /**
     * Checks if GoStats AI functionality is enabled.
     * @return true if GoStats is enabled, false otherwise
     */
    public static boolean isGoStatsEnabled() {
        if (!loaded) {
            loadConfig();
        }
        return goStatsEnabled;
    }

    /**
     * Sets whether GoStats AI functionality is enabled.
     * @param enabled true to enable GoStats, false to disable
     */
    public static void setGoStatsEnabled(boolean enabled) {
        goStatsEnabled = enabled;
        saveConfig();
    }
    
    /**
     * Checks if KillCounter is enabled.
     * @return true if KillCounter is enabled, false otherwise
     */
    public static boolean isKillCounterEnabled() {
        if (!loaded) {
            loadConfig();
        }
        return killCounterEnabled;
    }
    
    /**
     * Sets whether KillCounter is enabled.
     * @param enabled true to enable KillCounter, false to disable
     */
    public static void setKillCounterEnabled(boolean enabled) {
        killCounterEnabled = enabled;
        saveConfig();
    }
    
    /**
     * Checks if auto /ke all when cages open is enabled.
     * @return true if auto /ke all is enabled, false otherwise
     */
    public static boolean isKillCounterAutoKeAll() {
        if (!loaded) {
            loadConfig();
        }
        return killCounterAutoKeAll;
    }
    
    /**
     * Sets whether auto /ke all when cages open is enabled.
     * @param enabled true to enable auto /ke all, false to disable
     */
    public static void setKillCounterAutoKeAll(boolean enabled) {
        killCounterAutoKeAll = enabled;
        saveConfig();
    }
    
    /**
     * Checks if guild tag display is enabled.
     * @return true if guild tag display is enabled, false otherwise
     */
    public static boolean isKillCounterShowGuildTag() {
        if (!loaded) {
            loadConfig();
        }
        return killCounterShowGuildTag;
    }
    
    /**
     * Sets whether guild tag display is enabled.
     * @param enabled true to enable guild tag display, false to disable
     */
    public static void setKillCounterShowGuildTag(boolean enabled) {
        killCounterShowGuildTag = enabled;
        saveConfig();
    }
    
    /**
     * Checks if teammate kit display is enabled.
     * @return true if teammate kit display is enabled, false otherwise
     */
    public static boolean isKillCounterShowTeammateKits() {
        if (!loaded) {
            loadConfig();
        }
        return killCounterShowTeammateKits;
    }
    
    /**
     * Sets whether teammate kit display is enabled.
     * @param enabled true to enable teammate kit display, false to disable
     */
    public static void setKillCounterShowTeammateKits(boolean enabled) {
        killCounterShowTeammateKits = enabled;
        saveConfig();
    }
    
    /**
     * Checks if enemy kit display is enabled.
     * @return true if enemy kit display is enabled, false otherwise
     */
    public static boolean isKillCounterShowEnemyKits() {
        if (!loaded) {
            loadConfig();
        }
        return killCounterShowEnemyKits;
    }
    
    /**
     * Sets whether enemy kit display is enabled.
     * @param enabled true to enable enemy kit display, false to disable
     */
    public static void setKillCounterShowEnemyKits(boolean enabled) {
        killCounterShowEnemyKits = enabled;
        saveConfig();
    }
    
    /**
     * Checks if /ke all chat display is enabled.
     * @return true if /ke all chat display is enabled, false otherwise
     */
    public static boolean isKillCounterShowKeAllChat() {
        if (!loaded) {
            loadConfig();
        }
        return killCounterShowKeAllChat;
    }
    
    /**
     * Sets whether /ke all chat display is enabled.
     * @param enabled true to enable /ke all chat display, false to disable
     */
    public static void setKillCounterShowKeAllChat(boolean enabled) {
        killCounterShowKeAllChat = enabled;
        saveConfig();
    }


    // -------------------- Global Chat Settings --------------------
    public static boolean isGlobalChatEnabled() {
        if (!loaded) {
            loadConfig();
        }
        return globalChatEnabled;
    }

    public static void setGlobalChatEnabled(boolean enabled) {
        globalChatEnabled = enabled;
        saveConfig();
    }

    public static String getGlobalChatEndpoint() {
        if (!loaded) {
            loadConfig();
        }
        return globalChatEndpoint;
    }

    public static void setGlobalChatEndpoint(String endpoint) {
        if (endpoint == null || endpoint.trim().isEmpty()) return;
        globalChatEndpoint = endpoint.trim();
        saveConfig();
    }

    public static String getGlobalChatChannel() {
        if (!loaded) {
            loadConfig();
        }
        return globalChatChannel;
    }

    public static void setGlobalChatChannel(String channel) {
        if (channel == null || channel.trim().isEmpty()) return;
        globalChatChannel = channel.trim();
        saveConfig();
    }

    public static int getGlobalChatPollSeconds() {
        if (!loaded) {
            loadConfig();
        }
        return Math.max(1, globalChatPollSeconds);
    }

    public static void setGlobalChatPollSeconds(int seconds) {
        globalChatPollSeconds = Math.max(1, seconds);
        saveConfig();
    }
} 