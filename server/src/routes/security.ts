import { RequestHandler, Response, Router } from "express";
import admin from "firebase-admin";
import { AuthenticatedRequest, requireAuth, sendApiError } from "../middleware/auth";
import {
  createUserNotification,
  enqueueUserNotification,
  formatVnd,
} from "../services/notifications";
import { PinService, PinServiceError, pinService as defaultPinService } from "../services/pin";

export const MIN_PER_TXN_LIMIT_VND = 1_000_000;
export const MAX_PER_TXN_LIMIT_VND = 100_000_000;
export const MIN_DAILY_LIMIT_VND = 5_000_000;
export const MAX_DAILY_LIMIT_VND = 500_000_000;

export interface SecurityRouteDeps {
  getDb?: () => FirebaseFirestore.Firestore;
  authMiddleware?: RequestHandler;
  pinService?: PinService;
  now?: () => number;
}

class SecurityApiError extends Error {
  readonly statusCode: number;
  readonly errorCode: string;

  constructor(statusCode: number, errorCode: string, message: string) {
    super(message);
    this.name = "SecurityApiError";
    this.statusCode = statusCode;
    this.errorCode = errorCode;
  }
}

function asTrimmedString(val: unknown): string {
  return typeof val === "string" ? val.trim() : "";
}

function asPlainObject(val: unknown): Record<string, unknown> {
  return val && typeof val === "object" && !Array.isArray(val)
    ? (val as Record<string, unknown>)
    : {};
}

export function createSecurityRouter(deps?: SecurityRouteDeps): Router {
  const router = Router();
  const getDb = deps?.getDb ?? (() => admin.firestore());
  const auth = deps?.authMiddleware ?? requireAuth;
  const pinSvc = deps?.pinService ?? defaultPinService;
  const now = deps?.now ?? (() => Date.now());

  router.get("/status", auth, async (req: AuthenticatedRequest, res: Response): Promise<void> => {
    try {
      const uid = req.user?.uid;
      if (!uid) {
        sendApiError(res, 401, "PERMISSION_DENIED", "Không xác định được người dùng.");
        return;
      }
      if (req.user?.role === "OFFICER") {
        sendApiError(
          res,
          403,
          "PERMISSION_DENIED",
          "Tài khoản nhân viên ngân hàng không áp dụng cài đặt bảo mật khách hàng."
        );
        return;
      }

      const db = getDb();
      const userSnap = await db.collection("users").doc(uid).get();
      if (userSnap.exists && userSnap.data()?.status === "LOCKED") {
        sendApiError(res, 403, "PERMISSION_DENIED", "Tài khoản người dùng hiện đang bị khóa.");
        return;
      }

      const status = await pinSvc.getPinStatus(db, uid, now());
      res.status(200).json(status);
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      console.error("[NovaPay Security Status Error]:", message);
      sendApiError(res, 500, "INTERNAL_ERROR", "Không thể kiểm tra trạng thái bảo mật.");
    }
  });

  router.post("/pin/set", auth, async (req: AuthenticatedRequest, res: Response): Promise<void> => {
    try {
      const uid = req.user?.uid;
      if (!uid) {
        sendApiError(res, 401, "PERMISSION_DENIED", "Không xác định được người dùng.");
        return;
      }
      if (req.user?.role === "OFFICER") {
        sendApiError(
          res,
          403,
          "PERMISSION_DENIED",
          "Tài khoản nhân viên ngân hàng không áp dụng cài đặt bảo mật khách hàng."
        );
        return;
      }

      const body = asPlainObject(req.body);
      const db = getDb();
      const result = await pinSvc.setOrChangePin(
        db,
        uid,
        {
          currentPin: body.currentPin,
          newPin: body.newPin,
        },
        now()
      );

      await createUserNotification(db, uid, {
        category: "SECURITY",
        direction: null,
        title: result.isChange
          ? "Đã thay đổi mã PIN giao dịch"
          : "Đã thiết lập mã PIN giao dịch",
        body: result.isChange
          ? "Mã PIN giao dịch 6 số của Quý khách vừa được cập nhật thành công."
          : "Mã PIN giao dịch 6 số đã được kích hoạt để bảo vệ tài khoản của Quý khách.",
        amount: null,
        balanceAfter: null,
        accountNumber: null,
        txId: null,
        deepLink: "novapay://security",
      }).catch((notifErr) => {
        console.warn("[NovaPay Security] Không thể ghi thông báo đổi PIN:", notifErr);
      });

      res.status(200).json({
        ok: true,
        hasTransactionPin: true,
      });
    } catch (err) {
      if (err instanceof PinServiceError || err instanceof SecurityApiError) {
        sendApiError(res, err.statusCode, err.errorCode, err.message);
        return;
      }
      const message = err instanceof Error ? err.message : String(err);
      console.error("[NovaPay Security Set PIN Error]:", message);
      sendApiError(res, 500, "INTERNAL_ERROR", "Không thể cập nhật mã PIN giao dịch.");
    }
  });

  router.patch(
    "/accounts/:accountId/controls",
    auth,
    async (req: AuthenticatedRequest, res: Response): Promise<void> => {
      try {
        const uid = req.user?.uid;
        if (!uid) {
          sendApiError(res, 401, "PERMISSION_DENIED", "Không xác định được người dùng.");
          return;
        }
        if (req.user?.role === "OFFICER") {
          sendApiError(
            res,
            403,
            "PERMISSION_DENIED",
            "Tài khoản nhân viên ngân hàng không áp dụng cài đặt bảo mật khách hàng."
          );
          return;
        }

        const accountId = asTrimmedString(req.params.accountId);
        if (!accountId || accountId.length > 128) {
          sendApiError(res, 400, "INVALID_ARGUMENT", "Mã tài khoản không hợp lệ.");
          return;
        }

        const body = asPlainObject(req.body);
        const hasStatus = body.status !== undefined;
        const hasPerTxnLimit = body.perTxnLimit !== undefined;
        const hasDailyLimit = body.dailyLimit !== undefined;

        if (!hasStatus && !hasPerTxnLimit && !hasDailyLimit) {
          sendApiError(
            res,
            400,
            "INVALID_ARGUMENT",
            "Vui lòng cung cấp ít nhất một thông số cần cập nhật (status, perTxnLimit hoặc dailyLimit)."
          );
          return;
        }

        let requestedStatus: "ACTIVE" | "FROZEN" | undefined;
        if (hasStatus) {
          if (body.status !== "ACTIVE" && body.status !== "FROZEN") {
            sendApiError(
              res,
              400,
              "INVALID_ARGUMENT",
              "Trạng thái tài khoản chỉ hỗ trợ ACTIVE hoặc FROZEN."
            );
            return;
          }
          requestedStatus = body.status;
        }

        let requestedPerTxnLimit: number | undefined;
        if (hasPerTxnLimit) {
          if (
            typeof body.perTxnLimit !== "number" ||
            !Number.isSafeInteger(body.perTxnLimit) ||
            body.perTxnLimit < MIN_PER_TXN_LIMIT_VND ||
            body.perTxnLimit > MAX_PER_TXN_LIMIT_VND
          ) {
            sendApiError(
              res,
              400,
              "INVALID_ARGUMENT",
              `Hạn mức mỗi lần giao dịch phải từ ${formatVnd(MIN_PER_TXN_LIMIT_VND)} đến ${formatVnd(MAX_PER_TXN_LIMIT_VND)}.`
            );
            return;
          }
          requestedPerTxnLimit = body.perTxnLimit;
        }

        let requestedDailyLimit: number | undefined;
        if (hasDailyLimit) {
          if (
            typeof body.dailyLimit !== "number" ||
            !Number.isSafeInteger(body.dailyLimit) ||
            body.dailyLimit < MIN_DAILY_LIMIT_VND ||
            body.dailyLimit > MAX_DAILY_LIMIT_VND
          ) {
            sendApiError(
              res,
              400,
              "INVALID_ARGUMENT",
              `Hạn mức giao dịch ngày phải từ ${formatVnd(MIN_DAILY_LIMIT_VND)} đến ${formatVnd(MAX_DAILY_LIMIT_VND)}.`
            );
            return;
          }
          requestedDailyLimit = body.dailyLimit;
        }

        const db = getDb();
        const nowMs = now();
        const userRef = db.collection("users").doc(uid);
        const accRef = db.collection("accounts").doc(accountId);

        const [userSnap, accSnap] = await Promise.all([userRef.get(), accRef.get()]);
        if (!userSnap.exists || userSnap.data()?.status !== "ACTIVE") {
          sendApiError(
            res,
            403,
            "PERMISSION_DENIED",
            "Tài khoản người dùng hiện không hoạt động hoặc đã bị khóa."
          );
          return;
        }
        if (!accSnap.exists) {
          sendApiError(res, 404, "ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản.");
          return;
        }
        const accData = accSnap.data() ?? {};
        if (accData.ownerUid !== uid) {
          sendApiError(
            res,
            403,
            "PERMISSION_DENIED",
            "Bạn không có quyền điều chỉnh bảo mật tài khoản này."
          );
          return;
        }
        if (accData.type !== "CHECKING") {
          sendApiError(
            res,
            400,
            "INVALID_ARGUMENT",
            "Chỉ hỗ trợ điều chỉnh trạng thái và hạn mức cho tài khoản thanh toán (CHECKING)."
          );
          return;
        }
        if (accData.status === "CLOSED") {
          sendApiError(
            res,
            400,
            "INVALID_ARGUMENT",
            "Tài khoản thanh toán đã đóng, không thể thay đổi cấu hình bảo mật."
          );
          return;
        }

        const currentStatus = typeof accData.status === "string" ? accData.status : "ACTIVE";
        const currentPerTxnLimit = Number(accData.perTxnLimit ?? 0);
        const currentDailyLimit = Number(accData.dailyLimit ?? 0);

        const previewNextStatus = requestedStatus ?? (currentStatus as "ACTIVE" | "FROZEN");
        const previewNextPerTxn = requestedPerTxnLimit ?? currentPerTxnLimit;
        const previewNextDaily = requestedDailyLimit ?? currentDailyLimit;

        if ((hasPerTxnLimit || hasDailyLimit) && previewNextPerTxn > previewNextDaily) {
          sendApiError(
            res,
            400,
            "INVALID_ARGUMENT",
            "Hạn mức mỗi lần giao dịch không được vượt quá hạn mức giao dịch trong ngày."
          );
          return;
        }

        const isUnfreezing = currentStatus === "FROZEN" && previewNextStatus === "ACTIVE";
        const isRaisingPerTxn =
          requestedPerTxnLimit !== undefined && requestedPerTxnLimit > currentPerTxnLimit;
        const isRaisingDaily =
          requestedDailyLimit !== undefined && requestedDailyLimit > currentDailyLimit;

        let pinVerified = false;
        if (isUnfreezing || isRaisingPerTxn || isRaisingDaily) {
          const pinResult = await pinSvc.verifyTransactionPin(db, uid, body.pin, nowMs);
          pinVerified = pinResult.hasPin && pinResult.verified;
        }

        const secretRef = db.collection("user_secrets").doc(uid);
        const updated = await db.runTransaction(async (tx) => {
          const [freshUserSnap, freshAccSnap, freshSecretSnap] = await Promise.all([
            tx.get(userRef),
            tx.get(accRef),
            tx.get(secretRef),
          ]);

          if (!freshUserSnap.exists || freshUserSnap.data()?.status !== "ACTIVE") {
            throw new SecurityApiError(
              403,
              "PERMISSION_DENIED",
              "Tài khoản người dùng hiện không hoạt động."
            );
          }
          if (!freshAccSnap.exists) {
            throw new SecurityApiError(404, "ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản.");
          }
          const freshAccData = freshAccSnap.data() ?? {};
          if (freshAccData.ownerUid !== uid) {
            throw new SecurityApiError(
              403,
              "PERMISSION_DENIED",
              "Bạn không có quyền điều chỉnh bảo mật tài khoản này."
            );
          }
          if (freshAccData.type !== "CHECKING") {
            throw new SecurityApiError(
              400,
              "INVALID_ARGUMENT",
              "Chỉ hỗ trợ tài khoản thanh toán (CHECKING)."
            );
          }
          if (freshAccData.status === "CLOSED") {
            throw new SecurityApiError(
              400,
              "INVALID_ARGUMENT",
              "Tài khoản thanh toán đã đóng, không thể thay đổi cấu hình bảo mật."
            );
          }

          const prevStatus = typeof freshAccData.status === "string" ? freshAccData.status : "ACTIVE";
          const prevPerTxnLimit = Number(freshAccData.perTxnLimit ?? 0);
          const prevDailyLimit = Number(freshAccData.dailyLimit ?? 0);
          const nextStatus = requestedStatus ?? (prevStatus as "ACTIVE" | "FROZEN");
          const nextPerTxnLimit = requestedPerTxnLimit ?? prevPerTxnLimit;
          const nextDailyLimit = requestedDailyLimit ?? prevDailyLimit;

          if ((hasPerTxnLimit || hasDailyLimit) && nextPerTxnLimit > nextDailyLimit) {
            throw new SecurityApiError(
              400,
              "INVALID_ARGUMENT",
              "Hạn mức mỗi lần giao dịch không được vượt quá hạn mức giao dịch trong ngày."
            );
          }

          const freshSecretData = freshSecretSnap.exists ? freshSecretSnap.data() ?? {} : {};
          const hasPin =
            typeof freshSecretData.pinHash === "string" && freshSecretData.pinHash.length > 0;
          const txRequiresPin =
            (prevStatus === "FROZEN" && nextStatus === "ACTIVE") ||
            nextPerTxnLimit > prevPerTxnLimit ||
            nextDailyLimit > prevDailyLimit;

          if (hasPin && txRequiresPin && !pinVerified) {
            throw new SecurityApiError(
              400,
              "PIN_REQUIRED",
              "Vui lòng nhập mã PIN giao dịch để mở khóa hoặc nâng hạn mức tài khoản."
            );
          }

          const nowTs = admin.firestore.FieldValue.serverTimestamp();
          const updateFields: Record<string, unknown> = {
            updatedAt: nowTs,
          };
          if (requestedStatus !== undefined) {
            updateFields.status = nextStatus;
          }
          if (requestedPerTxnLimit !== undefined) {
            updateFields.perTxnLimit = nextPerTxnLimit;
          }
          if (requestedDailyLimit !== undefined) {
            updateFields.dailyLimit = nextDailyLimit;
          }

          tx.update(accRef, updateFields);

          const accountNumber = asTrimmedString(freshAccData.accountNumber);
          const statusChanged = requestedStatus !== undefined && nextStatus !== prevStatus;
          let title = "Cập nhật cài đặt bảo mật tài khoản";
          if (statusChanged && nextStatus === "FROZEN") {
            title = "Đã tạm khóa tài khoản thanh toán";
          } else if (statusChanged && nextStatus === "ACTIVE") {
            title = "Đã mở khóa tài khoản thanh toán";
          } else if (hasPerTxnLimit || hasDailyLimit) {
            title = "Đã cập nhật hạn mức giao dịch";
          }

          const bodyParts: string[] = [];
          if (statusChanged) {
            bodyParts.push(
              nextStatus === "FROZEN"
                ? `Tài khoản ${accountNumber || accountId} đã được tạm khóa khẩn cấp.`
                : `Tài khoản ${accountNumber || accountId} đã được kích hoạt hoạt động trở lại.`
            );
          }
          if (hasPerTxnLimit || hasDailyLimit) {
            bodyParts.push(
              `Hạn mức mới cho TK ${accountNumber || accountId}: ${formatVnd(nextPerTxnLimit)}/lần, ${formatVnd(nextDailyLimit)}/ngày.`
            );
          }

          enqueueUserNotification(
            tx,
            db,
            uid,
            {
              category: "SECURITY",
              direction: null,
              title,
              body: bodyParts.join(" "),
              amount: null,
              balanceAfter: null,
              accountNumber: accountNumber || null,
              txId: null,
              deepLink: `novapay://accounts/${accountId}`,
            },
            nowTs
          );

          return {
            ok: true as const,
            status: nextStatus,
            perTxnLimit: nextPerTxnLimit,
            dailyLimit: nextDailyLimit,
          };
        });

        res.status(200).json(updated);
      } catch (err) {
        if (err instanceof PinServiceError || err instanceof SecurityApiError) {
          sendApiError(res, err.statusCode, err.errorCode, err.message);
          return;
        }
        const message = err instanceof Error ? err.message : String(err);
        console.error("[NovaPay Security Controls Error]:", message);
        sendApiError(res, 500, "INTERNAL_ERROR", "Không thể cập nhật cài đặt bảo mật tài khoản.");
      }
    }
  );

  return router;
}

export const securityRouter = createSecurityRouter();
