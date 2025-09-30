package me.ballmc.gomod.mixins;

import me.ballmc.gomod.Main;
import me.ballmc.gomod.gui.GomodGUI;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for GuiIngameMenu (Pause Menu) to add a /gomod button.
 * This allows players to quickly access the GoMod123 settings GUI from the pause menu.
 */
@Mixin(GuiIngameMenu.class)
public class GuiIngameMenuMixin extends GuiScreen {
    
    private GuiButton gomodButton;
    
    @Inject(method = "initGui", at = @At("RETURN"))
    public void initGuiHook(CallbackInfo ci) {
        // Add the /gomod button to the pause menu
        // Position it below the "Return to Game" button
        int buttonWidth = 120;
        int buttonHeight = 20;
        int centerX = this.width / 2;
        int buttonY = this.height / 4 + 120 + 24; // Position below "Return to Game" button
        
        this.gomodButton = new GuiButton(999, // Use a high ID to avoid conflicts
            centerX - buttonWidth / 2,
            buttonY,
            buttonWidth,
            buttonHeight,
            EnumChatFormatting.DARK_GREEN + "gomod Menu");
        
        this.buttonList.add(this.gomodButton);
    }
    
    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    public void actionPerformedHook(GuiButton button, CallbackInfo ci) {
        if (button == this.gomodButton) {
            // Open the GoMod123 settings GUI
            try {
                this.mc.displayGuiScreen(new GomodGUI());
            } catch (Exception e) {
                Main.sendMessage(EnumChatFormatting.GOLD + "[gomod] " + EnumChatFormatting.RESET + 
                               EnumChatFormatting.RED + "Error opening settings: " + e.getMessage());
                System.err.println("Error opening GoMod123 settings GUI: " + e.getMessage());
                e.printStackTrace();
            }
            ci.cancel(); // Prevent the default action
        }
    }
}