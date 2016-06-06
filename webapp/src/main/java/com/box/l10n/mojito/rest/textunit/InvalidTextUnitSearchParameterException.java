package com.box.l10n.mojito.rest.textunit;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown to indicate that a search parameter is not valid for
 * {@link TextUnitWS#findAll(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.ArrayList, com.box.l10n.mojito.service.tm.search.UsedFilter, com.box.l10n.mojito.service.tm.search.TranslatedFilter, java.lang.Integer, java.lang.Integer) }
 *
 * @author jaurambault
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid search parameter")
public class InvalidTextUnitSearchParameterException extends Exception {

    public InvalidTextUnitSearchParameterException(String string) {
    }

}
