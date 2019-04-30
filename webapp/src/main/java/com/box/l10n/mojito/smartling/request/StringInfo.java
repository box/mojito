package com.box.l10n.mojito.smartling.request;

import java.util.List;

public class StringInfo {
    String hashcode;
    List<Key> keys;
    String parsedStringText;
    String stringText;
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
