import './review-project-page-v2.css';

import type { ApiReviewProjectDetail } from '../../api/review-projects';

type Props = {
  projectId: number;
  project: ApiReviewProjectDetail | null;
};

export function ReviewProjectPageViewV2({ projectId, project }: Props) {
  if (!project) {
    return <div className="review-project-page-v2__empty">No project data for id {projectId}</div>;
  }

  return (
    <div className="review-project-page-v2">
      <header className="review-project-page-v2__header">
        <div className="review-project-page-v2__header-top">
          <div className="review-project-page-v2__title-row">
            <span className="review-project-page-v2__title">
              {project.reviewProjectRequest?.name ?? `Project ${projectId}`}
            </span>
          </div>
        </div>
        <div className="review-project-page-v2__meta-row" />
      </header>

      <div className="review-project-page-v2__body">
        <aside className="review-project-page-v2__list">
          <div className="review-project-page-v2__placeholder">List area (add components)</div>
        </aside>

        <main className="review-project-page-v2__detail">
          <div className="review-project-page-v2__placeholder">
            Detail area (add components for selected item)
          </div>
        </main>
      </div>
    </div>
  );
}
