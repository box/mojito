package com.box.l10n.mojito.metrics;

import com.google.common.base.Strings;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.config.NamingConvention;

@Component
@ConfigurationPropertiesBinding
public class NamingConventionConverter implements Converter<String, NamingConvention> {

    @Override
    public NamingConvention convert(String from) {
        NamingConvention result;

        switch (Strings.nullToEmpty(from).toLowerCase()) {
            case "snakecase":
            case "snake_case":
                result = NamingConvention.snakeCase;
                break;
            case "camelcase":
            case "camel_case":
                result = NamingConvention.camelCase;
                break;
            case "uppercamelcase":
            case "upper_camel_case":
                result = NamingConvention.upperCamelCase;
                break;
            case "slashes":
                result = NamingConvention.slashes;
                break;
            default:
                result = NamingConvention.identity;
                break;
        }

        return result;
    }
}
