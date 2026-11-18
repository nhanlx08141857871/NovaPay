# Sơ đồ thực thể quan hệ (ER) và thiết kế cơ sở dữ liệu

Cơ sở dữ liệu là Cloud Firestore (NoSQL) gồm 14 collection (2 subcollection nằm trong `users` và `accounts`). Quan hệ giữa các collection được biểu diễn bằng trường tham chiếu (khóa ngoài logic).

## 1. Sơ đồ ER

```mermaid
erDiagram
    users {
        string uid PK
        string role
        string fullName
        string email
        string phone
        string nationalId
        string dob
        string address
        string avatarUrl
        string status
        map ekyc
        string createdBy
        timestamp createdAt
        timestamp updatedAt
    }
    beneficiaries {
        string id PK
        string name
        string accountNumber
        string bankCode FK
        string alias
    }
    ekyc_images {
        string uid PK
        string imageBase64
        timestamp capturedAt
        string capturedBy
    }
    accounts {
        string accountId PK
        string ownerUid FK
        string type
        string accountNumber UK
        string status
        long balance
        string currency
        long perTxnLimit
        long dailyLimit
        map saving
        map mortgage
        timestamp createdAt
    }
    ledger {
        string entryId PK
        string txId FK
        string direction
        long amount
        long balanceAfter
        string description
        timestamp createdAt
    }
    transactions {
        string txId PK
        string type
        string fromAccountId FK
        string toAccountId FK
        map toExternal
        long amount
        long fee
        string note
        string status
        boolean requiresBiometric
        boolean biometricPassed
        boolean otpVerified
        boolean faceChecked
        map meta
        string initiatedBy FK
        timestamp createdAt
    }
    transaction_drafts {
        string draftId PK
        string uid FK
        map payload
        string status
        string otpHash
        long otpExpiresAt
        long otpSentAt
        int resendCount
        int attempts
        int faceAttempts
        boolean faceChecked
        string stripeIntentId
        string txId FK
        timestamp createdAt
        timestamp expireAt
    }
    face_checks {
        string draftId PK
        string uid FK
        string txId FK
        string imageBase64
        boolean manualAuditRequired
        timestamp createdAt
    }
    interest_rates {
        string rateId PK
        int termMonths
        double annualRate
        timestamp effectiveFrom
        string updatedBy FK
        timestamp updatedAt
    }
    banks {
        string bankCode PK
        string name
        string logoUrl
        string napasCode
    }
    branches {
        string branchId PK
        string name
        string address
        geopoint location
        string phone
        string openHours
        string type
    }
    utility_bills {
        string billId PK
        string type
        string providerName
        string customerCode
        string customerName
        string period
        long amount
        timestamp dueDate
        string status
    }
    service_catalog {
        string itemId PK
        string category
        string title
        long price
        string details
        string imageUrl
    }
    orders {
        string orderId PK
        string uid FK
        string itemId FK
        string category
        int quantity
        long totalAmount
        string status
        string txId FK
        timestamp createdAt
    }
    messages {
        string msgId PK
        string role
        string text
        array actions
        timestamp createdAt
    }
    audit_logs {
        string logId PK
        string actorUid FK
        string action
        string targetId
        map details
        timestamp createdAt
    }

    users ||--o{ accounts : "sở hữu (ownerUid)"
    users ||--o{ beneficiaries : "danh bạ thụ hưởng"
    users ||--o| ekyc_images : "ảnh eKYC"
    users ||--o{ transaction_drafts : "khởi tạo"
    users ||--o{ orders : "đặt"
    users ||--o{ messages : "chat_sessions/{uid}"
    users ||--o{ audit_logs : "Officer thực hiện (actorUid)"
    accounts ||--o{ ledger : "bút toán"
    accounts ||--o{ transactions : "nguồn (fromAccountId)"
    accounts |o--o{ transactions : "đích (toAccountId)"
    transactions ||--o{ ledger : "sinh bút toán (txId)"
    transaction_drafts |o--o| transactions : "xác nhận thành (txId)"
    transaction_drafts ||--o| face_checks : "cùng draftId"
    banks ||--o{ beneficiaries : "bankCode"
    interest_rates ||--o{ accounts : "áp dụng theo termMonths"
    service_catalog ||--o{ orders : "itemId"
    orders |o--|| transactions : "txId"
    utility_bills |o--o| transactions : "meta.billId"
```

## 2. Quy ước kiểu dữ liệu

| Kiểu nghiệp vụ | Lưu trữ | Ghi chú |
| :--- | :--- | :--- |
| Tiền tệ | `Long` (VNĐ) | Không dùng số thực, lãi tháng = floor(balance x lãi suất / 1200) |
| Thời gian | `Timestamp` | Chuyển sang `Instant` ở tầng data, tính hạn mức ngày theo Asia/Ho_Chi_Minh |
| Vị trí | `GeoPoint` | Chuyển sang `GeoLocation` ở tầng data |

## 3. Chỉ mục tổng hợp (`firestore.indexes.json`)

| Collection | Trường | Mục đích |
| :--- | :--- | :--- |
| ledger | direction, createdAt giảm dần | Lọc thu/chi theo thời gian |
| transactions | fromAccountId, status, createdAt | Tính tổng chi trong ngày cho hạn mức ngày |
| utility_bills | customerCode, status | Tra cứu hóa đơn chưa thanh toán |
| orders | uid, createdAt giảm dần | Lịch sử đơn hàng của khách |
| face_checks | manualAuditRequired, createdAt giảm dần | Officer hậu kiểm FaceCheck |

## 4. Phân quyền (`firestore.rules`)

| Collection | Đọc | Ghi |
| :--- | :--- | :--- |
| users | Chủ tài khoản, Officer | Chủ chỉ sửa phone, address, avatarUrl khi ACTIVE |
| users/beneficiaries | Chủ tài khoản | Chủ tài khoản |
| accounts, ledger | Chủ tài khoản, Officer | Chỉ Backend |
| transactions | Người gửi, người nhận nội bộ, Officer | Chỉ Backend |
| transaction_drafts | Không ai (chỉ Backend) | Chỉ Backend |
| ekyc_images | Chủ tài khoản, Officer | Chỉ Backend |
| face_checks, audit_logs | Officer | Chỉ Backend |
| banks, branches, interest_rates, service_catalog, utility_bills | Người dùng đã đăng nhập | Chỉ Backend |
| orders | Chủ đơn, Officer | Chỉ Backend |
| chat_sessions/{uid}/messages | Chủ phiên chat | Chỉ Backend |
