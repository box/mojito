// Keep in sync with com.box.l10n.mojito.entity.review.ReviewProjectStatus
export const REVIEW_PROJECT_STATUSES = ['OPEN', 'CLOSED'] as const;
export type ApiReviewProjectStatus = (typeof REVIEW_PROJECT_STATUSES)[number];

// Keep in sync with com.box.l10n.mojito.entity.review.ReviewProjectType
export const REVIEW_PROJECT_TYPES = [
  'EMERGENCY',
  'NORMAL',
  'BUG_FIXES',
  'TERMINOLOGY',
  'UNKNOWN',
] as const;
export type ApiReviewProjectType = (typeof REVIEW_PROJECT_TYPES)[number];

export const REVIEW_PROJECT_STATUS_LABELS: Record<ApiReviewProjectStatus, string> = {
  OPEN: 'Open',
  CLOSED: 'Closed',
};

export const REVIEW_PROJECT_TYPE_LABELS: Record<ApiReviewProjectType, string> = {
  NORMAL: 'Normal',
  EMERGENCY: 'Emergency',
  BUG_FIXES: 'Bug fixes',
  TERMINOLOGY: 'Terminology',
  UNKNOWN: 'Unknown',
};

export type ApiReviewProjectRepositorySummary = {
  id: number;
  name: string;
};

export type ApiReviewProjectLocaleSummary = {
  id: number;
  bcp47Tag: string;
  displayName: string;
  selectedCount: number;
  acceptedCount: number;
};

export type ApiReviewProjectTextUnit = {
  reviewProjectTextUnitId: number;
  tmTextUnitId: number;
  tmTextUnitVariantId: number | null;
  selectedTmTextUnitVariantId: number | null;
  currentTmTextUnitVariantId: number | null;
  name: string;
  source: string;
  target: string | null;
  currentTarget: string | null;
  baselineStatus?: string | null;
  reviewStatus?: 'PENDING' | 'ACCEPTED_AS_IS' | 'ACCEPTED_WITH_CHANGE' | 'REJECTED' | null;
  notes?: string | null;
  reviewedAt?: string | null;
  reviewedBy?: string | null;
  status: string | null;
  repositoryId: number | null;
  repositoryName: string | null;
  assetPath: string | null;
  includedInLocalizedFile: boolean;
};

export type ApiReviewProjectLocaleDetail = ApiReviewProjectLocaleSummary & {
  textUnits: ApiReviewProjectTextUnit[];
};

export type ApiReviewProjectDetail = {
  id: number;
  name?: string | null;
  createdDate?: string | null;
  dueDate?: string | null;
  closeReason?: string | null;
  textUnitCount?: number | null;
  wordCount?: number | null;
  type: ApiReviewProjectType;
  status: ApiReviewProjectStatus;
  notes?: string | null;
  requestId?: number | null;
  requestUuid?: string | null;
  requestName?: string | null;
  locale?: ApiReviewProjectLocaleDetail | null;
  repositories: ApiReviewProjectRepositorySummary[];
  locales: ApiReviewProjectLocaleDetail[];
  screenshotImageIds?: string[] | null;
};

export type ApiReviewProjectSummary = {
  id: number;
  name?: string | null;
  requestName?: string | null;
  createdDate?: string | null;
  dueDate?: string | null;
  closeReason?: string | null;
  requestId?: number | null;
  requestUuid?: string | null;
  textUnitCount?: number | null;
  wordCount?: number | null;
  type: ApiReviewProjectType;
  status: ApiReviewProjectStatus;
  acceptedCount: number;
  repositories: ApiReviewProjectRepositorySummary[];
  locales: ApiReviewProjectLocaleSummary[];
  screenshotImageIds?: string[] | null;
};

export type ReviewProjectsSearchRequest = {
  localeTags?: string[];
  statuses?: ApiReviewProjectStatus[];
  types?: ApiReviewProjectType[];
  createdAfter?: string | null;
  createdBefore?: string | null;
  dueAfter?: string | null;
  dueBefore?: string | null;
  limit?: number;
  searchQuery?: string;
  searchField?: 'NAME' | 'ID';
  searchMatchType?: 'CONTAINS' | 'EXACT' | 'ILIKE';
};

export type ReviewProjectCreateRequest = {
  repositoryIds: number[];
  localeTags: string[];
  notes?: string | null;
  tmTextUnitIds?: number[] | null;
  type?: ApiReviewProjectType | null;
  dueDate: string; // ISO string
  screenshotImageIds?: string[] | null;
  name: string;
};

const jsonHeaders = {
  'Content-Type': 'application/json',
};

export const searchReviewProjects = async (
  params: ReviewProjectsSearchRequest,
): Promise<ApiReviewProjectSummary[]> => {
  const response = await fetch('/api/review-projects/search', {
    method: 'POST',
    credentials: 'include',
    headers: jsonHeaders,
    body: JSON.stringify(params ?? {}),
  });

  if (!response.ok) {
    const message = await response.text().catch(() => '');
    throw new Error(message || 'Failed to load review projects');
  }

  return (await response.json()) as ApiReviewProjectSummary[];
};

export const fetchReviewProjects = async (): Promise<ApiReviewProjectSummary[]> =>
  searchReviewProjects({});

export const createReviewProject = async (
  payload: ReviewProjectCreateRequest,
): Promise<ApiReviewProjectSummary[]> => {
  const response = await fetch('/api/review-projects', {
    method: 'POST',
    credentials: 'include',
    headers: jsonHeaders,
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const message = await response.text().catch(() => '');
    throw new Error(message || 'Failed to create review project');
  }

  return (await response.json()) as ApiReviewProjectSummary[];
};

export const fetchReviewProjectDetail = async (
  projectId: number,
): Promise<ApiReviewProjectDetail> => {
  const response = await fetch(`/api/review-projects/${projectId}`, {
    method: 'GET',
    credentials: 'include',
    headers: jsonHeaders,
  });

  if (!response.ok) {
    const message = await response.text().catch(() => '');
    throw new Error(message || 'Failed to load review project');
  }

  return (await response.json()) as ApiReviewProjectDetail;
};

export const acceptReviewProjectTextUnit = async ({
  projectId,
  textUnitId,
  target,
  includedInLocalizedFile = true,
  expectedCurrentTmTextUnitVariantId,
  overrideChangedCurrent = false,
  notes,
}: {
  projectId: number;
  textUnitId: number;
  target: string;
  includedInLocalizedFile?: boolean;
  expectedCurrentTmTextUnitVariantId?: number | null;
  overrideChangedCurrent?: boolean;
  notes?: string | null;
}): Promise<ApiReviewProjectTextUnit> => {
  const response = await fetch(`/api/review-projects/${projectId}/text-units/${textUnitId}/accept`, {
    method: 'POST',
    credentials: 'include',
    headers: jsonHeaders,
    body: JSON.stringify({
      target,
      includedInLocalizedFile,
      expectedCurrentTmTextUnitVariantId,
      overrideChangedCurrent,
      notes,
    }),
  });

  if (!response.ok) {
    const message = await response.text().catch(() => '');
    const error = new Error(message || 'Failed to accept text unit') as Error & { status?: number };
    error.status = response.status;
    throw error;
  }

  return (await response.json()) as ApiReviewProjectTextUnit;
};

export const updateReviewProjectTextUnitReview = async ({
  projectId,
  textUnitId,
  notes,
}: {
  projectId: number;
  textUnitId: number;
  notes?: string | null;
}): Promise<ApiReviewProjectTextUnit> => {
  const response = await fetch(`/api/review-projects/${projectId}/text-units/${textUnitId}/review`, {
    method: 'POST',
    credentials: 'include',
    headers: jsonHeaders,
    body: JSON.stringify({ notes }),
  });

  if (!response.ok) {
    const message = await response.text().catch(() => '');
    throw new Error(message || 'Failed to update review status');
  }

  return (await response.json()) as ApiReviewProjectTextUnit;
};
