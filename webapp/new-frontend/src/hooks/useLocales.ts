import { useQuery } from '@tanstack/react-query';

import type { ApiLocale } from '../api/locales';
import { fetchLocales } from '../api/locales';

const LOCALES_QUERY_KEY = ['locales'];

export const useLocales = () => {
  return useQuery<ApiLocale[]>({
    queryKey: LOCALES_QUERY_KEY,
    queryFn: fetchLocales,
    staleTime: 30_000,
  });
};

export { LOCALES_QUERY_KEY };
