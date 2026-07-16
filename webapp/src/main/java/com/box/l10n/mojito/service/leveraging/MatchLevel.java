package com.box.l10n.mojito.service.leveraging;

/**
 * Confidence levels for leveraging matches. The precision order is defined per mode in {@link
 * CopyTmLeverager#findBestMatchLevel}, not by enum ordinal.
 *
 * <p>MD5 and NAME_AND_CONTENT are high-confidence (translation can be reused as-is). NAME_ONLY and
 * CONTENT_ONLY are low-confidence (translation should be flagged for review). NAME_ONLY and
 * CONTENT_ONLY are at the same conceptual tier — they never compete since NAME_ONLY is only used in
 * NAME mode and CONTENT_ONLY only in EXACT mode.
 *
 * @author wwawrzenczak
 */
public enum MatchLevel {
  MD5(true),
  NAME_AND_CONTENT(true),
  NAME_ONLY(false),
  CONTENT_ONLY(false);

  private final boolean highPrecision;

  MatchLevel(boolean highPrecision) {
    this.highPrecision = highPrecision;
  }

  public boolean isHighPrecision() {
    return highPrecision;
  }
}
