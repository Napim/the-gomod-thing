package carlaus.gomod.command;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.command.Command;

import java.util.HashMap;
import java.util.Map;

public class KillEffectCommand extends Command {
    private static final Map<String, String> playerKillEffects = new HashMap<>();
    private static boolean enabled = true;

    public KillEffectCommand() {
        super("killeffect");
    }

    @Override
    public void handle(String[] args) {
        if (args.length == 0) {
            // Toggle the command
            enabled = !enabled;
            sendMessage(EnumChatFormatting.YELLOW + "Kill effect display " + (enabled ? "enabled" : "disabled"));
            return;
        }

        // Simple implementation only supports checking a player's effect
        if (args.length == 1) {
            String playerName = args[0];
            String effect = playerKillEffects.get(playerName.toLowerCase());
            
            if (effect != null) {
                sendMessage(EnumChatFormatting.GREEN + playerName + "'s kill effect: " + effect);
            } else {
                sendMessage(EnumChatFormatting.RED + "No kill effect data for " + playerName);
            }
            return;
        }

        sendMessage(EnumChatFormatting.RED + "Usage: /killeffect [player]");
    }

    private void sendMessage(String message) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
    }

    /**
     * Gets a player's kill effect.
     * @param playerName the player's name
     * @return the kill effect, or null if not known
     */
    public static String getPlayerEffect(String playerName) {
        return playerKillEffects.get(playerName.toLowerCase());
    }
} 