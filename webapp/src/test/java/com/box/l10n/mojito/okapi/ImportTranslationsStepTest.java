package com.box.l10n.mojito.okapi;

import static org.junit.Assert.*;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import net.sf.okapi.common.resource.TextContainer;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author jaurambault
 */
public class ImportTranslationsStepTest {

  @Test
  public void testGetStatusWithSpecificImportStatus() {

    TextContainer target = Mockito.mock(TextContainer.class);
    Mockito.when(target.getProperty(com.box.l10n.mojito.okapi.Property.STATE))
        .thenReturn(new net.sf.okapi.common.resource.Property("state", "doesnt matter"));

    ImportTranslationsByIdStep importTranslationsStep = new ImportTranslationsByIdStep();
    importTranslationsStep.importWithStatus = TMTextUnitVariant.Status.APPROVED;

    TMTextUnitVariant.Status expResult = TMTextUnitVariant.Status.APPROVED;
    TMTextUnitVariant.Status result =
        importTranslationsStep.getStatusForImport(new TMTextUnit(), target);
    assertEquals(expResult, result);
  }

  @Test
  public void testGetStatusFromStateTranslated() {

    TextContainer target = Mockito.mock(TextContainer.class);
    Mockito.when(target.getProperty(com.box.l10n.mojito.okapi.Property.STATE))
        .thenReturn(new net.sf.okapi.common.resource.Property("state", "translated"));

    ImportTranslationsByIdStep importTranslationsStep = new ImportTranslationsByIdStep();

    TMTextUnitVariant.Status expResult = TMTextUnitVariant.Status.REVIEW_NEEDED;
    TMTextUnitVariant.Status result =
        importTranslationsStep.getStatusForImport(new TMTextUnit(), target);
    assertEquals(expResult, result);
  }

  @Test
  public void testGetStatusFromStateSignedOff() {

    TextContainer target = Mockito.mock(TextContainer.class);
    Mockito.when(target.getProperty(com.box.l10n.mojito.okapi.Property.STATE))
        .thenReturn(new net.sf.okapi.common.resource.Property("state", "signed-off"));

    ImportTranslationsByIdStep importTranslationsStep = new ImportTranslationsByIdStep();

    TMTextUnitVariant.Status expResult = TMTextUnitVariant.Status.APPROVED;
    TMTextUnitVariant.Status result =
        importTranslationsStep.getStatusForImport(new TMTextUnit(), target);
    assertEquals(expResult, result);
  }

  @Test
  public void testGetStatusFromStateUnsupported() {

    TextContainer target = Mockito.mock(TextContainer.class);
    Mockito.when(target.getProperty(com.box.l10n.mojito.okapi.Property.STATE))
        .thenReturn(new net.sf.okapi.common.resource.Property("state", "unsupported"));

    ImportTranslationsByIdStep importTranslationsStep = new ImportTranslationsByIdStep();

    TMTextUnitVariant.Status expResult = null;
    TMTextUnitVariant.Status result =
        importTranslationsStep.getStatusForImport(new TMTextUnit(), target);
    assertEquals(expResult, result);
  }
}
