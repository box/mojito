package com.box.l10n.mojito.android.strings;

import static com.box.l10n.mojito.android.strings.AndroidStringDocumentUtils.documentBuilder;

import com.google.common.base.Strings;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class AndroidStringDocumentReader {

  public static AndroidStringDocument fromFile(String fileName) throws IOException, SAXException {
    return buildFromDocument(documentBuilder().parse(new File(fileName)));
  }

  public static AndroidStringDocument fromText(String text) {
    try {
      return buildFromDocument(
          documentBuilder().parse(new InputSource(new StringReader(Strings.nullToEmpty(text)))));
    } catch (SAXException | IOException e) {
      throw new AndroidStringDocumentReaderException(e);
    }
  }

  private static AndroidStringDocument buildFromDocument(Document source) {

    Queue<Node> commentNodes = new LinkedList<>();
    AndroidStringDocument document = new AndroidStringDocument();
    Node node;

    for (int i = 0; i < source.getDocumentElement().getChildNodes().getLength(); i++) {

      node = source.getDocumentElement().getChildNodes().item(i);

      if (Node.COMMENT_NODE == node.getNodeType()) {
        commentNodes.offer(node);

      } else if (Node.ELEMENT_NODE == node.getNodeType()) {
        Element nodeAsElement = (Element) node;
        Node comment = commentNodes.poll();

        if (nodeAsElement.getTagName().equals(AndroidStringElement.SINGULAR_ELEMENT_NAME)) {
          document.addSingular(
              new AndroidSingular(
                  getAttributeAs(
                      nodeAsElement, AndroidStringElement.ID_ATTRIBUTE_NAME, Long::valueOf),
                  getNameAttribute(nodeAsElement),
                  unescape(nodeAsElement.getTextContent()),
                  comment != null ? comment.getTextContent() : null));
        } else if (nodeAsElement.getTagName().equals(AndroidStringElement.PLURAL_ELEMENT_NAME)) {

          AndroidPlural.AndroidPluralBuilder builder = AndroidPlural.builder();

          builder.setName(nodeAsElement.getAttribute(AndroidStringElement.NAME_ATTRIBUTE_NAME));
          if (comment != null) {
            builder.setComment(comment.getTextContent());
          }

          NodeList nodeList =
              nodeAsElement.getElementsByTagName(AndroidStringElement.PLURAL_ITEM_ELEMENT_NAME);

          for (int j = 0; j < nodeList.getLength(); j++) {
            Element item = (Element) nodeList.item(j);

            builder.addItem(
                new AndroidPluralItem(
                    getAttribute(item, AndroidStringElement.QUANTITY_ATTRIBUTE_NAME),
                    getAttributeAs(item, AndroidStringElement.ID_ATTRIBUTE_NAME, Long::valueOf),
                    unescape(getTextContent(item))));
          }

          document.addPlural(builder.build());
        }
      }
    }

    return document;
  }

  public static String getAttribute(Element element, String name) {
    // looks like ofNullable is useless and then the orElse
    return Optional.ofNullable(element.getAttribute(name)).orElse("");
  }

  public static <T> T getAttributeAs(
      Element element, String attributeName, Function<String, T> converter) {

    T result = null;

    if (element.hasAttribute(attributeName)) {
      result = converter.apply(element.getAttribute(attributeName));
    }

    return result;
  }

  public static String getTextContent(Element element) {
    return Optional.ofNullable(element).map(Element::getTextContent).orElse("");
  }

  public static String getNameAttribute(Element element) {
    return element.getAttribute(AndroidStringElement.NAME_ATTRIBUTE_NAME);
  }

  /** should use {@link com.box.l10n.mojito.okapi.filters.AndroidFilter#unescape(String)} */
  static String unescape(String str) {

    String unescape = str;

    if (!Strings.isNullOrEmpty(str)) {

      if (StringUtils.startsWith(unescape, "\"") && StringUtils.endsWith(unescape, "\"")) {
        unescape = unescape.substring(1, unescape.length() - 1);
      }

      unescape =
          Strings.nullToEmpty(unescape)
              .replaceAll("\\\\'", "'")
              .replaceAll("\\\\\"", "\"")
              .replaceAll("\\\\@", "@")
              .replaceAll("\\\\n", "\n");
    }
    return unescape;
  }
}
