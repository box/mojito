package com.box.l10n.mojito.pseudoloc;

import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.LocalizableString;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.MessageFormatIntegrityChecker;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TextUnitIntegrityChecker;
import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author srizvi
 */
@Component
public class PseudoLocalization {

  /** Logger */
  static Logger logger = LoggerFactory.getLogger(PseudoLocalization.class);

  private static final int EXPANSION_FACTOR = 30;
  private static final String BEGINNING_CHAR = "萬";
  private static final String ENDING_CHAR = "國";

  private static Map<Character, String> pseudoLocMap = new HashMap<>();

  static {
    pseudoLocMap.put('a', "àáâãäåāăąǻάαа");
    pseudoLocMap.put('A', "ÀÁÂÃÄÅĀĂĄǺΆΑА");
    pseudoLocMap.put('b', "вьъ");
    pseudoLocMap.put('B', "ΒßβБВ");
    pseudoLocMap.put('c', "¢çćĉċčсς");
    pseudoLocMap.put('C', "ÇĆĈĊČС");
    pseudoLocMap.put('d', "ďđ");
    pseudoLocMap.put('D', "ÐĎĐ");
    pseudoLocMap.put('e', "èéêëēĕėęěέεеёє℮");
    pseudoLocMap.put('E', "ÈÉÊËĒĔĖĘĚΈΕΣЕЁЄЄ");
    pseudoLocMap.put('f', "ƒ");
    pseudoLocMap.put('F', "₣");
    pseudoLocMap.put('g', "ĝğġģ");
    pseudoLocMap.put('G', "ĜĞĠĢ");
    pseudoLocMap.put('h', "ĥħнћ");
    pseudoLocMap.put('H', "ĤĦΉΗН");
    pseudoLocMap.put('i', "ìíîïĩīĭįίιϊіїΐ");
    pseudoLocMap.put('I', "ÌÍÎĨĪĬĮİΊΪІЇ");
    pseudoLocMap.put('j', "ĵј");
    pseudoLocMap.put('J', "ĴЈ");
    pseudoLocMap.put('k', "ķĸκкќ");
    pseudoLocMap.put('K', "ĶΚЌК");
    pseudoLocMap.put('l', "ĺļľł");
    pseudoLocMap.put('L', "ĹĻĽĿŁ");
    pseudoLocMap.put('m', "mм");
    pseudoLocMap.put('M', "ΜМм");
    pseudoLocMap.put('n', "ийлпπήηńņňŉŋñ");
    pseudoLocMap.put('N', "ÑŃŅŇŊΝИЙП");
    pseudoLocMap.put('o', "òóôõöøōŏőοσόоǿ");
    pseudoLocMap.put('O', "ÒÓÔÕÖØŌŎŐǾΌΘΟО");
    pseudoLocMap.put('p', "ρр");
    pseudoLocMap.put('p', "ΡР");
    pseudoLocMap.put('r', "ŕŗřяѓґгř");
    pseudoLocMap.put('R', "ŔŖŘЯΓЃҐГ");
    pseudoLocMap.put('s', "śŝşѕš");
    pseudoLocMap.put('S', "ŚŜŞЅŠ");
    pseudoLocMap.put('t', "ţťŧτт");
    pseudoLocMap.put('T', "ŢŤŦΤТ");
    pseudoLocMap.put('u', "µùúûüũūŭůűųцμџ");
    pseudoLocMap.put('U', "ÙÚÛÜŨŪŬŮŰŲЏЦ");
    pseudoLocMap.put('v', "ν");
    pseudoLocMap.put('w', "ŵωώшщẁẃẅ");
    pseudoLocMap.put('W', "ŴШЩẀẂẄ");
    pseudoLocMap.put('x', "×хж");
    pseudoLocMap.put('X', "ΧχХЖ");
    pseudoLocMap.put('y', "ýÿŷγУўỳу");
    pseudoLocMap.put('Y', "ÝŶΎΥΫỲЎ");
    pseudoLocMap.put('z', "źżž");
    pseudoLocMap.put('Z', "ŹŻΖŽ");
  }

  /**
   * Pseudo localize a string depending on the associated integrity checker, expansion factor and
   * bracket option.
   *
   * @param string string to be pseudo localized
   * @param checkers integrity checkers for placeholder check
   * @return pseudo localized string
   */
  public String convertStringToPseudoLoc(String string, Set<TextUnitIntegrityChecker> checkers) {
    TextUnitIntegrityChecker checker = getIntegrityCheckerForPlaceholderProcessing(checkers);

    if (checker == null) {
      logger.debug("There is no checker for pseudolocalization placeholder processing.");
      return convertStringToPseudoLoc(string);
    } else {
      logger.debug("Found checker for pseudolocalization placeholder processing.");
      LocalizableString localizableString = checker.extractNonLocalizableParts(string);
      String pseudolocalized = convertStringToPseudoLoc(localizableString.getLocalizableString());
      localizableString.setLocalizableString(pseudolocalized);
      return checker.restoreNonLocalizableParts(localizableString);
    }
  }

  /**
   * Pseudo localize a string depending expansion factor and bracket option.
   *
   * @param string string to be pseudo localized
   * @return pseudo localized string
   */
  public String convertStringToPseudoLoc(String string) {
    StringBuilder sb = new StringBuilder();

    if (!Strings.isNullOrEmpty(string)) {
      String str = convertAsciiToDiacritics(string);
      sb.append(expand(str));
      sb.insert(0, '⟦');
      sb.append('⟧');
    }

    return sb.toString();
  }

  /**
   * Expands the given string by the expansion factor with some unicode characters.
   *
   * @param string
   * @return
   */
  public String expand(String string) {
    int expandedStringLength = (int) Math.ceil(string.length() * EXPANSION_FACTOR / 100.0);

    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < expandedStringLength; i++) {
      if (i == (expandedStringLength - 1)) {
        sb.append(" ");
      } else {
        sb.append(BEGINNING_CHAR);
      }
    }

    sb.append(string);

    for (int i = 0; i < expandedStringLength; i++) {
      if (i == 0) {
        sb.append(" ");
      } else {
        sb.append(ENDING_CHAR);
      }
    }

    return sb.toString();
  }

  /**
   * Converts ASCII letter into equivalent characters with accent/diacritics
   *
   * @param string String to be converted
   * @return
   */
  public String convertAsciiToDiacritics(String string) {
    int stringLength = string.length();

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < stringLength; i++) {
      char character = string.charAt(i);
      sb.append(getMappingCharFromMap(character));
    }

    return sb.toString();
  }

  /**
   * Get a non ASCII character mapping to provided character or the character itself if there is no
   * mapping
   *
   * @param character ASCII character to be mapped
   * @return Non ASCII character or character itself
   */
  private char getMappingCharFromMap(char character) {
    char mappedChar = character;

    String mappingCharsForChar = pseudoLocMap.get(mappedChar);

    if (mappingCharsForChar != null) {
      int maxIndex = mappingCharsForChar.length() - 1;
      int randomIndex = (int) (Math.random() * maxIndex);
      mappedChar = mappingCharsForChar.charAt(randomIndex);
    }

    return mappedChar;
  }

  /**
   * Returns {@link TextUnitIntegrityChecker} that contains patterns for placeholders. Only {@link
   * MessageFormatIntegrityChecker} is supported at this time.
   *
   * @param checkers
   * @return
   */
  private TextUnitIntegrityChecker getIntegrityCheckerForPlaceholderProcessing(
      Set<TextUnitIntegrityChecker> checkers) {
    for (TextUnitIntegrityChecker textUnitIntegrityChecker : checkers) {
      if (textUnitIntegrityChecker instanceof MessageFormatIntegrityChecker) {
        logger.debug(
            "Use the MessageFormatIntegrityChecker to process placeholder in pseudolocalization.");
        return textUnitIntegrityChecker;
      }
    }
    return null;
  }
}
