/**
 * Copyright 2026 Box, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { describe, expect, test } from "@jest/globals";
import { DefaultCliRunner } from "../src/cli-runner.js";
import { MojitoCliError } from "../src/errors.js";

/**
 * Contract tests for DefaultCliRunner.
 * These will fail until the runner is implemented (TDD red phase).
 */
describe("DefaultCliRunner", () => {
    test("run executes the configured binary with argv and returns exitCode/stdout/stderr", async () => {
        // Use a portable command: node -e prints and exits 0
        const runner = new DefaultCliRunner({
            cliBinary: process.execPath,
            timeoutMs: 10_000,
        });

        const result = await runner.run([
            "-e",
            "process.stdout.write('out'); process.stderr.write('err');",
        ]);

        expect(result.exitCode).toBe(0);
        expect(result.stdout).toBe("out");
        expect(result.stderr).toBe("err");
    });

    test("run returns non-zero exitCode without throwing when the child exits failed", async () => {
        const runner = new DefaultCliRunner({
            cliBinary: process.execPath,
            timeoutMs: 10_000,
        });

        const result = await runner.run(["-e", "process.exit(7)"]);

        expect(result.exitCode).toBe(7);
    });

    test("run times out and throws MojitoCliError with timedOut=true", async () => {
        const runner = new DefaultCliRunner({
            cliBinary: process.execPath,
            timeoutMs: 200,
        });

        await expect(runner.run(["-e", "setTimeout(() => {}, 10_000)"])).rejects.toMatchObject({
            name: "MojitoCliError",
            timedOut: true,
        } satisfies Partial<MojitoCliError>);
    });

    test("run throws MojitoCliError when the binary cannot be spawned", async () => {
        const runner = new DefaultCliRunner({
            cliBinary: "/nonexistent/mojito-binary-for-tests",
            timeoutMs: 5_000,
        });

        await expect(runner.run(["--help"])).rejects.toBeInstanceOf(MojitoCliError);
    });
});
