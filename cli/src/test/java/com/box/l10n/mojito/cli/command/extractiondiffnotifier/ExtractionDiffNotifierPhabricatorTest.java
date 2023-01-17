package com.box.l10n.mojito.cli.command.extractiondiffnotifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffStatistics;
import com.box.l10n.mojito.phabricator.DifferentialRevision;
import org.junit.Test;

public class ExtractionDiffNotifierPhabricatorTest {

  @Test
  public void sendDiffStatistics() {
    DifferentialRevision differentialRevision = mock(DifferentialRevision.class);

    String objectIdentifier = "obj-id";
    int prNumber = 1;

    ExtractionDiffNotifierPhabricator extractionDiffNotifierPhabricator =
        new ExtractionDiffNotifierPhabricator(
            new ExtractionDiffNotifierMessageBuilder("{baseMessage}"),
            differentialRevision,
            objectIdentifier);

    final String msg =
        extractionDiffNotifierPhabricator.sendDiffStatistics(
            ExtractionDiffStatistics.builder().build());

    assertThat(msg).isEqualTo("ℹ️ 0 strings removed and 0 strings added (from 0 to 0)");
    verify(differentialRevision)
        .addComment(objectIdentifier, "ℹ️ 0 strings removed and 0 strings added (from 0 to 0)");
  }
}
