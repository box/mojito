package com.box.l10n.mojito.phabricator;

import static org.junit.Assert.fail;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.phabricator.payload.ResultWithError;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.util.MultiValueMap;

public class StubPhabricatorHttpClient extends PhabricatorHttpClient {

  File inputDirectory;
  boolean overrideFiles = false;
  boolean shouldWrite = false;
  boolean overrideIds = true;
  PhabricatorHttpClient phabricatorHttpClient;
  Map<String, String> phidMap;

  public StubPhabricatorHttpClient(
      File inputDirectory,
      PhabricatorHttpClient phabricatorHttpClient,
      Map<String, String> phidMap) {
    super(getBaseUrl(phabricatorHttpClient), getAuthToken(phabricatorHttpClient));
    this.inputDirectory = inputDirectory;
    this.phidMap = phidMap;
  }

  static String getBaseUrl(PhabricatorHttpClient phabricatorHttpClient) {
    return phabricatorHttpClient == null ? "http://localhost" : phabricatorHttpClient.baseUrl;
  }

  static String getAuthToken(PhabricatorHttpClient phabricatorHttpClient) {
    return phabricatorHttpClient == null ? "testToken" : phabricatorHttpClient.authToken;
  }

  @Override
  public <T extends ResultWithError> T postEntityAndCheckResponse(
      Method method, HttpEntity<MultiValueMap<String, Object>> httpEntity, Class<T> clazz)
      throws PhabricatorException {
    try {
      String output;

      String phid = (String) httpEntity.getBody().getFirst(CONSTRAINTS_PHIDS_0);
      String phidNoId = phid.replaceFirst("(PHID-.*?)-.*", "$1");

      String filename = method.getMethod() + "_" + phidNoId + ".json";

      ObjectMapper objectMapper = new ObjectMapper();

      File outputFile = inputDirectory.toPath().resolve(filename).toFile();

      if (overrideFiles || !outputFile.exists()) {
        if (!shouldWrite) {
          fail("Stub not setup to write, fail, you can set shouldWrite = true.");
        }

        if (overrideIds && outputFile.exists()) {
          logger.info("override files");
        } else {
          logger.info("No local data for this test, call shell");
        }

        outputFile.getParentFile().mkdirs();

        String phidRequest = phidMap.getOrDefault(phid, phid);
        httpEntity.getBody().set(CONSTRAINTS_PHIDS_0, phidRequest);
        T t = super.postEntityAndCheckResponse(method, httpEntity, clazz);

        output = objectMapper.writeValueAsStringUnchecked(t);

        if (overrideIds) {
          output =
              output
                  .replaceAll("\"PHID-(.*?)-(.*?)\"", "\"PHID-$1-sometest\"")
                  .replaceAll("id\":(\\d\\d)\\d+", "id\": $1")
                  .replaceAll("identifier\":\"(\\w)\\w*?(\\w)", "identifier\":\"$1$2");
        }

        Files.write(output, outputFile, StandardCharsets.UTF_8);
      } else {
        logger.debug("Read local data from file");
        output = Files.toString(outputFile, StandardCharsets.UTF_8);
      }

      return objectMapper.readValueUnchecked(output, clazz);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
