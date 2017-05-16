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
public class CSVFileTypeTest {

    @Test
    public void testSourcePattern() {
        CSVFileType csvFileType = new CSVFileType();
        csvFileType.getLocaleType().setSourceLocale("en-US");
        FilePattern sourceFilePattern = csvFileType.getSourceFilePattern();
        Matcher matcher = sourceFilePattern.getPattern().matcher("/source/demo.csv");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
        Assert.assertEquals("demo", matcher.group(BASE_NAME));
        Assert.assertEquals("csv", matcher.group(FILE_EXTENSION));

    }

    @Test
    public void testTargetPattern() {
        CSVFileType csvFileType = new CSVFileType();
        Matcher matcher = csvFileType.getTargetFilePattern().getPattern().matcher("/source/demo_fr.csv");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("fr", matcher.group(LOCALE));
        Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
        Assert.assertEquals("demo", matcher.group(BASE_NAME));
        Assert.assertEquals("csv", matcher.group(FILE_EXTENSION));
    }

    @Test
    public void testTargetPatternBcp47() {
        CSVFileType csvFileType = new CSVFileType();
        Matcher matcher = csvFileType.getTargetFilePattern().getPattern().matcher("/source/demo_en-GB.csv");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("en-GB", matcher.group(LOCALE));
        Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
        Assert.assertEquals("demo", matcher.group(BASE_NAME));
        Assert.assertEquals("csv", matcher.group(FILE_EXTENSION));
    }
}
