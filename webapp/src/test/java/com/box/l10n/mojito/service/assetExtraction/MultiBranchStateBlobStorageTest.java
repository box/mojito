package com.box.l10n.mojito.service.assetExtraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.localtm.merger.Branch;
import com.box.l10n.mojito.localtm.merger.BranchData;
import com.box.l10n.mojito.localtm.merger.BranchStateTextUnit;
import com.box.l10n.mojito.localtm.merger.MultiBranchState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.Comparator;
import java.util.Optional;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

public class MultiBranchStateBlobStorageTest extends ServiceTestBase {

  static Logger logger = getLogger(MultiBranchStateBlobStorageTest.class);

  @Autowired MultiBranchStateBlobStorage multiBranchStateBlobStorage;

  @Test
  public void test() {
    long assetExtractionId = 123456789123L;
    long version = 1L;

    multiBranchStateBlobStorage.deleteMultiBranchStateForAssetExtractionId(
        assetExtractionId, version);
    Optional<MultiBranchState> multiBranchStateForAssetExtractionId =
        multiBranchStateBlobStorage.getMultiBranchStateForAssetExtractionId(
            assetExtractionId, version);
    Assertions.assertFalse(multiBranchStateForAssetExtractionId.isPresent());

    Branch branchTest =
        Branch.builder()
            .name("test")
            .createdAt(JSR310Migration.newDateTimeEmptyCtor().withNano(0))
            .build();
    MultiBranchState multiBranchState =
        MultiBranchState.builder()
            .branches(ImmutableSet.of(branchTest))
            .branchStateTextUnits(
                ImmutableList.of(
                    BranchStateTextUnit.builder()
                        .tmTextUnitId(123456L)
                        .name("name")
                        .branchNameToBranchDatas(
                            ImmutableMap.of(
                                branchTest.getName(),
                                BranchData.of().withUsages(ImmutableSet.of("somefile"))))
                        .build()))
            .build();

    multiBranchStateBlobStorage.putMultiBranchStateForAssetExtractionId(
        multiBranchState, assetExtractionId, version);

    multiBranchStateForAssetExtractionId =
        multiBranchStateBlobStorage.getMultiBranchStateForAssetExtractionId(
            assetExtractionId, version);

    // Must compare on instant here, else the order may be wrong because of
    // the timezones.
    RecursiveComparisonConfiguration recursiveComparisonConfiguration =
        RecursiveComparisonConfiguration.builder()
            .withComparatorForType(
                Comparator.comparing(ChronoZonedDateTime::toInstant), ZonedDateTime.class)
            .build();

    assertThat(multiBranchStateForAssetExtractionId.get())
        .usingRecursiveComparison(recursiveComparisonConfiguration)
        .isEqualTo(multiBranchState);
  }
}
