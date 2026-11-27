import assert from "node:assert/strict";
import express, { NextFunction, Response } from "express";
import { after, before, describe, it } from "node:test";
import { AddressInfo } from "node:net";
import { AuthenticatedRequest } from "../src/middleware/auth";
import { createAccountsRouter } from "../src/routes/accounts";
import { MockFirestore } from "./helpers/mockFirestore";

describe("Accounts Route (GET /accounts/lookup)", () => {
  const db = new MockFirestore();
  let baseUrl = "";
  let server: ReturnType<express.Express["listen"]>;

  before(async () => {
    db.seed("users/u_active", {
      fullName: "Nguyễn Minh Khôi",
      email: "khoi@novapay.vn",
      phone: "0909123456",
      nationalId: "079200000123",
      status: "ACTIVE",
    });

    db.seed("accounts/acc_chk_active", {
      ownerUid: "u_active",
      type: "CHECKING",
      accountNumber: "10010001",
      status: "ACTIVE",
      balance: 99_000_000,
    });

    db.seed("accounts/acc_saving", {
      ownerUid: "u_active",
      type: "SAVING",
      accountNumber: "20010001",
      status: "ACTIVE",
      balance: 50_000_000,
    });

    db.seed("accounts/acc_frozen", {
      ownerUid: "u_active",
      type: "CHECKING",
      accountNumber: "10010002",
      status: "FROZEN",
      balance: 1_000_000,
    });

    const app = express();
    app.use(express.json());
    app.use(
      "/accounts",
      createAccountsRouter({
        getDb: () => db.asFirestore(),
        authMiddleware: (req: AuthenticatedRequest, _res: Response, next: NextFunction) => {
          req.user = { uid: "caller_uid", role: "CUSTOMER" };
          next();
        },
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

  it("returns minimal { accountId, accountNumber, ownerName } without PII or balance", async () => {
    const res = await fetch(`${baseUrl}/accounts/lookup?accountNumber=10010001`);
    assert.equal(res.status, 200);
    const json = (await res.json()) as Record<string, unknown>;
    assert.deepEqual(json, {
      accountId: "acc_chk_active",
      accountNumber: "10010001",
      ownerName: "Nguyễn Minh Khôi",
    });
    assert.equal("balance" in json, false);
    assert.equal("email" in json, false);
    assert.equal("phone" in json, false);
    assert.equal("nationalId" in json, false);
  });

  it("returns 400 INVALID_ARGUMENT when accountNumber query is missing", async () => {
    const res = await fetch(`${baseUrl}/accounts/lookup`);
    assert.equal(res.status, 400);
    const json = (await res.json()) as { error: string };
    assert.equal(json.error, "INVALID_ARGUMENT");
  });

  it("returns 404 ACCOUNT_NOT_FOUND when account does not exist", async () => {
    const res = await fetch(`${baseUrl}/accounts/lookup?accountNumber=99999999`);
    assert.equal(res.status, 404);
    const json = (await res.json()) as { error: string };
    assert.equal(json.error, "ACCOUNT_NOT_FOUND");
  });

  it("returns 400 INVALID_ARGUMENT for non-CHECKING account", async () => {
    const res = await fetch(`${baseUrl}/accounts/lookup?accountNumber=20010001`);
    assert.equal(res.status, 400);
    const json = (await res.json()) as { error: string };
    assert.equal(json.error, "INVALID_ARGUMENT");
  });

  it("returns 400 ACCOUNT_NOT_ACTIVE for FROZEN CHECKING account", async () => {
    const res = await fetch(`${baseUrl}/accounts/lookup?accountNumber=10010002`);
    assert.equal(res.status, 400);
    const json = (await res.json()) as { error: string };
    assert.equal(json.error, "ACCOUNT_NOT_ACTIVE");
  });

  it("rejects malformed accountNumber query and CHECKING account owned by a LOCKED user", async () => {
    const badFormat = await fetch(
      `${baseUrl}/accounts/lookup?accountNumber=${encodeURIComponent("1001$DROP")}`
    );
    assert.equal(badFormat.status, 400);
    assert.equal(((await badFormat.json()) as { error: string }).error, "INVALID_ARGUMENT");

    db.seed("users/u_locked", {
      fullName: "Tài Khoản Khóa",
      status: "LOCKED",
    });
    db.seed("accounts/acc_chk_locked_owner", {
      ownerUid: "u_locked",
      type: "CHECKING",
      accountNumber: "10010099",
      status: "ACTIVE",
      balance: 500_000,
    });

    const lockedOwnerRes = await fetch(`${baseUrl}/accounts/lookup?accountNumber=10010099`);
    assert.equal(lockedOwnerRes.status, 400);
    assert.equal(((await lockedOwnerRes.json()) as { error: string }).error, "ACCOUNT_NOT_ACTIVE");

    // Rejects account lookup when the caller themselves is LOCKED in Firestore
    db.seed("users/caller_uid", {
      fullName: "Caller Locked",
      status: "LOCKED",
    });
    const lockedCallerRes = await fetch(`${baseUrl}/accounts/lookup?accountNumber=10010001`);
    assert.equal(lockedCallerRes.status, 403);
    assert.equal(((await lockedCallerRes.json()) as { error: string }).error, "PERMISSION_DENIED");
    db.seed("users/caller_uid", {
      fullName: "Caller Active",
      status: "ACTIVE",
    });
  });
});
