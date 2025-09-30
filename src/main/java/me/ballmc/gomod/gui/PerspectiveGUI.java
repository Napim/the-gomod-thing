package me.ballmc.gomod.gui;

import me.ballmc.gomod.features.PerspectiveDistance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

public class PerspectiveGUI extends GuiScreen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 25;

    private GuiButton toggleButton;
    private GuiButton resetOnExitButton;
    private GuiButton requireHoldButton;
    private GuiButton smoothZoomButton;
    private GuiButton lockScrollButton;
    private GuiButton zoomKeyButton;
    private GuiButton setKeyButton;
    private GuiButton backButton;

    // Slider for scrolling speed
    private SpeedSlider speedSlider;
    
    // Slider for smooth speed
    private SmoothSlider smoothSlider;
    
    // FOV slider
    private PerspectiveFOVSlider fovSlider;

    // no temp vars needed
    private int awaitingKey = -1;
    private int awaitingZoomKey = -1;
    private GuiButton selectedButton = null;

    // Scroll support
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private static final int SCROLL_STEP = 12;

    @Override
    public void initGui() {
        super.initGui();
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int centerY = sr.getScaledHeight() / 2;

        // Load current values (no longer needed for sliders)

        int rightColumnX = width / 2 + 80;
        int rowStartY = centerY - 100;
        int buttonWidth = 120;

        toggleButton = new GuiButton(0, rightColumnX, rowStartY, buttonWidth, BUTTON_HEIGHT,
                (PerspectiveDistance.isEnabled() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled"));
        resetOnExitButton = new GuiButton(2, rightColumnX, rowStartY + SPACING, buttonWidth, BUTTON_HEIGHT,
                (PerspectiveDistance.isResetOnExit() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled"));
        requireHoldButton = new GuiButton(4, rightColumnX, rowStartY + SPACING * 2, buttonWidth, BUTTON_HEIGHT,
                (PerspectiveDistance.isRequireHold() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled"));
        smoothZoomButton = new GuiButton(5, rightColumnX, rowStartY + SPACING * 3, buttonWidth, BUTTON_HEIGHT,
                (PerspectiveDistance.isSmoothZoom() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled"));
        lockScrollButton = new GuiButton(7, rightColumnX, rowStartY + SPACING * 9, buttonWidth, BUTTON_HEIGHT,
                (PerspectiveDistance.isLockScroll() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled"));
        // Lock scroll right after Smooth zoom transitions
        lockScrollButton = new GuiButton(7, rightColumnX, rowStartY + SPACING * 4, buttonWidth, BUTTON_HEIGHT,
                (PerspectiveDistance.isLockScroll() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled"));
        
        setKeyButton = new GuiButton(3, rightColumnX, rowStartY + SPACING * 5, buttonWidth, BUTTON_HEIGHT,
                formatKey(PerspectiveDistance.getResetKeyCode()));
        zoomKeyButton = new GuiButton(6, rightColumnX, rowStartY + SPACING * 8, buttonWidth, BUTTON_HEIGHT,
                formatKey(PerspectiveDistance.getZoomKeyCode()));
        // Hide the button initially if the setting is disabled
        if (!PerspectiveDistance.isRequireHold()) {
            zoomKeyButton.visible = false;
        }

        // Create speed slider (Minecraft-style)
        speedSlider = new SpeedSlider(101, rightColumnX, rowStartY + SPACING * 6, 
                "Speed", 0.20f, 3.00f, PerspectiveDistance.getScrollStep());
        
        // Create smooth speed slider (Minecraft-style) - position will be updated dynamically
        smoothSlider = new SmoothSlider(102, rightColumnX, rowStartY + (PerspectiveDistance.isRequireHold() ? SPACING * 9 : SPACING * 8), 
                "Smooth", 0.01f, 1.0f, PerspectiveDistance.getSmoothSpeed());
        smoothSlider.visible = PerspectiveDistance.isSmoothZoom();
        
        // Create FOV slider using custom slider (positioned after scrolling speed)
        fovSlider = new PerspectiveFOVSlider(100, rightColumnX, rowStartY + SPACING * 7, 
                "FOV", 30.0f, 110.0f, PerspectiveDistance.getFOV());

        // Back button
        backButton = new GuiButton(99, width / 2 - 100, height - 30, 200, BUTTON_HEIGHT,
                EnumChatFormatting.GRAY + "Back to Menu");

        this.buttonList.add(toggleButton);
        this.buttonList.add(resetOnExitButton);
        this.buttonList.add(requireHoldButton);
        this.buttonList.add(smoothZoomButton);
        this.buttonList.add(lockScrollButton);
        this.buttonList.add(setKeyButton);
        this.buttonList.add(zoomKeyButton);
        this.buttonList.add(speedSlider);
        this.buttonList.add(fovSlider);
        this.buttonList.add(smoothSlider);
        this.buttonList.add(backButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        String title = EnumChatFormatting.GOLD + "Perspective Settings";
        int titleWidth = fontRendererObj.getStringWidth(title);
        fontRendererObj.drawString(title, width / 2 - titleWidth / 2, 30, 0xFFFFFF);

        int labelX = width / 2 - 200;
        int rowStartY = height / 2 - 100;

        // Apply scroll offset to labels
        int y0 = rowStartY - scrollOffset;
        fontRendererObj.drawString("Toggle mod", labelX, y0 + 6, 0xFFFFFF);
        fontRendererObj.drawString("Reset zoom when quitting perspective", labelX, y0 + SPACING + 6, 0xFFFFFF);
        fontRendererObj.drawString("Zoom only while holding the zoom key", labelX, y0 + SPACING * 2 + 6, 0xFFFFFF);
        fontRendererObj.drawString("Smooth zoom transitions", labelX, y0 + SPACING * 3 + 6, 0xFFFFFF);
        fontRendererObj.drawString("Reset zoom", labelX, y0 + SPACING * 5 + 6, 0xFFFFFF);
        fontRendererObj.drawString("Scrolling speed", labelX, y0 + SPACING * 6 + 6, 0xFFFFFF);
        
        // Only show zoom key label if "zoom only while holding" is enabled (row 8)
        if (PerspectiveDistance.isRequireHold()) {
            fontRendererObj.drawString("Zoom key", labelX, y0 + SPACING * 8 + 6, 0xFFFFFF);
        }
        
        // Smooth zoom label and dynamic placement
        int smoothLabelY = y0 + SPACING * 8 + 6; // base position below FOV/zoom key
        if (PerspectiveDistance.isRequireHold()) {
            smoothLabelY = y0 + SPACING * 9 + 6; // account for visible zoom key row
        }
        if (PerspectiveDistance.isSmoothZoom()) {
            fontRendererObj.drawString("Smooth zoom speed", labelX, smoothLabelY, 0xFFFFFF);
        }

        fontRendererObj.drawString("Perspective FOV", labelX, y0 + SPACING * 7 + 6, 0xFFFFFF);

        // Lock scroll label is directly after Smooth zoom transitions
        int lockLabelRow = 4; // immediately after Smooth zoom transitions row
        fontRendererObj.drawString("Lock hotbar scroll while in perspective", labelX, y0 + SPACING * lockLabelRow + 6, 0xFFFFFF);

        // Update smooth slider position dynamically
        if (PerspectiveDistance.isSmoothZoom()) {
            int smoothSliderY = PerspectiveDistance.isRequireHold() ? y0 + SPACING * 9 : y0 + SPACING * 8;
            smoothSlider.yPosition = smoothSliderY;
        }

        // Update positions of buttons/sliders with scroll
        if (toggleButton != null) toggleButton.yPosition = y0;
        if (resetOnExitButton != null) resetOnExitButton.yPosition = y0 + SPACING;
        if (requireHoldButton != null) requireHoldButton.yPosition = y0 + SPACING * 2;
        if (smoothZoomButton != null) smoothZoomButton.yPosition = y0 + SPACING * 3;
        if (setKeyButton != null) setKeyButton.yPosition = y0 + SPACING * 5;
        if (speedSlider != null) speedSlider.yPosition = y0 + SPACING * 6;
        if (fovSlider != null) fovSlider.yPosition = y0 + SPACING * 7;
        if (PerspectiveDistance.isRequireHold() && zoomKeyButton != null) {
            zoomKeyButton.yPosition = y0 + SPACING * 8;
        }
        // Lock scroll is the row immediately after Smooth zoom transitions
        if (lockScrollButton != null) lockScrollButton.yPosition = y0 + SPACING * 4;

        // Draw scrollbar when content overflows
        drawScrollbar();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }




    
    
    
    

    @Override
    public void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                PerspectiveDistance.setEnabled(!PerspectiveDistance.isEnabled());
                toggleButton.displayString = (PerspectiveDistance.isEnabled() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled");
                break;
            case 2:
                PerspectiveDistance.setResetOnExit(!PerspectiveDistance.isResetOnExit());
                resetOnExitButton.displayString = (PerspectiveDistance.isResetOnExit() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled");
                break;
            case 3:
                awaitingKey = 1;
                setKeyButton.displayString = EnumChatFormatting.YELLOW + "Press a key...";
                break;
            case 4:
                PerspectiveDistance.setRequireHold(!PerspectiveDistance.isRequireHold());
                requireHoldButton.displayString = (PerspectiveDistance.isRequireHold() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled");
                // Show/hide zoom key button based on setting
                zoomKeyButton.visible = PerspectiveDistance.isRequireHold();
                break;
            case 5:
                PerspectiveDistance.setSmoothZoom(!PerspectiveDistance.isSmoothZoom());
                smoothZoomButton.displayString = (PerspectiveDistance.isSmoothZoom() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled");
                smoothSlider.visible = PerspectiveDistance.isSmoothZoom();
                break;
            case 6:
                awaitingZoomKey = 1;
                zoomKeyButton.displayString = EnumChatFormatting.YELLOW + "Press a key...";
                break;
            case 7:
                PerspectiveDistance.setLockScroll(!PerspectiveDistance.isLockScroll());
                lockScrollButton.displayString = (PerspectiveDistance.isLockScroll() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled");
                break;
            case 100:
                // FOV slider changed
                if (button instanceof PerspectiveFOVSlider) {
                    PerspectiveFOVSlider slider = (PerspectiveFOVSlider) button;
                    PerspectiveDistance.setFOV(slider.getValue());
                }
                break;
            case 101:
                // Speed slider changed
                if (button instanceof SpeedSlider) {
                    SpeedSlider slider = (SpeedSlider) button;
                    PerspectiveDistance.setScrollStep(slider.getValue());
                }
                break;
            case 102:
                // Smooth slider changed
                if (button instanceof SmoothSlider) {
                    SmoothSlider slider = (SmoothSlider) button;
                    PerspectiveDistance.setSmoothSpeed(slider.getValue());
                }
                break;
            case 99: // Back button
                this.mc.displayGuiScreen(new GomodGUI());
                break;
        }
    }
    
    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        // Handle slider clicks
        for (GuiButton button : this.buttonList) {
            if (button instanceof SpeedSlider || button instanceof SmoothSlider || button instanceof PerspectiveFOVSlider) {
                if (button.mousePressed(Minecraft.getMinecraft(), mouseX, mouseY)) {
                    this.selectedButton = button;
                    break;
                }
            }
        }
    }
    
    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        
        // Handle slider dragging
        if (this.selectedButton instanceof SpeedSlider || this.selectedButton instanceof SmoothSlider || this.selectedButton instanceof PerspectiveFOVSlider) {
            this.selectedButton.mouseDragged(Minecraft.getMinecraft(), mouseX, mouseY);
            
            // Save the value immediately during dragging
            if (this.selectedButton instanceof SpeedSlider) {
                SpeedSlider slider = (SpeedSlider) this.selectedButton;
                PerspectiveDistance.setScrollStep(slider.getValue());
            } else if (this.selectedButton instanceof SmoothSlider) {
                SmoothSlider slider = (SmoothSlider) this.selectedButton;
                PerspectiveDistance.setSmoothSpeed(slider.getValue());
            } else if (this.selectedButton instanceof PerspectiveFOVSlider) {
                PerspectiveFOVSlider slider = (PerspectiveFOVSlider) this.selectedButton;
                PerspectiveDistance.setFOV(slider.getValue());
            }
        }
    }
    
    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        
        // Handle slider release
        if (this.selectedButton instanceof SpeedSlider || this.selectedButton instanceof SmoothSlider || this.selectedButton instanceof PerspectiveFOVSlider) {
            this.selectedButton.mouseReleased(mouseX, mouseY);
            
            // Save the final value when releasing
            if (this.selectedButton instanceof SpeedSlider) {
                SpeedSlider slider = (SpeedSlider) this.selectedButton;
                PerspectiveDistance.setScrollStep(slider.getValue());
            } else if (this.selectedButton instanceof SmoothSlider) {
                SmoothSlider slider = (SmoothSlider) this.selectedButton;
                PerspectiveDistance.setSmoothSpeed(slider.getValue());
            } else if (this.selectedButton instanceof PerspectiveFOVSlider) {
                PerspectiveFOVSlider slider = (PerspectiveFOVSlider) this.selectedButton;
                PerspectiveDistance.setFOV(slider.getValue());
            }
        }
        this.selectedButton = null;
    }

    private void computeMaxScroll() {
        int rowStartY = height / 2 - 100;
        int contentTop = rowStartY;
        // Recompute rows based on new ordering (lock at row 4)
        int rows = 10; // generous bound to ensure scroll area fits after repositioning
        int lastY = contentTop + SPACING * rows;
        int contentBottom = lastY + BUTTON_HEIGHT;
        int visibleBottom = height - 40; // leave space for back button
        maxScroll = Math.max(0, contentBottom - visibleBottom);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    private void drawScrollbar() {
        computeMaxScroll();
        if (maxScroll <= 0) return;
        int trackX = this.width - 10;
        int trackWidth = 4;
        int visibleTop = height / 2 - 100;
        int visibleBottom = this.height - 40;
        int trackTop = Math.max(visibleTop, 30);
        int trackBottom = Math.max(trackTop + 1, visibleBottom);
        int trackHeight = trackBottom - trackTop;

        int totalContentHeight = Math.max(1, maxScroll + (visibleBottom - visibleTop));
        int visibleHeight = Math.max(1, visibleBottom - visibleTop);
        int thumbHeight = Math.max(20, (int) (trackHeight * (visibleHeight / (float) totalContentHeight)));
        thumbHeight = Math.min(thumbHeight, trackHeight);
        int maxThumbTravel = trackHeight - thumbHeight;
        int thumbY = trackTop + (maxScroll == 0 ? 0 : (int) (maxThumbTravel * (scrollOffset / (float) maxScroll)));

        net.minecraft.client.gui.Gui.drawRect(trackX, trackTop, trackX + trackWidth, trackBottom, 0x60000000);
        net.minecraft.client.gui.Gui.drawRect(trackX, thumbY, trackX + trackWidth, thumbY + thumbHeight, 0x90FFFFFF);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int dWheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (dWheel != 0 && maxScroll > 0) {
            if (dWheel > 0) {
                scrollOffset = Math.max(scrollOffset - SCROLL_STEP, 0);
            } else {
                scrollOffset = Math.min(scrollOffset + SCROLL_STEP, maxScroll);
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (awaitingKey == 1) {
            if (keyCode == 1) { // ESC cancels
                awaitingKey = -1;
                setKeyButton.displayString = formatKey(PerspectiveDistance.getResetKeyCode());
                return;
            }
            PerspectiveDistance.setResetKeyCode(keyCode);
            awaitingKey = -1;
            setKeyButton.displayString = formatKey(keyCode);
            return;
        }
        
        if (awaitingZoomKey == 1) {
            if (keyCode == 1) { // ESC cancels
                awaitingZoomKey = -1;
                zoomKeyButton.displayString = formatKey(PerspectiveDistance.getZoomKeyCode());
                return;
            }
            PerspectiveDistance.setZoomKeyCode(keyCode);
            awaitingZoomKey = -1;
            zoomKeyButton.displayString = formatKey(keyCode);
            return;
        }
        
        super.keyTyped(typedChar, keyCode);
    }

    private String formatKey(int keyCode) {
        String name = Keyboard.getKeyName(keyCode);
        if (name == null) name = "UNKNOWN";
        return "Key: " + name;
    }


    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

// Custom slider for speed (Minecraft-style)
class SpeedSlider extends GuiButton {
    private final float minValue;
    private final float maxValue;
    private float currentValue;
    private float sliderValue;
    private boolean dragging = false;
    
    public SpeedSlider(int id, int x, int y, String name, float min, float max, float current) {
        super(id, x, y, 120, 20, "");
        this.minValue = min;
        this.maxValue = max;
        this.currentValue = current;
        this.sliderValue = (current - min) / (max - min);
        this.displayString = String.format("Speed: %.2fb", currentValue);
    }
    
    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            // Draw vanilla button background (fixed normal state; no hover brightening)
            mc.getTextureManager().bindTexture(GuiButton.buttonTextures);
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            int v = 46; // always use dark normal row (no hover brightening)
            int half = this.width / 2;
            this.drawTexturedModalRect(this.xPosition, this.yPosition, 0, v, half, 20);
            this.drawTexturedModalRect(this.xPosition + half, this.yPosition, 200 - half, v, half, 20);
            
            // Draw slider knob
            int knobX = this.xPosition + (int)(this.sliderValue * (this.width - 8));
            this.drawTexturedModalRect(knobX, this.yPosition, 0, 66, 4, 20);
            this.drawTexturedModalRect(knobX + 4, this.yPosition, 196, 66, 4, 20);
            
            // Text on top
            boolean hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
            int textColor = hovered ? 0xFFFFA0 : 0xE0E0E0; // vanilla: yellow on hover, gray otherwise
            int textWidth = mc.fontRendererObj.getStringWidth(this.displayString);
            int textX = this.xPosition + (this.width - textWidth) / 2;
            int textY = this.yPosition + (this.height - 8) / 2;
            mc.fontRendererObj.drawString(this.displayString, textX, textY, textColor);
        }
    }
    
    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible && mouseX >= this.xPosition && mouseX <= this.xPosition + this.width &&
            mouseY >= this.yPosition && mouseY <= this.yPosition + this.height) {
            this.dragging = true;
            return true;
        }
        return false;
    }
    
    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        this.dragging = false;
    }
    
    @Override
    public void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible && this.dragging) {
            float normalizedPos = (float)(mouseX - (this.xPosition + 4)) / (float)(this.width - 8);
            normalizedPos = Math.max(0.0f, Math.min(1.0f, normalizedPos));
            this.sliderValue = normalizedPos;
            this.currentValue = minValue + (normalizedPos * (maxValue - minValue));
            this.displayString = String.format("Speed: %.2fb", currentValue);
        }
    }
    
    public float getValue() {
        return currentValue;
    }
}

// Custom slider for smooth speed (Minecraft-style)
class SmoothSlider extends GuiButton {
    private final float minValue;
    private final float maxValue;
    private float currentValue;
    private float sliderValue;
    private boolean dragging = false;
    
    public SmoothSlider(int id, int x, int y, String name, float min, float max, float current) {
        super(id, x, y, 120, 20, "");
        this.minValue = min;
        this.maxValue = max;
        this.currentValue = current;
        this.sliderValue = (current - min) / (max - min);
        this.displayString = String.format("Smooth: %.2f", currentValue);
    }
    
    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            mc.getTextureManager().bindTexture(GuiButton.buttonTextures);
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            int v = 46; // keep dark background
            int half = this.width / 2;
            this.drawTexturedModalRect(this.xPosition, this.yPosition, 0, v, half, 20);
            this.drawTexturedModalRect(this.xPosition + half, this.yPosition, 200 - half, v, half, 20);
            int knobX = this.xPosition + (int)(this.sliderValue * (this.width - 8));
            this.drawTexturedModalRect(knobX, this.yPosition, 0, 66, 4, 20);
            this.drawTexturedModalRect(knobX + 4, this.yPosition, 196, 66, 4, 20);
            boolean hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
            int textColor = hovered ? 0xFFFFA0 : 0xE0E0E0;
            int textWidth = mc.fontRendererObj.getStringWidth(this.displayString);
            int textX = this.xPosition + (this.width - textWidth) / 2;
            int textY = this.yPosition + (this.height - 8) / 2;
            mc.fontRendererObj.drawString(this.displayString, textX, textY, textColor);
        }
    }
    
    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible && mouseX >= this.xPosition && mouseX <= this.xPosition + this.width &&
            mouseY >= this.yPosition && mouseY <= this.yPosition + this.height) {
            this.dragging = true;
            return true;
        }
        return false;
    }
    
    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        this.dragging = false;
    }
    
    @Override
    public void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible && this.dragging) {
            float normalizedPos = (float)(mouseX - (this.xPosition + 4)) / (float)(this.width - 8);
            normalizedPos = Math.max(0.0f, Math.min(1.0f, normalizedPos));
            this.sliderValue = normalizedPos;
            this.currentValue = minValue + (normalizedPos * (maxValue - minValue));
            this.displayString = String.format("Smooth: %.2f", currentValue);
        }
    }
    
    public float getValue() {
        return currentValue;
    }
}

// Custom slider for perspective FOV (Minecraft-style)
class PerspectiveFOVSlider extends GuiButton {
    private final float minValue;
    private final float maxValue;
    private float currentValue;
    private float sliderValue;
    private boolean dragging = false;
    
    public PerspectiveFOVSlider(int id, int x, int y, String name, float min, float max, float current) {
        super(id, x, y, 120, 20, "");
        this.minValue = min;
        this.maxValue = max;
        this.currentValue = current;
        this.sliderValue = (current - min) / (max - min);
        this.displayString = String.format("FOV: %.0f", currentValue);
    }
    
    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            mc.getTextureManager().bindTexture(GuiButton.buttonTextures);
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            int v = 46; // keep dark background
            int half = this.width / 2;
            this.drawTexturedModalRect(this.xPosition, this.yPosition, 0, v, half, 20);
            this.drawTexturedModalRect(this.xPosition + half, this.yPosition, 200 - half, v, half, 20);
            int knobX = this.xPosition + (int)(this.sliderValue * (this.width - 8));
            this.drawTexturedModalRect(knobX, this.yPosition, 0, 66, 4, 20);
            this.drawTexturedModalRect(knobX + 4, this.yPosition, 196, 66, 4, 20);
            boolean hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
            int textColor = hovered ? 0xFFFFA0 : 0xE0E0E0;
            int textWidth = mc.fontRendererObj.getStringWidth(this.displayString);
            int textX = this.xPosition + (this.width - textWidth) / 2;
            int textY = this.yPosition + (this.height - 8) / 2;
            mc.fontRendererObj.drawString(this.displayString, textX, textY, textColor);
        }
    }
    
    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible && mouseX >= this.xPosition && mouseX <= this.xPosition + this.width &&
            mouseY >= this.yPosition && mouseY <= this.yPosition + this.height) {
            this.dragging = true;
            return true;
        }
        return false;
    }
    
    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        this.dragging = false;
    }
    
    @Override
    public void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible && this.dragging) {
            float normalizedPos = (float)(mouseX - (this.xPosition + 4)) / (float)(this.width - 8);
            normalizedPos = Math.max(0.0f, Math.min(1.0f, normalizedPos));
            this.sliderValue = normalizedPos;
            this.currentValue = minValue + (normalizedPos * (maxValue - minValue));
            this.displayString = String.format("FOV: %.0f", currentValue);
        }
    }
    
    public float getValue() {
        return currentValue;
    }
}