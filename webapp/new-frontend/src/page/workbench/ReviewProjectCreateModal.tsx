import './workbench-page.css';

import { useEffect, useMemo, useState } from 'react';

import {
  type ApiReviewProjectType,
  REVIEW_PROJECT_TYPE_LABELS,
  REVIEW_PROJECT_TYPES,
} from '../../api/review-projects';
import { LocaleMultiSelect } from '../../components/LocaleMultiSelect';
import { Modal } from '../../components/Modal';
import type { LocaleSelectionOption } from '../../utils/localeSelection';

type Props = {
  isOpen: boolean;
  onCancel: () => void;
  onCreate: (payload: {
    name: string;
    dueDate: string;
    type: ApiReviewProjectType;
    localeTags: string[];
    maxTextUnits: number | null;
    notes: string | null;
    tmTextUnitIds: number[];
  }) => void;
  defaultName: string;
  defaultDueDate: string;
  localeOptions: LocaleSelectionOption[];
  collectionSize: number;
  tmTextUnitIds: number[];
  collectionName?: string | null;
  myLocaleTags?: string[];
  isSubmitting?: boolean;
  errorMessage?: string | null;
};

export function ReviewProjectCreateModal({
  isOpen,
  onCancel,
  onCreate,
  defaultName,
  defaultDueDate,
  localeOptions,
  collectionSize,
  tmTextUnitIds,
  collectionName,
  myLocaleTags,
  isSubmitting = false,
  errorMessage,
}: Props) {
  const [name, setName] = useState(defaultName);
  const [dueDate, setDueDate] = useState(defaultDueDate);
  const [type, setType] = useState<ApiReviewProjectType>('NORMAL');
  const [notes, setNotes] = useState('');
  const [maxTextUnits, setMaxTextUnits] = useState<number | null>(collectionSize || null);
  const [selectedLocales, setSelectedLocales] = useState<string[]>(
    localeOptions.map((opt) => opt.tag),
  );
  const collectionLabel = collectionName?.trim()?.length ? collectionName.trim() : 'Active collection';

  useEffect(() => {
    if (!isOpen) {
      return;
    }
    setName(defaultName);
    setDueDate(defaultDueDate);
    setType('NORMAL');
    setNotes('');
    setMaxTextUnits(collectionSize || null);
    setSelectedLocales(localeOptions.map((opt) => opt.tag));
  }, [collectionSize, defaultDueDate, defaultName, isOpen, localeOptions]);

  const canSubmit = useMemo(
    () =>
      Boolean(name.trim()) &&
      Boolean(dueDate) &&
      selectedLocales.length > 0 &&
      (maxTextUnits === null || maxTextUnits > 0),
    [dueDate, maxTextUnits, name, selectedLocales.length],
  );

  const selectedLocaleSummary = useMemo(() => {
    if (!selectedLocales.length) {
      return 'Pick locales';
    }
    const labelByTag = new Map(
      localeOptions.map((option) => [option.tag.toLowerCase(), option.label]),
    );
    const labels = selectedLocales.map(
      (tag) => labelByTag.get(tag.toLowerCase()) ?? tag,
    );
    if (labels.length <= 2) {
      return labels.join(', ');
    }
    return `${labels.slice(0, 2).join(', ')} +${labels.length - 2}`;
  }, [localeOptions, selectedLocales]);

  const effectiveMax = maxTextUnits ?? collectionSize;

  if (!isOpen) {
    return null;
  }

  return (
    <Modal
      open={isOpen}
      onClose={onCancel}
      size="lg"
      ariaLabel="Create review project"
      closeOnBackdrop
    >
      <div className="modal__header">
        <div className="modal__title">Create review project</div>
      </div>
      <div className="modal__body workbench-modal__body">
        <div className="workbench-modal__lede">
          Launch a focused review using {collectionLabel.toLowerCase()}.
        </div>
        <div className="workbench-modal__grid">
          <div className="workbench-modal__panel">
            <label className="workbench-modal__field">
              <span className="workbench-modal__label">Name</span>
              <input
                className="workbench-modal__input"
                type="text"
              value={name}
              onChange={(event) => setName(event.target.value)}
              maxLength={120}
              placeholder="e.g. Release 12.3 review"
              disabled={isSubmitting}
            />
          </label>
          <div className="workbench-modal__two-up">
            <label className="workbench-modal__field">
              <span className="workbench-modal__label">Due date</span>
              <input
                className="workbench-modal__input"
                type="datetime-local"
                value={dueDate}
                onChange={(event) => setDueDate(event.target.value)}
                disabled={isSubmitting}
              />
            </label>
            <label className="workbench-modal__field">
              <span className="workbench-modal__label">Type</span>
              <select
                className="workbench-modal__select"
                value={type}
                onChange={(event) => setType(event.target.value as ApiReviewProjectType)}
                disabled={isSubmitting}
              >
                {REVIEW_PROJECT_TYPES.filter((t) => t !== 'UNKNOWN').map((option) => (
                  <option key={option} value={option}>
                    {REVIEW_PROJECT_TYPE_LABELS[option]}
                  </option>
                  ))}
                </select>
              </label>
            </div>

            <div className="workbench-modal__field">
              <div className="workbench-modal__label-row">
                <span className="workbench-modal__label">Locales</span>
                <span className="workbench-modal__hint">Pick one or more to include.</span>
              </div>
              <LocaleMultiSelect
                label="Locales"
                options={localeOptions}
                selectedTags={selectedLocales}
                onChange={setSelectedLocales}
                className="workbench-modal__locale-select"
                myLocaleTags={myLocaleTags}
                myLocalesLabel="My locales"
                buttonAriaLabel="Select locales for this review project"
                disabled={isSubmitting}
              />
              {localeOptions.length === 0 ? (
                <div className="workbench-modal__hint">No locales available for this collection.</div>
              ) : null}
            </div>

            <div className="workbench-modal__two-up">
              <label className="workbench-modal__field">
                <span className="workbench-modal__label">Max text units</span>
                <input
                  className="workbench-modal__input"
                  type="number"
                  min={1}
                  value={maxTextUnits ?? ''}
                  placeholder={`Use up to ${collectionSize.toLocaleString()}`}
                  onChange={(event) => {
                    const value = event.target.value;
                    if (!value.length) {
                      setMaxTextUnits(null);
                      return;
                    }
                    const parsed = Number(value);
                    setMaxTextUnits(Number.isFinite(parsed) ? parsed : null);
                  }}
                  disabled={isSubmitting}
                />
                <div className="workbench-modal__hint">
                  Leave blank to include every id from this collection.
                </div>
              </label>
              <label className="workbench-modal__field">
                <span className="workbench-modal__label">Notes (optional)</span>
                <textarea
                  className="workbench-modal__textarea"
                value={notes}
                onChange={(event) => setNotes(event.target.value)}
                rows={3}
                maxLength={400}
                placeholder="Call out priorities, owners, or scope."
                disabled={isSubmitting}
              />
            </label>
          </div>
        </div>

          <aside className="workbench-modal__summary">
            <div className="workbench-modal__summary-title">{collectionLabel}</div>
            <div className="workbench-modal__stat">
              <span>Text units</span>
              <strong>{collectionSize.toLocaleString()}</strong>
            </div>
            <div className="workbench-modal__stat">
              <span>Locales</span>
              <strong>{selectedLocaleSummary}</strong>
            </div>
            <div className="workbench-modal__stat">
              <span>Limit</span>
              <strong>{effectiveMax ? effectiveMax.toLocaleString() : 'All selected'}</strong>
            </div>
            <div className="workbench-modal__hint workbench-modal__summary-hint">
              IDs from this collection will be sent to the review project. If a locale is missing a
              translation, that text unit is skipped.
            </div>
          </aside>
        </div>
      </div>
      <div className="modal__actions workbench-modal__actions">
        {errorMessage ? <div className="workbench-modal__error">{errorMessage}</div> : null}
        <button type="button" className="workbench-worksetbar__button" onClick={onCancel}>
          Cancel
        </button>
        <button
          type="button"
          className="workbench-share-button"
          onClick={() => {
            if (!canSubmit || isSubmitting) return;
            const dueIso = new Date(dueDate).toISOString();
            onCreate({
              name: name.trim(),
              dueDate: dueIso,
              type,
              localeTags: selectedLocales,
              maxTextUnits: maxTextUnits ?? collectionSize,
              notes: notes.trim() || null,
              tmTextUnitIds,
            });
          }}
          disabled={!canSubmit || isSubmitting}
        >
          {isSubmitting ? (
            <>
              <span className="spinner" aria-hidden="true" /> Creating…
            </>
          ) : (
            'Create'
          )}
        </button>
      </div>
    </Modal>
  );
}
