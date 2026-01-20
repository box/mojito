import './review-projects-page.css';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

import { useCreateReviewProject } from '../../hooks/useCreateReviewProject';
import { useRepositories } from '../../hooks/useRepositories';
import { useLocaleOptionsWithDisplayNames } from '../../utils/localeSelection';
import type { ReviewProjectCreateFormValues } from './ReviewProjectCreateForm';
import { ReviewProjectCreateForm } from './ReviewProjectCreateForm';

function toLocalInput(value: Date) {
  const tzOffset = value.getTimezoneOffset() * 60000;
  const local = new Date(value.getTime() - tzOffset);
  return local.toISOString().slice(0, 16);
}

export function ReviewProjectCreatePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { data: repositories = [] } = useRepositories();
  const [repositoryIds, setRepositoryIds] = useState<number[]>([]);
  const [tmIds, setTmIds] = useState<number[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [prefillName, setPrefillName] = useState('Review project');
  const [prefillDueDate, setPrefillDueDate] = useState<string | null>(null);
  const [prefillCollectionName, setPrefillCollectionName] = useState<string | null>(null);

  const createReviewProject = useCreateReviewProject();

  const defaultDueDate = useMemo(
    () => toLocalInput(new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)),
    [],
  );

  const collectionSize = tmIds.length || 0;
  const localeOptions = useLocaleOptionsWithDisplayNames(
    repositories,
    repositoryIds.length ? new Set(repositoryIds) : undefined,
  );

  useEffect(() => {
    const state = location.state as
      | {
          repositoryIds?: number[];
          tmTextUnitIds?: number[];
          collectionName?: string | null;
          defaultName?: string;
          defaultDueDate?: string;
        }
      | null;
    if (!state) {
      return;
    }
    if (state.repositoryIds?.length) {
      setRepositoryIds(state.repositoryIds);
    }
    if (state.tmTextUnitIds?.length) {
      const unique = Array.from(new Set(state.tmTextUnitIds));
      setTmIds(unique);
    }
    if (state.collectionName) {
      setPrefillCollectionName(state.collectionName);
    }
    if (state.defaultName) {
      setPrefillName(state.defaultName);
    }
    if (state.defaultDueDate) {
      setPrefillDueDate(state.defaultDueDate);
    }
  }, [location.state]);

  useEffect(() => {
    if (!repositoryIds.length && repositories.length) {
      setRepositoryIds(repositories.map((repo) => repo.id));
    }
  }, [repositories, repositoryIds.length]);

  const handleSubmit = useCallback(
    (values: ReviewProjectCreateFormValues) => {
      if (createReviewProject.isPending) return;
      if (!tmIds.length) {
        setErrorMessage('Add at least one text unit id.');
        return;
      }
      setErrorMessage(null);
      createReviewProject.mutate(
        {
          repositoryIds,
          localeTags: values.localeTags,
          maxTextUnits: values.maxTextUnits ?? tmIds.length,
          maxWordCount: null,
          notes: values.notes,
          type: values.type,
          dueDate: values.dueDate,
          tmTextUnitIds: tmIds,
          screenshotImageIds: values.screenshotImageIds,
          name: values.name,
        },
        {
          onSuccess: (summaries) => {
            const firstId = summaries[0]?.id;
            void navigate(firstId ? `/review-projects/${firstId}` : '/review-projects');
          },
          onError: (err) => {
            setErrorMessage(err instanceof Error ? err.message : 'Failed to create project');
          },
        },
      );
    },
    [createReviewProject, navigate, repositoryIds, tmIds],
  );

  return (
    <div className="review-projects-page review-projects-create">
      <div className="review-projects-page__bar">
        <div className="review-projects-page__summary-bar" style={{ width: '100%' }}>
          <div className="modal__title">New review project</div>
        </div>
      </div>

      <div className="review-create__page-shell">
        <ReviewProjectCreateForm
          defaultName={prefillName || 'Review project'}
          defaultDueDate={prefillDueDate ?? defaultDueDate}
          localeOptions={localeOptions}
          collectionSize={collectionSize || 1}
          tmTextUnitIds={tmIds}
          collectionName={prefillCollectionName ?? null}
          collectionOptions={undefined}
          selectedCollectionId={null}
          onChangeCollection={undefined}
          isSubmitting={createReviewProject.isPending}
          errorMessage={errorMessage}
          submitLabel="Create"
          onSubmit={handleSubmit}
          onCancel={() => {
            void navigate(-1);
          }}
        />
      </div>
    </div>
  );
}
