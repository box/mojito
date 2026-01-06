import './modal.css';

import type { ReactNode } from 'react';

export type ModalSize = 'sm' | 'md' | 'lg' | 'xl';

type Props = {
  open: boolean;
  children: ReactNode;
  size?: ModalSize;
  role?: 'dialog' | 'alertdialog';
  ariaLabel?: string;
  onClose?: () => void;
  closeOnBackdrop?: boolean;
  className?: string;
};

export function Modal({
  open,
  children,
  size = 'md',
  role = 'dialog',
  ariaLabel,
  onClose,
  closeOnBackdrop = false,
  className,
}: Props) {
  if (!open) {
    return null;
  }

  const resolvedClassName = ['modal', `modal--${size}`, className].filter(Boolean).join(' ');
  const handleBackdropClick = () => {
    if (closeOnBackdrop && onClose) {
      onClose();
    }
  };

  return (
    <div className={resolvedClassName} role={role} aria-modal="true" aria-label={ariaLabel}>
      <div className="modal__backdrop" onClick={handleBackdropClick} aria-hidden="true" />
      <div className="modal__card">{children}</div>
    </div>
  );
}
