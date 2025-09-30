package me.ballmc.AntiShuffle.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.IChatComponent;
import net.weavemc.loader.api.event.RenderGameOverlayEvent;
import net.weavemc.loader.api.event.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.network.NetworkPlayerInfo;

public class PlayerLooker {
    private static final ResourceLocation CURSOR_TEXTURE = new ResourceLocation("gomod123:textures/cursor.png");
    private boolean enabled = false;
    private static final int ARROW_SIZE = 16;
    private static final int MARGIN = 10;
    private static final int HEAD_SIZE = 16;
    private String targetPlayerName = null; // Store target player name
    private static final double MAX_TRACKING_DISTANCE = 100.0; // Maximum distance to track a player
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    // Set the target player name
    public void setTargetPlayerName(String playerName) {
        this.targetPlayerName = playerName;
    }
    
    // Get the currently targeted player name
    public String getTargetPlayerName() {
        return targetPlayerName;
    }
    
    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent event) {
        if (!enabled || Minecraft.getMinecraft().thePlayer == null || Minecraft.getMinecraft().theWorld == null) return;
        
        EntityPlayer targetPlayer = findTargetPlayer();
        if (targetPlayer == null) return;
        
        // Calculate distance and angle to target player
        double horizontalDistance = getHorizontalDistance(targetPlayer);
        double verticalDistance = getVerticalDistance(targetPlayer);
        double totalDistance = Minecraft.getMinecraft().thePlayer.getDistanceToEntity(targetPlayer);
        
        // Don't render if player is too far away
        if (totalDistance > MAX_TRACKING_DISTANCE) return;
        
        double angle = calculateAngle(targetPlayer);
        
        // Render the player info, cursor, and distance with the new layout
        renderPlayerInfo(targetPlayer, horizontalDistance, verticalDistance, totalDistance, angle);
    }
    
    private EntityPlayer findTargetPlayer() {
        // If we have a target player name, try to find that specific player
        if (targetPlayerName != null && !targetPlayerName.isEmpty()) {
            for (EntityPlayer player : Minecraft.getMinecraft().theWorld.playerEntities) {
                if (player.getName().equalsIgnoreCase(targetPlayerName)) {
                    return player;
                }
            }
            return null; // Return null if the specific player is not found
        }
        
        // Fall back to nearest player if no target is specified
        return findNearestPlayer();
    }
    
    private EntityPlayer findNearestPlayer() {
        EntityPlayer nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (EntityPlayer player : Minecraft.getMinecraft().theWorld.playerEntities) {
            if (player == Minecraft.getMinecraft().thePlayer) continue;
            if (player.isInvisible()) continue;
            
            double distance = Minecraft.getMinecraft().thePlayer.getDistanceToEntity(player);
            if (distance < minDistance && distance <= MAX_TRACKING_DISTANCE) {
                minDistance = distance;
                nearest = player;
            }
        }
        
        return nearest;
    }
    
    private double getHorizontalDistance(EntityPlayer target) {
        double deltaX = target.posX - Minecraft.getMinecraft().thePlayer.posX;
        double deltaZ = target.posZ - Minecraft.getMinecraft().thePlayer.posZ;
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }
    
    private double getVerticalDistance(EntityPlayer target) {
        return target.posY - Minecraft.getMinecraft().thePlayer.posY;
    }
    
    private double calculateAngle(EntityPlayer target) {
        double deltaX = target.posX - Minecraft.getMinecraft().thePlayer.posX;
        double deltaZ = target.posZ - Minecraft.getMinecraft().thePlayer.posZ;
        
        // Calculate angle in radians
        double angle = Math.atan2(deltaZ, deltaX);
        
        // Convert to degrees and adjust for Minecraft's coordinate system
        angle = Math.toDegrees(angle) - Minecraft.getMinecraft().thePlayer.rotationYaw + 180;
        
        // Normalize angle to 0-360 range
        while (angle < 0) angle += 360;
        while (angle > 360) angle -= 360;
        
        return angle;
    }
    
    private void renderDirectionalArrow(double angle, int x, int y) {
        try {
            // Draw a custom arrow using lines
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, 0);
            GlStateManager.rotate((float) angle, 0, 0, 1);
            
            // Arrow styling
            float arrowSize = ARROW_SIZE * 0.7f;
            float arrowHeadWidth = arrowSize * 0.5f;
            
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            
            // Draw arrow body and head
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            
            // Draw arrow body - filled triangle
            GlStateManager.color(0.9F, 0.9F, 0.3F, 1.0F); // Yellow-gold color
            worldrenderer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION);
            worldrenderer.pos(arrowSize, 0, 0).endVertex();
            worldrenderer.pos(-arrowSize * 0.3f, arrowHeadWidth, 0).endVertex();
            worldrenderer.pos(-arrowSize * 0.3f, -arrowHeadWidth, 0).endVertex();
            tessellator.draw();
            
            // Draw outline
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            worldrenderer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
            worldrenderer.pos(arrowSize, 0, 0).endVertex();
            worldrenderer.pos(-arrowSize * 0.3f, arrowHeadWidth, 0).endVertex();
            worldrenderer.pos(-arrowSize * 0.3f, -arrowHeadWidth, 0).endVertex();
            tessellator.draw();
            
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
        } catch (Exception e) {
            System.out.println("Error rendering cursor: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void renderDistance(double horizontalDistance, double verticalDistance, double totalDistance, int x, int y) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        
        // Format the text with horizontal and vertical distances - using +/- instead of arrows
        String verticalPrefix = verticalDistance > 0 ? "+" : "-";
        String verticalText = verticalDistance != 0 ? String.format(" %s%.1f", verticalPrefix, Math.abs(verticalDistance)) : "";
        
        // Create the distance text - horizontal and total
        String horizontalText = String.format("%.1f", horizontalDistance);
        String totalText = String.format(" (%.1f)", totalDistance);
        
        // Draw horizontal distance with shadow in yellow
        fr.drawStringWithShadow(horizontalText, x, y, 0xFFFF55);
        
        // Draw vertical distance indicator with appropriate color (green for up, red for down)
        int verticalColor = verticalDistance >= 0 ? 0x55FF55 : 0xFF5555;
        int verticalX = x + fr.getStringWidth(horizontalText);
        fr.drawStringWithShadow(verticalText, verticalX, y, verticalColor);
        
        // Draw total distance in parentheses with gray color
        int totalX = verticalX + fr.getStringWidth(verticalText);
        fr.drawStringWithShadow(totalText, totalX, y, 0xAAAAAA);
    }
    
    // Get formatted player name exactly as it appears in the TAB menu
    private String getFormattedPlayerName(EntityPlayer player) {
        String formattedName = player.getName();
        try {
            if (Minecraft.getMinecraft().getNetHandler() != null) {
                NetworkPlayerInfo playerInfo = Minecraft.getMinecraft().getNetHandler().getPlayerInfo(player.getUniqueID());
                if (playerInfo != null) {
                    // Try to get the exact formatted display name
                    IChatComponent displayName = playerInfo.getDisplayName();
                    if (displayName != null) {
                        return displayName.getFormattedText();
                    }
                    
                    // Fallback to tab list name with team prefix/suffix
                    if (playerInfo.getPlayerTeam() != null) {
                        formattedName = playerInfo.getPlayerTeam().formatString(player.getName());
                    }
                }
            }
        } catch (Exception e) {
            // Fall back to default name on any error
            System.out.println("Error getting formatted name: " + e.getMessage());
        }
        return formattedName;
    }
    
    private void renderPlayerInfo(EntityPlayer player, double horizontalDistance, double verticalDistance, double totalDistance, double angle) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        
        // New layout:
        // Start with player head on the far right
        int headX = MARGIN;
        int headY = MARGIN;
        
        // Add directional cursor to the right of player head
        int cursorX = headX + HEAD_SIZE + MARGIN;
        int cursorY = headY + HEAD_SIZE/2;
        
        // Add distance text to the right of the cursor
        int distanceX = cursorX + ARROW_SIZE + MARGIN;
        int distanceY = headY + (HEAD_SIZE/2) - (fr.FONT_HEIGHT/2);
        
        // Render in reverse order from right to left
        
        // Draw player head and player info
        renderPlayerHead(player, headX, headY);
        renderPlayerDetails(player, headX, headY);
        
        // Draw directional cursor to the right of player head
        renderDirectionalArrow(angle, cursorX, cursorY);
        
        // Draw distance info to the right of cursor
        renderDistance(horizontalDistance, verticalDistance, totalDistance, distanceX, distanceY);
    }
    
    private void renderPlayerHead(EntityPlayer player, int x, int y) {
        // Get player skin safely
        ResourceLocation skin = DefaultPlayerSkin.getDefaultSkinLegacy();
        try {
            if (Minecraft.getMinecraft().getNetHandler() != null) {
                NetworkPlayerInfo playerInfo = Minecraft.getMinecraft().getNetHandler().getPlayerInfo(player.getUniqueID());
                if (playerInfo != null) {
                    skin = playerInfo.getLocationSkin();
                }
            }
        } catch (Exception e) {
            // Fall back to default skin on any error
        }
        
        // Draw player head
        Minecraft.getMinecraft().getTextureManager().bindTexture(skin);
        
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        // Enable alpha blending
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        
        // Draw face layer (8x8 pixels from 64x64 skin at 8,8)
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x, y + HEAD_SIZE, 0).tex(8.0F/64.0F, 16.0F/64.0F).endVertex();
        worldrenderer.pos(x + HEAD_SIZE, y + HEAD_SIZE, 0).tex(16.0F/64.0F, 16.0F/64.0F).endVertex();
        worldrenderer.pos(x + HEAD_SIZE, y, 0).tex(16.0F/64.0F, 8.0F/64.0F).endVertex();
        worldrenderer.pos(x, y, 0).tex(8.0F/64.0F, 8.0F/64.0F).endVertex();
        tessellator.draw();
        
        // Draw hat layer (8x8 pixels from 64x64 skin at 40,8)
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x, y + HEAD_SIZE, 0).tex(40.0F/64.0F, 16.0F/64.0F).endVertex();
        worldrenderer.pos(x + HEAD_SIZE, y + HEAD_SIZE, 0).tex(48.0F/64.0F, 16.0F/64.0F).endVertex();
        worldrenderer.pos(x + HEAD_SIZE, y, 0).tex(48.0F/64.0F, 8.0F/64.0F).endVertex();
        worldrenderer.pos(x, y, 0).tex(40.0F/64.0F, 8.0F/64.0F).endVertex();
        tessellator.draw();
        
        GlStateManager.disableBlend();
    }
    
    private void renderPlayerDetails(EntityPlayer player, int headX, int headY) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        
        // Draw player name with colored formatting based on health percentage
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float healthPercent = health / maxHealth;
        
        // Calculate health color (green to red gradient)
        int red = (int)(255 * (1 - healthPercent));
        int green = (int)(255 * healthPercent);
        int healthColor = (red << 16) | (green << 8);
        
        String healthText = String.valueOf((int)health);
        int nameX = headX + HEAD_SIZE + 4;
        int nameY = headY + (HEAD_SIZE/2) - (fr.FONT_HEIGHT/2);
        
        // Get the formatted player name as shown in TAB
        String playerName = getFormattedPlayerName(player);
        
        // Draw player name with its formatting
        fr.drawStringWithShadow(playerName, nameX, nameY, 0xFFFFFF);
        
        // Draw health text with health-based color
        String hpText = " [" + healthText + " HP]";
        int hpX = nameX + fr.getStringWidth(playerName);
        fr.drawStringWithShadow(hpText, hpX, nameY, healthColor);
        
        // Draw SHIFT indicator if player is sneaking
        if (player.isSneaking()) {
            // Create a red "SNEAKING" badge
            String shiftText = "SNEAKING";
            int shiftX = nameX;
            int shiftY = nameY + fr.FONT_HEIGHT + 2;
            fr.drawStringWithShadow(EnumChatFormatting.RED + shiftText, shiftX, shiftY, 0xFFFFFF);
        }
    }
} 