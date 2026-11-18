import { NextFunction, Request, Response } from "express";
import admin from "firebase-admin";
import { isFirebaseReady } from "../config";

export type UserRole = "CUSTOMER" | "OFFICER";

export interface AuthenticatedUser {
  uid: string;
  email?: string;
  role: UserRole;
}

export interface AuthenticatedRequest extends Request {
  user?: AuthenticatedUser;
}

export function sendApiError(
  res: Response,
  statusCode: number,
  errorCode: string,
  message: string
): void {
  res.status(statusCode).json({
    error: errorCode,
    message,
  });
}

/**
 * Middleware xác thực Firebase ID Token bắt buộc cho mọi API bảo vệ (trừ GET /health).
 */
export async function requireAuth(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    sendApiError(res, 401, "PERMISSION_DENIED", "Thiếu hoặc sai định dạng Bearer Token.");
    return;
  }

  const idToken = authHeader.slice("Bearer ".length).trim();
  if (!idToken) {
    sendApiError(res, 401, "PERMISSION_DENIED", "Firebase ID Token rỗng.");
    return;
  }

  if (!isFirebaseReady()) {
    sendApiError(
      res,
      503,
      "SERVICE_UNAVAILABLE",
      "Firebase Admin SDK chưa được cấu hình khóa dịch vụ trên máy chủ."
    );
    return;
  }

  try {
    const decoded = await admin.auth().verifyIdToken(idToken, true);
    const claimRole = decoded.role === "OFFICER" ? "OFFICER" : "CUSTOMER";
    req.user = {
      uid: decoded.uid,
      email: decoded.email,
      role: claimRole,
    };
    next();
  } catch (_err) {
    sendApiError(
      res,
      401,
      "PERMISSION_DENIED",
      "Firebase ID Token không hợp lệ, đã hết hạn hoặc tài khoản đã bị thu hồi."
    );
  }
}

/**
 * Middleware chỉ cho phép nhân viên ngân hàng (Custom Claim role === "OFFICER").
 */
export function requireOfficer(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): void {
  if (!req.user || req.user.role !== "OFFICER") {
    sendApiError(
      res,
      403,
      "PERMISSION_DENIED",
      "Chức năng này chỉ dành cho nhân viên ngân hàng (Bank Officer)."
    );
    return;
  }
  next();
}
