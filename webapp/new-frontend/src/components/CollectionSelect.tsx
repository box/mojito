export type CollectionOption = { id: string; name: string; size: number };

type Props = {
  options: CollectionOption[];
  value: string | null;
  onChange: (id: string | null) => void;
  disabled?: boolean;
  className?: string;
  noneLabel?: string;
};

export function CollectionSelect({
  options,
  value,
  onChange,
  disabled = false,
  className,
  noneLabel = '(None)',
}: Props) {
  const resolvedClassName = className ?? 'review-create__select';

  return (
    <select
      className={resolvedClassName}
      value={value ?? ''}
      onChange={(event) => onChange(event.target.value || null)}
      disabled={disabled}
    >
      <option value="">{noneLabel}</option>
      {options.map((opt) => (
        <option key={opt.id} value={opt.id}>
          {String(opt.name)} - {opt.size} ids
        </option>
      ))}
    </select>
  );
}
