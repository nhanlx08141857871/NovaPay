import { Router } from "express";
import { requireAuth, requireOfficer, sendApiError } from "../middleware/auth";

/**
 * Phân hệ eKYC Routes (Owner: Thành viên A — triển khai chi tiết ở Giai đoạn 3B).
 */
export const ekycRouter = Router();

ekycRouter.post("/submit", requireAuth, (_req, res) => {
  sendApiError(res, 501, "NOT_IMPLEMENTED", "Endpoint POST /ekyc/submit sẽ hoàn thiện ở Giai đoạn 3B (Member A).");
});

ekycRouter.post("/review", requireAuth, requireOfficer, (_req, res) => {
  sendApiError(res, 501, "NOT_IMPLEMENTED", "Endpoint POST /ekyc/review sẽ hoàn thiện ở Giai đoạn 3B (Member A).");
});
