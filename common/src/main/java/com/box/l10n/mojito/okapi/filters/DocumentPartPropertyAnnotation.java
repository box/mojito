package com.box.l10n.mojito.okapi.filters;

import net.sf.okapi.common.annotation.IAnnotation;

/**
 * Annotation used to store information about a {@link net.sf.okapi.common.resource.DocumentPart}'s
 * {@link net.sf.okapi.common.resource.Property} that can be localized.
 */
public class DocumentPartPropertyAnnotation implements IAnnotation {
  String propertyKey;
  String name;
  String source;
  String comment;

  public String getPropertyKey() {
    return propertyKey;
  }

  public void setPropertyKey(String propertyKey) {
    this.propertyKey = propertyKey;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
}
