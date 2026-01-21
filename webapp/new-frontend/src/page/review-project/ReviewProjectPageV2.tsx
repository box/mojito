import { useParams } from 'react-router-dom';

import { useReviewProjectDetail } from '../../hooks/useReviewProjectDetail';
import { ReviewProjectPageViewV2 } from './ReviewProjectPageViewV2';

export function ReviewProjectPageV2() {
  const { projectId: projectIdParam } = useParams<{ projectId: string }>();
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

  return (
    <ReviewProjectPageViewV2 projectId={projectId} project={projectDetailQuery.data ?? null} />
  );
}
