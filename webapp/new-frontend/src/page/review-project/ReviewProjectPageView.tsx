import './review-project-page.css';

import { Fragment, useMemo } from 'react';
import { Link } from 'react-router-dom';

import type {
  ApiReviewProjectDetail,
  ApiReviewProjectLocaleDetail,
  ApiReviewProjectTextUnit,
} from '../../api/review-projects';
import { REVIEW_PROJECT_STATUS_LABELS, REVIEW_PROJECT_TYPE_LABELS } from '../../api/review-projects';
import { Pill } from '../../components/Pill';

type Props = {
  status: 'loading' | 'error' | 'ready';
  project?: ApiReviewProjectDetail;
  errorMessage?: string;
  onRetry?: () => void;
};

const formatNumber = (value: number | null | undefined) => {
  if (value == null) return '—';
  return value.toLocaleString();
};

const formatDate = (value: string | null | undefined) => {
  if (!value) return '—';
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return parsed.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
  });
};

const LoadingState = () => (
  <div className="review-project__state">
    <div className="spinner spinner--md" aria-hidden />
    <div>Loading review project…</div>
  </div>
);

const ErrorState = ({ message, onRetry }: { message?: string; onRetry?: () => void }) => (
  <div className="review-project__state review-project__state--error">
    <div>{message ?? 'Something went wrong.'}</div>
    {onRetry ? (
      <button type="button" className="review-project__state-action" onClick={onRetry}>
        Try again
      </button>
    ) : null}
  </div>
);

const MetaItem = ({ label, value }: { label: string; value: string | number }) => (
  <div className="review-project__meta-item">
    <div className="review-project__meta-label">{label}</div>
    <div className="review-project__meta-value">{value}</div>
  </div>
);

const LocaleBadge = ({ locale }: { locale: ApiReviewProjectLocaleDetail }) => (
  <div className="review-project__locale-badge">
    <div className="review-project__locale-tag">{locale.bcp47Tag}</div>
    <div className="review-project__locale-counts">
      <span>{formatNumber(locale.acceptedCount)} accepted</span>
      <span className="review-project__separator">·</span>
      <span>{formatNumber(locale.selectedCount)} selected</span>
    </div>
  </div>
);

const TextUnitRow = ({ unit }: { unit: ApiReviewProjectTextUnit }) => (
  <tr>
    <td className="review-project__cell-id">{unit.tmTextUnitId ?? '—'}</td>
    <td className="review-project__cell-name">{unit.name}</td>
    <td className="review-project__cell-source">{unit.source}</td>
    <td className="review-project__cell-target">{unit.target ?? '—'}</td>
    <td>{unit.status ?? '—'}</td>
    <td>{unit.repositoryName ?? '—'}</td>
    <td className="review-project__cell-asset">{unit.assetPath ?? '—'}</td>
  </tr>
);

const LocaleSection = ({ locale }: { locale: ApiReviewProjectLocaleDetail }) => (
  <section className="review-project__locale">
    <header className="review-project__locale-header">
      <div>
        <div className="review-project__locale-title">{locale.displayName ?? locale.bcp47Tag}</div>
        <div className="review-project__locale-sub">
          {formatNumber(locale.acceptedCount)} accepted · {formatNumber(locale.selectedCount)} total
        </div>
      </div>
      <LocaleBadge locale={locale} />
    </header>
    <div className="review-project__table-shell">
      <table className="review-project__table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Source</th>
            <th>Target</th>
            <th>Status</th>
            <th>Repository</th>
            <th>Asset</th>
          </tr>
        </thead>
        <tbody>
          {locale.textUnits.length ? (
            locale.textUnits.map((unit) => <TextUnitRow key={unit.reviewProjectTextUnitId} unit={unit} />)
          ) : (
            <tr>
              <td colSpan={7} className="review-project__empty-row">
                No text units for this locale.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  </section>
);

export function ReviewProjectPageView({ status, project, errorMessage, onRetry }: Props) {
  const acceptedTotal = useMemo(
    () => project?.locales?.reduce((sum, locale) => sum + (locale.acceptedCount ?? 0), 0) ?? 0,
    [project],
  );

  if (status === 'loading') {
    return <LoadingState />;
  }

  if (status === 'error') {
    return <ErrorState message={errorMessage} onRetry={onRetry} />;
  }

  if (!project) {
    return <ErrorState message="Review project not found." onRetry={onRetry} />;
  }

  return (
    <div className="review-project">
      <header className="review-project__header">
        <div className="review-project__crumb">
          <Link to="/review-projects" className="review-project__crumb-link">
            ← Review projects
          </Link>
        </div>
        <div className="review-project__title-row">
          <div>
            <div className="review-project__title">{project.name ?? `Review project #${project.id}`}</div>
            <div className="review-project__subtitle">ID #{project.id}</div>
          </div>
          <div className="review-project__pills">
            <Pill className="review-project__pill">
              {REVIEW_PROJECT_TYPE_LABELS[project.type] ?? project.type}
            </Pill>
            <Pill
              className={`review-project__pill ${
                project.status === 'CLOSED' ? 'review-project__pill--closed' : 'review-project__pill--open'
              }`}
            >
              {REVIEW_PROJECT_STATUS_LABELS[project.status] ?? project.status}
            </Pill>
          </div>
        </div>
        <div className="review-project__meta">
          <MetaItem label="Created" value={formatDate(project.createdDate)} />
          <MetaItem label="Due" value={formatDate(project.dueDate)} />
          <MetaItem label="Words" value={formatNumber(project.wordCount)} />
          <MetaItem label="Strings" value={formatNumber(project.textUnitCount)} />
          <MetaItem label="Accepted" value={formatNumber(acceptedTotal)} />
          {project.closeReason ? <MetaItem label="Close reason" value={project.closeReason} /> : null}
        </div>
        {project.repositories?.length ? (
          <div className="review-project__chips">
            {project.repositories.map((repo) => (
              <span key={repo.id} className="review-project__chip">
                {repo.name}
              </span>
            ))}
          </div>
        ) : null}
        {project.notes ? <div className="review-project__notes">{project.notes}</div> : null}
      </header>

      <section className="review-project__locales">
        <div className="review-project__section-title">Locales</div>
        <div className="review-project__locale-list">
          {project.locales?.map((locale) => (
            <Fragment key={`${locale.bcp47Tag}-${locale.id}`}>
              <LocaleSection locale={locale} />
            </Fragment>
          ))}
        </div>
      </section>
    </div>
  );
}
