package com.quanlysinhvien.ui.theme;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Design tokens and styling helpers inspired by Stripe Clean Canvas
 * and Airtable data badge palettes.
 */
public final class UITheme {
    private UITheme() {}

    // Canvas & Surface Colors (Stripe-inspired)
    public static final Color CANVAS_BG = new Color(0xF6, 0xF9, 0xFC);
    public static final Color CARD_BG = Color.WHITE;
    public static final Color BORDER_COLOR = new Color(0xE3, 0xE8, 0xEE);
    public static final Color BORDER_FOCUS = new Color(0x63, 0x5B, 0xFF);
    public static final Color BG_MUTED = new Color(0xFA, 0xFB, 0xFC);

    // Primary Accents (Electric Indigo)
    public static final Color PRIMARY = new Color(0x63, 0x5B, 0xFF);
    public static final Color PRIMARY_HOVER = new Color(0x7A, 0x73, 0xFF);
    public static final Color PRIMARY_PRESSED = new Color(0x4E, 0x44, 0xE6);

    // Semantic Accents
    public static final Color SUCCESS = new Color(0x10, 0xB9, 0x81);
    public static final Color SUCCESS_HOVER = new Color(0x05, 0x96, 0x69);
    public static final Color SUCCESS_PRESSED = new Color(0x04, 0x78, 0x57);

    public static final Color DANGER = new Color(0xEF, 0x44, 0x44);
    public static final Color DANGER_HOVER = new Color(0xDC, 0x26, 0x26);
    public static final Color DANGER_PRESSED = new Color(0xB9, 0x1C, 0x1C);

    public static final Color WARNING = new Color(0xF5, 0x9E, 0x0B);

    // Neutral / Secondary Button
    public static final Color NEUTRAL_BTN_BG = new Color(0xF8, 0xFA, 0xFC);
    public static final Color NEUTRAL_BTN_HOVER = new Color(0xEE, 0xF2, 0xF6);
    public static final Color NEUTRAL_BTN_PRESSED = new Color(0xE2, 0xE8, 0xF0);
    public static final Color NEUTRAL_BTN_TEXT = new Color(0x33, 0x41, 0x55);
    public static final Color NEUTRAL_BTN_BORDER = new Color(0xCB, 0xD5, 0xE1);

    // Typography Colors
    public static final Color TEXT_PRIMARY = new Color(0x0D, 0x25, 0x3D);
    public static final Color TEXT_SECONDARY = new Color(0x42, 0x54, 0x66);
    public static final Color TEXT_MUTED = new Color(0x88, 0x98, 0xAA);

    // Table Colors
    public static final Color TABLE_HEADER_BG = new Color(0xF8, 0xFA, 0xFC);
    public static final Color TABLE_HEADER_TEXT = new Color(0x42, 0x54, 0x66);
    public static final Color TABLE_ROW_EVEN = Color.WHITE;
    public static final Color TABLE_ROW_ODD = new Color(0xF8, 0xFA, 0xFC);
    public static final Color TABLE_SELECTION_BG = new Color(0xEE, 0xF2, 0xFF);
    public static final Color TABLE_SELECTION_TEXT = new Color(0x1E, 0x1B, 0x4B);

    // Status Badge Palette (Airtable-inspired)
    public static final Color BADGE_XUAT_SAC_BG = new Color(0xDC, 0xFC, 0xE7);
    public static final Color BADGE_XUAT_SAC_TEXT = new Color(0x15, 0x80, 0x3D);

    public static final Color BADGE_GIOI_BG = new Color(0xDB, 0xEA, 0xFE);
    public static final Color BADGE_GIOI_TEXT = new Color(0x1D, 0x4E, 0xD8);

    public static final Color BADGE_KHA_BG = new Color(0xFE, 0xF3, 0xC7);
    public static final Color BADGE_KHA_TEXT = new Color(0xB4, 0x53, 0x09);

    public static final Color BADGE_TRUNG_BINH_BG = new Color(0xF3, 0xF4, 0xF6);
    public static final Color BADGE_TRUNG_BINH_TEXT = new Color(0x4B, 0x55, 0x63);

    public static final Color BADGE_YEU_BG = new Color(0xFE, 0xE2, 0xE2);
    public static final Color BADGE_YEU_TEXT = new Color(0xB9, 0x1C, 0x1C);

    // Typography
    public static final Font FONT_TITLE_LARGE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_TITLE_MEDIUM = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_SECTION_HEADER = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_BADGE = new Font("Segoe UI", Font.BOLD, 11);
    public static final Font FONT_DISPLAY = new Font("Segoe UI", Font.BOLD, 22);

    /**
     * Styles a text field with clean borders, padding, and focus highlight.
     */
    public static void styleTextField(JTextField tf) {
        tf.setFont(FONT_REGULAR);
        tf.setBackground(CARD_BG);
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(PRIMARY);

        Border normalBorder = new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        );
        Border focusedBorder = new CompoundBorder(
                new LineBorder(BORDER_FOCUS, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        );

        tf.setBorder(normalBorder);
        tf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (tf.isEditable()) {
                    tf.setBorder(focusedBorder);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                tf.setBorder(normalBorder);
            }
        });
    }

    /**
     * Styles a combo box with clean border and font.
     */
    public static void styleComboBox(JComboBox<?> cb) {
        cb.setFont(FONT_REGULAR);
        cb.setBackground(CARD_BG);
        cb.setForeground(TEXT_PRIMARY);
        cb.setBorder(new LineBorder(BORDER_COLOR, 1, true));
    }
}
