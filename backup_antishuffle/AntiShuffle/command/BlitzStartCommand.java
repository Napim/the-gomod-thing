package me.ballmc.AntiShuffle.command;

import net.weavemc.loader.api.command.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import me.ballmc.AntiShuffle.features.KillCounter;

public class BlitzStartCommand extends Command {
    private final KillCounter killCounter;

    public BlitzStartCommand(KillCounter killCounter) {
        super("bstart");
        this.killCounter = killCounter;
    }

    @Override
    public void handle(String[] args) {
        if (args.length == 0) {
            if (!killCounter.isInGame()) {
                killCounter.startGame();
            } else {
                killCounter.endGame();
            }
            return;
        }

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("start")) {
                killCounter.startGame();
            } else if (args[0].equalsIgnoreCase("stop")) {
                killCounter.endGame();
            } else {
                sendMessage(EnumChatFormatting.RED + "Usage: /bstart [start|stop]");
            }
            return;
        }

        sendMessage(EnumChatFormatting.RED + "Usage: /bstart [start|stop]");
    }

    private void sendMessage(String message) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
    }
} 