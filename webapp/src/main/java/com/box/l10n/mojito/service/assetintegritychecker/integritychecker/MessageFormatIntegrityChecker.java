package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.MessagePattern;
import com.ibm.icu.text.MessagePattern.Part;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks the validity of the message format in the target content.
 *
 * <p>Checks that the target can be compiled into a message format. Then it compares the number of
 * format/placeholders in the target and the source content. If the number is different the an
 * exception is thrown. To do this the source is also compile into a message format. If the source
 * is not valid an error is thrown though not directly related to the target (still the string needs
 * review).
 *
 * @author wyau
 */
public class MessageFormatIntegrityChecker extends AbstractTextUnitIntegrityChecker {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(MessageFormatIntegrityChecker.class);

  private static final String REPLACEMENT_CHARS = "\u0013";

  @Override
  public void check(String sourceContent, String targetContent)
      throws MessageFormatIntegrityCheckerException {

    MessageFormat targetMessageFormat = null;
    MessageFormat sourceMessageFormat = null;

    logger.debug("Check if the target pattern is valid");
    try {
      targetMessageFormat = new MessageFormat(targetContent);
    } catch (IllegalArgumentException iae) {
      throw new MessageFormatIntegrityCheckerException(
          String.format("Invalid pattern - %s", iae.getMessage()), iae);
    }

    logger.debug(
        "Check if the source pattern is valid to compare the number of format/placeholder");
    try {
      sourceMessageFormat = new MessageFormat(sourceContent);
    } catch (IllegalArgumentException iae) {
      throw new MessageFormatIntegrityCheckerException(
          String.format("Invalid source pattern - %s", iae.getMessage()), iae);
    }

    logger.debug(
        "Check number of format/placeholder in the source and target message formats is the same");
    int numberSourceFormats = sourceMessageFormat.getFormats().length;
    int numberTargetFormats = targetMessageFormat.getFormats().length;

    if (numberSourceFormats != numberTargetFormats) {
      throw new MessageFormatIntegrityCheckerException(
          "Number of top level placeholders in source ("
              + numberSourceFormats
              + ") and target ("
              + numberTargetFormats
              + ") is different");
    }

    Set<String> sourceArgumentNames = sourceMessageFormat.getArgumentNames();
    Set<String> targetArgumentNames = targetMessageFormat.getArgumentNames();

    if (!sourceArgumentNames.equals(targetArgumentNames)) {
      throw new MessageFormatIntegrityCheckerException(
          "Different placeholder name in source and target");
    }
  }

  /**
   * All non-localizable parts from the given string are extracted and replaced with identifiers.
   * {@link LocalizableString#nonLocalizableParts} is updated to have the map of identifiers and the
   * actual non-localizable parts of the string.
   *
   * <p>For example, if the input string is "Hello {username}.", the placeholder is extracted and
   * replaced with !!1!!. {@link LocalizableString#nonLocalizableParts} is updated with "!!1!!" =>
   * "username" and {@link LocalizableString#LocalizableString} is updated to "Hello {!!1!!}."
   *
   * @param string
   * @return {@link LocalizableString}
   */
  @Override
  public LocalizableString extractNonLocalizableParts(String string) {
    LocalizableString localizableString = new LocalizableString(string);
    StringBuilder localizable = new StringBuilder();
    StringBuilder notLocalizable = new StringBuilder();
    MessagePattern messagePattern = null;
    try {
      messagePattern = new MessagePattern(string);
    } catch (IllegalArgumentException iae) {
      throw new MessageFormatIntegrityCheckerException("Invalid string pattern", iae);
    }

    logger.debug("Extracting non localizable parts from: {}", string);
    int index = 0;
    boolean isLocalizable = true;
    for (int i = 0; i < messagePattern.countParts(); i++) {
      Part part = messagePattern.getPart(i);
      logger.debug("Type={}", part.getType().toString());

      if (part.getType().equals(Part.Type.MSG_START)
          || part.getType().equals(Part.Type.ARG_LIMIT)) {

        if (notLocalizable.length() > 0) {
          String replacement =
              REPLACEMENT_CHARS
                  + (localizableString.getNonLocalizableParts().size() + 1)
                  + REPLACEMENT_CHARS;
          localizableString.getNonLocalizableParts().put(replacement, notLocalizable.toString());
          localizable.append(replacement);
          notLocalizable.setLength(0);
        }

        isLocalizable = true;

      } else if (part.getType().equals(Part.Type.MSG_LIMIT)
          || part.getType().equals(Part.Type.ARG_START)) {

        localizable.append(string.substring(index, part.getLimit()));
        index = part.getLimit();

        isLocalizable = false;
        continue;
      }

      if (isLocalizable) {
        localizable.append(string.substring(index, part.getLimit()));
      } else {
        notLocalizable.append(string.substring(index, part.getLimit()));
      }
      index = part.getLimit();
    }

    localizableString.setLocalizableString(localizable.toString());
    return localizableString;
  }
}
