package com.box.l10n.mojito.service.oaitranslate;

import com.ibm.icu.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WordTrie<T extends WordTrie.Term> {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(WordTrie.class);

  private final Node<T> root = new Node<>(null, new ArrayList<>(), new HashMap<>());

  public void addTerm(T term) {
    String source = term.text();
    List<String> wordsInString = getWordsInString(source);
    Node<T> cur = root;
    for (String word : wordsInString) {
      cur =
          cur.child()
              .computeIfAbsent(word, k -> new Node<>(word, new ArrayList<>(), new HashMap<>()));
    }
    cur.terms().add(term);
  }

  public Set<T> findTerms(String text) {
    List<String> wordsInString = getWordsInString(text);
    Set<T> terms = new HashSet<>();
    for (int i = 0; i < wordsInString.size(); i++) {
      Node<T> cur = root;
      for (int j = i; j < wordsInString.size(); j++) {
        Node<T> node = cur.child().get(wordsInString.get(j));
        if (node == null) {
          break;
        }
        terms.addAll(node.terms());
        cur = node;
      }
    }
    return terms;
  }

  private static List<String> getWordsInString(String str) {
    List<String> words = new ArrayList<>();
    if (!str.isBlank()) {
      BreakIterator wordBreakIterator = BreakIterator.getWordInstance(Locale.ENGLISH);
      wordBreakIterator.setText(str);
      int start = wordBreakIterator.first();
      for (int end = wordBreakIterator.next();
          end != BreakIterator.DONE;
          start = end, end = wordBreakIterator.next()) {
        if (wordBreakIterator.getRuleStatus() != BreakIterator.WORD_NONE) {
          words.add(str.substring(start, end));
        }
      }
    }
    return words;
  }

  public interface Term {
    String text();
  }

  private record Node<T>(String word, List<T> terms, Map<String, Node<T>> child) {}
}
