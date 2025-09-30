package me.ballmc.AntiShuffle.features;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.event.Event;
import net.weavemc.loader.api.event.SubscribeEvent;
import net.weavemc.loader.api.event.PacketEvent.Receive;
import net.minecraft.network.play.server.S02PacketChat;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;

public class AIChatHandler {
    private final List<String> chatContext = new ArrayList<>();
    private static final int MAX_CONTEXT_MESSAGES = 1;
    private static boolean enabled = true; // Toggle for @gostats123 functionality
    private static boolean initialized = false;
    
    public AIChatHandler() {
        initialize();
    }
    
    /**
     * Initializes the AIChatHandler with saved settings from ConfigManager.
     */
    private static void initialize() {
        if (initialized) {
            return;
        }
        
        // Load setting from ConfigManager
        enabled = ConfigManager.isGoStatsEnabled();
        initialized = true;
        System.out.println("[AIChatHandler] Initialized with GoStats " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Toggles the @gostats123 functionality on or off.
     * 
     * @param value True to enable, false to disable
     * @return The new state
     */
    public static boolean setEnabled(boolean value) {
        if (!initialized) {
            initialize();
        }
        
        enabled = value;
        
        // Save to ConfigManager
        ConfigManager.setGoStatsEnabled(value);
        
        return enabled;
    }
    
    /**
     * Gets the current state of the @gostats123 functionality.
     * 
     * @return True if enabled, false if disabled
     */
    public static boolean isEnabled() {
        if (!initialized) {
            initialize();
        }
        
        return enabled;
    }

    @SubscribeEvent
    public void onChat(Receive event) {
        try {
            if (!(event.getPacket() instanceof S02PacketChat)) return;
            
            // Skip if functionality is disabled
            if (!enabled) return;
            
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            
            S02PacketChat packet = (S02PacketChat) event.getPacket();
            String message = packet.getChatComponent().getUnformattedText();
            String formattedMessage = packet.getChatComponent().getFormattedText();
            
            // Debug messages
            System.out.println("[Debug] Raw message: " + message);
            System.out.println("[Debug] Formatted message: " + formattedMessage);
            
            // Check for spectator error and retry without /shout
            if (message.contains("You are not allowed to use that command as a spectator!")) {
                String originalMessage = message.replace("You are not allowed to use that command as a spectator!", "").trim();
                if (originalMessage.startsWith("/shout ")) {
                    originalMessage = originalMessage.substring(7).trim();
                    if (!originalMessage.isEmpty()) {
                        Minecraft.getMinecraft().thePlayer.sendChatMessage(originalMessage);
                    }
                }
                return;
            }
            
            // Skip if it's an outgoing private message
            if (formattedMessage.contains("To:")) {
                System.out.println("[Debug] Skipping outgoing private message");
                return;
            }
            
            // Check if it's a private message by looking for "From:" followed by "@gostats123"
            boolean isPrivateMessage = message.contains("From:") && message.indexOf("@gostats123") > message.indexOf("From:");
            
            if (message.contains("@gostats123")) {
                System.out.println("[Debug] Found @gostats123 trigger");
                // Remove @gostats123 from the message
                String cleanMessage = message.replaceAll("@gostats123", "").trim();
                getChatGPTResponse(cleanMessage, isPrivateMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getChatGPTResponse(String message, boolean isPrivateMessage) {
        new Thread(() -> {
            try {
                // Add message to context
                chatContext.add(message);
                
                // Limit context size
                while (chatContext.size() > MAX_CONTEXT_MESSAGES) {
                    chatContext.remove(0);
                }
                
                // Get API key and validate
                String apiKey = ApiKeyManager.getApiKey("openai");
                
                // Debug API key (mask it for privacy)
                if (apiKey == null || apiKey.isEmpty()) {
                    sendPrivateMessage(EnumChatFormatting.RED + "[Error] No OpenAI API key has been set");
                    sendPrivateMessage(EnumChatFormatting.YELLOW + "Please set your API key with: /gmapi openai YOUR_API_KEY");
                    return;
                }
                
                String maskedKey = apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
                System.out.println("[Debug] Using OpenAI API key: " + maskedKey);
                
                // Create request body
                StringBuilder requestBody = new StringBuilder();
                requestBody.append("{\n");
                requestBody.append("  \"model\": \"gpt-3.5-turbo\",\n");
                requestBody.append("  \"messages\": [\n");
                requestBody.append("    {\n");
                requestBody.append("      \"role\": \"system\",\n");
                requestBody.append("      \"content\": \"You are a helpful assistant in a Minecraft game. Keep responses brief and casual. ");
                requestBody.append("Do not start sentences with capital letters, do not use a dot at the end of sentences. ");
                requestBody.append("Use minecraft slang and be concise. Maximum 1-2 sentences.\"\n");
                requestBody.append("    },\n");
                
                // Add context messages
                for (String contextMessage : chatContext) {
                    requestBody.append("    {\n");
                    requestBody.append("      \"role\": \"user\",\n");
                    requestBody.append("      \"content\": \"").append(escapeJson(contextMessage)).append("\"\n");
                    requestBody.append("    },\n");
                }
                
                // Remove trailing comma if there are context messages
                if (!chatContext.isEmpty()) {
                    requestBody.deleteCharAt(requestBody.length() - 2);
                }
                
                requestBody.append("  ],\n");
                requestBody.append("  \"temperature\": 0.7,\n");
                requestBody.append("  \"max_tokens\": 60\n");
                requestBody.append("}");
                
                // Send request to OpenAI API
                URL url = new URL("https://api.openai.com/v1/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                
                // Trim whitespace from API key and ensure it's properly formatted
                apiKey = apiKey.trim();
                System.out.println("[Debug] API key length: " + apiKey.length());
                
                // Set authorization header
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setDoOutput(true);
                
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        
                        JsonObject jsonResponse = new JsonParser().parse(response.toString()).getAsJsonObject();
                        String aiResponse = jsonResponse.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString().trim();
                        
                        // Add the <gostats123> prefix to the response
                        String formattedResponse = "<gostats123> " + aiResponse;
                        
                        // Send AI response
                        if (isPrivateMessage) {
                            sendPrivateMessage(EnumChatFormatting.LIGHT_PURPLE + "[AI] " + EnumChatFormatting.WHITE + formattedResponse);
                        } else {
                            Minecraft.getMinecraft().thePlayer.sendChatMessage(formattedResponse);
                        }
                    }
                } else {
                    // Handle error
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                        StringBuilder errorResponse = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            errorResponse.append(responseLine.trim());
                        }
                        sendPrivateMessage(EnumChatFormatting.RED + "[Error] API Error " + responseCode + ": " + errorResponse.toString());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendPrivateMessage(EnumChatFormatting.RED + "[Error] API error: " + e.getMessage());
            }
        }).start();
    }
    
    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    private void sendPrivateMessage(String message) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
    }
} 