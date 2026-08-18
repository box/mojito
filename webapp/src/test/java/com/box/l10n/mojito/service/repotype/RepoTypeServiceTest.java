package com.box.l10n.mojito.service.repotype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.box.l10n.mojito.entity.RepoType;
import com.box.l10n.mojito.entity.RepoTypeIntegrityChecker;
import com.box.l10n.mojito.rest.repotype.RepoTypeWithIdNotFoundException;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerType;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Contract tests for {@link RepoTypeService}. Behavior under test is defined in JavaDoc and {@code
 * docs/internal/Architecture.md} (Repo Types).
 */
public class RepoTypeServiceTest extends ServiceTestBase {

  @Autowired RepoTypeService repoTypeService;

  @Autowired RepoTypeRepository repoTypeRepository;

  @Autowired RepoTypeIntegrityCheckerRepository repoTypeIntegrityCheckerRepository;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Test
  public void testCreateRepoTypeAssignsIdAndName() throws Exception {
    String name = testIdWatcher.getEntityName("React");
    RepoType created = repoTypeService.createRepoType(name, null, null, null);

    assertNotNull(created.getId());
    assertEquals(name, created.getName());
  }

  @Test
  public void testCreateRepoTypeNullAiPromptStoredAsEmptyString() throws Exception {
    String name = testIdWatcher.getEntityName("Android");
    RepoType created = repoTypeService.createRepoType(name, null, null, null);

    assertEquals("", created.getAiPrompt());
  }

  @Test
  public void testCreateRepoTypePersistsDescription() throws Exception {
    String name = testIdWatcher.getEntityName("WithDescription");
    RepoType created =
        repoTypeService.createRepoType(name, "Android strings.xml", "", null);

    assertEquals("Android strings.xml", created.getDescription());
  }

  @Test
  public void testCreateRepoTypeNullIntegrityCheckersMeansNone() throws Exception {
    String name = testIdWatcher.getEntityName("NoCheckers");
    RepoType created = repoTypeService.createRepoType(name, null, "", null);

    assertNotNull(created.getIntegrityCheckers());
    assertTrue(created.getIntegrityCheckers().isEmpty());
  }

  @Test
  public void testCreateRepoTypePersistsAiPrompt() throws Exception {
    String name = testIdWatcher.getEntityName("WithPrompt");
    String prompt = "Preserve {placeholders} and ICU plural/select skeletons.";

    RepoType created = repoTypeService.createRepoType(name, null, prompt, null);

    assertEquals(prompt, created.getAiPrompt());
  }

  @Test
  public void testCreateRepoTypePersistsMultipleCheckersForSameExtension() throws Exception {
    String name = testIdWatcher.getEntityName("WithCheckers");
    Set<RepoTypeIntegrityChecker> checkers = new HashSet<>();
    checkers.add(checker("properties", IntegrityCheckerType.MESSAGE_FORMAT));
    checkers.add(checker("properties", IntegrityCheckerType.TRAILING_WHITESPACE));

    RepoType created = repoTypeService.createRepoType(name, null, "", checkers);

    assertEquals(2, created.getIntegrityCheckers().size());
    assertCheckerPresent(created, "properties", IntegrityCheckerType.MESSAGE_FORMAT);
    assertCheckerPresent(created, "properties", IntegrityCheckerType.TRAILING_WHITESPACE);
  }

  @Test
  public void testCreateRepoTypeDuplicateNameThrows() throws Exception {
    String name = testIdWatcher.getEntityName("Duplicate");
    repoTypeService.createRepoType(name, null, "", null);

    try {
      repoTypeService.createRepoType(name, "again", "", null);
      fail("Expected RepoTypeNameAlreadyUsedException");
    } catch (RepoTypeNameAlreadyUsedException expected) {
      assertNotNull(expected.getMessage());
    }
  }

  @Test
  public void testGetRepoTypeByIdReturnsType() throws Exception {
    String name = testIdWatcher.getEntityName("ById");
    RepoType created = repoTypeService.createRepoType(name, "desc", "prompt", null);

    RepoType loaded = repoTypeService.getRepoTypeById(created.getId());

    assertEquals(created.getId(), loaded.getId());
    assertEquals(name, loaded.getName());
  }

  @Test
  public void testGetRepoTypeByIdMissingThrows() {
    try {
      repoTypeService.getRepoTypeById(987654321L);
      fail("Expected RepoTypeWithIdNotFoundException");
    } catch (RepoTypeWithIdNotFoundException expected) {
      assertTrue(expected.getMessage().contains("987654321"));
    }
  }

  @Test
  public void testGetRepoTypesNullReturnsAllOrderedByName() throws Exception {
    String nameApple = testIdWatcher.getEntityName("Apple");
    String nameZebra = testIdWatcher.getEntityName("Zebra");
    repoTypeService.createRepoType(nameZebra, null, "", null);
    repoTypeService.createRepoType(nameApple, null, "", null);

    List<RepoType> all = repoTypeService.getRepoTypes(null);
    assertNotNull(all);

    List<String> ourNames =
        all.stream()
            .map(RepoType::getName)
            .filter(n -> n.equals(nameApple) || n.equals(nameZebra))
            .collect(Collectors.toList());
    assertEquals(List.of(nameApple, nameZebra), ourNames);
  }

  @Test
  public void testGetRepoTypesBlankReturnsAllOrderedByName() throws Exception {
    String nameApple = testIdWatcher.getEntityName("Apple");
    String nameZebra = testIdWatcher.getEntityName("Zebra");
    repoTypeService.createRepoType(nameZebra, null, "", null);
    repoTypeService.createRepoType(nameApple, null, "", null);

    List<RepoType> blankFilter = repoTypeService.getRepoTypes("   ");
    assertNotNull(blankFilter);

    List<String> blankNames =
        blankFilter.stream()
            .map(RepoType::getName)
            .filter(n -> n.equals(nameApple) || n.equals(nameZebra))
            .collect(Collectors.toList());
    assertEquals(List.of(nameApple, nameZebra), blankNames);
  }

  @Test
  public void testGetRepoTypesByNameExactMatch() throws Exception {
    String name = testIdWatcher.getEntityName("Exact");
    repoTypeService.createRepoType(name, null, "prompt", null);

    List<RepoType> found = repoTypeService.getRepoTypes(name);

    assertEquals(1, found.size());
    assertEquals(name, found.get(0).getName());
  }

  @Test
  public void testGetRepoTypesByUnknownNameReturnsEmptyList() throws Exception {
    String name = testIdWatcher.getEntityName("Exact");
    repoTypeService.createRepoType(name, null, "prompt", null);

    List<RepoType> missing = repoTypeService.getRepoTypes(name + "-unknown");

    assertNotNull(missing);
    assertTrue(missing.isEmpty());
  }

  @Test
  public void testUpdateRepoTypeNullFieldsLeaveValuesUnchanged() throws Exception {
    String name = testIdWatcher.getEntityName("PatchNull");
    Set<RepoTypeIntegrityChecker> checkers = new HashSet<>();
    checkers.add(checker("resw", IntegrityCheckerType.COMPOSITE_FORMAT));
    RepoType created =
        repoTypeService.createRepoType(name, "original desc", "original prompt", checkers);

    RepoType updated =
        repoTypeService.updateRepoType(created.getId(), null, null, null, null);

    assertEquals(name, updated.getName());
    assertEquals("original desc", updated.getDescription());
    assertEquals("original prompt", updated.getAiPrompt());
    assertEquals(1, updated.getIntegrityCheckers().size());
    assertCheckerPresent(updated, "resw", IntegrityCheckerType.COMPOSITE_FORMAT);
  }

  @Test
  public void testUpdateRepoTypeEmptyAiPromptClearsPrompt() throws Exception {
    String name = testIdWatcher.getEntityName("ClearPrompt");
    RepoType created = repoTypeService.createRepoType(name, null, "non-empty", null);

    RepoType updated =
        repoTypeService.updateRepoType(created.getId(), null, null, "", null);

    assertEquals("", updated.getAiPrompt());
  }

  @Test
  public void testUpdateRepoTypeNullIntegrityCheckersLeaveUnchanged() throws Exception {
    String name = testIdWatcher.getEntityName("LeaveCheckers");
    Set<RepoTypeIntegrityChecker> checkers = new HashSet<>();
    checkers.add(checker("properties", IntegrityCheckerType.MESSAGE_FORMAT));
    RepoType created = repoTypeService.createRepoType(name, null, "", checkers);

    RepoType leftAlone =
        repoTypeService.updateRepoType(created.getId(), null, null, null, null);

    assertEquals(1, leftAlone.getIntegrityCheckers().size());
  }

  @Test
  public void testUpdateRepoTypeEmptyIntegrityCheckersClearsAll() throws Exception {
    String name = testIdWatcher.getEntityName("ClearCheckers");
    Set<RepoTypeIntegrityChecker> checkers = new HashSet<>();
    checkers.add(checker("properties", IntegrityCheckerType.MESSAGE_FORMAT));
    RepoType created = repoTypeService.createRepoType(name, null, "", checkers);

    RepoType cleared =
        repoTypeService.updateRepoType(
            created.getId(), null, null, null, new HashSet<>());

    assertTrue(cleared.getIntegrityCheckers().isEmpty());
    assertTrue(repoTypeIntegrityCheckerRepository.findByRepoType(cleared).isEmpty());
  }

  @Test
  public void testUpdateRepoTypeRenameToSameNameAllowed() throws Exception {
    String name = testIdWatcher.getEntityName("SameName");
    RepoType type = repoTypeService.createRepoType(name, null, "", null);

    RepoType updated =
        repoTypeService.updateRepoType(type.getId(), name, "desc", null, null);

    assertEquals(name, updated.getName());
    assertEquals("desc", updated.getDescription());
  }

  @Test
  public void testUpdateRepoTypeRenameConflictThrows() throws Exception {
    String nameA = testIdWatcher.getEntityName("NameA");
    String nameB = testIdWatcher.getEntityName("NameB");
    RepoType typeA = repoTypeService.createRepoType(nameA, null, "", null);
    repoTypeService.createRepoType(nameB, null, "", null);

    try {
      repoTypeService.updateRepoType(typeA.getId(), nameB, null, null, null);
      fail("Expected RepoTypeNameAlreadyUsedException");
    } catch (RepoTypeNameAlreadyUsedException expected) {
      assertNotNull(expected.getMessage());
    }
  }

  @Test
  public void testUpdateRepoTypeMissingIdThrows() throws Exception {
    try {
      repoTypeService.updateRepoType(987654321L, "x", null, null, null);
      fail("Expected RepoTypeWithIdNotFoundException");
    } catch (RepoTypeWithIdNotFoundException expected) {
      assertTrue(expected.getMessage().contains("987654321"));
    }
  }

  @Test
  public void testUpdateIntegrityCheckersReusesIdForSamePair() throws Exception {
    String name = testIdWatcher.getEntityName("ReuseId");
    Set<RepoTypeIntegrityChecker> initial = new HashSet<>();
    initial.add(checker("properties", IntegrityCheckerType.MESSAGE_FORMAT));
    RepoType created = repoTypeService.createRepoType(name, null, "", initial);
    Long messageFormatId =
        findChecker(created, "properties", IntegrityCheckerType.MESSAGE_FORMAT).getId();

    Set<RepoTypeIntegrityChecker> withExtra = new HashSet<>();
    withExtra.add(checker("properties", IntegrityCheckerType.MESSAGE_FORMAT));
    withExtra.add(checker("properties", IntegrityCheckerType.TRAILING_WHITESPACE));
    repoTypeService.updateIntegrityCheckers(created, withExtra);

    RepoType afterAdd = repoTypeService.getRepoTypeById(created.getId());
    assertEquals(
        messageFormatId,
        findChecker(afterAdd, "properties", IntegrityCheckerType.MESSAGE_FORMAT).getId());
  }

  @Test
  public void testUpdateIntegrityCheckersAddsNewPair() throws Exception {
    String name = testIdWatcher.getEntityName("AddChecker");
    Set<RepoTypeIntegrityChecker> initial = new HashSet<>();
    initial.add(checker("properties", IntegrityCheckerType.MESSAGE_FORMAT));
    RepoType created = repoTypeService.createRepoType(name, null, "", initial);

    Set<RepoTypeIntegrityChecker> withExtra = new HashSet<>();
    withExtra.add(checker("properties", IntegrityCheckerType.MESSAGE_FORMAT));
    withExtra.add(checker("properties", IntegrityCheckerType.TRAILING_WHITESPACE));
    repoTypeService.updateIntegrityCheckers(created, withExtra);

    RepoType afterAdd = repoTypeService.getRepoTypeById(created.getId());
    assertEquals(2, afterAdd.getIntegrityCheckers().size());
    assertCheckerPresent(afterAdd, "properties", IntegrityCheckerType.TRAILING_WHITESPACE);
  }

  @Test
  public void testUpdateIntegrityCheckersRemovesMissingPair() throws Exception {
    String name = testIdWatcher.getEntityName("RemoveChecker");
    Set<RepoTypeIntegrityChecker> initial = new HashSet<>();
    initial.add(checker("properties", IntegrityCheckerType.MESSAGE_FORMAT));
    initial.add(checker("properties", IntegrityCheckerType.TRAILING_WHITESPACE));
    RepoType created = repoTypeService.createRepoType(name, null, "", initial);

    Set<RepoTypeIntegrityChecker> onlyTrailing = new HashSet<>();
    onlyTrailing.add(checker("properties", IntegrityCheckerType.TRAILING_WHITESPACE));
    repoTypeService.updateIntegrityCheckers(created, onlyTrailing);

    RepoType afterRemove = repoTypeService.getRepoTypeById(created.getId());
    assertEquals(1, afterRemove.getIntegrityCheckers().size());
    assertCheckerPresent(afterRemove, "properties", IntegrityCheckerType.TRAILING_WHITESPACE);
  }

  @Test
  public void testUpdateIntegrityCheckersNullClearsAll() throws Exception {
    String name = testIdWatcher.getEntityName("NullClearsCheckers");
    Set<RepoTypeIntegrityChecker> initial = new HashSet<>();
    initial.add(checker("properties", IntegrityCheckerType.MESSAGE_FORMAT));
    RepoType created = repoTypeService.createRepoType(name, null, "", initial);

    repoTypeService.updateIntegrityCheckers(created, null);

    RepoType afterNull = repoTypeService.getRepoTypeById(created.getId());
    assertTrue(afterNull.getIntegrityCheckers().isEmpty());
  }

  @Test
  public void testDeleteRepoTypeRemovesType() throws Exception {
    String name = testIdWatcher.getEntityName("ToDelete");
    RepoType created = repoTypeService.createRepoType(name, null, "prompt", null);
    Long id = created.getId();

    repoTypeService.deleteRepoType(id);

    assertNull(repoTypeRepository.findById(id).orElse(null));
  }

  @Test
  public void testDeleteRepoTypeRemovesCheckers() throws Exception {
    String name = testIdWatcher.getEntityName("DeleteCheckers");
    Set<RepoTypeIntegrityChecker> checkers = new HashSet<>();
    checkers.add(checker("xml", IntegrityCheckerType.HTML_TAG));
    RepoType created = repoTypeService.createRepoType(name, null, "prompt", checkers);

    repoTypeService.deleteRepoType(created.getId());

    assertTrue(repoTypeIntegrityCheckerRepository.findByRepoType(created).isEmpty());
  }

  @Test
  public void testDeleteRepoTypeMissingThrows() {
    try {
      repoTypeService.deleteRepoType(987654321L);
      fail("Expected RepoTypeWithIdNotFoundException");
    } catch (RepoTypeWithIdNotFoundException expected) {
      assertTrue(expected.getMessage().contains("987654321"));
    }
  }

  private static RepoTypeIntegrityChecker checker(
      String assetExtension, IntegrityCheckerType type) {
    RepoTypeIntegrityChecker checker = new RepoTypeIntegrityChecker();
    checker.setAssetExtension(assetExtension);
    checker.setIntegrityCheckerType(type);
    return checker;
  }

  private static void assertCheckerPresent(
      RepoType repoType, String assetExtension, IntegrityCheckerType type) {
    assertNotNull(findChecker(repoType, assetExtension, type));
  }

  private static RepoTypeIntegrityChecker findChecker(
      RepoType repoType, String assetExtension, IntegrityCheckerType type) {
    for (RepoTypeIntegrityChecker checker : repoType.getIntegrityCheckers()) {
      if (assetExtension.equals(checker.getAssetExtension())
          && type.equals(checker.getIntegrityCheckerType())) {
        return checker;
      }
    }
    fail("Checker not found: " + assetExtension + " / " + type);
    return null;
  }
}
