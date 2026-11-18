import cors from "cors";
import express, { NextFunction, Request, Response } from "express";
import helmet from "helmet";
import { config, initFirebaseAdmin } from "./config";
import { sendApiError } from "./middleware/auth";
import { accountsRouter } from "./routes/accounts";
import { aiRouter } from "./routes/ai";
import { ekycRouter } from "./routes/ekyc";
import { officerRouter } from "./routes/officer";
import { paymentsRouter } from "./routes/payments";
import { transactionsRouter } from "./routes/transactions";

initFirebaseAdmin();

export const app = express();

app.use(helmet());
app.use(cors());
// Giới hạn payload 1MB đủ cho ảnh Base64 eKYC / FaceCheck (< 200KB JPEG)
app.use(express.json({ limit: "1mb" }));

// Endpoint kiểm tra trạng thái sống (dùng cho UptimeRobot chống ngủ đông trên Render)
app.get("/health", (_req: Request, res: Response) => {
  res.status(200).json({ ok: true });
});

// Định tuyến phân tách theo đúng phạm vi sở hữu của 3 thành viên (A, B, C)
app.use("/ekyc", ekycRouter);
app.use("/accounts", accountsRouter);
app.use("/transactions", transactionsRouter);
app.use("/payments", paymentsRouter);
app.use("/officer", officerRouter);
app.use("/ai", aiRouter);

// Bắt lỗi route không tồn tại
app.use((_req: Request, res: Response) => {
  sendApiError(res, 404, "NOT_FOUND", "Đường dẫn API không tồn tại.");
});

// Bộ xử lý lỗi tập trung
app.use((err: Error, _req: Request, res: Response, _next: NextFunction) => {
  console.error("[NovaPay Server Error]:", err.message);
  sendApiError(res, 500, "INTERNAL_ERROR", "Lỗi xử lý nội bộ trên máy chủ.");
});

if (require.main === module) {
  app.listen(config.port, () => {
    console.log(`[NovaPay Server] Listening on port ${config.port}`);
  });
}
