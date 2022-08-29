package com.box.l10n.mojito.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.json.JsonValidator;
import org.junit.Test;

/** @author jaurambault */
public class JsonValidatorTest {

  JsonValidator jsonValidator = new JsonValidator();

  @Test
  public void notJsonString() {
    assertFalse(jsonValidator.isValidJsonString("Some regular string"));
  }

  @Test
  public void jsonString() {
    assertTrue(jsonValidator.isValidJsonString("{ \"a\" : 1,\"2\" : 1}"));
  }

  @Test
  public void nullValue() {
    assertTrue(jsonValidator.isValidJsonString(null));
  }
}
