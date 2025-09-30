package me.ballmc.gomod.gui;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import me.ballmc.gomod.features.KitLevelsManager;
import me.ballmc.gomod.features.KitLevelsManager.KitPlayer;
import me.ballmc.gomod.Main;

import java.util.List;
import java.util.ArrayList;

/**
 * GUI for viewing kit levels of players.
 * Allows searching for players with specific kits and displays their levels.
 */
public class KitLevelsGUI extends GuiScreen {
    private GuiScreen parent;
    private GuiButton backButton;
    private GuiButton searchButton;
    private GuiButton showAllButton; // deprecated (hidden)
    private GuiButton refreshButton;
    private GuiButton clearCacheButton;
    
    private GuiTextField kitNameField;
    private GuiTextField playerEditField;
    private GuiButton addPlayerButton;
    private GuiButton removePlayerButton;
    
    private List<KitPlayer> playersWithKit = new ArrayList<>();
    private String currentKitName = "";
    private boolean isLoading = false;
    private String loadingMessage = "";
    private String errorMessage = "";
    private boolean showingAllPlayers = true;
    
    private int scrollOffset = 0;
    private static final int PLAYERS_PER_PAGE = 18;
    private int maxScroll = 0;
    private static final int SCROLL_STEP = 3;
    
    // Refresh animation (persist across GUI re-opens)
    private static boolean isRefreshing = false;
    private static int refreshingIndex = -1;
    // Track changes between refreshes for hover tooltips
    private static final java.util.Map<String, java.util.Map<String, Integer>> lastSnapshot = new java.util.HashMap<>();
    private static final java.util.Map<String, java.util.List<String>> changeTooltip = new java.util.HashMap<>();
    
    public KitLevelsGUI(GuiScreen parent) {
        this.parent = parent;
        loadAllPlayers();
    }
    
    @Override
    public void initGui() {
        int centerX = width / 2;
        int startY = 30;
        
        // Back button
        backButton = new GuiButton(0, centerX - 75, height - 30, 150, 20, "Back to Menu");
        
        // Kit name search section
        kitNameField = new GuiTextField(1, fontRendererObj, centerX - 100, startY, 200, 20);
        kitNameField.setMaxStringLength(50);
        kitNameField.setText(currentKitName);
        
        searchButton = new GuiButton(2, centerX + 110, startY, 64, 20, "Search");
        showAllButton = new GuiButton(8, centerX + 110, startY + 30, 64, 20, "Show All");
        showAllButton.visible = false; // Hide to make search-first UX
        // Move refresh/clear to top-right corner
        refreshButton = new GuiButton(9, this.width - 110, 8, 100, 20, "Refresh All");
        clearCacheButton = new GuiButton(3, this.width - 110, 30, 100, 20, "Clear Cache");
        
        // Player add/remove row (single box centered, buttons left/right)
        int editY = startY + 60;
        playerEditField = new GuiTextField(10, fontRendererObj, centerX - 80, editY, 160, 20);
        playerEditField.setMaxStringLength(16);
        // Switch positions: Remove on left, Add on right
        removePlayerButton = new GuiButton(12, centerX - 160 - 10, editY, 80, 20, "Remove");
        addPlayerButton = new GuiButton(11, centerX + 160 + 10 - 80, editY, 80, 20, "Add");

        // Initialize scroll
        scrollOffset = 0;
        computeMaxScroll();
        
        // Add all buttons
        buttonList.add(backButton);
        buttonList.add(searchButton);
        // buttonList.add(showAllButton); // hidden
        buttonList.add(refreshButton);
        buttonList.add(clearCacheButton);
        buttonList.add(addPlayerButton);
        buttonList.add(removePlayerButton);
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        
        int centerX = width / 2;
        int startY = 30;
        
        // Title
        drawCenteredString(fontRendererObj, EnumChatFormatting.GOLD + "Kit Levels Viewer", centerX, 10, 0xFFFFFF);
        
        // Search section
        drawCenteredString(fontRendererObj, "Kit Name:", centerX, startY + 5, 0xFFFFFF);
        kitNameField.drawTextBox();
        
        // Status messages
        int statusY = startY + 40;
        if (isLoading) {
            drawCenteredString(fontRendererObj, EnumChatFormatting.YELLOW + loadingMessage, centerX, statusY, 0xFFFFFF);
        } else if (!errorMessage.isEmpty()) {
            drawCenteredString(fontRendererObj, EnumChatFormatting.RED + errorMessage, centerX, statusY, 0xFFFFFF);
        } else if (!currentKitName.isEmpty()) {
            String resultText = String.format("Players with %s kit (%d):", currentKitName, playersWithKit.size());
            drawCenteredString(fontRendererObj, EnumChatFormatting.GREEN + resultText, centerX, statusY, 0xFFFFFF);
        } else if (showingAllPlayers) {
            String resultText = String.format("All players in kit levels list (%d):", playersWithKit.size());
            drawCenteredString(fontRendererObj, EnumChatFormatting.AQUA + resultText, centerX, statusY, 0xFFFFFF);
        } else {
            drawCenteredString(fontRendererObj, EnumChatFormatting.GRAY + "Enter a kit name to search for players", centerX, statusY, 0xFFFFFF);
        }
        
        // Display players with scroll offset
        // Draw the player edit field
        playerEditField.drawTextBox();
        
        int baseY = startY + 120;
        int endIndex = Math.min(scrollOffset + PLAYERS_PER_PAGE, playersWithKit.size());
        int leftX = centerX - 220;
        int rightX = centerX + 220;
        
        // Prevent overlapping with back button
        int visibleBottom = height - 40;
        
        for (int i = scrollOffset; i < endIndex; i++) {
            KitPlayer player = playersWithKit.get(i);
            int fontH = fontRendererObj.FONT_HEIGHT;
            int yPos = baseY + (i - scrollOffset) * 15;
            boolean hovered = (mouseX >= leftX && mouseX <= rightX && mouseY >= yPos && mouseY <= yPos + fontH);

            // Build centered name segment (ranked or plain username)
            String nameSegment;
            String rankText = getPlayerRankText(player);
            if (!rankText.isEmpty()) {
                nameSegment = rankText;
            } else {
                nameSegment = EnumChatFormatting.WHITE + player.getUsername() + EnumChatFormatting.RESET;
            }

            // If showing search results, append kit and level info, separated by white dash
            if (!showingAllPlayers) {
                String kitName = getKitDisplayName(player);
                String kitColored = getKitColoredDisplay(kitName, player);
                nameSegment = nameSegment + EnumChatFormatting.RESET + " - " + kitColored;
            }

            // Determine centered X for the name segment
            int nameWidth = fontRendererObj.getStringWidth(nameSegment);
            int nameX = centerX - (nameWidth / 2);

            // Determine prefix (number or hover chevron) and positions for prefix and head to the left
            String displayPrefix = hovered ? (EnumChatFormatting.GREEN + ">> " + EnumChatFormatting.RESET) : ((i + 1) + ". ");
            int prefixWidth = fontRendererObj.getStringWidth(displayPrefix);
            int headSize = 10;
            int prefixX = nameX - (headSize + 4 + prefixWidth);
            int headX = prefixX + prefixWidth + 4;
            int headY = yPos - 2;

            // Stop rendering if we would overlap the back button area
            if (yPos + 8 > visibleBottom) {
                break;
            }

            // Hover highlight background spanning the row
            if (hovered) {
                int startXRect = leftX - 6;
                int endXRect = rightX + 6;
                int topY = yPos - 4;
                int bottomY = yPos + fontH + 2;
                net.minecraft.client.gui.Gui.drawRect(startXRect, topY, endXRect, bottomY, 0x40000000);
            }

            // Refresh animation overlay: match TeamSpam style (smaller bar)
            if (isRefreshing) {
                int startX2 = leftX - 6;
                int endX2 = rightX + 6;
                int topY2 = yPos - 2;
                int bottomY2 = yPos + fontH + 1;
                if (i < refreshingIndex) {
                    net.minecraft.client.gui.Gui.drawRect(startX2, topY2, endX2, bottomY2, 0x4022AA22);
                } else if (i == refreshingIndex) {
                    net.minecraft.client.gui.Gui.drawRect(startX2, topY2, endX2, bottomY2, 0x6044FF44);
                }
            }

            // Center the entire row group (prefix + head + name) like TeamSpam
            int totalWidth = prefixWidth + 4 + headSize + 4 + nameWidth;
            int rowStartX = centerX - (totalWidth / 2);
            int prefixDrawX = rowStartX;
            int headDrawX = prefixDrawX + prefixWidth + 4;
            int nameDrawX = headDrawX + headSize + 4;

            fontRendererObj.drawStringWithShadow(displayPrefix, prefixDrawX, yPos, 0xFFFFFF);
            drawPlayerHead(player.getUuid(), headDrawX, headY + 1, headSize);
            fontRendererObj.drawStringWithShadow(nameSegment, nameDrawX, yPos, 0xFFFFFF);
            // Show change tooltip on hover (built after refresh)
            if (hovered) {
                java.util.List<String> tips = changeTooltip.get(player.getUsername().toLowerCase());
                if (tips != null && !tips.isEmpty()) {
                    java.util.List<String> lines = new java.util.ArrayList<>();
                    lines.add(EnumChatFormatting.GOLD + "Changes since last refresh:");
                    lines.addAll(tips);
                    this.drawHoveringText(lines, mouseX, Math.max(0, mouseY - 12));
                }
            }
            continue;
        }
        
        // Draw scrollbar if needed
        drawScrollbar();
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    @Override
    public void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0: // Back
                mc.displayGuiScreen(parent);
                break;
            
            case 2: // Search
                performSearch();
                break;
            
            case 3: // Clear Cache
                KitLevelsManager.clearCache(null);
                Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN + "Kit levels cache cleared.");
                break;
            
            case 8: // Show All
                loadAllPlayers();
                kitNameField.setText("");
                Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.AQUA + "Showing all players in team spam list.");
                break;
            
            case 9: // Refresh All
                refreshAllPlayers();
                break;

            case 11: { // Add Player
                String name = playerEditField.getText().trim();
                if (!name.isEmpty()) {
                    Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Adding player " + name + "...");
                    new Thread(() -> {
                        try {
                            String[] data = me.ballmc.gomod.features.KitPlayersManager.resolvePlayer(name);
                            if (data != null) {
                                String correctName = data[0];
                                String uuid = data[1];
                                me.ballmc.gomod.features.KitPlayersManager.addPlayer(correctName, uuid);
                                // If Hypixel API key is available, fetch rank immediately so UI shows colors without full refresh
                                String apiKey = me.ballmc.gomod.features.ApiKeyManager.getApiKey("hypixel");
                                if (apiKey != null && !apiKey.isEmpty()) {
                                    try {
                                        String[] rankInfo = me.ballmc.gomod.features.KitLevelsManager.getOrFetchRankInfo(uuid, apiKey);
                                        me.ballmc.gomod.features.KitLevelsManager.cacheRankInfo(uuid, rankInfo[0], rankInfo[1], rankInfo[2]);
                                    } catch (Exception ignored) {}
                                }
                                Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN + "Added " + correctName + ".");
                                loadAllPlayers();
                            } else {
                                Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Player not found: " + name);
                            }
                        } catch (Exception e) {
                            Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Error: " + e.getMessage());
                        }
                    }).start();
                    playerEditField.setText("");
                }
                break;
            }
            case 12: { // Remove Player
                String name = playerEditField.getText().trim();
                if (!name.isEmpty()) {
                    java.util.List<me.ballmc.gomod.features.KitPlayersManager.Player> current = me.ballmc.gomod.features.KitPlayersManager.getPlayers();
                    java.util.List<me.ballmc.gomod.features.KitPlayersManager.Player> updated = new java.util.ArrayList<>();
                    boolean removed = false;
                    for (me.ballmc.gomod.features.KitPlayersManager.Player p : current) {
                        if (!p.getUsername().equalsIgnoreCase(name)) {
                            updated.add(p);
                        } else {
                            removed = true;
                        }
                    }
                    if (removed) {
                        me.ballmc.gomod.features.KitPlayersManager.setPlayers(updated);
                        Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN + "Removed " + name + ".");
                        loadAllPlayers();
                    } else {
                        Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + name + " not in list.");
                    }
                    playerEditField.setText("");
                }
                break;
            }
        }
    }
    
    @Override
    public void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);
        kitNameField.textboxKeyTyped(typedChar, keyCode);
        if (playerEditField != null) {
            playerEditField.textboxKeyTyped(typedChar, keyCode);
        }
        
        // Search on Enter key
        if (keyCode == 28) { // Enter key
            performSearch();
        }
    }
    
    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        kitNameField.mouseClicked(mouseX, mouseY, mouseButton);
        if (playerEditField != null) {
            playerEditField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }
    
    @Override
    public void updateScreen() {
        super.updateScreen();
        kitNameField.updateCursorCounter();
        if (playerEditField != null) {
            playerEditField.updateCursorCounter();
        }
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
            int maxPossibleOffset = Math.max(0, playersWithKit.size() - PLAYERS_PER_PAGE);
            scrollOffset = Math.min(scrollOffset, maxPossibleOffset);
        }
    }
    
    private void loadAllPlayers() {
        // Load all players from team spam list and display them
        playersWithKit.clear();
        currentKitName = "";
        showingAllPlayers = true;
        isLoading = false;
        errorMessage = "";
        
        // Get all players from KitPlayersManager and prefill with rank-only data (no kits yet)
        List<me.ballmc.gomod.features.KitPlayersManager.Player> allPlayers = me.ballmc.gomod.features.KitPlayersManager.getPlayers();
        for (me.ballmc.gomod.features.KitPlayersManager.Player player : allPlayers) {
            String rank = "Default";
            String rankPlus = null;
            String monthly = null;
            // Use cached rank only; do not hit API on open
            if (player.getUuid() != null && !player.getUuid().isEmpty()) {
                String[] cached = me.ballmc.gomod.features.KitLevelsManager.getCachedRankInfo(player.getUuid());
                if (cached != null) {
                    rank = cached[0];
                    rankPlus = cached[1];
                    monthly = cached[2];
                }
            }
            // Level 1 dummy but with rank fields for display in default list
            KitPlayer kitPlayer = new KitPlayer(player.getUsername(), player.getUuid(), 1, 0, false, null, null, rank, rankPlus, monthly);
            playersWithKit.add(kitPlayer);
        }
        
        // Reset scroll
        scrollOffset = 0;
        computeMaxScroll();
    }
    
    private void refreshAllPlayers() {
        showingAllPlayers = true;
        isLoading = true;
        loadingMessage = "Refreshing all players' kit data... This may take a while.";
        errorMessage = "";
        isRefreshing = true;
        refreshingIndex = -1;
        
        // Get all players from KitPlayersManager
        List<me.ballmc.gomod.features.KitPlayersManager.Player> allPlayers = me.ballmc.gomod.features.KitPlayersManager.getPlayers();
        
        // Clear cache to force fresh data
        KitLevelsManager.clearCache(null);
        
        // Fetch kit data for all players in background
        new Thread(() -> {
            try {
                List<KitPlayer> allKitPlayers = new ArrayList<>();
                String apiKey = me.ballmc.gomod.features.ApiKeyManager.getApiKey("hypixel");
                
                if (apiKey == null || apiKey.isEmpty()) {
                    this.isLoading = false;
                    this.loadingMessage = "";
                    this.errorMessage = "No Hypixel API key set. Please set one with /gmapi hypixel <key>";
                    return;
                }
                
                int processedCount = 0;
                for (int i = 0; i < allPlayers.size(); i++) {
                    me.ballmc.gomod.features.KitPlayersManager.Player player = allPlayers.get(i);
                    try {
                        // Update animation index
                        refreshingIndex = i;
                        // Cache player's kit data for future searches
                        KitLevelsManager.cachePlayerKitData(player.getUuid(), player.getUsername(), apiKey);
                        
                        // Get player's kit data from Hypixel API
                        java.util.List<KitPlayer> playerKits = KitLevelsManager.getPlayerAllKits(player.getUuid(), player.getUsername(), apiKey);
                        allKitPlayers.addAll(playerKits);
                        
                        processedCount++;
                        this.loadingMessage = "Refreshing all players' kit data... (" + processedCount + "/" + allPlayers.size() + ")";
                        
                        // Add delay to avoid rate limits
                        Thread.sleep(150);
                    } catch (Exception e) {
                        System.err.println("[KitLevelsGUI] Error refreshing player " + player.getUsername() + ": " + e.getMessage());
                    }
                }
                
                // Sort by level (highest first) and then by kit name
                allKitPlayers.sort((a, b) -> {
                    if (a.isPrestiged() && !b.isPrestiged()) return -1;
                    if (!a.isPrestiged() && b.isPrestiged()) return 1;
                    if (a.isPrestiged() && b.isPrestiged()) {
                        return Integer.compare(b.getPrestige(), a.getPrestige());
                    }
                    int levelCompare = Integer.compare(b.getLevel(), a.getLevel());
                    if (levelCompare != 0) return levelCompare;
                    return a.getUsername().compareToIgnoreCase(b.getUsername());
                });
                
                // Build snapshot of latest levels per player to compute changes
                java.util.Map<String, java.util.Map<String, Integer>> currentSnapshot = new java.util.HashMap<>();
                for (KitPlayer kp : allKitPlayers) {
                    String key = kp.getUsername().toLowerCase();
                    java.util.Map<String, Integer> kits = currentSnapshot.computeIfAbsent(key, k -> new java.util.HashMap<>());
                    kits.put(kp.getKitName(), kp.getLevel());
                }
                // Compare with last snapshot to generate hover tooltips
                changeTooltip.clear();
                for (java.util.Map.Entry<String, java.util.Map<String, Integer>> entry : currentSnapshot.entrySet()) {
                    String user = entry.getKey();
                    java.util.Map<String, Integer> now = entry.getValue();
                    java.util.Map<String, Integer> prev = lastSnapshot.get(user);
                    if (prev == null) prev = java.util.Collections.emptyMap();
                    java.util.List<String> diffs = new java.util.ArrayList<>();
                    // Level ups and new kits
                    for (java.util.Map.Entry<String, Integer> kv : now.entrySet()) {
                        String kit = kv.getKey();
                        int lvl = kv.getValue();
                        Integer old = prev.get(kit);
                        if (old == null) {
                            diffs.add(EnumChatFormatting.WHITE + "Bought " + EnumChatFormatting.YELLOW + kit + EnumChatFormatting.WHITE + " " + getColoredLevelDisplay(lvl));
                        } else if (lvl > old) {
                            diffs.add(EnumChatFormatting.YELLOW + kit + EnumChatFormatting.WHITE + " " + getColoredLevelDisplay(old) + EnumChatFormatting.GRAY + " -> " + EnumChatFormatting.WHITE + getColoredLevelDisplay(lvl));
                        }
                    }
                    if (!diffs.isEmpty()) {
                        changeTooltip.put(user, diffs);
                    }
                }
                // Save snapshot for next refresh
                lastSnapshot.clear();
                lastSnapshot.putAll(currentSnapshot);

                this.playersWithKit = allKitPlayers;
                this.isLoading = false;
                this.loadingMessage = "";
                isRefreshing = false;
                
                // Reset scroll
                scrollOffset = 0;
                computeMaxScroll();
                
                Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN + 
                    "Refreshed kit data for " + processedCount + " players. Found " + allKitPlayers.size() + " kit instances.");
                    
            } catch (Exception e) {
                this.isLoading = false;
                this.loadingMessage = "";
                this.errorMessage = "Error refreshing players: " + e.getMessage();
                isRefreshing = false;
                Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + 
                    "Error refreshing kit data: " + e.getMessage());
            }
        }).start();
    }
    
    private void performSearch() {
        String kitName = kitNameField.getText().trim();
        if (kitName.isEmpty()) {
            // If search field is empty, show all players again
            loadAllPlayers();
            return;
        }
        
        currentKitName = kitName;
        showingAllPlayers = false;
        isLoading = true;
        loadingMessage = "Searching for players with " + kitName + " kit...";
        errorMessage = "";
        
        // Perform search in background thread
        KitLevelsManager.getPlayersWithKit(kitName).thenAccept(players -> {
            this.playersWithKit = players;
            this.isLoading = false;
            this.loadingMessage = "";
            isRefreshing = false;
            
            if (players.isEmpty()) {
                this.errorMessage = "No players found with '" + kitName + "'. Tip: run Refresh All to populate data.";
                me.ballmc.gomod.Main.sendMessage(me.ballmc.gomod.Main.CHAT_PREFIX + net.minecraft.util.EnumChatFormatting.YELLOW +
                    "No results for '" + kitName + "'. If this seems wrong, click Refresh All to cache kits first.");
            }
            
            // Reset scroll
            scrollOffset = 0;
            computeMaxScroll();
            
            // Send message to chat
            if (!players.isEmpty()) {
                Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN + 
                    "Found " + players.size() + " players with " + kitName + " kit");
            } else {
                Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + 
                    "No players found with " + kitName + " kit");
            }
        }).exceptionally(throwable -> {
            this.isLoading = false;
            this.loadingMessage = "";
            this.errorMessage = "Error searching: " + throwable.getMessage();
            Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + 
                "Error searching for kit levels: " + throwable.getMessage());
            return null;
        });
    }
    
    private void drawScrollbar() {
        // Draw a scrollbar indicator if needed
        int maxPossibleOffset = Math.max(0, playersWithKit.size() - PLAYERS_PER_PAGE);
        if (maxPossibleOffset > 0) {
            int trackX = this.width - 10; // right side padding
            int trackWidth = 4;

            // Define scrollable region bounds
            int contentTop = 90; // start of player list
            int contentBottom = contentTop + (playersWithKit.size() * 15);
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
        int contentTop = 90; // start of player list
        int contentBottom = contentTop + (playersWithKit.size() * 15);
        int visibleBottom = this.height - 40; // account for back button area
        
        maxScroll = Math.max(0, contentBottom - visibleBottom);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
    }
    
    private String getKitDisplayName(KitPlayer player) {
        return player.getKitName();
    }

    private String getKitColoredDisplay(String kitName, KitPlayer player) {
        if (kitName == null) kitName = "Kit";
        if (player.isPrestiged()) {
            // Prestige coloring and stars
            String star = player.getPrestige() == 1 ? "\u272B" : "\u272B\u272B"; // ✫
            return EnumChatFormatting.GOLD + kitName + " " + EnumChatFormatting.GOLD + star;
        }
        // Level-based coloring
        int level = player.getLevel();
        EnumChatFormatting color;
        switch (level) {
            case 1:
            case 2:
                color = EnumChatFormatting.GREEN; // §a
                break;
            case 3:
            case 4:
                color = EnumChatFormatting.DARK_GREEN; // §2
                break;
            case 5:
            case 6:
                color = EnumChatFormatting.YELLOW; // §e
                break;
            case 7:
            case 8:
                color = EnumChatFormatting.GOLD; // §6
                break;
            case 9:
                color = EnumChatFormatting.RED; // §c
                break;
            case 10:
                color = EnumChatFormatting.DARK_RED; // §4
                break;
            default:
                color = EnumChatFormatting.WHITE;
        }
        // Ensure level display inherits the same color for the roman numerals
        return color + kitName + " " + color + player.getLevelDisplay();
    }

    // Helper to build colored roman numeral level for tooltips
    private static String getColoredLevelDisplay(int level) {
        String roman;
        switch (level) {
            case 1: roman = "I"; break;
            case 2: roman = "II"; break;
            case 3: roman = "III"; break;
            case 4: roman = "IV"; break;
            case 5: roman = "V"; break;
            case 6: roman = "VI"; break;
            case 7: roman = "VII"; break;
            case 8: roman = "VIII"; break;
            case 9: roman = "IX"; break;
            case 10: roman = "X"; break;
            default: roman = String.valueOf(level);
        }
        EnumChatFormatting color;
        switch (level) {
            case 1:
            case 2:
                color = EnumChatFormatting.GREEN; break;
            case 3:
            case 4:
                color = EnumChatFormatting.DARK_GREEN; break;
            case 5:
            case 6:
                color = EnumChatFormatting.YELLOW; break;
            case 7:
            case 8:
                color = EnumChatFormatting.GOLD; break;
            case 9:
                color = EnumChatFormatting.RED; break;
            case 10:
                color = EnumChatFormatting.DARK_RED; break;
            default:
                color = EnumChatFormatting.WHITE;
        }
        return color + roman + EnumChatFormatting.RESET;
    }
    
    private String getPlayerRankText(KitPlayer player) {
        String rank = player.getRank();
        if (rank == null || rank.isEmpty()) {
            return EnumChatFormatting.WHITE + player.getUsername() + EnumChatFormatting.RESET;
        }
        if ("Default".equals(rank)) {
            return EnumChatFormatting.GRAY + player.getUsername() + EnumChatFormatting.RESET;
        }
        // MVP++: monthlyRankColor for base + username color, rankPlusColor for ++ signs
        if ("MVP++".equals(rank)) {
            String base = getMVPPlusPlusColor(player.getMonthlyRankColor());
            String plus = getMVPPlusPlusSignsColor(player.getRankPlusColor());
            return base + "[MVP" + plus + "++" + base + "]" + EnumChatFormatting.RESET + " " + base + player.getUsername() + EnumChatFormatting.RESET;
        }
        // MVP+: aqua base, rankPlusColor for + sign, username aqua
        if ("MVP+".equals(rank)) {
            String plus = getMVPPlusColor(player.getRankPlusColor());
            return EnumChatFormatting.AQUA + "[MVP" + plus + "+" + EnumChatFormatting.AQUA + "]" + EnumChatFormatting.RESET + " " + EnumChatFormatting.AQUA + player.getUsername() + EnumChatFormatting.RESET;
        }
        // Other ranks: tag color is rank color; username color via mapping
        String tagColor = getRankColor(rank, player);
        String usernameColor = getUsernameColor(rank);
        return tagColor + "[" + rank + "]" + EnumChatFormatting.RESET + " " + usernameColor + player.getUsername() + EnumChatFormatting.RESET;
    }
    
    private String getRankColor(String rank, KitPlayer player) {
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
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
