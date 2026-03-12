package com.box.l10n.mojito.service.tm.filetype;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.okapi.ImportTranslationsFromLocalizedAssetStep.StatusForEqualTarget;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TMServiceMacStringsTest extends TMServiceFileTypeTestBase {

  static Logger logger = LoggerFactory.getLogger(TMServiceMacStringsTest.class);

  /**
   * This test is to test {@link SimpleEncoder} with special characters
   *
   * <p>According to iOS specification in
   * https://developer.apple.com/library/ios/documentation/Cocoa/Conceptual/LoadingResources/Strings/Strings.html,
   * the following characters should be escaped with backslash: double-quote, backslash,
   * newline(\n), carriage return (\r).
   *
   * @throws Exception
   */
  @Test
  public void testLocalizeMacStringsWithSpecialCharacters() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    String assetContent =
        "\"100_character_description\" = \"\\\"100\\\" character description:\";\n"
            + "\"two_lines\" = \"first\\nsecond\";";
    createAsset(repo, "en.lproj/Localizable.strings", assetContent);
    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("source=[{}]", textUnitDTO.getSource());
    }

    String localizedAsset = generateLocalized(assetContent, repoLocale, "en-GB");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(assetContent, localizedAsset);
  }

  @Test
  public void testLocalizeMacStringsRemoveUntranslated() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "ja-JP");

    String assetContent =
        """
            /* comment 1 */
            "key1" = "value1";

            /* comment 2 */
            "key2" = "value2";
            """;

    String expectedLocalizedAsset = "\n";

    createAsset(repo, "Localizable.strings", assetContent);
    processAsset(repo, assetContent);

    String localizedAsset = generateLocalizedRemoveUntranslated(assetContent, repoLocale, "ja-JP");
    logger.debug("localized=\n{}\nEOL", localizedAsset);
    assertEquals(expectedLocalizedAsset, localizedAsset);

    String forImport =
        """
            /* comment 1 */
            "key1" = "value1-jp";

            /* comment 2 */
            "key2" = "value2-jp";
            """;

    logger.debug("formimport=\n{}", forImport);

    importTranslations(repoLocale, forImport, StatusForEqualTarget.TRANSLATION_NEEDED);

    localizedAsset = generateLocalizedRemoveUntranslated(assetContent, repoLocale, "ja-JP");
    logger.info("localized after import=\n{}", localizedAsset);
    assertEquals(forImport, localizedAsset);
  }

  @Test
  public void testLocalizeMacStringsNamessNotEnclosedInDoubleQuotes() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    String assetContent = "NSUsageDescription = \"Usage description:\";\n";
    createAsset(repo, "en.lproj/Localizable.strings", assetContent);
    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);
    assertEquals(1, textUnitDTOs.size());
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("source=[{}]", textUnitDTO.getSource());
    }

    String localizedAsset = generateLocalized(assetContent, repoLocale, "en-GB");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(assetContent, localizedAsset);
  }
}
