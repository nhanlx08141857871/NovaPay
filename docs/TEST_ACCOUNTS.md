# NovaPay — Tài Khoản Kiểm Thử & Dữ Liệu Seed Trên Cloud Firestore

> **Lưu ý Academic Sandbox (TDTU):** NovaPay là ứng dụng mô phỏng nghiệp vụ ngân hàng số phục vụ học thuật & nghiên cứu tại Khoa CNTT — ĐH Tôn Đức Thắng. Không phát sinh lưu ký tiền tệ thực tế.

---

## 1. Bảng Danh Sách Tài Khoản Kiểm Thử (Đã Khởi Tạo Trên Firebase `novapay-71326`)

Tất cả tài khoản dưới đây đã được nạp trực tiếp lên **Firebase Authentication** (kèm Custom Claim `role`) và **Cloud Firestore** qua lệnh `npm --prefix server run seed`:

| Vai trò (`role`) | UID trên Firebase | Email đăng nhập | Mật khẩu | Trạng thái eKYC | Tài khoản mặc định & Số dư | Mục đích kiểm thử |
| :--- | :--- | :--- | :--- | :---: | :--- | :--- |
| **CUSTOMER** *(Chính)* | `uid_customer_verified_01` | `customer.verified@novapay.vn` | `NovaPay@User2026` | `VERIFIED` | • Checking `1001000001`: **`25.000.000 ₫`**<br>• Saving `2001000001`: **`100.000.000 ₫`** (kỳ hạn 12 tháng)<br>• Mortgage `3001000001`: dư nợ **`350.000.000 ₫`** (kỳ trả `6.200.000 ₫`) | Kiểm thử trọn vẹn luồng Khách hàng: Trang chủ, Biểu đồ thu/chi, Tài khoản, Lịch sử, Chuyển tiền, Quét QR, Nạp Stripe, OTP, FaceCheck > 5M, Hóa đơn, Dịch vụ, Bản đồ, AI Chatbot |
| **OFFICER** *(Quản trị)* | `uid_officer_novapay_01` | `officer@novapay.vn` | `NovaPay@Officer2026` | `VERIFIED` | *(Nhân viên ngân hàng)* | Kiểm thử phân hệ Bank Officer: Dashboard, Tạo mới / Cập nhật / Khóa Khách hàng, Chụp & Duyệt eKYC (`customer.pending@novapay.vn`), Cập nhật bảng lãi suất, Xem Audit Logs |
| **CUSTOMER** *(Chờ duyệt)* | `uid_customer_pending_02` | `customer.pending@novapay.vn` | `NovaPay@User2026` | `PENDING` | • Checking `1001000002`: **`10.000.000 ₫`** | Dùng làm **tài khoản nhận chuyển tiền nội bộ** (`1001000002` — Lê Thị Thu Hà) và kiểm thử luồng Officer phê duyệt hồ sơ eKYC |
| **CUSTOMER** *(Chưa định danh)* | `uid_customer_unverified_03` | `customer.unverified@novapay.vn` | `NovaPay@User2026` | `UNVERIFIED` | • Checking `1001000003`: **`2.000.000 ₫`** | Kiểm thử chặn giao dịch chi/rút khi chưa định danh (`EKYC_REQUIRED`) và luồng khách tự chụp khuôn mặt eKYC mới |

---

## 2. Dữ Liệu Mẫu Hỗ Trợ Kiểm Thử Giao Dịch & Tiện Ích

- **Tài khoản nhận chuyển tiền nội bộ NovaPay:**
  - Số tài khoản: `1001000002` (Chủ TK: **Lê Thị Thu Hà**) hoặc `1001000003` (Chủ TK: **Phạm Quốc Bảo**).
- **Mã khách hàng Hóa đơn Điện (`ELECTRIC` — EVN TP.HCM):**
  - `PE0100001234` — Số tiền: `850.000 ₫` (`UNPAID` — kiểm thử luồng OTP 2FA chuẩn `<= 5.000.000 ₫`).
  - `PE0100009999` — Số tiền: `6.200.000 ₫` (`UNPAID` — kích hoạt xác thực 3 lớp `> 5.000.000 ₫`: FaceCheck + Biometric + OTP).
- **Mã khách hàng Hóa đơn Nước (`WATER` — SAWACO Nhà Bè):**
  - `NU0700005678` — Số tiền: `240.000 ₫` (`UNPAID`).
  - `NU0700008888` — Số tiền: `195.000 ₫` (`PAID`).
- **Bản đồ Chi nhánh & Fallback GPS:**
  - 10 chi nhánh & cây ATM NovaBank tại TP.HCM; tọa độ fallback mặc định tại **ĐH Tôn Đức Thắng (`10.7326, 106.6998` — Quận 7, TP.HCM)**.
