package com.box.l10n.mojito.android.strings;

import org.junit.Test;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.box.l10n.mojito.android.strings.XmlHelper.*;
import static org.junit.Assert.*;

public class XmlWriterTest {

    @Test
    public void addStringTest() throws TransformerException, ParserConfigurationException {
        XmlDocument xmlDocument = new XmlDocument()
                .addSingle("comment", "name", "content");
        
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n" +
                        "<!--comment-->\n" +
                        "<string name=\"name\">content</string>\n" +
                        "</resources>\n",
                new XmlWriter().toText(xmlDocument.getItems()));
    }

    @Test
    public void addQuotesTest() throws TransformerException, ParserConfigurationException {
        XmlDocument xmlDocument = new XmlDocument()
                .addSingle("comment", "name\"", "content\"");

        assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n" +
                        "<!--comment-->\n" +
                        "<string name=\"name&quot;\">content\\\"</string>\n" +
                        "</resources>\n",
                new XmlWriter().toText(xmlDocument.getItems()));
    }
    
    @Test
    public void addPluralStringTest() throws TransformerException, ParserConfigurationException {
        XmlDocument xmlDocument = new XmlDocument()
                .addPlural("comment", "name")
                .addItem(PluralName.one, "singular")
                .addItem(PluralName.other, "plural")
                .getDocument();
        
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n" +
                        "<!--comment-->\n" +
                        "<plurals name=\"name\">\n" +
                        "<item quantity=\"one\">singular</item>\n" +
                        "<item quantity=\"other\">plural</item>\n" +
                        "</plurals>\n" +
                        "</resources>\n",
                new XmlWriter().toText(xmlDocument.getItems()));
    }
    
    @Test
    public void addMultipleStringTest() throws TransformerException, ParserConfigurationException {
        XmlDocument xmlDocument = new XmlDocument()
                .addSingle("comment0", "name0", "content0")
                .addPlural("comment1", "name1")
                .addItem(PluralName.one, "singular1")
                .addItem(PluralName.other, "plural1")
                .getDocument()
                .addSingle("comment2","name2", "content2")
                .addPlural("comment3", "name3")
                .addItem(PluralName.one, "singular3")
                .addItem(PluralName.other, "plural3")
                .getDocument();

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
                new XmlWriter().toText(xmlDocument.getItems()));
    }

    @Test
    public void listIsEmptyTest() throws TransformerException, ParserConfigurationException {
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources/>\n",
                new XmlWriter().toText(new ArrayList<>()));
    }

    @Test
    public void listTest() throws TransformerException, ParserConfigurationException {
        List<Item> itemList = Arrays.asList(
                createItem("comment0", "name0", "content0"),
                createItem("comment1", "name1 _other", "content1_plural", null, "other", "name1 _other", " _"),
                createItem("comment1", "name1 _one", "content1_singular", null, "one", "name1 _other", " _"),
                createItem("comment2", "name2", "content2")
        );

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
                new XmlWriter().toText(itemList));
    }

    @Test
    public void listWithIdTest() throws TransformerException, ParserConfigurationException {
        List<Item> itemList = Arrays.asList(
                createItem("comment0", "name0", "content0", "123"),
                createItem("comment1", "name1 _other", "content1_plural", "124", "other", "name1 _other", " _"),
                createItem("comment1", "name1 _one", "content1_singular", "125", "one", "name1 _other", " _"),
                createItem("comment2", "name2", "content2", "126")
        );

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
                new XmlWriter().toText(itemList));
    }

    @Test
    public void listOtherTest() throws TransformerException, ParserConfigurationException, IOException {
        List<Item> itemList = Arrays.asList(
                createItem("comment1", "name1 _other", "content1", null, "other", "name1 _other", " _")
        );

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><resources>\n" +
                        "<!--comment1-->\n" +
                        "<plurals name=\"name1\">\n" +
                        "<item quantity=\"other\">content1</item>\n" +
                        "</plurals>\n" +
                        "</resources>\n",
                new XmlWriter().toText(itemList));
    }

}
