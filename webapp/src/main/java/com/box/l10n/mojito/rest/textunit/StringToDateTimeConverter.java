package com.box.l10n.mojito.rest.textunit;

import org.joda.time.DateTime;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converts a {@link String} into {@link DateTime}. The string can either be
 * a number of milliseconds from 1970-01-01T00:00:00Z or any format 
 * recognized by {@link DateTime#DateTime(java.lang.Object) }
 * 
 * @author jeanaurambault
 */
@Component
public class StringToDateTimeConverter implements Converter<String, DateTime> {

    @Override
    public DateTime convert(String source) {

        DateTime converted = null;

        if (source != null) {
            Object instant;

            try {
                instant = Long.parseLong(source);
            } catch (NumberFormatException nfe) {
                instant = source;
            }
            converted = new DateTime(instant);
        }

        return converted;
    }
}
