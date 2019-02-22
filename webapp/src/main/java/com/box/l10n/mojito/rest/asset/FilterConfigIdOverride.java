package com.box.l10n.mojito.rest.asset;


import com.box.l10n.mojito.okapi.filters.MacStringsdictFilterKey;

/**
 * By default Okapi filter configuration is guessed from the file extension.
 * Use this to define a specific filter configuration to use.
 */
public enum FilterConfigIdOverride {
    PROPERTIES_JAVA("okf_properties"),
    MACSTRINGSDICT_FILTER_KEY(MacStringsdictFilterKey.FILTER_CONFIG_ID);
    String okapiFilterId;

    private FilterConfigIdOverride(String okapiFilterId) {
        this.okapiFilterId = okapiFilterId;
    }

    public String getOkapiFilterId() {
        return okapiFilterId;
    }
    
}
