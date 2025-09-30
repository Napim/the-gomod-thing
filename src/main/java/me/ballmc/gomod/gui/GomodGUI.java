package me.ballmc.gomod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;

public class GomodGUI extends GuiScreen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 30;
    private static final int BUTTON_WIDTH = 200;
    private String title = EnumChatFormatting.DARK_GREEN + "" + EnumChatFormatting.BOLD + "gomod Menu" + EnumChatFormatting.RESET;

    @Override
    public void initGui() {
        super.initGui();
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int centerX = sr.getScaledWidth() / 2;
        
        // Center the entire vertical stack of buttons on the screen
        int totalButtons = 9; // InventoryHUD, Perspective, GoStats, MapQueuer, PlayerQueuer, TeamSpammer, Kit Levels, Win Requeue, TAB Stats, API Keys
        int totalHeight = totalButtons * BUTTON_HEIGHT + (totalButtons - 1) * SPACING;
        int titleHeight = 60; // Space for title and description
        int startY = (sr.getScaledHeight() - totalHeight) / 2 + titleHeight / 2 + 50;
        
        // Inventory HUD button
        GuiButton invHudButton = new GuiButton(1, centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT,
                EnumChatFormatting.AQUA + "Inventory HUD");
        
        // Perspective Settings button
        GuiButton perspectiveButton = new GuiButton(2, centerX - BUTTON_WIDTH / 2, startY + SPACING, BUTTON_WIDTH, BUTTON_HEIGHT,
                EnumChatFormatting.GREEN + "Perspective Settings");
        
        // gostats123 AI button
        GuiButton goStatsButton = new GuiButton(3, centerX - BUTTON_WIDTH / 2, startY + SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT,
                EnumChatFormatting.DARK_GREEN + "gostats123 AI");
        
        // Map Queuer button
        GuiButton mapQueuerButton = new GuiButton(5, centerX - BUTTON_WIDTH / 2, startY + SPACING * 3, BUTTON_WIDTH, BUTTON_HEIGHT,
                EnumChatFormatting.LIGHT_PURPLE + "Map Queuer");
        
        // Player Queuer button
        GuiButton playerQueuerButton = new GuiButton(10, centerX - BUTTON_WIDTH / 2, startY + SPACING * 4, BUTTON_WIDTH, BUTTON_HEIGHT,
                EnumChatFormatting.DARK_PURPLE + "Player Queuer");
        
        // Teams Nick Finder button
        GuiButton teamSpammerButton = new GuiButton(6, centerX - BUTTON_WIDTH / 2, startY + SPACING * 5, BUTTON_WIDTH, BUTTON_HEIGHT,
                EnumChatFormatting.GOLD + "Teams Nick Finder");
        
        // Kit Levels button
        GuiButton kitLevelsButton = new GuiButton(7, centerX - BUTTON_WIDTH / 2, startY + SPACING * 6, BUTTON_WIDTH, BUTTON_HEIGHT,
                EnumChatFormatting.DARK_AQUA + "Kit Levels");
        
        // Win Requeue button
        GuiButton winRequeueButton = new GuiButton(8, centerX - BUTTON_WIDTH / 2, startY + SPACING * 7, BUTTON_WIDTH, BUTTON_HEIGHT,
                EnumChatFormatting.RED + "Win Requeue");
        
        // TAB Stats button
        GuiButton killCounterButton = new GuiButton(9, centerX - BUTTON_WIDTH / 2, startY + SPACING * 8, BUTTON_WIDTH, BUTTON_HEIGHT,
                EnumChatFormatting.DARK_RED + "TAB Stats");
        
        // API Keys button
        GuiButton apiKeysButton = new GuiButton(4, centerX - BUTTON_WIDTH / 2, startY + SPACING * 9, BUTTON_WIDTH, BUTTON_HEIGHT,
                EnumChatFormatting.YELLOW + "API Keys");
        
        this.buttonList.add(invHudButton);
        this.buttonList.add(perspectiveButton);
        this.buttonList.add(goStatsButton);
        this.buttonList.add(mapQueuerButton);
        this.buttonList.add(playerQueuerButton);
        this.buttonList.add(teamSpammerButton);
        this.buttonList.add(kitLevelsButton);
        this.buttonList.add(winRequeueButton);
        this.buttonList.add(killCounterButton);
        this.buttonList.add(apiKeysButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        
        // Draw title
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int centerX = sr.getScaledWidth() / 2;
        int titleY = 20;
        
        fontRendererObj.drawString(this.title, centerX - fontRendererObj.getStringWidth(this.title) / 2, titleY, 0xFFFFFF);
        
        // Draw description
        String description = EnumChatFormatting.GRAY + "Select a feature to configure:";
        fontRendererObj.drawString(description, centerX - fontRendererObj.getStringWidth(description) / 2, titleY + 20, 0xFFFFFF);
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                // Close
                this.mc.displayGuiScreen(null);
                break;
            case 1:
                // Open Inventory HUD GUI
                this.mc.displayGuiScreen(new InventoryHUDGUI());
                break;
            case 2:
                // Open Perspective Settings GUI
                this.mc.displayGuiScreen(new PerspectiveGUI());
                break;
            case 3:
                // Open gostats123 AI GUI
                this.mc.displayGuiScreen(new GoStatsGUI());
                break;
            case 4:
                // Open API Keys GUI
                this.mc.displayGuiScreen(new ApiKeyGUI(this));
                break;
            case 5:
                // Open Map Queuer GUI
                this.mc.displayGuiScreen(new me.ballmc.gomod.gui.MapQueuerGUI(this));
                break;
            case 10:
                // Open Player Queuer GUI
                this.mc.displayGuiScreen(new me.ballmc.gomod.gui.PlayerQueuerGUI(this));
                break;
            case 6:
                // Open Team Spammer GUI
                this.mc.displayGuiScreen(new TeamSpamGUI(this));
                break;
            case 7:
                // Open Kit Levels GUI
                this.mc.displayGuiScreen(new KitLevelsGUI(this));
                break;
            case 8:
                // Open Win Requeue GUI
                this.mc.displayGuiScreen(new AutoQueueGUI());
                break;
            case 9:
                // Open TAB Stats Settings GUI
                this.mc.displayGuiScreen(new KillCounterSettingsGUI());
                break;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
