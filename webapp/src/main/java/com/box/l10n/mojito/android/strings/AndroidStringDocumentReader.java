package com.box.l10n.mojito.android.strings;

import static com.box.l10n.mojito.android.strings.AndroidStringDocumentUtils.documentBuilder;

import com.google.common.base.Strings;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.Queue;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class AndroidStringDocumentReader {

  public static AndroidStringDocument fromFile(String fileName)
      throws ParserConfigurationException, IOException, SAXException {
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
        document.addElement(node, commentNodes.poll());
      }
    }

    return document;
  }
}
