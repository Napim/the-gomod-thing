package me.ballmc.gomod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;
import me.ballmc.gomod.features.KillCounter;

public class KillCounterSettingsGUI extends GuiScreen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 30;
    // private static final int BUTTON_WIDTH = 200;
    
    // Scrolling support (like InventoryHUD)
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private static final int SCROLL_STEP = 15;
    
    private GuiButton enableButton;
    private GuiButton statsDisplayButton;
    private GuiButton colorButton;
    private GuiButton wlButton;
    private GuiButton autoKeAllButton;
    private GuiButton guildTagButton;
    private GuiButton teammateKitButton;
    private GuiButton enemyKitButton;
    private GuiButton keAllChatButton;
    private GuiButton backButton;
    
    private boolean enabled = KillCounter.isEnabled();
    private boolean statsDisplay = KillCounter.isStatsDisplay(); // Load current setting
    private boolean showColors = KillCounter.isShowColors(); // Load current setting
    private int wlMode = KillCounter.getWlMode(); // Load current setting
    private boolean autoKeAll = KillCounter.isAutoKeAll(); // Load current setting
    private boolean showGuildTag = KillCounter.isShowGuildTag(); // Load current setting
    private boolean showTeammateKits = KillCounter.isShowTeammateKits(); // Load current setting
    private boolean showEnemyKits = KillCounter.isShowEnemyKits(); // Load current setting
    private boolean showKeAllChat = KillCounter.isShowKeAllChat(); // Load current setting
    private String playerName = "PlayerName"; // Default fallback

    @Override
    public void initGui() {
        super.initGui();
        
        // Get the actual player's name
        try {
            if (Minecraft.getMinecraft().thePlayer != null && Minecraft.getMinecraft().thePlayer.getName() != null) {
                playerName = Minecraft.getMinecraft().thePlayer.getName();
            }
        } catch (Exception e) {
            // Keep default "PlayerName" if we can't get the real name
        }
        
        // Two-column layout: left labels, right controls
        int rightColumnX = width / 2 + 80;
        int baseRowStartY = 175; // Start position for buttons (moved up by 25px total)
        int buttonWidth = 120;
        
        // Calculate button positions (no scroll offset in initGui - handled in drawScreen)
        int rowStartY = baseRowStartY;
        
        // Enable/Disable button
        enableButton = new GuiButton(0, rightColumnX, rowStartY, buttonWidth, BUTTON_HEIGHT,
                enabled ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled");
        
        // Stats display button
        statsDisplayButton = new GuiButton(1, rightColumnX, rowStartY + SPACING, buttonWidth, BUTTON_HEIGHT,
                statsDisplay ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled");
        
        // Color mode button
        colorButton = new GuiButton(2, rightColumnX, rowStartY + SPACING * 2, buttonWidth, BUTTON_HEIGHT,
                showColors ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled");
        
        // WL mode button - dynamic width based on player name (no "Username Only"; disable Stats display for that)
        if (wlMode > 2) wlMode = 2; // clamp legacy value
        String[] wlPreviews = {
            EnumChatFormatting.RED + playerName + EnumChatFormatting.DARK_RED + " [0.50wl]", 
            EnumChatFormatting.RED + playerName + EnumChatFormatting.DARK_RED + " [0.50wl/1000w]", 
            EnumChatFormatting.RED + playerName + EnumChatFormatting.DARK_RED + " [1000w]"
        };
        String currentPreview = wlPreviews[wlMode];
        int wlButtonWidth = Math.max(buttonWidth, fontRendererObj.getStringWidth(currentPreview) + 20); // Add padding
        // Center the button horizontally with other buttons
        int centeredX = rightColumnX + (buttonWidth - wlButtonWidth) / 2;
        wlButton = new GuiButton(3, centeredX, rowStartY + SPACING * 3, wlButtonWidth, BUTTON_HEIGHT,
                currentPreview);
        
        // Auto /ke all button
        autoKeAllButton = new GuiButton(4, rightColumnX, rowStartY + SPACING * 4, buttonWidth, BUTTON_HEIGHT,
                autoKeAll ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled");
        
        // Guild tag button
        guildTagButton = new GuiButton(5, rightColumnX, rowStartY + SPACING * 5, buttonWidth, BUTTON_HEIGHT,
                showGuildTag ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled");
        
        // Teammate kit button
        teammateKitButton = new GuiButton(6, rightColumnX, rowStartY + SPACING * 6, buttonWidth, BUTTON_HEIGHT,
                showTeammateKits ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled");
        
        // Enemy kit button
        enemyKitButton = new GuiButton(7, rightColumnX, rowStartY + SPACING * 7, buttonWidth, BUTTON_HEIGHT,
                showEnemyKits ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled");
        
        // /ke all chat button
        keAllChatButton = new GuiButton(8, rightColumnX, rowStartY + SPACING * 8, buttonWidth, BUTTON_HEIGHT,
                showKeAllChat ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled");
        
        // Back button
        backButton = new GuiButton(99, width / 2 - 100, height - 30, 200, BUTTON_HEIGHT,
                EnumChatFormatting.GRAY + "Back to Menu");
        
        this.buttonList.add(enableButton);
        this.buttonList.add(statsDisplayButton);
        this.buttonList.add(colorButton);
        this.buttonList.add(wlButton);
        this.buttonList.add(autoKeAllButton);
        this.buttonList.add(guildTagButton);
        this.buttonList.add(teammateKitButton);
        this.buttonList.add(enemyKitButton);
        this.buttonList.add(keAllChatButton);
        this.buttonList.add(backButton);

        // Reset scroll when opening and compute max scroll based on content size
        scrollOffset = 0;
        computeMaxScroll();
    }
    
    private void computeMaxScroll() {
        // Determine the lowest Y element (content bottom)
        int baseRowStartY = 175; // Same as in initGui (moved up by 25px total)
        int contentBottom = baseRowStartY + SPACING * 8 + BUTTON_HEIGHT; // Last button area
        int visibleBottom = this.height - 30; // leave some padding at bottom
        maxScroll = Math.max(0, contentBottom - visibleBottom);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        
        // Refresh button states to reflect current KillCounter state
        refreshButtonStates();
        
        // Draw title
        String title = EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "TAB Stats Settings" + EnumChatFormatting.RESET;
        int titleWidth = fontRendererObj.getStringWidth(title);
        fontRendererObj.drawString(title, width / 2 - titleWidth / 2, 30, 0xFFFFFF);
        
        // Draw preview section showing how the name would look with all settings
        int previewY = 60;
        String previewLabel = "Preview:";
        int previewLabelWidth = fontRendererObj.getStringWidth(previewLabel);
        fontRendererObj.drawString(previewLabel, width / 2 - previewLabelWidth / 2, previewY, 0xFFFFFF);
        
        // Build the preview text based on current settings
        String previewText = buildPreviewText();
        int previewTextWidth = fontRendererObj.getStringWidth(previewText);
        fontRendererObj.drawString(previewText, width / 2 - previewTextWidth / 2, previewY + 15, 0xFFFFFF);
        
        // Draw kill effect color legend below preview
        int legendY = previewY + 40;
        String legendLabel = "Kill Effect Colors:";
        int legendLabelWidth = fontRendererObj.getStringWidth(legendLabel);
        fontRendererObj.drawString(legendLabel, width / 2 - legendLabelWidth / 2, legendY, 0xFFFFFF);
        
        // Show different kill effect colors (like /ke all)
        String[] killEffectColors = {
            EnumChatFormatting.AQUA + "Speed", // Speed (Aqua)
            EnumChatFormatting.GOLD + "Resistance", // Resistance (Gold)
            EnumChatFormatting.LIGHT_PURPLE + "Regeneration", // Regeneration (Light Purple)
            EnumChatFormatting.DARK_RED + "Flaming Arrows", // Flaming Arrows (Dark Red)
            EnumChatFormatting.YELLOW + "Level Up", // Level Up (Yellow)
            EnumChatFormatting.GREEN + "Grave digger", // Grave digger (Green)
            EnumChatFormatting.DARK_BLUE + "Random" // Random (Dark Blue)
        };
        
        // Center the color grid properly with custom positioning for the last item
        int gridWidth = 240; // Total width for 3 columns
        int startX = width / 2 - gridWidth / 2;
        for (int i = 0; i < killEffectColors.length; i++) {
            String colorText = killEffectColors[i];
            int textWidth = fontRendererObj.getStringWidth(colorText);
            int x, y;
            
            if (i == 6) { // "Random" - position it under "Level Up" (index 4)
                // Calculate position based on "Level Up" position
                int levelUpCol = 1; // Column 1 (middle)
                int levelUpRow = 1; // Row 1 (second row)
                int cellWidth = gridWidth / 3;
                int cellCenterX = startX + levelUpCol * cellWidth + cellWidth / 2;
                x = cellCenterX - textWidth / 2;
                y = legendY + (levelUpRow + 1) * 12 + 15; // One row below Level Up
            } else {
                // Normal grid positioning for all other items
                int col = i % 3; // Column (0, 1, 2)
                int row = i / 3; // Row (0, 1, 2)
                int cellWidth = gridWidth / 3; // Width per cell
                int cellCenterX = startX + col * cellWidth + cellWidth / 2;
                x = cellCenterX - textWidth / 2;
                y = legendY + row * 12 + 15;
            }
            
            fontRendererObj.drawString(colorText, x, y, 0xFFFFFF);
        }
        
        // Two-column layout: left labels, right controls
        int labelX = width / 2 - 200;
        int baseRowStartY = 175;
        int rowStartY = baseRowStartY - scrollOffset;

        // Update button positions based on scroll so they remain interactive
        enableButton.yPosition = rowStartY;
        statsDisplayButton.yPosition = rowStartY + SPACING;
        colorButton.yPosition = rowStartY + SPACING * 2;
        wlButton.yPosition = rowStartY + SPACING * 3;
        autoKeAllButton.yPosition = rowStartY + SPACING * 4;
        guildTagButton.yPosition = rowStartY + SPACING * 5;
        teammateKitButton.yPosition = rowStartY + SPACING * 6;
        enemyKitButton.yPosition = rowStartY + SPACING * 7;
        keAllChatButton.yPosition = rowStartY + SPACING * 8;
        
        // Draw labels on the left side with scroll offset
        fontRendererObj.drawString("Kill effect counter", labelX, rowStartY + 6, 0xFFFFFF);
        fontRendererObj.drawString("Stats display", labelX, rowStartY + SPACING + 6, 0xFFFFFF);
        fontRendererObj.drawString("Kill effect colors", labelX, rowStartY + SPACING * 2 + 6, 0xFFFFFF);
        fontRendererObj.drawString("Stats display format", labelX, rowStartY + SPACING * 3 + 6, 0xFFFFFF);
        fontRendererObj.drawString("Run " + EnumChatFormatting.BOLD + "/ke all" + EnumChatFormatting.RESET + " when cages open", labelX, rowStartY + SPACING * 4 + 6, 0xFFFFFF);
        fontRendererObj.drawString("Show Guild Tag", labelX, rowStartY + SPACING * 5 + 6, 0xFFFFFF);
        fontRendererObj.drawString("Show teammate's kit", labelX, rowStartY + SPACING * 6 + 6, 0xFFFFFF);
        fontRendererObj.drawString("Show enemy players' kits", labelX, rowStartY + SPACING * 7 + 6, 0xFFFFFF);
        fontRendererObj.drawString("Show /ke all info in chat", labelX, rowStartY + SPACING * 8 + 6, 0xFFFFFF);
        
        // Recompute max scroll in case window resized
        computeMaxScroll();

        // Draw a scrollbar indicator if needed (like InventoryHUD)
        if (maxScroll > 0) {
            int trackX = this.width - 10; // right side padding
            int trackWidth = 4;

            // Define scrollable region bounds
            int contentTop = baseRowStartY;
            int contentBottom = baseRowStartY + SPACING * 8 + BUTTON_HEIGHT; // Last button area

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
    
    /**
     * Refreshes button states to reflect current KillCounter settings
     */
    private void refreshButtonStates() {
        // Update local state variables from KillCounter
        enabled = KillCounter.isEnabled();
        statsDisplay = KillCounter.isStatsDisplay();
        showColors = KillCounter.isShowColors();
        wlMode = KillCounter.getWlMode();
        autoKeAll = KillCounter.isAutoKeAll();
        showGuildTag = KillCounter.isShowGuildTag();
        showTeammateKits = KillCounter.isShowTeammateKits();
        showEnemyKits = KillCounter.isShowEnemyKits();
        showKeAllChat = KillCounter.isShowKeAllChat();
        
        // Update button display strings
        if (enableButton != null) {
            enableButton.displayString = enabled ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
        }
        if (statsDisplayButton != null) {
            statsDisplayButton.displayString = statsDisplay ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
        }
        if (colorButton != null) {
            colorButton.displayString = showColors ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
        }
        if (autoKeAllButton != null) {
            autoKeAllButton.displayString = autoKeAll ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
        }
        if (guildTagButton != null) {
            guildTagButton.displayString = showGuildTag ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
        }
        if (teammateKitButton != null) {
            teammateKitButton.displayString = showTeammateKits ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
        }
        if (enemyKitButton != null) {
            enemyKitButton.displayString = showEnemyKits ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
        }
        if (keAllChatButton != null) {
            keAllChatButton.displayString = showKeAllChat ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
        }
        
        // Update WL mode button
        if (wlButton != null) {
            if (wlMode > 2) { wlMode = 2; KillCounter.setWlMode(wlMode); }
            String[] wlPreviews2 = {
                EnumChatFormatting.RED + playerName + EnumChatFormatting.DARK_RED + " [0.50wl]", 
                EnumChatFormatting.RED + playerName + EnumChatFormatting.DARK_RED + " [0.50wl/1000w]", 
                EnumChatFormatting.RED + playerName + EnumChatFormatting.DARK_RED + " [1000w]"
            };
            wlButton.displayString = wlPreviews2[wlMode];
        }
    }

    @Override
    public void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0: // Enable/Disable
                enabled = !enabled;
                KillCounter.setEnabled(enabled);
                enableButton.displayString = enabled ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
                break;
                
            case 1: // Stats display
                statsDisplay = !statsDisplay;
                KillCounter.setStatsDisplay(statsDisplay);
                statsDisplayButton.displayString = statsDisplay ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
                // Refresh tab names to apply the setting
                KillCounter.forceTabRefresh();
                break;
                
            case 2: // Color mode
                showColors = !showColors;
                KillCounter.setShowColors(showColors);
                colorButton.displayString = showColors ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
                // Refresh tab names to apply the setting
                KillCounter.forceTabRefresh();
                break;
                
            case 3: // WL mode
                wlMode = (wlMode + 1) % 3;
                String[] wlPreviews3 = {
                    EnumChatFormatting.RED + playerName + EnumChatFormatting.DARK_RED + " [0.50wl]", 
                    EnumChatFormatting.RED + playerName + EnumChatFormatting.DARK_RED + " [0.50wl/1000w]", 
                    EnumChatFormatting.RED + playerName + EnumChatFormatting.DARK_RED + " [1000w]"
                };
                String currentPreview = wlPreviews3[wlMode];
                wlButton.displayString = currentPreview;
                
                // Update button width and position dynamically based on text length
                int newWidth = Math.max(120, fontRendererObj.getStringWidth(currentPreview) + 20);
                wlButton.width = newWidth;
                // Re-center the button horizontally
                int centeredX = (width / 2 + 80) + (120 - newWidth) / 2;
                wlButton.xPosition = centeredX;
                // Apply WL mode setting to KillCounter
                KillCounter.setWlMode(wlMode);
                // Refresh tab names to apply the setting
                KillCounter.forceTabRefresh();
                break;
                
            case 4: // Auto /ke all
                autoKeAll = !autoKeAll;
                KillCounter.setAutoKeAll(autoKeAll);
                autoKeAllButton.displayString = autoKeAll ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
                break;
                
            case 5: // Guild tag
                showGuildTag = !showGuildTag;
                KillCounter.setShowGuildTag(showGuildTag);
                guildTagButton.displayString = showGuildTag ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
                // Refresh tab names to apply the setting
                KillCounter.forceTabRefresh();
                break;
                
            case 6: // Teammate kit
                showTeammateKits = !showTeammateKits;
                KillCounter.setShowTeammateKits(showTeammateKits);
                teammateKitButton.displayString = showTeammateKits ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
                // Refresh tab names to apply the setting
                KillCounter.forceTabRefresh();
                break;
                
            case 7: // Enemy kit
                showEnemyKits = !showEnemyKits;
                KillCounter.setShowEnemyKits(showEnemyKits);
                enemyKitButton.displayString = showEnemyKits ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
                // Refresh tab names to apply the setting
                KillCounter.forceTabRefresh();
                break;
                
            case 8: // /ke all chat
                showKeAllChat = !showKeAllChat;
                KillCounter.setShowKeAllChat(showKeAllChat);
                keAllChatButton.displayString = showKeAllChat ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
                break;
                
            case 99: // Back
                this.mc.displayGuiScreen(new GomodGUI());
                break;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
    
    private String buildPreviewText() {
        StringBuilder preview = new StringBuilder();
        
        // Player name with kill effect color (light red for 0.50wl)
        preview.append(EnumChatFormatting.RED).append(playerName);
        
        // Add kill counter if enabled
        if (enabled) {
            preview.append(EnumChatFormatting.DARK_RED).append(" [2]");
        }
        
        // Add stats display if enabled
        if (statsDisplay) {
            String[] wlPreviews = {" [0.50wl]", " [0.50wl/1000w]", " [1000w]", ""};
            if (wlMode < 3) { // Only add stats for modes 0-2, not for "Username Only" (mode 3)
                preview.append(EnumChatFormatting.DARK_RED).append(wlPreviews[wlMode]);
            }
        }
        
        // Add enemy kit preview if enabled (teammate kit preview removed per user request)
        if (showEnemyKits) {
            preview.append(EnumChatFormatting.YELLOW).append(" (Guardian X)");
        }
        
        return preview.toString();
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
}
