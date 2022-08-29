package com.box.l10n.mojito.android.strings;

public enum AndroidPluralQuantity {
  ZERO,
  ONE,
  TWO,
  FEW,
  MANY,
  OTHER;

  @Override
  public String toString() {
    return name().toLowerCase();
  }
}
