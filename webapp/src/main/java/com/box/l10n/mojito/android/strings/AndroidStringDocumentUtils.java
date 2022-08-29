package com.box.l10n.mojito.android.strings;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;

public final class AndroidStringDocumentUtils {

  static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY =
      DocumentBuilderFactory.newInstance();
  static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

  static DocumentBuilder documentBuilder() throws ParserConfigurationException {
    return DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
  }

  private AndroidStringDocumentUtils() {
    throw new AssertionError("Do not instantiate");
  }
}
