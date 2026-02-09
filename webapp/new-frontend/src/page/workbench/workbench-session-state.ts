import type { TextUnitSearchRequest } from '../../api/text-units';
import { loadSessionTokenPayload, saveSessionTokenPayload } from '../../utils/sessionTokenStore';

const STORAGE_PREFIX = 'workbench.searchState.v1:';
const STORAGE_MAX_ENTRIES = 48;
export const WORKBENCH_SESSION_QUERY_KEY = 'ws';

function isSearchRequest(value: unknown): value is TextUnitSearchRequest {
  return typeof value === 'object' && value !== null;
}

export function loadWorkbenchSessionSearch(key: string): TextUnitSearchRequest | null {
  const payload = loadSessionTokenPayload({ storagePrefix: STORAGE_PREFIX, key });
  if (!isSearchRequest(payload)) {
    return null;
  }
  return payload;
}

export function saveWorkbenchSessionSearch(
  searchRequest: TextUnitSearchRequest,
  existingKey?: string | null,
): string {
  return saveSessionTokenPayload({
    storagePrefix: STORAGE_PREFIX,
    payload: searchRequest,
    existingKey,
    maxEntries: STORAGE_MAX_ENTRIES,
  });
}
