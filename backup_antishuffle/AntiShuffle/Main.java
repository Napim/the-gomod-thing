package me.ballmc.AntiShuffle;

import net.weavemc.loader.api.ModInitializer;
import net.weavemc.loader.api.command.CommandBus;
import net.weavemc.loader.api.event.*;
import me.ballmc.AntiShuffle.command.KillEffectCommand;
import me.ballmc.AntiShuffle.command.KillEffectCommand2;
import me.ballmc.AntiShuffle.command.KillCountCommand;
import me.ballmc.AntiShuffle.command.BlitzStartCommand;
import me.ballmc.AntiShuffle.command.TeamSpamCommand;
import me.ballmc.AntiShuffle.command.LanguageCommand;
import me.ballmc.AntiShuffle.command.TranslateCommand;
import me.ballmc.AntiShuffle.command.AutoTranslateCommand;
import me.ballmc.AntiShuffle.command.QueueStatusCommand;
import me.ballmc.AntiShuffle.command.PlayerInfoCommand;
import me.ballmc.AntiShuffle.command.ApiKeyCommand;
import me.ballmc.AntiShuffle.command.GMHelpCommand;
import me.ballmc.AntiShuffle.command.GoStatsToggleCommand;
import me.ballmc.AntiShuffle.command.AntiShuffleToggle;
import me.ballmc.AntiShuffle.command.GMGuiCommand;
import me.ballmc.AntiShuffle.command.PlayerLookCommand;
import me.ballmc.AntiShuffle.command.OpacityCommand;
import me.ballmc.AntiShuffle.command.FriendSpamCommand;
import me.ballmc.AntiShuffle.command.ItemCallToggleCommand;
import me.ballmc.AntiShuffle.command.PartySpamCommand;
import me.ballmc.AntiShuffle.command.AutoQueueCommand;
import me.ballmc.AntiShuffle.features.KillCounter;
import me.ballmc.AntiShuffle.features.AIChatHandler;
import me.ballmc.AntiShuffle.features.ChatTranslator;
import me.ballmc.AntiShuffle.features.ApiKeyManager;
import me.ballmc.AntiShuffle.features.TeamSpamManager;
import me.ballmc.AntiShuffle.features.ConfigManager;
import me.ballmc.AntiShuffle.features.PlayerLooker;
import me.ballmc.AntiShuffle.features.TeamInviteResponseHandler;
import me.ballmc.AntiShuffle.features.AutoQueue;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import java.util.regex.Pattern;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Main implements ModInitializer {
    // Whether anti-shuffle is enabled
    public static boolean enabled = true;
    
    // Key for kill effect
    private static final Pattern KILL_EFFECT_REGEX = Pattern.compile("Your Kill Effect is now ([\\w\\s]+)\\.");
    
    // Pattern for obfuscated text
    private static final Pattern shufflePattern = Pattern.compile("\u00a7k");
    
    // Pattern for "XXXX" prefix before names (common in games)
    private static final Pattern xxxxPattern = Pattern.compile("XXXX");
    
    // Anti-shuffle kill counter
    private final KillCounter killCounter = new KillCounter();
    
    // Player Looker feature
    private final PlayerLooker playerLooker = new PlayerLooker();
    
    // Team invite response handler
    private final TeamInviteResponseHandler teamInviteResponseHandler = new TeamInviteResponseHandler();

    // Auto-queue handler
    private final AutoQueue autoQueue = new AutoQueue();

    @Override
    public void preInit() {
        System.out.println("Initializing gomod!");
        
        // Initialize configurations
        ConfigManager.loadConfig();
        
        // Initialize settings
        enabled = ConfigManager.isAntishuffleEnabled();
        
        // Load auto-queue mode
        AutoQueue.loadMode();
        
        // Log initial settings
        System.out.println("Initial settings - AntiShuffle: " + (enabled ? "enabled" : "disabled"));
        
        // Register commands
        KillEffectCommand killEffectCommand = new KillEffectCommand();
        CommandBus.register(killEffectCommand);
        CommandBus.register(new KillEffectCommand2());
        CommandBus.register(new KillCountCommand(killCounter));
        CommandBus.register(new OpacityCommand());
        CommandBus.register(new BlitzStartCommand(killCounter));
        CommandBus.register(new GMHelpCommand());
        CommandBus.register(new PlayerLookCommand(playerLooker));  // Register PlayerLookCommand
        CommandBus.register(new QueueStatusCommand());  // Register QueueStatusCommand for /qstatus
        CommandBus.register(new TeamSpamCommand());  // Register TeamSpamCommand for /tspam
        CommandBus.register(new PartySpamCommand());  // Register PartySpamCommand for /pspam
        CommandBus.register(new AntiShuffleToggle());
        CommandBus.register(new ApiKeyCommand());
        CommandBus.register(new AutoTranslateCommand());
        CommandBus.register(new FriendSpamCommand());
        CommandBus.register(new GMGuiCommand());
        CommandBus.register(new GoStatsToggleCommand());
        CommandBus.register(new ItemCallToggleCommand());
        CommandBus.register(new LanguageCommand());
        CommandBus.register(new PlayerInfoCommand());
        CommandBus.register(new TranslateCommand());
        CommandBus.register(new AutoQueueCommand());  // Register AutoQueueCommand for /autoq
        
        // Register event subscribers
        EventBus.subscribe(killEffectCommand);
        EventBus.subscribe(killCounter);
        EventBus.subscribe(playerLooker);  // Register PlayerLooker event subscriber
        EventBus.subscribe(teamInviteResponseHandler);  // Register TeamInviteResponseHandler
        EventBus.subscribe(autoQueue);  // Register AutoQueue event subscriber
        
        // Initialize and register AIChatHandler
        AIChatHandler aiChatHandler = new AIChatHandler();
        EventBus.subscribe(aiChatHandler);
        
        // Initialize ChatTranslator
        ChatTranslator chatTranslator = new ChatTranslator();
        
        EventBus.subscribe(this);
    }
    
    /**
     * Migrates old configuration files to the new location and names.
     * This ensures that users don't lose their settings when upgrading.
     */
    private void migrateConfigFiles() {
        // Define old and new paths
        String configDir = System.getProperty("user.home") + "/.weave/gomod123";
        String oldConfigFile = "antishuffle_config.json";
        String newConfigFile = configDir + "/gomod123Config.json";
        String oldPlayersFile = "antishuffle_players.json";
        String newPlayersFile = configDir + "/tspam_list.json";
        
        // Ensure the directory exists
        File configDirFile = new File(configDir);
        if (!configDirFile.exists()) {
            boolean dirCreated = configDirFile.mkdirs();
            if (dirCreated) {
                System.out.println("[Main] Created config directory: " + configDir);
            } else {
                System.out.println("[Main] Failed to create config directory: " + configDir);
                return; // Can't proceed with migration if we can't create the directory
            }
        }
        
        // Attempt to migrate config file
        File oldConfig = new File(oldConfigFile);
        File newConfig = new File(newConfigFile);
        
        if (oldConfig.exists() && !newConfig.exists()) {
            try {
                Files.copy(oldConfig.toPath(), newConfig.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[Main] Successfully migrated configuration from " + oldConfigFile + " to " + newConfigFile);
            } catch (IOException e) {
                System.out.println("[Main] Failed to migrate " + oldConfigFile + ": " + e.getMessage());
            }
        }
        
        // Attempt to migrate players file
        File oldPlayers = new File(oldPlayersFile);
        File newPlayers = new File(newPlayersFile);
        
        if (oldPlayers.exists() && !newPlayers.exists()) {
            try {
                Files.copy(oldPlayers.toPath(), newPlayers.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[Main] Successfully migrated players from " + oldPlayersFile + " to " + newPlayersFile);
            } catch (IOException e) {
                System.out.println("[Main] Failed to migrate " + oldPlayersFile + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Sets whether anti-shuffle is enabled and updates the config
     */
    public static void setEnabled(boolean value) {
        enabled = value;
        ConfigManager.setAntishuffleEnabled(value);
        
        // Notify player of state change
        String status = enabled ? EnumChatFormatting.GREEN + "enabled" : EnumChatFormatting.RED + "disabled";
        sendMessage(EnumChatFormatting.YELLOW + "AntiShuffle " + status);
    }
    
    /**
     * Helper method to send a chat message to the player
     */
    public static void sendMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
    
    /**
     * Removes obfuscated text (§k) from a chat message
     * 
     * @param shuffleText The text that might contain obfuscated sections
     * @return The text with obfuscated sections removed
     */
    public static String getUnformattedTextForChat(String shuffleText) {
        if (shuffleText == null || !enabled) return shuffleText;
        
        try {
            // First remove the standard obfuscation character
            String result = shufflePattern.matcher(shuffleText).replaceAll("");
            
            // Then remove any "XXXX" prefix that might be added before player names in-game
            result = xxxxPattern.matcher(result).replaceAll("");
            
            return result;
        } catch (Exception e) {
            // Log the error but don't allow it to propagate
            System.err.println("Error in getUnformattedTextForChat: " + e.getMessage());
            return shuffleText;
        }
    }
}