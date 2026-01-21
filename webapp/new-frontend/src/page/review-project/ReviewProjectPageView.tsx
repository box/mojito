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
import { Modal } from '../../components/Modal';
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
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);
  const [isScreenshotModalOpen, setIsScreenshotModalOpen] = useState(false);
  const [selectedScreenshotIdx, setSelectedScreenshotIdx] = useState<number>(0);

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

  const screenshotImages = useMemo(
    () => project?.screenshotImageIds ?? [],
    [project?.screenshotImageIds],
  );

  useEffect(() => {
    if (!screenshotImages.length) {
      setSelectedScreenshotIdx(0);
      return;
    }
    setSelectedScreenshotIdx((idx) => Math.min(idx, screenshotImages.length - 1));
  }, [screenshotImages]);

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
            [id]: selectedTextUnit.reviewTarget ?? selectedTextUnit.target ?? '',
          },
    );
    setDraftNotes((prev) =>
      prev[id] !== undefined ? prev : { ...prev, [id]: selectedTextUnit.reviewNotes || '' },
    );
    setAcceptError(null);
  }, [selectedTextUnit]);

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
              updated.reviewTarget ?? draftTargetValue,
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
          setAcceptError(null);
        } catch (error) {
          const status = (error as { status?: number }).status;
          setAcceptError(
            status === 409
              ? 'Current translation changed; refresh to review the latest version before accepting.'
              : error instanceof Error
                ? error.message
                : 'Failed to accept translation',
          );
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

        {/*TODO(ja) why is the style here, move to CSS*/}
      <div
        className="review-project-page__content"
        ref={layoutRef}
        style={{ ['--review-list-width' as string]: `${listWidthPct}%` }}
      >
        <section className="review-project-page__list-pane">
          <div className="review-project-page__controls review-project-page__controls--compact">
            {/*  that search area does not work well with resizing ,  we need it more compact, probably one line
             with a single small filter button. we first need to clarify the states though */}
            <input
              className="review-project-page__search-input"
              type="search"
              placeholder="Search source/target/name"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
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
          {/* TODO(ja)  this looks great so far, but the font is too large we need to condensate the left bar we may even go to 9px, but use the app.css texxt size variable please */}
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
              screenshotCount={screenshotImages.length}
              screenshotImages={screenshotImages}
              currentScreenshotIdx={selectedScreenshotIdx}
              onChangeScreenshotIdx={setSelectedScreenshotIdx}
              onOpenGallery={() => setIsScreenshotModalOpen(true)}
              onAccept={handleAccept}
              onReviewStatus={handleReviewStatus}
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
            />
          ) : (
            <div className="review-project-page__empty-detail">No text unit selected</div>
          )}
        </section>
      </div>
      {isScreenshotModalOpen ? (
        <Modal
          open
          onClose={() => setIsScreenshotModalOpen(false)}
          closeOnBackdrop
          size="lg"
          className="modal--screenshot"
          ariaLabel="Screenshot gallery"
        >
          <div className="review-project-screenshot-modal">
            <div className="review-project-screenshot-modal__header">
              <div className="review-project-screenshot-modal__title">Screenshots</div>
              <div className="review-project-screenshot-modal__count">
                {screenshotImages.length} attached
              </div>
            </div>
            {screenshotImages.length ? (
              <div className="review-project-screenshot-modal__gallery">
                <button
                  type="button"
                  className="review-project-screenshot-lightbox__nav review-project-screenshot-lightbox__nav--prev"
                  onClick={() =>
                    setSelectedScreenshotIdx(
                      (selectedScreenshotIdx - 1 + screenshotImages.length) %
                        screenshotImages.length,
                    )
                  }
                  aria-label="Previous screenshot"
                >
                  ‹
                </button>
                <div className="review-project-detail__gallery-main review-project-detail__gallery-main--modal">
                  {renderMedia(
                    screenshotImages[selectedScreenshotIdx],
                    'review-project-screenshot-modal__image--main',
                  )}
                </div>
                <button
                  type="button"
                  className="review-project-screenshot-lightbox__nav review-project-screenshot-lightbox__nav--next"
                  onClick={() =>
                    setSelectedScreenshotIdx((selectedScreenshotIdx + 1) % screenshotImages.length)
                  }
                  aria-label="Next screenshot"
                >
                  ›
                </button>
              </div>
            ) : (
              <div className="review-project-screenshot-modal__empty">No screenshots attached.</div>
            )}
            {screenshotImages.length ? (
              <div className="review-project-detail__thumbs review-project-detail__thumbs--modal">
                {screenshotImages.map((key, idx) => {
                  const isActive = idx === selectedScreenshotIdx;
                  return (
                    <button
                      key={`${key}-${idx}`}
                      type="button"
                      className={`review-project-detail__thumb${isActive ? ' is-active' : ''}`}
                      onClick={() => setSelectedScreenshotIdx(idx)}
                      title="Click to preview"
                    >
                      {renderThumbMedia(key)}
                    </button>
                  );
                })}
              </div>
            ) : null}
            <div className="review-project-screenshot-modal__footer">
              <button
                type="button"
                className="review-project-detail__actions-button"
                onClick={() => setIsScreenshotModalOpen(false)}
              >
                Close
              </button>
            </div>
          </div>
        </Modal>
      ) : null}
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
  const { reviewProjectTextUnitId, name, source, target } = textUnit;
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
          title={target ?? undefined}
        >
          <span className="review-project-row__string-text review-project-row__string-text--target">
            {target || '—'}
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
  onChangeDraftTarget,
  onChangeDraftNote,
  onSaveNote,
  onReviewStatus,
  screenshotCount,
  screenshotImages,
  currentScreenshotIdx,
  onChangeScreenshotIdx,
  onOpenGallery,
  onAccept,
  acceptError,
  isAccepting,
  isUpdatingStatus,
}: {
  textUnit: ApiReviewProjectTextUnit;
  draftTarget: string;
  draftNote: string;
  localeTag: string;
  onChangeDraftTarget: (value: string) => void;
  onChangeDraftNote: (value: string) => void;
  onSaveNote: () => void;
  onReviewStatus: (status: ApiReviewProjectTextUnit['reviewStatus']) => Promise<void> | void;
  screenshotCount: number;
  screenshotImages: string[];
  currentScreenshotIdx: number;
  onChangeScreenshotIdx: (index: number) => void;
  onOpenGallery: () => void;
  onAccept: (override: boolean) => void;
  acceptError: string | null;
  isAccepting: boolean;
  isUpdatingStatus: boolean;
}) {
  const displayedTarget = textUnit.target;
  const proposedValue = textUnit.reviewTarget ?? draftTarget;
  const hasExternalChange =
    textUnit.currentTarget != null && textUnit.currentTarget !== proposedValue;
  type StatusOption = 'accepted' | 'needs_translation' | 'needs_review' | 'rejected';
  const [selectedStatus, setSelectedStatus] = useState<StatusOption>('accepted');
  const [isStatusMenuOpen, setIsStatusMenuOpen] = useState(false);
  const statusMenuRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!isStatusMenuOpen) return;
    const handlePointerDown = (event: PointerEvent) => {
      if (!statusMenuRef.current?.contains(event.target as Node)) {
        setIsStatusMenuOpen(false);
      }
    };
    window.addEventListener('pointerdown', handlePointerDown);
    return () => window.removeEventListener('pointerdown', handlePointerDown);
  }, [isStatusMenuOpen]);

  useEffect(() => {
    const current = textUnit.reviewStatus;
    const mapped: StatusOption =
      current === 'REJECTED'
        ? 'rejected'
        : current === 'SKIPPED'
          ? 'needs_translation'
          : current === 'VIEWED'
            ? 'needs_review'
            : current === 'ACCEPTED_AS_IS' || current === 'ACCEPTED_WITH_CHANGE'
              ? 'accepted'
              : 'accepted';
    setSelectedStatus(mapped);
  }, [textUnit.reviewStatus]);

  const handleStatusSelect = (key: StatusOption) => {
    setSelectedStatus(key);
    setIsStatusMenuOpen(false);
  };

  const applySelectedStatus = () => {
    if (selectedStatus === 'accepted') {
      onAccept(false);
      return;
    }
    if (selectedStatus === 'rejected') {
      void onReviewStatus('REJECTED');
      return;
    }
    if (selectedStatus === 'needs_review') {
      void onReviewStatus('VIEWED');
      return;
    }
    if (selectedStatus === 'needs_translation') {
      void onReviewStatus('SKIPPED');
    }
  };

  const statusDisplay: Record<StatusOption, string> = {
    accepted: 'Accepted',
    needs_review: 'To review',
    needs_translation: 'To translate',
    rejected: 'Rejected',
  };
  const statusMenuLabel = 'With status';
  const primaryLabel = 'Accept';
  const isBusy = isAccepting || isUpdatingStatus;

  return (
    <div className="review-project-detail">
      <div className="review-project-detail__header">
        <div className="review-project-detail__title" />
      </div>
      <div className="review-project-detail__layout">
        <div className="review-project-detail__main">
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
              <div className="review-project-detail__split-wrap">
                <div className="review-project-detail__split" ref={statusMenuRef}>
                  <button
                    type="button"
                    className="review-project-detail__actions-button review-project-detail__actions-button--primary review-project-detail__split-main"
                    onClick={applySelectedStatus}
                    disabled={isBusy}
                  >
                    <span className="review-project-detail__primary-label">{primaryLabel}</span>
                  </button>
                  <button
                    type="button"
                    className="review-project-detail__actions-button review-project-detail__split-caret"
                    aria-haspopup="menu"
                    aria-expanded={isStatusMenuOpen}
                    onClick={() => setIsStatusMenuOpen((open) => !open)}
                    disabled={isBusy}
                    title="Set different status"
                  >
                    ▾
                  </button>
                  {isStatusMenuOpen ? (
                    <div className="review-project-detail__status-menu" role="menu">
                      <div className="review-project-detail__status-menu-label">{statusMenuLabel}</div>
                      <button
                        type="button"
                        role="menuitem"
                        onClick={() => handleStatusSelect('accepted')}
                        className={selectedStatus === 'accepted' ? 'is-active' : undefined}
                      >
                        {statusDisplay.accepted}
                      </button>
                      <button
                        type="button"
                        role="menuitem"
                        onClick={() => handleStatusSelect('needs_review')}
                        className={selectedStatus === 'needs_review' ? 'is-active' : undefined}
                      >
                        {statusDisplay.needs_review}
                      </button>
                      <button
                        type="button"
                        role="menuitem"
                        onClick={() => handleStatusSelect('needs_translation')}
                        className={selectedStatus === 'needs_translation' ? 'is-active' : undefined}
                      >
                        {statusDisplay.needs_translation}
                      </button>
                      <button
                        type="button"
                        role="menuitem"
                        onClick={() => handleStatusSelect('rejected')}
                        className={selectedStatus === 'rejected' ? 'is-active' : undefined}
                      >
                        {statusDisplay.rejected}
                      </button>
                    </div>
                  ) : null}
                </div>
                <span className="review-project-detail__split-spinner">
                  {isBusy ? <span className="spinner spinner--inline" aria-hidden="true" /> : null}
                </span>
              </div>
              {getDisplayStatus(textUnit.reviewStatus ?? textUnit.status) ? (
                <span className="review-project-detail__status-pill review-project-detail__status-pill--right">
                  {getDisplayStatus(textUnit.reviewStatus ?? textUnit.status)}
                </span>
              ) : null}
            </div>
            {acceptError ? <div className="review-project-detail__error">{acceptError}</div> : null}
          </div>

          <div className="review-project-detail__field">
            <div className="review-project-detail__label">Reviewer notes</div>
            <textarea
              className="review-project-detail__input"
              value={draftNote}
              onChange={(e) => onChangeDraftNote(e.target.value)}
              rows={4}
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

        <div className="review-project-detail__side">
          <div className="review-project-detail__field">
            <div className="review-project-detail__label">Source</div>
            <div className="review-project-detail__value">{textUnit.source || '—'}</div>
          </div>

          <div className="review-project-detail__field">
            <div className="review-project-detail__label">Translation</div>
            <div className="review-project-detail__value review-project-detail__value--target review-project-detail__value--restorable">
              <span>{displayedTarget || '—'}</span>
                  {hasExternalChange ? (
                    <div className="review-project-detail__external">
                      <div className="review-project-detail__label">
                        <Pill className="review-project-detail__pill review-project-detail__pill--warning">
                          New target since project creation
                        </Pill>
                      </div>
                      <div className="review-project-detail__value review-project-detail__value--target">
                        <span>{textUnit.currentTarget || '—'}</span>
                      </div>
                </div>
              ) : null}
            </div>
          </div>

          <div className="review-project-detail__field">
            <div className="review-project-detail__label">String</div>
            <div className="review-project-detail__value review-project-detail__value--meta">
              <span className="review-project-detail__title-text">
                {textUnit.name || `Text unit ${textUnit.reviewProjectTextUnitId}`}
              </span>
            </div>
            <Link
              className="pill review-project-detail__pill-link review-project-detail__pill-link--title"
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

          <div className="review-project-detail__meta review-project-detail__field">
            <div className="review-project-detail__label">IDs</div>
            <div className="review-project-detail__value review-project-detail__value--meta">
              TM text unit ID {textUnit.tmTextUnitId}
            </div>
            {textUnit.repositoryName ? (
              <div className="review-project-detail__value review-project-detail__value--meta">
                Repo: {textUnit.repositoryName}
              </div>
            ) : null}
          </div>

          <div className="review-project-detail__field">
            <div className="review-project-detail__shots-header">
              <div className="review-project-detail__actions-label">Screenshots</div>
              <div className="review-project-detail__shots-count">
                {screenshotCount === 0
                  ? 'No attachments'
                  : screenshotCount === 1
                    ? '1 attachment'
                    : `${screenshotCount} attachments`}
              </div>
            </div>
            {screenshotImages.length ? (
              <div className="review-project-detail__gallery review-project-detail__gallery--compact">
                <button
                  type="button"
                  className="review-project-detail__gallery-nav"
                  onClick={() =>
                    onChangeScreenshotIdx(
                      (currentScreenshotIdx - 1 + screenshotImages.length) % screenshotImages.length,
                    )
                  }
                  aria-label="Previous screenshot"
                >
                  ‹
                </button>
                <button
                  type="button"
                  className="review-project-detail__gallery-main review-project-detail__gallery-main--compact"
                  onClick={() => onOpenGallery()}
                  title="Click to open fullscreen"
                >
                  {renderMedia(
                    screenshotImages[currentScreenshotIdx],
                    'review-project-detail__gallery-image',
                    {
                      controls: false,
                      muted: true,
                      loop: true,
                      preload: 'metadata',
                    },
                  )}
                </button>
                <button
                  type="button"
                  className="review-project-detail__gallery-nav"
                  onClick={() =>
                    onChangeScreenshotIdx((currentScreenshotIdx + 1) % screenshotImages.length)
                  }
                  aria-label="Next screenshot"
                >
                  ›
                </button>
              </div>
            ) : null}
          </div>
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

const getDisplayStatus = (status: string | null | undefined) => {
  if (!status) return null;
  const upper = status.toUpperCase();
  if (upper === 'ACCEPTED_AS_IS' || upper === 'ACCEPTED_WITH_CHANGE' || upper === 'ACCEPTED') {
    return 'Accepted';
  }
  if (upper === 'REJECTED') return 'Rejected';
  if (upper === 'VIEWED') return 'To review';
  if (upper === 'SKIPPED') return 'To translate';
  const cleaned = status.toLowerCase().replace(/_/g, ' ');
  return cleaned.charAt(0).toUpperCase() + cleaned.slice(1);
};

const VIDEO_EXTENSIONS = ['.mp4', '.mov', '.webm', '.ogv', '.ogg', '.m4v', '.mkv'];

const resolveMediaUrl = (key: string) => {
  const isExternal =
    /^https?:\/\//i.test(key) || key.startsWith('//') || key.startsWith('data:') || key.startsWith('blob:');
  return isExternal ? key : `/api/images/${encodeURIComponent(key)}`;
};

const isVideoKey = (key: string) => {
  const lower = key.split('?')[0].toLowerCase();
  return key.startsWith('data:video') || key.startsWith('blob:') || VIDEO_EXTENSIONS.some((ext) => lower.endsWith(ext));
};

type MediaRenderOptions = {
  controls?: boolean;
  muted?: boolean;
  loop?: boolean;
  preload?: 'none' | 'metadata' | 'auto';
};

const renderMedia = (key: string, className?: string, options: MediaRenderOptions = {}) => {
  const url = resolveMediaUrl(key);
  const baseClass = className ? `${className} review-project-media` : 'review-project-media';
  if (isVideoKey(key)) {
    return (
      <video
        key={url}
        className={`${baseClass} review-project-media--video`}
        src={url}
        controls={options.controls ?? true}
        muted={options.muted ?? false}
        loop={options.loop ?? false}
        playsInline
        preload={options.preload ?? 'metadata'}
      />
    );
  }
  return <img key={url} className={baseClass} src={url} alt="" loading="lazy" />;
};

const renderThumbMedia = (key: string) =>
  renderMedia(key, 'review-project-detail__thumb-media', {
    controls: false,
    muted: true,
    loop: true,
    preload: 'metadata',
  });
