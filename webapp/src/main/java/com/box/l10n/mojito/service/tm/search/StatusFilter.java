package com.box.l10n.mojito.service.tm.search;

import com.box.l10n.mojito.entity.TMTextUnitVariant;

/**
 * To filter text units based on status, status here is not mapping specifically
 * to {@link TMTextUnitVariant.Status} but a combination of this attribute with
 * other text units attributes like
 * {@link TMTextUnitVariant#includedInLocalizedFile}.
 *
 * @author jaurambault
 */
public enum StatusFilter {

    /**
     * All TextUnits translated and not translated with any status.
     */
    ALL,
    /**
     * All TextUnits that have a translation with any status
     * ({@link TMTextUnitVariant.Status}.
     */
    TRANSLATED,
    /**
     * All TextUnits that have no translation
     */
    UNTRANSLATED,
    /**
     * All TextUnits that have a translation with any status
     * ({@link TMTextUnitVariant.Status} but not rejected.
     */
    TRANSLATED_AND_NOT_REJECTED,
    /**
     * TextUnits with status ({@link TMTextUnitVariant.Status#APPROVED})
     * or ({@link TMTextUnitVariant.Status#REVIEW_NEEDED}) but not rejected.
     */
    APPROVED_OR_NEEDS_REVIEW_AND_NOT_REJECTED,
     /**
     * TextUnits with status ({@link TMTextUnitVariant.Status#APPROVED})
     * or ({@link TMTextUnitVariant.Status#REVIEW_NEEDED}) but not rejected.
     */
    APPROVED_AND_NOT_REJECTED,
    /**
     * TextUnits that should be sent for translation.
     *
     * Includes text units without translation or with status
     * ({@link TMTextUnitVariant.Status#TRANSLATION_NEEDED}) or not included in
     * file ({@link TMTextUnitVariant#includedInLocalizedFile}) .
     */
    FOR_TRANSLATION,
    /**
     * TextUnits with status ({@link TMTextUnitVariant.Status#REVIEW_NEEDED}).
     */
    REVIEW_NEEDED,
    /**
     * TextUnits that don't have status
     * ({@link TMTextUnitVariant.Status#REVIEW_NEEDED}).
     */
    REVIEW_NOT_NEEDED,
    /**
     * TextUnits with status ({@link TMTextUnitVariant.Status#TRANSLATION_NEEDED}).
     */
    TRANSLATION_NEEDED,
    /**
     * TextUnits that are rejected, ie
     * {@link TMTextUnitVariant#includedInLocalizedFile} is false.
     */
    REJECTED,
    /**
     * TextUnits that are not rejected, ie
     * {@link TMTextUnitVariant#includedInLocalizedFile} is true.
     */
    NOT_REJECTED,

}
