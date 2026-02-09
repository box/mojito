import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import type {
  ApiReviewProjectDetail,
  ApiReviewProjectStatus,
  ApiReviewProjectTextUnit,
} from '../../api/review-projects';
import {
  saveReviewProjectTextUnitDecision,
  setReviewProjectTextUnitDecisionState,
  updateReviewProjectStatus,
} from '../../api/review-projects';
import { checkTextUnitIntegrity, type TextUnitIntegrityCheckResult } from '../../api/text-units';
import { REVIEW_PROJECT_DETAIL_QUERY_KEY } from '../../hooks/useReviewProjectDetail';
import { REVIEW_PROJECTS_QUERY_KEY } from '../../hooks/useReviewProjects';

export type SaveDecisionRequest = {
  textUnitId: number;
  tmTextUnitId: number | null;
  target: string;
  comment: string | null;
  status: string;
  includedInLocalizedFile: boolean;
  decisionState: 'PENDING' | 'DECIDED';
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
  isProjectStatusSaving: boolean;
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
  onRequestProjectStatus: (status: ApiReviewProjectStatus) => void;
};

type MutationError = Error & { status?: number; data?: ApiReviewProjectTextUnit | null };

export function useReviewProjectMutations(
  projectId: number | undefined,
): ReviewProjectMutationControls {
  const queryClient = useQueryClient();
  const actionAttemptRef = useRef(0);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [activeTextUnitId, setActiveTextUnitId] = useState<number | null>(null);
  const [conflictTextUnit, setConflictTextUnit] = useState<ApiReviewProjectTextUnit | null>(null);
  const [conflictAction, setConflictAction] = useState<PendingAction | null>(null);
  const [pendingValidationSave, setPendingValidationSave] = useState<PendingValidationSave | null>(
    null,
  );

  const updateTextUnitInCache = useCallback(
    (updatedTextUnit: ApiReviewProjectTextUnit) => {
      if (projectId == null) {
        return;
      }
      queryClient.setQueryData<ApiReviewProjectDetail>(
        [...REVIEW_PROJECT_DETAIL_QUERY_KEY, projectId],
        (prev) => {
          if (!prev?.reviewProjectTextUnits) {
            return prev;
          }
          const nextTextUnits = prev.reviewProjectTextUnits.map((tu) =>
            tu.id === updatedTextUnit.id ? updatedTextUnit : tu,
          );
          return { ...prev, reviewProjectTextUnits: nextTextUnits };
        },
      );
    },
    [projectId, queryClient],
  );

  const formatCheckFailureBody = useCallback((result: TextUnitIntegrityCheckResult | null) => {
    const detail = result?.failureDetail?.trim();
    if (detail) {
      return `This translation failed the placeholder/integrity check:\n\n${detail}\n\nDo you want to save it anyway?`;
    }
    return 'This translation failed the placeholder/integrity check. Do you want to save it anyway?';
  }, []);

  const shouldRetry = useCallback((failureCount: number, error: MutationError) => {
    const status = error?.status;
    if (status && status >= 400 && status < 500) {
      return false;
    }
    return failureCount < 2;
  }, []);

  const mutation = useMutation<ApiReviewProjectTextUnit, MutationError, PendingAction>({
    mutationFn: async (action) => {
      if (projectId == null) {
        throw new Error('Missing project id');
      }
      if (action.kind === 'save-decision') {
        return saveReviewProjectTextUnitDecision({
          textUnitId: action.request.textUnitId,
          target: action.request.target,
          comment: action.request.comment,
          status: action.request.status,
          includedInLocalizedFile: action.request.includedInLocalizedFile,
          decisionState: action.request.decisionState,
          expectedCurrentTmTextUnitVariantId: action.request.expectedCurrentTmTextUnitVariantId,
          overrideChangedCurrent: action.request.overrideChangedCurrent,
          decisionNotes: action.request.decisionNotes,
        });
      }
      return setReviewProjectTextUnitDecisionState({
        textUnitId: action.request.textUnitId,
        decisionState: action.request.decisionState,
        expectedCurrentTmTextUnitVariantId: action.request.expectedCurrentTmTextUnitVariantId,
        overrideChangedCurrent: action.request.overrideChangedCurrent,
      });
    },
    retry: shouldRetry,
  });

  const projectStatusMutation = useMutation<ApiReviewProjectDetail, Error, ApiReviewProjectStatus>({
    mutationFn: async (nextStatus) => {
      if (projectId == null) {
        throw new Error('Missing project id');
      }
      return updateReviewProjectStatus(projectId, nextStatus);
    },
    onSuccess: (updatedProject) => {
      if (projectId == null) {
        return;
      }
      queryClient.setQueryData<ApiReviewProjectDetail>(
        [...REVIEW_PROJECT_DETAIL_QUERY_KEY, projectId],
        updatedProject,
      );
      void queryClient.invalidateQueries({ queryKey: [REVIEW_PROJECTS_QUERY_KEY] });
      setErrorMessage(null);
    },
    onError: (error) => {
      setErrorMessage(error.message || 'Failed to update project status');
    },
  });

  const executeAction = useCallback(
    async (action: PendingAction, attemptId: number) => {
      if (projectId == null) {
        return;
      }
      try {
        const updated = await mutation.mutateAsync(action);
        if (attemptId !== actionAttemptRef.current) {
          return;
        }
        updateTextUnitInCache(updated);
        void queryClient.invalidateQueries({ queryKey: [REVIEW_PROJECTS_QUERY_KEY] });
        setErrorMessage(null);
        setConflictTextUnit(null);
        setConflictAction(null);
        setPendingValidationSave(null);
      } catch (error) {
        if (attemptId !== actionAttemptRef.current) {
          return;
        }
        const err = error as MutationError;
        if (err.status === 409 && err.data) {
          setConflictTextUnit(err.data);
          setConflictAction(action);
          setErrorMessage(null);
        } else {
          setConflictTextUnit(null);
          setConflictAction(null);
          setErrorMessage(err.message || 'Failed to save changes');
        }
      }
    },
    [mutation, projectId, queryClient, updateTextUnitInCache],
  );

  const performAction = useCallback(
    (action: PendingAction, skipIntegrityCheck = false) => {
      if (projectId == null) {
        return;
      }
      const attemptId = (actionAttemptRef.current += 1);
      setActiveTextUnitId(action.request.textUnitId);
      setErrorMessage(null);
      setConflictTextUnit(null);
      setConflictAction(null);
      setPendingValidationSave(null);

      if (
        action.kind === 'save-decision' &&
        !skipIntegrityCheck &&
        action.request.tmTextUnitId != null
      ) {
        void checkTextUnitIntegrity({
          tmTextUnitId: action.request.tmTextUnitId,
          content: action.request.target,
        })
          .then((result) => {
            if (attemptId !== actionAttemptRef.current) {
              return;
            }
            if (result?.checkResult === false) {
              setPendingValidationSave({
                body: formatCheckFailureBody(result),
                action,
              });
              return;
            }
            void executeAction(action, attemptId);
          })
          .catch((error: unknown) => {
            if (attemptId !== actionAttemptRef.current) {
              return;
            }
            const message = error instanceof Error ? error.message : 'Unknown error';
            setPendingValidationSave({
              body: `Unable to validate placeholders (${message}). Do you want to save it anyway?`,
              action,
            });
          });
        return;
      }

      void executeAction(action, attemptId);
    },
    [executeAction, formatCheckFailureBody, projectId],
  );

  const onRequestSaveDecision = useCallback(
    (request: SaveDecisionRequest) => {
      performAction({ kind: 'save-decision', request });
    },
    [performAction],
  );

  const onRequestDecisionState = useCallback(
    (request: DecisionStateRequest) => {
      performAction({ kind: 'decision-state', request });
    },
    [performAction],
  );

  const onRequestProjectStatus = useCallback(
    (nextStatus: ApiReviewProjectStatus) => {
      if (projectId == null || projectStatusMutation.isPending) {
        return;
      }
      projectStatusMutation.mutate(nextStatus);
    },
    [projectId, projectStatusMutation],
  );

  const onConfirmValidationSave = useCallback(() => {
    if (!pendingValidationSave) {
      return;
    }
    performAction(pendingValidationSave.action, true);
  }, [pendingValidationSave, performAction]);

  const onDismissValidationSave = useCallback(() => {
    actionAttemptRef.current += 1;
    setPendingValidationSave(null);
  }, []);

  const onUseConflictCurrent = useCallback(() => {
    if (!conflictTextUnit) {
      return;
    }
    updateTextUnitInCache(conflictTextUnit);
    setConflictTextUnit(null);
    setConflictAction(null);
    setErrorMessage(null);
  }, [conflictTextUnit, updateTextUnitInCache]);

  const onOverwriteConflict = useCallback(() => {
    if (!conflictAction || !conflictTextUnit) {
      return;
    }
    const expectedCurrentId = conflictTextUnit.currentTmTextUnitVariant?.id ?? null;
    if (conflictAction.kind === 'save-decision') {
      performAction(
        {
          kind: 'save-decision',
          request: {
            ...conflictAction.request,
            expectedCurrentTmTextUnitVariantId: expectedCurrentId,
            overrideChangedCurrent: true,
          },
        },
        false,
      );
      return;
    }
    performAction({
      kind: 'decision-state',
      request: {
        ...conflictAction.request,
        expectedCurrentTmTextUnitVariantId: expectedCurrentId,
        overrideChangedCurrent: true,
      },
    });
  }, [conflictAction, conflictTextUnit, performAction]);

  useEffect(() => {
    actionAttemptRef.current += 1;
    setErrorMessage(null);
    setActiveTextUnitId(null);
    setConflictTextUnit(null);
    setConflictAction(null);
    setPendingValidationSave(null);
  }, [projectId]);

  return useMemo(
    () => ({
      isSaving: mutation.isPending,
      isProjectStatusSaving: projectStatusMutation.isPending,
      errorMessage,
      activeTextUnitId,
      conflictTextUnit,
      showValidationDialog: pendingValidationSave != null,
      validationDialogBody: pendingValidationSave?.body ?? '',
      onConfirmValidationSave,
      onDismissValidationSave,
      onUseConflictCurrent,
      onOverwriteConflict,
      onRequestSaveDecision,
      onRequestDecisionState,
      onRequestProjectStatus,
    }),
    [
      activeTextUnitId,
      conflictTextUnit,
      errorMessage,
      mutation.isPending,
      onConfirmValidationSave,
      onDismissValidationSave,
      onOverwriteConflict,
      onRequestDecisionState,
      onRequestProjectStatus,
      onRequestSaveDecision,
      onUseConflictCurrent,
      pendingValidationSave,
      projectStatusMutation.isPending,
    ],
  );
}
