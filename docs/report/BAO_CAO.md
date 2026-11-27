# BÁO CÁO ĐỒ ÁN CUỐI KỲ — NOVAPAY (ỨNG DỤNG NGÂN HÀNG SỐ)

**Môn học:** Phát triển Ứng dụng Di động (Mobile Apps Development)
**Khoa:** Công nghệ Thông tin — Trường Đại học Tôn Đức Thắng
**Nhóm thực hiện:** (điền họ tên, MSSV, vai trò từng thành viên)
**Giảng viên hướng dẫn:** (điền)

> Cách dùng tệp này: đây là khung báo cáo đã điền sẵn nội dung kỹ thuật lấy từ mã nguồn. Hãy chép vào mẫu báo cáo của Khoa (bảng tiêu chí chấm điểm yêu cầu dùng mẫu của Khoa, có mục lục và tài liệu tham khảo), thay các mục `(điền ...)` và chèn ảnh chụp màn hình. Các sơ đồ nằm trong `docs/diagrams/` (Mermaid, xuất PNG tại https://mermaid.live).

---

## MỤC LỤC

1. Giới thiệu
2. Kiến trúc hệ thống và công nghệ sử dụng
3. Phân tích và thiết kế
4. Hiện thực
5. Giao diện người dùng
6. Kiểm thử
7. Triển khai
8. Phân công công việc
9. Kết luận và hướng phát triển
10. Tài liệu tham khảo

---

## 1. GIỚI THIỆU

### 1.1 Bối cảnh
Ngân hàng số cho phép khách hàng thực hiện giao dịch mọi lúc mọi nơi mà không cần đến quầy hoặc cây ATM. NovaPay là ứng dụng mobile banking mô phỏng, xây dựng theo đề bài môn học, phục vụ hai nhóm người dùng: **khách hàng** (`CUSTOMER`) và **nhân viên ngân hàng** (`OFFICER`).

> NovaPay là ứng dụng mô phỏng học thuật (Academic Sandbox Simulation), mọi số dư và giao dịch không phát sinh lưu ký tiền tệ thực tế.

### 1.2 Phạm vi chức năng theo đề bài

| Yêu cầu đề bài | Hiện thực trong NovaPay |
| :--- | :--- |
| User profiles | Hai phân hệ giao diện riêng, phân quyền bằng Firebase Custom Claim `role`. Officer tạo khách hàng, sửa dữ liệu, khóa/mở khóa. Khách hàng xem và sửa số điện thoại, địa chỉ, ảnh đại diện |
| Security | Đăng nhập Firebase Auth, eKYC bằng ảnh khuôn mặt, FaceCheck cho giao dịch lớn, OTP email 2FA cho mọi giao dịch tài chính, BiometricPrompt |
| Account management | Ba loại tài khoản Checking, Saving, Mortgage với màn hình riêng. Xem lãi suất và tiền lãi tháng, kỳ trả nợ, lịch sử giao dịch, nạp và rút tiền. Officer chỉnh lãi suất |
| Transaction management | Chuyển nội bộ, liên ngân hàng, quét QR, danh bạ thụ hưởng. Server kiểm tra hợp lệ trước khi ghi nhận. Tích hợp Stripe |
| Utilities | Thanh toán điện, nước, nạp điện thoại, đặt vé máy bay, xem phim, khách sạn, mua sắm |
| Navigation | Bản đồ osmdroid, vị trí người dùng, chi nhánh và ATM, gợi ý chi nhánh gần nhất, đường đi bộ qua OpenRouteService |
| AI chatbot | Chatbot qua Gemini, trả lời kèm nút điều hướng (deep link) đến chức năng tương ứng, có bộ phân tích từ khóa dự phòng |

---

## 2. KIẾN TRÚC HỆ THỐNG VÀ CÔNG NGHỆ SỬ DỤNG

Xem sơ đồ: `docs/diagrams/01_kien_truc.md`.

### 2.1 Kiến trúc tổng thể: Hybrid Cloud
- **Android Client (Java 17)** đọc dữ liệu thời gian thực trực tiếp từ Cloud Firestore và gọi Backend cho mọi thao tác ghi nhạy cảm.
- **Backend Node.js + TypeScript + Express + Firebase Admin SDK** xử lý giao dịch nguyên tử, phát hành OTP, gọi Stripe, Gemini, Brevo.
- **Firebase Authentication** quản lý định danh và Custom Claim `role`; **Cloud Firestore** lưu 14 collection và được bảo vệ bởi `firestore.rules`.

### 2.2 Lý do chọn kiến trúc và công nghệ

| Quyết định | Lý do |
| :--- | :--- |
| MVVM + Clean Architecture (`presentation → domain ← data`) | Tách nghiệp vụ khỏi giao diện và hạ tầng, tầng domain là Java thuần nên kiểm thử đơn vị trên JVM không cần thiết bị. Ba thành viên làm song song ít xung đột mã |
| Backend riêng cho thao tác ghi | Client không thể tin cậy. Số dư, hạn mức, OTP, tích hợp Stripe và khóa API chỉ tồn tại trên server, không nằm trong APK |
| Firestore Transaction (ACID) | Trừ tiền, ghi sổ cái, cập nhật hóa đơn, đơn hàng, khoản vay là một đơn vị nguyên tử, không để lệch số dư |
| Firestore Security Rules | Khách chỉ đọc dữ liệu của chính mình. Mọi collection tài chính và kiểm toán cấm ghi từ client |
| Hilt, Retrofit, RxJava 3 | Tiêm phụ thuộc rõ ràng, hai Retrofit tách biệt (`@BackendRetrofit` có token, `@OpenRouteRetrofit` không token), luồng bất đồng bộ nhất quán |
| LiveData + `UiState` + `SingleLiveEvent` | Mọi màn hình xử lý đủ bốn trạng thái LOADING, SUCCESS, EMPTY, ERROR và sự kiện một lần |
| Kiểu `long` cho tiền tệ | Tránh sai số dấu phẩy động |
| Stripe PaymentSheet (Test Mode) | Đáp ứng yêu cầu tích hợp cổng thanh toán, finalize idempotent chống cộng tiền đúp |
| Gemini Flash qua server | Đối chiếu khuôn mặt và chatbot mà không lộ khóa API trong ứng dụng |

### 2.3 Công nghệ

| Lớp | Công nghệ |
| :--- | :--- |
| Android | Java 17, XML + ViewBinding, Material Design 3, Navigation Component (Single-Activity, DeepLink `novapay://`), Hilt, Retrofit 2, OkHttp, RxJava 3, CameraX, ML Kit (Face Detection, Barcode Scanning), AndroidX Biometric, Stripe Android SDK, osmdroid, MPAndroidChart |
| Backend | Node.js 20+, TypeScript, Express, Helmet, Firebase Admin SDK, Stripe SDK |
| Dữ liệu | Firebase Authentication, Cloud Firestore |
| Dịch vụ ngoài | Stripe, Brevo (email OTP), Google Gemini, OpenRouteService |

---

## 3. PHÂN TÍCH VÀ THIẾT KẾ

### 3.1 Use case
Xem `docs/diagrams/02_use_case.md`.

### 3.2 Class diagram
Xem `docs/diagrams/03_class_diagram.md`: domain model, repository và use case, mẫu MVVM, các lớp Backend.

### 3.3 Thiết kế cơ sở dữ liệu
Xem `docs/diagrams/04_er_diagram.md`: sơ đồ ER 14 collection, kiểu dữ liệu, chỉ mục tổng hợp, bảng phân quyền.

### 3.4 Các quy tắc nghiệp vụ chính
- Chỉ tài khoản `CHECKING` đang `ACTIVE` mới là nguồn chi tiêu. Không tự chuyển cho chính tài khoản nguồn.
- Giao dịch chi/rút cần `users.status = ACTIVE` và `ekyc.status = VERIFIED`.
- Giới hạn mỗi lần (`perTxnLimit`) và giới hạn ngày (`dailyLimit`, tính theo múi giờ Asia/Ho_Chi_Minh).
- Trả nợ vay: trừ tiền Checking, giảm dư nợ, dời ngày đến hạn thêm 1 tháng (MONTHLY) hoặc 14 ngày (BIWEEKLY), đóng khoản vay khi dư nợ bằng 0.
- Tiền lãi tiết kiệm dự kiến hàng tháng: `floor(balance x annualRate / 1200)`, tính lại realtime khi Officer đổi lãi suất.

### 3.5 Thiết kế bảo mật giao dịch

| Điều kiện | Các lớp xác thực |
| :--- | :--- |
| Từ 5.000.000 ₫ trở xuống | OTP email 6 số, hiệu lực 180 giây, băm SHA-256, khóa sau 5 lần sai |
| Trên 5.000.000 ₫ (chi/rút) | FaceCheck (liveness bằng ML Kit rồi đối chiếu Gemini), BiometricPrompt (sinh trắc học hoặc PIN thiết bị), rồi OTP |
| Nạp tiền Stripe | OTP, miễn FaceCheck và Biometric, cộng tiền sau khi Stripe xác nhận `succeeded` |

Biện pháp chống lạm dụng ở Backend: tối đa 5 lần sai OTP, tối đa 5 lần sai FaceCheck, gửi lại OTP cách nhau 30 giây và tối đa 5 lần, kiểm tra số tiền và loại tiền của Stripe khi finalize, giới hạn tần suất truy cập theo địa chỉ IP, từ chối JSON sai định dạng bằng mã 400.

Sơ đồ tuần tự: `docs/diagrams/01_kien_truc.md` (mục 3, 4, 5).

---

## 4. HIỆN THỰC

### 4.1 Cấu trúc mã nguồn

```text
NovaPay/
├── app/        Ứng dụng Android
│   └── src/main/java/com/novabank/novapay/
│       ├── core/          AppResult, UiState, SingleLiveEvent, Hilt modules, SessionManager, BiometricHelper, tiện ích
│       ├── domain/        model, repository (interface), usecase (Java thuần)
│       ├── data/          dto, mapper, remote (firebase, server, maps), repository impl
│       └── presentation/  auth, customer (home, account, transfer, payment, utility, branch, chatbot, ...), ekyc, officer
├── server/     Backend Node.js + TypeScript (routes, services, middleware, scripts, test)
├── docs/       Đề bài, tiêu chí, phân tích, sơ đồ, báo cáo
├── firestore.rules, firestore.indexes.json
```

### 4.2 Các kỹ thuật đáng chú ý
- **Phiên đăng nhập:** `SessionManager` lắng nghe realtime `users/{uid}`, khi `status = LOCKED` thì cưỡng bức đăng xuất. Chặn DeepLink theo đăng nhập và vai trò.
- **Giao tiếp giữa các màn hình:** dialog dùng chung (`FaceCheckDialogFragment`, `OtpVerificationDialogFragment`) trả kết quả qua `FragmentResultListener` với khóa khai báo trong `Constants`.
- **Bản đồ:** thời gian chờ GPS 5 giây, nếu mất GPS hoặc cách TP.HCM trên 100 km thì dùng vị trí mặc định ĐH Tôn Đức Thắng. Tìm chi nhánh gần nhất bằng công thức Haversine, đường đi bộ qua OpenRouteService, dự phòng đoạn thẳng khi mất mạng.
- **Chatbot:** Server đưa số dư và thu/chi tháng vào ngữ cảnh Gemini, bắt buộc trả JSON `{reply, actions}`, chỉ chấp nhận deep link trong danh sách cho phép, tự chuyển sang bộ phân tích từ khóa tiếng Việt khi Gemini lỗi hoặc hết hạn mức.
- **Biên lai:** xuất ảnh PNG và tệp PDF, chia sẻ qua FileProvider.
- **Phát hành:** R8 bật cho bản release kèm luật giữ lại các DTO của Gson và giữ tên lớp giao diện (`-keepnames class com.novabank.novapay.presentation.**`) cho chốt chặn phân quyền, tắt sao lưu tự động.

### 4.3 Bảng API Backend

| Endpoint | Chức năng |
| :--- | :--- |
| `GET /health` | Kiểm tra trạng thái server |
| `GET /accounts/lookup` | Tra cứu tên chủ tài khoản nhận nội bộ |
| `POST /transactions/initiate` | Tạo draft giao dịch, kiểm tra điều kiện |
| `POST /transactions/face-check` | Đối chiếu khuôn mặt, phát hành OTP |
| `POST /transactions/resend-otp` | Gửi lại OTP |
| `POST /transactions/confirm` | Xác nhận OTP và ghi giao dịch nguyên tử |
| `POST /payments/finalize` | Hoàn tất nạp tiền Stripe (idempotent) |
| `POST /ekyc/submit`, `POST /ekyc/review` | Gửi và duyệt eKYC |
| `POST /officer/customers`, `PATCH /officer/customers/:uid`, `POST /officer/interest-rates` | Quản trị của Officer, ghi `audit_logs` |
| `POST /ai/chat` | Chatbot |

---

## 5. GIAO DIỆN NGƯỜI DÙNG

Chụp màn hình từng chức năng và đặt vào `docs/screenshots/`. Gợi ý bố cục mỗi mục: ảnh, tên màn hình, mô tả thao tác.

| STT | Màn hình | Tài khoản dùng | Ảnh |
| :--- | :--- | :--- | :--- |
| 1 | Đăng nhập (giao diện Ngân hàng số thương mại) | Bất kỳ | (chèn ảnh) |
| 2 | Trang chủ khách hàng, biểu đồ thu/chi tháng | `customer.verified@novapay.vn` | (chèn ảnh) |
| 3 | Chi tiết tài khoản Checking, lịch sử giao dịch | như trên | (chèn ảnh) |
| 4 | Chi tiết tài khoản Saving, lãi suất và tiền lãi tháng | như trên | (chèn ảnh) |
| 5 | Chi tiết khoản vay Mortgage, kỳ trả nợ | như trên | (chèn ảnh) |
| 6 | Chuyển tiền nội bộ, liên ngân hàng, quét QR | như trên | (chèn ảnh) |
| 7 | FaceCheck, Biometric, OTP (giao dịch trên 5 triệu) | như trên, hóa đơn `PE0100009999` | (chèn ảnh) |
| 8 | Nạp tiền Stripe PaymentSheet, rút tiền | như trên | (chèn ảnh) |
| 9 | Thanh toán điện, nước, nạp điện thoại | như trên | (chèn ảnh) |
| 10 | Đặt vé, khách sạn, mua sắm | như trên | (chèn ảnh) |
| 11 | Biên lai điện tử, xuất PNG và PDF | như trên | (chèn ảnh) |
| 12 | Bản đồ chi nhánh, đường đi bộ | như trên | (chèn ảnh) |
| 13 | Chatbot AI kèm nút điều hướng | như trên | (chèn ảnh) |
| 14 | eKYC chụp khuôn mặt | `customer.unverified@novapay.vn` | (chèn ảnh) |
| 15 | Dashboard Officer | `officer@novapay.vn` | (chèn ảnh) |
| 16 | Tạo, sửa, khóa khách hàng | như trên | (chèn ảnh) |
| 17 | Duyệt eKYC, hậu kiểm FaceCheck | như trên | (chèn ảnh) |
| 18 | Quản lý lãi suất, Audit Logs | như trên | (chèn ảnh) |

---

## 6. KIỂM THỬ

### 6.1 Backend (tự động)
Chạy `npm test` trong thư mục `server/`. Bộ kiểm thử dùng `node:test` với Firestore giả lập, bao phủ: giao dịch nội bộ và liên ngân hàng, xác thực 3 lớp, OTP hết hạn và khóa sau 5 lần sai, giới hạn gửi lại OTP, khóa sau 5 lần sai FaceCheck, hóa đơn, dịch vụ, khoản vay MONTHLY và BIWEEKLY, hạn mức ngày theo múi giờ Việt Nam, Stripe idempotent và kiểm tra số tiền, tra cứu tài khoản, chatbot, nghiệp vụ Officer và audit log, giới hạn tần suất, xử lý lỗi JSON.

### 6.2 Android
Chạy `./gradlew testDebugUnitTest` (JUnit trên JVM) cho mapper, use case, repository, bộ đánh giá liveness và kiểm tra quyền DeepLink.

### 6.3 Kịch bản kiểm thử thủ công
Dùng bốn tài khoản và dữ liệu mẫu trong `docs/TEST_ACCOUNTS.md` (chuyển tiền thường, hóa đơn trên 5 triệu, khách chưa eKYC bị chặn với `EKYC_REQUIRED`, Officer duyệt eKYC, khóa khách và kiểm tra bị đăng xuất tức thì).

---

## 7. TRIỂN KHAI

- Backend: triển khai Node.js (ví dụ Render), cấu hình biến môi trường như `server/.env.example`, dùng `GET /health` để giữ server hoạt động. Triển khai quy tắc bằng `npm run deploy:rules`, nạp dữ liệu mẫu bằng `npm run seed`.
- Android: đặt `BACKEND_BASE_URL` trỏ tới địa chỉ HTTPS của server khi đóng gói bản release, ký ứng dụng, đưa lên Google Play.
- Google Play: ghi rõ đây là ứng dụng mô phỏng học thuật ở `README.md`, mô tả cửa hàng (Store Listing) và chính sách quyền riêng tư (giữ giao diện ứng dụng chuẩn thương mại). Khai báo tài khoản Customer và Officer trong mục App Access.

---

## 8. PHÂN CÔNG CÔNG VIỆC

| Thành viên | Phạm vi |
| :--- | :--- |
| A | Xác thực, hồ sơ, eKYC, trang chủ, tài khoản (Checking, Saving, Mortgage), lịch sử, `firestore.rules`, `ekyc.ts` |
| B | Chuyển tiền, QR, biên lai, Stripe, OTP, hóa đơn, nạp điện thoại, đặt dịch vụ, `accounts.ts`, `transactions.ts`, `payments.ts` |
| C | Phân hệ Officer, bản đồ chi nhánh, chatbot, giao diện dùng chung, `officer.ts`, `ai.ts` |

(Điều chỉnh theo thực tế nhóm.)

---

## 9. KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN

**Kết quả:** hoàn thành đầy đủ các yêu cầu nghiệp vụ trong đề bài với kiến trúc phân tầng rõ ràng và xử lý giao dịch an toàn.

**Hạn chế:** dữ liệu mô phỏng, liên ngân hàng chưa kết nối Napas thật, OTP gửi qua email thay vì SMS.

**Hướng phát triển:** thông báo đẩy khi có giao dịch, đặt hạn mức giao dịch do người dùng tự chọn, hỗ trợ đa ngôn ngữ, kết nối cổng thanh toán nội địa như VNPay.

---

## 10. TÀI LIỆU THAM KHẢO

1. Tôn Đức Thắng University, Faculty of Information Technology. *Mobile Apps Development — Final Project* (`docs/DE_BAI.md`).
2. Android Developers. *Guide to app architecture*. https://developer.android.com/topic/architecture
3. Firebase. *Cloud Firestore documentation*. https://firebase.google.com/docs/firestore
4. Firebase. *Firestore Security Rules*. https://firebase.google.com/docs/firestore/security/get-started
5. Stripe. *PaymentIntents API* và *Android PaymentSheet*. https://docs.stripe.com
6. Google. *ML Kit Face Detection* và *Barcode Scanning*. https://developers.google.com/ml-kit
7. Google. *Gemini API*. https://ai.google.dev/gemini-api/docs
8. OpenRouteService. *Directions API*. https://openrouteservice.org/dev/#/api-docs
9. Google Dagger. *Hilt*. https://dagger.dev/hilt/
10. Brevo. *Transactional email API*. https://developers.brevo.com
