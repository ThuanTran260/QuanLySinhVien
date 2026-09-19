package com.quanlysinhvien.ui.components;

import com.quanlysinhvien.ui.theme.UITheme;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

/**
 * Thanh điều hướng dọc hiện đại bên trái (Modern Sidebar):
 * - Chiều rộng: 210px (Expanded) ↔ 64px (Collapsed)
 * - Chuyển đổi trạng thái tức thì (Instant, 60fps, không Timer lồng nhau)
 * - Dải chỉ báo active 3px màu Electric Indigo (#635BFF)
 * - 4 mục điều hướng: Sinh Viên, Máy Tính, Thống Kê, Giới Thiệu
 */
public class ModernSidebar extends JPanel {

    public enum MenuId {
        SINH_VIEN,
        MAY_TINH,
        THONG_KE,
        GIOI_THIEU
    }

    @FunctionalInterface
    public interface NavigationListener {
        void onNavigate(MenuId menuId);
    }

    @FunctionalInterface
    public interface ToggleListener {
        void onToggle(boolean expanded);
    }

    public static final int WIDTH_EXPANDED = 210;
    public static final int WIDTH_COLLAPSED = 64;

    private boolean expanded = true;
    private MenuId activeMenu = MenuId.SINH_VIEN;
    private NavigationListener navigationListener;
    private ToggleListener toggleListener;

    private final Map<MenuId, NavButton> navButtons = new EnumMap<>(MenuId.class);
    private final JLabel lblLogoText = new JLabel("QL SINH VIÊN");
    private final JLabel lblLogoIcon = new JLabel("", SwingConstants.CENTER);
    private final JButton btnToggle = new JButton() {
        @Override
        protected void paintComponent(Graphics g) {
            if (getModel().isRollover()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xF1, 0xF5, 0xF9));
                g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 8, 8);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    };

    public ModernSidebar() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(WIDTH_EXPANDED, 0));
        setMinimumSize(new Dimension(WIDTH_EXPANDED, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UITheme.BORDER_COLOR));

        // 1. Header (Logo & Brand) - Cao 56px khớp với TopHeaderBar
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(Color.WHITE);
        logoPanel.setPreferredSize(new Dimension(WIDTH_EXPANDED, 56));
        logoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(0, 12, 0, 12)
        ));

        lblLogoIcon.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblLogoIcon.setPreferredSize(new Dimension(40, 56));
        lblLogoIcon.setHorizontalAlignment(SwingConstants.CENTER);
        lblLogoIcon.setIcon(SidebarIcons.createLogoIcon(26));
        lblLogoIcon.setText("");

        lblLogoText.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblLogoText.setForeground(UITheme.PRIMARY);

        logoPanel.add(lblLogoIcon, BorderLayout.WEST);
        logoPanel.add(lblLogoText, BorderLayout.CENTER);
        add(logoPanel, BorderLayout.NORTH);

        // 2. Menu Items List
        JPanel menuContainer = new JPanel();
        menuContainer.setBackground(Color.WHITE);
        menuContainer.setLayout(new BoxLayout(menuContainer, BoxLayout.Y_AXIS));
        menuContainer.setBorder(BorderFactory.createEmptyBorder(12, 6, 12, 6));

        addNavItem(menuContainer, MenuId.SINH_VIEN, "Sinh Viên", SidebarIcons.createStudentIcon(20));
        addNavItem(menuContainer, MenuId.MAY_TINH, "Máy Tính", SidebarIcons.createCalculatorIcon(20));
        addNavItem(menuContainer, MenuId.THONG_KE, "Thống Kê", SidebarIcons.createChartIcon(20));
        addNavItem(menuContainer, MenuId.GIOI_THIEU, "Giới Thiệu", SidebarIcons.createAboutIcon(20));

        add(menuContainer, BorderLayout.CENTER);

        // 3. Footer Toggle Button (Dưới đáy)
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(Color.WHITE);
        footerPanel.setPreferredSize(new Dimension(WIDTH_EXPANDED, 46));
        footerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));

        btnToggle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnToggle.setForeground(UITheme.TEXT_SECONDARY);
        btnToggle.setFocusPainted(false);
        btnToggle.setContentAreaFilled(false);
        btnToggle.setOpaque(false);
        btnToggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnToggle.setRolloverEnabled(true);
        btnToggle.setToolTipText("Thu gọn / Mở rộng thanh điều hướng");
        btnToggle.addActionListener(e -> toggle());
        btnToggle.addChangeListener(e -> {
            if (btnToggle.getModel().isRollover()) {
                btnToggle.setForeground(UITheme.PRIMARY);
            } else {
                btnToggle.setForeground(UITheme.TEXT_SECONDARY);
            }
        });

        footerPanel.add(btnToggle, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);

        updateItemsAppearance();
    }

    private void addNavItem(JPanel container, MenuId id, String label, Icon icon) {
        NavButton btn = new NavButton(id, label, icon);
        btn.addActionListener(e -> {
            setActive(id);
            if (navigationListener != null) {
                navigationListener.onNavigate(id);
            }
        });
        navButtons.put(id, btn);
        container.add(btn);
        container.add(Box.createRigidArea(new Dimension(0, 4)));
    }

    public void toggle() {
        if (toggleListener != null) {
            toggleListener.onToggle(!expanded);
        } else {
            setExpanded(!expanded);
        }
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        int targetWidth = expanded ? WIDTH_EXPANDED : WIDTH_COLLAPSED;
        setPreferredSize(new Dimension(targetWidth, getPreferredSize().height));
        setMinimumSize(new Dimension(targetWidth, 0));
        setMaximumSize(new Dimension(targetWidth, Integer.MAX_VALUE));
        updateItemsAppearance();
        revalidate();
        repaint();
    }

    public void setToggleListener(ToggleListener listener) {
        this.toggleListener = listener;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public MenuId getActive() {
        return activeMenu;
    }

    public void setActive(MenuId menuId) {
        if (menuId == null) return;
        this.activeMenu = menuId;
        for (Map.Entry<MenuId, NavButton> entry : navButtons.entrySet()) {
            entry.getValue().setActive(entry.getKey() == menuId);
        }
        repaint();
    }

    public void setNavigationListener(NavigationListener listener) {
        this.navigationListener = listener;
    }

    public JButton getToggleBtn() {
        return btnToggle;
    }

    public JButton getNavButton(MenuId id) {
        return navButtons.get(id);
    }

    private void updateItemsAppearance() {
        lblLogoText.setVisible(expanded);
        btnToggle.setIcon(SidebarIcons.createToggleIcon(16, expanded));
        btnToggle.setText(expanded ? "Thu gọn" : "");
        btnToggle.setHorizontalAlignment(expanded ? SwingConstants.LEFT : SwingConstants.CENTER);
        btnToggle.setIconTextGap(expanded ? 10 : 0);
        btnToggle.setBorder(BorderFactory.createEmptyBorder(6, expanded ? 14 : 0, 6, expanded ? 8 : 0));
        btnToggle.setToolTipText(expanded ? "Thu gọn thanh điều hướng" : "Mở rộng thanh điều hướng");

        for (NavButton btn : navButtons.values()) {
            btn.updateExpandedState(expanded);
        }
    }

    /**
     * Nút menu điều hướng hỗ trợ vẽ dải active 3px màu #635BFF bên lề trái.
     * Hoàn toàn tĩnh (không Timer hover) để bảo đảm 60fps tuyệt đối.
     */
    public static class NavButton extends JButton {
        private final MenuId id;
        private final String fullLabel;
        private final Icon vectorIcon;
        private boolean isActive = false;

        public NavButton(MenuId id, String fullLabel, Icon icon) {
            this.id = id;
            this.fullLabel = fullLabel;
            this.vectorIcon = icon;

            setFont(UITheme.FONT_REGULAR);
            setForeground(UITheme.TEXT_SECONDARY);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setRolloverEnabled(true);
            setIcon(vectorIcon);
            setIconTextGap(12);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setPreferredSize(new Dimension(WIDTH_EXPANDED - 12, 40));
            setMinimumSize(new Dimension(WIDTH_COLLAPSED - 12, 40));
            setHorizontalAlignment(SwingConstants.LEFT);
            setToolTipText(fullLabel);

            // Cập nhật màu chữ theo tương tác mà không gọi setForeground trong paintComponent
            addChangeListener(e -> {
                if (isActive) {
                    setForeground(UITheme.PRIMARY);
                } else if (getModel().isRollover()) {
                    setForeground(UITheme.TEXT_PRIMARY);
                } else {
                    setForeground(UITheme.TEXT_SECONDARY);
                }
            });

            updateExpandedState(true);
        }

        public NavButton(MenuId id, String fullLabel, Icon icon, String shortCode) {
            this(id, fullLabel, icon);
        }

        public NavButton(MenuId id, String fullLabel, String iconText, String shortCode) {
            this(id, fullLabel, SidebarIcons.getNavIcon(id, 20));
        }

        public MenuId getMenuId() {
            return id;
        }

        public boolean isActive() {
            return isActive;
        }

        public void setActive(boolean active) {
            this.isActive = active;
            setFont(active ? UITheme.FONT_BOLD : UITheme.FONT_REGULAR);
            setForeground(active ? UITheme.PRIMARY : UITheme.TEXT_SECONDARY);
            repaint();
        }

        public void updateExpandedState(boolean expanded) {
            setIcon(vectorIcon);
            if (expanded) {
                setText(fullLabel);
                setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 10));
                setIconTextGap(12);
            } else {
                setText("");
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
                setIconTextGap(0);
            }
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (isActive) {
                // Nền tím nhạt khi active
                g2.setColor(new Color(0xF0, 0xF0, 0xFF));
                g2.fillRoundRect(2, 2, w - 4, h - 4, 8, 8);

                // Dải tím Electric Indigo 3px lề trái
                g2.setColor(UITheme.PRIMARY);
                g2.fillRoundRect(2, 4, 3, h - 8, 2, 2);
            } else if (getModel().isRollover()) {
                // Nền hover tinh tế
                g2.setColor(new Color(0xF1, 0xF5, 0xF9));
                g2.fillRoundRect(2, 2, w - 4, h - 4, 8, 8);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
