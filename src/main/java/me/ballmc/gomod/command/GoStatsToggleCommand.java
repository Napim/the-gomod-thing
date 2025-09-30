package me.ballmc.gomod.command;

import net.weavemc.loader.api.command.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import me.ballmc.gomod.features.AIChatHandler;
import me.ballmc.gomod.features.ConfigManager;
import me.ballmc.gomod.features.ApiKeyManager;

/**
 * Command to toggle the @gostats123 functionality.
 * 
 * This command enables or disables the ability to chat with an AI assistant in Minecraft.
 * When enabled, players can mention @gostats123 in their messages to trigger an AI response.
 * 
 * Usage:
 * 1. Type "/gostats123" to toggle the feature on/off
 * 2. When enabled, type a message including "@gostats123" to get an AI response
 *    Example: "Hey @gostats123 what's the best strategy for bedwars?"
 * 
 * Note: This feature requires valid API keys to be set using /gomod api openai YOUR_API_KEY
 */
public class GoStatsToggleCommand extends Command {
    
    public GoStatsToggleCommand() {
        super("gostats123");
    }
    
    @Override
    public void handle(String[] args) {
        boolean newState = !ConfigManager.isGoStatsEnabled();
        ConfigManager.setGoStatsEnabled(newState);
        
        // Also update the AIChatHandler's state
        AIChatHandler.setEnabled(newState);
        
        String statusMessage = String.format(
            "%s@gostats123 functionality has been %s%s%s.",
            EnumChatFormatting.BLUE,
            newState ? EnumChatFormatting.GREEN : EnumChatFormatting.RED,
            newState ? "enabled" : "disabled",
            EnumChatFormatting.RESET
        );
        
        sendMessage(statusMessage);
        
        // Show usage information when enabling
        if (newState) {
            sendMessage(EnumChatFormatting.YELLOW + "To use: Include " + EnumChatFormatting.AQUA + "@gostats123" + 
                      EnumChatFormatting.YELLOW + " in your messages to get AI responses.");
            sendMessage(EnumChatFormatting.YELLOW + "Example: Hey " + EnumChatFormatting.AQUA + "@gostats123" + 
                      EnumChatFormatting.YELLOW + " what's the best strategy for bedwars?");
            
            // Check if API key is set
            String apiKey = ApiKeyManager.getApiKey("openai");
            if (apiKey == null || apiKey.isEmpty()) {
                sendMessage(EnumChatFormatting.RED + "Warning: You haven't set an OpenAI API key!");
                sendMessage(EnumChatFormatting.YELLOW + "Use: /gomod api openai YOUR_API_KEY to set your personal API key");
                sendMessage(EnumChatFormatting.YELLOW + "The @gostats123 feature will not work without an API key");
            }
        }
        
        System.out.println("GoStatsToggleCommand executed: " + statusMessage);
    }
    
    private void sendMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
}
