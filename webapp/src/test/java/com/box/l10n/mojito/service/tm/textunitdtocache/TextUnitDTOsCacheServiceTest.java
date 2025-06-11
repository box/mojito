package com.box.l10n.mojito.service.tm.textunitdtocache;

import static com.box.l10n.mojito.entity.TMTextUnitVariant.Status.APPROVED;
import static com.box.l10n.mojito.entity.TMTextUnitVariant.Status.TRANSLATION_NEEDED;
import static com.box.l10n.mojito.service.tm.search.StatusFilter.APPROVED_AND_NOT_REJECTED;
import static com.box.l10n.mojito.service.tm.search.StatusFilter.FOR_TRANSLATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTestData;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantService;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class TextUnitDTOsCacheServiceTest extends ServiceTestBase {

  static Logger logger = LoggerFactory.getLogger(TextUnitDTOsCacheServiceTest.class);

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired TextUnitDTOsCacheService textUnitDTOsCacheService;

  @Autowired TMTextUnitCurrentVariantService tmTextUnitCurrentVariantService;

  @Autowired LocaleService localeService;

  @Autowired TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

  @Autowired TMService tmService;

  @Autowired AssetService assetService;

  @Autowired TextUnitDTOsCacheBlobStorage textUnitDTOsCacheBlobStorage;

  @Test
  public void testNoValueInCacheNoUpdate() {
    TMTestData tmTestData = new TMTestData(testIdWatcher);
    textUnitDTOsCacheBlobStorage.redisStructuredBlobStorageProxy.delete(
        StructuredBlobStorage.Prefix.TEXT_UNIT_DTOS_CACHE,
        textUnitDTOsCacheBlobStorage.getName(tmTestData.asset.getId(), tmTestData.en.getId()));
    ImmutableMap<String, TextUnitDTO> withoutUpdate =
        textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(
            tmTestData.asset.getId(), tmTestData.en.getId(), null, true, UpdateType.NEVER);
    assertThat(withoutUpdate)
        .as("Without cache update the initial call should return empty map")
        .isEmpty();
  }

  @Test
  public void testNoValueInCacheUpdateIfMissing() {
    TMTestData tmTestData = new TMTestData(testIdWatcher);
    textUnitDTOsCacheBlobStorage.redisStructuredBlobStorageProxy.delete(
        StructuredBlobStorage.Prefix.TEXT_UNIT_DTOS_CACHE,
        textUnitDTOsCacheBlobStorage.getName(tmTestData.asset.getId(), tmTestData.en.getId()));
    ImmutableMap<String, TextUnitDTO> updateIfMissing =
        textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(
            tmTestData.asset.getId(), tmTestData.en.getId(), null, true, UpdateType.IF_MISSING);
    assertThat(updateIfMissing.values().stream())
        .as("Update from database if missing entry in the cache")
        .extracting(TextUnitDTO::getName, TextUnitDTO::getStatus, TextUnitDTO::isUsed)
        .containsExactly(
            tuple("zuora_error_message_verify_state_province", APPROVED, true),
            tuple("TEST2", APPROVED, true),
            tuple("TEST3", APPROVED, false));
  }

  @Test
  public void testRootLocaleWithPlurals() {
    TMTestData tmTestData = new TMTestData(testIdWatcher);
    tmTestData.addPluralString("plural_1");

    ImmutableMap<String, TextUnitDTO> textUnitDTOsForAssetAndLocaleByMD5 =
        textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(
            tmTestData.asset.getId(), tmTestData.en.getId(), null, true, UpdateType.ALWAYS);

    assertThat(textUnitDTOsForAssetAndLocaleByMD5.values().stream())
        .as("All text units with plural unfiltered (root locale)")
        .extracting(TextUnitDTO::getName, TextUnitDTO::getStatus, TextUnitDTO::isUsed)
        .containsExactly(
            tuple("zuora_error_message_verify_state_province", APPROVED, true),
            tuple("TEST2", APPROVED, true),
            tuple("TEST3", APPROVED, false),
            tuple("plural_1_other", APPROVED, false),
            tuple("plural_1_zero", APPROVED, false),
            tuple("plural_1_one", APPROVED, false),
            tuple("plural_1_two", APPROVED, false),
            tuple("plural_1_few", APPROVED, false),
            tuple("plural_1_many", APPROVED, false));

    assertThat(textUnitDTOsForAssetAndLocaleByMD5.values().stream())
        .extracting(
            TextUnitDTO::getTargetLocale, TextUnitDTO::isAssetDeleted, TextUnitDTO::getAssetId)
        .containsOnly(tuple("en", false, tmTestData.asset.getId()));
  }

  @Test
  public void testStandardLocaleWithPlural() {
    TMTestData tmTestData = new TMTestData(testIdWatcher);
    ImmutableMap<String, TMTextUnit> plural1 = tmTestData.addPluralString("plural_1");

    ImmutableMap<String, TextUnitDTO> textUnitDTOsForAssetAndLocaleByMD5 =
        textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(
            tmTestData.asset.getId(), tmTestData.frFR.getId(), null, false, UpdateType.ALWAYS);
    assertThat(textUnitDTOsForAssetAndLocaleByMD5.values().stream())
        .as("All text units with plural froms filtered (normal locale)")
        .extracting(TextUnitDTO::getName, TextUnitDTO::getStatus)
        .containsExactly(
            tuple("zuora_error_message_verify_state_province", APPROVED),
            tuple("TEST2", TRANSLATION_NEEDED),
            tuple("TEST3", APPROVED),
            tuple("plural_1_other", TRANSLATION_NEEDED),
            tuple("plural_1_one", TRANSLATION_NEEDED));
    assertThat(textUnitDTOsForAssetAndLocaleByMD5.values().stream())
        .extracting(
            TextUnitDTO::getTargetLocale, TextUnitDTO::isAssetDeleted, TextUnitDTO::getAssetId)
        .containsOnly(tuple("fr-FR", false, tmTestData.asset.getId()));

    textUnitDTOsForAssetAndLocaleByMD5 =
        textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(
            tmTestData.asset.getId(),
            tmTestData.frFR.getId(),
            FOR_TRANSLATION,
            false,
            UpdateType.ALWAYS);
    assertThat(textUnitDTOsForAssetAndLocaleByMD5.values().stream())
        .as("Only strings that need translation")
        .extracting(TextUnitDTO::getName, TextUnitDTO::getStatus)
        .containsExactly(
            tuple("TEST2", TRANSLATION_NEEDED),
            tuple("plural_1_other", TRANSLATION_NEEDED),
            tuple("plural_1_one", TRANSLATION_NEEDED));

    textUnitDTOsForAssetAndLocaleByMD5 =
        textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(
            tmTestData.asset.getId(),
            tmTestData.frFR.getId(),
            APPROVED_AND_NOT_REJECTED,
            false,
            UpdateType.ALWAYS);
    assertThat(textUnitDTOsForAssetAndLocaleByMD5.values().stream())
        .as("Only strings that are approved")
        .extracting(TextUnitDTO::getName, TextUnitDTO::getStatus, TextUnitDTO::getTarget)
        .containsExactly(
            tuple(
                "zuora_error_message_verify_state_province",
                APPROVED,
                "Veuillez indiquer un état, une région ou une province valide."),
            tuple("TEST3", APPROVED, "Content3 fr-FR"));

    Long idToRemove =
        tmTextUnitCurrentVariantRepository
            .findByLocale_IdAndTmTextUnit_Id(
                tmTestData.frFR.getId(), tmTestData.addTMTextUnit3.getId())
            .getId();

    tmService.addCurrentTMTextUnitVariant(
        tmTestData.addTMTextUnit1.getId(),
        tmTestData.frFR.getId(),
        "Veuillez indiquer un état, une région ou une province valide. - update");
    tmTextUnitCurrentVariantService.removeCurrentVariant(idToRemove);
    tmService.addCurrentTMTextUnitVariant(
        plural1.get("one").getId(), tmTestData.frFR.getId(), "plural1 - one - fr");
    textUnitDTOsForAssetAndLocaleByMD5 =
        textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(
            tmTestData.asset.getId(), tmTestData.frFR.getId(), null, false, UpdateType.ALWAYS);
    assertThat(textUnitDTOsForAssetAndLocaleByMD5.values().stream())
        .as("Add, update and delete translations")
        .extracting(TextUnitDTO::getName, TextUnitDTO::getStatus, TextUnitDTO::getTarget)
        .containsExactly(
            tuple(
                "zuora_error_message_verify_state_province",
                APPROVED,
                "Veuillez indiquer un état, une région ou une province valide. - update"),
            tuple("TEST2", TRANSLATION_NEEDED, null),
            tuple("TEST3", TRANSLATION_NEEDED, null),
            tuple("plural_1_other", TRANSLATION_NEEDED, null),
            tuple("plural_1_one", APPROVED, "plural1 - one - fr"));

    assetService.deleteAsset(tmTestData.asset);
    textUnitDTOsForAssetAndLocaleByMD5 =
        textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(
            tmTestData.asset.getId(), tmTestData.frFR.getId(), null, false, UpdateType.ALWAYS);
    assertThat(textUnitDTOsForAssetAndLocaleByMD5.values().stream())
        .as("All should be marked as deleted")
        .extracting(TextUnitDTO::isAssetDeleted)
        .containsOnly(true);
  }

  @Test
  public void testDeleteAsset() {
    TMTestData tmTestData = new TMTestData(testIdWatcher);

    assetService.deleteAsset(tmTestData.asset);

    ImmutableMap<String, TextUnitDTO> textUnitDTOsForAssetAndLocaleByMD5 =
        textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(
            tmTestData.asset.getId(), tmTestData.en.getId(), null, true, UpdateType.ALWAYS);

    assertThat(textUnitDTOsForAssetAndLocaleByMD5.values().stream())
        .extracting(TextUnitDTO::getName, TextUnitDTO::getStatus)
        .containsExactly(
            tuple("zuora_error_message_verify_state_province", APPROVED),
            tuple("TEST2", APPROVED),
            tuple("TEST3", APPROVED));

    assertThat(textUnitDTOsForAssetAndLocaleByMD5.values().stream())
        .extracting(
            TextUnitDTO::getTargetLocale, TextUnitDTO::isAssetDeleted, TextUnitDTO::getAssetId)
        .containsOnly(tuple("en", true, tmTestData.asset.getId()));
  }

  @Test
  public void fetchFromDatabaseIfduplicatedMd5InCache() {
    TextUnitDTOsCacheService textUnitDTOsCacheService = spy(new TextUnitDTOsCacheService());
    textUnitDTOsCacheService.textUnitUtils = new TextUnitUtils();
    TextUnitDTOsCacheBlobStorage textUnitDTOsCacheBlobStorageMock =
        mock(TextUnitDTOsCacheBlobStorage.class);
    textUnitDTOsCacheService.textUnitDTOsCacheBlobStorage = textUnitDTOsCacheBlobStorageMock;

    when(textUnitDTOsCacheBlobStorageMock.getTextUnitDTOs(any(), any()))
        .thenReturn(Optional.of(ImmutableList.of(new TextUnitDTO(), new TextUnitDTO())));

    TextUnitDTO textUnitDTO1 = new TextUnitDTO();
    textUnitDTO1.setName("name1");

    TextUnitDTO textUnitDTO2 = new TextUnitDTO();
    textUnitDTO2.setName("name2");

    Mockito.doReturn(ImmutableList.of(textUnitDTO1, textUnitDTO2))
        .when(textUnitDTOsCacheService)
        .updateTextUnitDTOsWithDeltaFromDatabase(any(), any(), any(), anyBoolean());

    final ImmutableMap<String, TextUnitDTO> byMd5 =
        textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(
            1L, 1L, StatusFilter.ALL, false, UpdateType.NEVER);

    assertThat(byMd5).containsValues(textUnitDTO1, textUnitDTO2);
  }
}
