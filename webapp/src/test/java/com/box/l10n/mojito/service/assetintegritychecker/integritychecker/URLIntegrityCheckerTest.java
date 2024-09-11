package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import org.junit.Test;

public class URLIntegrityCheckerTest {

  URLIntegrityChecker checker = new URLIntegrityChecker();

  @Test
  public void noURL() {
    String source = "No URL";
    String target = "Pas d'URL";
    checker.check(source, target);
  }

  @Test
  public void validUrl() {
    String source = "Visit our site at https://example.org";
    String target = "Visitez notre site à https://example.org";
    checker.check(source, target);
  }

  @Test
  public void validUrlCJK() {
    String source = "Please visit our website at http://example.org for more information.";
    String target = "詳細については、こちらのウェブサイトhttp://example.orgをご覧ください。";
    checker.check(source, target);
  }

  @Test
  public void invalidUrlCJK() {
    String source = "Please visit our website at http://example.org for more information.";
    String target = "詳細については、こちらのウェブサイトhttp://example.org-をご覧ください。";
    checker.check(source, target);
  }

  @Test
  public void validUrlDifferentQuotes() {
    String source = "Visit our site at \"https://example.org\"";
    String target = "Visitez notre site à 'https://example.org'";
    checker.check(source, target);
  }

  @Test
  public void validUrlParam() {
    String source = "Visit our site at \"https://example.org?test=value1\"";
    String target = "Visitez notre site à 'https://example.org?test=value1'";
    checker.check(source, target);
  }

  @Test(expected = URLIntegrityCheckerException.class)
  public void missingUrl() {
    String source = "Visit our site at https://example.org";
    String target = "Visitez notre site à";
    checker.check(source, target);
  }

  @Test(expected = URLIntegrityCheckerException.class)
  public void changedUrl() {
    String source = "The website is https://example.org";
    String target = "Le site web est https://bad.org";
    checker.check(source, target);
  }

  @Test
  public void validHttpUrl() {
    String source = "Visit our site at http://example.org";
    String target = "Visitez notre site à http://example.org";
    checker.check(source, target);
  }

  @Test
  public void validEmail() {
    String source = "Send message to mailto:test@test.org";
    String target = "Envoyer un message a mailto:test@test.org";
    checker.check(source, target);
  }

  @Test(expected = URLIntegrityCheckerException.class)
  public void missingEmail() {
    String source = "Send message to mailto:test@test.org";
    String target = "Envoyer un message a";
    checker.check(source, target);
  }

  @Test(expected = URLIntegrityCheckerException.class)
  public void chanagedEmail() {
    String source = "There is no mailto:test@test.org";
    String target = "Il n'y a pas d'email mailto:tedst@test.org";
    checker.check(source, target);
  }

  @Test(expected = URLIntegrityCheckerException.class)
  public void urlWithDash() {
    String source = "A url https://test.com/manage.";
    String target = "Une url https://test.com/account/manage-.";
    checker.check(source, target);
  }
}
