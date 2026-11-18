# NovaPay — Tài Khoản Kiểm Thử & Khai Báo Google Play App Access

> **Lưu ý Academic Sandbox (TDTU):** NovaPay là ứng dụng mô phỏng nghiệp vụ ngân hàng số phục vụ học thuật & nghiên cứu tại Khoa CNTT — ĐH Tôn Đức Thắng. Không phát sinh lưu ký tiền tệ thực tế.

---

## 1. Tài Khoản Đăng Nhập Kiểm Thử (Google Play Console → App Content → App Access)

Tất cả tài khoản dưới đây được khởi tạo tự động (idempotent) bởi lệnh `npm --prefix server run seed` kèm Firebase Custom Claim `role` tương ứng:

| Vai trò (`role`) | Email đăng nhập | Mật khẩu | Trạng thái eKYC | Tài khoản mặc định & Số dư | Mục đích kiểm thử |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **CUSTOMER** *(Primary Reviewer Account)* | `customer.verified@novapay.vn` | `NovaPay@User2026` | `VERIFIED` | Checking `1001000001` (`25.000.000 ₫`), Saving `2001000001` (`100.000.000 ₫`, 12 tháng), Mortgage `3001000001` (dư nợ `350.000.000 ₫`, kỳ hạn `6.200.000 ₫`) | Kiểm thử toàn bộ luồng Khách hàng: Trang chủ, Biểu đồ, Tài khoản, Lịch sử, Chuyển tiền, OTP, FaceCheck > 5M, Hóa đơn, Dịch vụ, Bản đồ, Chatbot |
| **OFFICER** *(Bank Officer Reviewer Account)* | `officer@novapay.vn` | `NovaPay@Officer2026` | `VERIFIED` | *(Quản trị viên ngân hàng)* | Kiểm thử phân hệ Bank Officer: Dashboard, Tạo/Cập nhật/Khóa Khách hàng, Duyệt eKYC (`customer.pending@novapay.vn`), Cập nhật lãi suất, Audit Logs |
| **CUSTOMER** *(Pending eKYC)* | `customer.pending@novapay.vn` | `NovaPay@User2026` | `PENDING` | Checking `1001000002` (`10.000.000 ₫`) | Tài khoản nhận chuyển tiền nội bộ (`1001000002`) và hồ sơ chờ Officer duyệt eKYC |
| **CUSTOMER** *(Unverified eKYC)* | `customer.unverified@novapay.vn` | `NovaPay@User2026` | `UNVERIFIED` | Checking `1001000003` (`2.000.000 ₫`) | Kiểm thử chặn giao dịch khi chưa eKYC (`EKYC_REQUIRED`) và luồng chụp eKYC mới |

---

## 2. Dữ Liệu Hóa Đơn & Danh Mục Kiểm Thử

- **Mã khách hàng Điện (`ELECTRIC` — EVN TP.HCM):**
  - `PE0100001234` (`850.000 ₫` — `UNPAID`, luồng OTP chuẩn `<= 5M`)
  - `PE0100009999` (`6.200.000 ₫` — `UNPAID`, kích hoạt bảo mật 3 lớp `> 5M`: FaceCheck + Biometric + OTP)
- **Mã khách hàng Nước (`WATER` — SAWACO Nhà Bè):**
  - `NU0700005678` (`240.000 ₫` — `UNPAID`)
  - `NU0700008888` (`195.000 ₫` — `PAID`)
- **Tọa độ Fallback Bản đồ (TDTU Quận 7):** `10.7326, 106.6998` với 10 chi nhánh/ATM NovaBank tại TP.HCM.
