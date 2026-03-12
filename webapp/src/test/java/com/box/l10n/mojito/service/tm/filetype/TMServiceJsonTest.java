package com.box.l10n.mojito.service.tm.filetype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TMServiceJsonTest extends TMServiceFileTypeTestBase {

  static Logger logger = LoggerFactory.getLogger(TMServiceJsonTest.class);

  @Test
  public void testLocalizeJsonWithComments() throws Exception {
    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    String assetContent =
        """
        {
          // Greeting from Main UI 1
          "hello1": "Hello 1",
          "hello2": "Hello 2",
          // Extra
          // Greeting from Main UI 3
          "hello3": "Hello 3"
        }""";
    String expectedContent = assetContent;

    createAsset(repo, "strings.json", assetContent);
    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);
    assertEquals(3, textUnitDTOs.size());
    assertEquals("Greeting from Main UI 1", textUnitDTOs.get(0).getComment());
    assertNull(textUnitDTOs.get(1).getComment());
    assertEquals("Greeting from Main UI 3", textUnitDTOs.get(2).getComment());
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("name=[{}], source=[{}]", textUnitDTO.getName(), textUnitDTO.getSource());
    }

    String localizedAsset = generateLocalized(assetContent, repoLocale, "en-GB");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedContent, localizedAsset);
  }

  @Test
  public void testLocalizeJson() throws Exception {
    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    List<String> jsonFilterOptions =
        Arrays.asList("useFullKeyPath=true", "extractAllPairs=false", "exceptions=.*/string");

    String assetContent =
        """
        {
          "this to ignore": {
            "k1": "v1"
          },
          "hello_world": {
            "string": "Hello World",
            "note": "The start of every language book."
          },
          "num_photos": {
            "string": "You have {numPhotos, plural, =0 {no photos.} =1 {one photo.} other {# photos.}}",
            "note": "A description that shows the number of photos a user has."
          }
        }""";
    String expectedContent = assetContent;

    createAsset(repo, "strings.json", assetContent);
    processAsset(repo, assetContent, jsonFilterOptions);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);
    assertEquals(2, textUnitDTOs.size());
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("name=[{}], source=[{}]", textUnitDTO.getName(), textUnitDTO.getSource());
    }

    assertEquals("hello_world/string", textUnitDTOs.get(0).getName());

    String localizedAsset = generateLocalized(assetContent, repoLocale, "en-GB", jsonFilterOptions);
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedContent, localizedAsset);
  }

  @Test
  public void testLocalizeJsonRemoveKeySuffix() throws Exception {
    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    List<String> jsonFilterOptions =
        Arrays.asList(
            "useFullKeyPath=true",
            "extractAllPairs=false",
            "exceptions=.*/string",
            "removeKeySuffix=/string");

    String assetContent =
        """
        {
          "this to ignore": {
            "k1": "v1"
          },
          "hello_world": {
            "string": "Hello World",
            "note": "The start of every language book."
          },
          "num_photos": {
            "string": "You have {numPhotos, plural, =0 {no photos.} =1 {one photo.} other {# photos.}}",
            "note": "A description that shows the number of photos a user has."
          }
        }""";
    String expectedContent = assetContent;

    createAsset(repo, "strings.json", assetContent);
    processAsset(repo, assetContent, jsonFilterOptions);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);
    assertEquals(2, textUnitDTOs.size());
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("name=[{}], source=[{}]", textUnitDTO.getName(), textUnitDTO.getSource());
    }

    assertEquals("hello_world", textUnitDTOs.get(0).getName());

    String localizedAsset = generateLocalized(assetContent, repoLocale, "en-GB", jsonFilterOptions);
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedContent, localizedAsset);
  }

  @Test
  public void testLocalizeJsonHTMLCode() throws Exception {
    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    List<String> jsonFilterOptions =
        Arrays.asList(
            "convertToHtmlCodes=true",
            """
            codeFinderData=#v1
            count.i=3
            rule0=%(([-0+#]?)[-0+#]?)((\\d\\$)?)(([\\d\\*]*)(\\.[\\d\\*]*)?)[dioxXucsfeEgGpn]
            rule1=(\\\\r\\\\n)|\\\\a|\\\\b|\\\\f|\\\\n|\\\\r|\\\\t|\\\\v
            rule2=\\{\\d.*?\\}
            sample=%s, %d, {1}, \\n, \\r, \\t, etc.
            useAllRulesWhenTesting.b=false""");

    String assetContent = "{\"hello\" : \"Hello %s!\" }\n";

    String expectedContent = assetContent;
    createAsset(repo, "strings.json", assetContent);
    processAsset(repo, assetContent, jsonFilterOptions);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);
    assertEquals(1, textUnitDTOs.size());
    assertEquals("Hello <br id='p1'/>!", textUnitDTOs.get(0).getSource());
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.info("name=[{}], source=[{}]", textUnitDTO.getName(), textUnitDTO.getSource());
    }

    String localizedAsset = generateLocalized(assetContent, repoLocale, "en-GB", jsonFilterOptions);
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedContent, localizedAsset);
  }
}
