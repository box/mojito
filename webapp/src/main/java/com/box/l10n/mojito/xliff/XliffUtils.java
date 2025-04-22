package com.box.l10n.mojito.xliff;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Utilities for working with XLIFF.
 *
 * @author jaurambault
 */
@Component
public class XliffUtils {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(XliffUtils.class);

  /**
   * Gets the target language of the XLIFF by looking at the first "file" element.
   *
   * @param xliffContent xliff content from which to extract the target language
   * @return the target language or {@code null} if not found
   */
  public String getTargetLanguage(String xliffContent) {

    String targetLanguage = null;

    InputSource inputSource = new InputSource(new StringReader(xliffContent));
    XPath xPath = XPathFactory.newInstance().newXPath();

    SimpleNamespaceContext simpleNamespaceContext = new SimpleNamespaceContext();
    simpleNamespaceContext.bindNamespaceUri("xlf", "urn:oasis:names:tc:xliff:document:1.2");
    xPath.setNamespaceContext(simpleNamespaceContext);

    try {
      Node node =
          (Node)
              xPath.evaluate(
                  "/xlf:xliff/xlf:file[1]/@target-language", inputSource, XPathConstants.NODE);

      if (node != null) {
        targetLanguage = node.getTextContent();
      }

    } catch (XPathExpressionException xpee) {
      logger.debug("Can't extract target language from xliff", xpee);
    }
    return targetLanguage;
  }

  public String removeAttribute(String xmlContent, String attributeName)
      throws ParserConfigurationException,
          IOException,
          SAXException,
          XPathExpressionException,
          TransformerException {
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    dbFactory.setNamespaceAware(true); // Enable namespace awareness
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(new InputSource(new StringReader(xmlContent)));

    // Use XPath to find the element containing the attribute
    XPath xPath = XPathFactory.newInstance().newXPath();
    NodeList nodes =
        (NodeList)
            xPath
                .compile(String.format("//*[@%s]", attributeName))
                .evaluate(doc, XPathConstants.NODESET);

    // Remove the attribute
    for (int i = 0; i < nodes.getLength(); i++) {
      Node node = nodes.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        ((org.w3c.dom.Element) node).removeAttribute(attributeName);
      }
    }

    // Convert the modified Document back to a string
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    StringWriter writer = new StringWriter();
    transformer.transform(new DOMSource(doc), new StreamResult(writer));
    return writer.toString();
  }

  public String removeElement(String xmlContent, String elementName)
      throws ParserConfigurationException, IOException, SAXException, TransformerException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = builder.parse(new InputSource(new StringReader(xmlContent)));
    document.getDocumentElement().normalize();
    NodeList elements = document.getElementsByTagName(elementName);

    while (elements.getLength() > 0) {
      Node binUnit = elements.item(0);
      binUnit.getParentNode().removeChild(binUnit);
    }

    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

    StringWriter writer = new StringWriter();
    transformer.transform(new DOMSource(document), new StreamResult(writer));
    return writer.toString();
  }
}
