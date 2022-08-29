package com.box.l10n.mojito.android.strings;

import static com.box.l10n.mojito.android.strings.AndroidStringDocumentWriter.escapeQuotes;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AndroidStringDocumentWriterTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  String result;
  AndroidStringDocumentWriter writer;
  AndroidStringDocument source;
  File tmpFile;

  @Before
  public void setUp() throws Exception {
    tmpFile = tempFolder.newFile("testOutput.xml");
  }

  @Test
  public void testWriteSingulars() throws Exception {

    result =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n"
            + "<!--test comment1-->\n"
            + "<string name=\"string1\" tmTextUnitId=\"10\">string1 @ content</string>\n"
            + "<!--test comment2-->\n"
            + "<string name=\"string2\" tmTextUnitId=\"11\">string2 @ content</string>\n"
            + "</resources>\n";

    source = new AndroidStringDocument();
    source.addSingular(new AndroidSingular(10L, "string1", "string1 @ content", "test comment1"));
    source.addSingular(new AndroidSingular(11L, "string2", "string2 @ content", "test comment2"));

    writer = new AndroidStringDocumentWriter(source);
    assertThat(writer.toText()).isEqualTo(result);

    writer.toFile(tmpFile.getPath());
    assertThat(getTempFileContent()).isEqualTo(result);
  }

  @Test
  public void testWriteEmpty() throws Exception {

    result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources/>\n";

    source = new AndroidStringDocument();

    writer = new AndroidStringDocumentWriter(source);
    assertThat(writer.toText()).isEqualTo(result);

    writer.toFile(tmpFile.getPath());
    assertThat(getTempFileContent()).isEqualTo(result);
  }

  @Test
  public void testWriteJP() throws Exception {

    result =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n"
            + "<!--コンテンツ-->\n"
            + "<string name=\"string1\" tmTextUnitId=\"120\">コンテンツ</string>\n"
            + "</resources>\n";

    source = new AndroidStringDocument();
    source.addSingular(new AndroidSingular(120L, "string1", "コンテンツ", "コンテンツ"));

    writer = new AndroidStringDocumentWriter(source);
    assertThat(writer.toText()).isEqualTo(result);

    writer.toFile(tmpFile.getPath());
    assertThat(getTempFileContent()).isEqualTo(result);
  }

  @Test
  public void testWriteAR() throws Exception {

    result =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n"
            + "<!--تعليق الاختبار-->\n"
            + "<string name=\"string1\" tmTextUnitId=\"120\">سلسلة اختبار</string>\n"
            + "</resources>\n";

    source = new AndroidStringDocument();
    source.addSingular(new AndroidSingular(120L, "string1", "سلسلة اختبار", "تعليق الاختبار"));

    writer = new AndroidStringDocumentWriter(source);
    assertThat(writer.toText()).isEqualTo(result);

    writer.toFile(tmpFile.getPath());
    assertThat(getTempFileContent()).isEqualTo(result);
  }

  @Test
  public void testWriteRU() throws Exception {

    result =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n"
            + "<!--тестовый комментарий-->\n"
            + "<string name=\"string1\" tmTextUnitId=\"120\">тестовая строка</string>\n"
            + "</resources>\n";

    source = new AndroidStringDocument();
    source.addSingular(
        new AndroidSingular(120L, "string1", "тестовая строка", "тестовый комментарий"));

    writer = new AndroidStringDocumentWriter(source);
    assertThat(writer.toText()).isEqualTo(result);

    writer.toFile(tmpFile.getPath());
    assertThat(getTempFileContent()).isEqualTo(result);
  }

  @Test
  public void testWriteChars() throws Exception {

    result =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n"
            + "<string name=\"testname&quot;\" tmTextUnitId=\"120\">test @ \\content\\\"</string>\n"
            + "</resources>\n";

    source = new AndroidStringDocument();
    source.addSingular(new AndroidSingular(120L, "testname\"", "test @ \\content\"", ""));

    writer = new AndroidStringDocumentWriter(source);
    assertThat(writer.toText()).isEqualTo(result);

    writer.toFile(tmpFile.getPath());
    assertThat(getTempFileContent()).isEqualTo(result);
  }

  @Test
  public void testWritePlurals() throws Exception {

    result =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n"
            + "<!--test comment1-->\n"
            + "<string name=\"string1\" tmTextUnitId=\"10\">string1 @ content</string>\n"
            + "<!--plural1 @ comment-->\n"
            + "<plurals name=\"plural1\">\n"
            + "<item quantity=\"one\" tmTextUnitId=\"20\">One item</item>\n"
            + "<item quantity=\"other\" tmTextUnitId=\"21\">{more} items</item>\n"
            + "</plurals>\n"
            + "<!--plural2 @ comment-->\n"
            + "<plurals name=\"plural2\">\n"
            + "<item quantity=\"one\" tmTextUnitId=\"22\">One test</item>\n"
            + "<item quantity=\"other\" tmTextUnitId=\"23\">{more} tests</item>\n"
            + "</plurals>\n"
            + "</resources>\n";

    source = new AndroidStringDocument();
    source.addSingular(new AndroidSingular(10L, "string1", "string1 @ content", "test comment1"));
    List<AndroidPluralItem> plurals1 =
        ImmutableList.of(
            new AndroidPluralItem(20L, AndroidPluralQuantity.ONE, "One item"),
            new AndroidPluralItem(21L, AndroidPluralQuantity.OTHER, "{more} items"));

    List<AndroidPluralItem> plurals2 =
        ImmutableList.of(
            new AndroidPluralItem(22L, AndroidPluralQuantity.ONE, "One test"),
            new AndroidPluralItem(23L, AndroidPluralQuantity.OTHER, "{more} tests"));

    source.addPlural(new AndroidPlural("plural1", "plural1 @ comment", plurals1));
    source.addPlural(new AndroidPlural("plural2", "plural2 @ comment", plurals2));

    writer = new AndroidStringDocumentWriter(source);
    assertThat(writer.toText()).isEqualTo(result);

    writer.toFile(tmpFile.getPath());
    assertThat(getTempFileContent()).isEqualTo(result);
  }

  @Test
  public void testWritePreservesOrdering() throws Exception {

    result =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n"
            + "<!--test comment1-->\n"
            + "<string name=\"string1\" tmTextUnitId=\"10\">string1 @ content</string>\n"
            + "<!--plural1 @ comment-->\n"
            + "<plurals name=\"plural1\">\n"
            + "<item quantity=\"one\" tmTextUnitId=\"20\">One item</item>\n"
            + "<item quantity=\"other\" tmTextUnitId=\"21\">{more} items</item>\n"
            + "</plurals>\n"
            + "<!--test comment2-->\n"
            + "<string name=\"string2\" tmTextUnitId=\"11\">string2 @ content</string>\n"
            + "<!--plural2 @ comment-->\n"
            + "<plurals name=\"plural2\">\n"
            + "<item quantity=\"one\" tmTextUnitId=\"22\">One test</item>\n"
            + "<item quantity=\"other\" tmTextUnitId=\"23\">{more} tests</item>\n"
            + "</plurals>\n"
            + "</resources>\n";

    source = new AndroidStringDocument();
    List<AndroidPluralItem> plurals1 =
        ImmutableList.of(
            new AndroidPluralItem(20L, AndroidPluralQuantity.ONE, "One item"),
            new AndroidPluralItem(21L, AndroidPluralQuantity.OTHER, "{more} items"));

    List<AndroidPluralItem> plurals2 =
        ImmutableList.of(
            new AndroidPluralItem(22L, AndroidPluralQuantity.ONE, "One test"),
            new AndroidPluralItem(23L, AndroidPluralQuantity.OTHER, "{more} tests"));

    source.addSingular(new AndroidSingular(10L, "string1", "string1 @ content", "test comment1"));
    source.addPlural(new AndroidPlural("plural1", "plural1 @ comment", plurals1));

    source.addSingular(new AndroidSingular(11L, "string2", "string2 @ content", "test comment2"));
    source.addPlural(new AndroidPlural("plural2", "plural2 @ comment", plurals2));

    writer = new AndroidStringDocumentWriter(source);
    assertThat(writer.toText()).isEqualTo(result);

    writer.toFile(tmpFile.getPath());
    assertThat(getTempFileContent()).isEqualTo(result);
  }

  @Test
  public void testEscapeQuotes() {
    assertThat(escapeQuotes(null)).isEqualTo("");
    assertThat(escapeQuotes("")).isEqualTo("");
    assertThat(escapeQuotes("String")).isEqualTo("String");
    assertThat(escapeQuotes("second\\\"String")).isEqualTo("second\\\\\"String");
    assertThat(escapeQuotes("third\nString")).isEqualTo("third\\nString");
    assertThat(escapeQuotes("fourth\ntest\\\"String\\\""))
        .isEqualTo("fourth\\ntest\\\\\"String\\\\\"");
  }

  private String getTempFileContent() throws IOException {
    return FileUtils.readFileToString(tmpFile, StandardCharsets.UTF_8);
  }
}
