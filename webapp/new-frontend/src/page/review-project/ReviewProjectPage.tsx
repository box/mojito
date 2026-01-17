import { useParams } from 'react-router-dom';

import { useReviewProjectDetail } from '../../hooks/useReviewProjectDetail';
import { ReviewProjectPageView } from './ReviewProjectPageView';

export function ReviewProjectPage() {
  const { projectId: projectIdParam } = useParams<{ projectId: string }>();

  // Convert the route param to a number once; hook accepts number | undefined.
  const projectId = projectIdParam ? Number(projectIdParam) : undefined;
  const projectDetailQuery = useReviewProjectDetail(projectId);

  if (!projectId) {
    return <div>Missing project id</div>;
  }

  if (projectDetailQuery.isLoading) {
    return <div>Loading project…</div>;
  }

  if (projectDetailQuery.isError) {
    const message =
      projectDetailQuery.error instanceof Error
        ? projectDetailQuery.error.message
        : 'Failed to load project';
    return <div>{message}</div>;
  }

  return <ReviewProjectPageView projectId={projectId} project={projectDetailQuery.data ?? null} />;
}
