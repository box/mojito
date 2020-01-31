package com.box.l10n.mojito.phabricator;

import com.box.l10n.mojito.phabricator.payload.Data;
import com.box.l10n.mojito.phabricator.payload.RevisionSearchFields;
import com.box.l10n.mojito.test.IOTestBase;
import com.box.l10n.mojito.test.TestWithEnableAutoConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {
        TestWithEnableAutoConfiguration.class,
        PhabricatorConfiguration.class,
        PhabricatorConfigurationProperties.class})
public class PhabricatorTest extends IOTestBase {

    @Autowired(required = false)
    PhabricatorHttpClient phabricatorHttpClient;

    /**
     * test.l10n.phabricator.phidMap={"PHID-HMBT-sometest":"PHID-HMBT-oij",
     * "PHID-HMBD-sometest":"PHID-HMBD-oij", "PHID-HMBB-sometest":"PHID-HMBB-oij",
     * "PHID-DIFF-sometest":"PHID-DIFF-oij", "PHID-DREV-sometest":"PHID-DREV-oij"}
     */
    @Value("#{${test.l10n.phabricator.phidMap:null}}")
    Map<String, String> phidMap = new HashMap<>();

    @Test
    public void getRevisionForTargetPhid() {
        StubPhabricatorHttpClient stubPhabricatorHttpClient = new StubPhabricatorHttpClient(getInputResourcesTestDir(), phabricatorHttpClient, phidMap);
        Phabricator phabricator = new Phabricator(
                new DifferentialDiff(stubPhabricatorHttpClient),
                new Harbormaster(stubPhabricatorHttpClient),
                new DifferentialRevision(stubPhabricatorHttpClient));

        Data<RevisionSearchFields> revisionSearchFieldsData = phabricator.getRevisionForTargetPhid("PHID-HMBT-sometest");
        assertEquals(49, (long) revisionSearchFieldsData.getId());
    }

}