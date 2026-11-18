import assert from "node:assert/strict";
import { AddressInfo } from "node:net";
import { after, before, describe, it } from "node:test";
import { app } from "../src/index";

describe("App error handling", () => {
  let baseUrl = "";
  let server: ReturnType<typeof app.listen>;

  before(async () => {
    server = app.listen(0);
    await new Promise<void>((resolve) => server.once("listening", () => resolve()));
    baseUrl = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
  });

  after(async () => {
    await new Promise<void>((resolve) => server.close(() => resolve()));
  });

  it("returns 400 INVALID_ARGUMENT for malformed JSON bodies", async () => {
    const res = await fetch(`${baseUrl}/transactions/initiate`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: "{not-json",
    });
    assert.equal(res.status, 400);
    assert.equal(((await res.json()) as { error: string }).error, "INVALID_ARGUMENT");
  });

  it("returns 413 for oversized bodies", async () => {
    const res = await fetch(`${baseUrl}/ekyc/submit`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ imageBase64: "A".repeat(1_200_000) }),
    });
    assert.equal(res.status, 413);
  });

  it("returns 404 NOT_FOUND for unknown routes and 200 for /health", async () => {
    const notFound = await fetch(`${baseUrl}/unknown`);
    assert.equal(notFound.status, 404);
    const health = await fetch(`${baseUrl}/health`);
    assert.equal(health.status, 200);
    assert.deepEqual(await health.json(), { ok: true });
  });
});
