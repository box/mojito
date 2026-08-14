import { spawn, type ChildProcessByStdio } from "node:child_process";
import type { Readable } from "node:stream";
import type { MojitoMcpConfig } from "./config.js";
import { MojitoCliError } from "./errors.js";

export type CliRunResult = {
    exitCode: number;
    stdout: string;
    stderr: string;
};

/**
 * Runs the Mojito CLI with captured stdout/stderr and an optional timeout.
 * Implementations must never inherit the child's stdout onto the MCP stdio transport.
 */
export interface CliRunner {
    /**
     * @param argv Arguments after the program name (e.g. `["api", "/api/repositories"]` or `["--help"]`)
     */
    run(argv: string[]): Promise<CliRunResult>;
}

/**
 * Default runner: spawns {@link MojitoMcpConfig.cliBinary} with captured stdio and a hard timeout.
 */
export class DefaultCliRunner implements CliRunner {
    constructor(private readonly _config: MojitoMcpConfig) {}

    get config(): MojitoMcpConfig {
        return this._config;
    }

    async run(argv: string[]): Promise<CliRunResult> {
        const { cliBinary, timeoutMs } = this._config;

        return new Promise<CliRunResult>((resolve, reject) => {
            let stdout = "";
            let stderr = "";
            let settled = false;
            let timedOut = false;

            let child: ChildProcessByStdio<null, Readable, Readable>;
            try {
                child = spawn(cliBinary, argv, {
                    stdio: ["ignore", "pipe", "pipe"],
                    env: process.env,
                });
            } catch (cause) {
                reject(
                    new MojitoCliError(`Failed to spawn Mojito CLI '${cliBinary}'`, {
                        cause,
                    }),
                );
                return;
            }

            const timer = setTimeout(() => {
                timedOut = true;
                child.kill("SIGTERM");
                setTimeout(() => {
                    if (!settled) {
                        child.kill("SIGKILL");
                    }
                }, 2_000).unref();
            }, timeoutMs);

            child.stdout.setEncoding("utf8");
            child.stderr.setEncoding("utf8");
            child.stdout.on("data", (chunk: string) => {
                stdout += chunk;
            });
            child.stderr.on("data", (chunk: string) => {
                stderr += chunk;
            });

            child.on("error", (cause) => {
                if (settled) {
                    return;
                }
                settled = true;
                clearTimeout(timer);
                reject(
                    new MojitoCliError(`Failed to spawn Mojito CLI '${cliBinary}'`, {
                        stdout,
                        stderr,
                        cause,
                    }),
                );
            });

            child.on("close", (code) => {
                if (settled) {
                    return;
                }
                settled = true;
                clearTimeout(timer);

                if (timedOut) {
                    reject(
                        new MojitoCliError(
                            `Mojito CLI '${cliBinary}' timed out after ${timeoutMs}ms`,
                            {
                                exitCode: code,
                                stdout,
                                stderr,
                                timedOut: true,
                            },
                        ),
                    );
                    return;
                }

                resolve({
                    exitCode: code ?? 1,
                    stdout,
                    stderr,
                });
            });
        });
    }
}
