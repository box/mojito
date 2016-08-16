package com.box.l10n.mojito.service;

import java.text.Normalizer;
import java.text.Normalizer.Form;

/**
 * Wrapper for {@link Normalizer} to do null-safe unicode normalization
 * 
 * @author jyi
 */
public class NormalizationUtils {
    
    public static final Form NORMALIZATION_FORM = Normalizer.Form.NFC;
    
    public static String normalize(String string) {
        if (string != null) {
            string = Normalizer.normalize(string, NORMALIZATION_FORM);
        }
        return string;
    }
}
