package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;

public class AssetExtractorTextUnitWithAssetPath extends AssetExtractorTextUnit {
    String assetPath;

    public String getAssetPath() {
        return assetPath;
    }

    public void setAssetPath(String assetPath) {
        this.assetPath = assetPath;
    }
}
