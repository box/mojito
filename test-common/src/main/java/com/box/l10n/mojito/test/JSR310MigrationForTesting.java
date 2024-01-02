package com.box.l10n.mojito.test;

import java.time.ZonedDateTime;
import org.assertj.core.api.Assertions;

public class JSR310MigrationForTesting {

  public static void junitAssertEquals(ZonedDateTime zonedDateTime, ZonedDateTime zonedDateTime2) {
    // Assert.assertEquals(zonedDateTime.toInstant(), zonedDateTime.toInstant());
    Assertions.assertThat(zonedDateTime).isEqualTo(zonedDateTime2);
  }

  public static void junitAssertEqualsDateTime(String str, String str2) {
    Assertions.assertThat(ZonedDateTime.parse(str)).isEqualTo(ZonedDateTime.parse(str2));
  }
}
