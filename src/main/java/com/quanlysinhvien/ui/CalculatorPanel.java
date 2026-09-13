package com.quanlysinhvien.ui;

import com.quanlysinhvien.model.Calculator;
import com.quanlysinhvien.ui.theme.ModernButton;
import com.quanlysinhvien.ui.theme.ModernCardPanel;
import com.quanlysinhvien.ui.theme.UITheme;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * Bài 2: Panel Máy tính cơ bản với phong cách hiện đại (Stripe Clean Canvas).
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
        setBackground(UITheme.CANVAS_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("MÁY TÍNH CƠ BẢN", SwingConstants.CENTER);
        lblTitle.setFont(UITheme.FONT_TITLE_LARGE);
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitle = new JLabel("Hỗ trợ tính toán số học chính xác & xử lý ngoại lệ", SwingConstants.CENTER);
        lblSubtitle.setFont(UITheme.FONT_REGULAR);
        lblSubtitle.setForeground(UITheme.TEXT_SECONDARY);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(lblTitle);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        headerPanel.add(lblSubtitle);
        add(headerPanel, BorderLayout.NORTH);

        // Center: Card container
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        ModernCardPanel card = new ModernCardPanel(new BorderLayout(15, 20), 30);
        card.setPreferredSize(new Dimension(620, 390));

        // Display card for Result at Top of Card
        ModernCardPanel displayBox = new ModernCardPanel(new FlowLayout(FlowLayout.CENTER, 10, 15), 10);
        displayBox.setCardBackground(UITheme.TABLE_ROW_ODD);
        displayBox.setBorderColor(UITheme.BORDER_COLOR);

        lblKetQua = new JLabel("Chưa có kết quả");
        lblKetQua.setFont(UITheme.FONT_TITLE_LARGE);
        lblKetQua.setForeground(UITheme.PRIMARY);
        displayBox.add(lblKetQua);

        card.add(displayBox, BorderLayout.NORTH);

        // Middle: Inputs (a & b)
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 12, 10, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Số a
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.25;
        JLabel lblA = new JLabel("Nhập số a:");
        lblA.setFont(UITheme.FONT_BOLD);
        lblA.setForeground(UITheme.TEXT_PRIMARY);
        inputPanel.add(lblA, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.75;
        txtA = new JTextField(20);
        UITheme.styleTextField(txtA);
        inputPanel.add(txtA, gbc);

        // Số b
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.25;
        JLabel lblB = new JLabel("Nhập số b:");
        lblB.setFont(UITheme.FONT_BOLD);
        lblB.setForeground(UITheme.TEXT_PRIMARY);
        inputPanel.add(lblB, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.75;
        txtB = new JTextField(20);
        UITheme.styleTextField(txtB);
        inputPanel.add(txtB, gbc);

        card.add(inputPanel, BorderLayout.CENTER);

        // Bottom: Operation Buttons
        JPanel buttonGroup = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        buttonGroup.setOpaque(false);

        ModernButton btnAdd = new ModernButton("+ (Cộng)", UITheme.PRIMARY);
        btnAdd.setPreferredSize(new Dimension(105, 38));

        ModernButton btnSub = new ModernButton("- (Trừ)", new Color(0x25, 0x63, 0xEB));
        btnSub.setPreferredSize(new Dimension(105, 38));

        ModernButton btnMul = new ModernButton("* (Nhân)", new Color(0xD9, 0x77, 0x06));
        btnMul.setPreferredSize(new Dimension(105, 38));

        ModernButton btnDiv = new ModernButton("/ (Chia)", new Color(0x7C, 0x3A, 0xED));
        btnDiv.setPreferredSize(new Dimension(105, 38));

        ModernButton btnClear = new ModernButton("Xóa lại", UITheme.NEUTRAL_BTN_BG, UITheme.NEUTRAL_BTN_HOVER, UITheme.NEUTRAL_BTN_TEXT);
        btnClear.setBorderColor(UITheme.NEUTRAL_BTN_BORDER);
        btnClear.setPreferredSize(new Dimension(105, 38));

        buttonGroup.add(btnAdd);
        buttonGroup.add(btnSub);
        buttonGroup.add(btnMul);
        buttonGroup.add(btnDiv);
        buttonGroup.add(btnClear);

        card.add(buttonGroup, BorderLayout.SOUTH);

        centerWrapper.add(card);
        add(centerWrapper, BorderLayout.CENTER);

        // Event Listeners
        btnAdd.addActionListener(e -> calculate('+'));
        btnSub.addActionListener(e -> calculate('-'));
        btnMul.addActionListener(e -> calculate('*'));
        btnDiv.addActionListener(e -> calculate('/'));
        btnClear.addActionListener(e -> {
            txtA.setText("");
            txtB.setText("");
            lblKetQua.setText("Chưa có kết quả");
            lblKetQua.setForeground(UITheme.PRIMARY);
            txtA.requestFocus();
        });
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
