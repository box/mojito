package com.box.l10n.mojito.service.tm.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import org.junit.Test;

public class TextUnitSearcherParametersTest {
  @Test
  public void testBuilder() {
    TextUnitSearcherParameters textUnitSearcherParameters =
        new TextUnitSearcherParameters.Builder().build();
    assertThat(textUnitSearcherParameters.getName()).isNull();
    assertThat(textUnitSearcherParameters.getSource()).isNull();
    assertThat(textUnitSearcherParameters.getTarget()).isNull();
    assertThat(textUnitSearcherParameters.getAssetPath()).isNull();
    assertThat(textUnitSearcherParameters.getPluralFormOther()).isNull();
    assertThat(textUnitSearcherParameters.getLocationUsage()).isNull();
    assertThat(textUnitSearcherParameters.getSearchType()).isNull();
    assertThat(textUnitSearcherParameters.getRepositoryIds()).isNull();
    assertThat(textUnitSearcherParameters.getRepositoryNames()).isNull();
    assertThat(textUnitSearcherParameters.getTmTextUnitIds()).isNull();
    assertThat(textUnitSearcherParameters.getLocaleTags()).isNull();
    assertThat(textUnitSearcherParameters.getLocaleId()).isNull();
    assertThat(textUnitSearcherParameters.getUsedFilter()).isNull();
    assertThat(textUnitSearcherParameters.getStatusFilter()).isNull();
    assertThat(textUnitSearcherParameters.getOffset()).isNull();
    assertThat(textUnitSearcherParameters.getLimit()).isNull();
    assertThat(textUnitSearcherParameters.getAssetId()).isNull();
    assertThat(textUnitSearcherParameters.getTmId()).isNull();
    assertThat(textUnitSearcherParameters.getMd5()).isNull();
    assertThat(textUnitSearcherParameters.isForRootLocale()).isFalse();
    assertThat(textUnitSearcherParameters.isRootLocaleExcluded()).isTrue();
    assertThat(textUnitSearcherParameters.getToBeFullyTranslatedFilter()).isNull();
    assertThat(textUnitSearcherParameters.isPluralFormsFiltered()).isTrue();
    assertThat(textUnitSearcherParameters.isPluralFormsExcluded()).isFalse();
    assertThat(textUnitSearcherParameters.isOrderedByTextUnitID()).isFalse();
    assertThat(textUnitSearcherParameters.getPluralFormId()).isNull();
    assertThat(textUnitSearcherParameters.getDoNotTranslateFilter()).isNull();
    assertThat(textUnitSearcherParameters.getTmTextUnitCreatedBefore()).isNull();
    assertThat(textUnitSearcherParameters.getTmTextUnitCreatedAfter()).isNull();
    assertThat(textUnitSearcherParameters.getBranchId()).isNull();
    assertThat(textUnitSearcherParameters.getSkipTextUnitWithPattern()).isNull();
    assertThat(textUnitSearcherParameters.getIncludeTextUnitsWithPattern()).isNull();
    assertThat(textUnitSearcherParameters.getSkipAssetPathWithPattern()).isNull();
    assertThat(textUnitSearcherParameters.isExcludeUnexpiredPendingMT()).isFalse();
    assertThat(textUnitSearcherParameters.getAiTranslationExpiryDuration()).isNull();

    ZonedDateTime now = ZonedDateTime.now();
    textUnitSearcherParameters =
        new TextUnitSearcherParameters.Builder()
            .name("testName")
            .source("testSource")
            .target("testTarget")
            .assetPath("testAssetPath")
            .pluralFormOther("testPluralFormOther")
            .locationUsage("testLocationUsage")
            .searchType(SearchType.CONTAINS)
            .repositoryId(1L)
            .repositoryNames(Arrays.asList("testRepository1", "testRepository2"))
            .tmTextUnitIds(null, null, 2L, 3L)
            .localeTags(Arrays.asList("ar-SA", "es-ES"))
            .localeId(4L)
            .usedFilter(UsedFilter.USED)
            .statusFilter(StatusFilter.FOR_TRANSLATION)
            .offset(10)
            .limit(20)
            .assetId(5L)
            .tmId(6L)
            .md5("md5")
            .pluralFormId(7L)
            .tmTextUnitCreatedBefore(now)
            .tmTextUnitCreatedAfter(now.minusDays(2))
            .branchId(8L)
            .skipTextUnitWithPattern("testSkipTextUnitWithPattern")
            .includeTextUnitsWithPattern("testSkipTextUnitWithPattern")
            .skipAssetPathWithPattern("testSkipAssetPathWithPattern")
            .aiTranslationExpiryDuration(Duration.ofMillis(1000))
            .build();

    assertThat(textUnitSearcherParameters.getName()).isEqualTo("testName");
    assertThat(textUnitSearcherParameters.getSource()).isEqualTo("testSource");
    assertThat(textUnitSearcherParameters.getTarget()).isEqualTo("testTarget");
    assertThat(textUnitSearcherParameters.getAssetPath()).isEqualTo("testAssetPath");
    assertThat(textUnitSearcherParameters.getPluralFormOther()).isEqualTo("testPluralFormOther");
    assertThat(textUnitSearcherParameters.getLocationUsage()).isEqualTo("testLocationUsage");
    assertThat(textUnitSearcherParameters.getSearchType()).isEqualTo(SearchType.CONTAINS);
    assertThat(textUnitSearcherParameters.getRepositoryIds().getFirst()).isEqualTo(1L);
    assertThat(textUnitSearcherParameters.getRepositoryNames().getFirst())
        .isEqualTo("testRepository1");
    assertThat(textUnitSearcherParameters.getRepositoryNames().get(1)).isEqualTo("testRepository2");
    assertThat(textUnitSearcherParameters.getTmTextUnitIds().getFirst()).isEqualTo(2L);
    assertThat(textUnitSearcherParameters.getTmTextUnitIds().get(1)).isEqualTo(3L);
    assertThat(textUnitSearcherParameters.getLocaleTags().getFirst()).isEqualTo("ar-SA");
    assertThat(textUnitSearcherParameters.getLocaleTags().get(1)).isEqualTo("es-ES");
    assertThat(textUnitSearcherParameters.getLocaleId()).isEqualTo(4L);
    assertThat(textUnitSearcherParameters.getUsedFilter()).isEqualTo(UsedFilter.USED);
    assertThat(textUnitSearcherParameters.getStatusFilter())
        .isEqualTo(StatusFilter.FOR_TRANSLATION);
    assertThat(textUnitSearcherParameters.getOffset()).isEqualTo(10);
    assertThat(textUnitSearcherParameters.getLimit()).isEqualTo(20);
    assertThat(textUnitSearcherParameters.getAssetId()).isEqualTo(5L);
    assertThat(textUnitSearcherParameters.getTmId()).isEqualTo(6L);
    assertThat(textUnitSearcherParameters.getMd5()).isEqualTo("md5");
    assertThat(textUnitSearcherParameters.getPluralFormId()).isEqualTo(7L);
    assertThat(textUnitSearcherParameters.getTmTextUnitCreatedBefore()).isEqualTo(now);
    assertThat(textUnitSearcherParameters.getTmTextUnitCreatedAfter()).isEqualTo(now.minusDays(2));
    assertThat(textUnitSearcherParameters.getBranchId()).isEqualTo(8L);
    assertThat(textUnitSearcherParameters.getSkipTextUnitWithPattern())
        .isEqualTo("testSkipTextUnitWithPattern");
    assertThat(textUnitSearcherParameters.getIncludeTextUnitsWithPattern())
        .isEqualTo("testSkipTextUnitWithPattern");
    assertThat(textUnitSearcherParameters.getSkipAssetPathWithPattern())
        .isEqualTo("testSkipAssetPathWithPattern");
    assertThat(textUnitSearcherParameters.getAiTranslationExpiryDuration())
        .isEqualTo(Duration.ofMillis(1000));

    assertThat(textUnitSearcherParameters.isForRootLocale()).isFalse();
    assertThat(textUnitSearcherParameters.isRootLocaleExcluded()).isTrue();
    assertThat(textUnitSearcherParameters.getToBeFullyTranslatedFilter()).isNull();
    assertThat(textUnitSearcherParameters.isPluralFormsFiltered()).isTrue();
    assertThat(textUnitSearcherParameters.isPluralFormsExcluded()).isFalse();
    assertThat(textUnitSearcherParameters.isOrderedByTextUnitID()).isFalse();
    assertThat(textUnitSearcherParameters.getDoNotTranslateFilter()).isNull();
    assertThat(textUnitSearcherParameters.isExcludeUnexpiredPendingMT()).isFalse();

    textUnitSearcherParameters =
        new TextUnitSearcherParameters.Builder().forRootLocale(true).build();

    assertThat(textUnitSearcherParameters.isForRootLocale()).isTrue();
    assertThat(textUnitSearcherParameters.isRootLocaleExcluded()).isTrue();
    assertThat(textUnitSearcherParameters.getToBeFullyTranslatedFilter()).isNull();
    assertThat(textUnitSearcherParameters.isPluralFormsFiltered()).isTrue();
    assertThat(textUnitSearcherParameters.isPluralFormsExcluded()).isFalse();
    assertThat(textUnitSearcherParameters.isOrderedByTextUnitID()).isFalse();
    assertThat(textUnitSearcherParameters.getDoNotTranslateFilter()).isNull();
    assertThat(textUnitSearcherParameters.isExcludeUnexpiredPendingMT()).isFalse();

    textUnitSearcherParameters =
        new TextUnitSearcherParameters.Builder().rootLocaleExcluded(false).build();

    assertThat(textUnitSearcherParameters.isForRootLocale()).isFalse();
    assertThat(textUnitSearcherParameters.isRootLocaleExcluded()).isFalse();
    assertThat(textUnitSearcherParameters.getToBeFullyTranslatedFilter()).isNull();
    assertThat(textUnitSearcherParameters.isPluralFormsFiltered()).isTrue();
    assertThat(textUnitSearcherParameters.isPluralFormsExcluded()).isFalse();
    assertThat(textUnitSearcherParameters.isOrderedByTextUnitID()).isFalse();
    assertThat(textUnitSearcherParameters.getDoNotTranslateFilter()).isNull();
    assertThat(textUnitSearcherParameters.isExcludeUnexpiredPendingMT()).isFalse();

    textUnitSearcherParameters =
        new TextUnitSearcherParameters.Builder().toBeFullyTranslatedFilter(true).build();

    assertThat(textUnitSearcherParameters.isForRootLocale()).isFalse();
    assertThat(textUnitSearcherParameters.isRootLocaleExcluded()).isTrue();
    assertThat(textUnitSearcherParameters.getToBeFullyTranslatedFilter()).isTrue();
    assertThat(textUnitSearcherParameters.isPluralFormsFiltered()).isTrue();
    assertThat(textUnitSearcherParameters.isPluralFormsExcluded()).isFalse();
    assertThat(textUnitSearcherParameters.isOrderedByTextUnitID()).isFalse();
    assertThat(textUnitSearcherParameters.getDoNotTranslateFilter()).isNull();
    assertThat(textUnitSearcherParameters.isExcludeUnexpiredPendingMT()).isFalse();

    textUnitSearcherParameters =
        new TextUnitSearcherParameters.Builder().pluralFormsFiltered(false).build();

    assertThat(textUnitSearcherParameters.isForRootLocale()).isFalse();
    assertThat(textUnitSearcherParameters.isRootLocaleExcluded()).isTrue();
    assertThat(textUnitSearcherParameters.getToBeFullyTranslatedFilter()).isNull();
    assertThat(textUnitSearcherParameters.isPluralFormsFiltered()).isFalse();
    assertThat(textUnitSearcherParameters.isPluralFormsExcluded()).isFalse();
    assertThat(textUnitSearcherParameters.isOrderedByTextUnitID()).isFalse();
    assertThat(textUnitSearcherParameters.getDoNotTranslateFilter()).isNull();
    assertThat(textUnitSearcherParameters.isExcludeUnexpiredPendingMT()).isFalse();

    textUnitSearcherParameters =
        new TextUnitSearcherParameters.Builder().pluralFormsExcluded(true).build();

    assertThat(textUnitSearcherParameters.isForRootLocale()).isFalse();
    assertThat(textUnitSearcherParameters.isRootLocaleExcluded()).isTrue();
    assertThat(textUnitSearcherParameters.getToBeFullyTranslatedFilter()).isNull();
    assertThat(textUnitSearcherParameters.isPluralFormsFiltered()).isTrue();
    assertThat(textUnitSearcherParameters.isPluralFormsExcluded()).isTrue();
    assertThat(textUnitSearcherParameters.isOrderedByTextUnitID()).isFalse();
    assertThat(textUnitSearcherParameters.getDoNotTranslateFilter()).isNull();
    assertThat(textUnitSearcherParameters.isExcludeUnexpiredPendingMT()).isFalse();

    textUnitSearcherParameters =
        new TextUnitSearcherParameters.Builder().isOrderedByTextUnitID(true).build();

    assertThat(textUnitSearcherParameters.isForRootLocale()).isFalse();
    assertThat(textUnitSearcherParameters.isRootLocaleExcluded()).isTrue();
    assertThat(textUnitSearcherParameters.getToBeFullyTranslatedFilter()).isNull();
    assertThat(textUnitSearcherParameters.isPluralFormsFiltered()).isTrue();
    assertThat(textUnitSearcherParameters.isPluralFormsExcluded()).isFalse();
    assertThat(textUnitSearcherParameters.isOrderedByTextUnitID()).isTrue();
    assertThat(textUnitSearcherParameters.getDoNotTranslateFilter()).isNull();
    assertThat(textUnitSearcherParameters.isExcludeUnexpiredPendingMT()).isFalse();

    textUnitSearcherParameters =
        new TextUnitSearcherParameters.Builder().doNotTranslateFilter(true).build();

    assertThat(textUnitSearcherParameters.isForRootLocale()).isFalse();
    assertThat(textUnitSearcherParameters.isRootLocaleExcluded()).isTrue();
    assertThat(textUnitSearcherParameters.getToBeFullyTranslatedFilter()).isNull();
    assertThat(textUnitSearcherParameters.isPluralFormsFiltered()).isTrue();
    assertThat(textUnitSearcherParameters.isPluralFormsExcluded()).isFalse();
    assertThat(textUnitSearcherParameters.isOrderedByTextUnitID()).isFalse();
    assertThat(textUnitSearcherParameters.getDoNotTranslateFilter()).isTrue();
    assertThat(textUnitSearcherParameters.isExcludeUnexpiredPendingMT()).isFalse();

    textUnitSearcherParameters =
        new TextUnitSearcherParameters.Builder().isExcludeUnexpiredPendingMT(true).build();

    assertThat(textUnitSearcherParameters.isForRootLocale()).isFalse();
    assertThat(textUnitSearcherParameters.isRootLocaleExcluded()).isTrue();
    assertThat(textUnitSearcherParameters.getToBeFullyTranslatedFilter()).isNull();
    assertThat(textUnitSearcherParameters.isPluralFormsFiltered()).isTrue();
    assertThat(textUnitSearcherParameters.isPluralFormsExcluded()).isFalse();
    assertThat(textUnitSearcherParameters.isOrderedByTextUnitID()).isFalse();
    assertThat(textUnitSearcherParameters.getDoNotTranslateFilter()).isNull();
    assertThat(textUnitSearcherParameters.isExcludeUnexpiredPendingMT()).isTrue();
  }
}
