package com.box.l10n.mojito.service.review;

public record ReviewProjectLocaleSummaryView(
    Long id, String bcp47Tag, String displayName, int selectedCount, long acceptedCount) {}
