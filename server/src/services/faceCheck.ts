import { config } from "../config";

export const DEFAULT_FACE_CHECK_TIMEOUT_MS = 15_000;
export const DEFAULT_CONFIDENCE_THRESHOLD = 0.8;

/**
 * Ảnh JPEG 1x1 pixel mẫu được sinh mặc định bởi `seed.ts` khi tài khoản chưa chụp ảnh khuôn mặt thật.
 * Tuyệt đối không coi ảnh 1x1 pixel này là ảnh khuôn mặt gốc hợp lệ khi xác thực FaceCheck.
 */
export const SAMPLE_SEED_PLACEHOLDER_BASE64 = Buffer.from([
  0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10, 0x4a, 0x46, 0x49, 0x46, 0x00, 0x01, 0x01, 0x01, 0x00, 0x48,
  0x00, 0x48, 0x00, 0x00, 0xff, 0xdb, 0x00, 0x43, 0x00, 0x03, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03,
  0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x03, 0x04, 0x06, 0x04, 0x04, 0x04, 0x04, 0x04, 0x08, 0x06,
  0x06, 0x05, 0x06, 0x09, 0x08, 0x0a, 0x0a, 0x09, 0x08, 0x09, 0x09, 0x0a, 0x0c, 0x0f, 0x0c, 0x0a,
  0x0b, 0x0e, 0x0b, 0x09, 0x09, 0x0d, 0x11, 0x0d, 0x0e, 0x0f, 0x10, 0x10, 0x11, 0x10, 0x0a, 0x0c,
  0x12, 0x13, 0x12, 0x10, 0x13, 0x0f, 0x10, 0x10, 0x10, 0xff, 0xc0, 0x00, 0x0b, 0x08, 0x00, 0x01,
  0x00, 0x01, 0x01, 0x01, 0x11, 0x00, 0xff, 0xc4, 0x00, 0x14, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00,
  0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x09, 0xff, 0xc4, 0x00, 0x14,
  0x10, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
  0x00, 0x00, 0xff, 0xda, 0x00, 0x08, 0x01, 0x01, 0x00, 0x00, 0x3f, 0x00, 0xd2, 0xcf, 0x20, 0xff,
  0xd9,
]).toString("base64");

const FACE_CHECK_SYSTEM_PROMPT = [
  "Bạn là chuyên gia thẩm định sinh trắc học khuôn mặt cấp cao của ngân hàng số NovaPay (Bank Biometric Verification Specialist).",
  "Nhiệm vụ của bạn là đối chiếu nghiêm ngặt Ảnh 1 (ảnh chân dung hồ sơ eKYC gốc đã đăng ký) và Ảnh 2 (ảnh chụp khuôn mặt trực tiếp khi xác thực giao dịch giá trị cao > 5.000.000 VNĐ) theo nguyên tắc Zero-False-Acceptance (ưu tiên an toàn tuyệt đối, tuyệt đối không xác nhận nhầm người khác).",
  "Quy trình phân tích bắt buộc:",
  "1. Kiểm tra tính hợp lệ của cả hai ảnh: Nếu bất kỳ ảnh nào không có khuôn mặt người thật rõ ràng, bị che khuất mắt/mũi/miệng, quá mờ, chụp lại qua màn hình khác, hoặc có nhiều khuôn mặt -> bắt buộc trả về \"matched\": false và \"confidence\": 0.0.",
  "2. Đối chiếu chi tiết cấu trúc giải phẫu khuôn mặt (craniofacial landmarks): khoảng cách hai đồng tử và hình dáng hốc mắt/chân mày, độ cao và độ rộng sống mũi/cánh mũi, xương gò má, đường viền khuôn hàm và cằm, tỷ lệ ba tầng mặt (trán - mũi - cằm), giới tính sinh học và nhóm độ tuổi.",
  "3. Tiêu chuẩn kết luận nghiêm ngặt:",
  "   - CHỈ trả về \"matched\": true khi cấu trúc giải phẫu khuôn mặt trùng khớp ở mức cao (confidence >= 0.80) và chắc chắn 100% là CÙNG MỘT NGƯỜI.",
  "   - Nếu là người khác (kể cả anh chị em ruột, người có kiểu tóc/kính mắt tương tự, hoặc khác giới tính/độ tuổi), hoặc có bất kỳ dấu hiệu nghi ngờ nào về khác biệt cấu trúc xương mặt -> BẮT BUỘC trả về \"matched\": false kèm lý do tiếng Việt ngắn gọn, rõ ràng trong trường \"reason\".",
  "4. Chỉ trả về DUY NHẤT một đối tượng JSON hợp lệ theo đúng cấu trúc: {\"matched\": boolean, \"confidence\": number, \"reason\": \"string\"} trong đó confidence thuộc đoạn [0.0, 1.0].",
].join("\n");

export interface FaceCheckResult {
  matched: boolean;
  manualAuditRequired: boolean;
  confidence: number | null;
  reason?: string;
}

export class EkycFaceNotFoundError extends Error {
  readonly statusCode = 403;
  readonly code = "EKYC_FACE_NOT_FOUND";

  constructor(
    message = "Tài khoản chưa có ảnh khuôn mặt gốc trong hồ sơ eKYC. Vui lòng cập nhật ảnh định danh eKYC trước khi thực hiện giao dịch."
  ) {
    super(message);
    this.name = "EkycFaceNotFoundError";
  }
}

export class FaceCheckUnavailableError extends Error {
  readonly statusCode = 503;
  readonly code = "SERVICE_UNAVAILABLE";
  readonly reasonCode: string;

  constructor(
    reasonCode: string,
    message = "Dịch vụ xác thực sinh trắc học khuôn mặt AI tạm thời gián đoạn hoặc quá tải. Vui lòng thử lại sau ít phút."
  ) {
    super(message);
    this.name = "FaceCheckUnavailableError";
    this.reasonCode = reasonCode;
  }
}

export interface FaceCheckServiceDeps {
  fetchFn?: typeof fetch;
  apiKey?: string;
  model?: string;
  timeoutMs?: number;
  confidenceThreshold?: number;
}

export interface FaceCheckService {
  verifyFaceMatch(ekycImageBase64: string, liveImageBase64: string): Promise<FaceCheckResult>;
}

/**
 * Strips an optional `data:image/...;base64,` prefix and surrounding whitespace.
 */
export function stripBase64DataUrlPrefix(input: string): string {
  const trimmed = input.trim();
  const match = /^data:image\/[a-zA-Z0-9.+-]+;base64,/i.exec(trimmed);
  return match ? trimmed.slice(match[0].length).trim() : trimmed;
}

export function isPlaceholderOrEmptyEkycImage(rawBase64: string): boolean {
  const cleaned = stripBase64DataUrlPrefix(rawBase64);
  return !cleaned || cleaned === SAMPLE_SEED_PLACEHOLDER_BASE64;
}

/**
 * Parses Gemini structured JSON output `{"matched": boolean, "confidence": number, "reason"?: string}`
 * (also tolerates legacy `samePerson` boolean key) and enforces `confidence >= confidenceThreshold` (default 0.80).
 */
export function parseGeminiFaceCheckResponse(
  rawText: string,
  confidenceThreshold = DEFAULT_CONFIDENCE_THRESHOLD
): FaceCheckResult {
  const cleaned = rawText
    .trim()
    .replace(/^```(?:json)?\s*/i, "")
    .replace(/\s*```$/i, "")
    .trim();

  const parsed = JSON.parse(cleaned) as Record<string, unknown>;
  if (typeof parsed !== "object" || parsed === null) {
    throw new Error("Invalid Gemini FaceCheck JSON payload");
  }

  const rawMatched =
    typeof parsed.matched === "boolean"
      ? parsed.matched
      : typeof parsed.samePerson === "boolean"
        ? parsed.samePerson
        : null;

  if (
    rawMatched === null ||
    typeof parsed.confidence !== "number" ||
    !Number.isFinite(parsed.confidence)
  ) {
    throw new Error("Invalid Gemini FaceCheck JSON schema");
  }

  const confidence = Math.max(0, Math.min(1, parsed.confidence));
  const matched = rawMatched === true && confidence >= confidenceThreshold;
  const reason =
    typeof parsed.reason === "string" && parsed.reason.trim().length > 0
      ? parsed.reason.trim()
      : undefined;

  return {
    matched,
    manualAuditRequired: false,
    confidence,
    reason,
  };
}

interface GeminiApiResponse {
  candidates?: Array<{
    content?: {
      parts?: Array<{
        text?: string;
      }>;
    };
  }>;
}

export function createFaceCheckService(deps?: FaceCheckServiceDeps): FaceCheckService {
  const fetchFn = deps?.fetchFn ?? fetch;
  const apiKey = (deps?.apiKey ?? config.geminiApiKey).trim();
  const model = (deps?.model ?? config.geminiModel).trim() || "gemini-2.5-flash";
  const timeoutMs = deps?.timeoutMs ?? DEFAULT_FACE_CHECK_TIMEOUT_MS;
  const confidenceThreshold = deps?.confidenceThreshold ?? DEFAULT_CONFIDENCE_THRESHOLD;

  return {
    async verifyFaceMatch(
      ekycImageBase64: string,
      liveImageBase64: string
    ): Promise<FaceCheckResult> {
      const cleanEkyc = stripBase64DataUrlPrefix(ekycImageBase64);
      const cleanLive = stripBase64DataUrlPrefix(liveImageBase64);

      if (isPlaceholderOrEmptyEkycImage(cleanEkyc)) {
        throw new EkycFaceNotFoundError();
      }

      if (!cleanLive) {
        return {
          matched: false,
          manualAuditRequired: false,
          confidence: 0,
          reason: "Ảnh chụp khuôn mặt trực tiếp trống hoặc không hợp lệ.",
        };
      }

      if (!apiKey) {
        throw new FaceCheckUnavailableError(
          "GEMINI_API_KEY_MISSING",
          "Dịch vụ xác thực sinh trắc học AI chưa được cấu hình khóa kết nối trên máy chủ."
        );
      }

      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), timeoutMs);

      const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(
        model
      )}:generateContent`;

      try {
        const response = await fetchFn(endpoint, {
          method: "POST",
          headers: {
            "content-type": "application/json",
            "x-goog-api-key": apiKey,
          },
          body: JSON.stringify({
            systemInstruction: {
              parts: [{ text: FACE_CHECK_SYSTEM_PROMPT }],
            },
            contents: [
              {
                role: "user",
                parts: [
                  {
                    text: "Ảnh 1 (Hồ sơ gốc): Ảnh chân dung định danh eKYC đã đăng ký của chủ tài khoản.",
                  },
                  {
                    inline_data: {
                      mime_type: "image/jpeg",
                      data: cleanEkyc,
                    },
                  },
                  {
                    text: "Ảnh 2 (Chụp trực tiếp): Ảnh khuôn mặt người thực hiện giao dịch ngay lúc này. Hãy đối chiếu nghiêm ngặt cấu trúc khuôn mặt giữa Ảnh 1 và Ảnh 2.",
                  },
                  {
                    inline_data: {
                      mime_type: "image/jpeg",
                      data: cleanLive,
                    },
                  },
                ],
              },
            ],
            generationConfig: {
              responseMimeType: "application/json",
              temperature: 0.0,
            },
          }),
          signal: controller.signal,
        });

        if (response.status === 429) {
          throw new FaceCheckUnavailableError(
            "GEMINI_QUOTA_EXCEEDED",
            "Hệ thống xác thực khuôn mặt AI đang quá tải hạn mức truy vấn. Vui lòng thử lại sau ít phút."
          );
        }

        if (!response.ok) {
          throw new FaceCheckUnavailableError(
            `GEMINI_HTTP_${response.status}`,
            "Dịch vụ xác thực khuôn mặt AI tạm thời gián đoạn. Vui lòng thử lại sau."
          );
        }

        const payload = (await response.json()) as GeminiApiResponse;
        const textParts =
          payload.candidates?.[0]?.content?.parts
            ?.map((part) => part.text ?? "")
            .filter((part) => part.length > 0) ?? [];
        const rawText = textParts.join("\n").trim();

        if (!rawText) {
          throw new FaceCheckUnavailableError(
            "GEMINI_EMPTY_RESPONSE",
            "Không nhận được kết quả thẩm định từ dịch vụ sinh trắc học AI. Vui lòng thử lại."
          );
        }

        try {
          return parseGeminiFaceCheckResponse(rawText, confidenceThreshold);
        } catch {
          throw new FaceCheckUnavailableError(
            "GEMINI_INVALID_JSON",
            "Phản hồi từ dịch vụ thẩm định khuôn mặt AI không hợp lệ. Vui lòng thử lại."
          );
        }
      } catch (err) {
        if (err instanceof FaceCheckUnavailableError || err instanceof EkycFaceNotFoundError) {
          throw err;
        }
        const isAbort =
          err instanceof Error && (err.name === "AbortError" || err.message.includes("aborted"));
        throw new FaceCheckUnavailableError(
          isAbort ? "GEMINI_TIMEOUT" : "GEMINI_NETWORK_ERROR",
          isAbort
            ? "Quá thời gian chờ đối chiếu khuôn mặt AI. Vui lòng kiểm tra kết nối mạng và thử lại."
            : "Không thể kết nối dịch vụ đối chiếu khuôn mặt AI lúc này. Vui lòng thử lại sau."
        );
      } finally {
        clearTimeout(timer);
      }
    },
  };
}

export const faceCheckService = createFaceCheckService();
