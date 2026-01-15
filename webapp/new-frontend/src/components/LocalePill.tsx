import './pill.css';

import type { HTMLAttributes } from 'react';

import { useLocaleDisplayNameResolver } from '../utils/localeDisplayNames';
import { Pill } from './Pill';

type Props = HTMLAttributes<HTMLSpanElement> & {
  bcp47Tag: string;
  displayName?: string | null;
  labelMode?: 'displayName' | 'tag' | 'both';
};

export function LocalePill({
  bcp47Tag,
  displayName,
  labelMode = 'displayName',
  className,
  ...rest
}: Props) {
  const resolveDisplayName = useLocaleDisplayNameResolver();
  const resolvedDisplayName = displayName || resolveDisplayName(bcp47Tag) || '';

  const fallbackLabel = bcp47Tag || 'Unknown locale';
  const label =
    labelMode === 'tag'
      ? fallbackLabel
      : labelMode === 'both'
        ? resolvedDisplayName
          ? `${resolvedDisplayName} (${fallbackLabel})`
          : fallbackLabel
        : resolvedDisplayName || fallbackLabel;

  const resolvedClassName = ['pill', className].filter(Boolean).join(' ');

  return (
    <Pill className={resolvedClassName} title={fallbackLabel} {...rest}>
      {label}
    </Pill>
  );
}
