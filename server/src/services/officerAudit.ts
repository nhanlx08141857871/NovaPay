import crypto from "node:crypto";
import admin from "firebase-admin";

export interface ValidationSuccess<T> {
  ok: true;
  value: T;
}

export interface ValidationFailure {
  ok: false;
  message: string;
}

export type ValidationResult<T> = ValidationSuccess<T> | ValidationFailure;

export interface CreateCustomerInput {
  fullName: string;
  email: string;
  phone: string;
  nationalId: string;
  dob: string;
  address: string;
  initialPassword: string;
}

export interface UpdateCustomerInput {
  uid: string;
  fullName?: string;
  phone?: string;
  address?: string;
  status?: "ACTIVE" | "LOCKED";
}

export interface UpsertInterestRateInput {
  termMonths: number;
  annualRate: number;
}

export class OfficerServiceError extends Error {
  public readonly statusCode: number;
  public readonly errorCode: string;

  constructor(statusCode: number, errorCode: string, message: string) {
    super(message);
    this.name = "OfficerServiceError";
    this.statusCode = statusCode;
    this.errorCode = errorCode;
  }
}

export interface OfficerAuthAdapter {
  createUser(props: {
    email: string;
    password: string;
    displayName: string;
    disabled: boolean;
  }): Promise<{ uid: string }>;
  setCustomUserClaims(uid: string, claims: Record<string, unknown>): Promise<void>;
  updateUser(uid: string, props: { disabled: boolean }): Promise<unknown>;
  revokeRefreshTokens(uid: string): Promise<void>;
  deleteUser(uid: string): Promise<void>;
}

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_REGEX = /^(0|\+84)[0-9]{9,10}$/;
const NATIONAL_ID_REGEX = /^[0-9]{9}([0-9]{3})?$/;
const DOB_ISO_REGEX = /^(\d{4})-(\d{2})-(\d{2})$/;

const DEFAULT_CHECKING_PER_TXN_LIMIT = 50_000_000;
const DEFAULT_CHECKING_DAILY_LIMIT = 200_000_000;
const MAX_ACCOUNT_NUMBER_ATTEMPTS = 5;

export function normalizePhone(rawPhone: string): string {
  return rawPhone.trim().replace(/[\s.-]/g, "");
}

export function isValidIsoDob(rawDob: string): boolean {
  const match = DOB_ISO_REGEX.exec(rawDob.trim());
  if (!match) {
    return false;
  }
  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  if (year < 1900 || month < 1 || month > 12 || day < 1 || day > 31) {
    return false;
  }
  const parsed = new Date(Date.UTC(year, month - 1, day));
  if (
    parsed.getUTCFullYear() !== year ||
    parsed.getUTCMonth() !== month - 1 ||
    parsed.getUTCDate() !== day
  ) {
    return false;
  }
  const todayUtc = new Date();
  const todayMidnightUtc = Date.UTC(
    todayUtc.getUTCFullYear(),
    todayUtc.getUTCMonth(),
    todayUtc.getUTCDate()
  );
  return parsed.getTime() <= todayMidnightUtc;
}

export function validateCreateCustomerPayload(
  body: unknown
): ValidationResult<CreateCustomerInput> {
  if (!body || typeof body !== "object") {
    return { ok: false, message: "Dữ liệu tạo khách hàng không hợp lệ." };
  }
  const raw = body as Record<string, unknown>;

  if (
    typeof raw.fullName !== "string" ||
    typeof raw.email !== "string" ||
    typeof raw.phone !== "string" ||
    typeof raw.nationalId !== "string" ||
    typeof raw.dob !== "string" ||
    typeof raw.address !== "string" ||
    typeof raw.initialPassword !== "string"
  ) {
    return {
      ok: false,
      message: "Thiếu thông tin bắt buộc để tạo hồ sơ khách hàng.",
    };
  }

  const fullName = raw.fullName.trim();
  const email = raw.email.trim().toLowerCase();
  const phone = normalizePhone(raw.phone);
  const nationalId = raw.nationalId.trim();
  const dob = raw.dob.trim();
  const address = raw.address.trim();
  const initialPassword = raw.initialPassword;

  if (fullName.length < 2 || fullName.length > 120) {
    return { ok: false, message: "Họ và tên phải từ 2 đến 120 ký tự." };
  }
  if (email.length > 254 || !EMAIL_REGEX.test(email)) {
    return { ok: false, message: "Địa chỉ email không đúng định dạng." };
  }
  if (!PHONE_REGEX.test(phone)) {
    return {
      ok: false,
      message: "Số điện thoại phải gồm 10–11 số theo chuẩn Việt Nam (0... hoặc +84...).",
    };
  }
  if (!NATIONAL_ID_REGEX.test(nationalId)) {
    return { ok: false, message: "Số CMND/CCCD phải gồm 9 hoặc 12 chữ số." };
  }
  if (!isValidIsoDob(dob)) {
    return {
      ok: false,
      message: "Ngày sinh không hợp lệ (định dạng YYYY-MM-DD, không vượt quá ngày hiện tại).",
    };
  }
  if (address.length === 0 || address.length > 500) {
    return { ok: false, message: "Địa chỉ liên hệ phải từ 1 đến 500 ký tự." };
  }
  if (initialPassword.length < 6 || initialPassword.length > 128) {
    return { ok: false, message: "Mật khẩu khởi tạo phải từ 6 đến 128 ký tự." };
  }

  return {
    ok: true,
    value: {
      fullName,
      email,
      phone,
      nationalId,
      dob,
      address,
      initialPassword,
    },
  };
}

export function validateUpdateCustomerPayload(
  uid: unknown,
  body: unknown
): ValidationResult<UpdateCustomerInput> {
  if (typeof uid !== "string" || uid.trim().length === 0 || uid.trim().length > 128) {
    return { ok: false, message: "Thiếu hoặc sai định dạng mã định danh khách hàng (uid)." };
  }
  if (!body || typeof body !== "object") {
    return { ok: false, message: "Dữ liệu cập nhật khách hàng không hợp lệ." };
  }

  const raw = body as Record<string, unknown>;
  const hasAnyField =
    raw.fullName !== undefined ||
    raw.phone !== undefined ||
    raw.address !== undefined ||
    raw.status !== undefined;

  if (!hasAnyField) {
    return {
      ok: false,
      message: "Cần cung cấp ít nhất một trường cần cập nhật (fullName, phone, address hoặc status).",
    };
  }

  const parsed: UpdateCustomerInput = { uid: uid.trim() };

  if (raw.fullName !== undefined) {
    if (
      typeof raw.fullName !== "string" ||
      raw.fullName.trim().length < 2 ||
      raw.fullName.trim().length > 120
    ) {
      return { ok: false, message: "Họ và tên phải từ 2 đến 120 ký tự." };
    }
    parsed.fullName = raw.fullName.trim();
  }

  if (raw.phone !== undefined) {
    if (typeof raw.phone !== "string") {
      return { ok: false, message: "Số điện thoại không hợp lệ." };
    }
    const normalized = normalizePhone(raw.phone);
    if (!PHONE_REGEX.test(normalized)) {
      return {
        ok: false,
        message: "Số điện thoại phải gồm 10–11 số theo chuẩn Việt Nam (0... hoặc +84...).",
      };
    }
    parsed.phone = normalized;
  }

  if (raw.address !== undefined) {
    if (
      typeof raw.address !== "string" ||
      raw.address.trim().length === 0 ||
      raw.address.trim().length > 500
    ) {
      return { ok: false, message: "Địa chỉ liên hệ phải từ 1 đến 500 ký tự." };
    }
    parsed.address = raw.address.trim();
  }

  if (raw.status !== undefined) {
    if (raw.status !== "ACTIVE" && raw.status !== "LOCKED") {
      return {
        ok: false,
        message: "Trạng thái tài khoản chỉ chấp nhận ACTIVE hoặc LOCKED.",
      };
    }
    parsed.status = raw.status;
  }

  return { ok: true, value: parsed };
}

export function validateUpsertInterestRatePayload(
  body: unknown
): ValidationResult<UpsertInterestRateInput> {
  if (!body || typeof body !== "object") {
    return { ok: false, message: "Dữ liệu lãi suất không hợp lệ." };
  }
  const raw = body as Record<string, unknown>;
  const termMonths = raw.termMonths;
  const annualRate = raw.annualRate;

  if (
    typeof termMonths !== "number" ||
    !Number.isInteger(termMonths) ||
    termMonths < 0 ||
    termMonths > 120
  ) {
    return {
      ok: false,
      message: "Kỳ hạn (termMonths) phải là số nguyên từ 0 đến 120 tháng (0 = không kỳ hạn).",
    };
  }

  if (
    typeof annualRate !== "number" ||
    !Number.isFinite(annualRate) ||
    annualRate <= 0 ||
    annualRate > 50
  ) {
    return {
      ok: false,
      message: "Lãi suất năm (annualRate) phải là số hợp lệ trong khoảng (0, 50].",
    };
  }

  return {
    ok: true,
    value: {
      termMonths,
      annualRate,
    },
  };
}

export function generateAccountNumber(): string {
  const randomDigits = crypto.randomInt(0, 100_000_000).toString().padStart(8, "0");
  return `10${randomDigits}`;
}

export function buildAuditLogPayload(
  actorUid: string,
  action: string,
  targetId: string,
  details: Record<string, unknown>,
  createdAt: admin.firestore.Timestamp
): Record<string, unknown> {
  return {
    actorUid,
    action,
    targetId,
    details,
    createdAt,
  };
}

export function mapFirebaseAuthCreateError(err: unknown): OfficerServiceError | null {
  if (!err || typeof err !== "object") {
    return null;
  }
  const code = (err as { code?: unknown }).code;
  if (typeof code !== "string") {
    return null;
  }
  if (code === "auth/email-already-exists" || code === "auth/email-already-in-use") {
    return new OfficerServiceError(
      409,
      "EMAIL_ALREADY_EXISTS",
      "Email này đã được đăng ký cho một tài khoản khác."
    );
  }
  if (
    code === "auth/invalid-email" ||
    code === "auth/invalid-password" ||
    code === "auth/weak-password"
  ) {
    return new OfficerServiceError(
      400,
      "INVALID_ARGUMENT",
      "Email hoặc mật khẩu không đáp ứng yêu cầu của hệ thống xác thực."
    );
  }
  return null;
}

function getDefaultAuthAdapter(): OfficerAuthAdapter {
  const auth = admin.auth();
  return {
    createUser: (props) => auth.createUser(props),
    setCustomUserClaims: (uid, claims) => auth.setCustomUserClaims(uid, claims),
    updateUser: (uid, props) => auth.updateUser(uid, props),
    revokeRefreshTokens: (uid) => auth.revokeRefreshTokens(uid),
    deleteUser: (uid) => auth.deleteUser(uid),
  };
}

export interface CreateCustomerDependencies {
  auth?: OfficerAuthAdapter;
  db?: FirebaseFirestore.Firestore;
  now?: () => admin.firestore.Timestamp;
  accountNumberGenerator?: () => string;
}

export async function createCustomerWithAudit(
  actorUid: string,
  input: CreateCustomerInput,
  deps: CreateCustomerDependencies = {}
): Promise<{ uid: string }> {
  const auth = deps.auth ?? getDefaultAuthAdapter();
  const db = deps.db ?? admin.firestore();
  const nowTs = (deps.now ?? (() => admin.firestore.Timestamp.now()))();
  const nextAccountNumber = deps.accountNumberGenerator ?? generateAccountNumber;

  let createdUid: string;
  try {
    const createdUser = await auth.createUser({
      email: input.email,
      password: input.initialPassword,
      displayName: input.fullName,
      disabled: false,
    });
    createdUid = createdUser.uid;
  } catch (err) {
    const mapped = mapFirebaseAuthCreateError(err);
    if (mapped) {
      throw mapped;
    }
    throw new OfficerServiceError(
      500,
      "INTERNAL_ERROR",
      "Không thể tạo tài khoản Firebase Auth cho khách hàng mới."
    );
  }

  try {
    await auth.setCustomUserClaims(createdUid, { role: "CUSTOMER" });

    await db.runTransaction(async (tx) => {
      let chosenAccountNumber: string | null = null;
      for (let attempt = 0; attempt < MAX_ACCOUNT_NUMBER_ATTEMPTS; attempt += 1) {
        const candidate = nextAccountNumber();
        const existingAccountSnap = await tx.get(
          db.collection("accounts").where("accountNumber", "==", candidate).limit(1)
        );
        if (existingAccountSnap.empty) {
          chosenAccountNumber = candidate;
          break;
        }
      }

      if (!chosenAccountNumber) {
        throw new Error("FAILED_UNIQUE_ACCOUNT_NUMBER");
      }

      const userRef = db.collection("users").doc(createdUid);
      const checkingAccountRef = db.collection("accounts").doc();
      const auditRef = db.collection("audit_logs").doc();

      tx.set(userRef, {
        role: "CUSTOMER",
        fullName: input.fullName,
        email: input.email,
        phone: input.phone,
        nationalId: input.nationalId,
        dob: input.dob,
        address: input.address,
        avatarUrl: "",
        status: "ACTIVE",
        ekyc: {
          status: "NONE",
          verifiedBy: null,
          verifiedAt: null,
        },
        createdBy: actorUid,
        createdAt: nowTs,
        updatedAt: nowTs,
      });

      tx.set(checkingAccountRef, {
        ownerUid: createdUid,
        type: "CHECKING",
        accountNumber: chosenAccountNumber,
        status: "ACTIVE",
        balance: 0,
        currency: "VND",
        perTxnLimit: DEFAULT_CHECKING_PER_TXN_LIMIT,
        dailyLimit: DEFAULT_CHECKING_DAILY_LIMIT,
        saving: null,
        mortgage: null,
        createdAt: nowTs,
      });

      tx.set(
        auditRef,
        buildAuditLogPayload(
          actorUid,
          "CREATE_CUSTOMER",
          createdUid,
          {
            email: input.email,
            fullName: input.fullName,
            accountId: checkingAccountRef.id,
            accountNumber: chosenAccountNumber,
          },
          nowTs
        )
      );
    });

    return { uid: createdUid };
  } catch (err) {
    try {
      await auth.deleteUser(createdUid);
    } catch (rollbackErr) {
      console.error("[NovaPay Officer] Rollback deleteUser thất bại:", rollbackErr);
    }
    if (err instanceof OfficerServiceError) {
      throw err;
    }
    throw new OfficerServiceError(
      500,
      "INTERNAL_ERROR",
      "Không thể hoàn tất khởi tạo hồ sơ và tài khoản thanh toán cho khách hàng."
    );
  }
}

export interface UpdateCustomerDependencies {
  auth?: OfficerAuthAdapter;
  db?: FirebaseFirestore.Firestore;
  now?: () => admin.firestore.Timestamp;
}

export async function updateCustomerWithAudit(
  actorUid: string,
  input: UpdateCustomerInput,
  deps: UpdateCustomerDependencies = {}
): Promise<{ ok: true }> {
  const auth = deps.auth ?? getDefaultAuthAdapter();
  const db = deps.db ?? admin.firestore();
  const nowTs = (deps.now ?? (() => admin.firestore.Timestamp.now()))();

  const transactionResult = await db.runTransaction(async (tx) => {
    const userRef = db.collection("users").doc(input.uid);
    const snap = await tx.get(userRef);
    if (!snap.exists) {
      throw new OfficerServiceError(
        404,
        "USER_NOT_FOUND",
        "Không tìm thấy hồ sơ khách hàng theo uid được cung cấp."
      );
    }

    const existingData = snap.data() || {};
    if (existingData.role === "OFFICER") {
      throw new OfficerServiceError(
        403,
        "PERMISSION_DENIED",
        "Không được phép chỉnh sửa hoặc khóa tài khoản nhân viên ngân hàng qua nghiệp vụ khách hàng."
      );
    }
    const previousStatus =
      typeof existingData.status === "string" ? existingData.status : "ACTIVE";

    const updateFields: Record<string, unknown> = {
      updatedAt: nowTs,
    };
    const updatedFieldNames: string[] = [];

    if (input.fullName !== undefined) {
      updateFields.fullName = input.fullName;
      updatedFieldNames.push("fullName");
    }
    if (input.phone !== undefined) {
      updateFields.phone = input.phone;
      updatedFieldNames.push("phone");
    }
    if (input.address !== undefined) {
      updateFields.address = input.address;
      updatedFieldNames.push("address");
    }
    if (input.status !== undefined) {
      updateFields.status = input.status;
      updatedFieldNames.push("status");
    }

    let action = "UPDATE_CUSTOMER";
    if (input.status !== undefined && input.status !== previousStatus) {
      action = input.status === "LOCKED" ? "LOCK_CUSTOMER" : "UNLOCK_CUSTOMER";
    }

    tx.update(userRef, updateFields);
    tx.set(
      db.collection("audit_logs").doc(),
      buildAuditLogPayload(
        actorUid,
        action,
        input.uid,
        {
          updatedFields: updatedFieldNames,
          previousStatus,
          newStatus: input.status ?? previousStatus,
        },
        nowTs
      )
    );

    return { statusChangedTo: input.status };
  });

  if (transactionResult.statusChangedTo !== undefined) {
    try {
      const shouldDisable = transactionResult.statusChangedTo === "LOCKED";
      await auth.updateUser(input.uid, { disabled: shouldDisable });
      if (shouldDisable) {
        await auth.revokeRefreshTokens(input.uid);
      }
    } catch (authErr) {
      const code = (authErr as { code?: unknown } | null)?.code;
      if (code === "auth/user-not-found") {
        console.warn(
          `[NovaPay Officer] Không tìm thấy Firebase Auth user ${input.uid} khi đồng bộ trạng thái.`
        );
      } else {
        throw new OfficerServiceError(
          500,
          "INTERNAL_ERROR",
          "Đã cập nhật Firestore nhưng không thể đồng bộ trạng thái khóa trên Firebase Auth."
        );
      }
    }
  }

  return { ok: true };
}

export interface UpsertInterestRateDependencies {
  db?: FirebaseFirestore.Firestore;
  now?: () => admin.firestore.Timestamp;
}

export async function upsertInterestRateWithAudit(
  actorUid: string,
  input: UpsertInterestRateInput,
  deps: UpsertInterestRateDependencies = {}
): Promise<{ rateId: string }> {
  const db = deps.db ?? admin.firestore();
  const nowTs = (deps.now ?? (() => admin.firestore.Timestamp.now()))();

  const rateId = await db.runTransaction(async (tx) => {
    const existingQuery = db
      .collection("interest_rates")
      .where("termMonths", "==", input.termMonths)
      .limit(1);
    const existingSnap = await tx.get(existingQuery);

    const existingDoc = existingSnap.empty ? null : existingSnap.docs[0];
    const targetRateId = existingDoc ? existingDoc.id : `term_${input.termMonths}`;
    const targetRef = db.collection("interest_rates").doc(targetRateId);
    const previousAnnualRate = existingDoc ? existingDoc.data().annualRate ?? null : null;

    tx.set(
      targetRef,
      {
        termMonths: input.termMonths,
        annualRate: input.annualRate,
        effectiveFrom: nowTs,
        updatedBy: actorUid,
        updatedAt: nowTs,
      },
      { merge: true }
    );

    tx.set(
      db.collection("audit_logs").doc(),
      buildAuditLogPayload(
        actorUid,
        "UPSERT_INTEREST_RATE",
        targetRateId,
        {
          termMonths: input.termMonths,
          previousAnnualRate,
          annualRate: input.annualRate,
        },
        nowTs
      )
    );

    return targetRateId;
  });

  return { rateId };
}
