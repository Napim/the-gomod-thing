package me.ballmc.AntiShuffle.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import me.ballmc.AntiShuffle.Main;
import me.ballmc.AntiShuffle.features.AIChatHandler;
import me.ballmc.AntiShuffle.features.ConfigManager;
import me.ballmc.AntiShuffle.features.ChatTranslator;
import me.ballmc.AntiShuffle.features.TeamSpamManager;

/**
 * Settings GUI for the GoMod123 mod.
 * 
 * This GUI provides a user-friendly interface to manage all settings in one place.
 * Features:
 * - Anti-Shuffle toggle: Enable/disable the main Anti-Shuffle functionality
 * - GoStats toggle: Enable/disable the GoStats AI chat functionality
 * - Auto Translate toggle: Enable/disable automatic chat translation
 * - Team Spam List: View information about the team spam player list
 * 
 * The GUI can be accessed using the command: /gm gui
 * 
 * All settings are saved to the config file in the .weave/gomod123 directory.
 */
public class GoMod123SettingsGui extends GuiScreen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 150;
    private static final int PADDING = 10;
    
    private GuiButton toggleAntiShuffleButton;
    private GuiButton toggleGoStatsButton;
    private GuiButton toggleAutoTranslateButton;
    private GuiButton viewTeamSpamButton;
    private GuiButton doneButton;
    
    @Override
    public void initGui() {
        int centerX = this.width / 2;
        int startY = 50;
        
        // Toggle anti-shuffle button
        this.buttonList.add(toggleAntiShuffleButton = new GuiButton(0, 
            centerX - BUTTON_WIDTH / 2, 
            startY, 
            BUTTON_WIDTH, 
            BUTTON_HEIGHT, 
            getAntiShuffleButtonText()));
        
        // Toggle GoStats button
        this.buttonList.add(toggleGoStatsButton = new GuiButton(1, 
            centerX - BUTTON_WIDTH / 2, 
            startY + BUTTON_HEIGHT + PADDING, 
            BUTTON_WIDTH, 
            BUTTON_HEIGHT, 
            getGoStatsButtonText()));
        
        // Toggle Auto Translate button
        this.buttonList.add(toggleAutoTranslateButton = new GuiButton(2, 
            centerX - BUTTON_WIDTH / 2, 
            startY + (BUTTON_HEIGHT + PADDING) * 2, 
            BUTTON_WIDTH, 
            BUTTON_HEIGHT, 
            getAutoTranslateButtonText()));
        
        // Team Spam List button
        this.buttonList.add(viewTeamSpamButton = new GuiButton(3, 
            centerX - BUTTON_WIDTH / 2, 
            startY + (BUTTON_HEIGHT + PADDING) * 3, 
            BUTTON_WIDTH, 
            BUTTON_HEIGHT, 
            getTeamSpamButtonText()));
        
        // Done button at the bottom
        this.buttonList.add(doneButton = new GuiButton(4, 
            centerX - BUTTON_WIDTH / 2, 
            this.height - BUTTON_HEIGHT - 20, 
            BUTTON_WIDTH, 
            BUTTON_HEIGHT, 
            "Done"));
    }
    
    @Override
    public void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0: // Toggle AntiShuffle
                Main.setEnabled(!Main.enabled);
                button.displayString = getAntiShuffleButtonText();
                break;
                
            case 1: // Toggle GoStats
                boolean newGoStatsState = !ConfigManager.isGoStatsEnabled();
                ConfigManager.setGoStatsEnabled(newGoStatsState);
                AIChatHandler.setEnabled(newGoStatsState);
                button.displayString = getGoStatsButtonText();
                break;
                
            case 2: // Toggle Auto Translate
                boolean newAutoTranslateState = !ConfigManager.isAutoTranslateEnabled();
                ConfigManager.setAutoTranslateEnabled(newAutoTranslateState);
                button.displayString = getAutoTranslateButtonText();
                break;
                
            case 3: // Team Spam info
                // Display info about team spam players
                StringBuilder msg = new StringBuilder();
                msg.append("§e§lTeam Spam Players: §r");
                msg.append(TeamSpamManager.getPlayers().size()).append(" players");
                
                Main.sendMessage(msg.toString());
                Main.sendMessage("§7Use §f/teamspam add <player>§7 to add players");
                Main.sendMessage("§7Use §f/teamspam remove <player>§7 to remove players");
                break;
                
            case 4: // Done
                this.mc.displayGuiScreen(null);
                break;
        }
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Draw a dark, semi-transparent background
        this.drawDefaultBackground();
        
        // Draw title
        String title = EnumChatFormatting.YELLOW + "GoMod123 Settings";
        this.drawCenteredString(this.fontRendererObj, title, this.width / 2, 20, 0xFFFFFF);
        
        // Draw subtitle/version
        String version = EnumChatFormatting.GRAY + "v1.0";
        this.drawCenteredString(this.fontRendererObj, version, this.width / 2, 34, 0xFFFFFF);
        
        // Draw buttons
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    private String getAntiShuffleButtonText() {
        String status = Main.enabled ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
        return "Anti-Shuffle: " + status;
    }
    
    private String getGoStatsButtonText() {
        String status = ConfigManager.isGoStatsEnabled() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
        return "GoStats: " + status;
    }
    
    private String getAutoTranslateButtonText() {
        String status = ConfigManager.isAutoTranslateEnabled() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
        return "Auto Translate: " + status;
    }
    
    private String getTeamSpamButtonText() {
        int playerCount = TeamSpamManager.getPlayers().size();
        return "Team Spam List: " + EnumChatFormatting.AQUA + playerCount + " players";
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
} 