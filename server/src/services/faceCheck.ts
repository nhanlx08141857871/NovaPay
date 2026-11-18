import { config } from "../config";

const DEFAULT_FACE_CHECK_TIMEOUT_MS = 8_000;
const DEFAULT_CONFIDENCE_THRESHOLD = 0.75;

export interface FaceCheckResult {
  matched: boolean;
  manualAuditRequired: boolean;
  confidence: number | null;
  reason?: string;
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

/**
 * Parses Gemini structured JSON output `{ samePerson, confidence }` and applies confidence threshold.
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
  if (
    typeof parsed !== "object" ||
    parsed === null ||
    typeof parsed.samePerson !== "boolean" ||
    typeof parsed.confidence !== "number" ||
    !Number.isFinite(parsed.confidence)
  ) {
    throw new Error("Invalid Gemini FaceCheck JSON schema");
  }

  const confidence = parsed.confidence;
  const matched = parsed.samePerson === true && confidence >= confidenceThreshold;

  return {
    matched,
    manualAuditRequired: false,
    confidence,
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

      if (!cleanEkyc || !cleanLive) {
        return {
          matched: false,
          manualAuditRequired: false,
          confidence: 0,
          reason: "EMPTY_IMAGE",
        };
      }

      if (!apiKey) {
        return {
          matched: true,
          manualAuditRequired: true,
          confidence: null,
          reason: "GEMINI_API_KEY_MISSING",
        };
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
            contents: [
              {
                parts: [
                  {
                    text: [
                      "Compare the two portrait photos (first: registered eKYC reference, second: live capture).",
                      "Determine whether both photos depict the exact same person.",
                      'Return ONLY JSON matching {"samePerson": boolean, "confidence": number} where confidence is in [0, 1].',
                    ].join(" "),
                  },
                  {
                    inline_data: {
                      mime_type: "image/jpeg",
                      data: cleanEkyc,
                    },
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
              temperature: 0.1,
            },
          }),
          signal: controller.signal,
        });

        if (response.status === 429) {
          return {
            matched: true,
            manualAuditRequired: true,
            confidence: null,
            reason: "GEMINI_QUOTA_EXCEEDED",
          };
        }

        if (!response.ok) {
          return {
            matched: true,
            manualAuditRequired: true,
            confidence: null,
            reason: `GEMINI_HTTP_${response.status}`,
          };
        }

        const payload = (await response.json()) as GeminiApiResponse;
        const textParts =
          payload.candidates?.[0]?.content?.parts
            ?.map((part) => part.text ?? "")
            .filter((part) => part.length > 0) ?? [];
        const rawText = textParts.join("\n").trim();

        if (!rawText) {
          return {
            matched: true,
            manualAuditRequired: true,
            confidence: null,
            reason: "GEMINI_EMPTY_RESPONSE",
          };
        }

        return parseGeminiFaceCheckResponse(rawText, confidenceThreshold);
      } catch (err) {
        const isAbort =
          err instanceof Error && (err.name === "AbortError" || err.message.includes("aborted"));
        return {
          matched: true,
          manualAuditRequired: true,
          confidence: null,
          reason: isAbort ? "GEMINI_TIMEOUT" : "GEMINI_ERROR",
        };
      } finally {
        clearTimeout(timer);
      }
    },
  };
}

export const faceCheckService = createFaceCheckService();
