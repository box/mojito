package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.immutables.NoPrefixNoBuiltinContainer;
import java.util.ArrayList;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable(singleton = true)
@NoPrefixNoBuiltinContainer
public abstract class AbstractExtractionDiffStatistics {
  @Value.Default
  public int getAdded() {
    return 0;
  }

  @Value.Default
  public int getRemoved() {
    return 0;
  }

  @Value.Default
  public int getBase() {
    return 0;
  }

  @Value.Default
  public int getCurrent() {
    return 0;
  }

  @Value.Default
  public List<String> getAddedStrings() {
    return new ArrayList<>();
  }

  @Value.Default
  public List<String> getRemovedStrings() {
    return new ArrayList<>();
  }
}
