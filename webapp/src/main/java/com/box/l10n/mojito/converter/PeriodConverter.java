package com.box.l10n.mojito.converter;

import org.joda.time.Period;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converts a String to a @{link Period}.
 *
 * @author jaurambault
 */
@Component
@ConfigurationPropertiesBinding
public class PeriodConverter implements Converter<String, Period> {

    @Override
    public Period convert(String source) {
        long sourceAsLong = Long.valueOf(source);
        return new Period(sourceAsLong);
    }

}
