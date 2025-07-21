package com.box.l10n.mojito.service.oaitranslate;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class AiTranslateTargetAutoFixTest {
  @Test
  void returnsNullWhenTargetIsNull() {
    assertNull(AiTranslateTargetAutoFix.fixTarget("source", null));
  }

  @Test
  void restoresNonBreakingSpace() {
    String source = "foo";
    String target = "foo\u0000a0bar";
    String expected = "foo\u00a0bar";
    assertEquals(expected, AiTranslateTargetAutoFix.fixTarget(source, target));
  }

  @Test
  void returnsUnchangedIfNoCorruption() {
    String source = "foo";
    String target = "foo bar";
    assertEquals(target, AiTranslateTargetAutoFix.fixTarget(source, target));
  }

  @Test
  void handlesMultipleCorruptedChars() {
    String source = "x";
    String target = "\u0000a0test\u0000a0";
    String expected = "\u00a0test\u00a0";
    assertEquals(expected, AiTranslateTargetAutoFix.fixTarget(source, target));
  }

  @Test
  void preservesLeadingAndTrailingWhitespace() {
    String source = " foo ";
    String target = "foo\u0000a0bar";
    String expected = " foo\u00a0bar ";
    assertEquals(expected, AiTranslateTargetAutoFix.fixTarget(source, target));
  }
}
