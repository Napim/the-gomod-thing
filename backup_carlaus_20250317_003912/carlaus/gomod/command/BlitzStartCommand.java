package carlaus.gomod.command;

import carlaus.gomod.features.KillCounter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.command.Command;

public class BlitzStartCommand extends Command {
    
    private final KillCounter killCounter;
    
    public BlitzStartCommand() {
        super("blitzstart");
        this.killCounter = new KillCounter();
    }
    
    @Override
    public void handle(String[] args) {
        // Reset kill counter at the start of a Blitz game
        killCounter.resetKillCount();
        
        // Enable kill tracking
        killCounter.toggleTracking();
        
        sendMessage(EnumChatFormatting.GREEN + "Blitz game started! Kill counter reset and tracking enabled.");
    }
    
    private void sendMessage(String message) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(
            new ChatComponentText(EnumChatFormatting.GOLD + "[gomod] " + EnumChatFormatting.RESET + message)
        );
    }
} 