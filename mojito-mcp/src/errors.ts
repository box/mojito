/**
 * Error raised when a Mojito CLI invocation fails (non-zero exit, timeout, or spawn failure).
 * Intended to be surfaced to the AI as a normal MCP tool error.
 */
export class MojitoCliError extends Error {
    readonly exitCode: number | null;
    readonly stdout: string;
    readonly stderr: string;
    readonly timedOut: boolean;

    constructor(
        message: string,
        options: {
            exitCode?: number | null;
            stdout?: string;
            stderr?: string;
            timedOut?: boolean;
            cause?: unknown;
        } = {},
    ) {
        super(message, options.cause !== undefined ? { cause: options.cause } : undefined);
        this.name = "MojitoCliError";
        this.exitCode = options.exitCode ?? null;
        this.stdout = options.stdout ?? "";
        this.stderr = options.stderr ?? "";
        this.timedOut = options.timedOut ?? false;
    }
}

export function notImplemented(what: string): never {
    throw new Error(`${what}: not implemented`);
}
