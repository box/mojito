package com.box.l10n.mojito.service.languagedetection;

import static org.junit.Assert.*;

import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import com.cybozu.labs.langdetect.LangDetectException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * TODO(P1) This is not actually an test but experiments ran on data from old webapp (this requires
 * data to be imported first), to be revisited (test are commented as not relevant to be ran without
 * data).
 *
 * <p>Those experiments shows that so far the language detection is acceptable for ko-KR but not so
 * good for fr-FR.
 *
 * <p>ko-KR: detection: 12384, source = target: 126, good: 12189, bad: 46, failed: 23, average
 * quality: 0.986633049203721, % good: 0.9842538759689923 fr-FR: detection: 12384, source = target:
 * 274, good: 10235, bad: 1867, failed: 8, average quality: 0.9445133386822007, % good:
 * 0.8264696382428941
 *
 * <p>The source = target difference is troubling too...
 *
 * @author jaurambault
 */
public class LanguageDetectionServiceTest extends ServiceTestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(LanguageDetectionServiceTest.class);

  @Autowired TextUnitSearcher textUnitSearcher;

  @Autowired LanguageDetectionService languageDetectionService;

  @Test
  public void testDetection()
      throws LangDetectException, IOException, UnsupportedLanguageException {

    int nbFailed = 0;
    int nbBad = 0;
    double probabilitiesSum = 0;
    int sourceEqualsTarget = 0;
    int nbGood = 0;

    List<TextUnitDTO> translationsForLanguage = getTranslationsForLanguage("ko-KR", null);

    for (TextUnitDTO transaltionForLanguage : translationsForLanguage) {

      if (transaltionForLanguage.getTarget().equals(transaltionForLanguage.getSource())) {
        logger.debug("Skip source = target: {}", transaltionForLanguage.getSource());
        sourceEqualsTarget++;
        continue;
      }

      LanguageDetectionResult ldr =
          languageDetectionService.detect(
              transaltionForLanguage.getTarget(), transaltionForLanguage.getTargetLocale());
      probabilitiesSum += ldr.getProbability();

      if (ldr.getLangDetectException() != null) {
        nbFailed++;
        logger.info("Language detection failed for: {}", transaltionForLanguage.getTarget());
        logger.info("Error was", ldr.getLangDetectException());
        continue;
      }

      if (!ldr.isExpectedLanguage()) {
        nbBad++;
        logger.info(
            "Not proper language, found: {} probability: {}, should be {}/{} ({}), text: {}, {}",
            ldr.getDetected(),
            ldr.getProbability(),
            ldr.getExpected(),
            transaltionForLanguage.getTargetLocale(),
            ldr.getProbabilityExpected(),
            transaltionForLanguage.getTarget(),
            ldr.getDetector() != null ? ldr.getDetector().getProbabilities() : "-");

      } else {
        nbGood++;
      }
    }

    int nbDetection = translationsForLanguage.size();

    logger.info(
        "detection: {}, source = target: {}, good: {},  bad: {}, failed: {}, average quality: {}, % good: {}",
        nbDetection,
        sourceEqualsTarget,
        nbGood,
        nbBad,
        nbFailed,
        probabilitiesSum / (double) nbDetection,
        nbGood / (double) nbDetection);
  }

  private List<TextUnitDTO> getTranslationsForLanguage(String bcp47Tag, Integer limit) {
    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED);
    textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);

    if (bcp47Tag != null) {
      textUnitSearcherParameters.setLocaleTags(Arrays.asList(bcp47Tag));
    }

    if (limit != null) {
      textUnitSearcherParameters.setLimit(limit);
    }
    return textUnitSearcher.search(textUnitSearcherParameters);
  }

  public void testIsSupportedLanguageFalse() {
    assertFalse(languageDetectionService.isSupportedBcp47Tag("x-ps-accent"));
  }

  public void testIsSupportedLanguageTrue() {
    assertTrue(languageDetectionService.isSupportedBcp47Tag("fr"));
    assertTrue(languageDetectionService.isSupportedBcp47Tag("fr-FR"));
    assertTrue(languageDetectionService.isSupportedBcp47Tag("fr-FR-x-bla"));
  }
}
