import '../../components/chip-dropdown.css';
import './workbench-page.css';

import { type RefObject, useEffect, useRef, useState } from 'react';

import type { SearchAttribute, SearchType } from '../../api/text-units';
import { AutoTextarea } from '../../components/AutoTextarea';
import { MultiSelectChip } from '../../components/MultiSelectChip';
import { UnsavedChangesModal } from '../../components/UnsavedChangesModal';
import { isRtlLocale } from '../../utils/localeDirection';

export type WorkbenchRow = {
  id: string;
  textUnitName: string;
  repositoryName: string;
  assetPath: string | null;
  locale: string;
  source: string;
  translation: string | null;
  status: string;
  comment: string | null;
  tmTextUnitId: number;
};

export type RepositoryOption = { id: number; name: string };
export type LocaleOption = { tag: string; label: string };

export type StatusFilterValue =
  | 'ALL'
  | 'FOR_TRANSLATION'
  | 'REVIEW_NEEDED'
  | 'TRANSLATED'
  | 'TRANSLATED_AND_NOT_REJECTED'
  | 'UNTRANSLATED'
  | 'REJECTED'
  | 'NOT_REJECTED'
  | 'APPROVED_AND_NOT_REJECTED';

type SearchAttributeOption = { value: SearchAttribute; label: string };
type SearchTypeOption = { value: SearchType; label: string; helper?: string };
type StatusFilterOption = { value: StatusFilterValue; label: string };

const searchAttributeOptions: SearchAttributeOption[] = [
  { value: 'target', label: 'Translation' },
  { value: 'source', label: 'Source' },
  { value: 'stringId', label: 'String ID' },
  { value: 'asset', label: 'Asset path' },
  { value: 'pluralFormOther', label: 'Plural (other)' },
  { value: 'tmTextUnitIds', label: 'TM TextUnit IDs' },
];

const searchTypeOptions: SearchTypeOption[] = [
  { value: 'exact', label: 'Exact match', helper: 'Full string' },
  { value: 'contains', label: 'Contains', helper: 'Case-sensitive' },
  { value: 'ilike', label: 'ILIKE', helper: 'Case-insensitive' },
];

const statusFilterOptions: StatusFilterOption[] = [
  { value: 'ALL', label: 'All' },
  { value: 'TRANSLATED', label: 'Translated' },
  { value: 'UNTRANSLATED', label: 'Untranslated' },
  { value: 'FOR_TRANSLATION', label: 'To translate' },
  { value: 'REVIEW_NEEDED', label: 'To review' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'NOT_REJECTED', label: 'Not rejected' },
  { value: 'APPROVED_AND_NOT_REJECTED', label: 'Approved' },
];

type Props = {
  rows: WorkbenchRow[];
  editingRowId: string | null;
  editingValue: string;
  onStartEditing: (rowId: string, translation: string | null) => void;
  onCancelEditing: () => void;
  onSaveEditing: () => void;
  onChangeEditingValue: (value: string) => void;
  onChangeStatus: (rowId: string, status: string) => void;
  statusOptions: string[];
  showDiscardDialog: boolean;
  onConfirmDiscardEditing: () => void;
  onDismissDiscardEditing: () => void;
  translationInputRef: RefObject<HTMLTextAreaElement | null>;
  registerRowRef: (rowId: string, element: HTMLDivElement | null) => void;
  searchAttribute: SearchAttribute;
  searchType: SearchType;
  searchInputValue: string;
  onChangeSearchInput: (value: string) => void;
  onSubmitSearch: () => void;
  onChangeSearchAttribute: (value: SearchAttribute) => void;
  onChangeSearchType: (value: SearchType) => void;
  isSearchDirty: boolean;
  totalRowCount: number;
  displayRowCount: number;
  repositoryOptions: RepositoryOption[];
  selectedRepositoryIds: number[];
  onChangeRepositorySelection: (next: number[]) => void;
  isRepositoryLoading: boolean;
  repositoryErrorMessage: string | null;
  localeOptions: LocaleOption[];
  selectedLocaleTags: string[];
  onChangeLocaleSelection: (next: string[]) => void;
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
  onRetrySearch: () => void;
  canSearch: boolean;
};

export function WorkbenchPageView({
  rows,
  editingRowId,
  editingValue,
  onStartEditing,
  onCancelEditing,
  onSaveEditing,
  onChangeEditingValue,
  onChangeStatus,
  statusOptions,
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
  isSearchDirty,
  totalRowCount,
  displayRowCount,
  repositoryOptions,
  selectedRepositoryIds,
  onChangeRepositorySelection,
  isRepositoryLoading,
  repositoryErrorMessage,
  localeOptions,
  selectedLocaleTags,
  onChangeLocaleSelection,
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
  onRetrySearch,
  canSearch,
}: Props) {
  const [isShareModalOpen, setIsShareModalOpen] = useState(false);
  return (
    <div className="workbench-page">
      <WorkbenchHeader
        repositoryOptions={repositoryOptions}
        selectedRepositoryIds={selectedRepositoryIds}
        onChangeRepositorySelection={onChangeRepositorySelection}
        isRepositoryLoading={isRepositoryLoading}
        localeOptions={localeOptions}
        selectedLocaleTags={selectedLocaleTags}
        onChangeLocaleSelection={onChangeLocaleSelection}
        searchAttribute={searchAttribute}
        searchType={searchType}
        onChangeSearchAttribute={onChangeSearchAttribute}
        onChangeSearchType={onChangeSearchType}
        searchInputValue={searchInputValue}
        onChangeSearchInput={onChangeSearchInput}
        onSubmitSearch={onSubmitSearch}
        isSearchDirty={isSearchDirty}
        canSearch={canSearch}
        isSearchLoading={isSearchLoading}
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
        onOpenShareModal={() => setIsShareModalOpen(true)}
      />
      <WorkbenchBody
        rows={rows}
        editingRowId={editingRowId}
        editingValue={editingValue}
        onStartEditing={onStartEditing}
        onCancelEditing={onCancelEditing}
        onSaveEditing={onSaveEditing}
        onChangeEditingValue={onChangeEditingValue}
        onChangeStatus={onChangeStatus}
        statusOptions={statusOptions}
        translationInputRef={translationInputRef}
        registerRowRef={registerRowRef}
        isRepositoryLoading={isRepositoryLoading}
        repositoryErrorMessage={repositoryErrorMessage}
        canSearch={canSearch}
        isSearchLoading={isSearchLoading}
        searchErrorMessage={searchErrorMessage}
        onRetrySearch={onRetrySearch}
        displayRowCount={displayRowCount}
        totalRowCount={totalRowCount}
      />
      <UnsavedChangesModal
        open={showDiscardDialog}
        title="Unsaved translation"
        body="You have unsaved edits. Do you want to discard them?"
        confirmLabel="Discard & switch"
        cancelLabel="Keep editing"
        onConfirm={onConfirmDiscardEditing}
        onCancel={onDismissDiscardEditing}
      />
      <ShareLinkModal open={isShareModalOpen} onClose={() => setIsShareModalOpen(false)} />
    </div>
  );
}

type WorkbenchHeaderProps = {
  repositoryOptions: RepositoryOption[];
  selectedRepositoryIds: number[];
  onChangeRepositorySelection: (next: number[]) => void;
  isRepositoryLoading: boolean;
  localeOptions: LocaleOption[];
  selectedLocaleTags: string[];
  onChangeLocaleSelection: (next: string[]) => void;
  searchAttribute: SearchAttribute;
  searchType: SearchType;
  onChangeSearchAttribute: (value: SearchAttribute) => void;
  onChangeSearchType: (value: SearchType) => void;
  searchInputValue: string;
  onChangeSearchInput: (value: string) => void;
  onSubmitSearch: () => void;
  isSearchDirty: boolean;
  canSearch: boolean;
  isSearchLoading: boolean;
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
  onOpenShareModal: () => void;
};

function WorkbenchHeader({
  repositoryOptions,
  selectedRepositoryIds,
  onChangeRepositorySelection,
  isRepositoryLoading,
  localeOptions,
  selectedLocaleTags,
  onChangeLocaleSelection,
  searchAttribute,
  searchType,
  onChangeSearchAttribute,
  onChangeSearchType,
  searchInputValue,
  onChangeSearchInput,
  onSubmitSearch,
  isSearchDirty,
  canSearch,
  isSearchLoading,
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
  onOpenShareModal,
}: WorkbenchHeaderProps) {
  const repositoryMultiOptions = repositoryOptions.map((option) => ({
    value: option.id,
    label: option.name,
  }));
  const localeMultiOptions = localeOptions.map((option) => ({
    value: option.tag,
    label: option.label,
  }));

  return (
    <div className="workbench-page__header">
      <MultiSelectChip
        className="workbench-chip-dropdown"
        label="Repositories"
        options={repositoryMultiOptions}
        selectedValues={selectedRepositoryIds}
        onChange={onChangeRepositorySelection}
        placeholder={
          repositoryOptions.length ? 'Select repositories' : 'No repositories available'
        }
        emptyOptionsLabel={
          isRepositoryLoading ? 'Loading repositories…' : 'No repositories available'
        }
      />
      <MultiSelectChip
        className="workbench-chip-dropdown workbench-chip-dropdown--locale"
        label="Locales"
        options={localeMultiOptions}
        selectedValues={selectedLocaleTags}
        onChange={onChangeLocaleSelection}
        placeholder={localeOptions.length ? 'Select locales' : 'No locales available'}
        emptyOptionsLabel="No locales available"
      />
      <SearchModeChip
        searchAttribute={searchAttribute}
        searchType={searchType}
        onChangeAttribute={onChangeSearchAttribute}
        onChangeType={onChangeSearchType}
      />
      <SearchForm
        value={searchInputValue}
        onChange={onChangeSearchInput}
        onSubmit={onSubmitSearch}
        disabled={!isSearchDirty || !canSearch || isSearchLoading}
      />
      <SearchFilter
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
      />
      <SearchDateFilter
        createdBefore={createdBefore}
        createdAfter={createdAfter}
        onChangeCreatedBefore={onChangeCreatedBefore}
        onChangeCreatedAfter={onChangeCreatedAfter}
      />
      <SearchShareButton onClick={onOpenShareModal} />
    </div>
  );
}

type WorkbenchSearchFormProps = {
  value: string;
  onChange: (value: string) => void;
  onSubmit: () => void;
  disabled: boolean;
};

function SearchForm({ value, onChange, onSubmit, disabled }: WorkbenchSearchFormProps) {
  return (
    <form
      className="workbench-search"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit();
      }}
    >
      <input
        id="workbench-search-input"
        className="workbench-search__input"
        type="text"
        value={value}
        placeholder="Search source, translation, asset, or ID"
        onChange={(event) => onChange(event.target.value)}
      />
      <button type="submit" className="workbench-search__submit" disabled={disabled}>
        Search
      </button>
    </form>
  );
}

function SearchShareButton({ onClick }: { onClick: () => void }) {
  return (
    <button type="button" className="workbench-share-button" onClick={onClick}>
      Share
    </button>
  );
}

type WorkbenchBodyProps = {
  rows: WorkbenchRow[];
  editingRowId: string | null;
  editingValue: string;
  onStartEditing: (rowId: string, translation: string | null) => void;
  onCancelEditing: () => void;
  onSaveEditing: () => void;
  onChangeEditingValue: (value: string) => void;
  onChangeStatus: (rowId: string, status: string) => void;
  statusOptions: string[];
  translationInputRef: RefObject<HTMLTextAreaElement | null>;
  registerRowRef: (rowId: string, element: HTMLDivElement | null) => void;
  isRepositoryLoading: boolean;
  repositoryErrorMessage: string | null;
  canSearch: boolean;
  isSearchLoading: boolean;
  searchErrorMessage: string | null;
  onRetrySearch: () => void;
  displayRowCount: number;
  totalRowCount: number;
};

function WorkbenchBody({
  rows,
  editingRowId,
  editingValue,
  onStartEditing,
  onCancelEditing,
  onSaveEditing,
  onChangeEditingValue,
  onChangeStatus,
  statusOptions,
  translationInputRef,
  registerRowRef,
  isRepositoryLoading,
  repositoryErrorMessage,
  canSearch,
  isSearchLoading,
  searchErrorMessage,
  onRetrySearch,
  displayRowCount,
  totalRowCount,
}: WorkbenchBodyProps) {
  return (
    <div className="workbench-page__body">
      <WorkbenchStatusBanner
        isRepositoryLoading={isRepositoryLoading}
        repositoryErrorMessage={repositoryErrorMessage}
        canSearch={canSearch}
        isSearchLoading={isSearchLoading}
        searchErrorMessage={searchErrorMessage}
        onRetrySearch={onRetrySearch}
        displayRowCount={displayRowCount}
        totalRowCount={totalRowCount}
      />
      <div className="workbench-page__table-header">
        <div className="workbench-page__table-header-cell">Text unit (id, asset, repository)</div>
        <div className="workbench-page__table-header-cell workbench-page__table-header-cell--source">
          Source &amp; comment
        </div>
        <div className="workbench-page__table-header-cell workbench-page__table-header-cell--translation">
          Translation
        </div>
        <div className="workbench-page__table-header-cell workbench-page__table-header-cell--locale">
          Locale
        </div>
        <div className="workbench-page__table-header-cell workbench-page__table-header-cell--status">
          Status
        </div>
      </div>
      <div className="workbench-page__rows">
        {rows.map((row) => {
          const isEditing = editingRowId === row.id;
          const translationValue = isEditing ? editingValue : (row.translation ?? '');
          const translationDirection = isRtlLocale(row.locale) ? 'rtl' : 'ltr';
          const translationLocale = row.locale;
          const translationStyle = isEditing ? undefined : { resize: 'none' as const };

          return (
            <div
              key={row.id}
              className="workbench-page__row"
              ref={(element) => registerRowRef(row.id, element)}
            >
              <div className="workbench-page__cell workbench-page__cell--meta">
                <span className="workbench-page__meta-id">{row.textUnitName}</span>
                <span className="workbench-page__meta-asset">{row.assetPath ?? ''}</span>
                <span className="workbench-page__repo-name">{row.repositoryName}</span>
              </div>
              <div className="workbench-page__cell workbench-page__cell--source">
                <div className="workbench-page__text-block workbench-page__source-text">
                  {row.source ?? ''}
                </div>
                <div className="workbench-page__text-block workbench-page__source-comment">
                  {row.comment ?? ''}
                </div>
              </div>
              <div className="workbench-page__cell workbench-page__cell--translation" data-editing={isEditing ? 'true' : undefined}>
                <AutoTextarea
                  className="workbench-page__translation-input"
                  value={translationValue}
                  onFocus={() => {
                    if (!isEditing) {
                      onStartEditing(row.id, row.translation);
                    }
                  }}
                  onChange={
                    isEditing ? (event) => onChangeEditingValue(event.target.value) : undefined
                  }
                  readOnly={!isEditing}
                  ref={isEditing ? translationInputRef : undefined}
                  lang={translationLocale}
                  dir={translationDirection}
                  style={translationStyle}
                />
                {isEditing ? (
                  <div className="workbench-page__translation-actions">
                    <button
                      type="button"
                      className="workbench-page__translation-button workbench-page__translation-button--primary"
                      onClick={(event) => {
                        event.stopPropagation();
                        onSaveEditing();
                      }}
                    >
                      Accept
                    </button>
                    <button
                      type="button"
                      className="workbench-page__translation-button"
                      onClick={(event) => {
                        event.stopPropagation();
                        onCancelEditing();
                      }}
                    >
                      Cancel
                    </button>
                  </div>
                ) : null}
              </div>
              <div className="workbench-page__cell workbench-page__cell--locale">
                <span className="workbench-page__locale-pill">{row.locale}</span>
              </div>
              <div className="workbench-page__cell workbench-page__cell--status">
                <select
                  className="workbench-page__status-select"
                  value={row.status}
                  aria-label="Translation status"
                  onChange={(event) => onChangeStatus(row.id, event.target.value)}
                >
                  {statusOptions.map((statusOption) => (
                    <option key={statusOption} value={statusOption}>
                      {statusOption}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

type WorkbenchStatusBannerProps = {
  isRepositoryLoading: boolean;
  repositoryErrorMessage: string | null;
  canSearch: boolean;
  isSearchLoading: boolean;
  searchErrorMessage: string | null;
  onRetrySearch: () => void;
  displayRowCount: number;
  totalRowCount: number;
};

function WorkbenchStatusBanner({
  isRepositoryLoading,
  repositoryErrorMessage,
  canSearch,
  isSearchLoading,
  searchErrorMessage,
  onRetrySearch,
  displayRowCount,
  totalRowCount,
}: WorkbenchStatusBannerProps) {
  const renderStatus = () => {
    if (isRepositoryLoading) {
      return <span>Loading repositories…</span>;
    }
    if (repositoryErrorMessage) {
      return <span>{repositoryErrorMessage}</span>;
    }
    if (!canSearch) {
      return <span>Select at least one repository and locale to start searching.</span>;
    }
    if (isSearchLoading) {
      return <span>Loading results…</span>;
    }
    if (searchErrorMessage) {
      return (
        <>
          <span>{searchErrorMessage}</span>
          <button type="button" className="workbench-status__retry" onClick={onRetrySearch}>
            Retry
          </button>
        </>
      );
    }
    if (!displayRowCount) {
      return <span>No results for this query.</span>;
    }
    const countLabel = totalRowCount > displayRowCount
      ? `${displayRowCount} of ${totalRowCount}`
      : `${displayRowCount}`;
    return <span>{`Showing ${countLabel} text units.`}</span>;
  };

  const content = renderStatus();
  return content ? <div className="workbench-page__status">{content}</div> : null;
}

type ShareLinkModalProps = {
  open: boolean;
  onClose: () => void;
};

function ShareLinkModal({ open, onClose }: ShareLinkModalProps) {
  if (!open) {
    return null;
  }

  return (
    <div className="workbench-share-modal" role="dialog" aria-modal="true" aria-label="Share workbench">
      <div className="workbench-share-modal__backdrop" onClick={onClose} aria-hidden="true" />
      <div className="workbench-share-modal__card">
        <div className="workbench-share-modal__title">Share this view</div>
        <div className="workbench-share-modal__body">
          This is a placeholder for the upcoming sharing workflow.
        </div>
        <button type="button" className="workbench-share-modal__close" onClick={onClose}>
          Close
        </button>
      </div>
    </div>
  );
}

type SearchModeChipProps = {
  searchAttribute: SearchAttribute;
  searchType: SearchType;
  onChangeAttribute: (value: SearchAttribute) => void;
  onChangeType: (value: SearchType) => void;
};

function SearchModeChip({
  searchAttribute,
  searchType,
  onChangeAttribute,
  onChangeType,
}: SearchModeChipProps) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!isOpen) {
      return;
    }
    const handlePointerDown = (event: PointerEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    window.addEventListener('pointerdown', handlePointerDown);
    return () => window.removeEventListener('pointerdown', handlePointerDown);
  }, [isOpen]);

  const attributeLabel =
    searchAttributeOptions.find((option) => option.value === searchAttribute)?.label ?? 'Attribute';
  const typeLabel =
    searchTypeOptions.find((option) => option.value === searchType)?.label ?? 'Match';
  const summary = `${attributeLabel} · ${typeLabel}`;

  return (
    <div className="chip-dropdown" ref={containerRef}>
      <button
        type="button"
        className="chip-dropdown__button workbench-searchmode__button"
        onClick={() => setIsOpen((previous) => !previous)}
        aria-expanded={isOpen}
      >
        <span className="chip-dropdown__summary">{summary}</span>
        <span className="chip-dropdown__chevron" aria-hidden="true" />
      </button>
      {isOpen ? (
        <div className="chip-dropdown__panel workbench-searchmode__panel" role="menu">
          <div className="workbench-searchmode__section">
            <div className="workbench-searchmode__label">Search attribute</div>
            <div className="workbench-searchmode__list">
              {searchAttributeOptions.map((option) => (
                <button
                  type="button"
                  key={option.value}
                  className={`workbench-searchmode__option${
                    option.value === searchAttribute ? ' is-active' : ''
                  }`}
                  onClick={() => onChangeAttribute(option.value)}
                >
                  <span>{option.label}</span>
                  {option.helper ? (
                    <span className="workbench-searchmode__helper">{option.helper}</span>
                  ) : null}
                </button>
              ))}
            </div>
          </div>
          <div className="workbench-searchmode__section">
            <div className="workbench-searchmode__label">Match type</div>
            <div className="workbench-searchmode__list">
              {searchTypeOptions.map((option) => (
                <button
                  type="button"
                  key={option.value}
                  className={`workbench-searchmode__option${
                    option.value === searchType ? ' is-active' : ''
                  }`}
                  onClick={() => onChangeType(option.value)}
                >
                  <span>{option.label}</span>
                  {option.helper ? (
                    <span className="workbench-searchmode__helper">{option.helper}</span>
                  ) : null}
                </button>
              ))}
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

type FilterChipProps = {
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
};

function SearchFilter({
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
}: FilterChipProps) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!isOpen) {
      return;
    }
    const handlePointerDown = (event: PointerEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    window.addEventListener('pointerdown', handlePointerDown);
    return () => window.removeEventListener('pointerdown', handlePointerDown);
  }, [isOpen]);

  const statusLabel =
    statusFilterOptions.find((option) => option.value === statusFilter)?.label ?? 'All statuses';
  const summarizeToggle = (label: string, yes: boolean, no: boolean) => {
    if (yes && no) {
      return `${label}: Yes & No`;
    }
    if (yes) {
      return `${label}: Yes`;
    }
    return `${label}: No`;
  };

  const usedLabel = summarizeToggle('Use', includeUsed, includeUnused);
  const translateLabel = summarizeToggle('Translate', includeTranslate, includeDoNotTranslate);
  const summaryParts = [statusLabel];
  if (includeUnused || !includeUsed) {
    summaryParts.push(usedLabel);
  }
  if (!(includeTranslate && includeDoNotTranslate)) {
    summaryParts.push(translateLabel);
  }
  const summary = summaryParts.join(' · ');

  return (
    <div className="chip-dropdown" ref={containerRef} data-align="right">
      <button
        type="button"
        className="chip-dropdown__button workbench-filterchip__button"
        onClick={() => setIsOpen((previous) => !previous)}
        aria-expanded={isOpen}
      >
        <span className="chip-dropdown__summary">{summary}</span>
        <span className="chip-dropdown__chevron" aria-hidden="true" />
      </button>
      {isOpen ? (
        <div className="chip-dropdown__panel workbench-filterchip__panel" role="menu">
          <div className="workbench-searchmode__section">
            <div className="workbench-searchmode__label">Status</div>
            <div className="workbench-searchmode__list">
              {statusFilterOptions.map((option) => (
                <button
                  type="button"
                  key={option.value}
                  className={`workbench-searchmode__option${
                    option.value === statusFilter ? ' is-active' : ''
                  }`}
                  onClick={() => onChangeStatusFilter(option.value)}
                >
                  <span>{option.label}</span>
                </button>
              ))}
            </div>
          </div>
          <div className="workbench-searchmode__section">
            <div className="workbench-searchmode__label">Used</div>
            <div className="workbench-searchmode__list">
              <label className="workbench-filterchip__checkbox">
                <input
                  type="checkbox"
                  checked={includeUsed}
                  onChange={() => {
                    if (includeUsed && !includeUnused) {
                      return;
                    }
                    onChangeIncludeUsed(!includeUsed);
                  }}
                />
                <span>Yes</span>
              </label>
              <label className="workbench-filterchip__checkbox">
                <input
                  type="checkbox"
                  checked={includeUnused}
                  onChange={() => {
                    if (includeUnused && !includeUsed) {
                      return;
                    }
                    onChangeIncludeUnused(!includeUnused);
                  }}
                />
                <span>No</span>
              </label>
            </div>
          </div>
          <div className="workbench-searchmode__section">
            <div className="workbench-searchmode__label">Translate</div>
            <div className="workbench-searchmode__list">
              <label className="workbench-filterchip__checkbox">
                <input
                  type="checkbox"
                  checked={includeTranslate}
                  onChange={() => {
                    if (includeTranslate && !includeDoNotTranslate) {
                      return;
                    }
                    onChangeIncludeTranslate(!includeTranslate);
                  }}
                />
                <span>Yes</span>
              </label>
              <label className="workbench-filterchip__checkbox">
                <input
                  type="checkbox"
                  checked={includeDoNotTranslate}
                  onChange={() => {
                    if (includeDoNotTranslate && !includeTranslate) {
                      return;
                    }
                    onChangeIncludeDoNotTranslate(!includeDoNotTranslate);
                  }}
                />
                <span>No</span>
              </label>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

type DateFiltersChipProps = {
  createdBefore: string | null;
  createdAfter: string | null;
  onChangeCreatedBefore: (value: string | null) => void;
  onChangeCreatedAfter: (value: string | null) => void;
};

function SearchDateFilter({
  createdBefore,
  createdAfter,
  onChangeCreatedBefore,
  onChangeCreatedAfter,
}: DateFiltersChipProps) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!isOpen) {
      return;
    }
    const handlePointerDown = (event: PointerEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    window.addEventListener('pointerdown', handlePointerDown);
    return () => window.removeEventListener('pointerdown', handlePointerDown);
  }, [isOpen]);

  const summary = (() => {
    if (!createdBefore && !createdAfter) {
      return 'Created: Any time';
    }
    if (createdBefore && createdAfter) {
      return `Created: ${formatDateTime(createdAfter)} – ${formatDateTime(createdBefore)}`;
    }
    if (createdBefore) {
      return `Created before ${formatDateTime(createdBefore)}`;
    }
    return `Created after ${formatDateTime(createdAfter as string)}`;
  })();

  const quickRanges: Array<{
    label: string;
    getRange: () => { after: string | null; before: string | null };
  }> = [
    { label: 'Last 5 min', getRange: () => ({ after: minutesAgoIso(5), before: null }) },
    { label: 'Last 10 min', getRange: () => ({ after: minutesAgoIso(10), before: null }) },
    { label: 'Last hour', getRange: () => ({ after: minutesAgoIso(60), before: null }) },
    { label: 'Today', getRange: () => ({ after: startOfTodayIso(), before: null }) },
    {
      label: 'Yesterday',
      getRange: () => ({ after: startOfYesterdayIso(), before: startOfTodayIso() }),
    },
    { label: 'This week', getRange: () => ({ after: startOfWeekIso(), before: null }) },
  ];

  return (
    <div className="chip-dropdown" ref={containerRef} data-align="right">
      <button
        type="button"
        className="chip-dropdown__button workbench-filterchip__button"
        onClick={() => setIsOpen((previous) => !previous)}
        aria-expanded={isOpen}
      >
        <span className="chip-dropdown__summary">{summary}</span>
        <span className="chip-dropdown__chevron" aria-hidden="true" />
      </button>
      {isOpen ? (
        <div className="chip-dropdown__panel workbench-filterchip__panel" role="menu">
          <div className="workbench-searchmode__section">
            <div className="workbench-searchmode__label">Created after</div>
            <input
              type="datetime-local"
              value={createdAfter ? isoToLocalInput(createdAfter) : ''}
              onChange={(event) =>
                onChangeCreatedAfter(
                  event.target.value ? localInputToIso(event.target.value) : null,
                )
              }
              className="workbench-datefilter__input"
            />
          </div>
          <div className="workbench-searchmode__section">
            <div className="workbench-searchmode__label">Created before</div>
            <input
              type="datetime-local"
              value={createdBefore ? isoToLocalInput(createdBefore) : ''}
              onChange={(event) =>
                onChangeCreatedBefore(
                  event.target.value ? localInputToIso(event.target.value) : null,
                )
              }
              className="workbench-datefilter__input"
            />
          </div>
          <div className="workbench-datefilter__quick">
            {quickRanges.map((range) => (
              <button
                type="button"
                key={range.label}
                className="workbench-datefilter__quick-chip"
                onClick={() => {
                  const { after, before } = range.getRange();
                  onChangeCreatedAfter(after);
                  onChangeCreatedBefore(before);
                }}
              >
                {range.label}
              </button>
            ))}
          </div>
          <div className="workbench-filterchip__actions">
            <button
              type="button"
              className="workbench-filterchip__action-button"
              onClick={() => {
                onChangeCreatedBefore(null);
                onChangeCreatedAfter(null);
              }}
            >
              Clear dates
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function formatDateTime(value: string) {
  try {
    return new Date(value).toLocaleString();
  } catch {
    return value;
  }
}

const minutesAgoIso = (minutes: number) => new Date(Date.now() - minutes * 60 * 1000).toISOString();

function startOfTodayIso() {
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  return now.toISOString();
}

function startOfYesterdayIso() {
  const date = new Date();
  date.setDate(date.getDate() - 1);
  date.setHours(0, 0, 0, 0);
  return date.toISOString();
}

function startOfWeekIso() {
  const now = new Date();
  const day = now.getDay();
  const diff = (day + 6) % 7;
  now.setDate(now.getDate() - diff);
  now.setHours(0, 0, 0, 0);
  return now.toISOString();
}

function isoToLocalInput(iso: string) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  const offset = date.getTimezoneOffset();
  const local = new Date(date.getTime() - offset * 60 * 1000);
  return local.toISOString().slice(0, 16);
}

function localInputToIso(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toISOString();
}
