package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.google.common.base.Joiner;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaurambault
 */
public class ReposLocalesUpdateCommandTest extends CLITestBase {
    
    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ReposLocalesUpdateCommandTest.class);
    
    @Test
    public void testUpdateAllWithNameFilter() throws Exception {
        Repository repository = createTestRepoUsingRepoService();
        String testRepoName = repository.getName();
        
        Repository repository2 = createTestRepoUsingRepoService("repo2");
        String testRepoName2 = repository2.getName();

        
        Repository repository3 = createTestRepoUsingRepoService("repo3");
        String testRepoName3 = repository3.getName();
        
        getL10nJCommander().run(
                "repos-locales-update",
                "-l", "fr-FR,ko-KR,ja-JP",
                "-rns", Joiner.on(",").join(testRepoName, testRepoName3)
        );

        assertFalse("Repositories not updated correctly", outputCapture.toString().contains("- updated repository: " + testRepoName2));
        assertTrue("Repositories not updated correctly", outputCapture.toString().contains("- updated repository: " + testRepoName));
        assertTrue("Repositories not update correctly", outputCapture.toString().contains("- updated repository: " + testRepoName3));
    }

}
