# Sơ đồ Use Case

```mermaid
flowchart LR
    KH(["Khách hàng"])
    NV(["Nhân viên ngân hàng (Officer)"])

    subgraph CUS["Phân hệ Khách hàng"]
        UC1["Đăng nhập"]
        UC2["Định danh eKYC"]
        UC3["Xem tài khoản Checking, Saving, Mortgage"]
        UC4["Xem lãi suất và tiền lãi hàng tháng"]
        UC5["Xem kỳ trả nợ vay"]
        UC6["Xem lịch sử giao dịch và thu chi tháng"]
        UC7["Chuyển tiền nội bộ, liên ngân hàng, quét QR"]
        UC8["Nạp tiền Stripe, rút tiền"]
        UC9["Thanh toán điện, nước"]
        UC10["Nạp tiền điện thoại"]
        UC11["Đặt vé, khách sạn, mua sắm"]
        UC12["Xác thực FaceCheck, Biometric, OTP"]
        UC13["Bản đồ chi nhánh và đường đi bộ"]
        UC14["Chat với trợ lý AI"]
        UC15["Xuất và chia sẻ biên lai PNG, PDF"]
        UC16["Cập nhật hồ sơ cá nhân"]
    end

    subgraph OFF["Phân hệ Nhân viên"]
        UO1["Xem Dashboard"]
        UO2["Tạo khách hàng mới"]
        UO3["Sửa, khóa, mở khóa khách hàng"]
        UO4["Chụp hộ và duyệt eKYC"]
        UO5["Cập nhật lãi suất tiết kiệm"]
        UO6["Xem Audit Logs"]
        UO7["Hậu kiểm FaceCheck thủ công"]
    end

    KH --> UC1 & UC2 & UC3 & UC6 & UC7 & UC8 & UC9 & UC10 & UC11 & UC13 & UC14 & UC15 & UC16
    UC3 -. include .-> UC4
    UC3 -. include .-> UC5
    UC7 -. include .-> UC12
    UC8 -. include .-> UC12
    UC9 -. include .-> UC12
    UC10 -. include .-> UC12
    UC11 -. include .-> UC12

    NV --> UC1
    NV --> UO1 & UO2 & UO3 & UO4 & UO5 & UO6 & UO7
```

## Đặc tả tóm tắt các use case chính

| Use case | Tác nhân | Tiền điều kiện | Kết quả |
| :--- | :--- | :--- | :--- |
| Chuyển tiền | Khách hàng | Tài khoản ACTIVE, eKYC VERIFIED, nguồn là CHECKING | Trừ tiền nguồn, cộng tiền đích (nội bộ), ghi ledger và transactions |
| Chi/rút trên 5 triệu | Khách hàng | Như trên | Bắt buộc đủ 3 lớp: FaceCheck, Biometric, OTP |
| Nạp tiền Stripe | Khách hàng | Tài khoản CHECKING ACTIVE | Cộng tiền đúng một lần sau khi Stripe báo succeeded |
| Thanh toán khoản vay | Khách hàng | Có tài khoản MORTGAGE ACTIVE | Giảm dư nợ, dời ngày đến hạn (1 tháng hoặc 14 ngày) |
| Đổi lãi suất | Officer | Custom Claim role = OFFICER | Ghi interest_rates và audit_logs, màn hình tiết kiệm cập nhật realtime |
| Khóa khách hàng | Officer | Role OFFICER | users.status = LOCKED, Firebase Auth bị vô hiệu hóa, client bị đăng xuất tức thì |
