package com.box.l10n.mojito.okapi;

import static org.junit.Assert.*;

import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.TextUnit;
import org.junit.Test;

/**
 * @author jaurambault
 */
public class TextUnitDTOAnnotationsTest {

  @Test
  public void testGetTextUnitDTO() {

    ITextUnit textUnit = new TextUnit();
    TextUnitDTO textUnitDTO = new TextUnitDTO();
    textUnit.setAnnotation(new TextUnitDTOAnnotation(textUnitDTO));

    TextUnitDTOAnnotations instance = new TextUnitDTOAnnotations();

    TextUnitDTO expResult = textUnitDTO;
    TextUnitDTO result = instance.getTextUnitDTO(textUnit);

    assertEquals(expResult, result);
  }

  @Test
  public void testGetTextUnitDTONull() {

    ITextUnit textUnit = new TextUnit();

    TextUnitDTOAnnotations instance = new TextUnitDTOAnnotations();

    TextUnitDTO expResult = null;
    TextUnitDTO result = instance.getTextUnitDTO(textUnit);

    assertEquals(expResult, result);
  }
}
