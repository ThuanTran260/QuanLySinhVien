package com.quanlysinhvien.model;

/**
 * Lớp nghiệp vụ tính toán số học cơ bản cho Bài 2.
 * Hỗ trợ 4 phép tính: +, -, *, /; kiểm tra tính hợp lệ và ngăn chặn chia cho 0.
 */
public class Calculator {

    private Calculator() {
        // Utility class
    }

    /**
     * Thực hiện phép tính giữa 2 số thực.
     *
     * @param a        số thứ nhất
     * @param b        số thứ hai
     * @param operator một trong các ký tự: '+', '-', '*', '/'
     * @return kết quả phép tính
     * @throws ArithmeticException      khi chia cho 0
     * @throws IllegalArgumentException khi đầu vào là NaN, Infinity hoặc toán tử không hợp lệ
     */
    public static double compute(double a, double b, char operator) {
        if (Double.isNaN(a) || Double.isInfinite(a)) {
            throw new IllegalArgumentException("Số a không phải là số thực hợp lệ!");
        }
        if (Double.isNaN(b) || Double.isInfinite(b)) {
            throw new IllegalArgumentException("Số b không phải là số thực hợp lệ!");
        }

        switch (operator) {
            case '+':
                return a + b;
            case '-':
                return a - b;
            case '*':
                return a * b;
            case '/':
                if (b == 0.0) {
                    throw new ArithmeticException("Không được phép chia cho 0!");
                }
                return a / b;
            default:
                throw new IllegalArgumentException("Phép toán không hợp lệ: '" + operator + "'. Chỉ hỗ trợ +, -, *, /");
        }
    }
}
