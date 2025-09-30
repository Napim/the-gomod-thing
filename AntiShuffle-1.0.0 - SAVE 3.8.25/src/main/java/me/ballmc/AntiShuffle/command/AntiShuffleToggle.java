package me.ballmc.AntiShuffle.command;

import net.weavemc.loader.api.command.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import static me.ballmc.AntiShuffle.Main.*;

public class AntiShuffleToggle extends Command {
    public AntiShuffleToggle() {
        super("antishuffle");
    }

    @Override
    public void handle(String[] args) {
        setEnabled(!enabled);
    }
}
