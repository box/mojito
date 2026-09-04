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

/**
 * Test scenarios: reading server configuration from the environment.
 *
 * The MCP host (for example Cursor) hands this server its configuration only through
 * environment variables, so these tests pin the behaviour for each shape of input an
 * operator can realistically produce. Mojito credentials and host are deliberately out
 * of scope: they live in the Mojito CLI's own config, never in ours. Whether the named
 * CLI actually exists on disk is also out of scope: that is a spawn failure in the
 * runner, not a config-parse failure.
 *
 * 1. Nothing is set. The server must still come up with usable defaults, namely the
 *    `mojito-prod` wrapper and a ten minute per-call timeout. This is the path most
 *    people get, so the defaults are asserted both through the exported constants and
 *    as literal values, to catch someone quietly retuning them.
 * 2. MOJITO_CLI names a wrapper. The operator's chosen script (say `mojito-dev`)
 *    replaces the default; this is how one machine runs both a prod and a dev server
 *    from the same package.
 * 3. MOJITO_CLI has surrounding whitespace. Hand-edited JSON config easily picks up a
 *    stray space, and a padded program name would fail to spawn, so it is trimmed.
 * 4. MOJITO_CLI is empty or only whitespace. A blank Cursor env value must not become
 *    an empty program name that can never spawn; it falls back to the default wrapper.
 * 5. MOJITO_CLI is a filesystem path rather than a name on PATH. Absolute and relative
 *    paths are accepted unchanged (after trim), because some operators launch a specific
 *    jar wrapper instead of a PATH script.
 * 6. MOJITO_CLI_TIMEOUT_MS is a valid positive whole number. Operators need to raise the
 *    spawn timeout for slow instances and long-running jobs, so the override is honoured.
 * 7. MOJITO_CLI_TIMEOUT_MS has surrounding whitespace. Number parsing already tolerates
 *    that, and it must still be accepted so a padded JSON value does not silently revert
 *    to the default.
 * 8. MOJITO_CLI_TIMEOUT_MS is not a number at all. A typo must not become NaN, which
 *    would make every CLI call fail instantly, so we fall back to the default.
 * 9. MOJITO_CLI_TIMEOUT_MS is an empty string. Number("") is zero; that must not kill
 *    every call, so we fall back to the default.
 * 10. MOJITO_CLI_TIMEOUT_MS is zero. A non-positive timeout would kill every call
 *     immediately, so the default wins.
 * 11. MOJITO_CLI_TIMEOUT_MS is negative. Same as zero: the default wins.
 * 12. MOJITO_CLI_TIMEOUT_MS is a fraction such as 1500.9. Timeouts are whole milliseconds
 *     only; a fractional value is rejected and the default is used.
 * 13. MOJITO_CLI_TIMEOUT_MS is non-finite, such as Infinity or an overflow like 1e1000.
 *     Those parse as Infinity, which must not disable the timer, so we fall back.
 * 14. Both variables are set together. Reading one field must not clobber or ignore the
 *     other.
 */

import { describe, expect, test } from "@jest/globals";
import { DEFAULT_CLI_BINARY, DEFAULT_TIMEOUT_MS, loadConfigFromEnv } from "../src/config.js";

describe("loadConfigFromEnv", () => {
    const originalEnv = process.env;

    function withEnv(env: NodeJS.ProcessEnv, fn: () => void): void {
        process.env = { ...env };
        try {
            fn();
        } finally {
            process.env = originalEnv;
        }
    }

    test("defaults to mojito-prod and 10 minute timeout when env is empty", () => {
        withEnv({}, () => {
            const config = loadConfigFromEnv();
            expect(config.cliBinary).toBe(DEFAULT_CLI_BINARY);
            expect(config.cliBinary).toBe("mojito-prod");
            expect(config.timeoutMs).toBe(DEFAULT_TIMEOUT_MS);
            expect(config.timeoutMs).toBe(600_000);
        });
    });

    test("reads MOJITO_CLI", () => {
        withEnv({ MOJITO_CLI: "mojito-dev" }, () => {
            expect(loadConfigFromEnv().cliBinary).toBe("mojito-dev");
        });
    });

    test("trims MOJITO_CLI", () => {
        withEnv({ MOJITO_CLI: "  mojito-dev  " }, () => {
            expect(loadConfigFromEnv().cliBinary).toBe("mojito-dev");
        });
    });

    test("falls back to default CLI when MOJITO_CLI is empty", () => {
        withEnv({ MOJITO_CLI: "" }, () => {
            expect(loadConfigFromEnv().cliBinary).toBe(DEFAULT_CLI_BINARY);
        });
    });

    test("falls back to default CLI when MOJITO_CLI is whitespace-only", () => {
        withEnv({ MOJITO_CLI: "   " }, () => {
            expect(loadConfigFromEnv().cliBinary).toBe(DEFAULT_CLI_BINARY);
        });
    });

    test("accepts an absolute path for MOJITO_CLI", () => {
        withEnv({ MOJITO_CLI: "/Users/me/bin/mojito-prod" }, () => {
            expect(loadConfigFromEnv().cliBinary).toBe("/Users/me/bin/mojito-prod");
        });
    });

    test("accepts a relative path for MOJITO_CLI", () => {
        withEnv({ MOJITO_CLI: "./scripts/mojito-dev" }, () => {
            expect(loadConfigFromEnv().cliBinary).toBe("./scripts/mojito-dev");
        });
    });

    test("reads MOJITO_CLI_TIMEOUT_MS", () => {
        withEnv({ MOJITO_CLI_TIMEOUT_MS: "120000" }, () => {
            expect(loadConfigFromEnv().timeoutMs).toBe(120_000);
        });
    });

    test("trims MOJITO_CLI_TIMEOUT_MS", () => {
        withEnv({ MOJITO_CLI_TIMEOUT_MS: "  120000  " }, () => {
            expect(loadConfigFromEnv().timeoutMs).toBe(120_000);
        });
    });

    test("falls back to default timeout when MOJITO_CLI_TIMEOUT_MS is invalid", () => {
        withEnv({ MOJITO_CLI_TIMEOUT_MS: "nope" }, () => {
            expect(loadConfigFromEnv().timeoutMs).toBe(DEFAULT_TIMEOUT_MS);
        });
    });

    test("falls back to default timeout when MOJITO_CLI_TIMEOUT_MS is empty", () => {
        withEnv({ MOJITO_CLI_TIMEOUT_MS: "" }, () => {
            expect(loadConfigFromEnv().timeoutMs).toBe(DEFAULT_TIMEOUT_MS);
        });
    });

    test("falls back to default timeout when MOJITO_CLI_TIMEOUT_MS is zero", () => {
        withEnv({ MOJITO_CLI_TIMEOUT_MS: "0" }, () => {
            expect(loadConfigFromEnv().timeoutMs).toBe(DEFAULT_TIMEOUT_MS);
        });
    });

    test("falls back to default timeout when MOJITO_CLI_TIMEOUT_MS is negative", () => {
        withEnv({ MOJITO_CLI_TIMEOUT_MS: "-1" }, () => {
            expect(loadConfigFromEnv().timeoutMs).toBe(DEFAULT_TIMEOUT_MS);
        });
    });

    test("falls back to default timeout when MOJITO_CLI_TIMEOUT_MS is fractional", () => {
        withEnv({ MOJITO_CLI_TIMEOUT_MS: "1500.9" }, () => {
            expect(loadConfigFromEnv().timeoutMs).toBe(DEFAULT_TIMEOUT_MS);
        });
    });

    test("falls back to default timeout when MOJITO_CLI_TIMEOUT_MS is Infinity", () => {
        withEnv({ MOJITO_CLI_TIMEOUT_MS: "Infinity" }, () => {
            expect(loadConfigFromEnv().timeoutMs).toBe(DEFAULT_TIMEOUT_MS);
        });
    });

    test("falls back to default timeout when MOJITO_CLI_TIMEOUT_MS overflows to Infinity", () => {
        withEnv({ MOJITO_CLI_TIMEOUT_MS: "1e1000" }, () => {
            expect(loadConfigFromEnv().timeoutMs).toBe(DEFAULT_TIMEOUT_MS);
        });
    });

    test("reads both MOJITO_CLI and MOJITO_CLI_TIMEOUT_MS together", () => {
        withEnv({ MOJITO_CLI: "mojito-dev", MOJITO_CLI_TIMEOUT_MS: "120000" }, () => {
            const config = loadConfigFromEnv();
            expect(config.cliBinary).toBe("mojito-dev");
            expect(config.timeoutMs).toBe(120_000);
        });
    });
});
