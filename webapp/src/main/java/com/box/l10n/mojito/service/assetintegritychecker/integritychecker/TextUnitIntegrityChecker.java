package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import com.box.l10n.mojito.common.notification.IntegrityCheckNotifier;
import com.box.l10n.mojito.entity.Repository;

/**
 * @author aloison
 */
public interface TextUnitIntegrityChecker {

  void check(String sourceContent, String targetContent) throws IntegrityCheckException;

  LocalizableString extractNonLocalizableParts(String string);

  String restoreNonLocalizableParts(LocalizableString localizableString);

  void setIntegrityCheckNotifier(IntegrityCheckNotifier integrityCheckNotifier);

  void setRepository(Repository icnRepository);

  void setTextUnitId(Long icnTextUnitId);

  Repository getRepository();

  Long getTextUnitId();

  IntegrityCheckNotifier getIntegrityCheckNotifier();
}
