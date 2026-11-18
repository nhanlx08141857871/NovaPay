import { NextFunction, Request, RequestHandler, Response } from "express";
import { sendApiError } from "./auth";

export interface RateLimitOptions {
  windowMs: number;
  max: number;
  keyFn?: (req: Request) => string;
  now?: () => number;
}

interface HitEntry {
  count: number;
  resetAt: number;
}

export function createRateLimiter(options: RateLimitOptions): RequestHandler {
  const hits = new Map<string, HitEntry>();
  const now = options.now ?? (() => Date.now());
  const keyFn = options.keyFn ?? ((req: Request) => req.ip ?? "unknown");
  let lastSweepAt = now();

  return (req: Request, res: Response, next: NextFunction): void => {
    const current = now();

    if (current - lastSweepAt >= options.windowMs) {
      for (const [key, entry] of hits) {
        if (entry.resetAt <= current) {
          hits.delete(key);
        }
      }
      lastSweepAt = current;
    }

    const key = keyFn(req);
    const entry = hits.get(key);
    if (!entry || entry.resetAt <= current) {
      hits.set(key, { count: 1, resetAt: current + options.windowMs });
      next();
      return;
    }

    entry.count += 1;
    if (entry.count > options.max) {
      const retryAfterSeconds = Math.max(1, Math.ceil((entry.resetAt - current) / 1000));
      res.setHeader("Retry-After", String(retryAfterSeconds));
      sendApiError(
        res,
        429,
        "RATE_LIMITED",
        "Bạn thao tác quá nhanh. Vui lòng thử lại sau ít phút."
      );
      return;
    }
    next();
  };
}
