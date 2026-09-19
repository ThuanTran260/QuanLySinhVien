package com.quanlysinhvien.model;

import java.util.List;

/**
 * Công thức điểm chung cho mọi môn (đã chốt):
 * DiemMon = BaoCao*0.4 + ChuyenCan*0.1 + CuoiKy*0.5 (thang 0-10, làm tròn 2).
 * DiemTB(SV) = AVG(DiemMon), làm tròn 2. Pure static, dễ unit-test.
 */
public final class DiemCalculator {
    public static final double W_BAO_CAO = 0.4;
    public static final double W_CHUYEN_CAN = 0.1;
    public static final double W_CUOI_KY = 0.5;

    private DiemCalculator() {
    }

    public static double round2(double x) {
        if (Double.isNaN(x) || Double.isInfinite(x)) {
            throw new IllegalArgumentException("Điểm không phải số thực hợp lệ!");
        }
        return Math.round(x * 100.0) / 100.0;
    }

    public static void validateDiem(double x, String tenTruong) {
        if (Double.isNaN(x) || Double.isInfinite(x) || x < 0.0 || x > 10.0) {
            throw new IllegalArgumentException(tenTruong + " phải trong khoảng 0.0 - 10.0!");
        }
    }

    /** Parse ô nhập trong JTable: chấp nhận dấu phẩy, trim, báo lỗi rõ tên trường. */
    public static float parseDiem(String raw, String tenTruong) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException(tenTruong + " không được để trống!");
        }
        float v;
        try {
            v = Float.parseFloat(raw.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(tenTruong + " phải là số thực (0-10)!");
        }
        validateDiem(v, tenTruong);
        return v;
    }

    public static double diemMon(double baoCao, double chuyenCan, double cuoiKy) {
        validateDiem(baoCao, "Điểm báo cáo");
        validateDiem(chuyenCan, "Điểm chuyên cần");
        validateDiem(cuoiKy, "Điểm cuối kỳ");
        return round2(baoCao * W_BAO_CAO + chuyenCan * W_CHUYEN_CAN + cuoiKy * W_CUOI_KY);
    }

    public static double diemTB(List<Double> diemMons) {
        if (diemMons == null || diemMons.isEmpty()) {
            return 0.0;
        }
        double sum = 0;
        for (double d : diemMons) {
            sum += d;
        }
        return round2(sum / diemMons.size());
    }

    /**
     * TB tích lũy chuẩn học vụ: Σ(Điểm môn × Tín chỉ) / Σ(Tín chỉ), làm tròn 2.
     * Đây là công thức chính thức cho DiemTB(SV). Rỗng -> 0.
     */
    public static double diemTBTinChi(List<Double> diemMons, List<Integer> tinChis) {
        if (diemMons == null || diemMons.isEmpty()) {
            return 0.0;
        }
        if (tinChis == null || tinChis.size() != diemMons.size()) {
            throw new IllegalArgumentException("Số tín chỉ phải khớp số môn!");
        }
        double weighted = 0;
        int totalTC = 0;
        for (int i = 0; i < diemMons.size(); i++) {
            int tc = tinChis.get(i);
            if (tc <= 0) {
                throw new IllegalArgumentException("Tín chỉ phải > 0!");
            }
            weighted += diemMons.get(i) * tc;
            totalTC += tc;
        }
        return round2(weighted / totalTC);
    }
}
