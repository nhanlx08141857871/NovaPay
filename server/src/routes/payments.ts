import { RequestHandler, Response, Router } from "express";
import admin from "firebase-admin";
import { AuthenticatedRequest, requireAuth, sendApiError } from "../middleware/auth";
import {
  StripeService,
  StripeServiceError,
  stripeService as defaultStripeService,
} from "../services/stripe";

export interface PaymentsRouteDeps {
  getDb?: () => FirebaseFirestore.Firestore;
  authMiddleware?: RequestHandler;
  stripeService?: StripeService;
}

class PaymentApiError extends Error {
  readonly statusCode: number;
  readonly errorCode: string;

  constructor(statusCode: number, errorCode: string, message: string) {
    super(message);
    this.name = "PaymentApiError";
    this.statusCode = statusCode;
    this.errorCode = errorCode;
  }
}

export function createPaymentsRouter(deps?: PaymentsRouteDeps): Router {
  const router = Router();
  const getDb = deps?.getDb ?? (() => admin.firestore());
  const auth = deps?.authMiddleware ?? requireAuth;
  const stripeSvc = deps?.stripeService ?? defaultStripeService;

  router.post("/finalize", auth, async (req: AuthenticatedRequest, res: Response): Promise<void> => {
    try {
      const uid = req.user?.uid;
      if (!uid) {
        sendApiError(res, 401, "PERMISSION_DENIED", "Không xác định được người dùng.");
        return;
      }

      const rawDraftId =
        req.body && typeof req.body === "object" && !Array.isArray(req.body)
          ? (req.body as Record<string, unknown>).draftId
          : undefined;
      const draftId = typeof rawDraftId === "string" ? rawDraftId.trim() : "";
      if (!draftId) {
        sendApiError(res, 400, "INVALID_ARGUMENT", "Thiếu mã draftId để hoàn tất thanh toán.");
        return;
      }

      const db = getDb();
      const draftRef = db.collection("transaction_drafts").doc(draftId);
      const draftSnap = await draftRef.get();
      if (!draftSnap.exists) {
        sendApiError(res, 404, "DRAFT_EXPIRED", "Phiên nạp tiền không tồn tại hoặc đã hết hạn.");
        return;
      }

      const draftData = draftSnap.data() ?? {};
      if (draftData.uid !== uid) {
        sendApiError(res, 403, "PERMISSION_DENIED", "Bạn không có quyền hoàn tất giao dịch này.");
        return;
      }

      if (draftData.status === "CONFIRMED") {
        res.status(200).json({
          txId: String(draftData.txId ?? ""),
          status: "CONFIRMED",
        });
        return;
      }

      if (draftData.status === "EXPIRED") {
        sendApiError(res, 400, "DRAFT_EXPIRED", "Phiên nạp tiền đã hết hạn.");
        return;
      }

      const stripeIntentId =
        typeof draftData.stripeIntentId === "string" ? draftData.stripeIntentId.trim() : "";
      if (draftData.status !== "PENDING_PAYMENT" || !stripeIntentId) {
        sendApiError(
          res,
          400,
          "PAYMENT_NOT_COMPLETED",
          "Giao dịch chưa ở trạng thái chờ hoàn tất thanh toán Stripe."
        );
        return;
      }

      const intent = await stripeSvc.retrievePaymentIntent(stripeIntentId);
      if (intent.status !== "succeeded") {
        sendApiError(
          res,
          400,
          "PAYMENT_NOT_COMPLETED",
          `Thanh toán Stripe chưa hoàn tất (trạng thái: ${intent.status}).`
        );
        return;
      }

      const draftPayload =
        draftData.payload && typeof draftData.payload === "object"
          ? (draftData.payload as Record<string, unknown>)
          : {};
      if (
        intent.amount !== Number(draftPayload.amount ?? 0) ||
        String(intent.currency).toLowerCase() !== "vnd"
      ) {
        sendApiError(
          res,
          400,
          "PAYMENT_NOT_COMPLETED",
          "Số tiền hoặc loại tiền thanh toán Stripe không khớp với giao dịch."
        );
        return;
      }

      const result = await db.runTransaction(async (tx) => {
        // PHASE 1: Tất cả tx.get trước mọi tx.set/tx.update
        const freshDraftSnap = await tx.get(draftRef);
        if (!freshDraftSnap.exists) {
          throw new PaymentApiError(404, "DRAFT_EXPIRED", "Phiên nạp tiền không tồn tại.");
        }

        const freshDraft = freshDraftSnap.data() ?? {};
        if (freshDraft.status === "CONFIRMED") {
          return {
            txId: String(freshDraft.txId ?? ""),
            status: "CONFIRMED",
          };
        }

        if (freshDraft.status !== "PENDING_PAYMENT") {
          throw new PaymentApiError(
            400,
            "PAYMENT_NOT_COMPLETED",
            "Trạng thái giao dịch không hợp lệ để cộng tiền."
          );
        }

        const payload =
          freshDraft.payload && typeof freshDraft.payload === "object"
            ? (freshDraft.payload as Record<string, unknown>)
            : {};
        const fromAccountId =
          typeof payload.fromAccountId === "string" ? payload.fromAccountId.trim() : "";
        const amount = Number(payload.amount ?? 0);
        const note = typeof payload.note === "string" ? payload.note.trim() : "";

        if (!fromAccountId || !Number.isSafeInteger(amount) || amount <= 0) {
          throw new PaymentApiError(400, "INVALID_ARGUMENT", "Dữ liệu nạp tiền không hợp lệ.");
        }

        const accRef = db.collection("accounts").doc(fromAccountId);
        const accSnap = await tx.get(accRef);
        if (!accSnap.exists) {
          throw new PaymentApiError(404, "ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản nhận tiền.");
        }
        const accData = accSnap.data() ?? {};
        if (accData.type !== "CHECKING" || accData.status !== "ACTIVE") {
          throw new PaymentApiError(
            400,
            "ACCOUNT_NOT_ACTIVE",
            "Tài khoản thanh toán nhận tiền không hoạt động."
          );
        }

        // PHASE 2: Ghi nguyên tử cộng tiền + ledger IN + transaction + khóa CONFIRMED
        const currentBalance = Number(accData.balance ?? 0);
        const newBalance = currentBalance + amount;
        const txRef = db.collection("transactions").doc();
        const txId = txRef.id;
        const nowTs = admin.firestore.FieldValue.serverTimestamp();

        tx.update(accRef, { balance: newBalance });
        tx.set(accRef.collection("ledger").doc(), {
          txId,
          direction: "IN",
          amount,
          balanceAfter: newBalance,
          description: note || "Nạp tiền vào tài khoản qua Stripe",
          createdAt: nowTs,
        });
        tx.set(txRef, {
          type: "GATEWAY",
          fromAccountId,
          amount,
          fee: 0,
          note,
          status: "SUCCESS",
          requiresBiometric: false,
          biometricPassed: false,
          otpVerified: true,
          faceChecked: false,
          meta: {
            gatewayRef: stripeIntentId,
          },
          initiatedBy: uid,
          createdAt: nowTs,
        });
        tx.update(draftRef, {
          status: "CONFIRMED",
          txId,
        });

        return {
          txId,
          status: "CONFIRMED",
        };
      });

      res.status(200).json(result);
    } catch (err) {
      if (err instanceof PaymentApiError) {
        sendApiError(res, err.statusCode, err.errorCode, err.message);
        return;
      }
      if (err instanceof StripeServiceError) {
        sendApiError(res, err.statusCode, err.code, err.message);
        return;
      }
      const message = err instanceof Error ? err.message : String(err);
      console.error("[NovaPay Payments Finalize Error]:", message);
      sendApiError(res, 500, "INTERNAL_ERROR", "Không thể hoàn tất thanh toán Stripe.");
    }
  });

  return router;
}

export const paymentsRouter = createPaymentsRouter();
