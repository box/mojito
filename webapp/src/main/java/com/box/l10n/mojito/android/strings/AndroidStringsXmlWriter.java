package com.box.l10n.mojito.android.strings;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static com.box.l10n.mojito.android.strings.AndroidStringsXmlHelper.*;

@Component
public class AndroidStringsXmlWriter {

    private static String addEscape(String str) {
        if (str == null) {
            return null;
        }

        return str.replaceAll("\"", "\\\\\"").replaceAll("\n", "\\\\\n");
    }

    private static class Xml {

        private static void addComment(Document document, Node node, String comment) {
            if (comment != null) node.appendChild(document.createComment(comment));
        }

        private static Element createChild(Document document, Node node, String name) {
            Element result = document.createElement(name);
            node.appendChild(result);

            return result;
        }

        private static void addText(Element element, String text) {
            if (text != null) element.setTextContent(text);
        }

        private static void addAttribute(Element element, String name, String value) {
            if (value != null) element.setAttribute(name, value);
        }
    }

    private static void addSingularString(Document document, Node node, Item item) {
        Xml.addComment(document, node, item.getComment());
        Element child = Xml.createChild(document, node, SINGULAR_ELEMENT_NAME);
        Xml.addText(child, addEscape(item.getContent()));
        Xml.addAttribute(child, NAME_ATTRIBUTE_NAME, item.getName());
        Xml.addAttribute(child, ID_ATTRIBUTE_NAME, item.getId());
    }

    private static Node addPluralString(Document document, Node node, Item item) {
        Xml.addComment(document, node, item.getComment());
        Element child = Xml.createChild(document, node, PLURAL_ELEMENT_NAME);
        Xml.addAttribute(child, NAME_ATTRIBUTE_NAME,
                item.getName().substring(0,
                        item.getName().length() - item.getNameSeparator().length() - item.getPluralForm().length()));

        return child;
    }

    private static void addPluralList(Document document, Node node, List<Item> itemList) {
        for (Item item : itemList) {
            Element child = Xml.createChild(document, node, PLURAL_ITEM_ELEMENT_NAME);
            Xml.addText(child, addEscape(item.getContent()));
            Xml.addAttribute(child, QUANTITY_ATTRIBUTE_NAME, item.getPluralForm());
            Xml.addAttribute(child, ID_ATTRIBUTE_NAME, item.getId());
        }
    }

    private static Document toDocument(List<Item> itemList) throws ParserConfigurationException {
        Document document = getDocumentBuilder().newDocument();
        document.setXmlStandalone(true);
        Node node = document.createElement(ROOT_ELEMENT_NAME);
        document.appendChild(node);

        for (int i = 0; i < itemList.size(); i++) {
            if (itemList.get(i).getPluralForm() == null) {
                addSingularString(document, node, itemList.get(i));
            } else {
                node = addPluralString(document, node, itemList.get(i));
                List<Item> pluralList = new ArrayList<>();
                for (; i < itemList.size() && itemList.get(i).getPluralForm() != null; i++) {
                    pluralList.add(itemList.get(i));
                }
                PluralName.sortList(pluralList);
                addPluralList(document, node, pluralList);

                i -= 1;
                node = node.getParentNode();
            }
        }

        return document;
    }

    private static <W extends java.io.Writer> W toWriter(Document document, W writer) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer;
    }

    public String toFile(List<Item> list, String fileName) throws IOException, TransformerException, ParserConfigurationException {
        String result = toText(list);
        new FileWriter(new File(fileName)).write(result);
        return result;
    }

    public String toText(List<Item> list) throws TransformerException, ParserConfigurationException {
        return toWriter(toDocument(list), new StringWriter()).toString();
    }
}
