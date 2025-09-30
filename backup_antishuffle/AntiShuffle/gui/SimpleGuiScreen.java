package me.ballmc.AntiShuffle.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;

/**
 * A very simple GUI screen for testing
 */
public class SimpleGuiScreen extends GuiScreen {
    private GuiButton testButton;
    
    @Override
    public void initGui() {
        this.buttonList.add(testButton = new GuiButton(0, 
            this.width / 2 - 100, 
            this.height / 2 - 10, 
            200, 
            20, 
            "Test Button"));
    }
    
    @Override
    public void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            this.mc.displayGuiScreen(null); // Close the GUI
        }
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Draw background
        this.drawDefaultBackground();
        
        // Draw title
        String title = EnumChatFormatting.YELLOW + "Simple Test GUI";
        this.drawCenteredString(this.fontRendererObj, title, this.width / 2, 20, 0xFFFFFF);
        
        // Draw buttons
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
} 