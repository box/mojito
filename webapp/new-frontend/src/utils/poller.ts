type PollOptions<T> = {
  intervalMs: number;
  timeoutMs?: number;
  timeoutMessage?: string;
  maxIntervalMs?: number;
  isTransientError?: (error: unknown) => boolean;
  shouldStop: (result: T) => boolean;
};

const DEFAULT_MAX_POLL_INTERVAL_MS = 8000;
const TRANSIENT_HTTP_STATUSES = new Set([
  502, 503, 504, 520, 521, 522, 523, 524, 525, 526, 527, 528, 529,
]);

export async function poll<T>(task: () => Promise<T>, options: PollOptions<T>): Promise<T> {
  const {
    intervalMs,
    timeoutMs,
    timeoutMessage,
    maxIntervalMs = DEFAULT_MAX_POLL_INTERVAL_MS,
    isTransientError,
    shouldStop,
  } = options;
  const startedAt = Date.now();
  let transientFailures = 0;

  for (;;) {
    if (typeof timeoutMs === 'number' && timeoutMs > 0 && Date.now() - startedAt > timeoutMs) {
      throw new Error(timeoutMessage ?? 'Timed out while waiting for response');
    }

    try {
      const result = await task();
      if (shouldStop(result)) {
        return result;
      }
      transientFailures = 0;
    } catch (error) {
      if (isTransientError?.(error)) {
        transientFailures += 1;
        await delay(getBackoffDelayMs(transientFailures, intervalMs, maxIntervalMs));
        continue;
      }
      throw error;
    }

    await delay(intervalMs);
  }
}

export const isTransientHttpError = (error: unknown) => {
  const transientOverride = (error as { isTransient?: boolean })?.isTransient;
  if (typeof transientOverride === 'boolean') {
    return transientOverride;
  }
  const status = (error as { status?: number })?.status;
  if (typeof status === 'number') {
    return TRANSIENT_HTTP_STATUSES.has(status);
  }
  return isNetworkError(error);
};

const isNetworkError = (error: unknown) => {
  if (error instanceof TypeError) {
    return true;
  }
  const message = (error as Error | undefined)?.message?.toLowerCase() ?? '';
  return message.includes('failed to fetch') || message.includes('network');
};

const getBackoffDelayMs = (failureCount: number, baseIntervalMs: number, maxIntervalMs: number) => {
  const safeFailureCount = Math.max(1, failureCount);
  const delayMs = baseIntervalMs * Math.pow(2, safeFailureCount - 1);
  return Math.min(delayMs, maxIntervalMs);
};

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));
