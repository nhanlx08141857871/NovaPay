# Sơ đồ kiến trúc hệ thống NovaPay

Các sơ đồ dùng cú pháp Mermaid (xem được trên GitHub, VS Code với tiện ích Mermaid, hoặc dán vào https://mermaid.live để xuất ảnh PNG/SVG đưa vào báo cáo).

## 1. Kiến trúc tổng thể (Hybrid Cloud)

```mermaid
flowchart LR
    subgraph Client["Android Client (Java 17, MVVM + Clean Architecture)"]
        UI["presentation<br/>Fragment + ViewModel + UiState"]
        DOM["domain<br/>Model + UseCase + Repository interface"]
        DATA["data<br/>RepositoryImpl + Mapper + DTO"]
        CORE["core<br/>Hilt DI, SessionManager, BiometricHelper"]
        UI --> DOM
        DATA --> DOM
        UI -. Hilt .-> CORE
    end

    subgraph Server["Backend Node.js 20 + TypeScript + Express"]
        MW["Auth middleware<br/>verifyIdToken + role claim"]
        RT["Routes<br/>ekyc / accounts / transactions<br/>payments / officer / ai"]
        SV["Services<br/>otp, email, faceCheck,<br/>stripe, chatbot, officerAudit"]
        MW --> RT --> SV
    end

    subgraph Firebase["Firebase (Spark)"]
        AUTH["Authentication<br/>Custom Claims role"]
        FS[("Cloud Firestore<br/>14 collections + rules")]
    end

    subgraph External["Dịch vụ ngoài"]
        STRIPE["Stripe PaymentIntent"]
        BREVO["Brevo Email OTP"]
        GEMINI["Gemini Flash<br/>FaceCheck + Chatbot"]
        ORS["OpenRouteService<br/>Chỉ đường đi bộ"]
    end

    DATA -- "Retrofit + Bearer ID Token" --> MW
    DATA -- "Firebase SDK (đọc realtime)" --> FS
    DATA --> AUTH
    DATA -- "Retrofit (không token)" --> ORS
    SV -- "Admin SDK (ghi giao dịch ACID)" --> FS
    SV --> AUTH
    SV --> STRIPE
    SV --> BREVO
    SV --> GEMINI
```

Nguyên tắc phân quyền dữ liệu: client chỉ **đọc** Firestore (theo `firestore.rules`); mọi thao tác **ghi** dữ liệu tài chính và kiểm toán đi qua Backend bằng Admin SDK.

## 2. Phân tầng Clean Architecture

```mermaid
flowchart TB
    P["presentation<br/>(auth, customer, ekyc, officer)"]
    D["domain (Pure Java)<br/>model, repository, usecase"]
    DA["data<br/>dto, mapper, remote, repository"]
    C["core<br/>common, di, security, ui, util"]
    P --> D
    DA --> D
    P --> C
    DA --> C
```

Quy tắc phụ thuộc: `presentation → domain ← data`. Tầng `domain` không import `android.*`, `androidx.*`, `firebase`, `retrofit2`, `okhttp3`, `gson`.

## 3. Luồng giao dịch an toàn (chi/rút trên 5.000.000 ₫)

```mermaid
sequenceDiagram
    autonumber
    actor KH as Khách hàng
    participant APP as Android App
    participant SRV as Backend
    participant FS as Firestore
    participant AI as Gemini
    participant MAIL as Brevo

    KH->>APP: Nhập giao dịch
    APP->>SRV: POST /transactions/initiate
    SRV->>FS: Kiểm tra user, eKYC, số dư, hạn mức
    SRV->>FS: Tạo draft PENDING_FACE
    SRV-->>APP: draftId, requiresFace, requiresBiometric
    APP->>APP: FaceCheck (CameraX + ML Kit liveness)
    APP->>SRV: POST /transactions/face-check
    SRV->>AI: So khớp ảnh eKYC và ảnh vừa chụp
    AI-->>SRV: samePerson, confidence
    SRV->>MAIL: Gửi OTP 6 số (hiệu lực 180 giây)
    SRV->>FS: draft PENDING_OTP
    SRV-->>APP: otpExpiresAt
    APP->>APP: BiometricPrompt (vân tay hoặc PIN thiết bị)
    KH->>APP: Nhập OTP
    APP->>SRV: POST /transactions/confirm
    SRV->>FS: runTransaction: trừ tiền, ledger, transactions, cập nhật bill/order/mortgage
    SRV-->>APP: txId, SUCCESS
```

## 4. Luồng nạp tiền Stripe (GATEWAY)

```mermaid
sequenceDiagram
    autonumber
    participant APP as Android App
    participant SRV as Backend
    participant ST as Stripe
    participant FS as Firestore

    APP->>SRV: POST /transactions/initiate (GATEWAY)
    SRV-->>APP: draftId (PENDING_OTP, miễn FaceCheck)
    APP->>SRV: POST /transactions/confirm (OTP)
    SRV->>ST: Tạo PaymentIntent
    SRV->>FS: draft PENDING_PAYMENT
    SRV-->>APP: clientSecret
    APP->>ST: PaymentSheet thanh toán
    APP->>SRV: POST /payments/finalize
    SRV->>ST: Truy vấn PaymentIntent (succeeded, đúng số tiền, VND)
    SRV->>FS: runTransaction idempotent: cộng tiền, ledger IN, draft CONFIRMED
    SRV-->>APP: txId
```

## 5. Chuyển trạng thái của transaction_drafts

```mermaid
stateDiagram-v2
    [*] --> PENDING_FACE: initiate (trên 5 triệu)
    [*] --> PENDING_OTP: initiate (từ 5 triệu trở xuống, GATEWAY)
    PENDING_FACE --> PENDING_OTP: face-check khớp
    PENDING_FACE --> EXPIRED: sai khuôn mặt 5 lần
    PENDING_OTP --> PENDING_OTP: gửi lại OTP (tối đa 5 lần, cách nhau 30 giây)
    PENDING_OTP --> CONFIRMED: OTP đúng, giao dịch thường
    PENDING_OTP --> PENDING_PAYMENT: OTP đúng, GATEWAY
    PENDING_OTP --> EXPIRED: sai OTP 5 lần
    PENDING_PAYMENT --> CONFIRMED: finalize thành công
    CONFIRMED --> [*]
    EXPIRED --> [*]
```
