import { useCallback, useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

import type {
  ApiReviewProjectStatus,
  ApiReviewProjectSummary,
  ApiReviewProjectType,
  ReviewProjectsSearchRequest,
} from '../../api/review-projects';
import {
  REVIEW_PROJECT_STATUS_LABELS,
  REVIEW_PROJECT_STATUSES,
  REVIEW_PROJECT_TYPE_LABELS,
  REVIEW_PROJECT_TYPES,
} from '../../api/review-projects';
import { useUser } from '../../components/RequireUser';
import { useRepositories } from '../../hooks/useRepositories';
import { useReviewProjects } from '../../hooks/useReviewProjects';
import { useLocaleOptionsWithDisplayNames } from '../../utils/localeSelection';
import { filterMyLocales } from '../../utils/localeSelection';
import { loadPreferredLocales } from '../workbench/workbench-preferences';
import { type ReviewProjectRow, ReviewProjectsPageView } from './ReviewProjectsPageView';

type FilterOption<T extends string | number> = { value: T; label: string };

type SelectAllLocalesParams = {
  localeOptions: { tag: string }[];
  projects: ApiReviewProjectSummary[] | undefined;
  selectedLocaleTags: string[];
  setSelectedLocaleTags: (tags: string[]) => void;
  userHasTouchedLocales: boolean;
};

type ReviewProjectsNavState = {
  requestId?: number | null;
  requestUuid?: string | null;
};

function isReviewProjectsNavState(value: unknown): value is ReviewProjectsNavState {
  return (
    typeof value === 'object' &&
    value !== null &&
    (('requestId' in value && typeof (value as { requestId?: unknown }).requestId === 'number') ||
      ('requestUuid' in value && typeof (value as { requestUuid?: unknown }).requestUuid === 'string'))
  );
}

function useSelectAllLocales({
  localeOptions,
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

    const next = Array.from(new Set(optionTags));
    const currentSet = new Set(selectedLocaleTags);
    const hasDifference =
      currentSet.size !== next.length || next.some((tag) => !currentSet.has(tag));

    if (hasDifference) {
      setSelectedLocaleTags(next);
    }
  }, [localeOptions, selectedLocaleTags, setSelectedLocaleTags, userHasTouchedLocales]);
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

  const [selectedLocaleTags, setSelectedLocaleTags] = useState<string[]>([]);
  const [hasTouchedLocales, setHasTouchedLocales] = useState(false);
  const [typeFilter, setTypeFilter] = useState<ApiReviewProjectType | 'all'>('all');
  const [statusFilter, setStatusFilter] = useState<ApiReviewProjectStatus | 'all'>('OPEN');
  const [limit, setLimit] = useState<number>(1000);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchField, setSearchField] = useState<'name' | 'id'>('name');
  const [searchType, setSearchType] = useState<'contains' | 'exact' | 'ilike'>('contains');
  const [createdAfter, setCreatedAfter] = useState<string | null>(null);
  const [createdBefore, setCreatedBefore] = useState<string | null>(null);
  const [dueAfter, setDueAfter] = useState<string | null>(null);
  const [dueBefore, setDueBefore] = useState<string | null>(null);
  const [requestIdFilter, setRequestIdFilter] = useState<number | null>(null);
  const [requestUuidFilter, setRequestUuidFilter] = useState<string | null>(null);

  const searchParams = useMemo<ReviewProjectsSearchRequest>(() => {
    const searchFieldValue: ReviewProjectsSearchRequest['searchField'] =
      searchField === 'id' ? 'ID' : 'NAME';
    const searchMatchTypeValue: ReviewProjectsSearchRequest['searchMatchType'] =
      searchType === 'exact' ? 'EXACT' : searchType === 'ilike' ? 'ILIKE' : 'CONTAINS';

    return {
      localeTags: selectedLocaleTags.length > 0 ? selectedLocaleTags : undefined,
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
  ]);

  useEffect(() => {
    const state = isReviewProjectsNavState(location.state) ? location.state : null;
    if (!state) {
      return;
    }
    setRequestIdFilter(state.requestId ?? null);
    setRequestUuidFilter(state.requestUuid ?? null);
    void navigate(location.pathname, { replace: true });
  }, [location.pathname, location.state, navigate]);

  const { data, isLoading, isError, error, refetch } = useReviewProjects(searchParams);

  const projects = useMemo(() => data ?? [], [data]);
  const repositories = useMemo(() => repositoryData ?? [], [repositoryData]);
  const localeOptions = useLocaleOptionsWithDisplayNames(repositories);
  const preferredLocales = useMemo(() => loadPreferredLocales(), []);
  const myLocaleSelections = useMemo(
    () =>
      filterMyLocales({
        availableLocaleTags: localeOptions.map((option) => option.tag),
        userLocales: user.userLocales ?? [],
        preferredLocales,
        isLimitedTranslator: !user.canTranslateAllLocales && (user.userLocales?.length ?? 0) > 0,
        isAdmin: user.role === 'ROLE_ADMIN',
      }),
    [localeOptions, preferredLocales, user.canTranslateAllLocales, user.role, user.userLocales],
  );

  const status: 'loading' | 'error' | 'ready' =
    isLoading ? 'loading' : isError ? 'error' : 'ready';

  const errorMessage =
    isError
      ? error instanceof Error
        ? error.message
        : 'Failed to load review projects.'
      : undefined;

  const filteredProjects = useMemo(() => {
    let source = projects;
    if (requestIdFilter !== null) {
      source = source.filter((project) => project.requestId === requestIdFilter);
    } else if (requestUuidFilter) {
      source = source.filter((project) => project.requestUuid === requestUuidFilter);
    }
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
      const field: 'id' | 'name' = sf === 'ID' ? 'id' : 'name';
      const value =
        field === 'id' ? String(project.id) : (project.name ?? `Review project #${project.id}`);
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
      const tags = (project.locales ?? []).map((loc) => loc.bcp47Tag);
      return tags.some((tag) => localeTags.includes(tag));
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
  }, [projects, searchParams, hasTouchedLocales, requestIdFilter, requestUuidFilter]);

  const rows = useMemo<ReviewProjectRow[]>(() => {
    return filteredProjects.map((project) => ({
      id: project.id,
      name: project.name ?? `Review project #${project.id}`,
      type: project.type,
      status: project.status,
      locales: (project.locales ?? []).map((locale) => locale.bcp47Tag),
      repositoryNames: (project.repositories ?? []).map((repo) => repo.name),
      acceptedCount: project.acceptedCount,
      textUnitCount: project.textUnitCount ?? null,
      wordCount: project.wordCount ?? null,
      dueDate: project.dueDate ?? null,
      closeReason: project.closeReason ?? null,
    }));
  }, [filteredProjects]);

  const requestFilter = useMemo(
    () =>
      requestIdFilter === null && !requestUuidFilter
        ? null
        : {
            requestId: requestIdFilter,
            requestUuid: requestUuidFilter,
            onClear: () => {
              setRequestIdFilter(null);
              setRequestUuidFilter(null);
            },
          },
    [requestIdFilter, requestUuidFilter],
  );

  const handleRetry = useCallback(() => {
    void refetch();
  }, [refetch]);

  useSelectAllLocales({
    localeOptions,
    projects: data,
    selectedLocaleTags,
    setSelectedLocaleTags,
    userHasTouchedLocales: hasTouchedLocales,
  });

  return (
    <ReviewProjectsPageView
      status={status}
      errorMessage={errorMessage}
      errorOnRetry={handleRetry}
      projects={rows}
      filters={{
        localeOptions,
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
      requestFilter={requestFilter ?? undefined}
      canCreate={user.role === 'ROLE_ADMIN'}
    />
  );
}
