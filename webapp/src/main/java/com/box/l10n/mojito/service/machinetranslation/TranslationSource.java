package com.box.l10n.mojito.service.machinetranslation;

/**
 * Translation source descriptions with associated priorities.
 * The lower the number, the higher the priority.
 */
public enum TranslationSource {
    MOJITO_TM_LEVERAGE(10),
    GOOGLE_MT(20),
    MICROSOFT_MT(30),
    NOOP(100),
    UNTRANSLATED(1000);

    private final int priority;

    TranslationSource(int priority) {
        this.priority = priority;
    }
    public int getPriority() {
        return priority;
    }
}
