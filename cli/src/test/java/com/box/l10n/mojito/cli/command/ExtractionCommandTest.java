package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jeanaurambault
 */
public class ExtractionCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ExtractionCommandTest.class);

    @Test
    public void extract() throws Exception {
        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("extract",
                "-s", getInputResourcesTestDir("source1").getAbsolutePath(),
                "-o" , getTargetTestDir().getAbsolutePath(),
                "-fo", "testoption=something",
                "-n", "source1");

        checkExpectedGeneratedResources();
    }

}
