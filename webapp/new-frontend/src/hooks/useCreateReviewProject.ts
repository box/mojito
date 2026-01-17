import { useMutation, useQueryClient } from '@tanstack/react-query';

import type { ApiReviewProjectSummary, ReviewProjectCreateRequest } from '../api/review-projects';
import { createReviewProject } from '../api/review-projects';
import { REVIEW_PROJECTS_QUERY_KEY } from './useReviewProjects';

export function useCreateReviewProject() {
  const queryClient = useQueryClient();
  return useMutation<ApiReviewProjectSummary[], Error, ReviewProjectCreateRequest>({
    mutationFn: (payload) => createReviewProject(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [REVIEW_PROJECTS_QUERY_KEY] });
    },
  });
}
