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
