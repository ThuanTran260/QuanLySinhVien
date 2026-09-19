package com.quanlysinhvien;

import com.quanlysinhvien.ui.animation.Easing;
import com.quanlysinhvien.ui.animation.SlideTabbedPane;
import com.quanlysinhvien.ui.theme.ModernButton;
import com.quanlysinhvien.ui.theme.ModernCardPanel;
import com.quanlysinhvien.ui.theme.ModernTableRenderer;
import com.quanlysinhvien.ui.theme.UITheme;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

public class ModernUIThemeAndAnimationTest {

    @Test
    @DisplayName("Kiểm tra các hàm nội suy Easing (EaseOutCubic & EaseInOutQuad)")
    void testEasingFormulas() {
        // Boundary t = 0
        assertEquals(0.0f, Easing.easeOutCubic(0.0f), 0.0001f);
        assertEquals(0.0f, Easing.easeInOutQuad(0.0f), 0.0001f);

        // Boundary t = 1
        assertEquals(1.0f, Easing.easeOutCubic(1.0f), 0.0001f);
        assertEquals(1.0f, Easing.easeInOutQuad(1.0f), 0.0001f);

        // Midpoint t = 0.5: easeOutCubic = 1 - 0.5^3 = 0.875
        assertEquals(0.875f, Easing.easeOutCubic(0.5f), 0.0001f);

        // Clamping values outside [0, 1]
        assertEquals(0.0f, Easing.easeOutCubic(-0.5f), 0.0001f);
        assertEquals(1.0f, Easing.easeOutCubic(1.5f), 0.0001f);
        assertEquals(0.0f, Easing.clamp(-1.0f), 0.0001f);
        assertEquals(1.0f, Easing.clamp(2.0f), 0.0001f);
    }

    @Test
    @DisplayName("Kiểm tra Design Tokens của UITheme")
    void testUIThemeTokens() {
        assertNotNull(UITheme.CANVAS_BG);
        assertEquals(new Color(0xF6, 0xF9, 0xFC), UITheme.CANVAS_BG);
        assertEquals(Color.WHITE, UITheme.CARD_BG);
        assertEquals(new Color(0x63, 0x5B, 0xFF), UITheme.PRIMARY);

        assertNotNull(UITheme.BADGE_XUAT_SAC_BG);
        assertNotNull(UITheme.BADGE_GIOI_BG);
        assertNotNull(UITheme.BADGE_KHA_BG);
        assertNotNull(UITheme.BADGE_TRUNG_BINH_BG);
        assertNotNull(UITheme.BADGE_YEU_BG);

        assertNotNull(UITheme.FONT_TITLE_LARGE);
        assertNotNull(UITheme.FONT_REGULAR);
        assertNotNull(UITheme.FONT_BADGE);

        // Kiểm tra nạp Look and Feel FlatMacLightLaf hiện đại
        assertTrue(Main.setupModernLaf(), "FlatMacLightLaf phải thiết lập thành công");
        assertEquals("underlined", UIManager.getString("TabbedPane.tabType"));
        assertEquals(UITheme.PRIMARY, UIManager.getColor("TabbedPane.underlineColor"));
    }

    @Test
    @DisplayName("Kiểm tra ModernButton khởi tạo và thuộc tính")
    void testModernButton() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong môi trường headless");

        ModernButton btn = new ModernButton("Click Me", UITheme.PRIMARY);
        assertNotNull(btn);
        assertEquals("Click Me", btn.getText());
        assertFalse(btn.isOpaque(), "Nút phải setOpaque(false) để vẽ bo góc khử răng cưa");
        assertFalse(btn.isFocusPainted());
        assertFalse(btn.isContentAreaFilled());

        btn.setCornerRadius(12);
        btn.setBorderColor(UITheme.BORDER_COLOR);

        boolean[] clicked = {false};
        btn.addActionListener(e -> clicked[0] = true);
        btn.doClick();
        assertTrue(clicked[0], "doClick() phải kích hoạt ActionListener");
    }

    @Test
    @DisplayName("Kiểm tra ModernCardPanel không bị ghosting / setOpaque(false)")
    void testModernCardPanel() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong môi trường headless");

        ModernCardPanel card = new ModernCardPanel(new FlowLayout(), 20);
        assertNotNull(card);
        assertFalse(card.isOpaque(), "Card phải setOpaque(false) để chống lỗi smear/ghosting");

        card.setCardBackground(Color.WHITE);
        card.setBorderColor(UITheme.BORDER_COLOR);
        card.setCornerRadius(10);
    }

    @Test
    @DisplayName("Kiểm tra ModernTableRenderer định dạng Badge xếp loại và số thực Điểm TB")
    void testModernTableRenderer() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong môi trường headless");

        String[] columns = {"STT", "Mã SV", "Họ và Tên", "Lớp", "Ngày Sinh", "Điểm TB", "Xếp Loại"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        model.addRow(new Object[]{1, "SV001", "Nguyễn Văn A", "DCT1241", "2006-01-01", 9.2f, "Xuất sắc"});
        model.addRow(new Object[]{2, "SV002", "Trần Thị B", "DCT1242", "2006-02-02", 8.1f, "Giỏi"});
        model.addRow(new Object[]{3, "SV003", "Lê Văn C", "DCT1243", "2006-03-03", 6.8f, "Khá"});
        model.addRow(new Object[]{4, "SV004", "Phạm Văn D", "DCT1244", "2006-04-04", 5.2f, "Trung bình"});
        model.addRow(new Object[]{5, "SV005", "Hoàng Văn E", "DCT1245", "2006-05-05", 3.5f, "Yếu"});

        JTable table = new JTable(model);
        ModernTableRenderer.applyModernStyle(table);

        assertEquals(32, table.getRowHeight(), "Chiều cao dòng bảng phải là 32px");

        ModernTableRenderer renderer = new ModernTableRenderer();

        // Kiểm tra cell Điểm TB (cột 5)
        Component compScore = renderer.getTableCellRendererComponent(table, 9.2f, false, false, 0, 5);
        assertTrue(compScore instanceof JLabel);
        assertEquals("9.2", ((JLabel) compScore).getText().trim());

        // Kiểm tra cell Xếp loại (cột 6 - Xuất sắc)
        Component compXuatSac = renderer.getTableCellRendererComponent(table, "Xuất sắc", false, false, 0, 6);
        assertEquals("Xuất sắc", ((JLabel) compXuatSac).getText().trim());
        assertEquals(UITheme.BADGE_XUAT_SAC_TEXT, compXuatSac.getForeground());

        // Kiểm tra cell Xếp loại (cột 6 - Giỏi)
        Component compGioi = renderer.getTableCellRendererComponent(table, "Giỏi", false, false, 1, 6);
        assertEquals(UITheme.BADGE_GIOI_TEXT, compGioi.getForeground());

        // Kiểm tra cell Xếp loại (cột 6 - Khá)
        Component compKha = renderer.getTableCellRendererComponent(table, "Khá", false, false, 2, 6);
        assertEquals(UITheme.BADGE_KHA_TEXT, compKha.getForeground());

        // Kiểm tra cell Xếp loại (cột 6 - Yếu)
        Component compYeu = renderer.getTableCellRendererComponent(table, "Yếu", false, false, 4, 6);
        assertEquals(UITheme.BADGE_YEU_TEXT, compYeu.getForeground());
    }

    @Test
    @DisplayName("Kiểm tra SlideTabbedPane tích hợp và hỗ trợ chuyển tab")
    void testSlideTabbedPane() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong môi trường headless");

        SlideTabbedPane pane = new SlideTabbedPane();
        assertNotNull(pane);
        assertTrue(pane.isAnimationEnabled());

        JPanel p1 = new JPanel();
        JPanel p2 = new JPanel();
        pane.addTab("Tab 1", p1);
        pane.addTab("Tab 2", p2);

        assertEquals(2, pane.getTabCount());
        assertEquals(0, pane.getSelectedIndex());

        pane.setSelectedIndex(1);
        assertEquals(1, pane.getSelectedIndex());

        pane.setAnimationEnabled(false);
        assertFalse(pane.isAnimationEnabled());

        // Kiểm tra lifecycle hooks không gây lỗi
        pane.removeNotify();
        pane.removeTabAt(0);
        assertEquals(1, pane.getTabCount());
        pane.removeAll();
        assertEquals(0, pane.getTabCount());
    }

    @Test
    @DisplayName("Kiểm tra SlideTabbedPane kích hoạt animation và chịu tải chuyển tab liên tục trong JFrame")
    void testSlideTabbedPaneAnimationInFrame() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong môi trường headless");

        JFrame frame = new JFrame("Test Frame");
        SlideTabbedPane pane = new SlideTabbedPane();
        JPanel p1 = new JPanel(); p1.add(new JLabel("Content 1"));
        JPanel p2 = new JPanel(); p2.add(new JLabel("Content 2"));
        pane.addTab("Tab 1", p1);
        pane.addTab("Tab 2", p2);
        frame.getContentPane().add(pane);
        frame.setSize(400, 300);
        frame.setVisible(true);

        try {
            pane.setSelectedIndex(1);
            assertEquals(1, pane.getSelectedIndex());

            // Rapid tab switching (stress test attack priority 1 from prior report)
            for (int i = 0; i < 10; i++) {
                pane.setSelectedIndex(i % 2);
            }
            assertTrue(pane.getSelectedIndex() == 0 || pane.getSelectedIndex() == 1);
        } finally {
            frame.dispose();
        }
    }

    @Test
    @DisplayName("Kiểm tra ModernButton trạng thái vô hiệu hóa")
    void testModernButtonDisabledState() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong môi trường headless");

        ModernButton btn = new ModernButton("Save", UITheme.PRIMARY);
        btn.setEnabled(false);
        assertFalse(btn.isEnabled());
        btn.setEnabled(true);
        assertTrue(btn.isEnabled());
    }

    @Test
    @DisplayName("Kiểm tra ModernTableRenderer có padding và border hợp lệ")
    void testModernTableRendererPadding() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong môi trường headless");

        JTable table = new JTable(new DefaultTableModel(new Object[]{"Col"}, 1));
        ModernTableRenderer renderer = new ModernTableRenderer();
        Component comp = renderer.getTableCellRendererComponent(table, "Test Value", false, false, 0, 0);
        assertTrue(comp instanceof JComponent);
        assertNotNull(((JComponent) comp).getBorder());
    }

    @Test
    @DisplayName("Kiểm tra StudentManagementPanel sort và search không crash khi ngoại tuyến")
    void testStudentManagementPanelSortingAndFallback() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong môi trường headless");

        com.quanlysinhvien.ui.StudentManagementPanel panel = new com.quanlysinhvien.ui.StudentManagementPanel();
        assertNotNull(panel);
        assertEquals(84, panel.getTable().getRowCount());

        // Kiểm tra column classes
        assertEquals(Integer.class, panel.getTableModel().getColumnClass(0));
        assertEquals(Float.class, panel.getTableModel().getColumnClass(5));

        // Sắp xếp in-memory fallback
        panel.setSortIndex(4); // Điểm TB (Tăng dần)
        panel.triggerSort();
        assertEquals(84, panel.getTable().getRowCount());

        // Kiểm tra dòng đầu có điểm <= dòng cuối
        float firstScore = ((Number) panel.getTableModel().getValueAt(0, 5)).floatValue();
        float lastScore = ((Number) panel.getTableModel().getValueAt(83, 5)).floatValue();
        assertTrue(firstScore <= lastScore, "Điểm tăng dần: " + firstScore + " <= " + lastScore);
    }
}
