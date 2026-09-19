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
    private JLabel lblStatus;

    public BangDiemDialog(Frame owner, String maSV, String hoTen, String lop) {
        super(owner, "Bảng điểm theo môn - " + maSV, true);
        this.maSV = maSV.trim();
        initUI(hoTen, lop);
        loadData();
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
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        // Style theo token hệ thống (KHÔNG dùng ModernTableRenderer: cột Điểm môn là số,
        // renderer đó sẽ nhầm thành badge "Yếu" vì nó key theo modelColumn == 6 là text xếp loại)
        table.setSelectionBackground(UITheme.TABLE_SELECTION_BG);
        table.setSelectionForeground(UITheme.TABLE_SELECTION_TEXT);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(241, 245, 249));
        table.getTableHeader().setFont(UITheme.FONT_BOLD);
        table.getTableHeader().setBackground(UITheme.TABLE_HEADER_BG);
        table.getTableHeader().setForeground(UITheme.TABLE_HEADER_TEXT);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(2).setPreferredWidth(40);
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
        JButton btnAdd = new ModernButton("Thêm", UITheme.SUCCESS);
        btnAdd.setPreferredSize(new Dimension(90, 32));
        btnAdd.addActionListener(e -> onAddMonHoc());
        addPanel.add(btnAdd, "");
        JButton btnDelete = new ModernButton("Xóa môn", UITheme.DANGER);
        btnDelete.setPreferredSize(new Dimension(90, 32));
        btnDelete.addActionListener(e -> onDeleteMonHoc());
        addPanel.add(btnDelete, "wrap");
        root.add(addPanel, "growx");

        // 4. Biểu đồ điểm môn (JFreeChart, cao cố định, không live-update từng phím)
        chartDataset = new DefaultCategoryDataset();
        JFreeChart chart = ChartFactory.createBarChart(
                "Điểm môn", "Môn học", "Điểm", chartDataset,
                PlotOrientation.VERTICAL, false, false, false);
        chart.setBackgroundPaint(UITheme.CARD_BG);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(UITheme.CANVAS_BG);
        plot.getRenderer().setSeriesPaint(0, UITheme.PRIMARY);
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
        JButton btnSave = new ModernButton("Lưu", UITheme.PRIMARY);
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

            @Override
            protected List<DiemDetail> doInBackground() throws Exception {
                mhs = monHocDAO.getAll();
                return diemDAO.getByMaSV(maSV);
            }

            @Override
            protected void done() {
                try {
                    allMonHoc = mhs != null ? mhs : new ArrayList<>();
                    renderTable(get());
                    lblStatus.setText(" ");
                } catch (Exception ex) {
                    lblStatus.setText("Không tải được (chế độ demo, không lưu).");
                    renderTable(new ArrayList<>());
                }
                refreshCombo();
            }
        }.execute();
    }

    private void renderTable(List<DiemDetail> list) {
        tableModel.setRowCount(0);
        List<Double> diemMons = new ArrayList<>();
        List<Integer> tinChis = new ArrayList<>();
        for (DiemDetail d : list) {
            double dm = d.getDiemMon();
            diemMons.add(dm);
            tinChis.add(d.getSoTC());
            tableModel.addRow(new Object[]{
                    d.getMaMH(), d.getTenMH(), d.getSoTC(),
                    (double) d.getDiemBaoCao(), (double) d.getDiemChuyenCan(),
                    (double) d.getDiemCuoiKy(), dm
            });
        }
        updateTichLuy(diemMons, tinChis);
        refreshChart();
    }

    private void updateTichLuy(List<Double> diemMons, List<Integer> tinChis) {
        if (diemMons.isEmpty()) {
            lblTichLuy.setText("TB tích lũy: — (0 môn)");
        } else {
            lblTichLuy.setText(String.format("TB tích lũy: %.2f (%d môn)",
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
            JOptionPane.showMessageDialog(this, "Sinh viên đã học hết môn trong danh sách!",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        tableModel.addRow(new Object[]{mh.getMaMH(), mh.getTenMH(), mh.getSoTC(), 7.0, 8.0, 7.0,
                DiemCalculator.diemMon(7.0, 8.0, 7.0)});
        refreshCombo();
        refreshTichLuyFromTable();
    }

    private void onDeleteMonHoc() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn 1 môn trên bảng để xóa!",
                    "Chưa chọn", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (tableModel.getRowCount() <= 1) {
            JOptionPane.showMessageDialog(this, "Mỗi sinh viên giữ tối thiểu 1 môn!",
                    "Không thể xóa", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        tableModel.removeRow(modelRow);
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

    private static double toDouble(Object o) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        return Double.parseDouble(String.valueOf(o).trim().replace(',', '.'));
    }

    private void onSave() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        // Validate toàn bộ trước khi ghi (báo đúng dòng + tên cột)
        List<Diem> toSave = new ArrayList<>();
        List<Double> diemMons = new ArrayList<>();
        List<Integer> tinChis = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String maMH = String.valueOf(tableModel.getValueAt(i, 0));
            float bc, cc, ck;
            try {
                bc = DiemCalculator.parseDiem(String.valueOf(tableModel.getValueAt(i, 3)), "Báo cáo (dòng " + (i + 1) + ")");
                cc = DiemCalculator.parseDiem(String.valueOf(tableModel.getValueAt(i, 4)), "Chuyên cần (dòng " + (i + 1) + ")");
                ck = DiemCalculator.parseDiem(String.valueOf(tableModel.getValueAt(i, 5)), "Cuối kỳ (dòng " + (i + 1) + ")");
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
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
            JOptionPane.showMessageDialog(this, "Bảng điểm trống, thêm ít nhất 1 môn!",
                    "Thiếu dữ liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        double tb = DiemCalculator.diemTBTinChi(diemMons, tinChis);
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
                try {
                    get();
                    lblStatus.setText("Đã lưu.");
                    if (onSaved != null) {
                        onSaved.run();
                    }
                    dispose();
                } catch (Exception ex) {
                    lblStatus.setText("Lỗi lưu: " + rootMsg(ex));
                    JOptionPane.showMessageDialog(BangDiemDialog.this,
                            "Không lưu được bảng điểm:\n" + rootMsg(ex),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
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

    // Getters cho test
    public JTable getTable() {
        return table;
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public JLabel getLblTichLuy() {
        return lblTichLuy;
    }
}
