package com.quanlysinhvien.dao;

import com.quanlysinhvien.model.Diem;
import com.quanlysinhvien.model.DiemCalculator;
import com.quanlysinhvien.model.DiemDetail;
import com.quanlysinhvien.model.MonHoc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Điểm thành phần theo môn (MaSV, MaMH) + seeder random 4-7 môn/SV.
 */
public class DiemDAO {

    public List<DiemDetail> getByMaSV(String maSV) throws SQLException {
        List<DiemDetail> list = new ArrayList<>();
        String sql = "SELECT d.MaSV, d.MaMH, m.TenMH, m.SoTC, "
                + "d.DiemBaoCao, d.DiemChuyenCan, d.DiemCuoiKy "
                + "FROM Diem d JOIN MonHoc m ON d.MaMH = m.MaMH "
                + "WHERE d.MaSV = ? ORDER BY d.MaMH ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new DiemDetail(
                            rs.getString("MaSV"), rs.getString("MaMH"),
                            rs.getString("TenMH"), rs.getInt("SoTC"),
                            rs.getFloat("DiemBaoCao"), rs.getFloat("DiemChuyenCan"),
                            rs.getFloat("DiemCuoiKy")));
                }
            }
        }
        return list;
    }

    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Diem";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public void upsert(Diem d) throws SQLException {
        upsertBatch(java.util.Collections.singletonList(d));
    }

    public void upsertBatch(List<Diem> list) throws SQLException {
        if (list == null || list.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO Diem (MaSV, MaMH, DiemBaoCao, DiemChuyenCan, DiemCuoiKy) "
                + "VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE DiemBaoCao = VALUES(DiemBaoCao), "
                + "DiemChuyenCan = VALUES(DiemChuyenCan), DiemCuoiKy = VALUES(DiemCuoiKy)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Diem d : list) {
                    DiemCalculator.validateDiem(d.getDiemBaoCao(), "Điểm báo cáo");
                    DiemCalculator.validateDiem(d.getDiemChuyenCan(), "Điểm chuyên cần");
                    DiemCalculator.validateDiem(d.getDiemCuoiKy(), "Điểm cuối kỳ");
                    ps.setString(1, d.getMaSV().trim());
                    ps.setString(2, d.getMaMH().trim());
                    ps.setFloat(3, d.getDiemBaoCao());
                    ps.setFloat(4, d.getDiemChuyenCan());
                    ps.setFloat(5, d.getDiemCuoiKy());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean delete(String maSV, String maMH) throws SQLException {
        String sql = "DELETE FROM Diem WHERE MaSV = ? AND MaMH = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV.trim());
            ps.setString(2, maMH.trim());
            return ps.executeUpdate() > 0;
        }
    }

    public void deleteByMaSV(String maSV) throws SQLException {
        String sql = "DELETE FROM Diem WHERE MaSV = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSV.trim());
            ps.executeUpdate();
        }
    }

    /**
     * Lưu toàn bộ bảng điểm 1 SV trong đúng 1 transaction duy nhất:
     * xóa môn đã gỡ + upsert batch + ghi đè SinhVien.DiemTB.
     * Dialog phải gọi hàm này thay vì gọi 3 DAO rời rạc (tránh dở dang khi 1 bước lỗi).
     */
    public void saveBangDiem(String maSV, List<Diem> toSave, double tbTichLuy) throws SQLException {
        if (toSave == null || toSave.isEmpty()) {
            throw new IllegalArgumentException("Bảng điểm trống, thêm ít nhất 1 môn!");
        }
        Set<String> keep = new java.util.HashSet<>();
        for (Diem d : toSave) {
            DiemCalculator.validateDiem(d.getDiemBaoCao(), "Điểm báo cáo");
            DiemCalculator.validateDiem(d.getDiemChuyenCan(), "Điểm chuyên cần");
            DiemCalculator.validateDiem(d.getDiemCuoiKy(), "Điểm cuối kỳ");
            if (!keep.add(d.getMaMH().trim())) {
                throw new IllegalArgumentException("Môn học trùng lặp trong bảng điểm: " + d.getMaMH());
            }
        }

        String selectSql = "SELECT MaMH FROM Diem WHERE MaSV = ?";
        String deleteSql = "DELETE FROM Diem WHERE MaSV = ? AND MaMH = ?";
        String upsertSql = "INSERT INTO Diem (MaSV, MaMH, DiemBaoCao, DiemChuyenCan, DiemCuoiKy) "
                + "VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE DiemBaoCao = VALUES(DiemBaoCao), "
                + "DiemChuyenCan = VALUES(DiemChuyenCan), DiemCuoiKy = VALUES(DiemCuoiKy)";
        String updateTbSql = "UPDATE SinhVien SET DiemTB = ? WHERE MaSV = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Đọc danh sách môn hiện có của SV vào bộ nhớ và đóng ngay ResultSet/PreparedStatement
                List<String> existingMH = new ArrayList<>();
                try (PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
                    psSelect.setString(1, maSV.trim());
                    try (ResultSet rs = psSelect.executeQuery()) {
                        while (rs.next()) {
                            existingMH.add(rs.getString(1));
                        }
                    }
                }

                // 2. Xóa các môn không còn trong danh sách lưu
                try (PreparedStatement psDel = conn.prepareStatement(deleteSql)) {
                    for (String oldMH : existingMH) {
                        if (!keep.contains(oldMH)) {
                            psDel.setString(1, maSV.trim());
                            psDel.setString(2, oldMH);
                            psDel.addBatch();
                        }
                    }
                    psDel.executeBatch();
                }

                // 3. Upsert batch các môn trong danh sách
                try (PreparedStatement psUp = conn.prepareStatement(upsertSql)) {
                    for (Diem d : toSave) {
                        psUp.setString(1, maSV.trim());
                        psUp.setString(2, d.getMaMH().trim());
                        psUp.setFloat(3, d.getDiemBaoCao());
                        psUp.setFloat(4, d.getDiemChuyenCan());
                        psUp.setFloat(5, d.getDiemCuoiKy());
                        psUp.addBatch();
                    }
                    psUp.executeBatch();
                }

                // 4. Cập nhật Điểm TB tích lũy của sinh viên
                try (PreparedStatement psTb = conn.prepareStatement(updateTbSql)) {
                    psTb.setFloat(1, (float) tbTichLuy);
                    psTb.setString(2, maSV.trim());
                    psTb.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** TB tích lũy chuẩn học vụ = Σ(DiemMon × TC) / Σ(TC), round2. Rỗng -> 0. */
    public double calcTichLuy(String maSV) throws SQLException {
        List<DiemDetail> list = getByMaSV(maSV);
        List<Double> mons = new ArrayList<>();
        List<Integer> tcs = new ArrayList<>();
        for (DiemDetail d : list) {
            mons.add(d.getDiemMon());
            tcs.add(d.getSoTC());
        }
        return DiemCalculator.diemTBTinChi(mons, tcs);
    }

    /**
     * Đồng bộ DiemTB cho mọi SV đã có dòng Diem (chạy mỗi lần khởi động, idempotent).
     * Dùng single-query UPDATE JOIN để triệt tiêu hoàn toàn N+1 connection churn.
     */
    public void syncTichLuyAll() throws SQLException {
        String batchSql = "UPDATE SinhVien s JOIN ("
                + "SELECT d.MaSV, ROUND(SUM(ROUND(d.DiemBaoCao*0.4 + d.DiemChuyenCan*0.1 + d.DiemCuoiKy*0.5, 2) * m.SoTC) / SUM(m.SoTC), 2) AS new_gpa "
                + "FROM Diem d JOIN MonHoc m ON d.MaMH = m.MaMH GROUP BY d.MaSV"
                + ") t ON s.MaSV = t.MaSV SET s.DiemTB = t.new_gpa";
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement st = conn.prepareStatement(batchSql)) {
                st.executeUpdate();
                return;
            } catch (SQLException e) {
                // Safe fallback nếu phương ngữ SQL không hỗ trợ UPDATE JOIN
            }
            // Fallback an toàn: truy vấn và cập nhật trên CÙNG 1 connection duy nhất
            String fallbackQuery = "SELECT d.MaSV, m.SoTC, d.DiemBaoCao, d.DiemChuyenCan, d.DiemCuoiKy "
                    + "FROM Diem d JOIN MonHoc m ON d.MaMH = m.MaMH ORDER BY d.MaSV";
            java.util.Map<String, List<Double>> gradesMap = new java.util.HashMap<>();
            java.util.Map<String, List<Integer>> tcsMap = new java.util.HashMap<>();
            try (PreparedStatement st = conn.prepareStatement(fallbackQuery);
                 ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    String sv = rs.getString("MaSV");
                    int tc = rs.getInt("SoTC");
                    float bc = rs.getFloat("DiemBaoCao");
                    float cc = rs.getFloat("DiemChuyenCan");
                    float ck = rs.getFloat("DiemCuoiKy");
                    double dm = DiemCalculator.diemMon(bc, cc, ck);
                    gradesMap.computeIfAbsent(sv, k -> new ArrayList<>()).add(dm);
                    tcsMap.computeIfAbsent(sv, k -> new ArrayList<>()).add(tc);
                }
            }
            String updateSql = "UPDATE SinhVien SET DiemTB = ? WHERE MaSV = ?";
            boolean origAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                for (String sv : gradesMap.keySet()) {
                    double gpa = DiemCalculator.diemTBTinChi(gradesMap.get(sv), tcsMap.get(sv));
                    ps.setFloat(1, (float) gpa);
                    ps.setString(2, sv);
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(origAutoCommit);
            }
        }
    }

    /**
     * Bậc học lực và khoảng GPA mục tiêu theo mô hình Stratified Stochastic Profile Model.
     */
    public enum AcademicTier {
        XUAT_SAC(9.0, 9.8),
        GIOI(8.0, 8.9),
        KHA(6.5, 7.9),
        TRUNG_BINH(5.0, 6.4),
        YEU(3.2, 4.9);

        private final double minGpa;
        private final double maxGpa;

        AcademicTier(double minGpa, double maxGpa) {
            this.minGpa = minGpa;
            this.maxGpa = maxGpa;
        }

        public double getMinGpa() {
            return minGpa;
        }

        public double getMaxGpa() {
            return maxGpa;
        }
    }

    /**
     * Kiểm tra dữ liệu điểm có bị tập trung hẹp (ví dụ >85% sinh viên có điểm Khá) hay không.
     * Sử dụng PreparedStatement an toàn và chỉ kích hoạt reseed tự động khi tỷ lệ điểm Khá vượt quá 85%.
     */
    public boolean hasNarrowVariance() throws SQLException {
        String sql = "SELECT DiemTB FROM SinhVien";
        int total = 0;
        int countKha = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                total++;
                float gpa = rs.getFloat(1);
                if (gpa >= 6.5f && gpa < 8.0f) {
                    countKha++;
                }
            }
        }
        if (total < 10) {
            return false;
        }
        double khaRatio = (double) countKha / total;
        return khaRatio > 0.85;
    }

    /**
     * Seed 1 lần duy nhất khi bảng Diem rỗng.
     * Sử dụng reseedDiverseGrades() để bảo đảm dữ liệu điểm phong phú ngay từ đầu.
     */
    public void seedIfEmpty() throws SQLException {
        if (countAll() == 0 || hasNarrowVariance()) {
            reseedDiverseGrades();
        } else {
            syncTichLuyAll();
        }
    }

    /**
     * Tạo lại dữ liệu điểm đa dạng trên toàn bộ sinh viên theo mô hình Stratified Stochastic Profile Model:
     * - Xuất sắc (~7%): GPA 9.0 - 9.8
     * - Giỏi (~23%): GPA 8.0 - 8.9
     * - Khá (~42%): GPA 6.5 - 7.9
     * - Trung bình (~21%): GPA 5.0 - 6.4
     * - Yếu (~7%): GPA 3.2 - 4.9
     * Mỗi sinh viên được gán ngẫu nhiên 4-7 môn từ 37 môn chuyên ngành (DCT.md).
     * Toàn bộ thao tác thực thi trong 1 transaction duy nhất, dùng PreparedStatement an toàn tuyệt đối.
     */
    public void reseedDiverseGrades() throws SQLException {
        new MonHocDAO().ensureSeeded();
        List<MonHoc> allMH = new MonHocDAO().getAll();
        if (allMH.isEmpty()) {
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Lấy danh sách toàn bộ MaSV
            List<String> allMaSV = new ArrayList<>();
            try (PreparedStatement st = conn.prepareStatement("SELECT MaSV FROM SinhVien ORDER BY MaSV ASC");
                 ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    allMaSV.add(rs.getString(1));
                }
            }
            if (allMaSV.isEmpty()) {
                return;
            }

            int n = allMaSV.size();
            int numXuatSac = (int) Math.round(n * 0.07);
            if (numXuatSac == 0 && n >= 5) numXuatSac = 1;
            int numYeu = (int) Math.round(n * 0.07);
            if (numYeu == 0 && n >= 5) numYeu = 1;
            int numGioi = (int) Math.round(n * 0.23);
            int numTB = (int) Math.round(n * 0.21);
            int numKha = n - (numXuatSac + numGioi + numTB + numYeu);
            if (numKha < 0) numKha = 0;

            List<AcademicTier> tiers = new ArrayList<>(n);
            for (int i = 0; i < numXuatSac; i++) tiers.add(AcademicTier.XUAT_SAC);
            for (int i = 0; i < numGioi; i++) tiers.add(AcademicTier.GIOI);
            for (int i = 0; i < numKha; i++) tiers.add(AcademicTier.KHA);
            for (int i = 0; i < numTB; i++) tiers.add(AcademicTier.TRUNG_BINH);
            for (int i = 0; i < numYeu; i++) tiers.add(AcademicTier.YEU);
            while (tiers.size() < n) tiers.add(AcademicTier.KHA);

            Collections.shuffle(tiers, ThreadLocalRandom.current());

            conn.setAutoCommit(false);
            try {
                // Xóa toàn bộ điểm cũ trong bảng Diem
                try (PreparedStatement psDel = conn.prepareStatement("DELETE FROM Diem")) {
                    psDel.executeUpdate();
                }

                String insertSql = "INSERT INTO Diem (MaSV, MaMH, DiemBaoCao, DiemChuyenCan, DiemCuoiKy) "
                        + "VALUES (?, ?, ?, ?, ?)";
                String updateTbSql = "UPDATE SinhVien SET DiemTB = ? WHERE MaSV = ?";

                try (PreparedStatement psIns = conn.prepareStatement(insertSql);
                     PreparedStatement psUpd = conn.prepareStatement(updateTbSql)) {

                    ThreadLocalRandom rnd = ThreadLocalRandom.current();
                    for (int sIdx = 0; sIdx < n; sIdx++) {
                        String maSV = allMaSV.get(sIdx);
                        AcademicTier tier = tiers.get(sIdx);

                        List<MonHoc> shuffledMH = new ArrayList<>(allMH);
                        Collections.shuffle(shuffledMH, new java.util.Random(rnd.nextLong()));
                        int nSubj = 4 + rnd.nextInt(4); // 4-7 môn
                        nSubj = Math.min(nSubj, shuffledMH.size());
                        List<MonHoc> chosenMH = shuffledMH.subList(0, nSubj);

                        GeneratedGrades gg = generateGradesForTier(tier, chosenMH, rnd);

                        for (int i = 0; i < nSubj; i++) {
                            MonHoc mh = chosenMH.get(i);
                            psIns.setString(1, maSV);
                            psIns.setString(2, mh.getMaMH());
                            psIns.setFloat(3, gg.bcs.get(i));
                            psIns.setFloat(4, gg.ccs.get(i));
                            psIns.setFloat(5, gg.cks.get(i));
                            psIns.addBatch();
                        }

                        psUpd.setFloat(1, (float) gg.gpa);
                        psUpd.setString(2, maSV);
                        psUpd.addBatch();
                    }

                    psIns.executeBatch();
                    psUpd.executeBatch();
                }

                conn.commit();
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private static class GeneratedGrades {
        List<Float> bcs = new ArrayList<>();
        List<Float> ccs = new ArrayList<>();
        List<Float> cks = new ArrayList<>();
        double gpa;
    }

    private GeneratedGrades generateGradesForTier(AcademicTier tier, List<MonHoc> subjects, ThreadLocalRandom rnd) {
        int n = subjects.size();
        List<Integer> tcs = new ArrayList<>(n);
        for (MonHoc mh : subjects) {
            tcs.add(mh.getSoTC());
        }

        double targetGpa = tier.getMinGpa() + rnd.nextDouble() * (tier.getMaxGpa() - tier.getMinGpa());

        for (int attempt = 0; attempt < 100; attempt++) {
            GeneratedGrades gg = new GeneratedGrades();
            List<Double> mons = new ArrayList<>(n);

            for (int i = 0; i < n; i++) {
                double subjBase = targetGpa + (rnd.nextDouble() - 0.5) * 0.8;
                subjBase = Math.max(tier.getMinGpa() - 0.3, Math.min(tier.getMaxGpa() + 0.3, subjBase));

                float cc, bc, ck;
                switch (tier) {
                    case XUAT_SAC:
                        cc = round1(clamp(subjBase + rnd.nextDouble() * 0.5, 9.0, 10.0));
                        bc = round1(clamp(subjBase + (rnd.nextDouble() - 0.4) * 0.6, 9.0, 10.0));
                        ck = round1(clamp(subjBase + (rnd.nextDouble() - 0.4) * 0.6, 9.0, 10.0));
                        break;
                    case GIOI:
                        cc = round1(clamp(subjBase + rnd.nextDouble() * 0.6, 8.0, 10.0));
                        bc = round1(clamp(subjBase + (rnd.nextDouble() - 0.5) * 0.8, 7.5, 9.5));
                        ck = round1(clamp(subjBase + (rnd.nextDouble() - 0.5) * 0.8, 7.8, 9.2));
                        break;
                    case KHA:
                        cc = round1(clamp(subjBase + rnd.nextDouble() * 0.6, 7.0, 9.5));
                        bc = round1(clamp(subjBase + (rnd.nextDouble() - 0.5) * 1.0, 6.0, 8.5));
                        ck = round1(clamp(subjBase + (rnd.nextDouble() - 0.5) * 1.0, 6.0, 8.2));
                        break;
                    case TRUNG_BINH:
                        cc = round1(clamp(subjBase + rnd.nextDouble() * 0.8, 5.5, 8.5));
                        bc = round1(clamp(subjBase + (rnd.nextDouble() - 0.5) * 1.0, 4.5, 7.0));
                        ck = round1(clamp(subjBase + (rnd.nextDouble() - 0.5) * 1.0, 4.5, 6.6));
                        break;
                    case YEU:
                    default:
                        cc = round1(clamp(subjBase + (rnd.nextDouble() - 0.5) * 1.0, 3.0, 7.0));
                        bc = round1(clamp(subjBase + (rnd.nextDouble() - 0.5) * 1.0, 2.5, 5.5));
                        ck = round1(clamp(subjBase + (rnd.nextDouble() - 0.5) * 1.0, 2.5, 5.0));
                        break;
                }

                gg.ccs.add(cc);
                gg.bcs.add(bc);
                gg.cks.add(ck);
                mons.add(DiemCalculator.diemMon(bc, cc, ck));
            }

            double calculatedGpa = DiemCalculator.diemTBTinChi(mons, tcs);
            if (calculatedGpa >= tier.getMinGpa() && calculatedGpa <= tier.getMaxGpa()) {
                gg.gpa = calculatedGpa;
                return gg;
            }
        }

        // Fallback định chuẩn bảo đảm 100% điểm TB rơi vào đúng khung xếp loại của tier
        GeneratedGrades gg = new GeneratedGrades();
        List<Double> mons = new ArrayList<>(n);
        float calibrated = round1(Math.max(tier.getMinGpa(), Math.min(tier.getMaxGpa(), targetGpa)));
        for (int i = 0; i < n; i++) {
            float bc = round1(clamp(calibrated - 0.2f, 0.0, 10.0));
            float cc = round1(clamp(calibrated + 0.3f, 0.0, 10.0));
            // Cuối kỳ điều chỉnh để điểm môn cân bằng
            float ck = round1(clamp((calibrated - bc * 0.4f - cc * 0.1f) / 0.5f, 0.0, 10.0));
            gg.ccs.add(cc);
            gg.bcs.add(bc);
            gg.cks.add(ck);
            mons.add(DiemCalculator.diemMon(bc, cc, ck));
        }
        gg.gpa = DiemCalculator.diemTBTinChi(mons, tcs);
        return gg;
    }

    private static double clamp(double val, double min, double max) {
        if (val < min) return min;
        if (val > max) return max;
        return val;
    }

    private static float round1(double x) {
        float v = (float) (Math.round(x * 10.0) / 10.0);
        if (v < 0f) return 0f;
        if (v > 10f) return 10f;
        return v;
    }

}
