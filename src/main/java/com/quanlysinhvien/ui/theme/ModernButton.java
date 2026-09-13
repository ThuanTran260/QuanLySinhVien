package com.quanlysinhvien.ui.theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Pure Swing custom flat button featuring smooth RGB hover animation (100ms),
 * rounded corners, and antialiased rendering.
 */
public class ModernButton extends JButton {
    private Color normalBg;
    private Color hoverBg;
    private Color pressedBg;
    private Color borderColor;
    private int cornerRadius = 8;

    private float hoverProgress = 0.0f;
    private Timer animationTimer;

    public ModernButton() {
        this("", UITheme.PRIMARY);
    }

    public ModernButton(String text) {
        this(text, UITheme.PRIMARY);
    }

    public ModernButton(String text, Color baseBg) {
        this(text, baseBg, lighten(baseBg, 0.15f), Color.WHITE);
    }

    public ModernButton(String text, Color baseBg, Color hoverBg, Color fgColor) {
        super(text);
        this.normalBg = baseBg;
        this.hoverBg = hoverBg;
        this.pressedBg = darken(baseBg, 0.15f);
        this.borderColor = null;

        setForeground(fgColor);
        setFont(UITheme.FONT_BOLD);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(8, 14, 8, 14));

        initHoverAnimation();
    }

    public void setColors(Color normal, Color hover, Color pressed, Color fg) {
        this.normalBg = normal;
        this.hoverBg = hover;
        this.pressedBg = pressed;
        setForeground(fg);
        repaint();
    }

    public void setBorderColor(Color border) {
        this.borderColor = border;
        repaint();
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    private void initHoverAnimation() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    startAnimation(true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                startAnimation(false);
            }
        });
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
                animationTimer = null;
            }
            hoverProgress = 0.0f;
        }
        repaint();
    }

    private synchronized void startAnimation(boolean forward) {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
            animationTimer = null;
        }

        final float step = 16.0f / 100.0f; // 100ms total duration with 16ms per frame (~60 FPS)

        animationTimer = new Timer(16, e -> {
            if (forward) {
                hoverProgress += step;
                if (hoverProgress >= 1.0f) {
                    hoverProgress = 1.0f;
                    ((Timer) e.getSource()).stop();
                    animationTimer = null;
                }
            } else {
                hoverProgress -= step;
                if (hoverProgress <= 0.0f) {
                    hoverProgress = 0.0f;
                    ((Timer) e.getSource()).stop();
                    animationTimer = null;
                }
            }
            repaint();
        });
        animationTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Compute current background color
        Color currentBg;
        if (!isEnabled()) {
            currentBg = new Color(225, 230, 235);
        } else if (getModel().isPressed()) {
            currentBg = pressedBg;
        } else {
            currentBg = interpolate(normalBg, hoverBg, hoverProgress);
        }

        // Fill background
        g2.setColor(currentBg);
        g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);

        // Draw border if defined
        if (borderColor != null && isEnabled()) {
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);
        }

        // Draw icon & text
        FontMetrics fm = g2.getFontMetrics(getFont());
        String text = getText();
        Icon icon = getIcon();

        int iconWidth = (icon != null) ? icon.getIconWidth() + getIconTextGap() : 0;
        int textWidth = (text != null && !text.isEmpty()) ? fm.stringWidth(text) : 0;
        int totalContentWidth = iconWidth + textWidth;

        int startX = (width - totalContentWidth) / 2;
        if (startX < 4) startX = 4;

        if (icon != null) {
            int iconY = (height - icon.getIconHeight()) / 2;
            icon.paintIcon(this, g2, startX, iconY);
            startX += iconWidth;
        }

        if (text != null && !text.isEmpty()) {
            g2.setFont(getFont());
            g2.setColor(isEnabled() ? getForeground() : UITheme.TEXT_MUTED);
            int textY = (height - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, startX, textY);
        }

        g2.dispose();
    }

    private static Color interpolate(Color c1, Color c2, float fraction) {
        float f = Math.max(0.0f, Math.min(1.0f, fraction));
        int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * f);
        int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * f);
        int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * f);
        return new Color(
                Math.min(255, Math.max(0, r)),
                Math.min(255, Math.max(0, g)),
                Math.min(255, Math.max(0, b))
        );
    }

    private static Color lighten(Color c, float amount) {
        int r = Math.min(255, (int) (c.getRed() + (255 - c.getRed()) * amount));
        int g = Math.min(255, (int) (c.getGreen() + (255 - c.getGreen()) * amount));
        int b = Math.min(255, (int) (c.getBlue() + (255 - c.getBlue()) * amount));
        return new Color(r, g, b);
    }

    private static Color darken(Color c, float amount) {
        int r = Math.max(0, (int) (c.getRed() * (1.0f - amount)));
        int g = Math.max(0, (int) (c.getGreen() * (1.0f - amount)));
        int b = Math.max(0, (int) (c.getBlue() * (1.0f - amount)));
        return new Color(r, g, b);
    }
}
