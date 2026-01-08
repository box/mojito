import type { ApiRepository } from '../api/repositories';

export function getNonRootRepositoryLocaleTags(repository: ApiRepository): string[] {
  const tags = (repository.repositoryLocales ?? [])
    .filter((repositoryLocale) => repositoryLocale.parentLocale != null)
    .map((repoLocale) => repoLocale.locale.bcp47Tag);

  return Array.from(new Set(tags));
}
