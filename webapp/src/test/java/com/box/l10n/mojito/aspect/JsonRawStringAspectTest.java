package com.box.l10n.mojito.aspect;

import com.box.l10n.mojito.aspect.security.RunAsAspectConfig;
import com.box.l10n.mojito.json.JsonValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.springframework.context.annotation.FilterType;

/**
 * @author jaurambault
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Configuration
@ComponentScan(basePackageClasses = {JsonRawStringAspectTest.class, JsonValidator.class}, 
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {RunAsAspectConfig.class}))
@SpringBootTest(classes = {JsonRawStringAspectTest.class})
public class JsonRawStringAspectTest {

    @Test
    public void annotatedBeanGiveValidJSON() throws JsonProcessingException {

        JsonRawStringAspectAnnotated jsonRawStringAspectData = new JsonRawStringAspectAnnotated();
        ObjectMapper objectMapper = new ObjectMapper();
        String writeValueAsString = objectMapper.writeValueAsString(jsonRawStringAspectData);

        assertEquals("{\"jsonString\":{\"a\": 1, \"b\": [1,2,3]},\"nonJsonString\":\"This is a simple string that doesn't contain JSON\"}", writeValueAsString);

        try {
            new JSONParser().parse(writeValueAsString);
        } catch (ParseException pe) {
            fail("Annotated bean must provided a String that can't be parse by a JSON parser");
        }
    }

    @Test
    public void notAnnotatedBeanGiveValidInvalidJSON() throws JsonProcessingException {

        JsonRawStringAspectNotAnnotated jsonRawStringAspectNotAnnotated = new JsonRawStringAspectNotAnnotated();

        ObjectMapper objectMapper = new ObjectMapper();
        String writeValueAsString = objectMapper.writeValueAsString(jsonRawStringAspectNotAnnotated);

        assertEquals("{\"jsonString\":{\"a\": 1, \"b\": [1,2,3]},\"nonJsonString\":This is a simple string that doesn't contain JSON}", writeValueAsString);

        try {
            new JSONParser().parse(writeValueAsString);
            fail("Not annotated bean provides a String that can't be parsed by a JSON parser (if it now works it should be reviewed and @JsonRawString probably removed)");
        } catch (ParseException pe) {

        }
    }
}
