package com.box.l10n.mojito.cli.filefinder.file;

import com.box.l10n.mojito.cli.filefinder.FilePattern;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.BASE_NAME;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.FILE_EXTENSION;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.LOCALE;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PARENT_PATH;
import java.util.regex.Matcher;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author jaurambault
 */
public class PropertiesFileTypeTest {

    @Test
    public void testSourcePattern() {
        PropertiesFileType propertiesFileType = new PropertiesFileType();
        FilePattern sourceFilePattern = propertiesFileType.getSourceFilePattern();
        Matcher matcher = sourceFilePattern.getPattern().matcher("/source/filefinder.properties");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
        Assert.assertEquals("filefinder", matcher.group(BASE_NAME));
        Assert.assertEquals("properties", matcher.group(FILE_EXTENSION));

    }

    @Test
    public void testTargetPattern() {
        PropertiesFileType propertiesFileType = new PropertiesFileType();
        Matcher matcher = propertiesFileType.getTargetFilePattern().getPattern().matcher("/source/filefinder_fr.properties");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("fr", matcher.group(LOCALE));
        Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
        Assert.assertEquals("filefinder", matcher.group(BASE_NAME));
        Assert.assertEquals("properties", matcher.group(FILE_EXTENSION));
    }

    @Test
    public void testTargetPatternBcp47() {
        PropertiesFileType propertiesFileType = new PropertiesFileType();
        Matcher matcher = propertiesFileType.getTargetFilePattern().getPattern().matcher("/source/filefinder_fr-FR.properties");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("fr-FR", matcher.group(LOCALE));
        Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
        Assert.assertEquals("filefinder", matcher.group(BASE_NAME));
        Assert.assertEquals("properties", matcher.group(FILE_EXTENSION));
    }

}
