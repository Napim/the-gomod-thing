package me.ballmc.AntiShuffle;

import net.weavemc.loader.api.ModInitializer;
import net.weavemc.loader.api.command.CommandBus;
import net.weavemc.loader.api.event.*;
import me.ballmc.AntiShuffle.command.AntiShuffleCommand;
import me.ballmc.AntiShuffle.command.KillEffectCommand;
import me.ballmc.AntiShuffle.command.KillEffectCommand2;
import me.ballmc.AntiShuffle.command.KillCountCommand;
import me.ballmc.AntiShuffle.command.BlitzStartCommand;
import me.ballmc.AntiShuffle.command.TeamSpamCommands;
import me.ballmc.AntiShuffle.command.LanguageCommand;
import me.ballmc.AntiShuffle.command.TranslateCommand;
import me.ballmc.AntiShuffle.command.AutoTranslateCommand;
import me.ballmc.AntiShuffle.command.QueueStatusCommand;
import me.ballmc.AntiShuffle.command.PlayerInfoCommand;
import me.ballmc.AntiShuffle.command.ApiKeyCommand;
import me.ballmc.AntiShuffle.command.GMHelpCommand;
import me.ballmc.AntiShuffle.command.GoStatsToggleCommand;
import me.ballmc.AntiShuffle.features.KillCounter;
import me.ballmc.AntiShuffle.features.AIChatHandler;
import me.ballmc.AntiShuffle.features.ChatTranslator;
import me.ballmc.AntiShuffle.features.ApiKeyManager;
import me.ballmc.AntiShuffle.features.TeamSpamManager;
import me.ballmc.AntiShuffle.features.ConfigManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import java.util.regex.Pattern;

public class Main implements ModInitializer {
    // Whether anti-shuffle is enabled
    public static boolean enabled = true;
    
    // Key for kill effect
    private static final Pattern KILL_EFFECT_REGEX = Pattern.compile("Your Kill Effect is now ([\\w\\s]+)\\.");
    
    // Pattern for obfuscated text
    private static final Pattern shufflePattern = Pattern.compile("\u00a7k[OX]*");
    
    // Anti-shuffle kill counter
    private final KillCounter killCounter = new KillCounter();

    @Override
    public void preInit() {
        System.out.println("Initializing AntiShuffle!");
        
        // Initialize configuration manager first
        ConfigManager.loadConfig();
        
        // Apply saved settings
        enabled = ConfigManager.isAntishuffleEnabled();
        AIChatHandler.setEnabled(ConfigManager.isGoStatsEnabled());
        
        System.out.println("Initial settings - AntiShuffle: " + (enabled ? "enabled" : "disabled") +
                          ", GoStats: " + (AIChatHandler.isEnabled() ? "enabled" : "disabled"));
        
        // Initialize managers
        TeamSpamManager.loadPlayers();
        
        // Register commands
        CommandBus.register(new AntiShuffleCommand());
        KillEffectCommand killEffectCommand = new KillEffectCommand();
        CommandBus.register(killEffectCommand);
        CommandBus.register(new KillEffectCommand2());
        CommandBus.register(new KillCountCommand(killCounter));
        CommandBus.register(new BlitzStartCommand(killCounter));
        
        // Setup team spam commands
        TeamSpamCommands teamSpamCommands = new TeamSpamCommands();
        CommandBus.register(teamSpamCommands);
        
        CommandBus.register(new LanguageCommand());
        CommandBus.register(new TranslateCommand());
        CommandBus.register(new AutoTranslateCommand());
        CommandBus.register(new QueueStatusCommand());
        CommandBus.register(new PlayerInfoCommand());
        CommandBus.register(new ApiKeyCommand());
        CommandBus.register(new GMHelpCommand());
        
        // Register GoStats commands with both names
        CommandBus.register(new GoStatsToggleCommand()); // /gostats123
        
        // Register event subscribers
        EventBus.subscribe(killEffectCommand);
        EventBus.subscribe(killCounter);
        EventBus.subscribe(new AIChatHandler());
        EventBus.subscribe(new ChatTranslator());
        EventBus.subscribe(this);
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
        return shufflePattern.matcher(shuffleText).replaceAll("");
    }
}