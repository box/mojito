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

export type ApiReviewProjectSummary = {
  id: number;
  name?: string | null;
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

export const generateSampleReviewProjects = async (): Promise<ApiReviewProjectSummary[]> => {
  const makeRequest = (method: 'POST' | 'GET') =>
    fetch('/api/review-projects/generate-sample?count=150', {
      method,
      credentials: 'include',
    });

  let response = await makeRequest('POST');
  // Some environments block POST here; retry with GET to avoid a hard failure.
  if (response.status === 405) {
    response = await makeRequest('GET');
  }

  if (!response.ok) {
    const message = await response.text().catch(() => '');
    throw new Error(message || 'Failed to generate sample review projects');
  }

  return (await response.json()) as ApiReviewProjectSummary[];
};
