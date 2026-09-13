# Đặc Tả Thiết Kế: Giao Diện Dashboard Hiện Đại & Mở Rộng Bảng Sinh Viên (Smart Pathshala Inspired)

**Dự án:** Quản Lý Sinh Viên & Tiện Ích (`QuanLySinhVien`)  
**Ngày:** 2026-09-13  
**Mục tiêu:** Chuyển đổi kiến trúc giao diện từ TabbedPane truyền thống sang **Modern SaaS School Dashboard** (lấy cảm hứng từ giao diện *Smart Pathshala*), tích hợp **Thanh Sidebar thu gọn thông minh**, **Hàng thẻ chỉ số KPI**, và giải quyết triệt để vấn đề **diện tích hiển thị bảng sinh viên (xem được 30+ sinh viên cùng lúc)** thông qua cơ chế Form thu gọn (Collapsible Form), Nút Phóng to bảng (Maximize View) và Chế độ mật độ dòng (Compact/Comfortable Density).

---

## 1. Kiến trúc Bố cục Tổng thể (Application Layout Architecture)

Giao diện cửa sổ chính (`MainFrame.java`) được tái cấu trúc thành hệ thống 3 khối hiện đại:

```
+---------------------------------------------------------------------------------------------------------+
|                                    CUSTOM WINDOW TITLE BAR                                              |
+-------------------+-------------------------------------------------------------------------------------+
|                   |  TOP HEADER BAR: "Xin chào, Admin!" | [● MySQL Sẵn sàng] | [Avatar Admin] [Đăng xuất] |
|                   +-------------------------------------------------------------------------------------+
|  MODERN SIDEBAR   |  MAIN CONTENT VIEW (Dynamic Card Layout / Animated Switcher)                        |
|                   |                                                                                     |
|  [Logo & Brand]   |  1. KPI METRIC CARDS ROW:                                                           |
|  - Dashboard      |     [ Tổng SV: 84 ]  [ Lớp học: 6 ]  [ Thủ khoa: 9.5 ]  [ Xuất sắc/Giỏi: 32 ]       |
|  - Sinh Viên (★)  |                                                                                     |
|  - Máy Tính       |  2. COLLAPSIBLE FORM PANEL:                                                         |
|  - Thống Kê       |     [▼ Thông tin Sinh viên] (Có nút Ẩn/Hiện Form để nhường diện tích cho Bảng)      |
|  - Giới Thiệu     |     - Mã SV | Họ tên | Lớp | Ngày sinh | Điểm TB + 4 Nút Thao tác                   |
|                   |                                                                                     |
|  [◀ Thu gọn Bar]  |  3. EXPANDED STUDENT TABLE CARD (Chiếm 75% - 100% diện tích màn hình):              |
|                   |     - Toolbar: [Tìm kiếm...] [Bộ lọc...] [Sắp xếp...] [Nạp/Xuất File]               |
|                   |     - Controls: [⛶ Phóng to Bảng] [≡ Mật độ: Thu gọn (26px) / Rộng rãi (34px)]      |
|                   |     - JTable 7 cột hiển thị Zebra + Badges xếp loại                                 |
+-------------------+-------------------------------------------------------------------------------------+
|  FOOTER STATUS    |  Người dùng: admin | Hệ thống: Sẵn sàng | Phiên bản: 2.0 (Smart Dashboard)            |
+-------------------+-------------------------------------------------------------------------------------+
```

---

## 2. Chi tiết Các Thành phần Mới

### 2.1. Thanh Sidebar Điều Hướng (`ModernSidebar.java`)
- **Chiều rộng mặc định:** `210px`.
- **Chế độ thu nhỏ (Icon-only mode):** `64px` (khi người dùng bấm nút thu gọn ở đáy sidebar, giúp giải phóng thêm `146px` chiều ngang cho bảng sinh viên).
- **Các mục điều hướng:**
  1. `Quản Lý Sinh Viên` (Icon Mũ cử nhân / Học sinh - Màn hình chính)
  2. `Máy Tính Tiện Ích` (Icon Máy tính - Bài 2)
  3. `Báo Cáo Thống Kê` (Icon Biểu đồ - Xem thống kê lớp, thủ khoa)
  4. `Giới Thiệu Hệ Thống` (Icon Thông tin / Hướng dẫn)
- **Hiệu ứng trực quan:**
  - Nền xám sáng tinh tế `#FFFFFF` hoặc `#F8FAFC`, phân cách bằng đường viền mỏng `1px #E2E8F0`.
  - Mục được chọn (Active item) có dải chỉ báo màu tím Indigo `#635BFF` ở mép trái, nền đổi sang `#F0EFFF` bo góc tròn mềm mại.

### 2.2. Thanh Đầu Trang (`TopHeaderBar.java`)
- **Chiều cao:** `56px`, nền trắng `#FFFFFF` viền đáy mỏng.
- **Bên trái:** Lời chào cá nhân hóa *"Xin chào, Quản trị viên (admin)!"* và breadcrumb định vị trang.
- **Bên phải:**
  - Badge trạng thái CSDL hình viên thuốc (Pill Badge): Xanh lục *"● MySQL Online"* hoặc Vàng *"○ Chế độ Offline"*.
  - Nút Avatar bo tròn chứa chữ cái đại diện `A` và nút **Đăng xuất (Sign Out)** dạng thẻ nổi bật giống ảnh mẫu.

### 2.3. Hàng Thẻ Chỉ Số Tổng Quan (KPI Metric Cards)
- 4 thẻ thống kê trực quan đặt ngang trên đầu màn hình Quản lý Sinh viên:
  - **Thẻ 1 (Xanh dương):** Tổng số sinh viên (`84` sinh viên).
  - **Thẻ 2 (Xanh lục):** Số lượng lớp học (`6` lớp).
  - **Thẻ 3 (Hổ phách):** Điểm trung bình cao nhất / Thủ khoa (`9.5` điểm).
  - **Thẻ 4 (Tím):** Tỷ lệ sinh viên Khá - Giỏi (`68.5%`).
- Mỗi thẻ gồm: Icon trong hình tròn màu nhạt, số liệu in đậm cỡ lớn (22pt) và nhãn phụ thanh lịch.

### 2.4. Giải Pháp Đột Phá Mở Rộng Diện Tích Bảng Sinh Viên
Để giải quyết triệt để yêu cầu *"diện tích hiện tại hơi bé, cần xem thêm nhiều sinh viên"*:
1. **Form nhập liệu có thể Thu gọn/Mở rộng (Collapsible Card):**
   - Tiêu đề Card có nút bấm `[Thu gọn Form ▲]` / `[Mở rộng Form ▼]`.
   - Khi chọn 1 dòng trên bảng để sửa: Form tự động mở ra và điền thông tin.
   - Khi muốn xem danh sách: Bấm thu gọn Form, toàn bộ diện tích của Form được giải phóng ngay lập tức cho Bảng JTable!
2. **Nút Phóng To Toàn Màn Hình Bảng (Maximize / Full-view Table):**
   - Thêm nút biểu tượng `[⛶ Phóng to]` trên thanh công cụ của Bảng: Khi bấm, cả hàng thẻ KPI và Form nhập liệu đều được ẩn đi, Bảng dữ liệu chiếm **100% chiều cao màn hình**, hiển thị cùng lúc **30 đến 40 sinh viên** mà không cần cuộn!
   - Bấm lại nút này để quay về chế độ xem tiêu chuẩn.
3. **Nút Chuyển Đổi Mật Độ Dòng (Compact / Comfortable Density Toggle):**
   - Chế độ **Thoáng đãng (Comfortable - 34px)**: Dòng bảng rộng rãi, phù hợp kiểm tra chi tiết.
   - Chế độ **Thu gọn (Compact - 26px)**: Thu nhỏ chiều cao dòng và padding, tăng số lượng sinh viên hiển thị trên màn hình lên gấp đôi!

---

## 3. Bảo Toàn Hợp Đồng Chức Năng & Kiểm Thử

1. **Hợp đồng `UIComponentTest`:**
   - Bảo toàn toàn bộ các thành phần và test hook (`getTable()`, `getTableModel()`, `getTxtMaSV()`, `getBtnAdd()`, `getBtnUpdate()`, `getBtnDelete()`, `getBtnClear()`, `checkCredentials()`, `Calculator.compute()`).
   - Đảm bảo `MainFrame` cung cấp phương thức hoặc cấu trúc tương thích cho phép truy xuất `StudentManagementPanel` và `CalculatorPanel`.
2. **Quy tắc Khóa Khóa Chính (Primary Key Lock):**
   - Chọn dòng trên bảng -> khóa ô `txtMaSV` (`setEditable(false)`).
   - Bấm "Làm mới form" -> mở khóa ô `txtMaSV` (`setEditable(true)`).
3. **Cơ chế Fallback nạp offline `sinhvien.txt`:**
   - Tiếp tục duy trì để `mvn test` chạy trơn tru ngay cả khi không có CSDL MySQL.
