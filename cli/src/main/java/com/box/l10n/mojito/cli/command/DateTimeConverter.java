package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.IStringConverter;
import org.joda.time.DateTime;

public class DateTimeConverter implements IStringConverter<DateTime> {
  @Override
  public DateTime convert(String dateAsText) {
    return DateTime.parse(dateAsText);
  }
}
