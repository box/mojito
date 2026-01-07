import { Modal } from './Modal';

type Props = {
  open: boolean;
  title: string;
  body: string;
  confirmLabel: string;
  cancelLabel: string;
  onConfirm: () => void;
  onCancel: () => void;
};

export function ConfirmModal({
  open,
  title,
  body,
  confirmLabel,
  cancelLabel,
  onConfirm,
  onCancel,
}: Props) {
  return (
    <Modal open={open} size="sm" role="alertdialog" ariaLabel={title}>
      <div className="modal__title">{title}</div>
      <div className="modal__body">{body}</div>
      <div className="modal__actions">
        <button type="button" className="modal__button modal__button--danger" onClick={onConfirm}>
          {confirmLabel}
        </button>
        <button type="button" className="modal__button" onClick={onCancel}>
          {cancelLabel}
        </button>
      </div>
    </Modal>
  );
}
