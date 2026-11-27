import assert from "node:assert/strict";
import express, { NextFunction, Response } from "express";
import { AddressInfo } from "node:net";
import { after, before, describe, it } from "node:test";
import { AuthenticatedRequest } from "../src/middleware/auth";
import { createPaymentsRouter } from "../src/routes/payments";
import { createTransactionsRouter } from "../src/routes/transactions";
import { EmailService } from "../src/services/email";
import { otpService } from "../src/services/otp";
import { StripeService } from "../src/services/stripe";
import { MockFirestore } from "./helpers/mockFirestore";

describe("Stripe GATEWAY 2-Phase Flow & Idempotent /payments/finalize", () => {
  const db = new MockFirestore();
  const sentOtps: string[] = [];
  let paymentIntentStatus = "succeeded";
  let paymentIntentAmount = 10_000_000;
  let baseUrl = "";
  let server: ReturnType<express.Express["listen"]>;

  before(async () => {
    db.seed("users/uid_gw", {
      fullName: "Phạm Quốc Bảo",
      email: "bao@novapay.vn",
      status: "ACTIVE",
      ekyc: { status: "PENDING" }, // GATEWAY exempt from eKYC if not debiting, and >5M exempt from FaceCheck!
    });
    db.seed("accounts/acc_chk_gw", {
      ownerUid: "uid_gw",
      type: "CHECKING",
      accountNumber: "10009999",
      status: "ACTIVE",
      balance: 1_000_000,
      perTxnLimit: 5_000_000,
      dailyLimit: 5_000_000,
    });

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
          status: paymentIntentStatus,
          amount: paymentIntentAmount,
          currency: "vnd",
        };
      },
    };

    const authMiddleware = (req: AuthenticatedRequest, _res: Response, next: NextFunction) => {
      req.user = { uid: "uid_gw", email: "bao@novapay.vn", role: "CUSTOMER" };
      next();
    };

    const app = express();
    app.use(express.json());
    app.use(
      "/transactions",
      createTransactionsRouter({
        getDb: () => db.asFirestore(),
        authMiddleware,
        otpService,
        emailService,
        stripeService,
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

    await new Promise<void>((resolve) => {
      server = app.listen(0, "127.0.0.1", () => {
        const addr = server.address() as AddressInfo;
        baseUrl = `http://127.0.0.1:${addr.port}`;
        resolve();
      });
    });
  });

  after(async () => {
    await new Promise<void>((resolve, reject) => {
      server.close((err) => (err ? reject(err) : resolve()));
    });
  });

  it("exempts GATEWAY > 5M from FaceCheck/Biometric, creates PENDING_PAYMENT without crediting, and finalizes idempotently", async () => {
    // 1. Initiate 10M GATEWAY deposit
    const initRes = await fetch(`${baseUrl}/transactions/initiate`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        type: "GATEWAY",
        fromAccountId: "acc_chk_gw",
        amount: 10_000_000,
        note: "Nạp tiền Stripe",
      }),
    });
    assert.equal(initRes.status, 200);
    const initJson = (await initRes.json()) as {
      draftId: string;
      requiresBiometric: boolean;
      requiresFace: boolean;
      otpExpiresAt: number;
    };
    assert.equal(initJson.requiresFace, false);
    assert.equal(initJson.requiresBiometric, false);
    assert.equal(typeof initJson.otpExpiresAt, "number");
    assert.equal(sentOtps.length, 1);

    // 2. Confirm OTP -> PENDING_PAYMENT with clientSecret, balance STILL 1_000_000!
    const confirmRes = await fetch(`${baseUrl}/transactions/confirm`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        draftId: initJson.draftId,
        otp: sentOtps[0],
        biometricPassed: false,
      }),
    });
    assert.equal(confirmRes.status, 200);
    const confirmJson = (await confirmRes.json()) as {
      txId: string | null;
      status: string;
      clientSecret: string;
    };
    assert.equal(confirmJson.txId, null);
    assert.equal(confirmJson.status, "PENDING_PAYMENT");
    assert.equal(confirmJson.clientSecret, `cs_${initJson.draftId}`);
    assert.equal(db.getRaw("accounts/acc_chk_gw")?.balance, 1_000_000);

    // 3. Reject finalize when Stripe PaymentIntent is not succeeded
    paymentIntentStatus = "requires_payment_method";
    const notReadyRes = await fetch(`${baseUrl}/payments/finalize`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ draftId: initJson.draftId }),
    });
    assert.equal(notReadyRes.status, 400);
    assert.equal(((await notReadyRes.json()) as { error: string }).error, "PAYMENT_NOT_COMPLETED");
    assert.equal(db.getRaw("accounts/acc_chk_gw")?.balance, 1_000_000);

    // 3b. Reject finalize when Stripe amount differs from the draft amount
    paymentIntentStatus = "succeeded";
    paymentIntentAmount = 1_000;
    const mismatchRes = await fetch(`${baseUrl}/payments/finalize`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ draftId: initJson.draftId }),
    });
    assert.equal(mismatchRes.status, 400);
    assert.equal(((await mismatchRes.json()) as { error: string }).error, "PAYMENT_NOT_COMPLETED");
    assert.equal(db.getRaw("accounts/acc_chk_gw")?.balance, 1_000_000);
    paymentIntentAmount = 10_000_000;

    // 3c. Reject finalize when user account is LOCKED inside ACID transaction
    db.seed("users/uid_gw", {
      fullName: "Phạm Quốc Bảo",
      email: "bao@novapay.vn",
      status: "LOCKED",
      ekyc: { status: "PENDING" },
    });
    const lockedUserRes = await fetch(`${baseUrl}/payments/finalize`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ draftId: initJson.draftId }),
    });
    assert.equal(lockedUserRes.status, 403);
    assert.equal(((await lockedUserRes.json()) as { error: string }).error, "PERMISSION_DENIED");
    assert.equal(db.getRaw("accounts/acc_chk_gw")?.balance, 1_000_000);
    db.seed("users/uid_gw", {
      fullName: "Phạm Quốc Bảo",
      email: "bao@novapay.vn",
      status: "ACTIVE",
      ekyc: { status: "PENDING" },
    });

    // 4. Finalize once succeeded -> credits 10M once
    paymentIntentStatus = "succeeded";
    const fin1Res = await fetch(`${baseUrl}/payments/finalize`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ draftId: initJson.draftId }),
    });
    assert.equal(fin1Res.status, 200);
    const fin1Json = (await fin1Res.json()) as { txId: string; status: string };
    assert.equal(fin1Json.status, "CONFIRMED");
    assert.ok(fin1Json.txId);
    assert.equal(db.getRaw("accounts/acc_chk_gw")?.balance, 11_000_000);
    assert.equal(db.listCollection("accounts/acc_chk_gw/ledger").length, 1);
    assert.equal(db.listCollection("accounts/acc_chk_gw/ledger")[0].data.direction, "IN");
    const gwTx = db.getRaw(`transactions/${fin1Json.txId}`);
    assert.equal(gwTx?.fromAccountNumber, "10009999");
    assert.equal(gwTx?.toAccountNumber, "10009999");
    assert.equal(gwTx?.toOwnerName, "Phạm Quốc Bảo");

    // 5. Retry finalize -> idempotent, identical txId, NO double-crediting!
    const fin2Res = await fetch(`${baseUrl}/payments/finalize`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ draftId: initJson.draftId }),
    });
    assert.equal(fin2Res.status, 200);
    const fin2Json = (await fin2Res.json()) as { txId: string; status: string };
    assert.equal(fin2Json.txId, fin1Json.txId);
    assert.equal(fin2Json.status, "CONFIRMED");
    assert.equal(db.getRaw("accounts/acc_chk_gw")?.balance, 11_000_000);
    assert.equal(db.listCollection("transactions").length, 1);
  });
});
