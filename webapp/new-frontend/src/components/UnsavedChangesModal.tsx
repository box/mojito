import './unsaved-changes-modal.css';

type Props = {
  open: boolean;
  title: string;
  body: string;
  confirmLabel: string;
  cancelLabel: string;
  onConfirm: () => void;
  onCancel: () => void;
};

export function UnsavedChangesModal({
  open,
  title,
  body,
  confirmLabel,
  cancelLabel,
  onConfirm,
  onCancel,
}: Props) {
  if (!open) {
    return null;
  }

  return (
    <div className="unsaved-changes-modal" role="alertdialog" aria-modal="true">
      <div className="unsaved-changes-modal__backdrop" aria-hidden="true" />
      <div className="unsaved-changes-modal__card">
        <div className="unsaved-changes-modal__title">{title}</div>
        <div className="unsaved-changes-modal__body">{body}</div>
        <div className="unsaved-changes-modal__actions">
          <button
            type="button"
            className="unsaved-changes-modal__button unsaved-changes-modal__button--danger"
            onClick={onConfirm}
          >
            {confirmLabel}
          </button>
          <button type="button" className="unsaved-changes-modal__button" onClick={onCancel}>
            {cancelLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
