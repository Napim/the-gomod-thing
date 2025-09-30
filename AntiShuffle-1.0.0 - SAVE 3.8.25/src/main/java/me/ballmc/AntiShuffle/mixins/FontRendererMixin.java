package me.ballmc.AntiShuffle.mixins;

import me.ballmc.AntiShuffle.Main;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FontRenderer.class)
public abstract class FontRendererMixin {
    @ModifyVariable(method = "renderString", at = @At(value = "HEAD"), argsOnly = true)
    private String renderStringHook(String s) {
        if (Main.enabled) {
         return Main.getUnformattedTextForChat(s);
      } else {
         return s;
      }
    }
}

