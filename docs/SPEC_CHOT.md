# SPEC_CHOT.md — Đặc Tả Kỹ Thuật & Quyết Định Kiến Trúc Chốt Cuối Cùng (NovaPay)

- **Dự án:** NovaPay — Ứng dụng Ngân hàng Số (Mobile Apps Development - TDTU)
- **Trạng thái:** **ĐÃ CHỐT (FROZEN ARCHITECTURE & BUSINESS SPEC)**
- **Phạm vi áp dụng:** Bắt buộc đối với cả 3 thành viên (**A**, **B**, **C**), Backend Node.js/TypeScript và toàn bộ AI Agents tham gia viết mã.
- **Tài liệu tham chiếu gốc:** `GEMINI.md`, `docs/DE_BAI.md`, `docs/MARKING.md`, `docs/PHAN_TICH.md`.

---

## Phần 1. Chuẩn Hóa Luồng Giao Dịch Android (Java 17) ↔ Server Node.js

### Quyết định 1: Điều phối xác thực 3 lớp (> 5.000.000 VNĐ), OTP 180s & Sinh trắc học
1. **Phân tách thời điểm gửi Email OTP theo ngưỡng giao dịch:**
   - **Giao dịch `<= 5.000.000 VNĐ` (hoặc giao dịch `GATEWAY` nạp tiền):**
     - `POST /transactions/initiate` kiểm tra điều kiện hợp lệ, tạo `transaction_drafts/{draftId}` ở trạng thái `"PENDING_OTP"`, phát sinh mã OTP 6 số (lưu SHA-256 hash), gửi Email qua Brevo HTTPS ngay lập tức và trả về `{ draftId, requiresBiometric: false, requiresFace: false, otpExpiresAt }`.
   - **Giao dịch chi/rút tiền `> 5.000.000 VNĐ` (`TRANSFER_INTERNAL`, `TRANSFER_INTERBANK`, `WITHDRAW`, `BILL`, `TOPUP`, `SERVICE_PAYMENT`, `MORTGAGE_PAYMENT`):**
     - `POST /transactions/initiate` kiểm tra toàn bộ điều kiện (số dư, hạn mức, eKYC), tạo `draft` ở trạng thái `"PENDING_FACE"` với `otpExpiresAt: null` (**chưa gửi Email OTP** nhằm bảo toàn trọn vẹn 180s đếm ngược cho khách hàng và tránh lãng phí quota Brevo nếu quét mặt thất bại).
     - Chỉ ngay sau khi `POST /transactions/face-check` xác minh khuôn mặt hợp lệ, Server mới chuyển `draft` sang `"PENDING_OTP"`, gửi Email OTP và trả về `{ ok: true, otpExpiresAt }`.
2. **Thứ tự thực thi trên Android đối với đơn `> 5.000.000 VNĐ`:**
   - Gọi `/transactions/initiate` → Mở `FaceCheckDialogFragment` (gọi `/transactions/face-check` nhận `otpExpiresAt`) → Kích hoạt `BiometricHelper` xác thực thiết bị → Mở `OtpVerificationDialogFragment` → Gọi `/transactions/confirm`.
3. **Cấu hình `BiometricHelper` chống kẹt trên máy ảo / máy không có vân tay:**
   - Bắt buộc cấu hình cờ xác thực: `BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL`.
   - Khi thiết bị chấm thi hoặc máy ảo không có cảm biến vân tay / chưa đăng ký sinh trắc học, hệ điều hành tự động cho phép xác thực bằng **PIN / Pattern / Mật khẩu khóa màn hình** của thiết bị. *(Lưu ý kỹ thuật AndroidX Biometric: không gọi `.setNegativeButtonText(...)` trên `PromptInfo.Builder` khi đã bật `DEVICE_CREDENTIAL`).*

---

### Quyết định 2: Xác minh danh tính `FaceCheck` thực chất (ML Kit Liveness + Gemini Multimodal)
1. **Kiểm tra Liveness thời gian thực trên Android (`CameraX` + `ML Kit FaceDetector`):**
   - Cấu hình `FaceDetectorOptions`: `PERFORMANCE_MODE_ACCURATE` và `CLASSIFICATION_MODE_ALL`.
   - Chỉ cho phép chụp ảnh khi khung hình có **đúng 1 khuôn mặt duy nhất**, mở cả hai mắt (`leftEyeOpenProbability >= 0.7f` và `rightEyeOpenProbability >= 0.7f`), và nhìn thẳng chính diện (`Math.abs(headEulerAngleY) <= 15f` và `Math.abs(headEulerAngleZ) <= 15f`).
   - Nén ảnh JPEG dung lượng `< 200KB` bằng `ImageCompressor` rồi mã hóa Base64 gửi lên Server.
2. **Đối chiếu khuôn mặt với hồ sơ eKYC gốc trên Server (`POST /transactions/face-check`):**
   - Server đọc ảnh chuẩn đã được duyệt từ `ekyc_images/{uid}` (bắt buộc `users/{uid}.ekyc.status == "VERIFIED"`).
   - Gọi **Gemini Flash Multimodal** trên Server để so sánh ảnh `ekyc_images/{uid}` và ảnh vừa chụp, ép kiểu Structured JSON `{ "samePerson": boolean, "confidence": number }`:
     - **Khớp danh tính (`samePerson == true` và `confidence >= 0.75`):** Lưu vào `face_checks/{draftId}` (`manualAuditRequired: false`), gán `draft.faceChecked = true`, phát hành OTP Email và trả `{ ok: true, otpExpiresAt }`.
     - **Sai danh tính (`samePerson == false` hoặc `confidence < 0.75`):** Từ chối giao dịch ngay lập tức với mã lỗi chuẩn `FACE_MISMATCH` (HTTP 403).
     - **Cơ chế Fallback bảo vệ Demo:** Nếu Gemini API gặp lỗi hết quota (HTTP 429) hoặc timeout (`> 8s`), Server cho phép vượt qua bước AI, lưu `face_checks/{draftId}` với cờ `manualAuditRequired: true` để Bank Officer hậu kiểm trên màn hình quản trị, không làm đứt luồng chấm thi.

---

### Quyết định 3: Tách bạch luồng `GATEWAY` (Stripe), Idempotency & `WITHDRAW`
1. **Luồng Nạp tiền qua cổng thanh toán Stripe (`type = "GATEWAY"`):**
   - Chỉ áp dụng `GATEWAY` cho nghiệp vụ **Nạp tiền vào tài khoản `CHECKING`**. Vẫn bắt buộc xác thực OTP 2FA theo đúng đề bài nhưng **miễn `FaceCheck` & `BiometricPrompt`** kể cả khi nạp `> 5.000.000 VNĐ`.
   - Tại `POST /transactions/confirm`: Sau khi OTP hợp lệ, Server gọi `stripe.paymentIntents.create(...)`, lưu `stripeIntentId` vào `draft`, chuyển `draft.status = "PENDING_PAYMENT"` và trả về `{ txId: null, status: "PENDING_PAYMENT", clientSecret }` (**TUYỆT ĐỐI chưa cộng tiền vào `accounts` và chưa tạo bản ghi `transactions`/`ledger`**).
   - Trên Android: Hiển thị `PaymentSheet` của Stripe SDK bằng `clientSecret`. Khi nhận kết quả `PaymentSheetResult.Completed`, gọi `POST /payments/finalize` truyền `{ draftId }`.
   - Tại `POST /payments/finalize` (**Khóa Idempotent chống cộng tiền đúp**):
     - Server gọi `stripe.paymentIntents.retrieve(draft.stripeIntentId)` xác nhận `status === "succeeded"`.
     - Thực thi `db.runTransaction(...)` trên Firestore:
       - Nếu đọc thấy `draft.status === "CONFIRMED"` → Trả ngay `{ txId: draft.txId, status: "SUCCESS" }` mà không cộng tiền lần hai (an toàn tuyệt đối khi rớt mạng hoặc người dùng bấm Retry).
       - Nếu `draft.status === "PENDING_PAYMENT"` → Cộng `balance` vào `accounts/{fromAccountId}`, ghi `transactions/{txId}`, ghi bút toán `IN` vào `ledger`, và cập nhật `draft.status = "CONFIRMED"`, `draft.txId = txId`.
2. **Luồng Rút tiền (`type = "WITHDRAW"`):**
   - Thực hiện trên tài khoản nguồn `CHECKING`, áp dụng kiểm tra số dư, hạn mức ngày, xác thực OTP (và đủ 3 lớp nếu `> 5.000.000 VNĐ`), trừ tiền nguyên tử ngay tại `/transactions/confirm` (mô phỏng rút tiền về tài khoản/thẻ liên kết ngoài).

---

## Phần 2. Quy Tắc Tích Hợp Giữa 3 Phân Hệ A – B – C

### Quyết định 4: Điều hướng Sub-Graphs, `AccountRouterFragment` & DeepLink Guard
1. **Cấm phụ thuộc trực tiếp `R.id` chéo giữa A, B, C:**
   - Thành viên ở sub-graph này **cấm import hoặc gọi trực tiếp `R.id.*`** thuộc sub-graph của thành viên khác (`nav_core.xml`, `nav_money.xml`, `nav_officer_map_ai.xml`).
   - Mọi điều hướng liên phân hệ bắt buộc gọi qua **Internal DeepLink URI**: `NavController.navigate(Uri.parse("novapay://..."))` (hỗ trợ truyền tham số qua query string, ví dụ: `novapay://transfer?type=MORTGAGE_PAYMENT&accountId=...`).
   - Các Dialog dùng chung (`FaceCheckDialogFragment` của A, `OtpVerificationDialogFragment` của B) giao tiếp kết quả thông qua **`FragmentResultListener`** với bộ khóa hằng số cố định trong `Constants.java`.
2. **Bộ định tuyến tài khoản `AccountRouterFragment` (Owner: A):**
   - URI `novapay://accounts/{accountId}` gắn duy nhất vào `AccountRouterFragment` trong `nav_core.xml`.
   - Khi mở, `AccountRouterFragment` đọc `type` của `accountId` và tự động điều hướng kèm `popUpTo(R.id.accountRouterFragment, true)` sang đúng `CheckingDetailFragment`, `SavingDetailFragment` hoặc `MortgageDetailFragment`.
3. **DeepLink Auth & Role Guard tại `MainActivity` (Owner: A):**
   - Mọi DeepLink đi vào `MainActivity` đều phải qua chốt kiểm tra của `SessionManager`:
     - Nếu chưa đăng nhập: lưu tạm `pendingDeepLink` và ép về `LoginFragment` (sau khi login thành công và đúng vai trò mới điều hướng tiếp).
     - Nếu đang đăng nhập với `role == "OFFICER"`: chặn toàn bộ DeepLink dành riêng cho `CUSTOMER` (`novapay://transfer`, `novapay://bills`, `novapay://topup`, `novapay://services`...) và ngược lại.

---

### Quyết định 5: Khóa chặt `firestore.rules`, API Tra cứu Người nhận & Cập nhật Nguyên tử
1. **Nguyên tắc phân quyền `firestore.rules` (Owner: A):**
   - `users/{uid}` (cùng `beneficiaries`) và `accounts/{accountId}` (cùng `ledger`): **Chỉ chính chủ (`uid == request.auth.uid` / `ownerUid == request.auth.uid`) hoặc `request.auth.token.role == "OFFICER"` mới có quyền đọc.** Cấm mở quyền đọc đại trà cho mọi user đăng nhập để bảo vệ tuyệt đối số dư và CCCD.
   - Các dữ liệu danh mục chung (`banks`, `branches`, `interest_rates`, `service_catalog`): cho phép đọc khi `request.auth != null`; cấm hoàn toàn quyền ghi từ Client SDK.
   - `utility_bills`: cho phép đọc khi `request.auth != null` để tra cứu theo `customerCode`; **cấm Client SDK tự cập nhật `status`**.
   - `orders`: chỉ chủ đơn hàng (`uid == request.auth.uid`) hoặc `OFFICER` được đọc; **cấm Client SDK tự ghi**.
   - `transactions`, `transaction_drafts`, `face_checks`, `ekyc_images`, `audit_logs`: **100% chỉ Backend Server (Firebase Admin SDK) mới có quyền ghi.**
2. **API Tra cứu tên người nhận nội bộ an toàn (`GET /accounts/lookup` — Owner: B):**
   - Endpoint: `GET /accounts/lookup?accountNumber=<số_TK>`
   - Chỉ tra cứu trên các tài khoản có `type == "CHECKING"` và `status == "ACTIVE"`, trả về tối giản `{ accountId, accountNumber, ownerName }` (không bao giờ trả số dư, email, SĐT hay CCCD).
3. **Cập nhật nguyên tử Hóa đơn & Đơn hàng dịch vụ tại `/transactions/confirm`:**
   - Khi `type == "BILL"` (kèm `meta.billId` / `meta.billCode`): bên trong chính Firestore Transaction trừ tiền của `/transactions/confirm`, Server kiểm tra hóa đơn còn `"UNPAID"` rồi chuyển sang `status = "PAID"`.
   - Khi `type == "SERVICE_PAYMENT"` (kèm thông tin mặt hàng trong `meta`): bên trong chính Firestore Transaction của `/transactions/confirm`, Server trừ tiền và tạo bản ghi `orders/{orderId}` (`status = "PAID"`, gắn `txId`).

---

### Quyết định 6: Hilt Qualifiers & Bộ khung "Day-0 Skeleton"
1. **Tách bạch 2 Qualifier `Retrofit` trong `NetworkModule.java`:**
   - `@BackendRetrofit`: Đính kèm `AuthInterceptor` tự động gắn header `Authorization: Bearer <Firebase_ID_Token>`, cấu hình timeout 30s (chống lỗi cold-start trên Render), dùng cho toàn bộ API của NovaPay Server.
   - `@OpenRouteRetrofit`: Không gắn `AuthInterceptor` ngân hàng, trỏ tới `https://api.openrouteservice.org/`, dùng riêng cho tính năng tìm đường đi bộ của Thành viên C.
2. **Chốt cứng "Day-0 Skeleton" trên nhánh `main` trước khi tách nhánh làm việc:**
   - Thành viên A và C phối hợp commit lên `main` bộ khung biên dịch chuẩn trước khi 3 người tách nhánh độc lập, bao gồm:
     - `NovaPayApp.java` (`@HiltAndroidApp`), `MainActivity.java` (`@AndroidEntryPoint`), `AppResult.java`, `UiState.java`, `SingleLiveEvent.java`, `Constants.java`.
     - `BaseFragment<VB extends ViewBinding>` và `StateLayoutHelper.java` cùng 3 layout trạng thái chuẩn.
     - Domain model `Account.java` + interface `AccountRepository.java` (để B lấy danh sách tài khoản nguồn `CHECKING` ngay từ đầu).
     - Bộ 4 file Navigation XML (`nav_graph.xml` và 3 sub-graphs stub hợp lệ) cùng 2 Qualifier `@BackendRetrofit`, `@OpenRouteRetrofit`.

---

## Phần 3. Phòng Thủ Edge-Cases Bảo Toàn Điểm Tối Đa (`MARKING.md` 10/10)

### Quyết định 7: Ràng buộc Nghiệp vụ Tài khoản, Kỳ hạn Vay & Cưỡng bức Đăng xuất Realtime
1. **Ràng buộc nguồn tiền & Chặn giao dịch phi lý:**
   - **Chỉ tài khoản `CHECKING` đang `ACTIVE`** mới được dùng làm `fromAccountId` cho mọi giao dịch thanh toán, chuyển tiền, rút tiền và trả nợ vay. Cấm chọn tài khoản `SAVING` hoặc `MORTGAGE` làm nguồn thanh toán trên cả UI lẫn Server (`/initiate` trả `INVALID_ARGUMENT` nếu vi phạm).
   - Chặn tuyệt đối chuyển tiền nội bộ cho chính tài khoản nguồn (`fromAccountId == toAccountId`).
2. **Nghiệp vụ Thanh toán Khoản vay Thế chấp (`MORTGAGE_PAYMENT`):**
   - Trong `meta` của `/transactions/initiate` truyền `mortgageAccountId` (tài khoản vay cần trả), còn `fromAccountId` là tài khoản `CHECKING` trích tiền, `amount` bằng `installmentAmount` (hoặc bằng `outstandingBalance` nếu dư nợ cuối kỳ nhỏ hơn `installmentAmount`).
   - Tại `/transactions/confirm`, Firestore Transaction đồng thời:
     1. Trừ `amount` khỏi `CHECKING.balance` + ghi `ledger` (`OUT`).
     2. Giảm `MORTGAGE.mortgage.outstandingBalance` đi `amount` + ghi `ledger` của tài khoản vay.
     3. Dời `MORTGAGE.mortgage.nextDueDate`: cộng thêm **1 tháng** nếu `paymentFrequency == "MONTHLY"` hoặc cộng thêm **14 ngày** nếu `paymentFrequency == "BIWEEKLY"`. Nếu `outstandingBalance == 0L`, cập nhật `MORTGAGE.status = "CLOSED"`.
3. **Kiểm tra hạn mức ngày (`dailyLimit`), eKYC & Khóa tài khoản Realtime:**
   - Server kiểm tra tại cả `/initiate` và `/confirm`: `users/{uid}.status == "ACTIVE"`, `users/{uid}.ekyc.status == "VERIFIED"`, `amount <= account.perTxnLimit`, và tổng tiền các giao dịch `OUT` thành công trong ngày hiện tại theo múi giờ **`Asia/Ho_Chi_Minh` (UTC+7)** cộng với `amount` phải `<= account.dailyLimit` (vượt trả `LIMIT_EXCEEDED`).
   - Trên Android: `SessionManager` duy trì `SnapshotListener` realtime tới `users/{uid}` trong suốt phiên đăng nhập; ngay khi phát hiện `status == "LOCKED"`, lập tức gọi `FirebaseAuth.signOut()`, hủy listener và chuyển thẳng về `LoginFragment` kèm thông báo tài khoản đã bị ngân hàng khóa.

---

### Quyết định 8: Chống lỗi Môi trường Bảo vệ Đồ án (GPS Fallback TDTU & Server-side AI Chatbot)
1. **Fallback Vị trí Thông minh tại ĐH Tôn Đức Thắng (`BranchMapFragment` — Owner: C):**
   - Xin quyền `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` với timeout lấy vị trí thực tế **5 giây**.
   - Nếu người dùng từ chối quyền, mất tín hiệu GPS trong phòng kín, **hoặc vị trí lấy được cách cụm chi nhánh TP.HCM `> 100 km`** (ví dụ: máy ảo Android mặc định tại California, Mỹ `37.422, -122.084`), ứng dụng tự động chuyển sang vị trí mặc định tại **Trường Đại học Tôn Đức Thắng (`10.7326, 106.6998` — 19 Nguyễn Hữu Thọ, Quận 7, TP.HCM)** và hiển thị Snackbar thông báo rõ ràng.
   - Nhờ đó, thuật toán tìm chi nhánh gần nhất (Haversine) và API vẽ đường đi bộ `OpenRouteService` luôn hoạt động trơn tru 100% trong mọi kịch bản chấm thi và xét duyệt Google Play (vẫn giữ fallback vẽ đoạn thẳng chim bay nếu mất kết nối Internet tới OpenRouteService).
2. **Kiến trúc AI Chatbot qua Backend (`POST /ai/chat` — Owner: C):**
   - **Tuyệt đối không lưu Gemini API Key trong code Android hay `BuildConfig`** (tránh lộ khóa khi dịch ngược APK và cảnh báo bảo mật trên Google Play).
   - Android gọi `POST /ai/chat` (`{ message }`) lên Server. Server truy vấn nhanh tổng số dư Checking và tổng thu/chi trong tháng hiện tại của `uid` để đưa vào ngữ cảnh System Prompt cho Gemini, ép trả về JSON chuẩn `{ "reply": "...", "actions": [{ "label": "...", "deepLink": "novapay://..." }] }` và lưu cả 2 chiều tin nhắn vào `chat_sessions/{uid}/messages`.
   - Nếu Gemini gặp lỗi 429 (hết hạn mức miễn phí) hoặc mất kết nối, Server tự động kích hoạt **Bộ phân tích từ khóa Tiếng Việt dự phòng (Rule-based NLP Fallback)** để trả về câu trả lời tự nhiên kèm đúng các nút DeepLink (`novapay://bills`, `novapay://topup`, `novapay://transactions?range=month`, `novapay://transfer`, `novapay://branches`), đảm bảo UI không bao giờ báo lỗi thất bại khi demo.

---

### Quyết định 9: Kiểm toán Officer (`audit_logs`), Lãi suất Động & Phát hành Google Play
1. **100% thao tác quản trị của Officer đi qua Server để ghi `audit_logs` bắt buộc:**
   - Bổ sung `PATCH /officer/customers/:uid` (cập nhật thông tin khách hàng, khóa/mở khóa `status`) và `POST /officer/interest-rates` (thêm/cập nhật biểu lãi suất theo `termMonths`) bên cạnh `POST /officer/customers` và `POST /ekyc/review`.
   - Mọi endpoint của Officer đều kiểm tra Custom Claim `role == "OFFICER"` và ghi nhận đồng thời bản ghi vào `audit_logs/{logId}` (`actorUid`, `action`, `targetId`, `details`, `createdAt`).
2. **Đồng bộ Lãi suất Tiết kiệm Realtime (`SavingDetailFragment` — Owner: A):**
   - Kết hợp luồng dữ liệu realtime (`Observable.combineLatest`) giữa tài khoản `accounts/{accountId}` và bảng `interest_rates` khớp theo kỳ hạn `termMonths`, tính lại tiền lãi dự kiến hàng tháng bằng kiểu `long` ngay lập tức mỗi khi Officer cập nhật lãi suất mới.
3. **Chiến lược vượt xét duyệt Google Play Console an toàn:**
   - Ghi rõ trong phần mô tả ứng dụng trên Store Listing, trang *Privacy Policy* (GitHub Pages) và dòng chú thích tại màn hình Đăng nhập: *"NovaPay là ứng dụng mô phỏng nghiệp vụ ngân hàng số phục vụ mục đích học tập & nghiên cứu tại Khoa CNTT - ĐH Tôn Đức Thắng (Academic Sandbox Simulation), không phát sinh lưu ký tiền tệ thực tế."*
   - Khai báo đầy đủ thông tin đăng nhập của 2 tài khoản kiểm thử (`Customer` đã duyệt eKYC sẵn số dư & `Officer`) trong mục **App Content → App Access** trên Google Play Console để đội ngũ Reviewer đăng nhập kiểm tra thuận lợi.
