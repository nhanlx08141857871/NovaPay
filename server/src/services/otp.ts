import crypto from "crypto";

export const OTP_EXPIRY_SECONDS = 180;
export const OTP_MAX_ATTEMPTS = 5;
export const OTP_RESEND_COOLDOWN_SECONDS = 30;
export const OTP_MAX_RESENDS = 5;
export const FACE_CHECK_MAX_ATTEMPTS = 5;
export const DRAFT_RETENTION_MS = 24 * 60 * 60 * 1000;
export const HIGH_VALUE_THRESHOLD_VND = 5_000_000;
export const MAX_GATEWAY_DEPOSIT_VND = 100_000_000;

export function generateOtpCode(): string {
  return crypto.randomInt(0, 1_000_000).toString().padStart(6, "0");
}

export function hashOtp(otp: string, salt?: string): string {
  const input = salt ? `${salt}:${otp}` : otp;
  return crypto.createHash("sha256").update(input, "utf8").digest("hex");
}

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

export function computeOtpExpiryEpochMs(nowMs: number = Date.now()): number {
  return nowMs + OTP_EXPIRY_SECONDS * 1000;
}

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
