package com.box.l10n.mojito.cli.filefinder.file;

import static com.box.l10n.mojito.cli.filefinder.FilePattern.BASE_NAME;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.DOT;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.FILE_EXTENSION;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.LOCALE;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PARENT_PATH;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PATH_SEPERATOR;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.SUB_PATH;

import com.box.l10n.mojito.cli.filefinder.locale.AnyLocaleTargetNotSourceType;

/** @author emagalindan */
public class MacStringsdictFileType extends FileType {

  public MacStringsdictFileType() {
    this.sourceFileExtension = "stringsdict";
    this.baseNamePattern = "Localizable";
    this.subPath = "lproj";
    this.sourceFilePatternTemplate =
        "{"
            + PARENT_PATH
            + "}{"
            + LOCALE
            + "}"
            + DOT
            + "{"
            + SUB_PATH
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
    this.localeType = new AnyLocaleTargetNotSourceType();
    this.gitBlameType = GitBlameType.TEXT_UNIT_USAGES;
  }
}
