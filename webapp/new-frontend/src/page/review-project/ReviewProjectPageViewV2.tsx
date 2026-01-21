import './review-project-page-v2.css';

import type React from 'react';
import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';

import type { ApiReviewProjectDetail, ApiReviewProjectTextUnit } from '../../api/review-projects';
import {
  REVIEW_PROJECT_STATUS_LABELS,
  REVIEW_PROJECT_TYPE_LABELS,
} from '../../api/review-projects';
import { LocalePill } from '../../components/LocalePill';
import { Pill } from '../../components/Pill';

type Props = {
  projectId: number;
  project: ApiReviewProjectDetail | null;
};

export function ReviewProjectPageViewV2({ projectId, project }: Props) {
  const primaryLocale = useMemo(
    () => project?.locale ?? project?.locales?.[0] ?? null,
    [project?.locale, project?.locales],
  );
  const textUnits = useMemo<ApiReviewProjectTextUnit[]>(
    () => primaryLocale?.textUnits ?? [],
    [primaryLocale],
  );

  const [selectedTextUnitId, setSelectedTextUnitId] = useState<number | null>(() =>
    textUnits.length ? textUnits[0].reviewProjectTextUnitId : null,
  );

  useEffect(() => {
    setSelectedTextUnitId(textUnits[0]?.reviewProjectTextUnitId ?? null);
  }, [textUnits]);

  if (!project) {
    return <div className="review-project-page-v2__empty">No project data for id {projectId}</div>;
  }

  const selectedTextUnit =
    textUnits.find((tu) => tu.reviewProjectTextUnitId === selectedTextUnitId) ?? null;

  return (
    <div className="review-project-page-v2">
      <header className="review-project-page-v2__header">
        <div className="review-project-page-v2__header-top">
          <div className="review-project-page-v2__title-row">
            <span className="review-project-page-v2__title">
              {project.name ?? `Project ${projectId}`}
            </span>
            <Pill className={`review-project-page-v2__pill review-project-page-v2__pill--type-${project.type}`}>
              {REVIEW_PROJECT_TYPE_LABELS[project.type]}
            </Pill>
            <Pill
              className={`review-project-page-v2__pill review-project-page-v2__pill--status-${project.status.toLowerCase()}`}
            >
              {REVIEW_PROJECT_STATUS_LABELS[project.status]}
            </Pill>
          </div>
          <Link to="/review-projects" className="review-project-page-v2__header-link">
            Review projects
          </Link>
        </div>
        <div className="review-project-page-v2__meta-row">
          <span>Due {formatDate(project.dueDate)}</span>
          <span className="review-project-page-v2__dot">•</span>
          <Counts words={project.wordCount} strings={project.textUnitCount ?? textUnits.length} />
          <span className="review-project-page-v2__dot">•</span>
          <LocaleList locales={project.locales ?? []} />
        </div>
      </header>

      <div className="review-project-page-v2__body">
        <aside className="review-project-page-v2__list">
          <div className="review-project-page-v2__list-header">
            <div className="review-project-page-v2__list-title">Strings</div>
            <div className="review-project-page-v2__list-count">{textUnits.length} items</div>
          </div>
          <div className="review-project-page-v2__list-items">
            {textUnits.map((tu) => {
              const isActive = tu.reviewProjectTextUnitId === selectedTextUnitId;
              return (
                <button
                  key={tu.reviewProjectTextUnitId}
                  type="button"
                  className={`review-project-page-v2__list-item${isActive ? ' is-active' : ''}`}
                  onClick={() => setSelectedTextUnitId(tu.reviewProjectTextUnitId)}
                >
                  <div className="review-project-page-v2__list-name">
                    {tu.name || `Text unit ${tu.reviewProjectTextUnitId}`}
                  </div>
                  <div className="review-project-page-v2__list-preview">
                    <span className="review-project-page-v2__preview-label">Source</span>
                    <span className="review-project-page-v2__preview-text">{tu.source || '—'}</span>
                  </div>
                  <div className="review-project-page-v2__list-preview">
                    <span className="review-project-page-v2__preview-label">Target</span>
                    <span className="review-project-page-v2__preview-text">{tu.target || '—'}</span>
                  </div>
                </button>
              );
            })}
            {!textUnits.length ? (
              <div className="review-project-page-v2__list-empty">No strings for this locale.</div>
            ) : null}
          </div>
        </aside>

        <main className="review-project-page-v2__detail">
          {!selectedTextUnit ? (
            <div className="review-project-page-v2__empty">Select a string to review.</div>
          ) : (
            <DetailCard textUnit={selectedTextUnit} localeTag={primaryLocale?.bcp47Tag ?? ''} />
          )}
        </main>
      </div>
    </div>
  );
}

function DetailCard({
  textUnit,
  localeTag,
}: {
  textUnit: ApiReviewProjectTextUnit;
  localeTag: string;
}) {
  return (
    <div className="review-project-page-v2__card">
      <div className="review-project-page-v2__section">
        <div className="review-project-page-v2__section-label">String</div>
        <div className="review-project-page-v2__section-value">
          {textUnit.name || `Text unit ${textUnit.reviewProjectTextUnitId}`}
        </div>
      </div>
      <div className="review-project-page-v2__grid">
        <div className="review-project-page-v2__section">
          <div className="review-project-page-v2__section-label">Source</div>
          <div className="review-project-page-v2__section-value">{textUnit.source || '—'}</div>
        </div>
        <div className="review-project-page-v2__section">
          <div className="review-project-page-v2__section-label">Translation</div>
          <div className="review-project-page-v2__section-value review-project-page-v2__section-value--emphasis">
            {textUnit.target || '—'}
          </div>
        </div>
      </div>
      <div className="review-project-page-v2__grid">
        <div className="review-project-page-v2__section">
          <div className="review-project-page-v2__section-label">Locale</div>
          {localeTag ? <LocalePill bcp47Tag={localeTag} displayName={localeTag} /> : '—'}
        </div>
        <div className="review-project-page-v2__section">
          <div className="review-project-page-v2__section-label">TM text unit ID</div>
          <div className="review-project-page-v2__section-value">{textUnit.tmTextUnitId}</div>
        </div>
      </div>
      <div className="review-project-page-v2__section">
        <div className="review-project-page-v2__section-label">Status</div>
        <Pill className="review-project-page-v2__status-pill">
          {getDisplayStatus(textUnit.reviewStatus ?? textUnit.status) ?? '—'}
        </Pill>
      </div>
    </div>
  );
}

function Counts({
  words,
  strings,
}: {
  words: number | null | undefined;
  strings: number | null | undefined;
}) {
  return (
    <span className="review-project-page-v2__counts">
      <span className="review-project-page-v2__count">{formatNumber(words)}</span>
      <span className="review-project-page-v2__count-label">words</span>
      <span className="review-project-page-v2__count-sep">·</span>
      <span className="review-project-page-v2__count">{formatNumber(strings)}</span>
      <span className="review-project-page-v2__count-label">strings</span>
    </span>
  );
}

function LocaleList({
  locales,
}: {
  locales: NonNullable<ApiReviewProjectDetail['locales']>;
}) {
  if (!locales.length) return <span className="review-project-page-v2__muted">No locale</span>;
  return (
    <div className="review-project-page-v2__locales">
      {locales.map((locale) => (
        <LocalePill
          key={locale.id ?? locale.bcp47Tag}
          bcp47Tag={locale.bcp47Tag}
          displayName={locale.displayName}
          labelMode="tag"
          className="review-project-page-v2__locale-pill"
        />
      ))}
    </div>
  );
}

const formatNumber = (value: number | null | undefined) => {
  if (value == null) return '—';
  return value.toLocaleString();
};

const formatDate = (value: string | null | undefined) => {
  if (!value) return '—';
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return parsed.toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
};

const getDisplayStatus = (status: string | null | undefined) => {
  if (!status) return null;
  const upper = status.toUpperCase();
  if (upper.startsWith('ACCEPTED')) return 'Accepted';
  if (upper === 'REJECTED') return 'Rejected';
  if (upper === 'VIEWED') return 'To review';
  if (upper === 'SKIPPED') return 'To translate';
  const cleaned = status.toLowerCase().replace(/_/g, ' ');
  return cleaned.charAt(0).toUpperCase() + cleaned.slice(1);
};
