package me.ballmc.gomod.command;

import me.ballmc.gomod.Main;
import me.ballmc.gomod.features.PerspectiveDistance;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.command.Command;

/**
 * /persp <number> - sets the third-person perspective camera distance (in blocks).
 */
public class PerspectiveCommand extends Command {
    public PerspectiveCommand() {
        super("persp");
    }

    @Override
    public void handle(String[] args) {
        if (args.length != 1) {
            Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Usage: /persp <blocks>");
            return;
        }
        try {
            float value = Float.parseFloat(args[0]);
            PerspectiveDistance.setDistance(value);
            Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Perspective distance set to " + EnumChatFormatting.GREEN + PerspectiveDistance.getDistance() + EnumChatFormatting.YELLOW + " blocks.");
        } catch (NumberFormatException ex) {
            Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Invalid number. Example: /persp 6.5");
        }
    }
}


