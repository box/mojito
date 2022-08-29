package com.box.l10n.mojito.service.machinetranslation;

/**
 * Defines a list of custom domain trained machine translation engines that can be used with the
 * corresponding MT APIs.
 *
 * @author garion
 */
public enum CustomModelType {
  NONE("");

  private final String value;

  CustomModelType(String customModel) {
    value = customModel;
  }

  public String getValue() {
    return value;
  }
}
