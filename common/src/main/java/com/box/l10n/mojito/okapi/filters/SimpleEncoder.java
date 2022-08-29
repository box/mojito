package com.box.l10n.mojito.okapi.filters;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.stream.Collectors;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.encoder.EncoderContext;
import net.sf.okapi.common.encoder.IEncoder;

/**
 * Encoder to handle escaping \n, \r, double-quotes.
 *
 * @author jyi
 */
public class SimpleEncoder implements IEncoder {

  private CharsetEncoder chsEnc;
  private String lineBreak;
  private String encoding;
  private IParameters params;

  @Override
  public void setOptions(IParameters params, String encoding, String lineBreak) {
    chsEnc = Charset.forName(encoding).newEncoder();
    this.lineBreak = lineBreak;
    this.encoding = encoding;
    this.params = params;
  }

  @Override
  public String encode(String text, EncoderContext context) {
    return text.codePoints()
        .mapToObj(codePoint -> encode(codePoint, context))
        .collect(Collectors.joining());
  }

  @Override
  public String encode(int value, EncoderContext context) {
    String encoded;
    if (Character.isSupplementaryCodePoint(value)) {
      encoded = new String(Character.toChars(value));
    } else {
      encoded = encode((char) value, context);
    }
    return encoded;
  }

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
      default:
        res = String.valueOf(value);
        break;
    }

    return res;
  }

  @Override
  public void reset() {}

  @Override
  public String toNative(String propertyName, String value) {
    // No changes for the other values
    return value;
  }

  @Override
  public String getLineBreak() {
    return lineBreak;
  }

  @Override
  public String getEncoding() {
    return encoding;
  }

  @Override
  public CharsetEncoder getCharsetEncoder() {
    return chsEnc;
  }

  @Override
  public IParameters getParameters() {
    return params;
  }
}
