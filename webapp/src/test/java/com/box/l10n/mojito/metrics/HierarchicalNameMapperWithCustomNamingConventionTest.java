package com.box.l10n.mojito.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;

import static org.assertj.core.api.Assertions.assertThat;

class HierarchicalNameMapperWithCustomNamingConventionTest {

    HierarchicalNameMapper       mapper = HierarchicalNameMapper.DEFAULT;
    StatsdNamingConventionConfig config;
    NamingConvention             namingConvention;
    Meter.Id taggedMeter, untaggedMeter;
    Tags tags, noTags;

    @BeforeEach
    void setUp() {
        config = new StatsdNamingConventionConfig();

        tags = Tags.of("tagA", "valA").and("tagB", "valB");
        noTags = Tags.empty();
    }

    @Test
    void withNamePrefixAndKeyPrefixAndIdentityDoesntChangesNames() {

        // Arrange
        config.setNamingConvention(NamingConvention.identity);
        config.setNamePrefix("prefix");
        config.setTagKeyPrefix("_t_");

        taggedMeter = new Meter.Id("test.meter.with.tags", tags, null, null, Meter.Type.COUNTER);
        untaggedMeter = new Meter.Id("test.meter.without.tags", noTags, null, null, Meter.Type.COUNTER);

        // Act
        namingConvention = NamingConventionBuilder.build(config);
        String taggedResult = mapper.toHierarchicalName(taggedMeter, namingConvention);
        String untaggedResult = mapper.toHierarchicalName(untaggedMeter, namingConvention);

        // Assert
        assertThat(taggedResult).isEqualTo("prefix.test.meter.with.tags._t_tagA.valA._t_tagB.valB");
        assertThat(untaggedResult).isEqualTo("prefix.test.meter.without.tags");

    }

    @Test
    void withNamePrefixAndKeyPrefixAndSnakeCaseGeneratesSnakedCasedNames() {

        // Arrange
        config.setNamingConvention(NamingConvention.snakeCase);
        config.setNamePrefix("prefix");
        config.setTagKeyPrefix("_t_");

        taggedMeter = new Meter.Id("test.meter.with.tags", tags, null, null, Meter.Type.COUNTER);
        untaggedMeter = new Meter.Id("test.meter.without.tags", noTags, null, null, Meter.Type.COUNTER);

        // Act
        namingConvention = NamingConventionBuilder.build(config);
        String taggedResult = mapper.toHierarchicalName(taggedMeter, namingConvention);
        String untaggedResult = mapper.toHierarchicalName(untaggedMeter, namingConvention);

        // Assert
        assertThat(taggedResult).isEqualTo("prefix.test_meter_with_tags._t_tagA.valA._t_tagB.valB");
        assertThat(untaggedResult).isEqualTo("prefix.test_meter_without_tags");

    }

    @Test
    void withNamePrefixAndKeyPrefixAndCamelCaseGeneratesCamelCasedNames() {

        // Arrange
        config.setNamingConvention(NamingConvention.camelCase);
        config.setNamePrefix("prefix");
        config.setTagKeyPrefix("_t_");

        taggedMeter = new Meter.Id("test.meter.with.tags", tags, null, null, Meter.Type.COUNTER);
        untaggedMeter = new Meter.Id("test.meter.without.tags", noTags, null, null, Meter.Type.COUNTER);

        // Act
        namingConvention = NamingConventionBuilder.build(config);
        String taggedResult = mapper.toHierarchicalName(taggedMeter, namingConvention);
        String untaggedResult = mapper.toHierarchicalName(untaggedMeter, namingConvention);

        // Assert
        assertThat(taggedResult).isEqualTo("prefix.testMeterWithTags._t_tagA.valA._t_tagB.valB");
        assertThat(untaggedResult).isEqualTo("prefix.testMeterWithoutTags");

    }

    @Test
    void withNamePrefixAndKeyPrefixAndUpperCamelCaseGeneratesUpperCamelCasedNames() {

        // Arrange
        config.setNamingConvention(NamingConvention.upperCamelCase);
        config.setNamePrefix("prefix");
        config.setTagKeyPrefix("_t_");

        taggedMeter = new Meter.Id("test.meter.with.tags", tags, null, null, Meter.Type.COUNTER);
        untaggedMeter = new Meter.Id("test.meter.without.tags", noTags, null, null, Meter.Type.COUNTER);

        // Act
        namingConvention = NamingConventionBuilder.build(config);
        String taggedResult = mapper.toHierarchicalName(taggedMeter, namingConvention);
        String untaggedResult = mapper.toHierarchicalName(untaggedMeter, namingConvention);

        // Assert
        assertThat(taggedResult).isEqualTo("prefix.TestMeterWithTags._t_TagA.valA._t_TagB.valB");
        assertThat(untaggedResult).isEqualTo("prefix.TestMeterWithoutTags");

    }

    @Test
    void withNamePrefixAndKeyPrefixAndSlashesGeneratesSlashedNames() {

        // Arrange
        config.setNamingConvention(NamingConvention.slashes);
        config.setNamePrefix("prefix");
        config.setTagKeyPrefix("_t_");

        taggedMeter = new Meter.Id("test.meter.with.tags", tags, null, null, Meter.Type.COUNTER);
        untaggedMeter = new Meter.Id("test.meter.without.tags", noTags, null, null, Meter.Type.COUNTER);

        // Act
        namingConvention = NamingConventionBuilder.build(config);
        String taggedResult = mapper.toHierarchicalName(taggedMeter, namingConvention);
        String untaggedResult = mapper.toHierarchicalName(untaggedMeter, namingConvention);

        // Assert
        assertThat(taggedResult).isEqualTo("prefix.test/meter/with/tags._t_tagA.valA._t_tagB.valB");
        assertThat(untaggedResult).isEqualTo("prefix.test/meter/without/tags");

    }

    @Test
    void withoutNamePrefixWithKeyPrefixAndIdentityDoesntChangesNames() {

        // Arrange
        config.setNamingConvention(NamingConvention.identity);
        config.setTagKeyPrefix("_t_");

        taggedMeter = new Meter.Id("test.meter.with.tags", tags, null, null, Meter.Type.COUNTER);
        untaggedMeter = new Meter.Id("test.meter.without.tags", noTags, null, null, Meter.Type.COUNTER);

        // Act
        namingConvention = NamingConventionBuilder.build(config);
        String taggedResult = mapper.toHierarchicalName(taggedMeter, namingConvention);
        String untaggedResult = mapper.toHierarchicalName(untaggedMeter, namingConvention);

        // Assert
        assertThat(taggedResult).isEqualTo("test.meter.with.tags._t_tagA.valA._t_tagB.valB");
        assertThat(untaggedResult).isEqualTo("test.meter.without.tags");

    }

    @Test
    void withoutNamePrefixWithKeyPrefixAndSnakeCaseGeneratesSnakedCasedNames() {

        // Arrange
        config.setNamingConvention(NamingConvention.snakeCase);
        config.setTagKeyPrefix("_t_");

        taggedMeter = new Meter.Id("test.meter.with.tags", tags, null, null, Meter.Type.COUNTER);
        untaggedMeter = new Meter.Id("test.meter.without.tags", noTags, null, null, Meter.Type.COUNTER);

        // Act
        namingConvention = NamingConventionBuilder.build(config);
        String taggedResult = mapper.toHierarchicalName(taggedMeter, namingConvention);
        String untaggedResult = mapper.toHierarchicalName(untaggedMeter, namingConvention);

        // Assert
        assertThat(taggedResult).isEqualTo("test_meter_with_tags._t_tagA.valA._t_tagB.valB");
        assertThat(untaggedResult).isEqualTo("test_meter_without_tags");

    }

    @Test
    void withoutNamePrefixWithKeyPrefixAndCamelCaseGeneratesCamelCasedNames() {

        // Arrange
        config.setNamingConvention(NamingConvention.camelCase);
        config.setTagKeyPrefix("_t_");

        taggedMeter = new Meter.Id("test.meter.with.tags", tags, null, null, Meter.Type.COUNTER);
        untaggedMeter = new Meter.Id("test.meter.without.tags", noTags, null, null, Meter.Type.COUNTER);

        // Act
        namingConvention = NamingConventionBuilder.build(config);
        String taggedResult = mapper.toHierarchicalName(taggedMeter, namingConvention);
        String untaggedResult = mapper.toHierarchicalName(untaggedMeter, namingConvention);

        // Assert
        assertThat(taggedResult).isEqualTo("testMeterWithTags._t_tagA.valA._t_tagB.valB");
        assertThat(untaggedResult).isEqualTo("testMeterWithoutTags");

    }

    @Test
    void withoutNamePrefixWithKeyPrefixAndUpperCamelCaseGeneratesUpperCamelCasedNames() {

        // Arrange
        config.setNamingConvention(NamingConvention.upperCamelCase);
        config.setTagKeyPrefix("_t_");

        taggedMeter = new Meter.Id("test.meter.with.tags", tags, null, null, Meter.Type.COUNTER);
        untaggedMeter = new Meter.Id("test.meter.without.tags", noTags, null, null, Meter.Type.COUNTER);

        // Act
        namingConvention = NamingConventionBuilder.build(config);
        String taggedResult = mapper.toHierarchicalName(taggedMeter, namingConvention);
        String untaggedResult = mapper.toHierarchicalName(untaggedMeter, namingConvention);

        // Assert
        assertThat(taggedResult).isEqualTo("TestMeterWithTags._t_TagA.valA._t_TagB.valB");
        assertThat(untaggedResult).isEqualTo("TestMeterWithoutTags");

    }

    @Test
    void withoutNamePrefixWithoutKeyPrefixAndIdentityDoesntChangesNames() {

        // Arrange
        config.setNamingConvention(NamingConvention.identity);

        taggedMeter = new Meter.Id("test.meter.with.tags", tags, null, null, Meter.Type.COUNTER);
        untaggedMeter = new Meter.Id("test.meter.without.tags", noTags, null, null, Meter.Type.COUNTER);

        // Act
        namingConvention = NamingConventionBuilder.build(config);
        String taggedResult = mapper.toHierarchicalName(taggedMeter, namingConvention);
        String untaggedResult = mapper.toHierarchicalName(untaggedMeter, namingConvention);

        // Assert
        assertThat(taggedResult).isEqualTo("test.meter.with.tags.tagA.valA.tagB.valB");
        assertThat(untaggedResult).isEqualTo("test.meter.without.tags");

    }

    @Test
    void withoutNamePrefixWithoutKeyPrefixAndSnakeCaseGeneratesSnakedCasedNames() {

        // Arrange
        config.setNamingConvention(NamingConvention.snakeCase);

        taggedMeter = new Meter.Id("test.meter.with.tags", tags, null, null, Meter.Type.COUNTER);
        untaggedMeter = new Meter.Id("test.meter.without.tags", noTags, null, null, Meter.Type.COUNTER);

        // Act
        namingConvention = NamingConventionBuilder.build(config);
        String taggedResult = mapper.toHierarchicalName(taggedMeter, namingConvention);
        String untaggedResult = mapper.toHierarchicalName(untaggedMeter, namingConvention);

        // Assert
        assertThat(taggedResult).isEqualTo("test_meter_with_tags.tagA.valA.tagB.valB");
        assertThat(untaggedResult).isEqualTo("test_meter_without_tags");

    }

    @Test
    void withoutNamePrefixWithoutKeyPrefixAndCamelCaseGeneratesCamelCasedNames() {

        // Arrange
        config.setNamingConvention(NamingConvention.camelCase);

        taggedMeter = new Meter.Id("test.meter.with.tags", tags, null, null, Meter.Type.COUNTER);
        untaggedMeter = new Meter.Id("test.meter.without.tags", noTags, null, null, Meter.Type.COUNTER);

        // Act
        namingConvention = NamingConventionBuilder.build(config);
        String taggedResult = mapper.toHierarchicalName(taggedMeter, namingConvention);
        String untaggedResult = mapper.toHierarchicalName(untaggedMeter, namingConvention);

        // Assert
        assertThat(taggedResult).isEqualTo("testMeterWithTags.tagA.valA.tagB.valB");
        assertThat(untaggedResult).isEqualTo("testMeterWithoutTags");

    }

    @Test
    void withoutNamePrefixWithoutKeyPrefixAndUpperCamelCaseGeneratesUpperCamelCasedNames() {

        // Arrange
        config.setNamingConvention(NamingConvention.upperCamelCase);

        taggedMeter = new Meter.Id("test.meter.with.tags", tags, null, null, Meter.Type.COUNTER);
        untaggedMeter = new Meter.Id("test.meter.without.tags", noTags, null, null, Meter.Type.COUNTER);

        // Act
        namingConvention = NamingConventionBuilder.build(config);
        String taggedResult = mapper.toHierarchicalName(taggedMeter, namingConvention);
        String untaggedResult = mapper.toHierarchicalName(untaggedMeter, namingConvention);

        // Assert
        assertThat(taggedResult).isEqualTo("TestMeterWithTags.TagA.valA.TagB.valB");
        assertThat(untaggedResult).isEqualTo("TestMeterWithoutTags");

    }

    @Test
    void withoutNamePrefixWithoutKeyPrefixAndSlashesGeneratesSlashedNames() {

        // Arrange
        config.setNamingConvention(NamingConvention.slashes);

        taggedMeter = new Meter.Id("test.meter.with.tags", tags, null, null, Meter.Type.COUNTER);
        untaggedMeter = new Meter.Id("test.meter.without.tags", noTags, null, null, Meter.Type.COUNTER);

        // Act
        namingConvention = NamingConventionBuilder.build(config);
        String taggedResult = mapper.toHierarchicalName(taggedMeter, namingConvention);
        String untaggedResult = mapper.toHierarchicalName(untaggedMeter, namingConvention);

        // Assert
        assertThat(taggedResult).isEqualTo("test/meter/with/tags.tagA.valA.tagB.valB");
        assertThat(untaggedResult).isEqualTo("test/meter/without/tags");

    }
}
