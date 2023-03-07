package com.box.l10n.mojito.okapi.filters;

import java.util.ArrayList;
import java.util.List;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.yaml.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author loliveiramontedonio */
public class YamlFilter extends net.sf.okapi.filters.yaml.YamlFilter {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(YamlFilter.class);

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

  @Override
  public void open(RawDocument input) {
    applyFilterOptions(input);
    super.open(input);
  }

  void applyFilterOptions(RawDocument input) {
    FilterOptions filterOptions = input.getAnnotation(FilterOptions.class);
    Parameters parameters = this.getParameters();
    logger.debug("Set default value for the filter");
    parameters.setUseFullKeyPath(true);
    parameters.setExtractAllPairs(true);

    logger.debug("Override with filter options");
    if (filterOptions != null) {
      // okapi options
      filterOptions.getBoolean("useFullKeyPath", b -> parameters.setUseFullKeyPath(b));
      filterOptions.getBoolean("extractAllPairs", b -> parameters.setExtractAllPairs(b));
      filterOptions.getString("exceptions", s -> parameters.setExceptions(s));
    }
  }
}
