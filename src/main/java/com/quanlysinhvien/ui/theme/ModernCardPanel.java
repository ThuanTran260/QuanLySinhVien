package com.quanlysinhvien.ui.theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Modern Card container panel with rounded corners, subtle 1px border,
 * and antialiased graphics. Avoids smearing/ghosting artifacts via setOpaque(false).
 */
public class ModernCardPanel extends JPanel {
    private int cornerRadius = 10;
    private Color cardBg = UITheme.CARD_BG;
    private Color borderColor = UITheme.BORDER_COLOR;

    public ModernCardPanel() {
        this(new BorderLayout(), 16);
    }

    public ModernCardPanel(int padding) {
        this(new BorderLayout(), padding);
    }

    public ModernCardPanel(LayoutManager layout) {
        this(layout, 16);
    }

    public ModernCardPanel(LayoutManager layout, int padding) {
        super(layout);
        setOpaque(false);
        setBorder(new EmptyBorder(padding, padding, padding, padding));
    }

    public void setCardBackground(Color bg) {
        this.cardBg = bg;
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Fill background rounded card
        g2.setColor(cardBg);
        g2.fillRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);

        // Draw subtle border
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);
        }

        g2.dispose();
    }
}
