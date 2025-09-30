package me.ballmc.AntiShuffle.mixins;

import me.ballmc.AntiShuffle.command.OpacityCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RendererLivingEntity.class, priority = 1500) // Lower priority to let other mods inject first
public class RenderLivingMixin {

    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true)
    private void onRenderModelPre(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, CallbackInfo ci) {
        try {
            if (entity != null && Minecraft.getMinecraft() != null && Minecraft.getMinecraft().thePlayer != null && 
                OpacityCommand.isEnabled() && entity == Minecraft.getMinecraft().thePlayer) {
                if (OpacityCommand.isInvisible()) {
                    ci.cancel();
                    return;
                }
                
                // Save the current GL state
                GlStateManager.pushAttrib();
                GlStateManager.enableBlend();
                GlStateManager.enableAlpha();
                GlStateManager.blendFunc(770, 771);
                GlStateManager.color(1.0F, 1.0F, 1.0F, OpacityCommand.getOpacity());
            }
        } catch (Exception e) {
            // Prevent exceptions from propagating and causing disconnection
            System.err.println("Error in RenderLivingMixin.onRenderModelPre: " + e.getMessage());
        }
    }

    @Inject(method = "renderModel", at = @At("RETURN"))
    private void onRenderModelPost(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, CallbackInfo ci) {
        try {
            if (entity != null && Minecraft.getMinecraft() != null && Minecraft.getMinecraft().thePlayer != null &&
                OpacityCommand.isEnabled() && entity == Minecraft.getMinecraft().thePlayer) {
                // Restore the previous GL state
                GlStateManager.popAttrib();
            }
        } catch (Exception e) {
            // Prevent exceptions from propagating and causing disconnection
            System.err.println("Error in RenderLivingMixin.onRenderModelPost: " + e.getMessage());
        }
    }

    @Inject(method = "renderLayers", at = @At("HEAD"), cancellable = true)
    private void onRenderLayers(EntityLivingBase entity, float p_177093_2_, float p_177093_3_, float partialTicks, float p_177093_5_, float p_177093_6_, float p_177093_7_, float p_177093_8_, CallbackInfo ci) {
        try {
            if (entity != null && Minecraft.getMinecraft() != null && Minecraft.getMinecraft().thePlayer != null &&
                OpacityCommand.isEnabled() && entity == Minecraft.getMinecraft().thePlayer && OpacityCommand.isInvisible()) {
                ci.cancel();
            }
        } catch (Exception e) {
            // Prevent exceptions from propagating and causing disconnection
            System.err.println("Error in RenderLivingMixin.onRenderLayers: " + e.getMessage());
        }
    }
} 