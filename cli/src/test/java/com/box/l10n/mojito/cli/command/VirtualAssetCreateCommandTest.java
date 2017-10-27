package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VirtualAssetCreateCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(VirtualAssetCreateCommandTest.class);

    @Autowired
    RepositoryClient repositoryClient; 

    @Test
    public void execute() throws Exception {
        Repository repository = createTestRepoUsingRepoService();        
        getL10nJCommander().run("virtual-asset-create", "-r", repository.getName(), "-p", "asset1");
        getL10nJCommander().run("virtual-asset-create", "-r", repository.getName(), "-p", "asset2");
    }
        
}
