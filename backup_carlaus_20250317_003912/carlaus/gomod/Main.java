package carlaus.gomod;

import net.weavemc.loader.api.ModInitializer;
import net.weavemc.loader.api.command.CommandBus;
import net.weavemc.loader.api.event.*;
import carlaus.gomod.command.KillEffectCommand;
import carlaus.gomod.command.KillEffectCommand2;
import carlaus.gomod.command.KillCountCommand;
import carlaus.gomod.command.OpacityCommand;
import carlaus.gomod.command.BlitzStartCommand;
import carlaus.gomod.command.GMHelpCommand;
import carlaus.gomod.features.KillCounter;
import carlaus.gomod.features.AIChatHandler;
import carlaus.gomod.features.ChatTranslator;
import carlaus.gomod.features.ApiKeyManager;
import carlaus.gomod.features.TeamSpamManager;
import carlaus.gomod.features.ConfigManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Main implements ModInitializer {
    
    public static boolean enabled = true;
    
    // Pattern for detecting shuffled text
    private static final Pattern shufflePattern = Pattern.compile("\u00a7k[OX]*");
    
    // Initialize features
    private final KillCounter killCounter = new KillCounter();
    
    @Override
    public void preInit() {
        System.out.println("Initializing gomod!");
        
        // Initialize configurations
        ConfigManager.loadConfig();
        ApiKeyManager.loadApiKey();
        TeamSpamManager.loadPlayers();
        
        // Initialize settings
        enabled = ConfigManager.isAntishuffleEnabled();
        
        // Log initial settings
        System.out.println("Initial settings - AntiShuffle: " + (enabled ? "enabled" : "disabled") +
                          ", GoStats: " + (ConfigManager.isGoStatsEnabled() ? "enabled" : "disabled"));
        
        // Register commands
        CommandBus.register(new KillEffectCommand());
        CommandBus.register(new KillEffectCommand2());
        CommandBus.register(new KillCountCommand());
        CommandBus.register(new OpacityCommand());
        CommandBus.register(new BlitzStartCommand());
        CommandBus.register(new GMHelpCommand());
        
        // Initialize chat handler
        AIChatHandler.initializeCommandsMap();
        
        // Register event listener for chat events
        EventBus.subscribe(ChatReceivedEvent.class, event -> {
            if (event.getMessage() instanceof ChatComponentText) {
                String message = event.getMessage().getUnformattedText();
                ChatComponentText component = (ChatComponentText) event.getMessage();
                
                AIChatHandler.handleChatMessage(message, component);
                ChatTranslator.handleChatMessage(message, component);
                killCounter.checkForKillMessage(message);
            }
        });
    }
    
    /**
     * Toggles whether anti-shuffle is enabled
     * @param value true to enable, false to disable
     */
    public static void setEnabled(boolean value) {
        enabled = value;
        ConfigManager.setAntishuffleEnabled(value);
        ConfigManager.saveConfig();
        
        String status = value ? "enabled" : "disabled";
        sendMessage(EnumChatFormatting.YELLOW + "gomod " + status);
    }
    
    /**
     * Sends a message to the player's chat
     * @param message the message to send
     */
    public static void sendMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(
                new ChatComponentText(EnumChatFormatting.GOLD + "[gomod] " + EnumChatFormatting.RESET + message)
            );
        }
    }
    
    /**
     * Processes text to remove shuffle formatting
     * @param shuffleText the text that may contain shuffle formatting
     * @return the unshuffled text
     */
    public static String getUnformattedTextForChat(String shuffleText) {
        if (shuffleText == null || !enabled) return shuffleText;
        return shuffleText.replaceAll("\u00a7k[OX]*", "");
    }
} 