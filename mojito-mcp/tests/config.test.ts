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

    test("reads MOJITO_CLI_TIMEOUT_MS", () => {
        withEnv({ MOJITO_CLI_TIMEOUT_MS: "120000" }, () => {
            expect(loadConfigFromEnv().timeoutMs).toBe(120_000);
        });
    });

    test("falls back to default timeout when MOJITO_CLI_TIMEOUT_MS is invalid", () => {
        withEnv({ MOJITO_CLI_TIMEOUT_MS: "nope" }, () => {
            expect(loadConfigFromEnv().timeoutMs).toBe(DEFAULT_TIMEOUT_MS);
        });
    });

    test("falls back to default timeout when MOJITO_CLI_TIMEOUT_MS is non-positive", () => {
        withEnv({ MOJITO_CLI_TIMEOUT_MS: "0" }, () => {
            expect(loadConfigFromEnv().timeoutMs).toBe(DEFAULT_TIMEOUT_MS);
        });
    });
});
