import './review-projects-page.css';

import { useEffect, useMemo, useRef, useState } from 'react';

import {
  type ApiReviewProjectType,
  REVIEW_PROJECT_TYPE_LABELS,
  REVIEW_PROJECT_TYPES,
} from '../../api/review-projects';
import type { LocaleSelectionOption } from '../../utils/localeSelection';

export type ReviewProjectCreateFormValues = {
  name: string;
  dueDate: string;
  type: ApiReviewProjectType;
  localeTags: string[];
  notes: string | null;
  tmTextUnitIds: number[];
  screenshotImageIds: string[];
  maxTextUnits?: number | null;
};

export type CollectionOption = { id: string; name: string; size: number };

type Props = {
  defaultName: string;
  defaultDueDate: string;
  localeOptions: LocaleSelectionOption[];
  collectionSize: number;
  tmTextUnitIds: number[];
  collectionName?: string | null;
  collectionOptions?: CollectionOption[];
  selectedCollectionId?: string | null;
  onChangeCollection?: (id: string | null) => void;
  isSubmitting?: boolean;
  errorMessage?: string | null;
  submitLabel?: string;
  onSubmit: (payload: ReviewProjectCreateFormValues) => void;
  onCancel?: () => void;
};

export function ReviewProjectCreateForm({
  defaultName,
  defaultDueDate,
  localeOptions,
  collectionSize,
  tmTextUnitIds,
  collectionName,
  collectionOptions,
  selectedCollectionId,
  onChangeCollection,
  isSubmitting = false,
  errorMessage,
  submitLabel = 'Create',
  onSubmit,
  onCancel,
}: Props) {
  const [name, setName] = useState(defaultName);
  const [dueDate, setDueDate] = useState(defaultDueDate);
  const [type, setType] = useState<ApiReviewProjectType>('NORMAL');
  const [notes, setNotes] = useState('');
  const [screenshotKeys, setScreenshotKeys] = useState<string[]>([]);
  const [screenshotDraft, setScreenshotDraft] = useState('');
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => setName(defaultName), [defaultName]);
  useEffect(() => setDueDate(defaultDueDate), [defaultDueDate]);

  const localeTags = useMemo(
    () => localeOptions.map((opt) => opt.tag),
    [localeOptions],
  );

  const canSubmit = useMemo(
    () => Boolean(name.trim()) && Boolean(dueDate) && tmTextUnitIds.length > 0,
    [dueDate, name, tmTextUnitIds.length],
  );

  const addScreenshotKeys = (raw: string[]) => {
    const next = raw
      .map((item) => item.trim())
      .filter(Boolean)
      .map((item) => item.slice(0, 255));
    if (!next.length) return;
    setScreenshotKeys((current) => {
      const set = new Set(current.map((key) => key.toLowerCase()));
      const merged = [...current];
      next.forEach((key) => {
        if (!set.has(key.toLowerCase())) {
          merged.push(key);
          set.add(key.toLowerCase());
        }
      });
      return merged;
    });
  };

  const handleScreenshotDraftCommit = () => {
    if (!screenshotDraft.trim()) return;
    addScreenshotKeys(screenshotDraft.split(/[\n,]/).map((value) => value.trim()));
    setScreenshotDraft('');
  };

  const handleFiles = (files: FileList | null) => {
    if (!files || files.length === 0) return;
    const names = Array.from(files)
      .map((file) => file.name)
      .filter(Boolean);
    addScreenshotKeys(names);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  return (
    <div className="review-create__body">
      <div className="review-create__stack">
        <label className="review-create__field">
          <span className="review-create__label">Project name</span>
          <input
            className="review-create__input"
            type="text"
            value={name}
            onChange={(event) => setName(event.target.value)}
            maxLength={120}
            placeholder="e.g. Release 12.3 review"
            disabled={isSubmitting}
          />
        </label>

        {collectionOptions && onChangeCollection ? (
          <label className="review-create__field">
            <span className="review-create__label">Collection</span>
            <select
              className="review-create__select"
              value={selectedCollectionId ?? ''}
              onChange={(event) => onChangeCollection(event.target.value || null)}
              disabled={isSubmitting}
            >
              <option value="">(None)</option>
              {collectionOptions.map((opt) => (
                <option key={opt.id} value={opt.id}>
                  {opt.name} · {opt.size} ids
                </option>
              ))}
            </select>
          </label>
        ) : collectionName ? (
          <div className="review-create__field">
            <span className="review-create__label">Collection</span>
            <div className="review-create__pill">{collectionName}</div>
          </div>
        ) : null}

        <div className="review-create__two-up">
          <label className="review-create__field">
            <span className="review-create__label">Type</span>
            <select
              className="review-create__select"
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
          <label className="review-create__field">
            <span className="review-create__label">Due date</span>
            <input
              className="review-create__input"
              type="datetime-local"
              value={dueDate}
              onChange={(event) => setDueDate(event.target.value)}
              disabled={isSubmitting}
            />
          </label>
        </div>

        <label className="review-create__field">
          <span className="review-create__label">Notes (optional)</span>
          <textarea
            className="review-create__textarea"
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
            rows={4}
            maxLength={400}
            placeholder="Call out priorities, owners, or scope."
            disabled={isSubmitting}
          />
        </label>

        <div className="review-create__field">
          <div className="review-create__label-row">
            <span className="review-create__label">Screenshots (optional)</span>
            <span className="review-create__hint">Paste image keys or drop files.</span>
          </div>
          <div
            className="review-create__dropzone"
            onDragOver={(event) => {
              event.preventDefault();
            }}
            onDrop={(event) => {
              event.preventDefault();
              if (isSubmitting) return;
              handleFiles(event.dataTransfer.files);
            }}
          >
            <input
              ref={fileInputRef}
              type="file"
              multiple
              className="review-create__file-input"
              onChange={(event) => handleFiles(event.target.files)}
              disabled={isSubmitting}
            />
            <div className="review-create__dropzone-main">
              <button
                type="button"
                className="review-create__ghost"
                onClick={() => fileInputRef.current?.click()}
                disabled={isSubmitting}
              >
                Choose files
              </button>
              <div className="review-create__drop-hint">or drop files / paste keys below</div>
            </div>
            <input
              type="text"
              className="review-create__input review-create__key-input"
              placeholder="screenshot-key or comma/newline separated"
              value={screenshotDraft}
              onChange={(event) => setScreenshotDraft(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  event.preventDefault();
                  handleScreenshotDraftCommit();
                }
                if (event.key === 'Escape') {
                  setScreenshotDraft('');
                }
              }}
              onBlur={handleScreenshotDraftCommit}
              disabled={isSubmitting}
            />
          </div>
          {screenshotKeys.length ? (
            <div className="review-create__chips" aria-label="Screenshot keys">
              {screenshotKeys.map((key) => (
                <span key={key} className="review-create__chip">
                  <span className="review-create__chip-label">{key}</span>
                  <button
                    type="button"
                    className="review-create__chip-remove"
                    onClick={() => setScreenshotKeys((current) => current.filter((value) => value !== key))}
                    disabled={isSubmitting}
                    aria-label={`Remove ${key}`}
                  >
                    ×
                  </button>
                </span>
              ))}
            </div>
          ) : null}
        </div>
      </div>

      <div className="review-create__actions">
        {errorMessage ? <div className="review-create__error">{errorMessage}</div> : null}
        {onCancel ? (
          <button type="button" className="review-create__ghost" onClick={onCancel}>
            Cancel
          </button>
        ) : null}
        <button
          type="button"
          className="review-create__cta"
          onClick={() => {
            if (!canSubmit || isSubmitting) return;
            const dueIso = new Date(dueDate).toISOString();
            const maxTextUnits = tmTextUnitIds.length || collectionSize || null;
            onSubmit({
              name: name.trim(),
              dueDate: dueIso,
              type,
              localeTags: localeTags.length ? localeTags : ['en'],
              maxTextUnits,
              notes: notes.trim() || null,
              tmTextUnitIds,
              screenshotImageIds: screenshotKeys,
            });
          }}
          disabled={!canSubmit || isSubmitting}
        >
          {isSubmitting ? (
            <>
              <span className="spinner" aria-hidden="true" /> {submitLabel}…
            </>
          ) : (
            submitLabel
          )}
        </button>
      </div>
    </div>
  );
}
