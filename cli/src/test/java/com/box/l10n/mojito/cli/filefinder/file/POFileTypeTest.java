package com.box.l10n.mojito.cli.filefinder.file;

import com.box.l10n.mojito.cli.filefinder.FilePattern;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.BASE_NAME;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.FILE_EXTENSION; 
import static com.box.l10n.mojito.cli.filefinder.FilePattern.LOCALE;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.PARENT_PATH;
import static com.box.l10n.mojito.cli.filefinder.FilePattern.SUB_PATH;
import java.util.regex.Matcher;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author jaurambault
 */
public class POFileTypeTest {

    @Test
    public void testSourcePattern() {
        POFileType potFileType = new POFileType();
        FilePattern sourceFilePattern = potFileType.getSourceFilePattern();
        Matcher matcher = sourceFilePattern.getPattern().matcher("/source/messages.pot");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
        Assert.assertEquals("messages", matcher.group(BASE_NAME));
        Assert.assertEquals("pot", matcher.group(FILE_EXTENSION));
    }

    @Test
    public void testSourcePatternNoMatch() {
        POFileType potFileType = new POFileType();
        FilePattern sourceFilePattern = potFileType.getSourceFilePattern();
        Matcher matcher = sourceFilePattern.getPattern().matcher("/source/fr_FR/LC_MESSAGES/messages.po");
        Assert.assertFalse(matcher.matches());
    }

    @Test
    public void testTargetPattern() {
        POFileType potFileType = new POFileType();
        Matcher matcher = potFileType.getTargetFilePattern().getPattern().matcher("/source/fr_FR/LC_MESSAGES/messages.po");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("fr_FR", matcher.group(LOCALE));
        Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
        Assert.assertEquals("messages", matcher.group(BASE_NAME));
        Assert.assertEquals("po", matcher.group(FILE_EXTENSION));
    }

    @Test
    public void testTargetPatternNoMatch() {
        POFileType potFileType = new POFileType();
        Matcher matcher = potFileType.getTargetFilePattern().getPattern().matcher("/source/messages.po");
        Assert.assertFalse(matcher.matches());
    }

}
