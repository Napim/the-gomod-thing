package me.ballmc.gomod.command;

import net.weavemc.loader.api.command.Command;
import me.ballmc.gomod.features.AutoQueue;
import static me.ballmc.gomod.Main.sendMessage;
import net.minecraft.util.EnumChatFormatting;

public class AutoQueueCommand extends Command {
    public AutoQueueCommand() {
        super("autoqueue");
    }

    @Override
    public void handle(String[] args) {
        AutoQueue.setEnabled(!AutoQueue.isEnabled());
    }
}
