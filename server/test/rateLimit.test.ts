import assert from "node:assert/strict";
import express from "express";
import { AddressInfo } from "node:net";
import { describe, it } from "node:test";
import { createRateLimiter } from "../src/middleware/rateLimit";

describe("createRateLimiter", () => {
  async function start(max: number, nowRef: { value: number }) {
    const app = express();
    app.use(createRateLimiter({ windowMs: 60_000, max, now: () => nowRef.value }));
    app.get("/ping", (_req, res) => {
      res.json({ ok: true });
    });
    const server = await new Promise<ReturnType<typeof app.listen>>((resolve) => {
      const s = app.listen(0, "127.0.0.1", () => resolve(s));
    });
    const baseUrl = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
    const close = () => new Promise<void>((resolve) => server.close(() => resolve()));
    return { baseUrl, close };
  }

  it("blocks requests above the limit with 429 RATE_LIMITED and recovers after the window", async () => {
    const nowRef = { value: 1_000_000 };
    const { baseUrl, close } = await start(3, nowRef);
    try {
      for (let i = 0; i < 3; i++) {
        const res = await fetch(`${baseUrl}/ping`);
        assert.equal(res.status, 200);
      }
      const blocked = await fetch(`${baseUrl}/ping`);
      assert.equal(blocked.status, 429);
      assert.equal(((await blocked.json()) as { error: string }).error, "RATE_LIMITED");
      assert.ok(Number(blocked.headers.get("retry-after")) >= 1);

      nowRef.value += 61_000;
      const recovered = await fetch(`${baseUrl}/ping`);
      assert.equal(recovered.status, 200);
    } finally {
      await close();
    }
  });
});
