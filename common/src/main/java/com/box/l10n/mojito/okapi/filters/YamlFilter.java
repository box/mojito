package com.box.l10n.mojito.okapi.filters;

import java.util.ArrayList;
import java.util.List;
import net.sf.okapi.common.filters.FilterConfiguration;

/** @author loliveiramontedonio */
public class YamlFilter extends net.sf.okapi.filters.yaml.YamlFilter {

  public static final String FILTER_CONFIG_ID = "okf_yaml@mojito";

  @Override
  public String getName() {
    return FILTER_CONFIG_ID;
  }

  @Override
  public List<FilterConfiguration> getConfigurations() {
    List<FilterConfiguration> list = new ArrayList();
    list.add(
        new FilterConfiguration(
            getName(),
            getMimeType(),
            getClass().getName(),
            "YAML (Yet Another Markup Language)",
            "Configuration for YAML files."));
    return list;
  }
}
