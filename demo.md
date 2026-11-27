# KỊCH BẢN KIỂM THỬ THỦ CÔNG, CHỤP ẢNH BÁO CÁO & QUAY VIDEO DEMO — NOVAPAY

> **Mục đích tài liệu:** Cẩm nang thực hành "3 trong 1" dành cho nhóm thực hiện dự án **NovaPay (`com.novabank.novapay`)** để:
> 1. **Kiểm thử thủ công toàn diện (Manual QA Checklist):** Rà soát 100% luồng nghiệp vụ `CUSTOMER` & `OFFICER` và 16 kịch bản biên (`TC-01` → `TC-16`).
> 2. **Thu hoạch trọn bộ 18 ảnh chụp màn hình (`Hình 4-1` → `Hình 4-18`):** Đi đúng 1 vòng từ trên xuống dưới là chụp đủ toàn bộ hình minh họa khớp 1–1 với Chương 4.3 trong `docs/report/BAO_CAO.md`.
> 3. **Quay Video Demo & Bảo vệ trực tiếp (8–10 phút):** Có sẵn kịch bản phân cảnh theo phút và lời thoại thuyết minh (*Voiceover*) nhấn đúng các điểm ăn điểm kỹ thuật cao nhất.

---

## PHẦN 1. CHUẨN BỊ MÔI TRƯỜNG TRƯỚC KHI TEST & BẤM MÁY QUAY (5 PHÚT)

Thực hiện lần lượt 5 bước chuẩn bị dưới đây để quá trình test và quay video mượt tuyệt đối, không bị khựng hay văng lỗi cấu hình:

- [ ] **Bước 1 — Đánh thức hoặc khởi chạy Backend Server:**
  - Nếu dùng máy chủ **Render Cloud**: Mở trình duyệt truy cập `<BACKEND_BASE_URL>/health` (chờ 15–30 giây nếu container ngủ đông) cho tới khi nhận được `{"ok":true}`.
  - Nếu chạy máy chủ cục bộ để quay video tốc độ phản hồi cực nhanh:
    ```powershell
    cd server
    npm run dev
    ```
- [ ] **Bước 2 — Khởi tạo lại dữ liệu mẫu sạch đẹp (nếu cần số dư tròn đẹp `50.000.000 ₫` trước khi bấm máy quay):**
  ```powershell
  cd server
  npm run seed
  ```
  *(Lệnh `seed` sẽ đặt lại số dư gốc, các hóa đơn `UNPAID` và **giữ nguyên ảnh khuôn mặt eKYC thật** nếu bạn đã chụp trước đó).*
- [ ] **Bước 3 — Chuẩn bị 2 Thiết bị / Máy ảo Android song song (Khuyên dùng để quay cảnh Realtime cực ăn điểm):**
  - **Máy 1 (Màn hình chính — Vai trò `CUSTOMER`):** Đăng nhập tài khoản **Nguyễn Phan Nhân** (`SEED_MEMBER_1_EMAIL`), mở sẵn ứng dụng **Gmail** của email này trên điện thoại hoặc tab trình duyệt kế bên để lấy mã OTP 6 số thật trong 1 giây.
  - **Máy 2 (Màn hình phụ cạnh bên — Vai trò `OFFICER` & `CUSTOMER 2`):** Dùng để đăng nhập **Trần Minh Quân** (`SEED_OFFICER_EMAIL`) khi demo quản trị thời gian thực, hoặc đăng nhập **Nguyễn Thành Đạt** (`SEED_MEMBER_2_EMAIL`) khi muốn quay cảnh 2 máy cùng nổ thông báo biến động số dư.
- [ ] **Bước 4 — Cài đặt khóa màn hình & Vân tay trên Máy ảo (nếu quay bằng Android Emulator):**
  - Vào **Settings → Security → Device lock / Pixel Imprint (Fingerprint)** trên máy ảo, đặt mã PIN máy (ví dụ `1111`).
  - Khi tới bước chạm vân tay trên máy ảo, chạy lệnh chạm cảm biến ảo trên Terminal:
    ```powershell
    adb emu finger touch 1
    ```
  - Cấp đủ 3 quyền cho ứng dụng NovaPay khi được hỏi: **Camera** (chụp eKYC / FaceCheck / QR), **Notifications** (hiện thông báo nổi biến động số dư) và **Location** (bản đồ chi nhánh).
- [ ] **Bước 5 (BẮT BUỘC TRƯỚC KHI TEST GIAO DỊCH `> 5.000.000 ₫`) — Nạp ảnh khuôn mặt thật vào hồ sơ eKYC:**
  - Do hệ thống áp dụng chuẩn **Zero-False-Acceptance**, nếu tài khoản mới chỉ có ảnh placeholder 1x1 từ lệnh `seed`, giao dịch `> 5.000.000 ₫` sẽ bị chặn bởi mã lỗi `403 EKYC_FACE_NOT_FOUND`.
  - Cách làm nhanh (mất 30 giây): Đăng nhập **Nguyễn Phan Nhân** → vào **Hồ sơ → Định danh eKYC** chụp khuôn mặt thật của người ngồi quay demo → qua Máy 2 đăng nhập **Officer Trần Minh Quân** → vào **Duyệt eKYC** bấm **Phê duyệt (`VERIFIED`)**. Từ lúc này, mọi giao dịch `> 5.000.000 ₫` sẽ đối chiếu trực tiếp với khuôn mặt thật của bạn!

---

## PHẦN 2. BẢNG TRA CỨU NHANH DỮ LIỆU KIỂM THỬ (CHEAT SHEET)

Giữ bảng này mở sẵn ở màn hình phụ khi kiểm thử hoặc quay video để copy-paste tức thì:

### 2.1 Danh sách 4 tài khoản chính thức (Email lấy từ `server/.env`)

| Vai trò | Họ và tên | Biến Email trong `server/.env` | Mật khẩu | Trạng thái gốc | Số TK Thanh toán (`CHECKING`) | Số TK Tiết kiệm (`SAVING`) | Số TK Vay (`MORTGAGE`) |
| :---: | :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| **`OFFICER`** | **Trần Minh Quân** | `SEED_OFFICER_EMAIL` | `NovaPay@Officer2026` | `ACTIVE` / `VERIFIED` | *(Quản trị viên)* | — | — |
| **`CUSTOMER 1`** | **Nguyễn Phan Nhân** | `SEED_MEMBER_1_EMAIL` | `NovaPay@2026` | `ACTIVE` / `VERIFIED` | **`1001000001`** (`50.000.000 ₫`) | **`2001000001`** (`100M`, 12T, `6.0%`) | **`3001000001`** (Dư nợ `350M`, `6,2M`/tháng) |
| **`CUSTOMER 2`** | **Nguyễn Thành Đạt** | `SEED_MEMBER_2_EMAIL` | `NovaPay@2026` | `ACTIVE` / `VERIFIED` | **`1001000002`** (`50.000.000 ₫`) | **`2001000002`** (`80M`, 6T, `5.2%`) | **`3001000002`** (Dư nợ `240M`, `3,5M`/2 tuần) |
| **`CUSTOMER 3`** | **Nguyễn Văn Duy** | `SEED_MEMBER_3_EMAIL` | `NovaPay@2026` | `ACTIVE` / **`PENDING`** | **`1001000003`** (`50.000.000 ₫`) | **`2001000003`** (`120M`, 12T, `6.0%`) | **`3001000003`** (Dư nợ `300M`, `5,5M`/tháng) |

### 2.2 Mã tra cứu giao dịch, Hóa đơn, Thẻ Stripe & Câu hỏi Chatbot mẫu

| Nhóm nghiệp vụ | Dữ liệu nhập để kiểm thử | Ý nghĩa / Kịch bản kiểm chứng |
| :--- | :--- | :--- |
| **Chuyển tiền Nội bộ** | Số TK: **`1001000002`** | Tự động hiện tên **Nguyễn Thành Đạt** qua `GET /accounts/lookup` |
| **Chuyển tiền Liên ngân hàng** | Ngân hàng: **`Vietcombank (VCB)`** — Số TK: **`0071000889911`** | Đã có sẵn trong danh bạ thụ hưởng (*Anh Bình VCB — Trần Văn Bình*) |
| **Hóa đơn Điện `<= 5M`** | Mã KH: **`PE0100001234`** (`EVN TP.HCM`) | Số tiền **`850.000 ₫`** (`UNPAID`) — Kiểm thử luồng OTP 2FA chuẩn |
| **Hóa đơn Điện `> 5M` (Đinh)** | Mã KH: **`PE0100009999`** (`EVN TP.HCM`) | Số tiền **`6.200.000 ₫`** (`UNPAID`) — Kích hoạt đủ **3 lớp bảo mật** (`FaceCheck` + Vân tay + OTP) |
| **Hóa đơn Nước** | Mã KH: **`NU0700005678`** (`240.000 ₫`, `UNPAID`) / **`NU0700008888`** (`PAID`) | Kiểm thử lọc loại hóa đơn Nước & trạng thái đã thanh toán |
| **Thẻ Quốc tế Stripe Test** | Số thẻ: **`4242 4242 4242 4242`** • Hạn: **`12/30`** • CVC: **`123`** • ZIP: **`70000`** | Thẻ Visa kiểm thử chuẩn của Stripe trên `PaymentSheet` (luôn `succeeded`) |
| **Thẻ Stripe Từ chối (Test lỗi)** | Số thẻ: **`4000 0000 0000 0002`** • Hạn: **`12/30`** • CVC: **`123`** | Kiểm thử thẻ bị từ chối — đảm bảo số dư tài khoản `CHECKING` **không bị cộng khống** |
| **Smart PIN Giao dịch 6 số** | PIN gợi ý khi thiết lập: **`123456`** (hoặc **`246810`**) | Dùng cho Mở/Tất toán sổ tiết kiệm, chỉnh hạn mức và khóa thẻ khẩn cấp |
| **Câu lệnh hỏi AI Chatbot** | *"Tháng này tôi thu chi bao nhiêu và còn bao nhiêu tiền? Cho tôi đóng tiền điện"* | Kiểm chứng AI đọc đúng số dư thực tế + hiện nút DeepLink `[Thanh toán hóa đơn]` |

---

## PHẦN 3. QUY TRÌNH KIỂM THỬ THỦ CÔNG TỪ A → Z & CHỤP ĐỦ 18 ẢNH BÁO CÁO

> **Quy ước:** Đi tuần tự từ **Giai đoạn 1 → Giai đoạn 6**. Tại mỗi dòng có biểu tượng **`📸 [CHỤP ẢNH BÁO CÁO — Hình 4-X]`**, hãy chụp màn hình lưu lại theo đúng tên `Hinh_4_X.png` để chèn thẳng vào `docs/report/BAO_CAO.md`!

### GIAI ĐOẠN 1: Xác thực, Định danh eKYC hai chiều & Phân quyền (`Hình 4-1`, `Hình 4-6a`, `Hình 4-17`)

- [ ] **1.1 Kiểm tra màn hình Đăng nhập & Quên mật khẩu:**
  - Mở ứng dụng NovaPay. Bấm **Quên mật khẩu?**, nhập email `SEED_MEMBER_1_EMAIL` để kiểm tra hộp thoại khôi phục mật khẩu qua Firebase Auth, sau đó đóng hộp thoại.
  - Đăng nhập tài khoản **Nguyễn Phan Nhân** (`SEED_MEMBER_1_EMAIL` / `NovaPay@2026`).
  - Vào **Hồ sơ → Cài đặt bảo mật** (hoặc hộp thoại gợi ý sau đăng nhập) bật **Đăng nhập nhanh bằng sinh trắc học**, đăng xuất ra lại màn hình Login và bấm nút **Đăng nhập bằng vân tay** để hiện `BiometricPrompt`.
  - **📸 [CHỤP ẢNH BÁO CÁO — Hình 4-1]:** Chụp (1) Màn hình Đăng nhập thương mại và (2) Hộp thoại quét vân tay `BiometricPrompt` (hoặc hộp thoại Quên mật khẩu) để ghép thành *Hình 4-1*.
- [ ] **1.2 Kiểm tra chụp eKYC có Liveness tại biên (`Customer`) & Chặn giao dịch khi `PENDING`:**
  - Đăng nhập tài khoản **Nguyễn Văn Duy** (`SEED_MEMBER_3_EMAIL` / `NovaPay@2026` — đang ở trạng thái `eKYC: PENDING`).
  - Thử vào **Chuyển tiền**, nhập chuyển `100.000 ₫` → Xác nhận hệ thống **chặn ngay** với thông báo yêu cầu hoàn tất định danh eKYC (`EKYC_REQUIRED`).
  - Mở màn hình **Định danh khuôn mặt eKYC (`EkycCaptureFragment`)**:
    - Thử nghiêng đầu sang trái `> 15°` → Kiểm tra dòng chữ nhắc *"Vui lòng nhìn thẳng vào camera"*.
    - Thử nhắm mắt → Kiểm tra dòng chữ nhắc *"Vui lòng mở cả hai mắt"*, nút chụp bị vô hiệu hóa.
    - Nhìn thẳng, mở rõ hai mắt → Khung chuyển xanh *"Khuôn mặt hợp lệ — Sẵn sàng chụp"* → Bấm chụp & gửi hồ sơ (`status: PENDING`).
  - *(Làm tương tự một lần chụp khuôn mặt thật cho tài khoản chính **Nguyễn Phan Nhân** để phục vụ bước `FaceCheck > 5M` ở Giai đoạn 3).*
  - **📸 [CHỤP ẢNH BÁO CÁO — Hình 4-6 (nửa trái)]:** Chụp màn hình camera eKYC khi báo khung xanh hợp lệ.
- [ ] **1.3 Kiểm tra Officer duyệt hồ sơ eKYC (`PENDING → VERIFIED`):**
  - Trên Máy 2, đăng nhập **Officer Trần Minh Quân** (`SEED_OFFICER_EMAIL` / `NovaPay@Officer2026`).
  - Vào **Duyệt eKYC & Hậu kiểm (`OfficerEkycReviewFragment`)**, kiểm tra ảnh chân dung vừa chụp hiện rõ nét.
  - **📸 [CHỤP ẢNH BÁO CÁO — Hình 4-17]:** Chụp màn hình Duyệt hồ sơ eKYC & Hậu kiểm trên tài khoản Officer (trước khi bấm nút Phê duyệt để thấy rõ thẻ hồ sơ chờ duyệt và 2 nút *Phê duyệt / Từ chối*).
  - Bấm **Phê duyệt (`VERIFIED`)** cho cả hồ sơ của **Nguyễn Phan Nhân** và **Nguyễn Văn Duy** → Kiểm tra trên máy khách hàng nhận được thông báo đẩy xác thực eKYC thành công.

---

### GIAI ĐOẠN 2: Trang chủ, 3 Loại Tài khoản & Tiết kiệm Trực tuyến (`Hình 4-2`, `Hình 4-3`, `Hình 4-4`)

*(Đăng nhập tài khoản chính **Nguyễn Phan Nhân** — `SEED_MEMBER_1_EMAIL` trên Máy 1)*

- [ ] **2.1 Kiểm tra Trang chủ & Biểu đồ Dòng tiền Tháng:**
  - Quan sát Trang chủ (`CustomerHomeFragment`): Bấm biểu tượng **Con mắt** để ẩn (`******** ₫`) rồi bấm lại để hiện tổng số dư.
  - Kiểm tra biểu đồ cột `MPAndroidChart` hiển thị cột **Tổng thu (`52.000.000 ₫`)** và **Tổng chi (`2.000.000 ₫`)** trong tháng.
  - **📸 [CHỤP ẢNH BÁO CÁO — Hình 4-2]:** Chụp trọn vẹn màn hình Trang chủ Khách hàng (hiện rõ số dư, chuông thông báo, danh sách thẻ tài khoản, lưới phím tắt và biểu đồ Thu/Chi).
- [ ] **2.2 Kiểm tra Bộ định tuyến `AccountRouterFragment` & Chi tiết 3 loại tài khoản:**
  - Chạm vào thẻ **Tài khoản Thanh toán (`1001000001`)** → Kiểm tra hiển thị số dư `50.000.000 ₫`, hạn mức ngày/lần và hình **Mã QR nhận tiền cá nhân** sắc nét.
  - Quay lại, chạm vào thẻ **Tài khoản Tiết kiệm (`2001000001`)** → Kiểm tra gốc `100.000.000 ₫`, kỳ hạn `12 tháng`, lãi suất `6,0%/năm` và **Tiền lãi dự kiến hàng tháng = `500.000 ₫`** (`floor(100.000.000 * 6.0 / 1200)`).
  - Quay lại, chạm vào thẻ **Khoản vay Thế chấp (`3001000001`)** → Kiểm tra dư nợ `350.000.000 ₫`, lãi vay `8,5%/năm`, chu kỳ trả hàng tháng `6.200.000 ₫` và ngày đến hạn tiếp theo.
  - **📸 [CHỤP ẢNH BÁO CÁO — Hình 4-3]:** Chụp đủ 3 màn hình chi tiết `Checking`, `Saving`, `Mortgage` để ghép bộ 3 ảnh ngang cho *Hình 4-3*.
- [ ] **2.3 Kiểm tra Thiết lập Smart PIN, Mở mới & Tất toán Sổ tiết kiệm trực tuyến (`TC-10`):**
  - Vào **Hồ sơ → Cài đặt bảo mật (`SecuritySettingsFragment`)**, thiết lập **Smart PIN giao dịch 6 số** lần đầu là `123456`.
  - Quay về Trang chủ / Tiết kiệm → chọn **Mở sổ tiết kiệm trực tuyến (`OpenSavingFragment`)**:
    - Chọn nguồn tiền `1001000001`, nhập số tiền gốc **`5.000.000 ₫`**, chọn kỳ hạn **`6 tháng` (`5,2%/năm`)** → Kiểm tra ngày đáo hạn và tiền lãi cuối kỳ tự động tính toán trên màn hình.
    - **📸 [CHỤP ẢNH BÁO CÁO — Hình 4-4 (nửa trái)]:** Chụp màn hình Mở sổ tiết kiệm trực tuyến khi đã điền đủ thông tin dự phóng lãi.
    - Bấm **Mở sổ tiết kiệm** → Nhập Smart PIN `123456` → Kiểm tra tài khoản `CHECKING` giảm xuống `45.000.000 ₫` và xuất hiện sổ tiết kiệm mới `5.000.000 ₫` ở trạng thái `ACTIVE`.
  - Chạm mở chính sổ tiết kiệm `5.000.000 ₫` vừa tạo → bấm **Tất toán sổ tiết kiệm**:
    - Kiểm tra hộp thoại dự toán (`dialog_settle_saving`) báo rõ đây là **Tất toán trước hạn** (áp dụng lãi không kỳ hạn `0,5%/năm` theo số ngày thực gửi).
    - **📸 [CHỤP ẢNH BÁO CÁO — Hình 4-4 (nửa phải)]:** Chụp hộp thoại xác nhận tất toán sổ tiết kiệm kèm ô nhập Smart PIN.
    - Nhập Smart PIN `123456` để xác nhận tất toán → Kiểm tra tiền gốc hoàn về tài khoản `CHECKING` (`50.000.000 ₫`), sổ chuyển sang `CLOSED` và nhận thông báo `SAVING`.

---

### GIAI ĐOẠN 3: Chuyển tiền, QR, Bảo mật 3 lớp `> 5M` & Cổng Stripe (`Hình 4-5` → `Hình 4-9`)

- [ ] **3.1 Kiểm tra Chặn tự chuyển cho chính mình (`TC-04`) & Tra cứu người nhận nội bộ (`TC-02`):**
  - Mở **Chuyển tiền (`TransferFragment`)**:
    - Thử nhập số tài khoản nhận `1001000001` (trùng tài khoản nguồn) → Kiểm tra hệ thống báo lỗi không cho phép chuyển cùng tài khoản (`TC-04`).
    - Đổi sang nhập số tài khoản **`1001000002`** → Kiểm tra ngay khi nhập xong, hệ thống tự gọi `/accounts/lookup` và hiển thị tên chủ tài khoản **Nguyễn Thành Đạt** (`TC-02`).
    - Mở thử màn hình **Quét mã QR (`QrScannerFragment`)** từ nút biểu tượng QR để kiểm tra camera sau & thử quét mã QR cá nhân của Máy 2 (tự động điền số TK `1001000002`).
  - **📸 [CHỤP ẢNH BÁO CÁO — Hình 4-5]:** Chụp (1) Màn hình Chuyển tiền đã tự hiện tên *Nguyễn Thành Đạt* và (2) Màn hình Quét mã QR CameraX.
- [ ] **3.2 Kiểm tra Chuyển tiền nội bộ `<= 5.000.000 ₫`, OTP Email thật, Thông báo 2 chiều & Biên lai (`TC-03`):**
  -*(Mẹo: Đăng nhập sẵn tài khoản **Nguyễn Thành Đạt** trên Máy 2 để thấy thông báo nổi song song cả 2 máy).*
  - Trên Máy 1 (**Nguyễn Phan Nhân**), nhập chuyển **`500.000 ₫`** tới `1001000002`, nội dung *"Chuyển tiền ăn trưa nhóm NovaPay"* → Bấm **Tiếp tục**.
  - Vì `500.000 ₫ <= 5.000.000 ₫`, ứng dụng mở thẳng hộp thoại **Xác thực OTP 2FA (`OtpVerificationDialogFragment`)** đếm ngược `180s` (không yêu cầu quét mặt).
  - Mở hộp thư **Gmail** của `SEED_MEMBER_1_EMAIL` lấy mã OTP 6 chữ số do Brevo vừa gửi.
  - **📸 [CHỤP ẢNH BÁO CÁO — Hình 4-7]:** Chụp (1) Hộp thoại nhập OTP đang đếm ngược 180s và (2) Email OTP thực tế từ NovaPay trong Gmail để ghép thành *Hình 4-7*.
  - Nhập đúng 6 số OTP → Bấm **Xác nhận**:
    - Kiểm tra chuyển sang màn hình **Biên lai điện tử (`ReceiptFragment`)** báo `THÀNH CÔNG`.
    - Kiểm tra **System Push Notification** bật nổi trên thanh trạng thái của cả Máy 1 (`-500.000 ₫`) và Máy 2 (`+500.000 ₫`).
  - **📸 [CHỤP ẢNH BÁO CÁO — Hình 4-8]:** Chụp màn hình Biên lai giao dịch thành công; bấm thử nút **Chia sẻ ảnh PNG** và **Xuất PDF** để kiểm chứng `FileProvider` hoạt động tốt.
- [ ] **3.3 Kiểm tra Giao dịch Giá trị cao `> 5.000.000 ₫` — Đủ 3 Lớp Bảo Mật (`TC-06`, `TC-07`, `TC-08`):**
  - Vào **Thanh toán Hóa đơn (`UtilityBillFragment`)**, chọn loại **Tiền điện**, nhập mã khách hàng **`PE0100009999`** → Bấm **Tra cứu** → Hiện hóa đơn EVN TP.HCM trị giá **`6.200.000 ₫`** (`UNPAID`).
  - Bấm **Thanh toán ngay**: Vì `6.200.000 ₫ > 5.000.000 ₫`, Server tạo bản nháp `PENDING_FACE` (**chưa gửi OTP**) và tự động bật **`FaceCheckDialogFragment`**:
    - **Bước thử tấn công giả mạo (`TC-06`):** Nhờ một bạn khác trong nhóm đưa khuôn mặt vào camera (hoặc che một phần khuôn mặt khác chủ TK) rồi bấm *Xác minh khuôn mặt* → Kiểm tra Gemini `2.5 Flash` trả về lỗi **`403 FACE_MISMATCH`** hiển thị cảnh báo màu đỏ rõ ràng và không gửi OTP!
    - **📸 [CHỤP ẢNH BÁO CÁO — Hình 4-6 (nửa phải)]:** Chụp ngay màn hình hộp thoại `FaceCheckDialogFragment` đang hiện cảnh báo đỏ từ chối khuôn mặt không khớp (`FACE_MISMATCH`).
    - **Bước xác thực chính chủ (`TC-07`):** Chủ tài khoản **Nguyễn Phan Nhân** nhìn thẳng vào camera, mở rõ 2 mắt → Bấm *Xác minh khuôn mặt* → Khớp (`confidence >= 0.80`)!
    - **Lớp 2 (Sinh trắc học thiết bị):** Hộp thoại vân tay/mã khóa máy bật lên → Quét vân tay (`adb emu finger touch 1` hoặc cảm biến thật) → Báo `Đã xác thực sinh trắc học`.
    - **Lớp 3 (OTP Email 180s):** Kiểm tra Gmail nhận mã OTP mới vừa phát hành sau khi qua `FaceCheck` → Nhập 6 chữ số OTP → Bấm **Xác nhận**!
    - Kiểm tra kết quả: Giao dịch `6.200.000 ₫` thành công, hóa đơn `PE0100009999` tự động chuyển sang trạng thái **`PAID` (Đã thanh toán)**!
  - *(Kiểm thử phụ `TC-08` nếu muốn kiểm chứng khóa OTP):* Khởi tạo thử 1 giao dịch nhỏ `100.000 ₫`, cố tình nhập sai OTP `000000` liên tiếp **5 lần** → Kiểm tra lần thứ 5 báo `TOO_MANY_ATTEMPTS` và hủy bản nháp.
- [ ] **3.4 Kiểm tra Nạp tiền Trực tuyến qua Cổng Quốc tế Stripe `PaymentSheet` (`TC-09`):**
  - Mở **Nạp / Rút tiền (`DepositWithdrawFragment`)**, chọn tab **Nạp trực tuyến (Stripe)**, nhập số tiền **`200.000 ₫`** → Bấm **Tiếp tục**.
  - Nhập mã OTP 6 số nhận từ Gmail (lưu ý: dù nạp `> 5M` thì giao dịch nạp `GATEWAY` vẫn được miễn `FaceCheck` đúng đặc tả).
  - Sau khi xác nhận OTP, giao diện **`Stripe PaymentSheet`** bật lên (lúc này kiểm tra nhanh số dư tài khoản vẫn **chưa cộng** vì đang ở `PENDING_PAYMENT`).
  - Nhập thẻ kiểm thử quốc tế: Số thẻ **`4242 4242 4242 4242`**, MM/YY **`12/30`**, CVC **`123`**, ZIP **`70000`**.
  - **📸 [CHỤP ẢNH BÁO CÁO — Hình 4-9]:** Chụp (1) Màn hình Nạp tiền và (2) Bảng thanh toán thẻ quốc tế `Stripe PaymentSheet` đang điền thẻ `4242...`.
  - Bấm **Pay `200.000 ₫`** → Ứng dụng gọi `/payments/finalize`, cộng đúng `+200.000 ₫` vào tài khoản `1001000001`, bắn thông báo biến động `IN` và mở Biên lai nạp tiền!

---

### GIAI ĐOẠN 4: Hệ sinh thái Tiện ích, Sao kê PDF, Thông báo, Bảo mật, Bản đồ & AI (`Hình 4-10` → `Hình 4-14`)

- [ ] **4.1 Kiểm tra Hóa đơn Nước, Nạp Điện thoại & Đặt Dịch vụ TMĐT (`Hình 4-10`):**
  - Mở **Thanh toán Hóa đơn**: Nhập mã **`PE0100001234`** (`850.000 ₫`) hoặc chuyển sang tab **Tiền nước** nhập **`NU0700005678`** (`240.000 ₫` — SAWACO Nhà Bè).
  - Mở **Nạp tiền Điện thoại (`PhoneTopupFragment`)**: Nhập SĐT `0902000001`, chọn nhà mạng **Viettel**, chọn mệnh giá **`100.000 ₫`** (có thể hoàn tất OTP để test giao dịch `TOPUP`).
  - Mở **Đặt vé & Dịch vụ (`ServiceBookingFragment`)**: Chuyển qua lại giữa 4 tab `Chuyến bay`, `Xem phim`, `Khách sạn`, `Mua sắm`; chọn thử *Vé xem phim CGV IMAX (`150.000 ₫`)* số lượng `2` vé (`300.000 ₫`), thanh toán xong chuyển qua tab **Đơn đã đặt** kiểm tra đơn hàng vừa sinh ra.
  - **📸 [CHỤP ẢNH BÁO CÁO — Hình 4-10]:** Chụp 3 màn hình *Hóa đơn Điện/Nước*, *Nạp tiền Điện thoại* và *Đặt vé Dịch vụ* để ghép thành *Hình 4-10*.
- [ ] **4.2 Kiểm tra Trả nợ Khoản vay Thế chấp (`MORTGAGE_PAYMENT`):**
  - Vào chi tiết **Khoản vay Thế chấp (`3001000001`)**, bấm **Thanh toán kỳ nợ (`6.200.000 ₫`)** → Xác thực 3 lớp → Kiểm tra `outstandingBalance` giảm từ `350.000.000 ₫` xuống `343.800.000 ₫` và `nextDueDate` tự động dời thêm `+1 tháng`!
- [ ] **4.3 Kiểm tra Lịch sử Giao dịch Đa Bộ lọc & Xuất Sao kê PDF (`TC-16`, `Hình 4-11`):**
  - Mở **Lịch sử giao dịch (`TransactionHistoryFragment`)**:
    - Bấm lọc theo Chip **`Tiền vào`**, **`Tiền ra`**, gõ từ khóa tìm kiếm (ví dụ *"ăn trưa"* hoặc *"EVN"*).
    - Chạm vào một dòng giao dịch bất kỳ để mở lại biên lai chi tiết của giao dịch đó.
    - Bấm nút **Xuất Sao kê PDF** → Mở tệp PDF vừa tạo kiểm tra bố cục bảng sao kê chuyên nghiệp.
  - **📸 [CHỤP ẢNH BÁO CÁO — Hình 4-11]:** Chụp (1) Màn hình Lịch sử giao dịch đang dùng bộ lọc và (2) Tệp Sao kê điện tử PDF mở trên máy.
- [ ] **4.4 Kiểm tra Trung tâm Thông báo & System Push Notification (`Hình 4-12`):**
  - Kéo thanh trạng thái Android xuống để xem thông báo biến động số dư của NovaPay, sau đó mở biểu tượng **Chuông** trên Trang chủ vào **Trung tâm Thông báo (`NotificationCenterFragment`)**.
  - Kiểm tra lọc theo danh mục (`Tất cả`, `Biến động số dư`, `Tiết kiệm`, `Bảo mật`, `eKYC`) và thử bấm **Đánh dấu đã đọc tất cả** (huy hiệu số đỏ trên chuông Trang chủ về `0`).
  - **📸 [CHỤP ẢNH BÁO CÁO — Hình 4-12]:** Chụp (1) Trung tâm Thông báo trong app và (2) Thanh thông báo hệ thống Android có tin biến động số dư.
- [ ] **4.5 Kiểm tra Hồ sơ cá nhân, Chỉnh Hạn mức & Khóa thẻ khẩn cấp `FROZEN` (`TC-11`, `Hình 4-13`):**
  - Mở **Hồ sơ cá nhân (`ProfileFragment`)**: Thử cập nhật địa chỉ mới → Bấm Lưu (kiểm chứng `firestore.rules` cho phép sửa đúng các trường liên hệ hợp lệ). Kiểm tra mục **Danh bạ thụ hưởng** (thêm/xóa người nhận).
  - Mở **Cài đặt Bảo mật (`SecuritySettingsFragment`)**:
    - **📸 [CHỤP ẢNH BÁO CÁO — Hình 4-13]:** Chụp (1) Màn hình Hồ sơ cá nhân và (2) Màn hình Cài đặt Bảo mật.
    - Thử điều chỉnh **Hạn mức mỗi giao dịch / Hạn mức ngày**, nhập Smart PIN `123456` để lưu.
    - Bật **Tạm khóa khẩn cấp tài khoản (`FROZEN`)** (nhập PIN `123456`) → Thử ra màn hình Chuyển tiền thực hiện giao dịch → Kiểm tra bị chặn ngay với lỗi tài khoản đang tạm khóa (`ACCOUNT_NOT_ACTIVE`)!
    - **Lưu ý:** Quay lại `SecuritySettingsFragment` nhập PIN `123456` **Mở khóa lại (`ACTIVE`)** ngay để tiếp tục các bước sau.
- [ ] **4.6 Kiểm tra Bản đồ Chi nhánh `osmdroid` & Fallback Quận 7 (`TC-14`, `Hình 4-14a`):**
  - Mở **Chi nhánh & ATM (`BranchMapFragment`)**:
    - Nếu chạy trên máy ảo mặc định ở Mỹ (`> 100 km` so với TP.HCM) hoặc tắt GPS quá 5 giây: Kiểm tra thông báo tự động định tâm về **Quận 7 (`10.7326, 106.6998` — ĐH Tôn Đức Thắng)**.
    - Kiểm tra thẻ chi nhánh gắn nhãn **Gần nhất** (*NovaBank PGD Nguyễn Hữu Thọ — Quận 7*) và chạm chọn 1 chi nhánh khác (ví dụ *Chi nhánh Phú Mỹ Hưng* hoặc *ATM Lotte Mart Q7*) để vẽ **tuyến đường đi bộ màu xanh** kèm quãng đường (km) và số phút đi bộ.
- [ ] **4.7 Kiểm tra Trợ lý ảo AI Chatbot & Điều hướng DeepLink (`TC-15`, `Hình 4-14b`):**
  - Mở **Trợ lý AI (`ChatbotFragment`)**, gửi tin nhắn:
    > *"Tháng này tôi thu chi bao nhiêu và số dư hiện tại còn bao nhiêu? Cho tôi nạp tiền điện thoại"*
  - Kiểm tra Bot trả lời Tiếng Việt chính xác theo số dư thực tế của tài khoản `1001000001` và hiển thị các nút hành động DeepLink bên dưới tin nhắn (ví dụ `[Nạp điện thoại]`, `[Lịch sử tháng]`).
  - **📸 [CHỤP ẢNH BÁO CÁO — Hình 4-14]:** Chụp (1) Màn hình Bản đồ Chi nhánh có đường đi bộ màu xanh và (2) Màn hình AI Chatbot có nút DeepLink để ghép thành *Hình 4-14*.
  - Bấm vào nút **`[Nạp điện thoại]`** trong khung chat → Kiểm tra ứng dụng điều hướng thẳng tới `PhoneTopupFragment`!

---

### GIAI ĐOẠN 5: Cổng Quản trị Nhân viên Ngân hàng (`OFFICER`) & Đồng bộ Thời gian thực 2 Thiết bị (`Hình 4-15`, `Hình 4-16`, `Hình 4-18`)

*(Trên Máy 2, đăng nhập tài khoản **Officer Trần Minh Quân** — `SEED_OFFICER_EMAIL` / `NovaPay@Officer2026`)*

- [ ] **5.1 Kiểm tra Phân quyền cứng Role Guard (`TC-01`) & Dashboard Kiểm toán (`Hình 4-15`):**
  - Xác nhận giao diện của `OFFICER` hoàn toàn tách biệt với `CUSTOMER` (không có thanh điều hướng chuyển tiền/tiết kiệm cá nhân). Nếu thử chạy lệnh `adb shell am start -W -a android.intent.action.VIEW -d "novapay://transfer" com.novabank.novapay`, `MainActivity` chặn ngay và giữ nguyên tại Dashboard của Officer (`TC-01`).
  - Quan sát **Dashboard Nhân viên (`OfficerDashboardFragment`)**: Kiểm tra các con số KPI tổng khách hàng, giao dịch hôm nay và cuộn xem danh sách **Nhật ký kiểm toán (`audit_logs`)**.
  - **📸 [CHỤP ẢNH BÁO CÁO — Hình 4-15]:** Chụp màn hình Dashboard của Nhân viên Ngân hàng kèm danh sách `audit_logs`.
- [ ] **5.2 Kiểm tra Mở tài khoản Khách hàng mới tại quầy & Quản lý Khách hàng (`Hình 4-16`):**
  - Vào **Quản lý khách hàng (`CustomerManagementFragment`)** → bấm nút **Tạo khách hàng mới (`CreateCustomerFragment`)**:
    - Nhập thông tin khách hàng mẫu mới (Họ tên, Email chưa tồn tại, SĐT `0909123456`, CCCD 12 số `079204009999`, Ngày sinh `2003-09-02`, Địa chỉ, Mật khẩu khởi tạo `NovaPay@2026`).
    - **📸 [CHỤP ẢNH BÁO CÁO — Hình 4-16]:** Chụp (1) Danh sách Quản lý khách hàng và (2) Biểu mẫu Tạo khách hàng mới tại quầy để ghép thành *Hình 4-16*.
    - Bấm **Tạo hồ sơ** → Kiểm tra khách hàng mới xuất hiện trong danh sách kèm tài khoản `CHECKING` 10 chữ số tự động khởi tạo và 1 bản ghi mới `CREATE_CUSTOMER` trong `audit_logs`.
- [ ] **5.3 Màn đối chiếu Song song 1 — Cập nhật Lãi suất Động Thời gian thực (`TC-12`, `Hình 4-18`):**
  - **Trên Máy 1 (`CUSTOMER` Nguyễn Phan Nhân):** Mở sẵn màn hình **Chi tiết Sổ tiết kiệm `2001000001`** (gốc `100.000.000 ₫`, kỳ hạn `12 tháng`, lãi hiện tại `6,0%/năm` → Lãi dự kiến hàng tháng đang hiển thị **`500.000 ₫`**).
  - **Trên Máy 2 (`OFFICER` Trần Minh Quân):** Mở **Quản lý Biểu lãi suất (`InterestRateManagementFragment`)**.
  - **📸 [CHỤP ẢNH BÁO CÁO — Hình 4-18]:** Chụp màn hình Quản lý Biểu lãi suất tiết kiệm của Officer.
  - Chọn kỳ hạn **`12 tháng`**, sửa lãi suất năm từ `6.0` thành **`6.6`** (`%/năm`) → bấm **Cập nhật lãi suất**:
    - Quan sát mắt thường trên **Máy 1** (không chạm tay vào Máy 1): Con số *Tiền lãi dự kiến hàng tháng* tự động nhảy tức thì từ **`500.000 ₫`** lên **`550.000 ₫`**!
  - *(Sau khi test xong có thể chỉnh lại về `6.0%` nếu muốn giữ nguyên số liệu gốc).*

---

### GIAI ĐOẠN 6: Màn Kết Thúc Đỉnh Cao — Cưỡng Bức Khóa Tài Khoản Thời Gian Thực (`TC-13`)

*(Luôn để bước này ở **cuối cùng** của buổi test hoặc cuối video demo để tạo hiệu ứng ấn tượng nhất!)*

- [ ] **6.1 Khóa khẩn cấp tài khoản khách hàng đang trực tuyến (`Realtime Force Logout`):**
  - **Máy 1 (`CUSTOMER` Nguyễn Phan Nhân):** Đang mở bình thường ở Trang chủ.
  - **Máy 2 (`OFFICER` Trần Minh Quân):** Vào **Quản lý khách hàng**, tìm dòng **Nguyễn Phan Nhân**, bấm nút **Khóa tài khoản (`LOCKED`)** → Xác nhận.
  - **Kết quả kỳ vọng tức thì (< 1 giây):**
    1. **Máy 1** lập tức bị cưỡng bức đăng xuất (`SessionManager.forceLogoutDueToLock()`), tự động văng về màn hình **Đăng nhập** kèm thông báo tài khoản đã bị khóa bởi ngân hàng!
    2. Thử nhập lại mật khẩu của `SEED_MEMBER_1_EMAIL` để đăng nhập lại → Bị từ chối hoàn toàn (do Firebase Auth đã đặt `disabled: true` và thu hồi Refresh Token).
    3. Trên **Máy 2**, vào Dashboard kiểm tra `audit_logs` đã ghi nhận hành động `UPDATE_CUSTOMER` (`status: "LOCKED"`).
- [ ] **6.2 Mở khóa khôi phục lại trạng thái sạch sau khi kết thúc test/quay:**
  - Trên **Máy 2 (`OFFICER`)**, bấm **Mở khóa (`ACTIVE`)** lại cho **Nguyễn Phan Nhân** → Xác nhận Máy 1 đăng nhập lại bình thường 100%!

---

## PHẦN 4. KỊCH BẢN BẤM MÁY QUAY VIDEO DEMO (TIMELINE 9 PHÚT + LỜI THOẠI GỢI Ý)

> **Bố cục khung hình quay video đẹp nhất (OBS Studio trên PC):**
> - Chia màn hình ngang `1920 × 1080`: **Bên trái** đặt cửa sổ Máy 1 (`CUSTOMER`), **Bên phải** đặt cửa sổ Máy 2 (`OFFICER` / `CUSTOMER 2`) hoặc trình duyệt mở sẵn hộp thư **Gmail** khi tới cảnh nhận OTP.

| Thời lượng | Phân cảnh & Thiết bị | Thao tác bấm trên màn hình | Lời thuyết minh gợi ý (*Voiceover*) |
| :---: | :--- | :--- | :--- |
| **00:00 – 01:30**<br>*(Mở đầu & Tài khoản)* | **Cảnh 1 — Tổng quan & 3 loại tài khoản**<br>*(Máy 1: Nguyễn Phan Nhân)* | • Đăng nhập nhanh bằng vân tay từ màn hình Login.<br>• Bấm ẩn/hiện tổng tài sản, chỉ vào biểu đồ cột Thu/Chi tháng.<br>• Chạm mở nhanh 3 tài khoản: `Checking` (hiện QR cá nhân), `Saving` (`100M`, lãi `500.000 ₫`/tháng) và `Mortgage` (`350M`). | *"Xin chào Thầy/Cô, nhóm em xin trình bày hệ thống Ngân hàng số NovaPay viết 100% bằng Java 17 theo chuẩn Clean Architecture + MVVM kết hợp Backend Node.js/TypeScript trên Render và Cloud Firestore. Sau khi đăng nhập bằng sinh trắc học bảo vệ qua Android Keystore, trang chủ hiển thị thời gian thực biểu đồ thu/chi tháng và định tuyến tự động sang 3 loại tài khoản: Thanh toán kèm QR cá nhân tự mã hóa chuẩn ISO 18004, Tiết kiệm và Vay thế chấp."* |
| **01:30 – 03:00**<br>*(Tiết kiệm & Bảo mật PIN)* | **Cảnh 2 — Mở & Tất toán Sổ tiết kiệm trực tuyến**<br>*(Máy 1)* | • Vào `Mở sổ tiết kiệm`, chọn kỳ hạn `6 tháng`, nhập `5.000.000 ₫`, nhập Smart PIN `123456` để mở sổ.<br>• Mở sổ vừa tạo, bấm `Tất toán` để cho xem bảng tính lãi trước hạn bằng số nguyên `long`. | *"Mọi giá trị tiền tệ trên NovaPay đều dùng kiểu số nguyên 64-bit `long` để triệt tiêu sai số dấu phẩy động. Khi mở hoặc tất toán sổ tiết kiệm trực tuyến, Backend kiểm tra mã băm Smart PIN HMAC-SHA256 chống lỗi tranh chấp TOCTOU ngay bên trong giao dịch nguyên tử Firestore Transaction."* |
| **03:00 – 05:30**<br>*(Chuyển tiền, 3 Lớp `> 5M` & Stripe)* | **Cảnh 3 — Giao dịch, FaceCheck AI & Stripe**<br>*(Máy 1 + Gmail + Máy 2)* | • Chuyển `500.000 ₫` sang `1001000002` (hiện tên *Nguyễn Thành Đạt*), nhập OTP Gmail → chỉ vào **System Push Notification** nổi trên cả 2 máy & xuất Biên lai PDF.<br>• Tra cứu hóa đơn điện `PE0100009999` (`6.200.000 ₫`) → Thử mặt người khác bị báo đỏ `403 FACE_MISMATCH` → Đưa đúng mặt chủ TK (`>= 0.80`) → Vân tay → OTP Gmail → Thành công.<br>• Nạp `200.000 ₫` qua thẻ `4242...` trên `Stripe PaymentSheet`. | *"Với giao dịch từ 5 triệu trở xuống, hệ thống xác thực OTP Email 180 giây qua Brevo và phát thông báo biến động 2 chiều ngay trong cùng Transaction. Đặc biệt với hóa đơn 6,2 triệu vượt ngưỡng 5 triệu, NovaPay kích hoạt bảo mật 3 lớp: kiểm tra thực thể sống ML Kit tại biên, đối chiếu khuôn mặt eKYC qua Gemini theo nguyên tắc Zero-False-Acceptance — từ chối ngay người lạ và chỉ phát hành OTP khi khớp chính chủ, kết hợp vân tay thiết bị. Ngoài ra hệ thống tích hợp cổng quốc tế Stripe PaymentSheet với cơ chế chốt thanh toán Idempotent chống cộng tiền hai lần."* |
| **05:30 – 07:15**<br>*(Sao kê, Bản đồ & AI Chatbot)* | **Cảnh 4 — Sao kê PDF, Khóa thẻ, Bản đồ & AI**<br>*(Máy 1)* | • Vào `Lịch sử giao dịch`, lọc `Tiền ra` và bấm `Xuất Sao kê PDF`.<br>• Mở `Chi nhánh & ATM`, chỉ nhãn *Gần nhất* tại Quận 7 và đường đi bộ màu xanh.<br>• Mở `Trợ lý AI`, hỏi thu/chi tháng & bấm nút `[Nạp điện thoại]` nhảy sang màn hình nạp thẻ. | *"Người dùng có thể lọc sao kê đa chiều và xuất tệp PDF trực tiếp. Phân hệ Bản đồ osmdroid tính khoảng cách cầu Haversine tìm chi nhánh gần nhất, vẽ lộ trình đi bộ qua OpenRouteService và tự động định tâm về Quận 7 nếu mất GPS. Trợ lý ảo AI nắm bắt đúng số dư thực tế của khách hàng và trả về các nút DeepLink đã kiểm duyệt Whitelist để điều hướng tức thì."* |
| **07:15 – 09:00**<br>*(Officer & Đồng bộ Realtime)* | **Cảnh 5 — Bank Officer, Lãi suất Động & Cưỡng bức Khóa TK**<br>*(Máy 1 & Máy 2 đặt song song)* | • Trên Máy 2 (`Officer`): Mở Dashboard & `audit_logs`, duyệt nhanh 1 hồ sơ eKYC `PENDING`.<br>• **Song song 1:** Máy 1 mở sổ 12T (`500.000 ₫`/tháng); Máy 2 đổi lãi 12T từ `6.0%` → `6.6%` → Máy 1 tự nhảy lên `550.000 ₫`/tháng.<br>• **Song song 2 (Kết thúc):** Máy 2 bấm **Khóa TK** Nguyễn Phan Nhân → Máy 1 tự văng ra màn hình Login ngay lập tức! | *"Cuối cùng là cổng quản trị Nhân viên Ngân hàng lưu vết kiểm toán bất biến `audit_logs`. Nhờ kiến trúc Hybrid Cloud kết hợp `SnapshotListener`, ngay khi Nhân viên điều chỉnh lãi suất kỳ hạn 12 tháng lên 6,6%, tiền lãi dự kiến trên máy khách hàng lập tức nhảy từ 500 nghìn lên 550 nghìn đồng. Và khi Nhân viên bấm Khóa khẩn cấp tài khoản nghi vấn, máy chủ thu hồi toàn bộ Refresh Token đồng thời ép thiết bị khách hàng đăng xuất tức thì trong chưa đầy 1 giây. Nhóm em xin cảm ơn Thầy/Cô đã theo dõi!"* |

---

## PHẦN 5. XỬ LÝ NHANH CÁC TÌNH HUỐNG THƯỜNG GẶP KHI TEST / QUAY VIDEO

1. **Báo lỗi `EKYC_FACE_NOT_FOUND` khi test giao dịch `> 5.000.000 ₫`:**
   - *Nguyên nhân:* Tài khoản đang giữ ảnh placeholder 1x1 mặc định từ `seed.ts`, chưa có ảnh khuôn mặt thật.
   - *Cách xử lý (30 giây):* Vào `Hồ sơ → Định danh eKYC` chụp mặt thật → Đăng nhập tài khoản Officer bấm **Phê duyệt** (lần sau chạy lại `npm run seed` ảnh thật này vẫn được giữ nguyên tự động, không cần chụp lại).
2. **Không thấy thông báo nổi (*Heads-up Push*) trên Android 13/14/15:**
   - *Cách xử lý:* Nhấn giữ biểu tượng app **NovaPay → App info → Notifications** → bật **Allow notifications** (và tắt chế độ *Do Not Disturb* trên thanh trạng thái máy ảo).
3. **Hóa đơn `PE0100009999` báo đã thanh toán (`PAID`) từ lần test nháp trước nên không bấm thanh toán lại được khi quay chính thức:**
   - *Cách xử lý:* Chạy `cd server ; npm run seed` trước khi bấm máy quay chính thức — toàn bộ hóa đơn `PE0100009999` (`6.200.000 ₫`) và `PE0100001234` (`850.000 ₫`) sẽ quay về trạng thái `UNPAID` mới tinh trong khi ảnh khuôn mặt eKYC thật của bạn vẫn được giữ nguyên!
4. **Không nhận được Email OTP trong hộp thư đến:**
   - *Cách xử lý:* Kiểm tra mục **Tất cả thư / Thư rác (Spam) / Quảng cáo** trong Gmail ở lần gửi đầu tiên và bấm *"Báo không phải thư rác"* để các email tiếp theo vào thẳng Hộp thư chính trong 1–2 giây.
