package com.box.l10n.mojito.android.strings;

import org.junit.Test;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.box.l10n.mojito.android.strings.AndroidStringsXmlHelper.*;
import static org.junit.Assert.*;

public class AndroidStringsXmlWriterTest {

    @Test
    public void addStringTest() throws TransformerException, ParserConfigurationException {
        List<Item> itemList = Collections.singletonList(
                createSingular("name", "content", "comment"));
        
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n" +
                        "<!--comment-->\n" +
                        "<string name=\"name\">content</string>\n" +
                        "</resources>\n",
                AndroidStringsXmlWriter.toText(itemList));
    }

    @Test
    public void addQuotesTest() throws TransformerException, ParserConfigurationException {
        List<Item> itemList = Collections.singletonList(
                createSingular("name\"", "content\"", "comment"));

        assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n" +
                        "<!--comment-->\n" +
                        "<string name=\"name&quot;\">content\\\"</string>\n" +
                        "</resources>\n",
                AndroidStringsXmlWriter.toText(itemList));
    }
    
    @Test
    public void addPluralStringTest() throws TransformerException, ParserConfigurationException {

        List<Item> itemList = Arrays.asList(
                createPlural("name", PluralItem.one, "singular", "comment"),
                createPlural("name", PluralItem.other, "plural", "comment"));

        assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n" +
                        "<!--comment-->\n" +
                        "<plurals name=\"name\">\n" +
                        "<item quantity=\"one\">singular</item>\n" +
                        "<item quantity=\"other\">plural</item>\n" +
                        "</plurals>\n" +
                        "</resources>\n",
                AndroidStringsXmlWriter.toText(itemList));
    }
    
    @Test
    public void addMultipleStringTest() throws TransformerException, ParserConfigurationException {

        List<Item> itemList = Arrays.asList(
                createSingular("name0", "content0", "comment0"),
                createPlural("name1", PluralItem.one, "singular1", "comment1"),
                createPlural("name1", PluralItem.other, "plural1", "comment1"),
                createPlural("name3", PluralItem.one, "singular3", "comment3"),
                createSingular("name2", "content2", "comment2"),
                createPlural("name3", PluralItem.other, "plural3", "comment3"));

        assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n" +
                        "<!--comment0-->\n" +
                        "<string name=\"name0\">content0</string>\n" +
                        "<!--comment1-->\n" +
                        "<plurals name=\"name1\">\n" +
                        "<item quantity=\"one\">singular1</item>\n" +
                        "<item quantity=\"other\">plural1</item>\n" +
                        "</plurals>\n" +
                        "<!--comment2-->\n" +
                        "<string name=\"name2\">content2</string>\n" +
                        "<!--comment3-->\n" +
                        "<plurals name=\"name3\">\n" +
                        "<item quantity=\"one\">singular3</item>\n" +
                        "<item quantity=\"other\">plural3</item>\n" +
                        "</plurals>\n" +
                        "</resources>\n",
                AndroidStringsXmlWriter.toText(itemList));
    }

    @Test
    public void listIsEmptyTest() throws TransformerException, ParserConfigurationException {
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources/>\n",
                AndroidStringsXmlWriter.toText(Collections.emptyList()));
    }

    @Test
    public void listTest() throws TransformerException, ParserConfigurationException {
        List<Item> itemList = Arrays.asList(
                createSingular("name0", "content0", "comment0"),
                createPlural("name1", PluralItem.other, "content1_plural", "comment1"),
                createPlural("name1", PluralItem.one, "content1_singular", "comment1"),
                createSingular("name2", "content2", "comment2"));

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n" +
                        "<!--comment0-->\n" +
                        "<string name=\"name0\">content0</string>\n" +
                        "<!--comment1-->\n" +
                        "<plurals name=\"name1\">\n" +
                        "<item quantity=\"one\">content1_singular</item>\n" +
                        "<item quantity=\"other\">content1_plural</item>\n" +
                        "</plurals>\n" +
                        "<!--comment2-->\n" +
                        "<string name=\"name2\">content2</string>\n" +
                        "</resources>\n",
                AndroidStringsXmlWriter.toText(itemList));
    }

    @Test
    public void listWithIdTest() throws TransformerException, ParserConfigurationException {
        List<Item> itemList = Arrays.asList(
                createSingular("name0", "content0", "comment0","123"),
                createPlural("name1", PluralItem.other, "content1_plural", "comment1","124", DEFAULT_PLURAL_SEPARATOR),
                createPlural("name1", PluralItem.one, "content1_singular", "comment1","125", DEFAULT_PLURAL_SEPARATOR),
                createSingular("name2", "content2", "comment2","126"));

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n" +
                        "<!--comment0-->\n" +
                        "<string name=\"name0\" tmTextUnitId=\"123\">content0</string>\n" +
                        "<!--comment1-->\n" +
                        "<plurals name=\"name1\">\n" +
                        "<item quantity=\"one\" tmTextUnitId=\"125\">content1_singular</item>\n" +
                        "<item quantity=\"other\" tmTextUnitId=\"124\">content1_plural</item>\n" +
                        "</plurals>\n" +
                        "<!--comment2-->\n" +
                        "<string name=\"name2\" tmTextUnitId=\"126\">content2</string>\n" +
                        "</resources>\n",
                AndroidStringsXmlWriter.toText(itemList));
    }

    @Test
    public void listOtherTest() throws TransformerException, ParserConfigurationException, IOException {
        List<Item> itemList = Collections.singletonList(
                createPlural("name1", PluralItem.other, "content1", "comment1"));

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n" +
                        "<!--comment1-->\n" +
                        "<plurals name=\"name1\">\n" +
                        "<item quantity=\"other\">content1</item>\n" +
                        "</plurals>\n" +
                        "</resources>\n",
                AndroidStringsXmlWriter.toText(itemList));
    }

}
