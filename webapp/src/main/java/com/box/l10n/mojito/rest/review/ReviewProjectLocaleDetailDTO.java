package com.box.l10n.mojito.rest.review;

import java.util.List;

public class ReviewProjectLocaleDetailDTO extends ReviewProjectLocaleSummaryDTO {

  private List<ReviewProjectTextUnitDTO> textUnits;

  public ReviewProjectLocaleDetailDTO() {}

  public ReviewProjectLocaleDetailDTO(
      Long id,
      String bcp47Tag,
      String displayName,
      int selectedCount,
      long acceptedCount,
      List<ReviewProjectTextUnitDTO> textUnits) {
    super(id, bcp47Tag, displayName, selectedCount, acceptedCount);
    this.textUnits = textUnits;
  }

  public List<ReviewProjectTextUnitDTO> getTextUnits() {
    return textUnits;
  }

  public void setTextUnits(List<ReviewProjectTextUnitDTO> textUnits) {
    this.textUnits = textUnits;
  }
}
