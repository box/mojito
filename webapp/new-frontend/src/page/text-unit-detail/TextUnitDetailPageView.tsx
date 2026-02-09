import '../review-project/review-project-page.css';
import './text-unit-detail-page.css';

import { type ReactNode, useMemo } from 'react';

import type { AiReviewSuggestion } from '../../api/ai-review';
import { AiChatReview, type AiChatReviewMessage } from '../../components/AiChatReview';
import { AutoTextarea } from '../../components/AutoTextarea';
import { ConfirmModal } from '../../components/ConfirmModal';
import { IcuMessagePreview } from '../../components/IcuMessagePreview';
import { Pill } from '../../components/Pill';
import { PillDropdown } from '../../components/PillDropdown';
import { parseIcuMessage } from '../../utils/icuMessageFormat';

export type TextUnitDetailMetaRow = {
  label: string;
  value: string;
};

export type TextUnitDetailMetaSection = {
  title: string;
  rows: TextUnitDetailMetaRow[];
};

export type TextUnitDetailHistoryComment = {
  key: string;
  type: string;
  severity: string;
  content: string;
};

export type TextUnitDetailHistoryRow = {
  key: string;
  variantId: string;
  userName: string;
  translation: string;
  date: string;
  status: string;
  comments: TextUnitDetailHistoryComment[];
};

export type TextUnitDetailAiMessage = AiChatReviewMessage;

type TextUnitDetailPageViewProps = {
  tmTextUnitId: number;
  onBack: () => void;
  editorInfo: {
    target: string;
    status: string;
    statusOptions: string[];
    canEdit: boolean;
    canDelete: boolean;
    isDirty: boolean;
    isSaving: boolean;
    isDeleting: boolean;
    errorMessage: string | null;
    warningMessage: string | null;
  };
  keyInfo: {
    stringId: string;
    locale: string;
    source: string;
    comment: string;
    repositoryName: string;
  };
  onChangeTarget: (value: string) => void;
  onChangeStatus: (value: string) => void;
  onSaveEditor: () => void;
  onResetEditor: () => void;
  onRequestDeleteEditor: () => void;
  previewLocale: string;
  isIcuPreviewCollapsed: boolean;
  onToggleIcuPreviewCollapsed: () => void;
  icuPreviewMode: 'source' | 'target';
  onChangeIcuPreviewMode: (mode: 'source' | 'target') => void;
  isAiCollapsed: boolean;
  onToggleAiCollapsed: () => void;
  aiMessages: TextUnitDetailAiMessage[];
  aiInput: string;
  onChangeAiInput: (value: string) => void;
  onSubmitAi: () => void;
  onUseAiSuggestion: (suggestion: AiReviewSuggestion) => void;
  isAiResponding: boolean;
  isMetaCollapsed: boolean;
  onToggleMetaCollapsed: () => void;
  isMetaLoading: boolean;
  metaErrorMessage: string | null;
  metaSections: TextUnitDetailMetaSection[];
  metaWarningMessage: string | null;
  isHistoryCollapsed: boolean;
  onToggleHistoryCollapsed: () => void;
  isHistoryLoading: boolean;
  historyErrorMessage: string | null;
  historyMissingLocale: boolean;
  historyRows: TextUnitDetailHistoryRow[];
  historyInitialDate: string;
  showDeletedHistoryEntry: boolean;
  showValidationDialog: boolean;
  validationDialogBody: string;
  onConfirmValidationSave: () => void;
  onDismissValidationDialog: () => void;
  showDeleteDialog: boolean;
  deleteDialogBody: string;
  onConfirmDeleteEditor: () => void;
  onDismissDeleteDialog: () => void;
};

export function TextUnitDetailPageView({
  tmTextUnitId,
  onBack,
  editorInfo,
  keyInfo,
  onChangeTarget,
  onChangeStatus,
  onSaveEditor,
  onResetEditor,
  onRequestDeleteEditor,
  previewLocale,
  isIcuPreviewCollapsed,
  onToggleIcuPreviewCollapsed,
  icuPreviewMode,
  onChangeIcuPreviewMode,
  isAiCollapsed,
  onToggleAiCollapsed,
  aiMessages,
  aiInput,
  onChangeAiInput,
  onSubmitAi,
  onUseAiSuggestion,
  isAiResponding,
  isMetaCollapsed,
  onToggleMetaCollapsed,
  isMetaLoading,
  metaErrorMessage,
  metaSections,
  metaWarningMessage,
  isHistoryCollapsed,
  onToggleHistoryCollapsed,
  isHistoryLoading,
  historyErrorMessage,
  historyMissingLocale,
  historyRows,
  historyInitialDate,
  showDeletedHistoryEntry,
  showValidationDialog,
  validationDialogBody,
  onConfirmValidationSave,
  onDismissValidationDialog,
  showDeleteDialog,
  deleteDialogBody,
  onConfirmDeleteEditor,
  onDismissDeleteDialog,
}: TextUnitDetailPageViewProps) {
  const hasIcuSource = useMemo(() => {
    const source = keyInfo.source?.trim();
    if (!source || source === '-') {
      return false;
    }
    if (!source.includes('{')) {
      return false;
    }
    try {
      return parseIcuMessage(source).parameters.length > 0;
    } catch {
      return false;
    }
  }, [keyInfo.source]);

  const hasIcuTarget = useMemo(() => {
    const target = editorInfo.target?.trim();
    if (!target || target === '-') {
      return false;
    }
    if (!target.includes('{')) {
      return false;
    }
    try {
      return parseIcuMessage(target).parameters.length > 0;
    } catch {
      return false;
    }
  }, [editorInfo.target]);

  const missingSourceParameters = useMemo(() => {
    const source = keyInfo.source?.trim();
    if (!source || source === '-' || !source.includes('{')) {
      return [] as string[];
    }

    let sourceParameterNames: string[];
    try {
      sourceParameterNames = parseIcuMessage(source).parameters.map((parameter) => parameter.name);
    } catch {
      return [] as string[];
    }

    if (sourceParameterNames.length === 0) {
      return [];
    }

    const target = editorInfo.target?.trim();
    if (!target || target === '-') {
      return sourceParameterNames;
    }

    let targetParameterNames: Set<string>;
    try {
      targetParameterNames = new Set(
        parseIcuMessage(target).parameters.map((parameter) => parameter.name),
      );
    } catch {
      return sourceParameterNames;
    }

    return sourceParameterNames.filter((name) => !targetParameterNames.has(name));
  }, [editorInfo.target, keyInfo.source]);

  const selectedIcuMode =
    icuPreviewMode === 'source' && hasIcuSource
      ? 'source'
      : hasIcuTarget
        ? 'target'
        : hasIcuSource
          ? 'source'
          : 'target';

  const selectedIcuMessage = selectedIcuMode === 'source' ? keyInfo.source : editorInfo.target;
  const selectedIcuLocale = selectedIcuMode === 'source' ? 'en' : previewLocale;
  const hasIcuMessage = hasIcuSource || hasIcuTarget;

  return (
    <div className="review-project-page text-unit-detail-page">
      <header className="review-project-page__header">
        <div className="review-project-page__header-row">
          <div className="review-project-page__header-group review-project-page__header-group--left">
            <button
              type="button"
              className="review-project-page__header-back-link"
              onClick={onBack}
              aria-label="Back to workbench"
              title="Back to workbench"
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
            <span className="review-project-page__header-name">Text unit #{tmTextUnitId}</span>
            <div className="text-unit-detail-page__header-context">
              <Pill>{keyInfo.locale}</Pill>
              <span
                className="text-unit-detail-page__header-repository"
                title={keyInfo.repositoryName}
              >
                {keyInfo.repositoryName}
              </span>
            </div>
          </div>
          <div className="review-project-page__header-group review-project-page__header-group--stats" />
          <div className="review-project-page__header-group review-project-page__header-group--meta" />
        </div>
      </header>

      <div className="text-unit-detail-page__content">
        <div className="text-unit-detail-page__layout">
          <section className="text-unit-detail-page__panel text-unit-detail-page__panel--editor">
            <h1 className="text-unit-detail-page__title">Translation</h1>

            <div className="text-unit-detail-page__editor-field">
              <AutoTextarea
                className="text-unit-detail-page__editor-textarea"
                value={editorInfo.target}
                onChange={(event) => onChangeTarget(event.target.value)}
                disabled={!editorInfo.canEdit || editorInfo.isSaving}
                placeholder="Add translated copy"
                rows={1}
                style={{ resize: 'none' }}
              />
            </div>

            <div className="text-unit-detail-page__editor-controls">
              <div className="text-unit-detail-page__editor-status">
                <PillDropdown
                  value={editorInfo.status}
                  options={editorInfo.statusOptions.map((option) => ({
                    value: option,
                    label: option,
                  }))}
                  onChange={onChangeStatus}
                  disabled={!editorInfo.canEdit || editorInfo.isSaving}
                  ariaLabel="Translation status"
                />
              </div>
              <div className="text-unit-detail-page__editor-actions">
                <button
                  type="button"
                  className="text-unit-detail-page__button"
                  onClick={onRequestDeleteEditor}
                  disabled={
                    !editorInfo.canDelete ||
                    editorInfo.isSaving ||
                    editorInfo.isDeleting ||
                    !editorInfo.canEdit
                  }
                >
                  {editorInfo.isDeleting ? 'Deleting…' : 'Delete'}
                </button>
                <button
                  type="button"
                  className="text-unit-detail-page__button"
                  onClick={onResetEditor}
                  disabled={!editorInfo.isDirty || editorInfo.isSaving || editorInfo.isDeleting}
                >
                  Reset
                </button>
                <button
                  type="button"
                  className="text-unit-detail-page__button text-unit-detail-page__button--primary"
                  onClick={onSaveEditor}
                  disabled={
                    !editorInfo.canEdit ||
                    !editorInfo.isDirty ||
                    editorInfo.isSaving ||
                    editorInfo.isDeleting
                  }
                >
                  {editorInfo.isSaving ? 'Saving…' : 'Save'}
                </button>
              </div>
            </div>

            {hasIcuMessage ? (
              <section className="text-unit-detail-page__panel text-unit-detail-page__panel--section text-unit-detail-page__panel--icu-inline">
                <SectionHeader
                  title="ICU preview"
                  expanded={!isIcuPreviewCollapsed}
                  onToggle={onToggleIcuPreviewCollapsed}
                  controls={
                    <div
                      className="text-unit-detail-page__icu-toggle text-unit-detail-page__icu-toggle--compact"
                      role="group"
                      aria-label="ICU message"
                    >
                      <button
                        type="button"
                        className={`text-unit-detail-page__icu-toggle-option ${
                          selectedIcuMode === 'target' ? 'is-active' : ''
                        }`}
                        onClick={() => onChangeIcuPreviewMode('target')}
                        disabled={!hasIcuTarget}
                      >
                        Target
                      </button>
                      <button
                        type="button"
                        className={`text-unit-detail-page__icu-toggle-option ${
                          selectedIcuMode === 'source' ? 'is-active' : ''
                        }`}
                        onClick={() => onChangeIcuPreviewMode('source')}
                        disabled={!hasIcuSource}
                      >
                        Source
                      </button>
                    </div>
                  }
                  summary={
                    missingSourceParameters.length > 0 ? (
                      <span className="text-unit-detail-page__section-summary-warning">
                        Missing in target:{' '}
                        {missingSourceParameters.map((name) => `{${name}}`).join(', ')}
                      </span>
                    ) : undefined
                  }
                />
                {!isIcuPreviewCollapsed ? (
                  <div className="text-unit-detail-page__icu-content">
                    <IcuMessagePreview
                      message={selectedIcuMessage}
                      locale={selectedIcuLocale}
                      showMessageEditor={false}
                      showLocaleInput={false}
                      showExamples={false}
                    />
                  </div>
                ) : null}
              </section>
            ) : null}

            <section className="text-unit-detail-page__panel text-unit-detail-page__panel--section text-unit-detail-page__panel--ai-inline">
              <SectionHeader
                title="AI Chat Review"
                expanded={!isAiCollapsed}
                onToggle={onToggleAiCollapsed}
              />
              {!isAiCollapsed ? (
                <AiChatReview
                  messages={aiMessages}
                  input={aiInput}
                  onChangeInput={onChangeAiInput}
                  onSubmit={onSubmitAi}
                  onUseSuggestion={onUseAiSuggestion}
                  isResponding={isAiResponding}
                />
              ) : null}
            </section>

            {editorInfo.warningMessage ? (
              <div className="text-unit-detail-page__state text-unit-detail-page__state--warning">
                {editorInfo.warningMessage}
              </div>
            ) : null}
            {editorInfo.errorMessage ? (
              <div className="text-unit-detail-page__state text-unit-detail-page__state--error">
                {editorInfo.errorMessage}
              </div>
            ) : null}
          </section>

          <div className="text-unit-detail-page__side">
            <section className="text-unit-detail-page__panel text-unit-detail-page__panel--section">
              <dl className="text-unit-detail-page__key-info">
                <div className="text-unit-detail-page__key-info-row">
                  <dt>Source</dt>
                  <dd>
                    <pre className="text-unit-detail-page__key-info-text text-unit-detail-page__key-info-text--primary">
                      {keyInfo.source}
                    </pre>
                  </dd>
                </div>
                <div className="text-unit-detail-page__key-info-row">
                  <dt>Comment</dt>
                  <dd>
                    <pre className="text-unit-detail-page__key-info-text text-unit-detail-page__key-info-text--primary">
                      {keyInfo.comment}
                    </pre>
                  </dd>
                </div>
                <div className="text-unit-detail-page__key-info-row">
                  <dt>Id</dt>
                  <dd>
                    <pre className="text-unit-detail-page__key-info-text">{keyInfo.stringId}</pre>
                  </dd>
                </div>
              </dl>
            </section>

            <section className="text-unit-detail-page__panel text-unit-detail-page__panel--section">
              <SectionHeader
                title={`History (${historyRows.length + 1})`}
                expanded={!isHistoryCollapsed}
                onToggle={onToggleHistoryCollapsed}
              />
              {!isHistoryCollapsed ? (
                historyMissingLocale ? (
                  <div className="text-unit-detail-page__state text-unit-detail-page__state--warning">
                    Missing locale. Open this page from the workbench row to load history.
                  </div>
                ) : isHistoryLoading ? (
                  <div className="text-unit-detail-page__state">
                    <span className="spinner spinner--md" aria-hidden />
                    <span>Loading history…</span>
                  </div>
                ) : historyErrorMessage ? (
                  <div className="text-unit-detail-page__state text-unit-detail-page__state--error">
                    {historyErrorMessage}
                  </div>
                ) : (
                  <ol className="text-unit-detail-page__timeline">
                    {showDeletedHistoryEntry ? (
                      <li className="text-unit-detail-page__timeline-item">
                        <div className="text-unit-detail-page__timeline-dot" aria-hidden="true" />
                        <div className="text-unit-detail-page__timeline-card">
                          <div className="text-unit-detail-page__timeline-header">
                            <div className="text-unit-detail-page__timeline-summary">
                              <span className="text-unit-detail-page__timeline-title">
                                Translation deleted
                              </span>
                              <span className="text-unit-detail-page__timeline-summary-separator">
                                &middot;
                              </span>
                              <span className="text-unit-detail-page__timeline-summary-meta">-</span>
                              <span className="text-unit-detail-page__timeline-summary-separator">
                                &middot;
                              </span>
                              <span className="text-unit-detail-page__timeline-summary-status">
                                Deleted
                              </span>
                            </div>
                            <time className="text-unit-detail-page__timeline-time">-</time>
                          </div>
                          <pre className="text-unit-detail-page__timeline-content">
                            {'<no current translation>'}
                          </pre>
                        </div>
                      </li>
                    ) : null}
                    {historyRows.map((item) => (
                      <li key={item.key} className="text-unit-detail-page__timeline-item">
                        <div className="text-unit-detail-page__timeline-dot" aria-hidden="true" />
                        <div className="text-unit-detail-page__timeline-card">
                          <div className="text-unit-detail-page__timeline-header">
                            <div className="text-unit-detail-page__timeline-summary">
                              <span className="text-unit-detail-page__timeline-title">
                                Translation updated
                                <span className="text-unit-detail-page__timeline-title-meta">
                                  #{item.variantId}
                                </span>
                              </span>
                              <span className="text-unit-detail-page__timeline-summary-separator">
                                &middot;
                              </span>
                              <span className="text-unit-detail-page__timeline-summary-meta">
                                {item.userName}
                              </span>
                              {item.status !== '-' ? (
                                <>
                                  <span className="text-unit-detail-page__timeline-summary-separator">
                                    &middot;
                                  </span>
                                  <span className="text-unit-detail-page__timeline-summary-status">
                                    {item.status}
                                  </span>
                                </>
                              ) : null}
                            </div>
                            <time className="text-unit-detail-page__timeline-time">
                              {item.date}
                            </time>
                          </div>
                          <pre className="text-unit-detail-page__timeline-content">
                            {item.translation}
                          </pre>
                          {item.comments.length > 0 ? (
                            <table className="text-unit-detail-page__history-comment-table">
                              <thead>
                                <tr>
                                  <th>type</th>
                                  <th>severity</th>
                                  <th>content</th>
                                </tr>
                              </thead>
                              <tbody>
                                {item.comments.map((comment) => (
                                  <tr key={comment.key}>
                                    <td>{comment.type}</td>
                                    <td>{comment.severity}</td>
                                    <td>{comment.content}</td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          ) : null}
                        </div>
                      </li>
                    ))}
                    <li className="text-unit-detail-page__timeline-item">
                      <div className="text-unit-detail-page__timeline-dot" aria-hidden="true" />
                      <div className="text-unit-detail-page__timeline-card">
                        <div className="text-unit-detail-page__timeline-header">
                          <div className="text-unit-detail-page__timeline-summary">
                            <span className="text-unit-detail-page__timeline-title">
                              Text unit created (untranslated)
                            </span>
                            <span className="text-unit-detail-page__timeline-summary-separator">
                              &middot;
                            </span>
                            <span className="text-unit-detail-page__timeline-summary-meta">-</span>
                            <span className="text-unit-detail-page__timeline-summary-separator">
                              &middot;
                            </span>
                            <span className="text-unit-detail-page__timeline-summary-status">
                              Untranslated
                            </span>
                          </div>
                          <time className="text-unit-detail-page__timeline-time">
                            {historyInitialDate}
                          </time>
                        </div>
                        <pre className="text-unit-detail-page__timeline-content">
                          {'<no translation yet>'}
                        </pre>
                      </div>
                    </li>
                  </ol>
                )
              ) : null}
            </section>

            <section className="text-unit-detail-page__panel text-unit-detail-page__panel--section">
              <SectionHeader
                title="Metadata"
                expanded={!isMetaCollapsed}
                onToggle={onToggleMetaCollapsed}
              />

              {!isMetaCollapsed ? (
                isMetaLoading ? (
                  <div className="text-unit-detail-page__state">
                    <span className="spinner spinner--md" aria-hidden />
                    <span>Loading text unit details…</span>
                  </div>
                ) : metaErrorMessage ? (
                  <div className="text-unit-detail-page__state text-unit-detail-page__state--error">
                    {metaErrorMessage}
                  </div>
                ) : (
                  <div className="text-unit-detail-page__sections">
                    {metaSections.map((section) => (
                      <MetaSection key={section.title} title={section.title} rows={section.rows} />
                    ))}

                    {metaWarningMessage ? (
                      <div className="text-unit-detail-page__state text-unit-detail-page__state--warning">
                        {metaWarningMessage}
                      </div>
                    ) : null}
                  </div>
                )
              ) : null}
            </section>
          </div>
        </div>
      </div>

      <ConfirmModal
        open={showValidationDialog}
        title="Translation check failed"
        body={validationDialogBody}
        confirmLabel="Save anyway"
        cancelLabel="Keep editing"
        onConfirm={onConfirmValidationSave}
        onCancel={onDismissValidationDialog}
      />
      <ConfirmModal
        open={showDeleteDialog}
        title="Delete?"
        body={deleteDialogBody}
        confirmLabel="Delete"
        cancelLabel="Cancel"
        onConfirm={onConfirmDeleteEditor}
        onCancel={onDismissDeleteDialog}
      />
    </div>
  );
}

function SectionHeader({
  title,
  expanded,
  onToggle,
  summary,
  controls,
}: {
  title: string;
  expanded: boolean;
  onToggle: () => void;
  summary?: ReactNode;
  controls?: ReactNode;
}) {
  return (
    <div className="text-unit-detail-page__section-header-row">
      <button
        type="button"
        className="text-unit-detail-page__section-header"
        onClick={onToggle}
        aria-expanded={expanded}
      >
        <span className="text-unit-detail-page__section-heading">
          <span className="text-unit-detail-page__section-title">{title}</span>
          {summary ? (
            <span className="text-unit-detail-page__section-summary">{summary}</span>
          ) : null}
        </span>
      </button>
      {controls ? <div className="text-unit-detail-page__section-controls">{controls}</div> : null}
      <button
        type="button"
        className="text-unit-detail-page__section-action"
        onClick={onToggle}
        aria-expanded={expanded}
      >
        {expanded ? 'Hide' : 'Show'}
      </button>
    </div>
  );
}

function MetaSection({ title, rows }: { title: string; rows: TextUnitDetailMetaRow[] }) {
  return (
    <section className="text-unit-detail-page__meta-section">
      <h3 className="text-unit-detail-page__meta-title">{title}</h3>
      <dl className="text-unit-detail-page__meta">
        {rows.map((row) => (
          <div key={row.label} className="text-unit-detail-page__meta-row">
            <dt>{row.label}</dt>
            <dd>{row.value}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}
