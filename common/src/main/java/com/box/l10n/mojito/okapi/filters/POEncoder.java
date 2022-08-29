package com.box.l10n.mojito.okapi.filters;

import net.sf.okapi.common.resource.Property;

public class POEncoder extends SimpleEncoder {

  @Override
  public String toNative(String propertyName, String value) {
    if (Property.APPROVED.equals(propertyName)) {
      if ((value != null) && (value.equals("no"))) {
        return "fuzzy";
      } else { // Don't set the fuzzy flag
        return "";
      }
    }

    // No changes for the other values
    return value;
  }
}
