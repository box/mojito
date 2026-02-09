import './chip-dropdown.css';
import './multi-select-chip.css';
import './single-select-dropdown.css';

import { useEffect, useMemo, useRef, useState } from 'react';

export type SingleSelectOption<T extends string | number> = {
  value: T;
  label: string;
};

type Props<T extends string | number> = {
  label: string;
  options: Array<SingleSelectOption<T>>;
  value: T | null;
  onChange: (next: T | null) => void;
  placeholder?: string;
  className?: string;
  disabled?: boolean;
  align?: 'left' | 'right';
  buttonAriaLabel?: string;
  searchPlaceholder?: string;
  noResultsLabel?: string;
  noneLabel?: string;
  searchable?: boolean;
  getOptionClassName?: (option: SingleSelectOption<T>) => string | undefined;
};

export function SingleSelectDropdown<T extends string | number>({
  label,
  options,
  value,
  onChange,
  placeholder,
  className,
  disabled = false,
  align = 'left',
  buttonAriaLabel,
  searchPlaceholder,
  noResultsLabel,
  noneLabel,
  searchable = true,
  getOptionClassName,
}: Props<T>) {
  const [isOpen, setIsOpen] = useState(false);
  const [filterQuery, setFilterQuery] = useState('');
  const containerRef = useRef<HTMLDivElement | null>(null);

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

  const normalizedOptions = useMemo(
    () => options.map((option) => ({ ...option, label: String(option.label) })),
    [options],
  );
  const selectedOption = normalizedOptions.find((option) => option.value === value) ?? null;
  const resolvedPlaceholder = placeholder ?? label;
  const summary = selectedOption ? selectedOption.label : resolvedPlaceholder;
  const isPlaceholder = !selectedOption;
  const isDisabled = disabled || (options.length === 0 && value === null);

  const visibleOptions = useMemo(() => {
    const query = filterQuery.trim().toLowerCase();
    if (!query) {
      return normalizedOptions;
    }
    return normalizedOptions.filter((option) => option.label.toLowerCase().includes(query));
  }, [filterQuery, normalizedOptions]);

  const filterInputPlaceholder = searchPlaceholder ?? `Filter ${label.toLowerCase()}`;
  const emptyLabel = noResultsLabel ?? 'No matches';

  return (
    <div
      className={['chip-dropdown', className].filter(Boolean).join(' ')}
      ref={containerRef}
      data-align={align === 'right' ? 'right' : undefined}
    >
      <button
        type="button"
        className="chip-dropdown__button"
        onClick={() => setIsOpen((previous) => !previous)}
        disabled={isDisabled}
        aria-expanded={isOpen}
        aria-label={buttonAriaLabel ?? label}
      >
        <span className={`chip-dropdown__summary${isPlaceholder ? ' is-placeholder' : ''}`}>
          {summary}
        </span>
        <span className="chip-dropdown__chevron" aria-hidden="true" />
      </button>
      {isOpen ? (
        <div className="chip-dropdown__panel" role="menu">
          {normalizedOptions.length || noneLabel ? (
            <>
              {normalizedOptions.length && searchable ? (
                <input
                  type="search"
                  value={filterQuery}
                  onChange={(event) => setFilterQuery(event.target.value)}
                  placeholder={filterInputPlaceholder}
                  className="multi-select-chip__search"
                />
              ) : null}
              <div className="single-select-dropdown__options">
                {noneLabel ? (
                  <button
                    type="button"
                    className={`single-select-dropdown__option${
                      value === null ? ' is-selected' : ''
                    }`}
                    onClick={() => {
                      onChange(null);
                      setIsOpen(false);
                    }}
                  >
                    {noneLabel}
                  </button>
                ) : null}
                {normalizedOptions.length ? (
                  visibleOptions.length ? (
                    visibleOptions.map((option) => (
                      <button
                        type="button"
                        key={String(option.value)}
                        className={[
                          'single-select-dropdown__option',
                          option.value === value ? 'is-selected' : '',
                          getOptionClassName?.(option) ?? '',
                        ]
                          .filter(Boolean)
                          .join(' ')}
                        onClick={() => {
                          onChange(option.value);
                          setIsOpen(false);
                        }}
                      >
                        {option.label}
                      </button>
                    ))
                  ) : (
                    <div className="single-select-dropdown__empty">{emptyLabel}</div>
                  )
                ) : (
                  <div className="single-select-dropdown__empty">{resolvedPlaceholder}</div>
                )}
              </div>
            </>
          ) : (
            <div className="single-select-dropdown__empty">{resolvedPlaceholder}</div>
          )}
        </div>
      ) : null}
    </div>
  );
}
