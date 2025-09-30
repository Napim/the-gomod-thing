package me.ballmc.AntiShuffle.command;

import net.weavemc.loader.api.command.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.ballmc.AntiShuffle.features.ApiKeyManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import me.ballmc.AntiShuffle.features.AIChatHandler;

/**
 * DEPRECATED: This command has been disabled to prevent duplicate commands.
 * Please use GoStatsToggleCommand instead.
 * 
 * This class is kept for reference but is no longer registered in the Main class.
 */
public class GoStatsCommandDeprecated extends Command {
    private boolean debugMode = false; // Set to false to disable
    
    public GoStatsCommandDeprecated() {
        super("gostats_disabled");
    }
    
    @Override
    public void handle(String[] args) {
        // This command is disabled to prevent duplicate execution
        sendMessage(EnumChatFormatting.RED + "This command has been deprecated. Please use /gostats123 instead.");
    }
    
    private void sendMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }

    private void getChatGPTResponse(String message) {
        new Thread(() -> {
            try {
                if (debugMode) {
                    sendMessage(EnumChatFormatting.YELLOW + "[Debug] Sending request to OpenAI...");
                }
                
                URL url = new URL("https://api.openai.com/v1/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + ApiKeyManager.getApiKey("openai").trim());
                conn.setDoOutput(true);

                // Build the prompt
                String prompt = "If blitz sg or bsg or blitz in mentioned, it refers to the blitz survival games of hypixel. "
                    + "Never type @everyone or @here. "
                    + "CRITICAL RULES:\n"
                    + "1. NEVER use any inappropriate language, slurs, hate speech, or sexual content\n"
                    + "2. Keep all responses family-friendly and appropriate for Hypixel's chat rules\n"
                    + "3. Do not use any words that could trigger chat filters\n"
                    + "4. Avoid references to violence, drugs, or adult themes\n"
                    + "5. Keep responses clean and playful\n"
                    + "6. CRITICAL: Keep responses VERY SHORT - under 100 characters total\n"
                    + "7. NEVER mention or discuss IP addresses, locations, or personal information\n"
                    + "8. NEVER engage in conversations about doxxing or revealing private information\n"
                    + "9. If someone asks about personal info or locations, change the subject to Blitz SG\n"
                    + "10. NEVER use the word 'ip' in any context\n\n"
                    + "Respond to the following message in a short, sarcastic and conversational funny way, while following the above rules. "
                    + "Write as if you were a Discord user, but keep it clean and appropriate. "
                    + "Do not start sentences with capital letters, do not use a dot at the end of sentences, "
                    + "IMPORTANT: Do not start your response with 'oh', 'wow', 'well', or any similar interjections - "
                    + "jump straight into your response with variety and creativity. "
                    + "CRITICAL: Never include any bot names, prefixes, or colons at the start of your message. "
                    + "Your response should start directly with lowercase text, as if you were a regular Discord user. "
                    + "For example, instead of 'gostats123: hello' just say 'hello'. "
                    + "Remember to keep responses VERY SHORT - under 100 characters.";

                // Create request body
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", "gpt-3.5-turbo");
                requestBody.addProperty("temperature", 0.7);
                requestBody.addProperty("max_tokens", 40);

                JsonObject systemMessage = new JsonObject();
                systemMessage.addProperty("role", "system");
                systemMessage.addProperty("content", prompt);

                JsonObject userMessage = new JsonObject();
                userMessage.addProperty("role", "user");
                userMessage.addProperty("content", message);

                com.google.gson.JsonArray messagesArray = new com.google.gson.JsonArray();
                messagesArray.add(systemMessage);
                messagesArray.add(userMessage);
                requestBody.add("messages", messagesArray);

                String jsonBody = requestBody.toString();

                // Send request
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // Check response code
                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    sendMessage(EnumChatFormatting.RED + "[Error] API Error " + responseCode + ": " + errorResponse.toString());
                    return;
                }

                // Get response
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }

                // Parse JSON response to get the message
                JsonParser parser = new JsonParser();
                JsonObject jsonResponse = parser.parse(response.toString()).getAsJsonObject();
                String aiMessage = jsonResponse.get("choices").getAsJsonArray()
                    .get(0).getAsJsonObject()
                    .get("message").getAsJsonObject()
                    .get("content").getAsString();

                // Ensure response is within character limit
                if (aiMessage.length() > 100) {
                    aiMessage = aiMessage.substring(0, 97) + "...";
                }

                // Send the message to the player
                if (debugMode) {
                    sendMessage(EnumChatFormatting.GREEN + "[Response] " + aiMessage);
                } else {
                    Minecraft.getMinecraft().thePlayer.sendChatMessage(aiMessage);
                }
            } catch (Exception e) {
                sendMessage(EnumChatFormatting.RED + "[Error] API error: " + e.getMessage());
            }
        }).start();
    }
} 