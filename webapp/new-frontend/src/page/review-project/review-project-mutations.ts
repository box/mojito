import type { ApiReviewProjectTextUnit } from '../../api/review-projects';

export type SaveDecisionRequest = {
  textUnitId: number;
  tmTextUnitId: number | null;
  target: string;
  comment: string | null;
  status: string;
  includedInLocalizedFile: boolean;
  expectedCurrentTmTextUnitVariantId?: number | null;
  overrideChangedCurrent?: boolean;
  decisionNotes?: string | null;
};

export type DecisionStateRequest = {
  textUnitId: number;
  decisionState: 'PENDING' | 'DECIDED';
  expectedCurrentTmTextUnitVariantId?: number | null;
  overrideChangedCurrent?: boolean;
};

export type PendingAction =
  | { kind: 'save-decision'; request: SaveDecisionRequest }
  | { kind: 'decision-state'; request: DecisionStateRequest };

export type PendingValidationSave = {
  body: string;
  action: PendingAction;
};

export type ReviewProjectMutationControls = {
  isSaving: boolean;
  errorMessage: string | null;
  activeTextUnitId: number | null;
  conflictTextUnit: ApiReviewProjectTextUnit | null;
  showValidationDialog: boolean;
  validationDialogBody: string;
  onConfirmValidationSave: () => void;
  onDismissValidationSave: () => void;
  onUseConflictCurrent: () => void;
  onOverwriteConflict: () => void;
  onRequestSaveDecision: (request: SaveDecisionRequest) => void;
  onRequestDecisionState: (request: DecisionStateRequest) => void;
};
