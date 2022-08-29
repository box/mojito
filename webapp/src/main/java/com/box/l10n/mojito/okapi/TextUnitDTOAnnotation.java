package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import net.sf.okapi.common.annotation.IAnnotation;

/**
 * Contains a {@link TextUnitDTO} link to the Okapi text unit.
 *
 * @author jaurambault
 */
public class TextUnitDTOAnnotation implements IAnnotation {

  TextUnitDTO textUnitDTO;

  public TextUnitDTOAnnotation(TextUnitDTO textUnitDTO) {
    this.textUnitDTO = textUnitDTO;
  }

  public TextUnitDTO getTextUnitDTO() {
    return textUnitDTO;
  }

  public void setTextUnitDTO(TextUnitDTO textUnitDTO) {
    this.textUnitDTO = textUnitDTO;
  }
}
