package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.entity.Locale;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class ThirdPartySyncCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ThirdPartySyncCommandTest.class);

    @Test
    public void execute() throws Exception {
        String repoName = testIdWatcher.getEntityName("thirdpartysync_execute");
        Repository repository = repositoryService.createRepository(repoName, repoName + " description", null, false);
        getL10nJCommander().run("thirdparty-sync", "-r", repository.getName(), "-p", "does-not-matter-yet");
    }

}
