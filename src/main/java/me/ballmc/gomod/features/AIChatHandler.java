package me.ballmc.gomod.features;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
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
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

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
            if (!enabled) {
                System.out.println("[AIChatHandler] Skipping chat processing - functionality is disabled");
                return;
            }
            
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            
            S02PacketChat packet = (S02PacketChat) event.getPacket();
            String message = packet.getChatComponent().getUnformattedText();
            String formattedMessage = packet.getChatComponent().getFormattedText();
            
            // Debug messages
            System.out.println("[AIChatHandler] Processing chat message");
            System.out.println("[AIChatHandler] Raw message: " + message);
            System.out.println("[AIChatHandler] Formatted message: " + formattedMessage);
            
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
                System.out.println("[AIChatHandler] Skipping outgoing private message");
                return;
            }
            
            // Check if it's a private message by looking for "From:" followed by "@gostats123"
            boolean isPrivateMessage = message.contains("From:") && message.indexOf("@gostats123") > message.indexOf("From:");
            
            // Debug check for @gostats123 trigger
            if (message.contains("@gostats123")) {
                System.out.println("[AIChatHandler] Found @gostats123 trigger in message!");
                System.out.println("[AIChatHandler] This should trigger an AI response");
                
                // Verify API key
                String apiKey = ApiKeyManager.getApiKey("openai");
                if (apiKey == null || apiKey.isEmpty()) {
                    System.out.println("[AIChatHandler] ERROR: No OpenAI API key found!");
                    sendPrivateMessage(EnumChatFormatting.RED + "[Error] No OpenAI API key has been set");
                    sendPrivateMessage(EnumChatFormatting.YELLOW + "Please set your API key with: /gomod api openai YOUR_API_KEY");
                    return;
                } else {
                    System.out.println("[AIChatHandler] OpenAI API key is set, attempting to generate response");
                }
                
                // Remove @gostats123 from the message
                String cleanMessage = message.replaceAll("@gostats123", "").trim();
                getChatGPTResponse(cleanMessage, isPrivateMessage);
            } else {
                // Skip messages without the trigger
                System.out.println("[AIChatHandler] Message does not contain @gostats123 trigger, skipping");
            }
        } catch (Exception e) {
            System.out.println("[AIChatHandler] ERROR in onChat: " + e.getMessage());
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
                    sendPrivateMessage(EnumChatFormatting.YELLOW + "Please set your API key with: /gomod api openai YOUR_API_KEY");
                    return;
                }
                
                String maskedKey = apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
                System.out.println("[Debug] Using OpenAI API key: " + maskedKey);

                // Get current player information
                String playerInfo = getCurrentPlayerInfo();
                
                // Create request body
                StringBuilder requestBody = new StringBuilder();
                requestBody.append("{\n");
                requestBody.append("  \"model\": \"gpt-3.5-turbo\",\n");
                requestBody.append("  \"messages\": [\n");
                requestBody.append("    {\n");
                requestBody.append("      \"role\": \"system\",\n");
                requestBody.append("      \"content\": \"You are a helpful assistant in a Minecraft game. Keep responses brief and casual. ");
                requestBody.append("Do not start sentences with capital letters, do not use a dot at the end of sentences. ");
                requestBody.append("Use minecraft slang and be concise. ");
                requestBody.append("If your response is longer than 100 characters, split it into multiple sentences, with a maximum of three sentences total. ");
                requestBody.append("Current game information: ").append(escapeJson(playerInfo)).append("\"\n");
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
                        
                        // Handle AI response
                        if (isPrivateMessage) {
                            sendPrivateMessage(EnumChatFormatting.LIGHT_PURPLE + "[AI] " + EnumChatFormatting.WHITE + formattedResponse);
                        } else {
                            // Split long responses into multiple messages if needed (max 100 chars per message)
                            if (formattedResponse.length() > 100) {
                                List<String> messages = splitResponseIntoMessages(formattedResponse);
                                for (String msgPart : messages) {
                                    Minecraft.getMinecraft().thePlayer.sendChatMessage(msgPart);
                                    
                                    // Small delay between messages to prevent chat rate limiting
                                    try {
                                        Thread.sleep(300);
                                    } catch (InterruptedException ie) {
                                        // Ignore interruption
                                    }
                                }
                            } else {
                                Minecraft.getMinecraft().thePlayer.sendChatMessage(formattedResponse);
                            }
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

    private String getCurrentPlayerInfo() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null) return "No player information available";

            StringBuilder info = new StringBuilder();
            
            // Information about the current player
            info.append("Your player: ").append(mc.thePlayer.getName());
            info.append(", Health: ").append(mc.thePlayer.getHealth()).append("HP");
            info.append(", XP level: ").append(mc.thePlayer.experienceLevel);
            info.append(", Food: ").append(mc.thePlayer.getFoodStats().getFoodLevel()).append("/20");
            
            // Player state information
            if (mc.thePlayer.capabilities.isFlying) info.append(", Flying");
            if (mc.thePlayer.isSprinting()) info.append(", Sprinting");
            if (mc.thePlayer.isSneaking()) info.append(", Sneaking");
            
            // Current equipment - enhanced with full armor details
            info.append(". Your equipment: ");
            
            // Main hand item with count and durability
            if (mc.thePlayer.getCurrentEquippedItem() != null) {
                info.append("Main hand: ").append(formatItemWithDetails(mc.thePlayer.getCurrentEquippedItem()));
            } else {
                info.append("Main hand: empty");
            }
            
            // Armor details
            info.append(". Armor: ");
            String[] armorSlots = {"Helmet", "Chestplate", "Leggings", "Boots"};
            boolean hasArmor = false;
            
            for (int i = 0; i < 4; i++) {
                if (mc.thePlayer.inventory.armorInventory[3-i] != null) {
                    if (hasArmor) info.append(", ");
                    info.append(armorSlots[i]).append(": ")
                        .append(formatItemWithDetails(mc.thePlayer.inventory.armorInventory[3-i]));
                    hasArmor = true;
                }
            }
            
            if (!hasArmor) {
                info.append("none");
            }
            
            // Selected inventory items (count of important items)
            info.append(". Inventory highlights: ");
            Map<String, Integer> importantItems = getImportantItemCounts(mc.thePlayer.inventory);
            if (importantItems.isEmpty()) {
                info.append("no notable items");
            } else {
                List<String> itemStrings = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : importantItems.entrySet()) {
                    itemStrings.add(entry.getKey() + " x" + entry.getValue());
                }
                info.append(String.join(", ", itemStrings));
            }
            
            // Current game mode
            info.append(". Game mode: ").append(mc.thePlayer.capabilities.isCreativeMode ? "Creative" : "Survival");
            
            // Other players information - enhanced with equipment details when possible
            info.append(". Other players: ");
            
            List<String> playerDetails = new ArrayList<>();
            mc.theWorld.playerEntities.forEach(player -> {
                if (player != mc.thePlayer) {  // Skip the current player
                    try {
                        StringBuilder playerInfo = new StringBuilder();
                        playerInfo.append(player.getName());
                        
                        // Calculate distance
                        double distanceSq = mc.thePlayer.getDistanceSqToEntity(player);
                        double distance = Math.sqrt(distanceSq);
                        playerInfo.append(" (").append(Math.round(distance)).append(" blocks away");
                        
                        // Direction information (simplified)
                        double dx = player.posX - mc.thePlayer.posX;
                        double dz = player.posZ - mc.thePlayer.posZ;
                        String direction = getCardinalDirection(dx, dz);
                        playerInfo.append(", ").append(direction).append(" direction");
                        
                        // Add vertical position information (important for Blitz SG)
                        double dy = player.posY - mc.thePlayer.posY;
                        if (Math.abs(dy) > 2) { // Only mention if significant height difference
                            playerInfo.append(", ").append(dy > 0 ? Math.round(dy) + " blocks above" : Math.abs(Math.round(dy)) + " blocks below");
                        }
                        
                        // Health if visible - using HP
                        playerInfo.append(", health: ").append(player.getHealth()).append("HP");
                        
                        // Add their held item if visible
                        net.minecraft.entity.player.EntityPlayer entityPlayer = (net.minecraft.entity.player.EntityPlayer)player;
                        if (entityPlayer.getCurrentEquippedItem() != null) {
                            playerInfo.append(", holding: ")
                                     .append(stripFormatting(entityPlayer.getCurrentEquippedItem().getDisplayName()));
                        }
                        
                        // Armor quality assessment (important for Blitz SG)
                        playerInfo.append(", armor: ").append(assessArmorQuality(entityPlayer));
                        
                        // Check for potion effects (important in Blitz SG)
                        List<String> activeEffects = getActiveEffects(entityPlayer);
                        if (!activeEffects.isEmpty()) {
                            playerInfo.append(", effects: ").append(String.join(", ", activeEffects));
                        }
                        
                        // Check if the player appears to be in combat
                        if (entityPlayer.hurtTime > 0) {
                            playerInfo.append(", in combat");
                        }
                        
                        // Check if player is sneaking/crouching
                        if (entityPlayer.isSneaking()) {
                            playerInfo.append(", sneaking");
                        }
                        
                        playerInfo.append(")");
                        playerDetails.add(playerInfo.toString());
                    } catch (Exception e) {
                        // Skip this player if there's an error
                        playerDetails.add(player.getName() + " (error getting details)");
                    }
                }
            });
            
            if (playerDetails.isEmpty()) {
                info.append("none nearby");
            } else {
                info.append(String.join(", ", playerDetails));
            }
            
            return info.toString();
        } catch (Exception e) {
            System.out.println("[Debug] Error getting player information: " + e.getMessage());
            e.printStackTrace();
            return "Error getting player information";
        }
    }
    
    /**
     * Formats an item with additional details (count, durability if applicable)
     */
    private String formatItemWithDetails(net.minecraft.item.ItemStack item) {
        if (item == null) return "none";
        
        StringBuilder result = new StringBuilder();
        // Strip any formatting codes from the item name
        result.append(stripFormatting(item.getDisplayName()));
        
        // Add count if more than 1
        if (item.stackSize > 1) {
            result.append(" x").append(item.stackSize);
        }
        
        // Add durability info for tools/weapons/armor
        if (item.isItemStackDamageable() && item.getMaxDamage() > 0) {
            int durabilityPercent = Math.round(100f * (item.getMaxDamage() - item.getItemDamage()) / item.getMaxDamage());
            result.append(" (").append(durabilityPercent).append("% durability)");
        }
        
        // Add enchantment info if applicable
        if (item.isItemEnchanted()) {
            result.append(" [enchanted]");
        }
        
        return result.toString();
    }
    
    /**
     * Gets counts of important items in the player's inventory
     */
    private Map<String, Integer> getImportantItemCounts(net.minecraft.entity.player.InventoryPlayer inventory) {
        Map<String, Integer> itemCounts = new HashMap<>();
        
        // Define important items to track
        List<String> importantItems = Arrays.asList(
            "diamond", "gold", "iron", "ender pearl", "potion", "golden apple",
            "arrow", "bow", "sword", "firework", "tnt", "egg", "bed"
        );
        
        // Count main inventory
        for (int i = 0; i < inventory.mainInventory.length; i++) {
            net.minecraft.item.ItemStack stack = inventory.mainInventory[i];
            if (stack != null) {
                String rawItemName = stack.getDisplayName();
                String itemName = stripFormatting(rawItemName).toLowerCase();
                for (String important : importantItems) {
                    if (itemName.contains(important)) {
                        // Use the stripped name for the map key
                        String cleanName = stripFormatting(rawItemName);
                        itemCounts.put(cleanName, 
                            itemCounts.getOrDefault(cleanName, 0) + stack.stackSize);
                        break;
                    }
                }
            }
        }
        
        return itemCounts;
    }
    
    /**
     * Gets the cardinal direction based on relative x and z coordinates.
     */
    private String getCardinalDirection(double dx, double dz) {
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? "east" : "west";
        } else {
            return dz > 0 ? "south" : "north";
        }
    }
    
    /**
     * Strips Minecraft formatting codes (§ and &) from text
     * This prevents issues with chat colors in AI responses
     */
    private String stripFormatting(String text) {
        if (text == null) return "";
        
        // Remove both § and & formatting codes
        // The pattern is § or & followed by one of the Minecraft format codes (0-9, a-f, k-o, r)
        return text.replaceAll("(?i)[§&][0-9a-fk-or]", "");
    }

    /**
     * Assesses the armor quality of a player based on their armor items
     * This is useful for Blitz SG to quickly determine threat level
     */
    private String assessArmorQuality(net.minecraft.entity.player.EntityPlayer player) {
        try {
            int armorValue = player.getTotalArmorValue();
            
            // Get material counts
            int diamondCount = 0;
            int ironCount = 0;
            int goldCount = 0;
            int chainCount = 0;
            int leatherCount = 0;
            
            for (int i = 0; i < 4; i++) {
                if (player.inventory.armorInventory[i] != null) {
                    String armorName = stripFormatting(player.inventory.armorInventory[i].getDisplayName()).toLowerCase();
                    if (armorName.contains("diamond")) diamondCount++;
                    else if (armorName.contains("iron")) ironCount++;
                    else if (armorName.contains("gold") || armorName.contains("golden")) goldCount++;
                    else if (armorName.contains("chain")) chainCount++;
                    else if (armorName.contains("leather")) leatherCount++;
                }
            }
            
            // Determine the primary armor material
            if (diamondCount >= 2) return diamondCount + " diamond pieces (" + armorValue + " points)";
            else if (diamondCount == 1) return "partial diamond (" + armorValue + " points)";
            else if (ironCount >= 3) return "full iron (" + armorValue + " points)";
            else if (ironCount >= 1) return "partial iron (" + armorValue + " points)";
            else if (chainCount + goldCount + leatherCount > 0) return "weak armor (" + armorValue + " points)";
            else return "no armor";
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Gets a list of active potion effects on a player
     * Very useful in Blitz SG where potions/effects provide major advantages
     */
    private List<String> getActiveEffects(net.minecraft.entity.player.EntityPlayer player) {
        List<String> effects = new ArrayList<>();
        try {
            if (player.isPotionActive(1)) effects.add("speed");
            if (player.isPotionActive(5)) effects.add("strength");
            if (player.isPotionActive(8)) effects.add("jump boost");
            if (player.isPotionActive(10)) effects.add("regeneration");
            if (player.isPotionActive(11)) effects.add("resistance");
            if (player.isPotionActive(12)) effects.add("fire resistance");
            if (player.isPotionActive(14)) effects.add("invisibility");
            if (player.isPotionActive(16)) effects.add("night vision");
            if (player.isPotionActive(22)) effects.add("absorption");
            
            // Negative effects
            if (player.isPotionActive(2)) effects.add("slowness");
            if (player.isPotionActive(4)) effects.add("mining fatigue");
            if (player.isPotionActive(9)) effects.add("nausea");
            if (player.isPotionActive(15)) effects.add("blindness");
            if (player.isPotionActive(18)) effects.add("weakness");
            if (player.isPotionActive(19)) effects.add("poison");
            if (player.isPotionActive(20)) effects.add("wither");
            
            // Check if the player is on fire
            if (player.isBurning()) effects.add("burning");
        } catch (Exception e) {
            // Ignore errors when checking potion effects
        }
        return effects;
    }

    /**
     * Splits a long response into multiple messages under 100 characters each.
     * Tries to split at sentence boundaries when possible.
     * Limits to a maximum of 3 messages.
     */
    private List<String> splitResponseIntoMessages(String response) {
        List<String> messages = new ArrayList<>();
        String prefix = "<gostats123> ";
        
        // Remove the prefix for processing if it exists
        String content = response;
        if (content.startsWith(prefix)) {
            content = content.substring(prefix.length());
        }
        
        // Check if this is a list response (contains commas or list formatting)
        if (content.contains(",") && content.split(",").length > 2) {
            return splitListResponse(prefix, content);
        }
        
        // Regular sentence-based splitting for non-list content
        // Split by sentences - simple implementation looking for ". " or "! " or "? "
        String[] sentences = content.split("(?<=[.!?]) ");
        
        StringBuilder currentMessage = new StringBuilder(prefix);
        int messageCount = 0;
        
        for (String sentence : sentences) {
            // Check if adding this sentence would exceed limit
            if (currentMessage.length() + sentence.length() > 100 - 3) { // -3 for possible "..." suffix
                // If we already have 3 messages, just add "..." to the last one and stop
                if (messageCount >= 2) {
                    if (currentMessage.length() + 3 <= 100) {
                        currentMessage.append("...");
                    }
                    messages.add(currentMessage.toString());
                    break;
                }
                
                // Otherwise, add the current message and start a new one
                messages.add(currentMessage.toString());
                currentMessage = new StringBuilder(prefix);
                messageCount++;
            }
            
            // Add the sentence to the current message
            currentMessage.append(sentence).append(" ");
        }
        
        // Add the last message if it's not empty and we haven't reached the limit
        if (currentMessage.length() > prefix.length() && messageCount < 3) {
            messages.add(currentMessage.toString().trim());
        }
        
        // Ensure we don't exceed 3 messages
        if (messages.size() > 3) {
            messages = messages.subList(0, 3);
        }
        
        return messages;
    }
    
    /**
     * Specifically handles splitting list-type responses (like lists of players)
     * to make efficient use of the 3 message limit.
     */
    private List<String> splitListResponse(String prefix, String content) {
        List<String> messages = new ArrayList<>();
        List<String> items = new ArrayList<>();
        
        // First determine if it's a standard list format
        if (content.contains(":") && content.indexOf(":") < 30) {
            // Split into intro and list content
            String intro = content.substring(0, content.indexOf(":") + 1);
            String listContent = content.substring(content.indexOf(":") + 1).trim();
            
            // Add items, splitting by comma
            if (listContent.contains(",")) {
                items.addAll(Arrays.asList(listContent.split("\\s*,\\s*")));
            } else {
                items.add(listContent);
            }
            
            // Rebuild messages with intro in first message
            StringBuilder currentMessage = new StringBuilder(prefix + intro + " ");
            
            for (int i = 0; i < items.size(); i++) {
                String item = items.get(i).trim();
                
                // Check if this item will fit in current message
                if (currentMessage.length() + item.length() + (i < items.size() - 1 ? 2 : 0) > 100) {
                    // If we already have 3 messages, add ".." and stop
                    if (messages.size() >= 2) {
                        // Trim any trailing comma+space
                        if (currentMessage.toString().endsWith(", ")) {
                            currentMessage.setLength(currentMessage.length() - 2);
                        }
                        currentMessage.append("..");
                        messages.add(currentMessage.toString());
                        return messages;
                    }
                    
                    // Otherwise add this message and start a new one
                    messages.add(currentMessage.toString().trim());
                    currentMessage = new StringBuilder(prefix);
                }
                
                currentMessage.append(item);
                if (i < items.size() - 1) {
                    currentMessage.append(", ");
                }
            }
            
            // Add the final message if not empty and we haven't reached 3 yet
            if (currentMessage.length() > prefix.length() && messages.size() < 3) {
                messages.add(currentMessage.toString().trim());
            }
        } else {
            // It's a list but not in standard format, try comma-split approach
            if (content.contains(",")) {
                items.addAll(Arrays.asList(content.split("\\s*,\\s*")));
            } else {
                items.add(content);
            }
            
            StringBuilder currentMessage = new StringBuilder(prefix);
            
            for (int i = 0; i < items.size(); i++) {
                String item = items.get(i).trim();
                
                // Check if this item will fit
                if (currentMessage.length() + item.length() + (i < items.size() - 1 ? 2 : 0) > 100) {
                    // If we already have 3 messages, add ".." and stop
                    if (messages.size() >= 2) {
                        // Trim any trailing comma+space
                        if (currentMessage.toString().endsWith(", ")) {
                            currentMessage.setLength(currentMessage.length() - 2);
                        }
                        currentMessage.append("..");
                        messages.add(currentMessage.toString());
                        return messages;
                    }
                    
                    // Otherwise add this message and start a new one
                    messages.add(currentMessage.toString().trim());
                    currentMessage = new StringBuilder(prefix);
                }
                
                currentMessage.append(item);
                if (i < items.size() - 1) {
                    currentMessage.append(", ");
                }
            }
            
            // Add the final message if not empty and we haven't reached 3 yet
            if (currentMessage.length() > prefix.length() && messages.size() < 3) {
                messages.add(currentMessage.toString().trim());
            }
        }
        
        return messages;
    }
}
