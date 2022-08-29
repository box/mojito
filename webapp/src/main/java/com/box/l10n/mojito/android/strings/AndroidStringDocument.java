package com.box.l10n.mojito.android.strings;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Node;

public class AndroidStringDocument {

  private List<AbstractAndroidString> strings = new ArrayList<>();
  private List<AndroidSingular> singulars = new ArrayList<>();
  private List<AndroidPlural> plurals = new ArrayList<>();

  public List<AndroidSingular> getSingulars() {
    return singulars;
  }

  public List<AndroidPlural> getPlurals() {
    return plurals;
  }

  public List<AbstractAndroidString> getStrings() {
    return strings;
  }

  public void addSingular(AndroidSingular string) {
    singulars.add(string);
    strings.add(string);
  }

  public void addSingular(AndroidStringElement element, Node comment) {

    checkArgument(element.isSingular(), "element should be singular");

    addSingular(
        new AndroidSingular(
            element.getIdAttribute(),
            element.getNameAttribute(),
            element.getUnescapedContent(),
            comment != null ? comment.getTextContent() : null));
  }

  public void addPlural(AndroidPlural plural) {
    plurals.add(plural);
    strings.add(plural);
  }

  public void addPlural(AndroidStringElement element, Node comment) {

    checkArgument(element.isPlural(), "element should be plural");

    AndroidPlural.AndroidPluralBuilder builder = AndroidPlural.builder();
    builder.setName(element.getNameAttribute());
    if (comment != null) {
      builder.setComment(comment.getTextContent());
    }

    element.forEachPluralItem(builder::addItem);

    addPlural(builder.build());
  }

  public void addElement(Node node, Node comment) {

    AndroidStringElement element = new AndroidStringElement(node);

    if (element.isSingular()) {
      addSingular(element, comment);
    } else if (element.isPlural()) {
      addPlural(element, comment);
    }
  }
}
