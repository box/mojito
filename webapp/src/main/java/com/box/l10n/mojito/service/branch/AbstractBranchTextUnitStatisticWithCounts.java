package com.box.l10n.mojito.service.branch;

import com.box.l10n.mojito.immutables.JpaDTO;
import org.immutables.value.Value;

@Value.Immutable
@JpaDTO
public abstract class AbstractBranchTextUnitStatisticWithCounts {
  @Value.Parameter(order = 1)
  abstract long getId();

  @Value.Parameter(order = 2)
  abstract long getBranchStatisticId();

  @Value.Parameter(order = 3)
  abstract long getTmTextUnitId();

  @Value.Parameter(order = 4)
  abstract long getForTranslationCount();

  @Value.Parameter(order = 5)
  abstract long getTotalCount();
}
