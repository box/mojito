import type { KeyboardEvent as ReactKeyboardEvent } from 'react';

const MAC_PLATFORM_REGEX = /mac|iphone|ipod|ipad/i;

const getNavigator = () => (typeof navigator === 'undefined' ? null : navigator);

export function isMacPlatform(nav: Navigator | null = getNavigator()) {
  if (!nav) {
    return false;
  }

  const platform =
    (nav as { userAgentData?: { platform?: string } }).userAgentData?.platform ??
    nav.platform ??
    (nav as Navigator & { userAgent?: string }).userAgent ??
    '';

  return MAC_PLATFORM_REGEX.test(platform);
}

type KeyEvent = ReactKeyboardEvent | KeyboardEvent;

export function isPrimaryActionShortcut(event: KeyEvent, nav: Navigator | null = getNavigator()) {
  if (event.key !== 'Enter') {
    return false;
  }
  return isMacPlatform(nav) ? event.metaKey : event.ctrlKey;
}

export function getPrimaryShortcutAriaLabel(nav: Navigator | null = getNavigator()) {
  return isMacPlatform(nav) ? 'Command plus Enter' : 'Control plus Enter';
}
