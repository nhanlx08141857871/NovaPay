import cors from "cors";
import express, { NextFunction, Request, Response } from "express";
import helmet from "helmet";
import { config, initFirebaseAdmin } from "./config";
import { sendApiError } from "./middleware/auth";
import { createRateLimiter } from "./middleware/rateLimit";
import { accountsRouter } from "./routes/accounts";
import { aiRouter } from "./routes/ai";
import { ekycRouter } from "./routes/ekyc";
import { officerRouter } from "./routes/officer";
import { paymentsRouter } from "./routes/payments";
import { savingsRouter } from "./routes/savings";
import { securityRouter } from "./routes/security";
import { transactionsRouter } from "./routes/transactions";

initFirebaseAdmin();

export const app = express();

app.set("trust proxy", 1);
app.use(helmet());
app.use(
  cors({
    origin: "*",
    methods: ["GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"],
    allowedHeaders: ["Content-Type", "Authorization"],
  })
);
// Giới hạn payload 1MB đủ cho ảnh Base64 eKYC / FaceCheck (< 200KB JPEG)
app.use(express.json({ limit: "1mb" }));

// Endpoint kiểm tra trạng thái sống (dùng cho UptimeRobot chống ngủ đông trên Render)
app.get("/health", (_req: Request, res: Response) => {
  res.status(200).json({ ok: true });
});

const MINUTE_MS = 60_000;
app.use(createRateLimiter({ windowMs: MINUTE_MS, max: 300 }));
app.use("/accounts/lookup", createRateLimiter({ windowMs: MINUTE_MS, max: 30 }));
app.use("/ai/chat", createRateLimiter({ windowMs: MINUTE_MS, max: 20 }));
app.use("/ekyc/submit", createRateLimiter({ windowMs: MINUTE_MS, max: 10 }));
app.use("/ekyc/review", createRateLimiter({ windowMs: MINUTE_MS, max: 30 }));
app.use("/transactions/initiate", createRateLimiter({ windowMs: MINUTE_MS, max: 30 }));
app.use("/transactions/confirm", createRateLimiter({ windowMs: MINUTE_MS, max: 20 }));
app.use("/transactions/face-check", createRateLimiter({ windowMs: MINUTE_MS, max: 10 }));
app.use("/transactions/resend-otp", createRateLimiter({ windowMs: MINUTE_MS, max: 10 }));
app.use("/payments/finalize", createRateLimiter({ windowMs: MINUTE_MS, max: 20 }));
app.use("/savings", createRateLimiter({ windowMs: MINUTE_MS, max: 20 }));
app.use("/security", createRateLimiter({ windowMs: MINUTE_MS, max: 30 }));
app.use("/officer", createRateLimiter({ windowMs: MINUTE_MS, max: 30 }));

// Định tuyến phân tách theo đúng phạm vi sở hữu của 3 thành viên (A, B, C)
app.use("/ekyc", ekycRouter);
app.use("/accounts", accountsRouter);
app.use("/transactions", transactionsRouter);
app.use("/payments", paymentsRouter);
app.use("/savings", savingsRouter);
app.use("/security", securityRouter);
app.use("/officer", officerRouter);
app.use("/ai", aiRouter);

// Bắt lỗi route không tồn tại
app.use((_req: Request, res: Response) => {
  sendApiError(res, 404, "NOT_FOUND", "Đường dẫn API không tồn tại.");
});

// Bộ xử lý lỗi tập trung
app.use((err: Error & { type?: string }, _req: Request, res: Response, _next: NextFunction) => {
  if (err.type === "entity.parse.failed") {
    sendApiError(res, 400, "INVALID_ARGUMENT", "Dữ liệu JSON gửi lên không hợp lệ.");
    return;
  }
  if (err.type === "entity.too.large") {
    sendApiError(res, 413, "INVALID_ARGUMENT", "Dữ liệu gửi lên vượt quá dung lượng cho phép.");
    return;
  }
  console.error("[NovaPay Server Error]:", err.message);
  sendApiError(res, 500, "INTERNAL_ERROR", "Lỗi xử lý nội bộ trên máy chủ.");
});

if (require.main === module) {
  const port = Number(process.env.PORT) || config.port || 3000;
  const host = "0.0.0.0";
  app.listen(port, host, () => {
    console.log(`[NovaPay Server] Listening on http://${host}:${port}`);
  });
}
