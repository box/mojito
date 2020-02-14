package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.okapi.extractor.TextUnit;

public class TextUnitWithAssetPath extends TextUnit {
    String assetPath;

    public String getAssetPath() {
        return assetPath;
    }

    public void setAssetPath(String assetPath) {
        this.assetPath = assetPath;
    }
}
