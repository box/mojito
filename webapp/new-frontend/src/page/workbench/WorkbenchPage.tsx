import { useCallback, useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

import type { TextUnitSearchRequest } from '../../api/text-units';
import { useWorkbenchEdits } from './useWorkbenchEdits';
import { useWorkbenchSearch } from './useWorkbenchSearch';
import { loadWorkbenchShare } from './workbench-share';
import { WorkbenchPageView } from './WorkbenchPageView';

const statusOptions = ['Accepted', 'To review', 'To translate', 'Rejected'];

export function WorkbenchPage() {
  const [isEditMode, setIsEditMode] = useState(false);
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

  const search = useWorkbenchSearch({
    isEditMode,
    initialSearchRequest: hydratedSearchRequest,
  });

  useEffect(() => {
    if (!shareId) {
      return;
    }
    setShareIdToHydrate(shareId);
    shareSearchParamsRef.current = new URLSearchParams(searchParams);
  }, [shareId, searchParams]);

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
            title: 'Choose a locale',
            body: 'This link intentionally does not set a locale. Pick one in the dropdown to review the text units.',
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
        const nextParams = new URLSearchParams(shareSearchParamsRef.current ?? undefined);
        if (nextParams.has('shareId')) {
          nextParams.delete('shareId');
          setSearchParams(nextParams, { replace: true });
        }
      });
    return () => {
      cancelled = true;
    };
  }, [setSearchParams, shareIdToHydrate]);

  const edits = useWorkbenchEdits({
    apiRows: search.rows,
    canSearch: search.canSearch,
    appliedSearchRequest: search.appliedSearchRequest,
    setIsEditMode,
  });
  const { clearWorksetEdits } = edits;
  const { refetchSearch } = search;

  const handleBackToSearch = useCallback(() => {
    setIsEditMode(false);
    clearWorksetEdits();
  }, [clearWorksetEdits]);

  const handleRefreshWorkset = useCallback(() => {
    clearWorksetEdits();
    void refetchSearch();
  }, [clearWorksetEdits, refetchSearch]);

  const headerDisabled = edits.editingRowId !== null;
  const hasSearched = search.appliedSearchRequest !== null;

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
      appliedSearchRequest={search.appliedSearchRequest}
    />
  );
}
