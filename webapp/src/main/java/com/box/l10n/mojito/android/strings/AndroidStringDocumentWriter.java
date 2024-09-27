package com.box.l10n.mojito.android.strings;

import static com.box.l10n.mojito.android.strings.AndroidStringDocumentUtils.TRANSFORMER_FACTORY;
import static com.box.l10n.mojito.android.strings.AndroidStringDocumentUtils.documentBuilder;
import static com.box.l10n.mojito.android.strings.AndroidStringElement.ID_ATTRIBUTE_NAME;
import static com.box.l10n.mojito.android.strings.AndroidStringElement.NAME_ATTRIBUTE_NAME;
import static com.box.l10n.mojito.android.strings.AndroidStringElement.PLURAL_ELEMENT_NAME;
import static com.box.l10n.mojito.android.strings.AndroidStringElement.PLURAL_ITEM_ELEMENT_NAME;
import static com.box.l10n.mojito.android.strings.AndroidStringElement.QUANTITY_ATTRIBUTE_NAME;
import static com.box.l10n.mojito.android.strings.AndroidStringElement.ROOT_ELEMENT_NAME;
import static com.box.l10n.mojito.android.strings.AndroidStringElement.SINGULAR_ELEMENT_NAME;
import static java.util.Objects.requireNonNull;

import com.google.common.base.Strings;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class AndroidStringDocumentWriter {

  private AndroidStringDocument source;
  private DOMSource domSource;
  private Document document;
  private Node root;
  private EscapeType escapeType;

  public AndroidStringDocumentWriter(final AndroidStringDocument source) {
    this(source, EscapeType.QUOTE_AND_NEW_LINE);
  }

  public AndroidStringDocumentWriter(final AndroidStringDocument source, EscapeType escapeType) {
    this.source = requireNonNull(source);
    this.escapeType = escapeType;
    buildDomSource();
  }

  public void buildDomSource() {
    document = documentBuilder().newDocument();
    root = document.createElement(ROOT_ELEMENT_NAME);
    document.setXmlStandalone(true);
    document.appendChild(root);
    source.getStrings().forEach(this::addString);
    domSource = new DOMSource(document);
  }

  private <W extends Writer> W buildWriter(W writer) {

    Transformer transformer = null;
    try {
      transformer = TRANSFORMER_FACTORY.newTransformer();
    } catch (TransformerConfigurationException e) {
      throw new RuntimeException(e);
    }

    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "0");
    try {
      transformer.transform(domSource, new StreamResult(writer));
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }

    return writer;
  }

  public String toText() {
    return buildWriter(new StringWriter()).toString();
  }

  public void toFile(String path) throws IOException, TransformerException {
    buildWriter(new FileWriter(new File(path)));
  }

  private void addPlural(AndroidPlural plural) {
    addComment(root, plural.getComment());

    Element element = addPluralChild(root);
    setNameAttribute(element, plural.getName());
    plural.forEachItemSorted(item -> addPluralItemChild(element, item));
  }

  private void addString(AbstractAndroidString string) {
    if (string.isSingular()) {
      addSingular((AndroidSingular) string);
    } else if (string.isPlural()) {
      addPlural((AndroidPlural) string);
    }
  }

  private void addSingular(AndroidSingular singular) {
    addComment(root, singular.getComment());
    addSingularChild(root, singular);
  }

  private void addSingularChild(Node node, AndroidSingular singular) {
    Element element = addChild(node, SINGULAR_ELEMENT_NAME);
    setContent(element, singular.getContent());
    setNameAttribute(element, singular.getName());
    setIdAttribute(element, singular.getId().toString());
  }

  private Element addPluralChild(Node node) {
    return addChild(node, PLURAL_ELEMENT_NAME);
  }

  private void addPluralItemChild(Node node, AndroidPluralItem item) {
    Element element = addChild(node, PLURAL_ITEM_ELEMENT_NAME);
    setContent(element, item.getContent());
    setQuantityAttribute(element, item.getQuantity());
    setIdAttribute(element, item.getId().toString());
  }

  private void addComment(Node node, String comment) {
    if (!Strings.isNullOrEmpty(comment)) {
      node.appendChild(document.createComment(comment));
    }
  }

  private Element addChild(Node node, String name) {
    Element result = document.createElement(name);
    node.appendChild(result);
    return result;
  }

  private void setContent(Element element, String content) {
    if (!Strings.isNullOrEmpty(content)) {
      element.setTextContent(escapeQuotes(content, escapeType));
    }
  }

  private void setNameAttribute(Element element, String string) {
    setAttribute(element, NAME_ATTRIBUTE_NAME, string);
  }

  private void setIdAttribute(Element element, String string) {
    setAttribute(element, ID_ATTRIBUTE_NAME, string);
  }

  private void setQuantityAttribute(Element element, AndroidPluralQuantity quantity) {
    setAttribute(element, QUANTITY_ATTRIBUTE_NAME, quantity.toString());
  }

  private void setAttribute(Element element, String name, String value) {
    if (!Strings.isNullOrEmpty(value)) {
      element.setAttribute(name, value);
    }
  }

  static String escapeQuotes(String str, EscapeType escapeType) {
    String escaped = str;
    if (!Strings.isNullOrEmpty(str)) {
      escaped =
          switch (escapeType) {
            case QUOTE_AND_NEW_LINE -> str.replaceAll("\"", "\\\\\"").replaceAll("\n", "\\\\n");
            case NEW_LINE -> str.replaceAll("\n", "\\\\n");
            case NONE -> str;
          };
    }
    return escaped;
  }

  public enum EscapeType {
    QUOTE_AND_NEW_LINE, // Legacy
    NEW_LINE,
    NONE
  }
}
