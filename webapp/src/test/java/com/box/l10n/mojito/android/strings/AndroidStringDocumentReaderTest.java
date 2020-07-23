package com.box.l10n.mojito.android.strings;

import com.google.common.io.Resources;
import org.junit.Test;

import java.util.Iterator;

import static com.box.l10n.mojito.android.strings.AndroidPluralQuantity.FEW;
import static com.box.l10n.mojito.android.strings.AndroidPluralQuantity.MANY;
import static com.box.l10n.mojito.android.strings.AndroidPluralQuantity.ONE;
import static com.box.l10n.mojito.android.strings.AndroidPluralQuantity.OTHER;
import static com.box.l10n.mojito.android.strings.AndroidPluralQuantity.TWO;
import static org.assertj.core.api.Assertions.assertThat;

public class AndroidStringDocumentReaderTest {

    AndroidStringDocument document;
    String path;
    String text;

    @Test
    public void testBuildFromFileRegular() throws Exception {

        path = Resources.getResource("com/box/l10n/mojito/android/strings/copytune_pinboard.xml").getPath();
        document = AndroidStringDocumentReader.fromFile(path);

        assertThat(document).isNotNull();
        assertThat(document.getSingulars()).hasSize(2);

        AndroidSingular singular = document.getSingulars().get(0);
        assertThat(singular.getComment()).isEqualTo("testing regular");
        assertThat(singular.getName()).isEqualTo("pinterest");
        assertThat(singular.getContent()).isEqualTo("Pinterest");

        singular = document.getSingulars().get(1);
        assertThat(singular.getComment()).isEqualTo("testing @ escape");
        assertThat(singular.getName()).isEqualTo("atescape");
        assertThat(singular.getContent()).isEqualTo("@");

        assertThat(document.getPlurals()).hasSize(1);

        AndroidPlural plural = document.getPlurals().get(0);
        assertThat(plural.getComment()).isEqualTo("testing plural");
        assertThat(plural.getName()).isEqualTo("pins");
        assertThat(plural.getItems()).hasSize(2);
        assertThat(plural.getItems().get(ONE).getContent()).isEqualTo("1 Pin");
        assertThat(plural.getItems().get(OTHER).getContent()).isEqualTo("{num_pins} Pins");

    }

    @Test
    public void testBuildFromFileKorean() throws Exception {

        path = Resources.getResource("com/box/l10n/mojito/android/strings/copytune_pinboard_ko-KR.xml").getPath();
        document = AndroidStringDocumentReader.fromFile(path);

        assertThat(document).isNotNull();

        assertThat(document.getSingulars()).hasSize(1);
        AndroidSingular singular = document.getSingulars().get(0);
        assertThat(singular.getComment()).isEqualTo("testing regular");
        assertThat(singular.getName()).isEqualTo("pinterest");
        assertThat(singular.getContent()).isEqualTo("pinterest_ko");

        assertThat(document.getPlurals()).hasSize(1);
        AndroidPlural plural = document.getPlurals().get(0);
        assertThat(plural.getComment()).isEqualTo("testing plural");
        assertThat(plural.getName()).isEqualTo("pins");

        assertThat(plural.getItems()).hasSize(1);
        assertThat(plural.getItems().get(OTHER).getContent()).isEqualTo("[Plural form OTHER]: {num_pins} Pins");

    }

    @Test
    public void testBuildFromTextJP() throws Exception {
        text = "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<resources>\n" +
                "  <!--comment-->\n" +
                "  <string name=\"name\">コンテンツ</string>\n" +
                "</resources>";

        document = AndroidStringDocumentReader.fromText(text);

        assertThat(document).isNotNull();

        assertThat(document.getSingulars()).hasSize(1);
        AndroidSingular singular = document.getSingulars().get(0);
        assertThat(singular.getComment()).isEqualTo("comment");
        assertThat(singular.getName()).isEqualTo("name");
        assertThat(singular.getContent()).isEqualTo("コンテンツ");
    }

    @Test
    public void testBuildFromTextWithIds() throws Exception {
        text = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "<!--testing regular-->\n"
                + "<string name=\"pinterest\" tmTextUnitId=\"234\">Pinterest</string>\n"
                + "<!--testing @ escape-->\n"
                + "<string name=\"atescape\">\\@</string>\n"
                + "<!--testing plural-->\n"
                + "<plurals name=\"pins\">\n"
                + "<item quantity=\"one\" tmTextUnitId=\"235\">1 Pin</item>\n"
                + "<item quantity=\"other\" tmTextUnitId=\"236\">{num_pins} Pins</item>\n"
                + "</plurals>\n"
                + "</resources>";

        document = AndroidStringDocumentReader.fromText(text);

        assertThat(document).isNotNull();

        assertThat(document.getSingulars()).hasSize(2);
        AndroidSingular singular = document.getSingulars().get(0);
        assertThat(singular.getComment()).isEqualTo("testing regular");
        assertThat(singular.getName()).isEqualTo("pinterest");
        assertThat(singular.getId()).isEqualTo(234L);
        assertThat(singular.getContent()).isEqualTo("Pinterest");
        singular = document.getSingulars().get(1);
        assertThat(singular.getComment()).isEqualTo("testing @ escape");
        assertThat(singular.getName()).isEqualTo("atescape");
        assertThat(singular.getId()).isNull();
        assertThat(singular.getContent()).isEqualTo("@");

        assertThat(document.getPlurals()).hasSize(1);
        AndroidPlural plural = document.getPlurals().get(0);
        assertThat(plural.getComment()).isEqualTo("testing plural");
        assertThat(plural.getName()).isEqualTo("pins");
        assertThat(plural.getItems()).hasSize(2);
        assertThat(plural.getItems().get(ONE).getId()).isEqualTo(235L);
        assertThat(plural.getItems().get(ONE).getContent()).isEqualTo("1 Pin");
        assertThat(plural.getItems().get(OTHER).getId()).isEqualTo(236L);
        assertThat(plural.getItems().get(OTHER).getContent()).isEqualTo("{num_pins} Pins");
    }

    @Test
    public void testBuildFromTextWithAllPlurals() throws Exception {
        text = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "<!--testing plural-->\n"
                + "<plurals name=\"pins\">\n"
                + "<!--testing plural-->\n"
                + "<item quantity=\"zero\">0 Pin</item>\n"
                + "<item quantity=\"one\">1 Pin</item>\n"
                + "<item quantity=\"two\" tmTextUnitId=\"20\">2 Pins</item>\n"
                + "<item quantity=\"few\">3 Pins</item>\n"
                + "<item quantity=\"many\">4 Pins</item>\n"
                + "<item quantity=\"other\">{num_pins} Pins</item>\n"
                + "</plurals>\n"
                + "</resources>";

        document = AndroidStringDocumentReader.fromText(text);

        assertThat(document).isNotNull();

        assertThat(document.getSingulars()).hasSize(0);

        assertThat(document.getPlurals()).hasSize(1);
        AndroidPlural plural = document.getPlurals().get(0);
        assertThat(plural.getComment()).isEqualTo("testing plural");
        assertThat(plural.getName()).isEqualTo("pins");
        assertThat(plural.getItems()).hasSize(6);
        assertThat(plural.getItems().get(ONE).getId()).isNull();
        assertThat(plural.getItems().get(ONE).getContent()).isEqualTo("1 Pin");
        assertThat(plural.getItems().get(TWO).getId()).isEqualTo(20L);
        assertThat(plural.getItems().get(TWO).getContent()).isEqualTo("2 Pins");
        assertThat(plural.getItems().get(FEW).getId()).isNull();
        assertThat(plural.getItems().get(FEW).getContent()).isEqualTo("3 Pins");
        assertThat(plural.getItems().get(MANY).getId()).isNull();
        assertThat(plural.getItems().get(MANY).getContent()).isEqualTo("4 Pins");
        assertThat(plural.getItems().get(OTHER).getId()).isNull();
        assertThat(plural.getItems().get(OTHER).getContent()).isEqualTo("{num_pins} Pins");
    }

    @Test
    public void testReadPreservesOrdering() throws Exception {

        text = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n" +
                "<!--test comment1-->\n" +
                "<string name=\"string1\" tmTextUnitId=\"10\">string1 @ content</string>\n" +
                "<!--plural1 @ comment-->\n" +
                "<plurals name=\"plural1\">\n" +
                "<item quantity=\"one\">One item</item>\n" +
                "<item quantity=\"other\">{more} items</item>\n" +
                "</plurals>\n" +
                "<!--test comment2-->\n" +
                "<string name=\"string2\" tmTextUnitId=\"11\">string2 @ content</string>\n" +
                "<!--plural2 @ comment-->\n" +
                "<plurals name=\"plural2\">\n" +
                "<item quantity=\"one\">One test</item>\n" +
                "<item quantity=\"two\">Two tests</item>\n" +
                "<item quantity=\"other\">{more} tests</item>\n" +
                "</plurals>\n" +
                "</resources>\n";

        document = AndroidStringDocumentReader.fromText(text);

        AbstractAndroidString string;
        AndroidSingular singular;
        AndroidPlural plural;
        Iterator<AbstractAndroidString> stringsIterator = document.getStrings().iterator();

        // First string is singular
        string = stringsIterator.next();
        assertThat(string.isSingular()).isTrue();
        singular = (AndroidSingular) string;
        assertThat(singular.getId()).isEqualTo(10);
        assertThat(singular.getName()).isEqualTo("string1");
        assertThat(singular.getComment()).isEqualTo("test comment1");
        assertThat(singular.getContent()).isEqualTo("string1 @ content");

        // Second string is plural
        string = stringsIterator.next();
        assertThat(string.isPlural()).isTrue();
        plural = (AndroidPlural) string;
        assertThat(plural.getName()).isEqualTo("plural1");
        assertThat(plural.getComment()).isEqualTo("plural1 @ comment");
        assertThat(plural.getItems()).hasSize(2);

        // 3rd string is singular
        string = stringsIterator.next();
        assertThat(string.isSingular()).isTrue();
        singular = (AndroidSingular) string;
        assertThat(singular.getId()).isEqualTo(11);
        assertThat(singular.getName()).isEqualTo("string2");
        assertThat(singular.getComment()).isEqualTo("test comment2");
        assertThat(singular.getContent()).isEqualTo("string2 @ content");

        // 4th string is plural
        string = stringsIterator.next();
        assertThat(string.isPlural()).isTrue();
        plural = (AndroidPlural) string;
        assertThat(plural.getName()).isEqualTo("plural2");
        assertThat(plural.getComment()).isEqualTo("plural2 @ comment");
        assertThat(plural.getItems()).hasSize(3);
    }
}
