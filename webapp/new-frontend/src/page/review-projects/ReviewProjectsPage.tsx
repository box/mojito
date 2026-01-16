import { useCallback, useEffect, useMemo, useState } from 'react';

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
import { generateSampleReviewProjects } from '../../api/review-projects';
import { useUser } from '../../components/RequireUser';
import { useRepositories } from '../../hooks/useRepositories';
import { useReviewProjects } from '../../hooks/useReviewProjects';
import { useLocaleOptionsWithDisplayNames } from '../../utils/localeSelection';
import { filterMyLocales } from '../../utils/localeSelection';
import { loadPreferredLocales } from '../workbench/workbench-preferences';
import { mockReviewProjects } from './mockReviewProjects';
import { type ReviewProjectRow, ReviewProjectsPageView } from './ReviewProjectsPageView';

type FilterOption<T extends string | number> = { value: T; label: string };

type SelectAllLocalesParams = {
  localeOptions: { tag: string }[];
  projects: ApiReviewProjectSummary[] | undefined;
  selectedLocaleTags: string[];
  setSelectedLocaleTags: (tags: string[]) => void;
};

function useSelectAllLocales({
  localeOptions,
  selectedLocaleTags,
  setSelectedLocaleTags,
}: SelectAllLocalesParams) {
  useEffect(() => {
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
  }, [localeOptions, selectedLocaleTags, setSelectedLocaleTags]);
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

  const [selectedLocaleTags, setSelectedLocaleTags] = useState<string[]>([]);
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
  const [overrideProjects, setOverrideProjects] = useState<ApiReviewProjectSummary[] | null>(null);
  const [generateError, setGenerateError] = useState<string | null>(null);
  const [isGenerating, setIsGenerating] = useState(false);

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

  const { data, isLoading, isError, error, refetch } = useReviewProjects(searchParams);

  const projects = useMemo(() => overrideProjects ?? data ?? [], [data, overrideProjects]);
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
    overrideProjects != null || isGenerating
      ? 'ready'
      : isLoading
        ? 'loading'
        : isError
          ? 'error'
          : 'ready';

  const errorMessage =
    overrideProjects != null
      ? undefined
      : (generateError ??
        (isError
          ? error instanceof Error
            ? error.message
            : 'Failed to load review projects.'
          : undefined));

  const ensureEmergencyProject = useCallback(
    (projects: ApiReviewProjectSummary[]): ApiReviewProjectSummary[] => {
      if (projects.some((project) => project.type === 'EMERGENCY')) {
        return projects;
      }

      const sampleEmergency =
        mockReviewProjects.find((project) => project.type === 'EMERGENCY') ??
        ({
          id: Number.MAX_SAFE_INTEGER,
          name: 'Emergency review project',
          createdDate: new Date().toISOString(),
          dueDate: new Date(Date.now() + 1000 * 60 * 60 * 24 * 2).toISOString(),
          textUnitCount: 50,
          wordCount: 10000,
          type: 'EMERGENCY' as const,
          status: 'OPEN' as const,
          acceptedCount: 0,
          repositories: [],
          locales: [],
        } satisfies ApiReviewProjectSummary);

      return [sampleEmergency, ...projects];
    },
    [],
  );

  const handleUseMock = useCallback(() => {
    setGenerateError(null);
    setIsGenerating(true);
    generateSampleReviewProjects()
      .then((generated) => {
        const baseProjects = generated.length > 0 ? generated : mockReviewProjects;
        setOverrideProjects(ensureEmergencyProject(baseProjects));
      })
      .catch((e) => {
        const message = e instanceof Error ? e.message : 'Failed to generate sample projects';
        setGenerateError(message);
        setOverrideProjects(ensureEmergencyProject(mockReviewProjects));
      })
      .finally(() => {
        setIsGenerating(false);
      });
  }, [ensureEmergencyProject]);

  const filteredProjects = useMemo(() => {
    const source = projects;
    const usingOverride = overrideProjects != null;
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

    const createdAfterDate = ca ? new Date(ca) : null;
    const createdBeforeDate = cb ? new Date(cb) : null;
    const dueAfterDate = da ? new Date(da) : null;
    const dueBeforeDate = db ? new Date(db) : null;

    const matchesSearch = (project: ApiReviewProjectSummary) => {
      if (!q) return true;
      const field: 'id' | 'name' = sf === 'ID' ? 'id' : 'name';
      const value =
        field === 'id' ? String(project.id) : (project.name ?? `Review project #${project.id}`);
      const query = q;
      if (mt === 'EXACT') {
        return value === query;
      }
      if (mt === 'ILIKE') {
        return value.toLowerCase().includes(query.toLowerCase());
      }
      return value.includes(query);
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
      if (usingOverride) {
        // Mock/override data may not share locales with repository options; avoid filtering it out.
        return true;
      }
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
  }, [projects, searchParams, overrideProjects]);

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

  const handleRetry = useCallback(() => {
    void refetch();
  }, [refetch]);

  useSelectAllLocales({
    localeOptions,
    projects: data,
    selectedLocaleTags,
    setSelectedLocaleTags,
  });

  return (
    <ReviewProjectsPageView
      status={status}
      errorMessage={errorMessage}
      errorOnRetry={handleRetry}
      onLoadMock={handleUseMock}
      projects={rows}
      filters={{
        localeOptions,
        selectedLocaleTags,
        onLocaleChange: setSelectedLocaleTags,
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
    />
  );
}
