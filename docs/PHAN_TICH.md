# Kế hoạch triển khai & Phân chia công việc – NovaPay (Mobile Banking) – Phiên bản Java Chuẩn Điểm Tối Đa

- **Môn học:** Phát triển ứng dụng di động (Mobile Apps Development - TDTU)
- **Quy mô:** Nhóm 3 người | **Thời gian thực hiện:** 14 ngày
- **Định hướng chất lượng:** Đáp ứng **100% yêu cầu nghiệp vụ & kỹ thuật** của đề bài, tối ưu hóa toàn bộ tiêu chí trong **Marking Scheme (10/10 điểm)** và bổ sung các tính năng nâng cao (Value-Added Features) phù hợp với quy mô đồ án cuối kỳ bậc Đại học.
- **Ngôn ngữ & Công nghệ cốt lõi:** **Java 17** (Android SDK), **XML ViewBinding / Material Design 3**, **MVVM + Clean Architecture**, **Dagger Hilt**, **RxJava 3 / Guava ListenableFuture** (xử lý bất đồng bộ chuẩn Java), **Firebase** (Auth, Firestore gói Spark) + **Backend Node.js 20 (TypeScript/Express)** miễn phí.

---

## 0. Các quyết định kiến trúc & Bổ sung nâng cao (Tối ưu Marking 10/10)

| # | Hạng mục | Quyết định kỹ thuật & Nâng cấp điểm cộng | Trạng thái |
| :--- | :--- | :--- | :--- |
| 1 | **Ngôn ngữ lập trình Java** | Sử dụng **Java 17** cho toàn bộ mã nguồn Android. Áp dụng triệt để các Design Pattern (Repository, Factory, Singleton, Observer qua `LiveData`, Builder) để ghi điểm tối đa ở mục *Architecture* và *Analysis & Design*. | Đã chốt |
| 2 | **Google Play Deployment** | Phát hành bản **Open Testing / Production** trên Google Play Console (dùng tài khoản tạo trước 2020 của thành viên C để không vướng luật 12 tester/14 ngày). A soạn và host trang *Privacy Policy* trên GitHub Pages. | Đã chốt |
| 3 | **Kiến trúc Hybrid Backend** | Dùng **Firebase Firestore & Auth (gói Spark miễn phí)** kết hợp **Server Node.js + Express + Firebase Admin SDK** triển khai miễn phí trên Render. Giải quyết bài toán bảo mật giao dịch, xử lý Stripe và gửi OTP mà không cần nâng cấp gói Blaze. | Đã chốt |
| 4 | **Cổng thanh toán (Stripe)** | Tích hợp **Stripe Android SDK (`PaymentSheet` - Test Mode)** cho tính năng nạp tiền (`GATEWAY`) theo luồng chuẩn bảo mật: `initiate -> confirm -> PaymentSheet -> finalize`. | Đã chốt |
| 5 | **Xác thực 2 lớp (2FA OTP)** | Gửi mã **OTP 6 số thật qua Email** (dùng Brevo REST API qua HTTPS để tránh lỗi Render chặn cổng SMTP). OTP băm SHA-256 trên server, hiệu lực 3 phút, khóa sau 5 lần nhập sai. | Đã chốt |
| 6 | **eKYC & Xác minh khuôn mặt** | Dùng **CameraX + Google ML Kit Face Detection** (Java API) nhận diện khuôn mặt thời gian thực (kiểm tra mắt mở, góc nghiêng đầu chống gian lận ảnh tĩnh cơ bản). Nén ảnh JPEG (< 200KB) gửi qua `/ekyc/submit` lưu Base64 trên Firestore. Giao dịch **> 5.000.000 VNĐ** bắt buộc quét lại khuôn mặt (`FaceCheck`) + **Android `BiometricPrompt`** (vân tay/FaceID thiết bị) + **OTP**. | Đã chốt |
| 7 | **Bản đồ & Lộ trình đi bộ** | Dùng **osmdroid (OpenStreetMap)** + **OpenRouteService API** (miễn phí, không cần thẻ Visa). Hiển thị vị trí hiện tại, danh sách chi nhánh ngân hàng gần nhất, tính khoảng cách theo công thức Haversine và vẽ tuyến đường đi bộ tối ưu. Có cơ chế fallback vẽ đường thẳng nếu mất mạng/lỗi API. | Đã chốt |
| 8 | **Trợ lý ảo AI Chatbot** | Tích hợp **Google Gemini API** (qua Server Node.js hoặc Firebase AI Logic) sử dụng *Structured Output (JSON)* trả về `{ "reply": "...", "actions": [{"label": "...", "deepLink": "..."}] }`. Kèm bộ trả lời dự phòng (Rule-based NLP) khi hết quota. | Đã chốt |
| 9 | **Tính năng điểm cộng (Value-Added)** | 1. **Biên lai chuyển tiền điện tử (E-Receipt):** Tự động xuất ảnh PNG biên lai giao dịch thành công và chia sẻ qua Intent.<br>2. **Quét mã QR chuyển khoản nhanh (VietQR-style):** Dùng ML Kit Barcode Scanning đọc thông tin người nhận.<br>3. **Thống kê chi tiêu trực quan:** Biểu đồ thu/chi hàng tháng bằng MPAndroidChart.<br>4. **Danh bạ thụ hưởng (Beneficiaries):** Lưu tự động người nhận sau khi chuyển khoản. | Đã chốt |
| 10 | **Tổ chức code Java** | Tuân thủ nghiêm ngặt **Clean Architecture** (`presentation` -> `domain` -> `data`). Phân chia quyền sở hữu theo feature; tách riêng các file resource XML (`strings_core.xml`, `strings_money.xml`, `strings_officer.xml`), Navigation Graph con và Hilt Module để 3 thành viên code song song không xung đột. | Đã chốt |

---

## 1. Phân công nhiệm vụ chi tiết (Chia dọc theo Feature)

| Thành viên | Vai trò | Phụ trách chuyên môn (Java Android + Server) | Mục tiêu điểm Barem |
| :--- | :--- | :--- | :--- |
| **A** | **Core, Security & Account Lead** | Khung dự án Java, Dagger Hilt, Navigation Component, Firebase Auth & Custom Claims, Hồ sơ khách hàng, eKYC (CameraX + ML Kit Java), FaceCheck & BiometricPrompt, Security Rules, Trang chủ Customer, Quản lý 3 loại tài khoản (Checking/Saving/Mortgage), Biểu đồ & Lịch sử giao dịch, Seed dữ liệu. | Architecture (1đ) + DB Design (1đ) + User Profiles (1đ) + Security (1đ) + Account Mgmt (0.5đ) |
| **B** | **Money, Backend & Utilities Lead** | Khung Server Node.js/TypeScript, Middleware xác thực ID Token, Hệ thống OTP Email HTTPS, Tích hợp Stripe PaymentIntent, Chuyển tiền nội bộ & liên ngân hàng (kèm quét QR nhận tiền & xuất ảnh biên lai), Nạp/Rút tiền, Trả kỳ hạn Mortgage, Thanh toán hóa đơn điện/nước, Nạp tiền điện thoại, Đặt vé/khách sạn/mua sắm. | Transaction Mgmt (1.0đ) + Utilities (0.5đ) + Sơ đồ luồng & Hạ tầng Server |
| **C** | **Officer, Map, AI & QA Lead** | Hệ thống Design System (Material 3 XML Styles, Themes, Custom Views), Giao diện Bank Officer (Tạo/sửa khách hàng, cập nhật lãi suất tiết kiệm, hỗ trợ chụp & duyệt hồ sơ eKYC, Audit Logs), Bản đồ chi nhánh & Chỉ đường đi bộ, AI Chatbot tích hợp Deep Link, Tổng hợp báo cáo chuẩn mẫu Khoa CNTT, Quay video demo & Upload Google Play Store. | UI Design (1.0đ) + Officer (1.0đ) + Navigation (0.5đ) + Chatbot (0.5đ) + Báo cáo & Demo (1.0đ) |

---

### 1.1 Thành viên A: Core, Security & Account Management
- **Khởi tạo & Kiến trúc nền tảng:**
  - Tạo project Android **Java 17** (`package com.novabank.novapay`, `minSdk = 26`, `compileSdk = 35`), cấu hình Gradle Version Catalog (`libs.versions.toml`), Dagger Hilt, Jetpack Navigation Component (XML `nav_graph.xml` bao gồm 3 sub-graph `nav_core.xml`, `nav_money.xml`, `nav_officer_map_ai.xml`).
  - Thiết lập `NetworkModule` (Retrofit + OkHttp + `AuthInterceptor` tự động đính kèm `FirebaseUser.getIdToken(false)` vào header `Authorization: Bearer <token>`).
- **Xác thực & Hồ sơ khách hàng:**
  - Màn hình đăng nhập (`LoginFragment`), tự động làm mới token (`getIdToken(true)`) để đọc Custom Claim `role` (`CUSTOMER` hoặc `OFFICER`) và điều hướng về đúng phân hệ.
  - Màn hình Hồ sơ cá nhân (`ProfileFragment`): xem thông tin định danh, cập nhật số điện thoại, địa chỉ, ảnh đại diện.
- **Hệ thống bảo mật eKYC, FaceCheck & Sinh trắc học:**
  - Xây dựng Fragment/Dialog dùng chung `EkycCaptureFragment` sử dụng **CameraX** và **ML Kit `FaceDetector`** (Java API) kiểm tra khuôn mặt hợp lệ (mắt mở, chính diện), nén ảnh JPEG dưới 200KB và gửi lên `POST /ekyc/submit`.
  - Viết route backend `server/src/routes/ekyc.ts` xử lý `/ekyc/submit` (cho cả Customer tự gửi và Officer gửi hộ qua `targetUid`) và `/ekyc/review` (Officer phê duyệt/từ chối).
  - Xây dựng `FaceCheckDialogFragment` (kiểm tra khuôn mặt trước giao dịch > 5 triệu VNĐ) và `BiometricHelper` bọc `androidx.biometric.BiometricPrompt` để B gọi trước khi xác nhận OTP.
- **Trang chủ & Quản lý 3 loại tài khoản:**
  - `CustomerHomeFragment`: Hiển thị tổng tài sản, danh sách thẻ tài khoản, truy cập nhanh các tiện ích, tích hợp biểu đồ thu/chi tháng bằng **MPAndroidChart**.
  - 3 màn hình chi tiết chuyên biệt:
    - `CheckingDetailFragment`: Số dư khả dụng, số tài khoản, mã QR cá nhân để nhận tiền.
    - `SavingDetailFragment`: Số dư gốc, kỳ hạn, lãi suất hiện hành (đọc từ `interest_rates`), **tiền lãi dự kiến hàng tháng** tính bằng kiểu `long` theo công thức: $\text{Lãi tháng} = \lfloor \frac{\text{Số dư} \times \text{Lãi suất năm}}{12 \times 100} \rfloor$.
    - `MortgageDetailFragment`: Dư nợ gốc còn lại, kỳ hạn vay, lãi suất vay, số tiền phải trả định kỳ (tháng hoặc 2 tuần), ngày đến hạn tiếp theo và nút thanh toán kỳ hạn (chuyển sang luồng của B).
  - `TransactionHistoryFragment`: Đọc biến động số dư từ collection `ledger`, hỗ trợ lọc theo tháng và loại giao dịch (Tiền vào / Tiền ra).
- **Cơ sở dữ liệu & Bảo mật:**
  - Viết và deploy `firestore.rules` và `firestore.indexes.json` ngay từ đầu, siết chặt phân quyền theo từng giai đoạn.
  - Viết `server/scripts/seed.ts` tạo dữ liệu mẫu chuẩn (tài khoản Officer, Customer đã/chưa eKYC, danh sách ngân hàng, 10 chi nhánh tại TP.HCM, hóa đơn mẫu, danh mục dịch vụ, bảng lãi suất) và tài liệu `docs/TEST_ACCOUNTS.md`.

### 1.2 Thành viên B: Money, Backend Server & Utilities
- **Xây dựng Backend Server (`server/` - Node.js 20 + TypeScript + Express):**
  - Thiết lập khung server (`index.ts`, `config.ts`, `middleware/auth.ts` xác thực Firebase ID Token bằng Firebase Admin SDK, endpoint `/health`).
  - Triển khai lên Render (gói miễn phí), cấu hình biến môi trường và cài đặt **UptimeRobot** ping `/health` mỗi 5 phút chống ngủ đông.
  - Viết dịch vụ gửi **OTP Email thật** qua **Brevo API (HTTPS)** (`services/email.ts`), lưu mã OTP đã băm SHA-256 (`services/otp.ts`), thời hạn 3 phút, tối đa 5 lần nhập sai.
  - Viết các endpoint giao dịch (`routes/transactions.ts`: `/initiate`, `/face-check`, `/confirm`, `/resend-otp`) và thanh toán (`routes/payments.ts`: `/payments/finalize` xác minh `PaymentIntent` với Stripe API).
- **Quản lý Giao dịch (Transaction Management):**
  - Xây dựng luồng giao dịch hợp nhất qua `TransactionRepositoryImpl` (Retrofit):
    - **Chuyển tiền nội bộ (`TRANSFER_INTERNAL`):** Nhập số tài khoản NovaPay, tự động tra cứu tên người nhận, hỗ trợ quét mã QR nhận tiền và lưu danh bạ thụ hưởng (`beneficiaries`).
    - **Chuyển tiền liên ngân hàng (`TRANSFER_INTERBANK`):** Chọn ngân hàng từ collection `banks`, nhập số tài khoản và tên người nhận (mô phỏng chuyển khoản nhanh 24/7).
    - **Nạp tiền qua cổng Stripe (`GATEWAY`):** Tích hợp `PaymentSheet` của Stripe Android SDK (Test Mode) -> gọi `/payments/finalize` cộng tiền vào tài khoản.
    - **Rút tiền (`WITHDRAW`) & Thanh toán khoản vay (`MORTGAGE_PAYMENT`).**
  - Màn hình xác thực `OtpVerificationDialogFragment` (đếm ngược 180s, gửi lại mã OTP) và màn hình **Biên lai điện tử (`ReceiptFragment`)** cho phép lưu/chia sẻ ảnh biên lai giao dịch.
- **Tiện ích thanh toán (Utilities):**
  - `UtilityBillFragment`: Tra cứu và thanh toán hóa đơn Điện / Nước theo mã khách hàng (`utility_bills`).
  - `PhoneTopupFragment`: Nạp tiền điện thoại theo nhà mạng và mệnh giá cố định.
  - `ServiceBookingFragment`: Hệ thống đặt vé máy bay, vé xem phim, đặt phòng khách sạn và mua sắm thương mại điện tử dùng chung kiến trúc dữ liệu động từ `service_catalog` và tạo đơn hàng vào `orders`.

### 1.3 Thành viên C: Bank Officer, Map Navigation, AI Chatbot & Đóng gói
- **Hệ thống giao diện chuẩn (Design System):**
  - Xây dựng trước tiên `themes.xml`, `colors.xml`, `styles.xml` (Material Design 3), các custom layout dùng chung (`view_loading_state.xml`, `view_error_state.xml`, `view_empty_state.xml`) để A và B tái sử dụng đồng bộ.
- **Phân hệ Nhân viên Ngân hàng (Bank Officer):**
  - `OfficerDashboardFragment`: Thống kê tổng số khách hàng, hồ sơ eKYC chờ duyệt, tổng số giao dịch trong ngày.
  - `CustomerManagementFragment` & `CreateCustomerFragment`: Tìm kiếm khách hàng, cập nhật thông tin cá nhân/trạng thái khóa tài khoản, tạo tài khoản khách hàng mới thông qua `POST /officer/customers` (server tự tạo Firebase Auth + gán Custom Claim `role=officer/customer` + tạo sẵn 1 tài khoản Checking).
  - `OfficerEkycReviewFragment`: Hỗ trợ chụp ảnh eKYC trực tiếp cho khách hàng tại quầy (tái sử dụng `EkycCaptureFragment` của A truyền `targetUid`) và màn hình duyệt/từ chối hồ sơ eKYC kèm đối chiếu ảnh `face_checks`.
  - `InterestRateManagementFragment`: Cập nhật biểu lãi suất tiết kiệm theo kỳ hạn (`interest_rates`), mọi thao tác của Officer đều tự động ghi nhật ký vào `audit_logs`.
- **Bản đồ & Chỉ đường thông minh (Navigation):**
  - `BranchMapFragment`: Xin quyền vị trí (`ACCESS_FINE_LOCATION`), hiển thị bản đồ **osmdroid**, đánh dấu vị trí người dùng và toàn bộ chi nhánh/ATM của NovaBank (`branches`).
  - Tự động tính khoảng cách và đề xuất **chi nhánh gần nhất**, gọi **OpenRouteService API** vẽ lộ trình đi bộ ngắn nhất (`Polyline` trên bản đồ) kèm thời gian di chuyển ước tính (có fallback vẽ đường chim bay nếu lỗi API).
- **Trợ lý ảo AI Chatbot:**
  - `ChatbotFragment`: Giao diện chat trực quan tích hợp **Gemini API** (qua Server hoặc Firebase AI Logic) với System Prompt định hướng trả về cấu trúc JSON `{ "reply": "...", "actions": [{"label": "...", "deepLink": "..."}] }`.
  - Hiển thị các nút hành động nhanh (Action Chips) ngay dưới câu trả lời của Bot, khi bấm vào sẽ kích hoạt Deep Link (`novapay://bills`, `novapay://topup`, `novapay://transactions?range=month`...) điều hướng thẳng tới màn hình chức năng tương ứng. Có bộ trả lời dự phòng theo từ khóa nếu mất kết nối AI.
- **Hoàn thiện đồ án, Báo cáo & Google Play:**
  - Biên soạn và định dạng báo cáo tổng hợp theo đúng mẫu quy định của Khoa CNTT - ĐH Tôn Đức Thắng (đầy đủ Mục lục, Danh mục hình vẽ, Tài liệu tham khảo).
  - Quay và dựng Video Demo trọn vẹn kịch bản nghiệp vụ.
  - Build file `.aab` (Signed App Bundle) và phát hành lên **Google Play Console**.

---

## 2. Quy ước kỹ thuật bắt buộc (Java 17 Standards)

### 2.1 Công nghệ & Phiên bản
- **Ngôn ngữ Android:** **Java 17** (`sourceCompatibility JavaVersion.VERSION_17`, `targetCompatibility JavaVersion.VERSION_17`).
- **Giao diện:** XML Layouts kết hợp **ViewBinding** và thư viện **Material Components for Android (`com.google.android.material:material`)**.
- **Bất đồng bộ & Luồng dữ liệu:**
  - Tầng `presentation`: Sử dụng `LiveData<UiState<T>>` và `SingleLiveEvent` (cho điều hướng, Toast/Snackbar).
  - Tầng `data` & `domain`: Sử dụng **RxJava 3** (`Single<AppResult<T>>`, `Observable<AppResult<T>>`) hoặc `CompletableFuture` để xử lý chuỗi tác vụ bất đồng bộ sạch sẽ, tránh callback hell.
- **Chuỗi hiển thị (Localization):** Tiếng Việt 100% trên UI, tách biệt thành 3 file XML trong `res/values/` để không bao giờ xung đột Git: `strings_core.xml` (A), `strings_money.xml` (B), `strings_officer.xml` (C).

### 2.2 Quy tắc kiểu dữ liệu & Kiến trúc
- **Tiền tệ:** Bắt buộc dùng kiểu nguyên thủy `long` hoặc `Long` (đơn vị VNĐ). Tuyệt đối cấm `double` hay `float` để tránh sai số dấu phẩy động. Format hiển thị qua lớp tiện ích `CurrencyFormatter.formatVnd(long amount)` (VD: `1.250.000 ₫`).
- **Thời gian & Tọa độ:**
  - Firestore lưu `com.google.firebase.Timestamp` và `com.google.firebase.firestore.GeoPoint`.
  - Tầng `domain` chỉ dùng `java.time.Instant` và lớp POJO `GeoLocation(double lat, double lng)`. Tầng `domain` **tuyệt đối không import bất kỳ package nào của `android.*` hay `com.google.firebase.*`**.
- **Cấu trúc mỗi màn hình (MVVM):** Gồm đúng 3 class Java + 1 file XML layout: `XxxFragment.java`, `XxxViewModel.java`, `XxxUiState.java`, và `fragment_xxx.xml`. Mọi màn hình đều xử lý đủ 4 trạng thái: `LOADING`, `SUCCESS`, `EMPTY`, `ERROR`.

### 2.3 Hợp đồng API Server (RESTful JSON)
Mọi request từ Retrofit đều đi qua `AuthInterceptor.java` tự động thêm header `Authorization: Bearer <Firebase_ID_Token>`.

| Endpoint | Owner | Request Body (JSON) | Response Body (JSON) | Mã lỗi chuẩn (`error`) |
| :--- | :---: | :--- | :--- | :--- |
| `POST /transactions/initiate` | B | `{ type, fromAccountId, toAccountId?, toExternal?, amount, note?, meta? }` | `{ draftId, requiresBiometric, requiresFace, otpExpiresAt }` | `INVALID_ARGUMENT`, `ACCOUNT_NOT_ACTIVE`, `INSUFFICIENT_FUNDS`, `LIMIT_EXCEEDED`, `EKYC_REQUIRED`, `EMAIL_SEND_FAILED` |
| `POST /transactions/face-check` | B | `{ draftId, imageBase64 }` | `{ ok: true }` | `DRAFT_EXPIRED`, `INVALID_ARGUMENT` |
| `POST /transactions/confirm` | B | `{ draftId, otp, biometricPassed }` | `{ txId, status, clientSecret? }` | `OTP_INVALID`, `OTP_EXPIRED`, `TOO_MANY_ATTEMPTS`, `BIOMETRIC_REQUIRED`, `FACE_REQUIRED` |
| `POST /transactions/resend-otp` | B | `{ draftId }` | `{ otpExpiresAt }` | `DRAFT_EXPIRED` |
| `POST /payments/finalize` | B | `{ draftId }` | `{ txId, status }` | `DRAFT_EXPIRED`, `PAYMENT_NOT_COMPLETED` |
| `POST /ekyc/submit` | A | `{ imageBase64, targetUid? }` | `{ status: "PENDING" }` | `INVALID_ARGUMENT`, `PERMISSION_DENIED` |
| `POST /ekyc/review` | A | `{ uid, decision }` | `{ status: "VERIFIED" \| "REJECTED" }` | `PERMISSION_DENIED`, `EKYC_NOT_PENDING` |
| `POST /officer/customers` | C | `{ fullName, email, phone, nationalId, dob, address, initialPassword }` | `{ uid }` | `PERMISSION_DENIED`, `INVALID_ARGUMENT`, `EMAIL_ALREADY_EXISTS` |
| `GET /health` | B | *(Không)* | `{ ok: true }` | *(Không)* |

### 2.4 Bảng định tuyến Deep Link (`novapay://`)
Dùng chung cho cả Navigation Component và AI Chatbot Action Chips:

| Deep Link URI | Màn hình đích | Chủ sở hữu |
| :--- | :--- | :---: |
| `novapay://home` | Trang chủ khách hàng (`CustomerHomeFragment`) | A |
| `novapay://accounts/{accountId}` | Chi tiết tài khoản (`Checking/Saving/MortgageDetailFragment`) | A |
| `novapay://transactions?range=month` | Lịch sử giao dịch trong tháng (`TransactionHistoryFragment`) | A |
| `novapay://profile` | Hồ sơ cá nhân (`ProfileFragment`) | A |
| `novapay://ekyc` | Định danh khuôn mặt eKYC (`EkycCaptureFragment`) | A |
| `novapay://transfer` | Chuyển tiền (`TransferFragment`) | B |
| `novapay://bills` | Thanh toán hóa đơn điện/nước (`UtilityBillFragment`) | B |
| `novapay://topup` | Nạp tiền điện thoại (`PhoneTopupFragment`) | B |
| `novapay://services` | Đặt vé máy bay, phim, khách sạn, mua hàng (`ServiceBookingFragment`) | B |
| `novapay://branches` | Bản đồ chi nhánh & Chỉ đường (`BranchMapFragment`) | C |

---

## 3. Kế hoạch phủ điểm Barem (Chiến lược đạt 10/10)

| Tiêu chí Marking | Trọng số | Giải pháp kỹ thuật & Minh chứng trong Báo cáo / Demo |
| :--- | :---: | :--- |
| **1. Architecture** | **1.0 đ** | Trình bày rõ kiến trúc **MVVM + Clean Architecture** kết hợp **Hybrid Cloud (Firebase Spark + Node.js Express)**; giải thích lý do dùng Java 17 + Dagger Hilt + RxJava3 và lý do tách Backend xử lý giao dịch để bảo toàn tính toàn vẹn dữ liệu (ACID Firestore Transaction). |
| **2. Analysis & Design** | **3.0 đ** | - **Class Diagrams (1.0đ):** Vẽ đầy đủ sơ đồ lớp cho từng tầng (`Domain Models`, `UseCases`, `Repositories`, `ViewModels`) bằng Draw.io.<br>- **UI (1.0đ):** Thiết kế chuẩn Material Design 3, đồng bộ bảng màu thương hiệu NovaPay, có Dark/Light mode, hiệu ứng mượt mà, đủ trạng thái Loading/Empty/Error.<br>- **Database Design (1.0đ):** Sơ đồ ERD 13 collections trên Firestore, giải thích rõ các mối quan hệ `1-N`, `1-1`, kiểu dữ liệu và chiến lược đánh Index. |
| **3. Features** | **5.0 đ** | Hoàn thiện 100% 7 nhóm chức năng cốt lõi + 4 tính năng điểm cộng (QR Scan, Biên lai ảnh, Biểu đồ chi tiêu, Danh bạ thụ hưởng). Mọi luồng đều hoạt động thực tế, không hardcode giả. |
| **4. Báo cáo & Demo** | **1.0 đ** | Báo cáo trình bày đúng chuẩn mẫu Khoa CNTT TDTU, không lỗi chính tả, có đủ Use Case, Sequence Diagram cho luồng OTP/eKYC/Stripe. Video demo kịch bản liền mạch dài 5-7 phút + Link tải ứng dụng trên Google Play Store. |

---

## Phụ lục A. Cấu trúc thư mục chuẩn dự án (Java 17)

```text
novapay/
├── app/src/main/
│   ├── AndroidManifest.xml                  # Permissions (CAMERA, LOCATION, BIOMETRIC), DeepLinks (A)
│   ├── res/
│   │   ├── layout/                          # fragment_*.xml, item_*.xml, dialog_*.xml (A, B, C theo feature)
│   │   ├── navigation/                      # nav_graph.xml (A), nav_core.xml (A), nav_money.xml (B), nav_officer_map_ai.xml (C)
│   │   └── values/                          # themes.xml, colors.xml, styles.xml (C); strings_core.xml (A), strings_money.xml (B), strings_officer.xml (C)
│   └── java/com/novabank/novapay/
│       ├── NovaPayApp.java                  # @HiltAndroidApp (A)
│       ├── MainActivity.java                # Single-Activity host NavHostFragment & DeepLink handler (A)
│       ├── core/
│       │   ├── common/                      # AppResult.java, SingleLiveEvent.java, Constants.java (A)
│       │   ├── di/                          # FirebaseModule.java, NetworkModule.java, CoreRepositoryModule.java (A)
│       │   │                                # MoneyRepositoryModule.java (B), OfficerMapAiRepositoryModule.java (C)
│       │   ├── security/                    # BiometricHelper.java, SessionManager.java (A)
│       │   ├── ui/                          # BaseFragment.java, StateLayoutHelper.java (C)
│       │   └── util/                        # CurrencyFormatter.java, DateUtils.java, ImageCompressor.java, QrHelper.java (A)
│       ├── domain/                          # Pure Java (Không import Android/Firebase)
│       │   ├── model/                       # user/, account/ (A); transaction/, utility/, bank/ (B); rate/, branch/, chat/, audit/ (C)
│       │   ├── repository/                  # Interfaces chia theo feature (A, B, C)
│       │   └── usecase/                     # Các lớp UseCase đơn nhiệm (VD: TransferMoneyUseCase.java) (A, B, C)
│       ├── data/
│       │   ├── dto/                         # Request/Response POJOs cho Retrofit & Firestore (A, B, C)
│       │   ├── mapper/                      # Chuyển đổi DTO/Timestamp/GeoPoint <-> Domain Model (A, B, C)
│       │   ├── remote/
│       │   │   ├── firebase/                # FirestoreDataSource.java, AuthDataSource.java (A)
│       │   │   ├── server/                  # AuthInterceptor.java, EkycApi.java (A); TransactionApi.java, PaymentApi.java (B); OfficerApi.java (C)
│       │   │   ├── maps/                    # OpenRouteServiceApi.java (C)
│       │   │   └── ai/                      # GeminiChatDataSource.java (C)
│       │   └── repository/                  # *RepositoryImpl.java chia theo feature (A, B, C)
│       └── presentation/
│           ├── auth/                        # LoginFragment, LoginViewModel, LoginUiState (A)
│           ├── ekyc/                        # EkycCaptureFragment, FaceCheckDialogFragment (A)
│           ├── customer/
│           │   ├── home/                    # CustomerHomeFragment + MPAndroidChart (A)
│           │   ├── account/                 # CheckingDetailFragment, SavingDetailFragment, MortgageDetailFragment (A)
│           │   ├── history/                 # TransactionHistoryFragment (A)
│           │   ├── profile/                 # ProfileFragment (A)
│           │   ├── transfer/                # TransferFragment, QrScannerFragment, ReceiptFragment (B)
│           │   ├── payment/                 # DepositWithdrawFragment, StripeGatewayHelper (B)
│           │   ├── utility/                 # UtilityBillFragment, PhoneTopupFragment, ServiceBookingFragment (B)
│           │   ├── otp/                     # OtpVerificationDialogFragment (B)
│           │   ├── branch/                  # BranchMapFragment (osmdroid + OpenRouteService) (C)
│           │   └── chatbot/                 # ChatbotFragment (C)
│           └── officer/                     # OfficerDashboardFragment, CustomerManagementFragment,
│                                            # OfficerEkycReviewFragment, InterestRateManagementFragment (C)
├── server/                                  # Node.js 20 + TypeScript + Express (B chủ trì khung, A & C đóng góp route)
│   ├── src/
│   │   ├── index.ts, config.ts              # Khởi tạo Express, CORS, /health (B)
│   │   ├── middleware/auth.ts               # Verify Firebase ID Token (B)
│   │   ├── routes/
│   │   │   ├── transactions.ts              # initiate, face-check, confirm, resend-otp (B)
│   │   │   ├── payments.ts                  # finalize Stripe (B)
│   │   │   ├── ekyc.ts                      # submit, review (A)
│   │   │   └── officer.ts                   # create/update customer (C)
│   │   └── services/                        # otp.ts, email.ts (Brevo HTTPS), stripe.ts (B)
│   └── scripts/seed.ts                      # Khởi tạo dữ liệu mẫu Firestore & Auth Claims (A)
├── docs/
│   ├── report/                              # Tài liệu báo cáo từng module (A, B, C)
│   ├── diagrams/                            # File nguồn .drawio (Class, ERD, UseCase, Sequence, Deployment)
│   ├── screenshots/                         # Ảnh chụp màn hình thực tế phục vụ báo cáo
│   └── TEST_ACCOUNTS.md                     # Danh sách tài khoản kiểm thử (A)
├── firestore.rules                          # Phân quyền chặt chẽ phía Firestore (A)
├── firestore.indexes.json                   # Composite indexes cho truy vấn lịch sử/hóa đơn (A)
└── gradle/libs.versions.toml                # Quản lý phiên bản thư viện tập trung (A)
```

---

## Phụ lục B. Lược đồ Cơ sở dữ liệu Firestore (13 Collections)

1. **`users/{uid}`**: `role` (`CUSTOMER` | `OFFICER`), `fullName`, `email`, `phone`, `nationalId`, `dob`, `address`, `avatarUrl`, `status` (`ACTIVE` | `LOCKED`), `ekyc: { status, verifiedBy, verifiedAt }`, `createdBy`, `createdAt`, `updatedAt`.
   - Subcollection **`users/{uid}/beneficiaries/{id}`**: `name`, `accountNumber`, `bankCode`, `alias`.
2. **`ekyc_images/{uid}`**: `imageBase64` (< 200KB), `capturedAt`, `capturedBy`. *(Chỉ Server ghi; chủ tài khoản và Officer đọc)*.
3. **`accounts/{accountId}`**: `ownerUid`, `type` (`CHECKING` | `SAVING` | `MORTGAGE`), `accountNumber` (unique), `status` (`ACTIVE` | `FROZEN` | `CLOSED`), `balance` (`Long`), `currency` (`"VND"`), `perTxnLimit` (`Long`), `dailyLimit` (`Long`), `saving: { termMonths, principal, interestRateId, openedAt, maturityDate }`, `mortgage: { loanAmount, outstandingBalance, termMonths, annualRate, paymentFrequency, installmentAmount, nextDueDate }`, `createdAt`.
   - Subcollection **`accounts/{accountId}/ledger/{entryId}`**: `txId`, `direction` (`IN` | `OUT`), `amount` (`Long`), `balanceAfter` (`Long`), `description`, `createdAt`.
4. **`transactions/{txId}`**: `type` (`TRANSFER_INTERNAL` | `TRANSFER_INTERBANK` | `DEPOSIT` | `WITHDRAW` | `BILL` | `TOPUP` | `SERVICE_PAYMENT` | `MORTGAGE_PAYMENT` | `GATEWAY`), `fromAccountId`, `toAccountId?`, `toExternal: { bankCode, accountNumber, name }?`, `amount` (`Long`), `fee` (`Long`), `note`, `status` (`SUCCESS` | `FAILED`), `requiresBiometric`, `biometricPassed`, `otpVerified`, `faceChecked`, `meta: { billCode?, phone?, orderId?, gatewayRef? }`, `initiatedBy`, `createdAt`.
5. **`transaction_drafts/{draftId}`**: `uid`, `payload`, `status` (`PENDING_OTP` | `PENDING_PAYMENT` | `CONFIRMED` | `EXPIRED`), `otpHash`, `otpExpiresAt`, `attempts`, `faceChecked`, `stripeIntentId?`, `createdAt`. *(Chỉ Server đọc/ghi)*.
6. **`face_checks/{draftId}`**: `uid`, `txId?`, `imageBase64`, `createdAt`. *(Chỉ Server ghi; Officer đọc để hậu kiểm)*.
7. **`interest_rates/{rateId}`**: `termMonths` (`0` = không kỳ hạn), `annualRate`, `effectiveFrom`, `updatedBy`, `updatedAt`.
8. **`banks/{bankCode}`**: `name`, `logoUrl`, `napasCode`.
9. **`branches/{branchId}`**: `name`, `address`, `location` (`GeoPoint`), `phone`, `openHours`, `type` (`BRANCH` | `ATM`).
10. **`utility_bills/{billId}`**: `type` (`ELECTRIC` | `WATER`), `providerName`, `customerCode`, `customerName`, `period`, `amount` (`Long`), `dueDate`, `status` (`UNPAID` | `PAID`).
11. **`service_catalog/{itemId}`**: `category` (`FLIGHT` | `MOVIE` | `HOTEL` | `PRODUCT`), `title`, `price` (`Long`), `details`, `imageUrl`.
12. **`orders/{orderId}`**: `uid`, `itemId`, `category`, `quantity`, `totalAmount` (`Long`), `status`, `txId`, `createdAt`.
13. **`chat_sessions/{uid}/messages/{msgId}`**: `role` (`USER` | `BOT`), `text`, `actions: [{ label, deepLink }]`, `createdAt`.
14. **`audit_logs/{logId}`**: `actorUid`, `action`, `targetId`, `details`, `createdAt`.