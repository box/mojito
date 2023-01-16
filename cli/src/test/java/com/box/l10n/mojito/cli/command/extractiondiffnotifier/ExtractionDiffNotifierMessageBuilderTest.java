package com.box.l10n.mojito.cli.command.extractiondiffnotifier;

import static org.junit.Assert.*;

import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffStatistics;
import org.junit.Test;

public class ExtractionDiffNotifierMessageBuilderTest {

  @Test
  public void getMessageInfo() {
    ExtractionDiffNotifierMessageBuilder extractionDiffNotifierMessageBuilder =
        new ExtractionDiffNotifierMessageBuilder("{baseMessage}");

    assertEquals(
        "ℹ️ 0 strings removed and 1 string added (from 10 to 11)",
        extractionDiffNotifierMessageBuilder.getMessage(
            ExtractionDiffStatistics.builder().added(1).removed(0).base(10).current(11).build()));
  }

  @Test
  public void getMessageWarning() {
    ExtractionDiffNotifierMessageBuilder extractionDiffNotifierMessageBuilder =
        new ExtractionDiffNotifierMessageBuilder("{baseMessage}");

    assertEquals(
        "⚠️ 10 strings removed and 8 strings added (from 20 to 18)",
        extractionDiffNotifierMessageBuilder.getMessage(
            ExtractionDiffStatistics.builder().added(8).removed(10).base(20).current(18).build()));
  }

  @Test
  public void getMessageError() {
    ExtractionDiffNotifierMessageBuilder extractionDiffNotifierMessageBuilder =
        new ExtractionDiffNotifierMessageBuilder("{baseMessage}");

    assertEquals(
        "🛑 200 strings removed and 0 strings added (from 500 to 300)",
        extractionDiffNotifierMessageBuilder.getMessage(
            ExtractionDiffStatistics.builder()
                .added(0)
                .removed(200)
                .base(500)
                .current(300)
                .build()));
  }

  @Test
  public void withTemplate() {
    ExtractionDiffNotifierMessageBuilder extractionDiffNotifierMessageBuilder =
        new ExtractionDiffNotifierMessageBuilder(
            "{baseMessage}. Check [[https://build.org/1234|build]].");

    assertEquals(
        "🛑 200 strings removed and 0 strings added (from 500 to 300). Check [[https://build.org/1234|build]].",
        extractionDiffNotifierMessageBuilder.getMessage(
            ExtractionDiffStatistics.builder()
                .added(0)
                .removed(200)
                .base(500)
                .current(300)
                .build()));
  }
}
