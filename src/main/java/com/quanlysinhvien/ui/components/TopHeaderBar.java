package com.quanlysinhvien.ui.components;

import com.quanlysinhvien.dao.DatabaseConnection;
import com.quanlysinhvien.ui.theme.UITheme;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;

/**
 * Thanh tiêu đề trên cùng (TopHeaderBar) cao 56px:
 * - Trái: Lời chào "Xin chào, Admin!" + Breadcrumb định vị
 * - Phải: Badge CSDL (Kiểm tra async bằng SwingWorker nền), Avatar đại diện tĩnh, Nút Đăng xuất
 */
public class TopHeaderBar extends JPanel {

    private final JLabel lblGreeting = new JLabel("Xin chào, Admin!");
    private final JLabel lblBreadcrumb = new JLabel(" / Quản Lý Sinh Viên");
    private final JButton btnDbBadge = new JButton("● Offline Mode");
    private final JButton btnLogout = new JButton("Đăng xuất");
    private boolean dbOnline = false;

    public TopHeaderBar() {
        initUI();
        // Kiểm tra kết nối CSDL bất đồng bộ qua SwingWorker nền (mặc định Offline... xám ban đầu)
        checkDatabaseAsync(false);
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(0, 56));
        setMinimumSize(new Dimension(0, 56));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(0, 18, 0, 18)
        ));

        // --- Left Panel: Greeting & Breadcrumb ---
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 16));
        leftPanel.setOpaque(false);

        lblGreeting.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblGreeting.setForeground(UITheme.TEXT_PRIMARY);

        lblBreadcrumb.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblBreadcrumb.setForeground(UITheme.TEXT_SECONDARY);

        leftPanel.add(lblGreeting);
        leftPanel.add(lblBreadcrumb);
        add(leftPanel, BorderLayout.WEST);

        // --- Right Panel: DB Badge, Avatar, Logout ---
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        rightPanel.setOpaque(false);

        // DB Status Badge
        styleDbBadge(false);
        btnDbBadge.setFocusPainted(false);
        btnDbBadge.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDbBadge.setToolTipText("Trạng thái CSDL MySQL. Nhấn để thử kết nối lại.");
        btnDbBadge.addActionListener(e -> checkDatabaseAsync());
        rightPanel.add(btnDbBadge);

        // Avatar tĩnh
        JComponent avatar = createStaticAvatar("AD");
        rightPanel.add(avatar);

        // Logout Button
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLogout.setForeground(new Color(0xDC, 0x26, 0x26)); // Soft Red
        btnLogout.setBackground(new Color(0xFE, 0xF2, 0xF2));
        btnLogout.setFocusPainted(false);
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xFE, 0xCA, 0xCA), 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        rightPanel.add(btnLogout);

        add(rightPanel, BorderLayout.EAST);
    }

    private JComponent createStaticAvatar(String initials) {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(32, 32));
                setMinimumSize(new Dimension(32, 32));
                setToolTipText("Tài khoản: Admin Quản Trị");
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int size = Math.min(getWidth(), getHeight()) - 2;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                // Vòng tròn tím Gradient / Flat Electric Indigo
                g2.setColor(UITheme.PRIMARY);
                g2.fillOval(x, y, size, size);

                // Viền nhẹ
                g2.setColor(new Color(0x4F, 0x46, 0xE5));
                g2.drawOval(x, y, size, size);

                // Chữ viết tắt Initials
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int textW = fm.stringWidth(initials);
                int textH = fm.getAscent();
                g2.drawString(initials, x + (size - textW) / 2, y + (size + textH) / 2 - 2);

                g2.dispose();
            }
        };
    }

    private void styleDbBadge(boolean online) {
        this.dbOnline = online;
        if (online) {
            btnDbBadge.setText("● MySQL Online");
            btnDbBadge.setForeground(new Color(0x05, 0x96, 0x69)); // Dark Emerald
            btnDbBadge.setBackground(new Color(0xEC, 0xFD, 0xF5)); // Soft Emerald
            btnDbBadge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xA7, 0xF3, 0xD0), 1, true),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));
        } else {
            btnDbBadge.setText("● Offline Mode");
            btnDbBadge.setForeground(new Color(0x64, 0x74, 0x8B)); // Slate Muted
            btnDbBadge.setBackground(new Color(0xF1, 0xF5, 0xF9)); // Soft Slate
            btnDbBadge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xCB, 0xD5, 0xE1), 1, true),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));
        }
    }

    /**
     * Cập nhật đường dẫn Breadcrumb trên thanh tiêu đề.
     */
    public void updateBreadcrumb(String sectionName) {
        if (sectionName == null || sectionName.trim().isEmpty()) {
            lblBreadcrumb.setText("");
        } else {
            lblBreadcrumb.setText(" / " + sectionName.trim());
        }
    }

    /**
     * Cập nhật trạng thái hiển thị của DB Badge.
     */
    public void setDbOnline(boolean online) {
        this.dbOnline = online;
        styleDbBadge(online);
        if (SwingUtilities.isEventDispatchThread()) {
            repaint();
        } else {
            SwingUtilities.invokeLater(this::repaint);
        }
    }

    /**
     * Kiểm tra kết nối MySQL ngầm qua SwingWorker mà không gây giật lag EDT.
     */
    public void checkDatabaseAsync() {
        checkDatabaseAsync(true);
    }

    public void checkDatabaseAsync(boolean showConnecting) {
        if (showConnecting) {
            btnDbBadge.setText("● Đang kết nối...");
            btnDbBadge.setForeground(new Color(0x64, 0x74, 0x8B));
        }

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    return conn != null && !conn.isClosed();
                } catch (Throwable t) {
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean online = get();
                    setDbOnline(online);
                } catch (Exception e) {
                    setDbOnline(false);
                }
            }
        };
        worker.execute();
    }

    public JButton getDbBadge() {
        return btnDbBadge;
    }

    public JButton getBtnLogout() {
        return btnLogout;
    }

    public boolean isDbOnline() {
        return dbOnline;
    }
}
