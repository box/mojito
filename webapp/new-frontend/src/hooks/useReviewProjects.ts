import { keepPreviousData, useQuery } from '@tanstack/react-query';

import type { ApiReviewProjectSummary, ReviewProjectsSearchRequest } from '../api/review-projects';
import { searchReviewProjects } from '../api/review-projects';

const REVIEW_PROJECTS_QUERY_KEY = ['review-projects'];

type UseReviewProjectsOptions = {
  enabled?: boolean;
};

export const useReviewProjects = (
  params: ReviewProjectsSearchRequest,
  options?: UseReviewProjectsOptions,
) => {
  return useQuery<ApiReviewProjectSummary[]>({
    queryKey: [REVIEW_PROJECTS_QUERY_KEY, params],
    queryFn: async () => {
      const result = await searchReviewProjects(params);
      return result.reviewProjects ?? [];
    },
    placeholderData: keepPreviousData,
    staleTime: 30_000,
    ...options,
  });
};

export { REVIEW_PROJECTS_QUERY_KEY };
