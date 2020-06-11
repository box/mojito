package com.box.l10n.mojito.monitoring;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.NamingConvention;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class PrefixHierarchicalNameMapperTest {

    static Logger logger = LoggerFactory.getLogger(PrefixHierarchicalNameMapperTest.class);

    @Test
    public void toHierarchicalName() {
        PrefixHierarchicalNameMapper prefixHierarchicalNameMapper = new PrefixHierarchicalNameMapper("mojito", "_t_", null);
        Meter.Id id = new Meter.Id("metric.name", Tags.of("tag1", "value1").and("tag2", "value2"),
                null, null, Meter.Type.COUNTER);
        String hierarchicalName = prefixHierarchicalNameMapper.toHierarchicalName(id, NamingConvention.camelCase);
        logger.debug(hierarchicalName);
        assertEquals("mojito.metricName._t_tag1.value1._t_tag2.value2", hierarchicalName);
    }

    @Test
    public void toHierarchicalNameOverrideNameConvention() {
        PrefixHierarchicalNameMapper prefixHierarchicalNameMapper = new PrefixHierarchicalNameMapper("mojito", "_t_", NamingConvention.snakeCase);
        Meter.Id id = new Meter.Id("metric.name", Tags.of("tag.with.case.1", "value1").and("tag2", "value2"),
                null, null, Meter.Type.COUNTER);
        String hierarchicalName = prefixHierarchicalNameMapper.toHierarchicalName(id, NamingConvention.camelCase);
        logger.debug(hierarchicalName);
        assertEquals("mojito.metric_name._t_tag_with_case_1.value1._t_tag2.value2", hierarchicalName);
    }

    @Test
    public void toHierarchicalNameOverrideTagCase() {
        PrefixHierarchicalNameMapper prefixHierarchicalNameMapper = new PrefixHierarchicalNameMapper("mojito", "_t_", NamingConvention.snakeCase);
        Meter.Id id = new Meter.Id("metric.name", Tags.of("tag.with.case1", "value1").and("tag2", "value2"),
                null, null, Meter.Type.COUNTER);
        String hierarchicalName = prefixHierarchicalNameMapper.toHierarchicalName(id, NamingConvention.camelCase);
        logger.debug(hierarchicalName);
        assertEquals("mojito.metric_name._t_tag_with_case1.value1._t_tag2.value2", hierarchicalName);
    }

    @Test
    public void toHierarchicalNameNoPrefix() {
        PrefixHierarchicalNameMapper prefixHierarchicalNameMapper = new PrefixHierarchicalNameMapper("", "_t_", null);
        Meter.Id id = new Meter.Id("metric.name", Tags.of("tag1", "value1").and("tag2", "value2"),
                null, null, Meter.Type.COUNTER);
        String hierarchicalName = prefixHierarchicalNameMapper.toHierarchicalName(id, NamingConvention.slashes);
        logger.debug(hierarchicalName);
        assertEquals("metric/name._t_tag1.value1._t_tag2.value2", hierarchicalName);
    }

    @Test
    public void toHierarchicalNamingConventionOnlyOnDottedInput() {
        PrefixHierarchicalNameMapper prefixHierarchicalNameMapper = new PrefixHierarchicalNameMapper("mojito", "_t_", NamingConvention.snakeCase);
        Meter.Id id = new Meter.Id("metricName", Tags.of("tagWithCase1", "value1").and("tag2", "value2"),
                null, null, Meter.Type.COUNTER);
        String hierarchicalName = prefixHierarchicalNameMapper.toHierarchicalName(id, NamingConvention.camelCase);
        logger.debug(hierarchicalName);
        assertEquals("mojito.metricName._t_tag2.value2._t_tagWithCase1.value1", hierarchicalName);
    }
}