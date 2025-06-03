package com.box.l10n.mojito.sarif.builder;

import com.box.l10n.mojito.sarif.model.Driver;
import com.box.l10n.mojito.sarif.model.Location;
import com.box.l10n.mojito.sarif.model.Result;
import com.box.l10n.mojito.sarif.model.ResultLevel;
import com.box.l10n.mojito.sarif.model.Run;
import com.box.l10n.mojito.sarif.model.Sarif;
import com.box.l10n.mojito.sarif.model.Tool;
import java.util.List;
import java.util.Map;

public class SarifBuilder {
  private final Sarif sarif;
  private Run currentRun;

  public SarifBuilder() {
    sarif = new Sarif();
  }

  public SarifBuilder addRun(String name, String infoUri) {
    currentRun = new Run(new Tool(new Driver(name, infoUri)));
    sarif.runs.add(currentRun);
    return this;
  }

  public SarifBuilder addResultWithoutLocation(String ruleId, ResultLevel level, String message) {
    Result result = new Result(ruleId, message, level, Map.of());
    currentRun.addResult(result);
    return this;
  }

  public SarifBuilder addResultWithLocations(
      String ruleId, ResultLevel level, String message, List<Location> locations) {
    Result result = new Result(ruleId, message, level);
    result.setLocations(locations);
    currentRun.addResult(result);
    return this;
  }

  public Sarif build() {
    return sarif;
  }
}
