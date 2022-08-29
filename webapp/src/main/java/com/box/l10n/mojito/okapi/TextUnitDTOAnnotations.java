package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import net.sf.okapi.common.resource.ITextUnit;
import org.springframework.stereotype.Component;

/**
 * Utilities to access {@link TextUnitDTOStepAnnotation}
 *
 * @author jaurambault
 */
@Component
public class TextUnitDTOAnnotations {

  /**
   * Gets the {@link TextUnitDTO} linked to a {@link ITextUnit}.
   *
   * @param textUnit the text unit to get the {@link TextUnitDTO} from
   * @return the {@link TextUnitDTO} or {@code null} if not available
   */
  public TextUnitDTO getTextUnitDTO(ITextUnit textUnit) {

    TextUnitDTO textUnitDTO = null;

    TextUnitDTOAnnotation annotation = textUnit.getAnnotation(TextUnitDTOAnnotation.class);

    if (annotation != null) {
      textUnitDTO = annotation.getTextUnitDTO();
    }

    return textUnitDTO;
  }
}
