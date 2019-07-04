package com.box.l10n.mojito.smartling;

import com.box.l10n.mojito.smartling.response.AuthenticationResponse;
import com.box.l10n.mojito.smartling.response.SourceStrings;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
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

import java.io.IOException;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {SmartlingClientTest.class, SmartlingClientConfiguration.class, ObjectMapper.class})
@EnableAutoConfiguration
@IntegrationTest("spring.datasource.initialize=false")
public class SmartlingClientTest {

    static Logger logger = LoggerFactory.getLogger(SmartlingClient.class);

    @Autowired(required = false)
    SmartlingClient smartlingClient;

    @Value("${test.l10n.smartling.projectId:#{null}}")
    String projectId = null;

    @Value("${test.l10n.smartling.fileUri:#{null}}")
    String fileUri = null;

    @Before
    public void init() {
        Assume.assumeNotNull(smartlingClient);
    }

    @Test
    public void testGetSourceStrings() throws SmartlingClientException {
        Assume.assumeNotNull(projectId);
        Assume.assumeNotNull(fileUri);

        logger.debug("Test getSourceStrings");
        SourceStrings sourceStrings = smartlingClient.getSourceStrings(
                projectId,
                fileUri,
                0,
                500);

        sourceStrings.getItems().stream().map(item -> item.getHashcode())
                .forEach(logger::debug);
    }

    @Test
    public void testGetSourceStringsStream() {
        Assume.assumeNotNull(projectId);
        Assume.assumeNotNull(fileUri);

        smartlingClient.getStringInfos(projectId, fileUri).forEach(stringInfo -> {
            if (stringInfo.getKeys().size() == 1) {
                logger.debug("hashcode: {}\nvariant: {}\nparsed string: {}\n stringtext: {}\nkeys:",
                        stringInfo.getHashcode(),
                        stringInfo.getStringVariant(),
                        stringInfo.getParsedStringText(),
                        stringInfo.getStringText());

                stringInfo.getKeys().stream().forEach(key -> logger.debug("key: {}, file: {}", key.getKey(), key.getFileUri()));

                logger.debug("\n\n");
            }
        });
    }

    @Test
    public void unwrapRootValueToDeserialize() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);

        String response = "{\n" +
                "  \"response\": {\n" +
                "    \"code\": \"SUCCESS\",\n" +
                "    \"data\": {\n" +
                "      \"accessToken\": \"b816424c-2e95-11e7-93ae-92361f002671\",\n" +
                "      \"expiresIn\": 480,\n" +
                "      \"refreshExpiresIn\": 3660,\n" +
                "      \"refreshToken\": \"c0a6f410-2e95-11e7-93ae-92361f002671\",\n" +
                "      \"tokenType\": \"Bearer\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        AuthenticationResponse authenticationResponse = objectMapper.readValue(response, AuthenticationResponse.class);
        Assert.assertEquals("b816424c-2e95-11e7-93ae-92361f002671", authenticationResponse.getData().getAccessToken());
    }
}