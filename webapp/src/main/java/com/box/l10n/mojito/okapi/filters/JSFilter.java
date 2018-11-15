package com.box.l10n.mojito.okapi.filters;

import static com.box.l10n.mojito.okapi.filters.MacStringsFilter.FILTER_CONFIG_ID;
import java.util.ArrayList;
import java.util.List;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.filters.regex.RegexFilter;

/**
 * @author jyi
 */
public class JSFilter extends RegexFilter {

    public static final String FILTER_CONFIG_ID = "okf_regex@mojito";

    @Override
    public String getName() {
        return FILTER_CONFIG_ID;
    }

    @Override
    public List<FilterConfiguration> getConfigurations() {
        List<FilterConfiguration> list = new ArrayList<FilterConfiguration>();
        list.add(new FilterConfiguration(getName() + "-js",
                getMimeType(),
                getClass().getName(),
                "Text (JS Strings)",
                "Configuration for JS .js/.ts files.",
                "js_mojito.fprm"));
        return list;
    }

}
