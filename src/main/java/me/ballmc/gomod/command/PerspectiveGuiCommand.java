package me.ballmc.gomod.command;

import me.ballmc.gomod.gui.PerspectiveGUI;
import net.minecraft.client.Minecraft;
import net.weavemc.loader.api.command.Command;

public class PerspectiveGuiCommand extends Command {
    public PerspectiveGuiCommand() {
        super("perspective");
    }

    @Override
    public void handle(String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException ignored) {}
                Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                    @Override
                    public void run() {
                        Minecraft.getMinecraft().displayGuiScreen(new PerspectiveGUI());
                    }
                });
            }
        }, "OpenPerspectiveGui").start();
    }
}


