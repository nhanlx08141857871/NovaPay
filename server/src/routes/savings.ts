import crypto from "node:crypto";
import { RequestHandler, Response, Router } from "express";
import admin from "firebase-admin";
import { AuthenticatedRequest, requireAuth, sendApiError } from "../middleware/auth";
import { enqueueUserNotification, formatVnd } from "../services/notifications";
import { PinService, PinServiceError, pinService as defaultPinService } from "../services/pin";

const VN_OFFSET_MS = 7 * 60 * 60 * 1000;
const DAY_MS = 86_400_000;
const MIN_SAVING_PRINCIPAL_VND = 1_000_000;
const DEFAULT_DEMAND_ANNUAL_RATE = 0.5;
const MAX_ACCOUNT_NUMBER_ATTEMPTS = 5;

export interface SavingsRouteDeps {
  getDb?: () => FirebaseFirestore.Firestore;
  authMiddleware?: RequestHandler;
  pinService?: PinService;
  now?: () => number;
  savingAccountNumberGenerator?: () => string;
}

class SavingsApiError extends Error {
  readonly statusCode: number;
  readonly errorCode: string;

  constructor(statusCode: number, errorCode: string, message: string) {
    super(message);
    this.name = "SavingsApiError";
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

function toDateSafe(value: unknown): Date | null {
  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : value;
  }
  if (value && typeof value === "object") {
    const maybeTs = value as { toDate?: () => Date; _seconds?: number; seconds?: number };
    if (typeof maybeTs.toDate === "function") {
      const d = maybeTs.toDate();
      return d instanceof Date && !Number.isNaN(d.getTime()) ? d : null;
    }
    const sec =
      typeof maybeTs._seconds === "number"
        ? maybeTs._seconds
        : typeof maybeTs.seconds === "number"
          ? maybeTs.seconds
          : null;
    if (sec !== null && Number.isFinite(sec)) {
      return new Date(sec * 1000);
    }
  }
  if (typeof value === "number" && Number.isFinite(value)) {
    return new Date(value);
  }
  if (typeof value === "string" && value.trim().length > 0) {
    const d = new Date(value);
    return Number.isNaN(d.getTime()) ? null : d;
  }
  return null;
}

export function generateSavingAccountNumber(): string {
  const randomDigits = crypto.randomInt(0, 100_000_000).toString().padStart(8, "0");
  return `20${randomDigits}`;
}

export function addCalendarMonthsHoChiMinh(baseMs: number, months: number): Date {
  const shifted = new Date(baseMs + VN_OFFSET_MS);
  const totalMonths = shifted.getUTCFullYear() * 12 + shifted.getUTCMonth() + months;
  const targetYear = Math.floor(totalMonths / 12);
  const targetMonth = totalMonths % 12;
  const origDay = shifted.getUTCDate();
  const lastDayOfTargetMonth = new Date(Date.UTC(targetYear, targetMonth + 1, 0)).getUTCDate();
  const clampedDay = Math.min(origDay, lastDayOfTargetMonth);

  const nextShiftedMs = Date.UTC(
    targetYear,
    targetMonth,
    clampedDay,
    shifted.getUTCHours(),
    shifted.getUTCMinutes(),
    shifted.getUTCSeconds(),
    shifted.getUTCMilliseconds()
  );
  return new Date(nextShiftedMs - VN_OFFSET_MS);
}

export function computeSavingsSettlementAmounts(params: {
  principalAmount: number;
  termMonths: number;
  termAnnualRate: number;
  demandAnnualRate: number;
  openedAtMs: number;
  maturityDateMs: number;
  nowMs: number;
}): {
  interestAmount: number;
  totalPayoutAmount: number;
  earlySettlement: boolean;
  appliedAnnualRate: number;
} {
  const {
    principalAmount,
    termMonths,
    termAnnualRate,
    demandAnnualRate,
    openedAtMs,
    maturityDateMs,
    nowMs,
  } = params;

  if (nowMs >= maturityDateMs) {
    const interestAmount = Math.floor((principalAmount * termAnnualRate * termMonths) / (12 * 100));
    return {
      interestAmount,
      totalPayoutAmount: principalAmount + interestAmount,
      earlySettlement: false,
      appliedAnnualRate: termAnnualRate,
    };
  }

  const daysHeld = Math.max(0, Math.floor((nowMs - openedAtMs) / DAY_MS));
  const interestAmount = Math.floor((principalAmount * demandAnnualRate * daysHeld) / (365 * 100));
  return {
    interestAmount,
    totalPayoutAmount: principalAmount + interestAmount,
    earlySettlement: true,
    appliedAnnualRate: demandAnnualRate,
  };
}

export function createSavingsRouter(deps?: SavingsRouteDeps): Router {
  const router = Router();
  const getDb = deps?.getDb ?? (() => admin.firestore());
  const auth = deps?.authMiddleware ?? requireAuth;
  const pinSvc = deps?.pinService ?? defaultPinService;
  const now = deps?.now ?? (() => Date.now());
  const nextSavingAccountNumber =
    deps?.savingAccountNumberGenerator ?? generateSavingAccountNumber;

  router.post("/open", auth, async (req: AuthenticatedRequest, res: Response): Promise<void> => {
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
          "Tài khoản nhân viên ngân hàng không sử dụng tính năng tiết kiệm cá nhân."
        );
        return;
      }

      const body = asPlainObject(req.body);
      const fromCheckingAccountId = asTrimmedString(body.fromCheckingAccountId);
      const termMonths = body.termMonths;
      const principalAmount = body.principalAmount;
      const pin = body.pin;

      if (!fromCheckingAccountId || fromCheckingAccountId.length > 128) {
        sendApiError(res, 400, "INVALID_ARGUMENT", "Thiếu mã tài khoản thanh toán nguồn.");
        return;
      }
      if (
        typeof termMonths !== "number" ||
        !Number.isInteger(termMonths) ||
        termMonths < 1 ||
        termMonths > 120
      ) {
        sendApiError(
          res,
          400,
          "INVALID_ARGUMENT",
          "Kỳ hạn gửi tiết kiệm không hợp lệ (tối thiểu 1 tháng)."
        );
        return;
      }
      if (
        typeof principalAmount !== "number" ||
        !Number.isSafeInteger(principalAmount) ||
        principalAmount < MIN_SAVING_PRINCIPAL_VND
      ) {
        sendApiError(
          res,
          400,
          "INVALID_ARGUMENT",
          "Số tiền gửi tiết kiệm phải là số nguyên tối thiểu 1.000.000 ₫."
        );
        return;
      }

      const db = getDb();
      const nowMs = now();

      // Kiểm tra điều kiện tiên quyết trước khi xác thực PIN để tránh trừ lượt nhập PIN sai vô ích.
      const [userSnap, fromAccSnap, preRateSnap] = await Promise.all([
        db.collection("users").doc(uid).get(),
        db.collection("accounts").doc(fromCheckingAccountId).get(),
        db.collection("interest_rates").where("termMonths", "==", termMonths).limit(1).get(),
      ]);

      if (!userSnap.exists) {
        sendApiError(res, 403, "PERMISSION_DENIED", "Không tìm thấy hồ sơ người dùng.");
        return;
      }
      const userData = userSnap.data() ?? {};
      if (userData.status !== "ACTIVE") {
        sendApiError(res, 403, "PERMISSION_DENIED", "Tài khoản người dùng hiện không hoạt động.");
        return;
      }
      if (asPlainObject(userData.ekyc).status !== "VERIFIED") {
        sendApiError(
          res,
          403,
          "EKYC_REQUIRED",
          "Vui lòng hoàn tất định danh eKYC trước khi mở sổ tiết kiệm."
        );
        return;
      }

      if (!fromAccSnap.exists) {
        sendApiError(res, 404, "ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản thanh toán nguồn.");
        return;
      }
      const fromAccData = fromAccSnap.data() ?? {};
      if (fromAccData.ownerUid !== uid) {
        sendApiError(res, 403, "PERMISSION_DENIED", "Bạn không có quyền sử dụng tài khoản này.");
        return;
      }
      if (fromAccData.type !== "CHECKING") {
        sendApiError(
          res,
          400,
          "INVALID_ARGUMENT",
          "Chỉ được trích tiền từ tài khoản thanh toán (CHECKING) để mở sổ tiết kiệm."
        );
        return;
      }
      if (fromAccData.status !== "ACTIVE") {
        sendApiError(
          res,
          400,
          "ACCOUNT_NOT_ACTIVE",
          "Tài khoản thanh toán nguồn hiện không hoạt động."
        );
        return;
      }
      if (Number(fromAccData.balance ?? 0) < principalAmount) {
        sendApiError(res, 400, "INSUFFICIENT_FUNDS", "Số dư tài khoản thanh toán không đủ.");
        return;
      }
      if (preRateSnap.empty) {
        sendApiError(
          res,
          400,
          "INVALID_ARGUMENT",
          `Kỳ hạn ${termMonths} tháng chưa được hỗ trợ trong biểu lãi suất hiện hành.`
        );
        return;
      }

      const pinCheck = await pinSvc.verifyTransactionPin(db, uid, pin, nowMs);

      const result = await db.runTransaction(async (tx) => {
        // PHASE 1: Tất cả tx.get trước mọi tx.set/tx.update
        const userRef = db.collection("users").doc(uid);
        const fromAccRef = db.collection("accounts").doc(fromCheckingAccountId);
        const secretRef = db.collection("user_secrets").doc(uid);
        const rateQuery = db
          .collection("interest_rates")
          .where("termMonths", "==", termMonths)
          .limit(1);

        const [freshUserSnap, freshFromAccSnap, rateSnap, freshSecretSnap] = await Promise.all([
          tx.get(userRef),
          tx.get(fromAccRef),
          tx.get(rateQuery),
          tx.get(secretRef),
        ]);

        let chosenAccountNumber: string | null = null;
        for (let attempt = 0; attempt < MAX_ACCOUNT_NUMBER_ATTEMPTS; attempt += 1) {
          const candidate = nextSavingAccountNumber();
          const existingSnap = await tx.get(
            db.collection("accounts").where("accountNumber", "==", candidate).limit(1)
          );
          if (existingSnap.empty) {
            chosenAccountNumber = candidate;
            break;
          }
        }
        if (!chosenAccountNumber) {
          throw new SavingsApiError(
            500,
            "INTERNAL_ERROR",
            "Không thể khởi tạo số tài khoản tiết kiệm duy nhất. Vui lòng thử lại."
          );
        }

        if (!freshUserSnap.exists || freshUserSnap.data()?.status !== "ACTIVE") {
          throw new SavingsApiError(
            403,
            "PERMISSION_DENIED",
            "Tài khoản người dùng hiện không hoạt động."
          );
        }
        const freshUser = freshUserSnap.data() ?? {};
        if (asPlainObject(freshUser.ekyc).status !== "VERIFIED") {
          throw new SavingsApiError(
            403,
            "EKYC_REQUIRED",
            "Vui lòng hoàn tất định danh eKYC trước khi mở sổ tiết kiệm."
          );
        }
        const freshSecretData = freshSecretSnap.exists ? freshSecretSnap.data() ?? {} : {};
        const hasPinInTx =
          typeof freshSecretData.pinHash === "string" && freshSecretData.pinHash.length > 0;
        if (hasPinInTx && !(pinCheck.hasPin && pinCheck.verified)) {
          throw new SavingsApiError(
            400,
            "PIN_REQUIRED",
            "Vui lòng nhập mã PIN giao dịch 6 số để xác thực mở sổ tiết kiệm."
          );
        }

        if (!freshFromAccSnap.exists) {
          throw new SavingsApiError(
            404,
            "ACCOUNT_NOT_FOUND",
            "Không tìm thấy tài khoản thanh toán nguồn."
          );
        }
        const freshFromData = freshFromAccSnap.data() ?? {};
        if (freshFromData.ownerUid !== uid) {
          throw new SavingsApiError(
            403,
            "PERMISSION_DENIED",
            "Bạn không có quyền sử dụng tài khoản này."
          );
        }
        if (freshFromData.type !== "CHECKING") {
          throw new SavingsApiError(
            400,
            "INVALID_ARGUMENT",
            "Chỉ tài khoản thanh toán (CHECKING) mới được mở sổ tiết kiệm."
          );
        }
        if (freshFromData.status !== "ACTIVE") {
          throw new SavingsApiError(
            400,
            "ACCOUNT_NOT_ACTIVE",
            "Tài khoản thanh toán nguồn hiện không hoạt động."
          );
        }

        const currentCheckingBalance = Number(freshFromData.balance ?? 0);
        if (currentCheckingBalance < principalAmount) {
          throw new SavingsApiError(
            400,
            "INSUFFICIENT_FUNDS",
            "Số dư tài khoản thanh toán không đủ."
          );
        }

        if (rateSnap.empty) {
          throw new SavingsApiError(
            400,
            "INVALID_ARGUMENT",
            "Kỳ hạn gửi tiết kiệm không tồn tại trong biểu lãi suất."
          );
        }
        const rateDoc = rateSnap.docs[0];
        const annualRate = Number(rateDoc.data()?.annualRate ?? 0);
        if (!Number.isFinite(annualRate) || annualRate <= 0) {
          throw new SavingsApiError(
            400,
            "INVALID_ARGUMENT",
            "Mức lãi suất của kỳ hạn này hiện không hợp lệ."
          );
        }

        const newCheckingBalance = currentCheckingBalance - principalAmount;
        const fromAccountNumber = asTrimmedString(freshFromData.accountNumber);
        const ownerName = asTrimmedString(freshUser.fullName);
        const openedAtTs = admin.firestore.Timestamp.fromMillis(nowMs);
        const maturityDate = addCalendarMonthsHoChiMinh(nowMs, termMonths);
        const maturityDateTs = admin.firestore.Timestamp.fromDate(maturityDate);
        const createdAtTs = admin.firestore.FieldValue.serverTimestamp();

        const savingRef = db.collection("accounts").doc();
        const savingAccountId = savingRef.id;
        const txRef = db.collection("transactions").doc();
        const txId = txRef.id;
        const note = `Mở sổ tiết kiệm trực tuyến kỳ hạn ${termMonths} tháng (${annualRate}%/năm)`;

        tx.update(fromAccRef, { balance: newCheckingBalance });
        tx.set(fromAccRef.collection("ledger").doc(), {
          txId,
          direction: "OUT",
          amount: principalAmount,
          balanceAfter: newCheckingBalance,
          description: note,
          createdAt: createdAtTs,
        });

        tx.set(savingRef, {
          ownerUid: uid,
          type: "SAVING",
          accountNumber: chosenAccountNumber,
          status: "ACTIVE",
          balance: principalAmount,
          currency: "VND",
          perTxnLimit: 0,
          dailyLimit: 0,
          saving: {
            termMonths,
            principal: principalAmount,
            interestRateId: rateDoc.id,
            annualRate,
            openedAt: openedAtTs,
            maturityDate: maturityDateTs,
          },
          mortgage: null,
          createdAt: createdAtTs,
        });
        tx.set(savingRef.collection("ledger").doc(), {
          txId,
          direction: "IN",
          amount: principalAmount,
          balanceAfter: principalAmount,
          description: `Gửi gốc mở sổ tiết kiệm kỳ hạn ${termMonths} tháng`,
          createdAt: createdAtTs,
        });

        tx.set(txRef, {
          type: "TRANSFER_INTERNAL",
          fromAccountId: fromCheckingAccountId,
          ...(fromAccountNumber ? { fromAccountNumber } : {}),
          toAccountId: savingAccountId,
          toAccountNumber: chosenAccountNumber,
          ...(ownerName ? { toOwnerName: ownerName } : {}),
          amount: principalAmount,
          fee: 0,
          note,
          status: "SUCCESS",
          requiresBiometric: false,
          biometricPassed: false,
          otpVerified: false,
          faceChecked: false,
          meta: {
            savingAccountId,
            termMonths,
            annualRate,
          },
          initiatedBy: uid,
          createdAt: createdAtTs,
        });

        enqueueUserNotification(
          tx,
          db,
          uid,
          {
            category: "SAVING",
            direction: "OUT",
            title: "Mở sổ tiết kiệm thành công",
            body: `Quý khách đã mở sổ tiết kiệm ${chosenAccountNumber} kỳ hạn ${termMonths} tháng với số tiền ${formatVnd(principalAmount)} (lãi suất ${annualRate}%/năm). Số dư TK ${fromAccountNumber || "thanh toán"} còn lại: ${formatVnd(newCheckingBalance)}.`,
            amount: principalAmount,
            balanceAfter: newCheckingBalance,
            accountNumber: fromAccountNumber || chosenAccountNumber,
            txId,
            deepLink: `novapay://accounts/${savingAccountId}`,
          },
          createdAtTs
        );

        return {
          savingAccountId,
          accountNumber: chosenAccountNumber,
          txId,
          annualRate,
          maturityDateEpochMs: maturityDate.getTime(),
        };
      });

      res.status(200).json(result);
    } catch (err) {
      if (err instanceof PinServiceError || err instanceof SavingsApiError) {
        sendApiError(res, err.statusCode, err.errorCode, err.message);
        return;
      }
      const message = err instanceof Error ? err.message : String(err);
      console.error("[NovaPay Savings Open Error]:", message);
      sendApiError(res, 500, "INTERNAL_ERROR", "Không thể mở sổ tiết kiệm trực tuyến.");
    }
  });

  router.post(
    "/:accountId/settle",
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
            "Tài khoản nhân viên ngân hàng không sử dụng tính năng tiết kiệm cá nhân."
          );
          return;
        }

        const savingAccountId = asTrimmedString(req.params.accountId);
        const body = asPlainObject(req.body);
        const toCheckingAccountId = asTrimmedString(body.toCheckingAccountId);
        const pin = body.pin;

        if (
          !savingAccountId ||
          savingAccountId.length > 128 ||
          !toCheckingAccountId ||
          toCheckingAccountId.length > 128
        ) {
          sendApiError(
            res,
            400,
            "INVALID_ARGUMENT",
            "Thiếu mã sổ tiết kiệm hoặc tài khoản thanh toán nhận tiền tất toán."
          );
          return;
        }

        const db = getDb();
        const nowMs = now();

        const [userSnap, savingSnap, checkingSnap] = await Promise.all([
          db.collection("users").doc(uid).get(),
          db.collection("accounts").doc(savingAccountId).get(),
          db.collection("accounts").doc(toCheckingAccountId).get(),
        ]);

        if (!userSnap.exists) {
          sendApiError(res, 403, "PERMISSION_DENIED", "Không tìm thấy hồ sơ người dùng.");
          return;
        }
        const userData = userSnap.data() ?? {};
        if (userData.status !== "ACTIVE") {
          sendApiError(res, 403, "PERMISSION_DENIED", "Tài khoản người dùng hiện không hoạt động.");
          return;
        }
        if (asPlainObject(userData.ekyc).status !== "VERIFIED") {
          sendApiError(
            res,
            403,
            "EKYC_REQUIRED",
            "Vui lòng hoàn tất định danh eKYC trước khi tất toán sổ tiết kiệm."
          );
          return;
        }

        if (!savingSnap.exists) {
          sendApiError(res, 404, "ACCOUNT_NOT_FOUND", "Không tìm thấy sổ tiết kiệm.");
          return;
        }
        const savingData = savingSnap.data() ?? {};
        if (savingData.ownerUid !== uid) {
          sendApiError(res, 403, "PERMISSION_DENIED", "Bạn không có quyền tất toán sổ tiết kiệm này.");
          return;
        }
        if (savingData.type !== "SAVING") {
          sendApiError(res, 400, "INVALID_ARGUMENT", "Tài khoản được chọn không phải sổ tiết kiệm.");
          return;
        }
        if (savingData.status !== "ACTIVE") {
          sendApiError(
            res,
            400,
            "ACCOUNT_NOT_ACTIVE",
            "Sổ tiết kiệm này đã tất toán hoặc không ở trạng thái hoạt động."
          );
          return;
        }

        if (!checkingSnap.exists) {
          sendApiError(res, 404, "ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản thanh toán nhận tiền.");
          return;
        }
        const checkingData = checkingSnap.data() ?? {};
        if (checkingData.ownerUid !== uid) {
          sendApiError(
            res,
            403,
            "PERMISSION_DENIED",
            "Tài khoản thanh toán nhận tiền không thuộc quyền sở hữu của bạn."
          );
          return;
        }
        if (checkingData.type !== "CHECKING") {
          sendApiError(
            res,
            400,
            "INVALID_ARGUMENT",
            "Tài khoản nhận tiền tất toán phải là tài khoản thanh toán (CHECKING)."
          );
          return;
        }
        if (checkingData.status !== "ACTIVE") {
          sendApiError(
            res,
            400,
            "ACCOUNT_NOT_ACTIVE",
            "Tài khoản thanh toán nhận tiền hiện không hoạt động."
          );
          return;
        }

        const pinCheck = await pinSvc.verifyTransactionPin(db, uid, pin, nowMs);

        const result = await db.runTransaction(async (tx) => {
          // PHASE 1: Đọc toàn bộ dữ liệu liên quan trước khi ghi
          const userRef = db.collection("users").doc(uid);
          const savingRef = db.collection("accounts").doc(savingAccountId);
          const checkingRef = db.collection("accounts").doc(toCheckingAccountId);
          const secretRef = db.collection("user_secrets").doc(uid);
          const demandRateQuery = db
            .collection("interest_rates")
            .where("termMonths", "==", 0)
            .limit(1);

          const [freshUserSnap, freshSavingSnap, freshCheckingSnap, demandRateSnap, freshSecretSnap] =
            await Promise.all([
              tx.get(userRef),
              tx.get(savingRef),
              tx.get(checkingRef),
              tx.get(demandRateQuery),
              tx.get(secretRef),
            ]);

          const freshSavingData = freshSavingSnap.data() ?? {};
          const savingInfo = asPlainObject(freshSavingData.saving);
          const interestRateId = asTrimmedString(savingInfo.interestRateId);
          const termMonths = Number(savingInfo.termMonths ?? 0);

          const rateByIdSnap = interestRateId
            ? await tx.get(db.collection("interest_rates").doc(interestRateId))
            : null;
          const termRateSnap =
            termMonths > 0
              ? await tx.get(
                  db.collection("interest_rates").where("termMonths", "==", termMonths).limit(1)
                )
              : null;

          if (!freshUserSnap.exists || freshUserSnap.data()?.status !== "ACTIVE") {
            throw new SavingsApiError(
              403,
              "PERMISSION_DENIED",
              "Tài khoản người dùng hiện không hoạt động."
            );
          }
          const freshUser = freshUserSnap.data() ?? {};
          if (asPlainObject(freshUser.ekyc).status !== "VERIFIED") {
            throw new SavingsApiError(
              403,
              "EKYC_REQUIRED",
              "Vui lòng hoàn tất định danh eKYC trước khi tất toán sổ tiết kiệm."
            );
          }
          const freshSecretData = freshSecretSnap.exists ? freshSecretSnap.data() ?? {} : {};
          const hasPinInTx =
            typeof freshSecretData.pinHash === "string" && freshSecretData.pinHash.length > 0;
          if (hasPinInTx && !(pinCheck.hasPin && pinCheck.verified)) {
            throw new SavingsApiError(
              400,
              "PIN_REQUIRED",
              "Vui lòng nhập mã PIN giao dịch 6 số để xác thực tất toán sổ tiết kiệm."
            );
          }

          if (!freshSavingSnap.exists) {
            throw new SavingsApiError(404, "ACCOUNT_NOT_FOUND", "Không tìm thấy sổ tiết kiệm.");
          }
          if (freshSavingData.ownerUid !== uid) {
            throw new SavingsApiError(
              403,
              "PERMISSION_DENIED",
              "Bạn không có quyền tất toán sổ tiết kiệm này."
            );
          }
          if (freshSavingData.type !== "SAVING") {
            throw new SavingsApiError(
              400,
              "INVALID_ARGUMENT",
              "Tài khoản được chọn không phải sổ tiết kiệm."
            );
          }
          if (freshSavingData.status !== "ACTIVE") {
            throw new SavingsApiError(
              400,
              "ACCOUNT_NOT_ACTIVE",
              "Sổ tiết kiệm này đã tất toán hoặc không hoạt động."
            );
          }

          const principalAmount = Number(freshSavingData.balance ?? 0);
          if (!Number.isSafeInteger(principalAmount) || principalAmount <= 0) {
            throw new SavingsApiError(
              400,
              "INVALID_ARGUMENT",
              "Số dư gốc của sổ tiết kiệm không hợp lệ để tất toán."
            );
          }

          if (!freshCheckingSnap.exists) {
            throw new SavingsApiError(
              404,
              "ACCOUNT_NOT_FOUND",
              "Không tìm thấy tài khoản thanh toán nhận tiền."
            );
          }
          const freshCheckingData = freshCheckingSnap.data() ?? {};
          if (freshCheckingData.ownerUid !== uid) {
            throw new SavingsApiError(
              403,
              "PERMISSION_DENIED",
              "Tài khoản nhận tiền không thuộc quyền sở hữu của bạn."
            );
          }
          if (freshCheckingData.type !== "CHECKING") {
            throw new SavingsApiError(
              400,
              "INVALID_ARGUMENT",
              "Tài khoản nhận tiền tất toán phải là CHECKING."
            );
          }
          if (freshCheckingData.status !== "ACTIVE") {
            throw new SavingsApiError(
              400,
              "ACCOUNT_NOT_ACTIVE",
              "Tài khoản thanh toán nhận tiền hiện không hoạt động."
            );
          }

          const openedAtMs =
            toDateSafe(savingInfo.openedAt ?? freshSavingData.createdAt)?.getTime() ?? nowMs;
          const maturityDateMs = toDateSafe(savingInfo.maturityDate)?.getTime() ?? nowMs;

          let termAnnualRate = Number.NaN;
          if (rateByIdSnap?.exists) {
            termAnnualRate = Number(rateByIdSnap.data()?.annualRate);
          } else if (termRateSnap && !termRateSnap.empty) {
            termAnnualRate = Number(termRateSnap.docs[0].data()?.annualRate);
          } else if (typeof savingInfo.annualRate === "number") {
            termAnnualRate = savingInfo.annualRate;
          }
          if (!Number.isFinite(termAnnualRate) || termAnnualRate < 0) {
            termAnnualRate = 0;
          }

          const rawDemandRate = !demandRateSnap.empty
            ? Number(demandRateSnap.docs[0].data()?.annualRate)
            : DEFAULT_DEMAND_ANNUAL_RATE;
          const demandAnnualRate =
            Number.isFinite(rawDemandRate) && rawDemandRate >= 0
              ? rawDemandRate
              : DEFAULT_DEMAND_ANNUAL_RATE;

          const settlement = computeSavingsSettlementAmounts({
            principalAmount,
            termMonths,
            termAnnualRate,
            demandAnnualRate,
            openedAtMs,
            maturityDateMs,
            nowMs,
          });

          const currentCheckingBalance = Number(freshCheckingData.balance ?? 0);
          const newCheckingBalance = currentCheckingBalance + settlement.totalPayoutAmount;
          const savingAccountNumber = asTrimmedString(freshSavingData.accountNumber);
          const checkingAccountNumber = asTrimmedString(freshCheckingData.accountNumber);
          const ownerName = asTrimmedString(freshUser.fullName);
          const createdAtTs = admin.firestore.FieldValue.serverTimestamp();
          const settledAtTs = admin.firestore.Timestamp.fromMillis(nowMs);
          const txRef = db.collection("transactions").doc();
          const txId = txRef.id;
          const settlementModeLabel = settlement.earlySettlement ? "trước hạn" : "đúng hạn";
          const note = `Tất toán ${settlementModeLabel} sổ tiết kiệm ${savingAccountNumber || savingAccountId} (Gốc ${formatVnd(principalAmount)} + Lãi ${formatVnd(settlement.interestAmount)})`;

          tx.update(savingRef, {
            status: "CLOSED",
            balance: 0,
            "saving.settledAt": settledAtTs,
            "saving.settledInterestAmount": settlement.interestAmount,
            "saving.earlySettlement": settlement.earlySettlement,
          });
          tx.set(savingRef.collection("ledger").doc(), {
            txId,
            direction: "OUT",
            amount: principalAmount,
            balanceAfter: 0,
            description: `Tất toán ${settlementModeLabel} sổ tiết kiệm`,
            createdAt: createdAtTs,
          });

          tx.update(checkingRef, { balance: newCheckingBalance });
          tx.set(checkingRef.collection("ledger").doc(), {
            txId,
            direction: "IN",
            amount: settlement.totalPayoutAmount,
            balanceAfter: newCheckingBalance,
            description: note,
            createdAt: createdAtTs,
          });

          tx.set(txRef, {
            type: "TRANSFER_INTERNAL",
            fromAccountId: savingAccountId,
            ...(savingAccountNumber ? { fromAccountNumber: savingAccountNumber } : {}),
            toAccountId: toCheckingAccountId,
            ...(checkingAccountNumber ? { toAccountNumber: checkingAccountNumber } : {}),
            ...(ownerName ? { toOwnerName: ownerName } : {}),
            amount: settlement.totalPayoutAmount,
            fee: 0,
            note,
            status: "SUCCESS",
            requiresBiometric: false,
            biometricPassed: false,
            otpVerified: false,
            faceChecked: false,
            meta: {
              savingAccountId,
              principalAmount,
              interestAmount: settlement.interestAmount,
              earlySettlement: settlement.earlySettlement,
              appliedAnnualRate: settlement.appliedAnnualRate,
            },
            initiatedBy: uid,
            createdAt: createdAtTs,
          });

          enqueueUserNotification(
            tx,
            db,
            uid,
            {
              category: "SAVING",
              direction: "IN",
              title: "Tất toán sổ tiết kiệm thành công",
              body: `Sổ tiết kiệm ${savingAccountNumber || savingAccountId} đã tất toán ${settlementModeLabel} (${settlement.appliedAnnualRate}%/năm). Tổng tiền ${formatVnd(settlement.totalPayoutAmount)} (gồm lãi ${formatVnd(settlement.interestAmount)}) đã được chuyển vào TK ${checkingAccountNumber || toCheckingAccountId}. Số dư hiện tại: ${formatVnd(newCheckingBalance)}.`,
              amount: settlement.totalPayoutAmount,
              balanceAfter: newCheckingBalance,
              accountNumber: checkingAccountNumber || savingAccountNumber || null,
              txId,
              deepLink: `novapay://receipt/${txId}`,
            },
            createdAtTs
          );

          return {
            txId,
            principalAmount,
            interestAmount: settlement.interestAmount,
            totalPayoutAmount: settlement.totalPayoutAmount,
            earlySettlement: settlement.earlySettlement,
            appliedAnnualRate: settlement.appliedAnnualRate,
          };
        });

        res.status(200).json(result);
      } catch (err) {
        if (err instanceof PinServiceError || err instanceof SavingsApiError) {
          sendApiError(res, err.statusCode, err.errorCode, err.message);
          return;
        }
        const message = err instanceof Error ? err.message : String(err);
        console.error("[NovaPay Savings Settle Error]:", message);
        sendApiError(res, 500, "INTERNAL_ERROR", "Không thể tất toán sổ tiết kiệm.");
      }
    }
  );

  return router;
}

export const savingsRouter = createSavingsRouter();
