package com.box.l10n.mojito.smartling.response;

import java.util.List;

public class StringInfo {
    /**
     * UUID in Smartling
     */
    String hashcode;

    List<Key> keys;

    /**
     * This is processed and often can't be match to the source string
     */
    String parsedStringText;
    /**
     * Seems empty
     */
    String stringText;

    /**
     * When used with Mojito naming convention, it corresponds to: {assetPath}#@#{textUnitName}
     *
     * This is not enought to uniquely identify a Mojito text unit. there could multiple text unit for the
     * same text unit name (different content/comment).
     */
    String stringVariant;

    Integer maxLength;

    public String getHashcode() {
        return hashcode;
    }

    public void setHashcode(String hashcode) {
        this.hashcode = hashcode;
    }

    public List<Key> getKeys() {
        return keys;
    }

    public void setKeys(List<Key> keys) {
        this.keys = keys;
    }

    public String getParsedStringText() {
        return parsedStringText;
    }

    public void setParsedStringText(String parsedStringText) {
        this.parsedStringText = parsedStringText;
    }

    public String getStringText() {
        return stringText;
    }

    public void setStringText(String stringText) {
        this.stringText = stringText;
    }

    public String getStringVariant() {
        return stringVariant;
    }

    public void setStringVariant(String stringVariant) {
        this.stringVariant = stringVariant;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

}
