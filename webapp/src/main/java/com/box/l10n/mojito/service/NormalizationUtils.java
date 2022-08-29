package com.box.l10n.mojito.service;

import com.ibm.icu.text.Normalizer2;

/**
 * Wrapper for {@link Normalizer} to do null-safe unicode normalization
 *
 * @author jyi
 */
public class NormalizationUtils {

  public static String normalize(String string) {
    if (string != null) {
      string = Normalizer2.getNFCInstance().normalize(string);
    }
    return string;
  }
}
