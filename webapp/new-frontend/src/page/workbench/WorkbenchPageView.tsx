import '../../components/chip-dropdown.css';
import './workbench-page.css';

import { type RefObject, useState } from 'react';

import type { ApiRepository } from '../../api/repositories';
import type { SearchAttribute, SearchType, TextUnitSearchRequest } from '../../api/text-units';
import { ConfirmModal } from '../../components/ConfirmModal';
import { Modal } from '../../components/Modal';
import type { LocaleSelectionOption } from '../../utils/localeSelection';
import type { RepositorySelectionOption } from '../../utils/repositorySelection';
import type {
  StatusFilterValue,
  WorkbenchCollection,
  WorkbenchDiffModalData,
  WorkbenchRow,
  WorkbenchShareOverrides,
} from './workbench-types';
import { DiffModal, WorkbenchBody } from './WorkbenchBody';
import { WorkbenchHeader } from './WorkbenchHeader';
import { WorkbenchShareModal } from './WorkbenchShareModal';
import { WorkbenchWorksetBar } from './WorkbenchWorksetBar';

type Props = {
  hasSearched: boolean;
  worksetSize: number;
  onChangeWorksetSize: (value: number) => void;
  editedRowIds: Set<string>;
  statusSavingRowIds: Set<string>;
  diffModal: WorkbenchDiffModalData | null;
  onShowDiff: (rowId: string) => void;
  onCloseDiff: () => void;
  rows: WorkbenchRow[];
  hasMoreResults: boolean;
  editingRowId: string | null;
  editingValue: string;
  canSaveEditing: boolean;
  onStartEditing: (rowId: string, translation: string | null) => void;
  onCancelEditing: () => void;
  onSaveEditing: () => void;
  onChangeEditingValue: (value: string) => void;
  onChangeStatus: (rowId: string, status: string) => void;
  statusOptions: string[];
  headerDisabled: boolean;
  isSaving: boolean;
  saveErrorMessage: string | null;
  showValidationDialog: boolean;
  validationDialogBody: string;
  onConfirmValidationSave: () => void;
  onDismissValidationDialog: () => void;
  showDiscardDialog: boolean;
  onConfirmDiscardEditing: () => void;
  onDismissDiscardEditing: () => void;
  translationInputRef: RefObject<HTMLTextAreaElement>;
  registerRowRef: (rowId: string, element: HTMLDivElement | null) => void;
  searchAttribute: SearchAttribute;
  searchType: SearchType;
  searchInputValue: string;
  onChangeSearchInput: (value: string) => void;
  onSubmitSearch: () => void;
  onResetWorkbench: () => void;
  onChangeSearchAttribute: (value: SearchAttribute) => void;
  onChangeSearchType: (value: SearchType) => void;
  onRefreshWorkset: () => void;
  repositoryOptions: RepositorySelectionOption[];
  selectedRepositoryIds: number[];
  onChangeRepositorySelection: (next: number[]) => void;
  isRepositoryLoading: boolean;
  repositoryErrorMessage: string | null;
  localeOptions: LocaleSelectionOption[];
  selectedLocaleTags: string[];
  onChangeLocaleSelection: (next: string[]) => void;
  userLocales: string[];
  isLimitedTranslator: boolean;
  statusFilter: StatusFilterValue;
  includeUsed: boolean;
  includeUnused: boolean;
  includeTranslate: boolean;
  includeDoNotTranslate: boolean;
  onChangeStatusFilter: (value: StatusFilterValue) => void;
  onChangeIncludeUsed: (value: boolean) => void;
  onChangeIncludeUnused: (value: boolean) => void;
  onChangeIncludeTranslate: (value: boolean) => void;
  onChangeIncludeDoNotTranslate: (value: boolean) => void;
  createdBefore: string | null;
  createdAfter: string | null;
  onChangeCreatedBefore: (value: string | null) => void;
  onChangeCreatedAfter: (value: string | null) => void;
  translationCreatedBefore: string | null;
  translationCreatedAfter: string | null;
  onChangeTranslationCreatedBefore: (value: string | null) => void;
  onChangeTranslationCreatedAfter: (value: string | null) => void;
  isSearchLoading: boolean;
  searchErrorMessage: string | null;
  hydrationModal?: { title: string; body: string } | null;
  onDismissHydrationModal?: () => void;
  onRetrySearch: () => void;
  canSearch: boolean;
  activeSearchRequest: TextUnitSearchRequest | null;
  repositories: ApiRepository[];
  collections: WorkbenchCollection[];
  activeCollectionId: string | null;
  activeCollectionName: string | null;
  activeCollectionCount: number;
  onCreateCollection: (name?: string) => string | null;
  onSelectCollection: (id: string | null) => void;
  onRenameCollection: (id: string, name: string) => void;
  onDeleteCollection: (id: string) => void;
  onClearCollection: () => void;
  onDeleteAllCollections: () => void;
  onAddAllToCollection: () => void;
  onAddToCollection: (tmTextUnitId: number, repositoryId: number | null) => void;
  onRemoveFromCollection: (tmTextUnitId: number) => void;
  activeCollectionIds: Set<number>;
  onOpenCollectionSearch: (id: string) => void;
  onShareCollection: (id: string) => boolean;
  onCreateReviewProject: (id: string) => void;
  onOpenAiTranslate: (id: string) => void;
  shareOverrides: WorkbenchShareOverrides | null;
  onPrepareShareOverrides: (overrides: WorkbenchShareOverrides | null) => void;
};

function HydrationModal({
  data,
  onClose,
}: {
  data?: { title: string; body: string } | null;
  onClose?: () => void;
}) {
  if (!data) {
    return null;
  }
  return (
    <Modal open size="md" ariaLabel={data.title} onClose={onClose} closeOnBackdrop>
      <div className="modal__header">
        <div className="modal__title">{data.title}</div>
      </div>
      <div className="modal__body">{data.body}</div>
      <div className="modal__actions">
        <button type="button" className="modal__button modal__button--primary" onClick={onClose}>
          Got it
        </button>
      </div>
    </Modal>
  );
}

export function WorkbenchPageView({
  hasSearched,
  worksetSize,
  onChangeWorksetSize,
  editedRowIds,
  statusSavingRowIds,
  diffModal,
  onShowDiff,
  onCloseDiff,
  rows,
  hasMoreResults,
  editingRowId,
  editingValue,
  canSaveEditing,
  onStartEditing,
  onCancelEditing,
  onSaveEditing,
  onChangeEditingValue,
  onChangeStatus,
  statusOptions,
  headerDisabled,
  isSaving,
  saveErrorMessage,
  showValidationDialog,
  validationDialogBody,
  onConfirmValidationSave,
  onDismissValidationDialog,
  showDiscardDialog,
  onConfirmDiscardEditing,
  onDismissDiscardEditing,
  translationInputRef,
  registerRowRef,
  searchAttribute,
  searchType,
  searchInputValue,
  onChangeSearchInput,
  onSubmitSearch,
  onResetWorkbench,
  onChangeSearchAttribute,
  onChangeSearchType,
  onRefreshWorkset,
  repositoryOptions,
  selectedRepositoryIds,
  onChangeRepositorySelection,
  isRepositoryLoading,
  repositoryErrorMessage,
  localeOptions,
  selectedLocaleTags,
  onChangeLocaleSelection,
  userLocales,
  isLimitedTranslator,
  statusFilter,
  includeUsed,
  includeUnused,
  includeTranslate,
  includeDoNotTranslate,
  onChangeStatusFilter,
  onChangeIncludeUsed,
  onChangeIncludeUnused,
  onChangeIncludeTranslate,
  onChangeIncludeDoNotTranslate,
  createdBefore,
  createdAfter,
  onChangeCreatedBefore,
  onChangeCreatedAfter,
  translationCreatedBefore,
  translationCreatedAfter,
  onChangeTranslationCreatedBefore,
  onChangeTranslationCreatedAfter,
  isSearchLoading,
  searchErrorMessage,
  hydrationModal,
  onDismissHydrationModal,
  onRetrySearch,
  canSearch,
  activeSearchRequest,
  repositories,
  collections,
  activeCollectionId,
  activeCollectionName,
  activeCollectionCount,
  onCreateCollection,
  onSelectCollection,
  onRenameCollection,
  onDeleteCollection,
  onClearCollection,
  onDeleteAllCollections,
  onAddAllToCollection,
  onAddToCollection,
  onRemoveFromCollection,
  activeCollectionIds,
  onOpenCollectionSearch,
  onShareCollection,
  onCreateReviewProject,
  onOpenAiTranslate,
  shareOverrides,
  onPrepareShareOverrides,
}: Props) {
  const [isShareModalOpen, setIsShareModalOpen] = useState(false);
  const editedCount = editedRowIds.size;
  const rowCount = rows.length;
  return (
    <div className="workbench-page">
      <WorkbenchHeader
        disabled={headerDisabled}
        worksetSize={worksetSize}
        onChangeWorksetSize={onChangeWorksetSize}
        repositoryOptions={repositoryOptions}
        selectedRepositoryIds={selectedRepositoryIds}
        onChangeRepositorySelection={onChangeRepositorySelection}
        isRepositoryLoading={isRepositoryLoading}
        localeOptions={localeOptions}
        selectedLocaleTags={selectedLocaleTags}
        onChangeLocaleSelection={onChangeLocaleSelection}
        userLocales={userLocales}
        isLimitedTranslator={isLimitedTranslator}
        searchAttribute={searchAttribute}
        searchType={searchType}
        onChangeSearchAttribute={onChangeSearchAttribute}
        onChangeSearchType={onChangeSearchType}
        searchInputValue={searchInputValue}
        onChangeSearchInput={onChangeSearchInput}
        onSubmitSearch={onSubmitSearch}
        onResetWorkbench={onResetWorkbench}
        statusFilter={statusFilter}
        includeUsed={includeUsed}
        includeUnused={includeUnused}
        includeTranslate={includeTranslate}
        includeDoNotTranslate={includeDoNotTranslate}
        onChangeStatusFilter={onChangeStatusFilter}
        onChangeIncludeUsed={onChangeIncludeUsed}
        onChangeIncludeUnused={onChangeIncludeUnused}
        onChangeIncludeTranslate={onChangeIncludeTranslate}
        onChangeIncludeDoNotTranslate={onChangeIncludeDoNotTranslate}
        createdBefore={createdBefore}
        createdAfter={createdAfter}
        onChangeCreatedBefore={onChangeCreatedBefore}
        onChangeCreatedAfter={onChangeCreatedAfter}
        translationCreatedBefore={translationCreatedBefore}
        translationCreatedAfter={translationCreatedAfter}
        onChangeTranslationCreatedBefore={onChangeTranslationCreatedBefore}
        onChangeTranslationCreatedAfter={onChangeTranslationCreatedAfter}
      />
      <WorkbenchWorksetBar
        disabled={headerDisabled}
        isSearchLoading={isSearchLoading}
        hasSearched={hasSearched}
        rowCount={rowCount}
        hasMoreResults={hasMoreResults}
        worksetSize={worksetSize}
        onChangeWorksetSize={onChangeWorksetSize}
        editedCount={editedCount}
        onRefreshWorkset={onRefreshWorkset}
        onResetWorkbench={onResetWorkbench}
        collections={collections}
        activeCollectionId={activeCollectionId}
        activeCollectionName={activeCollectionName}
        activeCollectionCount={activeCollectionCount}
        onCreateCollection={onCreateCollection}
        onSelectCollection={onSelectCollection}
        onRenameCollection={onRenameCollection}
        onDeleteCollection={onDeleteCollection}
        onClearCollection={onClearCollection}
        onDeleteAllCollections={onDeleteAllCollections}
        onAddAllToCollection={onAddAllToCollection}
        onOpenCollectionSearch={onOpenCollectionSearch}
        onShareCollection={(id) => {
          const ok = onShareCollection(id);
          if (ok) {
            setIsShareModalOpen(true);
          }
          return ok;
        }}
        onCreateReviewProject={onCreateReviewProject}
        onOpenAiTranslate={onOpenAiTranslate}
        onOpenShareModal={() => {
          onPrepareShareOverrides(null);
          setIsShareModalOpen(true);
        }}
      />
      <WorkbenchBody
        rows={rows}
        editingRowId={editingRowId}
        editingValue={editingValue}
        editedRowIds={editedRowIds}
        statusSavingRowIds={statusSavingRowIds}
        onShowDiff={onShowDiff}
        onStartEditing={onStartEditing}
        onCancelEditing={onCancelEditing}
        onSaveEditing={onSaveEditing}
        canSaveEditing={canSaveEditing}
        onChangeEditingValue={onChangeEditingValue}
        onChangeStatus={onChangeStatus}
        statusOptions={statusOptions}
        translationInputRef={translationInputRef}
        registerRowRef={registerRowRef}
        isSaving={isSaving}
        saveErrorMessage={saveErrorMessage}
        isRepositoryLoading={isRepositoryLoading}
        repositoryErrorMessage={repositoryErrorMessage}
        canSearch={canSearch}
        isSearchLoading={isSearchLoading}
        searchErrorMessage={searchErrorMessage}
        onRetrySearch={onRetrySearch}
        hasSearched={hasSearched}
        activeSearchRequest={activeSearchRequest}
        repositories={repositories}
        onAddToCollection={onAddToCollection}
        onRemoveFromCollection={onRemoveFromCollection}
        activeCollectionIds={activeCollectionIds}
        activeCollectionName={activeCollectionName}
      />
      <HydrationModal data={hydrationModal} onClose={onDismissHydrationModal} />
      <ConfirmModal
        open={showValidationDialog}
        title="Translation check failed"
        body={validationDialogBody}
        confirmLabel="Save anyway"
        cancelLabel="Keep editing"
        onConfirm={onConfirmValidationSave}
        onCancel={onDismissValidationDialog}
      />
      <ConfirmModal
        open={showDiscardDialog}
        title="Unsaved translation"
        body="You have unsaved edits. Do you want to discard them?"
        confirmLabel="Discard & switch"
        cancelLabel="Keep editing"
        onConfirm={onConfirmDiscardEditing}
        onCancel={onDismissDiscardEditing}
      />
      <DiffModal data={diffModal} onClose={onCloseDiff} />
      <WorkbenchShareModal
        open={isShareModalOpen}
        onClose={() => setIsShareModalOpen(false)}
        searchRequest={shareOverrides?.searchRequest ?? activeSearchRequest}
        rows={rows}
        availableLocales={selectedLocaleTags}
        overrides={shareOverrides ?? undefined}
      />
    </div>
  );
}
