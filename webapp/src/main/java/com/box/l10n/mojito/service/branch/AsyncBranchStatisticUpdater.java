package com.box.l10n.mojito.service.branch;

import static com.box.l10n.mojito.utils.TaskExecutorUtils.waitForAllFutures;

import com.box.l10n.mojito.entity.Branch;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AsyncBranchStatisticUpdater {

  // Lazy annotation is used to resolve the circular dependency between AsyncBranchStatisticUpdater
  // and BranchStatisticService,
  // this is needed because by re-using the Spring proxy to call the update Branch stats method, we
  // ensure a transaction is in place.
  // If we combine @Async and @Transactional on the same method then 2 proxies are created by Spring
  // and the transactional proxy
  // is not used when the method is called asynchronously.
  @Autowired @Lazy BranchStatisticService branchStatisticService;

  public void updateBranchStatistics(
      List<Branch> branches,
      Map<String, ImmutableMap<Long, ForTranslationCountForTmTextUnitId>>
          mapBranchNameToTranslationCountForTextUnitId) {
    List<CompletableFuture<Void>> futures =
        branches.stream()
            .map(
                branch ->
                    updateBranchStatisticsAsync(
                        branch, mapBranchNameToTranslationCountForTextUnitId))
            .collect(Collectors.toList());
    waitForAllFutures(futures);
  }

  @Async("statisticsTaskExecutor")
  public CompletableFuture<Void> updateBranchStatisticsAsync(
      Branch branch,
      Map<String, ImmutableMap<Long, ForTranslationCountForTmTextUnitId>>
          mapBranchNameToTranslationCountForTextUnitId) {
    branchStatisticService.updateBranchStatisticInTx(
        branch,
        mapBranchNameToTranslationCountForTextUnitId.getOrDefault(
            branch.getName(), ImmutableMap.of()));
    return CompletableFuture.completedFuture(null);
  }
}
