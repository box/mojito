type SessionEnvelope = {
  version: 1;
  savedAt: number;
  payload: unknown;
};

type SessionEntry = {
  storageKey: string;
  savedAt: number;
};

type LoadSessionTokenPayloadOptions = {
  storagePrefix: string;
  key: string;
  touch?: boolean;
};

type SaveSessionTokenPayloadOptions = {
  storagePrefix: string;
  payload: unknown;
  existingKey?: string | null;
  maxEntries?: number;
};

const ENVELOPE_VERSION = 1;
const DEFAULT_MAX_ENTRIES = 48;

function getStorage(): Storage | null {
  if (typeof window === 'undefined') {
    return null;
  }
  try {
    return window.sessionStorage;
  } catch {
    return null;
  }
}

function parseEnvelope(raw: string): SessionEnvelope | null {
  try {
    const parsed = JSON.parse(raw) as unknown;
    if (!parsed || typeof parsed !== 'object') {
      return null;
    }
    const envelope = parsed as SessionEnvelope;
    if (
      envelope.version !== ENVELOPE_VERSION ||
      typeof envelope.savedAt !== 'number' ||
      !Number.isFinite(envelope.savedAt) ||
      !('payload' in envelope)
    ) {
      return null;
    }
    return envelope;
  } catch {
    return null;
  }
}

function listEntries(storage: Storage, storagePrefix: string): SessionEntry[] {
  const matchingKeys: string[] = [];
  for (let index = 0; index < storage.length; index += 1) {
    const storageKey = storage.key(index);
    if (storageKey && storageKey.startsWith(storagePrefix)) {
      matchingKeys.push(storageKey);
    }
  }

  const entries: SessionEntry[] = [];
  matchingKeys.forEach((storageKey) => {
    const raw = storage.getItem(storageKey);
    if (!raw) {
      return;
    }
    const envelope = parseEnvelope(raw);
    if (!envelope) {
      storage.removeItem(storageKey);
      return;
    }
    entries.push({ storageKey, savedAt: envelope.savedAt });
  });

  return entries;
}

function isQuotaExceededError(error: unknown): boolean {
  if (!error || typeof error !== 'object') {
    return false;
  }

  const maybeError = error as { name?: string; code?: number };
  return (
    maybeError.name === 'QuotaExceededError' ||
    maybeError.name === 'NS_ERROR_DOM_QUOTA_REACHED' ||
    maybeError.code === 22 ||
    maybeError.code === 1014
  );
}

function enforceMaxEntries(
  storage: Storage,
  storagePrefix: string,
  maxEntries: number,
  protectedStorageKey: string,
) {
  if (maxEntries < 1) {
    return;
  }

  const entries = listEntries(storage, storagePrefix)
    .filter((entry) => entry.storageKey !== protectedStorageKey)
    .sort((first, second) => first.savedAt - second.savedAt);

  const overflow = entries.length + 1 - maxEntries;
  if (overflow <= 0) {
    return;
  }

  for (let index = 0; index < overflow; index += 1) {
    const entry = entries[index];
    if (!entry) {
      break;
    }
    storage.removeItem(entry.storageKey);
  }
}

function evictOldestEntries(
  storage: Storage,
  storagePrefix: string,
  protectedStorageKey: string,
  maxToRemove: number,
): number {
  const entries = listEntries(storage, storagePrefix)
    .filter((entry) => entry.storageKey !== protectedStorageKey)
    .sort((first, second) => first.savedAt - second.savedAt);

  if (entries.length === 0 || maxToRemove <= 0) {
    return 0;
  }

  const removeCount = Math.min(maxToRemove, entries.length);
  for (let index = 0; index < removeCount; index += 1) {
    const entry = entries[index];
    if (!entry) {
      break;
    }
    storage.removeItem(entry.storageKey);
  }

  return removeCount;
}

function touchEntry(storage: Storage, storageKey: string, payload: unknown) {
  const nextEnvelope: SessionEnvelope = {
    version: ENVELOPE_VERSION,
    savedAt: Date.now(),
    payload,
  };

  try {
    storage.setItem(storageKey, JSON.stringify(nextEnvelope));
  } catch {
    // Ignore touch failures.
  }
}

export function createSessionToken() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

export function loadSessionTokenPayload({
  storagePrefix,
  key,
  touch = true,
}: LoadSessionTokenPayloadOptions): unknown {
  const trimmedKey = key.trim();
  if (!trimmedKey) {
    return null;
  }

  const storage = getStorage();
  if (!storage) {
    return null;
  }

  const storageKey = `${storagePrefix}${trimmedKey}`;
  const raw = storage.getItem(storageKey);
  if (!raw) {
    return null;
  }

  const envelope = parseEnvelope(raw);
  if (!envelope) {
    storage.removeItem(storageKey);
    return null;
  }

  if (touch) {
    touchEntry(storage, storageKey, envelope.payload);
  }

  return envelope.payload;
}

export function saveSessionTokenPayload({
  storagePrefix,
  payload,
  existingKey,
  maxEntries = DEFAULT_MAX_ENTRIES,
}: SaveSessionTokenPayloadOptions): string {
  const key =
    existingKey && existingKey.trim().length > 0 ? existingKey.trim() : createSessionToken();
  const storage = getStorage();
  if (!storage) {
    return key;
  }

  const storageKey = `${storagePrefix}${key}`;
  const serialized = JSON.stringify({
    version: ENVELOPE_VERSION,
    savedAt: Date.now(),
    payload,
  } satisfies SessionEnvelope);

  const attempts = 6;
  for (let attempt = 0; attempt < attempts; attempt += 1) {
    try {
      storage.setItem(storageKey, serialized);
      enforceMaxEntries(storage, storagePrefix, maxEntries, storageKey);
      return key;
    } catch (error: unknown) {
      if (!isQuotaExceededError(error)) {
        return key;
      }

      const entries = listEntries(storage, storagePrefix);
      if (entries.length === 0) {
        return key;
      }

      const removed = evictOldestEntries(
        storage,
        storagePrefix,
        storageKey,
        Math.max(1, Math.ceil(entries.length / 4)),
      );
      if (removed === 0) {
        return key;
      }
    }
  }

  return key;
}
