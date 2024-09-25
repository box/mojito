package com.box.l10n.mojito.okapi.filters;

import static com.box.l10n.mojito.okapi.filters.MacStringsFilter.MacStringsFilterPostProcessor.collapseBlankLines;
import static com.box.l10n.mojito.okapi.filters.MacStringsFilter.MacStringsFilterPostProcessor.ensureEndLineAsInInput;
import static com.box.l10n.mojito.okapi.filters.MacStringsFilter.MacStringsFilterPostProcessor.removeComments;
import static com.box.l10n.mojito.okapi.filters.MacStringsFilter.MacStringsFilterPostProcessor.removeUntranslated;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MacStringsFilterTest {

  @Test
  public void testRemoveUntranslated_NoUntranslatedEntries() {
    String input =
        """
                    /* Welcome message */
                    "welcome_message" = "Welcome to our app!";

                    /* Farewell message */
                    "farewell_message" = "Goodbye!";
                    """;
    String output =
        """
                    /* Welcome message */
                    "welcome_message" = "Welcome to our app!";

                    /* Farewell message */
                    "farewell_message" = "Goodbye!";
                    """;
    String result = removeUntranslated(input);
    assertEquals(output, result);
  }

  @Test
  public void testRemoveUntranslated_WithUntranslatedEntry() {
    String input =
        """
                    /* Welcome message */
                    "welcome_message" = "@#$untranslated$#@";

                    /* Farewell message */
                    "farewell_message" = "Goodbye!";
                    """;
    String output =
        """
                    /* Farewell message */
                    "farewell_message" = "Goodbye!";
                    """;
    String result = removeUntranslated(input);
    assertEquals(output, result);
  }

  @Test
  public void testRemoveUntranslated_UntranslatedEntryWithoutComment() {
    String input =
        """
                    "welcome_message" = "@#$untranslated$#@";

                    /* Farewell message */
                    "farewell_message" = "Goodbye!";
                    """;

    String output =
        """
                    /* Farewell message */
                    "farewell_message" = "Goodbye!";
                    """;
    String result = removeUntranslated(input);
    assertEquals(output, result);
  }

  @Test
  public void testRemoveUntranslated_MultipleUntranslatedEntries() {
    String input =
        """
                    /* Welcome message */
                    "welcome_message" = "@#$untranslated$#@";

                    /* Farewell message */
                    "farewell_message" = "@#$untranslated$#@";

                    /* Info message */
                    "info_message" = "Information available.";
                    """;
    String output =
        """
                    /* Info message */
                    "info_message" = "Information available.";
                    """;
    String result = removeUntranslated(input);
    assertEquals(output, result);
  }

  @Test
  public void testRemoveUntranslated_EscapedCharacters() {
    String input =
        """
                    /* Message with escaped characters */
                    "escaped_message" = "Line1\\nLine2\\tTabbed";

                    /* Untranslated message */
                    "untranslated_key" = "@#$untranslated$#@";
                    """;
    String output =
        """
                    /* Message with escaped characters */
                    "escaped_message" = "Line1\\nLine2\\tTabbed";
                    """;
    String result = removeUntranslated(input);
    System.out.println("[" + input + "]");
    System.out.println("[" + result + "]");
    assertEquals(output, result);
  }

  @Test
  public void testRemoveUntranslated_MultilineValue() {
    String input =
        """
                    /* Multiline message */
                    "multiline_message" = "This is a long message that spans multiple lines \\
                    in the .strings file, but will be rendered as a single line \\
                    when displayed in the application.";

                    /* Untranslated message */
                    "untranslated_key" = "@#$untranslated$#@";
                    """;
    String output =
        """
                    /* Multiline message */
                    "multiline_message" = "This is a long message that spans multiple lines \\
                    in the .strings file, but will be rendered as a single line \\
                    when displayed in the application.";
                    """;
    String result = removeUntranslated(input);
    assertEquals(output, result);
  }

  @Test
  public void testRemoveUntranslated_OnlyUntranslatedEntries() {
    String input =
        """
                    /* Untranslated message */
                    "untranslated_key1" = "@#$untranslated$#@";

                    /* Untranslated message */
                    "untranslated_key2" = "@#$untranslated$#@";
                    """;
    String output = "\n";
    String result = removeUntranslated(input);
    assertEquals(output, result);
  }

  @Test
  public void testRemoveUntranslated_EmptyFile() {
    String input = "";
    String output = "";
    String result = removeUntranslated(input);
    assertEquals(output, result);
  }

  @Test
  public void testRemoveUntranslated_UntranslatedAtEnd() {
    String input =
        """
                    /* Welcome message */
                    "welcome_message" = "Welcome!";

                    /* Untranslated message */
                    "untranslated_key" = "@#$untranslated$#@";
                    """;
    String output =
        """
                    /* Welcome message */
                    "welcome_message" = "Welcome!";
                    """;
    String result = removeUntranslated(input);
    assertEquals(output, result);
  }

  @Test
  public void testRemoveComments_NoComments() {
    String input =
        """
            "welcome_message" = "Welcome to our app!";
            "farewell_message" = "Goodbye!";
            """;
    String expected =
        """
            "welcome_message" = "Welcome to our app!";
            "farewell_message" = "Goodbye!";
            """;
    String result = removeComments(input);
    assertEquals(expected, result);
  }

  @Test
  public void testRemoveComments_WithBlockComments() {
    String input =
        """
            /* Welcome message */
            "welcome_message" = "Welcome to our app!";

            /* Farewell message */
            "farewell_message" = "Goodbye!";
            """;
    String expected =
        """
            "welcome_message" = "Welcome to our app!";

            "farewell_message" = "Goodbye!";
            """;
    String result = removeComments(input);
    assertEquals(expected, result);
  }

  @Test
  public void testRemoveComments_UntranslatedEntryWithComments() {
    String input =
        """
            /* Untranslated message */
            "untranslated_key" = "@#$untranslated$#@";
            """;
    String expected =
        """
            "untranslated_key" = "@#$untranslated$#@";
            """;
    String result = removeComments(input);
    assertEquals(expected, result);
  }

  @Test
  public void testRemoveComments_EscapedCharacters() {
    String input =
        """
            /* Message with escaped characters */
            "escaped_message" = "Line1\\nLine2\\tTabbed";
            """;
    String expected =
        """
            "escaped_message" = "Line1\\nLine2\\tTabbed";
            """;
    String result = removeComments(input);
    assertEquals(expected, result);
  }

  @Test
  public void testRemoveComments_EmptyFile() {
    String input = "";
    String expected = "";
    String result = removeComments(input);
    assertEquals(expected, result);
  }

  @Test
  public void testRemoveComments_MultilineBlockComment() {
    String input =
        """
            /* Multiline
               block comment */
            "message" = "Hello!";
            """;
    String expected =
        """
            "message" = "Hello!";
            """;
    String result = removeComments(input);
    assertEquals(expected, result);
  }

  @Test
  public void testRemoveComments_NestedBlockComments() {
    String input =
        """
            /* Outer comment
                /* Inner comment */
            */
            "message" = "Hello!";
            """;
    String expected =
        """
            */
            "message" = "Hello!";
            """;
    String result = removeComments(input);
    assertEquals(expected, result);
  }

  @Test
  public void testAddEndLineAsInInput() {
    assertEquals("", ensureEndLineAsInInput("", ""));
    assertEquals("\n", ensureEndLineAsInInput("", "\n"));
    assertEquals("\n", ensureEndLineAsInInput("\n", "\n"));
    assertEquals("", ensureEndLineAsInInput("\n", ""));
  }

  @Test
  public void testCollapseBlankLines() {
    assertEquals("", collapseBlankLines(""));
    assertEquals("\n", collapseBlankLines("\n"));
    assertEquals("\n", collapseBlankLines("\n\n"));
    assertEquals("\na\nb\n\nc\n\n", collapseBlankLines("\na\nb\n\nc\n\n"));
    assertEquals("a\nb\n\nc\n\n", collapseBlankLines("a\nb\n\nc\n\n"));
    assertEquals("\na\nb\n\nc", collapseBlankLines("\na\nb\n\nc"));
  }
}
