package com.box.l10n.mojito.service.security.user;

import java.time.ZonedDateTime;

public record UserAdminSummaryProjection(
    Long id,
    String username,
    String givenName,
    String surname,
    String commonName,
    Boolean enabled,
    boolean canTranslateAllLocales,
    ZonedDateTime createdDate) {}
