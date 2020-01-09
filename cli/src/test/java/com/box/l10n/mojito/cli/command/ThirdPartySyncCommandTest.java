package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.client.ThirdPartySync;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ThirdPartySyncCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ThirdPartySyncCommandTest.class);

    @Test
    public void execute() throws Exception {
        String repoName = testIdWatcher.getEntityName("thirdpartysync_execute");
        Repository repository = repositoryService.createRepository(repoName, repoName + " description", null, false);
        getL10nJCommander().run("thirdparty-sync", "-r", repository.getName(), "-p", "does-not-matter-yet", "-ps", "\" _\"");

        String outputString = outputCapture.toString();
        assertTrue(outputString.contains(Arrays.asList(ThirdPartySync.Action.MAP_TEXTUNIT, ThirdPartySync.Action.PUSH_SCREENSHOT).toString()));
    }

}
