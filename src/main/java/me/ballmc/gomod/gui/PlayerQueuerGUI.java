package me.ballmc.gomod.gui;

import me.ballmc.gomod.features.PlayerQueuer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.EnumChatFormatting;

import java.util.List;

/**
 * Simple GUI for managing PlayerQueuer targets and toggle.
 */
public class PlayerQueuerGUI extends GuiScreen {
    private final GuiScreen parent;
    private GuiButton backButton;
    private GuiButton helpButton;
    private GuiButton toggleButton;
    private GuiButton includeAddButton;
    private GuiButton includeRemoveButton;
    private GuiButton includeClearButton;
    private GuiButton excludeAddButton;
    private GuiButton excludeRemoveButton;
    private GuiButton excludeClearButton;
    private GuiButton includeModeButton;
    private GuiButton excludeModeButton;
    private GuiTextField includeField;
    private GuiTextField excludeField;

    private List<String> includeList;
    private List<String> excludeList;

    // Scrolling for lists
    private int includeIndexOffset = 0;
    private int excludeIndexOffset = 0;
    private static final int ROW_HEIGHT = 12;
    private static final int SCROLL_ROWS_STEP = 3;

    public PlayerQueuerGUI(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        int centerX = width / 2;
        int panelWidth = 260;
        int gutter = 24;
        int leftX = centerX - panelWidth - gutter;
        int rightX = centerX + gutter;
        int topY = 60;

        // Status row
        // Place Enabled above Help/Back at bottom center
        toggleButton = new GuiButton(0, centerX - 60, height - 76, 120, 20,
            PlayerQueuer.isEnabled() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled");
        // Small mode buttons within panels
        includeModeButton = new GuiButton(5, leftX + panelWidth - 80, topY + 24, 80, 20,
            PlayerQueuer.isRequireAllInclude() ? EnumChatFormatting.GOLD + "ALL" : EnumChatFormatting.GREEN + "ANY");
        excludeModeButton = new GuiButton(10, rightX + panelWidth - 80, topY + 24, 80, 20,
            PlayerQueuer.isRequireAllExclude() ? EnumChatFormatting.GOLD + "ALL" : EnumChatFormatting.GREEN + "ANY");

        // Include panel controls
        // Row 1: Include field + Add button + Include mode
        includeField = new GuiTextField(1, fontRendererObj, leftX, topY, panelWidth - 90, 20);
        includeField.setMaxStringLength(16);
        includeAddButton = new GuiButton(2, leftX + panelWidth - 80, topY, 80, 20, "Add Include");
        includeModeButton = new GuiButton(5, leftX + panelWidth - 60, topY + 24, 60, 20,
            PlayerQueuer.isRequireAllInclude() ? EnumChatFormatting.GOLD + "ALL" : EnumChatFormatting.GREEN + "ANY");
        // Row 2: Remove + Clear
        includeRemoveButton = new GuiButton(3, leftX, topY + 24, 80, 20, "Remove");
        includeClearButton = new GuiButton(4, leftX + 84, topY + 24, 80, 20, "Clear All");

        // Exclude panel controls
        // Row 1: Exclude field + Add button + Exclude mode
        excludeField = new GuiTextField(7, fontRendererObj, rightX, topY, panelWidth - 90, 20);
        excludeField.setMaxStringLength(16);
        excludeAddButton = new GuiButton(8, rightX + panelWidth - 80, topY, 80, 20, "Add Exclude");
        excludeModeButton = new GuiButton(10, rightX + panelWidth - 60, topY + 24, 60, 20,
            PlayerQueuer.isRequireAllExclude() ? EnumChatFormatting.GOLD + "ALL" : EnumChatFormatting.GREEN + "ANY");
        // Row 2: Remove + Clear
        excludeRemoveButton = new GuiButton(9, rightX, topY + 24, 80, 20, "Remove");
        excludeClearButton = new GuiButton(11, rightX + 84, topY + 24, 80, 20, "Clear All");

        helpButton = new GuiButton(12, centerX - 60, height - 52, 120, 20, EnumChatFormatting.YELLOW + "Help");
        backButton = new GuiButton(6, centerX - 60, height - 28, 120, 20, EnumChatFormatting.GRAY + "Back");

        buttonList.clear();
        buttonList.add(toggleButton);
        buttonList.add(includeModeButton);
        buttonList.add(excludeModeButton);
        buttonList.add(includeAddButton);
        buttonList.add(includeRemoveButton);
        buttonList.add(includeClearButton);
        buttonList.add(excludeAddButton);
        buttonList.add(excludeRemoveButton);
        buttonList.add(excludeClearButton);
        buttonList.add(helpButton);
        buttonList.add(backButton);

        refreshLists();
        includeIndexOffset = 0;
        excludeIndexOffset = 0;
        updateButtons();
    }

    private void refreshLists() {
        includeList = PlayerQueuer.getTargets();
        excludeList = PlayerQueuer.getExcludeTargets();
    }

    private void updateButtons() {
        boolean hasText = includeField.getText() != null && !includeField.getText().trim().isEmpty();
        includeAddButton.enabled = hasText;
        includeRemoveButton.enabled = hasText;
        boolean hasExText = excludeField.getText() != null && !excludeField.getText().trim().isEmpty();
        excludeAddButton.enabled = hasExText;
        excludeRemoveButton.enabled = hasExText;
        toggleButton.displayString = PlayerQueuer.isEnabled() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
        includeModeButton.displayString = PlayerQueuer.isRequireAllInclude() ? EnumChatFormatting.GOLD + "ALL" : EnumChatFormatting.GREEN + "ANY";
        excludeModeButton.displayString = PlayerQueuer.isRequireAllExclude() ? EnumChatFormatting.GOLD + "ALL" : EnumChatFormatting.GREEN + "ANY";
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int centerX = width / 2;
        int panelWidth = 260;
        int panelHeight = height - 155; // reduce vertical extent by 15px
        int gutter = 24;
        int leftX = centerX - panelWidth - gutter;
        int rightX = centerX + gutter;
        int topY = 60;

        drawCenteredString(fontRendererObj, EnumChatFormatting.GOLD + "Player Queuer", centerX, 12, 0xFFFFFF);
        String includeInfo = PlayerQueuer.isRequireAllInclude() ? (EnumChatFormatting.RED + "Include: ALL") : (EnumChatFormatting.GREEN + "Include: ANY");
        String excludeInfo = PlayerQueuer.isRequireAllExclude() ? (EnumChatFormatting.RED + "Exclude: ALL") : (EnumChatFormatting.GREEN + "Exclude: ANY");
        String modeInfo = includeInfo + EnumChatFormatting.RESET + "  |  " + excludeInfo;
        drawCenteredString(fontRendererObj, EnumChatFormatting.YELLOW + "Modes: " + EnumChatFormatting.RESET + modeInfo, centerX, 24, 0xFFFFFF);

        // Panels
        drawPanel(leftX - 6, topY - 6, panelWidth + 12, panelHeight + 12);
        drawPanel(rightX - 6, topY - 6, panelWidth + 12, panelHeight + 12);

        // Headings (centered over each panel)
        String includeTitle = EnumChatFormatting.AQUA + "Include (queue when present)  [" + includeList.size() + "]";
        int includeTitleX = leftX + Math.max(0, (panelWidth - fontRendererObj.getStringWidth(includeTitle)) / 2);
        drawString(fontRendererObj, includeTitle, includeTitleX, topY - 18, 0xFFFFFF);
        String excludeTitle = EnumChatFormatting.RED + "Exclude (requeue if present)  [" + excludeList.size() + "]";
        int excludeTitleX = rightX + Math.max(0, (panelWidth - fontRendererObj.getStringWidth(excludeTitle)) / 2);
        drawString(fontRendererObj, excludeTitle, excludeTitleX, topY - 18, 0xFFFFFF);

        // List areas
        int listTop = topY + 54;
        int listHeight = panelHeight - 78;
        drawList(includeList, leftX, listTop, panelWidth, listHeight, includeIndexOffset);
        drawList(excludeList, rightX, listTop, panelWidth, listHeight, excludeIndexOffset);

        // Input fields
        includeField.drawTextBox();
        excludeField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawPanel(int x, int y, int w, int h) {
        int bg = 0x40000000;
        int border = 0x90FFFFFF;
        net.minecraft.client.gui.Gui.drawRect(x, y, x + w, y + h, bg);
        // simple border
        net.minecraft.client.gui.Gui.drawRect(x, y, x + w, y + 1, border);
        net.minecraft.client.gui.Gui.drawRect(x, y + h - 1, x + w, y + h, border);
        net.minecraft.client.gui.Gui.drawRect(x, y, x + 1, y + h, border);
        net.minecraft.client.gui.Gui.drawRect(x + w - 1, y, x + w, y + h, border);
    }

    private void drawList(List<String> items, int x, int y, int w, int h, int indexOffset) {
        if (items == null) return;
        int visible = Math.max(0, h / ROW_HEIGHT);
        int maxStart = Math.max(0, items.size() - visible);
        int start = Math.max(0, Math.min(indexOffset, maxStart));
        int end = Math.min(items.size(), start + visible);
        
        // Get mouse position for hover detection
        int mouseX = org.lwjgl.input.Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - org.lwjgl.input.Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        
        for (int i = start; i < end; i++) {
            int yy = y + (i - start) * ROW_HEIGHT;
            String t = items.get(i);
            
            // Check if mouse is hovering over this row
            boolean hovering = mouseX >= x && mouseX <= x + w && mouseY >= yy && mouseY <= yy + ROW_HEIGHT;
            
            // Draw alternating row background
            if ((i & 1) == 0) {
                net.minecraft.client.gui.Gui.drawRect(x, yy, x + w, yy + ROW_HEIGHT, 0x18000000);
            }
            
            // Draw hover effect (black box with opacity)
            if (hovering) {
                net.minecraft.client.gui.Gui.drawRect(x, yy, x + w, yy + ROW_HEIGHT, 0x80000000);
                // Draw delete text in red italic
                String deleteText = EnumChatFormatting.RED + "" + EnumChatFormatting.ITALIC + "Delete";
                int deleteX = x + w - fontRendererObj.getStringWidth(deleteText) - 4;
                fontRendererObj.drawString(deleteText, deleteX, yy + 2, 0xFFFFFF);
            }
            
            // Draw player name
            fontRendererObj.drawString(EnumChatFormatting.WHITE + "- " + t, x + 4, yy + 2, 0xFFFFFF);
        }
        
        // Scrollbar
        int content = items.size();
        if (content > visible) {
            int trackX = x + w - 4;
            int trackTop = y;
            int trackHeight = h;
            int thumbHeight = Math.max(20, (int)(trackHeight * (visible / (float)content)));
            int maxTravel = trackHeight - thumbHeight;
            int thumbY = trackTop + (int)(maxTravel * (start / (float)Math.max(1, content - visible)));
            net.minecraft.client.gui.Gui.drawRect(trackX, trackTop, trackX + 3, trackTop + trackHeight, 0x40000000);
            net.minecraft.client.gui.Gui.drawRect(trackX, thumbY, trackX + 3, thumbY + thumbHeight, 0x90FFFFFF);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);
        includeField.textboxKeyTyped(typedChar, keyCode);
        excludeField.textboxKeyTyped(typedChar, keyCode);
        
        // Handle ENTER key to add players
        if (keyCode == 28) { // ENTER key
            if (includeField.isFocused()) {
                String name = includeField.getText().trim();
                if (!name.isEmpty()) {
                    PlayerQueuer.addTargetSilent(name);
                    includeField.setText("");
                    refreshLists();
                    includeIndexOffset = 0;
                    updateButtons();
                }
            } else if (excludeField.isFocused()) {
                String name = excludeField.getText().trim();
                if (!name.isEmpty()) {
                    PlayerQueuer.addExcludeTargetSilent(name);
                    excludeField.setText("");
                    refreshLists();
                    excludeIndexOffset = 0;
                    updateButtons();
                }
            }
        }
        
        updateButtons();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        includeField.mouseClicked(mouseX, mouseY, mouseButton);
        excludeField.mouseClicked(mouseX, mouseY, mouseButton);
        
        // Handle clicks on player list items for deletion
        if (mouseButton == 0) { // Left click
            int centerX = width / 2;
            int panelWidth = 260;
            int panelHeight = height - 155;
            int gutter = 24;
            int leftX = centerX - panelWidth - gutter;
            int rightX = centerX + gutter;
            int topY = 60;
            int listTop = topY + 54;
            int listHeight = panelHeight - 78;
            
            // Check include list clicks
            if (mouseX >= leftX && mouseX <= leftX + panelWidth && mouseY >= listTop && mouseY <= listTop + listHeight) {
                handleListClick(includeList, leftX, listTop, panelWidth, listHeight, includeIndexOffset, true);
            }
            // Check exclude list clicks
            else if (mouseX >= rightX && mouseX <= rightX + panelWidth && mouseY >= listTop && mouseY <= listTop + listHeight) {
                handleListClick(excludeList, rightX, listTop, panelWidth, listHeight, excludeIndexOffset, false);
            }
        }
        
        updateButtons();
    }
    
    private void handleListClick(List<String> items, int x, int y, int w, int h, int indexOffset, boolean isIncludeList) {
        if (items == null || items.isEmpty()) return;
        
        int visible = Math.max(0, h / ROW_HEIGHT);
        int maxStart = Math.max(0, items.size() - visible);
        int start = Math.max(0, Math.min(indexOffset, maxStart));
        int end = Math.min(items.size(), start + visible);
        
        int mouseX = org.lwjgl.input.Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - org.lwjgl.input.Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        
        for (int i = start; i < end; i++) {
            int yy = y + (i - start) * ROW_HEIGHT;
            
            // Check if click is on this row
            if (mouseX >= x && mouseX <= x + w && mouseY >= yy && mouseY <= yy + ROW_HEIGHT) {
                String playerName = items.get(i);
                if (isIncludeList) {
                    PlayerQueuer.removeTargetSilent(playerName);
                    refreshLists();
                    includeIndexOffset = 0;
                } else {
                    PlayerQueuer.removeExcludeTargetSilent(playerName);
                    refreshLists();
                    excludeIndexOffset = 0;
                }
                updateButtons();
                break;
            }
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int d = org.lwjgl.input.Mouse.getEventDWheel();
        if (d != 0) {
            int panelWidth = 200;
            int panelHeight = height - 160;
            int gutter = 24;
            int centerX = width / 2;
            int leftX = centerX - panelWidth - gutter;
            int rightX = centerX + gutter;
            int topY = 60;
            int listTop = topY + 54;
            int listHeight = panelHeight - 72;
            int mouseX = org.lwjgl.input.Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - org.lwjgl.input.Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

            // Determine which panel mouse is over
            boolean overInclude = mouseX >= leftX && mouseX <= leftX + panelWidth && mouseY >= listTop && mouseY <= listTop + listHeight;
            boolean overExclude = mouseX >= rightX && mouseX <= rightX + panelWidth && mouseY >= listTop && mouseY <= listTop + listHeight;
            int deltaRows = d > 0 ? -SCROLL_ROWS_STEP : SCROLL_ROWS_STEP;
            if (overInclude) {
                int visible = Math.max(0, listHeight / ROW_HEIGHT);
                int maxStart = Math.max(0, includeList.size() - visible);
                includeIndexOffset = Math.max(0, Math.min(maxStart, includeIndexOffset + deltaRows));
            } else if (overExclude) {
                int visible = Math.max(0, listHeight / ROW_HEIGHT);
                int maxStart = Math.max(0, excludeList.size() - visible);
                excludeIndexOffset = Math.max(0, Math.min(maxStart, excludeIndexOffset + deltaRows));
            }
        }
    }

    @Override
    public void actionPerformed(GuiButton button) {
        if (button == backButton) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (button == helpButton) {
            mc.displayGuiScreen(new HelpDialog(this));
            return;
        }
        if (button == toggleButton) {
            PlayerQueuer.setEnabledSilent(!PlayerQueuer.isEnabled());
            updateButtons();
            return;
        }
        if (button == includeAddButton) {
            String name = includeField.getText().trim();
            if (!name.isEmpty()) {
                PlayerQueuer.addTargetSilent(name);
                includeField.setText("");
                refreshLists();
                includeIndexOffset = 0;
                updateButtons();
            }
            return;
        }
        if (button == includeRemoveButton) {
            String name = includeField.getText().trim();
            if (!name.isEmpty()) {
                PlayerQueuer.removeTargetSilent(name);
                includeField.setText("");
                refreshLists();
                includeIndexOffset = 0;
                updateButtons();
            }
            return;
        }
        if (button == includeClearButton) {
            PlayerQueuer.clearTargetsSilent();
            refreshLists();
            includeIndexOffset = 0;
            updateButtons();
            return;
        }
        if (button == includeModeButton) {
            PlayerQueuer.setRequireAllIncludeSilent(!PlayerQueuer.isRequireAllInclude());
            includeModeButton.displayString = PlayerQueuer.isRequireAllInclude() ? EnumChatFormatting.GOLD + "ALL" : EnumChatFormatting.GREEN + "ANY";
            updateButtons();
            return;
        }
        if (button == excludeModeButton) {
            PlayerQueuer.setRequireAllExcludeSilent(!PlayerQueuer.isRequireAllExclude());
            excludeModeButton.displayString = PlayerQueuer.isRequireAllExclude() ? EnumChatFormatting.GOLD + "ALL" : EnumChatFormatting.GREEN + "ANY";
            updateButtons();
            return;
        }
        if (button == excludeAddButton) { // ExAdd
            String name = excludeField.getText().trim();
            if (!name.isEmpty()) {
                PlayerQueuer.addExcludeTargetSilent(name);
                excludeField.setText("");
                refreshLists();
                excludeIndexOffset = 0;
                updateButtons();
            }
            return;
        }
        if (button == excludeRemoveButton) { // ExRem
            String name = excludeField.getText().trim();
            if (!name.isEmpty()) {
                PlayerQueuer.removeExcludeTargetSilent(name);
                excludeField.setText("");
                refreshLists();
                excludeIndexOffset = 0;
                updateButtons();
            }
            return;
        }
        if (button == excludeClearButton) {
            PlayerQueuer.clearExcludeTargetsSilent();
            refreshLists();
            excludeIndexOffset = 0;
            updateButtons();
            return;
        }
    }

    // Simple help dialog screen
    private static class HelpDialog extends GuiScreen {
        private final GuiScreen parent;
        private GuiButton closeButton;

        public HelpDialog(GuiScreen parent) {
            this.parent = parent;
        }

        @Override
        public void initGui() {
            int centerX = width / 2;
            closeButton = new GuiButton(0, centerX - 50, height - 40, 100, 20, EnumChatFormatting.GRAY + "Close");
            buttonList.clear();
            buttonList.add(closeButton);
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();
            int centerX = width / 2;
            int y = 40;
            drawCenteredString(fontRendererObj, EnumChatFormatting.GOLD + "Player Queuer - Help", centerX, y, 0xFFFFFF);
            y += 16;
            drawWrapped(EnumChatFormatting.WHITE + "Include: If mode = ANY, stay when any included player is in TAB. If mode = ALL, stay only if all included players are in TAB.", centerX, y); y += 32;
            drawWrapped(EnumChatFormatting.WHITE + "Exclude: If any excluded player is in TAB, requeue immediately (takes priority).", centerX, y); y += 32;
            drawWrapped(EnumChatFormatting.WHITE + "Trigger: Acts when the chat says 'The game starts in 1 second!'.", centerX, y); y += 32;
            drawWrapped(EnumChatFormatting.WHITE + "Buttons: Add/Remove/Clear for each list; Mode toggles ANY/ALL; Enabled turns the feature on/off.", centerX, y);
            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        private void drawWrapped(String text, int centerX, int topY) {
            int maxWidth = Math.min(420, width - 40);
            java.util.List<String> lines = fontRendererObj.listFormattedStringToWidth(text, maxWidth);
            int y = topY;
            for (String line : lines) {
                drawCenteredString(fontRendererObj, line, centerX, y, 0xFFFFFF);
                y += 12;
            }
        }

        @Override
        public void actionPerformed(GuiButton button) {
            if (button == closeButton) {
                mc.displayGuiScreen(parent);
            }
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }
}


