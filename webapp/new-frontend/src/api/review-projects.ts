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

export type ApiReviewProjectTextUnit = {
  id: number;
  tmTextUnit: {
    id: number;
    name?: number | string | null;
    content?: string | null;
    comment?: string | null;
    asset?: {
      assetPath?: number | string | null;
      repository?: { id?: number | null; name?: string | null } | null;
    } | null;
    wordCount?: number | null;
  } | null;
  tmTextUnitVariant: {
    id?: number | null;
    content?: string | null;
    status?: string | null;
    includedInLocalizedFile?: boolean | null;
    comment?: string | null;
  } | null;
  reviewProjectTextUnitDecision?: {
    tmTextUnitVariantId?: number | null;
    notes?: string | null;
  } | null;
};

export type ApiReviewProjectDetail = {
  id: number;
  createdDate?: string | null;
  dueDate?: string | null;
  closeReason?: string | null;
  textUnitCount?: number | null;
  wordCount?: number | null;
  type: ApiReviewProjectType;
  status: ApiReviewProjectStatus;
  reviewProjectRequest?: {
    id: number | null;
    name?: string | null;
    screenshotImageIds?: string[];
  } | null;
  locale?: { id: number | null; bcp47Tag?: string | null } | null;
  // New canonical field name from WS
  reviewProjectTextUnits?: ApiReviewProjectTextUnit[];
};

export type SearchReviewProjectsResponse = {
  reviewProjects: ApiReviewProjectSummary[];
};

export type ApiReviewProjectSummary = {
  id: number;
  createdDate?: string | null;
  lastModifiedDate?: string | null;
  dueDate?: string | null;
  closeReason?: string | null;
  textUnitCount?: number | null;
  wordCount?: number | null;
  type: ApiReviewProjectType;
  status: ApiReviewProjectStatus;
  locale?: { id: number | null; bcp47Tag?: string | null } | null;
  reviewProjectRequest?: { id: number | null; name?: string | null } | null;
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
  localeTags: string[];
  notes?: string | null;
  tmTextUnitIds?: number[] | null;
  type?: ApiReviewProjectType | null;
  dueDate: string; // ISO string
  screenshotImageIds?: string[] | null;
  name: string;
};

export type ReviewProjectCreateResponse = {
  requestId: number;
  requestName?: string | null;
  localeTags: string[];
  dueDate: string;
  projectIds: number[];
};

const jsonHeaders = {
  'Content-Type': 'application/json',
};

export const searchReviewProjects = async (
  params: ReviewProjectsSearchRequest,
): Promise<SearchReviewProjectsResponse> => {
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

  return (await response.json()) as SearchReviewProjectsResponse;
};

export const fetchReviewProjects = async (): Promise<ApiReviewProjectSummary[]> => {
  const res = await searchReviewProjects({});
  return res.reviewProjects ?? [];
};

export const createReviewProjectRequest = async (
  payload: ReviewProjectCreateRequest,
): Promise<ReviewProjectCreateResponse> => {
  const response = await fetch('/api/review-project-requests', {
    method: 'POST',
    credentials: 'include',
    headers: jsonHeaders,
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const message = await response.text().catch(() => '');
    throw new Error(message || 'Failed to create review project');
  }

  return (await response.json()) as ReviewProjectCreateResponse;
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
  const response = await fetch(
    `/api/review-projects/${projectId}/text-units/${textUnitId}/accept`,
    {
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
    },
  );

  if (!response.ok) {
    const message = await response.text().catch(() => '');
    const error = new Error(message || 'Failed to accept text unit') as Error & { status?: number };
    error.status = response.status;
    throw error;
  }

  const raw = (await response.json()) as ApiReviewProjectTextUnit;
  return raw;
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
  const response = await fetch(
    `/api/review-projects/${projectId}/text-units/${textUnitId}/review`,
    {
      method: 'POST',
      credentials: 'include',
      headers: jsonHeaders,
      body: JSON.stringify({ notes }),
    },
  );

  if (!response.ok) {
    const message = await response.text().catch(() => '');
    throw new Error(message || 'Failed to update review status');
  }

  const raw = (await response.json()) as ApiReviewProjectTextUnit;
  return raw;
};
