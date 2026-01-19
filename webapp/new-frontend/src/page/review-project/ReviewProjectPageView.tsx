import './review-project-page.css';
import '../review-projects/review-projects-page.css';

import type { VirtualItem } from '@tanstack/react-virtual';
import type React from 'react';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';

import type { ApiReviewProjectDetail, ApiReviewProjectTextUnit } from '../../api/review-projects';
import {
  acceptReviewProjectTextUnit,
  REVIEW_PROJECT_STATUS_LABELS,
  REVIEW_PROJECT_TYPE_LABELS,
  updateReviewProjectTextUnitReview,
} from '../../api/review-projects';
import { LocalePill } from '../../components/LocalePill';
import { Pill } from '../../components/Pill';
import { getRowHeightPx } from '../../components/virtual/getRowHeightPx';
import { useVirtualRows } from '../../components/virtual/useVirtualRows';
import { VirtualList } from '../../components/virtual/VirtualList';

type Props = {
  projectId: number;
  project: ApiReviewProjectDetail | null;
};

export function ReviewProjectPageView({ projectId, project }: Props) {
  const primaryLocale = project?.locale ?? project?.locales?.[0];
  const [textUnits, setTextUnits] = useState<ApiReviewProjectTextUnit[]>(
    () => primaryLocale?.textUnits ?? [],
  );

  useEffect(() => {
    setTextUnits(primaryLocale?.textUnits ?? []);
  }, [primaryLocale]);
  const layoutRef = useRef<HTMLDivElement>(null);
  const [listWidthPct, setListWidthPct] = useState(40);
  const [isResizing, setIsResizing] = useState(false);

  const [search, setSearch] = useState('');
  const [onlyReviewed, setOnlyReviewed] = useState(false);
  const [onlyEdited, setOnlyEdited] = useState(false);
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [selectedTextUnitId, setSelectedTextUnitId] = useState<number | null>(null);
  const [draftTargets, setDraftTargets] = useState<Record<number, string>>({});
  const [draftNotes, setDraftNotes] = useState<Record<number, string>>({});
  const [isAccepting, setIsAccepting] = useState(false);
  const [acceptError, setAcceptError] = useState<string | null>(null);
  const [needsOverride, setNeedsOverride] = useState(false);
  const [showLiveTarget, setShowLiveTarget] = useState<Record<number, boolean>>({});
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);

  const availableStatuses = useMemo(() => {
    const statuses = new Set<string>();
    textUnits.forEach((tu) => {
      if (tu?.status) {
        statuses.add(tu.status);
      }
    });
    return Array.from(statuses.values()).sort();
  }, [textUnits]);

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    return textUnits.filter((tu) => {
      if (!tu) return false;
      if (onlyReviewed && tu.selectedTmTextUnitVariantId == null) {
        return false;
      }
      if (
        onlyEdited &&
        !(
          tu.selectedTmTextUnitVariantId != null &&
          tu.currentTmTextUnitVariantId != null &&
          tu.selectedTmTextUnitVariantId !== tu.currentTmTextUnitVariantId
        )
      ) {
        return false;
      }
      if (statusFilter !== 'all' && tu.status !== statusFilter) {
        return false;
      }
      if (!term) return true;
      const haystacks = [tu.name, tu.source, tu.currentTarget, tu.target]
        .filter(Boolean)
        .map((s) => s!.toLowerCase());
      return haystacks.some((h) => h.includes(term));
    });
  }, [onlyEdited, onlyReviewed, search, statusFilter, textUnits]);

  const selectedTextUnit = useMemo(
    () => filtered.find((tu) => tu.reviewProjectTextUnitId === selectedTextUnitId),
    [filtered, selectedTextUnitId],
  );

  useEffect(() => {
    if (filtered.length === 0) {
      setSelectedTextUnitId(null);
      return;
    }
    if (
      selectedTextUnitId == null ||
      !filtered.some((tu) => tu.reviewProjectTextUnitId === selectedTextUnitId)
    ) {
      setSelectedTextUnitId(filtered[0]?.reviewProjectTextUnitId ?? null);
    }
  }, [filtered, selectedTextUnitId]);

  useEffect(() => {
    if (!selectedTextUnit) return;
    const id = selectedTextUnit.reviewProjectTextUnitId;
    setDraftTargets((prev) =>
      prev[id] !== undefined
        ? prev
        : {
            ...prev,
            [id]: selectedTextUnit.currentTarget || selectedTextUnit.target || '',
          },
    );
    setDraftNotes((prev) =>
      prev[id] !== undefined ? prev : { ...prev, [id]: selectedTextUnit.reviewNotes || '' },
    );
    setShowLiveTarget((prev) => ({
      ...prev,
      [id]: !!selectedTextUnit.currentTarget && selectedTextUnit.currentTarget !== selectedTextUnit.target,
    }));
    setAcceptError(null);
    setNeedsOverride(false);
  }, [selectedTextUnit]);

  // If a live current translation appears and the draft is still identical to the original,
  // re-prefill the draft with the live value (avoid clobbering user edits).
  useEffect(() => {
    if (!selectedTextUnit) return;
    const id = selectedTextUnit.reviewProjectTextUnitId;
    const draft = draftTargets[id];
    const original = selectedTextUnit.target ?? '';
    const current = selectedTextUnit.currentTarget ?? null;
    const liveDiffers = current != null && current !== original;
    if (liveDiffers && (draft === undefined || draft === original)) {
      setDraftTargets((prev) => ({
        ...prev,
        [id]: current,
      }));
    }
  }, [draftTargets, selectedTextUnit]);

  const estimateRowHeight = useCallback(
    () =>
      getRowHeightPx({
        cssVariable: '--review-project-row-height',
        defaultRem: 6,
      }),
    [],
  );

  const getItemKey = useCallback(
    (index: number) => filtered[index]?.reviewProjectTextUnitId ?? index,
    [filtered],
  );

  const { scrollRef, items, totalSize, measureElement, scrollToIndex } =
    useVirtualRows<HTMLDivElement>({
      count: filtered.length,
      estimateSize: estimateRowHeight,
      getItemKey,
    });

  const handleKeyNav = useCallback(
    (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null;
      if (
        target &&
        (target.tagName === 'INPUT' ||
          target.tagName === 'TEXTAREA' ||
          target.tagName === 'SELECT' ||
          target.isContentEditable)
      ) {
        return;
      }

      if (!filtered.length) return;

      const idx = selectedTextUnitId
        ? filtered.findIndex((tu) => tu.reviewProjectTextUnitId === selectedTextUnitId)
        : -1;

      if (event.key === 'ArrowDown' || event.key === 'j') {
        event.preventDefault();
        const nextIndex = Math.min(filtered.length - 1, idx + 1);
        const nextId = filtered[nextIndex]?.reviewProjectTextUnitId ?? null;
        if (nextId != null) {
          setSelectedTextUnitId(nextId);
          scrollToIndex(nextIndex, 'center');
        }
      } else if (event.key === 'ArrowUp' || event.key === 'k') {
        event.preventDefault();
        const prevIndex = Math.max(0, idx <= 0 ? 0 : idx - 1);
        const prevId = filtered[prevIndex]?.reviewProjectTextUnitId ?? null;
        if (prevId != null) {
          setSelectedTextUnitId(prevId);
          scrollToIndex(prevIndex, 'center');
        }
      }
    },
    [filtered, scrollToIndex, selectedTextUnitId],
  );

  useEffect(() => {
    window.addEventListener('keydown', handleKeyNav);
    return () => window.removeEventListener('keydown', handleKeyNav);
  }, [handleKeyNav]);

  useEffect(() => {
    if (!selectedTextUnit) return;
    if (selectedTextUnit.reviewStatus && selectedTextUnit.reviewStatus !== 'PENDING') {
      return;
    }
    void updateReviewProjectTextUnitReview({
      projectId,
      textUnitId: selectedTextUnit.reviewProjectTextUnitId,
      reviewStatus: 'VIEWED',
      reviewTarget: draftTargets[selectedTextUnit.reviewProjectTextUnitId],
      reviewNotes: draftNotes[selectedTextUnit.reviewProjectTextUnitId],
    }).then((updated) => {
      setTextUnits((prev) =>
        prev.map((tu) =>
          tu.reviewProjectTextUnitId === updated.reviewProjectTextUnitId ? { ...tu, ...updated } : tu,
        ),
      );
    });
  }, [draftNotes, draftTargets, projectId, selectedTextUnit, setTextUnits]);

  const startResize = useCallback((event: React.MouseEvent) => {
    event.preventDefault();
    setIsResizing(true);
    const onMove = (e: MouseEvent) => {
      if (!layoutRef.current) return;
      const rect = layoutRef.current.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const pct = Math.min(75, Math.max(20, (x / rect.width) * 100));
      setListWidthPct(pct);
    };
    const onUp = () => {
      setIsResizing(false);
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onUp);
    };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
  }, []);

  const handleAccept = useCallback(
    (override: boolean) => {
      if (!selectedTextUnit) return;
      const draftTargetValue =
        draftTargets[selectedTextUnit.reviewProjectTextUnitId] ??
        selectedTextUnit.currentTarget ??
        selectedTextUnit.target ??
        '';
      const reviewNotesValue = draftNotes[selectedTextUnit.reviewProjectTextUnitId] ?? '';

      setAcceptError(null);
      setIsAccepting(true);
      void (async () => {
        try {
          const updated = await acceptReviewProjectTextUnit({
            projectId,
            textUnitId: selectedTextUnit.reviewProjectTextUnitId,
            target: draftTargetValue,
            expectedCurrentTmTextUnitVariantId: selectedTextUnit.currentTmTextUnitVariantId ?? undefined,
            overrideChangedCurrent: override,
            reviewNotes: reviewNotesValue,
          });

          setDraftTargets((prev) => ({
            ...prev,
            [updated.reviewProjectTextUnitId]:
              updated.currentTarget ?? updated.target ?? draftTargetValue,
          }));
          setDraftNotes((prev) => ({
            ...prev,
            [updated.reviewProjectTextUnitId]: updated.reviewNotes ?? reviewNotesValue,
          }));
          setTextUnits((prev) =>
            prev.map((tu) =>
              tu.reviewProjectTextUnitId === updated.reviewProjectTextUnitId
                ? { ...tu, ...updated }
                : tu,
            ),
          );
          setNeedsOverride(false);
          setAcceptError(null);
        } catch (error) {
          const status = (error as { status?: number }).status;
          setAcceptError(
            status === 409
              ? 'Current translation changed; click "Override anyway" to proceed.'
              : error instanceof Error
                ? error.message
                : 'Failed to accept translation',
          );
          if (status === 409) {
            setNeedsOverride(true);
          }
        } finally {
          setIsAccepting(false);
        }
      })();
    },
    [draftNotes, draftTargets, projectId, selectedTextUnit],
  );

  const handleReviewStatus = useCallback(
    async (reviewStatus: ApiReviewProjectTextUnit['reviewStatus']) => {
      if (!selectedTextUnit || !reviewStatus) return;
      setIsUpdatingStatus(true);
      setAcceptError(null);
      try {
        const updated = await updateReviewProjectTextUnitReview({
          projectId,
          textUnitId: selectedTextUnit.reviewProjectTextUnitId,
          reviewStatus,
          reviewTarget: draftTargets[selectedTextUnit.reviewProjectTextUnitId],
          reviewNotes: draftNotes[selectedTextUnit.reviewProjectTextUnitId],
        });
        setTextUnits((prev) =>
          prev.map((tu) =>
            tu.reviewProjectTextUnitId === updated.reviewProjectTextUnitId ? { ...tu, ...updated } : tu,
          ),
        );
        setDraftNotes((prev) => ({
          ...prev,
          [updated.reviewProjectTextUnitId]: updated.reviewNotes ?? prev[updated.reviewProjectTextUnitId] ?? '',
        }));
        setNeedsOverride(false);
      } catch (error) {
        setAcceptError(error instanceof Error ? error.message : 'Failed to update review status');
      } finally {
        setIsUpdatingStatus(false);
      }
    },
    [draftNotes, draftTargets, projectId, selectedTextUnit],
  );

  if (!project) {
    return <div>No project data for id {projectId}</div>;
  }

  return (
    <div className="review-project-page">
      <ReviewProjectHeader projectId={projectId} project={project} textUnits={textUnits} />

      <div
        className="review-project-page__content"
        ref={layoutRef}
        style={{
          gridTemplateColumns: `${listWidthPct}% 8px ${100 - listWidthPct}%`,
        }}
      >
        <section className="review-project-page__list-pane">
          <div className="review-project-page__controls">
            <input
              className="review-project-page__search-input"
              type="search"
              placeholder="Search source/target/name"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <label className="review-project-page__control">
              <input
                type="checkbox"
                checked={onlyReviewed}
                onChange={(e) => setOnlyReviewed(e.target.checked)}
              />
              <span>Reviewed</span>
            </label>
            <label className="review-project-page__control">
              <input
                type="checkbox"
                checked={onlyEdited}
                onChange={(e) => setOnlyEdited(e.target.checked)}
              />
              <span>Edited</span>
            </label>
            <select
              className="review-project-page__control-select"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="all">All statuses</option>
              {availableStatuses.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </div>
          <VirtualList
            scrollRef={scrollRef}
            items={items}
            totalSize={totalSize}
            renderRow={(virtualItem: VirtualItem) => {
              const textUnit = filtered[virtualItem.index] as ApiReviewProjectTextUnit | undefined;
              if (!textUnit) {
                return null;
              }
              return {
                key: virtualItem.key,
                props: {
                  ref: measureElement,
                  onClick: () => setSelectedTextUnitId(textUnit.reviewProjectTextUnitId),
                  className:
                    textUnit.reviewProjectTextUnitId === selectedTextUnitId
                      ? 'review-project-row is-selected'
                      : 'review-project-row',
                },
                content: (
                  <TextUnitRow
                    textUnit={textUnit}
                    isSelected={textUnit.reviewProjectTextUnitId === selectedTextUnitId}
                  />
                ),
              };
            }}
          />
        </section>
        <div
          className={`review-project-page__resize-handle${isResizing ? ' is-resizing' : ''}`}
          onMouseDown={startResize}
        />
        <section className="review-project-page__detail-pane">
          {selectedTextUnit ? (
            <DetailPane
              textUnit={selectedTextUnit}
              draftTarget={draftTargets[selectedTextUnit.reviewProjectTextUnitId] ?? ''}
              draftNote={draftNotes[selectedTextUnit.reviewProjectTextUnitId] ?? ''}
              localeTag={primaryLocale?.bcp47Tag ?? primaryLocale?.displayName ?? ''}
              showLiveTarget={showLiveTarget[selectedTextUnit.reviewProjectTextUnitId] ?? false}
              onToggleLiveTarget={(next) =>
                setShowLiveTarget((prev) => ({
                  ...prev,
                  [selectedTextUnit.reviewProjectTextUnitId]: next,
                }))
              }
              onChangeDraftTarget={(value) =>
                setDraftTargets((prev) => ({
                  ...prev,
                  [selectedTextUnit.reviewProjectTextUnitId]: value,
                }))
              }
              onChangeDraftNote={(value) =>
                setDraftNotes((prev) => ({
                  ...prev,
                  [selectedTextUnit.reviewProjectTextUnitId]: value,
                }))
              }
              screenshotCount={project.screenshotImageIds?.length ?? 0}
              onAccept={handleAccept}
              onReviewStatus={handleReviewStatus}
              onRestoreOriginal={() => {
                setDraftTargets((prev) => ({
                  ...prev,
                  [selectedTextUnit.reviewProjectTextUnitId]: selectedTextUnit.target ?? '',
                }));
                setAcceptError(null);
                setNeedsOverride(false);
              }}
              onSaveNote={() => {
                void (async () => {
                  const note = draftNotes[selectedTextUnit.reviewProjectTextUnitId] ?? '';
                  const updated = await updateReviewProjectTextUnitReview({
                    projectId,
                    textUnitId: selectedTextUnit.reviewProjectTextUnitId,
                    reviewStatus: selectedTextUnit.reviewStatus ?? 'VIEWED',
                    reviewTarget: draftTargets[selectedTextUnit.reviewProjectTextUnitId],
                    reviewNotes: note,
                  });
                  setTextUnits((prev) =>
                    prev.map((tu) =>
                      tu.reviewProjectTextUnitId === updated.reviewProjectTextUnitId
                        ? { ...tu, ...updated }
                        : tu,
                    ),
                  );
                })();
              }}
              acceptError={acceptError}
              isAccepting={isAccepting}
              isUpdatingStatus={isUpdatingStatus}
              needsOverride={needsOverride}
            />
          ) : (
            <div className="review-project-page__empty-detail">No text unit selected</div>
          )}
        </section>
      </div>
    </div>
  );
}

function TextUnitRow({
  textUnit,
  isSelected,
}: {
  textUnit: ApiReviewProjectTextUnit;
  isSelected: boolean;
}) {
  if (!textUnit) {
    return null;
  }
  const { reviewProjectTextUnitId, name, source, target, currentTarget } = textUnit;
  return (
    <div className="review-project-row__inner" data-selected={isSelected ? 'true' : 'false'}>
      <div className="review-project-row__name" title={name ?? undefined}>
        {name || `Text unit ${reviewProjectTextUnitId}`}
      </div>
      <div className="review-project-row__strings">
        <div className="review-project-row__string-line" title={source}>
          <span className="review-project-row__string-text">{source || '—'}</span>
        </div>
        <div
          className="review-project-row__string-line review-project-row__string-line--target"
          title={(currentTarget ?? target) ?? undefined}
        >
          <span className="review-project-row__string-text review-project-row__string-text--target">
            {currentTarget || target || '—'}
          </span>
        </div>
      </div>
    </div>
  );
}

function DetailPane({
  textUnit,
  draftTarget,
  draftNote,
  localeTag,
  showLiveTarget,
  onToggleLiveTarget,
  onChangeDraftTarget,
  onChangeDraftNote,
  onSaveNote,
  onReviewStatus,
  screenshotCount,
  onAccept,
  onRestoreOriginal,
  acceptError,
  isAccepting,
  isUpdatingStatus,
  needsOverride,
}: {
  textUnit: ApiReviewProjectTextUnit;
  draftTarget: string;
  draftNote: string;
  localeTag: string;
  showLiveTarget: boolean;
  onToggleLiveTarget: (next: boolean) => void;
  onChangeDraftTarget: (value: string) => void;
  onChangeDraftNote: (value: string) => void;
  onSaveNote: () => void;
  onReviewStatus: (status: ApiReviewProjectTextUnit['reviewStatus']) => Promise<void> | void;
  screenshotCount: number;
  onAccept: (override: boolean) => void;
  onRestoreOriginal: () => void;
  acceptError: string | null;
  isAccepting: boolean;
  isUpdatingStatus: boolean;
  needsOverride: boolean;
}) {
  const hasStaleCurrent =
    textUnit.currentTmTextUnitVariantId != null &&
    textUnit.tmTextUnitVariantId != null &&
    textUnit.currentTmTextUnitVariantId !== textUnit.tmTextUnitVariantId;
  const liveDiffers =
    hasStaleCurrent || (textUnit.currentTarget && textUnit.currentTarget !== textUnit.target);
  const canShowToggle = !!textUnit.currentTarget && liveDiffers;
  const displayedTarget =
    canShowToggle && showLiveTarget ? textUnit.currentTarget : textUnit.target;

  return (
    <div className="review-project-detail">
      <div className="review-project-detail__header">
        <div className="review-project-detail__title">
          {textUnit.name || `Text unit ${textUnit.reviewProjectTextUnitId}`}
        </div>
      </div>
      <div className="review-project-detail__grid">
        <div className="review-project-detail__field">
          <div className="review-project-detail__label">Source</div>
          <div className="review-project-detail__value">{textUnit.source || '—'}</div>
        </div>
        <div className="review-project-detail__field">
          <div className="review-project-detail__label review-project-detail__label--with-link">
            <span>{showLiveTarget ? 'Current translation' : 'Original target'}</span>
            {liveDiffers ? (
              <span className="review-project-detail__pill review-project-detail__pill--warning">
                Updated
              </span>
            ) : null}
            {canShowToggle ? (
              <button
                type="button"
                className="review-project-detail__link review-project-detail__link--inline"
                onClick={() => onToggleLiveTarget(!showLiveTarget)}
              >
                {showLiveTarget ? 'Show original' : 'Show current'}
              </button>
            ) : null}
            <Link
              className="review-project-detail__link review-project-detail__link--inline"
              to={{
                pathname: '/workbench',
                search: `?tmTextUnitId=${encodeURIComponent(textUnit.tmTextUnitId)}${
                  localeTag ? `&locale=${encodeURIComponent(localeTag)}` : ''
                }${textUnit.repositoryId ? `&repo=${textUnit.repositoryId}` : ''}`,
                state: {
                  workbenchSearch: {
                    searchAttribute: 'tmTextUnitIds',
                    searchType: 'exact',
                    searchText: String(textUnit.tmTextUnitId),
                    localeTags: localeTag ? [localeTag] : undefined,
                    repositoryIds: textUnit.repositoryId ? [textUnit.repositoryId] : undefined,
                  },
                },
              }}
              title="Open this string in Workbench"
            >
              Open in Workbench
            </Link>
          </div>
          <div className="review-project-detail__value review-project-detail__value--target review-project-detail__value--restorable">
            <span>{displayedTarget || '—'}</span>
            {textUnit.target ? (
              <button
                type="button"
                className="review-project-detail__pill review-project-detail__pill--floating"
                onClick={onRestoreOriginal}
              >
                Restore to proposed
              </button>
            ) : null}
          </div>
          {hasStaleCurrent ? (
            <div className="review-project-detail__notice">
              Current translation changed since selection; saving will only record a suggestion.
            </div>
          ) : null}
        </div>
        <div className="review-project-detail__field">
          <div className="review-project-detail__label">Proposed translation</div>
          <textarea
            className="review-project-detail__input"
            value={draftTarget}
            onChange={(e) => onChangeDraftTarget(e.target.value)}
            rows={5}
            placeholder="Enter proposed translation"
          />
          <div className="review-project-detail__actions-inline">
            {textUnit.reviewStatus ? (
              <span className="review-project-detail__status-pill">
                {textUnit.reviewStatus.toLowerCase().replace(/_/g, ' ')}
              </span>
            ) : textUnit.status ? (
              <span className="review-project-detail__status-pill">
                {textUnit.status.toLowerCase().replace(/_/g, ' ')}
              </span>
            ) : null}
            <button
              type="button"
              className="review-project-detail__actions-button review-project-detail__actions-button--primary"
              onClick={() => onAccept(false)}
              disabled={isAccepting || needsOverride}
            >
              {isAccepting ? 'Accepting…' : 'Accept'}
            </button>
            <button
              type="button"
              className="review-project-detail__actions-button"
              onClick={() => onAccept(true)}
              disabled={isAccepting || !needsOverride}
              title={needsOverride ? 'Override current translation and accept' : 'Override not needed'}
            >
              Override anyway
            </button>
            <button
              type="button"
              className="review-project-detail__actions-button"
              onClick={() => {
                void onReviewStatus('REJECTED');
              }}
              disabled={isAccepting || isUpdatingStatus}
            >
              Reject
            </button>
            <button
              type="button"
              className="review-project-detail__actions-button"
              onClick={() => {
                void onReviewStatus('SKIPPED');
              }}
              disabled={isAccepting || isUpdatingStatus}
            >
              Skip
            </button>
          </div>
          {acceptError ? <div className="review-project-detail__error">{acceptError}</div> : null}
        </div>
        <div className="review-project-detail__field">
          <div className="review-project-detail__label">Reviewer notes</div>
          <textarea
            className="review-project-detail__input"
            value={draftNote}
            onChange={(e) => onChangeDraftNote(e.target.value)}
            rows={5}
            placeholder="Add context for AI/translator: errors seen, tone, glossary, or rationale (optional)"
          />
          <div className="review-project-detail__note-actions">
            <button
              type="button"
              className="review-project-detail__actions-button"
              onClick={() => {
                void onSaveNote();
              }}
            >
              Save note
            </button>
          </div>
        </div>
      </div>
      <div className="review-project-detail__actions">
        <div className="review-project-detail__actions-row">
          <div className="review-project-detail__actions-label">Screenshots</div>
          <div className="review-project-detail__actions-info">
            {screenshotCount > 0 ? `${screenshotCount} attached` : 'No screenshots attached'}
          </div>
          <button
            type="button"
            className="review-project-detail__actions-button"
            disabled={screenshotCount === 0}
          >
            Open
          </button>
        </div>
      </div>
    </div>
  );
}

function ReviewProjectHeader({
  projectId,
  project,
  textUnits: textUnitsProp,
}: {
  projectId: number;
  project: ApiReviewProjectDetail;
  textUnits: ApiReviewProjectTextUnit[];
}) {
  const { name, dueDate, textUnitCount, wordCount, status, type, locales: localesRaw } = project;
  const textUnits = useMemo(() => textUnitsProp ?? [], [textUnitsProp]);
  const locales = useMemo(() => localesRaw ?? [], [localesRaw]);

  const { acceptedCount, selectedCount, progressPercent } = useMemo(() => {
    const selected = textUnits?.length ?? 0;
    const accepted =
      textUnits?.filter(
        (tu) =>
          tu.reviewStatus === 'ACCEPTED_AS_IS' || tu.reviewStatus === 'ACCEPTED_WITH_CHANGE',
      ).length ?? 0;
    const percent = selected > 0 ? Math.round((accepted / selected) * 100) : 0;
    return { acceptedCount: accepted, selectedCount: selected, progressPercent: percent };
  }, [textUnits]);

  return (
    <header className="review-project-page__header review-project-page__header--compact">
      <div className="review-project-page__one-line">
        <div className="review-project-page__group review-project-page__group--left">
          <span className="review-project-page__title">{name ?? `Project ${projectId}`}</span>
          <Pill className={`review-project-page__pill review-project-page__pill--type-${type}`}>
            {REVIEW_PROJECT_TYPE_LABELS[type]}
          </Pill>
          <Pill
            className={`review-project-page__pill review-project-page__pill--status-${status.toLowerCase()}`}
          >
            {REVIEW_PROJECT_STATUS_LABELS[status]}
          </Pill>
        </div>

        <div className="review-project-page__group review-project-page__group--meta">
          <span>Due {formatDate(dueDate)}</span>
          <span className="review-project-page__dot">•</span>
          <div className="review-project-page__locale-row">
            {locales.length > 0 ? (
              locales.map((locale) => (
                <LocalePill
                  key={locale.id ?? locale.bcp47Tag}
                  bcp47Tag={locale.bcp47Tag}
                  displayName={locale.displayName}
                  labelMode="tag"
                  className="review-project-page__locale-pill"
                />
              ))
            ) : (
              <span className="review-project-page__muted">No locale</span>
            )}
          </div>
        </div>

        <div className="review-project-page__group review-project-page__group--stats">
          <CountsInline words={wordCount} strings={textUnitCount ?? selectedCount} />
          <span className="review-project-page__dot">•</span>
          <div className="review-project-page__progress-chip">
            <span
              className="review-project-page__progress-label"
              title={`${acceptedCount}/${selectedCount} accepted`}
            >
              {progressPercent}%
            </span>
            <ProgressBar percent={progressPercent} />
          </div>
        </div>
      </div>
    </header>
  );
}

function CountsInline({
  words,
  strings,
}: {
  words: number | null | undefined;
  strings: number | null | undefined;
}) {
  return (
    <span className="review-projects-page__count-line">
      <span className="review-projects-page__count">{formatNumber(words)}</span>
      <span className="review-projects-page__muted">&nbsp;words</span>
      <span className="review-projects-page__count-sep">&nbsp;·&nbsp;</span>
      <span className="review-projects-page__count">{formatNumber(strings)}</span>
      <span className="review-projects-page__muted">&nbsp;strings</span>
    </span>
  );
}

function ProgressBar({ percent }: { percent: number }) {
  return (
    <div className="review-project-page__progress-bar review-project-page__progress-bar--compact">
      <div
        className="review-project-page__progress-fill"
        style={{ width: `${Math.min(Math.max(percent, 0), 100)}%` }}
      />
    </div>
  );
}

const formatNumber = (value: number | null | undefined) => {
  if (value == null) {
    return '—';
  }
  return value.toLocaleString();
};

const formatDate = (value: string | null | undefined) => {
  if (!value) {
    return '—';
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
};
