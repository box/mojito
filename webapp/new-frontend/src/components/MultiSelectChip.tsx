import './chip-dropdown.css';
import './multi-select-chip.css';

import { useEffect, useRef, useState } from 'react';

export type MultiSelectOption<T extends string | number> = {
  value: T;
  label: string;
};

type SummaryFormatter<T extends string | number> = (args: {
  label: string;
  options: Array<MultiSelectOption<T>>;
  selectedValues: T[];
}) => string;

export type MultiSelectChipProps<T extends string | number> = {
  label: string;
  options: Array<MultiSelectOption<T>>;
  selectedValues: T[];
  onChange: (next: T[]) => void;
  placeholder: string;
  emptyOptionsLabel: string;
  className?: string;
  align?: 'left' | 'right';
  disabled?: boolean;
  buttonAriaLabel?: string;
  searchPlaceholder?: string;
  noResultsLabel?: string;
  selectAllLabel?: string;
  clearAllLabel?: string;
  onlyLabel?: string;
  summaryFormatter?: SummaryFormatter<T>;
};

export function MultiSelectChip<T extends string | number>({
  label,
  options,
  selectedValues,
  onChange,
  placeholder,
  emptyOptionsLabel,
  className,
  align = 'left',
  disabled = false,
  buttonAriaLabel,
  searchPlaceholder,
  noResultsLabel,
  selectAllLabel,
  clearAllLabel,
  onlyLabel,
  summaryFormatter,
}: MultiSelectChipProps<T>) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [filterQuery, setFilterQuery] = useState('');

  useEffect(() => {
    if (disabled && isOpen) {
      setIsOpen(false);
      return;
    }
    if (!isOpen) {
      setFilterQuery('');
      return;
    }

    const handlePointerDown = (event: PointerEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    window.addEventListener('pointerdown', handlePointerDown);
    return () => window.removeEventListener('pointerdown', handlePointerDown);
  }, [disabled, isOpen]);

  const selectedSet = new Set(selectedValues);

  const filterInputPlaceholder = (() => {
    if (searchPlaceholder) {
      return searchPlaceholder;
    }
    if (label?.length) {
      return `Filter ${label.toLowerCase()}`;
    }
    return 'Filter options';
  })();

  const summary = summaryFormatter
    ? summaryFormatter({ label, options, selectedValues })
    : (() => {
        if (!options.length) {
          return emptyOptionsLabel;
        }
        if (!selectedValues.length) {
          return placeholder;
        }
        if (selectedValues.length === options.length) {
          return `All ${label.toLowerCase()}`;
        }
        if (selectedValues.length <= 2) {
          return options
            .filter((option) => selectedSet.has(option.value))
            .map((option) => option.label)
            .join(', ');
        }
        return `${selectedValues.length} selected`;
      })();

  const isPlaceholder = options.length > 0 && selectedValues.length === 0;

  const toggleValue = (value: T) => {
    if (selectedSet.has(value)) {
      onChange(selectedValues.filter((item) => item !== value));
      return;
    }
    onChange([...selectedValues, value]);
  };

  const selectAll = () => {
    onChange(options.map((option) => option.value));
  };

  const clearAll = () => {
    onChange([]);
  };

  const visibleOptions = filterQuery
    ? options.filter((option) =>
        option.label.toLowerCase().includes(filterQuery.trim().toLowerCase()),
      )
    : options;

  const resolvedClassName = ['chip-dropdown', 'multi-select-chip', className]
    .filter(Boolean)
    .join(' ');
  const resolvedButtonAriaLabel = buttonAriaLabel ?? label;
  const noResultsText = noResultsLabel ?? 'No matches';
  const selectAllText = selectAllLabel ?? 'Select all';
  const clearAllText = clearAllLabel ?? 'Clear';
  const onlyText = onlyLabel ?? 'Only';

  return (
    <div
      className={resolvedClassName}
      ref={containerRef}
      data-align={align === 'right' ? 'right' : undefined}
    >
      <button
        type="button"
        className="chip-dropdown__button"
        onClick={() => setIsOpen((previous) => !previous)}
        disabled={disabled || !options.length}
        aria-expanded={isOpen}
        aria-label={resolvedButtonAriaLabel}
      >
        <span className={`chip-dropdown__summary${isPlaceholder ? ' is-placeholder' : ''}`}>
          {summary}
        </span>
        <span className="chip-dropdown__chevron" aria-hidden="true" />
      </button>
      {isOpen ? (
        <div className="chip-dropdown__panel" role="menu">
          {options.length ? (
            <>
              <input
                type="search"
                value={filterQuery}
                onChange={(event) => setFilterQuery(event.target.value)}
                placeholder={filterInputPlaceholder}
                className="multi-select-chip__search"
              />
              <div className="multi-select-chip__actions">
                <button
                  type="button"
                  className="multi-select-chip__action-button"
                  onClick={selectAll}
                  disabled={selectedValues.length === options.length}
                >
                  {selectAllText}
                </button>
                <button
                  type="button"
                  className="multi-select-chip__action-button"
                  onClick={clearAll}
                  disabled={selectedValues.length === 0}
                >
                  {clearAllText}
                </button>
              </div>
              <div className="multi-select-chip__options">
                {visibleOptions.length ? (
                  visibleOptions.map((option) => {
                    const checked = selectedSet.has(option.value);
                    return (
                      <label key={String(option.value)} className="multi-select-chip__option">
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={() => toggleValue(option.value)}
                        />
                        <span>{option.label}</span>
                        <button
                          type="button"
                          className="multi-select-chip__only"
                          onClick={(event) => {
                            event.preventDefault();
                            event.stopPropagation();
                            onChange([option.value]);
                          }}
                        >
                          {onlyText}
                        </button>
                      </label>
                    );
                  })
                ) : (
                  <div className="multi-select-chip__empty">{noResultsText}</div>
                )}
              </div>
            </>
          ) : (
            <div className="multi-select-chip__empty">{emptyOptionsLabel}</div>
          )}
        </div>
      ) : null}
    </div>
  );
}
