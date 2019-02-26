package com.box.l10n.mojito.okapi.filters;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.json.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jeanaurambault
 */
public class JSONFilter extends net.sf.okapi.filters.json.JSONFilter {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(JSONFilter.class);

    public static final String FILTER_CONFIG_ID = "okf_json@mojito";

    @Override
    public String getName() {
        return FILTER_CONFIG_ID;
    }

    @Override
    public List<FilterConfiguration> getConfigurations() {
        List<FilterConfiguration> list = new ArrayList<FilterConfiguration>();

        list.add(new FilterConfiguration(this.getName(),
                "application/json",
                this.getClass().getName(),
                "JSON (JavaScript Object Notation)",
                "Configuration for JSON files"));
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

        logger.debug("Override with filter options");
        if (filterOptions != null) {
            filterOptions.getBoolean("useFullKeyPath", b -> parameters.setUseFullKeyPath(b));
            filterOptions.getBoolean("extractAllPairs", b -> parameters.setExtractAllPairs(b));
            filterOptions.getString("exceptions", s -> parameters.setExceptions(s));
        }

        logger.debug("Parameters: {}", parameters);
    }
}
