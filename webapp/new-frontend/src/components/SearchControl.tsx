import './search-control.css';

import React, { useMemo } from 'react';

type Props = {
  value: string;
  onChange: (value: string) => void;
  onSubmit?: () => void;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
  inputAriaLabel?: string;
  leading?: React.ReactNode;
  inputId?: string;
};

export function SearchControl({
  value,
  onChange,
  onSubmit,
  placeholder = 'Search',
  disabled = false,
  className,
  inputAriaLabel,
  leading,
  inputId,
}: Props) {
  const showClear = value.length > 0 && !disabled;
  const hasLeading = Boolean(leading);

  const resolvedClassName = useMemo(
    () =>
      ['search-control', hasLeading ? 'search-control--with-leading' : null, className]
        .filter(Boolean)
        .join(' '),
    [className, hasLeading],
  );

  return (
    <div className={resolvedClassName}>
      <div className="search-control__shell">
        {leading ? <div className="search-control__leading">{leading}</div> : null}
        <form
          className="search-control__form"
          onSubmit={(event) => {
            event.preventDefault();
            onSubmit?.();
          }}
        >
          <input
            id={inputId}
            className="search-control__input"
            type="search"
            value={value}
            placeholder={placeholder}
            onChange={(event) => onChange(event.target.value)}
            disabled={disabled}
            aria-label={inputAriaLabel ?? placeholder}
          />
          {showClear ? (
            <button
              type="button"
              className="search-control__clear"
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
