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

        // Chọn dòng đầu -> DatePicker đồng bộ theo txtNgaySinh
        panel.getTable().setRowSelectionInterval(0, 0);
        String text = panel.getTxtNgaySinh().getText().trim();
        assertFalse(text.isEmpty(), "Chọn dòng phải đổ ngày sinh lên form");
        assertEquals(LocalDate.parse(text), panel.getDatePickerNgaySinh().getDate(),
                "DatePicker phải khớp ô Ngày sinh");
    }
}
