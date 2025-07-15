package com.box.l10n.mojito.json;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JsonObjectRemoverByValueTest {

  @Test
  void removeEmptyObject() {
    assertEquals("{ }", JsonObjectRemoverByValue.remove("{}", "anything"));
  }

  @Test
  void removeEmptyArray() {
    assertEquals("[ ]", JsonObjectRemoverByValue.remove("[]", "anything"));
  }

  @Test
  void removeEmptyObjectNone() {
    String jsonContent = "{\n" + "  \"k\" : \"v\"\n" + "}";
    assertEquals(jsonContent, JsonObjectRemoverByValue.remove(jsonContent, "anything"));
  }

  @Test
  void removeTextualFields() {
    assertEquals(
        "{ }",
        JsonObjectRemoverByValue.remove(
            "{\n"
                + "  \"value1\": \"##$UNTRANSLATED$##\",\n"
                + "  \"value2\": \"##$UNTRANSLATED$##\"\n"
                + "}",
            "##$UNTRANSLATED$##"));
  }

  @Test
  void removeInObject() {
    assertEquals(
        "{\n"
            + "  \"mykey2\" : {\n"
            + "    \"value\" : \"myvalue2\",\n"
            + "    \"comment\" : \"mycomment2\",\n"
            + "    \"usages\" : \"myusages2\"\n"
            + "  }\n"
            + "}",
        JsonObjectRemoverByValue.remove(
            "{\n"
                + "  \"mykey1\": {\n"
                + "    \"value\": \"##$UNTRANSLATED$##\",\n"
                + "    \"comment\": \"mycomment1\",\n"
                + "    \"usages\": \"myusages1\"\n"
                + "  },\n"
                + "  \"mykey2\": {\n"
                + "    \"value\": \"myvalue2\",\n"
                + "    \"comment\": \"mycomment2\",\n"
                + "    \"usages\": \"myusages2\"\n"
                + "  }\n"
                + "}",
            "##$UNTRANSLATED$##"));
  }

  @Test
  void removeInArray() {
    assertEquals(
        "[ {\n"
            + "  \"key\" : \"key3\",\n"
            + "  \"value\" : \"myvalue3\",\n"
            + "  \"comment\" : \"mycomment3\",\n"
            + "  \"usages\" : \"myusages3\"\n"
            + "} ]",
        JsonObjectRemoverByValue.remove(
            "[\n"
                + "  {\n"
                + "    \"key\": \"key3\",\n"
                + "    \"value\": \"myvalue3\",\n"
                + "    \"comment\": \"mycomment3\",\n"
                + "    \"usages\": \"myusages3\"\n"
                + "  },\n"
                + "  {\n"
                + "    \"key\": \"key4\",\n"
                + "    \"value\": \"##$UNTRANSLATED$##\",\n"
                + "    \"comment\": \"mycomment4\",\n"
                + "    \"usages\": \"myusages4\"\n"
                + "  }\n"
                + "]",
            "##$UNTRANSLATED$##"));
  }

  @Test
  void removeNested() {
    String jsonContent =
        "{\n"
            + "  \"mykey1\": {\n"
            + "    \"value\": \"##$UNTRANSLATED$##\",\n"
            + "    \"comment\": \"mycomment1\",\n"
            + "    \"usages\": \"myusages1\"\n"
            + "  },\n"
            + "  \"mykey2\": {\n"
            + "    \"value\": \"myvalue2\",\n"
            + "    \"comment\": \"mycomment2\",\n"
            + "    \"usages\": \"myusages2\"\n"
            + "  },\n"
            + "  \"array\": [\n"
            + "    {\n"
            + "      \"key\": \"key3\",\n"
            + "      \"value\": \"myvalue3\",\n"
            + "      \"comment\": \"mycomment3\",\n"
            + "      \"usages\": \"myusages3\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"key\": \"key4\",\n"
            + "      \"value\": \"##$UNTRANSLATED$##\",\n"
            + "      \"comment\": \"mycomment4\",\n"
            + "      \"usages\": \"myusages4\"\n"
            + "    }\n"
            + "  ],\n"
            + "  \"object\": {\n"
            + "    \"mykey5\": {\n"
            + "      \"value\": \"##$UNTRANSLATED$##\",\n"
            + "      \"comment\": \"mycomment5\",\n"
            + "      \"usages\": \"myusages5\"\n"
            + "    },\n"
            + "    \"mykey6\": {\n"
            + "      \"value\": \"myvalue6\",\n"
            + "      \"comment\": \"mycomment6\",\n"
            + "      \"usages\": \"myusages6\"\n"
            + "    }\n"
            + "  },\n"
            + "  \"arrayOfObject\": [\n"
            + "    {\n"
            + "      \"mykey7\": {\n"
            + "        \"value\": \"value7\",\n"
            + "        \"comment\": \"mycomment7\",\n"
            + "        \"usages\": \"myusages7\"\n"
            + "      },\n"
            + "      \"mykey8\": {\n"
            + "        \"value\": \"##$UNTRANSLATED$##\",\n"
            + "        \"comment\": \"mycomment8\",\n"
            + "        \"usages\": \"myusages8\"\n"
            + "      }\n"
            + "    },\n"
            + "    {\n"
            + "      \"mykey9\": {\n"
            + "        \"value\": \"myvalue9\",\n"
            + "        \"comment\": \"mycomment9\",\n"
            + "        \"usages\": \"myusages9\"\n"
            + "      }\n"
            + "    }\n"
            + "  ]\n"
            + "}\n";
    String remove = JsonObjectRemoverByValue.remove(jsonContent, "##$UNTRANSLATED$##");
    assertEquals(
        "{\n"
            + "  \"mykey2\" : {\n"
            + "    \"value\" : \"myvalue2\",\n"
            + "    \"comment\" : \"mycomment2\",\n"
            + "    \"usages\" : \"myusages2\"\n"
            + "  },\n"
            + "  \"array\" : [ {\n"
            + "    \"key\" : \"key3\",\n"
            + "    \"value\" : \"myvalue3\",\n"
            + "    \"comment\" : \"mycomment3\",\n"
            + "    \"usages\" : \"myusages3\"\n"
            + "  } ],\n"
            + "  \"object\" : {\n"
            + "    \"mykey6\" : {\n"
            + "      \"value\" : \"myvalue6\",\n"
            + "      \"comment\" : \"mycomment6\",\n"
            + "      \"usages\" : \"myusages6\"\n"
            + "    }\n"
            + "  },\n"
            + "  \"arrayOfObject\" : [ {\n"
            + "    \"mykey7\" : {\n"
            + "      \"value\" : \"value7\",\n"
            + "      \"comment\" : \"mycomment7\",\n"
            + "      \"usages\" : \"myusages7\"\n"
            + "    }\n"
            + "  }, {\n"
            + "    \"mykey9\" : {\n"
            + "      \"value\" : \"myvalue9\",\n"
            + "      \"comment\" : \"mycomment9\",\n"
            + "      \"usages\" : \"myusages9\"\n"
            + "    }\n"
            + "  } ]\n"
            + "}",
        remove);
  }

  @Test
  public void testControlCharsWithJackson() {
    ObjectMapper mapper = new ObjectMapper();

    assertJacksonParses("a\\tb", "a\tb"); // Tab
    assertJacksonParses("a\\nb", "a\nb"); // Newline
    assertJacksonParses("a\\rb", "a\rb"); // Carriage return
    assertJacksonParses("a\\u0000b", "a\u0000b");
    assertJacksonParses("a\\u0007b", "a\u0007b");
    assertJacksonParses("a\\u001Fb", "a\u001Fb");

    assertJacksonFails("a\u0000b");
    assertJacksonFails("a\u0000b");
    assertJacksonFails("a\u0007b");
    assertJacksonFails("a\u001Fb");
  }

  private void assertJacksonParses(String jsonEscapedValue, String expected) {
    String json = "{\n  \"text\" : \"" + jsonEscapedValue + "\"\n}";
    Assertions.assertEquals(json, JsonObjectRemoverByValue.remove(json, "whatever"));
  }

  private void assertJacksonFails(String jsonEscapedValue) {
    try {
      String json = "{\"text\":\"" + jsonEscapedValue + "\"}";
      JsonObjectRemoverByValue.remove(json, "whatever");
      fail("Expected failure for: " + json);
    } catch (Exception ignored) {
    }
  }
}
