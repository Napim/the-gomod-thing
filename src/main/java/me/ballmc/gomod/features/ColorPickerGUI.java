package me.ballmc.gomod.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import static me.ballmc.gomod.Main.sendMessage;
import me.ballmc.gomod.Main;

/**
 * Simple RGB color picker GUI for selecting inventory HUD background color
 */
public class ColorPickerGUI extends GuiScreen {
    private static final int SLIDER_WIDTH = 200;
    private static final int SLIDER_HEIGHT = 20;
    private static final int SPACING = 30;
    
    private int red = 0;
    private int green = 0;
    private int blue = 0;
    private boolean isDraggingRed = false;
    private boolean isDraggingGreen = false;
    private boolean isDraggingBlue = false;
    
    private int redSliderX, redSliderY;
    private int greenSliderX, greenSliderY;
    private int blueSliderX, blueSliderY;
    
    @Override
    public void initGui() {
        super.initGui();
        
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int centerX = scaledResolution.getScaledWidth() / 2;
        int centerY = scaledResolution.getScaledHeight() / 2;
        
        // Position sliders in the center
        redSliderX = centerX - SLIDER_WIDTH / 2;
        redSliderY = centerY - 50;
        greenSliderX = centerX - SLIDER_WIDTH / 2;
        greenSliderY = centerY - 20;
        blueSliderX = centerX - SLIDER_WIDTH / 2;
        blueSliderY = centerY + 10;
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Draw semi-transparent background
        drawDefaultBackground();
        
        // Draw title
        String title = EnumChatFormatting.GOLD + "Color Picker";
        int titleWidth = fontRendererObj.getStringWidth(title);
        fontRendererObj.drawString(title, width / 2 - titleWidth / 2, redSliderY - 30, 0xFFFFFF);
        
        // Draw red slider
        drawSlider(redSliderX, redSliderY, SLIDER_WIDTH, SLIDER_HEIGHT, red, 0xFF0000, "Red: " + red);
        
        // Draw green slider
        drawSlider(greenSliderX, greenSliderY, SLIDER_WIDTH, SLIDER_HEIGHT, green, 0x00FF00, "Green: " + green);
        
        // Draw blue slider
        drawSlider(blueSliderX, blueSliderY, SLIDER_WIDTH, SLIDER_HEIGHT, blue, 0x0000FF, "Blue: " + blue);
        
        // Draw color preview
        int previewX = blueSliderX + SLIDER_WIDTH + 20;
        int previewY = redSliderY;
        int previewSize = 60;
        
        int selectedColor = (red << 16) | (green << 8) | blue;
        Gui.drawRect(previewX, previewY, previewX + previewSize, previewY + previewSize, selectedColor);
        Gui.drawRect(previewX - 1, previewY - 1, previewX + previewSize + 1, previewY + previewSize + 1, 0xFFFFFFFF);
        
        // Draw instructions
        String instructions = EnumChatFormatting.YELLOW + "Click and drag sliders to adjust color";
        int instWidth = fontRendererObj.getStringWidth(instructions);
        fontRendererObj.drawString(instructions, width / 2 - instWidth / 2, blueSliderY + 40, 0xFFFFFF);
        
        String controls = EnumChatFormatting.YELLOW + "Press ENTER to apply, ESC to cancel";
        int ctrlWidth = fontRendererObj.getStringWidth(controls);
        fontRendererObj.drawString(controls, width / 2 - ctrlWidth / 2, blueSliderY + 55, 0xFFFFFF);
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    private void drawSlider(int x, int y, int width, int height, int value, int color, String label) {
        // Draw slider background
        Gui.drawRect(x, y, x + width, y + height, 0x88000000);
        
        // Draw slider fill
        int fillWidth = (int) ((value / 255.0f) * width);
        Gui.drawRect(x, y, x + fillWidth, y + height, color);
        
        // Draw slider border
        Gui.drawRect(x - 1, y - 1, x + width + 1, y + height + 1, 0xFFFFFFFF);
        
        // Draw label
        fontRendererObj.drawString(label, x, y - 12, 0xFFFFFF);
    }
    
    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        // Check red slider
        if (mouseX >= redSliderX && mouseX <= redSliderX + SLIDER_WIDTH &&
            mouseY >= redSliderY && mouseY <= redSliderY + SLIDER_HEIGHT) {
            isDraggingRed = true;
            updateRedFromMouse(mouseX);
        }
        
        // Check green slider
        if (mouseX >= greenSliderX && mouseX <= greenSliderX + SLIDER_WIDTH &&
            mouseY >= greenSliderY && mouseY <= greenSliderY + SLIDER_HEIGHT) {
            isDraggingGreen = true;
            updateGreenFromMouse(mouseX);
        }
        
        // Check blue slider
        if (mouseX >= blueSliderX && mouseX <= blueSliderX + SLIDER_WIDTH &&
            mouseY >= blueSliderY && mouseY <= blueSliderY + SLIDER_HEIGHT) {
            isDraggingBlue = true;
            updateBlueFromMouse(mouseX);
        }
    }
    
    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        
        if (isDraggingRed) {
            updateRedFromMouse(mouseX);
        }
        
        if (isDraggingGreen) {
            updateGreenFromMouse(mouseX);
        }
        
        if (isDraggingBlue) {
            updateBlueFromMouse(mouseX);
        }
    }
    
    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        isDraggingRed = false;
        isDraggingGreen = false;
        isDraggingBlue = false;
    }
    
    private void updateRedFromMouse(int mouseX) {
        red = Math.max(0, Math.min(255, (int) ((mouseX - redSliderX) * 255.0f / SLIDER_WIDTH)));
    }
    
    private void updateGreenFromMouse(int mouseX) {
        green = Math.max(0, Math.min(255, (int) ((mouseX - greenSliderX) * 255.0f / SLIDER_WIDTH)));
    }
    
    private void updateBlueFromMouse(int mouseX) {
        blue = Math.max(0, Math.min(255, (int) ((mouseX - blueSliderX) * 255.0f / SLIDER_WIDTH)));
    }
    
    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) { // ESC
            this.mc.displayGuiScreen(null);
        } else if (keyCode == 28) { // ENTER
            applySelectedColor();
            this.mc.displayGuiScreen(null);
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }
    
    private void applySelectedColor() {
        InventoryHUD.setBackgroundColor(red, green, blue);
        sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN +
                   "Background color set to RGB(" + red + ", " + green + ", " + blue + ")");
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
