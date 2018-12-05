package com.box.l10n.mojito.okapi.filters;

import java.util.ArrayList;
import java.util.List;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.filters.regex.RegexFilter;

/**
 * Overrides {@link RegexFilter} to handle escape/unescape special characters
 *
 * @author jyi
 */
public class MacStringsFilter extends RegexEscapeDoubleQuoteFilter {

    public static final String FILTER_CONFIG_ID = "okf_regex@mojito";

    @Override
    public String getName() {
        return FILTER_CONFIG_ID;
    }

    @Override
    public List<FilterConfiguration> getConfigurations() {
        List<FilterConfiguration> list = new ArrayList<FilterConfiguration>();
        list.add(new FilterConfiguration(getName() + "-macStrings",
                getMimeType(),
                getClass().getName(),
                "Text (Mac Strings)",
                "Configuration for Macintosh .strings files.",
                "macStrings_mojito.fprm"));
        return list;
    }

}
