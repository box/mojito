import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

import type {
  ApiReviewProjectStatus,
  ApiReviewProjectSummary,
  ApiReviewProjectType,
  ReviewProjectsSearchRequest,
} from '../../api/review-projects';
import {
  adminBatchDeleteReviewProjects,
  adminBatchUpdateReviewProjectStatus,
  REVIEW_PROJECT_STATUS_LABELS,
  REVIEW_PROJECT_STATUSES,
  REVIEW_PROJECT_TYPE_LABELS,
  REVIEW_PROJECT_TYPES,
} from '../../api/review-projects';
import { ConfirmModal } from '../../components/ConfirmModal';
import { useUser } from '../../components/RequireUser';
import { useRepositories } from '../../hooks/useRepositories';
import { REVIEW_PROJECTS_QUERY_KEY, useReviewProjects } from '../../hooks/useReviewProjects';
import { useLocaleOptionsWithDisplayNames } from '../../utils/localeSelection';
import { filterMyLocales } from '../../utils/localeSelection';
import { loadPreferredLocales } from '../workbench/workbench-preferences';
import {
  type ReviewProjectRow,
  type ReviewProjectsAdminControls,
  ReviewProjectsPageView,
} from './ReviewProjectsPageView';

type FilterOption<T extends string | number> = { value: T; label: string };

type SelectAllLocalesParams = {
  localeOptions: { tag: string }[];
  defaultLocaleTags?: string[];
  projects: ApiReviewProjectSummary[] | undefined;
  selectedLocaleTags: string[];
  setSelectedLocaleTags: (tags: string[]) => void;
  userHasTouchedLocales: boolean;
};

type ReviewProjectsNavState = {
  requestId?: number | null;
};

function isReviewProjectsNavState(value: unknown): value is ReviewProjectsNavState {
  return (
    typeof value === 'object' &&
    value !== null &&
    'requestId' in value &&
    typeof (value as { requestId?: unknown }).requestId === 'number'
  );
}

function useSelectAllLocales({
  localeOptions,
  defaultLocaleTags,
  selectedLocaleTags,
  setSelectedLocaleTags,
  userHasTouchedLocales,
}: SelectAllLocalesParams) {
  useEffect(() => {
    if (userHasTouchedLocales) {
      return;
    }

    if (selectedLocaleTags.length > 0) {
      return;
    }

    const optionTags = localeOptions.map((option) => option.tag).filter(Boolean);
    if (optionTags.length === 0) {
      return;
    }

    if (defaultLocaleTags && defaultLocaleTags.length > 0) {
      const optionTagSet = new Set(optionTags.map((tag) => tag.toLowerCase()));
      const defaults = Array.from(
        new Set(defaultLocaleTags.filter((tag) => optionTagSet.has(tag.toLowerCase()))),
      );
      if (defaults.length > 0) {
        setSelectedLocaleTags(defaults);
        return;
      }
    }

    const next = Array.from(new Set(optionTags));
    const currentSet = new Set(selectedLocaleTags);
    const hasDifference =
      currentSet.size !== next.length || next.some((tag) => !currentSet.has(tag));

    if (hasDifference) {
      setSelectedLocaleTags(next);
    }
  }, [
    defaultLocaleTags,
    localeOptions,
    selectedLocaleTags,
    setSelectedLocaleTags,
    userHasTouchedLocales,
  ]);
}

const typeOptions: FilterOption<ApiReviewProjectType | 'all'>[] = [
  { value: 'all', label: 'All types' },
  ...REVIEW_PROJECT_TYPES.filter((type) => type !== 'UNKNOWN').map((type) => ({
    value: type,
    label: REVIEW_PROJECT_TYPE_LABELS[type],
  })),
];

const statusOptions: FilterOption<ApiReviewProjectStatus | 'all'>[] = [
  { value: 'all', label: 'All statuses' },
  ...REVIEW_PROJECT_STATUSES.map((status) => ({
    value: status,
    label: REVIEW_PROJECT_STATUS_LABELS[status],
  })),
];

const limitOptions: FilterOption<number>[] = [
  { value: 10, label: '10' },
  { value: 100, label: '100' },
  { value: 1000, label: '1k' },
  { value: 10000, label: '10k' },
];

export function ReviewProjectsPage() {
  const user = useUser();
  const { data: repositoryData } = useRepositories();
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const isAdmin = user.role === 'ROLE_ADMIN';

  const [selectedLocaleTags, setSelectedLocaleTags] = useState<string[]>([]);
  const [hasTouchedLocales, setHasTouchedLocales] = useState(false);
  const [typeFilter, setTypeFilter] = useState<ApiReviewProjectType | 'all'>('all');
  const [statusFilter, setStatusFilter] = useState<ApiReviewProjectStatus | 'all'>('OPEN');
  const [limit, setLimit] = useState<number>(1000);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchField, setSearchField] = useState<'name' | 'id' | 'requestId'>('name');
  const [searchType, setSearchType] = useState<'contains' | 'exact' | 'ilike'>('contains');
  const [createdAfter, setCreatedAfter] = useState<string | null>(null);
  const [createdBefore, setCreatedBefore] = useState<string | null>(null);
  const [dueAfter, setDueAfter] = useState<string | null>(null);
  const [dueBefore, setDueBefore] = useState<string | null>(null);
  const [selectedProjectIds, setSelectedProjectIds] = useState<number[]>([]);
  const [adminErrorMessage, setAdminErrorMessage] = useState<string | null>(null);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);

  const repositories = useMemo(() => repositoryData ?? [], [repositoryData]);
  const localeOptions = useLocaleOptionsWithDisplayNames(repositories);
  const userLocales = useMemo(() => user.userLocales ?? [], [user.userLocales]);
  const isLimitedTranslator = useMemo(
    () => !user.canTranslateAllLocales && userLocales.length > 0,
    [user.canTranslateAllLocales, userLocales.length],
  );
  const userLocaleTagSet = useMemo(
    () => new Set(userLocales.map((tag) => tag.toLowerCase())),
    [userLocales],
  );
  const localeOptionsFiltered = useMemo(() => {
    if (!isLimitedTranslator) {
      return localeOptions;
    }
    return localeOptions.filter((option) => userLocaleTagSet.has(option.tag.toLowerCase()));
  }, [isLimitedTranslator, localeOptions, userLocaleTagSet]);
  const availableLocaleTags = useMemo(() => {
    const tags = localeOptionsFiltered.map((option) => option.tag).filter(Boolean);
    return Array.from(new Set(tags));
  }, [localeOptionsFiltered]);

  const searchParams = useMemo<ReviewProjectsSearchRequest>(() => {
    const searchFieldValue: ReviewProjectsSearchRequest['searchField'] =
      searchField === 'requestId' ? 'REQUEST_ID' : searchField === 'id' ? 'ID' : 'NAME';
    const searchMatchTypeValue: ReviewProjectsSearchRequest['searchMatchType'] =
      searchType === 'exact' ? 'EXACT' : searchType === 'ilike' ? 'ILIKE' : 'CONTAINS';
    const selectedLocaleSet = new Set(selectedLocaleTags);
    const localeTags =
      selectedLocaleTags.length === 0
        ? undefined
        : isLimitedTranslator
          ? selectedLocaleTags
          : availableLocaleTags.length > 0 &&
              availableLocaleTags.every((tag) => selectedLocaleSet.has(tag))
            ? undefined
            : selectedLocaleTags;

    return {
      localeTags,
      statuses: statusFilter === 'all' ? undefined : [statusFilter],
      types: typeFilter === 'all' ? undefined : [typeFilter],
      createdAfter,
      createdBefore,
      dueAfter,
      dueBefore,
      limit,
      searchQuery: searchQuery.trim() || undefined,
      searchField: searchFieldValue,
      searchMatchType: searchMatchTypeValue,
    };
  }, [
    availableLocaleTags,
    createdAfter,
    createdBefore,
    dueAfter,
    dueBefore,
    limit,
    searchField,
    searchQuery,
    searchType,
    selectedLocaleTags,
    statusFilter,
    typeFilter,
    isLimitedTranslator,
  ]);

  useEffect(() => {
    const state = isReviewProjectsNavState(location.state) ? location.state : null;
    if (!state) {
      return;
    }
    if (state.requestId != null) {
      setSearchField('requestId');
      setSearchType('exact');
      setSearchQuery(String(state.requestId));
    }
    void navigate(location.pathname, { replace: true });
  }, [location.pathname, location.state, navigate, setSearchField, setSearchQuery, setSearchType]);

  const { data, isLoading, isError, error, refetch } = useReviewProjects(searchParams);

  const projects = useMemo(() => data ?? [], [data]);
  const preferredLocales = useMemo(() => loadPreferredLocales(), []);
  const myLocaleSelections = useMemo(
    () =>
      filterMyLocales({
        availableLocaleTags: localeOptionsFiltered.map((option) => option.tag),
        userLocales,
        preferredLocales,
        isLimitedTranslator,
        isAdmin,
      }),
    [isAdmin, isLimitedTranslator, localeOptionsFiltered, preferredLocales, userLocales],
  );

  const status: 'loading' | 'error' | 'ready' = isLoading ? 'loading' : isError ? 'error' : 'ready';

  const errorMessage = isError
    ? error instanceof Error
      ? error.message
      : 'Failed to load review projects.'
    : undefined;

  const filteredProjects = useMemo(() => {
    const source = projects ?? [];
    const {
      localeTags,
      statuses,
      types,
      createdAfter: ca,
      createdBefore: cb,
      dueAfter: da,
      dueBefore: db,
      limit: lmt,
      searchQuery: q,
      searchField: sf,
      searchMatchType: mt,
    } = searchParams;

    // If the user intentionally cleared all locales, treat it as "no results"
    // instead of falling back to "all locales".
    const userClearedLocales = hasTouchedLocales && (!localeTags || localeTags.length === 0);
    if (userClearedLocales) {
      return [];
    }

    const createdAfterDate = ca ? new Date(ca) : null;
    const createdBeforeDate = cb ? new Date(cb) : null;
    const dueAfterDate = da ? new Date(da) : null;
    const dueBeforeDate = db ? new Date(db) : null;

    const matchesSearch = (project: ApiReviewProjectSummary) => {
      if (!q) return true;
      const field: 'id' | 'name' | 'requestId' =
        sf === 'ID' ? 'id' : sf === 'REQUEST_ID' ? 'requestId' : 'name';
      const value =
        field === 'id'
          ? String(project.id)
          : field === 'requestId'
            ? String(project.reviewProjectRequest?.id ?? '')
            : (project.reviewProjectRequest?.name ?? `Review project #${project.id}`);
      const valueLower = value.toLowerCase();
      const queryLower = q.toLowerCase();

      if (mt === 'EXACT') {
        // Match backend behavior: case-insensitive exact comparison on name;
        // IDs are numeric but comparing as string keeps it stable.
        return valueLower === queryLower;
      }

      // Backend treats CONTAINS and ILIKE identically (case-insensitive LIKE)
      return valueLower.includes(queryLower);
    };

    const matchesDateRange = (
      value: string | null | undefined,
      after: Date | null,
      before: Date | null,
    ) => {
      if (!value) return true;
      const parsed = new Date(value);
      if (Number.isNaN(parsed.getTime())) return true;
      if (after && parsed < after) return false;
      if (before && parsed > before) return false;
      return true;
    };

    const matchesLocales = (project: ApiReviewProjectSummary) => {
      if (!localeTags || localeTags.length === 0) {
        return true;
      }
      const localeId = project.locale?.id ?? null;
      const localeTag = project.locale?.bcp47Tag ?? null;
      if (localeId == null && !localeTag) {
        return true;
      }
      return localeTags.some((tag) => {
        const lowered = tag.toLowerCase();
        if (localeTag && localeTag.toLowerCase() === lowered) {
          return true;
        }
        return localeId != null && String(localeId) === tag;
      });
    };

    const matchesStatus = (project: ApiReviewProjectSummary) => {
      if (!statuses || statuses.length === 0) return true;
      return statuses.includes(project.status);
    };

    const matchesType = (project: ApiReviewProjectSummary) => {
      if (!types || types.length === 0) return true;
      return types.includes(project.type);
    };

    const filtered = source.filter(
      (project) =>
        matchesLocales(project) &&
        matchesStatus(project) &&
        matchesType(project) &&
        matchesDateRange(project.createdDate ?? null, createdAfterDate, createdBeforeDate) &&
        matchesDateRange(project.dueDate ?? null, dueAfterDate, dueBeforeDate) &&
        matchesSearch(project),
    );

    const limitValue = lmt && lmt > 0 ? lmt : undefined;
    return limitValue ? filtered.slice(0, limitValue) : filtered;
  }, [projects, searchParams, hasTouchedLocales]);

  const rows = useMemo<ReviewProjectRow[]>(() => {
    return filteredProjects.map((project) => ({
      id: project.id,
      name: project.reviewProjectRequest?.name ?? `Review project #${project.id}`,
      requestId: project.reviewProjectRequest?.id ?? null,
      type: project.type,
      status: project.status,
      localeTag: project.locale?.bcp47Tag ?? null,
      acceptedCount: project.acceptedCount ?? 0,
      textUnitCount: project.textUnitCount ?? null,
      wordCount: project.wordCount ?? null,
      dueDate: project.dueDate ?? null,
      closeReason: project.closeReason ?? null,
    }));
  }, [filteredProjects]);

  const visibleProjectIds = useMemo(() => rows.map((row) => row.id), [rows]);
  const visibleProjectIdSet = useMemo(() => new Set(visibleProjectIds), [visibleProjectIds]);

  useEffect(() => {
    if (!isAdmin) {
      return;
    }
    setSelectedProjectIds((prev) => prev.filter((id) => visibleProjectIdSet.has(id)));
  }, [isAdmin, visibleProjectIdSet]);

  useEffect(() => {
    if (!isAdmin || selectedProjectIds.length === 0) {
      setDeleteConfirmOpen(false);
    }
  }, [isAdmin, selectedProjectIds.length]);

  const batchStatusMutation = useMutation({
    mutationFn: async ({
      projectIds,
      status,
    }: {
      projectIds: number[];
      status: ApiReviewProjectStatus;
    }) => adminBatchUpdateReviewProjectStatus(projectIds, status),
    onSuccess: () => {
      setAdminErrorMessage(null);
      setSelectedProjectIds([]);
      void queryClient.invalidateQueries({ queryKey: [REVIEW_PROJECTS_QUERY_KEY] });
      void refetch();
    },
    onError: (error) => {
      setAdminErrorMessage(error instanceof Error ? error.message : 'Batch update failed');
    },
  });

  const batchDeleteMutation = useMutation({
    mutationFn: async ({ projectIds }: { projectIds: number[] }) =>
      adminBatchDeleteReviewProjects(projectIds),
    onSuccess: () => {
      setAdminErrorMessage(null);
      setSelectedProjectIds([]);
      void queryClient.invalidateQueries({ queryKey: [REVIEW_PROJECTS_QUERY_KEY] });
      void refetch();
    },
    onError: (error) => {
      setAdminErrorMessage(error instanceof Error ? error.message : 'Batch delete failed');
    },
  });

  const isBatchSaving = batchStatusMutation.isPending || batchDeleteMutation.isPending;

  const handleRetry = useCallback(() => {
    void refetch();
  }, [refetch]);

  const handleRequestIdClick = useCallback((requestId: number) => {
    setSearchField('requestId');
    setSearchType('exact');
    setSearchQuery(String(requestId));
  }, []);

  const toggleProjectSelection = useCallback((projectId: number) => {
    setSelectedProjectIds((prev) => {
      const exists = prev.includes(projectId);
      if (exists) {
        return prev.filter((id) => id !== projectId);
      }
      return [...prev, projectId];
    });
  }, []);

  const clearProjectSelection = useCallback(() => {
    setSelectedProjectIds([]);
  }, []);

  const selectAllVisibleProjects = useCallback(() => {
    setSelectedProjectIds(visibleProjectIds);
  }, [visibleProjectIds]);

  const requestBatchStatus = useCallback(
    (nextStatus: ApiReviewProjectStatus) => {
      if (!isAdmin || selectedProjectIds.length === 0 || isBatchSaving) {
        return;
      }
      batchStatusMutation.mutate({ projectIds: [...selectedProjectIds], status: nextStatus });
    },
    [batchStatusMutation, isAdmin, isBatchSaving, selectedProjectIds],
  );

  const requestBatchDelete = useCallback(() => {
    if (!isAdmin || selectedProjectIds.length === 0 || isBatchSaving) {
      return;
    }
    setDeleteConfirmOpen(true);
  }, [isAdmin, isBatchSaving, selectedProjectIds.length]);

  const handleConfirmBatchDelete = useCallback(() => {
    if (!isAdmin || selectedProjectIds.length === 0 || isBatchSaving) {
      setDeleteConfirmOpen(false);
      return;
    }
    batchDeleteMutation.mutate({ projectIds: [...selectedProjectIds] });
    setDeleteConfirmOpen(false);
  }, [batchDeleteMutation, isAdmin, isBatchSaving, selectedProjectIds]);

  const handleCancelBatchDelete = useCallback(() => {
    setDeleteConfirmOpen(false);
  }, []);

  const adminControls: ReviewProjectsAdminControls | undefined = isAdmin
    ? {
        enabled: true,
        selectedProjectIds,
        onToggleProjectSelection: toggleProjectSelection,
        onSelectAllVisible: selectAllVisibleProjects,
        onClearSelection: clearProjectSelection,
        onBatchStatus: requestBatchStatus,
        onBatchDelete: requestBatchDelete,
        isSaving: isBatchSaving,
        errorMessage: adminErrorMessage,
      }
    : undefined;

  useSelectAllLocales({
    localeOptions: localeOptionsFiltered,
    defaultLocaleTags: isLimitedTranslator ? userLocales : undefined,
    projects: data,
    selectedLocaleTags,
    setSelectedLocaleTags,
    userHasTouchedLocales: hasTouchedLocales,
  });

  return (
    <>
      <ReviewProjectsPageView
        status={status}
        errorMessage={errorMessage}
        errorOnRetry={handleRetry}
        projects={rows}
        filters={{
          localeOptions: localeOptionsFiltered,
          selectedLocaleTags,
          onLocaleChange: (next) => {
            setHasTouchedLocales(true);
            setSelectedLocaleTags(next);
          },
          myLocaleTags: myLocaleSelections,
          typeOptions,
          typeValue: typeFilter,
          onTypeChange: setTypeFilter,
          statusOptions,
          statusValue: statusFilter,
          onStatusChange: setStatusFilter,
          limitOptions,
          limitValue: limit,
          onLimitChange: setLimit,
          createdAfter,
          createdBefore,
          onChangeCreatedAfter: setCreatedAfter,
          onChangeCreatedBefore: setCreatedBefore,
          dueAfter,
          dueBefore,
          onChangeDueAfter: setDueAfter,
          onChangeDueBefore: setDueBefore,
          searchQuery,
          onSearchChange: setSearchQuery,
          searchField,
          onSearchFieldChange: setSearchField,
          searchType,
          onSearchTypeChange: setSearchType,
        }}
        onRequestIdClick={handleRequestIdClick}
        canCreate={isAdmin}
        adminControls={adminControls}
      />
      {isAdmin ? (
        <ConfirmModal
          open={deleteConfirmOpen}
          title="Delete review projects?"
          body={`This will permanently delete ${selectedProjectIds.length} project(s). This cannot be undone.`}
          confirmLabel="Delete"
          cancelLabel="Cancel"
          onConfirm={handleConfirmBatchDelete}
          onCancel={handleCancelBatchDelete}
        />
      ) : null}
    </>
  );
}
