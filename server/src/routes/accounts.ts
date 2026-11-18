import { RequestHandler, Response, Router } from "express";
import admin from "firebase-admin";
import { AuthenticatedRequest, requireAuth, sendApiError } from "../middleware/auth";

export interface AccountsRouteDeps {
  getDb?: () => FirebaseFirestore.Firestore;
  authMiddleware?: RequestHandler;
}

export function createAccountsRouter(deps?: AccountsRouteDeps): Router {
  const router = Router();
  const getDb = deps?.getDb ?? (() => admin.firestore());
  const auth = deps?.authMiddleware ?? requireAuth;

  router.get("/lookup", auth, async (req: AuthenticatedRequest, res: Response): Promise<void> => {
    try {
      const rawAccountNumber = req.query.accountNumber;
      const accountNumber =
        typeof rawAccountNumber === "string" ? rawAccountNumber.trim() : "";

      if (!accountNumber) {
        sendApiError(res, 400, "INVALID_ARGUMENT", "Vui lòng nhập số tài khoản cần tra cứu.");
        return;
      }

      const db = getDb();
      const snapshot = await db
        .collection("accounts")
        .where("accountNumber", "==", accountNumber)
        .limit(1)
        .get();

      if (snapshot.empty) {
        sendApiError(res, 404, "ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản tương ứng.");
        return;
      }

      const accountDoc = snapshot.docs[0];
      const accountData = accountDoc.data() ?? {};

      if (accountData.type !== "CHECKING") {
        sendApiError(
          res,
          400,
          "INVALID_ARGUMENT",
          "Chỉ hỗ trợ tra cứu tài khoản thanh toán (CHECKING)."
        );
        return;
      }

      if (accountData.status !== "ACTIVE") {
        sendApiError(
          res,
          400,
          "ACCOUNT_NOT_ACTIVE",
          "Tài khoản người nhận hiện đang bị khóa hoặc đã đóng."
        );
        return;
      }

      let ownerName = "";
      if (typeof accountData.ownerUid === "string" && accountData.ownerUid.trim().length > 0) {
        const userSnap = await db.collection("users").doc(accountData.ownerUid).get();
        if (userSnap.exists) {
          ownerName = String(userSnap.data()?.fullName ?? "");
        }
      }

      res.status(200).json({
        accountId: accountDoc.id,
        accountNumber: String(accountData.accountNumber ?? accountNumber),
        ownerName,
      });
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      console.error("[NovaPay Accounts Lookup Error]:", message);
      sendApiError(res, 500, "INTERNAL_ERROR", "Không thể tra cứu thông tin tài khoản.");
    }
  });

  return router;
}

export const accountsRouter = createAccountsRouter();
