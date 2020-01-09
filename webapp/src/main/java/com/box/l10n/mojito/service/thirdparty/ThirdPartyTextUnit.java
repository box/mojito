package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.service.tm.TextUnitForBatchMatcher;

public class ThirdPartyTextUnit implements TextUnitForBatchMatcher {

    /**
     * Id in the third party TMS
     */
    String id;

    /**
     * The asset path in Mojito (should be extracted from information from the third party TMS)
     */
    String assetPath;

    /**
     * The text unit name in Mojito (should be extracted from information from the third party TMS)
     */
    String name;

    /**
     * The source in Mojito (should be extracted from information from the third party TMS if possible)
     */
    String content;

    /**
     * If the name is a plural prefix (instead of the full text unit name) and so the entry map to a plural string in Mojito
     */
    boolean namePluralPrefix;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAssetPath() {
        return assetPath;
    }

    public void setAssetPath(String assetName) {
        this.assetPath = assetName;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public Long getTmTextUnitId() {
        return null;
    }

    @Override
    public boolean isNamePluralPrefix() {
        return namePluralPrefix;
    }

    public void setNamePluralPrefix(boolean namePluralPrefix) {
        this.namePluralPrefix = namePluralPrefix;
    }
}
