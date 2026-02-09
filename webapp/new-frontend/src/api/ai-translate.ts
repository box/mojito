import { isTransientHttpError, poll } from '../utils/poller';

export type AiTranslateRequest = {
  repositoryName: string;
  targetBcp47tags: string[] | null;
  sourceTextMaxCountPerLocale: number;
  tmTextUnitIds: number[] | null;
  useBatch: boolean;
  useModel: string | null;
  promptSuffix: string | null;
  relatedStringsType: string;
  translateType: string;
  statusFilter: string;
  importStatus: string;
  glossaryName: string | null;
  glossaryTermSource: string | null;
  glossaryTermSourceDescription: string | null;
  glossaryTermTarget: string | null;
  glossaryTermTargetDescription: string | null;
  glossaryTermDoNotTranslate: boolean;
  glossaryTermCaseSensitive: boolean;
  glossaryOnlyMatchedTextUnits: boolean;
  dryRun: boolean;
  timeoutSeconds: number | null;
};

type PollableTaskResponse = {
  id: number;
  isAllFinished?: boolean;
  allFinished?: boolean;
  errorMessage?: string | null;
};

export type PollableTask = {
  id: number;
  isAllFinished: boolean;
  errorMessage?: string | null;
};

type AiTranslateResponse = {
  pollableTask: PollableTaskResponse;
};

export type AiTranslateReportResponse = {
  reportLocaleUrls?: string[] | null;
};

export type AiTranslateReportLocaleResponse = {
  content?: string | null;
};

const DEFAULT_POLL_INTERVAL_MS = 500;
const MAX_POLL_INTERVAL_MS = 8000;

export async function translateRepository(request: AiTranslateRequest): Promise<PollableTask> {
  const response = await postJson<AiTranslateResponse>('/api/proto-ai-translate', request);
  return normalizePollableTask(response.pollableTask);
}

export async function fetchPollableTask(pollableTaskId: number): Promise<PollableTask> {
  const response = await getJson<PollableTaskResponse>(`/api/pollableTasks/${pollableTaskId}`);
  return normalizePollableTask(response);
}

export async function waitForPollableTaskToFinish(
  pollableTaskId: number,
  timeoutMs?: number,
): Promise<PollableTask> {
  return poll(() => fetchPollableTask(pollableTaskId), {
    intervalMs: DEFAULT_POLL_INTERVAL_MS,
    maxIntervalMs: MAX_POLL_INTERVAL_MS,
    timeoutMs,
    timeoutMessage: 'Timed out while waiting for AI translate to finish',
    isTransientError: isTransientHttpError,
    shouldStop: (task) => task.isAllFinished,
  });
}

export async function fetchAiTranslateReport(
  pollableTaskId: number,
): Promise<AiTranslateReportResponse> {
  return getJson<AiTranslateReportResponse>(`/api/proto-ai-translate/report/${pollableTaskId}`);
}

export async function fetchAiTranslateReportLocale(
  pollableTaskId: number,
  localeTag: string,
): Promise<AiTranslateReportLocaleResponse> {
  return getJson<AiTranslateReportLocaleResponse>(
    `/api/proto-ai-translate/report/${pollableTaskId}/locale/${localeTag}`,
  );
}

export async function fetchAiTranslateReportPath(
  path: string,
): Promise<AiTranslateReportLocaleResponse> {
  const trimmed = path.replace(/^\/+/, '');
  if (trimmed.startsWith('api/')) {
    return getJson<AiTranslateReportLocaleResponse>(`/${trimmed}`);
  }
  if (trimmed.startsWith('proto-ai-translate/')) {
    return getJson<AiTranslateReportLocaleResponse>(`/api/${trimmed}`);
  }
  return getJson<AiTranslateReportLocaleResponse>(`/api/proto-ai-translate/${trimmed}`);
}

function normalizePollableTask(task: PollableTaskResponse): PollableTask {
  const rawMessage = task.errorMessage;
  const normalizedMessage =
    typeof rawMessage === 'string' ? rawMessage : rawMessage ? JSON.stringify(rawMessage) : null;

  return {
    id: task.id,
    isAllFinished: task.isAllFinished ?? task.allFinished ?? false,
    errorMessage: normalizedMessage,
  };
}

async function postJson<TResponse>(url: string, body: unknown): Promise<TResponse> {
  const response = await fetch(url, {
    method: 'POST',
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    body: JSON.stringify(body),
  });

  const text = await response.text();
  if (!response.ok) {
    throw buildHttpError(text, response.status);
  }
  return parseJsonResponse(text, response.status);
}

async function getJson<TResponse>(url: string): Promise<TResponse> {
  const response = await fetch(url, {
    method: 'GET',
    credentials: 'same-origin',
    headers: {
      Accept: 'application/json',
    },
  });

  const text = await response.text();
  if (!response.ok) {
    throw buildHttpError(text, response.status);
  }
  return parseJsonResponse(text, response.status);
}

const parseJsonResponse = <TResponse>(text: string, status: number): TResponse => {
  if (!text) {
    return undefined as TResponse;
  }
  try {
    return JSON.parse(text) as TResponse;
  } catch {
    throw buildUnexpectedResponseError(text, status);
  }
};

const buildHttpError = (text: string, status: number) => {
  const message = buildErrorMessage(text, status);
  const error: Error & { status?: number } = new Error(message);
  error.status = status;
  return error;
};

const buildUnexpectedResponseError = (text: string, status: number) => {
  const message = buildUnexpectedResponseMessage(text, status);
  const error: Error & { status?: number } = new Error(message);
  error.status = status;
  return error;
};

const buildErrorMessage = (text: string, status: number) => {
  const trimmed = text.trim();
  if (looksLikeHtml(trimmed)) {
    return isServiceUnavailableStatus(status)
      ? buildServiceUnavailableMessage(status)
      : `AI translate returned an unexpected response (HTTP ${status}).`;
  }
  if (isServiceUnavailableStatus(status)) {
    return buildServiceUnavailableMessage(status);
  }
  return trimmed || `Request failed with status ${status}`;
};

const buildUnexpectedResponseMessage = (text: string, status: number) => {
  const trimmed = text.trim();
  if (looksLikeHtml(trimmed)) {
    return `AI translate returned an unexpected HTML response${status ? ` (HTTP ${status})` : ''}.`;
  }
  return trimmed
    ? `AI translate returned an unexpected response: ${trimmed}`
    : `AI translate returned an unexpected response${status ? ` (HTTP ${status})` : ''}.`;
};

const buildServiceUnavailableMessage = (status?: number) =>
  `AI translate is temporarily unavailable${status ? ` (HTTP ${status})` : ''}. Please try again.`;

const looksLikeHtml = (text: string) => {
  if (!text) {
    return false;
  }
  const lowered = text.toLowerCase();
  return (
    lowered.startsWith('<!doctype') ||
    lowered.startsWith('<html') ||
    lowered.includes('<html') ||
    lowered.includes('cloudflare')
  );
};

const isServiceUnavailableStatus = (status: number) =>
  status === 503 || status === 502 || status === 504 || (status >= 520 && status <= 529);
