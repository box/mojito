package com.box.l10n.mojito.okapi;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author jaurambault
 */
public class ImportNoteBuilderTest {

  @Test
  public void testAddMessage() {

    ImportNoteBuilder noteBuilder = new ImportNoteBuilder();
    noteBuilder.addError("error");
    noteBuilder.addInfo("info");
    noteBuilder.addWarning("warning");

    assertEquals(
        "OK\n" + "[ERROR] error\n" + "[INFO] info\n" + "[WARNING] warning", noteBuilder.toString());

    noteBuilder.setNeedsReview(true);
    assertEquals(
        "NEEDS REVIEW\n" + "[ERROR] error\n" + "[INFO] info\n" + "[WARNING] warning",
        noteBuilder.toString());

    noteBuilder.setMustReview(true);
    assertEquals(
        "MUST REVIEW\n" + "[ERROR] error\n" + "[INFO] info\n" + "[WARNING] warning",
        noteBuilder.toString());
  }

  @Test
  public void testDontAddIfNull() {

    ImportNoteBuilder noteBuilder = new ImportNoteBuilder();
    noteBuilder.addError("error");
    noteBuilder.addInfo(null);

    assertEquals("OK\n" + "[ERROR] error", noteBuilder.toString());
  }
}
