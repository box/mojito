import './review-projects-page.css';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';

import type { CollectionOption } from '../../components/CollectionSelect';
import { useCreateReviewProject } from '../../hooks/useCreateReviewProject';
import { useRepositories } from '../../hooks/useRepositories';
import { useLocaleOptionsWithDisplayNames } from '../../utils/localeSelection';
import { useWorkbenchCollections } from '../workbench/useWorkbenchCollections';
import type { ReviewProjectCreateFormValues } from './ReviewProjectCreateForm';
import { ReviewProjectCreateForm } from './ReviewProjectCreateForm';

function toLocalInput(value: Date) {
  const tzOffset = value.getTimezoneOffset() * 60000;
  const local = new Date(value.getTime() - tzOffset);
  return local.toISOString().slice(0, 16);
}

type ReviewProjectNavState = {
  tmTextUnitIds?: number[];
  collectionName?: string | null;
  collectionId?: string | null;
  defaultName?: string;
  defaultDueDate?: string;
};

function isReviewProjectNavState(value: unknown): value is ReviewProjectNavState {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const candidate = value as Partial<ReviewProjectNavState>;
  const isNumberArray = (input: unknown): input is number[] =>
    Array.isArray(input) && input.every((item) => typeof item === 'number');
  const isOptionalString = (input: unknown): input is string | null | undefined =>
    input === undefined || input === null || typeof input === 'string';

  return (
    (candidate.tmTextUnitIds === undefined || isNumberArray(candidate.tmTextUnitIds)) &&
    isOptionalString(candidate.collectionName) &&
    isOptionalString(candidate.collectionId) &&
    (candidate.defaultName === undefined || typeof candidate.defaultName === 'string') &&
    (candidate.defaultDueDate === undefined || typeof candidate.defaultDueDate === 'string')
  );
}

export function ReviewProjectCreatePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { data: repositories = [] } = useRepositories();
  const { collections, activeCollection } = useWorkbenchCollections();
  const [tmIds, setTmIds] = useState<number[]>([]);
  const [selectedCollectionId, setSelectedCollectionId] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [prefillName, setPrefillName] = useState('Review project');
  const [prefillDueDate, setPrefillDueDate] = useState<string | null>(null);
  const [prefillCollectionName, setPrefillCollectionName] = useState<string | null>(null);

  const createReviewProject = useCreateReviewProject();

  const defaultDueDate = useMemo(
    () => toLocalInput(new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)),
    [],
  );

  const localeOptions = useLocaleOptionsWithDisplayNames(repositories, undefined);
  const collectionOptions = useMemo<CollectionOption[]>(
    () =>
      [...collections]
        .filter((collection) => collection.entries.length > 0)
        .sort((a, b) => b.updatedAt - a.updatedAt)
        .map((collection) => ({
          id: collection.id,
          name: collection.name || 'Untitled collection',
          size: collection.entries.length,
        })),
    [collections],
  );
  const hasCollections = collectionOptions.length > 0;

  useEffect(() => {
    const state = isReviewProjectNavState(location.state) ? location.state : null;
    if (!state) {
      return;
    }
    if (state.tmTextUnitIds?.length) {
      const unique = Array.from(new Set(state.tmTextUnitIds));
      setTmIds(unique);
    }
    if (state.collectionName) {
      setPrefillCollectionName(state.collectionName);
    }
    if (state.collectionId) {
      setSelectedCollectionId(state.collectionId);
    }
    if (state.defaultName) {
      setPrefillName(state.defaultName);
    }
    if (state.defaultDueDate) {
      setPrefillDueDate(state.defaultDueDate);
    }
  }, [location.state]);

  useEffect(() => {
    if (selectedCollectionId === null) {
      return;
    }
    const collection = collections.find((item) => item.id === selectedCollectionId);
    if (!collection) {
      return;
    }
    if (collection.entries.length === 0) {
      setTmIds([]);
      setPrefillCollectionName(collection.name);
      return;
    }
    const nextTmIds = Array.from(new Set(collection.entries.map((entry) => entry.tmTextUnitId)));
    setTmIds(nextTmIds);
    setPrefillCollectionName(collection.name);
  }, [collections, selectedCollectionId]);

  useEffect(() => {
    if (selectedCollectionId || tmIds.length || !activeCollection) {
      return;
    }
    if (activeCollection.entries.length === 0) {
      return;
    }
    setSelectedCollectionId(activeCollection.id);
  }, [activeCollection, selectedCollectionId, tmIds.length]);

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
          localeTags: values.localeTags,
          notes: values.notes,
          type: values.type,
          dueDate: values.dueDate,
          tmTextUnitIds: tmIds,
          screenshotImageIds: values.screenshotImageIds,
          name: values.name,
        },
        {
          onSuccess: (response) => {
            const requestId = response.requestId ?? null;
            void navigate('/review-projects', {
              state: { requestId },
            });
          },
          onError: (err: unknown) => {
            const message = err instanceof Error ? err.message : typeof err === 'string' ? err : '';
            setErrorMessage(message.trim() || 'Failed to create project');
          },
        },
      );
    },
    [createReviewProject, navigate, tmIds],
  );

  return (
    <div className="review-projects-page review-projects-create">
      <div className="review-projects-page__bar">
        <div className="review-projects-page__summary-bar" style={{ width: '100%' }}>
          <div className="modal__title">New review project</div>
        </div>
      </div>

      <div className="review-create__page-shell">
        {hasCollections ? (
          <ReviewProjectCreateForm
            defaultName={prefillName || 'Review project'}
            defaultDueDate={prefillDueDate ?? defaultDueDate}
            localeOptions={localeOptions}
            tmTextUnitIds={tmIds}
            collectionName={prefillCollectionName ?? null}
            collectionOptions={collectionOptions}
            selectedCollectionId={selectedCollectionId}
            onChangeCollection={(id) => {
              setSelectedCollectionId(id);
              if (!id) {
                setPrefillCollectionName(null);
              }
            }}
            isSubmitting={createReviewProject.isPending}
            errorMessage={errorMessage}
            submitLabel="Create"
            onSubmit={handleSubmit}
            onCancel={() => {
              void navigate(-1);
            }}
          />
        ) : (
          <div className="review-create__empty">
            <div className="review-create__empty-title">No collections yet</div>
            <div className="review-create__empty-body">
              Review projects are created from Workbench collections.
            </div>
            <Link className="review-create__empty-cta" to="/workbench">
              Open Workbench
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}
