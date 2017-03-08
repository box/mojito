package com.box.l10n.mojito.cli.filefinder.file;

import static com.box.l10n.mojito.cli.filefinder.FilePattern.BASE_NAME;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.DOT;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.FILE_EXTENSION;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.HYPHEN;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.LOCALE;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PARENT_PATH;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PATH_SEPERATOR;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.SUB_PATH;
import com.box.l10n.mojito.cli.filefinder.locale.AndroidLocaleType;

/**
 *
 * @author jaurambault
 */
public class AndroidStringsFileType extends FileType {

    public AndroidStringsFileType() {
        this.sourceFileExtension = "xml";
        this.baseNamePattern = "strings";
        this.subPath = "res/values";
        this.sourceFilePatternTemplate = "{" + PARENT_PATH + "}{" + SUB_PATH + "}" + PATH_SEPERATOR + "{" + BASE_NAME + "}" + DOT + "{" + FILE_EXTENSION + "}";
        this.targetFilePatternTemplate = "{" + PARENT_PATH + "}{" + SUB_PATH + "}" + HYPHEN + "{" + LOCALE + "}" + PATH_SEPERATOR + "{" + BASE_NAME + "}" + DOT + "{" + FILE_EXTENSION + "}";
        this.localeType = new AndroidLocaleType();
    }
}
