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

function resolveEnvEmail(envKey: string, fallback: string): string {
  const raw = (process.env[envKey] || "").trim();
  return raw && raw.includes("@") ? raw : fallback;
}

function resolveOptionalEnvEmail(envKey: string): string | null {
  const raw = (process.env[envKey] || "").trim();
  return raw && raw.includes("@") ? raw : null;
}

async function upsertAuthUser(spec: SeedUserSpec): Promise<string> {
  const auth = admin.auth();
  let uid: string;
  try {
    const byUid = await auth.getUser(spec.defaultUid);
    await auth.updateUser(byUid.uid, {
      email: spec.email,
      password: spec.password,
      displayName: spec.displayName,
      emailVerified: true,
      disabled: false,
    });
    uid = byUid.uid;
  } catch (uidErr: unknown) {
    const uidCode = (uidErr as { code?: string })?.code;
    if (uidCode !== "auth/user-not-found") {
      throw uidErr;
    }
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
  }
  await auth.setCustomUserClaims(uid, { role: spec.role });
  return uid;
}

async function runSeed(): Promise<void> {
  initFirebase();
  const db = admin.firestore();
  const now = new Date();
  const nowTs = Timestamp.fromDate(now);

  // 1. Seed 3 Sandbox Auth users (Officer, Pending, Unverified) + 3 Team Member Auth users + Optional Sandbox Verified
  const pendingEmail = resolveEnvEmail("SEED_PENDING_EMAIL", "customer.pending@novapay.vn");
  const unverifiedEmail = resolveEnvEmail("SEED_UNVERIFIED_EMAIL", "customer.unverified@novapay.vn");
  const optionalVerifiedEmail = resolveOptionalEnvEmail("SEED_VERIFIED_EMAIL");

  const officerUid = await upsertAuthUser({
    defaultUid: "uid_officer_novapay_01",
    email: "officer@novapay.vn",
    password: "NovaPay@Officer2026",
    displayName: "Trần Minh Quân",
    role: "OFFICER",
  });

  const custPendingUid = await upsertAuthUser({
    defaultUid: "uid_customer_pending_02",
    email: pendingEmail,
    password: "NovaPay@User2026",
    displayName: "Lê Thị Thu Hà",
    role: "CUSTOMER",
  });

  const custUnverifiedUid = await upsertAuthUser({
    defaultUid: "uid_customer_unverified_03",
    email: unverifiedEmail,
    password: "NovaPay@User2026",
    displayName: "Phạm Quốc Bảo",
    role: "CUSTOMER",
  });

  let optionalVerifiedUid: string | null = null;
  if (optionalVerifiedEmail) {
    optionalVerifiedUid = await upsertAuthUser({
      defaultUid: "uid_customer_verified_01",
      email: optionalVerifiedEmail,
      password: "NovaPay@User2026",
      displayName: "Nguyễn Hoàng Anh",
      role: "CUSTOMER",
    });
  }

  interface TeamMemberSpec {
    defaultUid: string;
    uid?: string;
    fullName: string;
    email: string;
    password: string;
    phone: string;
    nationalId: string;
    dob: string;
    address: string;
    checkingAccountId: string;
    checkingNumber: string;
    checkingBalance: number;
    savingAccountId: string;
    savingNumber: string;
    savingBalance: number;
    savingTermMonths: number;
    savingRateId: string;
    mortgageAccountId: string;
    mortgageNumber: string;
    mortgageLoanAmount: number;
    mortgageOutstanding: number;
    mortgageTermMonths: number;
    mortgageFrequency: "MONTHLY" | "BIWEEKLY";
    mortgageInstallment: number;
    ledgerPrefix: string;
  }

  const teamMembers: TeamMemberSpec[] = [
    {
      defaultUid: "uid_member_nhan_01",
      fullName: "Nguyễn Phan Nhân",
      email: resolveEnvEmail("SEED_MEMBER_1_EMAIL", "member1.nhan@novapay.vn"),
      password: "NovaPay@2026",
      phone: "0902000001",
      nationalId: "079204000001",
      dob: "2004-06-15",
      address: "19 Nguyễn Hữu Thọ, Tân Phong, Quận 7, TP.HCM",
      checkingAccountId: "acc_member_nhan_checking",
      checkingNumber: "1001000001",
      checkingBalance: 50_000_000,
      savingAccountId: "acc_member_nhan_saving",
      savingNumber: "2001000001",
      savingBalance: 100_000_000,
      savingTermMonths: 12,
      savingRateId: "rate_12m",
      mortgageAccountId: "acc_member_nhan_mortgage",
      mortgageNumber: "3001000001",
      mortgageLoanAmount: 500_000_000,
      mortgageOutstanding: 350_000_000,
      mortgageTermMonths: 120,
      mortgageFrequency: "MONTHLY",
      mortgageInstallment: 6_200_000,
      ledgerPrefix: "nhan",
    },
    {
      defaultUid: "uid_member_dat_02",
      fullName: "Nguyễn Thành Đạt",
      email: resolveEnvEmail("SEED_MEMBER_2_EMAIL", "member2.dat@novapay.vn"),
      password: "NovaPay@2026",
      phone: "0902000002",
      nationalId: "079204000002",
      dob: "2004-08-20",
      address: "25 Nguyễn Thị Thập, Tân Hưng, Quận 7, TP.HCM",
      checkingAccountId: "acc_member_dat_checking",
      checkingNumber: "1001000002",
      checkingBalance: 50_000_000,
      savingAccountId: "acc_member_dat_saving",
      savingNumber: "2001000002",
      savingBalance: 80_000_000,
      savingTermMonths: 6,
      savingRateId: "rate_6m",
      mortgageAccountId: "acc_member_dat_mortgage",
      mortgageNumber: "3001000002",
      mortgageLoanAmount: 300_000_000,
      mortgageOutstanding: 240_000_000,
      mortgageTermMonths: 60,
      mortgageFrequency: "BIWEEKLY",
      mortgageInstallment: 3_500_000,
      ledgerPrefix: "dat",
    },
    {
      defaultUid: "uid_member_duy_03",
      fullName: "Nguyễn Văn Duy",
      email: resolveEnvEmail("SEED_MEMBER_3_EMAIL", "member3.duy@novapay.vn"),
      password: "NovaPay@2026",
      phone: "0902000003",
      nationalId: "079204000003",
      dob: "2004-07-18",
      address: "120 Lê Văn Lương, Tân Kiểng, Quận 7, TP.HCM",
      checkingAccountId: "acc_member_duy_checking",
      checkingNumber: "1001000003",
      checkingBalance: 50_000_000,
      savingAccountId: "acc_member_duy_saving",
      savingNumber: "2001000003",
      savingBalance: 120_000_000,
      savingTermMonths: 12,
      savingRateId: "rate_12m",
      mortgageAccountId: "acc_member_duy_mortgage",
      mortgageNumber: "3001000003",
      mortgageLoanAmount: 450_000_000,
      mortgageOutstanding: 300_000_000,
      mortgageTermMonths: 120,
      mortgageFrequency: "MONTHLY",
      mortgageInstallment: 5_500_000,
      ledgerPrefix: "duy",
    },
  ];

  for (const member of teamMembers) {
    member.uid = await upsertAuthUser({
      defaultUid: member.defaultUid,
      email: member.email,
      password: member.password,
      displayName: member.fullName,
      role: "CUSTOMER",
    });
  }

  // Bảo toàn ảnh eKYC thật (> 500 ký tự Base64) nếu thành viên đã chụp ảnh thật trước đó
  const existingRealEkycUids = new Set<string>();
  const ekycCheckUids = [
    custPendingUid,
    ...(optionalVerifiedUid ? [optionalVerifiedUid] : []),
    ...teamMembers.map((m) => m.uid as string),
  ];
  for (const uid of ekycCheckUids) {
    const snap = await db.collection("ekyc_images").doc(uid).get();
    const img = snap.exists ? String(snap.data()?.imageBase64 ?? "") : "";
    if (img.length > 500) {
      existingRealEkycUids.add(uid);
    }
  }

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

  if (optionalVerifiedUid && optionalVerifiedEmail) {
    batch.set(
      db.collection("users").doc(optionalVerifiedUid),
      {
        role: "CUSTOMER",
        fullName: "Nguyễn Hoàng Anh",
        email: optionalVerifiedEmail,
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
  }

  batch.set(
    db.collection("users").doc(custPendingUid),
    {
      role: "CUSTOMER",
      fullName: "Lê Thị Thu Hà",
      email: pendingEmail,
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
      email: unverifiedEmail,
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

  // 2b. Seed users/{uid} + beneficiaries cho 3 thành viên trong nhóm (VERIFIED)
  for (const member of teamMembers) {
    const memberUid = member.uid as string;
    batch.set(
      db.collection("users").doc(memberUid),
      {
        role: "CUSTOMER",
        fullName: member.fullName,
        email: member.email,
        phone: member.phone,
        nationalId: member.nationalId,
        dob: member.dob,
        address: member.address,
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

    // Danh bạ thụ hưởng: lưu sẵn 2 thành viên còn lại trong nhóm + 1 tài khoản VCB
    const peers = teamMembers.filter((other) => other.defaultUid !== member.defaultUid);
    peers.forEach((peer, idx) => {
      batch.set(
        db
          .collection("users")
          .doc(memberUid)
          .collection("beneficiaries")
          .doc(`ben_team_0${idx + 1}`),
        {
          name: peer.fullName,
          accountNumber: peer.checkingNumber,
          bankCode: "NOVAPAY",
          alias: peer.fullName,
        },
        { merge: true }
      );
    });
    batch.set(
      db.collection("users").doc(memberUid).collection("beneficiaries").doc("ben_vcb_01"),
      {
        name: "Trần Văn Bình",
        accountNumber: "0071000889911",
        bankCode: "VCB",
        alias: "Anh Bình VCB",
      },
      { merge: true }
    );
  }

  // 3. Seed ekyc_images/{uid} (chỉ nạp ảnh mẫu nếu chưa có ảnh mặt thật)
  if (optionalVerifiedUid && !existingRealEkycUids.has(optionalVerifiedUid)) {
    batch.set(
      db.collection("ekyc_images").doc(optionalVerifiedUid),
      {
        imageBase64: SAMPLE_EKYC_JPEG_BASE64,
        capturedAt: nowTs,
        capturedBy: optionalVerifiedUid,
      },
      { merge: true }
    );
  }
  if (!existingRealEkycUids.has(custPendingUid)) {
    batch.set(
      db.collection("ekyc_images").doc(custPendingUid),
      {
        imageBase64: SAMPLE_EKYC_JPEG_BASE64,
        capturedAt: nowTs,
        capturedBy: custPendingUid,
      },
      { merge: true }
    );
  }
  for (const member of teamMembers) {
    const memberUid = member.uid as string;
    if (!existingRealEkycUids.has(memberUid)) {
      batch.set(
        db.collection("ekyc_images").doc(memberUid),
        {
          imageBase64: SAMPLE_EKYC_JPEG_BASE64,
          capturedAt: nowTs,
          capturedBy: memberUid,
        },
        { merge: true }
      );
    }
  }

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

  if (optionalVerifiedUid) {
    batch.set(
      db.collection("accounts").doc("acc_cust1_checking"),
      {
        ownerUid: optionalVerifiedUid,
        type: "CHECKING",
        accountNumber: "1001000101",
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
  }

  batch.set(
    db.collection("accounts").doc("acc_cust2_checking"),
    {
      ownerUid: custPendingUid,
      type: "CHECKING",
      accountNumber: "1001000102",
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
      accountNumber: "1001000103",
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

  // 5b. Seed accounts (CHECKING 50M + SAVING + MORTGAGE) cho 3 thành viên nhóm
  for (const member of teamMembers) {
    const memberUid = member.uid as string;

    batch.set(
      db.collection("accounts").doc(member.checkingAccountId),
      {
        ownerUid: memberUid,
        type: "CHECKING",
        accountNumber: member.checkingNumber,
        status: "ACTIVE",
        balance: member.checkingBalance,
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
      db.collection("accounts").doc(member.savingAccountId),
      {
        ownerUid: memberUid,
        type: "SAVING",
        accountNumber: member.savingNumber,
        status: "ACTIVE",
        balance: member.savingBalance,
        currency: "VND",
        perTxnLimit: 0,
        dailyLimit: 0,
        saving: {
          termMonths: member.savingTermMonths,
          principal: member.savingBalance,
          interestRateId: member.savingRateId,
          openedAt,
          maturityDate,
        },
        mortgage: null,
        createdAt: openedAt,
      },
      { merge: true }
    );

    batch.set(
      db.collection("accounts").doc(member.mortgageAccountId),
      {
        ownerUid: memberUid,
        type: "MORTGAGE",
        accountNumber: member.mortgageNumber,
        status: "ACTIVE",
        balance: member.mortgageOutstanding,
        currency: "VND",
        perTxnLimit: 0,
        dailyLimit: 0,
        saving: null,
        mortgage: {
          loanAmount: member.mortgageLoanAmount,
          outstandingBalance: member.mortgageOutstanding,
          termMonths: member.mortgageTermMonths,
          annualRate: 8.5,
          paymentFrequency: member.mortgageFrequency,
          installmentAmount: member.mortgageInstallment,
          nextDueDate,
        },
        createdAt: openedAt,
      },
      { merge: true }
    );
  }

  // 6. Seed current-month ledger & transactions (always inside current month!)
  const startOfMonth = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), 1, 2, 0, 0));
  const dayOffsetMs = (days: number) =>
    Timestamp.fromDate(new Date(Math.min(now.getTime() - 60_000, startOfMonth.getTime() + days * 3_600_000)));

  // Seed bút toán mẫu trong tháng hiện tại cho 3 tài khoản thành viên nhóm (phục vụ biểu đồ Thu/Chi & Lịch sử)
  for (const member of teamMembers) {
    const memberUid = member.uid as string;
    const memberLedger = [
      {
        id: `led_member_${member.ledgerPrefix}_01`,
        txId: `tx_member_${member.ledgerPrefix}_01`,
        direction: "IN",
        amount: 52_000_000,
        balanceAfter: 52_000_000,
        description: "Nạp vốn khởi tạo tài khoản NovaPay",
        createdAt: dayOffsetMs(5),
        type: "DEPOSIT",
      },
      {
        id: `led_member_${member.ledgerPrefix}_02`,
        txId: `tx_member_${member.ledgerPrefix}_02`,
        direction: "OUT",
        amount: 2_000_000,
        balanceAfter: member.checkingBalance,
        description: "Thanh toán dịch vụ tiện ích trực tuyến",
        createdAt: dayOffsetMs(14),
        type: "SERVICE_PAYMENT",
      },
    ];

    for (const entry of memberLedger) {
      batch.set(
        db.collection("accounts").doc(member.checkingAccountId).collection("ledger").doc(entry.id),
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
          fromAccountId: member.checkingAccountId,
          fromAccountNumber: member.checkingNumber,
          toAccountId: null,
          amount: entry.amount,
          fee: 0,
          note: entry.description,
          status: "SUCCESS",
          requiresBiometric: false,
          biometricPassed: false,
          otpVerified: true,
          faceChecked: false,
          meta: {},
          initiatedBy: memberUid,
          createdAt: entry.createdAt,
        },
        { merge: true }
      );
    }
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
      name: "NovaBank PGD Nguyễn Hữu Thọ — Quận 7",
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
      customerName: "Công ty TNHH Thương mại Nam Sài Gòn",
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
      details: "Biệt thự hướng biển riêng tư, bao gồm đưa đón sân bay & buffet sáng",
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
  if (optionalVerifiedUid) {
    console.log("  Optional Sandbox Verified UID:", optionalVerifiedUid);
  }
  console.log("  Customer Pending UID:", custPendingUid);
  console.log("  Customer Unverified UID:", custUnverifiedUid);
  for (const member of teamMembers) {
    console.log(
      `  Team Member (${member.fullName}) UID: ${member.uid} | Checking: ${member.checkingNumber} (${member.checkingBalance.toLocaleString("vi-VN")} ₫)`
    );
  }
}

runSeed()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error("[NovaPay Seed] Lỗi khởi tạo dữ liệu:", err);
    process.exit(1);
  });
