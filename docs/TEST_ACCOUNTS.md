# NovaPay — Danh Sách 4 Tài Khoản Chính Thức & Dữ Liệu Seed Mới Nhất

> **Lưu ý Học thuật (TDTU):** Tài liệu này phục vụ báo cáo & kiểm thử hệ thống Ngân hàng số NovaPay (Khoa CNTT — ĐH Tôn Đức Thắng). Toàn bộ tài khoản ảo đuôi `@novapay.vn` đã được gỡ bỏ hoàn toàn khỏi Firebase Authentication và Cloud Firestore.

---

## 1. Cơ Chế Nạp 4 Gmail Thật Nhận OTP Qua `server/.env`

Mọi giao dịch tài chính trên NovaPay đều phát hành mã **OTP 6 chữ số qua Brevo API** gửi trực tiếp tới trường `users/{uid}.email` trên Cloud Firestore. Để bảo mật thông tin cá nhân trên kho lưu trữ GitHub công khai, script [`server/scripts/seed.ts`](../server/scripts/seed.ts) **bắt buộc** đọc đủ 4 địa chỉ Gmail thật khác nhau từ tệp cục bộ `server/.env` (đã được loại trừ trong `.gitignore`, xem mẫu tại [`server/.env.example`](../server/.env.example)):

```env
# Email người gửi đã xác minh (Verified Sender) trên Brevo Dashboard
BREVO_SENDER_EMAIL=<verified-sender-gmail>

# 4 tài khoản Gmail thật duy nhất của hệ thống (1 Officer + 3 Customer)
SEED_OFFICER_EMAIL=<gmail-phu-quan-tri-officer>
SEED_MEMBER_1_EMAIL=<gmail-thanh-vien-1>
SEED_MEMBER_2_EMAIL=<gmail-thanh-vien-2>
SEED_MEMBER_3_EMAIL=<gmail-thanh-vien-3>
```

> **Cơ chế dọn sạch tự động khi chạy `npm run seed`:** Trước khi tạo/cập nhật 4 tài khoản trên, script tự động quét và xóa toàn bộ tài khoản ảo `@novapay.vn` cùng các bản ghi mẫu cũ trên cả Firebase Authentication lẫn Cloud Firestore (`users`, `beneficiaries`, `accounts`, `ledger`, `ekyc_images`, `orders`, `transactions`), đồng thời **bảo toàn nguyên vẹn ảnh khuôn mặt eKYC thật** (`> 500` ký tự Base64) mà thành viên đã chụp trước đó.

---

## 2. Bảng Danh Sách 4 Tài Khoản Hiện Hành Trên Firebase (`novapay-71326`)

| Vai trò (`role`) | Họ và tên | UID Firebase | Email đăng nhập & nhận OTP | Mật khẩu | Trạng thái | Danh sách Tài khoản & Số dư khởi tạo | Phạm vi kiểm thử |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- | :--- |
| **`OFFICER`** | **Trần Minh Quân** | `uid_officer_novapay_01` | Biến `SEED_OFFICER_EMAIL` trong `server/.env` | `NovaPay@Officer2026` | `ACTIVE`<br>eKYC: **`VERIFIED`** | *(Tài khoản quản trị viên ngân hàng)* | Dashboard KPI, tạo/sửa/khóa khách hàng, chụp hộ eKYC tại quầy, duyệt eKYC cho tài khoản `PENDING`, chỉnh biểu lãi suất tiết kiệm, xem `audit_logs` |
| **`CUSTOMER`** | **Nguyễn Phan Nhân** | `uid_member_nhan_01` | Biến `SEED_MEMBER_1_EMAIL` trong `server/.env` | `NovaPay@2026` | `ACTIVE`<br>eKYC: **`VERIFIED`** | • Checking **`1001000001`**: **`50.000.000 ₫`**<br>• Saving **`2001000001`**: **`100.000.000 ₫`** (kỳ hạn 12T, 6,0%/năm)<br>• Mortgage **`3001000001`**: dư nợ **`350.000.000 ₫`** (`MONTHLY`, `6.200.000 ₫`/kỳ) | Toàn bộ luồng Khách hàng đã xác thực: Chuyển tiền nội bộ/liên NH, Quét QR, Biên lai PNG/PDF, Nạp trực tuyến Stripe, Hóa đơn, Nạp ĐT, Đặt dịch vụ, Trả nợ vay, Bản đồ, AI Chatbot |
| **`CUSTOMER`** | **Nguyễn Thành Đạt** | `uid_member_dat_02` | Biến `SEED_MEMBER_2_EMAIL` trong `server/.env` | `NovaPay@2026` | `ACTIVE`<br>eKYC: **`VERIFIED`** | • Checking **`1001000002`**: **`50.000.000 ₫`**<br>• Saving **`2001000002`**: **`80.000.000 ₫`** (kỳ hạn 6T, 5,2%/năm)<br>• Mortgage **`3001000002`**: dư nợ **`240.000.000 ₫`** (`BIWEEKLY`, `3.500.000 ₫`/kỳ) | Toàn bộ luồng Khách hàng đã xác thực, chuyển khoản chéo giữa 3 thành viên, thanh toán hóa đơn `> 5.000.000 ₫`, trả nợ vay chu kỳ 2 tuần |
| **`CUSTOMER`** | **Nguyễn Văn Duy** | `uid_member_duy_03` | Biến `SEED_MEMBER_3_EMAIL` trong `server/.env` | `NovaPay@2026` | `ACTIVE`<br>eKYC: **`PENDING`** | • Checking **`1001000003`**: **`50.000.000 ₫`**<br>• Saving **`2001000003`**: **`120.000.000 ₫`** (kỳ hạn 12T, 6,0%/năm)<br>• Mortgage **`3001000003`**: dư nợ **`300.000.000 ₫`** (`MONTHLY`, `5.500.000 ₫`/kỳ) | Kiểm thử hàng đợi duyệt eKYC trên phân hệ Officer (`PENDING → VERIFIED`), kiểm thử chặn giao dịch `EKYC_REQUIRED` trước khi được duyệt và đầy đủ giao dịch sau khi duyệt |

> **Chuẩn bị ảnh khuôn mặt thật cho FaceCheck (`> 5.000.000 ₫`):**
> 1. Đăng nhập tài khoản **Officer** (`SEED_OFFICER_EMAIL` / `NovaPay@Officer2026`) → vào **Quản lý khách hàng** → chọn **Chụp hộ eKYC tại quầy** cho tài khoản khách hàng tương ứng (hoặc khách hàng tự chụp ở màn hình eKYC).
> 2. Vào **Duyệt eKYC & Hậu kiểm** → bấm **Phê duyệt** để kích hoạt trạng thái `VERIFIED` kèm ảnh khuôn mặt gốc thực tế.
> 3. Nếu chưa chụp ảnh khuôn mặt thật, Server sẽ chặn giao dịch `> 5.000.000 ₫` bằng mã lỗi `EKYC_FACE_NOT_FOUND` (tuyệt đối không tự cho qua).

---

## 3. Mã Tra Cứu Nhanh Khi Kiểm Thử Giao Dịch & Tiện Ích

- **Số tài khoản nhận nội bộ NovaPay (đồng bộ danh bạ thụ hưởng giữa 3 thành viên):**
  - `1001000001` (**Nguyễn Phan Nhân**) • `1001000002` (**Nguyễn Thành Đạt**) • `1001000003` (**Nguyễn Văn Duy**)
  - *(Mỗi thành viên đã được nạp sẵn danh bạ gồm 2 thành viên còn lại và 1 tài khoản liên ngân hàng Vietcombank `0071000889911` — Trần Văn Bình).*
- **Mã khách hàng Hóa đơn Điện (`EVN TP.HCM`):**
  - `PE0100001234` — **Nguyễn Phan Nhân** (`uid_member_nhan_01`) — `850.000 ₫` (`UNPAID`, kiểm thử OTP 2FA chuẩn `<= 5.000.000 ₫`).
  - `PE0100009999` — **Nguyễn Thành Đạt** (`uid_member_dat_02`) — `6.200.000 ₫` (`UNPAID`, kiểm thử bảo mật 3 lớp `> 5.000.000 ₫`: FaceCheck + Sinh trắc học + OTP).
- **Mã khách hàng Hóa đơn Nước (`SAWACO Nhà Bè`):**
  - `NU0700005678` — **Nguyễn Phan Nhân** (`uid_member_nhan_01`) — `240.000 ₫` (`UNPAID`).
  - `NU0700008888` — **Nguyễn Văn Duy** (`uid_member_duy_03`) — `195.000 ₫` (`PAID`).
- **Mạng lưới Chi nhánh & ATM:**
  - 10 điểm giao dịch & ATM NovaBank tại TP.HCM (PGD Nguyễn Hữu Thọ — Quận 7, Chi nhánh Phú Mỹ Hưng, Hội sở Quận 1, PGD Landmark Bình Thạnh...).
