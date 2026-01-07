import type { TextUnitSearchRequest } from '../../api/text-units';
import type { WorkbenchRow } from './workbench-types';

export type WorkbenchShareMode = 'search' | 'search-ids';

export type WorkbenchShareLocaleSelectionType = 'ASK_RECIPIENT' | 'USE_SEARCH';

export type WorkbenchSharePayload = {
  version: 'workbench-share/v1';
  mode: WorkbenchShareMode;
  createdAt: string;
  localeFocus: WorkbenchShareLocaleSelectionType;
  searchRequest: TextUnitSearchRequest;
};

export function buildWorkbenchSharePayload({
  mode,
  searchRequest,
  rows,
  localeFocus,
}: {
  mode: WorkbenchShareMode;
  searchRequest: TextUnitSearchRequest;
  rows: WorkbenchRow[];
  localeFocus: WorkbenchShareLocaleSelectionType;
}): WorkbenchSharePayload {
  const normalizedSearchRequest: TextUnitSearchRequest =
    localeFocus === 'ASK_RECIPIENT' ? { ...searchRequest, localeTags: [] } : searchRequest;

  const ids = Array.from(new Set(rows.map((row) => row.tmTextUnitId)));

  const searchRequestWithIds: TextUnitSearchRequest =
    mode === 'search'
      ? normalizedSearchRequest
      : {
          ...normalizedSearchRequest,
          searchAttribute: 'tmTextUnitIds',
          searchType: 'contains',
          searchText: ids.join(','),
          limit: ids.length || normalizedSearchRequest.limit,
          offset: 0,
        };

  return {
    version: 'workbench-share/v1',
    mode,
    createdAt: new Date().toISOString(),
    localeFocus,
    searchRequest: searchRequestWithIds,
  };
}

export async function saveWorkbenchShare(payload: WorkbenchSharePayload): Promise<string> {
  const response = await fetch('/api/clobstorage', {
    method: 'POST',
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    body: JSON.stringify(payload),
  });

  const text = await response.text();
  if (!response.ok) {
    throw new Error(text || `Request failed with status ${response.status}`);
  }

  try {
    const parsed = JSON.parse(text) as unknown;
    if (typeof parsed === 'string') {
      return parsed;
    }
  } catch {
    // Fall through to raw text.
  }
  return text.trim();
}

export function buildWorkbenchShareUrl(shareId: string): string {
  const basePath = (import.meta.env.BASE_URL || '/').replace(/\/?$/, '/');
  const baseUrl = new URL(basePath, window.location.origin);
  baseUrl.pathname = `${basePath.replace(/\/$/, '')}/workbench`;
  baseUrl.searchParams.set('shareId', shareId);
  return baseUrl.toString();
}

export async function loadWorkbenchShare(shareId: string): Promise<WorkbenchSharePayload> {
  const response = await fetch(`/api/clobstorage/${encodeURIComponent(shareId)}`, {
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

  const payload = JSON.parse(text) as WorkbenchSharePayload & { localeFocus?: unknown };
  if (payload?.version !== 'workbench-share/v1') {
    throw new Error('Unsupported share payload');
  }
  const localeFocus =
    payload.localeFocus === 'ASK_RECIPIENT' || payload.localeFocus === 'USE_SEARCH'
      ? payload.localeFocus
      : 'USE_SEARCH';
  return { ...payload, localeFocus };
}
