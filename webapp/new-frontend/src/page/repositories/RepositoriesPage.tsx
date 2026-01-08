import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import type {
  ApiLocale,
  ApiRepository,
  ApiRepositoryLocaleStatistic,
} from '../../api/repositories';
import type { TextUnitSearchRequest } from '../../api/text-units';
import { useRepositories } from '../../hooks/useRepositories';
import { useLocaleDisplayNameResolver } from '../../utils/localeDisplayNames';
import { getNonRootRepositoryLocaleTags } from '../../utils/repositoryLocales';
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
): RepositoryRow => {
  const summary = getRepositorySummary(repository);

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
): RepositoryRow[] =>
  repositories.map((repository) => buildRepositoryRow(repository, selectedRepositoryId));

const getRepositorySummary = (repository: ApiRepository) => {
  const fullyTranslatedTags = getFullyTranslatedLocaleTags(repository);
  const localeStats = repository.repositoryStatistic?.repositoryLocaleStatistics ?? [];

  let rejected = 0;
  let needsTranslation = 0;
  let needsReview = 0;

  localeStats.forEach((localeStat) => {
    const localeTag = getLocaleTag(localeStat.locale);
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
): LocaleRow[] => {
  const localeStats = repository.repositoryStatistic?.repositoryLocaleStatistics ?? [];
  const sourceLocaleTag = repository.sourceLocale?.bcp47Tag;

  return localeStats
    .map((localeStat, index) => {
      const localeTag = getLocaleTag(localeStat.locale);
      if (!localeTag || localeTag === sourceLocaleTag) {
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
): LocaleRow[] => {
  if (!repository) {
    return [];
  }

  return buildLocaleRows(repository, resolveLocaleName);
};

export function RepositoriesPage() {
  const [selectedRepositoryId, setSelectedRepositoryId] = useState<number | null>(null);
  const [searchValue, setSearchValue] = useState('');
  const [statusFilter, setStatusFilter] = useState<RepositoryStatusFilter>('all');
  const navigate = useNavigate();
  const resolveLocaleDisplayName = useLocaleDisplayNameResolver();

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

  const normalizedSearchValue = searchValue.trim().toLowerCase();

  const filteredRepositories = useMemo(() => {
    return repositories.filter((repository) => {
      if (normalizedSearchValue && !repository.name.toLowerCase().includes(normalizedSearchValue)) {
        return false;
      }

      const summary = getRepositorySummary(repository);

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
  }, [normalizedSearchValue, repositories, statusFilter]);

  const repositoriesWithSelection = useMemo(
    () => buildRepositoriesWithSelection(filteredRepositories, selectedRepositoryId),
    [filteredRepositories, selectedRepositoryId],
  );

  const selectedRepository = useMemo(
    () => repositories.find((repository) => repository.id === selectedRepositoryId) ?? null,
    [repositories, selectedRepositoryId],
  );

  useEffect(() => {
    if (selectedRepositoryId == null) {
      return;
    }

    const stillVisible = filteredRepositories.some((repo) => repo.id === selectedRepositoryId);
    if (!stillVisible) {
      setSelectedRepositoryId(null);
    }
  }, [filteredRepositories, selectedRepositoryId]);

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
    return buildLocalesForRepository(selectedRepository ?? null, resolveLocaleDisplayName);
  }, [resolveLocaleDisplayName, selectedRepository]);

  const handleSelectRepository = useCallback((id: number) => {
    setSelectedRepositoryId((previous) => (previous === id ? null : id));
  }, []);

  const handleOpenWorkbench = useCallback(
    ({
      repositoryId,
      status,
      localeTag,
    }: {
      repositoryId: number;
      status?: string | null;
      localeTag?: string | null;
    }) => {
      const repository = repositories.find((repo) => repo.id === repositoryId);
      if (!repository) {
        return;
      }

      const searchRequest: TextUnitSearchRequest = {
        repositoryIds: [repositoryId],
        localeTags: localeTag != null ? [localeTag] : getNonRootRepositoryLocaleTags(repository),
        searchAttribute: 'target',
        searchType: 'contains',
        searchText: '',
        statusFilter: status ?? undefined,
      };

      void navigate('/workbench', { state: { workbenchSearch: searchRequest } });
    },
    [navigate, repositories],
  );

  return (
    <RepositoriesPageView
      status={status}
      errorMessage={errorMessage}
      errorOnRetry={handleRetryFetch}
      repositories={repositoriesWithSelection}
      locales={localesForSelectedRepository}
      hasSelection={selectedRepositoryId != null}
      searchValue={searchValue}
      onSearchChange={setSearchValue}
      statusFilter={statusFilter}
      onStatusFilterChange={setStatusFilter}
      onSelectRepository={handleSelectRepository}
      onOpenWorkbench={handleOpenWorkbench}
      selectedRepositoryId={selectedRepositoryId}
    />
  );
}
