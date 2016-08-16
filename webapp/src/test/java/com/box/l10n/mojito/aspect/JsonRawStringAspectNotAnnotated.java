package com.box.l10n.mojito.aspect;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;

/**
 * @author jaurambault
 */
@JsonPropertyOrder(alphabetic=true)
public class JsonRawStringAspectNotAnnotated {

    @JsonRawValue
    public String getJsonString() {
        return "{\"a\": 1, \"b\": [1,2,3]}";
    }

    @JsonRawValue
    public String getNonJsonString() {
        return "This is a simple string that doesn't contain JSON";
    }
}
