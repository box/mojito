import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import type {
  ApiLocale,
  ApiRepository,
  ApiRepositoryLocaleStatistic,
} from '../../api/repositories';
import type { TextUnitSearchRequest } from '../../api/text-units';
import { useUser } from '../../components/RequireUser';
import { useRepositories } from '../../hooks/useRepositories';
import type { LocaleSelectionOption as LocaleOption } from '../../utils/localeSelection';
import {
  filterMyLocales,
  isLocaleTagAllowed,
  useLocaleOptionsWithDisplayNames,
  useLocaleSelection,
} from '../../utils/localeSelection';
import { getNonRootRepositoryLocaleTags } from '../../utils/repositoryLocales';
import {
  useRepositorySelection,
  useRepositorySelectionOptions,
} from '../../utils/repositorySelection';
import { WORKSET_SIZE_DEFAULT } from '../workbench/workbench-constants';
import { clampWorksetSize } from '../workbench/workbench-helpers';
import {
  loadPreferredLocales,
  loadPreferredWorksetSize,
  PREFERRED_LOCALES_KEY,
} from '../workbench/workbench-preferences';
import {
  loadRepositoriesSessionState,
  REPOSITORIES_SESSION_QUERY_KEY,
  type RepositoriesSessionState,
  saveRepositoriesSessionState,
  serializeRepositoriesSessionState,
} from './repositories-session-state';
import type { LocaleRow, RepositoryRow, RepositoryStatusFilter } from './RepositoriesPageView';
import { RepositoriesPageView } from './RepositoriesPageView';

const getLocaleTag = (locale: ApiLocale | null) => locale?.bcp47Tag ?? '';

const getRejectedCount = (localeStat: ApiRepositoryLocaleStatistic) => {
  const translated = localeStat.translatedCount ?? 0;
  const includeInFile = localeStat.includeInFileCount ?? 0;
  const rejected = translated - includeInFile;
  return rejected > 0 ? rejected : 0;
};

const getFullyTranslatedLocaleTags = (repository: ApiRepository) => {
  const locales = repository.repositoryLocales ?? [];
  const tags = locales
    .filter((repoLocale) => repoLocale.toBeFullyTranslated)
    .map((repoLocale) => getLocaleTag(repoLocale.locale))
    .filter((tag): tag is string => Boolean(tag));

  return new Set(tags);
};

const buildRepositoryRow = (
  repository: ApiRepository,
  selectedRepositoryId: number | null,
  allowedLocaleTags?: Set<string>,
): RepositoryRow => {
  const summary = getRepositorySummary(repository, allowedLocaleTags);

  return {
    id: repository.id,
    name: repository.name,
    rejected: summary.rejected,
    needsTranslation: summary.needsTranslation,
    needsReview: summary.needsReview,
    selected: repository.id === selectedRepositoryId,
  };
};

const buildRepositoriesWithSelection = (
  repositories: ApiRepository[],
  selectedRepositoryId: number | null,
  allowedLocaleTags?: Set<string>,
): RepositoryRow[] =>
  repositories.map((repository) =>
    buildRepositoryRow(repository, selectedRepositoryId, allowedLocaleTags),
  );

const getRepositorySummary = (repository: ApiRepository, allowedLocaleTags?: Set<string>) => {
  const fullyTranslatedTags = getFullyTranslatedLocaleTags(repository);
  const localeStats = repository.repositoryStatistic?.repositoryLocaleStatistics ?? [];

  let rejected = 0;
  let needsTranslation = 0;
  let needsReview = 0;

  localeStats.forEach((localeStat) => {
    const localeTag = getLocaleTag(localeStat.locale);
    if (!isLocaleTagAllowed(localeTag, allowedLocaleTags)) {
      return;
    }
    rejected += getRejectedCount(localeStat);
    needsReview += localeStat.reviewNeededCount ?? 0;

    if (localeTag && fullyTranslatedTags.has(localeTag)) {
      needsTranslation += localeStat.forTranslationCount ?? 0;
    }
  });

  return { rejected, needsTranslation, needsReview };
};

const buildLocaleRows = (
  repository: ApiRepository,
  resolveLocaleName: (tag: string) => string,
  allowedLocaleTags?: Set<string>,
): LocaleRow[] => {
  const localeStats = repository.repositoryStatistic?.repositoryLocaleStatistics ?? [];
  const sourceLocaleTag = repository.sourceLocale?.bcp47Tag;

  return localeStats
    .map((localeStat, index) => {
      const localeTag = getLocaleTag(localeStat.locale);
      if (!localeTag || localeTag === sourceLocaleTag) {
        return null;
      }
      if (!isLocaleTagAllowed(localeTag, allowedLocaleTags)) {
        return null;
      }

      return {
        id: localeTag || `locale-${index}`,
        name: resolveLocaleName(localeTag),
        rejected: getRejectedCount(localeStat),
        needsTranslation: localeStat.forTranslationCount ?? 0,
        needsReview: localeStat.reviewNeededCount ?? 0,
      };
    })
    .filter((localeRow): localeRow is LocaleRow => Boolean(localeRow))
    .sort((first, second) =>
      first.name.localeCompare(second.name, undefined, { sensitivity: 'base' }),
    );
};

const buildLocalesForRepository = (
  repository: ApiRepository | null,
  resolveLocaleName: (tag: string) => string,
  allowedLocaleTags?: Set<string>,
): LocaleRow[] => {
  if (!repository) {
    return [];
  }

  return buildLocaleRows(repository, resolveLocaleName, allowedLocaleTags);
};

export function RepositoriesPage() {
  const user = useUser();
  const [searchParams, setSearchParams] = useSearchParams();
  const rsId = searchParams.get(REPOSITORIES_SESSION_QUERY_KEY);
  const [selectedRepositoryId, setSelectedRepositoryId] = useState<number | null>(null);
  const [statusFilter, setStatusFilter] = useState<RepositoryStatusFilter>('all');
  const [preferredLocales, setPreferredLocales] = useState<string[]>(() => loadPreferredLocales());
  const [hydratedSession, setHydratedSession] = useState<{
    key: string;
    state: RepositoriesSessionState;
  } | null>(() => {
    if (!rsId) {
      return null;
    }
    const initialState = loadRepositoriesSessionState(rsId);
    return initialState ? { key: rsId, state: initialState } : null;
  });
  const [hydrationPhase, setHydrationPhase] = useState<'idle' | 'pending' | 'applied'>(
    rsId ? 'pending' : 'idle',
  );
  const navigate = useNavigate();
  const persistedRsIdRef = useRef<string | null>(rsId);
  const lastPersistedStateSignatureRef = useRef<string | null>(null);

  const { data: repositoryData, isLoading, isError, error, refetch } = useRepositories();
  const handleRetryFetch = useCallback(() => {
    void refetch();
  }, [refetch]);

  const status: 'loading' | 'error' | 'ready' = isLoading ? 'loading' : isError ? 'error' : 'ready';
  const errorMessage = isError
    ? error instanceof Error
      ? error.message
      : 'Failed to load repositories.'
    : undefined;

  const repositories = useMemo(() => repositoryData ?? [], [repositoryData]);
  const repositoryOptions = useRepositorySelectionOptions(repositories);

  const {
    selectedIds: selectedRepositoryIds,
    onChangeSelection: onChangeRepositorySelection,
    hasTouched: hasTouchedRepositorySelection,
    setSelection: setRepositorySelection,
  } = useRepositorySelection({
    options: repositoryOptions,
    allowStaleSelections: true,
  });

  useEffect(() => {
    if (hasTouchedRepositorySelection || selectedRepositoryIds.length > 0) {
      return;
    }
    if (!repositoryOptions.length) {
      return;
    }
    setRepositorySelection(
      repositoryOptions.map((option) => option.id),
      { markTouched: false },
    );
  }, [
    hasTouchedRepositorySelection,
    repositoryOptions,
    selectedRepositoryIds.length,
    setRepositorySelection,
  ]);

  const allowedRepositoryIdSet = useMemo(() => {
    if (selectedRepositoryIds.length === 0) {
      return hasTouchedRepositorySelection ? new Set<number>() : undefined;
    }
    return new Set(selectedRepositoryIds);
  }, [hasTouchedRepositorySelection, selectedRepositoryIds]);

  const localeOptions: LocaleOption[] = useLocaleOptionsWithDisplayNames(
    repositories,
    allowedRepositoryIdSet,
  );

  const {
    selectedTags: selectedLocaleTags,
    onChangeSelection: onChangeLocaleSelection,
    hasTouched: hasTouchedLocaleSelection,
    setSelection: setLocaleSelection,
    allowedTagSet: allowedLocaleTagSet,
  } = useLocaleSelection({
    options: localeOptions,
    autoSelectAll: true,
    allowStaleSelections: true,
  });

  useEffect(() => {
    persistedRsIdRef.current = rsId;
  }, [rsId]);

  useEffect(() => {
    if (!rsId) {
      setHydratedSession(null);
      setHydrationPhase('idle');
      return;
    }
    setHydrationPhase('pending');
    const nextHydratedState = loadRepositoriesSessionState(rsId);
    setHydratedSession(nextHydratedState ? { key: rsId, state: nextHydratedState } : null);
    if (!nextHydratedState) {
      setHydrationPhase('idle');
    }
  }, [rsId]);

  useEffect(() => {
    if (!rsId || hydrationPhase !== 'pending') {
      return;
    }
    if (!hydratedSession || hydratedSession.key !== rsId) {
      setHydrationPhase('idle');
      return;
    }
    setSelectedRepositoryId(hydratedSession.state.selectedRepositoryId);
    setStatusFilter(hydratedSession.state.statusFilter);
    setRepositorySelection(hydratedSession.state.selectedRepositoryIds, {
      touched: hydratedSession.state.repositorySelectionTouched,
    });
    setLocaleSelection(hydratedSession.state.selectedLocaleTags, {
      touched: hydratedSession.state.localeSelectionTouched,
    });
    setHydrationPhase('applied');
  }, [hydratedSession, hydrationPhase, rsId, setLocaleSelection, setRepositorySelection]);

  useEffect(() => {
    if (hydrationPhase !== 'applied') {
      return;
    }
    setHydrationPhase('idle');
  }, [hydrationPhase]);

  const repositoriesSessionState = useMemo(
    () => ({
      selectedRepositoryId,
      selectedRepositoryIds,
      repositorySelectionTouched: hasTouchedRepositorySelection,
      selectedLocaleTags,
      localeSelectionTouched: hasTouchedLocaleSelection,
      statusFilter,
    }),
    [
      hasTouchedLocaleSelection,
      hasTouchedRepositorySelection,
      selectedLocaleTags,
      selectedRepositoryId,
      selectedRepositoryIds,
      statusFilter,
    ],
  );

  useEffect(() => {
    if (hydrationPhase !== 'idle') {
      return;
    }
    const currentRsId = searchParams.get(REPOSITORIES_SESSION_QUERY_KEY);

    const signature = serializeRepositoriesSessionState(repositoriesSessionState);
    const nextRsId = saveRepositoriesSessionState(
      repositoriesSessionState,
      persistedRsIdRef.current ?? currentRsId,
    );
    persistedRsIdRef.current = nextRsId;

    if (lastPersistedStateSignatureRef.current === signature && currentRsId === nextRsId) {
      return;
    }
    lastPersistedStateSignatureRef.current = signature;
    if (currentRsId === nextRsId) {
      return;
    }

    const nextParams = new URLSearchParams(searchParams);
    nextParams.set(REPOSITORIES_SESSION_QUERY_KEY, nextRsId);
    setSearchParams(nextParams, { replace: true });
  }, [hydrationPhase, repositoriesSessionState, searchParams, setSearchParams]);

  useEffect(() => {
    const handleStorage = (event: StorageEvent) => {
      if (event.key && event.key !== PREFERRED_LOCALES_KEY) {
        return;
      }
      setPreferredLocales(loadPreferredLocales());
    };
    window.addEventListener('storage', handleStorage);
    return () => window.removeEventListener('storage', handleStorage);
  }, []);

  const myLocaleSelections = useMemo(() => {
    const userLocales = user.userLocales ?? [];
    const isLimitedTranslator = !user.canTranslateAllLocales && userLocales.length > 0;
    return filterMyLocales({
      availableLocaleTags: localeOptions.map((option) => option.tag),
      userLocales,
      preferredLocales,
      isLimitedTranslator,
      isAdmin: user.role === 'ROLE_ADMIN',
    });
  }, [localeOptions, preferredLocales, user.canTranslateAllLocales, user.role, user.userLocales]);

  const filteredRepositories = useMemo(() => {
    return repositories.filter((repository) => {
      if (allowedRepositoryIdSet && !allowedRepositoryIdSet.has(repository.id)) {
        return false;
      }

      const summary = getRepositorySummary(repository, allowedLocaleTagSet);

      // When locales are explicitly deselected, hide repositories with no matching locale data.
      if (allowedLocaleTagSet && allowedLocaleTagSet.size === 0) {
        return false;
      }

      if (statusFilter === 'rejected') {
        return summary.rejected > 0;
      }
      if (statusFilter === 'needs-translation') {
        return summary.needsTranslation > 0;
      }
      if (statusFilter === 'needs-review') {
        return summary.needsReview > 0;
      }

      return true;
    });
  }, [allowedLocaleTagSet, allowedRepositoryIdSet, repositories, statusFilter]);

  const repositoriesWithSelection = useMemo(
    () =>
      buildRepositoriesWithSelection(
        filteredRepositories,
        selectedRepositoryId,
        allowedLocaleTagSet,
      ),
    [allowedLocaleTagSet, filteredRepositories, selectedRepositoryId],
  );

  const selectedRepository = useMemo(
    () => repositories.find((repository) => repository.id === selectedRepositoryId) ?? null,
    [repositories, selectedRepositoryId],
  );

  useEffect(() => {
    if (selectedRepositoryId == null) {
      return;
    }
    if (status !== 'ready') {
      return;
    }
    if (hydrationPhase !== 'idle') {
      return;
    }

    const stillVisible = filteredRepositories.some((repo) => repo.id === selectedRepositoryId);
    if (!stillVisible) {
      setSelectedRepositoryId(null);
    }
  }, [filteredRepositories, hydrationPhase, selectedRepositoryId, status]);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.defaultPrevented) {
        return;
      }

      if (event.key === 'Escape') {
        setSelectedRepositoryId(null);
        return;
      }

      if (event.key !== 'ArrowDown' && event.key !== 'ArrowUp') {
        return;
      }

      const target = event.target as HTMLElement | null;
      if (target) {
        const tag = target.tagName;
        if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || target.isContentEditable) {
          return;
        }
      }

      if (!repositoriesWithSelection.length) {
        return;
      }

      event.preventDefault();

      const currentIndex = repositoriesWithSelection.findIndex(
        (repo) => repo.id === selectedRepositoryId,
      );

      if (event.key === 'ArrowDown') {
        const nextIndex =
          currentIndex >= 0 ? Math.min(currentIndex + 1, repositoriesWithSelection.length - 1) : 0;
        const nextRepo = repositoriesWithSelection[nextIndex];
        if (nextRepo && nextRepo.id !== selectedRepositoryId) {
          setSelectedRepositoryId(nextRepo.id);
        }
      } else {
        const previousIndex = currentIndex > 0 ? currentIndex - 1 : 0;
        const previousRepo = repositoriesWithSelection[previousIndex];
        if (previousRepo && previousRepo.id !== selectedRepositoryId) {
          setSelectedRepositoryId(previousRepo.id);
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [repositoriesWithSelection, selectedRepositoryId]);

  const localesForSelectedRepository = useMemo<LocaleRow[]>(() => {
    return buildLocalesForRepository(
      selectedRepository ?? null,
      (tag) => localeOptions.find((option) => option.tag === tag)?.label ?? tag,
      allowedLocaleTagSet,
    );
  }, [allowedLocaleTagSet, localeOptions, selectedRepository]);

  const handleSelectRepository = useCallback((id: number) => {
    setSelectedRepositoryId((previous) => (previous === id ? null : id));
  }, []);

  const handleChangeLocaleSelection = useCallback(
    (next: string[]) => {
      onChangeLocaleSelection(next);
    },
    [onChangeLocaleSelection],
  );

  const handleOpenWorkbench = useCallback(
    ({
      repositoryId,
      status,
      localeTag,
      count,
      usedFilter,
    }: {
      repositoryId: number;
      status?: string | null;
      localeTag?: string | null;
      count?: number | null;
      usedFilter?: 'USED' | 'UNUSED';
    }) => {
      const repository = repositories.find((repo) => repo.id === repositoryId);
      if (!repository) {
        return;
      }
      const repoLocaleTags = getNonRootRepositoryLocaleTags(repository);
      const allowedLocaleTags = allowedLocaleTagSet
        ? repoLocaleTags.filter((tag) => isLocaleTagAllowed(tag, allowedLocaleTagSet))
        : repoLocaleTags;
      const localeTags =
        localeTag != null ? [localeTag] : allowedLocaleTags.length ? allowedLocaleTags : [];
      if (localeTags.length === 0) {
        return;
      }
      const searchRequest: TextUnitSearchRequest = {
        repositoryIds: [repositoryId],
        localeTags,
        searchAttribute: 'target',
        searchType: 'contains',
        searchText: '',
        statusFilter: status ?? undefined,
        usedFilter,
        offset: 0,
      };

      if (typeof count === 'number' && count > 0) {
        const preferred = loadPreferredWorksetSize() ?? WORKSET_SIZE_DEFAULT;
        searchRequest.limit = clampWorksetSize(Math.max(count, preferred));
      }

      void navigate('/workbench', { state: { workbenchSearch: searchRequest } });
    },
    [allowedLocaleTagSet, navigate, repositories],
  );

  const handleOpenAiTranslate = useCallback(
    (repositoryId: number) => {
      const params = new URLSearchParams({ repositoryId: String(repositoryId) });
      void navigate(`/ai-translate?${params.toString()}`);
    },
    [navigate],
  );

  return (
    <RepositoriesPageView
      status={status}
      errorMessage={errorMessage}
      errorOnRetry={handleRetryFetch}
      repositories={repositoriesWithSelection}
      locales={localesForSelectedRepository}
      hasSelection={selectedRepositoryId != null}
      repositoryOptions={repositoryOptions}
      selectedRepositoryIds={selectedRepositoryIds}
      onChangeRepositorySelection={onChangeRepositorySelection}
      onOpenAiTranslate={handleOpenAiTranslate}
      localeOptions={localeOptions}
      selectedLocaleTags={selectedLocaleTags}
      onChangeLocaleSelection={handleChangeLocaleSelection}
      myLocaleSelections={myLocaleSelections}
      statusFilter={statusFilter}
      onStatusFilterChange={setStatusFilter}
      onSelectRepository={handleSelectRepository}
      onOpenWorkbench={handleOpenWorkbench}
      isRepositorySelectionEmpty={selectedRepositoryIds.length === 0}
    />
  );
}
