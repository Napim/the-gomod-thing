package me.ballmc.gomod.command;

import net.weavemc.loader.api.command.Command;
import me.ballmc.gomod.gui.KitLevelsGUI;
import me.ballmc.gomod.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;

/**
 * Command to open the Kit Levels GUI.
 * Allows users to search for players with specific kits and view their levels.
 */
public class KitLevelsCommand extends Command {
    
    public KitLevelsCommand() {
        super("kitlevels");
    }
    
    @Override
    public void handle(String[] args) {
        if (args.length == 0) {
            // Open the GUI
            Minecraft.getMinecraft().displayGuiScreen(new KitLevelsGUI(null));
            Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN + "Opened Kit Levels GUI");
        } else {
            // Show help
            Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Kit Levels Viewer");
            Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GRAY + "Usage: /kitlevels");
            Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GRAY + "Opens the Kit Levels GUI to search for players with specific kits.");
            Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GRAY + "Requires a Hypixel API key set with /gmapi hypixel <key>");
        }
    }
}
