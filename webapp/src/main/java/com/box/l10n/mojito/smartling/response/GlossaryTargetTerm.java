package com.box.l10n.mojito.smartling.response;

public class GlossaryTargetTerm extends GlossarySourceTerm {

  GlossaryTermTranslation glossaryTermTranslation;

  public GlossaryTermTranslation getGlossaryTermTranslation() {
    return glossaryTermTranslation;
  }

  public void setGlossaryTermTranslation(GlossaryTermTranslation glossaryTermTranslation) {
    this.glossaryTermTranslation = glossaryTermTranslation;
  }
}
