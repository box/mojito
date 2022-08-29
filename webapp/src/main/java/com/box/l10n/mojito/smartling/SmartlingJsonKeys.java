package com.box.l10n.mojito.smartling;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.springframework.stereotype.Component;

@Component
public class SmartlingJsonKeys {

  static final String DELIMITER = "#@#";

  public String toKey(Long tmTextUnitId, String textUnitName, String assetPath) {
    return tmTextUnitId + DELIMITER + assetPath + DELIMITER + textUnitName;
  }

  public Key parse(String string) {
    Preconditions.checkNotNull(string);
    String[] split = string.split(DELIMITER, 3);
    if (split.length != 3) {
      throw new IllegalArgumentException("must contain 2 delimters: #@#");
    }

    return new Key(Long.valueOf(split[0]), split[1], split[2]);
  }

  public static class Key {
    Long tmTextUnitd;
    String assetPath;
    String textUnitName;

    public Key(Long tmTextUnitid, String assetPath, String textUnitName) {
      this.tmTextUnitd = tmTextUnitid;
      this.assetPath = assetPath;
      this.textUnitName = textUnitName;
    }

    public Long getTmTextUnitd() {
      return tmTextUnitd;
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
      return Objects.equal(tmTextUnitd, key.tmTextUnitd)
          && Objects.equal(assetPath, key.assetPath)
          && Objects.equal(textUnitName, key.textUnitName);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(tmTextUnitd, assetPath, textUnitName);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("tmTextUnitd", tmTextUnitd)
          .add("assetPath", assetPath)
          .add("textUnitName", textUnitName)
          .toString();
    }
  }
}
