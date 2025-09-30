package carlaus.gomod.command;

import carlaus.gomod.features.KillCounter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.command.Command;

public class KillCountCommand extends Command {
    
    private final KillCounter killCounter;
    
    public KillCountCommand() {
        super("killcount");
        this.killCounter = new KillCounter();
    }
    
    @Override
    public void handle(String[] args) {
        if (args.length == 0) {
            // Display current kill count
            int count = killCounter.getKillCount();
            sendMessage(EnumChatFormatting.GREEN + "Current kill count: " + count);
            return;
        }
        
        if (args.length == 1) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("reset")) {
                // Reset kill count
                killCounter.resetKillCount();
                sendMessage(EnumChatFormatting.YELLOW + "Kill count reset to 0");
                return;
            }
            
            if (subCommand.equals("toggle")) {
                // Toggle kill tracking
                boolean enabled = killCounter.toggleTracking();
                sendMessage(EnumChatFormatting.YELLOW + "Kill tracking " + (enabled ? "enabled" : "disabled"));
                return;
            }
        }
        
        sendMessage(EnumChatFormatting.RED + "Usage: /killcount [reset|toggle]");
    }
    
    private void sendMessage(String message) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
    }
} 