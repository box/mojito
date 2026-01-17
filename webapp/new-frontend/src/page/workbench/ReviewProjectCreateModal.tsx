import './workbench-page.css';

import { useEffect, useMemo, useState } from 'react';

import {
  type ApiReviewProjectType,
  REVIEW_PROJECT_TYPE_LABELS,
  REVIEW_PROJECT_TYPES,
} from '../../api/review-projects';
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
  }) => void;
  defaultName: string;
  defaultDueDate: string;
  localeOptions: LocaleSelectionOption[];
  collectionSize: number;
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
      (!maxTextUnits || maxTextUnits > 0),
    [dueDate, maxTextUnits, name, selectedLocales.length],
  );

  if (!isOpen) {
    return null;
  }

  return (
    <Modal
      open={isOpen}
      onClose={onCancel}
      size="md"
      ariaLabel="Create review project"
      closeOnBackdrop
    >
      <div className="modal__header">
        <div className="modal__title">Create review project</div>
      </div>
      <div className="modal__body workbench-modal__body">
        <label className="workbench-modal__field">
          <span className="workbench-modal__label">Name</span>
          <input
            type="text"
            value={name}
            onChange={(event) => setName(event.target.value)}
            maxLength={120}
          />
        </label>
        <label className="workbench-modal__field">
          <span className="workbench-modal__label">Due date</span>
          <input
            type="datetime-local"
            value={dueDate}
            onChange={(event) => setDueDate(event.target.value)}
          />
        </label>
        <label className="workbench-modal__field">
          <span className="workbench-modal__label">Type</span>
          <select
            value={type}
            onChange={(event) => setType(event.target.value as ApiReviewProjectType)}
          >
            {REVIEW_PROJECT_TYPES.filter((t) => t !== 'UNKNOWN').map((option) => (
              <option key={option} value={option}>
                {REVIEW_PROJECT_TYPE_LABELS[option]}
              </option>
            ))}
          </select>
        </label>
        <label className="workbench-modal__field">
          <span className="workbench-modal__label">Locales</span>
          <div className="workbench-modal__checkboxes">
            {localeOptions.map((option) => {
              const checked = selectedLocales.includes(option.tag);
              return (
                <label key={option.tag} className="workbench-modal__checkbox">
                  <input
                    type="checkbox"
                    checked={checked}
                    onChange={(event) => {
                      if (event.target.checked) {
                        setSelectedLocales([...selectedLocales, option.tag]);
                      } else {
                        setSelectedLocales(selectedLocales.filter((tag) => tag !== option.tag));
                      }
                    }}
                  />
                  <span>{option.label}</span>
                </label>
              );
            })}
            {localeOptions.length === 0 ? (
              <div className="workbench-modal__hint">No locales found for this collection.</div>
            ) : null}
          </div>
        </label>
        <label className="workbench-modal__field">
          <span className="workbench-modal__label">Max text units</span>
          <input
            type="number"
            min={1}
            value={maxTextUnits ?? ''}
            placeholder="Defaults to collection size"
            onChange={(event) => {
              const value = event.target.value;
              if (!value.length) {
                setMaxTextUnits(null);
                return;
              }
              const parsed = Number(value);
              setMaxTextUnits(Number.isFinite(parsed) ? parsed : null);
            }}
          />
          <div className="workbench-modal__hint">
            Collection has {collectionSize} ids; leave blank to use that size.
          </div>
        </label>
        <label className="workbench-modal__field">
          <span className="workbench-modal__label">Notes (optional)</span>
          <textarea
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
            rows={3}
            maxLength={400}
          />
        </label>
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
            if (!canSubmit) return;
            const dueIso = new Date(dueDate).toISOString();
            onCreate({
              name: name.trim(),
              dueDate: dueIso,
              type,
              localeTags: selectedLocales,
              maxTextUnits: maxTextUnits ?? collectionSize,
              notes: notes.trim() || null,
            });
          }}
          disabled={!canSubmit}
        >
          Create
        </button>
      </div>
    </Modal>
  );
}
