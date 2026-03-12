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

public class TMServiceMacStringsdictTest extends TMServiceFileTypeTestBase {

  static Logger logger = LoggerFactory.getLogger(TMServiceMacStringsdictTest.class);

  @Test
  public void testMacStringsdict() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale_ja = addLocale(repo, "ja-JP");
    RepositoryLocale repoLocale_en = addLocale(repo, "en-GB");

    String assetContent =
        """
        <plist version="1.0">
        <dict>
        <key>plural_recipe_cook_hours</key>
        <dict>
            <key>NSStringLocalizedFormatKey</key>
            <string>%#@hours@ to cook</string>
            <key>hours</key>
            <dict>
                <key>NSStringFormatSpecTypeKey</key>
                <string>NSStringPluralRuleType</string>
                <key>NSStringFormatValueTypeKey</key>
                <string>d</string>
                <key>one</key>
                <string>%d hour to cook</string>
                <key>other</key>
                <string>%d hours to cook</string>
            </dict>
        </dict>
        <key>collaborators</key>
        <dict>
            <key>NSStringLocalizedFormatKey</key>
            <string>%#@collaborators@</string>
            <key>collaborators</key>
            <dict>
                <key>NSStringFormatSpecTypeKey</key>
                <string>NSStringPluralRuleType</string>
                <key>NSStringFormatValueTypeKey</key>
                <string>d</string>
                <key>one</key>
                <string>%d collaborator</string>
                <key>other</key>
                <string>%d collaborators</string>
            </dict>
        </dict>
        </dict>
        </plist>""";
    String expectedLocalizedAsset_jaJP =
        """
        <plist version="1.0">
        <dict>
        <key>plural_recipe_cook_hours</key>
        <dict>
            <key>NSStringLocalizedFormatKey</key>
            <string>%#@hours@ to cook</string>
            <key>hours</key>
            <dict>
                <key>NSStringFormatSpecTypeKey</key>
                <string>NSStringPluralRuleType</string>
                <key>NSStringFormatValueTypeKey</key>
                <string>d</string>
                <key>other</key>
                <string>%d hours to cook</string>
            </dict>
        </dict>
        <key>collaborators</key>
        <dict>
            <key>NSStringLocalizedFormatKey</key>
            <string>%#@collaborators@</string>
            <key>collaborators</key>
            <dict>
                <key>NSStringFormatSpecTypeKey</key>
                <string>NSStringPluralRuleType</string>
                <key>NSStringFormatValueTypeKey</key>
                <string>d</string>
                <key>other</key>
                <string>%d collaborators</string>
            </dict>
        </dict>
        </dict>
        </plist>""";
    String expectedLocalizedAsset_enGB =
        """
        <plist version="1.0">
        <dict>
        <key>plural_recipe_cook_hours</key>
        <dict>
            <key>NSStringLocalizedFormatKey</key>
            <string>%#@hours@ to cook</string>
            <key>hours</key>
            <dict>
                <key>NSStringFormatSpecTypeKey</key>
                <string>NSStringPluralRuleType</string>
                <key>NSStringFormatValueTypeKey</key>
                <string>d</string>
                <key>one</key>
                <string>%d hour to cook</string>
                <key>other</key>
                <string>%d hours to cook</string>
            </dict>
        </dict>
        <key>collaborators</key>
        <dict>
            <key>NSStringLocalizedFormatKey</key>
            <string>%#@collaborators@</string>
            <key>collaborators</key>
            <dict>
                <key>NSStringFormatSpecTypeKey</key>
                <string>NSStringPluralRuleType</string>
                <key>NSStringFormatValueTypeKey</key>
                <string>d</string>
                <key>one</key>
                <string>%d collaborator</string>
                <key>other</key>
                <string>%d collaborators</string>
            </dict>
        </dict>
        </dict>
        </plist>""";
    createAsset(repo, "Localizable.stringsdict", assetContent);
    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);

    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("source [{}]=[{}]", textUnitDTO.getName(), textUnitDTO.getSource());
    }

    String localizedAsset = generateLocalized(assetContent, repoLocale_ja, "ja-JP");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedLocalizedAsset_jaJP, localizedAsset);

    localizedAsset = generateLocalized(assetContent, repoLocale_en, "en-GB");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedLocalizedAsset_enGB, localizedAsset);
  }

  @Test
  public void testLocalizeMacStringsdictPluralJp() throws Exception {

    Repository repo = createRepository();
    String bcp47Tag = "ja-JP";
    RepositoryLocale repoLocale = addLocale(repo, bcp47Tag);

    String assetContent =
        """
        <plist version="1.0">
        <dict>
        <key>%d file(s) remaining</key>
        <dict>
           <key>NSStringLocalizedFormatKey</key>
           <string>%#@files@</string>
           <key>files</key>
           <dict>
               <key>NSStringFormatSpecTypeKey</key>
               <string>NSStringPluralRuleType</string>
               <key>NSStringFormatValueTypeKey</key>
               <string>d</string>
               <key>one</key>
               <string>%d file remaining</string>
               <key>other</key>
               <string>%d files remaining</string>
           </dict>
        </dict>
        </dict>
        </plist>""";

    String expectedLocalizedAsset =
        """
        <plist version="1.0">
        <dict>
        <key>%d file(s) remaining</key>
        <dict>
           <key>NSStringLocalizedFormatKey</key>
           <string>%#@files@</string>
           <key>files</key>
           <dict>
               <key>NSStringFormatSpecTypeKey</key>
               <string>NSStringPluralRuleType</string>
               <key>NSStringFormatValueTypeKey</key>
               <string>d</string>
               <key>other</key>
               <string>%d files remaining</string>
           </dict>
        </dict>
        </dict>
        </plist>""";

    createAsset(repo, "Localizable.stringsdict", assetContent);
    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);

    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("source=[{}]", textUnitDTO.getSource());
    }

    String localizedAsset = generateLocalized(assetContent, repoLocale, bcp47Tag);
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedLocalizedAsset, localizedAsset);

    String forImport =
        """
        <plist version="1.0">
        <dict>
        <key>%d file(s) remaining</key>
        <dict>
           <key>NSStringLocalizedFormatKey</key>
           <string>%#@files@</string>
           <key>files</key>
           <dict>
               <key>NSStringFormatSpecTypeKey</key>
               <string>NSStringPluralRuleType</string>
               <key>NSStringFormatValueTypeKey</key>
               <string>d</string>
               <key>other</key>
               <string>%d files remaining-jp</string>
           </dict>
        </dict>
        </dict>
        </plist>""";

    importTranslations(repoLocale, forImport, StatusForEqualTarget.TRANSLATION_NEEDED);

    localizedAsset = generateLocalized(assetContent, repoLocale, bcp47Tag);
    logger.debug("localized after import=\n{}", localizedAsset);
    assertEquals(forImport, localizedAsset);
  }

  @Test
  public void testLocalizeMacStringsdictPluralWithDifferentIdentation() throws Exception {
    Repository repo = createRepository();
    String bcp47Tag = "fr-FR";
    RepositoryLocale repoLocale = addLocale(repo, bcp47Tag);

    String assetContent =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
        <plist version="1.0">
            <dict>
                <key>%lld follower(s)</key>
                <dict>
                    <key>NSStringLocalizedFormatKey</key>
                    <string>%#@followers@</string>
                    <key>followers</key>
                    <dict>
                        <key>NSStringFormatSpecTypeKey</key>
                        <string>NSStringPluralRuleType</string>
                        <key>NSStringFormatValueTypeKey</key>
                        <string>lld</string>
                        <key>one</key>
                        <string>%lld follower</string>
                        <key>other</key>
                        <string>%lld followers</string>
                    </dict>
                </dict>
                <key>%lld following(s)</key>
                <dict>
                    <key>NSStringLocalizedFormatKey</key>
                    <string>%#@following@</string>
                    <key>following</key>
                    <dict>
                        <key>NSStringFormatSpecTypeKey</key>
                        <string>NSStringPluralRuleType</string>
                        <key>NSStringFormatValueTypeKey</key>
                        <string>lld</string>
                        <key>one</key>
                        <string>%lld following</string>
                        <key>other</key>
                        <string>%lld following</string>
                    </dict>
                </dict>
            </dict>
        </plist>
        """;

    createAsset(repo, "Localizable.stringsdict", assetContent);
    processAsset(repo, assetContent);

    // notice the doc type is gone
    String expectedLocalizedAsset =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <plist version="1.0">
            <dict>
                <key>%lld follower(s)</key>
                <dict>
                    <key>NSStringLocalizedFormatKey</key>
                    <string>%#@followers@</string>
                    <key>followers</key>
                    <dict>
                        <key>NSStringFormatSpecTypeKey</key>
                        <string>NSStringPluralRuleType</string>
                        <key>NSStringFormatValueTypeKey</key>
                        <string>lld</string>
                        <key>one</key>
                        <string>%lld follower</string>
                        <key>other</key>
                        <string>%lld followers</string>
                    </dict>
                </dict>
                <key>%lld following(s)</key>
                <dict>
                    <key>NSStringLocalizedFormatKey</key>
                    <string>%#@following@</string>
                    <key>following</key>
                    <dict>
                        <key>NSStringFormatSpecTypeKey</key>
                        <string>NSStringPluralRuleType</string>
                        <key>NSStringFormatValueTypeKey</key>
                        <string>lld</string>
                        <key>one</key>
                        <string>%lld following</string>
                        <key>other</key>
                        <string>%lld following</string>
                    </dict>
                </dict>
            </dict>
        </plist>""";

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);

    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("source [{}]=[{}]", textUnitDTO.getName(), textUnitDTO.getSource());
    }

    String localizedAsset = generateLocalized(assetContent, repoLocale, bcp47Tag);
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedLocalizedAsset, localizedAsset);

    localizedAsset = generateLocalized(assetContent, repoLocale, bcp47Tag);
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedLocalizedAsset, localizedAsset);
  }

  @Test
  public void testLocalizeMacStringsdictPluralRu() throws Exception {

    Repository repo = createRepository();
    String bcp47Tag = "ru-RU";
    RepositoryLocale repoLocale = addLocale(repo, bcp47Tag);

    String assetContent =
        """
        <plist version="1.0">
        <dict>
        <key>%d file(s) remaining</key>
        <dict>
           <key>NSStringLocalizedFormatKey</key>
           <string>%#@files@</string>
           <key>files</key>
           <dict>
               <key>NSStringFormatSpecTypeKey</key>
               <string>NSStringPluralRuleType</string>
               <key>NSStringFormatValueTypeKey</key>
               <string>d</string>
               <key>one</key>
               <string>%d file remaining</string>
               <key>other</key>
               <string>%d files remaining</string>
           </dict>
        </dict>
        </dict>
        </plist>""";

    String expectedLocalizedAsset =
        """
        <plist version="1.0">
        <dict>
        <key>%d file(s) remaining</key>
        <dict>
           <key>NSStringLocalizedFormatKey</key>
           <string>%#@files@</string>
           <key>files</key>
           <dict>
               <key>NSStringFormatSpecTypeKey</key>
               <string>NSStringPluralRuleType</string>
               <key>NSStringFormatValueTypeKey</key>
               <string>d</string>
               <key>one</key>
               <string>%d file remaining</string>
               <key>few</key>
               <string>%d files remaining</string>
               <key>many</key>
               <string>%d files remaining</string>
               <key>other</key>
               <string>%d files remaining</string>
           </dict>
        </dict>
        </dict>
        </plist>""";

    createAsset(repo, "Localizable.stringsdict", assetContent);
    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);

    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("source=[{}]", textUnitDTO.getSource());
    }

    String localizedAsset = generateLocalized(assetContent, repoLocale, bcp47Tag);
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedLocalizedAsset, localizedAsset);

    String forImport =
        """
        <plist version="1.0">
        <dict>
        <key>%d file(s) remaining</key>
        <dict>
           <key>NSStringLocalizedFormatKey</key>
           <string>%#@files@</string>
           <key>files</key>
           <dict>
               <key>NSStringFormatSpecTypeKey</key>
               <string>NSStringPluralRuleType</string>
               <key>NSStringFormatValueTypeKey</key>
               <string>d</string>
               <key>one</key>
               <string>%d file remaining-ru</string>
               <key>few</key>
               <string>%d files remaining-ru</string>
               <key>many</key>
               <string>%d files remaining-ru</string>
               <key>other</key>
               <string>%d files remaining-ru</string>
           </dict>
        </dict>
        </dict>
        </plist>""";

    importTranslations(repoLocale, forImport, StatusForEqualTarget.TRANSLATION_NEEDED);

    localizedAsset = generateLocalized(assetContent, repoLocale, bcp47Tag);
    logger.debug("localized after import=\n{}", localizedAsset);
    assertEquals(forImport, localizedAsset);
  }
}
