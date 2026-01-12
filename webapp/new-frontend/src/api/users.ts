export type ApiUserProfile = {
  username: string;
  role: 'ROLE_PM' | 'ROLE_TRANSLATOR' | 'ROLE_ADMIN' | 'ROLE_USER';
  canTranslateAllLocales: boolean;
  userLocales: string[];
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
