package com.box.l10n.mojito.utils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.stereotype.Component;

/**
 *
 * @author jeanaurambault
 */
@Component
public class DateTimeUtils {

    public DateTime now(DateTimeZone dateTimeZone) {
        return new DateTime(dateTimeZone);
    }
}
