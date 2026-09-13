package com.quanlysinhvien.ui;

import com.quanlysinhvien.ui.theme.ModernButton;
import com.quanlysinhvien.ui.theme.ModernCardPanel;
import com.quanlysinhvien.ui.theme.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Bài 1: Form đăng nhập hệ thống hiện đại theo phong cách Stripe.
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
        setSize(450, 370);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(UITheme.CANVAS_BG);

        // Root container with canvas background and padding
        JPanel rootPanel = new JPanel(new GridBagLayout());
        rootPanel.setBackground(UITheme.CANVAS_BG);
        rootPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Modern Card Container
        ModernCardPanel cardPanel = new ModernCardPanel(new BorderLayout(15, 15), 25);
        cardPanel.setPreferredSize(new Dimension(390, 290));

        // Header Section
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("ĐĂNG NHẬP HỆ THỐNG", SwingConstants.CENTER);
        lblTitle.setFont(UITheme.FONT_TITLE_LARGE);
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitle = new JLabel("Quản Lý Sinh Viên & Tiện Ích", SwingConstants.CENTER);
        lblSubtitle.setFont(UITheme.FONT_REGULAR);
        lblSubtitle.setForeground(UITheme.TEXT_SECONDARY);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(lblTitle);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        headerPanel.add(lblSubtitle);

        cardPanel.add(headerPanel, BorderLayout.NORTH);

        // Form Inputs Section
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.35;
        JLabel lblUser = new JLabel("Tên đăng nhập:");
        lblUser.setFont(UITheme.FONT_BOLD);
        lblUser.setForeground(UITheme.TEXT_PRIMARY);
        formPanel.add(lblUser, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.65;
        txtUsername = new JTextField("admin");
        UITheme.styleTextField(txtUsername);
        formPanel.add(txtUsername, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.35;
        JLabel lblPass = new JLabel("Mật khẩu:");
        lblPass.setFont(UITheme.FONT_BOLD);
        lblPass.setForeground(UITheme.TEXT_PRIMARY);
        formPanel.add(lblPass, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.65;
        txtPassword = new JPasswordField("123");
        UITheme.styleTextField(txtPassword);
        formPanel.add(txtPassword, gbc);

        cardPanel.add(formPanel, BorderLayout.CENTER);

        // Buttons Section
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        buttonPanel.setOpaque(false);

        btnLogin = new ModernButton("Đăng nhập", UITheme.PRIMARY, UITheme.PRIMARY_HOVER, Color.WHITE);
        btnLogin.setPreferredSize(new Dimension(130, 36));

        btnExit = new ModernButton("Thoát", UITheme.NEUTRAL_BTN_BG, UITheme.NEUTRAL_BTN_HOVER, UITheme.NEUTRAL_BTN_TEXT);
        ((ModernButton) btnExit).setBorderColor(UITheme.NEUTRAL_BTN_BORDER);
        btnExit.setPreferredSize(new Dimension(100, 36));

        buttonPanel.add(btnLogin);
        buttonPanel.add(btnExit);
        cardPanel.add(buttonPanel, BorderLayout.SOUTH);

        rootPanel.add(cardPanel);
        add(rootPanel);

        // Event Handlers
        btnLogin.addActionListener(e -> performLogin());
        btnExit.addActionListener(e -> System.exit(0));

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

    // Các hàm getter phục vụ kiểm thử tự động
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
