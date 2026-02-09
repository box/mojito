package com.box.l10n.mojito.service.assetExtraction;

public enum LeveragingType {
  /** Existing asset-local behavior: prioritize name/source heuristics from asset-scoped cache. */
  LEGACY_SOURCE,

  /** Asset-local match only, using exact source+comment. */
  ASSET_SOURCE_AND_COMMENT,

  /**
   * Cross-asset fallback mode: keep asset-scoped matching first, then lookup repository-wide with
   * priority: 1) md5 (name+source+comment), 2) source+comment.
   */
  CROSS_ASSET_FALLBACK;
}
