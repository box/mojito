package com.box.l10n.mojito.service.assetExtraction;

import static com.box.l10n.mojito.service.assetExtraction.LocalBranchToEntityBranchConverter.NULL_BRANCH_DATE_PLACEHODLER;
import static com.box.l10n.mojito.service.assetExtraction.LocalBranchToEntityBranchConverter.NULL_BRANCH_TEXT_PLACEHOLDER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.localtm.merger.Branch;
import com.box.l10n.mojito.localtm.merger.BranchData;
import com.box.l10n.mojito.localtm.merger.BranchStateTextUnit;
import com.box.l10n.mojito.localtm.merger.MultiBranchState;
import com.box.l10n.mojito.service.DBUtils;
import com.box.l10n.mojito.service.tm.TMTestData;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class MultiBranchStateServiceTest extends ServiceTestBase {

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired MultiBranchStateService multiBranchStateService;

  @Autowired MultiBranchStateBlobStorage multiBranchStateBlobStorage;

  @Autowired DBUtils dbUtils;

  @Test
  public void getMultiBranchStateForAssetExtractionId() throws InterruptedException {
    TMTestData tmTestData = new TMTestData(testIdWatcher);

    Optional<MultiBranchState> multiBranchStateForAssetExtractionBefore =
        multiBranchStateBlobStorage.getMultiBranchStateForAssetExtractionId(
            tmTestData.assetExtraction.getId(), 0L);
    assertFalse(multiBranchStateForAssetExtractionBefore.isPresent());

    MultiBranchState multiBranchStateForAssetExtractionId =
        multiBranchStateService.getMultiBranchStateForAssetExtractionId(
            tmTestData.assetExtraction.getId(), 0L);

    MultiBranchState expectedMultiBranchState =
        MultiBranchState.of()
            .withBranches(
                ImmutableSet.of(
                    Branch.builder()
                        .name(NULL_BRANCH_TEXT_PLACEHOLDER)
                        .createdAt(NULL_BRANCH_DATE_PLACEHODLER)
                        .build()))
            .withBranchStateTextUnits(
                ImmutableList.of(
                    convertTmTextUnitToBranchStateTextUnit(
                        tmTestData.createAssetTextUnit1, tmTestData.addTMTextUnit1.getId()),
                    convertTmTextUnitToBranchStateTextUnit(
                        tmTestData.createAssetTextUnit2, tmTestData.addTMTextUnit2.getId())));
    expectedMultiBranchState = roundDateTimesIfMysql(expectedMultiBranchState);

    Assertions.assertThat(multiBranchStateForAssetExtractionId)
        .usingRecursiveComparison()
        .isEqualTo(expectedMultiBranchState);

    Optional<MultiBranchState> multiBranchStateForAssetExtractionAfter =
        multiBranchStateBlobStorage.getMultiBranchStateForAssetExtractionId(
            tmTestData.assetExtraction.getId(), 0L);
    assertTrue(multiBranchStateForAssetExtractionAfter.isPresent());
  }

  /**
   * Round date times, to make the test work with Mysql.
   *
   * <p>On current Mysql schema as precision of second for dates, the test fails with date precision
   * issue without rounding here - ignore for now - update the schema to have proper precision later
   *
   * @param expectedMultiBranchState
   * @return
   */
  MultiBranchState roundDateTimesIfMysql(MultiBranchState expectedMultiBranchState) {

    if (dbUtils.isMysql()) {
      expectedMultiBranchState =
          expectedMultiBranchState.withBranchStateTextUnits(
              expectedMultiBranchState.getBranchStateTextUnits().stream()
                  .map(bstu -> bstu.withCreatedDate(roundDateTimeToSecond(bstu.getCreatedDate())))
                  .collect(ImmutableList.toImmutableList()));

      expectedMultiBranchState =
          expectedMultiBranchState.withBranches(
              expectedMultiBranchState.getBranches().stream()
                  .map(b -> b.withCreatedAt(roundDateTimeToSecond(b.getCreatedAt())))
                  .collect(ImmutableSet.toImmutableSet()));
    } else {
      expectedMultiBranchState =
          expectedMultiBranchState.withBranchStateTextUnits(
              expectedMultiBranchState.getBranchStateTextUnits().stream()
                  .map(
                      bstu ->
                          bstu.withCreatedDate(
                              bstu.getCreatedDate().truncatedTo(ChronoUnit.MICROS)))
                  .collect(ImmutableList.toImmutableList()));

      expectedMultiBranchState =
          expectedMultiBranchState.withBranches(
              expectedMultiBranchState.getBranches().stream()
                  .map(b -> b.withCreatedAt(b.getCreatedAt().truncatedTo(ChronoUnit.MICROS)))
                  .collect(ImmutableSet.toImmutableSet()));
    }
    return expectedMultiBranchState;
  }

  ZonedDateTime roundDateTimeToSecond(ZonedDateTime dateTime) {

    if (JSR310Migration.dateTimeGetMillisOfSecond(dateTime) > 500) {
      dateTime = JSR310Migration.dateTimeWithMillisOfSeconds(dateTime, 0).plusSeconds(1);
    } else {
      dateTime = JSR310Migration.dateTimeWithMillisOfSeconds(dateTime, 0);
    }

    return dateTime;
  }

  BranchStateTextUnit convertTmTextUnitToBranchStateTextUnit(
      AssetTextUnit assetTextUnit, Long tmTextUnitId) {
    return BranchStateTextUnit.builder()
        .tmTextUnitId(tmTextUnitId)
        .assetTextUnitId(assetTextUnit.getId())
        .name(assetTextUnit.getName())
        .md5(assetTextUnit.getMd5())
        .source(assetTextUnit.getContent())
        .comments(assetTextUnit.getComment())
        .createdDate(assetTextUnit.getCreatedDate())
        .branchNameToBranchDatas(ImmutableMap.of(NULL_BRANCH_TEXT_PLACEHOLDER, BranchData.of()))
        .build();
  }
}
