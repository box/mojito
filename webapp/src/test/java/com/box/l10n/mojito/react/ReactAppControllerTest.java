package com.box.l10n.mojito.react;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author jaurambault
 */
public class ReactAppControllerTest {

  @Test
  public void testGetValidLocalFromCookie() {

    String localeCookieValue = "fr-FR";
    ReactAppController instance = new ReactAppController();

    String expResult = "fr-FR";
    String result = instance.getValidLocaleFromCookie(localeCookieValue);
    assertEquals(expResult, result);
  }

  @Test
  public void testGetValidLocalFromCookieInvalidValue() {

    String localeCookieValue = "dsfsazfsdf dsfdsfsfdsf";
    ReactAppController instance = new ReactAppController();

    String expResult = "en";
    String result = instance.getValidLocaleFromCookie(localeCookieValue);
    assertEquals(expResult, result);
  }

  @Test
  public void testGetValidLocalFromCookieNullValue() {

    String localeCookieValue = null;
    ReactAppController instance = new ReactAppController();

    String expResult = "en";
    String result = instance.getValidLocaleFromCookie(localeCookieValue);
    assertEquals(expResult, result);
  }
}
