import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom';

import {
  type AiReviewMessage,
  type AiReviewSuggestion,
  requestAiReview,
} from '../../api/ai-review';
import {
  type ApiGitBlameWithUsage,
  type ApiTextUnitHistoryItem,
  checkTextUnitIntegrity,
  deleteTextUnitCurrentVariant,
  fetchGitBlameWithUsages,
  fetchTextUnitHistory,
  saveTextUnit,
  type SaveTextUnitRequest,
  searchTextUnits,
  type TextUnitIntegrityCheckResult,
  type TextUnitSearchRequest,
} from '../../api/text-units';
import { useUser } from '../../components/RequireUser';
import { canEditLocale as canEditLocaleForUser } from '../../utils/permissions';
import { formatStatus, mapUiStatusToApi } from '../workbench/workbench-helpers';
import {
  type TextUnitDetailAiMessage,
  type TextUnitDetailHistoryComment,
  type TextUnitDetailHistoryRow,
  type TextUnitDetailMetaRow,
  type TextUnitDetailMetaSection,
  TextUnitDetailPageView,
} from './TextUnitDetailPageView';

type LocationState = {
  from?: string;
  workbenchSearch?: TextUnitSearchRequest | null;
};

const editorStatusOptions = ['Accepted', 'To review', 'To translate', 'Rejected'];
const DEFAULT_AI_REVIEW_PROMPT = 'Review the translation and suggest improvements.';

export function TextUnitDetailPage() {
  const { tmTextUnitId: tmTextUnitIdParam } = useParams<{ tmTextUnitId: string }>();
  const parsedTmTextUnitId = tmTextUnitIdParam ? Number(tmTextUnitIdParam) : NaN;
  const tmTextUnitId =
    Number.isFinite(parsedTmTextUnitId) && parsedTmTextUnitId > 0 ? parsedTmTextUnitId : null;

  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const locationState = (location.state as LocationState | null) ?? null;
  const currentUser = useUser();
  const queryClient = useQueryClient();

  const [isHistoryCollapsed, setIsHistoryCollapsed] = useState(true);
  const [isMetaCollapsed, setIsMetaCollapsed] = useState(true);
  const [isIcuPreviewCollapsed, setIsIcuPreviewCollapsed] = useState(true);
  const [icuPreviewMode, setIcuPreviewMode] = useState<'source' | 'target'>('target');
  const [isAiCollapsed, setIsAiCollapsed] = useState(false);

  const [draftTarget, setDraftTarget] = useState('');
  const [baselineTarget, setBaselineTarget] = useState('');
  const [draftStatus, setDraftStatus] = useState<
    'Accepted' | 'To review' | 'To translate' | 'Rejected'
  >('To translate');
  const [baselineStatus, setBaselineStatus] = useState<
    'Accepted' | 'To review' | 'To translate' | 'Rejected'
  >('To translate');
  const [saveErrorMessage, setSaveErrorMessage] = useState<string | null>(null);
  const [pendingValidationSave, setPendingValidationSave] = useState<{
    request: SaveTextUnitRequest;
    body: string;
  } | null>(null);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);

  const [aiMessages, setAiMessages] = useState<TextUnitDetailAiMessage[]>([]);
  const [aiInput, setAiInput] = useState('');
  const [isAiResponding, setIsAiResponding] = useState(false);

  const localeTag = useMemo(() => searchParams.get('locale')?.trim() ?? null, [searchParams]);

  const textUnitQuery = useQuery({
    queryKey: ['text-unit-detail', tmTextUnitId, localeTag],
    enabled: tmTextUnitId !== null && Boolean(localeTag),
    staleTime: 30_000,
    refetchOnWindowFocus: false,
    queryFn: async () => {
      if (tmTextUnitId === null || !localeTag) {
        return null;
      }

      const results = await searchTextUnits({
        repositoryIds: [],
        localeTags: [localeTag],
        searchAttribute: 'tmTextUnitIds',
        searchType: 'exact',
        searchText: String(tmTextUnitId),
        limit: 5,
        offset: 0,
      });

      return results.find((item) => item.targetLocale === localeTag) ?? results[0] ?? null;
    },
  });

  const historyQuery = useQuery({
    queryKey: ['text-unit-history', tmTextUnitId, localeTag],
    enabled: tmTextUnitId !== null && Boolean(localeTag),
    staleTime: 30_000,
    refetchOnWindowFocus: false,
    queryFn: () => {
      if (tmTextUnitId === null || !localeTag) {
        return Promise.resolve([] as ApiTextUnitHistoryItem[]);
      }
      return fetchTextUnitHistory(tmTextUnitId, localeTag);
    },
  });

  const gitBlameQuery = useQuery({
    queryKey: ['text-unit-git-blame', tmTextUnitId],
    enabled: tmTextUnitId !== null,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
    queryFn: () => {
      if (tmTextUnitId === null) {
        return Promise.resolve([] as ApiGitBlameWithUsage[]);
      }
      return fetchGitBlameWithUsages(tmTextUnitId);
    },
  });

  const saveMutation = useMutation({
    mutationFn: (request: SaveTextUnitRequest) => saveTextUnit(request),
    onSuccess: (saved, request) => {
      const nextTarget = saved.target ?? request.target;
      const nextStatus = normalizeEditorStatus(
        formatStatus(saved.status ?? request.status, saved.includedInLocalizedFile),
      );

      setBaselineTarget(nextTarget);
      setDraftTarget(nextTarget);
      setBaselineStatus(nextStatus);
      setDraftStatus(nextStatus);
      setSaveErrorMessage(null);
      setPendingValidationSave(null);
      setIsHistoryCollapsed(false);

      void queryClient.invalidateQueries({
        queryKey: ['text-unit-detail', tmTextUnitId, localeTag],
      });
      void queryClient.invalidateQueries({
        queryKey: ['text-unit-history', tmTextUnitId, localeTag],
      });
      void queryClient.invalidateQueries({ queryKey: ['workbench-search'] });
    },
    onError: (error: unknown) => {
      const status = (error as { status?: number })?.status;
      if (status === 403) {
        setSaveErrorMessage('You cannot edit this locale.');
        return;
      }
      setSaveErrorMessage(error instanceof Error ? error.message : 'Unable to save translation.');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (textUnitCurrentVariantId: number) =>
      deleteTextUnitCurrentVariant(textUnitCurrentVariantId),
    onSuccess: () => {
      setBaselineTarget('');
      setDraftTarget('');
      setBaselineStatus('To translate');
      setDraftStatus('To translate');
      setSaveErrorMessage(null);
      setPendingValidationSave(null);
      setShowDeleteDialog(false);
      setIsHistoryCollapsed(false);

      void queryClient.invalidateQueries({
        queryKey: ['text-unit-detail', tmTextUnitId, localeTag],
      });
      void queryClient.invalidateQueries({
        queryKey: ['text-unit-history', tmTextUnitId, localeTag],
      });
      void queryClient.invalidateQueries({ queryKey: ['workbench-search'] });
    },
    onError: (error: unknown) => {
      setShowDeleteDialog(false);
      const status = (error as { status?: number })?.status;
      if (status === 403) {
        setSaveErrorMessage('You cannot delete this locale translation.');
        return;
      }
      setSaveErrorMessage(
        error instanceof Error ? error.message : 'Unable to delete translation.',
      );
    },
  });

  const activeTextUnit = textUnitQuery.data;
  const localeForEditing = localeTag ?? activeTextUnit?.targetLocale ?? null;
  const canEdit = localeForEditing ? canEditLocaleForUser(currentUser, localeForEditing) : false;

  const editorSeedKey = useMemo(() => {
    if (!activeTextUnit) {
      return null;
    }
    const status = normalizeEditorStatus(
      formatStatus(activeTextUnit.status, activeTextUnit.includedInLocalizedFile),
    );
    const variantId =
      activeTextUnit.tmTextUnitVariantId ?? activeTextUnit.tmTextUnitCurrentVariantId;
    return [
      activeTextUnit.tmTextUnitId,
      localeForEditing ?? '',
      variantId ?? 'none',
      activeTextUnit.target ?? '',
      status,
    ].join(':');
  }, [activeTextUnit, localeForEditing]);

  useEffect(() => {
    if (!activeTextUnit || editorSeedKey === null) {
      return;
    }

    const nextTarget = activeTextUnit.target ?? '';
    const nextStatus = normalizeEditorStatus(
      formatStatus(activeTextUnit.status, activeTextUnit.includedInLocalizedFile),
    );

    setBaselineTarget(nextTarget);
    setDraftTarget(nextTarget);
    setBaselineStatus(nextStatus);
    setDraftStatus(nextStatus);
    setSaveErrorMessage(null);
    setPendingValidationSave(null);
  }, [
    activeTextUnit,
    activeTextUnit?.includedInLocalizedFile,
    activeTextUnit?.status,
    activeTextUnit?.target,
    editorSeedKey,
  ]);

  const aiContextKey = useMemo(() => {
    if (!activeTextUnit || !localeForEditing) {
      return null;
    }
    const variantId =
      activeTextUnit.tmTextUnitVariantId ?? activeTextUnit.tmTextUnitCurrentVariantId;
    return `${activeTextUnit.tmTextUnitId}:${localeForEditing}:${variantId ?? 'none'}`;
  }, [activeTextUnit, localeForEditing]);

  useEffect(() => {
    if (!activeTextUnit || !localeForEditing || aiContextKey === null) {
      setAiMessages([]);
      setAiInput('');
      setIsAiResponding(false);
      return;
    }

    let cancelled = false;
    setAiMessages([]);
    setAiInput('');
    setIsAiResponding(true);

    const initialMessage: AiReviewMessage = {
      role: 'user',
      content: DEFAULT_AI_REVIEW_PROMPT,
    };

    void requestAiReview({
      source: activeTextUnit.source ?? '',
      target: activeTextUnit.target ?? '',
      localeTag: localeForEditing,
      messages: [initialMessage],
    })
      .then((response) => {
        if (cancelled) {
          return;
        }

        setAiMessages([
          {
            id: `assistant-${Date.now()}`,
            sender: 'assistant',
            content: response.message.content,
            suggestions: response.suggestions,
            review: response.review,
          },
        ]);
      })
      .catch((error: unknown) => {
        if (cancelled) {
          return;
        }

        const message = error instanceof Error ? error.message : 'Unable to fetch AI suggestions.';
        setAiMessages([
          {
            id: `assistant-error-${Date.now()}`,
            sender: 'assistant',
            content: message,
          },
        ]);
      })
      .finally(() => {
        if (!cancelled) {
          setIsAiResponding(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [activeTextUnit, aiContextKey, localeForEditing]);

  const sortedHistoryItems = useMemo(() => {
    return [...(historyQuery.data ?? [])].sort((a, b) => {
      const dateDelta = safeDateValue(b.createdDate) - safeDateValue(a.createdDate);
      if (dateDelta !== 0) {
        return dateDelta;
      }
      return (b.id ?? 0) - (a.id ?? 0);
    });
  }, [historyQuery.data]);

  const gitBlame = useMemo(() => gitBlameQuery.data?.[0] ?? null, [gitBlameQuery.data]);

  const textUnitLocation = useMemo(() => {
    const usagesFromGitBlame = gitBlame?.usages ?? [];
    if (usagesFromGitBlame.length > 0) {
      return usagesFromGitBlame.join(', ');
    }

    const usages =
      textUnitQuery.data?.assetTextUnitUsages
        ?.split(',')
        .map((value) => value.trim())
        .filter(Boolean) ?? [];
    return usages.length > 0 ? usages.join(', ') : null;
  }, [gitBlame?.usages, textUnitQuery.data?.assetTextUnitUsages]);

  const metaSections = useMemo<TextUnitDetailMetaSection[]>(() => {
    const textUnitRows: TextUnitDetailMetaRow[] = [
      { label: 'Repository', value: formatValue(activeTextUnit?.repositoryName) },
      { label: 'Locale', value: formatValue(localeForEditing) },
      { label: 'Created', value: formatDateTime(textUnitQuery.data?.tmTextUnitCreatedDate) },
      { label: 'Translated', value: formatDateTime(textUnitQuery.data?.createdDate) },
      { label: 'AssetPath', value: formatValue(textUnitQuery.data?.assetPath) },
      { label: 'PluralForm', value: formatValue(textUnitQuery.data?.pluralForm) },
      { label: 'PluralFormOther', value: formatValue(textUnitQuery.data?.pluralFormOther) },
      { label: 'TargetComment', value: formatValue(textUnitQuery.data?.targetComment) },
      { label: 'Location', value: formatValue(textUnitLocation) },
      {
        label: 'Third party TMS',
        value: formatValue(gitBlame?.thirdPartyTextUnitId ? 'See in Phrase' : null),
      },
    ];

    const gitBlameRows: TextUnitDetailMetaRow[] = [
      { label: 'Author', value: formatValue(gitBlame?.gitBlame?.authorName) },
      { label: 'Email', value: formatValue(gitBlame?.gitBlame?.authorEmail) },
      { label: 'Commit', value: formatValue(gitBlame?.gitBlame?.commitName) },
      { label: 'Commit date', value: formatGitCommitTime(gitBlame?.gitBlame?.commitTime) },
    ];

    const moreRows: TextUnitDetailMetaRow[] = [
      { label: 'Virtual', value: formatBoolean(gitBlame?.isVirtual) },
      { label: 'TmTextUnitId', value: formatNumberish(textUnitQuery.data?.tmTextUnitId) },
      {
        label: 'TmTextUnitVariantId',
        value: formatNumberish(textUnitQuery.data?.tmTextUnitVariantId),
      },
      {
        label: 'TmTextUnitCurrentVariantId',
        value: formatNumberish(textUnitQuery.data?.tmTextUnitCurrentVariantId),
      },
      { label: 'AssetTextUnitId', value: formatNumberish(textUnitQuery.data?.assetTextUnitId) },
      { label: 'ThirdPartyTMSId', value: formatValue(gitBlame?.thirdPartyTextUnitId) },
      { label: 'AssetId', value: formatNumberish(textUnitQuery.data?.assetId) },
      {
        label: 'LastSuccessfulAssetExtractionId',
        value: formatNumberish(textUnitQuery.data?.lastSuccessfulAssetExtractionId),
      },
      { label: 'AssetExtractionId', value: formatNumberish(textUnitQuery.data?.assetExtractionId) },
      { label: 'Branch', value: formatValue(gitBlame?.branch?.name) },
      { label: 'Screenshots', value: formatScreenshots(gitBlame?.screenshots) },
    ];

    return [
      { title: 'Text unit', rows: textUnitRows },
      { title: 'Git blame', rows: gitBlameRows },
      { title: 'More', rows: moreRows },
    ];
  }, [
    activeTextUnit?.repositoryName,
    gitBlame,
    localeForEditing,
    textUnitLocation,
    textUnitQuery.data,
  ]);

  const historyRows = useMemo<TextUnitDetailHistoryRow[]>(() => {
    return sortedHistoryItems.map((item) => {
      const comments: TextUnitDetailHistoryComment[] = (item.tmTextUnitVariantComments ?? []).map(
        (comment, index) => ({
          key:
            comment.id != null
              ? String(comment.id)
              : `${comment.type ?? ''}-${comment.severity ?? ''}-${comment.content ?? ''}-${index}`,
          type: formatValue(comment.type),
          severity: formatValue(comment.severity),
          content: formatValue(comment.content),
        }),
      );

      return {
        key: String(item.id),
        variantId: String(item.id),
        userName: formatValue(item.createdByUser?.username ?? '<no user>'),
        translation: formatHistoryTranslation(item.content),
        date: formatDateTime(item.createdDate),
        status: formatValue(formatHistoryStatus(item.status, item.includedInLocalizedFile)),
        comments,
      };
    });
  }, [sortedHistoryItems]);

  const handleBack = () => {
    if (locationState?.workbenchSearch) {
      void navigate('/workbench', {
        state: { workbenchSearch: locationState.workbenchSearch },
      });
      return;
    }

    if (window.history.length > 1) {
      void navigate(-1);
      return;
    }
    void navigate(locationState?.from ?? '/workbench');
  };

  const isEditorDirty = draftTarget !== baselineTarget || draftStatus !== baselineStatus;
  const hasCurrentTranslation = typeof activeTextUnit?.tmTextUnitVariantId === 'number';
  const showDeletedHistoryEntry = !hasCurrentTranslation && historyRows.length > 1;
  const canDeleteCurrentTranslation =
    canEdit &&
    hasCurrentTranslation &&
    typeof activeTextUnit?.tmTextUnitCurrentVariantId === 'number';

  const buildSaveRequest = useCallback(
    (targetValue: string): SaveTextUnitRequest | null => {
      if (!activeTextUnit) {
        setSaveErrorMessage('Text unit is still loading.');
        return null;
      }

      if (typeof activeTextUnit.localeId !== 'number') {
        setSaveErrorMessage('Unable to save: missing locale id.');
        return null;
      }

      const statusUpdate = mapUiStatusToApi(draftStatus);
      if (!statusUpdate || !statusUpdate.status) {
        setSaveErrorMessage('Unable to save: invalid status selection.');
        return null;
      }

      return {
        tmTextUnitId: activeTextUnit.tmTextUnitId,
        localeId: activeTextUnit.localeId,
        target: targetValue,
        status: statusUpdate.status,
        includedInLocalizedFile: statusUpdate.includedInLocalizedFile,
      };
    },
    [activeTextUnit, draftStatus],
  );

  const saveDraft = useCallback(
    async (targetOverride?: string) => {
      if (!canEdit) {
        setSaveErrorMessage('You cannot edit this locale.');
        return;
      }

      const nextTarget = targetOverride ?? draftTarget;
      const hasChanges = nextTarget !== baselineTarget || draftStatus !== baselineStatus;
      if (!hasChanges) {
        return;
      }

      const request = buildSaveRequest(nextTarget);
      if (!request) {
        return;
      }

      setSaveErrorMessage(null);

      let integrityResult: TextUnitIntegrityCheckResult;
      try {
        integrityResult = await checkTextUnitIntegrity({
          tmTextUnitId: request.tmTextUnitId,
          content: request.target,
        });
      } catch (error: unknown) {
        const message = error instanceof Error ? error.message : 'Unknown error';
        setPendingValidationSave({
          request,
          body: `Unable to validate placeholders (${message}). Do you want to save it anyway?`,
        });
        return;
      }

      if (integrityResult?.checkResult === false) {
        setPendingValidationSave({
          request,
          body: formatCheckFailureBody(integrityResult),
        });
        return;
      }

      await saveMutation.mutateAsync(request);
    },
    [
      baselineStatus,
      baselineTarget,
      buildSaveRequest,
      canEdit,
      draftStatus,
      draftTarget,
      saveMutation,
    ],
  );

  const handleSaveEditor = useCallback(() => {
    void saveDraft();
  }, [saveDraft]);

  const handleResetEditor = useCallback(() => {
    setDraftTarget(baselineTarget);
    setDraftStatus(baselineStatus);
    setSaveErrorMessage(null);
    setPendingValidationSave(null);
  }, [baselineStatus, baselineTarget]);

  const handleRequestDeleteEditor = useCallback(() => {
    if (!canDeleteCurrentTranslation || deleteMutation.isPending || saveMutation.isPending) {
      return;
    }
    setPendingValidationSave(null);
    setSaveErrorMessage(null);
    setShowDeleteDialog(true);
  }, [canDeleteCurrentTranslation, deleteMutation.isPending, saveMutation.isPending]);

  const handleConfirmDeleteEditor = useCallback(() => {
    if (!canDeleteCurrentTranslation || !hasCurrentTranslation) {
      setShowDeleteDialog(false);
      return;
    }

    const currentVariantId = activeTextUnit?.tmTextUnitCurrentVariantId;
    if (typeof currentVariantId !== 'number') {
      setShowDeleteDialog(false);
      setSaveErrorMessage('No current translation to delete.');
      return;
    }

    setSaveErrorMessage(null);
    void deleteMutation.mutateAsync(currentVariantId);
  }, [
    activeTextUnit?.tmTextUnitCurrentVariantId,
    canDeleteCurrentTranslation,
    deleteMutation,
    hasCurrentTranslation,
  ]);

  const handleDismissDeleteDialog = useCallback(() => {
    if (deleteMutation.isPending) {
      return;
    }
    setShowDeleteDialog(false);
  }, [deleteMutation.isPending]);

  const handleConfirmValidationSave = useCallback(() => {
    if (!pendingValidationSave) {
      return;
    }

    const request = pendingValidationSave.request;
    setPendingValidationSave(null);
    setSaveErrorMessage(null);
    void saveMutation.mutateAsync(request);
  }, [pendingValidationSave, saveMutation]);

  const handleDismissValidationDialog = useCallback(() => {
    setPendingValidationSave(null);
  }, []);

  const handleSubmitAi = useCallback(() => {
    if (isAiResponding || !activeTextUnit || !localeForEditing) {
      return;
    }

    const trimmed = aiInput.trim();
    if (!trimmed) {
      return;
    }

    const userMessage: TextUnitDetailAiMessage = {
      id: `user-${Date.now()}`,
      sender: 'user',
      content: trimmed,
    };

    const baseMessages = [...aiMessages];
    setAiMessages((previous) => [...previous, userMessage]);
    setAiInput('');
    setIsAiResponding(true);

    void (async () => {
      try {
        const conversation: AiReviewMessage[] = [...baseMessages, userMessage].map((message) => ({
          role: message.sender,
          content: message.content,
        }));

        const response = await requestAiReview({
          source: activeTextUnit.source ?? '',
          target: draftTarget,
          localeTag: localeForEditing,
          messages: conversation,
        });

        const assistantMessage: TextUnitDetailAiMessage = {
          id: `assistant-${Date.now()}`,
          sender: 'assistant',
          content: response.message.content,
          suggestions: response.suggestions,
          review: response.review,
        };

        setAiMessages((previous) => [...previous, assistantMessage]);
      } catch (error: unknown) {
        const message = error instanceof Error ? error.message : 'Unable to fetch AI suggestions.';
        setAiMessages((previous) => [
          ...previous,
          {
            id: `assistant-error-${Date.now()}`,
            sender: 'assistant',
            content: message,
          },
        ]);
      } finally {
        setIsAiResponding(false);
      }
    })();
  }, [activeTextUnit, aiInput, aiMessages, draftTarget, isAiResponding, localeForEditing]);

  const handleUseAiSuggestion = useCallback((suggestion: AiReviewSuggestion) => {
    setDraftTarget(suggestion.content);
    setSaveErrorMessage(null);
  }, []);

  const editorWarningMessage =
    !localeForEditing || !activeTextUnit
      ? 'Missing locale. Open this page from a workbench row to enable editing.'
      : !canEdit
        ? 'You do not have permission to edit this locale.'
        : null;

  if (tmTextUnitId === null) {
    return (
      <div className="review-project-page__state review-project-page__state--error">
        <div>Missing or invalid text unit id.</div>
      </div>
    );
  }

  return (
    <TextUnitDetailPageView
      tmTextUnitId={tmTextUnitId}
      onBack={handleBack}
      editorInfo={{
        target: draftTarget,
        status: draftStatus,
        statusOptions: editorStatusOptions,
        canEdit,
        canDelete: canDeleteCurrentTranslation,
        isDirty: isEditorDirty,
        isSaving: saveMutation.isPending,
        isDeleting: deleteMutation.isPending,
        errorMessage: saveErrorMessage,
        warningMessage: editorWarningMessage,
      }}
      keyInfo={{
        stringId: formatValue(activeTextUnit?.name),
        locale: formatValue(localeForEditing),
        source: formatValue(activeTextUnit?.source),
        comment: formatValue(activeTextUnit?.comment),
        repositoryName: formatValue(activeTextUnit?.repositoryName),
      }}
      onChangeTarget={setDraftTarget}
      onChangeStatus={(value) => setDraftStatus(normalizeEditorStatus(value))}
      onSaveEditor={handleSaveEditor}
      onResetEditor={handleResetEditor}
      onRequestDeleteEditor={handleRequestDeleteEditor}
      previewLocale={localeForEditing ?? 'en'}
      isIcuPreviewCollapsed={isIcuPreviewCollapsed}
      onToggleIcuPreviewCollapsed={() => setIsIcuPreviewCollapsed((current) => !current)}
      icuPreviewMode={icuPreviewMode}
      onChangeIcuPreviewMode={setIcuPreviewMode}
      isAiCollapsed={isAiCollapsed}
      onToggleAiCollapsed={() => setIsAiCollapsed((current) => !current)}
      aiMessages={aiMessages}
      aiInput={aiInput}
      onChangeAiInput={setAiInput}
      onSubmitAi={handleSubmitAi}
      onUseAiSuggestion={handleUseAiSuggestion}
      isAiResponding={isAiResponding}
      isMetaCollapsed={isMetaCollapsed}
      onToggleMetaCollapsed={() => setIsMetaCollapsed((current) => !current)}
      isMetaLoading={textUnitQuery.isLoading}
      metaErrorMessage={getQueryErrorMessage(textUnitQuery.error)}
      metaSections={metaSections}
      metaWarningMessage={
        gitBlameQuery.isError
          ? `Git blame metadata is unavailable: ${getQueryErrorMessage(gitBlameQuery.error) ?? '-'}`
          : null
      }
      isHistoryCollapsed={isHistoryCollapsed}
      onToggleHistoryCollapsed={() => setIsHistoryCollapsed((current) => !current)}
      isHistoryLoading={!localeTag ? false : textUnitQuery.isLoading || historyQuery.isLoading}
      historyErrorMessage={
        !localeTag
          ? null
          : (getQueryErrorMessage(textUnitQuery.error) ?? getQueryErrorMessage(historyQuery.error))
      }
      historyMissingLocale={!localeTag}
      historyRows={historyRows}
      historyInitialDate={formatDateTime(textUnitQuery.data?.tmTextUnitCreatedDate)}
      showDeletedHistoryEntry={showDeletedHistoryEntry}
      showValidationDialog={pendingValidationSave !== null}
      validationDialogBody={pendingValidationSave?.body ?? ''}
      onConfirmValidationSave={handleConfirmValidationSave}
      onDismissValidationDialog={handleDismissValidationDialog}
      showDeleteDialog={showDeleteDialog}
      deleteDialogBody={
        isEditorDirty
          ? 'This will delete the current translation and discard unsaved edits.'
          : 'This will delete the current translation for this locale.'
      }
      onConfirmDeleteEditor={handleConfirmDeleteEditor}
      onDismissDeleteDialog={handleDismissDeleteDialog}
    />
  );
}

const normalizeEditorStatus = (
  value?: string | null,
): 'Accepted' | 'To review' | 'To translate' | 'Rejected' => {
  if (
    value === 'Accepted' ||
    value === 'To review' ||
    value === 'To translate' ||
    value === 'Rejected'
  ) {
    return value;
  }
  return 'To translate';
};

const formatCheckFailureBody = (result: TextUnitIntegrityCheckResult | null) => {
  const detail = result?.failureDetail?.trim();
  if (detail) {
    return `This translation failed the placeholder/integrity check:\n\n${detail}\n\nDo you want to save it anyway?`;
  }
  return 'This translation failed the placeholder/integrity check. Do you want to save it anyway?';
};

const safeDateValue = (value?: string | null) => {
  if (!value) {
    return Number.MIN_SAFE_INTEGER;
  }
  const parsed = Date.parse(value);
  return Number.isNaN(parsed) ? Number.MIN_SAFE_INTEGER : parsed;
};

const formatDateTime = (value?: string | null) => {
  if (!value) {
    return '-';
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
};

const formatHistoryStatus = (status?: string | null, includedInLocalizedFile?: boolean) => {
  if (includedInLocalizedFile === false) {
    return 'Rejected';
  }

  switch (status) {
    case 'APPROVED':
      return 'Accepted';
    case 'REVIEW_NEEDED':
      return 'To review';
    case 'TRANSLATION_NEEDED':
      return 'To translate';
    default:
      return null;
  }
};

const getQueryErrorMessage = (error: unknown) => {
  if (error instanceof Error) {
    return error.message;
  }
  return null;
};

const formatGitCommitTime = (commitTime?: string | null) => {
  if (!commitTime) {
    return '-';
  }

  const parsedSeconds = Number.parseInt(commitTime, 10);
  if (!Number.isFinite(parsedSeconds)) {
    return '-';
  }
  return formatDateTime(new Date(parsedSeconds * 1000).toISOString());
};

const formatBoolean = (value?: boolean) => {
  if (typeof value !== 'boolean') {
    return '-';
  }
  return value ? 'true' : 'false';
};

const formatNumberish = (value?: number | null) => {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    return '-';
  }
  return String(value);
};

const formatScreenshots = (
  screenshots?: Array<{
    id?: number | null;
    name?: string | null;
    src?: string | null;
  }> | null,
) => {
  if (!screenshots || screenshots.length === 0) {
    return '-';
  }
  return String(screenshots.length);
};

const formatHistoryTranslation = (translation?: string | null) => {
  if (translation == null) {
    return '<no translation yet>';
  }
  return translation;
};

const formatValue = (value?: string | null) => {
  if (value == null || value.trim().length === 0) {
    return '-';
  }
  return value;
};
