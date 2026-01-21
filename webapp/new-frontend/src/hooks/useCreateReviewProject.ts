import { useMutation, useQueryClient } from '@tanstack/react-query';

import type {
  ReviewProjectCreateRequest,
  ReviewProjectCreateResponse,
} from '../api/review-projects';
import { createReviewProjectRequest } from '../api/review-projects';
import { REVIEW_PROJECTS_QUERY_KEY } from './useReviewProjects';

export function useCreateReviewProject() {
  const queryClient = useQueryClient();
  return useMutation<ReviewProjectCreateResponse, Error, ReviewProjectCreateRequest>({
    mutationFn: (payload) => createReviewProjectRequest(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [REVIEW_PROJECTS_QUERY_KEY] });
    },
  });
}
