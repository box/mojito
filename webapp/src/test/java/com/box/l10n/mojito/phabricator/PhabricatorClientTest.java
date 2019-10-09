package com.box.l10n.mojito.phabricator;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@EnableAutoConfiguration
@SpringBootTest(classes = {PhabricatorClientTest.class, PhabricatorClientConfiguration.class},
        properties = "spring.datasource.initialize=false")
public class PhabricatorClientTest {

    @Autowired(required = false)
    PhabricatorClient phabricatorClient;

    @Before
    public void assumeClient() {
        Assume.assumeNotNull(phabricatorClient);
    }

    @Test
    public void testRemoveReviewer() throws PhabricatorClientException {
        phabricatorClient.removeReviewer("D401119", "PHID-PROJ-7lraulohbhrqynqgzv4z", false);
    }

    @Test
    public void testaddReviewer() throws PhabricatorClientException {
        phabricatorClient.addReviewer("D401119", "PHID-PROJ-7lraulohbhrqynqgzv4z", true);
    }

    @Test
    public void testAddComment() throws PhabricatorClientException {
        phabricatorClient.addComment("D401119", "test comment with **bold** and \n new line");
    }
}