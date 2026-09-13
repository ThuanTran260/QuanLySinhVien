package com.quanlysinhvien;

import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.quanlysinhvien.dao.DatabaseConnection;
import com.quanlysinhvien.ui.LoginForm;
import com.quanlysinhvien.ui.theme.UITheme;

import javax.swing.*;

/**
 * Lớp khởi chạy chính của chương trình.
 * Thiết lập FlatLaf (FlatMacLightLaf) Look and Feel hiện đại và mở Form Đăng Nhập (Bài 1).
 */
public class Main {

    /**
     * Cấu hình giao diện hiện đại FlatMacLightLaf với thanh tiêu đề tùy biến và underlined tabs
     */
    public static boolean setupModernLaf() {
        try {
            FlatMacLightLaf.setup();
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);

            // Cấu hình UIManager toàn cục cho các thành phần Swing
            UIManager.put("defaultFont", UITheme.FONT_REGULAR);
            UIManager.put("TabbedPane.tabType", "underlined");
            UIManager.put("TabbedPane.underlineColor", UITheme.PRIMARY);
            UIManager.put("TabbedPane.inactiveUnderlineColor", UITheme.BORDER_COLOR);
            UIManager.put("TabbedPane.selectedForeground", UITheme.PRIMARY);
            UIManager.put("TabbedPane.tabSelectionHeight", 3);
            return true;
        } catch (Exception e) {
            System.err.println("Không thể thiết lập FlatLaf Look and Feel: " + e.getMessage());
            return false;
        }
    }

    public static void main(String[] args) {
        // Thiết lập giao diện FlatLaf FlatMacLightLaf phong cách hiện đại
        setupModernLaf();

        // Tự động kiểm tra và khởi tạo CSDL trong luồng nền
        new Thread(DatabaseConnection::initializeDatabase).start();

        // Mở Form Đăng nhập trên Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            LoginForm loginForm = new LoginForm();
            loginForm.setVisible(true);
        });
    }
}
