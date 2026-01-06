import './repositories-page.css';

import { useVirtualizer } from '@tanstack/react-virtual';
import { useCallback, useEffect, useMemo, useRef } from 'react';

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

type Props = {
  status: 'loading' | 'error' | 'ready';
  errorMessage?: string;
  errorOnRetry?: () => void;
  repositories: RepositoryRow[];
  locales: LocaleRow[];
  hasSelection: boolean;
  searchValue: string;
  onSearchChange: (value: string) => void;
  onSelectRepository: (id: number) => void;
};

const formatCount = (value: number) => (value === 0 ? '' : value);

type RepositoryTableProps = {
  repositories: RepositoryRow[];
  searchValue: string;
  onSearchChange: (value: string) => void;
  onSelectRepository: (id: number) => void;
};

type LocaleTableProps = {
  locales: LocaleRow[];
  hasSelection: boolean;
};

type ErrorStateProps = {
  message?: string;
  onRetry?: () => void;
};

function LoadingState() {
  return (
    <div className="repositories-page__state">
      <div className="repositories-page__state-content">
        <span className="repositories-page__spinner" aria-hidden="true" />
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
  searchValue,
  onSearchChange,
  onSelectRepository,
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

  return (
    <div className="repositories-page__pane">
      <div className="repositories-page__header repositories-page__header--repo">
        <div className="repositories-page__header-cell" aria-hidden="true"></div>
        <div className="repositories-page__header-cell">
          <span>Name</span>
          <input
            type="search"
            className="repositories-page__repo-header-name-search"
            placeholder="Search"
            aria-label="Search repositories"
            value={searchValue}
            onChange={(event) => onSearchChange(event.target.value)}
          />
        </div>
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
                  <span>{repo.name}</span>
                </div>
                <div className="repositories-page__cell repositories-page__cell--number">
                  {formatCount(repo.rejected)}
                </div>
                <div
                  className="repositories-page__cell repositories-page__cell--number"
                  title={
                    repo.needsTranslation
                      ? `${formatCount(repo.needsTranslation)} units\n${formatCount(repo.needsTranslation * 20)} words`
                      : ''
                  }
                >
                  {formatCount(repo.needsTranslation)}
                </div>
                <div
                  className="repositories-page__cell repositories-page__cell--number"
                  title={
                    repo.needsReview
                      ? `${formatCount(repo.needsReview)} units\n${formatCount(repo.needsReview * 15)} words`
                      : ''
                  }
                >
                  {formatCount(repo.needsReview)}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

function LocaleTable({ locales, hasSelection }: LocaleTableProps) {
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
              <div className="repositories-page__cell">{locale.name}</div>
              <div className="repositories-page__cell repositories-page__cell--number">
                {formatCount(locale.rejected)}
              </div>
              <div
                className="repositories-page__cell repositories-page__cell--number"
                title={
                  locale.needsTranslation
                    ? `${formatCount(locale.needsTranslation)} units\n${formatCount(locale.needsTranslation * 10)} words`
                    : ''
                }
              >
                {formatCount(locale.needsTranslation)}
              </div>
              <div
                className="repositories-page__cell repositories-page__cell--number"
                title={
                  locale.needsReview
                    ? `${formatCount(locale.needsReview)} units\n${formatCount(locale.needsReview * 8)} words`
                    : ''
                }
              >
                {formatCount(locale.needsReview)}
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
  searchValue,
  onSearchChange,
  onSelectRepository,
}: Props) {
  if (status === 'loading') {
    return <LoadingState />;
  }

  if (status === 'error') {
    return <ErrorState message={errorMessage} onRetry={errorOnRetry} />;
  }

  const selectedRepoId = repositories.find((repo) => repo.selected)?.id ?? null;

  return (
    <div className="repositories-page repositories-page--split">
      <RepositoryTable
        repositories={repositories}
        searchValue={searchValue}
        onSearchChange={onSearchChange}
        onSelectRepository={onSelectRepository}
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
      <LocaleTable locales={locales} hasSelection={hasSelection} />
    </div>
  );
}
