package com.box.l10n.mojito.cli.filefinder.file;

import static com.box.l10n.mojito.cli.filefinder.FilePattern.BASE_NAME;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.DOT;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.FILE_EXTENSION;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.LOCALE;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PARENT_PATH;

import com.box.l10n.mojito.cli.filefinder.locale.AnyLocaleType;

/**
 * https://github.com/microsoft/vscode-extension-samples/tree/main/l10n-sample
 *
 * @author jaurambault
 */
public class VSCodeFileType extends JSONFileType {

  public VSCodeFileType() {
    this.baseNamePattern = "(package\\.nls|bundle\\.l10n)";
    this.sourceFilePatternTemplate =
        "{" + PARENT_PATH + "}{" + BASE_NAME + "}" + DOT + "{" + FILE_EXTENSION + "}";
    this.targetFilePatternTemplate =
        "{"
            + PARENT_PATH
            + "}{"
            + BASE_NAME
            + "}"
            + DOT
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
