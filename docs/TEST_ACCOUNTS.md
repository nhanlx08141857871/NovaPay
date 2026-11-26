# NovaPay — Tài Khoản Kiểm Thử & Dữ Liệu Seed Trên Cloud Firestore

> **Lưu ý Academic Sandbox (TDTU):** NovaPay là ứng dụng mô phỏng nghiệp vụ ngân hàng số phục vụ học thuật & nghiên cứu tại Khoa CNTT — ĐH Tôn Đức Thắng. Không phát sinh lưu ký tiền tệ thực tế.

---

## 1. Cấu Hình Email Thật Nhận OTP Trước Khi Chạy `npm run seed`

Mọi giao dịch tài chính trên NovaPay đều gửi mã **OTP 6 số qua Brevo API** tới trường `users/{uid}.email` trong Cloud Firestore. Để không đưa địa chỉ email cá nhân lên GitHub, script [`server/scripts/seed.ts`](../server/scripts/seed.ts) đọc email thật từ các biến môi trường trong file cục bộ `server/.env` (đã có trong `.gitignore`, xem mẫu tại [`server/.env.example`](../server/.env.example)):

```env
# Bắt buộc dùng email đã xác minh (Verified Sender) trên Brevo Dashboard
BREVO_SENDER_EMAIL=email-da-xac-minh-tren-brevo@gmail.com

# Gán email thật nhận OTP cho 3 tài khoản thành viên nhóm (nếu bỏ trống sẽ fallback về @novapay.vn)
SEED_MEMBER_1_EMAIL=email-thanh-vien-1@gmail.com
SEED_MEMBER_2_EMAIL=email-thanh-vien-2@gmail.com
SEED_MEMBER_3_EMAIL=email-thanh-vien-3@gmail.com

# (Tùy chọn) Gán email thật cho các tài khoản test trạng thái eKYC
SEED_PENDING_EMAIL=
SEED_UNVERIFIED_EMAIL=
SEED_VERIFIED_EMAIL=
```

Sau khi cấu hình `server/.env`, chạy lệnh `npm --prefix server run seed` để đồng bộ lên Firebase Auth (kèm Custom Claim `role`) và Cloud Firestore.

---

## 2. Bảng Danh Sách Tài Khoản Kiểm Thử (Dự Án Firebase `novapay-71326`)

### 2.1 Tài Khoản 3 Thành Viên Nhóm (`CUSTOMER` — `VERIFIED`)

| Thành viên | UID trên Firebase | Biến `.env` / Email fallback mặc định | Mật khẩu | Trạng thái eKYC | Tài khoản mặc định & Số dư | Mục đích kiểm thử |
| :--- | :--- | :--- | :--- | :---: | :--- | :--- |
| **Nguyễn Phan Nhân** | `uid_member_nhan_01` | `SEED_MEMBER_1_EMAIL`<br>*(fallback: `member1.nhan@novapay.vn`)* | `NovaPay@2026` | `VERIFIED` | • Checking **`1001000001`**: **`50.000.000 ₫`**<br>• Saving `2001000001`: **`100.000.000 ₫`** (12T)<br>• Mortgage `3001000001`: dư nợ **`350.000.000 ₫`** (`MONTHLY`, `6.200.000 ₫`/kỳ) | Kiểm thử trọn vẹn luồng Khách hàng: Chuyển tiền nội bộ/liên NH, Biên lai PNG/PDF, Stripe, Hóa đơn, Dịch vụ, Trả nợ vay, Bản đồ, AI Chatbot |
| **Nguyễn Thành Đạt** | `uid_member_dat_02` | `SEED_MEMBER_2_EMAIL`<br>*(fallback: `member2.dat@novapay.vn`)* | `NovaPay@2026` | `VERIFIED` | • Checking **`1001000002`**: **`50.000.000 ₫`**<br>• Saving `2001000002`: **`80.000.000 ₫`** (6T)<br>• Mortgage `3001000002`: dư nợ **`240.000.000 ₫`** (`BIWEEKLY`, `3.500.000 ₫`/kỳ) | Kiểm thử trọn vẹn luồng Khách hàng, chuyển tiền nội bộ chéo giữa 3 thành viên, trả nợ vay định kỳ 2 tuần |
| **Nguyễn Văn Duy** | `uid_member_duy_03` | `SEED_MEMBER_3_EMAIL`<br>*(fallback: `member3.duy@novapay.vn`)* | `NovaPay@2026` | `VERIFIED` | • Checking **`1001000003`**: **`50.000.000 ₫`**<br>• Saving `2001000003`: **`120.000.000 ₫`** (12T)<br>• Mortgage `3001000003`: dư nợ **`300.000.000 ₫`** (`MONTHLY`, `5.500.000 ₫`/kỳ) | Kiểm thử trọn vẹn luồng Khách hàng, chuyển tiền nội bộ/liên NH, Stripe, Hóa đơn, Dịch vụ, Bản đồ, AI Chatbot |

> **Hướng dẫn chuẩn bị ảnh khuôn mặt thật cho FaceCheck (`> 5.000.000 ₫`):**
> Ảnh eKYC khởi tạo trong `seed.ts` là ảnh mẫu 1×1 pixel để không lưu ảnh cá nhân trên Git. Trước khi kiểm thử giao dịch trên 5 triệu:
> 1. Đăng nhập tài khoản **Officer** (`officer@novapay.vn`) → mở **Quản lý khách hàng** → bấm **Chụp hộ eKYC tại quầy** cho tài khoản thành viên tương ứng để chụp ảnh khuôn mặt thật.
> 2. Vào **Duyệt eKYC** → bấm **Duyệt** để chuyển lại sang `VERIFIED`.
> 3. Khi chạy lại `npm run seed`, script tự động **bảo toàn ảnh khuôn mặt thật** (`> 500` ký tự Base64) và không ghi đè lại bằng ảnh 1×1 pixel.

### 2.2 Tài Khoản Sandbox Phân Hệ Officer & Kịch Bản Trạng Thái eKYC

| Vai trò (`role`) | UID trên Firebase | Biến `.env` / Email fallback mặc định | Mật khẩu | Trạng thái eKYC | Tài khoản mặc định & Số dư | Mục đích kiểm thử |
| :--- | :--- | :--- | :--- | :---: | :--- | :--- |
| **OFFICER** *(Quản trị)* | `uid_officer_novapay_01` | `officer@novapay.vn` *(không dùng OTP)* | `NovaPay@Officer2026` | `VERIFIED` | *(Nhân viên ngân hàng)* | Dashboard Officer, Tạo/Sửa/Khóa Khách hàng, Chụp hộ eKYC tại quầy, Duyệt eKYC, Quản lý lãi suất, Xem Audit Logs |
| **CUSTOMER** *(Chờ duyệt)* | `uid_customer_pending_02` | `SEED_PENDING_EMAIL`<br>*(fallback: `customer.pending@novapay.vn`)* | `NovaPay@User2026` | `PENDING` | • Checking `1001000102`: **`10.000.000 ₫`** | Kiểm thử luồng Officer phê duyệt/từ chối hồ sơ eKYC và làm tài khoản nhận nội bộ bổ sung (`1001000102` — Lê Thị Thu Hà) |
| **CUSTOMER** *(Chưa định danh)* | `uid_customer_unverified_03` | `SEED_UNVERIFIED_EMAIL`<br>*(fallback: `customer.unverified@novapay.vn`)* | `NovaPay@User2026` | `UNVERIFIED` | • Checking `1001000103`: **`2.000.000 ₫`** | Kiểm thử chặn giao dịch chi/rút khi chưa định danh (`EKYC_REQUIRED`) và luồng khách tự chụp khuôn mặt eKYC mới |

*(Nếu khai báo thêm biến tùy chọn `SEED_VERIFIED_EMAIL` trong `server/.env`, `seed.ts` sẽ tạo thêm tài khoản sandbox `uid_customer_verified_01` với Checking `1001000101` — `25.000.000 ₫`.)*

---

## 3. Dữ Liệu Mẫu Hỗ Trợ Kiểm Thử Giao Dịch & Tiện Ích

- **Tài khoản nhận chuyển tiền nội bộ NovaPay (3 thành viên nhóm):**
  - `1001000001` — **Nguyễn Phan Nhân**
  - `1001000002` — **Nguyễn Thành Đạt**
  - `1001000003` — **Nguyễn Văn Duy**
  - *(Tài khoản nhận sandbox bổ sung: `1001000102` — Lê Thị Thu Hà, `1001000103` — Phạm Quốc Bảo)*
- **Mã khách hàng Hóa đơn Điện (`ELECTRIC` — EVN TP.HCM):**
  - `PE0100001234` — Số tiền: `850.000 ₫` (`UNPAID` — kiểm thử luồng OTP 2FA chuẩn `<= 5.000.000 ₫`).
  - `PE0100009999` — Số tiền: `6.200.000 ₫` (`UNPAID` — kích hoạt xác thực 3 lớp `> 5.000.000 ₫`: FaceCheck + Biometric + OTP).
- **Mã khách hàng Hóa đơn Nước (`WATER` — SAWACO Nhà Bè):**
  - `NU0700005678` — Số tiền: `240.000 ₫` (`UNPAID`).
  - `NU0700008888` — Số tiền: `195.000 ₫` (`PAID`).
- **Bản đồ Chi nhánh & Fallback GPS:**
  - 10 chi nhánh & cây ATM NovaBank tại TP.HCM; tọa độ fallback mặc định tại **ĐH Tôn Đức Thắng (`10.7326, 106.6998` — Quận 7, TP.HCM)**.
