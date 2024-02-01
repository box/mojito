package com.box.l10n.mojito.cli.filefinder.file;

import static com.box.l10n.mojito.cli.filefinder.FilePattern.DOT;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.FILE_EXTENSION;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.LOCALE;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PARENT_PATH;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PATH_SEPERATOR;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.SUB_PATH;

import com.box.l10n.mojito.cli.filefinder.locale.CVSAdobeMagentoLocaleType;
import com.box.l10n.mojito.okapi.FilterConfigIdOverride;

/**
 * @author jaurambault
 */
public class CSVAdobeMagentoFileType extends LocaleAsFileNameType {

  public CSVAdobeMagentoFileType() {
    this.sourceFileExtension = "csv";
    this.subPath = "i18n";
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
            + DOT
            + "{"
            + FILE_EXTENSION
            + "}";
    this.targetFilePatternTemplate = this.sourceFilePatternTemplate;
    this.filterConfigIdOverride = FilterConfigIdOverride.CSV_ADOBE_MAGENTO;
    this.localeType = new CVSAdobeMagentoLocaleType();
  }
}
