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

/** @author emagalindan */
public class MacStringsdictFileTypeTest {

  @Test
  public void testSourcePattern() {
    MacStringsdictFileType macStringsdictFileType = new MacStringsdictFileType();
    FilePattern sourceFilePattern = macStringsdictFileType.getSourceFilePattern();
    Matcher matcher =
        sourceFilePattern.getPattern().matcher("/source/en.lproj/Localizable.stringsdict");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
    Assert.assertEquals("en", matcher.group(LOCALE));
    Assert.assertEquals("lproj", matcher.group(SUB_PATH));
    Assert.assertEquals("Localizable", matcher.group(BASE_NAME));
    Assert.assertEquals("stringsdict", matcher.group(FILE_EXTENSION));
  }

  @Test
  public void testSourcePatternNoMatch() {
    MacStringsdictFileType macStringsdictFileType = new MacStringsdictFileType();
    FilePattern sourceFilePattern = macStringsdictFileType.getSourceFilePattern();
    Matcher matcher =
        sourceFilePattern.getPattern().matcher("/source/fr.lproj/Localizable.stringsdict");
    Assert.assertFalse(matcher.matches());
  }

  @Test
  public void testTargetPattern() {
    MacStringsdictFileType macStringsdictFileType = new MacStringsdictFileType();
    Matcher matcher =
        macStringsdictFileType
            .getTargetFilePattern()
            .getPattern()
            .matcher("/source/fr.lproj/Localizable.stringsdict");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
    Assert.assertEquals("fr", matcher.group(LOCALE));
    Assert.assertEquals("lproj", matcher.group(SUB_PATH));
    Assert.assertEquals("Localizable", matcher.group(BASE_NAME));
    Assert.assertEquals("stringsdict", matcher.group(FILE_EXTENSION));
  }

  @Test
  public void testTargetPatternBcp47() {
    MacStringsdictFileType macStringsdictFileType = new MacStringsdictFileType();
    Matcher matcher =
        macStringsdictFileType
            .getTargetFilePattern()
            .getPattern()
            .matcher("/source/en-GB.lproj/Localizable.stringsdict");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
    Assert.assertEquals("en-GB", matcher.group(LOCALE));
    Assert.assertEquals("lproj", matcher.group(SUB_PATH));
    Assert.assertEquals("Localizable", matcher.group(BASE_NAME));
    Assert.assertEquals("stringsdict", matcher.group(FILE_EXTENSION));
  }

  @Test
  public void testTargetPatternNoMatch() {
    MacStringsdictFileType macStringsdictFileType = new MacStringsdictFileType();
    Matcher matcher =
        macStringsdictFileType
            .getTargetFilePattern()
            .getPattern()
            .matcher("/source/en.lproj/Localizable.stringsdict");
    Assert.assertFalse(matcher.matches());
  }
}
