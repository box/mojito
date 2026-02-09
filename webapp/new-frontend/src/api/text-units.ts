import { isTransientHttpError, poll } from '../utils/poller';

export type SearchAttribute =
  | 'stringId'
  | 'source'
  | 'target'
  | 'asset'
  | 'location'
  | 'pluralFormOther'
  | 'tmTextUnitIds';

export type SearchType = 'exact' | 'contains' | 'ilike' | 'regex';

// Backend-defined status values for a translation variant.
// Note: untranslated rows can still appear in results, but will have `target: null` (and `status` may be null/undefined).
export type ApiTextUnitStatus = 'APPROVED' | 'REVIEW_NEEDED' | 'TRANSLATION_NEEDED';

export type ApiTextUnit = {
  tmTextUnitId: number;
  tmTextUnitVariantId?: number | null;
  tmTextUnitCurrentVariantId?: number | null;
  localeId?: number | null;
  createdDate?: string | null;
  tmTextUnitCreatedDate?: string | null;
  name: string;
  source?: string | null;
  comment?: string | null;
  target?: string | null;
  targetLocale: string;
  targetComment?: string | null;
  pluralForm?: string | null;
  pluralFormOther?: string | null;
  doNotTranslate?: boolean;
  assetId?: number | null;
  lastSuccessfulAssetExtractionId?: number | null;
  assetExtractionId?: number | null;
  assetTextUnitId?: number | null;
  branchId?: number | null;
  assetPath?: string | null;
  assetTextUnitUsages?: string | null;
  used: boolean;
  repositoryName?: string | null;
  status?: ApiTextUnitStatus | null;
  includedInLocalizedFile?: boolean;
};

// Backend default (TextUnitSearchBody.limit) is 10; keep the client aligned if a caller omits limit.
const DEFAULT_SEARCH_LIMIT = 10;
const ASYNC_POLL_TIMEOUT_MS = 60_000;
const DEFAULT_POLL_INTERVAL_MS = 1000;
const MAX_POLL_INTERVAL_MS = 8000;

export type TextUnitSearchRequest = {
  repositoryIds: number[];
  localeTags: string[];
  searchAttribute: SearchAttribute;
  searchType: SearchType;
  searchText: string;
  offset?: number;
  limit?: number;
  statusFilter?: string;
  usedFilter?: 'USED' | 'UNUSED';
  doNotTranslateFilter?: boolean;
  tmTextUnitCreatedBefore?: string;
  tmTextUnitCreatedAfter?: string;
  tmTextUnitVariantCreatedBefore?: string;
  tmTextUnitVariantCreatedAfter?: string;
};

export type SaveTextUnitRequest = {
  tmTextUnitId: number;
  localeId: number;
  target: string;
  targetComment?: string | null;
  status: ApiTextUnitStatus;
  includedInLocalizedFile: boolean;
};

export type TextUnitIntegrityCheckRequest = {
  tmTextUnitId: number;
  content: string;
};

export type TextUnitIntegrityCheckResult = {
  checkResult?: boolean | null;
  failureDetail?: string | null;
};

export type ApiTextUnitHistoryComment = {
  id?: number | null;
  createdDate?: string | null;
  content?: string | null;
  severity?: string | null;
  type?: string | null;
};

export type ApiTextUnitHistoryItem = {
  id: number;
  createdDate?: string | null;
  content?: string | null;
  comment?: string | null;
  status?: ApiTextUnitStatus | null;
  includedInLocalizedFile?: boolean;
  createdByUser?: {
    id?: number | null;
    username?: string | null;
  } | null;
  tmTextUnitVariantComments?: ApiTextUnitHistoryComment[] | null;
};

export type ApiGitBlame = {
  authorName?: string | null;
  authorEmail?: string | null;
  commitName?: string | null;
  commitTime?: string | null;
};

export type ApiGitBlameWithUsage = {
  tmTextUnitId?: number | null;
  assetId?: number | null;
  assetTextUnitId?: number | null;
  thirdPartyTextUnitId?: string | null;
  branch?: {
    id?: number | null;
    name?: string | null;
  } | null;
  isVirtual?: boolean;
  usages?: string[] | null;
  gitBlame?: ApiGitBlame | null;
  screenshots?: Array<{
    id?: number | null;
    name?: string | null;
    src?: string | null;
  }> | null;
};

type SearchTextUnitsHybridResponse = {
  results?: ApiTextUnit[] | null;
  pollingToken?: {
    requestId: string;
    recommendedPollingDurationMillis?: number;
  } | null;
  error?: {
    message?: string;
  } | null;
};

type TextUnitSearchBody = {
  repositoryIds: number[];
  localeTags: string[];
  tmTextUnitIds?: number[];
  name?: string;
  source?: string;
  target?: string;
  assetPath?: string;
  assetTextUnitUsages?: string;
  pluralFormOther?: string;
  pluralFormFiltered: boolean;
  pluralFormExcluded: boolean;
  searchType?: string;
  usedFilter?: 'USED' | 'UNUSED';
  statusFilter?: string;
  doNotTranslateFilter?: boolean;
  tmTextUnitCreatedBefore?: string;
  tmTextUnitCreatedAfter?: string;
  tmTextUnitVariantCreatedBefore?: string;
  tmTextUnitVariantCreatedAfter?: string;
  limit: number;
  offset: number;
};

export async function searchTextUnits(request: TextUnitSearchRequest): Promise<ApiTextUnit[]> {
  const body = buildSearchBody(request);
  const response = await postJson<SearchTextUnitsHybridResponse>(
    '/api/textunits/search-hybrid',
    body,
  );

  if (response?.results) {
    return response.results;
  }

  if (response?.error) {
    throw new Error(response.error.message || 'Search failed');
  }

  if (response?.pollingToken) {
    return pollForHybridResults(response.pollingToken);
  }

  throw new Error('Unexpected search response');
}

export async function saveTextUnit(request: SaveTextUnitRequest): Promise<ApiTextUnit> {
  return postJson<ApiTextUnit>('/api/textunits', request);
}

export async function deleteTextUnitCurrentVariant(
  textUnitCurrentVariantId: number,
): Promise<void> {
  const response = await fetch(`/api/textunits/${textUnitCurrentVariantId}`, {
    method: 'DELETE',
    credentials: 'same-origin',
    headers: {
      Accept: 'application/json',
    },
  });

  if (!response.ok) {
    const message = await response.text().catch(() => '');
    const error: Error & { status?: number } = new Error(
      message || `Request failed with status ${response.status}`,
    );
    error.status = response.status;
    throw error;
  }
}

export async function checkTextUnitIntegrity(
  request: TextUnitIntegrityCheckRequest,
): Promise<TextUnitIntegrityCheckResult> {
  return postJson<TextUnitIntegrityCheckResult>('/api/textunits/check', request);
}

export async function fetchTextUnitHistory(
  tmTextUnitId: number,
  bcp47Tag: string,
): Promise<ApiTextUnitHistoryItem[]> {
  const params = new URLSearchParams();
  params.set('bcp47Tag', bcp47Tag);
  return getJson<ApiTextUnitHistoryItem[]>(
    `/api/textunits/${tmTextUnitId}/history?${params.toString()}`,
  );
}

export async function fetchGitBlameWithUsages(
  tmTextUnitId: number,
): Promise<ApiGitBlameWithUsage[]> {
  const params = new URLSearchParams();
  params.set('tmTextUnitId', String(tmTextUnitId));
  return getJson<ApiGitBlameWithUsage[]>(`/api/textunits/gitBlameWithUsages?${params.toString()}`);
}

function buildSearchBody(request: TextUnitSearchRequest): TextUnitSearchBody {
  const limit = request.limit ?? DEFAULT_SEARCH_LIMIT;
  const offset = request.offset ?? 0;
  const searchText = request.searchText?.trim();

  const body: TextUnitSearchBody = {
    repositoryIds: request.repositoryIds,
    localeTags: request.localeTags,
    pluralFormFiltered: true,
    pluralFormExcluded: false,
    limit,
    offset,
  };

  if (request.statusFilter) {
    body.statusFilter = request.statusFilter;
  }

  if (request.usedFilter) {
    body.usedFilter = request.usedFilter;
  }

  if (typeof request.doNotTranslateFilter === 'boolean') {
    body.doNotTranslateFilter = request.doNotTranslateFilter;
  }

  if (request.tmTextUnitCreatedBefore) {
    body.tmTextUnitCreatedBefore = request.tmTextUnitCreatedBefore;
  }

  if (request.tmTextUnitCreatedAfter) {
    body.tmTextUnitCreatedAfter = request.tmTextUnitCreatedAfter;
  }

  if (request.tmTextUnitVariantCreatedBefore) {
    body.tmTextUnitVariantCreatedBefore = request.tmTextUnitVariantCreatedBefore;
  }

  if (request.tmTextUnitVariantCreatedAfter) {
    body.tmTextUnitVariantCreatedAfter = request.tmTextUnitVariantCreatedAfter;
  }

  if (searchText) {
    body.searchType = request.searchType.toUpperCase();
    switch (request.searchAttribute) {
      case 'source':
        body.source = searchText;
        break;
      case 'target':
        body.target = searchText;
        break;
      case 'asset':
        body.assetPath = searchText;
        break;
      case 'location':
        body.assetTextUnitUsages = searchText;
        break;
      case 'pluralFormOther':
        body.pluralFormOther = searchText;
        break;
      case 'tmTextUnitIds':
        body.tmTextUnitIds = extractNumericIds(searchText);
        break;
      case 'stringId':
      default:
        body.name = searchText;
        break;
    }
  }

  return body;
}

function extractNumericIds(input: string): number[] {
  return input
    .split(/[,\s]+/)
    .map((value) => parseInt(value, 10))
    .filter((value) => !Number.isNaN(value));
}

async function pollForHybridResults(pollingToken: {
  requestId: string;
  recommendedPollingDurationMillis?: number;
}): Promise<ApiTextUnit[]> {
  const timeout = getPollingTimeoutMs(pollingToken.recommendedPollingDurationMillis);

  const results = await poll<ApiTextUnit[] | null>(
    async () => {
      const response = await getJson<SearchTextUnitsHybridResponse>(
        `/api/textunits/search-hybrid/results/${pollingToken.requestId}`,
      );

      if (response?.results) {
        return response.results;
      }

      if (response?.error) {
        throw createNonTransientError(response.error.message || 'Search failed');
      }

      return null;
    },
    {
      intervalMs: DEFAULT_POLL_INTERVAL_MS,
      maxIntervalMs: MAX_POLL_INTERVAL_MS,
      timeoutMs: timeout,
      timeoutMessage: 'Timed out while waiting for search results',
      isTransientError: isTransientHttpError,
      shouldStop: (response) => response !== null,
    },
  );

  return results ?? [];
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
    const error: Error & { status?: number } = new Error(
      text || `Request failed with status ${response.status}`,
    );
    error.status = response.status;
    throw error;
  }
  return text ? (JSON.parse(text) as TResponse) : (undefined as TResponse);
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
    throw new Error(text || `Request failed with status ${response.status}`);
  }
  return text ? (JSON.parse(text) as TResponse) : (undefined as TResponse);
}

const getPollingTimeoutMs = (recommended?: number) => {
  if (!Number.isFinite(recommended) || typeof recommended !== 'number' || recommended <= 0) {
    return ASYNC_POLL_TIMEOUT_MS;
  }
  return recommended;
};

const createNonTransientError = (message: string) => {
  const error: Error & { isTransient?: boolean } = new Error(message);
  error.isTransient = false;
  return error;
};
