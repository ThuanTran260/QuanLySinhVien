package com.quanlysinhvien.ui;

import com.quanlysinhvien.model.Calculator;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * Bài 2: Panel Máy tính cơ bản.
 * Hỗ trợ các phép toán +, -, *, /; bắt lỗi nhập liệu và cảnh báo chia cho 0.
 */
public class CalculatorPanel extends JPanel {
    private JTextField txtA;
    private JTextField txtB;
    private JLabel lblKetQua;
    private final DecimalFormat df = new DecimalFormat("#,##0.######");

    public CalculatorPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // Tiêu đề đầu trang
        JLabel lblHeader = new JLabel("MÁY TÍNH CƠ BẢN", SwingConstants.CENTER);
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblHeader.setForeground(new Color(24, 90, 157));
        add(lblHeader, BorderLayout.NORTH);

        // Khung trung tâm chứa các ô nhập và kết quả
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // Group nhập liệu
        JPanel inputGroup = new JPanel(new GridBagLayout());
        inputGroup.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Nhập dữ liệu tính toán",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Số a
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.2;
        JLabel lblA = new JLabel("Nhập số a:");
        lblA.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputGroup.add(lblA, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.8;
        txtA = new JTextField(20);
        txtA.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputGroup.add(txtA, gbc);

        // Số b
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.2;
        JLabel lblB = new JLabel("Nhập số b:");
        lblB.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputGroup.add(lblB, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.8;
        txtB = new JTextField(20);
        txtB.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputGroup.add(txtB, gbc);

        centerPanel.add(inputGroup);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Các nút phép tính
        JPanel buttonGroup = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton btnAdd = createButton("+ (Cộng)", new Color(76, 175, 80));
        JButton btnSub = createButton("- (Trừ)", new Color(33, 150, 243));
        JButton btnMul = createButton("* (Nhân)", new Color(255, 152, 0));
        JButton btnDiv = createButton("/ (Chia)", new Color(156, 39, 176));
        JButton btnClear = createButton("Xóa lại", new Color(158, 158, 158));

        buttonGroup.add(btnAdd);
        buttonGroup.add(btnSub);
        buttonGroup.add(btnMul);
        buttonGroup.add(btnDiv);
        buttonGroup.add(btnClear);

        centerPanel.add(buttonGroup);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Khung hiển thị kết quả
        JPanel resultPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        resultPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Kết quả phép tính",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14)));
        resultPanel.setPreferredSize(new Dimension(500, 80));

        lblKetQua = new JLabel("Chưa có kết quả");
        lblKetQua.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblKetQua.setForeground(new Color(24, 90, 157));
        resultPanel.add(lblKetQua);

        centerPanel.add(resultPanel);
        add(centerPanel, BorderLayout.CENTER);

        // Sự kiện các nút
        btnAdd.addActionListener(e -> calculate('+'));
        btnSub.addActionListener(e -> calculate('-'));
        btnMul.addActionListener(e -> calculate('*'));
        btnDiv.addActionListener(e -> calculate('/'));
        btnClear.addActionListener(e -> {
            txtA.setText("");
            txtB.setText("");
            lblKetQua.setText("Chưa có kết quả");
            lblKetQua.setForeground(new Color(24, 90, 157));
            txtA.requestFocus();
        });
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(110, 38));
        return btn;
    }

    public void calculate(char operator) {
        String strA = txtA.getText().trim();
        String strB = txtB.getText().trim();

        if (strA.isEmpty() || strB.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập đầy đủ cả hai số a và b!",
                    "Cảnh báo thiếu dữ liệu",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        double a, b;
        try {
            a = Double.parseDouble(strA.replace(',', '.'));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Số a không hợp lệ! Vui lòng nhập số thực.",
                    "Lỗi định dạng",
                    JOptionPane.ERROR_MESSAGE);
            txtA.requestFocus();
            return;
        }

        try {
            b = Double.parseDouble(strB.replace(',', '.'));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Số b không hợp lệ! Vui lòng nhập số thực.",
                    "Lỗi định dạng",
                    JOptionPane.ERROR_MESSAGE);
            txtB.requestFocus();
            return;
        }

        try {
            double result = Calculator.compute(a, b, operator);
            String opName = df.format(a) + " " + operator + " " + df.format(b) + " = " + df.format(result);
            lblKetQua.setForeground(new Color(33, 150, 243));
            lblKetQua.setText("Kết quả: " + opName);
        } catch (ArithmeticException e) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi: Không được phép chia cho 0!",
                    "Cảnh báo chia cho 0",
                    JOptionPane.WARNING_MESSAGE);
            lblKetQua.setText("Lỗi: Không thể chia cho 0!");
            lblKetQua.setForeground(Color.RED);
            txtB.requestFocus();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this,
                    e.getMessage(),
                    "Lỗi nhập liệu",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Các hàm hỗ trợ kiểm thử tự động
    public void setInputs(String a, String b) {
        txtA.setText(a);
        txtB.setText(b);
    }

    public void triggerCalculate(char operator) {
        calculate(operator);
    }

    public String getResultText() {
        return lblKetQua.getText();
    }
}
