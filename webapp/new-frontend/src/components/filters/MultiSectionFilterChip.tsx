import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import { resultSizePresets, WORKSET_SIZE_MIN } from '../../page/workbench/workbench-constants';

export type FilterOption<T extends string | number> = { value: T; label: string; helper?: string };

export type RadioSection<T extends string | number = string> = {
  kind: 'radio';
  label: string;
  options: Array<FilterOption<T>>;
  value: T;
  onChange: (value: T) => void;
};

export type SizeSection = {
  kind: 'size';
  label: string;
  options?: Array<FilterOption<number>>;
  value: number;
  onChange: (value: number) => void;
  min?: number;
};

export type DateQuickRange = {
  label: string;
  after?: string | null;
  before?: string | null;
};

export type DateSection = {
  kind: 'date';
  label: string;
  after?: string | null;
  before?: string | null;
  onChangeAfter?: (value: string | null) => void;
  onChangeBefore?: (value: string | null) => void;
  quickRanges?: DateQuickRange[];
  clearLabel?: string;
  onClear?: () => void;
  afterLabel?: string;
  beforeLabel?: string;
};

export type FilterSection = RadioSection<string | number> | SizeSection | DateSection;

type ResolvedClassNames = {
  button: string;
  panel: string;
  section: string;
  label: string;
  list: string;
  option: string;
  helper: string;
  pills: string;
  quick: string;
  quickChip: string;
  custom: string;
  customLabel: string;
  customInput: string;
  dateInput: string;
  clear: string;
};

export type MultiSectionFilterChipClassNames = Partial<ResolvedClassNames>;

type Props = {
  sections: Array<FilterSection>;
  align?: 'left' | 'right';
  summary?: string;
  ariaLabel?: string;
  className?: string;
  classNames?: MultiSectionFilterChipClassNames;
  disabled?: boolean;
  closeOnSelection?: boolean;
};

const defaultClassNames: ResolvedClassNames = {
  button: 'workbench-filterchip__button',
  panel: 'workbench-filterchip__panel',
  section: 'workbench-searchmode__section',
  label: 'workbench-searchmode__label',
  list: 'workbench-searchmode__list',
  option: 'workbench-searchmode__option',
  helper: 'workbench-searchmode__helper',
  pills: 'workbench-filterchip__pills',
  quick: 'workbench-datefilter__quick',
  quickChip: 'workbench-datefilter__quick-chip',
  custom: 'workbench-worksetcustom',
  customLabel: 'workbench-worksetcustom__label',
  customInput: 'workbench-worksetcustom__input',
  dateInput: 'workbench-datefilter__input',
  clear: 'workbench-filterchip__clear-link',
};

export function MultiSectionFilterChip({
  sections,
  align = 'right',
  summary,
  ariaLabel,
  className,
  classNames,
  disabled = false,
  closeOnSelection = false,
}: Props) {
  const mergedClassNames = useMemo<ResolvedClassNames>(
    () => ({ ...defaultClassNames, ...classNames }),
    [classNames],
  );
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const sizeInputRef = useRef<HTMLInputElement | null>(null);

  const sizeSection = sections.find((section): section is SizeSection => section.kind === 'size');
  const sizeOptions =
    sizeSection?.options && sizeSection.options.length > 0
      ? sizeSection.options
      : resultSizePresets;
  const sizeMin = sizeSection?.min ?? WORKSET_SIZE_MIN;
  const hasSizeSection = sizeSection != null;
  const sizeValue = sizeSection?.value;

  const sizeIsPreset =
    sizeSection != null && sizeOptions.some((option) => option.value === sizeSection.value);
  const [sizeDraft, setSizeDraft] = useState(() => String(sizeValue ?? ''));
  const [showCustomSize, setShowCustomSize] = useState(() => !sizeIsPreset);

  const commitSizeDraft = useCallback(() => {
    if (!sizeSection) return;
    const parsed = parseInt(sizeDraft, 10);
    if (Number.isNaN(parsed) || parsed < sizeMin) {
      setSizeDraft(String(sizeSection.value ?? ''));
      return;
    }
    if (parsed !== sizeSection.value) {
      sizeSection.onChange(parsed);
    }
  }, [sizeDraft, sizeMin, sizeSection]);

  useEffect(() => {
    if (disabled && isOpen) {
      commitSizeDraft();
      setIsOpen(false);
    }
  }, [commitSizeDraft, disabled, isOpen]);

  useEffect(() => {
    if (!isOpen) return;
    const handlePointerDown = (event: PointerEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) {
        commitSizeDraft();
        setIsOpen(false);
      }
    };
    window.addEventListener('pointerdown', handlePointerDown);
    return () => window.removeEventListener('pointerdown', handlePointerDown);
  }, [commitSizeDraft, isOpen]);

  useEffect(() => {
    if (!hasSizeSection) {
      return;
    }
    const nextDraft = String(sizeValue ?? '');
    const nextShowCustomSize = !sizeIsPreset;
    setSizeDraft((prev) => (prev === nextDraft ? prev : nextDraft));
    setShowCustomSize((prev) => (prev === nextShowCustomSize ? prev : nextShowCustomSize));
  }, [hasSizeSection, sizeIsPreset, sizeValue]);

  useEffect(() => {
    if (showCustomSize) {
      queueMicrotask(() => {
        sizeInputRef.current?.focus();
        sizeInputRef.current?.select();
      });
    }
  }, [showCustomSize]);

  const computedSummary = useMemo(() => {
    if (summary) return summary;
    const parts: string[] = [];
    sections.forEach((section) => {
      if (section.kind === 'radio') {
        const selected = section.options.find((opt) => opt.value === section.value);
        if (selected) parts.push(selected.label);
      } else if (section.kind === 'size') {
        const presetLabel = sizeOptions.find((opt) => opt.value === section.value)?.label;
        const label =
          presetLabel ??
          (section.value >= 1000 && section.value % 1000 === 0
            ? `${section.value / 1000}k`
            : String(section.value));
        parts.push(`Size ${label}`);
      } else if (section.kind === 'date') {
        if (section.after || section.before) {
          parts.push(section.label);
        }
      }
    });
    return parts.join(' · ') || 'Filters';
  }, [sections, sizeOptions, summary]);

  const containerClassName = ['chip-dropdown', className].filter(Boolean).join(' ');
  const buttonClassName = ['chip-dropdown__button', mergedClassNames.button]
    .filter(Boolean)
    .join(' ');
  const panelClassName = ['chip-dropdown__panel', mergedClassNames.panel].filter(Boolean).join(' ');

  return (
    <div className={containerClassName} ref={containerRef} data-align={align}>
      <button
        type="button"
        className={buttonClassName}
        onClick={() => {
          if (disabled) return;
          setIsOpen((prev) => !prev);
        }}
        aria-expanded={isOpen}
        aria-label={ariaLabel}
        disabled={disabled}
      >
        <span className="chip-dropdown__summary">{computedSummary}</span>
        <span className="chip-dropdown__chevron" aria-hidden="true" />
      </button>
      {isOpen ? (
        <div className={panelClassName} role="menu">
          {sections.map((section, index) => {
            if (section.kind === 'radio') {
              return (
                <div className={mergedClassNames.section} key={`radio-${index}`}>
                  <div className={mergedClassNames.label}>{section.label}</div>
                  <div className={mergedClassNames.list}>
                    {section.options.map((option) => (
                      <button
                        type="button"
                        key={String(option.value)}
                        className={`${mergedClassNames.option}${
                          option.value === section.value ? ' is-active' : ''
                        }`}
                        onClick={() => {
                          section.onChange(option.value);
                          if (closeOnSelection) {
                            setIsOpen(false);
                          }
                        }}
                      >
                        <span>{option.label}</span>
                        {option.helper ? (
                          <span className={mergedClassNames.helper}>{option.helper}</span>
                        ) : null}
                      </button>
                    ))}
                  </div>
                </div>
              );
            }
            if (section.kind === 'size') {
              return (
                <div className={mergedClassNames.section} key={`size-${index}`}>
                  <div className={mergedClassNames.label}>{section.label}</div>
                  <div className={mergedClassNames.pills}>
                    {sizeOptions.map((option) => (
                      <button
                        type="button"
                        key={option.value}
                        className={`${mergedClassNames.quickChip}${
                          option.value === section.value ? ' is-active' : ''
                        }`}
                        onClick={() => {
                          section.onChange(option.value);
                          setShowCustomSize(false);
                          if (closeOnSelection) {
                            setIsOpen(false);
                          }
                        }}
                      >
                        {option.label}
                      </button>
                    ))}
                    {showCustomSize ? (
                      <div className={`${mergedClassNames.custom} is-active`}>
                        <span className={mergedClassNames.customLabel}>Custom</span>
                        <input
                          ref={sizeInputRef}
                          className={mergedClassNames.customInput}
                          type="number"
                          inputMode="numeric"
                          min={sizeMin}
                          value={sizeDraft}
                          onChange={(event) => setSizeDraft(event.target.value)}
                          onBlur={() => {
                            commitSizeDraft();
                            if (closeOnSelection) {
                              setIsOpen(false);
                            }
                          }}
                          onKeyDown={(event) => {
                            if (event.key === 'Enter') {
                              commitSizeDraft();
                              if (closeOnSelection) {
                                setIsOpen(false);
                              }
                            }
                            if (event.key === 'Escape') {
                              setSizeDraft(String(section.value ?? ''));
                              setShowCustomSize(false);
                            }
                          }}
                        />
                      </div>
                    ) : (
                      <button
                        type="button"
                        className={mergedClassNames.quickChip}
                        onClick={() => {
                          setShowCustomSize(true);
                          setSizeDraft(String(section.value ?? ''));
                        }}
                      >
                        Custom…
                      </button>
                    )}
                  </div>
                </div>
              );
            }
            if (section.kind === 'date') {
              const hasQuickRanges = Boolean(section.quickRanges?.length);
              const showClearButton =
                Boolean(section.onClear) && Boolean(section.after || section.before);
              const afterLabel = section.afterLabel ?? `${section.label} after`;
              const beforeLabel = section.beforeLabel ?? `${section.label} before`;
              return (
                <div className={mergedClassNames.section} key={`date-${index}`}>
                  <div className={mergedClassNames.label}>{section.label}</div>
                  <div className={mergedClassNames.list}>
                    {section.onChangeAfter ? (
                      <input
                        type="datetime-local"
                        value={section.after ? isoToLocalInput(section.after) : ''}
                        onChange={(event) =>
                          section.onChangeAfter?.(
                            event.target.value ? localInputToIso(event.target.value) : null,
                          )
                        }
                        className={mergedClassNames.dateInput}
                        aria-label={afterLabel}
                        title={afterLabel}
                      />
                    ) : null}
                    {section.onChangeBefore ? (
                      <input
                        type="datetime-local"
                        value={section.before ? isoToLocalInput(section.before) : ''}
                        onChange={(event) =>
                          section.onChangeBefore?.(
                            event.target.value ? localInputToIso(event.target.value) : null,
                          )
                        }
                        className={mergedClassNames.dateInput}
                        aria-label={beforeLabel}
                        title={beforeLabel}
                      />
                    ) : null}
                  </div>
                  {hasQuickRanges || showClearButton ? (
                    <div className={mergedClassNames.quick}>
                      {section.quickRanges?.map((range) => (
                        <button
                          type="button"
                          key={range.label}
                          className={mergedClassNames.quickChip}
                          onClick={() => {
                            section.onChangeAfter?.(range.after ?? null);
                            section.onChangeBefore?.(range.before ?? null);
                            if (closeOnSelection) {
                              setIsOpen(false);
                            }
                          }}
                        >
                          {range.label}
                        </button>
                      ))}
                      {showClearButton ? (
                        <button
                          type="button"
                          className={mergedClassNames.clear}
                          onClick={() => {
                            section.onClear?.();
                            if (closeOnSelection) {
                              setIsOpen(false);
                            }
                          }}
                        >
                          {section.clearLabel ?? 'Clear'}
                        </button>
                      ) : null}
                    </div>
                  ) : null}
                </div>
              );
            }
            return null;
          })}
        </div>
      ) : null}
    </div>
  );
}

function isoToLocalInput(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  const tzOffset = date.getTimezoneOffset() * 60000;
  const local = new Date(date.getTime() - tzOffset);
  return local.toISOString().slice(0, 16);
}

function localInputToIso(local: string): string {
  if (!local) return '';
  const date = new Date(local);
  return date.toISOString();
}
