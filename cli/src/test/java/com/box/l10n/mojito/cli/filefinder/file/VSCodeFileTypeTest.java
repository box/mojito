package com.box.l10n.mojito.cli.filefinder.file;

import static com.box.l10n.mojito.cli.filefinder.FilePattern.BASE_NAME;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.FILE_EXTENSION;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.LOCALE;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PARENT_PATH;

import com.box.l10n.mojito.cli.filefinder.FilePattern;
import java.util.regex.Matcher;
import org.junit.Assert;
import org.junit.Test;

public class VSCodeFileTypeTest {

  @Test
  public void testSourcePattern() {
    VSCodeFileType vSCodeFileType = new VSCodeFileType();
    FilePattern sourceFilePattern = vSCodeFileType.getSourceFilePattern();
    Matcher matcher = sourceFilePattern.getPattern().matcher("/l10n/bundle.l10n.json");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("/l10n/", matcher.group(PARENT_PATH));
    Assert.assertEquals("bundle.l10n", matcher.group(BASE_NAME));
    Assert.assertEquals("json", matcher.group(FILE_EXTENSION));
  }

  @Test
  public void testSourcePatternPakcage() {
    VSCodeFileType vSCodeFileType = new VSCodeFileType();
    FilePattern sourceFilePattern = vSCodeFileType.getSourceFilePattern();
    Matcher matcher = sourceFilePattern.getPattern().matcher("package.nls.json");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("", matcher.group(PARENT_PATH));
    Assert.assertEquals("package.nls", matcher.group(BASE_NAME));
    Assert.assertEquals("json", matcher.group(FILE_EXTENSION));
  }

  @Test
  public void testTargetPattern() {
    VSCodeFileType vSCodeFileType = new VSCodeFileType();
    Matcher matcher =
        vSCodeFileType.getTargetFilePattern().getPattern().matcher("/l10n/bundle.l10n.fr.json");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("fr", matcher.group(LOCALE));
    Assert.assertEquals("/l10n/", matcher.group(PARENT_PATH));
    Assert.assertEquals("bundle.l10n", matcher.group(BASE_NAME));
    Assert.assertEquals("json", matcher.group(FILE_EXTENSION));
  }

  @Test
  public void testTargetPatternBcp47() {
    VSCodeFileType vSCodeFileType = new VSCodeFileType();
    Matcher matcher =
        vSCodeFileType.getTargetFilePattern().getPattern().matcher("/l10n/bundle.l10n.fr-FR.json");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("fr-FR", matcher.group(LOCALE));
    Assert.assertEquals("/l10n/", matcher.group(PARENT_PATH));
    Assert.assertEquals("bundle.l10n", matcher.group(BASE_NAME));
    Assert.assertEquals("json", matcher.group(FILE_EXTENSION));
  }
}
