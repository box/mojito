import { useQuery } from '@tanstack/react-query';

import type { ApiUser } from '../api/users';
import { fetchAllUsersAdmin } from '../api/users';

const USERS_QUERY_KEY = ['users'];

export const useUsers = () => {
  return useQuery<ApiUser[]>({
    queryKey: USERS_QUERY_KEY,
    queryFn: () => fetchAllUsersAdmin(),
    staleTime: 30_000,
  });
};

export { USERS_QUERY_KEY };
