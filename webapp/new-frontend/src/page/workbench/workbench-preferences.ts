import { clampWorksetSize } from './workbench-helpers';

const WORKSET_SIZE_KEY = 'workbench.worksetSize.v1';
export const PREFERRED_LOCALES_KEY = 'workbench.preferredLocales.v1';

function getStorage(): Storage | null {
  if (typeof window === 'undefined') {
    return null;
  }
  try {
    return window.localStorage;
  } catch {
    return null;
  }
}

export function loadPreferredWorksetSize(): number | null {
  const storage = getStorage();
  if (!storage) {
    return null;
  }
  const raw = storage.getItem(WORKSET_SIZE_KEY);
  if (!raw) {
    return null;
  }
  const parsed = parseInt(raw, 10);
  if (!Number.isFinite(parsed)) {
    return null;
  }
  return clampWorksetSize(parsed);
}

export function savePreferredWorksetSize(value: number | null): void {
  const storage = getStorage();
  if (!storage) {
    return;
  }
  if (value === null) {
    storage.removeItem(WORKSET_SIZE_KEY);
    return;
  }
  storage.setItem(WORKSET_SIZE_KEY, String(clampWorksetSize(value)));
}

function normalizeLocaleList(locales: unknown): string[] {
  if (!Array.isArray(locales)) {
    return [];
  }
  const seen = new Set<string>();
  const normalized: string[] = [];
  locales.forEach((entry) => {
    if (typeof entry !== 'string') {
      return;
    }
    const trimmed = entry.trim();
    if (!trimmed) {
      return;
    }
    const lowered = trimmed.toLowerCase();
    if (seen.has(lowered)) {
      return;
    }
    seen.add(lowered);
    normalized.push(trimmed);
  });
  return normalized;
}

export function loadPreferredLocales(): string[] {
  const storage = getStorage();
  if (!storage) {
    return [];
  }
  const raw = storage.getItem(PREFERRED_LOCALES_KEY);
  if (!raw) {
    return [];
  }
  try {
    const parsed: unknown = JSON.parse(raw);
    return normalizeLocaleList(parsed);
  } catch {
    return [];
  }
}

export function savePreferredLocales(locales: string[]): void {
  const storage = getStorage();
  if (!storage) {
    return;
  }
  const normalized = normalizeLocaleList(locales);
  if (normalized.length === 0) {
    storage.removeItem(PREFERRED_LOCALES_KEY);
    return;
  }
  storage.setItem(PREFERRED_LOCALES_KEY, JSON.stringify(normalized));
}
