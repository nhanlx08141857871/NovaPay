import crypto from "crypto";

export const OTP_EXPIRY_SECONDS = 180;
export const OTP_MAX_ATTEMPTS = 5;
export const HIGH_VALUE_THRESHOLD_VND = 5_000_000;

/**
 * Generates a cryptographically secure 6-digit numeric OTP code (000000..999999).
 */
export function generateOtpCode(): string {
  return crypto.randomInt(0, 1_000_000).toString().padStart(6, "0");
}

/**
 * Computes a SHA-256 hex digest for the given OTP code (with optional salt such as draftId).
 */
export function hashOtp(otp: string, salt?: string): string {
  const input = salt ? `${salt}:${otp}` : otp;
  return crypto.createHash("sha256").update(input, "utf8").digest("hex");
}

/**
 * Verifies a user-supplied OTP against the stored SHA-256 hash using constant-time comparison.
 */
export function verifyOtpHash(otp: string, expectedHash: string, salt?: string): boolean {
  if (!/^\d{6}$/.test(otp)) {
    return false;
  }
  if (!/^[a-f0-9]{64}$/i.test(expectedHash)) {
    return false;
  }

  const actualBuffer = Buffer.from(hashOtp(otp, salt), "hex");
  const expectedBuffer = Buffer.from(expectedHash.toLowerCase(), "hex");

  if (actualBuffer.length !== expectedBuffer.length) {
    return false;
  }

  return crypto.timingSafeEqual(actualBuffer, expectedBuffer);
}

/**
 * Computes the OTP expiration timestamp in Unix epoch milliseconds.
 */
export function computeOtpExpiryEpochMs(nowMs: number = Date.now()): number {
  return nowMs + OTP_EXPIRY_SECONDS * 1000;
}

/**
 * Checks whether an OTP expiration epoch-ms timestamp is missing or already past.
 */
export function isOtpExpired(
  otpExpiresAt: number | null | undefined,
  nowMs: number = Date.now()
): boolean {
  return typeof otpExpiresAt !== "number" || !Number.isFinite(otpExpiresAt) || nowMs > otpExpiresAt;
}

export interface OtpService {
  generateOtpCode: () => string;
  hashOtp: (otp: string, salt?: string) => string;
  verifyOtpHash: (otp: string, expectedHash: string, salt?: string) => boolean;
  computeOtpExpiryEpochMs: (nowMs?: number) => number;
  isOtpExpired: (otpExpiresAt: number | null | undefined, nowMs?: number) => boolean;
}

export const otpService: OtpService = {
  generateOtpCode,
  hashOtp,
  verifyOtpHash,
  computeOtpExpiryEpochMs,
  isOtpExpired,
};
