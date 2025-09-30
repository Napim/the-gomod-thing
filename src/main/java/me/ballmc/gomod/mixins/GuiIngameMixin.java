package me.ballmc.gomod.mixins;

import me.ballmc.gomod.features.InventoryHUD;
import net.minecraft.client.gui.GuiIngame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public abstract class GuiIngameMixin {
    
    /**
     * Renders the inventory HUD after the game overlay is drawn
     */
    @Inject(method = "renderGameOverlay", at = @At("RETURN"))
    private void renderInventoryHUD(CallbackInfo ci) {
        try {
            InventoryHUD.render();
        } catch (Exception e) {
            // Prevent exceptions from propagating and causing disconnection
            System.err.println("Error in GuiIngameMixin.renderInventoryHUD: " + e.getMessage());
        }
    }
}
