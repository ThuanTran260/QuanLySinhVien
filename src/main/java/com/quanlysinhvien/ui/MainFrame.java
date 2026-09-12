package com.quanlysinhvien.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Cửa sổ chính của ứng dụng sau khi đăng nhập thành công.
 * Sử dụng JTabbedPane tích hợp:
 * - Tab 1: Quản lý sinh viên (Bài 3 & Phần 1 - Phần 8)
 * - Tab 2: Máy tính cơ bản (Bài 2)
 */
public class MainFrame extends JFrame {
    private StudentManagementPanel studentPanel;
    private CalculatorPanel calculatorPanel;

    public MainFrame() {
        initUI();
    }

    private void initUI() {
        setTitle("Hệ Thống Quản Lý Sinh Viên & Tiện Ích - Java Swing & MySQL");
        setSize(1050, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Menu bar
        initMenuBar();

        // JTabbedPane chứa 2 chức năng chính
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));

        studentPanel = new StudentManagementPanel();
        calculatorPanel = new CalculatorPanel();

        tabbedPane.addTab(" Quản Lý Sinh Viên ", studentPanel);
        tabbedPane.addTab(" Máy Tính Cơ Bản ", calculatorPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Status bar ở dưới
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        JLabel lblStatus = new JLabel("Người dùng: admin | Kết nối: MySQL Server (localhost:3306) | Trạng thái: Sẵn sàng");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblStatus.setForeground(new Color(60, 60, 60));
        statusBar.add(lblStatus, BorderLayout.WEST);

        JLabel lblVersion = new JLabel("Phiên bản: 1.0.0");
        lblVersion.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusBar.add(lblVersion, BorderLayout.EAST);

        add(statusBar, BorderLayout.SOUTH);
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Menu Hệ thống
        JMenu menuSystem = new JMenu("Hệ thống");
        menuSystem.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JMenuItem itemLogout = new JMenuItem("Đăng xuất");
        itemLogout.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        itemLogout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc chắn muốn đăng xuất tài khoản hiện tại?",
                    "Xác nhận đăng xuất",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                this.dispose();
                SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
            }
        });

        JMenuItem itemExit = new JMenuItem("Thoát ứng dụng");
        itemExit.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        itemExit.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có muốn đóng toàn bộ ứng dụng?",
                    "Xác nhận thoát",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });

        menuSystem.add(itemLogout);
        menuSystem.addSeparator();
        menuSystem.add(itemExit);

        // Menu Trợ giúp
        JMenu menuHelp = new JMenu("Trợ giúp");
        menuHelp.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JMenuItem itemGuide = new JMenuItem("Hướng dẫn sử dụng");
        itemGuide.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        itemGuide.addActionListener(e -> {
            String msg = "HƯỚNG DẪN SỬ DỤNG HỆ THỐNG:\n\n" +
                    "1. Tab 'Quản Lý Sinh Viên':\n" +
                    "   - Thêm sinh viên: Điền đủ thông tin, bấm 'Thêm mới'.\n" +
                    "   - Sửa sinh viên: Chọn 1 dòng trên bảng, chỉnh sửa họ tên/lớp/ngày sinh/điểm, bấm 'Cập nhật'.\n" +
                    "   - Xóa sinh viên: Chọn 1 dòng trên bảng, bấm 'Xóa sinh viên'.\n" +
                    "   - Tìm kiếm: Chọn tiêu chí (Mã SV, Họ tên, Lớp), nhập từ khóa rồi bấm 'Tìm kiếm'. Bấm 'Tất cả' để xem lại toàn bộ.\n" +
                    "   - Sắp xếp: Chọn tiêu chí sắp xếp rồi bấm 'Sắp xếp' (hoặc click trực tiếp tiêu đề cột).\n" +
                    "   - Thống kê: Bấm nút 'Thống kê' để xem tổng số SV, điểm TB theo lớp và thủ khoa.\n" +
                    "   - Xuất / Nạp file: Dùng 'Xuất Text File' và 'Nạp từ File' để sao lưu dữ liệu.\n\n" +
                    "2. Tab 'Máy Tính Cơ Bản':\n" +
                    "   - Nhập số a, b rồi bấm các phép tính +, -, *, /.\n" +
                    "   - Hệ thống tự động kiểm tra số hợp lệ và ngăn chia cho 0.";
            JTextArea area = new JTextArea(msg, 16, 42);
            area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            area.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(area), "Hướng dẫn", JOptionPane.INFORMATION_MESSAGE);
        });

        JMenuItem itemAbout = new JMenuItem("Giới thiệu ứng dụng");
        itemAbout.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        itemAbout.addActionListener(e -> {
            String about = "ỨNG DỤNG QUẢN LÝ SINH VIÊN\n" +
                    "Môn học: Lập trình mạng / Ứng dụng Java\n" +
                    "Kiến trúc: 3 Tầng tối giản (Model - DAO - UI)\n" +
                    "Giao diện: Java Swing (Nimbus Look and Feel)\n" +
                    "Cơ sở dữ liệu: MySQL 8.0 qua JDBC Pure\n" +
                    "Bản quyền: 2026 - Mọi quyền được bảo lưu.";
            JOptionPane.showMessageDialog(this, about, "Giới thiệu", JOptionPane.INFORMATION_MESSAGE);
        });

        menuHelp.add(itemGuide);
        menuHelp.add(itemAbout);

        menuBar.add(menuSystem);
        menuBar.add(menuHelp);

        setJMenuBar(menuBar);
    }
}
