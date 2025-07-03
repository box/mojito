package com.box.l10n.mojito.service.tm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.Before;
import org.junit.Test;

public class TextUnitBatchMatcherTest {

  static final String PLURAL_SEPARATOR = "_";

  TextUnitBatchMatcher textUnitBatchMatcher;

  @Before
  public void before() {
    textUnitBatchMatcher = new TextUnitBatchMatcher();
    textUnitBatchMatcher.pluralNameParser = new PluralNameParser();
  }

  @Test
  public void testNotAlreadyMatched() {

    Predicate<TextUnitDTO> notAlreadyMatched = textUnitBatchMatcher.notAlreadyMatched("test");

    TextUnitDTO textUnitDTO = new TextUnitDTO();
    textUnitDTO.setTmTextUnitId(1000L);

    assertTrue(notAlreadyMatched.test(textUnitDTO));
    assertFalse(notAlreadyMatched.test(textUnitDTO));
  }

  @Test
  public void testMatch() {

    List<TextUnitDTO> existingTextUnitDTOs =
        Arrays.asList(
            createTextUnitDTO("name-0"),
            createTextUnitDTO("name-1"),
            createTextUnitDTO("will map by tmTextUnit id", 3L),
            createUnusedTextUnitDTO("name-4"));

    Function<TextUnitForBatchMatcher, Optional<TextUnitDTO>> matchByPluralPrefix =
        textUnitBatchMatcher.match(existingTextUnitDTOs);

    TextUnitDTO name0 = matchByPluralPrefix.apply(createTextUnitForBatchMatcher("name-0")).get();
    assertEquals("name-0", name0.getName());

    Optional<TextUnitDTO> name0SecondTime =
        matchByPluralPrefix.apply(createTextUnitForBatchMatcher("name-0"));
    assertFalse(name0SecondTime.isPresent());

    TextUnitDTO name1 = matchByPluralPrefix.apply(createTextUnitForBatchMatcher("name-1")).get();
    assertEquals("name-1", name1.getName());

    TextUnitDTO name3 =
        matchByPluralPrefix
            .apply(createTextUnitForBatchMatcher("will map tmTextUnit id (diff)", 3L))
            .get();
    assertEquals("will map by tmTextUnit id", name3.getName());

    TextUnitDTO name4 = matchByPluralPrefix.apply(createTextUnitForBatchMatcher("name-4")).get();
    assertEquals("name-4", name4.getName());
  }

  @Test
  public void testMatchByPluralPrefixNoPlural() {
    List<TextUnitDTO> existingTextUnitDTOs =
        Arrays.asList(createTextUnitDTO("name-0"), createTextUnitDTO("name-1"));

    Function<TextUnitForBatchMatcher, Optional<List<TextUnitDTO>>> matchByPluralPrefix =
        textUnitBatchMatcher.createMatchByPluralPrefixCommentAndUsed(
            existingTextUnitDTOs, PLURAL_SEPARATOR);

    Optional<List<TextUnitDTO>> result =
        matchByPluralPrefix.apply(createPluralTextUnitForBatchMatcher("name-0"));
    assertFalse(result.isPresent());
  }

  @Test
  public void testCreateMatchByPluralPrefixCommentAndUsed_DoesNotMatchNoPlural() {
    List<TextUnitDTO> existingTextUnitDTOs =
        Arrays.asList(
            createTextUnitDTO("name-0", "comment-0", "First string"),
            createTextUnitDTO("name-1", "comment-1", "Second string"));

    Function<TextUnitForBatchMatcher, Optional<List<TextUnitDTO>>> matchByPluralPrefixAndComment =
        textUnitBatchMatcher.createMatchByPluralPrefixCommentAndUsed(
            existingTextUnitDTOs, PLURAL_SEPARATOR);

    Optional<List<TextUnitDTO>> result =
        matchByPluralPrefixAndComment.apply(
            createPluralTextUnitForBatchMatcher("name-0", "comment-0"));
    assertTrue(result.isEmpty());
  }

  @Test
  public void testMatchByPluralPrefix() {
    List<TextUnitDTO> existingTextUnitDTOs =
        Arrays.asList(
            createTextUnitDTO("name-0"),
            createTextUnitDTO("name-1"),
            createPluralTextUnitDTO("name-2", "other"),
            createPluralTextUnitDTO("name-2", "one"),
            createPluralTextUnitDTO("name-3", "zero"),
            createPluralTextUnitDTO("name-3", "one"),
            createPluralTextUnitDTO("name-3", "two"),
            createPluralTextUnitDTO("name-3", "few"),
            createPluralTextUnitDTO("name-3", "many"),
            createPluralTextUnitDTO("name-3", "other"));

    Function<TextUnitForBatchMatcher, Optional<List<TextUnitDTO>>> matchByPluralPrefix =
        textUnitBatchMatcher.createMatchByPluralPrefixCommentAndUsed(
            existingTextUnitDTOs, PLURAL_SEPARATOR);

    List<TextUnitDTO> name2 =
        matchByPluralPrefix.apply(createPluralTextUnitForBatchMatcher("name-2")).get();
    assertEquals(2, name2.size());
    assertEquals("name-2_other", name2.get(0).getName());
    assertEquals("name-2_one", name2.get(1).getName());

    List<TextUnitDTO> name3 =
        matchByPluralPrefix.apply(createPluralTextUnitForBatchMatcher("name-3")).get();
    assertEquals(6, name3.size());
    assertEquals("name-3_zero", name3.get(0).getName());
    assertEquals("name-3_one", name3.get(1).getName());
    assertEquals("name-3_two", name3.get(2).getName());
    assertEquals("name-3_few", name3.get(3).getName());
    assertEquals("name-3_many", name3.get(4).getName());
    assertEquals("name-3_other", name3.get(5).getName());

    Optional<List<TextUnitDTO>> name3SecondTime =
        matchByPluralPrefix.apply(createPluralTextUnitForBatchMatcher("name-3"));
    assertFalse(name3SecondTime.isPresent());
  }

  @Test
  public void testCreateMatchByPluralPrefixCommentAndUsed() {
    List<TextUnitDTO> existingTextUnitDTOs =
        Arrays.asList(
            createTextUnitDTO("name-0"),
            createTextUnitDTO("name-1"),
            createPluralTextUnitDTO("name-2", "other", "comment-2"),
            createPluralTextUnitDTO("name-2", "one", "comment-2"),
            createPluralTextUnitDTO("name-3", "zero", "comment-3"),
            createPluralTextUnitDTO("name-3", "one", "comment-3"),
            createPluralTextUnitDTO("name-3", "two", "comment-3"),
            createPluralTextUnitDTO("name-3", "few", "comment-3"),
            createPluralTextUnitDTO("name-3", "many", "comment-3"),
            createPluralTextUnitDTO("name-3", "other", "comment-3"));

    Function<TextUnitForBatchMatcher, Optional<List<TextUnitDTO>>> matchByPluralPrefixAndComment =
        textUnitBatchMatcher.createMatchByPluralPrefixCommentAndUsed(
            existingTextUnitDTOs, PLURAL_SEPARATOR);

    List<TextUnitDTO> name2 =
        matchByPluralPrefixAndComment
            .apply(createPluralTextUnitForBatchMatcher("name-2", "comment-2"))
            .get();
    assertEquals(2, name2.size());
    assertEquals("name-2_other", name2.get(0).getName());
    assertEquals("name-2_one", name2.get(1).getName());

    List<TextUnitDTO> name3 =
        matchByPluralPrefixAndComment
            .apply(createPluralTextUnitForBatchMatcher("name-3", "comment-3"))
            .get();
    assertEquals(6, name3.size());
    assertEquals("name-3_zero", name3.get(0).getName());
    assertEquals("name-3_one", name3.get(1).getName());
    assertEquals("name-3_two", name3.get(2).getName());
    assertEquals("name-3_few", name3.get(3).getName());
    assertEquals("name-3_many", name3.get(4).getName());
    assertEquals("name-3_other", name3.get(5).getName());

    Optional<List<TextUnitDTO>> name3SecondTime =
        matchByPluralPrefixAndComment.apply(
            createPluralTextUnitForBatchMatcher("name-3", "comment-3"));
    assertFalse(name3SecondTime.isPresent());
  }

  @Test
  public void testCreateMatchByPluralPrefixCommentAndUsed_DoesNotMatch() {
    List<TextUnitDTO> existingTextUnitDTOs =
        Arrays.asList(
            createTextUnitDTO("name-0"),
            createTextUnitDTO("name-1"),
            createPluralTextUnitDTO("name-2", "other", "comment-2"),
            createPluralTextUnitDTO("name-2", "one", "comment-2"),
            createPluralTextUnitDTO("name-3", "zero", "comment-3"),
            createPluralTextUnitDTO("name-3", "one", "comment-3"),
            createPluralTextUnitDTO("name-3", "two", "comment-3"),
            createPluralTextUnitDTO("name-3", "few", "comment-3"),
            createPluralTextUnitDTO("name-3", "many", "comment-3"),
            createPluralTextUnitDTO("name-3", "other", "comment-3"));

    Function<TextUnitForBatchMatcher, Optional<List<TextUnitDTO>>> matchByPluralPrefixAndComment =
        textUnitBatchMatcher.createMatchByPluralPrefixCommentAndUsed(
            existingTextUnitDTOs, PLURAL_SEPARATOR);

    Optional<List<TextUnitDTO>> name2 =
        matchByPluralPrefixAndComment.apply(
            createPluralTextUnitForBatchMatcher("name-2", "comment-4"));
    assertTrue(name2.isEmpty());

    Optional<List<TextUnitDTO>> name3 =
        matchByPluralPrefixAndComment.apply(
            createPluralTextUnitForBatchMatcher("name-3", "comment-5"));
    assertTrue(name3.isEmpty());
  }

  @Test
  public void testMatchByPluralPrefixUnused() {
    List<TextUnitDTO> existingTextUnitDTOs =
        Arrays.asList(
            createTextUnitDTO("name-0"),
            createTextUnitDTO("name-1"),
            createUnusedPluralTextUnitDTO("name-2", "other"),
            createUnusedPluralTextUnitDTO("name-2", "one"),
            createUnusedPluralTextUnitDTO("name-3", "zero"),
            createUnusedPluralTextUnitDTO("name-3", "one"),
            createUnusedPluralTextUnitDTO("name-3", "two"),
            createUnusedPluralTextUnitDTO("name-3", "few"),
            createUnusedPluralTextUnitDTO("name-3", "many"),
            createUnusedPluralTextUnitDTO("name-3", "other"));

    Function<TextUnitForBatchMatcher, Optional<List<TextUnitDTO>>> matchByPluralPrefix =
        textUnitBatchMatcher.createMatchByPluralPrefixCommentAndUnused(
            existingTextUnitDTOs, PLURAL_SEPARATOR);

    List<TextUnitDTO> name2 =
        matchByPluralPrefix.apply(createPluralTextUnitForBatchMatcher("name-2")).get();
    assertEquals(2, name2.size());
    assertEquals("name-2_other", name2.get(0).getName());
    assertEquals("name-2_one", name2.get(1).getName());

    List<TextUnitDTO> name3 =
        matchByPluralPrefix.apply(createPluralTextUnitForBatchMatcher("name-3")).get();
    assertEquals(6, name3.size());
    assertEquals("name-3_zero", name3.get(0).getName());
    assertEquals("name-3_one", name3.get(1).getName());
    assertEquals("name-3_two", name3.get(2).getName());
    assertEquals("name-3_few", name3.get(3).getName());
    assertEquals("name-3_many", name3.get(4).getName());
    assertEquals("name-3_other", name3.get(5).getName());

    Optional<List<TextUnitDTO>> name3SecondTime =
        matchByPluralPrefix.apply(createPluralTextUnitForBatchMatcher("name-3"));
    assertFalse(name3SecondTime.isPresent());
  }

  @Test
  public void testCreateMatchByPluralPrefixCommentAndUnused() {
    List<TextUnitDTO> existingTextUnitDTOs =
        Arrays.asList(
            createTextUnitDTO("name-0"),
            createTextUnitDTO("name-1"),
            createUnusedPluralTextUnitDTO("name-2", "other", "comment-2"),
            createUnusedPluralTextUnitDTO("name-2", "one", "comment-2"),
            createUnusedPluralTextUnitDTO("name-3", "zero", "comment-3"),
            createUnusedPluralTextUnitDTO("name-3", "one", "comment-3"),
            createUnusedPluralTextUnitDTO("name-3", "two", "comment-3"),
            createUnusedPluralTextUnitDTO("name-3", "few", "comment-3"),
            createUnusedPluralTextUnitDTO("name-3", "many", "comment-3"),
            createUnusedPluralTextUnitDTO("name-3", "other", "comment-3"));

    Function<TextUnitForBatchMatcher, Optional<List<TextUnitDTO>>> matchByPluralPrefixAndComment =
        textUnitBatchMatcher.createMatchByPluralPrefixCommentAndUnused(
            existingTextUnitDTOs, PLURAL_SEPARATOR);

    List<TextUnitDTO> name2 =
        matchByPluralPrefixAndComment
            .apply(createPluralTextUnitForBatchMatcher("name-2", "comment-2"))
            .get();
    assertEquals(2, name2.size());
    assertEquals("name-2_other", name2.get(0).getName());
    assertEquals("name-2_one", name2.get(1).getName());

    List<TextUnitDTO> name3 =
        matchByPluralPrefixAndComment
            .apply(createPluralTextUnitForBatchMatcher("name-3", "comment-3"))
            .get();
    assertEquals(6, name3.size());
    assertEquals("name-3_zero", name3.get(0).getName());
    assertEquals("name-3_one", name3.get(1).getName());
    assertEquals("name-3_two", name3.get(2).getName());
    assertEquals("name-3_few", name3.get(3).getName());
    assertEquals("name-3_many", name3.get(4).getName());
    assertEquals("name-3_other", name3.get(5).getName());

    Optional<List<TextUnitDTO>> name3SecondTime =
        matchByPluralPrefixAndComment.apply(
            createPluralTextUnitForBatchMatcher("name-3", "comment-3"));
    assertFalse(name3SecondTime.isPresent());
  }

  @Test
  public void testCreateMatchByPluralPrefixCommentAndUnused_DoesNotMatch() {
    List<TextUnitDTO> existingTextUnitDTOs =
        Arrays.asList(
            createTextUnitDTO("name-0"),
            createTextUnitDTO("name-1"),
            createUnusedPluralTextUnitDTO("name-2", "other", "comment-2"),
            createUnusedPluralTextUnitDTO("name-2", "one", "comment-2"),
            createUnusedPluralTextUnitDTO("name-3", "zero", "comment-3"),
            createUnusedPluralTextUnitDTO("name-3", "one", "comment-3"),
            createUnusedPluralTextUnitDTO("name-3", "two", "comment-3"),
            createUnusedPluralTextUnitDTO("name-3", "few", "comment-3"),
            createUnusedPluralTextUnitDTO("name-3", "many", "comment-3"),
            createUnusedPluralTextUnitDTO("name-3", "other", "comment-3"));

    Function<TextUnitForBatchMatcher, Optional<List<TextUnitDTO>>> matchByPluralPrefixAndComment =
        textUnitBatchMatcher.createMatchByPluralPrefixCommentAndUnused(
            existingTextUnitDTOs, PLURAL_SEPARATOR);

    Optional<List<TextUnitDTO>> name2 =
        matchByPluralPrefixAndComment.apply(
            createPluralTextUnitForBatchMatcher("name-2", "comment-4"));
    assertTrue(name2.isEmpty());

    Optional<List<TextUnitDTO>> name3 =
        matchByPluralPrefixAndComment.apply(
            createPluralTextUnitForBatchMatcher("name-3", "comment-5"));
    assertTrue(name3.isEmpty());
  }

  @Test
  public void testCreateMatchByNameSourceAndUsed() {
    List<TextUnitDTO> existingTextUnitDTOs =
        Arrays.asList(
            createTextUnitDTO("name-1", "First source"),
            createTextUnitDTO("name-2", "Second source"));

    Function<TextUnitForBatchMatcher, Optional<TextUnitDTO>> matchByNameSourceAndUsed =
        textUnitBatchMatcher.createMatchByNameSourceAndUsed(existingTextUnitDTOs);

    Optional<TextUnitDTO> textUnit =
        matchByNameSourceAndUsed.apply(createTextUnitForBatchMatcher("name-2", "Second source"));

    assertTrue(textUnit.isPresent());
    assertEquals("name-2", textUnit.get().getName());
  }

  @Test
  public void testCreateMatchByNameSourceAndUsed_RemovesLeadingAndTrailingWhitespaces() {
    List<TextUnitDTO> existingTextUnitDTOs =
        Arrays.asList(
            createTextUnitDTO("name-1", "First source"),
            createTextUnitDTO("name-2", " Second source "));

    Function<TextUnitForBatchMatcher, Optional<TextUnitDTO>> matchByNameSourceAndUsed =
        textUnitBatchMatcher.createMatchByNameSourceAndUsed(existingTextUnitDTOs);

    Optional<TextUnitDTO> textUnit =
        matchByNameSourceAndUsed.apply(createTextUnitForBatchMatcher("name-2", "Second source"));

    assertTrue(textUnit.isPresent());
    assertEquals("name-2", textUnit.get().getName());
  }

  @Test
  public void testCreateMatchByNameSourceAndUsed_DoesNotMatch() {
    List<TextUnitDTO> existingTextUnitDTOs =
        Arrays.asList(
            createTextUnitDTO("name-1", "First source"),
            createTextUnitDTO("name-2", "Second source"));

    Function<TextUnitForBatchMatcher, Optional<TextUnitDTO>> matchByNameSourceAndUsed =
        textUnitBatchMatcher.createMatchByNameSourceAndUsed(existingTextUnitDTOs);

    Optional<TextUnitDTO> textUnit =
        matchByNameSourceAndUsed.apply(createTextUnitForBatchMatcher("name-1", "Second source"));

    assertTrue(textUnit.isEmpty());

    textUnit =
        matchByNameSourceAndUsed.apply(createTextUnitForBatchMatcher("name-2", "First source"));

    assertTrue(textUnit.isEmpty());
  }

  @Test
  public void testCreateMatchByNameCommentAndUsed() {
    List<TextUnitDTO> existingTextUnitDTOs =
        Arrays.asList(
            createTextUnitDTOWithComment("name-1", "comment-1"),
            createTextUnitDTOWithComment("name-2", "comment-2"));

    Function<TextUnitForBatchMatcher, Optional<TextUnitDTO>> matchByNameCommentAndUsed =
        textUnitBatchMatcher.createMatchByNameCommentAndUsed(existingTextUnitDTOs);

    Optional<TextUnitDTO> textUnit =
        matchByNameCommentAndUsed.apply(
            createTextUnitForBatchMatcherWithComment("name-2", "comment-2"));

    assertTrue(textUnit.isPresent());
    assertEquals("name-2", textUnit.get().getName());
  }

  @Test
  public void testCreateMatchByNameCommentAndUsed_DoesNotMatch() {
    List<TextUnitDTO> existingTextUnitDTOs =
        Arrays.asList(
            createTextUnitDTOWithComment("name-1", "comment-1"),
            createTextUnitDTOWithComment("name-2", "comment-2"));

    Function<TextUnitForBatchMatcher, Optional<TextUnitDTO>> matchByNameCommentAndUsed =
        textUnitBatchMatcher.createMatchByNameCommentAndUsed(existingTextUnitDTOs);

    Optional<TextUnitDTO> textUnit =
        matchByNameCommentAndUsed.apply(
            createTextUnitForBatchMatcherWithComment("name-1", "comment-2"));

    assertTrue(textUnit.isEmpty());

    textUnit =
        matchByNameCommentAndUsed.apply(
            createTextUnitForBatchMatcherWithComment("name-2", "comment-1"));

    assertTrue(textUnit.isEmpty());
  }

  @Test
  public void testCreateMatchByNameSourceAndUnused() {
    List<TextUnitDTO> existingTextUnitDTOs =
        Arrays.asList(
            createUnusedTextUnitDTO("name-1", "First string"),
            createUnusedTextUnitDTO("name-2", "Second string"));

    Function<TextUnitForBatchMatcher, Optional<TextUnitDTO>> matchByNameSourceAndUnused =
        textUnitBatchMatcher.createMatchByNameSourceAndUnused(existingTextUnitDTOs);

    Optional<TextUnitDTO> textUnit =
        matchByNameSourceAndUnused.apply(createTextUnitForBatchMatcher("name-2", "Second string"));

    assertTrue(textUnit.isPresent());
    assertEquals("name-2", textUnit.get().getName());
  }

  @Test
  public void testCreateMatchByNameSourceAndUnused_RemovesLeadingAndTrailingWhitespaces() {
    List<TextUnitDTO> existingTextUnitDTOs =
        Arrays.asList(
            createUnusedTextUnitDTO("name-1", "First source"),
            createUnusedTextUnitDTO("name-2", " Second source "));

    Function<TextUnitForBatchMatcher, Optional<TextUnitDTO>> matchByNameSourceAndUnused =
        textUnitBatchMatcher.createMatchByNameSourceAndUnused(existingTextUnitDTOs);

    Optional<TextUnitDTO> textUnit =
        matchByNameSourceAndUnused.apply(createTextUnitForBatchMatcher("name-2", "Second source"));

    assertTrue(textUnit.isPresent());
    assertEquals("name-2", textUnit.get().getName());
  }

  @Test
  public void testCreateMatchByNameSourceAndUnused_DoesNotMatch() {
    List<TextUnitDTO> existingTextUnitDTOs =
        Arrays.asList(
            createUnusedTextUnitDTO("name-1", "First string"),
            createUnusedTextUnitDTO("name-2", "Second string"));

    Function<TextUnitForBatchMatcher, Optional<TextUnitDTO>> matchByNameSourceAndUnused =
        textUnitBatchMatcher.createMatchByNameSourceAndUnused(existingTextUnitDTOs);

    Optional<TextUnitDTO> textUnit =
        matchByNameSourceAndUnused.apply(createTextUnitForBatchMatcher("name-2", "First string"));

    assertTrue(textUnit.isEmpty());
  }

  @Test
  public void testCreateMatchByNameCommentAndUnused() {
    List<TextUnitDTO> existingTextUnitDTOs =
        Arrays.asList(
            createUnusedTextUnitDTOWithComment("name-1", "comment-1"),
            createUnusedTextUnitDTOWithComment("name-2", "comment-2"));

    Function<TextUnitForBatchMatcher, Optional<TextUnitDTO>> matchByNameCommentAndUnused =
        textUnitBatchMatcher.createMatchByNameCommentAndUnused(existingTextUnitDTOs);

    Optional<TextUnitDTO> textUnit =
        matchByNameCommentAndUnused.apply(
            createTextUnitForBatchMatcherWithComment("name-2", "comment-2"));

    assertTrue(textUnit.isPresent());
    assertEquals("name-2", textUnit.get().getName());
  }

  @Test
  public void testCreateMatchByNameCommentAndUnused_DoesNotMatchByComment() {
    List<TextUnitDTO> existingTextUnitDTOs =
        Arrays.asList(
            createUnusedTextUnitDTOWithComment("name-1", "comment-1"),
            createUnusedTextUnitDTOWithComment("name-2", "comment-2"));

    Function<TextUnitForBatchMatcher, Optional<TextUnitDTO>> matchByNameCommentAndUnused =
        textUnitBatchMatcher.createMatchByNameCommentAndUnused(existingTextUnitDTOs);

    Optional<TextUnitDTO> textUnit =
        matchByNameCommentAndUnused.apply(
            createTextUnitForBatchMatcherWithComment("name-2", "comment-1"));

    assertTrue(textUnit.isEmpty());
  }

  @Test
  public void testMatchByNameAndPluralPrefix() {
    Function<TextUnitForBatchMatcher, List<TextUnitDTO>> textUnitForBatchMatcherListFunction =
        textUnitBatchMatcher.matchByNameAndPluralPrefix(Collections.emptyList(), PLURAL_SEPARATOR);
    textUnitForBatchMatcherListFunction.apply(createTextUnitForBatchMatcher("test", "source text"));
  }

  TextUnitForBatchMatcher createTextUnitForBatchMatcher(String name) {
    return createTextUnitForBatchMatcher(name, UUID.randomUUID().getMostSignificantBits(), null);
  }

  TextUnitForBatchMatcher createTextUnitForBatchMatcher(String name, Long tmTextUnitId) {
    return createTextUnitForBatchMatcher(name, tmTextUnitId, null);
  }

  TextUnitForBatchMatcher createTextUnitForBatchMatcherWithComment(String name, String comment) {
    return createTextUnitForBatchMatcher(name, comment, null);
  }

  TextUnitForBatchMatcher createTextUnitForBatchMatcher(String name, String source) {
    return createTextUnitForBatchMatcher(name, (String) null, source);
  }

  TextUnitForBatchMatcher createTextUnitForBatchMatcher(String name, Long id, String comment) {
    return createTextUnitForBatchMatcher(name, id, false, comment, null);
  }

  TextUnitForBatchMatcher createTextUnitForBatchMatcher(
      String name, String comment, String source) {
    return createTextUnitForBatchMatcher(
        name, UUID.randomUUID().getMostSignificantBits(), false, comment, source);
  }

  TextUnitForBatchMatcher createTextUnitForBatchMatcher(
      String name, Long id, boolean isNamePluralPrefix, String comment, String source) {
    TextUnitForBatchMatcher textUnitForBatchMatcher =
        new TextUnitForBatchMatcher() {
          @Override
          public String getName() {
            return name;
          }

          @Override
          public Long getTmTextUnitId() {
            return id;
          }

          @Override
          public boolean isNamePluralPrefix() {
            return isNamePluralPrefix;
          }

          @Override
          public String getSource() {
            return source;
          }

          @Override
          public String getComment() {
            return comment;
          }
        };

    return textUnitForBatchMatcher;
  }

  TextUnitForBatchMatcher createPluralTextUnitForBatchMatcher(
      String name, String comment, String source) {
    return createTextUnitForBatchMatcher(
        name, UUID.randomUUID().getMostSignificantBits(), true, comment, source);
  }

  TextUnitForBatchMatcher createPluralTextUnitForBatchMatcher(String name) {
    return createTextUnitForBatchMatcher(
        name, UUID.randomUUID().getMostSignificantBits(), true, null, null);
  }

  TextUnitForBatchMatcher createPluralTextUnitForBatchMatcher(String name, String comment) {
    return createTextUnitForBatchMatcher(
        name, UUID.randomUUID().getMostSignificantBits(), true, comment, null);
  }

  TextUnitDTO createTextUnitDTO(String name, Long tmTextUnitId, String comment, String source) {
    TextUnitDTO textUnitDTO =
        new TextUnitDTO() {
          @Override
          public boolean isUsed() {
            return true;
          }
        };
    textUnitDTO.setName(name);
    textUnitDTO.setTmTextUnitId(tmTextUnitId);
    textUnitDTO.setComment(comment);
    textUnitDTO.setSource(source);
    return textUnitDTO;
  }

  TextUnitDTO createTextUnitDTO(String name) {
    return createTextUnitDTO(name, UUID.randomUUID().getMostSignificantBits(), null, null);
  }

  TextUnitDTO createTextUnitDTO(String name, String comment, String source) {
    return createTextUnitDTO(name, UUID.randomUUID().getMostSignificantBits(), comment, source);
  }

  TextUnitDTO createTextUnitDTOWithComment(String name, String comment) {
    return createTextUnitDTO(name, UUID.randomUUID().getMostSignificantBits(), comment, null);
  }

  TextUnitDTO createTextUnitDTO(String name, String source) {
    return createTextUnitDTO(name, UUID.randomUUID().getMostSignificantBits(), null, source);
  }

  TextUnitDTO createTextUnitDTO(String name, Long tmTextUnitId) {
    return createTextUnitDTO(name, tmTextUnitId, null, null);
  }

  TextUnitDTO createUnusedTextUnitDTO(String name, String comment, String source) {
    TextUnitDTO textUnitDTO =
        new TextUnitDTO() {
          @Override
          public boolean isUsed() {
            return false;
          }
        };
    textUnitDTO.setName(name);
    textUnitDTO.setTmTextUnitId(UUID.randomUUID().getMostSignificantBits());
    textUnitDTO.setComment(comment);
    textUnitDTO.setSource(source);
    return textUnitDTO;
  }

  TextUnitDTO createUnusedTextUnitDTO(String name) {
    return this.createUnusedTextUnitDTO(name, null, null);
  }

  TextUnitDTO createUnusedTextUnitDTO(String name, String source) {
    return this.createUnusedTextUnitDTO(name, null, source);
  }

  TextUnitDTO createUnusedTextUnitDTOWithComment(String name, String comment) {
    return this.createUnusedTextUnitDTO(name, comment, null);
  }

  TextUnitDTO createPluralTextUnitDTO(String prefix, String form, String comment) {
    TextUnitDTO textUnitDTO = createTextUnitDTO(prefix + PLURAL_SEPARATOR + form);
    textUnitDTO.setPluralForm(form);
    textUnitDTO.setPluralFormOther(prefix + "_other");
    textUnitDTO.setComment(comment);
    return textUnitDTO;
  }

  TextUnitDTO createPluralTextUnitDTO(String prefix, String form) {
    return this.createPluralTextUnitDTO(prefix, form, null);
  }

  TextUnitDTO createUnusedPluralTextUnitDTO(String prefix, String form, String comment) {
    TextUnitDTO textUnitDTO = createUnusedTextUnitDTO(prefix + "_" + form);
    textUnitDTO.setPluralForm(form);
    textUnitDTO.setPluralFormOther(prefix + "_other");
    textUnitDTO.setComment(comment);
    return textUnitDTO;
  }

  TextUnitDTO createUnusedPluralTextUnitDTO(String prefix, String form) {
    return this.createUnusedPluralTextUnitDTO(prefix, form, null);
  }
}
