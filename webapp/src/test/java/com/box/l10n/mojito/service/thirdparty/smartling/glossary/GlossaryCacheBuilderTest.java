package com.box.l10n.mojito.service.thirdparty.smartling.glossary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.service.WordCountService;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class GlossaryCacheBuilderTest {

  @Mock GlossaryCacheConfiguration glossaryCacheConfiguration;

  @Mock GlossaryCacheBlobStorage blobStorage;

  @Mock TextUnitSearcher textUnitSearcher;

  @Mock WordCountService wordCountService;

  @Mock StemmerService stemmer;

  @Mock MeterRegistry meterRegistry;

  @Mock Counter counter;

  GlossaryCacheBuilder glossaryCacheBuilder;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    when(meterRegistry.counter(any())).thenReturn(counter);
    glossaryCacheBuilder =
        spy(
            new GlossaryCacheBuilder(
                glossaryCacheConfiguration,
                blobStorage,
                textUnitSearcher,
                wordCountService,
                stemmer,
                meterRegistry));
  }

  @Test
  void testBlobStorageNotUpdatedIfNoConfiguredRepos() {
    when(glossaryCacheConfiguration.getEnabled()).thenReturn(true);
    when(glossaryCacheConfiguration.getRepositories()).thenReturn(Collections.emptyList());

    glossaryCacheBuilder.buildCache();

    verify(blobStorage, never()).putGlossaryCache(any());
    verify(counter, never()).increment(isA(Integer.class));
  }

  @Test
  void testCacheIsStoredInBlobStorageOnBuild() {
    when(glossaryCacheConfiguration.getEnabled()).thenReturn(true);
    when(glossaryCacheConfiguration.getRepositories()).thenReturn(List.of("repo1"));

    GlossaryCache glossaryCache = new GlossaryCache();
    glossaryCache.setCache(
        Map.of("entry", List.of(new GlossaryTerm("term", false, false, false, 1L))));
    doReturn(glossaryCache).when(glossaryCacheBuilder).buildGlossaryCache();

    glossaryCacheBuilder.buildCache();

    verify(blobStorage).putGlossaryCache(glossaryCache);
    verify(counter, times(1)).increment(1);
  }

  @Test
  void testMaxNGramSizeIsUpdated() {
    GlossaryCache glossaryCache = new GlossaryCache();
    GlossaryTerm glossaryTerm = new GlossaryTerm("term", false, false, false, 1L);

    when(wordCountService.getEnglishWordCount("term")).thenReturn(5);

    glossaryCacheBuilder.processTerm(glossaryTerm, glossaryCache);

    Assertions.assertEquals(5, glossaryCache.getMaxNGramSize());

    when(wordCountService.getEnglishWordCount("term")).thenReturn(10);

    glossaryCacheBuilder.processTerm(glossaryTerm, glossaryCache);

    // Max Ngram size should be updated to 10
    Assertions.assertEquals(10, glossaryCache.getMaxNGramSize());

    when(wordCountService.getEnglishWordCount("term")).thenReturn(5);

    glossaryCacheBuilder.processTerm(glossaryTerm, glossaryCache);

    // Max ngram should remain as 10
    Assertions.assertEquals(10, glossaryCache.getMaxNGramSize());
  }

  @Test
  void testStemmedTermUsedAsKey() {
    GlossaryCache glossaryCache = new GlossaryCache();
    GlossaryTerm glossaryTerm = new GlossaryTerm("term", false, false, false, 1L);

    when(stemmer.stem("term")).thenReturn("stemmedTerm");

    glossaryCacheBuilder.processTerm(glossaryTerm, glossaryCache);

    Assertions.assertTrue(glossaryCache.getCache().containsKey("stemmedTerm"));
    Assertions.assertTrue(glossaryCache.getCache().get("stemmedTerm").contains(glossaryTerm));
  }

  @Test
  void testTranslationsAreRetrievedForGlossaryTerms() {
    Map<Long, List<GlossaryTerm>> glossaryTermMap = new HashMap<>();
    GlossaryTerm glossaryTerm = new GlossaryTerm("term", false, false, false, 1L);
    glossaryTermMap.put(1L, List.of(glossaryTerm));

    TextUnitDTO textUnitDTO = new TextUnitDTO();
    textUnitDTO.setTmTextUnitId(1L);
    textUnitDTO.setTargetLocale("fr");
    textUnitDTO.setTarget("terme");

    when(textUnitSearcher.search(any())).thenReturn(List.of(textUnitDTO));

    List<GlossaryTerm> result =
        glossaryCacheBuilder.retrieveTranslationsForGlossaryTerms(glossaryTermMap);

    Assertions.assertEquals(1, result.size());
    Assertions.assertEquals("terme", glossaryTerm.getTranslations().get("fr"));
  }

  @Test
  void testSourceTermVariationsMaintainGlossaryTermConfig() {
    TextUnitDTO textUnitDTO = new TextUnitDTO();
    textUnitDTO.setTmTextUnitId(1L);
    textUnitDTO.setSource("source");
    textUnitDTO.setComment("--- Variations: beginning,start --- Case Sensitive: true");

    when(textUnitSearcher.search(any())).thenReturn(List.of(textUnitDTO));

    Map<Long, List<GlossaryTerm>> result = glossaryCacheBuilder.getSourceTerms();

    Assertions.assertEquals(1, result.size());
    Assertions.assertTrue(result.containsKey(1L));
    Assertions.assertEquals(3, result.get(1L).size());
    for (GlossaryTerm glossaryTerm : result.get(1L)) {
      Assertions.assertTrue(glossaryTerm.isCaseSensitive());
      Assertions.assertFalse(glossaryTerm.isExactMatch());
      Assertions.assertFalse(glossaryTerm.isDoNotTranslate());
    }
  }

  @Test
  void testSourceTermVariationsAreGroupedByTmTextUnitIds() {
    TextUnitDTO textUnitDTO = new TextUnitDTO();
    textUnitDTO.setTmTextUnitId(1L);
    textUnitDTO.setSource("source");
    textUnitDTO.setComment("--- Variations: beginning,start");

    when(textUnitSearcher.search(any())).thenReturn(List.of(textUnitDTO));

    Map<Long, List<GlossaryTerm>> result = glossaryCacheBuilder.getSourceTerms();

    Assertions.assertEquals(1, result.size());
    Assertions.assertTrue(result.containsKey(1L));
    Assertions.assertEquals(3, result.get(1L).size());
  }
}
