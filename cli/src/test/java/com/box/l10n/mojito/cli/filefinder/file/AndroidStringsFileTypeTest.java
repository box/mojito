package com.box.l10n.mojito.cli.filefinder.file;

import com.box.l10n.mojito.cli.filefinder.FilePattern;
import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Matcher;

import static com.box.l10n.mojito.cli.filefinder.FilePattern.BASE_NAME;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.FILE_EXTENSION;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.LOCALE;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PARENT_PATH;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.SUB_PATH;

/**
 *
 * @author jaurambault
 */
public class AndroidStringsFileTypeTest {

    @Test
    public void testSourcePattern() {
        AndroidStringsFileType androidStringsFileType = new AndroidStringsFileType();
        FilePattern sourceFilePattern = androidStringsFileType.getSourceFilePattern();
        Matcher matcher = sourceFilePattern.getPattern().matcher("/source/res/values/strings.xml");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
        Assert.assertEquals("res/values", matcher.group(SUB_PATH));
        Assert.assertEquals("strings", matcher.group(BASE_NAME));
        Assert.assertEquals("xml", matcher.group(FILE_EXTENSION));
    }

    @Test
    public void testSourcePatternNoMatch() {
        AndroidStringsFileType androidStringsFileType = new AndroidStringsFileType();
        FilePattern sourceFilePattern = androidStringsFileType.getSourceFilePattern();
        Matcher matcher = sourceFilePattern.getPattern().matcher("/source/res/values-fr/strings.xml");
        Assert.assertFalse(matcher.matches());
    }

    @Test
    public void testTargetPattern() {
        AndroidStringsFileType androidStringsFileType = new AndroidStringsFileType();
        Matcher matcher = androidStringsFileType.getTargetFilePattern().getPattern().matcher("/source/res/values-fr/strings.xml");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
        Assert.assertEquals("fr", matcher.group(LOCALE));
        Assert.assertEquals("res/values", matcher.group(SUB_PATH));
        Assert.assertEquals("strings", matcher.group(BASE_NAME));
        Assert.assertEquals("xml", matcher.group(FILE_EXTENSION));
    }

    @Test
    public void testTargetPatternBcp47() {
        AndroidStringsFileType androidStringsFileType = new AndroidStringsFileType();
        Matcher matcher = androidStringsFileType.getTargetFilePattern().getPattern().matcher("/source/res/values-en-rGB/strings.xml");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
        Assert.assertEquals("en-rGB", matcher.group(LOCALE));
        Assert.assertEquals("res/values", matcher.group(SUB_PATH));
        Assert.assertEquals("strings", matcher.group(BASE_NAME));
        Assert.assertEquals("xml", matcher.group(FILE_EXTENSION));
    }

    @Test
    public void testTargetPatternNoMatch() {
        AndroidStringsFileType androidStringsFileType = new AndroidStringsFileType();
        Matcher matcher = androidStringsFileType.getTargetFilePattern().getPattern().matcher("/source/res/values/strings.xml");
        Assert.assertFalse(matcher.matches());
    }

}
