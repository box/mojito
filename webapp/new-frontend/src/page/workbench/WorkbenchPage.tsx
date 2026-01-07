import { useCallback, useState } from 'react';

import { useWorkbenchEdits } from './useWorkbenchEdits';
import { useWorkbenchSearch } from './useWorkbenchSearch';
import { WorkbenchPageView } from './WorkbenchPageView';

const statusOptions = ['Accepted', 'To review', 'To translate', 'Rejected'];

export function WorkbenchPage() {
  const [isEditMode, setIsEditMode] = useState(false);

  const search = useWorkbenchSearch({ isEditMode });
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
      onRetrySearch={() => {
        void search.refetchSearch();
      }}
      canSearch={search.canSearch}
    />
  );
}
