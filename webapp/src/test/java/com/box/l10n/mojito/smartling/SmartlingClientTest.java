package com.box.l10n.mojito.smartling;

import com.box.l10n.mojito.smartling.response.SourceStringsResponse;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {SmartlingClientTest.class})
@EnableAutoConfiguration
@IntegrationTest("spring.datasource.initialize=false")
public class SmartlingClientTest {

    @Value("${l10n.smartling.userIdentifier:#{null}}")
    String userIdentifier;

    @Value("${l10n.smartling.userSecret:#{null}}")
    String userSecret;

    @Value("${test.l10n.smartling.projectId:abc123}")
    String projectId;

    @Value("${test.l10n.smartling.file:demo.properties}")
    String file;

    @Value("${test.l10n.smartling.offset:0}")
    Integer offset;

    @Test
    public void testClient() throws SmartlingClientException {
        Assume.assumeNotNull(userIdentifier, userSecret);
        SmartlingClient smartlingClient = new SmartlingClient(userIdentifier, userSecret);
        SourceStringsResponse sourceStringsResponse = smartlingClient.getSourceStrings(projectId, file, offset);
    }

}