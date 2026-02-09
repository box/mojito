import { useQuery } from '@tanstack/react-query';

import type { ApiReviewProjectDetail } from '../api/review-projects';
import { fetchReviewProjectDetail } from '../api/review-projects';

const REVIEW_PROJECT_DETAIL_QUERY_KEY = ['review-project'];

export function useReviewProjectDetail(projectId: number | undefined) {
  return useQuery<ApiReviewProjectDetail>({
    queryKey: projectId
      ? [...REVIEW_PROJECT_DETAIL_QUERY_KEY, projectId]
      : REVIEW_PROJECT_DETAIL_QUERY_KEY,
    enabled: projectId != null,
    queryFn: () => {
      if (projectId == null) {
        throw new Error('Missing project id');
      }
      return fetchReviewProjectDetail(projectId);
    },
    staleTime: 30_000,
  });
}

export { REVIEW_PROJECT_DETAIL_QUERY_KEY };
