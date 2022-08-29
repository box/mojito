package com.box.l10n.mojito.okapi.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;

/** @author jeanaurambault */
public class POFilterTest {

  @Test
  public void loadMsgIDPluralFromParent() {
    POFilter poFilter = new POFilter();
    poFilter.loadMsgIDPluralFromParent();
    assertNull(poFilter.msgIDPlural);
  }

  @Test
  public void loadMsgIDFromParent() {
    POFilter poFilter = new POFilter();
    poFilter.loadMsgIDFromParent();
    assertNull(poFilter.msgID);
  }

  @Test
  public void getUsagesFromSkeleton() {
    String skeleton =
        "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "#: core/logic/week_in_review_email_logic.py:72\n"
            + "msgid \"repin\"\n"
            + "msgid_plural \"repins\"\n"
            + "msgstr[0] \"repin-ru\"\n"
            + "msgstr[1] \"repins-ru-1\"\n"
            + "msgstr[2] \"repins-ru-2\"\n";

    POFilter poFilter = new POFilter();
    List<String> usages = new ArrayList<>(poFilter.getUsagesFromSkeleton(skeleton));

    assertEquals("core/logic/week_in_review_email_logic.py:49", usages.get(0));
    assertEquals("core/logic/week_in_review_email_logic.py:72", usages.get(1));
  }

  @Test
  public void getUsagesFromSkeletonNone() {
    String skeleton =
        "#. Comments\n"
            + "msgid \"repin\"\n"
            + "msgid_plural \"repins\"\n"
            + "msgstr[0] \"repin-ru\"\n"
            + "msgstr[1] \"repins-ru-1\"\n"
            + "msgstr[2] \"repins-ru-2\"\n";

    POFilter poFilter = new POFilter();
    List<String> usages = new ArrayList<>(poFilter.getUsagesFromSkeleton(skeleton));
    assertEquals(0, usages.size());
  }

  @Test
  public void removeUntranslatedNone() {
    String poFile =
        "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "#: core/logic/week_in_review_email_logic.py:72\n"
            + "msgid \"repin\"\n"
            + "msgid_plural \"repins\"\n"
            + "msgstr[0] \"repin-ru\"\n"
            + "msgstr[1] \"repins-ru-1\"\n"
            + "msgstr[2] \"repins-ru-2\"";
    assertEquals(poFile, POFilter.removeUntranslated(poFile));
  }

  @Test
  public void removeUntranslatedPlural() {
    String poFile =
        "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "#: core/logic/week_in_review_email_logic.py:72\n"
            + "msgid \"repin\"\n"
            + "msgid_plural \"repins\"\n"
            + "msgstr[0] \""
            + RemoveUntranslatedStrategy.UNTRANSLATED_PLACEHOLDER
            + "\"\n"
            + "msgstr[1] \""
            + RemoveUntranslatedStrategy.UNTRANSLATED_PLACEHOLDER
            + "\"\n"
            + "msgstr[2] \""
            + RemoveUntranslatedStrategy.UNTRANSLATED_PLACEHOLDER
            + "\"";

    assertEquals("", POFilter.removeUntranslated(poFile));
  }

  @Test
  public void removeUntranslatedMixed() {
    String poFile =
        "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:39\n"
            + "msgid \"Hello\"\n"
            + "msgstr \""
            + RemoveUntranslatedStrategy.UNTRANSLATED_PLACEHOLDER
            + "\"\n"
            + "\n"
            + "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:39\n"
            + "msgid \"Bye\"\n"
            + "msgstr \"Au revoir\"\n"
            + "\n"
            + "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "msgid \"repin\"\n"
            + "msgid_plural \"repins\"\n"
            + "msgstr[0] \""
            + RemoveUntranslatedStrategy.UNTRANSLATED_PLACEHOLDER
            + "\"\n"
            + "msgstr[1] \""
            + RemoveUntranslatedStrategy.UNTRANSLATED_PLACEHOLDER
            + "\"\n"
            + "msgstr[2] \""
            + RemoveUntranslatedStrategy.UNTRANSLATED_PLACEHOLDER
            + "\"";

    assertEquals(
        "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:39\n"
            + "msgid \"Bye\"\n"
            + "msgstr \"Au revoir\"\n",
        POFilter.removeUntranslated(poFile));
  }

  @Test
  public void removeUntranslatedMixedNoLine() {
    String poFile =
        "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:39\n"
            + "msgid \"Hello\"\n"
            + "msgstr \""
            + RemoveUntranslatedStrategy.UNTRANSLATED_PLACEHOLDER
            + "\"\n"
            + "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:39\n"
            + "msgid \"Bye\"\n"
            + "msgstr \"Au revoir\"\n"
            + "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:49\n"
            + "msgid \"repin\"\n"
            + "msgid_plural \"repins\"\n"
            + "msgstr[0] \""
            + RemoveUntranslatedStrategy.UNTRANSLATED_PLACEHOLDER
            + "\"\n"
            + "msgstr[1] \""
            + RemoveUntranslatedStrategy.UNTRANSLATED_PLACEHOLDER
            + "\"\n"
            + "msgstr[2] \""
            + RemoveUntranslatedStrategy.UNTRANSLATED_PLACEHOLDER
            + "\"";

    assertEquals(
        "#. Comments\n"
            + "#: core/logic/week_in_review_email_logic.py:39\n"
            + "msgid \"Bye\"\n"
            + "msgstr \"Au revoir\"",
        POFilter.removeUntranslated(poFile));
  }

  @Test
  public void removeUntranslatedEOL() {
    Stream.of("", "\n", "#. Comments", "#. Comments\n")
        .forEach(s -> assertEquals(s, POFilter.removeUntranslated(s)));
  }
}
