package com.box.l10n.mojito.smartling;

import com.box.l10n.mojito.smartling.response.SourceStringsResponse;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {SmartlingClientTest.class, SmartlingClientConfiguration.class, SmartlingClient.class})
@EnableAutoConfiguration
@IntegrationTest("spring.datasource.initialize=false")
public class SmartlingClientTest {

    static Logger logger = LoggerFactory.getLogger(SmartlingClient.class);

    @Autowired
    SmartlingClient smartlingClient;

    @Value("${l10n.smartling.clientID:#{null}}")
    String clientID;

    @Value("${l10n.smartling.clientSecret:#{null}")
    String clientSecret;

    @Value("${test.l10n.smartling.projectId:#{null}}")
    String projectId = null;

    @Value("${test.l10n.smartling.fileUri:#{null}}")
    String fileUri = null;

    @Before
    public void init() {
        Assume.assumeNotNull(clientID);
        Assume.assumeNotNull(clientSecret);
    }

    @Test
    public void testGetSourceStrings() throws Exception {
        Assume.assumeNotNull(projectId);
        Assume.assumeNotNull(fileUri);

        for (int i = 0; i < 3; i++) {
            logger.debug("Test getSourceStrings");
            SourceStringsResponse sourceStrings = smartlingClient.getSourceStrings(
                    projectId,
                    fileUri,
                    0);
            sourceStrings.getResponse().getData().getItems().stream().map(item -> item.getHashcode())
                    .forEach(logger::debug);
            Thread.sleep(4000L);
        }
    }
}