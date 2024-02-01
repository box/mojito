package com.box.l10n.mojito.cli.filefinder.file;

import static com.box.l10n.mojito.cli.filefinder.FilePattern.DOT;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.FILE_EXTENSION;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.LOCALE;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PARENT_PATH;

import com.box.l10n.mojito.cli.filefinder.locale.AnyLocaleTargetNotSourceType;

/**
 * @author jaurambault
 */
public abstract class LocaleAsFileNameType extends FileType {

  public LocaleAsFileNameType() {
    this.baseNamePattern = "";
    this.sourceFilePatternTemplate =
        "{" + PARENT_PATH + "}{" + LOCALE + "}" + DOT + "{" + FILE_EXTENSION + "}";
    this.targetFilePatternTemplate = sourceFilePatternTemplate;
    this.localeType = new AnyLocaleTargetNotSourceType();
  }
}
