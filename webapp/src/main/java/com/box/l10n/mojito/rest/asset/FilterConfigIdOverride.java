package com.box.l10n.mojito.rest.asset;

import static com.box.l10n.mojito.service.assetExtraction.extractor.AssetPathToFilterConfigMapper.MACSTRINGSDICT_FILTER_KEY_CONFIG_ID;

/**
 * By default Okapi filter configuration is guessed from the file extension.
 * Use this to define a specific filter configuration to use.
 */
public enum FilterConfigIdOverride {
    PROPERTIES_JAVA("okf_properties"),
    MACSTRINGSDICT_FILTER_KEY(MACSTRINGSDICT_FILTER_KEY_CONFIG_ID);
    String okapiFilterId;

    private FilterConfigIdOverride(String okapiFilterId) {
        this.okapiFilterId = okapiFilterId;
    }

    public String getOkapiFilterId() {
        return okapiFilterId;
    }
    
}
