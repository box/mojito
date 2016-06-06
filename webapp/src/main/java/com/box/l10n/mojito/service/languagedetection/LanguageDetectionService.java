package com.box.l10n.mojito.service.languagedetection;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.cybozu.labs.langdetect.Language;
import com.google.common.io.Resources;
import org.apache.commons.io.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Service to perform language detection based langdetect.
 *
 * @author jaurambault
 */
@Service
public class LanguageDetectionService {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(LanguageDetectionService.class);

    /**
     * Languages supported by langdetect.
     */
    private final List<String> supportedLanguages;

    public LanguageDetectionService() {

        if (DetectorFactory.getLangList().isEmpty()) {

            logger.debug("Initialize langdetect with profiles");
            List<String> jsonProfiles = new ArrayList<>();

            Resource[] resources;

            try {
                PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
                resources = pathMatchingResourcePatternResolver.getResources("profiles/*");
            } catch (IOException ex) {
                throw new RuntimeException("Cannot get the list of resources maching langdetect profiles", ex);
            }

            for (Resource resource : resources) {
                String filename = resource.getFilename();

                logger.debug("Add profile for: {}", filename);
                try {
                    jsonProfiles.add(Resources.toString(resource.getURL(), Charsets.UTF_8));
                } catch (Exception e) {
                    throw new RuntimeException("Cannot load langdetect profile for " + filename, e);
                }
            }

            try {
                logger.debug("Load profiles");
                DetectorFactory.loadProfile(jsonProfiles);
            } catch (LangDetectException lde) {
                throw new RuntimeException("Cannot load langdetect profiles", lde);
            }
        } else {
            logger.debug("langdetect profiles are already initialized");
        }

        logger.debug("Sets langdetect supported languages");
        supportedLanguages = Collections.unmodifiableList(DetectorFactory.getLangList());
    }

    public List<String> getSupportedLanguages() {
        return supportedLanguages;
    }

    /**
     * Gets a language supported by the Detector that maps to the provided bcp47
     * tag.
     *
     * @param bcp47tag bcp47tag that will be mapped to a language supported by
     * langdetect
     *
     * @return language supported by langdetect
     */
    private String getDetectorLanguageForBcp47Tag(String bcp47tag) throws UnsupportedLanguageException {
        String detectorLanguage = null;

        String lowerCaseBcp47tag = bcp47tag.toLowerCase();

        if (supportedLanguages.contains(lowerCaseBcp47tag)) {
            detectorLanguage = lowerCaseBcp47tag;
        } else {
            Locale locale = Locale.forLanguageTag(lowerCaseBcp47tag);
            String language = locale.getLanguage();
            if (supportedLanguages.contains(language)) {
                detectorLanguage = language;
            }
        }

        if (detectorLanguage == null) {
            throw new UnsupportedLanguageException("Unsupported language by langdetect: " + bcp47tag);
        }

        return detectorLanguage;
    }

    /**
     * Indicates if the service supports requested language for a BCP47 tag.
     *
     * @param bcp47Tag a bcp47 tag (can contain a locale)
     * @return {@code true} if language is supported else {@code false}
     */
    public boolean isSupportedBcp47Tag(String bcp47Tag) {

        boolean supported = true;

        try {
            getDetectorLanguageForBcp47Tag(bcp47Tag);
        } catch (UnsupportedLanguageException ule) {
            supported = false;
        }

        return supported;
    }

    /**
     * Detects the language of input text taking in account the expected
     * language of the text.
     *
     * @param text text for language detection
     * @param expectedBcp47Tag the expected bcp47 tag of the text (can contain a locale)
     * @return a {@link LanguageDetectionResult}
     * @throws UnsupportedLanguageException if the language is not supported,
     * see {@link #isSupportedBcp47Tag(java.lang.String) } to check supported
     * languages.
     */
    public LanguageDetectionResult detect(String text, String expectedBcp47Tag) throws UnsupportedLanguageException {

        LanguageDetectionResult languageDetectionResult = new LanguageDetectionResult();

        try {
            String detectorLanguageForBcp47Tag = getDetectorLanguageForBcp47Tag(expectedBcp47Tag);

            Detector detector = getDetectorForLanguage(detectorLanguageForBcp47Tag);

            detector.append(text);
            String detect = detector.detect();

            ArrayList<Language> probabilities = detector.getProbabilities();

            for (Language probability : probabilities) {
                if (probability.lang.equals(detectorLanguageForBcp47Tag)) {
                    languageDetectionResult.setProbabilityExpected(probability.prob);
                }
            }

            languageDetectionResult.setProbability(detector.getProbabilities().get(0).prob);
            languageDetectionResult.setDetected(detect);
            languageDetectionResult.setExpected(detectorLanguageForBcp47Tag);
            languageDetectionResult.setDetector(detector);

        } catch (LangDetectException lde) {
            logger.error("language detection failed\ntext: {}", text);
            languageDetectionResult.setLangDetectException(lde);
        }

        return languageDetectionResult;
    }

    /**
     * Gets a customized detector for a given language.
     *
     * TODO(P1) Adding priority on the language seems to be relatively useless.
     * To be reviewed.
     *
     * @param language
     * @return a {@link Detector} customized for that language
     * @throws LangDetectException
     */
    private Detector getDetectorForLanguage(String language) throws LangDetectException {
        Detector detector = DetectorFactory.create();
        HashMap<String, Double> priorityMap = new HashMap();

        for (String supportedLanguage : getSupportedLanguages()) {
            if (supportedLanguage.equals(language)) {
                priorityMap.put(supportedLanguage, 0.8);
            } else if (supportedLanguage.equals("en") && !"en".equals(language)) {
                priorityMap.put(supportedLanguage, 0.5);
            } else {
                priorityMap.put(supportedLanguage, 0.1);
            }
        }

        detector.setPriorMap(priorityMap);

        return detector;
    }

}
