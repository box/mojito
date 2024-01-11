package com.box.l10n.mojito.cli.command.extraction;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ExtractionDiffServiceTest {

  ExtractionDiffService extractionDiffService = new ExtractionDiffService();

  @Test
  public void joinLists() {
    List<String> l1 = List.of("E1", "E2");
    List<String> l2 = List.of("E3", "E4");
    List<String> result = extractionDiffService.joinLists(l1, l2);
    List<String> expectedResult = List.of("E1", "E2", "E3", "E4");
    assertEquals(expectedResult, result);
  }

  @Test
  public void joinLists_firstListNull() {
    List<String> l1 = null;
    List<String> l2 = List.of("E3", "E4");
    List<String> result = extractionDiffService.joinLists(l1, l2);
    List<String> expectedResult = List.of("E3", "E4");
    assertEquals(expectedResult, result);
  }

  @Test
  public void joinLists_secondListNull() {
    List<String> l1 = List.of("E1", "E2");
    List<String> l2 = null;
    List<String> result = extractionDiffService.joinLists(l1, l2);
    List<String> expectedResult = List.of("E1", "E2");
    assertEquals(expectedResult, result);
  }

  @Test
  public void joinTwoLists_nullInputs() {
    List<String> l1 = null;
    List<String> l2 = null;
    List<String> result = extractionDiffService.joinLists(l1, l2);
    assertTrue(result.isEmpty());
  }

  @Test
  public void joinTwoLists_emptyInputs() {
    List<String> l1 = new ArrayList<>();
    List<String> l2 = new ArrayList<>();
    List<String> result = extractionDiffService.joinLists(l1, l2);
    assertTrue(result.isEmpty());
  }
}
