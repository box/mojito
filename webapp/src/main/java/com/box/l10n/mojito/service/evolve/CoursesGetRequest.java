package com.box.l10n.mojito.service.evolve;

import java.time.ZonedDateTime;

public record CoursesGetRequest(String locale, boolean active, ZonedDateTime updatedOnTo) {
  public CoursesGetRequest(String locale, ZonedDateTime updatedOnTo) {
    this(locale, true, updatedOnTo);
  }
}
