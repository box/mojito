package com.box.l10n.mojito.cli.filefinder.file;

import com.box.l10n.mojito.cli.filefinder.FilePattern;
import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Matcher;

import static com.box.l10n.mojito.cli.filefinder.FilePattern.BASE_NAME;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.FILE_EXTENSION;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.LOCALE;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PARENT_PATH;

/**
 *
 * @author jyi
 */
public class ResxFileTypeTest {

    @Test
    public void testSourcePattern() {
        ResxFileType resxFileType = new ResxFileType();
        FilePattern sourceFilePattern = resxFileType.getSourceFilePattern();
        Matcher matcher = sourceFilePattern.getPattern().matcher("/source/Filefinder.resx");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
        Assert.assertEquals("Filefinder", matcher.group(BASE_NAME));
        Assert.assertEquals("resx", matcher.group(FILE_EXTENSION));

    }

    @Test
    public void testTargetPattern() {
        ResxFileType resxFileType = new ResxFileType();
        Matcher matcher = resxFileType.getTargetFilePattern().getPattern().matcher("/source/Filefinder.fr.resx");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("fr", matcher.group(LOCALE));
        Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
        Assert.assertEquals("Filefinder", matcher.group(BASE_NAME));
        Assert.assertEquals("resx", matcher.group(FILE_EXTENSION));
    }

    @Test
    public void testTargetPatternBcp47() {
        ResxFileType resxFileType = new ResxFileType();
        Matcher matcher = resxFileType.getTargetFilePattern().getPattern().matcher("/source/Filefinder.en-GB.resx");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("en-GB", matcher.group(LOCALE));
        Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
        Assert.assertEquals("Filefinder", matcher.group(BASE_NAME));
        Assert.assertEquals("resx", matcher.group(FILE_EXTENSION));
    }

}
