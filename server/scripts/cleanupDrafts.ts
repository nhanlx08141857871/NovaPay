import admin from "firebase-admin";
import { initFirebaseAdmin, isFirebaseReady } from "../src/config";
import { DRAFT_RETENTION_MS } from "../src/services/otp";

const BATCH_SIZE = 400;

async function cleanupExpiredDrafts(): Promise<number> {
  initFirebaseAdmin();
  if (!isFirebaseReady()) {
    throw new Error("Firebase Admin chưa được cấu hình hợp lệ trong server/.env.");
  }

  const db = admin.firestore();
  const cutoff = admin.firestore.Timestamp.fromMillis(Date.now() - DRAFT_RETENTION_MS);
  let deleted = 0;

  for (;;) {
    const snapshot = await db
      .collection("transaction_drafts")
      .where("createdAt", "<", cutoff)
      .limit(BATCH_SIZE)
      .get();
    if (snapshot.empty) {
      break;
    }
    const batch = db.batch();
    snapshot.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
    deleted += snapshot.size;
  }

  return deleted;
}

cleanupExpiredDrafts()
  .then((deleted) => {
    console.log(`[NovaPay Cleanup] Đã xóa ${deleted} transaction_drafts quá hạn.`);
    process.exit(0);
  })
  .catch((err) => {
    const message = err instanceof Error ? err.message : String(err);
    console.error("[NovaPay Cleanup] Lỗi dọn dẹp transaction_drafts:", message);
    process.exit(1);
  });
