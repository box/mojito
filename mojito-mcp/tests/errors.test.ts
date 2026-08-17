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
import { MojitoCliError } from "../src/errors.js";

describe("MojitoCliError", () => {
    test("stores exit metadata for MCP error surfacing", () => {
        const err = new MojitoCliError("failed", {
            exitCode: 1,
            stdout: '{"message":"x"}',
            stderr: "mojito: x (HTTP 400)",
            timedOut: false,
        });

        expect(err.name).toBe("MojitoCliError");
        expect(err.message).toBe("failed");
        expect(err.exitCode).toBe(1);
        expect(err.stdout).toContain("message");
        expect(err.stderr).toContain("mojito:");
        expect(err.timedOut).toBe(false);
    });

    test("defaults optional fields", () => {
        const err = new MojitoCliError("timeout", { timedOut: true });
        expect(err.exitCode).toBeNull();
        expect(err.stdout).toBe("");
        expect(err.stderr).toBe("");
        expect(err.timedOut).toBe(true);
    });
});
