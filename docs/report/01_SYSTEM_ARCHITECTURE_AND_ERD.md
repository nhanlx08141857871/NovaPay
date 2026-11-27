# TÀI LIỆU BÁO CÁO PHẦN 1: KIẾN TRÚC HỆ THỐNG HYBRID CLOUD & THIẾT KẾ CƠ SỞ DỮ LIỆU (ERD)

> **Dự án:** Hệ thống Ngân hàng Số **NovaPay** (`com.novabank.novapay`)  
> **Học phần:** Phát triển Ứng dụng Di động — Khoa Công nghệ Thông tin, Trường Đại học Tôn Đức Thắng (TDTU)  
> **Định dạng sơ đồ:** Chuẩn **PlantUML** (`@startuml ... @enduml`) tối ưu chèn trực tiếp vào báo cáo Microsoft Word (nền trắng `#FFFFFF`, không đổ bóng, bo góc `8px`).

---

## 1. Phân Tích Kiến Trúc Tổng Thể Hybrid Cloud

### 1.1 Bài toán kiến trúc trong ứng dụng Mobile Banking
Một ứng dụng ngân hàng di động nếu chỉ kết nối trực tiếp từ thiết bị Android vào cơ sở dữ liệu đám mây (Serverless thuần túy) sẽ đối mặt với **4 rủi ro bảo mật chí tử**:
1. **Giả mạo số dư & giao dịch phía Client:** Thiết bị người dùng (máy đã root, sửa đổi bộ nhớ hoặc dịch ngược APK) có thể tự ý cộng `balance`, bỏ qua kiểm tra hạn mức ngày (`dailyLimit`), hoặc đánh dấu hóa đơn `PAID` mà không trừ tiền.
2. **Lộ khóa bí mật bên thứ ba (API Key Leakage):** Nếu nhúng `STRIPE_SECRET_KEY`, `GEMINI_API_KEY` hoặc `BREVO_API_KEY` vào `BuildConfig` của Android, kẻ tấn công chỉ mất vài phút dùng `jadx` giải dịch APK để chiếm đoạt toàn bộ hạn mức thanh toán và AI.
3. **Thiếu tính nguyên tử liên tập hợp (Cross-Collection ACID):** Một giao dịch chuyển khoản nội bộ hoặc thanh toán khoản vay cần cập nhật đồng thời 4–5 tài liệu (`accounts` nguồn, `ledger` nguồn, `accounts` đích / `utility_bills` / `orders`, `transactions` và `transaction_drafts`).
4. **Rò rỉ thông tin định danh cá nhân (PII):** Nếu mở quyền đọc bảng `accounts` và `users` cho mọi khách hàng để tra cứu tên người nhận chuyển khoản, kẻ xấu có thể quét toàn bộ số dư, số CCCD và số điện thoại của khách hàng khác.

Để giải quyết triệt để cả 4 bài toán trên mà vẫn giữ được trải nghiệm cập nhật số dư tức thì (Realtime UI), **NovaPay** áp dụng kiến trúc **Hybrid Cloud (Đám mây Lai)** kết hợp **Clean Architecture + MVVM** trên Android (100% Java 17) và **Backend Server độc lập** (Node.js 20 + TypeScript + Express + Firebase Admin SDK).

### 1.2 Phân vai trách nhiệm trong mô hình Hybrid Cloud

| Thành phần | Công nghệ cốt lõi | Trách nhiệm kiến trúc | Lý do lựa chọn |
| :--- | :--- | :--- | :--- |
| **Android Client** | **100% Java 17**, XML + ViewBinding, Material Design 3, Dagger Hilt, Jetpack Navigation (`novapay://`), RxJava 3 + LiveData `UiState<T>`, CameraX, ML Kit, BiometricPrompt, `osmdroid`, MPAndroidChart, Stripe Android SDK | • Hiển thị giao diện 2 phân hệ (`CUSTOMER` / `OFFICER`) xử lý đủ 4 trạng thái `LOADING`, `SUCCESS`, `EMPTY`, `ERROR`.<br>• Đọc dữ liệu thời gian thực từ Firestore cho chính chủ tài khoản.<br>• Kiểm tra Liveness khuôn mặt tại biên (Edge AI bằng ML Kit) và quét mã QR.<br>• Gửi yêu cầu ghi nhạy cảm qua `@BackendRetrofit` đính kèm Firebase ID Token. | Tầng `domain` thuần Java 17 giúp kiểm thử đơn vị (JUnit) siêu tốc trên JVM; tách 3 sub-graphs độc lập chống xung đột Git cho nhóm 3 thành viên. |
| **Backend Server** | **Node.js 20+**, **TypeScript 5**, Express, Helmet, CORS, Rate Limiter, **Firebase Admin SDK**, Stripe Node SDK | • Xác thực chữ ký JWT của Firebase ID Token (`verifyIdToken(token, true)`) và kiểm tra Custom Claim `role`.<br>• Thực thi **ACID Firestore Transaction** hai pha (đọc toàn bộ trước, ghi nguyên tử sau).<br>• Sinh mã OTP 6 số (`crypto.randomInt`), băm SHA-256 kèm salt `draftId`, so sánh hằng thời gian (`timingSafeEqual`).<br>• Giữ kín toàn bộ khóa bí mật (`STRIPE_SECRET_KEY`, `BREVO_API_KEY`, `GEMINI_API_KEY`, `FIREBASE_PRIVATE_KEY`). | Đảm bảo nguyên tắc **Zero-Trust Client**: mọi quy tắc tài chính, hạn mức ngày theo múi giờ `Asia/Ho_Chi_Minh` (UTC+7) và nhật ký kiểm toán `audit_logs` đều do máy chủ kiểm soát tuyệt đối. |
| **Cloud Backend-as-a-Service** | **Firebase Authentication** & **Cloud Firestore** + `firestore.rules` + `firestore.indexes.json` | • Lưu trữ 14 collections + 2 subcollections.<br>• Phát đồng bộ biến động số dư và khóa tài khoản (`status == "LOCKED"`) theo thời gian thực qua `SnapshotListener`.<br>• Thực thi hàng rào bảo vệ thứ hai bằng `firestore.rules` (chặn 100% thao tác ghi tài chính từ Client SDK). | Đồng bộ tức thì không cần tự vận hành WebSocket, hỗ trợ Offline Persistence và truy vấn phức hợp tốc độ cao qua Composite Indexes. |
| **Hệ sinh thái Dịch vụ Ngoài** | **Stripe API**, **Brevo Transactional Email**, **Google Gemini 2.5 Flash**, **OpenRouteService** | • **Stripe:** Cổng nạp tiền thẻ quốc tế (`PaymentIntent` + `PaymentSheet`).<br>• **Brevo:** Gửi Email OTP 2FA hiệu lực 180 giây.<br>• **Gemini Multimodal:** Đối chiếu khuôn mặt eKYC (`> 5.000.000 ₫`) & Trợ lý ảo tài chính kèm DeepLink.<br>• **OpenRouteService:** Định tuyến đường đi bộ ngắn nhất tới chi nhánh/ATM. | Kết hợp chuẩn thanh toán quốc tế, AI đa phương thức và bản đồ mở, đồng thời luôn có **cơ chế Fallback thông minh** (Rule-based NLP, `manualAuditRequired`, tọa độ ĐH Tôn Đức Thắng) chống gián đoạn khi bảo vệ đồ án. |

---

## 2. Sơ Đồ Thành Phần & Triển Khai Hệ Thống (Component & Deployment Diagram)

### 2.1 Sơ đồ Thành phần Kiến trúc Phân tầng (PlantUML Component Diagram)

```plantuml
@startuml NovaPay_Component_Architecture
skinparam backgroundColor #FFFFFF
skinparam shadowing false
skinparam roundcorner 8
skinparam defaultFontName "Segoe UI"
skinparam defaultFontSize 12
skinparam ArrowColor #334155
skinparam PackageBorderColor #64748B
skinparam PackageBackgroundColor #F8FAFC
skinparam ComponentBorderColor #0F4C81
skinparam ComponentBackgroundColor #EFF6FF
skinparam DatabaseBorderColor #047857
skinparam DatabaseBackgroundColor #ECFDF5

title Sơ Đồ Thành Phần Kiến Trúc Hybrid Cloud & Clean Architecture — NovaPay

package "Android Application (Java 17 — Single-Activity MVVM)" {
  package "Presentation Layer (UI & State)" {
    [MainActivity\n(Auth & Bidirectional Role Guard)] as MainAct
    [Customer Subsystems\n(Home, Accounts, Transfer, QR,\nStripe, Bills, Topup, Services)] as CustUI
    [eKYC & Security UI\n(EkycCapture, FaceCheckDialog,\nOtpVerificationDialog)] as SecUI
    [Officer, Map & AI UI\n(Dashboard, Customers, Rates,\nBranchMap osmdroid, Chatbot)] as OffUI
  }

  package "Core Layer (Shared Infrastructure)" {
    [SessionManager\n(Realtime Lock Listener)] as SessMgr
    [BiometricHelper\n(STRONG | DEVICE_CREDENTIAL)] as BioHelper
    [Hilt DI Qualifiers\n(@BackendRetrofit / @OpenRouteRetrofit)] as HiltDI
  }

  package "Domain Layer (Pure Java 17 — Zero Android Imports)" {
    [Domain UseCases\n(InitiateTx, ConfirmTx, SavingProjection,\nFindNearestBranch, OfficerUseCases)] as UseCases
    [Repository Interfaces\n(14 Pure Java Contracts)] as RepoInterfaces
    [Domain Models\n(long VND, Instant, GeoLocation)] as Models
  }

  package "Data Layer (Repositories, Mappers & Remote Sources)" {
    [Repository Implementations\n(*RepositoryImpl)] as RepoImpls
    [AuthInterceptor\n(Bearer Firebase ID Token)] as AuthInt
    [Firestore DataSources\n(Realtime SnapshotListeners)] as FsDS
    [OpenRouteServiceApi\n(Walking Directions)] as OrsApi
  }
}

package "Backend Server (Node.js 20 + TypeScript + Express)" {
  [Security Middleware\n(Helmet, CORS, RateLimit,\nverifyIdToken + requireOfficer)] as AuthMW
  [Financial Routes\n(/accounts, /transactions, /payments)] as FinRoutes
  [eKYC & Officer Routes\n(/ekyc, /officer + audit_logs)] as AdminRoutes
  [AI Chatbot Route\n(/ai/chat + Rule-based Fallback)] as AiRoute
  [Core Services\n(OtpService SHA-256, StripeService,\nFaceCheckService, EmailService)] as SvcLayer
}

cloud "Google Firebase Cloud" {
  [Firebase Authentication\n(JWT + Custom Claim: role)] as FbAuth
  database "Cloud Firestore\n(14 Collections + firestore.rules)" as FirestoreDB
}

cloud "External Cloud APIs" {
  [Stripe API\n(PaymentIntents VND)] as StripeCloud
  [Brevo SMTP API\n(Transactional OTP Email)] as BrevoCloud
  [Google Gemini 2.5 Flash\n(Multimodal FaceCheck & Chat)] as GeminiCloud
  [OpenRouteService API\n(foot-walking GeoJSON)] as OrsCloud
}

MainAct --> SessMgr
CustUI --> UseCases
SecUI --> UseCases
SecUI --> BioHelper
OffUI --> UseCases
UseCases --> RepoInterfaces
UseCases --> Models
RepoImpls ..|> RepoInterfaces
RepoImpls --> AuthInt : "@BackendRetrofit (HTTPS)"
RepoImpls --> FsDS : "Read-only / Allowed Profile"
RepoImpls --> OrsApi : "@OpenRouteRetrofit"

SessMgr --> FbAuth : "Auth State & Claims"
SessMgr --> FirestoreDB : "Listen users/{uid}.status"
FsDS --> FirestoreDB : "SnapshotListener (Guarded by Rules)"
OrsApi --> OrsCloud : "POST /v2/directions/foot-walking"

AuthInt --> AuthMW : "Authorization: Bearer <ID_Token>"
AuthMW --> FbAuth : "admin.auth().verifyIdToken(token, true)"
AuthMW --> FinRoutes
AuthMW --> AdminRoutes
AuthMW --> AiRoute

FinRoutes --> SvcLayer
AdminRoutes --> SvcLayer
AiRoute --> SvcLayer

FinRoutes --> FirestoreDB : "Admin SDK ACID runTransaction()"
AdminRoutes --> FirestoreDB : "Admin SDK + Immutable audit_logs"
AiRoute --> FirestoreDB : "Context Read + Batch Persist"

SvcLayer --> StripeCloud : "STRIPE_SECRET_KEY"
SvcLayer --> BrevoCloud : "BREVO_API_KEY"
SvcLayer --> GeminiCloud : "GEMINI_API_KEY"

@enduml
```

---

### 2.2 Sơ đồ Triển khai Hạ tầng Thực tế (PlantUML Deployment Diagram)

```plantuml
@startuml NovaPay_Deployment_Diagram
skinparam backgroundColor #FFFFFF
skinparam shadowing false
skinparam roundcorner 8
skinparam defaultFontName "Segoe UI"
skinparam defaultFontSize 12
skinparam ArrowColor #334155
skinparam NodeBorderColor #1E3A8A
skinparam NodeBackgroundColor #F8FAFC

title Sơ Đồ Triển Khai Hệ Thống (Deployment Diagram) — NovaPay

node "Thiết bị Người dùng (Android 8.0+ / SDK 26–35)" as AndroidNode {
  artifact "NovaPay APK / AAB\n(Google Play Store — R8 Enabled)" as ApkArtifact {
    component "CameraX + ML Kit\n(On-device Face & QR)" as EdgeML
    component "AndroidX Biometric\n(Hardware Keystore / PIN)" as DeviceBio
    component "Stripe PaymentSheet\n(Publishable Key pk_test_...)" as StripeSDK
    component "Retrofit + OkHttp TLS 1.3" as NetClient
  }
}

node "Render Cloud Platform (PaaS — Node.js 20 Runtime)" as RenderNode {
  artifact "novapay-server (dist/index.js)" as ServerArtifact {
    component "Express REST API\n(Trust Proxy + Helmet + RateLimit)" as ExpressApp
    component "Firebase Admin Cert\n(Env Vars Only)" as AdminSDK
  }
}

node "Uptime Monitoring" as MonitorNode {
  component "UptimeRobot\n(HTTP Ping mỗi 5 phút)" as UptimeBot
}

cloud "Google Cloud / Firebase (novapay-71326)" as GcpCloud {
  database "Cloud Firestore\n(asia-southeast1 / Multi-region)" as CloudFS
  component "Firebase Auth\n(Identity & Custom Claims)" as CloudAuth
}

cloud "Third-Party Managed Services (HTTPS TLS)" as ExtServices {
  component "Stripe Payment Gateway" as ExtStripe
  component "Brevo Email Service" as ExtBrevo
  component "Google AI Studio (Gemini)" as ExtGemini
  component "OpenRouteService GIS" as ExtORS
}

NetClient ==> ExpressApp : "HTTPS REST JSON\n(Bearer ID Token, Timeout 30s)"
NetClient ==> CloudFS : "gRPC / TLS Realtime Sync\n(Enforced by firestore.rules)"
NetClient ==> CloudAuth : "Sign-In & Token Refresh"
StripeSDK ==> ExtStripe : "PCI-DSS Card Tokenization\n(clientSecret)"
NetClient ==> ExtORS : "HTTPS GeoJSON Route"

UptimeBot ..> ExpressApp : "GET /health -> 200 {ok: true}\n(Chống Cold-Start ngủ đông)"

AdminSDK ==> CloudFS : "Privileged Admin Read/Write\n(ACID Transactions)"
AdminSDK ==> CloudAuth : "User Creation, Lock/Revoke,\nsetCustomUserClaims"
ExpressApp ==> ExtStripe : "Create & Retrieve PaymentIntent"
ExpressApp ==> ExtBrevo : "POST /v3/smtp/email (OTP 6 số)"
ExpressApp ==> ExtGemini : "POST generateContent\n(FaceCheck & AI Chat)"

@enduml
```

---

## 3. Thiết Kế Cơ Sở Dữ Liệu Cloud Firestore & Sơ Đồ Thực Thể Liên Kết (ERD)

### 3.1 Sơ đồ ERD 14 Collections & 2 Subcollections (PlantUML Entity-Relationship Diagram)

Hệ thống cơ sở dữ liệu NoSQL trên Cloud Firestore của NovaPay được chuẩn hóa thành **14 collections cấp gốc (root collections)** và **2 subcollections chuyên biệt** (`users/{uid}/beneficiaries` và `accounts/{accountId}/ledger`), đảm bảo truy xuất thời gian thực tốc độ cao, không bị giới hạn kích thước tài liệu 1 MiB khi lịch sử giao dịch tăng trưởng theo thời gian.

```plantuml
@startuml NovaPay_Firestore_ERD
skinparam backgroundColor #FFFFFF
skinparam shadowing false
skinparam roundcorner 8
skinparam defaultFontName "Segoe UI"
skinparam defaultFontSize 11
skinparam linetype ortho
skinparam ArrowColor #334155
skinparam ClassBorderColor #0F4C81
skinparam ClassBackgroundColor #F8FAFC
skinparam ClassHeaderBackgroundColor #DBEAFE

hide circle
hide methods

title Sơ Đồ Thực Thể Liên Kết (ERD) 14 Collections & 2 Subcollections Cloud Firestore — NovaPay

entity "1. users\n(users/{uid})" as users {
  * **uid** : String <<PK>>
  --
  * role : String ("CUSTOMER" | "OFFICER")
  * fullName : String
  * email : String <<Unique>>
  * phone : String
  * nationalId : String
  * dob : String (YYYY-MM-DD)
  * address : String
  avatarUrl : String
  * status : String ("ACTIVE" | "LOCKED")
  * ekyc : Map {status, verifiedBy, verifiedAt}
  createdBy : String <<FK -> users.uid>>
  * createdAt : Timestamp
  * updatedAt : Timestamp
}

entity "1a. beneficiaries\n(users/{uid}/beneficiaries/{id})" as beneficiaries {
  * **id** : String <<PK, Subcollection>>
  --
  * name : String
  * accountNumber : String
  * bankCode : String
  alias : String
}

entity "2. ekyc_images\n(ekyc_images/{uid})" as ekyc_images {
  * **uid** : String <<PK, FK -> users.uid>>
  --
  * imageBase64 : String (JPEG < 200KB)
  * capturedAt : Timestamp
  * capturedBy : String <<FK -> users.uid>>
}

entity "3. accounts\n(accounts/{accountId})" as accounts {
  * **accountId** : String <<PK>>
  --
  * ownerUid : String <<FK -> users.uid>>
  * type : String ("CHECKING" | "SAVING" | "MORTGAGE")
  * accountNumber : String <<Unique>>
  * status : String ("ACTIVE" | "FROZEN" | "CLOSED")
  * balance : Long (VND)
  * currency : String ("VND")
  * perTxnLimit : Long (VND)
  * dailyLimit : Long (VND)
  saving : Map? {termMonths, principal,\n  interestRateId, openedAt, maturityDate}
  mortgage : Map? {loanAmount, outstandingBalance,\n  termMonths, annualRate, paymentFrequency,\n  installmentAmount, nextDueDate}
  * createdAt : Timestamp
}

entity "3a. ledger\n(accounts/{accountId}/ledger/{entryId})" as ledger {
  * **entryId** : String <<PK, Subcollection>>
  --
  * txId : String <<FK -> transactions.txId>>
  * direction : String ("IN" | "OUT")
  * amount : Long (VND)
  * balanceAfter : Long (VND)
  * description : String
  * createdAt : Timestamp
}

entity "4. transactions\n(transactions/{txId})" as transactions {
  * **txId** : String <<PK>>
  --
  * type : String (9 TransactionTypes)
  * fromAccountId : String <<FK -> accounts.accountId>>
  fromAccountNumber : String?
  toAccountId : String? <<FK -> accounts.accountId>>
  toAccountNumber : String?
  toOwnerName : String?
  toExternal : Map? {bankCode, accountNumber, name}
  * amount : Long (VND)
  * fee : Long (VND)
  note : String
  * status : String ("SUCCESS" | "FAILED")
  * requiresBiometric : Boolean
  * biometricPassed : Boolean
  * otpVerified : Boolean
  * faceChecked : Boolean
  meta : Map {billId?, billCode?, phone?,\n  itemId?, orderId?, quantity?,\n  mortgageAccountId?, gatewayRef?}
  * initiatedBy : String <<FK -> users.uid>>
  * createdAt : Timestamp
}

entity "5. transaction_drafts\n(transaction_drafts/{draftId})" as drafts {
  * **draftId** : String <<PK>>
  --
  * uid : String <<FK -> users.uid>>
  * payload : Map
  * status : String ("PENDING_FACE" | "PENDING_OTP"\n  | "PENDING_PAYMENT" | "CONFIRMED" | "EXPIRED")
  * requiresBiometric : Boolean
  * requiresFace : Boolean
  otpHash : String? (SHA-256 + salt)
  otpExpiresAt : Long? (Epoch ms)
  otpSentAt : Long?
  * resendCount : Integer
  faceAttempts : Integer?
  * attempts : Integer (Max 5)
  * faceChecked : Boolean
  stripeIntentId : String?
  txId : String? <<FK -> transactions.txId>>
  * createdAt : Timestamp
  * expireAt : Timestamp (TTL +24h)
}

entity "6. face_checks\n(face_checks/{draftId})" as face_checks {
  * **draftId** : String <<PK, FK -> drafts.draftId>>
  --
  * uid : String <<FK -> users.uid>>
  txId : String? <<FK -> transactions.txId>>
  * imageBase64 : String (JPEG < 200KB)
  * manualAuditRequired : Boolean
  * createdAt : Timestamp
}

entity "7. interest_rates\n(interest_rates/{rateId})" as interest_rates {
  * **rateId** : String <<PK>>
  --
  * termMonths : Integer (0..120)
  * annualRate : Double (%/year)
  * effectiveFrom : Timestamp
  * updatedBy : String <<FK -> users.uid>>
  * updatedAt : Timestamp
}

entity "8. banks\n(banks/{bankCode})" as banks {
  * **bankCode** : String <<PK>>
  --
  * name : String
  logoUrl : String
  * napasCode : String
}

entity "9. branches\n(branches/{branchId})" as branches {
  * **branchId** : String <<PK>>
  --
  * name : String
  * address : String
  * location : GeoPoint (lat, lng)
  * phone : String
  * openHours : String
  * type : String ("BRANCH" | "ATM")
}

entity "10. utility_bills\n(utility_bills/{billId})" as utility_bills {
  * **billId** : String <<PK>>
  --
  * type : String ("ELECTRIC" | "WATER")
  * providerName : String
  * customerCode : String
  * customerName : String
  * period : String
  * amount : Long (VND)
  * dueDate : Timestamp
  * status : String ("UNPAID" | "PAID")
  txId : String? <<FK -> transactions.txId>>
}

entity "11. service_catalog\n(service_catalog/{itemId})" as service_catalog {
  * **itemId** : String <<PK>>
  --
  * category : String ("FLIGHT" | "MOVIE"\n  | "HOTEL" | "PRODUCT")
  * title : String
  * price : Long (VND)
  * details : String
  imageUrl : String
}

entity "12. orders\n(orders/{orderId})" as orders {
  * **orderId** : String <<PK>>
  --
  * uid : String <<FK -> users.uid>>
  * itemId : String <<FK -> service_catalog.itemId>>
  * category : String
  * quantity : Integer
  * totalAmount : Long (VND)
  * status : String ("PAID")
  * txId : String <<FK -> transactions.txId>>
  * createdAt : Timestamp
}

entity "13. chat_sessions/messages\n(chat_sessions/{uid}/messages/{msgId})" as chat_messages {
  * **uid / msgId** : String <<PK, Subcollection>>
  --
  * role : String ("USER" | "BOT")
  * text : String
  * actions : List<Map {label, deepLink}>
  * createdAt : Timestamp
}

entity "14. audit_logs\n(audit_logs/{logId})" as audit_logs {
  * **logId** : String <<PK>>
  --
  * actorUid : String <<FK -> users.uid>>
  * action : String
  * targetId : String
  * details : Map
  * createdAt : Timestamp
}

' --- Quan hệ thực thể (Crow's Foot Notation) ---
users ||--o{ beneficiaries : "owns subcollection"
users ||--o| ekyc_images : "1:1 reference photo"
users ||--o{ accounts : "ownerUid"
users ||--o{ transactions : "initiatedBy"
users ||--o{ drafts : "uid"
users ||--o{ face_checks : "uid"
users ||--o{ orders : "uid"
users ||--o{ chat_messages : "chat_sessions/{uid}"
users ||--o{ audit_logs : "actorUid (Officer)"
users ||--o{ interest_rates : "updatedBy (Officer)"

accounts ||--o{ ledger : "owns subcollection"
accounts ||--o{ transactions : "fromAccountId / toAccountId"
interest_rates |o..o{ accounts : "saving.termMonths / interestRateId"
banks |o..o{ beneficiaries : "bankCode"
banks |o..o{ transactions : "toExternal.bankCode"

transactions ||--o{ ledger : "txId"
drafts ||--o| transactions : "confirms into txId"
drafts ||--o| face_checks : "1:1 draftId"
utility_bills |o--o| transactions : "meta.billId / txId"
service_catalog ||--o{ orders : "itemId"
transactions ||--o| orders : "txId"

@enduml
```

---

### 3.2 Ma Trận Phân Quyền Bảo Mật (`firestore.rules`) & Chỉ Mục Phức Hợp (`firestore.indexes.json`)

#### A. Bảng Ma trận Phân quyền Truy cập 14 Collections

| STT | Collection / Subcollection | Quyền Đọc (`allow read`) phía Android | Quyền Ghi (`allow write`) phía Android | Cơ chế Bảo vệ & Ghi từ Backend |
| :---: | :--- | :--- | :--- | :--- |
| 1 | `users/{uid}` | Chính chủ (`uid == auth.uid`) hoặc `OFFICER` | Chỉ `update` đúng 4 khóa (`phone`, `address`, `avatarUrl`, `updatedAt`) khi `status == "ACTIVE"`, kèm kiểm tra kiểu `string`/`timestamp` & giới hạn độ dài | Tạo mới, khóa/mở khóa, duyệt eKYC bắt buộc qua `/officer/*` và `/ekyc/*` kèm `audit_logs` |
| 1a | `users/{uid}/beneficiaries/{id}` | Chính chủ (`uid == auth.uid`) hoặc `OFFICER` | Chính chủ đang `ACTIVE` (`isActiveUserDoc(uid)` kiểm tra `userDoc != null && status == "ACTIVE"`); kiểm tra nghiêm ngặt 4 khóa (`name`, `accountNumber`, `bankCode`, `alias`) và giới hạn ký tự | Đồng bộ danh bạ thụ hưởng cá nhân (đã biên dịch & triển khai trực tiếp lên Cloud Firestore) |
| 2 | `ekyc_images/{uid}` | Chính chủ hoặc `OFFICER` | **Cấm tuyệt đối (`false`)** | Server kiểm tra JPEG SOI `0xFF 0xD8` và `< 200KB` tại `POST /ekyc/submit` |
| 3 | `accounts/{accountId}` | Chính chủ (`ownerUid == auth.uid`) hoặc `OFFICER` | **Cấm tuyệt đối (`false`)** | Tra cứu tên người nhận nội bộ đi qua `GET /accounts/lookup` (ẩn số dư & CCCD) |
| 3a | `accounts/{accountId}/ledger/{entryId}` | Chủ tài khoản cha (`isAccountOwner(accountId)` kiểm tra an toàn `accDoc != null`) hoặc `OFFICER` | **Cấm tuyệt đối (`false`)** | Chỉ ghi trong ACID Transaction của `/transactions/confirm` và `/payments/finalize` |
| 4 | `transactions/{txId}` | Người khởi tạo (`initiatedBy`), chủ tài khoản nhận nội bộ (`toAccountId` không rỗng & `isAccountOwner`), hoặc `OFFICER` | **Cấm tuyệt đối (`false`)** | Bất biến sau khi hạch toán thành công |
| 5 | `transaction_drafts/{draftId}` | **Cấm tuyệt đối (`false`)** | **Cấm tuyệt đối (`false`)** | Chứa `otpHash` (SHA-256) và `stripeIntentId`; 100% chỉ Admin SDK truy cập |
| 6 | `face_checks/{draftId}` | Chỉ `OFFICER` (phục vụ hậu kiểm) | **Cấm tuyệt đối (`false`)** | Server kiểm tra `< 200KB` và lưu cờ `manualAuditRequired` |
| 7 | `interest_rates/{rateId}` | Mọi người dùng đã đăng nhập (`isSignedIn()`) | **Cấm tuyệt đối (`false`)** | Officer cập nhật qua `POST /officer/interest-rates` kèm `audit_logs` |
| 8 | `banks/{bankCode}` | Mọi người dùng đã đăng nhập | **Cấm tuyệt đối (`false`)** | Danh mục ngân hàng Napas chuẩn hóa qua `seed.ts` |
| 9 | `branches/{branchId}` | Mọi người dùng đã đăng nhập | **Cấm tuyệt đối (`false`)** | Tọa độ `GeoPoint` 10 chi nhánh & ATM TP.HCM |
| 10 | `utility_bills/{billId}` | Mọi người dùng đã đăng nhập (tra cứu cước) | **Cấm tuyệt đối (`false`)** | Cập nhật `UNPAID → PAID` nguyên tử cùng lệnh trừ tiền tại `/transactions/confirm` |
| 11 | `service_catalog/{itemId}` | Mọi người dùng đã đăng nhập | **Cấm tuyệt đối (`false`)** | Server đối chiếu lại `amount === price * quantity` chống sửa giá phía Client |
| 12 | `orders/{orderId}` | Chủ đơn hàng (`uid == auth.uid`) hoặc `OFFICER` | **Cấm tuyệt đối (`false`)** | Khởi tạo nguyên tử trong `/transactions/confirm` |
| 13 | `chat_sessions/{uid}/messages/{msgId}` | Chính chủ (`uid == auth.uid`) | **Cấm tuyệt đối (`false`)** | Ghi theo lô (`WriteBatch`) 2 chiều `USER` + `BOT` tại `POST /ai/chat` |
| 14 | `audit_logs/{logId}` | Chỉ `OFFICER` | **Cấm tuyệt đối (`false`)** | Nhật ký kiểm toán bất biến (Append-only từ Admin SDK) |

#### B. Danh sách 5 Composite Indexes tối ưu hiệu năng (`firestore.indexes.json`)
1. `ledger` (`COLLECTION`): `direction ASC` + `createdAt DESC` — phục vụ lọc sổ cái Tiền vào / Tiền ra theo tháng tại `TransactionHistoryFragment`.
2. `transactions` (`COLLECTION`): `fromAccountId ASC` + `status ASC` + `createdAt ASC` — phục vụ tính tổng hạn mức chi tiêu trong ngày (`dailyLimit`) theo múi giờ `Asia/Ho_Chi_Minh` bên trong Firestore Transaction.
3. `utility_bills` (`COLLECTION`): `customerCode ASC` + `status ASC` — phục vụ tra cứu hóa đơn Điện/Nước chưa thanh toán (`UNPAID`).
4. `orders` (`COLLECTION`): `uid ASC` + `createdAt DESC` — phục vụ hiển thị danh sách đơn đặt vé/mua sắm mới nhất của khách hàng.
5. `face_checks` (`COLLECTION`): `manualAuditRequired ASC` + `createdAt DESC` — phục vụ hàng đợi hậu kiểm khuôn mặt giao dịch `> 5.000.000 ₫` trên `OfficerDashboardFragment` và `OfficerEkycReviewFragment`.
