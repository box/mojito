package com.box.l10n.mojito.cli.filefinder.file;

import static com.box.l10n.mojito.cli.filefinder.FilePattern.BASE_NAME;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.FILE_EXTENSION;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.LOCALE;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PARENT_PATH;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.SUB_PATH;

import com.box.l10n.mojito.cli.filefinder.FilePattern;
import java.util.regex.Matcher;
import org.junit.Assert;
import org.junit.Test;

public class I18NextFileTypeTest {

  @Test
  public void testSourcePattern() {
    I18NextFileType i18NextFileType = new I18NextFileType();
    FilePattern sourceFilePattern = i18NextFileType.getSourceFilePattern();
    Matcher matcher = sourceFilePattern.getPattern().matcher("locales/en/messages.json");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("", matcher.group(PARENT_PATH));
    Assert.assertEquals("locales", matcher.group(SUB_PATH));
    Assert.assertEquals("messages", matcher.group(BASE_NAME));
    Assert.assertEquals("json", matcher.group(FILE_EXTENSION));
  }

  @Test
  public void testTargetPattern() {
    I18NextFileType i18NextFileType = new I18NextFileType();
    Matcher matcher =
        i18NextFileType.getTargetFilePattern().getPattern().matcher("locales/fr/messages.json");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("fr", matcher.group(LOCALE));
    Assert.assertEquals("", matcher.group(PARENT_PATH));
    Assert.assertEquals("locales", matcher.group(SUB_PATH));
    Assert.assertEquals("messages", matcher.group(BASE_NAME));
    Assert.assertEquals("json", matcher.group(FILE_EXTENSION));
  }

  @Test
  public void testTargetPatternBcp47() {
    I18NextFileType i18NextFileType = new I18NextFileType();
    Matcher matcher =
        i18NextFileType.getTargetFilePattern().getPattern().matcher("locales/fr-FR/messages.json");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("fr-FR", matcher.group(LOCALE));
    Assert.assertEquals("", matcher.group(PARENT_PATH));
    Assert.assertEquals("locales", matcher.group(SUB_PATH));
    Assert.assertEquals("messages", matcher.group(BASE_NAME));
    Assert.assertEquals("json", matcher.group(FILE_EXTENSION));
  }
}
