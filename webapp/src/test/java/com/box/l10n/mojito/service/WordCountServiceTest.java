package com.box.l10n.mojito.service;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author jaurambault
 */
public class WordCountServiceTest {

  @Test
  public void testGetEnglishWordCount() {
    String string = "";
    WordCountService instance = new WordCountService();
    int expResult = 0;
    int result = instance.getEnglishWordCount(string);
    assertEquals(expResult, result);
  }

  @Test
  public void testGetEnglishWordCountOne() {
    String string = "one";
    WordCountService instance = new WordCountService();
    int expResult = 1;
    int result = instance.getEnglishWordCount(string);
    assertEquals(expResult, result);
  }

  @Test
  public void testGetEnglishWordCountMany() {
    String string = "many words in a sentence. second sentence";
    WordCountService instance = new WordCountService();
    int expResult = 7;
    int result = instance.getEnglishWordCount(string);
    assertEquals(expResult, result);
  }

  /**
   * This implementation doesn't know about placeholder. For now we count them as word. We can do
   * something more clever using an Okapi step later to exclude them but this also make the services
   * more complex.
   */
  @Test
  public void testGetEnglishWordCountPlacehodler() {
    String string = "{1} words";
    WordCountService instance = new WordCountService();
    int expResult = 2; // if placeholders supported it should be 3
    int result = instance.getEnglishWordCount(string);
    assertEquals(expResult, result);
  }

  /**
   * Again complex message format returns a word count too high (9 instead of 3) but this rare
   * occurrence and the offset is acceptable.
   */
  @Test
  public void testGetEnglishWordCountComplexPlacehodler() {
    String string =
        "{numberOfSelectedRepositories, plural, =0{Repositories} =1{1 repository} other{# repositories}}";
    WordCountService instance = new WordCountService();
    int expResult = 9; // if placeholders supported it should be 3
    int result = instance.getEnglishWordCount(string);
    assertEquals(expResult, result);
  }
}
