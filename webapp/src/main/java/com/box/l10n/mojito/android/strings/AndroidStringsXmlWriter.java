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
        String result = null;
        if (str != null) {
            result = str.replaceAll("\"", "\\\\\"").replaceAll("\n", "\\\\\n");
        }

        return result;
    }

    private static String getPluralName(AndroidStringsTextUnit androidStringsTextUnit) {
        String result = null;
        if (androidStringsTextUnit.getPluralForm() != null) {
            result = androidStringsTextUnit.getName().substring(0, androidStringsTextUnit.getName().length() - androidStringsTextUnit.getPluralForm().length());
        }

        return result;
    }

    private static void addSingularString(Document document, Node node, AndroidStringsTextUnit androidStringsTextUnit) {
        addComment(document, node, androidStringsTextUnit.getComment());
        Element child = createChild(document, node, SINGULAR_ELEMENT_NAME);
        addText(child, addEscape(androidStringsTextUnit.getContent()));
        addAttribute(child, NAME_ATTRIBUTE_NAME, androidStringsTextUnit.getName());
        addAttribute(child, ID_ATTRIBUTE_NAME, androidStringsTextUnit.getId());
    }

    private static Node addPluralString(Document document, Node node, AndroidStringsTextUnit androidStringsTextUnit, String pluralNameSeparator) {
        addComment(document, node, androidStringsTextUnit.getComment());
        Element child = createChild(document, node, PLURAL_ELEMENT_NAME);
        addAttribute(child, NAME_ATTRIBUTE_NAME,
                androidStringsTextUnit.getName().substring(0,
                        androidStringsTextUnit.getName().length() - pluralNameSeparator.length() - androidStringsTextUnit.getPluralForm().length()));

        return child;
    }

    private static void addPluralList(Document document, Node node, List<AndroidStringsTextUnit> androidStringsTextUnitList) {
        for (AndroidStringsTextUnit androidStringsTextUnit : androidStringsTextUnitList) {
            Element child = createChild(document, node, PLURAL_ITEM_ELEMENT_NAME);
            addText(child, addEscape(androidStringsTextUnit.getContent()));
            addAttribute(child, QUANTITY_ATTRIBUTE_NAME, androidStringsTextUnit.getPluralForm());
            addAttribute(child, ID_ATTRIBUTE_NAME, androidStringsTextUnit.getId());
        }
    }

    private static Document toDocument(List<AndroidStringsTextUnit> androidStringsTextUnitList, String pluralNameSeparator) throws ParserConfigurationException {
        Document document = getDocumentBuilder().newDocument();
        document.setXmlStandalone(true);
        Node node = document.createElement(ROOT_ELEMENT_NAME);
        document.appendChild(node);

        List<AndroidStringsTextUnit> sortedAndroidStringsTextUnitList = androidStringsTextUnitList.stream().sorted(Comparator.comparing(AndroidStringsTextUnit::getName)).collect(Collectors.toList());
        for (int i = 0; i < sortedAndroidStringsTextUnitList.size(); i++) {
            if (sortedAndroidStringsTextUnitList.get(i).getPluralForm() == null) {
                addSingularString(document, node, sortedAndroidStringsTextUnitList.get(i));
            } else {
                String pluralName = getPluralName(sortedAndroidStringsTextUnitList.get(i));
                node = addPluralString(document, node, sortedAndroidStringsTextUnitList.get(i), pluralNameSeparator);
                List<AndroidStringsTextUnit> pluralList = new ArrayList<>();
                for (; i < sortedAndroidStringsTextUnitList.size() && Objects.equals(pluralName, getPluralName(sortedAndroidStringsTextUnitList.get(i))); i++) {
                    pluralList.add(sortedAndroidStringsTextUnitList.get(i));
                }
                pluralList.sort(Comparator.comparingInt(item -> PluralItem.valueOf(item.getPluralForm()).ordinal()));
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

    public static void toFile(List<AndroidStringsTextUnit> list, String fileName, String pluralNameSeparator) throws IOException, TransformerException, ParserConfigurationException {
        String text = toText(list, pluralNameSeparator);
        new FileWriter(new File(fileName)).write(text);
    }

    public static void toFile(List<AndroidStringsTextUnit> list, String fileName) throws IOException, TransformerException, ParserConfigurationException {
        String text = toText(list, DEFAULT_PLURAL_SEPARATOR);
        new FileWriter(new File(fileName)).write(text);
    }

    public static String toText(List<AndroidStringsTextUnit> list, String pluralNameSeparator) throws TransformerException, ParserConfigurationException {
        return toWriter(toDocument(list, pluralNameSeparator), new StringWriter()).toString();
    }

    public static String toText(List<AndroidStringsTextUnit> list) throws TransformerException, ParserConfigurationException {
        return toWriter(toDocument(list, DEFAULT_PLURAL_SEPARATOR), new StringWriter()).toString();
    }
}
