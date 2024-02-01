package com.box.l10n.mojito.cli.filefinder.file;

import static com.box.l10n.mojito.cli.filefinder.FilePattern.BASE_NAME;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.FILE_EXTENSION;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.LOCALE;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PARENT_PATH;

import com.box.l10n.mojito.cli.filefinder.FilePattern;
import java.util.regex.Matcher;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author jyi
 */
public class ReswFileTypeTest {

  @Test
  public void testSourcePattern() {
    ReswFileType reswFileType = new ReswFileType();
    FilePattern sourceFilePattern = reswFileType.getSourceFilePattern();
    Matcher matcher = sourceFilePattern.getPattern().matcher("/source/en/Resources.resw");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
    Assert.assertEquals("en", matcher.group(LOCALE));
    Assert.assertEquals("Resources", matcher.group(BASE_NAME));
    Assert.assertEquals("resw", matcher.group(FILE_EXTENSION));
  }

  @Test
  public void testSourcePatternNoMatch() {
    ReswFileType reswFileType = new ReswFileType();
    FilePattern sourceFilePattern = reswFileType.getSourceFilePattern();
    Matcher matcher = sourceFilePattern.getPattern().matcher("/source/fr/Resources.resw");
    Assert.assertFalse(matcher.matches());
  }

  @Test
  public void testTargetPattern() {
    ReswFileType reswFileType = new ReswFileType();
    Matcher matcher =
        reswFileType.getTargetFilePattern().getPattern().matcher("/source/fr/Resources.resw");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
    Assert.assertEquals("fr", matcher.group(LOCALE));
    Assert.assertEquals("Resources", matcher.group(BASE_NAME));
    Assert.assertEquals("resw", matcher.group(FILE_EXTENSION));
  }

  @Test
  public void testTargetPatternBcp47() {
    ReswFileType reswFileType = new ReswFileType();
    Matcher matcher =
        reswFileType.getTargetFilePattern().getPattern().matcher("/source/en-GB/Resources.resw");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
    Assert.assertEquals("en-GB", matcher.group(LOCALE));
    Assert.assertEquals("Resources", matcher.group(BASE_NAME));
    Assert.assertEquals("resw", matcher.group(FILE_EXTENSION));
  }

  @Test
  public void testTargetPatternNoMatch() {
    ReswFileType reswFileType = new ReswFileType();
    Matcher matcher =
        reswFileType.getTargetFilePattern().getPattern().matcher("/source/en/Resources.resw");
    Assert.assertFalse(matcher.matches());
  }
}
