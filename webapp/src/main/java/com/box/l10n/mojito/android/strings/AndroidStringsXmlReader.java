package com.box.l10n.mojito.android.strings;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static com.box.l10n.mojito.android.strings.AndroidStringsXmlHelper.*;

@Component
public class AndroidStringsXmlReader {

    private static String removeEscape(String str) {
        if (str == null) {
            return null;
        }

        return str.replaceAll("\\\\'", "'").replaceAll("\\\\\"", "\"").replace("\\\\\n", "\n").replaceAll("\\\\@", "@");
    }

    private static List<Item> fromDocument(Document document, String pluralNameSeparator) {

        List<Item> resultList = new ArrayList<>();

        Node lastCommentNode = null;
        for (int i = 0; i < document.getDocumentElement().getChildNodes().getLength(); i++) {
            Node currentNode = document.getDocumentElement().getChildNodes().item(i);
            if (Node.COMMENT_NODE == currentNode.getNodeType()) {
                lastCommentNode = currentNode;
            } else {
                switch (currentNode.getNodeName()) {
                    case SINGULAR_ELEMENT_NAME:
                        resultList.add(createSingular(
                                getAttribute(currentNode, NAME_ATTRIBUTE_NAME),
                                removeEscape(currentNode.getTextContent()),
                                getContent(lastCommentNode),
                                getAttribute(currentNode, ID_ATTRIBUTE_NAME)));
                        break;

                    case PLURAL_ELEMENT_NAME:
                        String comment = getContent(lastCommentNode);
                        String name = getAttribute(currentNode, NAME_ATTRIBUTE_NAME);

                        for (int j = 0; j < currentNode.getChildNodes().getLength(); j++) {
                            Node itemNode = currentNode.getChildNodes().item(j);
                            String quantity = getAttribute(itemNode, QUANTITY_ATTRIBUTE_NAME);
                            if (quantity != null) {
                                resultList.add(createPlural(name, PluralItem.valueOf(quantity),
                                        removeEscape(itemNode.getTextContent()), comment,
                                        getAttribute(itemNode, ID_ATTRIBUTE_NAME), pluralNameSeparator));
                            }
                        }
                        break;
                }
            }
        }

        return resultList;
    }

    public static List<Item> fromFile(String fileName, String pluralNameSeparator) throws ParserConfigurationException, IOException, SAXException {
        return fromDocument(getDocumentBuilder().parse(new File(fileName)), pluralNameSeparator);
    }

    public static List<Item> fromFile(String fileName) throws ParserConfigurationException, IOException, SAXException {
        return fromDocument(getDocumentBuilder().parse(new File(fileName)), DEFAULT_PLURAL_SEPARATOR);
    }

    public static List<Item> fromText(String text, String pluralNameSeparator) throws ParserConfigurationException, IOException, SAXException {
        return fromDocument(getDocumentBuilder().parse(new InputSource(new StringReader(text))), pluralNameSeparator);
    }

    public static List<Item> fromText(String text) throws ParserConfigurationException, IOException, SAXException {
        return fromDocument(getDocumentBuilder().parse(new InputSource(new StringReader(text))), DEFAULT_PLURAL_SEPARATOR);
    }
}
