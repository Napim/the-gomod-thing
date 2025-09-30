package me.ballmc.gomod.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import static me.ballmc.gomod.Main.sendMessage;
import me.ballmc.gomod.Main;

/**
 * Inventory HUD feature that displays the player's inventory in a corner of the screen
 */
public class InventoryHUD extends Gui {
    private static final ResourceLocation INVENTORY_TEXTURE = new ResourceLocation("textures/gui/container/inventory.png");
    private static boolean textureLoaded = false;
    private static final InventoryHUD INSTANCE = new InventoryHUD();
    private static boolean enabled = false;
    private static boolean showEmpty = false; // Show HUD even when inventory is empty
    private static boolean force = false; // Force show HUD even when in GUI
    private static int customX = 5; // Custom X position
    private static int customY = 5; // Custom Y position
    private static float scale = 2.0f; // Scale factor for the HUD (1.0-4.0, supports 1.5)
    private static int backgroundOpacity = 110; // Background opacity (0-255)
    private static int backgroundColor = 0x000000; // Background color (RGB)
    
    /**
     * Loads the inventory HUD settings from config
     */
    public static void loadSettings() {
        enabled = ConfigManager.isInventoryHUDEnabled();
        showEmpty = ConfigManager.isInventoryHUDShowEmpty();
        force = ConfigManager.isInventoryHUDForce();
        customX = ConfigManager.getInventoryHUDCustomX();
        customY = ConfigManager.getInventoryHUDCustomY();
        scale = ConfigManager.getInventoryHUDScale();
        backgroundOpacity = ConfigManager.getInventoryHUDBackgroundOpacity();
        backgroundColor = ConfigManager.getInventoryHUDBackgroundColor();
    }
    
    /**
     * Saves the inventory HUD settings to config
     */
    public static void saveSettings() {
        ConfigManager.setInventoryHUDEnabled(enabled);
        ConfigManager.setInventoryHUDShowEmpty(showEmpty);
        ConfigManager.setInventoryHUDForce(force);
        ConfigManager.setInventoryHUDCustomX(customX);
        ConfigManager.setInventoryHUDCustomY(customY);
        ConfigManager.setInventoryHUDScale(scale);
        ConfigManager.setInventoryHUDBackgroundOpacity(backgroundOpacity);
        ConfigManager.setInventoryHUDBackgroundColor(backgroundColor);
    }
    
    /**
     * Toggles the inventory HUD on/off
     */
    public static void toggle() {
        enabled = !enabled;
        saveSettings();
        
        // No debug message needed
    }
    
    /**
     * Toggles whether to show the HUD when inventory is empty
     */
    public static void toggleShowEmpty() {
        showEmpty = !showEmpty;
        saveSettings();
        
        // No debug message needed
    }
    
    /**
     * Toggles whether to force show the HUD even when in GUI
     */
    public static void toggleForce() {
        force = !force;
        saveSettings();
        
        // No debug message needed
    }
    
    /**
     * Sets custom X-Y position of the inventory HUD
     * @param x X coordinate
     * @param y Y coordinate
     */
    public static void setCustomPosition(int x, int y) {
        customX = x;
        customY = y;
        saveSettings();
        // No debug message needed
    }
    
    /**
     * Resets position to default (top-left)
     */
    public static void resetPosition() {
        // Capture current position before resetting
        int previousX = customX;
        int previousY = customY;
        
        customX = 5;
        customY = 5;
        saveSettings();
        sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Inventory HUD position reset from " + previousX + ", " + previousY + " to default (5, 5)");
    }
    
    /**
     * Sets the scale of the inventory HUD
     * @param newScale Scale factor (1.0-4.0, supports 1.5)
     */
    public static void setScale(float newScale) {
        if (newScale >= 1.0f && newScale <= 4.0f) {
            // Keep the visual center stable when changing scale by shifting customX/customY
            float previousScale = scale;
            if (previousScale <= 0) {
                previousScale = 1.0f;
            }

            // Background area visual size (uses 18px slots: 9*18 by 3*18 = 162x54)
            int previousBgWidth = (int)(162 * previousScale);
            int previousBgHeight = (int)(54 * previousScale);
            int newBgWidth = (int)(162 * newScale);
            int newBgHeight = (int)(54 * newScale);

            // Shift position by half the delta to preserve center
            int deltaW = newBgWidth - previousBgWidth;
            int deltaH = newBgHeight - previousBgHeight;
            customX -= deltaW / 2;
            customY -= deltaH / 2;

            scale = newScale;
            saveSettings();
            // No debug message needed
        } else {
            sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Invalid scale! Use 1.0-4.0 (supports 1.5)");
        }
    }
    
    /**
     * Sets the background opacity of the inventory HUD
     * @param newOpacity Opacity value (0-255)
     */
    public static void setBackgroundOpacity(int newOpacity) {
        if (newOpacity >= 0 && newOpacity <= 255) {
            backgroundOpacity = newOpacity;
            saveSettings();
            // No debug message needed
        } else {
            sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Invalid opacity! Use 0-255 (0=transparent, 255=opaque)");
        }
    }
    
    /**
     * Gets the position name for display
     */
    private static String getPositionName(int pos) {
        switch (pos) {
            case 0: return "top-left";
            case 1: return "top-right";
            case 2: return "bottom-left";
            case 3: return "bottom-right";
            default: return "unknown";
        }
    }
    
    /**
     * Checks if the inventory HUD is enabled
     */
    public static boolean isEnabled() {
        return enabled;
    }
    

    
    /**
     * Gets the current custom X position
     */
    public static int getCustomX() {
        return customX;
    }
    
    /**
     * Gets the current custom Y position
     */
    public static int getCustomY() {
        return customY;
    }
    
    /**
     * Gets the current scale
     */
    public static float getScale() {
        return scale;
    }
    
    /**
     * Gets the current background opacity
     */
    public static int getBackgroundOpacity() {
        return backgroundOpacity;
    }
    
    /**
     * Sets the background color of the inventory HUD
     * @param r Red component (0-255)
     * @param g Green component (0-255)
     * @param b Blue component (0-255)
     */
    public static void setBackgroundColor(int r, int g, int b) {
        backgroundColor = (r << 16) | (g << 8) | b;
        saveSettings();
        sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Inventory HUD background color set to RGB(" + r + ", " + g + ", " + b + ")");
    }
    
    /**
     * Resets background color to default (black)
     */
    public static void resetBackgroundColor() {
        backgroundColor = 0x000000;
        saveSettings();
        sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Inventory HUD background color reset to default (black)");
    }
    
    /**
     * Gets the current background color
     */
    public static int getBackgroundColor() {
        return backgroundColor;
    }
    
    /**
     * Checks if the HUD should show when inventory is empty
     */
    public static boolean isShowEmpty() {
        return showEmpty;
    }
    
    /**
     * Checks if force mode is enabled
     */
    public static boolean isForce() {
        return force;
    }
    
    /**
     * Gets the current position as a string
     */
    public static String getCurrentPosition() {
        return "(" + customX + ", " + customY + ")";
    }
    

    

    
    /**
     * Checks if the inventory has any items (excluding hotbar)
     * @return true if inventory has items, false if empty
     */
    private static boolean hasInventoryItems() {
        if (Minecraft.getMinecraft().thePlayer == null || Minecraft.getMinecraft().thePlayer.inventory == null) {
            return false;
        }
        
        // Check main inventory slots (9-35, excluding hotbar 0-8)
        for (int i = 9; i < 36; i++) {
            ItemStack itemStack = Minecraft.getMinecraft().thePlayer.inventory.getStackInSlot(i);
            if (itemStack != null) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Renders the inventory HUD
     */
    public static void render() {
        if (!enabled || Minecraft.getMinecraft().thePlayer == null || Minecraft.getMinecraft().thePlayer.inventory == null) {
            return;
        }
        
        // Check if player is in a GUI (unless force mode is enabled)
        if (!force && Minecraft.getMinecraft().currentScreen != null) {
            return;
        }
        
        // Check if inventory has any items before rendering (unless showEmpty is enabled)
        if (!showEmpty && !hasInventoryItems()) {
            return;
        }
        
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int screenWidth = scaledResolution.getScaledWidth();
        int screenHeight = scaledResolution.getScaledHeight();
        
        // Calculate position and scale
        int x = customX;
        int y = customY;
        float slotSize = 16 * scale; // Item slot size (keep as float for precision)
        float textureSlotSize = 18 * scale; // Texture slot size (vanilla uses 18px, keep as float)
        int inventoryWidth = (int)(9 * textureSlotSize); // 9 slots per row (use texture size for consistency)
        int inventoryHeight = (int)(3 * textureSlotSize); // 3 rows for main inventory
        int borderSize = (int)scale; // Small border that scales

        // Draw background image with custom opacity using vanilla inventory texture (respects texture pack)
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, backgroundOpacity / 255.0F);

        try {
            // Bind the vanilla inventory texture so resource packs can override it
            Minecraft.getMinecraft().getTextureManager().bindTexture(INVENTORY_TEXTURE);

            // The vanilla inventory texture has specific dimensions for the main inventory area
            int textureStartX = 7;   // X offset in texture for main inventory
            int textureStartY = 83;  // Y offset in texture for main inventory
            int textureWidth = 162;  // Width of 9 slots (9 * 18)
            int textureHeight = 54;  // Height of 3 slots (3 * 18)

            // Scale the entire rendering context instead of stretching the texture
            GlStateManager.scale(scale, scale, 1.0F);
            
            // Draw the inventory background at the scaled position with a 1px border for crisp edges
            Gui.drawModalRectWithCustomSizedTexture(
                (int)((x - borderSize) / scale), (int)((y - borderSize) / scale),
                textureStartX - 1, textureStartY - 1,
                textureWidth + 2, textureHeight + 2,
                256, 256
            );
            textureLoaded = true;
        } catch (Exception e) {
            try {
                // Fallback to solid color rectangle if textures fail
                int backgroundColorWithOpacity = (backgroundOpacity << 24) | backgroundColor;
                Gui.drawRect((int)((x - borderSize) / scale), (int)((y - borderSize) / scale), 
                           (int)((x + inventoryWidth + borderSize) / scale), (int)((y + inventoryHeight + borderSize) / scale), 
                           backgroundColorWithOpacity);
                textureLoaded = false;
            } catch (Exception ignored) {
                textureLoaded = false;
            }
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        // Draw inventory slots - use float precision and center items within 18px slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                // Position items to align with the 18-pixel texture slots (using float precision)
                float slotX = x + col * textureSlotSize + (textureSlotSize - slotSize) / 2;
                float slotY = y + row * textureSlotSize + (textureSlotSize - slotSize) / 2;

                // Get item from inventory (main inventory slots 9-35)
                int inventorySlot = row * 9 + col + 9;
                ItemStack itemStack = Minecraft.getMinecraft().thePlayer.inventory.getStackInSlot(inventorySlot);
                
                if (itemStack != null) {
                    // Enable item rendering
                    GlStateManager.pushMatrix();
                    GlStateManager.enableRescaleNormal();
                    RenderHelper.enableGUIStandardItemLighting();
                    
                    // Scale the item rendering
                    GlStateManager.scale(scale, scale, 1.0F);
                    
                    // Render the item and its standard GUI overlay (count and durability)
                    Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(itemStack, (int)(slotX / scale), (int)(slotY / scale));
                    Minecraft.getMinecraft().getRenderItem().renderItemOverlayIntoGUI(
                        Minecraft.getMinecraft().fontRendererObj,
                        itemStack,
                        (int)(slotX / scale),
                        (int)(slotY / scale),
                        null
                    );
                    
                    // Restore GL state
                    RenderHelper.disableStandardItemLighting();
                    GlStateManager.disableRescaleNormal();
                    GlStateManager.popMatrix();
                }
            }
        }
        
        // Item overlays (stack count, durability) are handled by renderItemOverlayIntoGUI above
    }
}
