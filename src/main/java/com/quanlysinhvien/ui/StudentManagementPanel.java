package com.quanlysinhvien.ui;

import com.quanlysinhvien.dao.SinhVienDAO;
import com.quanlysinhvien.model.SinhVien;
import com.quanlysinhvien.ui.animation.SlideTabbedPane;
import com.quanlysinhvien.ui.components.KpiCardsPanel;
import com.quanlysinhvien.ui.theme.ModernButton;
import com.quanlysinhvien.ui.theme.ModernCardPanel;
import com.quanlysinhvien.ui.theme.ModernTableRenderer;
import com.quanlysinhvien.ui.theme.UITheme;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Màn hình Quản Lý Sinh Viên hiện đại theo kiến trúc Modern SaaS Dashboard:
 * - Hàng 4 thẻ KPI in-memory từ masterList
 * - Form nhập liệu Collapsible (Thu gọn / Mở rộng linh hoạt)
 * - Thanh công cụ Toolbar 2 tầng cố định chiều cao
 * - Phóng to Bảng (Maximize View - F11 / Esc) hiển thị trọn vẹn 30-40 sinh viên
 * - Chuyển đổi mật độ dòng Density (26px Compact ↔ 34px Comfortable)
 * - Tách biệt masterList (phục vụ KPI) và viewList (phục vụ hiển thị / filter bảng)
 * - Render dữ liệu theo lô batch qua setDataVector() siêu tốc
 */
public class StudentManagementPanel extends JPanel {

    public enum ViewState {
        NORMAL,
        FORM_COLLAPSED,
        MAXIMIZED
    }

    private static final String[] COLUMN_NAMES = {"STT", "Mã SV", "Họ và Tên", "Lớp", "Ngày Sinh", "Điểm TB", "Xếp Loại"};

    private final SinhVienDAO dao = new SinhVienDAO();

    // Data lists
    private List<SinhVien> masterList = new ArrayList<>();
    private List<SinhVien> viewList = new ArrayList<>();

    // State Machine
    private ViewState currentState = ViewState.NORMAL;
    private ViewState stateBeforeMaximize = ViewState.NORMAL;
    private int density = 32;

    // Components: 2-Column Split View
    private JPanel centerSplitPanel;
    private ModernCardPanel leftFormCard;
    private ModernCardPanel rightTableCard;
    private JScrollPane scrollPane;
    private KpiCardsPanel kpiPanel;

    // Form inputs
    private JTextField txtMaSV;
    private JTextField txtHoTen;
    private JTextField txtLop;
    private JTextField txtNgaySinh;
    private DatePicker datePickerNgaySinh;
    private JTextField txtDiemTB;

    // Bộ lọc thống nhất: tiêu chí tìm + sắp xếp chung 1 popup (thay 2 JComboBox cũ)
    private JTextField txtSearch;
    private JButton btnFilter;
    private String searchCriteria = "Tất cả";
    /** -1 = mặc định (không sắp xếp), 0-7 = 8 kiểu sắp xếp như combo cũ. */
    private int sortIndex = -1;
    private JPopupMenu filterPopup;
    private List<JRadioButton> criteriaRadios;
    private List<JRadioButton> sortRadios;

    // Table
    private JTable tblSinhVien;
    private DefaultTableModel tableModel;

    // Buttons
    private JButton btnAdd;
    private JButton btnUpdate;
    private JButton btnDelete;
    private JButton btnClear;
    private JButton btnSearch;
    private JButton btnRefresh;
    private JButton btnStatistic;
    private JButton btnBangDiem;
    private JButton btnOpenBangDiem;
    private JPopupMenu tableContextMenu;
    private JButton btnExportFile;
    private JButton btnImportFile;
    private JButton btnMaximize;

    private final DecimalFormat scoreFormat = new DecimalFormat("#0.0#",
            DecimalFormatSymbols.getInstance(Locale.US));

    public StudentManagementPanel() {
        initUI();
        loadDataToTable();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 10));
        setBackground(UITheme.CANVAS_BG);
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        // 1. Top KPI Panel (4 KPI cards in a horizontal row)
        kpiPanel = new KpiCardsPanel();
        add(kpiPanel, BorderLayout.NORTH);

        // 2. Center Split: 2-Column View (Left: Form, Right: Table Full Height)
        centerSplitPanel = new JPanel(new BorderLayout(10, 0));
        centerSplitPanel.setOpaque(false);

        // 2.1 Cột Trái (~330px): Form nhập liệu thông tin sinh viên
        leftFormCard = new ModernCardPanel(new BorderLayout(0, 10), 16);
        leftFormCard.setPreferredSize(new Dimension(330, 0));

        // Header tiêu đề Form
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel lblFormTitle = new JLabel("Thông tin sinh viên");
        lblFormTitle.setFont(UITheme.FONT_TITLE_MEDIUM);
        lblFormTitle.setForeground(UITheme.TEXT_PRIMARY);

        JLabel lblFormSub = new JLabel("Nhập và quản lý hồ sơ sinh viên");
        lblFormSub.setFont(UITheme.FONT_SMALL);
        lblFormSub.setForeground(UITheme.TEXT_MUTED);

        titlePanel.add(lblFormTitle);
        titlePanel.add(Box.createRigidArea(new Dimension(0, 2)));
        titlePanel.add(lblFormSub);

        // 5 trường thông tin xếp dọc
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        int row = 0;

        // Mã SV
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 3, 0);
        JLabel lblMaSV = new JLabel("Mã Sinh Viên (*):");
        lblMaSV.setFont(UITheme.FONT_BOLD);
        lblMaSV.setForeground(UITheme.TEXT_PRIMARY);
        fieldsPanel.add(lblMaSV, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 8, 0);
        txtMaSV = new JTextField();
        UITheme.styleTextField(txtMaSV);
        txtMaSV.setPreferredSize(new Dimension(0, 32));
        fieldsPanel.add(txtMaSV, gbc);

        // Họ và Tên
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 3, 0);
        JLabel lblHoTen = new JLabel("Họ và Tên (*):");
        lblHoTen.setFont(UITheme.FONT_BOLD);
        lblHoTen.setForeground(UITheme.TEXT_PRIMARY);
        fieldsPanel.add(lblHoTen, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 8, 0);
        txtHoTen = new JTextField();
        UITheme.styleTextField(txtHoTen);
        txtHoTen.setPreferredSize(new Dimension(0, 32));
        fieldsPanel.add(txtHoTen, gbc);

        // Lớp
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 3, 0);
        JLabel lblLop = new JLabel("Lớp (*):");
        lblLop.setFont(UITheme.FONT_BOLD);
        lblLop.setForeground(UITheme.TEXT_PRIMARY);
        fieldsPanel.add(lblLop, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 8, 0);
        txtLop = new JTextField();
        UITheme.styleTextField(txtLop);
        txtLop.setPreferredSize(new Dimension(0, 32));
        fieldsPanel.add(txtLop, gbc);

        // Ngày Sinh
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 3, 0);
        JLabel lblNgaySinh = new JLabel("Ngày Sinh (yyyy-MM-dd):");
        lblNgaySinh.setFont(UITheme.FONT_BOLD);
        lblNgaySinh.setForeground(UITheme.TEXT_PRIMARY);
        fieldsPanel.add(lblNgaySinh, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 8, 0);
        txtNgaySinh = new JTextField();
        UITheme.styleTextField(txtNgaySinh);
        txtNgaySinh.setVisible(false);

        // Ô chọn ngày (LGoodDatePicker) hiển thị toàn bộ chiều rộng hàng ngày sinh.
        // Đồng bộ 2 chiều với txtNgaySinh (ẩn) để giữ nguyên vẹn validation và headless tests.
        DatePickerSettings dps = new DatePickerSettings(new Locale("vi"));
        dps.setFormatForDatesCommonEra("yyyy-MM-dd");
        dps.setAllowEmptyDates(true);
        datePickerNgaySinh = new DatePicker(dps);
        datePickerNgaySinh.setPreferredSize(new Dimension(0, 32));

        final boolean[] isSyncing = new boolean[]{false};
        datePickerNgaySinh.addDateChangeListener(e -> {
            if (isSyncing[0]) return;
            isSyncing[0] = true;
            try {
                LocalDate d = datePickerNgaySinh.getDate();
                txtNgaySinh.setText(d != null ? d.format(SinhVien.DATE_FORMATTER) : "");
            } finally {
                isSyncing[0] = false;
            }
        });

        txtNgaySinh.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void sync() {
                if (isSyncing[0]) return;
                isSyncing[0] = true;
                try {
                    String s = txtNgaySinh.getText().trim();
                    if (s.isEmpty()) {
                        datePickerNgaySinh.clear();
                    } else {
                        try {
                            datePickerNgaySinh.setDate(LocalDate.parse(s, SinhVien.DATE_FORMATTER));
                        } catch (Exception ex) {
                            datePickerNgaySinh.clear();
                        }
                    }
                } finally {
                    isSyncing[0] = false;
                }
            }
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { sync(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { sync(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { sync(); }
        });

        JPanel ngaySinhRow = new JPanel(new BorderLayout());
        ngaySinhRow.setOpaque(false);
        ngaySinhRow.add(datePickerNgaySinh, BorderLayout.CENTER);
        ngaySinhRow.add(txtNgaySinh, BorderLayout.SOUTH);
        fieldsPanel.add(ngaySinhRow, gbc);

        // Điểm TB
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 3, 0);
        JLabel lblDiemTB = new JLabel("Điểm Trung Bình (0-10):");
        lblDiemTB.setFont(UITheme.FONT_BOLD);
        lblDiemTB.setForeground(UITheme.TEXT_PRIMARY);
        fieldsPanel.add(lblDiemTB, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 4, 0);
        txtDiemTB = new JTextField();
        UITheme.styleTextField(txtDiemTB);
        txtDiemTB.setEditable(false);
        txtDiemTB.setBackground(UITheme.BG_MUTED);
        txtDiemTB.setToolTipText("Điểm TB được tự động tính theo trọng số tín chỉ từ Bảng điểm theo môn");
        txtDiemTB.setPreferredSize(new Dimension(0, 32));
        fieldsPanel.add(txtDiemTB, gbc);

        // Ghi chú
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 0, 2, 0);
        JLabel lblNote = new JLabel("(*) Bắt buộc | Điểm: 0.0 - 10.0");
        lblNote.setFont(UITheme.FONT_SMALL);
        lblNote.setForeground(UITheme.TEXT_MUTED);
        fieldsPanel.add(lblNote, gbc);

        // Cụm 4 nút CRUD chia lưới 2x2 cân đối
        JPanel btnGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        btnGrid.setOpaque(false);

        btnAdd = new ModernButton("Thêm mới", UITheme.PRIMARY);
        btnAdd.setPreferredSize(new Dimension(0, 34));

        btnUpdate = new ModernButton("Cập nhật", UITheme.SUCCESS);
        btnUpdate.setPreferredSize(new Dimension(0, 34));

        btnDelete = new ModernButton("Xóa sinh viên", UITheme.DANGER);
        btnDelete.setPreferredSize(new Dimension(0, 34));

        btnClear = new ModernButton("Làm mới form", UITheme.NEUTRAL_BTN_BG, UITheme.NEUTRAL_BTN_HOVER, UITheme.NEUTRAL_BTN_TEXT);
        ((ModernButton) btnClear).setBorderColor(UITheme.NEUTRAL_BTN_BORDER);
        btnClear.setPreferredSize(new Dimension(0, 34));

        btnGrid.add(btnAdd);
        btnGrid.add(btnUpdate);
        btnGrid.add(btnDelete);
        btnGrid.add(btnClear);

        // Nút Bảng Điểm Theo Môn nằm ngay bên dưới 4 nút CRUD, kích thước đầy đủ dễ thấy
        btnOpenBangDiem = new ModernButton("📊 Bảng Điểm Theo Môn", UITheme.PRIMARY);
        btnOpenBangDiem.setPreferredSize(new Dimension(0, 36));
        btnOpenBangDiem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnOpenBangDiem.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnOpenBangDiem.setToolTipText("Xem/sửa bảng điểm theo môn của sinh viên đang chọn");

        // Body Form bên trái
        JPanel formBody = new JPanel();
        formBody.setLayout(new BoxLayout(formBody, BoxLayout.Y_AXIS));
        formBody.setOpaque(false);

        formBody.add(titlePanel);
        formBody.add(Box.createRigidArea(new Dimension(0, 10)));
        formBody.add(fieldsPanel);
        formBody.add(Box.createRigidArea(new Dimension(0, 10)));
        formBody.add(btnGrid);
        formBody.add(Box.createRigidArea(new Dimension(0, 8)));
        formBody.add(btnOpenBangDiem);

        JScrollPane formScroll = new JScrollPane(formBody);
        formScroll.setBorder(null);
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        formScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        leftFormCard.add(formScroll, BorderLayout.CENTER);
        centerSplitPanel.add(leftFormCard, BorderLayout.WEST);

        // 2.2 Cột Phải: Bảng sinh viên Full Height + Toolbar 1 hàng
        rightTableCard = new ModernCardPanel(new BorderLayout(0, 8), 14);

        // Toolbar Container 1 hàng duy nhất: Phía tây/giữa là các công cụ cuộn ngang nếu hẹp, Phía đông là nút Phóng to luôn cố định
        JPanel toolBarContainer = new JPanel(new BorderLayout(8, 0));
        toolBarContainer.setOpaque(false);

        // Toolbar Panel chứa các nút chức năng
        JPanel toolBarPanel = new JPanel();
        toolBarPanel.setLayout(new BoxLayout(toolBarPanel, BoxLayout.X_AXIS));
        toolBarPanel.setOpaque(false);

        // Nhóm Tìm kiếm + Bộ lọc thống nhất (1 nút thay 2 combo cũ)
        txtSearch = new JTextField();
        UITheme.styleTextField(txtSearch);
        setFixedControlSize(txtSearch, 100, 30);
        txtSearch.setAlignmentY(Component.CENTER_ALIGNMENT);
        txtSearch.setToolTipText("Nhập từ khóa tìm kiếm...");

        btnFilter = new ModernButton("Bộ lọc", UITheme.NEUTRAL_BTN_BG, UITheme.NEUTRAL_BTN_HOVER, UITheme.NEUTRAL_BTN_TEXT);
        ((ModernButton) btnFilter).setBorderColor(UITheme.NEUTRAL_BTN_BORDER);
        setFixedControlSize(btnFilter, 90, 30);
        btnFilter.setAlignmentY(Component.CENTER_ALIGNMENT);
        btnFilter.setToolTipText("Chọn tiêu chí tìm kiếm và cách sắp xếp");
        buildFilterPopup();

        btnSearch = new ModernButton("Tìm kiếm", UITheme.PRIMARY);
        setFixedControlSize(btnSearch, 80, 30);
        btnSearch.setAlignmentY(Component.CENTER_ALIGNMENT);

        btnRefresh = new ModernButton("Tất cả", UITheme.NEUTRAL_BTN_BG, UITheme.NEUTRAL_BTN_HOVER, UITheme.NEUTRAL_BTN_TEXT);
        ((ModernButton) btnRefresh).setBorderColor(UITheme.NEUTRAL_BTN_BORDER);
        setFixedControlSize(btnRefresh, 65, 30);
        btnRefresh.setAlignmentY(Component.CENTER_ALIGNMENT);

        // Thống kê & File IO
        btnStatistic = new ModernButton("Thống kê", new Color(0x7C, 0x3A, 0xED));
        setFixedControlSize(btnStatistic, 80, 30);
        btnStatistic.setAlignmentY(Component.CENTER_ALIGNMENT);

        // Bảng điểm theo môn (hướng A)
        btnBangDiem = new ModernButton("Bảng điểm", UITheme.PRIMARY);
        setFixedControlSize(btnBangDiem, 95, 30);
        btnBangDiem.setAlignmentY(Component.CENTER_ALIGNMENT);
        btnBangDiem.setToolTipText("Xem/sửa điểm theo môn (Báo cáo 40% + Chuyên cần 10% + Cuối kỳ 50%)");

        btnExportFile = new ModernButton("Xuất File", UITheme.NEUTRAL_BTN_BG, UITheme.NEUTRAL_BTN_HOVER, UITheme.NEUTRAL_BTN_TEXT);
        ((ModernButton) btnExportFile).setBorderColor(UITheme.NEUTRAL_BTN_BORDER);
        setFixedControlSize(btnExportFile, 80, 30);
        btnExportFile.setAlignmentY(Component.CENTER_ALIGNMENT);

        btnImportFile = new ModernButton("Nạp File", UITheme.NEUTRAL_BTN_BG, UITheme.NEUTRAL_BTN_HOVER, UITheme.NEUTRAL_BTN_TEXT);
        ((ModernButton) btnImportFile).setBorderColor(UITheme.NEUTRAL_BTN_BORDER);
        setFixedControlSize(btnImportFile, 75, 30);
        btnImportFile.setAlignmentY(Component.CENTER_ALIGNMENT);

        // Nút Phóng to [⛶] - Luôn ghim ở góc phải trên cùng, không bao giờ bị cuộn che mất
        btnMaximize = new ModernButton("Phóng to [⛶]", new Color(0x0E, 0xA5, 0xE9));
        setFixedControlSize(btnMaximize, 105, 30);
        btnMaximize.setAlignmentY(Component.CENTER_ALIGNMENT);
        btnMaximize.setToolTipText("Phóng to bảng chiếm 100% diện tích (Phím tắt: F11 vào/ra, Esc thoát)");
        btnMaximize.addActionListener(e -> toggleMaximize());

        // Ghép vào thanh công cụ cuộn ngang: Tìm kiếm + Bộ lọc -> Thống kê & Bảng điểm -> File
        toolBarPanel.add(txtSearch);
        toolBarPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        toolBarPanel.add(btnFilter);
        toolBarPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        toolBarPanel.add(btnSearch);
        toolBarPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        toolBarPanel.add(btnRefresh);
        toolBarPanel.add(Box.createRigidArea(new Dimension(6, 0)));
        toolBarPanel.add(createToolbarSeparator());
        toolBarPanel.add(Box.createRigidArea(new Dimension(6, 0)));
        toolBarPanel.add(btnStatistic);
        toolBarPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        toolBarPanel.add(btnBangDiem);
        toolBarPanel.add(Box.createRigidArea(new Dimension(6, 0)));
        toolBarPanel.add(createToolbarSeparator());
        toolBarPanel.add(Box.createRigidArea(new Dimension(6, 0)));
        toolBarPanel.add(btnExportFile);
        toolBarPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        toolBarPanel.add(btnImportFile);

        JScrollPane toolBarScroll = new JScrollPane(toolBarPanel);
        toolBarScroll.setBorder(null);
        toolBarScroll.setOpaque(false);
        toolBarScroll.getViewport().setOpaque(false);
        toolBarScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        toolBarScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        toolBarScroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 7));

        toolBarContainer.setPreferredSize(new Dimension(0, 38));
        toolBarContainer.add(toolBarScroll, BorderLayout.CENTER);
        toolBarContainer.add(btnMaximize, BorderLayout.EAST);

        rightTableCard.add(toolBarContainer, BorderLayout.NORTH);

        // --- Table Section ---
        tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0: return Integer.class; // Numerical sort for STT
                    case 5: return Float.class;   // Numerical sort for DiemTB
                    default: return String.class;
                }
            }
        };

        tblSinhVien = new JTable(tableModel);
        tblSinhVien.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblSinhVien.setAutoCreateRowSorter(true);
        tblSinhVien.setFillsViewportHeight(true);
        ModernTableRenderer.applyModernStyle(tblSinhVien);
        tblSinhVien.setAutoCreateColumnsFromModel(false); // Ngăn setDataVector phá hủy renderer/width
        tblSinhVien.setRowHeight(density);

        // Column preferred widths
        tblSinhVien.getColumnModel().getColumn(0).setPreferredWidth(50);
        tblSinhVien.getColumnModel().getColumn(1).setPreferredWidth(95);
        tblSinhVien.getColumnModel().getColumn(2).setPreferredWidth(180);
        tblSinhVien.getColumnModel().getColumn(3).setPreferredWidth(95);
        tblSinhVien.getColumnModel().getColumn(4).setPreferredWidth(105);
        tblSinhVien.getColumnModel().getColumn(5).setPreferredWidth(85);
        tblSinhVien.getColumnModel().getColumn(6).setPreferredWidth(110);

        scrollPane = new JScrollPane(tblSinhVien);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);

        rightTableCard.add(scrollPane, BorderLayout.CENTER);
        centerSplitPanel.add(rightTableCard, BorderLayout.CENTER);

        add(centerSplitPanel, BorderLayout.CENTER);

        // Event Handlers & Key Bindings
        setupEventHandlers();
        setupKeyBindings();
    }

    private void setFixedControlSize(JComponent comp, int width, int height) {
        Dimension d = new Dimension(width, height);
        comp.setPreferredSize(d);
        comp.setMaximumSize(d);
        comp.setMinimumSize(d);
    }

    private static final String[] SEARCH_OPTIONS = {"Tất cả", "Mã SV", "Họ tên", "Lớp"};
    private static final String[] SORT_OPTIONS = {
            "Mặc định",
            "Tên (A-Z)",
            "Tên (Z-A)",
            "Họ và Tên (A-Z)",
            "Họ và Tên (Z-A)",
            "Điểm TB (Tăng dần)",
            "Điểm TB (Giảm dần)",
            "Mã SV (Tăng dần)",
            "Mã SV (Giảm dần)"
    };

    /** Dựng popup 2 nhóm: tiêu chí tìm + cách sắp xếp, chung 1 nút Bộ lọc. */
    private void buildFilterPopup() {
        filterPopup = new JPopupMenu();
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel lblCriteria = new JLabel("Tìm theo:");
        lblCriteria.setFont(UITheme.FONT_BOLD);
        lblCriteria.setForeground(UITheme.TEXT_PRIMARY);
        lblCriteria.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lblCriteria);

        ButtonGroup groupCriteria = new ButtonGroup();
        criteriaRadios = new ArrayList<>();
        for (String opt : SEARCH_OPTIONS) {
            JRadioButton radio = new JRadioButton(opt, opt.equals(searchCriteria));
            radio.setFont(UITheme.FONT_REGULAR);
            radio.setForeground(UITheme.TEXT_PRIMARY);
            radio.setOpaque(false);
            radio.setAlignmentX(Component.LEFT_ALIGNMENT);
            groupCriteria.add(radio);
            criteriaRadios.add(radio);
            panel.add(radio);
        }

        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        JLabel lblSort = new JLabel("Sắp xếp:");
        lblSort.setFont(UITheme.FONT_BOLD);
        lblSort.setForeground(UITheme.TEXT_PRIMARY);
        lblSort.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lblSort);

        ButtonGroup groupSort = new ButtonGroup();
        sortRadios = new ArrayList<>();
        for (int i = 0; i < SORT_OPTIONS.length; i++) {
            JRadioButton radio = new JRadioButton(SORT_OPTIONS[i], (i - 1) == sortIndex);
            radio.setFont(UITheme.FONT_REGULAR);
            radio.setForeground(UITheme.TEXT_PRIMARY);
            radio.setOpaque(false);
            radio.setAlignmentX(Component.LEFT_ALIGNMENT);
            groupSort.add(radio);
            sortRadios.add(radio);
            panel.add(radio);
        }

        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton btnApply = new ModernButton("Áp dụng", UITheme.PRIMARY);
        btnApply.setPreferredSize(new Dimension(90, 30));
        btnApply.addActionListener(e -> {
            readFilterPopup();
            applyFilterSort();
            filterPopup.setVisible(false);
        });
        JButton btnReset = new ModernButton("Đặt lại", UITheme.NEUTRAL_BTN_BG,
                UITheme.NEUTRAL_BTN_HOVER, UITheme.NEUTRAL_BTN_TEXT);
        ((ModernButton) btnReset).setBorderColor(UITheme.NEUTRAL_BTN_BORDER);
        btnReset.setPreferredSize(new Dimension(80, 30));
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            searchCriteria = "Tất cả";
            sortIndex = -1;
            syncFilterPopup();
            applyFilterSort();
            filterPopup.setVisible(false);
        });
        btnRow.add(btnReset);
        btnRow.add(btnApply);
        panel.add(btnRow);

        filterPopup.add(panel);
    }

    private void readFilterPopup() {
        for (JRadioButton radio : criteriaRadios) {
            if (radio.isSelected()) {
                searchCriteria = radio.getText();
                break;
            }
        }
        sortIndex = -1;
        for (int i = 1; i < sortRadios.size(); i++) {
            if (sortRadios.get(i).isSelected()) {
                sortIndex = i - 1;
                break;
            }
        }
    }

    private void syncFilterPopup() {
        if (criteriaRadios == null || sortRadios == null) {
            return;
        }
        for (JRadioButton radio : criteriaRadios) {
            radio.setSelected(radio.getText().equals(searchCriteria));
        }
        for (int i = 0; i < sortRadios.size(); i++) {
            sortRadios.get(i).setSelected((i - 1) == sortIndex);
        }
    }

    /** Dấu chấm trên nút khi lọc/sắp xếp đang khác mặc định. */
    private void updateFilterBadge() {
        boolean active = !"Tất cả".equals(searchCriteria)
                || !txtSearch.getText().trim().isEmpty()
                || sortIndex >= 0;
        btnFilter.setText(active ? "Bộ lọc •" : "Bộ lọc");
    }

    private JComponent createToolbarSeparator() {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        Dimension d = new Dimension(2, 22);
        sep.setPreferredSize(d);
        sep.setMaximumSize(d);
        sep.setMinimumSize(d);
        sep.setForeground(UITheme.BORDER_COLOR);
        sep.setAlignmentY(Component.CENTER_ALIGNMENT);
        return sep;
    }

    private void setupKeyBindings() {
        Action toggleAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleMaximize();
            }
        };

        Action exitAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isMaximized()) {
                    toggleMaximize();
                }
            }
        };

        // Phím tắt F11: Toggle Maximize (hỗ trợ cả khi focus bên trong hoặc trong cửa sổ)
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "toggleMaximize");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "toggleMaximize");
        getActionMap().put("toggleMaximize", toggleAction);

        // Phím tắt Esc: Thoát Maximize nếu đang phóng to
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "exitMaximize");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "exitMaximize");
        getActionMap().put("exitMaximize", exitAction);
    }

    private void setupEventHandlers() {
        // 1. Khi chọn dòng trên JTable -> Đổ dữ liệu lên form, KHÓA ô MaSV
        // QUYẾT ĐỊNH CHỐT: Khi ở chế độ Maximize, KHÔNG tự bung Form, chỉ đổ data vào field ẩn
        tblSinhVien.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = tblSinhVien.getSelectedRow();
                int rowCount = tblSinhVien.getRowSorter() != null ?
                        tblSinhVien.getRowSorter().getViewRowCount() : tblSinhVien.getRowCount();
                if (selectedRow >= 0 && selectedRow < rowCount) {
                    int modelRow = tblSinhVien.convertRowIndexToModel(selectedRow);
                    if (modelRow >= 0 && modelRow < tableModel.getRowCount()) {
                        txtMaSV.setText(tableModel.getValueAt(modelRow, 1).toString());
                        txtHoTen.setText(tableModel.getValueAt(modelRow, 2).toString());
                        txtLop.setText(tableModel.getValueAt(modelRow, 3).toString());
                        txtNgaySinh.setText(tableModel.getValueAt(modelRow, 4).toString());
                        syncDatePickerFromText();

                        Object diemVal = tableModel.getValueAt(modelRow, 5);
                        if (diemVal instanceof Number) {
                            txtDiemTB.setText(scoreFormat.format(((Number) diemVal).doubleValue()));
                        } else {
                            txtDiemTB.setText(diemVal != null ? diemVal.toString() : "");
                        }

                        // Khóa ô Mã SV (khóa chính)
                        txtMaSV.setEditable(false);
                        txtMaSV.setBackground(UITheme.BG_MUTED);
                    }
                }
            }
        });

        // 2. Double-click mở bảng điểm & Popup menu ngữ cảnh chuột phải trên bảng sinh viên
        tblSinhVien.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int row = tblSinhVien.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        tblSinhVien.setRowSelectionInterval(row, row);
                        openBangDiem();
                    }
                }
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                handleTablePopup(e);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                handleTablePopup(e);
            }

            private void handleTablePopup(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = tblSinhVien.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        if (!tblSinhVien.isRowSelected(row)) {
                            tblSinhVien.setRowSelectionInterval(row, row);
                        }
                    }
                }
            }
        });

        tableContextMenu = new JPopupMenu() {
            @Override
            public void show(Component invoker, int x, int y) {
                int row = tblSinhVien.rowAtPoint(new Point(x, y));
                if (row >= 0) {
                    if (!tblSinhVien.isRowSelected(row)) {
                        tblSinhVien.setRowSelectionInterval(row, row);
                    }
                    super.show(invoker, x, y);
                }
            }
        };
        JMenuItem miBangDiem = new JMenuItem("📊 Xem bảng điểm chi tiết môn học...");
        miBangDiem.setFont(UITheme.FONT_REGULAR);
        miBangDiem.addActionListener(e -> openBangDiem());
        tableContextMenu.add(miBangDiem);
        tblSinhVien.setComponentPopupMenu(tableContextMenu);

        btnAdd.addActionListener(e -> onAddStudent());
        btnUpdate.addActionListener(e -> onUpdateStudent());
        btnDelete.addActionListener(e -> onDeleteStudent());
        btnClear.addActionListener(e -> clearFormAndReload());

        btnSearch.addActionListener(e -> applyFilterSort());
        txtSearch.addActionListener(e -> applyFilterSort());
        btnFilter.addActionListener(e -> filterPopup.show(btnFilter, 0, btnFilter.getHeight()));

        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            searchCriteria = "Tất cả";
            sortIndex = -1;
            syncFilterPopup();
            updateFilterBadge();
            viewList = new ArrayList<>(masterList);
            renderBatch(viewList);
        });

        btnStatistic.addActionListener(e -> showStatisticDialog());
        btnBangDiem.addActionListener(e -> openBangDiem());
        if (btnOpenBangDiem != null) {
            btnOpenBangDiem.addActionListener(e -> openBangDiem());
        }
        btnExportFile.addActionListener(e -> onExportFile());
        btnImportFile.addActionListener(e -> onImportFile());
    }

    /** Đồng bộ DatePicker theo txtNgaySinh (khi chọn dòng / làm mới). Lỗi parse thì clear. */
    private void syncDatePickerFromText() {
        if (datePickerNgaySinh == null) {
            return;
        }
        try {
            String s = txtNgaySinh.getText().trim();
            if (s.isEmpty()) {
                datePickerNgaySinh.clear();
            } else {
                datePickerNgaySinh.setDate(LocalDate.parse(s, SinhVien.DATE_FORMATTER));
            }
        } catch (Exception ignored) {
            datePickerNgaySinh.clear();
        }
    }

    /** Mở dialog bảng điểm theo môn của SV đang chọn; lưu xong reload để cập nhật DiemTB. */
    public void openBangDiem() {
        int selectedRow = tblSinhVien.getSelectedRow();
        int rowCount = tblSinhVien.getRowSorter() != null ?
                tblSinhVien.getRowSorter().getViewRowCount() : tblSinhVien.getRowCount();
        if (selectedRow < 0 || selectedRow >= rowCount) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một sinh viên trên bảng để xem bảng điểm!",
                    "Chưa chọn sinh viên", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = tblSinhVien.convertRowIndexToModel(selectedRow);
        String maSV = tableModel.getValueAt(modelRow, 1).toString();
        String hoTen = tableModel.getValueAt(modelRow, 2).toString();
        String lop = tableModel.getValueAt(modelRow, 3).toString();
        java.awt.Window owner = SwingUtilities.getWindowAncestor(this);
        BangDiemDialog dlg = new BangDiemDialog(owner, maSV, hoTen, lop);
        dlg.setOnSaved(() -> refreshAfterMutation());
        dlg.setVisible(true);
    }

    /**
     * Nạp dữ liệu bất đồng bộ qua SwingWorker nền, hỗ trợ fallback sinhvien.txt.
     * Khi chạy trong môi trường test (ngoài EDT), tự động đợi nạp đồng bộ để bảo đảm 84 dòng có sẵn.
     */
    public void loadDataToTable() {
        if (!SwingUtilities.isEventDispatchThread()) {
            List<SinhVien> list = fetchStudentsData();
            applyLoadedData(list);
            return;
        }

        SwingWorker<List<SinhVien>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<SinhVien> doInBackground() {
                return fetchStudentsData();
            }

            @Override
            protected void done() {
                try {
                    List<SinhVien> list = get();
                    applyLoadedData(list);
                } catch (Exception ex) {
                    System.err.println("Lỗi loadDataToTable: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private List<SinhVien> fetchStudentsData() {
        try {
            return dao.getAll();
        } catch (SQLException e) {
            File fallbackFile = new File("sinhvien.txt");
            if (!fallbackFile.exists()) {
                fallbackFile = new File("src/main/resources/sinhvien.txt");
            }
            if (fallbackFile.exists()) {
                try {
                    return dao.importFromFile(fallbackFile);
                } catch (Exception ex) {
                    System.err.println("Fallback import failed: " + ex.getMessage());
                }
            }
            return new ArrayList<>();
        }
    }

    private void applyLoadedData(List<SinhVien> list) {
        masterList = new ArrayList<>(list);
        viewList = new ArrayList<>(list);
        renderBatch(viewList);
        if (kpiPanel != null) {
            kpiPanel.update(masterList);
        }
    }

    /**
     * Render batch dữ liệu siêu tốc bằng cách xây dựng Object[][] và gọi setDataVector() 1 lần.
     * Không destroy hay recreate RowSorter.
     */
    private void renderBatch(List<SinhVien> list) {
        Object[][] data = new Object[list.size()][7];
        for (int i = 0; i < list.size(); i++) {
            SinhVien sv = list.get(i);
            data[i][0] = Integer.valueOf(i + 1);
            data[i][1] = sv.getMaSV();
            data[i][2] = sv.getHoTen();
            data[i][3] = sv.getLop();
            data[i][4] = sv.getNgaySinhStr();
            data[i][5] = Float.valueOf(sv.getDiemTB());
            data[i][6] = sv.getXepLoai();
        }
        tableModel.setDataVector(data, COLUMN_NAMES);
    }

    /**
     * Đồng bộ lại dữ liệu sau mỗi thao tác Thêm / Sửa / Xóa / Nạp file.
     * Cập nhật cả masterList (KPI) và viewList (Bảng).
     */
    private void refreshAfterMutation() {
        List<SinhVien> list = fetchStudentsData();
        applyLoadedData(list);
    }

    private void withAnimationDisabled(Runnable action) {
        SlideTabbedPane tabbed = (SlideTabbedPane) SwingUtilities.getAncestorOfClass(SlideTabbedPane.class, this);
        if (tabbed != null) {
            tabbed.setAnimationEnabled(false);
        }
        try {
            action.run();
        } finally {
            if (tabbed != null) {
                SwingUtilities.invokeLater(() -> tabbed.setAnimationEnabled(true));
            }
        }
    }

    public void toggleFormCollapse() {
        withAnimationDisabled(() -> {
            if (currentState == ViewState.MAXIMIZED) return;
            if (currentState == ViewState.NORMAL) {
                setViewState(ViewState.FORM_COLLAPSED);
            } else {
                setViewState(ViewState.NORMAL);
            }
        });
    }

    public void toggleMaximize() {
        withAnimationDisabled(() -> {
            if (currentState == ViewState.MAXIMIZED) {
                setViewState(stateBeforeMaximize != null ? stateBeforeMaximize : ViewState.NORMAL);
            } else {
                stateBeforeMaximize = currentState;
                setViewState(ViewState.MAXIMIZED);
            }
        });
    }

    private void setViewState(ViewState newState) {
        if (currentState == newState) return;

        int selectedRow = tblSinhVien.getSelectedRow();
        Point scrollPos = scrollPane.getViewport().getViewPosition();

        currentState = newState;

        switch (currentState) {
            case NORMAL:
                kpiPanel.setVisible(true);
                leftFormCard.setVisible(true);
                btnMaximize.setText("Phóng to [⛶]");
                break;
            case FORM_COLLAPSED:
                kpiPanel.setVisible(true);
                leftFormCard.setVisible(false);
                btnMaximize.setText("Phóng to [⛶]");
                break;
            case MAXIMIZED:
                kpiPanel.setVisible(false);
                leftFormCard.setVisible(false);
                btnMaximize.setText("Thu nhỏ [⛶]");
                break;
        }

        revalidate();
        repaint();

        SwingUtilities.invokeLater(() -> {
            if (selectedRow >= 0 && selectedRow < tblSinhVien.getRowCount()) {
                tblSinhVien.setRowSelectionInterval(selectedRow, selectedRow);
            }
            scrollPane.getViewport().setViewPosition(scrollPos);
        });
    }

    private void clearFormAndReload() {
        txtMaSV.setText("");
        txtHoTen.setText("");
        txtLop.setText("");
        txtNgaySinh.setText("");
        if (datePickerNgaySinh != null) {
            datePickerNgaySinh.clear();
        }
        txtDiemTB.setText("");

        txtMaSV.setEditable(true);
        txtMaSV.setBackground(Color.WHITE);
        txtDiemTB.setEditable(false);
        txtDiemTB.setBackground(UITheme.BG_MUTED);
        tblSinhVien.clearSelection();
        loadDataToTable();
        txtMaSV.requestFocus();
    }

    private void onAddStudent() {
        if (!txtMaSV.isEditable()) {
            JOptionPane.showMessageDialog(this,
                    "Đang ở chế độ chọn sinh viên. Vui lòng bấm 'Làm mới form' trước khi thêm mới!",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            String scoreStr = txtDiemTB.getText().trim();
            if (scoreStr.isEmpty()) {
                scoreStr = "0.0";
            }
            SinhVien sv = SinhVien.validateAndCreate(
                    txtMaSV.getText(),
                    txtHoTen.getText(),
                    txtLop.getText(),
                    txtNgaySinh.getText(),
                    scoreStr
            );

            if (dao.existsById(sv.getMaSV())) {
                JOptionPane.showMessageDialog(this,
                        "Mã sinh viên '" + sv.getMaSV() + "' đã tồn tại! Vui lòng nhập mã khác.",
                        "Trùng khóa chính", JOptionPane.ERROR_MESSAGE);
                txtMaSV.requestFocus();
                return;
            }

            boolean success = dao.insert(sv);
            if (success) {
                JOptionPane.showMessageDialog(this,
                        "Thêm sinh viên thành công!",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                clearFormFields();
                refreshAfterMutation();
            } else {
                JOptionPane.showMessageDialog(this, "Không thể thêm sinh viên vào CSDL!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối CSDL: " + ex.getMessage(), "Lỗi SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onUpdateStudent() {
        int selectedRow = tblSinhVien.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một sinh viên trên bảng để cập nhật!",
                    "Chưa chọn sinh viên", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String scoreStr = txtDiemTB.getText().trim();
            if (scoreStr.isEmpty()) {
                scoreStr = "0.0";
            }
            SinhVien sv = SinhVien.validateAndCreate(
                    txtMaSV.getText(),
                    txtHoTen.getText(),
                    txtLop.getText(),
                    txtNgaySinh.getText(),
                    scoreStr
            );

            boolean success = dao.update(sv);
            if (success) {
                JOptionPane.showMessageDialog(this,
                        "Cập nhật thông tin sinh viên thành công!",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                clearFormFields();
                refreshAfterMutation();
            } else {
                JOptionPane.showMessageDialog(this, "Không tìm thấy sinh viên để cập nhật!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối CSDL: " + ex.getMessage(), "Lỗi SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDeleteStudent() {
        int selectedRow = tblSinhVien.getSelectedRow();
        int rowCount = tblSinhVien.getRowSorter() != null ?
                tblSinhVien.getRowSorter().getViewRowCount() : tblSinhVien.getRowCount();
        if (selectedRow < 0 || selectedRow >= rowCount) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một sinh viên trên bảng để xóa!",
                    "Chưa chọn sinh viên", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = tblSinhVien.convertRowIndexToModel(selectedRow);
        String maSV = tableModel.getValueAt(modelRow, 1).toString();
        String hoTen = tableModel.getValueAt(modelRow, 2).toString();

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa sinh viên [" + maSV + " - " + hoTen + "] khỏi hệ thống?",
                "Xác nhận xóa sinh viên",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean success = dao.delete(maSV);
                if (success) {
                    JOptionPane.showMessageDialog(this, "Đã xóa sinh viên thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    clearFormFields();
                    refreshAfterMutation();
                } else {
                    JOptionPane.showMessageDialog(this, "Không tìm thấy sinh viên cần xóa!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi xóa sinh viên: " + ex.getMessage(), "Lỗi SQL", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearFormFields() {
        txtMaSV.setText("");
        txtHoTen.setText("");
        txtLop.setText("");
        txtNgaySinh.setText("");
        if (datePickerNgaySinh != null) {
            datePickerNgaySinh.clear();
        }
        txtDiemTB.setText("");
        txtMaSV.setEditable(true);
        txtMaSV.setBackground(Color.WHITE);
        txtDiemTB.setEditable(false);
        txtDiemTB.setBackground(UITheme.BG_MUTED);
        tblSinhVien.clearSelection();
    }

    /**
     * Luồng lọc + sắp xếp thống nhất: tìm qua DAO (giữ so khớp không dấu của MySQL),
     * rồi sắp xếp in-memory trên kết quả (để lọc và sắp xếp cộng dồn, không xóa nhau).
     * Offline thì lọc in-memory.
     */
    public void applyFilterSort() {
        String keyword = txtSearch.getText().trim();
        List<SinhVien> base;
        try {
            base = dao.search(keyword, searchCriteria);
        } catch (SQLException ex) {
            base = filterInMemory(keyword, searchCriteria);
        }
        if (sortIndex >= 0) {
            String sortBy;
            boolean asc;
            switch (sortIndex) {
                case 0: sortBy = "TEN"; asc = true; break;
                case 1: sortBy = "TEN"; asc = false; break;
                case 2: sortBy = "HOTEN"; asc = true; break;
                case 3: sortBy = "HOTEN"; asc = false; break;
                case 4: sortBy = "DIEM"; asc = true; break;
                case 5: sortBy = "DIEM"; asc = false; break;
                case 6: sortBy = "MASV"; asc = true; break;
                case 7: sortBy = "MASV"; asc = false; break;
                default: sortBy = "TEN"; asc = true; break;
            }
            base.sort(buildComparator(sortBy, asc));
        }
        viewList = new ArrayList<>(base);
        renderBatch(viewList);
        updateFilterBadge();
        if (viewList.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Không tìm thấy sinh viên nào phù hợp với từ khóa: '" + keyword + "'",
                    "Kết quả tìm kiếm", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private List<SinhVien> filterInMemory(String keyword, String criteria) {
        if (masterList.isEmpty()) {
            loadFallbackStudents();
        }
        if (keyword == null || keyword.isEmpty()) {
            return new ArrayList<>(masterList);
        }
        String lowerKey = keyword.toLowerCase(Locale.ROOT);
        List<SinhVien> filtered = new ArrayList<>();
        for (SinhVien sv : masterList) {
            boolean matches = false;
            switch (criteria != null ? criteria : "Tất cả") {
                case "Mã SV":
                    matches = sv.getMaSV().toLowerCase(Locale.ROOT).contains(lowerKey);
                    break;
                case "Họ tên":
                    matches = sv.getHoTen().toLowerCase(Locale.ROOT).contains(lowerKey);
                    break;
                case "Lớp":
                    matches = sv.getLop().toLowerCase(Locale.ROOT).contains(lowerKey);
                    break;
                default:
                    matches = sv.getMaSV().toLowerCase(Locale.ROOT).contains(lowerKey)
                            || sv.getHoTen().toLowerCase(Locale.ROOT).contains(lowerKey)
                            || sv.getLop().toLowerCase(Locale.ROOT).contains(lowerKey);
                    break;
            }
            if (matches) {
                filtered.add(sv);
            }
        }
        return filtered;
    }

    private java.util.Comparator<SinhVien> buildComparator(String sortBy, boolean asc) {
        java.text.Collator viCollator = java.text.Collator.getInstance(new Locale("vi", "VN"));
        java.util.Comparator<SinhVien> comp;
        switch (sortBy) {
            case "TEN":
                comp = (a, b) -> viCollator.compare(a.getTen(), b.getTen());
                break;
            case "HOTEN":
                comp = (a, b) -> viCollator.compare(a.getHoTen(), b.getHoTen());
                break;
            case "DIEM":
                comp = (a, b) -> Float.compare(a.getDiemTB(), b.getDiemTB());
                break;
            case "MASV":
                comp = (a, b) -> a.getMaSV().compareToIgnoreCase(b.getMaSV());
                break;
            default:
                comp = (a, b) -> viCollator.compare(a.getTen(), b.getTen());
                break;
        }
        if (!asc) {
            comp = comp.reversed();
        }
        return comp;
    }

    private void loadFallbackStudents() {
        File fallbackFile = new File("sinhvien.txt");
        if (!fallbackFile.exists()) {
            fallbackFile = new File("src/main/resources/sinhvien.txt");
        }
        if (fallbackFile.exists()) {
            try {
                masterList = dao.importFromFile(fallbackFile);
                viewList = new ArrayList<>(masterList);
                if (kpiPanel != null) {
                    kpiPanel.update(masterList);
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Hiển thị kết quả thống kê sinh viên (Public để MainFrame và Sidebar có thể gọi lại).
     */
    public void showStatisticDialog() {
        try {
            int total = dao.getCount();
            Map<String, Double> avgByClass = dao.getAverageScoreByClass();
            List<SinhVien> valedictorians = dao.getValedictorians();

            StringBuilder sb = new StringBuilder();
            sb.append("=== KẾT QUẢ THỐNG KÊ SINH VIÊN ===\n\n");
            sb.append("1. Tổng số lượng sinh viên hiện có: ").append(total).append(" sinh viên\n\n");

            sb.append("2. Điểm trung bình theo từng lớp:\n");
            if (avgByClass.isEmpty()) {
                sb.append("   (Chưa có dữ liệu lớp)\n");
            } else {
                for (Map.Entry<String, Double> entry : avgByClass.entrySet()) {
                    sb.append("   - Lớp ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" điểm\n");
                }
            }
            sb.append("\n");

            sb.append("3. Sinh viên có điểm cao nhất (Thủ khoa):\n");
            if (valedictorians.isEmpty()) {
                sb.append("   (Chưa có dữ liệu)\n");
            } else {
                for (SinhVien sv : valedictorians) {
                    sb.append("   ★ ").append(sv.getMaSV()).append(" - ").append(sv.getHoTen())
                            .append(" (Lớp: ").append(sv.getLop()).append(") - Điểm TB: ")
                            .append(sv.getDiemTB()).append(" (").append(sv.getXepLoai()).append(")\n");
                }
            }

            JTextArea txtArea = new JTextArea(sb.toString(), 15, 45);
            txtArea.setFont(new Font("Consolas", Font.PLAIN, 14));
            txtArea.setEditable(false);
            txtArea.setCaretPosition(0);

            JOptionPane.showMessageDialog(this, new JScrollPane(txtArea),
                    "Thống kê sinh viên", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException ex) {
            // Fallback: thống kê in-memory nếu mất kết nối MySQL
            showStatisticDialogInMemory();
        }
    }

    private void showStatisticDialogInMemory() {
        if (masterList.isEmpty()) {
            loadFallbackStudents();
        }
        int total = masterList.size();
        java.util.Map<String, List<Float>> classScores = new java.util.HashMap<>();
        float maxScore = 0.0f;
        List<SinhVien> valedictorians = new ArrayList<>();

        for (SinhVien sv : masterList) {
            classScores.computeIfAbsent(sv.getLop(), k -> new ArrayList<>()).add(sv.getDiemTB());
            if (sv.getDiemTB() > maxScore) {
                maxScore = sv.getDiemTB();
                valedictorians.clear();
                valedictorians.add(sv);
            } else if (sv.getDiemTB() == maxScore && maxScore > 0) {
                valedictorians.add(sv);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== KẾT QUẢ THỐNG KÊ SINH VIÊN (OFFLINE) ===\n\n");
        sb.append("1. Tổng số lượng sinh viên hiện có: ").append(total).append(" sinh viên\n\n");

        sb.append("2. Điểm trung bình theo từng lớp:\n");
        for (Map.Entry<String, List<Float>> entry : classScores.entrySet()) {
            double avg = entry.getValue().stream().mapToDouble(Float::doubleValue).average().orElse(0.0);
            sb.append("   - Lớp ").append(entry.getKey()).append(": ")
                    .append(Math.round(avg * 100.0) / 100.0).append(" điểm\n");
        }
        sb.append("\n");

        sb.append("3. Sinh viên có điểm cao nhất (Thủ khoa):\n");
        for (SinhVien sv : valedictorians) {
            sb.append("   ★ ").append(sv.getMaSV()).append(" - ").append(sv.getHoTen())
                    .append(" (Lớp: ").append(sv.getLop()).append(") - Điểm TB: ")
                    .append(sv.getDiemTB()).append(" (").append(sv.getXepLoai()).append(")\n");
        }

        JTextArea txtArea = new JTextArea(sb.toString(), 15, 45);
        txtArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        txtArea.setEditable(false);
        txtArea.setCaretPosition(0);

        JOptionPane.showMessageDialog(this, new JScrollPane(txtArea),
                "Thống kê sinh viên (Dự phòng)", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onExportFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("."));
        fileChooser.setDialogTitle("Chọn nơi lưu file Text");
        fileChooser.setSelectedFile(new File("sinhvien.txt"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Tập tin văn bản (*.txt)", "txt"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".txt")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".txt");
            }

            try {
                List<SinhVien> list = masterList.isEmpty() ? dao.getAll() : masterList;
                dao.exportToFile(fileToSave, list);
                JOptionPane.showMessageDialog(this,
                        "Xuất thành công " + list.size() + " sinh viên ra file:\n" + fileToSave.getAbsolutePath(),
                        "Xuất File Thành Công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi khi xuất file: " + ex.getMessage(),
                        "Lỗi xuất file", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onImportFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("."));
        fileChooser.setDialogTitle("Chọn tập tin văn bản cần nạp");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Tập tin văn bản (*.txt)", "txt"));

        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                List<SinhVien> importedList = dao.importFromFile(fileToOpen);
                if (importedList.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Tập tin không có dữ liệu sinh viên hợp lệ!",
                            "Tập tin rỗng", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                Object[] options = {"Đồng bộ vào CSDL MySQL", "Chỉ xem trước trên bảng", "Hủy bỏ"};
                int choice = JOptionPane.showOptionDialog(this,
                        "Đã đọc thành công " + importedList.size() + " sinh viên từ file.\nBạn muốn thực hiện thao tác nào?",
                        "Tùy chọn nạp dữ liệu",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null, options, options[0]);

                if (choice == 0) {
                    int inserted = 0;
                    int updated = 0;
                    for (SinhVien sv : importedList) {
                        if (dao.existsById(sv.getMaSV())) {
                            dao.update(sv);
                            updated++;
                        } else {
                            dao.insert(sv);
                            inserted++;
                        }
                    }
                    JOptionPane.showMessageDialog(this,
                            "Đồng bộ thành công vào CSDL MySQL!\n- Thêm mới: " + inserted + "\n- Cập nhật: " + updated,
                            "Nạp file thành công", JOptionPane.INFORMATION_MESSAGE);
                    refreshAfterMutation();
                } else if (choice == 1) {
                    viewList = new ArrayList<>(importedList);
                    renderBatch(viewList);
                    JOptionPane.showMessageDialog(this,
                            "Đang hiển thị " + importedList.size() + " sinh viên từ file lên bảng!",
                            "Hiển thị bảng", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi khi đọc file: " + ex.getMessage(),
                        "Lỗi nạp file", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // --- Getters phục vụ kiểm thử tự động & tích hợp hệ thống ---
    public JTable getTable() {
        return tblSinhVien;
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public JTextField getTxtMaSV() {
        return txtMaSV;
    }

    public JTextField getTxtHoTen() {
        return txtHoTen;
    }

    public JTextField getTxtLop() {
        return txtLop;
    }

    public JTextField getTxtNgaySinh() {
        return txtNgaySinh;
    }

    public JTextField getTxtDiemTB() {
        return txtDiemTB;
    }

    public JButton getBtnAdd() {
        return btnAdd;
    }

    public JButton getBtnUpdate() {
        return btnUpdate;
    }

    public JButton getBtnDelete() {
        return btnDelete;
    }

    public JButton getBtnClear() {
        return btnClear;
    }

    /** Giữ tương thích test: áp dụng sắp xếp hiện tại của bộ lọc thống nhất. */
    public void triggerSort() {
        applyFilterSort();
    }

    public void setSortIndex(int index) {
        this.sortIndex = index;
        syncFilterPopup();
        updateFilterBadge();
    }

    public int getSortIndex() {
        return sortIndex;
    }

    public void setSearchCriteria(String criteria) {
        this.searchCriteria = criteria;
        syncFilterPopup();
        updateFilterBadge();
    }

    public String getSearchCriteria() {
        return searchCriteria;
    }

    public JButton getBtnFilter() {
        return btnFilter;
    }

    public KpiCardsPanel getKpiPanel() {
        return kpiPanel;
    }

    public ModernCardPanel getLeftFormCard() {
        return leftFormCard;
    }

    public ModernCardPanel getRightTableCard() {
        return rightTableCard;
    }

    public JButton getBtnMaximize() {
        return btnMaximize;
    }

    public boolean isFormCollapsed() {
        return currentState == ViewState.FORM_COLLAPSED;
    }

    public boolean isMaximized() {
        return currentState == ViewState.MAXIMIZED;
    }

    public JTextField getTxtSearch() {
        return txtSearch;
    }

    public JButton getBtnSearch() {
        return btnSearch;
    }

    public JPopupMenu getFilterPopup() {
        return filterPopup;
    }

    public JButton getBtnRefresh() {
        return btnRefresh;
    }

    public JButton getBtnStatistic() {
        return btnStatistic;
    }

    public JButton getBtnBangDiem() {
        return btnBangDiem;
    }

    public JButton getBtnOpenBangDiem() {
        return btnOpenBangDiem;
    }

    public JPopupMenu getTableContextMenu() {
        return tableContextMenu;
    }

    public DatePicker getDatePickerNgaySinh() {
        return datePickerNgaySinh;
    }

    public JButton getBtnExportFile() {
        return btnExportFile;
    }

    public JButton getBtnImportFile() {
        return btnImportFile;
    }

    public int getDensity() {
        return tblSinhVien != null ? tblSinhVien.getRowHeight() : density;
    }

    public List<SinhVien> getMasterList() {
        return masterList;
    }

    public List<SinhVien> getViewList() {
        return viewList;
    }
}
