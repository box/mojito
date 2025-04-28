package com.box.l10n.mojito.service.thirdparty.smartling.glossary;

import java.util.Arrays;
import opennlp.tools.stemmer.PorterStemmer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "l10n.glossary.cache.enabled", havingValue = "true")
public class StemmerService {

  private final PorterStemmer stemmer = new PorterStemmer();

  /**
   * Stems the input text using the Porter stemming algorithm.
   *
   * <p>Input text is converted to lowercase and split into words.
   *
   * @param text the input text to stem
   * @return the stemmed text
   */
  public String stem(String text) {
    return String.join(
        " ",
        Arrays.stream(text.toLowerCase().split("\\s+")).map(stemmer::stem).toArray(String[]::new));
  }
}
