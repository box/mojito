package com.box.l10n.mojito.cli.filefinder.file;

/**
 * UTF-8 with no basename.
 *
 * Format to support properties file that don't have a basename but use a locale
 * for the source file
 *
 * eg. source file: en.properties (assuming 'en' is the source locale) and
 * localized files: fr.properties, ko.properties, etc.
 *
 * @author jaurambault
 */
public class PropertiesNoBasenameFileType extends LocaleAsFileNameType {

    public PropertiesNoBasenameFileType() {
        this.sourceFileExtension = "properties";
    }

}
