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
import java.io.FileWriter;
import java.io.File;
import java.io.StringWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.box.l10n.mojito.android.strings.AndroidStringsXmlHelper.*;

@Component
public class AndroidStringsXmlWriter {

    private static String addEscape(String str) {
        if (str == null) {
            return null;
        }

        return str.replaceAll("\"", "\\\\\"").replaceAll("\n", "\\\\\n");
    }

    private static String getPluralName(Item item) {
        if (item.getPluralForm() != null) {
            return item.getName().substring(0, item.getName().length() - item.getPluralForm().length());
        }
        return null;
    }

    private static void addSingularString(Document document, Node node, Item item) {
        addComment(document, node, item.getComment());
        Element child = createChild(document, node, SINGULAR_ELEMENT_NAME);
        addText(child, addEscape(item.getContent()));
        addAttribute(child, NAME_ATTRIBUTE_NAME, item.getName());
        addAttribute(child, ID_ATTRIBUTE_NAME, item.getId());
    }

    private static Node addPluralString(Document document, Node node, Item item, String pluralNameSeparator) {
        addComment(document, node, item.getComment());
        Element child = createChild(document, node, PLURAL_ELEMENT_NAME);
        addAttribute(child, NAME_ATTRIBUTE_NAME,
                item.getName().substring(0,
                        item.getName().length() - pluralNameSeparator.length() - item.getPluralForm().length()));

        return child;
    }

    private static void addPluralList(Document document, Node node, List<Item> itemList) {
        for (Item item : itemList) {
            Element child = createChild(document, node, PLURAL_ITEM_ELEMENT_NAME);
            addText(child, addEscape(item.getContent()));
            addAttribute(child, QUANTITY_ATTRIBUTE_NAME, item.getPluralForm());
            addAttribute(child, ID_ATTRIBUTE_NAME, item.getId());
        }
    }

    private static Document toDocument(List<Item> itemList, String pluralNameSeparator) throws ParserConfigurationException {
        Document document = getDocumentBuilder().newDocument();
        document.setXmlStandalone(true);
        Node node = document.createElement(ROOT_ELEMENT_NAME);
        document.appendChild(node);

        List<Item> sortedItemList = itemList.stream().sorted(Comparator.comparing(Item::getName)).collect(Collectors.toList());
        for (int i = 0; i < sortedItemList.size(); i++) {
            if (sortedItemList.get(i).getPluralForm() == null) {
                addSingularString(document, node, sortedItemList.get(i));
            } else {
                String pluralName = getPluralName(sortedItemList.get(i));
                node = addPluralString(document, node, sortedItemList.get(i), pluralNameSeparator);
                List<Item> pluralList = new ArrayList<>();
                for (; i < sortedItemList.size() && Objects.equals(pluralName, getPluralName(sortedItemList.get(i))); i++) {
                    pluralList.add(sortedItemList.get(i));
                }
                pluralList.sort(PluralItem::compare);
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

    public static void toFile(List<Item> list, String fileName, String pluralNameSeparator) throws IOException, TransformerException, ParserConfigurationException {
        String text = toText(list, pluralNameSeparator);
        new FileWriter(new File(fileName)).write(text);
    }

    public static void toFile(List<Item> list, String fileName) throws IOException, TransformerException, ParserConfigurationException {
        String text = toText(list, DEFAULT_PLURAL_SEPARATOR);
        new FileWriter(new File(fileName)).write(text);
    }

    public static String toText(List<Item> list, String pluralNameSeparator) throws TransformerException, ParserConfigurationException {
        return toWriter(toDocument(list, pluralNameSeparator), new StringWriter()).toString();
    }

    public static String toText(List<Item> list) throws TransformerException, ParserConfigurationException {
        return toWriter(toDocument(list, DEFAULT_PLURAL_SEPARATOR), new StringWriter()).toString();
    }
}
