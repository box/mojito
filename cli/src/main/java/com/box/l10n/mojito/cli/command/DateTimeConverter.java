package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.IStringConverter;
import java.time.ZonedDateTime;

public class DateTimeConverter implements IStringConverter<ZonedDateTime> {
  @Override
  public ZonedDateTime convert(String dateAsText) {
    return ZonedDateTime.parse(dateAsText);
  }
}
