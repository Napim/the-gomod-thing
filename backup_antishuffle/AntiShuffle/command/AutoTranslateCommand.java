package me.ballmc.AntiShuffle.command;

import me.ballmc.AntiShuffle.features.ChatTranslator;
import me.ballmc.AntiShuffle.features.ConfigManager;
import net.weavemc.loader.api.command.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class AutoTranslateCommand extends Command {
    public AutoTranslateCommand() {
        super("autotranslate");
    }

    @Override
    public void handle(String[] args) {
        boolean newState = !ChatTranslator.isEnabled();
        ChatTranslator.setEnabled(newState);
        
        // Config is saved inside ChatTranslator.setEnabled()
    }
} 