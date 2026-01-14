import '../../components/chip-dropdown.css';
import '../../components/filters/filter-chip.css';
import '../workbench/workbench-page.css';
import './repositories-page.css';

import { useVirtualizer } from '@tanstack/react-virtual';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import type { LocaleOption } from '../../components/LocaleMultiSelect';
import { LocaleMultiSelect } from '../../components/LocaleMultiSelect';
import {
  RepositoryMultiSelect,
  type RepositoryMultiSelectOption,
} from '../../components/RepositoryMultiSelect';

const ROW_HEIGHT_PX = 48; // keep in sync with --repositories-page-row-height in CSS

export type RepositoryRow = {
  id: number;
  name: string;
  rejected: number;
  needsTranslation: number;
  needsReview: number;
  selected: boolean;
};

export type LocaleRow = {
  id: string;
  name: string;
  rejected: number;
  needsTranslation: number;
  needsReview: number;
};

export type RepositoryStatusFilter = 'all' | 'rejected' | 'needs-translation' | 'needs-review';

const statusFilterOptions: Array<{ value: RepositoryStatusFilter; label: string }> = [
  { value: 'all', label: 'All statuses' },
  { value: 'rejected', label: 'Rejected' },
  { value: 'needs-translation', label: 'To translate' },
  { value: 'needs-review', label: 'To review' },
];

type Props = {
  status: 'loading' | 'error' | 'ready';
  errorMessage?: string;
  errorOnRetry?: () => void;
  repositories: RepositoryRow[];
  locales: LocaleRow[];
  hasSelection: boolean;
  repositoryOptions: RepositoryMultiSelectOption[];
  selectedRepositoryIds: number[];
  onChangeRepositorySelection: (next: number[]) => void;
  localeOptions: LocaleOption[];
  selectedLocaleTags: string[];
  onChangeLocaleSelection: (next: string[]) => void;
  myLocaleSelections: string[];
  statusFilter: RepositoryStatusFilter;
  onStatusFilterChange: (value: RepositoryStatusFilter) => void;
  onSelectRepository: (id: number) => void;
  onOpenWorkbench: (params: {
    repositoryId: number;
    status?: string | null;
    localeTag?: string | null;
    count?: number | null;
  }) => void;
  isRepositorySelectionEmpty: boolean;
};

const formatCount = (value: number) => (value === 0 ? '' : value);

type RepositoryTableProps = {
  repositories: RepositoryRow[];
  isRepositorySelectionEmpty: boolean;
  onSelectRepository: (id: number) => void;
  onOpenWorkbench: (params: {
    repositoryId: number;
    status?: string | null;
    count?: number | null;
  }) => void;
};

type LocaleTableProps = {
  locales: LocaleRow[];
  hasSelection: boolean;
  repositoryId: number | null;
  onOpenWorkbench: (params: {
    repositoryId: number;
    status?: string | null;
    localeTag?: string | null;
    count?: number | null;
  }) => void;
};

type ErrorStateProps = {
  message?: string;
  onRetry?: () => void;
};

type CellLinkProps = {
  children: React.ReactNode;
  onClick: () => void;
  muted?: boolean;
  className?: string;
  ariaLabel?: string;
  title?: string;
  stopPropagation?: boolean;
};

type StatusFilterDropdownProps = {
  value: RepositoryStatusFilter;
  onChange: (value: RepositoryStatusFilter) => void;
};

function StatusFilterDropdown({ value, onChange }: StatusFilterDropdownProps) {
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

  const selectedLabel =
    statusFilterOptions.find((option) => option.value === value)?.label ?? 'All statuses';

  return (
    <div className="chip-dropdown filter-chip repositories-page__status-filter" ref={containerRef}>
      <button
        type="button"
        className="chip-dropdown__button filter-chip__button"
        onClick={() => setIsOpen((previous) => !previous)}
        aria-expanded={isOpen}
        aria-label="Filter repositories by status"
      >
        <span className="chip-dropdown__summary">{selectedLabel}</span>
        <span className="chip-dropdown__chevron" aria-hidden="true" />
      </button>
      {isOpen ? (
        <div className="chip-dropdown__panel filter-chip__panel" role="menu">
          <div className="filter-chip__section">
            <div className="filter-chip__label">Status</div>
            <div className="filter-chip__list">
              {statusFilterOptions.map((option) => (
                <button
                  type="button"
                  key={option.value}
                  className={`filter-chip__option${option.value === value ? ' is-active' : ''}`}
                  onClick={() => {
                    onChange(option.value);
                    setIsOpen(false);
                  }}
                >
                  <span>{option.label}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function CellLink({
  children,
  onClick,
  muted = false,
  className = '',
  ariaLabel,
  title,
  stopPropagation,
}: CellLinkProps) {
  return (
    <button
      type="button"
      className={`repositories-page__cell-link${muted ? ' is-muted' : ''}${
        className ? ` ${className}` : ''
      }`}
      onClick={(event) => {
        if (stopPropagation) {
          event.stopPropagation();
        }
        onClick();
      }}
      aria-label={ariaLabel}
      title={title}
    >
      {children}
    </button>
  );
}

function LoadingState() {
  return (
    <div className="repositories-page__state">
      <div className="repositories-page__state-content">
        <span className="spinner spinner--md" aria-hidden="true" />
        <div className="hint">Loading repositories…</div>
      </div>
    </div>
  );
}

function ErrorState({ message, onRetry }: ErrorStateProps) {
  const errorMessage = message || 'Failed to load repositories.';
  const handleRetry = () => {
    if (onRetry) {
      onRetry();
    }
  };

  return (
    <div className="repositories-page__state repositories-page__state--error">
      <div className="repositories-page__state-content">
        <div className="hint">{errorMessage}</div>
        {onRetry ? (
          <button type="button" className="repositories-page__state-action" onClick={handleRetry}>
            Try again
          </button>
        ) : null}
      </div>
    </div>
  );
}

function RepositoryTable({
  repositories,
  isRepositorySelectionEmpty,
  onSelectRepository,
  onOpenWorkbench,
}: RepositoryTableProps) {
  const scrollRef = useRef<HTMLDivElement | null>(null);
  const selectedIndex = useMemo(
    () => repositories.findIndex((repo) => repo.selected),
    [repositories],
  );

  const getItemKey = useCallback(
    (index: number) => repositories[index]?.id ?? index,
    [repositories],
  );

  const virtualizer = useVirtualizer<HTMLDivElement, HTMLDivElement>({
    count: repositories.length,
    getScrollElement: () => scrollRef.current,
    estimateSize: () => ROW_HEIGHT_PX,
    overscan: 8,
    getItemKey,
  });

  const items = virtualizer.getVirtualItems();

  useEffect(() => {
    if (selectedIndex < 0) {
      return;
    }

    virtualizer.scrollToIndex(selectedIndex, { align: 'auto' });
  }, [selectedIndex, virtualizer]);

  if (isRepositorySelectionEmpty) {
    return (
      <div className="repositories-page__pane">
        <div className="repositories-page__pane-placeholder hint">
          Select repositories to show results.
        </div>
      </div>
    );
  }

  return (
    <div className="repositories-page__pane">
      <div className="repositories-page__header repositories-page__header--repo">
        <div className="repositories-page__header-cell" aria-hidden="true"></div>
        <div className="repositories-page__header-cell">Name</div>
        <div className="repositories-page__header-cell repositories-page__header-cell--number">
          Rejected
        </div>
        <div className="repositories-page__header-cell repositories-page__header-cell--number">
          To translate
        </div>
        <div className="repositories-page__header-cell repositories-page__header-cell--number">
          To review
        </div>
      </div>

      <div className="repositories-page__repo-scroll" ref={scrollRef}>
        <div
          className="repositories-page__repo-scroll-inner"
          style={{ height: `${virtualizer.getTotalSize()}px` }}
        >
          {items.map((virtualRow) => {
            const repo = repositories[virtualRow.index];
            if (!repo) {
              return null;
            }

            return (
              <div
                key={repo.id}
                className={`repositories-page__repo-scroll-row${
                  repo.selected ? ' repositories-page__repo-scroll-row--selected' : ''
                }`}
                style={{
                  transform: `translateY(${virtualRow.start}px)`,
                  height: `${virtualRow.size}px`,
                }}
                onClick={() => onSelectRepository(repo.id)}
              >
                <div
                  className={`repositories-page__repo-scroll-row-select${
                    repo.selected ? ' repositories-page__repo-scroll-row-select--active' : ''
                  }`}
                ></div>
                <div className="repositories-page__cell repositories-page__cell--name">
                  <CellLink
                    className="repositories-page__cell-link--name"
                    stopPropagation
                    onClick={() => onOpenWorkbench({ repositoryId: repo.id, status: null })}
                  >
                    {repo.name}
                  </CellLink>
                </div>
                <div className="repositories-page__cell repositories-page__cell--number">
                  <CellLink
                    muted={repo.rejected === 0}
                    stopPropagation
                    onClick={() =>
                      onOpenWorkbench({
                        repositoryId: repo.id,
                        status: 'REJECTED',
                        count: repo.rejected,
                      })
                    }
                    ariaLabel={`Open rejected units for ${repo.name} in workbench`}
                  >
                    {formatCount(repo.rejected) || '-'}
                  </CellLink>
                </div>
                <div
                  className="repositories-page__cell repositories-page__cell--number"
                  title={
                    repo.needsTranslation
                      ? `${formatCount(repo.needsTranslation)} units\n${formatCount(repo.needsTranslation * 20)} words`
                      : ''
                  }
                >
                  <CellLink
                    muted={repo.needsTranslation === 0}
                    stopPropagation
                    onClick={() =>
                      onOpenWorkbench({
                        repositoryId: repo.id,
                        status: 'FOR_TRANSLATION',
                        count: repo.needsTranslation,
                      })
                    }
                    ariaLabel={`Open "to translate" units for ${repo.name} in workbench`}
                  >
                    {formatCount(repo.needsTranslation) || '-'}
                  </CellLink>
                </div>
                <div
                  className="repositories-page__cell repositories-page__cell--number"
                  title={
                    repo.needsReview
                      ? `${formatCount(repo.needsReview)} units\n${formatCount(repo.needsReview * 15)} words`
                      : ''
                  }
                >
                  <CellLink
                    muted={repo.needsReview === 0}
                    stopPropagation
                    onClick={() =>
                      onOpenWorkbench({
                        repositoryId: repo.id,
                        status: 'REVIEW_NEEDED',
                        count: repo.needsReview,
                      })
                    }
                    ariaLabel={`Open "to review" units for ${repo.name} in workbench`}
                  >
                    {formatCount(repo.needsReview) || '-'}
                  </CellLink>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

function LocaleTable({ locales, hasSelection, repositoryId, onOpenWorkbench }: LocaleTableProps) {
  if (!hasSelection) {
    return (
      <div className="repositories-page__pane">
        <div className="repositories-page__locale-placeholder">
          <span className="hint">Select a repository to show locale info.</span>
        </div>
      </div>
    );
  }

  return (
    <div className="repositories-page__pane">
      <div className="repositories-page__header repositories-page__header--locale">
        <div className="repositories-page__header-cell">Locale</div>
        <div className="repositories-page__header-cell repositories-page__header-cell--number">
          Rejected
        </div>
        <div className="repositories-page__header-cell repositories-page__header-cell--number">
          To translate
        </div>
        <div className="repositories-page__header-cell repositories-page__header-cell--number">
          To review
        </div>
      </div>
      {locales.length === 0 ? (
        <div className="repositories-page__pane-placeholder hint">No locale data available.</div>
      ) : (
        <div className="repositories-page__locale-scroll">
          {locales.map((locale) => (
            <div key={locale.id} className="repositories-page__locale-scroll-row">
              <div className="repositories-page__cell">
                <CellLink
                  className="repositories-page__cell-link--name"
                  onClick={() =>
                    onOpenWorkbench({
                      repositoryId: repositoryId ?? -1,
                      status: null,
                      localeTag: locale.id,
                      count: null,
                    })
                  }
                >
                  {locale.name}
                </CellLink>
              </div>
              <div className="repositories-page__cell repositories-page__cell--number">
                <CellLink
                  muted={locale.rejected === 0}
                  onClick={() =>
                    onOpenWorkbench({
                      repositoryId: repositoryId ?? -1,
                      status: 'REJECTED',
                      localeTag: locale.id,
                      count: locale.rejected,
                    })
                  }
                  ariaLabel={`Open rejected units for ${locale.name} in workbench`}
                >
                  {formatCount(locale.rejected) || '-'}
                </CellLink>
              </div>
              <div className="repositories-page__cell repositories-page__cell--number">
                <CellLink
                  muted={locale.needsTranslation === 0}
                  onClick={() =>
                    onOpenWorkbench({
                      repositoryId: repositoryId ?? -1,
                      status: 'FOR_TRANSLATION',
                      localeTag: locale.id,
                      count: locale.needsTranslation,
                    })
                  }
                  ariaLabel={`Open "to translate" units for ${locale.name} in workbench`}
                  title={
                    locale.needsTranslation
                      ? `${formatCount(locale.needsTranslation)} units\n${formatCount(locale.needsTranslation * 10)} words`
                      : ''
                  }
                >
                  {formatCount(locale.needsTranslation) || '-'}
                </CellLink>
              </div>
              <div className="repositories-page__cell repositories-page__cell--number">
                <CellLink
                  muted={locale.needsReview === 0}
                  onClick={() =>
                    onOpenWorkbench({
                      repositoryId: repositoryId ?? -1,
                      status: 'REVIEW_NEEDED',
                      localeTag: locale.id,
                      count: locale.needsReview,
                    })
                  }
                  ariaLabel={`Open "to review" units for ${locale.name} in workbench`}
                  title={
                    locale.needsReview
                      ? `${formatCount(locale.needsReview)} units\n${formatCount(locale.needsReview * 8)} words`
                      : ''
                  }
                >
                  {formatCount(locale.needsReview) || '-'}
                </CellLink>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export function RepositoriesPageView({
  status,
  errorMessage,
  errorOnRetry,
  repositories,
  locales,
  hasSelection,
  repositoryOptions,
  selectedRepositoryIds,
  onChangeRepositorySelection,
  localeOptions,
  selectedLocaleTags,
  onChangeLocaleSelection,
  myLocaleSelections,
  statusFilter,
  onStatusFilterChange,
  onSelectRepository,
  onOpenWorkbench,
  isRepositorySelectionEmpty,
}: Props) {
  if (status === 'loading') {
    return <LoadingState />;
  }

  if (status === 'error') {
    return <ErrorState message={errorMessage} onRetry={errorOnRetry} />;
  }

  const selectedRepoId = repositories.find((repo) => repo.selected)?.id ?? null;

  return (
    <div className="repositories-page">
      <div className="repositories-page__bar">
        <div className="repositories-page__controls">
          <div className="repositories-page__filters" role="group" aria-label="Filter repositories">
            <RepositoryMultiSelect
              options={repositoryOptions}
              selectedIds={selectedRepositoryIds}
              onChange={onChangeRepositorySelection}
              className="repositories-page__repository-filter"
              buttonAriaLabel="Filter repositories by name"
            />
            <LocaleMultiSelect
              options={localeOptions}
              selectedTags={selectedLocaleTags}
              onChange={onChangeLocaleSelection}
              className="repositories-page__locale-filter"
              myLocaleTags={myLocaleSelections}
            />
            <StatusFilterDropdown value={statusFilter} onChange={onStatusFilterChange} />
          </div>
        </div>
      </div>
      <div className="repositories-page__content repositories-page--split">
        <RepositoryTable
          repositories={repositories}
          isRepositorySelectionEmpty={isRepositorySelectionEmpty}
          onSelectRepository={onSelectRepository}
          onOpenWorkbench={onOpenWorkbench}
        />
        <div className="repositories-page__divider">
          {hasSelection ? (
            <button
              type="button"
              className="repositories-page__divider-action"
              onClick={() => {
                if (selectedRepoId != null) {
                  onSelectRepository(selectedRepoId);
                }
              }}
              aria-label="Hide locale stats"
            >
              ×
            </button>
          ) : null}
        </div>
        <LocaleTable
          locales={locales}
          hasSelection={hasSelection}
          repositoryId={selectedRepoId}
          onOpenWorkbench={onOpenWorkbench}
        />
      </div>
    </div>
  );
}
