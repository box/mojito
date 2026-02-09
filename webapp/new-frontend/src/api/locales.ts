export type ApiLocale = {
  bcp47Tag: string;
};

export const fetchLocales = async (): Promise<ApiLocale[]> => {
  const response = await fetch('/api/locales');

  if (!response.ok) {
    const message = await response.text().catch(() => '');
    throw new Error(message || 'Failed to load locales');
  }

  return (await response.json()) as ApiLocale[];
};
