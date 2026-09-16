package com.quanlysinhvien.ui.components;

import com.quanlysinhvien.ui.theme.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * Bộ Icon Vector thuần Java 2D dành cho ModernSidebar và Header:
 * - 100% Vector Rendering (Path2D, RoundRectangle2D, Line2D, Ellipse2D)
 * - Khử răng cưa Antialiasing độ phân giải cao, hỗ trợ đa tỷ lệ DPI (100% - 200%)
 * - Tự động thích ứng màu: Active (#635BFF), Hover (#1E293B), Inactive (#64748B), Disabled (#94A3B8)
 * - Tuyệt đối KHÔNG phụ thuộc font emoji hệ điều hành, triệt tiêu hoàn toàn lỗi ô vuông [▯]
 */
public final class SidebarIcons {

    private SidebarIcons() {}

    public enum IconType {
        STUDENT,
        CALCULATOR,
        CHART,
        ABOUT,
        TOGGLE_COLLAPSE,
        TOGGLE_EXPAND,
        LOGO
    }

    public static Icon getNavIcon(ModernSidebar.MenuId menuId, int size) {
        if (menuId == null) return createStudentIcon(size);
        switch (menuId) {
            case SINH_VIEN: return createStudentIcon(size);
            case MAY_TINH:  return createCalculatorIcon(size);
            case THONG_KE:  return createChartIcon(size);
            case GIOI_THIEU: return createAboutIcon(size);
            default:        return createStudentIcon(size);
        }
    }

    public static Icon createStudentIcon() {
        return createStudentIcon(20);
    }

    public static Icon createStudentIcon(int size) {
        return new VectorIcon(IconType.STUDENT, size, null);
    }

    public static Icon createCalculatorIcon() {
        return createCalculatorIcon(20);
    }

    public static Icon createCalculatorIcon(int size) {
        return new VectorIcon(IconType.CALCULATOR, size, null);
    }

    public static Icon createChartIcon() {
        return createChartIcon(20);
    }

    public static Icon createChartIcon(int size) {
        return new VectorIcon(IconType.CHART, size, null);
    }

    public static Icon createAboutIcon() {
        return createAboutIcon(20);
    }

    public static Icon createAboutIcon(int size) {
        return new VectorIcon(IconType.ABOUT, size, null);
    }

    public static Icon createToggleIcon(boolean expanded) {
        return createToggleIcon(16, expanded);
    }

    public static Icon createToggleIcon(int size, boolean expanded) {
        return new VectorIcon(expanded ? IconType.TOGGLE_COLLAPSE : IconType.TOGGLE_EXPAND, size, null);
    }

    public static Icon createLogoIcon() {
        return createLogoIcon(26);
    }

    public static Icon createLogoIcon(int size) {
        return new VectorIcon(IconType.LOGO, size, UITheme.PRIMARY);
    }

    /**
     * Lớp VectorIcon vẽ trực tiếp bằng Graphics2D (Java 2D),
     * tự động thích ứng với trạng thái Active / Hover / Inactive của nút.
     */
    public static class VectorIcon implements Icon {
        private final IconType type;
        private final int size;
        private final Color fixedColor;

        public VectorIcon(IconType type, int size, Color fixedColor) {
            this.type = type;
            this.size = size;
            this.fixedColor = fixedColor;
        }

        public IconType getType() {
            return type;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                Color fg = resolveColor(c);
                Color tint = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 36); // Phủ mờ duotone 14%

                g2.translate(x, y);
                if (type == IconType.LOGO) {
                    if (size != 26) {
                        double scale = (double) size / 26.0;
                        g2.scale(scale, scale);
                    }
                } else if (size != 20) {
                    double scale = (double) size / 20.0;
                    g2.scale(scale, scale);
                }

                switch (type) {
                    case STUDENT:
                        paintStudent(g2, fg, tint);
                        break;
                    case CALCULATOR:
                        paintCalculator(g2, fg, tint);
                        break;
                    case CHART:
                        paintChart(g2, fg, tint);
                        break;
                    case ABOUT:
                        paintAbout(g2, fg, tint);
                        break;
                    case TOGGLE_COLLAPSE:
                        paintToggle(g2, fg, true);
                        break;
                    case TOGGLE_EXPAND:
                        paintToggle(g2, fg, false);
                        break;
                    case LOGO:
                        paintLogo(g2, fg);
                        break;
                }
            } finally {
                g2.dispose();
            }
        }

        private Color resolveColor(Component c) {
            if (fixedColor != null) {
                return fixedColor;
            }
            if (c == null) {
                return UITheme.TEXT_SECONDARY;
            }
            if (!c.isEnabled()) {
                return UITheme.TEXT_MUTED;
            }
            if (c instanceof ModernSidebar.NavButton) {
                ModernSidebar.NavButton nav = (ModernSidebar.NavButton) c;
                if (nav.isActive()) {
                    return UITheme.PRIMARY;
                }
                if (nav.getModel().isRollover()) {
                    return UITheme.TEXT_PRIMARY;
                }
                return UITheme.TEXT_SECONDARY;
            }
            if (c instanceof AbstractButton) {
                AbstractButton btn = (AbstractButton) c;
                if (btn.getModel().isRollover()) {
                    return UITheme.PRIMARY;
                }
                Color btnFg = btn.getForeground();
                return btnFg != null ? btnFg : UITheme.TEXT_SECONDARY;
            }
            Color fg = c.getForeground();
            return fg != null ? fg : UITheme.TEXT_SECONDARY;
        }

        private void paintStudent(Graphics2D g2, Color fg, Color tint) {
            // 1. Mũ cử nhân (Mortarboard): Top diamond
            Path2D.Double diamond = new Path2D.Double();
            diamond.moveTo(10.0, 3.8);
            diamond.lineTo(18.5, 7.6);
            diamond.lineTo(10.0, 11.4);
            diamond.lineTo(1.5, 7.6);
            diamond.closePath();

            // 2. Vành mũ đội đầu (Skull cap)
            Path2D.Double skull = new Path2D.Double();
            skull.moveTo(4.6, 9.6);
            skull.lineTo(4.6, 13.0);
            skull.quadTo(10.0, 16.2, 15.4, 13.0);
            skull.lineTo(15.4, 9.6);

            // Nền mờ duotone
            g2.setColor(tint);
            g2.fill(diamond);
            g2.fill(skull);

            // Viền sắc nét
            g2.setColor(fg);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(diamond);
            g2.draw(skull);

            // Nút đỉnh mũ (Center apex button)
            g2.fill(new Ellipse2D.Double(9.1, 6.7, 1.8, 1.8));

            // Dây tua mũ (Tassel) thả từ đỉnh sang mép phải
            Path2D.Double tassel = new Path2D.Double();
            tassel.moveTo(18.5, 7.6);
            tassel.lineTo(18.5, 14.2);
            g2.draw(tassel);
            g2.fill(new Ellipse2D.Double(17.6, 13.8, 1.8, 1.8));
        }

        private void paintCalculator(Graphics2D g2, Color fg, Color tint) {
            // Thân máy tính bo góc
            RoundRectangle2D.Double body = new RoundRectangle2D.Double(3.5, 2.0, 13.0, 16.0, 3.2, 3.2);
            g2.setColor(tint);
            g2.fill(body);

            g2.setColor(fg);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(body);

            // Màn hình hiển thị
            RoundRectangle2D.Double screen = new RoundRectangle2D.Double(5.5, 4.2, 9.0, 2.8, 1.2, 1.2);
            g2.draw(screen);

            // Bàn phím: các nút tròn vector độc lập (chuẩn vector, không dùng zero-length line)
            double r = 0.85;
            double d = r * 2;
            // Hàng 1
            g2.fill(new Ellipse2D.Double(6.2 - r, 9.2 - r, d, d));
            g2.fill(new Ellipse2D.Double(10.0 - r, 9.2 - r, d, d));
            g2.fill(new Ellipse2D.Double(13.8 - r, 9.2 - r, d, d));
            // Hàng 2
            g2.fill(new Ellipse2D.Double(6.2 - r, 12.2 - r, d, d));
            g2.fill(new Ellipse2D.Double(10.0 - r, 12.2 - r, d, d));
            g2.fill(new Ellipse2D.Double(13.8 - r, 12.2 - r, d, d));
            // Hàng 3: 1 nút + phím bằng "=" rõ nét (2 vạch song song)
            g2.fill(new Ellipse2D.Double(6.2 - r, 15.2 - r, d, d));
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Double(9.5, 14.3, 13.8, 14.3));
            g2.draw(new Line2D.Double(9.5, 16.1, 13.8, 16.1));
        }

        private void paintChart(Graphics2D g2, Color fg, Color tint) {
            // Đường đáy cơ sở
            g2.setColor(fg);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Double(2.5, 17.5, 17.5, 17.5));

            // 3 cột biểu đồ tăng dần: đỉnh bo tròn tinh tế, đáy phẳng bám chắc đường cơ sở
            Shape b1 = createTopRoundedBar(3.8, 11.2, 3.0, 6.3, 1.5);
            Shape b2 = createTopRoundedBar(8.5, 6.8, 3.0, 10.7, 1.5);
            Shape b3 = createTopRoundedBar(13.2, 2.8, 3.0, 14.7, 1.5);

            // Duotone fill
            g2.setColor(tint);
            g2.fill(b1);
            g2.fill(b2);
            g2.fill(b3);

            // Viền sắc nét
            g2.setColor(fg);
            g2.draw(b1);
            g2.draw(b2);
            g2.draw(b3);
        }

        private static Shape createTopRoundedBar(double x, double y, double w, double h, double r) {
            Path2D.Double p = new Path2D.Double();
            p.moveTo(x, y + h);
            p.lineTo(x, y + r);
            p.quadTo(x, y, x + r, y);
            p.lineTo(x + w - r, y);
            p.quadTo(x + w, y, x + w, y + r);
            p.lineTo(x + w, y + h);
            p.closePath();
            return p;
        }

        private void paintAbout(Graphics2D g2, Color fg, Color tint) {
            // Vòng tròn bao ngoài
            Ellipse2D.Double circle = new Ellipse2D.Double(2.5, 2.5, 15.0, 15.0);
            g2.setColor(tint);
            g2.fill(circle);

            g2.setColor(fg);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(circle);

            // Chấm tròn chữ "i"
            Ellipse2D.Double dot = new Ellipse2D.Double(9.1, 5.7, 1.8, 1.8);
            g2.fill(dot);

            // Thân chữ "i"
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Double(10.0, 9.2, 10.0, 13.8));
            // Móc trên chữ "i"
            g2.draw(new Line2D.Double(8.7, 10.4, 10.0, 9.2));
            // Đế chân chữ "i"
            g2.draw(new Line2D.Double(8.6, 13.8, 11.4, 13.8));
        }

        private void paintToggle(Graphics2D g2, Color fg, boolean collapse) {
            g2.setColor(fg);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D.Double chevron = new Path2D.Double();
            if (collapse) {
                // Mũi tên trỏ sang trái: <
                chevron.moveTo(12.5, 5.5);
                chevron.lineTo(7.5, 10.0);
                chevron.lineTo(12.5, 14.5);
            } else {
                // Mũi tên trỏ sang phải: >
                chevron.moveTo(7.5, 5.5);
                chevron.lineTo(12.5, 10.0);
                chevron.lineTo(7.5, 14.5);
            }
            g2.draw(chevron);
        }

        private void paintLogo(Graphics2D g2, Color fg) {
            // Nền badge hình tròn tím nhạt hiện đại
            Ellipse2D.Double badge = new Ellipse2D.Double(1.0, 1.0, 24.0, 24.0);
            g2.setColor(new Color(0xEE, 0xF2, 0xFF));
            g2.fill(badge);

            g2.setColor(new Color(0xE0, 0xE7, 0xFF));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(badge);

            // Mũ cử nhân màu UITheme.PRIMARY sắc sảo bên trong badge
            Path2D.Double cap = new Path2D.Double();
            cap.moveTo(13.0, 6.0);
            cap.lineTo(20.5, 9.5);
            cap.lineTo(13.0, 13.0);
            cap.lineTo(5.5, 9.5);
            cap.closePath();

            Path2D.Double skull = new Path2D.Double();
            skull.moveTo(8.0, 11.2);
            skull.lineTo(8.0, 14.5);
            skull.quadTo(13.0, 17.5, 18.0, 14.5);
            skull.lineTo(18.0, 11.2);

            g2.setColor(fg != null ? fg : UITheme.PRIMARY);
            g2.fill(cap);

            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(skull);

            // Nút đỉnh mũ
            g2.fill(new Ellipse2D.Double(12.0, 8.5, 2.0, 2.0));

            // Dây tua nối từ đỉnh mũ sang phải
            Path2D.Double tassel = new Path2D.Double();
            tassel.moveTo(20.5, 9.5);
            tassel.lineTo(20.5, 15.5);
            g2.draw(tassel);
            g2.fill(new Ellipse2D.Double(19.5, 15.0, 2.0, 2.0));
        }
    }
}
