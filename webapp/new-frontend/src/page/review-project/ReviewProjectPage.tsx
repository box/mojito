import { useMemo } from 'react';
import { useParams } from 'react-router-dom';

import { useReviewProjectDetail } from '../../hooks/useReviewProjectDetail';
import { ReviewProjectPageView } from './ReviewProjectPageView';

export function ReviewProjectPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const numericId = useMemo(() => {
    if (!projectId) return undefined;
    const parsed = Number(projectId);
    return Number.isFinite(parsed) ? parsed : undefined;
  }, [projectId]);

  const {
    data: project,
    isLoading,
    isError,
    error,
    refetch,
  } = useReviewProjectDetail(numericId);

  const status: 'loading' | 'error' | 'ready' =
    isLoading ? 'loading' : isError ? 'error' : 'ready';

  const errorMessage =
    error instanceof Error ? error.message : 'Unable to load review project details.';

  return (
    <ReviewProjectPageView
      status={status}
      project={project}
      errorMessage={status === 'error' ? errorMessage : undefined}
      onRetry={() => refetch()}
    />
  );
}
