package com.box.l10n.mojito.cli.filefinder.locale;

/**
 * @author jaurambault
 */
public class Bcp47LocaleType extends LocaleType {

  @Override
  public String getTargetLocaleRegex() {
    // TODO(P1) that soudns bad, also would add validation
    return "([xX]([\\x2d]\\p{Alnum}{1,8})*)"
        + "|(((\\p{Alpha}{2,8}(?=\\x2d|)){1}"
        + "(([\\x2d]\\p{Alpha}{3})(?=\\x2d|)){0,3}"
        + "([\\x2d]\\p{Alpha}{4}(?=\\x2d|))?"
        + "([\\x2d](\\p{Alpha}{2}|\\d{3})(?=\\x2d|\\z))?"
        + "([\\x2d](\\d\\p{Alnum}{3}|\\p{Alnum}{5,8})(?=\\x2d|\\z))*)"
        + "(([\\x2d]([a-wyzA-WYZ](?=\\x2d))([\\x2d](\\p{Alnum}{2,8})+)*))*"
        + "([\\x2d][xX]([\\x2d]\\p{Alnum}{1,8})*)?)";
  }
}
