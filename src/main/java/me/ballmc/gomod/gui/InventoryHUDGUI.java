package me.ballmc.gomod.gui;

import me.ballmc.gomod.features.InventoryHUD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;

/**
 * GUI for configuring Inventory HUD settings
 */
public class InventoryHUDGUI extends GuiScreen {
    // private static final int BUTTON_WIDTH = 120; // unused
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 25;
    // private static final int SLIDER_WIDTH = 200; // unused
    private static final int SLIDER_HEIGHT = 20;
    
    // Toggle buttons (using vanilla Minecraft button styling)
    private GuiButton toggleButton;
    private GuiButton showEmptyButton;
    private GuiButton forceButton;
    private GuiButton resetPositionButton;
    private GuiButton backButton;
    
    // Sliders
    private int opacitySliderX, opacitySliderY;
    private int scaleSliderX, scaleSliderY;
    
    // Position inputs
    private int posXInputX, posXInputY;
    private int posYInputX, posYInputY;
    private String posXText = "";
    private String posYText = "";
    private boolean editingPosX = false;
    private boolean editingPosY = false;
    
    // Slider values
    private float scale = 2.0f;
    private int opacity = 100;
    // RGB fields no longer used
    
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
        // No debug message needed
        
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        // int centerX = scaledResolution.getScaledWidth() / 2; // unused
        int centerY = scaledResolution.getScaledHeight() / 2;
        
        // Load current values
        loadCurrentValues();
        
        // Two-column layout: left labels, right controls with proper spacing
        // Column X for all right-side controls (centered column look)
        int rightColumnX = width / 2 + 80; // closer to center
        int rowStartY = Math.max(90, centerY - 120);
        int buttonWidth = 120; // Consistent width for all buttons
        // int inputBoxWidth = 120; // Same width as buttons for alignment (unused here)

        toggleButton = new GuiButton(0, rightColumnX, rowStartY, buttonWidth, BUTTON_HEIGHT,
            (InventoryHUD.isEnabled() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled"));
        showEmptyButton = new GuiButton(1, rightColumnX, rowStartY + SPACING, buttonWidth, BUTTON_HEIGHT,
            (InventoryHUD.isShowEmpty() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled"));
        forceButton = new GuiButton(2, rightColumnX, rowStartY + SPACING * 2, buttonWidth, BUTTON_HEIGHT,
            (InventoryHUD.isForce() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled"));
        
        // Position input fields - below force button with more space
        posXInputX = rightColumnX;
        posXInputY = rowStartY + SPACING * 3 + 20; // Extra space after "Show while a GUI is open"
        posYInputX = rightColumnX;
        posYInputY = posXInputY + SPACING;
        
        // Reset position button - under the X/Y input boxes
        resetPositionButton = new GuiButton(3, rightColumnX, posYInputY + SPACING, buttonWidth, BUTTON_HEIGHT,
            "Reset Position");
        
        // Back button
        backButton = new GuiButton(99, width / 2 - 100, height - 30, 200, BUTTON_HEIGHT,
                EnumChatFormatting.GRAY + "Back to Menu");
        
        // Scale slider - below reset position button with more space
        scaleSliderX = rightColumnX;
        scaleSliderY = posYInputY + SPACING * 2 + 20; // Extra space after "Reset Position"
        
        // Opacity slider position beneath scale slider - properly centered
        opacitySliderX = rightColumnX;
        opacitySliderY = scaleSliderY + SPACING;
        
        this.buttonList.add(toggleButton);
        this.buttonList.add(showEmptyButton);
        this.buttonList.add(forceButton);
        this.buttonList.add(resetPositionButton);
        this.buttonList.add(backButton);

        // Reset scroll when opening and compute max scroll based on content size
        scrollOffset = 0;
        computeMaxScroll();
    }
    

    private void loadCurrentValues() {
        scale = InventoryHUD.getScale();
        opacity = InventoryHUD.getBackgroundOpacity();
        // Background color components no longer used in this GUI
        posXText = String.valueOf(InventoryHUD.getCustomX());
        posYText = String.valueOf(InventoryHUD.getCustomY());
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Draw semi-transparent background
        drawDefaultBackground();
        
        // Draw title
        String title = EnumChatFormatting.GOLD + "Inventory HUD Settings";
        int titleWidth = fontRendererObj.getStringWidth(title);
        fontRendererObj.drawString(title, width / 2 - titleWidth / 2, 30, 0xFFFFFF);
        
        // Left column labels (mimic the reference layout)
        int labelX = width / 2 - 200; // Further left for better visual balance
        int baseRowStartY = Math.max(90, height / 2 - 120);
        int rowStartY = baseRowStartY - scrollOffset;

        // Update button positions based on scroll so they remain interactive
        toggleButton.yPosition = rowStartY;
        showEmptyButton.yPosition = rowStartY + SPACING;
        forceButton.yPosition = rowStartY + SPACING * 2;
        resetPositionButton.yPosition = (posYInputY - scrollOffset) + SPACING;

        fontRendererObj.drawString("Toggle mod", labelX, rowStartY + 6, 0xFFFFFF);
        fontRendererObj.drawString("Show when inventory is empty", labelX, rowStartY + SPACING + 6, 0xFFFFFF);
        fontRendererObj.drawString("Show while a GUI is open", labelX, rowStartY + SPACING * 2 + 6, 0xFFFFFF);
        fontRendererObj.drawString("Position X", labelX, (posXInputY - scrollOffset) + 4, 0xFFFFFF);
        fontRendererObj.drawString("Position Y", labelX, (posYInputY - scrollOffset) + 4, 0xFFFFFF);
        fontRendererObj.drawString("Reset Position", labelX, (posYInputY - scrollOffset) + SPACING + 6, 0xFFFFFF);
        fontRendererObj.drawString("Scale (1.0-4.0)", labelX, (scaleSliderY - scrollOffset) + 4, 0xFFFFFF);
        fontRendererObj.drawString("Background Opacity (0-255)", labelX, (opacitySliderY - scrollOffset) + 4, 0xFFFFFF);
        
        // Draw position input boxes
        int inputBoxWidth = 120; // Same width as buttons for alignment
        int inputBoxHeight = 20;
        
        // X position input
        int xBorderColor = 0xFFFFFFFF;
        int posXVisibleY = posXInputY - scrollOffset;
        int posYVisibleY = posYInputY - scrollOffset;
        Gui.drawRect(posXInputX, posXVisibleY, posXInputX + inputBoxWidth, posXVisibleY + inputBoxHeight, 0xFF000000);
        // white border
        Gui.drawRect(posXInputX - 1, posXVisibleY - 1, posXInputX + inputBoxWidth + 1, posXVisibleY, xBorderColor);
        Gui.drawRect(posXInputX - 1, posXVisibleY + inputBoxHeight, posXInputX + inputBoxWidth + 1, posXVisibleY + inputBoxHeight + 1, xBorderColor);
        Gui.drawRect(posXInputX - 1, posXVisibleY, posXInputX, posXVisibleY + inputBoxHeight, xBorderColor);
        Gui.drawRect(posXInputX + inputBoxWidth, posXVisibleY, posXInputX + inputBoxWidth + 1, posXVisibleY + inputBoxHeight, xBorderColor);
        fontRendererObj.drawString(posXText, posXInputX + 5, posXVisibleY + 6, 0xFFFFFF);
        // caret for X
        if (editingPosX && (System.currentTimeMillis() / 500) % 2 == 0) {
            int caretX = posXInputX + 5 + fontRendererObj.getStringWidth(posXText);
            Gui.drawRect(caretX, posXVisibleY + 4, caretX + 1, posXVisibleY + inputBoxHeight - 4, 0xFFFFFFFF);
        }
        
        // Y position input
        Gui.drawRect(posYInputX, posYVisibleY, posYInputX + inputBoxWidth, posYVisibleY + inputBoxHeight, 0xFF000000);
        Gui.drawRect(posYInputX - 1, posYVisibleY - 1, posYInputX + inputBoxWidth + 1, posYVisibleY, xBorderColor);
        Gui.drawRect(posYInputX - 1, posYVisibleY + inputBoxHeight, posYInputX + inputBoxWidth + 1, posYVisibleY + inputBoxHeight + 1, xBorderColor);
        Gui.drawRect(posYInputX - 1, posYVisibleY, posYInputX, posYVisibleY + inputBoxHeight, xBorderColor);
        Gui.drawRect(posYInputX + inputBoxWidth, posYVisibleY, posYInputX + inputBoxWidth + 1, posYVisibleY + inputBoxHeight, xBorderColor);
        fontRendererObj.drawString(posYText, posYInputX + 5, posYVisibleY + 6, 0xFFFFFF);
        // caret for Y
        if (editingPosY && (System.currentTimeMillis() / 500) % 2 == 0) {
            int caretX2 = posYInputX + 5 + fontRendererObj.getStringWidth(posYText);
            Gui.drawRect(caretX2, posYVisibleY + 4, caretX2 + 1, posYVisibleY + inputBoxHeight - 4, 0xFFFFFFFF);
        }

        // Draw sliders with better styling - use same width as buttons for centering
        drawScaleSlider(scaleSliderX, scaleSliderY - scrollOffset, 120, SLIDER_HEIGHT, scale, 0xFF4CAF50,
            "Scale: " + String.format("%.1f", scale));
        drawSlider(opacitySliderX, opacitySliderY - scrollOffset, 120, SLIDER_HEIGHT, opacity, 0xFF2196F3,
            "Opacity: " + opacity);
        
        
        // No footer status needed
        
        // Recompute max scroll in case window resized
        computeMaxScroll();

        // Draw a scrollbar indicator if needed
        if (maxScroll > 0) {
            int trackX = this.width - 10; // right side padding
            int trackWidth = 4;

            // Define scrollable region bounds
            int contentTop = baseRowStartY;
            int contentBottom = Math.max(
                    Math.max(posYInputY + SPACING + BUTTON_HEIGHT, // reset button area
                            opacitySliderY + SLIDER_HEIGHT),
                    scaleSliderY + SLIDER_HEIGHT
            );

            int visibleTop = contentTop;
            int visibleBottom = this.height - 30; // same as computeMaxScroll padding

            int trackTop = Math.max(visibleTop, 30); // avoid title overlap
            int trackBottom = Math.max(trackTop + 1, visibleBottom);
            int trackHeight = trackBottom - trackTop;

            int totalContentHeight = Math.max(1, contentBottom - contentTop);
            int visibleHeight = Math.max(1, visibleBottom - visibleTop);

            // Thumb size proportional to visible area, minimum size for usability
            int thumbHeight = Math.max(20, (int) (trackHeight * (visibleHeight / (float) totalContentHeight)));
            thumbHeight = Math.min(thumbHeight, trackHeight);

            // Thumb position based on scrollOffset
            int maxThumbTravel = trackHeight - thumbHeight;
            int thumbY = trackTop + (maxScroll == 0 ? 0 : (int) (maxThumbTravel * (scrollOffset / (float) maxScroll)));

            // Track background
            Gui.drawRect(trackX, trackTop, trackX + trackWidth, trackBottom, 0x60000000);
            // Thumb (lighter)
            Gui.drawRect(trackX, thumbY, trackX + trackWidth, thumbY + thumbHeight, 0x90FFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    private void drawScaleSlider(int x, int y, int width, int height, float value, int color, String label) {
        // Draw slider background with gradient effect (darker gray)
        Gui.drawRect(x, y, x + width, y + height, 0xFF1A1A1A);
        
        // Add subtle inner border for depth
        Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF0F0F0F);

        // Draw slider fill with gradient effect - scale range 1.0 to 4.0
        float normalizedValue = (value - 1.0f) / 3.0f; // Normalize 1.0-4.0 to 0.0-1.0
        int fillWidth = Math.max(0, Math.min(width - 2, (int) (normalizedValue * (width - 2))));
        
        // Main fill color
        Gui.drawRect(x + 1, y + 1, x + 1 + fillWidth, y + height - 1, color);
        
        // Add highlight on top of fill for 3D effect
        if (fillWidth > 0) {
            Gui.drawRect(x + 1, y + 1, x + 1 + fillWidth, y + 2, 0x40FFFFFF); // Top highlight
        }

        // Modern border with rounded corners effect
        // Top border
        Gui.drawRect(x, y, x + width, y + 1, 0xFF404040);
        // Bottom border  
        Gui.drawRect(x, y + height - 1, x + width, y + height, 0xFF202020);
        // Left border
        Gui.drawRect(x, y, x + 1, y + height, 0xFF404040);
        // Right border
        Gui.drawRect(x + width - 1, y, x + width, y + height, 0xFF202020);
        
        // Draw label inside the bar with better positioning
        if (label != null && !label.isEmpty()) {
            int textX = x + 8;
            int textY = y + (height - 8) / 2; // Center vertically in the bar
            // Add text shadow for better readability
            fontRendererObj.drawString(label, textX + 1, textY + 1, 0x80000000);
            fontRendererObj.drawString(label, textX, textY, 0xFFFFFFFF);
        }
    }
    
    private void drawSlider(int x, int y, int width, int height, int value, int color, String label) {
        // Draw slider background with gradient effect (darker gray)
        Gui.drawRect(x, y, x + width, y + height, 0xFF1A1A1A);
        
        // Add subtle inner border for depth
        Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF0F0F0F);

        // Draw slider fill with gradient effect
        int maxValue = (label != null && label.contains("Scale")) ? 3 : 255;
        int fillWidth = Math.max(0, Math.min(width - 2, (int) ((value / (float) maxValue) * (width - 2))));
        
        // Main fill color
        Gui.drawRect(x + 1, y + 1, x + 1 + fillWidth, y + height - 1, color);
        
        // Add highlight on top of fill for 3D effect
        if (fillWidth > 0) {
            Gui.drawRect(x + 1, y + 1, x + 1 + fillWidth, y + 2, 0x40FFFFFF); // Top highlight
        }

        // Modern border with rounded corners effect
        // Top border
        Gui.drawRect(x, y, x + width, y + 1, 0xFF404040);
        // Bottom border  
        Gui.drawRect(x, y + height - 1, x + width, y + height, 0xFF202020);
        // Left border
        Gui.drawRect(x, y, x + 1, y + height, 0xFF404040);
        // Right border
        Gui.drawRect(x + width - 1, y, x + width, y + height, 0xFF202020);
        
        // Draw label inside the bar with better positioning
        if (label != null && !label.isEmpty()) {
            int textX = x + 8;
            int textY = y + (height - 8) / 2; // Center vertically in the bar
            // Add text shadow for better readability
            fontRendererObj.drawString(label, textX + 1, textY + 1, 0x80000000);
            fontRendererObj.drawString(label, textX, textY, 0xFFFFFFFF);
        }
    }
    
    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        // Check position input boxes
        int inputBoxWidth = 120;
        int inputBoxHeight = 20;
        
        int posXVisibleY = posXInputY - scrollOffset;
        int posYVisibleY = posYInputY - scrollOffset;

        if (mouseX >= posXInputX && mouseX <= posXInputX + inputBoxWidth &&
            mouseY >= posXVisibleY && mouseY <= posXVisibleY + inputBoxHeight) {
            editingPosX = true;
            editingPosY = false;
            return;
        }
        
        if (mouseX >= posYInputX && mouseX <= posYInputX + inputBoxWidth &&
            mouseY >= posYVisibleY && mouseY <= posYVisibleY + inputBoxHeight) {
            editingPosY = true;
            editingPosX = false;
            return;
        }
        
        // If clicking elsewhere, stop editing
        editingPosX = false;
        editingPosY = false;
        
        // Check sliders
        if (mouseX >= scaleSliderX && mouseX <= scaleSliderX + 120 &&
            mouseY >= (scaleSliderY - scrollOffset) && mouseY <= (scaleSliderY - scrollOffset) + SLIDER_HEIGHT) {
            isDraggingScale = true;
            updateScaleFromMouse(mouseX);
        }
        
        if (mouseX >= opacitySliderX && mouseX <= opacitySliderX + 120 &&
            mouseY >= (opacitySliderY - scrollOffset) && mouseY <= (opacitySliderY - scrollOffset) + SLIDER_HEIGHT) {
            isDraggingOpacity = true;
            updateOpacityFromMouse(mouseX);
        }
    }
    
    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        
        // no scale dragging anymore
        if (isDraggingScale) {
            updateScaleFromMouse(mouseX);
        }

        if (isDraggingOpacity) {
            updateOpacityFromMouse(mouseX);
        }
        
    }
    
    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        isDraggingScale = false;
        isDraggingOpacity = false;
    }
    
    private void updateScaleFromMouse(int mouseX) {
        // Convert mouse position to scale value (1.0 to 4.0)
        float normalizedPos = (mouseX - scaleSliderX) / 120.0f; // 120 is slider width
        normalizedPos = Math.max(0.0f, Math.min(1.0f, normalizedPos)); // Clamp to 0-1
        float newScale = 1.0f + (normalizedPos * 3.0f); // Convert to 1.0-4.0 range
        
        if (Math.abs(newScale - scale) > 0.05f) { // Only update if change is significant
            scale = newScale;
            InventoryHUD.setScale(scale);
        }
    }
    
    private void updateOpacityFromMouse(int mouseX) {
        int newOpacity = Math.max(0, Math.min(255, (int) ((mouseX - opacitySliderX) * 255.0f / 120)));
        if (newOpacity != opacity) {
            opacity = newOpacity;
            InventoryHUD.setBackgroundOpacity(opacity);
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int dWheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (dWheel != 0 && maxScroll > 0) {
            if (dWheel < 0) {
                scrollOffset = Math.min(scrollOffset + SCROLL_STEP, maxScroll);
            } else if (dWheel > 0) {
                scrollOffset = Math.max(scrollOffset - SCROLL_STEP, 0);
            }
        }
    }

    private void computeMaxScroll() {
        // Determine the lowest Y element (content bottom)
        int contentBottom = Math.max(
                Math.max(posYInputY + SPACING + BUTTON_HEIGHT, // reset button area
                        opacitySliderY + SLIDER_HEIGHT),
                scaleSliderY + SLIDER_HEIGHT
        );
        int visibleBottom = this.height - 30; // leave some padding at bottom
        maxScroll = Math.max(0, contentBottom - visibleBottom);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
    }
    
    // No RGB slider handlers anymore
    
    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (editingPosX) {
            if (keyCode == 1) { // ESC
                this.mc.displayGuiScreen(null); // Close the GUI
                return;
            } else if (keyCode == 28) { // ENTER
                try {
                    int x = Integer.parseInt(posXText);
                    int y = Integer.parseInt(posYText);
                    InventoryHUD.setCustomPosition(x, y);
                    // Don't stop editing - keep caret visible for continued typing
                } catch (NumberFormatException e) {
                    // No debug message needed
                }
            } else if (keyCode == 14) { // BACKSPACE
                if (posXText.length() > 0) {
                    posXText = posXText.substring(0, posXText.length() - 1);
                }
            } else if (Character.isDigit(typedChar) && posXText.length() < 4) {
                posXText += typedChar;
            }
            return;
        }
        
        if (editingPosY) {
            if (keyCode == 1) { // ESC
                this.mc.displayGuiScreen(null); // Close the GUI
                return;
            } else if (keyCode == 28) { // ENTER
                try {
                    int x = Integer.parseInt(posXText);
                    int y = Integer.parseInt(posYText);
                    InventoryHUD.setCustomPosition(x, y);
                    // Don't stop editing - keep caret visible for continued typing
                } catch (NumberFormatException e) {
                    // No debug message needed
                }
            } else if (keyCode == 14) { // BACKSPACE
                if (posYText.length() > 0) {
                    posYText = posYText.substring(0, posYText.length() - 1);
                }
            } else if (Character.isDigit(typedChar) && posYText.length() < 4) {
                posYText += typedChar;
            }
            return;
        }

        // No scale input handling needed - now using slider
        
        if (keyCode == 1) { // ESC
            this.mc.displayGuiScreen(null);
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }
    
    @Override
    public void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0: // Toggle
                InventoryHUD.toggle();
                toggleButton.displayString = (InventoryHUD.isEnabled() ? 
                    EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled");
                break;
                
            case 1: // Show Empty
                InventoryHUD.toggleShowEmpty();
                showEmptyButton.displayString = (InventoryHUD.isShowEmpty() ? 
                    EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled");
                break;
                
            case 2: // Force Mode
                InventoryHUD.toggleForce();
                forceButton.displayString = (InventoryHUD.isForce() ? 
                    EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled");
                break;
                
            case 3: // Reset Position
                InventoryHUD.resetPosition();
                posXText = String.valueOf(InventoryHUD.getCustomX());
                posYText = String.valueOf(InventoryHUD.getCustomY());
                break;
                
            case 99: // Back button
                this.mc.displayGuiScreen(new GomodGUI());
                break;
                
        }
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
