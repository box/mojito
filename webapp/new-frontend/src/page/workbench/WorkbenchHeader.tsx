import { useEffect, useMemo, useState } from 'react';

import type { SearchAttribute, SearchType } from '../../api/text-units';
import { MultiSectionFilterChip } from '../../components/filters/MultiSectionFilterChip';
import { LocaleMultiSelect } from '../../components/LocaleMultiSelect';
import {
  RepositoryMultiSelect,
  type RepositoryMultiSelectOption,
} from '../../components/RepositoryMultiSelect';
import { SearchControl } from '../../components/SearchControl';
import { getStandardDateQuickRanges } from '../../utils/dateQuickRanges';
import type { LocaleSelectionOption } from '../../utils/localeSelection';
import { filterMyLocales } from '../../utils/localeSelection';
import { resultSizePresets, WORKSET_SIZE_DEFAULT, WORKSET_SIZE_MIN } from './workbench-constants';
import { loadPreferredLocales, PREFERRED_LOCALES_KEY } from './workbench-preferences';
import type { StatusFilterValue } from './workbench-types';

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
  {
    value: 'regex',
    label: 'Regex',
    helper: '\\x{FFFF}, ^/$, .*, (?i)incensitive',
  },
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
  worksetSize: number;
  onChangeWorksetSize: (value: number) => void;
  repositoryOptions: RepositoryMultiSelectOption[];
  selectedRepositoryIds: number[];
  onChangeRepositorySelection: (next: number[]) => void;
  isRepositoryLoading: boolean;
  localeOptions: LocaleSelectionOption[];
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
  translationCreatedBefore: string | null;
  translationCreatedAfter: string | null;
  onChangeTranslationCreatedBefore: (value: string | null) => void;
  onChangeTranslationCreatedAfter: (value: string | null) => void;
};

export function WorkbenchHeader({
  disabled,
  worksetSize,
  onChangeWorksetSize,
  repositoryOptions,
  selectedRepositoryIds,
  onChangeRepositorySelection,
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
  translationCreatedBefore,
  translationCreatedAfter,
  onChangeTranslationCreatedBefore,
  onChangeTranslationCreatedAfter,
}: WorkbenchHeaderProps) {
  const [preferredLocales, setPreferredLocales] = useState<string[]>(() => loadPreferredLocales());

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

  const myLocaleSelections = useMemo(
    () =>
      filterMyLocales({
        availableLocaleTags: localeOptions.map((option) => option.tag),
        userLocales,
        preferredLocales,
        isLimitedTranslator,
        // Header does not receive role; mimic previous behavior by using preferred locales when not limited.
        isAdmin: !isLimitedTranslator,
      }),
    [isLimitedTranslator, localeOptions, preferredLocales, userLocales],
  );

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

  const searchControlsDisabled = disabled;
  const shouldShowMyLocalesAction = isLimitedTranslator || preferredLocales.length > 0;
  return (
    <div className="workbench-page__header workbench-header">
      <div className="workbench-header__left">
        <RepositoryMultiSelect
          className="workbench-chip-dropdown"
          options={repositoryOptions}
          selectedIds={selectedRepositoryIds}
          onChange={onChangeRepositorySelection}
          disabled={searchControlsDisabled}
          buttonAriaLabel="Select repositories"
        />
        <LocaleMultiSelect
          className="workbench-chip-dropdown workbench-chip-dropdown--locale"
          options={localeOptions}
          selectedTags={selectedLocaleTags}
          onChange={onChangeLocaleSelection}
          disabled={searchControlsDisabled}
          myLocaleTags={shouldShowMyLocalesAction ? myLocaleSelections : []}
          myLocalesAriaLabel={
            isLimitedTranslator ? 'Select your assigned locales' : 'Select your preferred locales'
          }
        />
      </div>

      <div className="workbench-header__search">
        <SearchControl
          value={searchInputValue}
          onChange={onChangeSearchInput}
          onSubmit={onSubmitSearch}
          disabled={searchControlsDisabled}
          placeholder={searchPlaceholder}
          inputAriaLabel="Search translations"
          className="workbench-searchcontrol"
          leading={
            <MultiSectionFilterChip
              align="left"
              ariaLabel="Select search mode"
              className="workbench-searchmode workbench-searchmode--inline"
              classNames={{
                button: 'workbench-searchmode__button',
                panel: 'workbench-searchmode__panel',
                section: 'workbench-searchmode__section',
                label: 'workbench-searchmode__label',
                list: 'workbench-searchmode__list',
                option: 'workbench-searchmode__option',
                helper: 'workbench-searchmode__helper',
              }}
              disabled={searchControlsDisabled}
              sections={[
                {
                  kind: 'radio',
                  label: 'Search attribute',
                  options: searchAttributeOptions,
                  value: searchAttribute,
                  onChange: (value) => onChangeSearchAttribute(value as SearchAttribute),
                },
                {
                  kind: 'radio',
                  label: 'Match type',
                  options: searchTypeOptions,
                  value: searchType,
                  onChange: (value) => onChangeSearchType(value as SearchType),
                },
              ]}
            />
          }
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
          translationCreatedBefore={translationCreatedBefore}
          translationCreatedAfter={translationCreatedAfter}
          onChangeCreatedBefore={onChangeCreatedBefore}
          onChangeCreatedAfter={onChangeCreatedAfter}
          onChangeTranslationCreatedBefore={onChangeTranslationCreatedBefore}
          onChangeTranslationCreatedAfter={onChangeTranslationCreatedAfter}
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

type FilterChipProps = {
  disabled: boolean;
  statusFilter: StatusFilterValue;
  includeUsed: boolean;
  includeUnused: boolean;
  includeTranslate: boolean;
  includeDoNotTranslate: boolean;
  createdBefore: string | null;
  createdAfter: string | null;
  translationCreatedBefore: string | null;
  translationCreatedAfter: string | null;
  onChangeCreatedBefore: (value: string | null) => void;
  onChangeCreatedAfter: (value: string | null) => void;
  onChangeTranslationCreatedBefore: (value: string | null) => void;
  onChangeTranslationCreatedAfter: (value: string | null) => void;
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
  translationCreatedBefore,
  translationCreatedAfter,
  onChangeCreatedBefore,
  onChangeCreatedAfter,
  onChangeTranslationCreatedBefore,
  onChangeTranslationCreatedAfter,
  worksetSize,
  onChangeWorksetSize,
  onChangeStatusFilter,
  onChangeIncludeUsed,
  onChangeIncludeUnused,
  onChangeIncludeTranslate,
  onChangeIncludeDoNotTranslate,
}: FilterChipProps) {
  const worksetPresets = resultSizePresets;
  const statusLabel =
    statusFilterOptions.find((option) => option.value === statusFilter)?.label ?? 'All statuses';
  const defaultWorksetSize = WORKSET_SIZE_DEFAULT;

  type UsageFilterValue = 'any' | 'used' | 'unused';
  type TranslateFilterValue = 'any' | 'translate' | 'do-not-translate';

  const usageValue: UsageFilterValue =
    includeUsed && includeUnused ? 'any' : includeUsed ? 'used' : 'unused';
  const translateValue: TranslateFilterValue =
    includeTranslate && includeDoNotTranslate
      ? 'any'
      : includeTranslate
        ? 'translate'
        : 'do-not-translate';

  const handleUsageChange = (value: UsageFilterValue) => {
    if (value === 'any') {
      onChangeIncludeUsed(true);
      onChangeIncludeUnused(true);
    } else if (value === 'used') {
      onChangeIncludeUsed(true);
      onChangeIncludeUnused(false);
    } else {
      onChangeIncludeUsed(false);
      onChangeIncludeUnused(true);
    }
  };

  const handleTranslateChange = (value: TranslateFilterValue) => {
    if (value === 'any') {
      onChangeIncludeTranslate(true);
      onChangeIncludeDoNotTranslate(true);
    } else if (value === 'translate') {
      onChangeIncludeTranslate(true);
      onChangeIncludeDoNotTranslate(false);
    } else {
      onChangeIncludeTranslate(false);
      onChangeIncludeDoNotTranslate(true);
    }
  };

  const hasDateFilter = Boolean(createdBefore) || Boolean(createdAfter);
  const hasTranslationDateFilter =
    Boolean(translationCreatedBefore) || Boolean(translationCreatedAfter);
  const usageLabel =
    includeUsed && includeUnused ? 'Used: any' : !includeUsed && includeUnused ? 'Unused' : null;
  const summaryParts: string[] = [statusLabel];
  if (usageLabel) {
    summaryParts.push(usageLabel);
  }
  if (hasDateFilter) {
    summaryParts.push('Text unit date');
  }
  if (hasTranslationDateFilter) {
    summaryParts.push('Translation date');
  }
  if (worksetSize !== defaultWorksetSize) {
    const presetLabel = worksetPresets.find((preset) => preset.value === worksetSize)?.label;
    const compactLabel =
      presetLabel ??
      (worksetSize >= 1000 && worksetSize % 1000 === 0
        ? `${worksetSize / 1000}k`
        : String(worksetSize));
    summaryParts.push(`Size ${compactLabel}`);
  }
  const summary = summaryParts.join(' · ');

  const quickRanges = getStandardDateQuickRanges();

  return (
    <MultiSectionFilterChip
      align="right"
      ariaLabel="Filter workbench results"
      disabled={disabled}
      summary={summary}
      sections={[
        {
          kind: 'radio',
          label: 'Status',
          options: statusFilterOptions,
          value: statusFilter,
          onChange: (value) => onChangeStatusFilter(value as StatusFilterValue),
        },
        {
          kind: 'radio',
          label: 'Used',
          options: [
            { value: 'any', label: 'Any' },
            { value: 'used', label: 'Yes' },
            { value: 'unused', label: 'No' },
          ],
          value: usageValue,
          onChange: (value) => handleUsageChange(value as UsageFilterValue),
        },
        {
          kind: 'radio',
          label: 'Translate',
          options: [
            { value: 'any', label: 'Any' },
            { value: 'translate', label: 'Yes' },
            { value: 'do-not-translate', label: 'No' },
          ],
          value: translateValue,
          onChange: (value) => handleTranslateChange(value as TranslateFilterValue),
        },
        {
          kind: 'date',
          label: 'Text unit created',
          after: createdAfter ?? undefined,
          before: createdBefore ?? undefined,
          onChangeAfter: onChangeCreatedAfter,
          onChangeBefore: onChangeCreatedBefore,
          quickRanges,
          onClear: hasDateFilter
            ? () => {
                onChangeCreatedBefore(null);
                onChangeCreatedAfter(null);
              }
            : undefined,
          clearLabel: 'Clear dates',
        },
        {
          kind: 'date',
          label: 'Translation created',
          after: translationCreatedAfter ?? undefined,
          before: translationCreatedBefore ?? undefined,
          onChangeAfter: onChangeTranslationCreatedAfter,
          onChangeBefore: onChangeTranslationCreatedBefore,
          quickRanges,
          onClear: hasTranslationDateFilter
            ? () => {
                onChangeTranslationCreatedBefore(null);
                onChangeTranslationCreatedAfter(null);
              }
            : undefined,
          clearLabel: 'Clear dates',
        },
        {
          kind: 'size',
          label: 'Result size limit',
          options: worksetPresets,
          value: worksetSize,
          onChange: onChangeWorksetSize,
          min: WORKSET_SIZE_MIN,
        },
      ]}
    />
  );
}
