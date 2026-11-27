# TÀI LIỆU BÁO CÁO PHẦN 3: SƠ ĐỒ TUẦN TỰ NGHIỆP VỤ TRỌNG TÂM (2 PLANTUML SEQUENCE DIAGRAMS)

> **Dự án:** Hệ thống Ngân hàng Số **NovaPay** (`com.novabank.novapay`)  
> **Tiêu chí `MARKING.md`:** Cô đọng đúng **2 sơ đồ tuần tự cốt lõi nhất** đại diện cho hai tiêu chí khó và quan trọng nhất của đề bài (**3.2 Security — 1.0 điểm** và **3.4 Transaction Management — 1.0 điểm**), giữ bố cục báo cáo Word súc tích, không bị dàn trải.

---

## 1. Sơ Đồ Tuần Tự 1/2: Chuyển Tiền Giá Trị Cao (`> 5.000.000 ₫`) — Bảo Mật 3 Lớp, ACID Transaction & Thông Báo Biến Động 2 Chiều

### Điểm nhấn kiến trúc ăn điểm:
- **Trì hoãn phát hành OTP thông minh:** Khi số tiền chi/rút `> 5.000.000 ₫`, `POST /transactions/initiate` tạo bản nháp `"PENDING_FACE"` với `otpExpiresAt: null` (**chưa gửi Email OTP**), giúp khách hàng không bị trôi thời gian 180 giây khi cân chỉnh khuôn mặt.
- **Đối chiếu khuôn mặt nghiêm ngặt (*Zero-False-Acceptance* & Fail-Closed):** Server kiểm tra `ekyc_images/{uid}` (chặn ảnh trống hoặc ảnh mẫu 1x1 `SAMPLE_SEED_PLACEHOLDER_BASE64` bằng `403 EKYC_FACE_NOT_FOUND`). Android dùng CameraX + ML Kit kiểm tra Liveness (1 khuôn mặt, 2 mắt mở `>= 0.7f`, `|EulerY/Z| <= 15°`), nén JPEG `< 200KB` gửi lên Backend để **Gemini 2.5 Flash** (`temperature: 0.0`) thẩm định: chỉ đạt khi `matched == true && confidence >= 0.80`; sai người trả `403 FACE_MISMATCH` kèm cảnh báo đỏ (tối đa 5 lần thử); nếu AI quá tải (`429`) hoặc timeout (`> 15s`), áp dụng **Fail-Closed (`503 SERVICE_UNAVAILABLE`)**.
- **Giao dịch nguyên tử 2 pha & Thông báo biến động thời gian thực:** Gom toàn bộ `tx.get(...)` vào Pha 1, sau đó Pha 2 ghi đồng thời tài liệu kế toán (`accounts`, `ledger OUT/IN`, `transactions`, `face_checks`) và gọi `enqueueTransactionNotifications()` sinh thông báo biến động 2 chiều; Cloud Firestore `SnapshotListener` lập tức kích hoạt `BalanceAlertSubscriber` & `SystemNotificationHelper` đẩy thông báo nổi `IMPORTANCE_HIGH` ra cả hai thiết bị.

> **Mã nguồn sơ đồ PlantUML:** [`docs/diagrams/07_seq_high_value_3layer_transfer.puml`](../diagrams/07_seq_high_value_3layer_transfer.puml) *(xuất SVG/PNG 300 DPI bằng PlantUML / Kroki)*.

---

## 2. Sơ Đồ Tuần Tự 2/2: Nạp Tiền Quốc Tế Qua Stripe `PaymentSheet` & Chốt Chặn Idempotency

### Điểm nhấn kiến trúc ăn điểm:
- **Không cộng tiền sớm:** Tại `POST /transactions/confirm`, dù đã xác thực OTP 2FA, Server **chưa cộng số dư**, mà chỉ khởi tạo `Stripe PaymentIntent` với tiền tệ không thập phân `currency: "vnd"` (truyền trực tiếp số nguyên VNĐ không nhân 100, kèm `idempotencyKey: novapay_draft_{draftId}`), chuyển bản nháp sang `"PENDING_PAYMENT"` và trả `clientSecret` mở `PaymentSheet`.
- **Chống cộng tiền đúp (Idempotent Finalization):** Tại `POST /payments/finalize`, Server truy vấn ngược lên máy chủ Stripe (`paymentIntents.retrieve`) xác minh `status === "succeeded"` và khớp đúng số tiền VNĐ trước khi mở Firestore Transaction kiểm tra `draft.status !== "CONFIRMED"`, đồng thời ghi nguyên tử số dư mới, sổ cái `IN` và thông báo biến động `BALANCE IN`. Mọi lệnh gọi thử lại khi mạng chập chờn đều trả về cùng một `txId` mà không bao giờ cộng tiền hai lần.

> **Mã nguồn sơ đồ PlantUML:** [`docs/diagrams/08_seq_stripe_gateway_idempotent.puml`](../diagrams/08_seq_stripe_gateway_idempotent.puml) *(xuất SVG/PNG 300 DPI bằng PlantUML / Kroki)*.

---

## 3. Tóm Tắt Các Luồng Bổ Trợ Khác *(Trình bày bằng văn bản — không vẽ thêm sơ đồ để giữ báo cáo gọn)*
- **Đăng nhập, Custom Claims & Khóa Realtime (`SessionManager`):** Đọc Custom Claim `role` qua `getIdToken(true)`; khi Officer khóa tài khoản (`PATCH /officer/customers/:uid` gọi `disabled: true` + `revokeRefreshTokens`), `SnapshotListener` trên `users/{uid}` cưỡng bức `signOut()` trong dưới 1 giây.
- **Trợ lý ảo AI Chatbot (`POST /ai/chat`):** Tổng hợp số dư & thu/chi tháng từ đầu tháng `Asia/Ho_Chi_Minh`, gọi Gemini `2.5 Flash`, lọc tối đa 3 nút DeepLink qua Whitelist `ALLOWED_CHAT_DEEPLINKS` chống Prompt Injection và tự chuyển sang Rule-based Vietnamese NLP Fallback khi AI quá tải/timeout.
- **Mở & Tất toán Sổ tiết kiệm trực tuyến (`/savings/open`, `/savings/:id/settle`):** Xác thực Smart PIN 6 số `HMAC-SHA256` chống TOCTOU ngay trong Pha 1 của `runTransaction`, tính lãi tất toán trước hạn (`0.5%/năm` theo số ngày thực gửi) hoặc đúng hạn bằng số nguyên `long`.
