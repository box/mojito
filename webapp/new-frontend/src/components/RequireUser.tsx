import type { ReactNode } from 'react';
import { createContext, useContext } from 'react';

import type { ApiUserProfile } from '../api/users';
import { useCurrentUser } from '../hooks/useCurrentUser';

const UserContext = createContext<ApiUserProfile | null>(null);

export function RequireUser({ children }: { children: ReactNode }) {
  const { data, isLoading, isError } = useCurrentUser();

  if (isLoading) {
    return <div>Loading user…</div>;
  }

  if (isError || !data) {
    return <div>Could not load user information.</div>;
  }

  return <UserContext.Provider value={data}>{children}</UserContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useUser(): ApiUserProfile {
  const user = useContext(UserContext);
  if (!user) {
    throw new Error('useUser must be used within RequireUser');
  }
  return user;
}
