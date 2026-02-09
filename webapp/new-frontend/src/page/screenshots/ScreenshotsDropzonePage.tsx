import './screenshots-page.css';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import { Modal } from '../../components/Modal';

type UploadStatus = 'uploading' | 'done' | 'error';

type UploadItem = {
  id: string;
  fileName: string;
  previewUrl: string | null;
  status: UploadStatus;
  uuid?: string;
  error?: string;
};

const SUPPORTED_EXTENSIONS = ['png', 'jpg', 'jpeg'];

const createUuid = () => {
  if (typeof crypto !== 'undefined') {
    if ('randomUUID' in crypto && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
    if ('getRandomValues' in crypto) {
      const bytes = new Uint8Array(16);
      crypto.getRandomValues(bytes);
      bytes[6] = (bytes[6] & 0x0f) | 0x40;
      bytes[8] = (bytes[8] & 0x3f) | 0x80;
      const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0'));
      return `${hex[0]}${hex[1]}${hex[2]}${hex[3]}-${hex[4]}${hex[5]}-${hex[6]}${hex[7]}-${hex[8]}${hex[9]}-${hex[10]}${hex[11]}${hex[12]}${hex[13]}${hex[14]}${hex[15]}`;
    }
  }
  const hex = (length: number) =>
    Array.from({ length }, () => Math.floor(Math.random() * 16).toString(16)).join('');
  return `${hex(8)}-${hex(4)}-${hex(4)}-${hex(4)}-${hex(12)}`;
};

const isSupportedImage = (file: File) => {
  if (file.type) {
    return file.type === 'image/png' || file.type === 'image/jpeg';
  }
  const lower = file.name.toLowerCase();
  return SUPPORTED_EXTENSIONS.some((ext) => lower.endsWith(`.${ext}`));
};

const createItemId = () => `${Date.now()}-${Math.random().toString(16).slice(2)}`;

async function uploadImage(uuid: string, file: File, signal?: AbortSignal) {
  const buffer = await file.arrayBuffer();
  const response = await fetch(`/api/images/${encodeURIComponent(uuid)}`, {
    method: 'PUT',
    credentials: 'include',
    body: buffer,
    signal,
  });

  if (!response.ok) {
    const message = await response.text().catch(() => '');
    throw new Error(message || 'Upload failed');
  }
}

export function ScreenshotsDropzonePage() {
  const [items, setItems] = useState<UploadItem[]>([]);
  const [dragging, setDragging] = useState(false);
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [zoomItem, setZoomItem] = useState<{ src: string; name: string } | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const dragCounter = useRef(0);
  const uploadControllers = useRef<Map<string, AbortController>>(new Map());
  const copyTimeoutRef = useRef<number | null>(null);
  const itemsRef = useRef<UploadItem[]>([]);

  const anyUploading = useMemo(() => items.some((item) => item.status === 'uploading'), [items]);

  const updateItem = useCallback((id: string, patch: Partial<UploadItem>) => {
    setItems((prev) => {
      let found = false;
      const next = prev.map((item) => {
        if (item.id !== id) return item;
        found = true;
        return { ...item, ...patch };
      });
      return found ? next : prev;
    });
  }, []);

  const revokePreview = useCallback((previewUrl: string | null) => {
    if (!previewUrl) return;
    URL.revokeObjectURL(previewUrl);
  }, []);

  const removeItem = useCallback(
    (id: string) => {
      setItems((prev) => {
        const item = prev.find((entry) => entry.id === id);
        if (item?.previewUrl) {
          revokePreview(item.previewUrl);
        }
        return prev.filter((entry) => entry.id !== id);
      });
      const controller = uploadControllers.current.get(id);
      if (controller) {
        controller.abort();
        uploadControllers.current.delete(id);
      }
      setCopiedId((current) => (current === id ? null : current));
    },
    [revokePreview],
  );

  const handleCopy = useCallback(async (id: string, uuid: string) => {
    try {
      await navigator.clipboard.writeText(`s:${uuid}`);
      setCopiedId(id);
      if (copyTimeoutRef.current) {
        window.clearTimeout(copyTimeoutRef.current);
      }
      copyTimeoutRef.current = window.setTimeout(() => {
        setCopiedId((current) => (current === id ? null : current));
      }, 900);
    } catch {
      // ignore clipboard errors
    }
  }, []);

  const handleFiles = useCallback(
    (files: FileList | null) => {
      if (!files || files.length === 0) return;
      const fileArr = Array.from(files);
      fileArr.forEach((file) => {
        const supported = isSupportedImage(file);
        const previewUrl = supported ? URL.createObjectURL(file) : null;
        const id = createItemId();

        if (!supported) {
          setItems((prev) => [
            {
              id,
              fileName: file.name,
              previewUrl: null,
              status: 'error',
              error: 'Unsupported file type. Use PNG or JPG.',
            },
            ...prev,
          ]);
          return;
        }

        const uuid = createUuid();
        setItems((prev) => [
          {
            id,
            fileName: file.name,
            previewUrl,
            status: 'uploading',
            uuid,
          },
          ...prev,
        ]);

        const controller = new AbortController();
        uploadControllers.current.set(id, controller);

        uploadImage(uuid, file, controller.signal)
          .then(() => {
            updateItem(id, { status: 'done' });
          })
          .catch((error: unknown) => {
            if (controller.signal.aborted) return;
            const message = error instanceof Error ? error.message : 'Upload failed';
            updateItem(id, { status: 'error', error: message });
          })
          .finally(() => {
            uploadControllers.current.delete(id);
          });
      });

      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    },
    [updateItem],
  );

  useEffect(() => {
    itemsRef.current = items;
  }, [items]);

  useEffect(() => {
    const controllers = uploadControllers.current;
    return () => {
      itemsRef.current.forEach((item) => revokePreview(item.previewUrl));
      controllers.forEach((controller) => controller.abort());
      if (copyTimeoutRef.current) {
        window.clearTimeout(copyTimeoutRef.current);
      }
    };
  }, [revokePreview]);

  return (
    <div className="page-wrapper screenshots-page">
      <div className="screenshots-page__header">
        <div>
          <h1 className="screenshots-page__title">Screenshot uploads</h1>
          <p className="screenshots-page__subtitle">
            Upload screenshots to generate IDs you can paste into text unit comments.
          </p>
        </div>
        {items.length ? (
          <button
            type="button"
            className="screenshots-page__button screenshots-page__button--ghost"
            onClick={() => {
              items.forEach((item) => revokePreview(item.previewUrl));
              uploadControllers.current.forEach((controller) => controller.abort());
              uploadControllers.current.clear();
              setItems([]);
              setCopiedId(null);
            }}
          >
            Clear all
          </button>
        ) : null}
      </div>

      <div
        className={`screenshots-page__card screenshots-dropzone${dragging ? ' is-dragging' : ''}`}
        onDragEnter={(event) => {
          event.preventDefault();
          dragCounter.current += 1;
          setDragging(true);
        }}
        onDragOver={(event) => {
          event.preventDefault();
        }}
        onDragLeave={(event) => {
          event.preventDefault();
          dragCounter.current = Math.max(0, dragCounter.current - 1);
          if (dragCounter.current === 0) {
            setDragging(false);
          }
        }}
        onDrop={(event) => {
          event.preventDefault();
          dragCounter.current = 0;
          setDragging(false);
          handleFiles(event.dataTransfer.files);
        }}
        onClick={() => fileInputRef.current?.click()}
        onKeyDown={(event) => {
          if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            fileInputRef.current?.click();
          }
        }}
        role="button"
        tabIndex={0}
        aria-label="Upload screenshots"
      >
        <div className="screenshots-dropzone__icon">+</div>
        <div className="screenshots-dropzone__title">
          {anyUploading ? 'Uploading screenshots...' : 'Drag screenshots here'}
        </div>
        <div className="screenshots-dropzone__hint">PNG or JPG. You can drop multiple files.</div>
        <button
          type="button"
          className="screenshots-page__button screenshots-page__button--primary"
          onClick={(event) => {
            event.stopPropagation();
            fileInputRef.current?.click();
          }}
        >
          Choose files
        </button>
        <input
          ref={fileInputRef}
          type="file"
          className="screenshots-dropzone__file-input"
          accept="image/png,image/jpeg"
          multiple
          onChange={(event) => {
            handleFiles(event.target.files);
          }}
        />
      </div>

      {items.length ? (
        <div className="screenshots-queue">
          <div className="screenshots-queue__header">
            <h2 className="screenshots-queue__title">Uploads</h2>
            <span className="screenshots-queue__count">{items.length} item(s)</span>
          </div>
          <div className="screenshots-grid" aria-live="polite">
            {items.map((item) => {
              const statusClass = `screenshots-item__status${
                item.status === 'done' ? ' is-done' : item.status === 'error' ? ' is-error' : ''
              }`;
              const statusNode =
                item.status === 'uploading' ? (
                  <>
                    <span className="spinner" aria-hidden="true" /> Uploading
                  </>
                ) : item.status === 'done' ? (
                  'Ready'
                ) : (
                  'Error'
                );

              return (
                <div key={item.id} className="screenshots-item">
                  <div className="screenshots-item__preview">
                    {item.previewUrl ? (
                      <img
                        src={item.previewUrl}
                        alt={item.fileName}
                        onClick={(event) => {
                          event.stopPropagation();
                          setZoomItem({ src: item.previewUrl || '', name: item.fileName });
                        }}
                      />
                    ) : (
                      <span className="screenshots-item__placeholder" aria-hidden="true" />
                    )}
                  </div>
                  <div className="screenshots-item__meta">
                    <div className="screenshots-item__name">{item.fileName}</div>
                    {item.status === 'done' && item.uuid ? (
                      <div className="screenshots-item__uuid">
                        <code>{`s:${item.uuid}`}</code>
                        <button
                          type="button"
                          className="screenshots-page__button screenshots-page__button--primary"
                          onClick={() => {
                            void handleCopy(item.id, item.uuid!);
                          }}
                        >
                          Copy
                        </button>
                        <span className={statusClass}>{statusNode}</span>
                        <button
                          type="button"
                          className="screenshots-item__remove"
                          onClick={() => removeItem(item.id)}
                          aria-label={`Clear ${item.fileName}`}
                        >
                          Clear
                        </button>
                        {copiedId === item.id ? (
                          <span className="screenshots-item__copied">Copied</span>
                        ) : null}
                      </div>
                    ) : null}
                    {item.error ? (
                      <div className="screenshots-item__error">{item.error}</div>
                    ) : null}
                  </div>
                  {item.status !== 'done' ? (
                    <div className="screenshots-item__actions">
                      <span className={statusClass}>{statusNode}</span>
                      <button
                        type="button"
                        className="screenshots-item__remove"
                        onClick={() => removeItem(item.id)}
                        aria-label={`Clear ${item.fileName}`}
                      >
                        Clear
                      </button>
                    </div>
                  ) : null}
                </div>
              );
            })}
          </div>
        </div>
      ) : null}

      <Modal
        open={Boolean(zoomItem)}
        onClose={() => setZoomItem(null)}
        closeOnBackdrop
        size="xl"
        className="screenshots-modal"
      >
        <div className="screenshots-modal__header">
          <div>
            <h2 className="screenshots-modal__title">Preview</h2>
            {zoomItem ? <div className="screenshots-page__subtitle">{zoomItem.name}</div> : null}
          </div>
          <button
            type="button"
            className="screenshots-page__button screenshots-page__button--ghost"
            onClick={() => setZoomItem(null)}
          >
            Close
          </button>
        </div>
        {zoomItem ? (
          <img src={zoomItem.src} alt={zoomItem.name} className="screenshots-modal__image" />
        ) : null}
      </Modal>
    </div>
  );
}
