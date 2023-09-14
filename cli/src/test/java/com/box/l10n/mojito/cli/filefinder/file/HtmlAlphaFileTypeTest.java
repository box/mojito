package com.box.l10n.mojito.cli.filefinder.file;

import static com.box.l10n.mojito.cli.filefinder.FilePattern.BASE_NAME;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.FILE_EXTENSION;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.LOCALE;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PARENT_PATH;

import com.box.l10n.mojito.cli.filefinder.FilePattern;
import java.util.regex.Matcher;
import org.junit.Assert;
import org.junit.Test;

/** @author jaurambault */
public class HtmlAlphaFileTypeTest {

  @Test
  public void testSourcePattern() {
    HtmlAlphaFileType htmlAlphaFileType = new HtmlAlphaFileType();
    FilePattern sourceFilePattern = htmlAlphaFileType.getSourceFilePattern();
    Matcher matcher = sourceFilePattern.getPattern().matcher("/source/filefinder.html");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
    Assert.assertEquals("filefinder", matcher.group(BASE_NAME));
    Assert.assertEquals("html", matcher.group(FILE_EXTENSION));
  }

  @Test
  public void testTargetPattern() {
    HtmlAlphaFileType htmlAlphaFileType = new HtmlAlphaFileType();
    Matcher matcher =
        htmlAlphaFileType.getTargetFilePattern().getPattern().matcher("/source/filefinder_fr.html");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("fr", matcher.group(LOCALE));
    Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
    Assert.assertEquals("filefinder", matcher.group(BASE_NAME));
    Assert.assertEquals("html", matcher.group(FILE_EXTENSION));
  }

  @Test
  public void testTargetPatternBcp47() {
    HtmlAlphaFileType htmlAlphaFileType = new HtmlAlphaFileType();
    Matcher matcher =
        htmlAlphaFileType
            .getTargetFilePattern()
            .getPattern()
            .matcher("/source/filefinder_fr-FR.html");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("fr-FR", matcher.group(LOCALE));
    Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
    Assert.assertEquals("filefinder", matcher.group(BASE_NAME));
    Assert.assertEquals("html", matcher.group(FILE_EXTENSION));
  }
}
