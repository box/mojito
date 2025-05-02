package com.box.l10n.mojito.service.appender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.asset.LocalizedAssetBody;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.branch.BranchStatisticService;
import com.box.l10n.mojito.service.pushrun.PushRunRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class AssetAppenderServiceTest {
  AssetAppenderService assetAppenderService;

  Asset asset = new Asset();
  LocalizedAssetBody localizedAssetBody = new LocalizedAssetBody();

  AssetAppenderFactory assetAppenderFactory;
  BranchRepository branchRepositoryMock;
  BranchStatisticService branchStatisticServiceMock;
  PushRunRepository pushRunRepositoryMock;
  AppendedAssetBlobStorage appendedAssetBlobStorageMock;
  MeterRegistry meterRegistryMock;
  AssetAppenderConfig assetAppenderConfigMock;

  String extension = "pot";
  String content = "Test source";

  Counter appendCountCounterMock;
  Counter appendBranchCountCounterMock;
  Counter exceedCountCounterMock;
  Counter exceedBranchCountCounterMock;

  @BeforeEach
  public void setup() {
    assetAppenderFactory = mock(AssetAppenderFactory.class);
    branchRepositoryMock = mock(BranchRepository.class);
    branchStatisticServiceMock = mock(BranchStatisticService.class);
    pushRunRepositoryMock = mock(PushRunRepository.class);
    appendedAssetBlobStorageMock = mock(AppendedAssetBlobStorage.class);
    meterRegistryMock = mock(MeterRegistry.class);
    assetAppenderConfigMock = mock(AssetAppenderConfig.class);
    assetAppenderService =
        new AssetAppenderService(
            assetAppenderFactory,
            branchRepositoryMock,
            branchStatisticServiceMock,
            pushRunRepositoryMock,
            appendedAssetBlobStorageMock,
            meterRegistryMock,
            assetAppenderConfigMock);

    appendCountCounterMock = Mockito.mock(Counter.class);
    when(meterRegistryMock.counter(
            eq("AssetAppenderService.appendBranchTextUnitsToSource.appendedTextUnitCount"),
            isA(Iterable.class)))
        .thenReturn(appendCountCounterMock);

    appendBranchCountCounterMock = Mockito.mock(Counter.class);
    when(meterRegistryMock.counter(
            eq("AssetAppenderService.appendBranchTextUnitsToSource.appendedBranchCount"),
            isA(Iterable.class)))
        .thenReturn(appendBranchCountCounterMock);

    exceedCountCounterMock = Mockito.mock(Counter.class);
    when(meterRegistryMock.counter(
            eq(
                "AssetAppenderService.appendBranchTextUnitsToSource.exceededAppendLimitTextUnitCount"),
            isA(Iterable.class)))
        .thenReturn(exceedCountCounterMock);

    exceedBranchCountCounterMock = Mockito.mock(Counter.class);
    when(meterRegistryMock.counter(
            eq("AssetAppenderService.appendBranchTextUnitsToSource.exceededAppendLimitBranchCount"),
            isA(Iterable.class)))
        .thenReturn(exceedBranchCountCounterMock);

    localizedAssetBody.setAppendBranchTextUnitsId("ba30d2a7-ec14-4d18-9372-5b8072094bbe");
    asset.setPath("strings.pot");

    Repository localRepository = new Repository();
    localRepository.setId(1L);
    localRepository.setName("localRepository");
    asset.setRepository(localRepository);

    assetAppenderService.DEFAULT_APPEND_LIMIT = 1000;
  }

  @Test
  public void testNoAppendWhenNoAppender() {
    when(assetAppenderFactory.fromExtension(any(), any())).thenReturn(Optional.empty());

    assetAppenderService.appendBranchTextUnitsToSource(
        asset, localizedAssetBody.getAppendBranchTextUnitsId(), content);

    verify(branchRepositoryMock, times(0)).findBranchesForAppending(any());
  }

  @Test
  public void testAppends() {
    POTAssetAppender potAssetAppenderMock = mock(POTAssetAppender.class);
    when(assetAppenderFactory.fromExtension(extension, content))
        .thenReturn(Optional.of(potAssetAppenderMock));

    List<Branch> branches = new ArrayList<>();

    IntStream.range(1, 4)
        .forEach(
            i -> {
              Branch branch = new Branch();
              branch.setName("branch" + i);
              branches.add(branch);
            });

    AtomicInteger i = new AtomicInteger(1);
    branches.forEach(
        branch -> {
          // Return 2 unique text units for each branch
          List<TextUnitDTO> textUnits =
              IntStream.range(i.get(), i.get() + 2)
                  .mapToObj(
                      x -> {
                        TextUnitDTO textUnitDTO = new TextUnitDTO();
                        textUnitDTO.setSource(content + x);
                        textUnitDTO.setTmTextUnitId(Integer.toUnsignedLong(x));
                        return textUnitDTO;
                      })
                  .toList();

          when(branchStatisticServiceMock.getTextUnitDTOsForBranch(branch)).thenReturn(textUnits);
          i.addAndGet(2);
        });

    when(branchRepositoryMock.findBranchesForAppending(any())).thenReturn(branches);

    assetAppenderService.appendBranchTextUnitsToSource(
        asset, localizedAssetBody.getAppendBranchTextUnitsId(), content);

    verify(potAssetAppenderMock, times(3)).appendTextUnits(any());
    verify(meterRegistryMock, times(2)).counter(Mockito.anyString(), isA(Iterable.class));
    verify(appendCountCounterMock, times(1)).increment(6);
    verify(appendBranchCountCounterMock, times(1)).increment(3);
  }

  @Test
  public void testStopsAppendingOverLimit() throws NoSuchFieldException, IllegalAccessException {
    POTAssetAppender potAssetAppenderMock = mock(POTAssetAppender.class);
    when(assetAppenderFactory.fromExtension(extension, content))
        .thenReturn(Optional.of(potAssetAppenderMock));

    List<Branch> branches = new ArrayList<>();

    // Create 12 branches
    IntStream.range(1, 13)
        .forEach(
            i -> {
              Branch branch = new Branch();
              branch.setName("branch" + i);
              branches.add(branch);
            });

    AtomicInteger i = new AtomicInteger(1);
    branches.forEach(
        branch -> {
          // Return unique text units for each branch
          List<TextUnitDTO> textUnits =
              IntStream.range(i.get(), i.get() + 2)
                  .mapToObj(
                      x -> {
                        TextUnitDTO textUnitDTO = new TextUnitDTO();
                        textUnitDTO.setSource(content + x);
                        textUnitDTO.setTmTextUnitId(Integer.toUnsignedLong(x));
                        return textUnitDTO;
                      })
                  .toList();

          when(branchStatisticServiceMock.getTextUnitDTOsForBranch(branch)).thenReturn(textUnits);
          i.addAndGet(2);
        });

    assetAppenderService.DEFAULT_APPEND_LIMIT = 10;

    when(branchRepositoryMock.findBranchesForAppending(any())).thenReturn(branches);

    assetAppenderService.appendBranchTextUnitsToSource(
        asset, localizedAssetBody.getAppendBranchTextUnitsId(), content);

    verify(potAssetAppenderMock, times(5)).appendTextUnits(any());

    // Verify metrics were emitted
    verify(meterRegistryMock, times(4)).counter(Mockito.anyString(), isA(Iterable.class));
    verify(appendCountCounterMock, times(1)).increment(10);
    verify(exceedCountCounterMock, times(1)).increment(14);
    verify(exceedBranchCountCounterMock, times(1)).increment(7);
  }

  @Test
  public void testDontAppendDuplicateTextUnits() {
    POTAssetAppender potAssetAppenderMock = mock(POTAssetAppender.class);
    when(assetAppenderFactory.fromExtension(extension, content))
        .thenReturn(Optional.of(potAssetAppenderMock));

    List<Branch> branches = new ArrayList<>();

    IntStream.range(1, 4)
        .forEach(
            i -> {
              Branch branch = new Branch();
              branch.setName("branch" + i);
              branches.add(branch);
            });

    List<TextUnitDTO> textUnits =
        IntStream.range(1, 3)
            .mapToObj(
                i -> {
                  TextUnitDTO textUnitDTO = new TextUnitDTO();
                  textUnitDTO.setSource(content + i);
                  textUnitDTO.setTmTextUnitId(Integer.toUnsignedLong(i));
                  return textUnitDTO;
                })
            .toList();

    // For every branch, return the same 2 text units for appending
    when(branchStatisticServiceMock.getTextUnitDTOsForBranch(any())).thenReturn(textUnits);
    when(branchRepositoryMock.findBranchesForAppending(any())).thenReturn(branches);

    assetAppenderService.appendBranchTextUnitsToSource(
        asset, localizedAssetBody.getAppendBranchTextUnitsId(), content);

    verify(potAssetAppenderMock, times(3)).appendTextUnits(any());
    verify(meterRegistryMock, times(2)).counter(Mockito.anyString(), isA(Iterable.class));
    // Make sure the appended count is only 2 as the text units were duplicates
    verify(appendCountCounterMock, times(1)).increment(2);
    verify(appendBranchCountCounterMock, times(1)).increment(3);
  }
}
