package com.box.l10n.mojito.service.languagedetection;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.LangDetectException;
import java.util.Objects;

/**
 * Contains language detection result from {@link LanguageDetectionService}
 *
 * @author jaurambault
 */
public class LanguageDetectionResult {

    /**
     * Probability of the detected language
     */
    double probability;

    /**
     * Probability of the expected language
     */
    double probabilityExpected;

    /**
     * Detected language
     */
    String detected;

    /**
     * Expected language
     */
    String expected;

    /**
     * If an error occurred
     */
    LangDetectException langDetectException;

    /**
     * The {@link Detector} instance used. Can be used to be get more
     * information
     */
    //TODO(P1) Probably want to remove this later, I use it for now to get
    // more info in the tests.
    Detector detector;

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    public String getDetected() {
        return detected;
    }

    public void setDetected(String detected) {
        this.detected = detected;
    }

    public LangDetectException getLangDetectException() {
        return langDetectException;
    }

    public void setLangDetectException(LangDetectException langDetectException) {
        this.langDetectException = langDetectException;
    }

    public String getExpected() {
        return expected;
    }

    public void setExpected(String expected) {
        this.expected = expected;
    }

    public boolean isExpectedLanguage() {
        return Objects.equals(expected, detected);
    }

    public double getProbabilityExpected() {
        return probabilityExpected;
    }

    public void setProbabilityExpected(double probabilityExpected) {
        this.probabilityExpected = probabilityExpected;
    }

    public Detector getDetector() {
        return detector;
    }

    public void setDetector(Detector detector) {
        this.detector = detector;
    }

}
