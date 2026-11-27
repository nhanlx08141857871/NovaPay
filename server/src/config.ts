import dotenv from "dotenv";
import admin from "firebase-admin";

dotenv.config();

export interface AppConfig {
  port: number;
  firebaseProjectId: string;
  firebaseClientEmail: string;
  firebasePrivateKey: string;
  brevoApiKey: string;
  brevoSenderEmail: string;
  brevoSenderName: string;
  stripeSecretKey: string;
  stripePublishableKey: string;
  geminiApiKey: string;
  geminiModel: string;
}

// Chuẩn hóa chuỗi PEM từ biến môi trường (bỏ ngoặc kép & chuyển "\\n" thành "\n").
export function normalizePrivateKey(rawKey?: string): string {
  if (!rawKey) {
    return "";
  }
  let cleaned = rawKey.trim();
  if (
    (cleaned.startsWith('"') && cleaned.endsWith('"')) ||
    (cleaned.startsWith("'") && cleaned.endsWith("'"))
  ) {
    cleaned = cleaned.slice(1, -1);
  }
  return cleaned.replace(/\\n/g, "\n").trim();
}

export const config: AppConfig = {
  port: Number(process.env.PORT) || 3000,
  firebaseProjectId: (process.env.FIREBASE_PROJECT_ID || "").trim(),
  firebaseClientEmail: (process.env.FIREBASE_CLIENT_EMAIL || "").trim(),
  firebasePrivateKey: normalizePrivateKey(process.env.FIREBASE_PRIVATE_KEY),
  brevoApiKey: process.env.BREVO_API_KEY || "",
  brevoSenderEmail: process.env.BREVO_SENDER_EMAIL || "no-reply@novapay.vn",
  brevoSenderName: process.env.BREVO_SENDER_NAME || "NovaPay Banking",
  stripeSecretKey: process.env.STRIPE_SECRET_KEY || "",
  stripePublishableKey: (process.env.STRIPE_PUBLISHABLE_KEY || "").trim(),
  geminiApiKey: process.env.GEMINI_API_KEY || "",
  geminiModel: process.env.GEMINI_MODEL || "gemini-2.5-flash",
};

let firebaseReady = false;

export function initFirebaseAdmin(): void {
  if (admin.apps.length > 0) {
    firebaseReady = true;
    return;
  }

  const projectId = (process.env.FIREBASE_PROJECT_ID || config.firebaseProjectId).trim();
  const clientEmail = (process.env.FIREBASE_CLIENT_EMAIL || config.firebaseClientEmail).trim();
  const privateKey = normalizePrivateKey(process.env.FIREBASE_PRIVATE_KEY);

  if (projectId && clientEmail && privateKey) {
    admin.initializeApp({
      credential: admin.credential.cert({
        projectId,
        clientEmail,
        privateKey,
      }),
    });
    firebaseReady = true;
    return;
  }

  const rawServiceAccount = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (rawServiceAccount && rawServiceAccount.trim().length > 0) {
    const parsedCredential = JSON.parse(rawServiceAccount) as admin.ServiceAccount;
    admin.initializeApp({
      credential: admin.credential.cert(parsedCredential),
    });
    firebaseReady = true;
    return;
  }

  if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    admin.initializeApp({
      credential: admin.credential.applicationDefault(),
    });
    firebaseReady = true;
    return;
  }

  console.warn(
    "[NovaPay Server] Chưa cấu hình thông tin Firebase Admin (FIREBASE_PROJECT_ID / FIREBASE_CLIENT_EMAIL / FIREBASE_PRIVATE_KEY) — chạy ở chế độ khung (/health sẵn sàng)."
  );
}

export function isFirebaseReady(): boolean {
  return firebaseReady && admin.apps.length > 0;
}
