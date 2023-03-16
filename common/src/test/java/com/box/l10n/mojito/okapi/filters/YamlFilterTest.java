package com.box.l10n.mojito.okapi.filters;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.Yaml;

public class YamlFilterTest {

  public static final String A_LONG_STRING =
      "this is a very long string that we'd love to get written on multiple lines."
          + " this is a very long string that we'd love to get written on multiple lines."
          + " this is a very long string that we'd love to get written on multiple lines."
          + " this is a very long string that we'd love to get written on multiple lines."
          + " this is a very long string that we'd love to get written on multiple lines."
          + " this is a very long string that we'd love to get written on multiple lines.";

  public static final String A_LONG_STRING_2 =
      "this is a very long string \nthat we'd love to get written on multiple lines."
          + " this is a very long string that we'd love to get written on multiple lines."
          + " this is a very long string that we'd love to get written on multiple lines."
          + " this is a very long string \nthat we'd love to get written on multiple lines."
          + " this is a very long string that we'd love to get written on multiple lines."
          + " this is a very long string that we'd love to get written on multiple lines.";

  /** logger */
  static Logger logger = LoggerFactory.getLogger(YamlFilterTest.class);

  @Test
  public void reindent() {
    final HashMap<String, String> stringStringHashMap = new LinkedHashMap<>();
    stringStringHashMap.put("string1", "value1");
    stringStringHashMap.put("aLongString", A_LONG_STRING);
    stringStringHashMap.put("aLongString2", A_LONG_STRING_2);
    stringStringHashMap.put(
        "aLongStringNoSpace",
        IntStream.range(0, 100).mapToObj(Objects::toString).collect(Collectors.joining(".")));
    stringStringHashMap.put("string2", "value2");

    DumperOptions options1 = new DumperOptions();
    printWithOptions("options 1", stringStringHashMap, options1);

    DumperOptions options2 = new DumperOptions();
    options2.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    printWithOptions("options 2", stringStringHashMap, options2);

    DumperOptions options3 = new DumperOptions();
    options3.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options3.setWidth(20);
    printWithOptions("options 3", stringStringHashMap, options3);

    // This one?
    DumperOptions options4 = new DumperOptions();
    options4.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options4.setSplitLines(false);
    printWithOptions("options 4", stringStringHashMap, options4);

    DumperOptions options5 = new DumperOptions();
    options5.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options5.setDefaultScalarStyle(ScalarStyle.LITERAL);
    printWithOptions("options 5", stringStringHashMap, options5);

    DumperOptions options6 = new DumperOptions();
    options6.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options6.setDefaultScalarStyle(ScalarStyle.FOLDED);
    printWithOptions("options 6", stringStringHashMap, options6);
  }

  static void printWithOptions(
      String title, HashMap<String, String> stringStringHashMap, DumperOptions option) {
    final String dump = new Yaml(option).dump(stringStringHashMap);
    logger.info("\n==== {} =====\ndump:\n\n{}\n", title, dump);
    LinkedHashMap<String, String> load = new Yaml().load(dump);
    final String aLongString = load.get("aLongString");
    if (aLongString != null && !A_LONG_STRING.equals(aLongString)) {
      logger.error("actual:\n{}\n<<<<<<\n\nexpected:\n{}\n<<<<<<", aLongString, A_LONG_STRING);
      throw new RuntimeException("A_LONG_STRING different!");
    }
    final String aLongString2 = load.get("aLongString2");
    if (aLongString2 != null && !A_LONG_STRING_2.equals(aLongString2)) {
      logger.error("actual:\n{}\n<<<<<<\n\nexpected:\n{}\n<<<<<<", aLongString2, A_LONG_STRING_2);
      throw new RuntimeException("A_LONG_STRING_2 different!");
    }
  }
}
