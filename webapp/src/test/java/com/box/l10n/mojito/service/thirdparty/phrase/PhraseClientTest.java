package com.box.l10n.mojito.service.thirdparty.phrase;

import static com.box.l10n.mojito.android.strings.AndroidStringDocumentWriter.EscapeType.NONE;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.android.strings.AndroidSingular;
import com.box.l10n.mojito.android.strings.AndroidStringDocument;
import com.box.l10n.mojito.android.strings.AndroidStringDocumentWriter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.phrase.client.model.Tag;
import com.phrase.client.model.Upload;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
      PhraseClientTest.class,
      PhraseClientConfig.class,
      PhraseClientPropertiesConfig.class
    })
@EnableConfigurationProperties
@Ignore
public class PhraseClientTest {

  static Logger logger = LoggerFactory.getLogger(PhraseClientTest.class);

  @Autowired(required = false)
  PhraseClient phraseClient;

  @Value("${test.phrase-client.projectId:}")
  String testProjectId;

  @Autowired private PhraseClientConfig phraseClientConfig;

  @Test
  public void testRemoveTag() {
    String tagForUpload = "push_2024_06_10_07_17_00_089_122";
    List<String> tagsToDelete =
        phraseClient.listTags(testProjectId).stream()
            .peek(tag -> logger.info("tag: {}", tag))
            .map(Tag::getName)
            .filter(Objects::nonNull)
            .filter(tagName -> !tagName.equals(tagForUpload))
            .toList();

    phraseClient.deleteTags(testProjectId, tagsToDelete);
  }

  @Test
  public void testParallelDownload() {

    Assume.assumeNotNull(testProjectId);
    // measure time of following call
    Stopwatch total = Stopwatch.createStarted();

    List<String> locales =
        List.of(
            "bg", "bn", "bs", "ca", "cs", "da", "de", "el", "es-419", "es", "et", "fi", "fr-CA",
            "fr", "gu", "hi", "hr", "hu", "hy", "id", "is", "it", "ja", "ka", "kn", "ko", "lv",
            "mk", "mr", "ms", "my", "nb", "nl", "pl", "pt", "pt-PT", "ro", "ru", "sk", "sl", "so",
            "sq", "sr", "sv", "sw", "ta", "te", "th", "tr", "uk", "vi", "zh", "zh-HK", "zh-Hant");

    locales.parallelStream()
        .forEach(
            l -> {
              Stopwatch started = Stopwatch.createStarted();
              String s =
                  phraseClient.localeDownload(
                      "9b6f4e167397549fe7ec1fe82497159b",
                      l,
                      "xml",
                      "push_chatgpt-web_2024_06_27_05_14_58_356_208",
                      null);
              //      logger.info(s);
              logger.info("Download: {} in {}", l, started.elapsed());
            });

    logger.info("total time: {}", total.elapsed());
  }

  @Test
  public void test() throws IOException, InterruptedException {
    Assume.assumeNotNull(testProjectId);

    for (int i = 1; i < 2; i++) {
      AndroidStringDocument source = new AndroidStringDocument();
      source.addSingular(
          new AndroidSingular(11L, i + "-string1", "some <a>link</a> to a page", "test comment1"));
      source.addSingular(
          new AndroidSingular(
              12L,
              i + "-string2",
              "some <a href=\"http://test.com/\">link</a> to a page",
              "test comment2"));
      source.addSingular(
          new AndroidSingular(
              13L,
              i + "-string3",
              "If that is your IP address <a href=\"{{ url }}\">click here</a> to unblock it.",
              "test comment2"));

      source.addSingular(
          new AndroidSingular(
              14L,
              i + "-string4",
              "If that is your IP address <a href=\"{ url }\">click here</a> to unblock it.",
              "test comment2"));

      source.addSingular(
          new AndroidSingular(
              15L,
              i + "-string5",
              "If that is your IP address <a href=\"url\">click here</a> to unblock it.",
              "test comment2"));

      source.addSingular(
          new AndroidSingular(
              16L,
              i + "-string6",
              "visit your <annotation url=\"settings\">settings</annotation>",
              "test comment2"));

      source.addSingular(
          new AndroidSingular(17L, i + "-string7", "a string\nwith return line", "test comment2"));

      source.addSingular(
          new AndroidSingular(
              18L, i + "-string9", "a string\n\nwith two return line", "test comment2"));

      source.addSingular(
          new AndroidSingular(
              19L, i + "-string10", "a string & and the &amp; escape", "test comment2"));

      source.addSingular(
          new AndroidSingular(20L, i + "-string11", "simple <i>tag</i>", "test comment2"));

      String androidFile = new AndroidStringDocumentWriter(source, NONE).toText();
      System.out.println(androidFile);

      Upload upload =
          phraseClient.nativeUploadAndWait(
              testProjectId,
              "en",
              "xml",
              "strings.xml",
              androidFile,
              ImmutableList.of(i % 2 == 0 ? "test-escaping" : "test-no-escaping"),
              ImmutableMap.of("unescape_tags", "true"));

      //      Upload upload =
      //          phraseClient.uploadAndWait(
      //              testProjectId,
      //              "en",
      //              "xml",
      //              "strings.xml",
      //              androidFile,
      //              ImmutableList.of(i % 2 == 0 ? "test-escaping" : "test-no-escaping"),
      //                null);

      System.out.println(upload);

      String s =
          phraseClient.localeDownload(
              testProjectId,
              "en",
              "xml",
              i % 2 == 0 ? "test-escaping" : "test-no-escaping",
              () -> null);
      System.out.println(s);

      String ns =
          phraseClient.nativeLocaleDownload(
              testProjectId,
              "en",
              "xml",
              i % 2 == 0 ? "test-escaping" : "test-no-escaping",
              ImmutableMap.of("escape_tags", "true"),
              () -> null);

      System.out.println(ns);

      //      Assert.assertEquals(androidFile, ns);
    }

    //    for (int i = 0; i < 3; i++) {
    //      String repoName = "repo_%d".formatted(i);
    //      String tagForUpload = ThirdPartyTMSPhrase.getTagForUpload(repoName);
    //
    //      logger.info("tagForUpload: {}", tagForUpload);
    //
    //      String fileContentAndroid = generateFileContent(repoName).toString();
    //      phraseClient.uploadAndWait(
    //          testProjectId,
    //          "en",
    //          "xml",
    //          "strings.xml",
    //          fileContentAndroid,
    //          ImmutableList.of(tagForUpload));
    //
    //      new ThirdPartyTMSPhrase(phraseClient)
    //          .removeUnusedKeysAndTags(testProjectId, repoName, tagForUpload);
    //    }
    //
    //    List<TranslationKey> translationKeys = phraseClient.getKeys(testProjectId);
    //    for (TranslationKey translationKey : translationKeys) {
    //      logger.info("{}", translationKey);
    //    }

    //
    //    String fileContentAndroid2 =
    //        """
    //                    <?xml version="1.0" encoding="UTF-8"?>
    //                    <resources>
    //                        <string name="app_name">Locale Tester - fr</string>
    //                        <string name="action_settings">Settings - fr</string>
    //                        <string name="hello">Hello</string>
    //                         <plurals name="plural_things">
    //                            <item quantity="one">One thing - fr</item>
    //                            <item quantity="other">Multiple things - fr</item>
    //                      </plurals>
    //                    </resources>
    //                    """;
    //    phraseClient.uploadCreateFile(
    //        testProjectId, "fr", "xml", "strings.xml", fileContentAndroid2, null);

    //    String s2 =
    //        phraseClient.localeDownload(
    //            testProjectId,
    //            "en",
    //            "xml",
    //            "startWithABadTag",
    //            () ->
    //                phraseClient.listTags(testProjectId).stream()
    //                    .map(Tag::getName)
    //                    .filter(Objects::nonNull)
    //                    .filter(tagName -> tagName.startsWith("push_repo_2"))
    //                    .collect(Collectors.joining(",")));
    //
    //    logger.info(s2);
  }

  static StringBuilder generateFileContent(String repositoryName) {
    StringBuilder fileContentAndroidBuilder = new StringBuilder();

    fileContentAndroidBuilder.append(
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <resources>
          <string name="app_name_%s">Locale Tester</string>
        """
            .formatted(repositoryName));

    ZonedDateTime now = JSR310Migration.dateTimeNowInUTC();
    for (int i = 0; i < 1; i++) {
      fileContentAndroidBuilder.append(
          String.format("<string name=\"action_settings-%d\">Settings</string>\n", i));
      fileContentAndroidBuilder.append(
          String.format(
              "<string name=\"%s_action_settings-%d-%s\">Settings</string>\n",
              repositoryName, i, now.toString()));
    }

    fileContentAndroidBuilder.append("</resources>");
    return fileContentAndroidBuilder;
  }
}
