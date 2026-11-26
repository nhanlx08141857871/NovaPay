import assert from "node:assert/strict";
import express, { NextFunction, Response } from "express";
import admin from "firebase-admin";
import { AddressInfo } from "node:net";
import { afterEach, describe, it } from "node:test";
import { AuthenticatedRequest } from "../src/middleware/auth";
import {
  computeMortgageAfterPayment,
  createTransactionsRouter,
  getHoChiMinhDayBounds,
} from "../src/routes/transactions";
import { EmailService } from "../src/services/email";
import { FaceCheckResult, FaceCheckService } from "../src/services/faceCheck";
import { otpService } from "../src/services/otp";
import { MockFirestore } from "./helpers/mockFirestore";

interface TestHarness {
  db: MockFirestore;
  baseUrl: string;
  sentEmails: Array<{ toEmail: string; otp: string; transactionType?: string; amountVnd?: number }>;
  setNow: (ms: number) => void;
  setUid: (uid: string) => void;
  setFaceResult: (result: FaceCheckResult) => void;
  close: () => Promise<void>;
}

async function createTestHarness(): Promise<TestHarness> {
  let currentNow = Date.UTC(2026, 10, 18, 4, 0, 0); // 11:00:00 UTC+7 (18/11/2026)
  let currentUid = "uid_1";
  let nextFaceResult: FaceCheckResult = {
    matched: true,
    manualAuditRequired: false,
    confidence: 0.92,
  };
  const sentEmails: TestHarness["sentEmails"] = [];
  const db = new MockFirestore(() => currentNow);

  db.seed("banks/VCB", { code: "VCB", name: "Vietcombank" });
  db.seed("users/uid_1", {
    fullName: "Trần Thị Lan",
    email: "lan@novapay.vn",
    status: "ACTIVE",
    ekyc: { status: "VERIFIED" },
  });
  db.seed("users/uid_2", {
    fullName: "Lê Thị Thu Hà",
    email: "ha@novapay.vn",
    status: "ACTIVE",
    ekyc: { status: "VERIFIED" },
  });
  db.seed("ekyc_images/uid_1", {
    imageBase64: "EKYC_REF_IMAGE_BASE64",
  });
  db.seed("accounts/acc_chk_1", {
    ownerUid: "uid_1",
    type: "CHECKING",
    accountNumber: "10000001",
    status: "ACTIVE",
    balance: 50_000_000,
    perTxnLimit: 20_000_000,
    dailyLimit: 30_000_000,
  });
  db.seed("accounts/acc_chk_2", {
    ownerUid: "uid_2",
    type: "CHECKING",
    accountNumber: "10000002",
    status: "ACTIVE",
    balance: 5_000_000,
    perTxnLimit: 20_000_000,
    dailyLimit: 30_000_000,
  });
  db.seed("accounts/acc_saving_1", {
    ownerUid: "uid_1",
    type: "SAVING",
    accountNumber: "20000001",
    status: "ACTIVE",
    balance: 100_000_000,
    perTxnLimit: 20_000_000,
    dailyLimit: 30_000_000,
  });
  db.seed("utility_bills/bill_1", {
    type: "ELECTRIC",
    providerName: "EVN HCMC",
    customerCode: "PE01001",
    amount: 450_000,
    status: "UNPAID",
  });
  db.seed("service_catalog/item_flight_1", {
    category: "FLIGHT",
    title: "SGN -> HAN",
    price: 1_500_000,
  });
  db.seed("accounts/acc_mort_monthly", {
    ownerUid: "uid_1",
    type: "MORTGAGE",
    accountNumber: "30000001",
    status: "ACTIVE",
    balance: 0,
    mortgage: {
      loanAmount: 100_000_000,
      outstandingBalance: 10_000_000,
      paymentFrequency: "MONTHLY",
      installmentAmount: 2_000_000,
      nextDueDate: admin.firestore.Timestamp.fromDate(new Date("2026-01-31T00:00:00.000+07:00")),
    },
  });
  db.seed("accounts/acc_mort_biweekly", {
    ownerUid: "uid_1",
    type: "MORTGAGE",
    accountNumber: "30000002",
    status: "ACTIVE",
    balance: 0,
    mortgage: {
      loanAmount: 20_000_000,
      outstandingBalance: 3_000_000,
      paymentFrequency: "BIWEEKLY",
      installmentAmount: 3_000_000,
      nextDueDate: admin.firestore.Timestamp.fromDate(new Date("2026-11-18T00:00:00.000+07:00")),
    },
  });

  const emailService: EmailService = {
    async sendOtpEmail(params) {
      sentEmails.push({
        toEmail: params.toEmail,
        otp: params.otp,
        transactionType: params.transactionType,
        amountVnd: params.amountVnd,
      });
    },
  };

  const faceCheckService: FaceCheckService = {
    async verifyFaceMatch() {
      return nextFaceResult;
    },
  };

  const app = express();
  app.use(express.json());
  app.use(
    "/transactions",
    createTransactionsRouter({
      getDb: () => db.asFirestore(),
      authMiddleware: (req: AuthenticatedRequest, _res: Response, next: NextFunction) => {
        req.user = { uid: currentUid, email: "lan@novapay.vn", role: "CUSTOMER" };
        next();
      },
      otpService,
      emailService,
      faceCheckService,
      now: () => currentNow,
    })
  );

  let server: ReturnType<express.Express["listen"]>;
  const baseUrl = await new Promise<string>((resolve) => {
    server = app.listen(0, "127.0.0.1", () => {
      const addr = server.address() as AddressInfo;
      resolve(`http://127.0.0.1:${addr.port}`);
    });
  });

  return {
    db,
    baseUrl,
    sentEmails,
    setNow: (ms) => {
      currentNow = ms;
    },
    setUid: (uid) => {
      currentUid = uid;
    },
    setFaceResult: (result) => {
      nextFaceResult = result;
    },
    close: () =>
      new Promise<void>((resolve, reject) => {
        server.close((err) => (err ? reject(err) : resolve()));
      }),
  };
}

describe("Transactions Pure Helpers", () => {
  it("computes Asia/Ho_Chi_Minh (UTC+7) day bounds accurately around UTC midnight", () => {
    // 2026-11-18T17:30:00Z is 2026-11-19T00:30:00+07:00
    const nowMs = Date.UTC(2026, 10, 18, 17, 30, 0);
    const { startUtc, endUtc } = getHoChiMinhDayBounds(nowMs);
    assert.equal(startUtc.toISOString(), "2026-11-18T17:00:00.000Z"); // 2026-11-19 00:00+07
    assert.equal(endUtc.toISOString(), "2026-11-19T17:00:00.000Z"); // 2026-11-20 00:00+07
  });

  it("advances MONTHLY (with month-end clamping) and BIWEEKLY mortgage schedules and closes at 0", () => {
    const monthly = computeMortgageAfterPayment(
      {
        outstandingBalance: 10_000_000,
        paymentFrequency: "MONTHLY",
        nextDueDate: new Date("2026-01-31T00:00:00.000+07:00"),
      },
      2_000_000
    );
    assert.equal(monthly.newOutstandingBalance, 8_000_000);
    assert.equal(monthly.shouldCloseAccount, false);
    assert.equal(
      monthly.newNextDueDate.toISOString(),
      new Date("2026-02-28T00:00:00.000+07:00").toISOString()
    );

    const biweeklyPaidOff = computeMortgageAfterPayment(
      {
        outstandingBalance: 3_000_000,
        paymentFrequency: "BIWEEKLY",
        nextDueDate: new Date("2026-11-18T00:00:00.000+07:00"),
      },
      3_000_000
    );
    assert.equal(biweeklyPaidOff.newOutstandingBalance, 0);
    assert.equal(biweeklyPaidOff.shouldCloseAccount, true);
    assert.equal(
      biweeklyPaidOff.newNextDueDate.toISOString(),
      new Date("2026-12-02T00:00:00.000+07:00").toISOString()
    );
  });
});

describe("Transactions Routes", () => {
  const harnesses: TestHarness[] = [];

  afterEach(async () => {
    while (harnesses.length > 0) {
      const h = harnesses.pop();
      if (h) {
        await h.close();
      }
    }
  });

  it("completes <= 5M TRANSFER_INTERNAL atomically with IN/OUT ledger and index-compliant daily query", async () => {
    const h = await createTestHarness();
    harnesses.push(h);

    // Seed a GATEWAY transaction today (must be excluded from daily spend sum in memory!)
    h.db.seed("transactions/tx_gateway_today", {
      type: "GATEWAY",
      fromAccountId: "acc_chk_1",
      amount: 25_000_000,
      status: "SUCCESS",
      createdAt: admin.firestore.Timestamp.fromDate(new Date(Date.UTC(2026, 10, 18, 2, 0, 0))),
    });

    const initRes = await fetch(`${h.baseUrl}/transactions/initiate`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        type: "TRANSFER_INTERNAL",
        fromAccountId: "acc_chk_1",
        toAccountId: "acc_chk_2",
        amount: 2_000_000,
        note: "Chuyển tiền ăn trưa",
      }),
    });
    assert.equal(initRes.status, 200);
    const initJson = (await initRes.json()) as {
      draftId: string;
      requiresBiometric: boolean;
      requiresFace: boolean;
      otpExpiresAt: number;
    };
    assert.equal(initJson.requiresBiometric, false);
    assert.equal(initJson.requiresFace, false);
    assert.equal(typeof initJson.otpExpiresAt, "number");
    assert.equal(h.sentEmails.length, 1);

    const confirmRes = await fetch(`${h.baseUrl}/transactions/confirm`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        draftId: initJson.draftId,
        otp: h.sentEmails[0].otp,
        biometricPassed: false,
      }),
    });
    assert.equal(confirmRes.status, 200);
    const confirmJson = (await confirmRes.json()) as { txId: string; status: string };
    assert.equal(confirmJson.status, "SUCCESS");
    assert.ok(confirmJson.txId);

    assert.equal(h.db.getRaw("accounts/acc_chk_1")?.balance, 48_000_000);
    assert.equal(h.db.getRaw("accounts/acc_chk_2")?.balance, 7_000_000);

    // Verify receipt snapshot fields persisted in transactions/{txId}
    const savedTx = h.db.getRaw(`transactions/${confirmJson.txId}`);
    assert.equal(savedTx?.fromAccountNumber, "10000001");
    assert.equal(savedTx?.toAccountNumber, "10000002");
    assert.equal(savedTx?.toOwnerName, "Lê Thị Thu Hà");

    const fromLedger = h.db.listCollection("accounts/acc_chk_1/ledger");
    const toLedger = h.db.listCollection("accounts/acc_chk_2/ledger");
    assert.equal(fromLedger.length, 1);
    assert.equal(fromLedger[0].data.direction, "OUT");
    assert.equal(fromLedger[0].data.balanceAfter, 48_000_000);
    assert.equal(toLedger.length, 1);
    assert.equal(toLedger[0].data.direction, "IN");
    assert.equal(toLedger[0].data.balanceAfter, 7_000_000);

    // Verify daily-limit queries strictly match firestore.indexes.json (fromAccountId, status, createdAt)
    const txQueries = h.db.recordedQueries.filter((q) => q.collectionPath === "transactions");
    assert.equal(txQueries.length, 2); // /initiate + /confirm
    for (const q of txQueries) {
      assert.deepEqual(
        q.filters.map((f) => [f.field, f.op]),
        [
          ["fromAccountId", "=="],
          ["status", "=="],
          ["createdAt", ">="],
          ["createdAt", "<"],
        ]
      );
    }
  });

  it("enforces > 5M 3-layer security: PENDING_FACE (no OTP yet) -> /face-check -> Biometric -> /confirm", async () => {
    const h = await createTestHarness();
    harnesses.push(h);

    const initRes = await fetch(`${h.baseUrl}/transactions/initiate`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        type: "TRANSFER_INTERBANK",
        fromAccountId: "acc_chk_1",
        toExternal: {
          bankCode: "VCB",
          accountNumber: "0071000999888",
          name: "Lê Hoàng Nam",
        },
        amount: 6_000_000,
      }),
    });
    assert.equal(initRes.status, 200);
    const initJson = (await initRes.json()) as {
      draftId: string;
      requiresBiometric: boolean;
      requiresFace: boolean;
      otpExpiresAt: number | null;
    };
    assert.equal(initJson.requiresFace, true);
    assert.equal(initJson.requiresBiometric, true);
    assert.equal(initJson.otpExpiresAt, null);
    assert.equal(h.sentEmails.length, 0); // Không gửi OTP trước FaceCheck!

    // Reject face mismatch with 403 FACE_MISMATCH
    h.setFaceResult({ matched: false, manualAuditRequired: false, confidence: 0.42 });
    const failFaceRes = await fetch(`${h.baseUrl}/transactions/face-check`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        draftId: initJson.draftId,
        imageBase64: "data:image/jpeg;base64,LIVE_WRONG_FACE",
      }),
    });
    assert.equal(failFaceRes.status, 403);
    assert.equal(((await failFaceRes.json()) as { error: string }).error, "FACE_MISMATCH");
    assert.equal(h.sentEmails.length, 0);

    // Fallback pass (429/timeout) -> writes face_checks with manualAuditRequired=true and sends OTP
    h.setFaceResult({ matched: true, manualAuditRequired: true, confidence: null });
    const passFaceRes = await fetch(`${h.baseUrl}/transactions/face-check`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        draftId: initJson.draftId,
        imageBase64: "data:image/jpeg;base64,LIVE_OK_FACE",
      }),
    });
    assert.equal(passFaceRes.status, 200);
    const passFaceJson = (await passFaceRes.json()) as { ok: boolean; otpExpiresAt: number };
    assert.equal(passFaceJson.ok, true);
    assert.equal(typeof passFaceJson.otpExpiresAt, "number");
    assert.equal(h.sentEmails.length, 1);

    const faceCheckDoc = h.db.getRaw(`face_checks/${initJson.draftId}`);
    assert.equal(faceCheckDoc?.manualAuditRequired, true);
    assert.equal(faceCheckDoc?.imageBase64, "LIVE_OK_FACE");

    // Must reject biometricPassed: false with BIOMETRIC_REQUIRED
    const noBioRes = await fetch(`${h.baseUrl}/transactions/confirm`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        draftId: initJson.draftId,
        otp: h.sentEmails[0].otp,
        biometricPassed: false,
      }),
    });
    assert.equal(noBioRes.status, 403);
    assert.equal(((await noBioRes.json()) as { error: string }).error, "BIOMETRIC_REQUIRED");

    // Succeed with biometricPassed: true
    const okRes = await fetch(`${h.baseUrl}/transactions/confirm`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        draftId: initJson.draftId,
        otp: h.sentEmails[0].otp,
        biometricPassed: true,
      }),
    });
    assert.equal(okRes.status, 200);
    const okJson = (await okRes.json()) as { txId: string; status: string };
    assert.equal(okJson.status, "SUCCESS");
    assert.equal(h.db.getRaw(`face_checks/${initJson.draftId}`)?.txId, okJson.txId);
    assert.equal(h.db.getRaw("accounts/acc_chk_1")?.balance, 44_000_000);
    const interbankTx = h.db.getRaw(`transactions/${okJson.txId}`);
    assert.equal(interbankTx?.fromAccountNumber, "10000001");
    assert.equal(interbankTx?.toAccountNumber, "0071000999888");
    assert.equal(interbankTx?.toOwnerName, "Lê Hoàng Nam");
  });

  it("keeps draft PENDING_OTP on OTP_EXPIRED so /resend-otp succeeds, and locks after 5 wrong attempts", async () => {
    const h = await createTestHarness();
    harnesses.push(h);

    const initRes = await fetch(`${h.baseUrl}/transactions/initiate`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        type: "TOPUP",
        fromAccountId: "acc_chk_1",
        amount: 100_000,
        meta: { phone: "0909111222" },
      }),
    });
    const { draftId } = (await initRes.json()) as { draftId: string };
    const firstOtp = h.sentEmails[0].otp;

    // Advance past 180s
    h.setNow(Date.UTC(2026, 10, 18, 4, 4, 0));
    const expiredRes = await fetch(`${h.baseUrl}/transactions/confirm`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ draftId, otp: firstOtp, biometricPassed: false }),
    });
    assert.equal(expiredRes.status, 400);
    assert.equal(((await expiredRes.json()) as { error: string }).error, "OTP_EXPIRED");
    assert.equal(h.db.getRaw(`transaction_drafts/${draftId}`)?.status, "PENDING_OTP");

    // Resend OTP works!
    const resendRes = await fetch(`${h.baseUrl}/transactions/resend-otp`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ draftId }),
    });
    assert.equal(resendRes.status, 200);
    assert.equal(h.sentEmails.length, 2);

    // 4 wrong attempts -> OTP_INVALID, 5th -> TOO_MANY_ATTEMPTS
    for (let attempt = 1; attempt <= 4; attempt++) {
      const badRes = await fetch(`${h.baseUrl}/transactions/confirm`, {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ draftId, otp: "999999", biometricPassed: false }),
      });
      assert.equal(badRes.status, 400);
      assert.equal(((await badRes.json()) as { error: string }).error, "OTP_INVALID");
    }

    const fifthRes = await fetch(`${h.baseUrl}/transactions/confirm`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ draftId, otp: "999999", biometricPassed: false }),
    });
    assert.equal(fifthRes.status, 429);
    assert.equal(((await fifthRes.json()) as { error: string }).error, "TOO_MANY_ATTEMPTS");
    assert.equal(h.db.getRaw(`transaction_drafts/${draftId}`)?.status, "EXPIRED");
  });

  it("handles BILL, SERVICE_PAYMENT, and MORTGAGE_PAYMENT (MONTHLY + BIWEEKLY full payoff -> CLOSED)", async () => {
    const h = await createTestHarness();
    harnesses.push(h);

    // 1. BILL
    const billInit = await fetch(`${h.baseUrl}/transactions/initiate`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        type: "BILL",
        fromAccountId: "acc_chk_1",
        amount: 450_000,
        meta: { billId: "bill_1" },
      }),
    });
    assert.equal(billInit.status, 200);
    const { draftId: billDraft } = (await billInit.json()) as { draftId: string };
    const billConfirm = await fetch(`${h.baseUrl}/transactions/confirm`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ draftId: billDraft, otp: h.sentEmails[0].otp, biometricPassed: false }),
    });
    assert.equal(billConfirm.status, 200);
    const billConfirmJson = (await billConfirm.json()) as { txId: string };
    assert.equal(h.db.getRaw("utility_bills/bill_1")?.status, "PAID");
    const billTx = h.db.getRaw(`transactions/${billConfirmJson.txId}`);
    assert.equal(billTx?.fromAccountNumber, "10000001");
    assert.equal(billTx?.toAccountNumber, "PE01001");
    assert.equal(billTx?.toOwnerName, "EVN HCMC");

    // 2. SERVICE_PAYMENT
    const svcInit = await fetch(`${h.baseUrl}/transactions/initiate`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        type: "SERVICE_PAYMENT",
        fromAccountId: "acc_chk_1",
        amount: 3_000_000,
        meta: { itemId: "item_flight_1", quantity: 2 },
      }),
    });
    assert.equal(svcInit.status, 200);
    const { draftId: svcDraft } = (await svcInit.json()) as { draftId: string };
    const svcConfirm = await fetch(`${h.baseUrl}/transactions/confirm`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ draftId: svcDraft, otp: h.sentEmails[1].otp, biometricPassed: false }),
    });
    assert.equal(svcConfirm.status, 200);
    const svcConfirmJson = (await svcConfirm.json()) as { txId: string };
    const orders = h.db.listCollection("orders");
    assert.equal(orders.length, 1);
    assert.equal(orders[0].data.uid, "uid_1");
    assert.equal(orders[0].data.quantity, 2);
    assert.equal(orders[0].data.totalAmount, 3_000_000);
    assert.equal(orders[0].data.status, "PAID");
    const svcTx = h.db.getRaw(`transactions/${svcConfirmJson.txId}`);
    assert.equal(svcTx?.fromAccountNumber, "10000001");
    assert.equal(svcTx?.toAccountNumber, orders[0].id);
    assert.equal(svcTx?.toOwnerName, "SGN -> HAN");

    // 3. MORTGAGE_PAYMENT BIWEEKLY full payoff -> CLOSED
    const mortInit = await fetch(`${h.baseUrl}/transactions/initiate`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        type: "MORTGAGE_PAYMENT",
        fromAccountId: "acc_chk_1",
        amount: 3_000_000,
        meta: { mortgageAccountId: "acc_mort_biweekly" },
      }),
    });
    assert.equal(mortInit.status, 200);
    const { draftId: mortDraft } = (await mortInit.json()) as { draftId: string };
    const mortConfirm = await fetch(`${h.baseUrl}/transactions/confirm`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ draftId: mortDraft, otp: h.sentEmails[2].otp, biometricPassed: false }),
    });
    assert.equal(mortConfirm.status, 200);
    const mortConfirmJson = (await mortConfirm.json()) as { txId: string };
    const mortAcc = h.db.getRaw("accounts/acc_mort_biweekly");
    assert.equal(mortAcc?.status, "CLOSED");
    assert.equal((mortAcc?.mortgage as { outstandingBalance: number }).outstandingBalance, 0);
    assert.equal(h.db.listCollection("accounts/acc_mort_biweekly/ledger").length, 1);
    const mortTx = h.db.getRaw(`transactions/${mortConfirmJson.txId}`);
    assert.equal(mortTx?.fromAccountNumber, "10000001");
    assert.equal(mortTx?.toAccountNumber, "30000002");
    assert.equal(mortTx?.toOwnerName, "Trần Thị Lan");
  });

  it("rejects DEPOSIT, non-CHECKING source, self-transfer, and daily limit overflow", async () => {
    const h = await createTestHarness();
    harnesses.push(h);

    const depositRes = await fetch(`${h.baseUrl}/transactions/initiate`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        type: "DEPOSIT",
        fromAccountId: "acc_chk_1",
        amount: 1_000_000,
      }),
    });
    assert.equal(depositRes.status, 400);
    assert.equal(((await depositRes.json()) as { error: string }).error, "INVALID_ARGUMENT");

    const savingRes = await fetch(`${h.baseUrl}/transactions/initiate`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        type: "WITHDRAW",
        fromAccountId: "acc_saving_1",
        amount: 1_000_000,
      }),
    });
    assert.equal(savingRes.status, 400);
    assert.equal(((await savingRes.json()) as { error: string }).error, "INVALID_ARGUMENT");

    const selfRes = await fetch(`${h.baseUrl}/transactions/initiate`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        type: "TRANSFER_INTERNAL",
        fromAccountId: "acc_chk_1",
        toAccountId: "acc_chk_1",
        amount: 1_000_000,
      }),
    });
    assert.equal(selfRes.status, 400);
    assert.equal(((await selfRes.json()) as { error: string }).error, "INVALID_ARGUMENT");

    // Seed 28M spent today; attempting 3M against 30M dailyLimit must fail LIMIT_EXCEEDED
    h.db.seed("transactions/tx_spent_28m", {
      type: "TRANSFER_INTERNAL",
      fromAccountId: "acc_chk_1",
      amount: 28_000_000,
      status: "SUCCESS",
      createdAt: admin.firestore.Timestamp.fromDate(new Date(Date.UTC(2026, 10, 18, 1, 0, 0))),
    });
    const limitRes = await fetch(`${h.baseUrl}/transactions/initiate`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        type: "WITHDRAW",
        fromAccountId: "acc_chk_1",
        amount: 3_000_000,
      }),
    });
    assert.equal(limitRes.status, 400);
    assert.equal(((await limitRes.json()) as { error: string }).error, "LIMIT_EXCEEDED");
  });
  it("locks a PENDING_FACE draft after 5 failed FaceCheck attempts", async () => {
    const h = await createTestHarness();
    harnesses.push(h);

    const initRes = await fetch(`${h.baseUrl}/transactions/initiate`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        type: "TRANSFER_INTERBANK",
        fromAccountId: "acc_chk_1",
        toExternal: { bankCode: "VCB", accountNumber: "0071000999888", name: "Lê Hoàng Nam" },
        amount: 6_000_000,
      }),
    });
    const { draftId } = (await initRes.json()) as { draftId: string };

    h.setFaceResult({ matched: false, manualAuditRequired: false, confidence: 0.2 });
    const faceCheck = () =>
      fetch(`${h.baseUrl}/transactions/face-check`, {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ draftId, imageBase64: "data:image/jpeg;base64,LIVE" }),
      });

    for (let attempt = 1; attempt <= 4; attempt++) {
      const res = await faceCheck();
      assert.equal(res.status, 403);
      assert.equal(((await res.json()) as { error: string }).error, "FACE_MISMATCH");
    }
    const lockedRes = await faceCheck();
    assert.equal(lockedRes.status, 429);
    assert.equal(((await lockedRes.json()) as { error: string }).error, "TOO_MANY_ATTEMPTS");
    assert.equal(h.db.getRaw(`transaction_drafts/${draftId}`)?.status, "EXPIRED");

    h.setFaceResult({ matched: true, manualAuditRequired: false, confidence: 0.95 });
    const afterLockRes = await faceCheck();
    assert.equal(afterLockRes.status, 400);
    assert.equal(h.sentEmails.length, 0);
  });

  it("throttles /resend-otp with a cooldown and a maximum number of resends", async () => {
    const h = await createTestHarness();
    harnesses.push(h);

    const initRes = await fetch(`${h.baseUrl}/transactions/initiate`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        type: "TOPUP",
        fromAccountId: "acc_chk_1",
        amount: 100_000,
        meta: { phone: "0909111222" },
      }),
    });
    const { draftId } = (await initRes.json()) as { draftId: string };
    const resend = () =>
      fetch(`${h.baseUrl}/transactions/resend-otp`, {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ draftId }),
      });

    const tooSoonRes = await resend();
    assert.equal(tooSoonRes.status, 429);
    assert.equal(((await tooSoonRes.json()) as { error: string }).error, "RESEND_TOO_SOON");
    assert.equal(h.sentEmails.length, 1);

    let clock = Date.UTC(2026, 10, 18, 4, 0, 0);
    for (let i = 1; i <= 5; i++) {
      clock += 31_000;
      h.setNow(clock);
      const okRes = await resend();
      assert.equal(okRes.status, 200);
    }
    assert.equal(h.sentEmails.length, 6);

    clock += 31_000;
    h.setNow(clock);
    const cappedRes = await resend();
    assert.equal(cappedRes.status, 429);
    assert.equal(((await cappedRes.json()) as { error: string }).error, "TOO_MANY_ATTEMPTS");
    assert.equal(h.sentEmails.length, 6);
  });

  it("rejects unknown interbank bank codes and oversized gateway deposits", async () => {
    const h = await createTestHarness();
    harnesses.push(h);

    const bankRes = await fetch(`${h.baseUrl}/transactions/initiate`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        type: "TRANSFER_INTERBANK",
        fromAccountId: "acc_chk_1",
        toExternal: { bankCode: "ZZZ", accountNumber: "0071000999888", name: "Lê Hoàng Nam" },
        amount: 100_000,
      }),
    });
    assert.equal(bankRes.status, 400);

    const depositRes = await fetch(`${h.baseUrl}/transactions/initiate`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ type: "GATEWAY", fromAccountId: "acc_chk_1", amount: 100_000_001 }),
    });
    assert.equal(depositRes.status, 400);
    assert.equal(((await depositRes.json()) as { error: string }).error, "LIMIT_EXCEEDED");
  });
});
