import { useEffect, useMemo, useState } from 'react';

import type { TextUnitSearchRequest } from '../../api/text-units';
import { Modal } from '../../components/Modal';
import { serializeSearchRequest } from './workbench-helpers';
import {
  buildWorkbenchSharePayload,
  buildWorkbenchShareUrl,
  saveWorkbenchShare,
  type WorkbenchShareLocaleSelectionType,
  type WorkbenchShareMode,
} from './workbench-share';
import type { WorkbenchRow } from './workbench-types';

type WorkbenchShareModalProps = {
  open: boolean;
  onClose: () => void;
  searchRequest: TextUnitSearchRequest | null;
  rows: WorkbenchRow[];
  availableLocales: string[];
};

type ShareModeOption = {
  value: WorkbenchShareMode;
  label: string;
  description: string;
  caution?: string;
};

const SHARE_MODE_OPTIONS: ShareModeOption[] = [
  {
    value: 'search',
    label: 'Search only',
    description: 'Re-run the search with current filters; results may change over time.',
  },
  {
    value: 'search-ids',
    label: 'Search + pinned text unit ids',
    description:
      'Re-run the search using the pinned text unit ids as the search attribute; only the ids are fixed, translations/status can still change over time.',
  },
];

export function WorkbenchShareModal({
  open,
  onClose,
  searchRequest,
  rows,
  availableLocales,
}: WorkbenchShareModalProps) {
  const [mode, setMode] = useState<WorkbenchShareMode>('search-ids');
  const [selectedLocaleChoice, setSelectedLocaleChoice] = useState<'use-search' | 'ask'>(
    'use-search',
  );
  const [shareLink, setShareLink] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [copyStatus, setCopyStatus] = useState<'idle' | 'copied' | 'failed'>('idle');

  const resetShareLink = () => {
    setShareLink(null);
    setCopyStatus('idle');
    setError(null);
  };

  const pinnedCount = useMemo(() => {
    const ids = new Set(rows.map((row) => row.tmTextUnitId));
    return ids.size;
  }, [rows]);

  useEffect(() => {
    if (!open) {
      return;
    }
    setMode(pinnedCount > 0 ? 'search-ids' : 'search');
    setSelectedLocaleChoice('use-search');
    resetShareLink();
  }, [open, pinnedCount]);

  const shareContextSignature = useMemo(() => {
    const requestSignature = searchRequest ? serializeSearchRequest(searchRequest) : 'null';
    const pinnedSignature = rows
      .map((row) => row.tmTextUnitId)
      .sort((first, second) => first - second)
      .join(',');
    return `${requestSignature}|${pinnedSignature}`;
  }, [rows, searchRequest]);

  useEffect(() => {
    if (!open) {
      return;
    }
    resetShareLink();
  }, [open, shareContextSignature]);

  const hasSelectedLocales = availableLocales.length > 0;
  const canShare =
    Boolean(searchRequest) && !isSaving && (mode !== 'search-ids' || pinnedCount > 0);
  const pinnedCountLabel = `${pinnedCount} ${pinnedCount === 1 ? 'id' : 'ids'}`;

  const handleGenerate = async (): Promise<string | null> => {
    if (!searchRequest) {
      setError('Run a search before sharing.');
      return null;
    }

    setIsSaving(true);
    setError(null);
    setCopyStatus('idle');

    try {
      const payload = buildWorkbenchSharePayload({
        mode,
        searchRequest,
        rows,
        localeFocus: resolveLocaleSelectionType(selectedLocaleChoice),
      });
      const shareId = await saveWorkbenchShare(payload);
      const url = buildWorkbenchShareUrl(shareId);
      setShareLink(url);
      setCopyStatus('idle');
      return url;
    } catch (unknownError: unknown) {
      const message =
        unknownError instanceof Error ? unknownError.message : 'Failed to generate share link.';
      setError(message);
      return null;
    } finally {
      setIsSaving(false);
    }
  };

  const handleCopyLink = async () => {
    if (isSaving) {
      return;
    }
    let nextLink = shareLink;
    if (!nextLink) {
      nextLink = await handleGenerate();
    }
    if (!nextLink) {
      return;
    }
    try {
      await navigator.clipboard.writeText(nextLink);
      setCopyStatus('copied');
    } catch {
      setCopyStatus('failed');
    }
  };

  return (
    <Modal open={open} size="lg" ariaLabel="Share workbench" onClose={onClose} closeOnBackdrop>
      <div className="modal__header">
        <div className="modal__title">Share link</div>
      </div>
      <div className="modal__body">
        <div className="workbench-share__section">
          <div className="workbench-share__label workbench-share__label--muted">Type</div>
          <div className="workbench-share__options">
            {SHARE_MODE_OPTIONS.map((option) => {
              const description =
                option.value === 'search-ids'
                  ? `${option.description} (${pinnedCountLabel})`
                  : option.description;
              return (
                <label key={option.value} className="workbench-share__option">
                  <input
                    type="radio"
                    name="workbench-share-mode"
                    value={option.value}
                    checked={mode === option.value}
                    disabled={option.value === 'search-ids' && pinnedCount === 0}
                    onChange={() => {
                      setMode(option.value);
                      resetShareLink();
                    }}
                  />
                  <div>
                    <div className="workbench-share__option-title">{option.label}</div>
                    <div className="workbench-share__option-description">{description}</div>
                    {option.caution ? (
                      <div className="workbench-share__option-caution">{option.caution}</div>
                    ) : null}
                  </div>
                </label>
              );
            })}
          </div>
        </div>

        <div className="workbench-share__section">
          <div className="workbench-share__label workbench-share__label--muted">Locale</div>
          <div className="workbench-share__options">
            <label className="workbench-share__option">
              <input
                type="radio"
                name="workbench-share-locale"
                value="use-search"
                checked={selectedLocaleChoice === 'use-search'}
                onChange={() => {
                  setSelectedLocaleChoice('use-search');
                  resetShareLink();
                }}
              />
              <div>
                <div className="workbench-share__option-title">Keep current selection</div>
                <div className="workbench-share__option-description">
                  {hasSelectedLocales
                    ? 'Shares with the locales you have selected now.'
                    : 'No locales selected yet; pick them before sharing.'}
                </div>
              </div>
            </label>
            <label className="workbench-share__option">
              <input
                type="radio"
                name="workbench-share-locale"
                value="ask"
                checked={selectedLocaleChoice === 'ask'}
                onChange={() => {
                  setSelectedLocaleChoice('ask');
                  resetShareLink();
                }}
              />
              <div>
                <div className="workbench-share__option-title">Ask on open</div>
                <div className="workbench-share__option-description">
                  Recipient will be prompted in a modal to choose a locale when they open the link.
                </div>
              </div>
            </label>
          </div>
        </div>

        <div className="workbench-share__section">
          <div
            className={`workbench-share__label workbench-share__label--muted${
              shareLink ? '' : ' workbench-share__label--hidden'
            }`}
          >
            Link
          </div>
          <div className="workbench-share__link">
            {shareLink ? (
              <div className="workbench-share__link-chip" title={shareLink}>
                {shareLink}
              </div>
            ) : (
              <div
                className="workbench-share__link-chip workbench-share__link-chip--placeholder"
                aria-hidden="true"
              >
                Placeholder share link
              </div>
            )}
          </div>
        </div>

        {error ? <div className="workbench-share__error">{error}</div> : null}
      </div>
      <div className="modal__actions">
        <div className="workbench-share__status" aria-live="polite">
          {copyStatus === 'failed' ? 'Copy blocked? Select and copy manually.' : null}
          {copyStatus === 'copied' ? (
            <span className="workbench-share__hint--success">Copied</span>
          ) : null}
        </div>
        <button
          type="button"
          className="modal__button modal__button--primary"
          onClick={() => {
            void handleGenerate();
          }}
          disabled={!canShare || isSaving}
        >
          {isSaving ? 'Generating…' : 'Generate link'}
        </button>
        <button
          type="button"
          className="modal__button"
          onClick={() => {
            void handleCopyLink();
          }}
          disabled={!canShare || isSaving}
        >
          Copy link
        </button>
        <button type="button" className="modal__button" onClick={onClose}>
          Close
        </button>
      </div>
    </Modal>
  );
}

function resolveLocaleSelectionType(choice: string): WorkbenchShareLocaleSelectionType {
  return choice === 'ask' ? 'ASK_RECIPIENT' : 'USE_SEARCH';
}
