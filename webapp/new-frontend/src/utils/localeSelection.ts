import { useCallback, useEffect, useMemo, useState } from 'react';

import type { ApiRepository } from '../api/repositories';
import { useLocaleDisplayNameResolver } from './localeDisplayNames';
import { getNonRootRepositoryLocaleTags } from './repositoryLocales';

export type LocaleSelectionOption = {
  tag: string;
  label: string;
};

type UseLocaleSelectionOptions = {
  options: LocaleSelectionOption[];
  initialSelected?: string[];
  autoSelectAll?: boolean;
  allowStaleSelections?: boolean;
};

type UseLocaleSelectionResult = {
  selectedTags: string[];
  hasTouched: boolean;
  onChangeSelection: (next: string[]) => void;
  setSelection: (next: string[], options?: { markTouched?: boolean; touched?: boolean }) => void;
  availableTagSet: Set<string>;
  allowedTagSet?: Set<string>;
};

export function buildLocaleOptionsFromRepositories(
  repositories: ApiRepository[],
  resolveLocaleName: (tag: string) => string,
  allowedRepositoryIds?: Set<number>,
): LocaleSelectionOption[] {
  const tags = new Set<string>();
  repositories.forEach((repo) => {
    if (allowedRepositoryIds && !allowedRepositoryIds.has(repo.id)) {
      return;
    }
    getNonRootRepositoryLocaleTags(repo).forEach((tag) => tags.add(tag));
  });
  return Array.from(tags)
    .sort((first, second) => first.localeCompare(second, undefined, { sensitivity: 'base' }))
    .map((tag) => ({ tag, label: resolveLocaleName(tag) }));
}

export function useLocaleOptionsWithDisplayNames(
  repositories: ApiRepository[],
  allowedRepositoryIds?: Set<number>,
): LocaleSelectionOption[] {
  const resolveLocaleDisplayName = useLocaleDisplayNameResolver();
  return useMemo(
    () =>
      buildLocaleOptionsFromRepositories(
        repositories,
        resolveLocaleDisplayName,
        allowedRepositoryIds,
      ),
    [allowedRepositoryIds, repositories, resolveLocaleDisplayName],
  );
}

export function filterMyLocales({
  availableLocaleTags,
  userLocales,
  preferredLocales,
  isLimitedTranslator,
  isAdmin,
}: {
  availableLocaleTags: string[];
  userLocales: string[];
  preferredLocales: string[];
  isLimitedTranslator: boolean;
  isAdmin: boolean;
}): string[] {
  const availableSet = new Set(availableLocaleTags.map((tag) => tag.toLowerCase()));
  const candidates = isLimitedTranslator ? userLocales : isAdmin ? preferredLocales : [];
  if (!candidates.length) {
    return [];
  }
  return candidates.filter((tag) => availableSet.has(tag.toLowerCase()));
}

export function isLocaleTagAllowed(
  tag: string | null | undefined,
  allowedTagSet?: Set<string>,
): boolean {
  if (!allowedTagSet) {
    return true;
  }
  if (!tag) {
    return false;
  }
  return allowedTagSet.has(tag.toLowerCase());
}

const normalizeLocaleSelection = (
  values: string[],
  availableTagSet: Set<string>,
  allowStaleSelections: boolean,
  options: LocaleSelectionOption[],
) => {
  const seen = new Set<string>();
  const shouldFilterByAvailable = availableTagSet.size > 0 || !allowStaleSelections;

  return values.reduce<string[]>((accumulator, value) => {
    const lower = value.toLowerCase();
    if (seen.has(lower)) {
      return accumulator;
    }
    if (shouldFilterByAvailable && !availableTagSet.has(lower)) {
      return accumulator;
    }
    seen.add(lower);
    const resolvedValue =
      options.find((option) => option.tag.toLowerCase() === lower)?.tag ?? value;
    accumulator.push(resolvedValue);
    return accumulator;
  }, []);
};

const areLocaleSelectionsEqual = (left: string[], right: string[]) => {
  if (left.length !== right.length) {
    return false;
  }
  return left.every((value, index) => value.toLowerCase() === (right[index] ?? '').toLowerCase());
};

export function useLocaleSelection({
  options,
  initialSelected = [],
  autoSelectAll = false,
  allowStaleSelections = false,
}: UseLocaleSelectionOptions): UseLocaleSelectionResult {
  const availableTagSet = useMemo(
    () => new Set(options.map((option) => option.tag.toLowerCase())),
    [options],
  );

  const normalize = useCallback(
    (values: string[]) =>
      normalizeLocaleSelection(values, availableTagSet, allowStaleSelections, options),
    [allowStaleSelections, availableTagSet, options],
  );

  const [selectedTags, setSelectedTags] = useState<string[]>(() => normalize(initialSelected));
  const [hasTouched, setHasTouched] = useState(false);

  const setSelection = useCallback(
    (
      next: string[],
      { markTouched = false, touched }: { markTouched?: boolean; touched?: boolean } = {},
    ) => {
      const normalized = normalize(next);
      setSelectedTags((current) =>
        areLocaleSelectionsEqual(current, normalized) ? current : normalized,
      );
      if (typeof touched === 'boolean') {
        setHasTouched(touched);
      } else if (markTouched) {
        setHasTouched(true);
      }
    },
    [normalize],
  );

  const onChangeSelection = useCallback(
    (next: string[]) => {
      setSelection(next, { markTouched: true });
    },
    [setSelection],
  );

  useEffect(() => {
    setSelectedTags((current) => {
      const normalized = normalize(current);
      return areLocaleSelectionsEqual(current, normalized) ? current : normalized;
    });
  }, [normalize]);

  useEffect(() => {
    if (!autoSelectAll || hasTouched || selectedTags.length > 0 || options.length === 0) {
      return;
    }
    setSelectedTags(options.map((option) => option.tag));
  }, [autoSelectAll, hasTouched, options, selectedTags.length]);

  const allowedTagSet = useMemo(() => {
    if (selectedTags.length === 0) {
      return hasTouched ? new Set<string>() : undefined;
    }
    return new Set(selectedTags.map((tag) => tag.toLowerCase()));
  }, [hasTouched, selectedTags]);

  return {
    selectedTags,
    hasTouched,
    onChangeSelection,
    setSelection,
    availableTagSet,
    allowedTagSet,
  };
}
