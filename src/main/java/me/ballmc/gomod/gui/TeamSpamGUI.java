package me.ballmc.gomod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import me.ballmc.gomod.features.TeamSpamManager;
import me.ballmc.gomod.features.TeamSpamManager.Player;
import me.ballmc.gomod.command.TeamSpamCommand;
import me.ballmc.gomod.Main;

import java.util.List;
import java.util.Collections;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TeamSpamGUI extends GuiScreen {
    private GuiScreen parent;
    private GuiButton backButton;
    private GuiButton addPlayerButton;
    private GuiButton removePlayerButton;
    private GuiButton refreshButton;
    private GuiButton clearButton;
    private GuiButton toggleSpamButton;
    
    // Static reference for external refresh
    private static TeamSpamGUI currentInstance = null;
    
    private GuiTextField playerNameField;
    private GuiTextField removePlayerField;
    
    private List<Player> players;
    private int scrollOffset = 0;
    private static final int PLAYERS_PER_PAGE = 18;
    // Scrollbar properties (matching InventoryHUD)
    private int maxScroll = 0;
    private static final int SCROLL_STEP = 3;
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
    // Refresh animation state (static so it persists across GUI reopen)
    private static volatile boolean isRefreshing = false;
    private static volatile int refreshingIndex = -1;
    // Start spam visual state
    private volatile boolean isSpamming = false;
    private volatile String currentInvited = "";
    private volatile String lastProcessedFound = "";
    private static final java.util.concurrent.ConcurrentHashMap<String, Long> foundHighlightUntil = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long FOUND_HIGHLIGHT_MS = 15L * 60L * 1000L; // 15 minutes
    
    public TeamSpamGUI(GuiScreen parent) {
        this.parent = parent;
        currentInstance = this;
    }
    
    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        if (currentInstance == this) {
            currentInstance = null;
        }
    }
    
    /**
     * Static method to refresh the current GUI instance
     */
    public static void refreshCurrentGUI() {
        if (currentInstance != null) {
            currentInstance.refreshPlayerList();
            System.out.println("[TeamSpamGUI] Refreshed current GUI instance");
        }
    }
    
    @Override
    public void initGui() {
        int centerX = width / 2;
        int startY = 30;
        
        // Load players and sort them using the same logic as TeamSpamCommand
        players = TeamSpamManager.getPlayers();
        // Sort players using the SAME logic as TeamSpamCommand: most recently found first, then most recently added, then MVP++ first, then alphabetically
        Collections.sort(players, (p1, p2) -> {
            // Primary: most recently found first (only players who were actually found online)
            long found1 = p1.getLastFoundTimestamp();
            long found2 = p2.getLastFoundTimestamp();
            boolean p1HasFound = found1 > 0;
            boolean p2HasFound = found2 > 0;

            // Players with a lastFoundTimestamp appear before those without
            if (p1HasFound && !p2HasFound) return -1;
            if (!p1HasFound && p2HasFound) return 1;

            // If both have found timestamps, newest first
            if (p1HasFound && p2HasFound && found1 != found2) {
                return Long.compare(found2, found1);
            }

            // Secondary: most recently added first among players with same found status
            long added1 = p1.getAddedTimestamp();
            long added2 = p2.getAddedTimestamp();
            int addedCompare = Long.compare(added2, added1);
            if (addedCompare != 0) return addedCompare;

            // Tertiary: MVP++ first
            if (p1.hasMVPPlusPlus() && !p2.hasMVPPlusPlus()) return -1;
            if (!p1.hasMVPPlusPlus() && p2.hasMVPPlusPlus()) return 1;

            // Quaternary: alphabetical by username
            return p1.getUsername().compareToIgnoreCase(p2.getUsername());
        });
        
        // Back button
        backButton = new GuiButton(0, centerX - 75, height - 30, 150, 20, "Back to Menu");
        
        // Single player edit field with Add/Remove buttons (centered box)
        playerNameField = new GuiTextField(1, fontRendererObj, centerX - 80, startY, 160, 20);
        playerNameField.setMaxStringLength(16);
        // Switch positions: Add on right, Remove on left
        removePlayerButton = new GuiButton(4, centerX - 160 - 10, startY, 80, 20, "Remove");
        removePlayerField = new GuiTextField(3, fontRendererObj, 0, 0, 0, 0); // unused now
        addPlayerButton = new GuiButton(2, centerX + 90, startY, 80, 20, "Add");
        
        // Initially disable both buttons until text is entered
        addPlayerButton.enabled = false;
        removePlayerButton.enabled = false;
        
        // Toggle Spam button (single centered)
        String toggleLabel = me.ballmc.gomod.command.TeamSpamCommand.isRunning() ? (EnumChatFormatting.RED + "Stop Spam") : (EnumChatFormatting.GREEN + "Start Spam");
        toggleSpamButton = new GuiButton(8, centerX - 60, startY + 60, 120, 20, toggleLabel);
        
        // Control buttons - same level as Start/Stop
        refreshButton = new GuiButton(5, centerX - 100, startY + 90, 80, 20, "Refresh");
        clearButton = new GuiButton(6, centerX + 20, startY + 90, 80, 20, "Clear All");
        
        // Initialize scroll
        scrollOffset = 0;
        computeMaxScroll();
        
        // Add all buttons
        buttonList.add(backButton);
        buttonList.add(addPlayerButton);
        buttonList.add(removePlayerButton);
        buttonList.add(refreshButton);
        buttonList.add(clearButton);
        buttonList.add(toggleSpamButton);
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        
        int centerX = width / 2;
        int startY = 30;
        
        // Title
        drawCenteredString(fontRendererObj, EnumChatFormatting.GOLD + "Teams Nick Finder", centerX, 10, 0xFFFFFF);
        
        // Add player section
        drawCenteredString(fontRendererObj, "Add Player:", centerX, startY + 5, 0xFFFFFF);
        playerNameField.drawTextBox();
        
        // Removed old secondary remove label/box
        
        // Player list
        drawCenteredString(fontRendererObj, "Players in List (" + players.size() + "):", centerX, startY + 120, 0xFFFFFF);
        
        // Display players with scroll offset
        int baseY = startY + 140;
        int endIndex = Math.min(scrollOffset + PLAYERS_PER_PAGE, players.size());
        int leftX = centerX - 220;
        int rightX = centerX + 220;
        
        // Prevent overlapping with back button: limit visible bottom to above the back button
        int visibleBottom = height - 40; // 10px above back button
        
        for (int i = scrollOffset; i < endIndex; i++) {
            Player player = players.get(i);
            int fontH = fontRendererObj.FONT_HEIGHT;
            int yPos = baseY + (i - scrollOffset) * 15;
            boolean hovered = (mouseX >= leftX && mouseX <= rightX && mouseY >= yPos && mouseY <= yPos + fontH);
            String numberPrefix = (i + 1) + ". ";
            String displayPrefix = hovered ? (EnumChatFormatting.GREEN + ">> " + EnumChatFormatting.RESET) : numberPrefix;
            String playerText;
            
            // Get player rank with proper colors
            String rank = getPlayerRank(player);
            if (!rank.isEmpty() && !rank.equals("Default")) {
                String usernameColor = getUsernameColor(rank);
                
                // Handle special coloring for MVP+ and MVP++
                if (rank.equals("MVP+")) {
                    String baseColor = EnumChatFormatting.AQUA + ""; // MVP part
                    String plusColor = getMVPPlusColor(player.getRankPlusColor()); // + part
                    playerText = baseColor + "[MVP" + plusColor + "+" + baseColor + "] " + usernameColor + player.getUsername();
                } else if (rank.equals("MVP++")) {
                    // MVP++: monthlyRankColor controls [MVP] and username color, rankPlusColor controls ++ color
                    String baseColor = getMVPPlusPlusColor(player.getMonthlyRankColor());
                    String plusColor = getMVPPlusPlusSignsColor(player.getRankPlusColor());
                    playerText = baseColor + "[MVP" + plusColor + "++" + baseColor + "] " + baseColor + player.getUsername();
                } else {
                    String rankColor = getRankColor(rank, player);
                    playerText = rankColor + "[" + rank + "] " + usernameColor + player.getUsername();
                }
            } else if (rank.equals("Default")) {
                // Show only gray username when no rank
                playerText = EnumChatFormatting.GRAY + player.getUsername();
            } else {
                playerText = EnumChatFormatting.WHITE + player.getUsername();
            }
            
            // yPos already computed above
            
            // Stop rendering if we would overlap the back button area
            if (yPos + 8 > visibleBottom) {
                break;
            }
            
            // Determine suffix based on hover
            if (hovered) {
                long lastUpdated = player.getLastFoundTimestamp() > 0 ? player.getLastFoundTimestamp() : player.getAddedTimestamp();
                playerText += EnumChatFormatting.RESET.toString() + EnumChatFormatting.GRAY + " (Last updated: " + 
                             dateFormat.format(new Date(lastUpdated)) + ")";
            } else {
                playerText += EnumChatFormatting.RESET.toString() + EnumChatFormatting.GRAY + " (Added: " + 
                             dateFormat.format(new Date(player.getAddedTimestamp())) + ")";
            }
            
            // Hover highlight with consistent horizontal width and extra top padding
            if (hovered) {
                int startX = leftX - 6; // extend horizontally so all rows align
                int endX = rightX + 6;
                int topY = yPos - 4; // a little more above the text
                int bottomY = yPos + fontH + 2; // slight padding below
                net.minecraft.client.gui.Gui.drawRect(startX, topY, endX, bottomY, 0x40000000);
            }
            // Refresh progress overlay (green) from top to current refreshing index
            if (TeamSpamGUI.isRefreshing) {
                int idx = i;
                int startX = leftX - 6;
                int endX = rightX + 6;
                int topY = yPos - 2;
                int bottomY = yPos + fontH + 1;
                if (idx < refreshingIndex) {
                    // Completed rows: darker green
                    net.minecraft.client.gui.Gui.drawRect(startX, topY, endX, bottomY, 0x4022AA22);
                } else if (idx == refreshingIndex) {
                    // Current row: brighter green
                    net.minecraft.client.gui.Gui.drawRect(startX, topY, endX, bottomY, 0x6044FF44);
                }
            }
            // Start spam overlay: highlight current invited and any recently found players
            long now = System.currentTimeMillis();
            // Pull latest values from command (thread-safe enough for GUI)
            currentInvited = TeamSpamCommand.lastInvitedPlayer;
            String latestFound = TeamSpamCommand.lastFoundPlayer;
            if (latestFound != null && !latestFound.isEmpty() && !latestFound.equalsIgnoreCase(lastProcessedFound)) {
                foundHighlightUntil.put(latestFound.toLowerCase(), now + FOUND_HIGHLIGHT_MS);
                lastProcessedFound = latestFound;
            }
            // Current invited row: blue overlay (persist across reopen by checking lastInvitedPlayer)
            boolean effectiveSpamming = isSpamming || (currentInvited != null && !currentInvited.isEmpty());
            if (effectiveSpamming && currentInvited != null && !currentInvited.isEmpty() && player.getUsername().equalsIgnoreCase(currentInvited)) {
                int startX2 = leftX - 6;
                int endX2 = rightX + 6;
                int topY2 = yPos - 2;
                int bottomY2 = yPos + fontH + 1;
                net.minecraft.client.gui.Gui.drawRect(startX2, topY2, endX2, bottomY2, 0x604488FF);
            }
            // Found rows: persistent green overlay
            Long until = foundHighlightUntil.get(player.getUsername().toLowerCase());
            if (until != null) {
                if (until > now) {
                    int startX3 = leftX - 6;
                    int endX3 = rightX + 6;
                    int topY3 = yPos - 2;
                    int bottomY3 = yPos + fontH + 1;
                    net.minecraft.client.gui.Gui.drawRect(startX3, topY3, endX3, bottomY3, 0x6033DD33);
                    // Show tooltip with nickname when hovering a found row
                    if (hovered) {
                        // Only show tooltip if we actually have a different nick
                        String real = player.getUsername();
                        String nick = me.ballmc.gomod.features.TeamInviteResponseHandler.REAL_TO_NICK.get(real.toLowerCase());
                        if (nick != null && !nick.equalsIgnoreCase(real)) {
                            java.util.List<String> tip = new java.util.ArrayList<>();
                            String coloredNick = me.ballmc.gomod.features.TeamInviteResponseHandler.REAL_TO_NICK_COLORED.get(real.toLowerCase());
                            if (coloredNick == null || coloredNick.isEmpty()) {
                                coloredNick = buildDisplayNameFor(player, nick);
                            }
                            // Orange label, then full colored nick, ensuring proper reset
                            tip.add(EnumChatFormatting.GOLD + "Nickname: " + EnumChatFormatting.RESET + coloredNick);
                            // Draw slightly offset above the suffix to avoid overlap
                            this.drawHoveringText(tip, mouseX, Math.max(0, mouseY - 12));
                        }
                    }
                } else {
                    foundHighlightUntil.remove(player.getUsername().toLowerCase());
                }
            }
            // Center the entire row: prefix + head + name
            int headSize = 10;
            int prefixWidth = fontRendererObj.getStringWidth(displayPrefix);
            int nameWidth = fontRendererObj.getStringWidth(playerText);
            int totalWidth = prefixWidth + 4 + headSize + 4 + nameWidth;
            int rowStartX = centerX - (totalWidth / 2);
            int prefixX = rowStartX;
            int headX = prefixX + prefixWidth + 4;
            int textX = headX + headSize + 4;
            int headY = yPos - 2;
            fontRendererObj.drawStringWithShadow(displayPrefix, prefixX, yPos, 0xFFFFFF);
            drawPlayerHead(player.getUuid(), headX, headY + 1, headSize);
            fontRendererObj.drawStringWithShadow(playerText, textX, yPos, 0xFFFFFF);
            // skip centered draw for this row
            continue;
        }
        
        // Draw scrollbar (matching InventoryHUD style)
        drawScrollbar();
        
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    private String buildDisplayName(Player p) {
        String rank = getPlayerRank(p);
        if (!rank.isEmpty() && !rank.equals("Default")) {
            String usernameColor = getUsernameColor(rank);
            if (rank.equals("MVP+")) {
                String baseColor = EnumChatFormatting.AQUA + "";
                String plusColor = getMVPPlusColor(p.getRankPlusColor());
                return baseColor + "[MVP" + plusColor + "+" + baseColor + "] " + usernameColor + p.getUsername() + EnumChatFormatting.RESET;
            } else if (rank.equals("MVP++")) {
                String baseColor = getMVPPlusPlusColor(p.getMonthlyRankColor());
                String plusColor = getMVPPlusPlusSignsColor(p.getRankPlusColor());
                return baseColor + "[MVP" + plusColor + "++" + baseColor + "] " + baseColor + p.getUsername() + EnumChatFormatting.RESET;
            } else {
                String rankColor = getRankColor(rank, p);
                return rankColor + "[" + rank + "] " + usernameColor + p.getUsername() + EnumChatFormatting.RESET;
            }
        }
        // No rank: gray username per request
        return EnumChatFormatting.GRAY + p.getUsername() + EnumChatFormatting.RESET;
    }

    private String buildDisplayNameFor(Player p, String displayName) {
        String rank = getPlayerRank(p);
        if (!rank.isEmpty() && !rank.equals("Default")) {
            String usernameColor = getUsernameColor(rank);
            if (rank.equals("MVP+")) {
                String baseColor = EnumChatFormatting.AQUA + "";
                String plusColor = getMVPPlusColor(p.getRankPlusColor());
                return baseColor + "[MVP" + plusColor + "+" + baseColor + "] " + usernameColor + displayName + EnumChatFormatting.RESET;
            } else if (rank.equals("MVP++")) {
                String baseColor = getMVPPlusPlusColor(p.getMonthlyRankColor());
                String plusColor = getMVPPlusPlusSignsColor(p.getRankPlusColor());
                return baseColor + "[MVP" + plusColor + "++" + baseColor + "] " + baseColor + displayName + EnumChatFormatting.RESET;
            } else {
                String rankColor = getRankColor(rank, p);
                return rankColor + "[" + rank + "] " + usernameColor + displayName + EnumChatFormatting.RESET;
            }
        }
        return EnumChatFormatting.GRAY + displayName + EnumChatFormatting.RESET;
    }

    @Override
    public void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0: // Back
                mc.displayGuiScreen(parent);
                break;
            
            case 2: // Add Player
                if (!button.enabled) {
                    return; // Button is disabled, don't process
                }
                String playerName = playerNameField.getText().trim();
                if (!playerName.isEmpty()) {
                    final String usernameToAdd = playerName;
                    Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Adding player " + usernameToAdd + "...");
                    new Thread(() -> {
                        String result = TeamSpamManager.addPlayer(usernameToAdd);
                        Main.sendMessage(result);
                        refreshPlayerListAndScrollToTop();
                    }).start();
                    playerNameField.setText("");
                    // Disable button again after adding
                    addPlayerButton.enabled = false;
                }
                break;
            
            case 4: // Remove Player
                if (!button.enabled) {
                    return; // Button is disabled, don't process
                }
                String removeName = playerNameField.getText().trim();
                removeName = sanitizeName(removeName);
                if (!removeName.isEmpty()) {
                    String result = TeamSpamManager.removePlayer(removeName);
                    Main.sendMessage(result);
                    playerNameField.setText("");
                    refreshPlayerList();
                    // Disable both buttons again after removing
                    addPlayerButton.enabled = false;
                    removePlayerButton.enabled = false;
                }
                break;
            
            case 5: // Refresh
                if (TeamSpamGUI.isRefreshing) break;
                // Show cooldown info if not allowed yet
                if (!me.ballmc.gomod.command.TeamSpamCommand.RefreshCooldownPublic.allow()) {
                    long waitMs = me.ballmc.gomod.command.TeamSpamCommand.RefreshCooldownPublic.timeRemainingMs();
                    long mins = waitMs / 60000;
                    long secs = (waitMs % 60000) / 1000;
                    Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Refresh is on cooldown. Try again in " + mins + "m " + secs + "s.");
                    break;
                }
                Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Refreshing player list... This might take a while.");
                TeamSpamGUI.isRefreshing = true;
                TeamSpamGUI.refreshingIndex = -1;
                // Snapshot of current order as displayed
                final java.util.List<Player> snapshot = new java.util.ArrayList<>(players);
                new Thread(() -> {
                    final java.util.List<RefreshChange> changes = new java.util.ArrayList<>();
                    int idx = 0;
                    for (Player p : snapshot) {
                        try {
                            TeamSpamGUI.refreshingIndex = idx;
                            // GUI ticks and redraws every frame; avoid re-opening the screen which resets scroll
                            // Update rank/name info per player
                            String beforeDisplay = buildDisplayName(p);
                            TeamSpamManager.updatePlayerInfo(p.getUsername());
                            // Rebuild after-display using potentially updated player data
                            String afterDisplay = buildDisplayName(p);
                            if (!afterDisplay.equals(beforeDisplay)) {
                                changes.add(new RefreshChange(p.getUsername(), beforeDisplay, afterDisplay));
                            }
                            // Optionally log per player
                            // Main.sendMessage(res);
                            // Ensure 0.1s pacing per player
                            try { Thread.sleep(100L); } catch (InterruptedException ignored) {}
                        } catch (Exception ignored) {}
                        idx++;
                    }
                    TeamSpamGUI.isRefreshing = false;
                    TeamSpamGUI.refreshingIndex = -1;
                    refreshPlayerList();
                    final int changedCount = changes.size();
                    Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN + "Refresh complete. " + EnumChatFormatting.YELLOW + "(" + changedCount + " updated)");
                    // Open results dialog on the main thread
                    try {
                        Minecraft.getMinecraft().addScheduledTask(() -> {
                            if (mc != null) {
                                mc.displayGuiScreen(new RefreshResultsDialog(TeamSpamGUI.this, changes));
                            }
                        });
                    } catch (Throwable t) {
                        // Fallback: open directly (may still work on client thread)
                        if (mc != null) {
                            mc.displayGuiScreen(new RefreshResultsDialog(TeamSpamGUI.this, changes));
                        }
                    }
                }).start();
                break;
            
            case 6: // Clear All
                // Show confirmation dialog
                mc.displayGuiScreen(new net.minecraft.client.gui.GuiYesNo(
                    (result, id) -> {
                        if (result) {
                            TeamSpamManager.clearPlayers();
                            Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN + "Cleared all players from team spam list.");
                            refreshPlayerList();
                        }
                        mc.displayGuiScreen(this);
                    },
                    EnumChatFormatting.RED + "Clear All Players?",
                    EnumChatFormatting.YELLOW + "This will permanently remove ALL players from your team spam list.\n\n" +
                    EnumChatFormatting.WHITE + "Players removed: " + EnumChatFormatting.RED + players.size() + "\n" +
                    EnumChatFormatting.GRAY + "This action cannot be undone!",
                    0
                ));
                break;
            
            case 8: // Toggle Spam
                // Run synchronously so the state is updated before we refresh the label
                TeamSpamCommand command = new TeamSpamCommand();
                command.handle(new String[0]); // Toggle command
                boolean nowRunning = me.ballmc.gomod.command.TeamSpamCommand.isRunning();
                isSpamming = nowRunning;
                toggleSpamButton.displayString = nowRunning ? (EnumChatFormatting.RED + "Stop Spam") : (EnumChatFormatting.GREEN + "Start Spam");
                break;
        }
    }
    
    @Override
    public void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);
        playerNameField.textboxKeyTyped(typedChar, keyCode);
        removePlayerField.textboxKeyTyped(typedChar, keyCode);
        
        // Enable/disable Add and Remove Player buttons based on text field content
        if (addPlayerButton != null) {
            addPlayerButton.enabled = !playerNameField.getText().trim().isEmpty();
        }
        if (removePlayerButton != null) {
            removePlayerButton.enabled = !playerNameField.getText().trim().isEmpty();
        }
        
        if (keyCode == 28) { // Enter key
            if (addPlayerButton.enabled) {
                actionPerformed(addPlayerButton);
            }
        }
    }
    
    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        playerNameField.mouseClicked(mouseX, mouseY, mouseButton);
        removePlayerField.mouseClicked(mouseX, mouseY, mouseButton);

        // Detect clicks on player rows to prompt Update/Delete
        int centerX = width / 2;
        int startY = 30;
        int baseY = startY + 140;
        int endIndex = Math.min(scrollOffset + PLAYERS_PER_PAGE, players.size());
        int rowHeight = 15;
        // Clickable horizontal bounds around the centered list text
        int leftX = centerX - 220;
        int rightX = centerX + 220;
        int visibleBottom = height - 40; // stop before back button

        for (int i = scrollOffset; i < endIndex; i++) {
            int yPos = baseY + (i - scrollOffset) * rowHeight;
            if (yPos + 8 > visibleBottom) break; // don't allow clicks below visible area
            int fontH = fontRendererObj.FONT_HEIGHT;
            // Only clickable if the same hover condition that shows the highlight is true
            boolean hovered = (mouseX >= leftX && mouseX <= rightX && mouseY >= yPos && mouseY <= yPos + fontH);
            if (hovered) {
                final Player clickedPlayer = players.get(i);
                // Pass the raw username to avoid rank tags confusing backend lookups
                mc.displayGuiScreen(new PlayerActionDialog(this, clickedPlayer.getUsername(), clickedPlayer.getUuid()));
                break;
            }
        }
    }
    
    @Override
    public void updateScreen() {
        super.updateScreen();
        playerNameField.updateCursorCounter();
        removePlayerField.updateCursorCounter();
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
            // Ensure we don't scroll beyond the actual content
            int maxPossibleOffset = Math.max(0, players.size() - PLAYERS_PER_PAGE);
            scrollOffset = Math.min(scrollOffset, maxPossibleOffset);
        }
    }
    
    public void refreshPlayerList() {
        players = TeamSpamManager.getPlayers();
        // Sort players using the SAME logic as TeamSpamCommand: most recently found first, then most recently added, then MVP++ first, then alphabetically
        Collections.sort(players, (p1, p2) -> {
            // Primary: most recently found first (only players who were actually found online)
            long found1 = p1.getLastFoundTimestamp();
            long found2 = p2.getLastFoundTimestamp();
            boolean p1HasFound = found1 > 0;
            boolean p2HasFound = found2 > 0;

            // Players with a lastFoundTimestamp appear before those without
            if (p1HasFound && !p2HasFound) return -1;
            if (!p1HasFound && p2HasFound) return 1;

            // If both have found timestamps, newest first
            if (p1HasFound && p2HasFound && found1 != found2) {
                return Long.compare(found2, found1);
            }

            // Secondary: most recently added first among players with same found status
            long added1 = p1.getAddedTimestamp();
            long added2 = p2.getAddedTimestamp();
            int addedCompare = Long.compare(added2, added1);
            if (addedCompare != 0) return addedCompare;

            // Tertiary: MVP++ first
            if (p1.hasMVPPlusPlus() && !p2.hasMVPPlusPlus()) return -1;
            if (!p1.hasMVPPlusPlus() && p2.hasMVPPlusPlus()) return 1;

            // Quaternary: alphabetical by username
            return p1.getUsername().compareToIgnoreCase(p2.getUsername());
        });
        
        // Debug: Log the GUI sorted order
        System.out.println("[TeamSpamGUI] GUI sorted player order:");
        for (int i = 0; i < Math.min(5, players.size()); i++) {
            Player p = players.get(i);
            System.out.println("  " + (i+1) + ". " + p.getUsername() + " (found: " + p.getLastFoundTimestamp() + ")");
        }
        
        // Reset scroll if needed
        if (scrollOffset >= players.size()) {
            scrollOffset = Math.max(0, players.size() - PLAYERS_PER_PAGE);
        }
        
        // Update max scroll
        computeMaxScroll();
    }
    
    private void refreshPlayerListAndScrollToTop() {
        refreshPlayerList();
        scrollOffset = 0; // Scroll to top to show newly added player
    }
    
    private String getPlayerRank(Player player) {
        String rank = player.getRank();
        if (rank != null && !rank.isEmpty()) {
            return rank;
        }
        // Fallback to old method if rank is not set
        if (player.hasMVPPlusPlus()) {
            return "MVP++";
        }
        return "Default";
    }
    
    private String getRankColor(String rank, Player player) {
        switch (rank) {
            case "OWNER":
                return EnumChatFormatting.DARK_RED + ""; // &4
            case "ADMIN":
                return EnumChatFormatting.RED + ""; // &c
            case "GM":
                return EnumChatFormatting.DARK_GREEN + ""; // &2
            case "YOUTUBER":
                return EnumChatFormatting.GOLD + ""; // &6
            case "MOJANG":
                return EnumChatFormatting.GOLD + ""; // &6
            case "EVENTS":
                return EnumChatFormatting.LIGHT_PURPLE + ""; // &d
            case "INNIT":
                return EnumChatFormatting.LIGHT_PURPLE + ""; // &d
            case "MVP++":
                return getMVPPlusPlusColor(player.getMonthlyRankColor());
            case "MVP+":
                return getMVPPlusColor(player.getRankPlusColor());
            case "MVP":
                return EnumChatFormatting.AQUA + ""; // &b
            case "VIP+":
                return EnumChatFormatting.GOLD + ""; // &6 (VIP+ is always gold)
            case "VIP":
                return EnumChatFormatting.GREEN + ""; // &a
            default:
                return EnumChatFormatting.YELLOW + ""; // &e
        }
    }
    
    private String getUsernameColor(String rank) {
        switch (rank) {
            case "OWNER":
                return EnumChatFormatting.DARK_RED + ""; // &4
            case "ADMIN":
                return EnumChatFormatting.RED + ""; // &c
            case "GM":
                return EnumChatFormatting.DARK_GREEN + ""; // &2
            case "YOUTUBER":
                return EnumChatFormatting.GOLD + ""; // &6
            case "MOJANG":
                return EnumChatFormatting.GOLD + ""; // &6
            case "EVENTS":
                return EnumChatFormatting.LIGHT_PURPLE + ""; // &d
            case "INNIT":
                return EnumChatFormatting.LIGHT_PURPLE + ""; // &d
            case "MVP++":
                // Username color follows monthlyRankColor for MVP++
                // Note: We don't have player here, so this method isn't used for MVP++ usernames anymore.
                return EnumChatFormatting.AQUA + "";
            case "MVP+":
                return EnumChatFormatting.AQUA + ""; // &b (MVP+ usernames are always aqua)
            case "MVP":
                return EnumChatFormatting.AQUA + ""; // &b
            case "VIP+":
                return EnumChatFormatting.GREEN + ""; // &a (VIP+ usernames are always green)
            case "VIP":
                return EnumChatFormatting.GREEN + ""; // &a
            default:
                return EnumChatFormatting.WHITE + ""; // &f
        }
    }
    
    private String getMVPPlusPlusColor(String monthlyRankColor) {
        if (monthlyRankColor == null || monthlyRankColor.isEmpty()) {
            return EnumChatFormatting.GOLD + ""; // Default gold (§6)
        }
        
        // Exact mapping as specified in the API documentation
        switch (monthlyRankColor) {
            case "DEFAULT": return EnumChatFormatting.GOLD + ""; // §6 - Gold (Default MVP++ color)
            case "DARK_PURPLE": return EnumChatFormatting.DARK_PURPLE + ""; // §5 - Dark Purple
            case "LIGHT_PURPLE": return EnumChatFormatting.LIGHT_PURPLE + ""; // §d - Light Purple
            case "DARK_AQUA": return EnumChatFormatting.DARK_AQUA + ""; // §3 - Dark Aqua
            case "AQUA": return EnumChatFormatting.AQUA + ""; // §b - Aqua
            case "RED": return EnumChatFormatting.RED + ""; // §c - Red
            case "GOLD": return EnumChatFormatting.GOLD + ""; // §6 - Gold (same as DEFAULT)
            case "GREEN": return EnumChatFormatting.GREEN + ""; // §a - Green
            case "YELLOW": return EnumChatFormatting.YELLOW + ""; // §e - Yellow
            case "WHITE": return EnumChatFormatting.WHITE + ""; // §f - White
            case "BLUE": return EnumChatFormatting.BLUE + ""; // §9 - Blue
            case "DARK_GREEN": return EnumChatFormatting.DARK_GREEN + ""; // §2 - Dark Green
            case "DARK_RED": return EnumChatFormatting.DARK_RED + ""; // §4 - Dark Red
            case "DARK_GRAY": return EnumChatFormatting.DARK_GRAY + ""; // §8 - Dark Gray
            case "BLACK": return EnumChatFormatting.BLACK + ""; // §0 - Black
            case "DARK_BLUE": return EnumChatFormatting.DARK_BLUE + ""; // §1 - Dark Blue
            default: return EnumChatFormatting.GOLD + ""; // §6 - Default to gold if not found
        }
    }
    
    private String getMVPPlusColor(String rankPlusColor) {
        if (rankPlusColor == null || rankPlusColor.isEmpty()) {
            return EnumChatFormatting.GOLD + ""; // Default gold
        }
        
        switch (rankPlusColor) {
            case "RED": return EnumChatFormatting.RED + ""; // &c
            case "BLUE": return EnumChatFormatting.BLUE + ""; // &9
            case "GREEN": return EnumChatFormatting.GREEN + ""; // &a
            case "YELLOW": return EnumChatFormatting.YELLOW + ""; // &e
            case "GOLD": return EnumChatFormatting.GOLD + ""; // &6
            case "AQUA": return EnumChatFormatting.AQUA + ""; // &b
            case "LIGHT_PURPLE": return EnumChatFormatting.LIGHT_PURPLE + ""; // &d
            case "DARK_PURPLE": return EnumChatFormatting.DARK_PURPLE + ""; // &5
            case "WHITE": return EnumChatFormatting.WHITE + ""; // &f
            case "GRAY": return EnumChatFormatting.GRAY + ""; // &7
            case "DARK_GRAY": return EnumChatFormatting.DARK_GRAY + ""; // &8
            case "BLACK": return EnumChatFormatting.BLACK + ""; // &0
            default: return EnumChatFormatting.GOLD + ""; // Default to gold
        }
    }
    
    private String getMVPPlusPlusSignsColor(String rankPlusColor) {
        if (rankPlusColor == null || rankPlusColor.isEmpty()) {
            return EnumChatFormatting.GOLD + ""; // Default gold (§6)
        }
        
        switch (rankPlusColor) {
            case "DEFAULT": return EnumChatFormatting.GOLD + ""; // §6 - Gold
            case "DARK_PURPLE": return EnumChatFormatting.DARK_PURPLE + ""; // §5 - Dark Purple
            case "LIGHT_PURPLE": return EnumChatFormatting.LIGHT_PURPLE + ""; // §d - Light Purple
            case "DARK_AQUA": return EnumChatFormatting.DARK_AQUA + ""; // §3 - Dark Aqua
            case "AQUA": return EnumChatFormatting.AQUA + ""; // §b - Aqua
            case "RED": return EnumChatFormatting.RED + ""; // §c - Red
            case "GOLD": return EnumChatFormatting.GOLD + ""; // §6 - Gold
            case "GREEN": return EnumChatFormatting.GREEN + ""; // §a - Green
            case "YELLOW": return EnumChatFormatting.YELLOW + ""; // §e - Yellow
            case "WHITE": return EnumChatFormatting.WHITE + ""; // §f - White
            case "BLUE": return EnumChatFormatting.BLUE + ""; // §9 - Blue
            case "DARK_GREEN": return EnumChatFormatting.DARK_GREEN + ""; // §2 - Dark Green
            case "DARK_RED": return EnumChatFormatting.DARK_RED + ""; // §4 - Dark Red
            case "DARK_GRAY": return EnumChatFormatting.DARK_GRAY + ""; // §8 - Dark Gray
            case "BLACK": return EnumChatFormatting.BLACK + ""; // §0 - Black
            case "DARK_BLUE": return EnumChatFormatting.DARK_BLUE + ""; // §1 - Dark Blue
            default: return EnumChatFormatting.GOLD + ""; // §6 - Default to gold
        }
    }
    
    private void drawScrollbar() {
        // Draw a scrollbar indicator if needed (matching InventoryHUD style)
        int maxPossibleOffset = Math.max(0, players.size() - PLAYERS_PER_PAGE);
        if (maxPossibleOffset > 0) {
            int trackX = this.width - 10; // right side padding
            int trackWidth = 4;

            // Define scrollable region bounds
            int contentTop = 140; // start of player list
            int contentBottom = contentTop + (players.size() * 15);
            int visibleTop = contentTop;
            int visibleBottom = this.height - 40; // stop before back button

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
            int thumbY = trackTop + (maxPossibleOffset == 0 ? 0 : (int) (maxThumbTravel * (scrollOffset / (float) maxPossibleOffset)));

            // Track background
            net.minecraft.client.gui.Gui.drawRect(trackX, trackTop, trackX + trackWidth, trackBottom, 0x60000000);
            // Thumb (lighter)
            net.minecraft.client.gui.Gui.drawRect(trackX, thumbY, trackX + trackWidth, thumbY + thumbHeight, 0x90FFFFFF);
        }
    }
    
    private void computeMaxScroll() {
        int contentTop = 140; // start of player list
        int contentBottom = contentTop + (players.size() * 15);
        int visibleBottom = this.height - 40; // account for back button area
        
        maxScroll = Math.max(0, contentBottom - visibleBottom);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
    }
    
    private void drawPlayerHead(String uuid, int x, int y, int size) {
        try {
            // Prefer Crafatar head render by UUID (fast, consistent)
            if (uuid == null || uuid.isEmpty()) {
                uuid = "00000000000000000000000000000000"; // fallback steve
            }
            String url = "https://crafatar.com/avatars/" + uuid + "?size=16&overlay";
            ResourceLocation rl = new ResourceLocation("crafatar/avatars/" + uuid);
            TextureManager tm = Minecraft.getMinecraft().getTextureManager();
            ITextureObject tex = tm.getTexture(rl);
            if (tex == null) {
                // REFLECTION SAFETY: Use proper ThreadDownloadImageData constructor to prevent InvocationTargetException
                // See REFLECTION_SAFETY_RULES.md for guidelines
                ThreadDownloadImageData data = new ThreadDownloadImageData(null, url, null, null);
                tm.loadTexture(rl, data);
            }
            tm.bindTexture(rl);
            // Draw whole 16x16 head image
            float z = this.zLevel;
            this.zLevel = 50.0F;
            // Save current color state
            net.minecraft.client.renderer.GlStateManager.pushMatrix();
            // Ensure white color (avoid black tint)
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            net.minecraft.client.renderer.GlStateManager.enableBlend();
            Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, 16, 16, size, size, 16, 16);
            // Restore color state
            net.minecraft.client.renderer.GlStateManager.popMatrix();
            this.zLevel = z;
        } catch (Exception e) {
            // Log the actual error for debugging
            System.err.println("[gomod] Player head drawing error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String sanitizeName(String input) {
        if (input == null) return "";
        // Strip Minecraft color codes (§x)
        String cleaned = input.replaceAll("\u00A7[0-9A-FK-ORa-fk-or]", "");
        // Remove any leading rank tags like [MVP], [MVP+], [MVP++] etc.
        cleaned = cleaned.replaceAll("^\\[[^]]+\\]\\s+", "");
        return cleaned.trim();
    }

    // Simple action dialog to choose Update or Delete for a player
    private static class PlayerActionDialog extends GuiScreen {
        private final GuiScreen parent;
        private final String username;
        private final String uuid;
        private GuiButton updateButton;
        private GuiButton deleteButton;
        private GuiButton cancelButton;

        public PlayerActionDialog(GuiScreen parent, String rawUsername, String uuid) {
            this.parent = parent;
            this.username = rawUsername; // ensure raw username without rank tags is used
            this.uuid = uuid;
        }

        @Override
        public void initGui() {
            int centerX = width / 2;
            int centerY = height / 2;
            int buttonWidth = 100;
            int spacing = 24;

            updateButton = new GuiButton(1, centerX - buttonWidth - 10, centerY + 10, buttonWidth, 20, EnumChatFormatting.GREEN + "Update");
            deleteButton = new GuiButton(2, centerX + 10, centerY + 10, buttonWidth, 20, EnumChatFormatting.RED + "Delete");
            cancelButton = new GuiButton(0, centerX - 50, centerY + 40 + spacing, 100, 20, EnumChatFormatting.GRAY + "Cancel");

            buttonList.clear();
            buttonList.add(updateButton);
            buttonList.add(deleteButton);
            buttonList.add(cancelButton);
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();
            // Build rank-colored display like main list
            String display = EnumChatFormatting.WHITE + username;
            try {
                java.util.List<TeamSpamManager.Player> players = TeamSpamManager.getPlayers();
                TeamSpamManager.Player p = null;
                for (TeamSpamManager.Player it : players) {
                    if ((uuid != null && !uuid.isEmpty() && uuid.equalsIgnoreCase(it.getUuid())) ||
                        (it.getUsername() != null && it.getUsername().equalsIgnoreCase(username))) {
                        p = it; break;
                    }
                }
                if (p != null) {
                    String rank = ((TeamSpamGUI) parent).getPlayerRank(p);
                    if (!rank.isEmpty() && !"Default".equals(rank)) {
                        if ("MVP+".equals(rank)) {
                            String baseColor = EnumChatFormatting.AQUA + "";
                            String plusColor = ((TeamSpamGUI) parent).getMVPPlusColor(p.getRankPlusColor());
                            display = baseColor + "[MVP" + plusColor + "+" + baseColor + "] " + ((TeamSpamGUI) parent).getUsernameColor(rank) + p.getUsername();
                        } else if ("MVP++".equals(rank)) {
                            String baseColor = ((TeamSpamGUI) parent).getMVPPlusPlusColor(p.getMonthlyRankColor());
                            String plusColor = ((TeamSpamGUI) parent).getMVPPlusPlusSignsColor(p.getRankPlusColor());
                            display = baseColor + "[MVP" + plusColor + "++" + baseColor + "] " + baseColor + p.getUsername();
                        } else {
                            String rankColor = ((TeamSpamGUI) parent).getRankColor(rank, p);
                            String usernameColor = ((TeamSpamGUI) parent).getUsernameColor(rank);
                            display = rankColor + "[" + rank + "] " + usernameColor + p.getUsername();
                        }
                    } else if ("Default".equals(rank)) {
                        display = EnumChatFormatting.GRAY + p.getUsername();
                    } else {
                        display = EnumChatFormatting.WHITE + p.getUsername();
                    }
                }
            } catch (Exception ignored) {}

            int titleY = height / 2 - 56; // move up so it doesn't touch buttons
            drawCenteredString(fontRendererObj, display + EnumChatFormatting.RESET, width / 2, titleY, 0xFFFFFF);
            // Draw player head below the name (also higher)
            int headSize = 32;
            int headX = width / 2 - headSize / 2;
            int headY = titleY + 12;
            ((TeamSpamGUI) parent).drawPlayerHead(uuid, headX, headY, headSize);
            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        public void actionPerformed(GuiButton button) {
            if (button == cancelButton) {
                mc.displayGuiScreen(parent);
                return;
            }
            if (button == updateButton) {
                mc.displayGuiScreen(parent);
                Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Updating " + username + "...");
                new Thread(() -> {
                    String result = TeamSpamManager.updatePlayerInfo(username);
                    Main.sendMessage(result);
                    if (parent instanceof TeamSpamGUI) {
                        ((TeamSpamGUI) parent).refreshPlayerListAndScrollToTop();
                    }
                }).start();
                return;
            }
            if (button == deleteButton) {
                // Immediately remove without confirmation
                String msg = TeamSpamManager.removePlayer(username);
                Main.sendMessage(msg);
                if (parent instanceof TeamSpamGUI) {
                    ((TeamSpamGUI) parent).refreshPlayerList();
                }
                mc.displayGuiScreen(parent);
            }
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }

    // Holds an individual change from refresh
    private static class RefreshChange {
        private final String username;
        private final String uuid;
        private final String beforeDisplay;
        private final String afterDisplay;

        public RefreshChange(String username, String beforeDisplay, String afterDisplay) {
            this.username = username;
            this.uuid = findPlayerUuid(username);
            this.beforeDisplay = beforeDisplay;
            this.afterDisplay = afterDisplay;
        }

        public String getUsername() { return username; }
        public String getUuid() { return uuid; }
        public String getBeforeDisplay() { return beforeDisplay; }
        public String getAfterDisplay() { return afterDisplay; }
        
        private String findPlayerUuid(String username) {
            // Find the player's UUID from the current player list
            List<Player> currentPlayers = TeamSpamManager.getPlayers();
            for (Player player : currentPlayers) {
                if (player.getUsername().equalsIgnoreCase(username)) {
                    return player.getUuid();
                }
            }
            return null; // Fallback if not found
        }
    }

    // Dialog that shows the list of refreshed players with before -> after display names
    private static class RefreshResultsDialog extends GuiScreen {
        private final GuiScreen parent;
        private final java.util.List<RefreshChange> changes;
        private GuiButton closeButton;
        private int scrollOffset = 0;
        private static final int ROW_HEIGHT = 14;

        public RefreshResultsDialog(GuiScreen parent, java.util.List<RefreshChange> changes) {
            this.parent = parent;
            this.changes = changes == null ? java.util.Collections.emptyList() : changes;
        }

        @Override
        public void initGui() {
            int centerX = width / 2;
            closeButton = new GuiButton(0, centerX - 50, height - 28, 100, 20, EnumChatFormatting.GRAY + "Close");
            buttonList.clear();
            buttonList.add(closeButton);
            scrollOffset = 0;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();
            int centerX = width / 2;
            String title = EnumChatFormatting.GOLD + "Refresh Results" + EnumChatFormatting.RESET;
            drawCenteredString(fontRendererObj, title, centerX, 12, 0xFFFFFF);
            String subtitle = EnumChatFormatting.YELLOW + "Updated entries: " + changes.size();
            drawCenteredString(fontRendererObj, subtitle, centerX, 26, 0xFFFFFF);

            int contentTop = 44;
            int contentBottom = height - 40;
            int visibleRows = Math.max(0, (contentBottom - contentTop) / ROW_HEIGHT);
            int endIndex = Math.min(changes.size(), scrollOffset + visibleRows);

            // Center the content area
            int contentWidth = Math.min(600, width - 80); // Max width of 600px, with 40px margin on each side
            int leftX = centerX - contentWidth / 2;
            int rightX = centerX + contentWidth / 2;

            for (int i = scrollOffset; i < endIndex; i++) {
                int y = contentTop + (i - scrollOffset) * ROW_HEIGHT;
                // Row background for readability
                if ((i & 1) == 0) {
                    net.minecraft.client.gui.Gui.drawRect(leftX - 6, y - 2, rightX + 6, y + fontRendererObj.FONT_HEIGHT + 2, 0x20000000);
                }
                RefreshChange c = changes.get(i);
                String before = c.getBeforeDisplay();
                String after = c.getAfterDisplay();
                String arrow = EnumChatFormatting.GRAY + "  ->  " + EnumChatFormatting.RESET;

                // Build centered composite string
                String fullText = before + EnumChatFormatting.RESET + arrow + after + EnumChatFormatting.RESET;
                int textWidth = fontRendererObj.getStringWidth(fullText);
                int textX = centerX - textWidth / 2;
                
                // Draw avatar head next to the text (left of the text)
                int headSize = 10;
                int headX = textX - headSize - 4; // Position head to the left of the text
                int headY = y - 1;
                drawPlayerHead(c.getUuid(), headX, headY, headSize);
                
                // Draw the text centered
                fontRendererObj.drawStringWithShadow(fullText, textX, y, 0xFFFFFF);
            }

            // Simple scrollbar thumb
            int maxOffset = Math.max(0, changes.size() - visibleRows);
            if (maxOffset > 0) {
                int trackX = width - 10;
                int trackTop = contentTop;
                int trackBottom = contentBottom;
                int trackHeight = trackBottom - trackTop;
                int thumbHeight = Math.max(20, (int) (trackHeight * (visibleRows / (float) Math.max(1, changes.size()))));
                int maxThumbTravel = trackHeight - thumbHeight;
                int thumbY = trackTop + (int) (maxThumbTravel * (scrollOffset / (float) maxOffset));
                net.minecraft.client.gui.Gui.drawRect(trackX, trackTop, trackX + 4, trackBottom, 0x60000000);
                net.minecraft.client.gui.Gui.drawRect(trackX, thumbY, trackX + 4, thumbY + thumbHeight, 0x90FFFFFF);
            }

            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        public void handleMouseInput() {
            super.handleMouseInput();
            int contentTop = 44;
            int contentBottom = height - 40;
            int visibleRows = Math.max(0, (contentBottom - contentTop) / ROW_HEIGHT);
            int maxOffset = Math.max(0, changes.size() - visibleRows);
            int dWheel = org.lwjgl.input.Mouse.getEventDWheel();
            if (dWheel != 0 && maxOffset > 0) {
                if (dWheel > 0) {
                    scrollOffset = Math.max(0, scrollOffset - 3);
                } else {
                    scrollOffset = Math.min(maxOffset, scrollOffset + 3);
                }
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
        
        private void drawPlayerHead(String uuid, int x, int y, int size) {
            try {
                // Prefer Crafatar head render by UUID (fast, consistent)
                if (uuid == null || uuid.isEmpty()) {
                    uuid = "00000000000000000000000000000000"; // fallback steve
                }
                String url = "https://crafatar.com/avatars/" + uuid + "?size=16&overlay";
                ResourceLocation rl = new ResourceLocation("crafatar/avatars/" + uuid);
                TextureManager tm = Minecraft.getMinecraft().getTextureManager();
                ITextureObject tex = tm.getTexture(rl);
                if (tex == null) {
                    // REFLECTION SAFETY: Use proper ThreadDownloadImageData constructor to prevent InvocationTargetException
                    // See REFLECTION_SAFETY_RULES.md for guidelines
                    ThreadDownloadImageData data = new ThreadDownloadImageData(null, url, null, null);
                    tm.loadTexture(rl, data);
                }
                tm.bindTexture(rl);
                // Draw whole 16x16 head image
                float z = this.zLevel;
                this.zLevel = 50.0F;
                // Save current color state
                net.minecraft.client.renderer.GlStateManager.pushMatrix();
                // Ensure white color (avoid black tint)
                net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                net.minecraft.client.renderer.GlStateManager.enableBlend();
                Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, 16, 16, size, size, 16, 16);
                // Restore color state
                net.minecraft.client.renderer.GlStateManager.popMatrix();
                this.zLevel = z;
            } catch (Exception e) {
                // Log the actual error for debugging
                System.err.println("[gomod] Player head drawing error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
