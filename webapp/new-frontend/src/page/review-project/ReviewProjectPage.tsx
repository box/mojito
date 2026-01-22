import { useParams } from 'react-router-dom';

import { useReviewProjectDetail } from '../../hooks/useReviewProjectDetail';
import { ReviewProjectPageView } from './ReviewProjectPageView';

export function ReviewProjectPage() {
  const { projectId: projectIdParam } = useParams<{ projectId: string }>();

  // Convert the route param to a number once; hook accepts number | undefined.
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

  return <ReviewProjectPageView projectId={projectId} project={projectDetailQuery.data ?? null} />;
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
