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

/** @author jyi */
public class MacStringsFileTypeTest {

  @Test
  public void testSourcePattern() {
    MacStringsFileType macStringsFileType = new MacStringsFileType();
    FilePattern sourceFilePattern = macStringsFileType.getSourceFilePattern();
    Matcher matcher =
        sourceFilePattern.getPattern().matcher("/source/en.lproj/Localizable.strings");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
    Assert.assertEquals("en", matcher.group(LOCALE));
    Assert.assertEquals("lproj", matcher.group(SUB_PATH));
    Assert.assertEquals("Localizable", matcher.group(BASE_NAME));
    Assert.assertEquals("strings", matcher.group(FILE_EXTENSION));
  }

  @Test
  public void testSourcePatternNoMatch() {
    MacStringsFileType macStringsFileType = new MacStringsFileType();
    FilePattern sourceFilePattern = macStringsFileType.getSourceFilePattern();
    Matcher matcher =
        sourceFilePattern.getPattern().matcher("/source/fr.lproj/Localizable.strings");
    Assert.assertFalse(matcher.matches());
  }

  @Test
  public void testTargetPattern() {
    MacStringsFileType macStringsFileType = new MacStringsFileType();
    Matcher matcher =
        macStringsFileType
            .getTargetFilePattern()
            .getPattern()
            .matcher("/source/fr.lproj/Localizable.strings");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
    Assert.assertEquals("fr", matcher.group(LOCALE));
    Assert.assertEquals("lproj", matcher.group(SUB_PATH));
    Assert.assertEquals("Localizable", matcher.group(BASE_NAME));
    Assert.assertEquals("strings", matcher.group(FILE_EXTENSION));
  }

  @Test
  public void testTargetPatternBcp47() {
    MacStringsFileType macStringsFileType = new MacStringsFileType();
    Matcher matcher =
        macStringsFileType
            .getTargetFilePattern()
            .getPattern()
            .matcher("/source/en-GB.lproj/Localizable.strings");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
    Assert.assertEquals("en-GB", matcher.group(LOCALE));
    Assert.assertEquals("lproj", matcher.group(SUB_PATH));
    Assert.assertEquals("Localizable", matcher.group(BASE_NAME));
    Assert.assertEquals("strings", matcher.group(FILE_EXTENSION));
  }

  @Test
  public void testTargetPatternNoMatch() {
    MacStringsFileType macStringsFileType = new MacStringsFileType();
    Matcher matcher =
        macStringsFileType
            .getTargetFilePattern()
            .getPattern()
            .matcher("/source/en.lproj/Localizable.strings");
    Assert.assertFalse(matcher.matches());
  }
}
