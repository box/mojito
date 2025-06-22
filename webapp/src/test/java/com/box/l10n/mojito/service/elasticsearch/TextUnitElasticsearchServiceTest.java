package com.box.l10n.mojito.service.elasticsearch;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class TextUnitElasticsearchServiceTest extends ServiceTestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(TextUnitElasticsearchServiceTest.class);

  @Autowired TextUnitSearcher textUnitSearcher;

  @Autowired TextUnitElasticsearchService textUnitElasticsearchService;

  @Autowired TextUnitElasticsearchRepository textUnitElasticsearchRepository;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Test
  public void testFuzzy() {

    textUnitElasticsearchRepository.deleteAll();

    List<String> locales = List.of("en", "fr", "fr-CA", "ja");

    for (String locale : locales) {
      TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
      textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED);
      textUnitSearcherParameters.setRepositoryNames(List.of("web"));
      if ("en".equals(locale)) {
        textUnitSearcherParameters.setForRootLocale(true);
      } else {
        textUnitSearcherParameters.setLocaleTags(List.of(locale));
      }
      textUnitSearcherParameters.setLimit(100000);

      List<TextUnitDTO> search = textUnitSearcher.search(textUnitSearcherParameters);

      logger.info("Indexing, count: {} for locale: {}", search.size(), locale);
      textUnitElasticsearchService.indexTextUnitDTOs(search);
    }

    logger.info("Match only on all locales");
    List<Hit<TextUnitElasticsearch>> fuzzies =
        textUnitElasticsearchService.fuzzySearchByTarget("valde", null);

    for (Hit<TextUnitElasticsearch> fuzzy : fuzzies) {
      logger.info("hit all locales: {}", fuzzy);
    }

    logger.info("Match only on some locales: fr and fr-CA");
    List<Hit<TextUnitElasticsearch>> fuzziesForFrench =
        textUnitElasticsearchService.fuzzySearchByTarget("valde", List.of("fr", "fr-CA"));

    for (Hit<TextUnitElasticsearch> fuzzy : fuzziesForFrench) {
      logger.info("hit french only: {}", fuzzy);
    }

    logger.info("Categorize source strings");
    TextUnitElasticsearchService.Buckets sourceGroupsRegex =
        textUnitElasticsearchService.categorizeText();
    sourceGroupsRegex.buckets().stream().forEach(b -> logger.info("bucket: {}", b));
  }
}
