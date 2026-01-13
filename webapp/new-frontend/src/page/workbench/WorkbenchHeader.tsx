import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import type { SearchAttribute, SearchType } from '../../api/text-units';
import { MultiSelectChip } from '../../components/MultiSelectChip';
import { resultSizePresets } from './workbench-constants';
import { loadPreferredLocales, PREFERRED_LOCALES_KEY } from './workbench-preferences';
import type { LocaleOption, RepositoryOption, StatusFilterValue } from './workbench-types';

type SearchAttributeOption = { value: SearchAttribute; label: string; helper?: string };
type SearchTypeOption = { value: SearchType; label: string; helper?: string };
type StatusFilterOption = { value: StatusFilterValue; label: string };

const searchAttributeOptions: SearchAttributeOption[] = [
  { value: 'target', label: 'Translation' },
  { value: 'source', label: 'Source' },
  { value: 'stringId', label: 'String ID' },
  { value: 'asset', label: 'Asset path' },
  { value: 'location', label: 'Location' },
  { value: 'pluralFormOther', label: 'Plural (other)' },
  { value: 'tmTextUnitIds', label: 'TextUnit IDs' },
];

const searchTypeOptions: SearchTypeOption[] = [
  { value: 'exact', label: 'Exact match', helper: 'Full string' },
  { value: 'contains', label: 'Contains', helper: 'Case-sensitive' },
  { value: 'ilike', label: 'iLike', helper: 'Pattern (% = any, _ = 1 char)' },
];

const statusFilterOptions: StatusFilterOption[] = [
  // Match legacy workbench semantics and wording.
  { value: 'ALL', label: 'All statuses' },
  { value: 'TRANSLATED', label: 'Translated' },
  { value: 'UNTRANSLATED', label: 'Untranslated' },
  { value: 'FOR_TRANSLATION', label: 'To translate' },
  { value: 'REVIEW_NEEDED', label: 'To review' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'APPROVED_AND_NOT_REJECTED', label: 'Accepted' },
];

type WorkbenchHeaderProps = {
  disabled: boolean;
  isEditMode: boolean;
  worksetSize: number;
  onChangeWorksetSize: (value: number) => void;
  repositoryOptions: RepositoryOption[];
  selectedRepositoryIds: number[];
  onChangeRepositorySelection: (next: number[]) => void;
  isRepositoryLoading: boolean;
  localeOptions: LocaleOption[];
  selectedLocaleTags: string[];
  onChangeLocaleSelection: (next: string[]) => void;
  userLocales: string[];
  isLimitedTranslator: boolean;
  searchAttribute: SearchAttribute;
  searchType: SearchType;
  onChangeSearchAttribute: (value: SearchAttribute) => void;
  onChangeSearchType: (value: SearchType) => void;
  searchInputValue: string;
  onChangeSearchInput: (value: string) => void;
  onSubmitSearch: () => void;
  statusFilter: StatusFilterValue;
  includeUsed: boolean;
  includeUnused: boolean;
  includeTranslate: boolean;
  includeDoNotTranslate: boolean;
  onChangeStatusFilter: (value: StatusFilterValue) => void;
  onChangeIncludeUsed: (value: boolean) => void;
  onChangeIncludeUnused: (value: boolean) => void;
  onChangeIncludeTranslate: (value: boolean) => void;
  onChangeIncludeDoNotTranslate: (value: boolean) => void;
  createdBefore: string | null;
  createdAfter: string | null;
  onChangeCreatedBefore: (value: string | null) => void;
  onChangeCreatedAfter: (value: string | null) => void;
};

export function WorkbenchHeader({
  disabled,
  isEditMode,
  worksetSize,
  onChangeWorksetSize,
  repositoryOptions,
  selectedRepositoryIds,
  onChangeRepositorySelection,
  isRepositoryLoading,
  localeOptions,
  selectedLocaleTags,
  onChangeLocaleSelection,
  userLocales,
  isLimitedTranslator,
  searchAttribute,
  searchType,
  onChangeSearchAttribute,
  onChangeSearchType,
  searchInputValue,
  onChangeSearchInput,
  onSubmitSearch,
  statusFilter,
  includeUsed,
  includeUnused,
  includeTranslate,
  includeDoNotTranslate,
  onChangeStatusFilter,
  onChangeIncludeUsed,
  onChangeIncludeUnused,
  onChangeIncludeTranslate,
  onChangeIncludeDoNotTranslate,
  createdBefore,
  createdAfter,
  onChangeCreatedBefore,
  onChangeCreatedAfter,
}: WorkbenchHeaderProps) {
  const [preferredLocales, setPreferredLocales] = useState<string[]>(() => loadPreferredLocales());
  const repositoryMultiOptions = repositoryOptions.map((option) => ({
    value: option.id,
    label: option.name,
  }));
  const localeMultiOptions = localeOptions.map((option) => ({
    value: option.tag,
    label: option.label,
  }));

  useEffect(() => {
    const handleStorage = (event: StorageEvent) => {
      if (event.key && event.key !== PREFERRED_LOCALES_KEY) {
        return;
      }
      setPreferredLocales(loadPreferredLocales());
    };
    window.addEventListener('storage', handleStorage);
    return () => window.removeEventListener('storage', handleStorage);
  }, []);

  const myLocaleSelections = useMemo(() => {
    const available = new Set(localeOptions.map((option) => option.tag.toLowerCase()));
    const candidates = isLimitedTranslator ? userLocales : preferredLocales;
    if (!candidates.length) {
      return [];
    }
    return candidates.filter((tag) => available.has(tag.toLowerCase()));
  }, [isLimitedTranslator, localeOptions, preferredLocales, userLocales]);

  const searchPlaceholder = (() => {
    switch (searchAttribute) {
      case 'target':
        return 'Search translation';
      case 'source':
        return 'Search source text';
      case 'stringId':
        return 'Search string ID';
      case 'asset':
        return 'Search asset path';
      case 'location':
        return 'Search location (usage)';
      case 'pluralFormOther':
        return 'Search plural form (other)';
      case 'tmTextUnitIds':
        return 'Search TM TextUnit IDs (comma or space separated)';
      default:
        return 'Search';
    }
  })();

  const searchControlsDisabled = disabled || isEditMode;
  // Keep the header summary stable while repositories are loading to avoid flicker.
  const repositoriesEmptyLabel = isRepositoryLoading ? 'Repositories' : 'No repositories';
  const localesEmptyLabel = isRepositoryLoading ? 'Locales' : 'No locales';
  const isMyLocaleSelectionActive = useMemo(() => {
    if (myLocaleSelections.length === 0) {
      return false;
    }
    if (selectedLocaleTags.length !== myLocaleSelections.length) {
      return false;
    }
    const selectedSet = new Set(selectedLocaleTags.map((tag) => tag.toLowerCase()));
    return myLocaleSelections.every((tag) => selectedSet.has(tag.toLowerCase()));
  }, [myLocaleSelections, selectedLocaleTags]);
  const handleSelectMyLocales = useCallback(() => {
    if (!myLocaleSelections.length) {
      return;
    }
    onChangeLocaleSelection(myLocaleSelections);
  }, [myLocaleSelections, onChangeLocaleSelection]);
  const shouldShowMyLocalesAction = isLimitedTranslator || preferredLocales.length > 0;
  const localeCustomActions = shouldShowMyLocalesAction
    ? [
        {
          label: 'My locales',
          onClick: handleSelectMyLocales,
          disabled: myLocaleSelections.length === 0 || isMyLocaleSelectionActive,
          ariaLabel: isLimitedTranslator
            ? 'Select your assigned locales'
            : 'Select your preferred locales',
        },
      ]
    : undefined;
  return (
    <div className="workbench-page__header workbench-header">
      <div className="workbench-header__left">
        <MultiSelectChip
          className="workbench-chip-dropdown"
          label="Repositories"
          options={repositoryMultiOptions}
          selectedValues={selectedRepositoryIds}
          onChange={onChangeRepositorySelection}
          disabled={searchControlsDisabled}
          placeholder="Repositories"
          emptyOptionsLabel={repositoriesEmptyLabel}
          summaryFormatter={({ options, selectedValues }) => {
            if (!options.length) {
              return repositoriesEmptyLabel;
            }
            if (!selectedValues.length) {
              return 'Repositories';
            }
            if (selectedValues.length === options.length) {
              return 'All repositories';
            }
            if (selectedValues.length === 1) {
              const selectedSet = new Set(selectedValues);
              return options
                .filter((option) => selectedSet.has(option.value))
                .map((option) => option.label)
                .join(', ');
            }
            return `${selectedValues.length} repositories`;
          }}
        />
        <MultiSelectChip
          className="workbench-chip-dropdown workbench-chip-dropdown--locale"
          label="Locales"
          options={localeMultiOptions}
          selectedValues={selectedLocaleTags}
          onChange={onChangeLocaleSelection}
          disabled={searchControlsDisabled}
          placeholder="Locales"
          emptyOptionsLabel={localesEmptyLabel}
          customActions={localeCustomActions}
          summaryFormatter={({ options, selectedValues }) => {
            if (!options.length) {
              return localesEmptyLabel;
            }
            if (!selectedValues.length) {
              return 'Locales';
            }
            if (selectedValues.length === options.length) {
              return 'All locales';
            }
            if (selectedValues.length === 1) {
              const selectedSet = new Set(selectedValues);
              return options
                .filter((option) => selectedSet.has(option.value))
                .map((option) => option.label)
                .join(', ');
            }
            return `${selectedValues.length} locales`;
          }}
        />
      </div>

      <div className="workbench-header__search">
        <SearchControl
          value={searchInputValue}
          onChange={onChangeSearchInput}
          onSubmit={onSubmitSearch}
          inputDisabled={searchControlsDisabled}
          placeholder={searchPlaceholder}
          searchAttribute={searchAttribute}
          searchType={searchType}
          onChangeAttribute={onChangeSearchAttribute}
          onChangeType={onChangeSearchType}
        />
      </div>

      <div className="workbench-header__right">
        <SearchFilter
          disabled={searchControlsDisabled}
          statusFilter={statusFilter}
          includeUsed={includeUsed}
          includeUnused={includeUnused}
          includeTranslate={includeTranslate}
          includeDoNotTranslate={includeDoNotTranslate}
          createdBefore={createdBefore}
          createdAfter={createdAfter}
          onChangeCreatedBefore={onChangeCreatedBefore}
          onChangeCreatedAfter={onChangeCreatedAfter}
          worksetSize={worksetSize}
          onChangeWorksetSize={onChangeWorksetSize}
          onChangeStatusFilter={onChangeStatusFilter}
          onChangeIncludeUsed={onChangeIncludeUsed}
          onChangeIncludeUnused={onChangeIncludeUnused}
          onChangeIncludeTranslate={onChangeIncludeTranslate}
          onChangeIncludeDoNotTranslate={onChangeIncludeDoNotTranslate}
        />
      </div>
    </div>
  );
}

type SearchControlProps = {
  value: string;
  onChange: (next: string) => void;
  onSubmit: () => void;
  inputDisabled: boolean;
  placeholder: string;
  searchAttribute: SearchAttribute;
  searchType: SearchType;
  onChangeAttribute: (value: SearchAttribute) => void;
  onChangeType: (value: SearchType) => void;
  variant?: 'standalone' | 'inline';
};

function SearchControl({
  value,
  onChange,
  onSubmit,
  inputDisabled,
  placeholder,
  searchAttribute,
  searchType,
  onChangeAttribute,
  onChangeType,
  variant = 'standalone',
}: SearchControlProps) {
  const showClear = value.length > 0 && !inputDisabled;
  const containerClassName = [
    'workbench-searchcontrol',
    variant === 'inline' ? 'workbench-searchcontrol--inline' : null,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div className={containerClassName}>
      <div className="workbench-searchcontrol__shell">
        <SearchModeChip
          disabled={inputDisabled}
          searchAttribute={searchAttribute}
          searchType={searchType}
          onChangeAttribute={onChangeAttribute}
          onChangeType={onChangeType}
          variant="inline"
        />
        <form
          className="workbench-searchcontrol__form"
          onSubmit={(event) => {
            event.preventDefault();
            onSubmit();
          }}
        >
          <input
            id="workbench-search-input"
            className="workbench-searchcontrol__input"
            type="text"
            value={value}
            placeholder={placeholder}
            onChange={(event) => onChange(event.target.value)}
            disabled={inputDisabled}
          />
          {showClear ? (
            <button
              type="button"
              className="workbench-searchcontrol__clear"
              onClick={() => onChange('')}
              aria-label="Clear search text"
            >
              ×
            </button>
          ) : null}
        </form>
      </div>
    </div>
  );
}

type SearchModeChipProps = {
  disabled: boolean;
  searchAttribute: SearchAttribute;
  searchType: SearchType;
  onChangeAttribute: (value: SearchAttribute) => void;
  onChangeType: (value: SearchType) => void;
  variant?: 'standalone' | 'inline';
};

function SearchModeChip({
  disabled,
  searchAttribute,
  searchType,
  onChangeAttribute,
  onChangeType,
  variant = 'standalone',
}: SearchModeChipProps) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement | null>(null);

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

  const attributeLabel =
    searchAttributeOptions.find((option) => option.value === searchAttribute)?.label ?? 'Attribute';
  const typeLabel =
    searchTypeOptions.find((option) => option.value === searchType)?.label ?? 'Match';
  const summary = `${attributeLabel} · ${typeLabel}`;

  const containerClassName = [
    'chip-dropdown',
    'workbench-searchmode',
    variant === 'inline' ? 'workbench-searchmode--inline' : null,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div className={containerClassName} ref={containerRef}>
      <button
        type="button"
        className="chip-dropdown__button workbench-searchmode__button"
        onClick={() => setIsOpen((previous) => !previous)}
        aria-expanded={isOpen}
        disabled={disabled}
      >
        <span className="chip-dropdown__summary">{summary}</span>
        <span className="chip-dropdown__chevron" aria-hidden="true" />
      </button>
      {isOpen ? (
        <div className="chip-dropdown__panel workbench-searchmode__panel" role="menu">
          <div className="workbench-searchmode__section">
            <div className="workbench-searchmode__label">Search attribute</div>
            <div className="workbench-searchmode__list">
              {searchAttributeOptions.map((option) => (
                <button
                  type="button"
                  key={option.value}
                  className={`workbench-searchmode__option${
                    option.value === searchAttribute ? ' is-active' : ''
                  }`}
                  onClick={() => onChangeAttribute(option.value)}
                >
                  <span>{option.label}</span>
                  {option.helper ? (
                    <span className="workbench-searchmode__helper">{option.helper}</span>
                  ) : null}
                </button>
              ))}
            </div>
          </div>
          <div className="workbench-searchmode__section">
            <div className="workbench-searchmode__label">Match type</div>
            <div className="workbench-searchmode__list">
              {searchTypeOptions.map((option) => (
                <button
                  type="button"
                  key={option.value}
                  className={`workbench-searchmode__option${
                    option.value === searchType ? ' is-active' : ''
                  }`}
                  onClick={() => onChangeType(option.value)}
                >
                  <span>{option.label}</span>
                  {option.helper ? (
                    <span className="workbench-searchmode__helper">{option.helper}</span>
                  ) : null}
                </button>
              ))}
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

type FilterChipProps = {
  disabled: boolean;
  statusFilter: StatusFilterValue;
  includeUsed: boolean;
  includeUnused: boolean;
  includeTranslate: boolean;
  includeDoNotTranslate: boolean;
  createdBefore: string | null;
  createdAfter: string | null;
  onChangeCreatedBefore: (value: string | null) => void;
  onChangeCreatedAfter: (value: string | null) => void;
  worksetSize: number;
  onChangeWorksetSize: (value: number) => void;
  onChangeStatusFilter: (value: StatusFilterValue) => void;
  onChangeIncludeUsed: (value: boolean) => void;
  onChangeIncludeUnused: (value: boolean) => void;
  onChangeIncludeTranslate: (value: boolean) => void;
  onChangeIncludeDoNotTranslate: (value: boolean) => void;
};

function SearchFilter({
  disabled,
  statusFilter,
  includeUsed,
  includeUnused,
  includeTranslate,
  includeDoNotTranslate,
  createdBefore,
  createdAfter,
  onChangeCreatedBefore,
  onChangeCreatedAfter,
  worksetSize,
  onChangeWorksetSize,
  onChangeStatusFilter,
  onChangeIncludeUsed,
  onChangeIncludeUnused,
  onChangeIncludeTranslate,
  onChangeIncludeDoNotTranslate,
}: FilterChipProps) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const worksetInputRef = useRef<HTMLInputElement | null>(null);
  const [worksetDraft, setWorksetDraft] = useState(String(worksetSize));
  const [showWorksetCustomInput, setShowWorksetCustomInput] = useState(false);

  const worksetPresets = resultSizePresets;

  const isWorksetPreset = worksetPresets.some((option) => option.value === worksetSize);
  const isWorksetCustomActive = showWorksetCustomInput || !isWorksetPreset;
  const commitWorksetDraft = useCallback(() => {
    const trimmed = worksetDraft.trim();
    if (!trimmed) {
      setWorksetDraft(String(worksetSize));
      return;
    }
    const next = parseInt(trimmed, 10);
    if (Number.isNaN(next) || next < 1) {
      setWorksetDraft(String(worksetSize));
      return;
    }
    if (next === worksetSize) {
      return;
    }
    onChangeWorksetSize(next);
  }, [onChangeWorksetSize, worksetDraft, worksetSize]);

  useEffect(() => {
    if (disabled && isOpen) {
      if (isWorksetCustomActive) {
        commitWorksetDraft();
      }
      setIsOpen(false);
    }
  }, [commitWorksetDraft, disabled, isOpen, isWorksetCustomActive]);

  useEffect(() => {
    if (!isOpen) {
      return;
    }
    setWorksetDraft(String(worksetSize));
    setShowWorksetCustomInput(false);
  }, [isOpen, worksetSize]);

  useEffect(() => {
    if (!isOpen) {
      return;
    }
    const handlePointerDown = (event: PointerEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) {
        // If the panel is closed via an outside click, commit the draft first so users don't
        // need to press Enter to apply the custom size.
        if (isWorksetCustomActive) {
          commitWorksetDraft();
        }
        setIsOpen(false);
      }
    };
    window.addEventListener('pointerdown', handlePointerDown);
    return () => window.removeEventListener('pointerdown', handlePointerDown);
  }, [commitWorksetDraft, isOpen, isWorksetCustomActive]);

  const statusLabel =
    statusFilterOptions.find((option) => option.value === statusFilter)?.label ?? 'All statuses';
  const summaryParts: string[] = [statusLabel];

  // Keep the chip label descriptive for the "high signal" filters (status, used, date),
  // and show the result limit when it differs from the default.
  const hasDateFilter = Boolean(createdBefore) || Boolean(createdAfter);
  if (hasDateFilter) {
    // Date ranges are too long for the chip; we only hint that a date filter is active.
    summaryParts.push('Date');
  }

  const usageLabel =
    includeUsed && includeUnused ? 'Used: any' : !includeUsed && includeUnused ? 'Unused' : null;
  if (usageLabel) {
    summaryParts.push(usageLabel);
  }

  const defaultWorksetSize = 10;
  if (worksetSize !== defaultWorksetSize) {
    const presetLabel = worksetPresets.find((preset) => preset.value === worksetSize)?.label;
    const compactLabel =
      presetLabel ??
      (worksetSize >= 1000 && worksetSize % 1000 === 0
        ? `${worksetSize / 1000}k`
        : String(worksetSize));
    summaryParts.push(`Size ${compactLabel}`);
  }

  // Translate filter is intentionally not surfaced in the summary (low signal for this UI).
  const summary = summaryParts.join(' · ');

  const quickRanges: Array<{
    label: string;
    getRange: () => { after: string | null; before: string | null };
  }> = [
    { label: 'Last 5 min', getRange: () => ({ after: minutesAgoIso(5), before: null }) },
    { label: 'Last 10 min', getRange: () => ({ after: minutesAgoIso(10), before: null }) },
    { label: 'Last hour', getRange: () => ({ after: minutesAgoIso(60), before: null }) },
    { label: 'Today', getRange: () => ({ after: startOfTodayIso(), before: null }) },
    {
      label: 'Yesterday',
      getRange: () => ({ after: startOfYesterdayIso(), before: startOfTodayIso() }),
    },
    { label: 'This week', getRange: () => ({ after: startOfWeekIso(), before: null }) },
  ];

  return (
    <div className="chip-dropdown" ref={containerRef} data-align="right">
      <button
        type="button"
        className="chip-dropdown__button workbench-filterchip__button"
        onClick={() => setIsOpen((previous) => !previous)}
        aria-expanded={isOpen}
        disabled={disabled}
      >
        <span className="chip-dropdown__summary">{summary}</span>
        <span className="chip-dropdown__chevron" aria-hidden="true" />
      </button>
      {isOpen ? (
        <div className="chip-dropdown__panel workbench-filterchip__panel" role="menu">
          <div className="workbench-searchmode__section">
            <div className="workbench-searchmode__label">Status</div>
            <div className="workbench-searchmode__list">
              {statusFilterOptions.map((option) => (
                <button
                  type="button"
                  key={option.value}
                  className={`workbench-searchmode__option${
                    option.value === statusFilter ? ' is-active' : ''
                  }`}
                  onClick={() => onChangeStatusFilter(option.value)}
                >
                  <span>{option.label}</span>
                </button>
              ))}
            </div>
          </div>
          <div className="workbench-searchmode__section">
            <div className="workbench-searchmode__label">Used</div>
            <div className="workbench-searchmode__list">
              <label className="workbench-filterchip__checkbox">
                <input
                  type="checkbox"
                  checked={includeUsed}
                  onChange={() => {
                    if (includeUsed && !includeUnused) {
                      return;
                    }
                    onChangeIncludeUsed(!includeUsed);
                  }}
                />
                <span>Yes</span>
              </label>
              <label className="workbench-filterchip__checkbox">
                <input
                  type="checkbox"
                  checked={includeUnused}
                  onChange={() => {
                    if (includeUnused && !includeUsed) {
                      return;
                    }
                    onChangeIncludeUnused(!includeUnused);
                  }}
                />
                <span>No</span>
              </label>
            </div>
          </div>
          <div className="workbench-searchmode__section">
            <div className="workbench-searchmode__label">Translate</div>
            <div className="workbench-searchmode__list">
              <label className="workbench-filterchip__checkbox">
                <input
                  type="checkbox"
                  checked={includeTranslate}
                  onChange={() => {
                    if (includeTranslate && !includeDoNotTranslate) {
                      return;
                    }
                    onChangeIncludeTranslate(!includeTranslate);
                  }}
                />
                <span>Yes</span>
              </label>
              <label className="workbench-filterchip__checkbox">
                <input
                  type="checkbox"
                  checked={includeDoNotTranslate}
                  onChange={() => {
                    if (includeDoNotTranslate && !includeTranslate) {
                      return;
                    }
                    onChangeIncludeDoNotTranslate(!includeDoNotTranslate);
                  }}
                />
                <span>No</span>
              </label>
            </div>
          </div>
          <div className="workbench-searchmode__section">
            <div className="workbench-searchmode__label">Created after</div>
            <input
              type="datetime-local"
              value={createdAfter ? isoToLocalInput(createdAfter) : ''}
              onChange={(event) =>
                onChangeCreatedAfter(
                  event.target.value ? localInputToIso(event.target.value) : null,
                )
              }
              className="workbench-datefilter__input"
              disabled={disabled}
            />
          </div>
          <div className="workbench-searchmode__section">
            <div className="workbench-searchmode__label">Created before</div>
            <input
              type="datetime-local"
              value={createdBefore ? isoToLocalInput(createdBefore) : ''}
              onChange={(event) =>
                onChangeCreatedBefore(
                  event.target.value ? localInputToIso(event.target.value) : null,
                )
              }
              className="workbench-datefilter__input"
              disabled={disabled}
            />
          </div>
          <div className="workbench-datefilter__quick">
            {quickRanges.map((range) => (
              <button
                type="button"
                key={range.label}
                className="workbench-datefilter__quick-chip"
                onClick={() => {
                  const { after, before } = range.getRange();
                  onChangeCreatedAfter(after);
                  onChangeCreatedBefore(before);
                }}
                disabled={disabled}
              >
                {range.label}
              </button>
            ))}
            {hasDateFilter ? (
              <button
                type="button"
                className="workbench-filterchip__clear-link"
                onClick={() => {
                  onChangeCreatedBefore(null);
                  onChangeCreatedAfter(null);
                }}
                disabled={disabled}
              >
                Clear dates
              </button>
            ) : null}
          </div>
          <div className="workbench-searchmode__section">
            <div className="workbench-searchmode__label">Result size limit</div>
            <div className="workbench-filterchip__pills">
              {worksetPresets.map((option) => (
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
              {isWorksetCustomActive ? (
                <div className="workbench-worksetcustom is-active">
                  <span className="workbench-worksetcustom__label">Custom</span>
                  <input
                    ref={worksetInputRef}
                    className="workbench-worksetcustom__input"
                    type="number"
                    inputMode="numeric"
                    min={1}
                    value={worksetDraft}
                    onChange={(event) => setWorksetDraft(event.target.value)}
                    onBlur={() => {
                      commitWorksetDraft();
                    }}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter') {
                        commitWorksetDraft();
                      }
                      if (event.key === 'Escape') {
                        setWorksetDraft(String(worksetSize));
                        setShowWorksetCustomInput(false);
                      }
                    }}
                  />
                </div>
              ) : (
                <button
                  type="button"
                  className="workbench-datefilter__quick-chip"
                  onClick={() => {
                    setShowWorksetCustomInput(true);
                    setWorksetDraft(String(worksetSize));
                    queueMicrotask(() => {
                      worksetInputRef.current?.focus();
                      worksetInputRef.current?.select();
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

const minutesAgoIso = (minutes: number) => new Date(Date.now() - minutes * 60 * 1000).toISOString();

function startOfTodayIso() {
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  return now.toISOString();
}

function startOfYesterdayIso() {
  const date = new Date();
  date.setDate(date.getDate() - 1);
  date.setHours(0, 0, 0, 0);
  return date.toISOString();
}

function startOfWeekIso() {
  const now = new Date();
  const day = now.getDay();
  const diff = (day + 6) % 7;
  now.setDate(now.getDate() - diff);
  now.setHours(0, 0, 0, 0);
  return now.toISOString();
}

function isoToLocalInput(iso: string) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  const offset = date.getTimezoneOffset();
  const local = new Date(date.getTime() - offset * 60 * 1000);
  return local.toISOString().slice(0, 16);
}

function localInputToIso(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toISOString();
}
