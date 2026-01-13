import './settings-page.css';

import type { KeyboardEvent } from 'react';
import { useEffect, useMemo, useState } from 'react';
import { Navigate } from 'react-router-dom';

import { useUser } from '../../components/RequireUser';
import { useRepositories } from '../../hooks/useRepositories';
import { useLocaleDisplayNameResolver } from '../../utils/localeDisplayNames';
import { getNonRootRepositoryLocaleTags } from '../../utils/repositoryLocales';
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
  const [preferredLocaleInput, setPreferredLocaleInput] = useState('');
  const [isLocaleMenuOpen, setIsLocaleMenuOpen] = useState(false);
  const [highlightedLocaleIndex, setHighlightedLocaleIndex] = useState(-1);
  const [preferredLocaleError, setPreferredLocaleError] = useState<string | null>(null);

  const localeOptions = useMemo(() => {
    const tags = new Set<string>();
    repositories?.forEach((repo) => {
      getNonRootRepositoryLocaleTags(repo).forEach((tag) => tags.add(tag));
    });
    preferredLocalesDraft.forEach((tag) => tags.add(tag));
    return Array.from(tags)
      .sort((first, second) => first.localeCompare(second, undefined, { sensitivity: 'base' }))
      .map((tag) => ({ tag, label: resolveLocaleName(tag) }));
  }, [preferredLocalesDraft, repositories, resolveLocaleName]);

  const localeLabelByTag = useMemo(
    () => new Map(localeOptions.map((option) => [option.tag.toLowerCase(), option.label])),
    [localeOptions],
  );

  const availableLocaleOptions = useMemo(() => {
    const selected = new Set(preferredLocalesDraft.map((tag) => tag.toLowerCase()));
    return localeOptions.filter((option) => !selected.has(option.tag.toLowerCase()));
  }, [localeOptions, preferredLocalesDraft]);
  const localeSuggestions = useMemo(() => {
    const query = preferredLocaleInput.trim().toLowerCase();
    const pool = availableLocaleOptions;
    const filtered = query
      ? pool.filter(
          (option) =>
            option.tag.toLowerCase().includes(query) || option.label.toLowerCase().includes(query),
        )
      : pool;
    return filtered.slice(0, 8);
  }, [availableLocaleOptions, preferredLocaleInput]);

  useEffect(() => {
    if (!isLocaleMenuOpen || !localeSuggestions.length) {
      setHighlightedLocaleIndex(-1);
      return;
    }
    setHighlightedLocaleIndex(0);
  }, [isLocaleMenuOpen, localeSuggestions.length]);

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

  const handleLocaleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Escape') {
      setIsLocaleMenuOpen(false);
      setHighlightedLocaleIndex(-1);
      return;
    }

    if (event.key === 'Enter') {
      event.preventDefault();
      const highlighted = localeSuggestions[highlightedLocaleIndex];
      if (highlighted) {
        addPreferredLocaleValue(highlighted.tag, { auto: true });
        return;
      }
      addPreferredLocaleValue(preferredLocaleInput, { auto: false });
      return;
    }

    if (!localeSuggestions.length) {
      return;
    }

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setIsLocaleMenuOpen(true);
      setHighlightedLocaleIndex((current) => {
        const next = current + 1;
        return next >= localeSuggestions.length ? 0 : next;
      });
      return;
    }

    if (event.key === 'ArrowUp') {
      event.preventDefault();
      setIsLocaleMenuOpen(true);
      setHighlightedLocaleIndex((current) => {
        if (current <= 0) {
          return localeSuggestions.length - 1;
        }
        return current - 1;
      });
    }
  };

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

  const addPreferredLocaleValue = (raw: string, { auto }: { auto: boolean }) => {
    const trimmed = raw.trim();
    if (!trimmed) {
      if (!auto) {
        setPreferredLocaleError('Enter a locale tag');
      }
      return;
    }
    const normalized =
      availableLocaleOptions.find((option) => option.tag.toLowerCase() === trimmed.toLowerCase())
        ?.tag ?? trimmed;
    const alreadySelected = preferredLocalesDraft.some(
      (tag) => tag.toLowerCase() === normalized.toLowerCase(),
    );
    if (alreadySelected) {
      setPreferredLocaleInput('');
      setPreferredLocaleError(auto ? null : 'Locale already added');
      return;
    }
    setPreferredLocalesDraft([...preferredLocalesDraft, normalized]);
    setPreferredLocaleInput('');
    setPreferredLocaleError(null);
  };

  const handleRemovePreferredLocale = (tag: string) => {
    setPreferredLocalesDraft((current) =>
      current.filter((value) => value.toLowerCase() !== tag.toLowerCase()),
    );
    setPreferredLocaleError(null);
  };

  const handleSavePreferredLocales = () => {
    savePreferredLocales(preferredLocalesDraft);
    const next = loadPreferredLocales();
    setSavedPreferredLocales(next);
    setPreferredLocalesDraft(next);
    setPreferredLocaleError(null);
  };

  const handleClearPreferredLocales = () => {
    if (!preferredLocalesDraft.length) {
      return;
    }
    setPreferredLocalesDraft([]);
    setPreferredLocaleError(null);
    setPreferredLocaleInput('');
    setIsLocaleMenuOpen(false);
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
          {preferredLocalesDraft.length ? (
            <div className="settings-chip-list" role="list">
              {preferredLocalesDraft.map((tag) => {
                const resolvedLabel = localeLabelByTag.get(tag.toLowerCase());
                return (
                  <div key={tag} className="settings-chip" role="listitem">
                    <div className="settings-chip__text">
                      <span className="settings-chip__tag">{tag}</span>
                      {resolvedLabel && resolvedLabel !== tag ? (
                        <span className="settings-chip__label">{resolvedLabel}</span>
                      ) : null}
                    </div>
                    <button
                      type="button"
                      className="settings-chip__remove"
                      onClick={() => handleRemovePreferredLocale(tag)}
                      aria-label={`Remove ${tag}`}
                    >
                      ×
                    </button>
                  </div>
                );
              })}
            </div>
          ) : (
            <p className="settings-hint">No preferred locales set.</p>
          )}
        </div>
        <div className="settings-field">
          <div className="settings-field__row settings-field__row--stacked">
            <input
              id="preferred-locale-input"
              type="text"
              className="settings-input"
              value={preferredLocaleInput}
              onChange={(event) => {
                setPreferredLocaleInput(event.target.value);
                setPreferredLocaleError(null);
              }}
              placeholder="Select or type a locale tag (e.g. fr-FR)"
              onKeyDown={(event) => {
                handleLocaleKeyDown(event);
              }}
              onFocus={() => setIsLocaleMenuOpen(true)}
              onBlur={() => {
                // Delay closing to allow click on a suggestion.
                setTimeout(() => setIsLocaleMenuOpen(false), 100);
              }}
            />
            {isLocaleMenuOpen && localeSuggestions.length ? (
              <div className="settings-locale-suggestions">
                {localeSuggestions.map((option, index) => (
                  <button
                    type="button"
                    key={option.tag}
                    className={`settings-locale-suggestion${
                      index === highlightedLocaleIndex ? ' is-active' : ''
                    }`}
                    onMouseDown={(event) => event.preventDefault()}
                    onClick={() => addPreferredLocaleValue(option.tag, { auto: true })}
                    onMouseEnter={() => setHighlightedLocaleIndex(index)}
                  >
                    <span className="settings-locale-suggestion__tag">{option.tag}</span>
                    <span className="settings-locale-suggestion__label">{option.label}</span>
                  </button>
                ))}
              </div>
            ) : null}
          </div>
          {preferredLocaleError ? (
            <div className="settings-hint is-error">{preferredLocaleError}</div>
          ) : (
            <div className="settings-hint">
              Press Enter to add. Suggestions come from your repositories, hide already added
              locales, and can be picked with the arrow keys.
            </div>
          )}
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
            <button
              type="button"
              className="settings-button settings-button--ghost"
              onClick={handleClearPreferredLocales}
              disabled={!preferredLocalesDraft.length}
            >
              Clear list
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}
