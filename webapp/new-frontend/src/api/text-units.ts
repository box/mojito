export type SearchAttribute =
  | 'stringId'
  | 'source'
  | 'target'
  | 'asset'
  | 'location'
  | 'pluralFormOther'
  | 'tmTextUnitIds';

export type SearchType = 'exact' | 'contains' | 'ilike';

// Backend-defined status values for a translation variant.
// Note: untranslated rows can still appear in results, but will have `target: null` (and `status` may be null/undefined).
export type ApiTextUnitStatus = 'APPROVED' | 'REVIEW_NEEDED' | 'TRANSLATION_NEEDED';

export type ApiTextUnit = {
  tmTextUnitId: number;
  tmTextUnitVariantId?: number | null;
  tmTextUnitCurrentVariantId?: number | null;
  localeId?: number | null;
  name: string;
  source?: string | null;
  comment?: string | null;
  target?: string | null;
  targetLocale: string;
  targetComment?: string | null;
  assetPath?: string | null;
  assetTextUnitUsages?: string | null;
  repositoryName?: string | null;
  status?: ApiTextUnitStatus | null;
  includedInLocalizedFile?: boolean;
};

// Backend default (TextUnitSearchBody.limit) is 10; keep the client aligned if a caller omits limit.
const DEFAULT_SEARCH_LIMIT = 10;
const ASYNC_POLL_TIMEOUT_MS = 60_000;
const DEFAULT_POLL_INTERVAL_MS = 1000;

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

export async function checkTextUnitIntegrity(
  request: TextUnitIntegrityCheckRequest,
): Promise<TextUnitIntegrityCheckResult> {
  return postJson<TextUnitIntegrityCheckResult>('/api/textunits/check', request);
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
  const startedAt = Date.now();
  const timeout = pollingToken.recommendedPollingDurationMillis ?? ASYNC_POLL_TIMEOUT_MS;

  for (;;) {
    if (Date.now() - startedAt > timeout) {
      throw new Error('Timed out while waiting for search results');
    }

    const response = await getJson<SearchTextUnitsHybridResponse>(
      `/api/textunits/search-hybrid/results/${pollingToken.requestId}`,
    );

    if (response?.results) {
      return response.results;
    }

    if (response?.error) {
      throw new Error(response.error.message || 'Search failed');
    }

    await delay(DEFAULT_POLL_INTERVAL_MS);
  }
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
    throw new Error(text || `Request failed with status ${response.status}`);
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

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));
