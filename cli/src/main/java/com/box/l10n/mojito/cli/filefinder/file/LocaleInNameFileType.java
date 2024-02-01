package com.box.l10n.mojito.cli.filefinder.file;

import static com.box.l10n.mojito.cli.filefinder.FilePattern.BASE_NAME;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.DOT;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.FILE_EXTENSION;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.LOCALE;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PARENT_PATH;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.UNDERSCORE;

import com.box.l10n.mojito.cli.filefinder.locale.AnyLocaleType;

/**
 * @author jaurambault
 */
public abstract class LocaleInNameFileType extends FileType {

  public LocaleInNameFileType() {
    this.sourceFilePatternTemplate =
        "{" + PARENT_PATH + "}{" + BASE_NAME + "}" + DOT + "{" + FILE_EXTENSION + "}";
    this.targetFilePatternTemplate =
        "{"
            + PARENT_PATH
            + "}{"
            + BASE_NAME
            + "}"
            + UNDERSCORE
            + "{"
            + LOCALE
            + "}"
            + DOT
            + "{"
            + FILE_EXTENSION
            + "}";
    this.localeType = new AnyLocaleType();
  }
}
