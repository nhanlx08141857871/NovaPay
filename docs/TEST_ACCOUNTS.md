# NovaPay — Danh Sách Tài Khoản Kiểm Thử & Dữ Liệu Seed Mới Nhất

> **Lưu ý Học thuật (TDTU):** Tài liệu này phục vụ báo cáo & kiểm thử hệ thống Ngân hàng số NovaPay (Khoa CNTT — ĐH Tôn Đức Thắng).

---

## 1. Cơ Chế Nạp Email Thật Nhận OTP Qua `server/.env`

Mọi giao dịch tài chính trên NovaPay đều phát hành mã **OTP 6 chữ số qua Brevo API** gửi tới trường `users/{uid}.email` trên Cloud Firestore. Để bảo mật thông tin cá nhân khi lưu trữ mã nguồn trên GitHub, script [`server/scripts/seed.ts`](../server/scripts/seed.ts) tự động nạp email thật từ file cục bộ `server/.env` (đã nằm trong `.gitignore`, xem mẫu tại [`server/.env.example`](../server/.env.example)):

```env
# Email người gửi đã xác minh (Verified Sender) trên Brevo Dashboard
BREVO_SENDER_EMAIL=verified-sender@gmail.com

# 3 tài khoản khách hàng chính thức (VERIFIED) của 3 thành viên nhóm
SEED_MEMBER_1_EMAIL=<gmail-thanh-vien-1>
SEED_MEMBER_2_EMAIL=<gmail-thanh-vien-2>
SEED_MEMBER_3_EMAIL=<gmail-thanh-vien-3>

# (Tùy chọn) Gán email riêng nếu muốn nhận OTP trên các tài khoản kịch bản phụ
SEED_PENDING_EMAIL=
SEED_UNVERIFIED_EMAIL=
SEED_VERIFIED_EMAIL=
```

> **Lưu ý tránh trùng Email trên Firebase Auth:** Mỗi tài khoản Firebase Auth bắt buộc dùng một địa chỉ email duy nhất. Không gán trùng cùng một email vào cả `SEED_MEMBER_1_EMAIL` và `SEED_VERIFIED_EMAIL` (khi bỏ trống `SEED_VERIFIED_EMAIL`, hệ thống giữ gọn đúng **6 tài khoản chuẩn** dưới đây, không sinh tài khoản dư thừa).

---

## 2. Bảng Danh Sách 6 Tài Khoản Hiện Hành Trên Firebase (`novapay-71326`)

### 2.1 Nhóm 3 Tài Khoản Thành Viên Chính (`CUSTOMER` — `VERIFIED`, Đầy Đủ 3 Loại Tài Khoản)

| Họ và tên | UID Firebase | Email đăng nhập & nhận OTP | Mật khẩu | Trạng thái | Danh sách Tài khoản & Số dư khởi tạo | Phạm vi kiểm thử |
| :--- | :--- | :--- | :--- | :---: | :--- | :--- |
| **Nguyễn Phan Nhân** | `uid_member_nhan_01` | Giá trị `SEED_MEMBER_1_EMAIL` trong `server/.env`<br>*(fallback: `member1.nhan@novapay.vn`)* | `NovaPay@2026` | `ACTIVE`<br>eKYC: **`VERIFIED`** | • Checking **`1001000001`**: **`50.000.000 ₫`**<br>• Saving **`2001000001`**: **`100.000.000 ₫`** (kỳ hạn 12T, 6,0%/năm)<br>• Mortgage **`3001000001`**: dư nợ **`350.000.000 ₫`** (`MONTHLY`, `6.200.000 ₫`/kỳ) | Toàn bộ tính năng Khách hàng: Chuyển tiền nội bộ/liên NH, Quét QR, Biên lai PNG/PDF, Nạp trực tuyến Stripe, Hóa đơn, Nạp ĐT, Đặt dịch vụ, Trả nợ vay, Bản đồ, AI Chatbot |
| **Nguyễn Thành Đạt** | `uid_member_dat_02` | Giá trị `SEED_MEMBER_2_EMAIL` trong `server/.env`<br>*(fallback: `member2.dat@novapay.vn`)* | `NovaPay@2026` | `ACTIVE`<br>eKYC: **`VERIFIED`** | • Checking **`1001000002`**: **`50.000.000 ₫`**<br>• Saving **`2001000002`**: **`80.000.000 ₫`** (kỳ hạn 6T, 5,2%/năm)<br>• Mortgage **`3001000002`**: dư nợ **`240.000.000 ₫`** (`BIWEEKLY`, `3.500.000 ₫`/kỳ) | Toàn bộ tính năng Khách hàng, chuyển tiền chéo nội bộ giữa 3 thành viên, trả nợ vay chu kỳ 2 tuần |
| **Nguyễn Văn Duy** | `uid_member_duy_03` | Giá trị `SEED_MEMBER_3_EMAIL` trong `server/.env`<br>*(fallback: `member3.duy@novapay.vn`)* | `NovaPay@2026` | `ACTIVE`<br>eKYC: **`VERIFIED`** | • Checking **`1001000003`**: **`50.000.000 ₫`**<br>• Saving **`2001000003`**: **`120.000.000 ₫`** (kỳ hạn 12T, 6,0%/năm)<br>• Mortgage **`3001000003`**: dư nợ **`300.000.000 ₫`** (`MONTHLY`, `5.500.000 ₫`/kỳ) | Toàn bộ tính năng Khách hàng, nhận/chuyển tiền nội bộ & liên ngân hàng, Nạp trực tuyến, Tiện ích, AI Chatbot |

> **Chuẩn bị ảnh khuôn mặt thật cho giao dịch giá trị cao (`> 5.000.000 ₫` — FaceCheck):**
> 1. Đăng nhập tài khoản **Officer** (`officer@novapay.vn` / `NovaPay@Officer2026`) → vào **Quản lý khách hàng** → bấm **Chụp hộ eKYC tại quầy** tại thẻ của thành viên tương ứng để chụp ảnh khuôn mặt thật.
> 2. Mở mục **Duyệt eKYC & Hậu kiểm** → bấm **Duyệt** để kích hoạt lại trạng thái `VERIFIED`.
> 3. Các lần chạy lại `npm run seed` về sau sẽ **tự động giữ nguyên ảnh khuôn mặt thật** (`> 500` ký tự Base64) mà không ghi đè lại bằng ảnh mẫu.

---

### 2.2 Nhóm 3 Tài Khoản Quản Trị Ngân Hàng (`OFFICER`) & Kịch Bản Trạng Thái eKYC

| Vai trò (`role`) | Họ và tên | UID Firebase | Email đăng nhập | Mật khẩu | Trạng thái | Tài khoản mặc định & Số dư | Mục đích kiểm thử |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- | :--- |
| **`OFFICER`** | **Trần Minh Quân** | `uid_officer_novapay_01` | `officer@novapay.vn` | `NovaPay@Officer2026` | `ACTIVE`<br>eKYC: `VERIFIED` | *(Tài khoản quản trị viên ngân hàng)* | Dashboard KPI, Tạo khách hàng mới, Sửa/Khóa/Mở khóa KH, Chụp hộ eKYC tại quầy, Duyệt eKYC 2 khung ảnh, Quản lý lãi suất tiết kiệm, Nhật ký kiểm toán (`audit_logs`) |
| **`CUSTOMER`** | **Lê Thị Thu Hà** | `uid_customer_pending_02` | `customer.pending@novapay.vn`<br>*(hoặc `SEED_PENDING_EMAIL`)* | `NovaPay@User2026` | `ACTIVE`<br>eKYC: **`PENDING`** | • Checking **`1001000102`**: **`10.000.000 ₫`** | Kiểm thử hàng đợi chờ Officer duyệt/từ chối eKYC và làm số tài khoản nhận nội bộ bổ sung |
| **`CUSTOMER`** | **Phạm Quốc Bảo** | `uid_customer_unverified_03` | `customer.unverified@novapay.vn`<br>*(hoặc `SEED_UNVERIFIED_EMAIL`)* | `NovaPay@User2026` | `ACTIVE`<br>eKYC: **`UNVERIFIED`** | • Checking **`1001000103`**: **`2.000.000 ₫`** | Kiểm thử chặn giao dịch chi/rút khi chưa định danh (`EKYC_REQUIRED`) và luồng khách hàng tự chụp eKYC |

---

## 3. Mã Tra Cứu Nhanh Khi Kiểm Thử Giao Dịch & Tiện Ích

- **Số tài khoản nhận nội bộ NovaPay:**
  - `1001000001` (**Nguyễn Phan Nhân**) • `1001000002` (**Nguyễn Thành Đạt**) • `1001000003` (**Nguyễn Văn Duy**)
  - `1001000102` (**Lê Thị Thu Hà**) • `1001000103` (**Phạm Quốc Bảo**)
  - *(Mỗi thành viên nhóm đã được lưu sẵn danh bạ thụ hưởng gồm 2 thành viên còn lại và 1 tài khoản liên ngân hàng Vietcombank `0071000889911` — Trần Văn Bình).*
- **Mã khách hàng Hóa đơn Điện (`EVN TP.HCM`):**
  - `PE0100001234` — **Nguyễn Hoàng Anh** — `850.000 ₫` (`UNPAID`, kiểm thử OTP 2FA chuẩn `<= 5.000.000 ₫`).
  - `PE0100009999` — **Công ty TNHH Thương mại Nam Sài Gòn** — `6.200.000 ₫` (`UNPAID`, kiểm thử bảo mật 3 lớp `> 5.000.000 ₫`: FaceCheck + Sinh trắc học + OTP).
- **Mã khách hàng Hóa đơn Nước (`SAWACO Nhà Bè`):**
  - `NU0700005678` — **Nguyễn Hoàng Anh** — `240.000 ₫` (`UNPAID`).
  - `NU0700008888` — **Nguyễn Hoàng Anh** — `195.000 ₫` (`PAID`).
- **Mạng lưới Chi nhánh & ATM:**
  - 10 điểm giao dịch & ATM NovaBank tại TP.HCM (PGD Nguyễn Hữu Thọ — Quận 7, Chi nhánh Phú Mỹ Hưng, Hội sở Quận 1, PGD Landmark Bình Thạnh...).
