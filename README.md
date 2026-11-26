> Xem tổng quan sản phẩm & hướng dẫn trải nghiệm nhanh tại **[GIOI_THIEU.md](./GIOI_THIEU.md)**

# NovaPay — Ứng Dụng Ngân Hàng Số (Mobile Banking)

> **Tuyên bố Học thuật (Academic Sandbox Simulation — TDTU):**
> Dự án **NovaPay** được xây dựng phục vụ môn học **Phát triển Ứng dụng Di động (Mobile Apps Development)** tại **Khoa Công nghệ Thông tin — Trường Đại học Tôn Đức Thắng (TDTU)**. Mọi số dư, tài khoản và giao dịch trong ứng dụng đều là dữ liệu mô phỏng học thuật, không phát sinh lưu ký tiền tệ thực tế.

---

## 1. Giới Thiệu & Công Nghệ Cốt Lõi

**NovaPay (`com.novabank.novapay`)** là hệ thống ngân hàng số hoàn chỉnh hỗ trợ 2 phân hệ người dùng riêng biệt (**Khách hàng — `CUSTOMER`** và **Nhân viên Ngân hàng — `OFFICER`**) trên nền tảng kiến trúc **MVVM + Clean Architecture** kết hợp **Hybrid Cloud Backend**.

### Công nghệ sử dụng
- **Android Client (`app/`):**
  - Ngôn ngữ: **100% Java 17** (`minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`).
  - Giao diện: **XML Layouts + ViewBinding + Material Design 3** (hỗ trợ Sáng/Tối, 100% Tiếng Việt).
  - Kiến trúc & DI: **Clean Architecture** (`presentation` → `domain` Pure Java ← `data`), **Dagger Hilt**, **Jetpack Navigation Component** (Single-Activity + DeepLinks `novapay://`), **RxJava 3 + RxAndroid**.
  - Bảo mật & Tiện ích: **CameraX + Google ML Kit** (Face Detection Liveness cho eKYC/FaceCheck & Barcode Scanning quét QR), **AndroidX Biometric** (`BiometricPrompt`), **Stripe Android SDK** (`PaymentSheet` Test Mode), **osmdroid + OpenRouteService** (bản đồ & chỉ đường đi bộ), **MPAndroidChart** (biểu đồ thu/chi).
- **Backend Server (`server/`):**
  - **Node.js 20+ + TypeScript + Express + Firebase Admin SDK** (xử lý giao dịch nguyên tử Firestore ACID Transaction, phát hành & băm OTP SHA-256 gửi qua **Brevo HTTPS API**, tạo/xác nhận **Stripe PaymentIntent**, đối chiếu khuôn mặt **Gemini Multimodal** & **AI Chatbot**).
- **Cơ sở dữ liệu & Xác thực:**
  - **Firebase Authentication** (kèm Custom Claims `role`) & **Cloud Firestore** (14 collections + 2 subcollections được bảo vệ bởi `firestore.rules`).

---

## 2. Yêu Cầu Môi Trường (Prerequisites)

Trước khi bắt đầu, hãy đảm bảo máy tính đã cài đặt:
1. **Android Studio** (phiên bản mới nhất, đi kèm sẵn **JetBrains Runtime JDK** và **Android SDK Platform 35**).
2. **Node.js `>= 20.0.0`** và **npm** (kiểm tra bằng `node -v` và `npm -v`).
3. **Git** (khuyến nghị bật hỗ trợ đường dẫn dài trên Windows: `git config --global core.longpaths true`).
4. Các khóa API miễn phí (chỉ cần tạo 1 lần cho cả nhóm):
   - **Firebase Project** (gói Spark miễn phí — bật *Authentication -> Email/Password* và *Cloud Firestore*).
   - **Brevo API Key** (gửi Email OTP xác thực giao dịch qua HTTPS, nhớ xác minh địa chỉ *Sender Email* trong Brevo).
   - **Stripe Test Keys** (`pk_test_...` và `sk_test_...` tại Stripe Dashboard ở chế độ *Test Mode*).
   - **Google Gemini API Key** (lấy miễn phí tại Google AI Studio).
   - **OpenRouteService API Key** (lấy miễn phí tại openrouteservice.org để vẽ lộ trình đi bộ tới chi nhánh).

---

## 3. Hướng Dẫn Cài Đặt Từng Bước (Step-by-Step Setup)

### Bước 1: Tải mã nguồn dự án
```bash
git clone <URL_REPO_CUA_BAN>
cd NovaPay
git config --global core.longpaths true
```

---

### Bước 2: Thiết lập Firebase (`google-services.json` & Service Account)
1. Truy cập [Firebase Console](https://console.firebase.google.com/), tạo hoặc chọn project Firebase của nhóm.
2. Vào **Build → Authentication → Sign-in method** → Bật phương thức **Email/Password**.
3. Vào **Build → Firestore Database** → Bấm **Create database** (chọn vùng gần nhất như `asia-southeast1`).
4. **Cấu hình cho Android App:**
   - Vào **Project settings (biểu tượng bánh răng) → General → Your apps → Add Android app**.
   - Nhập chính xác Android package name: **`com.novabank.novapay`**.
   - Tải file **`google-services.json`** về và đặt vào đúng đường dẫn:
     ```text
     NovaPay/app/google-services.json
     ```
5. **Lấy khóa cho Backend Server:**
   - Vào **Project settings → Service accounts → Firebase Admin SDK (Node.js)** → Bấm **Generate new private key** để tải file `.json` về máy (tuyệt đối không đưa file này vào Git).

---

### Bước 3: Cấu hình biến môi trường Backend (`server/.env`)
1. Tạo file `server/.env` bằng cách nhân bản từ file mẫu `server/.env.example`:
   ```powershell
   # Trên Windows PowerShell (tại thư mục gốc NovaPay):
   Copy-Item server\.env.example server\.env
   ```
2. Mở file `server/.env` và điền đầy đủ các thông số:
   ```env
   PORT=3000

   # 1. Mở file Service Account .json vừa tải ở Bước 2, copy 3 trường tương ứng:
   FIREBASE_PROJECT_ID=ten-project-firebase-cua-ban
   FIREBASE_CLIENT_EMAIL=firebase-adminsdk-xxxxx@ten-project.iam.gserviceaccount.com
   FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n"

   # 2. Brevo Email OTP (lưu ý BREVO_SENDER_EMAIL phải là email đã Verified trên Brevo)
   BREVO_API_KEY=xkeysib-...
   BREVO_SENDER_EMAIL=email-da-xac-minh-tren-brevo@gmail.com
   BREVO_SENDER_NAME=NovaPay Banking

   # 3. Stripe Test Mode Keys
   STRIPE_SECRET_KEY=sk_test_...
   STRIPE_PUBLISHABLE_KEY=pk_test_...

   # 4. Google Gemini API (phục vụ đối chiếu khuôn mặt FaceCheck > 5M & AI Chatbot)
   GEMINI_API_KEY=AIzaSy...
   GEMINI_MODEL=gemini-2.5-flash
   ```
   *(Lưu ý: Giữ nguyên dấu ngoặc kép `"..."` và các ký tự `\n` trong `FIREBASE_PRIVATE_KEY`.)*

---

### Bước 4: Cấu hình khóa phía Android (`local.properties`)
Mở file `local.properties` ở thư mục gốc dự án (Android Studio tự tạo sẵn dòng `sdk.dir=...`) và thêm các dòng sau vào cuối file:

```properties
# Khóa chỉ đường đi bộ OpenRouteService
ORS_API_KEY=eyJvcmciOi...

# Khóa công khai Stripe Test Mode (chỉ dùng pk_test_..., tuyệt đối KHÔNG điền sk_test_)
STRIPE_PUBLISHABLE_KEY=pk_test_...

# Địa chỉ Backend Server:
# - Nếu chạy trên Máy ảo Android (Emulator): dùng http://10.0.2.2:3000/
# - Nếu chạy trên Điện thoại thật cắm cáp USB: xem mẹo adb reverse bên dưới và dùng http://127.0.0.1:3000/
BACKEND_BASE_URL=http://10.0.2.2:3000/
```

> **Mẹo chạy trên Điện thoại thật qua cáp USB:**
> Cắm cáp điện thoại đã bật *USB Debugging*, chạy lệnh `adb reverse tcp:3000 tcp:3000` trên máy tính và đặt `BACKEND_BASE_URL=http://127.0.0.1:3000/` trong `local.properties`. Điện thoại thật sẽ gọi thẳng vào cổng `3000` của máy tính mà không lo bị tường lửa Wi-Fi chặn!

---

### Bước 5: Cài đặt thư viện Backend, Deploy `firestore.rules` & Nạp dữ liệu mẫu (`seed`)
Mở Terminal tại thư mục `server/` và chạy tuần tự các lệnh sau:

```powershell
cd server

# 1. Cài đặt dependencies
npm install

# 2. Kiểm tra biên dịch TypeScript
npm run build

# 2b. Chạy bộ kiểm thử tự động của Backend
npm test

# 3. Đẩy bộ quy tắc phân quyền firestore.rules trực tiếp lên Cloud Firestore
npm run deploy:rules

# 4. Khởi tạo dữ liệu mẫu (Tài khoản Customer/Officer, Custom Claims, Số dư, Ngân hàng, Chi nhánh, Hóa đơn, Dịch vụ)
npm run seed
```
*(Lệnh `npm run seed` có tính chất **Idempotent** — bạn có thể chạy lại bất kỳ lúc nào để khôi phục dữ liệu mẫu mà không bị trùng bản ghi.)*

> **Dọn dẹp `transaction_drafts` hết hạn:**
> Mỗi draft giao dịch có trường `expireAt` (giữ 24 giờ). Bạn có thể bật chính sách TTL của Firestore cho trường này (`gcloud firestore fields ttls update expireAt --collection-group=transaction_drafts --enable-ttl`) hoặc chạy thủ công `npm run cleanup:drafts` trong thư mục `server/` để xóa các draft quá hạn (kể cả draft cũ chưa có `expireAt`).

> **Lưu ý về Composite Indexes trên Firestore:**
> Nếu muốn đồng bộ luôn cấu hình đánh chỉ mục từ `firestore.indexes.json` (hoặc khi Firestore báo link tạo index lần đầu trong log), bạn có thể cài Firebase CLI (`npm i -g firebase-tools`), đăng nhập `firebase login` và chạy `firebase deploy --only firestore:indexes` tại thư mục gốc.

---

## 4. Hướng Dẫn Chạy Hệ Thống (Run Backend & Android App)

### 4.1 Khởi động Backend Server (Terminal 1)
Tại thư mục `server/`, chạy chế độ phát triển tự động reload:
```powershell
cd server
npm run dev
```
Hoặc chạy bản biên dịch production:
```powershell
npm run build
npm start
```
Kiểm tra nhanh Server đã sẵn sàng bằng trình duyệt hoặc PowerShell:
```powershell
Invoke-RestMethod http://localhost:3000/health
# Kết quả mong đợi: ok : True ({"ok":true})
```

### 4.2 Biên dịch & Chạy Ứng dụng Android (Android Studio hoặc Terminal 2)
- **Cách 1 (Khuyên dùng — Android Studio):**
  1. Mở thư mục gốc `NovaPay` bằng **Android Studio**.
  2. Bấm **Sync Project with Gradle Files** (biểu tượng con voi).
  3. Chọn Máy ảo (API 26–35) hoặc Điện thoại thật → Bấm nút **Run 'app' (`Shift + F10`)**.
- **Cách 2 (Bằng dòng lệnh Terminal trên Windows):**
  ```powershell
  # Trỏ JAVA_HOME về JDK đi kèm Android Studio để đảm bảo có đầy đủ công cụ jlink
  $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

  # Biên dịch file APK Debug (kết quả tại app/build/outputs/apk/debug/app-debug.apk)
  .\gradlew.bat assembleDebug

  # Chạy toàn bộ bộ kiểm thử tự động (Unit Tests)
  .\gradlew.bat testDebugUnitTest
  ```

---

## 5. Tài Khoản Kiểm Thử & Dữ Liệu Demo Có Sẵn

Sau khi chạy `npm run seed`, bạn có thể đăng nhập ngay bằng các tài khoản sau (chi tiết đầy đủ tại `docs/TEST_ACCOUNTS.md`):

| Vai trò (`role`) | Email đăng nhập | Mật khẩu | eKYC | Tài khoản & Số dư khởi tạo | Kịch bản kiểm thử chính |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **CUSTOMER** *(Chính)* | `customer.verified@novapay.vn` | `NovaPay@User2026` | `VERIFIED` | • Checking `1001000001`: **25.000.000 ₫**<br>• Saving `2001000001`: **100.000.000 ₫**<br>• Mortgage `3001000001`: dư nợ **350.000.000 ₫** | Chuyển tiền, Quét QR, Nạp Stripe, OTP, FaceCheck > 5M, Hóa đơn, Nạp ĐT, Đặt vé/Dịch vụ, Bản đồ, AI Chatbot |
| **OFFICER** *(Quản trị)* | `officer@novapay.vn` | `NovaPay@Officer2026` | `VERIFIED` | *(Nhân viên ngân hàng)* | Dashboard, Tạo/Sửa/Khóa KH, Chụp & Duyệt eKYC, Đổi lãi suất tiết kiệm, Xem Audit Logs |
| **CUSTOMER** *(Chờ duyệt)* | `customer.pending@novapay.vn` | `NovaPay@User2026` | `PENDING` | • Checking `1001000002`: **10.000.000 ₫** | Dùng làm **số TK nhận chuyển tiền nội bộ (`1001000002`)** & kiểm thử Officer duyệt eKYC |
| **CUSTOMER** *(Chưa eKYC)* | `customer.unverified@novapay.vn` | `NovaPay@User2026` | `UNVERIFIED` | • Checking `1001000003`: **2.000.000 ₫** | Kiểm thử chặn giao dịch khi chưa eKYC (`EKYC_REQUIRED`) & khách tự chụp eKYC |

### Các mã dữ liệu mẫu để test nhanh trên ứng dụng:
- **Số tài khoản nhận nội bộ NovaPay:** `1001000002` (tự động tra cứu ra tên **Lê Thị Thu Hà**).
- **Mã hóa đơn Điện (`ELECTRIC`):**
  - `PE0100001234` (`850.000 ₫` — kiểm thử xác thực OTP chuẩn `<= 5.000.000 ₫`).
  - `PE0100009999` (`6.200.000 ₫` — kích hoạt bảo mật 3 lớp `> 5.000.000 ₫`: **FaceCheck eKYC + Biometric/PIN + OTP**).
- **Mã hóa đơn Nước (`WATER`):** `NU0700005678` (`240.000 ₫`).
- **Thẻ quốc tế kiểm thử cổng Stripe `PaymentSheet`:**
  - Số thẻ: `4242 4242 4242 4242` | Ngày hết hạn: tháng/năm bất kỳ trong tương lai (VD: `12/30`) | CVC: `123` | Mã ZIP: `70000`.

---

## 6. Cấu Trúc Thư Mục & Tài Liệu Tham Chiếu

```text
NovaPay/
├── app/                        # Ứng dụng Android (100% Java 17, XML ViewBinding, Material 3, Hilt)
│   └── src/main/
│       ├── java/com/novabank/novapay/
│       │   ├── core/           # AppResult, UiState, SingleLiveEvent, Hilt Modules, Biometric, SessionManager, Utils
│       │   ├── domain/         # Pure Java 17 Models, Repository Interfaces & UseCases
│       │   ├── data/           # Retrofit APIs, Firestore DataSources, DTOs, Mappers, RepositoryImpls
│       │   └── presentation/   # Các màn hình MVVM chia theo Auth, eKYC, Customer & Bank Officer
│       └── res/                # Layouts, Navigation Sub-graphs (nav_core, nav_money, nav_officer_map_ai), Values
├── server/                     # Backend Node.js + TypeScript + Express + Firebase Admin SDK
│   ├── src/                    # Config, Auth Middleware, Routes (ekyc, accounts, transactions, payments, officer, ai), Services
│   └── scripts/                # seed.ts (khởi tạo dữ liệu mẫu) & deployRules.ts (deploy firestore.rules)
├── docs/
│   ├── DE_BAI.md               # Đề bài gốc môn học Mobile Apps Development
│   ├── MARKING.md              # Bảng tiêu chí chấm điểm (Marking Scheme 10/10)
│   ├── PHAN_TICH.md            # Kế hoạch phân tích & phân chia công việc
│   ├── SPEC_CHOT.md            # Đặc tả kiến trúc & các quy tắc xử lý ngoại lệ đã chốt
│   └── TEST_ACCOUNTS.md        # Danh sách tài khoản kiểm thử & App Access
├── GEMINI.md                   # Quy chuẩn kỹ thuật bắt buộc (Single Source of Truth)
├── firestore.rules             # Quy tắc bảo mật phân quyền Cloud Firestore
└── firestore.indexes.json      # Cấu hình Composite Indexes cho Firestore
```

---

## 7. Xử Lý Sự Cố Thường Gặp (Troubleshooting)

1. **Lỗi `jlink.exe does not exist` khi chạy `./gradlew` bằng Terminal ngoài:**
   - Nguyên nhân: Máy tính đang trỏ `JAVA_HOME` vào bản JRE rút gọn thay vì JDK đầy đủ.
   - Cách khắc phục: Chạy `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"` trước khi gọi `.\gradlew.bat`.
2. **Không nhận được Email OTP khi thử giao dịch:**
   - Lưu ý các email mẫu `@novapay.vn` (`customer.verified@novapay.vn`) là hộp thư giả lập dùng cho tài khoản seed. Để nhận mã OTP thật vào hộp thư cá nhân của bạn, hãy đăng nhập bằng tài khoản **Officer** (`officer@novapay.vn`), vào mục **Tạo khách hàng mới** và tạo một khách hàng với **địa chỉ Gmail thật của bạn** (đồng thời đảm bảo `BREVO_SENDER_EMAIL` trong `server/.env` đã được xác minh *Verified Sender* trên Brevo).
3. **Bản đồ trên máy ảo báo đang dùng vị trí mặc định ĐH Tôn Đức Thắng:**
   - Đây là cơ chế phòng vệ chủ động của NovaPay: máy ảo Android mặc định đặt GPS tại California (Mỹ), cách cụm chi nhánh TP.HCM `> 100 km`, nên ứng dụng tự động ghim về **ĐH Tôn Đức Thắng (`10.7326, 106.6998` — Quận 7)** để thuật toán tìm chi nhánh gần nhất và vẽ đường đi bộ OpenRouteService luôn hoạt động chính xác.
