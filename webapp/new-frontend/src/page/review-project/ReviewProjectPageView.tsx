import './review-project-page.css';

import { useRef } from 'react';
import { VirtualItem } from '@tanstack/react-virtual';

import type { ApiReviewProjectDetail } from '../../api/review-projects';
import { VirtualList } from '../../components/virtual/VirtualList';

type Props = {
  projectId: number;
  project: ApiReviewProjectDetail | null;
};

export function ReviewProjectPageView({ projectId, project }: Props) {
  const scrollRef = useRef<HTMLDivElement>(null);

  if (!project) {
    return <div>No project data for id {projectId}</div>;
  }

  return (
    <div className="review-project-page">
      <header className="review-project-page__header">
        <h1 className="review-project-page__title">{project.name ?? `Project ${projectId}`}</h1>
        <p className="review-project-page__meta">
          TODO: summary stats (word count, due date, progress, current edit status)
        </p>
      </header>

      <div className="review-project-page__content">
        <section className="review-project-page__list-pane" ref={scrollRef}>
          <div className="review-project-page__search">TODO: search bar for strings</div>
          <VirtualList
            scrollRef={scrollRef}
            items={[]}
            totalSize={0}
            renderRow={(virtualItem: VirtualItem) => ({
              key: virtualItem.key,
              content: <div>TODO: text unit row</div>,
            })}
          />
        </section>

        <section className="review-project-page__detail-pane">
          TODO: right pane with string details/editor
        </section>
      </div>
    </div>
  );
}
