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

public class ChromeExtensionJSONFileTypeTest {

    @Test
    public void testSourcePattern() {
        ChromeExtensionJSONFileType chromeExtensionJSONFileType = new ChromeExtensionJSONFileType();
        FilePattern sourceFilePattern = chromeExtensionJSONFileType.getSourceFilePattern();
        Matcher matcher = sourceFilePattern.getPattern().matcher("_locales/en/messages.json");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("", matcher.group(PARENT_PATH));
        Assert.assertEquals("_locales", matcher.group(SUB_PATH));
        Assert.assertEquals("messages", matcher.group(BASE_NAME));
        Assert.assertEquals("json", matcher.group(FILE_EXTENSION));
    }

    @Test
    public void testTargetPattern() {
        ChromeExtensionJSONFileType chromeExtensionJSONFileType = new ChromeExtensionJSONFileType();
        Matcher matcher = chromeExtensionJSONFileType.getTargetFilePattern().getPattern().matcher("_locales/fr/messages.json");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("fr", matcher.group(LOCALE));
        Assert.assertEquals("", matcher.group(PARENT_PATH));
        Assert.assertEquals("_locales", matcher.group(SUB_PATH));
        Assert.assertEquals("messages", matcher.group(BASE_NAME));
        Assert.assertEquals("json", matcher.group(FILE_EXTENSION));
    }

    @Test
    public void testTargetPatternBcp47() {
        ChromeExtensionJSONFileType chromeExtensionJSONFileType = new ChromeExtensionJSONFileType();
        Matcher matcher = chromeExtensionJSONFileType.getTargetFilePattern().getPattern().matcher("_locales/fr-FR/messages.json");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("fr-FR", matcher.group(LOCALE));
        Assert.assertEquals("", matcher.group(PARENT_PATH));
        Assert.assertEquals("_locales", matcher.group(SUB_PATH));
        Assert.assertEquals("messages", matcher.group(BASE_NAME));
        Assert.assertEquals("json", matcher.group(FILE_EXTENSION));
    }
}
