import type { ApiLocale } from './locales';

export type ApiUserProfile = {
  username: string;
  role: 'ROLE_PM' | 'ROLE_TRANSLATOR' | 'ROLE_ADMIN' | 'ROLE_USER';
  canTranslateAllLocales: boolean;
  userLocales: string[];
};

export type ApiAuthority = {
  authority: string;
};

export type ApiUserLocale = {
  id?: number;
  locale: ApiLocale;
};

export type ApiUser = {
  id: number;
  username: string;
  givenName?: string | null;
  surname?: string | null;
  commonName?: string | null;
  enabled?: boolean | null;
  createdDate?: string | null;
  canTranslateAllLocales: boolean;
  authorities?: ApiAuthority[] | null;
  userLocales?: ApiUserLocale[] | null;
};

export type UpdateUserPayload = {
  username?: string;
  givenName?: string | null;
  surname?: string | null;
  commonName?: string | null;
  password?: string;
  enabled?: boolean;
  canTranslateAllLocales: boolean;
  userLocales: ApiUserLocale[];
  authorities?: ApiAuthority[];
};

export type CreateUserPayload = {
  username: string;
  password: string;
  canTranslateAllLocales: boolean;
  userLocales: ApiUserLocale[];
  authorities?: ApiAuthority[];
  givenName?: string | null;
  surname?: string | null;
  commonName?: string | null;
};

export async function fetchCurrentUser(): Promise<ApiUserProfile> {
  const response = await fetch('/api/users/me', {
    credentials: 'same-origin',
    headers: { Accept: 'application/json' },
  });

  if (!response.ok) {
    const message = await response.text().catch(() => '');
    throw new Error(message || 'Failed to load user profile');
  }

  return (await response.json()) as ApiUserProfile;
}

export async function fetchAllUsersAdmin(): Promise<ApiUser[]> {
  const response = await fetch('/api/users/admin', {
    credentials: 'include',
    headers: { Accept: 'application/json' },
  });

  if (!response.ok) {
    const message = await response.text().catch(() => '');
    throw new Error(message || 'Failed to load users');
  }

  return (await response.json()) as ApiUser[];
}

export async function updateUser(userId: number, payload: UpdateUserPayload): Promise<void> {
  const response = await fetch(`/api/users/${userId}`, {
    method: 'PATCH',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const message = await response.text().catch(() => '');
    throw new Error(message || 'Failed to update user');
  }
}

export async function deleteUser(userId: number): Promise<void> {
  const response = await fetch(`/api/users/${userId}`, {
    method: 'DELETE',
    credentials: 'include',
  });

  if (!response.ok) {
    const message = await response.text().catch(() => '');
    throw new Error(message || 'Failed to delete user');
  }
}

export async function createUser(payload: CreateUserPayload): Promise<void> {
  const response = await fetch('/api/users', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const message = await response.text().catch(() => '');
    throw new Error(message || 'Failed to create user');
  }
}
