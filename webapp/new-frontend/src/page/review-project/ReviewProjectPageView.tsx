import '../../components/chip-dropdown.css';
import '../../components/filters/filter-chip.css';
import './review-project-page.css';

import type { VirtualItem } from '@tanstack/react-virtual';
import type React from 'react';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';

import type { ApiReviewProjectDetail, ApiReviewProjectTextUnit } from '../../api/review-projects';
import {
  REVIEW_PROJECT_STATUS_LABELS,
  REVIEW_PROJECT_TYPE_LABELS,
} from '../../api/review-projects';
import { ConfirmModal } from '../../components/ConfirmModal';
import {
  type FilterOption,
  MultiSectionFilterChip,
} from '../../components/filters/MultiSectionFilterChip';
import { LocalePill } from '../../components/LocalePill';
import { Pill } from '../../components/Pill';
import { PillDropdown } from '../../components/PillDropdown';
import { getRowHeightPx } from '../../components/virtual/getRowHeightPx';
import { useVirtualRows } from '../../components/virtual/useVirtualRows';
import { VirtualList } from '../../components/virtual/VirtualList';
import type { ReviewProjectMutationControls } from './review-project-mutations';

const Chevron = ({ direction }: { direction: 'left' | 'right' | 'up' | 'down' }) => (
  <svg
    className={`review-project-chevron review-project-chevron--${direction}`}
    viewBox="0 0 10 6"
    aria-hidden="true"
    focusable="false"
  >
    <path
      d="M1 1l4 4 4-4"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
    />
  </svg>
);

type StatusChoice = 'ACCEPTED' | 'NEEDS_REVIEW' | 'NEEDS_TRANSLATION' | 'REJECTED';

const STATUS_CHOICES: Array<{ value: StatusChoice; label: string }> = [
  { value: 'ACCEPTED', label: 'Accepted' },
  { value: 'NEEDS_REVIEW', label: 'To review' },
  { value: 'NEEDS_TRANSLATION', label: 'To translate' },
  { value: 'REJECTED', label: 'Rejected' },
];

type TextUnitVariant = ApiReviewProjectTextUnit['baselineTmTextUnitVariant'];
type DecisionStateChoice = 'PENDING' | 'DECIDED';
type DecisionStateFilter = DecisionStateChoice | 'all';
type StatusFilter = 'all' | 'APPROVED' | 'REVIEW_NEEDED' | 'TRANSLATION_NEEDED' | 'REJECTED';

const SAVING_INDICATOR_MIN_MS = 600;

function mapChoiceToApi(choice: StatusChoice): {
  status: string;
  includedInLocalizedFile: boolean;
} {
  switch (choice) {
    case 'ACCEPTED':
      return { status: 'APPROVED', includedInLocalizedFile: true };
    case 'NEEDS_REVIEW':
      return { status: 'REVIEW_NEEDED', includedInLocalizedFile: true };
    case 'REJECTED':
      return { status: 'TRANSLATION_NEEDED', includedInLocalizedFile: false };
    case 'NEEDS_TRANSLATION':
    default:
      return { status: 'TRANSLATION_NEEDED', includedInLocalizedFile: true };
  }
}

function mapVariantToChoice(status?: string | null, includedInLocalizedFile?: boolean | null) {
  if (includedInLocalizedFile === false) {
    return 'REJECTED' as const;
  }

  switch (status) {
    case 'APPROVED':
      return 'ACCEPTED' as const;
    case 'REVIEW_NEEDED':
      return 'NEEDS_REVIEW' as const;
    case 'TRANSLATION_NEEDED':
    default:
      return 'NEEDS_TRANSLATION' as const;
  }
}

function getEffectiveVariant(textUnit: ApiReviewProjectTextUnit): TextUnitVariant {
  const current = textUnit.currentTmTextUnitVariant;
  return current?.id != null ? current : textUnit.baselineTmTextUnitVariant;
}

function getDecisionState(textUnit: ApiReviewProjectTextUnit): DecisionStateChoice {
  const decision = textUnit.reviewProjectTextUnitDecision;
  if (decision?.decisionState === 'DECIDED' || decision?.decisionState === 'PENDING') {
    return decision.decisionState;
  }
  return decision?.decisionTmTextUnitVariant?.id != null ? 'DECIDED' : 'PENDING';
}

function getStatusKey(variant: TextUnitVariant | null | undefined): string | null {
  if (!variant) {
    return null;
  }
  if (variant.includedInLocalizedFile === false) {
    return 'REJECTED';
  }
  return variant.status ?? null;
}

function statusKeyToLabel(statusKey: string): string {
  switch (statusKey) {
    case 'APPROVED':
      return 'Accepted';
    case 'REVIEW_NEEDED':
      return 'Needs review';
    case 'TRANSLATION_NEEDED':
      return 'Needs translation';
    case 'REJECTED':
      return 'Rejected';
    default:
      return statusKey;
  }
}

function statusKeyToChipClass(statusKey: string | null): string {
  switch (statusKey) {
    case 'APPROVED':
      return 'accepted';
    case 'REVIEW_NEEDED':
      return 'needs-review';
    case 'TRANSLATION_NEEDED':
      return 'needs-translation';
    case 'REJECTED':
      return 'rejected';
    default:
      return 'unknown';
  }
}

const STATUS_FILTER_OPTIONS: Array<FilterOption<StatusFilter>> = [
  { value: 'all', label: 'All statuses' },
  { value: 'APPROVED', label: statusKeyToLabel('APPROVED') },
  { value: 'REVIEW_NEEDED', label: statusKeyToLabel('REVIEW_NEEDED') },
  { value: 'TRANSLATION_NEEDED', label: statusKeyToLabel('TRANSLATION_NEEDED') },
  { value: 'REJECTED', label: statusKeyToLabel('REJECTED') },
];

const DECISION_STATE_OPTIONS: Array<FilterOption<DecisionStateFilter>> = [
  { value: 'all', label: 'All states' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'DECIDED', label: 'Decided' },
];

function normalizeOptional(value: string): string | null {
  return value === '' ? null : value;
}

type DecisionSnapshot = {
  expectedCurrentVariantId: number | null;
  target: string;
  comment: string | null;
  decisionNotes: string | null;
  statusChoice: StatusChoice;
  decisionState: DecisionStateChoice;
};

function buildSnapshot(textUnit: ApiReviewProjectTextUnit): DecisionSnapshot {
  const current =
    textUnit.currentTmTextUnitVariant?.id != null ? textUnit.currentTmTextUnitVariant : null;
  const baseVariant = current ?? textUnit.baselineTmTextUnitVariant;
  const statusChoice = mapVariantToChoice(
    baseVariant?.status ?? null,
    baseVariant?.includedInLocalizedFile ?? null,
  );

  return {
    expectedCurrentVariantId: current?.id ?? null,
    target: baseVariant?.content ?? '',
    comment: baseVariant?.comment ?? null,
    decisionNotes: textUnit.reviewProjectTextUnitDecision?.notes ?? null,
    statusChoice,
    decisionState: getDecisionState(textUnit),
  };
}

function buildSnapshotKey(textUnit: ApiReviewProjectTextUnit, snapshot: DecisionSnapshot): string {
  const baselineId = textUnit.baselineTmTextUnitVariant?.id ?? 'null';
  const currentId = snapshot.expectedCurrentVariantId ?? 'null';
  const decisionVariantId =
    textUnit.reviewProjectTextUnitDecision?.decisionTmTextUnitVariant?.id ?? 'null';
  const decisionNotes = textUnit.reviewProjectTextUnitDecision?.notes ?? '';
  const decisionState = snapshot.decisionState;
  return `${textUnit.id}:${baselineId}:${currentId}:${decisionVariantId}:${decisionNotes}:${decisionState}`;
}

type Props = {
  projectId: number;
  project: ApiReviewProjectDetail | null;
  mutations: ReviewProjectMutationControls;
};

export function ReviewProjectPageView({ projectId, project, mutations }: Props) {
  const locale = project?.locale ?? null;
  const localeTag = locale?.bcp47Tag ?? '';
  const textUnits = useMemo<ApiReviewProjectTextUnit[]>(
    () => project?.reviewProjectTextUnits ?? [],
    [project?.reviewProjectTextUnits],
  );

  const layoutRef = useRef<HTMLDivElement>(null);
  const [listWidthPct, setListWidthPct] = useState(20);
  const [lastListWidthPct, setLastListWidthPct] = useState(20);
  const [isListCollapsed, setIsListCollapsed] = useState(false);
  const [isResizing, setIsResizing] = useState(false);

  useEffect(() => {
    if (layoutRef.current) {
      layoutRef.current.style.setProperty('--review-list-width', `${listWidthPct}%`);
    }
  }, [listWidthPct]);

  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('all');
  const [stateFilter, setStateFilter] = useState<DecisionStateFilter>('all');
  const [selectedTextUnitId, setSelectedTextUnitId] = useState<number | null>(null);
  const previousSelectedRef = useRef<number | null>(null);
  const [isScreenshotModalOpen, setIsScreenshotModalOpen] = useState(false);
  const [selectedScreenshotIdx, setSelectedScreenshotIdx] = useState<number>(0);
  const { onDismissValidationSave, showValidationDialog } = mutations;

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    return textUnits.filter((tu) => {
      if (!tu) return false;
      const statusKey = getStatusKey(getEffectiveVariant(tu));
      if (statusFilter !== 'all' && statusKey !== statusFilter) {
        return false;
      }
      if (stateFilter !== 'all' && getDecisionState(tu) !== stateFilter) {
        return false;
      }
      if (!term) return true;
      const haystacks = [
        tu.tmTextUnit?.name,
        tu.tmTextUnit?.content,
        tu.baselineTmTextUnitVariant?.content,
        tu.currentTmTextUnitVariant?.content,
      ]
        .filter(Boolean)
        .map((s) => String(s).toLowerCase());
      return haystacks.some((h) => h.includes(term));
    });
  }, [search, stateFilter, statusFilter, textUnits]);

  const screenshotImages = useMemo(
    () => project?.reviewProjectRequest?.screenshotImageIds ?? [],
    [project?.reviewProjectRequest?.screenshotImageIds],
  );

  useEffect(() => {
    if (!screenshotImages.length) {
      setSelectedScreenshotIdx(0);
      return;
    }
    setSelectedScreenshotIdx((idx) => Math.min(idx, screenshotImages.length - 1));
  }, [screenshotImages]);

  const selectedTextUnit = useMemo(
    () => filtered.find((tu) => tu.id === selectedTextUnitId),
    [filtered, selectedTextUnitId],
  );

  useEffect(() => {
    if (filtered.length === 0) {
      setSelectedTextUnitId(null);
      return;
    }
    if (selectedTextUnitId == null || !filtered.some((tu) => tu.id === selectedTextUnitId)) {
      setSelectedTextUnitId(filtered[0]?.id ?? null);
    }
  }, [filtered, selectedTextUnitId]);

  useEffect(() => {
    if (
      previousSelectedRef.current !== null &&
      previousSelectedRef.current !== selectedTextUnitId &&
      showValidationDialog
    ) {
      onDismissValidationSave();
    }
    previousSelectedRef.current = selectedTextUnitId;
  }, [onDismissValidationSave, selectedTextUnitId, showValidationDialog]);

  const estimateRowHeight = useCallback(
    () =>
      getRowHeightPx({
        cssVariable: '--review-project-row-height',
        defaultRem: 6,
      }),
    [],
  );

  const getItemKey = useCallback((index: number) => filtered[index]?.id ?? index, [filtered]);

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
        ? filtered.findIndex((tu) => tu.id === selectedTextUnitId)
        : -1;

      if (event.key === 'ArrowDown' || event.key === 'j') {
        event.preventDefault();
        const nextIndex = Math.min(filtered.length - 1, idx + 1);
        const nextId = filtered[nextIndex]?.id ?? null;
        if (nextId != null) {
          setSelectedTextUnitId(nextId);
          scrollToIndex(nextIndex, { align: 'center' });
        }
      } else if (event.key === 'ArrowUp' || event.key === 'k') {
        event.preventDefault();
        const prevIndex = Math.max(0, idx <= 0 ? 0 : idx - 1);
        const prevId = filtered[prevIndex]?.id ?? null;
        if (prevId != null) {
          setSelectedTextUnitId(prevId);
          scrollToIndex(prevIndex, { align: 'center' });
        }
      }
    },
    [filtered, scrollToIndex, selectedTextUnitId],
  );

  useEffect(() => {
    window.addEventListener('keydown', handleKeyNav);
    return () => window.removeEventListener('keydown', handleKeyNav);
  }, [handleKeyNav]);

  const collapseList = useCallback(() => {
    setIsListCollapsed(true);
    setListWidthPct(0);
  }, []);

  const expandList = useCallback(() => {
    setIsListCollapsed(false);
    setListWidthPct(lastListWidthPct || 20);
  }, [lastListWidthPct]);

  const toggleList = useCallback(() => {
    if (isListCollapsed) {
      expandList();
    } else {
      if (listWidthPct > 0) {
        setLastListWidthPct(listWidthPct);
      }
      collapseList();
    }
  }, [collapseList, expandList, isListCollapsed, listWidthPct]);

  const startResize = useCallback(
    (event: React.MouseEvent) => {
      event.preventDefault();
      setIsResizing(true);
      const onMove = (e: MouseEvent) => {
        if (!layoutRef.current) return;
        const rect = layoutRef.current.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const pct = Math.min(75, Math.max(0, (x / rect.width) * 100));
        if (pct <= 8) {
          if (listWidthPct > 0) {
            setLastListWidthPct(listWidthPct);
          }
          collapseList();
          return;
        }
        if (isListCollapsed) {
          setIsListCollapsed(false);
        }
        setLastListWidthPct(pct);
        setListWidthPct(pct);
      };
      const onUp = () => {
        setIsResizing(false);
        window.removeEventListener('mousemove', onMove);
        window.removeEventListener('mouseup', onUp);
      };
      window.addEventListener('mousemove', onMove);
      window.addEventListener('mouseup', onUp);
    },
    [collapseList, isListCollapsed, listWidthPct],
  );

  if (!project) {
    return <div>No project data for id {projectId}</div>;
  }

  return (
    <div className="review-project-page">
      <ReviewProjectHeader projectId={projectId} project={project} textUnits={textUnits} />

      <div
        className={`review-project-page__content${isListCollapsed ? ' review-project-page__content--collapsed' : ''}`}
        ref={layoutRef}
      >
        <section
          className={`review-project-page__list-pane${
            isListCollapsed ? ' review-project-page__list-pane--collapsed' : ''
          }`}
        >
          <div className="review-project-page__controls">
            <input
              className="review-project-page__search-input"
              type="search"
              placeholder="Search source, translation, or id"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <MultiSectionFilterChip
              ariaLabel="Filter text units"
              align="right"
              className="review-project-page__filter-chip"
              classNames={{
                button: 'filter-chip__button',
                panel: 'filter-chip__panel',
                section: 'filter-chip__section',
                label: 'filter-chip__label',
                list: 'filter-chip__list',
                option: 'filter-chip__option',
                helper: 'filter-chip__helper',
              }}
              sections={[
                {
                  kind: 'radio',
                  label: 'Status',
                  options: STATUS_FILTER_OPTIONS as Array<FilterOption<string | number>>,
                  value: statusFilter,
                  onChange: (value) => setStatusFilter(value as StatusFilter),
                },
                {
                  kind: 'radio',
                  label: 'State',
                  options: DECISION_STATE_OPTIONS as Array<FilterOption<string | number>>,
                  value: stateFilter,
                  onChange: (value) => setStateFilter(value as DecisionStateFilter),
                },
              ]}
            />
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
                  onClick: () => setSelectedTextUnitId(textUnit.id),
                  className:
                    textUnit.id === selectedTextUnitId
                      ? 'review-project-row is-selected'
                      : 'review-project-row',
                },
                content: (
                  <TextUnitRow
                    textUnit={textUnit}
                    isSelected={textUnit.id === selectedTextUnitId}
                  />
                ),
              };
            }}
          />
        </section>
        <div
          className={`review-project-page__resize-handle${
            isResizing ? ' is-resizing' : ''
          }${isListCollapsed ? ' review-project-page__resize-handle--collapsed' : ''}`}
          onMouseDown={startResize}
          role="separator"
          aria-label={isListCollapsed ? 'Expand review list' : 'Collapse review list'}
          aria-orientation="vertical"
          aria-expanded={!isListCollapsed}
        >
          <button
            type="button"
            className="review-project-handle-button review-project-page__collapse-toggle"
            onClick={toggleList}
            onMouseDown={(event) => event.stopPropagation()}
            aria-label={isListCollapsed ? 'Expand review list' : 'Collapse review list'}
            title={isListCollapsed ? 'Expand review list' : 'Collapse review list'}
          >
            <Chevron direction={isListCollapsed ? 'right' : 'left'} />
          </button>
        </div>
        <section className="review-project-page__detail-pane">
          {selectedTextUnit ? (
            <DetailPane
              textUnit={selectedTextUnit}
              localeTag={localeTag}
              mutations={mutations}
              screenshotImages={screenshotImages}
              currentScreenshotIdx={selectedScreenshotIdx}
              onChangeScreenshotIdx={setSelectedScreenshotIdx}
              onOpenGallery={() => setIsScreenshotModalOpen(true)}
            />
          ) : (
            <div className="review-project-page__empty-detail">No text unit selected</div>
          )}
        </section>
      </div>
      {isScreenshotModalOpen ? (
        <ScreenshotOverlay
          screenshotImages={screenshotImages}
          selectedScreenshotIdx={selectedScreenshotIdx}
          onChangeScreenshotIdx={setSelectedScreenshotIdx}
          onClose={() => setIsScreenshotModalOpen(false)}
        />
      ) : null}
      <ConfirmModal
        open={mutations.showValidationDialog}
        title="Translation check failed"
        body={mutations.validationDialogBody}
        confirmLabel="Save anyway"
        cancelLabel="Keep editing"
        onConfirm={mutations.onConfirmValidationSave}
        onCancel={mutations.onDismissValidationSave}
      />
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
  const name = textUnit.tmTextUnit?.name ?? null;
  const source = textUnit.tmTextUnit?.content ?? null;
  const target = getEffectiveVariant(textUnit)?.content ?? null;
  return (
    <div className="review-project-row__inner" data-selected={isSelected ? 'true' : 'false'}>
      <div className="review-project-row__name" title={name != null ? String(name) : undefined}>
        {name || `Text unit ${textUnit.id}`}
      </div>
      <div className="review-project-row__strings">
        <div className="review-project-row__string-line" title={source ?? undefined}>
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
  localeTag,
  mutations,
  screenshotImages,
  currentScreenshotIdx,
  onChangeScreenshotIdx,
  onOpenGallery,
}: {
  textUnit: ApiReviewProjectTextUnit;
  localeTag: string;
  mutations: ReviewProjectMutationControls;
  screenshotImages: string[];
  currentScreenshotIdx: number;
  onChangeScreenshotIdx: (index: number) => void;
  onOpenGallery: () => void;
}) {
  const [isScreenshotsCollapsed, setIsScreenshotsCollapsed] = useState(false);
  const [heroHeight, setHeroHeight] = useState<number | null>(null);
  const [isHeroResizing, setIsHeroResizing] = useState(false);
  const [lastHeroHeight, setLastHeroHeight] = useState<number | null>(null);
  const [showBaseline, setShowBaseline] = useState(false);
  const [showStaleDecision, setShowStaleDecision] = useState(false);
  const [showSavingIndicator, setShowSavingIndicator] = useState(false);
  const heroRef = useRef<HTMLDivElement | null>(null);
  const didAutoAcceptRef = useRef(false);
  const savingIndicatorStartRef = useRef<number | null>(null);
  const savingIndicatorTimeoutRef = useRef<number | null>(null);
  const workbenchTextUnitId = textUnit.tmTextUnit?.id ?? null;
  const repositoryId = textUnit.tmTextUnit?.asset?.repository?.id ?? null;
  const textUnitName = textUnit.tmTextUnit?.name ?? `Text unit ${textUnit.id}`;
  const source = textUnit.tmTextUnit?.content ?? null;
  const sourceComment = textUnit.tmTextUnit?.comment ?? null;
  const baselineVariant = textUnit.baselineTmTextUnitVariant;
  const baselineStatusKey = getStatusKey(baselineVariant);
  const snapshot = useMemo(() => buildSnapshot(textUnit), [textUnit]);
  const snapshotKey = useMemo(() => buildSnapshotKey(textUnit, snapshot), [textUnit, snapshot]);

  const [draftTarget, setDraftTarget] = useState(snapshot.target);
  const [draftStatusChoice, setDraftStatusChoice] = useState<StatusChoice>(snapshot.statusChoice);
  const [draftComment, setDraftComment] = useState(snapshot.comment ?? '');
  const [draftDecisionNotes, setDraftDecisionNotes] = useState(snapshot.decisionNotes ?? '');
  const isMutationActive = mutations.activeTextUnitId === textUnit.id;
  const isSavingGlobal = mutations.isSaving;
  const isSaving = isMutationActive && isSavingGlobal;
  const errorMessage = isMutationActive ? mutations.errorMessage : null;
  const conflictTextUnit = isMutationActive ? mutations.conflictTextUnit : null;
  const decision = textUnit.reviewProjectTextUnitDecision;
  const decisionVariant = decision?.decisionTmTextUnitVariant ?? null;
  const decisionVariantId = decisionVariant?.id ?? null;
  const currentVariantId = textUnit.currentTmTextUnitVariant?.id ?? null;
  const isDecisionStale =
    decision?.decisionState === 'DECIDED' &&
    decisionVariantId != null &&
    currentVariantId != null &&
    decisionVariantId !== currentVariantId;

  useEffect(() => {
    setDraftTarget(snapshot.target);
    setDraftStatusChoice(snapshot.statusChoice);
    setDraftComment(snapshot.comment ?? '');
    setDraftDecisionNotes(snapshot.decisionNotes ?? '');
    didAutoAcceptRef.current = false;
  }, [snapshot, snapshotKey]);

  const draftStatusApi = mapChoiceToApi(draftStatusChoice);
  const snapshotStatusApi = mapChoiceToApi(snapshot.statusChoice);
  const draftCommentNormalized = normalizeOptional(draftComment);
  const draftDecisionNotesNormalized = normalizeOptional(draftDecisionNotes);
  const isDirty =
    draftTarget !== snapshot.target ||
    draftStatusApi.status !== snapshotStatusApi.status ||
    draftStatusApi.includedInLocalizedFile !== snapshotStatusApi.includedInLocalizedFile ||
    draftCommentNormalized !== snapshot.comment ||
    draftDecisionNotesNormalized !== snapshot.decisionNotes;
  const isRejected = draftStatusApi.includedInLocalizedFile === false;

  const requestSaveDecision = useCallback(() => {
    mutations.onRequestSaveDecision({
      textUnitId: textUnit.id,
      tmTextUnitId: workbenchTextUnitId,
      target: draftTarget,
      comment: draftCommentNormalized,
      status: draftStatusApi.status,
      includedInLocalizedFile: draftStatusApi.includedInLocalizedFile,
      expectedCurrentTmTextUnitVariantId: snapshot.expectedCurrentVariantId,
      decisionNotes: draftDecisionNotesNormalized,
    });
  }, [
    draftCommentNormalized,
    draftDecisionNotesNormalized,
    draftStatusApi.includedInLocalizedFile,
    draftStatusApi.status,
    draftTarget,
    mutations,
    snapshot.expectedCurrentVariantId,
    textUnit.id,
    workbenchTextUnitId,
  ]);

  const requestDecisionState = useCallback(
    (decisionState: DecisionStateChoice) => {
      mutations.onRequestDecisionState({
        textUnitId: textUnit.id,
        decisionState,
        expectedCurrentTmTextUnitVariantId: snapshot.expectedCurrentVariantId,
      });
    },
    [mutations, snapshot.expectedCurrentVariantId, textUnit.id],
  );

  const handleReset = useCallback(() => {
    setDraftTarget(snapshot.target);
    setDraftStatusChoice(snapshot.statusChoice);
    setDraftComment(snapshot.comment ?? '');
    setDraftDecisionNotes(snapshot.decisionNotes ?? '');
    didAutoAcceptRef.current = false;
  }, [snapshot]);

  const handleSave = useCallback(() => {
    requestSaveDecision();
  }, [requestSaveDecision]);

  const isDecided = snapshot.decisionState === 'DECIDED';

  const handleToggleDecisionState = useCallback(() => {
    const nextState: DecisionStateChoice = isDecided ? 'PENDING' : 'DECIDED';
    requestDecisionState(nextState);
  }, [isDecided, requestDecisionState]);

  const handleUseCurrent = useCallback(() => {
    if (!isMutationActive) {
      return;
    }
    mutations.onUseConflictCurrent();
  }, [isMutationActive, mutations]);

  const handleOverwrite = useCallback(() => {
    if (!isMutationActive) {
      return;
    }
    mutations.onOverwriteConflict();
  }, [isMutationActive, mutations]);

  const conflictVariant = conflictTextUnit ? getEffectiveVariant(conflictTextUnit) : null;
  const conflictStatusKey = getStatusKey(conflictVariant);

  useEffect(() => {
    if (!heroRef.current || heroHeight != null || !screenshotImages.length) return;
    const rect = heroRef.current.getBoundingClientRect();
    const containerHeight = heroRef.current.parentElement?.clientHeight ?? rect.height;
    if (containerHeight) {
      const minHeight = 140;
      const targetHeight = Math.max(minHeight, Math.floor(containerHeight * 0.4));
      setHeroHeight(targetHeight);
      setLastHeroHeight(targetHeight);
    }
  }, [heroHeight, screenshotImages.length]);

  useEffect(() => {
    if (isScreenshotsCollapsed) {
      if (heroHeight != null) {
        setLastHeroHeight(heroHeight);
      }
      return;
    }
    if (heroHeight == null && lastHeroHeight != null) {
      setHeroHeight(lastHeroHeight);
    }
  }, [heroHeight, isScreenshotsCollapsed, lastHeroHeight]);

  useEffect(() => {
    setShowBaseline(false);
    setShowStaleDecision(false);
  }, [textUnit.id]);

  useEffect(() => {
    if (isSaving) {
      if (savingIndicatorTimeoutRef.current != null) {
        window.clearTimeout(savingIndicatorTimeoutRef.current);
        savingIndicatorTimeoutRef.current = null;
      }
      savingIndicatorStartRef.current = Date.now();
      setShowSavingIndicator(true);
      return;
    }

    if (!showSavingIndicator) {
      return;
    }

    const startedAt = savingIndicatorStartRef.current;
    const elapsed = startedAt != null ? Date.now() - startedAt : SAVING_INDICATOR_MIN_MS;
    const remaining = SAVING_INDICATOR_MIN_MS - elapsed;

    if (remaining <= 0) {
      setShowSavingIndicator(false);
      savingIndicatorStartRef.current = null;
      return;
    }

    savingIndicatorTimeoutRef.current = window.setTimeout(() => {
      setShowSavingIndicator(false);
      savingIndicatorStartRef.current = null;
      savingIndicatorTimeoutRef.current = null;
    }, remaining);

    return () => {
      if (savingIndicatorTimeoutRef.current != null) {
        window.clearTimeout(savingIndicatorTimeoutRef.current);
        savingIndicatorTimeoutRef.current = null;
      }
    };
  }, [isSaving, showSavingIndicator]);

  return (
    <div className="review-project-detail">
      <div className="review-project-detail__header">
        <div className="review-project-detail__title" />
      </div>
      <div
        className={`review-project-detail__hero${
          screenshotImages.length ? ' review-project-detail__hero--has-shots' : ''
        }${isScreenshotsCollapsed ? ' review-project-detail__hero--collapsed' : ''}`}
        ref={heroRef}
        style={
          !isScreenshotsCollapsed && heroHeight != null ? { height: `${heroHeight}px` } : undefined
        }
      >
        {screenshotImages.length ? (
          <>
            <div className="review-project-detail__shots-badge">
              {currentScreenshotIdx + 1} / {screenshotImages.length}
            </div>
            {isScreenshotsCollapsed ? null : (
              <div className="review-project-detail__gallery review-project-detail__gallery--hero">
                <button
                  type="button"
                  className="review-project-detail__gallery-nav"
                  onClick={() =>
                    onChangeScreenshotIdx(
                      (currentScreenshotIdx - 1 + screenshotImages.length) %
                        screenshotImages.length,
                    )
                  }
                  aria-label="Previous screenshot"
                >
                  ‹
                </button>
                <button
                  type="button"
                  className="review-project-detail__gallery-main review-project-detail__gallery-main--hero"
                  onClick={() => onOpenGallery()}
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
            )}
          </>
        ) : (
          <div className="review-project-detail__shots-empty">No screenshots attached.</div>
        )}
      </div>
      {screenshotImages.length ? (
        <div
          className={`review-project-detail__hero-resize-handle${
            isHeroResizing ? ' is-resizing' : ''
          }${isScreenshotsCollapsed ? ' review-project-detail__hero-resize-handle--collapsed' : ''}`}
          onMouseDown={(event) => {
            if (!heroRef.current) return;
            event.preventDefault();
            if (isScreenshotsCollapsed) {
              setIsScreenshotsCollapsed(false);
            }
            setIsHeroResizing(true);
            const rect = heroRef.current.getBoundingClientRect();
            if (heroHeight == null) {
              setHeroHeight(rect.height);
              setLastHeroHeight(rect.height);
            }
            const minHeight = 140;
            const containerHeight =
              heroRef.current.parentElement?.clientHeight ?? window.innerHeight;
            const maxHeight = Math.max(minHeight, Math.floor(containerHeight * 0.6));
            const onMove = (e: MouseEvent) => {
              const rawNext = Math.min(maxHeight, Math.max(0, e.clientY - rect.top));
              if (rawNext <= 80) {
                if (!isScreenshotsCollapsed) {
                  setIsScreenshotsCollapsed(true);
                }
                return;
              }
              if (isScreenshotsCollapsed) {
                setIsScreenshotsCollapsed(false);
              }
              const next = Math.max(minHeight, rawNext);
              setHeroHeight(next);
              setLastHeroHeight(next);
            };
            const onUp = () => {
              setIsHeroResizing(false);
              window.removeEventListener('mousemove', onMove);
              window.removeEventListener('mouseup', onUp);
            };
            window.addEventListener('mousemove', onMove);
            window.addEventListener('mouseup', onUp);
          }}
          role="separator"
          aria-label="Resize screenshots panel"
          aria-orientation="horizontal"
        >
          <button
            type="button"
            className="review-project-handle-button review-project-detail__hero-toggle"
            onClick={() =>
              setIsScreenshotsCollapsed((prev) => {
                if (!prev && heroHeight != null) {
                  setLastHeroHeight(heroHeight);
                }
                if (prev && lastHeroHeight != null) {
                  setHeroHeight(lastHeroHeight);
                }
                return !prev;
              })
            }
            onMouseDown={(event) => event.stopPropagation()}
            aria-label={isScreenshotsCollapsed ? 'Show screenshots' : 'Hide screenshots'}
            title={isScreenshotsCollapsed ? 'Show screenshots' : 'Hide screenshots'}
          >
            <Chevron direction={isScreenshotsCollapsed ? 'down' : 'up'} />
          </button>
        </div>
      ) : null}
      <div className="review-project-detail__layout">
        <div className="review-project-detail__main">
          {conflictTextUnit ? (
            <div className="review-project-detail__conflict" role="alert">
              <div className="review-project-detail__conflict-title">
                Translation changed while you were working.
              </div>
              <div className="review-project-detail__conflict-current">
                <span className="review-project-detail__meta-label">Current</span>{' '}
                {conflictVariant?.content ?? '—'}
                {conflictStatusKey ? (
                  <span className="review-project-detail__conflict-status">
                    {statusKeyToLabel(conflictStatusKey)}
                  </span>
                ) : null}
              </div>
              <div className="review-project-detail__conflict-actions">
                <button
                  type="button"
                  className="review-project-detail__actions-button"
                  onClick={handleUseCurrent}
                  disabled={isSavingGlobal}
                >
                  Use current
                </button>
                <button
                  type="button"
                  className="review-project-detail__actions-button review-project-detail__actions-button--primary"
                  onClick={handleOverwrite}
                  disabled={isSavingGlobal}
                >
                  Overwrite
                </button>
              </div>
            </div>
          ) : null}

          {showBaseline && baselineVariant?.id != null ? (
            <div className="review-project-detail__field review-project-detail__field--baseline">
              <div className="review-project-detail__label-row">
                <div className="review-project-detail__label">Baseline</div>
                {baselineStatusKey ? (
                  <Pill
                    className={`review-project-detail__status-pill review-project-detail__status-chip review-project-detail__status-chip--${statusKeyToChipClass(
                      baselineStatusKey,
                    )}`}
                  >
                    {statusKeyToLabel(baselineStatusKey)}
                  </Pill>
                ) : null}
              </div>
              <textarea
                className="review-project-detail__input review-project-detail__input--baseline"
                value={baselineVariant.content ?? ''}
                readOnly
                rows={6}
              />
            </div>
          ) : null}

          {showStaleDecision ? (
            <div className="review-project-detail__field review-project-detail__field--baseline">
              <div className="review-project-detail__label-row">
                <div className="review-project-detail__label">Stale decision</div>
                {(() => {
                  const staleStatusKey = getStatusKey(decisionVariant);
                  return staleStatusKey ? (
                    <Pill
                      className={`review-project-detail__status-pill review-project-detail__status-chip review-project-detail__status-chip--${statusKeyToChipClass(
                        staleStatusKey,
                      )}`}
                    >
                      {statusKeyToLabel(staleStatusKey)}
                    </Pill>
                  ) : null;
                })()}
              </div>
              <textarea
                className="review-project-detail__input review-project-detail__input--baseline"
                value={decisionVariant?.content ?? ''}
                readOnly
                rows={6}
              />
            </div>
          ) : null}

          <div className="review-project-detail__field">
            <div className="review-project-detail__label-row">
              <div className="review-project-detail__label">Translation</div>
              <div className="review-project-detail__label-actions">
                {isDecisionStale ? (
                  <button
                    type="button"
                    className="review-project-detail__baseline-toggle"
                    onClick={() => setShowStaleDecision((prev) => !prev)}
                    title="Show the translation used for the decision"
                  >
                    {showStaleDecision ? 'Hide stale decision' : 'Show stale decision'}
                  </button>
                ) : null}
                {baselineVariant?.id != null ? (
                  <button
                    type="button"
                    className="review-project-detail__baseline-toggle"
                    onClick={() => setShowBaseline((prev) => !prev)}
                  >
                    {showBaseline ? 'Hide baseline' : 'Show baseline'}
                  </button>
                ) : null}
              </div>
            </div>
            <textarea
              className={`review-project-detail__input${
                isRejected ? ' review-project-detail__input--rejected' : ''
              }`}
              value={draftTarget}
              onChange={(event) => {
                const next = event.target.value;
                if (!didAutoAcceptRef.current && next !== snapshot.target) {
                  setDraftStatusChoice('ACCEPTED');
                  didAutoAcceptRef.current = true;
                }
                setDraftTarget(next);
              }}
              rows={6}
            />
          </div>

          <div className="review-project-detail__editor-controls">
            <div className="review-project-detail__field review-project-detail__field--status">
              <PillDropdown
                value={draftStatusChoice}
                options={STATUS_CHOICES.map((option) => ({
                  value: option.value,
                  label: option.label,
                }))}
                onChange={(next) => setDraftStatusChoice(next)}
                ariaLabel="Translation status"
                className="review-project-detail__status-dropdown"
              />
            </div>
            <div
              className={`review-project-detail__saving-indicator${
                showSavingIndicator ? ' is-active' : ''
              }`}
              role="status"
              aria-live="polite"
              aria-hidden={!showSavingIndicator}
            >
              <span className="spinner" aria-hidden="true" />
              <span>Saving…</span>
            </div>
            <div className="review-project-detail__editor-actions">
              <button
                type="button"
                className="review-project-detail__actions-button"
                onClick={handleToggleDecisionState}
                disabled={isDirty || isSavingGlobal}
              >
                {isDecided ? 'Mark pending' : 'Mark decided'}
              </button>
              <button
                type="button"
                className="review-project-detail__actions-button"
                onClick={handleReset}
                disabled={!isDirty || isSavingGlobal}
              >
                Reset
              </button>
              <button
                type="button"
                className="review-project-detail__actions-button review-project-detail__actions-button--primary"
                onClick={handleSave}
                disabled={!isDirty || isSavingGlobal}
              >
                Save
              </button>
            </div>
          </div>

          <div className="review-project-detail__field">
            <div className="review-project-detail__label">Comment on translation</div>
            <textarea
              className="review-project-detail__input review-project-detail__input--compact"
              value={draftComment}
              onChange={(event) => setDraftComment(event.target.value)}
              placeholder="Explain why you chose this translation (if not obvious)."
              rows={1}
            />
          </div>

          <div className="review-project-detail__field">
            <div className="review-project-detail__label">Decision notes</div>
            <textarea
              className="review-project-detail__input review-project-detail__input--compact"
              value={draftDecisionNotes}
              onChange={(event) => setDraftDecisionNotes(event.target.value)}
              placeholder="Explain why the baseline translation was bad (to improve AI translation)."
              rows={1}
            />
          </div>

          {errorMessage ? <div className="review-project-detail__error">{errorMessage}</div> : null}
        </div>

        <div className="review-project-detail__side">
          <div className="review-project-detail__field">
            <div className="review-project-detail__label">Source</div>
            <div className="review-project-detail__value">{source || '—'}</div>
          </div>

          <div className="review-project-detail__field">
            <div className="review-project-detail__label">Comment</div>
            <div className="review-project-detail__value">{sourceComment ?? '—'}</div>
          </div>

          <div className="review-project-detail__field">
            <div className="review-project-detail__label">Id</div>
            <div className="review-project-detail__value review-project-detail__value--meta">
              <span className="review-project-detail__title-text">{textUnitName}</span>
            </div>
            {workbenchTextUnitId != null ? (
              <Link
                className="pill review-project-detail__pill-link review-project-detail__pill-link--always"
                to={{
                  pathname: '/workbench',
                  search: `?tmTextUnitId=${encodeURIComponent(
                    String(workbenchTextUnitId),
                  )}${localeTag ? `&locale=${encodeURIComponent(localeTag)}` : ''}${
                    repositoryId != null ? `&repo=${encodeURIComponent(String(repositoryId))}` : ''
                  }`,
                }}
                state={{
                  workbenchSearch: {
                    searchAttribute: 'tmTextUnitIds',
                    searchType: 'exact',
                    searchText: String(workbenchTextUnitId),
                    localeTags: [localeTag],
                    repositoryIds: repositoryId != null ? [repositoryId] : [],
                  },
                }}
                title="Open this string in Workbench"
              >
                Open in Workbench
              </Link>
            ) : null}
          </div>
        </div>
      </div>
    </div>
  );
}

function ScreenshotOverlay({
  screenshotImages,
  selectedScreenshotIdx,
  onChangeScreenshotIdx,
  onClose,
}: {
  screenshotImages: string[];
  selectedScreenshotIdx: number;
  onChangeScreenshotIdx: (index: number) => void;
  onClose: () => void;
}) {
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  return (
    <div
      className="review-project-screenshot-overlay"
      role="dialog"
      aria-modal="true"
      aria-label="Screenshot gallery"
    >
      <div className="review-project-screenshot-modal">
        <div className="review-project-screenshot-modal__header">
          <div className="review-project-screenshot-modal__header-group review-project-screenshot-modal__header-group--left">
            <button
              type="button"
              className="review-project-page__header-back-link review-project-screenshot-modal__back"
              onClick={onClose}
              aria-label="Back to review project"
              title="Back to review project"
            >
              <svg
                className="review-project-page__header-back-icon"
                viewBox="0 0 24 24"
                aria-hidden="true"
                focusable="false"
              >
                <path
                  d="M20 12H6m0 0l5-5m-5 5l5 5"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </button>
            <span className="review-project-page__header-name review-project-screenshot-modal__title">
              Screenshots
            </span>
          </div>
          <div className="review-project-screenshot-modal__header-group review-project-screenshot-modal__header-group--center">
            {screenshotImages.length ? (
              <span className="review-project-screenshot-modal__count">
                <span className="review-project-screenshot-modal__count-number">
                  {selectedScreenshotIdx + 1}
                </span>
                <span className="review-project-screenshot-modal__count-sep"> / </span>
                <span className="review-project-screenshot-modal__count-number">
                  {screenshotImages.length}
                </span>
              </span>
            ) : null}
          </div>
          <div className="review-project-screenshot-modal__header-group review-project-screenshot-modal__header-group--right" />
        </div>
        {screenshotImages.length ? (
          <div className="review-project-screenshot-modal__gallery">
            <button
              type="button"
              className="review-project-screenshot-lightbox__nav review-project-screenshot-lightbox__nav--prev"
              onClick={() =>
                onChangeScreenshotIdx(
                  (selectedScreenshotIdx - 1 + screenshotImages.length) % screenshotImages.length,
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
                onChangeScreenshotIdx((selectedScreenshotIdx + 1) % screenshotImages.length)
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
                  onClick={() => onChangeScreenshotIdx(idx)}
                  title="Click to preview"
                >
                  {renderThumbMedia(key)}
                </button>
              );
            })}
          </div>
        ) : null}
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
  const { dueDate, textUnitCount, wordCount, status, type } = project;
  const name = project.reviewProjectRequest?.name ?? null;
  const locale = project.locale ?? null;
  const textUnits = useMemo(() => textUnitsProp ?? [], [textUnitsProp]);
  const locales = useMemo(() => (locale ? [locale] : []), [locale]);

  const { selectedCount, progressPercent, progressTitle } = useMemo(() => {
    const selected = textUnits?.length ?? 0;
    const decided = textUnits?.filter((tu) => getDecisionState(tu) === 'DECIDED').length ?? 0;
    const percent = selected > 0 ? Math.round((decided / selected) * 100) : 0;
    const title = selected > 0 ? `${decided}/${selected}` : 'No text units';
    return {
      selectedCount: selected,
      progressPercent: percent,
      progressTitle: title,
    };
  }, [textUnits]);

  return (
    <header className="review-project-page__header">
      <div className="review-project-page__header-row">
        <div className="review-project-page__header-group review-project-page__header-group--left">
          <Link
            className="review-project-page__header-back-link"
            to="/review-projects"
            aria-label="Back to review projects"
            title="Back to review projects"
          >
            <svg
              className="review-project-page__header-back-icon"
              viewBox="0 0 24 24"
              aria-hidden="true"
              focusable="false"
            >
              <path
                d="M20 12H6m0 0l5-5m-5 5l5 5"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </Link>
          <span className="review-project-page__header-name">{name ?? `Project ${projectId}`}</span>
          <span className="review-project-page__header-pills">
            <Pill
              className={`review-project-page__header-pill review-project-page__header-pill--type-${type}`}
            >
              {REVIEW_PROJECT_TYPE_LABELS[type]}
            </Pill>
            <Pill
              className={`review-project-page__header-pill review-project-page__header-pill--status-${status.toLowerCase()}`}
            >
              {REVIEW_PROJECT_STATUS_LABELS[status]}
            </Pill>
          </span>
          <div className="review-project-page__header-locale-row">
            {locales.length > 0 ? (
              locales.map((locale) => {
                const tag = locale.bcp47Tag ?? '';
                return (
                  <LocalePill
                    key={String(locale.id ?? (tag || 'unknown-locale'))}
                    bcp47Tag={tag}
                    displayName={tag}
                    labelMode="tag"
                    className="review-project-page__header-locale-pill"
                  />
                );
              })
            ) : (
              <span className="review-project-page__header-muted">No locale</span>
            )}
          </div>
        </div>

        <div className="review-project-page__header-group review-project-page__header-group--stats">
          <CountsInline words={wordCount} strings={textUnitCount ?? selectedCount} />
          <span className="review-project-page__header-dot">•</span>
          <div className="review-project-page__header-progress">
            <span className="review-project-page__header-progress-label" title={progressTitle}>
              {progressPercent}%
            </span>
            <ProgressBar percent={progressPercent} title={progressTitle} />
          </div>
        </div>

        <div className="review-project-page__header-group review-project-page__header-group--meta">
          <span>Due {formatDate(dueDate)}</span>
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
    <span className="review-project-page__header-count-line">
      <span className="review-project-page__header-count">{formatNumber(words)}</span>
      <span className="review-project-page__header-muted">&nbsp;words</span>
      <span className="review-project-page__header-count-sep">&nbsp;·&nbsp;</span>
      <span className="review-project-page__header-count">{formatNumber(strings)}</span>
      <span className="review-project-page__header-muted">&nbsp;strings</span>
    </span>
  );
}

function ProgressBar({ percent, title }: { percent: number; title?: string }) {
  return (
    <div className="review-project-page__header-progress-bar" title={title}>
      <div
        className="review-project-page__header-progress-fill"
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

const VIDEO_EXTENSIONS = ['.mp4', '.mov', '.webm', '.ogv', '.ogg', '.m4v', '.mkv'];

const resolveMediaUrl = (key: string) => {
  const isExternal =
    /^https?:\/\//i.test(key) ||
    key.startsWith('//') ||
    key.startsWith('data:') ||
    key.startsWith('blob:');
  return isExternal ? key : `/api/images/${encodeURIComponent(key)}`;
};

const isVideoKey = (key: string) => {
  const lower = key.split('?')[0].toLowerCase();
  return (
    key.startsWith('data:video') ||
    key.startsWith('blob:') ||
    VIDEO_EXTENSIONS.some((ext) => lower.endsWith(ext))
  );
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
