import dotenv from "dotenv";
import admin from "firebase-admin";

dotenv.config();

export interface AppConfig {
  port: number;
  brevoApiKey: string;
  brevoSenderEmail: string;
  brevoSenderName: string;
  stripeSecretKey: string;
  geminiApiKey: string;
  geminiModel: string;
}

export const config: AppConfig = {
  port: Number(process.env.PORT || 3000),
  brevoApiKey: process.env.BREVO_API_KEY || "",
  brevoSenderEmail: process.env.BREVO_SENDER_EMAIL || "no-reply@novapay.vn",
  brevoSenderName: process.env.BREVO_SENDER_NAME || "NovaPay Banking",
  stripeSecretKey: process.env.STRIPE_SECRET_KEY || "",
  geminiApiKey: process.env.GEMINI_API_KEY || "",
  geminiModel: process.env.GEMINI_MODEL || "gemini-2.5-flash",
};

let firebaseReady = false;

export function initFirebaseAdmin(): void {
  if (admin.apps.length > 0) {
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
    "[NovaPay Server] FIREBASE_SERVICE_ACCOUNT_JSON chưa được cấu hình — chạy ở chế độ khung Day-0 (/health sẵn sàng)."
  );
}

export function isFirebaseReady(): boolean {
  return firebaseReady && admin.apps.length > 0;
}
