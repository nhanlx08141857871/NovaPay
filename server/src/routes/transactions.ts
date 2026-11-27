import { RequestHandler, Response, Router } from "express";
import admin from "firebase-admin";
import { AuthenticatedRequest, requireAuth, sendApiError } from "../middleware/auth";
import { EmailSendError, EmailService, emailService as defaultEmailService } from "../services/email";
import {
  EkycFaceNotFoundError,
  FaceCheckService,
  FaceCheckUnavailableError,
  faceCheckService as defaultFaceCheckService,
  isPlaceholderOrEmptyEkycImage,
  stripBase64DataUrlPrefix,
} from "../services/faceCheck";
import {
  DRAFT_RETENTION_MS,
  FACE_CHECK_MAX_ATTEMPTS,
  HIGH_VALUE_THRESHOLD_VND,
  MAX_GATEWAY_DEPOSIT_VND,
  OTP_EXPIRY_SECONDS,
  OTP_MAX_ATTEMPTS,
  OTP_MAX_RESENDS,
  OTP_RESEND_COOLDOWN_SECONDS,
  OtpService,
  otpService as defaultOtpService,
} from "../services/otp";
import {
  StripeService,
  StripeServiceError,
  stripeService as defaultStripeService,
} from "../services/stripe";
import { enqueueTransactionNotifications } from "../services/notifications";

const VN_OFFSET_MS = 7 * 60 * 60 * 1000;
const DAY_MS = 24 * 60 * 60 * 1000;

export type TransactionType =
  | "TRANSFER_INTERNAL"
  | "TRANSFER_INTERBANK"
  | "DEPOSIT"
  | "WITHDRAW"
  | "BILL"
  | "TOPUP"
  | "SERVICE_PAYMENT"
  | "MORTGAGE_PAYMENT"
  | "GATEWAY";

const ALLOWED_INITIATE_TYPES: ReadonlySet<string> = new Set<TransactionType>([
  "TRANSFER_INTERNAL",
  "TRANSFER_INTERBANK",
  "WITHDRAW",
  "BILL",
  "TOPUP",
  "SERVICE_PAYMENT",
  "MORTGAGE_PAYMENT",
  "GATEWAY",
]);

export interface MortgageState {
  loanAmount?: number;
  outstandingBalance: number;
  termMonths?: number;
  annualRate?: number;
  paymentFrequency: "MONTHLY" | "BIWEEKLY" | string;
  installmentAmount?: number;
  nextDueDate: unknown;
}

export interface TransactionsRouteDeps {
  getDb?: () => FirebaseFirestore.Firestore;
  authMiddleware?: RequestHandler;
  otpService?: OtpService;
  emailService?: EmailService;
  stripeService?: StripeService;
  faceCheckService?: FaceCheckService;
  now?: () => number;
}

class RouteApiError extends Error {
  readonly statusCode: number;
  readonly errorCode: string;

  constructor(statusCode: number, errorCode: string, message: string) {
    super(message);
    this.name = "RouteApiError";
    this.statusCode = statusCode;
    this.errorCode = errorCode;
  }
}

function toDateSafe(value: unknown): Date | null {
  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : value;
  }
  if (value && typeof value === "object") {
    const maybeTimestamp = value as { toDate?: () => Date; _seconds?: number; seconds?: number };
    if (typeof maybeTimestamp.toDate === "function") {
      const d = maybeTimestamp.toDate();
      return d instanceof Date && !Number.isNaN(d.getTime()) ? d : null;
    }
    const sec =
      typeof maybeTimestamp._seconds === "number"
        ? maybeTimestamp._seconds
        : typeof maybeTimestamp.seconds === "number"
          ? maybeTimestamp.seconds
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

/**
 * Computes [00:00:00.000+07:00, +24h) bounds for Asia/Ho_Chi_Minh independent of OS timezone.
 */
export function getHoChiMinhDayBounds(nowMs: number = Date.now()): {
  startUtc: Date;
  endUtc: Date;
} {
  const shifted = new Date(nowMs + VN_OFFSET_MS);
  const startMs =
    Date.UTC(shifted.getUTCFullYear(), shifted.getUTCMonth(), shifted.getUTCDate()) - VN_OFFSET_MS;
  return {
    startUtc: new Date(startMs),
    endUtc: new Date(startMs + DAY_MS),
  };
}

/**
 * Computes updated outstanding balance, next due date, and closure status after a mortgage payment.
 */
export function computeMortgageAfterPayment(
  mortgage: MortgageState,
  amount: number,
  nowMs: number = Date.now()
): {
  newOutstandingBalance: number;
  newNextDueDate: Date;
  shouldCloseAccount: boolean;
} {
  if (!Number.isSafeInteger(amount) || amount <= 0) {
    throw new RouteApiError(400, "INVALID_ARGUMENT", "Số tiền trả nợ vay phải là số nguyên dương.");
  }
  if (
    typeof mortgage.outstandingBalance !== "number" ||
    !Number.isSafeInteger(mortgage.outstandingBalance) ||
    amount > mortgage.outstandingBalance
  ) {
    throw new RouteApiError(
      400,
      "INVALID_ARGUMENT",
      "Số tiền trả nợ vượt quá dư nợ gốc còn lại."
    );
  }

  const newOutstandingBalance = mortgage.outstandingBalance - amount;
  const shouldCloseAccount = newOutstandingBalance === 0;
  const baseDueDate = toDateSafe(mortgage.nextDueDate) ?? new Date(nowMs);

  let newNextDueDate: Date;
  if (mortgage.paymentFrequency === "BIWEEKLY") {
    newNextDueDate = new Date(baseDueDate.getTime() + 14 * DAY_MS);
  } else {
    // MONTHLY: cộng 1 tháng theo lịch tại múi giờ Asia/Ho_Chi_Minh (UTC+7), kẹp ngày cuối tháng.
    const shifted = new Date(baseDueDate.getTime() + VN_OFFSET_MS);
    const targetYear = shifted.getUTCFullYear();
    const targetMonth = shifted.getUTCMonth() + 1;
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
    newNextDueDate = new Date(nextShiftedMs - VN_OFFSET_MS);
  }

  return {
    newOutstandingBalance,
    newNextDueDate,
    shouldCloseAccount,
  };
}

function asTrimmedString(val: unknown): string {
  return typeof val === "string" ? val.trim() : "";
}

function asPlainObject(val: unknown): Record<string, unknown> {
  return val && typeof val === "object" && !Array.isArray(val)
    ? (val as Record<string, unknown>)
    : {};
}

function buildDefaultLedgerDescription(
  type: TransactionType,
  note: string,
  direction: "IN" | "OUT"
): string {
  if (note) {
    return note;
  }
  switch (type) {
    case "TRANSFER_INTERNAL":
      return direction === "IN" ? "Nhận chuyển khoản nội bộ" : "Chuyển khoản nội bộ";
    case "TRANSFER_INTERBANK":
      return "Chuyển khoản liên ngân hàng";
    case "WITHDRAW":
      return "Rút tiền từ tài khoản thanh toán";
    case "BILL":
      return "Thanh toán hóa đơn tiện ích";
    case "TOPUP":
      return "Nạp tiền điện thoại";
    case "SERVICE_PAYMENT":
      return "Thanh toán dịch vụ / đơn hàng";
    case "MORTGAGE_PAYMENT":
      return direction === "IN" ? "Thu nợ khoản vay thế chấp" : "Thanh toán khoản vay thế chấp";
    case "GATEWAY":
      return "Nạp tiền qua cổng thanh toán";
    default:
      return "Giao dịch NovaPay";
  }
}

export function createTransactionsRouter(deps?: TransactionsRouteDeps): Router {
  const router = Router();
  const getDb = deps?.getDb ?? (() => admin.firestore());
  const auth = deps?.authMiddleware ?? requireAuth;
  const otpSvc = deps?.otpService ?? defaultOtpService;
  const emailSvc = deps?.emailService ?? defaultEmailService;
  const stripeSvc = deps?.stripeService ?? defaultStripeService;
  const faceCheckSvc = deps?.faceCheckService ?? defaultFaceCheckService;
  const now = deps?.now ?? (() => Date.now());

  async function querySpentTodayVnd(
    tx: FirebaseFirestore.Transaction | null,
    db: FirebaseFirestore.Firestore,
    fromAccountId: string,
    nowMs: number
  ): Promise<number> {
    const { startUtc, endUtc } = getHoChiMinhDayBounds(nowMs);
    // CRITICAL INDEX RULE: chỉ query đúng 3 trường (fromAccountId, status, createdAt) theo firestore.indexes.json.
    const query = db
      .collection("transactions")
      .where("fromAccountId", "==", fromAccountId)
      .where("status", "==", "SUCCESS")
      .where("createdAt", ">=", admin.firestore.Timestamp.fromDate(startUtc))
      .where("createdAt", "<", admin.firestore.Timestamp.fromDate(endUtc));

    const querySnap = tx ? await tx.get(query) : await query.get();

    let spentToday = 0;
    for (const doc of querySnap.docs) {
      const txData = doc.data();
      if (txData.type === "GATEWAY" || txData.type === "DEPOSIT") {
        continue;
      }
      if (typeof txData.amount === "number" && Number.isFinite(txData.amount)) {
        spentToday += txData.amount;
      }
    }
    return spentToday;
  }

  router.post("/initiate", auth, async (req: AuthenticatedRequest, res: Response): Promise<void> => {
    try {
      const uid = req.user?.uid;
      if (!uid) {
        sendApiError(res, 401, "PERMISSION_DENIED", "Không xác định được người dùng.");
        return;
      }

      const body = asPlainObject(req.body);
      const rawType = asTrimmedString(body.type);
      if (!ALLOWED_INITIATE_TYPES.has(rawType)) {
        sendApiError(res, 400, "INVALID_ARGUMENT", "Loại giao dịch không hợp lệ.");
        return;
      }
      const type = rawType as TransactionType;

      const fromAccountId = asTrimmedString(body.fromAccountId);
      const amount = body.amount;
      if (
        !fromAccountId ||
        typeof amount !== "number" ||
        !Number.isSafeInteger(amount) ||
        amount <= 0
      ) {
        sendApiError(
          res,
          400,
          "INVALID_ARGUMENT",
          "Thiếu tài khoản nguồn hoặc số tiền giao dịch (VNĐ nguyên dương) không hợp lệ."
        );
        return;
      }

      const note = asTrimmedString(body.note);
      if (note.length > 500) {
        sendApiError(
          res,
          400,
          "INVALID_ARGUMENT",
          "Nội dung giao dịch không được vượt quá 500 ký tự."
        );
        return;
      }
      const meta = { ...asPlainObject(body.meta) };
      const db = getDb();
      const nowMs = now();

      const userSnap = await db.collection("users").doc(uid).get();
      if (!userSnap.exists) {
        sendApiError(res, 403, "PERMISSION_DENIED", "Không tìm thấy hồ sơ người dùng.");
        return;
      }
      const userData = userSnap.data() ?? {};
      if (userData.status !== "ACTIVE") {
        sendApiError(
          res,
          403,
          "PERMISSION_DENIED",
          "Tài khoản người dùng hiện không hoạt động hoặc đã bị khóa."
        );
        return;
      }

      if (type !== "GATEWAY") {
        const ekycStatus = asPlainObject(userData.ekyc).status;
        if (ekycStatus !== "VERIFIED") {
          sendApiError(
            res,
            403,
            "EKYC_REQUIRED",
            "Vui lòng hoàn tất định danh eKYC trước khi thực hiện giao dịch."
          );
          return;
        }
      }

      const fromAccSnap = await db.collection("accounts").doc(fromAccountId).get();
      if (!fromAccSnap.exists) {
        sendApiError(res, 404, "ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản nguồn.");
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
          "Chỉ tài khoản thanh toán (CHECKING) mới được làm nguồn giao dịch."
        );
        return;
      }
      if (fromAccData.status !== "ACTIVE") {
        sendApiError(res, 400, "ACCOUNT_NOT_ACTIVE", "Tài khoản thanh toán nguồn không hoạt động.");
        return;
      }

      let toAccountId: string | null = null;
      let toExternal: { bankCode: string; accountNumber: string; name: string } | null = null;

      if (type === "TRANSFER_INTERNAL") {
        const targetId = asTrimmedString(body.toAccountId);
        if (!targetId) {
          sendApiError(res, 400, "INVALID_ARGUMENT", "Thiếu tài khoản nhận nội bộ.");
          return;
        }
        if (targetId === fromAccountId) {
          sendApiError(
            res,
            400,
            "INVALID_ARGUMENT",
            "Không thể chuyển tiền nội bộ cho chính tài khoản nguồn."
          );
          return;
        }
        const toAccSnap = await db.collection("accounts").doc(targetId).get();
        if (!toAccSnap.exists) {
          sendApiError(res, 404, "ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản người nhận.");
          return;
        }
        const toAccData = toAccSnap.data() ?? {};
        if (toAccData.type !== "CHECKING") {
          sendApiError(
            res,
            400,
            "INVALID_ARGUMENT",
            "Tài khoản nhận nội bộ phải là tài khoản thanh toán (CHECKING)."
          );
          return;
        }
        if (toAccData.status !== "ACTIVE") {
          sendApiError(res, 400, "ACCOUNT_NOT_ACTIVE", "Tài khoản người nhận không hoạt động.");
          return;
        }
        const toOwnerUidInit = asTrimmedString(toAccData.ownerUid);
        if (toOwnerUidInit) {
          const toOwnerInitSnap = await db.collection("users").doc(toOwnerUidInit).get();
          if (toOwnerInitSnap.exists && toOwnerInitSnap.data()?.status === "LOCKED") {
            sendApiError(
              res,
              400,
              "ACCOUNT_NOT_ACTIVE",
              "Chủ tài khoản người nhận hiện đang bị khóa giao dịch."
            );
            return;
          }
        }
        toAccountId = targetId;
      } else if (type === "TRANSFER_INTERBANK") {
        const ext = asPlainObject(body.toExternal);
        const bankCode = asTrimmedString(ext.bankCode);
        const accountNumber = asTrimmedString(ext.accountNumber);
        const name = asTrimmedString(ext.name);
        if (
          !bankCode ||
          bankCode.length > 20 ||
          !accountNumber ||
          accountNumber.length < 4 ||
          accountNumber.length > 34 ||
          !/^[A-Za-z0-9-]+$/.test(accountNumber) ||
          !name ||
          name.length < 2 ||
          name.length > 120
        ) {
          sendApiError(
            res,
            400,
            "INVALID_ARGUMENT",
            "Thông tin tài khoản nhận liên ngân hàng không đầy đủ hoặc không hợp lệ."
          );
          return;
        }
        const bankSnap = await db.collection("banks").doc(bankCode.toUpperCase()).get();
        if (!bankSnap.exists) {
          sendApiError(res, 400, "INVALID_ARGUMENT", "Ngân hàng nhận không được hỗ trợ.");
          return;
        }
        toExternal = { bankCode: bankCode.toUpperCase(), accountNumber, name };
      } else if (type === "BILL") {
        const billId = asTrimmedString(meta.billId);
        const billCode = asTrimmedString(meta.billCode);
        let billDoc: FirebaseFirestore.DocumentSnapshot | null = null;

        if (billId) {
          const snap = await db.collection("utility_bills").doc(billId).get();
          if (snap.exists) {
            billDoc = snap;
          }
        } else if (billCode) {
          const snap = await db
            .collection("utility_bills")
            .where("customerCode", "==", billCode)
            .where("status", "==", "UNPAID")
            .limit(1)
            .get();
          if (!snap.empty) {
            billDoc = snap.docs[0];
          }
        } else {
          sendApiError(res, 400, "INVALID_ARGUMENT", "Thiếu mã hóa đơn (billId hoặc billCode).");
          return;
        }

        if (!billDoc) {
          sendApiError(res, 400, "INVALID_ARGUMENT", "Không tìm thấy hóa đơn cần thanh toán.");
          return;
        }
        const billData = billDoc.data() ?? {};
        if (billData.status !== "UNPAID") {
          sendApiError(res, 400, "INVALID_ARGUMENT", "Hóa đơn này đã được thanh toán.");
          return;
        }
        if (billData.amount !== amount) {
          sendApiError(
            res,
            400,
            "INVALID_ARGUMENT",
            "Số tiền thanh toán không khớp với số tiền trên hóa đơn."
          );
          return;
        }
        meta.billId = billDoc.id;
        if (billData.customerCode && !meta.billCode) {
          meta.billCode = String(billData.customerCode);
        }
      } else if (type === "TOPUP") {
        const phone = asTrimmedString(meta.phone);
        if (!phone || !/^[0-9+\s-]{8,15}$/.test(phone)) {
          sendApiError(res, 400, "INVALID_ARGUMENT", "Số điện thoại nạp tiền không hợp lệ.");
          return;
        }
        meta.phone = phone;
      } else if (type === "SERVICE_PAYMENT") {
        const itemId = asTrimmedString(meta.itemId);
        const rawQty = meta.quantity === undefined ? 1 : meta.quantity;
        if (
          !itemId ||
          typeof rawQty !== "number" ||
          !Number.isSafeInteger(rawQty) ||
          rawQty < 1
        ) {
          sendApiError(
            res,
            400,
            "INVALID_ARGUMENT",
            "Thông tin dịch vụ (itemId, quantity) không hợp lệ."
          );
          return;
        }
        const itemSnap = await db.collection("service_catalog").doc(itemId).get();
        if (!itemSnap.exists) {
          sendApiError(res, 400, "INVALID_ARGUMENT", "Không tìm thấy dịch vụ trong danh mục.");
          return;
        }
        const itemData = itemSnap.data() ?? {};
        const price = itemData.price;
        if (typeof price !== "number" || !Number.isSafeInteger(price) || amount !== price * rawQty) {
          sendApiError(
            res,
            400,
            "INVALID_ARGUMENT",
            "Tổng số tiền thanh toán không khớp đơn giá dịch vụ."
          );
          return;
        }
        meta.itemId = itemId;
        meta.quantity = rawQty;
      } else if (type === "MORTGAGE_PAYMENT") {
        const mortgageAccountId = asTrimmedString(meta.mortgageAccountId);
        if (!mortgageAccountId) {
          sendApiError(res, 400, "INVALID_ARGUMENT", "Thiếu mã tài khoản vay thế chấp.");
          return;
        }
        const mortSnap = await db.collection("accounts").doc(mortgageAccountId).get();
        if (!mortSnap.exists) {
          sendApiError(res, 404, "ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản vay thế chấp.");
          return;
        }
        const mortData = mortSnap.data() ?? {};
        if (mortData.ownerUid !== uid) {
          sendApiError(
            res,
            403,
            "PERMISSION_DENIED",
            "Bạn không có quyền thanh toán khoản vay này."
          );
          return;
        }
        if (mortData.type !== "MORTGAGE") {
          sendApiError(res, 400, "INVALID_ARGUMENT", "Tài khoản đích không phải khoản vay thế chấp.");
          return;
        }
        if (mortData.status !== "ACTIVE") {
          sendApiError(res, 400, "ACCOUNT_NOT_ACTIVE", "Khoản vay thế chấp không ở trạng thái hoạt động.");
          return;
        }
        const mortgageInfo = asPlainObject(mortData.mortgage);
        const outstanding = mortgageInfo.outstandingBalance;
        if (
          typeof outstanding !== "number" ||
          !Number.isSafeInteger(outstanding) ||
          amount > outstanding
        ) {
          sendApiError(
            res,
            400,
            "INVALID_ARGUMENT",
            "Số tiền trả nợ không hợp lệ hoặc vượt quá dư nợ hiện tại."
          );
          return;
        }
        meta.mortgageAccountId = mortgageAccountId;
      }

      const fee = 0;
      if (type !== "GATEWAY") {
        const balance = Number(fromAccData.balance ?? 0);
        if (balance < amount + fee) {
          sendApiError(res, 400, "INSUFFICIENT_FUNDS", "Số dư tài khoản thanh toán không đủ.");
          return;
        }

        const perTxnLimit = Number(fromAccData.perTxnLimit ?? 0);
        if (amount > perTxnLimit) {
          sendApiError(
            res,
            400,
            "LIMIT_EXCEEDED",
            "Số tiền giao dịch vượt quá hạn mức mỗi lần."
          );
          return;
        }

        const dailyLimit = Number(fromAccData.dailyLimit ?? 0);
        const spentToday = await querySpentTodayVnd(null, db, fromAccountId, nowMs);
        if (spentToday + amount > dailyLimit) {
          sendApiError(
            res,
            400,
            "LIMIT_EXCEEDED",
            "Tổng giao dịch trong ngày vượt quá hạn mức cho phép."
          );
          return;
        }
      }

      if (type === "GATEWAY" && amount > MAX_GATEWAY_DEPOSIT_VND) {
        sendApiError(res, 400, "LIMIT_EXCEEDED", "Số tiền nạp vượt quá mức tối đa cho mỗi lần nạp.");
        return;
      }

      const requiresFace = type !== "GATEWAY" && amount > HIGH_VALUE_THRESHOLD_VND;
      const requiresBiometric = requiresFace;

      const draftRef = db.collection("transaction_drafts").doc();
      const draftId = draftRef.id;

      const payload = {
        type,
        fromAccountId,
        toAccountId,
        toExternal,
        amount,
        fee,
        note,
        meta,
      };

      if (requiresFace) {
        await draftRef.set({
          uid,
          payload,
          status: "PENDING_FACE",
          requiresBiometric: true,
          requiresFace: true,
          otpHash: null,
          otpExpiresAt: null,
          otpSentAt: null,
          resendCount: 0,
          faceAttempts: 0,
          attempts: 0,
          faceChecked: false,
          stripeIntentId: null,
          txId: null,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          expireAt: admin.firestore.Timestamp.fromMillis(nowMs + DRAFT_RETENTION_MS),
        });

        res.status(200).json({
          draftId,
          requiresBiometric: true,
          requiresFace: true,
          otpExpiresAt: null,
        });
        return;
      }

      const recipientEmail = String(userData.email ?? req.user?.email ?? "").trim();
      const otp = otpSvc.generateOtpCode();
      const otpHash = otpSvc.hashOtp(otp, draftId);
      const otpExpiresAt = otpSvc.computeOtpExpiryEpochMs(nowMs);

      await draftRef.set({
        uid,
        payload,
        status: "PENDING_OTP",
        requiresBiometric: false,
        requiresFace: false,
        otpHash,
        otpExpiresAt,
        otpSentAt: nowMs,
        resendCount: 0,
        attempts: 0,
        faceChecked: false,
        stripeIntentId: null,
        txId: null,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        expireAt: admin.firestore.Timestamp.fromMillis(nowMs + DRAFT_RETENTION_MS),
      });

      try {
        await emailSvc.sendOtpEmail({
          toEmail: recipientEmail,
          toName: String(userData.fullName ?? ""),
          otp,
          expiresInSeconds: OTP_EXPIRY_SECONDS,
          transactionType: type,
          amountVnd: amount,
        });
      } catch (err) {
        await draftRef.update({ status: "EXPIRED" }).catch(() => undefined);
        const msg = err instanceof Error ? err.message : "Không thể gửi mã OTP qua Email.";
        sendApiError(
          res,
          502,
          err instanceof EmailSendError ? err.code : "EMAIL_SEND_FAILED",
          msg
        );
        return;
      }

      res.status(200).json({
        draftId,
        requiresBiometric: false,
        requiresFace: false,
        otpExpiresAt,
      });
    } catch (err) {
      if (err instanceof RouteApiError) {
        sendApiError(res, err.statusCode, err.errorCode, err.message);
        return;
      }
      const message = err instanceof Error ? err.message : String(err);
      console.error("[NovaPay Transactions Initiate Error]:", message);
      sendApiError(res, 500, "INTERNAL_ERROR", "Không thể khởi tạo giao dịch.");
    }
  });

  router.post(
    "/face-check",
    auth,
    async (req: AuthenticatedRequest, res: Response): Promise<void> => {
      try {
        const uid = req.user?.uid;
        if (!uid) {
          sendApiError(res, 401, "PERMISSION_DENIED", "Không xác định được người dùng.");
          return;
        }

        const body = asPlainObject(req.body);
        const draftId = asTrimmedString(body.draftId);
        const imageBase64 = asTrimmedString(body.imageBase64);
        const cleanLiveImage = imageBase64 ? stripBase64DataUrlPrefix(imageBase64) : "";
        if (!draftId || draftId.length > 128 || !cleanLiveImage) {
          sendApiError(res, 400, "INVALID_ARGUMENT", "Thiếu draftId hoặc dữ liệu ảnh khuôn mặt không hợp lệ.");
          return;
        }
        const estimatedImageBytes = Math.floor((cleanLiveImage.length * 3) / 4);
        if (estimatedImageBytes > 200 * 1024) {
          sendApiError(
            res,
            400,
            "INVALID_ARGUMENT",
            "Dung lượng ảnh xác thực khuôn mặt không được vượt quá 200KB."
          );
          return;
        }

        const db = getDb();
        const draftRef = db.collection("transaction_drafts").doc(draftId);
        const draftSnap = await draftRef.get();
        if (!draftSnap.exists) {
          sendApiError(res, 404, "DRAFT_EXPIRED", "Phiên giao dịch không tồn tại hoặc đã hết hạn.");
          return;
        }

        const draftData = draftSnap.data() ?? {};
        if (draftData.uid !== uid) {
          sendApiError(res, 403, "PERMISSION_DENIED", "Bạn không có quyền xác thực giao dịch này.");
          return;
        }
        if (draftData.status !== "PENDING_FACE") {
          sendApiError(
            res,
            400,
            "DRAFT_EXPIRED",
            "Giao dịch không ở trạng thái chờ xác thực khuôn mặt."
          );
          return;
        }

        const userSnap = await db.collection("users").doc(uid).get();
        const userData = userSnap.data() ?? {};
        if (!userSnap.exists || userData.status !== "ACTIVE") {
          sendApiError(res, 403, "PERMISSION_DENIED", "Tài khoản người dùng không hoạt động.");
          return;
        }
        if (asPlainObject(userData.ekyc).status !== "VERIFIED") {
          sendApiError(res, 403, "EKYC_REQUIRED", "Hồ sơ eKYC chưa được duyệt.");
          return;
        }

        const ekycSnap = await db.collection("ekyc_images").doc(uid).get();
        let rawEkycImage = ekycSnap.exists
          ? asTrimmedString(ekycSnap.data()?.imageBase64 || ekycSnap.data()?.faceImageBase64)
          : "";
        if (!rawEkycImage) {
          const profileSnap = await db.collection("ekyc_profiles").doc(uid).get();
          if (profileSnap.exists) {
            rawEkycImage = asTrimmedString(
              profileSnap.data()?.faceImageBase64 || profileSnap.data()?.imageBase64
            );
          }
        }
        const cleanEkycImage = rawEkycImage ? stripBase64DataUrlPrefix(rawEkycImage) : "";
        if (!cleanEkycImage || isPlaceholderOrEmptyEkycImage(cleanEkycImage)) {
          sendApiError(
            res,
            403,
            "EKYC_FACE_NOT_FOUND",
            "Tài khoản chưa có ảnh khuôn mặt gốc trong hồ sơ eKYC. Vui lòng cập nhật ảnh định danh eKYC trước khi thực hiện giao dịch."
          );
          return;
        }

        let faceResult;
        try {
          faceResult = await faceCheckSvc.verifyFaceMatch(cleanEkycImage, cleanLiveImage);
        } catch (err) {
          if (err instanceof EkycFaceNotFoundError) {
            sendApiError(res, err.statusCode, err.code, err.message);
            return;
          }
          if (err instanceof FaceCheckUnavailableError) {
            sendApiError(res, 503, err.code, err.message);
            return;
          }
          const errMsg =
            err instanceof Error
              ? err.message
              : "Dịch vụ xác thực khuôn mặt AI tạm thời không khả dụng. Vui lòng thử lại sau.";
          sendApiError(res, 503, "SERVICE_UNAVAILABLE", errMsg);
          return;
        }

        if (!faceResult.matched) {
          const faceAttempts = Number(draftData.faceAttempts ?? 0) + 1;
          if (faceAttempts >= FACE_CHECK_MAX_ATTEMPTS) {
            await draftRef.update({ faceAttempts, status: "EXPIRED" });
            sendApiError(
              res,
              429,
              "TOO_MANY_ATTEMPTS",
              "Bạn đã xác thực khuôn mặt không khớp quá 5 lần cho phép. Giao dịch đã bị khóa."
            );
            return;
          }
          await draftRef.update({ faceAttempts });
          res.status(403).json({
            matched: false,
            error: "FACE_MISMATCH",
            message: "Khuôn mặt không khớp với hồ sơ eKYC đã đăng ký. Vui lòng thử lại!",
            reason: faceResult.reason ?? null,
            attempts: faceAttempts,
            remainingAttempts: FACE_CHECK_MAX_ATTEMPTS - faceAttempts,
          });
          return;
        }

        await db.collection("face_checks").doc(draftId).set({
          uid,
          txId: null,
          imageBase64: cleanLiveImage,
          manualAuditRequired: Boolean(faceResult.manualAuditRequired),
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        const nowMs = now();
        const recipientEmail = String(userData.email ?? req.user?.email ?? "").trim();
        const payload = asPlainObject(draftData.payload);
        const otp = otpSvc.generateOtpCode();
        const otpHash = otpSvc.hashOtp(otp, draftId);
        const otpExpiresAt = otpSvc.computeOtpExpiryEpochMs(nowMs);

        try {
          await emailSvc.sendOtpEmail({
            toEmail: recipientEmail,
            toName: String(userData.fullName ?? ""),
            otp,
            expiresInSeconds: OTP_EXPIRY_SECONDS,
            transactionType: asTrimmedString(payload.type),
            amountVnd: typeof payload.amount === "number" ? payload.amount : undefined,
          });
        } catch (err) {
          const msg = err instanceof Error ? err.message : "Không thể gửi mã OTP qua Email.";
          sendApiError(
            res,
            502,
            err instanceof EmailSendError ? err.code : "EMAIL_SEND_FAILED",
            msg
          );
          return;
        }

        await draftRef.update({
          status: "PENDING_OTP",
          faceChecked: true,
          otpHash,
          otpExpiresAt,
          otpSentAt: nowMs,
          attempts: 0,
        });

        res.status(200).json({
          ok: true,
          otpExpiresAt,
        });
      } catch (err) {
        const message = err instanceof Error ? err.message : String(err);
        console.error("[NovaPay FaceCheck Route Error]:", message);
        sendApiError(res, 500, "INTERNAL_ERROR", "Không thể xử lý xác thực khuôn mặt.");
      }
    }
  );

  router.post(
    "/resend-otp",
    auth,
    async (req: AuthenticatedRequest, res: Response): Promise<void> => {
      try {
        const uid = req.user?.uid;
        if (!uid) {
          sendApiError(res, 401, "PERMISSION_DENIED", "Không xác định được người dùng.");
          return;
        }

        const body = asPlainObject(req.body);
        const draftId = asTrimmedString(body.draftId);
        if (!draftId || draftId.length > 128) {
          sendApiError(res, 400, "INVALID_ARGUMENT", "Thiếu hoặc sai định dạng draftId.");
          return;
        }

        const db = getDb();
        const draftRef = db.collection("transaction_drafts").doc(draftId);
        const draftSnap = await draftRef.get();
        if (!draftSnap.exists) {
          sendApiError(res, 404, "DRAFT_EXPIRED", "Phiên giao dịch không tồn tại hoặc đã hết hạn.");
          return;
        }

        const draftData = draftSnap.data() ?? {};
        if (draftData.uid !== uid) {
          sendApiError(res, 403, "PERMISSION_DENIED", "Bạn không có quyền thực hiện thao tác này.");
          return;
        }
        if (draftData.status === "PENDING_FACE") {
          sendApiError(res, 400, "FACE_REQUIRED", "Vui lòng hoàn tất xác thực khuôn mặt trước.");
          return;
        }
        if (draftData.status !== "PENDING_OTP") {
          sendApiError(res, 400, "DRAFT_EXPIRED", "Phiên giao dịch đã hết hạn hoặc đã hoàn tất.");
          return;
        }

        const userSnap = await db.collection("users").doc(uid).get();
        const userData = userSnap.data() ?? {};
        if (!userSnap.exists || userData.status !== "ACTIVE") {
          sendApiError(res, 403, "PERMISSION_DENIED", "Tài khoản người dùng không hoạt động.");
          return;
        }

        const nowMs = now();
        const resendCount = Number(draftData.resendCount ?? 0);
        if (resendCount >= OTP_MAX_RESENDS) {
          sendApiError(
            res,
            429,
            "TOO_MANY_ATTEMPTS",
            "Bạn đã yêu cầu gửi lại mã OTP quá số lần cho phép."
          );
          return;
        }
        const lastSentAt = typeof draftData.otpSentAt === "number" ? draftData.otpSentAt : null;
        if (lastSentAt !== null && nowMs - lastSentAt < OTP_RESEND_COOLDOWN_SECONDS * 1000) {
          sendApiError(
            res,
            429,
            "RESEND_TOO_SOON",
            `Vui lòng đợi ${OTP_RESEND_COOLDOWN_SECONDS} giây trước khi yêu cầu gửi lại mã OTP.`
          );
          return;
        }

        const recipientEmail = String(userData.email ?? req.user?.email ?? "").trim();
        const payload = asPlainObject(draftData.payload);
        const otp = otpSvc.generateOtpCode();
        const otpHash = otpSvc.hashOtp(otp, draftId);
        const otpExpiresAt = otpSvc.computeOtpExpiryEpochMs(nowMs);

        try {
          await emailSvc.sendOtpEmail({
            toEmail: recipientEmail,
            toName: String(userData.fullName ?? ""),
            otp,
            expiresInSeconds: OTP_EXPIRY_SECONDS,
            transactionType: asTrimmedString(payload.type),
            amountVnd: typeof payload.amount === "number" ? payload.amount : undefined,
          });
        } catch (err) {
          const msg = err instanceof Error ? err.message : "Không thể gửi lại mã OTP.";
          sendApiError(
            res,
            502,
            err instanceof EmailSendError ? err.code : "EMAIL_SEND_FAILED",
            msg
          );
          return;
        }

        await draftRef.update({
          otpHash,
          otpExpiresAt,
          otpSentAt: nowMs,
          resendCount: resendCount + 1,
          attempts: 0,
        });

        res.status(200).json({ otpExpiresAt });
      } catch (err) {
        const message = err instanceof Error ? err.message : String(err);
        console.error("[NovaPay Resend OTP Error]:", message);
        sendApiError(res, 500, "INTERNAL_ERROR", "Không thể gửi lại mã OTP.");
      }
    }
  );

  router.post("/confirm", auth, async (req: AuthenticatedRequest, res: Response): Promise<void> => {
    try {
      const uid = req.user?.uid;
      if (!uid) {
        sendApiError(res, 401, "PERMISSION_DENIED", "Không xác định được người dùng.");
        return;
      }

      const body = asPlainObject(req.body);
      const draftId = asTrimmedString(body.draftId);
      const otp = asTrimmedString(body.otp);
      const biometricPassed = body.biometricPassed;

      if (!draftId || draftId.length > 128 || !otp || otp.length > 32) {
        sendApiError(res, 400, "INVALID_ARGUMENT", "Thiếu hoặc sai định dạng draftId / mã OTP.");
        return;
      }
      if (biometricPassed !== undefined && typeof biometricPassed !== "boolean") {
        sendApiError(res, 400, "INVALID_ARGUMENT", "Giá trị biometricPassed không hợp lệ.");
        return;
      }

      const db = getDb();
      const draftRef = db.collection("transaction_drafts").doc(draftId);
      const draftSnap = await draftRef.get();
      if (!draftSnap.exists) {
        sendApiError(res, 404, "DRAFT_EXPIRED", "Phiên giao dịch không tồn tại hoặc đã hết hạn.");
        return;
      }

      const draftData = draftSnap.data() ?? {};
      if (draftData.uid !== uid) {
        sendApiError(res, 403, "PERMISSION_DENIED", "Bạn không có quyền xác nhận giao dịch này.");
        return;
      }

      if (draftData.status === "PENDING_FACE") {
        sendApiError(res, 400, "FACE_REQUIRED", "Giao dịch yêu cầu xác thực khuôn mặt trước.");
        return;
      }

      const currentAttempts = Number(draftData.attempts ?? 0);
      if (draftData.status === "EXPIRED" && currentAttempts >= OTP_MAX_ATTEMPTS) {
        sendApiError(
          res,
          429,
          "TOO_MANY_ATTEMPTS",
          "Bạn đã nhập sai OTP quá 5 lần. Giao dịch đã bị khóa."
        );
        return;
      }

      if (draftData.status !== "PENDING_OTP") {
        sendApiError(res, 400, "DRAFT_EXPIRED", "Phiên giao dịch đã hết hạn hoặc đã xử lý.");
        return;
      }

      if (draftData.requiresFace && !draftData.faceChecked) {
        sendApiError(res, 403, "FACE_REQUIRED", "Giao dịch chưa hoàn tất bước FaceCheck.");
        return;
      }

      if (draftData.requiresBiometric && biometricPassed !== true) {
        sendApiError(
          res,
          403,
          "BIOMETRIC_REQUIRED",
          "Giao dịch giá trị cao yêu cầu xác thực sinh trắc học thiết bị."
        );
        return;
      }

      if (currentAttempts >= OTP_MAX_ATTEMPTS) {
        await draftRef.update({ status: "EXPIRED" });
        sendApiError(
          res,
          429,
          "TOO_MANY_ATTEMPTS",
          "Bạn đã nhập sai OTP quá 5 lần. Giao dịch đã bị khóa."
        );
        return;
      }

      const nowMs = now();
      if (
        otpSvc.isOtpExpired(
          typeof draftData.otpExpiresAt === "number" ? draftData.otpExpiresAt : null,
          nowMs
        )
      ) {
        // Giữ nguyên draft.status = "PENDING_OTP" để khách hàng có thể gọi /resend-otp.
        sendApiError(res, 400, "OTP_EXPIRED", "Mã OTP đã hết hạn. Vui lòng gửi lại mã mới.");
        return;
      }

      const expectedHash = typeof draftData.otpHash === "string" ? draftData.otpHash : "";
      if (!otpSvc.verifyOtpHash(otp, expectedHash, draftId)) {
        const nextAttempts = await db.runTransaction(async (tx) => {
          const fresh = await tx.get(draftRef);
          const next = Number(fresh.data()?.attempts ?? 0) + 1;
          tx.update(draftRef, next >= OTP_MAX_ATTEMPTS ? { attempts: next, status: "EXPIRED" } : { attempts: next });
          return next;
        });
        if (nextAttempts >= OTP_MAX_ATTEMPTS) {
          sendApiError(
            res,
            429,
            "TOO_MANY_ATTEMPTS",
            "Bạn đã nhập sai OTP quá 5 lần. Giao dịch đã bị khóa."
          );
          return;
        }
        sendApiError(res, 400, "OTP_INVALID", "Mã OTP không chính xác.");
        return;
      }

      const payload = asPlainObject(draftData.payload);
      const type = asTrimmedString(payload.type) as TransactionType;
      const fromAccountId = asTrimmedString(payload.fromAccountId);
      const amount = Number(payload.amount ?? 0);
      const fee = Number(payload.fee ?? 0);
      const note = asTrimmedString(payload.note);
      const meta = { ...asPlainObject(payload.meta) };

      if (type === "GATEWAY") {
        const paymentIntent = await stripeSvc.createPaymentIntent({
          amountVnd: amount,
          draftId,
          uid,
          accountId: fromAccountId,
        });

        await draftRef.update({
          status: "PENDING_PAYMENT",
          stripeIntentId: paymentIntent.stripeIntentId,
          otpVerified: true,
        });

        res.status(200).json({
          txId: null,
          status: "PENDING_PAYMENT",
          clientSecret: paymentIntent.clientSecret,
        });
        return;
      }

      const txResult = await db.runTransaction(async (tx) => {
        // --- PHASE 1: TẤT CẢ tx.get PHẢI THỰC HIỆN TRƯỚC MỌI tx.set/tx.update ---
        const freshDraftSnap = await tx.get(draftRef);
        const userRef = db.collection("users").doc(uid);
        const userSnap = await tx.get(userRef);
        const fromAccRef = db.collection("accounts").doc(fromAccountId);
        const fromAccSnap = await tx.get(fromAccRef);

        const spentToday = await querySpentTodayVnd(tx, db, fromAccountId, nowMs);

        const toAccountId = asTrimmedString(payload.toAccountId);
        const toAccRef = toAccountId ? db.collection("accounts").doc(toAccountId) : null;
        const toAccSnap = toAccRef ? await tx.get(toAccRef) : null;
        const toOwnerUid =
          type === "TRANSFER_INTERNAL" && toAccSnap?.exists
            ? asTrimmedString(toAccSnap.data()?.ownerUid)
            : "";
        const toOwnerSnap = toOwnerUid
          ? await tx.get(db.collection("users").doc(toOwnerUid))
          : null;

        const billId = asTrimmedString(meta.billId);
        const billRef = type === "BILL" && billId ? db.collection("utility_bills").doc(billId) : null;
        const billSnap = billRef ? await tx.get(billRef) : null;

        const itemId = asTrimmedString(meta.itemId);
        const itemRef =
          type === "SERVICE_PAYMENT" && itemId
            ? db.collection("service_catalog").doc(itemId)
            : null;
        const itemSnap = itemRef ? await tx.get(itemRef) : null;

        const mortgageAccountId = asTrimmedString(meta.mortgageAccountId);
        const mortAccRef =
          type === "MORTGAGE_PAYMENT" && mortgageAccountId
            ? db.collection("accounts").doc(mortgageAccountId)
            : null;
        const mortAccSnap = mortAccRef ? await tx.get(mortAccRef) : null;

        // --- KIỂM TRA LẠI TOÀN BỘ ĐIỀU KIỆN BÊN TRONG TRANSACTION ---
        if (!freshDraftSnap.exists) {
          throw new RouteApiError(404, "DRAFT_EXPIRED", "Phiên giao dịch không tồn tại.");
        }
        const freshDraft = freshDraftSnap.data() ?? {};
        if (freshDraft.status !== "PENDING_OTP") {
          throw new RouteApiError(400, "DRAFT_EXPIRED", "Phiên giao dịch đã được xử lý hoặc hết hạn.");
        }

        const freshUser = userSnap.data() ?? {};
        if (!userSnap.exists || freshUser.status !== "ACTIVE") {
          throw new RouteApiError(403, "PERMISSION_DENIED", "Tài khoản người dùng không hoạt động.");
        }
        if (asPlainObject(freshUser.ekyc).status !== "VERIFIED") {
          throw new RouteApiError(403, "EKYC_REQUIRED", "Hồ sơ eKYC chưa được xác thực.");
        }

        if (!fromAccSnap.exists) {
          throw new RouteApiError(404, "ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản nguồn.");
        }
        const fromAccData = fromAccSnap.data() ?? {};
        if (fromAccData.ownerUid !== uid) {
          throw new RouteApiError(403, "PERMISSION_DENIED", "Không có quyền trên tài khoản nguồn.");
        }
        if (fromAccData.type !== "CHECKING") {
          throw new RouteApiError(400, "INVALID_ARGUMENT", "Tài khoản nguồn phải là CHECKING.");
        }
        if (fromAccData.status !== "ACTIVE") {
          throw new RouteApiError(400, "ACCOUNT_NOT_ACTIVE", "Tài khoản nguồn không hoạt động.");
        }

        const fromBalance = Number(fromAccData.balance ?? 0);
        if (fromBalance < amount + fee) {
          throw new RouteApiError(400, "INSUFFICIENT_FUNDS", "Số dư tài khoản không đủ.");
        }

        const perTxnLimit = Number(fromAccData.perTxnLimit ?? 0);
        if (amount > perTxnLimit) {
          throw new RouteApiError(400, "LIMIT_EXCEEDED", "Số tiền vượt hạn mức mỗi giao dịch.");
        }

        const dailyLimit = Number(fromAccData.dailyLimit ?? 0);
        if (spentToday + amount > dailyLimit) {
          throw new RouteApiError(400, "LIMIT_EXCEEDED", "Tổng giao dịch trong ngày vượt hạn mức.");
        }

        if (type === "TRANSFER_INTERNAL") {
          if (!toAccSnap || !toAccSnap.exists) {
            throw new RouteApiError(404, "ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản nhận.");
          }
          const toAccData = toAccSnap.data() ?? {};
          if (toAccData.type !== "CHECKING") {
            throw new RouteApiError(400, "INVALID_ARGUMENT", "Tài khoản nhận phải là CHECKING.");
          }
          if (toAccData.status !== "ACTIVE") {
            throw new RouteApiError(400, "ACCOUNT_NOT_ACTIVE", "Tài khoản nhận không hoạt động.");
          }
          if (toOwnerSnap?.exists && toOwnerSnap.data()?.status === "LOCKED") {
            throw new RouteApiError(
              400,
              "ACCOUNT_NOT_ACTIVE",
              "Chủ tài khoản người nhận hiện đang bị khóa giao dịch."
            );
          }
        } else if (type === "BILL") {
          if (!billSnap || !billSnap.exists) {
            throw new RouteApiError(400, "INVALID_ARGUMENT", "Không tìm thấy hóa đơn.");
          }
          const billData = billSnap.data() ?? {};
          if (billData.status !== "UNPAID" || billData.amount !== amount) {
            throw new RouteApiError(
              400,
              "INVALID_ARGUMENT",
              "Hóa đơn đã thanh toán hoặc số tiền không khớp."
            );
          }
        } else if (type === "SERVICE_PAYMENT") {
          if (!itemSnap || !itemSnap.exists) {
            throw new RouteApiError(400, "INVALID_ARGUMENT", "Dịch vụ không tồn tại.");
          }
          const itemData = itemSnap.data() ?? {};
          const quantity = Number(meta.quantity ?? 1);
          if (typeof itemData.price !== "number" || amount !== itemData.price * quantity) {
            throw new RouteApiError(400, "INVALID_ARGUMENT", "Đơn giá dịch vụ đã thay đổi.");
          }
        } else if (type === "MORTGAGE_PAYMENT") {
          if (!mortAccSnap || !mortAccSnap.exists) {
            throw new RouteApiError(404, "ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản vay.");
          }
          const mortData = mortAccSnap.data() ?? {};
          if (mortData.ownerUid !== uid) {
            throw new RouteApiError(403, "PERMISSION_DENIED", "Không có quyền trên khoản vay.");
          }
          if (mortData.type !== "MORTGAGE" || mortData.status !== "ACTIVE") {
            throw new RouteApiError(400, "ACCOUNT_NOT_ACTIVE", "Khoản vay không hoạt động.");
          }
        }

        // --- PHASE 2: THỰC THI GHI NGUYÊN TỬ ---
        const txRef = db.collection("transactions").doc();
        const txId = txRef.id;
        const nowTs = admin.firestore.FieldValue.serverTimestamp();
        const newFromBalance = fromBalance - (amount + fee);

        tx.update(fromAccRef, { balance: newFromBalance });
        tx.set(fromAccRef.collection("ledger").doc(), {
          txId,
          direction: "OUT",
          amount: amount + fee,
          balanceAfter: newFromBalance,
          description: buildDefaultLedgerDescription(type, note, "OUT"),
          createdAt: nowTs,
        });

        let newToBalance: number | null = null;
        if (type === "TRANSFER_INTERNAL" && toAccRef && toAccSnap) {
          const toBalance = Number(toAccSnap.data()?.balance ?? 0);
          newToBalance = toBalance + amount;
          tx.update(toAccRef, { balance: newToBalance });
          tx.set(toAccRef.collection("ledger").doc(), {
            txId,
            direction: "IN",
            amount,
            balanceAfter: newToBalance,
            description: buildDefaultLedgerDescription(type, note, "IN"),
            createdAt: nowTs,
          });
        } else if (type === "BILL" && billRef) {
          tx.update(billRef, {
            status: "PAID",
            txId,
            paidAt: nowTs,
          });
        } else if (type === "SERVICE_PAYMENT" && itemSnap) {
          const itemData = itemSnap.data() ?? {};
          const quantity = Number(meta.quantity ?? 1);
          const orderRef = db.collection("orders").doc();
          tx.set(orderRef, {
            uid,
            itemId,
            category: String(itemData.category ?? ""),
            quantity,
            totalAmount: amount,
            status: "PAID",
            txId,
            createdAt: nowTs,
          });
          meta.orderId = orderRef.id;
        } else if (type === "MORTGAGE_PAYMENT" && mortAccRef && mortAccSnap) {
          const mortData = mortAccSnap.data() ?? {};
          const mortgageInfo = asPlainObject(mortData.mortgage) as unknown as MortgageState;
          const mortUpdate = computeMortgageAfterPayment(mortgageInfo, amount, nowMs);

          tx.update(mortAccRef, {
            "mortgage.outstandingBalance": mortUpdate.newOutstandingBalance,
            "mortgage.nextDueDate": admin.firestore.Timestamp.fromDate(mortUpdate.newNextDueDate),
            ...(mortUpdate.shouldCloseAccount ? { status: "CLOSED" } : {}),
          });
          tx.set(mortAccRef.collection("ledger").doc(), {
            txId,
            direction: "IN",
            amount,
            balanceAfter: mortUpdate.newOutstandingBalance,
            description: buildDefaultLedgerDescription(type, note, "IN"),
            createdAt: nowTs,
          });
        }

        const fromAccountNumber = asTrimmedString(fromAccData.accountNumber);
        const toExternal = payload.toExternal ? asPlainObject(payload.toExternal) : null;
        let toAccountNumber = "";
        let toOwnerName = "";

        if (type === "TRANSFER_INTERNAL" && toAccSnap?.exists) {
          toAccountNumber = asTrimmedString(toAccSnap.data()?.accountNumber);
          toOwnerName = toOwnerSnap?.exists ? asTrimmedString(toOwnerSnap.data()?.fullName) : "";
        } else if (type === "TRANSFER_INTERBANK" && toExternal) {
          toAccountNumber = asTrimmedString(toExternal.accountNumber);
          toOwnerName = asTrimmedString(toExternal.name);
        } else if (type === "BILL" && billSnap?.exists) {
          const billData = billSnap.data() ?? {};
          toAccountNumber =
            asTrimmedString(billData.customerCode) || asTrimmedString(meta.billCode);
          toOwnerName = asTrimmedString(billData.providerName);
        } else if (type === "TOPUP") {
          toAccountNumber = asTrimmedString(meta.phone);
        } else if (type === "SERVICE_PAYMENT" && itemSnap?.exists) {
          toAccountNumber = asTrimmedString(meta.orderId);
          toOwnerName = asTrimmedString(itemSnap.data()?.title);
        } else if (type === "MORTGAGE_PAYMENT" && mortAccSnap?.exists) {
          toAccountNumber = asTrimmedString(mortAccSnap.data()?.accountNumber);
          toOwnerName = asTrimmedString(freshUser.fullName);
        }

        tx.set(txRef, {
          type,
          fromAccountId,
          ...(fromAccountNumber ? { fromAccountNumber } : {}),
          ...(toAccountId ? { toAccountId } : {}),
          ...(toAccountNumber ? { toAccountNumber } : {}),
          ...(toOwnerName ? { toOwnerName } : {}),
          ...(toExternal ? { toExternal } : {}),
          amount,
          fee,
          note,
          status: "SUCCESS",
          requiresBiometric: Boolean(freshDraft.requiresBiometric),
          biometricPassed: Boolean(biometricPassed),
          otpVerified: true,
          faceChecked: Boolean(freshDraft.faceChecked),
          meta,
          initiatedBy: uid,
          createdAt: nowTs,
        });

        if (freshDraft.faceChecked) {
          tx.update(db.collection("face_checks").doc(draftId), { txId });
        }

        tx.update(draftRef, {
          status: "CONFIRMED",
          txId,
          otpVerified: true,
          biometricPassed: Boolean(biometricPassed),
        });

        enqueueTransactionNotifications(tx, db, {
          type,
          txId,
          amount,
          fee,
          senderUid: uid,
          senderAccountNumber: fromAccountNumber || null,
          senderBalanceAfter: newFromBalance,
          recipientUid: type === "TRANSFER_INTERNAL" ? toOwnerUid || null : null,
          recipientAccountNumber: toAccountNumber || null,
          recipientBalanceAfter: newToBalance,
          recipientName: toOwnerName || null,
          senderName: asTrimmedString(freshUser.fullName) || null,
          note,
          createdAt: nowTs,
        });

        return {
          txId,
          status: "SUCCESS",
          clientSecret: null,
        };
      });

      res.status(200).json(txResult);
    } catch (err) {
      if (err instanceof RouteApiError) {
        sendApiError(res, err.statusCode, err.errorCode, err.message);
        return;
      }
      if (err instanceof StripeServiceError) {
        sendApiError(res, err.statusCode, err.code, err.message);
        return;
      }
      const message = err instanceof Error ? err.message : String(err);
      console.error("[NovaPay Transactions Confirm Error]:", message);
      sendApiError(res, 500, "INTERNAL_ERROR", "Không thể xác nhận giao dịch.");
    }
  });

  return router;
}

export const transactionsRouter = createTransactionsRouter();
