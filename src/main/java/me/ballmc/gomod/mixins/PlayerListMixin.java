package me.ballmc.gomod.mixins;

import me.ballmc.gomod.features.KillCounter;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.EnumChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.regex.Pattern;

/**
 * Mixin to color player names in the tab list based on kill rankings.
 */
@Mixin(GuiPlayerTabOverlay.class)
public class PlayerListMixin {
    
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void gomod$colorPlayerName(NetworkPlayerInfo networkPlayerInfo, CallbackInfoReturnable<String> cir) {
        if (!KillCounter.isEnabled()) {
            return;
        }
        
        String originalName = cir.getReturnValue();
        if (originalName == null || originalName.isEmpty()) {
            return;
        }
        
        // Base username from profile (no colors)
        String baseUsername = networkPlayerInfo.getGameProfile().getName();

        String updated = originalName;
        // If we've already overridden displayName from code, skip adding a mixin suffix to avoid duplicates
        if (networkPlayerInfo.getDisplayName() != null) {
            cir.setReturnValue(originalName);
            return;
        }

        // Do not modify the player's existing colors/prefixes; only append a colored suffix if kills >= 1
        int kills = KillCounter.getKillCount(baseUsername);
        if (kills >= 1) {
            boolean isTop = KillCounter.getPlayerRank(baseUsername) == 1;
            String style = (isTop ? EnumChatFormatting.BOLD.toString() : "") + EnumChatFormatting.DARK_RED;
            String withKills = updated + " " + style + "[" + kills + "]" + EnumChatFormatting.RESET;
            cir.setReturnValue(withKills);
        } else {
            // Leave original server-provided formatting
            cir.setReturnValue(originalName);
        }
    }
}
