package carlaus.gomod.mixins;

import carlaus.gomod.command.OpacityCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RendererLivingEntity.class)
public class RenderLivingMixin {

    @Inject(method = "renderModel", at = @At("HEAD"))
    private void onRenderModelPre(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, CallbackInfo ci) {
        if (OpacityCommand.isEnabled() && entity == Minecraft.getMinecraft().thePlayer) {
            if (OpacityCommand.isInvisible()) {
                ci.cancel();
                return;
            }
            
            GlStateManager.enableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.blendFunc(770, 771);
            GlStateManager.color(1.0F, 1.0F, 1.0F, OpacityCommand.getOpacity());
        }
    }

    @Inject(method = "renderModel", at = @At("RETURN"))
    private void onRenderModelPost(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, CallbackInfo ci) {
        if (OpacityCommand.isEnabled() && entity == Minecraft.getMinecraft().thePlayer) {
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Inject(method = "renderLayers", at = @At("HEAD"), cancellable = true)
    private void onRenderLayers(EntityLivingBase entity, float p_177093_2_, float p_177093_3_, float partialTicks, float p_177093_5_, float p_177093_6_, float p_177093_7_, float p_177093_8_, CallbackInfo ci) {
        if (OpacityCommand.isEnabled() && entity == Minecraft.getMinecraft().thePlayer && OpacityCommand.isInvisible()) {
            ci.cancel();
        }
    }
} 