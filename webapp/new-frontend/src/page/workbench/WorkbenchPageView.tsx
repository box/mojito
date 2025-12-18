import './workbench-page.css';

import type { RefObject } from 'react';

import { AutoTextarea } from '../../components/AutoTextarea';
import { UnsavedChangesModal } from '../../components/UnsavedChangesModal';
import { isRtlLocale } from '../../utils/localeDirection';

export type WorkbenchRow = {
  id: string;
  textUnitName: string;
  repositoryName: string;
  assetPath: string;
  locale: string;
  source: string;
  translation: string | null;
  status: string;
  comment: string | null;
};

type Props = {
  rows: WorkbenchRow[];
  editingRowId: string | null;
  editingValue: string;
  onStartEditing: (rowId: string, translation: string | null) => void;
  onCancelEditing: () => void;
  onSaveEditing: () => void;
  onChangeEditingValue: (value: string) => void;
  onChangeStatus: (rowId: string, status: string) => void;
  statusOptions: string[];
  showDiscardDialog: boolean;
  onConfirmDiscardEditing: () => void;
  onDismissDiscardEditing: () => void;
  translationInputRef: RefObject<HTMLTextAreaElement | null>;
  registerRowRef: (rowId: string, element: HTMLDivElement | null) => void;
};

export function WorkbenchPageView({
  rows,
  editingRowId,
  editingValue,
  onStartEditing,
  onCancelEditing,
  onSaveEditing,
  onChangeEditingValue,
  onChangeStatus,
  statusOptions,
  showDiscardDialog,
  onConfirmDiscardEditing,
  onDismissDiscardEditing,
  translationInputRef,
  registerRowRef,
}: Props) {
  return (
    <div className="workbench-page">
      <div className="workbench-page__header">Search translations across repositories.</div>
      <div className="workbench-page__body">
        <div className="workbench-page__table-header">
          <div className="workbench-page__table-header-cell">Text unit (id, asset, repository)</div>
          <div className="workbench-page__table-header-cell workbench-page__table-header-cell--source">
            Source &amp; comment
          </div>
          <div className="workbench-page__table-header-cell workbench-page__table-header-cell--translation">
            Translation
          </div>
          <div className="workbench-page__table-header-cell workbench-page__table-header-cell--locale">
            Locale
          </div>
          <div className="workbench-page__table-header-cell workbench-page__table-header-cell--status">
            Status
          </div>
        </div>
        <div className="workbench-page__rows">
          {rows.map((row) => {
            const isEditing = editingRowId === row.id;
            const translationValue = isEditing ? editingValue : (row.translation ?? '');
            const translationDirection = isRtlLocale(row.locale) ? 'rtl' : 'ltr';
            const translationLocale = row.locale;
            const translationStyle = isEditing ? undefined : { resize: 'none' as const };

            return (
              <div
                key={row.id}
                className="workbench-page__row"
                ref={(element) => registerRowRef(row.id, element)}
              >
                <div className="workbench-page__cell workbench-page__cell--meta">
                  <span className="workbench-page__meta-id">{row.textUnitName}</span>
                  <span className="workbench-page__meta-asset">{row.assetPath}</span>
                  <span className="workbench-page__repo-name">{row.repositoryName}</span>
                </div>
                <div className="workbench-page__cell workbench-page__cell--source">
                  <div className="workbench-page__text-block workbench-page__source-text">
                    {row.source ?? ''}
                  </div>
                  <div className="workbench-page__text-block workbench-page__source-comment">
                    {row.comment ?? ''}
                  </div>
                </div>
                <div
                  className="workbench-page__cell workbench-page__cell--translation"
                  data-editing={isEditing ? 'true' : undefined}
                >
                  <AutoTextarea
                    className="workbench-page__translation-input"
                    value={translationValue}
                    onFocus={() => {
                      if (!isEditing) {
                        onStartEditing(row.id, row.translation);
                      }
                    }}
                    onChange={
                      isEditing ? (event) => onChangeEditingValue(event.target.value) : undefined
                    }
                    readOnly={!isEditing}
                    ref={isEditing ? translationInputRef : undefined}
                    lang={translationLocale}
                    dir={translationDirection}
                    style={translationStyle}
                  />
                  {isEditing ? (
                    <div className="workbench-page__translation-actions">
                      <button
                        type="button"
                        className="workbench-page__translation-button workbench-page__translation-button--primary"
                        onClick={(event) => {
                          event.stopPropagation();
                          onSaveEditing();
                        }}
                      >
                        Accept
                      </button>
                      <button
                        type="button"
                        className="workbench-page__translation-button"
                        onClick={(event) => {
                          event.stopPropagation();
                          onCancelEditing();
                        }}
                      >
                        Cancel
                      </button>
                    </div>
                  ) : null}
                </div>
                <div className="workbench-page__cell workbench-page__cell--locale">
                  <span className="workbench-page__locale-pill">{row.locale}</span>
                </div>
                <div className="workbench-page__cell workbench-page__cell--status">
                  <select
                    className="workbench-page__status-select"
                    value={row.status}
                    aria-label="Translation status"
                    onChange={(event) => onChangeStatus(row.id, event.target.value)}
                  >
                    {statusOptions.map((statusOption) => (
                      <option key={statusOption} value={statusOption}>
                        {statusOption}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            );
          })}
        </div>
      </div>
      <UnsavedChangesModal
        open={showDiscardDialog}
        title="Unsaved translation"
        body="You have unsaved edits. Do you want to discard them?"
        confirmLabel="Discard & switch"
        cancelLabel="Keep editing"
        onConfirm={onConfirmDiscardEditing}
        onCancel={onDismissDiscardEditing}
      />
    </div>
  );
}
