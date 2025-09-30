package me.ballmc.gomod.features;

import net.minecraft.client.Minecraft;
import net.weavemc.loader.api.event.RenderGameOverlayEvent;
import net.weavemc.loader.api.event.SubscribeEvent;
import org.lwjgl.input.Keyboard;

/**
 * WeaveLoader version: keeps the selected hotbar slot fixed while perspective is active.
 * Runs every frame via RenderGameOverlayEvent; no Forge dependencies.
 */
public class HotbarScrollBlocker {
    private int lockedSlot = -1; // -1 means not locked

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent e) {
        Minecraft mc = safeMc();
        if (mc == null || mc.thePlayer == null) {
            lockedSlot = -1;
            return;
        }

        if (isPerspectiveActive() && PerspectiveDistance.isLockScroll()) {
            if (lockedSlot == -1) lockedSlot = mc.thePlayer.inventory.currentItem;

            // Allow switching via direct hotbar number keys (when no GUI is open)
            if (mc.currentScreen == null) {
                int numSlot = getPressedNumberHotbarSlot();
                if (numSlot != -1) {
                    lockedSlot = numSlot;
                }
            }

            // Enforce the (possibly updated) locked slot to block wheel changes
            if (mc.thePlayer.inventory.currentItem != lockedSlot) {
                mc.thePlayer.inventory.currentItem = lockedSlot;
            }
        } else {
            lockedSlot = -1;
        }
    }

    private boolean isPerspectiveActive() {
        Minecraft mc = safeMc();
        if (mc == null || mc.gameSettings == null) return false;
        if (!PerspectiveDistance.isEnabled()) return false;
        // Only lock when actually in third-person (vanilla value != 0)
        if (mc.gameSettings.thirdPersonView == 0) return false;
        // If require-hold is enabled, verify the zoom key is held
        if (PerspectiveDistance.isRequireHold() && !Keyboard.isKeyDown(PerspectiveDistance.getZoomKeyCode())) {
            return false;
        }
        return true;
    }

    private Minecraft safeMc() {
        try {
            return Minecraft.getMinecraft();
        } catch (Throwable t) {
            return null;
        }
    }

    private int getPressedNumberHotbarSlot() {
        // Returns 0..8 if key 1..9 is currently held, else -1
        if (Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_1)) return 0;
        if (Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_2)) return 1;
        if (Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_3)) return 2;
        if (Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_4)) return 3;
        if (Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_5)) return 4;
        if (Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_6)) return 5;
        if (Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_7)) return 6;
        if (Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_8)) return 7;
        if (Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_9)) return 8;
        return -1;
    }
}


