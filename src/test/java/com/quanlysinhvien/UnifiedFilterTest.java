package com.quanlysinhvien;

import com.quanlysinhvien.ui.StudentManagementPanel;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.*;

public class UnifiedFilterTest {

    @Test
    @DisplayName("Bộ lọc thống nhất: mặc định tắt, sort + search cộng dồn, refresh đặt lại")
    void testUnifiedFilter() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong môi trường headless");

        StudentManagementPanel panel = new StudentManagementPanel();
        assertNotNull(panel.getBtnFilter());
        assertEquals("Tất cả", panel.getSearchCriteria());
        assertEquals(-1, panel.getSortIndex());
        assertEquals("Bộ lọc", panel.getBtnFilter().getText());

        // Sắp xếp điểm tăng dần trên toàn bộ
        panel.setSortIndex(4);
        panel.triggerSort();
        assertEquals(84, panel.getTable().getRowCount());
        float first = ((Number) panel.getTableModel().getValueAt(0, 5)).floatValue();
        float last = ((Number) panel.getTableModel().getValueAt(83, 5)).floatValue();
        assertTrue(first <= last, "Tăng dần: " + first + " <= " + last);
        assertEquals("Bộ lọc •", panel.getBtnFilter().getText(), "Sort khác mặc định phải hiện dấu chấm");

        // Lọc theo lớp + giữ sắp xếp (cộng dồn, sort không xóa filter như bản cũ)
        panel.setSearchCriteria("Lớp");
        panel.getTxtSearch().setText("DCT1241");
        panel.getBtnSearch().doClick();
        int n = panel.getTable().getRowCount();
        assertTrue(n > 0 && n < 84, "Lọc DCT1241 phải thu hẹp, thực tế: " + n);
        for (int i = 0; i < n; i++) {
            assertTrue(panel.getTableModel().getValueAt(i, 3).toString().contains("DCT1241"));
        }
        float f2 = ((Number) panel.getTableModel().getValueAt(0, 5)).floatValue();
        float l2 = ((Number) panel.getTableModel().getValueAt(n - 1, 5)).floatValue();
        assertTrue(f2 <= l2, "Sau lọc vẫn tăng dần: " + f2 + " <= " + l2);

        // Tìm không dấu qua MySQL + sort giảm dần cộng dồn
        panel.setSearchCriteria("Tất cả");
        panel.setSortIndex(5);
        panel.getTxtSearch().setText("Nguyen");
        panel.getBtnSearch().doClick();
        int m = panel.getTable().getRowCount();
        assertTrue(m > 0, "Tìm 'Nguyen' không dấu phải ra kết quả qua MySQL");
        float g1 = ((Number) panel.getTableModel().getValueAt(0, 5)).floatValue();
        float g2 = ((Number) panel.getTableModel().getValueAt(m - 1, 5)).floatValue();
        assertTrue(g1 >= g2, "Sau lọc vẫn giảm dần: " + g1 + " >= " + g2);

        // Refresh đặt lại toàn bộ
        panel.getBtnRefresh().doClick();
        assertEquals(84, panel.getTable().getRowCount());
        assertEquals("Tất cả", panel.getSearchCriteria());
        assertEquals(-1, panel.getSortIndex());
        assertEquals("Bộ lọc", panel.getBtnFilter().getText());
    }
}
