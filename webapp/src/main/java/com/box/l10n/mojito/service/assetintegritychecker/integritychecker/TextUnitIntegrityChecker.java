package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

/**
 * @author aloison
 */
public interface TextUnitIntegrityChecker {

  void check(String sourceContent, String targetContent) throws IntegrityCheckException;

  LocalizableString extractNonLocalizableParts(String string);

  String restoreNonLocalizableParts(LocalizableString localizableString);
}
