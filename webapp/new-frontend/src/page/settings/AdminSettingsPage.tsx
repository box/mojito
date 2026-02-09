import './settings-page.css';

import { useEffect, useMemo, useState } from 'react';
import { Navigate } from 'react-router-dom';

import { LocaleMultiSelect } from '../../components/LocaleMultiSelect';
import { useUser } from '../../components/RequireUser';
import { useRepositories } from '../../hooks/useRepositories';
import { useLocaleDisplayNameResolver } from '../../utils/localeDisplayNames';
import { buildLocaleOptionsFromRepositories } from '../../utils/localeSelection';
import { WORKSET_SIZE_DEFAULT } from '../workbench/workbench-constants';
import { clampWorksetSize } from '../workbench/workbench-helpers';
import {
  loadPreferredLocales,
  loadPreferredWorksetSize,
  PREFERRED_LOCALES_KEY,
  savePreferredLocales,
  savePreferredWorksetSize,
} from '../workbench/workbench-preferences';

export function AdminSettingsPage() {
  const user = useUser();
  const isAdmin = user.role === 'ROLE_ADMIN';
  const { data: repositories } = useRepositories();
  const resolveLocaleName = useLocaleDisplayNameResolver();
  const [savedWorkset, setSavedWorkset] = useState<number | null>(() => loadPreferredWorksetSize());
  const [savedPreferredLocales, setSavedPreferredLocales] = useState<string[]>(() =>
    loadPreferredLocales(),
  );
  const [worksetDraft, setWorksetDraft] = useState<string>(() =>
    savedWorkset == null ? '' : String(savedWorkset),
  );
  const [preferredLocalesDraft, setPreferredLocalesDraft] =
    useState<string[]>(savedPreferredLocales);

  const localeOptions = useMemo(() => {
    const repositoryOptions = buildLocaleOptionsFromRepositories(
      repositories ?? [],
      resolveLocaleName,
    );
    const seen = new Set(repositoryOptions.map((option) => option.tag.toLowerCase()));
    const mergedSelections = [
      ...savedPreferredLocales,
      ...preferredLocalesDraft,
      ...user.userLocales,
    ];
    const extraOptions = mergedSelections
      .filter((tag) => {
        const lower = tag.toLowerCase();
        if (seen.has(lower)) {
          return false;
        }
        seen.add(lower);
        return true;
      })
      .map((tag) => ({ tag, label: resolveLocaleName(tag) }));

    return [...repositoryOptions, ...extraOptions].sort((first, second) =>
      first.tag.localeCompare(second.tag, undefined, { sensitivity: 'base' }),
    );
  }, [
    preferredLocalesDraft,
    repositories,
    resolveLocaleName,
    savedPreferredLocales,
    user.userLocales,
  ]);

  useEffect(() => {
    const handleStorage = (event: StorageEvent) => {
      if (event.key && event.key !== PREFERRED_LOCALES_KEY) {
        return;
      }
      const next = loadPreferredLocales();
      setSavedPreferredLocales(next);
      setPreferredLocalesDraft(next);
    };
    window.addEventListener('storage', handleStorage);
    return () => window.removeEventListener('storage', handleStorage);
  }, []);

  const trimmedWorkset = worksetDraft.trim();
  const parsedWorkset = useMemo(() => {
    if (!trimmedWorkset) {
      return { value: null as number | null, valid: true };
    }
    const parsed = parseInt(trimmedWorkset, 10);
    if (!Number.isFinite(parsed) || parsed < 1) {
      return { value: null as number | null, valid: false };
    }
    return { value: clampWorksetSize(parsed), valid: true };
  }, [trimmedWorkset]);

  const worksetError = !parsedWorkset.valid && trimmedWorkset ? 'Enter a positive number' : null;
  const isWorksetDirty = useMemo(() => {
    const hasInput = trimmedWorkset.length > 0;
    if (!hasInput) {
      return savedWorkset !== null;
    }
    if (!parsedWorkset.valid) {
      return false;
    }
    return parsedWorkset.value !== savedWorkset;
  }, [parsedWorkset.valid, parsedWorkset.value, savedWorkset, trimmedWorkset]);
  const canResetWorkset = useMemo(
    () => savedWorkset !== null || trimmedWorkset.length > 0,
    [savedWorkset, trimmedWorkset],
  );

  const isPreferredLocalesDirty = useMemo(() => {
    if (preferredLocalesDraft.length !== savedPreferredLocales.length) {
      return true;
    }
    return preferredLocalesDraft.some(
      (value, index) => value.toLowerCase() !== (savedPreferredLocales[index]?.toLowerCase() ?? ''),
    );
  }, [preferredLocalesDraft, savedPreferredLocales]);

  if (!isAdmin) {
    return <Navigate to="/repositories" replace />;
  }

  const handleSaveWorksetPreference = () => {
    if (!parsedWorkset.valid || !isWorksetDirty) {
      return;
    }
    savePreferredWorksetSize(parsedWorkset.value);
    setSavedWorkset(parsedWorkset.value);
    setWorksetDraft(parsedWorkset.value == null ? '' : String(parsedWorkset.value));
  };

  const handleResetWorksetPreference = () => {
    savePreferredWorksetSize(null);
    setSavedWorkset(null);
    setWorksetDraft('');
  };

  const handleSavePreferredLocales = () => {
    savePreferredLocales(preferredLocalesDraft);
    const next = loadPreferredLocales();
    setSavedPreferredLocales(next);
    setPreferredLocalesDraft(next);
  };

  return (
    <div className="settings-page">
      <div className="settings-page__header">
        <h1>Admin settings</h1>
      </div>

      <section className="settings-card" aria-labelledby="settings-admin">
        <div className="settings-card__header">
          <h2 id="settings-admin">Override workbench result size limit</h2>
        </div>
        <p className="settings-note">
          Set the default workbench result size limit. The default ({WORKSET_SIZE_DEFAULT}) is
          intentionally conservative; increase it if you want to load more results by default. Very
          large values can slow things down—staying under about 1000 is usually a good balance.
        </p>
        <div className="settings-field">
          <div className="settings-field__row">
            <input
              id="workset-size-input"
              type="number"
              min={1}
              inputMode="numeric"
              className="settings-input"
              value={worksetDraft}
              onChange={(event) => setWorksetDraft(event.target.value)}
              placeholder="Default is 10"
            />
          </div>
          {worksetError ? <div className="settings-hint is-error">{worksetError}</div> : null}
        </div>
        <div className="settings-card__footer">
          <div className="settings-actions">
            <button
              type="button"
              className="settings-button settings-button--primary"
              onClick={handleSaveWorksetPreference}
              disabled={!parsedWorkset.valid || !isWorksetDirty}
            >
              Save
            </button>
            <button
              type="button"
              className="settings-button settings-button--ghost"
              onClick={handleResetWorksetPreference}
              disabled={!canResetWorkset}
            >
              Reset
            </button>
          </div>
        </div>
      </section>

      <section className="settings-card" aria-labelledby="settings-preferred-locales">
        <div className="settings-card__header">
          <h2 id="settings-preferred-locales">Preferred locales</h2>
        </div>
        <p className="settings-note">
          Define your personal locale shortcuts. The workbench locale picker will show a &quot;My
          locales&quot; action that selects this list.
        </p>
        <div className="settings-field">
          <div className="settings-field__header">
            <div className="settings-field__label">Locales</div>
          </div>
          <LocaleMultiSelect
            label="Preferred locales"
            options={localeOptions}
            selectedTags={preferredLocalesDraft}
            onChange={setPreferredLocalesDraft}
            className="settings-locale-select"
            buttonAriaLabel="Select preferred locales"
            myLocaleTags={user.userLocales}
          />
          {preferredLocalesDraft.length === 0 ? (
            <p className="settings-hint">No preferred locales set.</p>
          ) : null}
          <p className="settings-hint">
            Uses the same locale selector as workbench and repositories; includes repository locales
            plus any saved choices.
          </p>
        </div>
        <div className="settings-card__footer">
          <div className="settings-actions">
            <button
              type="button"
              className="settings-button settings-button--primary"
              onClick={handleSavePreferredLocales}
              disabled={!isPreferredLocalesDirty}
            >
              Save
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}
