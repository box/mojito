package com.box.l10n.mojito.converter;

import com.box.l10n.mojito.JSR310Migration;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.threeten.extra.PeriodDuration;

/**
 * Converts a String to a @{link Period}.
 *
 * @author jaurambault
 */
@Component
@ConfigurationPropertiesBinding
public class PeriodDurationConverter implements Converter<String, PeriodDuration> {

  @Override
  public PeriodDuration convert(String source) {
    long sourceAsLong = Long.valueOf(source);
    return JSR310Migration.newPeriodCtorWithLong(sourceAsLong);
  }
}
