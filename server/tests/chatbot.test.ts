import test from "node:test";
import assert from "node:assert/strict";
import {
  buildRuleBasedReply,
  formatVndInteger,
  generateChatReply,
  getVietnamMonthStartMillis,
  parseAndSanitizeAiResponse,
  validateChatMessageInput,
} from "../src/services/chatbot";

test("validateChatMessageInput accepts trimmed non-empty strings and rejects blank/oversized", () => {
  assert.deepEqual(validateChatMessageInput({ message: "  Số dư của tôi?  " }), {
    ok: true,
    message: "Số dư của tôi?",
  });
  assert.equal(validateChatMessageInput({ message: "   " }).ok, false);
  assert.equal(validateChatMessageInput({ message: 123 }).ok, false);
  assert.equal(validateChatMessageInput({ message: "a".repeat(2001) }).ok, false);
});

test("getVietnamMonthStartMillis respects Asia/Ho_Chi_Minh UTC+7 month boundary", () => {
  // 2026-11-30T18:00:00Z is 2026-12-01T01:00:00+07:00 -> Dec 2026 start in VN is 2026-11-30T17:00:00Z
  const utcMs = Date.UTC(2026, 10, 30, 18, 0, 0, 0);
  const vnMonthStart = getVietnamMonthStartMillis(utcMs);
  assert.equal(vnMonthStart, Date.UTC(2026, 10, 30, 17, 0, 0, 0));

  // 2026-11-30T16:30:00Z is 2026-11-30T23:30:00+07:00 -> Nov 2026 start in VN is 2026-10-31T17:00:00Z
  const prevDayUtcMs = Date.UTC(2026, 10, 30, 16, 30, 0, 0);
  assert.equal(
    getVietnamMonthStartMillis(prevDayUtcMs),
    Date.UTC(2026, 9, 31, 17, 0, 0, 0)
  );
});

test("parseAndSanitizeAiResponse strips markdown fences, validates reply, and whitelists deepLinks", () => {
  const raw = [
    "```json",
    JSON.stringify({
      reply: "Số dư hiện tại là 5.000.000 ₫.",
      actions: [
        { label: "Chuyển tiền", deepLink: "novapay://transfer" },
        { label: "Malicious", deepLink: "https://evil.example.com" },
        { label: "Officer only", deepLink: "novapay://officer" },
        { label: "Hóa đơn", deepLink: "novapay://bills" },
      ],
    }),
    "```",
  ].join("\n");

  const parsed = parseAndSanitizeAiResponse(raw);
  assert.ok(parsed);
  assert.equal(parsed.reply, "Số dư hiện tại là 5.000.000 ₫.");
  assert.deepEqual(parsed.actions, [
    { label: "Chuyển tiền", deepLink: "novapay://transfer" },
    { label: "Hóa đơn", deepLink: "novapay://bills" },
  ]);

  assert.equal(parseAndSanitizeAiResponse("not json"), null);
  assert.equal(parseAndSanitizeAiResponse('{"reply":"   ","actions":[]}'), null);
});

test("buildRuleBasedReply handles accented and unaccented Vietnamese banking intents", () => {
  const ctx = {
    checkingBalance: 12_500_000,
    monthlyInflow: 20_000_000,
    monthlyOutflow: 7_500_000,
  };

  assert.equal(formatVndInteger(12_500_000), "12.500.000 ₫");

  const balanceReply = buildRuleBasedReply("Kiểm tra số dư tài khoản", ctx);
  assert.ok(balanceReply.reply.includes("12.500.000 ₫"));
  assert.ok(balanceReply.actions.some((a) => a.deepLink === "novapay://home"));

  const historyReply = buildRuleBasedReply("chi tieu thang nay", ctx);
  assert.ok(historyReply.reply.includes("7.500.000 ₫"));
  assert.ok(
    historyReply.actions.some((a) => a.deepLink === "novapay://transactions?range=month")
  );

  const branchReply = buildRuleBasedReply("Tìm cây ATM gần nhất", ctx);
  assert.ok(branchReply.actions.some((a) => a.deepLink === "novapay://branches"));

  const billReply = buildRuleBasedReply("thanh toán tiền điện", ctx);
  assert.ok(billReply.actions.some((a) => a.deepLink === "novapay://bills"));
});

test("generateChatReply uses Gemini response when valid and falls back on missing key, 429, or timeout", async () => {
  const ctx = { checkingBalance: 3_000_000, monthlyInflow: 5_000_000, monthlyOutflow: 2_000_000 };

  // Empty API key -> immediate rule-based fallback
  const noKeyReply = await generateChatReply("thanh toán tiền nước", ctx, { apiKey: "" });
  assert.ok(noKeyReply.actions.some((a) => a.deepLink === "novapay://bills"));

  // Valid Gemini JSON response
  const okReply = await generateChatReply("Tôi muốn chuyển tiền", ctx, {
    apiKey: "fake-key",
    fetchFn: (async () =>
      ({
        ok: true,
        status: 200,
        async json() {
          return {
            candidates: [
              {
                content: {
                  parts: [
                    {
                      text: JSON.stringify({
                        reply: "Bạn có thể chuyển tiền ngay tại mục Chuyển tiền.",
                        actions: [{ label: "Chuyển tiền", deepLink: "novapay://transfer" }],
                      }),
                    },
                  ],
                },
              },
            ],
          };
        },
      }) as Response) as typeof fetch,
  });
  assert.equal(okReply.reply, "Bạn có thể chuyển tiền ngay tại mục Chuyển tiền.");
  assert.deepEqual(okReply.actions, [{ label: "Chuyển tiền", deepLink: "novapay://transfer" }]);

  // HTTP 429 quota error -> rule-based fallback
  const rateLimitedReply = await generateChatReply("xem bản đồ chi nhánh", ctx, {
    apiKey: "fake-key",
    fetchFn: (async () =>
      ({
        ok: false,
        status: 429,
      }) as Response) as typeof fetch,
  });
  assert.ok(rateLimitedReply.actions.some((a) => a.deepLink === "novapay://branches"));

  // Network / AbortError timeout -> rule-based fallback
  const timeoutReply = await generateChatReply("nạp tiền điện thoại", ctx, {
    apiKey: "fake-key",
    fetchFn: (async () => {
      const err = new Error("Aborted");
      err.name = "AbortError";
      throw err;
    }) as typeof fetch,
  });
  assert.ok(timeoutReply.actions.some((a) => a.deepLink === "novapay://topup"));
});
