package com.box.l10n.mojito.service.appender;

import com.box.l10n.mojito.service.converter.TextUnitConverter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.List;
import java.util.function.Predicate;

public abstract class AbstractAssetAppender {
  private final StringBuilder assetContent = new StringBuilder();

  /**
   * Appends new content to the asset.
   *
   * @param content The content to append.
   */
  protected void append(String content) {
    if (content != null && !content.isEmpty()) {
      assetContent.append(content);
    }
  }

  /**
   * Each subclass must supply its specific TextUnitConverter.
   *
   * @return the TextUnitConverter instance.
   */
  protected abstract TextUnitConverter getConverter();

  /**
   * Return a predicate to filter out text units in the appendTextUnits method.
   *
   * @return the filter.
   */
  protected Predicate<TextUnitDTO> getTextUnitFilter() {
    return null;
  }

  /**
   * Appends each TextUnitDTO using the provided converter.
   *
   * @param textUnitDTOList A list of text units.
   */
  public void appendTextUnits(List<TextUnitDTO> textUnitDTOList) {
    if (textUnitDTOList == null || textUnitDTOList.isEmpty()) return;
    textUnitDTOList.stream()
        .filter(tu -> getTextUnitFilter() == null || getTextUnitFilter().test(tu))
        .forEach(
            textUnitDTO -> {
              append(getConverter().convert(textUnitDTO));
            });
  }

  /**
   * Returns the full asset content with appended text units as a String.
   *
   * @return the asset content with text units appended.
   */
  public String getAssetContent() {
    return assetContent.toString();
  }
}
