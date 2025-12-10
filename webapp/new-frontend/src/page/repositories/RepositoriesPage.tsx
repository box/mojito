import { useMemo, useState } from 'react';

import type { LocaleRow, RepositoryRow } from './RepositoriesPageView';
import { RepositoriesPageView } from './RepositoriesPageView';

const REPOSITORIES: Omit<RepositoryRow, 'selected'>[] = [
  { id: 1, name: 'Web App', rejected: 16, needsTranslation: 58, needsReview: 27 },
  { id: 2, name: 'Mobile App', rejected: 6, needsTranslation: 16, needsReview: 8 },
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
  const [selectedRepositoryId, setSelectedRepositoryId] = useState<number>(REPOSITORIES[0].id);
  const [searchValue, setSearchValue] = useState('');

  const filteredRepositories = useMemo(() => {
    if (!searchValue.trim()) {
      return REPOSITORIES;
    }

    const search = searchValue.trim().toLowerCase();
    return REPOSITORIES.filter((repo) => repo.name.toLowerCase().includes(search));
  }, [searchValue]);

  const repositoriesWithSelection: RepositoryRow[] = filteredRepositories.map((repo) => ({
    ...repo,
    selected: repo.id === selectedRepositoryId,
  }));

  return (
    <RepositoriesPageView
      repositories={repositoriesWithSelection}
      locales={LOCALES}
      searchValue={searchValue}
      onSearchChange={setSearchValue}
      onSelectRepository={setSelectedRepositoryId}
    />
  );
}
