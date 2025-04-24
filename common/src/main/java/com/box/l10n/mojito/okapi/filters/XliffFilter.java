package com.box.l10n.mojito.okapi.filters;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.xliff.XLIFFFilter;

public class XliffFilter extends XLIFFFilter {
  public static final String FILTER_CONFIG_ID = "okf_xliff@mojito";

  private String usagesKeyRegex;

  @Override
  public String getName() {
    return FILTER_CONFIG_ID;
  }

  @Override
  public void open(RawDocument input) {
    super.open(input);
    FilterOptions filterOptions = input.getAnnotation(FilterOptions.class);
    if (filterOptions != null) {
      filterOptions.getString("usagesKeyRegexp", s -> usagesKeyRegex = s);
    }
  }

  public String getAttributeValue(String xmlContent) {
    String regex = String.format("%s=\\\"([^\\\"]+)\\\"", this.usagesKeyRegex);
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(xmlContent);
    if (matcher.find()) {
      return matcher.group(matcher.groupCount());
    }
    return null;
  }

  @Override
  public Event next() {
    Event event = super.next();
    if (event.isTextUnit() && this.usagesKeyRegex != null) {
      ITextUnit textUnit = event.getTextUnit();
      String usage = this.getAttributeValue(textUnit.getSkeleton().toString());
      if (usage != null) {
        UsagesAnnotation usagesAnnotation = new UsagesAnnotation(Set.of(usage));
        textUnit.setAnnotation(usagesAnnotation);
      }
    }
    return event;
  }
}
