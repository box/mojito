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
 * @author jaurambault
 */
public class JSONFileTypeTest {

  @Test
  public void testSourcePattern() {
    JSONFileType jsonFileType = new JSONFileType();
    FilePattern sourceFilePattern = jsonFileType.getSourceFilePattern();
    Matcher matcher = sourceFilePattern.getPattern().matcher("/source/filefinder.json");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
    Assert.assertEquals("filefinder", matcher.group(BASE_NAME));
    Assert.assertEquals("json", matcher.group(FILE_EXTENSION));
  }

  @Test
  public void testTargetPattern() {
    JSONFileType jsonFileType = new JSONFileType();
    Matcher matcher =
        jsonFileType.getTargetFilePattern().getPattern().matcher("/source/filefinder_fr.json");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("fr", matcher.group(LOCALE));
    Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
    Assert.assertEquals("filefinder", matcher.group(BASE_NAME));
    Assert.assertEquals("json", matcher.group(FILE_EXTENSION));
  }

  @Test
  public void testTargetPatternBcp47() {
    JSONFileType jsonFileType = new JSONFileType();
    Matcher matcher =
        jsonFileType.getTargetFilePattern().getPattern().matcher("/source/filefinder_fr-FR.json");
    Assert.assertTrue(matcher.matches());
    Assert.assertEquals("fr-FR", matcher.group(LOCALE));
    Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
    Assert.assertEquals("filefinder", matcher.group(BASE_NAME));
    Assert.assertEquals("json", matcher.group(FILE_EXTENSION));
  }
}
