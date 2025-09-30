package me.ballmc.AntiShuffle.command;

import me.ballmc.AntiShuffle.features.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.command.Command;

public class AntiShuffleCommand extends Command {

    public AntiShuffleCommand() {
        super("antishuffle");
    }

    @Override
    public void handle(String[] args) {
        // Toggle anti-shuffle functionality
        boolean newState = !ConfigManager.isAntishuffleEnabled();
        ConfigManager.setAntishuffleEnabled(newState);
        
        String status = newState ? EnumChatFormatting.GREEN + "enabled" : EnumChatFormatting.RED + "disabled";
        sendMessage(EnumChatFormatting.YELLOW + "AntiShuffle " + status);
    }
    
    private void sendMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
} 