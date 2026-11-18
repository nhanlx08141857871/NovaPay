import { Router } from "express";
import { requireAuth, requireOfficer, sendApiError } from "../middleware/auth";

/**
 * Phân hệ Quản trị Nhân viên Ngân hàng & Audit Logs (Owner: Thành viên C — triển khai ở Giai đoạn 3B).
 */
export const officerRouter = Router();

officerRouter.use(requireAuth, requireOfficer);

officerRouter.post("/customers", (_req, res) => {
  sendApiError(res, 501, "NOT_IMPLEMENTED", "Endpoint POST /officer/customers sẽ hoàn thiện ở Giai đoạn 3B (Member C).");
});

officerRouter.patch("/customers/:uid", (_req, res) => {
  sendApiError(res, 501, "NOT_IMPLEMENTED", "Endpoint PATCH /officer/customers/:uid sẽ hoàn thiện ở Giai đoạn 3B (Member C).");
});

officerRouter.post("/interest-rates", (_req, res) => {
  sendApiError(res, 501, "NOT_IMPLEMENTED", "Endpoint POST /officer/interest-rates sẽ hoàn thiện ở Giai đoạn 3B (Member C).");
});
