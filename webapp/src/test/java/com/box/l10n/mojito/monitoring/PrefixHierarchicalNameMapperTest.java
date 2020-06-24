package com.box.l10n.mojito.monitoring;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.NamingConvention;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class PrefixHierarchicalNameMapperTest {

    static Logger logger = LoggerFactory.getLogger(PrefixHierarchicalNameMapperTest.class);

    PrefixHierarchicalNameMapper mapper;
    String stripCharacters;

    @Test
    public void toHierarchicalName() {
        mapper = new PrefixHierarchicalNameMapper("mojito", "_t_", null, null);
        Meter.Id id = new Meter.Id("metric.name", Tags.of("tag1", "value1").and("tag2", "value2"),
                null, null, Meter.Type.COUNTER);
        String hierarchicalName = mapper.toHierarchicalName(id, NamingConvention.camelCase);
        logger.debug(hierarchicalName);
        assertEquals("mojito.metricName._t_tag1.value1._t_tag2.value2", hierarchicalName);
    }

    @Test
    public void toHierarchicalNameOverrideNameConvention() {
        mapper = new PrefixHierarchicalNameMapper("mojito", "_t_", null, NamingConvention.snakeCase);
        Meter.Id id = new Meter.Id("metric.name", Tags.of("tag.with.case.1", "value1").and("tag2", "value2"),
                null, null, Meter.Type.COUNTER);
        String hierarchicalName = mapper.toHierarchicalName(id, NamingConvention.camelCase);
        logger.debug(hierarchicalName);
        assertEquals("mojito.metric_name._t_tag_with_case_1.value1._t_tag2.value2", hierarchicalName);
    }

    @Test
    public void toHierarchicalNameOverrideTagCase() {
        mapper = new PrefixHierarchicalNameMapper("mojito", "_t_", null, NamingConvention.snakeCase);
        Meter.Id id = new Meter.Id("metric.name", Tags.of("tag.with.case1", "value1").and("tag2", "value2"),
                null, null, Meter.Type.COUNTER);
        String hierarchicalName = mapper.toHierarchicalName(id, NamingConvention.camelCase);
        logger.debug(hierarchicalName);
        assertEquals("mojito.metric_name._t_tag_with_case1.value1._t_tag2.value2", hierarchicalName);
    }

    @Test
    public void toHierarchicalNameNoPrefix() {
        mapper = new PrefixHierarchicalNameMapper("", "_t_", null, null);
        Meter.Id id = new Meter.Id("metric.name", Tags.of("tag1", "value1").and("tag2", "value2"),
                null, null, Meter.Type.COUNTER);
        String hierarchicalName = mapper.toHierarchicalName(id, NamingConvention.slashes);
        logger.debug(hierarchicalName);
        assertEquals("metric/name._t_tag1.value1._t_tag2.value2", hierarchicalName);
    }

    @Test
    public void toHierarchicalNamingConventionOnlyOnDottedInput() {
        mapper = new PrefixHierarchicalNameMapper("mojito", "_t_", null, NamingConvention.snakeCase);
        Meter.Id id = new Meter.Id("metricName", Tags.of("tagWithCase1", "value1").and("tag2", "value2"),
                null, null, Meter.Type.COUNTER);
        String hierarchicalName = mapper.toHierarchicalName(id, NamingConvention.camelCase);
        logger.debug(hierarchicalName);
        assertEquals("mojito.metricName._t_tag2.value2._t_tagWithCase1.value1", hierarchicalName);
    }

    @Test
    public void toHierarchicalNamingConventionStrippingCharacters() {
        stripCharacters = "{}$!@#";
        mapper = new PrefixHierarchicalNameMapper("mojito", "_t_", stripCharacters, NamingConvention.snakeCase);
        Tags tags = Tags.of("tag#1", "value/{1}/.")
                        .and("tag#2", "value$2/{test}/value")
                        .and("tag#3!", "value!3/@test#")
                        .and("tag#4", "");
        Meter.Id id = new Meter.Id("metricName", tags, null, null, Meter.Type.COUNTER);
        String hierarchicalName = mapper.toHierarchicalName(id, NamingConvention.camelCase);
        logger.debug(hierarchicalName);

        assertEquals("mojito.metricName._t_tag1.value/1/.._t_tag2.value2/test/value._t_tag3.value3/test._t_tag4.", hierarchicalName);
    }

    @Test
    public void toHierarchicalNamingConventionStrippingCharactersRealTestCase() {
        stripCharacters = "{}";
        mapper = new PrefixHierarchicalNameMapper("mojito", "_t_", stripCharacters, NamingConvention.snakeCase);
        Tags tags1 = Tags.of("status", "200").and("uri", "/api/pollableTasks/{pollableTaskId}");
        Tags tags2 = Tags.of("status", "404").and("uri", "/api/assets/{assetId}/localized/{localeId}");

        Meter.Id id1 = new Meter.Id("http.server.requests", tags1, null, null, Meter.Type.COUNTER);
        String name1 = mapper.toHierarchicalName(id1, NamingConvention.snakeCase);

        Meter.Id id2 = new Meter.Id("http.server.requests", tags2, null, null, Meter.Type.COUNTER);
        String name2 = mapper.toHierarchicalName(id2, NamingConvention.snakeCase);

        assertEquals("mojito.http_server_requests._t_status.200._t_uri./api/pollableTasks/pollableTaskId", name1);
        assertEquals("mojito.http_server_requests._t_status.404._t_uri./api/assets/assetId/localized/localeId", name2);
    }

    @Test
    public void stripCharacters(){

        PrefixHierarchicalNameMapper mapper1 = new PrefixHierarchicalNameMapper("prefix", "", null, NamingConvention.snakeCase);
        PrefixHierarchicalNameMapper mapper2 = new PrefixHierarchicalNameMapper("prefix", "", "", NamingConvention.snakeCase);
        PrefixHierarchicalNameMapper mapper3 = new PrefixHierarchicalNameMapper("prefix", "", "{}", NamingConvention.snakeCase);
        PrefixHierarchicalNameMapper mapper4 = new PrefixHierarchicalNameMapper("prefix", "", "$%", NamingConvention.snakeCase);
        PrefixHierarchicalNameMapper mapper5 = new PrefixHierarchicalNameMapper("prefix", "", "pre", NamingConvention.snakeCase);

        assertEquals(mapper1.stripCharacters(""), "");
        assertEquals(mapper1.stripCharacters("test"), "test");
        assertEquals(mapper1.stripCharacters("@#$"), "@#$");
        assertEquals(mapper2.stripCharacters(""), "");
        assertEquals(mapper2.stripCharacters("test"), "test");
        assertEquals(mapper2.stripCharacters("@#$"), "@#$");
        assertEquals(mapper3.stripCharacters(""), "");
        assertEquals(mapper3.stripCharacters("test"), "test");
        assertEquals(mapper3.stripCharacters("@#$"), "@#$");
        assertEquals(mapper3.stripCharacters("@#${"), "@#$");
        assertEquals(mapper3.stripCharacters("@#$}"), "@#$");
        assertEquals(mapper3.stripCharacters("@#${}"), "@#$");
        assertEquals(mapper4.stripCharacters("test"), "test");
        assertEquals(mapper4.stripCharacters("%@#$"), "@#");
        assertEquals(mapper4.stripCharacters("@%#${"), "@#{");
        assertEquals(mapper4.stripCharacters("@#%$}"), "@#}");
        assertEquals(mapper4.stripCharacters("@#$%{}"), "@#{}");
        assertEquals(mapper5.stripCharacters("test"), "tst");
        assertEquals(mapper5.stripCharacters("pre%@#$"), "%@#$");
        assertEquals(mapper5.stripCharacters("prefix"), "fix");
        assertEquals(mapper5.stripCharacters("prefixrep"), "fix");

    }
}
