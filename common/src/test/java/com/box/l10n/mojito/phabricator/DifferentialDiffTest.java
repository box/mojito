package com.box.l10n.mojito.phabricator;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.phabricator.payload.ObjectResult;
import com.box.l10n.mojito.phabricator.payload.QueryDiffsFields;
import com.box.l10n.mojito.test.IOTestBase;
import com.box.l10n.mojito.test.TestWithEnableAutoConfiguration;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        TestWithEnableAutoConfiguration.class,
        PhabricatorConfiguration.class,
        PhabricatorConfigurationProperties.class})
public class DifferentialDiffTest extends IOTestBase {

    static Logger logger = LoggerFactory.getLogger(DifferentialDiff.class);

    @Autowired(required = false)
    PhabricatorHttpClient phabricatorHttpClient;

    @Value("${test.l10n.phabricator.differentialdiff.diffId:}")
    String diffId;

    @Test
    public void queryDiff() {
        Assume.assumeNotNull(phabricatorHttpClient, diffId);
        DifferentialDiff differentialDiff = new DifferentialDiff(phabricatorHttpClient);
        QueryDiffsFields revisionPHID = differentialDiff.queryDiff(diffId);
        QueryDiffsFields queryDiffsFields = differentialDiff.queryDiff(diffId);
        logger.debug("queryDiff, revision: {}, base commit: {}", queryDiffsFields.getRevisionId(), queryDiffsFields.getSourceControlBaseRevision());
    }

    @Test
    public void justCallAPI() {
        Assume.assumeNotNull(phabricatorHttpClient, diffId);
        ObjectResult objectResult = phabricatorHttpClient.postEntityAndCheckResponse(
                Method.DIFFERENTIAL_QUERYDIFFS,
                phabricatorHttpClient.withId(diffId),
                ObjectResult.class);
        logger.debug("{}", new ObjectMapper().writeValueAsStringUnchecked(objectResult));
    }
}
