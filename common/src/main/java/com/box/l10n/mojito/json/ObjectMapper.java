package com.box.l10n.mojito.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.springframework.stereotype.Component;

/**
 * Extends {@link com.fasterxml.jackson.databind.ObjectMapper} to provide
 * convenient methods.
 *
 * @author jaurambault
 */
@Component
public class ObjectMapper extends com.fasterxml.jackson.databind.ObjectMapper {

    public ObjectMapper() {
        JodaModule jodaModule = new JodaModule();
        registerModule(jodaModule);
    }

    /**
     * Same as {@link #writeValueAsString(java.lang.Object) but throws
     * {@link RuntimeException} instead of {@link JsonProcessingException}
     *
     * @param value
     * @return
     */
    public String writeValueAsStringUnsafe(Object value) {
        try {
            return super.writeValueAsString(value);
        } catch (JsonProcessingException jpe) {
            throw new RuntimeException(jpe);
        }
    }

}
