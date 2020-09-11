package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.textunitdtocache.TextUnitDTOsCacheService;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MoreCollectors;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.checkerframework.checker.units.qual.A;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static com.box.l10n.mojito.entity.TMTextUnitVariant.Status.APPROVED;
import static com.box.l10n.mojito.entity.TMTextUnitVariant.Status.TRANSLATION_NEEDED;
import static com.box.l10n.mojito.service.tm.search.StatusFilter.APPROVED_AND_NOT_REJECTED;
import static com.box.l10n.mojito.service.tm.search.StatusFilter.FOR_TRANSLATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class TextUnitDTOsCacheServiceTest extends ServiceTestBase {

    static Logger logger = LoggerFactory.getLogger(TextUnitDTOsCacheServiceTest.class);

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Autowired
    TextUnitDTOsCacheService textUnitDTOsCacheService;

    @Autowired
    TMTextUnitCurrentVariantService tmTextUnitCurrentVariantService;

    @Test
    public void testEmpty() {
        TMTestData tmTestData = new TMTestData(testIdWatcher);
        // use zxx locale to minimise collision if persitent blob storage is used
        Locale zxx = tmTestData.localeService.findByBcp47Tag("zxx");
        ImmutableMap<String, TextUnitDTO> withoutUpdate = textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(tmTestData.asset.getId(), zxx.getId(), null, true, false);
        assertThat(withoutUpdate)
                .as("Without cache update the initial call should return nothing (if test fails make sure you don't use a DB with persistence and ID overlap)")
                .isEmpty();
    }

    @Test
    public void testRootLocaleWithPlurals() {
        TMTestData tmTestData = new TMTestData(testIdWatcher);
        tmTestData.addPluralString("plural_1");

        ImmutableMap<String, TextUnitDTO> textUnitDTOsForAssetAndLocaleByMD5 = textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(tmTestData.asset.getId(), tmTestData.en.getId(), null, true, true);

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
                .extracting(TextUnitDTO::getTargetLocale, TextUnitDTO::isAssetDeleted, TextUnitDTO::getAssetId)
                .containsOnly(tuple("en", false, tmTestData.asset.getId()));
    }

    @Test
    public void testStandardLocaleWithPlural() {
        TMTestData tmTestData = new TMTestData(testIdWatcher);
        ImmutableMap<String, TMTextUnit> plural1 = tmTestData.addPluralString("plural_1");

        ImmutableMap<String, TextUnitDTO> textUnitDTOsForAssetAndLocaleByMD5 = textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(tmTestData.asset.getId(), tmTestData.frFR.getId(), null, false, true);
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
                .extracting(TextUnitDTO::getTargetLocale, TextUnitDTO::isAssetDeleted, TextUnitDTO::getAssetId)
                .containsOnly(tuple("fr-FR", false, tmTestData.asset.getId()));

        textUnitDTOsForAssetAndLocaleByMD5 = textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(tmTestData.asset.getId(), tmTestData.frFR.getId(), FOR_TRANSLATION, false, true);
        assertThat(textUnitDTOsForAssetAndLocaleByMD5.values().stream())
                .as("Only strings that need translation")
                .extracting(TextUnitDTO::getName, TextUnitDTO::getStatus)
                .containsExactly(
                        tuple("TEST2", TRANSLATION_NEEDED),
                        tuple("plural_1_other", TRANSLATION_NEEDED),
                        tuple("plural_1_one", TRANSLATION_NEEDED));

        textUnitDTOsForAssetAndLocaleByMD5 = textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(tmTestData.asset.getId(), tmTestData.frFR.getId(), APPROVED_AND_NOT_REJECTED, false, true);
        assertThat(textUnitDTOsForAssetAndLocaleByMD5.values().stream())
                .as("Only strings that are approved")
                .extracting(TextUnitDTO::getName, TextUnitDTO::getStatus, TextUnitDTO::getTarget)
                .containsExactly(
                        tuple("zuora_error_message_verify_state_province", APPROVED, "Veuillez indiquer un état, une région ou une province valide."),
                        tuple("TEST3", APPROVED, "Content3 fr-FR")
                );


        tmTestData.tmService.addCurrentTMTextUnitVariant(tmTestData.addTMTextUnit1.getId(), tmTestData.frFR.getId(), "Veuillez indiquer un état, une région ou une province valide. - update");
        tmTextUnitCurrentVariantService.removeCurrentVariant(tmTestData.addCurrentTMTextUnitVariant3FrFR.getId());
        tmTestData.tmService.addCurrentTMTextUnitVariant(plural1.get("one").getId(), tmTestData.frFR.getId(), "plural1 - one - fr");
        textUnitDTOsForAssetAndLocaleByMD5 = textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(tmTestData.asset.getId(), tmTestData.frFR.getId(), null, false, true);
        assertThat(textUnitDTOsForAssetAndLocaleByMD5.values().stream())
                .as("Add, update and delete translations")
                .extracting(TextUnitDTO::getName, TextUnitDTO::getStatus, TextUnitDTO::getTarget)
                .containsExactly(
                        tuple("zuora_error_message_verify_state_province", APPROVED, "Veuillez indiquer un état, une région ou une province valide. - update"),
                        tuple("TEST2", TRANSLATION_NEEDED, null),
                        tuple("TEST3", TRANSLATION_NEEDED, null),
                        tuple("plural_1_other", TRANSLATION_NEEDED, null),
                        tuple("plural_1_one", APPROVED, "plural1 - one - fr")
                );

        tmTestData.assetService.deleteAsset(tmTestData.asset);
        textUnitDTOsForAssetAndLocaleByMD5 = textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(tmTestData.asset.getId(), tmTestData.frFR.getId(), null, false, true);
        assertThat(textUnitDTOsForAssetAndLocaleByMD5.values().stream())
                .as("All should be marked as deleted")
                .extracting(TextUnitDTO::isAssetDeleted)
                .containsOnly(true);
    }

    @Test
    public void testDeleteAsset() {
        TMTestData tmTestData = new TMTestData(testIdWatcher);

        tmTestData.assetService.deleteAsset(tmTestData.asset);

        ImmutableMap<String, TextUnitDTO> textUnitDTOsForAssetAndLocaleByMD5 = textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(tmTestData.asset.getId(), tmTestData.en.getId(), null, true, true);

        assertThat(textUnitDTOsForAssetAndLocaleByMD5.values().stream())
                .extracting(TextUnitDTO::getName, TextUnitDTO::getStatus)
                .containsExactly(
                        tuple("zuora_error_message_verify_state_province", APPROVED),
                        tuple("TEST2", APPROVED),
                        tuple("TEST3", APPROVED));

        assertThat(textUnitDTOsForAssetAndLocaleByMD5.values().stream())
                .extracting(TextUnitDTO::getTargetLocale, TextUnitDTO::isAssetDeleted, TextUnitDTO::getAssetId)
                .containsOnly(tuple("en", true, tmTestData.asset.getId()));
    }

}