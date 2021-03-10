package com.box.l10n.mojito.smartling;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.springframework.stereotype.Component;

@Component
public class AssetPathAndTextUnitNameKeys {

    static final String DELIMITER = "#@#";

    public String toKey(String assetPath, String textUnitName) {
        return assetPath + DELIMITER + textUnitName;
    }

    public Key parse(String string) {
        Preconditions.checkNotNull(string);
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return Objects.equal(assetPath, key.assetPath) && Objects.equal(textUnitName, key.textUnitName);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(assetPath, textUnitName);
        }

        @Override
        public String toString() {
            return "Key{" +
                    "assetPath='" + assetPath + '\'' +
                    ", textUnitName='" + textUnitName + '\'' +
                    '}';
        }
    }

}
