package me.ballmc.gomod.gui;

import me.ballmc.gomod.features.KillCounter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;

/**
 * GUI for configuring TAB Stats HUD settings
 */
public class KillCounterHUDGUI extends GuiScreen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 25;
    private static final int SLIDER_HEIGHT = 20;
    
    // Toggle buttons
    private GuiButton toggleButton;
    private GuiButton resetButton;
    private GuiButton backButton;
    
    // Position inputs
    private int posXInputX, posXInputY;
    private int posYInputX, posYInputY;
    private String posXText = "";
    private String posYText = "";
    private boolean editingPosX = false;
    private boolean editingPosY = false;
    
    // Slider values
    private float scale = 1.0f;
    private int opacity = 100;
    
    // Slider positions
    private int scaleSliderX, scaleSliderY;
    private int opacitySliderX, opacitySliderY;
    
    // Slider dragging states
    private boolean isDraggingScale = false;
    private boolean isDraggingOpacity = false;
    
    // Scrolling support
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private static final int SCROLL_STEP = 15;

    @Override
    public void initGui() {
        super.initGui();
        
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int centerY = scaledResolution.getScaledHeight() / 2;
        
        // Load current values
        loadCurrentValues();
        
        // Two-column layout: left labels, right controls with proper spacing
        int rightColumnX = width / 2 + 80; // closer to center
        int rowStartY = Math.max(90, centerY - 120);
        int buttonWidth = 120; // Consistent width for all buttons

        toggleButton = new GuiButton(0, rightColumnX, rowStartY, buttonWidth, BUTTON_HEIGHT,
            (KillCounter.isEnabled() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled"));
        
        resetButton = new GuiButton(1, rightColumnX, rowStartY + SPACING, buttonWidth, BUTTON_HEIGHT,
            "Reset Counts");
        
        // Position input fields - below reset button with more space
        posXInputX = rightColumnX;
        posXInputY = rowStartY + SPACING * 2 + 20; // Extra space after "Reset Counts"
        posYInputX = rightColumnX;
        posYInputY = posXInputY + SPACING;
        
        // Back button
        backButton = new GuiButton(99, width / 2 - 100, height - 30, 200, BUTTON_HEIGHT,
                EnumChatFormatting.GRAY + "Back to Menu");
        
        // Scale slider - below position inputs with more space
        scaleSliderX = rightColumnX;
        scaleSliderY = posYInputY + SPACING * 2 + 20; // Extra space after position inputs
        
        // Opacity slider position beneath scale slider - properly centered
        opacitySliderX = rightColumnX;
        opacitySliderY = scaleSliderY + SPACING;
        
        this.buttonList.add(toggleButton);
        this.buttonList.add(resetButton);
        this.buttonList.add(backButton);
        
        // Reset scroll when opening and compute max scroll based on content size
        scrollOffset = 0;
        maxScroll = Math.max(0, (opacitySliderY + 100) - height + 50);
    }
    
    private void loadCurrentValues() {
        // Load current position (default to 10, 10 if not set)
        posXText = "10";
        posYText = "10";
        
        // Load current scale (default to 1.0)
        scale = 1.0f;
        
        // Load current opacity (default to 100)
        opacity = 100;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Draw background
        drawDefaultBackground();
        
        // Draw title
        drawCenteredString(fontRendererObj, EnumChatFormatting.GOLD + "TAB Stats HUD Settings", width / 2, 30, 0xFFFFFF);
        
        // Draw labels and controls
        int leftColumnX = width / 2 - 200;
        int rowStartY = Math.max(90, height / 2 - 120);
        
        // Toggle
        fontRendererObj.drawString(EnumChatFormatting.WHITE + "Enable TAB Stats:", leftColumnX, rowStartY + 6, 0xFFFFFF);
        
        // Reset
        fontRendererObj.drawString(EnumChatFormatting.WHITE + "Reset Kill Counts:", leftColumnX, rowStartY + SPACING + 6, 0xFFFFFF);
        
        // Position
        fontRendererObj.drawString(EnumChatFormatting.WHITE + "Position X:", leftColumnX, posXInputY + 6, 0xFFFFFF);
        fontRendererObj.drawString(EnumChatFormatting.WHITE + "Position Y:", leftColumnX, posYInputY + 6, 0xFFFFFF);
        
        // Scale
        fontRendererObj.drawString(EnumChatFormatting.WHITE + "Scale:", leftColumnX, scaleSliderY + 6, 0xFFFFFF);
        fontRendererObj.drawString(EnumChatFormatting.GRAY + String.format("%.1f", scale), scaleSliderX + 200, scaleSliderY + 6, 0xFFFFFF);
        
        // Opacity
        fontRendererObj.drawString(EnumChatFormatting.WHITE + "Opacity:", leftColumnX, opacitySliderY + 6, 0xFFFFFF);
        fontRendererObj.drawString(EnumChatFormatting.GRAY + String.valueOf(opacity) + "%", opacitySliderX + 200, opacitySliderY + 6, 0xFFFFFF);
        
        // Draw input boxes
        drawInputBox(posXInputX, posXInputY, posXText, editingPosX);
        drawInputBox(posYInputX, posYInputY, posYText, editingPosY);
        
        // Draw sliders
        drawSlider(scaleSliderX, scaleSliderY, scale, 0.5f, 3.0f, "Scale");
        drawSlider(opacitySliderX, opacitySliderY, opacity, 0, 100, "Opacity");
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    private void drawInputBox(int x, int y, String text, boolean isEditing) {
        // Draw background
        Gui.drawRect(x, y, x + 120, y + 20, isEditing ? 0xFF404040 : 0xFF202020);
        Gui.drawRect(x, y, x + 120, y + 20, 0xFF808080);
        
        // Draw text
        fontRendererObj.drawString(text + (isEditing ? "_" : ""), x + 4, y + 6, 0xFFFFFF);
    }
    
    private void drawSlider(int x, int y, float value, float min, float max, String label) {
        int sliderWidth = 200;
        int sliderHeight = 20;
        
        // Draw background
        Gui.drawRect(x, y, x + sliderWidth, y + sliderHeight, 0xFF404040);
        Gui.drawRect(x, y, x + sliderWidth, y + sliderHeight, 0xFF808080);
        
        // Calculate slider position
        float normalizedValue = (value - min) / (max - min);
        int sliderPos = (int) (normalizedValue * (sliderWidth - 10));
        
        // Draw slider handle
        Gui.drawRect(x + sliderPos, y, x + sliderPos + 10, y + sliderHeight, 0xFF606060);
        Gui.drawRect(x + sliderPos, y, x + sliderPos + 10, y + sliderHeight, 0xFF808080);
    }

    @Override
    public void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0: // Toggle
                if (KillCounter.isEnabled()) {
                    KillCounter.stop();
                    toggleButton.displayString = EnumChatFormatting.RED + "Disabled";
                } else {
                    KillCounter.start();
                    toggleButton.displayString = EnumChatFormatting.GREEN + "Enabled";
                }
                break;
            case 1: // Reset
                KillCounter.reset();
                break;
            case 99: // Back
                mc.displayGuiScreen(null);
                break;
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        // Handle input box clicks
        if (mouseX >= posXInputX && mouseX <= posXInputX + 120 && 
            mouseY >= posXInputY && mouseY <= posXInputY + 20) {
            editingPosX = true;
            editingPosY = false;
        } else if (mouseX >= posYInputX && mouseX <= posYInputX + 120 && 
                   mouseY >= posYInputY && mouseY <= posYInputY + 20) {
            editingPosY = true;
            editingPosX = false;
        } else {
            editingPosX = false;
            editingPosY = false;
        }
        
        // Handle slider clicks
        if (mouseX >= scaleSliderX && mouseX <= scaleSliderX + 200 && 
            mouseY >= scaleSliderY && mouseY <= scaleSliderY + 20) {
            isDraggingScale = true;
        } else if (mouseX >= opacitySliderX && mouseX <= opacitySliderX + 200 && 
                   mouseY >= opacitySliderY && mouseY <= opacitySliderY + 20) {
            isDraggingOpacity = true;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        isDraggingScale = false;
        isDraggingOpacity = false;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (editingPosX) {
            if (keyCode == 1) { // Escape
                editingPosX = false;
            } else if (keyCode == 28) { // Enter
                try {
                    int newX = Integer.parseInt(posXText);
                    // Save position X
                    editingPosX = false;
                } catch (NumberFormatException e) {
                    // Invalid number
                }
            } else if (keyCode == 14) { // Backspace
                if (!posXText.isEmpty()) {
                    posXText = posXText.substring(0, posXText.length() - 1);
                }
            } else if (Character.isDigit(typedChar) || typedChar == '-') {
                if (posXText.length() < 4) {
                    posXText += typedChar;
                }
            }
        } else if (editingPosY) {
            if (keyCode == 1) { // Escape
                editingPosY = false;
            } else if (keyCode == 28) { // Enter
                try {
                    int newY = Integer.parseInt(posYText);
                    // Save position Y
                    editingPosY = false;
                } catch (NumberFormatException e) {
                    // Invalid number
                }
            } else if (keyCode == 14) { // Backspace
                if (!posYText.isEmpty()) {
                    posYText = posYText.substring(0, posYText.length() - 1);
                }
            } else if (Character.isDigit(typedChar) || typedChar == '-') {
                if (posYText.length() < 4) {
                    posYText += typedChar;
                }
            }
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        
        // Handle slider dragging (simplified)
        // TODO: Implement proper slider dragging
    }
}
