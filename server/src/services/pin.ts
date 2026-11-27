import crypto from "node:crypto";
import admin from "firebase-admin";
import { config } from "../config";

export const PIN_LENGTH = 6;
export const PIN_MAX_ATTEMPTS = 5;
export const PIN_LOCKOUT_MS = 15 * 60 * 1000;

const PIN_PATTERN = /^\d{6}$/;

export class PinServiceError extends Error {
  readonly statusCode: number;
  readonly errorCode: string;

  constructor(statusCode: number, errorCode: string, message: string) {
    super(message);
    this.name = "PinServiceError";
    this.statusCode = statusCode;
    this.errorCode = errorCode;
  }
}

export interface PinStatusResult {
  hasTransactionPin: boolean;
  pinLockedUntilEpochMs: number | null;
}

export interface PinVerificationResult {
  hasPin: boolean;
  verified: boolean;
}

export interface SetPinResult {
  ok: true;
  hasTransactionPin: true;
  isChange: boolean;
}

export interface PinServiceDeps {
  hmacSecret?: string;
}

export interface PinService {
  isValidPinFormat(pin: unknown): pin is string;
  hashPin(pin: string, uid: string): string;
  verifyPinHash(pin: string, uid: string, expectedHash: string): boolean;
  getPinStatus(
    db: FirebaseFirestore.Firestore,
    uid: string,
    nowMs?: number
  ): Promise<PinStatusResult>;
  setOrChangePin(
    db: FirebaseFirestore.Firestore,
    uid: string,
    params: { currentPin?: unknown; newPin: unknown },
    nowMs?: number
  ): Promise<SetPinResult>;
  verifyTransactionPin(
    db: FirebaseFirestore.Firestore,
    uid: string,
    pin: unknown,
    nowMs?: number
  ): Promise<PinVerificationResult>;
}

function resolveHmacSecret(explicitSecret?: string): string {
  const secret = (
    explicitSecret ??
    process.env.PIN_HMAC_SECRET ??
    config.firebaseProjectId ??
    ""
  ).trim();
  return secret || "novapay-transaction-pin-v1";
}

function toEpochMs(value: unknown): number | null {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  if (value instanceof Date && !Number.isNaN(value.getTime())) {
    return value.getTime();
  }
  if (value && typeof value === "object") {
    const maybeTs = value as { toMillis?: () => number; toDate?: () => Date; _seconds?: number; seconds?: number };
    if (typeof maybeTs.toMillis === "function") {
      return maybeTs.toMillis();
    }
    if (typeof maybeTs.toDate === "function") {
      return maybeTs.toDate().getTime();
    }
    const sec =
      typeof maybeTs._seconds === "number"
        ? maybeTs._seconds
        : typeof maybeTs.seconds === "number"
          ? maybeTs.seconds
          : null;
    if (sec !== null && Number.isFinite(sec)) {
      return sec * 1000;
    }
  }
  return null;
}

export function isValidPinFormat(pin: unknown): pin is string {
  return typeof pin === "string" && PIN_PATTERN.test(pin.trim());
}

export function hashPin(pin: string, uid: string, hmacSecret?: string): string {
  const key = `${resolveHmacSecret(hmacSecret)}:${uid}`;
  return crypto.createHmac("sha256", key).update(pin, "utf8").digest("hex");
}

export function verifyPinHash(
  pin: string,
  uid: string,
  expectedHash: string,
  hmacSecret?: string
): boolean {
  if (!PIN_PATTERN.test(pin)) {
    return false;
  }
  if (!/^[a-f0-9]{64}$/i.test(expectedHash)) {
    return false;
  }

  const actualBuf = Buffer.from(hashPin(pin, uid, hmacSecret), "hex");
  const expectedBuf = Buffer.from(expectedHash.toLowerCase(), "hex");
  if (actualBuf.length !== expectedBuf.length) {
    return false;
  }
  return crypto.timingSafeEqual(actualBuf, expectedBuf);
}

export function createPinService(deps?: PinServiceDeps): PinService {
  const secret = deps?.hmacSecret;

  return {
    isValidPinFormat,

    hashPin(pin: string, uid: string): string {
      return hashPin(pin, uid, secret);
    },

    verifyPinHash(pin: string, uid: string, expectedHash: string): boolean {
      return verifyPinHash(pin, uid, expectedHash, secret);
    },

    async getPinStatus(
      db: FirebaseFirestore.Firestore,
      uid: string,
      nowMs: number = Date.now()
    ): Promise<PinStatusResult> {
      const secretSnap = await db.collection("user_secrets").doc(uid).get();
      if (!secretSnap.exists) {
        return {
          hasTransactionPin: false,
          pinLockedUntilEpochMs: null,
        };
      }
      const data = secretSnap.data() ?? {};
      const hasPin = typeof data.pinHash === "string" && data.pinHash.length > 0;
      const lockedUntil = toEpochMs(data.pinLockedUntil);
      const activeLockedUntil = lockedUntil !== null && lockedUntil > nowMs ? lockedUntil : null;

      return {
        hasTransactionPin: hasPin,
        pinLockedUntilEpochMs: activeLockedUntil,
      };
    },

    async setOrChangePin(
      db: FirebaseFirestore.Firestore,
      uid: string,
      params: { currentPin?: unknown; newPin: unknown },
      nowMs: number = Date.now()
    ): Promise<SetPinResult> {
      const rawNewPin = typeof params.newPin === "string" ? params.newPin.trim() : "";
      if (!PIN_PATTERN.test(rawNewPin)) {
        throw new PinServiceError(
          400,
          "INVALID_ARGUMENT",
          "Mã PIN giao dịch mới bắt buộc phải gồm đúng 6 chữ số."
        );
      }

      const rawCurrentPin = typeof params.currentPin === "string" ? params.currentPin.trim() : "";
      const userRef = db.collection("users").doc(uid);
      const secretRef = db.collection("user_secrets").doc(uid);
      const nowTs = admin.firestore.Timestamp.fromMillis(nowMs);

      type TxOutcome =
        | { ok: true; isChange: boolean }
        | { ok: false; statusCode: number; errorCode: string; message: string };

      const outcome = await db.runTransaction(async (tx): Promise<TxOutcome> => {
        const userSnap = await tx.get(userRef);
        const secretSnap = await tx.get(secretRef);

        if (!userSnap.exists) {
          return {
            ok: false,
            statusCode: 403,
            errorCode: "PERMISSION_DENIED",
            message: "Không tìm thấy hồ sơ người dùng.",
          };
        }
        const userData = userSnap.data() ?? {};
        if (userData.status !== "ACTIVE") {
          return {
            ok: false,
            statusCode: 403,
            errorCode: "PERMISSION_DENIED",
            message: "Tài khoản người dùng hiện không hoạt động.",
          };
        }

        const secretData = secretSnap.exists ? secretSnap.data() ?? {} : {};
        const existingHash = typeof secretData.pinHash === "string" ? secretData.pinHash : "";
        const isChange = existingHash.length > 0;

        if (isChange) {
          const lockedUntil = toEpochMs(secretData.pinLockedUntil);
          if (lockedUntil !== null && nowMs < lockedUntil) {
            return {
              ok: false,
              statusCode: 429,
              errorCode: "PIN_LOCKED",
              message: "Mã PIN giao dịch đang tạm khóa 15 phút do nhập sai quá 5 lần.",
            };
          }

          if (!rawCurrentPin) {
            return {
              ok: false,
              statusCode: 400,
              errorCode: "INVALID_ARGUMENT",
              message: "Vui lòng nhập mã PIN giao dịch hiện tại.",
            };
          }

          if (!verifyPinHash(rawCurrentPin, uid, existingHash, secret)) {
            const baseAttempts =
              lockedUntil !== null && nowMs >= lockedUntil
                ? 0
                : Number(secretData.pinAttempts ?? 0);
            const nextAttempts = baseAttempts + 1;
            const locked = nextAttempts >= PIN_MAX_ATTEMPTS;
            const nextLockedUntil = locked ? nowMs + PIN_LOCKOUT_MS : null;

            tx.update(secretRef, {
              pinAttempts: nextAttempts,
              pinLockedUntil: nextLockedUntil,
              updatedAt: nowTs,
            });

            return locked
              ? {
                  ok: false,
                  statusCode: 429,
                  errorCode: "PIN_LOCKED",
                  message: "Bạn đã nhập sai mã PIN hiện tại quá 5 lần. Mã PIN tạm khóa 15 phút.",
                }
              : {
                  ok: false,
                  statusCode: 400,
                  errorCode: "PIN_INVALID",
                  message: "Mã PIN giao dịch hiện tại không chính xác.",
                };
          }
        }

        const newPinHash = hashPin(rawNewPin, uid, secret);
        tx.set(secretRef, {
          pinHash: newPinHash,
          pinAttempts: 0,
          pinLockedUntil: null,
          updatedAt: nowTs,
        });
        tx.update(userRef, {
          "security.hasPin": true,
          "security.pinUpdatedAt": nowTs,
          updatedAt: nowTs,
        });

        return { ok: true, isChange };
      });

      if (!outcome.ok) {
        throw new PinServiceError(outcome.statusCode, outcome.errorCode, outcome.message);
      }

      return {
        ok: true,
        hasTransactionPin: true,
        isChange: outcome.isChange,
      };
    },

    async verifyTransactionPin(
      db: FirebaseFirestore.Firestore,
      uid: string,
      pin: unknown,
      nowMs: number = Date.now()
    ): Promise<PinVerificationResult> {
      const secretRef = db.collection("user_secrets").doc(uid);
      const rawPin = typeof pin === "string" ? pin.trim() : "";
      const nowTs = admin.firestore.Timestamp.fromMillis(nowMs);

      type VerifyOutcome =
        | { status: "NO_PIN" }
        | { status: "VERIFIED" }
        | { status: "REQUIRED" }
        | { status: "INVALID" }
        | { status: "LOCKED" };

      const outcome = await db.runTransaction(async (tx): Promise<VerifyOutcome> => {
        const secretSnap = await tx.get(secretRef);
        if (!secretSnap.exists) {
          return { status: "NO_PIN" };
        }
        const secretData = secretSnap.data() ?? {};
        const existingHash = typeof secretData.pinHash === "string" ? secretData.pinHash : "";
        if (!existingHash) {
          return { status: "NO_PIN" };
        }

        const lockedUntil = toEpochMs(secretData.pinLockedUntil);
        if (lockedUntil !== null && nowMs < lockedUntil) {
          return { status: "LOCKED" };
        }

        if (!rawPin) {
          return { status: "REQUIRED" };
        }

        if (verifyPinHash(rawPin, uid, existingHash, secret)) {
          tx.update(secretRef, {
            pinAttempts: 0,
            pinLockedUntil: null,
            updatedAt: nowTs,
          });
          return { status: "VERIFIED" };
        }

        const baseAttempts =
          lockedUntil !== null && nowMs >= lockedUntil
            ? 0
            : Number(secretData.pinAttempts ?? 0);
        const nextAttempts = baseAttempts + 1;
        const locked = nextAttempts >= PIN_MAX_ATTEMPTS;
        const nextLockedUntil = locked ? nowMs + PIN_LOCKOUT_MS : null;

        tx.update(secretRef, {
          pinAttempts: nextAttempts,
          pinLockedUntil: nextLockedUntil,
          updatedAt: nowTs,
        });

        return { status: locked ? "LOCKED" : "INVALID" };
      });

      switch (outcome.status) {
        case "NO_PIN":
          return { hasPin: false, verified: true };
        case "VERIFIED":
          return { hasPin: true, verified: true };
        case "REQUIRED":
          throw new PinServiceError(
            400,
            "PIN_REQUIRED",
            "Vui lòng nhập mã PIN giao dịch 6 số để xác thực."
          );
        case "LOCKED":
          throw new PinServiceError(
            429,
            "PIN_LOCKED",
            "Mã PIN giao dịch đã bị tạm khóa 15 phút do nhập sai quá 5 lần."
          );
        case "INVALID":
          throw new PinServiceError(
            400,
            "PIN_INVALID",
            "Mã PIN giao dịch không chính xác."
          );
      }
    },
  };
}

export const pinService = createPinService();
