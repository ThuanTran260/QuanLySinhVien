package com.quanlysinhvien;

import com.quanlysinhvien.ui.StudentManagementPanel;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class BangDiemUiTest {

    @Test
    @DisplayName("Panel có nút Bảng điểm + DatePicker đồng bộ với ô Ngày sinh")
    void testBangDiemWiring() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong môi trường headless");

        StudentManagementPanel panel = new StudentManagementPanel();
        assertNotNull(panel.getBtnBangDiem(), "Phải có nút Bảng điểm trên toolbar");
        assertNotNull(panel.getDatePickerNgaySinh(), "Phải có DatePicker ngày sinh");

        // Kiểm tra txtNgaySinh ẩn khỏi giao diện để chỉ hiển thị 1 ô chọn ngày
        assertFalse(panel.getTxtNgaySinh().isVisible(), "txtNgaySinh phải được ẩn khỏi giao diện");

        // Chọn dòng đầu -> DatePicker đồng bộ theo txtNgaySinh
        panel.getTable().setRowSelectionInterval(0, 0);
        String text = panel.getTxtNgaySinh().getText().trim();
        assertFalse(text.isEmpty(), "Chọn dòng phải đổ ngày sinh lên form");
        assertEquals(LocalDate.parse(text), panel.getDatePickerNgaySinh().getDate(),
                "DatePicker phải khớp ô Ngày sinh");

        // 2-way sync: Sửa trên DatePicker -> Tự cập nhật txtNgaySinh
        panel.getDatePickerNgaySinh().setDate(LocalDate.of(2006, 11, 20));
        assertEquals("2006-11-20", panel.getTxtNgaySinh().getText(),
                "Sửa trên DatePicker phải tự đồng bộ sang txtNgaySinh");

        // 2-way sync: Sửa trên txtNgaySinh -> Tự cập nhật DatePicker
        panel.getTxtNgaySinh().setText("2005-08-15");
        assertEquals(LocalDate.of(2005, 8, 15), panel.getDatePickerNgaySinh().getDate(),
                "Sửa trên txtNgaySinh phải tự đồng bộ sang DatePicker");

        // 2-way sync: Xóa trên DatePicker -> txtNgaySinh trống
        panel.getDatePickerNgaySinh().clear();
        assertEquals("", panel.getTxtNgaySinh().getText(),
                "Clear DatePicker phải xóa trống txtNgaySinh");

        // 2-way sync: Xóa trên txtNgaySinh -> DatePicker trống
        panel.getTxtNgaySinh().setText("2007-01-01");
        assertEquals(LocalDate.of(2007, 1, 1), panel.getDatePickerNgaySinh().getDate());
        panel.getTxtNgaySinh().setText("");
        assertNull(panel.getDatePickerNgaySinh().getDate(),
                "Clear txtNgaySinh phải xóa DatePicker");
    }

    @Test
    @DisplayName("Giao diện Bảng Điểm: nút nổi bật form trái, khóa ô DiemTB, menu chuột phải, vị trí toolbar trong 450px")
    void testProminentBangDiemAccessUI() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong môi trường headless");

        StudentManagementPanel panel = new StudentManagementPanel();

        // 1. Nút btnOpenBangDiem trên form trái (dưới cụm 4 nút CRUD)
        assertNotNull(panel.getBtnOpenBangDiem(), "Phải có nút Bảng Điểm Theo Môn trên form trái");
        assertEquals("📊 Bảng Điểm Theo Môn", panel.getBtnOpenBangDiem().getText());
        assertEquals("Xem/sửa bảng điểm theo môn của sinh viên đang chọn", panel.getBtnOpenBangDiem().getToolTipText());

        // 2. Ô txtDiemTB không cho phép chỉnh sửa trực tiếp, có tooltip hướng dẫn rõ ràng
        assertFalse(panel.getTxtDiemTB().isEditable(), "Ô Điểm TB phải bị khóa không cho sửa trực tiếp");
        assertNotNull(panel.getTxtDiemTB().getToolTipText());
        assertTrue(panel.getTxtDiemTB().getToolTipText().contains("Điểm TB được tự động tính theo trọng số tín chỉ từ Bảng điểm theo môn"));

        // Làm mới form -> txtDiemTB vẫn tiếp tục bị khóa
        panel.getBtnClear().doClick();
        assertFalse(panel.getTxtDiemTB().isEditable(), "Ô Điểm TB vẫn phải bị khóa sau khi làm mới form");

        // 3. Context Menu chuột phải trên JTable
        assertNotNull(panel.getTableContextMenu(), "Bảng phải có menu ngữ cảnh chuột phải");
        assertEquals(1, panel.getTableContextMenu().getComponentCount());
        assertTrue(panel.getTableContextMenu().getComponent(0) instanceof javax.swing.JMenuItem);
        javax.swing.JMenuItem mi = (javax.swing.JMenuItem) panel.getTableContextMenu().getComponent(0);
        assertEquals("📊 Xem bảng điểm chi tiết môn học...", mi.getText());

        // 4. Kiểm tra vị trí btnBangDiem trên toolbar: liền kề btnStatistic và nằm trong phạm vi 450px đầu
        assertNotNull(panel.getBtnStatistic());
        assertNotNull(panel.getBtnBangDiem());
        java.awt.Container tb = panel.getBtnBangDiem().getParent();
        assertNotNull(tb, "btnBangDiem phải nằm trong toolbar");

        int idxStat = -1;
        int idxBangDiem = -1;
        int runningWidth = 0;
        for (int i = 0; i < tb.getComponentCount(); i++) {
            java.awt.Component c = tb.getComponent(i);
            if (c == panel.getBtnStatistic()) idxStat = i;
            if (c == panel.getBtnBangDiem()) {
                idxBangDiem = i;
                runningWidth += c.getPreferredSize().width;
                break;
            }
            runningWidth += c.getPreferredSize().width;
        }

        assertTrue(idxStat >= 0 && idxBangDiem >= 0, "Cả hai nút phải có mặt trong toolbar");
        assertEquals(2, idxBangDiem - idxStat, "btnBangDiem phải liền kề btnStatistic (cách 1 khoảng đệm rigid area)");
        int btnBangDiemStartX = runningWidth - panel.getBtnBangDiem().getPreferredSize().width;
        assertTrue(btnBangDiemStartX <= 450, "Nút Bảng điểm phải bắt đầu trong phạm vi 450px đầu toolbar để không bị cuộn che, thực tế bắt đầu tại: " + btnBangDiemStartX + "px");

        // 5. Kiểm tra đăng ký sự kiện: ActionListeners trên btnOpenBangDiem, MenuItem và MouseListener trên JTable
        assertTrue(panel.getBtnOpenBangDiem().getActionListeners().length > 0,
                "btnOpenBangDiem phải được gắn ActionListener");
        assertTrue(mi.getActionListeners().length > 0,
                "MenuItem trong context menu phải được gắn ActionListener");
        assertTrue(panel.getTable().getMouseListeners().length > 0,
                "Bảng sinh viên phải có MouseListener cho double-click và menu chuột phải");
    }
}
