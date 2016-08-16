package com.box.l10n.mojito.cli.filefinder.file;

import static com.box.l10n.mojito.cli.filefinder.FilePattern.BASE_NAME;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.DOT;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.FILE_EXTENSION;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.LOCALE;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PARENT_PATH;

/**
 *
 * @author jaurambault
 */
public class ResxFileType extends LocaleInNameFileType {

    public ResxFileType() {
        this.fileExtension = "resx";
        this.targetFilePatternTemplate = "{" + PARENT_PATH + "}{" + BASE_NAME + "}" + DOT + "{" + LOCALE + "}" + DOT + "{" + FILE_EXTENSION + "}";
    }

}