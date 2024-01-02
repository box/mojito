package com.box.l10n.mojito.utils;

import com.box.l10n.mojito.JSR310Migration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/** @author jeanaurambault */
@Component
public class DateTimeUtils {

  public ZonedDateTime now() {
    return JSR310Migration.newDateTimeEmptyCtor();
  }

  public ZonedDateTime now(ZoneId dateTimeZone) {
    return JSR310Migration.newDateTimeCtorWithDateTimeZone(dateTimeZone);
  }
}
