package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import com.box.l10n.mojito.common.notification.IntegrityCheckNotifier;
import com.box.l10n.mojito.entity.Repository;
import java.util.Map;

/**
 * Base class that contains the logic to process non-localizable parts of the string such as
 * placeholders.
 *
 * @author jyi
 */
public abstract class AbstractTextUnitIntegrityChecker implements TextUnitIntegrityChecker {

  private IntegrityCheckNotifier integrityCheckNotifier;
  private Repository repository;
  private Long textUnitId;

  /**
   * All non-localizable parts from the given string are extracted and replaced with identifiers.
   * {@link LocalizableString#nonLocalizableParts} is updated to have the map of identifiers and the
   * actual non-localizable parts of the string.
   *
   * @param string
   * @return {@link LocalizableString}
   */
  @Override
  public LocalizableString extractNonLocalizableParts(String string) {
    return new LocalizableString(string);
  }

  /**
   * Returns the string with non-localizable parts restored.
   *
   * @param localizableString
   * @return the actual string with non-localizable parts restored
   */
  @Override
  public String restoreNonLocalizableParts(LocalizableString localizableString) {
    Map<String, String> nonLocalizableParts = localizableString.getNonLocalizableParts();
    String restore = localizableString.getLocalizableString();
    for (String replacement : nonLocalizableParts.keySet()) {
      restore = restore.replace(replacement, nonLocalizableParts.get(replacement));
    }
    return restore;
  }

  @Override
  public void setIntegrityCheckNotifier(IntegrityCheckNotifier integrityCheckNotifier) {
    this.integrityCheckNotifier = integrityCheckNotifier;
  }

  @Override
  public void setRepository(Repository repository) {
    this.repository = repository;
  }

  @Override
  public void setTextUnitId(Long textUnitId) {
    this.textUnitId = textUnitId;
  }

  @Override
  public Repository getRepository() {
    return this.repository;
  }

  @Override
  public Long getTextUnitId() {
    return textUnitId;
  }

  @Override
  public IntegrityCheckNotifier getIntegrityCheckNotifier() {
    return integrityCheckNotifier;
  }
}
