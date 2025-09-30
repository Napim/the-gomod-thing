package carlaus.gomod.command;

import net.weavemc.loader.api.command.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public class OpacityCommand extends Command {
    private static boolean enabled = false;
    private static float opacity = 1.0f;
    private static boolean invisible = false;

    public OpacityCommand() {
        super("opacity");
    }

    @Override
    public void handle(String[] args) {
        if (args.length == 0) {
            enabled = !enabled;
            invisible = false;
            updateOpacityEffect();
            sendMessage(EnumChatFormatting.GREEN + "Opacity " + (enabled ? "enabled" : "disabled") + 
                       " (Current: " + (int)(opacity * 100) + "%)");
            return;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("invisible")) {
            invisible = !invisible;
            enabled = invisible;
            opacity = invisible ? 0.0f : 1.0f;
            updateOpacityEffect();
            sendMessage(EnumChatFormatting.GREEN + "Invisibility " + (invisible ? "enabled" : "disabled"));
            return;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("setamount")) {
            try {
                int percentage = Integer.parseInt(args[1]);
                if (percentage < 0 || percentage > 100) {
                    sendMessage(EnumChatFormatting.RED + "Opacity must be between 0 and 100!");
                    return;
                }
                opacity = percentage / 100.0f;
                invisible = false;
                if (enabled) {
                    updateOpacityEffect();
                }
                sendMessage(EnumChatFormatting.GREEN + "Opacity set to " + percentage + "%");
            } catch (NumberFormatException e) {
                sendMessage(EnumChatFormatting.RED + "Invalid number format! Usage: /opacity setamount <0-100>");
            }
            return;
        }

        sendMessage(EnumChatFormatting.RED + "Usage: /opacity [setamount <0-100>|invisible]");
    }

    private void updateOpacityEffect() {
        if (Minecraft.getMinecraft().thePlayer == null) return;

        if (enabled) {
            // Add invisibility effect with very long duration
            Minecraft.getMinecraft().thePlayer.addPotionEffect(
                new PotionEffect(Potion.invisibility.getId(), Integer.MAX_VALUE, invisible ? 1 : 0, true, false)
            );
        } else {
            // Remove invisibility effect
            Minecraft.getMinecraft().thePlayer.removePotionEffect(Potion.invisibility.getId());
        }
    }

    private void sendMessage(String message) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static float getOpacity() {
        return opacity;
    }

    public static boolean isInvisible() {
        return invisible;
    }
} 