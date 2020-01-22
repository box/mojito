package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jeanaurambault
 */
public class ExtractionDiffCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ExtractionDiffCommandTest.class);

    @Test
    public void extractDiff() throws Exception {
        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("extract",
                "-s", getInputResourcesTestDir("source1").getAbsolutePath(),
                "-o", getTargetTestDir().getAbsolutePath(),
                "-n", "source1");

        getL10nJCommander().run("extract",
                "-s", getInputResourcesTestDir("source2").getAbsolutePath(),
                "-o", getTargetTestDir().getAbsolutePath(),
                "-n", "source2");

        getL10nJCommander().run("extract",
                "-s", getInputResourcesTestDir("source3").getAbsolutePath(),
                "-o", getTargetTestDir().getAbsolutePath(),
                "-n", "source3");

        getL10nJCommander().run("extract",
                "-s", getInputResourcesTestDir("source4").getAbsolutePath(),
                "-o", getTargetTestDir().getAbsolutePath(),
                "-n", "source4");

        getL10nJCommander().run("extract-diff",
                "-o", getTargetTestDir().getAbsolutePath(),
                "-n", "source1",
                "-w", "source1",
                "-df", "diff_1_1.json");

        getL10nJCommander().run("extract-diff",
                "-o", getTargetTestDir().getAbsolutePath(),
                "-n", "source2",
                "-w", "source1",
                "-df", "diff_2_1.json");

        getL10nJCommander().run("extract-diff",
                "-o", getTargetTestDir().getAbsolutePath(),
                "-n", "source3",
                "-w", "source1",
                "-df", "diff_3_1.json");

        getL10nJCommander().run("extract-diff",
                "-o", getTargetTestDir().getAbsolutePath(),
                "-n", "source4",
                "-w", "source1",
                "-df", "diff_4_1.json");

        checkExpectedGeneratedResources();
    }
}
