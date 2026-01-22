import { useParams } from 'react-router-dom';

import { useReviewProjectDetail } from '../../hooks/useReviewProjectDetail';
import { ReviewProjectPageViewV2 } from './ReviewProjectPageViewV2';

export function ReviewProjectPageV2() {
  const { projectId: projectIdParam } = useParams<{ projectId: string }>();
  const projectId = projectIdParam ? Number(projectIdParam) : undefined;
  const projectDetailQuery = useReviewProjectDetail(projectId);

  if (!projectId) {
    return <ErrorState message="Missing project id." />;
  }

  if (projectDetailQuery.isLoading) {
    return <LoadingState />;
  }

  if (projectDetailQuery.isError) {
    const message =
      projectDetailQuery.error instanceof Error
        ? projectDetailQuery.error.message
        : 'Failed to load project';
    return <ErrorState message={message} />;
  }

  return (
    <ReviewProjectPageViewV2 projectId={projectId} project={projectDetailQuery.data ?? null} />
  );
}

function LoadingState() {
  return (
    <div className="review-project-page__state">
      <div className="spinner spinner--md" aria-hidden />
      <div>Loading project…</div>
    </div>
  );
}

function ErrorState({ message }: { message: string }) {
  return (
    <div className="review-project-page__state review-project-page__state--error">
      <div>{message}</div>
    </div>
  );
}
