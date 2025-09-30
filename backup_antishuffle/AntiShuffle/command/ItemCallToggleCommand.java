package me.ballmc.AntiShuffle.command;

import net.weavemc.loader.api.command.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import me.ballmc.AntiShuffle.features.ConfigManager;

/**
 * Command to toggle the /itemcall functionality.
 *
 * This command enables or disables the ability to automatically send a chat message
 * listing the items in a chest when it is opened.
 *
 * Usage:
 * 1. Type "/itemcall" to toggle the feature on/off
 */
public class ItemCallToggleCommand extends Command {
    
    public ItemCallToggleCommand() {
        super("itemcall");
    }
    
    @Override
    public void handle(String[] args) {
        boolean newState = !ConfigManager.isItemCallEnabled();
        ConfigManager.setItemCallEnabled(newState);
        
        String statusMessage = String.format(
            "%s/itemcall functionality has been %s%s%s.",
            EnumChatFormatting.BLUE,
            newState ? EnumChatFormatting.GREEN : EnumChatFormatting.RED,
            newState ? "enabled" : "disabled",
            EnumChatFormatting.RESET
        );
        
        sendMessage(statusMessage);
        System.out.println("ItemCallToggleCommand executed: " + statusMessage);
    }
    
    private void sendMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
} 