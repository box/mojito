import { useCallback, useEffect, useMemo, useState } from 'react';

import type { ApiRepository } from '../api/repositories';

export type RepositorySelectionOption = {
  id: number;
  name: string;
};

type UseRepositorySelectionOptions = {
  options: RepositorySelectionOption[];
  initialSelected?: number[];
  allowStaleSelections?: boolean;
};

type UseRepositorySelectionResult = {
  selectedIds: number[];
  hasTouched: boolean;
  onChangeSelection: (next: number[]) => void;
  setSelection: (next: number[], options?: { markTouched?: boolean; touched?: boolean }) => void;
  availableIdSet: Set<number>;
};

export function useRepositorySelectionOptions(
  repositories: Array<ApiRepository | null | undefined> | null | undefined,
): RepositorySelectionOption[] {
  return useMemo(() => {
    if (!Array.isArray(repositories)) {
      return [];
    }
    return repositories
      .filter((repo): repo is ApiRepository => Boolean(repo && typeof repo.id === 'number'))
      .map((repo) => ({ id: repo.id, name: repo.name }));
  }, [repositories]);
}

const normalizeRepositorySelection = (
  values: number[],
  availableIdSet: Set<number>,
  allowStaleSelections: boolean,
  options: RepositorySelectionOption[],
) => {
  const seen = new Set<number>();
  const shouldFilterByAvailable = availableIdSet.size > 0 || !allowStaleSelections;

  return values.reduce<number[]>((accumulator, value) => {
    if (seen.has(value)) {
      return accumulator;
    }
    if (shouldFilterByAvailable && !availableIdSet.has(value)) {
      return accumulator;
    }
    seen.add(value);
    const resolvedValue = options.find((option) => option.id === value)?.id ?? value;
    accumulator.push(resolvedValue);
    return accumulator;
  }, []);
};

const areRepositorySelectionsEqual = (left: number[], right: number[]) => {
  if (left.length !== right.length) {
    return false;
  }
  return left.every((value, index) => value === right[index]);
};

export function useRepositorySelection({
  options,
  initialSelected = [],
  allowStaleSelections = false,
}: UseRepositorySelectionOptions): UseRepositorySelectionResult {
  const availableIdSet = useMemo(() => new Set(options.map((option) => option.id)), [options]);

  const normalize = useCallback(
    (values: number[]) =>
      normalizeRepositorySelection(values, availableIdSet, allowStaleSelections, options),
    [allowStaleSelections, availableIdSet, options],
  );

  const [selectedIds, setSelectedIds] = useState<number[]>(() => normalize(initialSelected));
  const [hasTouched, setHasTouched] = useState(false);

  const setSelection = useCallback(
    (
      next: number[],
      { markTouched = false, touched }: { markTouched?: boolean; touched?: boolean } = {},
    ) => {
      const normalized = normalize(next);
      setSelectedIds((current) =>
        areRepositorySelectionsEqual(current, normalized) ? current : normalized,
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
    (next: number[]) => {
      setSelection(next, { markTouched: true });
    },
    [setSelection],
  );

  useEffect(() => {
    setSelectedIds((current) => {
      const normalized = normalize(current);
      return areRepositorySelectionsEqual(current, normalized) ? current : normalized;
    });
  }, [normalize]);

  return {
    selectedIds,
    hasTouched,
    onChangeSelection,
    setSelection,
    availableIdSet,
  };
}
