package com.quanlysinhvien.ui;

import com.quanlysinhvien.dao.SinhVienDAO;
import com.quanlysinhvien.model.SinhVien;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

/**
 * Bài 3 & Phần 1 - Phần 8: Quản lý sinh viên toàn diện.
 * Bao gồm: Thêm, Sửa, Xóa, Tìm kiếm, Sắp xếp, Thống kê, Đọc/Ghi Text File.
 */
public class StudentManagementPanel extends JPanel {
    private final SinhVienDAO dao = new SinhVienDAO();

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

    private final DecimalFormat scoreFormat = new DecimalFormat("#0.0#",
            java.text.DecimalFormatSymbols.getInstance(java.util.Locale.US));

    public StudentManagementPanel() {
        initUI();
        loadDataToTable();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

        // 1. Header
        JLabel lblTitle = new JLabel("CHƯƠNG TRÌNH QUẢN LÝ SINH VIÊN", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(24, 90, 157));
        add(lblTitle, BorderLayout.NORTH);

        // 2. Center Panel (Form nhập liệu + Bảng dữ liệu)
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));

        // --- Panel Form Nhập Liệu ---
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thông tin chi tiết sinh viên",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 13)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: MaSV & Lop
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.1;
        inputPanel.add(new JLabel("Mã Sinh Viên (*):"), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.4;
        txtMaSV = new JTextField();
        inputPanel.add(txtMaSV, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.1;
        inputPanel.add(new JLabel("Lớp (*):"), gbc);

        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 0.4;
        txtLop = new JTextField();
        inputPanel.add(txtLop, gbc);

        // Row 1: HoTen & NgaySinh
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.1;
        inputPanel.add(new JLabel("Họ và Tên (*):"), gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.4;
        txtHoTen = new JTextField();
        inputPanel.add(txtHoTen, gbc);

        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0.1;
        inputPanel.add(new JLabel("Ngày Sinh (yyyy-MM-dd):"), gbc);

        gbc.gridx = 3; gbc.gridy = 1; gbc.weightx = 0.4;
        txtNgaySinh = new JTextField();
        inputPanel.add(txtNgaySinh, gbc);

        // Row 2: DiemTB & Hướng dẫn
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.1;
        inputPanel.add(new JLabel("Điểm Trung Bình (0-10):"), gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.4;
        txtDiemTB = new JTextField();
        inputPanel.add(txtDiemTB, gbc);

        gbc.gridx = 2; gbc.gridy = 2; gbc.gridwidth = 2;
        JLabel lblNote = new JLabel("(*) Bắt buộc | Điểm: 0.0 - 10.0 | Ngày sinh: ví dụ 2003-05-15");
        lblNote.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblNote.setForeground(Color.GRAY);
        inputPanel.add(lblNote, gbc);
        gbc.gridwidth = 1;

        // --- Panel CRUD Buttons ---
        JPanel crudBtnBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        btnAdd = new JButton("Thêm mới");
        btnAdd.setIcon(UIManager.getIcon("FileView.fileIcon"));
        btnUpdate = new JButton("Cập nhật (Sửa)");
        btnDelete = new JButton("Xóa sinh viên");
        btnClear = new JButton("Làm mới form");

        crudBtnBox.add(btnAdd);
        crudBtnBox.add(btnUpdate);
        crudBtnBox.add(btnDelete);
        crudBtnBox.add(btnClear);

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(inputPanel, BorderLayout.CENTER);
        topContainer.add(crudBtnBox, BorderLayout.SOUTH);

        centerPanel.add(topContainer, BorderLayout.NORTH);

        // --- Panel Tìm kiếm, Sắp xếp và Thống kê ---
        JPanel toolBarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolBarPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Công cụ tìm kiếm, sắp xếp & tính năng mở rộng",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12)));

        // Tìm kiếm
        toolBarPanel.add(new JLabel("Tìm theo:"));
        cbSearchCriteria = new JComboBox<>(new String[]{"Tất cả", "Mã SV", "Họ tên", "Lớp"});
        toolBarPanel.add(cbSearchCriteria);

        txtSearch = new JTextField(12);
        toolBarPanel.add(txtSearch);

        btnSearch = new JButton("Tìm kiếm");
        toolBarPanel.add(btnSearch);

        btnRefresh = new JButton("Tất cả");
        toolBarPanel.add(btnRefresh);

        toolBarPanel.add(new JSeparator(SwingConstants.VERTICAL));

        // Sắp xếp
        toolBarPanel.add(new JLabel("Sắp xếp:"));
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
        toolBarPanel.add(cbSort);

        btnSort = new JButton("Sắp xếp");
        toolBarPanel.add(btnSort);

        toolBarPanel.add(new JSeparator(SwingConstants.VERTICAL));

        // Thống kê & File IO
        btnStatistic = new JButton("Thống kê");
        btnStatistic.setFont(new Font("Segoe UI", Font.BOLD, 12));
        toolBarPanel.add(btnStatistic);

        btnExportFile = new JButton("Xuất Text File");
        toolBarPanel.add(btnExportFile);

        btnImportFile = new JButton("Nạp từ File");
        toolBarPanel.add(btnImportFile);

        // --- Bảng hiển thị JTable ---
        String[] columnNames = {"STT", "Mã SV", "Họ và Tên", "Lớp", "Ngày Sinh", "Điểm TB", "Xếp Loại"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Không cho sửa trực tiếp trên cell table
            }
        };

        tblSinhVien = new JTable(tableModel);
        tblSinhVien.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblSinhVien.setRowHeight(24);
        tblSinhVien.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblSinhVien.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tblSinhVien.setAutoCreateRowSorter(true); // Cho phép bấm tiêu đề cột để sort

        // Căn lề giữa cho STT, Mã SV, Lớp, Ngày Sinh, Điểm TB, Xếp Loại
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tblSinhVien.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblSinhVien.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tblSinhVien.getColumnModel().getColumn(1).setPreferredWidth(90);
        tblSinhVien.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tblSinhVien.getColumnModel().getColumn(2).setPreferredWidth(180);
        tblSinhVien.getColumnModel().getColumn(3).setPreferredWidth(90);
        tblSinhVien.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        tblSinhVien.getColumnModel().getColumn(4).setPreferredWidth(100);
        tblSinhVien.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        tblSinhVien.getColumnModel().getColumn(5).setPreferredWidth(80);
        tblSinhVien.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        tblSinhVien.getColumnModel().getColumn(6).setPreferredWidth(100);
        tblSinhVien.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(tblSinhVien);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Danh sách sinh viên",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 13)));

        JPanel tableContainer = new JPanel(new BorderLayout(5, 5));
        tableContainer.add(toolBarPanel, BorderLayout.NORTH);
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        centerPanel.add(tableContainer, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // --- Đăng ký các sự kiện ---
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        // 1. Khi chọn dòng trên JTable -> Đổ dữ liệu lên form, KHÓA ô MaSV
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
                        txtDiemTB.setText(tableModel.getValueAt(modelRow, 5).toString());

                        // Khóa không cho sửa Mã SV (khóa chính)
                        txtMaSV.setEditable(false);
                        txtMaSV.setBackground(new Color(240, 240, 240));
                    }
                }
            }
        });

        // 2. Nút Thêm mới
        btnAdd.addActionListener(e -> onAddStudent());

        // 3. Nút Cập nhật (Sửa)
        btnUpdate.addActionListener(e -> onUpdateStudent());

        // 4. Nút Xóa sinh viên
        btnDelete.addActionListener(e -> onDeleteStudent());

        // 5. Nút Làm mới form
        btnClear.addActionListener(e -> clearFormAndReload());

        // 6. Nút Tìm kiếm
        btnSearch.addActionListener(e -> onSearch());
        txtSearch.addActionListener(e -> onSearch());

        // 7. Nút Hiển thị tất cả
        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            loadDataToTable();
        });

        // 8. Nút Sắp xếp
        btnSort.addActionListener(e -> onSort());

        // 9. Nút Thống kê
        btnStatistic.addActionListener(e -> showStatisticDialog());

        // 10. Nút Xuất File
        btnExportFile.addActionListener(e -> onExportFile());

        // 11. Nút Nạp File
        btnImportFile.addActionListener(e -> onImportFile());
    }

    /**
     * Nạp dữ liệu vào bảng
     */
    public void loadDataToTable() {
        try {
            List<SinhVien> list = dao.getAll();
            renderTableData(list);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi tải danh sách sinh viên từ CSDL: " + e.getMessage(),
                    "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renderTableData(List<SinhVien> list) {
        tblSinhVien.setRowSorter(null);
        tableModel.setRowCount(0);
        int stt = 1;
        for (SinhVien sv : list) {
            tableModel.addRow(new Object[]{
                    stt++,
                    sv.getMaSV(),
                    sv.getHoTen(),
                    sv.getLop(),
                    sv.getNgaySinhStr(),
                    scoreFormat.format(sv.getDiemTB()),
                    sv.getXepLoai()
            });
        }
        tblSinhVien.setAutoCreateRowSorter(true);
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
                clearFormAndReload();
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
                clearFormAndReload();
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
                    clearFormAndReload();
                } else {
                    JOptionPane.showMessageDialog(this, "Không tìm thấy sinh viên cần xóa!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi xóa sinh viên: " + ex.getMessage(), "Lỗi SQL", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onSearch() {
        String keyword = txtSearch.getText().trim();
        String criteria = (String) cbSearchCriteria.getSelectedItem();

        try {
            List<SinhVien> list = dao.search(keyword, criteria);
            renderTableData(list);
            if (list.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Không tìm thấy sinh viên nào phù hợp với từ khóa: '" + keyword + "'",
                        "Kết quả tìm kiếm", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi tìm kiếm: " + ex.getMessage(), "Lỗi SQL", JOptionPane.ERROR_MESSAGE);
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
            renderTableData(list);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi sắp xếp: " + ex.getMessage(), "Lỗi SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showStatisticDialog() {
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
            JOptionPane.showMessageDialog(this, "Lỗi lấy dữ liệu thống kê: " + ex.getMessage(),
                    "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
        }
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
                List<SinhVien> list = dao.getAll();
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

                if (choice == 0) { // Đồng bộ vào MySQL
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
                    clearFormAndReload();
                } else if (choice == 1) { // Chỉ hiển thị trên bảng
                    renderTableData(importedList);
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

    // Các hàm getter phục vụ kiểm thử tự động
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
}
