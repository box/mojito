import { useQuery } from '@tanstack/react-query';

import { type ApiUserProfile, fetchCurrentUser } from '../api/users';

export function useCurrentUser() {
  return useQuery<ApiUserProfile, Error>({
    queryKey: ['current-user'],
    queryFn: fetchCurrentUser,
    staleTime: 5 * 60 * 1000,
    retry: false,
  });
}
