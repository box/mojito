package com.box.l10n.mojito.rest.cli;

import com.box.l10n.mojito.rest.WSTestBase;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.test.TestIdWatcher;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jaurambault
 */
public class CliWSTest extends WSTestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(CliWSTest.class);

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Autowired
    CliWS cliWS;
    
    @Test
    public void testURLDefault() throws RepositoryNameAlreadyUsedException {
        String url = cliWS.getUrl();
        Assert.assertEquals("https://github.com/box/mojito/releases/download/v" + cliWS.version + "/mojito-cli-" + cliWS.version + ".jar", url);
    }

    @Test
    public void testURL() {
        CliWS cws = new CliWS();
        cws.version = "0.1";
        cws.cliUrl = "http://someserver.io/{version}/{gitCommit}|{gitShortCommit}";
        cws.gitInfo = new GitInfo();
        cws.gitInfo.getCommit().setId("141708fc7e80556d69261c2cf4cdc82acfa337bc");
        Assert.assertEquals("http://someserver.io/0.1/141708fc7e80556d69261c2cf4cdc82acfa337bc|141708f", cws.getUrl());
    }
}
