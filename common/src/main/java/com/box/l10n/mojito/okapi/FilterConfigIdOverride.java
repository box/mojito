package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.okapi.filters.MacStringsdictFilterKey;
import com.box.l10n.mojito.okapi.filters.XcodeXliffFilter;

/**
 * By default Okapi filter configuration is guessed from the file extension. Use
 * this to define a specific filter configuration to use.
 */
public enum FilterConfigIdOverride {

    PROPERTIES_JAVA("okf_properties"),
    MACSTRINGSDICT_FILTER_KEY(MacStringsdictFilterKey.FILTER_CONFIG_ID),
    XCODE_XLIFF(XcodeXliffFilter.FILTER_CONFIG_ID);

    String okapiFilterId;

    FilterConfigIdOverride(String okapiFilterId) {
        this.okapiFilterId = okapiFilterId;
    }

    public String getOkapiFilterId() {
        return okapiFilterId;
    }

}
