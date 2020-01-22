package com.box.l10n.mojito.android.strings;

import com.google.common.base.Strings;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class AndroidStringsXmlHelper {

    public static final String DEFAULT_PLURAL_SEPARATOR = " _";

    public static final String ROOT_ELEMENT_NAME = "resources";
    public static final String SINGULAR_ELEMENT_NAME = "string";
    public static final String PLURAL_ELEMENT_NAME = "plurals";
    public static final String PLURAL_ITEM_ELEMENT_NAME = "item";
    public static final String NAME_ATTRIBUTE_NAME = "name";
    public static final String QUANTITY_ATTRIBUTE_NAME = "quantity";
    public static final String ID_ATTRIBUTE_NAME = "tmTextUnitId";

    public enum PluralItem {
        zero,
        one,
        two,
        few,
        many,
        other
    }

    public static AndroidStringsTextUnit createTextUnit(String name, String content, String comment, String id, String pluralForm, String pluralFormOther) {
        return createTextUnit(name, content, comment, Strings.isNullOrEmpty(id) ? null : Long.parseLong(id), pluralForm, pluralFormOther);
    }

    public static AndroidStringsTextUnit createTextUnit(String name, String content, String comment, Long id, String pluralForm, String pluralFormOther) {
        return new AndroidStringsTextUnit(name, content, comment, id, pluralForm, pluralFormOther);
    }

    public static AndroidStringsTextUnit createSingular(String name, String content, String comment, String id) {
        return createTextUnit(name, content, comment, id, null, null);
    }

    public static AndroidStringsTextUnit createSingular(String name, String content, String comment) {
        return createSingular(name, content, comment, null);
    }

    public static AndroidStringsTextUnit createPlural(String pluralName, PluralItem pluralItem, String content, String comment, String id, String pluralNameSeparator) {
        return createTextUnit(pluralName + pluralNameSeparator + pluralItem.name(), content, comment, id, pluralItem.name(), pluralName + pluralNameSeparator + PluralItem.other.name());
    }

    public static AndroidStringsTextUnit createPlural(String pluralName, PluralItem pluralItem, String content, String comment) {
        return createPlural(pluralName, pluralItem, content, comment, null, DEFAULT_PLURAL_SEPARATOR);
    }

    static Element createChild(Document document, Node node, String name) {
        Element result = document.createElement(name);
        node.appendChild(result);

        return result;
    }

    static void addText(Element element, String text) {
        if (text != null) {
            element.setTextContent(text);
        }
    }

    static void addComment(Document document, Node node, String comment) {
        if (comment != null) {
            node.appendChild(document.createComment(comment));
        }
    }

    static void addAttribute(Element element, String name, Object value) {
        if (value != null) {
            element.setAttribute(name, value.toString());
        }
    }

    static String getAttribute(Element element, String attributeName) {
        String result = null;
        if (element.hasAttribute(attributeName)) {
            result = element.getAttribute(attributeName);
        }

        return result;
    }

    static String getContent(Node node) {
        String result = null;
        if (node != null) {
            result = node.getTextContent();
        }

        return result;
    }

    static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        return factory.newDocumentBuilder();
    }
}
