package com.box.l10n.mojito.converter;

import org.joda.time.LocalTime;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converts a String to a {@link LocalTime}.
 * 
 * @author jaurambault
 */
@Component
@ConfigurationPropertiesBinding
public class LocalTimeConverter implements Converter<String, LocalTime> {

    @Override
    public LocalTime convert(String source) {
        return new LocalTime(source);
    }
}
