package com.box.l10n.mojito.android.strings;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import static com.box.l10n.mojito.android.strings.AndroidStringsXmlHelper.*;

@Component
public class AndroidStringsXmlReader {

    private static String removeEscape(String str) {
        if (str == null) {
            return null;
        }

        return str.replaceAll("\\\\\'", "'").replaceAll("\\\\\"", "\"").replace("\\\\\n", "\n").replaceAll("\\\\@", "@");
    }

    private static class Xml {

        private static String getContent(Node node) {
            if (node == null) {
                return null;
            } else {
                return node.getTextContent();
            }
        }

        private static String getAttribute(Node node, String attributeName) {
            if (node != null) {
                NamedNodeMap map = node.getAttributes();
                if (map != null) {
                    return getContent(map.getNamedItem(attributeName));
                }
            }

            return null;
        }
    }

    private static XmlDocument fromDocument(Document document, XmlDocument result) {

        Node lastCommentNode = null;
        for (int i = 0; i < document.getDocumentElement().getChildNodes().getLength(); i++) {
            Node currentNode = document.getDocumentElement().getChildNodes().item(i);
            if (Node.COMMENT_NODE == currentNode.getNodeType()) {
                lastCommentNode = currentNode;
            } else {
                switch (currentNode.getNodeName()) {
                    case SINGULAR_ELEMENT_NAME:
                        result.addSingle(
                                Xml.getContent(lastCommentNode),
                                Xml.getAttribute(currentNode, NAME_ATTRIBUTE_NAME),
                                removeEscape(currentNode.getTextContent()),
                                Xml.getAttribute(currentNode, ID_ATTRIBUTE_NAME));
                        break;

                    case PLURAL_ELEMENT_NAME:
                        PluralXmlItem pluralItem = result.addPlural(
                                Xml.getContent(lastCommentNode),
                                Xml.getAttribute(currentNode, NAME_ATTRIBUTE_NAME));

                        for (int j = 0; j < currentNode.getChildNodes().getLength(); j++) {
                            Node itemNode = currentNode.getChildNodes().item(j);
                            String quantity = Xml.getAttribute(itemNode, QUANTITY_ATTRIBUTE_NAME);
                            if (quantity != null) {
                                pluralItem.addItem(
                                        PluralName.valueOf(quantity),
                                        removeEscape(itemNode.getTextContent()),
                                        Xml.getAttribute(itemNode, ID_ATTRIBUTE_NAME));
                            }
                        }
                        break;
                }
            }
        }

        return result;
    }

    public XmlDocument fromFile(String fileName, String nameGeneratorSeparator) throws ParserConfigurationException, IOException, SAXException {
        return fromDocument(getDocumentBuilder().parse(new File(fileName)), new XmlDocument(nameGeneratorSeparator));
    }

    public XmlDocument fromFile(String fileName) throws ParserConfigurationException, IOException, SAXException {
        return fromDocument(getDocumentBuilder().parse(new File(fileName)), new XmlDocument());
    }

    public XmlDocument fromText(String text, String nameGeneratorSeparator) throws ParserConfigurationException, IOException, SAXException {
        return fromDocument(getDocumentBuilder().parse(new InputSource(new StringReader(text))), new XmlDocument(nameGeneratorSeparator));
    }

    public XmlDocument fromText(String text) throws ParserConfigurationException, IOException, SAXException {
        return fromDocument(getDocumentBuilder().parse(new InputSource(new StringReader(text))), new XmlDocument());
    }
}
