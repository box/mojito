package com.box.l10n.mojito.phabricator;

import com.box.l10n.mojito.slack.SlackClientTest;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {PhabricatorClientTest.class, PhabricatorClientConfiguration.class})
@EnableAutoConfiguration
@IntegrationTest("spring.datasource.initialize=false")
public class PhabricatorClientTest {

    @Autowired(required = false)
    PhabricatorClient phabricatorClient;

    @Before
    public void assumeClient() {
        Assume.assumeNotNull(phabricatorClient);
    }

    @Test
    public void testRemoveReviewer() throws PhabricatorClientException {
        phabricatorClient.removeReviewer("PHID-DREV-qburvo22bn5vnibfpebb", "PHID-PROJ-7lraulohbhrqynqgzv4z", false);
    }

    @Test
    public void testaddReviewer() throws PhabricatorClientException {
        phabricatorClient.addReviewer("PHID-DREV-qburvo22bn5vnibfpebb", "PHID-PROJ-7lraulohbhrqynqgzv4z", true);
    }

    @Test
    public void testAddComment() throws PhabricatorClientException {
        phabricatorClient.addComment("PHID-DREV-qburvo22bn5vnibfpebb", "test comment");
    }
}