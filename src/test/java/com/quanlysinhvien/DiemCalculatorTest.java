package com.quanlysinhvien;

import com.quanlysinhvien.model.DiemCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class DiemCalculatorTest {

    @Test
    @DisplayName("Công thức 40/10/50 và làm tròn 2 chữ số")
    void testDiemMonFormula() {
        // 8*0.4 + 9*0.1 + 7*0.5 = 3.2 + 0.9 + 3.5 = 7.6
        assertEquals(7.6, DiemCalculator.diemMon(8, 9, 7), 0.001);
        assertEquals(10.0, DiemCalculator.diemMon(10, 10, 10), 0.001);
        assertEquals(0.0, DiemCalculator.diemMon(0, 0, 0), 0.001);
    }

    @Test
    @DisplayName("TB tích lũy = AVG, rỗng trả 0")
    void testDiemTB() {
        assertEquals(0.0, DiemCalculator.diemTB(Collections.emptyList()), 0.001);
        assertEquals(8.0, DiemCalculator.diemTB(Arrays.asList(8.0, 8.0, 8.0)), 0.001);
        assertEquals(7.5, DiemCalculator.diemTB(Arrays.asList(7.0, 8.0)), 0.001);
    }

    @Test
    @DisplayName("Từ chối điểm ngoài 0-10, NaN, rỗng, chữ")
    void testRejectInvalid() {
        assertThrows(IllegalArgumentException.class, () -> DiemCalculator.diemMon(-1, 8, 8));
        assertThrows(IllegalArgumentException.class, () -> DiemCalculator.diemMon(8, 11, 8));
        assertThrows(IllegalArgumentException.class, () -> DiemCalculator.diemMon(Double.NaN, 8, 8));
        assertThrows(IllegalArgumentException.class, () -> DiemCalculator.parseDiem("", "Điểm báo cáo"));
        assertThrows(IllegalArgumentException.class, () -> DiemCalculator.parseDiem("abc", "Điểm cuối kỳ"));
        // Chấp nhận dấu phẩy
        assertEquals(8.5f, DiemCalculator.parseDiem("8,5", "Điểm báo cáo"), 0.001f);
    }
}
