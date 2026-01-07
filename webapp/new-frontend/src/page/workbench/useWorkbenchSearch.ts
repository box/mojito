import type { InfiniteData } from '@tanstack/react-query';
import { useInfiniteQuery } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import {
  type ApiTextUnit,
  type SearchAttribute,
  searchTextUnits,
  type SearchType,
  type TextUnitSearchRequest,
} from '../../api/text-units';
import { useDebouncedValue } from '../../hooks/useDebouncedValue';
import { useRepositories } from '../../hooks/useRepositories';
import { useLocaleDisplayNameResolver } from '../../utils/localeDisplayNames';
import { WORKSET_SIZE_DEFAULT } from './workbench-constants';
import { clampWorksetSize, mapApiTextUnitToRow, serializeSearchRequest } from './workbench-helpers';
import type {
  LocaleOption,
  RepositoryOption,
  StatusFilterValue,
  WorkbenchRow,
} from './workbench-types';

type SearchQueryKey = ['workbench-search', TextUnitSearchRequest | null];

type Params = {
  isEditMode: boolean;
  initialSearchRequest?: TextUnitSearchRequest | null;
};

type SearchState = {
  repositoryOptions: RepositoryOption[];
  localeOptions: LocaleOption[];
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
  onChangeCreatedBefore: (value: string | null) => void;
  onChangeCreatedAfter: (value: string | null) => void;
  worksetSize: number;
  onChangeWorksetSize: (value: number) => void;
  canSearch: boolean;
  appliedSearchRequest: TextUnitSearchRequest | null;
  isSearchLoading: boolean;
  searchErrorMessage: string | null;
  rows: WorkbenchRow[];
  refetchSearch: () => Promise<unknown>;
};

export function useWorkbenchSearch({ isEditMode, initialSearchRequest }: Params): SearchState {
  const [appliedSearchRequest, setAppliedSearchRequest] = useState<TextUnitSearchRequest | null>(
    null,
  );
  const [searchAttribute, setSearchAttribute] = useState<SearchAttribute>('target');
  const [searchType, setSearchType] = useState<SearchType>('contains');
  const [searchInputValue, setSearchInputValue] = useState('');
  const [selectedRepositoryIds, setSelectedRepositoryIds] = useState<number[]>([]);
  const [selectedLocaleTags, setSelectedLocaleTags] = useState<string[]>([]);
  const [statusFilter, setStatusFilter] = useState<StatusFilterValue>('ALL');
  const [includeUsed, setIncludeUsed] = useState(true);
  const [includeUnused, setIncludeUnused] = useState(false);
  const [includeTranslate, setIncludeTranslate] = useState(true);
  const [includeDoNotTranslate, setIncludeDoNotTranslate] = useState(true);
  const [createdBefore, setCreatedBefore] = useState<string | null>(null);
  const [createdAfter, setCreatedAfter] = useState<string | null>(null);
  const [worksetSize, setWorksetSize] = useState<number>(WORKSET_SIZE_DEFAULT);
  const hydratedSignatureRef = useRef<string | null>(null);
  const [hasHydratedSearch, setHasHydratedSearch] = useState(false);
  const [shouldRestoreReposFromHydration, setShouldRestoreReposFromHydration] = useState(false);
  const [shouldRestoreLocalesFromHydration, setShouldRestoreLocalesFromHydration] = useState(false);

  const {
    data: repositories,
    isLoading: isRepositoriesLoading,
    isError: isRepositoriesError,
    error: repositoriesError,
  } = useRepositories();
  const resolveLocaleName = useLocaleDisplayNameResolver();

  const initialSearchSignature = useMemo(
    () => (initialSearchRequest ? serializeSearchRequest(initialSearchRequest) : null),
    [initialSearchRequest],
  );

  useEffect(() => {
    if (!initialSearchRequest || !initialSearchSignature) {
      return;
    }
    if (hydratedSignatureRef.current === initialSearchSignature) {
      return;
    }

    setSelectedRepositoryIds(initialSearchRequest.repositoryIds ?? []);
    setSelectedLocaleTags(initialSearchRequest.localeTags ?? []);
    setSearchAttribute(initialSearchRequest.searchAttribute ?? 'target');
    setSearchType(initialSearchRequest.searchType ?? 'contains');
    setSearchInputValue(initialSearchRequest.searchText ?? '');
    setStatusFilter((initialSearchRequest.statusFilter as StatusFilterValue | undefined) ?? 'ALL');

    const usedFilter = initialSearchRequest.usedFilter;
    setIncludeUsed(usedFilter !== 'UNUSED');
    setIncludeUnused(usedFilter === 'UNUSED');

    const dntFilter = initialSearchRequest.doNotTranslateFilter;
    if (typeof dntFilter === 'boolean') {
      setIncludeTranslate(!dntFilter);
      setIncludeDoNotTranslate(dntFilter);
    } else {
      setIncludeTranslate(true);
      setIncludeDoNotTranslate(true);
    }

    setCreatedBefore(initialSearchRequest.tmTextUnitCreatedBefore ?? null);
    setCreatedAfter(initialSearchRequest.tmTextUnitCreatedAfter ?? null);
    setWorksetSize(clampWorksetSize(initialSearchRequest.limit ?? WORKSET_SIZE_DEFAULT));

    if (initialSearchRequest.localeTags && initialSearchRequest.localeTags.length > 0) {
      setAppliedSearchRequest(initialSearchRequest);
    } else {
      setAppliedSearchRequest(null);
    }
    hydratedSignatureRef.current = initialSearchSignature;
    setHasHydratedSearch(true);
    setShouldRestoreReposFromHydration(true);
    setShouldRestoreLocalesFromHydration(true);
  }, [initialSearchRequest, initialSearchSignature]);

  const repositoryOptions: RepositoryOption[] = useMemo(
    () => (repositories ?? []).map((repo) => ({ id: repo.id, name: repo.name })),
    [repositories],
  );

  const localeOptions: LocaleOption[] = useMemo(() => {
    const allowedRepositoryIds = selectedRepositoryIds.length
      ? new Set(selectedRepositoryIds)
      : new Set(repositoryOptions.map((option) => option.id));

    const localeSet = new Set<string>();
    (repositories ?? []).forEach((repo) => {
      if (!allowedRepositoryIds.has(repo.id)) {
        return;
      }
      (repo.repositoryLocales ?? []).forEach((repoLocale) => {
        const tag = repoLocale.locale?.bcp47Tag;
        if (tag) {
          localeSet.add(tag);
        }
      });
    });

    if (!localeSet.size) {
      return [];
    }

    return Array.from(localeSet)
      .sort((first, second) => first.localeCompare(second, undefined, { sensitivity: 'base' }))
      .map((tag) => ({ tag, label: resolveLocaleName(tag) }));
  }, [repositories, resolveLocaleName, selectedRepositoryIds, repositoryOptions]);

  const onChangeRepositorySelection = useCallback(
    (nextSelected: number[]) => {
      setShouldRestoreReposFromHydration(false);
      const allowedIds = new Set(repositoryOptions.map((option) => option.id));
      const uniqueNext = nextSelected.filter(
        (value, index, array) => array.indexOf(value) === index,
      );
      const filtered = uniqueNext.filter((value) => allowedIds.has(value));
      setSelectedRepositoryIds(filtered);
    },
    [repositoryOptions],
  );

  const onChangeLocaleSelection = useCallback(
    (nextSelected: string[]) => {
      setShouldRestoreLocalesFromHydration(false);
      const allowedTags = new Set(localeOptions.map((option) => option.tag));
      const uniqueNext = nextSelected.filter(
        (value, index, array) => array.indexOf(value) === index,
      );
      const filtered = uniqueNext.filter((value) => allowedTags.has(value));
      setSelectedLocaleTags(filtered);
    },
    [localeOptions],
  );

  useEffect(() => {
    if (!repositoryOptions.length) {
      if (hasHydratedSearch) {
        return;
      }
      setSelectedRepositoryIds((current) => (current.length ? [] : current));
      return;
    }

    if (shouldRestoreReposFromHydration) {
      const allowedIds = new Set(repositoryOptions.map((option) => option.id));
      if (selectedRepositoryIds.length === 0) {
        const fromHydration = (initialSearchRequest?.repositoryIds ?? []).filter((id) => {
          return allowedIds.has(id);
        });
        if (fromHydration.length) {
          setSelectedRepositoryIds(fromHydration);
          setShouldRestoreReposFromHydration(false);
          return;
        }
      }
      setShouldRestoreReposFromHydration(false);
    }

    setSelectedRepositoryIds((current) => {
      const allowedIds = new Set(repositoryOptions.map((option) => option.id));
      const filtered = current.filter((id) => allowedIds.has(id));
      const same =
        filtered.length === current.length &&
        filtered.every((value, index) => value === current[index]);
      return same ? current : filtered;
    });
  }, [
    hasHydratedSearch,
    initialSearchRequest?.repositoryIds,
    repositoryOptions,
    selectedRepositoryIds.length,
    shouldRestoreReposFromHydration,
  ]);

  useEffect(() => {
    if (!localeOptions.length) {
      if (hasHydratedSearch) {
        return;
      }
      setSelectedLocaleTags((current) => (current.length ? [] : current));
      return;
    }

    if (shouldRestoreLocalesFromHydration) {
      const allowedTags = new Set(localeOptions.map((option) => option.tag));
      if (selectedLocaleTags.length === 0) {
        const fromHydration = (initialSearchRequest?.localeTags ?? []).filter((tag) => {
          return allowedTags.has(tag);
        });
        if (fromHydration.length) {
          setSelectedLocaleTags(fromHydration);
          setShouldRestoreLocalesFromHydration(false);
          return;
        }
      }
      setShouldRestoreLocalesFromHydration(false);
    }

    setSelectedLocaleTags((current) => {
      const allowedTags = new Set(localeOptions.map((option) => option.tag));
      const filtered = current.filter((tag) => allowedTags.has(tag));
      const same =
        filtered.length === current.length &&
        filtered.every((value, index) => value === current[index]);
      return same ? current : filtered;
    });
  }, [
    hasHydratedSearch,
    initialSearchRequest?.localeTags,
    localeOptions,
    selectedLocaleTags.length,
    shouldRestoreLocalesFromHydration,
  ]);

  const canSearch = selectedRepositoryIds.length > 0 && selectedLocaleTags.length > 0;
  const debouncedSearchInput = useDebouncedValue(searchInputValue, 350);
  const searchLimit = clampWorksetSize(worksetSize);

  const buildSearchRequest = useCallback(
    (searchText: string): TextUnitSearchRequest | null => {
      if (!selectedRepositoryIds.length || !selectedLocaleTags.length) {
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
        repositoryIds: selectedRepositoryIds,
        localeTags: selectedLocaleTags,
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
      };
    },
    [
      createdAfter,
      createdBefore,
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

  const draftSearchRequest = useMemo(
    () => buildSearchRequest(searchInputValue.trim()),
    [buildSearchRequest, searchInputValue],
  );

  const autoSearchRequest = useMemo(
    () => buildSearchRequest(debouncedSearchInput.trim()),
    [buildSearchRequest, debouncedSearchInput],
  );

  const nonTextDraftSignature = useMemo(() => {
    if (!draftSearchRequest) {
      return null;
    }
    return serializeSearchRequest({ ...draftSearchRequest, searchText: '' });
  }, [draftSearchRequest]);

  const lastAppliedNonTextSignatureRef = useRef<string | null>(null);
  const lastSuccessfulSearchDataRef = useRef<InfiniteData<ApiTextUnit[], number> | undefined>(
    undefined,
  );

  // Auto-apply non-text changes immediately when not locked.
  useEffect(() => {
    if (isEditMode || !draftSearchRequest || !canSearch) {
      return;
    }
    if (nonTextDraftSignature === null) {
      return;
    }

    if (nonTextDraftSignature === lastAppliedNonTextSignatureRef.current) {
      return;
    }

    lastAppliedNonTextSignatureRef.current = nonTextDraftSignature;
    setAppliedSearchRequest(draftSearchRequest);
  }, [canSearch, draftSearchRequest, isEditMode, nonTextDraftSignature]);

  // Auto-apply text changes with debounce when not locked.
  useEffect(() => {
    if (isEditMode || !autoSearchRequest || !canSearch) {
      return;
    }
    setAppliedSearchRequest(autoSearchRequest);
  }, [autoSearchRequest, canSearch, isEditMode]);

  // Reset search state if the scope is cleared.
  useEffect(() => {
    if (canSearch) {
      return;
    }
    lastAppliedNonTextSignatureRef.current = null;
    lastSuccessfulSearchDataRef.current = undefined;
    setAppliedSearchRequest((current) => (current === null ? current : null));
  }, [canSearch]);

  // Treat result size as a view setting (limit), not part of the locked search scope.
  // Changing it resets pagination to the first page.
  useEffect(() => {
    if (!appliedSearchRequest || isEditMode) {
      return;
    }
    const appliedLimit = appliedSearchRequest.limit ?? WORKSET_SIZE_DEFAULT;
    if (appliedLimit === searchLimit) {
      return;
    }
    setAppliedSearchRequest({ ...appliedSearchRequest, limit: searchLimit, offset: 0 });
  }, [appliedSearchRequest, isEditMode, searchLimit]);

  const searchQuery = useInfiniteQuery<
    ApiTextUnit[],
    Error,
    InfiniteData<ApiTextUnit[], number>,
    SearchQueryKey,
    number
  >({
    queryKey: ['workbench-search', appliedSearchRequest],
    placeholderData: () => (appliedSearchRequest ? lastSuccessfulSearchDataRef.current : undefined),
    queryFn: ({ pageParam }) => {
      if (!appliedSearchRequest) {
        return Promise.resolve([]);
      }
      return searchTextUnits({ ...appliedSearchRequest, offset: pageParam });
    },
    enabled: Boolean(appliedSearchRequest),
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

  const rows = useMemo(
    () => (searchQuery.data?.pages.flat() ?? []).map(mapApiTextUnitToRow),
    [searchQuery.data],
  );

  const onSubmitSearch = useCallback(() => {
    // Treat Enter as "search now" (flush debounce).
    if (!canSearch || !draftSearchRequest || isEditMode) {
      return;
    }
    setAppliedSearchRequest(draftSearchRequest);
  }, [canSearch, draftSearchRequest, isEditMode]);

  const onChangeSearchInput = useCallback((value: string) => {
    setSearchInputValue(value);
  }, []);

  const onChangeSearchAttribute = useCallback((attribute: SearchAttribute) => {
    setSearchAttribute(attribute);
  }, []);

  const onChangeSearchType = useCallback((type: SearchType) => {
    setSearchType(type);
  }, []);

  const onChangeWorksetSize = useCallback((value: number) => {
    setWorksetSize(clampWorksetSize(value));
  }, []);

  const repositoryErrorMessage = isRepositoriesError
    ? repositoriesError instanceof Error
      ? repositoriesError.message
      : 'Failed to load repositories.'
    : null;

  const searchErrorMessage =
    searchQuery.isError && appliedSearchRequest ? searchQuery.error.message : null;

  const isSearchLoading = Boolean(appliedSearchRequest) && searchQuery.isFetching;

  return {
    repositoryOptions,
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
    onChangeCreatedBefore: setCreatedBefore,
    onChangeCreatedAfter: setCreatedAfter,
    worksetSize,
    onChangeWorksetSize,
    canSearch,
    appliedSearchRequest,
    isSearchLoading,
    searchErrorMessage,
    rows,
    refetchSearch: () => searchQuery.refetch(),
  };
}
