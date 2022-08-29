package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Extends the regular import translation step that is used to import XLIFF coming from a
 * translation kit to import by MD5 instead of using text unit ids and translation kit id.
 *
 * <p>TODO(P1) Review, this assume only 1 asset is in the repository, or if multiple it will
 * arbitrarily take the first md5 matches to import translations.
 *
 * @author jaurambault
 */
@Configurable
public class ImportTranslationsByMd5Step extends AbstractImportTranslationsStep {

  /** Logger */
  static Logger logger = LoggerFactory.getLogger(ImportTranslationsByMd5Step.class);

  Repository repository;

  public ImportTranslationsByMd5Step(Repository repository) {
    this.repository = repository;
  }

  @Override
  public String getName() {
    return "Import translations by MD5";
  }

  @Override
  public String getDescription() {
    return "Updates the TM with the extracted new/changed variants."
        + " Expects: raw document. Sends back: original events.";
  }

  @Override
  TMTextUnit getTMTextUnit() {
    return tmTextUnitRepository.findFirstByTmAndMd5(repository.getTm(), md5);
  }
}
