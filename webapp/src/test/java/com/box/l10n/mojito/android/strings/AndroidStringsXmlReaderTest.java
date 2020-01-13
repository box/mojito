package com.box.l10n.mojito.android.strings;

import com.google.common.io.Resources;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class AndroidStringsXmlReaderTest {

    @Test
    public void fromFileTest() throws IOException, SAXException, ParserConfigurationException {
        List<AndroidStringsTextUnit> androidStringsTextUnitList = AndroidStringsXmlReader.fromFile(Resources.getResource("com/box/l10n/mojito/android/strings/copytune_pinboard.xml").getPath());

        int index = 0;
        assertEquals("pinterest", androidStringsTextUnitList.get(index).getName());
        assertEquals("Pinterest", androidStringsTextUnitList.get(index).getContent());

        index++;
        assertEquals("atescape", androidStringsTextUnitList.get(index).getName());
        assertEquals("@", androidStringsTextUnitList.get(index).getContent());

        index++;
        assertEquals("pins _one", androidStringsTextUnitList.get(index).getName());
        assertEquals("1 Pin", androidStringsTextUnitList.get(index).getContent());
        assertEquals("one", androidStringsTextUnitList.get(index).getPluralForm());
        assertEquals("pins _other", androidStringsTextUnitList.get(index).getPluralFormOther());

        index++;
        assertEquals("pins _other", androidStringsTextUnitList.get(index).getName());
        assertEquals("{num_pins} Pins", androidStringsTextUnitList.get(index).getContent());
        assertEquals("other", androidStringsTextUnitList.get(index).getPluralForm());
        assertEquals("pins _other", androidStringsTextUnitList.get(index).getPluralFormOther());
    }

    @Test
    public void fromFilePluralSeparatorTest() throws IOException, SAXException, ParserConfigurationException {
        List<AndroidStringsTextUnit> androidStringsTextUnitList = AndroidStringsXmlReader.fromFile(Resources.getResource("com/box/l10n/mojito/android/strings/copytune_pinboard_plural_separator.xml").getPath(), "_");

        int index = 0;
        assertEquals("pinterest", androidStringsTextUnitList.get(index).getName());
        assertEquals("Pinterest", androidStringsTextUnitList.get(index).getContent());

        index++;
        assertEquals("pins_one", androidStringsTextUnitList.get(index).getName());
        assertEquals("1 Pin", androidStringsTextUnitList.get(index).getContent());
        assertEquals("one", androidStringsTextUnitList.get(index).getPluralForm());
        assertEquals("pins_other", androidStringsTextUnitList.get(index).getPluralFormOther());

        index++;
        assertEquals("pins_other", androidStringsTextUnitList.get(index).getName());
        assertEquals("{num_pins} Pins", androidStringsTextUnitList.get(index).getContent());
        assertEquals("other", androidStringsTextUnitList.get(index).getPluralForm());
        assertEquals("pins_other", androidStringsTextUnitList.get(index).getPluralFormOther());
    }

    @Test
    public void fromFileKoreanTest() throws IOException, SAXException, ParserConfigurationException {
        List<AndroidStringsTextUnit> androidStringsTextUnitList = AndroidStringsXmlReader.fromFile(Resources.getResource("com/box/l10n/mojito/android/strings/copytune_pinboard_ko-KR.xml").getPath());

        int index = 0;
        assertEquals("pinterest", androidStringsTextUnitList.get(index).getName());
        assertEquals("pinterest_ko", androidStringsTextUnitList.get(index).getContent());

        index++;
        assertEquals("pins _other", androidStringsTextUnitList.get(index).getName());
        assertEquals("[Plural form OTHER]: {num_pins} Pins", androidStringsTextUnitList.get(index).getContent());
        assertEquals("other", androidStringsTextUnitList.get(index).getPluralForm());
        assertEquals("pins _other", androidStringsTextUnitList.get(index).getPluralFormOther());
    }

    @Test
    public void fromStringTest() throws IOException, SAXException, ParserConfigurationException {
        List<AndroidStringsTextUnit> androidStringsTextUnitList = AndroidStringsXmlReader.fromText(
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
            assertEquals("pinterest", androidStringsTextUnitList.get(index).getName());
            assertEquals("Pinterest", androidStringsTextUnitList.get(index).getContent());

        index++;
            assertEquals("atescape", androidStringsTextUnitList.get(index).getName());
            assertEquals("@", androidStringsTextUnitList.get(index).getContent());

        index++;
            assertEquals("pins _one", androidStringsTextUnitList.get(index).getName());
            assertEquals("1 Pin", androidStringsTextUnitList.get(index).getContent());
            assertEquals("one", androidStringsTextUnitList.get(index).getPluralForm());
            assertEquals("pins _other", androidStringsTextUnitList.get(index).getPluralFormOther());

        index++;
            assertEquals("pins _other", androidStringsTextUnitList.get(index).getName());
            assertEquals("{num_pins} Pins", androidStringsTextUnitList.get(index).getContent());
            assertEquals("other", androidStringsTextUnitList.get(index).getPluralForm());
            assertEquals("pins _other", androidStringsTextUnitList.get(index).getPluralFormOther());
    }
    
    @Test
    public void fromStringWithIdTest() throws IOException, SAXException, ParserConfigurationException {
        List<AndroidStringsTextUnit> androidStringsTextUnitList = AndroidStringsXmlReader.fromText(
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
        assertEquals("pinterest", androidStringsTextUnitList.get(index).getName());
        assertEquals("Pinterest", androidStringsTextUnitList.get(index).getContent());
        assertEquals("234", androidStringsTextUnitList.get(index).getId());

        index++;
        assertEquals("pins _one", androidStringsTextUnitList.get(index).getName());
        assertEquals("1 Pin", androidStringsTextUnitList.get(index).getContent());
        assertEquals("one", androidStringsTextUnitList.get(index).getPluralForm());
        assertEquals("pins _other", androidStringsTextUnitList.get(index).getPluralFormOther());
        assertEquals("235", androidStringsTextUnitList.get(index).getId());

        index++;
        assertEquals("pins _other", androidStringsTextUnitList.get(index).getName());
        assertEquals("{num_pins} Pins", androidStringsTextUnitList.get(index).getContent());
        assertEquals("other", androidStringsTextUnitList.get(index).getPluralForm());
        assertEquals("pins _other", androidStringsTextUnitList.get(index).getPluralFormOther());
        assertEquals("236", androidStringsTextUnitList.get(index).getId());
    }

    @Test
    public void fromStringWithSinglePluralFormTest() throws IOException, SAXException, ParserConfigurationException {
        List<AndroidStringsTextUnit> androidStringsTextUnitList = AndroidStringsXmlReader.fromText(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<resources>\n"
                        + "<!--testing plural-->\n"
                        + "<plurals name=\"pins\">\n"
                        + "<item quantity=\"other\">{num_pins} Pins</item>\n"
                        + "</plurals>\n"
                        + "</resources>");

        assertEquals("pins _other", androidStringsTextUnitList.get(0).getName());
        assertEquals("{num_pins} Pins", androidStringsTextUnitList.get(0).getContent());
        assertEquals("other", androidStringsTextUnitList.get(0).getPluralForm());
        assertEquals("pins _other", androidStringsTextUnitList.get(0).getPluralFormOther());
    }

    @Test
    public void fromStringWithAllPluralFormsTest() throws IOException, SAXException, ParserConfigurationException {
        List<AndroidStringsTextUnit> androidStringsTextUnitList = AndroidStringsXmlReader.fromText(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<resources>\n"
                        + "<!--testing plural-->\n"
                        + "<plurals name=\"pins\">\n"
                        + "<!--testing plural-->\n"
                        + "<item quantity=\"zero\">0 Pin</item>\n"
                        + "<item quantity=\"one\">1 Pin</item>\n"
                        + "<item quantity=\"two\">2 Pins</item>\n"
                        + "<item quantity=\"few\">3 Pins</item>\n"
                        + "<item quantity=\"many\">4 Pins</item>\n"
                        + "<item quantity=\"other\">{num_pins} Pins</item>\n"
                        + "</plurals>\n"
                        + "</resources>");

        int index = 0;
        assertEquals("pins _zero", androidStringsTextUnitList.get(index).getName());
        assertEquals("0 Pin", androidStringsTextUnitList.get(index).getContent());
        assertEquals("zero", androidStringsTextUnitList.get(index).getPluralForm());
        assertEquals("pins _other", androidStringsTextUnitList.get(index).getPluralFormOther());

        index++;
        assertEquals("pins _one", androidStringsTextUnitList.get(index).getName());
        assertEquals("1 Pin", androidStringsTextUnitList.get(index).getContent());
        assertEquals("one", androidStringsTextUnitList.get(index).getPluralForm());
        assertEquals("pins _other", androidStringsTextUnitList.get(index).getPluralFormOther());

        index++;
        assertEquals("pins _two", androidStringsTextUnitList.get(index).getName());
        assertEquals("2 Pins", androidStringsTextUnitList.get(index).getContent());
        assertEquals("two", androidStringsTextUnitList.get(index).getPluralForm());
        assertEquals("pins _other", androidStringsTextUnitList.get(index).getPluralFormOther());

        index++;
        assertEquals("pins _few", androidStringsTextUnitList.get(index).getName());
        assertEquals("3 Pins", androidStringsTextUnitList.get(index).getContent());
        assertEquals("few", androidStringsTextUnitList.get(index).getPluralForm());
        assertEquals("pins _other", androidStringsTextUnitList.get(index).getPluralFormOther());

        index++;
        assertEquals("pins _many", androidStringsTextUnitList.get(index).getName());
        assertEquals("4 Pins", androidStringsTextUnitList.get(index).getContent());
        assertEquals("many", androidStringsTextUnitList.get(index).getPluralForm());
        assertEquals("pins _other", androidStringsTextUnitList.get(index).getPluralFormOther());

        index++;
        assertEquals("pins _other", androidStringsTextUnitList.get(index).getName());
        assertEquals("{num_pins} Pins", androidStringsTextUnitList.get(index).getContent());
        assertEquals("other", androidStringsTextUnitList.get(index).getPluralForm());
        assertEquals("pins _other", androidStringsTextUnitList.get(index).getPluralFormOther());
    }
}
