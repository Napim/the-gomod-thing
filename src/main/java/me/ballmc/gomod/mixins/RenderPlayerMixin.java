package me.ballmc.gomod.mixins;

import me.ballmc.gomod.command.KillEffectCommand;
import me.ballmc.gomod.features.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Render.class)
public class RenderPlayerMixin {
    
    @ModifyVariable(method = "renderLivingLabel", at = @At("STORE"), ordinal = 0)
    private String gomod$modifyPlayerNameTag(String originalName, EntityPlayer entity, String name, double x, double y, double z, int maxDistance) {
        // Only modify if we have a valid player name and they have a kill effect
        if (name == null || name.isEmpty() || !KillEffectCommand.hasPlayerEffect(name)) {
            return originalName;
        }
        
        // Get the player's kill effect
        String killEffect = KillEffectCommand.getPlayerEffect(name);
        if (killEffect == null || killEffect.isEmpty()) {
            return originalName;
        }
        
        // Get the color for this kill effect
        EnumChatFormatting color = getKillEffectColor(killEffect);
        
        // Apply the color to the nametag
        return color + name;
    }
    
    private EnumChatFormatting getKillEffectColor(String effect) {
        switch (effect.toUpperCase()) {
            case "REGENERATION": return EnumChatFormatting.LIGHT_PURPLE;
            case "RESISTANCE": return EnumChatFormatting.GOLD;
            case "GRAVEDIGGER": return EnumChatFormatting.GREEN;
            case "RANDOM": return EnumChatFormatting.DARK_BLUE;
            case "LEVEL_UP": return EnumChatFormatting.YELLOW;
            case "LEVELUP": return EnumChatFormatting.YELLOW;
            case "RAPID_FIRE": return EnumChatFormatting.DARK_RED;
            case "FLAME": return EnumChatFormatting.DARK_RED;
            case "SPEED": return EnumChatFormatting.AQUA;
            default: return EnumChatFormatting.AQUA;
        }
    }

}
