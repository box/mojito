import './chip-dropdown.css';
import './pill.css';

import { type ReactNode, useCallback, useEffect, useRef, useState } from 'react';

type PillDropdownOption<T extends string | number> = {
  value: T;
  label: ReactNode;
};

type Props<T extends string | number> = {
  value: T;
  options: Array<PillDropdownOption<T>>;
  onChange: (value: T) => void;
  disabled?: boolean;
  ariaLabel?: string;
  isOpen?: boolean;
  onOpenChange?: (open: boolean) => void;
  className?: string;
  panelClassName?: string;
};

export function PillDropdown<T extends string | number>({
  value,
  options,
  onChange,
  disabled,
  ariaLabel,
  isOpen,
  onOpenChange,
  className,
  panelClassName,
}: Props<T>) {
  const [internalOpen, setInternalOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const controlled = typeof isOpen === 'boolean';
  const resolvedOpen = controlled ? isOpen : internalOpen;

  const setOpen = useCallback(
    (next: boolean) => {
      if (!controlled) {
        setInternalOpen(next);
      }
      onOpenChange?.(next);
    },
    [controlled, onOpenChange],
  );

  useEffect(() => {
    if (!resolvedOpen) {
      return;
    }
    const handlePointerDown = (event: PointerEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    window.addEventListener('pointerdown', handlePointerDown);
    return () => window.removeEventListener('pointerdown', handlePointerDown);
  }, [resolvedOpen, setOpen]);

  useEffect(() => {
    if (disabled && resolvedOpen) {
      setOpen(false);
    }
  }, [disabled, resolvedOpen, setOpen]);

  const selected = options.find((option) => option.value === value);
  const label = selected ? selected.label : String(value);

  const resolvedClassName = ['pill-dropdown', className].filter(Boolean).join(' ');
  const resolvedPanelClassName = ['chip-dropdown__panel', 'pill-dropdown__panel', panelClassName]
    .filter(Boolean)
    .join(' ');

  return (
    <div className={resolvedClassName} ref={containerRef}>
      <button
        type="button"
        className="pill-dropdown__button"
        onClick={() => setOpen(!resolvedOpen)}
        aria-expanded={resolvedOpen}
        aria-label={ariaLabel}
        disabled={disabled}
      >
        <span className="pill-dropdown__label">{label}</span>
        <span className="pill-dropdown__chevron" aria-hidden="true" />
      </button>
      {resolvedOpen ? (
        <div className={resolvedPanelClassName} role="menu">
          <div className="pill-dropdown__list">
            {options.map((option) => (
              <button
                type="button"
                key={String(option.value)}
                className={`pill-dropdown__option${option.value === value ? ' is-active' : ''}`}
                onClick={() => {
                  setOpen(false);
                  onChange(option.value);
                }}
              >
                <span>{option.label}</span>
              </button>
            ))}
          </div>
        </div>
      ) : null}
    </div>
  );
}
