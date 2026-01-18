import './review-project-page.css';
import '../review-projects/review-projects-page.css';

import { useCallback, useMemo } from 'react';
import { VirtualItem } from '@tanstack/react-virtual';

import type { ApiReviewProjectDetail, ApiReviewProjectTextUnit } from '../../api/review-projects';
import {
  REVIEW_PROJECT_STATUS_LABELS,
  REVIEW_PROJECT_TYPE_LABELS,
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
  if (!project) {
    return <div>No project data for id {projectId}</div>;
  }

  const primaryLocale = project.locales?.[0];
  const textUnits = useMemo(() => primaryLocale?.textUnits ?? [], [primaryLocale]);

  const estimateRowHeight = useCallback(
    () =>
      getRowHeightPx({
        cssVariable: '--review-project-row-height',
        defaultRem: 7,
      }),
    [],
  );

  const getItemKey = useCallback(
    (index: number) => textUnits[index]?.reviewProjectTextUnitId ?? index,
    [textUnits],
  );

  const { scrollRef, items, totalSize } = useVirtualRows<HTMLDivElement>({
    count: textUnits.length,
    estimateSize: estimateRowHeight,
    getItemKey,
  });

  return (
    <div className="review-project-page">
      <ReviewProjectHeader projectId={projectId} project={project} />

      <div className="review-project-page__content">
        <section className="review-project-page__list-pane">
          <div className="review-project-page__search">TODO: search bar for strings</div>
          <VirtualList
            scrollRef={scrollRef}
            items={items}
            totalSize={totalSize}
            renderRow={(virtualItem: VirtualItem) => {
              const textUnit = textUnits[virtualItem.index] as ApiReviewProjectTextUnit | undefined;
              if (!textUnit) {
                return null;
              }
              return {
                key: virtualItem.key,
                content: <TextUnitRow textUnit={textUnit} />,
              };
            }}
          />
        </section>
        <section className="review-project-page__detail-pane">
          TODO: right pane with string details/editor
        </section>
      </div>
    </div>
  );
}

function TextUnitRow({ textUnit }: { textUnit: ApiReviewProjectTextUnit }) {
  if (!textUnit) {
    return null;
  }
  const { reviewProjectTextUnitId, name, source, target } = textUnit;
  return (
    <div className="review-project-row">
      <div className="review-project-row__name" title={name ?? undefined}>
        {name || `Text unit ${reviewProjectTextUnitId}`}
      </div>
      <div className="review-project-row__strings">
        <div className="review-project-row__string-line" title={source}>
          <span className="review-project-row__string-text">{source || '—'}</span>
        </div>
        <div className="review-project-row__string-line review-project-row__string-line--target" title={target ?? undefined}>
          <span className="review-project-row__string-text review-project-row__string-text--target">
            {target || '—'}
          </span>
        </div>
      </div>
    </div>
  );
}

function ReviewProjectHeader({
  projectId,
  project,
}: {
  projectId: number;
  project: ApiReviewProjectDetail;
}) {
  const { name, dueDate, textUnitCount, wordCount, status, type, locales: localesRaw } = project;
  const locales = localesRaw ?? [];

  const { acceptedCount, selectedCount, progressPercent } = useMemo(() => {
    const accepted = locales.reduce((sum, locale) => sum + (locale.acceptedCount ?? 0), 0);
    const selected = locales.reduce((sum, locale) => sum + (locale.selectedCount ?? 0), 0);
    const percent = selected > 0 ? Math.round((accepted / selected) * 100) : 0;
    return { acceptedCount: accepted, selectedCount: selected, progressPercent: percent };
  }, [locales]);

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
            <span className="review-project-page__progress-label">{progressPercent}%</span>
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
