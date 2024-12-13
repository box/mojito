package com.box.l10n.mojito.service.thirdparty;

import static com.box.l10n.mojito.service.thirdparty.ThirdPartyTMSPhrase.areTagsWithin5Minutes;
import static com.box.l10n.mojito.service.thirdparty.ThirdPartyTMSPhrase.uploadTagToLocalDateTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.collect.ImmutableMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class ThirdPartyTMSPhraseTest extends ServiceTestBase {

  static Logger logger = LoggerFactory.getLogger(ThirdPartyTMSPhraseTest.class);

  @Autowired(required = false)
  ThirdPartyTMSPhrase thirdPartyTMSPhrase;

  @Autowired RepositoryService repositoryService;

  @Autowired TextUnitSearcher textUnitSearcher;

  @Value("${test.phrase-client.projectId:}")
  String testProjectId;

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Test
  public void testBasics() throws RepositoryLocaleCreationException {
    Assume.assumeNotNull(thirdPartyTMSPhrase);
    Assume.assumeNotNull(testProjectId);

    ThirdPartyServiceTestData thirdPartyServiceTestData =
        new ThirdPartyServiceTestData(testIdWatcher);

    Repository repository = thirdPartyServiceTestData.repository;
    repositoryService.addRepositoryLocale(repository, "fr");

    repository
        .getRepositoryLocales()
        .forEach(rl -> logger.info("repository locale: {}", rl.getLocale().getBcp47Tag()));

    thirdPartyTMSPhrase.push(
        repository,
        testProjectId,
        thirdPartyServiceTestData.getPluralSeparator(),
        null,
        null,
        null);

    thirdPartyTMSPhrase.push(
        repository,
        testProjectId,
        thirdPartyServiceTestData.getPluralSeparator(),
        null,
        null,
        null);

    thirdPartyTMSPhrase.pull(
        repository,
        testProjectId,
        thirdPartyServiceTestData.getPluralSeparator(),
        ImmutableMap.of("fr-FR", "fr"),
        null,
        null,
        null,
        null,
        null);

    TextUnitSearcherParameters params = new TextUnitSearcherParameters();
    params.setRepositoryIds(repository.getId());
    params.setRootLocaleExcluded(false);
    params.setStatusFilter(StatusFilter.TRANSLATED);
    List<TextUnitDTO> search = textUnitSearcher.search(params);

    if (logger.isInfoEnabled()) {
      ObjectMapper objectMapper = new ObjectMapper();
      search.stream().forEach(t -> logger.info(objectMapper.writeValueAsStringUnchecked(t)));
    }

    //        List<ThirdPartyTextUnit> thirdPartyTextUnits =
    // thirdPartyTMSPhrase.getThirdPartyTextUnits(repository, testProjectId, null);
    //        logger.info("Get")
    //        thirdPartyTextUnits.stream().forEach(t -> logger.info("third party text unit: {}",
    // t));

  }

  @Test
  public void testUploadTagToLocalDateTimeValid() {
    LocalDateTime localDateTime = uploadTagToLocalDateTime("push_test_2024_11_21_18_55_38_004_502");
    assertEquals(
        "Parsed LocalDateTime does not match the expected value",
        "2024-11-21T18:55:38.004",
        localDateTime.format(DateTimeFormatter.ISO_DATE_TIME));
  }

  @Test
  public void testUploadTagToLocalDateTimeInvalid() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> uploadTagToLocalDateTime("invalid_tag_2024_11_21_18_55_004"));
    assertTrue(exception.getMessage().contains("Invalid tag format"));
  }

  @Test
  public void testTagsWithin5Minutes() {
    String tag1 = "push_test_2024_11_21_18_55_38_004_502";
    String tag2 = "push_test_2024_11_21_18_53_30_123_456";
    assertTrue(areTagsWithin5Minutes(tag1, tag2));
  }

  @Test
  public void testTagsOutside5Minutes() {
    String tag1 = "push_test_2024_11_21_18_55_38_004_502";
    String tag2 = "push_test_2024_11_21_18_49_30_123_456";
    assertFalse(areTagsWithin5Minutes(tag1, tag2));
  }
}
