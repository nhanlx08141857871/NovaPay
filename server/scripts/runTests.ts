import { spawnSync } from "node:child_process";
import { existsSync, readdirSync } from "node:fs";
import { join } from "node:path";

const testDirs = ["test", "tests"];
const testFiles = testDirs
  .filter((dir) => existsSync(dir))
  .flatMap((dir) =>
    readdirSync(dir)
      .filter((name) => name.endsWith(".test.ts"))
      .map((name) => join(dir, name))
  );

const result = spawnSync(process.execPath, ["--import", "tsx", "--test", ...testFiles], {
  stdio: "inherit",
});
process.exit(result.status ?? 1);
