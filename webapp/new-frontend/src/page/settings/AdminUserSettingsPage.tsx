import './settings-page.css';
import './admin-user-settings-page.css';
import '../../components/filters/filter-chip.css';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';

import type { ApiUser } from '../../api/users';
import { deleteUser, updateUser } from '../../api/users';
import { ConfirmModal } from '../../components/ConfirmModal';
import {
  type FilterOption,
  MultiSectionFilterChip,
} from '../../components/filters/MultiSectionFilterChip';
import { useUser } from '../../components/RequireUser';
import { SearchControl } from '../../components/SearchControl';
import { SingleSelectDropdown } from '../../components/SingleSelectDropdown';
import { getRowHeightPx } from '../../components/virtual/getRowHeightPx';
import { useMeasuredRowRefs } from '../../components/virtual/useMeasuredRowRefs';
import { useVirtualRows } from '../../components/virtual/useVirtualRows';
import { VirtualList } from '../../components/virtual/VirtualList';
import { USERS_QUERY_KEY, useUsers } from '../../hooks/useUsers';
import { getStandardDateQuickRanges } from '../../utils/dateQuickRanges';

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

const buildLocalePayload = (user: ApiUser) =>
  getLocaleTags(user).map((tag) => ({ locale: { bcp47Tag: tag } }));

type UserRow = {
  id: number;
  username: string;
  displayName: string;
  roleLabel: string;
  roleValue: string;
  localeSummary: string;
  disabledLabel: string;
  enabled: boolean | null;
  createdDate: string | null;
  createdTimestamp: number | null;
  isSelf: boolean;
  searchText: string;
};

const ROLE_OPTIONS = [
  { value: 'ROLE_ADMIN', label: 'Admin' },
  { value: 'ROLE_PM', label: 'Project manager' },
  { value: 'ROLE_TRANSLATOR', label: 'Translator' },
  { value: 'ROLE_USER', label: 'User' },
];

const ROLE_FILTER_OPTIONS: Array<FilterOption<string>> = [
  { value: 'all', label: 'All roles' },
  ...ROLE_OPTIONS,
];

const STATUS_FILTER_OPTIONS: Array<FilterOption<string>> = [
  { value: 'all', label: 'All statuses' },
  { value: 'enabled', label: 'Enabled' },
  { value: 'disabled', label: 'Disabled' },
];

export function AdminUserSettingsPage() {
  const user = useUser();
  const isAdmin = user.role === 'ROLE_ADMIN';
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { data: users, isLoading, isError, refetch, isFetching } = useUsers();
  const [searchQuery, setSearchQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState<string>('all');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [createdAfter, setCreatedAfter] = useState<string | null>(null);
  const [createdBefore, setCreatedBefore] = useState<string | null>(null);
  const dateQuickRanges = useMemo(() => getStandardDateQuickRanges(), []);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [showDisableConfirm, setShowDisableConfirm] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const userRows = useMemo<UserRow[]>(() => {
    if (!users) {
      return [];
    }

    return users.map((entry) => {
      const displayName = getDisplayName(entry);
      const roleValue = getPrimaryRole(entry);
      const roleLabel = formatRole(roleValue);
      const localeTags = getLocaleTags(entry);
      const localeCount = localeTags.length;
      const localeSummary = entry.canTranslateAllLocales
        ? 'All locales'
        : localeCount === 1
          ? localeTags[0]
          : localeCount
            ? `${localeCount} locales`
            : 'No locales';
      const disabledLabel =
        entry.enabled === false ? 'Disabled' : entry.enabled === true ? 'Enabled' : '—';
      const createdDate = entry.createdDate ?? null;
      const createdTimestamp =
        createdDate && !Number.isNaN(Date.parse(createdDate)) ? Date.parse(createdDate) : null;
      const searchText = [
        entry.id,
        entry.username,
        entry.givenName,
        entry.surname,
        entry.commonName,
        displayName,
        roleLabel,
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();

      return {
        id: entry.id,
        username: entry.username,
        displayName,
        roleLabel,
        roleValue,
        localeSummary,
        disabledLabel,
        enabled: entry.enabled ?? null,
        createdDate,
        createdTimestamp,
        isSelf: entry.username === user.username,
        searchText,
      };
    });
  }, [user.username, users]);

  const normalizedQuery = searchQuery.trim().toLowerCase();
  const createdAfterTimestamp = createdAfter ? Date.parse(createdAfter) : null;
  const createdBeforeTimestamp = createdBefore ? Date.parse(createdBefore) : null;
  const filteredUsers = useMemo(() => {
    return userRows.filter((row) => {
      if (roleFilter !== 'all' && row.roleValue !== roleFilter) {
        return false;
      }
      if (statusFilter !== 'all') {
        const isDisabled = row.enabled === false;
        if (statusFilter === 'disabled' && !isDisabled) {
          return false;
        }
        if (statusFilter === 'enabled' && isDisabled) {
          return false;
        }
      }
      if (
        (createdAfterTimestamp != null || createdBeforeTimestamp != null) &&
        row.createdTimestamp == null
      ) {
        return false;
      }
      if (createdAfterTimestamp != null && (row.createdTimestamp ?? 0) < createdAfterTimestamp) {
        return false;
      }
      if (createdBeforeTimestamp != null && (row.createdTimestamp ?? 0) > createdBeforeTimestamp) {
        return false;
      }
      if (!normalizedQuery) {
        return true;
      }
      return row.searchText.includes(normalizedQuery);
    });
  }, [
    createdAfterTimestamp,
    createdBeforeTimestamp,
    normalizedQuery,
    roleFilter,
    statusFilter,
    userRows,
  ]);

  const selectableRows = useMemo(() => filteredUsers.filter((row) => !row.isSelf), [filteredUsers]);
  const selectedVisibleIds = useMemo(
    () => selectableRows.filter((row) => selectedIds.has(row.id)).map((row) => row.id),
    [selectableRows, selectedIds],
  );
  const allVisibleSelected =
    selectableRows.length > 0 && selectedVisibleIds.length === selectableRows.length;
  const selectedCount = selectedIds.size;
  const toggleSelection = useCallback((row: UserRow, isSelected: boolean) => {
    if (row.isSelf) {
      return;
    }
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (isSelected) {
        next.delete(row.id);
      } else {
        next.add(row.id);
      }
      return next;
    });
  }, []);
  const shouldToggleSelection = useCallback((event: React.MouseEvent<HTMLDivElement>) => {
    const rowElement = event.currentTarget;
    const checkboxCell = rowElement.querySelector<HTMLElement>('.user-admin-page__cell--checkbox');
    if (!checkboxCell) {
      return false;
    }
    const rowStyles = window.getComputedStyle(rowElement);
    const columnGap = Number.parseFloat(rowStyles.columnGap || '0');
    const checkboxRect = checkboxCell.getBoundingClientRect();
    return event.clientX <= checkboxRect.right + columnGap;
  }, []);

  const deleteMutation = useMutation<
    number,
    Error,
    number[],
    { previousUsers?: ApiUser[]; previousSelectedIds?: Set<number> }
  >({
    mutationFn: async (ids: number[]) => {
      const results = await Promise.allSettled(ids.map((id) => deleteUser(id)));
      const failed = results.filter((result) => result.status === 'rejected');
      if (failed.length > 0) {
        throw new Error(`Failed to delete ${failed.length} user(s).`);
      }
      return ids.length;
    },
    onMutate: async (ids) => {
      setActionError(null);
      await queryClient.cancelQueries({ queryKey: [USERS_QUERY_KEY] });
      const previousUsers = queryClient.getQueryData<ApiUser[]>(USERS_QUERY_KEY);
      const previousSelectedIds = new Set(selectedIds);
      const deletedIds = new Set(ids);
      setSelectedIds((prev) => {
        if (prev.size === 0) {
          return prev;
        }
        const next = new Set(prev);
        deletedIds.forEach((id) => next.delete(id));
        return next;
      });
      if (previousUsers) {
        queryClient.setQueryData<ApiUser[]>(
          USERS_QUERY_KEY,
          previousUsers.filter((user) => !deletedIds.has(user.id)),
        );
      }
      return { previousUsers, previousSelectedIds };
    },
    onError: (error, _ids, context) => {
      if (context?.previousUsers) {
        queryClient.setQueryData(USERS_QUERY_KEY, context.previousUsers);
      }
      if (context?.previousSelectedIds) {
        setSelectedIds(context.previousSelectedIds);
      }
      setShowDeleteConfirm(false);
      setActionError(error instanceof Error ? error.message : 'Failed to delete selected users.');
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY] });
      setSelectedIds(new Set());
      setShowDeleteConfirm(false);
    },
  });

  const disableMutation = useMutation<
    number,
    Error,
    number[],
    { previousUsers?: ApiUser[]; previousSelectedIds?: Set<number> }
  >({
    mutationFn: async (ids: number[]) => {
      if (!users) {
        throw new Error('User list not available');
      }
      const userById = new Map(users.map((entry) => [entry.id, entry]));
      const updates = ids
        .map((id) => userById.get(id))
        .filter((entry): entry is ApiUser => Boolean(entry));
      const results = await Promise.allSettled(
        updates.map((entry) =>
          updateUser(entry.id, {
            enabled: false,
            canTranslateAllLocales: entry.canTranslateAllLocales,
            userLocales: buildLocalePayload(entry),
            authorities:
              entry.authorities && entry.authorities.length
                ? entry.authorities
                : [{ authority: getPrimaryRole(entry) }],
          }),
        ),
      );
      const failed = results.filter((result) => result.status === 'rejected');
      if (failed.length > 0) {
        throw new Error(`Failed to disable ${failed.length} user(s).`);
      }
      return ids.length;
    },
    onMutate: async (ids) => {
      setActionError(null);
      await queryClient.cancelQueries({ queryKey: [USERS_QUERY_KEY] });
      const previousUsers = queryClient.getQueryData<ApiUser[]>(USERS_QUERY_KEY);
      const previousSelectedIds = new Set(selectedIds);
      const disabledIds = new Set(ids);
      setSelectedIds((prev) => {
        if (prev.size === 0) {
          return prev;
        }
        const next = new Set(prev);
        disabledIds.forEach((id) => next.delete(id));
        return next;
      });
      if (previousUsers) {
        queryClient.setQueryData<ApiUser[]>(
          USERS_QUERY_KEY,
          previousUsers.map((user) =>
            disabledIds.has(user.id)
              ? {
                  ...user,
                  enabled: false,
                }
              : user,
          ),
        );
      }
      return { previousUsers, previousSelectedIds };
    },
    onError: (error, _ids, context) => {
      if (context?.previousUsers) {
        queryClient.setQueryData(USERS_QUERY_KEY, context.previousUsers);
      }
      if (context?.previousSelectedIds) {
        setSelectedIds(context.previousSelectedIds);
      }
      setShowDisableConfirm(false);
      setActionError(error instanceof Error ? error.message : 'Failed to disable selected users.');
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY] });
      setSelectedIds(new Set());
      setShowDisableConfirm(false);
    },
  });

  const totalUsers = userRows.length;
  const filteredCount = filteredUsers.length;
  const isProgressActive = isFetching || deleteMutation.isPending || disableMutation.isPending;
  const [showProgressSpinner, setShowProgressSpinner] = useState(false);

  useEffect(() => {
    if (isProgressActive) {
      setShowProgressSpinner(true);
      return;
    }
    if (!showProgressSpinner) {
      return;
    }
    const timeoutId = window.setTimeout(() => {
      setShowProgressSpinner(false);
    }, 350);
    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [isProgressActive, showProgressSpinner]);

  const scrollElementRef = useRef<HTMLDivElement>(null);
  const getItemKey = useCallback(
    (index: number) => filteredUsers[index]?.id ?? index,
    [filteredUsers],
  );

  const estimateSize = useCallback(
    () =>
      getRowHeightPx({
        element: scrollElementRef.current,
        cssVariable: '--user-admin-row-height',
        defaultRem: 3,
      }),
    [],
  );

  const { items, totalSize, measureElement } = useVirtualRows<HTMLDivElement>({
    count: filteredUsers.length,
    estimateSize,
    getScrollElement: () => scrollElementRef.current,
    overscan: 8,
    getItemKey,
  });

  const { getRowRef } = useMeasuredRowRefs<number, HTMLDivElement>({ measureElement });

  if (!isAdmin) {
    return <Navigate to="/repositories" replace />;
  }

  const listEmptyMessage = isLoading
    ? 'Loading users...'
    : isError
      ? 'Failed to load users.'
      : normalizedQuery
        ? 'No users match this search.'
        : 'No users found.';

  return (
    <div className="settings-page user-admin-page">
      <div className="settings-page__header">
        <h1>User settings</h1>
      </div>

      <div className="user-admin-page__toolbar">
        <SearchControl
          value={searchQuery}
          onChange={setSearchQuery}
          placeholder="Search by id, username, or name"
          disabled={isLoading || isError || totalUsers === 0}
          className="user-admin-page__search"
        />
        <MultiSectionFilterChip
          ariaLabel="Filter users"
          align="right"
          className="filter-chip user-admin-page__filter"
          classNames={{
            button: 'filter-chip__button',
            panel: 'filter-chip__panel',
            section: 'filter-chip__section',
            label: 'filter-chip__label',
            list: 'filter-chip__list',
            option: 'filter-chip__option',
          }}
          sections={[
            {
              kind: 'radio',
              label: 'Role',
              options: ROLE_FILTER_OPTIONS,
              value: roleFilter,
              onChange: (value) => setRoleFilter(value as string),
            },
            {
              kind: 'radio',
              label: 'Status',
              options: STATUS_FILTER_OPTIONS,
              value: statusFilter,
              onChange: (value) => setStatusFilter(value as string),
            },
            {
              kind: 'date',
              label: 'Created date',
              after: createdAfter,
              before: createdBefore,
              onChangeAfter: setCreatedAfter,
              onChangeBefore: setCreatedBefore,
              quickRanges: dateQuickRanges,
              onClear: () => {
                setCreatedAfter(null);
                setCreatedBefore(null);
              },
            },
          ]}
          disabled={isLoading || isError || totalUsers === 0}
        />
        <button
          type="button"
          className="settings-button settings-button--primary user-admin-page__create"
          onClick={() => {
            void navigate('/settings/admin/users/batch');
          }}
        >
          Create users
        </button>
      </div>
      <div className="user-admin-page__count">
        <span className="user-admin-page__count-text">
          {filteredCount === 0
            ? 'No results'
            : `Showing ${filteredCount} ${filteredCount === 1 ? 'result' : 'results'}`}
        </span>
        <div className="user-admin-page__count-actions">
          <button
            type="button"
            className="user-admin-page__count-link"
            onClick={() => {
              setSelectedIds((prev) => {
                const next = new Set(prev);
                if (allVisibleSelected) {
                  selectableRows.forEach((row) => next.delete(row.id));
                } else {
                  selectableRows.forEach((row) => next.add(row.id));
                }
                return next;
              });
            }}
            disabled={!selectableRows.length}
          >
            {allVisibleSelected ? 'Clear selection' : 'Select all'}
          </button>
          <span className="user-admin-page__count-sep">•</span>
          <SingleSelectDropdown
            label="Bulk actions"
            value={null}
            options={[
              { value: 'disable', label: 'Disable' },
              { value: 'delete', label: 'Delete' },
            ]}
            placeholder={`${selectedCount} selected`}
            onChange={(next) => {
              if (!next) {
                return;
              }
              if (next === 'disable') {
                setShowDisableConfirm(true);
              }
              if (next === 'delete') {
                setShowDeleteConfirm(true);
              }
            }}
            className="user-admin-page__bulk"
            disabled={!selectedCount || disableMutation.isPending || deleteMutation.isPending}
            searchable={false}
            getOptionClassName={(option) => {
              if (option.value === 'delete') {
                return 'is-destructive';
              }
              if (option.value === 'disable') {
                return 'is-warning';
              }
              return '';
            }}
          />
        </div>
        <div
          className="user-admin-page__count-spinner"
          data-visible={showProgressSpinner ? 'true' : 'false'}
        >
          <span className="spinner" aria-hidden />
        </div>
      </div>
      {actionError ? <div className="user-admin-page__error">{actionError}</div> : null}

      <div className="user-admin-page__table" role="table" aria-label="User list">
        <div className="user-admin-page__table-header" role="row">
          <div
            className="user-admin-page__cell user-admin-page__cell--checkbox"
            role="columnheader"
          />
          <div className="user-admin-page__cell" role="columnheader">
            ID
          </div>
          <div className="user-admin-page__cell" role="columnheader">
            Username
          </div>
          <div className="user-admin-page__cell" role="columnheader">
            Display name
          </div>
          <div className="user-admin-page__cell" role="columnheader">
            Role
          </div>
          <div className="user-admin-page__cell" role="columnheader">
            Disabled
          </div>
          <div className="user-admin-page__cell" role="columnheader">
            Locales
          </div>
        </div>
        {filteredUsers.length ? (
          <VirtualList
            scrollRef={scrollElementRef}
            items={items}
            totalSize={totalSize}
            renderRow={(virtualItem) => {
              const row = filteredUsers[virtualItem.index];
              if (!row) {
                return null;
              }
              const isSelected = selectedIds.has(row.id);
              return {
                key: row.id,
                className: 'user-admin-page__row',
                props: {
                  ref: getRowRef(row.id),
                  role: 'row',
                  tabIndex: 0,
                  onClick: (event) => {
                    const target = event.target as HTMLElement | null;
                    if (target && target.closest('.user-admin-page__cell--checkbox')) {
                      return;
                    }
                    if (shouldToggleSelection(event)) {
                      toggleSelection(row, isSelected);
                      return;
                    }
                    void navigate(`/settings/admin/users/${row.id}`);
                  },
                  onKeyDown: (event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                      event.preventDefault();
                      void navigate(`/settings/admin/users/${row.id}`);
                    }
                  },
                },
                content: (
                  <>
                    <div
                      className="user-admin-page__cell user-admin-page__cell--checkbox"
                      role="cell"
                    >
                      <input
                        type="checkbox"
                        checked={isSelected}
                        disabled={row.isSelf}
                        title={row.isSelf ? 'You cannot delete your own account.' : undefined}
                        onChange={() => {
                          toggleSelection(row, isSelected);
                        }}
                        onClick={(event) => event.stopPropagation()}
                        aria-label={`Select user ${row.username}`}
                      />
                    </div>
                    <div className="user-admin-page__cell user-admin-page__cell--muted" role="cell">
                      {row.id}
                    </div>
                    <div className="user-admin-page__cell" role="cell">
                      {row.username}
                    </div>
                    <div className="user-admin-page__cell" role="cell">
                      {row.displayName}
                    </div>
                    <div className="user-admin-page__cell" role="cell">
                      {row.roleLabel}
                    </div>
                    <div className="user-admin-page__cell user-admin-page__cell--muted" role="cell">
                      {row.disabledLabel}
                    </div>
                    <div className="user-admin-page__cell user-admin-page__cell--muted" role="cell">
                      {row.localeSummary}
                    </div>
                  </>
                ),
              };
            }}
          />
        ) : (
          <div className="user-admin-page__empty">
            <div className="user-admin-page__empty-text">{listEmptyMessage}</div>
            {isError ? (
              <button
                type="button"
                className="user-admin-page__retry"
                onClick={() => {
                  void refetch();
                }}
              >
                Try again
              </button>
            ) : null}
          </div>
        )}
      </div>
      <ConfirmModal
        open={showDeleteConfirm}
        title={`Delete ${selectedCount} user${selectedCount === 1 ? '' : 's'}?`}
        body="This will disable the selected users and remove access. This action cannot be undone."
        confirmLabel={deleteMutation.isPending ? 'Deleting...' : 'Delete'}
        cancelLabel="Cancel"
        onCancel={() => setShowDeleteConfirm(false)}
        onConfirm={() => {
          const ids = Array.from(selectedIds).filter((id) =>
            selectableRows.some((row) => row.id === id),
          );
          if (!ids.length) {
            setShowDeleteConfirm(false);
            return;
          }
          deleteMutation.mutate(ids);
        }}
      />
      <ConfirmModal
        open={showDisableConfirm}
        title={`Disable ${selectedCount} user${selectedCount === 1 ? '' : 's'}?`}
        body="Selected users will be disabled and unable to sign in. You can re-enable them later."
        confirmLabel={disableMutation.isPending ? 'Disabling...' : 'Disable'}
        cancelLabel="Cancel"
        onCancel={() => setShowDisableConfirm(false)}
        onConfirm={() => {
          const ids = Array.from(selectedIds).filter((id) =>
            selectableRows.some((row) => row.id === id),
          );
          if (!ids.length) {
            setShowDisableConfirm(false);
            return;
          }
          disableMutation.mutate(ids);
        }}
      />
    </div>
  );
}
