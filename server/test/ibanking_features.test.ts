import assert from "node:assert/strict";
import express, { NextFunction, Response } from "express";
import admin from "firebase-admin";
import { AddressInfo } from "node:net";
import { afterEach, describe, it } from "node:test";
import { AuthenticatedRequest } from "../src/middleware/auth";
import { createEkycRouter } from "../src/routes/ekyc";
import { createPaymentsRouter } from "../src/routes/payments";
import {
  addCalendarMonthsHoChiMinh,
  computeSavingsSettlementAmounts,
  createSavingsRouter,
} from "../src/routes/savings";
import { createSecurityRouter } from "../src/routes/security";
import { createTransactionsRouter } from "../src/routes/transactions";
import { EmailService } from "../src/services/email";
import { otpService } from "../src/services/otp";
import { createPinService, PIN_LOCKOUT_MS } from "../src/services/pin";
import { StripeService } from "../src/services/stripe";
import { MockFirestore, MockTransaction } from "./helpers/mockFirestore";

interface FeaturesHarness {
  db: MockFirestore;
  baseUrl: string;
  sentOtps: string[];
  setNow: (ms: number) => void;
  getNow: () => number;
  setActor: (uid: string, role?: "CUSTOMER" | "OFFICER") => void;
  close: () => Promise<void>;
}

async function createFeaturesHarness(): Promise<FeaturesHarness> {
  let currentNow = Date.UTC(2026, 10, 1, 3, 0, 0); // 2026-11-01T10:00:00+07:00
  let actorUid = "uid_cust_1";
  let actorRole: "CUSTOMER" | "OFFICER" = "CUSTOMER";
  const sentOtps: string[] = [];
  const db = new MockFirestore(() => currentNow);
  const pinSvc = createPinService({ hmacSecret: "test-pin-secret-key" });

  db.seed("users/uid_cust_1", {
    role: "CUSTOMER",
    fullName: "Nguyễn Văn An",
    email: "an@novapay.vn",
    status: "ACTIVE",
    ekyc: { status: "VERIFIED" },
  });
  db.seed("users/uid_cust_2", {
    role: "CUSTOMER",
    fullName: "Trần Thị Bình",
    email: "binh@novapay.vn",
    status: "ACTIVE",
    ekyc: { status: "PENDING" },
  });
  db.seed("users/uid_officer_1", {
    role: "OFFICER",
    fullName: "Lê Minh Quân",
    email: "quan@novapay.vn",
    status: "ACTIVE",
    ekyc: { status: "VERIFIED" },
  });
  db.seed("ekyc_images/uid_cust_2", {
    imageBase64: "VALID_REF_IMAGE",
  });

  db.seed("accounts/acc_chk_1", {
    ownerUid: "uid_cust_1",
    type: "CHECKING",
    accountNumber: "1001000001",
    status: "ACTIVE",
    balance: 50_000_000,
    currency: "VND",
    perTxnLimit: 20_000_000,
    dailyLimit: 50_000_000,
  });
  db.seed("accounts/acc_chk_2", {
    ownerUid: "uid_cust_2",
    type: "CHECKING",
    accountNumber: "1001000002",
    status: "ACTIVE",
    balance: 5_000_000,
    currency: "VND",
    perTxnLimit: 20_000_000,
    dailyLimit: 50_000_000,
  });

  db.seed("interest_rates/rate_0m", { termMonths: 0, annualRate: 0.5 });
  db.seed("interest_rates/rate_1m", { termMonths: 1, annualRate: 3.5 });
  db.seed("interest_rates/rate_6m", { termMonths: 6, annualRate: 5.2 });
  db.seed("interest_rates/rate_12m", { termMonths: 12, annualRate: 6.0 });

  const emailService: EmailService = {
    async sendOtpEmail(params) {
      sentOtps.push(params.otp);
    },
  };

  const stripeService: StripeService = {
    async createPaymentIntent(params) {
      return {
        stripeIntentId: `pi_${params.draftId}`,
        clientSecret: `cs_${params.draftId}`,
      };
    },
    async retrievePaymentIntent(paymentIntentId) {
      return {
        id: paymentIntentId,
        status: "succeeded",
        amount: 4_000_000,
        currency: "vnd",
      };
    },
  };

  const authMiddleware = (req: AuthenticatedRequest, _res: Response, next: NextFunction) => {
    req.user = { uid: actorUid, email: `${actorUid}@novapay.vn`, role: actorRole };
    next();
  };
  const officerMiddleware = (req: AuthenticatedRequest, res: Response, next: NextFunction) => {
    if (req.user?.role !== "OFFICER") {
      res.status(403).json({ error: "PERMISSION_DENIED", message: "Forbidden" });
      return;
    }
    next();
  };

  const app = express();
  app.use(express.json());
  app.use(
    "/savings",
    createSavingsRouter({
      getDb: () => db.asFirestore(),
      authMiddleware,
      pinService: pinSvc,
      now: () => currentNow,
      savingAccountNumberGenerator: () => "2099000001",
    })
  );
  app.use(
    "/security",
    createSecurityRouter({
      getDb: () => db.asFirestore(),
      authMiddleware,
      pinService: pinSvc,
      now: () => currentNow,
    })
  );
  app.use(
    "/transactions",
    createTransactionsRouter({
      getDb: () => db.asFirestore(),
      authMiddleware,
      otpService,
      emailService,
      stripeService,
      now: () => currentNow,
    })
  );
  app.use(
    "/payments",
    createPaymentsRouter({
      getDb: () => db.asFirestore(),
      authMiddleware,
      stripeService,
    })
  );
  app.use(
    "/ekyc",
    createEkycRouter({
      getDb: () => db.asFirestore(),
      authMiddleware,
      officerMiddleware,
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
    sentOtps,
    setNow: (ms) => {
      currentNow = ms;
    },
    getNow: () => currentNow,
    setActor: (uid, role = "CUSTOMER") => {
      actorUid = uid;
      actorRole = role;
    },
    close: () =>
      new Promise<void>((resolve, reject) => {
        server.close((err) => (err ? reject(err) : resolve()));
      }),
  };
}

describe("iBanking Upgrade — Savings, Security PIN/Controls & Notifications", () => {
  const harnesses: FeaturesHarness[] = [];

  afterEach(async () => {
    while (harnesses.length > 0) {
      const h = harnesses.pop();
      if (h) {
        await h.close();
      }
    }
  });

  it("computes maturity date and integer settlement interest accurately (matured vs early)", () => {
    const startMs = Date.UTC(2026, 0, 31, 3, 0, 0); // 2026-01-31 10:00 +07:00
    const matured1m = addCalendarMonthsHoChiMinh(startMs, 1);
    assert.equal(matured1m.toISOString(), "2026-02-28T03:00:00.000Z");

    const maturedCalc = computeSavingsSettlementAmounts({
      principalAmount: 10_000_000,
      termMonths: 12,
      termAnnualRate: 6.0,
      demandAnnualRate: 0.5,
      openedAtMs: startMs,
      maturityDateMs: startMs + 365 * 86_400_000,
      nowMs: startMs + 365 * 86_400_000,
    });
    assert.equal(maturedCalc.earlySettlement, false);
    assert.equal(maturedCalc.appliedAnnualRate, 6.0);
    assert.equal(maturedCalc.interestAmount, 600_000);
    assert.equal(maturedCalc.totalPayoutAmount, 10_600_000);

    const earlyCalc = computeSavingsSettlementAmounts({
      principalAmount: 10_000_000,
      termMonths: 12,
      termAnnualRate: 6.0,
      demandAnnualRate: 0.5,
      openedAtMs: startMs,
      maturityDateMs: startMs + 365 * 86_400_000,
      nowMs: startMs + 30 * 86_400_000,
    });
    assert.equal(earlyCalc.earlySettlement, true);
    assert.equal(earlyCalc.appliedAnnualRate, 0.5);
    // floor((10_000_000 * 0.5 * 30) / (365 * 100)) = floor(4109.589...) = 4109
    assert.equal(earlyCalc.interestAmount, 4109);
    assert.equal(earlyCalc.totalPayoutAmount, 10_004_109);
  });

  it("manages 6-digit transaction PIN lifecycle: set, change, 5-attempt 15-min lockout & recovery", async () => {
    const h = await createFeaturesHarness();
    harnesses.push(h);

    // 1. Initial status
    const status0 = await fetch(`${h.baseUrl}/security/status`);
    assert.equal(status0.status, 200);
    assert.deepEqual(await status0.json(), {
      hasTransactionPin: false,
      pinLockedUntilEpochMs: null,
    });

    // 2. Reject invalid PIN format
    const badPinRes = await fetch(`${h.baseUrl}/security/pin/set`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ newPin: "12345" }),
    });
    assert.equal(badPinRes.status, 400);
    assert.equal(((await badPinRes.json()) as { error: string }).error, "INVALID_ARGUMENT");

    // 3. Set initial PIN
    const setRes = await fetch(`${h.baseUrl}/security/pin/set`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ newPin: "246810" }),
    });
    assert.equal(setRes.status, 200);
    assert.deepEqual(await setRes.json(), { ok: true, hasTransactionPin: true });

    const secretDoc = h.db.getRaw("user_secrets/uid_cust_1");
    assert.ok(secretDoc?.pinHash);
    assert.notEqual(secretDoc?.pinHash, "246810");
    assert.equal(h.db.getRaw("users/uid_cust_1")?.security && (h.db.getRaw("users/uid_cust_1")?.security as { hasPin: boolean }).hasPin, true);

    const notifsAfterSet = h.db.listCollection("users/uid_cust_1/notifications");
    assert.equal(notifsAfterSet.length, 1);
    assert.equal(notifsAfterSet[0].data.category, "SECURITY");
    assert.equal(notifsAfterSet[0].data.read, false);

    // 4. Require currentPin when changing existing PIN
    const missingCurrRes = await fetch(`${h.baseUrl}/security/pin/set`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ newPin: "135790" }),
    });
    assert.equal(missingCurrRes.status, 400);
    assert.equal(((await missingCurrRes.json()) as { error: string }).error, "INVALID_ARGUMENT");

    // 5. Lock after 5 wrong PIN attempts for 15 minutes
    for (let i = 1; i <= 4; i++) {
      const wrongRes = await fetch(`${h.baseUrl}/security/pin/set`, {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ currentPin: "000000", newPin: "135790" }),
      });
      assert.equal(wrongRes.status, 400);
      assert.equal(((await wrongRes.json()) as { error: string }).error, "PIN_INVALID");
    }

    const fifthWrongRes = await fetch(`${h.baseUrl}/security/pin/set`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ currentPin: "000000", newPin: "135790" }),
    });
    assert.equal(fifthWrongRes.status, 429);
    assert.equal(((await fifthWrongRes.json()) as { error: string }).error, "PIN_LOCKED");

    const statusLocked = await (await fetch(`${h.baseUrl}/security/status`)).json() as {
      hasTransactionPin: boolean;
      pinLockedUntilEpochMs: number | null;
    };
    assert.equal(statusLocked.hasTransactionPin, true);
    assert.equal(statusLocked.pinLockedUntilEpochMs, h.getNow() + PIN_LOCKOUT_MS);

    // Even correct PIN is rejected while locked
    const duringLockRes = await fetch(`${h.baseUrl}/security/pin/set`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ currentPin: "246810", newPin: "135790" }),
    });
    assert.equal(duringLockRes.status, 429);
    assert.equal(((await duringLockRes.json()) as { error: string }).error, "PIN_LOCKED");

    // 6. Advance clock past 15 minutes -> valid PIN change succeeds
    h.setNow(h.getNow() + PIN_LOCKOUT_MS + 1000);
    const changeOkRes = await fetch(`${h.baseUrl}/security/pin/set`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ currentPin: "246810", newPin: "135790" }),
    });
    assert.equal(changeOkRes.status, 200);
    assert.deepEqual(await changeOkRes.json(), { ok: true, hasTransactionPin: true });
    assert.equal(h.db.getRaw("user_secrets/uid_cust_1")?.pinAttempts, 0);
  });

  it("enforces CHECKING account controls: freeze without PIN, PIN required to unfreeze or raise limits, bounds validation", async () => {
    const h = await createFeaturesHarness();
    harnesses.push(h);

    await fetch(`${h.baseUrl}/security/pin/set`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ newPin: "112233" }),
    });

    // 1. Freeze account without PIN -> succeeds
    const freezeRes = await fetch(`${h.baseUrl}/security/accounts/acc_chk_1/controls`, {
      method: "PATCH",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ status: "FROZEN" }),
    });
    assert.equal(freezeRes.status, 200);
    assert.deepEqual(await freezeRes.json(), {
      ok: true,
      status: "FROZEN",
      perTxnLimit: 20_000_000,
      dailyLimit: 50_000_000,
    });
    assert.equal(h.db.getRaw("accounts/acc_chk_1")?.status, "FROZEN");

    // 2. Unfreeze without PIN -> PIN_REQUIRED
    const unfreezeNoPin = await fetch(`${h.baseUrl}/security/accounts/acc_chk_1/controls`, {
      method: "PATCH",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ status: "ACTIVE" }),
    });
    assert.equal(unfreezeNoPin.status, 400);
    assert.equal(((await unfreezeNoPin.json()) as { error: string }).error, "PIN_REQUIRED");

    // 3. Unfreeze with valid PIN -> succeeds
    const unfreezeOk = await fetch(`${h.baseUrl}/security/accounts/acc_chk_1/controls`, {
      method: "PATCH",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ status: "ACTIVE", pin: "112233" }),
    });
    assert.equal(unfreezeOk.status, 200);
    assert.equal(h.db.getRaw("accounts/acc_chk_1")?.status, "ACTIVE");

    // 4. Lowering limits works without PIN
    const lowerRes = await fetch(`${h.baseUrl}/security/accounts/acc_chk_1/controls`, {
      method: "PATCH",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ perTxnLimit: 10_000_000, dailyLimit: 30_000_000 }),
    });
    assert.equal(lowerRes.status, 200);
    assert.deepEqual(await lowerRes.json(), {
      ok: true,
      status: "ACTIVE",
      perTxnLimit: 10_000_000,
      dailyLimit: 30_000_000,
    });

    // 5. Raising limits requires PIN
    const raiseNoPin = await fetch(`${h.baseUrl}/security/accounts/acc_chk_1/controls`, {
      method: "PATCH",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ perTxnLimit: 50_000_000, dailyLimit: 200_000_000 }),
    });
    assert.equal(raiseNoPin.status, 400);
    assert.equal(((await raiseNoPin.json()) as { error: string }).error, "PIN_REQUIRED");

    // 6. Reject out-of-bounds or perTxnLimit > dailyLimit
    const badRange = await fetch(`${h.baseUrl}/security/accounts/acc_chk_1/controls`, {
      method: "PATCH",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ perTxnLimit: 60_000_000, dailyLimit: 40_000_000, pin: "112233" }),
    });
    assert.equal(badRange.status, 400);
    assert.equal(((await badRange.json()) as { error: string }).error, "INVALID_ARGUMENT");

    // 7. Raise limits with valid PIN -> succeeds
    const raiseOk = await fetch(`${h.baseUrl}/security/accounts/acc_chk_1/controls`, {
      method: "PATCH",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ perTxnLimit: 50_000_000, dailyLimit: 200_000_000, pin: "112233" }),
    });
    assert.equal(raiseOk.status, 200);
    assert.equal(h.db.getRaw("accounts/acc_chk_1")?.perTxnLimit, 50_000_000);
    assert.equal(h.db.getRaw("accounts/acc_chk_1")?.dailyLimit, 200_000_000);
  });

  it("blocks TOCTOU PIN bypass when account is concurrently frozen or limits lowered before transaction", async () => {
    const h = await createFeaturesHarness();
    harnesses.push(h);

    await fetch(`${h.baseUrl}/security/pin/set`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ newPin: "112233" }),
    });
    const initialNotifsCount = h.db.listCollection("users/uid_cust_1/notifications").length;

    const origRunTx = h.db.runTransaction.bind(h.db);
    let injectConcurrentMutation: (() => void) | null = null;
    h.db.runTransaction = async <T>(fn: (tx: MockTransaction) => Promise<T>): Promise<T> => {
      if (injectConcurrentMutation) {
        const mutate = injectConcurrentMutation;
        injectConcurrentMutation = null;
        mutate();
      }
      return origRunTx(fn);
    };

    try {
      // 1. Pre-read sees ACTIVE + perTxnLimit 20M; request sends status ACTIVE + lower limit 10M without PIN.
      //    Concurrent freeze occurs right before runTransaction -> must fail with PIN_REQUIRED and keep FROZEN.
      injectConcurrentMutation = () => {
        h.db.writeDocUpdate("accounts/acc_chk_1", { status: "FROZEN" });
      };
      const raceUnfreezeRes = await fetch(`${h.baseUrl}/security/accounts/acc_chk_1/controls`, {
        method: "PATCH",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ status: "ACTIVE", perTxnLimit: 10_000_000 }),
      });
      assert.equal(raceUnfreezeRes.status, 400);
      assert.equal(((await raceUnfreezeRes.json()) as { error: string }).error, "PIN_REQUIRED");
      assert.equal(h.db.getRaw("accounts/acc_chk_1")?.status, "FROZEN");
      assert.equal(h.db.getRaw("accounts/acc_chk_1")?.perTxnLimit, 20_000_000);
      assert.equal(
        h.db.listCollection("users/uid_cust_1/notifications").length,
        initialNotifsCount
      );

      // 2. Restore ACTIVE, then simulate concurrent limit drop (20M -> 5M) before request setting perTxnLimit 10M without PIN.
      h.db.writeDocUpdate("accounts/acc_chk_1", { status: "ACTIVE", perTxnLimit: 20_000_000 });
      injectConcurrentMutation = () => {
        h.db.writeDocUpdate("accounts/acc_chk_1", { perTxnLimit: 5_000_000 });
      };
      const raceRaiseLimitRes = await fetch(`${h.baseUrl}/security/accounts/acc_chk_1/controls`, {
        method: "PATCH",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ perTxnLimit: 10_000_000 }),
      });
      assert.equal(raceRaiseLimitRes.status, 400);
      assert.equal(((await raceRaiseLimitRes.json()) as { error: string }).error, "PIN_REQUIRED");
      assert.equal(h.db.getRaw("accounts/acc_chk_1")?.perTxnLimit, 5_000_000);
      assert.equal(
        h.db.listCollection("users/uid_cust_1/notifications").length,
        initialNotifsCount
      );

      // 3. Freezing an ACTIVE account without PIN still succeeds normally
      const freezeRes = await fetch(`${h.baseUrl}/security/accounts/acc_chk_1/controls`, {
        method: "PATCH",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ status: "FROZEN" }),
      });
      assert.equal(freezeRes.status, 200);
      assert.equal(h.db.getRaw("accounts/acc_chk_1")?.status, "FROZEN");

      // 4. Unfreezing + raising limit with valid PIN succeeds
      const unfreezeWithPin = await fetch(`${h.baseUrl}/security/accounts/acc_chk_1/controls`, {
        method: "PATCH",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ status: "ACTIVE", perTxnLimit: 10_000_000, pin: "112233" }),
      });
      assert.equal(unfreezeWithPin.status, 200);
      assert.equal(h.db.getRaw("accounts/acc_chk_1")?.status, "ACTIVE");
      assert.equal(h.db.getRaw("accounts/acc_chk_1")?.perTxnLimit, 10_000_000);
    } finally {
      h.db.runTransaction = origRunTx;
    }
  });

  it("opens online SAVING account and settles both early and at maturity atomically with notifications", async () => {
    const h = await createFeaturesHarness();
    harnesses.push(h);

    await fetch(`${h.baseUrl}/security/pin/set`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ newPin: "556677" }),
    });

    // 1. Open rejects missing PIN when PIN is configured
    const openNoPin = await fetch(`${h.baseUrl}/savings/open`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        fromCheckingAccountId: "acc_chk_1",
        termMonths: 6,
        principalAmount: 10_000_000,
      }),
    });
    assert.equal(openNoPin.status, 400);
    assert.equal(((await openNoPin.json()) as { error: string }).error, "PIN_REQUIRED");

    // 2. Open rejects unsupported termMonths or < 1_000_000
    const openBadTerm = await fetch(`${h.baseUrl}/savings/open`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        fromCheckingAccountId: "acc_chk_1",
        termMonths: 9,
        principalAmount: 10_000_000,
        pin: "556677",
      }),
    });
    assert.equal(openBadTerm.status, 400);
    assert.equal(((await openBadTerm.json()) as { error: string }).error, "INVALID_ARGUMENT");

    // 3. Open 6-month SAVING account with 10_000_000 VND
    const openRes = await fetch(`${h.baseUrl}/savings/open`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        fromCheckingAccountId: "acc_chk_1",
        termMonths: 6,
        principalAmount: 10_000_000,
        pin: "556677",
      }),
    });
    assert.equal(openRes.status, 200);
    const openJson = (await openRes.json()) as {
      savingAccountId: string;
      accountNumber: string;
      txId: string;
      annualRate: number;
      maturityDateEpochMs: number;
    };
    assert.ok(openJson.savingAccountId);
    assert.match(openJson.accountNumber, /^20\d{8}$/);
    assert.equal(openJson.annualRate, 5.2);
    assert.equal(h.db.getRaw("accounts/acc_chk_1")?.balance, 40_000_000);
    const savingDoc = h.db.getRaw(`accounts/${openJson.savingAccountId}`);
    assert.equal(savingDoc?.type, "SAVING");
    assert.equal(savingDoc?.status, "ACTIVE");
    assert.equal(savingDoc?.balance, 10_000_000);
    assert.equal(h.db.listCollection("accounts/acc_chk_1/ledger").length, 1);
    assert.equal(h.db.listCollection(`accounts/${openJson.savingAccountId}/ledger`).length, 1);

    // 4. Early settle after 30 days -> demand rate 0.5%
    h.setNow(h.getNow() + 30 * 86_400_000);
    const settleEarlyRes = await fetch(`${h.baseUrl}/savings/${openJson.savingAccountId}/settle`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        toCheckingAccountId: "acc_chk_1",
        pin: "556677",
      }),
    });
    assert.equal(settleEarlyRes.status, 200);
    const settleEarlyJson = (await settleEarlyRes.json()) as {
      txId: string;
      principalAmount: number;
      interestAmount: number;
      totalPayoutAmount: number;
      earlySettlement: boolean;
      appliedAnnualRate: number;
    };
    assert.equal(settleEarlyJson.earlySettlement, true);
    assert.equal(settleEarlyJson.appliedAnnualRate, 0.5);
    assert.equal(settleEarlyJson.principalAmount, 10_000_000);
    assert.equal(settleEarlyJson.interestAmount, 4109);
    assert.equal(settleEarlyJson.totalPayoutAmount, 10_004_109);
    assert.equal(h.db.getRaw(`accounts/${openJson.savingAccountId}`)?.status, "CLOSED");
    assert.equal(h.db.getRaw(`accounts/${openJson.savingAccountId}`)?.balance, 0);
    assert.equal(h.db.getRaw("accounts/acc_chk_1")?.balance, 50_004_109);

    // 5. Settling closed account fails
    const settleAgain = await fetch(`${h.baseUrl}/savings/${openJson.savingAccountId}/settle`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        toCheckingAccountId: "acc_chk_1",
        pin: "556677",
      }),
    });
    assert.equal(settleAgain.status, 400);
    assert.equal(((await settleAgain.json()) as { error: string }).error, "ACCOUNT_NOT_ACTIVE");

    // 6. Seed matured SAVING account and settle at maturity (12m @ 6.0%)
    const openedMs = h.getNow() - 365 * 86_400_000;
    h.db.seed("accounts/acc_saving_matured", {
      ownerUid: "uid_cust_1",
      type: "SAVING",
      accountNumber: "2099000002",
      status: "ACTIVE",
      balance: 20_000_000,
      currency: "VND",
      saving: {
        termMonths: 12,
        principal: 20_000_000,
        interestRateId: "rate_12m",
        openedAt: admin.firestore.Timestamp.fromMillis(openedMs),
        maturityDate: admin.firestore.Timestamp.fromMillis(h.getNow() - 1000),
      },
    });
    const settleMaturedRes = await fetch(`${h.baseUrl}/savings/acc_saving_matured/settle`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        toCheckingAccountId: "acc_chk_1",
        pin: "556677",
      }),
    });
    assert.equal(settleMaturedRes.status, 200);
    const settleMaturedJson = (await settleMaturedRes.json()) as {
      earlySettlement: boolean;
      appliedAnnualRate: number;
      interestAmount: number;
      totalPayoutAmount: number;
    };
    assert.equal(settleMaturedJson.earlySettlement, false);
    assert.equal(settleMaturedJson.appliedAnnualRate, 6.0);
    assert.equal(settleMaturedJson.interestAmount, 1_200_000);
    assert.equal(settleMaturedJson.totalPayoutAmount, 21_200_000);

    // 7. Verify SAVING notifications created
    const notifs = h.db
      .listCollection("users/uid_cust_1/notifications")
      .filter((n) => n.data.category === "SAVING");
    assert.equal(notifs.length, 3); // 1 open + 2 settle
    for (const n of notifs) {
      assert.equal(n.data.read, false);
      assert.ok(!String(n.data.title).toLowerCase().includes("tdtu"));
      assert.ok(!String(n.data.body).toLowerCase().includes("sandbox"));
    }
  });

  it("emits 2-way BALANCE notifications on internal transfer, IN notification on /payments/finalize, and EKYC notification on /ekyc/review", async () => {
    const h = await createFeaturesHarness();
    harnesses.push(h);

    // 1. Internal transfer 1_500_000 from uid_cust_1 -> uid_cust_2
    const initTransfer = await fetch(`${h.baseUrl}/transactions/initiate`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        type: "TRANSFER_INTERNAL",
        fromAccountId: "acc_chk_1",
        toAccountId: "acc_chk_2",
        amount: 1_500_000,
        note: "Thanh toán tiền ăn",
      }),
    });
    assert.equal(initTransfer.status, 200);
    const initJson = (await initTransfer.json()) as { draftId: string };

    const confirmTransfer = await fetch(`${h.baseUrl}/transactions/confirm`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        draftId: initJson.draftId,
        otp: h.sentOtps[0],
        biometricPassed: false,
      }),
    });
    assert.equal(confirmTransfer.status, 200);
    const confirmJson = (await confirmTransfer.json()) as { txId: string };

    const senderNotifs = h.db.listCollection("users/uid_cust_1/notifications");
    assert.equal(senderNotifs.length, 1);
    assert.equal(senderNotifs[0].data.category, "BALANCE");
    assert.equal(senderNotifs[0].data.direction, "OUT");
    assert.equal(senderNotifs[0].data.amount, 1_500_000);
    assert.equal(senderNotifs[0].data.balanceAfter, 48_500_000);
    assert.equal(senderNotifs[0].data.txId, confirmJson.txId);
    assert.equal(senderNotifs[0].data.deepLink, `novapay://receipt/${confirmJson.txId}`);

    const recipientNotifs = h.db.listCollection("users/uid_cust_2/notifications");
    assert.equal(recipientNotifs.length, 1);
    assert.equal(recipientNotifs[0].data.category, "BALANCE");
    assert.equal(recipientNotifs[0].data.direction, "IN");
    assert.equal(recipientNotifs[0].data.amount, 1_500_000);
    assert.equal(recipientNotifs[0].data.balanceAfter, 6_500_000);
    assert.equal(recipientNotifs[0].data.txId, confirmJson.txId);
    assert.equal(recipientNotifs[0].data.deepLink, `novapay://receipt/${confirmJson.txId}`);

    // 2. Stripe GATEWAY finalize emits single IN notification (idempotent on retry)
    const initGw = await fetch(`${h.baseUrl}/transactions/initiate`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        type: "GATEWAY",
        fromAccountId: "acc_chk_1",
        amount: 4_000_000,
        note: "Nạp thẻ",
      }),
    });
    assert.equal(initGw.status, 200);
    const initGwJson = (await initGw.json()) as { draftId: string };

    await fetch(`${h.baseUrl}/transactions/confirm`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        draftId: initGwJson.draftId,
        otp: h.sentOtps[1],
        biometricPassed: false,
      }),
    });

    const fin1 = await fetch(`${h.baseUrl}/payments/finalize`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ draftId: initGwJson.draftId }),
    });
    assert.equal(fin1.status, 200);
    const fin2 = await fetch(`${h.baseUrl}/payments/finalize`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ draftId: initGwJson.draftId }),
    });
    assert.equal(fin2.status, 200);

    const cust1NotifsAfterGw = h.db.listCollection("users/uid_cust_1/notifications");
    assert.equal(cust1NotifsAfterGw.length, 2);
    const gwNotif = cust1NotifsAfterGw.find((n) => n.data.direction === "IN");
    assert.ok(gwNotif);
    assert.equal(gwNotif.data.category, "BALANCE");
    assert.equal(gwNotif.data.amount, 4_000_000);
    assert.equal(gwNotif.data.balanceAfter, 52_500_000);

    // 3. Officer eKYC review emits EKYC notification
    h.setActor("uid_officer_1", "OFFICER");
    const reviewRes = await fetch(`${h.baseUrl}/ekyc/review`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ uid: "uid_cust_2", decision: "VERIFIED" }),
    });
    assert.equal(reviewRes.status, 200);

    const cust2Notifs = h.db.listCollection("users/uid_cust_2/notifications");
    assert.equal(cust2Notifs.length, 2);
    const ekycNotif = cust2Notifs.find((n) => n.data.category === "EKYC");
    assert.ok(ekycNotif);
    assert.equal(ekycNotif.data.direction, null);
    assert.equal(ekycNotif.data.deepLink, "novapay://profile");
    assert.equal(ekycNotif.data.read, false);
  });

  it("blocks OFFICER from calling customer-only /security and /savings endpoints", async () => {
    const h = await createFeaturesHarness();
    harnesses.push(h);
    h.setActor("uid_officer_1", "OFFICER");

    const endpoints: Array<{ method: string; path: string; body?: unknown }> = [
      { method: "GET", path: "/security/status" },
      { method: "POST", path: "/security/pin/set", body: { newPin: "123456" } },
      {
        method: "PATCH",
        path: "/security/accounts/acc_chk_1/controls",
        body: { status: "FROZEN" },
      },
      {
        method: "POST",
        path: "/savings/open",
        body: {
          fromCheckingAccountId: "acc_chk_1",
          termMonths: 6,
          principalAmount: 5_000_000,
        },
      },
      {
        method: "POST",
        path: "/savings/acc_sav_any/settle",
        body: { toCheckingAccountId: "acc_chk_1" },
      },
    ];

    for (const ep of endpoints) {
      const res = await fetch(`${h.baseUrl}${ep.path}`, {
        method: ep.method,
        headers: ep.body ? { "content-type": "application/json" } : undefined,
        body: ep.body ? JSON.stringify(ep.body) : undefined,
      });
      assert.equal(res.status, 403, `Expected 403 on ${ep.method} ${ep.path}`);
      const json = (await res.json()) as { error: string };
      assert.equal(json.error, "PERMISSION_DENIED");
    }
  });
});
