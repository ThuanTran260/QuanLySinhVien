package com.quanlysinhvien.ui.animation;

/**
 * Standard easing functions for smooth pure-Swing animations.
 */
public final class Easing {
    private Easing() {}

    /**
     * Cubic ease-out: starts fast and decelerates smoothly.
     * Formula: 1 - (1 - t)^3
     */
    public static float easeOutCubic(float t) {
        float clamped = clamp(t);
        return 1.0f - (float) Math.pow(1.0f - clamped, 3);
    }

    /**
     * Quadratic ease-in-out: smooth acceleration and deceleration.
     */
    public static float easeInOutQuad(float t) {
        float clamped = clamp(t);
        return clamped < 0.5f
                ? 2.0f * clamped * clamped
                : 1.0f - (float) Math.pow(-2.0f * clamped + 2.0f, 2) / 2.0f;
    }

    /**
     * Clamps a value into [0.0, 1.0].
     */
    public static float clamp(float t) {
        return Math.max(0.0f, Math.min(1.0f, t));
    }
}
