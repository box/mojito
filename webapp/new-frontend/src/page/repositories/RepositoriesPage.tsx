import { useCallback, useEffect, useMemo, useState } from 'react';

import type { LocaleRow, RepositoryRow } from './RepositoriesPageView';
import { RepositoriesPageView } from './RepositoriesPageView';

const REPOSITORIES: Omit<RepositoryRow, 'selected'>[] = [
  { id: 1, name: 'Web App', rejected: 8, needsTranslation: 58, needsReview: 120 },
  { id: 2, name: 'Mobile App', rejected: 3, needsTranslation: 20, needsReview: 95 },
  { id: 3, name: 'Marketing Site', rejected: 0, needsTranslation: 0, needsReview: 0 },
  ...Array.from({ length: 47 }, (_, index) => ({
    id: index + 4,
    name: `Repo ${index + 1}`,
    rejected: (index * 3) % 12,
    needsTranslation: (index * 7) % 20,
    needsReview: (index * 5) % 15,
  })),
];

const LOCALES: LocaleRow[] = [
  { id: 1, name: 'Spanish (Spain)', rejected: 5, needsTranslation: 12, needsReview: 6 },
  { id: 2, name: 'Japanese', rejected: 1, needsTranslation: 4, needsReview: 2 },
  { id: 3, name: 'French (France)', rejected: 0, needsTranslation: 5, needsReview: 2 },
  { id: 4, name: 'German', rejected: 0, needsTranslation: 3, needsReview: 1 },
  { id: 5, name: 'Portuguese (Brazil)', rejected: 0, needsTranslation: 6, needsReview: 3 },
  ...Array.from({ length: 45 }, (_, index) => ({
    id: index + 6,
    name: `Locale ${index + 1}`,
    rejected: (index * 2) % 7,
    needsTranslation: (index * 3) % 12,
    needsReview: (index * 4) % 10,
  })),
];

export function RepositoriesPage() {
  const [selectedRepositoryId, setSelectedRepositoryId] = useState<number | null>(null);
  const [searchValue, setSearchValue] = useState('');

  const filteredRepositories = useMemo(() => {
    if (!searchValue.trim()) {
      return REPOSITORIES;
    }

    const search = searchValue.trim().toLowerCase();
    return REPOSITORIES.filter((repo) => repo.name.toLowerCase().includes(search));
  }, [searchValue]);

  const repositoriesWithSelection: RepositoryRow[] = useMemo(
    () =>
      filteredRepositories.map((repo) => ({
        ...repo,
        selected: repo.id === selectedRepositoryId,
      })),
    [filteredRepositories, selectedRepositoryId],
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

  const localesForSelectedRepository = useMemo(() => {
    if (selectedRepositoryId == null) {
      return [];
    }

    // simple derived data to visualize repo-specific locale numbers
    const factor = (selectedRepositoryId % 4) + 1;
    const offset = selectedRepositoryId % 3;

    return LOCALES.map((locale, index) => ({
      ...locale,
      rejected: (locale.rejected + offset + index) % 10,
      needsTranslation: (locale.needsTranslation + factor + index) % 30,
      needsReview: (locale.needsReview + factor + offset + index) % 22,
    }));
  }, [selectedRepositoryId]);

  const handleSelectRepository = useCallback((id: number) => {
    setSelectedRepositoryId((previous) => (previous === id ? null : id));
  }, []);

  return (
    <RepositoriesPageView
      repositories={repositoriesWithSelection}
      locales={localesForSelectedRepository}
      hasSelection={selectedRepositoryId != null}
      searchValue={searchValue}
      onSearchChange={setSearchValue}
      onSelectRepository={handleSelectRepository}
    />
  );
}
