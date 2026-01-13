import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';

import type { TextUnitSearchRequest } from '../../api/text-units';
import { useUser } from '../../components/RequireUser';
import { canEditLocale as canEditLocaleForUser } from '../../utils/permissions';
import { getNonRootRepositoryLocaleTags } from '../../utils/repositoryLocales';
import { useWorkbenchCollections } from './useWorkbenchCollections';
import { useWorkbenchEdits } from './useWorkbenchEdits';
import { useWorkbenchSearch } from './useWorkbenchSearch';
import { clampWorksetSize } from './workbench-helpers';
import { loadWorkbenchShare } from './workbench-share';
import type { WorkbenchCollection, WorkbenchShareOverrides } from './workbench-types';
import { WorkbenchPageView } from './WorkbenchPageView';

const statusOptions = ['Accepted', 'To review', 'To translate', 'Rejected'];
const localePromptBody =
  'Pick one or more locales to open these collection results.\n\nIf you are working across repositories with different locale sets, select multiple locales to see all text units.';
const localePromptTitle = 'Choose a locale';

type WorkbenchLocationState = { workbenchSearch?: TextUnitSearchRequest | null } & Record<
  string,
  unknown
> & { localePrompt?: boolean };

function isWorkbenchLocationState(state: unknown): state is WorkbenchLocationState {
  return typeof state === 'object' && state !== null && 'workbenchSearch' in state;
}

export function WorkbenchPage() {
  const [isEditMode, setIsEditMode] = useState(false);
  const [pendingCollectionOpenId, setPendingCollectionOpenId] = useState<string | null>(null);
  const [shareOverrides, setShareOverrides] = useState<WorkbenchShareOverrides | null>(null);
  const location = useLocation();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const shareId = searchParams.get('shareId');
  const [shareIdToHydrate, setShareIdToHydrate] = useState<string | null>(shareId);
  const shareSearchParamsRef = useRef<URLSearchParams | null>(
    shareId ? new URLSearchParams(searchParams) : null,
  );
  const [hydratedSearchRequest, setHydratedSearchRequest] = useState<TextUnitSearchRequest | null>(
    null,
  );
  const [hydrationModal, setHydrationModal] = useState<{ title: string; body: string } | null>(
    null,
  );
  const currentUser = useUser();
  const locationState = (location.state as WorkbenchLocationState | null) ?? null;
  const stateSearchRequest = locationState?.workbenchSearch ?? null;
  const stateLocalePrompt = locationState?.localePrompt ?? false;
  const userLocales = currentUser.userLocales ?? [];
  const isLimitedTranslator = !currentUser.canTranslateAllLocales && userLocales.length > 0;
  const canEditLocale = useCallback(
    (locale: string) => canEditLocaleForUser(currentUser, locale),
    [currentUser],
  );

  const search = useWorkbenchSearch({
    isEditMode,
    initialSearchRequest: hydratedSearchRequest,
    canEditLocale,
  });
  const collections = useWorkbenchCollections();

  const clearShareIdFromUrl = useCallback(() => {
    const nextParams = new URLSearchParams(shareSearchParamsRef.current ?? undefined);
    if (!nextParams.has('shareId')) {
      return;
    }
    nextParams.delete('shareId');
    setSearchParams(nextParams, { replace: true });
    shareSearchParamsRef.current = nextParams;
  }, [setSearchParams]);

  const clearWorkbenchSearchState = useCallback(() => {
    if (!isWorkbenchLocationState(location.state)) {
      return;
    }
    const rest = { ...location.state };
    delete rest.workbenchSearch;
    if ('localePrompt' in rest) {
      delete (rest as { localePrompt?: boolean }).localePrompt;
    }
    void navigate(location.pathname + location.search, { replace: true, state: rest });
  }, [location.pathname, location.search, location.state, navigate]);

  useEffect(() => {
    if (!shareId) {
      return;
    }
    setShareIdToHydrate(shareId);
    shareSearchParamsRef.current = new URLSearchParams(searchParams);
  }, [shareId, searchParams]);

  useEffect(() => {
    if (!stateSearchRequest) {
      return;
    }
    setShareIdToHydrate(null);
    setHydrationModal(
      stateLocalePrompt
        ? {
            title: localePromptTitle,
            body: localePromptBody,
          }
        : null,
    );
    setHydratedSearchRequest(stateSearchRequest);
    clearWorkbenchSearchState();
  }, [clearWorkbenchSearchState, stateLocalePrompt, stateSearchRequest]);

  useEffect(() => {
    if (!shareIdToHydrate) {
      return;
    }
    setHydrationModal(null);
    let cancelled = false;
    loadWorkbenchShare(shareIdToHydrate)
      .then((payload) => {
        if (cancelled) {
          return;
        }
        const nextRequest = payload.searchRequest;
        if (payload.localeFocus === 'ASK_RECIPIENT') {
          setHydrationModal({
            title: localePromptTitle,
            body: localePromptBody,
          });
        } else {
          setHydrationModal(null);
        }
        setHydratedSearchRequest(nextRequest);
      })
      .catch((error: unknown) => {
        if (cancelled) {
          return;
        }
        const message = error instanceof Error ? error.message : 'Failed to load share link.';
        setHydrationModal({ title: 'Could not load share link', body: message });
      })
      .finally(() => {
        if (cancelled) {
          return;
        }
        clearShareIdFromUrl();
      });
    return () => {
      cancelled = true;
    };
  }, [clearShareIdFromUrl, shareIdToHydrate]);

  const edits = useWorkbenchEdits({
    apiRows: search.rows,
    canSearch: search.canSearch,
    activeSearchRequest: search.activeSearchRequest,
    setIsEditMode,
  });
  const { clearWorksetEdits } = edits;
  const { refetchSearch } = search;

  const activeCollectionIds = useMemo(() => {
    const entries = collections.activeCollection?.entries ?? [];
    return new Set(entries.map((entry) => entry.tmTextUnitId));
  }, [collections.activeCollection]);

  const activeCollectionCount = collections.activeCollection?.entries.length ?? 0;

  const repositoryIdByName = useMemo(
    () => new Map(search.repositories.map((repo) => [repo.name, repo.id])),
    [search.repositories],
  );

  const buildCollectionSearchRequest = useCallback(
    (collection: WorkbenchCollection): TextUnitSearchRequest | null => {
      const repoMap = new Map(search.repositories.map((repo) => [repo.id, repo]));
      const repoIds = new Set<number>();
      const localeSet = new Set<string>();

      collection.entries.forEach((entry) => {
        if (!entry.repositoryId) {
          return;
        }
        const repo = repoMap.get(entry.repositoryId);
        if (!repo) {
          return;
        }
        repoIds.add(repo.id);
        getNonRootRepositoryLocaleTags(repo).forEach((tag) => localeSet.add(tag));
      });

      const repositoryIds = Array.from(repoIds);
      const localeTags = Array.from(localeSet);
      const textUnitIds = Array.from(
        new Set(collection.entries.map((entry) => entry.tmTextUnitId)),
      );
      const worksetSize = clampWorksetSize(textUnitIds.length);

      if (!repositoryIds.length || !localeTags.length || !textUnitIds.length) {
        return null;
      }

      return {
        repositoryIds,
        localeTags: [],
        searchAttribute: 'tmTextUnitIds',
        searchType: 'exact',
        searchText: textUnitIds.join(','),
        limit: worksetSize,
        offset: 0,
      };
    },
    [search.repositories],
  );

  const handleAddToCollection = useCallback(
    (tmTextUnitId: number, repositoryId: number | null) => {
      collections.addToActiveCollection(tmTextUnitId, repositoryId);
    },
    [collections],
  );

  const handleAddAllToCollection = useCallback(() => {
    const entries = search.rows
      .map((row) => {
        const repositoryId = repositoryIdByName.get(row.repositoryName) ?? null;
        if (repositoryId === null) {
          return null;
        }
        return { tmTextUnitId: row.tmTextUnitId, repositoryId };
      })
      .filter((entry): entry is { tmTextUnitId: number; repositoryId: number } => entry !== null);
    if (!entries.length) {
      return;
    }
    collections.addEntriesToActiveCollection(entries);
  }, [collections, repositoryIdByName, search.rows]);

  const handleRemoveFromCollection = useCallback(
    (tmTextUnitId: number) => {
      collections.removeFromActiveCollection(tmTextUnitId);
    },
    [collections],
  );

  const handleOpenCollectionSearch = useCallback(
    (collectionId: string) => {
      const collection = collections.collections.find((item) => item.id === collectionId);
      if (!collection || collection.entries.length === 0) {
        return;
      }

      const request = buildCollectionSearchRequest(collection);
      if (!request) {
        setPendingCollectionOpenId(collectionId);
        return;
      }

      setPendingCollectionOpenId(null);
      void navigate('/workbench', { state: { workbenchSearch: request, localePrompt: true } });
    },
    [buildCollectionSearchRequest, collections.collections, navigate],
  );

  const handleShareCollection = useCallback(
    (collectionId: string): boolean => {
      const collection = collections.collections.find((item) => item.id === collectionId);
      if (!collection || collection.entries.length === 0) {
        return false;
      }
      const request = buildCollectionSearchRequest(collection);
      if (!request) {
        return false;
      }
      const pinnedIds = Array.from(new Set(collection.entries.map((entry) => entry.tmTextUnitId)));
      setShareOverrides({ searchRequest: request, pinnedIds, forceAskLocale: true });
      return true;
    },
    [buildCollectionSearchRequest, collections.collections],
  );

  useEffect(() => {
    if (!pendingCollectionOpenId) {
      return;
    }
    const collection = collections.collections.find((item) => item.id === pendingCollectionOpenId);
    if (!collection) {
      setPendingCollectionOpenId(null);
      return;
    }
    const request = buildCollectionSearchRequest(collection);
    if (!request) {
      if (!search.isRepositoriesLoading) {
        setPendingCollectionOpenId(null);
      }
      return;
    }
    setPendingCollectionOpenId(null);
    void navigate('/workbench', { state: { workbenchSearch: request, localePrompt: true } });
  }, [
    buildCollectionSearchRequest,
    collections.collections,
    navigate,
    pendingCollectionOpenId,
    search.isRepositoriesLoading,
  ]);

  const handleBackToSearch = useCallback(() => {
    setIsEditMode(false);
    clearWorksetEdits();
  }, [clearWorksetEdits]);

  const handleRefreshWorkset = useCallback(() => {
    clearWorksetEdits();
    void refetchSearch();
  }, [clearWorksetEdits, refetchSearch]);

  const headerDisabled = edits.editingRowId !== null;
  const hasSearched = search.activeSearchRequest !== null;

  return (
    <WorkbenchPageView
      hasSearched={hasSearched}
      isEditMode={isEditMode}
      worksetSize={search.worksetSize}
      onChangeWorksetSize={search.onChangeWorksetSize}
      editedRowIds={edits.editedRowIds}
      statusSavingRowIds={edits.statusSavingRowIds}
      diffModal={edits.diffModal}
      onShowDiff={edits.onShowDiff}
      onCloseDiff={edits.onCloseDiff}
      rows={search.rows}
      hasMoreResults={search.hasMoreResults}
      editingRowId={edits.editingRowId}
      editingValue={edits.editingValue}
      onStartEditing={edits.onStartEditing}
      onCancelEditing={edits.onCancelEditing}
      onSaveEditing={edits.onSaveEditing}
      onChangeEditingValue={edits.onChangeEditingValue}
      onChangeStatus={edits.onChangeStatus}
      statusOptions={statusOptions}
      headerDisabled={headerDisabled}
      isSaving={edits.isSaving}
      saveErrorMessage={edits.saveErrorMessage}
      showValidationDialog={edits.pendingValidationSave !== null}
      validationDialogBody={edits.pendingValidationSave?.body ?? ''}
      onConfirmValidationSave={edits.confirmValidationSave}
      onDismissValidationDialog={edits.dismissValidationSave}
      showDiscardDialog={edits.pendingEditingTarget !== null}
      onConfirmDiscardEditing={edits.confirmDiscardEditing}
      onDismissDiscardEditing={edits.dismissDiscardEditing}
      translationInputRef={edits.translationInputRef}
      registerRowRef={edits.registerRowRef}
      searchAttribute={search.searchAttribute}
      searchType={search.searchType}
      searchInputValue={search.searchInputValue}
      onChangeSearchInput={search.onChangeSearchInput}
      onSubmitSearch={search.onSubmitSearch}
      onChangeSearchAttribute={search.onChangeSearchAttribute}
      onChangeSearchType={search.onChangeSearchType}
      onBackToSearch={handleBackToSearch}
      onRefreshWorkset={handleRefreshWorkset}
      repositoryOptions={search.repositoryOptions}
      selectedRepositoryIds={search.selectedRepositoryIds}
      onChangeRepositorySelection={search.onChangeRepositorySelection}
      isRepositoryLoading={search.isRepositoriesLoading}
      repositoryErrorMessage={search.repositoryErrorMessage}
      localeOptions={search.localeOptions}
      selectedLocaleTags={search.selectedLocaleTags}
      onChangeLocaleSelection={search.onChangeLocaleSelection}
      userLocales={userLocales}
      isLimitedTranslator={isLimitedTranslator}
      statusFilter={search.statusFilter}
      includeUsed={search.includeUsed}
      includeUnused={search.includeUnused}
      includeTranslate={search.includeTranslate}
      includeDoNotTranslate={search.includeDoNotTranslate}
      onChangeStatusFilter={search.onChangeStatusFilter}
      onChangeIncludeUsed={search.onChangeIncludeUsed}
      onChangeIncludeUnused={search.onChangeIncludeUnused}
      onChangeIncludeTranslate={search.onChangeIncludeTranslate}
      onChangeIncludeDoNotTranslate={search.onChangeIncludeDoNotTranslate}
      createdBefore={search.createdBefore}
      createdAfter={search.createdAfter}
      onChangeCreatedBefore={search.onChangeCreatedBefore}
      onChangeCreatedAfter={search.onChangeCreatedAfter}
      isSearchLoading={search.isSearchLoading}
      searchErrorMessage={search.searchErrorMessage}
      hydrationModal={hydrationModal}
      onDismissHydrationModal={() => setHydrationModal(null)}
      onRetrySearch={() => {
        void search.refetchSearch();
      }}
      canSearch={search.canSearch}
      activeSearchRequest={search.activeSearchRequest}
      repositories={search.repositories}
      collections={collections.collections}
      activeCollectionId={collections.activeCollectionId}
      activeCollectionName={collections.activeCollection?.name ?? null}
      activeCollectionCount={activeCollectionCount}
      onCreateCollection={collections.createCollection}
      onSelectCollection={collections.selectCollection}
      onRenameCollection={collections.renameCollection}
      onDeleteCollection={collections.deleteCollection}
      onClearCollection={collections.clearActiveCollection}
      onDeleteAllCollections={collections.deleteAllCollections}
      onAddAllToCollection={handleAddAllToCollection}
      onAddToCollection={handleAddToCollection}
      onRemoveFromCollection={handleRemoveFromCollection}
      activeCollectionIds={activeCollectionIds}
      onOpenCollectionSearch={handleOpenCollectionSearch}
      onShareCollection={handleShareCollection}
      shareOverrides={shareOverrides}
      onPrepareShareOverrides={(overrides) => setShareOverrides(overrides)}
    />
  );
}
