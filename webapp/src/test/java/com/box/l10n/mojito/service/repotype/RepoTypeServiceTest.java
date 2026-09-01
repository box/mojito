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
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Contract tests for {@link RepoTypeService}. Behavior under test is defined in JavaDoc and {@code
 * docs/internal/Architecture.md} (Repo Types).
 */
public class RepoTypeServiceTest extends ServiceTestBase {

  @Autowired RepoTypeService repoTypeService;

  @Autowired RepoTypeRepository repoTypeRepository;

  @Autowired JdbcTemplate jdbcTemplate;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Test
  public void testCreateRepoTypeAssignsIdAndName() throws Exception {
    String name = testIdWatcher.getEntityName("React");
    RepoType created = repoTypeService.createRepoType(name, null, null, null);

    assertNotNull(created.getId());
    assertEquals(name, created.getName());
  }

  @Test
  public void testCreateRepoTypeTrimsName() throws Exception {
    String name = testIdWatcher.getEntityName("React");
    RepoType created = repoTypeService.createRepoType("  " + name + "  ", null, null, null);

    assertEquals(name, created.getName());
  }

  @Test
  public void testCreateRepoTypePaddedNameConflictsWithTrimmed() throws Exception {
    String name = testIdWatcher.getEntityName("DupType");
    repoTypeService.createRepoType(name, null, "", null);
    try {
      repoTypeService.createRepoType(name + "   ", null, "", null);
      fail("Expected RepoTypeNameAlreadyUsedException");
    } catch (RepoTypeNameAlreadyUsedException expected) {
      assertEquals(name + " is used by another repo type", expected.getMessage());
    }
  }

  @Test
  public void testCreateRepoTypeSameLettersDifferentCaseAreDistinct() throws Exception {
    String name = testIdWatcher.getEntityName("React");
    String otherCase = name.toLowerCase(Locale.ROOT);
    repoTypeService.createRepoType(name, null, "", null);
    RepoType other = repoTypeService.createRepoType(otherCase, null, "", null);

    assertEquals(otherCase, other.getName());
    assertEquals(1, repoTypeService.getRepoTypes(name).size());
    assertEquals(1, repoTypeService.getRepoTypes(otherCase).size());
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
    RepoType created = repoTypeService.createRepoType(name, "Android strings.xml", "", null);

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
  public void testCreateRepoTypePersistsAiPromptLongerThanNameMax() throws Exception {
    String name = testIdWatcher.getEntityName("LongPrompt");
    // No application max on aiPrompt (unlike name/description at 255). This size only proves we do
    // not reuse NAME_MAX_LENGTH and that dropping @Lob still persists long text on HSQL.
    String prompt = "p".repeat(10_000);

    RepoType created = repoTypeService.createRepoType(name, null, prompt, null);

    assertEquals(10_000, created.getAiPrompt().length());
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
  public void testCreateRepoTypeDedupesIdenticalCheckerPairs() throws Exception {
    String name = testIdWatcher.getEntityName("DupCheckers");
    Set<RepoTypeIntegrityChecker> checkers = Collections.newSetFromMap(new IdentityHashMap<>());
    checkers.add(checker("json", IntegrityCheckerType.SIMPLE_PRINTF_LIKE));
    checkers.add(checker("json", IntegrityCheckerType.SIMPLE_PRINTF_LIKE));
    assertEquals(
        "identity-based Set can hold two instances of the same pair before service de-dupe",
        2,
        checkers.size());

    RepoType created = repoTypeService.createRepoType(name, null, "", checkers);

    assertEquals(1, created.getIntegrityCheckers().size());
    assertCheckerPresent(created, "json", IntegrityCheckerType.SIMPLE_PRINTF_LIKE);
  }

  @Test
  public void testCreateRepoTypeNullCheckerThrows() throws Exception {
    String name = testIdWatcher.getEntityName("NullChecker");
    Set<RepoTypeIntegrityChecker> checkers = new HashSet<>();
    checkers.add(null);
    try {
      repoTypeService.createRepoType(name, null, "", checkers);
      fail("Expected RepoTypeInvalidException");
    } catch (RepoTypeInvalidException expected) {
      assertEquals("integrity checker must not be null", expected.getMessage());
    }
    // Parent is saveAndFlush'd before checker validation; the transaction must still roll back.
    assertNull(repoTypeRepository.findByName(name));
  }

  @Test
  public void testCreateRepoTypeNullCheckerTypeThrows() throws Exception {
    String name = testIdWatcher.getEntityName("NullCheckerType");
    Set<RepoTypeIntegrityChecker> checkers = new HashSet<>();
    checkers.add(checker("properties", null));
    try {
      repoTypeService.createRepoType(name, null, "", checkers);
      fail("Expected RepoTypeInvalidException");
    } catch (RepoTypeInvalidException expected) {
      assertEquals("integrityCheckerType is required", expected.getMessage());
    }
  }

  @Test
  public void testCreateRepoTypeNullAssetExtensionThrows() throws Exception {
    String name = testIdWatcher.getEntityName("NullExtension");
    Set<RepoTypeIntegrityChecker> checkers = new HashSet<>();
    checkers.add(checker(null, IntegrityCheckerType.MESSAGE_FORMAT));
    try {
      repoTypeService.createRepoType(name, null, "", checkers);
      fail("Expected RepoTypeInvalidException");
    } catch (RepoTypeInvalidException expected) {
      assertEquals("assetExtension is required", expected.getMessage());
    }
  }

  @Test
  public void testCreateRepoTypeBlankAssetExtensionThrows() throws Exception {
    String name = testIdWatcher.getEntityName("BlankExtension");
    Set<RepoTypeIntegrityChecker> checkers = new HashSet<>();
    checkers.add(checker("   ", IntegrityCheckerType.MESSAGE_FORMAT));
    try {
      repoTypeService.createRepoType(name, null, "", checkers);
      fail("Expected RepoTypeInvalidException");
    } catch (RepoTypeInvalidException expected) {
      assertEquals("assetExtension is required", expected.getMessage());
    }
  }

  @Test
  public void testCreateRepoTypeNormalizesAssetExtension() throws Exception {
    String name = testIdWatcher.getEntityName("NormExt");
    Set<RepoTypeIntegrityChecker> checkers = Collections.newSetFromMap(new IdentityHashMap<>());
    checkers.add(checker("json ", IntegrityCheckerType.SIMPLE_PRINTF_LIKE));
    checkers.add(checker(".json", IntegrityCheckerType.SIMPLE_PRINTF_LIKE));
    checkers.add(checker("json", IntegrityCheckerType.SIMPLE_PRINTF_LIKE));

    RepoType created = repoTypeService.createRepoType(name, null, "", checkers);

    assertEquals(1, created.getIntegrityCheckers().size());
    assertCheckerPresent(created, "json", IntegrityCheckerType.SIMPLE_PRINTF_LIKE);
  }

  /**
   * Pins the schema unique key, not Java de-dupe. Distinct pairs are written through {@link
   * RepoTypeService}. The colliding pair is inserted on the join table so service collapse cannot
   * keep this green. Same uniqueness is V69's primary key on MySQL and {@code
   * UK__REPO_TYPE_INTEGRITY_CHECKER} on Hibernate/HSQL — one test, no DB skip.
   */
  @Test
  public void testDuplicateCheckerPairRejectedByDatabaseConstraint() throws Exception {
    String name = testIdWatcher.getEntityName("DbUniqueCheckers");
    Set<RepoTypeIntegrityChecker> checkers = new HashSet<>();
    checkers.add(checker("json", IntegrityCheckerType.SIMPLE_PRINTF_LIKE));
    checkers.add(checker("json", IntegrityCheckerType.MESSAGE_FORMAT));
    checkers.add(checker("properties", IntegrityCheckerType.SIMPLE_PRINTF_LIKE));
    RepoType created = repoTypeService.createRepoType(name, null, "", checkers);
    Long id = created.getId();

    assertEquals(3, created.getIntegrityCheckers().size());
    assertEquals(3, repoTypeService.getRepoTypeById(id).getIntegrityCheckers().size());

    try {
      insertDuplicateCheckerRow(id, "json", IntegrityCheckerType.SIMPLE_PRINTF_LIKE.name());
      fail("Expected DataIntegrityViolationException");
    } catch (DataIntegrityViolationException expected) {
      // schema unique key; the service would have de-duped this pair before persist
    }
    assertEquals(3, repoTypeService.getRepoTypeById(id).getIntegrityCheckers().size());
  }

  @Test
  public void testUpdateIntegrityCheckersDedupesIdenticalCheckerPairs() throws Exception {
    String name = testIdWatcher.getEntityName("DupCheckersPatch");
    RepoType created = repoTypeService.createRepoType(name, null, "", null);

    Set<RepoTypeIntegrityChecker> checkers = Collections.newSetFromMap(new IdentityHashMap<>());
    checkers.add(checker("json", IntegrityCheckerType.SIMPLE_PRINTF_LIKE));
    checkers.add(checker("json", IntegrityCheckerType.SIMPLE_PRINTF_LIKE));
    repoTypeService.updateIntegrityCheckers(created, checkers);

    RepoType afterUpdate = repoTypeService.getRepoTypeById(created.getId());
    assertEquals(1, afterUpdate.getIntegrityCheckers().size());
    assertCheckerPresent(afterUpdate, "json", IntegrityCheckerType.SIMPLE_PRINTF_LIKE);
  }

  @Test
  public void testCreateRepoTypeDuplicateNameThrows() throws Exception {
    String name = testIdWatcher.getEntityName("Duplicate");
    repoTypeService.createRepoType(name, null, "", null);

    try {
      repoTypeService.createRepoType(name, "again", "", null);
      fail("Expected RepoTypeNameAlreadyUsedException");
    } catch (RepoTypeNameAlreadyUsedException expected) {
      assertEquals(name + " is used by another repo type", expected.getMessage());
    }
  }

  @Test
  public void testCreateRepoTypeNullNameThrows() throws Exception {
    try {
      repoTypeService.createRepoType(null, null, "", null);
      fail("Expected RepoTypeInvalidException");
    } catch (RepoTypeInvalidException expected) {
      assertEquals("name is required", expected.getMessage());
    }
  }

  @Test
  public void testCreateRepoTypeBlankNameThrows() throws Exception {
    try {
      repoTypeService.createRepoType("   ", null, "", null);
      fail("Expected RepoTypeInvalidException");
    } catch (RepoTypeInvalidException expected) {
      assertEquals("name is required", expected.getMessage());
    }
  }

  @Test
  public void testCreateRepoTypeNameTooLongThrows() throws Exception {
    String tooLong = "n".repeat(RepoType.NAME_MAX_LENGTH + 1);
    try {
      repoTypeService.createRepoType(tooLong, null, "", null);
      fail("Expected RepoTypeInvalidException");
    } catch (RepoTypeInvalidException expected) {
      assertEquals(
          "name must be at most " + RepoType.NAME_MAX_LENGTH + " characters",
          expected.getMessage());
    }
  }

  @Test
  public void testCreateRepoTypeDescriptionTooLongThrows() throws Exception {
    String name = testIdWatcher.getEntityName("LongDesc");
    String tooLong = "d".repeat(RepoType.DESCRIPTION_MAX_LENGTH + 1);
    try {
      repoTypeService.createRepoType(name, tooLong, "", null);
      fail("Expected RepoTypeInvalidException");
    } catch (RepoTypeInvalidException expected) {
      assertEquals(
          "description must be at most " + RepoType.DESCRIPTION_MAX_LENGTH + " characters",
          expected.getMessage());
    }
  }

  @Test
  public void testUpdateRepoTypeBlankNameThrows() throws Exception {
    String name = testIdWatcher.getEntityName("KeepName");
    RepoType created = repoTypeService.createRepoType(name, null, "", null);

    try {
      repoTypeService.updateRepoType(created.getId(), "  ", null, null, null);
      fail("Expected RepoTypeInvalidException");
    } catch (RepoTypeInvalidException expected) {
      assertEquals("name is required", expected.getMessage());
    }
    assertEquals(name, repoTypeService.getRepoTypeById(created.getId()).getName());
  }

  @Test
  public void testUpdateRepoTypeNameTooLongThrowsLeavesNameUnchanged() throws Exception {
    String name = testIdWatcher.getEntityName("KeepName");
    RepoType created = repoTypeService.createRepoType(name, "desc", "", null);
    String tooLong = "n".repeat(RepoType.NAME_MAX_LENGTH + 1);

    try {
      repoTypeService.updateRepoType(created.getId(), tooLong, null, null, null);
      fail("Expected RepoTypeInvalidException");
    } catch (RepoTypeInvalidException expected) {
      assertEquals(
          "name must be at most " + RepoType.NAME_MAX_LENGTH + " characters",
          expected.getMessage());
    }
    RepoType stored = repoTypeService.getRepoTypeById(created.getId());
    assertEquals(name, stored.getName());
    assertEquals("desc", stored.getDescription());
  }

  @Test
  public void testUpdateRepoTypeDescriptionTooLongThrowsLeavesDescriptionUnchanged()
      throws Exception {
    String name = testIdWatcher.getEntityName("KeepDesc");
    RepoType created = repoTypeService.createRepoType(name, "original", "", null);
    String tooLong = "d".repeat(RepoType.DESCRIPTION_MAX_LENGTH + 1);

    try {
      repoTypeService.updateRepoType(created.getId(), null, tooLong, null, null);
      fail("Expected RepoTypeInvalidException");
    } catch (RepoTypeInvalidException expected) {
      assertEquals(
          "description must be at most " + RepoType.DESCRIPTION_MAX_LENGTH + " characters",
          expected.getMessage());
    }
    RepoType stored = repoTypeService.getRepoTypeById(created.getId());
    assertEquals(name, stored.getName());
    assertEquals("original", stored.getDescription());
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
      assertEquals("RepoType with id: 987654321 not found", expected.getMessage());
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
  public void testGetRepoTypesByNameTrimsFilter() throws Exception {
    String name = testIdWatcher.getEntityName("Exact");
    repoTypeService.createRepoType(name, null, "prompt", null);

    List<RepoType> found = repoTypeService.getRepoTypes(name + " ");

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

    RepoType updated = repoTypeService.updateRepoType(created.getId(), null, null, null, null);

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

    RepoType updated = repoTypeService.updateRepoType(created.getId(), null, null, "", null);

    assertEquals("", updated.getAiPrompt());
  }

  @Test
  public void testUpdateRepoTypePersistsAiPromptLongerThanNameMax() throws Exception {
    String name = testIdWatcher.getEntityName("PatchLongPrompt");
    RepoType created = repoTypeService.createRepoType(name, null, "short", null);
    String prompt = "p".repeat(10_000);

    RepoType updated = repoTypeService.updateRepoType(created.getId(), null, null, prompt, null);

    assertEquals(10_000, updated.getAiPrompt().length());
    assertEquals(prompt, updated.getAiPrompt());
  }

  @Test
  public void testUpdateRepoTypeNullIntegrityCheckersLeaveUnchanged() throws Exception {
    String name = testIdWatcher.getEntityName("LeaveCheckers");
    Set<RepoTypeIntegrityChecker> checkers = new HashSet<>();
    checkers.add(checker("properties", IntegrityCheckerType.MESSAGE_FORMAT));
    RepoType created = repoTypeService.createRepoType(name, null, "", checkers);

    RepoType leftAlone = repoTypeService.updateRepoType(created.getId(), null, null, null, null);

    assertEquals(1, leftAlone.getIntegrityCheckers().size());
  }

  @Test
  public void testUpdateRepoTypeEmptyIntegrityCheckersClearsAll() throws Exception {
    String name = testIdWatcher.getEntityName("ClearCheckers");
    Set<RepoTypeIntegrityChecker> checkers = new HashSet<>();
    checkers.add(checker("properties", IntegrityCheckerType.MESSAGE_FORMAT));
    RepoType created = repoTypeService.createRepoType(name, null, "", checkers);

    RepoType cleared =
        repoTypeService.updateRepoType(created.getId(), null, null, null, new HashSet<>());

    assertTrue(cleared.getIntegrityCheckers().isEmpty());
  }

  @Test
  public void testUpdateRepoTypeRenameToSameNameAllowed() throws Exception {
    String name = testIdWatcher.getEntityName("SameName");
    RepoType type = repoTypeService.createRepoType(name, null, "", null);

    RepoType updated = repoTypeService.updateRepoType(type.getId(), name, "desc", null, null);

    assertEquals(name, updated.getName());
    assertEquals("desc", updated.getDescription());
  }

  @Test
  public void testUpdateRepoTypeTrimsName() throws Exception {
    String name = testIdWatcher.getEntityName("TrimPatch");
    RepoType created = repoTypeService.createRepoType(name, null, "", null);

    RepoType updated =
        repoTypeService.updateRepoType(created.getId(), name + "  ", null, null, null);

    assertEquals(name, updated.getName());
  }

  @Test
  public void testUpdateRepoTypeRenamesAndLookupFollowsNewName() throws Exception {
    String original = testIdWatcher.getEntityName("React");
    String renamed = testIdWatcher.getEntityName("React-ICU");
    RepoType created = repoTypeService.createRepoType(original, null, "", null);

    RepoType updated = repoTypeService.updateRepoType(created.getId(), renamed, null, null, null);

    assertEquals(renamed, updated.getName());
    assertEquals(renamed, repoTypeService.getRepoTypeById(created.getId()).getName());
    assertTrue(repoTypeService.getRepoTypes(original).isEmpty());
    List<RepoType> byNewName = repoTypeService.getRepoTypes(renamed);
    assertEquals(1, byNewName.size());
    assertEquals(created.getId(), byNewName.get(0).getId());
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
      assertEquals(nameB + " is used by another repo type", expected.getMessage());
    }
  }

  @Test
  public void testUpdateRepoTypeInvalidCheckerLeavesNameUnchanged() throws Exception {
    String original = testIdWatcher.getEntityName("Keep");
    String renamed = testIdWatcher.getEntityName("Renamed");
    RepoType created = repoTypeService.createRepoType(original, null, "", null);

    Set<RepoTypeIntegrityChecker> checkers = new HashSet<>();
    checkers.add(checker("properties", null));
    try {
      repoTypeService.updateRepoType(created.getId(), renamed, null, null, checkers);
      fail("Expected RepoTypeInvalidException");
    } catch (RepoTypeInvalidException expected) {
      assertEquals("integrityCheckerType is required", expected.getMessage());
    }
    assertEquals(original, repoTypeService.getRepoTypeById(created.getId()).getName());
    assertNull(repoTypeRepository.findByName(renamed));
  }

  @Test
  public void testUpdateRepoTypeMissingIdThrows() throws Exception {
    try {
      repoTypeService.updateRepoType(987654321L, "x", null, null, null);
      fail("Expected RepoTypeWithIdNotFoundException");
    } catch (RepoTypeWithIdNotFoundException expected) {
      assertEquals("RepoType with id: 987654321 not found", expected.getMessage());
    }
  }

  @Test
  public void testUpdateRepoTypePromptBumpsLastModifiedDate() throws Exception {
    String name = testIdWatcher.getEntityName("PromptTimestamp");
    RepoType created = repoTypeService.createRepoType(name, null, "before", null);
    ZonedDateTime originalModified = created.getLastModifiedDate();
    assertNotNull(originalModified);

    Thread.sleep(20);

    RepoType updated = repoTypeService.updateRepoType(created.getId(), null, null, "after", null);

    assertEquals("after", updated.getAiPrompt());
    assertNotNull(updated.getLastModifiedDate());
    assertTrue(updated.getLastModifiedDate().isAfter(originalModified));
    assertCreatedDateUnchanged(created, updated);
  }

  @Test
  public void testUpdateRepoTypeCheckersOnlyBumpsLastModifiedDate() throws Exception {
    String name = testIdWatcher.getEntityName("CheckerTimestamp");
    RepoType created = repoTypeService.createRepoType(name, null, "", null);
    ZonedDateTime originalModified = created.getLastModifiedDate();
    assertNotNull(originalModified);

    Thread.sleep(20);

    Set<RepoTypeIntegrityChecker> checkers = new HashSet<>();
    checkers.add(checker("properties", IntegrityCheckerType.MESSAGE_FORMAT));
    RepoType updated = repoTypeService.updateRepoType(created.getId(), null, null, null, checkers);

    assertNotNull(updated.getLastModifiedDate());
    assertTrue(updated.getLastModifiedDate().isAfter(originalModified));
    assertCreatedDateUnchanged(created, updated);
    assertEquals(1, updated.getIntegrityCheckers().size());
  }

  @Test
  public void testCopyingCheckersToAnotherTypeDoesNotRemoveThemFromOriginal() throws Exception {
    Set<RepoTypeIntegrityChecker> typeACheckers = new HashSet<>();
    typeACheckers.add(checker("json", IntegrityCheckerType.SIMPLE_PRINTF_LIKE));
    RepoType typeA =
        repoTypeService.createRepoType(
            testIdWatcher.getEntityName("TypeA"), null, "", typeACheckers);
    RepoType typeB =
        repoTypeService.createRepoType(testIdWatcher.getEntityName("TypeB"), null, "", null);

    Set<RepoTypeIntegrityChecker> typeBCheckers = new HashSet<>();
    typeBCheckers.add(checker("json", IntegrityCheckerType.SIMPLE_PRINTF_LIKE));
    repoTypeService.updateIntegrityCheckers(typeB, typeBCheckers);

    RepoType typeAAfter = repoTypeService.getRepoTypeById(typeA.getId());
    RepoType typeBAfter = repoTypeService.getRepoTypeById(typeB.getId());
    assertEquals(1, typeAAfter.getIntegrityCheckers().size());
    assertCheckerPresent(typeAAfter, "json", IntegrityCheckerType.SIMPLE_PRINTF_LIKE);
    assertEquals(1, typeBAfter.getIntegrityCheckers().size());
    assertCheckerPresent(typeBAfter, "json", IntegrityCheckerType.SIMPLE_PRINTF_LIKE);
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
  public void testUpdateRepoTypeAfterDeleteThrowsAndDoesNotResurrect() throws Exception {
    String name = testIdWatcher.getEntityName("GoneForPatch");
    RepoType created = repoTypeService.createRepoType(name, "before", "", null);
    Long id = created.getId();

    repoTypeService.deleteRepoType(id);

    try {
      repoTypeService.updateRepoType(id, null, "after", null, null);
      fail("Expected RepoTypeWithIdNotFoundException");
    } catch (RepoTypeWithIdNotFoundException expected) {
      assertEquals("RepoType with id: " + id + " not found", expected.getMessage());
    }

    assertNull(repoTypeRepository.findById(id).orElse(null));
    assertTrue(repoTypeService.getRepoTypes(name).isEmpty());
  }

  @Test
  public void testUpdateIntegrityCheckersAfterDeleteThrowsAndDoesNotResurrect() throws Exception {
    String name = testIdWatcher.getEntityName("GoneForCheckers");
    RepoType created = repoTypeService.createRepoType(name, null, "", null);
    Long id = created.getId();

    repoTypeService.deleteRepoType(id);

    Set<RepoTypeIntegrityChecker> checkers = new HashSet<>();
    checkers.add(checker("properties", IntegrityCheckerType.MESSAGE_FORMAT));
    try {
      repoTypeService.updateIntegrityCheckers(created, checkers);
      fail("Expected RepoTypeWithIdNotFoundException");
    } catch (RepoTypeWithIdNotFoundException expected) {
      assertEquals("RepoType with id: " + id + " not found", expected.getMessage());
    }

    assertNull(repoTypeRepository.findById(id).orElse(null));
    assertTrue(repoTypeService.getRepoTypes(name).isEmpty());
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

    assertNull(repoTypeRepository.findById(created.getId()).orElse(null));
    Number checkerRows =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM repo_type_integrity_checker WHERE repo_type_id = ?",
            Number.class,
            created.getId());
    assertEquals(0, checkerRows.intValue());
  }

  @Test
  public void testDeleteRepoTypeMissingThrows() {
    try {
      repoTypeService.deleteRepoType(987654321L);
      fail("Expected RepoTypeWithIdNotFoundException");
    } catch (RepoTypeWithIdNotFoundException expected) {
      assertEquals("RepoType with id: 987654321 not found", expected.getMessage());
    }
  }

  /**
   * {@code createdDate} is the same logical instant, but the in-memory auditing value can have
   * nanosecond precision while the value read back from the database is rounded or truncated to the
   * column's precision. A 1ms window covers that without depending on exact truncation.
   */
  private static void assertCreatedDateUnchanged(RepoType before, RepoType after) {
    assertNotNull(before.getCreatedDate());
    assertNotNull(after.getCreatedDate());
    Duration delta =
        Duration.between(before.getCreatedDate().toInstant(), after.getCreatedDate().toInstant())
            .abs();
    assertTrue(
        "createdDate drifted by " + delta + ", expected only storage-precision differences",
        delta.compareTo(Duration.ofMillis(1)) < 0);
  }

  /**
   * SQL on purpose — do not persist a {@link RepoType} / {@link RepoTypeIntegrityChecker} here.
   * Service de-dupe and {@code equals}/{@code hashCode} on the embeddable collapse a duplicate pair
   * in memory, so Spring would never insert a second row and this would not exercise the unique
   * key.
   */
  private void insertDuplicateCheckerRow(
      Long repoTypeId, String assetExtension, String checkerType) {
    jdbcTemplate.update(
        "INSERT INTO repo_type_integrity_checker"
            + " (repo_type_id, asset_extension, integrity_checker_type) VALUES (?, ?, ?)",
        repoTypeId,
        assetExtension,
        checkerType);
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
