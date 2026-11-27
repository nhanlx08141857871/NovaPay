<div align="center">

TỔNG LIÊN ĐOÀN LAO ĐỘNG VIỆT NAM  
**TRƯỜNG ĐẠI HỌC TÔN ĐỨC THẮNG**  
**KHOA CÔNG NGHỆ THÔNG TIN**

<br/>

**[VỊ TRÍ CHÈN ẢNH: Logo chính thức của Trường Đại học Tôn Đức Thắng (TDTU) — kích thước khoảng 3,5 cm × 2,5 cm]**

<br/>

**Nguyễn Thành Đạt - 52400007**  
**Nguyễn Văn Duy – 52400010**  
**Nguyễn Phan Nhân - 52400143**

<br/>

# XÂY DỰNG ỨNG DỤNG NGÂN HÀNG SỐ ĐA NĂNG NOVAPAY TRÊN NỀN TẢNG ANDROID KẾT HỢP KIẾN TRÚC ĐÁM MÂY LAI

<br/>

## BÁO CÁO ĐỒ ÁN CUỐI KỲ  
## PHÁT TRIỂN ỨNG DỤNG DI ĐỘNG

<br/>

Người hướng dẫn  
**[Họ tên Giảng viên]**

<br/>

**THÀNH PHỐ HỒ CHÍ MINH, NĂM 2026**

</div>

---

## THÔNG TIN NHÓM THỰC HIỆN

| STT | Họ và tên sinh viên | Mã số sinh viên | Vai trò trong nhóm | Phân hệ phụ trách chính | Tỷ lệ đóng góp |
| :---: | :--- | :---: | :--- | :--- | :---: |
| 1 | **Nguyễn Phan Nhân** | `52400143` | Trưởng nhóm — Core, Security & Account Lead | Phân hệ A (`nav_core`, Auth/RBAC, eKYC/FaceCheck Liveness, Accounts, Mở/Tất toán Tiết kiệm, Thông báo, Bảo mật PIN/Hạn mức, Sao kê PDF, `firestore.rules`) | **100%** |
| 2 | **Nguyễn Thành Đạt** | `52400007` | Thành viên — Money, Payment & Backend Security Lead | Phân hệ B (`nav_money`, Chuyển tiền, QR ISO/IEC 18004, Biên lai PNG/PDF, Stripe `PaymentSheet`, OTP 2FA, Hóa đơn & Dịch vụ, Lõi giao dịch ACID & FaceCheck AI Server) | **100%** |
| 3 | **Nguyễn Văn Duy** | `52400010` | Thành viên — Officer, GIS Map, AI & Design System Lead | Phân hệ C (`nav_officer_map_ai`, Cổng quản trị Bank Officer, Nhật ký kiểm toán `audit_logs`, Bản đồ `osmdroid` & OpenRouteService, AI Chatbot Gemini, Design System Material 3 & Render Cloud) | **100%** |

---

## LỜI CẢM ƠN

Chúng em xin gửi lời cảm ơn chân thành và sâu sắc nhất đến giảng viên **[Họ tên Giảng viên]** — Khoa Công nghệ Thông tin, Trường Đại học Tôn Đức Thắng. Trong suốt quá trình học tập và thực hiện đồ án cuối kỳ môn **Phát triển Ứng dụng Di động**, chúng em đã nhận được sự giảng dạy tận tình, những định hướng kiến trúc phần mềm thực tiễn cùng sự hỗ trợ quý báu từ Thầy/Cô. Những kiến thức nền tảng và tư duy kỹ thuật mà Thầy/Cô truyền đạt đã giúp nhóm tích lũy đủ năng lực để thiết kế, xây dựng và hoàn thiện hệ thống ứng dụng Ngân hàng số **NovaPay** một cách bài bản, chặt chẽ và an toàn.

Bên cạnh đó, chúng em cũng xin cảm ơn quý Thầy, Cô trong Khoa Công nghệ Thông tin cùng các bạn sinh viên đồng khóa đã luôn chia sẻ kinh nghiệm và đóng góp nhiều ý kiến thiết thực trong quá trình kiểm thử sản phẩm.

Mặc dù nhóm đã dành nhiều thời gian, tâm huyết và nỗ lực tối đa để hoàn thiện cả ứng dụng di động lẫn hệ thống máy chủ đám mây, song do phạm vi thời gian và kinh nghiệm thực tiễn còn nhất định nên báo cáo khó tránh khỏi những điểm thiếu sót. Chúng em kính mong nhận được những lời nhận xét, góp ý quý báu từ Hội đồng chấm thi và quý Thầy, Cô để sản phẩm ngày càng hoàn thiện hơn. Chúng em xin chân thành cảm ơn!

<div align="right">

*TP. Hồ Chí Minh, tháng 11 năm 2026*  
**Nhóm tác giả**  
*Nguyễn Thành Đạt — Nguyễn Văn Duy — Nguyễn Phan Nhân*

</div>

---

## CÔNG TRÌNH ĐƯỢC HOÀN THÀNH TẠI TRƯỜNG ĐẠI HỌC TÔN ĐỨC THẮNG

Chúng tôi xin cam đoan đây là công trình nghiên cứu và phát triển phần mềm của riêng nhóm chúng tôi dưới sự hướng dẫn khoa học của giảng viên **[Họ tên Giảng viên]**. Toàn bộ mã nguồn ứng dụng Android, máy chủ Backend, quy tắc phân quyền cơ sở dữ liệu cùng các số liệu thực nghiệm, sơ đồ thiết kế và kết quả kiểm thử trình bày trong báo cáo này đều là trung thực, được trích xuất trực tiếp từ sản phẩm thực tế do nhóm xây dựng.

Ngoài ra, các tài liệu kỹ thuật, giao thức chuẩn và thư viện mã nguồn mở của bên thứ ba được sử dụng trong dự án đều có trích dẫn và chú thích nguồn gốc đầy đủ tại phần Tài liệu tham khảo. Đồng thời, ứng dụng **NovaPay** được xây dựng phục vụ mục đích nghiên cứu học thuật (*Academic Sandbox Simulation*), mọi số dư, thẻ thanh toán kiểm thử và giao dịch trong hệ thống không phát sinh hoạt động lưu ký tiền tệ thực tế.

**Nếu phát hiện có bất kỳ sự gian lận nào, chúng tôi xin hoàn toàn chịu trách nhiệm về nội dung đồ án của mình.** Trường Đại học Tôn Đức Thắng không liên quan đến những vi phạm tác quyền, bản quyền do chúng tôi gây ra trong quá trình thực hiện (nếu có).

<div align="right">

*TP. Hồ Chí Minh, tháng 11 năm 2026*  
**Nhóm tác giả**  
*Nguyễn Thành Đạt — Nguyễn Văn Duy — Nguyễn Phan Nhân*

</div>

---

## TÓM TẮT (LỜI MỞ ĐẦU)

Dưới đây là nội dung báo cáo đồ án cuối kỳ môn **Phát triển Ứng dụng Di động** với đề tài xây dựng hệ thống Ngân hàng số di động **NovaPay (`com.novabank.novapay`)**. Báo cáo trình bày xuyên suốt từ bài toán thực tiễn của lĩnh vực công nghệ tài chính (*Fintech*), cơ sở lý thuyết kiến trúc phần mềm, phân tích thiết kế hệ thống cho đến chi tiết hiện thực hóa mã nguồn và kết quả kiểm thử thực nghiệm qua bố cục 5 chương:

- **Chương 1 — Tổng quan và phân tích yêu cầu hệ thống:** Trình bày bối cảnh chuyển đổi số ngân hàng thời đại 4.0, mục tiêu, phạm vi đề tài, bảng phân công chi tiết cho 3 thành viên cùng bảng đặc tả toàn diện các yêu cầu chức năng (cho 2 tác nhân `CUSTOMER` và `OFFICER`) và yêu cầu phi chức năng.
- **Chương 2 — Cơ sở lý thuyết và công nghệ sử dụng:** Phân tích sâu kiến trúc phân tầng **Clean Architecture kết hợp MVVM** trên nền tảng **100% Java 17** và mô hình **Đám mây Lai song kênh (Hybrid Cloud)** kết hợp Firebase BaaS với máy chủ Node.js/TypeScript trên Render; lập bảng thống kê chi tiết phiên bản từng thư viện và lý giải cơ sở lựa chọn công nghệ.
- **Chương 3 — Phân tích và thiết kế hệ thống:** Trình bày hệ thống **8 sơ đồ thiết kế cốt lõi** bao gồm Sơ đồ Use Case tổng quát, Sơ đồ Kiến trúc tổng thể Hybrid Cloud, Sơ đồ Thực thể Liên kết (ERD) của **15 Collections + 3 Subcollections** kèm từ điển dữ liệu chi tiết, bộ 3 Sơ đồ Lớp UML toàn hệ thống và 2 Sơ đồ Tuần tự cho luồng giao dịch bảo mật 3 lớp (`> 5.000.000 ₫`) cùng luồng nạp tiền quốc tế Stripe Idempotent.
- **Chương 4 — Triển khai, minh họa giao diện và kiểm thử hệ thống:** Trình bày chi tiết cấu trúc 350 lớp Java 17 và 16 API Backend, các giải thuật và kỹ thuật lập trình trọng tâm (giao dịch nguyên tử ACID 2 pha, toán học tiền tệ số nguyên `long`, Liveness ML Kit + Gemini *Zero-False-Acceptance*, thông báo biến động 2 chiều thời gian thực, `firestore.rules`, Haversine GIS & AI Chatbot DeepLink), hệ thống 18 khung minh họa giao diện thực tế và kết quả **156 bài kiểm thử tự động** cùng 16 kịch bản kiểm thử hệ thống.
- **Chương 5 — Kết luận và hướng phát triển:** Đánh giá toàn diện mức độ hoàn thành so với đề bài và bảng tiêu chí chấm điểm, phân tích khách quan các giới hạn kỹ thuật hiện tại và đề xuất lộ trình nâng cấp lên chuẩn thương mại (VietQR EMVCo Napas 247, thanh toán một chạm NFC HCE, Stripe Webhook và phát hành trên Google Play Store).

---

## MỤC LỤC

- **THÔNG TIN NHÓM THỰC HIỆN**
- **LỜI CẢM ƠN**
- **CAM ĐOAN HỌC THUẬT**
- **TÓM TẮT (LỜI MỞ ĐẦU)**
- **CHƯƠNG 1. TỔNG QUAN VÀ PHÂN TÍCH YÊU CẦU HỆ THỐNG**
  - 1.1 Trình bày về đề tài và lý do thực hiện
  - 1.2 Mục tiêu, phạm vi đề tài và phân công công việc chi tiết
  - 1.3 Đặc tả yêu cầu chức năng (Functional Requirements)
  - 1.4 Đặc tả yêu cầu phi chức năng (Non-Functional Requirements)
- **CHƯƠNG 2. CƠ SỞ LÝ THUYẾT VÀ CÔNG NGHỆ SỬ DỤNG**
  - 2.1 Kiến trúc Clean Architecture kết hợp MVVM trên Android Java 17
  - 2.2 Mô hình Kiến trúc Đám mây Lai song kênh (Hybrid Cloud Architecture)
  - 2.3 Hệ sinh thái công nghệ, phiên bản thực tế và lý giải lựa chọn kỹ thuật
- **CHƯƠNG 3. PHÂN TÍCH VÀ THIẾT KẾ HỆ THỐNG**
  - 3.1 Phân tích Use Case tổng quát và đặc tả các Use Case trọng tâm
  - 3.2 Thiết kế Kiến trúc Thành phần và Triển khai Hệ thống
  - 3.3 Thiết kế Cơ sở dữ liệu Cloud Firestore và Sơ đồ ERD
  - 3.4 Thiết kế Sơ đồ Lớp (Class Diagrams) toàn hệ thống
  - 3.5 Thiết kế Sơ đồ Tuần tự (Sequence Diagrams) các luồng nghiệp vụ cốt lõi
- **CHƯƠNG 4. TRIỂN KHAI, MINH HỌA GIAO DIỆN VÀ KIỂM THỬ HỆ THỐNG**
  - 4.1 Tổ chức cấu trúc mã nguồn dự án
  - 4.2 Các kỹ thuật xử lý và giải thuật trọng tâm đã hiện thực hóa
  - 4.3 Minh họa giao diện thực tế các phân hệ Customer và Bank Officer
  - 4.4 Quy trình và kết quả kiểm thử hệ thống (Unit Test & System Test)
- **CHƯƠNG 5. KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN**
  - 5.1 Tổng kết kết quả nghiên cứu và phát triển sản phẩm
  - 5.2 Những khuyết điểm và hạn chế hiện tại của hệ thống
  - 5.3 Định hướng nghiên cứu và phát triển trong tương lai
- **TÀI LIỆU THAM KHẢO**

---

# CHƯƠNG 1. TỔNG QUAN VÀ PHÂN TÍCH YÊU CẦU HỆ THỐNG

## 1.1 Trình bày về đề tài và lý do thực hiện

Trong kỷ nguyên Công nghiệp 4.0, dịch vụ Ngân hàng số trên thiết bị di động (*Mobile Banking*) đã trở thành hạ tầng tài chính thiết yếu đối với mọi cá nhân và tổ chức. Thay vì phải trực tiếp di chuyển đến phòng giao dịch trong giờ hành chính hay xếp hàng tại các trụ ATM vật lý, người dùng ngày nay kỳ vọng có thể thực hiện mọi hoạt động tài chính — từ kiểm tra số dư, chuyển tiền liên ngân hàng 24/7, quét mã QR, gửi tiết kiệm trực tuyến, thanh toán hóa đơn điện nước, đặt vé dịch vụ cho đến tra cứu trợ lý ảo — ngay trên chiếc điện thoại thông minh mọi lúc, mọi nơi. Tại Việt Nam, hầu hết các ngân hàng thương mại lớn như Vietcombank, BIDV hay OCB đều đầu tư mạnh mẽ vào nền tảng ứng dụng di động nhằm tối ưu hóa chi phí vận hành tại quầy và nâng cao trải nghiệm khách hàng.

Tuy nhiên, dưới góc độ kỹ thuật phần mềm, việc xây dựng một ứng dụng Mobile Banking đặt ra những thách thức khắt khe hơn rất nhiều so với các ứng dụng di động thông thường:
1. **Bài toán bảo mật đa tầng trên thiết bị không đáng tin cậy (*Untrusted Client*):** Thiết bị di động của người dùng có thể bị chiếm quyền điều khiển (*root*), dịch ngược mã nguồn APK hoặc giả mạo giao diện, đòi hỏi mọi giao dịch tài chính phải được xác thực đa nhân tố thích ứng (eKYC, đối chiếu sinh trắc học khuôn mặt, vân tay thiết bị, mã OTP một lần và mã PIN giao dịch).
2. **Bài toán toàn vẹn số học và tính nguyên tử của giao dịch (*ACID Consistency*):** Một giao dịch chuyển tiền hay gạch nợ hóa đơn liên quan đồng thời đến nhiều tập dữ liệu (tài khoản nguồn, tài khoản đích, bút toán sổ cái, trạng thái hóa đơn và hộp thư thông báo), tuyệt đối không được phép xảy ra sai số dấu phẩy động hay mất mát dữ liệu khi rớt mạng giữa chừng.
3. **Bài toán phân tách quyền hạn thời gian thực (*Realtime Role-Based Access Control*):** Hệ thống phải phục vụ đồng thời cả Khách hàng cá nhân lẫn Nhân viên quản trị ngân hàng trên cùng một ứng dụng duy nhất nhưng cách ly hoàn toàn về giao diện, luồng điều hướng và quyền truy xuất cơ sở dữ liệu.

Xuất phát từ yêu cầu thực tiễn đó và nội dung đề bài đồ án cuối kỳ môn **Phát triển Ứng dụng Di động**, nhóm em quyết định nghiên cứu và xây dựng hệ thống **Ngân hàng số NovaPay (`com.novabank.novapay`)** — một nền tảng ngân hàng di động hoàn chỉnh kết hợp ứng dụng Android Native thuần Java 17 với máy chủ đám mây Node.js/TypeScript và cơ sở dữ liệu thời gian thực Google Cloud Firestore.

## 1.2 Mục tiêu, phạm vi đề tài và phân công công việc chi tiết

### 1.2.1 Mục tiêu đề tài
- **Về mặt kiến trúc và học thuật:** Làm chủ kiến trúc phân tầng **Clean Architecture kết hợp MVVM** trên Android Native (100% Java 17), kỹ thuật tiêm phụ thuộc **Dagger Hilt**, lập trình phản ứng **RxJava 3**, điều hướng liên phân hệ qua **DeepLink nội bộ (`novapay://`)** và thiết kế giao dịch nguyên tử hai pha trên **Cloud Firestore**.
- **Về mặt sản phẩm ứng dụng:** Hoàn thiện toàn bộ 7 nhóm nghiệp vụ cốt lõi của đề bài (*User Profiles, Security, Account Management, Transaction Management, Utilities, Navigation, AI Chatbot*), đồng thời mở rộng thêm các tính năng chuẩn một ứng dụng iBanking thương mại thực tế bao gồm: **Thông báo biến động số dư 2 chiều thời gian thực kèm System Push Notification**, **Mở và tất toán sổ tiết kiệm trực tuyến**, **Trung tâm bảo mật quản lý Smart PIN 6 số, hạn mức giao dịch & khóa tài khoản khẩn cấp**, **Đăng nhập nhanh sinh trắc học bảo vệ bằng Android Keystore** và **Xuất sao kê điện tử PDF**.

### 1.2.2 Phạm vi nghiên cứu
- Ứng dụng phía thiết bị đầu cuối được phát triển hoàn toàn bằng **Java 17** (`minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`), sử dụng **XML Layouts + ViewBinding** và hệ thống thiết kế **Material Design 3** (*Clean Fintech Minimalism*), hiển thị 100% Tiếng Việt có dấu.
- Máy chủ xử lý giao dịch được xây dựng độc lập bằng **Node.js 20 + TypeScript + Express + Firebase Admin SDK**, triển khai trực tuyến qua giao thức **HTTPS** trên nền tảng đám mây **Render**, tích hợp thực tế với **Stripe PaymentIntents API** (Test Mode), **Brevo Transactional Email REST API**, **Google Gemini 2.5 Flash Multimodal API** và **OpenRouteService Directions API**.
- Toàn bộ hoạt động thanh toán, số dư và tài khoản ngân hàng vận hành trong môi trường mô phỏng học thuật an toàn (*Academic Sandbox Simulation*), sử dụng 4 tài khoản Gmail thật của nhóm để kiểm chứng luồng nhận mã OTP thực tế.

### 1.2.3 Bảng phân công nhiệm vụ chi tiết cho 3 thành viên

Để 3 thành viên làm việc song song trên kiến trúc Single-Activity mà không xảy ra xung đột mã nguồn (*Git Merge Conflict*), nhóm thiết lập bộ khung nền tảng dùng chung (*Day-0 Skeleton*) và phân vùng sở hữu tệp độc lập theo *Bảng 1-1*:

*Bảng 1-1. Phân công chi tiết các phân hệ kỹ thuật giữa 3 thành viên nhóm thực hiện*

| Thành viên (MSSV) | Phân hệ sở hữu | Hạng mục Android (`app/`) | Hạng mục Backend (`server/`) & Dữ liệu |
| :--- | :---: | :--- | :--- |
| **Nguyễn Phan Nhân**<br>(`52400143`) | **Module A**<br>*(Core, Auth, eKYC, Accounts, iBanking Security)* | • Khung `BaseFragment<VB>`, `AppResult`, `UiState`, `SingleLiveEvent`, `MainActivity` & chốt chặn phân quyền 2 chiều.<br>• `SessionManager` (khóa phiên realtime), `BiometricHelper`, `BiometricQuickLoginStore` (Keystore AES-GCM).<br>• `LoginFragment` (Đăng nhập, Quên MK), `ProfileFragment`, `EkycCaptureFragment` & `FaceCheckDialogFragment` (`FaceLivenessEvaluator` CameraX + ML Kit).<br>• `CustomerHomeFragment` (biểu đồ `MPAndroidChart`), `AccountRouterFragment`, 3 màn hình `Checking` (`QrBitmapEncoder`), `Saving`, `Mortgage`, `OpenSavingFragment`.<br>• `NotificationCenterFragment`, `BalanceAlertSubscriber`, `SystemNotificationHelper`, `SecuritySettingsFragment`, `TransactionHistoryFragment` & `StatementPdfExporter`.<br>• `FirebaseModule`, `NetworkModule`, `CoreRepositoryModule`, `nav_graph.xml`, `nav_core.xml`, `strings_core.xml`. | • Thiết kế lược đồ **15 Collections + 3 Subcollections**.<br>• Xây dựng và triển khai trực tiếp bộ quy tắc **`firestore.rules`** & `firestore.indexes.json`.<br>• Hiện thực API định danh `routes/ekyc.ts`, tiết kiệm trực tuyến `routes/savings.ts` và bảo mật hạn mức `routes/security.ts`. |
| **Nguyễn Thành Đạt**<br>(`52400007`) | **Module B**<br>*(Money, QR, Stripe, OTP & Utilities)* | • `TransferFragment` (chuyển tiền nội bộ tra cứu tên tự động, liên ngân hàng, danh bạ thụ hưởng).<br>• `QrScannerFragment` (CameraX + ML Kit Barcode) & `QrHelper` (giải mã/mã hóa QR chuyển khoản).<br>• `OtpVerificationDialogFragment` (điều phối liên hoàn 3 lớp bảo mật `FaceCheck → Biometric → OTP 180s`).<br>• `DepositWithdrawFragment` & `StripeGatewayHelper` (tích hợp `PaymentSheet`, rút tiền & trả nợ vay).<br>• `ReceiptFragment` (xuất & chia sẻ biên lai điện tử PNG/PDF qua `FileProvider`).<br>• `UtilityBillFragment` (Điện/Nước), `PhoneTopupFragment` (Nạp ĐT), `ServiceBookingFragment` (Vé máy bay, phim, khách sạn, mua sắm).<br>• `MoneyRepositoryModule`, `nav_money.xml`, `strings_money.xml`. | • Xây dựng lõi giao dịch nguyên tử 2 pha `routes/transactions.ts`, tra cứu ẩn PII `routes/accounts.ts`, cổng thanh toán `routes/payments.ts`.<br>• Hiện thực các dịch vụ `otp.ts` (`SHA-256`), `pin.ts` (`HMAC-SHA256`), `email.ts` (Brevo HTTPS), `stripe.ts`, `faceCheck.ts` (Gemini *Zero-False-Acceptance*), `notifications.ts` và `middleware/rateLimit.ts`.<br>• Xây dựng bộ kiểm thử tự động Backend (`61` test cases). |
| **Nguyễn Văn Duy**<br>(`52400010`) | **Module C**<br>*(Bank Officer, GIS Map, AI Chatbot & Design System)* | • Toàn bộ cổng quản trị Nhân viên Ngân hàng: `OfficerDashboardFragment` (KPI, hậu kiểm FaceCheck, `audit_logs`), `CustomerManagementFragment`, `CreateCustomerFragment`, `OfficerEkycReviewFragment` (duyệt 2 khung ảnh & chụp hộ tại quầy), `InterestRateManagementFragment`.<br>• `BranchMapFragment` (bản đồ `osmdroid`, thuật toán cầu Haversine tìm chi nhánh gần nhất, vẽ đường đi bộ OpenRouteService, GPS Fallback Quận 7).<br>• `ChatbotFragment` (trợ lý tài chính AI kèm chip điều hướng DeepLink).<br>• Hệ thống giao diện dùng chung Material 3 (`themes.xml`, `colors.xml`, `styles.xml`, `StateLayoutHelper` views), `OfficerMapAiRepositoryModule`, `nav_officer_map_ai.xml`, `strings_officer.xml`. | • Hiện thực các API quản trị viên `routes/officer.ts` & dịch vụ kiểm toán `services/officerAudit.ts` (tạo user, khóa tài khoản thu hồi token, cập nhật lãi suất kèm `audit_logs`).<br>• Hiện thực `routes/ai.ts` & `services/chatbot.ts` (ngữ cảnh thu/chi tháng, Whitelist DeepLink & Rule-based Vietnamese NLP Fallback).<br>• Cấu hình `seed.ts` cho 4 tài khoản Gmail thật và triển khai máy chủ HTTPS trên **Render Cloud**. |

---

## 1.3 Đặc tả yêu cầu chức năng (Functional Requirements)

Dựa trên đề bài gốc (`docs/DE_BAI.md`) và quy trình nghiệp vụ ngân hàng số thực tế, các yêu cầu chức năng của hệ thống được phân tích và chuẩn hóa theo hai nhóm tác nhân trong *Bảng 1-2* và *Bảng 1-3*:

*Bảng 1-2. Đặc tả yêu cầu chức năng phân hệ Khách hàng (`CUSTOMER`)*

| Mã YC | Nhóm chức năng | Mô tả chi tiết nghiệp vụ yêu cầu | Màn hình / Thành phần đáp ứng |
| :---: | :--- | :--- | :--- |
| **FR-C01** | Đăng nhập & Khôi phục truy cập | Đăng nhập bằng Email/Mật khẩu, kiểm tra trạng thái `ACTIVE`, gửi email đặt lại mật khẩu khi quên và hỗ trợ đăng nhập nhanh bằng vân tay/khuôn mặt. | `LoginFragment`, `BiometricQuickLoginStore` |
| **FR-C02** | Hồ sơ cá nhân & Định danh eKYC | Xem thông tin định danh (Họ tên, CCCD, ngày sinh, trạng thái eKYC); cập nhật SĐT, địa chỉ, ảnh đại diện; chụp ảnh khuôn mặt trực diện qua CameraX + ML Kit Liveness gửi thẩm định eKYC. | `ProfileFragment`, `EkycCaptureFragment` |
| **FR-C03** | Quản lý 3 loại Tài khoản | Xem tổng tài sản, biểu đồ thu/chi tháng và điều hướng riêng biệt cho 3 loại tài khoản: **Checking** (số dư, hạn mức, QR cá nhân), **Saving** (kỳ hạn, lãi suất & tiền lãi dự kiến hàng tháng realtime), **Mortgage** (dư nợ, chu kỳ `MONTHLY`/`BIWEEKLY`, ngày đến hạn). | `CustomerHomeFragment`, `AccountRouterFragment`, `Checking/Saving/MortgageDetailFragment` |
| **FR-C04** | Mở & Tất toán Tiết kiệm trực tuyến | Mở sổ tiết kiệm mới trích tiền từ tài khoản `CHECKING` theo biểu lãi suất kỳ hạn hiện hành; xem bảng dự toán và tất toán sổ tiết kiệm trước hạn hoặc đúng hạn về `CHECKING`. | `OpenSavingFragment`, `SavingDetailFragment`, `POST /savings/*` |
| **FR-C05** | Chuyển tiền Nội bộ, Liên NH & QR | Chuyển khoản nội bộ NovaPay (tự động tra cứu tên người nhận qua `/accounts/lookup`), chuyển khoản liên ngân hàng Napas, quét mã QR điền tự động thông tin, lưu/xóa danh bạ thụ hưởng và chia sẻ biên lai điện tử PNG/PDF. | `TransferFragment`, `QrScannerFragment`, `ReceiptFragment` |
| **FR-C06** | Nạp tiền Quốc tế Stripe, Rút tiền & Trả nợ vay | Nạp tiền trực tuyến vào `CHECKING` qua cổng quốc tế **Stripe `PaymentSheet`**, rút tiền về thẻ/tài khoản liên kết và thanh toán kỳ hạn khoản vay thế chấp (`MORTGAGE_PAYMENT`). | `DepositWithdrawFragment`, `StripeGatewayHelper`, `POST /payments/finalize` |
| **FR-C07** | Xác thực Giao dịch Đa lớp | Bắt buộc xác thực OTP Email 6 số (180s) cho mọi giao dịch; tự động kích hoạt quy trình **3 lớp (`FaceCheck` AI + Sinh trắc học thiết bị + OTP)** cho mọi giao dịch chi/rút `> 5.000.000 ₫`. | `FaceCheckDialogFragment`, `OtpVerificationDialogFragment`, `BiometricHelper` |
| **FR-C08** | Thanh toán Tiện ích & Dịch vụ số | Tra cứu và thanh toán hóa đơn Điện/Nước theo mã khách hàng, nạp tiền điện thoại di động, đặt vé máy bay, vé xem phim, phòng khách sạn, mua sắm trực tuyến và theo dõi danh sách đơn hàng. | `UtilityBillFragment`, `PhoneTopupFragment`, `ServiceBookingFragment` |
| **FR-C09** | Thông báo Biến động & Sao kê PDF | Nhận thông báo nổi hệ thống (*System Push*) và hộp thư biến động số dư 2 chiều thời gian thực; lọc lịch sử sổ cái theo chiều `IN`/`OUT`, khoảng ngày, từ khóa và xuất tệp Sao kê PDF chuẩn A4. | `NotificationCenterFragment`, `BalanceAlertSubscriber`, `TransactionHistoryFragment`, `StatementPdfExporter` |
| **FR-C10** | Trung tâm Bảo mật Cá nhân | Đổi mật khẩu đăng nhập, thiết lập/đổi mã Smart PIN giao dịch 6 số, tự điều chỉnh hạn mức giao dịch mỗi lần/mỗi ngày và tạm khóa khẩn cấp (`FROZEN`) tài khoản thanh toán. | `SecuritySettingsFragment`, `POST /security/*` |
| **FR-C11** | Bản đồ Chi nhánh & Chỉ đường đi bộ | Định vị GPS hiện tại của người dùng trên bản đồ, hiển thị mạng lưới Chi nhánh/ATM, tự động gợi ý điểm gần nhất và vẽ lộ trình đi bộ ngắn nhất. | `BranchMapFragment`, `OpenRouteServiceApi` |
| **FR-C12** | Trợ lý Ảo Tài chính AI | Trò chuyện tiếng Việt với trợ lý AI về số dư, tình hình thu/chi tháng và chạm trực tiếp vào các nút hành động DeepLink để chuyển ngay tới màn hình chức năng yêu cầu. | `ChatbotFragment`, `POST /ai/chat` |

*Bảng 1-3. Đặc tả yêu cầu chức năng phân hệ Nhân viên Ngân hàng (`OFFICER`)*

| Mã YC | Nhóm chức năng | Mô tả chi tiết nghiệp vụ yêu cầu | Màn hình / Thành phần đáp ứng |
| :---: | :--- | :--- | :--- |
| **FR-O01** | Bảng điều khiển Giám sát (Dashboard) | Theo dõi các chỉ số vận hành (tổng số khách hàng, giao dịch trong ngày, hồ sơ eKYC chờ duyệt), danh sách giao dịch giá trị cao cần hậu kiểm khuôn mặt và luồng nhật ký kiểm toán mới nhất. | `OfficerDashboardFragment` |
| **FR-O02** | Khởi tạo & Quản lý Hồ sơ Khách hàng | Mở tài khoản khách hàng mới kèm tài khoản `CHECKING` mặc định, chỉnh sửa thông tin khách hàng theo yêu cầu tại quầy và thực hiện Khóa (`LOCKED`) / Mở khóa (`ACTIVE`) tài khoản tức thời. | `CreateCustomerFragment`, `CustomerManagementFragment`, `POST/PATCH /officer/customers` |
| **FR-O03** | Chụp hộ & Thẩm định Hồ sơ eKYC | Hỗ trợ chụp ảnh định danh khuôn mặt trực tiếp tại quầy cho khách hàng (`targetUid`), đối chiếu trực quan ảnh chân dung để Phê duyệt (`VERIFIED`) hoặc Từ chối (`REJECTED`). | `OfficerEkycReviewFragment`, `EkycCaptureFragment`, `POST /ekyc/review` |
| **FR-O04** | Quản lý Biểu lãi suất & Kiểm toán | Cập nhật mức lãi suất năm (`%/năm`) cho từng kỳ hạn gửi tiết kiệm áp dụng thời gian thực trên toàn hệ thống; tự động ghi nhận mọi thao tác quản trị vào `audit_logs` bất biến. | `InterestRateManagementFragment`, `POST /officer/interest-rates` |

---

## 1.4 Đặc tả yêu cầu phi chức năng (Non-Functional Requirements)

*Bảng 1-4. Đặc tả các yêu cầu phi chức năng cốt lõi của hệ thống NovaPay*

| Mã YC | Thuộc tính chất lượng | Tiêu chuẩn kỹ thuật bắt buộc áp dụng trong hệ thống |
| :---: | :--- | :--- |
| **NFR-01** | **Bảo mật Khóa bí mật & Truyền tải** | Tuyệt đối không nhúng `STRIPE_SECRET_KEY`, `GEMINI_API_KEY`, `BREVO_API_KEY` hay `FIREBASE_PRIVATE_KEY` vào mã nguồn Android/`BuildConfig`. Toàn bộ giao tiếp Internet bắt buộc mã hóa HTTPS TLS 1.3 (`cleartextTrafficPermitted="false"`), tắt sao lưu ADB (`allowBackup="false"`). |
| **NFR-02** | **Độ chính xác Sinh trắc học (*Zero-False-Acceptance*)** | Luồng `FaceCheck` (`> 5.000.000 ₫`) bắt buộc kiểm tra Liveness tại biên (1 mặt, 2 mắt mở `>= 0.7`, góc nghiêng `<= 15°`), từ chối hồ sơ thiếu ảnh gốc thật (`403 EKYC_FACE_NOT_FOUND`), chỉ xác nhận khi Gemini đạt `matched == true && confidence >= 0.80` và áp dụng chế độ **Fail-Closed (`503`)** khi dịch vụ AI gián đoạn. |
| **NFR-03** | **Tính nguyên tử & Toàn vẹn Tiền tệ** | 100% số dư, số tiền giao dịch, hạn mức và tiền lãi phải lưu trữ bằng số nguyên 64-bit **`long`** (đơn vị VNĐ), cấm dùng `double`/`float`. Mọi giao dịch hạch toán phải thực thi trong **2-Phase Firestore ACID Transaction** và tính hạn mức ngày theo đúng múi giờ `Asia/Ho_Chi_Minh` (`UTC+7`). |
| **NFR-04** | **Hiệu năng & Đồng bộ Thời gian thực** | Biến động số dư, tiền lãi tiết kiệm dự kiến, thông báo đẩy và lệnh cưỡng bức khóa tài khoản (`LOCKED`) phải phản hồi tới giao diện người dùng trong dưới **1–2 giây** thông qua cơ chế `SnapshotListener`. Mọi màn hình xử lý đầy đủ 4 trạng thái `LOADING`, `SUCCESS`, `EMPTY`, `ERROR`. |
| **NFR-05** | **Khả năng Chịu lỗi & Tự phục hồi (*Resilience*)** | Bản đồ chi nhánh có cơ chế hẹn giờ GPS tối đa **5 giây** và tự động định tâm về Quận 7 (`10.7326, 106.6998`) nếu mất sóng vệ tinh hoặc máy ảo đặt tọa độ ngoài bán kính `100 km`; Trợ lý AI tự động chuyển sang bộ phân tích ngôn ngữ tự nhiên tiếng Việt theo luật (*Rule-based NLP*) nếu Gemini hết hạn mức (`HTTP 429`). |
| **NFR-06** | **Thẩm mỹ Giao diện & Tương thích Thiết bị** | Tuân thủ chuẩn **Material Design 3** theo phong cách ngân hàng số hiện đại (*Clean Fintech Minimalism*), hỗ trợ chế độ Sáng/Tối (*Light/Dark Mode*), chữ số bảng biểu đều hàng (*Tabular Figures*), tương thích dải rộng từ **Android 8.0 (API 26)** đến **Android 15 (API 35)**. |

---

# CHƯƠNG 2. CƠ SỞ LÝ THUYẾT VÀ CÔNG NGHỆ SỬ DỤNG

## 2.1 Kiến trúc Clean Architecture kết hợp MVVM trên Android Java 17

Để đảm bảo khả năng bảo trì, kiểm thử đơn vị độc lập và mở rộng quy mô lên tới **350 lớp Java 17** cùng **50 tệp giao diện XML**, ứng dụng Android NovaPay không viết trộn lẫn logic truy vấn mạng hay cơ sở dữ liệu vào trong `Fragment`/`Activity` mà áp dụng nghiêm ngặt kiến trúc phân tầng **Clean Architecture** kết hợp mẫu thiết kế **Model – View – ViewModel (MVVM)**:

1. **Tầng `domain` (Lõi Nghiệp vụ Độc lập — Pure Java 17, `128` lớp):**
   - Chứa các thực thể nghiệp vụ bất biến (`model/`), `17` giao diện hợp đồng truy xuất dữ liệu (`repository/`) và hơn `50` lớp ca sử dụng (`usecase/`).
   - **Quy tắc phụ thuộc cốt lõi (*Dependency Rule*):** Tầng `domain` hoàn toàn không chứa bất kỳ câu lệnh `import` nào thuộc `android.*`, `androidx.*`, `com.google.firebase.*`, `retrofit2.*` hay `com.google.gson.*`. Nhờ đó, toàn bộ công thức tính lãi tiết kiệm, kiểm tra điều kiện giao dịch, lọc sao kê và tính khoảng cách địa lý đều có thể chạy kiểm thử đơn vị trực tiếp trên máy ảo JVM chỉ trong vài giây.
2. **Tầng `data` (Triển khai Dữ liệu & Nguồn Từ xa — `94` lớp):**
   - Triển khai `17` lớp `*RepositoryImpl` thực thi các hợp đồng của tầng `domain`, kết nối song song tới hai nguồn dữ liệu: `data/remote/firebase/` (`SnapshotListener` thời gian thực) và `data/remote/server/` (`10` giao diện Retrofit REST API + `OpenRouteServiceApi`).
   - Toàn bộ quá trình chuyển đổi từ đối tượng mạng/cơ sở dữ liệu (`38` lớp DTO, `DocumentSnapshot`, `Timestamp`, `GeoPoint`) sang kiểu chuẩn của tầng `domain` (`long`, `java.time.Instant`, `GeoLocation`) được đóng gói cô lập tại `data/mapper/`.
3. **Tầng `presentation` (Giao diện Người dùng & Quản lý Trạng thái MVVM — `104` lớp):**
   - Mỗi màn hình tuân thủ chặt chẽ bộ ba lớp chuẩn mực: `*Fragment.java` (chỉ đảm nhiệm hiển thị ViewBinding và lắng nghe dữ liệu), `*ViewModel.java` (gọi `UseCase` qua RxJava 3 trên `Schedulers.io()` và nhận kết quả tại `AndroidSchedulers.mainThread()`) và `*UiState.java` (đối tượng trạng thái bất biến).
   - Trạng thái màn hình được bao gói thống nhất qua `LiveData<UiState<T>>` (đồng bộ 4 pha `LOADING`, `SUCCESS`, `EMPTY`, `ERROR` cùng `StateLayoutHelper`), trong khi các tác vụ chỉ thực thi một lần duy nhất (hiển thị `Snackbar`, mở hộp thoại OTP/FaceCheck, điều hướng màn hình) sử dụng `SingleLiveEvent<T>` dựa trên `AtomicBoolean` nhằm triệt tiêu lỗi lặp lại sự kiện khi xoay màn hình.
4. **Tầng `core` (Hạ tầng Dùng chung & Quản lý Vòng đời — `24` lớp):**
   - Lớp cơ sở trừu tượng `BaseFragment<VB extends ViewBinding>` tự động hóa việc giải phóng tham chiếu `binding = null` và hủy toàn bộ luồng bất đồng bộ `disposables.clear()` ngay bên trong `onDestroyView()`, ngăn chặn triệt để rò rỉ bộ nhớ (*Memory Leak*).
   - Kiến trúc **Single-Activity (`MainActivity`)** kết hợp **Jetpack Navigation Component** chia đồ thị điều hướng thành 3 sub-graphs độc lập (`nav_core.xml`, `nav_money.xml`, `nav_officer_map_ai.xml`); mọi điều hướng liên phân hệ đi qua URI nội bộ **`novapay://...`** được bảo vệ hai chiều bởi hàm `enforceDestinationGuard()`.

## 2.2 Mô hình Kiến trúc Đám mây Lai song kênh (Hybrid Cloud Architecture)

Trong kiến trúc hệ thống tài chính, nếu ứng dụng di động kết nối trực tiếp hoàn toàn vào cơ sở dữ liệu đám mây (*Serverless BaaS thuần túy*), hệ thống sẽ đối mặt với nguy cơ lộ khóa bí mật bên thứ ba khi bị dịch ngược APK và không thể ngăn chặn người dùng trên thiết bị đã root tự ý chỉnh sửa số dư hoặc bỏ qua bước xác thực 2FA. Ngược lại, nếu định tuyến mọi thao tác đọc qua máy chủ REST truyền thống thì ứng dụng sẽ đánh mất khả năng đẩy dữ liệu thời gian thực mượt mà của Cloud Firestore.

**NovaPay** giải quyết bài toán đánh đổi này bằng kiến trúc **Đám mây Lai song kênh (*Dual-Channel Hybrid Cloud*)**:
- **Kênh 1 — Đọc Thời gian thực Trực tiếp (*Direct Realtime Read Channel*):** Ứng dụng Android sử dụng Firebase SDK lắng nghe trực tiếp qua gRPC/TLS các tài liệu thuộc quyền sở hữu của người dùng đang đăng nhập (`users/{uid}`, `accounts`, `ledger`, `notifications`, `interest_rates`, `branches`). Bộ quy tắc **`firestore.rules`** trên đám mây đóng vai trò tường lửa dữ liệu, khóa cứng `allow write: if false` đối với toàn bộ các bảng tài chính và kiểm toán.
- **Kênh 2 — Ghi Giao dịch Đặc quyền (*Zero-Trust Transactional REST Channel*):** Mọi yêu cầu chuyển tiền, thanh toán hóa đơn, nạp tiền Stripe, mở/tất toán tiết kiệm, xác thực khuôn mặt/OTP/PIN và quản trị của nhân viên ngân hàng đều gửi qua HTTPS tới máy chủ **Node.js 20 + TypeScript + Express** đặt trên **Render Cloud** kèm `Authorization: Bearer <Firebase_ID_Token>`. Máy chủ kiểm chứng chữ ký số JWT với cờ kiểm tra thu hồi (`verifyIdToken(token, true)`), giữ kín toàn bộ khóa bí mật (`STRIPE_SECRET_KEY`, `BREVO_API_KEY`, `GEMINI_API_KEY`, `FIREBASE_PRIVATE_KEY`) và dùng **Firebase Admin SDK** thực thi giao dịch nguyên tử `db.runTransaction()`.

## 2.3 Hệ sinh thái công nghệ, phiên bản thực tế và lý giải lựa chọn kỹ thuật

Toàn bộ các thư viện và công cụ tích hợp trong dự án được trích xuất chính xác từ tệp cấu hình `gradle/libs.versions.toml`, `app/build.gradle.kts` và `server/package.json` như trình bày trong *Bảng 2-1* và *Bảng 2-2*:

*Bảng 2-1. Bảng thống kê công nghệ và thư viện phía ứng dụng Android (`app/`)*

| Nhóm công nghệ | Thư viện / Công cụ | Phiên bản | Vai trò và vị trí áp dụng thực tế trong mã nguồn |
| :--- | :--- | :---: | :--- |
| **Ngôn ngữ & SDK** | Java SE 17 & Android SDK | `Java 17`<br>`API 26–35` | 100% mã nguồn viết bằng Java 17 (`350` lớp `.java`, `0` tệp Kotlin); hỗ trợ gốc `java.time.Instant` và Notification Channels từ Android 8.0 đến Android 15. |
| **Build & Giao diện** | Gradle / AGP / ViewBinding / Material 3 | Gradle `9.6.0`<br>AGP `9.5.1`<br>Material `1.12.0` | Ánh xạ an toàn kiểu cho `50` tệp XML Layout (`BaseFragment`), tối ưu R8 bản phát hành (`rules.keep`), thiết kế *Clean Fintech Minimalism* hỗ trợ Sáng/Tối. |
| **Kiến trúc & DI** | AndroidX Lifecycle / Dagger Hilt | Lifecycle `2.8.7`<br>Hilt `2.60.1` | Quản lý `ViewModel` + `LiveData<UiState<T>>`; tiêm phụ thuộc tự động và tách riêng **2 Qualifiers** `@BackendRetrofit` (có gắn Bearer Token) và `@OpenRouteRetrofit` (không gửi Token ngân hàng ra ngoài). |
| **Bất đồng bộ & Mạng** | RxJava 3 / RxAndroid / Retrofit 2 / OkHttp 3 / Gson | RxJava `3.1.10`<br>Retrofit `2.11.0`<br>OkHttp `4.12.0` | Xử lý luồng bất đồng bộ `Single`/`Observable` (`combineLatest` tính lãi tiết kiệm realtime); `AuthInterceptor` tự động đính kèm JWT và tắt log mạng khi phát hành (`!BuildConfig.DEBUG`). |
| **Điều hướng** | Jetpack Navigation Component | `2.8.9` | Điều hướng Single-Activity qua `nav_graph.xml` gồm 3 sub-graphs độc lập, liên kết liên phân hệ bằng DeepLink `novapay://` và `FragmentResultListener`. |
| **AI Biên & Mã QR** | CameraX / Guava / Google ML Kit / Pure Java QR | CameraX `1.4.2`<br>Face `16.1.7`<br>Barcode `17.3.0` | Kiểm tra thực thể sống tại biên (`FaceLivenessEvaluator`), quét mã QR chuyển khoản (`QrScannerFragment`) và tự sinh ma trận QR cá nhân chuẩn **ISO/IEC 18004** thuần Java (`QrBitmapEncoder`). |
| **Sinh trắc học & Mật mã** | AndroidX Biometric & Android Keystore | `1.1.0` + TEE Keystore | Xác thực `BIOMETRIC_STRONG \| DEVICE_CREDENTIAL` (`BiometricHelper`) và mã hóa thông tin đăng nhập nhanh bằng **`AES/GCM/NoPadding` (128-bit)** trong phần cứng `AndroidKeyStore`. |
| **Thanh toán, Bản đồ & Đồ thị** | Stripe Android SDK / `osmdroid` / `MPAndroidChart` | Stripe `21.17.0`<br>OSM `6.1.20`<br>Chart `v3.1.0` | Tích hợp giao diện thanh toán thẻ quốc tế chuẩn PCI-DSS (`PaymentSheet`), bản đồ chi nhánh miễn phí không cần thẻ tín dụng kèm đường đi bộ và biểu đồ cột dòng tiền tháng. |
| **Kiểm thử Tự động** | JUnit 4 & AndroidX Espresso | JUnit `4.13.2`<br>Espresso `3.6.1` | Thực thi `32` bộ kiểm thử đơn vị (`95` test cases) trực tiếp trên JVM tốc độ cao cùng kiểm thử thiết bị Android. |

*Bảng 2-2. Bảng thống kê công nghệ phía máy chủ Backend (`server/`) và Đám mây*

| Thành phần / Gói thư viện | Phiên bản | Vai trò kỹ thuật và tệp mã nguồn áp dụng |
| :--- | :---: | :--- |
| **Node.js Runtime & TypeScript** | Node `>= 20.0.0`<br>TS `^5.7.3` (`tsx ^4.19.3`) | Thực thi máy chủ bất đồng bộ hiệu năng cao, kiểm tra kiểu tĩnh nghiêm ngặt (`strict: true`), tận dụng trực tiếp Web `fetch`, `AbortController`, `node:crypto` và `node:test`. |
| **Express.js, Helmet & CORS** | Express `^4.21.2`<br>Helmet `^8.0.0`<br>CORS `^2.8.5` | Xây dựng `8` bộ định tuyến RESTful JSON, bảo vệ HTTP Security Headers chuẩn OWASP, cấu hình `trust proxy = 1` trên Render, giới hạn `1MB` JSON body và `RateLimiter` chống vét cạn theo IP. |
| **Firebase Admin SDK** | `^13.1.0` | Xác minh `verifyIdToken(token, true)`, cấp phát Custom Claim `role`, thu hồi phiên `revokeRefreshTokens`, thực thi giao dịch nguyên tử `db.runTransaction()` và tự động triển khai `firestore.rules`. |
| **Stripe Node SDK** | `^17.7.0` | Tạo `PaymentIntent` cho tiền tệ không thập phân `currency: "vnd"` kèm `idempotencyKey` và đối soát ngược trạng thái `succeeded` tại `/payments/finalize`. |
| **Brevo HTTPS REST API** | REST `v3` (Port `443`) | Gửi email chứa mã OTP 6 số HTML Tiếng Việt qua giao thức HTTPS (`services/email.ts`, timeout `8s`), khắc phục hoàn toàn việc nhà cung cấp đám mây chặn các cổng SMTP truyền thống (`25/465/587`). |
| **Google Gemini Multimodal API** | `gemini-2.5-flash` (`v1beta` REST) | Đối chiếu sinh trắc học khuôn mặt `FaceCheck` (`temperature: 0.0`, ngưỡng `confidence >= 0.80`) tại `services/faceCheck.ts` và vận hành Trợ lý ảo tài chính tại `services/chatbot.ts`. |
| **Mô-đun Mật mã `node:crypto`** | Built-in Node 20+ | Sinh OTP an toàn mật mã `crypto.randomInt`, băm OTP bằng `SHA-256` kèm salt `draftId`, băm Smart PIN 6 số bằng `HMAC-SHA256` tại `user_secrets/{uid}` và so sánh hằng thời gian `timingSafeEqual`. |

Để làm rõ tính thuyết phục của các quyết định kỹ thuật trên, *Bảng 2-3* đối chiếu trực tiếp giải pháp của NovaPay với các phương án thay thế phổ biến:

*Bảng 2-3. Phân tích so sánh và lý giải lựa chọn công nghệ trọng tâm*

| Bài toán kỹ thuật | Giải pháp NovaPay lựa chọn | Phương án thay thế | Lý do lựa chọn dưới góc độ kỹ thuật phần mềm |
| :--- | :--- | :--- | :--- |
| **Biểu diễn tiền tệ VNĐ** | Số nguyên 64-bit **`long`** (Java) & Safe Integer (TS) | Số thực dấu phẩy động `double` / `float` | Chuẩn nhị phân IEEE 754 gây sai số tích lũy (`0.1 + 0.2 = 0.30000000000000004`), dễ làm lệch số dư hoặc trượt điều kiện khớp tiền hóa đơn; kiểu `long` biểu diễn tới $9,22 \times 10^{18}$ VNĐ với độ chính xác tuyệt đối 100%. |
| **Bản đồ & Chỉ đường** | **`osmdroid` + `OpenRouteService`** | Google Maps SDK + Directions API | Không đòi hỏi thẻ tín dụng quốc tế (Billing Account) dễ bị khóa hạn mức đột xuất khi bảo vệ đồ án; cho phép tùy biến mượt lộ trình đi bộ `foot-walking` và chủ động xử lý chế độ ngoại tuyến. |
| **Dịch vụ gửi OTP 2FA** | **Brevo HTTPS REST API (Cổng 443)** | `Nodemailer` qua giao thức SMTP (Cổng 465/587) | Nền tảng Render Cloud chặn kết nối SMTP đi ra ngoài để chống phát tán thư rác; truyền tải qua HTTPS cổng `443` giúp email OTP đến hộp thư Gmail thật ổn định chỉ trong 1–2 giây. |
| **Tạo & Hiển thị ảnh** | **`QrBitmapEncoder` thuần Java + `BitmapFactory` + VectorDrawable** | Nhúng thêm thư viện `ZXing` và `Glide`/`Coil` | Giảm dung lượng APK, cho phép kiểm thử ma trận QR trực tiếp trên JVM JUnit 4 và đặc biệt **không lưu cache ảnh chân dung eKYC nhạy cảm** ra bộ nhớ đệm của thư viện bên thứ ba. |

---

# CHƯƠNG 3. PHÂN TÍCH VÀ THIẾT KẾ HỆ THỐNG

Nhằm đảm bảo báo cáo trình bày chặt chẽ, trực quan và bám sát bảng tiêu chí chấm điểm (`docs/MARKING.md`), toàn bộ thiết kế hệ thống của NovaPay được chuẩn hóa thành **8 sơ đồ kỹ thuật cốt lõi** (*Hình 3-1* đến *Hình 3-8*).

## 3.1 Phân tích Use Case tổng quát và đặc tả các Use Case trọng tâm

Hệ thống phục vụ hai tác nhân chính là **Khách hàng (`CUSTOMER`)** và **Nhân viên Ngân hàng (`OFFICER`)** với ranh giới quyền hạn rõ ràng như thể hiện trong *Hình 3-1*:

> **[VỊ TRÍ CHÈN ẢNH SƠ ĐỒ — Hình 3-1]**  
> - **Loại sơ đồ:** Sơ đồ Use Case tổng quát hệ thống Ngân hàng số NovaPay (*Use Case Diagram*).  
> - **Nội dung cần thể hiện trong ảnh:**  
>   1. Hai tác nhân bên ngoài: **Khách hàng (`CUSTOMER`)** và **Nhân viên Ngân hàng (`OFFICER`)**.  
>   2. Năm nhóm chức năng bên trong hệ thống: *(a) Xác thực & Định danh* (Đăng nhập/Sinh trắc học, eKYC, Hồ sơ, Quản trị bảo mật PIN/Hạn mức/Khóa thẻ); *(b) Quản lý Tài khoản & Tiết kiệm* (Xem 3 loại TK & Biểu đồ Thu/Chi, Mở/Tất toán tiết kiệm, Lịch sử & Sao kê PDF, Thông báo biến động 2 chiều); *(c) Giao dịch Tài chính & Tiện ích* (Chuyển tiền/QR, Nạp Stripe/Rút/Trả góp, Hóa đơn/Nạp ĐT/Đặt vé, kèm `<<include>>` OTP Email 180s và `<<extend>>` Bảo mật 3 lớp `> 5M` + Biên lai PNG/PDF); *(d) Bản đồ & Trợ lý AI*; *(e) Cổng quản trị Bank Officer* (Dashboard & `audit_logs`, Quản lý/Khóa KH, Duyệt eKYC & Hậu kiểm, Quản lý lãi suất).  
> - **Tệp nguồn tham khảo (nếu kết xuất tự động):** `docs/diagrams/02_use_case.puml`.

<div align="center"><i>Hình 3-1. Sơ đồ Use Case tổng quan hệ thống Ngân hàng số NovaPay</i></div>

<br/>

Các Use Case nghiệp vụ trọng tâm nhất của hệ thống được đặc tả chi tiết trong *Bảng 3-1*:

*Bảng 3-1. Đặc tả tóm tắt các Use Case cốt lõi*

| Tên Use Case | Tác nhân | Tiền điều kiện (*Pre-conditions*) | Luồng xử lý chính & Hậu điều kiện (*Post-conditions*) |
| :--- | :---: | :--- | :--- |
| **UC01 — Chuyển tiền & Bảo mật Thích ứng** | `CUSTOMER` | `users.status == "ACTIVE"`, `ekyc.status == "VERIFIED"`, tài khoản nguồn là `CHECKING` đang `ACTIVE` và đủ số dư + hạn mức ngày. | Khởi tạo bản nháp tại `/transactions/initiate`. Nếu `amount > 5.000.000 ₫`, bắt buộc vượt qua `FaceCheck` (`>= 0.80`) rồi mới nhận OTP Email và xác thực `BiometricPrompt`. Sau khi khớp OTP tại `/transactions/confirm`, Server trừ/cộng số dư nguyên tử, ghi `ledger` hai chiều và phát thông báo biến động tức thì. |
| **UC02 — Nạp tiền Quốc tế Stripe 2 Pha** | `CUSTOMER` | Đã đăng nhập, sở hữu tài khoản `CHECKING` đang `ACTIVE`, số tiền nạp nguyên dương `<= 100.000.000 ₫`. | Gửi OTP 2FA tại `/initiate` (miễn `FaceCheck`); xác nhận OTP tại `/confirm` để tạo `PaymentIntent` (`PENDING_PAYMENT`) và mở `PaymentSheet`. Khi thanh toán thẻ thành công, `/payments/finalize` đối soát ngược với Stripe và cộng tiền idempotent đúng 1 lần duy nhất. |
| **UC03 — Mở & Tất toán Sổ tiết kiệm trực tuyến** | `CUSTOMER` | Đã xác thực `eKYC == VERIFIED`, tài khoản `CHECKING` nguồn đủ số tiền gốc tối thiểu `1.000.000 ₫`. | Chọn kỳ hạn từ bảng `interest_rates`, nhập mã Smart PIN 6 số (nếu đã bật). Server kiểm tra chống TOCTOU trong Transaction, trích tiền `CHECKING`, mở tài khoản `SAVING` mới (hoặc đóng sổ `CLOSED` và cộng gốc + lãi khi tất toán) kèm thông báo `SAVING`. |
| **UC04 — Duyệt eKYC & Quản trị Khách hàng** | `OFFICER` | Đăng nhập tài khoản có Custom Claim `role == "OFFICER"`. | Tạo khách hàng mới (tự động rollback Auth nếu lỗi Firestore), duyệt ảnh eKYC `PENDING → VERIFIED/REJECTED`, đổi biểu lãi suất kỳ hạn hoặc khóa khách hàng (`LOCKED` + `revokeRefreshTokens`). Mọi thao tác bắt buộc ghi vết vào `audit_logs`. |

---

## 3.2 Thiết kế Kiến trúc Thành phần và Triển khai Hệ thống

*Hình 3-2* mô tả kiến trúc thành phần và triển khai tổng thể của NovaPay, thể hiện rõ ranh giới giữa 4 tầng trên ứng dụng Android, đường ống bảo mật trên máy chủ Render HTTPS, hạ tầng Google Firebase và 4 dịch vụ đám mây bên ngoài:

> **[VỊ TRÍ CHÈN ẢNH SƠ ĐỒ — Hình 3-2]**  
> - **Loại sơ đồ:** Sơ đồ Kiến trúc Hệ thống & Triển khai Đám mây Lai (*System & Deployment Architecture Diagram*).  
> - **Nội dung cần thể hiện trong ảnh:**  
>   1. **Khối Android Native Client (Java 17):** Chia 4 tầng `Presentation` (MVVM 28 màn hình + `MainActivity` Role Guard), `Core` (`SessionManager`, `BalanceAlertSubscriber`, `BiometricHelper`, `CameraX/ML Kit`, 2 Hilt Qualifiers), `Domain` (Pure Java 17 — UseCases & Repository Interfaces) và `Data` (`*RepositoryImpl`, `AuthInterceptor`, `Firestore DataSources`, `OpenRouteServiceApi`).  
>   2. **Khối Backend Server trên Render Cloud (HTTPS):** Node.js 20 + TypeScript + Express gồm Middleware bảo mật (`Helmet`, `RateLimiter`, `verifyIdToken`), 8 REST Routers và 8 Core Services.  
>   3. **Khối Google Firebase Cloud:** `Firebase Authentication` (JWT + Custom Claim `role`) và `Cloud Firestore` (`firestore.rules` + 5 Composite Indexes), nối Kênh 1 (Realtime Read với Android) và Kênh 2 (ACID `runTransaction` với Backend Admin SDK).  
>   4. **Khối External Cloud Services:** `Stripe API`, `Brevo Email REST API`, `Google Gemini 2.5 Flash` (nối từ Backend) và `OpenRouteService API` (nối từ `@OpenRouteRetrofit`).  
> - **Tệp nguồn tham khảo (nếu kết xuất tự động):** `docs/diagrams/01_system_architecture.puml`.

<div align="center"><i>Hình 3-2. Sơ đồ kiến trúc tổng thể Hybrid Cloud và Clean Architecture của hệ thống NovaPay</i></div>

---

## 3.3 Thiết kế Cơ sở dữ liệu Cloud Firestore và Sơ đồ ERD

Cơ sở dữ liệu NoSQL hướng tài liệu trên **Cloud Firestore** được chuẩn hóa gồm **15 Collections cấp gốc** và **3 Subcollections** (`beneficiaries`, `notifications`, `ledger`). Việc tách `ledger` và `notifications` thành subcollections giúp lịch sử giao dịch tăng trưởng không giới hạn mà không bao giờ vượt ngưỡng `1 MiB/document`, đồng thời việc tách riêng `ekyc_images` và `user_secrets` khỏi `users` giúp tối ưu băng thông đọc hồ sơ và cô lập tuyệt đối mã băm PIN giao dịch ở phía máy chủ.

> **[VỊ TRÍ CHÈN ẢNH SƠ ĐỒ — Hình 3-3]**  
> - **Loại sơ đồ:** Sơ đồ Thực thể Liên kết cơ sở dữ liệu (*Database Entity-Relationship Diagram — ERD*).  
> - **Nội dung cần thể hiện trong ảnh:** Mối quan hệ khóa chính (`PK`), khóa ngoại (`FK`) và lực lượng (`1:1`, `1:N`) giữa **15 Collections gốc** (`users`, `ekyc_images`, `accounts`, `transactions`, `transaction_drafts`, `face_checks`, `interest_rates`, `banks`, `branches`, `utility_bills`, `service_catalog`, `orders`, `chat_sessions/messages`, `audit_logs`, `user_secrets`) và **3 Subcollections** (`users/{uid}/beneficiaries`, `users/{uid}/notifications`, `accounts/{accountId}/ledger`).  
> - **Tệp nguồn tham khảo (nếu kết xuất tự động):** `docs/diagrams/06_database_erd.puml`.

<div align="center"><i>Hình 3-3. Sơ đồ thực thể liên kết (ERD) của 15 Collections & 3 Subcollections trên Cloud Firestore</i></div>

<br/>

Cấu trúc chi tiết và quy tắc phân quyền truy cập của từng bảng dữ liệu được mô tả đầy đủ trong *Bảng 3-2*, kèm theo 5 chỉ mục phức hợp tối ưu truy vấn trong *Bảng 3-3*:

*Bảng 3-2. Mô tả cấu trúc dữ liệu và phân quyền truy cập của 15 Collections & 3 Subcollections*

| STT | Đường dẫn Collection | Các trường dữ liệu trọng tâm & Kiểu lưu trữ | Quyền Đọc (Client) | Quyền Ghi (Client) & Cơ chế Bảo vệ |
| :---: | :--- | :--- | :--- | :--- |
| **1** | `users/{uid}` | `role` (`CUSTOMER`/`OFFICER`), `fullName`, `email`, `phone`, `nationalId`, `dob`, `address`, `avatarUrl`, `status` (`ACTIVE`/`LOCKED`), `ekyc`, `security` | Chính chủ hoặc `OFFICER` | Chỉ cho sửa 4 khóa (`phone`, `address`, `avatarUrl`, `updatedAt`) khi `ACTIVE`; tạo/khóa/duyệt qua Backend |
| **1a** | `users/{uid}/beneficiaries/{id}` | `name`, `accountNumber`, `bankCode`, `alias` (`String` có kiểm tra giới hạn độ dài) | Chính chủ hoặc `OFFICER` | Chính chủ đang `ACTIVE` (`isActiveUserDoc(uid)`) |
| **1b** | `users/{uid}/notifications/{notifId}` | `category` (`BALANCE`/`SAVING`/`SECURITY`/`EKYC`), `direction` (`IN`/`OUT`), `title`, `body`, `amount` (`Long`), `balanceAfter` (`Long`), `txId`, `deepLink`, `read` (`Boolean`), `createdAt` | Chỉ chính chủ (`isOwner`) | Chỉ cho `update` đúng trường `read` (`bool`) khi `ACTIVE`; tạo mới 100% từ Admin SDK |
| **2** | `ekyc_images/{uid}` | `imageBase64` (JPEG `< 200KB`), `capturedAt` (`Timestamp`), `capturedBy` (`uid`) | Chính chủ hoặc `OFFICER` | **Cấm (`false`)** — Server kiểm tra JPEG SOI `0xFF 0xD8` tại `/ekyc/submit` |
| **3** | `accounts/{accountId}` | `ownerUid`, `type` (`CHECKING`/`SAVING`/`MORTGAGE`), `accountNumber` (unique 10 số), `status` (`ACTIVE`/`FROZEN`/`CLOSED`), `balance` (`Long`), `perTxnLimit`, `dailyLimit`, `saving?`, `mortgage?` | Chính chủ hoặc `OFFICER` | **Cấm (`false`)** — Tra cứu tên người nhận đi qua `GET /accounts/lookup` |
| **3a** | `accounts/{accountId}/ledger/{entryId}` | `txId`, `direction` (`IN`/`OUT`), `amount` (`Long`), `balanceAfter` (`Long`), `description`, `createdAt` | Chủ tài khoản cha hoặc `OFFICER` | **Cấm (`false`)** — Chỉ ghi nguyên tử trong ACID Transaction |
| **4** | `transactions/{txId}` | `type` (9 loại giao dịch), `fromAccountId`, `toAccountId?`, `toExternal?`, `amount` (`Long`), `fee` (`Long`), `status`, `faceChecked`, `biometricPassed`, `otpVerified`, `meta`, `initiatedBy`, `createdAt` | Người gửi, chủ TK nhận nội bộ hoặc `OFFICER` | **Cấm (`false`)** — Chứng từ giao dịch bất biến sau khi hạch toán |
| **5** | `transaction_drafts/{draftId}` | `uid`, `payload`, `status` (`PENDING_FACE` → `PENDING_OTP` → `PENDING_PAYMENT` → `CONFIRMED`/`EXPIRED`), `otpHash` (`SHA-256`), `otpExpiresAt`, `attempts`, `faceAttempts`, `stripeIntentId?`, `expireAt` | **Cấm tuyệt đối (`false`)** | **Cấm (`false`)** — Chứa `otpHash` và `stripeIntentId`, chỉ Admin SDK đọc/ghi |
| **6** | `face_checks/{draftId}` | `uid`, `txId?`, `imageBase64` (`< 200KB`), `manualAuditRequired` (`Boolean`), `createdAt` | Chỉ `OFFICER` | **Cấm (`false`)** — Lưu bằng chứng sinh trắc học giao dịch `> 5.000.000 ₫` |
| **7** | `interest_rates/{rateId}` | `termMonths` (`0` = không kỳ hạn, `1..36`), `annualRate` (`Double`), `effectiveFrom`, `updatedBy`, `updatedAt` | Người dùng đã đăng nhập | **Cấm (`false`)** — Officer cập nhật qua `/officer/interest-rates` |
| **8** | `banks/{bankCode}` | `name`, `logoUrl`, `napasCode` | Người dùng đã đăng nhập | **Cấm (`false`)** — Danh mục ngân hàng liên kết Napas |
| **9** | `branches/{branchId}` | `name`, `address`, `location` (`GeoPoint`), `phone`, `openHours`, `type` (`BRANCH`/`ATM`) | Người dùng đã đăng nhập | **Cấm (`false`)** — 10 điểm giao dịch & ATM tại TP.HCM |
| **10** | `utility_bills/{billId}` | `type` (`ELECTRIC`/`WATER`), `providerName`, `customerCode`, `customerName`, `period`, `amount` (`Long`), `dueDate`, `status` (`UNPAID`/`PAID`), `txId?` | Người dùng đã đăng nhập | **Cấm (`false`)** — Gạch nợ `UNPAID → PAID` nguyên tử khi thanh toán |
| **11** | `service_catalog/{itemId}` | `category` (`FLIGHT`/`MOVIE`/`HOTEL`/`PRODUCT`), `title`, `price` (`Long`), `details`, `imageUrl` | Người dùng đã đăng nhập | **Cấm (`false`)** — Server đối chiếu `amount == price * quantity` |
| **12** | `orders/{orderId}` | `uid`, `itemId`, `category`, `quantity`, `totalAmount` (`Long`), `status`, `txId`, `createdAt` | Chủ đơn hàng hoặc `OFFICER` | **Cấm (`false`)** — Khởi tạo nguyên tử cùng giao dịch `SERVICE_PAYMENT` |
| **13** | `chat_sessions/{uid}/messages/{msgId}` | `role` (`USER`/`BOT`), `text`, `actions: [{label, deepLink}]`, `createdAt` | Chỉ chính chủ (`isOwner`) | **Cấm (`false`)** — Ghi theo lô `WriteBatch` tại `POST /ai/chat` |
| **14** | `audit_logs/{logId}` | `actorUid`, `action`, `targetId`, `details` (`Map`), `createdAt` | Chỉ `OFFICER` | **Cấm (`false`)** — Nhật ký kiểm toán bất biến (*Append-only*) |
| **15** | `user_secrets/{uid}` | `pinHash` (`HMAC-SHA256`), `pinSalt`, `failedAttempts` (`Max 5`), `lockUntil` (`Long`), `createdAt`, `updatedAt` | **Cấm tuyệt đối (`false`)** | **Cấm (`false`)** — Chỉ `PinService` trên Backend được phép truy cập |

*Bảng 3-3. Cấu hình 5 Composite Indexes tối ưu truy vấn trong `firestore.indexes.json`*

| STT | Collection / Subcollection | Tổ hợp Trường Đánh chỉ mục | Mục đích Tối ưu Truy vấn Thực tế |
| :---: | :--- | :--- | :--- |
| 1 | `ledger` (`COLLECTION`) | `direction ASC` + `createdAt DESC` | Lọc nhanh bút toán Tiền vào (`IN`) hoặc Tiền ra (`OUT`) theo thứ tự mới nhất tại `TransactionHistoryFragment`. |
| 2 | `transactions` (`COLLECTION`) | `fromAccountId ASC` + `status ASC` + `createdAt ASC` | Tính tổng hạn mức chi tiêu trong ngày (`querySpentTodayVnd`) theo biên ngày `Asia/Ho_Chi_Minh` ngay trong Transaction. |
| 3 | `utility_bills` (`COLLECTION`) | `customerCode ASC` + `status ASC` | Tra cứu tức thì các hóa đơn Điện/Nước chưa thanh toán (`UNPAID`) theo mã khách hàng. |
| 4 | `orders` (`COLLECTION`) | `uid ASC` + `createdAt DESC` | Truy xuất danh sách đơn hàng dịch vụ mới nhất của từng khách hàng. |
| 5 | `face_checks` (`COLLECTION`) | `manualAuditRequired ASC` + `createdAt DESC` | Truy xuất hàng đợi hậu kiểm ảnh khuôn mặt giao dịch giá trị cao trên `OfficerDashboardFragment`. |


## 3.4 Thiết kế Sơ đồ Lớp (Class Diagrams) toàn hệ thống

Nhằm phản ánh đầy đủ cấu trúc hướng đối tượng của **350 lớp Java 17** phía ứng dụng di động và **16 mô-đun TypeScript** phía máy chủ đám mây mà vẫn giữ bố cục hình vẽ sắc nét trên khổ giấy in A4, nhóm phân rã thiết kế tĩnh của toàn hệ thống thành **3 Sơ đồ Lớp UML 2.5** bổ trợ chặt chẽ cho nhau, thể hiện trọn vẹn 5 kiểu quan hệ hướng đối tượng chuẩn: **Kế thừa (`--|>`)**, **Thực thi giao diện (`..|>`)**, **Hợp thành chặt (`*--`)**, **Kết tập (`o--`)** và **Liên kết / Phụ thuộc (`-->`, `..>`)**.

### 3.4.1 Sơ đồ Lớp Thực thể Miền nghiệp vụ (Domain Model — Pure Java 17)

Toàn bộ các lớp thực thể trong gói `com.novabank.novapay.domain.model.**` được viết bằng **Java 17 thuần túy**, đóng gói trạng thái bất biến (`final`), sử dụng kiểu số nguyên 64-bit `long` cho mọi đại lượng tiền tệ VNĐ, `java.time.Instant` cho mốc thời gian và `GeoLocation` cho tọa độ không gian (*Hình 3-4*):

> **[VỊ TRÍ CHÈN ẢNH SƠ ĐỒ — Hình 3-4]**  
> - **Loại sơ đồ:** Sơ đồ Lớp UML 1/3 — Thực thể miền nghiệp vụ (`Domain Model Class Diagram` — Pure Java 17).  
> - **Nội dung cần thể hiện trong ảnh:** Các lớp thực thể bất biến trong `com.novabank.novapay.domain.model.**` chia theo 4 gói:  
>   1. *User, Security & Notifications:* `User`, `EkycInfo`, `UserSecurityInfo`, `SecurityStatus`, `AppNotification`, `Beneficiary`.  
>   2. *Accounts, Savings & Mortgage:* `Account`, `SavingInfo`, `SavingCalculator`, `MortgageInfo`, `InterestRate`.  
>   3. *Transactions, Ledger & Utilities:* `Transaction`, `TransactionDraft`, `LedgerEntry`, `UtilityBill`, `ServiceItem`, `Order`.  
>   4. *Branch GIS, AI Chat & Officer Audit:* `Branch`, `GeoLocation`, `ChatMessage`, `ChatAction`, `AuditLog`, `FaceCheckAudit`.  
> - **Tệp nguồn tham khảo (nếu kết xuất tự động):** `docs/diagrams/03_class_domain_model.puml`.

<div align="center"><i>Hình 3-4. Sơ đồ lớp các thực thể miền nghiệp vụ cốt lõi (Domain Model — Pure Java 17)</i></div>

<br/>

**Giải thích thiết kế hướng đối tượng trên *Hình 3-4*:**
- **Quan hệ hợp thành chặt (*Composition* `*--`):** Đối tượng `User` sở hữu vòng đời của `EkycInfo` và `UserSecurityInfo`; thực thể đa hình `Account` hợp thành `SavingInfo` (khi `type == SAVING`) hoặc `MortgageInfo` (khi `type == MORTGAGE`); `Branch` hợp thành tọa độ `GeoLocation` và `ChatMessage` hợp thành danh sách nút điều hướng `ChatAction`.
- **Thực thể giàu hành vi (*Rich Domain Model*):** Thay vì chỉ tạo các lớp chứa dữ liệu thụ động (*Anemic Model*), các quy tắc cốt lõi được đặt ngay bên trong thực thể miền như `Account.isActiveChecking()` (ràng buộc chỉ tài khoản thanh toán đang hoạt động mới được làm nguồn chi), `SavingInfo.calculateMonthlyProfit()`, `SavingCalculator.previewSettlement()` và `GeoLocation.distanceMetersTo()`.

### 3.4.2 Sơ đồ Lớp Kiến trúc 4 Tầng Android Clean Architecture & MVVM

*Hình 3-5* minh họa luồng phụ thuộc một chiều từ tầng giao diện (`presentation`) đi qua các `UseCase` và giao diện `Repository` của tầng nghiệp vụ (`domain`), đồng thời tầng dữ liệu (`data`) thực thi nguyên lý đảo ngược phụ thuộc (*Dependency Inversion Principle*) thông qua Dagger Hilt:

> **[VỊ TRÍ CHÈN ẢNH SƠ ĐỒ — Hình 3-5]**  
> - **Loại sơ đồ:** Sơ đồ Lớp UML 2/3 — Kiến trúc 4 tầng Android Clean Architecture kết hợp MVVM và Dagger Hilt.  
> - **Nội dung cần thể hiện trong ảnh:** Quan hệ kế thừa (`--|> BaseFragment<VB>`), thực thi giao diện (`*RepositoryImpl ..|> Repository`), hợp thành (`*ViewModel *-- UiState / SingleLiveEvent`) và liên kết phụ thuộc một chiều giữa 4 tầng:  
>   1. *Core Infrastructure:* `BaseFragment<VB>`, `UiState<T>`, `SingleLiveEvent<T>`, `AppResult<T>`, `SessionManager`, `BiometricHelper`, `BiometricQuickLoginStore`, `BalanceAlertSubscriber`, `SystemNotificationHelper`, `StatementPdfExporter`, `QrBitmapEncoder`.  
>   2. *Presentation Layer (MVVM):* `MainActivity`, các cặp `*Fragment` – `*ViewModel` tiêu biểu (`CustomerHome`, `Transfer`, `OpenSaving`, `NotificationCenter`, `SecuritySettings`, `BranchMap`, `Chatbot`, `OfficerDashboard`, `OtpVerificationDialog`, `FaceCheckDialog`).  
>   3. *Domain Layer:* Các giao diện `Repository` (`AccountRepository`, `TransactionRepository`, `SavingsRepository`...) và các lớp `UseCase`.  
>   4. *Data Layer:* `AuthInterceptor`, các Retrofit APIs (`@BackendRetrofit`, `@OpenRouteRetrofit`), Firestore DataSources và các lớp `*RepositoryImpl`.  
> - **Tệp nguồn tham khảo (nếu kết xuất tự động):** `docs/diagrams/04_class_android_clean_mvvm.puml`.

<div align="center"><i>Hình 3-5. Sơ đồ lớp kiến trúc 4 tầng Android Clean Architecture kết hợp MVVM và Dagger Hilt</i></div>

<br/>

**Giải thích các điểm nhấn kỹ thuật trên *Hình 3-5*:**
- **Kế thừa khung giao diện chuẩn (`--|> BaseFragment<VB>`):** Toàn bộ 28 màn hình đều kế thừa lớp trừu tượng `BaseFragment<VB>`, tự động giải phóng tham chiếu `binding = null` trong `onDestroyView()` và hủy đăng ký `CompositeDisposable` nhằm ngăn chặn triệt để lỗi rò rỉ bộ nhớ (*Memory Leak*).
- **Tách biệt trạng thái bền vững và sự kiện một lần:** Các `ViewModel` hợp thành `LiveData<UiState<T>>` cho dữ liệu hiển thị sống sót qua xoay màn hình và dùng `SingleLiveEvent<T>` (dựa trên `AtomicBoolean`) cho các sự kiện chỉ được kích hoạt đúng 1 lần như mở hộp thoại OTP, chuyển sang màn hình biên lai hoặc hiển thị thông báo lỗi.
- **Cô lập phụ thuộc mạng bằng Qualifier:** Các giao diện API nội bộ (`TransactionApi`, `SavingsApi`, `SecurityApi`...) được gắn `@BackendRetrofit` đi qua `AuthInterceptor`, trong khi `OpenRouteServiceApi` dùng `@OpenRouteRetrofit` độc lập để không bao giờ gửi nhầm `Firebase ID Token` của khách hàng sang máy chủ bản đồ bên ngoài.

### 3.4.3 Sơ đồ Lớp Máy chủ Backend Node.js + TypeScript + Express

*Hình 3-6* mô tả kiến trúc phân lớp rõ ràng phía máy chủ `server/src/` gồm tầng Cấu hình & Middleware bảo vệ, **8 bộ định tuyến RESTful (`Routers`)** và **8 dịch vụ miền (`Domain Services`)** hỗ trợ tiêm phụ thuộc phục vụ kiểm thử đơn vị độc lập:

> **[VỊ TRÍ CHÈN ẢNH SƠ ĐỒ — Hình 3-6]**  
> - **Loại sơ đồ:** Sơ đồ Lớp UML 3/3 — Kiến trúc máy chủ Backend Node.js 20 + TypeScript + Express.  
> - **Nội dung cần thể hiện trong ảnh:** Mối liên kết giữa 3 gói mô-đun trong `server/src/`:  
>   1. *Config & Security Middleware:* `AppConfig`, `AuthMiddleware` (`requireAuth`, `requireOfficer`), `RateLimiter`.  
>   2. *8 Express REST Routers:* `AccountsRouter`, `TransactionsRouter`, `PaymentsRouter`, `SavingsRouter`, `SecurityRouter`, `EkycRouter`, `OfficerRouter`, `AiRouter`.  
>   3. *8 Domain Services:* `OtpService`, `PinService`, `EmailService`, `FaceCheckService`, `StripeService`, `NotificationsService`, `OfficerAuditService`, `ChatbotService`.  
> - **Tệp nguồn tham khảo (nếu kết xuất tự động):** `docs/diagrams/05_class_backend_server.puml`.

<div align="center"><i>Hình 3-6. Sơ đồ lớp máy chủ Backend Node.js 20 + TypeScript + Express (8 Routers & 8 Services)</i></div>

---

## 3.5 Thiết kế Sơ đồ Tuần tự (Sequence Diagrams) các luồng nghiệp vụ cốt lõi

Trong số các quy trình tương tác của hệ thống, nhóm trình bày chi tiết **2 Sơ đồ Tuần tự phức hợp nhất** đại diện cho cơ chế bảo mật đa tầng và tính nhất quán giao dịch phân tán của NovaPay.

### 3.5.1 Luồng Chuyển tiền giá trị cao (`> 5.000.000 ₫`): Bảo mật 3 lớp, ACID Transaction & Thông báo 2 chiều

Khi khách hàng thực hiện bất kỳ giao dịch chi tiêu hoặc chuyển khoản nào có mệnh giá vượt ngưỡng `5.000.000 ₫` (không thuộc loại nạp tiền `GATEWAY`), hệ thống kích hoạt tuần tự 3 chốt chặn xác thực độc lập kết hợp hạch toán nguyên tử hai pha và đẩy thông báo biến động số dư cho cả hai đầu giao dịch (*Hình 3-7*):

> **[VỊ TRÍ CHÈN ẢNH SƠ ĐỒ — Hình 3-7]**  
> - **Loại sơ đồ:** Sơ đồ Tuần tự UML 1/2 — Luồng Chuyển tiền giá trị cao (`> 5.000.000 ₫`).  
> - **Nội dung cần thể hiện trong ảnh:** Các đường đời (*Lifelines*) gồm `Khách hàng`, `TransferFragment & OtpDialog`, `FaceCheckDialog (CameraX + ML Kit)`, `BiometricHelper`, `NovaPay Server`, `Gemini 2.5 Flash`, `Brevo API` và `Cloud Firestore` qua 4 giai đoạn:  
>   1. *Bước 0:* Tra cứu người nhận nội bộ qua `GET /accounts/lookup` (chỉ trả tên, không lộ số dư).  
>   2. *Lớp 1/3:* `POST /transactions/initiate` tạo bản nháp `PENDING_FACE` (chưa gửi OTP) → Kiểm tra Liveness tại biên → `POST /transactions/face-check` đối chiếu Gemini (`confidence >= 0.80`, nhánh rẽ lỗi `403 FACE_MISMATCH` / `503 Fail-Closed`, khớp mới gửi Email OTP qua Brevo và chuyển sang `PENDING_OTP`).  
>   3. *Lớp 2/3:* Xác thực vân tay / mã khóa thiết bị qua `BiometricHelper` (`biometricPassed = true`).  
>   4. *Lớp 3/3:* `POST /transactions/confirm` kiểm tra `SHA-256` OTP → Thực thi `runTransaction()` 2 pha trừ/cộng số dư, ghi `ledger`, ghi `notifications` 2 chiều (`OUT`/`IN`) → `SnapshotListener` đẩy System Push Notification tức thì trên cả 2 thiết bị và mở màn hình Biên lai.  
> - **Tệp nguồn tham khảo (nếu kết xuất tự động):** `docs/diagrams/07_seq_high_value_3layer_transfer.puml`.

<div align="center"><i>Hình 3-7. Sơ đồ tuần tự luồng chuyển tiền giá trị cao (> 5.000.000 ₫) với bảo mật 3 lớp và thông báo 2 chiều</i></div>

<br/>

**Phân tích an toàn máy trạng thái trên *Hình 3-7*:**
1. **Chống tấn công bỏ bước (*Anti-Step-Skipping*):** Tại bước khởi tạo `/transactions/initiate`, bản nháp được đặt ở trạng thái `"PENDING_FACE"` và chưa hề sinh mã OTP (`otpExpiresAt: null`). Mọi nỗ lực dùng công cụ ngoài gọi tắt tới `/transactions/confirm` hoặc `/transactions/resend-otp` đều bị từ chối ngay với mã lỗi `400/403 FACE_REQUIRED`.
2. **Nguyên tắc *Zero-False-Acceptance* và *Fail-Closed*:** Máy chủ kiểm tra ảnh gốc trong `ekyc_images/{uid}` trước khi gọi AI (chặn ngay ảnh mẫu 1x1 pixel bằng `403 EKYC_FACE_NOT_FOUND`), yêu cầu Gemini `2.5 Flash` phân tích cấu trúc xương mặt với `temperature: 0.0` và chỉ chấp nhận khi `matched === true && confidence >= 0.80`. Nếu dịch vụ AI quá tải hoặc quá hạn 15 giây, máy chủ trả về `503 SERVICE_UNAVAILABLE` thay vì tự động cho qua.

### 3.5.2 Luồng Nạp tiền trực tuyến qua cổng quốc tế Stripe PaymentSheet (2-Phase & Idempotent Finalize)

Đối với giao dịch nạp tiền từ thẻ thanh toán quốc tế (`type == "GATEWAY"`), bài toán khó nhất là đảm bảo **không cộng tiền khống khi khách hàng chưa thanh toán xong** và **không bao giờ cộng tiền hai lần khi mạng di động chập chờn**. *Hình 3-8* mô tả cơ chế phối hợp hai pha giữa NovaPay Server, Stripe Cloud và Firestore:

> **[VỊ TRÍ CHÈN ẢNH SƠ ĐỒ — Hình 3-8]**  
> - **Loại sơ đồ:** Sơ đồ Tuần tự UML 2/2 — Luồng Nạp tiền trực tuyến qua cổng quốc tế Stripe `PaymentSheet`.  
> - **Nội dung cần thể hiện trong ảnh:** Các đường đời gồm `Khách hàng`, `DepositWithdrawFragment & OtpDialog`, `StripeGatewayHelper (PaymentSheet)`, `NovaPay Server`, `Brevo API`, `Stripe Cloud API` và `Cloud Firestore` qua 3 bước:  
>   1. `POST /transactions/initiate` (`type: "GATEWAY"`) gửi OTP 2FA qua Brevo (miễn `FaceCheck` & `Biometric`).  
>   2. `POST /transactions/confirm` xác thực OTP, gọi Stripe tạo `PaymentIntent` (`currency: "vnd"`, gắn `idempotencyKey`), chuyển bản nháp sang `PENDING_PAYMENT` (**chưa cộng số dư**) và trả `clientSecret` để mở `PaymentSheet` thanh toán thẻ `4242...`.  
>   3. `POST /payments/finalize` đối soát `paymentIntents.retrieve()` đạt `succeeded`, chạy `runTransaction()` kiểm tra khóa chống trùng (`draft.status == "CONFIRMED"` thì trả kết quả ngay không cộng lần 2; nếu `PENDING_PAYMENT` thì cộng số dư `CHECKING`, ghi `ledger IN`, `notifications` và khóa thành `CONFIRMED`).  
> - **Tệp nguồn tham khảo (nếu kết xuất tự động):** `docs/diagrams/08_seq_stripe_gateway_idempotent.puml`.

<div align="center"><i>Hình 3-8. Sơ đồ tuần tự luồng nạp tiền quốc tế qua cổng Stripe PaymentSheet đảm bảo tính Idempotent</i></div>

### 3.5.3 Khái quát các luồng tuần tự mở rộng (Tiết kiệm trực tuyến, Khóa khẩn cấp RBAC & AI Chatbot)

Bên cạnh hai sơ đồ trên, ba quy trình tương tác quan trọng khác của hệ thống được thiết kế đồng bộ theo cùng chuẩn an toàn:
- **Luồng Mở & Tất toán Sổ tiết kiệm trực tuyến (`POST /savings/open`, `POST /savings/:id/settle`):** Khách hàng chọn tài khoản nguồn `CHECKING`, kỳ hạn gửi và xác thực Smart PIN 6 chữ số (nếu đã bật). Trong một `db.runTransaction()`, máy chủ đọc lại `user_secrets/{uid}` để chống lỗi tranh chấp thời điểm (*TOCTOU*), trích/cộng gốc kèm lãi nguyên `long` vào tài khoản thanh toán, đóng sổ (`status: "CLOSED"` khi tất toán), sinh 2 bút toán `ledger` đối ứng và đẩy thông báo `SAVING` tức thì.
- **Luồng Quản trị Officer & Cưỡng bức khóa tài khoản thời gian thực (`PATCH /officer/customers/:uid`):** Khi Nhân viên ngân hàng chuyển trạng thái khách hàng sang `"LOCKED"`, `OfficerAuditService` đồng thời cập nhật `users/{uid}`, vô hiệu hóa tài khoản trên Firebase Auth (`disabled: true`), thu hồi toàn bộ phiên `revokeRefreshTokens(uid)` và ghi `audit_logs`. Trên thiết bị khách hàng đang mở ứng dụng, `SessionManager` nhận sự kiện `SnapshotListener` trong chưa đầy 1 giây và tự động gọi `signOut()` đưa thẳng về màn hình Đăng nhập.
- **Luồng Trợ lý ảo AI Chatbot (`POST /ai/chat`):** Máy chủ xác minh vai trò `CUSTOMER` đang `ACTIVE`, tổng hợp số dư `CHECKING` và dòng tiền thu/chi tháng hiện tại làm ngữ cảnh cho mô hình Gemini `2.5 Flash`, lọc danh sách nút hành động qua tập trắng `ALLOWED_CHAT_DEEPLINKS` (hoặc tự động chuyển sang bộ xử lý ngôn ngữ tự nhiên Tiếng Việt theo luật khi quá tải `429`), sau đó lưu cặp tin nhắn `USER`/`BOT` nguyên tử bằng `WriteBatch`.

---

# CHƯƠNG 4. TRIỂN KHAI, MINH HỌA GIAO DIỆN VÀ KIỂM THỬ HỆ THỐNG

## 4.1 Tổ chức cấu trúc mã nguồn dự án

Toàn bộ dự án **NovaPay** được tổ chức dưới dạng kho mã nguồn hợp nhất (*Monorepo*) gồm ứng dụng di động Android (`app/`), máy chủ xử lý giao dịch (`server/`), quy tắc cơ sở dữ liệu (`firestore.rules`, `firestore.indexes.json`) và hệ thống tài liệu kỹ thuật (`docs/`):

```text
NovaPay/
├── app/src/
│   ├── main/
│   │   ├── java/com/novabank/novapay/       # 350 tệp mã nguồn Java 17 (0 tệp Kotlin)
│   │   │   ├── NovaPayApp.java              # @HiltAndroidApp khởi tạo ứng dụng
│   │   │   ├── MainActivity.java            # Single-Activity Host & Role/DeepLink Guard
│   │   │   ├── core/                        # Hạ tầng dùng chung (24 lớp)
│   │   │   │   ├── common/                  # AppResult, UiState, SingleLiveEvent, Constants
│   │   │   │   ├── di/                      # 5 Hilt Modules & 2 Qualifiers (@Backend/@OpenRoute)
│   │   │   │   ├── notification/            # BalanceAlertSubscriber, SystemNotificationHelper
│   │   │   │   ├── security/                # SessionManager, BiometricHelper, BiometricQuickLoginStore
│   │   │   │   ├── ui/                      # BaseFragment<VB>, StateLayoutHelper
│   │   │   │   └── util/                    # CurrencyFormatter, QrBitmapEncoder, StatementPdfExporter...
│   │   │   ├── domain/                      # Lõi nghiệp vụ Pure Java 17 (128 lớp, 0 import android.*)
│   │   │   │   ├── model/                   # 57 thực thể & Enum (account, transaction, user, branch...)
│   │   │   │   ├── repository/              # 17 giao diện Repository (RxJava 3 Single/Observable)
│   │   │   │   └── usecase/                 # 54 lớp UseCase chia theo phân hệ nghiệp vụ
│   │   │   ├── data/                        # Triển khai dữ liệu & Tích hợp mạng (94 lớp)
│   │   │   │   ├── dto/ & mapper/           # 38 lớp DTO JSON & các bộ chuyển đổi hai chiều
│   │   │   │   ├── remote/firebase/         # 8 nguồn dữ liệu Firebase Auth & Firestore Realtime
│   │   │   │   ├── remote/server/ & maps/   # AuthInterceptor, 10 Retrofit APIs & OpenRouteServiceApi
│   │   │   │   └── repository/              # 17 lớp *RepositoryImpl thực thi giao diện Domain
│   │   │   └── presentation/                # Giao diện người dùng MVVM (102 lớp — 28 màn hình)
│   │   │       ├── auth/ & ekyc/            # Đăng nhập, Quên MK, Chụp eKYC, FaceCheck, FaceLivenessEvaluator
│   │   │       ├── customer/                # Home, Account, Savings, Transfer, Payment, Bill, Topup,
│   │   │       │                            # Service, History, Notification, Security, Branch, Chatbot, Profile
│   │   │       └── officer/                 # Dashboard, Quản lý KH, Duyệt eKYC & FaceCheck, Lãi suất
│   │   └── res/
│   │       ├── layout/                      # 50 tệp XML Layout (Material Design 3 + ViewBinding)
│   │       ├── navigation/                  # nav_graph.xml + 3 sub-graphs (core, money, officer_map_ai)
│   │       └── values/                      # themes, colors, styles & 3 tệp strings_*.xml (100% Tiếng Việt)
│   └── test/java/com/novabank/novapay/      # 32 bộ kiểm thử đơn vị JUnit 4 (95 test cases)
├── server/
│   ├── src/
│   │   ├── index.ts & config.ts             # Khởi tạo Express, Helmet, CORS, RateLimit & Firebase Admin
│   │   ├── middleware/                      # auth.ts (verifyIdToken checkRevoked), rateLimit.ts
│   │   ├── routes/                          # 8 Routers: accounts, transactions, payments, savings,
│   │   │                                    #            security, ekyc, officer, ai
│   │   └── services/                        # 8 Services: otp, pin, email, faceCheck, stripe,
│   │                                        #             notifications, officerAudit, chatbot
│   ├── scripts/                             # seed.ts, deployRules.ts, cleanupDrafts.ts, runTests.ts
│   └── test/                                # 10 tệp kiểm thử tự động node:test + MockFirestore (61 cases)
├── firestore.rules & firestore.indexes.json # Quy tắc bảo mật trường dữ liệu & 5 Composite Indexes
└── docs/                                    # Đặc tả đề bài, tiêu chí chấm điểm, 8 sơ đồ .puml & báo cáo
```

Quy mô thành phần mã nguồn và danh mục đầy đủ **16 điểm cuối RESTful API** trên máy chủ Backend được thống kê trong *Bảng 4-1* và *Bảng 4-2*:

*Bảng 4-1. Thống kê quy mô tệp mã nguồn và thành phần kiến trúc thực tế của dự án*

| Phân hệ / Tầng kiến trúc | Số lượng tệp thực tế | Thành phần kỹ thuật tiêu biểu |
| :--- | :---: | :--- |
| **Android `core/` + Root** | `26` tệp `.java` | `NovaPayApp`, `MainActivity`, `BaseFragment<VB>`, `SessionManager`, `BalanceAlertSubscriber`, `QrBitmapEncoder`, `StatementPdfExporter` |
| **Android `domain/` (Pure Java 17)** | `128` tệp `.java` | `57` lớp Model/Enum, `17` giao diện `Repository`, `54` lớp `UseCase` độc lập hoàn toàn với Android SDK |
| **Android `data/`** | `94` tệp `.java` | `38` lớp DTO, `14` lớp Mapper, `8` Firestore DataSources, `11` Retrofit Interfaces + `AuthInterceptor`, `17` `*RepositoryImpl` |
| **Android `presentation/` (MVVM)** | `102` tệp `.java` | **28 màn hình** (`Fragment` / `DialogFragment` + `ViewModel` + `UiState`) & `11` lớp `RecyclerView.Adapter` |
| **Tài nguyên Giao diện & Điều hướng** | `50` XML Layouts + `4` Nav XML | `1` Activity, `21` Fragments, `7` Dialogs, `18` Item Cards, `3` State Views (`loading`, `empty`, `error`), `3` tệp `strings_*.xml` |
| **Backend Server (`server/src/`)** | `20` tệp `.ts` | `8` Express Routers (`16` REST Endpoints), `8` Domain Services, `2` Middlewares, `config.ts`, `index.ts` |
| **Kiểm thử Tự động (`test/`)** | `42` tệp kiểm thử | `32` lớp JUnit 4 Android (`95` test cases) + `10` tệp `node:test` Backend (`61` test cases) = **`156` bài kiểm thử** |

*Bảng 4-2. Danh mục 16 RESTful API Endpoints vận hành trên máy chủ Backend (`server/src/routes/`)*

| STT | Phương thức & Đường dẫn | Phân quyền | Chức năng xử lý chính & Mã lỗi nghiệp vụ đặc trưng |
| :---: | :--- | :---: | :--- |
| 1 | `GET /health` | Công khai | Kiểm tra trạng thái hoạt động của máy chủ trên Render (`200 {"ok": true}`). |
| 2 | `GET /accounts/lookup` | Đã đăng nhập | Tra cứu tối giản `{accountId, accountNumber, ownerName}` của tài khoản `CHECKING` đang `ACTIVE` (`ACCOUNT_NOT_FOUND`, `ACCOUNT_NOT_ACTIVE`). |
| 3 | `POST /transactions/initiate` | `CUSTOMER` (`VERIFIED`) | Kiểm tra số dư, hạn mức đơn/ngày (`UTC+7`), tạo bản nháp `PENDING_FACE` (`> 5M`) hoặc gửi OTP 180s (`PENDING_OTP`). |
| 4 | `POST /transactions/face-check` | `CUSTOMER` | Đối chiếu ảnh chụp trực tiếp với `ekyc_images/{uid}` qua Gemini `2.5 Flash` (`>= 0.80`), đạt mới phát hành Email OTP (`FACE_MISMATCH`, `EKYC_FACE_NOT_FOUND`, `503`). |
| 5 | `POST /transactions/resend-otp` | `CUSTOMER` | Cấp lại mã OTP 6 chữ số mới và đặt lại thời hạn 180s khi đã qua bước `FaceCheck` (`FACE_REQUIRED`, `DRAFT_EXPIRED`). |
| 6 | `POST /transactions/confirm` | `CUSTOMER` | Xác minh `faceChecked`, `biometricPassed`, `SHA-256` OTP; hạch toán nguyên tử 8 loại giao dịch hoặc khởi tạo Stripe `PaymentIntent` (`PENDING_PAYMENT`). |
| 7 | `POST /payments/finalize` | `CUSTOMER` | Đối soát `PaymentIntent.status === "succeeded"` với Stripe và cộng tiền Idempotent đúng 1 lần vào tài khoản `CHECKING`. |
| 8 | `POST /savings/open` | `CUSTOMER` (`VERIFIED`) | Trích tiền gốc từ `CHECKING` (`>= 1.000.000 ₫`), mở sổ `SAVING` theo biểu lãi suất hiện hành (có xác thực Smart PIN chống TOCTOU). |
| 9 | `POST /savings/:accountId/settle` | `CUSTOMER` (`VERIFIED`) | Tất toán sổ tiết kiệm đúng hạn (hưởng lãi kỳ hạn) hoặc trước hạn (hưởng lãi không kỳ hạn theo ngày thực gửi), cộng gốc + lãi về `CHECKING`. |
| 10 | `GET /security/status` | `CUSTOMER` | Truy vấn trạng thái đã thiết lập Smart PIN 6 chữ số và thời điểm mở khóa nếu đang bị khóa tạm 15 phút. |
| 11 | `POST /security/pin/set` | `CUSTOMER` | Thiết lập lần đầu hoặc đổi mã PIN giao dịch 6 số (lưu băm `HMAC-SHA256` tại `user_secrets/{uid}`). |
| 12 | `PATCH /security/accounts/:id/controls` | `CUSTOMER` | Tạm khóa khẩn cấp (`FROZEN`), mở khóa lại (`ACTIVE`) hoặc điều chỉnh `perTxnLimit`, `dailyLimit` (bắt buộc xác thực PIN nếu đã cài). |
| 13 | `POST /ekyc/submit` | `CUSTOMER` / `OFFICER` | Kiểm tra định dạng JPEG SOI `0xFF 0xD8` (`<= 200KB`), lưu `ekyc_images/{targetUid}` và chuyển trạng thái hồ sơ sang `"PENDING"`. |
| 14 | `POST /ekyc/review` | Chỉ `OFFICER` | Phê duyệt `"VERIFIED"` hoặc từ chối `"REJECTED"` hồ sơ eKYC, ghi `audit_logs` và đẩy thông báo `EKYC` cho khách hàng. |
| 15 | `POST /officer/customers`<br>`PATCH /officer/customers/:uid`<br>`POST /officer/interest-rates` | Chỉ `OFFICER` | Tạo hồ sơ khách hàng kèm tài khoản `CHECKING` mặc định, sửa thông tin / khóa khẩn cấp (`revokeRefreshTokens`) và cập nhật biểu lãi suất (ghi `audit_logs`). |
| 16 | `POST /ai/chat` | `CUSTOMER` (`ACTIVE`) | Tư vấn tài chính thông minh dựa trên số dư & thu/chi tháng thực tế qua Gemini `2.5 Flash` + lọc Whitelist DeepLink + NLP Fallback. |

---

## 4.2 Các kỹ thuật xử lý và giải thuật trọng tâm đã hiện thực hóa

### 4.2.1 Giao dịch nguyên tử hai pha (2-Phase ACID Transaction) và toán học tiền tệ số nguyên `long`

Trên Cloud Firestore, một giao dịch `db.runTransaction(async (tx) => ...)` bắt buộc mọi thao tác đọc (`tx.get`) phải diễn ra trước mọi thao tác ghi (`tx.set`, `tx.update`). Tại `server/src/routes/transactions.ts` và `savings.ts`, nhóm tổ chức nghiêm ngặt quy trình hạch toán 2 pha:
- **Pha 1 (Đọc và Kiểm định đồng thời):** Đọc bản nháp tươi (`freshDraft`), hồ sơ chủ tài khoản (`ACTIVE`, `VERIFIED`), tài khoản nguồn `CHECKING` (`balance`, `perTxnLimit`, `dailyLimit`), tài khoản nhận/hóa đơn/đơn hàng và truy vấn tổng số tiền đã chi tiêu trong ngày theo múi giờ Việt Nam (`UTC+7`) qua khoảng nửa mở $[T_{\text{start}}, T_{\text{end}})$ tính bởi `getHoChiMinhDayBounds(nowMs)`.
- **Pha 2 (Ghi nguyên tử liên bảng):** Cập nhật số dư mới, ghi sổ cái kép `ledger` (`OUT`/`IN` kèm `balanceAfter`), tạo chứng từ `transactions/{txId}`, gạch nợ `utility_bills` / tạo `orders` / dời kỳ trả nợ `mortgage.nextDueDate`, khóa bản nháp sang `"CONFIRMED"` và chèn thông báo biến động vào `users/{uid}/notifications`.

Đối với nghiệp vụ **Tiết kiệm trực tuyến** (`SavingInfo.java`, `SavingCalculator.java` và `savings.ts`), toàn bộ kết quả tiền lãi đều được quy đổi về số nguyên 64-bit `long` (đơn vị: VNĐ) thông qua phép làm tròn xuống tường minh theo 3 công thức toán học:

- **Tiền lãi dự kiến mỗi tháng** (cập nhật thời gian thực qua `Observable.combineLatest` khi Nhân viên ngân hàng đổi lãi suất):

$$
\text{monthlyProfit} = \left\lfloor \frac{\text{balance} \times r_{\text{annual}}}{12 \times 100} \right\rfloor
$$

- **Tổng tiền lãi tất toán đúng hoặc sau ngày đáo hạn** ($\text{now} \ge \text{maturityDate}$):

$$
I_{\text{maturity}} = \left\lfloor \frac{P \times r_{\text{annual}} \times m}{12 \times 100} \right\rfloor
$$

- **Tiền lãi tất toán trước hạn** ($\text{now} < \text{maturityDate}$, hưởng lãi suất không kỳ hạn $r_{\text{demand}}$ theo số ngày thực gửi $d = \lfloor (\text{now} - \text{openedAt}) / 86.400.000 \rfloor$):

$$
I_{\text{early}} = \left\lfloor \frac{P \times r_{\text{demand}} \times d}{365 \times 100} \right\rfloor
$$

Đoạn mã hiện thực hóa thực tế tại `domain/model/account/SavingCalculator.java`:

```java
public static long calculateMaturityInterest(long principal, double annualRate, int termMonths) {
    if (principal <= 0L || annualRate <= 0.0 || termMonths <= 0) {
        return 0L;
    }
    return (long) Math.floor((principal * annualRate * termMonths) / (12.0 * 100.0));
}

public static long calculateEarlyInterest(long principal, double demandAnnualRate, long daysHeld) {
    if (principal <= 0L || demandAnnualRate <= 0.0 || daysHeld <= 0L) {
        return 0L;
    }
    return (long) Math.floor((principal * demandAnnualRate * daysHeld) / (365.0 * 100.0));
}
```

### 4.2.2 Xác thực đa nhân tố thích ứng 3 lớp (`> 5.000.000 ₫`): Edge ML Kit Liveness, Gemini Zero-False-Acceptance, Sinh trắc học & OTP SHA-256

Để ngăn chặn hiệu quả cả rủi ro dùng ảnh tĩnh/nhắm mắt tại thiết bị lẫn rủi ro người lạ cầm điện thoại của chủ tài khoản để rút tiền lớn, quy trình kiểm định sinh trắc học được chia làm hai tầng:

1. **Kiểm tra thực thể sống tại biên (*Edge Liveness Check* — `FaceLivenessEvaluator.java`):** Khung hình từ camera trước (`CameraX ImageAnalysis`) chỉ mở khóa nút chụp khi thỏa mãn đồng thời 4 ràng buộc hình học:

$$
N_{\text{face}} = 1, \quad P_{\text{leftEye}} \ge 0.70, \quad P_{\text{rightEye}} \ge 0.70, \quad |\theta_{\text{EulerY}}| \le 15^\circ \;\land\; |\theta_{\text{EulerZ}}| \le 15^\circ
$$

2. **Đối chiếu khuôn mặt *Zero-False-Acceptance* trên đám mây (`server/src/services/faceCheck.ts`):** Trước khi gọi AI, máy chủ từ chối ngay các hồ sơ chưa chụp ảnh chân dung thật (bao gồm ảnh mẫu 1x1 pixel sinh bởi kịch bản khởi tạo) bằng mã `403 EKYC_FACE_NOT_FOUND`. Sau đó, ảnh gốc `ekyc_images/{uid}` và ảnh chụp trực tiếp (đã nén JPEG `< 200KB` qua `ImageCompressor.java`) được gửi tới mô hình đa phương thức **Gemini `2.5 Flash`** với `temperature: 0.0` và ngưỡng chấp nhận kép `matched === true && confidence >= 0.80`:

```typescript
export function isPlaceholderOrEmptyEkycImage(rawBase64: string): boolean {
  const cleaned = stripBase64DataUrlPrefix(rawBase64);
  return !cleaned || cleaned === SAMPLE_SEED_PLACEHOLDER_BASE64;
}
// Tại verifyFaceMatch(): nếu Gemini trả HTTP 429 hoặc timeout > 15s -> ném FaceCheckUnavailableError (503)
// Chỉ chấp nhận khi: parsed.matched === true && parsed.confidence >= confidenceThreshold (0.80)
```

Sau khi vượt qua `FaceCheck` và `BiometricHelper` (`BIOMETRIC_STRONG | DEVICE_CREDENTIAL`), mã OTP 6 chữ số sinh bằng `crypto.randomInt(0, 1_000_000)` được đối chiếu băm một chiều `SHA-256("${draftId}:${otp}")` bằng hàm hằng thời gian **`crypto.timingSafeEqual`** nhằm triệt tiêu tấn công đo thời gian phản hồi (*Timing Attack*).

### 4.2.3 Thông báo biến động số dư 2 chiều thời gian thực và khử trùng lặp System Push Notification

Một bài toán thực tế thường gặp khi dùng `SnapshotListener` trên Firestore để phát thông báo hệ thống Android là hiện tượng **bắn lặp hàng loạt thông báo lịch sử cũ** mỗi khi người dùng tắt mở lại ứng dụng. Tại `core/notification/BalanceAlertSubscriber.java`, nhóm giải quyết triệt để bằng cơ chế đánh dấu lô khởi tạo đầu tiên (`initialSnapshotConsumed`) kết hợp tập khử trùng `seenNotifIds` và dung sai lệch đồng hồ `15 giây`:

```java
if (!initialSnapshotConsumed) {
    initialSnapshotConsumed = true;
    for (DocumentChange change : snapshots.getDocumentChanges()) {
        seenNotifIds.add(change.getDocument().getId());
    }
    return; // Chỉ ghi nhận mốc nền, không bắn lại thông báo cũ khi vừa đăng nhập
}
for (DocumentChange change : snapshots.getDocumentChanges()) {
    if (change.getType() != DocumentChange.Type.ADDED) continue;
    if (!seenNotifIds.add(change.getDocument().getId())) continue;
    AppNotification notification = NotificationMapper.fromSnapshot(change.getDocument());
    if (notification != null && !notification.isRead()) {
        systemNotificationHelper.showNotification(notification);
    }
}
```

Nhờ cơ chế này, ngay khoảnh khắc Khách hàng A hoàn tất chuyển khoản nội bộ cho Khách hàng B, cả hai thiết bị đang mở đều nhận được thanh thông báo nổi mức ưu tiên cao (`NotificationManager.IMPORTANCE_HIGH`) kèm huy hiệu số thông báo chưa đọc trên Trang chủ, chạm vào tự động mở biên lai giao dịch tương ứng.

### 4.2.4 Phân quyền đa tầng (`firestore.rules`), bảo vệ Smart PIN `HMAC-SHA256` chống TOCTOU và cưỡng bức khóa phiên tức thì

Trên tầng cơ sở dữ liệu, tệp `firestore.rules` khóa hoàn toàn quyền ghi từ Client vào các bảng tài chính và kiểm soát chặt đến từng khóa thuộc tính đối với các thao tác cá nhân hợp lệ khi tài khoản đang `"ACTIVE"`:

```javascript
match /notifications/{notifId} {
  allow read: if isOwner(uid);
  allow update: if isOwner(uid)
    && isActiveUserDoc(uid)
    && resource != null
    && request.resource.data.diff(resource.data).affectedKeys().hasOnly(["read"])
    && request.resource.data.read is bool;
  allow create, delete: if false;
}
match /user_secrets/{uid} {
  allow read, write: if false; // Cô lập hoàn toàn mã băm PIN giao dịch phía Backend
}
```

Đồng thời, tại `server/src/routes/security.ts` và `savings.ts`, việc kiểm tra Smart PIN 6 số không chỉ thực hiện bên ngoài mà còn đọc lại `tx.get(db.collection("user_secrets").doc(uid))` ngay trong **Pha 1 của `db.runTransaction()`**, đóng kín lỗ hổng tranh chấp thời điểm kiểm tra – sử dụng (*TOCTOU*) nếu có nhiều yêu cầu gửi song song.

### 4.2.5 Giải thuật khoảng cách mặt cầu Haversine, tự động định tâm bản đồ và trợ lý ảo AI Chatbot Whitelist DeepLink

Tại phân hệ Bản đồ chi nhánh (`GeoLocation.java` và `FindNearestBranchUseCase.java`), khoảng cách thực tế giữa vị trí người dùng $(\phi_1, \lambda_1)$ và từng điểm giao dịch $(\phi_2, \lambda_2)$ trên mặt cầu Trái Đất ($R = 6.371.000\text{ m}$) được tính chính xác bằng công thức **Haversine**:

$$
a = \sin^2\left(\frac{\Delta\phi}{2}\right) + \cos\phi_1 \cdot \cos\phi_2 \cdot \sin^2\left(\frac{\Delta\lambda}{2}\right), \qquad d = 2R \cdot \operatorname{atan2}\left(\sqrt{a},\; \sqrt{1 - a}\right)
$$

Nếu sau **5 giây** thiết bị chưa bắt được tín hiệu vệ tinh GPS, bị từ chối quyền vị trí hoặc tọa độ trả về cách cụm chi nhánh TP.HCM quá **100 km** (`isBeyondRadius` — thường gặp trên máy ảo Android mặc định đặt tọa độ ở bang California, Hoa Kỳ), `BranchMapFragment` tự động định tâm về tọa độ **Trường Đại học Tôn Đức Thắng (`10.7326, 106.6998` — Quận 7, TP.HCM)** để luôn gắn nhãn chi nhánh **Gần nhất** và vẽ mượt đường đi bộ `foot-walking` qua `OpenRouteServiceApi`.

Tương tự, trên phân hệ **AI Chatbot** (`server/src/services/chatbot.ts`), mọi đường dẫn `deepLink` do mô hình ngôn ngữ lớn sinh ra đều bị lọc qua tập hợp cố định `ALLOWED_CHAT_DEEPLINKS` (`novapay://home`, `novapay://transfer`, `novapay://bills`, `novapay://topup`, `novapay://services`, `novapay://branches`, `novapay://transactions?range=month`, `novapay://profile`, `novapay://ekyc`) nhằm ngăn chặn hoàn toàn nguy cơ *Prompt Injection* chèn URI độc hại hoặc dẫn sang màn hình quản trị của `OFFICER`.

---

## 4.3 Minh họa giao diện thực tế các phân hệ Customer và Bank Officer

### 4.3.1 Triết lý thiết kế giao diện Clean Fintech Minimalism (Material Design 3)

Toàn bộ **50 tệp giao diện XML** của NovaPay được thiết kế đồng nhất theo phong cách ngân hàng số thương mại hiện đại (*Clean Fintech Minimalism*): sử dụng bảng màu xanh lục bảo – xanh đen tin cậy (`colors.xml`), thẻ bo góc phân cấp rõ ràng (`MaterialCardView`), chữ số tiền tệ có độ tương phản cao định dạng chuẩn `1.250.000 ₫` (`CurrencyFormatter`), hỗ trợ đầy đủ 4 trạng thái giao diện chuẩn (`view_loading_state.xml`, `view_empty_state.xml`, `view_error_state.xml` kèm nút *Thử lại*) và hiển thị 100% Tiếng Việt có dấu.

> **Hướng dẫn hoàn thiện bản Word nộp Khoa:** Khi chuyển báo cáo sang tệp Microsoft Word theo mẫu của Khoa CNTT, nhóm chụp trực tiếp 18 màn hình thực tế trên thiết bị/máy ảo từ 4 tài khoản kiểm thử đã khởi tạo (`SEED_MEMBER_1..3_EMAIL` và `SEED_OFFICER_EMAIL`) và chèn đúng vào 18 vị trí khung hình dưới đây.

### 4.3.2 Minh họa giao diện phân hệ Khách hàng cá nhân (`CUSTOMER` — Hình 4-1 đến Hình 4-14)

1. **Màn hình Đăng nhập, Quên mật khẩu & Đăng nhập nhanh sinh trắc học (`fragment_login.xml`, `dialog_forgot_password.xml`):** Cho phép đăng nhập Email/Mật khẩu, gửi liên kết khôi phục mật khẩu qua Firebase Auth và chạm một nút mở khóa nhanh bằng vân tay lưu trong Android Keystore (`BiometricQuickLoginStore`).

> **[VỊ TRÍ CHÈN ẢNH GIAO DIỆN APP — Hình 4-1]**  
> - **Màn hình cần chụp (ghép 2 ảnh cạnh nhau):** (1) Giao diện **Đăng nhập** của ứng dụng NovaPay (hiển thị ô nhập Email/Mật khẩu, nút *Quên mật khẩu* và nút *Đăng nhập bằng sinh trắc học*); (2) Hộp thoại xác thực vân tay của hệ thống Android (`BiometricPrompt`) hoặc hộp thoại *Khôi phục mật khẩu*.

<div align="center"><i>Hình 4-1. Giao diện Đăng nhập ứng dụng NovaPay và hộp thoại đăng nhập nhanh bằng vân tay</i></div>

2. **Trang chủ Khách hàng & Biểu đồ Dòng tiền Tháng (`fragment_customer_home.xml`):** Hiển thị lời chào cá nhân hóa, nút ẩn/hiện tổng số dư khả dụng, danh sách thẻ tài khoản (`AccountCardAdapter`), huy hiệu đếm thông báo chưa đọc trên biểu tượng chuông, lưới phím tắt dịch vụ nhanh và biểu đồ cột `MPAndroidChart` đối chiếu trực quan Tổng thu (`IN`) — Tổng chi (`OUT`) trong tháng.

> **[VỊ TRÍ CHÈN ẢNH GIAO DIỆN APP — Hình 4-2]**  
> - **Màn hình cần chụp (Tài khoản `CUSTOMER` — Nguyễn Phan Nhân):** Chụp màn hình **Trang chủ Khách hàng (`CustomerHomeFragment`)** hiển thị rõ thẻ tổng số dư VNĐ, biểu tượng chuông có số thông báo chưa đọc, danh sách thẻ tài khoản, cụm nút chức năng nhanh và phần **Biểu đồ cột Thu/Chi trong tháng** ở nửa dưới màn hình.

<div align="center"><i>Hình 4-2. Giao diện Trang chủ Khách hàng (`CustomerHomeFragment`) cập nhật số dư thời gian thực</i></div>

3. **Chi tiết 3 loại Tài khoản Ngân hàng (`fragment_checking_detail.xml`, `fragment_saving_detail.xml`, `fragment_mortgage_detail.xml`):** Được định tuyến tự động qua `AccountRouterFragment` (`novapay://accounts/{id}`), gồm: Tài khoản Thanh toán `1001000001` tích hợp mã QR nhận tiền cá nhân (`QrBitmapEncoder`), Tài khoản Tiết kiệm `2001000001` hiển thị lãi dự kiến hàng tháng thời gian thực và Khoản vay Thế chấp `3001000001` kèm nút thanh toán kỳ nợ tiếp theo.

> **[VỊ TRÍ CHÈN ẢNH GIAO DIỆN APP — Hình 4-3]**  
> - **Màn hình cần chụp (ghép 3 ảnh cạnh nhau của tài khoản `CUSTOMER`):**  
>   (1) Màn hình **Chi tiết Tài khoản Thanh toán (`1001000001`)** có hiển thị hình mã QR nhận tiền cá nhân;  
>   (2) Màn hình **Chi tiết Tài khoản Tiết kiệm (`2001000001`)** hiển thị kỳ hạn, lãi suất `%/năm` và tiền lãi dự kiến hàng tháng;  
>   (3) Màn hình **Chi tiết Khoản vay Thế chấp (`3001000001`)** hiển thị dư nợ còn lại, kỳ hạn trả và nút *Thanh toán kỳ nợ*.

<div align="center"><i>Hình 4-3. Giao diện chi tiết 3 loại tài khoản `CHECKING`, `SAVING` và `MORTGAGE`</i></div>

4. **Mở Sổ Tiết Kiệm Trực Tuyến & Dự Toán Tất Toán (`fragment_open_saving.xml`, `dialog_settle_saving.xml`):** Cho phép khách hàng chọn kỳ hạn gửi từ bảng lãi suất động, xem trước ngày đáo hạn và tiền lãi dự kiến bằng số nguyên `long`, xác thực Smart PIN 6 số để mở sổ hoặc xem bảng đối chiếu lãi đúng hạn / trước hạn khi tất toán.

> **[VỊ TRÍ CHÈN ẢNH GIAO DIỆN APP — Hình 4-4]**  
> - **Màn hình cần chụp (ghép 2 ảnh cạnh nhau):** (1) Màn hình **Mở sổ tiết kiệm trực tuyến (`OpenSavingFragment`)** khi đã chọn kỳ hạn (ví dụ 6 hoặc 12 tháng), nhập số tiền gốc và hiển thị tiền lãi dự kiến cuối kỳ; (2) Hộp thoại **Xác nhận tất toán sổ tiết kiệm (`dialog_settle_saving`)** hiển thị tiền gốc, tiền lãi tính đến hiện tại và ô nhập Smart PIN 6 số.

<div align="center"><i>Hình 4-4. Giao diện Mở mới và Tất toán sổ tiết kiệm trực tuyến (`OpenSavingFragment`)</i></div>

5. **Chuyển tiền Nội bộ / Liên ngân hàng & Quét mã QR (`fragment_transfer.xml`, `fragment_qr_scanner.xml`):** Tự động tra cứu và hiển thị tên chủ tài khoản nhận nội bộ qua `GET /accounts/lookup`, hỗ trợ chọn nhanh từ Danh bạ thụ hưởng, chuyển liên ngân hàng Napas hoặc đưa camera quét mã QR NovaPay để tự điền thông tin chuyển khoản.

> **[VỊ TRÍ CHÈN ẢNH GIAO DIỆN APP — Hình 4-5]**  
> - **Màn hình cần chụp (ghép 2 ảnh cạnh nhau):** (1) Màn hình **Chuyển tiền (`TransferFragment`)** khi nhập số tài khoản `1001000002` và ứng dụng đã tự động tra cứu hiển thị tên người nhận **Nguyễn Thành Đạt**, số tiền và nội dung chuyển khoản; (2) Màn hình **Quét mã QR (`QrScannerFragment`)** mở camera với khung ngắm quét mã.

<div align="center"><i>Hình 4-5. Giao diện Chuyển khoản (`TransferFragment`) và Quét mã QR CameraX + ML Kit</i></div>

6. **Định danh eKYC & Đối chiếu Khuôn mặt Giao dịch Giá trị cao (`fragment_ekyc_capture.xml`, `dialog_face_check.xml`):** Hiển thị khung dẫn hướng khuôn mặt trực tiếp trên CameraX kèm thông điệp chỉ dẫn thời gian thực của `FaceLivenessEvaluator` (*"Vui lòng nhìn thẳng"*, *"Vui lòng mở cả hai mắt"*, *"Khuôn mặt hợp lệ — Sẵn sàng chụp"*) và cảnh báo đỏ tức thì nếu đối chiếu Gemini báo `403 FACE_MISMATCH`.

> **[VỊ TRÍ CHÈN ẢNH GIAO DIỆN APP — Hình 4-6]**  
> - **Màn hình cần chụp (ghép 2 ảnh cạnh nhau):** (1) Giao diện camera trước khi chụp **Định danh eKYC / Hộp thoại `FaceCheckDialogFragment`** (giao dịch `> 5.000.000 ₫`) đang báo trạng thái xanh *"Khuôn mặt hợp lệ — Sẵn sàng chụp"*; (2) Cảnh báo màu đỏ trên `FaceCheckDialogFragment` khi đưa khuôn mặt người khác vào và bị từ chối (`FACE_MISMATCH`).

<div align="center"><i>Hình 4-6. Giao diện chụp định danh eKYC và hộp thoại `FaceCheckDialogFragment` cho giao dịch trên 5 triệu đồng</i></div>

7. **Xác thực Sinh trắc học Thiết bị & Hộp thoại OTP Email 180 giây (`dialog_otp_verification.xml`):** Hiển thị trạng thái hoàn tất quét vân tay/mã khóa máy, đồng hồ đếm ngược 180 giây của mã OTP 6 chữ số gửi qua Brevo tới hộp thư Gmail thật và nút gửi lại mã khi hết hạn.

> **[VỊ TRÍ CHÈN ẢNH GIAO DIỆN APP — Hình 4-7]**  
> - **Màn hình cần chụp (ghép 2 ảnh cạnh nhau):** (1) Hộp thoại **Xác thực OTP 2FA (`OtpVerificationDialogFragment`)** trên ứng dụng đang đếm ngược thời gian hiệu lực `180 giây` và ô nhập 6 chữ số OTP; (2) Ảnh chụp email chứa mã OTP 6 chữ số thực tế gửi từ hệ thống NovaPay (qua Brevo) trong hộp thư đến **Gmail**.

<div align="center"><i>Hình 4-7. Giao diện xác thực OTP 2FA (`OtpVerificationDialogFragment`) và thư điện tử OTP nhận trên Gmail</i></div>

8. **Biên lai Giao dịch Điện tử & Chia sẻ PNG/PDF (`fragment_receipt.xml`):** Hiển thị mã tham chiếu giao dịch, thời gian hạch toán, tài khoản nguồn/đích, số tiền, trạng thái thành công và cho phép xuất ảnh PNG hoặc tài liệu PDF chia sẻ qua `FileProvider`.

> **[VỊ TRÍ CHÈN ẢNH GIAO DIỆN APP — Hình 4-8]**  
> - **Màn hình cần chụp:** Màn hình **Biên lai giao dịch thành công (`ReceiptFragment`)** hiển thị dấu tích hoàn tất, số tiền VNĐ, mã giao dịch `txId`, thông tin người gửi/người nhận, thời gian thực hiện và hai nút *Chia sẻ ảnh PNG* / *Xuất PDF*.

<div align="center"><i>Hình 4-8. Giao diện Biên lai điện tử (`ReceiptFragment`) sau khi hoàn tất giao dịch</i></div>

9. **Nạp tiền Trực tuyến qua Cổng Quốc tế Stripe (`fragment_deposit_withdraw.xml`):** Tích hợp trực tiếp giao diện `PaymentSheet` chuẩn PCI-DSS của Stripe Android SDK cho phép thanh toán bằng thẻ quốc tế kiểm thử (`4242 4242 4242 4242`) và đối soát cộng tiền tự động qua `/payments/finalize`.

> **[VỊ TRÍ CHÈN ẢNH GIAO DIỆN APP — Hình 4-9]**  
> - **Màn hình cần chụp (ghép 2 ảnh cạnh nhau):** (1) Màn hình **Nạp / Rút tiền (`DepositWithdrawFragment`)** ở tab *Nạp trực tuyến* đã nhập số tiền nạp; (2) Khung thanh toán thẻ quốc tế **Stripe `PaymentSheet`** bật lên trên màn hình với thông tin thẻ kiểm thử `4242 4242 4242 4242`.

<div align="center"><i>Hình 4-9. Giao diện Nạp tiền trực tuyến tích hợp `Stripe PaymentSheet`</i></div>

10. **Thanh toán Hóa đơn Điện/Nước, Nạp Điện thoại & Đặt Dịch vụ (`fragment_utility_bill.xml`, `fragment_phone_topup.xml`, `fragment_service_booking.xml`):** Bao phủ trọn vẹn hệ sinh thái tiện ích gồm tra cứu gạch nợ hóa đơn theo mã khách hàng (VD: `PE0100009999`), nạp tiền viễn thông 4 nhà mạng và đặt vé máy bay, xem phim, khách sạn, mua sắm kèm tab theo dõi đơn hàng đã đặt.

> **[VỊ TRÍ CHÈN ẢNH GIAO DIỆN APP — Hình 4-10]**  
> - **Màn hình cần chụp (ghép 3 ảnh cạnh nhau):**  
>   (1) Màn hình **Thanh toán Hóa đơn Điện/Nước (`UtilityBillFragment`)** hiển thị hóa đơn tra cứu theo mã khách hàng;  
>   (2) Màn hình **Nạp tiền Điện thoại (`PhoneTopupFragment`)** chọn nhà mạng và mệnh giá thẻ nạp;  
>   (3) Màn hình **Đặt vé & Dịch vụ (`ServiceBookingFragment`)** hiển thị danh mục Vé máy bay / Xem phim / Khách sạn / Mua sắm.

<div align="center"><i>Hình 4-10. Tổ hợp màn hình tiện ích thanh toán hóa đơn, viễn thông và thương mại điện tử</i></div>

11. **Lịch sử Giao dịch Đa Bộ lọc & Xuất Sao kê PDF (`fragment_transaction_history.xml`):** Cho phép tìm kiếm theo từ khóa, lọc theo chiều Tiền vào (`IN`) / Tiền ra (`OUT`), lọc nhanh khoảng thời gian và kết xuất tệp **Sao kê Tài khoản PDF** chuyên nghiệp (`StatementPdfExporter`).

> **[VỊ TRÍ CHÈN ẢNH GIAO DIỆN APP — Hình 4-11]**  
> - **Màn hình cần chụp (ghép 2 ảnh cạnh nhau):** (1) Màn hình **Lịch sử giao dịch (`TransactionHistoryFragment`)** hiển thị thanh tìm kiếm, các Chip lọc `Tất cả / Tiền vào / Tiền ra`, danh sách biến động sổ cái và nút *Xuất Sao kê PDF*; (2) Ảnh chụp tệp **Sao kê điện tử PDF** sau khi mở xem trên thiết bị.

<div align="center"><i>Hình 4-11. Giao diện Lịch sử giao dịch (`TransactionHistoryFragment`) và bản xem trước Sao kê PDF</i></div>

12. **Trung tâm Thông báo Biến động Số dư & System Push Notification (`fragment_notification_center.xml`):** Phân loại trực quan các thông báo `Biến động số dư`, `Tiết kiệm`, `Bảo mật`, `Định danh eKYC`, hỗ trợ đánh dấu đã đọc từng mục hoặc toàn bộ và đồng bộ với thanh thông báo hệ thống Android.

> **[VỊ TRÍ CHÈN ẢNH GIAO DIỆN APP — Hình 4-12]**  
> - **Màn hình cần chụp (ghép 2 ảnh cạnh nhau):** (1) Màn hình **Trung tâm Thông báo (`NotificationCenterFragment`)** hiển thị danh sách thông báo biến động số dư `+`/`-` VNĐ và các bộ lọc danh mục; (2) Thanh trạng thái hệ thống Android (*Notification Shade / Heads-up Banner*) đang hiển thị **System Push Notification** biến động số dư của NovaPay.

<div align="center"><i>Hình 4-12. Giao diện Trung tâm Thông báo (`NotificationCenterFragment`) và System Push Notification</i></div>

13. **Hồ sơ Cá nhân & Trung tâm Cài đặt Bảo mật (`fragment_profile.xml`, `fragment_security_settings.xml`):** Cho phép cập nhật thông tin liên hệ, quản lý danh bạ thụ hưởng, bật/tắt đăng nhập sinh trắc học, thiết lập/đổi **Smart PIN 6 số**, điều chỉnh hạn mức giao dịch ngày/lần và kích hoạt **Tạm khóa khẩn cấp (`FROZEN`)** tài khoản thanh toán.

> **[VỊ TRÍ CHÈN ẢNH GIAO DIỆN APP — Hình 4-13]**  
> - **Màn hình cần chụp (ghép 2 ảnh cạnh nhau):** (1) Màn hình **Hồ sơ cá nhân (`ProfileFragment`)** hiển thị thông tin khách hàng, trạng thái `eKYC: Đã xác thực` và danh bạ thụ hưởng; (2) Màn hình **Cài đặt Bảo mật (`SecuritySettingsFragment`)** hiển thị mục quản lý Smart PIN 6 số, thanh chỉnh hạn mức giao dịch và công tắc *Tạm khóa khẩn cấp tài khoản*.

<div align="center"><i>Hình 4-13. Giao diện Hồ sơ khách hàng (`ProfileFragment`) và Cài đặt bảo mật (`SecuritySettingsFragment`)</i></div>

14. **Bản đồ Chi nhánh & Trợ lý Ảo AI Chatbot (`fragment_branch_map.xml`, `fragment_chatbot.xml`):** Hiển thị bản đồ `osmdroid` đánh dấu 10 Chi nhánh/ATM kèm nhãn **Gần nhất**, vẽ lộ trình đi bộ thực tế từ vị trí hiện tại; bên cạnh màn hình Trợ lý AI tư vấn thu/chi cá nhân hóa kèm các nút hành động DeepLink chạm-để-mở ngay chức năng.

> **[VỊ TRÍ CHÈN ẢNH GIAO DIỆN APP — Hình 4-14]**  
> - **Màn hình cần chụp (ghép 2 ảnh cạnh nhau):** (1) Màn hình **Bản đồ Chi nhánh & ATM (`BranchMapFragment`)** hiển thị các ghim chi nhánh tại TP.HCM, thẻ chi nhánh có nhãn *Gần nhất* và đường vẽ lộ trình đi bộ màu xanh trên bản đồ `osmdroid`; (2) Màn hình **Trợ lý ảo AI (`ChatbotFragment`)** hiển thị đoạn hội thoại tư vấn số dư/chi tiêu tháng kèm các nút gợi ý điều hướng nhanh (`[Chuyển tiền]`, `[Thanh toán hóa đơn]`...).

<div align="center"><i>Hình 4-14. Giao diện Bản đồ chi nhánh (`BranchMapFragment`) và Trợ lý tài chính AI (`ChatbotFragment`)</i></div>

### 4.3.3 Minh họa giao diện phân hệ Nhân viên Ngân hàng (`OFFICER` — Hình 4-15 đến Hình 4-18)

15. **Bảng điều khiển Tổng quan Nhân viên Ngân hàng (`fragment_officer_dashboard.xml`):** Tổng hợp thời gian thực các chỉ số vận hành gồm tổng số khách hàng, số hồ sơ eKYC đang chờ duyệt, số ca hậu kiểm khuôn mặt, tổng giao dịch trong ngày và danh sách nhật ký kiểm toán `audit_logs` mới nhất.

> **[VỊ TRÍ CHÈN ẢNH GIAO DIỆN APP — Hình 4-15]**  
> - **Màn hình cần chụp (Đăng nhập tài khoản `OFFICER` — Trần Minh Quân):** Màn hình **Bảng điều khiển Nhân viên Ngân hàng (`OfficerDashboardFragment`)** hiển thị các thẻ thống kê KPI tổng quan và danh sách **Nhật ký kiểm toán (`audit_logs`)** ghi nhận các thao tác quản trị gần nhất.

<div align="center"><i>Hình 4-15. Giao diện Bảng điều khiển Nhân viên Ngân hàng (`OfficerDashboardFragment`)</i></div>

16. **Quản lý Danh sách Khách hàng, Tạo mới & Khóa khẩn cấp (`fragment_customer_management.xml`, `fragment_create_customer.xml`, `dialog_edit_customer.xml`):** Cho phép tìm kiếm khách hàng, mở hồ sơ khách hàng mới tại quầy (tự động cấp tài khoản `CHECKING` 10 chữ số), chụp hộ ảnh eKYC, chỉnh sửa thông tin liên hệ hoặc bấm **Khóa / Mở khóa tài khoản** có hiệu lực tức thì.

> **[VỊ TRÍ CHÈN ẢNH GIAO DIỆN APP — Hình 4-16]**  
> - **Màn hình cần chụp (ghép 2 ảnh cạnh nhau trên tài khoản `OFFICER`):** (1) Màn hình **Quản lý khách hàng (`CustomerManagementFragment`)** hiển thị danh sách khách hàng, trạng thái `ACTIVE` / `LOCKED` và các nút *Sửa / Khóa TK / Chụp eKYC*; (2) Màn hình **Tạo mới hồ sơ khách hàng tại quầy (`CreateCustomerFragment`)** hoặc hộp thoại chỉnh sửa khách hàng.

<div align="center"><i>Hình 4-16. Giao diện Quản lý và Tạo mới hồ sơ khách hàng dành cho Nhân viên Ngân hàng</i></div>

17. **Thẩm định Hồ sơ eKYC & Hậu kiểm Ảnh xác thực `FaceCheck` (`fragment_officer_ekyc_review.xml`):** Hiển thị ảnh chân dung giải mã trực tiếp trên bộ nhớ (`Base64ImageDecoder` — không lưu bộ nhớ đệm ngoài) để Nhân viên đối chiếu thông tin CCCD, bấm **Phê duyệt (`VERIFIED`)** hoặc **Từ chối (`REJECTED`)** và tra cứu lịch sử ảnh chụp giao dịch giá trị cao.

> **[VỊ TRÍ CHÈN ẢNH GIAO DIỆN APP — Hình 4-17]**  
> - **Màn hình cần chụp (Tài khoản `OFFICER`):** Màn hình **Duyệt định danh eKYC & Hậu kiểm (`OfficerEkycReviewFragment`)** hiển thị thẻ hồ sơ khách hàng đang chờ duyệt (`PENDING`) kèm ảnh chân dung eKYC, hai nút *Phê duyệt* / *Từ chối* và khu vực danh sách hậu kiểm ảnh chụp `FaceCheck`.

<div align="center"><i>Hình 4-17. Giao diện Duyệt hồ sơ eKYC và Hậu kiểm `FaceCheck` (`OfficerEkycReviewFragment`)</i></div>

18. **Quản lý Biểu Lãi suất Tiết kiệm Động (`fragment_interest_rate_management.xml`):** Cho phép Nhân viên ngân hàng cập nhật lãi suất năm (`%/năm`) cho từng kỳ hạn (`0`, `1`, `3`, `6`, `12`, `24`, `36` tháng), tự động đồng bộ tức thì sang màn hình dự phóng tiền lãi trên thiết bị của toàn bộ khách hàng đang mở ứng dụng.

> **[VỊ TRÍ CHÈN ẢNH GIAO DIỆN APP — Hình 4-18]**  
> - **Màn hình cần chụp (Tài khoản `OFFICER`):** Màn hình **Quản lý Biểu lãi suất tiết kiệm (`InterestRateManagementFragment`)** hiển thị bảng lãi suất hiện hành theo các kỳ hạn (`0`, `1`, `3`, `6`, `12`, `24`, `36` tháng) và biểu mẫu cập nhật mức lãi suất năm mới.

<div align="center"><i>Hình 4-18. Giao diện Cập nhật biểu lãi suất động (`InterestRateManagementFragment`)</i></div>

---

## 4.4 Quy trình và kết quả kiểm thử hệ thống (Unit Test & System Test)

### 4.4.1 Kết quả 156 bài kiểm thử tự động hai đầu (JUnit 4 & Node.js Test Runner)

Để đảm bảo mọi quy tắc tài chính, mật mã học và phân quyền không phát sinh lỗi hồi quy khi 3 thành viên hợp nhất mã nguồn, nhóm đã xây dựng tổng cộng **156 bài kiểm thử tự động** chạy độc lập với tốc độ cao (*Bảng 4-3*):

*Bảng 4-3. Tổng hợp kết quả thực thi 156 bài kiểm thử tự động trên Android và Backend Server*

| Phân hệ kiểm thử | Lệnh thực thi chuẩn | Số bộ Test | Số Test Cases | Tỷ lệ Đạt | Các nhóm kịch bản nghiệp vụ được kiểm chứng tự động |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **Android JVM Unit Tests** (`app/src/test/`) | `.\gradlew.bat testDebugUnitTest` | `32` lớp `*Test.java` | **`95 / 95`** | **100% PASS** | • Định dạng tiền tệ `CurrencyFormatterTest`, tạo/đọc ma trận QR `QrBitmapEncoderTest`, `QrHelperTest`.<br>• Toán học lãi tiết kiệm & tất toán trước/đúng hạn `SavingInfoTest`, `SavingCalculatorTest`, `SavingsUseCasesTest`, `ObserveSavingProjectionUseCaseTest`.<br>• Điều kiện Liveness `FaceLivenessEvaluatorTest`, chốt chặn vai trò/DeepLink `SessionManagerRoleGuardTest`, lọc sao kê `LedgerFilterHelperTest`, khoảng cách cầu Haversine `FindNearestBranchUseCaseTest` và toàn bộ các `UseCase` / `Mapper`. |
| **Backend Integration & Unit Tests** (`server/test/`) | `cd server ; npm test` | `10` tệp (`11` suites) | **`61 / 61`** | **100% PASS** | • Ẩn thông tin nhạy cảm tại `/accounts/lookup` (`accounts.test.ts`).<br>• Chu trình nháp 2FA, hạn mức ngày `UTC+7`, bảo mật 3 lớp `> 5M`, chặn tài khoản `LOCKED` (`transactions.test.ts`).<br>• Siết chặt `FaceCheck` (`>= 0.80`, chặn ảnh mẫu 1x1 `403`, `Fail-Closed 503` khi AI lỗi — `faceCheck.test.ts`).<br>• Nạp tiền Stripe Idempotent `/payments/finalize` (`payments.test.ts`).<br>• Mở/tất toán tiết kiệm & Smart PIN `HMAC-SHA256` chống TOCTOU, chặn `OFFICER` `403` (`savings.test.ts`, `security.test.ts`).<br>• Thông báo biến động 2 chiều, duyệt eKYC, `audit_logs` của Officer, Rate Limiter và AI Chatbot Whitelist/Fallback. |
| **Biên dịch Sản phẩm Đóng gói** | `npm run build` & `assembleDebug` | Toàn dự án | Biên dịch sạch | **0 Lỗi** | TypeScript kiểm tra cờ nghiêm ngặt (`strict`, `noUnusedLocals`, `noUnusedParameters`) và Gradle đóng gói APK Debug thành công 100%. |

### 4.4.2 Kịch bản và kết quả kiểm thử tích hợp hệ thống (System Test Cases)

Bên cạnh kiểm thử tự động, nhóm thực hiện kiểm thử hộp đen đầu-cuối (*End-to-End System Testing*) trực tiếp với máy chủ Render, Cloud Firestore, Brevo Email và Stripe Sandbox theo 16 kịch bản trọng tâm trong *Bảng 4-4*:

*Bảng 4-4. Kết quả kiểm thử 16 kịch bản nghiệp vụ và tấn công biên trên hệ thống thực tế*

| Mã TC | Hạng mục kiểm thử | Dữ liệu đầu vào & Thao tác thực hiện | Kết quả kỳ vọng | Kết quả thực tế | Đánh giá |
| :---: | :--- | :--- | :--- | :--- | :---: |
| **TC-01** | Phân tách vai trò & DeepLink Guard | Đăng nhập tài khoản `OFFICER` (`SEED_OFFICER_EMAIL`), thử kích hoạt URI `novapay://transfer` qua `adb` | `MainActivity` chặn điều hướng chéo vai trò và giữ nguyên tại `OfficerDashboardFragment` | Chặn chính xác 100%, không mở được màn hình giao dịch của khách hàng | **PASS** |
| **TC-02** | Tra cứu tên người nhận bảo mật PII | Khách hàng 1 nhập số TK `1001000002` trên màn hình Chuyển tiền | Hiển thị đúng tên **Nguyễn Thành Đạt**, gói tin JSON trả về tuyệt đối không chứa `balance` hay `nationalId` | Trả về đúng 3 khóa `{accountId, accountNumber, ownerName}` (`HTTP 200`) | **PASS** |
| **TC-03** | Chuyển tiền nội bộ `<= 5M` & Thông báo 2 chiều | Chuyển `500.000 ₫` từ `1001000001` sang `1001000002`, nhập đúng mã OTP 6 số nhận qua Gmail | Trừ/cộng đúng `500.000 ₫`, sinh 2 bút toán `ledger` (`OUT`/`IN`), cả 2 máy nổi System Push Notification tức thì | Số dư 2 bên khớp tuyệt đối, thông báo nổi trong ~1 giây kèm biên lai PDF/PNG | **PASS** |
| **TC-04** | Chặn tự chuyển tiền cho chính mình | Nhập số tài khoản nhận trùng với tài khoản nguồn `1001000001` | Hệ thống từ chối khởi tạo giao dịch với thông báo không được chuyển cùng tài khoản | Bị chặn ngay cả trên UI lẫn Server (`400 INVALID_ARGUMENT`) | **PASS** |
| **TC-05** | Tấn công bỏ bước `FaceCheck` (`> 5M`) | Gọi `/transactions/initiate` với `amount = 6_000_000`, sau đó bỏ qua `/face-check` gọi thẳng `/transactions/confirm` | Server từ chối hạch toán do bản nháp đang ở `PENDING_FACE` và chưa có mã OTP | Trả về mã lỗi `400/403 FACE_REQUIRED`, số dư không thay đổi | **PASS** |
| **TC-06** | `FaceCheck` từ chối khuôn mặt người lạ | Thanh toán hóa đơn `6.200.000 ₫`, đưa khuôn mặt người khác không phải chủ hồ sơ eKYC vào khung chụp | Gemini `2.5 Flash` trả `matched: false`, màn hình hiện cảnh báo đỏ và không gửi Email OTP | Trả về `403 FACE_MISMATCH` kèm lý do Tiếng Việt, tăng bộ đếm `faceAttempts` | **PASS** |
| **TC-07** | Giao dịch `> 5M` hợp lệ đủ 3 lớp bảo mật | Đưa đúng khuôn mặt chủ TK (`confidence >= 0.80`) → Quét vân tay/PIN máy → Nhập OTP Gmail vừa phát hành | Hạch toán thành công, gạch nợ hóa đơn `UNPAID → PAID` nguyên tử và lưu `face_checks/{draftId}` | Giao dịch thành công (`200 SUCCESS`), hóa đơn chuyển sang `PAID` ngay lập tức | **PASS** |
| **TC-08** | Khóa bản nháp khi nhập sai OTP quá 5 lần | Nhập sai liên tiếp 5 lần mã OTP 6 số trên `OtpVerificationDialogFragment` | Bản nháp bị chuyển sang `EXPIRED` và chặn mọi lần thử tiếp theo | Lần sai thứ 5 trả về `429 TOO_MANY_ATTEMPTS`, buộc tạo giao dịch mới | **PASS** |
| **TC-09** | Nạp tiền Stripe & Chống cộng tiền đúp | Thanh toán `200.000 ₫` bằng thẻ test `4242...` trên `PaymentSheet`, gửi lặp 3 request `/payments/finalize` cùng `draftId` | Tài khoản `CHECKING` chỉ được cộng đúng `200.000 ₫` duy nhất 1 lần | Cả 3 lần đều trả về cùng `txId` với `status: "CONFIRMED"`, số dư chỉ tăng 1 lần | **PASS** |
| **TC-10** | Mở và Tất toán sổ tiết kiệm trước hạn | Mở sổ tiết kiệm `5.000.000 ₫` kỳ hạn 6 tháng có nhập Smart PIN, sau đó bấm Tất toán ngay trước hạn | Trích đúng gốc khi mở; khi tất toán trước hạn áp dụng lãi không kỳ hạn `0.5%/năm` và đóng sổ `CLOSED` | Số dư `CHECKING`, bút toán `ledger` và thông báo `SAVING` khớp chính xác 100% | **PASS** |
| **TC-11** | Tạm khóa khẩn cấp `FROZEN` & Khóa sai PIN | Nhập sai PIN 5 lần (kiểm tra khóa tạm 15 phút); dùng PIN đúng bật khóa khẩn cấp `FROZEN` trên TK `CHECKING` | Khi `FROZEN`, mọi lệnh chuyển tiền đi từ TK này đều bị từ chối `ACCOUNT_NOT_ACTIVE` | Khóa PIN 15 phút (`429 PIN_LOCKED`) và chặn chi tiêu khi `FROZEN` hoạt động chuẩn xác | **PASS** |
| **TC-12** | Đồng bộ lãi suất động thời gian thực | Thiết bị 1 mở `SavingDetailFragment` (12T — `6.00%`); Thiết bị 2 (`OFFICER`) đổi lãi 12T lên `6.60%` | Lãi dự kiến tháng trên Thiết bị 1 tự nhảy từ `500.000 ₫` lên `550.000 ₫` không cần tải lại trang | Cập nhật tức thì trên màn hình Thiết bị 1 qua `Observable.combineLatest` | **PASS** |
| **TC-13** | Cưỡng bức đăng xuất tức thì khi bị `LOCKED` | Thiết bị 1 đang ở Trang chủ; Thiết bị 2 (`OFFICER`) bấm **Khóa TK** khách hàng đó | Thiết bị 1 tự động đăng xuất về `LoginFragment` trong `< 1s`, JWT cũ bị từ chối nếu gọi lại API | Tự động `signOut()` tức thì kèm thông báo tài khoản bị khóa; ghi đủ `audit_logs` | **PASS** |
| **TC-14** | Fallback bản đồ Quận 7 trên máy ảo Mỹ | Mở `BranchMapFragment` trên máy ảo mặc định tọa độ `37.4219, -122.0840` (California, cách TP.HCM `> 100 km`) | Tự động chuyển tâm về ĐH Tôn Đức Thắng (`10.7326, 106.6998`), tìm chi nhánh gần nhất & vẽ đường đi bộ | Định tâm chính xác về Quận 7 sau kiểm tra bán kính, vẽ mượt tuyến đi bộ màu xanh | **PASS** |
| **TC-15** | AI Chatbot tư vấn số dư & Whitelist DeepLink | Nhắn *"Tháng này tôi tiêu bao nhiêu, cho tôi đóng tiền điện"* tại `ChatbotFragment` | Trả lời khớp số dư & tổng chi thực tế của khách hàng, hiển thị nút `[Thanh toán hóa đơn]` mở `novapay://bills` | Phản hồi Tiếng Việt tự nhiên, chạm nút chuyển thẳng tới `UtilityBillFragment` | **PASS** |
| **TC-16** | Xuất Sao kê PDF & Kiểm tra rò rỉ khóa APK | Xuất sao kê trên `TransactionHistoryFragment`; kiểm tra tệp APK biên dịch bằng chuỗi khóa `sk_test_`, `AIza`, `xkeysib` | Tệp PDF mở rõ nét đầy đủ bảng bút toán; tệp APK không chứa bất kỳ khóa bí mật nào của Server | Xuất PDF thành công qua `FileProvider`; `0` chuỗi khóa bí mật xuất hiện trong APK | **PASS** |

---

# CHƯƠNG 5. KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN

## 5.1 Tổng kết kết quả nghiên cứu và phát triển sản phẩm

Đối chiếu trực tiếp với toàn bộ yêu cầu đề bài tại `docs/DE_BAI.md` và thang điểm đánh giá tại `docs/MARKING.md`, đồ án **Ứng dụng Ngân hàng số NovaPay** đã hoàn thành **100% các hạng mục bắt buộc** cùng nhiều tính năng nâng cao thiết thực:

### 5.1.1 Về mặt sản phẩm phần mềm ứng dụng
1. **Hoàn thiện trọn vẹn hệ sinh thái Ngân hàng số hai vai trò (`CUSTOMER` & `OFFICER`) gồm 28 màn hình:** Đáp ứng đầy đủ từ quản lý hồ sơ khách hàng, định danh sinh trắc học eKYC hai chiều (tự chụp hoặc nhân viên chụp hộ tại quầy), quản lý bộ 3 tài khoản (`CHECKING`, `SAVING`, `MORTGAGE`), chuyển khoản nội bộ/liên ngân hàng/quét QR, nạp tiền quốc tế qua cổng **Stripe `PaymentSheet`**, thanh toán hóa đơn Điện/Nước, nạp tiền điện thoại, đặt vé máy bay/phim/khách sạn/mua sắm cho đến bản đồ chỉ đường đi bộ tới chi nhánh gần nhất và trợ lý tài chính ảo **AI Chatbot**.
2. **Mở rộng các nghiệp vụ chuẩn iBanking thương mại:** Chủ động bổ sung thêm hệ thống **Thông báo biến động số dư 2 chiều thời gian thực kết hợp System Push Notification**, chức năng **Mở và Tất toán sổ tiết kiệm trực tuyến**, **Trung tâm bảo mật quản lý Smart PIN 6 số, hạn mức & khóa thẻ khẩn cấp**, **Đăng nhập nhanh sinh trắc học lưu khóa phần cứng Android Keystore** và **Xuất sao kê điện tử PDF / Biên lai PNG-PDF**.

### 5.1.2 Về mặt kỹ thuật kiến trúc và bảo mật hệ thống
1. **Kiến trúc sạch chuẩn mực và không xung đột nhóm:** Tuân thủ tuyệt đối **100% Java 17 + XML Layouts + ViewBinding + Material Design 3**, cô lập hoàn toàn tầng `domain` thuần Java (`128` lớp không phụ thuộc Android Framework), kết hợp mô hình **Day-0 Skeleton** với 3 sub-graphs điều hướng qua DeepLink `novapay://` giúp 3 thành viên làm việc song song hiệu quả.
2. **Bảo mật đa tầng không tin cậy thiết bị đầu cuối (*Zero-Trust Hybrid Cloud*):** Cô lập hoàn toàn khóa bí mật trên máy chủ Render HTTPS, khóa chặt quyền ghi dữ liệu tài chính bằng `firestore.rules`, bảo đảm tính toàn vẹn số học bằng kiểu số nguyên `long` bên trong giao dịch nguyên tử **2-Phase ACID Firestore Transaction** và kiểm chứng chất lượng qua **156/156 bài kiểm thử tự động đạt 100%**.

## 5.2 Những khuyết điểm và hạn chế hiện tại của hệ thống

Bên cạnh những kết quả đạt được trong khuôn khổ đồ án học thuật, nhóm nhìn nhận khách quan một số giới hạn kỹ thuật hiện tại khi đặt cạnh một hệ thống Core Banking thương mại quy mô lớn:
1. **Bộ giới hạn tần suất (*Rate Limiter*) lưu trên bộ nhớ RAM đơn tiến trình (`server/src/middleware/rateLimit.ts`):** Phù hợp và rất nhẹ khi triển khai trên một container Web Service duy nhất của Render, nhưng nếu mở rộng ngang (*Horizontal Auto-Scaling*) lên nhiều máy chủ song song thì bộ đếm cần được đồng bộ qua bộ nhớ đệm phân tán.
2. **Kiểm tra thực thể sống (*Liveness*) dựa trên tiêu chí hình học một khung hình (`FaceLivenessEvaluator.java`):** Cơ chế yêu cầu mở hai mắt `>= 0.7f` và nhìn thẳng `<= 15°` kết hợp đối chiếu Gemini Multimodal chặn tốt ảnh nhắm mắt, ảnh nghiêng hoặc người khác cầm máy, nhưng chưa chống được hoàn toàn kịch bản tấn công trình chiếu video độ phân giải cao (*Deepfake / Video Replay Attack*) như các giải pháp sinh trắc học 3D chuyên dụng.
3. **Bước chốt nạp tiền Stripe hiện do ứng dụng kích hoạt (`/payments/finalize`):** Dù đã đảm bảo tính Idempotent chống cộng tiền đúp, song nếu thiết bị người dùng sập nguồn đúng khoảnh khắc vừa thanh toán xong trên `PaymentSheet` trước khi kịp gọi `/payments/finalize`, giao dịch cần chờ phiên mở ứng dụng kế tiếp hoặc cơ chế đối soát bất đồng bộ từ máy chủ.
4. **Chuẩn mã QR chuyển khoản đang dùng lược đồ nội bộ `novapay://qr`:** Phục vụ nhanh và chính xác cho chuyển khoản trong hệ sinh thái NovaPay nhưng chưa tương thích liên thông trực tiếp với mã VietQR chuẩn EMVCo của các ứng dụng ngân hàng thương mại khác tại Việt Nam.

## 5.3 Định hướng nghiên cứu và phát triển trong tương lai

Để từng bước nâng cấp **NovaPay** từ mô hình mô phỏng học thuật lên tiệm cận sản phẩm công nghệ tài chính thực thụ, nhóm đề xuất 5 hướng phát triển tiếp theo:
1. **Chuẩn hóa mã QR theo đặc tả VietQR EMVCo Napas 247:** Nâng cấp bộ mã hóa `QrBitmapEncoder` và bộ giải mã QR để sinh/đọc chuỗi TLV kèm mã kiểm tra tuần hoàn `CRC-16/CCITT-FALSE`, cho phép quét chéo liên ngân hàng thực tế.
2. **Bổ sung thanh toán một chạm không tiếp xúc NFC (*Host Card Emulation — HCE*):** Khai thác dịch vụ `HostApduService` của Android để biến điện thoại thành thẻ thanh toán ảo chạm trực tiếp vào thiết bị POS tại quầy bên cạnh phương thức quét QR.
3. **Tích hợp bất đồng bộ Stripe Webhooks & FCM Background Push:** Xây dựng thêm endpoint `POST /webhooks/stripe` xác minh chữ ký số `Stripe-Signature` để tự động cộng tiền ngay cả khi ứng dụng đã đóng hẳn; đồng thời kết hợp **Firebase Cloud Messaging (FCM)** để đánh thức thông báo biến động số dư ngay cả khi hệ điều hành đóng tiến trình nền.
4. **Nâng cấp Active Challenge-Response Liveness & Mô hình nhúng khuôn mặt chuyên biệt:** Kết hợp chuỗi thử thách ngẫu nhiên theo thời gian thực (*chớp mắt → quay nhẹ sang trái → mỉm cười*) với mạng nơ-ron trích xuất vector đặc trưng khuôn mặt 512 chiều (*ArcFace / InsightFace*) nhằm đạt tốc độ phản hồi dưới `200 ms` và chống giả mạo video.
5. **Hoàn tất phát hành thử nghiệm trên Google Play Store:** Đóng gói bản dựng Android App Bundle (`.aab`) đã ký số, công bố chính sách quyền riêng tư (*Privacy Policy*), ghi rõ thông tin mô phỏng học thuật trên trang *Store Listing* và cung cấp tài khoản kiểm thử `CUSTOMER` / `OFFICER` tại mục *App Access* của Google Play Console.

---

# TÀI LIỆU THAM KHẢO

| STT | Tài liệu tham khảo |
| :---: | :--- |
| **[1]** | Khoa Công nghệ Thông tin — Trường Đại học Tôn Đức Thắng, *Tài liệu giảng dạy và Đề bài Đồ án Cuối kỳ môn Phát triển Ứng dụng Di động (Mobile Apps Development — Ibanking App)*, TP. Hồ Chí Minh, 2026. |
| **[2]** | Google Android Developers, *Guide to App Architecture (Clean Architecture, UI Layer, Domain Layer & Data Layer)*, truy cập tại: https://developer.android.com/topic/architecture |
| **[3]** | Google Android Developers, *Dependency Injection with Dagger Hilt on Android & Jetpack Navigation Component*, truy cập tại: https://developer.android.com/training/dependency-injection/hilt-android |
| **[4]** | ReactiveX Contributors, *RxJava 3: Reactive Extensions for the JVM Documentation*, truy cập tại: https://github.com/ReactiveX/RxJava |
| **[5]** | Google Firebase Documentation, *Cloud Firestore Data Model, Atomic Transactions & Security Rules Reference*, truy cập tại: https://firebase.google.com/docs/firestore |
| **[6]** | Google Firebase Documentation, *Firebase Admin Node.js SDK — Verify ID Tokens, Manage Users & Custom Claims*, truy cập tại: https://firebase.google.com/docs/auth/admin |
| **[7]** | Stripe Inc., *Stripe PaymentIntents API, Zero-Decimal Currencies & Android PaymentSheet Integration Guide*, truy cập tại: https://docs.stripe.com/payments/accept-a-payment?platform=android |
| **[8]** | Google ML Kit Documentation, *On-Device Face Detection & Barcode Scanning with CameraX on Android*, truy cập tại: https://developers.google.com/ml-kit/vision/face-detection/android |
| **[9]** | Google AI for Developers, *Gemini API Multimodal Vision & Structured JSON Output Reference (`gemini-2.5-flash`)*, truy cập tại: https://ai.google.dev/gemini-api/docs |
| **[10]** | Brevo Developers, *Transactional Emails REST API v3 Specification (`POST /v3/smtp/email`)*, truy cập tại: https://developers.brevo.com/reference/sendtransacemail |
| **[11]** | osmdroid Project & HeiGIT OpenRouteService, *OpenStreetMap Android MapView & OpenRouteService Directions API v2 (`foot-walking`)*, truy cập tại: https://openrouteservice.org/dev/#/api-docs |
| **[12]** | ISO/IEC 18004:2015, *Information technology — Automatic identification and data capture techniques — QR Code bar code symbology specification (Reed-Solomon Error Correction)*, International Organization for Standardization. |
