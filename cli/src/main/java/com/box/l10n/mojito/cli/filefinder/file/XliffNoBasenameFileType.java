package com.box.l10n.mojito.cli.filefinder.file;

/**
 * Format to support xliff file that doesn't have a basename but use a locale
 * for the source file
 *
 * eg. source file: en.xliff (assuming 'en' is the source locale) and localized
 * files: fr.xliff, ko.xliff, etc.
 *
 * @author jyi
 */
public class XliffNoBasenameFileType extends LocaleAsFileNameType {

    public XliffNoBasenameFileType() {
        this.fileExtension = "xliff";
    }

}
