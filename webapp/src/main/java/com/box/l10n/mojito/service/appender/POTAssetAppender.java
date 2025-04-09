package com.box.l10n.mojito.service.appender;

import com.box.l10n.mojito.service.converter.TextUnitConverter;
import com.box.l10n.mojito.service.converter.TextUnitConverterGettext;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.utils.AssetContentUtils;
import java.util.function.Predicate;

/**
 * Append text units to a POT source asset using the gettext text unit converter.
 *
 * @author mattwilshire
 */
public class POTAssetAppender extends AbstractAssetAppender {
  private final TextUnitConverterGettext converter;

  public POTAssetAppender(String content) {
    String lineBreak = AssetContentUtils.determineLineSeparator(content);
    converter = new TextUnitConverterGettext(lineBreak);
    append(content);
    append(lineBreak + lineBreak);
  }

  @Override
  protected TextUnitConverter getConverter() {
    return converter;
  }

  @Override
  protected Predicate<TextUnitDTO> getTextUnitFilter() {
    // This is required - text units will be passed in for _one, _few, _other, etc.
    // These text units shouldn't create a separate string that is appended onto the source asset
    // lets filter out all plurals except 'other' - this will create the appropriate string
    // with the singular and plural defined.
    return tu -> tu.getPluralForm() == null || tu.getPluralForm().equals("other");
  }
}
