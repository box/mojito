package com.box.l10n.mojito.service.delta;

/**
 * Types that classify if the deltas provide a new translation,
 * an updated translation or if that information is unknown, with a
 * corresponding priority associated.
 *
 * @author garion
 */
public enum DeltaType {
    UNKNOWN(DeltaPriority.LOW),
    NEW_TRANSLATION(DeltaPriority.HIGH),
    UPDATED_TRANSLATION(DeltaPriority.MEDIUM);

    private final DeltaPriority deltaPriority;

    DeltaType(DeltaPriority deltaPriority) {
        this.deltaPriority = deltaPriority;
    }

    public DeltaPriority getValue() {
        return this.deltaPriority;
    }
}
