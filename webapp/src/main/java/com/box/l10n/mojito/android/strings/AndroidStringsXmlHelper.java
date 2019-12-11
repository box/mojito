package com.box.l10n.mojito.android.strings;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AndroidStringsXmlHelper {

    public static final String OTHER_ITEM_NAME = PluralName.other.toString();

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

        String getNameSeparator();
    }

    private static class SimpleItem implements Item {

        private String comment;
        private String name;
        private String content;
        private String id;
        private String pluralForm;
        private String pluralFormOther;

        private String nameSeparator;

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

        @Override
        public String getNameSeparator() {
            return nameSeparator;
        }
    }

    public static Item createItem(String comment, String name, String content, String id, String pluralForm, String pluralFormOther, String nameSeparator) {
        SimpleItem result = new SimpleItem();

        result.comment = comment;
        result.name = name;
        result.content = content;
        result.id = id;
        result.pluralForm = pluralForm;
        result.pluralFormOther = pluralFormOther;
        result.nameSeparator = nameSeparator;

        return result;
    }

    public static Item createItem(String comment, String name, String content, String id) {
        return createItem(comment, name, content, id, null, null, null);
    }

    public static Item createItem(String comment, String name, String content) {
        return createItem(comment, name, content, null, null, null, null);
    }

    private static abstract class XmlItem {

        private XmlDocument xmlDocument;
        private XmlItem parent;

        private String comment;
        private String name;

        public XmlItem(XmlDocument xmlDocument, XmlItem parent, String comment, String name) {
            this.xmlDocument = xmlDocument;
            this.parent = parent;
            this.comment = comment;
            this.name = name;
        }

        protected XmlDocument getDocument() {
            return xmlDocument;
        }

        protected XmlItem getParent() {
            return parent;
        }

        public String getComment() {
            if (getParent() == null) {
                return comment;
            } else {
                return getParent().getComment();
            }
        }

        public String getName() {
            if (getParent() == null) {
                return name;
            } else {
                return xmlDocument.getPluralName(getParent().getName(), name);
            }
        }

        public String getPluralForm() {
            if (getParent() == null) {
                return null;
            } else {
                return name;
            }
        }

        public String getPluralFormOther() {
            if (getParent() == null) {
                return null;
            } else {
                return xmlDocument.getPluralName(getParent().getName(), OTHER_ITEM_NAME);
            }
        }

        public String getNameSeparator() {
            return getDocument().getNameSeparator();
        }
    }

    static class SingleXmlItem extends XmlItem implements Item {

        private String content;
        private String id;

        public SingleXmlItem(XmlDocument xmlDocument, XmlItem parent, String comment, String name, String content, String id) {
            super(xmlDocument, parent, comment, name);
            this.content = content;
            this.id = id;
        }

        @Override
        public String getContent() {
            return content;
        }

        @Override
        public String getId() {
            return id;
        }
    }

    public enum PluralName {
        zero,
        one,
        two,
        few,
        many,
        other;

        private static Map<String, Integer> map = IntStream
                .range(0, PluralName.values().length).boxed()
                .collect(Collectors.toMap(i -> PluralName.values()[i].toString(), Function.identity()));

        private static int getIndex(String name) {
            Integer index = map.get(name);
            if (index == null) {
                return Integer.MAX_VALUE;
            } else {
                return index;
            }
        }

        public static void sortList(List<Item> itemList) {
            itemList.sort(Comparator.comparingInt(i -> PluralName.getIndex(i.getPluralForm())));
        }
    }

    public static class PluralXmlItem extends XmlItem {

        private Map<PluralName, Item> itemMap = new HashMap<>();

        private PluralXmlItem(XmlDocument xmlDocument, String comment, String name) {
            super(xmlDocument,null, comment, name);
        }

        public PluralXmlItem addItem(PluralName name, String content, String id) {
            itemMap.put(name, new SingleXmlItem(getDocument(), this, null, name.toString(), content, id));
            return this;
        }

        public PluralXmlItem addItem(PluralName name, String content) {
            return addItem(name, content, null);
        }

        public List<Item> getItems() {
            List<Item> result = new ArrayList<>(itemMap.values());
            PluralName.sortList(result);
            return result;
        }

        @Override
        public XmlDocument getDocument() {
            return super.getDocument();
        }
    }

    public static class XmlDocument {

        private List<XmlItem> xmlItemList = new ArrayList<>();
        private String nameSeparator = " _";

        private String getPluralName(String parentName, String itemName) {
            return parentName + nameSeparator + itemName;
        }

        public XmlDocument() {}

        public XmlDocument(String nameSeparator) {
            this.nameSeparator = nameSeparator;
        }

        public String getNameSeparator() {
            return nameSeparator;
        }

        public XmlDocument addSingle(String comment, String name, String content, String id) {
            xmlItemList.add(new SingleXmlItem(this, null, comment, name, content, id));
            return this;
        }

        public XmlDocument addSingle(String comment, String name, String content) {
            addSingle(comment, name, content, null);
            return this;
        }

        public PluralXmlItem addPlural(String comment, String name) {
            PluralXmlItem result = new PluralXmlItem(this, comment, name);
            xmlItemList.add(result);
            return result;
        }

        public List<Item> getItems() {
            return xmlItemList.stream().flatMap(xmlItem -> {
                if (xmlItem instanceof PluralXmlItem) {
                    return ((PluralXmlItem) xmlItem).getItems().stream();
                } else if (xmlItem instanceof SingleXmlItem) {
                    return Stream.of((Item) xmlItem);
                } else {
                    return null;
                }
            }).collect(Collectors.toList());
        }
    }

    static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        return factory.newDocumentBuilder();
    }
}
