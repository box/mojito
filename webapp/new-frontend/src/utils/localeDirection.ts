const RTL_LANG_CODES = new Set([
  'ar',
  'arc',
  'dv',
  'fa',
  'ha',
  'he',
  'iw',
  'ku',
  'ks',
  'ps',
  'ur',
  'yi',
]);

export function isRtlLocale(locale: string) {
  const normalized = locale.toLowerCase();
  const [languageCode] = normalized.split('-');
  return RTL_LANG_CODES.has(normalized) || RTL_LANG_CODES.has(languageCode);
}
