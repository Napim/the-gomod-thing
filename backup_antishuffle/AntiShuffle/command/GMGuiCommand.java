package me.ballmc.AntiShuffle.command;

import net.weavemc.loader.api.command.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import me.ballmc.AntiShuffle.gui.GoMod123SettingsGui;
import me.ballmc.AntiShuffle.gui.SimpleGuiScreen;
import net.minecraft.util.ChatComponentText;
import me.ballmc.AntiShuffle.Main;

/**
 * Command that opens the settings GUI for GoMod123.
 * Usage: /gm gui
 */
public class GMGuiCommand extends Command {
    
    public GMGuiCommand() {
        super("gm", "gomod", "gomod123");  // Multiple aliases
    }

    @Override
    public void handle(String[] args) {
        // Debug message to see if the command is being called
        Main.sendMessage("§eGM Command received. Args length: " + args.length);
        
        if (args.length >= 1) {
            Main.sendMessage("§eArg 0: " + args[0]);
        }
        
        // If no args or first arg is "gui", open the GUI
        if (args.length == 0 || (args.length >= 1 && args[0].equalsIgnoreCase("gui"))) {
            Main.sendMessage("§aOpening GoMod123 Settings GUI...");
            
            try {
                // Try with the simple GUI first for testing
                Main.sendMessage("§eTrying to use SimpleGuiScreen...");
                Minecraft.getMinecraft().displayGuiScreen(new SimpleGuiScreen());
            } catch (Exception e) {
                Main.sendMessage("§cError opening SimpleGuiScreen: " + e.getMessage());
                e.printStackTrace();
                
                try {
                    // If that fails, try with scheduling
                    Main.sendMessage("§eTrying with scheduling...");
                    Minecraft mc = Minecraft.getMinecraft();
                    mc.addScheduledTask(() -> {
                        mc.displayGuiScreen(new SimpleGuiScreen());
                    });
                } catch (Exception e2) {
                    Main.sendMessage("§cError with scheduled GUI: " + e2.getMessage());
                    e2.printStackTrace();
                }
            }
        }
    }
} 