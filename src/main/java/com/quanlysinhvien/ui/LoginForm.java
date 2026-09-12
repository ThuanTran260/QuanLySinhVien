package com.quanlysinhvien.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Bài 1: Form đăng nhập hệ thống.
 * Tài khoản mặc định: username = admin, password = 123.
 */
public class LoginForm extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnExit;

    public LoginForm() {
        initUI();
    }

    private void initUI() {
        setTitle("Đăng nhập hệ thống - Quản lý sinh viên");
        setSize(420, 260);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Panel chính
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Tiêu đề
        JLabel lblTitle = new JLabel("HỆ THỐNG QUẢN LÝ SINH VIÊN", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(24, 90, 157));
        mainPanel.add(lblTitle, BorderLayout.NORTH);

        // Form nhập liệu
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Tên đăng nhập
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        JLabel lblUser = new JLabel("Tên đăng nhập:");
        lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(lblUser, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        txtUsername = new JTextField("admin");
        txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(txtUsername, gbc);

        // Mật khẩu
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        JLabel lblPass = new JLabel("Mật khẩu:");
        lblPass.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(lblPass, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        txtPassword = new JPasswordField("123");
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(txtPassword, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Các nút bấm
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        btnLogin = new JButton("Đăng nhập");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnLogin.setBackground(new Color(33, 150, 243));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);

        btnExit = new JButton("Thoát");
        btnExit.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnExit.setFocusPainted(false);

        buttonPanel.add(btnLogin);
        buttonPanel.add(btnExit);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Sự kiện
        btnLogin.addActionListener(e -> performLogin());
        btnExit.addActionListener(e -> System.exit(0));

        // Nhấn Enter để đăng nhập
        KeyAdapter enterKeyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin();
                }
            }
        };
        txtUsername.addKeyListener(enterKeyAdapter);
        txtPassword.addKeyListener(enterKeyAdapter);

        // Đặt mặc định nút Đăng nhập khi ấn Enter
        getRootPane().setDefaultButton(btnLogin);
    }

    public static boolean checkCredentials(String username, String password) {
        return "admin".equals(username != null ? username.trim() : "") &&
               "123".equals(password != null ? password.trim() : "");
    }

    private void performLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên đăng nhập!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            txtUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập mật khẩu!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            txtPassword.requestFocus();
            return;
        }

        if (checkCredentials(username, password)) {
            JOptionPane.showMessageDialog(this, "Đăng nhập thành công! Chào mừng Quản trị viên.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            this.dispose();
            SwingUtilities.invokeLater(() -> {
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
            });
        } else {
            JOptionPane.showMessageDialog(this, 
                    "Tên đăng nhập hoặc mật khẩu không chính xác!\n(Tài khoản mặc định: admin / 123)", 
                    "Đăng nhập thất bại", 
                    JOptionPane.ERROR_MESSAGE);
            txtPassword.setText("");
            txtPassword.requestFocus();
        }
    }

    // Các hàm getter phục vụ kiểm thử
    public JTextField getTxtUsername() {
        return txtUsername;
    }

    public JPasswordField getTxtPassword() {
        return txtPassword;
    }

    public JButton getBtnLogin() {
        return btnLogin;
    }
}
