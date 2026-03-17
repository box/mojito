package com.box.l10n.mojito.okapi.filters.extraction;

import static org.assertj.core.groups.Tuple.tuple;

import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class POFilterExtractionTest extends FilterExtractionTestBase {

  @Test
  public void extractSimple() throws UnsupportedAssetFilterTypeException {
    List<AssetExtractorTextUnit> units =
        extract(
            "test.pot",
            """
                #: src/main.py:10
                msgid "Hello"
                msgstr ""

                #: src/main.py:11
                msgid "World"
                msgstr ""
                """,
            null,
            null);

    Assertions.assertThat(units)
        .extracting(AssetExtractorTextUnit::getName, AssetExtractorTextUnit::getSource)
        .containsExactly(tuple("Hello", "Hello"), tuple("World", "World"));
  }

  @Test
  public void extractWithContext() throws UnsupportedAssetFilterTypeException {
    List<AssetExtractorTextUnit> units =
        extract(
            "test.pot",
            """
                msgctxt "menu"
                msgid "File"
                msgstr ""
                """,
            null,
            null);

    Assertions.assertThat(units)
        .extracting(AssetExtractorTextUnit::getName, AssetExtractorTextUnit::getSource)
        .containsExactly(tuple("File --- menu", "File"));
  }

  @Test
  public void extractWithEscapedDoubleQuotes() throws UnsupportedAssetFilterTypeException {
    List<AssetExtractorTextUnit> units =
        extract(
            "test.pot",
            """
                msgid "He said \\"hello\\""
                msgstr ""
                """,
            null,
            null);

    Assertions.assertThat(units)
        .extracting(AssetExtractorTextUnit::getName, AssetExtractorTextUnit::getSource)
        .containsExactly(tuple("He said \"hello\"", "He said \"hello\""));
  }

  @Test
  public void extractWithEscapedSingleQuotes() throws UnsupportedAssetFilterTypeException {
    List<AssetExtractorTextUnit> units =
        extract(
            "test.pot",
            """
                msgid "it\\'s a test"
                msgstr ""
                """,
            null,
            null);

    Assertions.assertThat(units)
        .extracting(AssetExtractorTextUnit::getName, AssetExtractorTextUnit::getSource)
        .containsExactly(tuple("it's a test", "it's a test"));
  }

  @Test
  public void extractWithComment() throws UnsupportedAssetFilterTypeException {
    List<AssetExtractorTextUnit> units =
        extract(
            "test.pot",
            """
                #. Translator comment
                #: src/app.py:5
                msgid "Save"
                msgstr ""
                """,
            null,
            null);

    Assertions.assertThat(units)
        .extracting(
            AssetExtractorTextUnit::getName,
            AssetExtractorTextUnit::getSource,
            AssetExtractorTextUnit::getComments)
        .containsExactly(tuple("Save", "Save", "Translator comment"));
  }

  @Test
  public void extractPlurals() throws UnsupportedAssetFilterTypeException {
    List<AssetExtractorTextUnit> units =
        extract(
            "test.pot",
            """
                #: src/main.py:10
                msgid "item"
                msgid_plural "items"
                msgstr[0] ""
                msgstr[1] ""
                """,
            null,
            null);

    Assertions.assertThat(units)
        .extracting(
            AssetExtractorTextUnit::getName,
            AssetExtractorTextUnit::getSource,
            AssetExtractorTextUnit::getPluralForm,
            AssetExtractorTextUnit::getPluralFormOther)
        .containsExactly(
            tuple("item _zero", "items", "zero", "item _other"),
            tuple("item _one", "item", "one", "item _other"),
            tuple("item _two", "items", "two", "item _other"),
            tuple("item _few", "items", "few", "item _other"),
            tuple("item _many", "items", "many", "item _other"),
            tuple("item _other", "items", "other", "item _other"));
  }

  @Test
  public void extractPluralsWithEscapedQuotes() throws UnsupportedAssetFilterTypeException {
    List<AssetExtractorTextUnit> units =
        extract(
            "test.pot",
            """
                msgid "the \\"item\\""
                msgid_plural "the \\"items\\""
                msgstr[0] ""
                msgstr[1] ""
                """,
            null,
            null);

    Assertions.assertThat(units)
        .extracting(
            AssetExtractorTextUnit::getName,
            AssetExtractorTextUnit::getSource,
            AssetExtractorTextUnit::getPluralForm,
            AssetExtractorTextUnit::getPluralFormOther)
        .containsExactly(
            tuple("the \"item\" _zero", "the \"items\"", "zero", "the \"item\" _other"),
            tuple("the \"item\" _one", "the \"item\"", "one", "the \"item\" _other"),
            tuple("the \"item\" _two", "the \"items\"", "two", "the \"item\" _other"),
            tuple("the \"item\" _few", "the \"items\"", "few", "the \"item\" _other"),
            tuple("the \"item\" _many", "the \"items\"", "many", "the \"item\" _other"),
            tuple("the \"item\" _other", "the \"items\"", "other", "the \"item\" _other"));
  }

  @Test
  public void extractWithEscapedBackslashQuoteNewlineTab()
      throws UnsupportedAssetFilterTypeException {
    // based on:
    // https://gitlab.com/okapiframework/Okapi/-/blob/v1.45.0/okapi/filters/po/src/test/java/net/sf/okapi/filters/po/POFilterTest.java#L241-251
    List<AssetExtractorTextUnit> units =
        extract(
            "test.pot",
            """
                msgid "Text \\\\ and \\" and \\n and \\t"
                msgstr ""
                """,
            null,
            null);

    Assertions.assertThat(units)
        .extracting(AssetExtractorTextUnit::getName, AssetExtractorTextUnit::getSource)
        .containsExactly(tuple("Text \\ and \" and \n and \t", "Text \\ and \" and \n and \t"));
  }

  @Test
  public void extractWithEscapedFormFeedBackspaceCarriageReturn()
      throws UnsupportedAssetFilterTypeException {
    // based on:
    // https://gitlab.com/okapiframework/Okapi/-/blob/v1.45.0/okapi/filters/po/src/test/java/net/sf/okapi/filters/po/POFilterTest.java#L253-263
    List<AssetExtractorTextUnit> units =
        extract(
            "test.pot",
            """
                msgid "Text \\f and \\b and \\rr"
                msgstr ""
                """,
            null,
            null);

    Assertions.assertThat(units)
        .extracting(AssetExtractorTextUnit::getName, AssetExtractorTextUnit::getSource)
        .containsExactly(tuple("Text \f and \b and \rr", "Text \f and \b and \rr"));
  }

  @Test
  public void extractWithEscapeAtStartOfString() throws UnsupportedAssetFilterTypeException {
    // based on:
    // https://gitlab.com/okapiframework/Okapi/-/blob/v1.45.0/okapi/filters/po/src/test/java/net/sf/okapi/filters/po/POFilterTest.java#L265-276
    List<AssetExtractorTextUnit> units =
        extract(
            "test.pot",
            """
                msgid "\\t."
                msgstr ""
                """,
            null,
            null);

    Assertions.assertThat(units)
        .extracting(AssetExtractorTextUnit::getName, AssetExtractorTextUnit::getSource)
        .containsExactly(tuple("\t.", "\t."));
  }

  @Test
  public void extractWithUnescapedQuoteInMiddle() throws UnsupportedAssetFilterTypeException {
    // based on:
    // https://gitlab.com/okapiframework/Okapi/-/blob/v1.45.0/okapi/filters/po/src/test/java/net/sf/okapi/filters/po/POFilterTest.java#L280-290

    // Verifies parity with how Okapi handles malformed escaping in PO files

    // Malformed PO: unescaped quote in middle of msgid string.
    // PO Parser uses lastIndexOf('"') to find the closing delimiter,
    // so given a PO snippet:
    // `msgid "A " and a \"`
    // the parsed string content after unescaping is:
    // `A " and a \`
    // (both an unescaped inner quote and the single trailing backslash are treated as literals)
    List<AssetExtractorTextUnit> units =
        extract(
            "test.pot",
            """
                msgid "A " and a \\"
                msgstr ""
                """,
            null,
            null);

    Assertions.assertThat(units)
        .extracting(AssetExtractorTextUnit::getName, AssetExtractorTextUnit::getSource)
        .containsExactly(tuple("A \" and a \\", "A \" and a \\"));
  }

  @Test
  public void extractWithEscapedAndDanglingBackslash() throws UnsupportedAssetFilterTypeException {
    // based on:
    // https://gitlab.com/okapiframework/Okapi/-/blob/v1.45.0/okapi/filters/po/src/test/java/net/sf/okapi/filters/po/POFilterTest.java#L292-303

    // Verifies parity with how Okapi handles malformed escaping in PO files

    // PO snippet
    // `msgid "\\\"`
    // has 3 backslashes between quotes: escaped backslash (\\)
    // followed by a dangling backslash (\) before closing quote.
    List<AssetExtractorTextUnit> units =
        extract(
            "test.pot",
            """
                msgid "\\\\\\"
                msgstr ""
                """,
            null,
            null);

    // After unescape: \\ → \ (one backslash), dangling \ stays → total 2 backslashes.
    Assertions.assertThat(units)
        .extracting(AssetExtractorTextUnit::getName, AssetExtractorTextUnit::getSource)
        .containsExactly(tuple("\\\\", "\\\\"));
  }
}
