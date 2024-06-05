package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class MarkdownLinkIntegrityCheckerTest {

  MarkdownLinkIntegrityChecker checker = new MarkdownLinkIntegrityChecker();

  @Test
  public void testLink() {
    String source = "This is [a link](http://localhost)";
    String target = "c'est [un lien](http://localhost)";
    checker.check(source, target);
  }

  @Test
  public void testBrokenLink() {
    String source = "This is [a link](http://localhost)";
    String target = "c'est [un lien] (http://localhost)";
    assertThrowsExactly(RegexCheckerException.class, () -> checker.check(source, target));
  }

  @Test
  public void testReorder() {
    String source =
        "This is the [first link](http://localhost/1) and this is the [second link](http://localhost/2)";
    String target =
        "C'est le [dieuxieme lien](http://localhost/2) et c'est le [premier lien](http://localhost/1)";
    checker.check(source, target);
  }

  @Test
  public void getPlaceholder() {
    String source =
        "This is [a link](http://localhost/1) and another link [2nd link](http://localhost/2)";
    Set<String> placeholders = checker.getPlaceholders(source);
    Assertions.assertThat(placeholders)
        .containsExactly(
            "[--translatable--](http://localhost/1)", "[--translatable--](http://localhost/2)");
  }
}
