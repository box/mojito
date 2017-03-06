package com.box.l10n.mojito.cli.filefinder.file;

import static com.box.l10n.mojito.cli.filefinder.FilePattern.BASE_NAME;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.DOT;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.FILE_EXTENSION;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.LOCALE;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PARENT_PATH;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PATH_SEPERATOR;
import com.box.l10n.mojito.cli.filefinder.locale.AnyLocaleType;

/**
 *
 * @author jyi
 */
public class ReswFileType extends FileType {

    public ReswFileType() {
        this.fileExtension = "resw";
        this.sourceFilePatternTemplate = "{" + PARENT_PATH + "}{" + LOCALE + "}" + PATH_SEPERATOR + "{" + BASE_NAME + "}" + DOT + "{" + FILE_EXTENSION + "}";
        this.targetFilePatternTemplate = sourceFilePatternTemplate;
        this.localeType = new AnyLocaleType();
    }
    
}
