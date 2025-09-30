package me.ballmc.AntiShuffle.command;

import net.weavemc.loader.api.command.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import me.ballmc.AntiShuffle.features.KillCounter;

public class KillCountCommand extends Command {
    private final KillCounter killCounter;

    public KillCountCommand(KillCounter killCounter) {
        super("kcount");
        this.killCounter = killCounter;
    }

    @Override
    public void handle(String[] args) {
        if (!killCounter.isInGame()) {
            sendMessage(EnumChatFormatting.RED + "No game is currently in progress!");
            return;
        }

        sendMessage(EnumChatFormatting.YELLOW + "Kill Counts:");
        for (var entry : killCounter.getSortedKills()) {
            String playerName = entry.getKey();
            int kills = entry.getValue();
            sendMessage(EnumChatFormatting.WHITE + playerName + 
                       EnumChatFormatting.GRAY + ": " + 
                       EnumChatFormatting.GREEN + kills);
        }
    }

    private void sendMessage(String message) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
    }
} 