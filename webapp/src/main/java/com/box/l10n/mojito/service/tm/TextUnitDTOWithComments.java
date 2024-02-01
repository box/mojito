package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.BeanUtils;

/**
 * @author aloison
 */
public class TextUnitDTOWithComments extends TextUnitDTO {

  private List<TMTextUnitVariantComment> tmTextUnitVariantComments = new ArrayList<>();

  public TextUnitDTOWithComments(TextUnitDTO textUnitDTO) {
    BeanUtils.copyProperties(textUnitDTO, this);
  }

  public List<TMTextUnitVariantComment> getTmTextUnitVariantComments() {
    return tmTextUnitVariantComments;
  }

  public void setTmTextUnitVariantComments(
      List<TMTextUnitVariantComment> tmTextUnitVariantComments) {
    this.tmTextUnitVariantComments = tmTextUnitVariantComments;
  }
}
