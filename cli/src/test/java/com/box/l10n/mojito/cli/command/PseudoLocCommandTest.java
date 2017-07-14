package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.entity.Asset;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

    @Autowired
    AssetClient assetClient;

    @Test
    public void pseudo() throws Exception {
        // sample call java -jar target/mojito-cli-0.74-SNAPSHOT.jar pseudo -r demo3 -s /box/mojito/cli/demo3/
        Repository repository = createTestRepoUsingRepoService();
        
         getL10nJCommander().run("push", "-r", repository.getName(),
                "-s", getInputResourcesTestDir("source").getAbsolutePath());
         
        getL10nJCommander().run("pseudo", "-r", repository.getName(),
            "-s", getInputResourcesTestDir("source").getAbsolutePath(),
            "-t", getTargetTestDir("target").getAbsolutePath());


//        getL10nJCommander().run("pull", "-r", repository.getName(),
//            "-s", getInputResourcesTestDir("source").getAbsolutePath(),
//            "-t", getTargetTestDir("target").getAbsolutePath());
//
//        getL10nJCommander().run("pull", "-r", repository.getName(),
//            "-s", getInputResourcesTestDir("source_modified").getAbsolutePath(),
//            "-t", getTargetTestDir("target_modified").getAbsolutePath());

        checkExpectedGeneratedResources();
   }

}
