package com.box.l10n.mojito.cli.filefinder.file;

import com.box.l10n.mojito.rest.entity.FilterConfigIdOverride;

/**
 * Format to support xliff file that is generated by Xcode.
 *
 * eg. source file: en.xliff (assuming 'en' is the source locale) and localized
 * files: fr.xliff, ko.xliff, etc.
 *
 * @author jyi
 */
public class XcodeXliffFileType extends LocaleAsFileNameType {

    public XcodeXliffFileType() {
        this.sourceFileExtension = "xliff";
        this.filterConfigIdOverride = FilterConfigIdOverride.XCODE_XLIFF;
    }

}
