package com.box.l10n.mojito.service.thirdparty.smartling.glossary;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class GlossaryCacheServiceTest {

  private final GlossaryCacheBlobStorage glossaryCacheBlobStorage =
      Mockito.mock(GlossaryCacheBlobStorage.class);
  private final StemmerService stemmerService = new StemmerService();
  private final GlossaryCacheBuilder glossaryCacheBuilder =
      Mockito.mock(GlossaryCacheBuilder.class);
  private final GlossaryCacheService glossaryCacheService =
      new GlossaryCacheService(glossaryCacheBlobStorage, glossaryCacheBuilder, stemmerService);

  private final String exactMatch = "exact match";
  private final String caseSensitive = "Case sensitive";
  private final String doNotTranslate = "Do not translate";
  private final String termMatch = "term match";
  private final String matchChain = "match chain";

  @BeforeEach
  public void setUp() {
    Map<String, List<GlossaryTerm>> map = new HashMap<>();
    map.put(
        stemmerService.stem(exactMatch),
        List.of(new GlossaryTerm(exactMatch, true, false, false, 1L)));
    map.put(
        stemmerService.stem(caseSensitive),
        List.of(new GlossaryTerm(caseSensitive, true, true, false, 2L)));
    map.put(
        stemmerService.stem(doNotTranslate),
        List.of(new GlossaryTerm(doNotTranslate, false, false, true, 3L)));
    map.put(
        stemmerService.stem(termMatch),
        List.of(new GlossaryTerm(termMatch, false, false, false, 4L)));
    map.put(
        stemmerService.stem(matchChain),
        List.of(new GlossaryTerm(matchChain, false, false, false, 5L)));
    GlossaryCache glossaryCache = new GlossaryCache();
    glossaryCache.setMaxNGramSize(10);
    glossaryCache.setCache(map);

    when(glossaryCacheBlobStorage.getGlossaryCache()).thenReturn(Optional.of(glossaryCache));
  }

  @Test
  void testExactMatchMatching() {

    List<GlossaryTerm> result = glossaryCacheService.getGlossaryTermsInText(exactMatch);

    Assertions.assertEquals(1, result.size());
    Assertions.assertEquals(exactMatch, result.getFirst().getText());
  }

  @Test
  void testGlossaryTermsAreNotRetrievedForTextWithNoMatch() {

    List<GlossaryTerm> result = glossaryCacheService.getGlossaryTermsInText("no match");

    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void testCaseSensitiveMatching() {
    List<GlossaryTerm> result = glossaryCacheService.getGlossaryTermsInText(caseSensitive);

    Assertions.assertEquals(1, result.size());
    Assertions.assertEquals(caseSensitive, result.getFirst().getText());
  }

  @Test
  void testOverlappingGlossaryTermsAreResolvedByLongestMatch() {

    GlossaryCache glossaryCache = glossaryCacheBlobStorage.getGlossaryCache().get();
    glossaryCache.add(
        stemmerService.stem("longer match"),
        new GlossaryTerm("longer match", false, false, false, 7L));
    glossaryCache.add(
        stemmerService.stem("match"), new GlossaryTerm("match", false, false, false, 8L));

    List<GlossaryTerm> result = glossaryCacheService.getGlossaryTermsInText("longer match");

    Assertions.assertEquals(1, result.size());
    Assertions.assertEquals("longer match", result.getFirst().getText());
  }

  @Test
  void testChainedMatchesAreBothReturned() {
    List<GlossaryTerm> result = glossaryCacheService.getGlossaryTermsInText("term match chain");

    Assertions.assertEquals(2, result.size());
    Assertions.assertTrue(result.stream().anyMatch(term -> term.getText().equals(termMatch)));
    Assertions.assertTrue(result.stream().anyMatch(term -> term.getText().equals(matchChain)));
  }

  @Test
  void testCaseSensitiveOverridesExactMatchIfBothConfigured() {
    List<GlossaryTerm> result = glossaryCacheService.getGlossaryTermsInText("Case Sensitive");

    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void testEmptyListReturnedIfNoGlossaryInBlobStorage() {
    when(glossaryCacheBlobStorage.getGlossaryCache()).thenReturn(Optional.empty());
    glossaryCacheService.loadGlossaryCache();

    Assertions.assertNotNull(glossaryCacheService.getGlossaryTermsInText(""));
  }

  @Test
  void testOverlappedTermAlsoSeparatelyMatchedInText() {
    GlossaryCache glossaryCache = glossaryCacheBlobStorage.getGlossaryCache().get();
    glossaryCache.add(
        stemmerService.stem("longer match"),
        new GlossaryTerm("longer match", false, false, false, 7L));
    glossaryCache.add(
        stemmerService.stem("match"), new GlossaryTerm("match", false, false, false, 8L));

    List<GlossaryTerm> result =
        glossaryCacheService.getGlossaryTermsInText("longer match is a match");
    Assertions.assertEquals(2, result.size());
    Assertions.assertEquals("longer match", result.getFirst().getText());
    Assertions.assertEquals("match", result.getLast().getText());
  }

  @Test
  void testMultipleSpacesBetweenTerms() {
    List<GlossaryTerm> result = glossaryCacheService.getGlossaryTermsInText("  term   match  ");

    Assertions.assertEquals(1, result.size());
    Assertions.assertEquals(termMatch, result.getFirst().getText());
  }

  @Test
  void testEmojisInBetweenTerm() {
    List<GlossaryTerm> result = glossaryCacheService.getGlossaryTermsInText("term 😊 match");

    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void testEmptyStringReturnsNoMatches() {
    List<GlossaryTerm> result = glossaryCacheService.getGlossaryTermsInText("");

    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void testOnlyWhitespaceReturnsNoMatches() {
    List<GlossaryTerm> result = glossaryCacheService.getGlossaryTermsInText("   ");

    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void textWithOnlyEmojisReturnsNoMatches() {
    List<GlossaryTerm> result = glossaryCacheService.getGlossaryTermsInText("😊😊");

    Assertions.assertTrue(result.isEmpty());
  }
}
