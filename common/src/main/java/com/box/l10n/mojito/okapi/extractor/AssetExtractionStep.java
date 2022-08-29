package com.box.l10n.mojito.okapi.extractor;

import com.box.l10n.mojito.okapi.filters.PluralFormAnnotation;
import com.box.l10n.mojito.okapi.filters.UsagesAnnotation;
import com.box.l10n.mojito.okapi.steps.AbstractMd5ComputationStep;
import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sf.okapi.common.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AssetExtractionStep extends AbstractMd5ComputationStep {

  static Logger logger = LoggerFactory.getLogger(AssetExtractionStep.class);

  Set<String> assetTextUnitMD5s;

  List<String> md5sToSkip;

  List<AssetExtractorTextUnit> assetExtractorTextUnits;

  public AssetExtractionStep(List<String> md5sToSkip) {
    this.md5sToSkip = MoreObjects.firstNonNull(md5sToSkip, Collections.emptyList());
    assetExtractorTextUnits = new ArrayList<>();
  }

  @Override
  protected Event handleStartDocument(Event event) {
    assetTextUnitMD5s = new HashSet<>();
    return super.handleStartDocument(event);
  }

  @Override
  public String getName() {
    return "Convert to text units step";
  }

  @Override
  public String getDescription() {
    return "Convert okapi text units to extraction text units";
  }

  @Override
  protected Event handleTextUnit(Event event) {
    Event eventToReturn = super.handleTextUnit(event);

    if (textUnit.isTranslatable()) {
      if (md5sToSkip.contains(md5)) {
        logger.debug("{} in list of md5s to be skipped", md5);
      } else if (!assetTextUnitMD5s.contains(md5)) {
        assetTextUnitMD5s.add(md5);

        PluralFormAnnotation annotation = textUnit.getAnnotation(PluralFormAnnotation.class);
        String pluralForm = null;
        String pluralFormOther = null;

        if (annotation != null) {
          pluralForm = annotation.getName();
          pluralFormOther = annotation.getOtherName();
        }

        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName(name);
        assetExtractorTextUnit.setSource(source);
        assetExtractorTextUnit.setComments(comments);
        assetExtractorTextUnit.setPluralForm(pluralForm);
        assetExtractorTextUnit.setPluralFormOther(pluralFormOther);
        assetExtractorTextUnit.setUsages(getUsages());

        assetExtractorTextUnits.add(assetExtractorTextUnit);
      } else {
        logger.debug("Duplicate assetTextUnit found, skip it");
      }
    }

    return eventToReturn;
  }

  Set<String> getUsages() {
    Set<String> usages = null;

    UsagesAnnotation usagesAnnotation = textUnit.getAnnotation(UsagesAnnotation.class);

    if (usagesAnnotation != null) {
      usages = usagesAnnotation.getUsages();
    }

    return usages;
  }

  public List<AssetExtractorTextUnit> getAssetExtractorTextUnits() {
    return assetExtractorTextUnits;
  }
}
