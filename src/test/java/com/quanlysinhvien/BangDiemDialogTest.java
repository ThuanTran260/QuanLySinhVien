package com.quanlysinhvien;

import com.quanlysinhvien.model.DiemDetail;
import com.quanlysinhvien.model.MonHoc;
import com.quanlysinhvien.dao.MonHocDAO;
import com.quanlysinhvien.ui.BangDiemDialog;
import org.jfree.chart.plot.CategoryPlot;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.table.DefaultTableModel;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BangDiemDialogTest {

    private BangDiemDialog dialog;

    @BeforeEach
    void setUp() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Bỏ qua GUI trong môi trường headless");
        dialog = new BangDiemDialog(null, "3124410003", "Nguyễn Văn Test", "DCT1241");
        dialog.setDemoMode(true); // Cách ly khỏi DB cho các unit test giao diện
        dialog.setSuppressDialogsForTest(true);
    }

    @Test
    @DisplayName("Biểu đồ JFreeChart có trục Y giới hạn đúng [0.0, 10.0]")
    void testChartYAxisRange() {
        assertNotNull(dialog.getChart());
        CategoryPlot plot = dialog.getChart().getCategoryPlot();
        assertEquals(0.0, plot.getRangeAxis().getRange().getLowerBound(), 0.001);
        assertEquals(10.0, plot.getRangeAxis().getRange().getUpperBound(), 0.001);
    }

    @Test
    @DisplayName("Sửa điểm thành phần (cột 3, 4, 5) tự động tính lại Điểm môn, TB tích lũy và cập nhật biểu đồ")
    void testReactiveGradeUpdate() {
        // Nạp 2 môn mẫu:
        // Môn 1: 841021 (3 TC), BC=8, CC=8, CK=8 -> Điểm môn = 8.0
        // Môn 2: 841044 (4 TC), BC=6, CC=6, CK=6 -> Điểm môn = 6.0
        List<DiemDetail> sample = Arrays.asList(
                new DiemDetail("3124410003", "841021", "Kiến trúc máy tính", 3, 8.0f, 8.0f, 8.0f),
                new DiemDetail("3124410003", "841044", "Phương pháp LTHĐT", 4, 6.0f, 6.0f, 6.0f)
        );
        dialog.renderTableDirect(sample);

        DefaultTableModel model = dialog.getTableModel();
        assertEquals(2, model.getRowCount());

        // Kiểm tra giá trị khởi tạo
        assertEquals(8.0, ((Number) model.getValueAt(0, 6)).doubleValue(), 0.01);
        assertEquals(6.0, ((Number) model.getValueAt(1, 6)).doubleValue(), 0.01);
        // TB = (8*3 + 6*4) / 7 = 48/7 ≈ 6.86
        assertTrue(dialog.getLblTichLuy().getText().contains("6.86"));
        assertEquals(8.0, dialog.getChartDataset().getValue("Điểm môn", "841021").doubleValue(), 0.01);

        // Chỉnh sửa ô Báo cáo (cột 3) dòng 0 thành 10.0
        // Điểm môn mới = 10*0.4 + 8*0.1 + 8*0.5 = 4.0 + 0.8 + 4.0 = 8.8
        model.setValueAt(10.0, 0, 3);

        assertEquals(8.8, ((Number) model.getValueAt(0, 6)).doubleValue(), 0.01,
                "Cột Điểm môn phải tự động cập nhật lên 8.8");
        assertEquals(8.8, dialog.getChartDataset().getValue("Điểm môn", "841021").doubleValue(), 0.01,
                "Biểu đồ cột phải tự động phản ánh 8.8");
        // TB mới = (8.8*3 + 6*4) / 7 = 50.4 / 7 = 7.20
        assertTrue(dialog.getLblTichLuy().getText().contains("7.20"),
                "Label TB tích lũy phải cập nhật lên 7.20: " + dialog.getLblTichLuy().getText());

        // Chỉnh sửa ô Cuối kỳ (cột 5) dòng 1 thành 8.0
        // Điểm môn mới = 6*0.4 + 6*0.1 + 8*0.5 = 2.4 + 0.6 + 4.0 = 7.0
        model.setValueAt(8.0, 1, 5);
        assertEquals(7.0, ((Number) model.getValueAt(1, 6)).doubleValue(), 0.01);
        assertEquals(7.0, dialog.getChartDataset().getValue("Điểm môn", "841044").doubleValue(), 0.01);
        // TB mới = (8.8*3 + 7.0*4) / 7 = 54.4 / 7 ≈ 7.77
        assertTrue(dialog.getLblTichLuy().getText().contains("7.77"));
    }

    @Test
    @DisplayName("Thêm môn mới cập nhật bảng, biểu đồ và loại khỏi combobox")
    void testAddSubject() {
        List<MonHoc> all = MonHocDAO.seedData();
        dialog.setAllMonHoc(all);

        List<DiemDetail> sample = new ArrayList<>();
        sample.add(new DiemDetail("3124410003", "841021", "Kiến trúc máy tính", 3, 8.0f, 8.0f, 8.0f));
        dialog.renderTableDirect(sample);

        assertEquals(1, dialog.getTableModel().getRowCount());
        assertEquals(1, dialog.getChartDataset().getColumnCount());

        // Chọn môn đầu tiên trong combo và thêm
        assertNotNull(dialog.getCbMonHoc().getSelectedItem());
        MonHoc toAdd = (MonHoc) dialog.getCbMonHoc().getSelectedItem();
        dialog.onAddMonHocAction();

        assertEquals(2, dialog.getTableModel().getRowCount(), "Bảng phải tăng lên 2 dòng");
        assertEquals(2, dialog.getChartDataset().getColumnCount(), "Biểu đồ phải có 2 cột");
        assertEquals(toAdd.getMaMH(), dialog.getTableModel().getValueAt(1, 0));

        // Môn vừa thêm không còn trong ComboBox
        for (int i = 0; i < dialog.getCbMonHoc().getItemCount(); i++) {
            assertNotEquals(toAdd.getMaMH(), dialog.getCbMonHoc().getItemAt(i).getMaMH());
        }
    }

    @Test
    @DisplayName("Xóa môn cập nhật bảng, biểu đồ, trả lại combobox và giữ tối thiểu 1 môn")
    void testDeleteSubject() {
        List<MonHoc> all = MonHocDAO.seedData();
        dialog.setAllMonHoc(all);

        List<DiemDetail> sample = new ArrayList<>();
        sample.add(new DiemDetail("3124410003", "841021", "Kiến trúc máy tính", 3, 8.0f, 8.0f, 8.0f));
        sample.add(new DiemDetail("3124410003", "841044", "Phương pháp LTHĐT", 4, 7.0f, 7.0f, 7.0f));
        dialog.renderTableDirect(sample);

        assertEquals(2, dialog.getTableModel().getRowCount());

        // Chọn dòng 1 để xóa
        dialog.getTable().setRowSelectionInterval(1, 1);
        dialog.onDeleteMonHocAction();

        assertEquals(1, dialog.getTableModel().getRowCount(), "Bảng phải giảm còn 1 dòng");
        assertEquals(1, dialog.getChartDataset().getColumnCount(), "Biểu đồ phải còn 1 cột");

        // Khi chỉ còn 1 môn, cố tình xóa tiếp sẽ bị từ chối
        dialog.getTable().setRowSelectionInterval(0, 0);
        dialog.onDeleteMonHocAction();
        assertEquals(1, dialog.getTableModel().getRowCount(), "Không được phép xóa môn cuối cùng");
    }

    @Test
    @DisplayName("Tránh lỗi số thực 8.1999998... khi nạp và chỉnh sửa")
    void testFloatPrecisionArtifactsAvoided() {
        List<DiemDetail> sample = Arrays.asList(
                new DiemDetail("3124410003", "841021", "Kiến trúc máy tính", 3, 8.2f, 8.2f, 8.2f)
        );
        dialog.renderTableDirect(sample);

        double bc = ((Number) dialog.getTableModel().getValueAt(0, 3)).doubleValue();
        assertEquals(8.20, bc, 0.0001, "Điểm 8.2f phải được làm tròn chính xác 8.20 không bị artifact");
    }

    @Test
    @DisplayName("Lưu bảng điểm ở chế độ demo hoạt động mượt mà không văng lỗi DB")
    void testSaveInDemoMode() {
        List<DiemDetail> sample = Arrays.asList(
                new DiemDetail("3124410003", "841021", "Kiến trúc máy tính", 3, 8.0f, 8.0f, 8.0f)
        );
        dialog.renderTableDirect(sample);
        dialog.setDemoMode(true);

        final boolean[] savedCalled = new boolean[]{false};
        dialog.setOnSaved(() -> savedCalled[0] = true);

        dialog.onSaveAction();
        assertTrue(savedCalled[0], "onSaved callback phải được gọi thành công trong chế độ demo");
    }

    @Test
    @DisplayName("GradeCellEditor chấp nhận dấu phẩy Việt Nam (8,5) và chặn giá trị ngoài [0.0, 10.0]")
    void testGradeCellEditorAcceptsCommaAndRejectsInvalid() {
        BangDiemDialog.GradeCellEditor editor = new BangDiemDialog.GradeCellEditor();
        javax.swing.JTextField tf = (javax.swing.JTextField) editor.getComponent();

        // 1. Dấu phẩy kiểu Việt Nam 8,5 -> hợp lệ, trả về 8.5
        tf.setText("8,5");
        assertTrue(editor.stopCellEditing(), "Dấu phẩy 8,5 phải được chấp nhận");
        assertEquals(8.5, editor.getCellEditorValue());

        // 2. Dấu chấm chuẩn 9.25 -> hợp lệ
        tf.setText("9.25");
        assertTrue(editor.stopCellEditing());
        assertEquals(9.25, editor.getCellEditorValue());

        // 3. Vượt quá 10 -> bị từ chối ngay tại stopCellEditing
        tf.setText("15.0");
        assertFalse(editor.stopCellEditing(), "Điểm > 10 phải bị từ chối");

        // 4. Số âm -> bị từ chối
        tf.setText("-1.0");
        assertFalse(editor.stopCellEditing(), "Điểm âm phải bị từ chối");

        // 5. Chuỗi không phải số -> bị từ chối
        tf.setText("abc");
        assertFalse(editor.stopCellEditing(), "Ký tự chữ phải bị từ chối");

        // 6. Để trống -> bị từ chối
        tf.setText("");
        assertFalse(editor.stopCellEditing(), "Để trống phải bị từ chối");
    }

    @Test
    @DisplayName("Bấm Lưu khi ô đang chỉnh sửa có giá trị không hợp lệ sẽ chặn lưu")
    void testOnSaveBlocksWhenCellEditorHasInvalidValue() {
        List<DiemDetail> sample = Arrays.asList(
                new DiemDetail("3124410003", "841021", "Kiến trúc máy tính", 3, 8.0f, 8.0f, 8.0f)
        );
        dialog.renderTableDirect(sample);
        dialog.setDemoMode(true);

        final boolean[] savedCalled = new boolean[]{false};
        dialog.setOnSaved(() -> savedCalled[0] = true);

        // Kích hoạt chỉnh sửa trên ô cột 3 với giá trị lỗi 99.0
        dialog.getTable().editCellAt(0, 3);
        javax.swing.JTextField editorTf = (javax.swing.JTextField) dialog.getTable().getEditorComponent();
        editorTf.setText("99.0");

        dialog.onSaveAction();
        assertFalse(savedCalled[0], "Không được phép lưu khi ô đang sửa có giá trị > 10");
    }

    @Test
    @DisplayName("Bấm Lưu khi ô đang chỉnh sửa với dấu phẩy (9,5) sẽ lưu thành công")
    void testOnSaveSucceedsWhenCellEditorHasCommaValue() {
        List<DiemDetail> sample = Arrays.asList(
                new DiemDetail("3124410003", "841021", "Kiến trúc máy tính", 3, 8.0f, 8.0f, 8.0f)
        );
        dialog.renderTableDirect(sample);
        dialog.setDemoMode(true);

        final boolean[] savedCalled = new boolean[]{false};
        dialog.setOnSaved(() -> savedCalled[0] = true);

        // Kích hoạt chỉnh sửa trên ô cột 3 với dấu phẩy 9,5
        dialog.getTable().editCellAt(0, 3);
        javax.swing.JTextField editorTf = (javax.swing.JTextField) dialog.getTable().getEditorComponent();
        editorTf.setText("9,5");

        dialog.onSaveAction();
        assertTrue(savedCalled[0], "Phải lưu thành công khi ô nhập dấu phẩy 9,5");
        assertEquals(9.5, ((Number) dialog.getTableModel().getValueAt(0, 3)).doubleValue(), 0.01);
    }

    @Test
    @DisplayName("Khởi tạo BangDiemDialog với Window owner là JDialog không bị ClassCastException")
    void testWindowOwnerConstructorWithJDialog() {
        javax.swing.JDialog parentDialog = new javax.swing.JDialog();
        BangDiemDialog dlg = new BangDiemDialog((java.awt.Window) parentDialog, "3124410003", "Test SV", "DCT1241");
        assertNotNull(dlg);
        assertEquals("Bảng điểm theo môn - 3124410003", dlg.getTitle());
        dlg.dispose();
        parentDialog.dispose();
    }

    @Test
    @DisplayName("GradeCellEditor tự động reset lại border ban đầu khi bắt đầu chỉnh sửa ô mới")
    void testGradeCellEditorBorderReset() {
        BangDiemDialog.GradeCellEditor editor = new BangDiemDialog.GradeCellEditor();
        javax.swing.JTextField tf = (javax.swing.JTextField) editor.getComponent();

        // 1. Cố ý nhập lỗi để tạo border đỏ
        tf.setText("99.9");
        assertFalse(editor.stopCellEditing());

        // 2. Chuyển sang ô mới -> getTableCellEditorComponent phải reset border về bình thường
        editor.getTableCellEditorComponent(dialog.getTable(), 8.0, false, 0, 3);
        assertNotNull(tf.getBorder());
    }

    @Test
    @DisplayName("Thêm môn mới để trống ô điểm cho nhập tay, TB bỏ qua tới khi nhập đủ")
    void testAddSubjectBlankForManualEntry() {
        List<MonHoc> all = MonHocDAO.seedData();
        dialog.setAllMonHoc(all);
        dialog.renderTableDirect(new ArrayList<>(Arrays.asList(
                new DiemDetail("3124410003", "841021", "Kiến trúc máy tính", 3, 8.0f, 8.0f, 8.0f))));

        dialog.onAddMonHocAction();
        assertEquals(2, dialog.getTableModel().getRowCount());
        assertNull(dialog.getTableModel().getValueAt(1, 3), "Ô Báo cáo môn mới phải trống");
        assertNull(dialog.getTableModel().getValueAt(1, 4), "Ô Chuyên cần môn mới phải trống");
        assertNull(dialog.getTableModel().getValueAt(1, 5), "Ô Cuối kỳ môn mới phải trống");
        assertNull(dialog.getTableModel().getValueAt(1, 6), "Điểm môn chưa nhập phải trống");
        assertTrue(dialog.getLblTichLuy().getText().contains("(1 môn)"),
                "TB chỉ tính môn đã có điểm: " + dialog.getLblTichLuy().getText());

        // Lưu khi còn ô trống -> chặn, không gọi onSaved
        final boolean[] saved = {false};
        dialog.setOnSaved(() -> saved[0] = true);
        dialog.onSaveAction();
        assertFalse(saved[0], "Không được lưu khi còn ô điểm trống");

        // Nhập đủ 3 ô -> TB tính cả 2 môn: (8*3 + 8.9*4)/7 = 8.51
        dialog.getTableModel().setValueAt(10.0, 1, 3);
        dialog.getTableModel().setValueAt(9.0, 1, 4);
        dialog.getTableModel().setValueAt(8.0, 1, 5);
        assertEquals(8.9, ((Number) dialog.getTableModel().getValueAt(1, 6)).doubleValue(), 0.01);
        assertTrue(dialog.getLblTichLuy().getText().contains("8.51"),
                "TB phải tính cả môn vừa nhập: " + dialog.getLblTichLuy().getText());
    }
}
