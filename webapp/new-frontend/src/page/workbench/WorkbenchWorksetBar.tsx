import { type ReactNode, useCallback, useEffect, useRef, useState } from 'react';

import { resultSizePresets } from './workbench-constants';

type WorkbenchWorksetBarProps = {
  disabled: boolean;
  isEditMode: boolean;
  isSearchLoading: boolean;
  rowCount: number;
  worksetSize: number;
  onChangeWorksetSize: (value: number) => void;
  editedCount: number;
  onBackToSearch: () => void;
  onRefreshWorkset: () => void;
  onOpenShareModal: () => void;
};

export function WorkbenchWorksetBar({
  disabled,
  isEditMode,
  isSearchLoading,
  rowCount,
  worksetSize,
  onChangeWorksetSize,
  editedCount,
  onBackToSearch,
  onRefreshWorkset,
  onOpenShareModal,
}: WorkbenchWorksetBarProps) {
  const countLabel = `${rowCount} results`;

  const parts: ReactNode[] = [
    <ResultSizeDropdown
      key="count"
      disabled={disabled || isEditMode}
      countLabel={countLabel}
      worksetSize={worksetSize}
      onChangeWorksetSize={onChangeWorksetSize}
    />,
  ];

  if (editedCount > 0) {
    parts.push(
      <span key="edited" className="workbench-worksetbar__meta">
        Edited: {editedCount}
      </span>,
    );
  }

  parts.push(
    isEditMode ? (
      <button
        key="editsearch"
        type="button"
        className="workbench-worksetbar__button"
        onClick={onBackToSearch}
        disabled={disabled}
      >
        Back to search
      </button>
    ) : (
      <button
        key="refresh"
        type="button"
        className="workbench-worksetbar__button workbench-worksetbar__button--refresh"
        onClick={() => {
          if (!isSearchLoading) {
            onRefreshWorkset();
          }
        }}
        disabled={disabled || isSearchLoading}
      >
        Refresh
      </button>
    ),
  );

  if (rowCount > 0) {
    parts.push(<SearchShareButton key="share" onClick={onOpenShareModal} disabled={disabled} />);
  }

  const content = parts.flatMap((part, index) =>
    index === 0
      ? [part]
      : [
          <span key={`sep-${index}`} className="workbench-worksetbar__sep" aria-hidden="true">
            ·
          </span>,
          part,
        ],
  );

  return (
    <div className="workbench-worksetbar">
      <div className="workbench-worksetbar__cluster">{content}</div>
    </div>
  );
}

function ResultSizeDropdown({
  disabled,
  countLabel,
  worksetSize,
  onChangeWorksetSize,
}: {
  disabled: boolean;
  countLabel: string;
  worksetSize: number;
  onChangeWorksetSize: (value: number) => void;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);
  const [draft, setDraft] = useState(String(worksetSize));
  const [showCustomInput, setShowCustomInput] = useState(false);

  const isPreset = resultSizePresets.some((option) => option.value === worksetSize);
  const isCustomActive = showCustomInput || !isPreset;

  const commitDraft = useCallback(() => {
    const trimmed = draft.trim();
    if (!trimmed) {
      setDraft(String(worksetSize));
      return;
    }
    const next = parseInt(trimmed, 10);
    if (Number.isNaN(next) || next < 1) {
      setDraft(String(worksetSize));
      return;
    }
    if (next === worksetSize) {
      return;
    }
    onChangeWorksetSize(next);
  }, [draft, onChangeWorksetSize, worksetSize]);

  useEffect(() => {
    if (disabled && isOpen) {
      if (isCustomActive) {
        commitDraft();
      }
      setIsOpen(false);
    }
  }, [commitDraft, disabled, isCustomActive, isOpen]);

  useEffect(() => {
    if (!isOpen) {
      return;
    }
    setDraft(String(worksetSize));
    setShowCustomInput(false);
  }, [isOpen, worksetSize]);

  useEffect(() => {
    if (!isOpen) {
      return;
    }
    const handlePointerDown = (event: PointerEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) {
        if (isCustomActive) {
          commitDraft();
        }
        setIsOpen(false);
      }
    };
    window.addEventListener('pointerdown', handlePointerDown);
    return () => window.removeEventListener('pointerdown', handlePointerDown);
  }, [commitDraft, isCustomActive, isOpen]);

  return (
    <div className="chip-dropdown workbench-worksetbar__count" ref={containerRef}>
      <button
        type="button"
        className="workbench-worksetbar__countbutton"
        onClick={() => setIsOpen((previous) => !previous)}
        aria-expanded={isOpen}
        disabled={disabled}
      >
        <span>{countLabel}</span>
        <span className="chip-dropdown__chevron" aria-hidden="true" />
      </button>
      {isOpen ? (
        <div className="chip-dropdown__panel workbench-worksetbar__panel" role="menu">
          <div className="workbench-searchmode__section">
            <div className="workbench-searchmode__label">Result size</div>
            <div className="workbench-filterchip__pills workbench-worksetbar__pills">
              {resultSizePresets.map((option) => (
                <button
                  type="button"
                  key={option.value}
                  className={`workbench-datefilter__quick-chip${
                    option.value === worksetSize ? ' is-active' : ''
                  }`}
                  onClick={() => onChangeWorksetSize(option.value)}
                >
                  {option.label}
                </button>
              ))}
              {isCustomActive ? (
                <div className="workbench-worksetcustom is-active">
                  <span className="workbench-worksetcustom__label">Custom</span>
                  <input
                    ref={inputRef}
                    className="workbench-worksetcustom__input"
                    type="number"
                    inputMode="numeric"
                    min={1}
                    value={draft}
                    onChange={(event) => setDraft(event.target.value)}
                    onBlur={() => {
                      commitDraft();
                    }}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter') {
                        commitDraft();
                      }
                      if (event.key === 'Escape') {
                        setDraft(String(worksetSize));
                        setShowCustomInput(false);
                      }
                    }}
                  />
                </div>
              ) : (
                <button
                  type="button"
                  className="workbench-datefilter__quick-chip"
                  onClick={() => {
                    setShowCustomInput(true);
                    setDraft(String(worksetSize));
                    queueMicrotask(() => {
                      inputRef.current?.focus();
                      inputRef.current?.select();
                    });
                  }}
                >
                  Custom…
                </button>
              )}
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function SearchShareButton({ onClick, disabled }: { onClick: () => void; disabled: boolean }) {
  return (
    <button type="button" className="workbench-share-button" onClick={onClick} disabled={disabled}>
      Share
    </button>
  );
}
