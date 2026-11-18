# GEMINI.md — Quy Chuẩn Kỹ Thuật Bắt Buộc Dự Án NovaPay

Tài liệu này là **quy chuẩn kỹ thuật tối cao (Single Source of Truth)** bắt buộc tuân thủ đối với mọi AI Agent và thành viên phát triển dự án **NovaPay (Mobile Banking)** (xem chi tiết lý giải kiến trúc tại `docs/SPEC_CHOT.md`). Mọi thay đổi mã nguồn trái với các quy tắc dưới đây đều bị coi là không hợp lệ.

---

## 1. Công Nghệ Cốt Lõi & Ngôn Ngữ Bắt Buộc

- **Định danh ứng dụng:**
  - `namespace`: `"com.novabank.novapay"`
  - `applicationId`: `"com.novabank.novapay"`
  - SDK: `minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`
- **Ngôn ngữ lập trình Android:**
  - **100% Java 17** (`JavaVersion.VERSION_17`).
  - **TUYỆT ĐỐI CẤM dùng Kotlin (`.kt`)** cho bất kỳ class nào của ứng dụng Android (ngoại trừ các file cấu hình Gradle Kotlin DSL `*.gradle.kts`).
  - **TUYỆT ĐỐI CẤM dùng Jetpack Compose.**
- **Giao diện người dùng (UI):**
  - **XML Layouts + ViewBinding** (`buildFeatures { viewBinding = true }`). Cấm dùng `findViewById` thủ công trong các Fragment/Activity mới hoặc `DataBinding`.
  - **Material Design 3** (`com.google.android.material:material`).
  - Ngôn ngữ hiển thị trên UI: **100% Tiếng Việt** (mọi chuỗi text hiển thị phải khai báo trong `res/values/strings_*.xml`, cấm hardcode chuỗi trực tiếp trong XML/Java).
- **Dependency Injection:**
  - **Dagger Hilt** (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `@Module`, `@InstallIn`), dùng `annotationProcessor` cho Java.
  - **Bắt buộc tách 2 Qualifier cho Retrofit** trong `com.novabank.novapay.core.di`:
    - `@BackendRetrofit`: Có `AuthInterceptor` tự động gắn Firebase Bearer Token, timeout 30s, gọi tới Node.js Server.
    - `@OpenRouteRetrofit`: Không gắn Token ngân hàng, gọi tới `https://api.openrouteservice.org/`.
- **Điều hướng (Navigation):** Kiến trúc Single-Activity (`MainActivity`) kết hợp **Jetpack Navigation Component** (`navigation-fragment` & `navigation-ui` bản Java).
- **Xử lý bất đồng bộ & Luồng dữ liệu:**
  - Tầng `presentation`: Dùng `LiveData<UiState<T>>` và `SingleLiveEvent<T>` (cho điều hướng một lần, Toast/Snackbar, Dialog).
  - Tầng `data` & `domain`: Dùng **RxJava 3** (`Single<AppResult<T>>`, `Observable<AppResult<T>>`, `Completable`) kết hợp `RxAndroid` (`AndroidSchedulers.mainThread()`), và Guava `ListenableFuture` (tương thích CameraX / ML Kit).
- **Chuẩn màn hình MVVM:**
  - Mỗi màn hình gồm đúng bộ 3 class Java + 1 file XML layout: `XxxFragment.java`, `XxxViewModel.java`, `XxxUiState.java` và `fragment_xxx.xml`.
  - Bắt buộc xử lý đầy đủ 4 trạng thái giao diện: `LOADING`, `SUCCESS`, `EMPTY`, `ERROR`.
- **Bảo mật API Keys:**
  - **TUYỆT ĐỐI CẤM nhúng Gemini API Key, Brevo API Key, Stripe Secret Key hoặc Firebase Admin Credentials vào mã nguồn Android / `BuildConfig`.** Phía Android chỉ được chứa `STRIPE_PUBLISHABLE_KEY` (`pk_test_...`).
- **Backend Server (`server/`):** Node.js 20 + TypeScript + Express + Firebase Admin SDK (kết hợp Firebase Auth & Cloud Firestore gói Spark).

---

## 2. Quy Tắc Clean Architecture, Kiểu Dữ Liệu & Nghiệp Vụ Cốt Lõi

### 2.1 Phân tầng kiến trúc (`presentation` → `domain` ← `data`)
- **`domain` (Pure Java 17 — Lõi nghiệp vụ độc lập):**
  - Chứa `model/`, `repository/` (interfaces), và `usecase/`.
  - **TUYỆT ĐỐI CẤM import bất kỳ package nào thuộc `android.*`, `androidx.*`, `com.google.firebase.*`, `retrofit2.*`, `okhttp3.*`, `com.google.gson.*`**.
  - Chỉ được phép sử dụng thư viện chuẩn Java 17 (`java.*`), `javax.inject.Inject`, và các kiểu luồng của `io.reactivex.rxjava3.*` cho interface Repository/UseCase.
- **`data` (Triển khai dữ liệu):**
  - Chứa `dto/`, `mapper/`, `remote/` (`firebase/`, `server/`, `maps/`, `ai/`), và `repository/*RepositoryImpl.java`.
  - Chịu trách nhiệm chuyển đổi hai chiều (mapping) giữa kiểu dữ liệu của Firestore/Retrofit DTO và `domain/model`.
- **`presentation` (Giao diện & Trạng thái):**
  - Chỉ giao tiếp với `domain` (thông qua `UseCase` hoặc `Repository` interface), **cấm gọi trực tiếp `data` layer (DTO, ApiService, FirestoreDataSource)**.
- **`core` (Hạ tầng dùng chung):**
  - Chứa `common/` (`AppResult`, `SingleLiveEvent`, `Constants`), `di/` (Hilt Modules & Qualifiers), `security/` (`BiometricHelper`, `SessionManager`), `ui/` (`BaseFragment`, `StateLayoutHelper`), `util/` (`CurrencyFormatter`, `DateUtils`, `ImageCompressor`, `QrHelper`).

### 2.2 Quy tắc kiểu dữ liệu bắt buộc
- **Tiền tệ (Money / Currency):**
  - **BẮT BUỘC dùng kiểu số nguyên `long` hoặc `Long`** (đơn vị: VNĐ).
  - **TUYỆT ĐỐI CẤM dùng `double` hoặc `float`** cho số dư, số tiền giao dịch, hạn mức, phí, hoặc tiền lãi để tránh sai số dấu phẩy động.
  - Công thức tính lãi tiết kiệm dự kiến hàng tháng (trả về `long`, kết hợp realtime `accounts` + `interest_rates` theo `termMonths`):
    `long monthlyProfit = (long) Math.floor((balance * annualRate) / (12.0 * 100.0));`
  - Định dạng hiển thị tiền tệ thống nhất qua `CurrencyFormatter.formatVnd(long amount)` (ví dụ: `1.250.000 ₫`).
- **Thời gian (Date / Time):**
  - Trong tầng `domain`: **BẮT BUỘC dùng `java.time.Instant`** (không dùng `java.util.Date` cũ hay `com.google.firebase.Timestamp`).
  - Trong tầng `data` (Firestore): Lưu `com.google.firebase.Timestamp` và chuyển đổi sang `java.time.Instant` tại `data/mapper/`.
  - Mọi phép tính tổng hạn mức giao dịch trong ngày (`dailyLimit`) trên Server bắt buộc tính theo múi giờ **`Asia/Ho_Chi_Minh` (UTC+7)**.
- **Tọa độ địa lý & Fallback Bản đồ (Location):**
  - Trong tầng `domain`: Dùng POJO thuần Java `GeoLocation(double lat, double lng)`.
  - Trong tầng `data` (Firestore): Lưu `com.google.firebase.firestore.GeoPoint` và chuyển đổi tại `data/mapper/`.
  - Tại `BranchMapFragment`: Timeout lấy GPS thực là **5 giây**; nếu mất GPS, từ chối quyền, hoặc tọa độ cách cụm chi nhánh TP.HCM **> 100 km** (như máy ảo mặc định ở Mỹ), bắt buộc tự động fallback về tọa độ **ĐH Tôn Đức Thắng (`10.7326, 106.6998` — Quận 7, TP.HCM)** kèm thông báo rõ ràng để luôn vẽ được đường đi bộ.

### 2.3 Quy tắc bảo mật giao dịch & Ràng buộc nghiệp vụ bắt buộc
- **Nguồn thanh toán & Ràng buộc tài khoản:**
  - **Chỉ tài khoản `type == "CHECKING"` đang `"ACTIVE"`** mới được làm `fromAccountId` cho mọi giao dịch thanh toán, chuyển tiền, rút tiền và trả nợ vay (cấm dùng `SAVING` hay `MORTGAGE` làm nguồn chi tiêu).
  - Cấm tự chuyển tiền nội bộ cho chính tài khoản nguồn (`fromAccountId != toAccountId`).
  - Giao dịch `MORTGAGE_PAYMENT`: Trừ tiền từ `CHECKING`, giảm `mortgage.outstandingBalance` và tự động dời `mortgage.nextDueDate` (`+1 tháng` nếu `MONTHLY`, `+14 ngày` nếu `BIWEEKLY`).
  - Khách hàng thực hiện giao dịch chi/rút bắt buộc có `users/{uid}.status == "ACTIVE"` và `users/{uid}.ekyc.status == "VERIFIED"`.
  - `SessionManager` trên Android phải lắng nghe realtime `users/{uid}`; ngay khi `status == "LOCKED"`, cưỡng bức `signOut()` và đưa về màn hình đăng nhập tức thì.
- **Điều phối OTP 2FA, FaceCheck & Sinh trắc học:**
  - Mọi giao dịch tài chính đều bắt buộc xác thực 2FA bằng mã **OTP 6 số qua Email** (hiệu lực 180s, băm SHA-256, khóa sau 5 lần nhập sai).
  - **Giao dịch chi/rút có `amount > 5_000_000L` (5.000.000 VNĐ)** bắt buộc thực hiện đủ 3 lớp theo thứ tự:
    1. `/transactions/initiate` tạo `draft` ở trạng thái `"PENDING_FACE"` (`otpExpiresAt: null`, **chưa gửi OTP**).
    2. `FaceCheck`: Android dùng CameraX + ML Kit kiểm tra Liveness (1 khuôn mặt, 2 mắt mở `>= 0.7f`, nhìn thẳng `|EulerY/Z| <= 15°`), nén JPEG `< 200KB` gửi lên `/transactions/face-check`. Server dùng **Gemini Flash Multimodal** đối chiếu với `ekyc_images/{uid}` (sai mặt trả `FACE_MISMATCH`; hết quota/timeout AI thì fallback cho qua kèm cờ `manualAuditRequired: true`), sau đó mới phát hành Email OTP 180s.
    3. `BiometricHelper`: Cấu hình `BIOMETRIC_STRONG | DEVICE_CREDENTIAL` (hỗ trợ fallback mã PIN/mật khẩu thiết bị nếu máy không có vân tay) → Nhập OTP → `/transactions/confirm`.
  - **Giao dịch nạp tiền Stripe (`type == "GATEWAY"`):**
    - Chỉ áp dụng cho Nạp tiền vào `CHECKING` (có OTP 2FA, **miễn `FaceCheck` > 5M**).
    - `/transactions/confirm` chỉ tạo Stripe `PaymentIntent`, chuyển `draft` sang `"PENDING_PAYMENT"` và trả `{ status: "PENDING_PAYMENT", clientSecret }` (**chưa cộng số dư**).
    - Sau khi `PaymentSheet` hoàn tất, `/payments/finalize` xác nhận `PaymentIntent.status == "succeeded"` với Stripe và chạy Firestore Transaction kiểm tra khóa idempotent (`draft.status != "CONFIRMED"`) để cộng tiền đúng 1 lần duy nhất.

---

## 3. Lược Đồ 14 Collections Firestore & Quy Tắc Security Rules

1. **`users/{uid}`**:
   - Trường: `role` (`"CUSTOMER"` | `"OFFICER"`), `fullName`, `email`, `phone`, `nationalId`, `dob`, `address`, `avatarUrl`, `status` (`"ACTIVE"` | `"LOCKED"`), `ekyc: { status, verifiedBy, verifiedAt }`, `createdBy`, `createdAt`, `updatedAt`.
   - Subcollection **`users/{uid}/beneficiaries/{id}`**: `name`, `accountNumber`, `bankCode`, `alias`.
2. **`ekyc_images/{uid}`**:
   - Trường: `imageBase64` (< 200KB), `capturedAt`, `capturedBy`. *(Chỉ Server ghi; chủ tài khoản và Officer đọc).*
3. **`accounts/{accountId}`**:
   - Trường: `ownerUid`, `type` (`"CHECKING"` | `"SAVING"` | `"MORTGAGE"`), `accountNumber` (unique), `status` (`"ACTIVE"` | `"FROZEN"` | `"CLOSED"`), `balance` (`Long`), `currency` (`"VND"`), `perTxnLimit` (`Long`), `dailyLimit` (`Long`), `saving: { termMonths, principal, interestRateId, openedAt, maturityDate }`, `mortgage: { loanAmount, outstandingBalance, termMonths, annualRate, paymentFrequency, installmentAmount, nextDueDate }`, `createdAt`.
   - Subcollection **`accounts/{accountId}/ledger/{entryId}`**: `txId`, `direction` (`"IN"` | `"OUT"`), `amount` (`Long`), `balanceAfter` (`Long`), `description`, `createdAt`.
4. **`transactions/{txId}`**:
   - Trường: `type` (`"TRANSFER_INTERNAL"` | `"TRANSFER_INTERBANK"` | `"DEPOSIT"` | `"WITHDRAW"` | `"BILL"` | `"TOPUP"` | `"SERVICE_PAYMENT"` | `"MORTGAGE_PAYMENT"` | `"GATEWAY"`), `fromAccountId`, `toAccountId?`, `toExternal: { bankCode, accountNumber, name }?`, `amount` (`Long`), `fee` (`Long`), `note`, `status` (`"SUCCESS"` | `"FAILED"`), `requiresBiometric`, `biometricPassed`, `otpVerified`, `faceChecked`, `meta: { billId?, billCode?, phone?, itemId?, orderId?, mortgageAccountId?, gatewayRef? }`, `initiatedBy`, `createdAt`.
5. **`transaction_drafts/{draftId}`**:
   - Trường: `uid`, `payload`, `status` (`"PENDING_FACE"` | `"PENDING_OTP"` | `"PENDING_PAYMENT"` | `"CONFIRMED"` | `"EXPIRED"`), `otpHash?`, `otpExpiresAt?`, `attempts`, `faceChecked`, `stripeIntentId?`, `txId?`, `createdAt`. *(Chỉ Server đọc/ghi).*
6. **`face_checks/{draftId}`**:
   - Trường: `uid`, `txId?`, `imageBase64`, `manualAuditRequired`, `createdAt`. *(Chỉ Server ghi; Officer đọc để hậu kiểm).*
7. **`interest_rates/{rateId}`**:
   - Trường: `termMonths` (`0` = không kỳ hạn), `annualRate`, `effectiveFrom`, `updatedBy`, `updatedAt`.
8. **`banks/{bankCode}`**:
   - Trường: `name`, `logoUrl`, `napasCode`.
9. **`branches/{branchId}`**:
   - Trường: `name`, `address`, `location` (`GeoPoint`), `phone`, `openHours`, `type` (`"BRANCH"` | `"ATM"`).
10. **`utility_bills/{billId}`**:
    - Trường: `type` (`"ELECTRIC"` | `"WATER"`), `providerName`, `customerCode`, `customerName`, `period`, `amount` (`Long`), `dueDate`, `status` (`"UNPAID"` | `"PAID"`).
11. **`service_catalog/{itemId}`**:
    - Trường: `category` (`"FLIGHT"` | `"MOVIE"` | `"HOTEL"` | `"PRODUCT"`), `title`, `price` (`Long`), `details`, `imageUrl`.
12. **`orders/{orderId}`**:
    - Trường: `uid`, `itemId`, `category`, `quantity`, `totalAmount` (`Long`), `status`, `txId`, `createdAt`.
13. **`chat_sessions/{uid}/messages/{msgId}`**:
    - Trường: `role` (`"USER"` | `"BOT"`), `text`, `actions: [{ label, deepLink }]`, `createdAt`.
14. **`audit_logs/{logId}`**:
    - Trường: `actorUid`, `action`, `targetId`, `details`, `createdAt`.

### Quy tắc `firestore.rules` bắt buộc:
- **Khóa chặt `users` & `accounts`:** Chỉ chính chủ (`uid == request.auth.uid` / `ownerUid == request.auth.uid`) hoặc `request.auth.token.role == "OFFICER"` mới có quyền đọc. Cấm mở quyền đọc toàn cục cho khách hàng khác (việc tra cứu tên người nhận nội bộ bắt buộc đi qua `GET /accounts/lookup`).
- **Cấm Client tự ghi dữ liệu tài chính & kiểm toán:** Toàn bộ thao tác ghi vào `accounts`, `ledger`, `transactions`, `transaction_drafts`, `face_checks`, `ekyc_images`, `utility_bills`, `orders`, `interest_rates`, `audit_logs` chỉ được thực hiện bởi Backend Server (Admin SDK).

---

## 4. Bảng Định Tuyến DeepLinks (`novapay://`) & Quy Tắc Điều Hướng Liên Phân Hệ

- **Cấm gọi trực tiếp `R.id.*` chéo giữa 3 sub-graph** (`nav_core.xml`, `nav_money.xml`, `nav_officer_map_ai.xml`). Mọi điều hướng chéo giữa phân hệ của A, B, C bắt buộc gọi qua `NavController.navigate(Uri.parse("novapay://..."))`.
- **Giao tiếp Dialog dùng chung** (`FaceCheckDialogFragment`, `OtpVerificationDialogFragment`) bắt buộc dùng **`FragmentResultListener`** với hằng số khóa khai báo trong `Constants.java`.
- **DeepLink Auth & Role Guard tại `MainActivity`:** Chặn mọi DeepLink khi chưa đăng nhập (đưa về `LoginFragment`) hoặc khi sai vai trò (`OFFICER` không được mở các URI giao dịch của `CUSTOMER` và ngược lại).

| Deep Link URI | Màn hình đích | Phụ trách |
| :--- | :--- | :---: |
| `novapay://home` | Trang chủ khách hàng (`CustomerHomeFragment`) | A |
| `novapay://accounts/{accountId}` | Bộ định tuyến tài khoản (`AccountRouterFragment` → tự rẽ nhánh kèm `popUpTo` sang `Checking` / `Saving` / `MortgageDetailFragment` theo `type`) | A |
| `novapay://transactions?range=month` | Lịch sử giao dịch trong tháng (`TransactionHistoryFragment`) | A |
| `novapay://profile` | Hồ sơ cá nhân (`ProfileFragment`) | A |
| `novapay://ekyc` | Định danh khuôn mặt eKYC (`EkycCaptureFragment`) | A |
| `novapay://transfer` | Chuyển tiền (`TransferFragment`) | B |
| `novapay://bills` | Thanh toán hóa đơn điện/nước (`UtilityBillFragment`) | B |
| `novapay://topup` | Nạp tiền điện thoại (`PhoneTopupFragment`) | B |
| `novapay://services` | Đặt vé máy bay, xem phim, khách sạn, mua sắm (`ServiceBookingFragment`) | B |
| `novapay://branches` | Bản đồ chi nhánh & Chỉ đường (`BranchMapFragment`) | C |

---

## 5. Hợp Đồng API Server (RESTful JSON)

Mọi request từ Android `@BackendRetrofit` (trừ `GET /health`) đều đi qua `AuthInterceptor.java` tự động đính kèm header:
`Authorization: Bearer <Firebase_ID_Token>`

| Endpoint | Owner | Request Body / Query | Response Body (JSON) | Mã lỗi chuẩn (`error`) |
| :--- | :---: | :--- | :--- | :--- |
| `GET /accounts/lookup` | B | Query: `?accountNumber=...` | `{ accountId, accountNumber, ownerName }` | `INVALID_ARGUMENT`, `ACCOUNT_NOT_FOUND`, `ACCOUNT_NOT_ACTIVE` |
| `POST /transactions/initiate` | B | `{ type, fromAccountId, toAccountId?, toExternal?, amount, note?, meta? }` | `{ draftId, requiresBiometric, requiresFace, otpExpiresAt? }` | `INVALID_ARGUMENT`, `ACCOUNT_NOT_ACTIVE`, `INSUFFICIENT_FUNDS`, `LIMIT_EXCEEDED`, `EKYC_REQUIRED`, `EMAIL_SEND_FAILED` |
| `POST /transactions/face-check` | B | `{ draftId, imageBase64 }` | `{ ok: true, otpExpiresAt }` | `DRAFT_EXPIRED`, `INVALID_ARGUMENT`, `FACE_MISMATCH`, `EMAIL_SEND_FAILED` |
| `POST /transactions/confirm` | B | `{ draftId, otp, biometricPassed }` | `{ txId?, status, clientSecret? }` *(cập nhật nguyên tử cả `utility_bills` / `orders` / `mortgage`)* | `OTP_INVALID`, `OTP_EXPIRED`, `TOO_MANY_ATTEMPTS`, `BIOMETRIC_REQUIRED`, `FACE_REQUIRED`, `INSUFFICIENT_FUNDS`, `LIMIT_EXCEEDED` |
| `POST /transactions/resend-otp` | B | `{ draftId }` | `{ otpExpiresAt }` | `DRAFT_EXPIRED`, `EMAIL_SEND_FAILED` |
| `POST /payments/finalize` | B | `{ draftId }` | `{ txId, status }` *(Idempotent trên `draftId`)* | `DRAFT_EXPIRED`, `PAYMENT_NOT_COMPLETED` |
| `POST /ekyc/submit` | A | `{ imageBase64, targetUid? }` | `{ status: "PENDING" }` | `INVALID_ARGUMENT`, `PERMISSION_DENIED` |
| `POST /ekyc/review` | A | `{ uid, decision }` | `{ status: "VERIFIED" \| "REJECTED" }` *(ghi `audit_logs`)* | `PERMISSION_DENIED`, `EKYC_NOT_PENDING` |
| `POST /officer/customers` | C | `{ fullName, email, phone, nationalId, dob, address, initialPassword }` | `{ uid }` *(ghi `audit_logs`)* | `PERMISSION_DENIED`, `INVALID_ARGUMENT`, `EMAIL_ALREADY_EXISTS` |
| `PATCH /officer/customers/:uid` | C | `{ fullName?, phone?, address?, status? }` | `{ ok: true }` *(ghi `audit_logs`)* | `PERMISSION_DENIED`, `INVALID_ARGUMENT`, `USER_NOT_FOUND` |
| `POST /officer/interest-rates` | C | `{ termMonths, annualRate }` | `{ rateId }` *(ghi `audit_logs`)* | `PERMISSION_DENIED`, `INVALID_ARGUMENT` |
| `POST /ai/chat` | C | `{ message }` | `{ reply, actions: [{ label, deepLink }] }` *(tự fallback Rule-based NLP nếu lỗi 429/timeout)* | `INVALID_ARGUMENT` |
| `GET /health` | B | *(Không có)* | `{ ok: true }` | *(Không có)* |

---

## 6. Quy Tắc Sở Hữu File, Day-0 Skeleton & Chống Xung Đột Git

Để các thành viên / Agent làm việc song song không xảy ra merge conflict, bắt buộc chốt bộ khung **Day-0 Skeleton** trên `main` (`NovaPayApp`, `MainActivity`, `AppResult`, `UiState`, `SingleLiveEvent`, `Constants`, `BaseFragment<VB>`, `Account` + `AccountRepository` interface, 2 Hilt Qualifiers và 4 file Navigation stub) trước khi tách nhánh theo đúng phạm vi sở hữu sau:

### 6.1 Chuỗi giao diện (`app/src/main/res/values/`)
- `strings.xml`: **Chỉ giữ `app_name`**, tuyệt đối không thêm chuỗi nghiệp vụ vào file này.
- `strings_core.xml` **(Owner: A)**: Đăng nhập, Hồ sơ cá nhân, eKYC, FaceCheck, Biometric, Trang chủ Customer, Tài khoản (Checking/Saving/Mortgage), Lịch sử giao dịch, lỗi mạng/chung.
- `strings_money.xml` **(Owner: B)**: Chuyển tiền nội bộ/liên ngân hàng, Quét QR, Biên lai điện tử, Nạp/Rút Stripe, OTP Dialog, Hóa đơn Điện/Nước, Nạp điện thoại, Đặt dịch vụ/vé/mua sắm.
- `strings_officer.xml` **(Owner: C)**: Phân hệ Bank Officer (Dashboard, Quản lý KH, Duyệt eKYC, Quản lý lãi suất, Audit Logs), Bản đồ chi nhánh (`osmdroid`), AI Chatbot, các trạng thái Design System dùng chung.

### 6.2 Sơ đồ điều hướng (`app/src/main/res/navigation/`)
- `nav_graph.xml` **(Owner: A)**: Graph gốc gắn với `NavHostFragment` trong `MainActivity`, chỉ chứa các thẻ `<include>` tới 3 sub-graph bên dưới.
- `nav_core.xml` **(Owner: A)**: Luồng Auth, Profile, eKYC, Customer Home, `AccountRouterFragment` & Chi tiết 3 loại tài khoản, Lịch sử giao dịch.
- `nav_money.xml` **(Owner: B)**: Luồng Chuyển tiền, Quét QR, Biên lai, Nạp/Rút cổng thanh toán, OTP, Hóa đơn tiện ích, Nạp điện thoại, Đặt dịch vụ.
- `nav_officer_map_ai.xml` **(Owner: C)**: Luồng Bank Officer, Bản đồ chi nhánh & Chỉ đường, AI Chatbot.

### 6.3 Dagger Hilt Modules, Design System & Backend Routes
- **Hilt DI (`com.novabank.novapay.core.di`)**:
  - Thành viên **A**: `FirebaseModule.java`, `NetworkModule.java` (`@BackendRetrofit`, `@OpenRouteRetrofit`), `CoreRepositoryModule.java`
  - Thành viên **B**: `MoneyRepositoryModule.java`
  - Thành viên **C**: `OfficerMapAiRepositoryModule.java`
- **Design System (`app/src/main/res/values/` & `layout/`)**:
  - `themes.xml`, `colors.xml`, `styles.xml` và các layout trạng thái dùng chung (`view_loading_state.xml`, `view_error_state.xml`, `view_empty_state.xml`) thuộc quyền quản lý của **Thành viên C**. A và B tái sử dụng, không tự ý sửa đổi trực tiếp nếu chưa thống nhất.
- **Backend Routes (`server/src/routes/`)**:
  - `ekyc.ts` **(Owner: A)** | `accounts.ts`, `transactions.ts`, `payments.ts` **(Owner: B)** | `officer.ts`, `ai.ts` **(Owner: C)**.
- **Phát hành Google Play Store:**
  - Nêu rõ trên Store Listing, Privacy Policy và màn hình Đăng nhập đây là ứng dụng mô phỏng học thuật (Academic Sandbox Simulation — TDTU), kèm tài khoản kiểm thử `Customer` & `Officer` khai báo đầy đủ trong mục **App Access** của Google Play Console.
