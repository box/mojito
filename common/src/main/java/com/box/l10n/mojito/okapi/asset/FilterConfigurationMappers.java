package com.box.l10n.mojito.okapi.asset;

import com.box.l10n.mojito.okapi.filters.*;
import net.sf.okapi.common.filters.DefaultFilters;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class FilterConfigurationMappers {

  /**
   * Creates a {@link net.sf.okapi.common.filters.FilterConfigurationMapper}, which has been
   * configured with the default mappings
   *
   * <p>Creating the FilterConfigurationMapper turns out to be very slow (XMLFilter? forgot which
   * one can be seen with profiler). So made it a lazy bean to run the code only once instead of
   * creating the instance by calling the function.
   *
   * <p>Follow up is to check if that impacts startup time in some ways and or why that is so slow.
   */
  @Lazy
  @Bean
  public IFilterConfigurationMapper getConfiguredFilterConfigurationMapper() {

    IFilterConfigurationMapper mapper = new net.sf.okapi.common.filters.FilterConfigurationMapper();

    // Adding default filter mappings
    DefaultFilters.setMappings(mapper, false, true);

    // Adding custom filters mappings
    mapper.addConfigurations(CSVFilter.class.getName());
    mapper.addConfigurations(AndroidFilter.class.getName());
    mapper.addConfigurations(POFilter.class.getName());
    mapper.addConfigurations(XMLFilter.class.getName());
    mapper.addConfigurations(MacStringsFilter.class.getName());
    mapper.addConfigurations(MacStringsdictFilter.class.getName());
    mapper.addConfigurations(MacStringsdictFilterKey.class.getName());
    mapper.addConfigurations(JSFilter.class.getName());
    mapper.addConfigurations(JSONFilter.class.getName());
    mapper.addConfigurations(XcodeXliffFilter.class.getName());
    mapper.addConfigurations(YamlFilter.class.getName());

    return mapper;
  }
}
