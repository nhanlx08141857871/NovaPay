import { Router } from "express";
import { requireAuth, sendApiError } from "../middleware/auth";

/**
 * Trợ lý ảo AI Chatbot tích hợp Gemini & Rule-based NLP Fallback (Owner: Thành viên C — triển khai ở Giai đoạn 3B).
 */
export const aiRouter = Router();

aiRouter.post("/chat", requireAuth, (_req, res) => {
  sendApiError(res, 501, "NOT_IMPLEMENTED", "Endpoint POST /ai/chat sẽ hoàn thiện ở Giai đoạn 3B (Member C).");
});
