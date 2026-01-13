import './settings-page.css';

import { useState } from 'react';
import { Navigate } from 'react-router-dom';

import { useUser } from '../../components/RequireUser';
import { WORKSET_SIZE_DEFAULT } from '../workbench/workbench-constants';
import { clampWorksetSize } from '../workbench/workbench-helpers';
import {
  loadPreferredWorksetSize,
  savePreferredWorksetSize,
} from '../workbench/workbench-preferences';

export function AdminSettingsPage() {
  const user = useUser();
  const isAdmin = user.role === 'ROLE_ADMIN';
  const [worksetDraft, setWorksetDraft] = useState<string>(() => {
    const value = loadPreferredWorksetSize();
    return value == null ? '' : String(value);
  });
  const [worksetError, setWorksetError] = useState<string | null>(null);

  if (!isAdmin) {
    return <Navigate to="/repositories" replace />;
  }

  const handleSaveWorksetPreference = () => {
    const trimmed = worksetDraft.trim();
    if (!trimmed) {
      savePreferredWorksetSize(null);
      setWorksetDraft('');
      setWorksetError(null);
      return;
    }
    const parsed = parseInt(trimmed, 10);
    if (!Number.isFinite(parsed) || parsed < 1) {
      setWorksetError('Enter a positive number');
      return;
    }
    const next = clampWorksetSize(parsed);
    savePreferredWorksetSize(next);
    setWorksetDraft(String(next));
    setWorksetError(null);
  };

  const handleResetWorksetPreference = () => {
    savePreferredWorksetSize(null);
    setWorksetDraft('');
    setWorksetError(null);
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
            <div className="settings-actions">
              <button
                type="button"
                className="settings-button"
                onClick={handleSaveWorksetPreference}
              >
                Save
              </button>
              <button
                type="button"
                className="settings-button settings-button--ghost"
                onClick={handleResetWorksetPreference}
              >
                Reset
              </button>
            </div>
          </div>
          {worksetError ? <div className="settings-hint is-error">{worksetError}</div> : null}
        </div>
      </section>
    </div>
  );
}
