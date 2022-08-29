package com.box.l10n.mojito.aspect;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;

/** @author jaurambault */
@JsonPropertyOrder(alphabetic = true)
public class JsonRawStringAspectAnnotated {

  @JsonRawValue
  @JsonRawString
  public String getJsonString() {
    return "{\"a\": 1, \"b\": [1,2,3]}";
  }

  @JsonRawValue
  @JsonRawString
  public String getNonJsonString() {
    return "This is a simple string that doesn't contain JSON";
  }
}
