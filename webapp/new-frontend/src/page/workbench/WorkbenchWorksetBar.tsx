import { type ReactNode, useCallback, useEffect, useRef, useState } from 'react';

import { resultSizePresets } from './workbench-constants';
import type { WorkbenchCollection } from './workbench-types';

type WorkbenchWorksetBarProps = {
  disabled: boolean;
  isSearchLoading: boolean;
  hasSearched: boolean;
  rowCount: number;
  hasMoreResults: boolean;
  worksetSize: number;
  onChangeWorksetSize: (value: number) => void;
  editedCount: number;
  onRefreshWorkset: () => void;
  onResetWorkbench: () => void;
  onOpenShareModal: () => void;
  collections: WorkbenchCollection[];
  activeCollectionId: string | null;
  activeCollectionName: string | null;
  activeCollectionCount: number;
  onCreateCollection: (name?: string) => string | null;
  onSelectCollection: (id: string | null) => void;
  onRenameCollection: (id: string, name: string) => void;
  onDeleteCollection: (id: string) => void;
  onClearCollection: () => void;
  onDeleteAllCollections: () => void;
  onAddAllToCollection: () => void;
  onOpenCollectionSearch: (id: string) => void;
  onShareCollection: (id: string) => boolean;
  onCreateReviewProject: (id: string) => void;
  onOpenAiTranslate: (id: string) => void;
};

export function WorkbenchWorksetBar({
  disabled,
  isSearchLoading,
  hasSearched,
  rowCount,
  hasMoreResults,
  worksetSize,
  onChangeWorksetSize,
  editedCount,
  onRefreshWorkset,
  onResetWorkbench,
  onOpenShareModal,
  collections,
  activeCollectionId,
  activeCollectionName,
  activeCollectionCount,
  onCreateCollection,
  onSelectCollection,
  onRenameCollection,
  onDeleteCollection,
  onClearCollection,
  onDeleteAllCollections,
  onAddAllToCollection,
  onOpenCollectionSearch,
  onShareCollection,
  onCreateReviewProject,
  onOpenAiTranslate,
}: WorkbenchWorksetBarProps) {
  const countLabel = hasMoreResults
    ? `Showing first ${worksetSize} results (more available)`
    : rowCount === 0
      ? 'No results'
      : `${rowCount} results`;
  const resultDropdownDisabled = disabled;
  const canRefresh = hasSearched && !disabled && !isSearchLoading;
  const canShare = hasSearched && !disabled;

  const parts: ReactNode[] = hasSearched
    ? [
        <ResultSizeDropdown
          key="count"
          disabled={resultDropdownDisabled}
          countLabel={countLabel}
          worksetSize={worksetSize}
          onChangeWorksetSize={onChangeWorksetSize}
        />,
      ]
    : [];

  if (editedCount > 0) {
    parts.push(
      <span key="edited" className="workbench-worksetbar__meta">
        Edited: {editedCount}
      </span>,
    );
  }

  if (hasSearched) {
    parts.push(
      <button
        key="refresh"
        type="button"
        className="workbench-worksetbar__button workbench-worksetbar__button--refresh"
        onClick={() => {
          if (canRefresh) {
            onRefreshWorkset();
          }
        }}
        disabled={!canRefresh}
      >
        Refresh
      </button>,
    );
    parts.push(
      <button
        key="reset"
        type="button"
        className="workbench-worksetbar__button"
        onClick={() => {
          onResetWorkbench();
        }}
        disabled={disabled}
      >
        Reset
      </button>,
    );
  }

  parts.push(
    <CollectionDropdown
      key="collections"
      disabled={disabled}
      isSearchLoading={isSearchLoading}
      hasSearched={hasSearched}
      rowCount={rowCount}
      collections={collections}
      activeCollectionId={activeCollectionId}
      activeCollectionName={activeCollectionName}
      activeCollectionCount={activeCollectionCount}
      onCreateCollection={onCreateCollection}
      onSelectCollection={onSelectCollection}
      onRenameCollection={onRenameCollection}
      onDeleteCollection={onDeleteCollection}
      onClearCollection={onClearCollection}
      onDeleteAllCollections={onDeleteAllCollections}
      onAddAllToCollection={onAddAllToCollection}
      onOpenCollectionSearch={onOpenCollectionSearch}
      onShareCollection={onShareCollection}
      onCreateReviewProject={onCreateReviewProject}
      onOpenAiTranslate={onOpenAiTranslate}
    />,
  );

  if (hasSearched) {
    parts.push(<SearchShareButton key="share" onClick={onOpenShareModal} disabled={!canShare} />);
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

  const presetOptions = resultSizePresets.filter((option) => option.value >= worksetSize);
  const isPreset = presetOptions.some((option) => option.value === worksetSize);
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
            <div className="workbench-searchmode__label">Result size limit</div>
            <div className="workbench-filterchip__pills workbench-worksetbar__pills">
              {presetOptions.map((option) => (
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

function CollectionDropdown({
  disabled,
  isSearchLoading,
  hasSearched,
  rowCount,
  collections,
  activeCollectionId,
  activeCollectionName,
  activeCollectionCount,
  onCreateCollection,
  onSelectCollection,
  onRenameCollection,
  onDeleteCollection,
  onClearCollection,
  onDeleteAllCollections,
  onAddAllToCollection,
  onOpenCollectionSearch,
  onShareCollection,
  onCreateReviewProject,
  onOpenAiTranslate,
}: {
  disabled: boolean;
  isSearchLoading: boolean;
  hasSearched: boolean;
  rowCount: number;
  collections: WorkbenchCollection[];
  activeCollectionId: string | null;
  activeCollectionName: string | null;
  activeCollectionCount: number;
  onCreateCollection: (name?: string) => string | null;
  onSelectCollection: (id: string | null) => void;
  onRenameCollection: (id: string, name: string) => void;
  onDeleteCollection: (id: string) => void;
  onClearCollection: () => void;
  onDeleteAllCollections: () => void;
  onAddAllToCollection: () => void;
  onOpenCollectionSearch: (id: string) => void;
  onShareCollection: (id: string) => boolean;
  onCreateReviewProject: (id: string) => void;
  onOpenAiTranslate: (id: string) => void;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [nameDraft, setNameDraft] = useState(activeCollectionName ?? '');
  const [isEditingName, setIsEditingName] = useState(false);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const nameInputRef = useRef<HTMLInputElement | null>(null);

  const buttonLabel = activeCollectionName
    ? `${activeCollectionName} (${activeCollectionCount})`
    : 'None selected';
  const hasCollections = collections.length > 0;
  const hasActiveCollection = Boolean(activeCollectionId);
  const canClearIds = hasActiveCollection && activeCollectionCount > 0;
  const canAddAll = hasActiveCollection && hasSearched && !isSearchLoading && rowCount > 0;

  useEffect(() => {
    if (disabled && isOpen) {
      setIsOpen(false);
    }
  }, [disabled, isOpen]);

  useEffect(() => {
    if (!isOpen) {
      return;
    }
    const handlePointerDown = (event: PointerEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    window.addEventListener('pointerdown', handlePointerDown);
    return () => window.removeEventListener('pointerdown', handlePointerDown);
  }, [isOpen]);

  useEffect(() => {
    setNameDraft(activeCollectionName ?? '');
  }, [activeCollectionName]);

  useEffect(() => {
    if (!isEditingName) {
      return;
    }
    queueMicrotask(() => {
      nameInputRef.current?.focus();
      nameInputRef.current?.select();
    });
  }, [isEditingName]);

  return (
    <div className="chip-dropdown workbench-worksetbar__collection" ref={containerRef}>
      <button
        type="button"
        className="workbench-worksetbar__countbutton"
        onClick={() => setIsOpen((previous) => !previous)}
        aria-expanded={isOpen}
        disabled={disabled}
      >
        <span>{`Collection: ${buttonLabel}`}</span>
        <span className="chip-dropdown__chevron" aria-hidden="true" />
      </button>
      {isOpen ? (
        <div className="chip-dropdown__panel workbench-worksetbar__panel" role="menu">
          <div className="workbench-searchmode__section">
            <div className="workbench-collections__section">
              <div className="workbench-collections__row workbench-collections__row--actions">
                <span className="workbench-collections__label">Collections</span>
                <div className="workbench-collections__actions">
                  <button
                    type="button"
                    className="workbench-worksetbar__button"
                    onClick={() => {
                      onSelectCollection(null);
                      setIsEditingName(false);
                    }}
                    disabled={disabled || !hasActiveCollection}
                    title="Deselect collection"
                  >
                    None
                  </button>
                  <button
                    type="button"
                    className="workbench-worksetbar__button"
                    onClick={() => {
                      onCreateCollection();
                      setNameDraft('');
                      setIsEditingName(true);
                    }}
                    disabled={disabled}
                    title="Create new collection"
                  >
                    New
                  </button>
                  <button
                    type="button"
                    className="workbench-worksetbar__button"
                    onClick={() => {
                      if (activeCollectionId) {
                        onDeleteCollection(activeCollectionId);
                        setIsEditingName(false);
                      }
                    }}
                    disabled={disabled || !hasActiveCollection}
                    title="Delete active collection"
                  >
                    Delete
                  </button>
                  <button
                    type="button"
                    className="workbench-worksetbar__button"
                    onClick={() => {
                      onDeleteAllCollections();
                      setNameDraft('');
                      setIsEditingName(false);
                    }}
                    disabled={disabled || !hasCollections}
                  >
                    Delete all
                  </button>
                </div>
              </div>
              <div className="workbench-collections__list">
                <div
                  role="button"
                  tabIndex={disabled ? -1 : 0}
                  className={`workbench-collections__item${
                    !hasActiveCollection ? ' is-active' : ''
                  }`}
                  onClick={() => {
                    if (disabled) {
                      return;
                    }
                    onSelectCollection(null);
                    setIsEditingName(false);
                  }}
                  onKeyDown={(event) => {
                    if (disabled) {
                      return;
                    }
                    if (event.key === 'Enter' || event.key === ' ') {
                      event.preventDefault();
                      onSelectCollection(null);
                      setIsEditingName(false);
                    }
                  }}
                >
                  <span className="workbench-collections__item-name">None</span>
                  <span className="workbench-collections__item-count">—</span>
                </div>
                {collections.length === 0 ? (
                  <div className="workbench-collections__empty hint">No collections yet</div>
                ) : (
                  collections.map((collection) => {
                    const isActive = collection.id === activeCollectionId;
                    const count = collection.entries.length;
                    const hasIds = count > 0;
                    const isEditingRow = isActive && isEditingName;
                    const showInput = isActive && isEditingName;
                    return (
                      <div
                        key={collection.id}
                        role="button"
                        tabIndex={disabled ? -1 : 0}
                        className={`workbench-collections__item${isActive ? ' is-active' : ''}`}
                        onClick={() => {
                          if (disabled) {
                            return;
                          }
                          onSelectCollection(collection.id);
                          setNameDraft(collection.name);
                          setIsEditingName(true);
                          queueMicrotask(() => {
                            nameInputRef.current?.focus();
                            nameInputRef.current?.select();
                          });
                        }}
                        onKeyDown={(event) => {
                          if (disabled) {
                            return;
                          }
                          if (event.key === 'Enter' || event.key === ' ') {
                            event.preventDefault();
                            onSelectCollection(collection.id);
                            setNameDraft(collection.name);
                            setIsEditingName(true);
                            queueMicrotask(() => {
                              nameInputRef.current?.focus();
                              nameInputRef.current?.select();
                            });
                          }
                        }}
                      >
                        {showInput ? (
                          <input
                            ref={nameInputRef}
                            className="workbench-collections__name-input"
                            type="text"
                            value={isEditingRow ? nameDraft : collection.name}
                            placeholder="Name this collection"
                            onFocus={() => setIsEditingName(true)}
                            maxLength={80}
                            onChange={(event) => setNameDraft(event.target.value.slice(0, 80))}
                            onBlur={() => {
                              setIsEditingName(false);
                              onRenameCollection(
                                collection.id,
                                nameDraft.trim() || collection.name,
                              );
                              setNameDraft(collection.name);
                            }}
                            onKeyDown={(event) => {
                              if (event.key === 'Enter') {
                                onRenameCollection(
                                  collection.id,
                                  nameDraft.trim() || collection.name,
                                );
                                setIsEditingName(false);
                              }
                              if (event.key === 'Escape') {
                                setNameDraft(collection.name);
                                setIsEditingName(false);
                                (event.target as HTMLInputElement).blur();
                              }
                            }}
                            disabled={disabled}
                          />
                        ) : (
                          <span className="workbench-collections__item-name">
                            {collection.name}
                          </span>
                        )}
                        <span className="workbench-collections__item-count">{count}</span>
                        <div className="workbench-collections__item-actions">
                          <button
                            type="button"
                            className="workbench-worksetbar__button"
                            onClick={(event) => {
                              event.stopPropagation();
                              onOpenCollectionSearch(collection.id);
                            }}
                            disabled={disabled || !hasIds}
                            title={
                              hasIds ? 'View these ids in the workbench' : 'Add ids before opening'
                            }
                          >
                            View
                          </button>
                          <button
                            type="button"
                            className="workbench-worksetbar__button"
                            onClick={(event) => {
                              event.stopPropagation();
                              onCreateReviewProject(collection.id);
                            }}
                            disabled={disabled || !hasIds}
                            title="Create review project from this collection"
                          >
                            Review
                          </button>
                          <button
                            type="button"
                            className="workbench-worksetbar__button"
                            onClick={(event) => {
                              event.stopPropagation();
                              onOpenAiTranslate(collection.id);
                            }}
                            disabled={disabled || !hasIds}
                            title="AI translate this collection"
                          >
                            AI Translate
                          </button>
                          <button
                            type="button"
                            className="workbench-worksetbar__button"
                            onClick={(event) => {
                              event.stopPropagation();
                              const ok = onShareCollection(collection.id);
                              if (ok) {
                                setIsOpen(false);
                              }
                            }}
                            disabled={disabled || !hasIds}
                            title="Share this collection"
                          >
                            Share
                          </button>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </div>
            <div className="workbench-collections__section">
              <div className="workbench-collections__row workbench-collections__row--actions">
                <span className="workbench-collections__label">Text unit ids</span>
                <div className="workbench-collections__actions">
                  <button
                    type="button"
                    className="workbench-worksetbar__button"
                    onClick={() => {
                      onClearCollection();
                    }}
                    disabled={disabled || !canClearIds}
                    title="Clear ids in the active collection"
                  >
                    Clear ids
                  </button>
                  <button
                    type="button"
                    className="workbench-worksetbar__button"
                    onClick={() => {
                      onAddAllToCollection();
                    }}
                    disabled={disabled || !canAddAll}
                    title="Add all ids from the current search results"
                  >
                    Add all in search
                  </button>
                </div>
              </div>
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
