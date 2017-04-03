package com.box.l10n.mojito.cli.filefinder.file;

import com.box.l10n.mojito.cli.filefinder.locale.LocaleType;
import com.box.l10n.mojito.cli.filefinder.locale.PropertiesJavaLocaleType;
import com.box.l10n.mojito.rest.entity.FilterConfigIdOverride;
import com.box.l10n.mojito.rest.entity.SourceAsset;

/**
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
