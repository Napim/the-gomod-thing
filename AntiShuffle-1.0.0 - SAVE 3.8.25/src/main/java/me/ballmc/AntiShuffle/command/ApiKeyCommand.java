package me.ballmc.AntiShuffle.command;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.command.Command;
import me.ballmc.AntiShuffle.features.ApiKeyManager;

/**
 * Command to set API keys for various services.
 */
public class ApiKeyCommand extends Command {
    
    public ApiKeyCommand() {
        super("gmapi");
    }
    
    @Override
    public void handle(String[] args) {
        if (args.length == 0) {
            sendUsage();
            return;
        }
        
        if (args[0].equalsIgnoreCase("reset")) {
            ApiKeyManager.resetApiKeys();
            sendMessage(EnumChatFormatting.GREEN + "All API keys have been removed.");
            sendMessage(EnumChatFormatting.YELLOW + "You'll need to set your own API keys for OpenAI and Translation services.");
            return;
        }
        
        if (args[0].equalsIgnoreCase("list")) {
            sendMessage(EnumChatFormatting.YELLOW + "Available API services:");
            
            String hypixelKey = ApiKeyManager.getApiKey("hypixel");
            String openaiKey = ApiKeyManager.getApiKey("openai");
            String translationKey = ApiKeyManager.getApiKey("translation");
            
            boolean hypixelValid = !hypixelKey.isEmpty();
            boolean openaiValid = !openaiKey.isEmpty();
            boolean translationValid = !translationKey.isEmpty();
            
            sendMessage(EnumChatFormatting.GOLD + " - Hypixel: " + 
                         (hypixelValid ? EnumChatFormatting.GREEN + maskApiKey(hypixelKey) : 
                                        EnumChatFormatting.RED + "Not set"));
            
            sendMessage(EnumChatFormatting.GOLD + " - OpenAI: " + 
                         (openaiValid ? EnumChatFormatting.GREEN + maskApiKey(openaiKey) : 
                                      EnumChatFormatting.RED + "Not set"));
            
            sendMessage(EnumChatFormatting.GOLD + " - Translation: " + 
                         (translationValid ? EnumChatFormatting.GREEN + maskApiKey(translationKey) : 
                                           EnumChatFormatting.RED + "Not set"));
            
            // Show status of features related to API keys
            sendMessage("");
            sendMessage(EnumChatFormatting.YELLOW + "Features status:");
            
            // AI Chat feature
            sendMessage(EnumChatFormatting.AQUA + " - AI Chat (@gostats123): " + 
                        (openaiValid ? EnumChatFormatting.GREEN + "Ready" : 
                                     EnumChatFormatting.RED + "No API key"));
            
            // Translation feature  
            sendMessage(EnumChatFormatting.AQUA + " - Translation: " + 
                        (translationValid ? EnumChatFormatting.GREEN + "Ready" : 
                                          EnumChatFormatting.RED + "No API key"));
            
            // Hypixel API features
            sendMessage(EnumChatFormatting.AQUA + " - Hypixel API features: " + 
                        (hypixelValid ? EnumChatFormatting.GREEN + "Ready" : 
                                      EnumChatFormatting.RED + "No API key"));
            return;
        }
        
        if (args.length >= 2) {
            String service = args[0].toLowerCase();
            String apiKey = args[1];
            
            if (service.equals("hypixel") || service.equals("openai") || service.equals("translation")) {
                // Validate the API key - keys should never be just spaces or too short
                apiKey = apiKey.trim();
                if (apiKey.isEmpty() || apiKey.length() < 5) {
                    sendMessage(EnumChatFormatting.RED + "Error: The API key provided is too short or invalid.");
                    return;
                }
                
                ApiKeyManager.setApiKey(service, apiKey);
                sendMessage(EnumChatFormatting.GREEN + "Updated " + service + " API key to: " + maskApiKey(apiKey));
                
                // Give specific feedback based on service
                if (service.equals("openai")) {
                    sendMessage(EnumChatFormatting.YELLOW + "AI Chat with @gostats123 should now work properly!");
                    sendMessage(EnumChatFormatting.YELLOW + "Try sending a message with @gostats123 mentioned to test it.");
                } else if (service.equals("translation")) {
                    sendMessage(EnumChatFormatting.YELLOW + "Translation features should now work properly!");
                } else if (service.equals("hypixel")) {
                    sendMessage(EnumChatFormatting.YELLOW + "Hypixel API features should now work properly!");
                }
                
                return;
            }
            
            sendMessage(EnumChatFormatting.RED + "Unknown service: " + service);
            sendMessage(EnumChatFormatting.YELLOW + "Available services: hypixel, openai, translation");
            return;
        }
        
        sendUsage();
    }
    
    private void sendUsage() {
        sendMessage(EnumChatFormatting.YELLOW + "Usage:");
        sendMessage(EnumChatFormatting.GOLD + " - /gmapi list " + 
                     EnumChatFormatting.WHITE + "- Show all API keys");
        sendMessage(EnumChatFormatting.GOLD + " - /gmapi reset " + 
                     EnumChatFormatting.WHITE + "- Remove all API keys");
        sendMessage(EnumChatFormatting.GOLD + " - /gmapi <service> <key> " + 
                     EnumChatFormatting.WHITE + "- Set API key for service");
        sendMessage(EnumChatFormatting.YELLOW + "Available services: hypixel, openai, translation");
        sendMessage(EnumChatFormatting.YELLOW + "Note: You need to provide your own API keys for OpenAI and Translation services.");
    }
    
    private void sendMessage(String message) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
    }
    
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "Not set";
        }
        
        // Show first 4 and last 4 characters, mask the rest
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
} 