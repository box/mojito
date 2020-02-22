package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AssertionErrors;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author jeanaurambault
 */
public class ExtractionDiffCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ExtractionDiffCommandTest.class);

    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Test
    public void extractDiff() throws Exception {
        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("extract",
                "-s", getInputResourcesTestDir("source1").getAbsolutePath(),
                "-o", getTargetTestDir("extractions").getAbsolutePath(),
                "-n", "source1",
                "-fo", "sometestoption=value1");

        getL10nJCommander().run("extract",
                "-s", getInputResourcesTestDir("source2").getAbsolutePath(),
                "-o", getTargetTestDir("extractions").getAbsolutePath(),
                "-n", "source2",
                "-fo", "sometestoption=value1");

        getL10nJCommander().run("extract",
                "-s", getInputResourcesTestDir("source3").getAbsolutePath(),
                "-o", getTargetTestDir("extractions").getAbsolutePath(),
                "-n", "source3",
                "-fo", "sometestoption=value1");

        getL10nJCommander().run("extract",
                "-s", getInputResourcesTestDir("source4").getAbsolutePath(),
                "-o", getTargetTestDir("extractions").getAbsolutePath(),
                "-n", "source4",
                "-fo", "sometestoption=value1");

        getL10nJCommander().run("extract-diff",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source1",
                "-b", "source1",
                "--push-to", repository.getName());

        AssertionErrors.assertTrue("there shouldn't be any text units", getTextUnitDTOS(repository).isEmpty());

        getL10nJCommander().run("extract-diff",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source2",
                "-b", "source1",
                "--push-to", repository.getName());

        AssertionErrors.assertTrue("there shouldn't be any text units", getTextUnitDTOS(repository).isEmpty());

        getL10nJCommander().run("extract-diff",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source3",
                "-b", "source1",
                "--push-to", repository.getName());

        List<TextUnitDTO> textUnitDTOS = getTextUnitDTOS(repository);
        assertEquals(1L, textUnitDTOS.size());
        assertEquals("1 day update --- 1_day_duration", textUnitDTOS.get(0).getName());
        assertEquals("1 day update", textUnitDTOS.get(0).getSource());
        assertEquals("File lock dialog duration", textUnitDTOS.get(0).getComment());
        assertEquals("LC_MESSAGES/messages.pot", textUnitDTOS.get(0).getAssetPath());

        getL10nJCommander().run("extract-diff",
                "-i", getTargetTestDir("extractions").getAbsolutePath(),
                "-o", getTargetTestDir("extraction-diffs").getAbsolutePath(),
                "-c", "source4",
                "-b", "source1",
                "--push-to", repository.getName());

        textUnitDTOS = getTextUnitDTOS(repository);
        assertEquals(8L, textUnitDTOS.size());
        assertEquals("1 day update --- 1_day_duration", textUnitDTOS.get(0).getName());
        assertEquals("1 day update", textUnitDTOS.get(0).getSource());
        assertEquals("File lock dialog duration", textUnitDTOS.get(0).getComment());
        assertEquals("LC_MESSAGES/messages.pot", textUnitDTOS.get(0).getAssetPath());

        assertEquals("100 character description: --- 100_character_description_", textUnitDTOS.get(1).getName());
        assertEquals("100 character description:", textUnitDTOS.get(1).getSource());
        assertEquals(null, textUnitDTOS.get(1).getComment());
        assertEquals("LC_MESSAGES/messages2.pot", textUnitDTOS.get(1).getAssetPath());

        assertEquals("15 min --- 15_min_duration", textUnitDTOS.get(2).getName());
        assertEquals("15 min", textUnitDTOS.get(2).getSource());
        assertEquals("File lock dialog duration", textUnitDTOS.get(2).getComment());
        assertEquals("LC_MESSAGES/messages2.pot", textUnitDTOS.get(2).getAssetPath());

        assertEquals("1 day --- 1_day_duration", textUnitDTOS.get(3).getName());
        assertEquals("1 day", textUnitDTOS.get(3).getSource());
        assertEquals("File lock dialog duration", textUnitDTOS.get(3).getComment());
        assertEquals("LC_MESSAGES/messages2.pot", textUnitDTOS.get(3).getAssetPath());

        assertEquals("1 hour --- 1_hour_duration", textUnitDTOS.get(4).getName());
        assertEquals("1 hour", textUnitDTOS.get(4).getSource());
        assertEquals("File lock dialog duration", textUnitDTOS.get(4).getComment());
        assertEquals("LC_MESSAGES/messages2.pot", textUnitDTOS.get(4).getAssetPath());

        assertEquals("1 month --- 1_month_duration", textUnitDTOS.get(5).getName());
        assertEquals("1 month", textUnitDTOS.get(5).getSource());
        assertEquals("File lock dialog duration", textUnitDTOS.get(5).getComment());
        assertEquals("LC_MESSAGES/messages2.pot", textUnitDTOS.get(5).getAssetPath());

        assertEquals("There is {number} car --- car _one", textUnitDTOS.get(6).getName());
        assertEquals("There is {number} car", textUnitDTOS.get(6).getSource());
        assertEquals("Test plural", textUnitDTOS.get(6).getComment());
        assertEquals("one", textUnitDTOS.get(6).getPluralForm());
        assertEquals("LC_MESSAGES/messages2.pot", textUnitDTOS.get(6).getAssetPath());

        assertEquals("There is {number} car --- car _other", textUnitDTOS.get(7).getName());
        assertEquals("There are {number} cars", textUnitDTOS.get(7).getSource());
        assertEquals("Test plural", textUnitDTOS.get(7).getComment());
        assertEquals("other", textUnitDTOS.get(7).getPluralForm());
        assertEquals("LC_MESSAGES/messages2.pot", textUnitDTOS.get(7).getAssetPath());

        checkExpectedGeneratedResources();
    }

    @Test
    public void getDiffExtractionNameOrDefault() {
        ExtractionDiffCommand extractionDiffCommand = new ExtractionDiffCommand();
        extractionDiffCommand.currentExtractionName = "current";
        extractionDiffCommand.baseExtractionName = "base";
        String diffExtractionNameOrDefault = extractionDiffCommand.getDiffExtractionNameOrDefault();
        Assert.assertEquals("current_base", diffExtractionNameOrDefault);
    }

    @Test
    public void getDiffExtractionNameOrDefaultWithName() {
        ExtractionDiffCommand extractionDiffCommand = new ExtractionDiffCommand();
        extractionDiffCommand.extractionDiffName = "edf";
        String diffExtractionNameOrDefault = extractionDiffCommand.getDiffExtractionNameOrDefault();
        Assert.assertEquals("edf", diffExtractionNameOrDefault);
    }

    List<TextUnitDTO> getTextUnitDTOS(Repository repository) {
        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repository.getId());
        textUnitSearcherParameters.setForRootLocale(true);
        return textUnitSearcher.search(textUnitSearcherParameters).stream()
                .sorted(Comparator.comparing(TextUnitDTO::getTmTextUnitVariantId))
                .collect(Collectors.toList());
    }
}
