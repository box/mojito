package com.box.l10n.mojito.okapi;

/**
 * Enum that contains XLIFF states, see
 * http://docs.oasis-open.org/xliff/v1.2/os/xliff-core.html#state
 *
 * @author jaurambault
 */
public enum XliffState {
  NEW("new"),
  TRANSLATED("translated"),
  SIGNED_OFF("signed-off"),
  FINAL("final"),
  NEEDS_REVIEW_TRANSLATION("needs-review-translation"),
  NEEDS_TRANSLATION("needs-translation");

  String value;

  XliffState(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }
}
