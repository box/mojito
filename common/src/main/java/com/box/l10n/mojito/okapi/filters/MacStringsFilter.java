package com.box.l10n.mojito.okapi.filters;

import com.box.l10n.mojito.okapi.ExtractUsagesFromTextUnitComments;
import java.util.ArrayList;
import java.util.List;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.filters.regex.RegexFilter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Overrides {@link RegexFilter} to handle escape/unescape special characters
 *
 * @author jyi
 */
public class MacStringsFilter extends RegexEscapeDoubleQuoteFilter {

  public static final String FILTER_CONFIG_ID = "okf_regex@mojito";

  @Autowired ExtractUsagesFromTextUnitComments extractUsagesFromTextUnitComments;

  @Override
  public String getName() {
    return FILTER_CONFIG_ID;
  }

  @Override
  public List<FilterConfiguration> getConfigurations() {
    List<FilterConfiguration> list = new ArrayList<>();
    list.add(
        new FilterConfiguration(
            getName() + "-macStrings",
            getMimeType(),
            getClass().getName(),
            "Text (Mac Strings)",
            "Configuration for Macintosh .strings files.",
            "macStrings_mojito.fprm"));
    return list;
  }

  @Override
  public Event next() {
    Event event = super.next();

    if (event.getEventType() == EventType.TEXT_UNIT) {
      TextUnit textUnit = (TextUnit) event.getTextUnit();
      extractUsagesFromTextUnitComments.addUsagesToTextUnit(textUnit);
    }

    return event;
  }
}
