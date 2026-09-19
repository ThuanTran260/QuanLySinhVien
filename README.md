# HỆ THỐNG QUẢN LÝ SINH VIÊN - JAVA SWING & MYSQL

Dự án ứng dụng Desktop hoàn chỉnh xây dựng bằng **Java Swing** kết nối cơ sở dữ liệu **MySQL 8.0** qua JDBC thuần và hỗ trợ đọc/ghi **Text File**.

---

## 1. Cấu trúc kiến trúc tối giản (Simple 3-Tier)
Tuân thủ nguyên tắc chống over-engineering theo [0001-simplified-swing-architecture.md](docs/adr/0001-simplified-swing-architecture.md):
```
src/main/java/com/quanlysinhvien/
├── Main.java                          # Điểm khởi chạy ứng dụng (FlatLaf FlatMacLightLaf)
├── model/
│   ├── SinhVien.java                  # Entity Domain Model + Validation dữ liệu
│   ├── MonHoc.java                    # Master môn học (seed từ TKB DCT.md)
│   ├── Diem.java / DiemDetail.java    # Điểm thành phần theo môn (BC/CC/CK)
│   └── DiemCalculator.java            # Công thức chung: Môn = BC*0.4 + CC*0.1 + CK*0.5
├── dao/
│   ├── DatabaseConnection.java        # Quản lý kết nối JDBC, đọc db.properties, auto-init CSDL
│   ├── SinhVienDAO.java               # Thực thi CRUD, Tìm kiếm, Sắp xếp, Thống kê, Đọc/Ghi File
│   ├── MonHocDAO.java                 # Seed + truy vấn 20 môn chuyên ngành
│   └── DiemDAO.java                   # Upsert batch, seed random 4-7 môn/SV, TB tích lũy
└── ui/
    ├── LoginForm.java                 # Bài 1: Form đăng nhập
    ├── CalculatorPanel.java           # Bài 2: Máy tính cơ bản (+, -, *, /, bắt lỗi chia cho 0)
    ├── StudentManagementPanel.java    # Bài 3 & P1-P8: Giao diện Quản lý Sinh viên toàn diện
    ├── BangDiemDialog.java            # Bảng điểm theo môn (MigLayout + JTable + JFreeChart)
    └── MainFrame.java                 # Cửa sổ chính chứa JTabbedPane và Menu hệ thống
```

---

## 2. Thông tin tài khoản & Cấu hình CSDL

### 2.1. Đăng nhập ứng dụng (Bài 1)
- **Tên đăng nhập:** `admin`
- **Mật khẩu:** `123`

### 2.2. Kết nối MySQL Database
- **Host:** `localhost:3306`
- **Tên Database:** `QuanLySinhVien`
- **Tài khoản root:** `root`
- **Mật khẩu:** *(Để trống hoặc điền mật khẩu MySQL của bạn)*
- Cấu hình được lưu tại: `db.properties` và `src/main/resources/db.properties`.
- Script khởi tạo: `database.sql` (Chương trình có cơ chế tự động khởi tạo database và nạp toàn bộ 84 sinh viên từ file `sinhvien.txt` nếu chưa có).

---

## 3. Danh sách chức năng chi tiết

### Bài 1: Form Đăng Nhập (`LoginForm.java`)
- Kiểm tra tài khoản `admin` / `123`.
- Bắt lỗi khi để trống trường thông tin.
- Hỗ trợ phím `Enter` để đăng nhập nhanh.
- Đăng nhập thành công sẽ đóng form đăng nhập và mở màn hình chính `MainFrame`.

### Bài 2: Máy Tính Cơ Bản (`CalculatorPanel.java`)
- Nhập hai số thực `a` và `b`.
- 4 nút chức năng: `+ (Cộng)`, `- (Trừ)`, `* (Nhân)`, `/ (Chia)`.
- Nút `Xóa lại` để nhập phép tính mới.
- Bắt lỗi dữ liệu nhập không phải số.
- Bắt lỗi và cảnh báo nghiêm ngặt khi thực hiện **phép chia cho 0**.

### Bài 3 & Phần 1 - Phần 8: Quản Lý Sinh Viên (`StudentManagementPanel.java`)
1. **Hiển thị danh sách JTable:**
   - Cột: STT, Mã SV, Họ và Tên, Lớp, Ngày Sinh, Điểm TB, Xếp Loại.
   - Căn chỉnh định dạng số và ngày rõ ràng, hỗ trợ sắp xếp khi click tiêu đề cột.
2. **Thêm sinh viên mới:**
   - Kiểm tra ràng buộc hợp lệ: không để trống, Mã SV tối đa 10 ký tự, Họ tên tối đa 50 ký tự, Lớp tối đa 20 ký tự.
   - Kiểm tra ngày sinh đúng định dạng `yyyy-MM-dd` và không lớn hơn ngày hiện tại.
   - Kiểm tra điểm trung bình trong đoạn `[0.0, 10.0]`.
   - Kiểm tra trùng khóa chính `MaSV` và cảnh báo người dùng.
3. **Cập nhật (Sửa) thông tin sinh viên:**
   - Chọn dòng trên JTable -> dữ liệu tự động đổ lên form nhập liệu.
   - Ô `Mã SV` bị khóa (read-only) để đảm bảo toàn vẹn khóa chính.
   - Cập nhật thông tin vào MySQL và reload lại bảng.
4. **Xóa sinh viên:**
   - Chọn dòng trên bảng -> Bấm `Xóa sinh viên`.
   - Hộp thoại xác nhận `JOptionPane.showConfirmDialog` bảo đảm không xóa nhầm.
5. **Tìm kiếm + Sắp xếp (Bộ lọc thống nhất):**
   - 1 nút **Bộ lọc** thay 2 dropdown cũ, popup 2 nhóm: *Tìm theo* (`Tất cả`, `Mã SV`, `Họ tên`, `Lớp`)
     và *Sắp xếp* (Mặc định + Tên A-Z/Z-A, Họ tên A-Z/Z-A, Điểm TB tăng/giảm, Mã SV tăng/giảm).
   - Nút hiện dấu `•` khi lọc/sort khác mặc định; nút **Tất cả** đặt lại toàn bộ.
   - Tìm qua DAO (giữ so khớp không dấu của MySQL, VD gõ `Nguyen` vẫn ra `Nguyễn`),
     sắp xếp cộng dồn trên kết quả đã lọc. Enter trong ô tìm = Áp dụng.
7. **Thống kê:**
   - Hộp thoại thống kê chi tiết:
     + Tổng số lượng sinh viên hiện có.
     + Điểm trung bình phân theo từng lớp.
     + Danh sách sinh viên đạt điểm cao nhất (Thủ khoa).
8. **Lưu và Đọc Text File (Bài 8):**
   - **Xuất Text File:** Dùng `JFileChooser`, lưu ra file UTF-8 chuẩn định dạng: `MaSV|HoTen|Lop|NgaySinh|DiemTB`.
   - **Nạp từ File:** Đọc dữ liệu từ file text, validate từng dòng, hỗ trợ tùy chọn:
     + *Đồng bộ vào MySQL*: Thêm mới hoặc cập nhật tự động vào CSDL.
     + *Chỉ xem trước*: Đổ dữ liệu file trực tiếp lên JTable.

---

## 4. Hướng dẫn chạy và kiểm thử

### 4.1. Chạy chương trình giao diện
```bash
mvn clean compile exec:java
```

### Nhánh `feature/bang-diem-theo-mon` — Bảng điểm theo môn (chi tiết)

**Mô hình dữ liệu (mới, không sửa bảng cũ):**
- `MonHoc(MaMH, TenMH, SoTC)`: 20 môn chuyên ngành seed từ TKB HK1 2026-2027
  (mã thật: 841021, 841044, 841047, 841072, 841111, ..., 841438, 841467, 841468).
- `Diem(MaSV, MaMH, DiemBaoCao, DiemChuyenCan, DiemCuoiKy)`: khóa chính kép,
  FK `CASCADE` khi xóa SV, `RESTRICT` khi xóa môn đang có điểm. **Điểm môn không lưu cứng**,
  luôn tính realtime để khỏi lệch khi sửa 1 đầu điểm.
- App tự tạo bảng + seed ở lần chạy đầu (`DatabaseConnection.initializeDatabase`,
  script tươi trong `database.sql`): mỗi SV random **4–7 môn phân tầng học lực**
  (giỏi/khá/TB rải đều GPA, tránh TB dồn một cục), rồi ghi đè `SinhVien.DiemTB`.

**Công thức điểm (đã chốt, chung mọi môn):**
- `Điểm môn = Báo cáo×0.4 + Chuyên cần×0.1 + Cuối kỳ×0.5` (thang 0–10, làm tròn 2).
- `DiemTB = Σ(Điểm môn × Tín chỉ) / Σ(Tín chỉ)` chuẩn học vụ; mỗi lần khởi động
  tự đồng bộ lại TB cho SV đã có điểm (`syncTichLuyAll`, idempotent).

**Dialog Bảng điểm (`BangDiemDialog`, mở từ nút Bảng điểm / menu chuột phải trên bảng):**
- Bảng 7 cột: Mã MH, Môn học, TC, 3 ô điểm sửa trực tiếp, Điểm môn tự tính.
  Sửa ô nào là Điểm môn, TB tích lũy và biểu đồ cập nhật ngay (reactive).
- Nút **Thêm**: môn mới **để trống 3 ô cho nhập tay** (không prefill điểm mẫu);
  môn chưa nhập đủ thì TB/chart bỏ qua. Nút **Xóa môn** giữ tối thiểu 1 môn/SV.
- Nút **Lưu**: validate toàn bộ, báo đúng dòng khi thiếu/sai (VD
  `Báo cáo (dòng 5) không được để trống!`), ghi **1 transaction duy nhất**
  (xóa môn đã gỡ + upsert + ghi TB — lỗi là rollback hết). Ô đang gõ dở mà sai
  (trống, chữ, ngoài 0–10) thì chặn ngay tại chỗ, viền đỏ.
- Biểu đồ cột JFreeChart điểm từng môn (trục Y cố định 0–10, nền theo theme FlatLaf).
- Mất MySQL thì tự vào **chế độ demo**: 4 môn mẫu xem được, lưu tạm không ghi DB.

**Form chính:** ô Ngày sinh dùng DatePicker lịch (đồng bộ 2 chiều với ô text,
  validation `yyyy-MM-dd` và test cũ giữ nguyên).

**Thư viện bổ sung (xem `pom.xml`):** `MigLayout` (layout dialog),
  `JFreeChart` (biểu đồ), `LGoodDatePicker` (chọn ngày).

### 4.2. Chạy toàn bộ Test tự động (83 Tests)
```bash
mvn clean test
```
Bao gồm: `DiemCalculatorTest` (công thức 40/10/50, TB trọng số TC),
  `DiemDAOTest` (seed 20 môn, 4–7 môn/SV, upsert idempotent, save 1 transaction),
  `BangDiemDialogTest` (chart, thêm/xóa môn, ô trống nhập tay, chặn lưu sai, demo mode),
  `BangDiemUiTest`, `UnifiedFilterTest` (bộ lọc cộng dồn, tìm không dấu, badge, refresh).

### 4.3. Đóng gói ứng dụng thành file JAR
```bash
mvn clean package
```
File thực thi nằm tại: `target/QuanLySinhVien-1.0.0.jar`.
Chạy bằng lệnh:
```bash
java -jar target/QuanLySinhVien-1.0.0.jar
```
