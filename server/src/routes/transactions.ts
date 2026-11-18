import { Router } from "express";
import { requireAuth, sendApiError } from "../middleware/auth";

/**
 * Động cơ điều phối Giao dịch, OTP & FaceCheck (Owner: Thành viên B — triển khai ở Giai đoạn 3B).
 */
export const transactionsRouter = Router();

transactionsRouter.post("/initiate", requireAuth, (_req, res) => {
  sendApiError(res, 501, "NOT_IMPLEMENTED", "Endpoint POST /transactions/initiate sẽ hoàn thiện ở Giai đoạn 3B (Member B).");
});

transactionsRouter.post("/face-check", requireAuth, (_req, res) => {
  sendApiError(res, 501, "NOT_IMPLEMENTED", "Endpoint POST /transactions/face-check sẽ hoàn thiện ở Giai đoạn 3B (Member B).");
});

transactionsRouter.post("/confirm", requireAuth, (_req, res) => {
  sendApiError(res, 501, "NOT_IMPLEMENTED", "Endpoint POST /transactions/confirm sẽ hoàn thiện ở Giai đoạn 3B (Member B).");
});

transactionsRouter.post("/resend-otp", requireAuth, (_req, res) => {
  sendApiError(res, 501, "NOT_IMPLEMENTED", "Endpoint POST /transactions/resend-otp sẽ hoàn thiện ở Giai đoạn 3B (Member B).");
});
