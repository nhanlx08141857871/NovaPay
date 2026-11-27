# NovaPay — Nền Tảng Ngân Hàng Số Toàn Diện (Mobile Banking)

[![Platform](https://img.shields.io/badge/Android-Java%2017%20%7C%20API%2026--35-0F172A?logo=android&logoColor=white)](app/build.gradle.kts)
[![Design](https://img.shields.io/badge/UI-Clean%20Fintech%20Minimalism%20%7C%20Material%203-059669)](GEMINI.md)
[![Backend](https://img.shields.io/badge/Cloud%20API-Node.js%2020%20%2B%20TypeScript-339933?logo=node.js&logoColor=white)](server/package.json)
[![Hosting](https://img.shields.io/badge/Deployed-Render%20HTTPS%20%7C%20%2Fhealth-46E3B7?logo=render&logoColor=black)](server/src/index.ts)
[![Payments](https://img.shields.io/badge/Payments-Stripe%20PaymentSheet-635BFF?logo=stripe&logoColor=white)](server/src/routes/payments.ts)
[![AI](https://img.shields.io/badge/AI%20Assistant-Google%20Gemini%202.5%20Flash-4285F4?logo=google&logoColor=white)](server/src/services/chatbot.ts)
[![Tests](https://img.shields.io/badge/Automated%20Tests-156%20Passing%20(95%20JUnit%20%2B%2061%20Server)-2563EB)](docs/report/05_TECH_STACK_AND_IMPLEMENTATION.md)

> **Mục lục nhanh:** [1. Tổng quan Sản phẩm](#1-tổng-quan-sản-phẩm-product-overview) • [2. Hệ sinh thái Tính năng](#2-hệ-sinh-thái-tính-năng-core-features) • [3. Bảo mật Đa lớp](#3-cơ-chế-bảo-mật-đa-lớp-multi-layer-security) • [4. Kiến trúc Hybrid Cloud](#4-kiến-trúc-hệ-thống--ngăn-xếp-công-nghệ-architecture--tech-stack) • [5. Trải nghiệm Nhanh & Tài khoản](#5-trải-nghiệm-nhanh--danh-sách-4-tài-khoản-chính-thức) • [6. Hướng dẫn Cài đặt Mã nguồn](#6-hướng-dẫn-cài-đặt--vận-hành-từ-mã-nguồn-developer-setup) • [7. Cấu trúc Thư mục & Báo cáo](#7-cấu-trúc-thư-mục--tài-liệu-kỹ-thuật) • [8. Xử lý Sự cố](#8-xử-lý-sự-cố-thường-gặp-troubleshooting) • [9. Đội ngũ Phát triển](#9-đội-ngũ-phát-triển-development-team)

---

## 1. Tổng Quan Sản Phẩm (Product Overview)

**NovaPay (`com.novabank.novapay`)** là nền tảng Ngân hàng số di động toàn diện, an toàn và hiện đại, hỗ trợ đồng thời 2 phân hệ người dùng chuyên biệt (**Khách hàng — `CUSTOMER`** và **Nhân viên Ngân hàng — `OFFICER`**). Ứng dụng được xây dựng theo triết lý lấy người dùng làm trung tâm với ngôn ngữ thiết kế **Clean Fintech Minimalism** trên nền tảng **Material Design 3** (hỗ trợ chế độ Sáng/Tối và hiển thị 100% bằng Tiếng Việt).

### Giá trị cốt lõi

- **Giao dịch & Thông báo biến động tức thì 24/7:** Mọi thao tác chuyển khoản, thanh toán hóa đơn, mở/tất toán tiết kiệm, cập nhật biểu lãi suất và thông báo biến động số dư hai chiều đều đồng bộ theo thời gian thực với độ chính xác số học tuyệt đối.
- **Bảo mật đa lớp chuẩn ngân hàng thương mại:** Kết hợp định danh khuôn mặt trực tuyến (eKYC), xác thực sinh trắc học thiết bị, đối chiếu khuôn mặt nghiêm ngặt (*Zero-False-Acceptance*) cho giao dịch giá trị cao, mã PIN giao dịch 6 số (`HMAC-SHA256`), mã OTP Email `SHA-256` và cơ chế khóa phiên từ xa tức thì.
- **Quản trị tài chính cá nhân khép kín:** Tích hợp trọn vẹn 3 dòng tài khoản (`Checking`, `Saving`, `Mortgage`), cổng nạp tiền quốc tế **Stripe**, sao kê điện tử PDF, bản đồ chỉ đường chi nhánh/ATM và hệ sinh thái thanh toán tiện ích – dịch vụ số.
- **Trợ lý tài chính AI hỗ trợ 24/7:** Tích hợp **Google Gemini 2.5 Flash** am hiểu ngữ cảnh số dư và dòng tiền cá nhân, phản hồi bằng tiếng Việt tự nhiên kèm nút điều hướng DeepLink mở thẳng màn hình chức năng chỉ với một chạm.

> **Tuyên bố Học thuật (Academic Sandbox Simulation — TDTU):** Dự án **NovaPay** được nghiên cứu và phát triển phục vụ học phần **Phát triển Ứng dụng Di động (Mobile Apps Development)** tại **Khoa Công nghệ Thông tin — Trường Đại học Tôn Đức Thắng (TDTU)**. Mọi số dư, thẻ kiểm thử và giao dịch trong hệ thống hoạt động trên môi trường kiểm thử an toàn, không phát sinh lưu ký tiền tệ thực tế.

---

## 2. Hệ Sinh Thái Tính Năng (Core Features)

### 2.1 Phân hệ Khách hàng (`Customer Banking`)

- **Quản lý đa tài khoản (`Checking`, `Saving`, `Mortgage`):**
  - **Trang chủ tổng quan (`CustomerHomeFragment`):** Hợp nhất danh mục tài sản (có nút ẩn/hiện số dư), biểu đồ cột thu/chi tháng trực quan (`MPAndroidChart`), huy hiệu chuông thông báo chưa đọc và thanh điều hướng 4 tab (*Trang chủ*, *Chuyển tiền*, *Lịch sử*, *Cá nhân*).
  - **Tài khoản Thanh toán (`CheckingDetailFragment`):** Theo dõi số dư tức thì, hạn mức giao dịch, mã QR nhận tiền cá nhân và lối tắt chuyển khoản, nạp/rút, sao kê.
  - **Tài khoản Tiết kiệm & Mở/Tất toán trực tuyến (`SavingDetailFragment`, `OpenSavingFragment`):** Mở sổ tiết kiệm trực tuyến trích tiền từ `CHECKING` theo biểu lãi suất kỳ hạn hiện hành; theo dõi tiền lãi dự kiến hàng tháng cập nhật thời gian thực và thực hiện **tất toán trước hạn hoặc đúng hạn** kèm bảng dự toán gốc + lãi minh bạch.
  - **Khoản vay Thế chấp (`MortgageDetailFragment`):** Theo dõi dư nợ gốc còn lại, chu kỳ thanh toán (`MONTHLY` hoặc `BIWEEKLY`), ngày đến hạn tiếp theo và thanh toán kỳ hạn trực tiếp từ tài khoản `CHECKING`.
- **Chuyển tiền nội bộ & liên ngân hàng 24/7 (`TransferFragment`):**
  - Tự động tra cứu và hiển thị tên chủ tài khoản nhận nội bộ NovaPay qua API bảo mật `GET /accounts/lookup` (không làm lộ số dư hay CCCD), quản lý danh bạ người thụ hưởng thường xuyên và xuất/chia sẻ biên lai điện tử định dạng **PNG & PDF** (`ReceiptFragment`).
- **Thanh toán & Tạo mã QR cá nhân (`QrBitmapEncoder` & `QrScannerFragment`):**
  - Tự động sinh mã QR nhận tiền chuẩn quốc tế **ISO/IEC 18004** (viết thuần Java 17) ngay trên chi tiết tài khoản thanh toán, kết hợp trình quét QR tốc độ cao qua **CameraX + Google ML Kit Barcode Scanning**.
- **Thông báo biến động số dư thời gian thực & System Push (`NotificationCenterFragment`):**
  - Ghi nhận thông báo biến động hai chiều (`OUT` cho người chuyển, `IN` cho người nhận), tiết kiệm, bảo mật và kết quả duyệt eKYC ngay trong giao dịch nguyên tử; tự động đẩy **System Push Notification (`IMPORTANCE_HIGH`)** ra thanh trạng thái Android (`BalanceAlertSubscriber` + `SystemNotificationHelper`) và mở thẳng biên lai giao dịch khi chạm vào.
- **Lịch sử giao dịch đa bộ lọc & Xuất Sao kê PDF (`TransactionHistoryFragment`, `StatementPdfExporter`):**
  - Tra cứu sổ cái theo khoảng thời gian, lọc theo chiều Tiền vào (`IN`) / Tiền ra (`OUT`), tìm kiếm từ khóa tức thì (`LedgerFilterHelper`), chạm xem lại biên lai gốc và xuất tệp **Sao kê tài khoản PDF** chia sẻ qua `FileProvider`.
- **Trung tâm Bảo mật, Smart PIN & Kiểm soát Tài khoản (`SecuritySettingsFragment`):**
  - Hỗ trợ **Đăng nhập nhanh sinh trắc học** (`BiometricQuickLoginStore` mã hóa `AES/GCM/NoPadding` trong `AndroidKeyStore`), **Đổi mật khẩu / Quên mật khẩu**, thiết lập & đổi **mã PIN giao dịch 6 số**, điều chỉnh hạn mức giao dịch mỗi lần / mỗi ngày và nút **Tạm khóa khẩn cấp (`FROZEN`)** tài khoản thanh toán.
- **Nạp tiền trực tuyến qua cổng quốc tế Stripe (`DepositWithdrawFragment`):**
  - Tích hợp **Stripe `PaymentSheet`** với quy trình xác nhận 2 pha chống cộng tiền đúp (`/transactions/confirm` → `/payments/finalize`), đồng thời hỗ trợ rút tiền linh hoạt.
- **Hệ sinh thái Thanh toán Hóa đơn & Dịch vụ số:**
  - Tra cứu & gạch nợ tức thì hóa đơn **Điện / Nước** (`UtilityBillFragment`), **nạp tiền điện thoại** đa nhà mạng (`PhoneTopupFragment`), đặt **vé máy bay, vé xem phim, phòng khách sạn** và mua sắm trực tuyến (`ServiceBookingFragment`) kèm tab quản lý đơn hàng đã đặt.
- **Bản đồ Chi nhánh & Chỉ đường đi bộ thông minh (`BranchMapFragment`):**
  - Tích hợp bản đồ **OpenStreetMap (`osmdroid`)**, định vị GPS người dùng, tính khoảng cách cầu Haversine đề xuất chi nhánh/ATM **Gần nhất** và vẽ lộ trình đi bộ tối ưu qua **OpenRouteService API**.
- **Trợ lý Tài chính AI (`ChatbotFragment`):**
  - Trợ lý **Google Gemini 2.5 Flash** phân tích ngữ cảnh tài chính thực tế của khách hàng, giải đáp bằng tiếng Việt và đính kèm các nút hành động DeepLink (`novapay://...`) đã kiểm duyệt Whitelist để mở ngay màn hình dịch vụ tương ứng.

### 2.2 Phân hệ Quản trị Ngân hàng (`Bank Officer Portal`)

- **Bảng điều khiển giám sát vận hành (`OfficerDashboardFragment`):** Thống kê trực quan tổng số khách hàng, giao dịch trong ngày, hàng đợi hồ sơ eKYC chờ duyệt, danh sách hậu kiểm FaceCheck và nhật ký hệ thống.
- **Quản lý & Khởi tạo hồ sơ khách hàng (`CustomerManagementFragment`, `CreateCustomerFragment`):** Mở hồ sơ khách hàng mới kèm tài khoản thanh toán mặc định, cập nhật thông tin định danh và khóa/mở khóa quyền truy cập (`ACTIVE` / `LOCKED`) đồng bộ thu hồi phiên tức thì.
- **Thẩm định & Chụp định danh eKYC tại quầy (`OfficerEkycReviewFragment`):** Đối chiếu ảnh chân dung định danh để phê duyệt (`VERIFIED`) hoặc từ chối (`REJECTED`) kèm thông báo tự động tới khách hàng, hậu kiểm giao dịch giá trị lớn và hỗ trợ chụp hộ eKYC trực tiếp tại quầy (`novapay://ekyc?targetUid=...`).
- **Quản lý biểu lãi suất & Nhật ký kiểm toán (`InterestRateManagementFragment`, `audit_logs`):** Điều chỉnh lãi suất tiết kiệm theo từng kỳ hạn áp dụng tức thời trên toàn hệ thống; mọi thao tác của nhân viên ngân hàng đều được ghi vết bất biến vào `audit_logs`.

---

## 3. Cơ Chế Bảo Mật Đa Lớp (Multi-Layer Security)

- **Định danh khuôn mặt trực tuyến (eKYC) với CameraX & ML Kit:**
  - Phân tích thực thể sống (**On-device Liveness**) ngay trên thiết bị qua **CameraX** và **Google ML Kit Face Detection** (bắt buộc duy nhất 1 khuôn mặt trong khung, 2 mắt mở `>= 0.7f`, góc nhìn thẳng `|EulerY/Z| <= 15°`), nén JPEG `< 200KB` trước khi gửi lưu trữ bảo mật trên Server. Tài khoản chưa đạt `VERIFIED` bị chặn toàn bộ giao dịch chi/rút (`EKYC_REQUIRED`).
- **Xác thực thích ứng theo giá trị giao dịch (`Adaptive MFA`):**
  - **Giao dịch tiêu chuẩn (`<= 5.000.000 ₫`) & Nạp tiền Stripe:** Xác thực bằng **mã OTP 6 chữ số** sinh ngẫu nhiên mật mã học (`crypto.randomInt`), gửi qua **Brevo HTTPS REST API** (hiệu lực `180 giây`, băm **`SHA-256`** kèm salt `draftId`, so sánh hằng thời gian `timingSafeEqual`, khóa sau 5 lần nhập sai).
  - **Giao dịch chi/rút giá trị cao (`> 5.000.000 ₫`):** Bắt buộc vượt qua **3 lớp bảo vệ liên hoàn** theo đúng thứ tự máy trạng thái:
    1. **`FaceCheck` chuẩn *Zero-False-Acceptance*:** Chặn ngay `403 EKYC_FACE_NOT_FOUND` nếu hồ sơ chưa có ảnh khuôn mặt thật; gửi đối chiếu ảnh gốc eKYC và ảnh trực tiếp trên **Gemini Multimodal** (`temperature: 0.0`, chỉ chấp nhận khi `matched == true && confidence >= 0.80`; sai mặt cảnh báo đỏ `403 FACE_MISMATCH` tối đa 5 lần; quá tải/mất kết nối AI áp dụng **Fail-Closed `503 SERVICE_UNAVAILABLE`** — tuyệt đối không tự động cho qua). Chỉ sau khi khớp khuôn mặt, Server mới phát hành Email OTP.
    2. **Sinh trắc học thiết bị (`BiometricPrompt`):** Xác thực vân tay, khuôn mặt hệ thống hoặc mã khóa màn hình thiết bị (`BIOMETRIC_STRONG | DEVICE_CREDENTIAL`).
    3. **Mã OTP Email 2FA:** Xác nhận bước cuối trước khi thực thi hạch toán.
- **Smart PIN 6 số (`HMAC-SHA256`) & Khóa Phiên Tức Thì:**
  - Mã PIN giao dịch dùng cho Mở/Tất toán tiết kiệm và Mở khóa/Nâng hạn mức tài khoản được băm `HMAC-SHA256` lưu tách biệt trong `user_secrets/{uid}` (cấm tuyệt đối Client đọc/ghi, khóa 15 phút nếu sai 5 lần và kiểm tra chống TOCTOU ngay trong Transaction).
  - Khi Officer khóa tài khoản (`LOCKED`), Backend thu hồi toàn bộ Refresh Token (`revokeRefreshTokens`) đồng thời `SessionManager` trên Android tự động đăng xuất về màn hình đăng nhập trong dưới 1 giây.
- **Giao dịch nguyên tử ACID & Quy tắc `firestore.rules`:**
  - Toàn bộ thao tác biến đổi số dư, sổ cái `ledger`, hóa đơn, đơn hàng, tiết kiệm và thông báo đều thực thi trong **2-Phase Firestore Transaction (ACID)** bằng Firebase Admin SDK; kết hợp **Rate Limiter** theo từng endpoint và **`firestore.rules`** chặn 100% thao tác ghi tài chính trái phép từ Client.

---

## 4. Kiến Trúc Hệ Thống & Ngăn Xếp Công Nghệ (Architecture & Tech Stack)

NovaPay vận hành trên kiến trúc **Hybrid Cloud** kết hợp **Clean Architecture + MVVM**, tách bạch giữa kênh đồng bộ giao diện thời gian thực và lõi hạch toán tài chính không tin cậy thiết bị đầu cuối (*Zero-Trust Client*):

```text
┌───────────────────────────────────────────────────────────────────────────┐
│               ANDROID NATIVE CLIENT (100% Java 17 | 350 Classes)          │
│  Presentation : MVVM + Material 3 + ViewBinding + Navigation (novapay://) │
│  Domain       : Pure Java 17 Models (long VND, Instant), Repos, UseCases  │
│  Data         : Firestore SnapshotListeners + Retrofit 2 + Hilt Modules   │
│  Edge & Sec   : CameraX, ML Kit, BiometricPrompt, Android Keystore AES    │
└──────────────┬─────────────────────────────────────────────┬──────────────┘
               │ Kênh 1: Đọc dữ liệu thời gian thực          │ Kênh 2: Giao dịch & Quản trị
               │ (Kiểm soát bởi firestore.rules)             │ (Bearer Firebase ID Token)
               ▼                                             ▼
┌────────────────────────────────┐        ┌─────────────────────────────────┐
│   FIREBASE AUTH & FIRESTORE    │        │  BACKEND API (Node.js 20 + TS)  │
│  - Custom Claims (role)        │◄──────►│  Triển khai: Render (HTTPS)     │
│  - 15 Collections + 3 Subcolls │ Admin  │  ACID runTransaction + Limiter  │
│  - 5 Composite Indexes         │  SDK   │  Health Check: GET /health      │
└────────────────────────────────┘        └─────┬──────────┬──────────┬─────┘
                                                │          │          │
                                                ▼          ▼          ▼
                                             Stripe      Brevo     Gemini
                                           (Payments)   (2FA OTP)  (AI/eKYC)
```

### Tóm tắt công nghệ cốt lõi (Xem chi tiết từng phiên bản tại [`05_TECH_STACK_AND_IMPLEMENTATION.md`](docs/report/05_TECH_STACK_AND_IMPLEMENTATION.md))

- **Android Native (`app/`):** **100% Java 17** (`minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`, AGP `9.5.1`, Gradle `9.6.0`), **XML Layouts + ViewBinding**, **Material Design 3 (`1.12.0`)**, **Dagger Hilt (`2.60.1`)** tách 2 Qualifiers `@BackendRetrofit` và `@OpenRouteRetrofit`, **Jetpack Navigation (`2.8.9`)** (Single-Activity + 3 sub-graphs + DeepLinks `novapay://`), **Retrofit 2 & Gson (`2.11.0`)**, **OkHttp (`4.12.0`)**, **RxJava 3 (`3.1.10`) & RxAndroid (`3.0.2`)**, **Firebase BOM (`33.16.0`)**, **CameraX (`1.4.2`)**, **ML Kit Face Detection (`16.1.7`) & Barcode Scanning (`17.3.0`)**, **AndroidX Biometric (`1.1.0`)**, **Stripe Android SDK (`21.17.0`)**, **osmdroid (`6.1.20`)**, **MPAndroidChart (`v3.1.0`)**. Toàn bộ tiền tệ dùng kiểu số nguyên 64-bit **`long` (VNĐ)**.
- **Backend Server (`server/`):** **Node.js `>= 20.0.0`**, **TypeScript (`^5.7.3`)**, **Express (`^4.21.2`)**, **Helmet (`^8.0.0`)**, **CORS (`^2.8.5`)**, **Firebase Admin SDK (`^13.1.0`)**, **Stripe Node SDK (`^17.7.0`)**, **Brevo Transactional HTTPS API** (cổng `443`), **Google Gemini `2.5 Flash` REST API** và **`node:crypto`** (`SHA-256`, `HMAC-SHA256`, `timingSafeEqual`). Mọi khóa bí mật chỉ lưu trên biến môi trường máy chủ, tuyệt đối không nhúng vào APK.
- **Triển khai Đám mây & Kiểm thử:** Backend vận hành trực tuyến qua **HTTPS trên Render** (kèm endpoint **`GET /health`** chống ngủ đông); hệ thống đạt **156/156 bài kiểm thử tự động xanh sạch** (**`95`** Android JUnit 4 tests + **`61`** Backend `node:test` integration/unit tests).

---

## 5. Trải Nghiệm Nhanh & Danh Sách 4 Tài Khoản Chính Thức

### 5.1 Cài đặt trực tiếp APK trên điện thoại Android thật (Không cần dựng Server cục bộ)

Phiên bản APK của **NovaPay** đã được cấu hình kết nối trực tiếp tới **Backend Render (HTTPS)** qua biến `SERVER_BASE_URL` trong [`app/build.gradle.kts`](app/build.gradle.kts), cho phép cài đặt và trải nghiệm ngay toàn bộ tính năng trên điện thoại Android thật:

1. Tải tệp **`app-debug.apk`** *(hoặc **`novapay-release.apk`**)* về điện thoại Android (Android 8.0 / API 26 trở lên).
2. Mở tệp `.apk` để cài đặt (cấp quyền *"Cài đặt ứng dụng không xác định"* nếu trình quản lý tệp yêu cầu).
3. Mở **NovaPay**, cho phép quyền **Thông báo**, **Camera** (cho eKYC, FaceCheck, Quét QR) và **Vị trí** (cho Bản đồ chi nhánh), sau đó đăng nhập bằng một trong 4 tài khoản dưới đây.

### 5.2 Danh sách 4 tài khoản Gmail thật nhận OTP (Đã loại bỏ hoàn toàn email ảo)

Hệ thống chỉ sử dụng **4 tài khoản gắn với hộp thư Gmail thật** của nhóm (1 Quản trị viên `OFFICER` và 3 Khách hàng `CUSTOMER`) được nạp từ tệp cấu hình cục bộ `server/.env` khi chạy `npm run seed` để nhận mã OTP 6 số thực tế qua Brevo (xem chi tiết tại [`docs/TEST_ACCOUNTS.md`](docs/TEST_ACCOUNTS.md)):

| Vai trò & Trạng thái | Họ và tên | Biến Email trong `server/.env` | Mật khẩu | Danh mục Tài khoản & Số dư Khởi tạo |
| :--- | :--- | :--- | :--- | :--- |
| **`OFFICER`** (`VERIFIED`) | **Trần Minh Quân** | `SEED_OFFICER_EMAIL` | `NovaPay@Officer2026` | Quản trị viên: Dashboard KPI, tạo/sửa/khóa KH, chụp hộ & duyệt eKYC, đổi lãi suất, xem `audit_logs` |
| **`CUSTOMER`** (`VERIFIED`) | **Nguyễn Phan Nhân** | `SEED_MEMBER_1_EMAIL` | `NovaPay@2026` | • Checking **`1001000001`**: `50.000.000 ₫`<br>• Saving **`2001000001`**: `100.000.000 ₫` (12T, 6,0%/năm)<br>• Mortgage **`3001000001`**: dư nợ `350.000.000 ₫` (`MONTHLY`) |
| **`CUSTOMER`** (`VERIFIED`) | **Nguyễn Thành Đạt** | `SEED_MEMBER_2_EMAIL` | `NovaPay@2026` | • Checking **`1001000002`**: `50.000.000 ₫`<br>• Saving **`2001000002`**: `80.000.000 ₫` (6T, 5,2%/năm)<br>• Mortgage **`3001000002`**: dư nợ `240.000.000 ₫` (`BIWEEKLY`) |
| **`CUSTOMER`** (`PENDING`) | **Nguyễn Văn Duy** | `SEED_MEMBER_3_EMAIL` | `NovaPay@2026` | • Checking **`1001000003`**: `50.000.000 ₫`<br>• Saving **`2001000003`**: `120.000.000 ₫` (12T, 6,0%/năm)<br>• Mortgage **`3001000003`**: dư nợ `300.000.000 ₫` — *Chờ Officer duyệt eKYC* |

### 5.3 Mã dữ liệu tra cứu nhanh khi kiểm thử giao dịch

- **Số tài khoản nhận nội bộ NovaPay:** `1001000001` (*Nguyễn Phan Nhân*) • `1001000002` (*Nguyễn Thành Đạt*) • `1001000003` (*Nguyễn Văn Duy*).
- **Thẻ quốc tế kiểm thử cổng Stripe `PaymentSheet`:** Số thẻ `4242 4242 4242 4242` • Hết hạn tương lai bất kỳ (VD: `12/30`) • CVC `424` (hoặc `123`) • Mã ZIP `70000`.
- **Mã khách hàng Hóa đơn Điện (`EVN TP.HCM`):**
  - `PE0100001234` (*Nguyễn Phan Nhân* — **`850.000 ₫`**, kiểm thử xác thực OTP 2FA tiêu chuẩn `<= 5.000.000 ₫`).
  - `PE0100009999` (*Nguyễn Thành Đạt* — **`6.200.000 ₫`**, kích hoạt bảo mật 3 lớp `> 5.000.000 ₫`: **FaceCheck + Sinh trắc học + OTP**).
- **Mã khách hàng Hóa đơn Nước (`SAWACO Nhà Bè`):** `NU0700005678` (*Nguyễn Phan Nhân* — **`240.000 ₫`**, chưa thanh toán).

> **Lưu ý quan trọng trước khi thử giao dịch `> 5.000.000 ₫` (`FaceCheck`):** Hãy chụp ảnh khuôn mặt thật vào hồ sơ eKYC của tài khoản khách hàng (tự chụp ở màn hình eKYC hoặc đăng nhập **Officer** → **Quản lý khách hàng** → **Chụp hộ eKYC tại quầy**, sau đó bấm **Phê duyệt** trong mục **Duyệt eKYC & Hậu kiểm**) để Gemini đối chiếu chính xác gương mặt thật của bạn.

---

## 6. Hướng Dẫn Cài Đặt & Vận Hành Từ Mã Nguồn (Developer Setup)

### 6.1 Yêu cầu môi trường (Prerequisites)
1. **Android Studio** (phiên bản mới nhất, đi kèm sẵn **JetBrains Runtime JDK 17+** và **Android SDK Platform 35**).
2. **Node.js `>= 20.0.0`** và **npm** (kiểm tra bằng `node -v` và `npm -v`).
3. **Git** (khuyến nghị bật hỗ trợ đường dẫn dài trên Windows: `git config --global core.longpaths true`).
4. Các khóa dịch vụ đám mây miễn phí:
   - **Firebase Project** (gói Spark — bật *Authentication → Email/Password* và *Cloud Firestore*).
   - **Brevo API Key** (gửi Email OTP qua HTTPS, đã xác minh *Verified Sender Email* trên Brevo Dashboard).
   - **Stripe Test Keys** (`pk_test_...` và `sk_test_...` ở chế độ *Test Mode*).
   - **Google Gemini API Key** (tạo tại Google AI Studio).
   - **OpenRouteService API Key** (tạo miễn phí tại `openrouteservice.org`).

### 6.2 Các bước thiết lập chi tiết

#### Bước 1: Tải mã nguồn dự án
```bash
git clone <URL_REPO_CUA_BAN>
cd NovaPay
git config --global core.longpaths true
```

#### Bước 2: Thiết lập Firebase (`google-services.json` & Service Account)
1. Trên [Firebase Console](https://console.firebase.google.com/), bật **Authentication → Email/Password** và tạo **Firestore Database** (vùng `asia-southeast1`).
2. Vào **Project settings → General → Your apps → Add Android app**, đăng ký package name **`com.novabank.novapay`**, tải tệp **`google-services.json`** đặt vào:
   ```text
   NovaPay/app/google-services.json
   ```
3. Vào **Project settings → Service accounts → Firebase Admin SDK (Node.js)** → bấm **Generate new private key** để lấy thông tin cấu hình cho Backend (tuyệt đối không đưa tệp `.json` này vào Git).

#### Bước 3: Cấu hình biến môi trường Backend (`server/.env`)
Nhân bản tệp mẫu `server/.env.example` sang `server/.env` và điền đầy đủ thông số:
```powershell
Copy-Item server\.env.example server\.env
```
```env
PORT=3000

# 1. Firebase Admin SDK (lấy từ tệp Service Account .json)
FIREBASE_PROJECT_ID=novapay-71326
FIREBASE_CLIENT_EMAIL=firebase-adminsdk-xxxxx@novapay-71326.iam.gserviceaccount.com
FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n"

# 2. Dịch vụ gửi OTP Email qua HTTPS (Brevo API — dùng địa chỉ Sender đã Verified)
BREVO_API_KEY=xkeysib-...
BREVO_SENDER_EMAIL=your-verified-sender@gmail.com
BREVO_SENDER_NAME=NovaPay Banking

# 3. Cổng thanh toán Stripe (Test Mode Secret Key)
STRIPE_SECRET_KEY=sk_test_...

# 4. Google Gemini API (phục vụ FaceCheck Multimodal & AI Chatbot)
GEMINI_API_KEY=AIzaSy...
GEMINI_MODEL=gemini-2.5-flash

# 5. Bốn địa chỉ Gmail thật duy nhất cho 1 Officer & 3 Customer khi chạy npm run seed
SEED_OFFICER_EMAIL=gmail-quan-tri-officer@gmail.com
SEED_MEMBER_1_EMAIL=gmail-thanh-vien-1@gmail.com
SEED_MEMBER_2_EMAIL=gmail-thanh-vien-2@gmail.com
SEED_MEMBER_3_EMAIL=gmail-thanh-vien-3@gmail.com
```

#### Bước 4: Cấu hình khóa phía Android (`local.properties`)
Mở tệp `local.properties` ở thư mục gốc dự án và bổ sung 3 khóa cho phép phía Client (tuyệt đối không đặt các khóa bí mật của Server vào đây):
```properties
# Khóa chỉ đường đi bộ OpenRouteService
ORS_API_KEY=eyJvcmciOi...

# Khóa công khai Stripe Test Mode (chỉ dùng pk_test_...)
STRIPE_PUBLISHABLE_KEY=pk_test_...

# Địa chỉ Backend Server:
# - Kết nối tới Server Cloud Render: https://<ten-service-cua-ban>.onrender.com/
# - Chạy cục bộ trên Máy ảo Android (Emulator): http://10.0.2.2:3000/
# - Chạy cục bộ trên Điện thoại thật qua cáp USB (kèm lệnh `adb reverse tcp:3000 tcp:3000`): http://127.0.0.1:3000/
SERVER_BASE_URL=http://10.0.2.2:3000/
```

#### Bước 5: Cài đặt Backend, Kiểm thử, Deploy `firestore.rules` & Khởi tạo dữ liệu (`seed`)
```powershell
cd server

# 1. Cài đặt dependencies
npm install

# 2. Biên dịch TypeScript & chạy toàn bộ 61 bài kiểm thử tự động
npm run build
npm test

# 3. Đẩy bộ quy tắc bảo mật firestore.rules trực tiếp lên Cloud Firestore
npm run deploy:rules

# 4. Dọn tài khoản ảo cũ & khởi tạo 4 tài khoản Gmail thật cùng dữ liệu ngân hàng mẫu (Idempotent)
npm run seed
```
> *Mẹo vận hành bổ sung:* Có thể chạy `npm run cleanup:drafts` để dọn các bản nháp giao dịch quá 24 giờ, và chạy `firebase deploy --only firestore:indexes` tại thư mục gốc nếu khởi tạo dự án Firebase mới lần đầu từ [`firestore.indexes.json`](firestore.indexes.json).

### 6.3 Khởi chạy Hệ thống & Kiểm thử Android

- **Khởi động Backend Server (Terminal 1):**
  ```powershell
  cd server
  npm run dev
  # Hoặc chạy bản Production: npm run build ; npm start
  # Kiểm tra trạng thái sống: Invoke-RestMethod http://localhost:3000/health  ->  {"ok": true}
  ```
- **Biên dịch & Chạy Ứng dụng Android (Android Studio hoặc Terminal 2):**
  - **Trên Android Studio:** Mở thư mục gốc `NovaPay` → **Sync Project with Gradle Files** → Chọn thiết bị (API 26–35) → **Run `'app'` (`Shift + F10`)**.
  - **Trên Windows PowerShell:**
    ```powershell
    $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

    # Biên dịch APK Debug (xuất ra tại app/build/outputs/apk/debug/app-debug.apk)
    .\gradlew.bat assembleDebug

    # Chạy toàn bộ 95 bài kiểm thử đơn vị JUnit 4 trên JVM
    .\gradlew.bat testDebugUnitTest
    ```

---

## 7. Cấu Trúc Thư Mục & Tài Liệu Kỹ Thuật

```text
NovaPay/
├── app/                                # Ứng dụng Android Native (100% Java 17 — 350 tệp .java, 50 XML Layouts)
│   └── src/
│       ├── main/
│       │   ├── java/com/novabank/novapay/
│       │   │   ├── core/               # AppResult, UiState, SingleLiveEvent, Hilt DI, Biometric, Keystore,
│       │   │   │                       # SessionManager, BalanceAlertSubscriber, SystemNotificationHelper, PDF/QR Utils
│       │   │   ├── domain/             # Pure Java 17: Models (long VND, Instant), 17 Repository Interfaces, 50+ UseCases
│       │   │   ├── data/               # Retrofit APIs, Firestore DataSources, 38 DTOs, Mappers, 17 RepositoryImpls
│       │   │   └── presentation/       # 28 màn hình MVVM (Auth, eKYC/FaceCheck, Customer, Notifications, Security, Officer)
│       │   ├── res/                    # Material 3 Layouts, 3 Navigation Sub-graphs, Strings phân tách theo Module
│       │   └── keepRules/rules.keep    # Quy tắc bảo vệ lớp DTO & Navigation Guard khi tối ưu hóa R8
│       └── test/                       # 32 bộ kiểm thử đơn vị JUnit 4 (95 test cases)
├── server/                             # Backend Node.js 20+ + TypeScript + Express + Firebase Admin SDK
│   ├── src/
│   │   ├── middleware/                 # auth.ts (verifyIdToken + RBAC) & rateLimit.ts
│   │   ├── routes/                     # accounts, transactions, payments, savings, security, ekyc, officer, ai
│   │   └── services/                   # otp, pin, email (Brevo), faceCheck (Gemini), stripe, notifications, chatbot, officerAudit
│   ├── scripts/                        # seed.ts, deployRules.ts, cleanupDrafts.ts, runTests.ts
│   └── test/                           # 10 tệp kiểm thử tự động node:test + MockFirestore (61 test cases)
├── docs/
│   ├── TEST_ACCOUNTS.md                # Hướng dẫn 4 tài khoản Gmail thật & mã dữ liệu kiểm thử nhanh
│   ├── SPEC_CHOT.md                    # Đặc tả kiến trúc & quy tắc nghiệp vụ cốt lõi
│   ├── diagrams/                       # 8 tệp sơ đồ PlantUML cốt lõi ánh xạ 1-1 theo docs/MARKING.md (.puml)
│   │   ├── 01_system_architecture.puml # Kiến trúc tổng thể Hybrid Cloud & Clean Architecture
│   │   ├── 02_use_case.puml            # Use Case tổng quan 2 phân hệ Customer & Officer
│   │   ├── 03..05_class_*.puml         # 3 Sơ đồ lớp UML (Domain Model, Android Clean MVVM, Backend Server)
│   │   ├── 06_database_erd.puml        # Sơ đồ ERD Cloud Firestore (15 Collections + 3 Subcollections)
│   │   └── 07..08_seq_*.puml           # 2 Sơ đồ tuần tự trọng tâm (Bảo mật 3 lớp > 5M & Stripe Idempotent)
│   └── report/                         # Bộ Hồ sơ Thuyết minh Báo cáo Kỹ thuật (liên kết trực tiếp tới docs/diagrams/*.puml):
│       ├── BAO_CAO.md                              # Khung báo cáo tổng hợp theo mẫu Khoa CNTT TDTU
│       ├── 01_SYSTEM_ARCHITECTURE_AND_ERD.md       # Thuyết minh Hybrid Cloud, Ma trận firestore.rules & 5 Indexes
│       ├── 02_CLASS_DIAGRAMS.md                    # Thuyết minh 3 Sơ đồ lớp UML cô đọng toàn hệ thống
│       ├── 03_SEQUENCE_DIAGRAMS.md                 # Phân tích 2 luồng tuần tự trọng tâm & các luồng bổ trợ
│       ├── 04_DEFENSE_QA_AND_DEMO.md               # Bảng tự đánh giá, 13 câu hỏi phản biện & Kịch bản Demo 10 phút
│       └── 05_TECH_STACK_AND_IMPLEMENTATION.md     # Thống kê chi tiết thư viện, phiên bản & Ma trận lựa chọn công nghệ
├── GEMINI.md                           # Quy chuẩn kỹ thuật bắt buộc của dự án (Single Source of Truth)
├── firestore.rules                     # Quy tắc phân quyền bảo mật Cloud Firestore
└── firestore.indexes.json              # Cấu hình 5 Composite Indexes tối ưu truy vấn Firestore
```

---

## 8. Xử Lý Sự Cố Thường Gặp (Troubleshooting)

1. **Lỗi `jlink.exe does not exist` khi chạy `.\gradlew.bat` bằng Terminal ngoài:**
   - **Nguyên nhân:** Biến `JAVA_HOME` của Windows đang trỏ vào bản JRE rút gọn thay vì JDK đầy đủ.
   - **Khắc phục:** Chạy `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"` trong phiên PowerShell trước khi gọi Gradle.
2. **Không nhận được Email OTP khi thực hiện giao dịch:**
   - **Khắc phục:** Kiểm tra `BREVO_API_KEY` và đảm bảo địa chỉ `BREVO_SENDER_EMAIL` trong `server/.env` đã ở trạng thái **Verified Sender** trên Brevo Dashboard; đồng thời kiểm tra cả mục *Spam / Quảng cáo* trên hộp thư Gmail của tài khoản `SEED_MEMBER_*_EMAIL` đang đăng nhập.
3. **Giao dịch `> 5.000.000 ₫` báo lỗi `EKYC_FACE_NOT_FOUND` hoặc `FACE_MISMATCH`:**
   - **Nguyên nhân:** Theo chuẩn bảo mật *Zero-False-Acceptance*, Server từ chối mọi tài khoản chưa có ảnh chân dung thật (hoặc đang giữ ảnh mẫu khởi tạo ban đầu) và từ chối khi khuôn mặt trước camera không khớp với hồ sơ gốc (`confidence < 0.80`).
   - **Khắc phục:** Vào màn hình định danh eKYC để chụp ảnh khuôn mặt thật rõ nét, đủ sáng (hoặc nhờ tài khoản `OFFICER` chụp hộ tại quầy và bấm **Phê duyệt**).
4. **Bản đồ chi nhánh trên máy ảo hiển thị thông báo dùng vị trí mặc định Quận 7 (`10.7326, 106.6998`):**
   - **Giải thích:** Máy ảo Android mặc định đặt tọa độ GPS tại California (Mỹ), cách cụm chi nhánh TP.HCM `> 100 km`. Ứng dụng chủ động chuyển tâm bản đồ về khu vực **ĐH Tôn Đức Thắng — Quận 7 (`10.7326, 106.6998`)** để thuật toán tìm chi nhánh gần nhất và vẽ đường đi bộ OpenRouteService luôn hoạt động mượt mà.

---

## 9. Đội Ngũ Phát Triển (Development Team)

| Thành viên | Vai trò phụ trách | Phạm vi đóng góp chính |
| :--- | :--- | :--- |
| **Nguyễn Phan Nhân** | **Core, Security & Account Lead (`Module A`)** | Hệ thống xác thực & `SessionManager` khóa phiên realtime, `BiometricHelper` & đăng nhập nhanh `BiometricQuickLoginStore`, `MainActivity` & DeepLink Role Guard (`nav_graph.xml`, `nav_core.xml`), Hồ sơ cá nhân, Định danh eKYC & `FaceCheck` Liveness (`CameraX + ML Kit`), Trang chủ & Biểu đồ dòng tiền, Trung tâm Thông báo & `BalanceAlertSubscriber`, 3 loại Tài khoản (`Checking`, `Saving`, `Mortgage`), Mở/Tất toán tiết kiệm trực tuyến, Trung tâm Bảo mật PIN/Hạn mức, Lịch sử & Xuất sao kê PDF, Hilt Core/Network Modules, Backend `ekyc.ts`, `savings.ts`, `security.ts` & `firestore.rules`. |
| **Nguyễn Thành Đạt** | **Money, Payment & Security Hardening (`Module B`)** | Chuyển tiền nội bộ/liên ngân hàng (`nav_money.xml`), Quét & tạo mã QR, Biên lai điện tử PNG & PDF, Cổng nạp tiền quốc tế Stripe `PaymentSheet` 2 pha idempotent & rút tiền, Xác thực OTP 2FA, Thanh toán hóa đơn Điện/Nước, Nạp điện thoại, Đặt dịch vụ & quản lý đơn hàng, Backend `accounts.ts`, `transactions.ts`, `payments.ts`, các dịch vụ lõi `otp.ts`, `pin.ts`, `email.ts`, `stripe.ts`, `faceCheck.ts` (*Zero-False-Acceptance*), `notifications.ts`, Rate Limiter & bộ kiểm thử tự động Backend. |
| **Nguyễn Văn Duy** | **Officer, Map, AI & Design System (`Module C`)** | Toàn bộ Phân hệ Quản trị Ngân hàng Bank Officer (`nav_officer_map_ai.xml`: Dashboard KPI, Quản lý & Khởi tạo KH, Thẩm định & Chụp hộ eKYC, Quản lý biểu lãi suất realtime, Nhật ký kiểm toán `audit_logs`), Bản đồ chi nhánh `osmdroid` & chỉ đường đi bộ OpenRouteService, Trợ lý tài chính AI Gemini (`ChatbotFragment`), Design System Material 3 (*Clean Fintech Minimalism*), Backend `officer.ts`, `officerAudit.ts`, `ai.ts`, `chatbot.ts`, cấu hình Gradle & triển khai đám mây Render HTTPS. |
