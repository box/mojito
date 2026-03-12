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
        "<plist version=\"1.0\">\n"
            + "<dict>\n"
            + "<key>plural_recipe_cook_hours</key>\n"
            + "<dict>\n"
            + "    <key>NSStringLocalizedFormatKey</key>\n"
            + "    <string>%#@hours@ to cook</string>\n"
            + "    <key>hours</key>\n"
            + "    <dict>\n"
            + "        <key>NSStringFormatSpecTypeKey</key>\n"
            + "        <string>NSStringPluralRuleType</string>\n"
            + "        <key>NSStringFormatValueTypeKey</key>\n"
            + "        <string>d</string>\n"
            + "        <key>one</key>\n"
            + "        <string>%d hour to cook</string>\n"
            + "        <key>other</key>\n"
            + "        <string>%d hours to cook</string>\n"
            + "    </dict>\n"
            + "</dict>\n"
            + "<key>collaborators</key>\n"
            + "<dict>\n"
            + "    <key>NSStringLocalizedFormatKey</key>\n"
            + "    <string>%#@collaborators@</string>\n"
            + "    <key>collaborators</key>\n"
            + "    <dict>\n"
            + "        <key>NSStringFormatSpecTypeKey</key>\n"
            + "        <string>NSStringPluralRuleType</string>\n"
            + "        <key>NSStringFormatValueTypeKey</key>\n"
            + "        <string>d</string>\n"
            + "        <key>one</key>\n"
            + "        <string>%d collaborator</string>\n"
            + "        <key>other</key>\n"
            + "        <string>%d collaborators</string>\n"
            + "    </dict>\n"
            + "</dict>\n"
            + "</dict>\n"
            + "</plist>";
    String expectedLocalizedAsset_jaJP =
        "<plist version=\"1.0\">\n"
            + "<dict>\n"
            + "<key>plural_recipe_cook_hours</key>\n"
            + "<dict>\n"
            + "    <key>NSStringLocalizedFormatKey</key>\n"
            + "    <string>%#@hours@ to cook</string>\n"
            + "    <key>hours</key>\n"
            + "    <dict>\n"
            + "        <key>NSStringFormatSpecTypeKey</key>\n"
            + "        <string>NSStringPluralRuleType</string>\n"
            + "        <key>NSStringFormatValueTypeKey</key>\n"
            + "        <string>d</string>\n"
            + "        <key>other</key>\n"
            + "        <string>%d hours to cook</string>\n"
            + "    </dict>\n"
            + "</dict>\n"
            + "<key>collaborators</key>\n"
            + "<dict>\n"
            + "    <key>NSStringLocalizedFormatKey</key>\n"
            + "    <string>%#@collaborators@</string>\n"
            + "    <key>collaborators</key>\n"
            + "    <dict>\n"
            + "        <key>NSStringFormatSpecTypeKey</key>\n"
            + "        <string>NSStringPluralRuleType</string>\n"
            + "        <key>NSStringFormatValueTypeKey</key>\n"
            + "        <string>d</string>\n"
            + "        <key>other</key>\n"
            + "        <string>%d collaborators</string>\n"
            + "    </dict>\n"
            + "</dict>\n"
            + "</dict>\n"
            + "</plist>";
    String expectedLocalizedAsset_enGB =
        "<plist version=\"1.0\">\n"
            + "<dict>\n"
            + "<key>plural_recipe_cook_hours</key>\n"
            + "<dict>\n"
            + "    <key>NSStringLocalizedFormatKey</key>\n"
            + "    <string>%#@hours@ to cook</string>\n"
            + "    <key>hours</key>\n"
            + "    <dict>\n"
            + "        <key>NSStringFormatSpecTypeKey</key>\n"
            + "        <string>NSStringPluralRuleType</string>\n"
            + "        <key>NSStringFormatValueTypeKey</key>\n"
            + "        <string>d</string>\n"
            + "        <key>one</key>\n"
            + "        <string>%d hour to cook</string>\n"
            + "        <key>other</key>\n"
            + "        <string>%d hours to cook</string>\n"
            + "    </dict>\n"
            + "</dict>\n"
            + "<key>collaborators</key>\n"
            + "<dict>\n"
            + "    <key>NSStringLocalizedFormatKey</key>\n"
            + "    <string>%#@collaborators@</string>\n"
            + "    <key>collaborators</key>\n"
            + "    <dict>\n"
            + "        <key>NSStringFormatSpecTypeKey</key>\n"
            + "        <string>NSStringPluralRuleType</string>\n"
            + "        <key>NSStringFormatValueTypeKey</key>\n"
            + "        <string>d</string>\n"
            + "        <key>one</key>\n"
            + "        <string>%d collaborator</string>\n"
            + "        <key>other</key>\n"
            + "        <string>%d collaborators</string>\n"
            + "    </dict>\n"
            + "</dict>\n"
            + "</dict>\n"
            + "</plist>";
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
        "<plist version=\"1.0\">\n"
            + "<dict>\n"
            + "<key>%d file(s) remaining</key>\n"
            + "<dict>\n"
            + "   <key>NSStringLocalizedFormatKey</key>\n"
            + "   <string>%#@files@</string>\n"
            + "   <key>files</key>\n"
            + "   <dict>\n"
            + "       <key>NSStringFormatSpecTypeKey</key>\n"
            + "       <string>NSStringPluralRuleType</string>\n"
            + "       <key>NSStringFormatValueTypeKey</key>\n"
            + "       <string>d</string>\n"
            + "       <key>one</key>\n"
            + "       <string>%d file remaining</string>\n"
            + "       <key>other</key>\n"
            + "       <string>%d files remaining</string>\n"
            + "   </dict>\n"
            + "</dict>\n"
            + "</dict>\n"
            + "</plist>";

    String expectedLocalizedAsset =
        "<plist version=\"1.0\">\n"
            + "<dict>\n"
            + "<key>%d file(s) remaining</key>\n"
            + "<dict>\n"
            + "   <key>NSStringLocalizedFormatKey</key>\n"
            + "   <string>%#@files@</string>\n"
            + "   <key>files</key>\n"
            + "   <dict>\n"
            + "       <key>NSStringFormatSpecTypeKey</key>\n"
            + "       <string>NSStringPluralRuleType</string>\n"
            + "       <key>NSStringFormatValueTypeKey</key>\n"
            + "       <string>d</string>\n"
            + "       <key>other</key>\n"
            + "       <string>%d files remaining</string>\n"
            + "   </dict>\n"
            + "</dict>\n"
            + "</dict>\n"
            + "</plist>";

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
        "<plist version=\"1.0\">\n"
            + "<dict>\n"
            + "<key>%d file(s) remaining</key>\n"
            + "<dict>\n"
            + "   <key>NSStringLocalizedFormatKey</key>\n"
            + "   <string>%#@files@</string>\n"
            + "   <key>files</key>\n"
            + "   <dict>\n"
            + "       <key>NSStringFormatSpecTypeKey</key>\n"
            + "       <string>NSStringPluralRuleType</string>\n"
            + "       <key>NSStringFormatValueTypeKey</key>\n"
            + "       <string>d</string>\n"
            + "       <key>other</key>\n"
            + "       <string>%d files remaining-jp</string>\n"
            + "   </dict>\n"
            + "</dict>\n"
            + "</dict>\n"
            + "</plist>";

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
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
            + "<plist version=\"1.0\">\n"
            + "    <dict>\n"
            + "        <key>%lld follower(s)</key>\n"
            + "        <dict>\n"
            + "            <key>NSStringLocalizedFormatKey</key>\n"
            + "            <string>%#@followers@</string>\n"
            + "            <key>followers</key>\n"
            + "            <dict>\n"
            + "                <key>NSStringFormatSpecTypeKey</key>\n"
            + "                <string>NSStringPluralRuleType</string>\n"
            + "                <key>NSStringFormatValueTypeKey</key>\n"
            + "                <string>lld</string>\n"
            + "                <key>one</key>\n"
            + "                <string>%lld follower</string>\n"
            + "                <key>other</key>\n"
            + "                <string>%lld followers</string>\n"
            + "            </dict>\n"
            + "        </dict>\n"
            + "        <key>%lld following(s)</key>\n"
            + "        <dict>\n"
            + "            <key>NSStringLocalizedFormatKey</key>\n"
            + "            <string>%#@following@</string>\n"
            + "            <key>following</key>\n"
            + "            <dict>\n"
            + "                <key>NSStringFormatSpecTypeKey</key>\n"
            + "                <string>NSStringPluralRuleType</string>\n"
            + "                <key>NSStringFormatValueTypeKey</key>\n"
            + "                <string>lld</string>\n"
            + "                <key>one</key>\n"
            + "                <string>%lld following</string>\n"
            + "                <key>other</key>\n"
            + "                <string>%lld following</string>\n"
            + "            </dict>\n"
            + "        </dict>\n"
            + "    </dict>\n"
            + "</plist>\n";

    createAsset(repo, "Localizable.stringsdict", assetContent);
    processAsset(repo, assetContent);

    // notice the doc type is gone
    String expectedLocalizedAsset =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<plist version=\"1.0\">\n"
            + "    <dict>\n"
            + "        <key>%lld follower(s)</key>\n"
            + "        <dict>\n"
            + "            <key>NSStringLocalizedFormatKey</key>\n"
            + "            <string>%#@followers@</string>\n"
            + "            <key>followers</key>\n"
            + "            <dict>\n"
            + "                <key>NSStringFormatSpecTypeKey</key>\n"
            + "                <string>NSStringPluralRuleType</string>\n"
            + "                <key>NSStringFormatValueTypeKey</key>\n"
            + "                <string>lld</string>\n"
            + "                <key>one</key>\n"
            + "                <string>%lld follower</string>\n"
            + "                <key>other</key>\n"
            + "                <string>%lld followers</string>\n"
            + "            </dict>\n"
            + "        </dict>\n"
            + "        <key>%lld following(s)</key>\n"
            + "        <dict>\n"
            + "            <key>NSStringLocalizedFormatKey</key>\n"
            + "            <string>%#@following@</string>\n"
            + "            <key>following</key>\n"
            + "            <dict>\n"
            + "                <key>NSStringFormatSpecTypeKey</key>\n"
            + "                <string>NSStringPluralRuleType</string>\n"
            + "                <key>NSStringFormatValueTypeKey</key>\n"
            + "                <string>lld</string>\n"
            + "                <key>one</key>\n"
            + "                <string>%lld following</string>\n"
            + "                <key>other</key>\n"
            + "                <string>%lld following</string>\n"
            + "            </dict>\n"
            + "        </dict>\n"
            + "    </dict>\n"
            + "</plist>";

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
        "<plist version=\"1.0\">\n"
            + "<dict>\n"
            + "<key>%d file(s) remaining</key>\n"
            + "<dict>\n"
            + "   <key>NSStringLocalizedFormatKey</key>\n"
            + "   <string>%#@files@</string>\n"
            + "   <key>files</key>\n"
            + "   <dict>\n"
            + "       <key>NSStringFormatSpecTypeKey</key>\n"
            + "       <string>NSStringPluralRuleType</string>\n"
            + "       <key>NSStringFormatValueTypeKey</key>\n"
            + "       <string>d</string>\n"
            + "       <key>one</key>\n"
            + "       <string>%d file remaining</string>\n"
            + "       <key>other</key>\n"
            + "       <string>%d files remaining</string>\n"
            + "   </dict>\n"
            + "</dict>\n"
            + "</dict>\n"
            + "</plist>";

    String expectedLocalizedAsset =
        "<plist version=\"1.0\">\n"
            + "<dict>\n"
            + "<key>%d file(s) remaining</key>\n"
            + "<dict>\n"
            + "   <key>NSStringLocalizedFormatKey</key>\n"
            + "   <string>%#@files@</string>\n"
            + "   <key>files</key>\n"
            + "   <dict>\n"
            + "       <key>NSStringFormatSpecTypeKey</key>\n"
            + "       <string>NSStringPluralRuleType</string>\n"
            + "       <key>NSStringFormatValueTypeKey</key>\n"
            + "       <string>d</string>\n"
            + "       <key>one</key>\n"
            + "       <string>%d file remaining</string>\n"
            + "       <key>few</key>\n"
            + "       <string>%d files remaining</string>\n"
            + "       <key>many</key>\n"
            + "       <string>%d files remaining</string>\n"
            + "       <key>other</key>\n"
            + "       <string>%d files remaining</string>\n"
            + "   </dict>\n"
            + "</dict>\n"
            + "</dict>\n"
            + "</plist>";

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
        "<plist version=\"1.0\">\n"
            + "<dict>\n"
            + "<key>%d file(s) remaining</key>\n"
            + "<dict>\n"
            + "   <key>NSStringLocalizedFormatKey</key>\n"
            + "   <string>%#@files@</string>\n"
            + "   <key>files</key>\n"
            + "   <dict>\n"
            + "       <key>NSStringFormatSpecTypeKey</key>\n"
            + "       <string>NSStringPluralRuleType</string>\n"
            + "       <key>NSStringFormatValueTypeKey</key>\n"
            + "       <string>d</string>\n"
            + "       <key>one</key>\n"
            + "       <string>%d file remaining-ru</string>\n"
            + "       <key>few</key>\n"
            + "       <string>%d files remaining-ru</string>\n"
            + "       <key>many</key>\n"
            + "       <string>%d files remaining-ru</string>\n"
            + "       <key>other</key>\n"
            + "       <string>%d files remaining-ru</string>\n"
            + "   </dict>\n"
            + "</dict>\n"
            + "</dict>\n"
            + "</plist>";

    importTranslations(repoLocale, forImport, StatusForEqualTarget.TRANSLATION_NEEDED);

    localizedAsset = generateLocalized(assetContent, repoLocale, bcp47Tag);
    logger.debug("localized after import=\n{}", localizedAsset);
    assertEquals(forImport, localizedAsset);
  }
}
