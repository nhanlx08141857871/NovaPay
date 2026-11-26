# NovaPay — Nền Tảng Ngân Hàng Số Toàn Diện

[![Platform](https://img.shields.io/badge/Android-Java%2017%20%7C%20API%2026--35-0F172A?logo=android&logoColor=white)](app/build.gradle.kts)
[![Design](https://img.shields.io/badge/UI-Clean%20Fintech%20Minimalism%20%7C%20Material%203-059669)](GEMINI.md)
[![Backend](https://img.shields.io/badge/Cloud%20API-Node.js%2020%20%2B%20TypeScript-339933?logo=node.js&logoColor=white)](server/package.json)
[![Hosting](https://img.shields.io/badge/Deployed-Render%20HTTPS%20%7C%20%2Fhealth-46E3B7?logo=render&logoColor=black)](server/src/index.ts)
[![Payments](https://img.shields.io/badge/Payments-Stripe%20PaymentSheet-635BFF?logo=stripe&logoColor=white)](server/src/routes/payments.ts)
[![AI](https://img.shields.io/badge/AI%20Assistant-Google%20Gemini-4285F4?logo=google&logoColor=white)](server/src/services/chatbot.ts)

---

## 1. Tổng quan Sản phẩm (Product Overview)

**NovaPay (`com.novabank.novapay`)** là nền tảng Ngân hàng số di động toàn diện, an toàn và hiện đại, được xây dựng theo triết lý lấy người dùng làm trung tâm cùng ngôn ngữ thiết kế **Clean Fintech Minimalism** trên nền tảng **Material Design 3** (hỗ trợ giao diện Sáng/Tối và hiển thị hoàn toàn bằng Tiếng Việt).

### Giá trị cốt lõi

- **Giao dịch tức thì 24/7:** Mọi thao tác chuyển khoản, thanh toán hóa đơn, theo dõi sổ cái và cập nhật lãi suất đều xử lý theo thời gian thực với độ chính xác tuyệt đối.
- **Bảo mật đa lớp chuẩn ngân hàng:** Kết hợp định danh khuôn mặt eKYC, xác thực sinh trắc học thiết bị, đối chiếu khuôn mặt giao dịch giá trị lớn, mã OTP Email SHA-256 và khóa phiên tức thì khi phát hiện rủi ro.
- **Trợ lý tài chính AI hỗ trợ 24/7:** Tích hợp **Google Gemini** am hiểu ngữ cảnh thu/chi cá nhân, sẵn sàng giải đáp bằng ngôn ngữ tự nhiên tiếng Việt và điều hướng thẳng tới từng tác vụ ngân hàng chỉ với một chạm.

---

## 2. Hệ sinh thái Tính năng (Core Features)

### 2.1 Phân hệ Khách hàng (`Customer Banking`)

- **Quản lý đa tài khoản (`Checking`, `Saving`, `Mortgage`):**
  - **Trang chủ tổng quan (`CustomerHomeFragment`):** Trình bày hợp nhất toàn bộ danh mục tài khoản, biểu đồ thu/chi hàng tháng trực quan (`MPAndroidChart`) cùng thanh điều hướng 4 tab (*Trang chủ*, *Chuyển tiền*, *Lịch sử*, *Cá nhân*).
  - **Tài khoản Thanh toán (`CheckingDetailFragment`):** Quản lý số dư tức thì, kiểm soát hạn mức mỗi giao dịch và hạn mức ngày, truy cập nhanh chuyển tiền, nạp/rút và xem sao kê chi tiết.
  - **Tài khoản Tiết kiệm (`SavingDetailFragment`):** Theo dõi kỳ hạn gửi, ngày đáo hạn và **tiền lãi dự kiến hàng tháng cập nhật theo thời gian thực** theo biểu lãi suất hiện hành của ngân hàng.
  - **Khoản vay (`MortgageDetailFragment`):** Kiểm soát tổng dư nợ gốc, chu kỳ thanh toán (`MONTHLY` hoặc `BIWEEKLY`), ngày đến hạn và thực hiện trả nợ kỳ hạn trực tiếp từ tài khoản thanh toán.
- **Chuyển tiền nội bộ & liên ngân hàng 24/7 (`TransferFragment`):**
  - Tự động tra cứu và hiển thị tên người thụ hưởng khi chuyển khoản nội bộ NovaPay (`/accounts/lookup`), lưu danh bạ thụ hưởng thường xuyên và xuất/chia sẻ biên lai điện tử dưới dạng **ảnh PNG** hoặc **tài liệu PDF** (`ReceiptFragment`).
- **Thanh toán & Tạo mã QR cá nhân (`QrBitmapEncoder` & `QrScannerFragment`):**
  - Tự động tạo mã QR nhận tiền chuẩn **ISO/IEC 18004** ngay trên màn hình chi tiết tài khoản thanh toán, kết hợp trình quét QR tốc độ cao qua **CameraX + Google ML Kit Barcode Scanning** giúp chuyển khoản chỉ trong vài giây.
- **Nạp tiền trực tuyến qua cổng quốc tế Stripe (`DepositWithdrawFragment`):**
  - Tích hợp **Stripe `PaymentSheet`** với cơ chế xác nhận 2 pha idempotent (`/transactions/confirm` → `/payments/finalize`) đảm bảo nạp tiền an toàn, cộng số dư chính xác một lần duy nhất ngay khi thanh toán thành công; đồng thời hỗ trợ rút tiền linh hoạt.
- **Hệ sinh thái Thanh toán Hóa đơn & Dịch vụ số:**
  - Thanh toán hóa đơn **Điện / Nước** (`UtilityBillFragment`) gạch nợ tức thì, **nạp tiền điện thoại** (`PhoneTopupFragment`), đặt **vé máy bay, vé xem phim, phòng khách sạn** và mua sắm trực tuyến (`ServiceBookingFragment`) kèm quản lý danh sách đơn hàng.
- **Bản đồ Chi nhánh & Chỉ đường đi bộ thông minh (`BranchMapFragment`):**
  - Tích hợp **OpenStreetMap (`osmdroid`)** định vị vị trí người dùng, đề xuất chi nhánh hoặc ATM gần nhất và vẽ lộ trình đi bộ tối ưu qua **OpenRouteService API**.
- **Trợ lý Tài chính AI (`ChatbotFragment`):**
  - Trợ lý **Google Gemini** xử lý câu hỏi tài chính cá nhân bằng tiếng Việt (xem số dư, thống kê thu/chi tháng) và gắn sẵn các nút điều hướng DeepLink (`novapay://...`) mở ngay đúng màn hình dịch vụ theo nhu cầu.

### 2.2 Phân hệ Quản trị Ngân hàng (`Bank Officer Portal`)

- **Bảng điều khiển giám sát vận hành (`OfficerDashboardFragment`):** Theo dõi tập trung số lượng khách hàng, hàng đợi hồ sơ eKYC chờ thẩm định, giao dịch cần hậu kiểm khuôn mặt và biến động hệ thống.
- **Quản lý & Khởi tạo hồ sơ khách hàng (`CustomerManagementFragment`, `CreateCustomerFragment`):** Mở tài khoản khách hàng mới kèm tài khoản thanh toán ban đầu, cập nhật thông tin liên hệ và kích hoạt hoặc khóa quyền truy cập (`ACTIVE` / `LOCKED`) tức thời.
- **Thẩm định & Chụp định danh eKYC tại quầy (`OfficerEkycReviewFragment`):** Đối chiếu trực quan ảnh khuôn mặt định danh để phê duyệt (`VERIFIED`) hoặc từ chối (`REJECTED`), hậu kiểm các giao dịch cần xác minh thủ công và hỗ trợ chụp eKYC trực tiếp cho khách hàng tại quầy giao dịch.
- **Quản lý biểu lãi suất & Nhật ký kiểm toán (`InterestRateManagementFragment`, `audit_logs`):** Cập nhật biểu lãi suất tiết kiệm theo kỳ hạn áp dụng ngay lập tức lên toàn hệ thống; mọi thao tác quản trị đều được lưu vết minh bạch vào nhật ký kiểm toán (`audit_logs`).

---

## 3. Cơ chế Bảo mật Đa lớp (Multi-Layer Security)

- **Định danh khuôn mặt trực tuyến (eKYC) với CameraX & ML Kit:**
  - Phân tích Liveness thời gian thực ngay trên thiết bị thông qua **CameraX** và **Google ML Kit Face Detection** (kiểm tra duy nhất 1 khuôn mặt, 2 mắt mở, góc nhìn trực diện) và nén ảnh tối ưu trước khi lưu trữ bảo mật phía máy chủ. Khách hàng chưa hoàn tất eKYC (`VERIFIED`) được bảo vệ bằng cơ chế chặn mọi giao dịch chi/rút (`EKYC_REQUIRED`).
- **Xác thực thích ứng theo hạn mức giao dịch (`Adaptive MFA`):**
  - **Giao dịch tiêu chuẩn (`<= 5.000.000 ₫`) & Nạp tiền Stripe:** Xác thực bằng **mã OTP 6 chữ số** gửi qua **Brevo HTTPS API** (hiệu lực 180 giây, băm **SHA-256** trên máy chủ, tự động khóa sau 5 lần nhập sai).
  - **Giao dịch giá trị cao (`> 5.000.000 ₫`):** Tự động kích hoạt quy trình bảo mật **3 lớp liên hoàn**:
    1. **FaceCheck:** Chụp khuôn mặt trực tiếp kèm Liveness và đối chiếu với hồ sơ eKYC gốc bằng **Gemini Flash Multimodal** trên máy chủ.
    2. **Sinh trắc học thiết bị (`BiometricPrompt`):** Xác thực vân tay, khuôn mặt hệ thống hoặc mã khóa bảo mật thiết bị (`BIOMETRIC_STRONG | DEVICE_CREDENTIAL`).
    3. **Mã OTP Email 2FA:** Xác nhận bước cuối trước khi thực thi chuyển tiền.
- **Giao dịch nguyên tử ACID & Phân quyền chặt chẽ:**
  - Mọi nghiệp vụ thay đổi số dư, ghi sổ cái (`ledger`), gạch nợ hóa đơn, cập nhật đơn hàng và kỳ trả nợ đều thực thi trong **Firestore Transaction (ACID)** trên Backend; kết hợp **Firebase Custom Claims (`CUSTOMER` / `OFFICER`)**, **DeepLink Role Guard**, giám sát phiên thời gian thực (`SessionManager`), giới hạn tần suất theo IP và luật **`firestore.rules`** ngăn chặn hoàn toàn thao tác ghi trái phép từ ứng dụng khách.

---

## 4. Kiến trúc Hệ thống & Triển khai (Architecture & Deployment)

NovaPay vận hành trên kiến trúc **Hybrid Cloud** tách bạch rành mạch giữa trải nghiệm phản hồi tức thời trên di động và lõi xử lý tài chính an toàn trên đám mây:

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                    ANDROID CLIENT (Java 17 | Native)                    │
│  Presentation (MVVM + Material 3 + ViewBinding + Navigation novapay://) │
│  Domain       (Pure Java 17: Models, Repository Interfaces, UseCases)   │
│  Data         (Cloud Firestore Realtime + Retrofit + Hilt Qualifiers)   │
└──────────────┬───────────────────────────────────────────┬──────────────┘
               │ Đọc dữ liệu thời gian thực                │ Yêu cầu ghi tài chính
               │ (Bảo vệ bởi firestore.rules)              │ (Bearer Firebase ID Token)
               ▼                                           ▼
┌──────────────────────────────┐        ┌─────────────────────────────────┐
│  FIREBASE AUTH & FIRESTORE   │        │   BACKEND API (Node.js 20 + TS) │
│  - Custom Claims (role)      │◄──────►│   Triển khai: Render (HTTPS)    │
│  - 14 Collections & Indexes  │ Admin  │   Health Check: GET /health     │
└──────────────────────────────┘  SDK   └─────┬──────────┬──────────┬─────┘
                                              │          │          │
                                              ▼          ▼          ▼
                                           Stripe      Brevo     Gemini
                                         (Payments)   (2FA OTP)  (AI/eKYC)
```

- **Android Native (`app/`):** Xây dựng 100% bằng **Java 17** (`minSdk 26`, `targetSdk 35`) theo **Clean Architecture** và **MVVM**, sử dụng **Dagger Hilt** (tách biệt `@BackendRetrofit` xác thực Firebase Bearer Token và `@OpenRouteRetrofit`), **Jetpack Navigation Component** (Single-Activity + 3 sub-graph + DeepLinks `novapay://`), **RxJava 3**, và kiểu số nguyên `long` (VNĐ) cho toàn bộ phép tính tiền tệ.
- **Backend API (`server/`):** Xây dựng trên **Node.js 20+**, **Express**, **TypeScript**, **Helmet**, **Firebase Admin SDK**, **Stripe SDK**, **Brevo API** và **Google Gemini API**. Toàn bộ khóa bí mật hệ thống chỉ lưu trữ và xử lý tại máy chủ, tuyệt đối không nhúng vào mã nguồn APK.
- **Triển khai đám mây Render (HTTPS):** Backend đã được cấu hình sẵn sàng (`npm install && npm run build` → `npm start`, lắng nghe `process.env.PORT || 3000` trên host `0.0.0.0`) và triển khai trực tuyến qua **HTTPS** trên nền tảng **Render**, đi kèm endpoint giám sát trạng thái hoạt động liên tục **`GET /health`** (`200 OK`).

---

## 5. Hướng dẫn Trải nghiệm Nhanh & Đội ngũ Phát triển

### 5.1 Cài đặt APK trên thiết bị Android thật

Phiên bản APK phát hành của **NovaPay** đã cấu hình kết nối trực tiếp tới **Backend Render (HTTPS)** qua biến `SERVER_BASE_URL` trong [app/build.gradle.kts](app/build.gradle.kts), cho phép bạn cài đặt và trải nghiệm trọn vẹn mọi tính năng ngân hàng trực tuyến ngay trên điện thoại Android thật **mà không cần cài đặt hay khởi chạy máy chủ cục bộ**:

1. Tải tệp cài đặt **`novapay-release.apk`** *(hoặc **`app-debug.apk`**)* về điện thoại Android (Android 8.0 / API 26 trở lên).
2. Mở tệp `.apk` để cài đặt (cho phép *"Cài đặt ứng dụng không xác định"* từ trình quản lý tệp nếu hệ thống yêu cầu).
3. Mở **NovaPay**, cấp quyền **Camera** (cho eKYC, FaceCheck, Quét mã QR) và **Vị trí** (cho Bản đồ chi nhánh), sau đó đăng nhập bằng một trong các tài khoản trải nghiệm bên dưới.

> *(Nếu bạn là nhà phát triển muốn tự biên dịch mã nguồn Android và vận hành Backend riêng, xem hướng dẫn kỹ thuật chi tiết tại [README.md](./README.md)).*

### 5.2 Danh sách tài khoản trải nghiệm thực tế

Hệ thống khởi tạo sẵn bộ tài khoản đa vai trò và đầy đủ dữ liệu giao dịch thực tế (chi tiết xem tại [docs/TEST_ACCOUNTS.md](docs/TEST_ACCOUNTS.md)):

| Vai trò / Trạng thái | Họ và tên | Email đăng nhập *(hoặc biến `.env`)* | Mật khẩu | Thông tin tài khoản & Phạm vi trải nghiệm |
| :--- | :--- | :--- | :--- | :--- |
| **`OFFICER`** | Trần Minh Quân | `officer@novapay.vn` | `NovaPay@Officer2026` | Quản trị viên: Dashboard vận hành, duyệt eKYC, tạo/sửa/khóa KH, chỉnh lãi suất, Audit Logs |
| **`CUSTOMER`** (`VERIFIED`) | Nguyễn Phan Nhân | `member1.nhan@novapay.vn` *(`SEED_MEMBER_1_EMAIL`)* | `NovaPay@2026` | Checking **`1001000001`** (`50.000.000 ₫`) • Saving **`2001000001`** (`100.000.000 ₫`, 12T) • Mortgage **`3001000001`** (dư nợ `350.000.000 ₫`) |
| **`CUSTOMER`** (`VERIFIED`) | Nguyễn Thành Đạt | `member2.dat@novapay.vn` *(`SEED_MEMBER_2_EMAIL`)* | `NovaPay@2026` | Checking **`1001000002`** (`50.000.000 ₫`) • Saving **`2001000002`** (`80.000.000 ₫`, 6T) • Mortgage **`3001000002`** (dư nợ `240.000.000 ₫`, `BIWEEKLY`) |
| **`CUSTOMER`** (`VERIFIED`) | Nguyễn Văn Duy | `member3.duy@novapay.vn` *(`SEED_MEMBER_3_EMAIL`)* | `NovaPay@2026` | Checking **`1001000003`** (`50.000.000 ₫`) • Saving **`2001000003`** (`120.000.000 ₫`, 12T) • Mortgage **`3001000003`** (dư nợ `300.000.000 ₫`) |
| **`CUSTOMER`** (`PENDING`) | Lê Thị Thu Hà | `customer.pending@novapay.vn` *(`SEED_PENDING_EMAIL`)* | `NovaPay@User2026` | Checking **`1001000102`** (`10.000.000 ₫`) — Trải nghiệm hồ sơ eKYC đang chờ Officer phê duyệt |
| **`CUSTOMER`** (`UNVERIFIED`) | Phạm Quốc Bảo | `customer.unverified@novapay.vn` *(`SEED_UNVERIFIED_EMAIL`)* | `NovaPay@User2026` | Checking **`1001000103`** (`2.000.000 ₫`) — Trải nghiệm luồng tự định danh eKYC trên ứng dụng |

**Dữ liệu tra cứu nhanh khi trải nghiệm giao dịch:**
- **Nạp tiền trực tuyến qua Stripe:** Số thẻ `4242 4242 4242 4242` • Hết hạn tương lai (VD: `12/34`) • CVC `424` • ZIP `70000`.
- **Hóa đơn Điện (`EVN TP.HCM`):** `PE0100001234` (`850.000 ₫` — xác thực OTP chuẩn) • `PE0100009999` (`6.200.000 ₫` — kích hoạt bảo mật 3 lớp FaceCheck + Sinh trắc học + OTP).
- **Hóa đơn Nước (`SAWACO Nhà Bè`):** `NU0700005678` (`240.000 ₫` chưa thanh toán).

> *Mẹo trải nghiệm FaceCheck `> 5.000.000 ₫`: Hãy dùng tài khoản **Officer** → **Quản lý khách hàng** → **Chụp hộ eKYC tại quầy** cho tài khoản khách hàng bạn đang sử dụng và phê duyệt trong **Duyệt eKYC & Hậu kiểm**, giúp Gemini đối chiếu chính xác gương mặt của bạn.*

### 5.3 Đội ngũ phát triển

| Thành viên | Vai trò phụ trách | Phạm vi đóng góp chính |
| :--- | :--- | :--- |
| **Nguyễn Phan Nhân** | **Core, Security & Account Lead (`Module A`)** | Hệ thống xác thực & `SessionManager`, `BiometricHelper`, `MainActivity` & DeepLink Guard (`nav_graph.xml`, `nav_core.xml`), Hồ sơ cá nhân, Định danh eKYC & FaceCheck Liveness (`CameraX + ML Kit`), Trang chủ & Biểu đồ dòng tiền, 3 màn hình Tài khoản (`Checking`, `Saving`, `Mortgage`), Lịch sử giao dịch, Hilt Core/Network Modules, Backend `ekyc.ts` & `firestore.rules`. |
| **Nguyễn Thành Đạt** | **Money, Payment & Security Hardening (`Module B`)** | Chuyển tiền nội bộ/liên ngân hàng, Quét & tạo mã QR, Biên lai điện tử PNG & PDF, Cổng nạp tiền Stripe `PaymentSheet` & rút tiền, Xác thực OTP 2FA, Thanh toán hóa đơn Điện/Nước, Nạp điện thoại, Đặt dịch vụ & đơn hàng, Backend `accounts.ts`, `transactions.ts`, `payments.ts`, dịch vụ `otp.ts`, `email.ts`, `stripe.ts`, `faceCheck.ts`, Rate Limiter & kiểm thử Backend. |
| **Nguyễn Văn Duy** | **Officer, Map, AI & Design System (`Module C`)** | Toàn bộ Phân hệ Bank Officer (Dashboard, Quản lý KH, Duyệt eKYC, Điều chỉnh lãi suất, Audit Logs), Bản đồ chi nhánh `osmdroid` & chỉ đường OpenRouteService, Trợ lý tài chính AI Gemini (`ChatbotFragment`), Design System Material 3 (*Clean Fintech Minimalism*), Backend `officer.ts`, `ai.ts`, `chatbot.ts`, cấu hình Gradle & triển khai Render HTTPS. |
