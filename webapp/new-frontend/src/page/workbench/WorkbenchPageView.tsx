import '../../components/chip-dropdown.css';
import './workbench-page.css';

import { type RefObject, useState } from 'react';

import type { ApiRepository } from '../../api/repositories';
import type { SearchAttribute, SearchType, TextUnitSearchRequest } from '../../api/text-units';
import { ConfirmModal } from '../../components/ConfirmModal';
import { Modal } from '../../components/Modal';
import type {
  LocaleOption,
  RepositoryOption,
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
  isEditMode: boolean;
  worksetSize: number;
  onChangeWorksetSize: (value: number) => void;
  editedRowIds: Set<string>;
  statusSavingRowIds: Set<string>;
  diffModal: WorkbenchDiffModalData | null;
  onShowDiff: (rowId: string) => void;
  onCloseDiff: () => void;
  rows: WorkbenchRow[];
  editingRowId: string | null;
  editingValue: string;
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
  onChangeSearchAttribute: (value: SearchAttribute) => void;
  onChangeSearchType: (value: SearchType) => void;
  onBackToSearch: () => void;
  onRefreshWorkset: () => void;
  repositoryOptions: RepositoryOption[];
  selectedRepositoryIds: number[];
  onChangeRepositorySelection: (next: number[]) => void;
  isRepositoryLoading: boolean;
  repositoryErrorMessage: string | null;
  localeOptions: LocaleOption[];
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
  isEditMode,
  worksetSize,
  onChangeWorksetSize,
  editedRowIds,
  statusSavingRowIds,
  diffModal,
  onShowDiff,
  onCloseDiff,
  rows,
  editingRowId,
  editingValue,
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
  onChangeSearchAttribute,
  onChangeSearchType,
  onBackToSearch,
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
        isEditMode={isEditMode}
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
      />
      <WorkbenchWorksetBar
        disabled={headerDisabled}
        isEditMode={isEditMode}
        isSearchLoading={isSearchLoading}
        hasSearched={hasSearched}
        rowCount={rowCount}
        worksetSize={worksetSize}
        onChangeWorksetSize={onChangeWorksetSize}
        editedCount={editedCount}
        onBackToSearch={onBackToSearch}
        onRefreshWorkset={onRefreshWorkset}
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
