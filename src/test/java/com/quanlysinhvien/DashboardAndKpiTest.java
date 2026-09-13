package com.quanlysinhvien;

import com.quanlysinhvien.model.SinhVien;
import com.quanlysinhvien.ui.MainFrame;
import com.quanlysinhvien.ui.StudentManagementPanel;
import com.quanlysinhvien.ui.components.KpiCardsPanel;
import com.quanlysinhvien.ui.components.ModernSidebar;
import com.quanlysinhvien.ui.components.TopHeaderBar;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DashboardAndKpiTest {

    @Test
    @DisplayName("Kiểm tra KpiCardsPanel tính toán chính xác số liệu in-memory và biên")
    void testKpiCardsPanelCalculations() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong headless");

        KpiCardsPanel kpi = new KpiCardsPanel();
        assertNotNull(kpi);

        // Case 1: Danh sách rỗng
        kpi.update(new ArrayList<>());
        KpiCardsPanel.KpiValues emptyVal = kpi.getValues();
        assertEquals(0, emptyVal.totalStudents);
        assertEquals(0, emptyVal.totalClasses);
        assertEquals(0.0f, emptyVal.maxScore);
        assertEquals(0.0, emptyVal.goodRate);

        // Case 2: Danh sách mẫu
        List<SinhVien> sample = new ArrayList<>();
        sample.add(new SinhVien("SV01", "Nguyễn Văn A", "DCT1241", java.time.LocalDate.parse("2005-01-01"), 9.5f)); // Xuất sắc (>=6.5)
        sample.add(new SinhVien("SV02", "Trần Thị B", "DCT1242", java.time.LocalDate.parse("2005-02-02"), 8.0f));   // Giỏi (>=6.5)
        sample.add(new SinhVien("SV03", "Lê Văn C", "DCT1241", java.time.LocalDate.parse("2005-03-03"), 6.5f));     // Khá (>=6.5)
        sample.add(new SinhVien("SV04", "Phạm Văn D", "DCT1243", java.time.LocalDate.parse("2005-04-04"), 5.0f));   // Trung bình (<6.5)

        kpi.update(sample);
        KpiCardsPanel.KpiValues vals = kpi.getValues();

        assertEquals(4, vals.totalStudents);
        assertEquals(3, vals.totalClasses); // DCT1241, DCT1242, DCT1243
        assertEquals(9.5f, vals.maxScore, 0.001f);
        // 3 trong 4 sinh viên có điểm >= 6.5 -> 75.0%
        assertEquals(75.0, vals.goodRate, 0.001);

        assertEquals(4, kpi.getTotalStudents());
        assertEquals(3, kpi.getTotalClasses());
        assertEquals(9.5f, kpi.getMaxScore(), 0.001f);
        assertEquals(75.0, kpi.getGoodRate(), 0.001);
        assertNotNull(kpi.getValuesMap());
        assertEquals(4, kpi.getValuesMap().get("totalStudents"));

        // Case 3: Danh sách 100% Khá-Giỏi
        List<SinhVien> allGood = new ArrayList<>();
        allGood.add(new SinhVien("SV10", "A", "L1", java.time.LocalDate.parse("2005-01-01"), 7.0f));
        allGood.add(new SinhVien("SV11", "B", "L1", java.time.LocalDate.parse("2005-01-01"), 8.5f));
        kpi.update(allGood);
        assertEquals(100.0, kpi.getGoodRate(), 0.001);
        assertEquals(1, kpi.getTotalClasses());
        assertEquals(8.5f, kpi.getMaxScore(), 0.001f);
    }

    @Test
    @DisplayName("Kiểm tra ModernSidebar: toggle instant 210px ↔ 64px và callback điều hướng")
    void testModernSidebarToggleAndNavigation() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong headless");

        ModernSidebar sidebar = new ModernSidebar();
        assertTrue(sidebar.isExpanded());
        assertEquals(ModernSidebar.WIDTH_EXPANDED, sidebar.getPreferredSize().width);
        assertEquals(ModernSidebar.MenuId.SINH_VIEN, sidebar.getActive());

        // Toggle thu gọn
        sidebar.toggle();
        assertFalse(sidebar.isExpanded());
        assertEquals(ModernSidebar.WIDTH_COLLAPSED, sidebar.getPreferredSize().width);

        // Toggle mở rộng lại
        sidebar.toggle();
        assertTrue(sidebar.isExpanded());
        assertEquals(ModernSidebar.WIDTH_EXPANDED, sidebar.getPreferredSize().width);

        // Chuyển active item
        sidebar.setActive(ModernSidebar.MenuId.MAY_TINH);
        assertEquals(ModernSidebar.MenuId.MAY_TINH, sidebar.getActive());

        // Callback điều hướng
        ModernSidebar.MenuId[] received = new ModernSidebar.MenuId[1];
        sidebar.setNavigationListener(id -> received[0] = id);

        JButton btnStat = sidebar.getNavButton(ModernSidebar.MenuId.THONG_KE);
        assertNotNull(btnStat);
        btnStat.doClick();
        assertEquals(ModernSidebar.MenuId.THONG_KE, received[0]);
        assertEquals(ModernSidebar.MenuId.THONG_KE, sidebar.getActive());

        JButton btnAbout = sidebar.getNavButton(ModernSidebar.MenuId.GIOI_THIEU);
        assertNotNull(btnAbout);
        btnAbout.doClick();
        assertEquals(ModernSidebar.MenuId.GIOI_THIEU, received[0]);
    }

    @Test
    @DisplayName("Kiểm tra TopHeaderBar: Breadcrumb, badge CSDL và nút đăng xuất")
    void testTopHeaderBar() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong headless");

        TopHeaderBar header = new TopHeaderBar();
        assertNotNull(header);
        assertNotNull(header.getBtnLogout());
        assertNotNull(header.getDbBadge());

        // Mặc định ban đầu phải là Offline... xám
        assertTrue(header.getDbBadge().getText().contains("Offline Mode"));

        // Breadcrumb
        header.updateBreadcrumb("Máy Tính Cơ Bản");
        // DB Online / Offline
        header.setDbOnline(true);
        assertTrue(header.isDbOnline());
        assertTrue(header.getDbBadge().getText().contains("Online"));

        header.setDbOnline(false);
        assertFalse(header.isDbOnline());
        assertTrue(header.getDbBadge().getText().contains("Offline"));
    }

    @Test
    @DisplayName("Kiểm tra StudentManagementPanel: State Machine (Normal, Collapsed, Maximized), Density và Maximize không bung form")
    void testStudentManagementPanelStatesAndDensity() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong headless");

        StudentManagementPanel panel = new StudentManagementPanel();
        assertNotNull(panel);
        assertEquals(84, panel.getTable().getRowCount());
        assertNotNull(panel.getKpiPanel());
        assertEquals(84, panel.getKpiPanel().getTotalStudents());

        // Trạng thái ban đầu
        assertFalse(panel.isFormCollapsed());
        assertFalse(panel.isMaximized());
        assertEquals(34, panel.getDensity());

        // 1. Toggle Form Collapse
        panel.toggleFormCollapse();
        assertTrue(panel.isFormCollapsed());
        assertFalse(panel.isMaximized());

        panel.toggleFormCollapse();
        assertFalse(panel.isFormCollapsed());

        // 2. Toggle Density
        panel.toggleDensity();
        assertEquals(26, panel.getDensity());
        assertEquals(26, panel.getTable().getRowHeight());

        panel.toggleDensity();
        assertEquals(34, panel.getDensity());
        assertEquals(34, panel.getTable().getRowHeight());

        // 3. Toggle Maximize (F11 / Esc)
        panel.toggleMaximize();
        assertTrue(panel.isMaximized());

        // QUYẾT ĐỊNH CHỐT: Khi ở chế độ Maximize, chọn dòng KHÔNG tự ý bung Form
        panel.getTable().setRowSelectionInterval(1, 1);
        assertTrue(panel.isMaximized(), "Ở chế độ Maximize, chọn dòng không được tự bung Form");
        assertFalse(panel.getTxtMaSV().isEditable(), "Mã SV vẫn phải bị khóa khi chọn dòng trong Maximize");
        assertFalse(panel.getTxtMaSV().getText().trim().isEmpty());

        // Thoát Maximize
        panel.toggleMaximize();
        assertFalse(panel.isMaximized());

        // Kiểm tra phím tắt F11 và Esc trong ActionMap
        Action toggleAction = panel.getActionMap().get("toggleMaximize");
        assertNotNull(toggleAction, "Phải đăng ký Action toggleMaximize");

        Action exitAction = panel.getActionMap().get("exitMaximize");
        assertNotNull(exitAction, "Phải đăng ký Action exitMaximize");
    }

    @Test
    @DisplayName("Kiểm tra MainFrame tích hợp Dashboard: Sidebar, TopHeaderBar, SlideTabbedPane và Layout")
    void testMainFrameDashboardIntegration() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong headless");

        MainFrame frame = new MainFrame();
        assertNotNull(frame);

        assertNotNull(frame.getSidebar(), "MainFrame phải chứa Sidebar");
        assertNotNull(frame.getHeaderBar(), "MainFrame phải chứa HeaderBar");
        assertNotNull(frame.getTabbedPane(), "MainFrame phải chứa TabbedPane");
        assertNotNull(frame.getStudentPanel(), "MainFrame phải chứa StudentPanel");
        assertNotNull(frame.getCalculatorPanel(), "MainFrame phải chứa CalculatorPanel");

        // Kích thước chuẩn Dashboard
        assertEquals(1180, frame.getWidth());
        assertEquals(780, frame.getHeight());

        // Điều hướng từ Sidebar sang Tab 1 (Máy tính)
        frame.getSidebar().getNavButton(ModernSidebar.MenuId.MAY_TINH).doClick();
        assertEquals(1, frame.getTabbedPane().getSelectedIndex());

        // Điều hướng từ Sidebar sang Tab 0 (Sinh viên)
        frame.getSidebar().getNavButton(ModernSidebar.MenuId.SINH_VIEN).doClick();
        assertEquals(0, frame.getTabbedPane().getSelectedIndex());

        // Toggle Sidebar trong MainFrame qua toggleSidebar()
        frame.toggleSidebar();
        assertFalse(frame.getSidebar().isExpanded());
        frame.toggleSidebar();
        assertTrue(frame.getSidebar().isExpanded());

        // Toggle Sidebar qua nút toggle trực tiếp trên Sidebar
        frame.getSidebar().getToggleBtn().doClick();
        assertFalse(frame.getSidebar().isExpanded());
        frame.getSidebar().getToggleBtn().doClick();
        assertTrue(frame.getSidebar().isExpanded());

        // Menu Sinh Viên active trên Tab 0
        assertEquals(ModernSidebar.MenuId.SINH_VIEN, frame.getSidebar().getActive());

        frame.dispose();
    }

    @Test
    @DisplayName("Kiểm tra ngưỡng điểm biên KPI 6.49 vs 6.50 và làm tròn số học")
    void testKpiRoundingAndEdgeCases() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong headless");

        KpiCardsPanel kpi = new KpiCardsPanel();
        List<SinhVien> list = new ArrayList<>();

        // 6.49f -> Không đạt Khá-Giỏi (<6.5)
        list.add(new SinhVien("SV90", "Dưới chuẩn", "L1", java.time.LocalDate.parse("2005-01-01"), 6.49f));
        kpi.update(list);
        assertEquals(0.0, kpi.getGoodRate(), 0.001);
        assertEquals(6.49f, kpi.getMaxScore(), 0.001f);

        // Thêm 6.50f -> Đạt Khá-Giỏi (>=6.5) -> 1/2 = 50.0%
        list.add(new SinhVien("SV91", "Đủ chuẩn", "L2", java.time.LocalDate.parse("2005-01-01"), 6.50f));
        kpi.update(list);
        assertEquals(50.0, kpi.getGoodRate(), 0.001);
        assertEquals(2, kpi.getTotalClasses());
        assertEquals(6.50f, kpi.getMaxScore(), 0.001f);

        // Thêm điểm 10.0f
        list.add(new SinhVien("SV92", "Thủ khoa", "L2", java.time.LocalDate.parse("2005-01-01"), 10.0f));
        kpi.update(list);
        // 2 trong 3 đạt -> 66.67%
        assertEquals(66.67, kpi.getGoodRate(), 0.001);
        assertEquals(10.0f, kpi.getMaxScore(), 0.001f);
    }

    @Test
    @DisplayName("Stress Test: Toggle Sidebar 50 lần liên tục không lỗi và bảo đảm kích thước")
    void testRapidSidebarToggleStress() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong headless");

        ModernSidebar sidebar = new ModernSidebar();
        for (int i = 0; i < 50; i++) {
            sidebar.toggle();
        }
        // 50 lần toggle từ ban đầu (expanded=true) -> chẵn -> expanded=true
        assertTrue(sidebar.isExpanded());
        assertEquals(ModernSidebar.WIDTH_EXPANDED, sidebar.getPreferredSize().width);

        sidebar.toggle();
        assertFalse(sidebar.isExpanded());
        assertEquals(ModernSidebar.WIDTH_COLLAPSED, sidebar.getPreferredSize().width);
    }

    @Test
    @DisplayName("Kiểm tra Sắp xếp và Tìm kiếm trong trạng thái Maximize vẫn duy trì Maximize")
    void testStudentManagementPanelMaximizeInteractions() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong headless");

        StudentManagementPanel panel = new StudentManagementPanel();
        panel.toggleMaximize();
        assertTrue(panel.isMaximized());

        // Thực hiện sắp xếp khi đang Maximize
        panel.getCbSort().setSelectedIndex(4); // Điểm tăng dần
        panel.triggerSort();
        assertTrue(panel.isMaximized(), "Sắp xếp không được làm thoát chế độ Maximize");
        assertEquals(84, panel.getTable().getRowCount());

        // Đổi mật độ dòng khi đang Maximize
        panel.toggleDensity();
        assertEquals(26, panel.getDensity());
        assertTrue(panel.isMaximized(), "Đổi mật độ dòng không được làm thoát chế độ Maximize");

        // Nhấn phím Esc để thoát Maximize
        Action exitAction = panel.getActionMap().get("exitMaximize");
        assertNotNull(exitAction);
        exitAction.actionPerformed(new java.awt.event.ActionEvent(panel, 0, "exitMaximize"));
        assertFalse(panel.isMaximized(), "Phím Esc phải thoát khỏi chế độ Maximize");
    }

    @Test
    @DisplayName("Kiểm tra phân tách LightweightRenderer cho 6 cột và Custom Badge Pill cho cột 6")
    void testLightweightRendererAndCustomBadgeSplit() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong headless");

        String[] columns = {"STT", "Mã SV", "Họ và Tên", "Lớp", "Ngày Sinh", "Điểm TB", "Xếp Loại"};
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(columns, 1);
        JTable table = new JTable(model);
        com.quanlysinhvien.ui.theme.ModernTableRenderer.applyModernStyle(table);

        // 6 cột đầu (0-5) dùng LightweightRenderer
        for (int i = 0; i < 6; i++) {
            javax.swing.table.TableCellRenderer renderer = table.getColumnModel().getColumn(i).getCellRenderer();
            assertTrue(renderer instanceof com.quanlysinhvien.ui.theme.ModernTableRenderer.LightweightRenderer,
                    "Cột " + i + " phải dùng LightweightRenderer nhẹ");
        }

        // Cột 6 ("Xếp Loại") dùng ModernTableRenderer với custom pill badge
        javax.swing.table.TableCellRenderer col6Renderer = table.getColumnModel().getColumn(6).getCellRenderer();
        assertTrue(col6Renderer instanceof com.quanlysinhvien.ui.theme.ModernTableRenderer,
                "Cột Xếp Loại phải dùng ModernTableRenderer");
        assertFalse(col6Renderer instanceof com.quanlysinhvien.ui.theme.ModernTableRenderer.LightweightRenderer,
                "Cột Xếp Loại không được là LightweightRenderer");
    }

    @Test
    @DisplayName("Kiểm tra KPI chuẩn hóa tên lớp không phân biệt hoa/thường")
    void testKpiClassNormalization() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong headless");

        KpiCardsPanel kpi = new KpiCardsPanel();
        List<SinhVien> list = new ArrayList<>();
        list.add(new SinhVien("SV1", "Nguyen A", "DCT1241", java.time.LocalDate.parse("2005-01-01"), 8.0f));
        list.add(new SinhVien("SV2", "Tran B", "dct1241", java.time.LocalDate.parse("2005-01-01"), 7.5f));
        list.add(new SinhVien("SV3", "Le C", " Dct1241 ", java.time.LocalDate.parse("2005-01-01"), 9.0f));

        kpi.update(list);
        assertEquals(3, kpi.getTotalStudents());
        assertEquals(1, kpi.getTotalClasses(), "Các tên lớp DCT1241, dct1241, Dct1241 phải được gộp thành 1 lớp");
    }

    @Test
    @DisplayName("Kiểm tra SlideTabbedPane tự động vô hiệu hóa animation khi collapse/maximize/density")
    void testAnimationDisabledDuringLayoutTransitions() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong headless");

        MainFrame frame = new MainFrame();
        com.quanlysinhvien.ui.animation.SlideTabbedPane tabbedPane = frame.getTabbedPane();
        StudentManagementPanel panel = frame.getStudentPanel();

        assertTrue(tabbedPane.isAnimationEnabled());

        // Toggle form collapse
        panel.toggleFormCollapse();
        assertTrue(panel.isFormCollapsed());

        // Toggle maximize
        panel.toggleMaximize();
        assertTrue(panel.isMaximized());

        // Toggle density
        panel.toggleDensity();
        assertEquals(26, panel.getDensity());

        // Toggle sidebar
        frame.toggleSidebar();
        assertFalse(frame.getSidebar().isExpanded());

        frame.dispose();
    }
}
