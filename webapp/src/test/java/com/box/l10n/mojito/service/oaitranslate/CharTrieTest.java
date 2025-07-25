package com.box.l10n.mojito.service.oaitranslate;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

public class CharTrieTest {

  record TestTerm(String text) implements CharTrie.Term {}

  @Test
  void testFindHello() {
    CharTrie<TestTerm> trie = new CharTrie<>(true);
    TestTerm hello = new TestTerm("hello");
    trie.addTerm(hello);

    Set<TestTerm> matches = trie.findTerms("hello");
    assertTrue(matches.contains(hello));
  }

  @Test
  void testFindWorld() {
    CharTrie<TestTerm> trie = new CharTrie<>(true);
    TestTerm world = new TestTerm("world");
    trie.addTerm(world);

    Set<TestTerm> matches = trie.findTerms("world");
    assertTrue(matches.contains(world));
  }

  @Test
  void testFindBothWords() {
    CharTrie<TestTerm> trie = new CharTrie<>(true);
    TestTerm hello = new TestTerm("hello");
    TestTerm world = new TestTerm("world");
    trie.addTerm(hello);
    trie.addTerm(world);

    Set<TestTerm> matches = trie.findTerms("hello world");
    assertTrue(matches.contains(hello));
    assertTrue(matches.contains(world));
  }

  @Test
  void testFindHelloWorldPhrase() {
    CharTrie<TestTerm> trie = new CharTrie<>(true);
    TestTerm helloWorld = new TestTerm("hello world");
    trie.addTerm(helloWorld);

    Set<TestTerm> matches = trie.findTerms("say hello world now");
    assertTrue(matches.contains(helloWorld));
  }

  @Test
  void testNoMatchForGoodbye() {
    CharTrie<TestTerm> trie = new CharTrie<>(true);
    trie.addTerm(new TestTerm("hello"));

    Set<TestTerm> matches = trie.findTerms("goodbye");
    assertTrue(matches.isEmpty());
  }

  @Test
  void testOverlap() {
    CharTrie<TestTerm> trie = new CharTrie<>(true);
    TestTerm hello = new TestTerm("hello");
    TestTerm helloWorld = new TestTerm("hello world");
    trie.addTerm(hello);
    trie.addTerm(helloWorld);

    Set<TestTerm> matches = trie.findTerms("hello world");
    assertTrue(matches.contains(hello));
    assertTrue(matches.contains(helloWorld));
  }

  @Test
  void testDuplicatedTerm() {
    CharTrie<TestTerm> trie = new CharTrie<>(true);
    TestTerm hello1 = new TestTerm("hello");
    TestTerm hello2 = new TestTerm("hello");
    trie.addTerm(hello1);
    trie.addTerm(hello2);

    Set<TestTerm> matches = trie.findTerms("hello world");
    assertTrue(matches.contains(hello1));
    assertTrue(matches.contains(hello2));
  }

  @Test
  void testCaseInsensitiveMatch() {
    CharTrie<TestTerm> trie = new CharTrie<>(false);
    TestTerm hello = new TestTerm("hello");
    trie.addTerm(hello);

    assertTrue(trie.findTerms("HELLO").contains(hello));
    assertTrue(trie.findTerms("HeLlO").contains(hello));
    assertTrue(trie.findTerms("hello").contains(hello));
  }

  @Test
  void testCaseInsensitiveMatchWithPhrase() {
    CharTrie<TestTerm> trie = new CharTrie<>(false);
    TestTerm phrase = new TestTerm("hello world");
    trie.addTerm(phrase);

    Set<TestTerm> matches = trie.findTerms("Say HELLO WORLD now");
    assertTrue(matches.contains(phrase));
  }

  @Test
  void testCaseInsensitiveNoMatch() {
    CharTrie<TestTerm> trie = new CharTrie<>(false);
    trie.addTerm(new TestTerm("hello"));

    Set<TestTerm> matches = trie.findTerms("goodbye");
    assertTrue(matches.isEmpty());
  }
}
