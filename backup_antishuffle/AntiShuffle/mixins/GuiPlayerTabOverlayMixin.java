package me.ballmc.AntiShuffle.mixins;

import me.ballmc.AntiShuffle.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Comparator;
import com.google.common.collect.Ordering;

@Mixin(GuiPlayerTabOverlay.class)
public class GuiPlayerTabOverlayMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    public void getPlayerNameHook(NetworkPlayerInfo networkPlayerInfoIn, CallbackInfoReturnable<String> cir) {
        try {
            String name = cir.getReturnValue();
            if (name != null && Main.enabled) {
                cir.setReturnValue(Main.getUnformattedTextForChat(name));
            }
        } catch (Exception e) {
            // Prevent exceptions from propagating and causing disconnection
            System.err.println("Error in GuiPlayerTabOverlayMixin.getPlayerNameHook: " + e.getMessage());
        }
    }
} 