import dotenv from "dotenv";
import admin from "firebase-admin";
import { GeoPoint, Timestamp } from "firebase-admin/firestore";

dotenv.config();

/**
 * Ảnh JPEG 1x1 pixel chuẩn JFIF (SOI 0xFF 0xD8 ... EOI 0xFF 0xD9) dùng làm mẫu ekyc_images khi seed.
 */
const SAMPLE_EKYC_JPEG_BASE64 = Buffer.from([
  0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10, 0x4a, 0x46, 0x49, 0x46, 0x00, 0x01, 0x01, 0x01, 0x00, 0x48,
  0x00, 0x48, 0x00, 0x00, 0xff, 0xdb, 0x00, 0x43, 0x00, 0x03, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03,
  0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x03, 0x04, 0x06, 0x04, 0x04, 0x04, 0x04, 0x04, 0x08, 0x06,
  0x06, 0x05, 0x06, 0x09, 0x08, 0x0a, 0x0a, 0x09, 0x08, 0x09, 0x09, 0x0a, 0x0c, 0x0f, 0x0c, 0x0a,
  0x0b, 0x0e, 0x0b, 0x09, 0x09, 0x0d, 0x11, 0x0d, 0x0e, 0x0f, 0x10, 0x10, 0x11, 0x10, 0x0a, 0x0c,
  0x12, 0x13, 0x12, 0x10, 0x13, 0x0f, 0x10, 0x10, 0x10, 0xff, 0xc0, 0x00, 0x0b, 0x08, 0x00, 0x01,
  0x00, 0x01, 0x01, 0x01, 0x11, 0x00, 0xff, 0xc4, 0x00, 0x14, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00,
  0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x09, 0xff, 0xc4, 0x00, 0x14,
  0x10, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
  0x00, 0x00, 0xff, 0xda, 0x00, 0x08, 0x01, 0x01, 0x00, 0x00, 0x3f, 0x00, 0xd2, 0xcf, 0x20, 0xff,
  0xd9,
]).toString("base64");

function initFirebase(): void {
  if (admin.apps.length > 0) {
    return;
  }

  const projectId = (process.env.FIREBASE_PROJECT_ID || "").trim();
  const clientEmail = (process.env.FIREBASE_CLIENT_EMAIL || "").trim();
  const rawPrivateKey = (process.env.FIREBASE_PRIVATE_KEY || "").trim();
  const privateKey = rawPrivateKey
    .replace(/^["']|["']$/g, "")
    .replace(/\\n/g, "\n")
    .trim();

  if (projectId && clientEmail && privateKey) {
    admin.initializeApp({
      credential: admin.credential.cert({
        projectId,
        clientEmail,
        privateKey,
      }),
    });
    return;
  }

  const raw = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (raw && raw.trim().length > 0) {
    admin.initializeApp({
      credential: admin.credential.cert(JSON.parse(raw) as admin.ServiceAccount),
    });
    return;
  }
  if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    admin.initializeApp({
      credential: admin.credential.applicationDefault(),
    });
    return;
  }
  throw new Error(
    "Thiếu cấu hình Firebase Admin (FIREBASE_PROJECT_ID + FIREBASE_CLIENT_EMAIL + FIREBASE_PRIVATE_KEY) trong server/.env để chạy seed."
  );
}

interface SeedUserSpec {
  defaultUid: string;
  email: string;
  password: string;
  displayName: string;
  role: "CUSTOMER" | "OFFICER";
}

async function upsertAuthUser(spec: SeedUserSpec): Promise<string> {
  const auth = admin.auth();
  let uid: string;
  try {
    const existing = await auth.getUserByEmail(spec.email);
    await auth.updateUser(existing.uid, {
      password: spec.password,
      displayName: spec.displayName,
      emailVerified: true,
      disabled: false,
    });
    uid = existing.uid;
  } catch (err: unknown) {
    const code = (err as { code?: string })?.code;
    if (code === "auth/user-not-found") {
      const created = await auth.createUser({
        uid: spec.defaultUid,
        email: spec.email,
        password: spec.password,
        displayName: spec.displayName,
        emailVerified: true,
      });
      uid = created.uid;
    } else {
      throw err;
    }
  }
  await auth.setCustomUserClaims(uid, { role: spec.role });
  return uid;
}

async function runSeed(): Promise<void> {
  initFirebase();
  const db = admin.firestore();
  const now = new Date();
  const nowTs = Timestamp.fromDate(now);

  // 1. Seed 4 Auth users + Custom Claims
  const officerUid = await upsertAuthUser({
    defaultUid: "uid_officer_novapay_01",
    email: "officer@novapay.vn",
    password: "NovaPay@Officer2026",
    displayName: "Trần Minh Quân",
    role: "OFFICER",
  });

  const custVerifiedUid = await upsertAuthUser({
    defaultUid: "uid_customer_verified_01",
    email: "customer.verified@novapay.vn",
    password: "NovaPay@User2026",
    displayName: "Nguyễn Hoàng Anh",
    role: "CUSTOMER",
  });

  const custPendingUid = await upsertAuthUser({
    defaultUid: "uid_customer_pending_02",
    email: "customer.pending@novapay.vn",
    password: "NovaPay@User2026",
    displayName: "Lê Thị Thu Hà",
    role: "CUSTOMER",
  });

  const custUnverifiedUid = await upsertAuthUser({
    defaultUid: "uid_customer_unverified_03",
    email: "customer.unverified@novapay.vn",
    password: "NovaPay@User2026",
    displayName: "Phạm Quốc Bảo",
    role: "CUSTOMER",
  });

  const batch = db.batch();

  // 2. Seed users/{uid}
  batch.set(
    db.collection("users").doc(officerUid),
    {
      role: "OFFICER",
      fullName: "Trần Minh Quân",
      email: "officer@novapay.vn",
      phone: "0901000001",
      nationalId: "079090000001",
      dob: "1990-05-12",
      address: "19 Nguyễn Hữu Thọ, Tân Phong, Quận 7, TP.HCM",
      avatarUrl: "",
      status: "ACTIVE",
      ekyc: {
        status: "VERIFIED",
        verifiedBy: "SYSTEM",
        verifiedAt: nowTs,
      },
      createdBy: "SYSTEM",
      createdAt: nowTs,
      updatedAt: nowTs,
    },
    { merge: true }
  );

  batch.set(
    db.collection("users").doc(custVerifiedUid),
    {
      role: "CUSTOMER",
      fullName: "Nguyễn Hoàng Anh",
      email: "customer.verified@novapay.vn",
      phone: "0909123456",
      nationalId: "079201001234",
      dob: "2001-09-20",
      address: "25 Nguyễn Thị Thập, Tân Hưng, Quận 7, TP.HCM",
      avatarUrl: "",
      status: "ACTIVE",
      ekyc: {
        status: "VERIFIED",
        verifiedBy: officerUid,
        verifiedAt: nowTs,
      },
      createdBy: officerUid,
      createdAt: nowTs,
      updatedAt: nowTs,
    },
    { merge: true }
  );

  batch.set(
    db.collection("users").doc(custPendingUid),
    {
      role: "CUSTOMER",
      fullName: "Lê Thị Thu Hà",
      email: "customer.pending@novapay.vn",
      phone: "0912345678",
      nationalId: "079302005678",
      dob: "2002-03-15",
      address: "120 Lê Văn Lương, Tân Kiểng, Quận 7, TP.HCM",
      avatarUrl: "",
      status: "ACTIVE",
      ekyc: {
        status: "PENDING",
        verifiedBy: null,
        verifiedAt: null,
      },
      createdBy: officerUid,
      createdAt: nowTs,
      updatedAt: nowTs,
    },
    { merge: true }
  );

  batch.set(
    db.collection("users").doc(custUnverifiedUid),
    {
      role: "CUSTOMER",
      fullName: "Phạm Quốc Bảo",
      email: "customer.unverified@novapay.vn",
      phone: "0987654321",
      nationalId: "079203009876",
      dob: "2003-11-08",
      address: "45 Huỳnh Tấn Phát, Phú Thuận, Quận 7, TP.HCM",
      avatarUrl: "",
      status: "ACTIVE",
      ekyc: {
        status: "UNVERIFIED",
        verifiedBy: null,
        verifiedAt: null,
      },
      createdBy: officerUid,
      createdAt: nowTs,
      updatedAt: nowTs,
    },
    { merge: true }
  );

  // Saved beneficiaries for Customer 1
  batch.set(
    db.collection("users").doc(custVerifiedUid).collection("beneficiaries").doc("ben_internal_01"),
    {
      name: "Lê Thị Thu Hà",
      accountNumber: "1001000002",
      bankCode: "NOVAPAY",
      alias: "Thu Hà",
    },
    { merge: true }
  );
  batch.set(
    db.collection("users").doc(custVerifiedUid).collection("beneficiaries").doc("ben_vcb_01"),
    {
      name: "Trần Văn Bình",
      accountNumber: "0071000889911",
      bankCode: "VCB",
      alias: "Anh Bình VCB",
    },
    { merge: true }
  );

  // 3. Seed ekyc_images/{uid} (valid JPEG base64 < 200KB)
  batch.set(
    db.collection("ekyc_images").doc(custVerifiedUid),
    {
      imageBase64: SAMPLE_EKYC_JPEG_BASE64,
      capturedAt: nowTs,
      capturedBy: custVerifiedUid,
    },
    { merge: true }
  );
  batch.set(
    db.collection("ekyc_images").doc(custPendingUid),
    {
      imageBase64: SAMPLE_EKYC_JPEG_BASE64,
      capturedAt: nowTs,
      capturedBy: custPendingUid,
    },
    { merge: true }
  );

  // 4. Seed interest_rates/{rateId}
  const rateSpecs = [
    { id: "rate_0m", termMonths: 0, annualRate: 0.5 },
    { id: "rate_1m", termMonths: 1, annualRate: 3.5 },
    { id: "rate_3m", termMonths: 3, annualRate: 4.2 },
    { id: "rate_6m", termMonths: 6, annualRate: 5.2 },
    { id: "rate_12m", termMonths: 12, annualRate: 6.0 },
    { id: "rate_24m", termMonths: 24, annualRate: 6.5 },
  ];
  for (const rate of rateSpecs) {
    batch.set(
      db.collection("interest_rates").doc(rate.id),
      {
        termMonths: rate.termMonths,
        annualRate: rate.annualRate,
        effectiveFrom: nowTs,
        updatedBy: officerUid,
        updatedAt: nowTs,
      },
      { merge: true }
    );
  }

  // 5. Seed accounts/{accountId}
  const openedAt = Timestamp.fromDate(new Date(now.getTime() - 90 * 24 * 60 * 60 * 1000));
  const maturityDate = Timestamp.fromDate(new Date(now.getTime() + 275 * 24 * 60 * 60 * 1000));
  const nextDueDate = Timestamp.fromDate(new Date(now.getTime() + 15 * 24 * 60 * 60 * 1000));

  batch.set(
    db.collection("accounts").doc("acc_cust1_checking"),
    {
      ownerUid: custVerifiedUid,
      type: "CHECKING",
      accountNumber: "1001000001",
      status: "ACTIVE",
      balance: 25_000_000,
      currency: "VND",
      perTxnLimit: 50_000_000,
      dailyLimit: 200_000_000,
      saving: null,
      mortgage: null,
      createdAt: openedAt,
    },
    { merge: true }
  );

  batch.set(
    db.collection("accounts").doc("acc_cust1_saving"),
    {
      ownerUid: custVerifiedUid,
      type: "SAVING",
      accountNumber: "2001000001",
      status: "ACTIVE",
      balance: 100_000_000,
      currency: "VND",
      perTxnLimit: 0,
      dailyLimit: 0,
      saving: {
        termMonths: 12,
        principal: 100_000_000,
        interestRateId: "rate_12m",
        openedAt,
        maturityDate,
      },
      mortgage: null,
      createdAt: openedAt,
    },
    { merge: true }
  );

  batch.set(
    db.collection("accounts").doc("acc_cust1_mortgage"),
    {
      ownerUid: custVerifiedUid,
      type: "MORTGAGE",
      accountNumber: "3001000001",
      status: "ACTIVE",
      balance: 350_000_000,
      currency: "VND",
      perTxnLimit: 0,
      dailyLimit: 0,
      saving: null,
      mortgage: {
        loanAmount: 500_000_000,
        outstandingBalance: 350_000_000,
        termMonths: 120,
        annualRate: 8.5,
        paymentFrequency: "MONTHLY",
        installmentAmount: 6_200_000,
        nextDueDate,
      },
      createdAt: openedAt,
    },
    { merge: true }
  );

  batch.set(
    db.collection("accounts").doc("acc_cust2_checking"),
    {
      ownerUid: custPendingUid,
      type: "CHECKING",
      accountNumber: "1001000002",
      status: "ACTIVE",
      balance: 10_000_000,
      currency: "VND",
      perTxnLimit: 20_000_000,
      dailyLimit: 100_000_000,
      saving: null,
      mortgage: null,
      createdAt: openedAt,
    },
    { merge: true }
  );

  batch.set(
    db.collection("accounts").doc("acc_cust3_checking"),
    {
      ownerUid: custUnverifiedUid,
      type: "CHECKING",
      accountNumber: "1001000003",
      status: "ACTIVE",
      balance: 2_000_000,
      currency: "VND",
      perTxnLimit: 20_000_000,
      dailyLimit: 50_000_000,
      saving: null,
      mortgage: null,
      createdAt: openedAt,
    },
    { merge: true }
  );

  // 6. Seed current-month ledger & transactions (always inside current month!)
  const startOfMonth = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), 1, 2, 0, 0));
  const dayOffsetMs = (days: number) =>
    Timestamp.fromDate(new Date(Math.min(now.getTime() - 60_000, startOfMonth.getTime() + days * 3_600_000)));

  const ledgerEntries = [
    {
      id: "led_seed_01",
      txId: "tx_seed_01",
      direction: "IN",
      amount: 15_000_000,
      balanceAfter: 29_500_000,
      description: "Nhận lương tháng hiện tại",
      createdAt: dayOffsetMs(4),
      type: "DEPOSIT",
    },
    {
      id: "led_seed_02",
      txId: "tx_seed_02",
      direction: "OUT",
      amount: 1_500_000,
      balanceAfter: 28_000_000,
      description: "Thanh toán tiền điện EVN HCMC",
      createdAt: dayOffsetMs(12),
      type: "BILL",
    },
    {
      id: "led_seed_03",
      txId: "tx_seed_03",
      direction: "OUT",
      amount: 2_000_000,
      balanceAfter: 26_000_000,
      description: "Chuyển khoản nội bộ đến Lê Thị Thu Hà",
      createdAt: dayOffsetMs(20),
      type: "TRANSFER_INTERNAL",
    },
    {
      id: "led_seed_04",
      txId: "tx_seed_04",
      direction: "OUT",
      amount: 1_000_000,
      balanceAfter: 25_000_000,
      description: "Đặt vé xem phim & tiện ích NovaPay",
      createdAt: dayOffsetMs(28),
      type: "SERVICE_PAYMENT",
    },
  ];

  for (const entry of ledgerEntries) {
    batch.set(
      db.collection("accounts").doc("acc_cust1_checking").collection("ledger").doc(entry.id),
      {
        txId: entry.txId,
        direction: entry.direction,
        amount: entry.amount,
        balanceAfter: entry.balanceAfter,
        description: entry.description,
        createdAt: entry.createdAt,
      },
      { merge: true }
    );

    batch.set(
      db.collection("transactions").doc(entry.txId),
      {
        type: entry.type,
        fromAccountId: "acc_cust1_checking",
        toAccountId: entry.type === "TRANSFER_INTERNAL" ? "acc_cust2_checking" : null,
        amount: entry.amount,
        fee: 0,
        note: entry.description,
        status: "SUCCESS",
        requiresBiometric: false,
        biometricPassed: false,
        otpVerified: true,
        faceChecked: false,
        meta: {},
        initiatedBy: custVerifiedUid,
        createdAt: entry.createdAt,
      },
      { merge: true }
    );
  }

  // 7. Seed banks/{bankCode}
  const banks = [
    { code: "VCB", name: "Vietcombank", napasCode: "970436" },
    { code: "BIDV", name: "BIDV", napasCode: "970418" },
    { code: "TCB", name: "Techcombank", napasCode: "970407" },
    { code: "MB", name: "MB Bank", napasCode: "970422" },
    { code: "ACB", name: "ACB", napasCode: "970416" },
    { code: "VPB", name: "VPBank", napasCode: "970432" },
  ];
  for (const bank of banks) {
    batch.set(
      db.collection("banks").doc(bank.code),
      {
        name: bank.name,
        logoUrl: "",
        napasCode: bank.napasCode,
      },
      { merge: true }
    );
  }

  // 8. Seed 10 HCMC branches & ATMs around TDTU (10.7326, 106.6998)
  const branches = [
    {
      id: "br_tdtu_q7",
      name: "NovaBank PGD ĐH Tôn Đức Thắng",
      address: "19 Nguyễn Hữu Thọ, Tân Phong, Quận 7, TP.HCM",
      lat: 10.7326,
      lng: 106.6998,
      phone: "02837755001",
      openHours: "08:00 - 17:00",
      type: "BRANCH",
    },
    {
      id: "atm_lotte_q7",
      name: "NovaBank ATM Lotte Mart Quận 7",
      address: "469 Nguyễn Hữu Thọ, Tân Hưng, Quận 7, TP.HCM",
      lat: 10.7408,
      lng: 106.7019,
      phone: "19001234",
      openHours: "24/7",
      type: "ATM",
    },
    {
      id: "br_pmh_q7",
      name: "NovaBank Chi nhánh Phú Mỹ Hưng",
      address: "101 Tôn Dật Tiên, Tân Phú, Quận 7, TP.HCM",
      lat: 10.7285,
      lng: 106.7185,
      phone: "02837755002",
      openHours: "08:00 - 17:00",
      type: "BRANCH",
    },
    {
      id: "atm_crescent_q7",
      name: "NovaBank ATM Crescent Mall",
      address: "101 Tôn Dật Tiên, Phú Mỹ Hưng, Quận 7, TP.HCM",
      lat: 10.7294,
      lng: 106.7181,
      phone: "19001234",
      openHours: "24/7",
      type: "ATM",
    },
    {
      id: "br_ntt_q7",
      name: "NovaBank PGD Nguyễn Thị Thập",
      address: "280 Nguyễn Thị Thập, Tân Quy, Quận 7, TP.HCM",
      lat: 10.7392,
      lng: 106.7102,
      phone: "02837755003",
      openHours: "08:00 - 17:00",
      type: "BRANCH",
    },
    {
      id: "br_q4_khanhhoi",
      name: "NovaBank PGD Khánh Hội Quận 4",
      address: "180 Khánh Hội, Phường 6, Quận 4, TP.HCM",
      lat: 10.7586,
      lng: 106.7005,
      phone: "02837755004",
      openHours: "08:00 - 17:00",
      type: "BRANCH",
    },
    {
      id: "br_q1_benthanh",
      name: "NovaBank Hội sở Trung tâm Quận 1",
      address: "72 Lê Thánh Tôn, Bến Nghé, Quận 1, TP.HCM",
      lat: 10.7777,
      lng: 106.7019,
      phone: "02837755005",
      openHours: "08:00 - 17:30",
      type: "BRANCH",
    },
    {
      id: "atm_nguyenhue_q1",
      name: "NovaBank ATM Phố đi bộ Nguyễn Huệ",
      address: "68 Nguyễn Huệ, Bến Nghé, Quận 1, TP.HCM",
      lat: 10.7745,
      lng: 106.7037,
      phone: "19001234",
      openHours: "24/7",
      type: "ATM",
    },
    {
      id: "br_q3_cmt8",
      name: "NovaBank PGD Cách Mạng Tháng 8",
      address: "225 Cách Mạng Tháng 8, Phường 4, Quận 3, TP.HCM",
      lat: 10.7785,
      lng: 106.6838,
      phone: "02837755006",
      openHours: "08:00 - 17:00",
      type: "BRANCH",
    },
    {
      id: "br_binhthanh_lm81",
      name: "NovaBank PGD Landmark Bình Thạnh",
      address: "720A Điện Biên Phủ, Phường 22, Bình Thạnh, TP.HCM",
      lat: 10.7950,
      lng: 106.7218,
      phone: "02837755007",
      openHours: "08:00 - 17:00",
      type: "BRANCH",
    },
  ];
  for (const br of branches) {
    batch.set(
      db.collection("branches").doc(br.id),
      {
        name: br.name,
        address: br.address,
        location: new GeoPoint(br.lat, br.lng),
        phone: br.phone,
        openHours: br.openHours,
        type: br.type,
      },
      { merge: true }
    );
  }

  // 9. Seed utility_bills/{billId}
  const billDue = Timestamp.fromDate(new Date(now.getTime() + 10 * 24 * 60 * 60 * 1000));
  const bills = [
    {
      id: "bill_elec_01",
      type: "ELECTRIC",
      providerName: "EVN TP.HCM",
      customerCode: "PE0100001234",
      customerName: "Nguyễn Hoàng Anh",
      period: "Tháng hiện tại",
      amount: 850_000,
      status: "UNPAID",
    },
    {
      id: "bill_elec_high",
      type: "ELECTRIC",
      providerName: "EVN TP.HCM",
      customerCode: "PE0100009999",
      customerName: "Công ty TNHH NovaDemo",
      period: "Tháng hiện tại",
      amount: 6_200_000,
      status: "UNPAID",
    },
    {
      id: "bill_water_01",
      type: "WATER",
      providerName: "SAWACO Nhà Bè",
      customerCode: "NU0700005678",
      customerName: "Nguyễn Hoàng Anh",
      period: "Tháng hiện tại",
      amount: 240_000,
      status: "UNPAID",
    },
    {
      id: "bill_water_paid",
      type: "WATER",
      providerName: "SAWACO Nhà Bè",
      customerCode: "NU0700008888",
      customerName: "Nguyễn Hoàng Anh",
      period: "Tháng trước",
      amount: 195_000,
      status: "PAID",
    },
  ];
  for (const bill of bills) {
    batch.set(
      db.collection("utility_bills").doc(bill.id),
      {
        type: bill.type,
        providerName: bill.providerName,
        customerCode: bill.customerCode,
        customerName: bill.customerName,
        period: bill.period,
        amount: bill.amount,
        dueDate: billDue,
        status: bill.status,
      },
      { merge: true }
    );
  }

  // 10. Seed service_catalog/{itemId} (4 categories)
  const catalogItems = [
    {
      id: "svc_flight_sgn_han",
      category: "FLIGHT",
      title: "Vé máy bay TP.HCM (SGN) → Hà Nội (HAN)",
      price: 1_850_000,
      details: "Vietnam Airlines — Phổ thông linh hoạt, 23kg hành lý ký gửi",
      imageUrl: "",
    },
    {
      id: "svc_flight_sgn_dad",
      category: "FLIGHT",
      title: "Vé máy bay TP.HCM (SGN) → Đà Nẵng (DAD)",
      price: 1_150_000,
      details: "Vietjet Air — Bay thẳng 1h20p",
      imageUrl: "",
    },
    {
      id: "svc_movie_cgv_01",
      category: "MOVIE",
      title: "Vé xem phim CGV IMAX (2D/3D)",
      price: 150_000,
      details: "Áp dụng tại CGV Crescent Mall Quận 7",
      imageUrl: "",
    },
    {
      id: "svc_movie_combo",
      category: "MOVIE",
      title: "Combo Bắp Nước Đôi Lotte Cinema",
      price: 110_000,
      details: "1 Bắp lớn + 2 Nước ngọt tại Lotte Mart Q7",
      imageUrl: "",
    },
    {
      id: "svc_hotel_dalat",
      category: "HOTEL",
      title: "Khách sạn Dalat Heritage 4* (1 đêm)",
      price: 2_200_000,
      details: "Phòng Deluxe view thung lũng, bao gồm buffet sáng",
      imageUrl: "",
    },
    {
      id: "svc_hotel_phuquoc",
      category: "HOTEL",
      title: "Resort Phú Quốc Sunset 5* (1 đêm)",
      price: 5_800_000,
      details: "Biệt thự hướng biển (kích hoạt xác thực 3 lớp > 5M)",
      imageUrl: "",
    },
    {
      id: "svc_product_airpods",
      category: "PRODUCT",
      title: "Tai nghe không dây NovaAudio Pro",
      price: 3_490_000,
      details: "Chống ồn chủ động ANC, bảo hành chính hãng 12 tháng",
      imageUrl: "",
    },
    {
      id: "svc_product_watch",
      category: "PRODUCT",
      title: "Đồng hồ thông minh NovaWatch Ultra",
      price: 6_500_000,
      details: "Titanium GPS, thanh toán chạm NFC",
      imageUrl: "",
    },
  ];
  for (const item of catalogItems) {
    batch.set(
      db.collection("service_catalog").doc(item.id),
      {
        category: item.category,
        title: item.title,
        price: item.price,
        details: item.details,
        imageUrl: item.imageUrl,
      },
      { merge: true }
    );
  }

  await batch.commit();
  console.log("[NovaPay Seed] Đã khởi tạo dữ liệu mẫu thành công (Idempotent).");
  console.log("  Officer UID:", officerUid);
  console.log("  Customer Verified UID:", custVerifiedUid);
  console.log("  Customer Pending UID:", custPendingUid);
  console.log("  Customer Unverified UID:", custUnverifiedUid);
}

runSeed()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error("[NovaPay Seed] Lỗi khởi tạo dữ liệu:", err);
    process.exit(1);
  });
