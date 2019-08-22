package com.box.l10n.mojito.smartling;

import org.springframework.stereotype.Component;

@Component
public class AssetPathAndTextUnitNameKeys {

    static final String DELIMITER = "#@#";

    public String toKey(String assetPath, String textUnitName) {
        return assetPath + DELIMITER + textUnitName;
    }

    public Key parse(String string) {
        String[] split = string.split(DELIMITER, 2);
        if (split.length != 2) {
            throw new IllegalArgumentException("must contain delimter #@#");
        }

        return new Key(split[0], split[1]);
    }

    public class Key {
        String assetPath;
        String textUnitName;

        public Key(String assetPath, String textUnitName) {
            this.assetPath = assetPath;
            this.textUnitName = textUnitName;
        }

        public String getAssetPath() {
            return assetPath;
        }

        public String getTextUnitName() {
            return textUnitName;
        }
    }

}
