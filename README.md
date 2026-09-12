# HỆ THỐNG QUẢN LÝ SINH VIÊN - JAVA SWING & MYSQL

Dự án ứng dụng Desktop hoàn chỉnh xây dựng bằng **Java Swing** kết nối cơ sở dữ liệu **MySQL 8.0** qua JDBC thuần và hỗ trợ đọc/ghi **Text File**.

---

## 1. Cấu trúc kiến trúc tối giản (Simple 3-Tier)
Tuân thủ nguyên tắc chống over-engineering theo [0001-simplified-swing-architecture.md](docs/adr/0001-simplified-swing-architecture.md):
```
src/main/java/com/quanlysinhvien/
├── Main.java                          # Điểm khởi chạy ứng dụng (Nimbus Look & Feel)
├── model/
│   └── SinhVien.java                  # Entity Domain Model + Validation dữ liệu
├── dao/
│   ├── DatabaseConnection.java        # Quản lý kết nối JDBC, đọc db.properties, auto-init CSDL
│   └── SinhVienDAO.java               # Thực thi CRUD, Tìm kiếm, Sắp xếp, Thống kê, Đọc/Ghi File
└── ui/
    ├── LoginForm.java                 # Bài 1: Form đăng nhập
    ├── CalculatorPanel.java           # Bài 2: Máy tính cơ bản (+, -, *, /, bắt lỗi chia cho 0)
    ├── StudentManagementPanel.java    # Bài 3 & P1-P8: Giao diện Quản lý Sinh viên toàn diện
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
5. **Tìm kiếm sinh viên:**
   - Tìm kiếm linh hoạt theo: `Tất cả`, `Mã SV`, `Họ tên`, `Lớp`.
   - Nút `Tất cả` để làm mới danh sách ban đầu.
6. **Sắp xếp:**
   - Sắp xếp theo: Tên sinh viên (A-Z, Z-A), Điểm trung bình (Tăng dần, Giảm dần), Mã SV.
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

### 4.2. Chạy toàn bộ Test tự động (18 Tests)
```bash
mvn test
```

### 4.3. Đóng gói ứng dụng thành file JAR
```bash
mvn clean package
```
File thực thi nằm tại: `target/QuanLySinhVien-1.0.0.jar`.
Chạy bằng lệnh:
```bash
java -jar target/QuanLySinhVien-1.0.0.jar
```
