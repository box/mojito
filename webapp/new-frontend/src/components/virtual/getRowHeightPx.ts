const DEFAULT_FONT_SIZE_PX = 16;

type GetRowHeightPxOptions = {
  element?: HTMLElement | null;
  cssVariable: string;
  defaultRem: number;
};

export function getRowHeightPx({ element, cssVariable, defaultRem }: GetRowHeightPxOptions) {
  const remBase = (() => {
    if (typeof window === 'undefined') {
      return DEFAULT_FONT_SIZE_PX;
    }
    const rootFontSize = Number.parseFloat(getComputedStyle(document.documentElement).fontSize);
    return Number.isNaN(rootFontSize) ? DEFAULT_FONT_SIZE_PX : rootFontSize;
  })();

  const fallbackPx = defaultRem * remBase;

  if (!element || typeof window === 'undefined') {
    return fallbackPx;
  }

  const styles = getComputedStyle(element);
  const rawValue = styles.getPropertyValue(cssVariable).trim();
  if (!rawValue) {
    return fallbackPx;
  }

  const numericValue = Number.parseFloat(rawValue);
  if (Number.isNaN(numericValue)) {
    return fallbackPx;
  }

  const elementFontSize = Number.parseFloat(styles.fontSize);
  const emBase = Number.isNaN(elementFontSize) ? remBase : elementFontSize;

  if (rawValue.endsWith('rem')) {
    return numericValue * remBase;
  }

  if (rawValue.endsWith('em')) {
    return numericValue * emBase;
  }

  return numericValue;
}
