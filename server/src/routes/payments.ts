import { Router } from "express";
import { requireAuth, sendApiError } from "../middleware/auth";

/**
 * Xác nhận hoàn tất thanh toán cổng Stripe Idempotent (Owner: Thành viên B — triển khai ở Giai đoạn 3B).
 */
export const paymentsRouter = Router();

paymentsRouter.post("/finalize", requireAuth, (_req, res) => {
  sendApiError(res, 501, "NOT_IMPLEMENTED", "Endpoint POST /payments/finalize sẽ hoàn thiện ở Giai đoạn 3B (Member B).");
});
