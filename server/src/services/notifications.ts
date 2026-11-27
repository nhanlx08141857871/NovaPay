import admin from "firebase-admin";

export type NotificationCategory = "BALANCE" | "SECURITY" | "SAVING" | "EKYC";
export type NotificationDirection = "IN" | "OUT" | null;

export interface NotificationPayload {
  category: NotificationCategory;
  direction?: NotificationDirection;
  title: string;
  body: string;
  amount?: number | null;
  balanceAfter?: number | null;
  accountNumber?: string | null;
  txId?: string | null;
  deepLink?: string | null;
}

export interface NotificationDocument {
  category: NotificationCategory;
  direction: NotificationDirection;
  title: string;
  body: string;
  amount: number | null;
  balanceAfter: number | null;
  accountNumber: string | null;
  txId: string | null;
  deepLink: string | null;
  read: boolean;
  createdAt: FirebaseFirestore.FieldValue | FirebaseFirestore.Timestamp;
}

export interface BalanceNotificationParams {
  direction: "IN" | "OUT";
  title: string;
  body: string;
  amount: number;
  balanceAfter: number;
  accountNumber?: string | null;
  txId?: string | null;
  deepLink?: string | null;
  category?: "BALANCE" | "SAVING";
  createdAt?: FirebaseFirestore.FieldValue | FirebaseFirestore.Timestamp;
}

export interface TransactionNotificationParams {
  type: string;
  txId: string;
  amount: number;
  fee?: number;
  senderUid: string;
  senderAccountNumber?: string | null;
  senderBalanceAfter: number;
  recipientUid?: string | null;
  recipientAccountNumber?: string | null;
  recipientBalanceAfter?: number | null;
  recipientName?: string | null;
  senderName?: string | null;
  note?: string;
  createdAt?: FirebaseFirestore.FieldValue | FirebaseFirestore.Timestamp;
}

const MAX_TITLE_LENGTH = 160;
const MAX_BODY_LENGTH = 600;

export function formatVnd(amount: number): string {
  const safe = Number.isFinite(amount) ? Math.trunc(amount) : 0;
  const sign = safe < 0 ? "-" : "";
  const absStr = Math.abs(safe)
    .toString()
    .replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  return `${sign}${absStr} ₫`;
}

function clampText(value: string, maxLength: number): string {
  const trimmed = value.trim();
  if (trimmed.length <= maxLength) {
    return trimmed;
  }
  return trimmed.slice(0, maxLength - 1).trimEnd() + "…";
}

function normalizeNullableString(value: string | null | undefined): string | null {
  if (typeof value !== "string") {
    return null;
  }
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function normalizeNullableInteger(value: number | null | undefined): number | null {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    return null;
  }
  return Math.trunc(value);
}

export function buildNotificationDoc(
  payload: NotificationPayload,
  createdAt?: FirebaseFirestore.FieldValue | FirebaseFirestore.Timestamp
): NotificationDocument {
  return {
    category: payload.category,
    direction: payload.direction ?? null,
    title: clampText(payload.title, MAX_TITLE_LENGTH),
    body: clampText(payload.body, MAX_BODY_LENGTH),
    amount: normalizeNullableInteger(payload.amount),
    balanceAfter: normalizeNullableInteger(payload.balanceAfter),
    accountNumber: normalizeNullableString(payload.accountNumber),
    txId: normalizeNullableString(payload.txId),
    deepLink: normalizeNullableString(payload.deepLink),
    read: false,
    createdAt: createdAt ?? admin.firestore.FieldValue.serverTimestamp(),
  };
}

export function buildBalanceNotificationDoc(
  params: BalanceNotificationParams
): NotificationDocument {
  return buildNotificationDoc(
    {
      category: params.category ?? "BALANCE",
      direction: params.direction,
      title: params.title,
      body: params.body,
      amount: params.amount,
      balanceAfter: params.balanceAfter,
      accountNumber: params.accountNumber ?? null,
      txId: params.txId ?? null,
      deepLink: params.deepLink ?? (params.txId ? `novapay://receipt/${params.txId}` : null),
    },
    params.createdAt
  );
}

function buildSenderTitle(type: string): string {
  switch (type) {
    case "TRANSFER_INTERNAL":
    case "TRANSFER_INTERBANK":
      return "Chuyển tiền thành công";
    case "BILL":
      return "Thanh toán hóa đơn thành công";
    case "TOPUP":
      return "Nạp tiền điện thoại thành công";
    case "SERVICE_PAYMENT":
      return "Thanh toán dịch vụ thành công";
    case "MORTGAGE_PAYMENT":
      return "Thanh toán khoản vay thành công";
    case "WITHDRAW":
      return "Rút tiền thành công";
    default:
      return "Biến động số dư tài khoản";
  }
}

export function enqueueUserNotification(
  tx: FirebaseFirestore.Transaction,
  db: FirebaseFirestore.Firestore,
  uid: string,
  payload: NotificationPayload,
  createdAt?: FirebaseFirestore.FieldValue | FirebaseFirestore.Timestamp
): string {
  const notifRef = db.collection("users").doc(uid).collection("notifications").doc();
  tx.set(notifRef, buildNotificationDoc(payload, createdAt));
  return notifRef.id;
}

export function enqueueTransactionNotifications(
  tx: FirebaseFirestore.Transaction,
  db: FirebaseFirestore.Firestore,
  params: TransactionNotificationParams
): { senderNotifId: string; recipientNotifId: string | null } {
  const createdAt = params.createdAt ?? admin.firestore.FieldValue.serverTimestamp();
  const totalOut = Math.trunc(params.amount) + Math.trunc(params.fee ?? 0);
  const senderAccLabel = normalizeNullableString(params.senderAccountNumber);
  const recipientAccLabel = normalizeNullableString(params.recipientAccountNumber);
  const recipientName = normalizeNullableString(params.recipientName);
  const senderName = normalizeNullableString(params.senderName);
  const noteText = normalizeNullableString(params.note);
  const deepLink = `novapay://receipt/${params.txId}`;

  const senderTitle = buildSenderTitle(params.type);
  const targetDetail = recipientName
    ? ` tới ${recipientName}${recipientAccLabel ? ` (${recipientAccLabel})` : ""}`
    : recipientAccLabel
      ? ` tới ${recipientAccLabel}`
      : "";
  const senderBody = `TK ${senderAccLabel ?? "thanh toán"} biến động -${formatVnd(totalOut)}${targetDetail}. Số dư hiện tại: ${formatVnd(params.senderBalanceAfter)}.${noteText ? ` Nội dung: ${noteText}` : ""}`;

  const senderNotifRef = db.collection("users").doc(params.senderUid).collection("notifications").doc();
  tx.set(
    senderNotifRef,
    buildBalanceNotificationDoc({
      direction: "OUT",
      title: senderTitle,
      body: senderBody,
      amount: totalOut,
      balanceAfter: params.senderBalanceAfter,
      accountNumber: senderAccLabel,
      txId: params.txId,
      deepLink,
      createdAt,
    })
  );

  let recipientNotifId: string | null = null;
  if (
    params.type === "TRANSFER_INTERNAL" &&
    typeof params.recipientUid === "string" &&
    params.recipientUid.trim().length > 0 &&
    typeof params.recipientBalanceAfter === "number" &&
    Number.isFinite(params.recipientBalanceAfter)
  ) {
    const recipientUid = params.recipientUid.trim();
    const recipientNotifRef = db
      .collection("users")
      .doc(recipientUid)
      .collection("notifications")
      .doc();
    const fromDetail = senderName
      ? ` từ ${senderName}${senderAccLabel ? ` (${senderAccLabel})` : ""}`
      : senderAccLabel
        ? ` từ TK ${senderAccLabel}`
        : "";
    const recipientBody = `TK ${recipientAccLabel ?? "thanh toán"} biến động +${formatVnd(params.amount)}${fromDetail}. Số dư hiện tại: ${formatVnd(params.recipientBalanceAfter)}.${noteText ? ` Nội dung: ${noteText}` : ""}`;

    tx.set(
      recipientNotifRef,
      buildBalanceNotificationDoc({
        direction: "IN",
        title: "Nhận tiền chuyển khoản",
        body: recipientBody,
        amount: params.amount,
        balanceAfter: params.recipientBalanceAfter,
        accountNumber: recipientAccLabel,
        txId: params.txId,
        deepLink,
        createdAt,
      })
    );
    recipientNotifId = recipientNotifRef.id;
  }

  return {
    senderNotifId: senderNotifRef.id,
    recipientNotifId,
  };
}

export async function createUserNotification(
  db: FirebaseFirestore.Firestore,
  uid: string,
  payload: NotificationPayload,
  createdAt?: FirebaseFirestore.FieldValue | FirebaseFirestore.Timestamp
): Promise<string> {
  const notifRef = db.collection("users").doc(uid).collection("notifications").doc();
  await notifRef.set(buildNotificationDoc(payload, createdAt));
  return notifRef.id;
}
