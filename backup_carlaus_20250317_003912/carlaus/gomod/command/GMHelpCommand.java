package carlaus.gomod.command;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.command.Command;

public class GMHelpCommand extends Command {
    
    public GMHelpCommand() {
        super("gmhelp");
    }
    
    @Override
    public void handle(String[] args) {
        // Show help information
        sendMessage(EnumChatFormatting.GOLD + "=== gomod Help ===");
        sendMessage(EnumChatFormatting.GREEN + "/opacity " + EnumChatFormatting.YELLOW + "- Toggle player opacity or set amount");
        sendMessage(EnumChatFormatting.GREEN + "/killcount " + EnumChatFormatting.YELLOW + "- Show or reset kill counter");
        sendMessage(EnumChatFormatting.GREEN + "/killeffect <player> " + EnumChatFormatting.YELLOW + "- Check player's kill effect");
        sendMessage(EnumChatFormatting.GREEN + "/ke <player> " + EnumChatFormatting.YELLOW + "- Short command for kill effect");
        sendMessage(EnumChatFormatting.GREEN + "/blitzstart " + EnumChatFormatting.YELLOW + "- Reset and start kill counter for Blitz games");
    }
    
    private void sendMessage(String message) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
    }
} 