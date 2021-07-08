package com.box.l10n.mojito.cli.filefinder.file;

import static com.box.l10n.mojito.cli.filefinder.FilePattern.BASE_NAME;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.DOT;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.FILE_EXTENSION;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.LOCALE;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PARENT_PATH;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PATH_SEPERATOR;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.SUB_PATH;

import com.box.l10n.mojito.cli.filefinder.locale.AnyLocaleTargetNotSourceType;

/**
 * https://github.com/i18next/i18next-parser/tree/master/test
 */
public class I18NextFileType extends FileType {

  public I18NextFileType() {
    this.sourceFileExtension = "json";
    this.subPath = "locales";
    this.sourceFilePatternTemplate = "{" + PARENT_PATH + "}{" + SUB_PATH + "}" + PATH_SEPERATOR + "{" + LOCALE + "}" + PATH_SEPERATOR + "{" + BASE_NAME + "}" + DOT + "{" + FILE_EXTENSION + "}";
    this.targetFilePatternTemplate = sourceFilePatternTemplate;
    this.localeType = new AnyLocaleTargetNotSourceType();
  }

}
