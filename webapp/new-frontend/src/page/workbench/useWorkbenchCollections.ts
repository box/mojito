import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo } from 'react';

import type {
  WorkbenchCollection,
  WorkbenchCollectionEntry,
  WorkbenchCollectionsState,
} from './workbench-types';

const COLLECTIONS_QUERY_KEY = ['workbench-collections'];
const STORAGE_KEY = 'workbench.collections.v1';

const emptyState: WorkbenchCollectionsState = { collections: [], activeCollectionId: null };

function readFromStorage(): WorkbenchCollectionsState {
  if (typeof window === 'undefined' || typeof window.localStorage === 'undefined') {
    return emptyState;
  }
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return emptyState;
    }
    const parsed = JSON.parse(raw) as unknown;
    if (
      typeof parsed !== 'object' ||
      parsed === null ||
      !Array.isArray((parsed as { collections?: unknown }).collections)
    ) {
      return emptyState;
    }

    const collections: WorkbenchCollection[] = (parsed as WorkbenchCollectionsState).collections
      .filter(
        (item): item is WorkbenchCollection =>
          Boolean(item) &&
          typeof item === 'object' &&
          typeof item.id === 'string' &&
          typeof item.name === 'string',
      )
      .map((item) => {
        const rawEntries: unknown[] | undefined = (item as { entries?: unknown[] }).entries;

        const parsedEntries: WorkbenchCollectionEntry[] = Array.isArray(rawEntries)
          ? rawEntries
              .map((entry) => {
                if (typeof entry !== 'object' || entry === null) {
                  return null;
                }
                const tmTextUnitId =
                  'tmTextUnitId' in entry && typeof entry.tmTextUnitId === 'number'
                    ? entry.tmTextUnitId
                    : null;
                if (tmTextUnitId === null) {
                  return null;
                }
                const repositoryId =
                  'repositoryId' in entry && typeof entry.repositoryId === 'number'
                    ? entry.repositoryId
                    : null;
                return { tmTextUnitId, repositoryId };
              })
              .filter((value): value is WorkbenchCollectionEntry => Boolean(value))
          : [];

        const nextEntries = normalizeCollections([
          { id: 'tmp', name: '', entries: parsedEntries, updatedAt: 0 },
        ])[0].entries;

        return {
          id: item.id,
          name: item.name,
          entries: nextEntries,
          updatedAt:
            typeof (item as { updatedAt?: unknown }).updatedAt === 'number'
              ? (item as { updatedAt: number }).updatedAt
              : Date.now(),
        };
      });

    const activeCollectionId =
      typeof (parsed as WorkbenchCollectionsState).activeCollectionId === 'string'
        ? (parsed as WorkbenchCollectionsState).activeCollectionId
        : null;

    return { collections, activeCollectionId };
  } catch (error) {
    void error;
    return emptyState;
  }
}

function persistState(state: WorkbenchCollectionsState): void {
  if (typeof window === 'undefined' || typeof window.localStorage === 'undefined') {
    return;
  }
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  } catch (error) {
    void error;
  }
}

function clearPersistedState(): void {
  if (typeof window === 'undefined' || typeof window.localStorage === 'undefined') {
    return;
  }
  try {
    window.localStorage.removeItem(STORAGE_KEY);
  } catch (error) {
    void error;
  }
}

function generateCollectionId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `col_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
}

function makeUniqueName(name: string, collections: WorkbenchCollection[]): string {
  const existing = new Set(collections.map((item) => item.name.toLowerCase()));
  if (!existing.has(name.toLowerCase())) {
    return name;
  }
  let suffix = 2;
  while (suffix < 200) {
    const candidate = `${name} ${suffix}`;
    if (!existing.has(candidate.toLowerCase())) {
      return candidate;
    }
    suffix += 1;
  }
  return `${name} ${Date.now()}`;
}

function normalizeCollections(collections: WorkbenchCollection[]): WorkbenchCollection[] {
  return collections.map((collection) => {
    const byId = new Map<number, WorkbenchCollectionEntry>();
    collection.entries.forEach((entry) => {
      if (!byId.has(entry.tmTextUnitId)) {
        byId.set(entry.tmTextUnitId, entry);
      }
    });
    const nextEntries = Array.from(byId.values());
    if (nextEntries.length === collection.entries.length) {
      return collection;
    }
    return { ...collection, entries: nextEntries };
  });
}

export function useWorkbenchCollections() {
  const queryClient = useQueryClient();

  const hydrateState = useCallback(() => {
    const cached = queryClient.getQueryData<WorkbenchCollectionsState>(COLLECTIONS_QUERY_KEY);
    if (cached) {
      return cached;
    }
    const stored = readFromStorage();
    queryClient.setQueryData(COLLECTIONS_QUERY_KEY, stored);
    return stored;
  }, [queryClient]);

  const { data: state = emptyState } = useQuery({
    queryKey: COLLECTIONS_QUERY_KEY,
    queryFn: hydrateState,
    staleTime: Infinity,
  });

  useEffect(() => {
    const handler = (event: StorageEvent) => {
      if (event.key !== STORAGE_KEY) {
        return;
      }
      queryClient.setQueryData(COLLECTIONS_QUERY_KEY, readFromStorage());
    };
    window.addEventListener('storage', handler);
    return () => window.removeEventListener('storage', handler);
  }, [queryClient]);

  const setState = useCallback(
    (
      updater:
        | WorkbenchCollectionsState
        | ((current: WorkbenchCollectionsState) => WorkbenchCollectionsState),
    ) => {
      return queryClient.setQueryData<WorkbenchCollectionsState>(
        COLLECTIONS_QUERY_KEY,
        (current) => {
          const base = current ?? hydrateState();
          const updated = typeof updater === 'function' ? updater(base) : updater;
          const normalizedCollections = normalizeCollections(updated.collections);
          const nextState =
            normalizedCollections === updated.collections
              ? updated
              : { ...updated, collections: normalizedCollections };
          persistState(nextState);
          return nextState;
        },
      );
    },
    [hydrateState, queryClient],
  );

  const activeCollection = useMemo(
    () => state.collections.find((item) => item.id === state.activeCollectionId) ?? null,
    [state.activeCollectionId, state.collections],
  );

  const createCollection = useCallback(
    (name?: string) => {
      let createdId: string | null = null;
      setState((current) => {
        const trimmed = (name ?? '').trim() || 'Collection';
        const id = generateCollectionId();
        createdId = id;
        const collection: WorkbenchCollection = {
          id,
          name: makeUniqueName(trimmed, current.collections),
          entries: [],
          updatedAt: Date.now(),
        };
        return {
          activeCollectionId: id,
          collections: [collection, ...current.collections],
        };
      });
      return createdId;
    },
    [setState],
  );

  const selectCollection = useCallback(
    (id: string | null) => {
      setState((current) => {
        if (id === null) {
          if (current.activeCollectionId === null) {
            return current;
          }
          return { ...current, activeCollectionId: null };
        }
        if (current.activeCollectionId === id) {
          return current;
        }
        if (!current.collections.some((item) => item.id === id)) {
          return current;
        }
        return { ...current, activeCollectionId: id };
      });
    },
    [setState],
  );

  const renameCollection = useCallback(
    (id: string, name: string) => {
      setState((current) => {
        const trimmed = name.trim();
        if (!trimmed) {
          return current;
        }
        const index = current.collections.findIndex((item) => item.id === id);
        if (index === -1) {
          return current;
        }
        const collection = current.collections[index];
        const nextName = makeUniqueName(
          trimmed,
          current.collections.filter((item) => item.id !== id),
        );
        const updated: WorkbenchCollection = {
          ...collection,
          name: nextName,
          updatedAt: Date.now(),
        };
        const nextCollections = [...current.collections];
        nextCollections[index] = updated;
        return { ...current, collections: nextCollections };
      });
    },
    [setState],
  );

  const deleteCollection = useCallback(
    (id: string) => {
      setState((current) => {
        const nextCollections = current.collections.filter((item) => item.id !== id);
        if (nextCollections.length === current.collections.length) {
          return current;
        }
        const nextActive =
          current.activeCollectionId === id
            ? (nextCollections[0]?.id ?? null)
            : current.activeCollectionId;
        return { collections: nextCollections, activeCollectionId: nextActive ?? null };
      });
    },
    [setState],
  );

  const deleteAllCollections = useCallback(() => {
    setState(() => {
      const cleared: WorkbenchCollectionsState = { collections: [], activeCollectionId: null };
      clearPersistedState();
      persistState(cleared);
      return cleared;
    });
  }, [setState]);

  const clearActiveCollection = useCallback(() => {
    setState((current) => {
      const index = current.collections.findIndex((item) => item.id === current.activeCollectionId);
      if (index === -1) {
        return current;
      }
      const collection = current.collections[index];
      if (collection.entries.length === 0) {
        return current;
      }
      const nextCollections = [...current.collections];
      nextCollections[index] = {
        ...collection,
        entries: [],
        updatedAt: Date.now(),
      };
      return { ...current, collections: nextCollections };
    });
  }, [setState]);

  const removeFromActiveCollection = useCallback(
    (textUnitId: number) => {
      setState((current) => {
        const index = current.collections.findIndex(
          (item) => item.id === current.activeCollectionId,
        );
        if (index === -1) {
          return current;
        }
        const collection = current.collections[index];
        const nextEntries = collection.entries.filter((entry) => entry.tmTextUnitId !== textUnitId);
        if (nextEntries.length === collection.entries.length) {
          return current;
        }
        const nextCollections = [...current.collections];
        nextCollections[index] = {
          ...collection,
          entries: nextEntries,
          updatedAt: Date.now(),
        };
        return { ...current, collections: nextCollections };
      });
    },
    [setState],
  );

  const addToActiveCollection = useCallback(
    (textUnitId: number, repositoryId: number | null = null) => {
      if (repositoryId === null) {
        return;
      }
      setState((current) => {
        const index = current.collections.findIndex(
          (item) => item.id === current.activeCollectionId,
        );
        if (index === -1) {
          return current;
        }
        const collection = current.collections[index];
        if (collection.entries.some((entry) => entry.tmTextUnitId === textUnitId)) {
          return current;
        }
        const nextCollections = [...current.collections];
        nextCollections[index] = {
          ...collection,
          entries: [...collection.entries, { tmTextUnitId: textUnitId, repositoryId }],
          updatedAt: Date.now(),
        };
        return { ...current, collections: nextCollections };
      });
    },
    [setState],
  );

  const addEntriesToActiveCollection = useCallback(
    (entries: WorkbenchCollectionEntry[]) => {
      setState((current) => {
        const index = current.collections.findIndex(
          (item) => item.id === current.activeCollectionId,
        );
        if (index === -1 || entries.length === 0) {
          return current;
        }
        const collection = current.collections[index];
        const existingIds = new Set(collection.entries.map((entry) => entry.tmTextUnitId));
        const filtered = entries.filter(
          (entry) => entry.repositoryId !== null && !existingIds.has(entry.tmTextUnitId),
        );
        if (!filtered.length) {
          return current;
        }
        const nextCollections = [...current.collections];
        nextCollections[index] = {
          ...collection,
          entries: [...collection.entries, ...filtered],
          updatedAt: Date.now(),
        };
        return { ...current, collections: nextCollections };
      });
    },
    [setState],
  );

  return {
    collections: state.collections,
    activeCollection,
    activeCollectionId: state.activeCollectionId,
    addToActiveCollection,
    addEntriesToActiveCollection,
    removeFromActiveCollection,
    createCollection,
    selectCollection,
    renameCollection,
    deleteCollection,
    deleteAllCollections,
    clearActiveCollection,
  };
}
