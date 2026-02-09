import '../../components/chip-dropdown.css';
import './review-projects-page.css';

import { type VirtualItem } from '@tanstack/react-virtual';
import { useCallback, useMemo, useRef } from 'react';
import { Link } from 'react-router-dom';

import type { ApiReviewProjectStatus, ApiReviewProjectType } from '../../api/review-projects';
import {
  REVIEW_PROJECT_STATUS_LABELS,
  REVIEW_PROJECT_TYPE_LABELS,
} from '../../api/review-projects';
import {
  type FilterOption,
  MultiSectionFilterChip,
} from '../../components/filters/MultiSectionFilterChip';
import { LocaleMultiSelect, type LocaleOption } from '../../components/LocaleMultiSelect';
import { LocalePill } from '../../components/LocalePill';
import { Pill } from '../../components/Pill';
import { SearchControl } from '../../components/SearchControl';
import { getRowHeightPx } from '../../components/virtual/getRowHeightPx';
import { useMeasuredRowRefs } from '../../components/virtual/useMeasuredRowRefs';
import { useVirtualRows } from '../../components/virtual/useVirtualRows';
import { VirtualList } from '../../components/virtual/VirtualList';
import { getStandardDateQuickRanges } from '../../utils/dateQuickRanges';

type FiltersProps = {
  localeOptions: LocaleOption[];
  selectedLocaleTags: string[];
  onLocaleChange: (value: string[]) => void;
  myLocaleTags?: string[];
  typeOptions: FilterOption<ApiReviewProjectType | 'all'>[];
  typeValue: ApiReviewProjectType | 'all';
  onTypeChange: (value: ApiReviewProjectType | 'all') => void;
  statusOptions: FilterOption<ApiReviewProjectStatus | 'all'>[];
  statusValue: ApiReviewProjectStatus | 'all';
  onStatusChange: (value: ApiReviewProjectStatus | 'all') => void;
  limitOptions: FilterOption<number>[];
  limitValue: number;
  onLimitChange: (value: number) => void;
  createdAfter: string | null;
  createdBefore: string | null;
  onChangeCreatedAfter: (value: string | null) => void;
  onChangeCreatedBefore: (value: string | null) => void;
  dueAfter: string | null;
  dueBefore: string | null;
  onChangeDueAfter: (value: string | null) => void;
  onChangeDueBefore: (value: string | null) => void;
  searchQuery: string;
  onSearchChange: (value: string) => void;
  searchField: 'name' | 'id' | 'requestId';
  onSearchFieldChange: (value: 'name' | 'id' | 'requestId') => void;
  searchType: 'contains' | 'exact' | 'ilike';
  onSearchTypeChange: (value: 'contains' | 'exact' | 'ilike') => void;
};

export type ReviewProjectRow = {
  id: number;
  name: string;
  requestId: number | null;
  type: ApiReviewProjectType;
  status: ApiReviewProjectStatus;
  localeTag: string | null;
  acceptedCount: number;
  textUnitCount: number | null;
  wordCount: number | null;
  dueDate: string | null;
  closeReason?: string | null;
};

export type ReviewProjectsAdminControls = {
  enabled: boolean;
  selectedProjectIds: number[];
  onToggleProjectSelection: (projectId: number) => void;
  onSelectAllVisible: () => void;
  onClearSelection: () => void;
  onBatchStatus: (status: ApiReviewProjectStatus) => void;
  onBatchDelete: () => void;
  isSaving: boolean;
  errorMessage?: string | null;
};

type Props = {
  status: 'loading' | 'error' | 'ready';
  errorMessage?: string;
  errorOnRetry?: () => void;
  onLoadMock?: () => void;
  projects: ReviewProjectRow[];
  filters: FiltersProps;
  requestFilter?: { requestId: number | null; onClear: () => void };
  onRequestIdClick?: (requestId: number) => void;
  canCreate?: boolean;
  adminControls?: ReviewProjectsAdminControls;
};

function CountsInline({ words, strings }: { words: number | null; strings: number | null }) {
  return (
    <div className="review-projects-page__count-line">
      <span className="review-projects-page__count">{formatNumber(words)}</span>
      <span className="review-projects-page__muted">&nbsp;words</span>
      <span className="review-projects-page__count-sep">&nbsp;·&nbsp;</span>
      <span className="review-projects-page__count">{formatNumber(strings)}</span>
      <span className="review-projects-page__muted">&nbsp;strings</span>
    </div>
  );
}

function formatSummaryText(resultCount: number) {
  if (resultCount === 0) {
    return 'No results';
  }
  const noun = resultCount === 1 ? 'result' : 'results';
  return `Showing ${resultCount} ${noun}`;
}

function SummaryBar({
  resultCount,
  totalWords,
  totalStrings,
}: {
  resultCount: number;
  totalWords: number;
  totalStrings: number;
}) {
  return (
    <div className="review-projects-page__summary-bar">
      <div>{formatSummaryText(resultCount)}</div>
      <CountsInline words={totalWords} strings={totalStrings} />
    </div>
  );
}

function AdminBar({
  adminControls,
  visibleCount,
}: {
  adminControls: ReviewProjectsAdminControls;
  visibleCount: number;
}) {
  const selectedCount = adminControls.selectedProjectIds.length;
  const hasSelection = selectedCount > 0;
  const disabled = adminControls.isSaving;

  return (
    <div className="review-projects-page__admin-bar" role="region" aria-label="Admin actions">
      <div className="review-projects-page__admin-left">
        <span className="review-projects-page__admin-count">
          {hasSelection ? `${selectedCount} selected` : 'No selection'}
        </span>
        <button
          type="button"
          className="review-projects-page__admin-link"
          onClick={adminControls.onSelectAllVisible}
          disabled={visibleCount === 0 || disabled}
        >
          Select all visible
        </button>
        <button
          type="button"
          className="review-projects-page__admin-link"
          onClick={adminControls.onClearSelection}
          disabled={!hasSelection || disabled}
        >
          Clear
        </button>
      </div>
      <div className="review-projects-page__admin-actions">
        <button
          type="button"
          className="review-projects-page__admin-button"
          onClick={() => adminControls.onBatchStatus('CLOSED')}
          disabled={!hasSelection || disabled}
        >
          Close
        </button>
        <button
          type="button"
          className="review-projects-page__admin-button"
          onClick={() => adminControls.onBatchStatus('OPEN')}
          disabled={!hasSelection || disabled}
        >
          Reopen
        </button>
        <button
          type="button"
          className="review-projects-page__admin-button review-projects-page__admin-button--danger"
          onClick={adminControls.onBatchDelete}
          disabled={!hasSelection || disabled}
        >
          Delete
        </button>
      </div>
      {adminControls.errorMessage ? (
        <div className="review-projects-page__admin-error">{adminControls.errorMessage}</div>
      ) : null}
    </div>
  );
}

function ContentSection({
  projects,
  rowsParentRef,
  virtualItems,
  totalSize,
  getRowRef,
  adminControls,
  onRequestIdClick,
}: {
  projects: ReviewProjectRow[];
  rowsParentRef: React.RefObject<HTMLDivElement>;
  virtualItems: VirtualItem[];
  totalSize: number;
  getRowRef: (rowId: number) => (element: HTMLDivElement | null) => void;
  adminControls?: ReviewProjectsAdminControls;
  onRequestIdClick?: (requestId: number) => void;
}) {
  const isAdmin = adminControls?.enabled ?? false;
  const selectedProjectIdSet = useMemo(
    () => new Set(adminControls?.selectedProjectIds ?? []),
    [adminControls?.selectedProjectIds],
  );

  if (projects.length === 0) {
    return (
      <div className="review-projects-page__rows-frame">
        <div className="review-projects-page__rows review-projects-page__rows--empty">
          <EmptyState />
        </div>
      </div>
    );
  }

  return (
    <div className="review-projects-page__rows-frame">
      <div className="review-projects-page__rows-shell">
        <VirtualList
          scrollRef={rowsParentRef}
          items={virtualItems}
          totalSize={totalSize}
          renderRow={(virtualRow: VirtualItem) => {
            const project = projects[virtualRow.index];
            if (!project) {
              return null;
            }
            return {
              key: project.id,
              props: {
                ref: getRowRef(project.id),
              },
              content: (
                <ReviewProjectRowView
                  project={project}
                  isAdmin={isAdmin}
                  isSelected={selectedProjectIdSet.has(project.id)}
                  onToggleSelection={adminControls?.onToggleProjectSelection}
                  onRequestIdClick={onRequestIdClick}
                />
              ),
            };
          }}
        />
      </div>
    </div>
  );
}

const formatDateTime = (value: string | null) => {
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
    hour: 'numeric',
  });
};

const formatNumber = (value: number | null) => {
  if (value == null) {
    return '—';
  }
  return value.toLocaleString();
};

const formatPercent = (accepted: number, total: number) => {
  if (total === 0) {
    return '0%';
  }
  const value = Math.round((accepted / total) * 100);
  return `${value}%`;
};

function FilterControls({ filters, canCreate }: { filters: FiltersProps; canCreate: boolean }) {
  const dateQuickRanges = getStandardDateQuickRanges();

  return (
    <div className="review-projects-page__bar">
      <LocaleMultiSelect
        options={filters.localeOptions}
        selectedTags={filters.selectedLocaleTags}
        onChange={filters.onLocaleChange}
        buttonAriaLabel="Filter by locale"
        myLocaleTags={filters.myLocaleTags}
      />
      <SearchControl
        value={filters.searchQuery}
        onChange={filters.onSearchChange}
        placeholder={
          filters.searchField === 'id'
            ? 'Search by project #id'
            : filters.searchField === 'requestId'
              ? 'Search by request #id'
              : 'Search by project name'
        }
        inputAriaLabel="Search projects"
        className="review-projects-page__searchcontrol"
        leading={
          <MultiSectionFilterChip
            ariaLabel="Search options"
            closeOnSelection
            align="left"
            className="review-projects-page__searchmode"
            classNames={{
              button: 'filter-chip__button',
              panel: 'filter-chip__panel',
              section: 'filter-chip__section',
              label: 'filter-chip__label',
              list: 'filter-chip__list',
              option: 'filter-chip__option',
            }}
            summary={`${filters.searchField === 'id' ? 'ID' : filters.searchField === 'requestId' ? 'Request' : 'Name'} · ${
              filters.searchType === 'contains'
                ? 'Contains'
                : filters.searchType === 'exact'
                  ? 'Exact'
                  : 'iLike'
            }`}
            sections={[
              {
                kind: 'radio',
                label: 'Field',
                options: [
                  { value: 'name', label: 'Name' },
                  { value: 'id', label: 'ID' },
                  { value: 'requestId', label: 'Request ID' },
                ],
                value: filters.searchField,
                onChange: (value) =>
                  filters.onSearchFieldChange(value as 'name' | 'id' | 'requestId'),
              },
              {
                kind: 'radio',
                label: 'Match',
                options: [
                  { value: 'contains', label: 'Contains' },
                  { value: 'exact', label: 'Exact' },
                  { value: 'ilike', label: 'iLike' },
                ],
                value: filters.searchType,
                onChange: (value) =>
                  filters.onSearchTypeChange(value as 'contains' | 'exact' | 'ilike'),
              },
            ]}
          />
        }
      />
      <MultiSectionFilterChip
        ariaLabel="Filter by status, type, size, and date"
        align="right"
        className="review-projects-page__filter-chip"
        sections={[
          {
            kind: 'radio',
            label: 'Type',
            options: filters.typeOptions as Array<FilterOption<string | number>>,
            value: filters.typeValue as string,
            onChange: (value) => filters.onTypeChange(value as ApiReviewProjectType | 'all'),
          },
          {
            kind: 'radio',
            label: 'Status',
            options: filters.statusOptions as Array<FilterOption<string | number>>,
            value: filters.statusValue as string,
            onChange: (value) => filters.onStatusChange(value as ApiReviewProjectStatus | 'all'),
          },
          {
            kind: 'size',
            label: 'Result size limit',
            options: filters.limitOptions,
            value: filters.limitValue,
            onChange: filters.onLimitChange,
          },
          {
            kind: 'date',
            label: 'Due date',
            after: filters.dueAfter,
            before: filters.dueBefore,
            onChangeAfter: filters.onChangeDueAfter,
            onChangeBefore: filters.onChangeDueBefore,
            quickRanges: dateQuickRanges,
            onClear: () => {
              filters.onChangeDueAfter(null);
              filters.onChangeDueBefore(null);
            },
          },
          {
            kind: 'date',
            label: 'Created date',
            after: filters.createdAfter,
            before: filters.createdBefore,
            onChangeAfter: filters.onChangeCreatedAfter,
            onChangeBefore: filters.onChangeCreatedBefore,
            quickRanges: dateQuickRanges,
            onClear: () => {
              filters.onChangeCreatedAfter(null);
              filters.onChangeCreatedBefore(null);
            },
          },
        ]}
      />
      {canCreate ? (
        <Link to="/review-projects/new" className="review-projects-page__create-button">
          New Project
        </Link>
      ) : null}
    </div>
  );
}

function LoadingState() {
  return (
    <div className="review-projects-page__state">
      <div className="spinner spinner--md" aria-hidden />
      <div>Loading review projects...</div>
    </div>
  );
}

function ErrorState({ message, onRetry }: { message?: string; onRetry?: () => void }) {
  return (
    <div className="review-projects-page__state review-projects-page__state--error">
      <div>{message || 'Something went wrong while loading review projects.'}</div>
      {onRetry ? (
        <button type="button" className="review-projects-page__state-action" onClick={onRetry}>
          Try again
        </button>
      ) : null}
    </div>
  );
}

function EmptyState() {
  return (
    <div className="review-projects-page__empty">
      <div className="review-projects-page__empty-copy hint">
        No review projects match these filters. Adjust locale, type, status, limit, or search.
      </div>
    </div>
  );
}

function ReviewProjectRowView({
  project,
  isAdmin,
  isSelected,
  onToggleSelection,
  onRequestIdClick,
}: {
  project: ReviewProjectRow;
  isAdmin: boolean;
  isSelected: boolean;
  onToggleSelection?: (projectId: number) => void;
  onRequestIdClick?: (requestId: number) => void;
}) {
  const textUnitCount = project.textUnitCount ?? 0;
  const percent = formatPercent(project.acceptedCount, textUnitCount);
  const percentValue =
    textUnitCount === 0 ? 0 : Math.round((project.acceptedCount / textUnitCount) * 100);
  const localeTag = project.localeTag;
  const requestId = project.requestId;
  const typeClass =
    project.type === 'EMERGENCY'
      ? 'review-projects-page__type-pill--emergency'
      : 'review-projects-page__type-pill--default';

  return (
    <div className="review-projects-page__row-card">
      <div className="review-projects-page__row-grid">
        <div className="review-projects-page__project">
          <div className="review-projects-page__id-row">
            {isAdmin ? (
              <label className="review-projects-page__select">
                <input
                  type="checkbox"
                  className="review-projects-page__select-input"
                  checked={isSelected}
                  onChange={() => onToggleSelection?.(project.id)}
                  aria-label={`Select review project ${project.id}`}
                />
              </label>
            ) : null}
            <Link to={`/review-projects/${project.id}`} className="review-projects-page__link">
              <span className="review-projects-page__project-name">{project.name}</span>
            </Link>
            {requestId != null && onRequestIdClick ? (
              <button
                type="button"
                className="review-projects-page__request-link review-projects-page__link"
                onClick={() => onRequestIdClick(requestId)}
              >
                Request #{requestId}
              </button>
            ) : null}
          </div>
        </div>
        <div className="review-projects-page__counts">
          <CountsInline words={project.wordCount ?? null} strings={project.textUnitCount ?? null} />
        </div>
        <div className="review-projects-page__type">
          <Pill className={`review-projects-page__type-pill ${typeClass}`}>
            {REVIEW_PROJECT_TYPE_LABELS[project.type] ?? REVIEW_PROJECT_TYPE_LABELS.NORMAL}
          </Pill>
        </div>
        <div className="review-projects-page__meta">
          {project.status === 'CLOSED' ? (
            <Pill className="review-projects-page__status-pill status-closed">
              {REVIEW_PROJECT_STATUS_LABELS[project.status]}
            </Pill>
          ) : project.dueDate ? (
            <span>Due {formatDateTime(project.dueDate)}</span>
          ) : null}
        </div>
        <div className="review-projects-page__locales">
          {localeTag ? (
            <div className="review-projects-page__pill-list">
              <LocalePill
                className="review-projects-page__pill review-projects-page__pill--locale"
                bcp47Tag={localeTag}
                labelMode="tag"
              />
            </div>
          ) : (
            <span className="review-projects-page__muted">No locale</span>
          )}
        </div>
        <div className="review-projects-page__progress">
          <div
            className="review-projects-page__progress-bar"
            title={`${formatNumber(project.acceptedCount)} of ${formatNumber(textUnitCount)} processed`}
          >
            <div
              className="review-projects-page__progress-fill"
              style={{ width: `${percentValue}%` }}
              aria-hidden
            />
          </div>
          <div className="review-projects-page__progress-meta">
            <div className="review-projects-page__progress-percent">{percent} reviewed</div>
          </div>
        </div>
      </div>
    </div>
  );
}

export function ReviewProjectsPageView({
  status,
  errorMessage,
  errorOnRetry,
  projects,
  filters,
  requestFilter,
  onRequestIdClick,
  canCreate = true,
  adminControls,
}: Props) {
  const scrollElementRef = useRef<HTMLDivElement>(null);
  const getItemKey = useCallback((index: number) => projects[index]?.id ?? index, [projects]);
  const hasResults = projects.length > 0;

  const estimateSize = useCallback(
    () =>
      getRowHeightPx({
        element: scrollElementRef.current,
        cssVariable: '--review-projects-row-height',
        defaultRem: 7.5,
      }),
    [],
  );

  const {
    items: virtualItems,
    totalSize,
    measureElement,
  } = useVirtualRows<HTMLDivElement>({
    count: projects.length,
    getItemKey,
    estimateSize,
    getScrollElement: () => scrollElementRef.current,
    overscan: 8,
  });

  const { getRowRef } = useMeasuredRowRefs<number, HTMLDivElement>({ measureElement });

  const totalWords = useMemo(
    () => projects.reduce((sum, project) => sum + (project.wordCount ?? 0), 0),
    [projects],
  );

  const totalStrings = useMemo(
    () => projects.reduce((sum, project) => sum + (project.textUnitCount ?? 0), 0),
    [projects],
  );

  if (status === 'loading') {
    return <LoadingState />;
  }

  if (status === 'error') {
    return <ErrorState message={errorMessage} onRetry={errorOnRetry} />;
  }

  return (
    <div className="review-projects-page">
      {requestFilter && requestFilter.requestId !== null ? (
        <div className="review-projects-page__notice">
          <span className="review-projects-page__notice-text">
            Showing projects from request #{requestFilter.requestId}
          </span>
          <button
            type="button"
            className="review-projects-page__notice-clear"
            onClick={requestFilter.onClear}
          >
            Clear
          </button>
        </div>
      ) : null}
      {}
      <FilterControls filters={filters} canCreate={canCreate} />
      {hasResults ? (
        <SummaryBar
          resultCount={projects.length}
          totalWords={totalWords}
          totalStrings={totalStrings}
        />
      ) : null}
      {hasResults && adminControls && adminControls.enabled ? (
        <AdminBar adminControls={adminControls} visibleCount={projects.length} />
      ) : null}
      <ContentSection
        projects={projects}
        rowsParentRef={scrollElementRef}
        virtualItems={virtualItems}
        totalSize={totalSize}
        getRowRef={getRowRef}
        adminControls={adminControls}
        onRequestIdClick={onRequestIdClick}
      />
    </div>
  );
}
