import type { ApiRepository } from '../api/repositories';

export function getRepositoryLocaleTags(repository: ApiRepository): string[] {
  const sourceLocaleTag = repository.sourceLocale?.bcp47Tag;

  const tags = (repository.repositoryLocales ?? [])
    .map((repoLocale) => repoLocale.locale?.bcp47Tag)
    .filter((tag): tag is string => Boolean(tag) && tag !== sourceLocaleTag);

  return Array.from(new Set(tags));
}
