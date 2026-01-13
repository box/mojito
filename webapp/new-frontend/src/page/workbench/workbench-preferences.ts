import { clampWorksetSize } from './workbench-helpers';

const WORKSET_SIZE_KEY = 'workbench.worksetSize.v1';

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
