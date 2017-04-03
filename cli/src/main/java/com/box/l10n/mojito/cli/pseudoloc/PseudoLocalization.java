package com.box.l10n.mojito.cli.pseudoloc;

import java.util.HashMap;
import java.util.Map;

/**
 * @author srizvi
 */
public class PseudoLocalization {

	private static Map<String, String> pseudoLocMap = new HashMap<String, String>();
	static {
		pseudoLocMap.put("a", "àáâãäåāăąǻάαа");
		pseudoLocMap.put("A", "ÀÁÂÃÄÅĀĂĄǺΆΑА");
		pseudoLocMap.put("b", "вьъ");
		pseudoLocMap.put("B", "ΒßβБВ");
		pseudoLocMap.put("c", "¢çćĉċčсς");
		pseudoLocMap.put("C", "ÇĆĈĊČС");
		pseudoLocMap.put("d", "ďđ");
		pseudoLocMap.put("D", "ÐĎĐ");
		pseudoLocMap.put("e", "èéêëēĕėęěέεеёє℮");
		pseudoLocMap.put("E", "ÈÉÊËĒĔĖĘĚΈΕΣЕЁЄЄ");
		pseudoLocMap.put("f", "ƒ");
		pseudoLocMap.put("F", "₣");
		pseudoLocMap.put("g", "ĝğġģ");
		pseudoLocMap.put("G", "ĜĞĠĢ");
		pseudoLocMap.put("h", "ĥħнћ");
		pseudoLocMap.put("H", "ĤĦΉΗН");
		pseudoLocMap.put("i", "ìíîïĩīĭįίιϊіїΐ");
		pseudoLocMap.put("I", "ÌÍÎĨĪĬĮİΊΪІЇ");
		pseudoLocMap.put("j", "ĵј");
		pseudoLocMap.put("J", "ĴЈ");
		pseudoLocMap.put("k", "ķĸκкќ");
		pseudoLocMap.put("K", "ĶΚЌК");
		pseudoLocMap.put("l", "ĺļľł");
		pseudoLocMap.put("L", "ĹĻĽĿŁ");
		pseudoLocMap.put("m", "mм");
		pseudoLocMap.put("M", "ΜМм");
		pseudoLocMap.put("n", "ийлпπήηńņňŉŋñ");
		pseudoLocMap.put("N", "ÑŃŅŇŊΝИЙП");
		pseudoLocMap.put("o", "òóôõöøōŏőοσόоǿ");
		pseudoLocMap.put("O", "ÒÓÔÕÖØŌŎŐǾΌΘΟО");
		pseudoLocMap.put("p", "ρр");
		pseudoLocMap.put("p", "ΡР");
		pseudoLocMap.put("r", "ŕŗřяѓґгř");
		pseudoLocMap.put("R", "ŔŖŘЯΓЃҐГ");
		pseudoLocMap.put("s", "śŝşѕš");
		pseudoLocMap.put("S", "ŚŜŞЅŠ");
		pseudoLocMap.put("t", "ţťŧτт");
		pseudoLocMap.put("T", "ŢŤŦΤТ");
		pseudoLocMap.put("u", "µùúûüũūŭůűųцμџ");
		pseudoLocMap.put("U", "ÙÚÛÜŨŪŬŮŰŲЏЦ");
		pseudoLocMap.put("v", "ν");
		pseudoLocMap.put("w", "ŵωώшщẁẃẅ");
		pseudoLocMap.put("W", "ŴШЩẀẂẄ");
		pseudoLocMap.put("x", "×хж");
		pseudoLocMap.put("X", "ΧχХЖ");
		pseudoLocMap.put("y", "ýÿŷγУўỳу");
		pseudoLocMap.put("Y", "ÝŶΎΥΫỲЎ");
		pseudoLocMap.put("z", "źżž");
		pseudoLocMap.put("Z", "ŹŻΖŽ");
	}

	/**
	 * Pseudo localize a string depending on instance settings to choose the type, expansion factor and bracket option.
	 * If pseudo-localization is disabled, returns the passed in string
	 *
	 * @param string string to be pseudo localized
	 * @return pseudo localized string
	 */
	public static String convertStringToPseudoLoc(String string)
	{
		String expanded = "";
		if (string != null && !string.isEmpty()) {
			expanded = convertAsciiToDiacritics(string);
			expanded = expand(expanded);
			expanded = '⟦' + expanded + '⟧';
		}

		return expanded;
	}

	/**
	 * Expands the given string by the expansion factor with some unicode characters.
	 *
	 * @param string
	 * @return
	 */
	public static String expand(String string) {
		String expandedString = "";
		int expansionFactor = 30;

		int expandedStringLength = string.length() * expansionFactor / 100;
		int affixLength = (int) Math.ceil((expandedStringLength) / 2);

		for (int i = 0; i < affixLength; i++) {
			if (i == (affixLength - 1)) {
				expandedString +=  " ";
			} else {
				expandedString +=  "萬";
			}
		}

		expandedString += string;

		for (int i = 0; i < affixLength; i++) {
			if (i == 0) {
				expandedString +=  " ";
			} else {
				expandedString +=  "國";
			}
		}

		return expandedString;
	}

	/**
	 * Converts ASCII letter into equivalent characters with accent/diacritics
	 *
	 * @param string String to be converted
	 * @return
	 */
	public static String convertAsciiToDiacritics(String string) {
		String convertedString = "";
		int stringLength = string.length();

		for (int i = 0; i < stringLength; i++) {
			String character = substr(string, i, 1);
			convertedString += getMappingCharFromMap(character);
		}

		return convertedString;
	}

	/**
	 * Get a non ASCII character mapping to provided character or the character itself if there is no mapping
	 *
	 * @param character ASCII character to be mapped
	 * @return Non ASCII character or character itself
	 */
	private static String getMappingCharFromMap(String character) {
		String mappedChar = character;

		String mappingCharsForChar = pseudoLocMap.get(mappedChar);

		if (mappingCharsForChar != null) {

			int matchesLength = mappingCharsForChar.length();
			int min = 0 + (int) (Math.random() * (matchesLength - 1));
			mappedChar = substr(mappingCharsForChar, min, 1);
		}

		return mappedChar;
	}

	/**
	 * This is the equivalent to PHP substr method.
	 * @param string
	 * @param from
	 * @param to
	 * @return
	 */
	private static  String substr(String string, int from, int to) {
		if (from < 0 && to < 0) {
			if (Math.abs(from) > Math.abs(to)) {
				String s = string.substring(string.length() - Math.abs(from));
				return s.substring(s.length() - Math.abs(to));
			} else {
				return "";
			}
		} else if (from >= 0 && to < 0) {
			String s = string.substring(from);
			if (Math.abs(to) >= s.length()) {
				return "";
			} else {
				return s.substring(0, s.length() - Math.abs(to));
			}
		} else if (from < 0 && to >= 0) {
			String s = string.substring(string.length() - Math.abs(from));
			if (to >= s.length()) {
				return s;
			}
			return s.substring(0, to);
		} else {
			String s = string.substring(Math.abs(from));
			if (to >= s.length()) {
				return s;
			} else {
				return s.substring(0, Math.abs(to));
			}
		}
	}
}
