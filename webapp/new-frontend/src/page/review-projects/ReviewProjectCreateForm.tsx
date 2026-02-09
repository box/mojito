import './review-projects-page.css';

import { useEffect, useMemo, useRef, useState } from 'react';

import {
  type ApiReviewProjectType,
  REVIEW_PROJECT_TYPE_LABELS,
  REVIEW_PROJECT_TYPES,
} from '../../api/review-projects';
import { type CollectionOption, CollectionSelect } from '../../components/CollectionSelect';
import { LocaleMultiSelect } from '../../components/LocaleMultiSelect';
import type { LocaleSelectionOption } from '../../utils/localeSelection';

export type ReviewProjectCreateFormValues = {
  name: string;
  dueDate: string;
  type: ApiReviewProjectType;
  localeTags: string[];
  notes: string | null;
  tmTextUnitIds: number[];
  screenshotImageIds: string[];
};

type UploadQueueItem = {
  key: string;
  name: string;
  status: 'uploading' | 'done' | 'error';
  kind: 'image' | 'video';
  preview?: string | null;
  error?: string;
};

type Props = {
  defaultName: string;
  defaultDueDate: string;
  localeOptions: LocaleSelectionOption[];
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
  const [selectedLocaleTags, setSelectedLocaleTags] = useState<string[]>([]);
  const [notes, setNotes] = useState('');
  const [screenshotKeys, setScreenshotKeys] = useState<string[]>([]);
  const [uploadQueue, setUploadQueue] = useState<UploadQueueItem[]>([]);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => setName(defaultName), [defaultName]);
  useEffect(() => setDueDate(defaultDueDate), [defaultDueDate]);
  useEffect(() => {
    setSelectedLocaleTags((current) => {
      const allowed = new Set(localeOptions.map((opt) => opt.tag));
      return current.filter((tag) => allowed.has(tag));
    });
  }, [localeOptions]);

  const canSubmit = useMemo(
    () =>
      Boolean(name.trim()) &&
      Boolean(dueDate) &&
      tmTextUnitIds.length > 0 &&
      selectedLocaleTags.length > 0 &&
      uploadQueue.every((item) => item.status !== 'uploading'),
    [dueDate, name, selectedLocaleTags.length, tmTextUnitIds.length, uploadQueue],
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

  const uploadImage = async (file: File): Promise<string> => {
    const ext = file.name.includes('.') ? (file.name.split('.').pop() ?? '') : '';
    const suffix = ext ? `.${ext.toLowerCase()}` : '';
    const key =
      (typeof crypto !== 'undefined' && 'randomUUID' in crypto && crypto.randomUUID
        ? crypto.randomUUID()
        : Math.random().toString(36).slice(2)) + suffix;
    const buffer = await file.arrayBuffer();
    const response = await fetch(`/api/images/${encodeURIComponent(key)}`, {
      method: 'PUT',
      credentials: 'include',
      body: buffer,
    });
    if (!response.ok) {
      const msg = await response.text().catch(() => '');
      throw new Error(msg || 'Upload failed');
    }
    return key;
  };

  const VIDEO_EXTENSIONS = ['mp4', 'mov', 'webm', 'm4v', 'ogv', 'ogg', 'mkv'];

  const isVideoFile = (file: File) => {
    if (file.type.startsWith('video/')) return true;
    const lower = file.name.toLowerCase();
    return VIDEO_EXTENSIONS.some((ext) => lower.endsWith(`.${ext}`));
  };

  const isSupportedFile = (file: File) =>
    file.type.startsWith('image/') ||
    file.type.startsWith('video/') ||
    /\.(png|jpe?g|gif|webp|bmp|svg|mp4|mov|webm|m4v|ogv|ogg|mkv)$/i.test(file.name);

  const handleFiles = async (files: FileList | null) => {
    if (!files || files.length === 0) return;
    const fileArr = Array.from(files);
    const queueEntries: UploadQueueItem[] = fileArr.map((file) => ({
      key: `${file.name}-${Date.now()}-${Math.random().toString(36).slice(2)}`,
      name: file.name,
      status: isSupportedFile(file) ? ('uploading' as const) : ('error' as const),
      kind: isVideoFile(file) ? 'video' : 'image',
      preview: URL.createObjectURL(file),
      error: isSupportedFile(file) ? undefined : 'Unsupported file type',
    }));
    setUploadQueue((prev) => [...queueEntries, ...prev]);
    if (fileInputRef.current) fileInputRef.current.value = '';
    await Promise.all(
      queueEntries.map(async (entry, index) => {
        const file = fileArr[index];
        if (!isSupportedFile(file)) {
          return;
        }
        try {
          const uploadedKey = await uploadImage(file);
          setUploadQueue((prev) =>
            prev.map((item) => (item.key === entry.key ? { ...item, status: 'done' } : item)),
          );
          addScreenshotKeys([uploadedKey]);
        } catch (error) {
          const message =
            error instanceof Error ? error.message : 'Upload failed. Please try again.';
          setUploadQueue((prev) =>
            prev.map((item) =>
              item.key === entry.key ? { ...item, status: 'error', error: message } : item,
            ),
          );
        }
      }),
    );
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
            <CollectionSelect
              options={collectionOptions}
              value={selectedCollectionId ?? null}
              onChange={onChangeCollection}
              disabled={isSubmitting}
              className="review-create__select"
            />
          </label>
        ) : collectionName ? (
          <div className="review-create__field">
            <span className="review-create__label">Collection</span>
            <div className="review-create__pill">{collectionName}</div>
          </div>
        ) : null}

        <div className="review-create__field">
          <span className="review-create__label">Locales</span>
          <LocaleMultiSelect
            options={localeOptions.map((opt) => ({ tag: opt.tag, label: opt.label }))}
            selectedTags={selectedLocaleTags}
            onChange={setSelectedLocaleTags}
            className="review-create__locale-select"
            align="left"
            disabled={isSubmitting}
          />
        </div>

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
            placeholder="Describe the feature, glossary guidance, etc"
            disabled={isSubmitting}
          />
        </label>

        <div className="review-create__field">
          <div className="review-create__label-row">
            <span className="review-create__label">Screenshots (optional)</span>
            <span className="review-create__hint">Drop images/videos to upload.</span>
          </div>
          <div
            className="review-create__dropzone"
            onDragOver={(event) => {
              event.preventDefault();
            }}
            onDrop={(event) => {
              event.preventDefault();
              if (isSubmitting) return;
              void handleFiles(event.dataTransfer.files);
            }}
          >
            <input
              ref={fileInputRef}
              type="file"
              multiple
              className="review-create__file-input"
              accept="image/*,video/*"
              onChange={(event) => {
                void handleFiles(event.target.files);
              }}
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
              <div className="review-create__drop-hint">or drop files here</div>
            </div>
          </div>
          {screenshotKeys.length ? (
            <div className="review-create__chips" aria-label="Screenshot keys">
              {screenshotKeys.map((key) => (
                <span key={key} className="review-create__chip">
                  <span className="review-create__chip-label">{key}</span>
                  <button
                    type="button"
                    className="review-create__chip-remove"
                    onClick={() =>
                      setScreenshotKeys((current) => current.filter((value) => value !== key))
                    }
                    disabled={isSubmitting}
                    aria-label={`Remove ${key}`}
                  >
                    ×
                  </button>
                </span>
              ))}
            </div>
          ) : null}
          {uploadQueue.length ? (
            <div className="review-create__upload-list" aria-label="Upload status">
              {uploadQueue.map((item) => (
                <div key={item.key} className="review-create__upload-row">
                  {item.preview ? (
                    item.kind === 'video' ? (
                      <video
                        src={item.preview}
                        className="review-create__upload-thumb review-create__upload-thumb--video"
                        muted
                        loop
                        playsInline
                        preload="metadata"
                      />
                    ) : (
                      <img
                        src={item.preview}
                        alt=""
                        className="review-create__upload-thumb"
                        loading="lazy"
                      />
                    )
                  ) : (
                    <span className="review-create__upload-thumb placeholder" />
                  )}
                  <span className="review-create__upload-name">{item.name}</span>
                  <span className={`review-create__upload-status status-${item.status}`}>
                    {item.status === 'uploading'
                      ? 'Uploading…'
                      : item.status === 'done'
                        ? 'Ready'
                        : item.error || 'Failed'}
                  </span>
                </div>
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
            onSubmit({
              name: name.trim(),
              dueDate: dueIso,
              type,
              localeTags: selectedLocaleTags,
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
