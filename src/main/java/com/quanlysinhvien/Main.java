package com.quanlysinhvien;

import com.quanlysinhvien.dao.DatabaseConnection;
import com.quanlysinhvien.ui.LoginForm;

import javax.swing.*;

/**
 * Lớp khởi chạy chính của chương trình.
 * Thiết lập Nimbus Look and Feel và mở Form Đăng Nhập (Bài 1).
 */
public class Main {
    public static void main(String[] args) {
        // Thiết lập giao diện Nimbus Look and Feel có sẵn trong Swing
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Không thể thiết lập Nimbus Look and Feel: " + e.getMessage());
        }

        // Tự động kiểm tra và khởi tạo CSDL trong luồng nền
        new Thread(DatabaseConnection::initializeDatabase).start();

        // Mở Form Đăng nhập trên Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            LoginForm loginForm = new LoginForm();
            loginForm.setVisible(true);
        });
    }
}
