import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { EmailSendError, createEmailService } from "../src/services/email";
import {
  EkycFaceNotFoundError,
  FaceCheckUnavailableError,
  SAMPLE_SEED_PLACEHOLDER_BASE64,
  createFaceCheckService,
  parseGeminiFaceCheckResponse,
  stripBase64DataUrlPrefix,
} from "../src/services/faceCheck";
import {
  HIGH_VALUE_THRESHOLD_VND,
  OTP_EXPIRY_SECONDS,
  OTP_MAX_ATTEMPTS,
  computeOtpExpiryEpochMs,
  generateOtpCode,
  hashOtp,
  isOtpExpired,
  verifyOtpHash,
} from "../src/services/otp";
import { StripeServiceError, createStripeService } from "../src/services/stripe";

describe("OTP Service", () => {
  it("exposes required security constants", () => {
    assert.equal(OTP_EXPIRY_SECONDS, 180);
    assert.equal(OTP_MAX_ATTEMPTS, 5);
    assert.equal(HIGH_VALUE_THRESHOLD_VND, 5_000_000);
  });

  it("generates 6-digit zero-padded numeric OTP codes", () => {
    for (let i = 0; i < 50; i++) {
      const code = generateOtpCode();
      assert.match(code, /^\d{6}$/);
    }
  });

  it("hashes OTP with SHA-256 and verifies via constant-time comparison", () => {
    const hash = hashOtp("042918", "draft_123");
    assert.match(hash, /^[a-f0-9]{64}$/);
    assert.equal(verifyOtpHash("042918", hash, "draft_123"), true);
    assert.equal(verifyOtpHash("042919", hash, "draft_123"), false);
    assert.equal(verifyOtpHash("042918", hash, "draft_999"), false);
    assert.equal(verifyOtpHash("abc123", hash, "draft_123"), false);
    assert.equal(verifyOtpHash("042918", "short_hash", "draft_123"), false);
  });

  it("computes 180s expiry in Unix epoch milliseconds", () => {
    const now = 1_700_000_000_000;
    const expiresAt = computeOtpExpiryEpochMs(now);
    assert.equal(expiresAt, now + 180_000);
    assert.equal(isOtpExpired(expiresAt, now + 180_000), false);
    assert.equal(isOtpExpired(expiresAt, now + 180_001), true);
    assert.equal(isOtpExpired(null, now), true);
  });
});

describe("Email Service (Brevo HTTPS)", () => {
  it("sends POST request to Brevo v3/smtp/email with api-key and Vietnamese HTML body", async () => {
    let capturedUrl = "";
    let capturedInit: RequestInit | undefined;

    const svc = createEmailService({
      apiKey: "xkeysib-test-key",
      senderEmail: "no-reply@novapay.vn",
      senderName: "NovaPay Banking",
      fetchFn: async (input, init) => {
        capturedUrl = String(input);
        capturedInit = init;
        return new Response(JSON.stringify({ messageId: "msg_1" }), { status: 201 });
      },
    });

    await svc.sendOtpEmail({
      toEmail: "customer@novapay.vn",
      toName: "Nguyễn Văn A",
      otp: "654321",
      expiresInSeconds: 180,
      transactionType: "TRANSFER_INTERNAL",
      amountVnd: 1_500_000,
    });

    assert.equal(capturedUrl, "https://api.brevo.com/v3/smtp/email");
    assert.equal(capturedInit?.method, "POST");
    const headers = capturedInit?.headers as Record<string, string>;
    assert.equal(headers["api-key"], "xkeysib-test-key");
    assert.equal(headers["content-type"], "application/json");

    const parsedBody = JSON.parse(String(capturedInit?.body)) as {
      sender: { name: string; email: string };
      to: Array<{ email: string; name: string }>;
      subject: string;
      htmlContent: string;
    };
    assert.equal(parsedBody.sender.email, "no-reply@novapay.vn");
    assert.equal(parsedBody.to[0].email, "customer@novapay.vn");
    assert.ok(parsedBody.htmlContent.includes("654321"));
    assert.ok(parsedBody.htmlContent.includes("180 giây"));
  });

  it("throws EmailSendError (EMAIL_SEND_FAILED) when Brevo returns non-2xx", async () => {
    const svc = createEmailService({
      apiKey: "xkeysib-test-key",
      fetchFn: async () => new Response("Unauthorized", { status: 401 }),
    });

    await assert.rejects(
      () =>
        svc.sendOtpEmail({
          toEmail: "customer@novapay.vn",
          otp: "123456",
        }),
      (err: unknown) => err instanceof EmailSendError && err.code === "EMAIL_SEND_FAILED"
    );
  });

  it("skips network call gracefully in non-production when BREVO_API_KEY is missing", async () => {
    let called = false;
    const svc = createEmailService({
      apiKey: "",
      nodeEnv: "test",
      fetchFn: async () => {
        called = true;
        return new Response("ok", { status: 200 });
      },
    });

    await svc.sendOtpEmail({
      toEmail: "dev@novapay.vn",
      otp: "111222",
    });
    assert.equal(called, false);
  });

  it("throws EmailSendError when strictApiKey is true and BREVO_API_KEY is missing", async () => {
    const svc = createEmailService({
      apiKey: "",
      strictApiKey: true,
    });

    await assert.rejects(
      () => svc.sendOtpEmail({ toEmail: "prod@novapay.vn", otp: "111222" }),
      (err: unknown) => err instanceof EmailSendError && err.code === "EMAIL_SEND_FAILED"
    );
  });
});

describe("Stripe Service", () => {
  it("passes zero-decimal VND amount directly (never * 100), metadata, and idempotencyKey", async () => {
    let capturedCreateParams: unknown;
    let capturedOptions: unknown;

    const svc = createStripeService({
      stripeClient: {
        paymentIntents: {
          create: async (params, options) => {
            capturedCreateParams = params;
            capturedOptions = options;
            return {
              id: "pi_test_123",
              client_secret: "pi_test_123_secret_abc",
            } as never;
          },
          retrieve: async (id) =>
            ({
              id,
              status: "succeeded",
              amount: 2_500_000,
              currency: "vnd",
            }) as never,
        },
      },
    });

    const created = await svc.createPaymentIntent({
      amountVnd: 2_500_000,
      draftId: "draft_abc",
      uid: "user_1",
      accountId: "acc_chk_1",
    });

    assert.equal(created.stripeIntentId, "pi_test_123");
    assert.equal(created.clientSecret, "pi_test_123_secret_abc");
    assert.deepEqual(capturedCreateParams, {
      amount: 2_500_000,
      currency: "vnd",
      automatic_payment_methods: { enabled: true },
      metadata: {
        draftId: "draft_abc",
        uid: "user_1",
        accountId: "acc_chk_1",
      },
    });
    assert.deepEqual(capturedOptions, { idempotencyKey: "novapay_draft_draft_abc" });

    const retrieved = await svc.retrievePaymentIntent("pi_test_123");
    assert.equal(retrieved.status, "succeeded");
    assert.equal(retrieved.amount, 2_500_000);
    assert.equal(retrieved.currency, "vnd");
  });

  it("rejects non-integer or non-positive VND amounts", async () => {
    const svc = createStripeService({
      secretKey: "sk_test_dummy",
    });

    await assert.rejects(
      () =>
        svc.createPaymentIntent({
          amountVnd: 100.5,
          draftId: "d1",
          uid: "u1",
          accountId: "a1",
        }),
      (err: unknown) => err instanceof StripeServiceError && err.code === "INVALID_ARGUMENT"
    );
  });

  it("fails clearly when invoked without STRIPE_SECRET_KEY", async () => {
    const svc = createStripeService({ secretKey: "" });
    await assert.rejects(
      () =>
        svc.createPaymentIntent({
          amountVnd: 50_000,
          draftId: "d1",
          uid: "u1",
          accountId: "a1",
        }),
      (err: unknown) => err instanceof StripeServiceError && err.code === "SERVICE_UNAVAILABLE"
    );
  });
});

describe("Gemini FaceCheck Service", () => {
  it("strips optional data:image base64 prefixes", () => {
    assert.equal(
      stripBase64DataUrlPrefix("  data:image/jpeg;base64,/9j/4AAQ==  "),
      "/9j/4AAQ=="
    );
    assert.equal(stripBase64DataUrlPrefix("rawBase64=="), "rawBase64==");
  });

  it("sends both images and strict biometric systemInstruction to configured GEMINI_MODEL and passes when matched=true & confidence >= 0.80", async () => {
    let capturedUrl = "";
    let capturedBody: Record<string, unknown> = {};

    const svc = createFaceCheckService({
      apiKey: "gemini-key",
      model: "gemini-2.5-flash",
      fetchFn: async (input, init) => {
        capturedUrl = String(input);
        capturedBody = JSON.parse(String(init?.body)) as Record<string, unknown>;
        return new Response(
          JSON.stringify({
            candidates: [
              {
                content: {
                  parts: [
                    {
                      text: JSON.stringify({
                        matched: true,
                        confidence: 0.91,
                        reason: "Cấu trúc khuôn mặt trùng khớp.",
                      }),
                    },
                  ],
                },
              },
            ],
          }),
          { status: 200 }
        );
      },
    });

    const res = await svc.verifyFaceMatch("data:image/jpeg;base64,REF_FACE_IMG", "LIVE_FACE_IMG");
    assert.equal(res.matched, true);
    assert.equal(res.manualAuditRequired, false);
    assert.equal(res.confidence, 0.91);
    assert.ok(capturedUrl.includes("models/gemini-2.5-flash:generateContent"));
    assert.ok(capturedBody.systemInstruction);
    const contents = capturedBody.contents as Array<{ parts: Array<Record<string, unknown>> }>;
    const inlineParts = contents[0].parts.filter((p) => p.inline_data !== undefined);
    assert.equal(inlineParts.length, 2);
    assert.deepEqual(inlineParts[0].inline_data, { mime_type: "image/jpeg", data: "REF_FACE_IMG" });
    assert.deepEqual(inlineParts[1].inline_data, { mime_type: "image/jpeg", data: "LIVE_FACE_IMG" });
  });

  it("rejects when matched is false or confidence < 0.80 and preserves Vietnamese reason", () => {
    const lowConf = parseGeminiFaceCheckResponse(
      '{"matched": true, "confidence": 0.79, "reason": "Độ tin cậy chưa đạt ngưỡng 0.80"}'
    );
    assert.equal(lowConf.matched, false);
    assert.equal(lowConf.manualAuditRequired, false);

    const diffPerson = parseGeminiFaceCheckResponse(
      '{"matched": false, "confidence": 0.12, "reason": "Khác cấu trúc sống mũi và khuôn hàm"}'
    );
    assert.equal(diffPerson.matched, false);
    assert.equal(diffPerson.reason, "Khác cấu trúc sống mũi và khuôn hàm");
  });

  it("throws EkycFaceNotFoundError when reference eKYC image is empty or 1x1 seed placeholder", async () => {
    const svc = createFaceCheckService({ apiKey: "gemini-key" });
    await assert.rejects(
      () => svc.verifyFaceMatch("", "LIVE_FACE"),
      (err: unknown) => err instanceof EkycFaceNotFoundError && err.code === "EKYC_FACE_NOT_FOUND"
    );
    await assert.rejects(
      () => svc.verifyFaceMatch(SAMPLE_SEED_PLACEHOLDER_BASE64, "LIVE_FACE"),
      (err: unknown) => err instanceof EkycFaceNotFoundError && err.code === "EKYC_FACE_NOT_FOUND"
    );
  });

  it("throws FaceCheckUnavailableError (HTTP 503) instead of auto-passing on HTTP 429, missing key, or timeout", async () => {
    const quotaSvc = createFaceCheckService({
      apiKey: "gemini-key",
      fetchFn: async () => new Response("Too Many Requests", { status: 429 }),
    });
    await assert.rejects(
      () => quotaSvc.verifyFaceMatch("REF_FACE", "LIVE_FACE"),
      (err: unknown) =>
        err instanceof FaceCheckUnavailableError &&
        err.statusCode === 503 &&
        err.reasonCode === "GEMINI_QUOTA_EXCEEDED"
    );

    const noKeySvc = createFaceCheckService({ apiKey: "" });
    await assert.rejects(
      () => noKeySvc.verifyFaceMatch("REF_FACE", "LIVE_FACE"),
      (err: unknown) =>
        err instanceof FaceCheckUnavailableError &&
        err.statusCode === 503 &&
        err.reasonCode === "GEMINI_API_KEY_MISSING"
    );

    const timeoutSvc = createFaceCheckService({
      apiKey: "gemini-key",
      timeoutMs: 20,
      fetchFn: async (_url, init) =>
        new Promise((_resolve, reject) => {
          init?.signal?.addEventListener("abort", () => {
            const err = new Error("The operation was aborted");
            err.name = "AbortError";
            reject(err);
          });
        }),
    });
    await assert.rejects(
      () => timeoutSvc.verifyFaceMatch("REF_FACE", "LIVE_FACE"),
      (err: unknown) =>
        err instanceof FaceCheckUnavailableError &&
        err.statusCode === 503 &&
        err.reasonCode === "GEMINI_TIMEOUT"
    );
  });
});
