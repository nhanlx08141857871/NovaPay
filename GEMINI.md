# GEMINI.md — Quy Chuẩn Kỹ Thuật Bắt Buộc Dự Án NovaPay

Tài liệu này là **quy chuẩn kỹ thuật tối cao (Single Source of Truth)** bắt buộc tuân thủ đối với mọi AI Agent và thành viên phát triển dự án **NovaPay (Mobile Banking)**. Mọi thay đổi mã nguồn trái với các quy tắc dưới đây đều bị coi là không hợp lệ.

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
- **Dependency Injection:** **Dagger Hilt** (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `@Module`, `@InstallIn`). Dùng `annotationProcessor` cho trình biên dịch Java.
- **Điều hướng (Navigation):** Kiến trúc Single-Activity (`MainActivity`) kết hợp **Jetpack Navigation Component** (`navigation-fragment` & `navigation-ui` bản Java).
- **Xử lý bất đồng bộ & Luồng dữ liệu:**
  - Tầng `presentation`: Dùng `LiveData<UiState<T>>` và `SingleLiveEvent<T>` (cho điều hướng một lần, Toast/Snackbar, Dialog).
  - Tầng `data` & `domain`: Dùng **RxJava 3** (`Single<AppResult<T>>`, `Observable<AppResult<T>>`, `Completable`) kết hợp `RxAndroid` (`AndroidSchedulers.mainThread()`), và Guava `ListenableFuture` (tương thích CameraX / ML Kit).
- **Chuẩn màn hình MVVM:**
  - Mỗi màn hình gồm đúng bộ 3 class Java + 1 file XML layout: `XxxFragment.java`, `XxxViewModel.java`, `XxxUiState.java` và `fragment_xxx.xml`.
  - Bắt buộc xử lý đầy đủ 4 trạng thái giao diện: `LOADING`, `SUCCESS`, `EMPTY`, `ERROR`.
- **Backend Server (`server/`):** Node.js 20 + TypeScript + Express + Firebase Admin SDK (kết hợp Firebase Auth & Cloud Firestore gói Spark).

---

## 2. Quy Tắc Clean Architecture & Kiểu Dữ Liệu

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
  - Chứa `common/` (`AppResult`, `SingleLiveEvent`, `Constants`), `di/` (Hilt Modules), `security/` (`BiometricHelper`, `SessionManager`), `ui/` (`BaseFragment`, `StateLayoutHelper`), `util/` (`CurrencyFormatter`, `DateUtils`, `ImageCompressor`, `QrHelper`).

### 2.2 Quy tắc kiểu dữ liệu bắt buộc
- **Tiền tệ (Money / Currency):**
  - **BẮT BUỘC dùng kiểu số nguyên `long` hoặc `Long`** (đơn vị: VNĐ).
  - **TUYỆT ĐỐI CẤM dùng `double` hoặc `float`** cho số dư, số tiền giao dịch, hạn mức, phí, hoặc tiền lãi để tránh sai số dấu phẩy động.
  - Công thức tính lãi tiết kiệm dự kiến hàng tháng (trả về `long`):
    `long monthlyProfit = (long) Math.floor((balance * annualRate) / (12.0 * 100.0));`
  - Định dạng hiển thị tiền tệ thống nhất qua `CurrencyFormatter.formatVnd(long amount)` (ví dụ: `1.250.000 ₫`).
- **Thời gian (Date / Time):**
  - Trong tầng `domain`: **BẮT BUỘC dùng `java.time.Instant`** (không dùng `java.util.Date` cũ hay `com.google.firebase.Timestamp`).
  - Trong tầng `data` (Firestore): Lưu `com.google.firebase.Timestamp` và chuyển đổi sang `java.time.Instant` tại `data/mapper/`.
- **Tọa độ địa lý (Location):**
  - Trong tầng `domain`: Dùng POJO thuần Java `GeoLocation(double lat, double lng)`.
  - Trong tầng `data` (Firestore): Lưu `com.google.firebase.firestore.GeoPoint` và chuyển đổi tại `data/mapper/`.
- **Quy tắc bảo mật giao dịch:**
  - Mọi giao dịch tài chính bắt buộc xác thực 2FA bằng mã **OTP 6 số qua Email** (hiệu lực 180s, khóa sau 5 lần nhập sai).
  - Giao dịch có số tiền **`amount > 5_000_000L` (5.000.000 VNĐ)** bắt buộc thực hiện đủ 3 lớp: **Quét khuôn mặt (`FaceCheck`) + Sinh trắc học thiết bị (`BiometricPrompt`) + OTP**.

---

## 3. Lược Đồ 14 Collections Firestore

1. **`users/{uid}`**:
   - Trường: `role` (`"CUSTOMER"` | `"OFFICER"`), `fullName`, `email`, `phone`, `nationalId`, `dob`, `address`, `avatarUrl`, `status` (`"ACTIVE"` | `"LOCKED"`), `ekyc: { status, verifiedBy, verifiedAt }`, `createdBy`, `createdAt`, `updatedAt`.
   - Subcollection **`users/{uid}/beneficiaries/{id}`**: `name`, `accountNumber`, `bankCode`, `alias`.
2. **`ekyc_images/{uid}`**:
   - Trường: `imageBase64` (< 200KB), `capturedAt`, `capturedBy`. *(Chỉ Server ghi; chủ tài khoản và Officer đọc).*
3. **`accounts/{accountId}`**:
   - Trường: `ownerUid`, `type` (`"CHECKING"` | `"SAVING"` | `"MORTGAGE"`), `accountNumber` (unique), `status` (`"ACTIVE"` | `"FROZEN"` | `"CLOSED"`), `balance` (`Long`), `currency` (`"VND"`), `perTxnLimit` (`Long`), `dailyLimit` (`Long`), `saving: { termMonths, principal, interestRateId, openedAt, maturityDate }`, `mortgage: { loanAmount, outstandingBalance, termMonths, annualRate, paymentFrequency, installmentAmount, nextDueDate }`, `createdAt`.
   - Subcollection **`accounts/{accountId}/ledger/{entryId}`**: `txId`, `direction` (`"IN"` | `"OUT"`), `amount` (`Long`), `balanceAfter` (`Long`), `description`, `createdAt`.
4. **`transactions/{txId}`**:
   - Trường: `type` (`"TRANSFER_INTERNAL"` | `"TRANSFER_INTERBANK"` | `"DEPOSIT"` | `"WITHDRAW"` | `"BILL"` | `"TOPUP"` | `"SERVICE_PAYMENT"` | `"MORTGAGE_PAYMENT"` | `"GATEWAY"`), `fromAccountId`, `toAccountId?`, `toExternal: { bankCode, accountNumber, name }?`, `amount` (`Long`), `fee` (`Long`), `note`, `status` (`"SUCCESS"` | `"FAILED"`), `requiresBiometric`, `biometricPassed`, `otpVerified`, `faceChecked`, `meta: { billCode?, phone?, orderId?, gatewayRef? }`, `initiatedBy`, `createdAt`.
5. **`transaction_drafts/{draftId}`**:
   - Trường: `uid`, `payload`, `status` (`"PENDING_OTP"` | `"PENDING_PAYMENT"` | `"CONFIRMED"` | `"EXPIRED"`), `otpHash`, `otpExpiresAt`, `attempts`, `faceChecked`, `stripeIntentId?`, `createdAt`. *(Chỉ Server đọc/ghi).*
6. **`face_checks/{draftId}`**:
   - Trường: `uid`, `txId?`, `imageBase64`, `createdAt`. *(Chỉ Server ghi; Officer đọc để hậu kiểm).*
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

---

## 4. Bảng Định Tuyến DeepLinks (`novapay://`)

Dùng thống nhất cho cả Jetpack Navigation Component và Action Chips của AI Chatbot:

| Deep Link URI | Màn hình đích | Phụ trách |
| :--- | :--- | :---: |
| `novapay://home` | Trang chủ khách hàng (`CustomerHomeFragment`) | A |
| `novapay://accounts/{accountId}` | Chi tiết tài khoản (`CheckingDetailFragment` / `SavingDetailFragment` / `MortgageDetailFragment`) | A |
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

Mọi request từ Android Retrofit (trừ `GET /health`) đều đi qua `AuthInterceptor.java` tự động đính kèm header:
`Authorization: Bearer <Firebase_ID_Token>`

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
| `GET /health` | B | *(Không có)* | `{ ok: true }` | *(Không có)* |

---

## 6. Quy Tắc Sở Hữu File & Chống Xung Đột Git

Để các thành viên / Agent làm việc song song không xảy ra merge conflict, bắt buộc tách biệt tài nguyên dùng chung theo đúng phạm vi sở hữu sau:

### 6.1 Chuỗi giao diện (`app/src/main/res/values/`)
- `strings.xml`: **Chỉ giữ `app_name`**, tuyệt đối không thêm chuỗi nghiệp vụ vào file này.
- `strings_core.xml` **(Owner: A)**: Đăng nhập, Hồ sơ cá nhân, eKYC, FaceCheck, Biometric, Trang chủ Customer, Tài khoản (Checking/Saving/Mortgage), Lịch sử giao dịch, lỗi mạng/chung.
- `strings_money.xml` **(Owner: B)**: Chuyển tiền nội bộ/liên ngân hàng, Quét QR, Biên lai điện tử, Nạp/Rút Stripe, OTP Dialog, Hóa đơn Điện/Nước, Nạp điện thoại, Đặt dịch vụ/vé/mua sắm.
- `strings_officer.xml` **(Owner: C)**: Phân hệ Bank Officer (Dashboard, Quản lý KH, Duyệt eKYC, Quản lý lãi suất, Audit Logs), Bản đồ chi nhánh (`osmdroid`), AI Chatbot, các trạng thái Design System dùng chung.

### 6.2 Sơ đồ điều hướng (`app/src/main/res/navigation/`)
- `nav_graph.xml` **(Owner: A)**: Graph gốc gắn với `NavHostFragment` trong `MainActivity`, chỉ chứa các thẻ `<include>` tới 3 sub-graph bên dưới.
- `nav_core.xml` **(Owner: A)**: Luồng Auth, Profile, eKYC, Customer Home, Chi tiết 3 loại tài khoản, Lịch sử giao dịch.
- `nav_money.xml` **(Owner: B)**: Luồng Chuyển tiền, Quét QR, Biên lai, Nạp/Rút cổng thanh toán, OTP, Hóa đơn tiện ích, Nạp điện thoại, Đặt dịch vụ.
- `nav_officer_map_ai.xml` **(Owner: C)**: Luồng Bank Officer, Bản đồ chi nhánh & Chỉ đường, AI Chatbot.

### 6.3 Dagger Hilt Modules & Design System
- **Hilt DI (`com.novabank.novapay.core.di`)**:
  - Thành viên **A**: `FirebaseModule.java`, `NetworkModule.java`, `CoreRepositoryModule.java`
  - Thành viên **B**: `MoneyRepositoryModule.java`
  - Thành viên **C**: `OfficerMapAiRepositoryModule.java`
- **Design System (`app/src/main/res/values/`)**:
  - `themes.xml`, `colors.xml`, `styles.xml` và các layout trạng thái dùng chung (`view_loading_state.xml`, `view_error_state.xml`, `view_empty_state.xml`) thuộc quyền quản lý của **Thành viên C**. A và B tái sử dụng, không tự ý sửa đổi trực tiếp nếu chưa thống nhất.
- **Backend Routes (`server/src/routes/`)**:
  - `ekyc.ts` **(Owner: A)** | `transactions.ts`, `payments.ts` **(Owner: B)** | `officer.ts` **(Owner: C)**.
