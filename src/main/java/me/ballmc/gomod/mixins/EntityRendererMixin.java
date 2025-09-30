package me.ballmc.gomod.mixins;

import me.ballmc.gomod.features.PerspectiveDistance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    private int gomod$prevThirdPersonView = 0;
    private float gomod$currentDistance = 4.0f;
    private float gomod$targetDistance = 4.0f;
    private float gomod$originalFOV = 70.0f; // Store original FOV

    @Inject(method = "updateCameraAndRender", at = @At("HEAD"))
    private void gomod$applyPerspectiveDistance(float partialTicks, long nanoTime, CallbackInfo ci) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) return;
            int currentView = mc.gameSettings.thirdPersonView;

            // If we just entered third person, immediately set distance to config value to prevent glitch
            if (gomod$prevThirdPersonView == 0 && currentView != 0) {
                try {
                    float configDistance = me.ballmc.gomod.features.PerspectiveDistance.getDistance();
                    gomod$currentDistance = configDistance;
                    gomod$targetDistance = configDistance;
                    // Store original FOV when entering perspective mode
                    gomod$originalFOV = mc.gameSettings.fovSetting;
                } catch (Throwable ignored) {}
            }
            
            // If we just exited third person, reset saved distance to default 4.0 for next time
            if (gomod$prevThirdPersonView != 0 && currentView == 0 && me.ballmc.gomod.features.PerspectiveDistance.isResetOnExit()) {
                try {
                    me.ballmc.gomod.features.PerspectiveDistance.setDistance(4.0f);
                    gomod$currentDistance = 4.0f;
                    gomod$targetDistance = 4.0f;
                } catch (Throwable ignored) {}
            }
            
            // If we just exited third person, restore original FOV
            if (gomod$prevThirdPersonView != 0 && currentView == 0) {
                try {
                    mc.gameSettings.fovSetting = gomod$originalFOV;
                } catch (Throwable ignored) {}
            }
            
            gomod$prevThirdPersonView = currentView;

            if (currentView == 0) return; // only apply in third person

            if (!me.ballmc.gomod.features.PerspectiveDistance.isEnabled()) {
                return;
            }

            // Check if zoom key is held (this locks hotbar scroll and enables zooming)
            try {
                int zoomKey = me.ballmc.gomod.features.PerspectiveDistance.getZoomKeyCode();
                boolean zoomKeyHeld = zoomKey > 0 && org.lwjgl.input.Keyboard.isKeyDown(zoomKey);
                boolean requireHold = me.ballmc.gomod.features.PerspectiveDistance.isRequireHold();
                
                // If a GUI is open, don't handle wheel zoom at all
                if (mc.currentScreen != null) {
                    return;
                }

                // If require hold is enabled, only zoom when zoom key is held
                // If require hold is disabled, always allow zooming when in third-person
                if (!requireHold || zoomKeyHeld) {
                    // Consume all wheel movement to prevent hotbar scrolling this tick
                    int dWheel = 0;
                    int wheel;
                    while ((wheel = Mouse.getDWheel()) != 0) {
                        dWheel += wheel;
                    }
                    if (dWheel != 0) {
                        int notches = (int) Math.signum(dWheel) * Math.max(1, Math.abs(dWheel) / 120);
                        float step = me.ballmc.gomod.features.PerspectiveDistance.getScrollStep();
                        float current = PerspectiveDistance.getDistance();
                        float updated = current - (notches * step);
                        PerspectiveDistance.setDistance(updated);
                        gomod$targetDistance = updated; // Update target for smooth interpolation
                    }
                }
            } catch (Throwable ignored) {
                // ignore missing key binding or mouse input issues
            }

            // Handle reset key: if pressed while in third-person, reset to 4.0 immediately
            try {
                int resetKey = me.ballmc.gomod.features.PerspectiveDistance.getResetKeyCode();
                if (resetKey > 0 && org.lwjgl.input.Keyboard.isKeyDown(resetKey)) {
                    // Don't trigger reset if player is typing in a text field or chat
                    if (mc.currentScreen != null) {
                        // Check for common text input screens
                        if (mc.currentScreen instanceof net.minecraft.client.gui.GuiChat ||
                            mc.currentScreen instanceof net.minecraft.client.gui.GuiCommandBlock ||
                            mc.currentScreen instanceof net.minecraft.client.gui.GuiRepair) {
                            // Player is typing in a text field, don't trigger reset
                            return;
                        }
                        
                        // Check if any text field is currently focused in the current screen
                        // REFLECTION SAFETY: See REFLECTION_SAFETY_RULES.md for guidelines
                        try {
                            java.lang.reflect.Field focusedField = mc.currentScreen.getClass().getDeclaredField("field_146209_f"); // focused field
                            if (focusedField != null) {
                                focusedField.setAccessible(true);
                                Object focused = focusedField.get(mc.currentScreen);
                                if (focused != null) {
                                    // A text field is focused, don't trigger reset
                                    return;
                                }
                            }
                        } catch (Exception e) {
                            if (e.getClass().getSimpleName().equals("InvocationTargetException")) {
                                // Get the actual cause of the InvocationTargetException
                                Throwable cause = ((java.lang.reflect.InvocationTargetException) e).getCause();
                                System.err.println("[gomod] Reflection InvocationTargetException checking text field focus:");
                                System.err.println("  - Target exception: " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
                                System.err.println("  - Stack trace:");
                                cause.printStackTrace();
                            } else if (e.getClass().getSimpleName().equals("NoSuchFieldException")) {
                                System.err.println("[gomod] Field 'field_146209_f' not found in " + mc.currentScreen.getClass().getSimpleName() + ": " + e.getMessage());
                            } else {
                                System.err.println("[gomod] Unexpected error checking text field focus: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                                e.printStackTrace();
                            }
                            // Continue with normal behavior for any reflection failure
                        } catch (Throwable e) {
                            System.err.println("[gomod] Unexpected error checking text field focus: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                            e.printStackTrace();
                            // Continue with normal behavior for any reflection failure
                        }
                    }
                    me.ballmc.gomod.features.PerspectiveDistance.setDistance(4.0f);
                    gomod$targetDistance = 4.0f; // Update target for smooth interpolation
                    gomod$currentDistance = 4.0f; // Immediately set current distance to prevent glitch
                }
            } catch (Throwable ignored) {}

            // Update target distance from config if it changed
            gomod$targetDistance = PerspectiveDistance.getDistance();
            
            // Smooth interpolation to target distance (like Lunar's freelook)
            if (me.ballmc.gomod.features.PerspectiveDistance.isSmoothZoom()) {
                float smoothSpeed = me.ballmc.gomod.features.PerspectiveDistance.getSmoothSpeed();
                float diff = gomod$targetDistance - gomod$currentDistance;
                if (Math.abs(diff) > 0.001f) {
                    gomod$currentDistance += diff * smoothSpeed;
                } else {
                    gomod$currentDistance = gomod$targetDistance;
                }
            } else {
                gomod$currentDistance = gomod$targetDistance;
            }
            
            // EntityRenderer uses thirdPersonDistance and thirdPersonDistancePrev fields (private). 
            // We reproduce behavior via accessor through the public GameSettings distance property if present.
            // On LC 1.8.9, EntityRenderer reads from a field; however Mixin into the class allows us to set it via obfuscated field names.
            // We fallback by using reflection to set distance fields.
            // REFLECTION SAFETY: See REFLECTION_SAFETY_RULES.md for guidelines
            float dist = gomod$currentDistance;
            try {
                java.lang.reflect.Field f = EntityRenderer.class.getDeclaredField("thirdPersonDistance");
                f.setAccessible(true);
                f.setFloat(this, dist);
                java.lang.reflect.Field f2 = EntityRenderer.class.getDeclaredField("thirdPersonDistancePrev");
                f2.setAccessible(true);
                f2.setFloat(this, dist);
            } catch (Exception e) {
                if (e.getClass().getSimpleName().equals("InvocationTargetException")) {
                    // Get the actual cause of the InvocationTargetException
                    Throwable cause = ((java.lang.reflect.InvocationTargetException) e).getCause();
                    System.err.println("[gomod] Reflection InvocationTargetException setting distance fields:");
                    System.err.println("  - Target exception: " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
                    System.err.println("  - Stack trace:");
                    cause.printStackTrace();
                } else if (e.getClass().getSimpleName().equals("NoSuchFieldException")) {
                    System.err.println("[gomod] Field not found setting distance fields: " + e.getMessage());
                } else {
                    System.err.println("[gomod] Unexpected error setting distance fields: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
                // best-effort; ignore if fields not found
            } catch (Throwable e) {
                System.err.println("[gomod] Unexpected error setting distance fields: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
                // best-effort; ignore if fields not found
            }
            
            // Apply perspective FOV setting (only when in third-person view)
            try {
                float perspectiveFOV = me.ballmc.gomod.features.PerspectiveDistance.getFOV();
                mc.gameSettings.fovSetting = perspectiveFOV;
            } catch (Throwable ignored) {
                // ignore FOV setting issues
            }
        } catch (Exception e) {
            System.err.println("[gomod] EntityRendererMixin error: " + e.getMessage());
        }
    }
}

 