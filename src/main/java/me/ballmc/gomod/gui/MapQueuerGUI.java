package me.ballmc.gomod.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;
import me.ballmc.gomod.features.MapQueuer;
import me.ballmc.gomod.Main;

public class MapQueuerGUI extends GuiScreen {
    private GuiScreen parent;
    private GuiButton backButton;
    
    // Map buttons - organized in rows
    private GuiButton[] mapButtons;
    boolean[] mapSelected; // Track which maps are selected (package-private for MapButton access)
    
    // Animation state for green overlays
    private static final java.util.Map<String, Long> overlayAnimationStart = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long ANIMATION_DURATION_MS = 800L; // Animation duration in milliseconds
    
    // Game filter system
    private static final java.util.Map<String, Long> filteredGames = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long FILTER_DURATION_MS = 15L * 60L * 1000L; // 15 minutes
    private GuiButton addFilterButton;
    private GuiButton clearFiltersButton;
    private GuiButton infoButton;
    private String[] allMaps = {
        // Solo-only maps (alphabetically sorted)
        "Alexandria", "Aelin's Tower", "Bastion", "Caelum", "Caelum v2", "Citadel", "Enthorran", "Greece", "Hamani",
        "Mithril Revived", "Peaks", "Riverside", "Seafloor", "Shroom Valley", "Thorin", "Valley",
        // Both modes (alphabetically sorted)
        "Alice", "Cattle Drive", "City", "Despair v1", "Despair v2", "Docks v1", "Docks v2", "Egypt",
        "Gulch", "Impact", "KTulu Island", "Moonbase", "Pandora", "Persia", "Pixelville", "Proxima", "Ruins",
        "Shogun", "Winter", "Woodlands",
        // Teams-only maps (alphabetically sorted)
        "Darkstone", "Mirador Basin", "Stoneguard"
    };
    
    private String selectedMode = "solo";
    private GuiButton soloButton;
    private GuiButton teamsButton;
    private GuiButton startButton;
    private GuiButton stopButton;
    private GuiButton selectAllButton;
    private GuiButton clearAllButton;

    // Scroll support for map grid
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private static final int SCROLL_STEP = 12;

    public MapQueuerGUI(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        int centerX = width / 2;
        
        // Load saved map selections
        mapSelected = MapQueuer.loadSelectedMaps();
        selectedMode = MapQueuer.getSavedMode();
        
        // Back button
        backButton = new GuiButton(0, centerX - 75, height - 30, 150, 20, "Back to Menu");
        buttonList.add(backButton);
        
        // Mode selection buttons (moved up to fit better)
        soloButton = new GuiButton(3, centerX - 150, 60, 100, 20, EnumChatFormatting.BLUE + "Solo Mode");
        teamsButton = new GuiButton(4, centerX + 50, 60, 100, 20, EnumChatFormatting.RED + "Teams Mode");
        buttonList.add(soloButton);
        buttonList.add(teamsButton);
        
        // Selection control buttons (moved up to fit better)
        selectAllButton = new GuiButton(6, centerX - 150, 85, 100, 20, "Select All");
        clearAllButton = new GuiButton(7, centerX + 50, 85, 100, 20, "Clear All");
        buttonList.add(selectAllButton);
        buttonList.add(clearAllButton);
        
        // Game filter section - moved to top right (ensure everything fits on screen)
        int filterX = width - 220; // base anchor to keep buttons within bounds
        
        // Get current game ID for the filter button
        String currentGameId = me.ballmc.gomod.features.MapQueuer.getCurrentGameId();
        String filterButtonText = "Filter Current Map";
        if (currentGameId != null && !currentGameId.isEmpty()) {
            filterButtonText = "Filter " + currentGameId;
            System.out.println("[MapQueuerGUI] Setting filter button text to: " + filterButtonText);
        } else {
            System.out.println("[MapQueuerGUI] No game ID detected, using default button text");
        }
        
        // Make the filter button wider to accommodate game ID
        addFilterButton = new GuiButton(10, filterX + 25, 10, 150, 20, filterButtonText);
        // Center the Clear button below the Filter button
        clearFiltersButton = new GuiButton(11, filterX + 25 + (150 - 58) / 2, 35, 58, 20, "Clear");
        // Use a real GuiButton so it looks and behaves like a button
        infoButton = new GuiButton(12, filterX, 10, 20, 20, "?");
        buttonList.add(addFilterButton);
        buttonList.add(clearFiltersButton);
        buttonList.add(infoButton);
        
        // Start button - use standard button height (20) for correct skin
        startButton = new GuiButton(5, centerX - 75, height - 80, 150, 20, "Select Maps");
        buttonList.add(startButton);
        
        // Stop button - positioned under start button when queuing
        stopButton = new GuiButton(8, centerX - 75, height - 55, 150, 20, EnumChatFormatting.RED + "Stop queuing");
        buttonList.add(stopButton);
        
        // Create map buttons in a grid layout
        createMapButtons();
        
        // Update button states
        updateModeButtons();
        updateStartButton();
        updateMapButtons();
        updateFilterButtons();
    }
    
    private void createMapButtons() {
        mapButtons = new GuiButton[allMaps.length];
        int centerX = width / 2;
        int startY = 135; // Base Y for map grid (contentTop) - moved down to make room for filter section
        int buttonWidth = 100;
        int buttonHeight = 20;
        int cols = 4; // 4 columns
        int spacing = 5;
        
        for (int i = 0; i < allMaps.length; i++) {
            int row = i / cols;
            int col = i % cols;
            
            int x = centerX - (cols * (buttonWidth + spacing)) / 2 + col * (buttonWidth + spacing);
            int y = startY + row * (buttonHeight + spacing) - scrollOffset;
            
            String mapName = allMaps[i];
            String displayName = mapName;
            
            // Color code based on mode restrictions
            if (MapQueuer.isTeamsOnlyMap(mapName)) {
                displayName = EnumChatFormatting.RED + mapName;
            } else if (MapQueuer.isSoloOnlyMap(mapName)) {
                displayName = EnumChatFormatting.BLUE + mapName;
            } else {
                displayName = EnumChatFormatting.WHITE + mapName;
            }
            
            mapButtons[i] = new GuiButton(100 + i, x, y, buttonWidth, buttonHeight, displayName);
            buttonList.add(mapButtons[i]);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Draw background
        drawDefaultBackground();
        
        // Draw title
        drawCenteredString(fontRendererObj, 
            EnumChatFormatting.BOLD.toString() + EnumChatFormatting.GOLD.toString() + "Map Queuer", 
            width / 2, 20, 0xFFFFFF);
        
        // Draw instructions
        drawCenteredString(fontRendererObj, 
            EnumChatFormatting.YELLOW.toString() + "Select a map and mode, then start queuing!", 
            width / 2, 35, 0xFFFFFF);
        
        // Game filter buttons are drawn by super.drawScreen
        // Update filter button states in real-time
        updateFilterButtons();
        
        // Draw selected maps and mode (between instructions and mode buttons)
        int selectedCount = getSelectedMapCount();
        if (selectedCount > 0) {
            double percentage = MapQueuer.getMapSelectionPercentage(mapSelected, selectedMode);
            String selectedMapsText = "Selected: " + selectedCount + " map(s) (" + selectedMode + ") - " + 
                String.format("%.1f", percentage) + "% chance";
            drawCenteredString(fontRendererObj, 
                EnumChatFormatting.AQUA.toString() + selectedMapsText, 
                width / 2, 48, 0xFFFFFF);
        }
        
        // Draw filtered games list - moved to top right
        drawFilteredGamesList();
        
        // Status is now shown in chat as clickable message
        
        // Draw map legend
        drawString(fontRendererObj, 
            EnumChatFormatting.BLUE.toString() + "Blue = Solo Only", 
            10, height - 60, 0xFFFFFF);
        drawString(fontRendererObj, 
            EnumChatFormatting.RED.toString() + "Red = Teams Only", 
            10, height - 50, 0xFFFFFF);
        
        // Update positions of header (mode/select) and map buttons according to current scroll offset
        computeMapMaxScroll();
        int centerX = width / 2;
        if (soloButton != null) soloButton.yPosition = 60 - scrollOffset;
        if (teamsButton != null) teamsButton.yPosition = 60 - scrollOffset;
        if (selectAllButton != null) selectAllButton.yPosition = 85 - scrollOffset;
        if (clearAllButton != null) clearAllButton.yPosition = 85 - scrollOffset;

        // Respect visible area for header controls to avoid off-screen click targets
        int visibleTop = 20;
        int visibleBottom = this.height - 40;
        if (soloButton != null) soloButton.visible = soloButton.yPosition >= visibleTop && soloButton.yPosition + soloButton.height <= visibleBottom;
        if (teamsButton != null) teamsButton.visible = teamsButton.yPosition >= visibleTop && teamsButton.yPosition + teamsButton.height <= visibleBottom;
        if (selectAllButton != null) selectAllButton.visible = selectAllButton.yPosition >= visibleTop && selectAllButton.yPosition + selectAllButton.height <= visibleBottom;
        if (clearAllButton != null) clearAllButton.visible = clearAllButton.yPosition >= visibleTop && clearAllButton.yPosition + clearAllButton.height <= visibleBottom;

        int contentTop = 115;
        int buttonWidth = 100;
        int buttonHeight = 20;
        int cols = 4;
        int spacing = 5;
        for (int i = 0; i < allMaps.length; i++) {
            int row = i / cols;
            int col = i % cols;
            int x = centerX - (cols * (buttonWidth + spacing)) / 2 + col * (buttonWidth + spacing);
            int y = contentTop + row * (buttonHeight + spacing) - scrollOffset;
            if (mapButtons[i] != null) {
                mapButtons[i].xPosition = x;
                mapButtons[i].yPosition = y;
                mapButtons[i].visible = y >= visibleTop && y + buttonHeight <= visibleBottom;
            }
        }

        // Position start/stop buttons: scroll with content only when scrolling is needed
        if (maxScroll > 0) {
            int rows = (int) Math.ceil(allMaps.length / (double) cols);
            int gridBottom = contentTop + (rows - 1) * (buttonHeight + spacing) + buttonHeight; // precise bottom
            int footerTop = gridBottom + 15; // spacing below grid
            if (startButton != null) startButton.yPosition = footerTop - scrollOffset;
            if (stopButton != null) stopButton.yPosition = (footerTop + 25) - scrollOffset;
        } else {
            if (startButton != null) startButton.yPosition = height - 80;
            if (stopButton != null) stopButton.yPosition = height - 55;
        }
        // Ensure start/stop are visible only when inside visible area
        if (startButton != null) startButton.visible = startButton.yPosition + startButton.height <= visibleBottom;
        // Stop button should always be visible when MapQueuer is running, regardless of scroll
        if (stopButton != null) {
            boolean isRunning = MapQueuer.isRunning();
            if (isRunning) {
                stopButton.visible = true; // Always visible when running
            } else {
                stopButton.visible = stopButton.yPosition + stopButton.height <= visibleBottom;
            }
        }

        // Draw scrollbar for the grid area
        drawMapScrollbar();

        super.drawScreen(mouseX, mouseY, partialTicks);
        
        // Draw green overlays for selected map buttons (after buttons are drawn)
        drawSelectedMapOverlays();
    }

    @Override
    public void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            // Back button
            mc.displayGuiScreen(parent);
        } else if (button.id == 3) {
            // Solo mode
            selectedMode = "solo";
            // Start animation for mode button
            overlayAnimationStart.put("mode_solo", System.currentTimeMillis());
            // Deselect teams-only maps when switching to solo
            deselectIncompatibleMaps();
            updateModeButtons();
            updateMapButtons(); // This will disable incompatible buttons
            updateStartButton();
            MapQueuer.saveSelectedMaps(mapSelected, selectedMode);
            if (MapQueuer.isRunning()) {
                MapQueuer.updateSelection(getSelectedMaps(), selectedMode);
            }
        } else if (button.id == 4) {
            // Teams mode
            selectedMode = "teams";
            // Start animation for mode button
            overlayAnimationStart.put("mode_teams", System.currentTimeMillis());
            // Deselect solo-only maps when switching to teams
            deselectIncompatibleMaps();
            updateModeButtons();
            updateMapButtons(); // This will disable incompatible buttons
            updateStartButton();
            MapQueuer.saveSelectedMaps(mapSelected, selectedMode);
            if (MapQueuer.isRunning()) {
                MapQueuer.updateSelection(getSelectedMaps(), selectedMode);
            }
        } else if (button.id == 5) {
            // Start button
            String[] selectedMaps = getSelectedMaps();
            if (selectedMaps.length > 0) {
                MapQueuer.startMapQueuer(selectedMaps, selectedMode);
                mc.displayGuiScreen(null); // Close GUI
            }
        } else if (button.id == 8) {
            // Stop button
            MapQueuer.stopMapQueuer();
            updateStartButton();
        } else if (button.id == 6) {
            // Select All button
            selectAllMaps();
            updateMapButtons();
            updateStartButton();
            MapQueuer.saveSelectedMaps(mapSelected, selectedMode);
            if (MapQueuer.isRunning()) {
                MapQueuer.updateSelection(getSelectedMaps(), selectedMode);
            }
        } else if (button.id == 7) {
            // Clear All button
            clearAllMaps();
            updateMapButtons();
            updateStartButton();
            MapQueuer.saveSelectedMaps(mapSelected, selectedMode);
            if (MapQueuer.isRunning()) {
                MapQueuer.updateSelection(getSelectedMaps(), selectedMode);
            }
        } else if (button.id == 10) {
            // Filter Current Map button
            System.out.println("[MapQueuerGUI] Filter button clicked!");
            String currentGameId = me.ballmc.gomod.features.MapQueuer.getCurrentGameId();
            System.out.println("[MapQueuerGUI] Current game ID: " + currentGameId);
            if (currentGameId != null && !currentGameId.isEmpty()) {
                addGameFilter(currentGameId);
                updateFilterButtons(); // This will update the button text and state
                System.out.println("[MapQueuerGUI] Filter added successfully");
            } else {
                Main.sendMessage(EnumChatFormatting.RED + "No game ID detected! Make sure you're in a game or lobby.");
                System.out.println("[MapQueuerGUI] No game ID detected");
            }
        } else if (button.id == 11) {
            // Clear Filters button
            clearAllFilters();
            updateFilterButtons();
        } else if (button.id == 12) {
            // Info button
            showFilterInfo();
        } else if (button.id >= 100 && button.id < 100 + allMaps.length) {
            // Map selection toggle
            int mapIndex = button.id - 100;
            String mapName = allMaps[mapIndex];
            
            // Check if button is enabled (compatible with current mode)
            if (!button.enabled) {
                // Button is disabled, show appropriate message
                if (selectedMode.equals("solo") && MapQueuer.isTeamsOnlyMap(mapName)) {
                    Main.sendMessage(EnumChatFormatting.RED + mapName + " is only available in Teams mode!");
                } else if (selectedMode.equals("teams") && MapQueuer.isSoloOnlyMap(mapName)) {
                    Main.sendMessage(EnumChatFormatting.RED + mapName + " is only available in Solo mode!");
                }
                return;
            }
            
            boolean wasSelected = mapSelected[mapIndex];
            mapSelected[mapIndex] = !mapSelected[mapIndex];
            
            // Start animation when map becomes selected
            if (!wasSelected && mapSelected[mapIndex]) {
                String mapKey = mapName + "_" + (mapIndex);
                overlayAnimationStart.put(mapKey, System.currentTimeMillis());
            } else if (wasSelected && !mapSelected[mapIndex]) {
                // Cancel animation when map becomes deselected
                String mapKey = mapName + "_" + (mapIndex);
                overlayAnimationStart.remove(mapKey);
            }
            
            // Check if we need to change mode based on selected maps
            validateModeForSelectedMaps();
            
            updateMapButtons();
            updateStartButton();
            
            // Auto-save selection
            MapQueuer.saveSelectedMaps(mapSelected, selectedMode);
            if (MapQueuer.isRunning()) {
                MapQueuer.updateSelection(getSelectedMaps(), selectedMode);
            }
        }
    }
    
    private void updateModeButtons() {
        if (soloButton != null && teamsButton != null) {
            if (selectedMode.equals("solo")) {
                soloButton.displayString = EnumChatFormatting.BLUE + "Solo Mode";
                teamsButton.displayString = EnumChatFormatting.RED + "Teams Mode";
            } else {
                soloButton.displayString = EnumChatFormatting.BLUE + "Solo Mode";
                teamsButton.displayString = EnumChatFormatting.RED + "Teams Mode";
            }
        }
    }
    
    private void updateStartButton() {
        if (startButton != null && stopButton != null) {
            int selectedCount = getSelectedMapCount();
            boolean isRunning = MapQueuer.isRunning();
            
            if (isRunning) {
                // When queuing, show "Queuing" and enable stop button
                startButton.displayString = EnumChatFormatting.GREEN + "Queuing (" + selectedCount + ")...";
                startButton.enabled = false;
                stopButton.visible = true;
                stopButton.enabled = true;
            } else {
                // When not queuing, show start button and hide stop button
                if (selectedCount == 0) {
                    startButton.displayString = EnumChatFormatting.GRAY + "Select Maps";
                    startButton.enabled = false;
                } else {
                    startButton.displayString = EnumChatFormatting.GOLD + "Start Queuer (" + selectedCount + ")";
                    startButton.enabled = true;
                }
                stopButton.visible = false;
                stopButton.enabled = false;
            }
        }
    }
    
    private int getSelectedMapCount() {
        int count = 0;
        for (boolean selected : mapSelected) {
            if (selected) count++;
        }
        return count;
    }
    
    private String[] getSelectedMaps() {
        java.util.List<String> selected = new java.util.ArrayList<>();
        for (int i = 0; i < allMaps.length; i++) {
            if (mapSelected[i]) {
                selected.add(allMaps[i]);
            }
        }
        return selected.toArray(new String[0]);
    }
    
    private void selectAllMaps() {
        for (int i = 0; i < mapSelected.length; i++) {
            String mapName = allMaps[i];
            // Only select maps compatible with current mode
            if (selectedMode.equals("solo")) {
                mapSelected[i] = !MapQueuer.isTeamsOnlyMap(mapName);
            } else if (selectedMode.equals("teams")) {
                mapSelected[i] = !MapQueuer.isSoloOnlyMap(mapName);
            } else {
                mapSelected[i] = true;
            }
        }
        // Update button states after selecting
        updateMapButtons();
    }
    
    private void clearAllMaps() {
        for (int i = 0; i < mapSelected.length; i++) {
            mapSelected[i] = false;
        }
        // Update button states after clearing
        updateMapButtons();
    }
    
    private void deselectIncompatibleMaps() {
        for (int i = 0; i < allMaps.length; i++) {
            String mapName = allMaps[i];
            if (selectedMode.equals("solo") && MapQueuer.isTeamsOnlyMap(mapName)) {
                mapSelected[i] = false;
            } else if (selectedMode.equals("teams") && MapQueuer.isSoloOnlyMap(mapName)) {
                mapSelected[i] = false;
            }
        }
    }
    
    private void validateModeForSelectedMaps() {
        boolean hasTeamsOnly = false;
        boolean hasSoloOnly = false;
        
        for (int i = 0; i < allMaps.length; i++) {
            if (mapSelected[i]) {
                if (MapQueuer.isTeamsOnlyMap(allMaps[i])) {
                    hasTeamsOnly = true;
                }
                if (MapQueuer.isSoloOnlyMap(allMaps[i])) {
                    hasSoloOnly = true;
                }
            }
        }
        
        // Auto-switch mode if needed
        if (hasTeamsOnly && selectedMode.equals("solo")) {
            selectedMode = "teams";
            updateModeButtons();
        } else if (hasSoloOnly && selectedMode.equals("teams")) {
            selectedMode = "solo";
            updateModeButtons();
        }
    }
    
    private void updateMapButtons() {
        for (int i = 0; i < mapButtons.length; i++) {
            if (mapButtons[i] != null) {
                String mapName = allMaps[i];
                String displayName = mapName;
                
                // Check if map is compatible with current mode
                boolean isCompatible = true;
                if (selectedMode.equals("solo") && MapQueuer.isTeamsOnlyMap(mapName)) {
                    isCompatible = false;
                } else if (selectedMode.equals("teams") && MapQueuer.isSoloOnlyMap(mapName)) {
                    isCompatible = false;
                }
                
                // Color code based on mode restrictions and compatibility
                if (MapQueuer.isTeamsOnlyMap(mapName)) {
                    displayName = EnumChatFormatting.RED + displayName;
                } else if (MapQueuer.isSoloOnlyMap(mapName)) {
                    displayName = EnumChatFormatting.BLUE + displayName;
                } else {
                    displayName = EnumChatFormatting.WHITE + displayName;
                }
                
                // Add strikethrough and gray out incompatible maps
                if (!isCompatible) {
                    displayName = EnumChatFormatting.STRIKETHROUGH.toString() + EnumChatFormatting.GRAY.toString() + displayName;
                }
                
                mapButtons[i].displayString = displayName;
                mapButtons[i].enabled = isCompatible; // Disable incompatible maps
            }
        }
    }

    private void drawSelectedMapOverlays() {
        int centerX = width / 2;
        int contentTop = 115;
        int buttonWidth = 100;
        int buttonHeight = 20;
        int cols = 4;
        int spacing = 5;
        
        // Draw overlays for selected map buttons and animations
        for (int i = 0; i < allMaps.length; i++) {
            if (mapButtons[i] != null) {
                int row = i / cols;
                int col = i % cols;
                int x = centerX - (cols * (buttonWidth + spacing)) / 2 + col * (buttonWidth + spacing);
                int y = contentTop + row * (buttonHeight + spacing) - scrollOffset;
                
                // Only draw overlay if button is visible
                int visibleTop = 20;
                int visibleBottom = this.height - 40;
                if (y >= visibleTop && y + buttonHeight <= visibleBottom) {
                    String mapKey = allMaps[i] + "_" + i;
                    
                    // Check if this map has an active animation (regardless of selection state)
                    if (overlayAnimationStart.containsKey(mapKey)) {
                        drawAnimatedGreenOverlay(x, y, buttonWidth, buttonHeight, mapKey);
                    } else if (mapSelected[i]) {
                        // Only draw static overlay if map is selected and no animation
                        drawGreenOverlay(x, y, buttonWidth, buttonHeight);
                    }
                }
            }
        }
        
        // Draw overlays for mode buttons (animations and selected state)
        if (soloButton != null) {
            if (overlayAnimationStart.containsKey("mode_solo")) {
                drawAnimatedGreenOverlay(soloButton.xPosition, soloButton.yPosition, soloButton.width, soloButton.height, "mode_solo");
            } else if (selectedMode.equals("solo")) {
                drawGreenOverlay(soloButton.xPosition, soloButton.yPosition, soloButton.width, soloButton.height);
            }
        }
        if (teamsButton != null) {
            if (overlayAnimationStart.containsKey("mode_teams")) {
                drawAnimatedGreenOverlay(teamsButton.xPosition, teamsButton.yPosition, teamsButton.width, teamsButton.height, "mode_teams");
            } else if (selectedMode.equals("teams")) {
                drawGreenOverlay(teamsButton.xPosition, teamsButton.yPosition, teamsButton.width, teamsButton.height);
            }
        }
    }
    
    private void drawGreenOverlay(int x, int y, int width, int height) {
        // Ensure overlay renders above buttons
        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        net.minecraft.client.renderer.GlStateManager.translate(0, 0, 300);
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        
        // Draw green overlay (very transparent to not interfere with text)
        net.minecraft.client.gui.Gui.drawRect(x, y, x + width, y + height, 0x204CAF50);
        
        // Draw green border (thicker for better visibility)
        net.minecraft.client.gui.Gui.drawRect(x, y, x + width, y + 2, 0xFF4CAF50); // Top
        net.minecraft.client.gui.Gui.drawRect(x, y, x + 2, y + height, 0xFF4CAF50); // Left
        net.minecraft.client.gui.Gui.drawRect(x, y + height - 2, x + width, y + height, 0xFF4CAF50); // Bottom
        net.minecraft.client.gui.Gui.drawRect(x + width - 2, y, x + width, y + height, 0xFF4CAF50); // Right
        
        net.minecraft.client.renderer.GlStateManager.popMatrix();
    }
    
    private void drawAnimatedGreenOverlay(int x, int y, int width, int height, String mapKey) {
        // Ensure overlay renders above buttons
        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        net.minecraft.client.renderer.GlStateManager.translate(0, 0, 300);
        net.minecraft.client.renderer.GlStateManager.enableBlend();

        // Get animation progress
        Long startTime = overlayAnimationStart.get(mapKey);
        if (startTime == null) {
            // No animation data, draw full border
            drawGreenOverlay(x, y, width, height);
            net.minecraft.client.renderer.GlStateManager.popMatrix();
            return;
        }
        
        // Draw green overlay (very transparent to not interfere with text)
        net.minecraft.client.gui.Gui.drawRect(x, y, x + width, y + height, 0x204CAF50);
        
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - startTime;
        float progress = Math.min(1.0f, (float) elapsed / ANIMATION_DURATION_MS);
        
        
        // Calculate border completion based on progress
        // Animation: Start from middle of left and right edges, expand up and down simultaneously
        int borderThickness = 2;
        
        // Calculate smooth progress for each direction
        // Left and right sides grow from center outward
        float leftRightProgress = Math.min(1.0f, progress * 2.0f); // First half of animation
        float topBottomProgress = Math.max(0.0f, (progress - 0.5f) * 2.0f); // Second half of animation
        
        // Left side: start from middle, expand up and down smoothly
        int leftCenterY = y + height / 2;
        int leftMaxPixels = height / 2;
        int leftUpPixels = (int) (leftMaxPixels * leftRightProgress);
        int leftDownPixels = (int) (leftMaxPixels * leftRightProgress);
        
        if (leftUpPixels > 0) {
            net.minecraft.client.gui.Gui.drawRect(x, leftCenterY - leftUpPixels, x + borderThickness, leftCenterY, 0xFF4CAF50);
        }
        if (leftDownPixels > 0) {
            net.minecraft.client.gui.Gui.drawRect(x, leftCenterY, x + borderThickness, leftCenterY + leftDownPixels, 0xFF4CAF50);
        }
        
        // Right side: start from middle, expand up and down smoothly
        int rightCenterY = y + height / 2;
        int rightMaxPixels = height / 2;
        int rightUpPixels = (int) (rightMaxPixels * leftRightProgress);
        int rightDownPixels = (int) (rightMaxPixels * leftRightProgress);
        
        if (rightUpPixels > 0) {
            net.minecraft.client.gui.Gui.drawRect(x + width - borderThickness, rightCenterY - rightUpPixels, x + width, rightCenterY, 0xFF4CAF50);
        }
        if (rightDownPixels > 0) {
            net.minecraft.client.gui.Gui.drawRect(x + width - borderThickness, rightCenterY, x + width, rightCenterY + rightDownPixels, 0xFF4CAF50);
        }
        
        // Top border: draw from left and right edges toward center smoothly
        int topMaxPixels = width / 2;
        int topLeftPixels = (int) (topMaxPixels * topBottomProgress);
        int topRightPixels = (int) (topMaxPixels * topBottomProgress);
        
        if (topLeftPixels > 0) {
            net.minecraft.client.gui.Gui.drawRect(x, y, x + topLeftPixels, y + borderThickness, 0xFF4CAF50);
        }
        if (topRightPixels > 0) {
            net.minecraft.client.gui.Gui.drawRect(x + width - topRightPixels, y, x + width, y + borderThickness, 0xFF4CAF50);
        }
        
        // Bottom border: draw from left and right edges toward center smoothly
        int bottomMaxPixels = width / 2;
        int bottomLeftPixels = (int) (bottomMaxPixels * topBottomProgress);
        int bottomRightPixels = (int) (bottomMaxPixels * topBottomProgress);
        
        if (bottomLeftPixels > 0) {
            net.minecraft.client.gui.Gui.drawRect(x, y + height - borderThickness, x + bottomLeftPixels, y + height, 0xFF4CAF50);
        }
        if (bottomRightPixels > 0) {
            net.minecraft.client.gui.Gui.drawRect(x + width - bottomRightPixels, y + height - borderThickness, x + width, y + height, 0xFF4CAF50);
        }
        
        // Clean up completed animations
        if (progress >= 1.0f) {
            overlayAnimationStart.remove(mapKey);
        }

        net.minecraft.client.renderer.GlStateManager.popMatrix();
    }

    private void computeMapMaxScroll() {
        int buttonHeight = 20;
        int spacing = 5;
        int cols = 4;
        int rows = (int) Math.ceil(allMaps.length / (double) cols);
        int contentTop = 115;
        int gridBottom = contentTop + (rows - 1) * (buttonHeight + spacing) + buttonHeight;
        int footerTop = gridBottom + 15; // start/stop area
        int footerBottom = footerTop + 20 + 25 + 20; // start(20) + spacing(25) + stop(20)
        int contentBottom = Math.max(gridBottom, footerBottom);
        int visibleBottom = height - 40; // leave room for back button
        maxScroll = Math.max(0, contentBottom - visibleBottom);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    private void drawMapScrollbar() {
        computeMapMaxScroll();
        if (maxScroll <= 0) return;

        int trackX = this.width - 10;
        int trackWidth = 4;
        int visibleTop = 20;
        int visibleBottom = this.height - 40;
        int trackTop = Math.max(visibleTop, 30);
        int trackBottom = Math.max(trackTop + 1, visibleBottom);
        int trackHeight = trackBottom - trackTop;

        int buttonHeight = 20;
        int spacing = 5;
        int cols = 4;
        int rows = (int) Math.ceil(allMaps.length / (double) cols);
        int gridBottom = 100 + (rows - 1) * (buttonHeight + spacing) + buttonHeight;
        int footerTop = gridBottom + 15;
        int contentBottom = Math.max(gridBottom, footerTop + 20 + 25 + 20);
        int totalContentHeight = Math.max(1, contentBottom - visibleTop);
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
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        // No manual handling needed for info button; it's a GuiButton now
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
    
    // Game filter methods
    public static void addGameFilter(String gameId) {
        long currentTime = System.currentTimeMillis();
        filteredGames.put(gameId.toLowerCase(), currentTime + FILTER_DURATION_MS);
        Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN + "Added game filter: " + gameId + " (15 minutes)");
    }
    
    private void showFilterInfo() {
        // Show help dialog instead of chat messages
        mc.displayGuiScreen(new FilterInfoDialog(this));
    }
    
    private void clearAllFilters() {
        filteredGames.clear();
        Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Cleared all game filters");
    }
    
    private void updateFilterButtons() {
        // Update button states based on current filters
        if (addFilterButton != null) {
            // Check if current game ID is available and not already filtered
            String currentGameId = me.ballmc.gomod.features.MapQueuer.getCurrentGameId();
            boolean hasGameId = currentGameId != null && !currentGameId.isEmpty();
            boolean isAlreadyFiltered = hasGameId && filteredGames.containsKey(currentGameId.toLowerCase());
            
            // Update button text based on current state
            if (isAlreadyFiltered) {
                addFilterButton.displayString = EnumChatFormatting.GREEN + "Filtered " + currentGameId;
                addFilterButton.enabled = false;
            } else if (hasGameId) {
                addFilterButton.displayString = "Filter " + currentGameId;
                addFilterButton.enabled = true;
            } else {
                addFilterButton.displayString = "Filter Current Map";
                addFilterButton.enabled = false;
            }
            
            System.out.println("[MapQueuerGUI] updateFilterButtons - Game ID: " + currentGameId + ", hasGameId: " + hasGameId + ", isAlreadyFiltered: " + isAlreadyFiltered + ", button enabled: " + addFilterButton.enabled);
        }
        if (clearFiltersButton != null) {
            clearFiltersButton.enabled = !filteredGames.isEmpty();
        }
    }
    
    private void drawFilteredGamesList() {
        if (filteredGames.isEmpty()) return;
        
        int startY = 60; // Moved to top right area
        int lineHeight = 12;
        int maxLines = 3; // Show up to 3 filtered games
        int lineCount = 0;
        int filterX = width - 200; // Right side of screen
        
        long currentTime = System.currentTimeMillis();
        java.util.Iterator<java.util.Map.Entry<String, Long>> iterator = filteredGames.entrySet().iterator();
        
        while (iterator.hasNext() && lineCount < maxLines) {
            java.util.Map.Entry<String, Long> entry = iterator.next();
            String gameId = entry.getKey();
            Long expireTime = entry.getValue();
            
            if (expireTime <= currentTime) {
                // Filter expired, remove it
                iterator.remove();
                continue;
            }
            
            long remainingMs = expireTime - currentTime;
            long remainingMinutes = remainingMs / (60 * 1000);
            long remainingSeconds = (remainingMs % (60 * 1000)) / 1000;
            
            String timeText = remainingMinutes > 0 ? 
                remainingMinutes + "m " + remainingSeconds + "s" : 
                remainingSeconds + "s";
            
            String displayText = EnumChatFormatting.RED + "Filtered: " + gameId + " (" + timeText + ")";
            drawString(fontRendererObj, displayText, filterX, startY + lineCount * lineHeight, 0xFFFFFF);
            lineCount++;
        }
    }
    
    // Static method to check if a game is filtered (called from MapQueuer)
    public static boolean isGameFiltered(String gameId) {
        if (gameId == null || gameId.isEmpty()) {
            System.out.println("[MapQueuer] isGameFiltered - Game ID is null or empty");
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        String lowerGameId = gameId.toLowerCase();
        Long expireTime = filteredGames.get(lowerGameId);
        
        System.out.println("[MapQueuer] isGameFiltered - Checking game ID: " + gameId + " (lowercase: " + lowerGameId + ")");
        System.out.println("[MapQueuer] isGameFiltered - Expire time: " + expireTime + ", Current time: " + currentTime);
        System.out.println("[MapQueuer] isGameFiltered - Filtered games: " + filteredGames.keySet());
        
        if (expireTime == null) {
            System.out.println("[MapQueuer] isGameFiltered - No filter found for this game ID");
            return false;
        }
        
        if (expireTime <= currentTime) {
            // Filter expired, remove it
            filteredGames.remove(lowerGameId);
            System.out.println("[MapQueuer] isGameFiltered - Filter expired, removed");
            return false;
        }
        
        System.out.println("[MapQueuer] isGameFiltered - Game is filtered!");
        return true;
    }
    
    // Dialog for showing filter help information
    private static class FilterInfoDialog extends GuiScreen {
        private final GuiScreen parent;
        private GuiButton closeButton;
        
        public FilterInfoDialog(GuiScreen parent) {
            this.parent = parent;
        }
        
        @Override
        public void initGui() {
            int centerX = width / 2;
            closeButton = new GuiButton(0, centerX - 50, height / 2 + 40, 100, 20, EnumChatFormatting.GRAY + "Close");
            buttonList.clear();
            buttonList.add(closeButton);
        }
        
        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            // Draw semi-transparent background (lighter so parent GUI shows through)
            drawRect(0, 0, width, height, 0x60000000);
            
            int centerX = width / 2;
            int centerY = height / 2;
            int dialogWidth = 300;
            int dialogHeight = 80;
            int dialogX = centerX - dialogWidth / 2;
            int dialogY = centerY - dialogHeight / 2;
            
            // Draw dialog background with opacity
            drawRect(dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, 0xCC000000);
            
            // Draw border
            drawRect(dialogX, dialogY, dialogX + dialogWidth, dialogY + 1, 0xFF4CAF50); // Top
            drawRect(dialogX, dialogY, dialogX + 1, dialogY + dialogHeight, 0xFF4CAF50); // Left
            drawRect(dialogX + dialogWidth - 1, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, 0xFF4CAF50); // Right
            drawRect(dialogX, dialogY + dialogHeight - 1, dialogX + dialogWidth, dialogY + dialogHeight, 0xFF4CAF50); // Bottom
            
            // Draw title
            drawCenteredString(fontRendererObj, EnumChatFormatting.GOLD + "Game Filter Help", centerX, dialogY + 10, 0xFFFFFF);
            
            // Draw help text
            String line1 = EnumChatFormatting.YELLOW + "Click the Filter button (e.g., Filter m188CF) to filter it";
            String line2 = EnumChatFormatting.YELLOW + "Filtered games will be skipped for 15 minutes";
            
            drawCenteredString(fontRendererObj, line1, centerX, dialogY + 25, 0xFFFFFF);
            drawCenteredString(fontRendererObj, line2, centerX, dialogY + 40, 0xFFFFFF);
            
            super.drawScreen(mouseX, mouseY, partialTicks);
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

