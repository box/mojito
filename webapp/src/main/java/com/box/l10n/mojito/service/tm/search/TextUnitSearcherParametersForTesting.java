package com.box.l10n.mojito.service.tm.search;

/**
 * To be used only by tests.
 *
 * @author jaurambault
 */
public class TextUnitSearcherParametersForTesting extends TextUnitSearcherParameters {

  boolean ordered = true;

  public boolean isOrdered() {
    return ordered;
  }

  public void setOrdered(boolean ordered) {
    this.ordered = ordered;
  }
}
