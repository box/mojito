package com.box.l10n.mojito.cli.filefinder.file;

import static com.box.l10n.mojito.cli.filefinder.FilePattern.BASE_NAME;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.DOT;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.FILE_EXTENSION;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.HYPHEN;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.LOCALE;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PARENT_PATH;

import com.box.l10n.mojito.cli.filefinder.locale.AnyLocaleTargetNotSourceType;

/** @author jyi */
public class XtbFileType extends FileType {

  public XtbFileType() {
    this.sourceFileExtension = "xtb";
    this.sourceFilePatternTemplate =
        "{"
            + PARENT_PATH
            + "}{"
            + BASE_NAME
            + "}"
            + HYPHEN
            + "{"
            + LOCALE
            + "}"
            + DOT
            + "{"
            + FILE_EXTENSION
            + "}";
    this.targetFilePatternTemplate = sourceFilePatternTemplate;
    this.localeType = new AnyLocaleTargetNotSourceType();
  }
}
