package com.box.l10n.mojito.android.strings;

import com.google.common.base.Strings;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AndroidStringElement {

  static final String ROOT_ELEMENT_NAME = "resources";
  static final String SINGULAR_ELEMENT_NAME = "string";
  static final String PLURAL_ELEMENT_NAME = "plurals";
  static final String PLURAL_ITEM_ELEMENT_NAME = "item";
  static final String NAME_ATTRIBUTE_NAME = "name";
  static final String QUANTITY_ATTRIBUTE_NAME = "quantity";
  static final String ID_ATTRIBUTE_NAME = "tmTextUnitId";

  private final Element element;

  public AndroidStringElement(Element element) {
    this.element = element;
  }

  public AndroidStringElement(Node node) {
    this((Element) node);
  }

  public boolean isSingular() {
    return element.getTagName().equals(SINGULAR_ELEMENT_NAME);
  }

  public boolean isPlural() {
    return element.getTagName().equals(PLURAL_ELEMENT_NAME);
  }

  public boolean isPluralItem() {
    return element.getTagName().equals(PLURAL_ITEM_ELEMENT_NAME);
  }

  public String getTextContent() {
    return Optional.ofNullable(element).map(Element::getTextContent).orElse("");
  }

  public Long getIdAttribute() {
    return getLongAttribute(ID_ATTRIBUTE_NAME);
  }

  public String getNameAttribute() {
    return getAttribute(NAME_ATTRIBUTE_NAME);
  }

  public String getAttribute(String name) {
    return Optional.ofNullable(element.getAttribute(name)).orElse("");
  }

  public String getUnescapedContent() {
    return removeEscape(element.getTextContent());
  }

  public Long getLongAttribute(String attributeName) {
    return getAttributeAs(attributeName, Long::valueOf);
  }

  public <T> T getAttributeAs(String attributeName, Function<String, T> converter) {

    T result = null;

    if (element.hasAttribute(attributeName)) {
      result = converter.apply(element.getAttribute(attributeName));
    }

    return result;
  }

  public void forEachPluralItem(Consumer<AndroidPluralItem> consumer) {

    AndroidStringElement node;
    NodeList nodeList = element.getElementsByTagName(PLURAL_ITEM_ELEMENT_NAME);

    for (int i = 0; i < nodeList.getLength(); i++) {

      node = new AndroidStringElement((Element) nodeList.item(i));

      if (node.isPluralItem()) {
        consumer.accept(
            new AndroidPluralItem(
                node.getAttribute(QUANTITY_ATTRIBUTE_NAME),
                node.getLongAttribute(ID_ATTRIBUTE_NAME),
                node.getTextContent()));
      }
    }
  }

  private static String removeEscape(String str) {
    return Strings.nullToEmpty(str)
        .replaceAll("\\\\'", "'")
        .replaceAll("\\\\\"", "\"")
        .replace("\\\\\n", "\n")
        .replaceAll("\\\\@", "@");
  }
}
