package com.box.l10n.mojito.service.tm.search;

import com.github.pnowy.nc.core.NativeCriteria;

/**
 *
 * @author jeanaurambault
 */
public class TextUnitSearcherError extends RuntimeException {

    NativeCriteria nativeCriteria;

    public TextUnitSearcherError(NativeCriteria nativeCriteria, String message, Throwable cause) {
        super(message, cause);
        this.nativeCriteria = nativeCriteria;
    }
}
