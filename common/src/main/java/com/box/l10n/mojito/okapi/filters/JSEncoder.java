package com.box.l10n.mojito.okapi.filters;

import net.sf.okapi.common.encoder.EncoderContext;

/** @author jyi */
public class JSEncoder extends SimpleEncoder {

  @Override
  public String encode(char value, EncoderContext context) {
    String res;

    switch (value) {
      case '\n':
        res = "\\n";
        break;
      case '\r':
        res = "\\r";
        break;
      case '"':
        res = "\\\"";
        break;
      case '`':
        res = "\\`";
        break;
      default:
        res = String.valueOf(value);
        break;
    }

    return res;
  }
}
