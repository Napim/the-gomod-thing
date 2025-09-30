package me.ballmc.gomod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ChatComponentText;
import me.ballmc.gomod.features.AIChatHandler;
import me.ballmc.gomod.features.ConfigManager;
import me.ballmc.gomod.features.ApiKeyManager;
import org.lwjgl.input.Keyboard;

/**
 * GUI for managing GoStats AI functionality settings.
 * 
 * This GUI allows users to:
 * - Toggle the @gostats123 AI functionality on/off
 * - View API key status
 * - Access help information
 * 
 * Accessible via /gomod command or the main GUI.
 */
public class GoStatsGUI extends GuiScreen {
    private GuiButton toggleButton;
    private GuiButton apiKeyButton;
    private GuiButton helpButton;
    private GuiButton backButton;
    
    private GuiTextField apiKeyField;
    private boolean showApiKeyInput = false;
    private boolean goStatsEnabled;
    
    @Override
    public void initGui() {
        super.initGui();
        
        // Update current state
        goStatsEnabled = ConfigManager.isGoStatsEnabled();
        
        // Calculate button positions
        int centerX = width / 2;
        int centerY = height / 2;
        int buttonWidth = 200;
        int buttonHeight = 20;
        
        // Toggle button
        toggleButton = new GuiButton(0, centerX - buttonWidth / 2, centerY - 60, buttonWidth, buttonHeight, 
            "gostats123 AI: " + (goStatsEnabled ? (EnumChatFormatting.GREEN + "Enabled") : (EnumChatFormatting.RED + "Disabled")));
        
        // API Key button - now opens input field
        apiKeyButton = new GuiButton(1, centerX - buttonWidth / 2, centerY - 35, buttonWidth, buttonHeight, 
            EnumChatFormatting.YELLOW + "Set OpenAI API Key");
        
        // Help button
        helpButton = new GuiButton(2, centerX - buttonWidth / 2, centerY - 10, buttonWidth, buttonHeight, 
            EnumChatFormatting.YELLOW + "Help & Usage");
        
        // Back button
        backButton = new GuiButton(3, centerX - buttonWidth / 2, centerY + 15, buttonWidth, buttonHeight, 
            EnumChatFormatting.GRAY + "Back to Menu");
        
        // Add buttons to the screen
        buttonList.add(toggleButton);
        buttonList.add(apiKeyButton);
        buttonList.add(helpButton);
        buttonList.add(backButton);
        
        // Initialize API key text field (hidden by default)
        apiKeyField = new GuiTextField(4, fontRendererObj, centerX - 200, centerY + 50, 400, 20);
        apiKeyField.setMaxStringLength(300); // OpenAI keys can be up to ~200 chars, giving extra buffer
        apiKeyField.setFocused(false);
        
        // Pre-fill with existing API key if available
        if (ApiKeyManager.hasApiKey("openai")) {
            apiKeyField.setText(ApiKeyManager.getApiKey("openai"));
        }
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Draw background
        drawDefaultBackground();
        
        // Draw title
        drawCenteredString(fontRendererObj, 
            EnumChatFormatting.BOLD.toString() + EnumChatFormatting.BLUE.toString() + "gostats123 AI Settings", 
            width / 2 + 6, 20, 0xFFFFFF);
        
        // Draw description
        String description = EnumChatFormatting.GRAY + "Configure gostats123 AI";
        int descWidth = fontRendererObj.getStringWidth(description);
        fontRendererObj.drawString(description, width / 2 - descWidth / 2, 40, 0xFFFFFF);
        
        // Draw API key status (moved up to replace current status line)
        String apiStatus = "API Key: " + (ApiKeyManager.hasApiKey("openai") ? 
            EnumChatFormatting.GREEN + "Configured" : EnumChatFormatting.RED + "Not Configured");
        int apiWidth = fontRendererObj.getStringWidth(apiStatus);
        fontRendererObj.drawString(apiStatus, width / 2 - apiWidth / 2, 60, 0xFFFFFF);
        
        // Draw API key input field if visible
        if (showApiKeyInput) {
            // Draw background for the input area (wider to accommodate longer keys)
            drawRect(width / 2 - 210, height / 2 + 40, width / 2 + 210, height / 2 + 80, 0x80000000);
            drawRect(width / 2 - 209, height / 2 + 41, width / 2 + 209, height / 2 + 79, 0xFF404040);
            
            // Draw label
            String label = EnumChatFormatting.YELLOW + "Enter your OpenAI API Key:";
            int labelWidth = fontRendererObj.getStringWidth(label);
            fontRendererObj.drawString(label, width / 2 - labelWidth / 2, height / 2 + 30, 0xFFFFFF);
            
            // Draw instructions
            String instructions = EnumChatFormatting.GRAY + "Press ENTER to save, ESC to cancel";
            int instWidth = fontRendererObj.getStringWidth(instructions);
            fontRendererObj.drawString(instructions, width / 2 - instWidth / 2, height / 2 + 85, 0xFFFFFF);
            
            // Draw additional info about key length
            String keyInfo = EnumChatFormatting.DARK_GRAY + "OpenAI API keys are typically ~200 characters long";
            int keyInfoWidth = fontRendererObj.getStringWidth(keyInfo);
            fontRendererObj.drawString(keyInfo, width / 2 - keyInfoWidth / 2, height / 2 + 100, 0xFFFFFF);
            
            // Draw the text field
            apiKeyField.drawTextBox();
        }
        
        // Draw usage instructions
        if (goStatsEnabled && ApiKeyManager.hasApiKey("openai")) {
            String instruction1 = EnumChatFormatting.YELLOW + "Usage: Type @gostats123 in chat to get AI responses";
            String instruction2 = EnumChatFormatting.YELLOW + "Example: Hey @gostats123 what's the best strategy?";
            
            int inst1Width = fontRendererObj.getStringWidth(instruction1);
            int inst2Width = fontRendererObj.getStringWidth(instruction2);
            
            fontRendererObj.drawString(instruction1, width / 2 - inst1Width / 2, height - 80, 0xFFFFFF);
            fontRendererObj.drawString(instruction2, width / 2 - inst2Width / 2, height - 60, 0xFFFFFF);
        } else if (goStatsEnabled && !ApiKeyManager.hasApiKey("openai")) {
            String warning = EnumChatFormatting.RED + "Warning: GoStats is enabled but no API key is set!";
            String instruction = EnumChatFormatting.YELLOW + "Use /gomod api openai YOUR_KEY to set your API key";
            
            int warnWidth = fontRendererObj.getStringWidth(warning);
            int instWidth = fontRendererObj.getStringWidth(instruction);
            
            fontRendererObj.drawString(warning, width / 2 - warnWidth / 2, height - 80, 0xFFFFFF);
            fontRendererObj.drawString(instruction, width / 2 - instWidth / 2, height - 60, 0xFFFFFF);
        }
        
        // Call parent method to draw buttons
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    @Override
    public void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0: // Toggle GoStats
                toggleGoStats();
                break;
            case 1: // API Key input
                toggleApiKeyInput();
                break;
            case 2: // Help
                showHelp();
                break;
            case 3: // Back
                mc.displayGuiScreen(new GomodGUI());
                break;
        }
    }
    
    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (showApiKeyInput) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                // Cancel input
                showApiKeyInput = false;
                apiKeyField.setFocused(false);
            } else if (keyCode == Keyboard.KEY_RETURN) {
                // Save API key
                saveApiKey();
            } else {
                // Handle text input
                apiKeyField.textboxKeyTyped(typedChar, keyCode);
            }
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }
    
    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        try {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            
            if (showApiKeyInput) {
                apiKeyField.mouseClicked(mouseX, mouseY, mouseButton);
            }
        } catch (Exception e) {
            // Ignore mouse click errors
        }
    }
    
    @Override
    public void updateScreen() {
        if (showApiKeyInput) {
            apiKeyField.updateCursorCounter();
        }
    }
    
    private void toggleGoStats() {
        goStatsEnabled = !goStatsEnabled;
        ConfigManager.setGoStatsEnabled(goStatsEnabled);
        AIChatHandler.setEnabled(goStatsEnabled);
        
        // Update button text
        toggleButton.displayString = "gostats123 AI: " + (goStatsEnabled ? 
            EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled");
        
        // Suppress chat spam: no chat message on toggle
    }
    
    private void toggleApiKeyInput() {
        showApiKeyInput = !showApiKeyInput;
        if (showApiKeyInput) {
            apiKeyField.setFocused(true);
            // Clear the field if no key is set
            if (!ApiKeyManager.hasApiKey("openai")) {
                apiKeyField.setText("");
            }
            // Update button text
            apiKeyButton.displayString = EnumChatFormatting.RED + "Cancel API Key Input";
        } else {
            apiKeyField.setFocused(false);
            // Update button text
            apiKeyButton.displayString = EnumChatFormatting.YELLOW + "Set OpenAI API Key";
        }
    }
    
    private void saveApiKey() {
        String apiKey = apiKeyField.getText().trim();
        if (apiKey.isEmpty()) {
            ApiKeyManager.setApiKey("openai", "");
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "API key cleared."));
            }
        } else {
            ApiKeyManager.setApiKey("openai", apiKey);
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "OpenAI API key saved successfully!"));
            }
        }
        showApiKeyInput = false;
        apiKeyField.setFocused(false);
        // Update button text back to normal
        apiKeyButton.displayString = EnumChatFormatting.YELLOW + "Set OpenAI API Key";
    }
    
    private void showHelp() {
        String[] helpMessages = {
            EnumChatFormatting.BLUE + "=== gostats123 AI Help ===",
            EnumChatFormatting.YELLOW + "What can gostats123 AI do?",
            "",
            EnumChatFormatting.WHITE + "- " + EnumChatFormatting.GREEN + "Player Tracking:" + EnumChatFormatting.WHITE + " Ask where players are, their distance, direction, and health",
            EnumChatFormatting.WHITE + "- " + EnumChatFormatting.GREEN + "Equipment Analysis:" + EnumChatFormatting.WHITE + " See what items players are holding and their renamed armor",
            EnumChatFormatting.WHITE + "- " + EnumChatFormatting.GREEN + "PvP Tips:" + EnumChatFormatting.WHITE + " Get advice on PvP strategies and positioning during the game",
            "",
            EnumChatFormatting.YELLOW + "Example Questions:",
            EnumChatFormatting.WHITE + "- @gostats123 where is Gallantdrop1196?",
            EnumChatFormatting.WHITE + "- @gostats123 what's SaturnAndJupiter holding?",
            EnumChatFormatting.WHITE + "- @gostats123 should I rush or wait?",
            EnumChatFormatting.WHITE + "- @gostats123 who should I star?",
            "",
            EnumChatFormatting.YELLOW + "How to use:",
            EnumChatFormatting.WHITE + "1. Enable gostats123 AI using the toggle button",
            EnumChatFormatting.WHITE + "2. Set your OpenAI API key",
            EnumChatFormatting.WHITE + "3. Type @gostats123 in any chat message"
        };
        
        if (Minecraft.getMinecraft().thePlayer != null) {
            for (String msg : helpMessages) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg));
            }
        }
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
