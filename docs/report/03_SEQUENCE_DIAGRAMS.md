# TÀI LIỆU BÁO CÁO PHẦN 3: SƠ ĐỒ TUẦN TỰ NGHIỆP VỤ TRỌNG TÂM (PLANTUML SEQUENCE DIAGRAMS)

> **Dự án:** Hệ thống Ngân hàng Số **NovaPay** (`com.novabank.novapay`)  
> **Định dạng sơ đồ:** 100% mã nguồn **PlantUML Sequence Diagram** (`@startuml ... @enduml`) tái hiện chính xác từng lớp mã nguồn Java 17, API Endpoint Node.js/TypeScript, quy tắc `firestore.rules` và giao dịch nguyên tử Cloud Firestore.

---

## 1. Luồng 1: Đăng Nhập, Phân Quyền Custom Claims (`CUSTOMER` / `OFFICER`) & Khóa Tài Khoản Tức Thì

### Điểm nhấn kiến trúc ăn điểm:
- Phân quyền vai trò dựa trên **Firebase Custom Claim `role`** được ký số bên trong JWT ID Token (`user.getIdToken(true)`), kết hợp kiểm tra kép trạng thái `users/{uid}.status` trước khi vào màn hình chính.
- `SessionManager` gắn `SnapshotListener` thời gian thực vào `users/{uid}` trong suốt phiên làm việc; ngay khi `OFFICER` khóa tài khoản trên máy khác (`PATCH /officer/customers/:uid` → cập nhật Firestore + `revokeRefreshTokens`), thiết bị của khách hàng tự động `signOut()` và chuyển về `LoginFragment` trong dưới 1 giây.

```plantuml
@startuml NovaPay_Seq_01_Auth_RBAC_RealtimeLock
skinparam backgroundColor #FFFFFF
skinparam shadowing false
skinparam roundcorner 8
skinparam defaultFontName "Segoe UI"
skinparam defaultFontSize 11
skinparam ArrowColor #334155
skinparam SequenceLifeLineBorderColor #64748B
skinparam ParticipantBorderColor #0F4C81
skinparam ParticipantBackgroundColor #EFF6FF
skinparam ActorBorderColor #0F4C81
skinparam DatabaseBorderColor #047857
skinparam DatabaseBackgroundColor #ECFDF5

autonumber
title Luồng 1: Đăng Nhập, Phân Quyền Custom Claims & Cưỡng Bức Đăng Xuất Realtime (RBAC)

actor "Người dùng\n(Customer / Officer)" as UserActor
participant "LoginFragment /\nMainActivity" as UI
participant "LoginViewModel" as VM
participant "FirebaseAuthDataSource" as AuthDS
participant "SessionManager\n(@Singleton)" as SessMgr
participant "Firebase Auth\n(JWT + Claims)" as FbAuth
database "Cloud Firestore\n(users/{uid})" as FS

UserActor -> UI : Nhập Email & Mật khẩu -> Bấm "Đăng nhập"
activate UI
UI -> VM : onLoginClicked(email, password)
activate VM

VM -> VM : Kiểm tra rỗng & định dạng Patterns.EMAIL_ADDRESS
VM -> UI : UiState.loading()
VM -> AuthDS : signIn(email, password)
activate AuthDS

AuthDS -> FbAuth : signInWithEmailAndPassword(email, password)
activate FbAuth
alt Sai thông tin đăng nhập / Tài khoản Auth bị vô hiệu hóa
  FbAuth --> AuthDS : FirebaseAuthInvalidCredentialsException
  AuthDS --> VM : AppResult.error("INVALID_CREDENTIALS")
  VM --> UI : UiState.success(formReady) + Snackbar báo lỗi
else Xác thực mật khẩu hợp lệ
  FbAuth --> AuthDS : AuthResult (FirebaseUser)
  AuthDS -> FbAuth : user.getIdToken(forceRefresh = true)
  FbAuth --> AuthDS : GetTokenResult (JWT chứa claims.role)
  deactivate FbAuth

  AuthDS -> SessMgr : updateCachedRole(role) [roleClaimLoaded = true + postValue(role)]
  AuthDS --> VM : AppResult.success(UserRole)
  deactivate AuthDS

  VM -> FS : getUser(uid) đọc users/{uid}
  activate FS
  FS --> VM : DocumentSnapshot (status, fullName, ekyc)
  deactivate FS

  alt users/{uid}.status == "LOCKED"
    VM -> SessMgr : signOut()
    VM --> UI : Snackbar "Tài khoản đã bị khóa bởi ngân hàng"
  else users/{uid}.status == "ACTIVE"
    VM -> SessMgr : startUserSession()
    activate SessMgr
    SessMgr -> FS : addSnapshotListener("users/{uid}")
    deactivate SessMgr

    VM -> SessMgr : consumePendingDeepLinkIfAllowed(role, uid)
    alt role == UserRole.OFFICER
      VM --> UI : navigateDeepLink("novapay://officer")
      UI -> UI : MainActivity.enforceDestinationGuard()\nẨn BottomNav Customer -> Mở OfficerDashboardFragment
    else role == UserRole.CUSTOMER
      VM --> UI : navigateDeepLink(pendingUri ?: "novapay://home")
      UI -> UI : MainActivity.enforceDestinationGuard()\nHiện BottomNav 4 Tab -> Mở CustomerHomeFragment
    end
  end
end
deactivate VM
deactivate UI

== Kịch bản Bảo mật Realtime: Nhân viên Ngân hàng Khóa Tài khoản từ Xa ==

FS ->> SessMgr : Snapshot Event: users/{uid}.status == "LOCKED"
activate SessMgr
SessMgr -> FbAuth : firebaseAuth.signOut()
SessMgr -> SessMgr : stopUserListener() & reset cachedRole
SessMgr ->> UI : accountLockedEvent.postValue(true)
deactivate SessMgr
activate UI
UI -> UI : redirectToLogin() [popUpTo(nav_graph, inclusive=true)]
UI --> UserActor : Hiển thị LoginFragment + Snackbar cảnh báo khóa tài khoản
deactivate UI

@enduml
```

---

## 2. Luồng 2: Chuyển Tiền Giá Trị Cao (`> 5.000.000 ₫`) — Xác Thực 3 Lớp (`FaceCheck` Siết Chặt + `Biometric` + `OTP Brevo`)

### Điểm nhấn kiến trúc ăn điểm:
- **Trì hoãn phát hành OTP thông minh:** Khi số tiền chi/rút `> 5.000.000 ₫`, `POST /transactions/initiate` tạo bản nháp ở trạng thái `"PENDING_FACE"` với `otpExpiresAt: null` (**chưa gửi Email OTP**), giúp khách hàng không bị trôi mất thời gian 180 giây trong lúc cân chỉnh khuôn mặt trước camera và tránh tốn hạn mức gửi mail nếu khuôn mặt không khớp.
- **Đối chiếu khuôn mặt nghiêm ngặt (Zero-False-Acceptance & Fail-Closed):** Trước khi gọi AI, Server kiểm tra `ekyc_images/{uid}`; nếu trống hoặc chỉ là ảnh mẫu 1x1 pixel (`SAMPLE_SEED_PLACEHOLDER_BASE64`), Server từ chối ngay bằng `403 EKYC_FACE_NOT_FOUND`. Trên Android, CameraX + ML Kit kiểm tra Liveness (1 khuôn mặt, mở cả 2 mắt `>= 0.7`, góc nghiêng `<= 15°`), nén JPEG `< 200KB` gửi lên Backend để **Gemini 2.5 Flash** (`temperature: 0.0`) thẩm định cấu trúc khuôn mặt: chỉ đạt khi `matched == true && confidence >= 0.80`; sai người trả `403 FACE_MISMATCH` kèm cảnh báo đỏ (tối đa 5 lần thử); nếu Gemini quá tải (HTTP 429) hoặc timeout (`> 15s`), áp dụng **Fail-Closed (`503 SERVICE_UNAVAILABLE`)** — tuyệt đối không tự cho qua để đảm bảo an toàn sinh trắc học.
- **Giao dịch nguyên tử 2 pha (Two-Phase Firestore ACID Transaction):** Toàn bộ lệnh `tx.get(...)` được gom vào Pha 1 (kiểm tra lại số dư, trạng thái `ACTIVE`, chủ tài khoản nhận không bị `LOCKED`, hạn mức ngày theo múi giờ `Asia/Ho_Chi_Minh`) trước khi thực thi Pha 2 ghi đồng thời tài liệu kế toán và bản ghi **thông báo biến động số dư (`notifications`)** hai chiều.

```plantuml
@startuml NovaPay_Seq_02_HighValue_3Layer_Transfer
skinparam backgroundColor #FFFFFF
skinparam shadowing false
skinparam roundcorner 8
skinparam defaultFontName "Segoe UI"
skinparam defaultFontSize 10
skinparam ArrowColor #334155
skinparam SequenceLifeLineBorderColor #64748B
skinparam ParticipantBorderColor #065F46
skinparam ParticipantBackgroundColor #ECFDF5
skinparam DatabaseBorderColor #0F4C81
skinparam DatabaseBackgroundColor #EFF6FF

autonumber
title Luồng 2: Chuyển Tiền > 5.000.000 ₫ (FaceCheck Zero-False-Acceptance + BiometricPrompt + Brevo OTP 180s)

actor "Khách hàng\n(VERIFIED)" as Cust
participant "TransferFragment &\nOtpVerificationDialog" as TxUI
participant "FaceCheckDialog &\nCameraX + ML Kit" as FaceUI
participant "BiometricHelper\n(AndroidX)" as Bio
participant "NovaPay Server\n(/transactions/*)" as Server
participant "Gemini 2.5 Flash\n(Multimodal AI)" as Gemini
participant "Brevo API\n(Email OTP)" as Brevo
database "Cloud Firestore" as FS

== Bước 0: Tra cứu người nhận nội bộ an toàn (Không lộ số dư / CCCD) ==
Cust -> TxUI : Nhập số TK nhận & số tiền 6.000.000 ₫
TxUI -> Server : GET /accounts/lookup?accountNumber=1001000002
activate Server
Server -> FS : Query accounts (CHECKING, ACTIVE) & users/{ownerUid} (!LOCKED)
Server --> TxUI : 200 {accountId, accountNumber, ownerName}
deactivate Server

== Lớp bảo mật 1/3: Khởi tạo PENDING_FACE & Đối chiếu khuôn mặt eKYC siết chặt ==
Cust -> TxUI : Bấm "Tiếp tục xác thực giao dịch"
TxUI -> Server : POST /transactions/initiate {type, fromAccountId, toAccountId, amount: 6000000}
activate Server
Server -> FS : Kiểm tra users(ACTIVE, VERIFIED), accounts(balance, perTxnLimit, dailyLimit UTC+7)
Server -> FS : Tạo transaction_drafts/{draftId}\n{status: "PENDING_FACE", otpExpiresAt: null}
Server --> TxUI : 200 {draftId, requiresFace: true, requiresBiometric: true, otpExpiresAt: null}
deactivate Server

TxUI -> FaceUI : Mở novapay://face-check?draftId={draftId}
activate FaceUI
FaceUI -> FaceUI : FaceLivenessEvaluator: 1 mặt, 2 mắt >= 0.7f, |EulerY/Z| <= 15°\nImageCompressor nén JPEG < 200KB -> Base64
Cust -> FaceUI : Bấm "Xác minh khuôn mặt"
FaceUI -> Server : POST /transactions/face-check {draftId, imageBase64}
activate Server
Server -> FS : Đọc draft (PENDING_FACE) & ảnh gốc ekyc_images/{uid}

alt Ảnh eKYC trống hoặc là ảnh mẫu 1x1 (isPlaceholderOrEmptyEkycImage)
  Server --> FaceUI : 403 EKYC_FACE_NOT_FOUND
else Có ảnh eKYC thật hợp lệ
  Server -> Gemini : So sánh 2 ảnh chân dung (temperature=0.0, timeout=15s)
  activate Gemini
  alt Gemini HTTP 429 / Timeout > 15s / Lỗi mạng AI (Fail-Closed)
    Gemini --> Server : HTTP 429 / AbortError
    Server --> FaceUI : 503 SERVICE_UNAVAILABLE (Tuyệt đối không cho qua)
  else Khuôn mặt không khớp (matched == false || confidence < 0.80)
    Gemini --> Server : {matched: false, confidence: 0.32, reason: "..."}
    Server -> FS : Tăng faceAttempts (nếu >= 5 lần -> status = EXPIRED, 429 TOO_MANY_ATTEMPTS)
    Server --> FaceUI : 403 FACE_MISMATCH (Banner đỏ cảnh báo & cho chụp lại)
  else Khớp cùng một người (matched == true && confidence >= 0.80)
    Gemini --> Server : {matched: true, confidence: 0.94}
    deactivate Gemini
    Server -> FS : Ghi face_checks/{draftId} {imageBase64, manualAuditRequired: false}
    Server -> Server : Sinh OTP 6 số, băm SHA-256(draftId:otp)
    Server -> Brevo : POST /v3/smtp/email (Mã OTP hiệu lực 180s)
    activate Brevo
    Brevo --> Server : 201 Sent
    deactivate Brevo
    Server -> FS : Cập nhật draft -> {status: "PENDING_OTP", faceChecked: true, otpHash, otpExpiresAt}
    Server --> FaceUI : 200 {ok: true, otpExpiresAt}
    deactivate Server
    FaceUI --> TxUI : setFragmentResult(REQ_FACE_CHECK, passed=true, otpExpiresAt)
    deactivate FaceUI
  end
end

== Lớp bảo mật 2/3: Sinh trắc học Thiết bị (Vân tay / Khuôn mặt / Mã khóa máy) ==
TxUI -> TxUI : Bắt đầu đếm ngược đồng hồ OTP 180s
TxUI -> Bio : authenticate(BIOMETRIC_STRONG | DEVICE_CREDENTIAL)
activate Bio
Cust -> Bio : Quét vân tay hoặc nhập PIN màn hình khóa
Bio --> TxUI : onAuthSucceeded() -> biometricPassed = true
deactivate Bio

== Lớp bảo mật 3/3: Xác nhận OTP & Hạch toán ACID Transaction ==
Cust -> TxUI : Nhập mã OTP 6 chữ số từ Email -> Bấm "Xác nhận"
TxUI -> Server : POST /transactions/confirm {draftId, otp, biometricPassed: true}
activate Server
Server -> Server : Kiểm tra PENDING_OTP, faceChecked==true, biometricPassed==true,\n!isOtpExpired, timingSafeEqual(otpHash) (khóa sau 5 lần sai)
Server -> FS : runTransaction() — PHA 1: tx.get(draft, user, fromAcc, toAcc, toOwner, spentToday)\nPHA 2: Trừ fromAcc.balance + ghi ledger OUT,\nCộng toAcc.balance + ghi ledger IN,\nGhi transactions/{txId}, gắn txId vào face_checks,\nGhi users/{uidA, uidB}/notifications (OUT & IN) & khóa draft CONFIRMED
activate FS
FS --> Server : Commit nguyên tử thành công (txId)
deactivate FS
Server --> TxUI : 200 {txId, status: "SUCCESS", clientSecret: null}
deactivate Server

TxUI --> Cust : Điều hướng sang ReceiptFragment (Xuất & chia sẻ biên lai PNG / PDF)

@enduml
```

---

## 3. Luồng 3: Nạp Tiền Quốc Tế Qua Stripe `PaymentSheet` & Chốt Chặn Idempotency

### Điểm nhấn kiến trúc ăn điểm:
- **Không cộng tiền sớm:** Tại `POST /transactions/confirm`, dù người dùng đã nhập đúng mã OTP 2FA, Server **tuyệt đối chưa cộng số dư**, mà chỉ khởi tạo `Stripe PaymentIntent` (tiền tệ `vnd` không nhân 100 — zero-decimal currency, kèm `idempotencyKey: novapay_draft_{draftId}`), chuyển bản nháp sang `"PENDING_PAYMENT"` và trả `clientSecret` cho Android mở `PaymentSheet`.
- **Chống cộng tiền đúp (Idempotent Finalization):** Tại `POST /payments/finalize`, Server truy vấn ngược trực tiếp lên máy chủ Stripe (`paymentIntents.retrieve`) để xác minh `status === "succeeded"` và đối chiếu đúng số tiền VNĐ trước khi mở Firestore Transaction kiểm tra khóa `draft.status !== "CONFIRMED"`. Mọi thao tác bấm thử lại khi mạng chập chờn đều trả về cùng một `txId` mà không bao giờ cộng tiền hai lần.

```plantuml
@startuml NovaPay_Seq_03_Stripe_Gateway_Idempotent
skinparam backgroundColor #FFFFFF
skinparam shadowing false
skinparam roundcorner 8
skinparam defaultFontName "Segoe UI"
skinparam defaultFontSize 11
skinparam ArrowColor #334155
skinparam SequenceLifeLineBorderColor #64748B
skinparam ParticipantBorderColor #1E3A8A
skinparam ParticipantBackgroundColor #EFF6FF
skinparam DatabaseBorderColor #047857
skinparam DatabaseBackgroundColor #ECFDF5

autonumber
title Luồng 3: Nạp Tiền Trực Tuyến Qua Cổng Stripe PaymentSheet (2-Phase & Idempotent Finalize)

actor "Khách hàng" as Cust
participant "DepositWithdrawFragment &\nOtpVerificationDialog" as AppUI
participant "StripeGatewayHelper\n(Stripe Android SDK)" as Sheet
participant "NovaPay Server\n(/transactions & /payments)" as Server
participant "Brevo API\n(Email OTP)" as Brevo
participant "Stripe Cloud API\n(PaymentIntents)" as StripeAPI
database "Cloud Firestore" as FS

Cust -> AppUI : Chọn Tab "Nạp trực tuyến", nhập số tiền (VD: 2.000.000 ₫)
AppUI -> Server : POST /transactions/initiate {type: "GATEWAY", fromAccountId, amount: 2000000}
activate Server
note right of Server
  Giao dịch GATEWAY nạp tiền vào CHECKING:
  Bắt buộc OTP 2FA, miễn FaceCheck & Biometric kể cả > 5M
end note
Server -> FS : Tạo transaction_drafts/{draftId} {status: "PENDING_OTP", otpHash, otpExpiresAt}
Server -> Brevo : Gửi mã OTP 6 số (hiệu lực 180s)
Server --> AppUI : 200 {draftId, requiresFace: false, requiresBiometric: false, otpExpiresAt}
deactivate Server

Cust -> AppUI : Nhập mã OTP 6 chữ số vào OtpVerificationDialog
AppUI -> Server : POST /transactions/confirm {draftId, otp, biometricPassed: false}
activate Server
Server -> Server : Xác thực SHA-256 OTP hợp lệ & còn hạn
Server -> StripeAPI : paymentIntents.create({amount: 2000000, currency: "vnd"},\n{idempotencyKey: "novapay_draft_" + draftId})
activate StripeAPI
StripeAPI --> Server : PaymentIntent {id: "pi_...", client_secret: "pi_..._secret_..."}
deactivate StripeAPI
Server -> FS : Cập nhật draft -> {status: "PENDING_PAYMENT", stripeIntentId: "pi_..."}\n(CHƯA cộng tiền vào accounts!)
Server --> AppUI : 200 {txId: null, status: "PENDING_PAYMENT", clientSecret}
deactivate Server

AppUI -> Sheet : presentPaymentSheet(STRIPE_PUBLISHABLE_KEY, clientSecret)
activate Sheet
Cust -> Sheet : Nhập thẻ quốc tế kiểm thử (4242...) & Xác nhận thanh toán
Sheet -> StripeAPI : Xác thực thẻ PCI-DSS trực tiếp tới Stripe
activate StripeAPI
StripeAPI --> Sheet : PaymentSheetResult.Completed
deactivate StripeAPI
Sheet --> AppUI : onPaymentCompleted(draftId)
deactivate Sheet

AppUI -> Server : POST /payments/finalize {draftId}
activate Server
Server -> FS : Đọc transaction_drafts/{draftId}
alt Gọi lặp lại khi mạng chập chờn (draft.status == "CONFIRMED")
  Server --> AppUI : 200 {txId: draft.txId, status: "CONFIRMED"} (Không cộng tiền lần 2)
else Lần chốt thanh toán đầu tiên (draft.status == "PENDING_PAYMENT")
  Server -> StripeAPI : paymentIntents.retrieve(stripeIntentId)
  activate StripeAPI
  StripeAPI --> Server : {status: "succeeded", amount: 2000000, currency: "vnd"}
  deactivate StripeAPI
  Server -> Server : Đối chiếu status == "succeeded" & khớp đúng amountVnd
  Server -> FS : runTransaction():\n1. Kiểm tra lại freshDraft.status == "PENDING_PAYMENT", user.status == "ACTIVE" & acc.ownerUid == uid\n2. Cộng accounts/{fromAccountId}.balance += 2.000.000 ₫\n3. Ghi ledger/{entryId} (direction: "IN", balanceAfter)\n4. Ghi transactions/{txId} (type: "GATEWAY", gatewayRef: stripeIntentId)\n5. Khóa draft -> {status: "CONFIRMED", txId}
  activate FS
  FS --> Server : Commit thành công
  deactivate FS
  Server --> AppUI : 200 {txId, status: "CONFIRMED"}
end
deactivate Server

AppUI --> Cust : Mở ReceiptFragment hiển thị biên lai nạp tiền thành công

@enduml
```

---

## 4. Luồng 4: Trợ Lý Ảo Tài Chính Gemini Điều Hướng DeepLink `novapay://` & Cơ Chế Dự Phòng NLP

### Điểm nhấn kiến trúc ăn điểm:
- **Bảo mật khóa API tuyệt đối & Cá nhân hóa theo ngữ cảnh thực:** Android không giữ `GEMINI_API_KEY`. Khi gọi `POST /ai/chat`, Backend tự động tổng hợp ngữ cảnh tài chính thời gian thực của chính khách hàng đó (`checkingBalance`, `monthlyInflow`, `monthlyOutflow` tính theo đầu tháng `Asia/Ho_Chi_Minh`) và nạp 6 tin nhắn gần nhất vào `Gemini 2.5 Flash`.
- **Chống Prompt Injection qua Whitelist DeepLink:** Phản hồi JSON của AI được lọc qua `parseAndSanitizeAiResponse`, chỉ giữ lại tối đa 3 nút hành động có URI nằm trong tập hợp cho phép `ALLOWED_CHAT_DEEPLINKS` (`novapay://home`, `novapay://transfer`, `novapay://bills`, `novapay://topup`, `novapay://services`, `novapay://transactions?range=month`, `novapay://branches`, `novapay://profile`, `novapay://ekyc`).
- **Độ sẵn sàng 100% khi chấm thi:** Nếu Gemini API hết hạn mức miễn phí (HTTP 429) hoặc phản hồi chậm quá 8 giây (`AbortController`), hệ thống tự động chuyển mượt sang bộ phân tích ý định Tiếng Việt có dấu/không dấu (`buildRuleBasedReply`), trả lời chính xác số dư thực tế kèm đúng nút điều hướng DeepLink.

```plantuml
@startuml NovaPay_Seq_04_AI_Chatbot_DeepLink
skinparam backgroundColor #FFFFFF
skinparam shadowing false
skinparam roundcorner 8
skinparam defaultFontName "Segoe UI"
skinparam defaultFontSize 11
skinparam ArrowColor #334155
skinparam SequenceLifeLineBorderColor #64748B
skinparam ParticipantBorderColor #5B21B6
skinparam ParticipantBackgroundColor #F5F3FF
skinparam DatabaseBorderColor #047857
skinparam DatabaseBackgroundColor #ECFDF5

autonumber
title Luồng 4: Trợ Lý Ảo AI Chatbot (Gemini 2.5 Flash + Rule-based NLP Fallback + DeepLink Navigation)

actor "Khách hàng" as Cust
participant "ChatbotFragment &\nChatMessageAdapter" as ChatUI
participant "ChatbotViewModel" as ChatVM
participant "MainActivity &\nNavController" as Nav
participant "NovaPay Server\n(POST /ai/chat)" as Server
participant "Google Gemini API\n(gemini-2.5-flash)" as Gemini
database "Cloud Firestore" as FS

Cust -> ChatUI : Mở Trợ lý AI -> Gắn SnapshotListener chat_sessions/{uid}/messages
Cust -> ChatUI : Nhập "Tháng này tôi tiêu bao nhiêu rồi, cho tôi đóng tiền điện"
ChatUI -> ChatVM : sendMessage(text)
activate ChatVM
ChatVM -> ChatUI : sendingMessage = true (Hiện thanh tiến trình)
ChatVM -> Server : POST /ai/chat {message} (Header: Bearer <ID_Token>)
activate Server

Server -> Server : validateChatMessageInput (1..2000 ký tự)
Server -> FS : getCustomerFinancialContext(uid):\nĐọc số dư CHECKING & tổng thu/chi ledger từ 01/MM 00:00 UTC+7
activate FS
FS --> Server : {checkingBalance, monthlyInflow, monthlyOutflow}
deactivate FS

Server -> FS : Đọc 6 tin nhắn gần nhất làm ngữ cảnh hội thoại
Server -> Gemini : generateContent(systemPrompt + financialContext + history, timeout=8s)
activate Gemini

alt Gemini phản hồi JSON hợp lệ trong 8 giây
  Gemini --> Server : {"reply": "...", "actions": [{"label": "Thanh toán hóa đơn", "deepLink": "novapay://bills"}, ...]}
  Server -> Server : parseAndSanitizeAiResponse()\nKiểm tra đối chiếu ALLOWED_CHAT_DEEPLINKS (tối đa 3 actions)
else Gemini gặp lỗi HTTP 429 (Quota) / Timeout > 8s / Mất khóa
  Gemini --> Server : HTTP 429 / AbortError
  deactivate Gemini
  Server -> Server : buildRuleBasedReply(message, context)\nChuẩn hóa Tiếng Việt -> Trả câu trả lời kèm số dư thực & DeepLink chuẩn
end

Server -> FS : WriteBatch.commit() ghi đồng thời 2 bản ghi:\n1. USER message (createdAt = nowMs)\n2. BOT message + actions[] (createdAt = nowMs + 1)
activate FS
FS -->> ChatUI : SnapshotListener đẩy tin nhắn USER & BOT mới tức thì
deactivate FS

Server --> ChatVM : 200 {reply, actions: [{label, deepLink}]}
deactivate Server
ChatVM -> ChatUI : sendingMessage = false & xóa ô nhập liệu
deactivate ChatVM

ChatUI --> Cust : Hiển thị bong bóng trả lời của Bot kèm Chip hành động "Thanh toán hóa đơn"
Cust -> ChatUI : Chạm vào nút hành động [Thanh toán hóa đơn] (novapay://bills)
ChatUI -> Nav : navigateDeepLink("novapay://bills")
activate Nav
Nav -> Nav : enforceDestinationGuard(): kiểm tra đã đăng nhập & role == CUSTOMER
Nav --> Cust : Điều hướng sang UtilityBillFragment (nav_money) không phụ thuộc R.id chéo!
deactivate Nav

@enduml
```

---

## 5. Luồng 5: Thông Báo Biến Động Số Dư Thời Gian Thực 2 Chiều & System Push Notification

### Điểm nhấn kiến trúc ăn điểm:
- **Phát sinh nguyên tử ngay trong giao dịch kế toán:** Mọi lệnh chuyển tiền, thanh toán, nạp Stripe, mở/tất toán tiết kiệm hay duyệt eKYC đều gọi `NotificationsService.enqueueTransactionNotifications()` (hoặc `enqueueUserNotification()`) **bên trong cùng `db.runTransaction()`**. Nhờ vậy không bao giờ xảy ra tình trạng tiền đã trừ/cộng nhưng mất thông báo biến động số dư.
- **Chống nổ thông báo lịch sử khi mở ứng dụng (`BalanceAlertSubscriber`):** Khi khách hàng đăng nhập, `SessionManager` kích hoạt `BalanceAlertSubscriber.start(uid)` gắn `SnapshotListener` vào `users/{uid}/notifications`. Ở lần đồng bộ đầu tiên (`baselinePrimed == false`), bộ lắng nghe chỉ ghi nhận toàn bộ `notifId` đã có vào tập `seenIds` rồi bật `baselinePrimed = true`. Từ các sự kiện tiếp theo, mọi thông báo mới chưa đọc (`!isRead()`) mới được kích hoạt qua `SystemNotificationHelper` (Notification Channel mức `IMPORTANCE_HIGH` có thể hiện banner ngay cả khi đang ở màn hình khác).
- **Đánh dấu đã đọc an toàn bởi Security Rules:** Khi người dùng chạm vào thông báo hệ thống hoặc một mục trong `NotificationCenterFragment`, ứng dụng gọi `MarkNotificationReadUseCase` (`update({ read: true })` được bảo vệ bởi `isOwner(uid) && isActiveUserDoc(uid)`) và điều hướng thẳng tới `ReceiptFragment` thông qua DeepLink `novapay://receipt/{txId}`.

```plantuml
@startuml NovaPay_Seq_05_Realtime_Balance_Notification
skinparam backgroundColor #FFFFFF
skinparam shadowing false
skinparam roundcorner 8
skinparam defaultFontName "Segoe UI"
skinparam defaultFontSize 10
skinparam ArrowColor #334155
skinparam SequenceLifeLineBorderColor #64748B
skinparam ParticipantBorderColor #0F4C81
skinparam ParticipantBackgroundColor #EFF6FF
skinparam DatabaseBorderColor #047857
skinparam DatabaseBackgroundColor #ECFDF5

autonumber
title Luồng 5: Thông Báo Biến Động Số Dư Thời Gian Thực 2 Chiều & System Push Notification

actor "Khách hàng A\n(Người gửi)" as Sender
participant "NovaPay Server\n(/transactions/confirm)" as Server
participant "NotificationsService\n(notifications.ts)" as NotifSvc
database "Cloud Firestore\n(users/*/notifications)" as FS
participant "BalanceAlertSubscriber &\nSystemNotificationHelper\n(Thiết bị A & B)" as AlertSub
participant "CustomerHomeViewModel &\nNotificationCenterFragment" as NotifUI
actor "Khách hàng B\n(Người nhận)" as Recipient

== Pha 1: Khởi tạo kết nối lắng nghe thông báo ngay khi đăng nhập ==
AlertSub -> FS : SnapshotListener users/{uidB}/notifications (ORDER BY createdAt DESC)
activate FS
FS --> AlertSub : Lô dữ liệu đầu tiên (lịch sử cũ)
AlertSub -> AlertSub : Lưu vào seenIds & đặt baselinePrimed = true\n(Không bắn lại System Push cho thông báo cũ)
deactivate FS

== Pha 2: Phát sinh thông báo biến động số dư nguyên tử cùng lệnh chuyển tiền ==
Sender -> Server : POST /transactions/confirm (Chuyển 1.500.000 ₫ tới TK B)
activate Server
Server -> FS : db.runTransaction() [PHA 2 - Ghi đồng thời]
activate FS
Server -> NotifSvc : enqueueTransactionNotifications(tx, db, senderUid, recipientUid, ...)
activate NotifSvc
NotifSvc -> FS : tx.set(users/{uidA}/notifications/{n1})\n{category:"BALANCE", direction:"OUT", amount:1500000, balanceAfter, read:false, deepLink:"novapay://receipt/{txId}"}
NotifSvc -> FS : tx.set(users/{uidB}/notifications/{n2})\n{category:"BALANCE", direction:"IN", amount:1500000, balanceAfter, read:false, deepLink:"novapay://receipt/{txId}"}
deactivate NotifSvc
FS --> Server : Commit thành công đồng thời sổ cái & 2 thông báo
deactivate FS
Server --> Sender : 200 {txId, status: "SUCCESS"}
deactivate Server

== Pha 3: Đẩy thông báo nổi (Heads-up) & Cập nhật Huy hiệu (Unread Badge) tức thì ==
FS -->> AlertSub : Snapshot Event tới Thiết bị A (OUT) & Thiết bị B (IN)
activate AlertSub
AlertSub -> AlertSub : Kiểm tra baselinePrimed == true && !seenIds.contains(notifId) && !read
AlertSub -> AlertSub : SystemNotificationHelper.showNotification()\n(NotificationChannel IMPORTANCE_HIGH + PendingIntent novapay://receipt/{txId})
AlertSub -->> Recipient : Hiện thanh thông báo nổi hệ thống: "Nhận tiền chuyển khoản +1.500.000 ₫"
deactivate AlertSub

FS -->> NotifUI : ObserveUnreadNotificationCountUseCase() -> unreadCount++
activate NotifUI
NotifUI --> Recipient : Cập nhật chấm số chưa đọc trên biểu tượng Chuông (CustomerHomeFragment)

== Pha 4: Xem chi tiết biến động số dư & Đánh dấu đã đọc ==
Recipient -> NotifUI : Chạm Thông báo hệ thống hoặc mở Trung tâm Thông báo (novapay://notifications)
NotifUI -> NotifUI : Lọc theo Tab (Tất cả / Biến động / Tiết kiệm / Bảo mật / eKYC)
Recipient -> NotifUI : Chạm vào thông báo "+1.500.000 ₫"
NotifUI -> FS : MarkNotificationReadUseCase(notifId) -> update({read: true})
note right of FS
  firestore.rules kiểm tra:
  isOwner(uid) && isActiveUserDoc(uid)
  && affectedKeys().hasOnly(["read"])
end note
NotifUI -> NotifUI : NavController.navigate("novapay://receipt/{txId}")
NotifUI --> Recipient : Mở ReceiptFragment hiển thị biên lai giao dịch tức thì
deactivate NotifUI

@enduml
```
