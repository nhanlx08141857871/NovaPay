import fs from "fs";
import path from "path";
import { getSecurityRules } from "firebase-admin/security-rules";
import { initFirebaseAdmin, isFirebaseReady } from "../src/config";

async function deployFirestoreRules(): Promise<void> {
  initFirebaseAdmin();
  if (!isFirebaseReady()) {
    throw new Error("Firebase Admin chưa được cấu hình hợp lệ trong server/.env.");
  }

  const rulesPath = path.resolve(__dirname, "../../firestore.rules");
  const source = fs.readFileSync(rulesPath, "utf8");

  const securityRules = getSecurityRules();
  const ruleset = await securityRules.releaseFirestoreRulesetFromSource(source);
  console.log(`[NovaPay Rules] Đã deploy firestore.rules lên Cloud Firestore thành công -> Ruleset: ${ruleset.name}`);
}

deployFirestoreRules()
  .then(() => process.exit(0))
  .catch((err) => {
    const message = err instanceof Error ? err.message : String(err);
    console.error("[NovaPay Rules] Lỗi deploy firestore.rules:", message);
    process.exit(1);
  });
