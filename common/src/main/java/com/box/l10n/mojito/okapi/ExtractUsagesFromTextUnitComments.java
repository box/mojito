package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.okapi.filters.UsagesAnnotation;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.okapi.common.resource.TextUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** @author emagalindan */
@Component
public class ExtractUsagesFromTextUnitComments {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(ExtractUsagesFromTextUnitComments.class);

  @Autowired TextUnitUtils textUnitUtils;

  public static final String USAGES_PATTERN =
      "\\s*?<locations>\n?(?<usages>(.*?\\s)*?)</locations>";
  public static final String USAGES_GROUP_NAME = "usages";

  /**
   * Add usage locations to the text unit
   *
   * @param textUnit the text unit to add usages
   */
  public void addUsagesToTextUnit(TextUnit textUnit) {
    Set<String> usagesFromComment = extractUsagesFromTextUnit(textUnit);
    textUnit.setAnnotation(new UsagesAnnotation(usagesFromComment));
  }

  /**
   * Get and delete the usage locations from the comment in textUnit.
   *
   * @param textUnit to get the usages from
   * @return the locations or empty set
   */
  Set<String> extractUsagesFromTextUnit(TextUnit textUnit) {
    Set<String> locations = null;
    String comment = textUnitUtils.getNote(textUnit);
    if (comment != null) {
      locations = getUsagesFromTextUnitComments(comment);

      if (locations.size() > 0) {
        removeUsagesFromTextUnitComment(textUnit);
      }
    }
    return locations;
  }

  /** @param textUnit used to remove usages from comments */
  void removeUsagesFromTextUnitComment(TextUnit textUnit) {
    String comment = textUnitUtils.getNote(textUnit);
    Pattern pattern = Pattern.compile(USAGES_PATTERN);
    Matcher matcher = pattern.matcher(textUnitUtils.getNote(textUnit));

    if (matcher.find()) {
      textUnitUtils.setNote(textUnit, comment.replace(matcher.group(0), ""));
    }
  }

  /**
   * Gets the usage locations from the comment in textUnit.
   *
   * @param comment to get the usages from
   * @return the locations or empty set
   */
  Set<String> getUsagesFromTextUnitComments(String comment) {

    String locations_string = null;
    Set<String> locations = new LinkedHashSet<>();

    Pattern pattern = Pattern.compile(USAGES_PATTERN);
    Matcher matcher = pattern.matcher(comment);

    if (matcher.find()) {
      locations_string = matcher.group(USAGES_GROUP_NAME);
    }

    if (locations_string != null) {
      for (String location : locations_string.split("\n")) {
        location = location.trim();
        if (!location.equals("")) {
          locations.add(location);
        }
      }
    }

    return locations;
  }
}
