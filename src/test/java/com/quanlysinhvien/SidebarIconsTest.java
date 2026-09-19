package com.quanlysinhvien;

import com.quanlysinhvien.ui.components.ModernSidebar;
import com.quanlysinhvien.ui.components.SidebarIcons;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class SidebarIconsTest {

    @Test
    @DisplayName("Kiểm tra khởi tạo đầy đủ các loại SidebarIcons vector")
    void testSidebarIconsCreation() {
        Icon student = SidebarIcons.createStudentIcon(20);
        assertNotNull(student);
        assertEquals(20, student.getIconWidth());
        assertEquals(20, student.getIconHeight());

        Icon calc = SidebarIcons.createCalculatorIcon(20);
        assertNotNull(calc);
        assertEquals(20, calc.getIconWidth());
        assertEquals(20, calc.getIconHeight());

        Icon chart = SidebarIcons.createChartIcon(20);
        assertNotNull(chart);
        assertEquals(20, chart.getIconWidth());
        assertEquals(20, chart.getIconHeight());

        Icon about = SidebarIcons.createAboutIcon(20);
        assertNotNull(about);
        assertEquals(20, about.getIconWidth());
        assertEquals(20, about.getIconHeight());

        Icon toggleCollapse = SidebarIcons.createToggleIcon(16, true);
        assertNotNull(toggleCollapse);
        assertEquals(16, toggleCollapse.getIconWidth());
        assertEquals(16, toggleCollapse.getIconHeight());

        Icon toggleExpand = SidebarIcons.createToggleIcon(16, false);
        assertNotNull(toggleExpand);
        assertEquals(16, toggleExpand.getIconWidth());
        assertEquals(16, toggleExpand.getIconHeight());

        Icon logo = SidebarIcons.createLogoIcon(26);
        assertNotNull(logo);
        assertEquals(26, logo.getIconWidth());
        assertEquals(26, logo.getIconHeight());

        // Default overloaded constructors
        assertEquals(20, SidebarIcons.createStudentIcon().getIconWidth());
        assertEquals(20, SidebarIcons.createCalculatorIcon().getIconWidth());
        assertEquals(20, SidebarIcons.createChartIcon().getIconWidth());
        assertEquals(20, SidebarIcons.createAboutIcon().getIconWidth());
        assertEquals(16, SidebarIcons.createToggleIcon(true).getIconWidth());
        assertEquals(26, SidebarIcons.createLogoIcon().getIconWidth());
    }

    @Test
    @DisplayName("Kiểm tra ánh xạ getNavIcon theo MenuId")
    void testGetNavIconByMenuId() {
        Icon iconSV = SidebarIcons.getNavIcon(ModernSidebar.MenuId.SINH_VIEN, 20);
        assertTrue(iconSV instanceof SidebarIcons.VectorIcon);
        assertEquals(SidebarIcons.IconType.STUDENT, ((SidebarIcons.VectorIcon) iconSV).getType());

        Icon iconMT = SidebarIcons.getNavIcon(ModernSidebar.MenuId.MAY_TINH, 20);
        assertTrue(iconMT instanceof SidebarIcons.VectorIcon);
        assertEquals(SidebarIcons.IconType.CALCULATOR, ((SidebarIcons.VectorIcon) iconMT).getType());

        Icon iconTK = SidebarIcons.getNavIcon(ModernSidebar.MenuId.THONG_KE, 20);
        assertTrue(iconTK instanceof SidebarIcons.VectorIcon);
        assertEquals(SidebarIcons.IconType.CHART, ((SidebarIcons.VectorIcon) iconTK).getType());

        Icon iconGT = SidebarIcons.getNavIcon(ModernSidebar.MenuId.GIOI_THIEU, 20);
        assertTrue(iconGT instanceof SidebarIcons.VectorIcon);
        assertEquals(SidebarIcons.IconType.ABOUT, ((SidebarIcons.VectorIcon) iconGT).getType());

        // Null fallback
        Icon iconNull = SidebarIcons.getNavIcon(null, 20);
        assertNotNull(iconNull);
        assertEquals(SidebarIcons.IconType.STUDENT, ((SidebarIcons.VectorIcon) iconNull).getType());
    }

    @Test
    @DisplayName("Kiểm tra paintIcon thực thi vẽ pixel thành công trên Graphics2D (chống crash và không rỗng)")
    void testPaintIconOffscreen() {
        ModernSidebar sidebar = new ModernSidebar();
        ModernSidebar.NavButton activeBtn = (ModernSidebar.NavButton) sidebar.getNavButton(ModernSidebar.MenuId.SINH_VIEN);
        ModernSidebar.NavButton inactiveBtn = (ModernSidebar.NavButton) sidebar.getNavButton(ModernSidebar.MenuId.MAY_TINH);

        SidebarIcons.IconType[] types = SidebarIcons.IconType.values();
        for (SidebarIcons.IconType type : types) {
            SidebarIcons.VectorIcon icon = new SidebarIcons.VectorIcon(type, 20, null);

            // 1. Vẽ với component null (không văng lỗi)
            BufferedImage img1 = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g1 = img1.createGraphics();
            assertDoesNotThrow(() -> icon.paintIcon(null, g1, 5, 5));
            g1.dispose();
            assertTrue(hasNonTransparentPixel(img1), "Icon " + type + " phải vẽ được pixel khi c == null");

            // 2. Vẽ với nút active
            BufferedImage img2 = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img2.createGraphics();
            assertDoesNotThrow(() -> icon.paintIcon(activeBtn, g2, 5, 5));
            g2.dispose();
            assertTrue(hasNonTransparentPixel(img2), "Icon " + type + " phải vẽ được pixel với active button");

            // 3. Vẽ với nút inactive
            BufferedImage img3 = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g3 = img3.createGraphics();
            assertDoesNotThrow(() -> icon.paintIcon(inactiveBtn, g3, 5, 5));
            g3.dispose();
            assertTrue(hasNonTransparentPixel(img3), "Icon " + type + " phải vẽ được pixel với inactive button");

            // 4. Vẽ với nút disabled
            JButton disabledBtn = new JButton();
            disabledBtn.setEnabled(false);
            BufferedImage img4 = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g4 = img4.createGraphics();
            assertDoesNotThrow(() -> icon.paintIcon(disabledBtn, g4, 5, 5));
            g4.dispose();
            assertTrue(hasNonTransparentPixel(img4), "Icon " + type + " phải vẽ được pixel khi disabled");
        }
    }

    @Test
    @DisplayName("Kiểm tra tỷ lệ co giãn DPI (scale != 20) vẽ mượt mà không lỗi")
    void testScalingDpi() {
        int[] sizes = {16, 18, 24, 32, 48};
        for (int sz : sizes) {
            Icon student = SidebarIcons.createStudentIcon(sz);
            Icon calc = SidebarIcons.createCalculatorIcon(sz);
            Icon chart = SidebarIcons.createChartIcon(sz);
            Icon about = SidebarIcons.createAboutIcon(sz);
            Icon logo = SidebarIcons.createLogoIcon(sz);

            BufferedImage img = new BufferedImage(sz + 10, sz + 10, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();

            assertDoesNotThrow(() -> student.paintIcon(null, g, 0, 0));
            assertDoesNotThrow(() -> calc.paintIcon(null, g, 0, 0));
            assertDoesNotThrow(() -> chart.paintIcon(null, g, 0, 0));
            assertDoesNotThrow(() -> about.paintIcon(null, g, 0, 0));
            assertDoesNotThrow(() -> logo.paintIcon(null, g, 0, 0));

            g.dispose();
        }
    }

    @Test
    @DisplayName("Kiểm tra tích hợp ModernSidebar: tất cả các mục có Icon hợp lệ và loại bỏ emoji chuỗi")
    void testModernSidebarIconsIntegration() {
        ModernSidebar sidebar = new ModernSidebar();

        // 1. Kiểm tra 4 nút điều hướng
        for (ModernSidebar.MenuId id : ModernSidebar.MenuId.values()) {
            JButton btn = sidebar.getNavButton(id);
            assertNotNull(btn, "Nút " + id + " phải tồn tại");
            assertNotNull(btn.getIcon(), "Nút " + id + " phải có Icon vector");
            assertTrue(btn.getIcon() instanceof SidebarIcons.VectorIcon, "Phải là SidebarIcons.VectorIcon");

            // Trong chế độ Expanded, text không được chứa ký tự emoji hoặc dấu chấm vuông
            String text = btn.getText();
            assertNotNull(text);
            assertFalse(text.contains("🎓"), "Text không được chứa emoji tốt nghiệp");
            assertFalse(text.contains("🧮"), "Text không được chứa emoji bàn tính");
            assertFalse(text.contains("📊"), "Text không được chứa emoji biểu đồ");
            assertFalse(text.contains("ℹ"), "Text không được chứa emoji info");
        }

        // 2. Kiểm tra Toggle Button
        JButton toggleBtn = sidebar.getToggleBtn();
        assertNotNull(toggleBtn);
        assertNotNull(toggleBtn.getIcon());
        assertTrue(toggleBtn.getIcon() instanceof SidebarIcons.VectorIcon);
        assertEquals(SidebarIcons.IconType.TOGGLE_COLLAPSE, ((SidebarIcons.VectorIcon) toggleBtn.getIcon()).getType());

        // Thu gọn sidebar
        sidebar.toggle();
        assertFalse(sidebar.isExpanded());

        // Trong chế độ collapsed, text của các nút phải là rỗng để căn giữa Icon hoàn hảo
        for (ModernSidebar.MenuId id : ModernSidebar.MenuId.values()) {
            JButton btn = sidebar.getNavButton(id);
            assertEquals("", btn.getText(), "Khi collapsed, text nút phải rỗng");
            assertNotNull(btn.getIcon(), "Khi collapsed, icon vẫn phải hiện diện");
        }

        // Toggle button chuyển sang icon TOGGLE_EXPAND
        assertEquals(SidebarIcons.IconType.TOGGLE_EXPAND, ((SidebarIcons.VectorIcon) toggleBtn.getIcon()).getType());

        // Mở rộng lại
        sidebar.toggle();
        assertTrue(sidebar.isExpanded());
        assertEquals(SidebarIcons.IconType.TOGGLE_COLLAPSE, ((SidebarIcons.VectorIcon) toggleBtn.getIcon()).getType());
    }

    @Test
    @DisplayName("Kiểm tra căn trục dọc (Vertical alignment) chính xác giữa Toggle button và Nav button")
    void testIconVerticalAlignment() {
        ModernSidebar sb = new ModernSidebar();

        // 1. Kiểm tra ở chế độ Collapsed (64px) -> trục tâm icon phải khớp nhau ở ~31-32px
        sb.setSize(ModernSidebar.WIDTH_COLLAPSED, 600);
        sb.setExpanded(false);
        sb.doLayout();
        for (Component c : sb.getComponents()) c.doLayout();

        JButton toggle = sb.getToggleBtn();
        JButton navSV = sb.getNavButton(ModernSidebar.MenuId.SINH_VIEN);

        int toggleCenterCollapsed = computeIconCenter(toggle);
        int navCenterCollapsed = computeIconCenter(navSV);
        assertEquals(navCenterCollapsed, toggleCenterCollapsed,
                "Trong collapsed mode, tâm Toggle icon (" + toggleCenterCollapsed + ") phải thẳng hàng với Nav icon (" + navCenterCollapsed + ")");

        // 2. Kiểm tra ở chế độ Expanded (210px) -> trục tâm icon phải khớp nhau ở 28px
        sb.setSize(ModernSidebar.WIDTH_EXPANDED, 600);
        sb.setExpanded(true);
        sb.doLayout();
        for (Component c : sb.getComponents()) c.doLayout();

        int toggleCenterExpanded = computeIconCenter(toggle);
        int navCenterExpanded = computeIconCenter(navSV);
        assertEquals(navCenterExpanded, toggleCenterExpanded,
                "Trong expanded mode, tâm Toggle icon (" + toggleCenterExpanded + ") phải thẳng hàng với Nav icon (" + navCenterExpanded + ")");
    }

    @Test
    @DisplayName("Kiểm tra an toàn luồng Swing: paintComponent không gọi setForeground gây repaint lặp vô tận")
    void testNavButtonRepaintSafety() {
        class RepaintCountNavButton extends ModernSidebar.NavButton {
            int repaintCount = 0;
            public RepaintCountNavButton() {
                super(ModernSidebar.MenuId.SINH_VIEN, "Sinh Viên", SidebarIcons.createStudentIcon(20), "SV");
            }
            @Override
            public void repaint() {
                repaintCount++;
                super.repaint();
            }
        }

        RepaintCountNavButton btn = new RepaintCountNavButton();
        btn.setSize(198, 40);
        btn.doLayout();

        BufferedImage img = new BufferedImage(198, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        int repaintsBefore = btn.repaintCount;
        btn.paint(g);
        int repaintsAfter = btn.repaintCount;
        g.dispose();

        assertEquals(repaintsBefore, repaintsAfter, "paint() không được gây re-entrant repaint()");
    }

    private int computeIconCenter(JButton btn) {
        Rectangle viewR = new Rectangle(0, 0, btn.getWidth(), btn.getHeight());
        Insets insets = btn.getInsets();
        viewR.x += insets.left;
        viewR.y += insets.top;
        viewR.width -= (insets.left + insets.right);
        viewR.height -= (insets.top + insets.bottom);
        Rectangle iconR = new Rectangle();
        Rectangle textR = new Rectangle();
        SwingUtilities.layoutCompoundLabel(btn, btn.getFontMetrics(btn.getFont()),
                btn.getText(), btn.getIcon(),
                btn.getVerticalAlignment(), btn.getHorizontalAlignment(),
                btn.getVerticalTextPosition(), btn.getHorizontalTextPosition(),
                viewR, iconR, textR, btn.getIconTextGap());
        return btn.getX() + iconR.x + iconR.width / 2;
    }

    private boolean hasNonTransparentPixel(BufferedImage img) {
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int argb = img.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha > 10) {
                    return true;
                }
            }
        }
        return false;
    }
}
