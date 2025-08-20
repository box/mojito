package com.box.l10n.mojito.service.oaitranslate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.box.l10n.mojito.service.oaitranslate.GlossaryService.GlossaryTerm;
import com.box.l10n.mojito.service.oaitranslate.GlossaryService.GlossaryTrie;
import java.util.Set;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlossaryServiceTest {

  static Logger logger = LoggerFactory.getLogger(GlossaryServiceTest.class);

  GlossaryTerm term(long id, String source, boolean caseSensitive) {
    return new GlossaryTerm(id, "TERM", source, null, null, null, false, caseSensitive);
  }

  @Test
  public void matchInsensitiveTerm_anyCasing() {
    GlossaryTrie trie = new GlossaryTrie();
    trie.addTerm(term(1, "Settings", false));

    assertMatches(trie, "settings");
    assertMatches(trie, "SeTtInGs");
    assertMatches(trie, "SETTINGS");
    assertMatches(trie, "Settings");
  }

  @Test
  public void matchSensitiveTerm_onlyExactCasing() {
    GlossaryTrie trie = new GlossaryTrie();
    trie.addTerm(term(2, "Settings", true));

    assertMatches(trie, "Settings");
    assertNoMatch(trie, "settings");
    assertNoMatch(trie, "SETTINGS");
    assertNoMatch(trie, "SeTtInGs");
  }

  @Test
  public void mixOfSensitiveAndInsensitiveTerms() {
    GlossaryTrie trie = new GlossaryTrie();
    trie.addTerm(term(3, "Settings", true));
    trie.addTerm(term(4, "Accounts", false));

    Set<GlossaryTerm> match1 = trie.findTerms("Settings");
    assertTrue(match1.stream().anyMatch(t -> t.text().equals("Settings")));

    Set<GlossaryTerm> match2 = trie.findTerms("ACCOUNTS");
    assertTrue(match2.stream().anyMatch(t -> t.text().equals("Accounts")));

    Set<GlossaryTerm> match3 = trie.findTerms("settings");
    assertFalse(match3.stream().anyMatch(t -> t.text().equals("Settings")));
  }

  void assertMatches(GlossaryTrie trie, String text) {
    Set<GlossaryTerm> results = trie.findTerms(text);
    assertFalse(results.isEmpty(), "Expected match for: '" + text + "'");
  }

  void assertNoMatch(GlossaryTrie trie, String text) {
    Set<GlossaryTerm> results = trie.findTerms(text);
    assertTrue(results.isEmpty(), "Expected no match for: '" + text + "'");
  }
}
