package com.box.l10n.mojito.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonIndenter {

    public static String indent(String jsonStr) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        Object json = null;
        try {
            json = objectMapper.readValue(jsonStr, Object.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        String indented = objectMapper.writeValueAsStringUnchecked(json);
        return indented;
    }
}
