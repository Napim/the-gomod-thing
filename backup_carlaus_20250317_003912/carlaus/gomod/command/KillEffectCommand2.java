package carlaus.gomod.command;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.command.Command;

import java.util.HashMap;
import java.util.Map;

public class KillEffectCommand2 extends Command {
    private static final Map<String, String> playerKillEffects = new HashMap<>();
    
    public KillEffectCommand2() {
        super("ke");
    }
    
    @Override
    public void handle(String[] args) {
        if (args.length == 0) {
            sendMessage(EnumChatFormatting.RED + "Usage: /ke <player>");
            return;
        }
        
        // Simple implementation just checks a player's effect
        String playerName = args[0];
        String effect = playerKillEffects.get(playerName.toLowerCase());
        
        if (effect != null) {
            sendMessage(EnumChatFormatting.GREEN + playerName + "'s kill effect: " + effect);
        } else {
            sendMessage(EnumChatFormatting.RED + "No kill effect data for " + playerName);
        }
    }
    
    private void sendMessage(String message) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
    }
} 