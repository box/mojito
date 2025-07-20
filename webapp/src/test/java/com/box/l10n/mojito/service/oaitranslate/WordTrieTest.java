package com.box.l10n.mojito.service.oaitranslate;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

public class WordTrieTest {

  record TestTerm(String text) implements WordTrie.Term {}

  @Test
  void testFindHello() {
    WordTrie<TestTerm> trie = new WordTrie<>();
    TestTerm hello = new TestTerm("hello");
    trie.addTerm(hello);

    Set<TestTerm> matches = trie.findTerms("hello");
    assertTrue(matches.contains(hello));
  }

  @Test
  void testFindWorld() {
    WordTrie<TestTerm> trie = new WordTrie<>();
    TestTerm world = new TestTerm("world");
    trie.addTerm(world);

    Set<TestTerm> matches = trie.findTerms("world");
    assertTrue(matches.contains(world));
  }

  @Test
  void testFindBothWords() {
    WordTrie<TestTerm> trie = new WordTrie<>();
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
    WordTrie<TestTerm> trie = new WordTrie<>();
    TestTerm helloWorld = new TestTerm("hello world");
    trie.addTerm(helloWorld);

    Set<TestTerm> matches = trie.findTerms("say hello world now");
    assertTrue(matches.contains(helloWorld));
  }

  @Test
  void testNoMatchForGoodbye() {
    WordTrie<TestTerm> trie = new WordTrie<>();
    trie.addTerm(new TestTerm("hello"));

    Set<TestTerm> matches = trie.findTerms("goodbye");
    assertTrue(matches.isEmpty());
  }

  @Test
  void testOverlap() {
    WordTrie<TestTerm> trie = new WordTrie<>();
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
    WordTrie<TestTerm> trie = new WordTrie<>();
    TestTerm hello1 = new TestTerm("hello");
    TestTerm hello2 = new TestTerm("hello");
    trie.addTerm(hello1);
    trie.addTerm(hello2);

    Set<TestTerm> matches = trie.findTerms("hello world");
    assertTrue(matches.contains(hello1));
    assertTrue(matches.contains(hello2));
  }
}
