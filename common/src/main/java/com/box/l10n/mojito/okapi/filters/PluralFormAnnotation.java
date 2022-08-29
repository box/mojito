package com.box.l10n.mojito.okapi.filters;

import net.sf.okapi.common.annotation.IAnnotation;

/** @author jaurambault */
public class PluralFormAnnotation implements IAnnotation {

  String name;
  String otherName;

  public PluralFormAnnotation(String name, String otherName) {
    this.name = name;
    this.otherName = otherName;
  }

  public String getName() {
    return name;
  }

  public String getOtherName() {
    return otherName;
  }
}
