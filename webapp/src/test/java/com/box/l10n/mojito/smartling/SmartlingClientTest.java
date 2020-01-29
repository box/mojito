package com.box.l10n.mojito.smartling;

import com.box.l10n.mojito.smartling.request.Binding;
import com.box.l10n.mojito.smartling.request.Bindings;
import com.box.l10n.mojito.smartling.response.AuthenticationResponse;
import com.box.l10n.mojito.smartling.response.File;
import com.box.l10n.mojito.smartling.response.Items;
import com.box.l10n.mojito.smartling.response.StringInfo;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.stream.Stream;

@RunWith(SpringJUnit4ClassRunner.class)
@EnableAutoConfiguration
@SpringBootTest(properties = "spring.datasource.initialize=false",
        classes = {SmartlingClientTest.class, SmartlingClientConfiguration.class, ObjectMapper.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class SmartlingClientTest {

    static Logger logger = LoggerFactory.getLogger(SmartlingClient.class);

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Autowired(required = false)
    SmartlingClient smartlingClient;

    @Value("${test.l10n.smartling.project-id:#{null}}")
    String projectId = null;

    @Value("${test.l10n.smartling.file-uri:#{null}}")
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
        Items<StringInfo> sourceStrings = smartlingClient.getSourceStrings(
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
    public void testUploadFile() {
        Assume.assumeNotNull(projectId);
        uploadFile(projectId, "strings.xml");
    }

    private void uploadFile(String projectId, String fileName) {
        smartlingClient.uploadFile(projectId,
                fileName,
                "android",
                "<resources>\n" +
                        "    <string name=\"hello\">Hello</string>\n" +
                        "    <string name=\"bye\">Bye</string>\n" +
                        "</resources>\n",
                null,
                null);
    }

    @Test
    public void testUploadDownloadAndDeleteFile() {
        Assume.assumeNotNull(projectId);
        String fileName = testIdWatcher.getEntityName("") + "-string.xml";

        uploadFile(projectId, fileName);
        try {
            String result = smartlingClient.downloadFile(projectId,
                    "fr-FR",
                    fileName,
                    false,
                    SmartlingClient.RetrievalType.PUBLISHED);

            String expected =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<resources>\n";
            Assert.assertEquals(expected, result.substring(0, expected.length()));
        } finally {
            smartlingClient.deleteFile(projectId, fileName);
        }
    }

    @Test
    public void testUploadContext() throws IOException {
        Assume.assumeNotNull(projectId);
        ClassPathResource classPathResource = new ClassPathResource("/com/box/l10n/mojito/img/1.png");
        byte[] content = ByteStreams.toByteArray(classPathResource.getInputStream());
        smartlingClient.uploadContext(projectId, "image1.png", content);
    }

    @Test
    public void testGetFiles() {
        Assume.assumeNotNull(projectId);
        Items<File> files = smartlingClient.getFiles(projectId);
        files.getItems().stream().forEach(
                f -> logger.debug(f.getFileUri())
        );
    }

    @Test
    public void testGetStringInfosFromFile() {
        Assume.assumeNotNull(projectId);
        Items<File> files = smartlingClient.getFiles(projectId);
        Stream<StringInfo> stringInfosFromFiles = files.getItems().stream().flatMap(
                file -> smartlingClient.getStringInfos(projectId, file.getFileUri()));
        stringInfosFromFiles.forEach(
                stringInfo -> logger.debug(stringInfo.getHashcode())
        );
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

    @Test
    public void bindingsSerialization() throws JsonProcessingException {
        Bindings bindings = new Bindings();
        Binding binding = new Binding();
        binding.setContextUid("c1");
        binding.setStringHashcode("h1");
        bindings.getBindings().add(binding);

        Binding binding2 = new Binding();
        binding2.setContextUid("c1");
        binding2.setStringHashcode("h1");
        bindings.getBindings().add(binding2);

        ObjectMapper objectMapper = new ObjectMapper();
        String str = objectMapper.writeValueAsString(bindings);
        Assert.assertEquals("{\"bindings\":[{\"contextUid\":\"c1\",\"stringHashcode\":\"h1\"},{\"contextUid\":\"c1\",\"stringHashcode\":\"h1\"}]}", str);
    }
}