package com.box.l10n.mojito.service.thirdparty.smartling.glossary;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlossaryCache implements Serializable {

  private int maxNGramSize = 1;

  private Map<String, List<GlossaryTerm>> cache = new HashMap<>();

  public int getMaxNGramSize() {
    return maxNGramSize;
  }

  public void setMaxNGramSize(int maxNGramSize) {
    this.maxNGramSize = maxNGramSize;
  }

  public Map<String, List<GlossaryTerm>> getCache() {
    return cache;
  }

  public void setCache(Map<String, List<GlossaryTerm>> cache) {
    this.cache = cache;
  }

  public void add(String key, GlossaryTerm glossaryTerm) {
    if (cache.containsKey(key)) {
      cache.get(key).add(glossaryTerm);
    } else {
      List<GlossaryTerm> terms = new ArrayList<>();
      terms.add(glossaryTerm);
      cache.put(key, terms);
    }
  }

  /**
   * Get the list of glossary terms for a given .
   *
   * @param key
   * @return
   */
  public List<GlossaryTerm> get(String key) {
    return cache.getOrDefault(key, new ArrayList<>());
  }

  public int size() {
    return cache.size();
  }
}
