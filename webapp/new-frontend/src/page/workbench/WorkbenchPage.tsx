import { useQuery } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import type { ApiTextUnit, TextUnitSearchRequest } from '../../api/text-units';
import type { SearchAttribute, SearchType } from '../../api/text-units';
import { searchTextUnits } from '../../api/text-units';
import { useRepositories } from '../../hooks/useRepositories';
import { useLocaleDisplayNameResolver } from '../../utils/localeDisplayNames';
import type {
  LocaleOption,
  RepositoryOption,
  StatusFilterValue,
  WorkbenchRow,
} from './WorkbenchPageView';
import { WorkbenchPageView } from './WorkbenchPageView';

const statusOptions = ['Accepted', 'To review', 'To translate', 'Rejected'];
const DEFAULT_SEARCH_LIMIT = 50;

type SearchQueryKey = ['workbench-search', TextUnitSearchRequest | null];

export function WorkbenchPage() {
  const [rows, setRows] = useState<WorkbenchRow[]>([]);
  const [editingRowId, setEditingRowId] = useState<string | null>(null);
  const [editingValue, setEditingValue] = useState('');
  const [editingInitialValue, setEditingInitialValue] = useState('');
  const [pendingEditingTarget, setPendingEditingTarget] = useState<{
    rowId: string;
    translation: string | null;
  } | null>(null);
  const [searchAttribute, setSearchAttribute] = useState<SearchAttribute>('target');
  const [searchType, setSearchType] = useState<SearchType>('contains');
  const [searchInputValue, setSearchInputValue] = useState('');
  const [appliedSearchText, setAppliedSearchText] = useState('');
  const [selectedRepositoryIds, setSelectedRepositoryIds] = useState<number[]>([]);
  const [selectedLocaleTags, setSelectedLocaleTags] = useState<string[]>([]);
  const [statusFilter, setStatusFilter] = useState<StatusFilterValue>('ALL');
  const [includeUsed, setIncludeUsed] = useState(true);
  const [includeUnused, setIncludeUnused] = useState(false);
  const [includeTranslate, setIncludeTranslate] = useState(true);
  const [includeDoNotTranslate, setIncludeDoNotTranslate] = useState(true);
  const [createdBefore, setCreatedBefore] = useState<string | null>(null);
  const [createdAfter, setCreatedAfter] = useState<string | null>(null);
  const translationInputRef = useRef<HTMLTextAreaElement | null>(null);
  const rowRefs = useRef<Record<string, HTMLDivElement | null>>({});

  const {
    data: repositories,
    isLoading: isRepositoriesLoading,
    isError: isRepositoriesError,
    error: repositoriesError,
  } = useRepositories();
  const resolveLocaleName = useLocaleDisplayNameResolver();

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

  const handleChangeRepositorySelection = useCallback(
    (nextSelected: number[]) => {
      const allowedIds = new Set(repositoryOptions.map((option) => option.id));
      const uniqueNext = nextSelected.filter(
        (value, index, array) => array.indexOf(value) === index,
      );
      const filtered = uniqueNext.filter((value) => allowedIds.has(value));
      setSelectedRepositoryIds(filtered);
    },
    [repositoryOptions],
  );

  const handleChangeLocaleSelection = useCallback(
    (nextSelected: string[]) => {
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
      setSelectedRepositoryIds([]);
      return;
    }

    setSelectedRepositoryIds((current) => {
      const allowedIds = new Set(repositoryOptions.map((option) => option.id));
      const filtered = current.filter((id) => allowedIds.has(id));
      if (filtered.length > 0) {
        return filtered;
      }
      return [repositoryOptions[0].id];
    });
  }, [repositoryOptions]);

  useEffect(() => {
    if (!localeOptions.length) {
      setSelectedLocaleTags([]);
      return;
    }

    setSelectedLocaleTags((current) => {
      const allowedTags = new Set(localeOptions.map((option) => option.tag));
      const filtered = current.filter((tag) => allowedTags.has(tag));
      if (filtered.length > 0) {
        return filtered;
      }
      return [localeOptions[0].tag];
    });
  }, [localeOptions]);

  const canSearch = selectedRepositoryIds.length > 0 && selectedLocaleTags.length > 0;

  const searchRequest: TextUnitSearchRequest | null = useMemo(() => {
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
      searchText: appliedSearchText,
      limit: DEFAULT_SEARCH_LIMIT,
      offset: 0,
      statusFilter: statusFilter === 'ALL' ? undefined : statusFilter,
      usedFilter,
      doNotTranslateFilter,
      tmTextUnitCreatedBefore: createdBefore ?? undefined,
      tmTextUnitCreatedAfter: createdAfter ?? undefined,
    };
  }, [
    appliedSearchText,
    searchAttribute,
    searchType,
    selectedLocaleTags,
    selectedRepositoryIds,
    statusFilter,
    includeUsed,
    includeUnused,
    includeTranslate,
    includeDoNotTranslate,
    createdBefore,
    createdAfter,
  ]);

  const searchQuery = useQuery<ApiTextUnit[], Error, ApiTextUnit[], SearchQueryKey>({
    queryKey: ['workbench-search', searchRequest],
    queryFn: () => {
      if (!searchRequest) {
        return Promise.resolve([]);
      }
      return searchTextUnits(searchRequest);
    },
    enabled: Boolean(searchRequest),
    keepPreviousData: true,
  });

  const apiRows = useMemo(
    () => (searchQuery.data ?? []).map(mapApiTextUnitToRow),
    [searchQuery.data],
  );

  useEffect(() => {
    if (!searchRequest) {
      setRows([]);
      setEditingRowId(null);
      setEditingValue('');
      setEditingInitialValue('');
      setPendingEditingTarget(null);
      return;
    }

    if (!searchQuery.data) {
      return;
    }

    setRows(apiRows);
    setEditingRowId(null);
    setEditingValue('');
    setEditingInitialValue('');
    setPendingEditingTarget(null);
  }, [apiRows, searchQuery.data, searchRequest]);

  const registerRowRef = useCallback((rowId: string, element: HTMLDivElement | null) => {
    rowRefs.current[rowId] = element;
  }, []);

  const applySearchText = useCallback((text: string) => {
    setAppliedSearchText((previous) => (previous === text ? previous : text));
  }, []);

  const handleChangeSearchInput = useCallback((value: string) => {
    setSearchInputValue(value);
  }, []);

  const handleSubmitSearch = useCallback(() => {
    if (!canSearch) {
      return;
    }

    applySearchText(searchInputValue);
  }, [applySearchText, canSearch, searchInputValue]);

  const handleChangeSearchAttribute = useCallback(
    (attribute: SearchAttribute) => {
      setSearchAttribute(attribute);
      applySearchText(searchInputValue);
    },
    [applySearchText, searchInputValue],
  );

  const handleChangeSearchType = useCallback(
    (type: SearchType) => {
      setSearchType(type);
      applySearchText(searchInputValue);
    },
    [applySearchText, searchInputValue],
  );

  const handleStartEditing = useCallback((rowId: string, translation: string | null) => {
    const nextValue = translation ?? '';
    setEditingRowId(rowId);
    setEditingValue(nextValue);
    setEditingInitialValue(nextValue);
    setPendingEditingTarget(null);
  }, []);

  const handleCancelEditing = useCallback(() => {
    setEditingRowId(null);
    setEditingValue('');
    setEditingInitialValue('');
    setPendingEditingTarget(null);
  }, []);

  const handleChangeEditingValue = useCallback((value: string) => {
    setEditingValue(value);
  }, []);

  const hasUnsavedChanges = editingRowId !== null && editingValue !== editingInitialValue;

  const handleRequestStartEditing = useCallback(
    (rowId: string, translation: string | null) => {
      if (editingRowId && editingRowId !== rowId && hasUnsavedChanges) {
        setPendingEditingTarget({ rowId, translation });
        return;
      }

      handleStartEditing(rowId, translation);
    },
    [editingRowId, hasUnsavedChanges, handleStartEditing],
  );

  const handleConfirmDiscardEditing = useCallback(() => {
    if (!pendingEditingTarget) {
      return;
    }
    handleStartEditing(pendingEditingTarget.rowId, pendingEditingTarget.translation);
  }, [pendingEditingTarget, handleStartEditing]);

  const handleDismissDiscardEditing = useCallback(() => {
    setPendingEditingTarget(null);
  }, []);

  const handleSaveEditing = useCallback(() => {
    if (!editingRowId) {
      return;
    }
    setRows((previousRows) =>
      previousRows.map((row) =>
        row.id === editingRowId
          ? {
              ...row,
              translation: editingValue === '' ? null : editingValue,
              status: 'Accepted',
            }
          : row,
      ),
    );
    handleCancelEditing();
  }, [editingRowId, editingValue, handleCancelEditing]);

  const handleChangeStatus = useCallback((rowId: string, status: string) => {
    setRows((previousRows) =>
      previousRows.map((row) => (row.id === rowId ? { ...row, status } : row)),
    );
  }, []);

  useEffect(() => {
    if (!editingRowId) {
      return;
    }

    const rowElement = rowRefs.current[editingRowId];
    if (rowElement) {
      rowElement.scrollIntoView({ block: 'nearest' });
    }

    if (translationInputRef.current && document.activeElement !== translationInputRef.current) {
      translationInputRef.current.focus();
    }
  }, [editingRowId]);

  const repositoryErrorMessage = isRepositoriesError
    ? repositoriesError instanceof Error
      ? repositoriesError.message
      : 'Failed to load repositories.'
    : null;

  const searchErrorMessage =
    searchQuery.isError && searchRequest ? searchQuery.error.message : null;

  const isSearchDirty = searchInputValue !== appliedSearchText;
  const displayRowCount = rows.length;
  const totalRowCount = rows.length;

  return (
    <WorkbenchPageView
      rows={rows}
      editingRowId={editingRowId}
      editingValue={editingValue}
      onStartEditing={handleRequestStartEditing}
      onCancelEditing={handleCancelEditing}
      onSaveEditing={handleSaveEditing}
      onChangeEditingValue={handleChangeEditingValue}
      onChangeStatus={handleChangeStatus}
      statusOptions={statusOptions}
      showDiscardDialog={pendingEditingTarget !== null}
      onConfirmDiscardEditing={handleConfirmDiscardEditing}
      onDismissDiscardEditing={handleDismissDiscardEditing}
      translationInputRef={translationInputRef}
      registerRowRef={registerRowRef}
      searchAttribute={searchAttribute}
      searchType={searchType}
      searchInputValue={searchInputValue}
      onChangeSearchInput={handleChangeSearchInput}
      onSubmitSearch={handleSubmitSearch}
      onChangeSearchAttribute={handleChangeSearchAttribute}
      onChangeSearchType={handleChangeSearchType}
      isSearchDirty={isSearchDirty}
      totalRowCount={totalRowCount}
      displayRowCount={displayRowCount}
      repositoryOptions={repositoryOptions}
      selectedRepositoryIds={selectedRepositoryIds}
      onChangeRepositorySelection={handleChangeRepositorySelection}
      isRepositoryLoading={isRepositoriesLoading}
      repositoryErrorMessage={repositoryErrorMessage}
      localeOptions={localeOptions}
      selectedLocaleTags={selectedLocaleTags}
      onChangeLocaleSelection={handleChangeLocaleSelection}
      statusFilter={statusFilter}
      includeUsed={includeUsed}
      includeUnused={includeUnused}
      includeTranslate={includeTranslate}
      includeDoNotTranslate={includeDoNotTranslate}
      onChangeStatusFilter={setStatusFilter}
      onChangeIncludeUsed={setIncludeUsed}
      onChangeIncludeUnused={setIncludeUnused}
      onChangeIncludeTranslate={setIncludeTranslate}
      onChangeIncludeDoNotTranslate={setIncludeDoNotTranslate}
      createdBefore={createdBefore}
      createdAfter={createdAfter}
      onChangeCreatedBefore={setCreatedBefore}
      onChangeCreatedAfter={setCreatedAfter}
      isSearchLoading={searchQuery.isFetching}
      searchErrorMessage={searchErrorMessage}
      onRetrySearch={() => {
        void searchQuery.refetch();
      }}
      canSearch={canSearch}
    />
  );
}

function mapApiTextUnitToRow(textUnit: ApiTextUnit): WorkbenchRow {
  return {
    id: `${textUnit.tmTextUnitId}:${textUnit.targetLocale}`,
    textUnitName: textUnit.name,
    repositoryName: textUnit.repositoryName ?? '',
    assetPath: textUnit.assetPath ?? null,
    locale: textUnit.targetLocale,
    source: textUnit.source ?? '',
    translation: textUnit.target ?? null,
    status: formatStatus(textUnit.status, textUnit.includedInLocalizedFile),
    comment: textUnit.comment ?? null,
    tmTextUnitId: textUnit.tmTextUnitId,
  };
}

function formatStatus(status?: string | null, includedInLocalizedFile?: boolean) {
  if (includedInLocalizedFile === false) {
    return 'Rejected';
  }

  switch (status) {
    case 'APPROVED':
      return 'Accepted';
    case 'REVIEW_NEEDED':
      return 'To review';
    case 'TRANSLATION_NEEDED':
      return 'To translate';
    default:
      return 'To translate';
  }
}
