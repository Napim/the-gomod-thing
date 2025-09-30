package me.ballmc.gomod.gui;

import me.ballmc.gomod.features.AutoQueue;
import me.ballmc.gomod.features.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;

/**
 * GUI for configuring AutoQueue (Win Requeue) settings
 */
public class AutoQueueGUI extends GuiScreen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 30;
    private static final int BUTTON_WIDTH = 200;
    
    // Toggle button
    private GuiButton toggleButton;
    private GuiButton backButton;
    
    // Sliders
    private SoloDelaySlider soloDelaySlider;
    private TeamsDelaySlider teamsDelaySlider;
    
    // Selected button for dragging
    private GuiButton selectedButton = null;

    @Override
    public void initGui() {
        super.initGui();
        
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int centerX = scaledResolution.getScaledWidth() / 2;
        
        // Center the entire vertical stack of buttons
        int totalButtons = 4; // Toggle, Solo Delay, Teams Delay, Back
        int totalHeight = totalButtons * BUTTON_HEIGHT + (totalButtons - 1) * SPACING;
        int titleAndDescHeight = 60;
        int availableHeight = scaledResolution.getScaledHeight() - titleAndDescHeight;
        int startY = titleAndDescHeight + (availableHeight - totalHeight) / 2;
        
        // Toggle button
        toggleButton = new GuiButton(0, centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT,
            (AutoQueue.isEnabled() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled"));
        
        // Solo delay slider
        soloDelaySlider = new SoloDelaySlider(1, centerX - BUTTON_WIDTH / 2, startY + SPACING, 
                "Solo Delay", 0.0f, 5.0f, ConfigManager.getAutoQueueSoloDelay());
        
        // Teams delay slider
        teamsDelaySlider = new TeamsDelaySlider(2, centerX - BUTTON_WIDTH / 2, startY + SPACING * 2, 
                "Teams Delay", 0.0f, 5.0f, ConfigManager.getAutoQueueTeamsDelay());
        
        // Back button
        backButton = new GuiButton(99, centerX - BUTTON_WIDTH / 2, startY + SPACING * 3, BUTTON_WIDTH, BUTTON_HEIGHT,
                EnumChatFormatting.GRAY + "Back to Menu");
        
        this.buttonList.add(toggleButton);
        this.buttonList.add(soloDelaySlider);
        this.buttonList.add(teamsDelaySlider);
        this.buttonList.add(backButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Draw background
        drawDefaultBackground();
        
        // Draw title
        drawCenteredString(fontRendererObj, EnumChatFormatting.GOLD + "Win Requeue Settings", 
                          width / 2, 20, 0xFFFFFF);
        
        // Draw description
        String description = EnumChatFormatting.GRAY + "Configure auto-queue delays for solo and teams games";
        drawCenteredString(fontRendererObj, description, width / 2, 40, 0xFFFFFF);
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0: // Toggle button
                boolean newState = !AutoQueue.isEnabled();
                AutoQueue.setEnabled(newState);
                button.displayString = (newState ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled");
                break;
            case 1: // Solo delay slider
                if (button instanceof SoloDelaySlider) {
                    SoloDelaySlider slider = (SoloDelaySlider) button;
                    ConfigManager.setAutoQueueSoloDelay(slider.getValue());
                }
                break;
            case 2: // Teams delay slider
                if (button instanceof TeamsDelaySlider) {
                    TeamsDelaySlider slider = (TeamsDelaySlider) button;
                    ConfigManager.setAutoQueueTeamsDelay(slider.getValue());
                }
                break;
            case 99: // Back button
                Minecraft.getMinecraft().displayGuiScreen(new GomodGUI());
                break;
        }
    }
    
    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        // Handle slider clicks
        for (GuiButton button : this.buttonList) {
            if (button instanceof SoloDelaySlider || button instanceof TeamsDelaySlider) {
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
        if (this.selectedButton instanceof SoloDelaySlider || this.selectedButton instanceof TeamsDelaySlider) {
            this.selectedButton.mouseDragged(Minecraft.getMinecraft(), mouseX, mouseY);
            
            // Save the value immediately during dragging
            if (this.selectedButton instanceof SoloDelaySlider) {
                SoloDelaySlider slider = (SoloDelaySlider) this.selectedButton;
                ConfigManager.setAutoQueueSoloDelay(slider.getValue());
            } else if (this.selectedButton instanceof TeamsDelaySlider) {
                TeamsDelaySlider slider = (TeamsDelaySlider) this.selectedButton;
                ConfigManager.setAutoQueueTeamsDelay(slider.getValue());
            }
        }
    }
    
    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        
        // Handle slider release
        if (this.selectedButton instanceof SoloDelaySlider || this.selectedButton instanceof TeamsDelaySlider) {
            this.selectedButton.mouseReleased(mouseX, mouseY);
            
            // Save the final value when releasing
            if (this.selectedButton instanceof SoloDelaySlider) {
                SoloDelaySlider slider = (SoloDelaySlider) this.selectedButton;
                ConfigManager.setAutoQueueSoloDelay(slider.getValue());
            } else if (this.selectedButton instanceof TeamsDelaySlider) {
                TeamsDelaySlider slider = (TeamsDelaySlider) this.selectedButton;
                ConfigManager.setAutoQueueTeamsDelay(slider.getValue());
            }
            
            this.selectedButton = null;
        }
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

// Custom slider for solo delay (Minecraft-style)
class SoloDelaySlider extends GuiButton {
    private final float minValue;
    private final float maxValue;
    private float currentValue;
    private float sliderValue;
    private boolean dragging = false;
    
    public SoloDelaySlider(int id, int x, int y, String name, float min, float max, float current) {
        super(id, x, y, 200, 20, "");
        this.minValue = min;
        this.maxValue = max;
        this.currentValue = current;
        this.sliderValue = (current - min) / (max - min);
        this.displayString = String.format("Solo Delay: %.2fs", currentValue);
    }
    
    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            // Draw vanilla button background
            mc.getTextureManager().bindTexture(GuiButton.buttonTextures);
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            int v = 46; // always use dark normal row
            int half = this.width / 2;
            this.drawTexturedModalRect(this.xPosition, this.yPosition, 0, v, half, 20);
            this.drawTexturedModalRect(this.xPosition + half, this.yPosition, 200 - half, v, half, 20);
            
            // Draw slider knob
            int knobX = this.xPosition + (int)(this.sliderValue * (this.width - 8));
            this.drawTexturedModalRect(knobX, this.yPosition, 0, 66, 4, 20);
            this.drawTexturedModalRect(knobX + 4, this.yPosition, 196, 66, 4, 20);
            
            // Text on top
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
            this.displayString = String.format("Solo Delay: %.2fs", currentValue);
        }
    }
    
    public float getValue() {
        return currentValue;
    }
}

// Custom slider for teams delay (Minecraft-style)
class TeamsDelaySlider extends GuiButton {
    private final float minValue;
    private final float maxValue;
    private float currentValue;
    private float sliderValue;
    private boolean dragging = false;
    
    public TeamsDelaySlider(int id, int x, int y, String name, float min, float max, float current) {
        super(id, x, y, 200, 20, "");
        this.minValue = min;
        this.maxValue = max;
        this.currentValue = current;
        this.sliderValue = (current - min) / (max - min);
        this.displayString = String.format("Teams Delay: %.2fs", currentValue);
    }
    
    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            // Draw vanilla button background
            mc.getTextureManager().bindTexture(GuiButton.buttonTextures);
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            int v = 46; // always use dark normal row
            int half = this.width / 2;
            this.drawTexturedModalRect(this.xPosition, this.yPosition, 0, v, half, 20);
            this.drawTexturedModalRect(this.xPosition + half, this.yPosition, 200 - half, v, half, 20);
            
            // Draw slider knob
            int knobX = this.xPosition + (int)(this.sliderValue * (this.width - 8));
            this.drawTexturedModalRect(knobX, this.yPosition, 0, 66, 4, 20);
            this.drawTexturedModalRect(knobX + 4, this.yPosition, 196, 66, 4, 20);
            
            // Text on top
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
            this.displayString = String.format("Teams Delay: %.2fs", currentValue);
        }
    }
    
    public float getValue() {
        return currentValue;
    }
}