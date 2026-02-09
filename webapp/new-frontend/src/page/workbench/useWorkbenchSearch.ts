import type { InfiniteData } from '@tanstack/react-query';
import { useInfiniteQuery } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import type { ApiRepository } from '../../api/repositories';
import {
  type ApiTextUnit,
  type SearchAttribute,
  searchTextUnits,
  type SearchType,
  type TextUnitSearchRequest,
} from '../../api/text-units';
import { useDebouncedValue } from '../../hooks/useDebouncedValue';
import { useRepositories } from '../../hooks/useRepositories';
import type { LocaleSelectionOption } from '../../utils/localeSelection';
import { useLocaleOptionsWithDisplayNames, useLocaleSelection } from '../../utils/localeSelection';
import type { RepositorySelectionOption } from '../../utils/repositorySelection';
import {
  useRepositorySelection,
  useRepositorySelectionOptions,
} from '../../utils/repositorySelection';
import { WORKSET_SIZE_DEFAULT } from './workbench-constants';
import { clampWorksetSize, mapApiTextUnitToRow, serializeSearchRequest } from './workbench-helpers';
import { loadPreferredWorksetSize } from './workbench-preferences';
import type { StatusFilterValue, WorkbenchRow } from './workbench-types';

type SearchQueryKey = ['workbench-search', TextUnitSearchRequest | null];

type Params = {
  initialSearchRequest?: TextUnitSearchRequest | null;
  canEditLocale?: (locale: string) => boolean;
};

type SearchState = {
  repositoryOptions: RepositorySelectionOption[];
  repositories: ApiRepository[];
  localeOptions: LocaleSelectionOption[];
  isRepositoriesLoading: boolean;
  repositoryErrorMessage: string | null;
  selectedRepositoryIds: number[];
  selectedLocaleTags: string[];
  onChangeRepositorySelection: (next: number[]) => void;
  onChangeLocaleSelection: (next: string[]) => void;
  searchAttribute: SearchAttribute;
  searchType: SearchType;
  searchInputValue: string;
  onChangeSearchInput: (value: string) => void;
  onSubmitSearch: () => void;
  onChangeSearchAttribute: (value: SearchAttribute) => void;
  onChangeSearchType: (value: SearchType) => void;
  statusFilter: StatusFilterValue;
  includeUsed: boolean;
  includeUnused: boolean;
  includeTranslate: boolean;
  includeDoNotTranslate: boolean;
  onChangeStatusFilter: (value: StatusFilterValue) => void;
  onChangeIncludeUsed: (value: boolean) => void;
  onChangeIncludeUnused: (value: boolean) => void;
  onChangeIncludeTranslate: (value: boolean) => void;
  onChangeIncludeDoNotTranslate: (value: boolean) => void;
  createdBefore: string | null;
  createdAfter: string | null;
  translationCreatedBefore: string | null;
  translationCreatedAfter: string | null;
  onChangeCreatedBefore: (value: string | null) => void;
  onChangeCreatedAfter: (value: string | null) => void;
  onChangeTranslationCreatedBefore: (value: string | null) => void;
  onChangeTranslationCreatedAfter: (value: string | null) => void;
  worksetSize: number;
  onChangeWorksetSize: (value: number) => void;
  canSearch: boolean;
  activeSearchRequest: TextUnitSearchRequest | null;
  isSearchLoading: boolean;
  searchErrorMessage: string | null;
  rows: WorkbenchRow[];
  hasMoreResults: boolean;
  refetchSearch: () => Promise<unknown>;
  resetSearch: () => void;
  hasHydratedSearch: boolean;
};

type SearchRequestInputs = {
  repositoryIds: number[];
  localeTags: string[];
  searchAttribute: SearchAttribute;
  searchType: SearchType;
  searchText: string;
  searchLimit: number;
  statusFilter: StatusFilterValue;
  includeUsed: boolean;
  includeUnused: boolean;
  includeTranslate: boolean;
  includeDoNotTranslate: boolean;
  createdBefore: string | null;
  createdAfter: string | null;
  translationCreatedBefore: string | null;
  translationCreatedAfter: string | null;
};

function buildSearchRequestFromInputs({
  repositoryIds,
  localeTags,
  searchAttribute,
  searchType,
  searchText,
  searchLimit,
  statusFilter,
  includeUsed,
  includeUnused,
  includeTranslate,
  includeDoNotTranslate,
  createdBefore,
  createdAfter,
  translationCreatedBefore,
  translationCreatedAfter,
}: SearchRequestInputs): TextUnitSearchRequest | null {
  if (!repositoryIds.length || !localeTags.length) {
    return null;
  }

  const usedFilter = (() => {
    if (!includeUsed && !includeUnused) {
      return undefined;
    }
    if (includeUsed && !includeUnused) {
      return 'USED' as const;
    }
    if (!includeUsed && includeUnused) {
      return 'UNUSED' as const;
    }
    return undefined;
  })();

  const doNotTranslateFilter = (() => {
    if (!includeTranslate && !includeDoNotTranslate) {
      return undefined;
    }
    if (includeDoNotTranslate && !includeTranslate) {
      return true;
    }
    if (includeTranslate && !includeDoNotTranslate) {
      return false;
    }
    return undefined;
  })();

  return {
    repositoryIds,
    localeTags,
    searchAttribute,
    searchType,
    searchText,
    limit: searchLimit,
    offset: 0,
    statusFilter: statusFilter === 'ALL' ? undefined : statusFilter,
    usedFilter,
    doNotTranslateFilter,
    tmTextUnitCreatedBefore: createdBefore ?? undefined,
    tmTextUnitCreatedAfter: createdAfter ?? undefined,
    tmTextUnitVariantCreatedBefore: translationCreatedBefore ?? undefined,
    tmTextUnitVariantCreatedAfter: translationCreatedAfter ?? undefined,
  };
}

export function useWorkbenchSearch({ initialSearchRequest, canEditLocale }: Params): SearchState {
  const [activeSearchRequest, setActiveSearchRequest] = useState<TextUnitSearchRequest | null>(
    null,
  );
  const setActiveSearchRequestIfChanged = useCallback((next: TextUnitSearchRequest | null) => {
    setActiveSearchRequest((current) => {
      if (current === next) {
        return current;
      }
      if (current === null || next === null) {
        return current === next ? current : next;
      }
      return serializeSearchRequest(current) === serializeSearchRequest(next) ? current : next;
    });
  }, []);
  const updateActiveSearchRequest = useCallback(
    (updater: (current: TextUnitSearchRequest | null) => TextUnitSearchRequest | null): void => {
      setActiveSearchRequest((current) => {
        const next = updater(current);
        if (current === next) {
          return current;
        }
        if (current === null || next === null) {
          return current === next ? current : next;
        }
        return serializeSearchRequest(current) === serializeSearchRequest(next) ? current : next;
      });
    },
    [],
  );
  const [searchAttribute, setSearchAttribute] = useState<SearchAttribute>('target');
  const [searchType, setSearchType] = useState<SearchType>('contains');
  const [searchInputValue, setSearchInputValue] = useState('');
  const [statusFilter, setStatusFilter] = useState<StatusFilterValue>('ALL');
  const [includeUsed, setIncludeUsed] = useState(true);
  const [includeUnused, setIncludeUnused] = useState(false);
  const [includeTranslate, setIncludeTranslate] = useState(true);
  const [includeDoNotTranslate, setIncludeDoNotTranslate] = useState(true);
  const [createdBefore, setCreatedBefore] = useState<string | null>(null);
  const [createdAfter, setCreatedAfter] = useState<string | null>(null);
  const [translationCreatedBefore, setTranslationCreatedBefore] = useState<string | null>(null);
  const [translationCreatedAfter, setTranslationCreatedAfter] = useState<string | null>(null);
  const [worksetSize, setWorksetSize] = useState<number>(
    () => loadPreferredWorksetSize() ?? WORKSET_SIZE_DEFAULT,
  );
  const hydratedSearchSignatureRef = useRef<string | null>(null);
  const lastHydratedRequestRef = useRef<TextUnitSearchRequest | null>(null);
  const [hasHydratedSearch, setHasHydratedSearch] = useState(initialSearchRequest == null);

  const {
    data: repositories,
    isLoading: isRepositoriesLoading,
    isError: isRepositoriesError,
    error: repositoriesError,
  } = useRepositories();

  const repositoryOptions: RepositorySelectionOption[] =
    useRepositorySelectionOptions(repositories);

  const {
    selectedIds: selectedRepositoryIds,
    onChangeSelection: onChangeRepositorySelection,
    setSelection: setRepositorySelection,
  } = useRepositorySelection({
    options: repositoryOptions,
    initialSelected: initialSearchRequest?.repositoryIds ?? [],
    allowStaleSelections: true,
  });

  const allowedRepositoryIds = useMemo(
    () =>
      selectedRepositoryIds.length
        ? new Set(selectedRepositoryIds)
        : new Set(repositoryOptions.map((option) => option.id)),
    [repositoryOptions, selectedRepositoryIds],
  );

  const localeOptions: LocaleSelectionOption[] = useLocaleOptionsWithDisplayNames(
    repositories ?? [],
    allowedRepositoryIds,
  );

  const {
    selectedTags: selectedLocaleTags,
    onChangeSelection: onChangeLocaleSelection,
    setSelection: setLocaleSelection,
  } = useLocaleSelection({
    options: localeOptions,
    initialSelected: initialSearchRequest?.localeTags ?? [],
    allowStaleSelections: true,
  });

  const initialSearchSignature = useMemo(
    () => (initialSearchRequest ? serializeSearchRequest(initialSearchRequest) : null),
    [initialSearchRequest],
  );
  useEffect(() => {
    if (!initialSearchRequest || !initialSearchSignature) {
      return;
    }
    const alreadyHydrated =
      hydratedSearchSignatureRef.current === initialSearchSignature &&
      lastHydratedRequestRef.current === initialSearchRequest;
    if (alreadyHydrated) {
      return;
    }

    setRepositorySelection(initialSearchRequest.repositoryIds ?? [], { markTouched: false });
    const initialLocales = initialSearchRequest.localeTags ?? [];
    setLocaleSelection(initialLocales, { markTouched: false });
    setSearchAttribute(initialSearchRequest.searchAttribute ?? 'target');
    setSearchType(initialSearchRequest.searchType ?? 'contains');
    setSearchInputValue(initialSearchRequest.searchText ?? '');
    setStatusFilter((initialSearchRequest.statusFilter as StatusFilterValue | undefined) ?? 'ALL');

    const usedFilter = initialSearchRequest.usedFilter;
    const initialIncludeUsed = usedFilter === 'UNUSED' ? false : true;
    const initialIncludeUnused = usedFilter === 'USED' ? false : true;
    setIncludeUsed(initialIncludeUsed);
    setIncludeUnused(initialIncludeUnused);

    const dntFilter = initialSearchRequest.doNotTranslateFilter;
    const initialIncludeTranslate = typeof dntFilter === 'boolean' ? !dntFilter : true;
    const initialIncludeDoNotTranslate = typeof dntFilter === 'boolean' ? dntFilter : true;
    setIncludeTranslate(initialIncludeTranslate);
    setIncludeDoNotTranslate(initialIncludeDoNotTranslate);

    setCreatedBefore(initialSearchRequest.tmTextUnitCreatedBefore ?? null);
    setCreatedAfter(initialSearchRequest.tmTextUnitCreatedAfter ?? null);
    setTranslationCreatedBefore(initialSearchRequest.tmTextUnitVariantCreatedBefore ?? null);
    setTranslationCreatedAfter(initialSearchRequest.tmTextUnitVariantCreatedAfter ?? null);
    const initialLimit = clampWorksetSize(
      initialSearchRequest.limit ?? loadPreferredWorksetSize() ?? WORKSET_SIZE_DEFAULT,
    );
    setWorksetSize(initialLimit);

    if (initialLocales.length > 0) {
      const normalizedRequest = buildSearchRequestFromInputs({
        repositoryIds: initialSearchRequest.repositoryIds ?? [],
        localeTags: initialLocales,
        searchAttribute: initialSearchRequest.searchAttribute ?? 'target',
        searchType: initialSearchRequest.searchType ?? 'contains',
        searchText: initialSearchRequest.searchText ?? '',
        searchLimit: initialLimit,
        statusFilter: (initialSearchRequest.statusFilter as StatusFilterValue | undefined) ?? 'ALL',
        includeUsed: initialIncludeUsed,
        includeUnused: initialIncludeUnused,
        includeTranslate: initialIncludeTranslate,
        includeDoNotTranslate: initialIncludeDoNotTranslate,
        createdBefore: initialSearchRequest.tmTextUnitCreatedBefore ?? null,
        createdAfter: initialSearchRequest.tmTextUnitCreatedAfter ?? null,
        translationCreatedBefore: initialSearchRequest.tmTextUnitVariantCreatedBefore ?? null,
        translationCreatedAfter: initialSearchRequest.tmTextUnitVariantCreatedAfter ?? null,
      });
      if (normalizedRequest) {
        setActiveSearchRequestIfChanged(normalizedRequest);
        lastAppliedNonTextSignatureRef.current = serializeSearchRequest({
          ...normalizedRequest,
          searchText: '',
        });
      } else {
        setActiveSearchRequestIfChanged(null);
      }
    } else {
      setActiveSearchRequestIfChanged(null);
    }
    hydratedSearchSignatureRef.current = initialSearchSignature;
    lastHydratedRequestRef.current = initialSearchRequest;
    setHasHydratedSearch(true);
  }, [
    initialSearchRequest,
    initialSearchSignature,
    setActiveSearchRequestIfChanged,
    setLocaleSelection,
    setRepositorySelection,
  ]);

  useEffect(() => {
    if (hasHydratedSearch || localeOptions.length) {
      return;
    }
    setLocaleSelection([], { markTouched: false });
  }, [hasHydratedSearch, localeOptions.length, setLocaleSelection]);

  const canSearch = selectedRepositoryIds.length > 0 && selectedLocaleTags.length > 0;
  const debouncedSearchInput = useDebouncedValue(searchInputValue, 350);
  const trimmedSearchInput = searchInputValue.trim();
  const trimmedDebouncedSearchInput = debouncedSearchInput.trim();
  const searchLimit = clampWorksetSize(worksetSize);

  const buildPendingSearchRequest = useCallback(
    (searchText: string): TextUnitSearchRequest | null =>
      buildSearchRequestFromInputs({
        repositoryIds: selectedRepositoryIds,
        localeTags: selectedLocaleTags,
        searchAttribute,
        searchType,
        searchText,
        searchLimit,
        statusFilter,
        includeUsed,
        includeUnused,
        includeTranslate,
        includeDoNotTranslate,
        createdBefore,
        createdAfter,
        translationCreatedBefore,
        translationCreatedAfter,
      }),
    [
      createdAfter,
      createdBefore,
      translationCreatedAfter,
      translationCreatedBefore,
      includeDoNotTranslate,
      includeTranslate,
      includeUnused,
      includeUsed,
      searchAttribute,
      searchLimit,
      searchType,
      selectedLocaleTags,
      selectedRepositoryIds,
      statusFilter,
    ],
  );

  const pendingSearchRequest = useMemo(
    () => buildPendingSearchRequest(trimmedSearchInput),
    [buildPendingSearchRequest, trimmedSearchInput],
  );

  const pendingSearchRequestDebounced = useMemo(
    () => buildPendingSearchRequest(trimmedDebouncedSearchInput),
    [buildPendingSearchRequest, trimmedDebouncedSearchInput],
  );

  const nonTextDraftSignature = useMemo(() => {
    if (!pendingSearchRequest) {
      return null;
    }
    return serializeSearchRequest({ ...pendingSearchRequest, searchText: '' });
  }, [pendingSearchRequest]);

  const lastAppliedNonTextSignatureRef = useRef<string | null>(null);
  const lastSuccessfulSearchDataRef = useRef<InfiniteData<ApiTextUnit[], number> | undefined>(
    undefined,
  );

  const resetSearch = useCallback(() => {
    const preferredWorkset = loadPreferredWorksetSize() ?? WORKSET_SIZE_DEFAULT;
    const nextWorksetSize = clampWorksetSize(preferredWorkset);

    setRepositorySelection([], { markTouched: false });
    setLocaleSelection([], { markTouched: false });
    setSearchAttribute('target');
    setSearchType('contains');
    setSearchInputValue('');
    setStatusFilter('ALL');
    setIncludeUsed(true);
    setIncludeUnused(false);
    setIncludeTranslate(true);
    setIncludeDoNotTranslate(true);
    setCreatedBefore(null);
    setCreatedAfter(null);
    setTranslationCreatedBefore(null);
    setTranslationCreatedAfter(null);
    setWorksetSize(nextWorksetSize);
    setActiveSearchRequestIfChanged(null);
    lastAppliedNonTextSignatureRef.current = null;
    lastSuccessfulSearchDataRef.current = undefined;
  }, [setActiveSearchRequestIfChanged, setLocaleSelection, setRepositorySelection]);

  // Auto-apply non-text changes immediately.
  useEffect(() => {
    if (!pendingSearchRequest || !canSearch) {
      return;
    }
    if (nonTextDraftSignature === null) {
      return;
    }

    if (nonTextDraftSignature === lastAppliedNonTextSignatureRef.current) {
      return;
    }

    lastAppliedNonTextSignatureRef.current = nonTextDraftSignature;
    setActiveSearchRequestIfChanged(pendingSearchRequest);
  }, [canSearch, nonTextDraftSignature, pendingSearchRequest, setActiveSearchRequestIfChanged]);

  // Auto-apply text changes with debounce.
  useEffect(() => {
    if (!pendingSearchRequestDebounced || !canSearch) {
      return;
    }
    // Ignore stale debounced values during hydration/transitions.
    if (trimmedDebouncedSearchInput !== trimmedSearchInput) {
      return;
    }
    setActiveSearchRequestIfChanged(pendingSearchRequestDebounced);
  }, [
    canSearch,
    pendingSearchRequestDebounced,
    setActiveSearchRequestIfChanged,
    trimmedDebouncedSearchInput,
    trimmedSearchInput,
  ]);

  // Reset search state if the scope is cleared.
  useEffect(() => {
    if (canSearch) {
      return;
    }
    lastAppliedNonTextSignatureRef.current = null;
    lastSuccessfulSearchDataRef.current = undefined;
    updateActiveSearchRequest((current) => (current === null ? current : null));
  }, [canSearch, updateActiveSearchRequest]);

  // Treat result size as a view setting (limit); changing it resets pagination to the first page.
  useEffect(() => {
    if (!activeSearchRequest) {
      return;
    }
    const appliedLimit = activeSearchRequest.limit ?? WORKSET_SIZE_DEFAULT;
    if (appliedLimit === searchLimit) {
      return;
    }
    setActiveSearchRequestIfChanged({ ...activeSearchRequest, limit: searchLimit, offset: 0 });
  }, [activeSearchRequest, searchLimit, setActiveSearchRequestIfChanged]);

  const searchQuery = useInfiniteQuery<
    ApiTextUnit[],
    Error,
    InfiniteData<ApiTextUnit[], number>,
    SearchQueryKey,
    number
  >({
    queryKey: ['workbench-search', activeSearchRequest],
    placeholderData: () => (activeSearchRequest ? lastSuccessfulSearchDataRef.current : undefined),
    queryFn: ({ pageParam }) => {
      if (!activeSearchRequest) {
        return Promise.resolve([]);
      }
      const limit = clampWorksetSize(activeSearchRequest.limit ?? WORKSET_SIZE_DEFAULT);
      return searchTextUnits({ ...activeSearchRequest, limit: limit + 1, offset: pageParam });
    },
    enabled: Boolean(activeSearchRequest),
    initialPageParam: 0,
    // Workbench uses a snapshot workset. We don't do offset pagination client-side because it
    // becomes unstable under concurrent edits.
    getNextPageParam: () => undefined,
    // Keep things predictable while the workset concept is landing.
    staleTime: Infinity,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });

  useEffect(() => {
    // Cache the latest successful snapshot so we can keep showing it as placeholder data while
    // a new search is loading (prevents N -> 0 -> M flicker).
    if (searchQuery.data && !searchQuery.isFetching) {
      lastSuccessfulSearchDataRef.current = searchQuery.data;
    }
  }, [searchQuery.data, searchQuery.isFetching]);

  const rows = useMemo(() => {
    const limit = clampWorksetSize(activeSearchRequest?.limit ?? searchLimit);
    const flatResults = searchQuery.data?.pages.flat() ?? [];
    const visibleResults = flatResults.slice(0, limit);
    return visibleResults.map((item) => mapApiTextUnitToRow(item, canEditLocale));
  }, [activeSearchRequest?.limit, canEditLocale, searchLimit, searchQuery.data]);

  const hasMoreResults = useMemo(() => {
    if (!activeSearchRequest) {
      return false;
    }
    const limit = clampWorksetSize(activeSearchRequest.limit ?? searchLimit);
    const flatResults = searchQuery.data?.pages.flat() ?? [];
    return flatResults.length > limit;
  }, [activeSearchRequest, searchLimit, searchQuery.data]);

  const onSubmitSearch = useCallback(() => {
    // Treat Enter as "search now" (flush debounce).
    if (!canSearch || !pendingSearchRequest) {
      return;
    }
    setActiveSearchRequestIfChanged(pendingSearchRequest);
  }, [canSearch, pendingSearchRequest, setActiveSearchRequestIfChanged]);

  const onChangeSearchInput = useCallback((value: string) => {
    setSearchInputValue(value);
  }, []);

  const onChangeSearchAttribute = useCallback((attribute: SearchAttribute) => {
    setSearchAttribute(attribute);
  }, []);

  const onChangeSearchType = useCallback((type: SearchType) => {
    setSearchType(type);
  }, []);

  const onChangeWorksetSize = useCallback(
    (value: number) => {
      const next = clampWorksetSize(value);
      if (next === worksetSize) {
        return;
      }
      setWorksetSize(next);
      if (activeSearchRequest) {
        updateActiveSearchRequest((previous) =>
          previous ? { ...previous, limit: next, offset: previous.offset ?? 0 } : previous,
        );
      }
    },
    [activeSearchRequest, updateActiveSearchRequest, worksetSize],
  );

  const repositoryErrorMessage = isRepositoriesError
    ? repositoriesError instanceof Error
      ? repositoriesError.message
      : 'Failed to load repositories.'
    : null;

  const searchErrorMessage =
    searchQuery.isError && activeSearchRequest ? searchQuery.error.message : null;

  const isSearchLoading = Boolean(activeSearchRequest) && searchQuery.isFetching;

  return {
    repositoryOptions,
    repositories: repositories ?? [],
    localeOptions,
    isRepositoriesLoading,
    repositoryErrorMessage,
    selectedRepositoryIds,
    selectedLocaleTags,
    onChangeRepositorySelection,
    onChangeLocaleSelection,
    searchAttribute,
    searchType,
    searchInputValue,
    onChangeSearchInput,
    onSubmitSearch,
    onChangeSearchAttribute,
    onChangeSearchType,
    statusFilter,
    includeUsed,
    includeUnused,
    includeTranslate,
    includeDoNotTranslate,
    onChangeStatusFilter: setStatusFilter,
    onChangeIncludeUsed: setIncludeUsed,
    onChangeIncludeUnused: setIncludeUnused,
    onChangeIncludeTranslate: setIncludeTranslate,
    onChangeIncludeDoNotTranslate: setIncludeDoNotTranslate,
    createdBefore,
    createdAfter,
    translationCreatedBefore,
    translationCreatedAfter,
    onChangeCreatedBefore: setCreatedBefore,
    onChangeCreatedAfter: setCreatedAfter,
    onChangeTranslationCreatedBefore: setTranslationCreatedBefore,
    onChangeTranslationCreatedAfter: setTranslationCreatedAfter,
    worksetSize,
    onChangeWorksetSize,
    canSearch,
    activeSearchRequest,
    isSearchLoading,
    searchErrorMessage,
    rows,
    hasMoreResults,
    refetchSearch: () => searchQuery.refetch(),
    resetSearch,
    hasHydratedSearch,
  };
}
