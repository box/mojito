package com.box.l10n.mojito.android.strings;

import com.google.common.io.Resources;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

import static com.box.l10n.mojito.android.strings.AndroidStringsXmlHelper.*;
import static org.junit.Assert.*;

public class AndroidStringsXmlReaderTest {

    @Test
    public void fromFileTest() throws IOException, SAXException, ParserConfigurationException {
        List<Item> itemList = AndroidStringsXmlReader.fromFile(Resources.getResource("com/box/l10n/mojito/android/strings/copytune_pinboard.xml").getPath());

        int index = 0;
        assertEquals("pinterest", itemList.get(index).getName());
        assertEquals("Pinterest", itemList.get(index).getContent());

        index++;
        assertEquals("atescape", itemList.get(index).getName());
        assertEquals("@", itemList.get(index).getContent());

        index++;
        assertEquals("pins _one", itemList.get(index).getName());
        assertEquals("1 Pin", itemList.get(index).getContent());
        assertEquals("one", itemList.get(index).getPluralForm());
        assertEquals("pins _other", itemList.get(index).getPluralFormOther());

        index++;
        assertEquals("pins _other", itemList.get(index).getName());
        assertEquals("{num_pins} Pins", itemList.get(index).getContent());
        assertEquals("other", itemList.get(index).getPluralForm());
        assertEquals("pins _other", itemList.get(index).getPluralFormOther());
    }

    @Test
    public void fromFilePluralSeparatorTest() throws IOException, SAXException, ParserConfigurationException {
        List<Item> itemList = AndroidStringsXmlReader.fromFile(Resources.getResource("com/box/l10n/mojito/android/strings/copytune_pinboard_plural_separator.xml").getPath(), "_");

        int index = 0;
        assertEquals("pinterest", itemList.get(index).getName());
        assertEquals("Pinterest", itemList.get(index).getContent());

        index++;
        assertEquals("pins_one", itemList.get(index).getName());
        assertEquals("1 Pin", itemList.get(index).getContent());
        assertEquals("one", itemList.get(index).getPluralForm());
        assertEquals("pins_other", itemList.get(index).getPluralFormOther());

        index++;
        assertEquals("pins_other", itemList.get(index).getName());
        assertEquals("{num_pins} Pins", itemList.get(index).getContent());
        assertEquals("other", itemList.get(index).getPluralForm());
        assertEquals("pins_other", itemList.get(index).getPluralFormOther());
    }

    @Test
    public void fromFileKoreanTest() throws IOException, SAXException, ParserConfigurationException {
        List<Item> itemList = AndroidStringsXmlReader.fromFile(Resources.getResource("com/box/l10n/mojito/android/strings/copytune_pinboard_ko-KR.xml").getPath());

        int index = 0;
        assertEquals("pinterest", itemList.get(index).getName());
        assertEquals("P~íñté~rést", itemList.get(index).getContent());

        index++;
        assertEquals("pins _other", itemList.get(index).getName());
        assertEquals("[Plural form OTHER]: {num_pins} Pins", itemList.get(index).getContent());
        assertEquals("other", itemList.get(index).getPluralForm());
        assertEquals("pins _other", itemList.get(index).getPluralFormOther());
    }

    @Test
    public void fromStringTest() throws IOException, SAXException, ParserConfigurationException {
        List<Item> itemList = AndroidStringsXmlReader.fromText(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" 
                + "<resources>\n"
                + "<!--testing regular-->\n"
                + "<string name=\"pinterest\">Pinterest</string>\n"
                + "<!--testing @ escape-->\n"
                + "<string name=\"atescape\">\\@</string>\n"
                + "<!--testing plural-->\n"
                + "<plurals name=\"pins\">\n"
                + "<item quantity=\"one\">1 Pin</item>\n"
                + "<item quantity=\"other\">{num_pins} Pins</item>\n"
                + "</plurals>\n"
                + "</resources>");

        int index = 0;
            assertEquals("pinterest", itemList.get(index).getName());
            assertEquals("Pinterest", itemList.get(index).getContent());

        index++;
            assertEquals("atescape", itemList.get(index).getName());
            assertEquals("@", itemList.get(index).getContent());

        index++;
            assertEquals("pins _one", itemList.get(index).getName());
            assertEquals("1 Pin", itemList.get(index).getContent());
            assertEquals("one", itemList.get(index).getPluralForm());
            assertEquals("pins _other", itemList.get(index).getPluralFormOther());

        index++;
            assertEquals("pins _other", itemList.get(index).getName());
            assertEquals("{num_pins} Pins", itemList.get(index).getContent());
            assertEquals("other", itemList.get(index).getPluralForm());
            assertEquals("pins _other", itemList.get(index).getPluralFormOther());
    }
    
    @Test
    public void fromStringWithIdTest() throws IOException, SAXException, ParserConfigurationException {
        List<Item> itemList = AndroidStringsXmlReader.fromText(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<resources>\n"
                        + "<!--testing regular-->\n"
                        + "<string name=\"pinterest\" tmTextUnitId=\"234\">Pinterest</string>\n"
                        + "<!--testing plural-->\n"
                        + "<plurals name=\"pins\">\n"
                        + "<item quantity=\"one\" tmTextUnitId=\"235\">1 Pin</item>\n"
                        + "<item quantity=\"other\" tmTextUnitId=\"236\">{num_pins} Pins</item>\n"
                        + "</plurals>\n"
                        + "</resources>");

        int index = 0;
        assertEquals("pinterest", itemList.get(index).getName());
        assertEquals("Pinterest", itemList.get(index).getContent());
        assertEquals("234", itemList.get(index).getId());

        index++;
        assertEquals("pins _one", itemList.get(index).getName());
        assertEquals("1 Pin", itemList.get(index).getContent());
        assertEquals("one", itemList.get(index).getPluralForm());
        assertEquals("pins _other", itemList.get(index).getPluralFormOther());
        assertEquals("235", itemList.get(index).getId());

        index++;
        assertEquals("pins _other", itemList.get(index).getName());
        assertEquals("{num_pins} Pins", itemList.get(index).getContent());
        assertEquals("other", itemList.get(index).getPluralForm());
        assertEquals("pins _other", itemList.get(index).getPluralFormOther());
        assertEquals("236", itemList.get(index).getId());
    }

    @Test
    public void fromStringWithSinglePluralFormTest() throws IOException, SAXException, ParserConfigurationException {
        List<Item> itemList = AndroidStringsXmlReader.fromText(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<resources>\n"
                        + "<!--testing plural-->\n"
                        + "<plurals name=\"pins\">\n"
                        + "<item quantity=\"other\">{num_pins} Pins</item>\n"
                        + "</plurals>\n"
                        + "</resources>");

        assertEquals("pins _other", itemList.get(0).getName());
        assertEquals("{num_pins} Pins", itemList.get(0).getContent());
        assertEquals("other", itemList.get(0).getPluralForm());
        assertEquals("pins _other", itemList.get(0).getPluralFormOther());
    }

    @Test
    public void fromStringWithAllPluralFormsTest() throws IOException, SAXException, ParserConfigurationException {
        List<Item> itemList = AndroidStringsXmlReader.fromText(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<resources>\n"
                        + "<!--testing plural-->\n"
                        + "<plurals name=\"pins\">\n"
                        + "<item quantity=\"zero\">0 Pin</item>\n"
                        + "<item quantity=\"one\">1 Pin</item>\n"
                        + "<item quantity=\"two\">2 Pins</item>\n"
                        + "<item quantity=\"few\">3 Pins</item>\n"
                        + "<item quantity=\"many\">4 Pins</item>\n"
                        + "<item quantity=\"other\">{num_pins} Pins</item>\n"
                        + "</plurals>\n"
                        + "</resources>");

        int index = 0;
        assertEquals("pins _zero", itemList.get(index).getName());
        assertEquals("0 Pin", itemList.get(index).getContent());
        assertEquals("zero", itemList.get(index).getPluralForm());
        assertEquals("pins _other", itemList.get(index).getPluralFormOther());

        index++;
        assertEquals("pins _one", itemList.get(index).getName());
        assertEquals("1 Pin", itemList.get(index).getContent());
        assertEquals("one", itemList.get(index).getPluralForm());
        assertEquals("pins _other", itemList.get(index).getPluralFormOther());

        index++;
        assertEquals("pins _two", itemList.get(index).getName());
        assertEquals("2 Pins", itemList.get(index).getContent());
        assertEquals("two", itemList.get(index).getPluralForm());
        assertEquals("pins _other", itemList.get(index).getPluralFormOther());

        index++;
        assertEquals("pins _few", itemList.get(index).getName());
        assertEquals("3 Pins", itemList.get(index).getContent());
        assertEquals("few", itemList.get(index).getPluralForm());
        assertEquals("pins _other", itemList.get(index).getPluralFormOther());

        index++;
        assertEquals("pins _many", itemList.get(index).getName());
        assertEquals("4 Pins", itemList.get(index).getContent());
        assertEquals("many", itemList.get(index).getPluralForm());
        assertEquals("pins _other", itemList.get(index).getPluralFormOther());

        index++;
        assertEquals("pins _other", itemList.get(index).getName());
        assertEquals("{num_pins} Pins", itemList.get(index).getContent());
        assertEquals("other", itemList.get(index).getPluralForm());
        assertEquals("pins _other", itemList.get(index).getPluralFormOther());
    }
}
