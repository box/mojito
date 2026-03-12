package com.box.l10n.mojito.service.tm.filetype;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.okapi.ImportTranslationsFromLocalizedAssetStep.StatusForEqualTarget;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TMServiceAndroidTest extends TMServiceFileTypeTestBase {

  static Logger logger = LoggerFactory.getLogger(TMServiceAndroidTest.class);

  /**
   * This test is to test {@link com.box.l10n.mojito.okapi.filters.AndroidXMLEncoder} with option to
   * override encoding of &lt; and &gt;
   *
   * <p>According to Android specification in
   * http://developer.android.com/guide/topics/resources/string-resource.html, <b>bold</b>,
   * <i>italic</i> and <u>underline</u> should be in localized file as-is.
   *
   * @throws Exception
   */
  @Test
  public void testLocalizeAndroidStringsWithSpecialCharactersOldEscaping() throws Exception {

    List<String> filterOptionOldEscaping = Arrays.asList("oldEscaping=true");
    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    String assetContent =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources>\n"
            + "    <string description=\"Example html markup string1\" name=\"welcome1\">Welcome to <b>Android</b>!</string>\n"
            + "    <string description=\"Example html markup string2\" name=\"welcome2\">Welcome to <i>Android</i>!</string>\n"
            + "    <string description=\"Example html markup string3\" name=\"welcome3\">Welcome to <u>Android</u>!</string>\n"
            + "    <string description=\"Example html markup string4\" name=\"welcome4\">Welcome to <annotation font=\"title_emphasis\">Android</annotation>!</string>\n"
            + "    <string name=\"subheader_text1\">\\\'Make sure you\\\'d \\\"escaped\\\" special characters like quotes &amp; ampersands.\\n</string>\n"
            + "    <string name=\"subheader_text2\">\"This'll also work\"</string>\n"
            + "</resources>";
    createAsset(repo, "res/values/strings.xml", assetContent);

    processAsset(repo, assetContent, filterOptionOldEscaping);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("source=[{}]", textUnitDTO.getSource());
    }

    String localizedAsset =
        generateLocalized(assetContent, repoLocale, "en-GB", filterOptionOldEscaping);
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(assetContent, localizedAsset);
  }

  @Test
  public void testLocalizeAndroidStringsWithSpecialCharacters() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    String assetContent =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources>\n"
            + "    <string description=\"Text for when user is drawing.\" name=\"test_annotation\"><annotation tag=\"user\">%1$s</annotation> is drawing…</string>"
            + "    <string description=\"Example html markup string1\" name=\"welcome1\">Welcome to <b>Android</b>!</string>\n"
            + "    <string description=\"Example html markup string2\" name=\"welcome2\">Welcome to <i>Android</i>!</string>\n"
            + "    <string description=\"Example html markup string3\" name=\"welcome3\">Welcome to <u>Android</u>!</string>\n"
            + "    <string description=\"Example html markup string4\" name=\"welcome4\">Welcome to <annotation font=\"title_emphasis\">Android</annotation>!</string>\n"
            + "    <string name=\"subheader_text1\">\\\'Make sure you\\\'d \\\"escaped\\\" special characters like quotes &amp; ampersands.\\n</string>\n"
            + "    <string name=\"subheader_text2\">\"This'll also work\"</string>\n"
            + "    <string name=\"escape_dot\">\\.</string>\n"
            + "    <string name=\"escape_quote\">\\'</string>\n"
            + "    <string name=\"escape_apostrophe\">\\ʼ</string>\n"
            + "    <string name=\"escape_escape\">\\\\</string>\n"
            + "    <string name=\"escape_at_sign\">\\@</string>\n"
            + "    <string name=\"escape_ampersand\">&amp;</string>\n"
            + "    <string name=\"escape_lowerthan\">&lt;</string>\n"
            + "    <string name=\"replace_tab\">a\tb\tc</string>\n"
            + "    <string name=\"remove_line_feed\">\nline1\n   line2\n  line3   </string>\n"
            + "    <string name=\"escape_line_feed2\">\\nline1\\n   line2\\n  line3   </string>\n"
            + "    <string name=\"trim\">    \n a \n   </string>\n"
            + "</resources>";
    createAsset(repo, "res/values/strings.xml", assetContent);

    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("source=[{}]", textUnitDTO.getSource());
    }

    String localizedAsset = generateLocalized(assetContent, repoLocale, "en-GB");
    logger.debug("localized=\n{}", localizedAsset);

    String expected =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources>\n"
            + "    <string description=\"Text for when user is drawing.\" name=\"test_annotation\"><annotation tag=\"user\">%1$s</annotation> is drawing…</string>"
            + "    <string description=\"Example html markup string1\" name=\"welcome1\">Welcome to <b>Android</b>!</string>\n"
            + "    <string description=\"Example html markup string2\" name=\"welcome2\">Welcome to <i>Android</i>!</string>\n"
            + "    <string description=\"Example html markup string3\" name=\"welcome3\">Welcome to <u>Android</u>!</string>\n"
            + "    <string description=\"Example html markup string4\" name=\"welcome4\">Welcome to <annotation font=\"title_emphasis\">Android</annotation>!</string>\n"
            + "    <string name=\"subheader_text1\">\\'Make sure you\\'d \\\"escaped\\\" special characters like quotes &amp; ampersands.\\n</string>\n"
            + "    <string name=\"subheader_text2\">This\\'ll also work</string>\n"
            + "    <string name=\"escape_dot\">.</string>\n"
            + "    <string name=\"escape_quote\">\\'</string>\n"
            + "    <string name=\"escape_apostrophe\">ʼ</string>\n"
            + "    <string name=\"escape_escape\">\\</string>\n"
            + "    <string name=\"escape_at_sign\">@</string>\n"
            + "    <string name=\"escape_ampersand\">&amp;</string>\n"
            + "    <string name=\"escape_lowerthan\">&lt;</string>\n"
            + "    <string name=\"replace_tab\">a b c</string>\n"
            + "    <string name=\"remove_line_feed\">line1 line2 line3</string>\n"
            + "    <string name=\"escape_line_feed2\">\\nline1\\n line2\\n line3</string>\n"
            + "    <string name=\"trim\">a</string>\n"
            + "</resources>";

    assertEquals(expected, localizedAsset);
  }

  /**
   * This test is to test {@link com.box.l10n.mojito.okapi.filters.AndroidXMLEncoder} with escaped
   * HTML and CDATA
   *
   * @throws Exception
   */
  @Test
  public void testLocalizeAndroidStringsWithEscapedHTMLAndCDATA() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    String assetContent =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources>\n"
            + "    <string name=\"welcome_messages0\">Hello, %1$s! You have <b>%2$d new messages</b>.</string>\n"
            + "    <string name=\"welcome_messages1\">Hello, %1$s! You have &lt;b>%2$d new messages&lt;/b>.</string>\n"
            + "    <string name=\"welcome_messages2\">Hello, %1$s! You have <![CDATA[<b>%2$d new messages</b>]]>.</string>\n"
            + "</resources>";
    String expectedLocalizedAsset =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources>\n"
            + "    <string name=\"welcome_messages0\">Hello, %1$s! You have &lt;b>%2$d new messages&lt;/b>.</string>\n"
            + "    <string name=\"welcome_messages1\">Hello, %1$s! You have &lt;b>%2$d new messages&lt;/b>.</string>\n"
            + "    <string name=\"welcome_messages2\">Hello, %1$s! You have &lt;b>%2$d new messages&lt;/b>.</string>\n"
            + "</resources>";
    createAsset(repo, "res/values/strings.xml", assetContent);

    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("source=[{}]", textUnitDTO.getSource());
      assertEquals("Hello, %1$s! You have <b>%2$d new messages</b>.", textUnitDTO.getSource());
    }

    String localizedAsset = generateLocalized(assetContent, repoLocale, "en-GB");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedLocalizedAsset, localizedAsset);
  }

  @Test
  public void testLocalizeAndroidCommentWithTranslatableFalse() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    String assetContent =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources>\n"
            + "    <!-- Comment that should be skipped -->\n"
            + "    <string name=\"to_skip\" translatable=\"false\">Some string to skip</string>\n"
            + "    <!-- Comment for hello string -->\n"
            + "    <string name=\"hello\">Hello</string>\n"
            + "</resources>";

    createAsset(repo, "res/values/strings.xml", assetContent);

    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("comment=[{}]", textUnitDTO.getComment());
    }
    assertEquals(1, textUnitDTOs.size());
    assertEquals("Hello", textUnitDTOs.get(0).getSource());
    assertEquals("Comment for hello string", textUnitDTOs.get(0).getComment());

    String localizedAsset = generateLocalized(assetContent, repoLocale, "en-GB");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(assetContent, localizedAsset);
  }

  @Test
  public void testLocalizeAndroidTranslatableFalse() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    String assetContent =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <resources>
            <string name="do_not_translate" translatable="false">do_not_translate</string>

            <plurals name="plural_do_not_translate" translatable="false">
                <item formatted="false" quantity="one">plural_do_not_translate_one</item>
                <item formatted="false" quantity="other">plural_do_not_translate_other</item>
            </plurals>
        </resources>""";

    createAsset(repo, "res/values/strings.xml", assetContent);

    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.info("name=[{}]", textUnitDTO.getName());
    }
    assertEquals(2, textUnitDTOs.size());
    // TODO translatable on plural strings don't work

  }

  @Test
  public void testLocalizeAndroidUnicodeEscape() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    String assetContent =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <resources>
            <!-- Test Unicode Escapes -->
            <string name="unicode_escape">A string with\\u00A0Unicode Escape</string>
            <string name="unicode_escape2">A string with&#x00a0;Unicode Escape</string>
            <string name="unicode_escape3">A string with&#160;Unicode Escape</string>
            <string name="unicode_escape4">A string with&#xa0;Unicode Escape</string>
        </resources>""";

    createAsset(repo, "res/values/strings.xml", assetContent);

    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("comment=[{}]", textUnitDTO.getComment());
    }
    assertEquals(4, textUnitDTOs.size());
    assertEquals("A string with\u00A0Unicode Escape", textUnitDTOs.get(0).getSource());
    assertEquals("Test Unicode Escapes", textUnitDTOs.get(0).getComment());
    assertEquals("A string with\u00A0Unicode Escape", textUnitDTOs.get(1).getSource());
    assertEquals("A string with\u00A0Unicode Escape", textUnitDTOs.get(2).getSource());
    assertEquals("A string with\u00A0Unicode Escape", textUnitDTOs.get(3).getSource());

    String localizedAsset = generateLocalized(assetContent, repoLocale, "en-GB");
    logger.error("localized=\n{}", localizedAsset);
    String expectedLocalizedAsset =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <resources>
            <!-- Test Unicode Escapes -->
            <string name="unicode_escape">A string with&#x00a0;Unicode Escape</string>
            <string name="unicode_escape2">A string with&#x00a0;Unicode Escape</string>
            <string name="unicode_escape3">A string with&#x00a0;Unicode Escape</string>
            <string name="unicode_escape4">A string with&#x00a0;Unicode Escape</string>
        </resources>""";
    assertEquals(expectedLocalizedAsset, localizedAsset);
  }

  @Test
  public void testLocalizeAndroidStringsPlural() throws Exception {

    Repository repo = createRepository();
    addLocale(repo, "ja-JP");
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    String assetContent =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources>\n"
            + "  <!-- Example of plurals -->\n"
            + "  <plurals name=\"numberOfCollaborators\">\n"
            + "    <item quantity=\"one\">%1$d people</item>\n"
            + "    <item quantity=\"other\">%1$d people</item>\n"
            + "  </plurals>\n"
            + "  <!-- Example2 of plurals -->\n"
            + "  <plurals name=\"numberOfCollaborators2\">\n"
            + "    <item quantity=\"one\">%1$d people</item>\n"
            + "    <item quantity=\"other\">%1$d people</item>\n"
            + "  </plurals>\n"
            + "  <plurals name=\"numberOfCollaborators3\">\n"
            + "    <item quantity=\"other\">%1$d people</item>\n"
            + "  </plurals>\n"
            + "</resources>";
    String expectedLocalizedAsset_jaJP =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources>\n"
            + "  <!-- Example of plurals -->\n"
            + "  <plurals name=\"numberOfCollaborators\">\n"
            + "    <item quantity=\"other\">%1$d people</item>\n"
            + "  </plurals>\n"
            + "  <!-- Example2 of plurals -->\n"
            + "  <plurals name=\"numberOfCollaborators2\">\n"
            + "    <item quantity=\"other\">%1$d people</item>\n"
            + "  </plurals>\n"
            + "  <plurals name=\"numberOfCollaborators3\">\n"
            + "    <item quantity=\"other\">%1$d people</item>\n"
            + "  </plurals>\n"
            + "</resources>";
    String expectedLocalizedAsset_enGB =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources>\n"
            + "  <!-- Example of plurals -->\n"
            + "  <plurals name=\"numberOfCollaborators\">\n"
            + "    <item quantity=\"one\">%1$d people</item>\n"
            + "    <item quantity=\"other\">%1$d people</item>\n"
            + "  </plurals>\n"
            + "  <!-- Example2 of plurals -->\n"
            + "  <plurals name=\"numberOfCollaborators2\">\n"
            + "    <item quantity=\"one\">%1$d people</item>\n"
            + "    <item quantity=\"other\">%1$d people</item>\n"
            + "  </plurals>\n"
            + "  <plurals name=\"numberOfCollaborators3\">\n"
            + "    <item quantity=\"one\">%1$d people</item>\n"
            + "    <item quantity=\"other\">%1$d people</item>\n"
            + "  </plurals>\n"
            + "</resources>";
    createAsset(repo, "res/values/strings.xml", assetContent);

    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);

    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("source [{}]=[{}]", textUnitDTO.getName(), textUnitDTO.getSource());
    }

    String localizedAsset = generateLocalized(assetContent, repoLocale, "ja-JP");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedLocalizedAsset_jaJP, localizedAsset);

    localizedAsset = generateLocalized(assetContent, repoLocale, "en-GB");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedLocalizedAsset_enGB, localizedAsset);
  }

  /**
   * This test is to test AndroidStrings array with empty item
   *
   * @throws Exception
   */
  @Test
  public void testLocalizeAndroidStringsArrayWithEmptyItem() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    String assetContent =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resources>\n"
            + "    <string-array name=\"N_items_failed_to_move\">\n"
            + "        <item/>\n"
            + "        <item>1 item failed to move</item>\n"
            + "        <item>%1$d items failed to move</item>\n"
            + "    </string-array>\n"
            + "</resources>";
    createAsset(repo, "res/values/strings.xml", assetContent);

    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("source=[{}]", textUnitDTO.getSource());
    }

    String localizedAsset = generateLocalized(assetContent, repoLocale, "en-GB");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(assetContent, localizedAsset);
  }

  /**
   * This test is to test AndroidStrings array with no xml version
   *
   * @throws Exception
   */
  @Test
  public void testLocalizeAndroidStringsNoXMLVersion() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    String assetContent =
        "<resources>\n" + "    <string name=\"test\">This is test</string>\n" + "</resources>";
    createAsset(repo, "res/values/strings.xml", assetContent);

    processAsset(repo, assetContent);

    List<TextUnitDTO> textUnitDTOs = searchTextUnits(repo);
    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      logger.debug("source=[{}]", textUnitDTO.getSource());
    }

    String localizedAsset = generateLocalized(assetContent, repoLocale, "en-GB");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(assetContent, localizedAsset);
  }

  /**
   * This test is to test AndroidStrings with REMOVE_UNTRANSLATED inheritance mode
   *
   * @throws Exception
   */
  @Test
  public void testLocalizeAndroidStringsRemoveUntranslatedOldEsaping() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    String assetContent =
        "<resources>\n"
            + "    <string name=\"test\">This is test</string>\n"
            + "    <string name=\"desc\">This is a description</string>\n"
            + "</resources>";
    createAsset(repo, "res/values/strings.xml", assetContent);

    processAsset(repo, assetContent);

    String forImport =
        "<resources>\n" + "    <string name=\"test\">Le test</string>\n" + "</resources>\n";

    importTranslations(repoLocale, forImport, StatusForEqualTarget.TRANSLATION_NEEDED);

    String localizedAsset =
        generateLocalizedRemoveUntranslated(
            assetContent, repoLocale, "en-GB", List.of("postProcessIndent=4"));
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(forImport, localizedAsset);
  }

  /**
   * This test is to test AndroidStrings with REMOVE_UNTRANSLATED inheritance mode with a single
   * item We need a special case in {@link com.box.l10n.mojito.okapi.TranslateStep} to keep the part
   * of the skeleton that contains the begining of the document when skipping the text unit.
   *
   * @throws Exception
   */
  @Test
  public void testLocalizeAndroidStringsRemoveUntranslatedSingleItem() throws Exception {

    Repository repo = createRepository();
    RepositoryLocale repoLocale = addLocale(repo, "en-GB");

    String assetContent =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!-- comment after prolog -->\n\n"
            + "<resources>\n"
            + "    <string name=\"test\">This is test</string>\n"
            + "</resources>";
    createAsset(repo, "res/values/strings.xml", assetContent);

    processAsset(repo, assetContent);

    String expectedLocalized =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!-- comment after prolog -->"
            + "<resources/>\n";

    String localizedAsset = generateLocalizedRemoveUntranslated(assetContent, repoLocale, "en-GB");
    logger.debug("localized=\n{}", localizedAsset);
    assertEquals(expectedLocalized, localizedAsset);
  }
}
