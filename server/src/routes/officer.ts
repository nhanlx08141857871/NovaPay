import { Router } from "express";
import {
  AuthenticatedRequest,
  requireAuth,
  requireOfficer,
  sendApiError,
} from "../middleware/auth";
import {
  OfficerServiceError,
  createCustomerWithAudit,
  updateCustomerWithAudit,
  upsertInterestRateWithAudit,
  validateCreateCustomerPayload,
  validateUpdateCustomerPayload,
  validateUpsertInterestRatePayload,
} from "../services/officerAudit";

/**
 * Phân hệ Quản trị Nhân viên Ngân hàng & Audit Logs (Owner: Thành viên C).
 */
export const officerRouter = Router();

officerRouter.use(requireAuth, requireOfficer);

officerRouter.post("/customers", async (req: AuthenticatedRequest, res) => {
  const actorUid = req.user?.uid;
  if (!actorUid) {
    sendApiError(res, 401, "PERMISSION_DENIED", "Thiếu thông tin nhân viên xác thực.");
    return;
  }

  const validation = validateCreateCustomerPayload(req.body);
  if (!validation.ok) {
    sendApiError(res, 400, "INVALID_ARGUMENT", validation.message);
    return;
  }

  try {
    const result = await createCustomerWithAudit(actorUid, validation.value);
    res.status(201).json(result);
  } catch (err) {
    if (err instanceof OfficerServiceError) {
      sendApiError(res, err.statusCode, err.errorCode, err.message);
      return;
    }
    console.error("[NovaPay Officer] POST /officer/customers error:", err);
    sendApiError(res, 500, "INTERNAL_ERROR", "Lỗi hệ thống khi tạo tài khoản khách hàng.");
  }
});

officerRouter.patch("/customers/:uid", async (req: AuthenticatedRequest, res) => {
  const actorUid = req.user?.uid;
  if (!actorUid) {
    sendApiError(res, 401, "PERMISSION_DENIED", "Thiếu thông tin nhân viên xác thực.");
    return;
  }

  const validation = validateUpdateCustomerPayload(req.params.uid, req.body);
  if (!validation.ok) {
    sendApiError(res, 400, "INVALID_ARGUMENT", validation.message);
    return;
  }

  try {
    const result = await updateCustomerWithAudit(actorUid, validation.value);
    res.status(200).json(result);
  } catch (err) {
    if (err instanceof OfficerServiceError) {
      sendApiError(res, err.statusCode, err.errorCode, err.message);
      return;
    }
    console.error("[NovaPay Officer] PATCH /officer/customers/:uid error:", err);
    sendApiError(res, 500, "INTERNAL_ERROR", "Lỗi hệ thống khi cập nhật hồ sơ khách hàng.");
  }
});

officerRouter.post("/interest-rates", async (req: AuthenticatedRequest, res) => {
  const actorUid = req.user?.uid;
  if (!actorUid) {
    sendApiError(res, 401, "PERMISSION_DENIED", "Thiếu thông tin nhân viên xác thực.");
    return;
  }

  const validation = validateUpsertInterestRatePayload(req.body);
  if (!validation.ok) {
    sendApiError(res, 400, "INVALID_ARGUMENT", validation.message);
    return;
  }

  try {
    const result = await upsertInterestRateWithAudit(actorUid, validation.value);
    res.status(200).json(result);
  } catch (err) {
    if (err instanceof OfficerServiceError) {
      sendApiError(res, err.statusCode, err.errorCode, err.message);
      return;
    }
    console.error("[NovaPay Officer] POST /officer/interest-rates error:", err);
    sendApiError(res, 500, "INTERNAL_ERROR", "Lỗi hệ thống khi cập nhật bảng lãi suất.");
  }
});
