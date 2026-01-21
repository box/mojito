package com.box.l10n.mojito.service.review;

import java.util.List;

public record ReviewProjectLocaleDetailView(
    Long id,
    String bcp47Tag,
    String displayName,
    int selectedCount,
    long acceptedCount,
    List<ReviewProjectTextUnitView> textUnits) {}
