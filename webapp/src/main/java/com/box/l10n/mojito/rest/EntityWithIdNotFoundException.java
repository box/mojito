package com.box.l10n.mojito.rest;

import com.ibm.icu.text.MessageFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 *
 * @author jeanaurambault
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class EntityWithIdNotFoundException extends Exception {

    public EntityWithIdNotFoundException(String entity, Long id) {
        super(getMessage(entity, id));
    }
    
    static String getMessage(String entity, Long id) {
        return MessageFormat.format("{0} with id: {1} not found", entity, id.toString());
    }

}
