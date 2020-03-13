package com.box.l10n.mojito.phabricator;

import com.box.l10n.mojito.test.IOTestBase;
import com.box.l10n.mojito.test.TestWithEnableAutoConfiguration;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {
        TestWithEnableAutoConfiguration.class,
        PhabricatorConfiguration.class,
        PhabricatorConfigurationProperties.class})
public class DifferentialRevisionTest extends IOTestBase {

    @Autowired(required = false)
    DifferentialRevision differentialRevision;

    @Value("${test.l10n.phabricator.differentialrevision.to:}")
    String objectIdentifier;

    @Value("${test.l10n.phabricator.differentialrevision.reviewer:}")
    String reviewer;

    @Test
    public void optionalWrapWithBlocking() {
        DifferentialRevision differentialRevision = new DifferentialRevision(null);
        String optionalWrapWithBlocking = differentialRevision.optionalWrapWithBlocking("testvalue", true);
        assertEquals("blocking(testvalue)", optionalWrapWithBlocking);
    }

    @Test
    public void testRemoveReviewer() throws PhabricatorException {
        Assume.assumeNotNull(differentialRevision, objectIdentifier, reviewer);
        differentialRevision.removeReviewer(objectIdentifier, reviewer, false);
    }

    @Test
    public void testaddReviewer() throws PhabricatorException {
        Assume.assumeNotNull(differentialRevision, objectIdentifier, reviewer);
        differentialRevision.addReviewer(objectIdentifier, reviewer, true);
    }

    @Test
    public void testAddComment() throws PhabricatorException {
        Assume.assumeNotNull(differentialRevision, objectIdentifier);
        differentialRevision.addComment(objectIdentifier, "test comment with **bold** and \n new line");
    }
}
