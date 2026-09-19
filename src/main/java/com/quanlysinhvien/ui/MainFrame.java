package com.quanlysinhvien.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.quanlysinhvien.ui.animation.SlideTabbedPane;
import com.quanlysinhvien.ui.components.ModernSidebar;
import com.quanlysinhvien.ui.components.TopHeaderBar;
import com.quanlysinhvien.ui.theme.UITheme;

import javax.swing.*;
import java.awt.*;

/**
 * Cửa sổ chính của ứng dụng sau khi đăng nhập thành công:
 * - Bố cục Dashboard hiện đại:
 *   + WEST: ModernSidebar thu gọn linh hoạt (210px ↔ 64px)
 *   + NORTH: TopHeaderBar (56px) với Breadcrumb, DB badge async và avatar
 *   + CENTER: SlideTabbedPane (2 tab: Quản Lý Sinh Viên & Máy Tính Cơ Bản)
 *   + SOUTH: StatusBar hiện đại ở chân trang
 * - Đảm bảo tabbedPane là direct child của getContentPane() để tương thích 100% với UIComponentTest
 */
public class MainFrame extends JFrame {
    private ModernSidebar sidebar;
    private TopHeaderBar headerBar;
    private StudentManagementPanel studentPanel;
    private CalculatorPanel calculatorPanel;
    private SlideTabbedPane tabbedPane;
    private JPanel statusBar;
    private JMenuItem itemReseed;

    public MainFrame() {
        initUI();
    }

    private void initUI() {
        setTitle("Hệ Thống Quản Lý Sinh Viên & Tiện Ích - Java Swing & MySQL");
        setSize(1180, 780);
        setMinimumSize(new Dimension(920, 620));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(UITheme.CANVAS_BG);

        // Giữ thanh menu ở hàng riêng bên dưới thanh tiêu đề phẳng (cấu hình trước khi initMenuBar)
        getRootPane().putClientProperty(FlatClientProperties.MENU_BAR_EMBEDDED, false);
        getRootPane().putClientProperty("JRootPane.menuBarEmbedded", false);

        // 1. Menu bar
        initMenuBar();

        // 2. Thiết lập MainFrameLayout để toàn bộ 4 khối (sidebar, headerBar, tabbedPane, statusBar)
        // đều là component trực tiếp của getContentPane(), bảo đảm testMainFrameTabs() pass 100%
        getContentPane().setLayout(new MainFrameLayout());

        // 3. Sidebar bên trái (210px ↔ 64px, Instant, không timer)
        sidebar = new ModernSidebar();
        sidebar.setToggleListener(expanded -> toggleSidebar());
        getContentPane().add(sidebar, "WEST");

        // 4. TopHeaderBar ở đỉnh vùng nội dung (56px)
        headerBar = new TopHeaderBar();
        headerBar.getBtnLogout().addActionListener(e -> doLogout());
        getContentPane().add(headerBar, "NORTH");

        // 5. SlideTabbedPane chứa 2 chức năng chính với hiệu ứng Slide & Fade mượt mà
        tabbedPane = new SlideTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BOLD);

        // Cấu hình phong cách Underlined Tabs hiện đại của FlatLaf
        tabbedPane.putClientProperty("JTabbedPane.tabType", "underlined");
        tabbedPane.putClientProperty("JTabbedPane.underlineColor", UITheme.PRIMARY);
        tabbedPane.putClientProperty("JTabbedPane.hasFullBorder", false);
        tabbedPane.putClientProperty("JTabbedPane.tabInsets", new Insets(10, 20, 10, 20));
        tabbedPane.putClientProperty(FlatClientProperties.STYLE,
                "underlineColor: #635BFF; inactiveUnderlineColor: #E2E8F0; selectedForeground: #635BFF; tabSelectionHeight: 3");

        studentPanel = new StudentManagementPanel();
        calculatorPanel = new CalculatorPanel();

        tabbedPane.addTab(" Quản Lý Sinh Viên ", studentPanel);
        tabbedPane.addTab(" Máy Tính Cơ Bản ", calculatorPanel);

        // tabbedPane add trực tiếp vào getContentPane()
        getContentPane().add(tabbedPane, "CENTER");

        // Đồng bộ chuyển tab từ tabbedPane sang Sidebar và HeaderBar
        tabbedPane.addChangeListener(e -> {
            int sel = tabbedPane.getSelectedIndex();
            if (sel == 0) {
                sidebar.setActive(ModernSidebar.MenuId.SINH_VIEN);
                headerBar.updateBreadcrumb("Quản Lý Sinh Viên");
            } else if (sel == 1) {
                sidebar.setActive(ModernSidebar.MenuId.MAY_TINH);
                headerBar.updateBreadcrumb("Máy Tính Cơ Bản");
            }
        });

        // Đấu nối điều hướng từ Sidebar sang MainFrame
        sidebar.setNavigationListener(menuId -> {
            switch (menuId) {
                case SINH_VIEN:
                    tabbedPane.setSelectedIndex(0);
                    headerBar.updateBreadcrumb("Quản Lý Sinh Viên");
                    break;
                case MAY_TINH:
                    tabbedPane.setSelectedIndex(1);
                    headerBar.updateBreadcrumb("Máy Tính Cơ Bản");
                    break;
                case THONG_KE:
                    studentPanel.showStatisticDialog();
                    sidebar.setActive(tabbedPane.getSelectedIndex() == 0 ?
                            ModernSidebar.MenuId.SINH_VIEN : ModernSidebar.MenuId.MAY_TINH);
                    break;
                case GIOI_THIEU:
                    showAboutDialog();
                    sidebar.setActive(tabbedPane.getSelectedIndex() == 0 ?
                            ModernSidebar.MenuId.SINH_VIEN : ModernSidebar.MenuId.MAY_TINH);
                    break;
            }
        });

        // 6. Status bar hiện đại ở chân trang
        statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(Color.WHITE);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));

        JLabel lblStatus = new JLabel("Người dùng: admin | Phiên làm việc: Sẵn sàng | Nền tảng: Java Swing Modern Dashboard");
        lblStatus.setFont(UITheme.FONT_SMALL);
        lblStatus.setForeground(UITheme.TEXT_SECONDARY);
        statusBar.add(lblStatus, BorderLayout.WEST);

        JLabel lblVersion = new JLabel("Phiên bản: 1.0.0 (Smart Pathshala Dashboard Theme)");
        lblVersion.setFont(UITheme.FONT_SMALL);
        lblVersion.setForeground(UITheme.TEXT_MUTED);
        statusBar.add(lblVersion, BorderLayout.EAST);

        getContentPane().add(statusBar, "SOUTH");
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(Color.WHITE);
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR));

        // Menu Hệ thống
        JMenu menuSystem = new JMenu("Hệ thống");
        menuSystem.setFont(UITheme.FONT_REGULAR);
        menuSystem.setForeground(UITheme.TEXT_PRIMARY);

        itemReseed = new JMenuItem("🔄 Tạo lại dữ liệu điểm đa dạng (Xuất sắc - Giỏi - Khá - TB - Yếu)");
        itemReseed.setFont(UITheme.FONT_REGULAR);
        itemReseed.addActionListener(e -> doReseedDiverseGrades());

        JMenuItem itemLogout = new JMenuItem("Đăng xuất");
        itemLogout.setFont(UITheme.FONT_REGULAR);
        itemLogout.addActionListener(e -> doLogout());

        JMenuItem itemExit = new JMenuItem("Thoát ứng dụng");
        itemExit.setFont(UITheme.FONT_REGULAR);
        itemExit.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có muốn đóng toàn bộ ứng dụng?",
                    "Xác nhận thoát",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });

        menuSystem.add(itemReseed);
        menuSystem.addSeparator();
        menuSystem.add(itemLogout);
        menuSystem.addSeparator();
        menuSystem.add(itemExit);

        // Menu Trợ giúp
        JMenu menuHelp = new JMenu("Trợ giúp");
        menuHelp.setFont(UITheme.FONT_REGULAR);
        menuHelp.setForeground(UITheme.TEXT_PRIMARY);

        JMenuItem itemGuide = new JMenuItem("Hướng dẫn sử dụng");
        itemGuide.setFont(UITheme.FONT_REGULAR);
        itemGuide.addActionListener(e -> {
            String msg = "HƯỚNG DẪN SỬ DỤNG HỆ THỐNG:\n\n" +
                    "1. Tab 'Quản Lý Sinh Viên':\n" +
                    "   - Thêm sinh viên: Điền đủ thông tin, bấm 'Thêm mới'.\n" +
                    "   - Sửa sinh viên: Chọn 1 dòng trên bảng, chỉnh sửa họ tên/lớp/ngày sinh/điểm, bấm 'Cập nhật'.\n" +
                    "   - Xóa sinh viên: Chọn 1 dòng trên bảng, bấm 'Xóa sinh viên'.\n" +
                    "   - Tìm kiếm: Chọn tiêu chí (Mã SV, Họ tên, Lớp), nhập từ khóa rồi bấm 'Tìm kiếm'. Bấm 'Tất cả' để xem lại toàn bộ.\n" +
                    "   - Sắp xếp: Chọn tiêu chí sắp xếp rồi bấm 'Sắp xếp' (hoặc click trực tiếp tiêu đề cột để sắp xếp số học chính xác).\n" +
                    "   - Thống kê: Bấm nút 'Thống kê' trên Sidebar hoặc Toolbar để xem tổng số SV, điểm TB theo lớp và thủ khoa.\n" +
                    "   - Xuất / Nạp file: Dùng 'Xuất Text File' và 'Nạp từ File' để sao lưu dữ liệu.\n" +
                    "   - Phóng to bảng: Nhấn 'Phóng to [⛶]' hoặc phím F11 để xem 30+ sinh viên cùng lúc; nhấn Esc để thu nhỏ.\n" +
                    "   - Mật độ dòng: Bấm 'Mật độ: 34px/26px' để xem gọn hơn.\n\n" +
                    "2. Tab 'Máy Tính Cơ Bản':\n" +
                    "   - Nhập số a, b rồi bấm các phép tính +, -, *, /.\n" +
                    "   - Hệ thống tự động kiểm tra số hợp lệ và ngăn chia cho 0.";
            JTextArea area = new JTextArea(msg, 16, 42);
            area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            area.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(area), "Hướng dẫn", JOptionPane.INFORMATION_MESSAGE);
        });

        JMenuItem itemAbout = new JMenuItem("Giới thiệu ứng dụng");
        itemAbout.setFont(UITheme.FONT_REGULAR);
        itemAbout.addActionListener(e -> showAboutDialog());

        menuHelp.add(itemGuide);
        menuHelp.add(itemAbout);

        menuBar.add(menuSystem);
        menuBar.add(menuHelp);

        setJMenuBar(menuBar);
    }

    public void doReseedDiverseGrades() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Hành động này sẽ xóa và sinh lại toàn bộ điểm các môn học cho tất cả sinh viên,\n"
                        + "phân bổ theo 5 mức học lực đa dạng:\n"
                        + "• Xuất sắc (~7%): GPA 9.0 - 9.8\n"
                        + "• Giỏi (~23%): GPA 8.0 - 8.9\n"
                        + "• Khá (~42%): GPA 6.5 - 7.9\n"
                        + "• Trung bình (~21%): GPA 5.0 - 6.4\n"
                        + "• Yếu (~7%): GPA 3.2 - 4.9\n\n"
                        + "Bạn có chắc chắn muốn tạo lại dữ liệu điểm đa dạng?",
                "Xác nhận tạo lại dữ liệu điểm đa dạng",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                executeReseedDiverseGrades();
                JOptionPane.showMessageDialog(this,
                        "Đã tạo lại dữ liệu điểm đa dạng thành công!\n"
                                + "Toàn bộ sinh viên đã được cập nhật điểm và xếp loại đa dạng trên hệ thống.",
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Lỗi khi tạo lại dữ liệu điểm: " + ex.getMessage(),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void executeReseedDiverseGrades() throws java.sql.SQLException {
        new com.quanlysinhvien.dao.DiemDAO().reseedDiverseGrades();
        if (tabbedPane != null) {
            tabbedPane.setSelectedIndex(0);
        }
        if (studentPanel != null) {
            studentPanel.loadDataToTable();
        }
    }

    public JMenuItem getItemReseed() {
        return itemReseed;
    }

    public void doLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn đăng xuất tài khoản hiện tại?",
                "Xác nhận đăng xuất",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            this.dispose();
            SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
        }
    }

    public void showAboutDialog() {
        String about = "ỨNG DỤNG QUẢN LÝ SINH VIÊN\n" +
                "Môn học: Lập trình mạng / Ứng dụng Java\n" +
                "Kiến trúc: 3 Tầng tối giản (Model - DAO - UI)\n" +
                "Giao diện: Java Swing Hiện Đại (Smart Pathshala Dashboard Theme)\n" +
                "Animation: Thuần Pure Swing (Easing Out Cubic & AlphaComposite)\n" +
                "Cơ sở dữ liệu: MySQL 8.0 qua JDBC Pure (Hỗ trợ nạp Text File dự phòng)\n" +
                "Bản quyền: 2026 - Mọi quyền được bảo lưu.";
        JOptionPane.showMessageDialog(this, about, "Giới thiệu", JOptionPane.INFORMATION_MESSAGE);
    }

    public void toggleSidebar() {
        if (tabbedPane != null) {
            tabbedPane.setAnimationEnabled(false);
        }
        sidebar.setExpanded(!sidebar.isExpanded());
        getContentPane().revalidate();
        getContentPane().repaint();
        if (tabbedPane != null) {
            SwingUtilities.invokeLater(() -> tabbedPane.setAnimationEnabled(true));
        }
    }

    // --- Getters phục vụ kiểm thử và tương tác module ---
    public ModernSidebar getSidebar() {
        return sidebar;
    }

    public TopHeaderBar getHeaderBar() {
        return headerBar;
    }

    public SlideTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public StudentManagementPanel getStudentPanel() {
        return studentPanel;
    }

    public CalculatorPanel getCalculatorPanel() {
        return calculatorPanel;
    }

    /**
     * LayoutManager tùy biến định vị 4 thành phần trong MainFrame mà không cần wrap panel trung gian,
     * bảo đảm tabbedPane là direct child của getContentPane() cho UIComponentTest.
     */
    public static class MainFrameLayout implements LayoutManager {
        private Component sidebar;
        private Component header;
        private Component center;
        private Component statusBar;

        @Override
        public void addLayoutComponent(String name, Component comp) {
            if ("WEST".equalsIgnoreCase(name) || comp instanceof ModernSidebar) {
                sidebar = comp;
            } else if ("NORTH".equalsIgnoreCase(name) || comp instanceof TopHeaderBar) {
                header = comp;
            } else if ("CENTER".equalsIgnoreCase(name) || comp instanceof JTabbedPane) {
                center = comp;
            } else if ("SOUTH".equalsIgnoreCase(name)) {
                statusBar = comp;
            } else {
                if (comp instanceof ModernSidebar) sidebar = comp;
                else if (comp instanceof TopHeaderBar) header = comp;
                else if (comp instanceof JTabbedPane) center = comp;
                else statusBar = comp;
            }
        }

        @Override
        public void removeLayoutComponent(Component comp) {
            if (comp == sidebar) sidebar = null;
            if (comp == header) header = null;
            if (comp == center) center = null;
            if (comp == statusBar) statusBar = null;
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return new Dimension(1180, 780);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return new Dimension(920, 620);
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets insets = parent.getInsets();
            int x = insets.left;
            int y = insets.top;
            int totalW = parent.getWidth() - insets.left - insets.right;
            int totalH = parent.getHeight() - insets.top - insets.bottom;

            int sbH = (statusBar != null && statusBar.isVisible()) ? statusBar.getPreferredSize().height : 0;
            int sideW = (sidebar != null && sidebar.isVisible()) ? sidebar.getPreferredSize().width : 0;
            int headH = (header != null && header.isVisible()) ? header.getPreferredSize().height : 0;

            int contentW = Math.max(0, totalW - sideW);
            int mainH = Math.max(0, totalH - sbH);
            int centerH = Math.max(0, mainH - headH);

            if (sidebar != null && sidebar.isVisible()) {
                sidebar.setBounds(x, y, sideW, mainH);
            }
            if (header != null && header.isVisible()) {
                header.setBounds(x + sideW, y, contentW, headH);
            }
            if (center != null && center.isVisible()) {
                center.setBounds(x + sideW, y + headH, contentW, centerH);
            }
            if (statusBar != null && statusBar.isVisible()) {
                statusBar.setBounds(x, y + mainH, totalW, sbH);
            }
        }
    }
}
