import './admin-user-batch-page.css';
import '../../components/chip-dropdown.css';

import { useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';

import type { ApiAuthority, ApiUserLocale } from '../../api/users';
import { createUser } from '../../api/users';
import { useUser } from '../../components/RequireUser';
import { SingleSelectDropdown } from '../../components/SingleSelectDropdown';
import { USERS_QUERY_KEY } from '../../hooks/useUsers';

const ROLE_OPTIONS = [
  { value: 'ROLE_ADMIN', label: 'Admin' },
  { value: 'ROLE_PM', label: 'Project manager' },
  { value: 'ROLE_TRANSLATOR', label: 'Translator' },
  { value: 'ROLE_USER', label: 'User' },
];

const ROLE_VALUES = new Set(ROLE_OPTIONS.map((option) => option.value));

const formatRole = (role?: string | null) => {
  switch (role) {
    case 'ROLE_ADMIN':
      return 'Admin';
    case 'ROLE_PM':
      return 'Project manager';
    case 'ROLE_TRANSLATOR':
      return 'Translator';
    default:
      return 'User';
  }
};

const normalizeRoleInput = (value?: string | null) => {
  const trimmed = (value ?? '').trim();
  if (!trimmed) {
    return null;
  }
  const upper = trimmed.toUpperCase();
  if (ROLE_VALUES.has(upper)) {
    return upper;
  }
  const compact = trimmed.toLowerCase().replace(/[\s_-]+/g, '');
  switch (compact) {
    case 'admin':
    case 'roleadmin':
      return 'ROLE_ADMIN';
    case 'pm':
    case 'projectmanager':
    case 'rolepm':
      return 'ROLE_PM';
    case 'translator':
    case 'roletranslator':
      return 'ROLE_TRANSLATOR';
    case 'user':
    case 'roleuser':
      return 'ROLE_USER';
    default:
      return null;
  }
};

type ParsedRow = {
  lineNumber: number;
  raw: string;
  username: string;
  role: string;
  locales: string[];
  password?: string;
  givenName?: string;
  surname?: string;
  commonName?: string;
  canTranslateAllLocales: boolean;
  errors: string[];
};

type CreateResult = {
  lineNumber: number;
  status: 'success' | 'error';
  message?: string;
  username?: string;
  password?: string;
};

type ColumnKey =
  | 'username'
  | 'role'
  | 'locales'
  | 'password'
  | 'givenName'
  | 'surname'
  | 'commonName'
  | 'ignore';

const COLUMN_OPTIONS = [
  { value: 'ignore', label: 'Ignore' },
  { value: 'username', label: 'Username' },
  { value: 'role', label: 'Role' },
  { value: 'locales', label: 'Locales' },
  { value: 'password', label: 'Password' },
  { value: 'givenName', label: 'Given name' },
  { value: 'surname', label: 'Surname' },
  { value: 'commonName', label: 'Common name' },
];

const DEFAULT_COLUMN_MAPPING: ColumnKey[] = [
  'username',
  'role',
  'locales',
  'password',
  'givenName',
  'surname',
  'commonName',
];

const EXAMPLE_CSV = `jane.doe, PM, en-US|fr-FR, StrongPass!, Jane, Doe, JD
max, User
translator, Translator, fr-FR, TempPass123`;

function getMaxColumns(input: string) {
  return input
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .reduce((max, line) => Math.max(max, line.split(',').length), 0);
}

function parseBatchInput(
  input: string,
  defaultRole: string,
  columnMapping: ColumnKey[],
): ParsedRow[] {
  const lines = input
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);

  return lines.map((line, index) => {
    const parts = line.split(',').map((part) => part.trim());
    const assigned: Partial<Record<ColumnKey, string>> = {};
    parts.forEach((value, columnIndex) => {
      const key = columnMapping[columnIndex] ?? 'ignore';
      if (key === 'ignore' || !value) {
        return;
      }
      if (assigned[key]) {
        return;
      }
      assigned[key] = value;
    });

    const username = assigned.username ?? '';
    const roleInput = assigned.role ?? null;
    const resolvedRole = roleInput ? normalizeRoleInput(roleInput) : defaultRole;
    const localesRaw = assigned.locales ?? '';
    const locales = localesRaw
      ? localesRaw
          .split(/[|;]+/)
          .map((tag) => tag.trim())
          .filter(Boolean)
      : [];
    const normalizedPassword = assigned.password?.trim() ?? '';

    const errors: string[] = [];
    if (!username) {
      errors.push('Missing username');
    }
    if (roleInput && !resolvedRole) {
      errors.push('Unknown role');
    }

    return {
      lineNumber: index + 1,
      raw: line,
      username,
      role: resolvedRole ?? defaultRole,
      locales,
      password: normalizedPassword || undefined,
      givenName: assigned.givenName?.trim() || undefined,
      surname: assigned.surname?.trim() || undefined,
      commonName: assigned.commonName?.trim() || undefined,
      canTranslateAllLocales: locales.length === 0,
      errors,
    };
  });
}

function generatePassword(length = 12) {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789';
  const values = new Uint32Array(length);
  window.crypto.getRandomValues(values);
  return Array.from(values)
    .map((value) => chars[value % chars.length])
    .join('');
}

const getRowPasswordKey = (row: ParsedRow) => `${row.lineNumber}:${row.raw}`;

export function AdminUserBatchPage() {
  const user = useUser();
  const isAdmin = user.role === 'ROLE_ADMIN';
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [input, setInput] = useState('');
  const [defaultRole, setDefaultRole] = useState('ROLE_USER');
  const [columnMapping, setColumnMapping] = useState<ColumnKey[]>(DEFAULT_COLUMN_MAPPING);
  const [results, setResults] = useState<CreateResult[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [generatedPasswords, setGeneratedPasswords] = useState<Record<string, string>>({});
  const [copyStatus, setCopyStatus] = useState<'idle' | 'copied'>('idle');

  const maxColumns = useMemo(() => Math.max(getMaxColumns(input), 1), [input]);

  useEffect(() => {
    if (columnMapping.length >= maxColumns) {
      return;
    }
    setColumnMapping((prev) => [
      ...prev,
      ...Array.from({ length: maxColumns - prev.length }, () => 'ignore' as ColumnKey),
    ]);
  }, [columnMapping.length, maxColumns]);

  const parsedRows = useMemo(
    () => parseBatchInput(input, defaultRole, columnMapping.slice(0, maxColumns)),
    [columnMapping, defaultRole, input, maxColumns],
  );
  const errorRows = parsedRows.filter((row) => row.errors.length);
  const validRows = parsedRows.filter((row) => row.errors.length === 0);
  const resultsByLine = useMemo(() => {
    const map = new Map<number, CreateResult>();
    results.forEach((result) => {
      map.set(result.lineNumber, result);
    });
    return map;
  }, [results]);

  useEffect(() => {
    if (!parsedRows.length) {
      setGeneratedPasswords({});
      return;
    }
    setGeneratedPasswords((prev) => {
      const next: Record<string, string> = {};
      parsedRows.forEach((row) => {
        if (row.password) {
          return;
        }
        const key = getRowPasswordKey(row);
        next[key] = prev[key] ?? generatePassword();
      });
      return next;
    });
  }, [parsedRows]);

  const handleCreate = async () => {
    if (!validRows.length) {
      return;
    }
    setIsSubmitting(true);
    const nextResults: CreateResult[] = [];
    for (const row of validRows) {
      const payloadLocales: ApiUserLocale[] = row.locales.map((tag) => ({
        locale: { bcp47Tag: tag },
      }));
      const payloadAuthorities: ApiAuthority[] = [{ authority: row.role }];
      const generatedPassword = generatedPasswords[getRowPasswordKey(row)];
      const password = row.password || generatedPassword || generatePassword();
      try {
        await createUser({
          username: row.username,
          password,
          canTranslateAllLocales: row.canTranslateAllLocales,
          userLocales: payloadLocales,
          authorities: payloadAuthorities,
          givenName: row.givenName ?? null,
          surname: row.surname ?? null,
          commonName: row.commonName ?? null,
        });
        nextResults.push({
          lineNumber: row.lineNumber,
          status: 'success',
          username: row.username,
          password,
        });
      } catch (error) {
        const message = error instanceof Error ? error.message : 'Failed to create user';
        nextResults.push({
          lineNumber: row.lineNumber,
          status: 'error',
          message,
          username: row.username,
        });
      }
      setResults([...nextResults]);
    }
    void queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY] });
    setIsSubmitting(false);
  };

  const handleCopyExample = useCallback(async () => {
    try {
      await navigator.clipboard.writeText(EXAMPLE_CSV);
      setCopyStatus('copied');
      window.setTimeout(() => setCopyStatus('idle'), 1500);
    } catch {
      setCopyStatus('idle');
    }
  }, []);

  if (!isAdmin) {
    return <Navigate to="/repositories" replace />;
  }

  return (
    <div className="user-batch-page">
      <header className="user-batch-page__header">
        <div className="user-batch-page__header-row">
          <div className="user-batch-page__header-group user-batch-page__header-group--left">
            <button
              type="button"
              className="user-batch-page__header-back"
              onClick={() => {
                void navigate('/settings/admin/users');
              }}
              aria-label="Back to user settings"
              title="Back to users"
            >
              <svg
                className="user-batch-page__header-back-icon"
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
            <span className="user-batch-page__header-name">Create users</span>
          </div>
          <div className="user-batch-page__header-group user-batch-page__header-group--center" />
          <div className="user-batch-page__header-group user-batch-page__header-group--right" />
        </div>
      </header>

      <div className="user-batch-page__content">
        <section className="user-batch-page__grid">
          <div className="user-batch-page__intro-text">
            <strong>
              Provide a CSV list and customize column mapping. Default mapping: username, role,
              locales, password, given name, surname, common name.
            </strong>
          </div>
          <div className="user-batch-page__example-header">
            <span className="user-batch-page__example-title">Example CSV</span>
            <button
              type="button"
              className="settings-button settings-button--ghost user-batch-page__example-copy"
              onClick={() => {
                void handleCopyExample();
              }}
            >
              {copyStatus === 'copied' ? 'Copied' : 'Copy'}
            </button>
          </div>
          <ul className="user-batch-page__intro-list">
            <li>Leave a column empty to skip it.</li>
            <li>
              Locales can be separated with <code>|</code> (for example <code>en-US|fr-FR</code>).
            </li>
            <li>If the locales column is empty, we set “can translate all locales”.</li>
            <li>If the password column is empty, a password is generated automatically.</li>
          </ul>
          <pre className="user-batch-page__example-code">{EXAMPLE_CSV}</pre>
          <div className="user-batch-page__field user-batch-page__input">
            <textarea
              id="batch-input"
              className="user-batch-page__textarea"
              placeholder="Enter CSV list here"
              value={input}
              onChange={(event) => setInput(event.target.value)}
            />
          </div>
          <div className="user-batch-page__field user-batch-page__mapping-panel">
            <div className="user-batch-page__mapping">
              {Array.from({ length: maxColumns }, (_, index) => {
                const current = columnMapping[index] ?? 'ignore';
                const showDefaultRole = current === 'role';
                return (
                  <div key={`column-${index}`} className="user-batch-page__mapping-row">
                    <span className="user-batch-page__mapping-label">Column {index + 1}</span>
                    <div className="user-batch-page__mapping-controls">
                      <SingleSelectDropdown
                        label={`Column ${index + 1}`}
                        options={COLUMN_OPTIONS}
                        value={current}
                        onChange={(next) => {
                          const value = (next ?? 'ignore') as ColumnKey;
                          setColumnMapping((prev) => {
                            const nextMapping = [...prev];
                            nextMapping[index] = value;
                            return nextMapping;
                          });
                        }}
                        className="user-batch-page__mapping-select"
                      />
                      {showDefaultRole ? (
                        <SingleSelectDropdown
                          label="Default role"
                          options={ROLE_OPTIONS}
                          value={defaultRole}
                          onChange={(next) => setDefaultRole(next ?? 'ROLE_USER')}
                          className="user-batch-page__role-select"
                        />
                      ) : null}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </section>

        <section className="user-batch-page__section user-batch-page__section--full">
          <div className="user-batch-page__summary">
            {parsedRows.length === 0
              ? 'No users parsed yet.'
              : `Ready to create ${validRows.length} ${validRows.length === 1 ? 'user' : 'users'}.`}
            {errorRows.length ? ` ${errorRows.length} line(s) need attention.` : ''}
          </div>
          <div className="user-batch-page__preview">
            <div className="user-batch-page__preview-header">
              <div>Line</div>
              <div>Username</div>
              <div>Role</div>
              <div>Locales</div>
              <div>Password</div>
              <div>Name</div>
              <div>Status</div>
            </div>
            {parsedRows.length ? (
              parsedRows.map((row) => (
                <div key={row.lineNumber} className="user-batch-page__preview-row">
                  <div className="user-batch-page__cell--muted">{row.lineNumber}</div>
                  <div>{row.username || '—'}</div>
                  <div>{formatRole(row.role)}</div>
                  <div className="user-batch-page__cell--muted">
                    {row.canTranslateAllLocales
                      ? 'All locales'
                      : row.locales.length
                        ? `${row.locales[0]}${
                            row.locales.length > 1 ? ` +${row.locales.length - 1}` : ''
                          }`
                        : 'None'}
                  </div>
                  <div className="user-batch-page__cell--muted">
                    {row.password ||
                      generatedPasswords[getRowPasswordKey(row)] ||
                      (row.username ? 'Generating…' : '—')}
                  </div>
                  <div className="user-batch-page__cell--muted">
                    {row.commonName ||
                      [row.givenName, row.surname].filter(Boolean).join(' ') ||
                      '—'}
                  </div>
                  <div>
                    {row.errors.length ? (
                      <span className="user-batch-page__error">{row.errors.join(', ')}</span>
                    ) : resultsByLine.get(row.lineNumber)?.status === 'error' ? (
                      <span className="user-batch-page__error">
                        {resultsByLine.get(row.lineNumber)?.message}
                      </span>
                    ) : resultsByLine.get(row.lineNumber)?.status === 'success' ? (
                      'Created'
                    ) : isSubmitting ? (
                      'Creating…'
                    ) : (
                      'Ready'
                    )}
                  </div>
                </div>
              ))
            ) : (
              <div className="user-batch-page__empty">Paste users to see a preview.</div>
            )}
          </div>
        </section>
        <div className="user-batch-page__actions">
          <button
            type="button"
            className="settings-button settings-button--primary user-batch-page__create"
            onClick={() => {
              void handleCreate();
            }}
            disabled={isSubmitting || validRows.length === 0 || errorRows.length > 0}
          >
            Create users
          </button>
          {isSubmitting ? <span className="user-batch-page__status">Creating…</span> : null}
        </div>
      </div>
    </div>
  );
}
