package com.box.l10n.mojito.cli.filefinder.file;

import com.box.l10n.mojito.cli.filefinder.FilePattern;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.service.NormalizationUtils;
import org.junit.Assert;
import org.junit.Test;

import javax.validation.constraints.AssertTrue;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static com.box.l10n.mojito.cli.filefinder.FilePattern.*;

/**
 *
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
        Matcher matcher = jsonFileType.getTargetFilePattern().getPattern().matcher("/source/filefinder_fr.json");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("fr", matcher.group(LOCALE));
        Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
        Assert.assertEquals("filefinder", matcher.group(BASE_NAME));
        Assert.assertEquals("json", matcher.group(FILE_EXTENSION));
    }

    @Test
    public void testTargetPatternBcp47() {
        JSONFileType jsonFileType = new JSONFileType();
        Matcher matcher = jsonFileType.getTargetFilePattern().getPattern().matcher("/source/filefinder_fr-FR.json");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("fr-FR", matcher.group(LOCALE));
        Assert.assertEquals("/source/", matcher.group(PARENT_PATH));
        Assert.assertEquals("filefinder", matcher.group(BASE_NAME));
        Assert.assertEquals("json", matcher.group(FILE_EXTENSION));
    }
}
