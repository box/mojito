package com.box.l10n.mojito.service.delta;

/**
 * Enum describing an order for different delta types.
 *
 * @author garion
 */
public enum DeltaPriority {
    HIGH(1),
    MEDIUM(2),
    LOW(3);

    private final int value;

    DeltaPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
