package com.quanlysinhvien;

import com.quanlysinhvien.ui.CalculatorPanel;
import com.quanlysinhvien.ui.LoginForm;
import com.quanlysinhvien.ui.MainFrame;
import com.quanlysinhvien.ui.StudentManagementPanel;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.*;

public class UIComponentTest {

    @Test
    @DisplayName("Kiểm tra xác thực tài khoản LoginForm")
    void testLoginAuthentication() {
        assertTrue(LoginForm.checkCredentials("admin", "123"));
        assertTrue(LoginForm.checkCredentials(" admin ", " 123 "));
        assertFalse(LoginForm.checkCredentials("admin", "wrong"));
        assertFalse(LoginForm.checkCredentials("wrong", "123"));
        assertFalse(LoginForm.checkCredentials("", ""));
        assertFalse(LoginForm.checkCredentials(null, null));
    }

    @Test
    @DisplayName("Kiểm tra tương tác tính toán trực quan trên CalculatorPanel")
    void testCalculatorPanelInteractions() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua kiểm tra GUI trong môi trường headless");

        CalculatorPanel calc = new CalculatorPanel();
        assertNotNull(calc);

        // Phép tính hợp lệ: 15.5 + 4.5 = 20
        calc.setInputs("15.5", "4.5");
        calc.triggerCalculate('+');
        assertTrue(calc.getResultText().contains("20"), "Kết quả phép tính phải chứa 20");

        // Phép nhân: 10 * 2.5 = 25
        calc.setInputs("10", "2.5");
        calc.triggerCalculate('*');
        assertTrue(calc.getResultText().contains("25"), "Kết quả phép tính phải chứa 25");

        // Phép chia hợp lệ: 10 / 2 = 5
        calc.setInputs("10", "2");
        calc.triggerCalculate('/');
        assertTrue(calc.getResultText().contains("5"), "Kết quả phép tính phải chứa 5");
    }

    @Test
    @DisplayName("Kiểm tra JTable và khóa ô MaSV khi chọn dòng trên StudentManagementPanel")
    void testStudentManagementPanelInteractions() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua kiểm tra GUI trong môi trường headless");

        StudentManagementPanel panel = new StudentManagementPanel();
        assertNotNull(panel);

        JTable table = panel.getTable();
        assertNotNull(table);
        assertEquals(7, table.getColumnCount(), "Bảng phải có đúng 7 cột");
        assertEquals("Mã SV", table.getColumnName(1));
        assertEquals("Họ và Tên", table.getColumnName(2));
        assertEquals("Lớp", table.getColumnName(3));
        assertEquals("Ngày Sinh", table.getColumnName(4));
        assertEquals("Điểm TB", table.getColumnName(5));
        assertEquals("Xếp Loại", table.getColumnName(6));

        assertTrue(table.getRowCount() >= 10, "Bảng phải nạp ít nhất 10 bản ghi mẫu");

        // Mô phỏng chọn dòng đầu tiên trên bảng
        table.setRowSelectionInterval(0, 0);
        assertFalse(panel.getTxtMaSV().isEditable(), "Ô Mã SV phải bị khóa không cho sửa khóa chính");
        assertFalse(panel.getTxtMaSV().getText().trim().isEmpty());

        // Mô phỏng bấm nút Làm mới form
        panel.getBtnClear().doClick();
        assertTrue(panel.getTxtMaSV().isEditable(), "Ô Mã SV phải mở khóa sau khi làm mới form");
        assertEquals("", panel.getTxtMaSV().getText(), "Ô Mã SV phải được xóa trống");
    }

    @Test
    @DisplayName("Kiểm tra khởi tạo MainFrame tích hợp 2 Tab chính")
    void testMainFrameTabs() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua kiểm tra GUI trong môi trường headless");

        MainFrame mainFrame = new MainFrame();
        assertNotNull(mainFrame);

        // Tìm JTabbedPane trong MainFrame
        JTabbedPane tabbedPane = null;
        for (java.awt.Component comp : mainFrame.getContentPane().getComponents()) {
            if (comp instanceof JTabbedPane) {
                tabbedPane = (JTabbedPane) comp;
                break;
            }
        }

        assertNotNull(tabbedPane, "MainFrame phải chứa JTabbedPane");
        assertEquals(2, tabbedPane.getTabCount(), "Phải có đúng 2 tab");
        assertTrue(tabbedPane.getTitleAt(0).contains("Quản Lý Sinh Viên"));
        assertTrue(tabbedPane.getTitleAt(1).contains("Máy Tính Cơ Bản"));

        mainFrame.dispose();
    }
}
