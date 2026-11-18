import { Response, Router } from "express";
import admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import {
  AuthenticatedRequest,
  requireAuth,
  requireOfficer,
  sendApiError,
} from "../middleware/auth";

const MAX_EKYC_IMAGE_BYTES = 200 * 1024;
const BASE64_PATTERN = /^[A-Za-z0-9+/]+={0,2}$/;

class RouteHttpError extends Error {
  readonly statusCode: number;
  readonly errorCode: string;

  constructor(statusCode: number, errorCode: string, message: string) {
    super(message);
    this.statusCode = statusCode;
    this.errorCode = errorCode;
  }
}

function normalizeAndValidateJpegBase64(raw: unknown): string {
  if (typeof raw !== "string") {
    throw new RouteHttpError(400, "INVALID_ARGUMENT", "Thiếu dữ liệu ảnh eKYC.");
  }
  const stripped = raw.trim().replace(/^data:image\/[a-zA-Z0-9.+-]+;base64,/, "");
  if (!stripped || stripped.length % 4 !== 0 || !BASE64_PATTERN.test(stripped)) {
    throw new RouteHttpError(400, "INVALID_ARGUMENT", "Chuỗi Base64 ảnh eKYC không hợp lệ.");
  }
  const decoded = Buffer.from(stripped, "base64");
  if (decoded.length === 0 || decoded.length > MAX_EKYC_IMAGE_BYTES) {
    throw new RouteHttpError(
      400,
      "INVALID_ARGUMENT",
      "Dung lượng ảnh eKYC phải lớn hơn 0 và không vượt quá 200KB."
    );
  }
  // Kiểm tra JPEG SOI marker (0xFF 0xD8)
  if (decoded.length < 2 || decoded[0] !== 0xff || decoded[1] !== 0xd8) {
    throw new RouteHttpError(400, "INVALID_ARGUMENT", "Ảnh eKYC bắt buộc phải ở định dạng JPEG.");
  }
  return stripped;
}

/**
 * Phân hệ eKYC Routes (Owner: Thành viên A).
 * - POST /ekyc/submit: Khách hàng tự chụp hoặc Officer chụp hộ qua targetUid.
 * - POST /ekyc/review: Officer duyệt VERIFIED / REJECTED và ghi audit_logs bắt buộc.
 */
export const ekycRouter = Router();

ekycRouter.post("/submit", requireAuth, async (req: AuthenticatedRequest, res: Response): Promise<void> => {
  const actor = req.user;
  if (!actor) {
    sendApiError(res, 401, "PERMISSION_DENIED", "Chưa xác thực người dùng.");
    return;
  }

  const body = (req.body ?? {}) as { imageBase64?: unknown; targetUid?: unknown };
  let cleanBase64: string;
  try {
    cleanBase64 = normalizeAndValidateJpegBase64(body.imageBase64);
  } catch (err) {
    if (err instanceof RouteHttpError) {
      sendApiError(res, err.statusCode, err.errorCode, err.message);
      return;
    }
    sendApiError(res, 400, "INVALID_ARGUMENT", "Dữ liệu ảnh eKYC không hợp lệ.");
    return;
  }

  let effectiveUid = actor.uid;
  if (body.targetUid !== undefined && body.targetUid !== null) {
    if (typeof body.targetUid !== "string") {
      sendApiError(res, 400, "INVALID_ARGUMENT", "Trường targetUid không hợp lệ.");
      return;
    }
    const trimmedTarget = body.targetUid.trim();
    if (trimmedTarget.length > 0 && trimmedTarget !== actor.uid) {
      if (actor.role !== "OFFICER") {
        sendApiError(
          res,
          403,
          "PERMISSION_DENIED",
          "Chỉ nhân viên ngân hàng (Bank Officer) mới được phép gửi eKYC hộ khách hàng."
        );
        return;
      }
      effectiveUid = trimmedTarget;
    }
  }

  const db = admin.firestore();
  const userRef = db.collection("users").doc(effectiveUid);
  const imageRef = db.collection("ekyc_images").doc(effectiveUid);
  const isOfficerCounterSubmit = actor.role === "OFFICER" && effectiveUid !== actor.uid;

  try {
    await db.runTransaction(async (tx) => {
      const userSnap = await tx.get(userRef);
      if (!userSnap.exists) {
        throw new RouteHttpError(400, "INVALID_ARGUMENT", "Không tìm thấy hồ sơ người dùng để định danh eKYC.");
      }
      const userData = userSnap.data() ?? {};
      if (userData.status === "LOCKED") {
        throw new RouteHttpError(403, "PERMISSION_DENIED", "Tài khoản đang bị khóa, không thể thực hiện eKYC.");
      }

      const currentEkyc = (userData.ekyc ?? {}) as { status?: string };
      if (currentEkyc.status === "VERIFIED" && !isOfficerCounterSubmit) {
        throw new RouteHttpError(409, "ALREADY_VERIFIED", "Tài khoản đã được xác minh eKYC.");
      }

      tx.set(imageRef, {
        imageBase64: cleanBase64,
        capturedAt: FieldValue.serverTimestamp(),
        capturedBy: actor.uid,
      });

      tx.update(userRef, {
        "ekyc.status": "PENDING",
        "ekyc.verifiedBy": null,
        "ekyc.verifiedAt": null,
        updatedAt: FieldValue.serverTimestamp(),
      });

      if (isOfficerCounterSubmit) {
        const auditRef = db.collection("audit_logs").doc();
        tx.set(auditRef, {
          actorUid: actor.uid,
          action: "EKYC_COUNTER_SUBMIT",
          targetId: effectiveUid,
          details: { capturedBy: actor.uid },
          createdAt: FieldValue.serverTimestamp(),
        });
      }
    });

    res.status(200).json({ status: "PENDING" });
  } catch (err) {
    if (err instanceof RouteHttpError) {
      sendApiError(res, err.statusCode, err.errorCode, err.message);
      return;
    }
    sendApiError(res, 500, "INTERNAL_ERROR", "Không thể lưu ảnh eKYC. Vui lòng thử lại.");
  }
});

ekycRouter.post(
  "/review",
  requireAuth,
  requireOfficer,
  async (req: AuthenticatedRequest, res: Response): Promise<void> => {
    const actor = req.user;
    if (!actor) {
      sendApiError(res, 401, "PERMISSION_DENIED", "Chưa xác thực nhân viên.");
      return;
    }

    const body = (req.body ?? {}) as { uid?: unknown; decision?: unknown };
    const uid = typeof body.uid === "string" ? body.uid.trim() : "";
    const decision = typeof body.decision === "string" ? body.decision.trim().toUpperCase() : "";

    if (!uid || (decision !== "VERIFIED" && decision !== "REJECTED")) {
      sendApiError(
        res,
        400,
        "INVALID_ARGUMENT",
        "Yêu cầu phải có uid hợp lệ và decision thuộc VERIFIED hoặc REJECTED."
      );
      return;
    }

    const db = admin.firestore();
    const userRef = db.collection("users").doc(uid);
    const imageRef = db.collection("ekyc_images").doc(uid);

    try {
      await db.runTransaction(async (tx) => {
        const userSnap = await tx.get(userRef);
        if (!userSnap.exists) {
          throw new RouteHttpError(400, "INVALID_ARGUMENT", "Không tìm thấy hồ sơ khách hàng.");
        }
        const userData = userSnap.data() as { ekyc?: { status?: string } } | undefined;
        const currentEkycStatus = userData?.ekyc?.status;
        if (currentEkycStatus !== "PENDING") {
          throw new RouteHttpError(
            400,
            "EKYC_NOT_PENDING",
            "Hồ sơ eKYC không ở trạng thái PENDING để xét duyệt."
          );
        }

        if (decision === "VERIFIED") {
          const imageSnap = await tx.get(imageRef);
          if (!imageSnap.exists) {
            throw new RouteHttpError(
              400,
              "INVALID_ARGUMENT",
              "Chưa có ảnh khuôn mặt trong ekyc_images để xác thực."
            );
          }
        }

        tx.update(userRef, {
          "ekyc.status": decision,
          "ekyc.verifiedBy": actor.uid,
          "ekyc.verifiedAt": FieldValue.serverTimestamp(),
          updatedAt: FieldValue.serverTimestamp(),
        });

        const auditRef = db.collection("audit_logs").doc();
        tx.set(auditRef, {
          actorUid: actor.uid,
          action: "EKYC_REVIEW",
          targetId: uid,
          details: {
            decision,
            previousStatus: "PENDING",
          },
          createdAt: FieldValue.serverTimestamp(),
        });
      });

      res.status(200).json({ status: decision });
    } catch (err) {
      if (err instanceof RouteHttpError) {
        sendApiError(res, err.statusCode, err.errorCode, err.message);
        return;
      }
      sendApiError(res, 500, "INTERNAL_ERROR", "Không thể xử lý duyệt eKYC. Vui lòng thử lại.");
    }
  }
);
