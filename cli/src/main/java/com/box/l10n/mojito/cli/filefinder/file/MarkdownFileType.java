package com.box.l10n.mojito.cli.filefinder.file;

import com.box.l10n.mojito.cli.filefinder.FilePattern;
import com.box.l10n.mojito.cli.filefinder.locale.AnyLocaleTargetNotSourceType;
import com.box.l10n.mojito.cli.filefinder.locale.POLocaleType;

import static com.box.l10n.mojito.cli.filefinder.FilePattern.*;

/**
 *
 * @author jeanaurambault
 */
public class MarkdownFileType extends LocaleInNameFileType {

    public MarkdownFileType() {
        this.sourceFileExtension = "md";
        this.sourceFilePatternTemplate = "{" + PARENT_PATH + "}{" + LOCALE + "}" + PATH_SEPERATOR + "{" + BASE_NAME + "}" + DOT + "{" + FILE_EXTENSION + "}";
        this.targetFilePatternTemplate = sourceFilePatternTemplate;
        this.localeType = new AnyLocaleTargetNotSourceType();
    }
}
