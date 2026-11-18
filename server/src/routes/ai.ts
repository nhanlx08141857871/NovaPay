import { Router } from "express";
import {
  AuthenticatedRequest,
  requireAuth,
  sendApiError,
} from "../middleware/auth";
import {
  handleChatAndPersist,
  validateChatMessageInput,
} from "../services/chatbot";

/**
 * Trợ lý ảo AI Chatbot tích hợp Gemini & Rule-based NLP Fallback (Owner: Thành viên C).
 */
export const aiRouter = Router();

aiRouter.post("/chat", requireAuth, async (req: AuthenticatedRequest, res) => {
  const uid = req.user?.uid;
  if (!uid) {
    sendApiError(res, 401, "PERMISSION_DENIED", "Thiếu thông tin người dùng xác thực.");
    return;
  }

  const validation = validateChatMessageInput(req.body);
  if (!validation.ok) {
    sendApiError(res, 400, "INVALID_ARGUMENT", validation.errorMessage);
    return;
  }

  try {
    const reply = await handleChatAndPersist(uid, validation.message);
    res.status(200).json(reply);
  } catch (err) {
    console.error("[NovaPay AI] POST /ai/chat error:", err);
    sendApiError(res, 500, "INTERNAL_ERROR", "Không thể xử lý yêu cầu trợ lý AI lúc này.");
  }
});
