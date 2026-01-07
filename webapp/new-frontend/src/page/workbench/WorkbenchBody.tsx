import { useVirtualizer } from '@tanstack/react-virtual';
import { type RefObject, useCallback, useEffect, useRef, useState } from 'react';

import { AutoTextarea } from '../../components/AutoTextarea';
import { Modal } from '../../components/Modal';
import { Pill } from '../../components/Pill';
import { PillDropdown } from '../../components/PillDropdown';
import { isRtlLocale } from '../../utils/localeDirection';
import type { WorkbenchDiffModalData, WorkbenchRow } from './workbench-types';

type WorkbenchBodyProps = {
  rows: WorkbenchRow[];
  editingRowId: string | null;
  editingValue: string;
  editedRowIds: Set<string>;
  statusSavingRowIds: Set<string>;
  onShowDiff: (rowId: string) => void;
  onStartEditing: (rowId: string, translation: string | null) => void;
  onCancelEditing: () => void;
  onSaveEditing: () => void;
  onChangeEditingValue: (value: string) => void;
  onChangeStatus: (rowId: string, status: string) => void;
  statusOptions: string[];
  translationInputRef: RefObject<HTMLTextAreaElement>;
  registerRowRef: (rowId: string, element: HTMLDivElement | null) => void;
  isSaving: boolean;
  saveErrorMessage: string | null;
  isRepositoryLoading: boolean;
  repositoryErrorMessage: string | null;
  canSearch: boolean;
  isSearchLoading: boolean;
  searchErrorMessage: string | null;
  onRetrySearch: () => void;
  hasSearched: boolean;
};

export function WorkbenchBody({
  rows,
  editingRowId,
  editingValue,
  editedRowIds,
  statusSavingRowIds,
  onShowDiff,
  onStartEditing,
  onCancelEditing,
  onSaveEditing,
  onChangeEditingValue,
  onChangeStatus,
  statusOptions,
  translationInputRef,
  registerRowRef,
  isSaving,
  saveErrorMessage,
  isRepositoryLoading,
  repositoryErrorMessage,
  canSearch,
  isSearchLoading,
  searchErrorMessage,
  onRetrySearch,
  hasSearched,
}: WorkbenchBodyProps) {
  const scrollRef = useRef<HTMLDivElement | null>(null);
  const rowRefCallbacks = useRef(new Map<string, (element: HTMLDivElement | null) => void>());
  const registerRowRefRef = useRef(registerRowRef);

  // Keep latest callback without changing ref callback identities.
  registerRowRefRef.current = registerRowRef;

  const hasRows = rows.length > 0;

  const showNoResults =
    hasSearched &&
    canSearch &&
    !isRepositoryLoading &&
    !isSearchLoading &&
    !repositoryErrorMessage &&
    !searchErrorMessage &&
    !saveErrorMessage &&
    rows.length === 0;

  const showRepositoryLoading =
    isRepositoryLoading &&
    !repositoryErrorMessage &&
    !searchErrorMessage &&
    !saveErrorMessage &&
    rows.length === 0;

  const showEmptyPrompt =
    !hasSearched &&
    !isRepositoryLoading &&
    !repositoryErrorMessage &&
    !searchErrorMessage &&
    !saveErrorMessage;

  const [showSearchOverlay, setShowSearchOverlay] = useState(false);
  const [openStatusRowId, setOpenStatusRowId] = useState<string | null>(null);
  useEffect(() => {
    if (!isSearchLoading) {
      setShowSearchOverlay(false);
      return;
    }
    // Avoid flicker for fast searches.
    const timeout = window.setTimeout(() => setShowSearchOverlay(true), 180);
    return () => window.clearTimeout(timeout);
  }, [isSearchLoading]);

  const isShowingStaleResults = showSearchOverlay && isSearchLoading && rows.length > 0;
  const showInlineErrorNotice =
    hasRows &&
    (Boolean(repositoryErrorMessage) || Boolean(saveErrorMessage) || Boolean(searchErrorMessage));

  const getScrollElement = useCallback(() => scrollRef.current, []);
  const estimateSize = useCallback(() => 190, []);
  const getItemKey = useCallback((index: number) => rows[index]?.id ?? index, [rows]);

  const virtualizer = useVirtualizer<HTMLDivElement, HTMLDivElement>({
    count: rows.length,
    getScrollElement,
    estimateSize,
    overscan: 6,
    getItemKey,
  });

  const virtualizerRef = useRef(virtualizer);
  // Keep latest instance available to stable ref callbacks.
  virtualizerRef.current = virtualizer;

  const items = virtualizer.getVirtualItems();

  const getRowRefCallback = useCallback((rowId: string) => {
    const existing = rowRefCallbacks.current.get(rowId);
    if (existing) {
      return existing;
    }

    // Important: keep the ref callback stable per rowId. React will call ref callbacks when they
    // change; if we create a new function every render and also measure elements (which triggers
    // virtualizer state updates), it can spiral into a render loop.
    const callback = (element: HTMLDivElement | null) => {
      registerRowRefRef.current(rowId, element);
      if (element) {
        virtualizerRef.current.measureElement(element);
      }
    };

    rowRefCallbacks.current.set(rowId, callback);
    return callback;
  }, []);

  return (
    <div className="workbench-page__body">
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
      <div
        className="workbench-page__rows-frame"
        data-stale={isShowingStaleResults ? 'true' : undefined}
      >
        {showInlineErrorNotice ? (
          <div className="workbench-page__notice">
            <div className="workbench-page__notice-text">
              {repositoryErrorMessage ?? saveErrorMessage ?? searchErrorMessage}
            </div>
            {searchErrorMessage ? (
              <button type="button" className="workbench-status__retry" onClick={onRetrySearch}>
                Retry
              </button>
            ) : null}
          </div>
        ) : null}
        {isShowingStaleResults ? (
          <div className="workbench-page__search-overlay" aria-hidden="true">
            <span className="spinner spinner--md" />
          </div>
        ) : null}
        <div className="workbench-page__rows" ref={scrollRef}>
          {showNoResults ||
          showEmptyPrompt ||
          showRepositoryLoading ||
          (!hasRows && (repositoryErrorMessage || saveErrorMessage || searchErrorMessage)) ? (
            <div className="workbench-page__empty">
              <div className="workbench-page__empty-text hint">
                {repositoryErrorMessage ? (
                  repositoryErrorMessage
                ) : saveErrorMessage ? (
                  saveErrorMessage
                ) : searchErrorMessage ? (
                  <>
                    <span>{searchErrorMessage}</span>
                    <div className="workbench-page__empty-action">
                      <button
                        type="button"
                        className="workbench-status__retry"
                        onClick={onRetrySearch}
                      >
                        Retry
                      </button>
                    </div>
                  </>
                ) : showRepositoryLoading ? (
                  <>
                    <span className="workbench-page__empty-spinner" aria-hidden="true">
                      <span className="spinner" />
                    </span>
                    <span>Loading repositories…</span>
                  </>
                ) : showNoResults ? (
                  'No results. Try a different search term, broaden your filters, or switch repositories/locales.'
                ) : canSearch ? (
                  'Searching…'
                ) : (
                  'Select repositories & locales. Pick at least one of each to get started.'
                )}
              </div>
            </div>
          ) : (
            <div
              className="workbench-page__rows-inner"
              style={{ height: `${virtualizer.getTotalSize()}px` }}
            >
              {items.map((virtualRow) => {
                const row = rows[virtualRow.index];
                if (!row) {
                  return null;
                }

                const isEditing = editingRowId === row.id;
                const translationValue = isEditing ? editingValue : (row.translation ?? '');
                const translationDirection = isRtlLocale(row.locale) ? 'rtl' : 'ltr';
                const translationLocale = row.locale;
                const translationStyle = isEditing ? undefined : { resize: 'none' as const };
                const isStatusSaving = statusSavingRowIds.has(row.id);
                const isEdited = editedRowIds.has(row.id);
                const isStatusOpen = openStatusRowId === row.id;

                return (
                  <div
                    key={row.id}
                    className="workbench-page__row"
                    data-index={virtualRow.index}
                    data-status-open={isStatusOpen ? 'true' : undefined}
                    ref={getRowRefCallback(row.id)}
                    style={{
                      transform: `translateY(${virtualRow.start}px)`,
                    }}
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
                    <div
                      className="workbench-page__cell workbench-page__cell--translation"
                      data-editing={isEditing ? 'true' : undefined}
                    >
                      <AutoTextarea
                        className="workbench-page__translation-input"
                        value={translationValue}
                        onFocus={() => {
                          if (!isEditing) {
                            onStartEditing(row.id, row.translation);
                          }
                        }}
                        onChange={
                          isEditing
                            ? (event) => onChangeEditingValue(event.target.value)
                            : undefined
                        }
                        readOnly={!isEditing}
                        ref={isEditing ? translationInputRef : undefined}
                        lang={translationLocale}
                        dir={translationDirection}
                        style={translationStyle}
                      />
                      {isEditing || isEdited ? (
                        <div className="workbench-page__translation-footer">
                          {isEdited ? (
                            <button
                              type="button"
                              className="workbench-page__edited-indicator"
                              onClick={() => onShowDiff(row.id)}
                            >
                              <span className="workbench-page__edited-dot" aria-hidden="true" />
                              <span className="workbench-page__edited-text">Edited</span>
                            </button>
                          ) : null}
                          {isEditing ? (
                            <div className="workbench-page__translation-actions">
                              <button
                                type="button"
                                className="workbench-page__translation-button workbench-page__translation-button--primary"
                                onClick={(event) => {
                                  event.stopPropagation();
                                  onSaveEditing();
                                }}
                                disabled={isSaving}
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
                                disabled={isSaving}
                              >
                                Cancel
                              </button>
                            </div>
                          ) : null}
                        </div>
                      ) : null}
                    </div>
                    <div className="workbench-page__cell workbench-page__cell--locale">
                      <Pill>{row.locale}</Pill>
                    </div>
                    <div className="workbench-page__cell workbench-page__cell--status">
                      {row.translation === null ? (
                        <span className="workbench-page__status-empty">—</span>
                      ) : (
                        <PillDropdown
                          value={row.status}
                          options={statusOptions.map((status) => ({
                            value: status,
                            label: status,
                          }))}
                          onChange={(next) => onChangeStatus(row.id, next)}
                          disabled={Boolean(editingRowId) || isStatusSaving}
                          aria-label="Translation status"
                          isOpen={isStatusOpen}
                          onOpenChange={(nextOpen) => {
                            setOpenStatusRowId((current) => {
                              if (nextOpen) {
                                return row.id;
                              }
                              return current === row.id ? null : current;
                            });
                          }}
                        />
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

type DiffModalProps = {
  data: WorkbenchDiffModalData | null;
  onClose: () => void;
};

export function DiffModal({ data, onClose }: DiffModalProps) {
  if (!data) {
    return null;
  }

  const beforeTarget = data.baselineTarget ?? '';
  const afterTarget = data.latestTarget ?? '';
  const beforeStatus = data.baselineStatusLabel;
  const afterStatus = data.latestStatusLabel;

  return (
    <Modal open size="xl" ariaLabel="Edit diff" onClose={onClose} closeOnBackdrop>
      <div className="modal__header workbench-diff-modal__header">
        <div className="modal__title workbench-diff-modal__title">Changes</div>
        <button type="button" className="modal__button" onClick={onClose}>
          Close
        </button>
      </div>

      <div className="workbench-diff-modal__meta">
        <div className="workbench-diff-modal__meta-line">
          <span className="workbench-diff-modal__meta-key">String</span>
          <span className="workbench-diff-modal__meta-value">{data.textUnitName}</span>
        </div>
        <div className="workbench-diff-modal__meta-line">
          <span className="workbench-diff-modal__meta-key">Locale</span>
          <span className="workbench-diff-modal__meta-value">{data.locale}</span>
        </div>
        <div className="workbench-diff-modal__meta-line">
          <span className="workbench-diff-modal__meta-key">Repo</span>
          <span className="workbench-diff-modal__meta-value">{data.repositoryName}</span>
        </div>
        {data.assetPath ? (
          <div className="workbench-diff-modal__meta-line">
            <span className="workbench-diff-modal__meta-key">Asset</span>
            <span className="workbench-diff-modal__meta-value">{data.assetPath}</span>
          </div>
        ) : null}
      </div>

      <div className="workbench-diff-modal__grid">
        <div className="workbench-diff-modal__column">
          <div className="workbench-diff-modal__column-title">Before</div>
          <div className="workbench-diff-modal__field">
            <div className="workbench-diff-modal__field-label">Variant</div>
            <div className="workbench-diff-modal__field-value">{data.baselineVariantId ?? '—'}</div>
          </div>
          <div className="workbench-diff-modal__field">
            <div className="workbench-diff-modal__field-label">Status</div>
            <div className="workbench-diff-modal__field-value">{beforeStatus}</div>
          </div>
          <div className="workbench-diff-modal__field">
            <div className="workbench-diff-modal__field-label">Translation</div>
            <pre className="workbench-diff-modal__code">{beforeTarget}</pre>
          </div>
        </div>

        <div className="workbench-diff-modal__column">
          <div className="workbench-diff-modal__column-title">After</div>
          <div className="workbench-diff-modal__field">
            <div className="workbench-diff-modal__field-label">Variant</div>
            <div className="workbench-diff-modal__field-value">{data.savedVariantId ?? '—'}</div>
          </div>
          <div className="workbench-diff-modal__field">
            <div className="workbench-diff-modal__field-label">Status</div>
            <div className="workbench-diff-modal__field-value">{afterStatus}</div>
          </div>
          <div className="workbench-diff-modal__field">
            <div className="workbench-diff-modal__field-label">Translation</div>
            <pre className="workbench-diff-modal__code">{afterTarget}</pre>
          </div>
        </div>
      </div>
    </Modal>
  );
}
