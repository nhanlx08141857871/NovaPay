# TÀI LIỆU BÁO CÁO PHẦN 2: SƠ ĐỒ LỚP TINH GỌN (3 PLANTUML CLASS DIAGRAMS)

> **Dự án:** Hệ thống Ngân hàng Số **NovaPay** (`com.novabank.novapay`)  
> **Học phần:** Phát triển Ứng dụng Di động — Khoa Công nghệ Thông tin, Trường Đại học Tôn Đức Thắng (TDTU)  
> **Tiêu chí `MARKING.md`:** *Mục 2.1 — Class Diagrams (1.0/10 điểm): Vẽ đúng, đầy đủ các lớp cốt lõi và mối quan hệ giữa các lớp, trình bày gọn gàng không làm loãng báo cáo.*  
> **Chuẩn ký hiệu UML 2.5 sử dụng thống nhất trên cả 3 sơ đồ lớp:**
> - `--|>` **Kế thừa (Generalization):** `*Fragment --|> BaseFragment<VB>`.
> - `..|>` **Thực thi giao diện (Realization):** `*RepositoryImpl ..|> *Repository`.
> - `*--` **Hợp thành chặt (Composition):** `User *-- EkycInfo`, `Account *-- SavingInfo`, `Branch *-- GeoLocation`, `*ViewModel *-- UiState`.
> - `o--` **Kết tập (Aggregation):** `CustomerHomeFragment o-- AccountCardAdapter`.
> - `-->` **Phụ thuộc / Tiêm phụ thuộc (Hilt DI & Association):** `Fragment --> ViewModel --> UseCase --> Repository <-- RepositoryImpl --> Api / DataSource`.

---

## 1. Sơ Đồ Lớp 1/3: Toàn Bộ Thực Thể Domain Model & Quy Tắc Nghiệp Vụ (Pure Java 17)

Trình bày toàn bộ các thực thể lõi của hệ thống thuộc 4 nhóm (*User/Security/Notifications*, *Accounts/Savings/Mortgage*, *Transactions/Ledger/Utilities*, *Branch GIS/AI Chat/Officer Audit*) cùng kiểu dữ liệu bắt buộc (`long` cho mọi giá trị VNĐ, `java.time.Instant` cho thời gian, `GeoLocation` cho tọa độ), các phương thức nghiệp vụ cốt lõi (`isActiveChecking()`, `calculateMonthlyProfit()`, `SavingCalculator`, `distanceMetersTo()`) và đầy đủ quan hệ + bản số (`1`, `0..1`, `0..*`).

> **Mã nguồn sơ đồ PlantUML:** [`docs/diagrams/03_class_domain_model.puml`](../diagrams/03_class_domain_model.puml) *(xuất SVG/PNG 300 DPI bằng PlantUML / Kroki)*.

---

## 2. Sơ Đồ Lớp 2/3: Kiến Trúc 4 Tầng Android Clean Architecture + MVVM (`core` / `domain` / `data` / `presentation`)

Thay vì chia vụn lặp lại mẫu MVVM qua nhiều trang, sơ đồ này cô đọng rõ nét kiến trúc 4 tầng của 350 lớp Java 17 (28 màn hình, 17 Repository Interfaces đối xứng 17 `*RepositoryImpl`, 50+ UseCases): thể hiện đầy đủ hạ tầng dùng chung (`BaseFragment<VB>`, `UiState<T>`, `SingleLiveEvent<T>`, `AppResult<T>`, `SessionManager`, `BiometricHelper`, `BiometricQuickLoginStore`, `BalanceAlertSubscriber`, `SystemNotificationHelper`, `StatementPdfExporter`, `QrBitmapEncoder`), nguyên tắc phụ thuộc `presentation → domain ← data`, và tách biệt `@BackendRetrofit` (`AuthInterceptor`) với `@OpenRouteRetrofit`.

> **Mã nguồn sơ đồ PlantUML:** [`docs/diagrams/04_class_android_clean_mvvm.puml`](../diagrams/04_class_android_clean_mvvm.puml) *(xuất SVG/PNG 300 DPI bằng PlantUML / Kroki)*.

---

## 3. Sơ Đồ Lớp 3/3: Kiến Trúc Máy Chủ Backend Server (`server/src` — Node.js 20 + TypeScript)

Hợp nhất toàn bộ kiến trúc phía máy chủ trên một sơ đồ duy nhất: `AppConfig`, 2 lớp Middleware bảo mật (`AuthMiddleware`, `RateLimiter`), **8 Express REST Routers** (`Accounts`, `Transactions`, `Payments`, `Savings`, `Security`, `Ekyc`, `Officer`, `Ai`) và **8 Domain Services** (`OtpService` SHA-256, `PinService` HMAC-SHA256 chống TOCTOU, `EmailService` Brevo, `FaceCheckService` Gemini `>= 0.80`, `StripeService`, `NotificationsService`, `OfficerAuditService`, `ChatbotService`).

> **Mã nguồn sơ đồ PlantUML:** [`docs/diagrams/05_class_backend_server.puml`](../diagrams/05_class_backend_server.puml) *(xuất SVG/PNG 300 DPI bằng PlantUML / Kroki)*.
