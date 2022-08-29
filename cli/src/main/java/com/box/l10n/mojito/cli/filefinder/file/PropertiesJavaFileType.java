package com.box.l10n.mojito.cli.filefinder.file;

import com.box.l10n.mojito.cli.filefinder.locale.LocaleType;
import com.box.l10n.mojito.cli.filefinder.locale.PropertiesJavaLocaleType;
import com.box.l10n.mojito.okapi.FilterConfigIdOverride;

/**
 * Escaped/ISO 8859-1 with base name
 *
 * @author jaurambault
 */
public class PropertiesJavaFileType extends LocaleInNameFileType {

  public PropertiesJavaFileType() {
    this.sourceFileExtension = "properties";
    this.filterConfigIdOverride = FilterConfigIdOverride.PROPERTIES_JAVA;
  }

  @Override
  public LocaleType getLocaleType() {
    return new PropertiesJavaLocaleType();
  }
}
