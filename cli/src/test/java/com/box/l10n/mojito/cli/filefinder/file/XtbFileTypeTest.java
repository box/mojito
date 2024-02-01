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
public class XtbFileTypeTest {

  @Test
  public void testSourcePattern() {
    XtbFileType xtbFileType = new XtbFileType();
    xtbFileType.getLocaleType().setSourceLocale("en-US");
    FilePattern sourceFilePattern = xtbFileType.getSourceFilePattern();
    Matcher matcher = sourceFilePattern.getPattern().matcher("/source/Filefinder-en-US.xtb");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("en-US", matcher.group(LOCALE));
    Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
    Assert.assertEquals("Filefinder", matcher.group(BASE_NAME));
    Assert.assertEquals("xtb", matcher.group(FILE_EXTENSION));
  }

  @Test
  public void testTargetPattern() {
    XtbFileType xtbFileType = new XtbFileType();
    Matcher matcher =
        xtbFileType.getTargetFilePattern().getPattern().matcher("/source/Filefinder-fr.xtb");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("fr", matcher.group(LOCALE));
    Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
    Assert.assertEquals("Filefinder", matcher.group(BASE_NAME));
    Assert.assertEquals("xtb", matcher.group(FILE_EXTENSION));
  }

  @Test
  public void testTargetPatternBcp47() {
    XtbFileType xtbFileType = new XtbFileType();
    Matcher matcher =
        xtbFileType.getTargetFilePattern().getPattern().matcher("/source/Filefinder-en-GB.xtb");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("en-GB", matcher.group(LOCALE));
    Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
    Assert.assertEquals("Filefinder", matcher.group(BASE_NAME));
    Assert.assertEquals("xtb", matcher.group(FILE_EXTENSION));
  }
}
