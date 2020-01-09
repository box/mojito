package com.box.l10n.mojito.android.strings;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class AndroidStringsXmlHelper {

    public static final String DEFAULT_PLURAL_SEPARATOR = " _";

    public static final String ROOT_ELEMENT_NAME = "resources";
    public static final String SINGULAR_ELEMENT_NAME = "string";
    public static final String PLURAL_ELEMENT_NAME = "plurals";
    public static final String PLURAL_ITEM_ELEMENT_NAME = "item";
    public static final String NAME_ATTRIBUTE_NAME = "name";
    public static final String QUANTITY_ATTRIBUTE_NAME = "quantity";
    public static final String ID_ATTRIBUTE_NAME = "tmTextUnitId";

    public interface Item {
        String getComment();
        String getName();
        String getContent();
        String getId();
        String getPluralForm();
        String getPluralFormOther();
    }

    public enum PluralItem {
        zero,
        one,
        two,
        few,
        many,
        other;

        private static final Map<String, Integer> ordinalMap = Arrays.stream(PluralItem.values()).collect(
                Collectors.toMap(PluralItem::name, PluralItem::ordinal));

        public static int compare(Item o1, Item o2) {
            return Integer.compare(
                    ordinalMap.getOrDefault(o1.getPluralForm(), Integer.MAX_VALUE),
                    ordinalMap.getOrDefault(o2.getPluralForm(), Integer.MAX_VALUE));
        }
    }

    private static class PlainItem implements Item {

        private String comment;
        private String name;
        private String content;
        private String id;
        private String pluralForm;
        private String pluralFormOther;

        public PlainItem(String comment, String name, String content, String id, String pluralForm, String pluralFormOther) {
            this.comment = comment;
            this.name = name;
            this.content = content;
            this.id = id;
            this.pluralForm = pluralForm;
            this.pluralFormOther = pluralFormOther;
        }

        @Override
        public String getComment() {
            return comment;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getContent() {
            return content;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getPluralForm() {
            return pluralForm;
        }

        @Override
        public String getPluralFormOther() {
            return pluralFormOther;
        }
    }

    public static Item createItem(String comment, String name, String content, String id, String pluralForm, String pluralFormOther) {
        return new PlainItem(comment, name, content, id, pluralForm, pluralFormOther);
    }

    public static Item createSingular(String comment, String name, String content, String id) {
        return createItem(comment, name, content, id, null, null);
    }

    public static Item createSingular(String comment, String name, String content) {
        return createItem(comment, name, content, null, null, null);
    }

    public static Item createPlural(String comment, String pluralName, PluralItem pluralItem, String content, String id, String pluralNameSeparator) {
        return createItem(comment, pluralName + pluralNameSeparator + pluralItem.name(), content, id, pluralItem.name(), pluralName + pluralNameSeparator + PluralItem.other.name());
    }

    public static Item createPlural(String comment, String pluralName, PluralItem pluralItem, String content) {
        return createPlural(comment, pluralName, pluralItem, content, null, DEFAULT_PLURAL_SEPARATOR);
    }

    static Element createChild(Document document, Node node, String name) {
        Element result = document.createElement(name);
        node.appendChild(result);

        return result;
    }

    static void addText(Element element, String text) {
        if (text != null) element.setTextContent(text);
    }

    static void addComment(Document document, Node node, String comment) {
        if (comment != null) node.appendChild(document.createComment(comment));
    }

    static void addAttribute(Element element, String name, String value) {
        if (value != null) element.setAttribute(name, value);
    }

    static String getAttribute(Node node, String attributeName) {
        if (node != null) {
            NamedNodeMap map = node.getAttributes();
            if (map != null) {
                return getContent(map.getNamedItem(attributeName));
            }
        }

        return null;
    }

    static String getContent(Node node) {
        if (node == null) {
            return null;
        } else {
            return node.getTextContent();
        }
    }

    static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        return factory.newDocumentBuilder();
    }
}
