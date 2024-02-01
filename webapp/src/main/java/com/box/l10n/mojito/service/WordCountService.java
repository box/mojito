package com.box.l10n.mojito.service;

import java.text.BreakIterator;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * @author jaurambault
 */
@Component
public class WordCountService {

  /**
   * Gets the number of words in the string assuming the string is in English.
   *
   * <p>This implementation doesn't know about placeholders. They are counted as word. Later, we can
   * do something more clever using an Okapi step later to exclude them.
   *
   * @param string
   * @return number of word
   */
  public int getEnglishWordCount(String string) {

    int wordCount = 0;

    BreakIterator wordBreakIterator = BreakIterator.getWordInstance(Locale.ENGLISH);

    wordBreakIterator.setText(string);

    int start = wordBreakIterator.first();
    int end = wordBreakIterator.next();

    while (end != BreakIterator.DONE) {

      if (Character.isLetterOrDigit(string.charAt(start))) {
        wordCount += 1;
      }

      start = end;
      end = wordBreakIterator.next();
    }

    return wordCount;
  }
}
