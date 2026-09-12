-- Script khởi tạo CSDL Quản Lý Sinh Viên
CREATE DATABASE IF NOT EXISTS QuanLySinhVien CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE QuanLySinhVien;

-- Xóa bảng nếu đã tồn tại
DROP TABLE IF EXISTS SinhVien;

-- Tạo bảng SinhVien theo đúng yêu cầu đề bài
CREATE TABLE SinhVien (
    MaSV VARCHAR(10) NOT NULL PRIMARY KEY,
    HoTen VARCHAR(50) NOT NULL,
    Lop VARCHAR(20) NOT NULL,
    NgaySinh DATE NOT NULL,
    DiemTB FLOAT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Thêm ít nhất 10 bản ghi mẫu phục vụ kiểm thử
INSERT INTO SinhVien (MaSV, HoTen, Lop, NgaySinh, DiemTB) VALUES
('SV001', 'Nguyễn Văn An', 'CNTT1', '2003-05-15', 8.5),
('SV002', 'Trần Thị Bích', 'CNTT1', '2003-08-20', 9.2),
('SV003', 'Lê Hoàng Cường', 'CNTT2', '2003-01-10', 7.0),
('SV004', 'Phạm Minh Đức', 'CNTT2', '2003-11-25', 6.5),
('SV005', 'Hoàng Thu Hà', 'KTPM1', '2003-03-30', 8.8),
('SV006', 'Đỗ Tuấn Hải', 'KTPM1', '2003-07-12', 7.8),
('SV007', 'Vũ Thị Mai', 'HTTT1', '2003-09-05', 9.5),
('SV008', 'Bùi Quang Nam', 'HTTT1', '2003-12-18', 5.5),
('SV009', 'Ngô Phương Oanh', 'CNTT1', '2003-04-22', 8.0),
('SV010', 'Đặng Quốc Việt', 'CNTT2', '2003-10-08', 9.0);
