package com.quanlysinhvien.ui.components;

import com.quanlysinhvien.model.SinhVien;
import com.quanlysinhvien.ui.theme.ModernCardPanel;
import com.quanlysinhvien.ui.theme.UITheme;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hàng 4 thẻ chỉ số nhanh (KPI Cards) phong cách Stripe / Modern SaaS:
 * 1. Tổng Sinh Viên
 * 2. Số Lớp Học
 * 3. Điểm Cao Nhất (Thủ khoa)
 * 4. Tỷ Lệ Khá - Giỏi (Điểm TB >= 6.5)
 *
 * Tính toán in-memory từ masterList, chạy trên EDT, cache string để chống repaint thừa.
 */
public class KpiCardsPanel extends JPanel {

    public static class KpiValues {
        public final int totalStudents;
        public final int totalClasses;
        public final float maxScore;
        public final double goodRate;

        public KpiValues(int totalStudents, int totalClasses, float maxScore, double goodRate) {
            this.totalStudents = totalStudents;
            this.totalClasses = totalClasses;
            this.maxScore = maxScore;
            this.goodRate = goodRate;
        }

        @Override
        public String toString() {
            return "KpiValues{" +
                    "totalStudents=" + totalStudents +
                    ", totalClasses=" + totalClasses +
                    ", maxScore=" + maxScore +
                    ", goodRate=" + goodRate + "%" +
                    '}';
        }
    }

    private final JLabel lblTotalVal = new JLabel("0");
    private final JLabel lblClassesVal = new JLabel("0");
    private final JLabel lblMaxScoreVal = new JLabel("0.0");
    private final JLabel lblGoodRateVal = new JLabel("0.0%");

    private String lastTotalStr = "";
    private String lastClassesStr = "";
    private String lastMaxScoreStr = "";
    private String lastGoodRateStr = "";

    private int totalStudents = 0;
    private int totalClasses = 0;
    private float maxScore = 0.0f;
    private double goodRate = 0.0;

    public KpiCardsPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new GridLayout(1, 4, 12, 0));
        setOpaque(false);
        setPreferredSize(new Dimension(0, 84));
        setMinimumSize(new Dimension(0, 84));

        add(createCard("TỔNG SINH VIÊN", lblTotalVal, "Hồ sơ hệ thống", new Color(0x63, 0x5B, 0xFF)));
        add(createCard("SỐ LỚP HỌC", lblClassesVal, "Đang đào tạo", new Color(0x0E, 0xA5, 0xE9)));
        add(createCard("ĐIỂM CAO NHẤT", lblMaxScoreVal, "Thủ khoa trường", new Color(0xF5, 0x9E, 0x0B)));
        add(createCard("TỶ LỆ KHÁ - GIỎI", lblGoodRateVal, "Điểm TB ≥ 6.5", new Color(0x10, 0xB9, 0x81)));
    }

    private JPanel createCard(String title, JLabel valLabel, String subtitle, Color accentColor) {
        ModernCardPanel card = new ModernCardPanel(new BorderLayout(4, 4), 12);
        card.setCardBackground(Color.WHITE);
        card.setBorderColor(UITheme.BORDER_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblTitle.setForeground(new Color(0x64, 0x74, 0x8B));

        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valLabel.setForeground(UITheme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblSub.setForeground(UITheme.TEXT_MUTED);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valLabel, BorderLayout.CENTER);
        card.add(lblSub, BorderLayout.SOUTH);

        return card;
    }

    /**
     * Cập nhật các chỉ số KPI in-memory từ masterList.
     * Chạy trên EDT và cache giá trị chuỗi để chỉ repaint khi dữ liệu thực sự đổi.
     */
    public void update(List<SinhVien> master) {
        if (master == null || master.isEmpty()) {
            totalStudents = 0;
            totalClasses = 0;
            maxScore = 0.0f;
            goodRate = 0.0;
        } else {
            totalStudents = master.size();

            Set<String> classes = master.stream()
                    .map(SinhVien::getLop)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.toUpperCase(java.util.Locale.ROOT))
                    .collect(Collectors.toSet());
            totalClasses = classes.size();

            float rawMax = 0.0f;
            int goodCount = 0;
            for (SinhVien sv : master) {
                float score = sv.getDiemTB();
                if (score > rawMax) {
                    rawMax = score;
                }
                if (score >= 6.5f) {
                    goodCount++;
                }
            }
            maxScore = (float) (Math.round(rawMax * 100.0) / 100.0);
            goodRate = Math.round((goodCount * 100.0 / totalStudents) * 100.0) / 100.0;
        }

        String tStr = String.valueOf(totalStudents);
        String cStr = String.valueOf(totalClasses);
        String mStr = String.valueOf(maxScore);
        String gStr = goodRate + "%";

        Runnable updater = () -> {
            boolean changed = false;
            if (!tStr.equals(lastTotalStr)) {
                lblTotalVal.setText(tStr);
                lastTotalStr = tStr;
                changed = true;
            }
            if (!cStr.equals(lastClassesStr)) {
                lblClassesVal.setText(cStr);
                lastClassesStr = cStr;
                changed = true;
            }
            if (!mStr.equals(lastMaxScoreStr)) {
                lblMaxScoreVal.setText(mStr);
                lastMaxScoreStr = mStr;
                changed = true;
            }
            if (!gStr.equals(lastGoodRateStr)) {
                lblGoodRateVal.setText(gStr);
                lastGoodRateStr = gStr;
                changed = true;
            }
            if (changed) {
                repaint();
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            updater.run();
        } else {
            SwingUtilities.invokeLater(updater);
        }
    }

    public KpiValues getValues() {
        return new KpiValues(totalStudents, totalClasses, maxScore, goodRate);
    }

    public Map<String, Object> getValuesMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("totalStudents", totalStudents);
        map.put("totalClasses", totalClasses);
        map.put("maxScore", maxScore);
        map.put("goodRate", goodRate);
        return map;
    }

    public int getTotalStudents() {
        return totalStudents;
    }

    public int getTotalClasses() {
        return totalClasses;
    }

    public float getMaxScore() {
        return maxScore;
    }

    public double getGoodRate() {
        return goodRate;
    }
}
