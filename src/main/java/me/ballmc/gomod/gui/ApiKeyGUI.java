package me.ballmc.gomod.gui;

import me.ballmc.gomod.features.ApiKeyManager;
import me.ballmc.gomod.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ChatComponentText;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import java.net.URI;
import java.awt.Desktop;

public class ApiKeyGUI extends GuiScreen {
    private GuiScreen parent;
    private GuiTextField openaiField;
    private GuiTextField hypixelField;
    private GuiButton saveButton;
    private GuiButton backButton;
    private GuiButton clearOpenAIButton;
    private GuiButton clearHypixelButton;
    private GuiButton revealOpenAIButton;
    private GuiButton revealHypixelButton;
	private GuiButton hypixelSiteButton;
    
    // Store actual API keys separately from displayed masked versions
    private String actualOpenaiKey = "";
    private String actualHypixelKey = "";
    
    // Track whether keys are currently revealed
    private boolean openaiRevealed = false;
    private boolean hypixelRevealed = false;
    
    public ApiKeyGUI(GuiScreen parent) {
        this.parent = parent;
    }
    
    @Override
    public void initGui() {
        super.initGui();
        
        // Calculate center positions
        int centerX = width / 2;
        int centerY = height / 2;
        
        // OpenAI API Key Field
        openaiField = new GuiTextField(0, fontRendererObj, centerX - 200, centerY - 80, 400, 20);
        openaiField.setMaxStringLength(300);
        String openaiKey = ApiKeyManager.getApiKey("openai");
        actualOpenaiKey = openaiKey != null ? openaiKey : "";
        openaiField.setText(openaiKey != null ? maskApiKey(openaiKey) : "");
        
        // Hypixel API Key Field
        hypixelField = new GuiTextField(1, fontRendererObj, centerX - 200, centerY - 40, 400, 20);
        hypixelField.setMaxStringLength(100);
        String hypixelKey = ApiKeyManager.getApiKey("hypixel");
        actualHypixelKey = hypixelKey != null ? hypixelKey : "";
        hypixelField.setText(hypixelKey != null ? maskApiKey(hypixelKey) : "");
        
		// Buttons
		hypixelSiteButton = new IconCenterButton(8, centerX - 100, centerY - 15, 200, 20, "\u00A7b \u27A6", "\u00A7b developer.hypixel.net");
		saveButton = new GuiButton(2, centerX - 100, centerY + 10, 200, 20, "Save API Keys");
		backButton = new GuiButton(3, centerX - 100, centerY + 40, 200, 20, "Back to Menu");
        clearOpenAIButton = new GuiButton(4, centerX + 210, centerY - 80, 60, 20, "Clear");
        clearHypixelButton = new GuiButton(5, centerX + 210, centerY - 40, 60, 20, "Clear");
        revealOpenAIButton = new GuiButton(6, centerX - 270, centerY - 80, 60, 20, "Reveal");
        revealHypixelButton = new GuiButton(7, centerX - 270, centerY - 40, 60, 20, "Reveal");
        
		buttonList.add(hypixelSiteButton);
        buttonList.add(saveButton);
        buttonList.add(backButton);
        buttonList.add(clearOpenAIButton);
        buttonList.add(clearHypixelButton);
        buttonList.add(revealOpenAIButton);
        buttonList.add(revealHypixelButton);
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Draw background
        drawDefaultBackground();
        
        // Calculate center positions
        int centerX = width / 2;
        int centerY = height / 2;
        
        // Draw title
        drawCenteredString(fontRendererObj, 
            EnumChatFormatting.BOLD.toString() + EnumChatFormatting.YELLOW.toString() + "API Key Management", 
            centerX, centerY - 130, 0xFFFFFF);
        
        // Draw instructions
        drawCenteredString(fontRendererObj, 
            EnumChatFormatting.YELLOW.toString() + "Enter your API keys below:", 
            centerX, centerY - 110, 0xFFFFFF);
        
        // Draw OpenAI section
        drawString(fontRendererObj, 
            EnumChatFormatting.WHITE.toString() + "OpenAI API Key (for gostats123 AI):", 
            centerX - 200, centerY - 90, 0xFFFFFF);
        
        // Draw Hypixel section
        drawString(fontRendererObj, 
            EnumChatFormatting.WHITE.toString() + "Hypixel API Key:", 
            centerX - 200, centerY - 50, 0xFFFFFF);
        
        // Draw help text
        drawCenteredString(fontRendererObj, 
            EnumChatFormatting.GRAY.toString() + "OpenAI: Get from https://platform.openai.com/api-keys", 
            centerX, centerY + 70, 0xFFFFFF);
        
        drawCenteredString(fontRendererObj, 
            EnumChatFormatting.GRAY.toString() + "Hypixel: Get from https://developer.hypixel.net", 
            centerX, centerY + 85, 0xFFFFFF);
        
        // Draw text fields
        openaiField.drawTextBox();
        hypixelField.drawTextBox();
        
        // Draw buttons
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    
    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        openaiField.mouseClicked(mouseX, mouseY, mouseButton);
        hypixelField.mouseClicked(mouseX, mouseY, mouseButton);
    }
    
    @Override
    public void updateScreen() {
        super.updateScreen();
        openaiField.updateCursorCounter();
        hypixelField.updateCursorCounter();
    }
    
    @Override
    public void actionPerformed(GuiButton button) {
        if (button == saveButton) {
            // Save API keys using the actual stored keys
            ApiKeyManager.setApiKey("openai", actualOpenaiKey.isEmpty() ? null : actualOpenaiKey);
            ApiKeyManager.setApiKey("hypixel", actualHypixelKey.isEmpty() ? null : actualHypixelKey);
            
            // Send confirmation message
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(
                Main.CHAT_PREFIX + EnumChatFormatting.GREEN + "API keys saved successfully!"
            ));
            
            // Go back to parent
            Minecraft.getMinecraft().displayGuiScreen(parent);
            
		} else if (button == backButton) {
            Minecraft.getMinecraft().displayGuiScreen(parent);
            
		} else if (button == hypixelSiteButton) {
			String url = "https://developer.hypixel.net";
			try {
				if (Desktop.isDesktopSupported()) {
					Desktop.getDesktop().browse(new URI(url));
				} else {
					// Fallback: send clickable chat message
					ChatComponentText msg = new ChatComponentText(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Click to open Hypixel API site");
					msg.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
					msg.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.AQUA + url)));
					Minecraft.getMinecraft().thePlayer.addChatMessage(msg);
				}
			} catch (Exception e) {
				// Fallback if opening fails: clickable chat message
				ChatComponentText msg = new ChatComponentText(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Click to open Hypixel API site");
				msg.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
				msg.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.AQUA + url)));
				Minecraft.getMinecraft().thePlayer.addChatMessage(msg);
			}
			
        } else if (button == clearOpenAIButton) {
            actualOpenaiKey = "";
            openaiField.setText("");
            openaiRevealed = false;
            revealOpenAIButton.displayString = "Reveal";
            
        } else if (button == clearHypixelButton) {
            actualHypixelKey = "";
            hypixelField.setText("");
            hypixelRevealed = false;
            revealHypixelButton.displayString = "Reveal";
            
        } else if (button == revealOpenAIButton) {
            if (openaiRevealed) {
                // Hide the key
                openaiField.setText(actualOpenaiKey.isEmpty() ? "" : maskApiKey(actualOpenaiKey));
                revealOpenAIButton.displayString = "Reveal";
                openaiRevealed = false;
            } else {
                // Show the key
                openaiField.setText(actualOpenaiKey);
                revealOpenAIButton.displayString = "Hide";
                openaiRevealed = true;
            }
            
        } else if (button == revealHypixelButton) {
            if (hypixelRevealed) {
                // Hide the key
                hypixelField.setText(actualHypixelKey.isEmpty() ? "" : maskApiKey(actualHypixelKey));
                revealHypixelButton.displayString = "Reveal";
                hypixelRevealed = false;
            } else {
                // Show the key
                hypixelField.setText(actualHypixelKey);
                revealHypixelButton.displayString = "Hide";
                hypixelRevealed = true;
            }
        }
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
    
    /**
     * Masks an API key for display purposes with proper formatting
     */
    private String maskApiKey(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        
        // Handle OpenAI keys (sk-... format)
        if (key.startsWith("sk-")) {
            if (key.length() <= 8) {
                return "sk-****";
            }
            return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
        }
        
        // Handle Hypixel keys (UUID format with dashes)
        if (key.contains("-") && key.length() == 36) {
            // Format: 12345678-1234-1234-1234-123456789012
            return key.substring(0, 4) + "****-****-****-****-****" + key.substring(key.length() - 4);
        }
        
        // Handle other keys (fallback)
        if (key.length() <= 8) {
            return "****";
        }
        
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
    
    @Override
    public void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);
        
        if (openaiField.isFocused()) {
            openaiField.textboxKeyTyped(typedChar, keyCode);
            // Update actual key when typing (only if revealed or not masked)
            String currentText = openaiField.getText();
            if (openaiRevealed || !currentText.contains("****")) {
                actualOpenaiKey = currentText;
            }
        } else if (hypixelField.isFocused()) {
            hypixelField.textboxKeyTyped(typedChar, keyCode);
            // Update actual key when typing (only if revealed or not masked)
            String currentText = hypixelField.getText();
            if (hypixelRevealed || !currentText.contains("****")) {
                actualHypixelKey = currentText;
            }
        }
    }
}

// Button that renders an icon/prefix left-aligned while centering the main label
class IconCenterButton extends GuiButton {
    private final String prefix;
    private final String mainLabel;

    public IconCenterButton(int buttonId, int x, int y, int widthIn, int heightIn, String prefix, String mainLabel) {
        super(buttonId, x, y, widthIn, heightIn, mainLabel);
        this.prefix = prefix;
        this.mainLabel = mainLabel;
    }

    @Override
    public void drawButton(net.minecraft.client.Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            // Render default button background without default text
            String saved = this.displayString;
            this.displayString = "";
            super.drawButton(mc, mouseX, mouseY);
            this.displayString = saved;

            int textColor;
            if (!this.enabled) {
                textColor = 10526880; // disabled gray
            } else if (this.hovered) {
                textColor = 16777120; // hovered yellow-ish
            } else {
                textColor = 14737632; // normal
            }

            // Draw prefix (arrow) left-aligned inside the button
            String prefixToDraw = this.prefix == null ? "" : this.prefix;
            int textY = this.yPosition + (this.height - 8) / 2;
            int prefixX = this.xPosition + 4; // small left padding
            mc.fontRendererObj.drawString(prefixToDraw, prefixX, textY, textColor, false);

            // Draw main label centered ignoring prefix width
            String labelToDraw = this.mainLabel == null ? "" : this.mainLabel;
            int labelWidth = mc.fontRendererObj.getStringWidth(labelToDraw);
            int labelX = this.xPosition + (this.width - labelWidth) / 2;
            mc.fontRendererObj.drawString(labelToDraw, labelX, textY, textColor, false);
        }
    }
}
