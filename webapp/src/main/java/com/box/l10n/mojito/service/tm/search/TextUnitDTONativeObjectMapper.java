package com.box.l10n.mojito.service.tm.search;

import static com.box.l10n.mojito.entity.TMTextUnitVariant.Status.TRANSLATION_NEEDED;
import static com.box.l10n.mojito.entity.TMTextUnitVariant.Status.valueOf;

import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.github.pnowy.nc.core.CriteriaResult;
import com.github.pnowy.nc.core.mappers.NativeObjectMapper;
import org.joda.time.DateTime;

/** @author jaurambault */
public class TextUnitDTONativeObjectMapper implements NativeObjectMapper<TextUnitDTO> {

  @Override
  public TextUnitDTO mapObject(CriteriaResult cr) {

    int idx = 0;

    TextUnitDTO t = new TextUnitDTO();
    t.setTmTextUnitId(cr.getLong(idx++));
    t.setTmTextUnitVariantId(cr.getLong(idx++));

    // TODO(PO) THIS NOT CONSISTANT !! chooose
    t.setLocaleId(cr.getLong(idx++));
    t.setTargetLocale(cr.getString(idx++));
    t.setName(cr.getString(idx++));
    t.setSource(cr.getString(idx++));
    t.setComment(cr.getString(idx++));
    t.setTarget(cr.getString(idx++));
    t.setTargetComment(cr.getString(idx++));
    t.setAssetId(cr.getLong(idx++));
    t.setLastSuccessfulAssetExtractionId(cr.getLong(idx++));
    t.setAssetExtractionId(cr.getLong(idx++));
    t.setTmTextUnitCurrentVariantId(cr.getLong(idx++));
    t.setStatus(getStatus(cr.getString(idx++)));

    String includedInLocalizedFile = cr.getString(idx++);
    // TODO(P1) getBoolean doesn't work nor getValue with a cast
    // to boolean (previous code). It would require more digging in lib
    // implementation to understand why getBoolean doesn't work. This
    // seems to work fine, use this code for now.
    t.setIncludedInLocalizedFile(Boolean.valueOf(includedInLocalizedFile));
    t.setCreatedDate(new DateTime(cr.getDate(idx++)));
    String assetDeleted = cr.getString(idx++);
    t.setAssetDeleted(Boolean.valueOf(assetDeleted));
    t.setPluralForm(cr.getString(idx++));
    t.setPluralFormOther(cr.getString(idx++));
    t.setRepositoryName(cr.getString(idx++));
    t.setAssetPath(cr.getString(idx++));
    t.setAssetTextUnitId(cr.getLong(idx++));
    t.setTmTextUnitCreatedDate(new DateTime(cr.getDate(idx++)));
    t.setDoNotTranslate(Boolean.valueOf(includedInLocalizedFile));

    String doNotTranslate = cr.getString(idx++);
    t.setDoNotTranslate(Boolean.valueOf(doNotTranslate));

    return t;
  }

  /**
   * Gets the status for the status string returned by the query. That string can be null for text
   * unit that maps to an untranslated string. In that case the status will be {@link
   * TMTextUnitVariant.Status.TRANSLATION_NEEDED} .
   *
   * @param statusStr status string coming from the dataset (can be null)
   * @return the status
   */
  public TMTextUnitVariant.Status getStatus(String statusStr) {

    TMTextUnitVariant.Status status;

    if (statusStr == null) {
      status = TRANSLATION_NEEDED;
    } else {
      status = valueOf(statusStr);
    }

    return status;
  }
}
