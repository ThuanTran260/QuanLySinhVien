package com.quanlysinhvien.ui;

import com.quanlysinhvien.dao.DiemDAO;
import com.quanlysinhvien.dao.MonHocDAO;
import com.quanlysinhvien.model.Diem;
import com.quanlysinhvien.model.DiemCalculator;
import com.quanlysinhvien.model.DiemDetail;
import com.quanlysinhvien.model.MonHoc;
import com.quanlysinhvien.ui.theme.ModernButton;
import com.quanlysinhvien.ui.theme.UITheme;
import net.miginfocom.swing.MigLayout;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dialog xem/sửa bảng điểm theo môn của 1 sinh viên (hướng A).
 * Công thức chung: DiemMon = BC*0.4 + CC*0.1 + CK*0.5. Lưu xong ghi đè DiemTB.
 * Mọi truy vấn DB chạy nền (SwingWorker), chỉ render trên EDT.
 */
public class BangDiemDialog extends JDialog {
    private static final String[] COLUMNS = {
            "Mã MH", "Môn học", "TC", "Báo cáo (40%)", "Chuyên cần (10%)", "Cuối kỳ (50%)", "Điểm môn"
    };

    private final String maSV;
    private final DiemDAO diemDAO = new DiemDAO();
    private final MonHocDAO monHocDAO = new MonHocDAO();

    private JLabel lblInfo;
    private JLabel lblTichLuy;
    private JTable table;
    private DefaultTableModel tableModel;
    private JComboBox<MonHoc> cbMonHoc;
    private List<MonHoc> allMonHoc = new ArrayList<>();
    private Runnable onSaved;
    private DefaultCategoryDataset chartDataset;
    private JFreeChart chart;
    private JLabel lblStatus;
    private JButton btnAdd;
    private JButton btnDelete;
    private JButton btnSave;
    private boolean isUpdatingTable = false;
    private boolean isDemoMode = false;
    private boolean suppressDialogsForTest = false;

    public void setSuppressDialogsForTest(boolean suppress) {
        this.suppressDialogsForTest = suppress;
    }

    public boolean isSuppressDialogsForTest() {
        return suppressDialogsForTest;
    }

    private void showMessage(String message, String title, int messageType) {
        if (!suppressDialogsForTest) {
            JOptionPane.showMessageDialog(this, message, title, messageType);
        }
    }

    public BangDiemDialog(java.awt.Window owner, String maSV, String hoTen, String lop) {
        super(owner, "Bảng điểm theo môn - " + maSV, ModalityType.APPLICATION_MODAL);
        this.maSV = maSV.trim();
        initUI(hoTen, lop);
        loadData();
    }

    public BangDiemDialog(Frame owner, String maSV, String hoTen, String lop) {
        this((java.awt.Window) owner, maSV, hoTen, lop);
    }

    private void initUI(String hoTen, String lop) {
        setSize(760, 620);
        setMinimumSize(new Dimension(640, 520));
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(UITheme.CANVAS_BG);
        getRootPane().setDefaultButton(null);

        JPanel root = new JPanel(new MigLayout("wrap 1, fillx, insets 16", "[grow, fill]", "[][][grow][]20[]"));
        root.setBackground(UITheme.CANVAS_BG);
        getContentPane().add(root, BorderLayout.CENTER);

        // 1. Thông tin SV
        lblInfo = new JLabel(maSV + " - " + hoTen + " (Lớp: " + lop + ")");
        lblInfo.setFont(UITheme.FONT_TITLE_MEDIUM);
        lblInfo.setForeground(UITheme.TEXT_PRIMARY);
        root.add(lblInfo, "growx");

        lblTichLuy = new JLabel("TB tích lũy: …");
        lblTichLuy.setFont(UITheme.FONT_BOLD);
        lblTichLuy.setForeground(UITheme.PRIMARY);
        root.add(lblTichLuy, "growx");

        // 2. Bảng điểm (3 cột điểm sửa được, còn lại khóa)
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3 || column == 4 || column == 5;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 2: return Integer.class;
                    case 3:
                    case 4:
                    case 5:
                    case 6: return Double.class;
                    default: return String.class;
                }
            }

            @Override
            public void setValueAt(Object aValue, int row, int column) {
                if (column >= 3 && column <= 6 && aValue != null) {
                    try {
                        double val = toDouble(aValue);
                        super.setValueAt(val, row, column);
                        return;
                    } catch (Exception ignored) {
                    }
                }
                super.setValueAt(aValue, row, column);
            }
        };

        // TableModelListener: phản ứng thời gian thực khi chỉnh sửa cột điểm 3, 4, 5
        tableModel.addTableModelListener(e -> {
            if (isUpdatingTable) {
                return;
            }
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int col = e.getColumn();
                if (col == 3 || col == 4 || col == 5) {
                    int row = e.getFirstRow();
                    if (row >= 0 && row < tableModel.getRowCount()) {
                        double dm = 0.0;
                        try {
                            double bc = toDouble(tableModel.getValueAt(row, 3));
                            double cc = toDouble(tableModel.getValueAt(row, 4));
                            double ck = toDouble(tableModel.getValueAt(row, 5));
                            dm = DiemCalculator.diemMon(bc, cc, ck);
                        } catch (Exception ignored) {
                        }
                        isUpdatingTable = true;
                        try {
                            tableModel.setValueAt(dm, row, 6);
                        } finally {
                            isUpdatingTable = false;
                        }
                        refreshTichLuyFromTable();
                    }
                }
            }
        });

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(UITheme.TABLE_SELECTION_BG);
        table.setSelectionForeground(UITheme.TABLE_SELECTION_TEXT);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(UITheme.BORDER_COLOR);
        table.getTableHeader().setFont(UITheme.FONT_BOLD);
        table.getTableHeader().setBackground(UITheme.TABLE_HEADER_BG);
        table.getTableHeader().setForeground(UITheme.TABLE_HEADER_TEXT);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(2).setPreferredWidth(40);

        // Render định dạng số thực 2 chữ số tránh lỗi hiển thị 8.1999998...
        javax.swing.table.DefaultTableCellRenderer scoreRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                if (value instanceof Number) {
                    setText(String.format(java.util.Locale.US, "%.2f", ((Number) value).doubleValue()));
                }
                setHorizontalAlignment(SwingConstants.RIGHT);
                return this;
            }
        };
        table.setDefaultRenderer(Double.class, scoreRenderer);
        table.setDefaultEditor(Double.class, new GradeCellEditor());

        javax.swing.table.DefaultTableCellRenderer centerRenderer = new javax.swing.table.DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Integer.class, centerRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(700, 200));
        root.add(scrollPane, "grow, push, h 200!");

        // 3. Thêm/xóa môn
        JPanel addPanel = new JPanel(new MigLayout("insets 0", "[grow, fill][][]", "[]"));
        addPanel.setOpaque(false);
        cbMonHoc = new JComboBox<>();
        UITheme.styleComboBox(cbMonHoc);
        addPanel.add(new JLabel("Thêm môn:"), "w 70!");
        addPanel.add(cbMonHoc, "growx");
        btnAdd = new ModernButton("Thêm", UITheme.SUCCESS);
        btnAdd.setPreferredSize(new Dimension(90, 32));
        btnAdd.addActionListener(e -> onAddMonHoc());
        addPanel.add(btnAdd, "");
        btnDelete = new ModernButton("Xóa môn", UITheme.DANGER);
        btnDelete.setPreferredSize(new Dimension(90, 32));
        btnDelete.addActionListener(e -> onDeleteMonHoc());
        addPanel.add(btnDelete, "wrap");
        root.add(addPanel, "growx");

        // 4. Biểu đồ điểm môn (JFreeChart giới hạn trục Y trong [0.0, 10.0])
        chartDataset = new DefaultCategoryDataset();
        chart = ChartFactory.createBarChart(
                "Điểm môn", "Môn học", "Điểm", chartDataset,
                PlotOrientation.VERTICAL, false, false, false);
        chart.setBackgroundPaint(UITheme.CARD_BG);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(UITheme.CANVAS_BG);
        plot.getRenderer().setSeriesPaint(0, UITheme.PRIMARY);
        plot.getRangeAxis().setRange(0.0, 10.0);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(700, 180));
        chartPanel.setMouseWheelEnabled(false);
        chartPanel.setBackground(UITheme.CARD_BG);
        root.add(chartPanel, "growx, h 180!");

        // 5. Footer
        JPanel footer = new JPanel(new MigLayout("insets 0", "[grow, fill][][]", "[]"));
        footer.setOpaque(false);
        lblStatus = new JLabel(" ");
        lblStatus.setFont(UITheme.FONT_SMALL);
        lblStatus.setForeground(UITheme.TEXT_MUTED);
        footer.add(lblStatus, "growx");
        btnSave = new ModernButton("Lưu", UITheme.PRIMARY);
        btnSave.setPreferredSize(new Dimension(100, 34));
        btnSave.addActionListener(e -> onSave());
        footer.add(btnSave, "");
        getRootPane().setDefaultButton((JButton) btnSave);
        JButton btnClose = new ModernButton("Đóng", UITheme.NEUTRAL_BTN_BG,
                UITheme.NEUTRAL_BTN_HOVER, UITheme.NEUTRAL_BTN_TEXT);
        ((ModernButton) btnClose).setBorderColor(UITheme.NEUTRAL_BTN_BORDER);
        btnClose.setPreferredSize(new Dimension(90, 34));
        btnClose.addActionListener(e -> dispose());
        footer.add(btnClose, "");
        root.add(footer, "growx");

        // Esc đóng
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void loadData() {
        lblStatus.setText("Đang tải bảng điểm…");
        new SwingWorker<List<DiemDetail>, Void>() {
            private List<MonHoc> mhs;
            private boolean demo = false;

            @Override
            protected List<DiemDetail> doInBackground() throws Exception {
                try {
                    mhs = monHocDAO.getAll();
                    return diemDAO.getByMaSV(maSV);
                } catch (Exception ex) {
                    demo = true;
                    mhs = MonHocDAO.seedData();
                    List<DiemDetail> demoList = new ArrayList<>();
                    for (int i = 0; i < Math.min(4, mhs.size()); i++) {
                        MonHoc m = mhs.get(i);
                        demoList.add(new DiemDetail(maSV, m.getMaMH(), m.getTenMH(), m.getSoTC(),
                                8.0f, 8.5f, 7.5f));
                    }
                    return demoList;
                }
            }

            @Override
            protected void done() {
                try {
                    isDemoMode = demo;
                    allMonHoc = (mhs != null && !mhs.isEmpty()) ? mhs : MonHocDAO.seedData();
                    List<DiemDetail> list = get();
                    renderTable(list);
                    if (isDemoMode) {
                        lblStatus.setText("Chế độ demo (CSDL offline) — 4 môn mẫu.");
                    } else {
                        lblStatus.setText(" ");
                    }
                } catch (Exception ex) {
                    isDemoMode = true;
                    allMonHoc = MonHocDAO.seedData();
                    lblStatus.setText("Chế độ demo (CSDL offline).");
                    renderTable(new ArrayList<>());
                }
                refreshCombo();
            }
        }.execute();
    }

    private void renderTable(List<DiemDetail> list) {
        isUpdatingTable = true;
        try {
            tableModel.setRowCount(0);
            List<Double> diemMons = new ArrayList<>();
            List<Integer> tinChis = new ArrayList<>();
            for (DiemDetail d : list) {
                double dm = DiemCalculator.round2(d.getDiemMon());
                diemMons.add(dm);
                tinChis.add(d.getSoTC());
                tableModel.addRow(new Object[]{
                        d.getMaMH(), d.getTenMH(), d.getSoTC(),
                        DiemCalculator.round2(d.getDiemBaoCao()),
                        DiemCalculator.round2(d.getDiemChuyenCan()),
                        DiemCalculator.round2(d.getDiemCuoiKy()),
                        dm
                });
            }
            updateTichLuy(diemMons, tinChis);
            refreshChart();
        } finally {
            isUpdatingTable = false;
        }
        refreshCombo();
    }

    private void updateTichLuy(List<Double> diemMons, List<Integer> tinChis) {
        if (diemMons.isEmpty()) {
            lblTichLuy.setText("TB tích lũy: — (0 môn)");
        } else {
            lblTichLuy.setText(String.format(java.util.Locale.US, "TB tích lũy: %.2f (%d môn)",
                    DiemCalculator.diemTBTinChi(diemMons, tinChis), diemMons.size()));
        }
    }

    private void refreshChart() {
        chartDataset.clear();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String maMH = String.valueOf(tableModel.getValueAt(i, 0));
            Object dm = tableModel.getValueAt(i, 6);
            double v = dm instanceof Number ? ((Number) dm).doubleValue() : 0;
            chartDataset.addValue(v, "Điểm môn", maMH);
        }
    }

    private void refreshCombo() {
        Set<String> learned = new HashSet<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            learned.add(String.valueOf(tableModel.getValueAt(i, 0)));
        }
        cbMonHoc.removeAllItems();
        for (MonHoc mh : allMonHoc) {
            if (!learned.contains(mh.getMaMH())) {
                cbMonHoc.addItem(mh);
            }
        }
    }

    private void onAddMonHoc() {
        MonHoc mh = (MonHoc) cbMonHoc.getSelectedItem();
        if (mh == null) {
            showMessage("Sinh viên đã học hết môn trong danh sách!",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Môn mới để trống 3 ô điểm cho người dùng nhập tay (không prefill điểm mẫu).
        // null hiển thị blank, TB/chart preview tự bỏ qua cho tới khi nhập đủ.
        isUpdatingTable = true;
        try {
            tableModel.addRow(new Object[]{mh.getMaMH(), mh.getTenMH(), mh.getSoTC(), null, null, null, null});
        } finally {
            isUpdatingTable = false;
        }
        refreshCombo();
        refreshTichLuyFromTable();
    }

    private void onDeleteMonHoc() {
        int row = table.getSelectedRow();
        if (row < 0) {
            showMessage("Chọn 1 môn trên bảng để xóa!",
                    "Chưa chọn", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (tableModel.getRowCount() <= 1) {
            showMessage("Mỗi sinh viên giữ tối thiểu 1 môn!",
                    "Không thể xóa", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        isUpdatingTable = true;
        try {
            tableModel.removeRow(modelRow);
        } finally {
            isUpdatingTable = false;
        }
        refreshCombo();
        refreshTichLuyFromTable();
    }

    private void refreshTichLuyFromTable() {
        List<Double> diemMons = new ArrayList<>();
        List<Integer> tinChis = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                double bc = toDouble(tableModel.getValueAt(i, 3));
                double cc = toDouble(tableModel.getValueAt(i, 4));
                double ck = toDouble(tableModel.getValueAt(i, 5));
                diemMons.add(DiemCalculator.diemMon(bc, cc, ck));
                tinChis.add(((Number) tableModel.getValueAt(i, 2)).intValue());
            } catch (Exception ignored) {
                // Ô đang nhập dở — bỏ qua khi preview
            }
        }
        updateTichLuy(diemMons, tinChis);
        refreshChart();
    }

    /** null -> "" để ô trống báo đúng lỗi "không được để trống" thay vì "null". */
    private static String cellText(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static double toDouble(Object o) {
        if (o instanceof Number) {
            return DiemCalculator.round2(((Number) o).doubleValue());
        }
        return DiemCalculator.round2(Double.parseDouble(String.valueOf(o).trim().replace(',', '.')));
    }

    private void onSave() {
        if (table.isEditing()) {
            if (!table.getCellEditor().stopCellEditing()) {
                showMessage(
                        "Ô đang chỉnh sửa có giá trị chưa hợp lệ, vui lòng kiểm tra lại!",
                        "Điểm chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        // Validate toàn bộ trước khi ghi (báo đúng dòng + tên cột)
        List<Diem> toSave = new ArrayList<>();
        List<Double> diemMons = new ArrayList<>();
        List<Integer> tinChis = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String maMH = String.valueOf(tableModel.getValueAt(i, 0));
            float bc, cc, ck;
            try {
                bc = DiemCalculator.parseDiem(cellText(tableModel.getValueAt(i, 3)), "Báo cáo (dòng " + (i + 1) + ")");
                cc = DiemCalculator.parseDiem(cellText(tableModel.getValueAt(i, 4)), "Chuyên cần (dòng " + (i + 1) + ")");
                ck = DiemCalculator.parseDiem(cellText(tableModel.getValueAt(i, 5)), "Cuối kỳ (dòng " + (i + 1) + ")");
            } catch (IllegalArgumentException ex) {
                showMessage(ex.getMessage(),
                        "Điểm chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
                table.changeSelection(i, 3, false, false);
                table.requestFocus();
                return;
            }
            toSave.add(new Diem(maSV, maMH, bc, cc, ck));
            diemMons.add(DiemCalculator.diemMon(bc, cc, ck));
            tinChis.add(((Number) tableModel.getValueAt(i, 2)).intValue());
        }
        if (toSave.isEmpty()) {
            showMessage("Bảng điểm trống, thêm ít nhất 1 môn!",
                    "Thiếu dữ liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        double tb = DiemCalculator.diemTBTinChi(diemMons, tinChis);

        if (isDemoMode) {
            lblStatus.setText("Đã lưu tạm (chế độ demo).");
            showMessage(
                    "Đã lưu tạm bảng điểm thành công (Chế độ demo CSDL offline)!\nTB tích lũy: " + String.format(java.util.Locale.US, "%.2f", tb),
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            if (onSaved != null) {
                onSaved.run();
            }
            dispose();
            return;
        }

        btnSave.setEnabled(false);
        btnAdd.setEnabled(false);
        btnDelete.setEnabled(false);
        lblStatus.setText("Đang lưu…");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // 1 transaction duy nhất: xóa môn đã gỡ + upsert + ghi đè DiemTB
                diemDAO.saveBangDiem(maSV, toSave, tb);
                return null;
            }

            @Override
            protected void done() {
                btnSave.setEnabled(true);
                btnAdd.setEnabled(true);
                btnDelete.setEnabled(true);
                try {
                    get();
                    lblStatus.setText("Đã lưu.");
                    showMessage(
                            "Đã lưu bảng điểm thành công!\nTB tích lũy: " + String.format(java.util.Locale.US, "%.2f", tb),
                            "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    if (onSaved != null) {
                        onSaved.run();
                    }
                    dispose();
                } catch (Exception ex) {
                    lblStatus.setText("Lỗi lưu: " + rootMsg(ex));
                    showMessage(
                            "Không lưu được bảng điểm:\n" + rootMsg(ex),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /**
     * Editor tùy biến cho các ô điểm: hỗ trợ dấu phẩy thập phân kiểu Việt Nam (8,5),
     * làm tròn 2 chữ số, và chặn giá trị ngoài khoảng [0.0, 10.0] ngay tại stopCellEditing().
     */
    public static class GradeCellEditor extends DefaultCellEditor {
        private final JTextField textField;

        public GradeCellEditor() {
            super(new JTextField());
            this.textField = (JTextField) getComponent();
            UITheme.styleTextField(textField);
            textField.setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            JTextField tf = (JTextField) super.getTableCellEditorComponent(table, value, isSelected, row, column);
            tf.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1));
            if (value instanceof Number) {
                tf.setText(String.format(java.util.Locale.US, "%.2f", ((Number) value).doubleValue()));
            } else if (value != null) {
                tf.setText(value.toString());
            } else {
                tf.setText("");
            }
            tf.selectAll();
            return tf;
        }

        @Override
        public boolean stopCellEditing() {
            String text = textField.getText();
            if (text == null || text.trim().isEmpty()) {
                textField.setBorder(BorderFactory.createLineBorder(UITheme.DANGER, 1));
                return false;
            }
            try {
                double val = Double.parseDouble(text.trim().replace(',', '.'));
                if (val < 0.0 || val > 10.0) {
                    textField.setBorder(BorderFactory.createLineBorder(UITheme.DANGER, 1));
                    return false;
                }
            } catch (Exception e) {
                textField.setBorder(BorderFactory.createLineBorder(UITheme.DANGER, 1));
                return false;
            }
            textField.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1));
            return super.stopCellEditing();
        }

        @Override
        public Object getCellEditorValue() {
            String text = textField.getText();
            if (text == null || text.trim().isEmpty()) {
                return 0.0;
            }
            try {
                return DiemCalculator.round2(Double.parseDouble(text.trim().replace(',', '.')));
            } catch (Exception e) {
                return 0.0;
            }
        }
    }

    private static String rootMsg(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        String m = cur.getMessage();
        return m != null && !m.isBlank() ? m : cur.toString();
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    // Getters và helpers cho unit test
    public JTable getTable() {
        return table;
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public JLabel getLblTichLuy() {
        return lblTichLuy;
    }

    public JLabel getLblStatus() {
        return lblStatus;
    }

    public JComboBox<MonHoc> getCbMonHoc() {
        return cbMonHoc;
    }

    public DefaultCategoryDataset getChartDataset() {
        return chartDataset;
    }

    public JFreeChart getChart() {
        return chart;
    }

    public JButton getBtnAdd() {
        return btnAdd;
    }

    public JButton getBtnDelete() {
        return btnDelete;
    }

    public JButton getBtnSave() {
        return btnSave;
    }

    public boolean isDemoMode() {
        return isDemoMode;
    }

    public void setDemoMode(boolean demoMode) {
        this.isDemoMode = demoMode;
    }

    public void renderTableDirect(List<DiemDetail> list) {
        renderTable(list);
    }

    public void setAllMonHoc(List<MonHoc> list) {
        this.allMonHoc = list;
        refreshCombo();
    }

    public void onAddMonHocAction() {
        onAddMonHoc();
    }

    public void onDeleteMonHocAction() {
        onDeleteMonHoc();
    }

    public void onSaveAction() {
        onSave();
    }
}
