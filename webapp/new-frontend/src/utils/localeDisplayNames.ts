import { useMemo } from 'react';

type LocaleResolver = (tag: string) => string;

const ENGLISH_LOCALE = 'en';

const createLocaleDisplayNameResolver = (): LocaleResolver => {
  // UI is English-only today, so we hardcode the language for display names.
  // If/when we localize the UI we should source preferred locales from FormatJS config.
  const languageNames = new Intl.DisplayNames([ENGLISH_LOCALE], {
    type: 'language',
    fallback: 'code',
  });
  const regionNames = new Intl.DisplayNames([ENGLISH_LOCALE], {
    type: 'region',
    fallback: 'code',
  });
  const scriptNames = new Intl.DisplayNames([ENGLISH_LOCALE], {
    type: 'script',
    fallback: 'code',
  });

  return (tag: string) => {
    try {
      const locale = new Intl.Locale(tag);
      const language = locale.language;
      const script = locale.script;
      const region = locale.region;

      let displayName = language ? (languageNames.of(language) ?? language) : tag;

      const qualifiers: string[] = [];
      if (script) {
        qualifiers.push(scriptNames.of(script) ?? script);
      }

      if (region) {
        qualifiers.push(regionNames.of(region) ?? region);
      }

      if (qualifiers.length) {
        displayName = `${displayName} (${qualifiers.join(', ')})`;
      }

      return displayName;
    } catch {
      return tag;
    }
  };
};

export const useLocaleDisplayNameResolver = (): LocaleResolver => {
  return useMemo(createLocaleDisplayNameResolver, []);
};
