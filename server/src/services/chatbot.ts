import admin from "firebase-admin";
import { config } from "../config";

export interface ChatAction {
  label: string;
  deepLink: string;
}

export interface ChatReply {
  reply: string;
  actions: ChatAction[];
}

export interface CustomerFinancialContext {
  checkingBalance: number;
  monthlyInflow: number;
  monthlyOutflow: number;
}

export const ALLOWED_CHAT_DEEPLINKS = new Set<string>([
  "novapay://home",
  "novapay://transactions?range=month",
  "novapay://profile",
  "novapay://ekyc",
  "novapay://transfer",
  "novapay://bills",
  "novapay://topup",
  "novapay://services",
  "novapay://branches",
]);

const VIETNAM_UTC_OFFSET_MS = 7 * 60 * 60 * 1000;
const GEMINI_TIMEOUT_MS = 8_000;
const MAX_CHAT_MESSAGE_LENGTH = 2_000;
const MAX_CHAT_ACTIONS = 3;

export function validateChatMessageInput(
  body: unknown
): { ok: true; message: string } | { ok: false; errorMessage: string } {
  if (!body || typeof body !== "object") {
    return { ok: false, errorMessage: "Dữ liệu tin nhắn không hợp lệ." };
  }
  const rawMessage = (body as { message?: unknown }).message;
  if (typeof rawMessage !== "string") {
    return { ok: false, errorMessage: "Trường message phải là chuỗi ký tự." };
  }
  const trimmed = rawMessage.trim();
  if (trimmed.length === 0) {
    return { ok: false, errorMessage: "Nội dung tin nhắn không được để trống." };
  }
  if (trimmed.length > MAX_CHAT_MESSAGE_LENGTH) {
    return {
      ok: false,
      errorMessage: `Tin nhắn vượt quá giới hạn ${MAX_CHAT_MESSAGE_LENGTH} ký tự.`,
    };
  }
  return { ok: true, message: trimmed };
}

export function getVietnamMonthStartMillis(nowMs: number = Date.now()): number {
  const vnNow = new Date(nowMs + VIETNAM_UTC_OFFSET_MS);
  const vnMonthStartUtc = Date.UTC(
    vnNow.getUTCFullYear(),
    vnNow.getUTCMonth(),
    1,
    0,
    0,
    0,
    0
  );
  return vnMonthStartUtc - VIETNAM_UTC_OFFSET_MS;
}

export function formatVndInteger(amount: number): string {
  const safeInt = Math.trunc(Number.isFinite(amount) ? amount : 0);
  const absFormatted = Math.abs(safeInt)
    .toString()
    .replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  return safeInt < 0 ? `-${absFormatted} ₫` : `${absFormatted} ₫`;
}

export function normalizeVietnameseQuery(input: string): string {
  return input
    .toLowerCase()
    .replace(/đ/g, "d")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/\s+/g, " ")
    .trim();
}

export function parseAndSanitizeAiResponse(rawText: string): ChatReply | null {
  if (typeof rawText !== "string" || rawText.trim().length === 0) {
    return null;
  }

  let cleaned = rawText.trim();
  if (cleaned.startsWith("```")) {
    cleaned = cleaned
      .replace(/^```(?:json)?\s*/i, "")
      .replace(/\s*```$/i, "")
      .trim();
  }

  let parsed: unknown;
  try {
    parsed = JSON.parse(cleaned);
  } catch {
    return null;
  }

  if (!parsed || typeof parsed !== "object") {
    return null;
  }

  const obj = parsed as { reply?: unknown; actions?: unknown };
  if (typeof obj.reply !== "string" || obj.reply.trim().length === 0) {
    return null;
  }

  const sanitizedActions: ChatAction[] = [];
  if (Array.isArray(obj.actions)) {
    for (const candidate of obj.actions) {
      if (sanitizedActions.length >= MAX_CHAT_ACTIONS) {
        break;
      }
      if (!candidate || typeof candidate !== "object") {
        continue;
      }
      const item = candidate as { label?: unknown; deepLink?: unknown };
      if (
        typeof item.label === "string" &&
        item.label.trim().length > 0 &&
        typeof item.deepLink === "string" &&
        ALLOWED_CHAT_DEEPLINKS.has(item.deepLink.trim())
      ) {
        sanitizedActions.push({
          label: item.label.trim(),
          deepLink: item.deepLink.trim(),
        });
      }
    }
  }

  return {
    reply: obj.reply.trim(),
    actions: sanitizedActions,
  };
}

export function buildRuleBasedReply(
  message: string,
  context: CustomerFinancialContext
): ChatReply {
  const q = normalizeVietnameseQuery(message);
  const balanceText = formatVndInteger(context.checkingBalance);
  const inflowText = formatVndInteger(context.monthlyInflow);
  const outflowText = formatVndInteger(context.monthlyOutflow);

  if (
    q.includes("so du") ||
    q.includes("tai khoan") ||
    q.includes("checking") ||
    q.includes("bao nhieu tien") ||
    q.includes("con bao nhieu")
  ) {
    return {
      reply: `Số dư khả dụng hiện tại trên tài khoản thanh toán (CHECKING) của bạn là ${balanceText}. Tổng tiền vào trong tháng này là ${inflowText} và tổng chi tiêu là ${outflowText}.`,
      actions: [
        { label: "Về trang chủ", deepLink: "novapay://home" },
        { label: "Chuyển tiền ngay", deepLink: "novapay://transfer" },
      ],
    };
  }

  if (
    q.includes("lich su") ||
    q.includes("giao dich") ||
    q.includes("chi tieu") ||
    q.includes("thu chi") ||
    q.includes("thang nay") ||
    q.includes("bien dong")
  ) {
    return {
      reply: `Trong tháng này (tính theo giờ Việt Nam), tổng thu của bạn là ${inflowText} và tổng chi là ${outflowText}. Bạn có thể xem chi tiết biến động số dư tại mục Lịch sử giao dịch.`,
      actions: [
        { label: "Lịch sử giao dịch tháng", deepLink: "novapay://transactions?range=month" },
        { label: "Về trang chủ", deepLink: "novapay://home" },
      ],
    };
  }

  if (
    q.includes("chuyen tien") ||
    q.includes("chuyen khoan") ||
    q.includes("gui tien") ||
    q.includes("qr")
  ) {
    return {
      reply: `Bạn có thể chuyển tiền nội bộ hoặc liên ngân hàng nhanh chóng (số dư thanh toán hiện có: ${balanceText}). Giao dịch trên 5.000.000 ₫ sẽ được bảo vệ thêm bằng FaceCheck, sinh trắc học và OTP Email.`,
      actions: [
        { label: "Mở Chuyển tiền", deepLink: "novapay://transfer" },
        { label: "Tìm ATM/Chi nhánh", deepLink: "novapay://branches" },
      ],
    };
  }

  if (
    q.includes("nap tien") ||
    q.includes("dien thoai") ||
    q.includes("nap card") ||
    q.includes("topup") ||
    q.includes("viettel") ||
    q.includes("mobifone") ||
    q.includes("vinaphone")
  ) {
    return {
      reply: "Bạn có thể nạp tiền điện thoại các nhà mạng ngay trong mục Nạp điện thoại của NovaPay.",
      actions: [{ label: "Nạp điện thoại", deepLink: "novapay://topup" }],
    };
  }

  if (
    q.includes("hoa don") ||
    q.includes("tien dien") ||
    q.includes("tien nuoc") ||
    q.includes("dien nuoc") ||
    q.includes("thanh toan dien") ||
    q.includes("thanh toan nuoc")
  ) {
    return {
      reply: "NovaPay hỗ trợ tra cứu và thanh toán hóa đơn Điện / Nước trực tiếp từ tài khoản thanh toán CHECKING.",
      actions: [
        { label: "Thanh toán hóa đơn", deepLink: "novapay://bills" },
        { label: "Lịch sử giao dịch", deepLink: "novapay://transactions?range=month" },
      ],
    };
  }

  if (
    q.includes("ve may bay") ||
    q.includes("xem phim") ||
    q.includes("khach san") ||
    q.includes("mua sam") ||
    q.includes("dat ve") ||
    q.includes("dich vu")
  ) {
    return {
      reply: "NovaPay cung cấp tiện ích đặt vé máy bay, vé xem phim, phòng khách sạn và mua sắm trực tuyến ngay trên ứng dụng.",
      actions: [{ label: "Đặt dịch vụ & Vé", deepLink: "novapay://services" }],
    };
  }

  if (
    q.includes("chi nhanh") ||
    q.includes("atm") ||
    q.includes("ban do") ||
    q.includes("chi duong") ||
    q.includes("gan nhat")
  ) {
    return {
      reply: "Bạn có thể xem bản đồ các chi nhánh và cây ATM NovaPay gần nhất kèm chỉ đường đi bộ trên bản đồ.",
      actions: [{ label: "Mở Bản đồ chi nhánh", deepLink: "novapay://branches" }],
    };
  }

  if (
    q.includes("ekyc") ||
    q.includes("xac minh") ||
    q.includes("khuon mat") ||
    q.includes("ho so")
  ) {
    return {
      reply: "Để thực hiện giao dịch chi/rút, tài khoản cần hoàn tất định danh khuôn mặt eKYC. Bạn có thể kiểm tra hồ sơ hoặc chụp ảnh eKYC ngay.",
      actions: [
        { label: "Định danh eKYC", deepLink: "novapay://ekyc" },
        { label: "Hồ sơ cá nhân", deepLink: "novapay://profile" },
      ],
    };
  }

  return {
    reply: `Xin chào Quý khách! Tôi là trợ lý chính thức của Ngân hàng số NovaBank / NovaPay. Số dư tài khoản thanh toán của Quý khách hiện là ${balanceText} (Tổng chi tiêu tháng này: ${outflowText}). Tôi có thể hỗ trợ chuyển tiền, thanh toán hóa đơn, nạp điện thoại hoặc tra cứu chi nhánh gần nhất.`,
    actions: [
      { label: "Chuyển tiền", deepLink: "novapay://transfer" },
      { label: "Thanh toán hóa đơn", deepLink: "novapay://bills" },
      { label: "Lịch sử tháng", deepLink: "novapay://transactions?range=month" },
    ],
  };
}

export async function getCustomerFinancialContext(
  db: FirebaseFirestore.Firestore,
  uid: string,
  nowMs: number = Date.now()
): Promise<CustomerFinancialContext> {
  try {
    const accountsSnap = await db
      .collection("accounts")
      .where("ownerUid", "==", uid)
      .get();

    let checkingBalance = 0;
    let monthlyInflow = 0;
    let monthlyOutflow = 0;
    const monthStartTs = admin.firestore.Timestamp.fromMillis(
      getVietnamMonthStartMillis(nowMs)
    );

    for (const accDoc of accountsSnap.docs) {
      const accData = accDoc.data() || {};
      if (accData.type === "MORTGAGE") {
        continue;
      }
      if (accData.type === "CHECKING" && typeof accData.balance === "number") {
        checkingBalance += Math.trunc(accData.balance);
      }

      const ledgerSnap = await accDoc.ref
        .collection("ledger")
        .where("createdAt", ">=", monthStartTs)
        .get();

      for (const entryDoc of ledgerSnap.docs) {
        const entry = entryDoc.data() || {};
        const amount =
          typeof entry.amount === "number" ? Math.trunc(entry.amount) : 0;
        if (entry.direction === "IN") {
          monthlyInflow += amount;
        } else if (entry.direction === "OUT") {
          monthlyOutflow += amount;
        }
      }
    }

    return { checkingBalance, monthlyInflow, monthlyOutflow };
  } catch (err) {
    console.warn("[NovaPay Chatbot] Lỗi đọc ngữ cảnh tài chính:", err);
    return { checkingBalance: 0, monthlyInflow: 0, monthlyOutflow: 0 };
  }
}

export interface GenerateChatReplyOptions {
  apiKey?: string;
  model?: string;
  fetchFn?: typeof fetch;
  timeoutMs?: number;
  history?: Array<{ role: "USER" | "BOT"; text: string }>;
}

export const CHAT_HISTORY_LIMIT = 6;

export function buildGeminiContents(
  message: string,
  history: Array<{ role: "USER" | "BOT"; text: string }> = []
): Array<{ role: "user" | "model"; parts: Array<{ text: string }> }> {
  const contents: Array<{ role: "user" | "model"; parts: Array<{ text: string }> }> = [];
  for (const item of history.slice(-CHAT_HISTORY_LIMIT)) {
    const text = typeof item.text === "string" ? item.text.trim() : "";
    if (!text) continue;
    const role = item.role === "BOT" ? "model" : "user";
    if (contents.length === 0 && role === "model") continue;
    if (contents.length > 0 && contents[contents.length - 1].role === role) {
      contents[contents.length - 1].parts[0].text += `\n${text}`;
      continue;
    }
    contents.push({ role, parts: [{ text }] });
  }
  if (contents.length > 0 && contents[contents.length - 1].role === "user") {
    contents[contents.length - 1].parts[0].text += `\n${message}`;
  } else {
    contents.push({ role: "user", parts: [{ text: message }] });
  }
  return contents;
}

export async function generateChatReply(
  message: string,
  context: CustomerFinancialContext,
  options: GenerateChatReplyOptions = {}
): Promise<ChatReply> {
  const apiKey = (options.apiKey ?? config.geminiApiKey).trim();
  if (!apiKey) {
    return buildRuleBasedReply(message, context);
  }

  const model = (options.model ?? config.geminiModel).trim() || "gemini-2.5-flash";
  const fetchImpl = options.fetchFn ?? fetch;
  const timeoutMs = options.timeoutMs ?? GEMINI_TIMEOUT_MS;

  const systemPrompt = [
    "Bạn là Trợ lý Ngân hàng số chính thức của Ngân hàng số NovaBank / NovaPay tại Việt Nam. Xưng 'tôi' và gọi khách hàng là 'Quý khách' hoặc 'bạn' với phong thái chuyên nghiệp, lịch sự, chuẩn mực ngân hàng thương mại.",
    "Tuyệt đối KHÔNG bao giờ tự nhận NovaBank / NovaPay là ứng dụng mô phỏng, đồ án sinh viên, bài tập môn học, bản demo hay môi trường sandbox/test.",
    `Dữ liệu tài chính thời gian thực của khách hàng: Số dư CHECKING = ${formatVndInteger(context.checkingBalance)}; Tổng thu tháng này = ${formatVndInteger(context.monthlyInflow)}; Tổng chi tháng này = ${formatVndInteger(context.monthlyOutflow)}.`,
    "Chỉ trả về đúng 1 JSON object theo mẫu {\"reply\": \"...\", \"actions\": [{\"label\": \"...\", \"deepLink\": \"...\"}]}.",
    "Danh sách deepLink hợp lệ duy nhất: novapay://home, novapay://transactions?range=month, novapay://profile, novapay://ekyc, novapay://transfer, novapay://bills, novapay://topup, novapay://services, novapay://branches.",
    "Không tự bịa số tiền ngoài dữ liệu được cung cấp. Tối đa 3 actions.",
  ].join("\n");

  const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(
    model
  )}:generateContent`;

  const controller = new AbortController();
  const timeoutHandle = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const response = await fetchImpl(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-goog-api-key": apiKey,
      },
      body: JSON.stringify({
        systemInstruction: {
          parts: [{ text: systemPrompt }],
        },
        contents: buildGeminiContents(message, options.history),
        generationConfig: {
          temperature: 0.3,
          responseMimeType: "application/json",
        },
      }),
      signal: controller.signal,
    });

    if (!response.ok) {
      return buildRuleBasedReply(message, context);
    }

    const payload = (await response.json()) as {
      candidates?: Array<{
        content?: {
          parts?: Array<{ text?: string }>;
        };
      }>;
    };

    const rawCandidateText = payload?.candidates?.[0]?.content?.parts?.[0]?.text ?? "";
    const parsed = parseAndSanitizeAiResponse(rawCandidateText);
    return parsed ?? buildRuleBasedReply(message, context);
  } catch {
    return buildRuleBasedReply(message, context);
  } finally {
    clearTimeout(timeoutHandle);
  }
}

export class ChatbotAccessError extends Error {
  public readonly statusCode: number;
  public readonly errorCode: string;

  constructor(statusCode: number, errorCode: string, message: string) {
    super(message);
    this.name = "ChatbotAccessError";
    this.statusCode = statusCode;
    this.errorCode = errorCode;
  }
}

export async function handleChatAndPersist(
  uid: string,
  message: string,
  deps: {
    db?: FirebaseFirestore.Firestore;
    nowMs?: () => number;
    chatOptions?: GenerateChatReplyOptions;
  } = {}
): Promise<ChatReply> {
  const db = deps.db ?? admin.firestore();
  const nowMs = (deps.nowMs ?? Date.now)();

  const userSnap = await db.collection("users").doc(uid).get();
  if (userSnap.exists && userSnap.data()?.status === "LOCKED") {
    throw new ChatbotAccessError(
      403,
      "PERMISSION_DENIED",
      "Tài khoản người dùng hiện đang bị khóa."
    );
  }

  const context = await getCustomerFinancialContext(db, uid, nowMs);
  const messagesCol = db.collection("chat_sessions").doc(uid).collection("messages");

  let history: Array<{ role: "USER" | "BOT"; text: string }> = [];
  try {
    const recent = await messagesCol.orderBy("createdAt", "desc").limit(CHAT_HISTORY_LIMIT).get();
    history = recent.docs
      .map((doc) => doc.data() as { role?: string; text?: string })
      .filter((d) => (d.role === "USER" || d.role === "BOT") && typeof d.text === "string")
      .map((d) => ({ role: d.role as "USER" | "BOT", text: d.text as string }))
      .reverse();
  } catch {
    history = [];
  }

  const result = await generateChatReply(message, context, { ...deps.chatOptions, history });
  const batch = db.batch();

  batch.set(messagesCol.doc(), {
    role: "USER",
    text: message,
    actions: [],
    createdAt: admin.firestore.Timestamp.fromMillis(nowMs),
  });

  batch.set(messagesCol.doc(), {
    role: "BOT",
    text: result.reply,
    actions: result.actions,
    createdAt: admin.firestore.Timestamp.fromMillis(nowMs + 1),
  });

  await batch.commit();
  return result;
}
