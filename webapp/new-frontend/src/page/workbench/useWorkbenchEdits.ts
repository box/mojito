import type { InfiniteData } from '@tanstack/react-query';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { type RefObject, useCallback, useEffect, useMemo, useRef, useState } from 'react';

import {
  type ApiTextUnit,
  checkTextUnitIntegrity,
  saveTextUnit,
  type SaveTextUnitRequest,
  type TextUnitIntegrityCheckResult,
  type TextUnitSearchRequest,
} from '../../api/text-units';
import { formatStatus, mapUiStatusToApi, updateInfiniteData } from './workbench-helpers';
import type { WorkbenchRow } from './workbench-types';

type WorkbenchSearchSnapshot = Array<
  [readonly unknown[], InfiniteData<ApiTextUnit[], number> | undefined]
>;

type SaveTextUnitMutationVars = SaveTextUnitRequest & {
  // Extra identifiers for optimistic UI / workset tracking; not sent to the backend.
  __targetLocale?: string;
  __rowId?: string;
};

type WorksetEditEntry = {
  hasSavedChange: boolean;
  baselineVariantId: number | null;
  savedVariantId: number | null;
  baselineCurrentVariantId: number | null;
  savedCurrentVariantId: number | null;
  baselineTarget: string | null;
  baselineStatusLabel: string;
  latestTarget: string | null;
  latestStatusLabel: string;
  updatedAt: number;
};

type Params = {
  apiRows: WorkbenchRow[];
  canSearch: boolean;
  activeSearchRequest: TextUnitSearchRequest | null;
};

type UseWorkbenchEditsResult = {
  editingRowId: string | null;
  editingValue: string;
  editedRowIds: Set<string>;
  statusSavingRowIds: Set<string>;
  diffModal: {
    rowId: string;
    textUnitName: string;
    repositoryName: string;
    assetPath: string | null;
    locale: string;
    baselineTarget: string | null;
    latestTarget: string | null;
    baselineStatusLabel: string;
    latestStatusLabel: string;
    baselineVariantId: number | null;
    savedVariantId: number | null;
    baselineCurrentVariantId: number | null;
    savedCurrentVariantId: number | null;
  } | null;
  onShowDiff: (rowId: string) => void;
  onCloseDiff: () => void;
  onStartEditing: (rowId: string, translation: string | null) => void;
  onCancelEditing: () => void;
  onSaveEditing: () => void;
  onChangeEditingValue: (value: string) => void;
  onChangeStatus: (rowId: string, status: string) => void;
  translationInputRef: RefObject<HTMLTextAreaElement>;
  registerRowRef: (rowId: string, element: HTMLDivElement | null) => void;
  isSaving: boolean;
  saveErrorMessage: string | null;
  pendingValidationSave: { request: SaveTextUnitMutationVars; body: string } | null;
  confirmValidationSave: () => void;
  dismissValidationSave: () => void;
  pendingEditingTarget: { rowId: string; translation: string | null } | null;
  confirmDiscardEditing: () => void;
  dismissDiscardEditing: () => void;
  clearWorksetEdits: () => void;
};

export function useWorkbenchEdits({
  apiRows,
  canSearch,
  activeSearchRequest,
}: Params): UseWorkbenchEditsResult {
  const [editingRowId, setEditingRowId] = useState<string | null>(null);
  const [editingValue, setEditingValue] = useState('');
  const [editingInitialValue, setEditingInitialValue] = useState('');
  const [pendingEditingTarget, setPendingEditingTarget] = useState<{
    rowId: string;
    translation: string | null;
  } | null>(null);
  const [saveErrorMessage, setSaveErrorMessage] = useState<string | null>(null);
  const [pendingValidationSave, setPendingValidationSave] = useState<{
    request: SaveTextUnitMutationVars;
    body: string;
  } | null>(null);
  const [statusSavingRowIds, setStatusSavingRowIds] = useState<Set<string>>(() => new Set());
  const [worksetEdits, setWorksetEdits] = useState<Map<string, WorksetEditEntry>>(() => new Map());
  const [diffRowId, setDiffRowId] = useState<string | null>(null);
  const translationInputRef = useRef<HTMLTextAreaElement>(null);
  const rowRefs = useRef<Record<string, HTMLDivElement | null>>({});
  const saveAttemptRef = useRef(0);
  const queryClient = useQueryClient();

  const matchesMutationTarget = (
    item: ApiTextUnit,
    vars: { tmTextUnitId: number; localeId: number; __targetLocale?: string },
  ) => {
    if (item.tmTextUnitId !== vars.tmTextUnitId) {
      return false;
    }
    if (typeof item.localeId === 'number') {
      return item.localeId === vars.localeId;
    }
    return Boolean(vars.__targetLocale) && item.targetLocale === vars.__targetLocale;
  };

  const ensureBaselineEdit = useCallback((row: WorkbenchRow) => {
    setWorksetEdits((previous) => {
      if (previous.has(row.id)) {
        return previous;
      }

      const next = new Map(previous);
      const baselineVariantId = row.tmTextUnitVariantId ?? row.tmTextUnitCurrentVariantId ?? null;
      next.set(row.id, {
        hasSavedChange: false,
        baselineVariantId,
        savedVariantId: baselineVariantId,
        baselineCurrentVariantId: row.tmTextUnitCurrentVariantId ?? null,
        savedCurrentVariantId: row.tmTextUnitCurrentVariantId ?? null,
        baselineTarget: row.translation ?? null,
        baselineStatusLabel: row.status,
        latestTarget: row.translation ?? null,
        latestStatusLabel: row.status,
        updatedAt: Date.now(),
      });
      return next;
    });
  }, []);

  const saveTextUnitMutation = useMutation<
    ApiTextUnit,
    Error,
    SaveTextUnitMutationVars,
    { previous: WorkbenchSearchSnapshot }
  >({
    mutationFn: (variables) => {
      const { __targetLocale, __rowId, ...request } = variables;
      void __targetLocale;
      void __rowId;
      return saveTextUnit(request);
    },
    onMutate: async (variables) => {
      setSaveErrorMessage(null);

      await queryClient.cancelQueries({ queryKey: ['workbench-search'] });

      // Snapshot all cached workbench queries for rollback.
      const previous: WorkbenchSearchSnapshot = queryClient.getQueriesData<
        InfiniteData<ApiTextUnit[], number>
      >({ queryKey: ['workbench-search'] });

      queryClient.setQueriesData<InfiniteData<ApiTextUnit[], number>>(
        { queryKey: ['workbench-search'] },
        (data) =>
          updateInfiniteData(data, (item) => {
            if (!matchesMutationTarget(item, variables)) {
              return item;
            }

            return {
              ...item,
              target: variables.target,
              targetComment: variables.targetComment ?? item.targetComment,
              status: variables.status ?? item.status,
              includedInLocalizedFile:
                typeof variables.includedInLocalizedFile === 'boolean'
                  ? variables.includedInLocalizedFile
                  : item.includedInLocalizedFile,
            };
          }),
      );

      return { previous };
    },
    onSuccess: (saved, variables) => {
      setSaveErrorMessage(null);
      const rowId = variables.__rowId;
      if (rowId) {
        const nextTarget = saved.target ?? variables.target ?? null;
        const nextStatusLabel = formatStatus(
          saved.status ?? variables.status ?? null,
          saved.includedInLocalizedFile ?? variables.includedInLocalizedFile,
        );
        const savedVariantId =
          saved.tmTextUnitVariantId ?? saved.tmTextUnitCurrentVariantId ?? null;
        const savedCurrentVariantId =
          saved.tmTextUnitCurrentVariantId ?? saved.tmTextUnitVariantId ?? null;
        setWorksetEdits((previous) => {
          const next = new Map(previous);
          const existing = next.get(rowId);
          if (existing) {
            next.set(rowId, {
              ...existing,
              hasSavedChange: true,
              savedVariantId,
              savedCurrentVariantId,
              latestTarget: nextTarget,
              latestStatusLabel: nextStatusLabel,
              updatedAt: Date.now(),
            });
          }
          return next;
        });
      }
      // Update any cached workbench result sets that contain this text unit.
      queryClient.setQueriesData<InfiniteData<ApiTextUnit[], number>>(
        { queryKey: ['workbench-search'] },
        (previous) =>
          updateInfiniteData(previous, (item) => {
            const sameId = item.tmTextUnitId === saved.tmTextUnitId;
            const sameLocale =
              typeof saved.localeId === 'number'
                ? item.localeId === saved.localeId
                : item.targetLocale === saved.targetLocale;

            if (!sameId || !sameLocale) {
              return item;
            }

            return {
              ...item,
              target: saved.target ?? item.target,
              targetComment: saved.targetComment ?? item.targetComment,
              status: saved.status ?? item.status,
              includedInLocalizedFile:
                saved.includedInLocalizedFile ?? item.includedInLocalizedFile,
              tmTextUnitVariantId: saved.tmTextUnitVariantId ?? item.tmTextUnitVariantId,
              tmTextUnitCurrentVariantId:
                saved.tmTextUnitCurrentVariantId ??
                saved.tmTextUnitVariantId ??
                item.tmTextUnitCurrentVariantId,
              localeId: saved.localeId ?? item.localeId,
            };
          }),
      );
    },
    onError: (error, _variables, context) => {
      if (context?.previous) {
        for (const [key, data] of context.previous) {
          queryClient.setQueryData(key, data);
        }
      }
      const message =
        (error as { status?: number }).status === 403
          ? 'You cannot edit this locale.'
          : error.message;
      setSaveErrorMessage(message);
    },
  });

  const registerRowRef = useCallback((rowId: string, element: HTMLDivElement | null) => {
    if (!element) {
      delete rowRefs.current[rowId];
      return;
    }
    rowRefs.current[rowId] = element;
  }, []);

  const handleStartEditing = useCallback((rowId: string, translation: string | null) => {
    saveAttemptRef.current += 1;
    const nextValue = translation ?? '';
    setEditingRowId(rowId);
    setEditingValue(nextValue);
    setEditingInitialValue(nextValue);
    setSaveErrorMessage(null);
    setPendingValidationSave(null);
    setPendingEditingTarget(null);
  }, []);

  const handleCancelEditing = useCallback(() => {
    saveAttemptRef.current += 1;
    setEditingRowId(null);
    setEditingValue('');
    setEditingInitialValue('');
    setSaveErrorMessage(null);
    setPendingValidationSave(null);
    setPendingEditingTarget(null);
  }, []);

  const handleChangeEditingValue = useCallback((value: string) => {
    setEditingValue(value);
  }, []);

  const hasUnsavedChanges = editingRowId !== null && editingValue !== editingInitialValue;

  const handleRequestStartEditing = useCallback(
    (rowId: string, translation: string | null) => {
      const row = apiRows.find((candidate) => candidate.id === rowId);
      if (row && !row.canEdit) {
        setSaveErrorMessage('You cannot edit this locale.');
        return;
      }
      if (editingRowId && editingRowId !== rowId && hasUnsavedChanges) {
        setPendingEditingTarget({ rowId, translation });
        return;
      }

      handleStartEditing(rowId, translation);
    },
    [apiRows, editingRowId, hasUnsavedChanges, handleStartEditing],
  );

  const confirmDiscardEditing = useCallback(() => {
    if (!pendingEditingTarget) {
      return;
    }
    handleStartEditing(pendingEditingTarget.rowId, pendingEditingTarget.translation);
  }, [pendingEditingTarget, handleStartEditing]);

  const dismissDiscardEditing = useCallback(() => {
    setPendingEditingTarget(null);
  }, []);

  const confirmValidationSave = useCallback(() => {
    if (!pendingValidationSave) {
      return;
    }

    const request = pendingValidationSave.request;
    setPendingValidationSave(null);
    setSaveErrorMessage(null);

    void saveTextUnitMutation.mutateAsync(request).then(() => {
      handleCancelEditing();
    });
  }, [handleCancelEditing, pendingValidationSave, saveTextUnitMutation]);

  const dismissValidationSave = useCallback(() => {
    setPendingValidationSave(null);
  }, []);

  const handleSaveEditing = useCallback(() => {
    if (!editingRowId) {
      return;
    }

    const attemptId = (saveAttemptRef.current += 1);

    const row = apiRows.find((candidate) => candidate.id === editingRowId);
    if (!row) {
      setSaveErrorMessage('Unable to save: row is no longer available.');
      return;
    }
    if (!row.canEdit) {
      setSaveErrorMessage('You cannot edit this locale.');
      return;
    }
    if (typeof row.localeId !== 'number') {
      setSaveErrorMessage('Unable to save: missing locale id.');
      return;
    }

    if (editingValue === editingInitialValue) {
      // Match legacy behavior: don't post if nothing changed.
      handleCancelEditing();
      return;
    }

    setSaveErrorMessage(null);

    ensureBaselineEdit(row);

    const request: SaveTextUnitMutationVars = {
      tmTextUnitId: row.tmTextUnitId,
      localeId: row.localeId,
      target: editingValue,
      status: 'APPROVED',
      includedInLocalizedFile: true,
      __targetLocale: row.locale,
      __rowId: row.id,
    };

    const formatCheckFailureBody = (result: TextUnitIntegrityCheckResult | null) => {
      const detail = result?.failureDetail?.trim();
      if (detail) {
        return `This translation failed the placeholder/integrity check:\n\n${detail}\n\nDo you want to save it anyway?`;
      }
      return 'This translation failed the placeholder/integrity check. Do you want to save it anyway?';
    };

    void checkTextUnitIntegrity({ tmTextUnitId: row.tmTextUnitId, content: editingValue })
      .then((result) => {
        if (saveAttemptRef.current !== attemptId) {
          return;
        }
        if (result?.checkResult === false) {
          setPendingValidationSave({ request, body: formatCheckFailureBody(result) });
          return;
        }
        void saveTextUnitMutation.mutateAsync(request).then(() => {
          handleCancelEditing();
        });
      })
      .catch((error: unknown) => {
        if (saveAttemptRef.current !== attemptId) {
          return;
        }
        const message = error instanceof Error ? error.message : 'Unknown error';
        setPendingValidationSave({
          request,
          body: `Unable to validate placeholders (${message}). Do you want to save it anyway?`,
        });
      });
  }, [
    apiRows,
    editingInitialValue,
    editingRowId,
    editingValue,
    ensureBaselineEdit,
    handleCancelEditing,
    saveTextUnitMutation,
  ]);

  const handleChangeStatus = useCallback(
    (rowId: string, status: string) => {
      const row = apiRows.find((candidate) => candidate.id === rowId);
      if (!row) {
        return;
      }
      if (!row.canEdit) {
        setSaveErrorMessage('You cannot edit this locale.');
        return;
      }
      if (typeof row.localeId !== 'number') {
        setSaveErrorMessage('Unable to update status: missing locale id.');
        return;
      }

      const target = row.translation ?? '';

      const statusUpdate = mapUiStatusToApi(status);
      if (!statusUpdate || !statusUpdate.status) {
        return;
      }

      ensureBaselineEdit(row);

      // Status changes are edits too; keep edits tracked alongside the current dataset.
      setSaveErrorMessage(null);
      setStatusSavingRowIds((previous) => {
        const next = new Set(previous);
        next.add(rowId);
        return next;
      });

      void saveTextUnitMutation
        .mutateAsync({
          tmTextUnitId: row.tmTextUnitId,
          localeId: row.localeId,
          target,
          status: statusUpdate.status,
          includedInLocalizedFile: statusUpdate.includedInLocalizedFile,
          __targetLocale: row.locale,
          __rowId: row.id,
        })
        .finally(() => {
          setStatusSavingRowIds((previous) => {
            if (!previous.has(rowId)) {
              return previous;
            }
            const next = new Set(previous);
            next.delete(rowId);
            return next;
          });
        });
    },
    [apiRows, ensureBaselineEdit, saveTextUnitMutation],
  );

  useEffect(() => {
    if (!editingRowId) {
      return;
    }

    const rowElement = rowRefs.current[editingRowId];
    if (rowElement) {
      rowElement.scrollIntoView({ block: 'nearest' });
    }

    if (translationInputRef.current && document.activeElement !== translationInputRef.current) {
      translationInputRef.current.focus();
    }
  }, [editingRowId]);

  const editedRowIds = useMemo(() => {
    const edited = new Set<string>();
    for (const [rowId, entry] of worksetEdits.entries()) {
      if (entry.hasSavedChange) {
        edited.add(rowId);
      }
    }
    return edited;
  }, [worksetEdits]);

  const clearWorksetEdits = useCallback(() => {
    setDiffRowId(null);
    setWorksetEdits(new Map());
  }, []);

  useEffect(() => {
    if (canSearch) {
      return;
    }
    setEditingRowId(null);
    setEditingValue('');
    setEditingInitialValue('');
    setPendingEditingTarget(null);
    setPendingValidationSave(null);
    clearWorksetEdits();
  }, [canSearch, clearWorksetEdits]);

  // Whenever the applied search changes (new workset), clear edit markers.
  useEffect(() => {
    clearWorksetEdits();
  }, [activeSearchRequest, clearWorksetEdits]);

  const diffEntry = diffRowId ? worksetEdits.get(diffRowId) : null;
  const diffRow = diffRowId ? apiRows.find((row) => row.id === diffRowId) : null;
  const diffModal =
    diffEntry && diffRow
      ? {
          rowId: diffRow.id,
          textUnitName: diffRow.textUnitName,
          repositoryName: diffRow.repositoryName,
          assetPath: diffRow.assetPath,
          locale: diffRow.locale,
          baselineTarget: diffEntry.baselineTarget,
          latestTarget: diffEntry.latestTarget,
          baselineStatusLabel: diffEntry.baselineStatusLabel,
          latestStatusLabel: diffEntry.latestStatusLabel,
          baselineVariantId: diffEntry.baselineVariantId,
          savedVariantId: diffEntry.savedVariantId,
          baselineCurrentVariantId: diffEntry.baselineCurrentVariantId,
          savedCurrentVariantId: diffEntry.savedCurrentVariantId,
        }
      : null;

  return {
    editingRowId,
    editingValue,
    editedRowIds,
    statusSavingRowIds,
    diffModal,
    onShowDiff: (rowId) => setDiffRowId(rowId),
    onCloseDiff: () => setDiffRowId(null),
    onStartEditing: handleRequestStartEditing,
    onCancelEditing: handleCancelEditing,
    onSaveEditing: handleSaveEditing,
    onChangeEditingValue: handleChangeEditingValue,
    onChangeStatus: handleChangeStatus,
    translationInputRef,
    registerRowRef,
    isSaving: saveTextUnitMutation.isPending,
    saveErrorMessage,
    pendingValidationSave,
    confirmValidationSave,
    dismissValidationSave,
    pendingEditingTarget,
    confirmDiscardEditing,
    dismissDiscardEditing,
    clearWorksetEdits,
  };
}
