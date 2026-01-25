import { useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';

import type { ApiReviewProjectDetail, ApiReviewProjectTextUnit } from '../../api/review-projects';
import {
  saveReviewProjectTextUnitDecision,
  setReviewProjectTextUnitDecisionState,
} from '../../api/review-projects';
import { checkTextUnitIntegrity, type TextUnitIntegrityCheckResult } from '../../api/text-units';
import {
  REVIEW_PROJECT_DETAIL_QUERY_KEY,
  useReviewProjectDetail,
} from '../../hooks/useReviewProjectDetail';
import type {
  DecisionStateRequest,
  PendingAction,
  PendingValidationSave,
  ReviewProjectMutationControls,
  SaveDecisionRequest,
} from './review-project-mutations';
import { ReviewProjectPageView } from './ReviewProjectPageView';

export function ReviewProjectPage() {
  const { projectId: projectIdParam } = useParams<{ projectId: string }>();

  const projectId = projectIdParam ? Number(projectIdParam) : undefined;
  const projectDetailQuery = useReviewProjectDetail(projectId);
  const queryClient = useQueryClient();
  const actionAttemptRef = useRef(0);
  const [isSaving, setIsSaving] = useState(false);
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

  const executeAction = useCallback(
    async (action: PendingAction, attemptId: number) => {
      if (projectId == null) {
        return;
      }
      if (attemptId !== actionAttemptRef.current) {
        return;
      }
      setIsSaving(true);
      try {
        let updated: ApiReviewProjectTextUnit;
        if (action.kind === 'save-decision') {
          updated = await saveReviewProjectTextUnitDecision({
            textUnitId: action.request.textUnitId,
            target: action.request.target,
            comment: action.request.comment,
            status: action.request.status,
            includedInLocalizedFile: action.request.includedInLocalizedFile,
            expectedCurrentTmTextUnitVariantId: action.request.expectedCurrentTmTextUnitVariantId,
            overrideChangedCurrent: action.request.overrideChangedCurrent,
            decisionNotes: action.request.decisionNotes,
          });
        } else {
          updated = await setReviewProjectTextUnitDecisionState({
            textUnitId: action.request.textUnitId,
            decisionState: action.request.decisionState,
            expectedCurrentTmTextUnitVariantId: action.request.expectedCurrentTmTextUnitVariantId,
            overrideChangedCurrent: action.request.overrideChangedCurrent,
          });
        }

        if (attemptId !== actionAttemptRef.current) {
          return;
        }
        updateTextUnitInCache(updated);
        setErrorMessage(null);
        setConflictTextUnit(null);
        setConflictAction(null);
        setPendingValidationSave(null);
      } catch (error) {
        if (attemptId !== actionAttemptRef.current) {
          return;
        }
        const err = error as Error & {
          status?: number;
          data?: ApiReviewProjectTextUnit | null;
        };
        if (err.status === 409 && err.data) {
          setConflictTextUnit(err.data);
          setConflictAction(action);
          setErrorMessage(null);
        } else {
          setConflictTextUnit(null);
          setConflictAction(null);
          setErrorMessage(err.message || 'Failed to save changes');
        }
      } finally {
        if (attemptId === actionAttemptRef.current) {
          setIsSaving(false);
        }
      }
    },
    [projectId, updateTextUnitInCache],
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
    setIsSaving(false);
    setErrorMessage(null);
    setActiveTextUnitId(null);
    setConflictTextUnit(null);
    setConflictAction(null);
    setPendingValidationSave(null);
  }, [projectId]);

  const mutationControls: ReviewProjectMutationControls = {
    isSaving,
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
  };

  if (!projectId) {
    return <ErrorState message="Missing project id." />;
  }

  if (projectDetailQuery.isLoading) {
    return <LoadingState />;
  }

  if (projectDetailQuery.isError) {
    const message =
      projectDetailQuery.error instanceof Error
        ? projectDetailQuery.error.message
        : 'Failed to load project';
    return <ErrorState message={message} />;
  }

  return (
    <ReviewProjectPageView
      projectId={projectId}
      project={projectDetailQuery.data ?? null}
      mutations={mutationControls}
    />
  );
}

function LoadingState() {
  return (
    <div className="review-project-page__state">
      <div className="spinner spinner--md" aria-hidden />
      <div>Loading project…</div>
    </div>
  );
}

function ErrorState({ message }: { message: string }) {
  return (
    <div className="review-project-page__state review-project-page__state--error">
      <div>{message}</div>
    </div>
  );
}
