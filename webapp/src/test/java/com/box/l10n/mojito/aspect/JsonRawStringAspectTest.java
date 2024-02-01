package com.box.l10n.mojito.aspect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.box.l10n.mojito.json.JsonValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author jaurambault
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
      JsonRawStringAspectTest.class,
      JsonRawStringAspectAnnotated.class,
      JsonRawStringAspectNotAnnotated.class,
      JsonRawStringAspect.class,
      JsonValidator.class,
      JsonRawStringAspectConfig.class
    })
public class JsonRawStringAspectTest {

  @Test
  public void annotatedBeanGiveValidJSON() throws JsonProcessingException {

    JsonRawStringAspectAnnotated jsonRawStringAspectData = new JsonRawStringAspectAnnotated();
    ObjectMapper objectMapper = new ObjectMapper();
    String writeValueAsString = objectMapper.writeValueAsString(jsonRawStringAspectData);

    assertEquals(
        "{\"jsonString\":{\"a\": 1, \"b\": [1,2,3]},\"nonJsonString\":\"This is a simple string that doesn't contain JSON\"}",
        writeValueAsString);

    try {
      new JSONParser().parse(writeValueAsString);
    } catch (ParseException pe) {
      fail("Annotated bean must provided a String that can't be parse by a JSON parser");
    }
  }

  @Test
  public void notAnnotatedBeanGiveValidInvalidJSON() throws JsonProcessingException {

    JsonRawStringAspectNotAnnotated jsonRawStringAspectNotAnnotated =
        new JsonRawStringAspectNotAnnotated();

    ObjectMapper objectMapper = new ObjectMapper();
    String writeValueAsString = objectMapper.writeValueAsString(jsonRawStringAspectNotAnnotated);

    assertEquals(
        "{\"jsonString\":{\"a\": 1, \"b\": [1,2,3]},\"nonJsonString\":This is a simple string that doesn't contain JSON}",
        writeValueAsString);

    try {
      new JSONParser().parse(writeValueAsString);
      fail(
          "Not annotated bean provides a String that can't be parsed by a JSON parser (if it now works it should be reviewed and @JsonRawString probably removed)");
    } catch (ParseException pe) {

    }
  }
}
