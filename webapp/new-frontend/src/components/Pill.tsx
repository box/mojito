import './pill.css';

import type { HTMLAttributes, ReactNode } from 'react';

type Props = HTMLAttributes<HTMLSpanElement> & {
  children: ReactNode;
};

export function Pill({ children, className, ...rest }: Props) {
  const resolvedClassName = ['pill', className].filter(Boolean).join(' ');
  return (
    <span className={resolvedClassName} {...rest}>
      {children}
    </span>
  );
}
