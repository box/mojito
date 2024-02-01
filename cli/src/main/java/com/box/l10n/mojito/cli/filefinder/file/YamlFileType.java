package com.box.l10n.mojito.cli.filefinder.file;

/**
 * @author loliveiramontedonio
 */
public class YamlFileType
    extends LocaleInNameFileType { // TODO: Discuss with API team how the locale/filename would work

  public YamlFileType() {
    this.sourceFileExtension = "yaml";
  }
}
