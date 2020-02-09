package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.evolve.Evolve;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class EvolveCommandTest extends CLITestBase {

    static Logger logger = LoggerFactory.getLogger(EvolveCommandTest.class);

    @Autowired(required = false)
    Evolve evolve;

    @Before
    public void before() {
        Assume.assumeNotNull(evolve);
    }

    @Test
    public void execute() throws Exception {
        Repository repository = createTestRepoUsingRepoService();
        getL10nJCommander().run("evolve-sync", "-r", repository.getName());
    }

}