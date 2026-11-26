import { config } from "../config";
import { OTP_EXPIRY_SECONDS } from "./otp";

const BREVO_SMTP_EMAIL_URL = "https://api.brevo.com/v3/smtp/email";
const DEFAULT_EMAIL_TIMEOUT_MS = 8_000;

export class EmailSendError extends Error {
  readonly code = "EMAIL_SEND_FAILED";

  constructor(message: string) {
    super(message);
    this.name = "EmailSendError";
  }
}

export interface SendOtpEmailParams {
  toEmail: string;
  toName?: string;
  otp: string;
  expiresInSeconds?: number;
  transactionType?: string;
  amountVnd?: number;
}

export interface EmailServiceDeps {
  fetchFn?: typeof fetch;
  apiKey?: string;
  senderEmail?: string;
  senderName?: string;
  timeoutMs?: number;
  strictApiKey?: boolean;
  nodeEnv?: string;
}

export interface EmailService {
  sendOtpEmail(params: SendOtpEmailParams): Promise<void>;
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function formatVnd(amount: number): string {
  return `${Math.trunc(amount).toLocaleString("vi-VN")} ₫`;
}

function buildOtpEmailHtml(params: SendOtpEmailParams): string {
  const safeName = escapeHtml((params.toName || "Quý khách").trim() || "Quý khách");
  const safeOtp = escapeHtml(params.otp);
  const expiresIn = params.expiresInSeconds ?? OTP_EXPIRY_SECONDS;
  const txSummary =
    typeof params.amountVnd === "number" && Number.isFinite(params.amountVnd)
      ? `<p>Loại giao dịch: <strong>${escapeHtml(params.transactionType || "Giao dịch tài chính")}</strong> — Số tiền: <strong>${escapeHtml(formatVnd(params.amountVnd))}</strong></p>`
      : "";

  return [
    '<div style="font-family: Arial, sans-serif; line-height: 1.6; color: #1f2937;">',
    `<h2 style="color: #0f4c81;">NovaPay — Xác thực giao dịch OTP</h2>`,
    `<p>Xin chào <strong>${safeName}</strong>,</p>`,
    txSummary,
    `<p>Mã OTP xác thực giao dịch của bạn là:</p>`,
    `<div style="font-size: 28px; font-weight: bold; letter-spacing: 6px; color: #0f4c81; padding: 12px 16px; background: #f3f6fa; display: inline-block; border-radius: 8px;">${safeOtp}</div>`,
    `<p>Mã OTP có hiệu lực trong <strong>${expiresIn} giây</strong>. Tuyệt đối không chia sẻ mã này cho bất kỳ ai.</p>`,
    `<p style="color: #6b7280; font-size: 12px;">Ngân hàng số NovaBank — Hệ thống Thanh toán Điện tử NovaPay</p>`,
    "</div>",
  ].join("");
}

export function createEmailService(deps?: EmailServiceDeps): EmailService {
  const fetchFn = deps?.fetchFn ?? fetch;
  const apiKey = (deps?.apiKey ?? config.brevoApiKey).trim();
  const senderEmail = (deps?.senderEmail ?? config.brevoSenderEmail).trim() || "no-reply@novapay.vn";
  const senderName = (deps?.senderName ?? config.brevoSenderName).trim() || "NovaPay Banking";
  const timeoutMs = deps?.timeoutMs ?? DEFAULT_EMAIL_TIMEOUT_MS;
  const nodeEnv = deps?.nodeEnv ?? process.env.NODE_ENV ?? "development";
  const strictApiKey = deps?.strictApiKey ?? nodeEnv === "production";

  return {
    async sendOtpEmail(params: SendOtpEmailParams): Promise<void> {
      const toEmail = params.toEmail?.trim() ?? "";
      if (!toEmail || !toEmail.includes("@")) {
        throw new EmailSendError("Địa chỉ email người nhận OTP không hợp lệ.");
      }

      if (!apiKey) {
        if (strictApiKey) {
          throw new EmailSendError("BREVO_API_KEY chưa được cấu hình trên máy chủ.");
        }
        console.warn(
          `[NovaPay Email] BREVO_API_KEY chưa được cấu hình (môi trường ${nodeEnv}) — bỏ qua gửi email OTP tới ${toEmail}.`
        );
        return;
      }

      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), timeoutMs);

      try {
        const response = await fetchFn(BREVO_SMTP_EMAIL_URL, {
          method: "POST",
          headers: {
            accept: "application/json",
            "content-type": "application/json",
            "api-key": apiKey,
          },
          body: JSON.stringify({
            sender: {
              name: senderName,
              email: senderEmail,
            },
            to: [
              {
                email: toEmail,
                name: (params.toName || toEmail).trim(),
              },
            ],
            subject: "Mã xác thực OTP giao dịch NovaPay",
            htmlContent: buildOtpEmailHtml(params),
          }),
          signal: controller.signal,
        });

        if (!response.ok) {
          const detail = await response.text().catch(() => "");
          throw new EmailSendError(
            `Không thể gửi email OTP qua Brevo (HTTP ${response.status})${detail ? `: ${detail.slice(0, 200)}` : "."}`
          );
        }
      } catch (err) {
        if (err instanceof EmailSendError) {
          throw err;
        }
        const message = err instanceof Error ? err.message : String(err);
        throw new EmailSendError(`Lỗi kết nối dịch vụ gửi email OTP Brevo: ${message}`);
      } finally {
        clearTimeout(timer);
      }
    },
  };
}

export const emailService = createEmailService();

export async function sendOtpEmail(params: SendOtpEmailParams): Promise<void> {
  return emailService.sendOtpEmail(params);
}
