package com.box.l10n.mojito.service.thirdparty.phrase;

import com.box.l10n.mojito.service.thirdparty.ThirdPartyTMSPhrase;
import com.google.common.collect.ImmutableList;
import com.phrase.client.model.Tag;
import com.phrase.client.model.TranslationKey;
import java.util.List;
import org.junit.Assume;
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
public class PhraseClientTest {

  static Logger logger = LoggerFactory.getLogger(PhraseClientTest.class);

  @Autowired PhraseClient phraseClient;

  @Value("${test.phrase-client.projectId}")
  String testProjectId;

  @Test
  public void testRemoveTag() {
    String tagForUpload = "push_2024_06_10_07_17_00_089_122";
    List<Tag> tagsToDelete =
        phraseClient.listTags(testProjectId).stream()
            .peek(tag -> logger.info("tag: {}", tag))
            .filter(tag -> tag.getName() != null && !tag.getName().equals(tagForUpload))
            .toList();

    phraseClient.deleteTags(testProjectId, tagsToDelete);
  }

  @Test
  public void test() {
    Assume.assumeNotNull(testProjectId);

    String tagForUpload = ThirdPartyTMSPhrase.getTagForUpload();

    logger.info("tagForUpload: {}", tagForUpload);

    StringBuilder fileContentAndroidBuilder = generateFileContent();

    String fileContentAndroid = fileContentAndroidBuilder.toString();
    phraseClient.uploadAndWait(
        testProjectId,
        "en",
        "xml",
        "strings.xml",
        fileContentAndroid,
        ImmutableList.of(tagForUpload));

    phraseClient.removeKeysNotTaggedWith(testProjectId, tagForUpload);

    List<TranslationKey> translationKeys = phraseClient.getKeys(testProjectId);
    for (TranslationKey translationKey : translationKeys) {
      logger.info("{}", translationKey);
    }

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
    //    String s2 = phraseClient.localeDownload(testProjectId, "fr", "xml");
    //    logger.info(s2);
  }

  static StringBuilder generateFileContent() {
    StringBuilder fileContentAndroidBuilder = new StringBuilder();

    fileContentAndroidBuilder.append(
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <resources>
          <string name="app_name">Locale Tester</string>
        """);

    for (int i = 0; i < 2000; i++) {
      fileContentAndroidBuilder.append(
          String.format("<string name=\"action_settings-%d\">Settings</string>\n", i));
    }

    fileContentAndroidBuilder.append("</resources>");
    return fileContentAndroidBuilder;
  }
}
