import {
  type KeyboardEvent,
  type RefObject,
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react';
import { useNavigate } from 'react-router-dom';

import type { ApiRepository } from '../../api/repositories';
import type { TextUnitSearchRequest } from '../../api/text-units';
import { AutoTextarea } from '../../components/AutoTextarea';
import { LocalePill } from '../../components/LocalePill';
import { Modal } from '../../components/Modal';
import { PillDropdown } from '../../components/PillDropdown';
import { getRowHeightPx } from '../../components/virtual/getRowHeightPx';
import { useMeasuredRowRefs } from '../../components/virtual/useMeasuredRowRefs';
import { useVirtualRows } from '../../components/virtual/useVirtualRows';
import { VirtualList } from '../../components/virtual/VirtualList';
import { isPrimaryActionShortcut } from '../../utils/keyboardShortcuts';
import { isRtlLocale } from '../../utils/localeDirection';
import { getNonRootRepositoryLocaleTags } from '../../utils/repositoryLocales';
import { saveWorkbenchSessionSearch, WORKBENCH_SESSION_QUERY_KEY } from './workbench-session-state';
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
  canSaveEditing: boolean;
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
  activeSearchRequest: TextUnitSearchRequest | null;
  repositories: ApiRepository[];
  onAddToCollection: (tmTextUnitId: number, repositoryId: number | null) => void;
  onRemoveFromCollection: (tmTextUnitId: number) => void;
  activeCollectionIds: Set<number>;
  activeCollectionName: string | null;
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
  canSaveEditing,
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
  activeSearchRequest,
  repositories,
  onAddToCollection,
  onRemoveFromCollection,
  activeCollectionIds,
  activeCollectionName,
}: WorkbenchBodyProps) {
  const navigate = useNavigate();
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

  const showSearchLoading =
    isSearchLoading &&
    !isRepositoryLoading &&
    !repositoryErrorMessage &&
    !searchErrorMessage &&
    !saveErrorMessage &&
    rows.length === 0;

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

  const getRepositoryScope = useCallback(
    (row: WorkbenchRow): { repoId: number; localeTags: string[] } | null => {
      const repository = repositories.find((repo) => repo.name === row.repositoryName);
      const repoId =
        repository?.id ??
        (activeSearchRequest?.repositoryIds?.length === 1
          ? activeSearchRequest.repositoryIds[0]
          : null);
      if (!repoId) {
        return null;
      }

      const repoLocales = repository ? getNonRootRepositoryLocaleTags(repository) : [];
      const localeTags =
        repoLocales.length > 0
          ? repoLocales
          : activeSearchRequest?.localeTags?.length
            ? activeSearchRequest.localeTags
            : [row.locale];

      if (!localeTags.length) {
        return null;
      }

      return { repoId, localeTags };
    },
    [activeSearchRequest, repositories],
  );

  const openTextUnitLink = useCallback(
    (row: WorkbenchRow) => {
      const scope = getRepositoryScope(row);
      if (!scope) {
        return;
      }

      const request: TextUnitSearchRequest = {
        repositoryIds: [scope.repoId],
        localeTags: scope.localeTags,
        searchAttribute: 'tmTextUnitIds',
        searchType: 'exact',
        searchText: String(row.tmTextUnitId),
        offset: 0,
      };

      const sessionKey = saveWorkbenchSessionSearch(request);
      const params = new URLSearchParams();
      params.set(WORKBENCH_SESSION_QUERY_KEY, sessionKey);
      void navigate(`/workbench?${params.toString()}`);
    },
    [getRepositoryScope, navigate],
  );

  const openAssetLink = useCallback(
    (row: WorkbenchRow) => {
      if (!row.assetPath) {
        return;
      }
      const scope = getRepositoryScope(row);
      if (!scope) {
        return;
      }

      const request: TextUnitSearchRequest = {
        repositoryIds: [scope.repoId],
        localeTags: scope.localeTags,
        searchAttribute: 'asset',
        searchType: 'exact',
        searchText: row.assetPath,
        offset: 0,
      };

      const sessionKey = saveWorkbenchSessionSearch(request);
      const params = new URLSearchParams();
      params.set(WORKBENCH_SESSION_QUERY_KEY, sessionKey);
      void navigate(`/workbench?${params.toString()}`);
    },
    [getRepositoryScope, navigate],
  );

  const openTextUnitDetail = useCallback(
    (row: WorkbenchRow) => {
      const params = new URLSearchParams();
      params.set('locale', row.locale);
      void navigate(`/text-units/${row.tmTextUnitId}?${params.toString()}`, {
        state: {
          from: '/workbench',
          workbenchSearch: activeSearchRequest,
        },
      });
    },
    [activeSearchRequest, navigate],
  );

  const scrollElementRef = useRef<HTMLDivElement>(null);

  const estimateSize = useCallback(
    () =>
      getRowHeightPx({
        element: scrollElementRef.current,
        cssVariable: '--workbench-row-height',
        defaultRem: 11.875,
      }),
    [],
  );
  const getItemKey = useCallback((index: number) => rows[index]?.id ?? index, [rows]);

  const {
    items: virtualItems,
    totalSize,
    measureElement,
  } = useVirtualRows<HTMLDivElement>({
    count: rows.length,
    getItemKey,
    estimateSize,
    getScrollElement: () => scrollElementRef.current,
    overscan: 6,
  });

  const { getRowRef } = useMeasuredRowRefs<string, HTMLDivElement>({
    measureElement,
    onAssign: (rowId, element) => {
      registerRowRefRef.current(rowId, element);
    },
  });

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
        {showNoResults ||
        showEmptyPrompt ||
        showSearchLoading ||
        showRepositoryLoading ||
        (!hasRows && (repositoryErrorMessage || saveErrorMessage || searchErrorMessage)) ? (
          <div className="workbench-page__rows">
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
                ) : showSearchLoading ? (
                  <>
                    <span className="workbench-page__empty-spinner" aria-hidden="true">
                      <span className="spinner" />
                    </span>
                    <span>Searching…</span>
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
          </div>
        ) : (
          <VirtualList
            scrollRef={scrollElementRef}
            items={virtualItems}
            totalSize={totalSize}
            renderRow={(virtualRow) => {
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
              const isUnused = !row.isUsed;
              const hasActiveCollection = Boolean(activeCollectionName);
              const isInCollection = hasActiveCollection
                ? activeCollectionIds.has(row.tmTextUnitId)
                : false;
              const collectionButtonLabel =
                hasActiveCollection && isInCollection ? 'In collection' : 'Add to collection';
              const handleTranslationKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
                if (!isEditing || !row.canEdit || isSaving) {
                  return;
                }
                if (isPrimaryActionShortcut(event)) {
                  event.preventDefault();
                  onSaveEditing();
                }
              };

              return {
                key: row.id,
                className: 'workbench-page__row',
                props: {
                  'data-index': virtualRow.index,
                  'data-status-open': isStatusOpen ? 'true' : undefined,
                  ref: getRowRef(row.id),
                },
                content: (
                  <>
                    <div className="workbench-page__cell workbench-page__cell--meta">
                      <div className="workbench-page__meta-link">
                        <span className="workbench-page__meta-id">{row.textUnitName}</span>
                        <button
                          type="button"
                          className="workbench-link-button"
                          onClick={(event) => {
                            event.stopPropagation();
                            openTextUnitLink(row);
                          }}
                          aria-label={`Open ${row.textUnitName} in workbench`}
                          title="Open in workbench"
                        >
                          ↗
                        </button>
                        {hasActiveCollection ? (
                          <button
                            type="button"
                            className={`workbench-collection__inline${isInCollection ? ' is-active' : ''}`}
                            onClick={(event) => {
                              event.stopPropagation();
                              if (isInCollection) {
                                onRemoveFromCollection(row.tmTextUnitId);
                              } else {
                                const scope = getRepositoryScope(row);
                                onAddToCollection(row.tmTextUnitId, scope?.repoId ?? null);
                              }
                            }}
                            title={`Add to ${activeCollectionName}`}
                          >
                            {collectionButtonLabel}
                          </button>
                        ) : null}
                      </div>
                      <div className="workbench-page__meta-link">
                        <span className="workbench-page__meta-asset">{row.assetPath ?? ''}</span>
                        {row.assetPath ? (
                          <button
                            type="button"
                            className="workbench-link-button"
                            onClick={(event) => {
                              event.stopPropagation();
                              openAssetLink(row);
                            }}
                            aria-label={`Open asset ${row.assetPath} in workbench`}
                            title="Open asset in workbench"
                          >
                            ↗
                          </button>
                        ) : null}
                      </div>
                      <div className="workbench-page__meta-link workbench-page__meta-link--stack">
                        <span className="workbench-page__repo-name">{row.repositoryName}</span>
                        {row.locations.length > 0 ? (
                          <span className="workbench-page__meta-location">
                            {row.locations.join(', ')}
                          </span>
                        ) : null}
                      </div>
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
                          if (!isEditing && row.canEdit) {
                            onStartEditing(row.id, row.translation);
                          }
                        }}
                        onChange={
                          isEditing
                            ? (event) => onChangeEditingValue(event.target.value)
                            : undefined
                        }
                        onKeyDown={isEditing ? handleTranslationKeyDown : undefined}
                        readOnly={!isEditing || !row.canEdit}
                        aria-disabled={row.canEdit ? undefined : 'true'}
                        ref={isEditing ? translationInputRef : undefined}
                        lang={translationLocale}
                        dir={translationDirection}
                        style={translationStyle}
                      />
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
                        <div className="workbench-page__translation-actions">
                          {isEditing ? (
                            <>
                              <button
                                type="button"
                                className="workbench-page__translation-button workbench-page__translation-button--primary"
                                onClick={(event) => {
                                  event.stopPropagation();
                                  onSaveEditing();
                                }}
                                disabled={!canSaveEditing || isSaving}
                                title={
                                  !canSaveEditing
                                    ? 'Already accepted. Edit the translation to save again.'
                                    : undefined
                                }
                              >
                                <span>Accept</span>
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
                            </>
                          ) : null}
                          <button
                            type="button"
                            className="workbench-page__translation-button"
                            onClick={(event) => {
                              event.stopPropagation();
                              openTextUnitDetail(row);
                            }}
                          >
                            Details
                          </button>
                        </div>
                      </div>
                    </div>
                    <div className="workbench-page__cell workbench-page__cell--locale">
                      <LocalePill bcp47Tag={row.locale} labelMode="tag" />
                    </div>
                    <div className="workbench-page__cell workbench-page__cell--status">
                      <div className="workbench-page__status-stack">
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
                            disabled={Boolean(editingRowId) || isStatusSaving || !row.canEdit}
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
                        {isUnused ? (
                          <span
                            className="workbench-page__unused-pill"
                            aria-label="Unused text unit"
                          >
                            Unused
                          </span>
                        ) : null}
                      </div>
                    </div>
                  </>
                ),
              };
            }}
          />
        )}
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
