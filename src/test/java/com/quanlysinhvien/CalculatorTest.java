package com.quanlysinhvien;

import com.quanlysinhvien.model.Calculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CalculatorTest {

    @Test
    @DisplayName("Kiểm tra 4 phép tính cơ bản +, -, *, / bằng Calculator.compute")
    void testBasicOperations() {
        assertEquals(15.0, Calculator.compute(10.0, 5.0, '+'), 0.0001);
        assertEquals(5.0, Calculator.compute(10.0, 5.0, '-'), 0.0001);
        assertEquals(50.0, Calculator.compute(10.0, 5.0, '*'), 0.0001);
        assertEquals(2.0, Calculator.compute(10.0, 5.0, '/'), 0.0001);
    }

    @Test
    @DisplayName("Kiểm tra phép tính với số âm và số thập phân")
    void testDecimalAndNegativeNumbers() {
        assertEquals(-2.5, Calculator.compute(-5.0, 2.5, '+'), 0.0001);
        assertEquals(-7.5, Calculator.compute(-5.0, 2.5, '-'), 0.0001);
        assertEquals(-12.5, Calculator.compute(-5.0, 2.5, '*'), 0.0001);
        assertEquals(-2.0, Calculator.compute(-5.0, 2.5, '/'), 0.0001);
    }

    @Test
    @DisplayName("Kiểm tra cảnh báo chia cho 0 phải ném ngoại lệ ArithmeticException")
    void testDivisionByZero() {
        assertThrows(ArithmeticException.class, () -> Calculator.compute(10.0, 0.0, '/'));
        assertThrows(ArithmeticException.class, () -> Calculator.compute(-5.5, 0.0, '/'));
    }

    @Test
    @DisplayName("Kiểm tra toán tử không hợp lệ phải ném ngoại lệ IllegalArgumentException")
    void testInvalidOperator() {
        assertThrows(IllegalArgumentException.class, () -> Calculator.compute(10.0, 5.0, '%'));
        assertThrows(IllegalArgumentException.class, () -> Calculator.compute(10.0, 5.0, '^'));
    }

    @Test
    @DisplayName("Kiểm tra đầu vào NaN hoặc Infinity phải ném IllegalArgumentException")
    void testNaNAndInfinityInputs() {
        assertThrows(IllegalArgumentException.class, () -> Calculator.compute(Double.NaN, 5.0, '+'));
        assertThrows(IllegalArgumentException.class, () -> Calculator.compute(10.0, Double.NaN, '+'));
        assertThrows(IllegalArgumentException.class, () -> Calculator.compute(Double.POSITIVE_INFINITY, 5.0, '*'));
        assertThrows(IllegalArgumentException.class, () -> Calculator.compute(10.0, Double.NEGATIVE_INFINITY, '/'));
    }
}
