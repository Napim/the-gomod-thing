package me.ballmc.AntiShuffle.command;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.command.Command;

import java.util.HashMap;
import java.util.Map;

/**
 * Command to list all available commands in the mod.
 */
public class GMHelpCommand extends Command {
    
    // Define command categories
    private static final String CATEGORY_GENERAL = "General";
    private static final String CATEGORY_KILL_EFFECTS = "Kill Effects";
    private static final String CATEGORY_TEAM = "Team";
    private static final String CATEGORY_TRANSLATION = "Translation";
    private static final String CATEGORY_API = "API";
    private static final String CATEGORY_MISC = "Miscellaneous";
    
    // Map of commands and their descriptions, organized by category
    private final Map<String, Map<String, String>> commandsByCategory = new HashMap<>();
    
    public GMHelpCommand() {
        super("gmhelp");
        initializeCommands();
    }
    
    private void initializeCommands() {
        // General commands
        addCommand(CATEGORY_GENERAL, "gmhelp", "Show this help message");
        addCommand(CATEGORY_GENERAL, "gostats123", "Toggle @gostats123 AI chat functionality - Allows sending messages with @gostats123 mention");
        addCommand(CATEGORY_GENERAL, "unshuffle", "Toggle anti-shuffle functionality for removing obfuscated (§k) text");
        
        // Kill Effects commands
        addCommand(CATEGORY_KILL_EFFECTS, "ke <player/ALL/toggle>", "Show kill effects for a player or all players");
        
        // Team commands
        addCommand(CATEGORY_TEAM, "tspam", "Toggle team spam invites");
        addCommand(CATEGORY_TEAM, "tspam add <username>", "Add a player to team spam list");
        addCommand(CATEGORY_TEAM, "tspam remove <username>", "Remove a player from team spam list");
        addCommand(CATEGORY_TEAM, "tspam players", "List all players in team spam list");
        addCommand(CATEGORY_TEAM, "tspam confirm", "Confirm using default player list");
        addCommand(CATEGORY_TEAM, "tspam clear", "Clear all players from the list");
        addCommand(CATEGORY_TEAM, "tspam refresh", "Update player names and check MVP++ status");
        addCommand(CATEGORY_TEAM, "pspam <player>", "Toggle party spam for a specific player");
        
        // Translation commands
        addCommand(CATEGORY_TRANSLATION, "lg <lang>", "Set your language for translation");
        addCommand(CATEGORY_TRANSLATION, "translate <text>", "Translate text to your language");
        addCommand(CATEGORY_TRANSLATION, "autotranslate <on/off>", "Toggle automatic translation");
        
        // API commands
        addCommand(CATEGORY_API, "gmapi <service> <apikey>", "Set API key for a service");
        addCommand(CATEGORY_API, "gmapi list", "List all available services and their API keys");
        addCommand(CATEGORY_API, "gmapi check", "Check status of your API keys");
        addCommand(CATEGORY_API, "gmapi reset", "Reset all API keys to default values");
        
        // Miscellaneous commands
        addCommand(CATEGORY_MISC, "qstatus", "Check queue status");
        addCommand(CATEGORY_MISC, "infop <player>", "Get player information");
    }
    
    private void addCommand(String category, String command, String description) {
        if (!commandsByCategory.containsKey(category)) {
            commandsByCategory.put(category, new HashMap<>());
        }
        commandsByCategory.get(category).put(command, description);
    }
    
    @Override
    public void handle(String[] args) {
        if (args.length == 0) {
            sendCommandList();
        } else if (args.length == 1) {
            String category = args[0].toLowerCase();
            
            // Find the matching category (case-insensitive)
            for (String cat : commandsByCategory.keySet()) {
                if (cat.toLowerCase().equals(category)) {
                    sendCategoryCommands(cat);
                    return;
                }
            }
            
            // If no matching category, show all categories
            sendMessage(EnumChatFormatting.RED + "Unknown category: " + category);
            sendMessage(EnumChatFormatting.YELLOW + "Available categories:");
            for (String cat : commandsByCategory.keySet()) {
                sendMessage(EnumChatFormatting.AQUA + "- " + cat);
            }
        } else {
            sendMessage(EnumChatFormatting.RED + "Usage: /gmhelp [category]");
        }
    }
    
    private void sendCommandList() {
        sendMessage(EnumChatFormatting.GOLD + "=== gomod123 Commands ===");
        sendMessage(EnumChatFormatting.YELLOW + "Use " + EnumChatFormatting.WHITE + "/gmhelp <category>" + 
                  EnumChatFormatting.YELLOW + " for more details on a specific category.");
        
        for (String category : commandsByCategory.keySet()) {
            sendMessage("");
            sendMessage(EnumChatFormatting.AQUA + "== " + category + " ==");
            Map<String, String> commands = commandsByCategory.get(category);
            
            for (Map.Entry<String, String> entry : commands.entrySet()) {
                sendMessage(EnumChatFormatting.GREEN + "/" + entry.getKey() + 
                          EnumChatFormatting.GRAY + " - " + entry.getValue());
            }
        }
    }
    
    private void sendCategoryCommands(String category) {
        Map<String, String> commands = commandsByCategory.get(category);
        
        sendMessage(EnumChatFormatting.GOLD + "=== " + category + " Commands ===");
        
        for (Map.Entry<String, String> entry : commands.entrySet()) {
            sendMessage(EnumChatFormatting.GREEN + "/" + entry.getKey() + 
                      EnumChatFormatting.GRAY + " - " + entry.getValue());
        }
    }
    
    private void sendMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
} 