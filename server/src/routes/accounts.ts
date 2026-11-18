import { Router } from "express";
import { requireAuth, sendApiError } from "../middleware/auth";

/**
 * Tra cứu tên người nhận nội bộ an toàn (Owner: Thành viên B — triển khai ở Giai đoạn 3B).
 */
export const accountsRouter = Router();

accountsRouter.get("/lookup", requireAuth, (_req, res) => {
  sendApiError(res, 501, "NOT_IMPLEMENTED", "Endpoint GET /accounts/lookup sẽ hoàn thiện ở Giai đoạn 3B (Member B).");
});
