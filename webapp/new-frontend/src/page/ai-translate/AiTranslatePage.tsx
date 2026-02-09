import '../review-projects/review-projects-page.css';
import './ai-translate-page.css';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

import {
  fetchAiTranslateReport,
  fetchAiTranslateReportLocale,
  fetchAiTranslateReportPath,
  type PollableTask,
  translateRepository,
  waitForPollableTaskToFinish,
} from '../../api/ai-translate';
import { type CollectionOption, CollectionSelect } from '../../components/CollectionSelect';
import type { LocaleOption } from '../../components/LocaleMultiSelect';
import { LocaleMultiSelect } from '../../components/LocaleMultiSelect';
import { SingleSelectDropdown } from '../../components/SingleSelectDropdown';
import { useRepositories } from '../../hooks/useRepositories';
import { useLocaleDisplayNameResolver } from '../../utils/localeDisplayNames';
import { getNonRootRepositoryLocaleTags } from '../../utils/repositoryLocales';
import { useWorkbenchCollections } from '../workbench/useWorkbenchCollections';

const RELATED_STRINGS_OPTIONS = ['NONE', 'USAGES', 'ID_PREFIX'];
const TRANSLATE_TYPE_OPTIONS = ['WITH_REVIEW', 'TARGET_ONLY', 'TARGET_ONLY_NEW'];
const STATUS_FILTER_OPTIONS = ['FOR_TRANSLATION', 'ALL'];
const IMPORT_STATUS_OPTIONS = ['REVIEW_NEEDED', 'APPROVED', 'TRANSLATION_NEEDED'];
const SOURCE_TEXT_SIZES = [
  { value: 50, label: '50' },
  { value: 100, label: '100' },
  { value: 500, label: '500' },
  { value: 1000, label: '1k' },
];

const DEFAULT_SOURCE_TEXT_MAX = 100;

const parseInteger = (raw: string, { allowEmpty, min }: { allowEmpty: boolean; min: number }) => {
  if (raw === '') {
    if (allowEmpty) {
      return null;
    }
    throw new Error('missingNumber');
  }
  const parsed = Number(raw);
  if (!Number.isFinite(parsed) || !Number.isInteger(parsed)) {
    throw new Error('invalidNumber');
  }
  if (parsed < min) {
    throw new Error('invalidNumber');
  }
  return parsed;
};

type ReportDownload = {
  locale: string;
  filename: string;
  href: string;
};

export function AiTranslatePage() {
  const { data: repositories, isLoading, error } = useRepositories();
  const resolveLocaleName = useLocaleDisplayNameResolver();
  const { collections } = useWorkbenchCollections();
  const [searchParams] = useSearchParams();

  const [selectedRepositoryId, setSelectedRepositoryId] = useState<number | null>(null);
  const [selectedLocales, setSelectedLocales] = useState<string[]>([]);
  const [selectedCollectionId, setSelectedCollectionId] = useState<string | null>(null);
  const [sourceTextMaxCount, setSourceTextMaxCount] = useState(String(DEFAULT_SOURCE_TEXT_MAX));
  const [useModel, setUseModel] = useState('gpt-4.1');
  const [promptSuffix, setPromptSuffix] = useState('');
  const [relatedStrings, setRelatedStrings] = useState('NONE');
  const [translateType, setTranslateType] = useState('TARGET_ONLY_NEW');
  const [statusFilter, setStatusFilter] = useState('FOR_TRANSLATION');
  const [importStatus, setImportStatus] = useState('REVIEW_NEEDED');
  const [timeoutSeconds, setTimeoutSeconds] = useState('');
  const [downloadReport, setDownloadReport] = useState(false);
  const [dryRun, setDryRun] = useState(false);
  const [pollableTask, setPollableTask] = useState<PollableTask | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isWaiting, setIsWaiting] = useState(false);
  const [jobError, setJobError] = useState<string | null>(null);
  const [reportError, setReportError] = useState<string | null>(null);
  const [isFetchingReport, setIsFetchingReport] = useState(false);
  const [reportDownloads, setReportDownloads] = useState<ReportDownload[]>([]);

  const requestRef = useRef(0);
  const objectUrlsRef = useRef<string[]>([]);

  useEffect(() => {
    return () => {
      objectUrlsRef.current.forEach((url) => URL.revokeObjectURL(url));
      objectUrlsRef.current = [];
    };
  }, []);

  const repositoryOptions = useMemo(() => {
    if (!repositories) {
      return [];
    }
    return repositories
      .map((repo) => ({ id: repo.id, name: repo.name }))
      .sort((a, b) => a.name.localeCompare(b.name));
  }, [repositories]);

  const repositorySelectOptions = useMemo(
    () => repositoryOptions.map((repo) => ({ value: repo.id, label: repo.name })),
    [repositoryOptions],
  );

  const clearRunState = useCallback(() => {
    setJobError(null);
    setReportError(null);
    setPollableTask(null);
    setReportDownloads([]);
    setIsFetchingReport(false);
    objectUrlsRef.current.forEach((url) => URL.revokeObjectURL(url));
    objectUrlsRef.current = [];
  }, []);

  useEffect(() => {
    const repositoryParam = searchParams.get('repositoryId');
    const collectionParam = searchParams.get('collectionId');

    if (repositoryParam) {
      const parsed = Number(repositoryParam);
      if (Number.isFinite(parsed) && parsed !== selectedRepositoryId) {
        setSelectedRepositoryId(parsed);
        setSelectedLocales([]);
        clearRunState();
      }
    }

    if (collectionParam && collectionParam !== selectedCollectionId) {
      setSelectedCollectionId(collectionParam);
      clearRunState();
    }
  }, [clearRunState, searchParams, selectedCollectionId, selectedRepositoryId]);

  useEffect(() => {
    if (!selectedRepositoryId || isLoading) {
      return;
    }
    if (repositoryOptions.length === 0) {
      return;
    }
    const exists = repositoryOptions.some((repo) => repo.id === selectedRepositoryId);
    if (!exists) {
      setSelectedRepositoryId(null);
    }
  }, [isLoading, repositoryOptions, selectedRepositoryId]);

  const selectedRepository = useMemo(
    () => repositoryOptions.find((repo) => repo.id === selectedRepositoryId) ?? null,
    [repositoryOptions, selectedRepositoryId],
  );

  const selectedRepositoryData = useMemo(
    () => repositories?.find((repo) => repo.id === selectedRepositoryId) ?? null,
    [repositories, selectedRepositoryId],
  );

  const localeOptions = useMemo<LocaleOption[]>(() => {
    if (!selectedRepositoryData) {
      return [];
    }
    return getNonRootRepositoryLocaleTags(selectedRepositoryData)
      .sort((a, b) => a.localeCompare(b, undefined, { sensitivity: 'base' }))
      .map((tag) => ({ tag, label: resolveLocaleName(tag) }));
  }, [resolveLocaleName, selectedRepositoryData]);

  const collectionOptions = useMemo<CollectionOption[]>(() => {
    return collections
      .filter((collection) => collection.entries.length > 0)
      .slice()
      .sort((a, b) => a.name.localeCompare(b.name))
      .map((collection) => ({
        id: collection.id,
        name: collection.name || 'Untitled collection',
        size: collection.entries.length,
      }));
  }, [collections]);

  useEffect(() => {
    if (!selectedCollectionId) {
      return;
    }
    const exists = collectionOptions.some((option) => option.id === selectedCollectionId);
    if (!exists) {
      setSelectedCollectionId(null);
    }
  }, [collectionOptions, selectedCollectionId]);

  const selectedCollection = useMemo(() => {
    if (!selectedCollectionId) {
      return null;
    }
    return collections.find((collection) => collection.id === selectedCollectionId) ?? null;
  }, [collections, selectedCollectionId]);

  const collectionRepositoryIds = useMemo(() => {
    if (!selectedCollection) {
      return [] as number[];
    }
    const ids = new Set(
      selectedCollection.entries
        .map((entry) => entry.repositoryId)
        .filter((id): id is number => typeof id === 'number'),
    );
    return Array.from(ids);
  }, [selectedCollection]);

  const collectionSummary = useMemo(() => {
    if (!selectedCollection) {
      return {
        tmTextUnitIds: [] as number[],
        totalEntries: 0,
        matchingEntries: 0,
        mismatchedEntries: 0,
      };
    }
    const entries = selectedCollection.entries;
    const matchingEntries =
      selectedRepositoryId === null
        ? entries
        : entries.filter(
            (entry) => entry.repositoryId === null || entry.repositoryId === selectedRepositoryId,
          );
    const mismatchedEntries =
      selectedRepositoryId === null
        ? 0
        : entries.filter(
            (entry) => entry.repositoryId !== null && entry.repositoryId !== selectedRepositoryId,
          ).length;

    return {
      tmTextUnitIds: matchingEntries.map((entry) => entry.tmTextUnitId),
      totalEntries: entries.length,
      matchingEntries: matchingEntries.length,
      mismatchedEntries,
    };
  }, [selectedCollection, selectedRepositoryId]);

  const disableForm = isSubmitting || isWaiting;
  const usesCollections = Boolean(selectedCollectionId);

  useEffect(() => {
    if (!selectedCollection || selectedRepositoryId) {
      return;
    }
    if (collectionRepositoryIds.length === 1) {
      setSelectedRepositoryId(collectionRepositoryIds[0]);
      setSelectedLocales([]);
    }
  }, [collectionRepositoryIds, selectedCollection, selectedRepositoryId]);

  const handleRepositoryChange = (repoId: number | null) => {
    if (repoId === null) {
      setSelectedRepositoryId(null);
      setSelectedLocales([]);
      clearRunState();
      return;
    }
    setSelectedRepositoryId(repoId);
    setSelectedLocales([]);
    clearRunState();
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    clearRunState();

    if (!selectedRepository) {
      setJobError('Select a repository before starting the translation.');
      return;
    }

    let parsedSourceTextMaxCount: number;
    let parsedTimeoutSeconds: number | null;

    try {
      parsedSourceTextMaxCount =
        parseInteger(sourceTextMaxCount.trim(), { allowEmpty: false, min: 1 }) ??
        DEFAULT_SOURCE_TEXT_MAX;
      parsedTimeoutSeconds =
        parseInteger(timeoutSeconds.trim(), { allowEmpty: true, min: 0 }) ?? null;
    } catch (error) {
      const message = (error as Error).message;
      if (message === 'invalidNumber' || message === 'missingNumber') {
        setJobError('Provide valid numeric values greater than zero.');
        return;
      }
      setJobError('Unexpected error while validating the form.');
      return;
    }

    const tmTextUnitIds = collectionSummary.tmTextUnitIds.length
      ? collectionSummary.tmTextUnitIds
      : null;

    if (usesCollections && (!tmTextUnitIds || tmTextUnitIds.length === 0)) {
      setJobError('Selected collection has no text units for this repository.');
      return;
    }

    setIsSubmitting(true);
    setIsWaiting(false);
    setJobError(null);
    setReportError(null);

    const currentRequest = requestRef.current + 1;
    requestRef.current = currentRequest;

    try {
      const pollable = await translateRepository({
        repositoryName: selectedRepository.name,
        targetBcp47tags: selectedLocales.length ? selectedLocales : null,
        sourceTextMaxCountPerLocale: parsedSourceTextMaxCount,
        tmTextUnitIds,
        useBatch: false,
        useModel: useModel.trim() ? useModel.trim() : null,
        promptSuffix: promptSuffix.trim() ? promptSuffix.trim() : null,
        relatedStringsType: relatedStrings,
        translateType,
        statusFilter,
        importStatus,
        glossaryName: null,
        glossaryTermSource: null,
        glossaryTermSourceDescription: null,
        glossaryTermTarget: null,
        glossaryTermTargetDescription: null,
        glossaryTermDoNotTranslate: false,
        glossaryTermCaseSensitive: false,
        glossaryOnlyMatchedTextUnits: false,
        dryRun,
        timeoutSeconds: parsedTimeoutSeconds,
      });

      if (requestRef.current !== currentRequest) {
        return;
      }

      setPollableTask(pollable);
      setIsSubmitting(false);
      setIsWaiting(true);

      const completedTask = await waitForPollableTaskToFinish(pollable.id);
      if (requestRef.current !== currentRequest) {
        return;
      }

      setPollableTask(completedTask);
      setIsWaiting(false);

      if (completedTask.errorMessage) {
        setJobError(String(completedTask.errorMessage));
        return;
      }

      if (downloadReport) {
        await fetchReportDownloads(completedTask.id, currentRequest);
      }
    } catch (err) {
      if (requestRef.current !== currentRequest) {
        return;
      }
      const errorMessage =
        err instanceof Error ? err.message : 'Unexpected error while submitting.';
      setJobError(String(errorMessage || 'Unexpected error while submitting.'));
      setIsSubmitting(false);
      setIsWaiting(false);
    }
  };

  const fetchReportDownloads = useCallback(async (pollableTaskId: number, requestToken: number) => {
    setIsFetchingReport(true);
    setReportError(null);
    setReportDownloads([]);

    try {
      const report = await fetchAiTranslateReport(pollableTaskId);
      const reportLocaleUrls = report.reportLocaleUrls ?? [];

      const downloads = await Promise.all(
        reportLocaleUrls.map(async (path) => {
          const locale = path.split('/').pop() || 'report';
          let contentResponse;

          try {
            contentResponse = await fetchAiTranslateReportPath(path);
          } catch {
            contentResponse = await fetchAiTranslateReportLocale(pollableTaskId, locale);
          }

          const content = contentResponse.content ?? '';
          const blob = new Blob([content], { type: 'application/json' });
          const href = URL.createObjectURL(blob);
          objectUrlsRef.current.push(href);
          return {
            locale,
            href,
            filename: `${pollableTaskId}-${locale}.json`,
          };
        }),
      );

      if (requestRef.current !== requestToken) {
        return;
      }

      setReportDownloads(downloads);
      setIsFetchingReport(false);
    } catch {
      if (requestRef.current !== requestToken) {
        return;
      }
      setReportError('Unable to download the report files.');
      setIsFetchingReport(false);
    }
  }, []);

  const reportStatus = pollableTask && pollableTask.isAllFinished && !jobError;

  return (
    <div className="review-projects-page review-projects-create ai-translate-page">
      <div className="review-projects-page__bar">
        <div className="review-projects-page__summary-bar" style={{ width: '100%' }}>
          <div className="modal__title">AI Translate</div>
        </div>
      </div>

      <div className="review-create__page-shell">
        <form
          className="review-create__body"
          onSubmit={(event) => {
            void handleSubmit(event);
          }}
        >
          <div className="ai-translate-grid">
            <div className="review-create__stack">
              <div className="review-create__field">
                <span className="review-create__label">Repository</span>
                <SingleSelectDropdown
                  label="Repository"
                  options={repositorySelectOptions}
                  value={selectedRepositoryId}
                  onChange={handleRepositoryChange}
                  placeholder={
                    isLoading
                      ? 'Loading repositories...'
                      : error
                        ? 'Unable to load repositories'
                        : 'Select repository'
                  }
                  disabled={disableForm || isLoading}
                  className="ai-translate-repo-select"
                  searchPlaceholder="Search repositories"
                  noneLabel="Clear selection"
                />
                {error ? (
                  <div className="review-create__hint ai-translate-hint-error">
                    Unable to load repositories.
                  </div>
                ) : null}
              </div>

              <div className="review-create__field">
                <span className="review-create__label">Target locales</span>
                <LocaleMultiSelect
                  className="review-create__locale-select"
                  label="Locales"
                  options={localeOptions}
                  selectedTags={selectedLocales}
                  onChange={setSelectedLocales}
                  disabled={disableForm || !selectedRepository}
                />
                <div className="review-create__hint">Leave empty to translate all locales.</div>
              </div>

              <div className="review-create__field">
                <span className="review-create__label">Collection</span>
                <CollectionSelect
                  options={collectionOptions}
                  value={selectedCollectionId}
                  onChange={(id) => {
                    setSelectedCollectionId(id);
                    clearRunState();
                  }}
                  disabled={disableForm}
                  className="review-create__select"
                  noneLabel="All text units"
                />
                <div className="review-create__hint">
                  Optional. Uses source texts per locale when empty.
                </div>
                {selectedCollection ? (
                  <div className="review-create__hint">
                    {collectionSummary.matchingEntries} of {collectionSummary.totalEntries} ids in
                    collection.
                    {collectionSummary.mismatchedEntries > 0
                      ? ` ${collectionSummary.mismatchedEntries} ids belong to other repositories.`
                      : ''}
                  </div>
                ) : null}
                {selectedCollection &&
                !selectedRepositoryId &&
                collectionRepositoryIds.length > 1 ? (
                  <div className="review-create__hint">
                    Collection includes multiple repositories. Select one to translate.
                  </div>
                ) : null}
              </div>

              <label className="review-create__field" htmlFor="sourceTextMaxCount">
                <span className="review-create__label">Source texts per locale</span>
                <input
                  id="sourceTextMaxCount"
                  type="number"
                  min={1}
                  className="review-create__input"
                  value={sourceTextMaxCount}
                  onChange={(event) => setSourceTextMaxCount(event.target.value)}
                  disabled={disableForm}
                />
                <div className="ai-translate-quick-sizes">
                  {SOURCE_TEXT_SIZES.map((size) => (
                    <button
                      key={size.value}
                      type="button"
                      className={`ai-translate-quick-size${
                        Number(sourceTextMaxCount) === size.value ? ' is-active' : ''
                      }`}
                      onClick={() => setSourceTextMaxCount(String(size.value))}
                      disabled={disableForm}
                    >
                      {size.label}
                    </button>
                  ))}
                </div>
              </label>
            </div>

            <div className="review-create__stack">
              <label className="review-create__field" htmlFor="useModel">
                <span className="review-create__label">Model override</span>
                <input
                  id="useModel"
                  type="text"
                  className="review-create__input"
                  value={useModel}
                  onChange={(event) => setUseModel(event.target.value)}
                  disabled={disableForm}
                />
              </label>

              <label className="review-create__field" htmlFor="promptSuffix">
                <span className="review-create__label">Prompt suffix</span>
                <textarea
                  id="promptSuffix"
                  className="review-create__textarea"
                  value={promptSuffix}
                  onChange={(event) => setPromptSuffix(event.target.value)}
                  disabled={disableForm}
                />
              </label>

              <label className="review-create__field" htmlFor="relatedStrings">
                <span className="review-create__label">Related strings</span>
                <select
                  id="relatedStrings"
                  className="review-create__select"
                  value={relatedStrings}
                  onChange={(event) => setRelatedStrings(event.target.value)}
                  disabled={disableForm}
                >
                  {RELATED_STRINGS_OPTIONS.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
              </label>

              <label className="review-create__field" htmlFor="translateType">
                <span className="review-create__label">Translate type</span>
                <select
                  id="translateType"
                  className="review-create__select"
                  value={translateType}
                  onChange={(event) => setTranslateType(event.target.value)}
                  disabled={disableForm}
                >
                  {TRANSLATE_TYPE_OPTIONS.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
              </label>

              <label className="review-create__field" htmlFor="statusFilter">
                <span className="review-create__label">Status filter</span>
                <select
                  id="statusFilter"
                  className="review-create__select"
                  value={statusFilter}
                  onChange={(event) => setStatusFilter(event.target.value)}
                  disabled={disableForm}
                >
                  {STATUS_FILTER_OPTIONS.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
              </label>

              <label className="review-create__field" htmlFor="importStatus">
                <span className="review-create__label">Import status</span>
                <select
                  id="importStatus"
                  className="review-create__select"
                  value={importStatus}
                  onChange={(event) => setImportStatus(event.target.value)}
                  disabled={disableForm}
                >
                  {IMPORT_STATUS_OPTIONS.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
              </label>

              <label className="review-create__field" htmlFor="timeoutSeconds">
                <span className="review-create__label">Request timeout (seconds)</span>
                <input
                  id="timeoutSeconds"
                  type="number"
                  min={0}
                  className="review-create__input"
                  value={timeoutSeconds}
                  onChange={(event) => setTimeoutSeconds(event.target.value)}
                  disabled={disableForm}
                />
              </label>

              <div className="review-create__field ai-translate-toggle-row">
                <label className="ai-translate-toggle">
                  <input
                    type="checkbox"
                    checked={downloadReport}
                    onChange={(event) => setDownloadReport(event.target.checked)}
                    disabled={disableForm}
                  />
                  <span>Download JSON report</span>
                </label>
                <label className="ai-translate-toggle">
                  <input
                    type="checkbox"
                    checked={dryRun}
                    onChange={(event) => setDryRun(event.target.checked)}
                    disabled={disableForm}
                  />
                  <span>Dry run</span>
                </label>
              </div>
            </div>
          </div>

          <div className="review-create__actions">
            <div className="ai-translate-status">
              {jobError ? <div className="review-create__error">{String(jobError)}</div> : null}
              {reportError ? (
                <div className="review-create__error">{String(reportError)}</div>
              ) : null}
              {isWaiting ? (
                <div className="review-create__hint">
                  Waiting for AI translation job {pollableTask ? pollableTask.id : ''} to finish...
                </div>
              ) : null}
              {reportStatus ? (
                <div className="review-create__hint ai-translate-status--success">
                  AI translation job {pollableTask?.id} finished successfully.
                </div>
              ) : null}
              {isFetchingReport ? (
                <div className="review-create__hint">Preparing report downloads...</div>
              ) : null}
              {reportDownloads.length ? (
                <div className="ai-translate-report">
                  <h3>Download report</h3>
                  <ul>
                    {reportDownloads.map((download) => (
                      <li key={download.locale}>
                        <a href={download.href} download={download.filename}>
                          Download report for {download.locale}
                        </a>
                      </li>
                    ))}
                  </ul>
                </div>
              ) : null}
            </div>
            <button
              type="submit"
              className="review-create__cta"
              disabled={disableForm || !selectedRepository}
            >
              {isSubmitting ? (
                <>
                  <span className="spinner" aria-hidden="true" /> Submitting...
                </>
              ) : (
                'Start translation'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
