package me.ballmc.AntiShuffle.features;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.event.SubscribeEvent;
import net.weavemc.loader.api.event.PacketEvent.Receive;
import net.minecraft.network.play.server.S02PacketChat;
import net.weavemc.loader.api.event.EventBus;

public class ChatTranslator {
    private static boolean enabled = false;
    private static boolean initialized = false;

    public ChatTranslator() {
        EventBus.subscribe(this);
        initialize();
    }
    
    /**
     * Initializes the ChatTranslator with saved settings from ConfigManager.
     */
    private static void initialize() {
        if (initialized) {
            return;
        }
        
        // Load setting from ConfigManager
        enabled = ConfigManager.isAutoTranslateEnabled();
        initialized = true;
        System.out.println("[ChatTranslator] Initialized with auto-translate " + (enabled ? "enabled" : "disabled"));
    }

    @SubscribeEvent
    public void onChat(Receive event) {
        if (!enabled || !(event.getPacket() instanceof S02PacketChat)) return;

        S02PacketChat packet = (S02PacketChat) event.getPacket();
        // Skip if not a chat message (0 = chat, 1 = system message, 2 = game info)
        if (packet.getType() != 0) return;

        String message = packet.getChatComponent().getUnformattedText();
        
        // Skip system messages, commands, messages that look like English, and translation messages
        if (message.startsWith("/") || 
            message.startsWith("[") || 
            message.startsWith("§") || 
            message.matches("^[A-Za-z0-9\\s.,!?'\"()-_]*$") ||
            message.startsWith("Translation:")) {
            return;
        }

        new Thread(() -> {
            try {
                System.out.println("Attempting to translate: " + message);
                String translatedText = LanguageTranslator.translateText(message, "EN");
                
                if (translatedText != null && !translatedText.equals(message) && 
                    !translatedText.equalsIgnoreCase(message)) {
                    String translationMessage = EnumChatFormatting.GRAY + "Translation: " + 
                                             EnumChatFormatting.WHITE + translatedText;
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(translationMessage));
                }
            } catch (Exception e) {
                System.out.println("Translation error: " + e.getMessage());
            }
        }).start();
    }

    public static void setEnabled(boolean value) {
        if (!initialized) {
            initialize();
        }
        
        enabled = value;
        
        // Save to ConfigManager
        ConfigManager.setAutoTranslateEnabled(value);
        
        String status = enabled ? EnumChatFormatting.GREEN + "enabled" : EnumChatFormatting.RED + "disabled";
        Minecraft.getMinecraft().thePlayer.addChatMessage(
            new ChatComponentText(EnumChatFormatting.YELLOW + "Auto-translation " + status)
        );
    }

    public static boolean isEnabled() {
        if (!initialized) {
            initialize();
        }
        
        return enabled;
    }
} 