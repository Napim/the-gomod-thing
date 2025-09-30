package me.ballmc.gomod.features;

public final class PerspectiveDistance {
    private PerspectiveDistance() {}

    public static void loadSetting() {
        // access triggers config load; nothing else needed
        ConfigManager.getPerspectiveDistance();
    }

    public static float getDistance() {
        return ConfigManager.getPerspectiveDistance();
    }

    public static void setDistance(float distance) {
        // clamp to sensible range (0.5 - 100 blocks)
        float clamped = Math.max(0.5f, Math.min(100.0f, distance));
        ConfigManager.setPerspectiveDistance(clamped);
    }

    public static boolean isEnabled() {
        return ConfigManager.isPerspectiveEnabled();
    }

    public static void setEnabled(boolean enabled) {
        ConfigManager.setPerspectiveEnabled(enabled);
    }

    public static float getScrollStep() {
        return Math.max(0.01f, Math.min(10.0f, ConfigManager.getPerspectiveScrollStep()));
    }

    public static void setScrollStep(float step) {
        float clamped = Math.max(0.01f, Math.min(10.0f, step));
        ConfigManager.setPerspectiveScrollStep(clamped);
    }

    public static boolean isResetOnExit() {
        return ConfigManager.isPerspectiveResetOnExit();
    }

    public static void setResetOnExit(boolean value) {
        ConfigManager.setPerspectiveResetOnExit(value);
    }

    public static int getResetKeyCode() {
        return ConfigManager.getPerspectiveResetKeyCode();
    }

    public static void setResetKeyCode(int keyCode) {
        ConfigManager.setPerspectiveResetKeyCode(keyCode);
    }

    public static boolean isRequireHold() {
        return ConfigManager.isPerspectiveRequireHold();
    }

    public static void setRequireHold(boolean value) {
        ConfigManager.setPerspectiveRequireHold(value);
    }

    public static boolean isSmoothZoom() {
        return ConfigManager.isPerspectiveSmoothZoom();
    }

    public static void setSmoothZoom(boolean value) {
        ConfigManager.setPerspectiveSmoothZoom(value);
    }

    public static float getSmoothSpeed() {
        // Invert the scale: 1.0 means slow/smooth, 0.01 means fast/snappy
        // So we convert slider value (0.01-1.0) to interpolation speed (1.0-0.01)
        float sliderValue = ConfigManager.getPerspectiveSmoothSpeed();
        return 1.0f - sliderValue + 0.01f;
    }

    public static void setSmoothSpeed(float speed) {
        ConfigManager.setPerspectiveSmoothSpeed(speed);
    }

    public static int getZoomKeyCode() {
        return ConfigManager.getPerspectiveZoomKeyCode();
    }

    public static void setZoomKeyCode(int keyCode) {
        ConfigManager.setPerspectiveZoomKeyCode(keyCode);
    }

    public static float getFOV() {
        return ConfigManager.getPerspectiveFOV();
    }

    public static void setFOV(float fov) {
        ConfigManager.setPerspectiveFOV(fov);
    }

    public static boolean isLockScroll() {
        return ConfigManager.isPerspectiveLockScroll();
    }

    public static void setLockScroll(boolean value) {
        ConfigManager.setPerspectiveLockScroll(value);
    }
}


