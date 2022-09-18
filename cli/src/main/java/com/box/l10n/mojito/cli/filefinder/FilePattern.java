package com.box.l10n.mojito.cli.filefinder;

import com.box.l10n.mojito.cli.filefinder.file.FileType;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes a file pattern for source and target files.
 *
 * <p>Placeholder can be used to extract specific information when matching filename and can get
 * there values from {@link FileType}.
 *
 * @author jaurambault
 */
public class FilePattern {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(FilePattern.class);

  public static final String FILE_EXTENSION = "fileExtension";
  public static final String BASE_NAME = "baseName";
  public static final String PARENT_PATH = "parentPath";
  public static final String SUB_PATH = "subPath";
  public static final String LOCALE = "locale";
  public static final String DOT = ".";
  public static final String UNDERSCORE = "_";
  public static final String HYPHEN = "-";
  public static final String PATH_SEPERATOR = "/";

  /** Pattern string to find placeholder in a file pattern template */
  static final String PLACEHOLDER_PATTERN_STR = "\\{(.*?)}";

  /** Actual pattern that matches placeholder in a filename expression. */
  static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(PLACEHOLDER_PATTERN_STR);

  /** Pattern used to match file names. */
  private Pattern pattern;

  /** Groups in the regex. */
  private final Set<String> groups = new HashSet<>();

  /** File type associated with this pattern. */
  private final FileType fileType;

  /** Indicates if this pattern is used to match source or target files. */
  private final boolean forSourceFile;

  public FilePattern(FileType fileType, boolean forSourceFile) {
    this.fileType = fileType;
    this.forSourceFile = forSourceFile;
    initPattern();
  }

  public Pattern getPattern() {
    return pattern;
  }

  public Set<String> getGroups() {
    return groups;
  }

  /** Initialize the pattern to match files. */
  private void initPattern() {

    StringBuffer patternTemplateRegex = new StringBuffer();

    logger.debug("Get placeholder matcher");
    Matcher placeholderMatcher = getPlaceholderMatcher();

    logger.debug("Replace all placeholder with regex group capture");
    while (placeholderMatcher.find()) {

      String group = placeholderMatcher.group(1);

      if (!groups.contains(group)) {
        logger.trace("New group found, add it, replace with a capture group");
        groups.add(group);

        String groupRegex = getGroupRegex(group);

        placeholderMatcher.appendReplacement(
            patternTemplateRegex, Matcher.quoteReplacement("(?<" + group + ">" + groupRegex + ")"));
      } else {
        logger.trace("Existing group found, replace with a back reference");
        placeholderMatcher.appendReplacement(
            patternTemplateRegex, Matcher.quoteReplacement("\\k<" + group + ">"));
      }
    }

    placeholderMatcher.appendTail(patternTemplateRegex);

    String patternRegex = patternTemplateRegex.toString();
    logger.debug("pattern:" + patternRegex);

    pattern = Pattern.compile(patternRegex);
  }

  /**
   * Gets the placeholder matcher for either source or target file pattern template.
   *
   * @return a placholder matcher.
   */
  private Matcher getPlaceholderMatcher() {

    String filePatternTemplate;

    if (forSourceFile) {
      filePatternTemplate = fileType.getSourceFilePatternTemplate();
    } else {
      filePatternTemplate = fileType.getTargetFilePatternTemplate();
    }

    return PLACEHOLDER_PATTERN.matcher(patternTemplateToRegex(filePatternTemplate));
  }

  /**
   * Gets the regex for a group.
   *
   * @param group the group
   * @return
   */
  private String getGroupRegex(String group) {

    String groupRegex = ".+?";

    switch (group) {
      case PARENT_PATH:
        groupRegex = fileType.getParentPath();
        break;

      case SUB_PATH:
        groupRegex = fileType.getSubPath();
        break;

      case BASE_NAME:
        groupRegex = fileType.getBaseNamePattern();
        break;

      case FILE_EXTENSION:
        if (forSourceFile) {
          groupRegex = fileType.getSourceFileExtension();
        } else {
          groupRegex = fileType.getTargetFileExtension();
        }
        break;

      case LOCALE:
        if (forSourceFile) {
          groupRegex = fileType.getLocaleType().getSourceLocale();
        } else {
          groupRegex = fileType.getLocaleType().getTargetLocaleRegex();
        }
        break;
    }

    return groupRegex;
  }

  /**
   * Transform the tempalte into a proper regex.
   *
   * <p>Right now, it just escapes dots
   *
   * @param filePatternTemplate a template to be transform into a regex
   * @return the corresponding regex
   */
  private String patternTemplateToRegex(String filePatternTemplate) {
    return filePatternTemplate.replaceAll("\\.", Matcher.quoteReplacement("\\."));
  }
}
