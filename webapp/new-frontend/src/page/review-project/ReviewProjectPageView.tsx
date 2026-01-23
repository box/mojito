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
import { LocalePill } from '../../components/LocalePill';
import { Modal } from '../../components/Modal';
import { Pill } from '../../components/Pill';
import { getRowHeightPx } from '../../components/virtual/getRowHeightPx';
import { useVirtualRows } from '../../components/virtual/useVirtualRows';
import { VirtualList } from '../../components/virtual/VirtualList';

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

type Props = {
  projectId: number;
  project: ApiReviewProjectDetail | null;
};

export function ReviewProjectPageView({ projectId, project }: Props) {
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
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [selectedTextUnitId, setSelectedTextUnitId] = useState<number | null>(null);
  const [isScreenshotModalOpen, setIsScreenshotModalOpen] = useState(false);
  const [selectedScreenshotIdx, setSelectedScreenshotIdx] = useState<number>(0);
  const filterRef = useRef<HTMLDivElement | null>(null);

  const availableStatuses = useMemo(() => {
    const statuses = new Set<string>();
    textUnits.forEach((tu) => {
      if (tu?.tmTextUnitVariant?.status) {
        statuses.add(tu.tmTextUnitVariant.status);
      }
    });
    return Array.from(statuses.values()).sort();
  }, [textUnits]);

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    return textUnits.filter((tu) => {
      if (!tu) return false;
      if (statusFilter !== 'all' && tu.tmTextUnitVariant?.status !== statusFilter) {
        return false;
      }
      if (!term) return true;
      const haystacks = [tu.tmTextUnit?.name, tu.tmTextUnit?.content, tu.tmTextUnitVariant?.content]
        .filter(Boolean)
        .map((s) => String(s).toLowerCase());
      return haystacks.some((h) => h.includes(term));
    });
  }, [search, statusFilter, textUnits]);

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

  useEffect(() => {
    const handleOutside = (event: MouseEvent) => {
      if (!filterRef.current) return;
      if (!filterRef.current.contains(event.target as Node)) {
        setIsFilterOpen(false);
      }
    };
    document.addEventListener('mousedown', handleOutside);
    return () => document.removeEventListener('mousedown', handleOutside);
  }, []);

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
              placeholder="Search source/target/name"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <div className="review-project-page__filter" ref={filterRef}>
              <button
                type="button"
                className="review-project-page__filter-button"
                onClick={() => setIsFilterOpen((open) => !open)}
                aria-label="Filter text units"
                aria-expanded={isFilterOpen}
              >
                <span className="review-project-page__filter-bars" aria-hidden="true">
                  <span />
                  <span />
                  <span />
                </span>
              </button>
              {isFilterOpen ? (
                <div className="review-project-page__filter-panel">
                  <label
                    className="review-project-page__filter-label"
                    htmlFor="review-project-status-filter"
                  >
                    Status
                  </label>
                  <select
                    id="review-project-status-filter"
                    className="review-project-page__control-select review-project-page__control-select--panel"
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
              ) : null}
            </div>
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
  const name = textUnit.tmTextUnit?.name ?? null;
  const source = textUnit.tmTextUnit?.content ?? null;
  const target = textUnit.tmTextUnitVariant?.content ?? null;
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
  screenshotImages,
  currentScreenshotIdx,
  onChangeScreenshotIdx,
  onOpenGallery,
}: {
  textUnit: ApiReviewProjectTextUnit;
  localeTag: string;
  screenshotImages: string[];
  currentScreenshotIdx: number;
  onChangeScreenshotIdx: (index: number) => void;
  onOpenGallery: () => void;
}) {
  const [isScreenshotsCollapsed, setIsScreenshotsCollapsed] = useState(false);
  const [heroHeight, setHeroHeight] = useState<number | null>(null);
  const [isHeroResizing, setIsHeroResizing] = useState(false);
  const [lastHeroHeight, setLastHeroHeight] = useState<number | null>(null);
  const heroRef = useRef<HTMLDivElement | null>(null);
  const workbenchTextUnitId = textUnit.tmTextUnit?.id ?? null;
  const repositoryId = textUnit.tmTextUnit?.asset?.repository?.id ?? null;
  const textUnitName = textUnit.tmTextUnit?.name ?? `Text unit ${textUnit.id}`;
  const source = textUnit.tmTextUnit?.content ?? null;
  const comment = textUnit.tmTextUnit?.comment ?? null;

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
        <div className="review-project-detail__main" />

        <div className="review-project-detail__side">
          <div className="review-project-detail__field">
            <div className="review-project-detail__label">Source</div>
            <div className="review-project-detail__value">{source || '—'}</div>
          </div>

          <div className="review-project-detail__field">
            <div className="review-project-detail__label">Comment</div>
            <div className="review-project-detail__value">{comment || '—'}</div>
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

  const { acceptedCount, selectedCount, progressPercent } = useMemo(() => {
    const selected = textUnits?.length ?? 0;
    const accepted =
      textUnits?.filter((tu) => tu.reviewProjectTextUnitDecision?.tmTextUnitVariantId != null)
        .length ?? 0;
    const percent = selected > 0 ? Math.round((accepted / selected) * 100) : 0;
    return { acceptedCount: accepted, selectedCount: selected, progressPercent: percent };
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
            <span
              className="review-project-page__header-progress-label"
              title={`${acceptedCount}/${selectedCount} accepted`}
            >
              {progressPercent}%
            </span>
            <ProgressBar percent={progressPercent} />
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

function ProgressBar({ percent }: { percent: number }) {
  return (
    <div className="review-project-page__header-progress-bar">
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
