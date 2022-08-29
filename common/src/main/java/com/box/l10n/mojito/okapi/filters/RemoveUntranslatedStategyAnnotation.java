package com.box.l10n.mojito.okapi.filters;

import com.google.common.base.Preconditions;
import net.sf.okapi.common.annotation.IAnnotation;

/**
 * Holds the strategy to remove untranslated text units.
 *
 * <p>To be set by the filter depending on their implementation.
 *
 * <p>Some filter may work by dropping the whole text unit (eg. properties) while other may generate
 * a corrupted output (JSON filter)
 *
 * @author jaurambault
 */
public class RemoveUntranslatedStategyAnnotation implements IAnnotation {

  RemoveUntranslatedStrategy removeUntranslatedStrategy;

  public RemoveUntranslatedStategyAnnotation(
      RemoveUntranslatedStrategy removeUntranslatedStrategy) {
    this.removeUntranslatedStrategy = Preconditions.checkNotNull(removeUntranslatedStrategy);
  }

  public RemoveUntranslatedStrategy getUntranslatedStrategy() {
    return removeUntranslatedStrategy;
  }
}
