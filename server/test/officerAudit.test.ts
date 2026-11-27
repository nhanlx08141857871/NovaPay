import test from "node:test";
import assert from "node:assert/strict";
import admin from "firebase-admin";
import {
  OfficerAuthAdapter,
  OfficerServiceError,
  buildAuditLogPayload,
  createCustomerWithAudit,
  generateAccountNumber,
  mapFirebaseAuthCreateError,
  updateCustomerWithAudit,
  upsertInterestRateWithAudit,
  validateCreateCustomerPayload,
  validateUpdateCustomerPayload,
  validateUpsertInterestRatePayload,
} from "../src/services/officerAudit";

test("validateCreateCustomerPayload accepts valid Vietnamese customer input", () => {
  const result = validateCreateCustomerPayload({
    fullName: "  Nguyễn Văn An ",
    email: " An.Nguyen@Example.com ",
    phone: "0901 234 567",
    nationalId: "079099001234",
    dob: "1999-05-20",
    address: "19 Nguyễn Hữu Thọ, Quận 7, TP.HCM",
    initialPassword: "secret123",
  });

  assert.equal(result.ok, true);
  if (result.ok) {
    assert.equal(result.value.fullName, "Nguyễn Văn An");
    assert.equal(result.value.email, "an.nguyen@example.com");
    assert.equal(result.value.phone, "0901234567");
    assert.equal(result.value.nationalId, "079099001234");
    assert.equal(result.value.dob, "1999-05-20");
  }
});

test("validateCreateCustomerPayload rejects invalid fields", () => {
  const base = {
    fullName: "Nguyễn Văn An",
    email: "an@novapay.vn",
    phone: "0901234567",
    nationalId: "079099001234",
    dob: "1999-05-20",
    address: "Quận 7, TP.HCM",
    initialPassword: "secret123",
  };

  assert.equal(validateCreateCustomerPayload({ ...base, fullName: "A" }).ok, false);
  assert.equal(validateCreateCustomerPayload({ ...base, fullName: "A".repeat(121) }).ok, false);
  assert.equal(validateCreateCustomerPayload({ ...base, email: "not-an-email" }).ok, false);
  assert.equal(validateCreateCustomerPayload({ ...base, phone: "12345" }).ok, false);
  assert.equal(validateCreateCustomerPayload({ ...base, nationalId: "12345" }).ok, false);
  assert.equal(validateCreateCustomerPayload({ ...base, dob: "2026-02-30" }).ok, false);
  assert.equal(validateCreateCustomerPayload({ ...base, dob: "2999-01-01" }).ok, false);
  assert.equal(validateCreateCustomerPayload({ ...base, address: "   " }).ok, false);
  assert.equal(validateCreateCustomerPayload({ ...base, address: "x".repeat(501) }).ok, false);
  assert.equal(validateCreateCustomerPayload({ ...base, initialPassword: "123" }).ok, false);
  assert.equal(validateCreateCustomerPayload({ ...base, initialPassword: "p".repeat(129) }).ok, false);
});

test("validateUpdateCustomerPayload validates partial updates and status values", () => {
  assert.equal(validateUpdateCustomerPayload("", { fullName: "Test" }).ok, false);
  assert.equal(validateUpdateCustomerPayload("u".repeat(129), { fullName: "Test" }).ok, false);
  assert.equal(validateUpdateCustomerPayload("uid1", {}).ok, false);
  assert.equal(validateUpdateCustomerPayload("uid1", { fullName: "A".repeat(121) }).ok, false);
  assert.equal(validateUpdateCustomerPayload("uid1", { address: "x".repeat(501) }).ok, false);
  assert.equal(
    validateUpdateCustomerPayload("uid1", { status: "DELETED" }).ok,
    false
  );
  assert.equal(validateUpdateCustomerPayload("uid1", { phone: "09" }).ok, false);

  const valid = validateUpdateCustomerPayload("uid1", {
    fullName: "Trần Bình",
    status: "LOCKED",
  });
  assert.equal(valid.ok, true);
  if (valid.ok) {
    assert.equal(valid.value.uid, "uid1");
    assert.equal(valid.value.fullName, "Trần Bình");
    assert.equal(valid.value.status, "LOCKED");
    assert.equal(valid.value.phone, undefined);
  }
});

test("validateUpsertInterestRatePayload enforces integer termMonths >= 0 and finite rate > 0", () => {
  assert.equal(validateUpsertInterestRatePayload({ termMonths: 0, annualRate: 0.5 }).ok, true);
  assert.equal(validateUpsertInterestRatePayload({ termMonths: 12, annualRate: 6.5 }).ok, true);
  assert.equal(validateUpsertInterestRatePayload({ termMonths: -1, annualRate: 6.5 }).ok, false);
  assert.equal(validateUpsertInterestRatePayload({ termMonths: 1.5, annualRate: 6.5 }).ok, false);
  assert.equal(validateUpsertInterestRatePayload({ termMonths: 6, annualRate: 0 }).ok, false);
  assert.equal(validateUpsertInterestRatePayload({ termMonths: 6, annualRate: Number.NaN }).ok, false);
  assert.equal(validateUpsertInterestRatePayload({ termMonths: 6, annualRate: 80 }).ok, false);
});

test("generateAccountNumber returns 10-digit numeric string prefixed with 10", () => {
  for (let i = 0; i < 20; i += 1) {
    const acc = generateAccountNumber();
    assert.match(acc, /^10\d{8}$/);
  }
});

test("mapFirebaseAuthCreateError maps duplicate email to 409 EMAIL_ALREADY_EXISTS", () => {
  const err = mapFirebaseAuthCreateError({ code: "auth/email-already-exists" });
  assert.ok(err instanceof OfficerServiceError);
  assert.equal(err.statusCode, 409);
  assert.equal(err.errorCode, "EMAIL_ALREADY_EXISTS");

  const weakErr = mapFirebaseAuthCreateError({ code: "auth/weak-password" });
  assert.ok(weakErr instanceof OfficerServiceError);
  assert.equal(weakErr.statusCode, 400);
  assert.equal(weakErr.errorCode, "INVALID_ARGUMENT");
});

test("createCustomerWithAudit creates Auth user, claims, user/checking/audit docs and rolls back on failure", async () => {
  const writes: Array<{ path: string; data: Record<string, unknown> }> = [];
  const deletedUids: string[] = [];
  const claimed: Array<{ uid: string; claims: Record<string, unknown> }> = [];
  let autoIdCounter = 0;

  const mockAuth: OfficerAuthAdapter = {
    async createUser() {
      return { uid: "cust_uid_1" };
    },
    async setCustomUserClaims(uid, claims) {
      claimed.push({ uid, claims });
    },
    async updateUser() {},
    async revokeRefreshTokens() {},
    async deleteUser(uid) {
      deletedUids.push(uid);
    },
  };

  const makeDocRef = (col: string, id?: string) => {
    const docId = id ?? `${col}_auto_${++autoIdCounter}`;
    return { id: docId, path: `${col}/${docId}` };
  };

  const mockDb = {
    collection(col: string) {
      return {
        doc(id?: string) {
          return makeDocRef(col, id);
        },
        where() {
          return {
            limit() {
              return { kind: "accountLookup" };
            },
          };
        },
      };
    },
    async runTransaction<T>(fn: (tx: unknown) => Promise<T>): Promise<T> {
      const tx = {
        async get() {
          return { empty: true, docs: [] };
        },
        set(ref: { path: string }, data: Record<string, unknown>) {
          writes.push({ path: ref.path, data });
        },
      };
      return fn(tx);
    },
  } as unknown as FirebaseFirestore.Firestore;

  const fixedTs = admin.firestore.Timestamp.fromMillis(1_700_000_000_000);
  const res = await createCustomerWithAudit(
    "officer_1",
    {
      fullName: "Nguyễn Văn An",
      email: "an@novapay.vn",
      phone: "0901234567",
      nationalId: "079099001234",
      dob: "1999-05-20",
      address: "Quận 7",
      initialPassword: "secret123",
    },
    {
      auth: mockAuth,
      db: mockDb,
      now: () => fixedTs,
      accountNumberGenerator: () => "1012345678",
    }
  );

  assert.equal(res.uid, "cust_uid_1");
  assert.deepEqual(claimed, [{ uid: "cust_uid_1", claims: { role: "CUSTOMER" } }]);
  assert.equal(deletedUids.length, 0);
  assert.equal(writes.length, 3);
  assert.equal(writes[0].path, "users/cust_uid_1");
  assert.equal((writes[0].data.ekyc as { status: string }).status, "NONE");
  assert.equal(writes[1].data.type, "CHECKING");
  assert.equal(writes[1].data.balance, 0);
  assert.equal(writes[1].data.accountNumber, "1012345678");
  assert.equal(writes[2].data.action, "CREATE_CUSTOMER");
  assert.equal(writes[2].data.actorUid, "officer_1");

  // Rollback verification when Firestore transaction throws
  const failingDb = {
    collection(col: string) {
      return {
        doc(id?: string) {
          return makeDocRef(col, id);
        },
        where() {
          return { limit: () => ({}) };
        },
      };
    },
    async runTransaction(): Promise<never> {
      throw new Error("FIRESTORE_DOWN");
    },
  } as unknown as FirebaseFirestore.Firestore;

  await assert.rejects(
    () =>
      createCustomerWithAudit(
        "officer_1",
        {
          fullName: "Nguyễn Văn An",
          email: "an@novapay.vn",
          phone: "0901234567",
          nationalId: "079099001234",
          dob: "1999-05-20",
          address: "Quận 7",
          initialPassword: "secret123",
        },
        { auth: mockAuth, db: failingDb }
      ),
    (err: unknown) => err instanceof OfficerServiceError && err.statusCode === 500
  );
  assert.deepEqual(deletedUids, ["cust_uid_1"]);
});

test("updateCustomerWithAudit writes audit log and syncs Auth disabled + revokeRefreshTokens on LOCKED", async () => {
  const updates: Array<{ path: string; data: Record<string, unknown> }> = [];
  const auditWrites: Array<Record<string, unknown>> = [];
  const authCalls: string[] = [];

  const mockAuth: OfficerAuthAdapter = {
    async createUser() {
      return { uid: "unused" };
    },
    async setCustomUserClaims() {},
    async updateUser(uid, props) {
      authCalls.push(`updateUser:${uid}:disabled=${props.disabled}`);
    },
    async revokeRefreshTokens(uid) {
      authCalls.push(`revoke:${uid}`);
    },
    async deleteUser() {},
  };

  const mockDb = {
    collection(col: string) {
      return {
        doc(id?: string) {
          return { path: `${col}/${id ?? "auto"}` };
        },
      };
    },
    async runTransaction<T>(fn: (tx: unknown) => Promise<T>): Promise<T> {
      const tx = {
        async get() {
          return {
            exists: true,
            data: () => ({ status: "ACTIVE" }),
          };
        },
        update(ref: { path: string }, data: Record<string, unknown>) {
          updates.push({ path: ref.path, data });
        },
        set(_ref: { path: string }, data: Record<string, unknown>) {
          auditWrites.push(data);
        },
      };
      return fn(tx);
    },
  } as unknown as FirebaseFirestore.Firestore;

  const res = await updateCustomerWithAudit(
    "officer_1",
    { uid: "cust_1", status: "LOCKED" },
    { auth: mockAuth, db: mockDb }
  );

  assert.deepEqual(res, { ok: true });
  assert.equal(updates[0].path, "users/cust_1");
  assert.equal(updates[0].data.status, "LOCKED");
  assert.equal(auditWrites[0].action, "LOCK_CUSTOMER");
  assert.deepEqual(authCalls, ["updateUser:cust_1:disabled=true", "revoke:cust_1"]);

  // Rejects cross-officer lock/edit via customer management endpoint
  const officerTargetDb = {
    collection(col: string) {
      return {
        doc(id?: string) {
          return { path: `${col}/${id ?? "auto"}` };
        },
      };
    },
    async runTransaction<T>(fn: (tx: unknown) => Promise<T>): Promise<T> {
      const tx = {
        async get() {
          return {
            exists: true,
            data: () => ({ role: "OFFICER", status: "ACTIVE" }),
          };
        },
        update() {},
        set() {},
      };
      return fn(tx);
    },
  } as unknown as FirebaseFirestore.Firestore;

  await assert.rejects(
    () =>
      updateCustomerWithAudit(
        "officer_1",
        { uid: "officer_2", status: "LOCKED" },
        { auth: mockAuth, db: officerTargetDb }
      ),
    (err: unknown) =>
      err instanceof OfficerServiceError &&
      err.statusCode === 403 &&
      err.errorCode === "PERMISSION_DENIED"
  );
});

test("upsertInterestRateWithAudit updates existing rate doc and records audit log", async () => {
  const sets: Array<{ path: string; data: Record<string, unknown> }> = [];
  const fixedTs = admin.firestore.Timestamp.fromMillis(1_700_000_000_000);

  const mockDb = {
    collection(col: string) {
      return {
        doc(id?: string) {
          return { id: id ?? "auto_audit", path: `${col}/${id ?? "auto_audit"}` };
        },
        where() {
          return {
            limit: () => ({ kind: "rateLookup" }),
          };
        },
      };
    },
    async runTransaction<T>(fn: (tx: unknown) => Promise<T>): Promise<T> {
      const tx = {
        async get() {
          return {
            empty: false,
            docs: [{ id: "rate_6m", data: () => ({ termMonths: 6, annualRate: 5.5 }) }],
          };
        },
        set(ref: { path: string }, data: Record<string, unknown>) {
          sets.push({ path: ref.path, data });
        },
      };
      return fn(tx);
    },
  } as unknown as FirebaseFirestore.Firestore;

  const result = await upsertInterestRateWithAudit(
    "officer_1",
    { termMonths: 6, annualRate: 6.2 },
    { db: mockDb, now: () => fixedTs }
  );

  assert.equal(result.rateId, "rate_6m");
  assert.equal(sets.length, 2);
  assert.equal(sets[0].path, "interest_rates/rate_6m");
  assert.equal(sets[0].data.annualRate, 6.2);
  assert.equal(sets[1].data.action, "UPSERT_INTEREST_RATE");
  assert.deepEqual(
    buildAuditLogPayload("o1", "A", "t1", { k: 1 }, fixedTs).actorUid,
    "o1"
  );
});
