package com.box.l10n.mojito.smartling;

import static org.assertj.core.api.Assertions.assertThat;

import com.box.l10n.mojito.smartling.request.Binding;
import com.box.l10n.mojito.smartling.request.Bindings;
import com.box.l10n.mojito.smartling.response.*;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
      SmartlingClientTest.class,
      SmartlingClientConfiguration.class,
      ObjectMapper.class,
      SmartlingTestConfig.class
    })
@EnableConfigurationProperties
public class SmartlingClientTest {

  static Logger logger = LoggerFactory.getLogger(SmartlingClient.class);

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired(required = false)
  SmartlingClient smartlingClient;

  @Autowired SmartlingTestConfig smartlingTestConfig;

  @Mock OAuth2RestTemplate mockedOAuth2RestTemplate;

  @Before
  public void init() {
    Assume.assumeNotNull(smartlingClient);
  }

  @Test
  public void testGetSourceStrings() throws SmartlingClientException {
    Assume.assumeNotNull(smartlingTestConfig.projectId);
    Assume.assumeNotNull(smartlingTestConfig.fileUri);

    logger.debug("Test getSourceStrings");
    Items<StringInfo> sourceStrings =
        smartlingClient.getSourceStrings(
            smartlingTestConfig.projectId, smartlingTestConfig.fileUri, 0, 500);

    sourceStrings.getItems().stream().map(item -> item.getHashcode()).forEach(logger::debug);
  }

  @Test
  public void testGetSourceStringsStream() {
    Assume.assumeNotNull(smartlingTestConfig.projectId);
    Assume.assumeNotNull(smartlingTestConfig.fileUri);

    smartlingClient
        .getStringInfos(smartlingTestConfig.projectId, smartlingTestConfig.fileUri)
        .forEach(
            stringInfo -> {
              if (stringInfo.getKeys().size() == 1) {
                logger.debug(
                    "hashcode: {}\nvariant: {}\nparsed string: {}\n stringtext: {}\nkeys:",
                    stringInfo.getHashcode(),
                    stringInfo.getStringVariant(),
                    stringInfo.getParsedStringText(),
                    stringInfo.getStringText());

                stringInfo.getKeys().stream()
                    .forEach(
                        key -> logger.debug("key: {}, file: {}", key.getKey(), key.getFileUri()));

                logger.debug("\n\n");
              }
            });
  }

  @Ignore
  public void testRefreshToken() throws InterruptedException {
    Assume.assumeNotNull(smartlingTestConfig.projectId);
    Assume.assumeNotNull(smartlingTestConfig.fileUri);

    smartlingClient
        .getStringInfos(smartlingTestConfig.projectId, smartlingTestConfig.fileUri)
        .forEach(
            stringInfo -> {
              if (stringInfo.getKeys().size() == 1) {
                logger.debug(
                    "hashcode: {}\nvariant: {}\nparsed string: {}\n stringtext: {}\nkeys:",
                    stringInfo.getHashcode(),
                    stringInfo.getStringVariant(),
                    stringInfo.getParsedStringText(),
                    stringInfo.getStringText());

                stringInfo.getKeys().stream()
                    .forEach(
                        key -> logger.debug("key: {}, file: {}", key.getKey(), key.getFileUri()));

                logger.debug("\n\n");
              }
            });

    System.out.println("Sleeping until token is almost expired");
    Thread.sleep(460 * 1000L);

    for (int i = 0; i < 350; i++) {
      smartlingClient
          .getStringInfos(smartlingTestConfig.projectId, smartlingTestConfig.fileUri)
          .forEach(
              stringInfo -> {
                if (stringInfo.getKeys().size() == 1) {
                  logger.debug(
                      "hashcode: {}\nvariant: {}\nparsed string: {}\n stringtext: {}\nkeys:",
                      stringInfo.getHashcode(),
                      stringInfo.getStringVariant(),
                      stringInfo.getParsedStringText(),
                      stringInfo.getStringText());

                  stringInfo.getKeys().stream()
                      .forEach(
                          key -> logger.debug("key: {}, file: {}", key.getKey(), key.getFileUri()));

                  logger.debug("\n\n");
                }
              });

      System.out.println("Sleeping for token to expire and get refreshed");
      Thread.sleep(100L);
    }
  }

  @Test
  public void testUploadFileWithWhitespace() {
    Assume.assumeNotNull(smartlingTestConfig.projectId);
    uploadFile(
        smartlingTestConfig.projectId,
        "strings.xml",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>"
            + "<!-- smartling.whitespace_trim=off -->"
            + "<string name=\"bundle.yaml#@#info/description\" tmTextUnitId=\"3511\" xml:space=\"preserve\">Example's REST API</string>"
            + "<string name=\"bundle.yaml#@#tags/description\" tmTextUnitId=\"3512\" xml:space=\"preserve\">       Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n      - Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. \n      - Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. \n      Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</string>"
            + "<string name=\"bundle.yaml#@#tags/description\" tmTextUnitId=\"3513\" xml:space=\"preserve\">1    \n    Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n    - Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. \n    - Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. \n    Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</string>"
            + "<!-- smartling.whitespace_trim=off -->"
            + "<string name=\"bundle.yaml#@#info/title\" tmTextUnitId=\"3514\" xml:space=\"preserve\">       Example      REST           API\n    - Example2      REST           API\n    - Example2      REST           API       </string>"
            + "</resources>");
  }

  @Test
  public void testUploadFile() {
    Assume.assumeNotNull(smartlingTestConfig.projectId);
    uploadFile(smartlingTestConfig.projectId, "strings.xml");
  }

  private void uploadFile(String projectId, String fileName, String fileContent) {
    smartlingClient.uploadFile(projectId, fileName, "android", fileContent, null, null, null);
  }

  private void uploadFile(String projectId, String fileName) {
    uploadFile(
        projectId,
        fileName,
        "<resources>\n"
            + "    <string name=\"hello\">Hello</string>\n"
            + "    <string name=\"bye\">Bye</string>\n"
            + "</resources>\n");
  }

  @Test
  public void testUploadDownloadAndDeleteFile() {
    Assume.assumeNotNull(smartlingTestConfig.projectId);
    String fileName = testIdWatcher.getEntityName("") + "-string.xml";

    uploadFile(smartlingTestConfig.projectId, fileName);
    try {
      String result =
          smartlingClient.downloadFile(
              smartlingTestConfig.projectId,
              "fr-FR",
              fileName,
              false,
              SmartlingClient.RetrievalType.PUBLISHED);

      Assert.assertEquals(
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
              + "<resources>\n"
              + "    \n"
              + "</resources>",
          result);
    } finally {
      smartlingClient.deleteFile(smartlingTestConfig.projectId, fileName);
    }
  }

  @Test
  public void testUploadDownloadAndDeleteLocalizedFile() {
    Assume.assumeNotNull(smartlingTestConfig.projectId);

    FileUploadResponse response;
    String downloadFile;
    String fileName = "strings.xml";

    uploadFile(smartlingTestConfig.projectId, fileName);

    String content =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources>\n"
            + "    <string name=\"hello\">Hola</string>\n"
            + "    <string name=\"bye\">Adios</string>\n"
            + "</resources>";

    response =
        smartlingClient.uploadLocalizedFile(
            smartlingTestConfig.projectId, fileName, "android", "es-MX", content, null, null);

    downloadFile =
        smartlingClient.downloadFile(
            smartlingTestConfig.projectId,
            "es-MX",
            fileName,
            false,
            SmartlingClient.RetrievalType.PENDING);

    assertThat(response.getCode()).isEqualTo("SUCCESS");
    assertThat(downloadFile).isEqualTo(content);

    smartlingClient.deleteFile(smartlingTestConfig.projectId, fileName);
  }

  @Test
  public void testUploadContextPNGUpperCase() throws IOException {
    Assume.assumeNotNull(smartlingTestConfig.projectId);
    ClassPathResource classPathResource = new ClassPathResource("/com/box/l10n/mojito/img/1.png");
    byte[] content = ByteStreams.toByteArray(classPathResource.getInputStream());
    Context createdContext =
        smartlingClient.uploadContext(
            smartlingTestConfig.projectId, "caseissuewithpng.PNG", content);

    Context context =
        smartlingClient.getContext(smartlingTestConfig.projectId, createdContext.getContextUid());

    Assert.assertNotNull(context.getContextUid());
    Assert.assertEquals(createdContext.getContextUid(), context.getContextUid());
  }

  @Test
  public void testCRUDContext() throws IOException {
    Assume.assumeNotNull(smartlingTestConfig.projectId);

    ClassPathResource classPathResource = new ClassPathResource("/com/box/l10n/mojito/img/1.png");
    byte[] content = ByteStreams.toByteArray(classPathResource.getInputStream());
    Context createdContext =
        smartlingClient.uploadContext(smartlingTestConfig.projectId, "image1.png", content);

    Assert.assertNotNull(createdContext.getContextUid());

    Context context =
        smartlingClient.getContext(smartlingTestConfig.projectId, createdContext.getContextUid());

    Assert.assertNotNull(context.getContextUid());
    Assert.assertEquals(createdContext.getContextUid(), context.getContextUid());

    smartlingClient.deleteContext(smartlingTestConfig.projectId, context.getContextUid());

    SmartlingClientException contextNotFoundException =
        Assert.assertThrows(
            SmartlingClientException.class,
            () ->
                smartlingClient.getContext(smartlingTestConfig.projectId, context.getContextUid()));
    Assert.assertTrue(
        contextNotFoundException
            .getMessage()
            .contains(String.format("Can't get context: %s", context.getContextUid())));
  }

  @Test
  public void testGetFiles() {
    Assume.assumeNotNull(smartlingTestConfig.projectId);
    Items<File> files = smartlingClient.getFiles(smartlingTestConfig.projectId);
    files.getItems().stream().forEach(f -> logger.debug(f.getFileUri()));
  }

  @Test
  public void testGetStringInfosFromFile() {
    Assume.assumeNotNull(smartlingTestConfig.projectId);
    Items<File> files = smartlingClient.getFiles(smartlingTestConfig.projectId);
    Stream<StringInfo> stringInfosFromFiles =
        files.getItems().stream()
            .flatMap(
                file ->
                    smartlingClient.getStringInfos(
                        smartlingTestConfig.projectId, file.getFileUri()));
    stringInfosFromFiles.forEach(stringInfo -> logger.debug(stringInfo.getHashcode()));
  }

  @Test
  public void unwrapRootValueToDeserialize() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);

    String response =
        "{\n"
            + "  \"response\": {\n"
            + "    \"code\": \"SUCCESS\",\n"
            + "    \"data\": {\n"
            + "      \"accessToken\": \"b816424c-2e95-11e7-93ae-92361f002671\",\n"
            + "      \"expiresIn\": 480,\n"
            + "      \"refreshExpiresIn\": 3660,\n"
            + "      \"refreshToken\": \"c0a6f410-2e95-11e7-93ae-92361f002671\",\n"
            + "      \"tokenType\": \"Bearer\"\n"
            + "    }\n"
            + "  }\n"
            + "}";

    AuthenticationResponse authenticationResponse =
        objectMapper.readValue(response, AuthenticationResponse.class);
    Assert.assertEquals(
        "b816424c-2e95-11e7-93ae-92361f002671", authenticationResponse.getData().getAccessToken());
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
    Assert.assertEquals(
        "{\"bindings\":[{\"contextUid\":\"c1\",\"stringHashcode\":\"h1\"},{\"contextUid\":\"c1\",\"stringHashcode\":\"h1\"}]}",
        str);
  }
}
