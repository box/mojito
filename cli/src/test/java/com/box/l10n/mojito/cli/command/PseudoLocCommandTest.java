package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 */
public class PseudoLocCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(PseudoLocCommandTest.class);

    @Autowired
    RepositoryClient repositoryClient;

    @Test
    public void pseudo() throws Exception {

        Repository repository = createTestRepoUsingRepoService();

        getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());

        getL10nJCommander().run("pseudo", "-r", repository.getName(),
            "-s", getInputResourcesTestDir("source").getAbsolutePath(),
            "-t", getTargetTestDir("target").getAbsolutePath());

        checkExpectedGeneratedResources();
   }
}
