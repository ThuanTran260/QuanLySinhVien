package com.quanlysinhvien.ui;

import com.quanlysinhvien.dao.SinhVienDAO;
import com.quanlysinhvien.model.SinhVien;
import com.quanlysinhvien.ui.animation.SlideTabbedPane;
import com.quanlysinhvien.ui.components.KpiCardsPanel;
import com.quanlysinhvien.ui.theme.ModernButton;
import com.quanlysinhvien.ui.theme.ModernCardPanel;
import com.quanlysinhvien.ui.theme.ModernTableRenderer;
import com.quanlysinhvien.ui.theme.UITheme;

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
    private int density = 34;

    // Components
    private JPanel headerPanel;
    private JPanel mainContentPanel;
    private JPanel upperPanel;
    private ModernCardPanel topCard;
    private JPanel inputPanel;
    private JPanel crudBtnBox;
    private ModernCardPanel bottomCard;
    private JScrollPane scrollPane;
    private KpiCardsPanel kpiPanel;

    // Form inputs
    private JTextField txtMaSV;
    private JTextField txtHoTen;
    private JTextField txtLop;
    private JTextField txtNgaySinh;
    private JTextField txtDiemTB;

    // Search and Sort
    private JTextField txtSearch;
    private JComboBox<String> cbSearchCriteria;
    private JComboBox<String> cbSort;

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
    private JButton btnSort;
    private JButton btnStatistic;
    private JButton btnExportFile;
    private JButton btnImportFile;
    private JButton btnToggleForm;
    private JButton btnDensity;
    private JButton btnMaximize;

    private final DecimalFormat scoreFormat = new DecimalFormat("#0.0#",
            DecimalFormatSymbols.getInstance(Locale.US));

    public StudentManagementPanel() {
        initUI();
        loadDataToTable();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.CANVAS_BG);
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        // 1. Header Title
        headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("CHƯƠNG TRÌNH QUẢN LÝ SINH VIÊN", SwingConstants.CENTER);
        lblTitle.setFont(UITheme.FONT_TITLE_LARGE);
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitle = new JLabel("Hệ thống quản lý thông tin sinh viên, điểm số và tra cứu nhanh", SwingConstants.CENTER);
        lblSubtitle.setFont(UITheme.FONT_REGULAR);
        lblSubtitle.setForeground(UITheme.TEXT_SECONDARY);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(lblTitle);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        headerPanel.add(lblSubtitle);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Main Center Content
        mainContentPanel = new JPanel(new BorderLayout(0, 10));
        mainContentPanel.setOpaque(false);

        // --- Upper Container: KPI Cards + Collapsible Form Card ---
        upperPanel = new JPanel(new BorderLayout(0, 10));
        upperPanel.setOpaque(false);

        // 2.1 KPI Cards Panel
        kpiPanel = new KpiCardsPanel();
        upperPanel.add(kpiPanel, BorderLayout.NORTH);

        // 2.2 Form Card (Collapsible)
        topCard = new ModernCardPanel(new BorderLayout(10, 8), 16);

        // Form Header Bar: Title + Toggle Collapse Button
        JPanel formHeaderBar = new JPanel(new BorderLayout());
        formHeaderBar.setOpaque(false);

        JLabel lblFormTitle = new JLabel("Thông tin chi tiết sinh viên");
        lblFormTitle.setFont(UITheme.FONT_TITLE_MEDIUM);
        lblFormTitle.setForeground(UITheme.TEXT_PRIMARY);
        formHeaderBar.add(lblFormTitle, BorderLayout.WEST);

        btnToggleForm = new ModernButton("Thu gọn Form ▲", UITheme.NEUTRAL_BTN_BG, UITheme.NEUTRAL_BTN_HOVER, UITheme.NEUTRAL_BTN_TEXT);
        ((ModernButton) btnToggleForm).setBorderColor(UITheme.NEUTRAL_BTN_BORDER);
        btnToggleForm.setPreferredSize(new Dimension(135, 28));
        btnToggleForm.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnToggleForm.addActionListener(e -> toggleFormCollapse());
        formHeaderBar.add(btnToggleForm, BorderLayout.EAST);

        topCard.add(formHeaderBar, BorderLayout.NORTH);

        // Grid Inputs
        inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: MaSV & Lop
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.12;
        JLabel lblMaSV = new JLabel("Mã Sinh Viên (*):");
        lblMaSV.setFont(UITheme.FONT_BOLD);
        lblMaSV.setForeground(UITheme.TEXT_PRIMARY);
        inputPanel.add(lblMaSV, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.38;
        txtMaSV = new JTextField();
        UITheme.styleTextField(txtMaSV);
        inputPanel.add(txtMaSV, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.12;
        JLabel lblLop = new JLabel("Lớp (*):");
        lblLop.setFont(UITheme.FONT_BOLD);
        lblLop.setForeground(UITheme.TEXT_PRIMARY);
        inputPanel.add(lblLop, gbc);

        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 0.38;
        txtLop = new JTextField();
        UITheme.styleTextField(txtLop);
        inputPanel.add(txtLop, gbc);

        // Row 1: HoTen & NgaySinh
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.12;
        JLabel lblHoTen = new JLabel("Họ và Tên (*):");
        lblHoTen.setFont(UITheme.FONT_BOLD);
        lblHoTen.setForeground(UITheme.TEXT_PRIMARY);
        inputPanel.add(lblHoTen, gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.38;
        txtHoTen = new JTextField();
        UITheme.styleTextField(txtHoTen);
        inputPanel.add(txtHoTen, gbc);

        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0.12;
        JLabel lblNgaySinh = new JLabel("Ngày Sinh (yyyy-MM-dd):");
        lblNgaySinh.setFont(UITheme.FONT_BOLD);
        lblNgaySinh.setForeground(UITheme.TEXT_PRIMARY);
        inputPanel.add(lblNgaySinh, gbc);

        gbc.gridx = 3; gbc.gridy = 1; gbc.weightx = 0.38;
        txtNgaySinh = new JTextField();
        UITheme.styleTextField(txtNgaySinh);
        inputPanel.add(txtNgaySinh, gbc);

        // Row 2: DiemTB & Notes
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.12;
        JLabel lblDiemTB = new JLabel("Điểm Trung Bình (0-10):");
        lblDiemTB.setFont(UITheme.FONT_BOLD);
        lblDiemTB.setForeground(UITheme.TEXT_PRIMARY);
        inputPanel.add(lblDiemTB, gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.38;
        txtDiemTB = new JTextField();
        UITheme.styleTextField(txtDiemTB);
        inputPanel.add(txtDiemTB, gbc);

        gbc.gridx = 2; gbc.gridy = 2; gbc.gridwidth = 2;
        JLabel lblNote = new JLabel("(*) Bắt buộc | Điểm: 0.0 - 10.0 | Ngày sinh: ví dụ 2003-05-15");
        lblNote.setFont(UITheme.FONT_SMALL);
        lblNote.setForeground(UITheme.TEXT_MUTED);
        inputPanel.add(lblNote, gbc);
        gbc.gridwidth = 1;

        topCard.add(inputPanel, BorderLayout.CENTER);

        // Action CRUD Buttons Bar
        crudBtnBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 6));
        crudBtnBox.setOpaque(false);

        btnAdd = new ModernButton("Thêm mới", UITheme.PRIMARY);
        btnAdd.setPreferredSize(new Dimension(115, 32));

        btnUpdate = new ModernButton("Cập nhật (Sửa)", UITheme.SUCCESS);
        btnUpdate.setPreferredSize(new Dimension(135, 32));

        btnDelete = new ModernButton("Xóa sinh viên", UITheme.DANGER);
        btnDelete.setPreferredSize(new Dimension(125, 32));

        btnClear = new ModernButton("Làm mới form", UITheme.NEUTRAL_BTN_BG, UITheme.NEUTRAL_BTN_HOVER, UITheme.NEUTRAL_BTN_TEXT);
        ((ModernButton) btnClear).setBorderColor(UITheme.NEUTRAL_BTN_BORDER);
        btnClear.setPreferredSize(new Dimension(125, 32));

        crudBtnBox.add(btnAdd);
        crudBtnBox.add(btnUpdate);
        crudBtnBox.add(btnDelete);
        crudBtnBox.add(btnClear);

        topCard.add(crudBtnBox, BorderLayout.SOUTH);
        upperPanel.add(topCard, BorderLayout.CENTER);

        mainContentPanel.add(upperPanel, BorderLayout.NORTH);

        // --- Bottom Card: 2-Tier Toolbar & Data Table ---
        bottomCard = new ModernCardPanel(new BorderLayout(6, 6), 16);

        // Toolbar Panel: 2 Tầng Cố Định Chiều Cao (Chống wrap layout khi co giãn cửa sổ)
        JPanel toolBarPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        toolBarPanel.setOpaque(false);

        // TẦNG 1: Tìm kiếm & Sắp xếp
        JPanel tier1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        tier1.setOpaque(false);

        JLabel lblSearchBy = new JLabel("Tìm theo:");
        lblSearchBy.setFont(UITheme.FONT_BOLD);
        lblSearchBy.setForeground(UITheme.TEXT_PRIMARY);
        tier1.add(lblSearchBy);

        cbSearchCriteria = new JComboBox<>(new String[]{"Tất cả", "Mã SV", "Họ tên", "Lớp"});
        UITheme.styleComboBox(cbSearchCriteria);
        tier1.add(cbSearchCriteria);

        txtSearch = new JTextField(12);
        UITheme.styleTextField(txtSearch);
        tier1.add(txtSearch);

        btnSearch = new ModernButton("Tìm kiếm", UITheme.PRIMARY);
        btnSearch.setPreferredSize(new Dimension(90, 30));
        tier1.add(btnSearch);

        btnRefresh = new ModernButton("Tất cả", UITheme.NEUTRAL_BTN_BG, UITheme.NEUTRAL_BTN_HOVER, UITheme.NEUTRAL_BTN_TEXT);
        ((ModernButton) btnRefresh).setBorderColor(UITheme.NEUTRAL_BTN_BORDER);
        btnRefresh.setPreferredSize(new Dimension(75, 30));
        tier1.add(btnRefresh);

        tier1.add(createToolbarSeparator());

        JLabel lblSortBy = new JLabel("Sắp xếp:");
        lblSortBy.setFont(UITheme.FONT_BOLD);
        lblSortBy.setForeground(UITheme.TEXT_PRIMARY);
        tier1.add(lblSortBy);

        cbSort = new JComboBox<>(new String[]{
                "Tên (A-Z)",
                "Tên (Z-A)",
                "Họ và Tên (A-Z)",
                "Họ và Tên (Z-A)",
                "Điểm TB (Tăng dần)",
                "Điểm TB (Giảm dần)",
                "Mã SV (Tăng dần)",
                "Mã SV (Giảm dần)"
        });
        UITheme.styleComboBox(cbSort);
        tier1.add(cbSort);

        btnSort = new ModernButton("Sắp xếp", UITheme.PRIMARY);
        btnSort.setPreferredSize(new Dimension(85, 30));
        tier1.add(btnSort);

        toolBarPanel.add(tier1);

        // TẦNG 2: Thống kê, File IO, Mật độ dòng & Phóng to
        JPanel tier2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        tier2.setOpaque(false);

        btnStatistic = new ModernButton("Thống kê", new Color(0x7C, 0x3A, 0xED));
        btnStatistic.setPreferredSize(new Dimension(95, 30));
        tier2.add(btnStatistic);

        btnExportFile = new ModernButton("Xuất Text File", UITheme.NEUTRAL_BTN_BG, UITheme.NEUTRAL_BTN_HOVER, UITheme.NEUTRAL_BTN_TEXT);
        ((ModernButton) btnExportFile).setBorderColor(UITheme.NEUTRAL_BTN_BORDER);
        btnExportFile.setPreferredSize(new Dimension(115, 30));
        tier2.add(btnExportFile);

        btnImportFile = new ModernButton("Nạp từ File", UITheme.NEUTRAL_BTN_BG, UITheme.NEUTRAL_BTN_HOVER, UITheme.NEUTRAL_BTN_TEXT);
        ((ModernButton) btnImportFile).setBorderColor(UITheme.NEUTRAL_BTN_BORDER);
        btnImportFile.setPreferredSize(new Dimension(100, 30));
        tier2.add(btnImportFile);

        tier2.add(createToolbarSeparator());

        // Density Button (26px ↔ 34px)
        btnDensity = new ModernButton("Mật độ: 34px", UITheme.NEUTRAL_BTN_BG, UITheme.NEUTRAL_BTN_HOVER, UITheme.NEUTRAL_BTN_TEXT);
        ((ModernButton) btnDensity).setBorderColor(UITheme.NEUTRAL_BTN_BORDER);
        btnDensity.setPreferredSize(new Dimension(105, 30));
        btnDensity.setToolTipText("Chuyển đổi giữa dòng thoáng (34px) và dòng thu gọn (26px)");
        btnDensity.addActionListener(e -> toggleDensity());
        tier2.add(btnDensity);

        // Maximize Button [⛶]
        btnMaximize = new ModernButton("Phóng to [⛶]", new Color(0x0E, 0xA5, 0xE9));
        btnMaximize.setPreferredSize(new Dimension(110, 30));
        btnMaximize.setToolTipText("Phóng to bảng chiếm 100% diện tích (Phím tắt: F11 vào/ra, Esc thoát)");
        btnMaximize.addActionListener(e -> toggleMaximize());
        tier2.add(btnMaximize);

        toolBarPanel.add(tier2);

        bottomCard.add(toolBarPanel, BorderLayout.NORTH);

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

        bottomCard.add(scrollPane, BorderLayout.CENTER);
        mainContentPanel.add(bottomCard, BorderLayout.CENTER);

        add(mainContentPanel, BorderLayout.CENTER);

        // Event Handlers & Key Bindings
        setupEventHandlers();
        setupKeyBindings();
    }

    private JComponent createToolbarSeparator() {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(2, 22));
        sep.setForeground(UITheme.BORDER_COLOR);
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

        btnAdd.addActionListener(e -> onAddStudent());
        btnUpdate.addActionListener(e -> onUpdateStudent());
        btnDelete.addActionListener(e -> onDeleteStudent());
        btnClear.addActionListener(e -> clearFormAndReload());

        btnSearch.addActionListener(e -> onSearch());
        txtSearch.addActionListener(e -> onSearch());

        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            viewList = new ArrayList<>(masterList);
            renderBatch(viewList);
        });

        btnSort.addActionListener(e -> onSort());
        btnStatistic.addActionListener(e -> showStatisticDialog());
        btnExportFile.addActionListener(e -> onExportFile());
        btnImportFile.addActionListener(e -> onImportFile());
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

    public void toggleDensity() {
        withAnimationDisabled(() -> {
            if (density == 34) {
                density = 26;
                btnDensity.setText("Mật độ: 26px");
            } else {
                density = 34;
                btnDensity.setText("Mật độ: 34px");
            }
            tblSinhVien.setRowHeight(density);
            tblSinhVien.repaint();
        });
    }

    private void setViewState(ViewState newState) {
        if (currentState == newState) return;

        int selectedRow = tblSinhVien.getSelectedRow();
        Point scrollPos = scrollPane.getViewport().getViewPosition();

        currentState = newState;

        switch (currentState) {
            case NORMAL:
                headerPanel.setVisible(true);
                upperPanel.setVisible(true);
                kpiPanel.setVisible(true);
                inputPanel.setVisible(true);
                crudBtnBox.setVisible(true);
                btnToggleForm.setText("Thu gọn Form ▲");
                btnMaximize.setText("Phóng to [⛶]");
                break;
            case FORM_COLLAPSED:
                headerPanel.setVisible(true);
                upperPanel.setVisible(true);
                kpiPanel.setVisible(true);
                inputPanel.setVisible(false);
                crudBtnBox.setVisible(false);
                btnToggleForm.setText("Mở rộng Form ▼");
                btnMaximize.setText("Phóng to [⛶]");
                break;
            case MAXIMIZED:
                headerPanel.setVisible(false);
                upperPanel.setVisible(false);
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
        txtDiemTB.setText("");

        txtMaSV.setEditable(true);
        txtMaSV.setBackground(Color.WHITE);
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
            SinhVien sv = SinhVien.validateAndCreate(
                    txtMaSV.getText(),
                    txtHoTen.getText(),
                    txtLop.getText(),
                    txtNgaySinh.getText(),
                    txtDiemTB.getText()
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
            SinhVien sv = SinhVien.validateAndCreate(
                    txtMaSV.getText(),
                    txtHoTen.getText(),
                    txtLop.getText(),
                    txtNgaySinh.getText(),
                    txtDiemTB.getText()
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
        txtDiemTB.setText("");
        txtMaSV.setEditable(true);
        txtMaSV.setBackground(Color.WHITE);
        tblSinhVien.clearSelection();
    }

    private void onSearch() {
        String keyword = txtSearch.getText().trim();
        String criteria = (String) cbSearchCriteria.getSelectedItem();

        try {
            List<SinhVien> list = dao.search(keyword, criteria);
            viewList = new ArrayList<>(list);
            renderBatch(viewList);
            if (viewList.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Không tìm thấy sinh viên nào phù hợp với từ khóa: '" + keyword + "'",
                        "Kết quả tìm kiếm", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            searchInMemory(keyword, criteria);
        }
    }

    private void searchInMemory(String keyword, String criteria) {
        if (masterList.isEmpty()) {
            loadFallbackStudents();
        }
        if (keyword == null || keyword.isEmpty()) {
            viewList = new ArrayList<>(masterList);
            renderBatch(viewList);
            return;
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
        viewList = filtered;
        renderBatch(viewList);
        if (viewList.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Không tìm thấy sinh viên nào phù hợp với từ khóa: '" + keyword + "'",
                    "Kết quả tìm kiếm", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onSort() {
        int sortIndex = cbSort.getSelectedIndex();
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

        try {
            List<SinhVien> list = dao.getAllSorted(sortBy, asc);
            viewList = new ArrayList<>(list);
            renderBatch(viewList);
        } catch (SQLException ex) {
            sortInMemory(sortBy, asc);
        }
    }

    private void sortInMemory(String sortBy, boolean asc) {
        if (masterList.isEmpty()) {
            loadFallbackStudents();
        }
        List<SinhVien> sorted = new ArrayList<>(masterList);
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
        sorted.sort(comp);
        viewList = sorted;
        renderBatch(viewList);
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

    public JComboBox<String> getCbSort() {
        return cbSort;
    }

    public void triggerSort() {
        onSort();
    }

    public KpiCardsPanel getKpiPanel() {
        return kpiPanel;
    }

    public boolean isFormCollapsed() {
        return currentState == ViewState.FORM_COLLAPSED;
    }

    public boolean isMaximized() {
        return currentState == ViewState.MAXIMIZED;
    }

    public int getDensity() {
        return density;
    }

    public List<SinhVien> getMasterList() {
        return masterList;
    }

    public List<SinhVien> getViewList() {
        return viewList;
    }
}
