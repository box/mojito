package com.box.l10n.mojito.cli.filefinder.file;

import static com.box.l10n.mojito.cli.filefinder.FilePattern.BASE_NAME;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.DOT;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.FILE_EXTENSION;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.LOCALE;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PARENT_PATH;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PATH_SEPERATOR;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.SUB_PATH;

import com.box.l10n.mojito.cli.filefinder.locale.ChromeExtJsonLocaleType;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * @author jeanaurambault
 */
public class ChromeExtensionJSONFileType extends FileType {

  public ChromeExtensionJSONFileType() {
    this.sourceFileExtension = "json";
    this.baseNamePattern = "messages";
    this.subPath = "_locales";
    this.sourceFilePatternTemplate =
        "{"
            + PARENT_PATH
            + "}{"
            + SUB_PATH
            + "}"
            + PATH_SEPERATOR
            + "{"
            + LOCALE
            + "}"
            + PATH_SEPERATOR
            + "{"
            + BASE_NAME
            + "}"
            + DOT
            + "{"
            + FILE_EXTENSION
            + "}";
    this.targetFilePatternTemplate = sourceFilePatternTemplate;
    this.localeType = new ChromeExtJsonLocaleType();
    this.textUnitNameToTextUnitNameInSourceSingular = Pattern.compile("(?<s>.*)/message");
    this.textUnitNameToTextUnitNameInSourcePlural =
        Pattern.compile("(?<s>.*)"); // plural not support just accept anything
    this.defaultFilterOptions =
        Arrays.asList("noteKeyPattern=description", "extractAllPairs=false", "exceptions=message");
  }
}
