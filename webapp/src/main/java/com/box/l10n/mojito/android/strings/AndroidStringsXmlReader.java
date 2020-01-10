package com.box.l10n.mojito.android.strings;

import com.google.common.base.Strings;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
        String result = null;
        if (str != null) {
            result = str.replaceAll("\\\\'", "'").replaceAll("\\\\\"", "\"").replace("\\\\\n", "\n").replaceAll("\\\\@", "@");
        }

        return result;
    }

    private static List<AndroidStringsTextUnit> fromDocument(Document document, String pluralNameSeparator) {

        List<AndroidStringsTextUnit> resultList = new ArrayList<>();

        Node lastCommentNode = null;
        for (int i = 0; i < document.getDocumentElement().getChildNodes().getLength(); i++) {
            Node currentNode = document.getDocumentElement().getChildNodes().item(i);
            if (Node.COMMENT_NODE == currentNode.getNodeType()) {
                lastCommentNode = currentNode;
            } else if (Node.ELEMENT_NODE == currentNode.getNodeType()) {
                Element currentElement = (Element) currentNode;

                switch (currentElement.getTagName()) {
                    case SINGULAR_ELEMENT_NAME:
                        resultList.add(createSingular(
                                getAttribute(currentElement, NAME_ATTRIBUTE_NAME),
                                removeEscape(currentElement.getTextContent()),
                                getContent(lastCommentNode),
                                getAttribute(currentElement, ID_ATTRIBUTE_NAME)));
                        break;

                    case PLURAL_ELEMENT_NAME:
                        String comment = getContent(lastCommentNode);
                        String name = getAttribute(currentElement, NAME_ATTRIBUTE_NAME);

                        NodeList nodeList = currentElement.getElementsByTagName(PLURAL_ITEM_ELEMENT_NAME);
                        for (int j = 0; j < nodeList.getLength(); j++) {
                            if (Node.ELEMENT_NODE == nodeList.item(j).getNodeType()) {
                                Element itemElement = (Element) nodeList.item(j);
                                resultList.add(createPlural(name, PluralItem.valueOf(getAttribute(itemElement, QUANTITY_ATTRIBUTE_NAME)),
                                        removeEscape(itemElement.getTextContent()), comment,
                                        getAttribute(itemElement, ID_ATTRIBUTE_NAME), pluralNameSeparator));
                            }
                        }
                        break;
                }
            }
        }

        return resultList;
    }

    public static List<AndroidStringsTextUnit> fromFile(String fileName, String pluralNameSeparator) throws ParserConfigurationException, IOException, SAXException {
        return fromDocument(getDocumentBuilder().parse(new File(fileName)), pluralNameSeparator);
    }

    public static List<AndroidStringsTextUnit> fromFile(String fileName) throws ParserConfigurationException, IOException, SAXException {
        return fromDocument(getDocumentBuilder().parse(new File(fileName)), DEFAULT_PLURAL_SEPARATOR);
    }

    public static List<AndroidStringsTextUnit> fromText(String text, String pluralNameSeparator) throws ParserConfigurationException, IOException, SAXException {
        return fromDocument(getDocumentBuilder().parse(new InputSource(new StringReader(text))), pluralNameSeparator);
    }

    public static List<AndroidStringsTextUnit> fromText(String text) throws ParserConfigurationException, IOException, SAXException {
        return fromDocument(getDocumentBuilder().parse(new InputSource(new StringReader(text))), DEFAULT_PLURAL_SEPARATOR);
    }
}
