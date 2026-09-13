package com.quanlysinhvien.ui.theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Custom table cell renderer supporting:
 * - 32px row height and cell padding
 * - Soft zebra striping (#FFFFFF & #F8FAFC)
 * - Colored badge pill rendering for the "Xếp Loại" column
 * - Numeric formatting for DiemTB (Float/Double)
 */
public class ModernTableRenderer extends DefaultTableCellRenderer {
    private static final DecimalFormat SCORE_FORMAT = new DecimalFormat("#0.0#",
            DecimalFormatSymbols.getInstance(Locale.US));
    private static final EmptyBorder CELL_BORDER = new EmptyBorder(4, 10, 4, 10);

    // Precomputed pill metrics cache for the 5 rank values
    private static final java.util.Map<String, Integer> PILL_WIDTH_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile FontMetrics cachedBadgeFontMetrics = null;

    static {
        // Pre-seed the 5 standard rank widths (text width + 18 padding)
        PILL_WIDTH_CACHE.put("Xuất sắc", 74);
        PILL_WIDTH_CACHE.put("Giỏi", 52);
        PILL_WIDTH_CACHE.put("Khá", 52);
        PILL_WIDTH_CACHE.put("Trung bình", 88);
        PILL_WIDTH_CACHE.put("Yếu", 50);
    }

    private boolean isBadgeColumn = false;
    private Color badgeBg = null;
    private Color badgeText = null;

    public ModernTableRenderer() {
        super();
        setBorder(CELL_BORDER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // Reset badge flag
        isBadgeColumn = false;
        badgeBg = null;
        badgeText = null;

        // Font
        setFont(UITheme.FONT_REGULAR);

        // Ensure horizontal padding is consistently applied using cached Border
        setBorder(CELL_BORDER);

        // Text formatting
        if (value instanceof Number) {
            if (value instanceof Float || value instanceof Double) {
                synchronized (SCORE_FORMAT) {
                    setText(SCORE_FORMAT.format(((Number) value).doubleValue()));
                }
            } else {
                setText(value.toString());
            }
        }

        // Zebra striping background
        if (!isSelected) {
            if (row % 2 == 0) {
                setBackground(UITheme.TABLE_ROW_EVEN);
            } else {
                setBackground(UITheme.TABLE_ROW_ODD);
            }
            setForeground(UITheme.TEXT_PRIMARY);
        } else {
            setBackground(UITheme.TABLE_SELECTION_BG);
            setForeground(UITheme.TABLE_SELECTION_TEXT);
        }

        // Alignment
        int modelColumn = table.convertColumnIndexToModel(column);
        if (modelColumn == 2) {
            // Họ và Tên: căn trái
            setHorizontalAlignment(SwingConstants.LEFT);
        } else {
            // STT, Mã SV, Lớp, Ngày Sinh, Điểm TB, Xếp Loại: căn giữa
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        // Special Badge Rendering for column 6 ("Xếp Loại")
        if (modelColumn == 6 && value != null) {
            String rank = value.toString().trim();
            isBadgeColumn = true;
            setFont(UITheme.FONT_BADGE);

            if (rank.equalsIgnoreCase("Xuất sắc")) {
                badgeBg = UITheme.BADGE_XUAT_SAC_BG;
                badgeText = UITheme.BADGE_XUAT_SAC_TEXT;
            } else if (rank.equalsIgnoreCase("Giỏi")) {
                badgeBg = UITheme.BADGE_GIOI_BG;
                badgeText = UITheme.BADGE_GIOI_TEXT;
            } else if (rank.equalsIgnoreCase("Khá")) {
                badgeBg = UITheme.BADGE_KHA_BG;
                badgeText = UITheme.BADGE_KHA_TEXT;
            } else if (rank.equalsIgnoreCase("Trung bình")) {
                badgeBg = UITheme.BADGE_TRUNG_BINH_BG;
                badgeText = UITheme.BADGE_TRUNG_BINH_TEXT;
            } else {
                // Yếu hoặc khác
                badgeBg = UITheme.BADGE_YEU_BG;
                badgeText = UITheme.BADGE_YEU_TEXT;
            }
            setForeground(badgeText);
        }

        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (isBadgeColumn && badgeBg != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Paint cell background first
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (cachedBadgeFontMetrics == null) {
                cachedBadgeFontMetrics = g2.getFontMetrics(getFont());
            }
            FontMetrics fm = cachedBadgeFontMetrics;

            // Compute pill dimensions with cached width
            String text = getText();
            String safeText = (text != null) ? text : "";
            Integer cachedW = PILL_WIDTH_CACHE.get(safeText);
            int textWidth;
            int pillWidth;
            if (cachedW != null) {
                pillWidth = cachedW;
                textWidth = pillWidth - 18;
            } else {
                textWidth = fm.stringWidth(safeText);
                pillWidth = textWidth + 18;
                PILL_WIDTH_CACHE.put(safeText, pillWidth);
            }
            int pillHeight = 22;
            int pillX = (getWidth() - pillWidth) / 2;
            int pillY = (getHeight() - pillHeight) / 2;

            // Draw pill background
            g2.setColor(badgeBg);
            g2.fillRoundRect(pillX, pillY, pillWidth, pillHeight, 12, 12);

            // Draw text
            g2.setColor(badgeText);
            g2.setFont(getFont());
            int textX = pillX + (pillWidth - textWidth) / 2;
            int textY = pillY + (pillHeight - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(safeText, textX, textY);

            g2.dispose();
        } else {
            super.paintComponent(g);
        }
    }

    /**
     * Renderer nhẹ cho 6 cột thông thường (cột 0-5), không override paintComponent.
     */
    public static class LightweightRenderer extends DefaultTableCellRenderer {
        public LightweightRenderer() {
            super();
            setBorder(CELL_BORDER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(UITheme.FONT_REGULAR);
            setBorder(CELL_BORDER);

            if (value instanceof Number) {
                if (value instanceof Float || value instanceof Double) {
                    synchronized (SCORE_FORMAT) {
                        setText(SCORE_FORMAT.format(((Number) value).doubleValue()));
                    }
                } else {
                    setText(value.toString());
                }
            }

            if (!isSelected) {
                setBackground((row % 2 == 0) ? UITheme.TABLE_ROW_EVEN : UITheme.TABLE_ROW_ODD);
                setForeground(UITheme.TEXT_PRIMARY);
            } else {
                setBackground(UITheme.TABLE_SELECTION_BG);
                setForeground(UITheme.TABLE_SELECTION_TEXT);
            }

            int modelColumn = table.convertColumnIndexToModel(column);
            if (modelColumn == 2) {
                setHorizontalAlignment(SwingConstants.LEFT);
            } else {
                setHorizontalAlignment(SwingConstants.CENTER);
            }

            return this;
        }
    }

    /**
     * Applies full modern Stripe/Airtable styling to a JTable:
     * - 6 regular columns use LightweightRenderer
     * - Column 6 ("Xếp Loại") uses ModernTableRenderer with custom pill badge
     */
    public static void applyModernStyle(JTable table) {
        table.setRowHeight(32);
        table.setFont(UITheme.FONT_REGULAR);
        table.setSelectionBackground(UITheme.TABLE_SELECTION_BG);
        table.setSelectionForeground(UITheme.TABLE_SELECTION_TEXT);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(241, 245, 249));

        // Style header
        JTableHeader header = table.getTableHeader();
        header.setFont(UITheme.FONT_BOLD);
        header.setBackground(UITheme.TABLE_HEADER_BG);
        header.setForeground(UITheme.TABLE_HEADER_TEXT);
        header.setPreferredSize(new Dimension(header.getWidth(), 36));
        header.setReorderingAllowed(false);

        // Header renderer
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                setBackground(UITheme.TABLE_HEADER_BG);
                setForeground(UITheme.TABLE_HEADER_TEXT);
                setFont(UITheme.FONT_BOLD);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR),
                        new EmptyBorder(6, 8, 6, 8)
                ));
                return this;
            }
        };

        LightweightRenderer lightweightRenderer = new LightweightRenderer();
        ModernTableRenderer badgeRenderer = new ModernTableRenderer();

        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
            if (i == 6) {
                table.getColumnModel().getColumn(i).setCellRenderer(badgeRenderer);
            } else {
                table.getColumnModel().getColumn(i).setCellRenderer(lightweightRenderer);
            }
        }
    }
}
