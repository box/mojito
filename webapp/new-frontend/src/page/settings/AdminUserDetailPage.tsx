import './admin-user-detail-page.css';
import './settings-page.css';
import '../../components/chip-dropdown.css';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { Navigate, useNavigate, useParams } from 'react-router-dom';

import type { ApiAuthority, ApiUser, ApiUserLocale } from '../../api/users';
import { deleteUser, updateUser } from '../../api/users';
import { ConfirmModal } from '../../components/ConfirmModal';
import { LocaleMultiSelect } from '../../components/LocaleMultiSelect';
import { useUser } from '../../components/RequireUser';
import { SingleSelectDropdown } from '../../components/SingleSelectDropdown';
import { useLocales } from '../../hooks/useLocales';
import { USERS_QUERY_KEY, useUsers } from '../../hooks/useUsers';
import { useLocaleDisplayNameResolver } from '../../utils/localeDisplayNames';

const normalizeLocaleList = (tags: string[]) =>
  [...new Set(tags.map((tag) => tag.trim()).filter(Boolean))].sort((a, b) =>
    a.localeCompare(b, undefined, { sensitivity: 'base' }),
  );

const getPrimaryRole = (user: ApiUser) => user.authorities?.[0]?.authority ?? 'ROLE_USER';

const getDisplayName = (user: ApiUser) => {
  if (user.commonName) {
    return user.commonName;
  }
  if (user.givenName && user.surname) {
    return `${user.givenName} ${user.surname}`;
  }
  return user.username;
};

const getLocaleTags = (user: ApiUser) =>
  (user.userLocales ?? [])
    .map((locale) => locale.locale?.bcp47Tag)
    .filter((tag): tag is string => Boolean(tag));

const ROLE_OPTIONS = [
  { value: 'ROLE_ADMIN', label: 'Admin' },
  { value: 'ROLE_PM', label: 'Project manager' },
  { value: 'ROLE_TRANSLATOR', label: 'Translator' },
  { value: 'ROLE_USER', label: 'User' },
];

export function AdminUserDetailPage() {
  const viewer = useUser();
  const isAdmin = viewer.role === 'ROLE_ADMIN';
  const navigate = useNavigate();
  const { userId: userIdParam } = useParams<{ userId: string }>();
  const userId = userIdParam ? Number(userIdParam) : NaN;
  const { data: users, isLoading, isError } = useUsers();
  const { data: locales, isLoading: localesLoading, isError: localesError } = useLocales();
  const resolveLocaleName = useLocaleDisplayNameResolver();
  const queryClient = useQueryClient();
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [saveStatus, setSaveStatus] = useState<'saved' | null>(null);

  const userRecord = useMemo(() => {
    if (!Number.isFinite(userId)) {
      return null;
    }
    return users?.find((entry) => entry.id === userId) ?? null;
  }, [userId, users]);

  const [roleDraft, setRoleDraft] = useState('ROLE_USER');
  const [canTranslateAllLocalesDraft, setCanTranslateAllLocalesDraft] = useState(true);
  const [localeDraft, setLocaleDraft] = useState<string[]>([]);
  const [usernameDraft, setUsernameDraft] = useState('');
  const [givenNameDraft, setGivenNameDraft] = useState('');
  const [surnameDraft, setSurnameDraft] = useState('');
  const [commonNameDraft, setCommonNameDraft] = useState('');
  const [passwordDraft, setPasswordDraft] = useState('');
  const [enabledDraft, setEnabledDraft] = useState(true);

  useEffect(() => {
    if (!userRecord) {
      return;
    }
    setUsernameDraft(userRecord.username ?? '');
    setGivenNameDraft(userRecord.givenName ?? '');
    setSurnameDraft(userRecord.surname ?? '');
    setCommonNameDraft(userRecord.commonName ?? '');
    setRoleDraft(getPrimaryRole(userRecord));
    setCanTranslateAllLocalesDraft(userRecord.canTranslateAllLocales);
    setLocaleDraft(getLocaleTags(userRecord));
    setEnabledDraft(userRecord.enabled ?? true);
    setPasswordDraft('');
  }, [userRecord]);

  const localeOptions = useMemo(() => {
    const baseOptions = (locales ?? []).map((locale) => ({
      tag: locale.bcp47Tag,
      label: resolveLocaleName(locale.bcp47Tag),
    }));
    const seen = new Set(baseOptions.map((option) => option.tag.toLowerCase()));
    const extras = localeDraft
      .filter((tag) => {
        const lowered = tag.toLowerCase();
        if (seen.has(lowered)) {
          return false;
        }
        seen.add(lowered);
        return true;
      })
      .map((tag) => ({ tag, label: resolveLocaleName(tag) }));

    return [...baseOptions, ...extras].sort((first, second) =>
      first.tag.localeCompare(second.tag, undefined, { sensitivity: 'base' }),
    );
  }, [localeDraft, locales, resolveLocaleName]);

  const normalizedDraftLocales = useMemo(() => normalizeLocaleList(localeDraft), [localeDraft]);
  const normalizedSavedLocales = useMemo(
    () => (userRecord ? normalizeLocaleList(getLocaleTags(userRecord)) : []),
    [userRecord],
  );

  const isLocaleDirty = useMemo(() => {
    if (normalizedDraftLocales.length !== normalizedSavedLocales.length) {
      return true;
    }
    return normalizedDraftLocales.some(
      (value, index) => value.toLowerCase() !== normalizedSavedLocales[index]?.toLowerCase(),
    );
  }, [normalizedDraftLocales, normalizedSavedLocales]);

  const normalizeText = (value?: string | null) => (value ?? '').trim();
  const normalizedUsername = normalizeText(usernameDraft);
  const normalizedGivenName = normalizeText(givenNameDraft);
  const normalizedSurname = normalizeText(surnameDraft);
  const normalizedCommonName = normalizeText(commonNameDraft);
  const normalizedPassword = passwordDraft;
  const usernameMissing = normalizedUsername.length === 0;
  const passwordDirty = normalizedPassword.length > 0;

  const isDirty = Boolean(
    userRecord &&
      (userRecord.canTranslateAllLocales !== canTranslateAllLocalesDraft ||
        (userRecord.enabled ?? true) !== enabledDraft ||
        getPrimaryRole(userRecord) !== roleDraft ||
        isLocaleDirty ||
        normalizeText(userRecord.username) !== normalizedUsername ||
        normalizeText(userRecord.givenName) !== normalizedGivenName ||
        normalizeText(userRecord.surname) !== normalizedSurname ||
        normalizeText(userRecord.commonName) !== normalizedCommonName ||
        passwordDirty),
  );

  useEffect(() => {
    if (isDirty) {
      setSaveStatus(null);
    }
  }, [isDirty]);

  const updateMutation = useMutation({
    mutationFn: () => {
      if (!userRecord) {
        throw new Error('User not found');
      }
      if (!normalizedUsername) {
        throw new Error('Username is required');
      }
      const payloadLocales: ApiUserLocale[] = normalizeLocaleList(localeDraft).map((tag) => ({
        locale: { bcp47Tag: tag },
      }));
      const payloadAuthorities: ApiAuthority[] = [{ authority: roleDraft }];
      const payload = {
        username: normalizedUsername,
        givenName: normalizedGivenName,
        surname: normalizedSurname,
        commonName: normalizedCommonName,
        canTranslateAllLocales: canTranslateAllLocalesDraft,
        enabled: enabledDraft,
        userLocales: payloadLocales,
        authorities: payloadAuthorities,
        ...(normalizedPassword ? { password: normalizedPassword } : {}),
      };
      return updateUser(userRecord.id, payload);
    },
    onMutate: () => {
      setSaveStatus(null);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY] });
      setSaveStatus('saved');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => {
      if (!userRecord) {
        throw new Error('User not found');
      }
      return deleteUser(userRecord.id);
    },
    onSuccess: () => {
      setShowDeleteConfirm(false);
      void queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY] });
      void navigate('/settings/admin/users');
    },
  });

  if (!isAdmin) {
    return <Navigate to="/repositories" replace />;
  }

  if (isLoading) {
    return (
      <div className="user-detail-page__state">
        <div className="spinner spinner--md" aria-hidden />
        <div>Loading user…</div>
      </div>
    );
  }

  if (isError || !userRecord) {
    return (
      <div className="user-detail-page__state user-detail-page__state--error">
        <div>Unable to load user.</div>
        <button
          type="button"
          className="user-detail-page__back"
          onClick={() => {
            void navigate('/settings/admin/users');
          }}
        >
          Back to users
        </button>
      </div>
    );
  }

  const displayName = getDisplayName(userRecord);
  const displayNamePreview =
    normalizedCommonName ||
    [normalizedGivenName, normalizedSurname].filter(Boolean).join(' ') ||
    normalizedUsername ||
    displayName;

  const handleReset = () => {
    setUsernameDraft(userRecord.username ?? '');
    setGivenNameDraft(userRecord.givenName ?? '');
    setSurnameDraft(userRecord.surname ?? '');
    setCommonNameDraft(userRecord.commonName ?? '');
    setRoleDraft(getPrimaryRole(userRecord));
    setCanTranslateAllLocalesDraft(userRecord.canTranslateAllLocales);
    setLocaleDraft(getLocaleTags(userRecord));
    setEnabledDraft(userRecord.enabled ?? true);
    setPasswordDraft('');
  };

  return (
    <div className="user-detail-page">
      <header className="user-detail-page__header">
        <div className="user-detail-page__header-row">
          <div className="user-detail-page__header-group user-detail-page__header-group--left">
            <button
              type="button"
              className="user-detail-page__header-back"
              onClick={() => {
                void navigate('/settings/admin/users');
              }}
              aria-label="Back to user settings"
              title="Back to users"
            >
              <svg
                className="user-detail-page__header-back-icon"
                viewBox="0 0 24 24"
                aria-hidden="true"
                focusable="false"
              >
                <path
                  d="M20 12H6m0 0l5-5m-5 5l5 5"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </button>
            <span className="user-detail-page__header-name">{displayNamePreview}</span>
          </div>
          <div className="user-detail-page__header-group user-detail-page__header-group--center">
            <span className="user-detail-page__header-meta">ID</span>
            <span className="user-detail-page__header-dot">#</span>
            <span className="user-detail-page__header-meta">{userRecord.id}</span>
          </div>
        </div>
      </header>

      <div className="user-detail-page__content">
        <section className="user-detail-page__section">
          <div className="user-detail-page__field">
            <div className="user-detail-page__label">Username</div>
            <input
              type="text"
              className="settings-input"
              value={usernameDraft}
              onChange={(event) => setUsernameDraft(event.target.value)}
              placeholder="Enter the username"
            />
            {usernameMissing ? (
              <div className="user-detail-page__hint is-error">Username is required.</div>
            ) : null}
          </div>

          <div className="user-detail-page__field">
            <div className="user-detail-page__label">Given Name</div>
            <input
              type="text"
              className="settings-input"
              value={givenNameDraft}
              onChange={(event) => setGivenNameDraft(event.target.value)}
              placeholder="Enter the given name"
            />
          </div>

          <div className="user-detail-page__field">
            <div className="user-detail-page__label">Surname</div>
            <input
              type="text"
              className="settings-input"
              value={surnameDraft}
              onChange={(event) => setSurnameDraft(event.target.value)}
              placeholder="Enter the surname"
            />
          </div>

          <div className="user-detail-page__field">
            <div className="user-detail-page__label">Common Name</div>
            <input
              type="text"
              className="settings-input"
              value={commonNameDraft}
              onChange={(event) => setCommonNameDraft(event.target.value)}
              placeholder="Enter the common name"
            />
            <div className="user-detail-page__hint">
              Can be used instead of given name + surname.
            </div>
          </div>

          <div className="user-detail-page__field">
            <div className="user-detail-page__label">Display name</div>
            <div className="user-detail-page__value">{displayNamePreview || '—'}</div>
          </div>

          <div className="user-detail-page__field">
            <div className="user-detail-page__label">Password (optional)</div>
            <input
              type="password"
              className="settings-input"
              value={passwordDraft}
              onChange={(event) => setPasswordDraft(event.target.value)}
              placeholder="Enter the password"
            />
          </div>

          <div className="user-detail-page__field">
            <div className="user-detail-page__label">Role</div>
            <SingleSelectDropdown
              label="Role"
              options={ROLE_OPTIONS}
              value={roleDraft}
              onChange={(next) => setRoleDraft(next ?? 'ROLE_USER')}
              className="user-detail-page__select"
            />
          </div>

          <div className="user-detail-page__field">
            <label className="user-detail-page__toggle">
              <input
                type="checkbox"
                checked={enabledDraft}
                onChange={(event) => setEnabledDraft(event.target.checked)}
              />
              <span>Enabled</span>
            </label>
            <div className="user-detail-page__hint">Disabled users cannot sign in.</div>
          </div>

          <div className="user-detail-page__field">
            <div className="user-detail-page__label">Locales</div>
            <label className="user-detail-page__toggle">
              <input
                type="checkbox"
                checked={canTranslateAllLocalesDraft}
                onChange={(event) => setCanTranslateAllLocalesDraft(event.target.checked)}
              />
              <span>Can translate all locales</span>
            </label>
            <div className="user-detail-page__hint">
              When enabled, this user can translate any locale without restrictions.
            </div>
            <LocaleMultiSelect
              label="Locales"
              options={localeOptions}
              selectedTags={localeDraft}
              onChange={setLocaleDraft}
              className="user-detail-page__locale-select"
              buttonAriaLabel="Select user locales"
              disabled={canTranslateAllLocalesDraft || localesLoading || localesError}
            />
            {localesLoading ? (
              <div className="user-detail-page__hint">Loading locales...</div>
            ) : null}
            {localesError ? (
              <div className="user-detail-page__hint is-error">Failed to load locale list.</div>
            ) : null}
            {!canTranslateAllLocalesDraft && !localeDraft.length && !localesLoading ? (
              <div className="user-detail-page__hint">No locale restrictions set.</div>
            ) : null}
          </div>
        </section>

        <div className="user-detail-page__danger">
          <button
            type="button"
            className="user-detail-page__delete"
            onClick={() => setShowDeleteConfirm(true)}
            disabled={userRecord.username === viewer.username || deleteMutation.isPending}
            title={
              userRecord.username === viewer.username
                ? 'You cannot delete your own account.'
                : undefined
            }
          >
            Delete user
          </button>
          <div className="user-detail-page__actions">
            <button
              type="button"
              className="settings-button settings-button--primary"
              onClick={() => updateMutation.mutate()}
              disabled={!isDirty || updateMutation.isPending || usernameMissing}
            >
              Save
            </button>
            <button
              type="button"
              className="settings-button settings-button--ghost"
              onClick={handleReset}
              disabled={!isDirty || updateMutation.isPending}
            >
              Reset
            </button>
            {updateMutation.isPending ? (
              <span className="user-detail-page__status">Saving…</span>
            ) : saveStatus === 'saved' ? (
              <span className="user-detail-page__status">Saved</span>
            ) : null}
          </div>
          {deleteMutation.isError ? (
            <div className="user-detail-page__hint is-error">
              {deleteMutation.error instanceof Error
                ? deleteMutation.error.message
                : 'Failed to delete user.'}
            </div>
          ) : null}
        </div>

        {updateMutation.isError ? (
          <div className="user-detail-page__hint is-error">
            {updateMutation.error instanceof Error
              ? updateMutation.error.message
              : 'Failed to update user.'}
          </div>
        ) : null}
      </div>

      <ConfirmModal
        open={showDeleteConfirm}
        title={`Delete ${displayName}?`}
        body="This will disable the user and remove access. This action cannot be undone."
        confirmLabel={deleteMutation.isPending ? 'Deleting...' : 'Delete'}
        cancelLabel="Cancel"
        onCancel={() => setShowDeleteConfirm(false)}
        onConfirm={() => deleteMutation.mutate()}
      />
    </div>
  );
}
