package com.box.l10n.mojito.monitoring;

import io.micrometer.core.instrument.config.NamingConvention;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesBinding
public class NamingConventionConverter implements Converter<String, NamingConvention> {

    @Override
    public NamingConvention convert(String s) {
        NamingConvention namingConvention = null;

        if (s != null) {
            try {
                namingConvention = (NamingConvention) NamingConvention.class.getField(s).get(null);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Name convention not valid: " + s);
            }
        }

        return namingConvention;
    }
}
