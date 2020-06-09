package com.box.l10n.mojito.metrics;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.config.NamingConvention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class NamingConventionConverterTest {

    @Test
    void testConvert() {

        NamingConventionConverter converter = new NamingConventionConverter();

        assertThat(converter.convert(null)).isEqualTo(NamingConvention.identity);
        assertThat(converter.convert("")).isEqualTo(NamingConvention.identity);
        assertThat(converter.convert("something")).isEqualTo(NamingConvention.identity);

        assertThat(converter.convert("SNAKE_CASE")).isEqualTo(NamingConvention.snakeCase);
        assertThat(converter.convert("snake_case")).isEqualTo(NamingConvention.snakeCase);
        assertThat(converter.convert("SNAKECASE")).isEqualTo(NamingConvention.snakeCase);
        assertThat(converter.convert("snakecase")).isEqualTo(NamingConvention.snakeCase);

        assertThat(converter.convert("CAMEL_CASE")).isEqualTo(NamingConvention.camelCase);
        assertThat(converter.convert("camel_case")).isEqualTo(NamingConvention.camelCase);
        assertThat(converter.convert("CAMELCASE")).isEqualTo(NamingConvention.camelCase);
        assertThat(converter.convert("camelCase")).isEqualTo(NamingConvention.camelCase);

        assertThat(converter.convert("UPPER_CAMEL_CASE")).isEqualTo(NamingConvention.upperCamelCase);
        assertThat(converter.convert("upper_camel_case")).isEqualTo(NamingConvention.upperCamelCase);
        assertThat(converter.convert("UPPERCAMELCASE")).isEqualTo(NamingConvention.upperCamelCase);
        assertThat(converter.convert("uppercamelcase")).isEqualTo(NamingConvention.upperCamelCase);

        assertThat(converter.convert("SLASHES")).isEqualTo(NamingConvention.slashes);
        assertThat(converter.convert("slashes")).isEqualTo(NamingConvention.slashes);

    }
}
