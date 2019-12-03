package com.box.l10n.mojito.json;

import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Component;

/**
 * Simple utility to validate JSON
 *
 * @author jaurambault
 */
@Component
public class JsonValidator {

    /**
     * Indicates if a string contains valid JSON representation.
     *
     * {@code null} is considered a valid string.
     *
     * @param jsonStr the string to be validated (can be {@code null})
     * @return {@code true} if the string contains a valid JSON representation
     * else {@code true}
     */
    public boolean isValidJsonString(String jsonStr) {
        boolean isValid = true;

        if (jsonStr != null) {
            try {
                JSONParser jsonParser = new JSONParser();
                jsonParser.parse(jsonStr);
            } catch (Exception e) {
                isValid = false;
            }
        }

        return isValid;
    }

}
